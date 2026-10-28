package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.*

@Entity(tableName = "olts")
data class OltEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val vendorName: String,
    val model: String,
    val totalSlots: Int,
    val totalPonPorts: Int,
    val statusName: String,
    val cpuUsage: Int,
    val memoryUsage: Int,
    val temperatureCelsius: Float,
    val snmpCommunity: String,
    val location: String,
    val latitude: Double = 37.7750,
    val longitude: Double = -122.4180,
    val avgLatencyMs: Float = 2.8f,
    val packetLossPercent: Float = 0.0f,
    val currentRxBps: Long = 2_840_000_000L,
    val currentTxBps: Long = 3_450_000_000L,
    val uplinkCapacityMbps: Long = 10000L,
    val bandwidthUtilizationPercent: Float = 34.5f,
    val lastSeenTimestamp: Long
) {
    fun toDomain(): OltDevice = OltDevice(
        id = id,
        name = name,
        ipAddress = ipAddress,
        port = port,
        vendor = try { OltVendor.valueOf(vendorName) } catch (_: Exception) { OltVendor.HUAWEI },
        model = model,
        totalSlots = totalSlots,
        totalPonPorts = totalPonPorts,
        status = try { DeviceStatus.valueOf(statusName) } catch (_: Exception) { DeviceStatus.ONLINE },
        cpuUsage = cpuUsage,
        memoryUsage = memoryUsage,
        temperatureCelsius = temperatureCelsius,
        snmpCommunity = snmpCommunity,
        location = location,
        latitude = latitude,
        longitude = longitude,
        avgLatencyMs = avgLatencyMs,
        packetLossPercent = packetLossPercent,
        currentRxBps = currentRxBps,
        currentTxBps = currentTxBps,
        uplinkCapacityMbps = uplinkCapacityMbps,
        bandwidthUtilizationPercent = bandwidthUtilizationPercent,
        lastSeenTimestamp = lastSeenTimestamp
    )

    companion object {
        fun fromDomain(d: OltDevice): OltEntity = OltEntity(
            id = d.id,
            name = d.name,
            ipAddress = d.ipAddress,
            port = d.port,
            vendorName = d.vendor.name,
            model = d.model,
            totalSlots = d.totalSlots,
            totalPonPorts = d.totalPonPorts,
            statusName = d.status.name,
            cpuUsage = d.cpuUsage,
            memoryUsage = d.memoryUsage,
            temperatureCelsius = d.temperatureCelsius,
            snmpCommunity = d.snmpCommunity,
            location = d.location,
            latitude = d.latitude,
            longitude = d.longitude,
            avgLatencyMs = d.avgLatencyMs,
            packetLossPercent = d.packetLossPercent,
            currentRxBps = d.currentRxBps,
            currentTxBps = d.currentTxBps,
            uplinkCapacityMbps = d.uplinkCapacityMbps,
            bandwidthUtilizationPercent = d.bandwidthUtilizationPercent,
            lastSeenTimestamp = d.lastSeenTimestamp
        )
    }
}

@Entity(tableName = "pon_ports")
data class PonPortEntity(
    @PrimaryKey val id: String,
    val oltId: String,
    val name: String,
    val totalOnus: Int,
    val onlineOnus: Int,
    val txPowerDbm: Float,
    val adminState: Boolean,
    val operState: Boolean
) {
    fun toDomain(): PonPort = PonPort(
        id = id,
        oltId = oltId,
        name = name,
        totalOnus = totalOnus,
        onlineOnus = onlineOnus,
        txPowerDbm = txPowerDbm,
        adminState = adminState,
        operState = operState
    )

    companion object {
        fun fromDomain(p: PonPort): PonPortEntity = PonPortEntity(
            id = p.id,
            oltId = p.oltId,
            name = p.name,
            totalOnus = p.totalOnus,
            onlineOnus = p.onlineOnus,
            txPowerDbm = p.txPowerDbm,
            adminState = p.adminState,
            operState = p.operState
        )
    }
}

