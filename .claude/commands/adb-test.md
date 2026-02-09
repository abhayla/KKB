# ADB Manual E2E Testing

Test app screens via ADB (uiautomator dump, screencap, input tap) — fully autonomous with self-healing fix loops.

**Target screen:** $ARGUMENTS

If `$ARGUMENTS` is empty, test ALL 12 screens sequentially. If a screen name is provided (e.g., `home`, `grocery`), test only that screen.

**Valid screen names:** `auth-flow`, `home`, `grocery`, `chat`, `favorites`, `stats`, `settings`, `notifications`, `recipe-detail`, `cooking-mode`, `pantry`, `recipe-rules`

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
import os, sys
path = sys.argv[1]
size = os.path.getsize(path)
if size < 1024:
    print(f'BLANK_SUSPECT: file size {size} bytes (< 1KB)')
    sys.exit(1)
print(f'VALID: file size {size} bytes')
" "$SCREENSHOT_DIR/{name}.png"
```
If BLANK_SUSPECT: wake device (`$ADB shell input keyevent WAKEUP`), wait 2s, retry capture (max 2 retries). If still blank → skip visual analysis, rely on XML checklist only.

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
3. If still blank after retries → skip visual analysis, rely on XML checklist only, log warning

Then read the screenshot with the Read tool and analyze:
- Layout correctness (elements properly positioned)
- Alignment issues (text cut off, overlapping)
- Data presence (are actual values shown, not placeholders)
- Color/theme correctness (matches design system)
- Empty states (appropriate when no data)

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

**E6. Classify Screen Result**

Based on E2-E5 results:

| Classification | Criteria |
|----------------|----------|
| **PASS** | All required elements found, all interactions work as expected |
| **ISSUE_FOUND** | Missing elements, broken interactions, or visual problems detected |
| **BLOCKED** | Cannot reach screen (navigation failure) or cannot verify arrival |

**E6.5. Logcat Snapshot**

Capture logcat based on screen result using Pattern 13:

| Screen Result | Capture Scope |
|---------------|---------------|
| PASS | App-specific logs: `$ADB logcat -d -t 50 --pid=$($ADB shell pidof $APP_PACKAGE)` |
| ISSUE_FOUND | All error logs: `$ADB logcat -d -t 200 *:E` |
| BLOCKED | Crash traces: `$ADB logcat -d -t 100 AndroidRuntime:E *:S` |

Save to: `$LOG_DIR/{session}/logcat_{screen}_{status}.txt`
Then clear logcat: `$ADB logcat -c`
Track: `logcat_captures += 1`

If **PASS** → move to next screen.
If **ISSUE_FOUND** → enter Fix Loop (Section F).
If **BLOCKED** → log as blocked, move to next screen.

---

## FIX LOOP (when ISSUE_FOUND)

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

**Step F1: Read Previous Iteration Logs**

Read all previous iteration logs for this screen from `$LOG_DIR/{session}/`. Understand:
- What was tried before
- What the root causes were
- Why previous fixes didn't work
- Which issues are resolved vs. still open

**Step F2: Analyze Root Cause (Code vs. Definition Decision)**

Before assuming a code bug, check if the test definition is outdated:
1. Cross-reference the screenshot — does the element exist with different text?
2. Search XML for partial/semantic matches of the expected value
3. **Decision:**
   - If partial match found (e.g., expected "Grocery List" but XML has "Grocery") → update `adb-test-definitions.md` instead of production code. Track: `definition_updates += 1`
   - If no match at all → fix production code

**If fixing production code**, identify the ROOT CAUSE, not symptoms:
- Trace to a specific file and line number
- Common root causes:
  - Missing text/content-desc on Compose elements
  - Navigation route misconfiguration
  - ViewModel state not propagating to UI
  - Missing data from repository/API
  - Accessibility labels not set
  - Layout issues (element off-screen or overlapped)

**Attempt 1 (Normal):**
Read the failure, read the relevant source files, identify the root cause manually.

**Attempt 2 (Thinkhard):**
Launch the `debugger` agent (via Task tool) with:
- All previous attempt logs (concatenated)
- Current UI dump XML
- Current screenshot path
- Logcat capture from E6.5
- Instruction: "Use extended thinking (thinkhard). Systematically enumerate ALL possible root causes. Consider: Compose layout, ViewModel state flow, Room data availability, backend API response, navigation graph, accessibility semantics. Return a ranked list of hypotheses with the most likely fix."
Track: `debugger_invocations += 1`

**Attempt 3 (Ultrathink):**
Launch the `debugger` agent with:
- Complete history of all fix attempts
- Instruction: "Use maximum thinking depth (thinkUltrahard). Re-examine every assumption. Consider architectural issues, cross-module interactions, non-obvious failure modes. Explore unconventional fixes."
Track: `debugger_invocations += 1`

**Step F3: Apply Fix**

Make the minimum code change to fix the root cause. Fix production code (`.kt`, `.py`) not test infrastructure. If the issue was a definition mismatch (F2 decision), update `docs/testing/adb-test-definitions.md` instead.

**Step F4: Code Review Gate**

Launch the `code-reviewer` agent (via Task tool) with:
- The diff of changes (`git diff`)
- The issue that was found and the fix applied
- Instruction: "Review this fix for: regressions to other screens, weakened UI elements, removed accessibility labels, security issues (OWASP Top 10), hardcoded values. Categorize as Critical / High / Medium / Low. Return: APPROVED or FLAGGED."

- **If Critical issue found**: revert the fix (`git checkout -- <files>`), re-attempt from F2
- **If High/Medium/Low issues**: log them in `review_issues[]` but proceed
- **If APPROVED**: proceed

Track: `code_reviews += 1`, `code_reviews_approved += 1` or `code_reviews_flagged += 1`

**Step F5: Rebuild & Reinstall (with retry loop)**

**If Android code changed:**
```bash
cd D:/Abhay/VibeCoding/KKB/android && ./gradlew assembleDebug
$ADB install -r D:/Abhay/VibeCoding/KKB/android/app/build/outputs/apk/debug/app-debug.apk
```

**Build retry loop (max 3 attempts within F5):**
1. If build fails → analyze compilation error, apply minimal fix, retry build
2. If 3 build failures → revert fix (`git checkout -- {files}`), mark issue as FAILED_BUILD, proceed to F8
3. Track: `build_failures += 1` per failure, `build_reverts += 1` if reverted

**If only backend code changed:**
Kill existing uvicorn and restart:
```bash
# Find and kill uvicorn
kill $(ps aux | grep uvicorn | grep -v grep | awk '{print $2}') 2>/dev/null
cd D:/Abhay/VibeCoding/KKB/backend && source venv/bin/activate && uvicorn app.main:app --reload &
```
Poll health check every 3s for 15s before proceeding.

**Step F6: Relaunch App & Navigate**

```bash
$ADB shell am force-stop $APP_PACKAGE
$ADB shell am start -n $APP_PACKAGE/$APP_ACTIVITY
```

Wait for Home screen to load, then navigate back to the screen under test.

**Step F7: Retest**

Repeat the full screen testing protocol (E1-E6.5) for this screen.

**Step F8: Per-Issue Increment**

- If current issue RESOLVED and other issues remain → move to next OPEN issue (reset attempt counter for new issue)
- If current issue exhausted (3 attempts) → mark as UNRESOLVED, move to next OPEN issue
- If ALL issues resolved → exit fix loop with PASS, move to next screen
- If screen total iterations >= 12 → exit fix loop, classify remaining issues
- New status: **PARTIAL** — some issues resolved, some not (logged as UNRESOLVED with partial fix list)

### NEVER Do These During Fix Loop

- Delete or hide UI elements to make issues disappear
- Weaken the element checklist or interaction expectations
- Skip interactive testing after a fix
- Mark a screen as PASS when issues remain
- Create "fix later" GitHub issues to bypass failures

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

| Agent | Trigger | Purpose |
|-------|---------|---------|
| `debugger` | Fix attempt 2+ (thinkhard at 2, ultrathink at 3) | Deep root cause analysis with UI dump + screenshots + logcat + full history |
| `code-reviewer` | After every fix (Step F4) | Quality gate — catch regressions, removed elements, security issues |
| `tester` | Post-run (Step B, if fixes > 0) | Run backend + Android test suites as commit gate |
| `docs-manager` | Post-run (Step A, if fixes > 0) | Update CONTINUE_PROMPT.md, test docs |
| `git-manager` | Post-run (Step C, after tests pass) | Commit all changes with conventional format |

### How Agents Are Launched

All agents are launched via the **Task tool** with `subagent_type` matching the agent name. Include in the prompt:
- The agent's name and role context
- All relevant inputs (error output, UI dumps, screenshots, diffs)
- A clear instruction of what to return

### Post-Run Agent Pipeline

After all screens complete (or the single requested screen), check if any fixes were applied.

**If `len(all_fixes) == 0`**: skip agents — no changes to commit.

**If `len(all_fixes) > 0`**: run sequentially:

**Step A: docs-manager agent**
- List of all fixes applied
- Instructions: Update `docs/CONTINUE_PROMPT.md` with session summary

**Step B: Test Suite Verification** (after Step A)

Run test suites to verify fixes don't break existing tests:

**B1. Backend tests:**
```bash
cd D:/Abhay/VibeCoding/KKB/backend && PYTHONPATH=. pytest --tb=short -q
```
Record: pass count, fail count, test names that failed.

**B2. Android unit tests:**
```bash
cd D:/Abhay/VibeCoding/KKB/android && ./gradlew test --console=plain
```
Record: pass count, fail count, test names that failed.

**B3. Gate Decision:**
- ALL tests pass → `test_suite_gate = "PASSED"` → proceed to Step C
- Any tests fail → launch `tester` agent with failure details and diff of changes, instruction: "Analyze these test failures against the recent ADB-test fixes. Attempt to fix the failing tests (max 2 auto-fix attempts). Return fixed/still-failing status."
  - If auto-fix succeeds → `test_suite_gate = "PASSED_AFTER_FIX"` → proceed to Step C
  - If still failing after 2 attempts → `test_suite_gate = "FAILED"` → **DO NOT COMMIT**, report warning in final report

Track: `backend_tests_passed`, `backend_tests_failed`, `android_tests_passed`, `android_tests_failed`

**Step C: git-manager agent** (after Step B, only if `test_suite_gate != "FAILED"`)
- Instructions: Stage fix files + doc files + definition updates, create conventional commit: `fix(adb-test): [summary]`
- Do NOT push unless user explicitly requested

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
all_fixes = []           // { file, line, description } for each fix
total_iterations = 0
definition_updates = 0   // times adb-test-definitions.md was updated instead of production code

// Agent tracking
debugger_invocations = 0
code_reviews = 0
code_reviews_approved = 0
code_reviews_flagged = 0
review_issues = []       // logged High/Medium/Low issues from code-reviewer

// Backend health tracking
backend_health_checks = 0
backend_restarts = 0

// Build tracking
build_failures = 0
build_reverts = 0

// Logcat tracking
logcat_captures = 0

// Regression testing
regression_screens_tested = 0
regression_passes = 0
regressions_found = 0

// Test suite verification
backend_tests_passed = 0
backend_tests_failed = 0
android_tests_passed = 0
android_tests_failed = 0
test_suite_gate = "NOT_RUN"  // NOT_RUN | PASSED | PASSED_AFTER_FIX | FAILED

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

Test Suite Verification (if fixes were applied):
  - Backend: X passed, Y failed
  - Android: X passed, Y failed
  - Gate: PASSED / PASSED_AFTER_FIX / FAILED
  (If FAILED, list failing test names)

Build Issues (if any):
  - Build failures: X
  - Build reverts: X
  (If none, omit this section.)

Logcat Captures:
  - Total captures: X
  - Files: $LOG_DIR/{session}/logcat_*.txt
  (If 0, omit this section.)

Agent Activity:
  - Debugger invocations: X
  - Code reviews: X (Y approved, Z flagged)
  - Docs updated: [list of files, or "none — no fixes applied"]
  - Commit: [hash] — [message] (or "none — no fixes applied" or "BLOCKED — test suite gate failed")

Review Issues (if any):
  - [severity] [file:line] — {description from code-reviewer}

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
