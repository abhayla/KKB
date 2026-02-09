# ADB Test Definitions — Per-Screen Checklists

Reference file for `/adb-test` command. Each screen defines:
- **Navigation path** — ADB taps to reach the screen from Home
- **Primary identifier** — text/content-desc to verify arrival
- **Required elements** — text or content-desc values to find in uiautomator XML
- **Interactive elements** — click targets with expected results
- **Data validation** — what data should appear when the screen has content
- **Known issues** — previously identified quirks

**Important:** Compose `testTag()` values are NOT visible in uiautomator XML. All searches use `text`, `content-desc`, `resource-id`, or `class` attributes.

---

## Screen 1: auth-flow

**Navigation:** App launch (fresh install or after sign-out)
**Last Verified:** 2026-02-09 | **Definition Confidence:** HIGH

**Primary Identifier:** text="Sign in with Google" OR text="Welcome"

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| App logo/title | text | "RasoiAI" or app name |
| Welcome text | text | Contains "Welcome" or "Sign in" |
| Google sign-in button | text | "Sign in with Google" |
| App tagline | text | Contains "meal" or "cooking" or "family" |

### Interactive Elements

| Action | Target | Expected Result |
|--------|--------|-----------------|
| Tap "Sign in with Google" | text="Sign in with Google" | Navigates to Google auth flow (fake auth returns immediately) → Onboarding or Home |

### Data Validation

- No data expected on auth screen (pre-login)
- After sign-in, should transition to Onboarding (first-time) or Home (returning user)

### Known Issues

- Fake auth (`fake-firebase-token`) bypasses Google OAuth, returns immediately
- E2E test email is `e2e-test@rasoiai.test` (changed from `abhayinfosys@gmail.com`)

---

## Screen 2: home

**Navigation:** Post-auth landing page (after auth + onboarding + meal generation)
**Last Verified:** 2026-02-09 | **Definition Confidence:** MEDIUM

**Primary Identifier:** text="This Week's Menu" OR text="BREAKFAST" OR text contains day names (MON, TUE, etc.)

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Screen title | text | "This Week's Menu" |
| Week selector | text | Day abbreviations (M, T, W, T, F, S, S) or full names |
| Breakfast section | text | "BREAKFAST" (uppercase) |
| Lunch section | text | "LUNCH" (uppercase) |
| Dinner section | text | "DINNER" (uppercase) | ⬇️ Below fold — requires scroll |
| Snacks section | text | "SNACKS" (uppercase) | ⬇️ Below fold — requires scroll |
| Menu/hamburger icon | content-desc | "Menu" or "Navigation" |
| Notifications icon | content-desc | "Notifications" |
| Profile icon | content-desc | "Profile" or "Settings" |
| Bottom nav - Home | text | "Home" |
| Bottom nav - Grocery | text | "Grocery" |
| Bottom nav - Chat | text | "Chat" |
| Bottom nav - Favs | text | "Favs" or "Favorites" |
| Bottom nav - Stats | text | "Stats" |
| Day lock button | content-desc | Contains "lock" or "Lock" |
| Refresh button | content-desc | Contains "Refresh" or "Regenerate" |

### Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap day tab (e.g., TUE) | text containing day name | Day meals update, selected day highlighted | Already on Home |
| Tap BREAKFAST card | text="BREAKFAST" area, tap below header | Action sheet appears (View Recipe, Swap, Lock, Remove) | Press BACK or tap outside |
| Tap lock icon (per meal) | content-desc contains "lock" near meal | Lock icon toggles (locked/unlocked) | Already on Home |
| Tap refresh button | content-desc contains "Refresh" | Refresh options sheet (This Day Only / Entire Week) | Press BACK |
| Tap Menu/hamburger | content-desc="Menu" | Navigation drawer or Settings screen | Press BACK |
| Tap Notifications | content-desc="Notifications" | Notifications screen | Press BACK |
| Tap Profile | content-desc="Profile" | Settings screen | Press BACK |
| Tap bottom nav Grocery | text="Grocery" | Grocery screen | Tap "Home" bottom nav |
| Tap bottom nav Chat | text="Chat" | Chat screen | Tap "Home" bottom nav |
| Tap bottom nav Favs | text="Favs" | Favorites screen | Tap "Home" bottom nav |
| Tap bottom nav Stats | text="Stats" | Stats screen | Tap "Home" bottom nav |
| Tap Add button on meal | content-desc="Add" near meal section | Add Recipe sheet | Press BACK |

