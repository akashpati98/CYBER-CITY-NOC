package com.example.data

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.example.model.DiscoveredNetworkDevice
import kotlinx.coroutines.*

/**
 * Android Background Service that performs automated IP range scans to discover
 * active OLTs (Huawei, ZTE, V-SOL on ports 23/80/161/443) and MikroTik RouterOS
 * devices (on ports 8728/8729/8291/443) on the local network and adds them
 * automatically to the Room monitoring list.
 */
class IpDiscoveryService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var periodicDiscoveryJob: Job? = null
    private val binder = LocalBinder()

    @Volatile
    var isServiceRunning: Boolean = false
        private set

    @Volatile
    var activeSubnetRange: String = "10.100.1.1-254"
        private set

    @Volatile
    var scanIntervalMs: Long = DEFAULT_SCAN_INTERVAL_MS
        private set

    inner class LocalBinder : Binder() {
        fun getService(): IpDiscoveryService = this@IpDiscoveryService
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        NetworkRepository.getInstance(applicationContext).setServiceRunningState(true)
    }

    override fun onBind(intent: Intent?): IBinder {
        isServiceRunning = true
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isServiceRunning = true
        val repository = NetworkRepository.getInstance(applicationContext)
        repository.setServiceRunningState(true)

        val action = intent?.action ?: ACTION_START_AUTO_DISCOVERY
        val requestedSubnet = intent?.getStringExtra(EXTRA_SUBNET_RANGE)
        if (!requestedSubnet.isNullOrBlank()) {
            activeSubnetRange = requestedSubnet.trim()
            repository.setSubnetRange(activeSubnetRange)
        } else {
            activeSubnetRange = repository.ipScanState.value.subnetRange
        }

        val requestedInterval = intent?.getLongExtra(EXTRA_SCAN_INTERVAL_MS, DEFAULT_SCAN_INTERVAL_MS) ?: DEFAULT_SCAN_INTERVAL_MS
        scanIntervalMs = requestedInterval.coerceAtLeast(10_000L)

        when (action) {
            ACTION_STOP_AUTO_DISCOVERY -> {
                periodicDiscoveryJob?.cancel()
                periodicDiscoveryJob = null
                isServiceRunning = false
                repository.setServiceRunningState(false)
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_DETECT_LOCAL_SUBNET -> {
                val detected = RealNetworkProbe.detectLocalLanSubnet()
                activeSubnetRange = detected
                repository.setSubnetRange(detected)
                serviceScope.launch {
                    repository.executeDiscoverySweep(detected)
                }
            }

            ACTION_SCAN_SUBNET_NOW -> {
                serviceScope.launch {
                    repository.executeDiscoverySweep(activeSubnetRange)
                }
            }

            ACTION_START_AUTO_DISCOVERY -> {
                val immediateScan = intent?.getBooleanExtra(EXTRA_IMMEDIATE_SCAN, false) ?: false
                if (immediateScan) {
                    serviceScope.launch {
                        repository.executeDiscoverySweep(activeSubnetRange)
                    }
                }
                ensurePeriodicDiscoveryLoop(repository)
            }
        }

        ensurePeriodicDiscoveryLoop(repository)
        return START_STICKY
    }

    private fun ensurePeriodicDiscoveryLoop(repository: NetworkRepository) {
        if (periodicDiscoveryJob?.isActive == true) return
        periodicDiscoveryJob = serviceScope.launch {
            while (isActive) {
                delay(scanIntervalMs)
                val currentState = repository.ipScanState.value
                if (currentState.autoBackgroundScanEnabled && !currentState.isScanning) {
                    activeSubnetRange = currentState.subnetRange
                    repository.executeDiscoverySweep(activeSubnetRange)
                }
            }
        }
    }

    /**
     * Performs an immediate IP range discovery sweep via the Service and returns the newly
     * identified active OLTs and MikroTik devices.
     */
    suspend fun performImmediateDiscoverySweep(subnetRange: String? = null): List<DiscoveredNetworkDevice> {
        val repository = NetworkRepository.getInstance(applicationContext)
        val target = (subnetRange ?: activeSubnetRange).trim().ifBlank { "10.100.1.1-254" }
        activeSubnetRange = target
        return repository.executeDiscoverySweep(target)
    }

    /**
     * Auto-detects the local network IPv4 subnet and performs an immediate discovery sweep.
     */
    suspend fun detectLocalSubnetAndSweep(): List<DiscoveredNetworkDevice> {
        val detected = RealNetworkProbe.detectLocalLanSubnet()
        activeSubnetRange = detected
        val repository = NetworkRepository.getInstance(applicationContext)
        repository.setSubnetRange(detected)
        return repository.executeDiscoverySweep(detected)
    }

    override fun onDestroy() {
        isServiceRunning = false
        periodicDiscoveryJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_AUTO_DISCOVERY = "com.example.action.START_AUTO_DISCOVERY"
        const val ACTION_STOP_AUTO_DISCOVERY = "com.example.action.STOP_AUTO_DISCOVERY"
        const val ACTION_SCAN_SUBNET_NOW = "com.example.action.SCAN_SUBNET_NOW"
        const val ACTION_DETECT_LOCAL_SUBNET = "com.example.action.DETECT_LOCAL_SUBNET"

        const val EXTRA_SUBNET_RANGE = "extra_subnet_range"
        const val EXTRA_IMMEDIATE_SCAN = "extra_immediate_scan"
        const val EXTRA_SCAN_INTERVAL_MS = "extra_scan_interval_ms"
        const val DEFAULT_SCAN_INTERVAL_MS = 45_000L

        fun startService(
            context: Context,
            subnetRange: String = "10.100.1.1-254",
            immediate: Boolean = false
        ) {
            try {
                val intent = Intent(context, IpDiscoveryService::class.java).apply {
                    action = ACTION_START_AUTO_DISCOVERY
                    putExtra(EXTRA_SUBNET_RANGE, subnetRange)
                    putExtra(EXTRA_IMMEDIATE_SCAN, immediate)
                }
                context.startService(intent)
            } catch (_: Exception) {
                // Safe fallback in unit test environments
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, IpDiscoveryService::class.java).apply {
                    action = ACTION_STOP_AUTO_DISCOVERY
                }
                context.startService(intent)
            } catch (_: Exception) {
                // Safe fallback in unit test environments
            }
        }

        fun triggerScanNow(context: Context, subnetRange: String) {
            try {
                val intent = Intent(context, IpDiscoveryService::class.java).apply {
                    action = ACTION_SCAN_SUBNET_NOW
                    putExtra(EXTRA_SUBNET_RANGE, subnetRange)
                }
                context.startService(intent)
            } catch (_: Exception) {
                // Safe fallback in unit test environments
            }
        }
    }
}
