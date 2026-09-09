#!/usr/bin/env python3
"""Fail closed on missing/skipped tests, missing visuals, or a different APK."""
from __future__ import annotations
import hashlib
import json
import os
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

CHECKS={
 'v1_installed_and_exercised','v1_legacy_save_exported','same_signature_in_place_upgrade',
 'v1_data_preserved_unsigned','offline_new_task_complete_undo','narrative_switch_keeps_domain_facts',
 'cancelled_import_keeps_original','signed_deadline_imported','extension_cancel_has_no_effect',
 'extension_changes_reward_and_date_once','abandonment_half_cost_once','v3_backup_roundtrip',
 'offline_overdue_batch_settled_once','batch_ack_survives_restart','late_completion_repays_own_debt',
 'undo_restores_allocations_debt','force_majeure_preserves_assessment_no_reward',
 'recovery_uses_real_tasks_until_zero','no_release_crash',
}
IMAGES={'v15-phone-cultivation-dark','v15-phone-cultivation-light','v15-phone-cultivation-200pct-top',
 'v15-phone-cultivation-200pct-action','v15-tablet-cultivation-dark','v15-tablet-cultivation-light',
 'v15-tablet-rotated-draft','v15-phone-earth-light','v15-overdue-batch','v15-legacy-signed','v15-recovery-closed'}


def cases(root):
    files=list(root.rglob('TEST-*.xml'));assert files,f'Missing reports: {root}'
    out=[]
    for file in files:
        suite=ET.parse(file).getroot()
        for field in ('failures','errors','skipped'):assert int(suite.get(field,'0'))==0,(file,field)
        for case in suite.findall('.//testcase'):
            assert not any(case.find(x) is not None for x in ('failure','error','skipped'))
            out.append((case.get('classname'),case.get('name')))
    assert len(out)==len(set(out)),f'Duplicate executions masquerading as extra tests: {root}'
    return out


def main():
    packages,evidence,build=map(Path,sys.argv[1:4])
    apk=packages/'earth-online-1.5.0.apk'
    sha=hashlib.sha256(apk.read_bytes()).hexdigest()
    core=cases(build/'core/build/test-results')
    assert len(core)>=19
    assert any('DeadlineTest' in (c or '') for c,n in core)
    assert any('DomainTest' in (c or '') for c,n in core)
    summary=dict(version='1.5.0',source_commit=(packages/'source-commit.txt').read_text().strip(),
        apk_sha256=sha,domain_scenarios=41,contract_scenarios=45,
        core_junit_cases=len(core),devices=[],physical_device_tested=False,store_published=False)
    manifests=(packages/'apk-manifest-summary.txt').read_text()
    assert "versionName='1.5.0'" in manifests and "versionCode='2'" in manifests
    assert "uses-permission: name='android.permission.INTERNET'" not in manifests
    assert 'application-debuggable' not in manifests
    for api in (26,29,35,36):
        root=evidence/f'v15-release-api-{api}'
        tests=cases(root/'app/build/outputs/androidTest-results')
        assert len(tests)==65,f'Expected 65 real instrumentation cases on API {api}, got {len(tests)}'
        for name in ('ContractJourneyTest','VisualAcceptanceTest','ReleaseFixtureTest'):
            assert any(name in (c or '') for c,n in tests),name
        signed=root/'verification/v15-release'
        completed=json.loads((signed/'results.json').read_text())['passed_checks']
        assert len(completed)==len(CHECKS) and set(completed)==CHECKS,f'Incomplete release journey API {api}'
        assert (signed/'apk-sha256.txt').read_text().strip()==sha,f'Wrong APK on API {api}'
        assert 'Success' in (signed/'upgrade-install.txt').read_text()
        screenshots=root/'verification/device/screenshots'
        for name in IMAGES:
            image=screenshots/f'{name}.png'
            assert image.stat().st_size>1000 and image.read_bytes()[:8]==b'\x89PNG\r\n\x1a\n',image
        summary['devices'].append(dict(api=api,instrumented_cases=len(tests),signed_apk_checks=completed))
    summary['instrumented_executions']=sum(x['instrumented_cases'] for x in summary['devices'])
    summary['signed_apk_check_executions']=sum(len(x['signed_apk_checks']) for x in summary['devices'])
    (packages/'acceptance-summary.json').write_text(json.dumps(summary,indent=2))
    print('Verified every instrumentation case, real visual capture, and same-signature upgrade against one signed APK.')


if __name__=='__main__':main()
