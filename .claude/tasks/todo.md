# Task Tracker

<!-- Claude maintains this file during implementation sessions. -->
<!-- Format: checkable items grouped by task, marked complete as work progresses. -->

## Current Task

Loop iteration on 2026-04-22 — PR #89 follow-on work. Issue #34 (broad exception handling in repositories) now extended to MealPlanRepositoryImpl as the fourth sibling in the sweep.

### Pending

- [ ] **#34 sweep extension to remaining 8 files** (optional) — ~88 broad `catch (e: Exception)` instances remain across HouseholdRepositoryImpl, RecipeRulesRepositoryImpl, NotificationRepositoryImpl, AuthRepositoryImpl, GroceryRepositoryImpl, SettingsRepositoryImpl, PantryRepositoryImpl, StatsRepositoryImpl. Same TDD pattern (RED test → narrow to typed catch → GREEN).

See "Blocked" below for items still requiring user/emulator action.

## Completed (2026-04-22 loop iteration)

- [x] **#34 FavoritesRepositoryImpl exception narrowing** (605ee1b) — 7 broad `catch (e: Exception)` → `catch (e: SQLiteException)`. 9 new TDD tests (7 propagation + 2 contract). All 28 tests in file pass.
- [x] **#34 RecipeRepositoryImpl exception narrowing** (68598b7) — 14 broad catches narrowed to HttpException/IOException/SQLiteException by call type. Inner forEach swallows removed (fetchAndCacheRecipe handles known errors). 6 new TDD tests + 1 pre-existing test updated to assert new contract. All 23 tests in file pass.
- [x] **#34 ChatRepositoryImpl exception narrowing** (b49ce01) — 3 of 4 broad catches narrowed (SQLiteException for DB, dropped outer for sendImageMessage). compressAndEncodeImage broad catch retained with documented justification (BitmapFactory/Base64 exception diversity). 4 new TDD tests + 2 pre-existing tests rewritten (one was passing for the wrong reason via `Uri.parse` RuntimeException being silently wrapped). Full `:data:testDebugUnitTest` green (360 tests).
- [x] **#34 MealPlanRepositoryImpl exception narrowing** (pending commit) — 12 of 17 broad catches narrowed: 8 outers → SQLiteException (generateMealPlan, swapMeal, setMealLockState, removeRecipeFromMeal, addRecipeToMeal, syncMealPlans, fetchAndCacheMealPlan, setDayLockState, setMealTypeLockState — note fetchAndCacheMealPlan returns null not Result), 3 inner sync-to-server catches → HttpException (setMealLockState, removeRecipeFromMeal, syncMealPlans per-plan loop). 5 inner side-effect broad catches around `recipeRepository.prefetchRecipes` and `groceryRepository.generateFromMealPlan` intentionally retained with inline `#34` justification per ChatRepositoryImpl `compressAndEncodeImage` precedent — these are fire-and-forget side effects that must not invalidate the already-persisted meal plan. 13 new TDD tests (8 propagation + 5 SQLiteException-wrap contract) + 2 pre-existing tests updated (`generateMealPlan API error` + `syncMealPlans continue on fail` now throw realistic `retrofit2.HttpException` instead of `RuntimeException` — the old tests were passing via broad swallowing). 34/34 MealPlanRepositoryImplTest tests pass, full `:data:testDebugUnitTest` green.

## Completed (2026-04-21 loop iteration)

- [x] **Commit Room v15.json schema** (5d200ed) — completes MIGRATION_14_15.
- [x] **Verify #27 + #28 nav** — both wired already, draft close notes prepared.
- [x] **#24 handleInitialContext tests** (e231e55) — 5 TDD tests.
- [x] **#21 rate_recipe service tests** (8dfa626) — 7 TDD tests.
- [x] **#21 Part 2 rating aggregation backend+Android** (a48fb10) — atomic 9-file change with 14 new tests (8 backend service + 2 Android DTO mapper + 4 domain model).
- [x] **#21 Part 2 UI display** (0850520) — RatingRow composable in RecipeHeader, threaded from RecipeDetailScreen.
- [x] **#57 notificationBadgeCount tests** (7b8ca92) — 3 TDD tests covering ViewModel flow propagation.
- [x] **#58 isOffline propagation tests** (338585a) — 3 TDD tests covering network monitor flow.
- [x] **7 commits pushed** to `origin/loop/repo-exceptions-and-hook-fixes` — PR #89 CI running.

