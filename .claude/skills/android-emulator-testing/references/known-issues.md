# Known Emulator Issues & Solutions

## [EMULATOR] First boot timeout on WHPX (Windows)
**Symptom:** Emulator boot times out at 120s on first launch with WHPX (Windows Hypervisor Platform).
**Root Cause:** First boot on x86_64 with WHPX is slow — needs to create system partition, optimize DEX files.
**Solution:** Increase boot timeout to 240s. Subsequent boots are ~25-45s.
**Date Added:** 2026-03-21
**Verified On:** API 34, Pixel_8a_API_34, Windows 11 WHPX

---

## [EMULATOR] "Process system isn't responding" dialog
**Symptom:** Emulator shows "Process system isn't responding" dialog, app can't launch.
**Root Cause:** Emulator was left running too long without interaction, system process hung.
**Solution:** Kill emulator (`adb emu kill`), restart fresh (`emulator -avd NAME -no-snapshot-load`).
**Date Added:** 2026-03-21
**Verified On:** API 34, Pixel_8a_API_34

---

## [NETWORK] Backend URL must be 10.0.2.2 for emulator
**Symptom:** App can't connect to backend running on localhost.
**Root Cause:** Android emulator maps `10.0.2.2` to host machine's localhost. Using `localhost` or `127.0.0.1` from emulator points to the emulator's own loopback.
**Solution:** Use `http://10.0.2.2:8000` in `BaseE2ETest.BACKEND_BASE_URL`. For physical devices, use the machine's LAN IP (e.g., `192.168.1.3`).
**Date Added:** 2026-03-21
**Verified On:** API 34, emulator-5554

---

## [NETWORK] DNS resolution failures in emulator
**Symptom:** Logcat shows `UnknownHostException: Unable to resolve host "www.google.com"`, `ConnectivityService: validation failed`.
**Root Cause:** Emulator launched with `-no-window` may have network configuration issues. Does NOT affect `10.0.2.2` connections (local, no DNS needed).
**Solution:** This is cosmetic — backend connectivity still works via `10.0.2.2`. If actual internet needed, restart emulator without `-no-window` flag or configure DNS: `adb shell setprop net.dns1 8.8.8.8`.
**Date Added:** 2026-03-21
**Verified On:** API 34, Pixel_8a_API_34

---

## [AUTH] E2E test auth timing — 2-second splash window
**Symptom:** `FullJourneyFlowTest.step1_auth()` fails with "Should navigate to onboarding or home within 20000ms".
**Root Cause:** `createAndroidComposeRule` launches TestActivity BEFORE `@Before` runs. `SplashViewModel` checks `phoneAuthClient.isSignedIn` after a 2-second delay (one-shot). Auth tokens must be stored before that 2s check fires.
**Solution:** For `setUpAuthenticatedState()`: authenticate with backend (~0.5s) + store tokens (~instant) = ~0.8s, within 2s window. For `setUpNewUserState()`: `clearAllState()` + `setSignInSuccess()` makes splash navigate to Auth screen (correct). The UI auth flow (enter phone, tap OTP) must then complete within 20s. If it doesn't, check: (1) emulator is responsive (no "system not responding"), (2) backend is reachable at 10.0.2.2:8000, (3) `FakePhoneAuthClient.shouldSucceed` is true.
**Date Added:** 2026-03-22
**Verified On:** API 34, Pixel_8a_API_34 — ongoing investigation

---

## [BUILD] Package name is com.rasoiai.app (not .debug)
**Symptom:** `pm clear com.rasoiai.app.debug` fails silently — app data not cleared.
**Root Cause:** `applicationIdSuffix` is commented out in build.gradle.kts — debug and release use the same package name.
**Solution:** Always use `com.rasoiai.app` (not `.debug`) for adb commands.
**Date Added:** 2026-03-22
**Verified On:** com.rasoiai.app confirmed via `adb shell pm list packages`

---

## [API-COMPAT] API 36 16KB page size images crash native libs
**Symptom:** App crashes on launch with `Library '*.so' is not PAGE(16384)-aligned`.
**Root Cause:** Some API 36 system images (tagged `16KBPageSize`) use 16KB memory pages. Native libraries compiled for 4KB pages can't load.
**Solution:** Use standard `google_apis` images, NOT `16KBPageSize` variants. Check image type: `grep tag.display ~/.android/avd/NAME.avd/config.ini`.
**Date Added:** 2026-03-22
**Verified On:** Web research — not directly observed (our Pixel_7a uses 16KBPageSize variant)

---

## [API-COMPAT] API 36 auto-resizability may affect test layouts
**Symptom:** Compose layout assertions fail on API 36 — elements positioned differently than expected.
**Root Cause:** Android 16 (API 36) makes apps targeting it auto-resizable + multi-window capable when display smallest width >= 600dp.
**Solution:** Test on API 34 for consistent layout behavior. If API 36 is needed, ensure test assertions use flexible matchers (not exact pixel positions).
**Date Added:** 2026-03-22
**Verified On:** Web research — not directly observed

