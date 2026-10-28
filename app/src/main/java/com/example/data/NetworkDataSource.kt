package com.example.data

import com.example.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin
import kotlin.random.Random

object NetworkDataSource {

    fun getInitialOlts(): List<OltDevice> = listOf(
        OltDevice(
            id = "olt-huawei-01",
            name = "HQ-Core-MA5800",
            ipAddress = "10.100.1.10",
            port = 23,
            vendor = OltVendor.HUAWEI,
            model = "SmartAX MA5800-X7",
            totalSlots = 7,
            totalPonPorts = 16,
            status = DeviceStatus.ONLINE,
            cpuUsage = 24,
            memoryUsage = 46,
            temperatureCelsius = 43.8f,
            snmpCommunity = "noc_read",
            location = "Main Central Office, Rack 03",
            latitude = 37.7750,
            longitude = -122.4180,
            avgLatencyMs = 2.4f,
            packetLossPercent = 0.0f,
            currentRxBps = 2_840_000_000L,
            currentTxBps = 3_450_000_000L,
            uplinkCapacityMbps = 10000L,
            bandwidthUtilizationPercent = 34.5f
        ),
        OltDevice(
            id = "olt-zte-02",
            name = "North-Dist-C320",
            ipAddress = "10.100.2.10",
            port = 23,
            vendor = OltVendor.ZTE,
            model = "ZXA10 C320",
            totalSlots = 2,
            totalPonPorts = 8,
            status = DeviceStatus.WARNING,
            cpuUsage = 58,
            memoryUsage = 64,
            temperatureCelsius = 52.1f,
            snmpCommunity = "noc_read",
            location = "North Substation Shelter, Rack 01",
            latitude = 37.7850,
            longitude = -122.4120,
            avgLatencyMs = 19.6f,
            packetLossPercent = 0.85f,
            currentRxBps = 890_000_000L,
            currentTxBps = 1_150_000_000L,
            uplinkCapacityMbps = 2000L,
            bandwidthUtilizationPercent = 57.5f
        ),
        OltDevice(
            id = "olt-vsol-03",
            name = "South-Feeder-V1600",
            ipAddress = "10.100.3.10",
            port = 80,
            vendor = OltVendor.VSOL,
            model = "V1600G1-B GPON",
            totalSlots = 1,
            totalPonPorts = 8,
            status = DeviceStatus.ONLINE,
            cpuUsage = 17,
            memoryUsage = 38,
            temperatureCelsius = 41.2f,
            snmpCommunity = "public",
            location = "South Metro Tower Pop",
            latitude = 37.7650,
            longitude = -122.4250,
            avgLatencyMs = 4.1f,
            packetLossPercent = 0.05f,
            currentRxBps = 1_420_000_000L,
            currentTxBps = 1_980_000_000L,
            uplinkCapacityMbps = 10000L,
            bandwidthUtilizationPercent = 19.8f
        )
    )

    fun getInitialPonPorts(): List<PonPort> = listOf(
        PonPort("pon-01-01", "olt-huawei-01", "0/1/1", totalOnus = 64, onlineOnus = 62, txPowerDbm = 4.4f),
        PonPort("pon-01-02", "olt-huawei-01", "0/1/2", totalOnus = 48, onlineOnus = 48, txPowerDbm = 4.2f),
        PonPort("pon-01-03", "olt-huawei-01", "0/1/3", totalOnus = 56, onlineOnus = 51, txPowerDbm = 4.1f),
        PonPort("pon-02-01", "olt-zte-02", "1/1/1", totalOnus = 60, onlineOnus = 56, txPowerDbm = 3.9f),
        PonPort("pon-02-02", "olt-zte-02", "1/1/2", totalOnus = 50, onlineOnus = 44, txPowerDbm = 3.8f),
        PonPort("pon-03-01", "olt-vsol-03", "PON-1", totalOnus = 40, onlineOnus = 39, txPowerDbm = 4.6f)
    )

