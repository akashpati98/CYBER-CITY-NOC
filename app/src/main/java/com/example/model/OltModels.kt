package com.example.model

import kotlin.math.abs

enum class OltVendor(val displayName: String, val defaultPort: Int) {
    HUAWEI("Huawei", 23),
    ZTE("ZTE", 23),
    VSOL("V-SOL", 80),
    BDCOM("BDCOM", 23),
    FIBERHOME("Fiberhome", 23)
}

enum class DeviceStatus(val label: String) {
    ONLINE("Online"),
    WARNING("Warning"),
    CRITICAL("Critical"),
    OFFLINE("Offline")
}

enum class MapVisualizationLayer(val label: String, val shortLabel: String) {
    LATENCY_AND_PACKET_LOSS("Latency & Packet Loss (Live)", "Latency + Loss"),
    OPTICAL_POWER("Optical Power (dBm)", "Optical Power"),
    AVG_LATENCY("Average Latency (ms)", "Avg Latency"),
    PACKET_LOSS("Packet Loss (%)", "Packet Loss")
}

enum class NodeHealthQuality(val label: String) {
    OPTIMAL("Optimal"),
    WARNING("Elevated / Warning"),
    CRITICAL("Critical / Degraded"),
    OFFLINE("Offline / Unreachable")
}

data class OltDevice(
    val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int = 23,
    val vendor: OltVendor,
    val model: String,
    val totalSlots: Int = 2,
    val totalPonPorts: Int = 16,
    val status: DeviceStatus = DeviceStatus.ONLINE,
    val cpuUsage: Int = 18,
    val memoryUsage: Int = 42,
    val temperatureCelsius: Float = 44.5f,
    val snmpCommunity: String = "public",
    val location: String = "Main Datacenter Rack A-04",
    val latitude: Double = 37.7750,
    val longitude: Double = -122.4180,
    val avgLatencyMs: Float = 2.8f,
    val packetLossPercent: Float = 0.0f,
    val currentRxBps: Long = 2_840_000_000L,
    val currentTxBps: Long = 3_450_000_000L,
    val uplinkCapacityMbps: Long = 10000L,
    val bandwidthUtilizationPercent: Float = 34.5f,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
) {
    fun getLatencyQuality(): NodeHealthQuality {
        return when {
            status == DeviceStatus.OFFLINE -> NodeHealthQuality.OFFLINE
            avgLatencyMs <= 15.0f -> NodeHealthQuality.OPTIMAL
            avgLatencyMs <= 45.0f -> NodeHealthQuality.WARNING
            else -> NodeHealthQuality.CRITICAL
        }
    }

    fun getPacketLossQuality(): NodeHealthQuality {
        return when {
            status == DeviceStatus.OFFLINE -> NodeHealthQuality.OFFLINE
            packetLossPercent < 0.5f -> NodeHealthQuality.OPTIMAL
            packetLossPercent <= 2.5f -> NodeHealthQuality.WARNING
            else -> NodeHealthQuality.CRITICAL
        }
    }

    fun getCombinedLatencyLossQuality(): NodeHealthQuality {
        if (status == DeviceStatus.OFFLINE) return NodeHealthQuality.OFFLINE
        val latQ = getLatencyQuality()
        val lossQ = getPacketLossQuality()
        return when {
            latQ == NodeHealthQuality.CRITICAL || lossQ == NodeHealthQuality.CRITICAL -> NodeHealthQuality.CRITICAL
            latQ == NodeHealthQuality.WARNING || lossQ == NodeHealthQuality.WARNING -> NodeHealthQuality.WARNING
            else -> NodeHealthQuality.OPTIMAL
        }
    }

    fun getLayerQuality(layer: MapVisualizationLayer): NodeHealthQuality {
        return when (layer) {
            MapVisualizationLayer.LATENCY_AND_PACKET_LOSS -> getCombinedLatencyLossQuality()
            MapVisualizationLayer.AVG_LATENCY -> getLatencyQuality()
            MapVisualizationLayer.PACKET_LOSS -> getPacketLossQuality()
            MapVisualizationLayer.OPTICAL_POWER -> when (status) {
                DeviceStatus.ONLINE -> NodeHealthQuality.OPTIMAL
                DeviceStatus.WARNING -> NodeHealthQuality.WARNING
                DeviceStatus.CRITICAL -> NodeHealthQuality.CRITICAL
                DeviceStatus.OFFLINE -> NodeHealthQuality.OFFLINE
            }
        }
    }
}

enum class OnuStatus(val label: String) {
    ONLINE("Online"),
    WARNING("Low/High Optical"),
    CRITICAL("Crit Optical"),
    OFFLINE("Offline / LOS"),
    DYING_GASP("Dying Gasp")
}

data class PonPort(
    val id: String,
    val oltId: String,
    val name: String,
    val totalOnus: Int,
    val onlineOnus: Int,
    val txPowerDbm: Float = 4.2f,
    val adminState: Boolean = true,
    val operState: Boolean = true
)

data class OnuPowerHistoryPoint(
    val id: String,
    val onuId: String,
    val timestamp: Long,
    val rxPowerDbm: Float,
    val txPowerDbm: Float,
    val nominalPowerDbm: Float = -20.0f
)

