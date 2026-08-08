package com.lifespan.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lifespan.app.domain.alert.AlertThresholds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lifespan_settings")

/** Persists user-configurable alert thresholds using Jetpack DataStore. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val OVERHEAT_C = doublePreferencesKey("overheat_celsius")
        val CHARGE_LIMIT = intPreferencesKey("charge_limit_percent")
        val OVERHEAT_ENABLED = booleanPreferencesKey("overheat_enabled")
        val CHARGE_LIMIT_ENABLED = booleanPreferencesKey("charge_limit_enabled")
        val BUBBLE_ENABLED = booleanPreferencesKey("bubble_enabled")
    }

    val thresholds: Flow<AlertThresholds> = context.dataStore.data.map { prefs ->
        val defaults = AlertThresholds()
        AlertThresholds(
            overheatCelsius = prefs[Keys.OVERHEAT_C] ?: defaults.overheatCelsius,
            chargeLimitPercent = prefs[Keys.CHARGE_LIMIT] ?: defaults.chargeLimitPercent,
            overheatEnabled = prefs[Keys.OVERHEAT_ENABLED] ?: defaults.overheatEnabled,
            chargeLimitEnabled = prefs[Keys.CHARGE_LIMIT_ENABLED] ?: defaults.chargeLimitEnabled,
        )
    }

    suspend fun setChargeLimit(percent: Int) {
        context.dataStore.edit { it[Keys.CHARGE_LIMIT] = percent.coerceIn(1, 100) }
    }

    suspend fun setOverheatCelsius(celsius: Double) {
        context.dataStore.edit { it[Keys.OVERHEAT_C] = celsius }
    }

    suspend fun setOverheatEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.OVERHEAT_ENABLED] = enabled }
    }

    suspend fun setChargeLimitEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CHARGE_LIMIT_ENABLED] = enabled }
    }

    /** Whether the floating charging bubble overlay is enabled. Defaults to on. */
    val bubbleEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.BUBBLE_ENABLED] ?: true
    }

    suspend fun setBubbleEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BUBBLE_ENABLED] = enabled }
    }
}
