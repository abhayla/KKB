# Flow 11: Settings Deep Dive

## Metadata
- **Flow Name:** `settings-deep-dive`
- **Goal:** Navigate all 12 settings sub-screens, verify data display, test CRUD on family members, modify preferences, and cross-validate with backend API
- **Preconditions:** Authenticated user with preferences set
- **Estimated Duration:** 15-20 minutes
- **Screens Covered:** Settings + 12 sub-screens (Dietary Restrictions, Disliked Ingredients, Cuisine Preferences, Spice Level, Cooking Time, Family Members, Notification Settings, Units, Edit Profile, Friends & Leaderboard, Connected Accounts, Dark Mode Dialog)
- **Depends On:** none (needs authenticated user with preferences)
- **State Produced:** Modified preferences (diet, dislikes, cuisines), family member CRUD, restored at end

## Prerequisites

Beyond standard D1-D7 prerequisites:
- [ ] User authenticated with preferences (from onboarding or prior flow)
- [ ] Backend running (sub-screens sync preferences)
- [ ] App on Home screen

## Test User Persona

Uses existing Sharma family data. Will modify and restore preferences.

**Starting State:**

| Field | Expected Value |
|-------|---------------|
| Dietary Type | Vegetarian (or current) |
| Spice Level | Mild (or current) |
| Cuisines | North Indian, South Indian (or current) |
| Family Members | 0 (or current count) |

**Planned Changes:**

| Setting | Change To | Revert To |
|---------|-----------|-----------|
| Dietary Type | Vegan | Vegetarian |
| Disliked | Add "Bhindi" | Remove "Bhindi" |
| Cuisines | Add "West Indian" | Remove "West Indian" |
| Family Member | Add "Dadaji" (Senior, 70) | Delete "Dadaji" |

## Steps

### Phase A: Navigation Tour — Visit All 12 Sub-Screens (Steps 1-28)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| A1 | Navigate: tap Profile icon | Settings screen | UI | `flow11_settings.png` | — |
| A2 | Tap "Dietary Restrictions" | Dietary Restrictions screen loads | UI | `flow11_dietary.png` | — |
| A3 | Verify radio buttons: Vegetarian, Non-Vegetarian, Vegan, Eggetarian | At least 4 diet options visible | UI | — | — |
| A4 | Verify checkboxes: Jain, Sattvic, Halal | Modifier checkboxes visible | UI | — | — |
| A5 | Press BACK | Return to Settings | UI | — | — |
| A6 | Tap "Disliked Ingredients" | Disliked Ingredients screen loads | UI | `flow11_disliked.png` | — |
| A7 | Verify existing dislikes as chips | Current disliked ingredients shown (e.g., Karela, Lauki) | UI | — | — |
| A8 | Press BACK | Return to Settings | UI | — | — |
| A9 | Tap "Cuisine Preferences" | Cuisine Preferences screen loads | UI | `flow11_cuisine.png` | — |
| A10 | Verify 4 cuisine cards | North Indian, South Indian, East Indian, West Indian | UI | — | — |
| A11 | Press BACK | Return to Settings | UI | — | — |
| A12 | Tap "Spice Level" | Spice Level screen loads | UI | `flow11_spice.png` | — |
| A13 | Verify spice options | Mild, Medium, Spicy, Very Spicy visible | UI | — | — |
| A14 | Press BACK | Return to Settings | UI | — | — |
| A15 | Tap "Cooking Time" | Cooking Time screen loads | UI | `flow11_cooking_time.png` | — |
| A16 | Verify weekday/weekend time + busy day chips | Time values and day chips visible | UI | — | — |
| A17 | Press BACK | Return to Settings | UI | — | — |
| A18 | Scroll down if needed, tap "Family Members" | Family Members screen loads | UI | `flow11_family.png` | — |
| A19 | Verify member list or empty state | Members shown or "No members" | UI | — | — |
| A20 | Press BACK | Return to Settings | UI | — | — |
| A21 | Tap "Notification Settings" | Notification Settings screen loads | UI | `flow11_notif_settings.png` | — |
| A22 | Verify toggle switches for notification types | At least 2 toggles visible | UI | — | — |
| A23 | Press BACK | Return to Settings | UI | — | — |
| A24 | Tap "Units" | Units screen loads | UI | `flow11_units.png` | — |
| A25 | Verify Weight and Volume unit options | Metric/US/Indian for both | UI | — | — |
| A26 | Press BACK | Return to Settings | UI | — | — |
| A27 | Tap "Edit Profile" | Edit Profile screen loads | UI | `flow11_edit_profile.png` | — |
| A28 | Verify name and email fields | Current display name and email visible | UI | — | — |
| A29 | Press BACK | Return to Settings | UI | — | — |
| A30 | Scroll down, tap "Friends & Leaderboard" | Friends screen loads | UI | `flow11_friends.png` | — |
| A31 | Verify screen loads (empty state OK) | Title visible, no crash | UI | — | — |
| A32 | Press BACK | Return to Settings | UI | — | — |
| A33 | Tap "Connected Accounts" | Connected Accounts screen loads | UI | `flow11_connected.png` | — |
| A34 | Verify Google account status | Google connection shown | UI | — | — |
| A35 | Press BACK | Return to Settings | UI | — | — |

