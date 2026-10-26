package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.db.TestHistoryDatabase
import com.example.data.db.TestRecordEntity
import com.example.data.network.NetworkMonitor
import com.example.data.network.PingService
import com.example.data.network.PublicIpService
import com.example.data.network.SpeedTestService
import com.example.data.preferences.AppLanguage
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.ThemeMode
import com.example.model.MetricModuleId
import com.example.model.MetricModuleItem
import com.example.model.NetworkInfoState
import com.example.model.NetworkQualityGrade
import com.example.model.PingStatistics
import com.example.model.PingTarget
import com.example.model.PublicIpInfo
import com.example.model.SpeedMeasurementResult
import com.example.model.SpeedState
import com.example.model.TestPhase
import com.example.service.LiveNotificationManager
import com.example.service.NetPulseLiveService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class UiState(
    val selectedTarget: PingTarget = PingTarget.CLOUDFLARE,
    val isRealtimeMonitoring: Boolean = true,
    val isDiagnosticRunning: Boolean = false,
    val speedState: SpeedState = SpeedState(),
    val pingStats: PingStatistics = PingStatistics(),
    val qualityGrade: NetworkQualityGrade? = null,
    val errorMessage: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val notificationEnabled: Boolean = false,
    val notifShowPing: Boolean = true,
    val notifShowJitter: Boolean = true,
    val notifShowSpeed: Boolean = true,
    val notifShowIp: Boolean = true
)

class NetworkSpeedViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        TestHistoryDatabase::class.java,
        "netpulse_history.db"
    ).fallbackToDestructiveMigration().build()

    private val testRecordDao = db.testRecordDao()
    private val networkMonitor = NetworkMonitor(application)
    private val pingService = PingService()
    private val speedTestService = SpeedTestService()
    private val publicIpService = PublicIpService()
    private val prefs = AppPreferences(application)
    private val liveNotificationManager = LiveNotificationManager(application)

    val networkInfo: StateFlow<NetworkInfoState> = networkMonitor.observeNetworkState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NetworkInfoState()
        )

    val historyRecords: StateFlow<List<TestRecordEntity>> = testRecordDao.getAllRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _publicIpInfo = MutableStateFlow<PublicIpInfo?>(null)
    val publicIpInfo: StateFlow<PublicIpInfo?> = _publicIpInfo.asStateFlow()

    private val _modules = MutableStateFlow<List<MetricModuleItem>>(prefs.getModules())
    val modules: StateFlow<List<MetricModuleItem>> = _modules.asStateFlow()

    private val _availableTargets = MutableStateFlow<List<PingTarget>>(
        PingTarget.DEFAULT_TARGETS + prefs.getCustomServers()
    )
    val availableTargets: StateFlow<List<PingTarget>> = _availableTargets.asStateFlow()

    private val initialTarget = (PingTarget.DEFAULT_TARGETS + prefs.getCustomServers())
        .find { it.id == prefs.selectedServerId } ?: PingTarget.CLOUDFLARE

    private val _uiState = MutableStateFlow(
        UiState(
            selectedTarget = initialTarget,
            themeMode = prefs.themeMode,
            appLanguage = prefs.appLanguage,
            notificationEnabled = prefs.notificationEnabled,
            notifShowPing = prefs.notifShowPing,
            notifShowJitter = prefs.notifShowJitter,
            notifShowSpeed = prefs.notifShowSpeed,
            notifShowIp = prefs.notifShowIp
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var monitorJob: Job? = null
    private var diagnosticJob: Job? = null

    init {
        startRealtimeMonitoring()
        refreshPublicIp()

        if (prefs.notificationEnabled) {
            NetPulseLiveService.start(application)
        }

        viewModelScope.launch {
            networkInfo.collect { net ->
                if (net.isConnected) {
                    refreshPublicIp()
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.themeMode = mode
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setAppLanguage(lang: AppLanguage) {
        prefs.appLanguage = lang
        _uiState.update { it.copy(appLanguage = lang) }
        updateLiveNotification()
    }

    fun refreshPublicIp() {
        viewModelScope.launch {
            val ipInfo = publicIpService.getPublicIpInfo(forceRefresh = true)
            _publicIpInfo.value = ipInfo
            updateLiveNotification()
        }
    }

    // --- Module Customization & Editing ---
    fun removeModule(id: MetricModuleId) {
        val current = _modules.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            current[index] = current[index].copy(isVisible = false)
            _modules.value = current
            prefs.saveModules(current)
        }
    }

    fun addModule(id: MetricModuleId) {
        val current = _modules.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            current[index] = current[index].copy(isVisible = true)
        } else {
            current.add(MetricModuleItem(id, isVisible = true))
        }
        _modules.value = current
        prefs.saveModules(current)
    }

    fun moveModule(index: Int, offset: Int) {
        val newIndex = index + offset
        if (index !in _modules.value.indices || newIndex !in _modules.value.indices) return
        val current = _modules.value.toMutableList()
        val item = current.removeAt(index)
        current.add(newIndex, item)
        _modules.value = current
        prefs.saveModules(current)
    }

    fun resetDefaultModules() {
        val defaults = prefs.defaultModules()
        _modules.value = defaults
        prefs.saveModules(defaults)
    }

    fun resetPacketLoss() {
        val updated = pingService.resetPacketLoss()
        _uiState.update { it.copy(pingStats = updated) }
    }

    // --- Settings & Server ---
    fun selectTarget(target: PingTarget) {
        prefs.selectedServerId = target.id
        _uiState.update { it.copy(selectedTarget = target) }
        pingService.reset()
        _uiState.update { it.copy(pingStats = PingStatistics()) }
    }

    fun addCustomTarget(name: String, host: String, port: Int) {
        val cleanHost = host.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore('/')
            .trim()
        if (cleanHost.isEmpty()) return
        val validPort = if (port in 1..65535) port else 53
        val cleanName = name.trim().ifEmpty { cleanHost }
        val newTarget = PingTarget(
            id = "custom_" + System.currentTimeMillis(),
            name = cleanName,
            host = cleanHost,
            port = validPort,
            httpFallbackUrl = "https://$cleanHost",
            isCustom = true
        )
        val currentCustom = prefs.getCustomServers().toMutableList()
        currentCustom.add(newTarget)
        prefs.saveCustomServers(currentCustom)
        val all = PingTarget.DEFAULT_TARGETS + currentCustom
        _availableTargets.value = all
        selectTarget(newTarget)
    }

    fun deleteCustomTarget(target: PingTarget) {
        if (!target.isCustom) return
        val currentCustom = prefs.getCustomServers().toMutableList()
        currentCustom.removeAll { it.id == target.id }
        prefs.saveCustomServers(currentCustom)
        val all = PingTarget.DEFAULT_TARGETS + currentCustom
        _availableTargets.value = all
        if (_uiState.value.selectedTarget.id == target.id) {
            selectTarget(PingTarget.CLOUDFLARE)
        }
    }

    // --- Notifications Setting ---
    fun setNotificationEnabled(enabled: Boolean) {
        prefs.notificationEnabled = enabled
        _uiState.update { it.copy(notificationEnabled = enabled) }
        if (!enabled) {
            liveNotificationManager.cancelNotification()
            NetPulseLiveService.stop(getApplication())
        } else {
            NetPulseLiveService.start(getApplication())
            updateLiveNotification()
        }
    }

    fun setShowPingInNotif(show: Boolean) {
        prefs.notifShowPing = show
        _uiState.update { it.copy(notifShowPing = show) }
        updateLiveNotification()
    }

    fun setShowJitterInNotif(show: Boolean) {
        prefs.notifShowJitter = show
        _uiState.update { it.copy(notifShowJitter = show) }
        updateLiveNotification()
    }

    fun setShowSpeedInNotif(show: Boolean) {
        prefs.notifShowSpeed = show
        _uiState.update { it.copy(notifShowSpeed = show) }
        updateLiveNotification()
    }

    fun setShowIpInNotif(show: Boolean) {
        prefs.notifShowIp = show
        _uiState.update { it.copy(notifShowIp = show) }
        updateLiveNotification()
    }

    private fun updateLiveNotification() {
        if (_uiState.value.notificationEnabled) {
            liveNotificationManager.updateNotification(
                pingStats = _uiState.value.pingStats,
                networkInfo = networkInfo.value,
                speedState = _uiState.value.speedState,
                publicIp = _publicIpInfo.value
            )
        }
    }

    // --- Realtime Ping & Latency ---
    fun toggleRealtimeMonitoring() {
        if (_uiState.value.isRealtimeMonitoring) {
            stopRealtimeMonitoring()
        } else {
            startRealtimeMonitoring()
        }
    }

    private fun startRealtimeMonitoring() {
        monitorJob?.cancel()
        _uiState.update { it.copy(isRealtimeMonitoring = true) }

        monitorJob = viewModelScope.launch {
            while (isActive && _uiState.value.isRealtimeMonitoring) {
                if (!_uiState.value.isDiagnosticRunning) {
                    val target = _uiState.value.selectedTarget
                    val latency = pingService.measureSinglePing(target)
                    val updatedStats = pingService.recordPing(latency, maxHistoryWindow = 35)
                    _uiState.update { it.copy(pingStats = updatedStats) }
                    updateLiveNotification()
                }
                delay(850)
            }
        }
    }

    private fun stopRealtimeMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        _uiState.update { it.copy(isRealtimeMonitoring = false) }
    }

    // --- Full Diagnostic Speed Test ---
    fun startDiagnosticTest() {
        if (_uiState.value.isDiagnosticRunning) return

        diagnosticJob?.cancel()
        diagnosticJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDiagnosticRunning = true,
                    qualityGrade = null,
                    errorMessage = null,
                    speedState = SpeedState(
                        phase = TestPhase.PINGING,
                        statusText = "Проверка задержки и джиттера…"
                    )
                )
            }

            // Phase 1: Ping & Jitter measurement
            val diagnosticPingService = PingService()
            val target = _uiState.value.selectedTarget
            for (i in 1..6) {
                if (!isActive) return@launch
                val latency = diagnosticPingService.measureSinglePing(target)
                val stats = diagnosticPingService.recordPing(latency)
                _uiState.update {
                    it.copy(
                        pingStats = stats,
                        speedState = it.speedState.copy(
                            progressPercent = (i.toFloat() / 6f) * 0.2f
                        )
                    )
                }
                updateLiveNotification()
                delay(200)
            }

            val finalPingStats = _uiState.value.pingStats

            // Phase 2: Download Speed
            _uiState.update {
                it.copy(
                    speedState = it.speedState.copy(
                        phase = TestPhase.DOWNLOADING,
                        statusText = "Тестирование скорости загрузки…"
                    )
                )
            }

            val downloadResult = speedTestService.runDownloadTest { currentMbps, peakMbps, progress ->
                _uiState.update {
                    it.copy(
                        speedState = it.speedState.copy(
                            currentMbps = currentMbps,
                            peakMbps = peakMbps,
                            progressPercent = 0.2f + (progress * 0.45f)
                        )
                    )
                }
                updateLiveNotification()
            }

            // Phase 3: Upload Speed
            _uiState.update {
                it.copy(
                    speedState = it.speedState.copy(
                        phase = TestPhase.UPLOADING,
                        downloadResult = downloadResult,
                        downloadFinalMbps = downloadResult.finalAvgMbps,
                        currentMbps = 0.0,
                        statusText = "Тестирование скорости отдачи…"
                    )
                )
            }

            val uploadResult = speedTestService.runUploadTest { currentMbps, peakMbps, progress ->
                _uiState.update {
                    it.copy(
                        speedState = it.speedState.copy(
                            currentMbps = currentMbps,
                            peakMbps = peakMbps,
                            progressPercent = 0.65f + (progress * 0.35f)
                        )
                    )
                }
                updateLiveNotification()
            }

            // Phase 4: Completed
            val grade = calculateQualityGrade(
                downloadMbps = downloadResult.finalAvgMbps,
                uploadMbps = uploadResult.finalAvgMbps,
                pingMs = finalPingStats.currentPingMs,
                jitterMs = finalPingStats.jitterMs
            )

            val now = System.currentTimeMillis()

            _uiState.update {
                it.copy(
                    isDiagnosticRunning = false,
                    qualityGrade = grade,
                    speedState = it.speedState.copy(
                        phase = TestPhase.COMPLETED,
                        downloadResult = downloadResult,
                        uploadResult = uploadResult,
                        downloadFinalMbps = downloadResult.finalAvgMbps,
                        uploadFinalMbps = uploadResult.finalAvgMbps,
                        currentMbps = downloadResult.finalAvgMbps,
                        progressPercent = 1f,
                        statusText = "Тестирование завершено",
                        completedTimestamp = now
                    )
                )
            }
            updateLiveNotification()

            val record = TestRecordEntity(
                downloadMbps = downloadResult.finalAvgMbps,
                uploadMbps = uploadResult.finalAvgMbps,
                pingMs = finalPingStats.currentPingMs,
                avgLatencyMs = finalPingStats.avgLatencyMs,
                jitterMs = finalPingStats.jitterMs,
                connectionType = networkInfo.value.networkName,
                ratingTitle = grade.title,
                serverName = target.name
            )
            testRecordDao.insertRecord(record)
        }
    }

    fun cancelDiagnosticTest() {
        diagnosticJob?.cancel()
        diagnosticJob = null
        _uiState.update {
            it.copy(
                isDiagnosticRunning = false,
                speedState = SpeedState(phase = TestPhase.IDLE)
            )
        }
        updateLiveNotification()
    }

    fun deleteHistoryRecord(record: TestRecordEntity) {
        viewModelScope.launch {
            testRecordDao.deleteRecord(record)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            testRecordDao.clearAll()
        }
    }

    private fun calculateQualityGrade(
        downloadMbps: Double,
        uploadMbps: Double,
        pingMs: Long,
        jitterMs: Long
    ): NetworkQualityGrade {
        return when {
            downloadMbps >= 80.0 && pingMs in 1..35 && jitterMs <= 6 -> {
                NetworkQualityGrade(
                    title = "Превосходно",
                    subtitle = "Идеально для 4K Ultra HD стриминга и киберспорта",
                    scoreOutOf100 = 98,
                    colorHex = 0xFF10B981
                )
            }
            downloadMbps >= 30.0 && pingMs in 1..70 && jitterMs <= 15 -> {
                NetworkQualityGrade(
                    title = "Отлично",
                    subtitle = "Комфортный просмотр HD видео и стабильная видеосвязь",
                    scoreOutOf100 = 85,
                    colorHex = 0xFF00E5FF
                )
            }
            downloadMbps >= 10.0 && pingMs in 1..120 -> {
                NetworkQualityGrade(
                    title = "Хорошо",
                    subtitle = "Подходит для веб-серфинга, соцсетей и музыки",
                    scoreOutOf100 = 70,
                    colorHex = 0xFFF59E0B
                )
            }
            downloadMbps > 0.0 -> {
                NetworkQualityGrade(
                    title = "Базовое",
                    subtitle = "Возможны задержки при загрузке медиа и звонках",
                    scoreOutOf100 = 50,
                    colorHex = 0xFFF43F5E
                )
            }
            else -> {
                NetworkQualityGrade(
                    title = "Нестабильно",
                    subtitle = "Проверьте сигнал сети или обратитесь к провайдеру",
                    scoreOutOf100 = 20,
                    colorHex = 0xFF64748B
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        monitorJob?.cancel()
        diagnosticJob?.cancel()
        liveNotificationManager.cancelNotification()
    }
}