### Data Validation

- Each meal section should show at least 1-2 recipe names (not empty)
- Recipe names should be real Indian dish names (not placeholder text)
- Day selector should show current week's dates
- At least one day should have all 4 meal sections visible (after scrolling)

### Known Issues

- Meal cards require scrolling to see DINNER/SNACKS sections
- Lock state may not persist across app restarts (Room sync issue)
- Day lock button may be hard to find in XML — look in the day header row near refresh button
- Festival banner only appears on festival days (not always visible)

---

## Screen 3: grocery

**Navigation:** Home → tap bottom nav "Grocery" (text="Grocery")
**Last Verified:** 2026-02-09 | **Definition Confidence:** MEDIUM

**Primary Identifier:** text contains "Grocery" as screen title OR text="grocery_screen"

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Screen title | text | "Grocery List" or "Grocery" |
| Week header | text | Date range or "This Week" |
| Category sections | text | Category names like "Vegetables", "Spices", "Dairy", "Grains" |
| Grocery items | text | Ingredient names with quantities |
| WhatsApp share | content-desc | Contains "WhatsApp" or "Share" |
| Total items count | text | Contains number + "items" |
| Bottom navigation | text | "Home", "Grocery", etc. |

### Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap checkbox on item | Checkbox near ingredient name | Item marked as purchased (strikethrough) | Already on Grocery |
| Tap category header | text=category name (e.g., "Vegetables") | Category expands/collapses | Already on Grocery |
| Tap WhatsApp share | content-desc contains "WhatsApp" or "Share" | Share intent / WhatsApp opens | Press BACK to return |
| Tap bottom nav Home | text="Home" | Home screen | Tap "Grocery" bottom nav |

### Data Validation

- Grocery items should correspond to ingredients from the current meal plan
- Quantities should include Indian measurements where applicable (katori, chammach)
- At least 3-5 categories should be visible
- Items should have meaningful names (not empty or placeholder)

### Known Issues

- WhatsApp share may not work if WhatsApp is not installed on emulator
- Category expansion state may reset on screen rotation

---

## Screen 4: chat

**Navigation:** Home → tap bottom nav "Chat" (text="Chat")
**Last Verified:** 2026-02-09 | **Definition Confidence:** MEDIUM

**Primary Identifier:** text contains "Chat" as title OR presence of text input field (class=`android.widget.EditText`)

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Screen title | text | "Chat" or "AI Assistant" |
| Input field | class | `android.widget.EditText` or text="Type a message" |
| Send button | content-desc | "Send" |
| Welcome/intro message | text | Contains "Hi" or "Hello" or "How can I help" |
| Bottom navigation | text | "Home", "Chat", etc. |

### Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap input field | class=EditText | Keyboard appears, field focused | Already on Chat |
| Type message + tap Send | Input "Add chai to breakfast" → tap Send | Message appears in chat, AI response after 5-15s | Already on Chat |
| Tap bottom nav Home | text="Home" | Home screen | Tap "Chat" bottom nav |

### Data Validation

- AI response should appear within 30 seconds
- Response should be contextual (about food/meals)
- Chat history should persist across screen navigation

### Known Issues

- AI response requires Claude API call — may take 5-30 seconds
- Tool calling (update_recipe_rule etc.) happens server-side, results shown as text
- Keyboard may obscure chat messages — may need to scroll

---

## Screen 5: favorites

**Navigation:** Home → tap bottom nav "Favs" (text="Favs" or "Favorites")
**Last Verified:** 2026-02-09 | **Definition Confidence:** MEDIUM