### Phase B: Dialog Tests (Steps 29-35)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| B1 | Tap "Dark Mode" item on Settings | Dialog: Light / Dark / System | UI | `flow11_dark_dialog.png` | — |
| B2 | Verify 3 options in dialog | "Light", "Dark", "System" text visible | UI | — | — |
| B3 | Tap "Dark" | Dark theme applied, dialog closes | UI | `flow11_dark_applied.png` | — |
| B4 | Verify Settings is now dark-themed | Dark background on Settings screen | UI | — | — |
| B5 | Tap "Dark Mode" again | Dialog reappears | UI | — | — |
| B6 | Tap "Light" | Light theme restored | UI | — | — |
| B7 | Tap "Items per Meal" (or items count setting) | Number selector or dialog appears | UI | `flow11_items_dialog.png` | — |

### Phase C: CRUD Family Members (Steps 36-44)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| C1 | Tap "Family Members" | Family Members screen | UI | — | — |
| C2 | Tap Add member button (FAB or "+") | Add member dialog/sheet | UI | `flow11_add_member.png` | — |
| C3 | Enter name: "Dadaji" | Name field filled | UI | — | — |
| C4 | Select type: "Senior" | Type selected | UI | — | — |
| C5 | Enter age: "70" | Age entered | UI | — | — |
| C6 | Tap Save/Add | Dadaji appears in member list | UI | `flow11_member_added.png` | — |
| C7 | Tap Edit on Dadaji's card | Edit dialog with current values | UI | — | — |
| C8 | Change age from 70 to 72 | Age updated | UI | — | — |
| C9 | Save edit | Dadaji shows age 72 | UI | `flow11_member_edited.png` | — |
| C10 | Tap Delete on Dadaji's card | Confirmation dialog | UI | — | — |
| C11 | Confirm delete | Dadaji removed from list | UI | `flow11_member_deleted.png` | — |
| C12 | Press BACK | Return to Settings | UI | — | — |

### Phase D: Modify Preferences (Steps 45-54)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| D1 | Tap "Dietary Restrictions" | Dietary screen | UI | — | — |
| D2 | Tap "Vegan" radio button | Vegan selected | UI | — | — |
| D3 | Tap "Sattvic" checkbox | Sattvic enabled | UI | — | — |
| D4 | Tap Save (or BACK if auto-save) | Preferences saved | UI | `flow11_diet_vegan.png` | — |
| D5 | Tap "Disliked Ingredients" | Disliked screen | UI | — | — |
| D6 | Type "Bhindi" in search field | Bhindi suggestion appears | UI | — | — |
| D7 | Add Bhindi to dislikes | Bhindi chip added | UI | `flow11_bhindi_added.png` | — |
| D8 | Tap Save or BACK | Saved | UI | — | — |
| D9 | Tap "Cuisine Preferences" | Cuisine screen | UI | — | — |
| D10 | Tap "West Indian" card | West Indian selected (checkmark appears) | UI | `flow11_west_added.png` | — |
| D11 | Tap Save or BACK | Saved | UI | — | — |

### Phase E: Backend Cross-Validation (Steps 55-56)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| E1 | Run preferences API check | All changes reflected | API | — | HARD |
| E2 | Run family members API check | CRUD verified | API | — | HARD |