    fun getInitialOnus(): List<OnuDevice> = listOf(
        OnuDevice(
            id = "onu-101",
            oltId = "olt-huawei-01",
            ponPort = "0/1/1",
            onuIndex = 1,
            customerName = "Apex Hospital OPD",
            pppoeAccount = "apex_hospital_corp",
            serialNumber = "HWTC-9281-AA12",
            rxPowerDbm = -18.4f,
            txPowerDbm = 2.4f,
            oltRxPowerDbm = -19.2f,
            nominalPowerDbm = -20.0f,
            distanceMeters = 840,
            status = OnuStatus.ONLINE,
            temperatureCelsius = 37.4f,
            voltageVolts = 3.31f,
            planSpeed = "500 Mbps Dedicated",
            address = "104 Metro Medical Blvd, Central",
            latitude = 37.7735,
            longitude = -122.4150,
            avgLatencyMs = 2.6f,
            packetLossPercent = 0.0f
        ),
        OnuDevice(
            id = "onu-102",
            oltId = "olt-huawei-01",
            ponPort = "0/1/1",
            onuIndex = 2,
            customerName = "TechHub Co-Working",
            pppoeAccount = "techhub_office",
            serialNumber = "HWTC-4820-BB34",
            rxPowerDbm = -21.2f,
            txPowerDbm = 2.2f,
            oltRxPowerDbm = -22.1f,
            nominalPowerDbm = -20.0f,
            distanceMeters = 1420,
            status = OnuStatus.ONLINE,
            temperatureCelsius = 39.8f,
            voltageVolts = 3.29f,
            planSpeed = "300 Mbps",
            address = "502 Silicon Ave, Floor 3",
            latitude = 37.7765,
            longitude = -122.4195,
            avgLatencyMs = 4.8f,
            packetLossPercent = 0.05f
        ),
        OnuDevice(
            id = "onu-103",
            oltId = "olt-huawei-01",
            ponPort = "0/1/1",
            onuIndex = 3,
            customerName = "David Miller (Res-A4)",
            pppoeAccount = "david_miller_ftth",
            serialNumber = "HWTC-1109-CC56",
            rxPowerDbm = -15.8f, // High optical power (+4.2 dBm above -20 dBm nominal -> triggers high power alert!)
            txPowerDbm = 2.5f,
            oltRxPowerDbm = -16.2f,
            nominalPowerDbm = -20.0f,
            distanceMeters = 180,
            status = OnuStatus.WARNING,
            temperatureCelsius = 43.0f,
            voltageVolts = 3.34f,
            planSpeed = "100 Mbps",
            address = "78 Redwood Residential Way",
            latitude = 37.7720,
            longitude = -122.4210,
            avgLatencyMs = 18.4f,
            packetLossPercent = 0.7f,
            lastOfflineReason = "High optical input overload (+4.2 dB over nominal -20 dBm, attenuator needed)"
        ),
        OnuDevice(
            id = "onu-104",
            oltId = "olt-huawei-01",
            ponPort = "0/1/1",
            onuIndex = 4,
            customerName = "Metro Cineplex Tower",
            pppoeAccount = "metro_cineplex_vip",
            serialNumber = "HWTC-7721-DD78",
            rxPowerDbm = -26.3f, // -6.3 dB below -20 dBm nominal
            txPowerDbm = 1.9f,
            oltRxPowerDbm = -27.1f,
            nominalPowerDbm = -20.0f,
            distanceMeters = 3450,
            status = OnuStatus.WARNING,
            temperatureCelsius = 44.2f,
            voltageVolts = 3.24f,
            planSpeed = "200 Mbps",
            address = "12 Entertainment Plaza",
            latitude = 37.7780,
            longitude = -122.4130,
            avgLatencyMs = 28.5f,
            packetLossPercent = 1.4f,
            lastOfflineReason = "Optical power outside +/- 3 dBm nominal range"
        ),
        OnuDevice(
            id = "onu-105",
            oltId = "olt-huawei-01",
            ponPort = "0/1/1",
            onuIndex = 5,
            customerName = "Green Valley School",
            pppoeAccount = "gv_school_lab",
            serialNumber = "HWTC-6310-EE90",
            rxPowerDbm = -28.9f,
            txPowerDbm = 1.6f,
            oltRxPowerDbm = -29.8f,
            nominalPowerDbm = -20.0f,
            distanceMeters = 4200,
            status = OnuStatus.CRITICAL,
            temperatureCelsius = 46.5f,
            voltageVolts = 3.21f,
            planSpeed = "100 Mbps",
            address = "300 Campus Drive, North Gate",
            latitude = 37.7810,
            longitude = -122.4230,
            avgLatencyMs = 64.2f,
            packetLossPercent = 5.8f,
            lastOfflineReason = "Optical link budget degraded / high splice loss"
        ),
        OnuDevice(
            id = "onu-106",
            oltId = "olt-huawei-01",
            ponPort = "0/1/3",
            onuIndex = 6,
            customerName = "Grand Hyatt Banquet",
            pppoeAccount = "hyatt_banquet",
            serialNumber = "HWTC-9032-FF11",
            rxPowerDbm = -36.0f,
            txPowerDbm = 0.0f,
            oltRxPowerDbm = -40.0f,
            nominalPowerDbm = -20.0f,
            distanceMeters = 1850,
            status = OnuStatus.OFFLINE,
            temperatureCelsius = 0.0f,
            voltageVolts = 0.0f,
            planSpeed = "1 Gbps",
            address = "88 Grand Avenue, Central",
            latitude = 37.7710,
            longitude = -122.4140,
            avgLatencyMs = 999.0f,
            packetLossPercent = 100.0f,
            lastOfflineReason = "Loss of Signal (LOS) / Fiber unplugged"
        ),
        OnuDevice(
            id = "onu-201",
            oltId = "olt-zte-02",
            ponPort = "1/1/1",
            onuIndex = 1,
            customerName = "Northway Diagnostics Lab",
            pppoeAccount = "northway_lab_corp",
            serialNumber = "ZTEG-1144-JJ44",
            rxPowerDbm = -19.6f,
            txPowerDbm = 2.4f,
            oltRxPowerDbm = -20.2f,
            nominalPowerDbm = -20.0f,
            distanceMeters = 1100,
            status = OnuStatus.ONLINE,
            temperatureCelsius = 39.1f,
            voltageVolts = 3.28f,
            planSpeed = "200 Mbps",
            address = "90 Innovation Park, North",
            latitude = 37.7865,
            longitude = -122.4105,
            avgLatencyMs = 7.4f,
            packetLossPercent = 0.1f
        ),
        OnuDevice(
            id = "onu-202",
            oltId = "olt-zte-02",
            ponPort = "1/1/1",
            onuIndex = 2,
            customerName = "Skyline Apartments Block C",
            pppoeAccount = "skyline_c_dist",
            serialNumber = "ZTEG-5599-KK55",
            rxPowerDbm = -27.2f,
            txPowerDbm = 1.8f,
            oltRxPowerDbm = -28.0f,
            nominalPowerDbm = -20.0f,
            distanceMeters = 3800,
            status = OnuStatus.WARNING,
            temperatureCelsius = 43.1f,
            voltageVolts = 3.25f,
            planSpeed = "100 Mbps",
            address = "45 Skyline Boulevard, North",
            latitude = 37.7880,
            longitude = -122.4140,
            avgLatencyMs = 34.0f,
            packetLossPercent = 1.9f,
            lastOfflineReason = "Optical power marginal (-7.2 dB from nominal)"
        ),
        OnuDevice(
            id = "onu-203",
            oltId = "olt-zte-02",
            ponPort = "1/1/2",
            onuIndex = 3,
            customerName = "Redwood Primary School",
            pppoeAccount = "redwood_school_net",
            serialNumber = "ZTEG-8833-LL66",
            rxPowerDbm = -29.4f,
            txPowerDbm = 1.4f,
            oltRxPowerDbm = -30.5f,
            nominalPowerDbm = -20.0f,
            distanceMeters = 4600,
            status = OnuStatus.CRITICAL,
            temperatureCelsius = 45.8f,
            voltageVolts = 3.20f,
            planSpeed = "50 Mbps",
            address = "12 Forest Lane, North Suburb",
            latitude = 37.7830,
            longitude = -122.4080,
            avgLatencyMs = 78.5f,
            packetLossPercent = 6.4f,
            lastOfflineReason = "Optical power below sensitivity limit (-28 dBm)"
        ),
        OnuDevice(
            id = "onu-301",
            oltId = "olt-vsol-03",
            ponPort = "PON-1",
            onuIndex = 1,
            customerName = "Southern Harbor Hotel",
            pppoeAccount = "south_harbor_hotel",
            serialNumber = "VSOL-2233-NN88",
            rxPowerDbm = -18.9f,
            txPowerDbm = 2.6f,
            oltRxPowerDbm = -19.5f,
            nominalPowerDbm = -20.0f,
            distanceMeters = 750,
            status = OnuStatus.ONLINE,
            temperatureCelsius = 36.5f,
            voltageVolts = 3.32f,
            planSpeed = "500 Mbps",
            address = "20 Marina Gateway, South Port",
            latitude = 37.7630,
            longitude = -122.4270,
            avgLatencyMs = 3.2f,
            packetLossPercent = 0.0f
        ),
        OnuDevice(
            id = "onu-302",
            oltId = "olt-vsol-03",
            ponPort = "PON-1",
            onuIndex = 2,
            customerName = "Harbor Logistics Hub",
            pppoeAccount = "harbor_logistics",
            serialNumber = "VSOL-7788-PP99",
            rxPowerDbm = -22.1f,
            txPowerDbm = 2.3f,
            oltRxPowerDbm = -22.8f,
            nominalPowerDbm = -20.0f,
            distanceMeters = 1700,
            status = OnuStatus.ONLINE,
            temperatureCelsius = 39.4f,
            voltageVolts = 3.29f,
            planSpeed = "200 Mbps",
            address = "70 Harbor Cargo Way, South Port",
            latitude = 37.7610,
            longitude = -122.4230,
            avgLatencyMs = 5.9f,
            packetLossPercent = 0.0f
        )
    )