**Primary Identifier:** text contains "Favorites" OR text="No favorites yet" (empty state)

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Screen title | text | "Favorites" |
| Recipe cards OR empty state | text | Recipe names OR "No favorites yet" |
| Search button | content-desc | "Search" |
| Bottom navigation | text | "Home", "Favs", etc. |

### Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap recipe card (if exists) | text=recipe name | Navigate to Recipe Detail | Press BACK |
| Tap search icon | content-desc="Search" | Search field appears | Press BACK or tap search again |
| Tap bottom nav Home | text="Home" | Home screen | Tap "Favs" bottom nav |

### Data Validation

- If user has favorited recipes, they should appear as cards
- If no favorites, empty state message should be clear and helpful
- Recipe cards should show name, cuisine type, prep time

### Known Issues

- Empty state is expected on fresh test accounts (no favorites yet)
- Favorites are stored in Room — no backend sync for favorites list

---

## Screen 6: stats

**Navigation:** Home → tap bottom nav "Stats" (text="Stats")
**Last Verified:** 2026-02-09 | **Definition Confidence:** MEDIUM

**Primary Identifier:** text contains "Stats" OR text contains "Cooking" as screen title

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Screen title | text | "Stats" or "Cooking Stats" |
| Time period tabs | text | "Week", "Month", "All Time" or similar |
| Cooking streak | text | Contains "streak" or number + "days" |
| Cuisine chart | content-desc | Contains "chart" or "cuisine" |
| Bottom navigation | text | "Home", "Stats", etc. |

### Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap time period tab | text="Month" or "All Time" | Stats data updates for selected period | Already on Stats |
| Tap bottom nav Home | text="Home" | Home screen | Tap "Stats" bottom nav |

### Data Validation

- Stats may show zeros for fresh accounts (expected)
- If cooking has been logged, streak should be > 0
- Cuisine chart should show cuisine distribution if data exists

### Known Issues

- Stats screen may be empty for fresh test accounts
- No dedicated backend tests for stats endpoints
- Chart rendering may not be visible in uiautomator XML (Canvas-based)

---

## Screen 7: settings

**Navigation:** Home → tap Profile icon (content-desc="Profile" or "Settings") in top bar
**Last Verified:** 2026-02-09 | **Definition Confidence:** MEDIUM

**Primary Identifier:** text="Settings" as screen title

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Screen title | text | "Settings" |
| Profile section | text | Contains "Profile" or user name/email |
| Meal generation section | text | "Meal Generation" or "Preferences" |
| Items per meal setting | text | Contains "items" or "per meal" |
| Strict allergen toggle | text | Contains "allergen" or "strict" |
| Dietary toggle | text | Contains "dietary" |
| Allow repeats toggle | text | Contains "repeat" |
| Sign out button | text | "Sign Out" or "Logout" | ⬇️ Below fold — requires scroll |
| Pantry link | text | "Pantry" or "My Pantry" | ⬇️ Below fold — requires scroll |
| Recipe Rules link | text | "Recipe Rules" | ⬇️ Below fold — requires scroll |

### Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap allergen toggle | text contains "allergen" toggle area | Toggle switches state | Already on Settings |
| Tap dietary toggle | text contains "dietary" toggle area | Toggle switches state | Already on Settings |
| Tap repeats toggle | text contains "repeat" toggle area | Toggle switches state | Already on Settings |
| Tap "Pantry" | text="Pantry" | Navigate to Pantry screen | Press BACK |
| Tap "Recipe Rules" | text="Recipe Rules" | Navigate to Recipe Rules screen | Press BACK |
| Tap "Sign Out" | text="Sign Out" | Confirmation dialog appears | Tap "Cancel" on dialog |

### Data Validation

- User email should match the test account email
- Toggle states should reflect actual preferences
- Section headers should be properly organized

### Known Issues

- Sign out confirmation dialog text may vary
- Some settings sync to backend on change — may need network

---

## Screen 8: notifications

**Navigation:** Home → tap Notifications icon (content-desc="Notifications") in top bar
**Last Verified:** 2026-02-09 | **Definition Confidence:** MEDIUM

