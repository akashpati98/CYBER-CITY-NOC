package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
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
import com.example.model.HourlyTrafficPoint
import com.example.model.NetworkInterface
import com.example.ui.theme.*
import kotlin.math.roundToInt

/**
 * Recharts-inspired 24-Hour Historical Interface Traffic Dashboard Widget.
 * Visualizes historical IN (RX) and OUT (TX) throughput across the past 24 hours
 * with interactive tooltip inspection, 95th percentile indicator, and interface selector.
 */
@Composable
fun Historical24hTrafficWidget(
    hourlyPoints: List<HourlyTrafficPoint>,
    interfaces: List<NetworkInterface>,
    selectedInterfaceId: String?,
    onSelectInterface: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedHourIdx by remember(hourlyPoints) {
        mutableStateOf(hourlyPoints.lastIndex.coerceAtLeast(0))
    }

    val activePoint = hourlyPoints.getOrNull(selectedHourIdx) ?: hourlyPoints.lastOrNull()
    val maxThroughput = remember(hourlyPoints) {
        hourlyPoints.maxOfOrNull { maxOf(it.inBps, it.outBps, it.peakInBps) }?.coerceAtLeast(100_000_000L)
            ?: 10_000_000_000L
    }
    val chartCeiling = (maxThroughput * 1.15f).toLong()

    val avgInBps = remember(hourlyPoints) {
        if (hourlyPoints.isEmpty()) 0L else hourlyPoints.map { it.inBps }.average().toLong()
    }
    val avgOutBps = remember(hourlyPoints) {
        if (hourlyPoints.isEmpty()) 0L else hourlyPoints.map { it.outBps }.average().toLong()
    }
    val percentile95Bps = remember(hourlyPoints) {
        if (hourlyPoints.isEmpty()) 0L
        else {
            val sorted = hourlyPoints.map { maxOf(it.inBps, it.outBps) }.sorted()
            val idx = ((sorted.size - 1) * 0.95f).roundToInt().coerceIn(0, sorted.lastIndex)
            sorted[idx]
        }
    }

    val textMeasurer = rememberTextMeasurer()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("historical_24h_traffic_widget"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Timeline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "24-HOUR INTERFACE TRAFFIC HISTORY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "In/Out Historical Throughput (24h)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 95th Percentile Badge
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("95th PERCENTILE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NocTextSecondary)
                        Text(
                            text = Formatters.formatBps(percentile95Bps),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NocWarningAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interface Selector Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedInterfaceId == null,
                        onClick = { onSelectInterface(null) },
                        label = { Text("All Uplinks (Aggregated)", fontSize = 11.sp) },
                        modifier = Modifier.testTag("24h_filter_all_uplinks")
                    )
                }
                items(interfaces.take(6)) { intf ->
                    FilterChip(
                        selected = selectedInterfaceId == intf.id,
                        onClick = { onSelectInterface(intf.id) },
                        label = { Text(intf.name, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Recharts-style Tooltip Banner for Selected Hour
            if (activePoint != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "WINDOW: ${activePoint.hourLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap chart to inspect hour",
                            fontSize = 9.sp,
                            color = NocTextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(TrafficRxColor)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("IN (RX)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NocTextSecondary)
                            }
                            Text(
                                text = Formatters.formatBps(activePoint.inBps),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = TrafficRxColor
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(TrafficTxColor)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("OUT (TX)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NocTextSecondary)
                            }
                            Text(
                                text = Formatters.formatBps(activePoint.outBps),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = TrafficTxColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Recharts-style Area & Line Chart Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .pointerInput(hourlyPoints) {
                        detectTapGestures { tapOffset ->
                            if (hourlyPoints.size > 1 && size.width > 0) {
                                val fraction = (tapOffset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                val idx = (fraction * (hourlyPoints.size - 1)).roundToInt()
                                    .coerceIn(0, hourlyPoints.lastIndex)
                                selectedHourIdx = idx
                            }
                        }
                    }
            ) {
                val width = size.width
                val height = size.height - 22.dp.toPx() // Reserve bottom space for X-axis hour labels

                // 1. Cartesian Grid (Horizontal dashed lines + Y-axis labels)
                val gridLevels = 4
                val gridDash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                for (i in 0..gridLevels) {
                    val y = height * (i.toFloat() / gridLevels)
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f,
                        pathEffect = if (i < gridLevels) gridDash else null
                    )
                }

                if (hourlyPoints.size < 2) return@Canvas

                val stepX = width / (hourlyPoints.size - 1).coerceAtLeast(1)

                // 2. 95th Percentile Reference Line
                val p95Fraction = (percentile95Bps.toFloat() / chartCeiling).coerceIn(0f, 1f)
                val p95Y = height - (p95Fraction * height)
                drawLine(
                    color = NocWarningAmber.copy(alpha = 0.65f),
                    start = Offset(0f, p95Y),
                    end = Offset(width, p95Y),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                )

                // 3. Build Smooth Cubic Paths for IN (RX) and OUT (TX)
                val inPath = Path()
                val inFillPath = Path()
                val outPath = Path()
                val outFillPath = Path()

                hourlyPoints.forEachIndexed { index, pt ->
                    val x = index * stepX
                    val inY = height - ((pt.inBps.toFloat() / chartCeiling).coerceIn(0f, 1f) * height)
                    val outY = height - ((pt.outBps.toFloat() / chartCeiling).coerceIn(0f, 1f) * height)

                    if (index == 0) {
                        inPath.moveTo(x, inY)
                        inFillPath.moveTo(x, height)
                        inFillPath.lineTo(x, inY)

                        outPath.moveTo(x, outY)
                        outFillPath.moveTo(x, height)
                        outFillPath.lineTo(x, outY)
                    } else {
                        val prevX = (index - 1) * stepX
                        val prevInY = height - ((hourlyPoints[index - 1].inBps.toFloat() / chartCeiling).coerceIn(0f, 1f) * height)
                        val prevOutY = height - ((hourlyPoints[index - 1].outBps.toFloat() / chartCeiling).coerceIn(0f, 1f) * height)
                        val midX = (prevX + x) / 2f

                        inPath.cubicTo(midX, prevInY, midX, inY, x, inY)
                        inFillPath.cubicTo(midX, prevInY, midX, inY, x, inY)

                        outPath.cubicTo(midX, prevOutY, midX, outY, x, outY)
                        outFillPath.cubicTo(midX, prevOutY, midX, outY, x, outY)
                    }
                }

                inFillPath.lineTo((hourlyPoints.size - 1) * stepX, height)
                inFillPath.close()

                outFillPath.lineTo((hourlyPoints.size - 1) * stepX, height)
                outFillPath.close()

                // Draw Area Fills & Strokes
                drawPath(
                    path = inFillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(TrafficRxColor.copy(alpha = 0.28f), TrafficRxColor.copy(alpha = 0.02f)),
                        startY = 0f,
                        endY = height
                    )
                )
                drawPath(
                    path = inPath,
                    color = TrafficRxColor,
                    style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                )

                drawPath(
                    path = outFillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(TrafficTxColor.copy(alpha = 0.22f), TrafficTxColor.copy(alpha = 0.02f)),
                        startY = 0f,
                        endY = height
                    )
                )
                drawPath(
                    path = outPath,
                    color = TrafficTxColor,
                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                )

                // 4. Draw X-Axis Hourly Tick Labels every 4 hours
                val labelIndices = listOf(0, 4, 8, 12, 16, 20, hourlyPoints.lastIndex).distinct()
                labelIndices.forEach { idx ->
                    if (idx in hourlyPoints.indices) {
                        val x = (idx * stepX).coerceIn(0f, width - 28.dp.toPx())
                        drawText(
                            textMeasurer = textMeasurer,
                            text = hourlyPoints[idx].hourLabel,
                            topLeft = Offset(x, height + 5.dp.toPx()),
                            style = TextStyle(
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                // 5. Interactive Crosshair & Highlight Dots for Selected Hour
                val safeIdx = selectedHourIdx.coerceIn(0, hourlyPoints.lastIndex)
                val selPt = hourlyPoints[safeIdx]
                val selX = safeIdx * stepX
                val selInY = height - ((selPt.inBps.toFloat() / chartCeiling).coerceIn(0f, 1f) * height)
                val selOutY = height - ((selPt.outBps.toFloat() / chartCeiling).coerceIn(0f, 1f) * height)

                drawLine(
                    color = NocPrimaryBlue.copy(alpha = 0.5f),
                    start = Offset(selX, 0f),
                    end = Offset(selX, height),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                drawCircle(color = TrafficRxColor, radius = 5.5.dp.toPx(), center = Offset(selX, selInY))
                drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = Offset(selX, selInY))

                drawCircle(color = TrafficTxColor, radius = 5.5.dp.toPx(), center = Offset(selX, selOutY))
                drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = Offset(selX, selOutY))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 24-Hour Summary Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("24H AVG IN (RX)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NocTextSecondary)
                    Text(
                        text = Formatters.formatBps(avgInBps),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TrafficRxColor
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("24H AVG OUT (TX)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NocTextSecondary)
                    Text(
                        text = Formatters.formatBps(avgOutBps),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TrafficTxColor
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("24H PEAK LOAD", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NocTextSecondary)
                    Text(
                        text = Formatters.formatBps(maxThroughput),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
