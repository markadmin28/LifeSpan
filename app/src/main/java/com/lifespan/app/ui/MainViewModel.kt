package com.lifespan.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifespan.app.LifeSpanApp
import com.lifespan.app.data.db.ChargeSessionEntity
import com.lifespan.app.data.db.TelemetryLogEntity
import com.lifespan.app.data.prefs.Accent
import com.lifespan.app.data.prefs.AppSettings
import com.lifespan.app.data.prefs.ThemeMode
import com.lifespan.app.domain.alert.AlertEvaluator
import com.lifespan.app.domain.alert.AlertThresholds
import com.lifespan.app.domain.alert.AlertType
import com.lifespan.app.domain.health.BatteryHealth
import com.lifespan.app.domain.health.BatteryHealthCalculator
import com.lifespan.app.domain.history.ChartPoint
import com.lifespan.app.domain.history.ChartSeries
import com.lifespan.app.domain.history.ChartSeriesBuilder
import com.lifespan.app.domain.model.BatterySnapshot
import com.lifespan.app.domain.usage.AppUsage
import com.lifespan.app.domain.usage.UsageRanker
import com.lifespan.app.service.BatteryMonitorService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val snapshot: BatterySnapshot? = null,
    val thresholds: AlertThresholds = AlertThresholds(),
    val monitoring: Boolean = false,
    val sessions: List<ChargeSessionEntity> = emptyList(),
    val activeAlerts: Set<AlertType> = emptySet(),
    val bubbleEnabled: Boolean = true,
)

data class HistoryUiState(
    val levelSeries: ChartSeries? = null,
    val tempSeries: ChartSeries? = null,
    val sampleCount: Int = 0,
) {
    val hasData: Boolean get() = levelSeries != null || tempSeries != null
}

data class UsageUiState(
    val hasAccess: Boolean = false,
    val loading: Boolean = false,
    val apps: List<AppUsage> = emptyList(),
) {
    val highCount: Int get() = UsageRanker.highCount(apps)
    val topApp: AppUsage? get() = apps.firstOrNull()
    fun isHigh(app: AppUsage): Boolean = UsageRanker.isHigh(app)
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as LifeSpanApp).container
    private val batteryRepository = container.batteryRepository
    private val settingsRepository = container.settingsRepository
    private val appUsageRepository = container.appUsageRepository

    val uiState: StateFlow<MainUiState> = combine(
        batteryRepository.latest,
        settingsRepository.thresholds,
        batteryRepository.monitoring,
        batteryRepository.observeRecentSessions(limit = 25),
        settingsRepository.bubbleEnabled,
    ) { snapshot, thresholds, monitoring, sessions, bubbleEnabled ->
        MainUiState(
            snapshot = snapshot,
            thresholds = thresholds,
            monitoring = monitoring,
            sessions = sessions,
            activeAlerts = snapshot?.let { AlertEvaluator.evaluate(it, thresholds) } ?: emptySet(),
            bubbleEnabled = bubbleEnabled,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    val health: StateFlow<BatteryHealth> = batteryRepository.observeRecentSessions(limit = 200)
        .map { BatteryHealthCalculator.compute(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BatteryHealth.EMPTY,
        )

    /** Rolling telemetry window rendered as the dashboard history chart. */
    val history: StateFlow<HistoryUiState> = batteryRepository
        .observeRecentTelemetry(limit = HISTORY_FETCH_LIMIT)
        .map { logs ->
            val levels = ChartSeriesBuilder.windowByLatest(
                logs.map { ChartPoint(it.timestamp, it.batteryLevel.toDouble()) },
                HISTORY_WINDOW_MILLIS,
            )
            val temps = ChartSeriesBuilder.windowByLatest(
                logs.map { ChartPoint(it.timestamp, it.temperatureCelsius) },
                HISTORY_WINDOW_MILLIS,
            )
            HistoryUiState(
                levelSeries = ChartSeriesBuilder.build(levels, minValueSpan = 4.0),
                tempSeries = ChartSeriesBuilder.build(temps, minValueSpan = 2.0),
                sampleCount = levels.size,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(),
        )

    /** Erase all recorded charge sessions and telemetry. */
    fun clearHistory() = viewModelScope.launch {
        batteryRepository.clearHistory()
    }

    private val _selectedSessionId = MutableStateFlow<Long?>(null)
    val selectedSessionId: StateFlow<Long?> = _selectedSessionId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionLogs: StateFlow<List<TelemetryLogEntity>> = _selectedSessionId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else batteryRepository.observeSessionLogs(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun openSession(id: Long) {
        _selectedSessionId.value = id
    }

    fun closeSession() {
        _selectedSessionId.value = null
    }

    fun startMonitoring() = BatteryMonitorService.start(getApplication())

    fun stopMonitoring() = BatteryMonitorService.stop(getApplication())

    fun setChargeLimit(percent: Int) = viewModelScope.launch {
        settingsRepository.setChargeLimit(percent)
    }

    fun setOverheatCelsius(celsius: Double) = viewModelScope.launch {
        settingsRepository.setOverheatCelsius(celsius)
    }

    fun setOverheatEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setOverheatEnabled(enabled)
    }

    fun setChargeLimitEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setChargeLimitEnabled(enabled)
    }

    fun setBubbleEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setBubbleEnabled(enabled)
    }

    val settings: StateFlow<AppSettings> = settingsRepository.appSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    fun setOnboardingComplete(complete: Boolean) = viewModelScope.launch {
        settingsRepository.setOnboardingComplete(complete)
    }

    fun setAutoStartEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setAutoStartEnabled(enabled)
    }

    fun setPeriodicSamplingEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setPeriodicSamplingEnabled(enabled)
    }

    fun setPersistentChargeAlarm(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setPersistentChargeAlarm(enabled)
    }

    fun setRapidRiseEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setRapidRiseEnabled(enabled)
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        settingsRepository.setThemeMode(mode)
    }

    fun setAccent(accent: Accent) = viewModelScope.launch {
        settingsRepository.setAccent(accent)
    }

    private val _usage = MutableStateFlow(UsageUiState())
    val usage: StateFlow<UsageUiState> = _usage.asStateFlow()

    /** Refresh the high-usage app list (and usage-access state). */
    fun refreshUsage() {
        val hasAccess = appUsageRepository.hasUsageAccess()
        _usage.update { it.copy(hasAccess = hasAccess, loading = hasAccess) }
        if (!hasAccess) {
            _usage.update { it.copy(apps = emptyList(), loading = false) }
            return
        }
        viewModelScope.launch {
            val apps = appUsageRepository.topBatteryApps()
            _usage.value = UsageUiState(hasAccess = true, loading = false, apps = apps)
        }
    }

    /** Best-effort stop of a package's background processes. */
    fun killBackground(packageName: String) = appUsageRepository.killBackground(packageName)

    companion object {
        /** How much recent telemetry to fetch for the dashboard chart. */
        private const val HISTORY_FETCH_LIMIT = 1_500

        /** Rolling window rendered by the dashboard history chart. */
        private const val HISTORY_WINDOW_MILLIS = 6L * 60L * 60L * 1000L
    }
}
