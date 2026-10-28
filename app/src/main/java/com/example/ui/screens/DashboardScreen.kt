package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.NocNavTab
import com.example.ui.NocViewModel
import com.example.ui.components.Formatters
import com.example.ui.components.Historical24hTrafficWidget
import com.example.ui.components.LiveTrafficChart
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    viewModel: NocViewModel,
    modifier: Modifier = Modifier
) {
    val olts by viewModel.olts.collectAsState()
    val mikrotiks by viewModel.mikrotiks.collectAsState()
    val onus by viewModel.onus.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val trafficHistory by viewModel.trafficHistory.collectAsState()
    val historical24hTraffic by viewModel.historical24hTraffic.collectAsState()
    val selected24hInterfaceId by viewModel.selected24hInterfaceId.collectAsState()
    val ipScanState by viewModel.ipScanState.collectAsState()
    val interfaces by viewModel.interfaces.collectAsState()
    val analytics by viewModel.computedAnalytics.collectAsState()

    val totalOnus = onus.size
    val onlineOnus = onus.count { it.status == OnuStatus.ONLINE }
    val warningOnus = onus.count { it.status == OnuStatus.WARNING }
    val criticalOnus = onus.count { it.status == OnuStatus.CRITICAL || it.status == OnuStatus.OFFLINE }
    val outOfNominalOnus = onus.count { it.isOutsideNominalRange(3.0f) }

    val unackedAlerts = alerts.filter { !it.isAcknowledged }
    val criticalAlertsCount = unackedAlerts.count { it.severity == AlertSeverity.CRITICAL }

    val totalRxBps = interfaces.filter { it.linkStatus == LinkStatus.UP }.sumOf { it.currentRxBps }
    val totalTxBps = interfaces.filter { it.linkStatus == LinkStatus.UP }.sumOf { it.currentTxBps }
    val totalPppoe = mikrotiks.sumOf { it.activePppoeClients }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // NOC Header Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_header_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (criticalAlertsCount > 0) NocCriticalRed else NocSuccessGreen)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (criticalAlertsCount > 0) "NOC ATTENTION REQUIRED" else "SYSTEMS OPERATIONAL",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (criticalAlertsCount > 0) NocCriticalRed else NocSuccessGreen,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                text = "ISP Network Operations Center",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Live Auto-Refresh: Every 3s (< 15s SLA) • Email & Webhook Active",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = NocTextSecondary
                            )
                        }

                        // Simulation test trigger menu button
                        OutlinedButton(
                            onClick = { viewModel.restoreNormalNetwork() },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("reset_simulation_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset NOC", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Simulation scenario quick buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { viewModel.triggerFiberCut("0/1/3") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE4E6), contentColor = NocCriticalRed),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sim_fiber_cut_button")
                        ) {
                            Text("Fiber Cut", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.triggerTrafficSpike() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0F2FE), contentColor = NocPrimaryBlue),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sim_traffic_spike_button")
                        ) {
                            Text("Traffic Spike", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.triggerOpticalDegradation("onu-102") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF3C7), contentColor = NocWarningAmber),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sim_degrade_onu_button")
                        ) {
                            Text("Low Rx (-3dB)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.triggerHighOpticalOverload("onu-101") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3E8FF), contentColor = Color(0xFF7E22CE)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sim_high_power_onu_button")
                        ) {
                            Text("High Rx (+3dB)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Direct Quick Launch Buttons for Live Map & ISP FMS Portals
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.selectTab(NocNavTab.LIVE_MAP) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_open_live_map_btn")
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Live GIS Map", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.selectTab(NocNavTab.ISP_PORTALS) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_open_isp_fms_btn")
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open ISP FMS Portals", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { viewModel.selectTab(NocNavTab.TOOLS) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_open_diagnostics_btn")
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "IP Gateway • Speed Test • Traceroute • Latency • DNS Lookup",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Automated Background IP Range Discovery Service Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ip_discovery_service_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFCCFBF1)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Radar,
                                    contentDescription = "IP Range Discovery Service",
                                    tint = Color(0xFF0F766E),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Automated IP Range Discovery Service",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = ipScanState.statusMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = NocTextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = ipScanState.autoBackgroundScanEnabled,
                            onCheckedChange = { viewModel.toggleAutoBackgroundScan(it) },
                            modifier = Modifier.testTag("toggle_auto_ip_scan_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Service Telemetry & Signature Ports Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (ipScanState.isServiceRunning && ipScanState.autoBackgroundScanEnabled)
                                            NocSuccessGreen
                                        else NocWarningAmber
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (ipScanState.isServiceRunning && ipScanState.autoBackgroundScanEnabled)
                                    "BG SERVICE ACTIVE (${ipScanState.scanIntervalSeconds}s) • Cycle #${ipScanState.scanCycleCount}"
                                else "BG SERVICE PAUSED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (ipScanState.isServiceRunning && ipScanState.autoBackgroundScanEnabled)
                                    NocSuccessGreen
                                else NocWarningAmber
                            )
                        }
                        Text(
                            text = ipScanState.probedPortsSummary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NocTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Subnet Presets & Auto-Detect LAN Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.setSubnetRange("10.100.1.1-254") },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .testTag("preset_olt_subnet_btn")
                        ) {
                            Text("10.100.1.1-254", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }

                        OutlinedButton(
                            onClick = { viewModel.setSubnetRange("192.168.88.0/24") },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .testTag("preset_mikrotik_subnet_btn")
                        ) {
                            Text("192.168.88.0/24", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }

                        Button(
                            onClick = { viewModel.detectLocalSubnetAndScan() },
                            enabled = !ipScanState.isScanning,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F766E),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .testTag("detect_local_subnet_btn")
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Auto LAN", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = ipScanState.subnetRange,
                            onValueChange = { viewModel.setSubnetRange(it) },
                            label = { Text("IP Range / CIDR Subnet") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ip_discovery_range_input")
                        )

                        Button(
                            onClick = { viewModel.runAutomatedIpRangeScan() },
                            enabled = !ipScanState.isScanning,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("run_ip_discovery_scan_button")
                        ) {
                            if (ipScanState.isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("${ipScanState.scannedHosts}/${ipScanState.totalHosts}", fontSize = 11.sp)
                            } else {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scan Subnet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (ipScanState.isScanning) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val progressFraction = if (ipScanState.totalHosts > 0) {
                            (ipScanState.scannedHosts.toFloat() / ipScanState.totalHosts.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        if (ipScanState.currentProbingIp.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Probing ${ipScanState.currentProbingIp} on OLT (23/80/161) & MikroTik (8728/8291)...",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (ipScanState.discoveredDevices.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ACTIVE DEVICES DISCOVERED (${ipScanState.activeOltsFoundCount} OLTs • ${ipScanState.activeMikrotiksFoundCount} MikroTiks)",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (ipScanState.lastScanDurationMs > 0L) {
                                Text(
                                    text = "Sweep: ${ipScanState.lastScanDurationMs}ms",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = NocTextMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        ipScanState.discoveredDevices.take(4).forEach { disc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable {
                                        if (disc.deviceType == DiscoveredDeviceType.OLT) {
                                            viewModel.selectTab(NocNavTab.OLT_ONU)
                                        } else {
                                            viewModel.selectTab(NocNavTab.MIKROTIK)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${disc.hostname} (${disc.ipAddress}:${disc.openPort})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "${disc.deviceType.label} • ${disc.vendorOrModel}",
                                        fontSize = 10.sp,
                                        color = NocTextSecondary
                                    )
                                    Text(
                                        text = "${disc.protocolSignature} • MAC ${disc.macAddress}",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = NocTextMuted
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "ONLINE (${disc.latencyMs}ms)",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = NocSuccessGreen
                                    )
                                    Text(
                                        text = "Monitored →",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Multi-Device Fetch & In-App Calculation Engine Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_calculation_engine_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Memory,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "In-App Telemetry & Computation",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Fetched from every OLT, ONT & MikroTik",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NocTextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.fetchAndCalculateAllTelemetry() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("fetch_all_devices_btn")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fetch All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Computed Analytics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("AVERAGE OPTICAL RX", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NocTextSecondary)
                                Text(
                                    text = "${String.format("%.1f", analytics.averageOpticalRxDbm)} dBm",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (analytics.averageOpticalRxDbm >= -23.0f) NocSuccessGreen else NocWarningAmber
                                )
                                Text("$outOfNominalOnus outside ±3 dBm", fontSize = 9.sp, color = NocTextMuted)
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("AVG LATENCY & LOSS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NocTextSecondary)
                                Text(
                                    text = "${String.format("%.1f", analytics.avgNetworkLatencyMs)} ms",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (analytics.avgNetworkLatencyMs <= 15f) NocSuccessGreen else NocWarningAmber
                                )
                                Text("Loss: ${String.format("%.2f", analytics.avgPacketLossPercent)}%", fontSize = 9.sp, color = NocTextMuted)
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("TRUNK SATURATION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NocTextSecondary)
                                Text(
                                    text = "${String.format("%.1f", analytics.networkSaturationPercent)}%",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (analytics.networkSaturationPercent > 80f) NocCriticalRed else NocPrimaryBlue
                                )
                                Text("Max: ${analytics.highestSaturationInterfaceName.take(10)}...", fontSize = 9.sp, color = NocTextMuted)
                            }
                        }
                    }
                }
            }
        }

        // Key KPI Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "TOTAL OLTS",
                        mainValue = "${olts.size} Units",
                        subText = "${olts.sumOf { it.totalPonPorts }} Active PONs",
                        icon = Icons.Default.Hub,
                        accentColor = NocPrimaryBlue,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.selectTab(NocNavTab.OLT_ONU) }
                    )
                    KpiCard(
                        title = "MIKROTIK ROUTERS",
                        mainValue = "${mikrotiks.size} Gateways",
                        subText = "$totalPppoe PPPoE sessions",
                        icon = Icons.Default.Router,
                        accentColor = NocSecondaryTeal,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.selectTab(NocNavTab.MIKROTIK) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "ONU POWER (±3 dBm)",
                        mainValue = "$onlineOnus / $totalOnus",
                        subText = "$outOfNominalOnus Out of ±3dB | $criticalOnus Crit",
                        icon = Icons.Default.Lightbulb,
                        accentColor = if (criticalOnus > 0) NocCriticalRed else NocSuccessGreen,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.selectTab(NocNavTab.OLT_ONU) }
                    )
                    KpiCard(
                        title = "ACTIVE ALERTS",
                        mainValue = "${unackedAlerts.size} Events",
                        subText = "$criticalAlertsCount Critical alerts",
                        icon = Icons.Default.NotificationsActive,
                        accentColor = if (unackedAlerts.isNotEmpty()) NocWarningAmber else NocSuccessGreen,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.selectTab(NocNavTab.ALERTS) }
                    )
                }
            }
        }

        // 24-Hour Historical Interface Traffic Dashboard Widget (Recharts-style)
        item {
            Historical24hTrafficWidget(
                hourlyPoints = historical24hTraffic,
                interfaces = interfaces,
                selectedInterfaceId = selected24hInterfaceId,
                onSelectInterface = { viewModel.select24hInterface(it) }
            )
        }

        // Real-Time Connected OLT & MikroTik Bandwidth Utilization, Latency & Packet Loss Matrix
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("realtime_device_telemetry_matrix"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CONNECTED OLT & MIKROTIK REAL-TIME TELEMETRY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Bandwidth Utilization, Latency & Packet Loss",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        TextButton(
                            onClick = { viewModel.triggerDeviceUnreachable("olt-zte-02", isOlt = true) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("sim_olt_unreachable_btn")
                        ) {
                            Text("Sim Unreachable", fontSize = 10.sp, color = NocCriticalRed, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // OLTs Rows
                    olts.forEach { olt ->
                        DeviceRealtimeTelemetryRow(
                            name = olt.name,
                            roleLabel = "OLT • ${olt.vendor.displayName} (${olt.ipAddress})",
                            status = olt.status,
                            rxBps = olt.currentRxBps,
                            txBps = olt.currentTxBps,
                            utilizationPercent = olt.bandwidthUtilizationPercent,
                            latencyMs = olt.avgLatencyMs,
                            packetLossPercent = olt.packetLossPercent
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // MikroTiks Rows
                    mikrotiks.forEach { mtk ->
                        DeviceRealtimeTelemetryRow(
                            name = mtk.name,
                            roleLabel = "MikroTik • ${mtk.model} (${mtk.ipAddress})",
                            status = mtk.status,
                            rxBps = mtk.currentRxBps,
                            txBps = mtk.currentTxBps,
                            utilizationPercent = mtk.bandwidthUtilizationPercent,
                            latencyMs = mtk.avgLatencyMs,
                            packetLossPercent = mtk.packetLossPercent
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Real-time Rolling 60s Traffic Graph
        item {
            LiveTrafficChart(
                points = trafficHistory,
                interfaceName = "Aggregated Uplink Traffic (HQ CCR2004)",
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            )
        }

        // Live Throughput Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOTAL DOWNLOAD (RX)", style = MaterialTheme.typography.labelSmall, color = NocTextSecondary)
                        Text(
                            text = Formatters.formatBps(totalRxBps),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TrafficRxColor
                        )
                    }
                    Divider(modifier = Modifier.height(28.dp).width(1.dp), color = MaterialTheme.colorScheme.outline)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOTAL UPLOAD (TX)", style = MaterialTheme.typography.labelSmall, color = NocTextSecondary)
                        Text(
                            text = Formatters.formatBps(totalTxBps),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TrafficTxColor
                        )
                    }
                }
            }
        }

        // Critical Alerts Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CRITICAL NOC ALERTS (${unackedAlerts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = { viewModel.selectTab(NocNavTab.ALERTS) }) {
                    Text("View All", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Alert Items
        if (unackedAlerts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NocSuccessGreen)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "All systems healthy. No unacknowledged alerts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        } else {
            items(unackedAlerts.take(4)) { alert ->
                AlertCardItem(
                    alert = alert,
                    onAcknowledge = { viewModel.acknowledgeAlert(alert.id) }
                )
            }
        }
    }
}

@Composable
fun DeviceRealtimeTelemetryRow(
    name: String,
    roleLabel: String,
    status: DeviceStatus,
    rxBps: Long,
    txBps: Long,
    utilizationPercent: Float,
    latencyMs: Float,
    packetLossPercent: Float
) {
    val statusColor = when (status) {
        DeviceStatus.ONLINE -> NocSuccessGreen
        DeviceStatus.WARNING -> NocWarningAmber
        DeviceStatus.CRITICAL, DeviceStatus.OFFLINE -> NocCriticalRed
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = roleLabel,
                        fontSize = 10.sp,
                        color = NocTextSecondary
                    )
                }
            }

            Text(
                text = if (status == DeviceStatus.OFFLINE) "UNREACHABLE" else "Util: ${String.format("%.1f", utilizationPercent)}%",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (utilizationPercent > 80f || status == DeviceStatus.OFFLINE) NocCriticalRed else MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "RX: ${Formatters.formatBps(rxBps)} | TX: ${Formatters.formatBps(txBps)}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (status == DeviceStatus.OFFLINE) "Loss: 100% • Timeout"
                       else "Latency: ${String.format("%.1f", latencyMs)} ms • Loss: ${String.format("%.2f", packetLossPercent)}%",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = if (packetLossPercent > 1.0f || latencyMs > 30f || status == DeviceStatus.OFFLINE) NocWarningAmber else NocSuccessGreen
            )
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    mainValue: String,
    subText: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NocTextSecondary
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = mainValue,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subText,
                style = MaterialTheme.typography.bodySmall,
                color = NocTextSecondary
            )
        }
    }
}

@Composable
fun AlertCardItem(
    alert: NetworkAlert,
    onAcknowledge: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (color, badgeBg) = when (alert.severity) {
        AlertSeverity.CRITICAL -> Pair(NocCriticalRed, Color(0xFFFFE4E6))
        AlertSeverity.WARNING -> Pair(NocWarningAmber, Color(0xFFFEF3C7))
        AlertSeverity.INFO -> Pair(NocPrimaryBlue, Color(0xFFE0F2FE))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (alert.severity) {
                        AlertSeverity.CRITICAL -> Icons.Default.Warning
                        AlertSeverity.WARNING -> Icons.Default.WarningAmber
                        AlertSeverity.INFO -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = alert.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = Formatters.formatTimeAgo(alert.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = NocTextMuted
                    )
                }

                Text(
                    text = alert.targetName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = alert.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = NocTextSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Email & Webhook Dispatch Status Line
                Text(
                    text = "Dispatch: ${alert.deliveryChannels} • ${alert.deliveryStatus}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = NocSuccessGreen,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Value: ${alert.metricValue} (Limit: ${alert.thresholdValue})",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = color,
                        fontWeight = FontWeight.SemiBold
                    )

                    TextButton(
                        onClick = onAcknowledge,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Acknowledge", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
