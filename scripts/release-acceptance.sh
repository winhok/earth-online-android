#!/usr/bin/env bash
set -uo pipefail
# Collect both evidence streams even if one fails. Either failure blocks publication.
bash scripts/emulator-acceptance.sh
instrumented_status=$?
python3 scripts/release-smoke.py release-output/earth-online-1.0.0.apk
release_status=$?
printf 'ACCEPTANCE_EXIT instrumentation=%s signed_apk=%s\n' "$instrumented_status" "$release_status"
if [ "$instrumented_status" -ne 0 ] || [ "$release_status" -ne 0 ]; then exit 1; fi