    fun getInitialOnuPowerHistory(onus: List<OnuDevice> = getInitialOnus()): List<OnuPowerHistoryPoint> {
        val now = System.currentTimeMillis()
        val points = mutableListOf<OnuPowerHistoryPoint>()
        for (onu in onus) {
            for (step in 11 downTo 0) {
                val ts = now - (step * 3600_000L) // Past 12 hours at 1h intervals
                val drift = if (onu.status == OnuStatus.OFFLINE) 0f
                            else (sin(step.toDouble() * 0.6) * 0.45f).toFloat()
                val sampleRx = if (onu.status == OnuStatus.OFFLINE) -36.0f
                               else (onu.rxPowerDbm + drift).coerceIn(-35.0f, -12.0f)
                points.add(
                    OnuPowerHistoryPoint(
                        id = "pwr-${onu.id}-$step",
                        onuId = onu.id,
                        timestamp = ts,
                        rxPowerDbm = sampleRx,
                        txPowerDbm = onu.txPowerDbm,
                        nominalPowerDbm = onu.nominalPowerDbm
                    )
                )
            }
        }
        return points
    }

    fun getInitialMikrotiks(): List<MikrotikDevice> = listOf(
        MikrotikDevice(
            id = "mtk-ccr2004-01",
            name = "HQ-Core-CCR2004",
            ipAddress = "10.100.0.1",
            restPort = 443,
            apiPort = 8728,
            username = "admin",
            model = "CCR2004-1G-12S+2XS",
            routerOsVersion = "v7.16.1",
            uptimeString = "52d 18h 44m",
            cpuUsage = 28,
            totalRamMb = 4096,
            freeRamMb = 2950,
            temperatureCelsius = 44.5f,
            voltage = 24.2f,
            status = DeviceStatus.ONLINE,
            activePppoeClients = 948,
            activeHotspotUsers = 135,
            avgLatencyMs = 1.4f,
            packetLossPercent = 0.0f,
            currentRxBps = 4_850_000_000L,
            currentTxBps = 1_920_000_000L,
            bandwidthUtilizationPercent = 48.5f
        ),
        MikrotikDevice(
            id = "mtk-rb4011-02",
            name = "North-Edge-RB4011",
            ipAddress = "10.100.0.2",
            restPort = 443,
            apiPort = 8728,
            username = "admin",
            model = "RB4011iGS+5HacQ2HnD",
            routerOsVersion = "v7.15.2",
            uptimeString = "21d 06h 12m",
            cpuUsage = 64,
            totalRamMb = 1024,
            freeRamMb = 412,
            temperatureCelsius = 49.8f,
            voltage = 24.0f,
            status = DeviceStatus.WARNING,
            activePppoeClients = 380,
            activeHotspotUsers = 45,
            avgLatencyMs = 16.8f,
            packetLossPercent = 0.65f,
            currentRxBps = 890_000_000L,
            currentTxBps = 1_120_000_000L,
            bandwidthUtilizationPercent = 82.4f
        ),
        MikrotikDevice(
            id = "mtk-ccr1036-03",
            name = "South-Gateway-CCR1036",
            ipAddress = "10.100.0.3",
            restPort = 443,
            apiPort = 8728,
            username = "admin",
            model = "CCR1036-8G-2S+",
            routerOsVersion = "v7.16.0",
            uptimeString = "118d 02h 55m",
            cpuUsage = 19,
            totalRamMb = 8192,
            freeRamMb = 6450,
            temperatureCelsius = 42.1f,
            voltage = 24.4f,
            status = DeviceStatus.ONLINE,
            activePppoeClients = 620,
            activeHotspotUsers = 80,
            avgLatencyMs = 2.9f,
            packetLossPercent = 0.0f,
            currentRxBps = 2_150_000_000L,
            currentTxBps = 1_340_000_000L,
            bandwidthUtilizationPercent = 21.5f
        )
    )

