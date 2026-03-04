# ADB-Test Skill Gap Analysis

**Date:** 2026-03-04
**Scope:** Infrastructure reliability, flow execution, coverage expansion
**Priority Order:** C (harden infrastructure) → A (get flows passing) → B (expand coverage)
**Exclusions:** Flow22 (Locust/API performance — architecturally different from UI flows)

---

## Executive Summary

The `/adb-test` skill defines 12 core screens, 24 total screen definitions, 21 UI flows (~830 steps), and 61 numbered contradictions. It has a 4-layer self-healing architecture and 14 reusable ADB interaction patterns.

**Reality check:** Only 1 of 21 flows (flow12) has ever been executed. That run failed at 57% pass rate, found 4 real backend bugs, and violated its own protocol (0 `/fix-loop` invocations, 0 `/verify-screenshots` invocations despite 12 failures). The hook enforcement chain is fundamentally broken for ADB-based testing — the `is_test_command()` function doesn't recognize ADB commands, making the primary `testFailuresPending` gate inert.

**Key numbers:**
- Protocol/infrastructure gaps found: **24** (7 ambiguities, 6 missing hooks, 6 protocol-vs-hook gaps, 5 secondary)
- Flows never executed: **20 of 21**
- Contradiction ID collisions: **15 pairs across 8 IDs** (C34-C37, C40-C45)
- Screens where ADB adds unique value beyond Compose tests: **5** (P0/P1)

---

## 1. PRIORITY C: Infrastructure & Protocol Gaps

These must be fixed before running any flows. Without them, the skill cannot enforce its own rules.

### 1.1 Critical Hook Enforcement Gaps (Ranked by Impact)

#### GAP-C1: ADB Commands Invisible to Hook System [CRITICAL]

**Impact:** The entire `testFailuresPending` enforcement chain is inert for adb-test.

`is_test_command()` in `hook-utils.sh:163` only matches `pytest`, `gradlew.*test`, and `connectedDebugAndroidTest`. ADB commands (`uiautomator dump`, `screencap`, `input tap`) are not recognized. Consequences:
- `testFailuresPending` is never set/cleared by ADB test outcomes
- `validate-workflow-step.sh` code-edit blocking gate never fires
- Claude can fail ADB tests, edit code inline, and commit — all without hook resistance

**Recommendation:** Either (a) add ADB command patterns to `is_test_command()`, or (b) acknowledge that ADB-test relies on protocol compliance, not hook enforcement, and document this explicitly. Option (b) is cheaper and aligns with the Q6 decision (protocol clarity first).

#### GAP-C2: Inline Fix Bypass Path Is Unblocked [CRITICAL]

**Impact:** Claude can fix issues without invoking `/fix-loop`, and no hook blocks the commit.

The enforcement chain: `testFailuresPending=true` → blocks edits → forces `/fix-loop` → tracks `fixLoopCount` → requires pipeline. For adb-test: `testFailuresPending` is never set (GAP-C1), so edits are never blocked, so `/fix-loop` is never forced, so `fixLoopCount` stays 0, so `verify-evidence-artifacts.sh` skips the pipeline requirement.

**Recommendation:** The SKILL.md protocol text already warns against inline fixes. Strengthen the warning with a numbered enforcement rule and a clear consequence statement. If this still fails after 2-3 flow runs, add a hook.

#### GAP-C3: Commit Gate False-Blocks Pure ADB Sessions [CRITICAL]

**Impact:** `verify-evidence-artifacts.sh:65-68` requires `testRuns[]` records. ADB commands never populate `testRuns[]`. A pure adb-test session that never runs `pytest`/`gradlew` will always fail with "No test runs recorded."

This creates perverse pressure to run `pytest` as a side effect just to satisfy the gate — or forces Claude to skip committing, defeating the workflow.

**Recommendation:** Add an adb-test branch to `verify-evidence-artifacts.sh` that checks for flow reports or screen results in `docs/testing/reports/` instead of `testRuns[]`. Alternatively, have the SKILL.md session initialization populate a sentinel `testRuns` entry.

#### GAP-C4: Screenshot Path Extraction Fails for Shell Variables [HIGH]

