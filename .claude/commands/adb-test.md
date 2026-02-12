# ADB Manual E2E Testing

Test app screens via ADB (uiautomator dump, screencap, input tap) — fully autonomous with self-healing fix loops.

**Target screen:** $ARGUMENTS

If `$ARGUMENTS` is empty, test ALL 12 screens sequentially. If a screen name is provided (e.g., `home`, `grocery`), test only that screen. If a flow name is provided (e.g., `new-user-journey`), test that user journey flow.

**Valid screen names:** `auth-flow`, `home`, `grocery`, `chat`, `favorites`, `stats`, `settings`, `notifications`, `recipe-detail`, `cooking-mode`, `pantry`, `recipe-rules`

**Valid flow names:** `new-user-journey`, `existing-user`, `recipe-interaction`, `chat-ai`, `grocery-management`, `offline-mode`, `edge-cases`, `dark-mode`, `pantry-rules-crud`, `stats-tracking`

**Special arguments:** `all-flows` (run all 10 flows sequentially), `all-flows-from <name>` (run from specified flow onwards)

**Argument detection:**
1. If `$ARGUMENTS` matches a valid screen name → run existing screen test protocol (Sections E-F)
2. If `$ARGUMENTS` matches a valid flow name → run flow execution protocol (Section G)
3. If `$ARGUMENTS` is `all-flows` → run all 10 flows sequentially (flow01 → flow10)
4. If `$ARGUMENTS` is `all-flows-from <name>` → run flows from the specified flow onwards
5. If `$ARGUMENTS` is empty → run all 12 screen tests (existing behavior)

---

## AUTO-PROCEED RULES (MANDATORY)

- Do NOT ask for any confirmations before, during, or after testing
- Do NOT ask "Should I proceed?", "Ready to continue?", or similar
- Automatically handle all prerequisites (emulator, backend, build, auth, onboarding)
- If build fails, auto-fix compilation errors and rebuild — do not ask
- If emulator is not running, auto-start it and wait for boot — do not ask
- If backend is not running, auto-start it in the background and wait — do not ask
- If app is on auth screen, auto-authenticate via ADB taps — do not ask
- If app is on onboarding, auto-complete all 5 steps via ADB taps — do not ask
- If no meal plan exists, auto-generate via backend API — do not ask
- Proceed through all screens without pausing for user input
- Never stop for user input. If a screen exhausts 12 fix iterations (3 per issue), skip it and continue.

---

## ADB CONSTANTS & HELPERS

```
ADB = C:/Users/itsab/AppData/Local/Android/Sdk/platform-tools/adb.exe
EMULATOR = C:/Users/itsab/AppData/Local/Android/Sdk/emulator/emulator.exe
SCREENSHOT_DIR = docs/testing/screenshots
LOG_DIR = .claude/logs/adb-test
APP_PACKAGE = com.rasoiai.app
APP_ACTIVITY = com.rasoiai.app.MainActivity
```

### 13 Reusable ADB Patterns

**1. UI Dump — Capture current UI hierarchy:**
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

**2. Screenshot — Capture current screen:**
```bash
$ADB exec-out screencap -p > $SCREENSHOT_DIR/{name}.png
```
Screenshots are auto-resized by the existing PostToolUse hook (`.claude/hooks/post-screenshot-resize.sh`).

**3. Tap — Click at coordinates:**
```bash
$ADB shell input tap {cx} {cy}
```
Where `{cx}` and `{cy}` are the center of the element's bounds.

**4. Text Input — Type text into focused field:**
```bash
$ADB shell input text "{text}"
```
For special characters, use `$ADB shell input keyevent {keycode}`.

**5. Back Press — Navigate back:**
```bash
$ADB shell input keyevent BACK
```

**6. Parse Bounds — Extract center coordinates from bounds string:**
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

**7. Find Element — Search XML by text, content-desc, or resource-id:**
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

**8. Scroll and Redump — Scroll down, re-capture XML, check for new elements:**
```bash
# Scroll down (swipe up gesture) — repeat up to 3 times
$ADB shell input swipe 540 1800 540 600 500
sleep 1
$ADB shell uiautomator dump //sdcard/window_dump.xml && $ADB pull //sdcard/window_dump.xml /tmp/window_dump.xml
```
After scrolling, re-parse the XML and check for elements marked PENDING_SCROLL. Max 3 scroll attempts per screen.

Scroll back to top when done:
```bash
# Scroll to top (3 fast upward swipes)
for i in 1 2 3; do $ADB shell input swipe 540 600 540 1800 300; sleep 0.3; done
```

**9. Crash/ANR Detection — Check for app crash or ANR dialogs:**
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

**10. Keyboard Dismiss — Detect and dismiss soft keyboard:**
```bash
# Check if keyboard is visible
$ADB shell dumpsys input_method | grep mInputShown
# If mInputShown=true → keyboard is visible, dismiss it:
$ADB shell input keyevent BACK
sleep 0.5
```
Always dismiss keyboard before bottom nav taps or element searches below the keyboard area.

**11. System Dialog Detection — Detect permission/battery/system dialogs:**
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

**12. Screenshot Validation — Verify screenshot is not blank/corrupt:**
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
If BLANK_SUSPECT: wake device (`$ADB shell input keyevent WAKEUP`), wait 2s, retry capture (max 2 retries). If still BLANK_SUSPECT after retries → set `visual_verified=false` for this screen, track `blank_screenshots += 1`. This DOES NOT skip the issue — a blank screenshot is itself evidence of an issue that must be classified in E6.