    fun getInitialInterfaces(): List<NetworkInterface> = listOf(
        NetworkInterface(
            id = "if-ccr-01",
            routerId = "mtk-ccr2004-01",
            name = "sfp-sfpplus1-Transit-NTT",
            comment = "10G Primary Internet Uplink Transit (NTT Tier-1)",
            type = InterfaceType.SFP_PLUS,
            linkStatus = LinkStatus.UP,
            linkSpeedMbps = 10000,
            currentRxBps = 4_850_000_000L,
            currentTxBps = 1_920_000_000L,
            rxPacketsPerSec = 412000,
            txPacketsPerSec = 195000,
            mtu = 1500,
            macAddress = "48:8F:5A:01:22:11"
        ),
        NetworkInterface(
            id = "if-ccr-02",
            routerId = "mtk-ccr2004-01",
            name = "sfp-sfpplus2-Transit-Cogent",
            comment = "10G Secondary Internet Transit (Cogent)",
            type = InterfaceType.SFP_PLUS,
            linkStatus = LinkStatus.UP,
            linkSpeedMbps = 10000,
            currentRxBps = 1_120_000_000L,
            currentTxBps = 480_000_000L,
            rxPacketsPerSec = 98000,
            txPacketsPerSec = 45000,
            mtu = 1500,
            macAddress = "48:8F:5A:01:22:12"
        ),
        NetworkInterface(
            id = "if-ccr-03",
            routerId = "mtk-ccr2004-01",
            name = "sfp-sfpplus3-Trunk-MA5800",
            comment = "Trunk to Huawei Core OLT MA5800",
            type = InterfaceType.SFP_PLUS,
            linkStatus = LinkStatus.UP,
            linkSpeedMbps = 10000,
            currentRxBps = 2_840_000_000L,
            currentTxBps = 3_450_000_000L,
            rxPacketsPerSec = 285000,
            txPacketsPerSec = 310000,
            mtu = 1500,
            macAddress = "48:8F:5A:01:22:13"
        ),
        NetworkInterface(
            id = "if-ccr-04",
            routerId = "mtk-ccr2004-01",
            name = "sfp-sfpplus4-Trunk-ZTE-C320",
            comment = "Trunk to North Substation ZTE C320",
            type = InterfaceType.SFP_PLUS,
            linkStatus = LinkStatus.UP,
            linkSpeedMbps = 10000,
            currentRxBps = 890_000_000L,
            currentTxBps = 1_150_000_000L,
            rxPacketsPerSec = 82000,
            txPacketsPerSec = 94000,
            mtu = 1500,
            macAddress = "48:8F:5A:01:22:14"
        ),
        NetworkInterface(
            id = "if-ccr-05",
            routerId = "mtk-ccr2004-01",
            name = "ether1-Mgmt-OOB",
            comment = "Out-of-band management console",
            type = InterfaceType.ETHERNET,
            linkStatus = LinkStatus.UP,
            linkSpeedMbps = 1000,
            currentRxBps = 145_000L,
            currentTxBps = 210_000L,
            rxPacketsPerSec = 180,
            txPacketsPerSec = 220,
            mtu = 1500,
            macAddress = "48:8F:5A:01:22:10"
        ),
        NetworkInterface(
            id = "if-ccr-06",
            routerId = "mtk-ccr2004-01",
            name = "sfp-sfpplus5-Cache-Server",
            comment = "Local CDN Cache Engine",
            type = InterfaceType.SFP_PLUS,
            linkStatus = LinkStatus.DOWN,
            linkSpeedMbps = 10000,
            currentRxBps = 0,
            currentTxBps = 0,
            rxPacketsPerSec = 0,
            txPacketsPerSec = 0,
            mtu = 1500,
            macAddress = "48:8F:5A:01:22:15",
            errorCount = 14
        ),
        NetworkInterface(
            id = "if-rb-01",
            routerId = "mtk-rb4011-02",
            name = "sfp-sfpplus1-Uplink",
            comment = "10G Uplink to HQ CCR2004",
            type = InterfaceType.SFP_PLUS,
            linkStatus = LinkStatus.UP,
            linkSpeedMbps = 10000,
            currentRxBps = 890_000_000L,
            currentTxBps = 1_120_000_000L,
            rxPacketsPerSec = 84000,
            txPacketsPerSec = 92000,
            mtu = 1500,
            macAddress = "B8:69:F4:88:1A:01"
        ),
        NetworkInterface(
            id = "if-rb-02",
            routerId = "mtk-rb4011-02",
            name = "ether1-C320-Uplink",
            comment = "Direct Gigabit feed to ZTE C320",
            type = InterfaceType.ETHERNET,
            linkStatus = LinkStatus.UP,
            linkSpeedMbps = 1000,
            currentRxBps = 875_000_000L,
            currentTxBps = 910_000_000L,
            rxPacketsPerSec = 89000,
            txPacketsPerSec = 96000,
            mtu = 1500,
            macAddress = "B8:69:F4:88:1A:02"
        ),
        NetworkInterface(
            id = "if-ccr36-01",
            routerId = "mtk-ccr1036-03",
            name = "sfp1-Transit-Telia",
            comment = "Telia Carrier Uplink",
            type = InterfaceType.SFP_PLUS,
            linkStatus = LinkStatus.UP,
            linkSpeedMbps = 10000,
            currentRxBps = 2_150_000_000L,
            currentTxBps = 1_340_000_000L,
            rxPacketsPerSec = 220000,
            txPacketsPerSec = 145000,
            mtu = 1500,
            macAddress = "00:0C:42:DE:01:A1"
        )
    )

