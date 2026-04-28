#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
REPO_ROOT="$SCRIPT_DIR/.."
UNSIGNED_APK="$REPO_ROOT/android/app/build/outputs/apk/release/app-release-unsigned.apk"
SIGNED_APK="$REPO_ROOT/build/sinch-android-reference-app.apk"

mkdir -p "$(dirname "$SIGNED_APK")"

echo "Building Android release APK..."
cd "$REPO_ROOT/android"
./gradlew clean :app:assembleRelease

echo "Signing APK..."
APKSIGNER=$(find "$ANDROID_HOME/build-tools" -name "apksigner" -type f | sort | tail -1)
"$APKSIGNER" sign \
  --ks /tmp/release.jks \
  --ks-key-alias "$RELEASE_KEY_ALIAS" \
  --ks-pass "pass:$RELEASE_STORE_PASSWORD" \
  --key-pass "pass:$RELEASE_KEY_PASSWORD" \
  --out "$SIGNED_APK" \
  "$UNSIGNED_APK"

echo "Signed APK available at $SIGNED_APK"
