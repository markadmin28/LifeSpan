package com.lifespan.app

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.lifespan.app.data.db.ChargeSessionEntity
import com.lifespan.app.data.prefs.Accent
import com.lifespan.app.data.prefs.AppSettings
import com.lifespan.app.domain.health.BatteryHealth
import com.lifespan.app.domain.model.BatterySnapshot
import com.lifespan.app.domain.model.PlugType
import com.lifespan.app.domain.usage.AppUsage
import com.lifespan.app.ui.HighUsageScreen
import com.lifespan.app.ui.MainScreen
import com.lifespan.app.ui.MainUiState
import com.lifespan.app.ui.UsageUiState
import com.lifespan.app.ui.theme.LifeSpanTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel5, application = Application::class)
class LifeSpanScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dashboardDark() {
        composeRule.setContent {
            LifeSpanTheme(darkTheme = true, accent = Accent.COOL) {
                MainScreen(
                    state = sampleUiState(),
                    health = sampleHealth(),
                    settings = AppSettings(onboardingComplete = true),
                    onStart = {},
                    onStop = {},
                    onChargeLimitChange = {},
                    onOverheatChange = {},
                    onOverheatEnabledChange = {},
                    onChargeLimitEnabledChange = {},
                    topUsageLabel = "Maps",
                    topUsagePercent = 18,
                    highUsageCount = 2,
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun dashboardLight() {
        composeRule.setContent {
            LifeSpanTheme(darkTheme = false, accent = Accent.COOL) {
                MainScreen(
                    state = sampleUiState(),
                    health = sampleHealth(),
                    settings = AppSettings(onboardingComplete = true),
                    onStart = {},
                    onStop = {},
                    onChargeLimitChange = {},
                    onOverheatChange = {},
                    onOverheatEnabledChange = {},
                    onChargeLimitEnabledChange = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun highUsageList() {
        composeRule.setContent {
            LifeSpanTheme(darkTheme = true, accent = Accent.COOL) {
                HighUsageScreen(
                    state = UsageUiState(
                        hasAccess = true,
                        loading = false,
                        apps = listOf(
                            AppUsage(
                                packageName = "com.maps",
                                label = "Maps",
                                foregroundMillis = 3_600_000,
                                batteryPercent = 18.0,
                            ),
                            AppUsage(
                                packageName = "com.chat",
                                label = "Chat",
                                foregroundMillis = 1_800_000,
                                batteryPercent = 9.0,
                            ),
                        ),
                    ),
                    onBack = {},
                    onForceStop = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    private fun sampleUiState() = MainUiState(
        snapshot = BatterySnapshot(
            timestamp = 1_700_000_000_000L,
            level = 78,
            voltageMv = 4210,
            currentUa = 1_850_000,
            temperatureCelsius = 39.4,
            isCharging = true,
            plugType = PlugType.AC,
            technology = "Li-ion",
            chargeCounterMicroAh = 2_400_000,
        ),
        monitoring = true,
        sessions = listOf(
            ChargeSessionEntity(
                id = 1,
                startTime = 1_700_000_000_000L - 3_600_000,
                endTime = 1_700_000_000_000L,
                startLevel = 42,
                endLevel = 80,
                plugType = "AC",
                peakTempCelsius = 40.1,
                peakCurrentMa = 2100.0,
                totalMahAdded = 1500.0,
            ),
        ),
    )

    private fun sampleHealth() = BatteryHealth(
        estimatedCycles = 12.4,
        estimatedCapacityMah = 3820,
        healthPercent = 94,
        sampleCount = 8,
    )
}