---

## [PERFORMANCE] Disable animations for test stability
**Symptom:** Flaky tests — elements not found after transitions, timing-dependent failures.
**Root Cause:** Default animation scales (1.0) add latency to transitions, making test assertions flaky.
**Solution:** Disable all 3 animation scales before running tests:
```bash
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
```
**Date Added:** 2026-03-21
**Verified On:** API 34, Pixel_8a_API_34

---

## [EMULATOR] Windows port 8000 conflicts
**Symptom:** Backend won't start — "address already in use" on port 8000.
**Root Cause:** Previous uvicorn process still running. Windows doesn't auto-kill orphaned processes.
**Solution:** Find and kill: `netstat -ano | findstr :8000` then `taskkill /PID <PID> /F`.
**Date Added:** 2026-03-21
**Verified On:** Windows 11

---

## [NETWORK] BuildConfig.BASE_URL was localhost — must be 10.0.2.2 for emulator
**Symptom:** `FullJourneyFlowTest.step1_auth()` fails with "Should navigate to onboarding or home within 20000ms". Logcat shows `okhttp.OkHttpClient: --> POST http://localhost:8000/api/v1/auth/firebase` — the app calls `localhost` which is the emulator's own loopback, not the host machine.
**Root Cause:** `data/build.gradle.kts` debug `buildConfigField` was set to `http://localhost:8000/`. This is the URL used by the app's Retrofit service (`RasoiApiService`). Meanwhile, `BaseE2ETest.BACKEND_BASE_URL` was correctly set to `http://10.0.2.2:8000` — but that's only used for direct test helper HTTP calls, not the app's own API calls. Two separate URL configs caused a mismatch.
**Solution:** Change `data/build.gradle.kts` line 27 from `"http://localhost:8000/"` to `"http://10.0.2.2:8000/"`. This makes the app's Retrofit base URL match the emulator-to-host mapping.
**Date Added:** 2026-03-22
**Verified On:** API 34, Pixel_8a_API_34 — J03 FullJourneyFlowTest PASSED after fix

---

## [EMULATOR] Emulator crashes after ~2 hours of continuous testing
**Symptom:** `adb devices` shows empty list mid-test-run. Gradle hangs with "waiting for device". No crash dialog on emulator — it just disappears.
**Root Cause:** WHPX (Windows Hypervisor Platform) emulator is unstable under sustained load (continuous instrumented test execution for 2+ hours). Memory pressure and GPU driver fatigue contribute.
**Solution:** (1) Plan emulator restart between test tiers in the E2E pipeline. (2) Add emulator health check (`adb devices | grep device`) between each suite run in `run-e2e.sh`. (3) If emulator dead, auto-restart: kill process, `emulator -avd NAME -no-snapshot-load`, wait for boot, disable animations, reinstall APKs. (4) Always clean stale `androidTest-results/` lock files after emulator crash: `rm -rf app/build/outputs/androidTest-results/`.
**Date Added:** 2026-03-22
**Verified On:** API 34, Pixel_8a_API_34 — crashed 3 times during extended session

---

## [TIMING] Bottom sheet animation needs 1500ms wait in RecipeRulesRobot
**Symptom:** `selectIncludeAction()` / `selectExcludeAction()` fails with "Could not find node: TestTag = 'rule_action_include/exclude'" or "Condition still not satisfied after 5000ms".
**Root Cause:** `tapAddRuleButton()` had only 300ms `Thread.sleep` after clicking. The bottom sheet animation takes longer, especially when the rules list already has many items (more content to push down).
**Solution:** (1) Increase `tapAddRuleButton` sleep from 300ms to 1500ms. (2) Add `composeTestRule.waitUntil(10000)` in `selectIncludeAction` and `selectExcludeAction` to wait for the TestTag to appear before clicking. (3) Use `onAllNodesWithText()[0]` instead of `onNodeWithText()` in `tapRuleCard` to tolerate duplicate rule names.
**Date Added:** 2026-03-22
**Verified On:** API 34 — fixed J07 (29/29), J11 (55/55)

---

## [BUILD] Stale .lck files block Gradle after emulator crash
**Symptom:** `connectedDebugAndroidTest` fails instantly with "Cannot access output property 'resultsDir' — Failed to create MD5 hash for file utp.0.log.lck".
**Root Cause:** When the emulator crashes mid-test, Gradle leaves lock files in `app/build/outputs/androidTest-results/connected/debug/`. Next run can't write to the locked directory.
**Solution:** Always clean before re-running: `rm -rf android/app/build/outputs/androidTest-results/ android/app/build/reports/androidTests/`
**Date Added:** 2026-03-22
**Verified On:** Windows 11, Gradle 9.2.1
