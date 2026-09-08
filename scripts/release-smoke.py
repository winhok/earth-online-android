#!/usr/bin/env python3
"""Black-box checks against the exact signed release APK on a disposable emulator."""
from __future__ import annotations
import hashlib
import json
import re
import subprocess
import sys
import time
import traceback
import xml.etree.ElementTree as ET
from pathlib import Path

PACKAGE = 'xyz.winhok.earthonline'
OUT = Path('verification/release-device')
OUT.mkdir(parents=True, exist_ok=True)
results: list[str] = []
PLATFORM_ANR_TITLES = (
    "Quickstep isn't responding",
    "Pixel Launcher isn't responding",
    "Launcher isn't responding",
    "System UI isn't responding",
)


def adb(*args: str, check: bool = True) -> str:
    result = subprocess.run(['adb', *args], capture_output=True, text=True, timeout=35)
    if check and result.returncode:
        raise RuntimeError(f'ADB {args[:3]} failed: {result.stderr[:600]}')
    return result.stdout


def raw_dump() -> tuple[ET.Element, str]:
    adb('shell', 'uiautomator', 'dump', '/sdcard/earth-window.xml')
    text = adb('shell', 'cat', '/sdcard/earth-window.xml')
    (OUT / 'last-window.xml').write_text(text)
    return ET.fromstring(text[text.index('<?xml'):]), text


def labels(n: ET.Element) -> list[str]:
    return [n.get('text', ''), n.get('content-desc', '')]


def is_label(value: str):
    return lambda n: value in labels(n)


def bounds(n: ET.Element) -> tuple[int, int, int, int]:
    coords = list(map(int, re.findall(r'\d+', n.get('bounds', ''))))
    assert len(coords) == 4, n.attrib
    return tuple(coords)  # type: ignore[return-value]