**13. Logcat Capture — Capture app logs for debugging:**
```bash
# Crash traces only (AndroidRuntime errors)
$ADB logcat -d -t 100 AndroidRuntime:E *:S > $LOG_DIR/{session}/logcat_{screen}_crash.txt

# All errors
$ADB logcat -d -t 200 *:E > $LOG_DIR/{session}/logcat_{screen}_errors.txt

# App-specific logs
$ADB logcat -d -t 50 --pid=$($ADB shell pidof $APP_PACKAGE) > $LOG_DIR/{session}/logcat_{screen}_app.txt
```
Capture scope depends on screen result: PASS=app-only (50 lines), ISSUE_FOUND=all errors (200 lines), BLOCKED=crash traces (100 lines).

### CRITICAL: Compose testTag() is NOT visible in uiautomator XML

Jetpack Compose `testTag()` values do NOT appear in uiautomator XML dumps. All element searches must use:
- **`text`** attribute — visible text on screen (e.g., "BREAKFAST", "Home", "Settings")
- **`content-desc`** attribute — accessibility labels (e.g., "Menu", "Notifications", "Profile")
- **`resource-id`** attribute — Android resource IDs (rare in Compose)
- **`class`** attribute — widget type (e.g., `android.widget.EditText`)
- **Bounds position** — relative screen position (bottom nav y > 90%, top bar y < 15%)

---

## PREREQUISITES — Run These First (Every Time)

### D1. Check Emulator

```bash
$ADB devices
```

- If a device is listed as `device` → emulator is ready, continue.
- If no device or only `offline` entries:
  1. Start the emulator:
     ```bash
     $EMULATOR -avd Pixel_6 &
     ```
  2. Wait for boot:
     ```bash
     $ADB wait-for-device
     $ADB shell getprop sys.boot_completed
     ```
     Loop until `sys.boot_completed` returns `1` (max 120 seconds).

### D2. Check Backend

```bash
curl -s http://localhost:8000/health
```

- If healthy response → continue.
- If connection refused or error → auto-start:
  1. Start in background:
     ```bash
     cd D:/Abhay/VibeCoding/KKB/backend && source venv/bin/activate && uvicorn app.main:app --reload &
     ```
  2. Poll health check every 3 seconds, up to 30 seconds:
     ```bash
     for i in {1..10}; do curl -sf http://localhost:8000/health && break || sleep 3; done
     ```
  3. If healthy → log `Backend auto-started` and continue.
  4. If still unhealthy → log warning but continue (screens not requiring backend may still work).

### D3. Build & Install App

```bash
cd D:/Abhay/VibeCoding/KKB/android && ./gradlew assembleDebug
$ADB install -r D:/Abhay/VibeCoding/KKB/android/app/build/outputs/apk/debug/app-debug.apk
```

If build fails, auto-fix compilation errors and rebuild (max 3 attempts).

### D4. Clean Test Data

```bash
cd D:/Abhay/VibeCoding/KKB/backend && PYTHONPATH=. python scripts/cleanup_user.py
```

This ensures a fresh test environment.

### D5. Launch App & Detect Current Screen

```bash
$ADB shell am force-stop $APP_PACKAGE
$ADB shell am start -n $APP_PACKAGE/$APP_ACTIVITY
```

Wait 3 seconds, then dump UI and detect which screen is showing:

| Screen | Detection (text or content-desc in XML) |
|--------|----------------------------------------|
| Splash | text="RasoiAI" or loading indicator |
| Auth | text="Sign in with Google" or text="Welcome" |
| Onboarding | text="Tell us about your household" or progress bar |
| Home | text="This Week's Menu" or text="BREAKFAST" |
| Other | Check screen-specific identifiers |

### D6. Auto-Complete Auth & Onboarding (if needed)

**If on Auth screen:**
1. Find "Sign in with Google" button in XML
2. Compute center from bounds, tap via ADB
3. Wait up to 10 seconds for auth to complete
4. The fake auth (`fake-firebase-token`) will authenticate automatically
5. If onboarding appears next, proceed to onboarding steps below

**If on Onboarding screen — complete all 5 steps:**

| Step | Actions |
|------|---------|
| 1. Household | Find "Next" button (or text "Next"), tap household size dropdown if shown, tap to proceed |
| 2. Dietary | Tap a diet preference (e.g., text="Vegetarian"), tap "Next" |
| 3. Cuisine | Tap a cuisine (e.g., text="North Indian"), tap "Next" |
| 4. Dislikes | Tap "Next" (skip dislikes for speed) |
| 5. Cooking Time | Tap "Next" or "Get Started" or "Generate" |

After each step, wait 1-2 seconds and dump UI to verify progression. If generation screen appears, wait up to 90 seconds for meal plan generation (Gemini AI call).

**If no meal plan data on Home screen:**
Generate via backend API:
```bash
curl -X POST http://localhost:8000/api/v1/meal-plans/generate \
  -H "Authorization: Bearer $(curl -s -X POST http://localhost:8000/api/v1/auth/firebase -H 'Content-Type: application/json' -d '{"firebase_token":"fake-firebase-token"}' | python -c 'import sys,json;print(json.load(sys.stdin).get(\"access_token\",\"\"))')" \
  -H "Content-Type: application/json"
```
Wait up to 90 seconds for generation. Then force-stop and restart app to load the new meal plan.

### D7. Clear Logcat Buffer

```bash
$ADB logcat -c
```

Clear the log buffer so Pattern 13 captures only contain this session's entries.

---

## SCREEN TESTING PROTOCOL

### Test Definitions Reference

Read `docs/testing/adb-test-definitions.md` for the per-screen test checklists. That file contains for each screen:
- Navigation path (ADB taps from Home)
- Primary identifier (text/content-desc to verify arrival)
- Required elements checklist
- Interactive elements (click targets with expected results)
- Data validation expectations
- Known issues

