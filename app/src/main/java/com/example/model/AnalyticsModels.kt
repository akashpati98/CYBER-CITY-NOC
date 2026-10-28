package com.example.model

enum class DiscoveredDeviceType(val label: String) {
    OLT("OLT Optical Chassis"),
    MIKROTIK("MikroTik RouterOS")
}

data class DiscoveredNetworkDevice(
    val id: String,
    val ipAddress: String,
    val deviceType: DiscoveredDeviceType,
    val hostname: String,
    val vendorOrModel: String,
    val openPort: Int,
    val latencyMs: Long,
    val protocolSignature: String = "TCP/SNMP Probe",
    val macAddress: String = "00:0C:42:FA:18:01",
    val discoveredTimestamp: Long = System.currentTimeMillis(),
    val autoAddedToMonitoring: Boolean = true
)

data class IpRangeScanState(
    val isScanning: Boolean = false,
    val isServiceRunning: Boolean = true,
    val subnetRange: String = "10.100.1.1-254",
    val currentProbingIp: String = "",
    val scannedHosts: Int = 0,
    val totalHosts: Int = 254,
    val discoveredDevices: List<DiscoveredNetworkDevice> = emptyList(),
    val activeOltsFoundCount: Int = 0,
    val activeMikrotiksFoundCount: Int = 0,
    val lastScanTimestamp: Long = 0L,
    val lastScanDurationMs: Long = 0L,
    val autoBackgroundScanEnabled: Boolean = true,
    val scanIntervalSeconds: Int = 45,
    val scanCycleCount: Int = 0,
    val probedPortsSummary: String = "OLT: 23/80/161/443 • MikroTik: 8728/8291/443",
    val statusMessage: String = "Automated background IP discovery active"
)

data class NocComputedAnalytics(
    val totalRxBps: Long = 0L,
    val totalTxBps: Long = 0L,
    val totalBandwidthCapacityBps: Long = 0L,
    val networkSaturationPercent: Float = 0f,
    val totalOlts: Int = 0,
    val onlineOlts: Int = 0,
    val totalOnus: Int = 0,
    val onlineOnus: Int = 0,
    val warningOnus: Int = 0,
    val criticalOnus: Int = 0,
    val losOnus: Int = 0,
    val averageOpticalRxDbm: Float = -22.5f,
    val avgNetworkLatencyMs: Float = 5.4f,
    val avgPacketLossPercent: Float = 0.15f,
    val totalPppoeClients: Int = 0,
    val totalHotspotUsers: Int = 0,
    val highestSaturationInterfaceName: String = "sfp-sfpplus1-Transit-Tata",
    val highestSaturationPercent: Float = 0f,
    val hottestDeviceName: String = "HQ-Core-CCR2004",
    val hottestTemperatureCelsius: Float = 48.5f,
    val lastFetchedTimestamp: Long = System.currentTimeMillis(),
    val isFetching: Boolean = false
)
