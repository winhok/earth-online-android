#!/usr/bin/env bash
# One-time signing bootstrap. Only the recovery PUBLIC certificate is in Git.
set -euo pipefail
umask 077
SIGNING_DIR="$RUNNER_TEMP/earth-signing"
mkdir -p "$SIGNING_DIR" release-output
password="$(openssl rand -hex 32)"
echo "::add-mask::$password"
export ANDROID_KEYSTORE_PASSWORD="$password"
export ANDROID_KEY_PASSWORD="$password"
export ANDROID_KEY_ALIAS=earth-online
export ANDROID_KEYSTORE_PATH="$SIGNING_DIR/release.p12"
keytool -genkeypair -keystore "$ANDROID_KEYSTORE_PATH" -storetype PKCS12 \
  -storepass:env ANDROID_KEYSTORE_PASSWORD -keypass:env ANDROID_KEY_PASSWORD \
  -alias "$ANDROID_KEY_ALIAS" -keyalg RSA -keysize 3072 -validity 10000 \
  -dname 'CN=Earth Online, OU=Android, O=winhok' >/dev/null 2>&1
python3 - <<'PY'
import json, os
from pathlib import Path
p=Path(os.environ['ANDROID_KEYSTORE_PATH']).parent
(p/'credentials.json').write_text(json.dumps({
 'keystore':'release.p12', 'alias':os.environ['ANDROID_KEY_ALIAS'],
 'store_password':os.environ['ANDROID_KEYSTORE_PASSWORD'],
 'key_password':os.environ['ANDROID_KEY_PASSWORD'],
 'application_id':'xyz.winhok.earthonline', 'source_sha':os.environ['GITHUB_SHA']}, indent=2))
PY
keytool -exportcert -rfc -keystore "$ANDROID_KEYSTORE_PATH" \
  -storepass:env ANDROID_KEYSTORE_PASSWORD -alias "$ANDROID_KEY_ALIAS" \
  -file release-output/app-signing-certificate.crt >/dev/null 2>&1
openssl x509 -in release-output/app-signing-certificate.crt -noout -fingerprint -sha256 > release-output/certificate-sha256.txt
tar -C "$SIGNING_DIR" -czf "$SIGNING_DIR/material.tar.gz" release.p12 credentials.json
openssl cms -encrypt -binary -aes-256-gcm -in "$SIGNING_DIR/material.tar.gz" \
  -outform DER -out release-output/signing-backup.cms \
  -recip release/recovery-certificate.crt -keyopt rsa_padding_mode:oaep -keyopt rsa_oaep_md:sha256
rm "$SIGNING_DIR/material.tar.gz"
{
 echo "ANDROID_KEYSTORE_PATH=$ANDROID_KEYSTORE_PATH"
 echo "ANDROID_KEYSTORE_PASSWORD=$ANDROID_KEYSTORE_PASSWORD"
 echo "ANDROID_KEY_PASSWORD=$ANDROID_KEY_PASSWORD"
 echo "ANDROID_KEY_ALIAS=$ANDROID_KEY_ALIAS"
} >> "$GITHUB_ENV"
