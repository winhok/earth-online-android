#!/usr/bin/env bash
set -uo pipefail
mkdir -p verification/device
# Capture evidence even when a test fails; preserve the real Gradle exit status.
gradle :app:connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true --stacktrace > verification/device/instrumentation.log 2>&1
status=$?
adb pull /sdcard/Android/data/xyz.winhok.earthonline.debug/files/acceptance verification/device/screenshots >/dev/null 2>&1 || true
adb logcat -d -b crash > verification/device/crash-log.txt || true
python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
for p in Path('app/build/outputs/androidTest-results').rglob('TEST-*.xml'):
    root = ET.parse(p).getroot()
    print('TEST_RESULT', p.name, root.attrib)
    for case in root.findall('.//testcase'):
        for failure in list(case.findall('failure')) + list(case.findall('error')):
            print('FAILED', case.get('classname'), case.get('name'), failure.get('message'), (failure.text or '')[:4000])
PY
if [ "$status" -ne 0 ]; then tail -n 120 verification/device/instrumentation.log; fi
exit "$status"
