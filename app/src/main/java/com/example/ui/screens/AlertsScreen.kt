package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AlertSeverity
import com.example.model.AlertThresholdSettings
import com.example.ui.NocViewModel
import com.example.ui.theme.*

@Composable
fun AlertsScreen(
    viewModel: NocViewModel,
    modifier: Modifier = Modifier
) {
    val alerts by viewModel.alerts.collectAsState()
    val thresholds by viewModel.thresholds.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var selectedFilter by remember { mutableStateOf<AlertSeverity?>(null) }
    var showOnlyUnacked by remember { mutableStateOf(false) }

    val filteredAlerts = remember(alerts, selectedFilter, showOnlyUnacked) {
        alerts.filter { alert ->
            val matchSeverity = selectedFilter == null || alert.severity == selectedFilter
            val matchAck = !showOnlyUnacked || !alert.isAcknowledged
            matchSeverity && matchAck
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AUTOMATED ALERT ENGINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Active Incident Feed",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.setThresholdDialogOpen(true) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("threshold_settings_button")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rules & Channels")
                    }

                    if (alerts.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.clearAllAlerts() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }
        }

        // Active Alert Rules & Email / Webhook Channels Summary Card
        item {
            val highLimit = thresholds.onuNominalPowerDbm + thresholds.onuNominalToleranceDb
            val lowLimit = thresholds.onuNominalPowerDbm - thresholds.onuNominalToleranceDb
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alert_automation_rules_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Automated Alert Triggers & Dispatch Channels",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Button(
                            onClick = { viewModel.sendTestEmailAndWebhookAlert() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("test_email_webhook_btn")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Dispatch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Device Reachability: Alert when any OLT or MikroTik becomes unreachable\n" +
                                "• Interface State: Alert when any monitored interface changes to DOWN\n" +
                                "• ONU Power (±${thresholds.onuNominalToleranceDb} dBm from ${thresholds.onuNominalPowerDbm} dBm): High > ${String.format("%.1f", highLimit)} dBm | Low < ${String.format("%.1f", lowLimit)} dBm | Crit < ${thresholds.onuRxCriticalThresholdDbm} dBm",
                        style = MaterialTheme.typography.bodySmall,
                        color = NocTextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("EMAIL NOTIFICATION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NocTextSecondary)
                            Text(
                                text = if (thresholds.emailAlertsEnabled) thresholds.alertEmailRecipient else "Disabled",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = if (thresholds.emailAlertsEnabled) NocSuccessGreen else NocTextMuted
                            )
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("WEBHOOK ENDPOINT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NocTextSecondary)
                            Text(
                                text = if (thresholds.webhookAlertsEnabled) thresholds.webhookEndpointUrl.take(28) + "..." else "Disabled",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = if (thresholds.webhookAlertsEnabled) MaterialTheme.colorScheme.primary else NocTextMuted
                            )
                        }
                    }
                }
            }
        }

        // Filters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null && !showOnlyUnacked,
                    onClick = {
                        selectedFilter = null
                        showOnlyUnacked = false
                    },
                    label = { Text("All (${alerts.size})") }
                )
                FilterChip(
                    selected = showOnlyUnacked,
                    onClick = { showOnlyUnacked = !showOnlyUnacked },
                    label = { Text("Unacked (${alerts.count { !it.isAcknowledged }})") }
                )
                FilterChip(
                    selected = selectedFilter == AlertSeverity.CRITICAL,
                    onClick = { selectedFilter = if (selectedFilter == AlertSeverity.CRITICAL) null else AlertSeverity.CRITICAL },
                    label = { Text("Critical") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFE4E6),
                        selectedLabelColor = NocCriticalRed
                    )
                )
                FilterChip(
                    selected = selectedFilter == AlertSeverity.WARNING,
                    onClick = { selectedFilter = if (selectedFilter == AlertSeverity.WARNING) null else AlertSeverity.WARNING },
                    label = { Text("Warning") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFEF3C7),
                        selectedLabelColor = NocWarningAmber
                    )
                )
            }
        }

        // Alert Items
        if (filteredAlerts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NocSuccessGreen, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No active alerts matching criteria", color = NocTextSecondary)
                    }
                }
            }
        } else {
            items(filteredAlerts) { alert ->
                AlertCardItem(
                    alert = alert,
                    onAcknowledge = { viewModel.acknowledgeAlert(alert.id) }
                )
            }
        }
    }

    if (uiState.isThresholdDialogOpen) {
        ThresholdSettingsDialog(
            current = thresholds,
            onDismiss = { viewModel.setThresholdDialogOpen(false) },
            onSave = {
                viewModel.updateThresholds(it)
                viewModel.setThresholdDialogOpen(false)
            }
        )
    }
}