@Entity(tableName = "onus")
data class OnuEntity(
    @PrimaryKey val id: String,
    val oltId: String,
    val ponPort: String,
    val onuIndex: Int,
    val customerName: String,
    val pppoeAccount: String,
    val serialNumber: String,
    val rxPowerDbm: Float,
    val txPowerDbm: Float,
    val oltRxPowerDbm: Float,
    val nominalPowerDbm: Float = -20.0f,
    val distanceMeters: Int,
    val statusName: String,
    val temperatureCelsius: Float,
    val voltageVolts: Float,
    val biasCurrentMa: Float,
    val planSpeed: String,
    val address: String = "Cyber City Sector 4",
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194,
    val avgLatencyMs: Float = 4.2f,
    val packetLossPercent: Float = 0.0f,
    val lastOfflineReason: String?,
    val lastSeenTimestamp: Long
) {
    fun toDomain(): OnuDevice = OnuDevice(
        id = id,
        oltId = oltId,
        ponPort = ponPort,
        onuIndex = onuIndex,
        customerName = customerName,
        pppoeAccount = pppoeAccount,
        serialNumber = serialNumber,
        rxPowerDbm = rxPowerDbm,
        txPowerDbm = txPowerDbm,
        oltRxPowerDbm = oltRxPowerDbm,
        nominalPowerDbm = nominalPowerDbm,
        distanceMeters = distanceMeters,
        status = try { OnuStatus.valueOf(statusName) } catch (_: Exception) { OnuStatus.ONLINE },
        temperatureCelsius = temperatureCelsius,
        voltageVolts = voltageVolts,
        biasCurrentMa = biasCurrentMa,
        planSpeed = planSpeed,
        address = address,
        latitude = latitude,
        longitude = longitude,
        avgLatencyMs = avgLatencyMs,
        packetLossPercent = packetLossPercent,
        lastOfflineReason = lastOfflineReason,
        lastSeenTimestamp = lastSeenTimestamp
    )

    companion object {
        fun fromDomain(o: OnuDevice): OnuEntity = OnuEntity(
            id = o.id,
            oltId = o.oltId,
            ponPort = o.ponPort,
            onuIndex = o.onuIndex,
            customerName = o.customerName,
            pppoeAccount = o.pppoeAccount,
            serialNumber = o.serialNumber,
            rxPowerDbm = o.rxPowerDbm,
            txPowerDbm = o.txPowerDbm,
            oltRxPowerDbm = o.oltRxPowerDbm,
            nominalPowerDbm = o.nominalPowerDbm,
            distanceMeters = o.distanceMeters,
            statusName = o.status.name,
            temperatureCelsius = o.temperatureCelsius,
            voltageVolts = o.voltageVolts,
            biasCurrentMa = o.biasCurrentMa,
            planSpeed = o.planSpeed,
            address = o.address,
            latitude = o.latitude,
            longitude = o.longitude,
            avgLatencyMs = o.avgLatencyMs,
            packetLossPercent = o.packetLossPercent,
            lastOfflineReason = o.lastOfflineReason,
            lastSeenTimestamp = o.lastSeenTimestamp
        )
    }
}

@Entity(tableName = "onu_power_history")
data class OnuPowerHistoryEntity(
    @PrimaryKey val id: String,
    val onuId: String,
    val timestamp: Long,
    val rxPowerDbm: Float,
    val txPowerDbm: Float,
    val nominalPowerDbm: Float = -20.0f
) {
    fun toDomain(): OnuPowerHistoryPoint = OnuPowerHistoryPoint(
        id = id,
        onuId = onuId,
        timestamp = timestamp,
        rxPowerDbm = rxPowerDbm,
        txPowerDbm = txPowerDbm,
        nominalPowerDbm = nominalPowerDbm
    )

    companion object {
        fun fromDomain(p: OnuPowerHistoryPoint): OnuPowerHistoryEntity = OnuPowerHistoryEntity(
            id = p.id,
            onuId = p.onuId,
            timestamp = p.timestamp,
            rxPowerDbm = p.rxPowerDbm,
            txPowerDbm = p.txPowerDbm,
            nominalPowerDbm = p.nominalPowerDbm
        )
    }
}

