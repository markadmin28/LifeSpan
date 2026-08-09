package com.lifespan.app

import com.github.takahirom.roborazzi.captureRoboImage
import com.lifespan.app.data.db.ChargeSessionEntity
import com.lifespan.app.data.db.TelemetryLogEntity
import com.lifespan.app.data.prefs.Accent
import com.lifespan.app.data.prefs.AppSettings
import com.lifespan.app.domain.health.BatteryHealth
import com.lifespan.app.domain.history.ChartPoint
import com.lifespan.app.domain.history.ChartSeriesBuilder
import com.lifespan.app.domain.model.BatterySnapshot
import com.lifespan.app.domain.model.PlugType
import com.lifespan.app.domain.usage.AppUsage
import com.lifespan.app.ui.HighUsageScreen
import com.lifespan.app.ui.HistoryUiState
import com.lifespan.app.ui.MainScreen
import com.lifespan.app.ui.MainUiState
import com.lifespan.app.ui.SessionDetailScreen
import com.lifespan.app.ui.UsageUiState
import com.lifespan.app.ui.theme.LifeSpanTheme
import kotlin.math.sin
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

    /** Deterministic 4-hour telemetry window (one sample every 10 minutes). */
    private val history: HistoryUiState = run {
        val start = 1_699_985_600_000L
        val step = 10L * 60L * 1000L
        val levels = (0..24).map { i ->
            ChartPoint(start + i * step, (42.0 + i * 1.6).coerceAtMost(80.0))
        }
        val temps = (0..24).map { i ->
            ChartPoint(start + i * step, 33.0 + 4.0 * sin(i / 5.0) + i * 0.1)
        }
        HistoryUiState(
            levelSeries = ChartSeriesBuilder.build(levels, minValueSpan = 4.0),
            tempSeries = ChartSeriesBuilder.build(temps, minValueSpan = 2.0),
            sampleCount = levels.size,
        )
    }

    private val sessionLogs: List<TelemetryLogEntity> = run {
        val start = 1_699_996_400_000L
        val step = 3L * 60L * 1000L
        (0..20).map { i ->
            TelemetryLogEntity(
                id = i.toLong() + 1,
                sessionId = 1,
                timestamp = start + i * step,
                batteryLevel = 42 + (i * 38) / 20,
                voltageMv = 4200 + i * 5,
                currentMa = 2100 - i * 60,
                temperatureCelsius = 34.0 + 6.0 * sin(i / 6.5),
                isCharging = true,
            )
        }
    }

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
                    history = history,
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
                    history = history,
                )
            }
        }
    }

    @Test
    fun sessionDetail() {
        captureRoboImage("src/test/screenshots/session_detail.png") {
            LifeSpanTheme(darkTheme = true, accent = Accent.COOL) {
                SessionDetailScreen(
                    session = sessions.first(),
                    logs = sessionLogs,
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun highUsageList() {
        // lastUsedMillis=0 renders the stable "background" label (avoids wall-clock flakes).
        val usage = UsageUiState(
            hasAccess = true,
            apps = listOf(
                AppUsage("com.maps", "Maps", 45 * 60_000L, 0L, 42.0),
                AppUsage("com.browser", "Browser", 20 * 60_000L, 0L, 19.0),
                AppUsage("com.player", "Player", 8 * 60_000L, 0L, 9.0),
            ),
        )
        captureRoboImage("src/test/screenshots/high_usage.png") {
            LifeSpanTheme(darkTheme = true, accent = Accent.COOL) {
                HighUsageScreen(state = usage, onBack = {}, onForceStop = {})
            }
        }
    }
}
