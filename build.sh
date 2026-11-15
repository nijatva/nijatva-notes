#!/usr/bin/env bash
# Builds nijatva-notes into a signed debug APK without Gradle.
# aapt2 -> javac -> d8 -> zip -> zipalign -> apksigner
#
# Needs: a JDK 17, Android build-tools 34.0.0 and platform android-34.
# Set SDK / BT / PLAT below to wherever your copy lives.
set -euo pipefail

SDK="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
BT="$SDK/build-tools/34.0.0"
PLAT="$SDK/platforms/android-34/android.jar"
APP="$(cd "$(dirname "$0")" && pwd)/app"
OUT="$(cd "$(dirname "$0")" && pwd)/build"
NAME=nijatva-notes

rm -rf "$OUT"
mkdir -p "$OUT/compiled" "$OUT/gen" "$OUT/classes" "$OUT/dex"

echo "== aapt2 compile"
"$BT/aapt2" compile --dir "$APP/res" -o "$OUT/compiled/res.zip"

echo "== aapt2 link"
"$BT/aapt2" link \
  -o "$OUT/base.apk" \
  -I "$PLAT" \
  --manifest "$APP/AndroidManifest.xml" \
  -R "$OUT/compiled/res.zip" \
  --java "$OUT/gen" \
  --min-sdk-version 26 \
  --target-sdk-version 34 \
  --version-code 2 \
  --version-name "0.1.1" \
  --auto-add-overlay

echo "== javac"
find "$APP/src" "$OUT/gen" -name '*.java' > "$OUT/sources.txt"
javac -source 8 -target 8 -nowarn \
  -bootclasspath "$PLAT" \
  -d "$OUT/classes" \
  @"$OUT/sources.txt" 2>&1 | grep -v 'bootstrap class path' || true

echo "== d8"
find "$OUT/classes" -name '*.class' > "$OUT/classes.txt"
"$BT/d8" -JXmx256M -JXss512k --lib "$PLAT" --min-api 26 --output "$OUT/dex" @"$OUT/classes.txt"

echo "== package"
cp "$OUT/base.apk" "$OUT/unsigned.apk"
(cd "$OUT/dex" && zip -q "$OUT/unsigned.apk" classes.dex)

echo "== zipalign"
"$BT/zipalign" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

echo "== keystore"
KS="${NIJATVA_KEYSTORE:-$HOME/.nijatva-debug.keystore}"
if [ ! -f "$KS" ]; then
  keytool -genkeypair -v -keystore "$KS" \
    -storepass android -keypass android -alias debugkey \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Nijatva Debug, O=DEVIRA, C=IN" >/dev/null 2>&1
fi

echo "== sign"
"$BT/apksigner" -JXmx256M -JXss512k sign \
  --ks "$KS" \
  --ks-pass pass:android \
  --key-pass pass:android \
  --ks-key-alias debugkey \
  --min-sdk-version 26 \
  --out "$OUT/$NAME.apk" \
  "$OUT/aligned.apk"

"$BT/apksigner" -JXmx256M -JXss512k verify --print-certs "$OUT/$NAME.apk" | head -4
echo "== done: $OUT/$NAME.apk"
ls -lh "$OUT/$NAME.apk"