@Composable
fun ThresholdSettingsDialog(
    current: AlertThresholdSettings,
    onDismiss: () -> Unit,
    onSave: (AlertThresholdSettings) -> Unit
) {
    var nominalDbm by remember { mutableStateOf(current.onuNominalPowerDbm.toString()) }
    var toleranceDb by remember { mutableStateOf(current.onuNominalToleranceDb.toString()) }
    var rxCritical by remember { mutableStateOf(current.onuRxCriticalThresholdDbm.toString()) }
    var intfSaturation by remember { mutableStateOf(current.interfaceSaturationPercent.toString()) }
    var emailEnabled by remember { mutableStateOf(current.emailAlertsEnabled) }
    var emailRecipient by remember { mutableStateOf(current.alertEmailRecipient) }
    var webhookEnabled by remember { mutableStateOf(current.webhookAlertsEnabled) }
    var webhookUrl by remember { mutableStateOf(current.webhookEndpointUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Alert Thresholds & Dispatch", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 420.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = nominalDbm,
                        onValueChange = { nominalDbm = it },
                        label = { Text("ONU Nominal Rx Power (dBm, e.g. -20.0)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = toleranceDb,
                        onValueChange = { toleranceDb = it },
                        label = { Text("Allowed Range (+/- dBm, default 3.0)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = rxCritical,
                        onValueChange = { rxCritical = it },
                        label = { Text("ONU Rx Critical Threshold (dBm)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = intfSaturation,
                        onValueChange = { intfSaturation = it },
                        label = { Text("Interface Saturation Alert (%)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Email Notifications", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Switch(checked = emailEnabled, onCheckedChange = { emailEnabled = it })
                    }
                }
                if (emailEnabled) {
                    item {
                        OutlinedTextField(
                            value = emailRecipient,
                            onValueChange = { emailRecipient = it },
                            label = { Text("Alert Email Recipient") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Webhook POST Alerts", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Switch(checked = webhookEnabled, onCheckedChange = { webhookEnabled = it })
                    }
                }
                if (webhookEnabled) {
                    item {
                        OutlinedTextField(
                            value = webhookUrl,
                            onValueChange = { webhookUrl = it },
                            label = { Text("Webhook Endpoint URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val nom = nominalDbm.toFloatOrNull() ?: current.onuNominalPowerDbm
                    val tol = toleranceDb.toFloatOrNull() ?: current.onuNominalToleranceDb
                    val updated = current.copy(
                        onuNominalPowerDbm = nom,
                        onuNominalToleranceDb = tol,
                        onuRxWarningThresholdDbm = nom - tol,
                        onuRxHighOverloadDbm = nom + tol,
                        onuRxCriticalThresholdDbm = rxCritical.toFloatOrNull() ?: current.onuRxCriticalThresholdDbm,
                        interfaceSaturationPercent = intfSaturation.toIntOrNull() ?: current.interfaceSaturationPercent,
                        emailAlertsEnabled = emailEnabled,
                        alertEmailRecipient = emailRecipient.trim(),
                        webhookAlertsEnabled = webhookEnabled,
                        webhookEndpointUrl = webhookUrl.trim()
                    )
                    onSave(updated)
                }
            ) {
                Text("Save Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
