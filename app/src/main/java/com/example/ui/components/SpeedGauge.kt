package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TestPhase
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RobotoMonoFontFamily
import java.util.Locale

@Composable
fun SpeedGauge(
    currentMbps: Double,
    peakMbps: Double,
    phase: TestPhase,
    progress: Float,
    modifier: Modifier = Modifier,
    isEn: Boolean = false
) {
    // Animate speed smoothly
    val animatedMbps by animateFloatAsState(
        targetValue = currentMbps.toFloat(),
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "animated_mbps"
    )

    // Sweep angle calculation for speed arc (240 degree arc)
    // Assume 0 to 150 Mbps scale with logarithmic/smooth scaling
    val normalizedProgress = when {
        phase == TestPhase.IDLE -> 0f
        phase == TestPhase.PINGING -> progress.coerceIn(0f, 1f)
        else -> {
            val maxScale = 150f
            (animatedMbps / maxScale).coerceIn(0.02f, 1f)
        }
    }

    val activeColor = when (phase) {
        TestPhase.DOWNLOADING -> CyanAccent
        TestPhase.UPLOADING -> EmeraldSuccess
        TestPhase.PINGING -> CyanGlow
        TestPhase.COMPLETED -> EmeraldSuccess
        TestPhase.IDLE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    }

    Box(
        modifier = modifier
            .size(240.dp)
            .testTag("speed_gauge_display"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val diameter = size.minDimension - strokeWidth * 2
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)

            val startAngle = 150f
            val sweepTotalAngle = 240f

            // Background inactive arc
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = startAngle,
                sweepAngle = sweepTotalAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active neon gradient arc
            val activeSweep = sweepTotalAngle * normalizedProgress
            if (activeSweep > 1f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            activeColor.copy(alpha = 0.4f),
                            activeColor
                        )
                    ),
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            val phaseLabel = com.example.ui.i18n.AppI18n.getPhaseLabel(phase, isEn)

            Text(
                text = phaseLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.1.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            val displaySpeedFormatted = if (animatedMbps >= 100) {
                String.format(Locale.US, "%.0f", animatedMbps)
            } else if (animatedMbps >= 10) {
                String.format(Locale.US, "%.1f", animatedMbps)
            } else {
                String.format(Locale.US, "%.2f", animatedMbps)
            }

            Text(
                text = displaySpeedFormatted,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 42.sp,
                    lineHeight = 46.sp
                ),
                fontFamily = RobotoMonoFontFamily,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Text(
                text = if (isEn) "Mbps" else "Мбит / с",
                style = MaterialTheme.typography.labelLarge,
                color = activeColor,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            if (peakMbps > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                val peakText = if (isEn) {
                    "Peak: ${String.format(Locale.US, "%.1f", peakMbps)} Mbps"
                } else {
                    "Пик: ${String.format(Locale.US, "%.1f", peakMbps)} Мбит/с"
                }
                Text(
                    text = peakText,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = RobotoMonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
