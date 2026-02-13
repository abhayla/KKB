# ADB Interaction Patterns Reference

13 reusable ADB patterns for manual E2E testing via uiautomator. Referenced by `/adb-test` Skill.

**Constants:**
```
ADB = C:/Users/itsab/AppData/Local/Android/Sdk/platform-tools/adb.exe
SCREENSHOT_DIR = docs/testing/screenshots
LOG_DIR = .claude/logs/adb-test
APP_PACKAGE = com.rasoiai.app
```

---

## Pattern 1: UI Dump — Capture current UI hierarchy

```bash
$ADB shell uiautomator dump //sdcard/window_dump.xml && $ADB pull //sdcard/window_dump.xml /tmp/window_dump.xml
```

Then parse the XML with Python (MUST set `PYTHONIOENCODING=utf-8`):
```bash
PYTHONIOENCODING=utf-8 python -c "
import xml.etree.ElementTree as ET
tree = ET.parse('/tmp/window_dump.xml')
for node in tree.iter('node'):
    text = node.get('text', '')
    desc = node.get('content-desc', '')
    cls = node.get('class', '')
    bounds = node.get('bounds', '')
    rid = node.get('resource-id', '')
    if text or desc:
        print(f'text={text!r} desc={desc!r} class={cls} bounds={bounds} id={rid}')
"
```

---

## Pattern 2: Screenshot — Capture current screen

```bash
$ADB exec-out screencap -p > $SCREENSHOT_DIR/{name}.png
```

Screenshots are auto-resized by the existing PostToolUse hook (`.claude/hooks/post-screenshot-resize.sh`).

---

## Pattern 3: Tap — Click at coordinates

```bash
$ADB shell input tap {cx} {cy}
```

Where `{cx}` and `{cy}` are the center of the element's bounds.

---

## Pattern 4: Text Input — Type text into focused field

```bash
$ADB shell input text "{text}"
```

For special characters, use `$ADB shell input keyevent {keycode}`.

---

## Pattern 5: Back Press — Navigate back

```bash
$ADB shell input keyevent BACK
```

---

## Pattern 6: Parse Bounds — Extract center coordinates from bounds string

```bash
python -c "
bounds = '[100,200][300,400]'  # example
import re
m = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', bounds)
left, top, right, bottom = int(m.group(1)), int(m.group(2)), int(m.group(3)), int(m.group(4))
cx, cy = (left + right) // 2, (top + bottom) // 2
print(f'Tap: $ADB shell input tap {cx} {cy}')
"
```

---

## Pattern 7: Find Element — Search XML by text, content-desc, or resource-id

```bash
PYTHONIOENCODING=utf-8 python -c "
import xml.etree.ElementTree as ET
tree = ET.parse('/tmp/window_dump.xml')
target = 'Home'  # search value
for node in tree.iter('node'):
    text = node.get('text', '')
    desc = node.get('content-desc', '')
    rid = node.get('resource-id', '')
    if target.lower() in text.lower() or target.lower() in desc.lower() or target.lower() in rid.lower():
        bounds = node.get('bounds', '')
        print(f'FOUND: text={text!r} desc={desc!r} bounds={bounds} id={rid}')
"
```

---

## Pattern 8: Scroll and Redump — Scroll down, re-capture XML

```bash
# Scroll down (swipe up gesture) — repeat up to 3 times
$ADB shell input swipe 540 1800 540 600 500
sleep 1
$ADB shell uiautomator dump //sdcard/window_dump.xml && $ADB pull //sdcard/window_dump.xml /tmp/window_dump.xml
```

After scrolling, re-parse XML and check for elements marked PENDING_SCROLL. Max 3 scroll attempts per screen.

Scroll back to top when done:
```bash
# Scroll to top (3 fast upward swipes)
for i in 1 2 3; do $ADB shell input swipe 540 600 540 1800 300; sleep 0.3; done
```

---

## Pattern 9: Crash/ANR Detection

```bash
PYTHONIOENCODING=utf-8 python -c "
import xml.etree.ElementTree as ET
tree = ET.parse('/tmp/window_dump.xml')
crash_indicators = ['has stopped', 'keeps stopping', 'isn\\'t responding', 'not responding']
for node in tree.iter('node'):
    text = node.get('text', '').lower()
    rid = node.get('resource-id', '').lower()
    if any(ind in text for ind in crash_indicators) or 'aerr' in rid:
        bounds = node.get('bounds', '')
        print(f'CRASH_DETECTED: text={node.get(\"text\",\"\")} id={node.get(\"resource-id\",\"\")} bounds={bounds}')
"
```

