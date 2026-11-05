#!/usr/bin/env bash
# Builds dist/Songbook-v<version>.apk from source.
# Needs: bash, Java 17+ (javac), Python 3 with pandas + xlrd (pip install -r requirements.txt), curl, unzip.
# Signing: set SONGBOOK_KEYSTORE, SONGBOOK_KS_PASS, SONGBOOK_KEY_ALIAS and SONGBOOK_KEY_PASS to sign with your
# own release key (needed so updates install over an existing install). Without them the APK is signed with a
# debug key, which installs fine but can't update an APK signed with a different key.
set -euo pipefail
cd "$(dirname "$0")"
B=build; T=tools/bin; mkdir -p "$B" "$T" dist

dl() { [ -s "$2" ] || { echo "downloading $(basename "$2")"; curl -fsSL -o "$2" "$1"; }; }
dl https://github.com/iBotPeaches/Apktool/releases/download/v2.10.0/apktool_2.10.0.jar "$T/apktool.jar"
dl https://github.com/patrickfav/uber-apk-signer/releases/download/v1.3.0/uber-apk-signer-1.3.0.jar "$T/uber-apk-signer.jar"
if [ ! -s "$T/jadx/lib/jadx-1.5.1-all.jar" ]; then      # jadx bundles Google's D8 dex compiler
  dl https://github.com/skylot/jadx/releases/download/v1.5.1/jadx-1.5.1.zip "$T/jadx.zip"
  unzip -q -o "$T/jadx.zip" -d "$T/jadx"
fi

echo "1/5 song list -> data"
python3 scripts/build_data.py data/songhub_prebuilt_songlist.xls "$B/data.json"

echo "2/5 page"
python3 scripts/make_page.py web/template.html "$B/data.json" android/assets/index.html android/apktool.yml

echo "3/5 native code (Wi-Fi connection, scan, Bluetooth keypad)"
rm -rf "$B/stubcls" "$B/classes" "$B/dex"; mkdir -p "$B/stubcls" "$B/classes" "$B/dex"
javac --release 8 -nowarn -d "$B/stubcls" $(find native/stubs -name '*.java')
javac --release 8 -nowarn -cp "$B/stubcls" -d "$B/classes" native/src/com/songbook/app/*.java
java -cp "$T/jadx/lib/jadx-1.5.1-all.jar" com.android.tools.r8.D8 --release --min-api 21 --output "$B/dex" \
  $(find "$B/classes" -name '*.class') 2>&1 | grep -v -E "^Warning|desugaring|was not found" || true

echo "4/5 apk"
java -jar "$T/apktool.jar" b android -o "$B/unsigned.apk"
python3 - "$B/unsigned.apk" "$B/dex/classes.dex" <<'PY'
import sys, zipfile
apk, dex = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(apk, 'a', zipfile.ZIP_DEFLATED) as z:
    if 'classes2.dex' not in z.namelist(): z.write(dex, 'classes2.dex')
PY

echo "5/5 sign"
VER=$(grep -E 'versionName:' android/apktool.yml | awk '{print $2}')
rm -rf "$B/signed"
if [ -n "${SONGBOOK_KEYSTORE:-}" ]; then
  java -jar "$T/uber-apk-signer.jar" -a "$B/unsigned.apk" -o "$B/signed" --ks "$SONGBOOK_KEYSTORE" \
    --ksAlias "${SONGBOOK_KEY_ALIAS:-songbook}" --ksPass "$SONGBOOK_KS_PASS" --ksKeyPass "${SONGBOOK_KEY_PASS:-$SONGBOOK_KS_PASS}" | grep -E "verified|Successfully"
else
  echo "SONGBOOK_KEYSTORE not set: signing with a debug key"
  java -jar "$T/uber-apk-signer.jar" -a "$B/unsigned.apk" -o "$B/signed" | grep -E "verified|Successfully"
fi
cp "$B"/signed/*.apk "dist/Songbook-v$VER.apk"
echo "done: dist/Songbook-v$VER.apk"