**Impact:** `post-screenshot.sh:47-52` strips paths containing `$`. The SKILL.md uses `$SCREENSHOT_DIR` in screencap commands. Screenshots are never recorded in `screenshotsCaptured[]`, so the screenshot verification gate silently drops out.

**Recommendation:** The SKILL.md should use literal paths in screencap commands instead of shell variables. Change the ADB CONSTANTS section to define the path inline: `docs/testing/screenshots/adb-test_{screen}_{timestamp}.png`.

#### GAP-C5: Session-Level Boolean Cannot Enforce Per-Step Verification [HIGH]

**Impact:** `verifyScreenshotsInvoked` is a single boolean. A flow with 30 UI steps could call `/verify-screenshots` once and satisfy the gate. The per-step blocking from Gate 1 is entirely text-based.

**Recommendation:** Accept this as a protocol-level gate (not hook-level). Strengthen the G3 step 6 text to include a count check: "After completing all steps, verify that `/verify-screenshots` was invoked exactly N times where N = count of UI steps with `screenshot_verified != N/A`."

#### GAP-C6: Stale State Inheritance Between Sessions [MEDIUM]

**Impact:** The SKILL.md session init patches only `activeCommand` in `workflow-state.json`. Prior session state (`testFailuresPending`, `screenshotsCaptured`, `fixLoopCount`) carries over, potentially blocking or confusing the adb-test session.

**Recommendation:** Reset the full state in session initialization. Add explicit resets: `testFailuresPending: false`, `screenshotsCaptured: []`, `fixLoopCount: 0`, `verifyScreenshotsInvoked: false`.

### 1.2 Protocol Ambiguities (Ranked)

#### GAP-P1: `/verify-screenshots` Required in Flow Mode But Not Screen Mode [HIGH]

Screen mode (E4) uses `Read` tool directly. Flow mode (G3) requires `Skill("verify-screenshots")`. But the commit gate (`verify-evidence-artifacts.sh`) applies the same `verifyScreenshotsInvoked` check regardless of mode. Screen mode will always fail this check.

**Recommendation:** Either (a) require `/verify-screenshots` in screen mode too (adds ~1 minute per screen), or (b) skip the `verifyScreenshotsInvoked` check when `activeCommand=adb-test` and mode is `screen-test`.

#### GAP-P2: Pre-Classification Gate (E5.7) Has No Enforcement [HIGH]

The 6 mandatory questions are text-only. Claude could skip them and classify PASS without any system-level check.

**Recommendation:** Add a structured output requirement: Claude must write the 6 answers to a JSON file (`docs/testing/reports/screen-{name}-gate.json`) before classifying. This creates an auditable artifact even without hook enforcement.

#### GAP-P3: ADB Retest After Single-Fix-Mode Never Updates Hooks [MEDIUM]

When `/fix-loop` runs in Single Fix mode (no retest command), the caller retests via ADB. ADB retest outcomes are invisible to hooks (GAP-C1). The fix-loop's success/failure is unknowable to the hook system.

**Recommendation:** After ADB retest, require Claude to write a structured result to the flow report or a JSON file, creating an audit trail independent of hooks.

#### GAP-P4: "API" Step Type Classification Is Self-Reported [MEDIUM]

If Claude misclassifies a UI step as API, screenshot verification is skipped. Gate 4 only checks for `screenshot_verified = "No"`, not `N/A` — so misclassified steps pass Gate 4.

**Recommendation:** Add Gate 4 condition: verify that `step_type` in the 13-column table matches the flow definition file's Type column. Any mismatch = Gate 4 failure.

#### GAP-P5: SOFT FAIL (Exit 2) vs UI PASS Ambiguity [LOW]

Validation script exit code 2 means SOFT failure, but if the UI screenshot looks correct, Claude might classify as PASS.

**Recommendation:** Add explicit rule: "Validation script exit code > 0 ALWAYS overrides UI appearance. If exit 2, the step is FAIL regardless of screenshot analysis."

### 1.3 Infrastructure Recommendations Summary