**Primary Identifier:** text="Notifications" as screen title

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Screen title | text | "Notifications" |
| Filter: All | text | "All" |
| Filter: Unread | text | "Unread" |
| Mark all read | text | "Mark all read" or content-desc="Mark all read" |
| Notification items OR empty state | text | Notification text OR "No notifications" |
| Back button | content-desc | "Back" or "Navigate up" |

### Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap "All" filter | text="All" | Shows all notifications | Already on Notifications |
| Tap "Unread" filter | text="Unread" | Shows only unread notifications | Already on Notifications |
| Tap "Mark all read" | text="Mark all read" | All notifications marked as read | Already on Notifications |
| Tap Back | content-desc="Back" | Return to Home | Already returned |

### Data Validation

- Fresh accounts may have no notifications (empty state expected)
- If notifications exist, they should have timestamps and descriptions
- Unread filter should show fewer or equal items compared to All

### Known Issues

- Empty state is expected for fresh test accounts
- Notifications stored in Room — no backend push mechanism in test
- "Mark all read" may not be visible if no notifications exist

---

## Screen 9: recipe-detail

**Navigation:** Home → tap a meal card (e.g., tap BREAKFAST area) → tap "View Recipe" on action sheet
**Last Verified:** 2026-02-09 | **Definition Confidence:** MEDIUM

**Primary Identifier:** Recipe name as title OR text contains "Ingredients" OR text contains "Instructions"

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Recipe name/title | text | An Indian dish name (e.g., "Poha", "Dal Tadka") |
| Cuisine type | text | "North Indian", "South Indian", etc. |
| Prep time | text | Contains "min" or "minutes" |
| Favorite button | content-desc | Contains "Favorite" or "Heart" |
| Servings selector | text | Contains "Servings" or a number |
| Ingredients section | text | "Ingredients" |
| Ingredient items | text | Ingredient names with quantities |
| Instructions section | text | "Instructions" or "Steps" or "Method" | ⬇️ Below fold — requires scroll |
| Start Cooking button | text | "Start Cooking" | ⬇️ Below fold — requires scroll |
| Back button | content-desc | "Back" or "Navigate up" |

### Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap Favorite | content-desc contains "Favorite" | Heart icon toggles, snackbar "added to favorites" | Already on Recipe Detail |
| Tap Servings + | text="+" near servings | Servings count increases, ingredients adjust | Already on Recipe Detail |
| Tap Servings - | text="-" near servings | Servings count decreases, ingredients adjust | Already on Recipe Detail |
| Tap "Start Cooking" | text="Start Cooking" | Navigate to Cooking Mode | Press BACK |
| Tap Back | content-desc="Back" | Return to Home | Already returned |

### Data Validation

- Recipe name should be an actual Indian dish name
- Ingredients should include quantities and Indian measurements
- Instructions should have numbered steps
- Prep time should be a reasonable number (5-120 minutes)
- Servings should default to family size

### Known Issues

- Recipe data comes from backend — requires network
- Favorite state persists in Room
- Scrolling may be needed to see all ingredients/instructions

---

## Screen 10: cooking-mode

**Navigation:** Recipe Detail → tap "Start Cooking" (text="Start Cooking")
**Last Verified:** 2026-02-09 | **Definition Confidence:** MEDIUM

**Primary Identifier:** Step counter text (e.g., "Step 1 of 5") OR text contains cooking instruction

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Step counter | text | "Step X of Y" pattern |
| Current instruction | text | Cooking instruction text |
| Next/Previous buttons | text or content-desc | "Next", "Previous", "Back", arrows |
| Complete/Finish button | text | "Complete" or "Done" or "Finish Cooking" (on last step) |
| Exit button | content-desc | "Close" or "Exit" or "Back" |

### Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap Next | text="Next" or forward arrow | Advances to next step, counter updates | Already in Cooking Mode |
| Tap Previous | text="Previous" or back arrow | Goes to previous step | Already in Cooking Mode |
| Tap Complete (last step) | text="Complete" or "Finish" | Exits cooking mode, may show completion dialog | Press BACK if needed |
| Tap Exit/Close | content-desc="Close" or "Exit" | Returns to Recipe Detail | Already returned |

