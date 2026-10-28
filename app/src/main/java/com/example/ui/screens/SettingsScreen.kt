package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.NocViewModel
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: NocViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val olts by viewModel.olts.collectAsState()
    val ponPorts by viewModel.ponPorts.collectAsState()

    val activeOlt = olts.find { it.id == uiState.selectedSettingsOltId } ?: olts.firstOrNull()
    val oltPonPorts = remember(ponPorts, activeOlt) {
        if (activeOlt != null) ponPorts.filter { it.oltId == activeOlt.id } else emptyList()
    }

    var globalRoCommunity by remember(uiState.globalSnmpReadCommunity) {
        mutableStateOf(uiState.globalSnmpReadCommunity)
    }
    var globalRwCommunity by remember(uiState.globalSnmpWriteCommunity) {
        mutableStateOf(uiState.globalSnmpWriteCommunity)
    }
    var globalUdpPortText by remember(uiState.globalSnmpUdpPort) {
        mutableStateOf(uiState.globalSnmpUdpPort.toString())
    }
    var selectedSnmpVersion by remember(uiState.snmpVersion) {
        mutableStateOf(uiState.snmpVersion)
    }

    var oltIpInput by remember(activeOlt?.id, activeOlt?.ipAddress) {
        mutableStateOf(activeOlt?.ipAddress ?: "")
    }
    var oltPortInput by remember(activeOlt?.id, activeOlt?.port) {
        mutableStateOf((activeOlt?.port ?: 161).toString())
    }
    var oltSnmpCommunityInput by remember(activeOlt?.id, activeOlt?.snmpCommunity) {
        mutableStateOf(activeOlt?.snmpCommunity ?: "public")
    }
    var oltTotalPonPortsInput by remember(activeOlt?.id, activeOlt?.totalPonPorts) {
        mutableStateOf((activeOlt?.totalPonPorts ?: 16).toString())
    }

    var newPonPortName by remember(activeOlt?.id, oltPonPorts.size) {
        mutableStateOf("0/1/${oltPonPorts.size + 1}")
    }
    var newPonPortTxDbm by remember { mutableStateOf("4.5") }
    var newPonPortSplit by remember { mutableStateOf("64") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "HARDWARE & SNMP CONFIGURATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "SNMP Community Strings & OLT Port Diagnostics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Configure real SNMPv2c/v3 credentials, OLT management ports, PON SFP optical budgets, and run live hardware DOM diagnostics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NocTextSecondary
                )
            }
        }

        // Status Feedback Banner
        if (!uiState.settingsFeedbackMessage.isNullOrBlank()) {
            item {
                Surface(
                    color = Color(0xFFECFDF5),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_feedback_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = NocSuccessGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.settingsFeedbackMessage ?: "",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NocSuccessGreen
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearSettingsFeedbackMessage() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = NocSuccessGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 1. REAL-LIFE WORKSPACE & LIVE NETWORK POLLING CONTROLS
        // =========================================================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("real_life_workspace_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Real-Time Hardware Polling & Workspace Data",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Probe real OLT & MikroTik IPs on your LAN or clear preloaded devices",
                                style = MaterialTheme.typography.labelSmall,
                                color = NocTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.probeAllRealDevicesNow()
                                Toast.makeText(context, "Probing all OLTs & MikroTiks via ICMP/TCP/SNMP...", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_poll_real_devices_btn")
                        ) {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Probe All Devices Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.clearPreloadedDevicesForRealUse()
                                Toast.makeText(context, "Cleared preloaded devices. Ready for your real OLTs & routers.", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("clear_sample_devices_btn")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Clean (Empty)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 2. GLOBAL SNMP COMMUNITY STRINGS & UDP PORT CONFIGURATION
        // =========================================================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("global_snmp_config_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Global SNMP Community Strings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Default SNMP Read-Only (RO), Read-Write (RW) & UDP Port for OLT MIB polling",
                                style = MaterialTheme.typography.labelSmall,
                                color = NocTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // SNMP Version Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SNMPv2c", "SNMPv1", "SNMPv3").forEach { ver ->
                            FilterChip(
                                selected = selectedSnmpVersion == ver,
                                onClick = { selectedSnmpVersion = ver },
                                label = { Text(ver, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("snmp_version_chip_$ver")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = globalRoCommunity,
                            onValueChange = { globalRoCommunity = it },
                            label = { Text("Read-Only (RO) Community") },
                            placeholder = { Text("e.g. public") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("global_snmp_ro_input")
                        )

                        OutlinedTextField(
                            value = globalRwCommunity,
                            onValueChange = { globalRwCommunity = it },
                            label = { Text("Read-Write (RW) Community") },
                            placeholder = { Text("e.g. private_noc_rw") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("global_snmp_rw_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = globalUdpPortText,
                            onValueChange = { globalUdpPortText = it },
                            label = { Text("SNMP UDP Port") },
                            placeholder = { Text("161") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(0.45f)
                                .testTag("global_snmp_port_input")
                        )

                        Button(
                            onClick = {
                                val parsedPort = globalUdpPortText.toIntOrNull() ?: 161
                                viewModel.updateGlobalSnmpSettings(
                                    readCommunity = globalRoCommunity,
                                    writeCommunity = globalRwCommunity,
                                    udpPort = parsedPort,
                                    version = selectedSnmpVersion
                                )
                                viewModel.applyGlobalSnmpCommunityToAllOlts(
                                    readCommunity = globalRoCommunity,
                                    writeCommunity = globalRwCommunity,
                                    udpPort = parsedPort
                                )
                                oltSnmpCommunityInput = globalRoCommunity.trim().ifBlank { "public" }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(0.55f)
                                .height(52.dp)
                                .testTag("apply_global_snmp_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Apply to All OLTs", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 3. PER-OLT SNMP COMMUNITY & MANAGEMENT PORT CONFIGURATION
        // =========================================================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("per_olt_snmp_port_config_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Hub,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "OLT SNMP & Port Configuration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Select OLT chassis to configure IP, Management Port, SNMP string & PON count",
                                style = MaterialTheme.typography.labelSmall,
                                color = NocTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (olts.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(olts) { olt ->
                                FilterChip(
                                    selected = olt.id == activeOlt?.id,
                                    onClick = { viewModel.selectSettingsOlt(olt.id) },
                                    label = {
                                        Text(
                                            text = "${olt.name} (${olt.ipAddress})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    modifier = Modifier.testTag("settings_olt_chip_${olt.id}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (activeOlt != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = oltIpInput,
                                    onValueChange = { oltIpInput = it },
                                    label = { Text("OLT IP Address") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("olt_ip_input")
                                )

                                OutlinedTextField(
                                    value = oltPortInput,
                                    onValueChange = { oltPortInput = it },
                                    label = { Text("Mgmt Port (CLI/HTTP)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(0.75f)
                                        .testTag("olt_port_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = oltSnmpCommunityInput,
                                    onValueChange = { oltSnmpCommunityInput = it },
                                    label = { Text("OLT SNMP Community String") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("snmp_community_input")
                                )

                                OutlinedTextField(
                                    value = oltTotalPonPortsInput,
                                    onValueChange = { oltTotalPonPortsInput = it },
                                    label = { Text("Total PON Ports") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(0.75f)
                                        .testTag("olt_total_pon_ports_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    viewModel.saveOltSnmpAndPortConfig(
                                        oltId = activeOlt.id,
                                        ipAddress = oltIpInput,
                                        port = oltPortInput.toIntOrNull() ?: activeOlt.port,
                                        snmpCommunity = oltSnmpCommunityInput,
                                        totalPonPorts = oltTotalPonPortsInput.toIntOrNull() ?: activeOlt.totalPonPorts
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_olt_config_btn")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Save OLT SNMP & Port Configuration",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "No OLTs currently configured. Add a real OLT from the OLT/ONU tab.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NocTextSecondary
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 4. PON PORT OPTICAL & SPLIT RATIO CONFIGURATIONS
        // =========================================================================
        if (activeOlt != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pon_port_configs_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFECFDF5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.SettingsInputComponent,
                                    contentDescription = null,
                                    tint = NocSuccessGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "PON Port Optical & Split Configurations (${activeOlt.name})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Configure SFP Tx Launch Power (dBm), Max Split Ratio (1:64/1:128) & Admin State",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NocTextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            oltPonPorts.forEach { port ->
                                PonPortConfigRow(
                                    port = port,
                                    onSavePort = { name, txDbm, split, adminUp ->
                                        viewModel.savePonPortConfig(
                                            portId = port.id,
                                            name = name,
                                            txPowerDbm = txDbm,
                                            totalOnus = split,
                                            adminState = adminUp
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Add New PON Port Configuration",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newPonPortName,
                                onValueChange = { newPonPortName = it },
                                label = { Text("PON Port") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("new_pon_port_name_input")
                            )

                            OutlinedTextField(
                                value = newPonPortTxDbm,
                                onValueChange = { newPonPortTxDbm = it },
                                label = { Text("Tx (dBm)") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(0.8f)
                                    .testTag("new_pon_port_tx_input")
                            )

                            OutlinedTextField(
                                value = newPonPortSplit,
                                onValueChange = { newPonPortSplit = it },
                                label = { Text("Max Split") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(0.8f)
                                    .testTag("new_pon_port_split_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                viewModel.addNewPonPortConfig(
                                    oltId = activeOlt.id,
                                    name = newPonPortName,
                                    txPowerDbm = newPonPortTxDbm.toFloatOrNull() ?: 4.5f,
                                    totalOnus = newPonPortSplit.toIntOrNull() ?: 64
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_pon_port_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add PON Port to ${activeOlt.name}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 5. DEEPER HARDWARE DIAGNOSTICS CONSOLE (SNMP OID & SFP DOM TELEMETRY)
        // =========================================================================
        item {
            val report = uiState.hardwareDiagnosticReport
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("deep_hardware_diagnostics_card"),
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
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Memory,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Deep Hardware & SFP Optical Diagnostics",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "SNMP OID Walk • SFP DOM Bias/Temp/Voltage • PSU & Fan Health",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NocTextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.runDeepHardwareDiagnostics(activeOlt?.id) },
                            enabled = !uiState.isRunningHardwareDiag && activeOlt != null,
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("run_deep_diagnostics_btn")
                        ) {
                            if (uiState.isRunningHardwareDiag) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Probing...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Run Diagnostics", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (report != null) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("hardware_diagnostic_report_box")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${report.oltName} (${report.ipAddress}:${report.managementPort})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "SNMP '${report.snmpCommunityUsed}' • ${report.snmpResponseTimeMs} ms",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = NocSuccessGreen
                                    )
                                }

                                Text(
                                    text = "MIB sysObjectID: ${report.sysObjectIdOid}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = NocTextSecondary
                                )
                                Text(
                                    text = "Firmware: ${report.firmwareVersion} • SyncE: ${report.opticalClockSyncState}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = NocTextSecondary
                                )
                                Text(
                                    text = "Chassis Temp: ${String.format("%.1f", report.chassisBackplaneTempC)}°C • PSU: ${report.psuRedundancyStatus} (-${report.psuRailAVoltage}V / -${report.psuRailBVoltage}V) • Fans: ${report.fanTrayRpm.joinToString("/")} RPM",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = report.summaryRecommendation,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Per-PON Port SFP Transceiver DOM Diagnostics
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            report.ponPortDiagnostics.forEach { portDiag ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "PON ${portDiag.portName} • ${portDiag.sfpVendorPart}",
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = portDiag.diagnosticHealth,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (portDiag.diagnosticHealth.startsWith("HEALTHY")) NocSuccessGreen else NocWarningAmber
                                            )
                                        }

                                        Text(
                                            text = "${portDiag.sfpClass} • ${portDiag.wavelengthNm}nm • Split ${portDiag.activeOnus}/${portDiag.maxSplitCapacity} ONUs",
                                            fontSize = 10.sp,
                                            color = NocTextSecondary
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Tx: ${String.format("%+.1f", portDiag.txPowerDbm)} dBm | Avg Rx: ${String.format("%.1f", portDiag.avgOnuRxPowerDbm)} dBm",
                                                fontSize = 10.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Bias: ${String.format("%.1f", portDiag.laserBiasCurrentMa)}mA | ${String.format("%.2f", portDiag.sfpSupplyVoltageVolts)}V | ${String.format("%.1f", portDiag.sfpTemperatureCelsius)}°C",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = NocTextSecondary
                                            )
                                        }

                                        Text(
                                            text = "ITU-T G.984 Error Counters: BIP8=${portDiag.bip8ErrorCount} •GEM/CRC=${portDiag.crcErrorCount}",
                                            fontSize = 9.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (portDiag.bip8ErrorCount > 50) NocWarningAmber else NocSuccessGreen
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap 'Run Diagnostics' to execute an SNMP MIB walk and query SFP optical transceiver DOM parameters (Laser Bias mA, Voltage V, Temp °C, Tx/Rx dBm, and BIP8/CRC error counters) for the selected OLT.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NocTextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PonPortConfigRow(
    port: PonPort,
    onSavePort: (name: String, txPowerDbm: Float, totalOnus: Int, adminState: Boolean) -> Unit
) {
    var nameInput by remember(port.id, port.name) { mutableStateOf(port.name) }
    var txInput by remember(port.id, port.txPowerDbm) { mutableStateOf(String.format("%.1f", port.txPowerDbm)) }
    var splitInput by remember(port.id, port.totalOnus) { mutableStateOf(port.totalOnus.toString()) }
    var adminUp by remember(port.id, port.adminState) { mutableStateOf(port.adminState) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pon_port_row_${port.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PON ${port.name} (${port.onlineOnus}/${port.totalOnus} ONUs Online)",
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (adminUp) "ADMIN UP" else "ADMIN DOWN",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (adminUp) NocSuccessGreen else NocCriticalRed
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = adminUp,
                        onCheckedChange = { adminUp = it },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Port Label") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = txInput,
                    onValueChange = { txInput = it },
                    label = { Text("Tx dBm") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(0.75f)
                )

                OutlinedTextField(
                    value = splitInput,
                    onValueChange = { splitInput = it },
                    label = { Text("Max ONUs") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(0.75f)
                )

                Button(
                    onClick = {
                        onSavePort(
                            nameInput,
                            txInput.toFloatOrNull() ?: port.txPowerDbm,
                            splitInput.toIntOrNull() ?: port.totalOnus,
                            adminUp
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("save_pon_port_btn_${port.id}")
                ) {
                    Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
