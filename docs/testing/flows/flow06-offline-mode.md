# Flow 06: Offline Mode

## Metadata
- **Flow Name:** `offline-mode`
- **Goal:** Verify Room cache works offline, test offline mutations, and sync on reconnect (C14-C15)
- **Preconditions:** User has meal plan and preferences cached in Room
- **Estimated Duration:** 6-10 minutes
- **Screens Covered:** Home, Grocery, Favorites, Recipe Detail, Settings
- **Depends On:** none (needs cached data in Room)
- **State Produced:** Potential unsynced offline changes

## Prerequisites

Beyond standard D1-D7 prerequisites:
- [ ] User has at least one meal plan (cached in Room)
- [ ] App has been used online (Room has data)
- [ ] Emulator has WiFi/data toggle capability

## Test User Persona

Uses existing Sharma family data. Tests offline cache, not settings changes.

## Steps

### Phase A: Go Offline (Steps 1-3)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| A1 | Disable WiFi and mobile data via ADB | Network disabled | — | — |
| A2 | Verify offline: `$ADB shell ping -c 1 google.com` should fail | ping fails / network unreachable | — | — |
| A3 | Wait 2s for app to detect offline state | NetworkMonitor should detect offline | — | — |

**ADB commands to go offline:**
```bash
$ADB shell svc wifi disable
$ADB shell svc data disable
```

### Phase B: Verify Room Cache (Steps 4-10)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| B1 | Force-stop and relaunch app | App starts | — | — |
| B2 | Wait for Home screen | Home loads from Room cache (may show offline indicator) | `flow06_home_offline.png` | — |
| B3 | Verify meal plan still displays | Recipe names visible from cached plan | — | — |
| B4 | Tap a day tab (e.g., THU) | Thursday meals displayed from cache | — | — |
| B5 | Tap bottom nav "Grocery" | Grocery loads from Room cache | `flow06_grocery_offline.png` | — |
| B6 | Verify grocery items visible | Cached grocery items with categories | — | — |
| B7 | Tap bottom nav "Favs" | Favorites loads from Room | `flow06_favorites_offline.png` | — |
| B8 | Tap bottom nav "Home" | Return to Home | — | — |
| B9 | Tap a meal card → "View Recipe" | Recipe Detail loads from cache | `flow06_recipe_offline.png` | — |
| B10 | Verify ingredients section | Ingredients visible from cached data | — | — |

### Phase C: Offline Mutations — C14 & C15 (Steps 11-16)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| C1 | **C14 Setup:** Navigate to Settings | Settings loads from cache/DataStore | — | — |
| C2 | **C14:** Change a dietary setting (e.g., toggle strict allergen) | Setting changes locally | `flow06_c14_offline_change.png` | — |
| C3 | Note: change queued for sync (OfflineQueue) | No error/crash despite no network | — | — |
| C4 | Press BACK to Home | Home screen | — | — |
| C5 | **C15:** Tap a meal card → "View Recipe" | Recipe Detail | — | — |
| C6 | **C15:** Tap Favorite button | Heart toggles (saved in Room) | `flow06_c15_offline_fav.png` | — |
| C7 | Note: favorite saved locally | No error despite offline | — | — |

### Phase D: Attempt Online-Only Action (Steps 17-19)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| D1 | Navigate to Home | Home screen | — | — |
| D2 | Tap Refresh/Regenerate | Attempt to regenerate meal plan | — | — |
| D3 | Verify offline handling | Error message, snackbar, or disabled button — NOT a crash | `flow06_regen_offline.png` | — |

### Phase E: Restore Network & Verify Sync (Steps 20-27)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| E1 | Re-enable WiFi and data | Network restored | — | — |
| E2 | Wait 5s for app to detect online | NetworkMonitor detects reconnection | — | — |
| E3 | **C14 Verify:** Check if dietary setting synced | Backend should have updated preference | — | — |
| E4 | **C15 Verify:** Navigate to Favorites | Favorited recipe should appear | `flow06_favorites_online.png` | — |
| E5 | Verify favorited recipe still in list | Recipe from C6 visible | — | — |
| E6 | Navigate to Settings | Verify setting from C2 persisted | — | — |
| E7 | Verify no data corruption | All screens load normally | — | — |
| E8 | Run crash/ANR detection (Pattern 9) | No crashes | — | — |

**ADB commands to restore network:**
```bash
$ADB shell svc wifi enable
$ADB shell svc data enable
```

## Validation Checkpoints

No `validate_meal_plan.py` checkpoints — validation is offline-behavior:
- Room cache serves data without network
- Offline mutations don't crash
- Data syncs on reconnect
- No data corruption after offline period

## Fix Strategy

**Relevant files for this flow:**
- NetworkMonitor: `core/src/main/java/com/rasoiai/core/network/NetworkMonitor.kt`
- Offline queue: `data/local/dao/OfflineQueueDao.kt`, `data/local/entity/OfflineQueueEntity.kt`
- Repository offline handling: `data/repository/*RepositoryImpl.kt` (all follow offline-first pattern)
- Room cache: `data/local/RasoiDatabase.kt`, all DAOs

**Common issues:**
- App crashes without network → missing null checks on API responses
- Room cache empty → data wasn't cached on initial load
- Offline changes lost on reconnect → OfflineQueue not processing
- ADB network commands need root on some emulators → use airplane mode toggle instead

## Screen Coverage

| Screen | Step(s) | Verification |
|--------|---------|-------------|
| Home | B2-B4, D1-D3 | Cached plan, offline regen attempt |
| Grocery | B5-B6 | Cached grocery list |
| Favorites | B7, E4-E5 | Cached favorites, post-sync |
| Recipe Detail | B9-B10, C5-C6 | Cached recipe, offline favorite |
| Settings | C1-C3, E6 | Offline setting change, sync verify |