def tap(n: ET.Element) -> None:
    x1, y1, x2, y2 = bounds(n)
    assert n.get('enabled', 'true') == 'true', n.attrib
    adb('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))


def dismiss_platform_dialog(root: ET.Element) -> bool:
    """Dismiss emulator/launcher ANRs, but never hide an Earth Online crash/ANR."""
    visible = [text for n in root.iter('node') for text in labels(n) if text]
    title = next((text for text in visible if text in PLATFORM_ANR_TITLES), None)
    if title is None:
        return False
    wait = [n for n in root.iter('node') if n.get('text') in ('Wait', '等待')]
    if not wait:
        raise AssertionError(f'Platform ANR is blocking the app and has no Wait action: {title}')
    print('RELEASE_RECOVER_PLATFORM_DIALOG', title, flush=True)
    tap(wait[-1])
    time.sleep(1.0)
    adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity', check=False)
    time.sleep(.5)
    return True


def dump() -> ET.Element:
    root, _ = raw_dump()
    if dismiss_platform_dialog(root):
        root, _ = raw_dump()
    return root


def nodes(predicate, seconds: int = 25) -> list[ET.Element]:
    deadline = time.monotonic() + seconds
    last = None
    while time.monotonic() < deadline:
        try:
            root = dump()
            found = [n for n in root.iter('node') if predicate(n) and n.get('bounds') != '[0,0][0,0]']
            if found:
                return found
        except (ValueError, ET.ParseError, RuntimeError) as error:
            last = error
        time.sleep(.3)
    raise AssertionError(f'UI condition did not become true; last error={last}')


def wait_absent(predicate, seconds: int = 10) -> None:
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if not any(predicate(n) for n in dump().iter('node')):
            return
        time.sleep(.25)
    raise AssertionError('UI condition did not disappear')


def field_for_label(label: str, seconds: int = 25) -> ET.Element:
    """Choose the editable that geometrically contains its Material field label."""
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        root = dump()
        labels_found = [n for n in root.iter('node') if is_label(label)(n)]
        edits = [n for n in root.iter('node') if n.get('class') == 'android.widget.EditText' and n.get('bounds') != '[0,0][0,0]']
        for label_node in labels_found:
            lx1, ly1, lx2, ly2 = bounds(label_node)
            cx, cy = (lx1 + lx2) // 2, (ly1 + ly2) // 2
            containing = []
            for edit in edits:
                x1, y1, x2, y2 = bounds(edit)
                if x1 <= cx <= x2 and y1 <= cy <= y2:
                    containing.append(edit)
            if containing:
                return min(containing, key=lambda n: bounds(n)[1])
        if labels_found and edits:
            return min(edits, key=lambda n: (bounds(n)[1], bounds(n)[0]))
        time.sleep(.3)
    raise AssertionError(f'Editable for label {label!r} did not become available')


def ime_visible(windows: str) -> bool:
    # InputMethodManager dumps include historical visibility events. Only a current
    # input-method window with a surface/on-screen signal is safe to dismiss.
    for block in re.split(r'\n\s*Window #\d+ ', windows)[1:]:
        if 'InputMethod' not in block.splitlines()[0]:
            continue
        if re.search(r'\bmViewVisibility=0x0\b', block) and (
            'isOnScreen=true' in block or 'isVisible=true' in block or 'mHasSurface=true' in block
        ):
            return True
    return False


def hide_keyboard() -> None:
    windows = adb('shell', 'dumpsys', 'window', 'windows')
    (OUT / 'last-keyboard-windows.txt').write_text(windows)
    if ime_visible(windows):
        adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')
        deadline = time.monotonic() + 5
        while time.monotonic() < deadline:
            if not ime_visible(adb('shell', 'dumpsys', 'window', 'windows')):
                return
            time.sleep(.2)
        print('RELEASE_WARN keyboard_visibility_signal_stale', flush=True)


def click(value: str, scroll: bool = False) -> None:
    print('RELEASE_UI_CLICK', value, flush=True)
    if scroll:
        hide_keyboard()
        for _ in range(7):
            root = dump()
            found = [n for n in root.iter('node') if is_label(value)(n)]
            if found:
                tap(found[-1]); return
            size = re.findall(r'(\d+)x(\d+)', adb('shell', 'wm', 'size'))[-1]
            w, h = map(int, size)
            adb('shell', 'input', 'swipe', str(w//2), str(int(h*.78)), str(w//2), str(int(h*.38)), '300')
        raise AssertionError(f'Could not scroll to {value}')
    candidates = nodes(is_label(value))
    tap(max(candidates, key=lambda n: bounds(n)[3]))


def screenshot(name: str) -> None:
    with (OUT / f'{name}.png').open('wb') as stream:
        subprocess.run(['adb', 'exec-out', 'screencap', '-p'], stdout=stream, check=True, timeout=20)


def ok(name: str) -> None:
    results.append(name)
    print('RELEASE_PASS', name, flush=True)


def enter_text(field: ET.Element, text: str) -> None:
    tap(field)
    adb('shell', 'input', 'text', text)


def ensure_task_visible(title: str) -> None:
    # Saving is asynchronous. Require the editor to close and the task list to settle
    # before asserting persistence; never turn a failed save into a passing test.
    wait_absent(is_label('任务标题'), seconds=15)
    nodes(is_label('任务日志'), seconds=15)
    click('进行中')
    nodes(is_label(title), seconds=20)


def create_task(title: str) -> None:
    click('接取任务')
    nodes(is_label('接取新任务'))
    field = field_for_label('任务标题')
    enter_text(field, title)
    hide_keyboard()
    click('保存')
    ensure_task_visible(title)


def main() -> None:
    apk = Path(sys.argv[1])
    assert apk.is_file()
    (OUT / 'apk-sha256.txt').write_text(hashlib.sha256(apk.read_bytes()).hexdigest() + '\n')
    adb('install', '--no-streaming', str(apk))
    ok('exact_signed_apk_installs')
    adb('shell', 'svc', 'wifi', 'disable', check=False)
    adb('shell', 'svc', 'data', 'disable', check=False)
    adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity')
    nodes(is_label('地球 Online'))
    field = field_for_label('玩家名')
    enter_text(field, 'ReleaseTester')
    hide_keyboard()
    click('创建本地角色', scroll=True)
    nodes(is_label('指挥台'))
    ok('offline_onboarding')
    screenshot('01-dashboard')

    click('任务')
    create_task('ReleaseSmokeTask')
    ok('create_task')
    click('完成任务：ReleaseSmokeTask')
    click('已完成')
    nodes(is_label('ReleaseSmokeTask'))
    click('ReleaseSmokeTask')
    nodes(is_label('撤销本次完成'))
    ok('completion_is_persisted')
    click('撤销本次完成')
    nodes(is_label('确认完成'))
    click('关闭'); click('进行中')
    nodes(is_label('ReleaseSmokeTask'))
    ok('undo_restores_active_task')
    screenshot('02-quests')

    adb('shell', 'am', 'force-stop', PACKAGE)
    adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity')
    nodes(is_label('指挥台')); click('任务'); nodes(is_label('ReleaseSmokeTask'))
    ok('force_stop_and_relaunch_keep_data')
    for label, filename in [('角色', '03-character'), ('日志', '04-journal')]:
        click(label); screenshot(filename)

    click('设置与存档')
    nodes(is_label('玩家设置'))
    screenshot('05-settings')
    click('导出存档', scroll=True)
    save = nodes(lambda n: n.get('text', '').upper() == 'SAVE' or n.get('text') == '保存')[-1]
    tap(save)
    nodes(lambda n: any(t.startswith('存档已导出') for t in labels(n)))
    files = adb('shell', 'find', '/sdcard/Download', '-name', 'earth-online-*.json').strip().splitlines()
    assert files, 'System document export did not create a backup'
    remote = files[-1].strip(); filename = remote.rsplit('/', 1)[-1]
    adb('pull', remote, str(OUT / 'exported-test-save.json'))
    envelope = json.loads((OUT / 'exported-test-save.json').read_text())
    payload = json.loads(envelope['payload'])
    assert payload['player']['name'] == 'ReleaseTester'
    assert len(payload['quests']) == 1
    ok('real_SAF_export_contains_saved_data')

    click('关闭编辑')
    click('任务')
    create_task('MustDisappearAfterRestore')
    nodes(is_label('MustDisappearAfterRestore'))
    click('设置与存档')
    click('导入存档', scroll=True)
    chosen = nodes(lambda n: n.get('text') == filename)
    tap(chosen[0])
    nodes(is_label('覆盖本机存档？'))
    click('确认覆盖')
    nodes(is_label('任务')); click('任务')
    nodes(is_label('ReleaseSmokeTask'))
    assert not any(is_label('MustDisappearAfterRestore')(n) for n in dump().iter('node'))
    ok('real_SAF_restore_replaces_only_after_confirmation')
    screenshot('06-after-restore')

    crashes = adb('logcat', '-d', '-b', 'crash')
    (OUT / 'crash-log.txt').write_text(crashes)
    assert PACKAGE not in crashes, 'Release app crash appears in crash buffer'
    ok('no_app_crash_in_test_session')


try:
    main()
except Exception:
    screenshot('failure')
    (OUT / 'failure.txt').write_text(traceback.format_exc())
    raise
finally:
    (OUT / 'results.json').write_text(json.dumps({'passed_checks': results}, indent=2))