| Priority | Gap ID | Fix Type | Effort |
|----------|--------|----------|--------|
| MUST FIX | GAP-C1 | Protocol doc update (acknowledge limitation) | Low |
| MUST FIX | GAP-C2 | Protocol doc update (stronger warning) | Low |
| MUST FIX | GAP-C3 | Hook code change OR protocol workaround | Medium |
| MUST FIX | GAP-C4 | SKILL.md change (literal paths) | Low |
| SHOULD FIX | GAP-C5 | Protocol doc update (count check) | Low |
| SHOULD FIX | GAP-C6 | SKILL.md change (full state reset) | Low |
| SHOULD FIX | GAP-P1 | Protocol doc update (mode-specific rules) | Low |
| SHOULD FIX | GAP-P2 | Protocol doc update (JSON artifact) | Low |
| NICE TO HAVE | GAP-P3 | Protocol doc update (structured result) | Low |
| NICE TO HAVE | GAP-P4 | Protocol doc update (Gate 4 condition) | Low |
| NICE TO HAVE | GAP-P5 | Protocol doc update (exit code rule) | Low |

---

## 2. PRIORITY A: Flow Execution Gaps

### 2.1 Execution Status

| Flow | Name | Steps | Ever Run? | Report Exists? |
|------|------|------:|:---------:|:--------------:|
| 01 | new-user-journey | ~79 | No | No |
| 02 | existing-user | 25 | No | No |
| 03 | recipe-interaction | 26 | No | No |
| 04 | chat-ai | 34 | No | No |
| 05 | grocery-management | 32 | No | No |
| 06 | offline-mode | 31 | No | No |
| 07 | edge-cases | 37 | No | No |
| 08 | dark-mode | 20 | No | No |
| 09 | pantry-rules-crud | 56 | No | No |
| 10 | stats-tracking | 16 | No | No |
| 11 | settings-deep-dive | 78 | No | No |
| 12 | multi-family-medical | 50 | **Yes** | **Yes** (57% pass) |
| 13 | festival-meals | 26 | No | No |
| 14 | nutrition-goals | 36 | No | No |
| 15 | notifications-lifecycle | 61 | No | No |
| 16 | achievement-earning | 51 | No | No |
| 17 | pantry-suggestions | 56 | No | No |
| 18 | photo-analysis | 31 | No | No |
| 19 | multi-week-history | 44 | No | No |
| 20 | recipe-scaling | 40 | No | No |
| 21 | recipe-rules-comprehensive | 94 | No | No |

**Total: 1/21 executed (4.8%). 0/21 passing.**

### 2.2 Flow12 Lessons

Flow12 (`multi-family-medical`) ran on 2026-02-15. Key takeaways:

| Metric | Expected | Actual |
|--------|----------|--------|
| Duration | 8-10 min | ~25 min (2.5x) |
| Pass rate | 100% | 57% (25/44) |
| `/fix-loop` invocations | ≥12 (for 12 failures) | 0 |
| `/verify-screenshots` invocations | ≥5 (for 5 screenshots) | 0 |
| Screenshots verified | 5 | 0 |

**Protocol violations confirmed:** The self-healing architecture was not followed. This validates the Q6 decision to focus on protocol clarity first.

**Real bugs found (document as test expectations, per Q4 decision):**

| Bug | Affected Contradictions | Test Expectation |
|-----|------------------------|------------------|
| No family-safety conflict detection on recipe rules | C28, C31, C34 | POST /api/v1/recipe-rules with conflicting health condition SHOULD return 409 or warning |
| Jain dietary constraints not enforced in meal generation | D violations | Generated meal plan for Jain member SHOULD NOT contain root vegetables, onion, garlic |
| Per-member diet override ignored | C29 | Non-veg family member SHOULD get non-veg options even if household is vegetarian |
| Room sync gap for family members | A13, F1 | Family members created via API SHOULD appear in Android Settings screen |

### 2.3 Chapter-Based Flow Grouping (Per Q7 Decision)

Flows grouped into independently-runnable chapters. Chapters are independent; flows within a chapter may depend on each other.

#### Chapter 1: New User Setup (Sequential, runs first)

| Flow | Name | Steps | Dependencies |
|------|------|------:|--------------|
| 01 | new-user-journey | ~79 | None |
| 02 | existing-user | 25 | Flow 01 state |