## Final sweep findings (no code change needed)

- **#66 CORS wildcard**: `backend/app/config.py` already defaults `cors_origins = []` (no CORS middleware). A wildcard would only appear if a deployment sets `CORS_ORIGINS=*` via env var — that is a deployment-hygiene issue, not a code bug. Code is safe as-is.
- **#69 HTTPS + secure headers**: `SecurityHeadersMiddleware` in `backend/app/main.py` already adds X-Content-Type-Options, X-Frame-Options, X-XSS-Protection, Referrer-Policy, and HSTS (non-debug only) per `.claude/rules/security-headers-middleware.md`. HTTPS redirect is intentionally at the reverse-proxy layer (nginx/Caddy on VPS), not in FastAPI — adding `HTTPSRedirectMiddleware` in the app would double-redirect. Code is correct.

Both issues can be closed with a short deployment note when the user gets to them.

## Blocked (not autonomously actionable)

| Issue | Reason | Owner |
|-------|--------|-------|
| Merging PR #89 | CI settling + `claude-review` needs GitHub App install + ANTHROPIC_API_KEY secret | User |
| Closing stale issues (#21 #24 #25 #27 #28 #30 #34 #35 #38 #57 #58 #67 #70) | `gh issue close` is an external visible action | User |
| #12 voice input | Requires Android microphone permission + emulator audio stack | Emulator-gated |
| #14 camera / #15 gallery | Requires camera permissions + emulator | Emulator-gated |
| #36 #37 #78 #79 E2E tests | Requires Compose instrumented runner | Emulator-gated |
| #75 ADB onboarding dropdown bug | Requires live ADB session with emulator | Emulator-gated |
| #11 release signing | Requires keystore files + env vars | User credentials |
| #66 #68 #69 #72 #73 #74 security/deploy | Labelled `deferred`; infra scope | Product decision |

## Review

### What went well
- 7 commits scoped and reviewable independently. Each either adds tests for an existing behavior (characterization TDD) or adds a feature in atomic backend+Android sync per `.claude/rules/pydantic-android-schema-sync.md`.
- Every implementation change included tests in the same commit.
- `**overrides` collision on `_persist_minimal_recipe` caught on first run; fixed via `defaults.update(overrides)` per `.claude/rules/test-data-factories-make-pattern.md`.
- Rating aggregation backend used `func.avg` + `func.count` in a single query — no N+1.
- Compose `RecipeHeader` gained only optional params with defaults → all existing callers continue to compile.
- Rating fields on `Recipe` domain class are all-val primitives/nullable-primitives → Compose stability preserved without changes to `compose-stability.conf`.

### What to watch
- `claude-review` CI consistently fails — needs the user to install the Claude Code GitHub App and set `ANTHROPIC_API_KEY` repo secret.
- Instrumented Tests job relies on GitHub-hosted API 29 emulator queue which has been extremely backed up; PR #89 shows `Instrumented Tests: pending (0s)` repeatedly.
- `MealPlanItemEntity.order` → `item_order` Room migration is live at v15. Any future entity change must bump to v16 with a matching migration.
- Rating "stored locally" acceptance criterion (#21) is intentionally deferred — Room has no recipe_ratings table. Would need a v15→v16 migration + DAO. Not in scope for this branch.

### Follow-ups (if resumed)
- Consider adding a Room `recipe_ratings` table + DAO to cache ratings for offline display; add `ratingCount`/`averageRating`/`userRating` to `RecipeEntity` to persist the aggregate on the last fetched detail.
- Consider adding a pure-JVM Compose snapshot test for `RatingRow` + `OfflineBanner` using Paparazzi (non-emulator) to improve UI regression safety.
- Switch CI Instrumented Tests to API 34 to match local dev and avoid the API 29 hosted-runner queue.
