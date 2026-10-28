package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.model.*
import com.example.ui.NocViewModel
import com.example.ui.components.Formatters
import com.example.ui.components.LiveTrafficChart
import com.example.ui.theme.*

@Composable
fun MikrotikMonitoringScreen(
    viewModel: NocViewModel,
    modifier: Modifier = Modifier
) {
    val mikrotiks by viewModel.mikrotiks.collectAsState()
    val interfaces by viewModel.interfaces.collectAsState()
    val trafficHistory by viewModel.trafficHistory.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val activeRouter = mikrotiks.find { it.id == uiState.selectedRouterId } ?: mikrotiks.firstOrNull()
    val routerInterfaces = interfaces.filter { it.routerId == activeRouter?.id }

    val activeInterface = routerInterfaces.find { it.id == uiState.selectedInterfaceId } ?: routerInterfaces.firstOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MIKROTIK ROUTEROS MONITORING",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Real-time Traffic & Interfaces",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.setAddRouterDialogOpen(true) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_router_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Router")
                }
            }
        }

        // Router Selection Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(mikrotiks) { mtk ->
                    val isSelected = mtk.id == activeRouter?.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectRouter(mtk.id) },
                        label = {
                            Text(
                                text = "${mtk.name} (${mtk.model})",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (mtk.status == DeviceStatus.ONLINE) NocSuccessGreen else NocWarningAmber)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // Router Hardware Summary Card
        if (activeRouter != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Router, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = activeRouter.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${activeRouter.ipAddress} • OS ${activeRouter.routerOsVersion} • Up: ${activeRouter.uptimeString}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = NocTextSecondary
                                    )
                                }
                            }

                            IconButton(onClick = { viewModel.deleteMikrotik(activeRouter.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Router", tint = NocCriticalRed)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricPill(label = "CPU LOAD", value = "${activeRouter.cpuUsage}%", isAlert = activeRouter.cpuUsage > 75)
                            MetricPill(label = "FREE RAM", value = "${activeRouter.freeRamMb} MB", isAlert = false)
                            MetricPill(label = "BOARD TEMP", value = "${activeRouter.temperatureCelsius}°C", isAlert = activeRouter.temperatureCelsius > 55f)
                            MetricPill(label = "PPPoE USERS", value = "${activeRouter.activePppoeClients}", isAlert = false)
                        }
                    }
                }
            }
        }

        // Live Real-Time Traffic Graph of selected interface
        item {
            val chartInterfaceName = activeInterface?.let { "${it.name} (${it.comment.ifEmpty { it.type.label }})" } ?: "Select an interface below"
            LiveTrafficChart(
                points = trafficHistory,
                interfaceName = chartInterfaceName,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            )
        }

        // Interfaces Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NETWORK INTERFACES (${routerInterfaces.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tap interface to graph live traffic",
                    style = MaterialTheme.typography.bodySmall,
                    color = NocTextSecondary
                )
            }
        }

        // Interface Cards
        items(routerInterfaces) { intf ->
            val isSelected = intf.id == activeInterface?.id
            InterfaceRowCard(
                intf = intf,
                isSelected = isSelected,
                onClick = { viewModel.selectInterface(intf.id) },
                onToggleLink = { viewModel.toggleInterfaceLink(intf.id) }
            )
        }
    }

    if (uiState.isAddRouterDialogOpen) {
        AddRouterDialog(
            onDismiss = { viewModel.setAddRouterDialogOpen(false) },
            onAdd = { viewModel.addMikrotik(it) }
        )
    }
}

@Composable
fun InterfaceRowCard(
    intf: NetworkInterface,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggleLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary), width = 1.5.dp)
        } else {
            CardDefaults.outlinedCardBorder()
        }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (intf.linkStatus == LinkStatus.UP) NocSuccessGreen else OpticalLosGray)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = intf.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Link Status Toggle Button
                OutlinedButton(
                    onClick = onToggleLink,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (intf.linkStatus == LinkStatus.UP) NocSuccessGreen else NocCriticalRed
                    )
                ) {
                    Text(
                        text = if (intf.linkStatus == LinkStatus.UP) "LINK UP" else "LINK DOWN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (intf.comment.isNotEmpty()) {
                Text(
                    text = intf.comment,
                    style = MaterialTheme.typography.bodySmall,
                    color = NocTextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Speeds & Packet Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "RX DOWNLOAD", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = NocTextSecondary)
                    Text(
                        text = Formatters.formatBps(intf.currentRxBps),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TrafficRxColor
                    )
                    Text(
                        text = "${Formatters.formatPps(intf.rxPacketsPerSec)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = NocTextMuted
                    )
                }

                Divider(modifier = Modifier.height(30.dp).width(1.dp), color = MaterialTheme.colorScheme.outline)

                Column {
                    Text(text = "TX UPLOAD", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = NocTextSecondary)
                    Text(
                        text = Formatters.formatBps(intf.currentTxBps),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TrafficTxColor
                    )
                    Text(
                        text = "${Formatters.formatPps(intf.txPacketsPerSec)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = NocTextMuted
                    )
                }

                Divider(modifier = Modifier.height(30.dp).width(1.dp), color = MaterialTheme.colorScheme.outline)

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "SPEED & TYPE", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = NocTextSecondary)
                    Text(
                        text = "${intf.linkSpeedMbps / 1000} Gbps ${intf.type.label}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "MTU ${intf.mtu}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = NocTextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun AddRouterDialog(
    onDismiss: () -> Unit,
    onAdd: (MikrotikDevice) -> Unit
) {
    var name by remember { mutableStateOf("Edge-Router-03") }
    var ip by remember { mutableStateOf("10.100.0.15") }
    var restPort by remember { mutableStateOf("443") }
    var model by remember { mutableStateOf("CCR2004-16G-2S+") }
    var username by remember { mutableStateOf("admin") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add MikroTik RouterOS Device", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Router Hostname / Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP Address") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = restPort,
                    onValueChange = { restPort = it },
                    label = { Text("REST API Port") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Hardware Model") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Admin Username") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newDevice = MikrotikDevice(
                        id = "mtk-${System.currentTimeMillis()}",
                        name = name,
                        ipAddress = ip,
                        restPort = restPort.toIntOrNull() ?: 443,
                        model = model,
                        username = username
                    )
                    onAdd(newDevice)
                }
            ) {
                Text("Connect & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
