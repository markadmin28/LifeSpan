package com.lifespan.app

import com.github.takahirom.roborazzi.captureRoboImage
import com.lifespan.app.data.db.ChargeSessionEntity
import com.lifespan.app.data.db.TelemetryLogEntity
import com.lifespan.app.data.prefs.Accent
import com.lifespan.app.data.prefs.AppSettings
import com.lifespan.app.domain.health.BatteryGuidanceStatus
import com.lifespan.app.domain.health.BatteryHealth
import com.lifespan.app.domain.model.BatterySnapshot
import com.lifespan.app.domain.model.PlugType
import com.lifespan.app.domain.trends.ChargeTrendCalculator
import com.lifespan.app.domain.usage.AppUsage
import com.lifespan.app.ui.ChargeTrendsScreen
import com.lifespan.app.ui.HighUsageScreen
import com.lifespan.app.ui.MainScreen
import com.lifespan.app.ui.MainUiState
import com.lifespan.app.ui.OnboardingScreen
import com.lifespan.app.ui.SessionDetailScreen
import com.lifespan.app.ui.UsageUiState
import com.lifespan.app.ui.theme.LifeSpanTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.ZoneId

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
        capacityFadePercent = 6,
        guidanceStatus = BatteryGuidanceStatus.STABLE,
    )

    @Test
    fun dashboardDark() {
        captureRoboImage("src/test/screenshots/dashboard_dark.png") {
            LifeSpanTheme(darkTheme = true, accent = Accent.COOL) {
                MainScreen(
                    state = dashboardState,
                    settings = AppSettings(),
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
                    settings = AppSettings(),
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
                AppUsage("com.maps", "Maps", 45 * 60_000L, Long.MAX_VALUE, 42.0),
                AppUsage("com.browser", "Browser", 20 * 60_000L, Long.MAX_VALUE, 19.0),
                AppUsage("com.player", "Player", 8 * 60_000L, Long.MAX_VALUE, 9.0),
            ),
        )
        captureRoboImage("src/test/screenshots/high_usage.png") {
            LifeSpanTheme(darkTheme = true, accent = Accent.COOL) {
                HighUsageScreen(state = usage, onBack = {}, onForceStop = {})
            }
        }
    }

    @Test
    fun onboardingSetup() {
        captureRoboImage("src/test/screenshots/onboarding_setup.png") {
            LifeSpanTheme(darkTheme = false, accent = Accent.COOL) {
                OnboardingScreen(
                    notificationsGranted = true,
                    usageAccessGranted = false,
                    canDrawOverlays = false,
                    ignoringBatteryOptimizations = true,
                    onRequestNotifications = {},
                    onRequestUsageAccess = {},
                    onRequestOverlay = {},
                    onRequestBatteryOptimizations = {},
                    onFinish = {},
                    autoStartEnabled = true,
                    periodicSamplingEnabled = true,
                )
            }
        }
    }

    @Test
    fun chargeSessionDetail() {
        val logs = listOf(
            telemetry(1, 1_699_996_400_000L, 42, 1_900, 34.5),
            telemetry(2, 1_699_997_120_000L, 49, 2_100, 36.2),
            telemetry(3, 1_699_997_840_000L, 57, 1_850, 38.1),
            telemetry(4, 1_699_998_560_000L, 65, 1_600, 39.4),
            telemetry(5, 1_699_999_280_000L, 73, 1_250, 39.0),
            telemetry(6, 1_700_000_000_000L, 80, 900, 37.8),
        )
        captureRoboImage("src/test/screenshots/session_detail.png") {
            LifeSpanTheme(darkTheme = true, accent = Accent.COOL) {
                SessionDetailScreen(
                    session = sessions.first(),
                    logs = logs,
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun chargeTrendsDaily() {
        val now = Instant.parse("2026-08-09T12:00:00Z")
        val trendSessions = listOf(
            trendSession(1, "2026-08-03T08:00:00Z", 25, 80, 1_650.0, 36.5),
            trendSession(2, "2026-08-05T09:30:00Z", 40, 82, 1_300.0, 38.2),
            trendSession(3, "2026-08-05T18:00:00Z", 60, 90, 950.0, 40.1),
            trendSession(4, "2026-08-07T07:15:00Z", 15, 75, 1_850.0, 37.4),
            trendSession(5, "2026-08-09T10:00:00Z", 48, 80, 1_020.0, 39.3),
        )
        val trends = ChargeTrendCalculator.compute(trendSessions, now, ZoneId.of("UTC"))
        captureRoboImage("src/test/screenshots/charge_trends_daily.png") {
            LifeSpanTheme(darkTheme = false, accent = Accent.COOL) {
                ChargeTrendsScreen(trends = trends, onBack = {})
            }
        }
    }

    private fun telemetry(
        id: Long,
        timestamp: Long,
        level: Int,
        currentMa: Int,
        temperature: Double,
    ) = TelemetryLogEntity(
        id = id,
        sessionId = 1,
        timestamp = timestamp,
        batteryLevel = level,
        voltageMv = 4_100,
        currentMa = currentMa,
        temperatureCelsius = temperature,
        isCharging = true,
    )

    private fun trendSession(
        id: Long,
        start: String,
        startLevel: Int,
        endLevel: Int,
        mah: Double,
        temperature: Double,
    ): ChargeSessionEntity {
        val startInstant = Instant.parse(start)
        return ChargeSessionEntity(
            id = id,
            startTime = startInstant.toEpochMilli(),
            endTime = startInstant.plusSeconds(3_600).toEpochMilli(),
            startLevel = startLevel,
            endLevel = endLevel,
            plugType = "AC",
            peakTempCelsius = temperature,
            peakCurrentMa = 2_100.0,
            totalMahAdded = mah,
        )
    }
}
