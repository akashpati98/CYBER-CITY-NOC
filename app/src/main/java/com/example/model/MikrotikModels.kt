package com.example.model

enum class InterfaceType(val label: String) {
    SFP_PLUS("SFP+ 10G"),
    SFP("SFP 1G"),
    ETHERNET("Ethernet 1G"),
    BRIDGE("Bridge"),
    VLAN("VLAN"),
    BONDING("Bonding"),
    WIRELESS("WLAN")
}

enum class LinkStatus(val label: String) {
    UP("Connected"),
    DOWN("Link Down"),
    DISABLED("Disabled")
}

data class NetworkInterface(
    val id: String,
    val routerId: String,
    val name: String, // e.g. "sfp-sfpplus1-Uplink"
    val comment: String = "",
    val type: InterfaceType,
    val linkStatus: LinkStatus = LinkStatus.UP,
    val linkSpeedMbps: Long = 10000, // 10 Gbps
    val currentRxBps: Long = 0,
    val currentTxBps: Long = 0,
    val rxPacketsPerSec: Long = 0,
    val txPacketsPerSec: Long = 0,
    val mtu: Int = 1500,
    val macAddress: String = "48:A9:8A:2F:11:4A",
    val errorCount: Long = 0,
    val dropCount: Long = 0,
    val isMonitored: Boolean = true
)

data class TrafficPoint(
    val timestamp: Long,
    val rxBps: Long,
    val txBps: Long
)

data class HourlyTrafficPoint(
    val hourIndex: Int,
    val hourLabel: String, // e.g. "14:00"
    val timestamp: Long,
    val inBps: Long,       // RX / Download throughput
    val outBps: Long,      // TX / Upload throughput
    val peakInBps: Long,
    val peakOutBps: Long
)

data class MikrotikDevice(
    val id: String,
    val name: String,
    val ipAddress: String,
    val restPort: Int = 443,
    val apiPort: Int = 8728,
    val username: String = "admin",
    val model: String = "CCR2004-1G-12S+2XS",
    val routerOsVersion: String = "v7.16.1",
    val uptimeString: String = "48d 14h 22m",
    val cpuUsage: Int = 14, // %
    val totalRamMb: Long = 4096,
    val freeRamMb: Long = 3120,
    val temperatureCelsius: Float = 42.0f,
    val voltage: Float = 24.1f,
    val status: DeviceStatus = DeviceStatus.ONLINE,
    val activePppoeClients: Int = 840,
    val activeHotspotUsers: Int = 112,
    val avgLatencyMs: Float = 1.8f,
    val packetLossPercent: Float = 0.0f,
    val currentRxBps: Long = 4_850_000_000L,
    val currentTxBps: Long = 1_920_000_000L,
    val bandwidthUtilizationPercent: Float = 48.5f,
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

    fun getLayerQuality(layer: MapVisualizationLayer): NodeHealthQuality {
        if (status == DeviceStatus.OFFLINE) return NodeHealthQuality.OFFLINE
        return when (layer) {
            MapVisualizationLayer.LATENCY_AND_PACKET_LOSS -> {
                val latQ = getLatencyQuality()
                val lossQ = getPacketLossQuality()
                when {
                    latQ == NodeHealthQuality.CRITICAL || lossQ == NodeHealthQuality.CRITICAL -> NodeHealthQuality.CRITICAL
                    latQ == NodeHealthQuality.WARNING || lossQ == NodeHealthQuality.WARNING -> NodeHealthQuality.WARNING
                    else -> NodeHealthQuality.OPTIMAL
                }
            }
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