### Execution Order

Test screens in this order (matches dependency chain):

1. `auth-flow` (Splash → Auth → Onboarding)
2. `home` (post-auth landing, has meal data)
3. `grocery` (requires meal plan)
4. `chat` (requires auth)
5. `favorites` (may be empty)
6. `stats` (may be empty)
7. `settings` (accessible from Home)
8. `notifications` (accessible from Home)
9. `recipe-detail` (requires tapping a meal card)
10. `cooking-mode` (requires recipe detail)
11. `pantry` (accessible from Settings)
12. `recipe-rules` (accessible from Settings)

### Per-Screen Protocol (8 Steps)

For each screen, execute these 8 steps:

**E0. Pre-Screen Checks**

**E0a. Backend Health** (only for backend-dependent screens: `auth-flow`, `home`, `chat`, `recipe-detail`, `recipe-rules`):
```bash
curl -sf http://localhost:8000/health --max-time 5
```
If unhealthy:
1. Kill existing uvicorn: find and kill the process
2. Restart: `cd D:/Abhay/VibeCoding/KKB/backend && source venv/bin/activate && uvicorn app.main:app --reload &`
3. Poll health every 3s for 30s
4. If still down after 30s → classify screen as BLOCKED with reason "Backend unhealthy"
Track: `backend_health_checks += 1`, `backend_restarts += 1` if restarted.

**E0b. System Dialog Check:**
Run Pattern 11 to detect any permission/battery/system dialogs. Auto-tap "Allow" or "While using the app" to dismiss. Re-dump XML after dismissal. This prevents system dialogs from interfering with screen navigation.

**E1. Navigate to Screen**

Follow the navigation path from `adb-test-definitions.md`. Use ADB taps to navigate:
- Bottom nav taps (Home, Grocery, Chat, Favorites, Stats)
- Top bar icon taps (Menu/Profile → Settings, Bell → Notifications)
- In-screen navigation (meal card → recipe detail → cooking mode)

After each tap, wait 1-2 seconds for navigation animation.

**E2. Verify Arrival (with Crash Detection)**

**E2a. Crash/ANR Check:**
Dump UI with uiautomator. Run Pattern 9 (Crash/ANR Detection) on the XML. If crash detected:
1. Capture logcat crash trace with Pattern 13
2. Take screenshot: `$ADB exec-out screencap -p > $SCREENSHOT_DIR/adb-test_{screen}_crash.png`
3. Dismiss crash dialog (tap "Close" or "OK")
4. Relaunch app: `$ADB shell am start -n $APP_PACKAGE/$APP_ACTIVITY`
5. Wait 3s, re-navigate to the screen (repeat E1)
6. Re-dump XML
7. Log as CRITICAL issue in `per_screen_issues[screen]`

**E2b. Primary Identifier Check:**
Search XML for the screen's primary identifier from `adb-test-definitions.md`. If not found after 3 dump attempts (with 2s gaps), classify as BLOCKED.

**E3. Element Checklist (Two-Phase Scroll Protocol)**

**Phase 1 — Initial Scan:**
Parse the UI dump XML and check each required element from the screen's checklist in `adb-test-definitions.md`. For each element:
- Search by `text`, `content-desc`, or `resource-id` attribute
- Record: FOUND (with bounds) or PENDING_SCROLL (if not found and element is marked "Below fold" in definitions)
- Elements NOT marked as below-fold that are missing → immediately record as MISSING

**Phase 2 — Scroll Search** (only if PENDING_SCROLL elements exist):
Use Pattern 8 (Scroll and Redump) — max 3 scroll attempts:
1. Scroll down: `$ADB shell input swipe 540 1800 540 600 500`
2. Wait 1s, re-dump XML
3. Re-check ALL PENDING_SCROLL elements against new XML
4. Mark newly found elements as FOUND_AFTER_SCROLL
5. Repeat until all found or 3 scrolls exhausted
6. Scroll back to top when done: `for i in 1 2 3; do $ADB shell input swipe 540 600 540 1800 300; sleep 0.3; done`

**Final Status Assignment:**
- FOUND → element present in initial dump
- FOUND_AFTER_SCROLL → element found after scrolling (counts as found for pass/fail)
- MISSING → element not found even after scrolling

Output a table:
```
| Element | Expected | Found | Status |
|---------|----------|-------|--------|
| Week selector | text or content-desc | bounds=[x,y][x,y] | FOUND |
| Dinner section | text="DINNER" | bounds=[x,y][x,y] | FOUND_AFTER_SCROLL |
| Snacks section | text="SNACKS" | NOT_FOUND | MISSING |
```

**ISSUE threshold:** >50% MISSING elements is an ISSUE. FOUND_AFTER_SCROLL counts as found (not missing).

**E4. Screenshot + AI Visual Analysis**

```bash
$ADB exec-out screencap -p > $SCREENSHOT_DIR/adb-test_{screen}_{timestamp}.png
```

**Screenshot Validation** (Pattern 12):
After capture, validate the screenshot:
1. Check file size (must be > 1KB)
2. If BLANK_SUSPECT: wake device (`$ADB shell input keyevent WAKEUP`), wait 2s, retry (max 2 retries)
3. If still BLANK_SUSPECT after retries → set `visual_verified=false`, log as issue evidence. Do NOT skip — a blank screenshot means visual verification CANNOT confirm correctness. This screen CANNOT be classified as PASS unless ALL elements are verified via XML AND all interactions pass.

