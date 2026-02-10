# RasoiAI Gap Analysis Report

**Date:** 2026-02-10
**Scope:** Deep verification of ~525 BDD requirements across 12 screen specifications
**Method:** Cross-referenced BDD requirements against actual codebase implementation

---

## Executive Summary

| Category | Gaps Found | Immediate | Deferred |
|----------|-----------|-----------|----------|
| Functional | 17 | 17 | 0 |
| Code Quality | 7 | 7 | 0 |
| Performance | 3 | 3 | 0 |
| Security | 6 | 0 | 6 |
| Deployment | 6 | 0 | 6 |
| **Total** | **39** | **27** | **12** |

---

## Phase 1A: Quick Wins (Functional)

### GAP-001: Auth Screen — Terms of Service URL Navigation
- **BDD Ref:** AUTH-010
- **Severity:** Low
- **Issue:** #17
- **Description:** Terms of Service link exists but navigates nowhere
- **Fix:** Add URL intent launcher in AuthScreen.kt

### GAP-002: Auth Screen — Privacy Policy URL Navigation
- **BDD Ref:** AUTH-011
- **Severity:** Low
- **Issue:** #18
- **Description:** Privacy Policy link exists but navigates nowhere
- **Fix:** Add URL intent launcher in AuthScreen.kt

### GAP-003: Chat Screen — Settings Navigation from Menu
- **BDD Ref:** CHAT-009
- **Severity:** Low
- **Issue:** #19
- **Description:** Chat menu item for settings not connected to navigation
- **Fix:** Wire up settings navigation in ChatScreen.kt

### GAP-004: Home Screen — Festival Recipes Navigation
- **BDD Ref:** HOME-015
- **Severity:** Low
- **Issue:** #22
- **Description:** Festival card tap doesn't navigate to festival recipes
- **Fix:** Add navigation callback in HomeScreen/ViewModel

### GAP-005: Favorites Screen — Undo Snackbar on Removal
- **BDD Ref:** FAV-018
- **Severity:** Low
- **Issue:** #55
- **Description:** Removing a recipe from favorites has no undo option
- **Fix:** Add Snackbar with undo action in FavoritesScreen

### GAP-006: Notifications Screen — Clear All Button
- **BDD Ref:** NOTIF-013
- **Severity:** Low
- **Issue:** #56
- **Description:** No bulk clear option for notifications
- **Fix:** Add Clear All button in NotificationsScreen

---

## Phase 1B: Medium Features (Functional)

### GAP-007: Recipe Detail — More Options Menu
- **BDD Ref:** RECIPE-020
- **Severity:** Low
- **Issue:** #25
- **Description:** More options menu (share, report, add to collection) not implemented
- **Fix:** Add dropdown menu in RecipeDetailScreen

### GAP-008: Bottom Navigation — Unread Badge Count
- **BDD Ref:** COM-007
- **Severity:** Medium
- **Issue:** #57
- **Description:** Bottom navigation doesn't show unread notification count
- **Fix:** Add badge to RasoiBottomNavigation component

### GAP-009: Offline Banner Component
- **BDD Ref:** COM-008
- **Severity:** Medium
- **Issue:** #58
- **Description:** No visible indicator when app is offline
- **Fix:** Create OfflineBanner composable in core/ui

### GAP-010: Chat Screen — Contextual Help
- **BDD Ref:** CHAT-015
- **Severity:** Medium
- **Issue:** #24
- **Description:** Chat doesn't receive context about what screen user came from
- **Fix:** Pass navigation arguments to ChatScreen

### GAP-011: Stats Screen — Share Intent
- **BDD Ref:** STATS-010
- **Severity:** Low
- **Issue:** #30
- **Description:** No share button for achievements/stats
- **Fix:** Add Android share intent in StatsScreen

### GAP-012: Cooking Mode — Recipe Rating Submission
- **BDD Ref:** COOK-013
- **Severity:** Medium
- **Issue:** #21
- **Description:** Rating collected in UI but not submitted to backend
- **Fix:** Add backend endpoint + wire up CookingModeViewModel

---

## Phase 1C: Larger Features (Functional)

### GAP-013: Achievements Screen
- **BDD Ref:** STATS-015
- **Severity:** Low
- **Issue:** #27
- **Description:** Achievements screen navigation exists but screen is placeholder
- **Fix:** Build full AchievementsScreen with ViewModel

### GAP-014: Leaderboard Screen
- **BDD Ref:** STATS-016
- **Severity:** Low
- **Issue:** #28
- **Description:** Leaderboard screen navigation exists but screen is placeholder
- **Fix:** Build full LeaderboardScreen with ViewModel

### GAP-015: Chat Voice Input
- **BDD Ref:** CHAT-012
- **Severity:** Medium
- **Issue:** #12
- **Description:** Voice input button exists but SpeechRecognizer not implemented
- **Fix:** Implement Android SpeechRecognizer in ChatInputBar

### GAP-016: Pantry Camera Capture
- **BDD Ref:** PANTRY-010
- **Severity:** Medium
- **Issue:** #14
- **Description:** Camera button exists but CameraX not implemented
- **Fix:** Implement CameraX integration in PantryScreen

### GAP-017: Pantry Gallery Selection
- **BDD Ref:** PANTRY-011
- **Severity:** Medium
- **Issue:** #15
- **Description:** Gallery button exists but PhotoPicker not implemented
- **Fix:** Implement PhotoPicker in PantryScreen

---

## Phase 2: Code Quality

### GAP-018: postgres.py Missing Model Imports
- **Severity:** Low (no runtime impact — models loaded elsewhere)
- **Issue:** #59
- **Description:** `notification` and `recipe_rule` models not imported in 3 import blocks
- **Fix:** Add imports to init_db, create_tables, drop_tables blocks

