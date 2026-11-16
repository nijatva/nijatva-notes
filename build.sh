#!/usr/bin/env bash
# Fallback build: aapt2 -> javac -> d8 -> zipalign -> apksigner, no Gradle.
# The primary build is `./gradlew assembleDebug`; this script exists so the app
# can still be packaged on a machine without Gradle or network access.
#
# Needs: JDK 17, build-tools 34.0.0 and platform android-34.
set -euo pipefail

SDK="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
BT="$SDK/build-tools/34.0.0"
PLAT="$SDK/platforms/android-34/android.jar"
ROOT="$(cd "$(dirname "$0")" && pwd)"
MAIN="$ROOT/app/src/main"
OUT="$ROOT/build/script"
NAME=nijatva-notes

rm -rf "$OUT"
mkdir -p "$OUT/compiled" "$OUT/gen" "$OUT/classes" "$OUT/dex"

echo "== aapt2 compile"
"$BT/aapt2" compile --dir "$MAIN/res" -o "$OUT/compiled/res.zip"

echo "== aapt2 link"
"$BT/aapt2" link \
  -o "$OUT/base.apk" \
  -I "$PLAT" \
  --manifest "$MAIN/AndroidManifest.xml" \
  -R "$OUT/compiled/res.zip" \
  --java "$OUT/gen" \
  --min-sdk-version 26 \
  --target-sdk-version 34 \
  --version-code 3 \
  --version-name "0.2.0" \
  --auto-add-overlay

echo "== javac"
find "$MAIN/java" "$OUT/gen" -name '*.java' > "$OUT/sources.txt"
javac -source 8 -target 8 -nowarn \
  -bootclasspath "$PLAT" \
  -d "$OUT/classes" \
  @"$OUT/sources.txt" 2>&1 | grep -v 'bootstrap class path' || true

echo "== d8"
find "$OUT/classes" -name '*.class' > "$OUT/classes.txt"
"$BT/d8" --lib "$PLAT" --min-api 26 --output "$OUT/dex" @"$OUT/classes.txt"

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
"$BT/apksigner" sign \
  --ks "$KS" \
  --ks-pass pass:android \
  --key-pass pass:android \
  --ks-key-alias debugkey \
  --min-sdk-version 26 \
  --out "$OUT/$NAME.apk" \
  "$OUT/aligned.apk"

"$BT/apksigner" verify --print-certs "$OUT/$NAME.apk" | head -4
echo "== done: $OUT/$NAME.apk"
