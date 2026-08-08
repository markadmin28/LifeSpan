# LifeSpan

LifeSpan is an Android battery, thermal-health, and charge-profile monitor built
with **Kotlin** and **Jetpack Compose (Material 3)**. It tracks charging speeds,
logs battery-wear telemetry to a local Room database, alerts on dangerous thermal
levels, and manages custom charge limits (e.g. an 80% stop alert).

## Features
- **Foreground monitoring service** (`BatteryMonitorService`): a sticky
  (`START_STICKY`) foreground service that listens for `ACTION_BATTERY_CHANGED`,
  reads instantaneous current via `BatteryManager.BATTERY_PROPERTY_CURRENT_NOW`,
  computes live wattage, and shows a persistent status notification.
- **Alerts**: audio + vibration when the battery temperature reaches the overheat
  threshold (default 42°C) or the user-defined charge limit is hit while charging.
- **Room database** (`LifeSpanDatabase`): `charge_sessions` and
  `battery_telemetry_logs` (with a cascading foreign key) persist every session
  and telemetry sample.
- **Compose UI**: live battery card (level, temperature, power, current, voltage),
  start/stop control, configurable thresholds, and charge-session history.

## Architecture
MVVM + Clean-ish layering with unidirectional data flow:
- `domain/` — pure, unit-tested logic (`BatteryCalculator`, `AlertEvaluator`,
  `SessionAccumulator`, models). No Android dependencies.
- `data/` — Room database + DAOs, DataStore-backed `SettingsRepository`, and
  `BatteryRepository` (the single source of truth exposing `StateFlow`s).
- `service/` — the foreground `BatteryMonitorService` and battery `BroadcastReceiver`.
- `ui/` — `MainViewModel` (StateFlow) and Compose Material 3 screens.

## Tech stack
- Kotlin 2.0, Jetpack Compose (Material 3), Coroutines/Flow
- Room, DataStore Preferences
- AGP 8.7, Gradle 8.11, `minSdk 26` / `target/compileSdk 35`

## Building
Requires a JDK (17+) and the Android SDK (platform 35, build-tools 35).

```bash
# One-time toolchain bootstrap (installs the SDK + writes local.properties)
bash scripts/cloud-setup-android.sh

./gradlew testDebugUnitTest   # run the JVM unit tests
./gradlew assembleDebug       # build the debug APK (app/build/outputs/apk/debug/)
./gradlew lintDebug           # Android lint
```

If you already have an Android SDK, create a `local.properties` with
`sdk.dir=/path/to/Android/sdk` instead of running the bootstrap script.

## Permissions
`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`,
`FOREGROUND_SERVICE_CONNECTED_DEVICE`, `POST_NOTIFICATIONS`, and `VIBRATE`.
