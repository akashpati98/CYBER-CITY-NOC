package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OnuPowerHistoryPoint
import com.example.ui.theme.*
import kotlin.math.abs

/**
 * Visualizes historical optical Rx power trends for an ONU relative to its
 * Nominal Power (default -20.0 dBm) and the +/- 3 dBm recommended range band (-17 to -23 dBm).
 */
@Composable
fun OnuPowerTrendChart(
    historyPoints: List<OnuPowerHistoryPoint>,
    nominalPowerDbm: Float = -20.0f,
    toleranceDb: Float = 3.0f,
    modifier: Modifier = Modifier
) {
    val upperLimitDbm = nominalPowerDbm + toleranceDb // e.g. -17.0 dBm
    val lowerLimitDbm = nominalPowerDbm - toleranceDb // e.g. -23.0 dBm

    val latestRx = historyPoints.lastOrNull()?.rxPowerDbm ?: nominalPowerDbm
    val minRx = historyPoints.minOfOrNull { it.rxPowerDbm } ?: latestRx
    val maxRx = historyPoints.maxOfOrNull { it.rxPowerDbm } ?: latestRx
    val isOutOfRange = abs(latestRx - nominalPowerDbm) > toleranceDb

    // Y-axis range from -12 dBm (top) down to -36 dBm (bottom)
    val maxAxisDbm = -12.0f
    val minAxisDbm = -36.0f
    val spanDb = maxAxisDbm - minAxisDbm // 24 dB span

    fun dbmToY(dbm: Float, height: Float): Float {
        val clamped = dbm.coerceIn(minAxisDbm, maxAxisDbm)
        val fractionFromTop = (maxAxisDbm - clamped) / spanDb
        return fractionFromTop * height
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("onu_power_trend_chart")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "HISTORICAL OPTICAL POWER TREND",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Nominal ${String.format("%.1f", nominalPowerDbm)} dBm (±${String.format("%.1f", toleranceDb)} dB Safe Band)",
                    fontSize = 10.sp,
                    color = NocTextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isOutOfRange) Color(0xFFFFE4E6) else Color(0xFFECFDF5))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isOutOfRange) "OUTSIDE ±3 dBm" else "WITHIN ±3 dBm",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isOutOfRange) NocCriticalRed else NocSuccessGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            val width = size.width
            val height = size.height

            val topBandY = dbmToY(upperLimitDbm, height)
            val bottomBandY = dbmToY(lowerLimitDbm, height)
            val nominalY = dbmToY(nominalPowerDbm, height)

            // 1. Recommended +/- 3 dBm Green Shaded Band
            drawRect(
                color = NocSuccessGreen.copy(alpha = 0.12f),
                topLeft = Offset(0f, topBandY),
                size = Size(width, (bottomBandY - topBandY).coerceAtLeast(2f))
            )

            // Upper & Lower +/- 3 dBm Threshold Dashed Lines
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            drawLine(
                color = NocWarningAmber.copy(alpha = 0.8f),
                start = Offset(0f, topBandY),
                end = Offset(width, topBandY),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = dashEffect
            )
            drawLine(
                color = NocWarningAmber.copy(alpha = 0.8f),
                start = Offset(0f, bottomBandY),
                end = Offset(width, bottomBandY),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = dashEffect
            )

            // Nominal Center Line (-20.0 dBm)
            drawLine(
                color = NocSuccessGreen.copy(alpha = 0.55f),
                start = Offset(0f, nominalY),
                end = Offset(width, nominalY),
                strokeWidth = 1.dp.toPx()
            )

            if (historyPoints.size < 2) return@Canvas

            val stepX = width / (historyPoints.size - 1).coerceAtLeast(1)
            val trendPath = Path()

            historyPoints.forEachIndexed { idx, pt ->
                val x = idx * stepX
                val y = dbmToY(pt.rxPowerDbm, height)
                if (idx == 0) {
                    trendPath.moveTo(x, y)
                } else {
                    val prevX = (idx - 1) * stepX
                    val prevY = dbmToY(historyPoints[idx - 1].rxPowerDbm, height)
                    val midX = (prevX + x) / 2f
                    trendPath.cubicTo(midX, prevY, midX, y, x, y)
                }
            }

            val lineColor = if (isOutOfRange) NocCriticalRed else NocPrimaryBlue
            drawPath(
                path = trendPath,
                color = lineColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw sample dots
            historyPoints.forEachIndexed { idx, pt ->
                val x = idx * stepX
                val y = dbmToY(pt.rxPowerDbm, height)
                val pointOut = abs(pt.rxPowerDbm - nominalPowerDbm) > toleranceDb
                drawCircle(
                    color = if (pointOut) NocCriticalRed else NocSuccessGreen,
                    radius = 3.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Min: ${String.format("%.1f", minRx)} dBm",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = NocTextSecondary
            )
            Text(
                text = "Safe Range: ${String.format("%.1f", lowerLimitDbm)} to ${String.format("%.1f", upperLimitDbm)} dBm",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = NocSuccessGreen
            )
            Text(
                text = "Max: ${String.format("%.1f", maxRx)} dBm",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = NocTextSecondary
            )
        }
    }
}
