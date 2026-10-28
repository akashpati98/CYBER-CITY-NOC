package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.NocNavTab
import com.example.ui.NocViewModel
import com.example.ui.components.Formatters
import com.example.ui.components.OpticalPowerGauge
import com.example.ui.theme.*
import kotlin.math.sqrt

private enum class GisMapTheme(val label: String) {
    STREET_GIS("Street GIS"),
    DARK_RADAR("NOC Radar"),
    SATELLITE("Satellite")
}

@Composable
fun CustomerLiveMapScreen(
    viewModel: NocViewModel,
    modifier: Modifier = Modifier
) {
    val olts by viewModel.olts.collectAsState()
    val mikrotiks by viewModel.mikrotiks.collectAsState()
    val onus by viewModel.onus.collectAsState()
    val onuPowerHistory by viewModel.onuPowerHistory.collectAsState()
    val thresholds by viewModel.thresholds.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val activeLayer = uiState.mapVisualizationLayer

    var mapTheme by remember { mutableStateOf(GisMapTheme.STREET_GIS) }
    var selectedOltOnMap by remember { mutableStateOf<OltDevice?>(null) }
    var selectedMikrotikOnMap by remember { mutableStateOf<MikrotikDevice?>(null) }

    // Filter ONUs on map according to selected quality filter
    val filteredOnus = remember(onus, uiState.mapFilterQuality, activeLayer) {
        when (uiState.mapFilterQuality) {
            null -> onus
            OpticalQuality.CRITICAL -> onus.filter {
                val q = it.getLayerQuality(activeLayer)
                q == NodeHealthQuality.CRITICAL || q == NodeHealthQuality.OFFLINE
            }
            OpticalQuality.WARNING -> onus.filter {
                it.getLayerQuality(activeLayer) == NodeHealthQuality.WARNING
            }
            else -> onus.filter { it.getPowerQuality() == uiState.mapFilterQuality }
        }
    }

    // Layer health breakdown counts across all connected devices (MikroTiks + OLTs + ONUs)
    val allNodeQualities = remember(filteredOnus, olts, mikrotiks, activeLayer) {
        mikrotiks.map { it.getLayerQuality(activeLayer) } +
            olts.map { it.getLayerQuality(activeLayer) } +
            filteredOnus.map { it.getLayerQuality(activeLayer) }
    }
    val optimalCount = remember(allNodeQualities) {
        allNodeQualities.count { it == NodeHealthQuality.OPTIMAL }
    }
    val warningCount = remember(allNodeQualities) {
        allNodeQualities.count { it == NodeHealthQuality.WARNING }
    }
    val criticalCount = remember(allNodeQualities) {
        allNodeQualities.count { it == NodeHealthQuality.CRITICAL || it == NodeHealthQuality.OFFLINE }
    }

    // Real-time network-wide average latency & packet loss across connected devices
    val networkAvgLatencyMs = remember(filteredOnus, olts, mikrotiks) {
        val latencies = mikrotiks.filter { it.status != DeviceStatus.OFFLINE }.map { it.avgLatencyMs } +
            olts.filter { it.status != DeviceStatus.OFFLINE }.map { it.avgLatencyMs } +
            filteredOnus.filter { it.status != OnuStatus.OFFLINE }.map { it.avgLatencyMs }
        if (latencies.isNotEmpty()) latencies.average().toFloat() else 0f
    }
    val networkAvgLossPercent = remember(filteredOnus, olts, mikrotiks) {
        val losses = mikrotiks.map { it.packetLossPercent } +
            olts.map { it.packetLossPercent } +
            filteredOnus.map { it.packetLossPercent }
        if (losses.isNotEmpty()) losses.average().toFloat() else 0f
    }

    // Dynamic center from actual OLT + ONU coordinates so nodes are always in view
    val mapOrigin = remember(olts, onus) {
        val allLats = olts.map { it.latitude } + onus.map { it.latitude }
        val allLngs = olts.map { it.longitude } + onus.map { it.longitude }
        if (allLats.isNotEmpty() && allLngs.isNotEmpty()) {
            Pair(allLats.average(), allLngs.average())
        } else {
            Pair(37.7750, -122.4180)
        }
    }

    // Assign deterministic GIS coordinates for each MikroTik Core/BNG Router around the NOC HQ
    val mikrotikCoordinates = remember(mikrotiks, mapOrigin) {
        mikrotiks.mapIndexed { index, mtk ->
            val latOffset = 0.0022 + (index * 0.0014) * if (index % 2 == 0) 1 else -1
            val lngOffset = -0.0024 + (index * 0.0028)
            mtk.id to Pair(mapOrigin.first + latOffset, mapOrigin.second + lngOffset)
        }.toMap()
    }

    // Map view transform state (pan & zoom)
    // Shift default Y offset downward (+65f) so northern OLTs/ONUs sit clearly below the top overlay cards
    var scale by remember { mutableFloatStateOf(1.0f) }
    var offset by remember { mutableStateOf(Offset(0f, 65f)) }
    var canvasSize by remember { mutableStateOf(Size(1080f, 1920f)) }

    val currentFilteredOnus by rememberUpdatedState(filteredOnus)
    val currentOlts by rememberUpdatedState(olts)
    val currentMikrotiks by rememberUpdatedState(mikrotiks)
    val currentMikrotikCoords by rememberUpdatedState(mikrotikCoordinates)
    val currentScale by rememberUpdatedState(scale)
    val currentOffset by rememberUpdatedState(offset)
    val currentOrigin by rememberUpdatedState(mapOrigin)

    // Pulsing halo & fiber optical packet animation
    val infiniteTransition = rememberInfiniteTransition(label = "mapPulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )
    val packetFlowProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "packetFlow"
    )

    val textMeasurer = rememberTextMeasurer()

    val mapBgColor = when (mapTheme) {
        GisMapTheme.STREET_GIS -> Color(0xFFF1F5F9)
        GisMapTheme.DARK_RADAR -> Color(0xFF0B1120)
        GisMapTheme.SATELLITE -> Color(0xFF14261C)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(mapBgColor)
            .testTag("customer_live_map_screen")
    ) {
        // Main Interactive GIS Canvas Live Map (Smooth 1-finger drag pan + tap node selection + double-tap zoom)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("live_map_canvas")
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offset += dragAmount
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            scale = (scale * 1.3f).coerceAtMost(3.0f)
                        },
                        onTap = { tapOffset ->
                            val centerW = size.width / 2f
                            val centerH = size.height / 2f

                            // 1. Check if user tapped a Customer ONU pin
                            val tappedOnu = currentFilteredOnus.find { onu ->
                                val screenPos = calculateNodePosition(
                                    onu.latitude, onu.longitude,
                                    currentOrigin.first, currentOrigin.second,
                                    currentScale, currentOffset, centerW, centerH
                                )
                                val dist = sqrt(
                                    (tapOffset.x - screenPos.x) * (tapOffset.x - screenPos.x) +
                                        (tapOffset.y - screenPos.y) * (tapOffset.y - screenPos.y)
                                )
                                dist <= 34.dp.toPx()
                            }

                            if (tappedOnu != null) {
                                selectedOltOnMap = null
                                selectedMikrotikOnMap = null
                                viewModel.selectMapOnu(tappedOnu)
                            } else {
                                // 2. Check if user tapped an OLT Central Hub
                                val tappedOlt = currentOlts.find { olt ->
                                    val oltPos = calculateNodePosition(
                                        olt.latitude, olt.longitude,
                                        currentOrigin.first, currentOrigin.second,
                                        currentScale, currentOffset, centerW, centerH
                                    )
                                    val dist = sqrt(
                                        (tapOffset.x - oltPos.x) * (tapOffset.x - oltPos.x) +
                                            (tapOffset.y - oltPos.y) * (tapOffset.y - oltPos.y)
                                    )
                                    dist <= 38.dp.toPx()
                                }
                                if (tappedOlt != null) {
                                    viewModel.selectMapOnu(null)
                                    selectedMikrotikOnMap = null
                                    selectedOltOnMap = tappedOlt
                                } else {
                                    // 3. Check if user tapped a MikroTik Core/BNG Router
                                    val tappedMtk = currentMikrotiks.find { mtk ->
                                        val coords = currentMikrotikCoords[mtk.id] ?: currentOrigin
                                        val mtkPos = calculateNodePosition(
                                            coords.first, coords.second,
                                            currentOrigin.first, currentOrigin.second,
                                            currentScale, currentOffset, centerW, centerH
                                        )
                                        val dist = sqrt(
                                            (tapOffset.x - mtkPos.x) * (tapOffset.x - mtkPos.x) +
                                                (tapOffset.y - mtkPos.y) * (tapOffset.y - mtkPos.y)
                                        )
                                        dist <= 38.dp.toPx()
                                    }
                                    if (tappedMtk != null) {
                                        viewModel.selectMapOnu(null)
                                        selectedOltOnMap = null
                                        selectedMikrotikOnMap = tappedMtk
                                    } else {
                                        viewModel.selectMapOnu(null)
                                        selectedOltOnMap = null
                                        selectedMikrotikOnMap = null
                                    }
                                }
                            }
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            canvasSize = Size(width, height)
            val centerW = width / 2f
            val centerH = height / 2f

            // 1. Draw GIS Cartographic Base Layer (Sectors, Roads, River, Building Footprints & Grid)
            drawCartographicBaseMap(
                width = width,
                height = height,
                offset = offset,
                scale = scale,
                mapTheme = mapTheme,
                textMeasurer = textMeasurer
            )

            // 2A. Draw Core Uplink Trunks from MikroTik Core/BNG Routers to OLTs
            if (uiState.showFiberRoutes && mikrotiks.isNotEmpty() && olts.isNotEmpty()) {
                olts.forEachIndexed { idx, olt ->
                    val parentMtk = mikrotiks[idx % mikrotiks.size]
                    val mtkCoord = mikrotikCoordinates[parentMtk.id] ?: mapOrigin
                    val mtkPos = calculateNodePosition(
                        mtkCoord.first, mtkCoord.second,
                        mapOrigin.first, mapOrigin.second,
                        scale, offset, centerW, centerH
                    )
                    val oltPos = calculateNodePosition(
                        olt.latitude, olt.longitude,
                        mapOrigin.first, mapOrigin.second,
                        scale, offset, centerW, centerH
                    )
                    val trunkQuality = olt.getLayerQuality(activeLayer)
                    val trunkColor = when (trunkQuality) {
                        NodeHealthQuality.OPTIMAL -> NocSuccessGreen.copy(alpha = 0.65f)
                        NodeHealthQuality.WARNING -> NocWarningAmber.copy(alpha = 0.85f)
                        NodeHealthQuality.CRITICAL -> NocCriticalRed.copy(alpha = 0.9f)
                        NodeHealthQuality.OFFLINE -> OpticalLosGray.copy(alpha = 0.55f)
                    }
                    drawLine(
                        color = trunkColor,
                        start = mtkPos,
                        end = oltPos,
                        strokeWidth = 3.dp.toPx() * scale.coerceIn(0.8f, 1.4f),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 8f))
                    )
                    if (olt.status != DeviceStatus.OFFLINE && parentMtk.status != DeviceStatus.OFFLINE) {
                        val t = packetFlowProgress
                        val px = mtkPos.x + (oltPos.x - mtkPos.x) * t
                        val py = mtkPos.y + (oltPos.y - mtkPos.y) * t
                        drawCircle(
                            color = trunkColor.copy(alpha = 1f),
                            radius = 4.dp.toPx() * scale.coerceIn(0.8f, 1.4f),
                            center = Offset(px, py)
                        )
                    }
                }
            }

            // 2B. Draw Fiber Cable Routes from OLTs to ONUs (Color-coded by Active Visualization Layer + Animated Light Pulses)
            if (uiState.showFiberRoutes) {
                filteredOnus.forEach { onu ->
                    val parentOlt = olts.find { it.id == onu.oltId } ?: olts.firstOrNull()
                    if (parentOlt != null) {
                        val oltPos = calculateNodePosition(
                            parentOlt.latitude, parentOlt.longitude,
                            mapOrigin.first, mapOrigin.second,
                            scale, offset, centerW, centerH
                        )
                        val onuPos = calculateNodePosition(
                            onu.latitude, onu.longitude,
                            mapOrigin.first, mapOrigin.second,
                            scale, offset, centerW, centerH
                        )

                        val nodeQuality = onu.getLayerQuality(activeLayer)
                        val lineColor = when (nodeQuality) {
                            NodeHealthQuality.OPTIMAL -> if (mapTheme == GisMapTheme.STREET_GIS) NocSuccessGreen.copy(alpha = 0.65f) else Color(0xFF10B981).copy(alpha = 0.75f)
                            NodeHealthQuality.WARNING -> NocWarningAmber.copy(alpha = 0.85f)
                            NodeHealthQuality.CRITICAL -> NocCriticalRed.copy(alpha = 0.92f)
                            NodeHealthQuality.OFFLINE -> OpticalLosGray.copy(alpha = 0.55f)
                        }

                        val midX = (oltPos.x + onuPos.x) / 2f
                        val midY = (oltPos.y + onuPos.y) / 2f - 22f * scale

                        val routePath = Path().apply {
                            moveTo(oltPos.x, oltPos.y)
                            quadraticTo(midX, midY, onuPos.x, onuPos.y)
                        }

                        drawPath(
                            path = routePath,
                            color = lineColor,
                            style = Stroke(
                                width = if (nodeQuality == NodeHealthQuality.CRITICAL) 3.2.dp.toPx() else 2.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = if (nodeQuality == NodeHealthQuality.OFFLINE) PathEffect.dashPathEffect(floatArrayOf(12f, 8f)) else null
                            )
                        )

                        // Animated optical pulse dot traveling from OLT to ONU along quadratic Bezier curve
                        if (onu.status != OnuStatus.OFFLINE && parentOlt.status != DeviceStatus.OFFLINE) {
                            val t = packetFlowProgress
                            val invT = 1f - t
                            val pulseX = invT * invT * oltPos.x + 2f * invT * t * midX + t * t * onuPos.x
                            val pulseY = invT * invT * oltPos.y + 2f * invT * t * midY + t * t * onuPos.y
                            drawCircle(
                                color = lineColor.copy(alpha = 1f),
                                radius = 3.5.dp.toPx() * scale.coerceIn(0.8f, 1.5f),
                                center = Offset(pulseX, pulseY)
                            )
                        }
                    }
                }
            }

            // 2C. Draw MikroTik Core/BNG Routers (Color-coded by active layer health)
            mikrotiks.forEach { mtk ->
                val coord = mikrotikCoordinates[mtk.id] ?: mapOrigin
                val mtkPos = calculateNodePosition(
                    coord.first, coord.second,
                    mapOrigin.first, mapOrigin.second,
                    scale, offset, centerW, centerH
                )
                val mtkQuality = mtk.getLayerQuality(activeLayer)
                val mtkColor = when (mtkQuality) {
                    NodeHealthQuality.OPTIMAL -> NocSuccessGreen
                    NodeHealthQuality.WARNING -> NocWarningAmber
                    NodeHealthQuality.CRITICAL -> NocCriticalRed
                    NodeHealthQuality.OFFLINE -> OpticalLosGray
                }
                val isMtkSelected = selectedMikrotikOnMap?.id == mtk.id

                // Outer Router Halo
                drawCircle(
                    color = mtkColor.copy(alpha = if (isMtkSelected) 0.38f else 0.22f),
                    radius = (if (isMtkSelected) 30.dp.toPx() else 23.dp.toPx()) * scale,
                    center = mtkPos
                )
                // Router Body
                drawCircle(
                    color = mtkColor,
                    radius = 15.dp.toPx() * scale,
                    center = mtkPos
                )
                drawCircle(
                    color = Color.White,
                    radius = 15.dp.toPx() * scale,
                    center = mtkPos,
                    style = Stroke(width = 2.dp.toPx())
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = "BNG",
                    topLeft = Offset(mtkPos.x - 10.dp.toPx() * scale, mtkPos.y - 6.dp.toPx() * scale),
                    style = TextStyle(
                        fontSize = (8.5f * scale).coerceIn(8f, 12f).sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )
                val mtkMetric = when (activeLayer) {
                    MapVisualizationLayer.LATENCY_AND_PACKET_LOSS ->
                        if (mtk.status == DeviceStatus.OFFLINE) "OFFLINE"
                        else "${String.format("%.1f", mtk.avgLatencyMs)}ms • ${String.format("%.1f", mtk.packetLossPercent)}%"
                    MapVisualizationLayer.AVG_LATENCY ->
                        if (mtk.status == DeviceStatus.OFFLINE) "OFFLINE" else "${String.format("%.1f", mtk.avgLatencyMs)} ms"
                    MapVisualizationLayer.PACKET_LOSS ->
                        "${String.format("%.1f", mtk.packetLossPercent)}% loss"
                    MapVisualizationLayer.OPTICAL_POWER ->
                        "${String.format("%.1f", mtk.avgLatencyMs)}ms • ${mtk.activePppoeClients} PPPoE"
                }
                val mtkLabel = "${mtk.name.take(12)} • $mtkMetric"
                val mtkLabelBg = if (mapTheme == GisMapTheme.STREET_GIS) Color(0xFF0F172A).copy(alpha = 0.9f) else Color(0xFF1E293B).copy(alpha = 0.94f)
                drawRoundRect(
                    color = mtkLabelBg,
                    topLeft = Offset(mtkPos.x - 68.dp.toPx(), mtkPos.y - 34.dp.toPx() * scale),
                    size = Size(138.dp.toPx(), 18.dp.toPx()),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = mtkLabel,
                    topLeft = Offset(mtkPos.x - 63.dp.toPx(), mtkPos.y - 32.dp.toPx() * scale),
                    style = TextStyle(
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            // 3. Draw OLT Central Hubs (Color-coded by active layer health)
            olts.forEach { olt ->
                val oltPos = calculateNodePosition(
                    olt.latitude, olt.longitude,
                    mapOrigin.first, mapOrigin.second,
                    scale, offset, centerW, centerH
                )

                val oltQuality = olt.getLayerQuality(activeLayer)

                val oltHubColor = when (oltQuality) {
                    NodeHealthQuality.OPTIMAL -> if (activeLayer == MapVisualizationLayer.OPTICAL_POWER) NocPrimaryBlue else NocSuccessGreen
                    NodeHealthQuality.WARNING -> NocWarningAmber
                    NodeHealthQuality.CRITICAL -> NocCriticalRed
                    NodeHealthQuality.OFFLINE -> OpticalLosGray
                }

                val isOltSelected = selectedOltOnMap?.id == olt.id

                // Outer OLT Halo
                drawCircle(
                    color = oltHubColor.copy(alpha = if (isOltSelected) 0.35f else 0.2f),
                    radius = (if (isOltSelected) 32.dp.toPx() else 26.dp.toPx()) * scale,
                    center = oltPos
                )
                // OLT Hub Body
                drawRoundRect(
                    color = oltHubColor,
                    topLeft = Offset(oltPos.x - 15.dp.toPx() * scale, oltPos.y - 15.dp.toPx() * scale),
                    size = Size(30.dp.toPx() * scale, 30.dp.toPx() * scale),
                    cornerRadius = CornerRadius(7.dp.toPx() * scale, 7.dp.toPx() * scale)
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(oltPos.x - 15.dp.toPx() * scale, oltPos.y - 15.dp.toPx() * scale),
                    size = Size(30.dp.toPx() * scale, 30.dp.toPx() * scale),
                    cornerRadius = CornerRadius(7.dp.toPx() * scale, 7.dp.toPx() * scale),
                    style = Stroke(width = 2.dp.toPx())
                )

                // OLT Inner Badge Text ("OLT")
                drawText(
                    textMeasurer = textMeasurer,
                    text = "OLT",
                    topLeft = Offset(oltPos.x - 10.dp.toPx() * scale, oltPos.y - 6.dp.toPx() * scale),
                    style = TextStyle(
                        fontSize = (9 * scale).coerceIn(8f, 13f).sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )

                // OLT Label Pill with Latency / Packet Loss telemetry
                val oltMetricSuffix = when (activeLayer) {
                    MapVisualizationLayer.LATENCY_AND_PACKET_LOSS ->
                        if (olt.status == DeviceStatus.OFFLINE) "OFFLINE / 100% LOSS"
                        else "${String.format("%.1f", olt.avgLatencyMs)}ms • ${String.format("%.1f", olt.packetLossPercent)}%"
                    MapVisualizationLayer.OPTICAL_POWER -> olt.vendor.displayName
                    MapVisualizationLayer.AVG_LATENCY -> if (olt.status == DeviceStatus.OFFLINE) "OFFLINE" else "${String.format("%.1f", olt.avgLatencyMs)} ms"
                    MapVisualizationLayer.PACKET_LOSS -> "${String.format("%.1f", olt.packetLossPercent)}% loss"
                }
                val oltLabel = "${olt.name.take(12)} • $oltMetricSuffix"
                val labelBg = if (mapTheme == GisMapTheme.STREET_GIS) Color(0xFF0F172A).copy(alpha = 0.88f) else Color(0xFF1E293B).copy(alpha = 0.92f)
                drawRoundRect(
                    color = labelBg,
                    topLeft = Offset(oltPos.x - 68.dp.toPx(), oltPos.y + 18.dp.toPx() * scale),
                    size = Size(140.dp.toPx(), 18.dp.toPx()),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = oltLabel,
                    topLeft = Offset(oltPos.x - 62.dp.toPx(), oltPos.y + 20.dp.toPx() * scale),
                    style = TextStyle(
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            // 4. Draw Customer ONU Nodes Color-Coded by Active Layer (Latency+Loss / Optical Power / Latency / Packet Loss)
            filteredOnus.forEach { onu ->
                val onuPos = calculateNodePosition(
                    onu.latitude, onu.longitude,
                    mapOrigin.first, mapOrigin.second,
                    scale, offset, centerW, centerH
                )

                val layerQuality = onu.getLayerQuality(activeLayer)
                val (nodeColor, textColor) = when (layerQuality) {
                    NodeHealthQuality.OPTIMAL -> Pair(OpticalSafeGreen, OpticalSafeGreen)
                    NodeHealthQuality.WARNING -> Pair(OpticalWarningAmber, OpticalWarningAmber)
                    NodeHealthQuality.CRITICAL -> Pair(OpticalCriticalRed, OpticalCriticalRed)
                    NodeHealthQuality.OFFLINE -> Pair(OpticalLosGray, OpticalLosGray)
                }

                val isSelected = uiState.selectedMapOnu?.id == onu.id

                // Pulse ring for active nodes
                if (onu.status != OnuStatus.OFFLINE) {
                    drawCircle(
                        color = nodeColor.copy(alpha = pulseAlpha),
                        radius = (pulseRadius + (if (isSelected) 8f else 0f)) * scale,
                        center = onuPos
                    )
                }

                // Selected highlight ring
                if (isSelected) {
                    drawCircle(
                        color = NocPrimaryBlue,
                        radius = 16.dp.toPx() * scale,
                        center = onuPos,
                        style = Stroke(width = 3.dp.toPx() * scale)
                    )
                }

                // Customer Node Dot
                drawCircle(
                    color = nodeColor,
                    radius = (if (isSelected) 11.dp.toPx() else 8.5.dp.toPx()) * scale,
                    center = onuPos
                )
                drawCircle(
                    color = Color.White,
                    radius = (if (isSelected) 4.5.dp.toPx() else 3.2.dp.toPx()) * scale,
                    center = onuPos
                )

                // Realtime Telemetry Badge adjacent to customer pin (Latency + Loss, Power dBm, Avg Latency ms, or Packet Loss %)
                if (uiState.showRealtimePowerBadge) {
                    val badgeText = when {
                        onu.status == OnuStatus.OFFLINE -> when (activeLayer) {
                            MapVisualizationLayer.LATENCY_AND_PACKET_LOSS -> "TIMEOUT • 100%"
                            MapVisualizationLayer.OPTICAL_POWER -> "LOS"
                            MapVisualizationLayer.AVG_LATENCY -> "TIMEOUT"
                            MapVisualizationLayer.PACKET_LOSS -> "100% LOSS"
                        }
                        activeLayer == MapVisualizationLayer.LATENCY_AND_PACKET_LOSS ->
                            "${String.format("%.1f", onu.avgLatencyMs)}ms • ${String.format("%.1f", onu.packetLossPercent)}%"
                        activeLayer == MapVisualizationLayer.OPTICAL_POWER -> "${String.format("%.1f", onu.rxPowerDbm)} dBm"
                        activeLayer == MapVisualizationLayer.AVG_LATENCY -> "${String.format("%.1f", onu.avgLatencyMs)} ms"
                        else -> "${String.format("%.1f", onu.packetLossPercent)}% Loss"
                    }

                    val badgeX = onuPos.x + 11.dp.toPx() * scale
                    val badgeY = onuPos.y - 14.dp.toPx() * scale
                    val badgeW = 84.dp.toPx() * scale.coerceIn(0.85f, 1.35f)
                    val badgeH = 26.dp.toPx() * scale.coerceIn(0.85f, 1.35f)

                    drawRoundRect(
                        color = if (mapTheme == GisMapTheme.STREET_GIS) Color.White.copy(alpha = 0.95f) else Color(0xFF0F172A).copy(alpha = 0.92f),
                        topLeft = Offset(badgeX, badgeY),
                        size = Size(badgeW, badgeH),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                    drawRoundRect(
                        color = nodeColor,
                        topLeft = Offset(badgeX, badgeY),
                        size = Size(badgeW, badgeH),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
                        style = Stroke(width = 1.4.dp.toPx())
                    )

                    // Customer short name + metric inside badge
                    drawText(
                        textMeasurer = textMeasurer,
                        text = onu.customerName.take(12),
                        topLeft = Offset(badgeX + 4.dp.toPx(), badgeY + 1.dp.toPx()),
                        style = TextStyle(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (mapTheme == GisMapTheme.STREET_GIS) Color(0xFF334155) else Color(0xFFCBD5E1)
                        )
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = badgeText,
                        topLeft = Offset(badgeX + 4.dp.toPx(), badgeY + 11.dp.toPx()),
                        style = TextStyle(
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    )
                }
            }
        }

        // Top Overlay: Status Summary, Visualization Layer Switcher & Map Theme Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Live Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("map_live_summary_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(NocSuccessGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GIS LAYER: ${activeLayer.label.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.6.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Map Theme Cycle Button (Street GIS / NOC Radar / Satellite)
                            OutlinedButton(
                                onClick = {
                                    mapTheme = when (mapTheme) {
                                        GisMapTheme.STREET_GIS -> GisMapTheme.DARK_RADAR
                                        GisMapTheme.DARK_RADAR -> GisMapTheme.SATELLITE
                                        GisMapTheme.SATELLITE -> GisMapTheme.STREET_GIS
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp).testTag("map_theme_toggle_btn")
                            ) {
                                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(mapTheme.label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            // Reset View Button
                            OutlinedButton(
                                onClick = {
                                    scale = 1.0f
                                    offset = Offset(0f, 65f)
                                    selectedOltOnMap = null
                                    selectedMikrotikOnMap = null
                                    viewModel.selectMapOnu(null)
                                },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp).testTag("reset_map_view_btn")
                            ) {
                                Icon(Icons.Default.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Fit All", fontSize = 10.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Layer Color-Coding Legend & Counts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val legendDesc = when (activeLayer) {
                            MapVisualizationLayer.LATENCY_AND_PACKET_LOSS -> "Green: ≤15ms/<0.5% • Amber: Warn • Red: >45ms/>2.5%"
                            MapVisualizationLayer.OPTICAL_POWER -> "Green: ±3dB Nominal • Amber: Warn • Red: Crit"
                            MapVisualizationLayer.AVG_LATENCY -> "Green: ≤15ms • Amber: 15-45ms • Red: >45ms"
                            MapVisualizationLayer.PACKET_LOSS -> "Green: <0.5% • Amber: 0.5-2.5% • Red: >2.5%"
                        }
                        Text(
                            text = legendDesc,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = NocTextSecondary
                        )
                        Text(
                            text = "$optimalCount OK • $warningCount Warn • $criticalCount Crit",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (criticalCount > 0) NocCriticalRed else NocSuccessGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Real-time Network Latency & Packet Loss Summary Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Connected Devices: ${mikrotiks.size} BNG • ${olts.size} OLTs • ${filteredOnus.size} ONUs",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NocTextSecondary
                        )
                        Text(
                            text = "Net Avg: ${String.format("%.1f", networkAvgLatencyMs)} ms • ${String.format("%.2f", networkAvgLossPercent)}% loss",
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Visualization Layer Selector Row (Latency+Loss vs Avg Latency vs Packet Loss vs Optical Power)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = activeLayer == MapVisualizationLayer.LATENCY_AND_PACKET_LOSS,
                        onClick = { viewModel.setMapVisualizationLayer(MapVisualizationLayer.LATENCY_AND_PACKET_LOSS) },
                        label = { Text("Latency & Loss (Live)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(Icons.Default.Insights, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        modifier = Modifier.height(30.dp).testTag("map_layer_latency_packet_loss")
                    )
                }
                item {
                    FilterChip(
                        selected = activeLayer == MapVisualizationLayer.AVG_LATENCY,
                        onClick = { viewModel.setMapVisualizationLayer(MapVisualizationLayer.AVG_LATENCY) },
                        label = { Text("Avg Latency (ms)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        modifier = Modifier.height(30.dp).testTag("map_layer_avg_latency")
                    )
                }
                item {
                    FilterChip(
                        selected = activeLayer == MapVisualizationLayer.PACKET_LOSS,
                        onClick = { viewModel.setMapVisualizationLayer(MapVisualizationLayer.PACKET_LOSS) },
                        label = { Text("Packet Loss (%)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(Icons.Default.WarningAmber, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        modifier = Modifier.height(30.dp).testTag("map_layer_packet_loss")
                    )
                }
                item {
                    FilterChip(
                        selected = activeLayer == MapVisualizationLayer.OPTICAL_POWER,
                        onClick = { viewModel.setMapVisualizationLayer(MapVisualizationLayer.OPTICAL_POWER) },
                        label = { Text("Optical Power (dBm)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        modifier = Modifier.height(30.dp).testTag("map_layer_optical_power")
                    )
                }
            }

            // Map Option Toggles & Quality Filters
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = uiState.showRealtimePowerBadge,
                        onClick = { viewModel.toggleRealtimePowerBadge() },
                        label = { Text("Node Badges", fontSize = 10.sp) },
                        leadingIcon = {
                            Icon(
                                if (uiState.showRealtimePowerBadge) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                        },
                        modifier = Modifier.height(28.dp).testTag("toggle_realtime_power_badges")
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.showFiberRoutes,
                        onClick = { viewModel.toggleFiberRoutes() },
                        label = { Text("Fiber Links", fontSize = 10.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.AltRoute, contentDescription = null, modifier = Modifier.size(13.dp))
                        },
                        modifier = Modifier.height(28.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.mapFilterQuality == null,
                        onClick = { viewModel.setMapFilterQuality(null) },
                        label = { Text("All (${onus.size})", fontSize = 10.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.mapFilterQuality == OpticalQuality.CRITICAL,
                        onClick = {
                            viewModel.setMapFilterQuality(
                                if (uiState.mapFilterQuality == OpticalQuality.CRITICAL) null else OpticalQuality.CRITICAL
                            )
                        },
                        label = { Text("Critical", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFE4E6),
                            selectedLabelColor = NocCriticalRed
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.mapFilterQuality == OpticalQuality.WARNING,
                        onClick = {
                            viewModel.setMapFilterQuality(
                                if (uiState.mapFilterQuality == OpticalQuality.WARNING) null else OpticalQuality.WARNING
                            )
                        },
                        label = { Text("Warning", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFEF3C7),
                            selectedLabelColor = NocWarningAmber
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }

        // Floating Zoom Controls (+ / -)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalIconButton(
                onClick = { scale = (scale * 1.25f).coerceAtMost(3.0f) },
                shape = CircleShape,
                modifier = Modifier.testTag("map_zoom_in_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }
            FilledTonalIconButton(
                onClick = { scale = (scale * 0.8f).coerceAtLeast(0.6f) },
                shape = CircleShape,
                modifier = Modifier.testTag("map_zoom_out_btn")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
            }
        }

        // Bottom Interactive Node Directory Strip (Quick Jump to any MikroTik, OLT, or Customer ONU)
        if (uiState.selectedMapOnu == null && selectedOltOnMap == null && selectedMikrotikOnMap == null) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .testTag("map_node_directory_bar")
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TAP MARKER TO INSPECT (${mikrotiks.size} BNG • ${olts.size} OLTs • ${filteredOnus.size} ONUs)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Drag to Pan • +/- Zoom",
                            fontSize = 10.sp,
                            color = NocTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // MikroTik Core/BNG Quick Jump Chips
                        items(mikrotiks, key = { it.id }) { mtk ->
                            val mtkCoord = mikrotikCoordinates[mtk.id] ?: mapOrigin
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFECFDF5),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        scale = 1.35f
                                        val dx = -((mtkCoord.second - mapOrigin.second).toFloat() * 22000f * scale)
                                        val dy = ((mtkCoord.first - mapOrigin.first).toFloat() * 18000f * scale)
                                        offset = Offset(dx, dy)
                                        viewModel.selectMapOnu(null)
                                        selectedOltOnMap = null
                                        selectedMikrotikOnMap = mtk
                                    }
                                    .testTag("map_quick_mtk_${mtk.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Router,
                                        contentDescription = null,
                                        tint = NocSuccessGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Column {
                                        Text(mtk.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${String.format("%.1f", mtk.avgLatencyMs)}ms • ${String.format("%.1f", mtk.packetLossPercent)}% loss",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = NocSuccessGreen
                                        )
                                    }
                                }
                            }
                        }

                        // OLT Quick Jump Chips
                        items(olts, key = { it.id }) { olt ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        // Center map onto this OLT
                                        scale = 1.35f
                                        val dx = -((olt.longitude - mapOrigin.second).toFloat() * 22000f * scale)
                                        val dy = ((olt.latitude - mapOrigin.first).toFloat() * 18000f * scale)
                                        offset = Offset(dx, dy)
                                        viewModel.selectMapOnu(null)
                                        selectedMikrotikOnMap = null
                                        selectedOltOnMap = olt
                                    }
                                    .testTag("map_quick_olt_${olt.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Hub,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Column {
                                        Text(olt.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${String.format("%.1f", olt.avgLatencyMs)}ms • ${String.format("%.1f", olt.packetLossPercent)}% loss",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = NocTextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // Customer ONU Quick Jump Chips
                        items(filteredOnus, key = { it.id }) { onu ->
                            val q = onu.getLayerQuality(activeLayer)
                            val dotColor = when (q) {
                                NodeHealthQuality.OPTIMAL -> NocSuccessGreen
                                NodeHealthQuality.WARNING -> NocWarningAmber
                                NodeHealthQuality.CRITICAL -> NocCriticalRed
                                NodeHealthQuality.OFFLINE -> OpticalLosGray
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        scale = 1.4f
                                        val dx = -((onu.longitude - mapOrigin.second).toFloat() * 22000f * scale)
                                        val dy = ((onu.latitude - mapOrigin.first).toFloat() * 18000f * scale)
                                        offset = Offset(dx, dy)
                                        selectedOltOnMap = null
                                        selectedMikrotikOnMap = null
                                        viewModel.selectMapOnu(onu)
                                    }
                                    .testTag("map_quick_onu_${onu.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                    Column {
                                        Text(onu.customerName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        val subText = if (onu.status == OnuStatus.OFFLINE) "LOS • 100% loss" else "${String.format("%.1f", onu.avgLatencyMs)}ms • ${String.format("%.1f", onu.packetLossPercent)}% loss"
                                        Text(
                                            text = subText,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = dotColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Selected MikroTik Live Telemetry Card
        AnimatedVisibility(
            visible = selectedMikrotikOnMap != null && selectedOltOnMap == null && uiState.selectedMapOnu == null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedMikrotikOnMap?.let { mtk ->
                MikrotikMapDetailCard(
                    mikrotik = mtk,
                    onClose = { selectedMikrotikOnMap = null },
                    onOpenMikrotikConsole = {
                        viewModel.selectRouter(mtk.id)
                        viewModel.selectTab(NocNavTab.MIKROTIK)
                    }
                )
            }
        }

        // Bottom Selected OLT Live Telemetry Card
        AnimatedVisibility(
            visible = selectedOltOnMap != null && uiState.selectedMapOnu == null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedOltOnMap?.let { olt ->
                val connectedOnuCount = onus.count { it.oltId == olt.id }
                OltMapDetailCard(
                    olt = olt,
                    connectedOnuCount = connectedOnuCount,
                    onClose = { selectedOltOnMap = null },
                    onOpenOltConsole = {
                        viewModel.selectOlt(olt.id)
                        viewModel.selectTab(NocNavTab.OLT_ONU)
                    }
                )
            }
        }

        // Bottom Selected Customer Live Telemetry Card
        AnimatedVisibility(
            visible = uiState.selectedMapOnu != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            uiState.selectedMapOnu?.let { selectedOnu ->
                val liveOnu = onus.find { it.id == selectedOnu.id } ?: selectedOnu
                CustomerMapDetailCard(
                    onu = liveOnu,
                    onClose = { viewModel.selectMapOnu(null) },
                    onInspect = { viewModel.inspectOnu(liveOnu) },
                    onDegrade = { viewModel.triggerOpticalDegradation(liveOnu.id) }
                )
            }
        }
    }

    // Render ONU Power History & Optical Diagnostics Modal when triggered from Live Map!
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
}

@Composable
private fun MikrotikMapDetailCard(
    mikrotik: MikrotikDevice,
    onClose: () -> Unit,
    onOpenMikrotikConsole: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
            .testTag("mikrotik_map_detail_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "MIKROTIK CORE / BNG",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NocSuccessGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "RouterOS ${mikrotik.routerOsVersion}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NocTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${mikrotik.name} (${mikrotik.model})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "IP: ${mikrotik.ipAddress}:${mikrotik.apiPort} • Uptime: ${mikrotik.uptimeString}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = NocTextSecondary
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = NocTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("AVG LATENCY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NocTextSecondary)
                        Text(
                            text = "${String.format("%.1f", mikrotik.avgLatencyMs)} ms",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (mikrotik.avgLatencyMs <= 15f) NocSuccessGreen else NocWarningAmber
                        )
                        Text("Loss: ${String.format("%.2f", mikrotik.packetLossPercent)}%", fontSize = 9.sp, color = NocTextMuted)
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("THROUGHPUT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NocTextSecondary)
                        Text(
                            text = Formatters.formatBps(mikrotik.currentRxBps),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Util: ${String.format("%.1f", mikrotik.bandwidthUtilizationPercent)}%", fontSize = 9.sp, color = NocTextMuted)
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("SUBSCRIBERS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NocTextSecondary)
                        Text(
                            text = "${mikrotik.activePppoeClients} PPPoE",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NocSuccessGreen
                        )
                        Text("CPU: ${mikrotik.cpuUsage}%", fontSize = 9.sp, color = NocTextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onOpenMikrotikConsole,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Router, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open MikroTik RouterOS Telemetry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun OltMapDetailCard(
    olt: OltDevice,
    connectedOnuCount: Int,
    onClose: () -> Unit,
    onOpenOltConsole: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
            .testTag("olt_map_detail_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "OLT CENTRAL HUB",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = olt.vendor.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NocTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${olt.name} (${olt.model})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${olt.location} • IP: ${olt.ipAddress}:${olt.port}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = NocTextSecondary
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = NocTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("UPLINK UTIL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NocTextSecondary)
                        Text(
                            text = "${String.format("%.1f", olt.bandwidthUtilizationPercent)}%",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(Formatters.formatBps(olt.currentRxBps), fontSize = 9.sp, color = NocTextMuted)
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("AVG LATENCY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NocTextSecondary)
                        Text(
                            text = "${String.format("%.1f", olt.avgLatencyMs)} ms",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (olt.avgLatencyMs <= 15f) NocSuccessGreen else NocWarningAmber
                        )
                        Text("Loss: ${String.format("%.2f", olt.packetLossPercent)}%", fontSize = 9.sp, color = NocTextMuted)
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("PON / ONUs", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NocTextSecondary)
                        Text(
                            text = "$connectedOnuCount Active",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NocSuccessGreen
                        )
                        Text("${olt.totalPonPorts} PON Ports", fontSize = 9.sp, color = NocTextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onOpenOltConsole,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open OLT & PON Port Console", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Bottom Floating Card for tapped Customer ONT on Live Map
 * Displays Optical Power, Average Latency (ms), and Packet Loss (%)
 */
@Composable
fun CustomerMapDetailCard(
    onu: OnuDevice,
    onClose: () -> Unit,
    onInspect: () -> Unit,
    onDegrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(14.dp)
            .testTag("customer_map_detail_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = onu.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = onu.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = NocTextSecondary
                    )
                    Text(
                        text = "SN: ${onu.serialNumber} • PON ${onu.ponPort} • ${onu.planSpeed}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = NocTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3-Column Node Telemetry Strip (Optical Power, Avg Latency, Packet Loss)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("OPTICAL RX", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NocTextSecondary)
                        Text(
                            text = if (onu.status == OnuStatus.OFFLINE) "LOS" else "${String.format("%.1f", onu.rxPowerDbm)} dBm",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (onu.isOutsideNominalRange()) NocWarningAmber else NocSuccessGreen
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("AVG LATENCY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NocTextSecondary)
                        val latColor = when (onu.getLatencyQuality()) {
                            NodeHealthQuality.OPTIMAL -> NocSuccessGreen
                            NodeHealthQuality.WARNING -> NocWarningAmber
                            else -> NocCriticalRed
                        }
                        Text(
                            text = if (onu.status == OnuStatus.OFFLINE) "Timeout" else "${String.format("%.1f", onu.avgLatencyMs)} ms",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = latColor
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("PACKET LOSS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NocTextSecondary)
                        val lossColor = when (onu.getPacketLossQuality()) {
                            NodeHealthQuality.OPTIMAL -> NocSuccessGreen
                            NodeHealthQuality.WARNING -> NocWarningAmber
                            else -> NocCriticalRed
                        }
                        Text(
                            text = "${String.format("%.1f", onu.packetLossPercent)}%",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = lossColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Realtime Optical Power Gauge
            OpticalPowerGauge(
                rxPowerDbm = onu.rxPowerDbm,
                txPowerDbm = onu.txPowerDbm,
                status = onu.status
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onInspect,
                    modifier = Modifier.weight(1f).testTag("map_onu_power_history_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Power History", fontSize = 11.sp)
                }

                Button(
                    onClick = onDegrade,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF3C7), contentColor = NocWarningAmber),
                    modifier = Modifier.weight(1f).testTag("map_onu_test_degrade_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Test Degrade", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun calculateNodePosition(
    lat: Double,
    lng: Double,
    originLat: Double,
    originLng: Double,
    scale: Float,
    offset: Offset,
    centerW: Float,
    centerH: Float
): Offset {
    val latScale = 18000f * scale
    val lngScale = 22000f * scale

    val deltaX = (lng - originLng).toFloat() * lngScale
    val deltaY = -(lat - originLat).toFloat() * latScale

    val posX = centerW + deltaX + offset.x
    val posY = centerH + deltaY + offset.y

    return Offset(posX, posY)
}

/**
 * Draws a rich cartographic GIS map background with Cyber City Sectors, Parks, Water Canal,
 * Major Highways, Secondary Roads, Building Blocks, and Sector Labels.
 */
private fun DrawScope.drawCartographicBaseMap(
    width: Float,
    height: Float,
    offset: Offset,
    scale: Float,
    mapTheme: GisMapTheme,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val isDark = mapTheme != GisMapTheme.STREET_GIS
    val gridColor = when (mapTheme) {
        GisMapTheme.STREET_GIS -> Color(0xFFE2E8F0)
        GisMapTheme.DARK_RADAR -> Color(0xFF1E293B)
        GisMapTheme.SATELLITE -> Color(0xFF1E3A2F)
    }
    val blockColor = when (mapTheme) {
        GisMapTheme.STREET_GIS -> Color(0xFFE8EEF5)
        GisMapTheme.DARK_RADAR -> Color(0xFF111C33)
        GisMapTheme.SATELLITE -> Color(0xFF1A3326)
    }
    val parkColor = when (mapTheme) {
        GisMapTheme.STREET_GIS -> Color(0xFFDCFCE7).copy(alpha = 0.65f)
        GisMapTheme.DARK_RADAR -> Color(0xFF064E3B).copy(alpha = 0.3f)
        GisMapTheme.SATELLITE -> Color(0xFF14532D).copy(alpha = 0.55f)
    }
    val waterColor = when (mapTheme) {
        GisMapTheme.STREET_GIS -> Color(0xFFBAE6FD).copy(alpha = 0.75f)
        GisMapTheme.DARK_RADAR -> Color(0xFF0C4A6E).copy(alpha = 0.5f)
        GisMapTheme.SATELLITE -> Color(0xFF0369A1).copy(alpha = 0.55f)
    }
    val majorRoadColor = when (mapTheme) {
        GisMapTheme.STREET_GIS -> Color(0xFFFFFFFF)
        GisMapTheme.DARK_RADAR -> Color(0xFF334155)
        GisMapTheme.SATELLITE -> Color(0xFF475569)
    }
    val highwayColor = when (mapTheme) {
        GisMapTheme.STREET_GIS -> Color(0xFFFDE68A)
        GisMapTheme.DARK_RADAR -> Color(0xFF475569)
        GisMapTheme.SATELLITE -> Color(0xFFD97706).copy(alpha = 0.6f)
    }
    val sectorTextColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

    val centerW = width / 2f + offset.x
    val centerH = height / 2f + offset.y

    // 1. Coordinate Grid Lines
    val step = (55f * scale).coerceAtLeast(24f)
    val startX = ((offset.x % step) + step) % step
    val startY = ((offset.y % step) + step) % step

    var x = startX
    while (x < width) {
        drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, height), strokeWidth = 1f)
        x += step
    }
    var y = startY
    while (y < height) {
        drawLine(color = gridColor, start = Offset(0f, y), end = Offset(width, y), strokeWidth = 1f)
        y += step
    }

    // 2. Green Belt & Tech Park Zones
    drawRoundRect(
        color = parkColor,
        topLeft = Offset(centerW - 220f * scale, centerH - 250f * scale),
        size = Size(160f * scale, 110f * scale),
        cornerRadius = CornerRadius(16f * scale, 16f * scale)
    )
    drawRoundRect(
        color = parkColor,
        topLeft = Offset(centerW + 60f * scale, centerH + 80f * scale),
        size = Size(180f * scale, 130f * scale),
        cornerRadius = CornerRadius(16f * scale, 16f * scale)
    )

    // 3. City Building Blocks
    val blocks = listOf(
        Offset(-140f, -80f) to Size(95f, 70f),
        Offset(30f, -110f) to Size(110f, 80f),
        Offset(-180f, 40f) to Size(120f, 95f),
        Offset(-30f, 20f) to Size(80f, 75f),
        Offset(50f, -240f) to Size(130f, 90f),
        Offset(-190f, 180f) to Size(140f, 85f)
    )
    blocks.forEach { (relPos, relSize) ->
        drawRoundRect(
            color = blockColor,
            topLeft = Offset(centerW + relPos.x * scale, centerH + relPos.y * scale),
            size = Size(relSize.width * scale, relSize.height * scale),
            cornerRadius = CornerRadius(6f * scale, 6f * scale)
        )
    }

    // 4. Waterfront Canal / River
    val riverPath = Path().apply {
        moveTo(0f, centerH + 220f * scale)
        quadraticTo(centerW - 80f * scale, centerH + 150f * scale, centerW + 120f * scale, centerH + 210f * scale)
        lineTo(width, centerH + 170f * scale)
    }
    drawPath(
        path = riverPath,
        color = waterColor,
        style = Stroke(width = 24f * scale, cap = StrokeCap.Round)
    )

    // 5. Secondary Arterial Roads
    val roadOffsets = listOf(-180f, -60f, 70f, 190f)
    roadOffsets.forEach { r ->
        drawLine(
            color = majorRoadColor,
            start = Offset(0f, centerH + r * scale),
            end = Offset(width, centerH + r * scale),
            strokeWidth = 6f * scale
        )
        drawLine(
            color = majorRoadColor,
            start = Offset(centerW + r * scale, 0f),
            end = Offset(centerW + r * scale, height),
            strokeWidth = 6f * scale
        )
    }

    // 6. Cyber City Express Highway (Diagonal Trunk Corridor)
    drawLine(
        color = highwayColor,
        start = Offset(centerW - 350f * scale, centerH + 280f * scale),
        end = Offset(centerW + 350f * scale, centerH - 320f * scale),
        strokeWidth = 10f * scale
    )

    // 7. Sector & Landmark Labels
    val sectors = listOf(
        Triple("SECTOR I • CENTRAL NOC HQ", -60f, -15f),
        Triple("SECTOR II • NORTH SUBSTATION", -40f, -195f),
        Triple("SECTOR V • SOUTH HARBOR POP", -145f, 165f),
        Triple("SILICON TECH CORRIDOR", 55f, -50f),
        Triple("METRO ARTERIAL EXPRESSWAY", 75f, 95f)
    )
    sectors.forEach { (title, dx, dy) ->
        drawText(
            textMeasurer = textMeasurer,
            text = title,
            topLeft = Offset(centerW + dx * scale, centerH + dy * scale),
            style = TextStyle(
                fontSize = (9 * scale).coerceIn(8f, 12f).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = sectorTextColor
            )
        )
    }
}
