# Implementation Plan: Automated IP Discovery, 24-Hour Traffic Widget & Map Latency/Packet Loss Layer

## 1. Overview & Defaults Selected
Since the clarifying questions were skipped, we will implement the recommended production defaults for **Cyber City Monitoring Tool**:
1. **Automated IP Range Discovery Service**: A background discovery service and coroutine scanner that sweeps configurable local IP ranges (e.g., `10.10.10.1–254`, `192.168.88.1–254`) on both a periodic background schedule and manual trigger, fingerprints OLT (SNMP/HTTP/Telnet ports `161`, `80`, `23`) and MikroTik RouterOS (API/Winbox/HTTP ports `8728`, `8291`, `80`, `443`) signatures, and automatically adds newly discovered devices to the Room monitoring database with an alert log entry.
2. **24-Hour Historical Interface Traffic Dashboard Widget**: A native Jetpack Compose Canvas dual-series area & line chart widget (providing Recharts-style interactive hourly tooltips, interface selector, peak/average KPIs, and smooth cubic bezier curves for RX/In and TX/Out throughput across the past 24 hours).
3. **Map Visualization Layer (Latency & Packet Loss)**: An interactive visualization layer selector on the **Customer Live Map** allowing operators to switch node and fiber-link color-coding between **Optical Power (dBm)**, **Average Latency (ms)**, and **Packet Loss (%)**, complete with live metric badges and detailed node telemetry cards.

---

## 2. Architecture & Data Layer Changes

### A. Domain Models (`model/OltModels.kt`, `model/MikrotikModels.kt`, `model/AnalyticsModels.kt`)
- **Latency & Packet Loss Telemetry**:
  - Extend `OltDevice`, `OnuDevice`, and `MikrotikDevice` (plus corresponding Room entities in `NocEntities.kt`) with `avgLatencyMs: Float` and `packetLossPercent: Float`, computed and updated dynamically during telemetry ticks and network probes.
  - Define `MapVisualizationLayer` enum (`OPTICAL_POWER`, `AVG_LATENCY`, `PACKET_LOSS`) with color-coding thresholds:
    - **Latency**: Optimal (`< 15 ms` Green), Elevated (`15–45 ms` Amber), High (`> 45 ms` or unreachable Red).
    - **Packet Loss**: Zero/Low (`< 0.5%` Green), Moderate (`0.5%–2.5%` Amber), Severe (`> 2.5%` Red).
- **24-Hour Historical Traffic Model**:
  - Add `HourlyTrafficPoint(hourLabel: String, timestamp: Long, inBps: Long, outBps: Long, peakInBps: Long, peakOutBps: Long)` and per-interface 24-hour historical series in `NetworkRepository`.
- **IP Range Discovery State & Models**:
  - Add `IpRangeScanState` (`isScanning: Boolean`, `subnetRange: String`, `scannedHostsCount: Int`, `totalHostsToScan: Int`, `discoveredDevices: List<DiscoveredNetworkDevice>`, `lastScanTimestamp: Long`, `autoBackgroundScanEnabled: Boolean`).

### B. Background IP Discovery Service (`data/IpDiscoveryService.kt` & `NetworkRepository.kt`)
- Implement an Android background `Service` (`IpDiscoveryBackgroundService`) paired with a structured coroutine scanner in `RealNetworkProbe` / `NetworkRepository`:
  - Parses CIDR or start-end IP ranges (defaulting to `10.10.25.1-254` and `192.168.88.1-254`).
  - Performs concurrent TCP/ICMP socket probes across target IPs for MikroTik ports (`8728`, `8291`, `443`) and OLT management ports (`161`, `8080`, `23`).
  - Automatically deduplicates against existing `olts` and `mikrotiks` in Room DB by IP address, inserts newly discovered OLTs and MikroTik routers into Room, and generates an `INFO` / `WARNING` discovery alert in the NOC alert feed.
  - Exposes manual scan controls, custom IP range input, and background periodic scan toggle on both the **NOC Overview Dashboard** and **Tools & Probe** screens.

---

## 3. UI & Screen Enhancements

### A. 24-Hour Historical Traffic Widget (`ui/components/Historical24hTrafficWidget.kt` & `DashboardScreen.kt`)
- Build a Recharts-inspired interactive 24-Hour Interface Throughput card on `DashboardScreen`:
  - **Interface Filter Chips**: Switch between `Aggregated Uplink (All Interfaces)` or individual MikroTik interfaces (`sfp-sfpplus1-WAN`, `ether1-BSNL`, `sfp2-OLT-Trunk`, etc.).
  - **Interactive Scrubbing / Tap Tooltip**: Tap or drag across the 24-hour timeline (`00:00` to `23:00`) to inspect exact hourly `IN (RX)` and `OUT (TX)` throughput values, 95th percentile line, and peak bandwidth badges.
  - **Summary Footer**: Displays 24h Total Volume transferred, 24h Peak In/Out, and 24h Average Throughput.

### B. Map Visualization Layer (`ui/screens/CustomerLiveMapScreen.kt`)
- Add a **Visualization Layer Selector** bar in the map's top control overlay:
  - `Optical Power (dBm)` | `Avg Latency (ms)` | `Packet Loss (%)`
- Update the map `Canvas` rendering:
  - Color-code OLT hubs, fiber trunk cables, and customer ONU nodes dynamically based on the active layer (`OPTICAL_POWER`, `AVG_LATENCY`, or `PACKET_LOSS`).
  - Update floating node badges to display `"18.4 dBm"`, `"4.2 ms"`, or `"0.0% Loss"` depending on the selected visualization mode.
  - Add a dynamic Map Color Legend pill and update `CustomerMapDetailCard` to display live latency and packet loss metrics alongside optical power.

---

## 4. Verification Plan
- Compile the application with `compile_applet` to ensure zero Kotlin or Room schema compilation errors.
- Add unit/Robolectric tests in `ExampleRobolectricTest.kt` covering IP range discovery device insertion, 24-hour traffic history generation, and map visualization layer switching.
