package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.NocViewModel
import com.example.ui.theme.*

@Composable
fun DiagnosticsScreen(
    viewModel: NocViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val ipInfo = uiState.myIpAndGateway
    val speedTest = uiState.speedTestState
    val traceState = uiState.tracerouteState
    val dnsResult = uiState.dnsLookupResult
    val latencyAnalysis = uiState.latencyAnalysisResult

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("diagnostics_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "NETWORK TESTING & DIAGNOSTICS SUITE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "IP Gateway • Speed Test • Latency • Traceroute • DNS",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // ==========================================
        // 1. WHAT IS MY IP & DEFAULT GATEWAY INSPECTOR
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("my_ip_gateway_card"),
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Router,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "What Is My IP & Gateway",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Local LAN, Default Gateway & Public WAN IP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NocTextSecondary
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.refreshMyIpAndGateway() },
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("refresh_ip_gateway_button")
                        ) {
                            if (ipInfo.isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Detect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Primary 3-column IP & Gateway Banner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("DEFAULT GATEWAY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = ipInfo.gatewayIp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${ipInfo.gatewayLatencyMs} ms • Online",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = NocSuccessGreen
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("MY LOCAL LAN IP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NocTextSecondary)
                                Text(
                                    text = ipInfo.localIp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = ipInfo.subnetCidr,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = NocTextSecondary
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("PUBLIC WAN IP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NocSuccessGreen)
                                Text(
                                    text = ipInfo.publicIp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = ipInfo.interfaceName,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = NocTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("ISP / Transit ASN:", fontSize = 11.sp, color = NocTextSecondary)
                                Text(ipInfo.ispAndAsn, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("DNS Resolvers:", fontSize = 11.sp, color = NocTextSecondary)
                                Text(ipInfo.dnsServers, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Interface MAC:", fontSize = 11.sp, color = NocTextSecondary)
                                Text(ipInfo.macAddress, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. SPEED TEST (DOWNLOAD / UPLOAD / PING / JITTER)
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("speed_test_card"),
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFECFDF5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = NocSuccessGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Bandwidth Speed Test",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = speedTest.serverName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NocTextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.startSpeedTest() },
                            enabled = !speedTest.isRunning,
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("speed_test_start_button")
                        ) {
                            if (speedTest.isRunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Start Speed Test", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (speedTest.isRunning) {
                        LinearProgressIndicator(
                            progress = { speedTest.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = NocSuccessGreen
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Text(
                        text = speedTest.phaseLabel,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = if (speedTest.isRunning) MaterialTheme.colorScheme.primary else NocSuccessGreen
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Download Mbps
                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("DOWNLOAD", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NocSuccessGreen)
                                Text(
                                    text = String.format("%.1f", speedTest.downloadMbps),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = NocSuccessGreen
                                )
                                Text("Mbps", fontSize = 10.sp, color = NocTextSecondary)
                            }
                        }

                        // Upload Mbps
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("UPLOAD", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = String.format("%.1f", speedTest.uploadMbps),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("Mbps", fontSize = 10.sp, color = NocTextSecondary)
                            }
                        }

                        // Ping & Jitter
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("LATENCY / JITTER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NocTextSecondary)
                                Text(
                                    text = "${String.format("%.1f", speedTest.pingMs)} ms",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Jitter: ${String.format("%.1f", speedTest.jitterMs)} ms",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = NocTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. LATENCY & DEDICATED PING TESTING TOOL
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ping_testing_tool_card"),
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.NetworkCheck,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Latency, Jitter & Ping Tool",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Real-time RTT ms, Jitter & reachability",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NocTextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.pingAllPresets() },
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("ping_all_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ping All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Target 1: 8.8.8.8
                    PingTargetItem(
                        targetIndex = "1",
                        targetHost = "8.8.8.8",
                        targetLabel = "Google Public DNS",
                        status = uiState.pingResults["8.8.8.8"],
                        isPinging = uiState.pingingHosts.contains("8.8.8.8"),
                        onPing = { viewModel.pingTarget("8.8.8.8") },
                        modifier = Modifier.testTag("ping_target_8_8_8_8")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Target 2: 1.1.1.1
                    PingTargetItem(
                        targetIndex = "2",
                        targetHost = "1.1.1.1",
                        targetLabel = "Cloudflare DNS",
                        status = uiState.pingResults["1.1.1.1"],
                        isPinging = uiState.pingingHosts.contains("1.1.1.1"),
                        onPing = { viewModel.pingTarget("1.1.1.1") },
                        modifier = Modifier.testTag("ping_target_1_1_1_1")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Target 3: 10.10.25.1
                    PingTargetItem(
                        targetIndex = "3",
                        targetHost = "10.10.25.1",
                        targetLabel = "NOC Gateway / Core Route",
                        status = uiState.pingResults["10.10.25.1"],
                        isPinging = uiState.pingingHosts.contains("10.10.25.1"),
                        onPing = { viewModel.pingTarget("10.10.25.1") },
                        modifier = Modifier.testTag("ping_target_10_10_25_1")
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Target 4: Blank for Manual input
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "4",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Manual IP / Host Latency & Jitter Probe",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.manualPingHost,
                                onValueChange = { viewModel.setManualPingHost(it) },
                                placeholder = { Text("Enter IP (e.g. 192.168.1.1)") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("manual_ping_input")
                            )

                            Button(
                                onClick = { viewModel.pingTarget(uiState.manualPingHost) },
                                enabled = uiState.manualPingHost.isNotBlank() && !uiState.pingingHosts.contains(uiState.manualPingHost.trim()),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("manual_ping_button")
                            ) {
                                if (uiState.pingingHosts.contains(uiState.manualPingHost.trim())) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Ping")
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.runLatencyAndJitterAnalysis(uiState.manualPingHost.ifBlank { "8.8.8.8" }) },
                                enabled = !uiState.isAnalyzingLatency,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("analyze_latency_button")
                            ) {
                                Text("Jitter", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Show only ms reachable for manual host
                        val manualTarget = uiState.manualPingHost.trim()
                        val manualStatus = uiState.pingResults[manualTarget]
                        if (manualTarget.isNotBlank() && manualStatus != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            PingResultBadge(status = manualStatus)
                        }

                        // Multi-sample Latency & Jitter Summary Strip
                        if (latencyAnalysis != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "LATENCY BREAKDOWN (${latencyAnalysis.host})",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Min ${String.format("%.1f", latencyAnalysis.minMs)}ms • Avg ${String.format("%.1f", latencyAnalysis.avgMs)}ms • Max ${String.format("%.1f", latencyAnalysis.maxMs)}ms",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "Jitter: ${String.format("%.1f", latencyAnalysis.jitterMs)}ms • Loss: ${String.format("%.0f", latencyAnalysis.packetLossPercent)}%",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = NocSuccessGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. INTERACTIVE TRACEROUTE TOOL
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("traceroute_tool_card"),
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AltRoute,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Traceroute Hop Analyzer",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = traceState.statusMessage,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NocTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Target Presets
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("8.8.8.8", "1.1.1.1", "fms.bsnl.in", "10.10.25.1")) { preset ->
                            FilterChip(
                                selected = uiState.tracerouteTargetInput == preset,
                                onClick = { viewModel.startTraceroute(preset) },
                                label = { Text(preset, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.tracerouteTargetInput,
                            onValueChange = { viewModel.setTracerouteTarget(it) },
                            placeholder = { Text("Host or IP (e.g. 8.8.8.8)") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("traceroute_target_input")
                        )

                        Button(
                            onClick = { viewModel.startTraceroute() },
                            enabled = !traceState.isRunning,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("traceroute_run_button")
                        ) {
                            if (traceState.isRunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Trace Route", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Hop-by-Hop Route List
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        traceState.hops.forEach { hop ->
                            val hopColor = when {
                                !hop.isReachable -> NocCriticalRed
                                hop.avgLatencyMs <= 15f -> NocSuccessGreen
                                hop.avgLatencyMs <= 45f -> NocWarningAmber
                                else -> NocCriticalRed
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(hopColor.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${hop.hopNumber}",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = hopColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = hop.ipAddress,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = hop.hopRole,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = hop.hostname,
                                            fontSize = 10.sp,
                                            color = NocTextSecondary
                                        )
                                    }
                                }

                                Text(
                                    text = "${String.format("%.1f", hop.avgLatencyMs)} ms",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = hopColor
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 5. DNS LOOKUP & RECORD RESOLVER
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dns_lookup_card"),
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Dns,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "DNS Lookup & Resolver",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = dnsResult?.statusMessage ?: "Resolve A, AAAA, MX, NS, TXT, CNAME records",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NocTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Record Type Selector Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("A", "AAAA", "MX", "NS", "TXT", "CNAME")) { type ->
                            FilterChip(
                                selected = uiState.dnsRecordType == type,
                                onClick = {
                                    viewModel.setDnsRecordType(type)
                                    viewModel.performDnsLookup(customType = type)
                                },
                                label = { Text(type, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.height(28.dp).testTag("dns_type_$type")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.dnsQueryInput,
                            onValueChange = { viewModel.setDnsQueryInput(it) },
                            placeholder = { Text("Domain (e.g. fms.bsnl.in)") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dns_lookup_input")
                        )

                        Button(
                            onClick = { viewModel.performDnsLookup() },
                            enabled = !uiState.isDnsLoading,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("dns_lookup_button")
                        ) {
                            if (uiState.isDnsLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Lookup", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (dnsResult != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Resolver: ${dnsResult.resolverUsed} • ${dnsResult.queryTimeMs} ms",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NocTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            dnsResult.records.forEach { rec ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = rec.recordType,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = rec.value,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = rec.domain,
                                                fontSize = 10.sp,
                                                color = NocTextSecondary
                                            )
                                        }
                                    }
                                    Text(
                                        text = "TTL ${rec.ttlSeconds}s",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NocSuccessGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 6. Fast Reference CLI Commands
        // ==========================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("NOC Quick CLI Reference", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Huawei OLT Optical Query:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text("display ont optical-info 0 1 1", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NocTextSecondary)

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("ZTE C300/C320 Optical Query:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text("show pon power onu-rx gpon-onu_1/1/1:1", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NocTextSecondary)

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("MikroTik Real-time Traffic Monitor:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text("/interface monitor-traffic sfp-sfpplus1 once", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NocTextSecondary)
                }
            }
        }
    }
}

@Composable
fun PingTargetItem(
    targetIndex: String,
    targetHost: String,
    targetLabel: String,
    status: HostPingStatus?,
    isPinging: Boolean,
    onPing: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = targetIndex,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = targetHost,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = targetLabel,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = NocTextSecondary
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Show ONLY ms reachable or Unreachable
            if (isPinging) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (status != null) {
                PingResultBadge(status = status)
            }

            IconButton(
                onClick = onPing,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Ping $targetHost",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Renders ONLY the ms reachable or Unreachable result
 */
@Composable
fun PingResultBadge(status: HostPingStatus) {
    val (bgColor, textColor, textLabel) = if (status.isReachable) {
        Triple(
            Color(0xFFECFDF5),
            NocSuccessGreen,
            "${status.latencyMs} ms  Reachable"
        )
    } else {
        Triple(
            Color(0xFFFFE4E6),
            NocCriticalRed,
            "Unreachable"
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = textLabel,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}