```bash
# E1: Verify preferences changes
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/users/me | \
  python -c "
import sys, json
d = json.load(sys.stdin)
checks = []
# Check dietary type changed to Vegan
dt = d.get('dietary_type', '')
checks.append(('dietary_type=Vegan', 'vegan' in dt.lower()))
# Check Bhindi in dislikes
dislikes = d.get('disliked_ingredients', [])
has_bhindi = any('bhindi' in x.lower() for x in dislikes)
checks.append(('disliked_has_Bhindi', has_bhindi))
# Check West Indian in cuisines
cuisines = d.get('cuisines', [])
has_west = any('west' in x.lower() for x in cuisines)
checks.append(('cuisines_has_West_Indian', has_west))
for name, passed in checks:
    print(f'{name}: {\"PASS\" if passed else \"FAIL\"}')
all_pass = all(p for _, p in checks)
print(f'Overall: {\"ALL PASS\" if all_pass else \"SOME FAILED\"}')"

# E2: Verify family member CRUD (Dadaji should be deleted by now)
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/family-members | \
  python -c "
import sys, json
d = json.load(sys.stdin)
members = d if isinstance(d, list) else d.get('members', [])
dadaji = [m for m in members if 'dadaji' in m.get('name', '').lower()]
print(f'Total members: {len(members)}')
print(f'Dadaji exists: {len(dadaji) > 0} (expected: False after delete)')
if not dadaji:
    print('Family member CRUD -> PASS')
else:
    print('WARNING: Dadaji still exists after deletion')"
```

### Phase F: Revert & Cleanup (Steps 57-61)

| Step | Action | Expected | Type | Screenshot | Validation |
|------|--------|----------|------|------------|------------|
| F1 | Tap "Dietary Restrictions" | Dietary screen | UI | — | — |
| F2 | Tap "Vegetarian" radio | Restore to Vegetarian | UI | — | — |
| F3 | Uncheck "Sattvic" if checked | Sattvic removed | UI | — | — |
| F4 | Save or BACK | Saved | UI | — | — |
| F5 | Tap "Disliked Ingredients" | Disliked screen | UI | — | — |
| F6 | Remove "Bhindi" chip | Bhindi removed | UI | — | — |
| F7 | Save or BACK | Saved | UI | — | — |
| F8 | Tap "Cuisine Preferences" | Cuisine screen | UI | — | — |
| F9 | Deselect "West Indian" card | West Indian removed | UI | — | — |
| F10 | Save or BACK | Saved | UI | — | — |
| F11 | Press BACK to Settings, then BACK to Home | Home screen | UI | `flow11_final.png` | — |

## Validation Checkpoints

1. **Checkpoint 1 (Phase E):** All preference changes reflected in `GET /api/v1/users/me`
2. **Checkpoint 2 (Phase E):** Family member CRUD verified via `GET /api/v1/family-members`

## Fix Strategy

**Relevant files for this flow:**
- Settings main: `app/presentation/settings/SettingsViewModel.kt`, `SettingsScreen.kt`
- Sub-screens: `app/presentation/settings/screens/` (11 screen files)
- Repository: `data/repository/SettingsRepositoryImpl.kt`
- Backend users: `backend/app/api/v1/endpoints/users.py`
- Backend family: `backend/app/api/v1/endpoints/family_members.py`
- Navigation: `app/presentation/navigation/RasoiNavGraph.kt`

**Common issues:**
- Sub-screen doesn't load → check navigation route in RasoiNavGraph
- Save not persisting → check SettingsRepository.updateUserPreferences()
- Family member CRUD fails → check backend /api/v1/family-members endpoints
- Dark mode dialog not appearing → check SettingsScreen dialog trigger

## Screen Coverage

| Screen | Step(s) | Verification |
|--------|---------|-------------|
| Settings | A1, A5, A8, A11, A14, A17, A20, A23, A26, A29, A32, A35 | Navigation hub |
| Dietary Restrictions | A2-A4, D1-D4, F1-F4 | View + modify + revert |
| Disliked Ingredients | A6-A7, D5-D8, F5-F7 | View + modify + revert |
| Cuisine Preferences | A9-A10, D9-D11, F8-F10 | View + modify + revert |
| Spice Level | A12-A13 | View only |
| Cooking Time | A15-A16 | View only |
| Family Members | A18-A19, C1-C12 | Full CRUD |
| Notification Settings | A21-A22 | View toggles |
| Units | A24-A25 | View options |
| Edit Profile | A27-A28 | View fields |
| Friends & Leaderboard | A30-A31 | View (empty state OK) |
| Connected Accounts | A33-A34 | View status |
| Dark Mode Dialog | B1-B6 | Toggle dark/light |
