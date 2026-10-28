package com.example.model

enum class AlertSeverity(val label: String) {
    CRITICAL("Critical"),
    WARNING("Warning"),
    INFO("Info")
}

enum class AlertTargetType {
    OLT,
    ONU,
    MIKROTIK,
    INTERFACE
}

data class NetworkAlert(
    val id: String,
    val targetType: AlertTargetType,
    val targetId: String,
    val targetName: String,
    val severity: AlertSeverity,
    val title: String,
    val description: String,
    val metricValue: String,
    val thresholdValue: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isAcknowledged: Boolean = false,
    val deliveryChannels: String = "Email + Webhook",
    val deliveryStatus: String = "Dispatched (SMTP & Webhook 200 OK)"
)

data class AlertThresholdSettings(
    val onuNominalPowerDbm: Float = -20.0f,
    val onuNominalToleranceDb: Float = 3.0f, // +/- 3 dBm recommended range (-17.0 to -23.0 dBm)
    val onuRxWarningThresholdDbm: Float = -23.0f,
    val onuRxCriticalThresholdDbm: Float = -27.5f,
    val onuRxHighOverloadDbm: Float = -17.0f,
    val interfaceSaturationPercent: Int = 85,
    val routerCpuAlertPercent: Int = 80,
    val pollingIntervalSeconds: Int = 5, // <= 15 seconds real-time update interval
    val soundAlertEnabled: Boolean = true,
    val vibrateAlertEnabled: Boolean = true,
    val emailAlertsEnabled: Boolean = true,
    val alertEmailRecipient: String = "noc-alerts@cybercity.in",
    val webhookAlertsEnabled: Boolean = true,
    val webhookEndpointUrl: String = "https://hooks.cybercity.in/api/v1/noc-webhook"
)