@Entity(tableName = "mikrotiks")
data class MikrotikEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ipAddress: String,
    val restPort: Int,
    val apiPort: Int,
    val username: String,
    val model: String,
    val routerOsVersion: String,
    val uptimeString: String,
    val cpuUsage: Int,
    val totalRamMb: Long,
    val freeRamMb: Long,
    val temperatureCelsius: Float,
    val voltage: Float,
    val statusName: String,
    val activePppoeClients: Int,
    val activeHotspotUsers: Int,
    val avgLatencyMs: Float = 1.8f,
    val packetLossPercent: Float = 0.0f,
    val currentRxBps: Long = 4_850_000_000L,
    val currentTxBps: Long = 1_920_000_000L,
    val bandwidthUtilizationPercent: Float = 48.5f,
    val lastSeenTimestamp: Long
) {
    fun toDomain(): MikrotikDevice = MikrotikDevice(
        id = id,
        name = name,
        ipAddress = ipAddress,
        restPort = restPort,
        apiPort = apiPort,
        username = username,
        model = model,
        routerOsVersion = routerOsVersion,
        uptimeString = uptimeString,
        cpuUsage = cpuUsage,
        totalRamMb = totalRamMb,
        freeRamMb = freeRamMb,
        temperatureCelsius = temperatureCelsius,
        voltage = voltage,
        status = try { DeviceStatus.valueOf(statusName) } catch (_: Exception) { DeviceStatus.ONLINE },
        activePppoeClients = activePppoeClients,
        activeHotspotUsers = activeHotspotUsers,
        avgLatencyMs = avgLatencyMs,
        packetLossPercent = packetLossPercent,
        currentRxBps = currentRxBps,
        currentTxBps = currentTxBps,
        bandwidthUtilizationPercent = bandwidthUtilizationPercent,
        lastSeenTimestamp = lastSeenTimestamp
    )

    companion object {
        fun fromDomain(m: MikrotikDevice): MikrotikEntity = MikrotikEntity(
            id = m.id,
            name = m.name,
            ipAddress = m.ipAddress,
            restPort = m.restPort,
            apiPort = m.apiPort,
            username = m.username,
            model = m.model,
            routerOsVersion = m.routerOsVersion,
            uptimeString = m.uptimeString,
            cpuUsage = m.cpuUsage,
            totalRamMb = m.totalRamMb,
            freeRamMb = m.freeRamMb,
            temperatureCelsius = m.temperatureCelsius,
            voltage = m.voltage,
            statusName = m.status.name,
            activePppoeClients = m.activePppoeClients,
            activeHotspotUsers = m.activeHotspotUsers,
            avgLatencyMs = m.avgLatencyMs,
            packetLossPercent = m.packetLossPercent,
            currentRxBps = m.currentRxBps,
            currentTxBps = m.currentTxBps,
            bandwidthUtilizationPercent = m.bandwidthUtilizationPercent,
            lastSeenTimestamp = m.lastSeenTimestamp
        )
    }
}

@Entity(tableName = "interfaces")
data class NetworkInterfaceEntity(
    @PrimaryKey val id: String,
    val routerId: String,
    val name: String,
    val comment: String,
    val typeName: String,
    val linkStatusName: String,
    val linkSpeedMbps: Long,
    val currentRxBps: Long,
    val currentTxBps: Long,
    val rxPacketsPerSec: Long,
    val txPacketsPerSec: Long,
    val mtu: Int,
    val macAddress: String,
    val errorCount: Long,
    val dropCount: Long,
    val isMonitored: Boolean
) {
    fun toDomain(): NetworkInterface = NetworkInterface(
        id = id,
        routerId = routerId,
        name = name,
        comment = comment,
        type = try { InterfaceType.valueOf(typeName) } catch (_: Exception) { InterfaceType.SFP_PLUS },
        linkStatus = try { LinkStatus.valueOf(linkStatusName) } catch (_: Exception) { LinkStatus.UP },
        linkSpeedMbps = linkSpeedMbps,
        currentRxBps = currentRxBps,
        currentTxBps = currentTxBps,
        rxPacketsPerSec = rxPacketsPerSec,
        txPacketsPerSec = txPacketsPerSec,
        mtu = mtu,
        macAddress = macAddress,
        errorCount = errorCount,
        dropCount = dropCount,
        isMonitored = isMonitored
    )

    companion object {
        fun fromDomain(i: NetworkInterface): NetworkInterfaceEntity = NetworkInterfaceEntity(
            id = i.id,
            routerId = i.routerId,
            name = i.name,
            comment = i.comment,
            typeName = i.type.name,
            linkStatusName = i.linkStatus.name,
            linkSpeedMbps = i.linkSpeedMbps,
            currentRxBps = i.currentRxBps,
            currentTxBps = i.currentTxBps,
            rxPacketsPerSec = i.rxPacketsPerSec,
            txPacketsPerSec = i.txPacketsPerSec,
            mtu = i.mtu,
            macAddress = i.macAddress,
            errorCount = i.errorCount,
            dropCount = i.dropCount,
            isMonitored = i.isMonitored
        )
    }
}

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val id: String,
    val targetTypeName: String,
    val targetId: String,
    val targetName: String,
    val severityName: String,
    val title: String,
    val description: String,
    val metricValue: String,
    val thresholdValue: String,
    val timestamp: Long,
    val isAcknowledged: Boolean,
    val deliveryChannels: String = "Email + Webhook",
    val deliveryStatus: String = "Dispatched (SMTP & Webhook 200 OK)"
) {
    fun toDomain(): NetworkAlert = NetworkAlert(
        id = id,
        targetType = try { AlertTargetType.valueOf(targetTypeName) } catch (_: Exception) { AlertTargetType.ONU },
        targetId = targetId,
        targetName = targetName,
        severity = try { AlertSeverity.valueOf(severityName) } catch (_: Exception) { AlertSeverity.WARNING },
        title = title,
        description = description,
        metricValue = metricValue,
        thresholdValue = thresholdValue,
        timestamp = timestamp,
        isAcknowledged = isAcknowledged,
        deliveryChannels = deliveryChannels,
        deliveryStatus = deliveryStatus
    )

    companion object {
        fun fromDomain(a: NetworkAlert): AlertEntity = AlertEntity(
            id = a.id,
            targetTypeName = a.targetType.name,
            targetId = a.targetId,
            targetName = a.targetName,
            severityName = a.severity.name,
            title = a.title,
            description = a.description,
            metricValue = a.metricValue,
            thresholdValue = a.thresholdValue,
            timestamp = a.timestamp,
            isAcknowledged = a.isAcknowledged,
            deliveryChannels = a.deliveryChannels,
            deliveryStatus = a.deliveryStatus
        )
    }
}