Then read the screenshot with the Read tool and analyze:
- Layout correctness (elements properly positioned)
- Alignment issues (text cut off, overlapping)
- Data presence (are actual values shown, not placeholders)
- Color/theme correctness (matches design system)
- Empty states (appropriate when no data)

**If `visual_verified=false`:** Skip the Read tool visual analysis (there is nothing to see). Instead, log:
```
⚠️ VISUAL VERIFICATION SKIPPED: Screenshot blank/uniform (GPU rendering issue)
   This screen requires ALL elements found via XML AND ALL interactions passing to achieve PASS.
   Any MISSING element or FAILED interaction → ISSUE_FOUND (visual cannot compensate).
```

**E5. Interactive Testing**

For each interactive element in the screen's definition:
1. Find element in uiautomator XML (by text or content-desc)
2. Compute center coordinates from bounds
3. Tap via `$ADB shell input tap {cx} {cy}`
4. Wait 1-2 seconds for UI response
5. Dump UI again
6. Verify expected result (new screen appeared, sheet opened, state changed, etc.)
7. Take screenshot if state changed meaningfully
8. Navigate back to the original screen before testing next interaction:
   - For sheets/dialogs: tap outside or press BACK
   - For new screens: press BACK
   - For bottom nav: tap the original nav item
9. **Keyboard Dismiss** (Pattern 10): After any text input interaction (especially on Chat screen), check if keyboard is visible and dismiss it before proceeding. Always dismiss keyboard before bottom nav taps.

Record each interaction result:
```
| Action | Element | Expected | Actual | Status |
|--------|---------|----------|--------|--------|
| Tap "GROCERY" nav | bottom nav | Navigate to Grocery | Grocery screen shown | PASS |
| Tap bell icon | top bar | Open Notifications | Notifications screen shown | PASS |
```

**E5.5. Logcat Pre-Check**

Capture app-level logcat BEFORE classification to inform the Pre-Classification Gate:

```bash
$ADB logcat -d -t 50 --pid=$($ADB shell pidof $APP_PACKAGE) > $LOG_DIR/{session}/logcat_{screen}_precheck.txt
```

Scan for errors:
```bash
PYTHONIOENCODING=utf-8 python -c "
import sys
error_count = 0
with open(sys.argv[1]) as f:
    for line in f:
        if ' E ' in line or 'FATAL' in line or 'Exception' in line:
            error_count += 1
            print(f'ERROR: {line.strip()[:200]}')
print(f'Total app errors: {error_count}')
" "$LOG_DIR/{session}/logcat_{screen}_precheck.txt"
```

Record `app_error_count` for use in E5.7 Gate Question 5.
Track: `logcat_captures += 1`

**E5.7. Pre-Classification Gate (MANDATORY)**

Before classifying the screen in E6, you MUST answer ALL 6 questions below. Copy this checklist into your response and fill in each answer. If ANY answer is NO or a non-zero count, classification MUST be ISSUE_FOUND (not PASS).

```
□ Pre-Classification Gate for [{screen_name}]:
  1. "All required elements found (FOUND or FOUND_AFTER_SCROLL)?" → [YES: N/N / NO: N missing — ISSUE_FOUND]
  2. "All interactive tests passed?" → [YES: N/N / NO: N failed — ISSUE_FOUND]
  3. "Screenshot visually verified?" → [YES / NO (blank/GPU issue) — see E4 visual_verified flag]
  4. "Zero crashes/ANRs detected?" → [YES / NO — ISSUE_FOUND]
  5. "Logcat shows zero app errors?" → [YES / NO: N errors — ISSUE_FOUND]
  6. "Any observations that indicate unexpected behavior?" → [NO / YES: {list} — ISSUE_FOUND]

  GATE RESULT: [PASS_ELIGIBLE / ISSUE_FOUND]
  If ISSUE_FOUND, list each discrete issue with severity (CRASH/MISSING/FAILED/VISUAL/BEHAVIORAL).
```

**Rules:**
- If `visual_verified=false` (from E4): Question 3 is automatically NO. The screen CAN still pass if Questions 1,2,4,5,6 are all YES — but this is a narrow gate.
- Question 6 catches behavioral issues not covered by element checklists (e.g., "chat doesn't update preferences", "INCLUDE+EXCLUDE conflict not prevented"). These are real issues even if all XML elements are present.
- **An "observation" IS an issue.** There is no category called "observation" or "finding" or "noted behavior" — if something deviates from expected behavior, it is an ISSUE_FOUND.
- You MUST NOT proceed to E6 without completing this gate. Skipping the gate = PROCESS VIOLATION.

**E6. Classify Screen Result**

Classification is determined by the Pre-Classification Gate (E5.7) result:

| Classification | Criteria |
|----------------|----------|
| **PASS** | Gate result = PASS_ELIGIBLE: ALL 6 gate questions answered YES, zero missing elements, zero failed interactions, zero crashes, zero unexpected behaviors |
| **ISSUE_FOUND** | Gate result = ISSUE_FOUND: ANY gate question answered NO, OR any missing element (>0), OR any failed interaction (>0), OR any crash/ANR, OR any behavioral deviation from expected, OR `visual_verified=false` AND (any element MISSING or any interaction FAILED) |
| **BLOCKED** | Cannot reach screen (navigation failure) or cannot verify arrival after 3 attempts |

**CRITICAL:** There is no "PASS with observations" or "PASS with notes" category. If you detected ANY deviation from expected behavior — no matter how minor, known, or seemingly architectural — the classification is ISSUE_FOUND. The fix-loop budget (3 attempts per issue, 12 total) is the ONLY mechanism for resolving or marking issues as UNRESOLVED.

