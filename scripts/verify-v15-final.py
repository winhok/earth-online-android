#!/usr/bin/env python3
"""Add native capture-state evidence to the exact-binary release gate."""
import json
import os
from pathlib import Path
import re
import subprocess
import sys

packages,evidence,build=map(Path,sys.argv[1:4])
subprocess.run([sys.executable,str(Path(__file__).with_name('verify-v15-release.py')),str(packages),str(evidence),str(build)],check=True)
visuals={
 'v15-phone-cultivation-dark','v15-phone-cultivation-light',
 'v15-phone-cultivation-200pct-top','v15-phone-cultivation-200pct-action',
 'v15-tablet-cultivation-dark','v15-tablet-cultivation-light',
 'v15-tablet-rotated-draft','v15-phone-earth-light',
}
for api in (26,29,35,36):
    root=evidence/f'v15-release-api-{api}'/'verification/device/screenshots'
    for name in visuals:
        state=json.loads((root/f'{name}-capture-state.json').read_text())
        assert state['compose_expected_text_visible'] is True,(api,name)
        assert state['native_app_window_visible'] is True,(api,name)
        assert state['package']=='xyz.winhok.earthonline.debug',(api,name)
        assert state['settled_accessibility_idle_ms']>=700,(api,name)
        assert state['expected_text'],(api,name)
summary_path=packages/'acceptance-summary.json'
summary=json.loads(summary_path.read_text())
harness=os.environ['SOURCE_COMMIT']
assert re.fullmatch('[0-9a-f]{40}',harness)
assert (packages/'source-commit.txt').read_text().strip()==harness
summary['test_harness_commit']=harness
summary['signed_application_source_commit']=(packages/'source-commit.txt').read_text().strip()
summary['application_source_equivalence']='The signed app, test APK, core tests and device jobs all checked out the same recorded source commit.'
summary['native_capture_states_verified']=32
summary['visual_review']='Actual screenshots retained for completion review; no claim of human physical-device validation.'
summary_path.write_text(json.dumps(summary,ensure_ascii=False,indent=2))
print('32 native capture states verified in addition to all exact-APK and instrumentation gates.')
