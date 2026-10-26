package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RobotoMonoFontFamily
import kotlin.math.max

@Composable
fun PulseWaveformGraph(
    samples: List<Float>,
    currentPingMs: Long,
    avgLatencyMs: Long,
    isLive: Boolean,
    modifier: Modifier = Modifier,
    isEn: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_radar")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .padding(16.dp)
            .testTag("pulse_waveform_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isLive) EmeraldSuccess.copy(alpha = pulseAlpha)
                            else Color.Gray
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                val titleText = if (isLive) {
                    if (isEn) "NETWORK PULSE (LIVE)" else "ПУЛЬС СЕТИ (LIVE)"
                } else {
                    if (isEn) "NETWORK PULSE (PAUSED)" else "ПУЛЬС СЕТИ (ПАУЗА)"
                }
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isEn) "Avg: " else "Средн: ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${avgLatencyMs}${if (isEn) "ms" else "мс"}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = RobotoMonoFontFamily,
                    color = CyanAccent,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 12.dp)) {
                val width = size.width
                val height = size.height

                // Draw horizontal guide lines (grid)
                val lineCount = 3
                for (i in 1..lineCount) {
                    val y = height * (i.toFloat() / (lineCount + 1))
                    drawLine(
                        color = Color.White.copy(alpha = 0.07f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                if (samples.isEmpty()) {
                    // Empty state baseline line
                    drawLine(
                        color = CyanAccent.copy(alpha = 0.3f),
                        start = Offset(0f, height * 0.7f),
                        end = Offset(width, height * 0.7f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    return@Canvas
                }

                val maxSample = max(samples.maxOrNull() ?: 100f, 60f) * 1.25f
                val minSample = 0f
                val range = max(maxSample - minSample, 1f)

                val points = samples.mapIndexed { index, value ->
                    val x = if (samples.size > 1) {
                        index * (width / (samples.size - 1))
                    } else {
                        width / 2f
                    }
                    val normalizedY = ((value - minSample) / range).coerceIn(0f, 1f)
                    val y = height - (normalizedY * (height - 10f)) - 5f
                    Offset(x, y)
                }

                // Fill gradient under curve
                val fillPath = Path().apply {
                    moveTo(points.first().x, height)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(points.last().x, height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CyanAccent.copy(alpha = 0.28f),
                            CyanAccent.copy(alpha = 0.02f)
                        )
                    )
                )

                // Stroke curve line
                val strokePath = Path().apply {
                    points.forEachIndexed { i, pt ->
                        if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                    }
                }

                drawPath(
                    path = strokePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            CyanGlow.copy(alpha = 0.5f),
                            CyanAccent
                        )
                    ),
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                // Current leading edge glowing dot
                val lastPoint = points.last()
                drawCircle(
                    color = CyanAccent.copy(alpha = 0.35f * pulseAlpha),
                    radius = 9.dp.toPx(),
                    center = lastPoint
                )
                drawCircle(
                    color = CyanAccent,
                    radius = 4.dp.toPx(),
                    center = lastPoint
                )
            }

            // Real-time ping readout in top right of waveform
            if (currentPingMs > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$currentPingMs ms",
                        fontFamily = RobotoMonoFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            currentPingMs <= 40 -> EmeraldSuccess
                            currentPingMs <= 90 -> CyanAccent
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }
    }
}
