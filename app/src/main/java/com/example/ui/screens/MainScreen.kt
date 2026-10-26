package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionType
import com.example.model.MetricHelpDirectory
import com.example.model.MetricHelpInfo
import com.example.model.MetricModuleId
import com.example.model.MetricModuleItem
import com.example.model.TestPhase
import com.example.ui.components.HistorySheet
import com.example.ui.components.MetricCard
import com.example.ui.components.PulseWaveformGraph
import com.example.ui.components.SpeedGauge
import com.example.ui.dialogs.HelpExplanationDialog
import com.example.ui.dialogs.SettingsSheet
import com.example.ui.dialogs.TestDetailsDialog
import com.example.ui.i18n.AppI18n
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import com.example.ui.viewmodel.NetworkSpeedViewModel
import java.util.Locale

@Composable
fun MainScreen(
    viewModel: NetworkSpeedViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val networkInfo by viewModel.networkInfo.collectAsState()
    val historyList by viewModel.historyRecords.collectAsState()
    val publicIpInfo by viewModel.publicIpInfo.collectAsState()
    val modules by viewModel.modules.collectAsState()
    val availableTargets by viewModel.availableTargets.collectAsState()

    val isEn = remember { AppI18n.isEn() }

    var isEditMode by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showTestDetailsDialog by remember { mutableStateOf(false) }
    var activeHelpModuleId by remember { mutableStateOf<MetricModuleId?>(null) }

    val scrollState = rememberScrollState()

    // Notification permission launcher for Android 13+
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setNotificationEnabled(isGranted)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header: Logo + Right Action Buttons (Pencil Icon for Edit, History, Settings)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "NetPulse",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (isEditMode) {
                                if (isEn) "Edit Mode" else "Режим настройки"
                            } else if (networkInfo.isConnected) {
                                if (isEn) "Online" else "Сеть активна"
                            } else {
                                if (isEn) "Offline" else "Офлайн"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isEditMode) CyanAccent else if (networkInfo.isConnected) EmeraldSuccess else RoseError,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Actions: Pencil Icon (Edit), History, Settings with increased horizontal spacing
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    // Edit mode toggle with pencil icon (no text as requested)
                    IconButton(
                        onClick = { isEditMode = !isEditMode },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isEditMode) CyanAccent else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                1.dp,
                                if (isEditMode) CyanAccent else MaterialTheme.colorScheme.outline,
                                CircleShape
                            )
                            .testTag("toggle_edit_mode_btn")
                    ) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (isEditMode) {
                                if (isEn) "Finish editing" else "Завершить редактирование"
                            } else {
                                if (isEn) "Edit metrics" else "Редактировать показатели"
                            },
                            tint = if (isEditMode) MaterialTheme.colorScheme.onPrimary else CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // History icon button
                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .testTag("open_history_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = if (isEn) "Test history" else "История проверок",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Settings icon button
                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .testTag("open_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = if (isEn) "Settings" else "Настройки",
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Scrollable Content Body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // When in Edit Mode: Speed Gauge and Pulse Waveform Graph are hidden!
                if (!isEditMode) {
                    // Real-time Pulse Waveform Graph
                    PulseWaveformGraph(
                        samples = uiState.pingStats.historySamples,
                        currentPingMs = uiState.pingStats.currentPingMs,
                        avgLatencyMs = uiState.pingStats.avgLatencyMs,
                        isLive = uiState.isRealtimeMonitoring && !uiState.isDiagnosticRunning,
                        isEn = isEn,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    // Speed Gauge
                    SpeedGauge(
                        currentMbps = uiState.speedState.currentMbps,
                        peakMbps = uiState.speedState.peakMbps,
                        phase = uiState.speedState.phase,
                        progress = uiState.speedState.progressPercent,
                        isEn = isEn,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Clickable Quality Grade Card (opens detailed breakdown)
                    AnimatedVisibility(
                        visible = uiState.qualityGrade != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        uiState.qualityGrade?.let { grade ->
                            QualityGradeBanner(
                                grade = grade,
                                onClick = { showTestDetailsDialog = true },
                                isEn = isEn,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    // Status Text
                    val phaseStatus = AppI18n.getPhaseStatusText(uiState.speedState.phase, isEn)
                    if (phaseStatus.isNotEmpty()) {
                        Text(
                            text = phaseStatus,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    // Edit Mode Header Guidance Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CyanAccent.copy(alpha = 0.1f))
                            .border(1.dp, CyanAccent.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = if (isEn) "Customize screen metrics" else "Настройка карточек на экране",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isEn)
                                    "Tap ✕ to remove metric. Use arrows to reorder. You can restore hidden metrics below."
                                else
                                    "Нажмите ✕ чтобы убрать показатель. Используйте стрелки для перемещения. Снизу можно вернуть скрытые показатели.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // DYNAMIC MODULAR CARDS
                val visibleModules = modules.filter { it.isVisible }

                RenderModularCards(
                    modules = visibleModules,
                    isEditMode = isEditMode,
                    isEn = isEn,
                    uiState = uiState,
                    networkInfo = networkInfo,
                    publicIpInfo = publicIpInfo,
                    onRemove = { id -> viewModel.removeModule(id) },
                    onMove = { id, offset ->
                        val index = modules.indexOfFirst { it.id == id }
                        if (index != -1) viewModel.moveModule(index, offset)
                    },
                    onOpenHelp = { helpId ->
                        activeHelpModuleId = helpId
                    },
                    onResetPacketLoss = { viewModel.resetPacketLoss() }
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Bottom Actions Area:
            // In Normal Mode: Start Speed Test & Toggle Realtime Ping Radar
            // In Edit Mode: Bottom dock to add hidden indicators + Done button
            if (!isEditMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    if (uiState.isDiagnosticRunning) {
                        Button(
                            onClick = { viewModel.cancelDiagnosticTest() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("stop_diagnostic_btn")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEn) "Stop test" else "Остановить тест",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = { viewModel.startDiagnosticTest() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanAccent,
                                contentColor = Color(0xFF031A26)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_diagnostic_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEn) "Start speed test" else "Запустить тест скорости",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { viewModel.toggleRealtimeMonitoring() },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("toggle_realtime_ping_btn")
                    ) {
                        Text(
                            text = if (uiState.isRealtimeMonitoring) {
                                if (isEn) "Live pulse: Active (Tap to pause)" else "Живой пульс: Активен (Нажмите для паузы)"
                            } else {
                                if (isEn) "Live pulse: Resume" else "Живой пульс: Возобновить"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                // EDIT MODE BOTTOM DOCK: "Добавить показатели" + "Готово"
                val hiddenModules = modules.filter { !it.isVisible }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = if (isEn) "ADD METRICS" else "ДОБАВИТЬ ПОКАЗАТЕЛИ",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (hiddenModules.isEmpty()) {
                        Text(
                            text = if (isEn) "All metrics are already added to the screen" else "Все показатели уже добавлены на экран",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        val chipScrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(chipScrollState),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            hiddenModules.forEach { item ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                        .clickable { viewModel.addModule(item.id) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag("add_chip_${item.id.name}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = if (isEn) "Add" else "Добавить",
                                            tint = CyanAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = AppI18n.getMetricTitle(item.id, isEn).replace("\n", " "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { isEditMode = false },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = MaterialTheme.colorScheme.onPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("finish_edit_mode_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEn) "Done" else "Готово",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Test Details Dialog (Result Click Breakdown)
        if (showTestDetailsDialog) {
            TestDetailsDialog(
                speedState = uiState.speedState,
                pingStats = uiState.pingStats,
                qualityGrade = uiState.qualityGrade,
                networkInfo = networkInfo,
                publicIpInfo = publicIpInfo,
                serverName = uiState.selectedTarget.name,
                isEn = isEn,
                onDismiss = { showTestDetailsDialog = false }
            )
        }

        // Help Explanation Dialog (When tapping '?' on any metric, including full IP inspection for GEO_IP)
        activeHelpModuleId?.let { moduleId ->
            val helpInfo = AppI18n.getMetricHelp(moduleId, isEn)
            if (helpInfo != null) {
                HelpExplanationDialog(
                    helpInfo = helpInfo,
                    isEn = isEn,
                    moduleId = moduleId,
                    publicIpInfo = if (moduleId == MetricModuleId.GEO_IP) publicIpInfo else null,
                    localIp = if (moduleId == MetricModuleId.GEO_IP && networkInfo.ipAddress.isNotBlank() && networkInfo.ipAddress != "—") networkInfo.ipAddress else null,
                    onDismiss = { activeHelpModuleId = null }
                )
            }
        }

        // Settings Sheet (Theme selector + Server selector + Notifications)
        if (showSettingsSheet) {
            SettingsSheet(
                currentTarget = uiState.selectedTarget,
                availableTargets = availableTargets,
                onSelectTarget = { viewModel.selectTarget(it) },
                onAddCustomTarget = { name, host, port -> viewModel.addCustomTarget(name, host, port) },
                onDeleteCustomTarget = { viewModel.deleteCustomTarget(it) },
                currentThemeMode = uiState.themeMode,
                onSelectThemeMode = { viewModel.setThemeMode(it) },
                isEn = isEn,
                notifEnabled = uiState.notificationEnabled,
                onToggleNotifEnabled = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.setNotificationEnabled(enabled)
                    }
                },
                showPingInNotif = uiState.notifShowPing,
                onToggleShowPing = { viewModel.setShowPingInNotif(it) },
                showJitterInNotif = uiState.notifShowJitter,
                onToggleShowJitter = { viewModel.setShowJitterInNotif(it) },
                showSpeedInNotif = uiState.notifShowSpeed,
                onToggleShowSpeed = { viewModel.setShowSpeedInNotif(it) },
                showIpInNotif = uiState.notifShowIp,
                onToggleShowIp = { viewModel.setShowIpInNotif(it) },
                onRequestNotificationPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onDismiss = { showSettingsSheet = false }
            )
        }

        // History modal sheet
        if (showHistorySheet) {
            HistorySheet(
                historyList = historyList,
                isEn = isEn,
                onDeleteRecord = { viewModel.deleteHistoryRecord(it) },
                onClearAll = { viewModel.clearAllHistory() },
                onDismiss = { showHistorySheet = false }
            )
        }
    }
}

@Composable
private fun RenderModularCards(
    modules: List<MetricModuleItem>,
    isEditMode: Boolean,
    isEn: Boolean = false,
    uiState: com.example.ui.viewmodel.UiState,
    networkInfo: com.example.model.NetworkInfoState,
    publicIpInfo: com.example.model.PublicIpInfo?,
    onRemove: (MetricModuleId) -> Unit,
    onMove: (MetricModuleId, Int) -> Unit,
    onOpenHelp: (MetricModuleId) -> Unit,
    onResetPacketLoss: () -> Unit
) {
    val pairs = modules.chunked(2)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        pairs.forEachIndexed { rowIndex, rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEachIndexed { itemIndex, moduleItem ->
                    val globalIndex = rowIndex * 2 + itemIndex
                    val canMoveBackward = globalIndex > 0
                    val canMoveForward = globalIndex < modules.size - 1

                    Box(modifier = Modifier.weight(1f)) {
                        RenderSingleModuleCard(
                            id = moduleItem.id,
                            isEditMode = isEditMode,
                            isEn = isEn,
                            uiState = uiState,
                            networkInfo = networkInfo,
                            publicIpInfo = publicIpInfo,
                            onRemoveClick = { onRemove(moduleItem.id) },
                            onMoveBackward = if (canMoveBackward) { { onMove(moduleItem.id, -1) } } else null,
                            onMoveForward = if (canMoveForward) { { onMove(moduleItem.id, 1) } } else null,
                            onHelpClick = if (moduleItem.id.hasHelp) {
                                { onOpenHelp(moduleItem.id) }
                            } else null,
                            onResetPacketLoss = onResetPacketLoss
                        )
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RenderSingleModuleCard(
    id: MetricModuleId,
    isEditMode: Boolean,
    isEn: Boolean = false,
    uiState: com.example.ui.viewmodel.UiState,
    networkInfo: com.example.model.NetworkInfoState,
    publicIpInfo: com.example.model.PublicIpInfo?,
    onRemoveClick: () -> Unit,
    onMoveBackward: (() -> Unit)?,
    onMoveForward: (() -> Unit)?,
    onHelpClick: (() -> Unit)?,
    onResetPacketLoss: (() -> Unit)? = null
) {
    when (id) {
        MetricModuleId.CONNECTION_TYPE -> {
            val netIcon = when (networkInfo.connectionType) {
                ConnectionType.WIFI -> Icons.Default.Wifi
                ConnectionType.CELLULAR -> Icons.Default.CellTower
                ConnectionType.ETHERNET -> Icons.Default.Speed
                ConnectionType.NONE -> Icons.Default.WifiOff
            }
            val statusColor = if (networkInfo.isConnected) EmeraldSuccess else MaterialTheme.colorScheme.error

            MetricCard(
                title = AppI18n.getMetricTitle(MetricModuleId.CONNECTION_TYPE, isEn),
                value = networkInfo.getDisplayName(isEn),
                unit = "",
                icon = netIcon,
                accentColor = statusColor,
                tag = "metric_connection_type",
                subInfo = if (networkInfo.linkDownstreamSpeedMbps > 0) {
                    if (isEn) "Link: ~${networkInfo.linkDownstreamSpeedMbps} Mbps" else "Канал: ~${networkInfo.linkDownstreamSpeedMbps} Мбит/с"
                } else {
                    if (isEn) "Link: —" else "Линк: —"
                },
                isEditMode = isEditMode,
                onRemoveClick = onRemoveClick,
                onMoveBackward = onMoveBackward,
                onMoveForward = onMoveForward,
                onHelpClick = onHelpClick
            )
        }

        MetricModuleId.GEO_IP -> {
            val ipDisplay = publicIpInfo?.ip ?: if (isEn) "Loading…" else "Загрузка…"
            val countryText = if (publicIpInfo != null) {
                "${publicIpInfo.flagEmoji} ${publicIpInfo.getCountryDisplayName(isEn)}"
            } else {
                if (isEn) "Detecting country…" else "Определение страны…"
            }

            MetricCard(
                title = AppI18n.getMetricTitle(MetricModuleId.GEO_IP, isEn),
                value = ipDisplay,
                unit = "",
                icon = Icons.Default.Public,
                accentColor = CyanAccent,
                tag = "metric_geo_ip",
                subInfo = countryText,
                isEditMode = isEditMode,
                onRemoveClick = onRemoveClick,
                onMoveBackward = onMoveBackward,
                onMoveForward = onMoveForward,
                onHelpClick = onHelpClick
            )
        }

        MetricModuleId.CURRENT_PING -> {
            val pingVal = if (uiState.pingStats.currentPingMs > 0) "${uiState.pingStats.currentPingMs}" else "—"
            MetricCard(
                title = AppI18n.getMetricTitle(MetricModuleId.CURRENT_PING, isEn),
                value = pingVal,
                unit = if (isEn) "ms" else "мс",
                icon = Icons.Default.NetworkCheck,
                accentColor = CyanAccent,
                tag = "metric_current_ping",
                subInfo = if (uiState.pingStats.minLatencyMs > 0) {
                    if (isEn) "min ${uiState.pingStats.minLatencyMs}ms" else "мин ${uiState.pingStats.minLatencyMs}мс"
                } else null,
                isEditMode = isEditMode,
                onRemoveClick = onRemoveClick,
                onMoveBackward = onMoveBackward,
                onMoveForward = onMoveForward,
                onHelpClick = onHelpClick
            )
        }

        MetricModuleId.AVG_LATENCY -> {
            val avgVal = if (uiState.pingStats.avgLatencyMs > 0) "${uiState.pingStats.avgLatencyMs}" else "—"
            MetricCard(
                title = AppI18n.getMetricTitle(MetricModuleId.AVG_LATENCY, isEn),
                value = avgVal,
                unit = if (isEn) "ms" else "мс",
                icon = Icons.Default.Timer,
                accentColor = CyanAccent,
                tag = "metric_avg_latency",
                subInfo = if (uiState.pingStats.maxLatencyMs > 0) {
                    if (isEn) "max ${uiState.pingStats.maxLatencyMs}ms" else "макс ${uiState.pingStats.maxLatencyMs}мс"
                } else null,
                isEditMode = isEditMode,
                onRemoveClick = onRemoveClick,
                onMoveBackward = onMoveBackward,
                onMoveForward = onMoveForward,
                onHelpClick = onHelpClick
            )
        }

        MetricModuleId.JITTER -> {
            val jitterVal = if (uiState.pingStats.jitterMs > 0) "${uiState.pingStats.jitterMs}" else "0"
            MetricCard(
                title = AppI18n.getMetricTitle(MetricModuleId.JITTER, isEn),
                value = jitterVal,
                unit = if (isEn) "ms" else "мс",
                icon = Icons.Default.Speed,
                accentColor = EmeraldSuccess,
                tag = "metric_jitter",
                subInfo = if (isEn) "ping variation" else "вариация пинга",
                isEditMode = isEditMode,
                onRemoveClick = onRemoveClick,
                onMoveBackward = onMoveBackward,
                onMoveForward = onMoveForward,
                onHelpClick = onHelpClick
            )
        }

        MetricModuleId.PACKET_LOSS -> {
            MetricCard(
                title = AppI18n.getMetricTitle(MetricModuleId.PACKET_LOSS, isEn),
                value = "${uiState.pingStats.packetLossPercent}",
                unit = "%",
                icon = Icons.Default.Public,
                accentColor = if (uiState.pingStats.packetLossPercent == 0) EmeraldSuccess else RoseError,
                tag = "metric_packet_loss",
                subInfo = if (isEn) "${uiState.pingStats.totalPingsSent} packets" else "${uiState.pingStats.totalPingsSent} пакетов",
                isEditMode = isEditMode,
                onRemoveClick = onRemoveClick,
                onMoveBackward = onMoveBackward,
                onMoveForward = onMoveForward,
                onHelpClick = onHelpClick,
                actionButton = {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CyanAccent.copy(alpha = 0.15f))
                            .border(1.dp, CyanAccent.copy(alpha = 0.35f), CircleShape)
                            .clickable { onResetPacketLoss?.invoke() }
                            .testTag("reset_packet_loss_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = if (isEn) "Reset packet loss" else "Сбросить потери",
                            tint = CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            )
        }

        MetricModuleId.DOWNLOAD -> {
            val downloadVal = if (uiState.speedState.downloadFinalMbps > 0) {
                String.format(Locale.US, "%.1f", uiState.speedState.downloadFinalMbps)
            } else if (uiState.speedState.phase == TestPhase.DOWNLOADING) {
                String.format(Locale.US, "%.1f", uiState.speedState.currentMbps)
            } else "—"

            MetricCard(
                title = AppI18n.getMetricTitle(MetricModuleId.DOWNLOAD, isEn),
                value = downloadVal,
                unit = if (isEn) "Mbps" else "Мбит/с",
                icon = Icons.Default.ArrowDownward,
                accentColor = CyanAccent,
                tag = "metric_download_speed",
                isEditMode = isEditMode,
                onRemoveClick = onRemoveClick,
                onMoveBackward = onMoveBackward,
                onMoveForward = onMoveForward,
                onHelpClick = onHelpClick
            )
        }

        MetricModuleId.UPLOAD -> {
            val uploadVal = if (uiState.speedState.uploadFinalMbps > 0) {
                String.format(Locale.US, "%.1f", uiState.speedState.uploadFinalMbps)
            } else if (uiState.speedState.phase == TestPhase.UPLOADING) {
                String.format(Locale.US, "%.1f", uiState.speedState.currentMbps)
            } else "—"

            MetricCard(
                title = AppI18n.getMetricTitle(MetricModuleId.UPLOAD, isEn),
                value = uploadVal,
                unit = if (isEn) "Mbps" else "Мбит/с",
                icon = Icons.Default.ArrowUpward,
                accentColor = EmeraldSuccess,
                tag = "metric_upload_speed",
                isEditMode = isEditMode,
                onRemoveClick = onRemoveClick,
                onMoveBackward = onMoveBackward,
                onMoveForward = onMoveForward,
                onHelpClick = onHelpClick
            )
        }
    }
}

@Composable
private fun QualityGradeBanner(
    grade: com.example.model.NetworkQualityGrade,
    onClick: () -> Unit,
    isEn: Boolean = false,
    modifier: Modifier = Modifier
) {
    val gradeColor = Color(grade.colorHex)
    val title = AppI18n.getGradeTitle(grade.scoreOutOf100, isEn)
    val subtitle = AppI18n.getGradeSubtitle(grade.scoreOutOf100, isEn)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(gradeColor.copy(alpha = 0.12f))
            .border(1.dp, gradeColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
            .testTag("network_quality_grade_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEn) "Network Grade: $title" else "Оценка сети: $title",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = gradeColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isEn) "Tap for detailed report" else "Нажмите для подробного отчёта",
                        style = MaterialTheme.typography.labelSmall,
                        color = gradeColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = gradeColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(gradeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${grade.scoreOutOf100}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = gradeColor
                )
            }
        }
    }
}