If crash detected: capture logcat crash trace with Pattern 13, screenshot, dismiss dialog (tap "Close" or "OK"), relaunch app.

---

## Pattern 10: Keyboard Dismiss

```bash
# Check if keyboard is visible
$ADB shell dumpsys input_method | grep mInputShown
# If mInputShown=true → keyboard is visible, dismiss it:
$ADB shell input keyevent BACK
sleep 0.5
```

Always dismiss keyboard before bottom nav taps or element searches below the keyboard area.

---

## Pattern 11: System Dialog Detection

```bash
PYTHONIOENCODING=utf-8 python -c "
import xml.etree.ElementTree as ET
tree = ET.parse('/tmp/window_dump.xml')
system_packages = ['com.android.permissioncontroller', 'com.android.systemui', 'com.google.android.permissioncontroller']
for node in tree.iter('node'):
    pkg = node.get('package', '')
    text = node.get('text', '').lower()
    if pkg in system_packages or text in ['allow', 'deny', 'while using the app', 'only this time', 'don\\'t allow']:
        bounds = node.get('bounds', '')
        print(f'SYSTEM_DIALOG: text={node.get(\"text\",\"\")} pkg={pkg} bounds={bounds}')
"
```

If system dialog detected: auto-tap "Allow" or "While using the app" to dismiss. Re-dump XML after dismissal.

---

## Pattern 12: Screenshot Validation

```bash
PYTHONIOENCODING=utf-8 python -c "
import os, sys, math
path = sys.argv[1]
size = os.path.getsize(path)
if size < 1024:
    print(f'BLANK_SUSPECT: file size {size} bytes (< 1KB)')
    sys.exit(1)
try:
    from PIL import Image
    img = Image.open(path).convert('L')
    pixels = list(img.getdata())
    n = len(pixels)
    mean = sum(pixels) / n
    variance = sum((p - mean) ** 2 for p in pixels) / n
    std_dev = math.sqrt(variance)
    unique_colors = len(set(pixels))
    if std_dev < 5.0 or unique_colors < 20:
        print(f'BLANK_SUSPECT: std_dev={std_dev:.1f} unique_colors={unique_colors} (content too uniform)')
        sys.exit(1)
    print(f'VALID: size={size} std_dev={std_dev:.1f} unique_colors={unique_colors}')
except ImportError:
    print(f'WARN: PIL not available, file size {size} bytes only')
    if size < 5000:
        print('BLANK_SUSPECT: small file and no PIL for content check')
        sys.exit(1)
    print('VALID: file size check only (install Pillow for content validation)')
" "$SCREENSHOT_DIR/{name}.png"
```

If BLANK_SUSPECT: wake device (`$ADB shell input keyevent WAKEUP`), wait 2s, retry capture (max 2 retries). If still BLANK_SUSPECT after retries, set `visual_verified=false` for this screen, track `blank_screenshots += 1`. This does NOT skip the issue.

---

## Pattern 13: Logcat Capture

```bash
# Crash traces only (AndroidRuntime errors)
$ADB logcat -d -t 100 AndroidRuntime:E *:S > $LOG_DIR/{session}/logcat_{screen}_crash.txt

# All errors
$ADB logcat -d -t 200 *:E > $LOG_DIR/{session}/logcat_{screen}_errors.txt

# App-specific logs
$ADB logcat -d -t 50 --pid=$($ADB shell pidof $APP_PACKAGE) > $LOG_DIR/{session}/logcat_{screen}_app.txt
```

Capture scope depends on screen result: PASS=app-only (50 lines), ISSUE_FOUND=all errors (200 lines), BLOCKED=crash traces (100 lines).

---

## CRITICAL: Compose testTag() is NOT visible in uiautomator XML

Jetpack Compose `testTag()` values do NOT appear in uiautomator XML dumps. All element searches must use:
- **`text`** attribute — visible text on screen
- **`content-desc`** attribute — accessibility labels
- **`resource-id`** attribute — Android resource IDs (rare in Compose)
- **`class`** attribute — widget type
- **Bounds position** — relative screen position (bottom nav y > 90%, top bar y < 15%)