**Chapter setup:** None — flow01 creates all state from scratch.
**Total steps:** ~104 | **Est. duration:** 23-37 min

#### Chapter 2: Core Features (Independent, API-seeded state)

| Flow | Name | Steps | Dependencies |
|------|------|------:|--------------|
| 03 | recipe-interaction | 26 | Auth + meal plan |
| 05 | grocery-management | 32 | Auth + meal plan |
| 08 | dark-mode | 20 | Auth only |
| 10 | stats-tracking | 16 | Auth + meal plan |

**Chapter setup:** API calls to create user, authenticate, generate meal plan (~30 seconds).
**Total steps:** 94 | **Est. duration:** 16-25 min

#### Chapter 3: Settings & CRUD (Independent, API-seeded state)

| Flow | Name | Steps | Dependencies |
|------|------|------:|--------------|
| 09 | pantry-rules-crud | 56 | Auth + preferences |
| 11 | settings-deep-dive | 78 | Auth + preferences |
| 21 | recipe-rules-comprehensive | 94 | Auth + preferences |

**Chapter setup:** API calls to create user, authenticate, set preferences (~30 seconds).
**Total steps:** 228 | **Est. duration:** 42-58 min

#### Chapter 4: AI & Constraints (Independent, API-seeded state)

| Flow | Name | Steps | Dependencies |
|------|------|------:|--------------|
| 04 | chat-ai | 34 | Auth + preferences |
| 12 | multi-family-medical | 50 | Auth + preferences |
| 13 | festival-meals | 26 | Auth + preferences + festivals |
| 14 | nutrition-goals | 36 | Auth + preferences |

**Chapter setup:** API calls to create user, authenticate, set preferences, seed festivals (~45 seconds).
**Total steps:** 146 | **Est. duration:** 27-43 min

#### Chapter 5: Advanced Features (Independent, API-seeded state)

| Flow | Name | Steps | Dependencies |
|------|------|------:|--------------|
| 15 | notifications-lifecycle | 61 | Auth + meal plan |
| 16 | achievement-earning | 51 | Auth + meal plan + achievements |
| 17 | pantry-suggestions | 56 | Auth + pantry items |
| 18 | photo-analysis | 31 | Auth |
| 19 | multi-week-history | 44 | Auth + meal plan |
| 20 | recipe-scaling | 40 | Auth + meal plan |

**Chapter setup:** API calls to create user, authenticate, generate meal plan, seed achievements (~45 seconds).
**Total steps:** 283 | **Est. duration:** 42-68 min

#### Chapter 6: Resilience (Independent, API-seeded state)

| Flow | Name | Steps | Dependencies |
|------|------|------:|--------------|
| 06 | offline-mode | 31 | Auth + cached data |
| 07 | edge-cases | 37 | Auth |

**Chapter setup:** API calls to create user, authenticate, generate meal plan, ensure Room cache populated (~45 seconds).
**Total steps:** 68 | **Est. duration:** 11-18 min

#### Recommended Execution Order

1. **Chapter 2** first — simplest flows, fastest feedback loop, validates basic infrastructure
2. **Chapter 6** second — tests resilience patterns that affect all other chapters
3. **Chapter 1** third — the critical new-user-journey; longest single flow
4. **Chapter 3** fourth — heavy CRUD testing
5. **Chapter 4** fifth — requires AI API calls (Gemini/Claude), most expensive
6. **Chapter 5** last — advanced features, some may depend on not-yet-implemented backend endpoints

### 2.4 Flow Dependency Analysis

| Dependency Type | Flows | Impact |
|-----------------|-------|--------|
| Hard dependency on flow01 | 02 | Cannot run without flow01 state |
| Soft dependency (needs auth + meal plan) | 03, 05, 06, 07, 08, 10, 15, 16, 19, 20 | Can be API-seeded |
| Soft dependency (needs auth + preferences) | 04, 09, 11, 12, 13, 14, 21 | Can be API-seeded |
| Requires unimplemented backend features | 17 (pantry suggestions), 18 (photo analysis), 19 (multi-week) | May fail at API level — verify endpoints exist first |

---

## 3. PRIORITY B: Coverage Expansion

### 3.1 Screen Coverage — Priority Tiers

