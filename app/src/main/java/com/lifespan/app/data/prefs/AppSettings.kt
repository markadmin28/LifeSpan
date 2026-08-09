package com.lifespan.app.data.prefs

import com.lifespan.app.domain.alert.AlertThresholds

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class Accent { COOL, WARM }

/** All user-configurable settings, surfaced as a single immutable snapshot. */
data class AppSettings(
    val thresholds: AlertThresholds = AlertThresholds(),
    val bubbleEnabled: Boolean = true,
    val autoStartEnabled: Boolean = false,
    val periodicSamplingEnabled: Boolean = false,
    val persistentChargeAlarm: Boolean = false,
    val rapidRiseEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: Accent = Accent.COOL,
    val onboardingCompleted: Boolean = false,
)
