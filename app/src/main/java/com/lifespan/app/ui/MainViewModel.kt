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
import com.lifespan.app.service.BatteryMonitorService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val snapshot: BatterySnapshot? = null,
    val thresholds: AlertThresholds = AlertThresholds(),
    val monitoring: Boolean = false,
    val sessions: List<ChargeSessionEntity> = emptyList(),
    val activeAlerts: Set<AlertType> = emptySet(),
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as LifeSpanApp).container
    private val batteryRepository = container.batteryRepository
    private val settingsRepository = container.settingsRepository

    val uiState: StateFlow<MainUiState> = combine(
        batteryRepository.latest,
        settingsRepository.thresholds,
        batteryRepository.monitoring,
        batteryRepository.observeRecentSessions(limit = 25),
    ) { snapshot, thresholds, monitoring, sessions ->
        MainUiState(
            snapshot = snapshot,
            thresholds = thresholds,
            monitoring = monitoring,
            sessions = sessions,
            activeAlerts = snapshot?.let { AlertEvaluator.evaluate(it, thresholds) } ?: emptySet(),
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
}