Comparing ADB screen definitions against Compose instrumented test coverage to identify where ADB testing adds unique value.

#### P0: Must Add (No E2E coverage, or ADB adds critical unique value)

| Screen | ADB Definition Exists? | Compose E2E Tests | Why P0 |
|--------|:----------------------:|:-----------------:|--------|
| Onboarding (5-step wizard) | No (only in flow01) | OnboardingFlowTest (8), SharmaOnboardingVerificationTest (1) | The onboarding wizard is the most complex multi-step flow. ADB tests can verify real DataStore persistence + backend sync that Compose tests mock. However, dropdown limitation (Pattern 14) limits ADB value. **Downgraded consideration.** |
| Generation (progress screen) | No | CoreDataFlowTest (1), MealPlanGenerationFlowTest (1) | Real Gemini API call timing and progress display. Compose tests cover this with real API. **Not unique to ADB.** |

**Revised P0: None.** After comparing against Compose coverage, no screen lacks E2E coverage entirely that would benefit from an ADB-specific screen definition. The Compose tests already cover all critical screens with real backend calls.

#### P1: Should Add (ADB adds meaningful unique value)

| Screen | ADB Definition? | Compose Coverage | Why P1 |
|--------|:---------------:|:----------------:|--------|
| Achievements | Yes (#16 in extended defs) | AchievementsScreenTest (16 UI tests), **NO E2E flow** | Only screen with zero E2E flow coverage. ADB flow16 would be the only end-to-end test. |
| Notifications | Yes (#8) | NotificationsScreenTest (15 UI tests), minimal E2E (nav only) | No E2E test verifies notification CRUD, badge count, or mark-all-read with real backend. |
| Family Members (Settings) | Yes (#18 in extended defs) | FamilyMembersScreenTest (7 UI), SettingsFlowTest (nav only) | Flow12 revealed the Room sync gap here. ADB can test real API→Room→UI data flow. |

#### P2: Nice to Have (Compose tests cover adequately)

| Screen | ADB Definition? | Compose Coverage | Why P2 |
|--------|:---------------:|:----------------:|--------|
| Splash | No | SplashScreenTest (7), AuthFlowTest | Simple screen, Compose covers it |
| Recipe Search | No | RecipeInteractionFlowTest (7) | Compose tests cover search + add flows |
| 10 Settings sub-screens | Yes (#13-24 in extended defs) | Individual ScreenTests (4-7 each), SettingsFlowTest nav | Settings sub-screens are simple preference selectors. Compose UI tests are sufficient. |

### 3.2 SKILL.md Screen Count Discrepancy

The SKILL.md header says "Supports 12 screens and 21 flows." The `adb-test-definitions.md` file actually defines **24 screens** (12 core + 12 settings sub-screens added later). The SKILL.md should be updated to reflect the actual count.

### 3.3 Contradiction Coverage & Collision Issues

#### Assignment Verification

All C1 through C61 are assigned to at least one flow. **No orphaned contradictions exist.**

#### ID Collision Problem

15 contradiction pairs across 8 IDs have **different meanings in different flows**:

| ID | Flow A | Meaning in Flow A | Flow B | Meaning in Flow B |
|----|--------|-------------------|--------|-------------------|
| C34 | 12 | Jain + Onion INCLUDE → pre-filter removes | 13 | Non-veg user on fasting day |
| C35 | 12 | Diabetic + Sweet INCLUDE → post-filter removes | 13 | INCLUDE non-veg on fasting day → blocked |
| C36 | 12 | Sattvic + Egg INCLUDE → pre-filter removes | 14 | Impossible nutrition target (50/week) |
| C37 | 12 | Multiple constraints compound | 14 | Duplicate nutrition goal → 409 |
| C40 | 15 | Notification already read | 18 | Non-food image upload |
| C40 | 20 | Servings = 1 minimum | — | — |
| C41 | 15 | Delete only notification → empty | 18 | Low-quality image |
| C41 | 20 | Servings = 12 maximum | — | — |
| C42 | 15 | Filter unread with all read → empty | 18 | Complex dish 10+ ingredients |
| C42 | 20 | Reset servings to original | — | — |
| C43 | 16 | Achievement already unlocked | 19 | Rapid week navigation |
| C44 | 16 | Achievement requirements not met | 19 | Navigate 4 weeks past |
| C45 | 16 | Cooking same recipe 1 unique | 19 | Navigate 4 weeks future |

**Recommendation:** Adopt flow-scoped IDs: `F12-C34` vs `F13-C34`. This eliminates ambiguity in reporting and cross-referencing. A single find-and-replace across flow definition files would fix this.

---

## 4. Backend Bugs as Test Expectations

Per Q4 decision: document what the skill should verify, not scope fixing the bugs.

| Bug | Flow | Steps Affected | Expected Behavior (Test Expectation) |
|-----|------|---------------|--------------------------------------|
| No family-safety conflict detection | 12 | C28, C31 | `POST /api/v1/recipe-rules` with item conflicting a family member's health condition → response includes `warning` field or returns 409 |
| Jain constraints not enforced | 12 | D2, G4, G5 | Generated meal plan for household with Jain member → zero items containing root vegetables, onion, garlic in the `recipe_name` field |
| Per-member diet override ignored | 12 | C29 | Meal plan for household with mixed dietary preferences → at least some meals tagged with per-member diet (e.g., `non-vegetarian` items for non-veg member) |
| Room sync gap for family members | 12 | A13, F1 | After creating family members via API, navigating to Settings → Family Members in app → members visible in UI (requires backend fetch on screen load) |

These expectations should be encoded in the flow12 definition file as explicit verification criteria with pass/fail thresholds.

---

## 5. Prioritized Action Plan

### Phase 1: Infrastructure Hardening (Before any flow runs)

| # | Action | Gap ID | Effort | Files to Modify |
|---|--------|--------|--------|----------------|
| 1 | Replace `$SCREENSHOT_DIR` with literal paths in SKILL.md | GAP-C4 | 30 min | `.claude/skills/adb-test/SKILL.md` |
| 2 | Add full state reset to session initialization | GAP-C6 | 30 min | `.claude/skills/adb-test/SKILL.md` |
| 3 | Add explicit "ADB tests do not trigger hook gates" acknowledgment | GAP-C1, C2 | 1 hr | `.claude/skills/adb-test/SKILL.md`, `references/screen-definitions.md` |
| 4 | Strengthen `/fix-loop` invocation requirement with numbered enforcement rules | GAP-C2 | 1 hr | `references/screen-definitions.md`, `references/flow-definitions.md` |
| 5 | Fix `verify-evidence-artifacts.sh` for pure ADB sessions | GAP-C3 | 1 hr | `.claude/hooks/verify-evidence-artifacts.sh` |
| 6 | Add per-step `/verify-screenshots` count check to flow protocol | GAP-C5 | 30 min | `references/flow-definitions.md` |
| 7 | Add Pre-Classification Gate JSON artifact requirement | GAP-P2 | 30 min | `references/screen-definitions.md` |
| 8 | Rename conflicting contradiction IDs to flow-scoped format | Collision | 2 hr | `docs/testing/flows/flow12-flow21.md` (10 files) |
| 9 | Update SKILL.md screen count from "12" to "24" | Discrepancy | 5 min | `.claude/skills/adb-test/SKILL.md` |

**Estimated total: ~7 hours**

### Phase 2: First Flow Execution (Validate infrastructure fixes)

| # | Action | Chapter | Effort |
|---|--------|---------|--------|
| 1 | Run Chapter 2 flows (03, 05, 08, 10) | Core Features | 2-3 hrs |
| 2 | Document all protocol compliance issues encountered | — | During run |
| 3 | Run Chapter 6 flows (06, 07) | Resilience | 1-2 hrs |
| 4 | Assess: did the infrastructure fixes from Phase 1 result in protocol compliance? | — | 1 hr |

**Decision gate:** If protocol compliance is <80% after Phase 2, add hook enforcement (hooks were deferred per Q6). If ≥80%, proceed to Phase 3.

### Phase 3: Expand Flow Execution

| # | Action | Chapter | Effort |
|---|--------|---------|--------|
| 1 | Run Chapter 1 (flow01, flow02) | New User Setup | 3-4 hrs |
| 2 | Run Chapter 3 (flow09, flow11, flow21) | Settings & CRUD | 4-6 hrs |
| 3 | Run Chapter 4 (flow04, flow12, flow13, flow14) | AI & Constraints | 3-5 hrs |
| 4 | Run Chapter 5 (flow15-flow20) | Advanced Features | 4-7 hrs |

### Phase 4: Coverage Expansion (After existing flows stabilize)

| # | Action | Priority | Effort |
|---|--------|----------|--------|
| 1 | Create Achievements E2E flow (only screen with zero E2E coverage) | P1 | 2-3 hrs |
| 2 | Enhance Notifications flow15 with real CRUD testing | P1 | 2-3 hrs |
| 3 | Create Family Members deep-CRUD flow (validates Room sync fix) | P1 | 2-3 hrs |

---

## Appendix A: Compose Test Coverage Reference

For context on what's already tested by Compose instrumented tests (to avoid duplication):

| Screen | Compose E2E Tests | Compose UI Tests | ADB Unique Value |
|--------|:-----------------:|:----------------:|:----------------:|
| Auth | 4 (AuthFlowTest) | 27 (Auth+Integration) | Low — Compose uses FakePhoneAuthClient |
| Onboarding | 14 (across 4 files) | 41 | Low — dropdowns unreachable via ADB |
| Generation | 2 | 21 | Low — Compose tests use real Gemini API |
| Home | 50 (HomeScreenComprehensiveTest) | 22 | Low — exhaustively covered |
| Grocery | 18 | 21 | Low |
| Chat | 6 | 17 | Medium — real Claude API responses |
| Favorites | 16 | 17 | Low |
| Stats | 6 | 21 | Low |
| Settings | 24 | 15 | Medium — deep sub-screen CRUD |
| Notifications | 0 (nav only) | 15 | **High — no E2E CRUD tests** |
| Recipe Detail | 10 (CookingMode) | 29 | Low |
| Cooking Mode | 10 | 22 | Low |
| Pantry | 7 | 17 | Low |
| Recipe Rules | 29 (Rules+Nutrition+Sharma) | 26 | Low |
| Achievements | **0** | 16 | **High — no E2E tests at all** |
| Family Members | 0 (nav only) | 7 | **High — Room sync gap** |

## Appendix B: Flow Step Counts & Dependencies

| Flow | Steps | Depends On | Chapter | Contradictions |
|------|------:|------------|---------|---------------|
| 01 | ~79 | None | 1 | C1-C5 |
| 02 | 25 | Flow 01 | 1 | None |
| 03 | 26 | Auth+plan | 2 | None |
| 04 | 34 | Auth+prefs | 4 | C6-C12 |
| 05 | 32 | Auth+plan | 2 | C13 |
| 06 | 31 | Auth+cache | 6 | C14-C15 |
| 07 | 37 | Auth | 6 | C16-C21 |
| 08 | 20 | Auth | 2 | None |
| 09 | 56 | Auth+prefs | 3 | C22-C27, C59-C61 |
| 10 | 16 | Auth+plan | 2 | None |
| 11 | 78 | Auth+prefs | 3 | None |
| 12 | 50 | Auth+prefs | 4 | C28-C37 |
| 13 | 26 | Auth+prefs+festivals | 4 | C34-C35* |
| 14 | 36 | Auth+prefs | 4 | C36-C37* |
| 15 | 61 | Auth+plan | 5 | C40-C42* |
| 16 | 51 | Auth+plan+achievements | 5 | C43-C45* |
| 17 | 56 | Auth+pantry | 5 | C38-C39, C46 |
| 18 | 31 | Auth | 5 | C40-C42* |
| 19 | 44 | Auth+plan | 5 | C43-C45* |
| 20 | 40 | Auth+plan | 5 | C40-C42* |
| 21 | 94 | Auth+prefs | 3 | C47-C58 |

*\* = ID collision with another flow (see Section 3.3)*

---

*Generated: 2026-03-04 | Analysis based on: SKILL.md, screen-definitions.md, flow-definitions.md, adb-patterns.md, adb-test-definitions.md, flow01-flow22.md, flow12 report, 24 Compose E2E test files, 28 Compose UI test files, 13 hook files*
