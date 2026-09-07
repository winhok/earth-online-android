#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
command -v kotlinc >/dev/null || { echo "Install Kotlin 1.9+ and a JDK to use this offline runner." >&2; exit 1; }
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
mapfile -t SOURCES < <(find "$ROOT/core/src/main/kotlin" -name '*.kt' -type f | sort)
kotlinc "${SOURCES[@]}" "$ROOT/core/src/test/kotlin/xyz/winhok/earthonline/core/ScenarioSuite.kt" -include-runtime -d "$OUT/core-tests.jar"
java -jar "$OUT/core-tests.jar"