data class OnuDevice(
    val id: String,
    val oltId: String,
    val ponPort: String,
    val onuIndex: Int,
    val customerName: String,
    val pppoeAccount: String,
    val serialNumber: String,
    val rxPowerDbm: Float,
    val txPowerDbm: Float = 2.1f,
    val oltRxPowerDbm: Float = -21.8f,
    val nominalPowerDbm: Float = -20.0f,
    val distanceMeters: Int = 1250,
    val status: OnuStatus = OnuStatus.ONLINE,
    val temperatureCelsius: Float = 38.2f,
    val voltageVolts: Float = 3.28f,
    val biasCurrentMa: Float = 14.5f,
    val planSpeed: String = "100 Mbps",
    val address: String = "Cyber City Central",
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194,
    val avgLatencyMs: Float = 4.2f,
    val packetLossPercent: Float = 0.0f,
    val lastOfflineReason: String? = null,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
) {
    fun getNominalDeviationDb(): Float = rxPowerDbm - nominalPowerDbm

    fun isOutsideNominalRange(toleranceDb: Float = 3.0f): Boolean {
        if (status == OnuStatus.OFFLINE || status == OnuStatus.DYING_GASP) return true
        return abs(rxPowerDbm - nominalPowerDbm) > toleranceDb
    }

    fun isHighPowerOverload(toleranceDb: Float = 3.0f): Boolean {
        if (status == OnuStatus.OFFLINE || status == OnuStatus.DYING_GASP) return false
        return rxPowerDbm > (nominalPowerDbm + toleranceDb)
    }

    fun isLowPowerDeviation(toleranceDb: Float = 3.0f): Boolean {
        if (status == OnuStatus.OFFLINE || status == OnuStatus.DYING_GASP) return true
        return rxPowerDbm < (nominalPowerDbm - toleranceDb)
    }

    fun getPowerQuality(): OpticalQuality {
        return when {
            status == OnuStatus.OFFLINE || status == OnuStatus.DYING_GASP -> OpticalQuality.LOS
            rxPowerDbm > nominalPowerDbm + 3.0f -> OpticalQuality.WARNING // High optical overload (> -17 dBm)
            rxPowerDbm >= -23.0f -> OpticalQuality.EXCELLENT // Within +/- 3 dBm of -20 dBm nominal
            rxPowerDbm >= -25.5f -> OpticalQuality.GOOD
            rxPowerDbm >= -27.5f -> OpticalQuality.WARNING
            else -> OpticalQuality.CRITICAL
        }
    }

    fun getLatencyQuality(): NodeHealthQuality {
        return when {
            status == OnuStatus.OFFLINE || status == OnuStatus.DYING_GASP -> NodeHealthQuality.OFFLINE
            avgLatencyMs <= 15.0f -> NodeHealthQuality.OPTIMAL
            avgLatencyMs <= 45.0f -> NodeHealthQuality.WARNING
            else -> NodeHealthQuality.CRITICAL
        }
    }

    fun getPacketLossQuality(): NodeHealthQuality {
        return when {
            status == OnuStatus.OFFLINE || status == OnuStatus.DYING_GASP -> NodeHealthQuality.OFFLINE
            packetLossPercent < 0.5f -> NodeHealthQuality.OPTIMAL
            packetLossPercent <= 2.5f -> NodeHealthQuality.WARNING
            else -> NodeHealthQuality.CRITICAL
        }
    }

    fun getCombinedLatencyLossQuality(): NodeHealthQuality {
        if (status == OnuStatus.OFFLINE || status == OnuStatus.DYING_GASP) return NodeHealthQuality.OFFLINE
        val latQ = getLatencyQuality()
        val lossQ = getPacketLossQuality()
        return when {
            latQ == NodeHealthQuality.CRITICAL || lossQ == NodeHealthQuality.CRITICAL -> NodeHealthQuality.CRITICAL
            latQ == NodeHealthQuality.WARNING || lossQ == NodeHealthQuality.WARNING -> NodeHealthQuality.WARNING
            else -> NodeHealthQuality.OPTIMAL
        }
    }

    fun getLayerQuality(layer: MapVisualizationLayer): NodeHealthQuality {
        return when (layer) {
            MapVisualizationLayer.LATENCY_AND_PACKET_LOSS -> getCombinedLatencyLossQuality()
            MapVisualizationLayer.OPTICAL_POWER -> when (getPowerQuality()) {
                OpticalQuality.EXCELLENT, OpticalQuality.GOOD -> NodeHealthQuality.OPTIMAL
                OpticalQuality.WARNING -> NodeHealthQuality.WARNING
                OpticalQuality.CRITICAL -> NodeHealthQuality.CRITICAL
                OpticalQuality.LOS -> NodeHealthQuality.OFFLINE
            }
            MapVisualizationLayer.AVG_LATENCY -> getLatencyQuality()
            MapVisualizationLayer.PACKET_LOSS -> getPacketLossQuality()
        }
    }
}

enum class OpticalQuality(val label: String) {
    EXCELLENT("Nominal (-17 to -23 dBm)"),
    GOOD("Good (-23 to -25.5 dBm)"),
    WARNING("Out of Range (+/-3 dBm)"),
    CRITICAL("Critical (< -27.5 dBm)"),
    LOS("Loss of Signal (LOS)")
}

// V-SOL Integration Ecosystem Models
data class VsolUnregisteredOnu(
    val id: String,
    val oltId: String,
    val ponPort: String,
    val serialNumber: String,
    val model: String,
    val discoveredTimestamp: Long = System.currentTimeMillis()
)

data class VsolBatchAuditResult(
    val totalOnus: Int,
    val optimalCount: Int,
    val warningCount: Int,
    val criticalCount: Int,
    val averageRxDbm: Float,
    val scanTimestamp: Long = System.currentTimeMillis()
)
