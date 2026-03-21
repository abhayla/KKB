---
name: android-emulator-testing
description: >
  Android emulator E2E testing knowledge base and execution guide. Covers AVD setup,
  API level compatibility, backend connectivity, test timing, and known issues with
  solutions. Self-improving: add new learnings after resolving emulator issues.
  Use when running E2E tests, debugging emulator issues, or setting up test infrastructure.
allowed-tools: "Bash Read Grep Glob Write Edit"
argument-hint: "[run|setup|troubleshoot|add <learning>|search <query>|status]"
version: "1.0.0"
type: workflow
---

# Android Emulator Testing — Knowledge Base & Execution Guide

Knowledge base of emulator testing patterns, known issues, and solutions. Self-improving: every resolved issue gets recorded so it never repeats.

**Arguments:** $ARGUMENTS

---

## Modes

| Mode | Trigger | Description |
|------|---------|-------------|
| `run [--tier N] [--suite J0N]` | Execute E2E tests | Run `scripts/run-e2e.sh` with proper setup |
| `setup` | Prepare environment | Create AVD, start emulator, verify backend |
| `troubleshoot` | Diagnose failure | Read logcat, screenshots, test reports — apply knowledge base |
| `add <learning>` | Record new knowledge | Add an emulator/testing insight to the knowledge base |
| `search <query>` | Find relevant knowledge | Search entries by keyword |
| `status` | Check current state | Report emulator, backend, app install status |

---

## STEP 1: Mode Detection

Parse `$ARGUMENTS`:
- Starts with `run` → Execute Mode
- Starts with `setup` → Setup Mode
- Starts with `troubleshoot` → Troubleshoot Mode
- Starts with `add` → Add Knowledge Mode
- Starts with `search` → Search Mode
- Starts with `status` or empty → Status Mode

---

## Execute Mode (`run`)

### Pre-Run Checklist (auto-verify each)

1. **Emulator running?** → `adb devices` must show a device
2. **Correct API level?** → Read knowledge base for compatible APIs (currently: API 34)
3. **Backend healthy?** → `curl localhost:8000/health` must return `{"status":"healthy"}`
4. **App installed?** → `adb shell pm list packages | grep rasoiai` must show package
5. **Backend URL correct?** → `BaseE2ETest.BACKEND_BASE_URL` must be `http://10.0.2.2:8000`

If any check fails, auto-fix using knowledge base solutions. Then run:

```bash
./scripts/run-e2e.sh [flags from $ARGUMENTS]
```

### Post-Run Analysis

If tests fail:
1. Read test XML reports from `android/app/build/outputs/androidTest-results/`
2. Capture screenshot: `adb exec-out screencap -p > docs/testing/screenshots/e2e_failure.png`
3. Read logcat: `adb logcat -d -s RasoiAI:* BaseE2ETest:* AuthViewModel:*`
4. Search knowledge base for matching error patterns
5. If solution found → apply and re-run
6. If new issue → invoke `/systematic-debugging`, then `add` the learning

---

## Setup Mode (`setup`)

### 1. Find or Create Compatible AVD

```bash
# List AVDs
$ANDROID_HOME/emulator/emulator -list-avds

# Check API levels (path varies: ~/.android on Linux/Mac, $USERPROFILE/.android on Windows)
AVD_DIR="${HOME:-$USERPROFILE}/.android/avd"
for avd in $(emulator -list-avds); do
    grep "image.sysdir" "$AVD_DIR/${avd}.avd/config.ini" | grep -oP 'android-\K\d+'
done
```

**If no compatible AVD exists**, create one:
```bash
$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager create avd \
  --name "Pixel_8a_API_34" \
  --package "system-images;android-34;google_apis;x86_64" \
  --device "pixel_8a" --force
```

### 2. Start Emulator

```bash
$ANDROID_HOME/emulator/emulator -avd Pixel_8a_API_34 -no-snapshot-load -no-audio &
adb wait-for-device
# Wait for boot
while [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]]; do sleep 3; done
# Disable animations
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
```