**E6.5. Post-Classification Logcat**

Capture scope-appropriate logcat based on the E6 classification:

| Screen Result | Capture Scope |
|---------------|---------------|
| PASS | (E5.5 pre-check is sufficient — no additional capture needed) |
| ISSUE_FOUND | All error logs: `$ADB logcat -d -t 200 *:E > $LOG_DIR/{session}/logcat_{screen}_errors.txt` |
| BLOCKED | Crash traces: `$ADB logcat -d -t 100 AndroidRuntime:E *:S > $LOG_DIR/{session}/logcat_{screen}_crash.txt` |

Then clear logcat: `$ADB logcat -c`

If **PASS** → move to next screen.
If **ISSUE_FOUND** → enter Fix Loop (Section F). **NO EXCEPTIONS** — even for known or pre-existing issues. The fix-loop budget (3 attempts per issue, 12 total) is the ONLY exit condition.
If **BLOCKED** → log as blocked, move to next screen.

---

## FIX LOOP (when ISSUE_FOUND)

**MANDATORY — NO EXCEPTIONS.** Every ISSUE_FOUND enters this loop regardless of whether the issue is known, pre-existing, or seems architectural. The budget limits (3 attempts per issue, 12 total iterations per screen) are the ONLY valid exit conditions. Do NOT skip the fix-loop based on your own judgment about issue complexity.

**Per-issue budgets: 3 attempts per issue, 12 max total iterations per screen.**

Issues are enumerated individually from E2a/E3/E4/E5 results and processed in severity order:
1. **CRASH** — App crashed or ANR detected (from E2a)
2. **MISSING** — Required elements not found (from E3)
3. **FAILED** — Interactive elements not working (from E5)
4. **VISUAL** — Layout/alignment/data issues (from E4)

Per-issue thinking escalation:

| Attempt | Thinking Level | Approach |
|---------|---------------|----------|
| 1 | **Normal** | Manual root cause analysis: read failure, trace to code, fix |
| 2 | **Thinkhard** | Launch `debugger` agent with all attempt logs, XML dumps, screenshots |
| 3 | **Ultrathink** | Launch `debugger` agent with max thinking + complete history |

### Fix Loop Steps

For each issue, for each attempt (up to 3 per issue, 12 total iterations per screen):

**Step F1: Pre-Fix Decision — Code vs. Definition**

Before assuming a code bug, check if the test definition is outdated:
1. Cross-reference the screenshot — does the element exist with different text?
2. Search XML for partial/semantic matches of the expected value
3. **Decision:**
   - If partial match found (e.g., expected "Grocery List" but XML has "Grocery") → update `docs/testing/adb-test-definitions.md`. Track: `definition_updates += 1`. **Re-verify:** After updating the definition, re-run E3 element checklist with the updated definition to confirm the element now passes. If it still fails → revert definition change, proceed to F2.
   - If no match at all → proceed to F2 (fix production code via agent)
   - **Limit:** Maximum 2 definition updates per screen. If a 3rd definition update is needed, this indicates the definitions are broadly wrong — log as UNRESOLVED and flag for manual review instead of continuing to patch definitions.

**Step F2: Run fix-loop (Single Fix mode)**

Read and follow the fix-loop process in `.claude/commands/fix-loop.md` in **Single Fix** mode (no `retest_command`):

```
failure_output:             {issue description + XML evidence + screenshot path + logcat}
failure_context:            "ADB test: screen={screen_name}, issue={issue_id} ({severity})"
files_of_interest:          {relevant source files for this screen}
build_command:              "./gradlew assembleDebug"  (or null if backend-only)
install_command:            "$ADB install -r android/app/build/outputs/apk/debug/app-debug.apk"  (if Android)
attempt_number:             {current attempt for this issue}
previous_attempts_summary:  {summary of prior attempts from iteration logs}
prohibited_actions:         ["Delete UI elements", "Weaken checklist", "Skip testing", "Mark PASS with issues", "Fix-later issues", "Downgrade issues to observations", "Skip Pre-Classification Gate", "Classify PASS with visual_verified=false AND missing/failed elements"]
fix_target:                 "production"
log_dir:                    ".claude/logs/adb-test/"
session_id:                 {current session id}
```

Collect fix-loop output:
- If `fix_applied`: append to `all_fixes[]`, accumulate metrics
- If `revert_applied` or `fix_applied == false`: log, proceed to F5

**Step F3: Relaunch App & Navigate** (caller responsibility — ADB-specific)

```bash
$ADB shell am force-stop $APP_PACKAGE
$ADB shell am start -n $APP_PACKAGE/$APP_ACTIVITY
```

Wait for Home screen to load, then navigate back to the screen under test.

**Step F4: Retest** (caller responsibility — ADB-specific)

Repeat the full screen testing protocol (E1-E6.5) for this screen.

**Step F5: Per-Issue Increment**

- If current issue RESOLVED and other issues remain → move to next OPEN issue (reset attempt counter)
- If current issue exhausted (3 attempts) → mark as UNRESOLVED, move to next OPEN issue
- If ALL issues resolved → exit fix loop with PASS, move to next screen
- If screen total iterations >= 12 → exit fix loop, classify remaining issues
- New status: **PARTIAL** — some issues resolved, some not

---

## REGRESSION TESTING (after Fix Loop)

**Trigger:** `total_fixes > 0` AND at least one screen has PASSED.

For each previously-passed screen (in execution order):

**R1. Navigate** — Follow the E1 navigation path for this screen.

**R2. Verify Arrival** — Run E2 (including E2a crash detection). If crash detected → mark as REGRESSED.