### Data Validation

- Step instructions should be meaningful cooking steps
- Step counter should accurately reflect total steps
- Navigation should wrap correctly (no crash on first/last step)

### Known Issues

- Cooking mode may not have content-desc on all buttons
- Step text may be long — check for truncation
- Timer functionality may not be visible in uiautomator

---

## Screen 11: pantry

**Navigation:** Home → Profile icon → Settings → tap "Pantry" (text="Pantry")
**Last Verified:** 2026-02-09 | **Definition Confidence:** MEDIUM

**Primary Identifier:** text="Pantry" OR text="My Pantry" OR text="No items" (empty state)

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Screen title | text | "Pantry" or "My Pantry" |
| Add item button | text or content-desc | "Add" or "+" or "Add Item" |
| Pantry items OR empty state | text | Ingredient names OR "No items in pantry" |
| Back button | content-desc | "Back" or "Navigate up" |

### Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap Add item | text="Add" or content-desc="Add" | Add item dialog/sheet appears | Press BACK or Cancel |
| Tap Back | content-desc="Back" | Return to Settings | Already returned |

### Data Validation

- Fresh accounts should show empty state
- If items exist, they should be common Indian cooking ingredients
- Known ingredients seed includes 40+ items on fresh install

### Known Issues

- Pantry is Room-only — no backend sync
- Empty state is expected for fresh test accounts
- Add item may require text input via keyboard

---

## Screen 12: recipe-rules

**Navigation:** Home → Profile icon → Settings → tap "Recipe Rules" (text="Recipe Rules")
**Last Verified:** 2026-02-09 | **Definition Confidence:** MEDIUM

**Primary Identifier:** text="Recipe Rules" as screen title OR tab text "Rules" / "Nutrition"

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Screen title | text | "Recipe Rules" |
| Rules tab | text | "Rules" |
| Nutrition tab | text | "Nutrition" or "Nutrition Goals" |
| Add rule button | text or content-desc | "Add" or "+" or "Add Rule" |
| Rule cards OR empty state | text | Rule descriptions OR "No rules" |
| Back button | content-desc | "Back" or "Navigate up" |

### Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap "Rules" tab | text="Rules" | Shows recipe rules list | Already on Recipe Rules |
| Tap "Nutrition" tab | text="Nutrition" | Shows nutrition goals list | Already on Recipe Rules |
| Tap Add rule | text="Add" or content-desc="Add" | Add rule bottom sheet appears | Press BACK or Cancel |
| Tap rule card menu | content-desc="More" or "Options" near rule | Edit/Delete options | Press BACK |
| Tap Back | content-desc="Back" | Return to Settings | Already returned |

### Data Validation

- Rules should show INCLUDE/EXCLUDE type, food name, frequency, meal slot
- If Sharma family data was set up, expect rules like "Include Chai", "Include Paratha"
- Nutrition goals should show category, target servings, timeframe

### Known Issues

- Rule sync requires backend — may show stale data without network
- Add rule sheet has multiple fields (action type, food search, frequency, meal slot)
- Duplicate rule prevention returns 409 — snackbar should show error
- Case normalization: rule names stored as UPPERCASE in backend

---

## Element Search Strategy Reference

Since Compose `testTag()` values (from `TestTags.kt`) are NOT visible in uiautomator XML, here is how the 222 TestTag constants map to searchable attributes:

### Bottom Navigation
| TestTag | Search Via | Value |
|---------|-----------|-------|
| `BOTTOM_NAV_HOME` | text | "Home" |
| `BOTTOM_NAV_GROCERY` | text | "Grocery" |
| `BOTTOM_NAV_CHAT` | text | "Chat" |
| `BOTTOM_NAV_FAVORITES` | text | "Favs" or "Favorites" |
| `BOTTOM_NAV_STATS` | text | "Stats" |