### 3. Verify Backend

```bash
curl -s http://localhost:8000/health
# If not running (activate venv then start server):
cd backend && source venv/bin/activate 2>/dev/null || source venv/Scripts/activate && uvicorn app.main:app --host 0.0.0.0 --port 8000 &
```

---

## Troubleshoot Mode (`troubleshoot`)

### Auto-Diagnosis Steps

1. **Take screenshot** → `adb exec-out screencap -p > docs/testing/screenshots/diag.png` → Read it
2. **Check logcat** → `adb logcat -d -s RasoiAI:* BaseE2ETest:*` → Look for errors
3. **Check test reports** → Parse XML in `android/app/build/outputs/androidTest-results/`
4. **Search knowledge base** → Match error against `references/known-issues.md`
5. **If match found** → Apply solution from knowledge base
6. **If no match** → Invoke `/systematic-debugging` → Record new learning via `add`

---

## Add Knowledge Mode (`add <learning>`)

Append to `references/known-issues.md` in this format:

```markdown
### [CATEGORY] Short title
**Symptom:** What you observe (error message, screenshot description)
**Root Cause:** Why it happens
**Solution:** Step-by-step fix
**Date Added:** YYYY-MM-DD
**Verified On:** API level, emulator name
```

Categories: `API-COMPAT`, `TIMING`, `NETWORK`, `BUILD`, `EMULATOR`, `AUTH`, `CRASH`, `PERFORMANCE`

---

## Search Mode (`search <query>`)

Search `references/known-issues.md` and `references/api-compatibility.md` for matching entries. Return the most relevant solution.

---

## Status Mode (`status`)

Report current state:

```bash
# Emulator
adb devices
adb shell getprop ro.build.version.sdk  # API level
adb shell getprop ro.product.model       # Device name

# Backend
curl -s http://localhost:8000/health

# App
adb shell pm list packages | grep rasoiai
adb shell dumpsys package com.rasoiai.app | grep versionName

# Test infrastructure
ls android/app/build/outputs/apk/debug/app-debug.apk 2>/dev/null && echo "Debug APK exists"
ls android/app/build/outputs/apk/androidTest/debug/ 2>/dev/null && echo "Test APK exists"
```

---

## Output Format

All modes produce a structured status block at the end:

```
EMULATOR TEST REPORT
====================
Mode: [run|setup|troubleshoot|add|search|status]
Emulator: [device name] API [level] | NOT RUNNING
Backend: [healthy|unreachable|not started]
App: [installed (vN.N)|not installed]
Result: [PASSED|FAILED|FIXED|INFO]
Details:
  - [action taken or finding]
  - [action taken or finding]
Knowledge Base: [N entries in known-issues.md]
```

---

## Relationship to Other Skills

| Skill | Focus | When to use instead |
|-------|-------|-------------------|
| `/adb-test` | ADB-based manual UI testing (tap, swipe, screencap) | Testing app screens via uiautomator without Compose framework |
| `/test-knowledge` | General testing knowledge base (all platforms) | Recording non-emulator testing patterns (fixtures, mocking, etc.) |
| `/run-android-tests` | Running Gradle-based Android tests | Executing `./gradlew connectedDebugAndroidTest` with class resolution |
| `/android-emulator-testing` (this) | Emulator infrastructure and known issues | Setting up AVDs, debugging connectivity, resolving emulator-specific failures |

---

## CRITICAL RULES

- **ALWAYS** check the knowledge base (`references/known-issues.md`) before debugging from scratch
- **ALWAYS** record new solutions via `add` mode after resolving any emulator issue
- **NEVER** retry the same failed approach 3+ times — switch to `/systematic-debugging`
- **NEVER** assume API level compatibility — verify from `references/api-compatibility.md`
- **NEVER** hardcode IP addresses in test code — use `10.0.2.2` for emulator, read from config for devices
- When tests fail with unclear errors, take a screenshot FIRST — visual state reveals 80% of issues
