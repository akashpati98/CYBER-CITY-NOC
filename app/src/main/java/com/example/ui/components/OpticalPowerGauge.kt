package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OnuStatus
import com.example.model.OpticalQuality
import com.example.ui.theme.*

@Composable
fun OpticalPowerGauge(
    rxPowerDbm: Float,
    txPowerDbm: Float = 2.2f,
    status: OnuStatus = OnuStatus.ONLINE,
    modifier: Modifier = Modifier
) {
    // Standard GPON range mapping: -8 dBm (left/strong) to -34 dBm (right/weak)
    val minDbm = -8f
    val maxDbm = -34f

    val normalizedFraction = ((rxPowerDbm - minDbm) / (maxDbm - minDbm)).coerceIn(0f, 1f)
    val animatedPos by animateFloatAsState(
        targetValue = normalizedFraction,
        animationSpec = tween(durationMillis = 600),
        label = "opticalMarker"
    )

    val (qualityColor, qualityText) = when {
        status == OnuStatus.OFFLINE || rxPowerDbm <= -32f -> Pair(OpticalLosGray, "LOS / Disconnected")
        rxPowerDbm >= -24.5f -> Pair(OpticalSafeGreen, "Optimal Optical Link")
        rxPowerDbm >= -26.5f -> Pair(OpticalWarningAmber, "Marginal Power (Warning)")
        else -> Pair(OpticalCriticalRed, "Critical Weak Light")
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "OPTICAL RX POWER",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (status == OnuStatus.OFFLINE) "LOS" else String.format("%.2f", rxPowerDbm),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = qualityColor
                    )
                    Text(
                        text = " dBm",
                        style = MaterialTheme.typography.bodyMedium,
                        color = qualityColor,
                        modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                    )
                }
            }

            // Status chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(qualityColor.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(qualityColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = qualityText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = qualityColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Multi-zone color bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
        ) {
            val barWidth = size.width
            val barHeight = size.height

            // Calculate zone boundary positions (mapped from minDbm -8 to maxDbm -34)
            val pos24 = ((-24.5f - minDbm) / (maxDbm - minDbm)) * barWidth
            val pos27 = ((-27.0f - minDbm) / (maxDbm - minDbm)) * barWidth

            // Zone 1: Safe Green (-8 to -24.5)
            drawRoundRect(
                color = OpticalSafeGreen.copy(alpha = 0.85f),
                topLeft = Offset(0f, 4f),
                size = Size(pos24, barHeight - 8f),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            // Zone 2: Warning Amber (-24.5 to -27.0)
            drawRect(
                color = OpticalWarningAmber.copy(alpha = 0.85f),
                topLeft = Offset(pos24, 4f),
                size = Size(pos27 - pos24, barHeight - 8f)
            )
            // Zone 3: Critical Red (-27.0 to -34)
            drawRoundRect(
                color = OpticalCriticalRed.copy(alpha = 0.85f),
                topLeft = Offset(pos27, 4f),
                size = Size(barWidth - pos27, barHeight - 8f),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // Current indicator pin
            val pinX = (animatedPos * barWidth).coerceIn(4f, barWidth - 4f)
            drawCircle(
                color = Color.White,
                radius = 7.dp.toPx(),
                center = Offset(pinX, barHeight / 2f)
            )
            drawCircle(
                color = qualityColor,
                radius = 5.dp.toPx(),
                center = Offset(pinX, barHeight / 2f)
            )
        }

        // Scale legends
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "-8 dBm (Strong)",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Warning: -25.5",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = OpticalWarningAmber
            )
            Text(
                text = "Crit: -27.5",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = OpticalCriticalRed
            )
            Text(
                text = "-34 dBm (LOS)",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Secondary stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "ONT Laser Tx: +${String.format("%.1f", txPowerDbm)} dBm",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Budget Class: GPON B+ (28dB)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
