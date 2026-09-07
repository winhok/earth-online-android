#!/usr/bin/env sh
# Downloads only the official Gradle 8.13 wrapper JAR; verifies pinned upstream SHA-256.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
JAR="$ROOT/gradle/wrapper/gradle-wrapper.jar"
EXPECTED=81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f
hash_file() {
    if command -v sha256sum >/dev/null 2>&1; then sha256sum "$1" | cut -d ' ' -f 1
    elif command -v shasum >/dev/null 2>&1; then shasum -a 256 "$1" | cut -d ' ' -f 1
    else echo "A SHA-256 utility is required." >&2; exit 1; fi
}
if [ ! -f "$JAR" ]; then
    command -v curl >/dev/null 2>&1 || { echo "curl is required for first-run bootstrap." >&2; exit 1; }
    TEMP=$(mktemp "$ROOT/gradle/wrapper/.wrapper.XXXXXX")
    trap 'rm -f "$TEMP"' EXIT HUP INT TERM
    curl --fail --location --retry 3 --connect-timeout 20 --max-time 120 --proto '=https' --tlsv1.2         https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradle/wrapper/gradle-wrapper.jar -o "$TEMP"
    [ "$(hash_file "$TEMP")" = "$EXPECTED" ] || { echo "Wrapper checksum mismatch. Nothing executed." >&2; exit 1; }
    mv "$TEMP" "$JAR"
    trap - EXIT HUP INT TERM
fi
[ "$(hash_file "$JAR")" = "$EXPECTED" ] || { echo "Existing wrapper JAR checksum mismatch. Refusing to run." >&2; exit 1; }
