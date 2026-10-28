package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.NetworkDataSource
import com.example.data.RealNetworkProbe
import com.example.data.local.IspPortalEntity
import com.example.data.local.NocDatabase
import com.example.data.local.OltEntity
import com.example.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Cyber City Monitoring Tool", appName)
    }

    @Test
    fun `verify initial network dataset`() {
        val olts = NetworkDataSource.getInitialOlts()
        val mikrotiks = NetworkDataSource.getInitialMikrotiks()
        val onus = NetworkDataSource.getInitialOnus()

        assertTrue(olts.isNotEmpty())
        assertTrue(mikrotiks.isNotEmpty())
        assertTrue(onus.isNotEmpty())
    }

    @Test
    fun `verify 4 ISP portals and URLs`() {
        val portals = NetworkDataSource.getInitialIspPortals()
        assertEquals(4, portals.size)

        val urls = portals.map { it.url }
        assertTrue(urls.contains("https://fms.bsnl.in/login"))
        assertTrue(urls.contains("https://partner.meghbelabroadband.in/Partner/Accounts.aspx"))
        assertTrue(urls.contains("https://partners.gtplkcbpl.com/partner"))
        assertTrue(urls.contains("https://cybercity.phpradius.com/admin"))

        portals.forEach { portal ->
            assertTrue("Password must not be blank for ${portal.name}", portal.password.isNotBlank())
            assertTrue("Username must not be blank for ${portal.name}", portal.username.isNotBlank())
        }
    }

    @Test
    fun `verify room local database isp credentials saving`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = NocDatabase.getDatabase(context)
        val dao = db.nocDao()

        val bsnlPortal = IspPortalEntity(
            id = "fms_test_bsnl",
            name = "BSNL Test FMS",
            shortName = "BSNL",
            url = "https://fms.bsnl.in/login",
            username = "test_user_bsnl",
            password = "InitialPassword#123",
            partnerCode = "SSA-TEST-01",
            notes = "Test Note",
            category = "FMS",
            primaryColorHex = 0xFF0284C7
        )
        dao.insertIspPortal(bsnlPortal)

        dao.updateIspCredentials(
            id = "fms_test_bsnl",
            username = "updated_bsnl_user",
            password = "NewEncryptedPassword#2026",
            partnerCode = "SSA-TEST-02",
            notes = "Updated credentials saved"
        )

        val portalsList = dao.getAllIspPortals().first()
        val saved = portalsList.find { it.id == "fms_test_bsnl" }
        assertNotNull(saved)
        assertEquals("NewEncryptedPassword#2026", saved?.password)
        assertEquals("updated_bsnl_user", saved?.username)
    }

    @Test
    fun `verify vsol router configuration default options`() {
        val config = VsolRouterConfig(
            serialNumber = "VSOL-9921-A101",
            model = "V2804AX (WiFi 6 GPON)",
            wanMode = VsolWanMode.PPPOE,
            vlanId = 100,
            wifiSsid5 = "CyberCity_Fiber_5G_WiFi6"
        )

        assertEquals("VSOL-9921-A101", config.serialNumber)
        assertEquals(VsolWanMode.PPPOE, config.wanMode)
        assertEquals(100, config.vlanId)
        assertTrue(config.wifiSsid5.contains("5G"))
    }

    @Test
    fun `verify 24 hour historical traffic series and onu power history`() {
        val series = NetworkDataSource.generate24HourTrafficSeries("sfp-sfpplus1-Transit-NTT")
        assertEquals(24, series.size)
        assertEquals("Now", series.last().hourLabel)
        assertTrue(series.all { it.inBps > 0 && it.outBps > 0 })

        val powerHistory = NetworkDataSource.getInitialOnuPowerHistory()
        assertTrue(powerHistory.isNotEmpty())
    }

    @Test
    fun `verify onu nominal range and map visualization layer quality`() {
        val onus = NetworkDataSource.getInitialOnus()
        val normalOnu = onus.first { it.id == "onu-101" } // -18.4 dBm (within +/- 3 dBm of -20.0 dBm)
        val highPowerOnu = onus.first { it.id == "onu-103" } // -15.8 dBm (> -17.0 dBm, high overload)
        val lowPowerOnu = onus.first { it.id == "onu-105" } // -28.9 dBm (< -23.0 dBm, critical low)

        assertFalse(normalOnu.isOutsideNominalRange(3.0f))
        assertTrue(highPowerOnu.isOutsideNominalRange(3.0f))
        assertTrue(highPowerOnu.isHighPowerOverload(3.0f))
        assertTrue(lowPowerOnu.isOutsideNominalRange(3.0f))
        assertTrue(lowPowerOnu.isLowPowerDeviation(3.0f))

        assertEquals(NodeHealthQuality.OPTIMAL, normalOnu.getLayerQuality(MapVisualizationLayer.AVG_LATENCY))
        assertEquals(NodeHealthQuality.CRITICAL, lowPowerOnu.getLayerQuality(MapVisualizationLayer.PACKET_LOSS))
    }

    @Test
    fun `verify automated ip range discovery scanner`() = runBlocking {
        val discovered = RealNetworkProbe.scanIpRangeForDevices(
            subnetRange = "10.100.1.1-254",
            existingIps = emptySet(),
            scanCycle = 1,
            onProgress = { _, _ -> }
        )
        assertTrue(discovered.isNotEmpty())
        assertTrue(discovered.any { it.deviceType == DiscoveredDeviceType.OLT || it.deviceType == DiscoveredDeviceType.MIKROTIK })
    }

    @Test
    fun `verify room local database persistence for OLTs`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = NocDatabase.getDatabase(context)
        val dao = db.nocDao()
        val olt = OltEntity(
            id = "test-olt-1",
            name = "Test OLT",
            ipAddress = "192.168.1.1",
            port = 23,
            vendorName = "HUAWEI",
            model = "MA5608T",
            totalSlots = 2,
            totalPonPorts = 8,
            statusName = "ONLINE",
            cpuUsage = 20,
            memoryUsage = 35,
            temperatureCelsius = 40.0f,
            snmpCommunity = "public",
            location = "Lab",
            lastSeenTimestamp = System.currentTimeMillis()
        )
        dao.insertOlt(olt)
        val count = dao.getOltCount()
        assertTrue(count >= 1)
    }

    @Test
    fun `verify IpDiscoveryService background service identifies active OLTs and MikroTiks`() = runBlocking {
        val controller = org.robolectric.Robolectric.buildService(com.example.data.IpDiscoveryService::class.java).create()
        val service = controller.get()
        assertTrue(service.isServiceRunning)

        val binder = service.onBind(null) as com.example.data.IpDiscoveryService.LocalBinder
        val boundService = binder.getService()
        assertNotNull(boundService)

        val discovered = boundService.performImmediateDiscoverySweep("192.168.88.0/24")
        assertTrue(discovered.isNotEmpty())
        assertTrue(discovered.any { it.deviceType == DiscoveredDeviceType.OLT })
        assertTrue(discovered.any { it.deviceType == DiscoveredDeviceType.MIKROTIK })
        assertEquals("192.168.88.0/24", boundService.activeSubnetRange)

        controller.destroy()
        assertFalse(service.isServiceRunning)
    }

    @Test
    fun `verify live map latency and packet loss layer for OLTs MikroTiks and ONUs`() {
        val olts = NetworkDataSource.getInitialOlts()
        val mikrotiks = NetworkDataSource.getInitialMikrotiks()
        val onus = NetworkDataSource.getInitialOnus()

        val huaweiOlt = olts.first { it.id == "olt-huawei-01" }
        val zteOlt = olts.first { it.id == "olt-zte-02" }
        assertEquals(NodeHealthQuality.OPTIMAL, huaweiOlt.getLayerQuality(MapVisualizationLayer.LATENCY_AND_PACKET_LOSS))
        assertEquals(NodeHealthQuality.WARNING, zteOlt.getLayerQuality(MapVisualizationLayer.LATENCY_AND_PACKET_LOSS))
        assertTrue(mikrotiks.any { it.getLayerQuality(MapVisualizationLayer.LATENCY_AND_PACKET_LOSS) == NodeHealthQuality.OPTIMAL })
        val degradedOnu = onus.first { it.id == "onu-105" }
        assertEquals(NodeHealthQuality.CRITICAL, degradedOnu.getLayerQuality(MapVisualizationLayer.LATENCY_AND_PACKET_LOSS))
    }

    @Test
    fun `verify traceroute speed test latency dns lookup and ip gateway tools`() = runBlocking {
        // 1. What is my IP & Gateway
        val ipInfo = RealNetworkProbe.inspectMyIpAndGateway()
        assertTrue(ipInfo.localIp.isNotBlank())
        assertTrue(ipInfo.gatewayIp.isNotBlank())
        assertTrue(ipInfo.publicIp.isNotBlank())

        // 2. Latency & Jitter Analysis
        val latencyResult = RealNetworkProbe.analyzeLatencyAndJitter("8.8.8.8", samples = 3)
        assertEquals("8.8.8.8", latencyResult.host)
        assertTrue(latencyResult.avgMs > 0f)
        assertTrue(latencyResult.jitterMs >= 0f)

        // 3. Traceroute
        val traceState = RealNetworkProbe.runTraceroute("8.8.8.8") { _, _ -> }
        assertFalse(traceState.isRunning)
        assertTrue(traceState.hops.size >= 3)
        assertEquals(1, traceState.hops.first().hopNumber)

        // 4. DNS Lookup
        val dnsResult = RealNetworkProbe.lookupDns("fms.bsnl.in", "A")
        assertTrue(dnsResult.isSuccess)
        assertTrue(dnsResult.records.isNotEmpty())

        // 5. Speed Test
        val speedState = RealNetworkProbe.runSpeedTest { }
        assertFalse(speedState.isRunning)
        assertTrue(speedState.downloadMbps > 0f)
        assertTrue(speedState.uploadMbps > 0f)
        assertTrue(speedState.pingMs > 0f)
    }
}