    fun getInitialAlerts(): List<NetworkAlert> = listOf(
        NetworkAlert(
            id = "alert-001",
            targetType = AlertTargetType.ONU,
            targetId = "onu-105",
            targetName = "Green Valley School (HWTC-6310-EE90)",
            severity = AlertSeverity.CRITICAL,
            title = "Critical Optical Rx Power Degradation",
            description = "Rx optical power dropped to -28.9 dBm (outside +/- 3 dBm nominal -20.0 dBm & below critical -27.5 dBm).",
            metricValue = "-28.9 dBm",
            thresholdValue = "-23.0 to -17.0 dBm",
            timestamp = System.currentTimeMillis() - 180000,
            deliveryChannels = "Email + Webhook",
            deliveryStatus = "Dispatched (SMTP & Webhook 200 OK)"
        ),
        NetworkAlert(
            id = "alert-002",
            targetType = AlertTargetType.ONU,
            targetId = "onu-103",
            targetName = "David Miller (HWTC-1109-CC56)",
            severity = AlertSeverity.WARNING,
            title = "High Optical Power Overload (> +3 dBm Nominal)",
            description = "ONU Rx power -15.8 dBm exceeds upper acceptable threshold (-17.0 dBm, +3 dBm from nominal -20.0 dBm). Install optical attenuator.",
            metricValue = "-15.8 dBm",
            thresholdValue = "Max -17.0 dBm",
            timestamp = System.currentTimeMillis() - 320000,
            deliveryChannels = "Email + Webhook",
            deliveryStatus = "Dispatched (SMTP & Webhook 200 OK)"
        ),
        NetworkAlert(
            id = "alert-003",
            targetType = AlertTargetType.ONU,
            targetId = "onu-106",
            targetName = "Grand Hyatt Banquet (HWTC-9032-FF11)",
            severity = AlertSeverity.CRITICAL,
            title = "Loss of Signal (LOS) / Fiber Cut",
            description = "ONU optical signal loss detected on PON 0/1/3. SFP Rx is -40.0 dBm (No light).",
            metricValue = "-36.0 dBm (LOS)",
            thresholdValue = "-27.5 dBm",
            timestamp = System.currentTimeMillis() - 720000,
            deliveryChannels = "Email + Webhook",
            deliveryStatus = "Dispatched (SMTP & Webhook 200 OK)"
        ),
        NetworkAlert(
            id = "alert-004",
            targetType = AlertTargetType.INTERFACE,
            targetId = "if-ccr-06",
            targetName = "sfp-sfpplus5-Cache-Server on HQ-Core-CCR2004",
            severity = AlertSeverity.CRITICAL,
            title = "Interface Link Status Changed to DOWN",
            description = "SFP+ link down event detected on HQ-Core-CCR2004. SFP module reporting no optical carrier.",
            metricValue = "Link DOWN",
            thresholdValue = "Link UP",
            timestamp = System.currentTimeMillis() - 1500000,
            deliveryChannels = "Email + Webhook",
            deliveryStatus = "Dispatched (SMTP & Webhook 200 OK)"
        ),
        NetworkAlert(
            id = "alert-005",
            targetType = AlertTargetType.ONU,
            targetId = "onu-104",
            targetName = "Metro Cineplex Tower (HWTC-7721-DD78)",
            severity = AlertSeverity.WARNING,
            title = "Low Optical Power Outside +/- 3 dBm Nominal Range",
            description = "Rx optical power degraded to -26.3 dBm (-6.3 dB from nominal -20.0 dBm).",
            metricValue = "-26.3 dBm",
            thresholdValue = "-23.0 dBm (-3 dB)",
            timestamp = System.currentTimeMillis() - 950000,
            deliveryChannels = "Email + Webhook",
            deliveryStatus = "Dispatched (SMTP & Webhook 200 OK)"
        )
    )