@Entity(tableName = "threshold_settings")
data class ThresholdSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val onuNominalPowerDbm: Float = -20.0f,
    val onuNominalToleranceDb: Float = 3.0f,
    val onuRxWarningThresholdDbm: Float,
    val onuRxCriticalThresholdDbm: Float,
    val onuRxHighOverloadDbm: Float = -17.0f,
    val interfaceSaturationPercent: Int,
    val routerCpuAlertPercent: Int,
    val pollingIntervalSeconds: Int,
    val soundAlertEnabled: Boolean,
    val vibrateAlertEnabled: Boolean,
    val emailAlertsEnabled: Boolean = true,
    val alertEmailRecipient: String = "noc-alerts@cybercity.in",
    val webhookAlertsEnabled: Boolean = true,
    val webhookEndpointUrl: String = "https://hooks.cybercity.in/api/v1/noc-webhook"
) {
    fun toDomain(): AlertThresholdSettings = AlertThresholdSettings(
        onuNominalPowerDbm = onuNominalPowerDbm,
        onuNominalToleranceDb = onuNominalToleranceDb,
        onuRxWarningThresholdDbm = onuRxWarningThresholdDbm,
        onuRxCriticalThresholdDbm = onuRxCriticalThresholdDbm,
        onuRxHighOverloadDbm = onuRxHighOverloadDbm,
        interfaceSaturationPercent = interfaceSaturationPercent,
        routerCpuAlertPercent = routerCpuAlertPercent,
        pollingIntervalSeconds = pollingIntervalSeconds,
        soundAlertEnabled = soundAlertEnabled,
        vibrateAlertEnabled = vibrateAlertEnabled,
        emailAlertsEnabled = emailAlertsEnabled,
        alertEmailRecipient = alertEmailRecipient,
        webhookAlertsEnabled = webhookAlertsEnabled,
        webhookEndpointUrl = webhookEndpointUrl
    )

    companion object {
        fun fromDomain(s: AlertThresholdSettings): ThresholdSettingsEntity = ThresholdSettingsEntity(
            id = 1,
            onuNominalPowerDbm = s.onuNominalPowerDbm,
            onuNominalToleranceDb = s.onuNominalToleranceDb,
            onuRxWarningThresholdDbm = s.onuRxWarningThresholdDbm,
            onuRxCriticalThresholdDbm = s.onuRxCriticalThresholdDbm,
            onuRxHighOverloadDbm = s.onuRxHighOverloadDbm,
            interfaceSaturationPercent = s.interfaceSaturationPercent,
            routerCpuAlertPercent = s.routerCpuAlertPercent,
            pollingIntervalSeconds = s.pollingIntervalSeconds,
            soundAlertEnabled = s.soundAlertEnabled,
            vibrateAlertEnabled = s.vibrateAlertEnabled,
            emailAlertsEnabled = s.emailAlertsEnabled,
            alertEmailRecipient = s.alertEmailRecipient,
            webhookAlertsEnabled = s.webhookAlertsEnabled,
            webhookEndpointUrl = s.webhookEndpointUrl
        )
    }
}

@Entity(tableName = "isp_portals")
data class IspPortalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val shortName: String,
    val url: String,
    val username: String,
    val password: String,
    val partnerCode: String = "",
    val notes: String = "",
    val category: String = "FMS",
    val primaryColorHex: Long = 0xFF1E40AF,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(): IspPortal = IspPortal(
        id = id,
        name = name,
        shortName = shortName,
        url = url,
        username = username,
        password = password,
        partnerCode = partnerCode,
        notes = notes,
        category = category,
        primaryColorHex = primaryColorHex
    )

    companion object {
        fun fromDomain(p: IspPortal): IspPortalEntity = IspPortalEntity(
            id = p.id,
            name = p.name,
            shortName = p.shortName,
            url = p.url,
            username = p.username,
            password = p.password,
            partnerCode = p.partnerCode,
            notes = p.notes,
            category = p.category,
            primaryColorHex = p.primaryColorHex,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
    }
}
