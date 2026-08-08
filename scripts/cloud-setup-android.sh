#!/usr/bin/env bash
#
# Idempotent Android toolchain bootstrap for Cloud Agent environments.
# Installs the Android command-line tools, platform 35 and build-tools 35 into
# $ANDROID_HOME (default: $HOME/android-sdk), accepts licenses, and points
# Gradle at the SDK via local.properties. Safe to re-run.
set -euo pipefail

ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
CMDLINE_TOOLS_VERSION="11076708"
PLATFORM="platforms;android-35"
BUILD_TOOLS="build-tools;35.0.0"

echo "==> Using ANDROID_HOME=$ANDROID_HOME"
mkdir -p "$ANDROID_HOME/cmdline-tools"

SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
if [ ! -x "$SDKMANAGER" ]; then
  echo "==> Installing Android command-line tools"
  tmp="$(mktemp -d)"
  curl -fL --retry 4 --retry-delay 3 -o "$tmp/cmdtools.zip" \
    "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
  unzip -q "$tmp/cmdtools.zip" -d "$tmp"
  rm -rf "$ANDROID_HOME/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
  rm -rf "$tmp"
fi

echo "==> Accepting SDK licenses"
yes | "$SDKMANAGER" --licenses >/dev/null 2>&1 || true

echo "==> Installing platform-tools, $PLATFORM, $BUILD_TOOLS"
"$SDKMANAGER" "platform-tools" "$PLATFORM" "$BUILD_TOOLS" >/dev/null

echo "==> Writing local.properties"
echo "sdk.dir=$ANDROID_HOME" > local.properties

echo "==> Verifying Gradle wrapper"
./gradlew --version >/dev/null

echo "==> Android toolchain ready"
