#!/usr/bin/env python3
"""Exercise the exact signed APK through physical UI actions, including a real v1 upgrade.

Only disposable emulator data is used. Timeline fixtures enter through the public SAF
import UI; tests never write the production database or instrument a release build.
"""
from __future__ import annotations
import hashlib
import importlib.util
import json
import re
import subprocess
import sys
import time
import traceback
from pathlib import Path

spec=importlib.util.spec_from_file_location('release_ui',Path(__file__).with_name('release-smoke.py'))
ui=importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(ui)
OUT=Path('verification/v15-release');OUT.mkdir(parents=True,exist_ok=True)
ui.OUT=OUT
checks=[]
PACKAGE=ui.PACKAGE
CONFIRM='理解上述数值并确认'
ACK='已核对，继续行动'


def ok(name):
    checks.append(name);print('V15_PASS',name,flush=True)


def has(text):
    return any(ui.is_label(text)(n) for n in ui.dump().iter('node'))


def start():
    ui.adb('shell','am','start','-W','-n',PACKAGE+'/.MainActivity')


def restart():
    ui.adb('shell','am','force-stop',PACKAGE);start()


def settings():
    if not has('玩家设置'):
        ui.click('设置与存档');ui.nodes(ui.is_label('玩家设置'))


def pick_file(name):
    for attempt in range(12):
        root=ui.dump()
        found=[n for n in root.iter('node') if n.get('text')==name]
        if found: ui.tap(found[-1]);return
        if attempt == 0:
            roots=[n for n in root.iter('node') if any(x in ('Show roots','显示根目录','显示根目录列表') for x in ui.labels(n))]
            if roots: ui.tap(roots[0])
            downloads=ui.nodes(lambda n:n.get('text') in ('Downloads','下载'),seconds=8)
            ui.tap(downloads[-1])
            continue
        w,h=map(int,re.findall(r'(\d+)x(\d+)',ui.adb('shell','wm','size'))[-1])
        ui.adb('shell','input','swipe',str(w//2),str(int(h*.78)),str(w//2),str(int(h*.28)),'300')
    raise AssertionError('Document picker could not find '+name)


def restore_file(file:Path,*,cancel=False,overdue=False):
    assert file.is_file()
    ui.adb('push',str(file),'/sdcard/Download/'+file.name)
    ui.adb('shell','am','broadcast','-a','android.intent.action.MEDIA_SCANNER_SCAN_FILE','-d','file:///sdcard/Download/'+file.name,check=False)
    settings();ui.click('导入存档',scroll=True)
    pick_file(file.name)
    ui.nodes(ui.is_label('覆盖本机存档？'))
    if cancel:
        ui.click('取消');return
    ui.click('确认覆盖')
    ui.wait_absent(ui.is_label('覆盖本机存档？'))
    if overdue: ui.nodes(ui.is_label(ACK))
    else: ui.nodes(lambda n:any(t in ('任务','历练','指挥台','洞天') for t in ui.labels(n)))
    # Import may leave the settings window open; closing is an explicit user action.
    if not overdue and has('玩家设置'):ui.click('关闭编辑')


def export_file(stage):
    name='earth-online-'+stage+'.json'
    def exported():
        return set(ui.adb('shell','find','/sdcard/Download','-maxdepth','1','-type','f',
            '-name','earth-online-*.json').strip().splitlines())
    before=exported()
    settings();ui.click('导出存档',scroll=True)
    save=ui.nodes(lambda n:n.get('text','').upper()=='SAVE' or n.get('text')=='保存')[-1]
    ui.tap(save)
    ui.nodes(lambda n:any(t.startswith('存档已导出') for t in ui.labels(n)))
    deadline=time.monotonic()+10
    created=set()
    while time.monotonic()<deadline:
        created=exported()-before
        if len(created)==1:break
        time.sleep(.2)
    assert len(created)==1,created
    remote=created.pop()
    file=OUT/name
    ui.adb('pull',remote,str(file))
    envelope=json.loads(file.read_text())
    assert hashlib.sha256(envelope['payload'].encode()).hexdigest()==envelope['sha256']
    data=json.loads(envelope['payload'])
    ui.click('关闭编辑')
    return data,file


def facts(data):
    return {k:data.get(k,[]) for k in ('quests','goals','completions','contracts','contractRevisions',
        'consequences','consequenceAdjustments','repaymentAllocations','recoveryRoutes','recoveryNodes')}


def net(data):
    rewards=sum(c['xp'] for c in data['completions'] if c['revokedAt'] is None)
    cost=sum(c['xp'] for c in data['consequences'])
    credit=sum(a['xp'] for a in data['consequenceAdjustments'] if a['kind'] in ('WAIVER','RESTITUTION'))
    return rewards-cost+credit


def legacy_fixture():
    now=int(time.time()*1000)-2*86400000;day=now//86400000
    p=dict(name='ReleaseTester',server='Verification',zoneId='UTC',joinedAt=now,onboarded=True,
        theme='LIGHT',remindersEnabled=False,reminderHour=20,lastReminderDay=None)
    def quest(id,due=None):
        return dict(id=id,title=id,description='Synthetic verification data',kind='SIDE',difficulty='NORMAL',
            skill='DISCIPLINE',priority=2,estimatedMinutes=25,dueDay=due,goalId=None,state='ACTIVE',
            snoozedUntilDay=None,postponeCount=0,createdAt=now,updatedAt=now)
    qs=[quest('LegacyDate',day),quest('LegacyReady'),quest('LegacyDone')]
    completion=dict(id='LegacyDone:once',questId='LegacyDone',occurrence='once',title='LegacyDone',
        kind='SIDE',skill='DISCIPLINE',xp=25,completedAt=now,completedDay=day,revokedAt=None)
    raw=json.dumps(dict(player=p,goals=[],quests=qs,completions=[completion],events=[]),separators=(',',':'))
    file=OUT/'v1-legacy.json'
    file.write_text(json.dumps(dict(format='earth-online-backup',version=1,exportedAt=now,
        sha256=hashlib.sha256(raw.encode()).hexdigest(),payload=raw)))
    return file


def open_task(title):
    ui.click('任务');ui.click('进行中');ui.click(title,scroll=True)


def main():
    apk,old_apk=map(Path,sys.argv[1:3])
    assert apk.is_file() and old_apk.is_file()
    (OUT/'apk-sha256.txt').write_text(hashlib.sha256(apk.read_bytes()).hexdigest()+'\n')
    assert hashlib.sha256(old_apk.read_bytes()).hexdigest()=='e48fb969889aec5df9e80af93564b1bcf0a335f1b3a3c3fbd1146295651563a4'
    # The published 1.0 UI is exercised before replacing its APK, never its app data.
    subprocess.run([sys.executable,str(Path(__file__).with_name('release-smoke.py')),str(old_apk)],check=True)
    ok('v1_installed_and_exercised')
    restore_file(legacy_fixture())
    before,_=export_file('before-upgrade')
    assert len(before['quests'])==3 and before['completions'][0]['xp']==25
    ok('v1_legacy_save_exported')
    ui.adb('shell','am','force-stop',PACKAGE)
    install=ui.adb('install','--no-streaming','-r',str(apk))
    assert 'Success' in install
    (OUT/'upgrade-install.txt').write_text(install)
    package=ui.adb('shell','dumpsys','package',PACKAGE)
    assert 'versionName=1.5.0' in package and re.search(r'versionCode=2\b',package)
    (OUT/'installed-package.txt').write_text(package)
    start();ui.nodes(ui.is_label('任务'))
    ok('same_signature_in_place_upgrade')
    after,_=export_file('after-upgrade')
    for key in ('quests','goals','completions'):assert after[key]==before[key],key
    assert after['player']['name']==before['player']['name']
    assert not after['contracts'] and not after['consequences']
    ok('v1_data_preserved_unsigned')
    ui.adb('shell','svc','wifi','disable',check=False);ui.adb('shell','svc','data','disable',check=False)
    ui.click('任务');ui.create_task('UpgradeAction')
    ui.click('完成任务：UpgradeAction',scroll=True);ui.click('已完成');ui.click('UpgradeAction')
    ui.click('撤销本次完成',scroll=True);ui.nodes(ui.is_label('确认完成'));ui.click('关闭')
    action,_=export_file('ordinary-action')
    assert len([c for c in action['completions'] if c['title']=='UpgradeAction'])==1
    assert next(c for c in action['completions'] if c['title']=='UpgradeAction')['revokedAt'] is not None
    ok('offline_new_task_complete_undo')
    settings();ui.click('修仙 · 宗门战令',scroll=True);ui.click('关闭编辑')
    ui.nodes(ui.is_label('洞天'));ui.click('洞天');ui.screenshot('01-cultivation-dashboard')
    switched,_=export_file('cultivation')
    assert facts(switched)==facts(action)
    settings();ui.click('地球原生',scroll=True);ui.click('关闭编辑');ui.nodes(ui.is_label('指挥台'))
    ok('narrative_switch_keeps_domain_facts')

    fixtures=Path('verification/device/screenshots')
    active=fixtures/'v15-active.json';overdue=fixtures/'v15-overdue.json'
    restore_file(active,cancel=True)
    ui.click('关闭编辑')
    kept,_=export_file('cancelled-restore');assert facts(kept)==facts(action)
    ok('cancelled_import_keeps_original')
    restore_file(active)
    initial,_=export_file('signed-initial')
    assert len(initial['contracts'])==1 and initial['contracts'][0]['currentRewardXp']==25
    ok('signed_deadline_imported')
    open_task('SignedPromise');ui.click('延期一天（先查看奖励变化）',scroll=True)
    ui.nodes(ui.is_label(CONFIRM));ui.click('取消');ui.click('关闭')
    cancelled,_=export_file('cancelled-extension');assert facts(cancelled)==facts(initial)
    ok('extension_cancel_has_no_effect')
    open_task('SignedPromise');ui.click('延期一天（先查看奖励变化）',scroll=True)
    ui.click(CONFIRM);ui.wait_absent(ui.is_label(CONFIRM));ui.click('关闭')
    extended,_=export_file('extended')
    c=extended['contracts'][0]
    assert c['extensionCount']==1 and c['currentRewardXp']==20
    assert c['dueDay']==initial['contracts'][0]['dueDay']+1
    ok('extension_changes_reward_and_date_once')
    open_task('SignedPromise');ui.click('暂停这项任务',scroll=True);ui.click(CONFIRM)
    ui.wait_absent(ui.is_label(CONFIRM));ui.click('关闭')
    abandoned,backup=export_file('abandoned')
    assert len(abandoned['consequences'])==1 and abandoned['consequences'][0]['xp']==13 and net(abandoned)==-13
    ok('abandonment_half_cost_once')
    restore_file(backup)
    restored,_=export_file('roundtrip');assert facts(restored)==facts(abandoned)
    ok('v3_backup_roundtrip')

    restore_file(overdue,overdue=True)
    ui.screenshot('02-overdue-batch')
    ui.adb('shell','input','keyevent','KEYCODE_BACK');ui.nodes(ui.is_label(ACK))
    ui.click(ACK);ui.wait_absent(ui.is_label(ACK))
    settled,_=export_file('settled')
    assert len(settled['consequences'])==3 and net(settled)==-75
    assert len(settled['recoveryRoutes'])==1 and settled['recoveryRoutes'][0]['status']=='OPEN'
    ok('offline_overdue_batch_settled_once')
    restart();ui.nodes(ui.is_label('任务'));assert not has(ACK)
    repeated,_=export_file('restart');assert facts(repeated)==facts(settled)
    assert len(repeated['settlementReceipts'])==3
    ok('batch_ack_survives_restart')
    ui.click('任务');ui.click('进行中');ui.click('完成任务：ExpiredPromise0',scroll=True)
    late,_=export_file('late');assert net(late)==-50 and len(late['repaymentAllocations'])==1
    alloc=late['repaymentAllocations'][0]
    own_contract=next(c for c in late['contracts'] if c['questId']=='ExpiredPromise0')
    own_cost=next(c for c in late['consequences'] if c['contractId']==own_contract['id'])
    assert alloc['consequenceId']==own_cost['id'] and alloc['xp']==25
    ok('late_completion_repays_own_debt')
    ui.click('任务');ui.click('已完成');ui.click('ExpiredPromise0');ui.click('撤销本次完成',scroll=True)
    ui.nodes(ui.is_label('确认完成'));ui.click('关闭')
    undone,_=export_file('undone');assert net(undone)==-75
    assert len(undone['repaymentAllocations'])==2 and undone['repaymentAllocations'][1]['reversalOf'] is not None
    ok('undo_restores_allocations_debt')
    open_task('ExpiredPromise0');ui.click('不可抗力 / 现实豁免',scroll=True)
    ui.click('健康原因');ui.click('确定');ui.click(CONFIRM)
    ui.wait_absent(ui.is_label(CONFIRM));ui.click('关闭')
    waived,_=export_file('waived');assert net(waived)==-50 and len(waived['consequences'])==3
    assert not [c for c in waived['completions'] if c['revokedAt'] is None]
    ok('force_majeure_preserves_assessment_no_reward')
    ui.click('查看债务及来源');ui.click('选择恢复任务',scroll=True)
    ui.click('ExpiredPromise1',scroll=True);ui.click('ExpiredPromise2',scroll=True)
    ui.click('确认选中任务',scroll=True)
    ui.click('完成任务：ExpiredPromise1',scroll=True)
    ui.click('完成任务：ExpiredPromise2',scroll=True)
    ui.click('关闭')
    recovered,_=export_file('recovered')
    assert net(recovered)==0 and len(recovered['quests'])==4
    assert recovered['recoveryRoutes'][0]['status']=='CLOSED'
    assert len(recovered['recoveryNodes'])==2 and all(n['completedAt'] is not None for n in recovered['recoveryNodes'])
    assert len([c for c in recovered['completions'] if c['revokedAt'] is None])==2
    ui.click('角色');ui.screenshot('03-recovered-profile')
    ok('recovery_uses_real_tasks_until_zero')
    crash=ui.adb('logcat','-d','-b','crash');(OUT/'crash-log.txt').write_text(crash)
    assert PACKAGE not in crash
    ok('no_release_crash')


if __name__=='__main__':
    try:main()
    except Exception:
        try:ui.screenshot('failure')
        except Exception:pass
        (OUT/'failure.txt').write_text(traceback.format_exc());raise
    finally:
        (OUT/'results.json').write_text(json.dumps(dict(passed_checks=checks),indent=2))
