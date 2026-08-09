package com.lifespan.app

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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * JVM screenshot tests (Robolectric + Roborazzi) — no emulator required.
 * Record goldens with `./gradlew recordRoborazziDebug`; verify with
 * `./gradlew verifyRoborazziDebug`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h915dp-xhdpi")
class LifeSpanScreenshotTest {

    private val snapshot = BatterySnapshot(
        timestamp = 1_700_000_000_000L,
        level = 64,
        voltageMv = 4300,
        currentUa = 1_500_000,
        temperatureCelsius = 38.5,
        isCharging = true,
        plugType = PlugType.AC,
        technology = "Li-ion",
        chargeCounterMicroAh = 1_800_000,
    )

    private val sessions = listOf(
        ChargeSessionEntity(
            id = 1,
            startTime = 1_699_996_400_000L,
            endTime = 1_700_000_000_000L,
            startLevel = 42,
            endLevel = 80,
            plugType = "AC",
            peakTempCelsius = 40.1,
            peakCurrentMa = 2100.0,
            totalMahAdded = 1500.0,
        ),
    )

    private val dashboardState = MainUiState(
        snapshot = snapshot,
        monitoring = true,
        sessions = sessions,
        bubbleEnabled = true,
    )

    private val health = BatteryHealth(
        estimatedCycles = 1.9,
        estimatedCapacityMah = 3000,
        healthPercent = 94,
        sampleCount = 3,
    )

    @Test
    fun dashboardDark() {
        captureRoboImage("src/test/screenshots/dashboard_dark.png") {
            LifeSpanTheme(darkTheme = true, accent = Accent.COOL) {
                MainScreen(
                    state = dashboardState,
                    settings = AppSettings(onboardingComplete = true),
                    onStart = {},
                    onStop = {},
                    onChargeLimitChange = {},
                    onOverheatChange = {},
                    onOverheatEnabledChange = {},
                    onChargeLimitEnabledChange = {},
                    health = health,
                )
            }
        }
    }

    @Test
    fun dashboardLight() {
        captureRoboImage("src/test/screenshots/dashboard_light.png") {
            LifeSpanTheme(darkTheme = false, accent = Accent.COOL) {
                MainScreen(
                    state = dashboardState,
                    settings = AppSettings(onboardingComplete = true),
                    onStart = {},
                    onStop = {},
                    onChargeLimitChange = {},
                    onOverheatChange = {},
                    onOverheatEnabledChange = {},
                    onChargeLimitEnabledChange = {},
                    health = health,
                )
            }
        }
    }

    @Test
    fun highUsageList() {
        val usage = UsageUiState(
            hasAccess = true,
            apps = listOf(
                AppUsage("com.maps", "Maps", 45 * 60_000L, snapshot.timestamp, 42.0),
                AppUsage("com.browser", "Browser", 20 * 60_000L, snapshot.timestamp, 19.0),
                AppUsage("com.player", "Player", 8 * 60_000L, snapshot.timestamp, 9.0),
            ),
        )
        captureRoboImage("src/test/screenshots/high_usage.png") {
            LifeSpanTheme(darkTheme = true, accent = Accent.COOL) {
                HighUsageScreen(state = usage, onBack = {}, onForceStop = {})
            }
        }
    }
}