**R3. Element Spot-Check** — Run E3 element checklist only (skip interactive E5 for speed). Check all required elements are still present.

**R4. Classify:**
- All elements still present → **REGRESSION_PASS**
- Missing elements or crash → **REGRESSED** — log which fix likely caused it

Track: `regression_screens_tested += 1`, `regression_passes += 1` or `regressions_found += 1`

Regressions are logged in the final report for manual review — they are NOT auto-fixed (to avoid cascading fix loops).

---

## LOGGING

### Session Directory

Create at the start of the run:
```
.claude/logs/adb-test/{YYYYMMDD_HHMMSS}/
```

### Per-Iteration Log

File: `iteration-{NNN}-{screen}.md`

```markdown
# Iteration {NNN} - {Screen Name}

## Metadata
- Session: {id}
- Screen: {name}
- Iteration: {N}/12 (screen total)
- Issue: {issue_id} — Attempt {M}/3
- Thinking Level: {normal|thinkhard|ultrathink}
- Timestamp: {ISO 8601}

## Previous Iterations Summary
{2-3 line summary of each prior iteration for this screen}

## UI Dump Analysis
| Element | Expected | Found | Status |
|---------|----------|-------|--------|
{element checklist results}

## Screenshot Analysis
- File: {path}
- Visual Findings: {description of layout, alignment, data, colors}

## Interaction Results
| Action | Element | Expected | Actual | Status |
|--------|---------|----------|--------|--------|
{interaction test results}

## Issues Found
1. {severity}: {description} — Evidence: {XML node / screenshot}

## Root Cause Analysis
- Root Cause: {description}
- File: {path}
- Line: {N}

## Fix Applied
- File: {path}
- Change: {description}
- Code Review: {APPROVED/FLAGGED: details}

## Retest Result
- Status: {PASS / STILL_FAILING / NEW_ISSUE}
```

### Per-Screen Summary

File: `screen-{name}-summary.md`

Written after a screen completes (PASS, BLOCKED, or UNRESOLVED). Contains:
- Final status
- Total iterations used
- Fix history (all fixes applied)
- Remaining issues (if UNRESOLVED)
- Screenshots captured

---

## AGENT INTEGRATION

| Command/Agent | Trigger | Purpose |
|---------------|---------|---------|
| `fix-loop` command | Step F2, for each issue (Single Fix mode) | Analyze root cause, apply fix, code review gate, rebuild |
| `post-fix-pipeline` command | Post-run, if any fixes were applied | Test suite verification + documentation + git commit |

### How Commands Are Invoked

- **fix-loop**: Followed per-issue in Single Fix mode (no `retest_command`). Caller handles ADB relaunch/navigate/retest.
- **post-fix-pipeline**: Followed once after all screens complete (if fixes were applied).

### Post-Run Pipeline

After all screens complete (or the single requested screen), check if any fixes were applied.

**If `len(all_fixes) == 0`**: skip — no changes to commit.

**If `len(all_fixes) > 0`**: read and follow the post-fix-pipeline process in `.claude/commands/post-fix-pipeline.md`:

```
fixes_applied:            {all_fixes from tracking}
files_changed:            {all modified file paths}
session_summary:          "ADB test run: {N} fixes across {screens}"
regression_commands:      []   (ADB regression R1-R4 runs inline, not delegated)
test_suite_commands:      [
  { name: "backend", command: "cd backend && PYTHONPATH=. pytest --tb=short -q", timeout: 300 },
  { name: "android-unit", command: "cd android && ./gradlew test --console=plain", timeout: 600 }
]
test_suite_max_fix_attempts: 2
docs_instructions:        "Update docs/CONTINUE_PROMPT.md with session summary"
commit_format:            "fix(adb-test): {summary}"
commit_scope:             "adb-test"
push:                     false
```

Collect pipeline output for the final report:
- `test_suite_gate` from pipeline's test suite verification status
- Commit hash and message from pipeline's git commit result

---

## TRACKING VARIABLES

Initialize at the start of the run:

```
// Screen tracking
screens_tested = 0
screens_passed = 0
screens_failed = 0
screens_blocked = 0
screens_unresolved = 0
per_screen_results = {}  // screen_name → { status, iterations, fixes[], issues[] }
per_screen_issues = {}   // screen_name → [ { id, severity, description, status, attempts } ]

// Fix tracking
total_fixes = 0
all_fixes = []           // { file, line, description } collected from fix-loop outputs
total_iterations = 0
definition_updates = 0   // times adb-test-definitions.md was updated instead of production code

// Fix-loop tracking (accumulated from fix-loop outputs)
fix_loop_metrics = {
  debugger_invocations: 0,
  code_reviews: 0,
  code_reviews_approved: 0,
  code_reviews_flagged: 0,
  review_issues: [],
  build_failures: 0,
  reverts: 0
}

// Backend health tracking
backend_health_checks = 0
backend_restarts = 0

// Logcat tracking
logcat_captures = 0

// Visual verification tracking
blank_screenshots = 0          // screenshots that failed PIL content validation
visual_verified_screens = {}   // screen_name → true/false

// Regression testing (inline R1-R4)
regression_screens_tested = 0
regression_passes = 0
regressions_found = 0

// Post-fix pipeline results (from post-fix-pipeline command)
pipeline_status = "NOT_RUN"       // NOT_RUN | COMPLETED | BLOCKED_BY_TEST_SUITE | NO_FIXES
test_suite_gate = "NOT_RUN"       // NOT_RUN | PASSED | PASSED_AFTER_FIX | FAILED
commit_hash = ""
commit_message = ""

// Timing
start_time = now()
per_screen_times = {}    // screen_name → duration_seconds
```

---

