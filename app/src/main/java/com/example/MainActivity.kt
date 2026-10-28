package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AlertSeverity
import com.example.model.DeviceStatus
import com.example.ui.NocNavTab
import com.example.ui.NocViewModel
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    private val viewModel: NocViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val alerts by viewModel.alerts.collectAsState()
            val olts by viewModel.olts.collectAsState()
            val onus by viewModel.onus.collectAsState()

            val offlineOlts = olts.filter { it.status == DeviceStatus.OFFLINE }
            val unackedCount = alerts.count { !it.isAcknowledged }
            val hasCritical = alerts.any { it.severity == AlertSeverity.CRITICAL && !it.isAcknowledged } || offlineOlts.isNotEmpty()

            MyApplicationTheme(darkTheme = uiState.isDarkMode) {
                BackHandler(enabled = uiState.currentTab != NocNavTab.DASHBOARD) {
                    viewModel.selectTab(NocNavTab.DASHBOARD)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.NetworkCheck,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Cyber City",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "MONITORING TOOL",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                letterSpacing = 1.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    // Dark Mode Switch Pill
                                    Surface(
                                        onClick = { viewModel.toggleDarkMode() },
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.testTag("dark_mode_toggle_button")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (uiState.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                                contentDescription = "Toggle Dark Mode",
                                                tint = if (uiState.isDarkMode) NocWarningAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (uiState.isDarkMode) "DARK" else "LIGHT",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // SNMP & Port Settings Button
                                    IconButton(
                                        onClick = { viewModel.selectTab(NocNavTab.SETTINGS) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("top_bar_settings_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "SNMP & Port Settings",
                                            tint = if (uiState.currentTab == NocNavTab.SETTINGS)
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Live NOC status pill
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (hasCritical) Color(0xFFFFE4E6) else Color(0xFFECFDF5))
                                            .clickable { viewModel.selectTab(NocNavTab.ALERTS) }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(if (hasCritical) NocCriticalRed else NocSuccessGreen)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (offlineOlts.isNotEmpty()) "OLT OFFLINE" else if (hasCritical) "ALERT" else "ONLINE",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (hasCritical) NocCriticalRed else NocSuccessGreen
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            // Global Emergency OLT Offline Alert Banner
                            offlineOlts.forEach { offlineOlt ->
                                val impactedCount = onus.count { it.oltId == offlineOlt.id }
                                Surface(
                                    color = NocCriticalRed,
                                    contentColor = Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("global_olt_offline_alert_banner")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                Icons.Default.Warning,
                                                contentDescription = "OLT Offline Alert",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "OLT OFFLINE ALERT: ${offlineOlt.name} (${offlineOlt.ipAddress})",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "100% Packet Loss • $impactedCount Subscriber ONUs Impacted • Email & Webhook Sent",
                                                    fontSize = 10.sp,
                                                    color = Color.White.copy(alpha = 0.92f)
                                                )
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(
                                                onClick = {
                                                    viewModel.selectOlt(offlineOlt.id)
                                                    viewModel.selectTab(NocNavTab.OLT_ONU)
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color.White.copy(alpha = 0.2f),
                                                modifier = Modifier.testTag("global_olt_offline_view_btn")
                                            ) {
                                                Text(
                                                    text = "Inspect",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }

                                            Surface(
                                                onClick = { viewModel.restoreOltOnline(offlineOlt.id) },
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color.White,
                                                modifier = Modifier.testTag("global_olt_offline_restore_btn")
                                            ) {
                                                Text(
                                                    text = "Restore OLT",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NocCriticalRed,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 4.dp
                        ) {
                            NavigationBarItem(
                                selected = uiState.currentTab == NocNavTab.DASHBOARD,
                                onClick = { viewModel.selectTab(NocNavTab.DASHBOARD) },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                label = { Text("Overview", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                                alwaysShowLabel = true,
                                modifier = Modifier.testTag("nav_tab_dashboard"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )

                            NavigationBarItem(
                                selected = uiState.currentTab == NocNavTab.OLT_ONU,
                                onClick = { viewModel.selectTab(NocNavTab.OLT_ONU) },
                                icon = { Icon(Icons.Default.Hub, contentDescription = "OLT & ONUs") },
                                label = { Text("OLT/ONU", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                                alwaysShowLabel = true,
                                modifier = Modifier.testTag("nav_tab_olt"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )

                            NavigationBarItem(
                                selected = uiState.currentTab == NocNavTab.MIKROTIK,
                                onClick = { viewModel.selectTab(NocNavTab.MIKROTIK) },
                                icon = { Icon(Icons.Default.Router, contentDescription = "MikroTik") },
                                label = { Text("MikroTik", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                                alwaysShowLabel = true,
                                modifier = Modifier.testTag("nav_tab_mikrotik"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )

                            NavigationBarItem(
                                selected = uiState.currentTab == NocNavTab.LIVE_MAP,
                                onClick = { viewModel.selectTab(NocNavTab.LIVE_MAP) },
                                icon = { Icon(Icons.Default.Map, contentDescription = "Live Map") },
                                label = { Text("Live Map", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                                alwaysShowLabel = true,
                                modifier = Modifier.testTag("nav_tab_live_map"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )

                            NavigationBarItem(
                                selected = uiState.currentTab == NocNavTab.ISP_PORTALS,
                                onClick = { viewModel.selectTab(NocNavTab.ISP_PORTALS) },
                                icon = { Icon(Icons.Default.Language, contentDescription = "ISP FMS") },
                                label = { Text("ISP FMS", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                                alwaysShowLabel = true,
                                modifier = Modifier.testTag("nav_tab_isp_portals"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )

                            NavigationBarItem(
                                selected = uiState.currentTab == NocNavTab.TOOLS || uiState.currentTab == NocNavTab.ALERTS || uiState.currentTab == NocNavTab.SETTINGS,
                                onClick = { viewModel.selectTab(NocNavTab.TOOLS) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (unackedCount > 0) {
                                                Badge(
                                                    containerColor = if (hasCritical) NocCriticalRed else NocWarningAmber
                                                ) {
                                                    Text("$unackedCount")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Build, contentDescription = "Tools & Probe")
                                    }
                                },
                                label = { Text("Tools", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                                alwaysShowLabel = true,
                                modifier = Modifier.testTag("nav_tab_tools"),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    AnimatedContent(
                        targetState = uiState.currentTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "tab_switch"
                    ) { tab ->
                        when (tab) {
                            NocNavTab.DASHBOARD -> DashboardScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                            NocNavTab.OLT_ONU -> OltMonitoringScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                            NocNavTab.MIKROTIK -> MikrotikMonitoringScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                            NocNavTab.LIVE_MAP -> CustomerLiveMapScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                            NocNavTab.ISP_PORTALS -> IspPortalsScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                            NocNavTab.ALERTS -> AlertsScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                            NocNavTab.TOOLS -> DiagnosticsScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                            NocNavTab.SETTINGS -> SettingsScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}
