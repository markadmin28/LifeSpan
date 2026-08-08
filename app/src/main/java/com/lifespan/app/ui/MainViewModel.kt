package com.lifespan.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifespan.app.LifeSpanApp
import com.lifespan.app.data.db.ChargeSessionEntity
import com.lifespan.app.domain.alert.AlertEvaluator
import com.lifespan.app.domain.alert.AlertThresholds
import com.lifespan.app.domain.alert.AlertType
import com.lifespan.app.domain.model.BatterySnapshot
import com.lifespan.app.domain.usage.AppUsage
import com.lifespan.app.domain.usage.UsageRanker
import com.lifespan.app.service.BatteryMonitorService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
}
