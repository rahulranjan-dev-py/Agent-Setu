#!/usr/bin/env bash
# Prepares a signed release APK for distribution: copies it to AgentSetu-v<version>.apk, prints its
# SHA-256 checksum and the signing certificate fingerprint, and shows the version.json fields to
# update. Run from the repository root after `./gradlew :app:assembleRelease` with keystore.properties
# in place. See docs/RELEASE.md.
set -euo pipefail

APK="app/build/outputs/apk/release/app-release.apk"
if [[ ! -f "$APK" ]]; then
  echo "No signed APK at $APK. Is keystore.properties set up? (docs/RELEASE.md, step 2)" >&2
  exit 1
fi

VERSION_NAME=$(grep -m1 'versionName = ' app/build.gradle.kts | sed -E 's/.*"(.*)".*/\1/')
VERSION_CODE=$(grep -m1 'versionCode = ' app/build.gradle.kts | sed -E 's/[^0-9]*([0-9]+).*/\1/')
OUT_DIR="release/out"
OUT="$OUT_DIR/AgentSetu-v${VERSION_NAME}.apk"
mkdir -p "$OUT_DIR"
cp "$APK" "$OUT"

if command -v sha256sum >/dev/null; then
  SHA=$(sha256sum "$OUT" | cut -d' ' -f1)
else
  SHA=$(shasum -a 256 "$OUT" | cut -d' ' -f1)
fi

echo "APK:          $OUT"
echo "Version:      $VERSION_NAME (code $VERSION_CODE)"
echo "SHA-256:      $SHA"

APKSIGNER=$(ls "${ANDROID_HOME:-$HOME/Android/Sdk}"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1 || true)
if [[ -n "$APKSIGNER" ]]; then
  echo
  echo "Signing certificate (must match the fingerprint pinned in the WhatsApp group):"
  "$APKSIGNER" verify --print-certs "$OUT" | grep -i 'SHA-256'
else
  echo "(apksigner not found; check the certificate with: apksigner verify --print-certs $OUT)"
fi

cat <<JSON

Update release/version.json with:
  "latestVersionCode": $VERSION_CODE,
  "latestVersionName": "$VERSION_NAME",
  "sha256": "$SHA",
  "releasedOn": "$(date +%d-%m-%Y)",
JSON
