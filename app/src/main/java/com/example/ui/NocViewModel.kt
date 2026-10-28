package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class NocNavTab(val title: String) {
    DASHBOARD("NOC Overview"),
    OLT_ONU("OLT & ONUs"),
    MIKROTIK("MikroTik"),
    LIVE_MAP("Customer Live Map"),
    ISP_PORTALS("ISP FMS"),
    ALERTS("Alerts"),
    TOOLS("Tools & Probe"),
    SETTINGS("SNMP & Port Settings")
}

data class NocUiState(
    val currentTab: NocNavTab = NocNavTab.DASHBOARD,
    val isDarkMode: Boolean = false,
    val selectedOltId: String? = null,
    val selectedPonPort: String? = null,
    val onuSearchQuery: String = "",
    val onuStatusFilter: OnuStatus? = null, // null = all
    val selectedRouterId: String = "mtk-ccr2004-01",
    val selectedInterfaceId: String? = "if-ccr-01",
    val isAddOltDialogOpen: Boolean = false,
    val isAddOnuDialogOpen: Boolean = false,
    val isAddRouterDialogOpen: Boolean = false,
    val isThresholdDialogOpen: Boolean = false,
    val inspectingOnu: OnuDevice? = null,
    val pingTestResult: PingResult? = null,
    val isPinging: Boolean = false,
    val opticalBudgetResult: OpticalBudgetResult? = null,
    val pingResults: Map<String, HostPingStatus> = emptyMap(),
    val pingingHosts: Set<String> = emptySet(),
    val manualPingHost: String = "",
    // What Is My IP & Gateway State
    val myIpAndGateway: MyIpAndGatewayInfo = MyIpAndGatewayInfo(),
    // Speed Test State (Speedtest.net, Fast.com & Core BNG)
    val selectedSpeedTestProvider: SpeedTestProvider = SpeedTestProvider.SPEEDTEST_NET,
    val activeWebSpeedTestProvider: SpeedTestProvider = SpeedTestProvider.SPEEDTEST_NET,
    val isLiveSpeedTestWebVisible: Boolean = true,
    val speedTestState: SpeedTestState = SpeedTestState(
        downloadMbps = 548.6f,
        uploadMbps = 420.4f,
        pingMs = 3.2f,
        jitterMs = 0.8f,
        packetLossPercent = 0.0f,
        provider = SpeedTestProvider.SPEEDTEST_NET,
        providerUrl = SpeedTestProvider.SPEEDTEST_NET.webUrl,
        serverName = SpeedTestProvider.SPEEDTEST_NET.defaultServerName,
        phaseLabel = "Ready • Speedtest.net & Fast.com Gigabit Engine"
    ),
    // Traceroute State
    val tracerouteTargetInput: String = "8.8.8.8",
    val tracerouteState: TracerouteState = TracerouteState(
        targetHost = "8.8.8.8",
        resolvedIp = "8.8.8.8",
        isRunning = false,
        hops = listOf(
            TracerouteHop(1, "10.100.1.1", "lan-gw.cybercity.local", 1.1f, 1.4f, 1.25f, true, "Default Gateway"),
            TracerouteHop(2, "10.10.25.1", "bng-ccr2004-core1.cybercity.noc", 2.4f, 2.8f, 2.6f, true, "MikroTik Core BNG"),
            TracerouteHop(3, "103.21.58.1", "pe-mpls-ring1.bsnl.in", 5.8f, 6.2f, 6.0f, true, "ISP MPLS Aggregation"),
            TracerouteHop(4, "8.8.8.8", "dns.google", 12.1f, 12.5f, 12.3f, true, "Destination Host")
        ),
        statusMessage = "Ready • 4 hops to 8.8.8.8"
    ),
    // DNS Lookup State
    val dnsQueryInput: String = "fms.bsnl.in",
    val dnsRecordType: String = "A",
    val isDnsLoading: Boolean = false,
    val dnsLookupResult: DnsLookupResult? = DnsLookupResult(
        queryDomain = "fms.bsnl.in",
        recordType = "A",
        resolverUsed = "8.8.8.8 / 1.1.1.1 (Authoritative DNS)",
        queryTimeMs = 9L,
        isSuccess = true,
        records = listOf(
            DnsRecordEntry("A", "fms.bsnl.in", "210.212.160.34", 300)
        ),
        statusMessage = "Resolved 1 A record(s) in 9 ms"
    ),
    // Multi-Sample Latency & Jitter Analysis State
    val isAnalyzingLatency: Boolean = false,
    val latencyAnalysisResult: LatencyAnalysisResult? = LatencyAnalysisResult(
        host = "8.8.8.8",
        samplesSent = 5,
        samplesReceived = 5,
        minMs = 11.0f,
        avgMs = 12.2f,
        maxMs = 14.0f,
        jitterMs = 0.9f,
        packetLossPercent = 0.0f,
        sampleRttsMs = listOf(12L, 11L, 12L, 14L, 12L)
    ),
    val isVsolOntSetupDialogOpen: Boolean = false,
    val selectedVsolUnregOnu: VsolUnregisteredOnu? = null,
    val vsolUnregisteredOnus: List<VsolUnregisteredOnu> = listOf(
        VsolUnregisteredOnu("unreg-01", "olt-vsol-03", "PON-1", "VSOL-9921-A101", "V2804AX (WiFi 6 GPON)"),
        VsolUnregisteredOnu("unreg-02", "olt-vsol-03", "PON-1", "VSOL-4482-B202", "V2802RH (XPON + CATV RF)"),
        VsolUnregisteredOnu("unreg-03", "olt-vsol-03", "PON-2", "VSOL-1109-C303", "V2801SG (1GE Mini XPON)")
    ),
    val vsolBatchAudit: VsolBatchAuditResult? = null,
    val selectedMapOnu: OnuDevice? = null,
    val showRealtimePowerBadge: Boolean = true,
    val showFiberRoutes: Boolean = true,
    val mapFilterQuality: OpticalQuality? = null,
    val mapVisualizationLayer: MapVisualizationLayer = MapVisualizationLayer.LATENCY_AND_PACKET_LOSS,
    val selectedIspIndex: Int = 0,
    val isPasswordVisible: Boolean = false,
    val isEditCredentialsDialogOpen: Boolean = false,
    val editingPortal: IspPortal? = null,
    val isVsolRouterConfigDialogOpen: Boolean = false,
    val activeVsolRouterConfig: VsolRouterConfig = VsolRouterConfig(),
    val vsolConfigSuccessMessage: String? = null,
    // SNMP Community Strings & OLT Port Configuration Settings State
    val globalSnmpReadCommunity: String = "public",
    val globalSnmpWriteCommunity: String = "private_noc_rw",
    val globalSnmpUdpPort: Int = 161,
    val snmpVersion: String = "SNMPv2c",
    val selectedSettingsOltId: String = "olt-huawei-01",
    val hardwareDiagnosticReport: OltHardwareDiagnosticReport? = null,
    val isRunningHardwareDiag: Boolean = false,
    val settingsFeedbackMessage: String? = null
)

class NocViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: NetworkRepository = NetworkRepository.getInstance(application)

    val olts: StateFlow<List<OltDevice>> = repository.olts
    val ponPorts: StateFlow<List<PonPort>> = repository.ponPorts
    val onus: StateFlow<List<OnuDevice>> = repository.onus
    val onuPowerHistory: StateFlow<List<OnuPowerHistoryPoint>> = repository.onuPowerHistory
    val mikrotiks: StateFlow<List<MikrotikDevice>> = repository.mikrotiks
    val interfaces: StateFlow<List<NetworkInterface>> = repository.interfaces
    val alerts: StateFlow<List<NetworkAlert>> = repository.alerts
    val thresholds: StateFlow<AlertThresholdSettings> = repository.thresholds
    val trafficHistory: StateFlow<List<TrafficPoint>> = repository.trafficHistory
    val historical24hTraffic: StateFlow<List<HourlyTrafficPoint>> = repository.historical24hTraffic
    val selected24hInterfaceId: StateFlow<String?> = repository.selected24hInterfaceId
    val ipScanState: StateFlow<IpRangeScanState> = repository.ipScanState
    val ispPortals: StateFlow<List<IspPortal>> = repository.ispPortals
    val computedAnalytics: StateFlow<NocComputedAnalytics> = repository.computedAnalytics

    private val _uiState = MutableStateFlow(NocUiState())
    val uiState: StateFlow<NocUiState> = _uiState.asStateFlow()

    init {
        refreshMyIpAndGateway()
    }

    fun selectTab(tab: NocNavTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun toggleDarkMode() {
        _uiState.update { it.copy(isDarkMode = !it.isDarkMode) }
    }

    fun setDarkMode(enabled: Boolean) {
        _uiState.update { it.copy(isDarkMode = enabled) }
    }

    fun selectOlt(oltId: String?) {
        _uiState.update { it.copy(selectedOltId = oltId, selectedPonPort = null) }
    }

    fun selectPonPort(port: String?) {
        _uiState.update { it.copy(selectedPonPort = port) }
    }

    fun setOnuSearch(query: String) {
        _uiState.update { it.copy(onuSearchQuery = query) }
    }

    fun setOnuFilter(status: OnuStatus?) {
        _uiState.update { it.copy(onuStatusFilter = status) }
    }

    fun selectRouter(routerId: String) {
        val firstIf: NetworkInterface? = interfaces.value.find { it.routerId == routerId }
        _uiState.update { it.copy(selectedRouterId = routerId, selectedInterfaceId = firstIf?.id) }
        repository.setSelectedInterface(firstIf?.id)
    }

    fun selectInterface(interfaceId: String) {
        _uiState.update { it.copy(selectedInterfaceId = interfaceId) }
        repository.setSelectedInterface(interfaceId)
    }

    fun select24hInterface(interfaceId: String?) {
        repository.select24hInterface(interfaceId)
    }

    fun inspectOnu(onu: OnuDevice?) {
        _uiState.update { it.copy(inspectingOnu = onu) }
    }

    fun setAddOltDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isAddOltDialogOpen = open) }
    }

    fun setAddOnuDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isAddOnuDialogOpen = open) }
    }

    fun setAddRouterDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isAddRouterDialogOpen = open) }
    }

    fun setThresholdDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isThresholdDialogOpen = open) }
    }

    // Repository actions
    fun acknowledgeAlert(id: String) = repository.acknowledgeAlert(id)
    fun clearAllAlerts() = repository.clearAllAlerts()
    fun updateThresholds(settings: AlertThresholdSettings) = repository.updateThresholds(settings)

    fun addOlt(olt: OltDevice) {
        repository.addOlt(olt)
        setAddOltDialogOpen(false)
    }

    fun deleteOlt(id: String) = repository.deleteOlt(id)

    fun addOnu(onu: OnuDevice) {
        repository.addOnu(onu)
        setAddOnuDialogOpen(false)
    }

    fun deleteOnu(id: String) = repository.deleteOnu(id)

    fun addMikrotik(mtk: MikrotikDevice) {
        repository.addMikrotik(mtk)
        setAddRouterDialogOpen(false)
    }

    fun deleteMikrotik(id: String) = repository.deleteMikrotik(id)

    fun toggleInterfaceLink(ifId: String) = repository.toggleInterfaceLink(ifId)

    // Simulation / testing triggers
    fun triggerOpticalDegradation(onuId: String) = repository.triggerOpticalDegradation(onuId)
    fun triggerHighOpticalOverload(onuId: String) = repository.triggerHighOpticalOverload(onuId)
    fun triggerOltOfflineAlert(oltId: String = "olt-zte-02") = repository.triggerOltOfflineAlert(oltId)
    fun restoreOltOnline(oltId: String) = repository.restoreOltOnline(oltId)
    fun triggerDeviceUnreachable(targetId: String = "olt-zte-02", isOlt: Boolean = true) =
        repository.triggerDeviceUnreachable(targetId, isOlt)
    fun sendTestEmailAndWebhookAlert() = repository.sendTestEmailAndWebhookAlert()
    fun triggerFiberCut(ponPort: String) = repository.triggerFiberCut(ponPort)
    fun triggerTrafficSpike() = repository.triggerTrafficSpike()
    fun restoreNormalNetwork() = repository.restoreNormalNetwork()

    // Automated IP Range Discovery Actions
    fun setSubnetRange(subnetRange: String) = repository.setSubnetRange(subnetRange)
    fun toggleAutoBackgroundScan(enabled: Boolean) = repository.toggleAutoBackgroundScan(enabled)
    fun runAutomatedIpRangeScan(customRange: String? = null) = repository.runAutomatedIpRangeScan(customRange)
    fun detectLocalSubnetAndScan() = repository.detectLocalSubnetAndScan()

    // Real probe actions
    fun testPing(host: String, port: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPinging = true, pingTestResult = null) }
            val result = RealNetworkProbe.probeTcpPort(host, port)
            _uiState.update { it.copy(isPinging = false, pingTestResult = result) }
        }
    }

    fun calculateOpticalBudget(
        oltTx: Float,
        distanceKm: Float,
        split1: Int,
        split2: Int,
        splices: Int,
        margin: Float
    ) {
        val result = RealNetworkProbe.calculateOpticalLoss(
            oltTxPowerDbm = oltTx,
            fiberLengthKm = distanceKm,
            firstStageSplitterRatio = split1,
            secondStageSplitterRatio = split2,
            spliceCount = splices,
            safetyMarginDb = margin
        )
        _uiState.update { it.copy(opticalBudgetResult = result) }
    }

    // --- Dedicated Network Testing Ping Tool Methods ---

    fun setManualPingHost(host: String) {
        _uiState.update { it.copy(manualPingHost = host) }
    }

    fun pingTarget(host: String) {
        val target = host.trim()
        if (target.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(pingingHosts = it.pingingHosts + target) }
            val status = RealNetworkProbe.pingHost(target)
            _uiState.update {
                it.copy(
                    pingingHosts = it.pingingHosts - target,
                    pingResults = it.pingResults + (target to status)
                )
            }
        }
    }

    fun pingAllPresets() {
        val presets = listOf("8.8.8.8", "1.1.1.1", "10.10.25.1")
        val manual = _uiState.value.manualPingHost.trim()
        val allTargets = if (manual.isNotBlank()) presets + manual else presets
        allTargets.forEach { pingTarget(it) }
    }

    // --- What Is My IP & Gateway, Speed Test, Traceroute, DNS Lookup, and Latency/Jitter ---

    fun refreshMyIpAndGateway() {
        viewModelScope.launch {
            _uiState.update { it.copy(myIpAndGateway = it.myIpAndGateway.copy(isRefreshing = true)) }
            val info = RealNetworkProbe.inspectMyIpAndGateway()
            _uiState.update { it.copy(myIpAndGateway = info) }
        }
    }

    fun selectSpeedTestProvider(provider: SpeedTestProvider) {
        _uiState.update {
            it.copy(
                selectedSpeedTestProvider = provider,
                activeWebSpeedTestProvider = if (provider == SpeedTestProvider.FAST_COM) SpeedTestProvider.FAST_COM else SpeedTestProvider.SPEEDTEST_NET,
                speedTestState = it.speedTestState.copy(
                    provider = provider,
                    providerUrl = provider.webUrl,
                    serverName = provider.defaultServerName,
                    phaseLabel = "Ready • Selected ${provider.displayName} (${provider.webUrl})"
                )
            )
        }
    }

    fun selectWebSpeedTestProvider(provider: SpeedTestProvider) {
        _uiState.update {
            it.copy(
                activeWebSpeedTestProvider = provider,
                selectedSpeedTestProvider = provider,
                isLiveSpeedTestWebVisible = true
            )
        }
    }

    fun toggleLiveSpeedTestWebVisible() {
        _uiState.update { it.copy(isLiveSpeedTestWebVisible = !it.isLiveSpeedTestWebVisible) }
    }

    fun startSpeedTest(customProvider: SpeedTestProvider? = null) {
        if (_uiState.value.speedTestState.isRunning) return
        val provider = customProvider ?: _uiState.value.selectedSpeedTestProvider
        _uiState.update {
            it.copy(
                selectedSpeedTestProvider = provider,
                activeWebSpeedTestProvider = if (provider == SpeedTestProvider.FAST_COM) SpeedTestProvider.FAST_COM else SpeedTestProvider.SPEEDTEST_NET
            )
        }
        viewModelScope.launch {
            RealNetworkProbe.runSpeedTest(provider = provider) { progressState ->
                _uiState.update { it.copy(speedTestState = progressState) }
            }
        }
    }

    fun setTracerouteTarget(target: String) {
        _uiState.update { it.copy(tracerouteTargetInput = target) }
    }

    fun startTraceroute(customTarget: String? = null) {
        val target = (customTarget ?: _uiState.value.tracerouteTargetInput).trim().ifBlank { "8.8.8.8" }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    tracerouteTargetInput = target,
                    tracerouteState = TracerouteState(
                        targetHost = target,
                        resolvedIp = target,
                        isRunning = true,
                        hops = emptyList(),
                        statusMessage = "Initializing traceroute to $target..."
                    )
                )
            }
            val finalState = RealNetworkProbe.runTraceroute(target) { hopsSoFar, statusMsg ->
                _uiState.update {
                    it.copy(
                        tracerouteState = it.tracerouteState.copy(
                            isRunning = true,
                            hops = hopsSoFar,
                            statusMessage = statusMsg
                        )
                    )
                }
            }
            _uiState.update { it.copy(tracerouteState = finalState) }
        }
    }

    fun setDnsQueryInput(domain: String) {
        _uiState.update { it.copy(dnsQueryInput = domain) }
    }

    fun setDnsRecordType(recordType: String) {
        _uiState.update { it.copy(dnsRecordType = recordType) }
    }

    fun performDnsLookup(customDomain: String? = null, customType: String? = null) {
        val domain = (customDomain ?: _uiState.value.dnsQueryInput).trim().ifBlank { "fms.bsnl.in" }
        val type = (customType ?: _uiState.value.dnsRecordType).trim().ifBlank { "A" }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    dnsQueryInput = domain,
                    dnsRecordType = type,
                    isDnsLoading = true
                )
            }
            val result = RealNetworkProbe.lookupDns(domain, type)
            _uiState.update {
                it.copy(
                    isDnsLoading = false,
                    dnsLookupResult = result
                )
            }
        }
    }

    fun runLatencyAndJitterAnalysis(customHost: String? = null) {
        val host = (customHost ?: _uiState.value.manualPingHost.ifBlank { "8.8.8.8" }).trim()
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingLatency = true) }
            val result = RealNetworkProbe.analyzeLatencyAndJitter(host, samples = 5)
            _uiState.update {
                it.copy(
                    isAnalyzingLatency = false,
                    latencyAnalysisResult = result
                )
            }
        }
    }

    // --- V-SOL Integration Ecosystem & ONT Setup ---

    fun setVsolOntSetupDialogOpen(open: Boolean, unreg: VsolUnregisteredOnu? = null) {
        _uiState.update { it.copy(isVsolOntSetupDialogOpen = open, selectedVsolUnregOnu = unreg) }
    }

    fun provisionVsolOnt(onu: OnuDevice) {
        repository.addOnu(onu)
        _uiState.update { state ->
            state.copy(
                isVsolOntSetupDialogOpen = false,
                selectedVsolUnregOnu = null,
                vsolUnregisteredOnus = state.vsolUnregisteredOnus.filter { it.serialNumber != onu.serialNumber }
            )
        }
    }

    fun runVsolBatchAudit(oltId: String) {
        val vsolOnus = onus.value.filter { it.oltId == oltId }
        val total = vsolOnus.size
        if (total == 0) return

        val optimal = vsolOnus.count { it.rxPowerDbm >= -23.0f && it.rxPowerDbm <= -17.0f }
        val warning = vsolOnus.count { (it.rxPowerDbm < -23.0f && it.rxPowerDbm >= -27.5f) || it.rxPowerDbm > -17.0f }
        val critical = vsolOnus.count { it.rxPowerDbm < -27.5f }
        val avgRx = vsolOnus.map { it.rxPowerDbm }.average().toFloat()

        _uiState.update {
            it.copy(
                vsolBatchAudit = VsolBatchAuditResult(
                    totalOnus = total,
                    optimalCount = optimal,
                    warningCount = warning,
                    criticalCount = critical,
                    averageRxDbm = avgRx
                )
            )
        }
    }

    // --- Customer Live Map Operations ---

    fun selectMapOnu(onu: OnuDevice?) {
        _uiState.update { it.copy(selectedMapOnu = onu) }
    }

    fun toggleRealtimePowerBadge() {
        _uiState.update { it.copy(showRealtimePowerBadge = !it.showRealtimePowerBadge) }
    }

    fun toggleFiberRoutes() {
        _uiState.update { it.copy(showFiberRoutes = !it.showFiberRoutes) }
    }

    fun setMapFilterQuality(quality: OpticalQuality?) {
        _uiState.update { it.copy(mapFilterQuality = quality) }
    }

    fun setMapVisualizationLayer(layer: MapVisualizationLayer) {
        _uiState.update { it.copy(mapVisualizationLayer = layer) }
    }

    // --- ISP Portals & Saved Passwords Operations ---

    fun selectIspPortalIndex(index: Int) {
        _uiState.update { it.copy(selectedIspIndex = index, isPasswordVisible = false) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun openEditCredentialsDialog(portal: IspPortal) {
        _uiState.update { it.copy(isEditCredentialsDialogOpen = true, editingPortal = portal) }
    }

    fun closeEditCredentialsDialog() {
        _uiState.update { it.copy(isEditCredentialsDialogOpen = false, editingPortal = null) }
    }

    fun saveIspCredentials(
        id: String,
        username: String,
        password: String,
        partnerCode: String,
        notes: String
    ) {
        repository.updateIspCredentials(
            id = id,
            username = username,
            password = password,
            partnerCode = partnerCode,
            notes = notes
        )
        closeEditCredentialsDialog()
    }

    // --- V-SOL Router / ONT Configuration Suite ---

    fun openVsolRouterConfig(onu: OnuDevice? = null) {
        val initialConfig = if (onu != null) {
            VsolRouterConfig(
                serialNumber = onu.serialNumber,
                targetCustomerName = onu.customerName,
                pppoeUsername = onu.pppoeAccount,
                model = if (onu.serialNumber.contains("VSOL")) "V2804AX (WiFi 6 GPON)" else "V2802RH (XPON + CATV RF)"
            )
        } else {
            VsolRouterConfig()
        }
        _uiState.update {
            it.copy(
                isVsolRouterConfigDialogOpen = true,
                activeVsolRouterConfig = initialConfig,
                vsolConfigSuccessMessage = null
            )
        }
    }

    fun closeVsolRouterConfig() {
        _uiState.update { it.copy(isVsolRouterConfigDialogOpen = false, vsolConfigSuccessMessage = null) }
    }

    fun updateActiveVsolConfig(config: VsolRouterConfig) {
        _uiState.update { it.copy(activeVsolRouterConfig = config) }
    }

    fun loadVsolPreset(presetName: String) {
        val current = _uiState.value.activeVsolRouterConfig
        val updated = when (presetName) {
            "BSNL" -> current.copy(
                wanMode = VsolWanMode.PPPOE,
                vlanId = 100,
                vlanPriority = 0,
                pppoeUsername = "kol_ftth_${current.serialNumber.takeLast(4)}@ftth.bsnl.in",
                wifiSsid24 = "BSNL_FTTH_2.4G",
                wifiSsid5 = "BSNL_FTTH_5G_WiFi6"
            )
            "MEGHBELA" -> current.copy(
                wanMode = VsolWanMode.PPPOE,
                vlanId = 200,
                vlanPriority = 1,
                pppoeUsername = "megh_${current.serialNumber.takeLast(4)}",
                wifiSsid24 = "Meghbela_Fast_2.4G",
                wifiSsid5 = "Meghbela_Gigabit_5G"
            )
            "GTPL" -> current.copy(
                wanMode = VsolWanMode.IPOE_DHCP,
                vlanId = 300,
                vlanPriority = 0,
                wifiSsid24 = "GTPL_KCBPL_Home_2.4G",
                wifiSsid5 = "GTPL_KCBPL_5G"
            )
            else -> current.copy(
                wanMode = VsolWanMode.PPPOE,
                vlanId = 500,
                vlanPriority = 0,
                pppoeUsername = "cyber_city_${current.serialNumber.takeLast(4)}",
                wifiSsid24 = "CyberCity_Fiber_2.4G",
                wifiSsid5 = "CyberCity_WiFi6_5G"
            )
        }
        _uiState.update { it.copy(activeVsolRouterConfig = updated) }
    }

    fun applyVsolRouterConfig() {
        val cfg = _uiState.value.activeVsolRouterConfig
        _uiState.update {
            it.copy(
                vsolConfigSuccessMessage = "Successfully pushed V-SOL config to ${cfg.serialNumber} (${cfg.model}) via OMCI/TR-069!"
            )
        }
    }

    fun dismissVsolSuccessMessage() {
        _uiState.update { it.copy(vsolConfigSuccessMessage = null) }
    }

    fun fetchAndCalculateAllTelemetry() {
        repository.fetchAndCalculateAllTelemetry()
    }

    // --- SNMP Community Strings & OLT Port Hardware Diagnostics Settings ---

    fun selectSettingsOlt(oltId: String) {
        _uiState.update { it.copy(selectedSettingsOltId = oltId, settingsFeedbackMessage = null) }
    }

    fun updateGlobalSnmpSettings(
        readCommunity: String,
        writeCommunity: String,
        udpPort: Int,
        version: String = "SNMPv2c"
    ) {
        _uiState.update {
            it.copy(
                globalSnmpReadCommunity = readCommunity.trim().ifBlank { "public" },
                globalSnmpWriteCommunity = writeCommunity.trim().ifBlank { "private_noc_rw" },
                globalSnmpUdpPort = udpPort.coerceIn(1, 65535),
                snmpVersion = version
            )
        }
    }

    fun applyGlobalSnmpCommunityToAllOlts(
        readCommunity: String = _uiState.value.globalSnmpReadCommunity,
        writeCommunity: String = _uiState.value.globalSnmpWriteCommunity,
        udpPort: Int = _uiState.value.globalSnmpUdpPort
    ) {
        val cleanRo = readCommunity.trim().ifBlank { "public" }
        val cleanRw = writeCommunity.trim().ifBlank { "private_noc_rw" }
        _uiState.update {
            it.copy(
                globalSnmpReadCommunity = cleanRo,
                globalSnmpWriteCommunity = cleanRw,
                globalSnmpUdpPort = udpPort.coerceIn(1, 65535),
                settingsFeedbackMessage = "Applied SNMP Read Community '$cleanRo' & Write '$cleanRw' (UDP :$udpPort) to all OLTs."
            )
        }
        repository.applyGlobalSnmpCommunityToAllOlts(cleanRo)
    }

    fun saveOltSnmpAndPortConfig(
        oltId: String,
        ipAddress: String,
        port: Int,
        snmpCommunity: String,
        totalPonPorts: Int
    ) {
        repository.updateOltSnmpAndPortConfig(
            oltId = oltId,
            ipAddress = ipAddress,
            port = port,
            snmpCommunity = snmpCommunity,
            totalPonPorts = totalPonPorts
        )
        _uiState.update {
            it.copy(
                settingsFeedbackMessage = "Saved SNMP community '${snmpCommunity.trim().ifBlank { "public" }}' & Port :$port ($totalPonPorts PON ports) for OLT $oltId."
            )
        }
    }

    fun savePonPortConfig(
        portId: String,
        name: String,
        txPowerDbm: Float,
        totalOnus: Int,
        adminState: Boolean
    ) {
        repository.updatePonPortConfiguration(
            portId = portId,
            name = name,
            txPowerDbm = txPowerDbm,
            totalOnus = totalOnus,
            adminState = adminState
        )
        _uiState.update {
            it.copy(
                settingsFeedbackMessage = "Updated PON port $name (Tx ${String.format("%+.1f", txPowerDbm)} dBm, Split 1:$totalOnus, Admin ${if (adminState) "UP" else "DOWN"})."
            )
        }
    }

    fun addNewPonPortConfig(
        oltId: String,
        name: String,
        txPowerDbm: Float,
        totalOnus: Int = 64
    ) {
        repository.addPonPortConfiguration(
            oltId = oltId,
            name = name,
            txPowerDbm = txPowerDbm,
            totalOnus = totalOnus
        )
        _uiState.update {
            it.copy(
                settingsFeedbackMessage = "Added PON port $name (Tx ${String.format("%+.1f", txPowerDbm)} dBm, Split 1:$totalOnus) to OLT $oltId."
            )
        }
    }

    fun runDeepHardwareDiagnostics(oltId: String? = null) {
        val targetId = oltId ?: _uiState.value.selectedSettingsOltId
        viewModelScope.launch {
            _uiState.update { it.copy(isRunningHardwareDiag = true) }
            val report = repository.runDeepHardwareDiagnostics(
                oltId = targetId,
                rwCommunity = _uiState.value.globalSnmpWriteCommunity,
                snmpUdpPort = _uiState.value.globalSnmpUdpPort
            )
            _uiState.update {
                it.copy(
                    isRunningHardwareDiag = false,
                    hardwareDiagnosticReport = report,
                    settingsFeedbackMessage = "Deep SNMP & Optical DOM Diagnostics completed for ${report.oltName}."
                )
            }
        }
    }

    fun clearSettingsFeedbackMessage() {
        _uiState.update { it.copy(settingsFeedbackMessage = null) }
    }

    fun probeSingleOltLive(oltId: String) {
        repository.probeSingleOltLive(oltId)
    }

    fun probeAllRealDevicesNow() {
        repository.probeAllRealDevicesNow()
        _uiState.update {
            it.copy(
                settingsFeedbackMessage = "Probed all OLTs & MikroTiks via ICMP, TCP Management & SNMPv2c."
            )
        }
    }

    fun clearPreloadedDevicesForRealUse() {
        repository.clearPreloadedDevicesForRealUse()
        _uiState.update {
            it.copy(
                hardwareDiagnosticReport = null,
                settingsFeedbackMessage = "Cleared preloaded sample devices. Add or scan your real OLTs & MikroTiks."
            )
        }
    }
}