### Top Bar (Home)
| TestTag | Search Via | Value |
|---------|-----------|-------|
| `HOME_MENU_BUTTON` | content-desc | "Menu" or "Navigation" |
| `HOME_NOTIFICATIONS_BUTTON` | content-desc | "Notifications" |
| `HOME_PROFILE_BUTTON` | content-desc | "Profile" or "Settings" |

### Meal Sections
| TestTag | Search Via | Value |
|---------|-----------|-------|
| `MEAL_CARD_PREFIX + breakfast` | text | "BREAKFAST" |
| `MEAL_CARD_PREFIX + lunch` | text | "LUNCH" |
| `MEAL_CARD_PREFIX + dinner` | text | "DINNER" |
| `MEAL_CARD_PREFIX + snacks` | text | "SNACKS" |
| `MEAL_LOCK_BUTTON_PREFIX + *` | content-desc | Contains "lock" or "Lock" |
| `HOME_REFRESH_BUTTON` | content-desc | Contains "Refresh" |
| `HOME_DAY_LOCK_BUTTON` | content-desc | Contains "lock" near day header |

### Screen Identifiers
| Screen | Search Via | Value |
|--------|-----------|-------|
| Auth | text | "Sign in with Google" |
| Onboarding | text | "Tell us about your household" or "Next" |
| Home | text | "This Week's Menu" or "BREAKFAST" |
| Grocery | text | "Grocery List" or "Grocery" (title) |
| Chat | text | "Chat" (title) + class=EditText |
| Favorites | text | "Favorites" (title) |
| Stats | text | "Stats" or "Cooking Stats" |
| Settings | text | "Settings" (title) |
| Notifications | text | "Notifications" (title) |
| Recipe Detail | text | "Ingredients" + "Instructions" |
| Cooking Mode | text | "Step X of Y" pattern |
| Pantry | text | "Pantry" (title) |
| Recipe Rules | text | "Recipe Rules" (title) |

### Action Sheets & Dialogs
| Context | Search Via | Value |
|---------|-----------|-------|
| Recipe action sheet | text | "View Recipe", "Swap Recipe", "Lock Recipe", "Remove from Meal" |
| Refresh options | text | "This Day Only", "Entire Week", "Regenerate Meals" |
| Add recipe sheet | text | "Add Recipe to", "Suggestions", "Favorites" (tabs) |
| Swap recipe sheet | text | "Swap", "Select a similar recipe", "Similar Recipes" |
| Sign out dialog | text | "Sign Out", "Cancel", "Are you sure" |

### Interactive Controls
| Control | Search Via | Value |
|---------|-----------|-------|
| Toggle/Switch | class | `android.widget.Switch` or `android.widget.ToggleButton` |
| Text input | class | `android.widget.EditText` |
| Button | class | `android.widget.Button` or text matches action |
| Dropdown | class | `android.widget.Spinner` or tap triggers popup |
| Checkbox | class | `android.widget.CheckBox` |

---

## Coordinate Hints

These are approximate screen regions for common elements on a 1080x2400 Pixel 6 display:

| Region | Y Range | Contains |
|--------|---------|----------|
| Status bar | 0-80 | System icons, time, battery |
| Top app bar | 80-200 | Title, menu, notifications, profile icons |
| Week selector | 200-360 | Day tabs (M T W T F S S) |
| Day header | 360-450 | Selected day name, lock button, refresh button |
| Content area | 450-2200 | Meal cards, lists, main content |
| Bottom nav | 2200-2400 | Home, Grocery, Chat, Favs, Stats |

Bottom nav items are evenly spaced across the width:
| Nav Item | Approx X Center |
|----------|----------------|
| Home | 108 |
| Grocery | 324 |
| Chat | 540 |
| Favs | 756 |
| Stats | 972 |

**Note:** These are approximations. Always use uiautomator XML bounds for precise coordinates.

---

## Definition Update Log

Track changes to test definitions made during `/adb-test` fix loops (F2 code-vs-definition decisions).

| Date | Screen | Element | Old Value | New Value | Reason |
|------|--------|---------|-----------|-----------|--------|
| 2026-02-09 | — | — | — | — | Initial creation of update log |