## FINAL REPORT

After all screens complete, produce this report:

```
====================================================================
  ADB MANUAL E2E TEST REPORT
====================================================================
Screen  1: auth-flow        -> PASS (0 fixes)
Screen  2: home             -> PASS (1 fix, 2 iterations)
Screen  3: grocery          -> PASS (0 fixes)
Screen  4: chat             -> PASS (0 fixes)
Screen  5: favorites        -> PASS (0 fixes)
Screen  6: stats            -> PASS (0 fixes)
Screen  7: settings         -> PASS (0 fixes)
Screen  8: notifications    -> PASS (0 fixes)
Screen  9: recipe-detail    -> PASS (0 fixes)
Screen 10: cooking-mode     -> PASS (0 fixes)
Screen 11: pantry           -> PASS (0 fixes)
Screen 12: recipe-rules     -> PASS (0 fixes)
--------------------------------------------------------------------
TOTAL: X/12 passed | X fixes | X iterations | X blocked | X unresolved
====================================================================

Fixes Applied:
  1. [file:line] - {root cause and fix description}
  2. [file:line] - {root cause and fix description}

Definition Updates (if any):
  1. [screen] — Updated expected value: "{old}" → "{new}" in adb-test-definitions.md
  (If none, omit this section.)

Unresolved Issues:
  - [screen] — {description} (3 attempts exhausted per issue, or 12 total iterations exhausted)
  (If none, omit this section.)

Blocked Screens:
  - [screen] — {reason: could not navigate / backend unhealthy / could not verify arrival}
  (If none, omit this section.)

Backend Health:
  - Health checks: X
  - Restarts: X
  (If 0 restarts, omit this section.)

Regression Testing (if fixes were applied):
  - Screens retested: X
  - Regression passes: X
  - Regressions found: X
  - Regressed screens: [list with suspected fix cause]
  (If no fixes applied, show "Skipped — no fixes to verify")

Test Suite Verification (from post-fix-pipeline):
  - Gate: PASSED / PASSED_AFTER_FIX / FAILED / NOT_RUN
  - Details: [per-suite pass/fail from pipeline output]
  (If FAILED, list failing test names)

Logcat Captures:
  - Total captures: X
  - Files: $LOG_DIR/{session}/logcat_*.txt
  (If 0, omit this section.)

Agent Activity (from fix-loop + post-fix-pipeline):
  - Fix-loop invocations: X
  - Debugger invocations: X (from fix_loop_metrics)
  - Code reviews: X (Y approved, Z flagged)
  - Pipeline status: COMPLETED | BLOCKED_BY_TEST_SUITE | NOT_RUN
  - Docs updated: [list from pipeline, or "none — no fixes applied"]
  - Commit: [hash] — [message] (or "none" or "BLOCKED — test suite gate failed")

Review Issues (if any):
  - [severity] [file:line] — {description from fix-loop code review}

Session logs: .claude/logs/adb-test/{session}/
Duration: X minutes Y seconds
```

If only a single screen was requested, show just that screen's result.

---

## QUICK REFERENCE

| Screen | Shorthand | Navigation From | Backend Needed? | Key Interactions |
|--------|-----------|----------------|-----------------|-----------------|
| `auth-flow` | 1 | App launch | Yes | Sign-in, 5 onboarding steps |
| `home` | 2 | Post-auth landing | Yes | Day tabs, meal cards, lock, refresh, add, bottom nav |
| `grocery` | 3 | Bottom nav | Room | Categories, checkboxes, WhatsApp share |
| `chat` | 4 | Bottom nav | Yes (AI) | Type message, send, wait for response |
| `favorites` | 5 | Bottom nav | Room | Tabs, recipe cards (or empty state) |
| `stats` | 6 | Bottom nav | Room | Time tabs, streak, chart |
| `settings` | 7 | Profile icon | No | Toggles, sign-out, links to Pantry/Rules |
| `notifications` | 8 | Bell icon | Room | Filters, mark all read (or empty) |
| `recipe-detail` | 9 | Meal card tap | Yes | Favorite, servings, start cooking |
| `cooking-mode` | 10 | Recipe detail | No | Step navigation, complete |
| `pantry` | 11 | Settings link | Room | Add item (or empty) |
| `recipe-rules` | 12 | Settings link | Yes | Tabs, add rule, delete |

---

## SECTION G: FLOW EXECUTION PROTOCOL

When `$ARGUMENTS` matches a flow name, execute the corresponding flow definition file instead of the per-screen protocol.

### G1. Load Flow Definition

```bash
# Flow definition files
FLOW_DIR=docs/testing/flows
```

| Flow Name | File |
|-----------|------|
| `new-user-journey` | `flow01-new-user-journey.md` |
| `existing-user` | `flow02-existing-user.md` |
| `recipe-interaction` | `flow03-recipe-interaction.md` |
| `chat-ai` | `flow04-chat-ai.md` |
| `grocery-management` | `flow05-grocery-management.md` |
| `offline-mode` | `flow06-offline-mode.md` |
| `edge-cases` | `flow07-edge-cases.md` |
| `dark-mode` | `flow08-dark-mode.md` |
| `pantry-rules-crud` | `flow09-pantry-rules-crud.md` |
| `stats-tracking` | `flow10-stats-tracking.md` |

Read the flow definition file: `$FLOW_DIR/{flow-file}.md`

### G2. Check Flow Prerequisites

Each flow file has a **Prerequisites** section. Verify:
1. Standard D1-D7 prerequisites (same as screen tests)
2. Flow-specific prerequisites (e.g., Flow 2 requires Flow 1 state — do NOT clean test data)
3. **Depends On:** If the flow depends on another flow, verify that flow's state exists

