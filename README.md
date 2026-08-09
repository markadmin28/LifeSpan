# LifeSpan

LifeSpan is an Android battery, thermal-health, and charge-profile monitor built
with **Kotlin** and **Jetpack Compose (Material 3)**. It tracks charging speeds,
logs battery-wear telemetry to a local Room database, alerts on dangerous thermal
levels, and manages custom charge limits (e.g. an 80% stop alert).

## Features
- **Foreground monitoring service** (`BatteryMonitorService`): sticky FGS with a
  live notification (level, temp, wattage, time-to-full/empty).
- **Alerts**: overheat, charge-limit (optionally persistent), and rapid
  temperature-rise (≈2°C in 5 min).
- **Battery health**: estimated cycles, capacity (mAh), and health % from logged
  charge sessions.
- **Session charts**: tap a charge session for level / temperature / current trends.
- **High battery usage**: ranked apps with estimated % share and force-stop.
- **Floating charging bubble**: expandable overlay with charge-limit and overheat bars.
- **Reliability**: boot auto-start, WorkManager sampling, battery-opt exemption,
  home-screen widget, Quick Settings tile.
- **Onboarding**: first-run permission wizard (notifications, usage access, overlay,
  battery optimization).
- **Theme switcher**: System / Light / Dark × Cool / Warm accents.

## Architecture
MVVM + Clean-ish layering with unidirectional data flow:
- `domain/` — pure, unit-tested logic (`BatteryCalculator`, `BatteryHealthCalculator`,
  `AlertEvaluator`, `TimeEstimator`, `ChartMath`, …).
- `data/` — Room + DataStore + repositories.
- `service/` / `work/` / `widget/` — FGS, receivers, WorkManager, QS tile, widget.
- `ui/` — Compose screens + `MainViewModel` (StateFlow).

## Tech stack
- Kotlin 2.0, Jetpack Compose (Material 3), Coroutines/Flow
- Room, DataStore Preferences, WorkManager
- Roborazzi screenshot tests (Robolectric)
- AGP 8.7, Gradle 8.11, `minSdk 26` / `target/compileSdk 35`

## Building
Requires a JDK (17+) and the Android SDK (platform 35, build-tools 35).

```bash
# One-time toolchain bootstrap (installs the SDK + writes local.properties)
bash scripts/cloud-setup-android.sh

./gradlew testDebugUnitTest        # JVM unit tests (+ screenshot verify)
./gradlew recordRoborazziDebug     # (re)record goldens under app/src/test/screenshots/
./gradlew verifyRoborazziDebug     # compare against committed goldens
./gradlew assembleDebug            # debug APK
./gradlew lintDebug                # Android lint
```

If you already have an Android SDK, create a `local.properties` with
`sdk.dir=/path/to/Android/sdk` instead of running the bootstrap script.

## Permissions
`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`,
`VIBRATE`, `SYSTEM_ALERT_WINDOW`, `PACKAGE_USAGE_STATS`, `QUERY_ALL_PACKAGES`,
`KILL_BACKGROUND_PROCESSES`, `RECEIVE_BOOT_COMPLETED`,
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
