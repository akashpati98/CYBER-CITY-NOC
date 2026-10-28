package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.NocViewModel
import com.example.ui.components.OnuPowerTrendChart
import com.example.ui.components.OpticalPowerGauge
import com.example.ui.components.VsolRouterConfigDialog
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OltMonitoringScreen(
    viewModel: NocViewModel,
    modifier: Modifier = Modifier
) {
    val olts by viewModel.olts.collectAsState()
    val onus by viewModel.onus.collectAsState()
    val onuPowerHistory by viewModel.onuPowerHistory.collectAsState()
    val thresholds by viewModel.thresholds.collectAsState()
    val ponPorts by viewModel.ponPorts.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val activeOltId = uiState.selectedOltId ?: olts.firstOrNull()?.id
    val activeOlt = olts.find { it.id == activeOltId } ?: olts.firstOrNull()

    val isVsolActive = activeOlt?.vendor == OltVendor.VSOL
    val filteredPorts = ponPorts.filter { it.oltId == activeOlt?.id }

    val displayOnus = remember(onus, activeOlt, uiState.selectedPonPort, uiState.onuSearchQuery, uiState.onuStatusFilter) {
        onus.filter { onu ->
            val matchOlt = activeOlt == null || onu.oltId == activeOlt.id
            val matchPort = uiState.selectedPonPort == null || onu.ponPort == uiState.selectedPonPort
            val matchQuery = uiState.onuSearchQuery.isEmpty() ||
                    onu.customerName.contains(uiState.onuSearchQuery, ignoreCase = true) ||
                    onu.serialNumber.contains(uiState.onuSearchQuery, ignoreCase = true) ||
                    onu.pppoeAccount.contains(uiState.onuSearchQuery, ignoreCase = true)
            val matchStatus = when (uiState.onuStatusFilter) {
                null -> true
                OnuStatus.CRITICAL -> onu.status == OnuStatus.CRITICAL || onu.status == OnuStatus.OFFLINE
                OnuStatus.WARNING -> onu.status == OnuStatus.WARNING
                OnuStatus.ONLINE -> onu.status == OnuStatus.ONLINE
                else -> onu.status == uiState.onuStatusFilter
            }
            matchOlt && matchPort && matchQuery && matchStatus
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Quick V-SOL Setup FAB
                FloatingActionButton(
                    onClick = { viewModel.setVsolOntSetupDialogOpen(true) },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("vsol_ont_setup_fab")
                ) {
                    Icon(Icons.Default.SettingsEthernet, contentDescription = "V-SOL ONT Setup")
                }

                ExtendedFloatingActionButton(
                    onClick = { viewModel.setAddOnuDialogOpen(true) },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add ONU") },
                    text = { Text("Add ONU") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_onu_fab")
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Multiple OLT selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MULTIPLE OLT MONITORING",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Central OLT Fleet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.openVsolRouterConfig() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("header_vsol_router_config_btn")
                        ) {
                            Icon(Icons.Default.Router, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Router Config", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.setVsolOntSetupDialogOpen(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("header_vsol_ont_setup_btn")
                        ) {
                            Icon(Icons.Default.SettingsEthernet, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ONT Setup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.setAddOltDialogOpen(true) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_olt_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New OLT", fontSize = 11.sp)
                        }
                    }
                }
            }

            // OLT Selector Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(olts) { olt ->
                        val isSelected = olt.id == activeOlt?.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectOlt(olt.id) },
                            label = {
                                Text(
                                    text = "${olt.name} (${olt.vendor.displayName})",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (olt.status) {
                                                DeviceStatus.ONLINE -> NocSuccessGreen
                                                DeviceStatus.WARNING -> NocWarningAmber
                                                DeviceStatus.CRITICAL -> NocCriticalRed
                                                DeviceStatus.OFFLINE -> OpticalLosGray
                                                else -> NocSuccessGreen
                                            }
                                        )
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

            // Selected OLT Details Card
            if (activeOlt != null) {
                item {
                    OltStatusCard(
                        olt = activeOlt,
                        onProbeLive = { viewModel.probeSingleOltLive(activeOlt.id) },
                        onOpenSnmpPortSettings = {
                            viewModel.selectSettingsOlt(activeOlt.id)
                            viewModel.selectTab(com.example.ui.NocNavTab.SETTINGS)
                        },
                        onRestoreOnline = { viewModel.restoreOltOnline(activeOlt.id) },
                        onDelete = { viewModel.deleteOlt(activeOlt.id) }
                    )
                }
            }

            // ==========================================
            // V-SOL INTEGRATION ECOSYSTEM CARD
            // ==========================================
            item {
                VsolEcosystemCard(
                    activeOlt = activeOlt,
                    unregOnus = uiState.vsolUnregisteredOnus,
                    auditResult = uiState.vsolBatchAudit,
                    onOpenSetup = { unreg ->
                        viewModel.setVsolOntSetupDialogOpen(true, unreg)
                    },
                    onRunAudit = {
                        activeOlt?.id?.let { viewModel.runVsolBatchAudit(it) }
                    }
                )
            }

            // Search & Filters Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.onuSearchQuery,
                        onValueChange = { viewModel.setOnuSearch(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onu_search_input"),
                        placeholder = { Text("Search subscriber, PPPoE user, or Serial...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (uiState.onuSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setOnuSearch("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Optical Health Quick Filters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.onuStatusFilter == null,
                            onClick = { viewModel.setOnuFilter(null) },
                            label = { Text("All (${onus.count { it.oltId == activeOlt?.id }})") }
                        )
                        FilterChip(
                            selected = uiState.onuStatusFilter == OnuStatus.CRITICAL,
                            onClick = { viewModel.setOnuFilter(if (uiState.onuStatusFilter == OnuStatus.CRITICAL) null else OnuStatus.CRITICAL) },
                            label = { Text("Crit / LOS") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFE4E6),
                                selectedLabelColor = NocCriticalRed
                            )
                        )
                        FilterChip(
                            selected = uiState.onuStatusFilter == OnuStatus.WARNING,
                            onClick = { viewModel.setOnuFilter(if (uiState.onuStatusFilter == OnuStatus.WARNING) null else OnuStatus.WARNING) },
                            label = { Text("Low Power") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFEF3C7),
                                selectedLabelColor = NocWarningAmber
                            )
                        )
                        FilterChip(
                            selected = uiState.onuStatusFilter == OnuStatus.ONLINE,
                            onClick = { viewModel.setOnuFilter(if (uiState.onuStatusFilter == OnuStatus.ONLINE) null else OnuStatus.ONLINE) },
                            label = { Text("Optimal") }
                        )
                    }

                    // PON Port Chips
                    if (filteredPorts.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = uiState.selectedPonPort == null,
                                    onClick = { viewModel.selectPonPort(null) },
                                    label = { Text("All PONs") }
                                )
                            }
                            items(filteredPorts) { port ->
                                FilterChip(
                                    selected = uiState.selectedPonPort == port.name,
                                    onClick = { viewModel.selectPonPort(if (uiState.selectedPonPort == port.name) null else port.name) },
                                    label = { Text("PON ${port.name} (${port.onlineOnus}/${port.totalOnus})") }
                                )
                            }
                        }
                    }
                }
            }

            // ONUs List Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SUBSCRIBER OPTICAL UNITS (${displayOnus.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (displayOnus.isEmpty()) {
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
                            Icon(Icons.Default.SearchOff, contentDescription = null, tint = NocTextMuted, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No ONUs found matching your filter", color = NocTextSecondary)
                        }
                    }
                }
            } else {
                items(displayOnus) { onu ->
                    OnuCardItem(
                        onu = onu,
                        onInspect = { viewModel.inspectOnu(onu) },
                        onDegrade = { viewModel.triggerOpticalDegradation(onu.id) },
                        onDelete = { viewModel.deleteOnu(onu.id) }
                    )
                }
            }
        }
    }

    // Modal Dialogs
    if (uiState.inspectingOnu != null) {
        val targetOnu = onus.find { it.id == uiState.inspectingOnu!!.id } ?: uiState.inspectingOnu!!
        val historyForOnu = onuPowerHistory.filter { it.onuId == targetOnu.id }
        OnuDiagnosticsDialog(
            onu = targetOnu,
            historyPoints = historyForOnu,
            nominalPowerDbm = thresholds.onuNominalPowerDbm,
            toleranceDb = thresholds.onuNominalToleranceDb,
            onTriggerLowPower = { viewModel.triggerOpticalDegradation(targetOnu.id) },
            onTriggerHighPower = { viewModel.triggerHighOpticalOverload(targetOnu.id) },
            onDismiss = { viewModel.inspectOnu(null) }
        )
    }

    if (uiState.isAddOltDialogOpen) {
        AddOltDialog(
            onDismiss = { viewModel.setAddOltDialogOpen(false) },
            onAdd = { viewModel.addOlt(it) }
        )
    }

    if (uiState.isAddOnuDialogOpen && activeOlt != null) {
        AddOnuDialog(
            oltId = activeOlt.id,
            onDismiss = { viewModel.setAddOnuDialogOpen(false) },
            onAdd = { viewModel.addOnu(it) }
        )
    }

    // V-SOL ONT Setup Option Dialog
    if (uiState.isVsolOntSetupDialogOpen) {
        VsolOntSetupDialog(
            activeOltId = activeOlt?.id ?: "olt-vsol-03",
            unregOnu = uiState.selectedVsolUnregOnu,
            onDismiss = { viewModel.setVsolOntSetupDialogOpen(false) },
            onProvision = { onu ->
                viewModel.provisionVsolOnt(onu)
                Toast.makeText(context, "V-SOL ONT Authorized: ${onu.serialNumber}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // V-SOL Router Configuration Suite Dialog
    if (uiState.isVsolRouterConfigDialogOpen) {
        VsolRouterConfigDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeVsolRouterConfig() }
        )
    }
}

/**
 * V-SOL Integration Ecosystem Card
 * Displays V-SOL OLT models, BS-EMS Web GUI bridge, and Unregistered ONUs Queue
 */
@Composable
fun VsolEcosystemCard(
    activeOlt: OltDevice?,
    unregOnus: List<VsolUnregisteredOnu>,
    auditResult: VsolBatchAuditResult?,
    onOpenSetup: (VsolUnregisteredOnu?) -> Unit,
    onRunAudit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                            .background(Color(0xFFCCFBF1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SettingsEthernet,
                            contentDescription = null,
                            tint = Color(0xFF0F766E),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "V-SOL Integration Ecosystem",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "BS-EMS Web GUI • OMCI Auto-Find • V2804/V2802",
                            style = MaterialTheme.typography.labelSmall,
                            color = NocTextSecondary
                        )
                    }
                }

                Button(
                    onClick = { onOpenSetup(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("open_vsol_setup_btn")
                ) {
                    Text("+ Setup ONT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Unregistered ONUs Queue (Auto-Find)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "UNREGISTERED V-SOL ONTS (${unregOnus.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                TextButton(
                    onClick = onRunAudit,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Optical Batch Audit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (unregOnus.isEmpty()) {
                Text(
                    text = "No pending unregistered ONTs detected on PON ports.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NocTextSecondary
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    unregOnus.forEach { unreg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${unreg.model} • PON ${unreg.ponPort}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "SN: ${unreg.serialNumber}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Button(
                                onClick = { onOpenSetup(unreg) },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Setup Option", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Batch Audit Result if available
            if (auditResult != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("V-SOL Audit Scan", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Avg Rx: ${String.format("%.1f", auditResult.averageRxDbm)} dBm",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${auditResult.optimalCount} Optimal | ${auditResult.warningCount} Low | ${auditResult.criticalCount} Crit",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (auditResult.criticalCount > 0) NocCriticalRed else NocSuccessGreen
                            )
                            Text("Scanned ${auditResult.totalOnus} ONUs", style = MaterialTheme.typography.labelSmall, color = NocTextSecondary)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated V-SOL ONT Setup Option Dialog
 * Configures model, PPPoE credentials, VLAN mode, Line Profile, CATV RF switch, and generates CLI script
 */
@Composable
fun VsolOntSetupDialog(
    activeOltId: String,
    unregOnu: VsolUnregisteredOnu?,
    onDismiss: () -> Unit,
    onProvision: (OnuDevice) -> Unit
) {
    val context = LocalContext.current

    var customerName by remember { mutableStateOf(if (unregOnu != null) "New Subscriber" else "Corporate Office") }
    var pppoeAccount by remember { mutableStateOf("user_vsol_99") }
    var pppoePassword by remember { mutableStateOf("pass1234") }
    var serialNumber by remember { mutableStateOf(unregOnu?.serialNumber ?: "VSOL-9921-A101") }
    var ponPort by remember { mutableStateOf(unregOnu?.ponPort ?: "PON-1") }
    var selectedModel by remember { mutableStateOf(unregOnu?.model ?: "V2804AX (WiFi 6 GPON)") }

    var wanMode by remember { mutableStateOf("PPPoE") }
    var vlanId by remember { mutableStateOf("100") }
    var vlanMode by remember { mutableStateOf("Tag (802.1Q)") }
    var lineProfile by remember { mutableStateOf("VSOL_100M_PROFILE") }
    var serviceProfile by remember { mutableStateOf("VSOL_SRV_DEFAULT") }
    var catvEnabled by remember { mutableStateOf(true) }
    var wifiSsid by remember { mutableStateOf("CyberCity_Fiber") }
    var speedPlan by remember { mutableStateOf("200 Mbps") }

    val isCatvModel = selectedModel.contains("CATV") || selectedModel.contains("V2802RH")

    // Generate V-SOL OLT CLI script
    val generatedCli = remember(serialNumber, ponPort, customerName, pppoeAccount, pppoePassword, vlanId, catvEnabled, isCatvModel) {
        val portNum = ponPort.filter { it.isDigit() }.ifEmpty { "1" }
        """
        ! V-SOL OLT Configuration Script
        interface gpon 0/$portNum
        ont add 1 sn-auth "$serialNumber" ont-lineprofile-name "$lineProfile" ont-srvprofile-name "$serviceProfile" desc "$customerName"
        ont port native-vlan 1 eth 1 vlan $vlanId
        ont ipconfig 1 ip-index 1 mode pppoe user "$pppoeAccount" password "$pppoePassword"
        ${if (isCatvModel && catvEnabled) "ont catv 1 enable" else if (isCatvModel) "ont catv 1 disable" else "! No CATV module"}
        """.trimIndent()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SettingsEthernet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("V-SOL ONT Setup Option", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Model Chips
                item {
                    Text("Select V-SOL ONT Hardware:", style = MaterialTheme.typography.labelSmall, color = NocTextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "V2804AX (WiFi 6)",
                            "V2802RH (CATV)",
                            "V2801SG (1GE)"
                        ).forEach { m ->
                            FilterChip(
                                selected = selectedModel.contains(m.substringBefore(" ")),
                                onClick = { selectedModel = m },
                                label = { Text(m, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Basic Identification
                item {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Customer Name / Account") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = serialNumber,
                            onValueChange = { serialNumber = it },
                            label = { Text("ONT SN / MAC") },
                            singleLine = true,
                            modifier = Modifier.weight(1.5f)
                        )
                        OutlinedTextField(
                            value = ponPort,
                            onValueChange = { ponPort = it },
                            label = { Text("PON Port") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // WAN & PPPoE Option
                item {
                    Text("WAN Connection Mode:", style = MaterialTheme.typography.labelSmall, color = NocTextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("PPPoE", "Bridge", "Static IP").forEach { mode ->
                            FilterChip(
                                selected = wanMode == mode,
                                onClick = { wanMode = mode },
                                label = { Text(mode, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                if (wanMode == "PPPoE") {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = pppoeAccount,
                                onValueChange = { pppoeAccount = it },
                                label = { Text("PPPoE Username") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = pppoePassword,
                                onValueChange = { pppoePassword = it },
                                label = { Text("Password") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // VLAN & Profiles
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = vlanId,
                            onValueChange = { vlanId = it },
                            label = { Text("Internet VLAN") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = speedPlan,
                            onValueChange = { speedPlan = it },
                            label = { Text("Plan Speed") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // CATV RF Switch option for V2802RH
                if (isCatvModel) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("CATV / RF Receiver Output", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text("Remote enable RF optical receiver port", style = MaterialTheme.typography.labelSmall, color = NocTextSecondary)
                                }
                                Switch(
                                    checked = catvEnabled,
                                    onCheckedChange = { catvEnabled = it }
                                )
                            }
                        }
                    }
                }

                // CLI Script Preview
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Generated V-SOL OLT CLI Script:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            TextButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("VSOL CLI", generatedCli))
                                    Toast.makeText(context, "V-SOL CLI copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy CLI", fontSize = 11.sp)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = generatedCli,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color(0xFF1E293B),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newOnu = OnuDevice(
                        id = "vsol-onu-${System.currentTimeMillis()}",
                        oltId = activeOltId,
                        ponPort = ponPort,
                        onuIndex = 1,
                        customerName = customerName,
                        pppoeAccount = pppoeAccount,
                        serialNumber = serialNumber,
                        rxPowerDbm = -21.4f,
                        txPowerDbm = 2.4f,
                        oltRxPowerDbm = -22.1f,
                        distanceMeters = 1150,
                        status = OnuStatus.ONLINE,
                        planSpeed = "$speedPlan ($selectedModel)"
                    )
                    onProvision(newOnu)
                }
            ) {
                Text("Authorize & Save ONT")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun OltStatusCard(
    olt: OltDevice,
    onProbeLive: () -> Unit = {},
    onOpenSnmpPortSettings: () -> Unit = {},
    onRestoreOnline: () -> Unit = {},
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOffline = olt.status == DeviceStatus.OFFLINE
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("olt_status_card"),
        colors = CardDefaults.cardColors(
            containerColor = if (isOffline) NocCriticalRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isOffline)
            androidx.compose.foundation.BorderStroke(1.5.dp, NocCriticalRed)
        else CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isOffline) Color(0xFFFFE4E6) else MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isOffline) Icons.Default.Warning else Icons.Default.Hub,
                            contentDescription = null,
                            tint = if (isOffline) NocCriticalRed else MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = olt.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isOffline) NocCriticalRed else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = if (isOffline) NocCriticalRed else Color(0xFFECFDF5),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isOffline) "OFFLINE ALERT" else "ONLINE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOffline) Color.White else NocSuccessGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${olt.vendor.displayName} ${olt.model} • ${olt.ipAddress}:${olt.port}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = NocTextSecondary
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete OLT", tint = NocCriticalRed)
                }
            }

            if (isOffline) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NocCriticalRed)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CRITICAL: OLT OFFLINE ALERT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "SNMP/ICMP unreachable (100% packet loss). Email & Webhook alert dispatched.",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    Button(
                        onClick = onRestoreOnline,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = NocCriticalRed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("restore_olt_online_btn")
                    ) {
                        Text("Restore Online", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Resource indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricPill(label = "CPU LOAD", value = "${olt.cpuUsage}%", isAlert = olt.cpuUsage > 75 || isOffline)
                MetricPill(label = "BW UTIL", value = "${String.format("%.1f", olt.bandwidthUtilizationPercent)}%", isAlert = olt.bandwidthUtilizationPercent > 80f || isOffline)
                MetricPill(label = "LATENCY", value = if (isOffline) "TIMEOUT" else "${String.format("%.1f", olt.avgLatencyMs)}ms", isAlert = olt.avgLatencyMs > 30f || isOffline)
                MetricPill(label = "PKT LOSS", value = "${String.format("%.2f", olt.packetLossPercent)}%", isAlert = olt.packetLossPercent > 1.0f || isOffline)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Location: ${olt.location} • ${olt.totalPonPorts} PON Ports (SNMP: ${olt.snmpCommunity})",
                    style = MaterialTheme.typography.bodySmall,
                    color = NocTextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onProbeLive,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("probe_olt_live_btn")
                    ) {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Live Probe", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onOpenSnmpPortSettings,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("configure_olt_snmp_ports_btn")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SNMP & Ports", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MetricPill(
    label: String,
    value: String,
    isAlert: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = NocTextSecondary, fontSize = 10.sp)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (isAlert) NocCriticalRed else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun OnuCardItem(
    onu: OnuDevice,
    onInspect: () -> Unit,
    onDegrade: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val devDb = onu.getNominalDeviationDb()
    val isOut = onu.isOutsideNominalRange(3.0f)
    val devSign = if (devDb >= 0f) "+" else ""

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onInspect() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = onu.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "PON ${onu.ponPort} • PPPoE: ${onu.pppoeAccount} • ${onu.planSpeed}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NocTextSecondary
                    )
                    Text(
                        text = "SN: ${onu.serialNumber} • Latency: ${String.format("%.1f", onu.avgLatencyMs)}ms • Loss: ${String.format("%.1f", onu.packetLossPercent)}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = if (isOut) Color(0xFFFFE4E6) else Color(0xFFECFDF5),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (onu.status == OnuStatus.OFFLINE) "LOS"
                                   else "${devSign}${String.format("%.1f", devDb)} dB vs Nom",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isOut) NocCriticalRed else NocSuccessGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    IconButton(onClick = onInspect) {
                        Icon(Icons.Default.Visibility, contentDescription = "Inspect Trend", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Embedded Optical Power Meter
            OpticalPowerGauge(
                rxPowerDbm = onu.rxPowerDbm,
                txPowerDbm = onu.txPowerDbm,
                status = onu.status
            )
        }
    }
}

@Composable
fun OnuDiagnosticsDialog(
    onu: OnuDevice,
    historyPoints: List<OnuPowerHistoryPoint>,
    nominalPowerDbm: Float = -20.0f,
    toleranceDb: Float = 3.0f,
    onTriggerLowPower: () -> Unit = {},
    onTriggerHighPower: () -> Unit = {},
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ONU Power Trend & Diagnostics",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 460.dp)
            ) {
                item {
                    Text(
                        text = "${onu.customerName} (${onu.serialNumber})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    OpticalPowerGauge(
                        rxPowerDbm = onu.rxPowerDbm,
                        txPowerDbm = onu.txPowerDbm,
                        status = onu.status
                    )
                }

                // Historical Power Trend Chart with +/- 3 dBm Nominal Band
                item {
                    OnuPowerTrendChart(
                        historyPoints = historyPoints,
                        nominalPowerDbm = nominalPowerDbm,
                        toleranceDb = toleranceDb
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onTriggerLowPower,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Sim Low (-3dB)", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = onTriggerHighPower,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Sim High (+3dB)", fontSize = 11.sp)
                        }
                    }
                }

                item {
                    Divider(color = MaterialTheme.colorScheme.outline)
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Telemetry Parameters", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Nominal Reference Power:", color = NocTextSecondary)
                            Text("$nominalPowerDbm dBm (±$toleranceDb dB)", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("OLT Received Power (Rx):", color = NocTextSecondary)
                            Text("${onu.oltRxPowerDbm} dBm", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Average Latency / Loss:", color = NocTextSecondary)
                            Text("${String.format("%.1f", onu.avgLatencyMs)} ms / ${String.format("%.1f", onu.packetLossPercent)}%", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Optical Distance:", color = NocTextSecondary)
                            Text("${onu.distanceMeters} meters", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Laser Temperature:", color = NocTextSecondary)
                            Text("${onu.temperatureCelsius}°C", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (onu.lastOfflineReason != null) {
                    item {
                        Text(
                            text = "Fault Info: ${onu.lastOfflineReason}",
                            color = NocCriticalRed,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun AddOltDialog(
    onDismiss: () -> Unit,
    onAdd: (OltDevice) -> Unit
) {
    var name by remember { mutableStateOf("New-Edge-OLT") }
    var ip by remember { mutableStateOf("10.100.4.10") }
    var port by remember { mutableStateOf("23") }
    var model by remember { mutableStateOf("V1600G1-B") }
    var vendor by remember { mutableStateOf(OltVendor.VSOL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New OLT Instance", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("OLT Identifier") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP Address / Host") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Telnet/REST Port") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model Spec") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newOlt = OltDevice(
                        id = "olt-${System.currentTimeMillis()}",
                        name = name,
                        ipAddress = ip,
                        port = port.toIntOrNull() ?: 23,
                        vendor = vendor,
                        model = model,
                        totalPonPorts = 8
                    )
                    onAdd(newOlt)
                }
            ) {
                Text("Save OLT")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddOnuDialog(
    oltId: String,
    onDismiss: () -> Unit,
    onAdd: (OnuDevice) -> Unit
) {
    var customerName by remember { mutableStateOf("New Subscriber") }
    var pppoeAccount by remember { mutableStateOf("user_ftth_99") }
    var serialNumber by remember { mutableStateOf("HWTC-5522-XX00") }
    var ponPort by remember { mutableStateOf("0/1/1") }
    var rxPower by remember { mutableStateOf("-21.5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Provision New ONU", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Subscriber Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = pppoeAccount,
                    onValueChange = { pppoeAccount = it },
                    label = { Text("PPPoE Account") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = serialNumber,
                    onValueChange = { serialNumber = it },
                    label = { Text("ONT Serial Number / MAC") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = ponPort,
                    onValueChange = { ponPort = it },
                    label = { Text("PON Port (e.g. 0/1/1)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = rxPower,
                    onValueChange = { rxPower = it },
                    label = { Text("Rx Power (dBm, e.g. -21.5)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rx = rxPower.toFloatOrNull() ?: -22.0f
                    val newOnu = OnuDevice(
                        id = "onu-${System.currentTimeMillis()}",
                        oltId = oltId,
                        ponPort = ponPort,
                        onuIndex = 12,
                        customerName = customerName,
                        pppoeAccount = pppoeAccount,
                        serialNumber = serialNumber,
                        rxPowerDbm = rx,
                        status = if (rx < -27.5f) OnuStatus.CRITICAL else if (rx < -25.5f) OnuStatus.WARNING else OnuStatus.ONLINE
                    )
                    onAdd(newOnu)
                }
            ) {
                Text("Add ONU")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
