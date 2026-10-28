package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.VsolRouterConfig
import com.example.model.VsolWanMode
import com.example.ui.NocViewModel
import com.example.ui.theme.NocPrimaryBlue
import com.example.ui.theme.NocSuccessGreen
import com.example.ui.theme.NocWarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VsolRouterConfigDialog(
    viewModel: NocViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val config = uiState.activeVsolRouterConfig

    var serialNumber by remember { mutableStateOf(config.serialNumber) }
    var model by remember { mutableStateOf(config.model) }
    var targetCustomerName by remember { mutableStateOf(config.targetCustomerName) }
    var wanMode by remember { mutableStateOf(config.wanMode) }
    var pppoeUsername by remember { mutableStateOf(config.pppoeUsername) }
    var pppoePassword by remember { mutableStateOf(config.pppoePassword) }
    var showPppoePassword by remember { mutableStateOf(false) }
    var vlanId by remember { mutableStateOf(config.vlanId.toString()) }
    var vlanPriority by remember { mutableStateOf(config.vlanPriority.toString()) }

    var wifiSsid24 by remember { mutableStateOf(config.wifiSsid24) }
    var wifiSsid5 by remember { mutableStateOf(config.wifiSsid5) }
    var wifiPassword by remember { mutableStateOf(config.wifiPassword) }
    var showWifiPassword by remember { mutableStateOf(false) }

    var catvRfEnabled by remember { mutableStateOf(config.catvRfEnabled) }
    var lanIpAddress by remember { mutableStateOf(config.lanIpAddress) }
    var remoteWanAccess by remember { mutableStateOf(config.remoteWanAccess) }
    var tr069AcsUrl by remember { mutableStateOf(config.tr069AcsUrl) }

    var selectedSection by remember { mutableIntStateOf(0) } // 0: WAN/VLAN, 1: WiFi 6, 2: CATV/LAN, 3: CLI Script

    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Router,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "V-SOL Router Configuration",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$model • SN: $serialNumber",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                // Quick ISP Presets Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = "QUICK ISP PRESETS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "BSNL" to "BSNL (VLAN 100)",
                                "MEGHBELA" to "Meghbela (VLAN 200)",
                                "GTPL" to "GTPL (DHCP 300)",
                                "CYBER" to "CyberCity (VLAN 500)"
                            ).forEach { (presetKey, label) ->
                                OutlinedButton(
                                    onClick = {
                                        viewModel.loadVsolPreset(presetKey)
                                        val p = uiState.activeVsolRouterConfig
                                        wanMode = p.wanMode
                                        vlanId = p.vlanId.toString()
                                        vlanPriority = p.vlanPriority.toString()
                                        pppoeUsername = p.pppoeUsername
                                        wifiSsid24 = p.wifiSsid24
                                        wifiSsid5 = p.wifiSsid5
                                        Toast.makeText(context, "Loaded $presetKey preset!", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(28.dp).weight(1f)
                                ) {
                                    Text(label.substringBefore(" ("), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Section Tabs: WAN/VLAN, WiFi 6, CATV/LAN, CLI
                TabRow(
                    selectedTabIndex = selectedSection,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedSection == 0,
                        onClick = { selectedSection = 0 },
                        text = { Text("WAN & VLAN", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedSection == 1,
                        onClick = { selectedSection = 1 },
                        text = { Text("WiFi 6", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedSection == 2,
                        onClick = { selectedSection = 2 },
                        text = { Text("CATV & LAN", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedSection == 3,
                        onClick = { selectedSection = 3 },
                        text = { Text("CLI Script", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                // Main Form Content Scrollable Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Success Message Banner if applied
                    uiState.vsolConfigSuccessMessage?.let { msg ->
                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NocSuccessGreen.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NocSuccessGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    fontSize = 12.sp,
                                    color = Color(0xFF065F46),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    when (selectedSection) {
                        0 -> {
                            // Section 0: WAN & VLAN Configuration
                            Text(
                                text = "WAN CONNECTION MODE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VsolWanMode.values().forEach { mode ->
                                    val isSelected = wanMode == mode
                                    Surface(
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { wanMode = mode }
                                    ) {
                                        Text(
                                            text = mode.label.substringBefore(" "),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }

                            if (wanMode == VsolWanMode.PPPOE) {
                                OutlinedTextField(
                                    value = pppoeUsername,
                                    onValueChange = { pppoeUsername = it },
                                    label = { Text("PPPoE Dial-in Username") },
                                    leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("vsol_pppoe_user_input")
                                )

                                OutlinedTextField(
                                    value = pppoePassword,
                                    onValueChange = { pppoePassword = it },
                                    label = { Text("PPPoE Password") },
                                    leadingIcon = { Icon(Icons.Default.Password, contentDescription = null) },
                                    singleLine = true,
                                    visualTransformation = if (showPppoePassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showPppoePassword = !showPppoePassword }) {
                                            Icon(
                                                if (showPppoePassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("vsol_pppoe_pass_input")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = vlanId,
                                    onValueChange = { vlanId = it },
                                    label = { Text("VLAN ID (1-4094)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = vlanPriority,
                                    onValueChange = { vlanPriority = it },
                                    label = { Text("802.1p Priority (0-7)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        1 -> {
                            // Section 1: WiFi 6 Dual-Band Settings
                            Text(
                                text = "V-SOL DUAL-BAND WIFI 6 CONFIGURATION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = wifiSsid24,
                                onValueChange = { wifiSsid24 = it },
                                label = { Text("2.4 GHz WiFi SSID") },
                                leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = wifiSsid5,
                                onValueChange = { wifiSsid5 = it },
                                label = { Text("5.0 GHz WiFi 6 SSID (802.11ax)") },
                                leadingIcon = { Icon(Icons.Default.WifiTethering, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = wifiPassword,
                                onValueChange = { wifiPassword = it },
                                label = { Text("WPA2/WPA3 WiFi Passphrase") },
                                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                                singleLine = true,
                                visualTransformation = if (showWifiPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showWifiPassword = !showWifiPassword }) {
                                        Icon(
                                            if (showWifiPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("WiFi Capabilities: OFDMA, MU-MIMO 2x2, 1024-QAM, 160MHz Bandwidth support enabled on V2804AX.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        2 -> {
                            // Section 2: CATV RF & LAN Setup
                            Text(
                                text = "CATV RF RECEIVER & LAN SETTINGS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // CATV RF Switch
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("CATV Optical RF Output", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Control RF switch for Cable TV distribution (V2802RH)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = catvRfEnabled,
                                        onCheckedChange = { catvRfEnabled = it }
                                    )
                                }
                            }

                            // Remote WAN Access Switch
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Remote WAN Web Management", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Allow NOC access to router WebGUI on port 80/8080", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = remoteWanAccess,
                                        onCheckedChange = { remoteWanAccess = it }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = lanIpAddress,
                                onValueChange = { lanIpAddress = it },
                                label = { Text("Router LAN IP Address") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = tr069AcsUrl,
                                onValueChange = { tr069AcsUrl = it },
                                label = { Text("TR-069 ACS URL (Auto-Provisioning)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        3 -> {
                            // Section 3: Generated V-SOL OLT / ONT CLI Script
                            val cliScript = """
# ========================================================
# V-SOL OLT / ONT PROVISIONING CLI SCRIPT
# Generated for: $model (SN: $serialNumber)
# Target Customer: $targetCustomerName
# ========================================================

configure terminal
ont-profile wan add profile-id 10
 wan-mode ${wanMode.name.lowercase()}
 vlan-id ${vlanId} priority ${vlanPriority}
${if (wanMode == VsolWanMode.PPPOE) " username \"$pppoeUsername\" password \"$pppoePassword\"" else " ip-mode dhcp"}
 exit

# Bind to ONT on OLT PON Port
interface gpon 0/1
 ont add 1 sn-auth $serialNumber omci ont-lineprofile-id 1 ont-srvprofile-id 1
 ont description 1 1 "$targetCustomerName"
 ont wan-config 1 1 profile-id 10
${if (catvRfEnabled) " ont catv 1 1 enable" else " ont catv 1 1 disable"}
 ont wifi 1 1 ssid-2.4g "$wifiSsid24" ssid-5g "$wifiSsid5" key "$wifiPassword"
 ont tr069 1 1 acs-url "$tr069AcsUrl"
 write
                            """.trimIndent()

                            Text(
                                text = "AUTOMATED V-SOL CLI PROVISIONING SCRIPT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = cliScript,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color(0xFFE2E8F0)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            clipboardManager.setPrimaryClip(ClipData.newPlainText("V-SOL CLI Script", cliScript))
                                            Toast.makeText(context, "V-SOL CLI script copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.align(Alignment.End).height(32.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy Script", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action Buttons: Cancel & Push Config
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val updated = config.copy(
                                    serialNumber = serialNumber,
                                    model = model,
                                    targetCustomerName = targetCustomerName,
                                    wanMode = wanMode,
                                    pppoeUsername = pppoeUsername,
                                    pppoePassword = pppoePassword,
                                    vlanId = vlanId.toIntOrNull() ?: 100,
                                    vlanPriority = vlanPriority.toIntOrNull() ?: 0,
                                    wifiSsid24 = wifiSsid24,
                                    wifiSsid5 = wifiSsid5,
                                    wifiPassword = wifiPassword,
                                    catvRfEnabled = catvRfEnabled,
                                    lanIpAddress = lanIpAddress,
                                    remoteWanAccess = remoteWanAccess,
                                    tr069AcsUrl = tr069AcsUrl
                                )
                                viewModel.updateActiveVsolConfig(updated)
                                viewModel.applyVsolRouterConfig()
                                Toast.makeText(context, "Applied V-SOL configuration to $serialNumber!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(2f).testTag("push_vsol_config_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Push & Save Config", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
