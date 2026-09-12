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
    "Quickstep isn't responding", "Pixel Launcher isn't responding",
    "Launcher isn't responding", "System UI isn't responding",
    "Process system isn't responding",
)


def adb(*args: str, check: bool = True) -> str:
    result = subprocess.run(['adb', *args], capture_output=True, text=True, timeout=35)
    if check and result.returncode:
        raise RuntimeError(f'ADB {args[:3]} failed: {result.stderr[:600]}')
    return result.stdout


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
    with (OUT / 'interactions.jsonl').open('a') as trace:
        trace.write(json.dumps({'time': time.monotonic(), 'tap': n.attrib}, ensure_ascii=False) + '\n')
    adb('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))


def raw_dump() -> ET.Element:
    adb('shell', 'uiautomator', 'dump', '/sdcard/earth-window.xml')
    text = adb('shell', 'cat', '/sdcard/earth-window.xml')
    (OUT / 'last-window.xml').write_text(text)
    return ET.fromstring(text[text.index('<?xml'):])


def dump() -> ET.Element:
    root = raw_dump()
    visible = [text for n in root.iter('node') for text in labels(n) if text]
    title = next((text for text in visible if text in PLATFORM_ANR_TITLES), None)
    if title is not None:
        # Only recover a named emulator/launcher failure. Never dismiss this app's ANR.
        wait = [n for n in root.iter('node') if n.get('text') in ('Wait', '等待')]
        assert wait, f'Platform dialog has no Wait action: {title}'
        print('RELEASE_RECOVER_PLATFORM_DIALOG', title, flush=True)
        tap(wait[-1]); time.sleep(1)
        adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity', check=False)
        root = raw_dump()
    return root


def nodes(predicate, seconds: int = 25) -> list[ET.Element]:
    deadline = time.monotonic() + seconds
    last = None
    while time.monotonic() < deadline:
        try:
            found = [n for n in dump().iter('node') if predicate(n) and n.get('bounds') != '[0,0][0,0]']
            if found:
                return found
        except (ValueError, ET.ParseError, RuntimeError) as error:
            last = error
        time.sleep(.3)
    raise AssertionError(f'UI condition did not become true; last error={last}')


def wait_absent(predicate, seconds: int = 15) -> None:
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        if not any(predicate(n) for n in dump().iter('node')):
            return
        time.sleep(.25)
    raise AssertionError('UI condition did not disappear')


def field_for_label(label: str) -> ET.Element:
    deadline = time.monotonic() + 25
    while time.monotonic() < deadline:
        root = dump()
        names = [n for n in root.iter('node') if is_label(label)(n)]
        edits = [n for n in root.iter('node') if n.get('class') == 'android.widget.EditText' and n.get('bounds') != '[0,0][0,0]']
        for name in names:
            x1, y1, x2, y2 = bounds(name)
            cx, cy = (x1+x2)//2, (y1+y2)//2
            containing = [n for n in edits if bounds(n)[0] <= cx <= bounds(n)[2] and bounds(n)[1] <= cy <= bounds(n)[3]]
            if containing:
                return min(containing, key=lambda n: bounds(n)[1])
        if names and edits:
            return min(edits, key=lambda n: (bounds(n)[1], bounds(n)[0]))
        time.sleep(.3)
    raise AssertionError(f'Editable for {label!r} unavailable')


def ime_visible(windows: str) -> bool:
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
        # A second BACK could navigate out of the app. Never send it blindly.
        print('RELEASE_WARN keyboard_visibility_signal_stale', flush=True)


def is_checkable_label(value: str):
    return lambda n: n.get('checkable') == 'true' and n.get('clickable') == 'true' and any(
        is_label(value)(child) for child in n.iter('node')
    )


def scroll_to(value: str, predicate=None) -> ET.Element:
    matches = predicate if predicate is not None else is_label(value)
    for forward in (True, False):
        for _ in range(12):
            # First let the new window appear; an IME may arrive after the initial tap.
            dump()
            hide_keyboard()
            root = dump()
            w, h = map(int, re.findall(r'(\d+)x(\d+)', adb('shell', 'wm', 'size'))[-1])
            found = [n for n in root.iter('node') if matches(n) and
                     n.get('bounds') != '[0,0][0,0]' and
                     0 <= bounds(n)[1] < bounds(n)[3] <= h - 100]
            if found:
                return found[-1]
            scrolling = [n for n in root.iter('node') if n.get('scrollable') == 'true' and n.get('package') == PACKAGE]
            assert scrolling, f'No scrollable app content while searching for {value}'
            container = max(scrolling, key=lambda n: (bounds(n)[2]-bounds(n)[0])*(bounds(n)[3]-bounds(n)[1]))
            x1, y1, x2, y2 = bounds(container)
            x = min(w-12, x2-12)
            upper = max(y1+24, int(h*.23))
            lower = min(y2-24, int(h*.72))
            assert lower > upper + 50, (container.attrib, value)
            start_y, end_y = (lower, upper) if forward else (upper, lower)
            adb('shell', 'input', 'swipe', str(x), str(start_y), str(x), str(end_y), '350')
    raise AssertionError(f'Could not scroll to {value}')


def click(value: str, scroll: bool = False) -> None:
    print('RELEASE_UI_CLICK', value, flush=True)
    node = scroll_to(value) if scroll else max(nodes(is_label(value)), key=lambda n: bounds(n)[3])
    tap(node)


def screenshot(name: str) -> None:
    with (OUT / f'{name}.png').open('wb') as stream:
        subprocess.run(['adb', 'exec-out', 'screencap', '-p'], stdout=stream, check=True, timeout=20)


def ok(name: str) -> None:
    results.append(name)
    print('RELEASE_PASS', name, flush=True)


def enter_text(field: ET.Element, text: str) -> None:
    tap(field)
    nodes(lambda n: n.get('class') == 'android.widget.EditText' and n.get('focused') == 'true')
    adb('shell', 'input', 'text', text)
    nodes(lambda n: n.get('class') == 'android.widget.EditText' and n.get('text') == text)


def create_task(title: str) -> None:
    click('接取任务', scroll=True)
    nodes(is_label('接取新任务'))
    enter_text(field_for_label('任务标题'), title)
    hide_keyboard(); click('保存')
    wait_absent(is_label('任务标题'))
    nodes(is_label('任务日志'))
    click('进行中', scroll=True); scroll_to(title)


def main() -> None:
    apk = Path(sys.argv[1]); assert apk.is_file()
    (OUT / 'apk-sha256.txt').write_text(hashlib.sha256(apk.read_bytes()).hexdigest() + '\n')
    adb('install', '--no-streaming', str(apk))
    ok('exact_signed_apk_installs')
    adb('shell', 'svc', 'wifi', 'disable', check=False)
    adb('shell', 'svc', 'data', 'disable', check=False)
    adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity')
    nodes(is_label('地球 Online'))
    enter_text(field_for_label('玩家名'), 'ReleaseTester')
    hide_keyboard(); click('创建本地角色', scroll=True)
    nodes(is_label('指挥台')); ok('offline_onboarding'); screenshot('01-dashboard')
    click('任务'); create_task('ReleaseSmokeTask'); ok('create_task')
    click('完成任务：ReleaseSmokeTask'); click('已完成', scroll=True)
    nodes(is_label('ReleaseSmokeTask')); click('ReleaseSmokeTask')
    nodes(is_label('撤销本次完成')); ok('completion_is_persisted')
    click('撤销本次完成'); nodes(is_label('确认完成'))
    click('关闭'); click('进行中', scroll=True); nodes(is_label('ReleaseSmokeTask'))
    ok('undo_restores_active_task'); screenshot('02-quests')
    adb('shell', 'am', 'force-stop', PACKAGE)
    adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity')
    nodes(is_label('指挥台')); click('任务'); nodes(is_label('ReleaseSmokeTask'))
    ok('force_stop_and_relaunch_keep_data')
    for label, filename in [('角色', '03-character'), ('日志', '04-journal')]:
        click(label); screenshot(filename)
    click('设置与存档'); nodes(is_label('玩家设置')); screenshot('05-settings')
    # This harness only runs on a disposable emulator. Remove its earlier exports so
    # a repeated verification cannot collide with the system picker's default name.
    adb('shell', 'rm', '-f', '/sdcard/Download/earth-online-*.json')
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
    assert payload['player']['name'] == 'ReleaseTester' and len(payload['quests']) == 1
    ok('real_SAF_export_contains_saved_data')
    click('关闭编辑'); click('任务'); create_task('MustDisappearAfterRestore')
    nodes(is_label('MustDisappearAfterRestore'))
    click('设置与存档'); click('导入存档', scroll=True)
    tap(nodes(lambda n: n.get('text') == filename)[0])
    nodes(is_label('覆盖本机存档？')); click('确认覆盖')
    nodes(is_label('任务')); click('任务'); scroll_to('ReleaseSmokeTask')
    assert not any(is_label('MustDisappearAfterRestore')(n) for n in dump().iter('node'))
    ok('real_SAF_restore_replaces_only_after_confirmation'); screenshot('06-after-restore')
    crashes = adb('logcat', '-d', '-b', 'crash')
    (OUT / 'crash-log.txt').write_text(crashes)
    assert PACKAGE not in crashes, 'Release app crash appears in crash buffer'
    ok('no_app_crash_in_test_session')


if __name__ == '__main__':
    try:
        main()
    except Exception:
        screenshot('failure')
        (OUT / 'failure.txt').write_text(traceback.format_exc())
        raise
    finally:
        (OUT / 'results.json').write_text(json.dumps({'passed_checks': results}, indent=2))