    fun getInitialIspPortals(): List<IspPortal> = listOf(
        IspPortal(
            id = "fms_bsnl",
            name = "BSNL FTTH FMS Portal",
            shortName = "BSNL FMS",
            url = "https://fms.bsnl.in/",
            username = "bsnl_partner_cc",
            password = "Bsnl@NocPass2026",
            partnerCode = "SSA-KOL-FTTH-09",
            notes = "BSNL Fiber Management System - ONT Provisioning & Line Test",
            category = "FMS Portal",
            primaryColorHex = 0xFF0284C7
        ),
        IspPortal(
            id = "fms_meghbela",
            name = "Meghbela Broadband Partner Portal",
            shortName = "Meghbela",
            url = "https://partner.meghbelabroadband.in/Partner/Accounts.aspx",
            username = "meghbela_cc_admin",
            password = "Meghbela#Auth88",
            partnerCode = "MEGH-PARTNER-772",
            notes = "Meghbela Partner Billing, Accounts & Customer Management",
            category = "Partner Billing",
            primaryColorHex = 0xFFD97706
        ),
        IspPortal(
            id = "fms_gtpl",
            name = "GTPL KCBPL Partner Portal",
            shortName = "GTPL KCBPL",
            url = "https://partners.gtplkcbpl.com/partner",
            username = "gtpl_kcbpl_9901",
            password = "GtplKcbpl*Sec26",
            partnerCode = "KCBPL-LCO-4412",
            notes = "GTPL KCBPL LCO Operator & Subscriber Provisioning Portal",
            category = "LCO Portal",
            primaryColorHex = 0xFFDC2626
        ),
        IspPortal(
            id = "fms_phpradius",
            name = "Cyber City phpRadius Admin",
            shortName = "CyberCity Radius",
            url = "https://cybercity.phpradius.com/admin",
            username = "admin_cybercity",
            password = "CyberCity@Radius#99",
            partnerCode = "CYBER-AAA-01",
            notes = "phpRadius AAA Server - User Profiles, NAS & Bandwidth Queues",
            category = "Radius Admin",
            primaryColorHex = 0xFF059669
        )
    )

