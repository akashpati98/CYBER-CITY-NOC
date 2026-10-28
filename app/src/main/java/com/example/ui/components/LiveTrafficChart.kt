package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TrafficPoint
import com.example.ui.theme.TrafficRxColor
import com.example.ui.theme.TrafficTxColor

@Composable
fun LiveTrafficChart(
    points: List<TrafficPoint>,
    interfaceName: String,
    modifier: Modifier = Modifier
) {
    val currentRx = points.lastOrNull()?.rxBps ?: 0L
    val currentTx = points.lastOrNull()?.txBps ?: 0L

    val maxVal = points.maxOfOrNull { maxOf(it.rxBps, it.txBps) }?.coerceAtLeast(10_000_000L) ?: 100_000_000L
    // Add 15% headroom for clean visual space
    val chartCeiling = (maxVal * 1.15f).toLong()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Chart Header with Live Rates
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "REAL-TIME TRAFFIC STATISTICS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = interfaceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Legend indicators
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(TrafficRxColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "RX: ${Formatters.formatBps(currentRx)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TrafficRxColor
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(TrafficTxColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "TX: ${Formatters.formatBps(currentTx)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TrafficTxColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Canvas Waveform
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val width = size.width
            val height = size.height

            // 1. Draw horizontal grid lines & axis labels
            val gridCount = 3
            val gridColor = Color(0xFFE2E8F0)
            for (i in 0..gridCount) {
                val y = height * (i.toFloat() / gridCount)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            if (points.size < 2) return@Canvas

            val stepX = width / (points.size - 1).coerceAtLeast(1)

            // 2. Generate Path for RX
            val rxPath = Path()
            val rxFillPath = Path()

            points.forEachIndexed { index, point ->
                val x = index * stepX
                val yFraction = (point.rxBps.toFloat() / chartCeiling).coerceIn(0f, 1f)
                val y = height - (yFraction * height)

                if (index == 0) {
                    rxPath.moveTo(x, y)
                    rxFillPath.moveTo(x, height)
                    rxFillPath.lineTo(x, y)
                } else {
                    val prevX = (index - 1) * stepX
                    val prevYFraction = (points[index - 1].rxBps.toFloat() / chartCeiling).coerceIn(0f, 1f)
                    val prevY = height - (prevYFraction * height)

                    val midX = (prevX + x) / 2f
                    rxPath.cubicTo(midX, prevY, midX, y, x, y)
                    rxFillPath.cubicTo(midX, prevY, midX, y, x, y)
                }
            }
            rxFillPath.lineTo((points.size - 1) * stepX, height)
            rxFillPath.close()

            // Draw RX gradient area fill
            drawPath(
                path = rxFillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(TrafficRxColor.copy(alpha = 0.25f), TrafficRxColor.copy(alpha = 0.02f)),
                    startY = 0f,
                    endY = height
                )
            )

            // Draw RX curve stroke
            drawPath(
                path = rxPath,
                color = TrafficRxColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3. Generate Path for TX
            val txPath = Path()
            val txFillPath = Path()

            points.forEachIndexed { index, point ->
                val x = index * stepX
                val yFraction = (point.txBps.toFloat() / chartCeiling).coerceIn(0f, 1f)
                val y = height - (yFraction * height)

                if (index == 0) {
                    txPath.moveTo(x, y)
                    txFillPath.moveTo(x, height)
                    txFillPath.lineTo(x, y)
                } else {
                    val prevX = (index - 1) * stepX
                    val prevYFraction = (points[index - 1].txBps.toFloat() / chartCeiling).coerceIn(0f, 1f)
                    val prevY = height - (prevYFraction * height)

                    val midX = (prevX + x) / 2f
                    txPath.cubicTo(midX, prevY, midX, y, x, y)
                    txFillPath.cubicTo(midX, prevY, midX, y, x, y)
                }
            }
            txFillPath.lineTo((points.size - 1) * stepX, height)
            txFillPath.close()

            drawPath(
                path = txFillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(TrafficTxColor.copy(alpha = 0.18f), TrafficTxColor.copy(alpha = 0.01f)),
                    startY = 0f,
                    endY = height
                )
            )

            drawPath(
                path = txPath,
                color = TrafficTxColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw current endpoint glow circles
            val lastX = (points.size - 1) * stepX
            val lastRxY = height - ((currentRx.toFloat() / chartCeiling).coerceIn(0f, 1f) * height)
            val lastTxY = height - ((currentTx.toFloat() / chartCeiling).coerceIn(0f, 1f) * height)

            drawCircle(color = TrafficRxColor, radius = 5.dp.toPx(), center = Offset(lastX, lastRxY))
            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(lastX, lastRxY))

            drawCircle(color = TrafficTxColor, radius = 5.dp.toPx(), center = Offset(lastX, lastTxY))
            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(lastX, lastTxY))
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Bottom timeline bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Peak: ${Formatters.formatBps(maxVal)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Rolling 60s Live Feed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Interval: 3s",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
