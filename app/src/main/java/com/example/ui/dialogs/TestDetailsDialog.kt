package com.example.ui.dialogs

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.NetworkInfoState
import com.example.model.NetworkQualityGrade
import com.example.model.PingStatistics
import com.example.model.PublicIpInfo
import com.example.model.SpeedState
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RobotoMonoFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TestDetailsDialog(
    speedState: SpeedState,
    pingStats: PingStatistics,
    qualityGrade: NetworkQualityGrade?,
    networkInfo: NetworkInfoState,
    publicIpInfo: PublicIpInfo?,
    serverName: String,
    onDismiss: () -> Unit,
    isEn: Boolean = false
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val dateFormat = if (isEn) {
        SimpleDateFormat("MMMM dd, yyyy, HH:mm", Locale.US)
    } else {
        SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("ru"))
    }
    val timestamp = if (speedState.completedTimestamp > 0) speedState.completedTimestamp else System.currentTimeMillis()
    val dateStr = dateFormat.format(Date(timestamp))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                .padding(20.dp)
                .testTag("test_details_dialog")
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header (Pinned at top of card)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyanAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isEn) "Speed Test Breakdown" else "Детали теста скорости",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content Body (takes all remaining space with weight(1f))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    // Quality Grade Card
                    if (qualityGrade != null) {
                        val gradeColor = Color(qualityGrade.colorHex)
                        val gradeTitle = com.example.ui.i18n.AppI18n.getGradeTitle(qualityGrade.scoreOutOf100, isEn)
                        val gradeSubtitle = com.example.ui.i18n.AppI18n.getGradeSubtitle(qualityGrade.scoreOutOf100, isEn)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(gradeColor.copy(alpha = 0.12f))
                                .border(1.dp, gradeColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isEn) "Result: $gradeTitle" else "Итог: $gradeTitle",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = gradeColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = gradeSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(gradeColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${qualityGrade.scoreOutOf100}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = gradeColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    val msUnit = if (isEn) "ms" else "мс"

                    // DOWNLOAD DETAILS SECTION
                    SpeedBreakdownSection(
                        title = if (isEn) "DOWNLOAD SPEED" else "СКАЧИВАНИЕ (DOWNLOAD)",
                        icon = Icons.Default.ArrowDownward,
                        color = CyanAccent,
                        avgSpeed = speedState.downloadResult.finalAvgMbps.takeIf { it > 0 } ?: speedState.downloadFinalMbps,
                        peakSpeed = speedState.downloadResult.peakMbps.takeIf { it > 0 } ?: speedState.downloadFinalMbps,
                        minSpeed = speedState.downloadResult.minMbps.takeIf { it > 0 } ?: (speedState.downloadFinalMbps * 0.7),
                        isEn = isEn
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // UPLOAD DETAILS SECTION
                    SpeedBreakdownSection(
                        title = if (isEn) "UPLOAD SPEED" else "ОТДАЧА (UPLOAD)",
                        icon = Icons.Default.ArrowUpward,
                        color = EmeraldSuccess,
                        avgSpeed = speedState.uploadResult.finalAvgMbps.takeIf { it > 0 } ?: speedState.uploadFinalMbps,
                        peakSpeed = speedState.uploadResult.peakMbps.takeIf { it > 0 } ?: speedState.uploadFinalMbps,
                        minSpeed = speedState.uploadResult.minMbps.takeIf { it > 0 } ?: (speedState.uploadFinalMbps * 0.7),
                        isEn = isEn
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // LATENCY & STABILITY SECTION
                    Text(
                        text = if (isEn) "LATENCY & STABILITY" else "ЗАДЕРЖКА И СТАБИЛЬНОСТЬ",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailMetricRow(if (isEn) "Current Ping:" else "Текущий пинг:", "${pingStats.currentPingMs} $msUnit")
                        DetailMetricRow(if (isEn) "Average Latency:" else "Средняя задержка:", "${pingStats.avgLatencyMs} $msUnit")
                        DetailMetricRow(if (isEn) "Minimum Latency:" else "Минимальный пинг:", "${pingStats.minLatencyMs} $msUnit")
                        DetailMetricRow(if (isEn) "Maximum Latency:" else "Максимальный пинг:", "${pingStats.maxLatencyMs} $msUnit")
                        DetailMetricRow(if (isEn) "Jitter (variation):" else "Джиттер (вариация):", "${pingStats.jitterMs} $msUnit")
                        DetailMetricRow(if (isEn) "Packet Loss:" else "Потери пакетов:", "${pingStats.packetLossPercent}%")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // NETWORK & SERVER SECTION
                    Text(
                        text = if (isEn) "NETWORK & NODE PARAMETERS" else "ПАРАМЕТРЫ СЕТИ И УЗЛА",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val speedUnit = if (isEn) "Mbps" else "Мбит/с"

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailMetricRow(if (isEn) "Connection Type:" else "Тип подключения:", networkInfo.getDisplayName(isEn))
                        if (networkInfo.linkDownstreamSpeedMbps > 0) {
                            DetailMetricRow(if (isEn) "Physical Link:" else "Канал линка:", "~${networkInfo.linkDownstreamSpeedMbps} $speedUnit")
                        }
                        DetailMetricRow(if (isEn) "Test Server:" else "Тестовый сервер:", serverName)
                        if (publicIpInfo != null) {
                            DetailMetricRow(if (isEn) "Public IP:" else "Внешний IP:", "${publicIpInfo.ip} ${publicIpInfo.flagEmoji}")
                            DetailMetricRow(if (isEn) "Country:" else "Страна:", publicIpInfo.getCountryDisplayName(isEn))
                        }
                    }

                    // Generous bottom spacer inside scrollable view to guarantee clear view of last item
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Pinned Action Buttons: Share & Close (Always visible and reachable at bottom)
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            shareTestResults(
                                context = context,
                                speedState = speedState,
                                pingStats = pingStats,
                                qualityGrade = qualityGrade,
                                serverName = serverName,
                                isEn = isEn
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_test_details_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isEn) "Share" else "Поделиться", style = MaterialTheme.typography.labelLarge)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = MaterialTheme.colorScheme.onPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("close_test_details_btn")
                    ) {
                        Text(if (isEn) "Close" else "Закрыть", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedBreakdownSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    avgSpeed: Double,
    peakSpeed: Double,
    minSpeed: Double,
    isEn: Boolean = false
) {
    val unit = if (isEn) "Mbps" else "Мбит/с"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "${String.format(Locale.US, "%.1f", avgSpeed)} $unit",
                fontFamily = RobotoMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(if (isEn) "Peak speed" else "Максимум (пик)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "${String.format(Locale.US, "%.1f", peakSpeed)} $unit",
                    fontFamily = RobotoMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(if (isEn) "Minimum speed" else "Минимум", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "${String.format(Locale.US, "%.1f", minSpeed)} $unit",
                    fontFamily = RobotoMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DetailMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontFamily = RobotoMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun shareTestResults(
    context: Context,
    speedState: SpeedState,
    pingStats: PingStatistics,
    qualityGrade: NetworkQualityGrade?,
    serverName: String,
    isEn: Boolean = false
) {
    val downloadStr = String.format(Locale.US, "%.1f", speedState.downloadFinalMbps)
    val uploadStr = String.format(Locale.US, "%.1f", speedState.uploadFinalMbps)
    val speedUnit = if (isEn) "Mbps" else "Мбит/с"
    val msUnit = if (isEn) "ms" else "мс"
    val gradeText = if (qualityGrade != null) {
        com.example.ui.i18n.AppI18n.getGradeTitle(qualityGrade.scoreOutOf100, isEn)
    } else {
        if (isEn) "Network test" else "Тест сети"
    }

    val text = if (isEn) {
        """
            📊 NetPulse Speed Test Results:
            • Grade: $gradeText
            • Download: $downloadStr $speedUnit (peak: ${String.format(Locale.US, "%.1f", speedState.downloadResult.peakMbps)})
            • Upload: $uploadStr $speedUnit (peak: ${String.format(Locale.US, "%.1f", speedState.uploadResult.peakMbps)})
            • Ping: ${pingStats.currentPingMs} $msUnit (avg: ${pingStats.avgLatencyMs} $msUnit)
            • Jitter: ${pingStats.jitterMs} $msUnit | Packet Loss: ${pingStats.packetLossPercent}%
            • Server: $serverName
        """.trimIndent()
    } else {
        """
            📊 Результат проверки NetPulse:
            • Оценка: $gradeText
            • Скачивание: $downloadStr $speedUnit (пик: ${String.format(Locale.US, "%.1f", speedState.downloadResult.peakMbps)})
            • Отдача: $uploadStr $speedUnit (пик: ${String.format(Locale.US, "%.1f", speedState.uploadResult.peakMbps)})
            • Пинг: ${pingStats.currentPingMs} $msUnit (ср: ${pingStats.avgLatencyMs} $msUnit)
            • Джиттер: ${pingStats.jitterMs} $msUnit | Потери: ${pingStats.packetLossPercent}%
            • Сервер: $serverName
        """.trimIndent()
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooserTitle = if (isEn) "Share NetPulse results" else "Поделиться результатами NetPulse"
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}
