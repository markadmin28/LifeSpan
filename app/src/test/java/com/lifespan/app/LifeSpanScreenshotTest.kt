package com.lifespan.app

import com.github.takahirom.roborazzi.captureRoboImage
import com.lifespan.app.data.db.ChargeSessionEntity
import com.lifespan.app.data.prefs.Accent
import com.lifespan.app.data.prefs.AppSettings
import com.lifespan.app.data.prefs.ThemeMode
import com.lifespan.app.domain.health.BatteryHealthCalculator
import com.lifespan.app.domain.model.BatterySnapshot
import com.lifespan.app.domain.model.PlugType
import com.lifespan.app.domain.usage.AppUsage
import com.lifespan.app.ui.HighUsageScreen
import com.lifespan.app.ui.MainScreen
import com.lifespan.app.ui.MainUiState
import com.lifespan.app.ui.UsageUiState
import com.lifespan.app.ui.theme.LifeSpanTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w412dp-h915dp-xxhdpi")
class LifeSpanScreenshotTest {

    @Test
    fun dashboardDark() {
        captureRoboImage(filePath = "src/test/screenshots/dashboard_dark.png") {
            LifeSpanTheme(darkTheme = true, accent = Accent.COOL) {
                MainScreen(
                    state = dashboardState,
                    settings = AppSettings(
                        onboardingCompleted = true,
                        themeMode = ThemeMode.DARK,
                    ),
                    onStart = {},
                    onStop = {},
                    onChargeLimitChange = {},
                    onOverheatChange = {},
                    onOverheatEnabledChange = {},
                    onChargeLimitEnabledChange = {},
                    topUsageLabel = "Maps",
                    topUsagePercent = 34,
                    highUsageCount = 2,
                )
            }
        }
    }

    @Test
    fun dashboardLight() {
        captureRoboImage(filePath = "src/test/screenshots/dashboard_light.png") {
            LifeSpanTheme(darkTheme = false, accent = Accent.COOL) {
                MainScreen(
                    state = dashboardState,
                    settings = AppSettings(
                        onboardingCompleted = true,
                        themeMode = ThemeMode.LIGHT,
                    ),
                    onStart = {},
                    onStop = {},
                    onChargeLimitChange = {},
                    onOverheatChange = {},
                    onOverheatEnabledChange = {},
                    onChargeLimitEnabledChange = {},
                    topUsageLabel = "Maps",
                    topUsagePercent = 34,
                    highUsageCount = 2,
                )
            }
        }
    }

    @Test
    fun highUsage() {
        captureRoboImage(filePath = "src/test/screenshots/high_usage.png") {
            LifeSpanTheme(darkTheme = true, accent = Accent.COOL) {
                HighUsageScreen(
                    state = UsageUiState(
                        hasAccess = true,
                        apps = listOf(
                            AppUsage("com.example.maps", "Maps", 3_600_000, batteryPercent = 42.0),
                            AppUsage("com.example.video", "Video", 2_400_000, batteryPercent = 28.0),
                            AppUsage("com.example.chat", "Chat", 1_200_000, batteryPercent = 14.0),
                        ),
                    ),
                    onBack = {},
                    onForceStop = {},
                )
            }
        }
    }

    private companion object {
        const val FIXED_TIME = 1_735_732_800_000L

        val session = ChargeSessionEntity(
            id = 1,
            startTime = FIXED_TIME - 3_600_000,
            endTime = FIXED_TIME,
            startLevel = 28,
            endLevel = 80,
            plugType = "AC",
            peakTempCelsius = 38.2,
            peakCurrentMa = 2_350.0,
            totalMahAdded = 2_080.0,
        )

        val dashboardState = MainUiState(
            snapshot = BatterySnapshot(
                timestamp = FIXED_TIME,
                level = 78,
                voltageMv = 4_210,
                currentUa = 1_850_000,
                temperatureCelsius = 36.4,
                isCharging = true,
                plugType = PlugType.AC,
                technology = "Li-ion",
                chargeCounterMicroAh = 3_120_000,
            ),
            monitoring = true,
            sessions = listOf(session),
            health = BatteryHealthCalculator.calculate(listOf(session)),
        )
    }
}