    /**
     * Generates a 24-hour historical interface throughput series (hourly intervals from -23h to Now)
     * modeling realistic ISP diurnal traffic patterns (night trough, daytime office ramp, evening streaming peak).
     */
    fun generate24HourTrafficSeries(
        interfaceName: String = "All Uplinks",
        baseInBps: Long = 5_200_000_000L,
        baseOutBps: Long = 2_100_000_000L
    ): List<HourlyTrafficPoint> {
        val now = System.currentTimeMillis()
        val hourFormatter = SimpleDateFormat("HH:00", Locale.US)
        val seedOffset = interfaceName.hashCode().let { if (it < 0) -it else it } % 17

        // Diurnal ISP load curve factors across 24 hours relative to current time
        val diurnalFactors = listOf(
            0.42f, 0.34f, 0.28f, 0.25f, 0.27f, 0.35f,
            0.48f, 0.62f, 0.74f, 0.81f, 0.85f, 0.88f,
            0.86f, 0.84f, 0.87f, 0.91f, 0.96f, 1.04f,
            1.15f, 1.26f, 1.32f, 1.24f, 1.08f, 1.00f
        )

        return (0..23).map { index ->
            val hoursAgo = 23 - index
            val pointTimestamp = now - (hoursAgo * 3_600_000L)
            val label = if (hoursAgo == 0) "Now" else hourFormatter.format(Date(pointTimestamp))
            val factor = diurnalFactors[index]
            val wave = 1.0f + (sin((index + seedOffset) * 0.55) * 0.06).toFloat()

            val inVal = (baseInBps * factor * wave).toLong().coerceAtLeast(50_000_000L)
            val outVal = (baseOutBps * factor * (2.0f - wave)).toLong().coerceAtLeast(25_000_000L)

            HourlyTrafficPoint(
                hourIndex = index,
                hourLabel = label,
                timestamp = pointTimestamp,
                inBps = inVal,
                outBps = outVal,
                peakInBps = (inVal * 1.14f).toLong(),
                peakOutBps = (outVal * 1.12f).toLong()
            )
        }
    }
}
