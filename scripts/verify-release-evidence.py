#!/usr/bin/env python3
"""Reject missing, skipped, failed or mismatched release evidence before publishing."""
import hashlib
import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

packages, evidence = map(Path, sys.argv[1:3])
apk_sha = hashlib.sha256((packages / 'earth-online-1.0.0.apk').read_bytes()).hexdigest()
expected = {
    'exact_signed_apk_installs', 'offline_onboarding', 'create_task',
    'completion_is_persisted', 'undo_restores_active_task',
    'force_stop_and_relaunch_keep_data', 'real_SAF_export_contains_saved_data',
    'real_SAF_restore_replaces_only_after_confirmation', 'no_app_crash_in_test_session',
}
summary = {'apk_sha256': apk_sha, 'devices': []}
for api in (26, 29, 35, 36):
    root = evidence / f'release-device-api-{api}'
    reports = list((root / 'app/build/outputs/androidTest-results').rglob('TEST-*.xml'))
    assert reports, f'No instrumentation report for API {api}'
    cases = []
    for report in reports:
        suite = ET.parse(report).getroot()
        assert int(suite.get('failures', '0')) == 0 and int(suite.get('errors', '0')) == 0, report
        assert int(suite.get('skipped', '0')) == 0, report
        cases.extend(suite.findall('.//testcase'))
    assert len(cases) == 20, f'Expected 20 actual tests on API {api}, got {len(cases)}'
    assert all(not any(c.find(tag) is not None for tag in ('failure', 'error', 'skipped')) for c in cases)
    signed = root / 'verification/release-device'
    actual = json.loads((signed / 'results.json').read_text())['passed_checks']
    assert set(actual) == expected and len(actual) == len(expected), f'Incomplete signed APK journey on API {api}'
    assert (signed / 'apk-sha256.txt').read_text().strip() == apk_sha, f'Wrong APK tested on API {api}'
    summary['devices'].append({'api': api, 'instrumented_tests': len(cases), 'signed_apk_checks': actual})
(packages / 'acceptance-summary.json').write_text(json.dumps(summary, indent=2))
print('Verified 80 instrumentation executions and 36 signed-APK checks against one APK SHA-256.')