### GAP-019: Auth Test Failures from conftest Override
- **Severity:** Low (known issue, not a regression)
- **Issue:** #60
- **Description:** 4 auth tests fail because conftest globally overrides auth dependency
- **Fix:** Conditional auth override in conftest.py

### GAP-020: OnboardingViewModelTest Compilation Error
- **Severity:** Low (known issue, not a regression)
- **Issue:** #61
- **Description:** Missing `generateMealPlanUseCase` constructor parameter
- **Fix:** Add missing parameter to test setup

### GAP-021: Broad Exception Handling in Repositories
- **Severity:** Medium
- **Issue:** #34
- **Description:** Repository implementations catch `Exception` broadly, hiding real errors
- **Fix:** Use specific exception types (IOException, HttpException)

### GAP-022: SplashScreen Missing Tests
- **Severity:** Low
- **Issue:** #37
- **Description:** SplashScreen has no UI tests
- **Fix:** Create SplashScreenTest.kt

### GAP-023: NotificationsScreen Missing Tests
- **Severity:** Low
- **Issue:** #36
- **Description:** NotificationsScreen has no UI tests
- **Fix:** Create NotificationsScreenTest.kt

### GAP-024: Backend CI/CD Pipeline Missing
- **Severity:** Medium
- **Issue:** #62
- **Description:** No GitHub Actions workflow for backend tests
- **Fix:** Create `.github/workflows/backend-ci.yml`

---

## Phase 3: Performance

### GAP-025: Meal Generation No Async Timeout
- **Severity:** Medium
- **Issue:** #63
- **Description:** Gemini API call has no timeout, can block indefinitely
- **Fix:** Add `asyncio.wait_for()` with 60s timeout

### GAP-026: Recipe Search Not Pushed to Database
- **Severity:** Low
- **Issue:** #64
- **Description:** Recipe search filters in Python after full DB fetch
- **Fix:** Push ILIKE filtering to SQL query

### GAP-027: PostgreSQL Engine Missing Query Timeouts
- **Severity:** Low
- **Issue:** #65
- **Description:** No statement_timeout or pool_timeout configured
- **Fix:** Add timeout settings to create_async_engine config

---

## Deferred: Security (Future Sprint)

### GAP-028: CORS Wildcard Configuration
- **Issue:** #66
- **Description:** `CORS_ORIGINS=["*"]` allows any origin

### GAP-029: DEBUG Default + JWT Secret Fallback
- **Issue:** #67
- **Description:** DEBUG defaults to True, JWT has hardcoded fallback secret

### GAP-030: No Rate Limiting
- **Issue:** #68
- **Description:** No rate limiting middleware on any endpoint

### GAP-031: No HTTPS Enforcement
- **Issue:** #69
- **Description:** No secure headers or HTTPS redirect middleware

### GAP-032: Hardcoded DB Password in docker-compose
- **Issue:** #70
- **Description:** Database password visible in docker-compose.yml

### GAP-033: Missing Input Sanitization Audit
- **Issue:** (deferred — no issue created)
- **Description:** No systematic input sanitization review completed

---

## Deferred: Deployment (Future Sprint)

### GAP-034: No Deployment Documentation
- **Issue:** #71

### GAP-035: No Database Backup Strategy
- **Issue:** #72

### GAP-036: No Monitoring/Observability
- **Issue:** #73

### GAP-037: No Production Environment Config
- **Issue:** #74

### GAP-038: No Health Check Endpoint
- **Issue:** (deferred — no issue created)

### GAP-039: No Load Testing
- **Issue:** (deferred — no issue created)

---

## Traceability Matrix

| Gap ID | BDD Ref | Issue # | Phase | Status |
|--------|---------|---------|-------|--------|
| GAP-001 | AUTH-010 | #17 | 1A | Pending |
| GAP-002 | AUTH-011 | #18 | 1A | Pending |
| GAP-003 | CHAT-009 | #19 | 1A | Pending |
| GAP-004 | HOME-015 | #22 | 1A | Pending |
| GAP-005 | FAV-018 | #55 | 1A | Pending |
| GAP-006 | NOTIF-013 | #56 | 1A | Pending |
| GAP-007 | RECIPE-020 | #25 | 1B | Pending |
| GAP-008 | COM-007 | #57 | 1B | Pending |
| GAP-009 | COM-008 | #58 | 1B | Pending |
| GAP-010 | CHAT-015 | #24 | 1B | Pending |
| GAP-011 | STATS-010 | #30 | 1B | Pending |
| GAP-012 | COOK-013 | #21 | 1B | Pending |
| GAP-013 | STATS-015 | #27 | 1C | Pending |
| GAP-014 | STATS-016 | #28 | 1C | Pending |
| GAP-015 | CHAT-012 | #12 | 1C | Pending |
| GAP-016 | PANTRY-010 | #14 | 1C | Pending |
| GAP-017 | PANTRY-011 | #15 | 1C | Pending |
| GAP-018 | — | #59 | 2 | Pending |
| GAP-019 | — | #60 | 2 | Pending |
| GAP-020 | — | #61 | 2 | Pending |
| GAP-021 | — | #34 | 2 | Pending |
| GAP-022 | — | #37 | 2 | Pending |
| GAP-023 | — | #36 | 2 | Pending |
| GAP-024 | — | #62 | 2 | Pending |
| GAP-025 | — | #63 | 3 | Pending |
| GAP-026 | — | #64 | 3 | Pending |
| GAP-027 | — | #65 | 3 | Pending |
| GAP-028–039 | — | #66–#74 | Deferred | Deferred |
