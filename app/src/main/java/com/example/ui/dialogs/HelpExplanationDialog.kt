package com.example.ui.dialogs

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MetricHelpInfo
import com.example.model.MetricModuleId
import com.example.model.PublicIpInfo
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RobotoMonoFontFamily
import kotlinx.coroutines.delay

@Composable
fun HelpExplanationDialog(
    helpInfo: MetricHelpInfo,
    onDismiss: () -> Unit,
    isEn: Boolean = false,
    moduleId: MetricModuleId? = null,
    publicIpInfo: PublicIpInfo? = null,
    localIp: String? = null
) {
    val scrollState = rememberScrollState()
    val isGeoIpModule = moduleId == MetricModuleId.GEO_IP || publicIpInfo != null

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("help_explanation_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isGeoIpModule) Icons.Default.Public else Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = helpInfo.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // If this is the Public IP module, prominently display the full IP (essential for long IPv6)
                if (isGeoIpModule) {
                    val rawIp = publicIpInfo?.ip?.trim().orEmpty()
                    val hasIp = rawIp.isNotEmpty() && rawIp != "—"
                    val isIpv6 = rawIp.contains(":")
                    val isIpv4 = rawIp.contains(".")
                    val clipboardManager = LocalClipboardManager.current
                    var isCopied by remember { mutableStateOf(false) }

                    LaunchedEffect(isCopied) {
                        if (isCopied) {
                            delay(2200)
                            isCopied = false
                        }
                    }

                    val buttonBgColor by animateColorAsState(
                        targetValue = if (isCopied) EmeraldSuccess.copy(alpha = 0.22f) else CyanAccent.copy(alpha = 0.15f),
                        label = "copyButtonBg"
                    )
                    val buttonContentColor by animateColorAsState(
                        targetValue = if (isCopied) EmeraldSuccess else CyanAccent,
                        label = "copyButtonContent"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, CyanAccent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                            .testTag("ip_inspection_card")
                    ) {
                        // Header with IP Protocol Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEn) "YOUR PUBLIC IP ADDRESS" else "ВАШ ПУБЛИЧНЫЙ IP-АДРЕС",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            if (hasIp) {
                                val protocolLabel = if (isIpv6) "IPv6" else if (isIpv4) "IPv4" else "IP"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CyanAccent.copy(alpha = 0.18f))
                                        .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = protocolLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Full IP address with monospaced font, full selection, and complete multi-line wrapping
                        val displayIp = if (hasIp) rawIp else if (isEn) "Detecting IP address…" else "Определение IP-адреса…"
                        SelectionContainer {
                            Text(
                                text = displayIp,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = RobotoMonoFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = if (isIpv6) 15.sp else 18.sp,
                                    lineHeight = if (isIpv6) 20.sp else 24.sp
                                ),
                                color = if (hasIp) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dialog_full_ip_text")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Copy full IP action button
                        FilledTonalButton(
                            onClick = {
                                if (hasIp) {
                                    clipboardManager.setText(AnnotatedString(rawIp))
                                    isCopied = true
                                }
                            },
                            enabled = hasIp,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = buttonBgColor,
                                contentColor = buttonContentColor,
                                disabledContainerColor = MaterialTheme.colorScheme.surface,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("dialog_copy_ip_btn")
                        ) {
                            Icon(
                                imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCopied) {
                                    if (isEn) "Copied to clipboard!" else "Скопировано в буфер!"
                                } else {
                                    if (isEn) "Copy full IP" else "Скопировать полный IP"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Additional Geo / ISP / Local IP metadata if available
                        if (publicIpInfo != null) {
                            val countryDisplay = publicIpInfo.getCountryDisplayName(isEn)
                            if (countryDisplay.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${publicIpInfo.flagEmoji} $countryDisplay",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (publicIpInfo.isp.isNotBlank()) {
                                        Text(
                                            text = publicIpInfo.isp,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        if (!localIp.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${if (isEn) "Local IP: " else "Локальный IP: "}$localIp",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // General Description
                Text(
                    text = helpInfo.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Impact block
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (isEn) "NETWORK IMPACT" else "НА ЧТО ВЛИЯЕТ",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = helpInfo.impactText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Good range block
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldSuccess.copy(alpha = 0.12f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (isEn) "BENCHMARKS & RANGES" else "НОРМА И ОЦЕНКА",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldSuccess,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = helpInfo.goodRange,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_help_dialog_btn")
            ) {
                Text(
                    text = if (isEn) "Got it" else "Понятно",
                    color = CyanAccent,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