**Special prerequisite handling:**
- If flow says "Do NOT run cleanup_user.py" → skip D4
- If flow says "needs existing user" → skip D4 and D6 auto-onboarding

### G3. Execute Steps

Follow the flow's **Steps** section sequentially. Each step uses the same 13 ADB patterns from above.

**Per-step execution:**
1. Read the step's **Action** column
2. Execute using appropriate ADB pattern (tap, type, scroll, wait, etc.)
3. Dump UI and verify the **Expected** column
4. If **Screenshot** column has a filename → capture: `$ADB exec-out screencap -p > $SCREENSHOT_DIR/{filename}`
5. If **Validation** column specifies a check → run it (see G4)
6. Run crash/ANR detection (Pattern 9) after each major navigation

**Between phases:** Log phase completion:
```
✅ Phase {X} Complete: {N}/{total} steps passed
```

### G4. Run Validation

When a step's Validation column says "V4a-V4k", run the validation script:

```bash
cd D:/Abhay/VibeCoding/KKB
python scripts/validate_meal_plan.py \
  --jwt "$JWT" \
  {args from flow's Validation Checkpoints section}
```

**JWT acquisition:**
```bash
JWT=$(curl -s -X POST http://localhost:8000/api/v1/auth/firebase \
  -H 'Content-Type: application/json' \
  -d '{"firebase_token":"fake-firebase-token"}' | \
  python -c 'import sys,json;print(json.load(sys.stdin).get("access_token",""))')
```

**Validation result handling:**
- Exit code 0 → all checks pass, continue
- Exit code 1 → HARD failure → log as flow issue, continue (do NOT stop flow)
- Exit code 2 → SOFT warnings only → log as warnings, continue

### G5. Per-Step Issue Detection

A step **fails** when ANY of the following is true:
- Expected text/element from the step's **Expected** column is not found in XML
- Expected behavior described in the step did not occur (e.g., "AI responded with tool calling" but AI only gave text suggestions)
- A crash or ANR is detected after the step's action
- The step's **Validation** column check returns exit code 1 (HARD failure)
- Any behavioral deviation from the step's **Expected** column, even if the app didn't crash
- Screenshot is blank AND the expected result cannot be verified via XML alone

**"Behavioral deviation" includes:** AI not performing expected actions (e.g., not calling tools, not updating preferences), missing conflict detection, missing validation that the step's Expected column describes, data not persisting as expected.

When a step fails:

1. **Same fix-loop integration as screen tests (MANDATORY — no exceptions, even for known issues)** — follow fix-loop process with:
   - `failure_context: "ADB flow test: flow={flow_name}, step={step_id}"`
   - `files_of_interest:` from the flow's **Fix Strategy** section
2. **Max 3 attempts per step** (same as per-issue budget in screen tests)
3. After fix, re-execute the failed step (not the entire flow)
4. If step still fails after 3 attempts → mark as UNRESOLVED, continue to next step

**There is no "PASS with observation" for flow steps.** If the Expected column says X and you observed Y, the step FAILED.

### G6. Flow Report

After flow completes, produce a flow-level report:

```
====================================================================
  ADB FLOW TEST REPORT: {flow-name}
====================================================================
Phase A: {name}          -> {N}/{total} steps PASS
Phase B: {name}          -> {N}/{total} steps PASS
...
--------------------------------------------------------------------
Validation Checkpoints:
  Checkpoint 1: {V4a-V4k results summary}
  Checkpoint 2: {V4a-V4k results summary}
--------------------------------------------------------------------
Contradictions Tested:
  C{N}: {description} -> {PASS/FAIL/WARNING}
--------------------------------------------------------------------
TOTAL: {passed}/{total} steps | {fixes} fixes | {screenshots} screenshots
Duration: X minutes Y seconds
Session logs: .claude/logs/adb-test/{session}/
====================================================================
```

### G7. All-Flows Mode

When `$ARGUMENTS` is `all-flows`:
1. Run flows in order: flow01 → flow10
2. Do NOT run `cleanup_user.py` between flows (flows build on each other's state)
3. Only run D4 cleanup before flow01
4. Produce a combined report at the end

When `$ARGUMENTS` is `all-flows-from <name>`:
1. Find the flow number for `<name>` (e.g., `chat-ai` = flow04)
2. Run flows from that number onwards (flow04 → flow10)
3. Assume prior flows' state already exists

---

## FLOW QUICK REFERENCE

| # | Flow | Screens | Contradictions | Duration | Key Feature |
|---|------|---------|----------------|----------|-------------|
| 1 | `new-user-journey` | 13 | C1-C5 | 15-25 min | Full onboarding + 2 meal plans |
| 2 | `existing-user` | 4 | — | 8-12 min | Persistence + plan #3 |
| 3 | `recipe-interaction` | 4 | — | 5-8 min | Favorite, cook, unfavorite |
| 4 | `chat-ai` | 3 | C6-C12 | 8-15 min | Chat + tool calling |
| 5 | `grocery-management` | 2 | C13 | 4-6 min | Categories, checkboxes, share |
| 6 | `offline-mode` | 5 | C14-C15 | 6-10 min | WiFi off, Room cache |
| 7 | `edge-cases` | 10 | C16-C21 | 5-8 min | Rapid nav, back stack |
| 8 | `dark-mode` | 6 | — | 4-6 min | Theme toggle + visual |
| 9 | `pantry-rules-crud` | 3 | C22-C27 | 8-12 min | CRUD + duplicate prevention |
| 10 | `stats-tracking` | 1 | — | 3-5 min | Streak, chart, tabs |
