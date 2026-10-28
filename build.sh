#!/bin/bash
# Build the NGP Pad IME without Gradle/Android Studio.
# Needs: JDK 17 + Android SDK platform 34 + build-tools 34.0.0.
set -euo pipefail

ROOT=$(cd "$(dirname "$0")" && pwd -W)
JDK=${JDK:-E:/ngp45ht/ime-tools/jdk-17.0.20.1+1}
SDK=${SDK:-E:/ngp45ht/ime-tools/sdk}
BT="$SDK/build-tools/34.0.0"
AJ="$SDK/platforms/android-34/android.jar"
JAVA="$JDK/bin/java"
JAVAC="$JDK/bin/javac"
JAR="$JDK/bin/jar"
KEYTOOL="$JDK/bin/keytool"
KS="$ROOT/keystore/debug.keystore"

AAPT2="$BT/aapt2.exe"
ZIPALIGN="$BT/zipalign.exe"
D8JAR="$BT/lib/d8.jar"
APKSIGNER="$BT/lib/apksigner.jar"

APP="$ROOT/app"
OUT="$ROOT/build"
rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/dex"

echo "== compile resources =="
"$AAPT2" compile --dir "$APP/res" -o "$OUT/res.zip"

echo "== link base apk =="
"$AAPT2" link \
  -o "$OUT/base.apk" \
  -I "$AJ" \
  --manifest "$APP/AndroidManifest.xml" \
  -R "$OUT/res.zip" \
  --auto-add-overlay \
  --java "$OUT/gen" \
  -A "$APP/assets" \
  --min-sdk-version 30 \
  --target-sdk-version 34

echo "== compile java =="
find "$APP/src" -name '*.java' > "$OUT/sources.txt"
find "$OUT/gen" -name '*.java' >> "$OUT/sources.txt"
"$JAVAC" -encoding UTF-8 -source 8 -target 8 -bootclasspath "$AJ" \
  -d "$OUT/classes" @"$OUT/sources.txt"

echo "== dex =="
find "$OUT/classes" -name '*.class' > "$OUT/classes.txt"
"$JAVA" -cp "$D8JAR" com.android.tools.r8.D8 \
  --min-api 30 --lib "$AJ" --output "$OUT/dex" @"$OUT/classes.txt"

echo "== package =="
cp -f "$OUT/base.apk" "$OUT/unsigned.apk"
(cd "$OUT/dex" && "$JAR" uf ../unsigned.apk classes.dex)
"$ZIPALIGN" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

if [ ! -f "$KS" ]; then
  mkdir -p "$ROOT/keystore"
  "$KEYTOOL" -genkeypair -keystore "$KS" \
    -storepass android -keypass android -alias androiddebugkey \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1
fi

echo "== sign =="
"$JAVA" -cp "$APKSIGNER" com.android.apksigner.ApkSignerTool sign \
  --ks "$KS" --ks-pass pass:android --key-pass pass:android \
  --ks-key-alias androiddebugkey \
  --out "$OUT/ngp-ime.apk" "$OUT/aligned.apk"

ls -lh "$OUT/ngp-ime.apk"
"$JDK/bin/keytool" -printcert -jarfile "$OUT/ngp-ime.apk" 2>/dev/null | head -5 || true
