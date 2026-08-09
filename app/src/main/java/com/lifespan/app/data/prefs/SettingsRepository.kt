package com.lifespan.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lifespan.app.domain.alert.AlertThresholds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lifespan_settings")

/** Persists all user settings using Jetpack DataStore. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val OVERHEAT_C = doublePreferencesKey("overheat_celsius")
        val CHARGE_LIMIT = intPreferencesKey("charge_limit_percent")
        val OVERHEAT_ENABLED = booleanPreferencesKey("overheat_enabled")
        val CHARGE_LIMIT_ENABLED = booleanPreferencesKey("charge_limit_enabled")
        val BUBBLE_ENABLED = booleanPreferencesKey("bubble_enabled")
        val AUTO_START = booleanPreferencesKey("auto_start_enabled")
        val PERIODIC_SAMPLING = booleanPreferencesKey("periodic_sampling_enabled")
        val PERSISTENT_ALARM = booleanPreferencesKey("persistent_charge_alarm")
        val RAPID_RISE = booleanPreferencesKey("rapid_rise_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT = stringPreferencesKey("accent")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    val appSettings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val d = AppSettings()
        AppSettings(
            thresholds = AlertThresholds(
                overheatCelsius = prefs[Keys.OVERHEAT_C] ?: d.thresholds.overheatCelsius,
                chargeLimitPercent = prefs[Keys.CHARGE_LIMIT] ?: d.thresholds.chargeLimitPercent,
                overheatEnabled = prefs[Keys.OVERHEAT_ENABLED] ?: d.thresholds.overheatEnabled,
                chargeLimitEnabled = prefs[Keys.CHARGE_LIMIT_ENABLED] ?: d.thresholds.chargeLimitEnabled,
            ),
            bubbleEnabled = prefs[Keys.BUBBLE_ENABLED] ?: d.bubbleEnabled,
            autoStartEnabled = prefs[Keys.AUTO_START] ?: d.autoStartEnabled,
            periodicSamplingEnabled = prefs[Keys.PERIODIC_SAMPLING] ?: d.periodicSamplingEnabled,
            persistentChargeAlarm = prefs[Keys.PERSISTENT_ALARM] ?: d.persistentChargeAlarm,
            rapidRiseEnabled = prefs[Keys.RAPID_RISE] ?: d.rapidRiseEnabled,
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: d.themeMode,
            accent = prefs[Keys.ACCENT]?.let { runCatching { Accent.valueOf(it) }.getOrNull() }
                ?: d.accent,
            onboardingComplete = prefs[Keys.ONBOARDING_COMPLETE] ?: d.onboardingComplete,
        )
    }

    val thresholds: Flow<AlertThresholds> = appSettings.map { it.thresholds }
    val bubbleEnabled: Flow<Boolean> = appSettings.map { it.bubbleEnabled }

    suspend fun setChargeLimit(percent: Int) =
        edit { it[Keys.CHARGE_LIMIT] = percent.coerceIn(1, 100) }

    suspend fun setOverheatCelsius(celsius: Double) = edit { it[Keys.OVERHEAT_C] = celsius }

    suspend fun setOverheatEnabled(enabled: Boolean) = edit { it[Keys.OVERHEAT_ENABLED] = enabled }

    suspend fun setChargeLimitEnabled(enabled: Boolean) =
        edit { it[Keys.CHARGE_LIMIT_ENABLED] = enabled }

    suspend fun setBubbleEnabled(enabled: Boolean) = edit { it[Keys.BUBBLE_ENABLED] = enabled }

    suspend fun setAutoStartEnabled(enabled: Boolean) = edit { it[Keys.AUTO_START] = enabled }

    suspend fun setPeriodicSamplingEnabled(enabled: Boolean) =
        edit { it[Keys.PERIODIC_SAMPLING] = enabled }

    suspend fun setPersistentChargeAlarm(enabled: Boolean) =
        edit { it[Keys.PERSISTENT_ALARM] = enabled }

    suspend fun setRapidRiseEnabled(enabled: Boolean) = edit { it[Keys.RAPID_RISE] = enabled }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME_MODE] = mode.name }

    suspend fun setAccent(accent: Accent) = edit { it[Keys.ACCENT] = accent.name }

    suspend fun setOnboardingComplete(complete: Boolean) =
        edit { it[Keys.ONBOARDING_COMPLETE] = complete }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
