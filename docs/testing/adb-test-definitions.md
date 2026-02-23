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

**Primary Identifier:** text="Continue with Google" OR text="Welcome"

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| App logo/title | text | "RasoiAI" or app name |
| Welcome text | text | Contains "Welcome" or "Sign in" |
| Google sign-in button | text | "Continue with Google" |
| App tagline | text | Contains "meal" or "cooking" or "family" |

### Interactive Elements

| Action | Target | Expected Result |
|--------|--------|-----------------|
| Tap "Continue with Google" | text="Continue with Google" | Navigates to Google auth flow (fake auth returns immediately) → Onboarding or Home |

### Data Validation

- No data expected on auth screen (pre-login)
- After sign-in, should transition to Onboarding (first-time) or Home (returning user)

### Known Issues

- Fake auth (`fake-firebase-token`) bypasses Google OAuth in Compose instrumented tests only
- ADB tests use real Google OAuth with test Gmail accounts (see `memory/test-accounts.md` for credentials)
- Primary ADB test account: `abhayfaircent@gmail.com`
- Secondary ADB test account: `zmphzc@gmail.com`
- Emulator must have one of these Google accounts signed in before ADB auth flow

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

### Additional Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Offline banner | text | "You're offline" or "Offline" |
| Festival banner | text | Contains "days!" or "festive recipes" or festival name |
| Meal item prep time | text | Pattern: number + "min" (e.g., "25 min") |
| Meal item calories | text | Pattern: number + "cal" (e.g., "320 cal") |
| Meal total row | text | Pattern: "Total:" + number + "min" |
| Swap icon per item | content-desc | "Swap" |
| Dietary indicator dot | class | Colored `View` near recipe name |

### Additional Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap meal item card body | Recipe name text area | Recipe Action Sheet: "View Recipe", "Swap Recipe", "Lock Recipe", "Remove from Meal" | Tap outside or BACK |
| Tap "View Recipe" on action sheet | text="View Recipe" | Navigate to Recipe Detail | Press BACK |
| Tap "Swap Recipe" on action sheet | text="Swap Recipe" | Swap Sheet with search + recipe grid | Press BACK |
| Tap "Lock Recipe" on action sheet | text="Lock Recipe" | Lock icon toggles for that meal item | Already on Home |
| Tap "Remove from Meal" on action sheet | text="Remove from Meal" | Recipe removed from meal slot | Already on Home |
| Tap "This Day Only" in refresh sheet | text="This Day Only" | Regenerates single day's meals | Already on Home |
| Tap "Entire Week" in refresh sheet | text="Entire Week" | Regenerates full week of meals | Already on Home |
| Tap Add per meal section | text="Add" near meal section | Add Recipe Sheet (Suggestions/Favorites tabs) | Press BACK |
| Search in Swap/Add sheet | EditText in sheet | Filters recipes by search term | Already in sheet |
| Tap festival banner | text containing "festive" or festival name | Festival Recipes Sheet | Press BACK |
| Swipe meal item left | Swipe on recipe row | Delete action (red background) | Already on Home |
| Swipe meal item right | Swipe on recipe row | Lock action | Already on Home |

### Backend Cross-Validation

```bash
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/meal-plans/current
# Verify: 7 days, 4 meal slots each, recipe_name + prep_time_minutes + dietary_tags per item
```

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
| Category sections | text | Category names like "OTHER", "Vegetables", "Spices", "Dairy", "Grains" (current impl groups as "OTHER" with recipe-level bundles) |
| Grocery items | text | "Ingredients for {RecipeName}" with "1 set" quantity (recipe-level bundles, not individual ingredients) |
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
- Checkbox tap via ADB does not change state (Compose checkbox interaction limitation, similar to dropdown popups)
- All items currently grouped under single "OTHER" category as recipe-level bundles ("Ingredients for {Recipe}") rather than individual ingredients by category

### Additional Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| More options (3-dot) | content-desc | "More options" or "More" |
| Add custom item button | text or content-desc | "Add custom item" or "Add Item" or "+" |
| Purchased items count | text | Pattern: number + "purchased" |

### Additional Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap More options (3-dot) | content-desc="More options" | Menu: "Clear purchased items", "Share as text" | Tap outside or BACK |
| Tap "Clear purchased items" | text="Clear purchased items" | All checkmarks cleared | Already on Grocery |
| Tap "Share as text" | text="Share as text" | Share intent (text format) | Press BACK |
| Tap "Add custom item" | text="Add custom item" or "+" | Dialog: name, quantity, unit, category fields | Tap "Cancel" or BACK |
| Swipe item left | Swipe on grocery item row | Delete (red background) | Already on Grocery |
| Swipe item right | Swipe on grocery item row | Edit dialog (blue background) | Cancel or BACK |
| Tap item row checkbox | Checkbox next to ingredient | Checkbox toggles purchased state | Already on Grocery |

### Backend Cross-Validation

```bash
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/grocery-list
# Verify: categories with items, item names match meal plan ingredients, quantities > 0
```

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

### Additional Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Attachment button | content-desc | "Attach photo" or "Attachment" |
| Voice button | content-desc | "Voice input" or "Voice" |
| Quick action chips | text | Suggestion chips (e.g., "What's for dinner?", "Add recipe") |
| More options | content-desc | "More options" or "More" |
| Recipe suggestion card | text | Contains recipe name + "Add to Meal Plan" |

### Additional Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap attachment button | content-desc="Attach photo" | Image Source Dialog: Camera / Gallery options | Press BACK |
| Tap voice button | content-desc="Voice input" | Speech recognizer intent launches | Press BACK |
| Tap More options | content-desc="More options" | Menu: "Clear Chat History" + possible others | Tap outside or BACK |
| Tap "Clear Chat History" | text="Clear Chat History" | Confirmation dialog | Tap "Cancel" |
| Tap quick action chip | text=chip label | Auto-fills and sends message, or navigates | Already on Chat |
| Tap recipe suggestion card | Recipe card in AI response | Navigate to Recipe Detail | Press BACK |
| Tap "Add to Meal Plan" on suggestion | text="Add to Meal Plan" | Recipe added to current meal plan | Already on Chat |

### Backend Cross-Validation

```bash
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/chat/history
# Verify: messages array with user + assistant roles, timestamps, tool_calls if any
```

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

### Additional Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Filter chips | text | "Cuisine", "Time", or other filter labels |
| Collection cards | text | Collection names (if any exist) |
| Create Collection button | text or content-desc | "Create Collection" or "New Collection" |

### Additional Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap filter chip (Cuisine) | text="Cuisine" or filter chip | Filters favorites by cuisine type | Already on Favorites |
| Tap filter chip (Time) | text="Time" or filter chip | Filters favorites by prep time | Already on Favorites |
| Tap collection card | text=collection name | Shows collection's recipes | Press BACK |
| Tap "Create Collection" | text="Create Collection" | Dialog with name input | Cancel or BACK |
| Long-press recipe card | Long press on recipe card area | Context menu (Remove, Add to Collection) | Tap outside or BACK |

---

## Screen 6: stats

**Navigation:** Home → tap bottom nav "Stats" (text="Stats")
**Last Verified:** 2026-02-09 | **Definition Confidence:** MEDIUM

**Primary Identifier:** text contains "Stats" OR text contains "Cooking" as screen title

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Screen title | text | "Stats" or "Cooking Stats" |
| Time period tabs | text | No tabs in current impl — uses calendar + "THIS MONTH" section instead |
| Cooking streak | text | "CURRENT STREAK" + number + "days" |
| Calendar | text | Month name + year + day numbers (full calendar grid) |
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

### Additional Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Share button | content-desc | "Share" |
| Calendar month label | text | Month name + year (e.g., "February 2026") |
| Calendar navigation arrows | content-desc | "Previous month" / "Next month" or arrows |
| Calendar date cells | text | Day numbers (1-31) |
| View All achievements link | text | "View All" near achievements |
| Challenge section | text | Contains "Challenge" or "Weekly Challenge" |
| Leaderboard section | text | Contains "Leaderboard" |

### Additional Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap Share button | content-desc="Share" | Share intent with stats summary | Press BACK |
| Tap calendar prev month | content-desc="Previous month" or left arrow | Calendar navigates to previous month | Already on Stats |
| Tap calendar next month | content-desc="Next month" or right arrow | Calendar navigates to next month | Already on Stats |
| Tap calendar date cell | text=day number in calendar | Shows that day's cooking data/highlight | Already on Stats |
| Tap "View All" achievements | text="View All" near achievements | Achievements screen loads | Press BACK |
| Tap achievement card | Achievement card area | Achievement detail/progress | Already on Stats |

### Backend Cross-Validation

```bash
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/stats
# Verify: cooking_streak, cuisine_breakdown, total_cooked
```

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

- User email should match the test account (`abhayfaircent@gmail.com` or `zmphzc@gmail.com`)
- Toggle states should reflect actual preferences
- Section headers should be properly organized

### Known Issues

- Sign out confirmation dialog text may vary
- Some settings sync to backend on change — may need network

### Additional Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Dietary Restrictions link | text | "Dietary Restrictions" |
| Disliked Ingredients link | text | "Disliked Ingredients" |
| Cuisine Preferences link | text | "Cuisine Preferences" |
| Spice Level link | text | "Spice Level" |
| Cooking Time link | text | "Cooking Time" |
| Family Members link | text | "Family Members" | ⬇️ Below fold |
| Notification Settings link | text | "Notification Settings" or "Notifications" | ⬇️ Below fold |
| Units link | text | "Units" | ⬇️ Below fold |
| Edit Profile link | text | "Edit Profile" | ⬇️ Below fold |
| Friends & Leaderboard link | text | "Friends & Leaderboard" or "Friends" | ⬇️ Below fold |
| Connected Accounts link | text | "Connected Accounts" | ⬇️ Below fold |
| Dark Mode setting | text | "Dark Mode" or "Theme" |
| Share App link | text | "Share App" | ⬇️ Below fold |
| Help & FAQ link | text | "Help & FAQ" or "Help" | ⬇️ Below fold |
| Contact Us link | text | "Contact Us" | ⬇️ Below fold |
| Rate App link | text | "Rate App" | ⬇️ Below fold |
| Privacy Policy link | text | "Privacy Policy" | ⬇️ Below fold |
| Terms of Service link | text | "Terms of Service" | ⬇️ Below fold |

### Additional Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap "Dietary Restrictions" | text="Dietary Restrictions" | Dietary Restrictions sub-screen | Press BACK |
| Tap "Disliked Ingredients" | text="Disliked Ingredients" | Disliked Ingredients sub-screen | Press BACK |
| Tap "Cuisine Preferences" | text="Cuisine Preferences" | Cuisine Preferences sub-screen | Press BACK |
| Tap "Spice Level" | text="Spice Level" | Spice Level sub-screen | Press BACK |
| Tap "Cooking Time" | text="Cooking Time" | Cooking Time sub-screen | Press BACK |
| Tap "Family Members" | text="Family Members" | Family Members sub-screen | Press BACK |
| Tap "Notification Settings" | text="Notification Settings" | Notification Settings sub-screen | Press BACK |
| Tap "Units" | text="Units" | Units sub-screen | Press BACK |
| Tap "Edit Profile" | text="Edit Profile" | Edit Profile sub-screen | Press BACK |
| Tap "Friends & Leaderboard" | text="Friends & Leaderboard" | Friends & Leaderboard sub-screen | Press BACK |
| Tap "Connected Accounts" | text="Connected Accounts" | Connected Accounts sub-screen | Press BACK |
| Tap "Dark Mode" | text="Dark Mode" | Dialog: Light / Dark / System options | Tap option or Cancel |
| Tap "Items per Meal" | text containing "items" or "per meal" | Number selector dialog | Select number or Cancel |
| Tap "Share App" | text="Share App" | Share intent (app store link) | Press BACK |
| Tap "Help & FAQ" | text="Help & FAQ" | External browser opens | Press BACK |
| Tap "Contact Us" | text="Contact Us" | Email client or browser opens | Press BACK |
| Tap "Rate App" | text="Rate App" | Play Store or rating dialog | Press BACK |
| Tap "Privacy Policy" | text="Privacy Policy" | Browser opens privacy policy | Press BACK |
| Tap "Terms of Service" | text="Terms of Service" | Browser opens terms | Press BACK |

### Backend Cross-Validation

```bash
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/users/me
# Verify: email, dietary_type, spice_level, cuisines, items_per_meal, toggle states
```

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

### Additional Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Clear All button | text or content-desc | "Clear All" or "Clear" |
| Date group headers | text | Date strings (e.g., "Today", "Yesterday", "Feb 10") |
| Notification timestamp | text | Time string (e.g., "2:30 PM", "3 hours ago") |

### Additional Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap "Clear All" | text="Clear All" | Confirmation dialog: "Clear all notifications?" | Tap "Cancel" |
| Confirm Clear All | text="Clear" or "OK" in dialog | All notifications removed, empty state | Already on Notifications |
| Swipe notification left | Swipe on notification row | Delete notification (red background) | Already on Notifications |
| Tap notification | Tap notification row | Navigate to relevant screen (meal plan, recipe, etc.) | Press BACK |

### Backend Cross-Validation

```bash
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/notifications
# Verify: notifications array with type, message, read status, timestamps
```

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

### Additional Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Share button | content-desc | "Share" |
| More options | content-desc | "More options" or "More" |
| Ingredients tab | text | "Ingredients" (tab header) |
| Instructions tab | text | "Instructions" (tab header) |
| Ingredient checkbox | class | `android.widget.CheckBox` near ingredient |
| Add all to Grocery button | text | "Add all to Grocery" or "Add to Grocery" | ⬇️ Below fold |
| Modify with AI button | text | "Modify with AI" or "Ask AI" | ⬇️ Below fold |
| Nutrition info | text | Contains "Calories" or "Protein" or "Nutrition" |
| Rating stars | content-desc | Contains "star" or "rating" |

### Additional Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap Share button | content-desc="Share" | Share intent with recipe details | Press BACK |
| Tap More options | content-desc="More options" | Menu: "Report Issue" or similar | Tap outside or BACK |
| Tap Ingredients tab | text="Ingredients" | Shows ingredients list | Already on Recipe Detail |
| Tap Instructions tab | text="Instructions" | Shows instructions/steps | Already on Recipe Detail |
| Tap ingredient checkbox | Checkbox near ingredient name | Ingredient struck through | Already on Recipe Detail |
| Tap "Add all to Grocery" | text="Add all to Grocery" | Ingredients added to grocery list, snackbar confirmation | Already on Recipe Detail |
| Tap "Modify with AI" | text="Modify with AI" | Navigate to Chat with recipe context | Press BACK |

### Backend Cross-Validation

```bash
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/recipes/{recipeId}
# Verify: name, cuisine_type, prep_time_minutes, ingredients[], instructions[], dietary_tags
```

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

### Additional Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Voice guidance toggle | text or content-desc | "Voice" or "Voice guidance" or "Speaker" |
| Timer display | text | Pattern: "MM:SS" or "0:00" |
| Timer Start button | text | "Start" or "Start Timer" |
| Timer Pause button | text | "Pause" |
| Timer Resume button | text | "Resume" |
| Timer Stop button | text | "Stop" or "Reset" |
| Progress bar | content-desc | "Progress" or progress indicator |
| Ingredient checklist per step | class | Checkboxes near step ingredients |
| Rating dialog (after last step) | text | "Rate this recipe" or "How was it?" |

### Additional Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap voice guidance toggle | content-desc="Voice" or toggle | Voice TTS enables/disables | Already in Cooking Mode |
| Tap Timer Start | text="Start" or "Start Timer" | Timer countdown begins, display updates | Already in Cooking Mode |
| Tap Timer Pause | text="Pause" | Timer pauses | Already in Cooking Mode |
| Tap Timer Resume | text="Resume" | Timer resumes | Already in Cooking Mode |
| Tap Timer Stop | text="Stop" or "Reset" | Timer resets to 0:00 | Already in Cooking Mode |
| Tap Exit/Close | content-desc="Close" or "Exit" | Confirmation dialog: "Exit cooking?" | Tap "Cancel" to stay |
| Confirm Exit | text="Exit" or "Yes" in dialog | Returns to Recipe Detail | Already returned |
| Tap rating stars (after last step) | Star icons in rating dialog | Stars fill to selected level | Already in dialog |
| Tap Submit rating | text="Submit" or "Rate" | Rating saved, returns to Recipe Detail | Already returned |
| Tap Skip rating | text="Skip" or "Not now" | Rating skipped, returns to Recipe Detail | Already returned |

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

### Additional Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Camera button | content-desc | "Camera" or "Scan" or "Take photo" |
| Gallery button | content-desc | "Gallery" or "Pick image" |
| Find Recipes button | text | "Find Recipes" or "Recipes from Pantry" |
| View All button | text | "View All" |
| Remove Expired button | text | "Remove Expired" or "Clear Expired" |
| Item name | text | Ingredient name in list |
| Item quantity | text | Quantity string near item |

### Additional Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap Camera button | content-desc="Camera" or "Scan" | Camera intent opens | Press BACK |
| Tap Gallery button | content-desc="Gallery" | Gallery picker opens | Press BACK |
| Tap scan result "Add All" | text="Add All" in results sheet | All scanned items added to pantry | Already on Pantry |
| Tap "Find Recipes" | text="Find Recipes" | Navigate to Chat with pantry ingredients | Press BACK |
| Tap "View All" | text="View All" | Full pantry list view | Press BACK |
| Tap "Remove Expired" | text="Remove Expired" | Confirmation dialog | Tap "Cancel" or confirm |
| Tap item for details | Tap pantry item row | Item details (name, quantity, expiry) | Already on Pantry |

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
- **All 8 nutrition categories:** Green Leafy, Citrus/Vitamin C, Iron Rich, High Protein, Calcium Rich, Fiber Rich, Omega-3, Antioxidant
- **Edit rule verification:** After editing a rule, changed fields (enforcement, frequency, meal slot) should persist across screen exits and returns
- **Diet conflict warning text patterns:** "conflict", "warning", "diet", "non-vegetarian" — appears when vegetarian user adds meat-based INCLUDE rules
- **Search empty state text:** "No results", "No matching", "not found" — appears when search query has no matches

### Known Issues

- Rule sync requires backend — may show stale data without network
- Add rule sheet has multiple fields (action type, food search, frequency, meal slot)
- Duplicate rule prevention returns 409 — snackbar should show error
- Case normalization: rule names stored as UPPERCASE in backend
- Diet conflict warning only implemented on client side (not enforced by backend)
- When all 8 nutrition categories are used, add button behavior varies (disabled, message, or empty dropdown)

### Additional Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Rule toggle switch | class | `android.widget.Switch` on rule card |
| Rule card actions | content-desc | "Edit" or "Delete" or "More" on rule card |
| INCLUDE/EXCLUDE selector | text | "INCLUDE" / "EXCLUDE" in add rule sheet |
| Food search field | class | `android.widget.EditText` in add rule sheet |
| Frequency selector | text | "Every day" / "Weekdays" / "Weekends" / "Once a week" |
| Meal slot selector | text | "BREAKFAST" / "LUNCH" / "DINNER" / "SNACKS" / "ANY" |
| Nutrition goal card | text | Goal description with category and target |

### Additional Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap rule toggle switch | Switch on rule card | Rule toggles active/inactive | Already on Recipe Rules |
| Tap rule card Edit | content-desc="Edit" or menu item | Edit rule sheet opens with current values | Cancel or BACK |
| Tap rule card Delete | content-desc="Delete" or menu item | Confirmation dialog: "Delete this rule?" | Tap "Cancel" |
| Confirm Delete | text="Delete" or "OK" in dialog | Rule removed from list | Already on Recipe Rules |
| Tap INCLUDE/EXCLUDE selector | text="INCLUDE" or "EXCLUDE" | Toggles between INCLUDE and EXCLUDE action | Already in sheet |
| Search food in add rule | EditText in add rule sheet | Food search results appear | Already in sheet |
| Select frequency | text=frequency option | Frequency set | Already in sheet |
| Select meal slot | text=meal slot option | Meal slot set | Already in sheet |
| Tap nutrition goal toggle | Switch on nutrition goal card | Goal toggles enforcement | Already on Nutrition tab |
| Tap Add Nutrition Goal | text="Add" or "+" on Nutrition tab | Add Nutrition Goal sheet | Cancel or BACK |

### Backend Cross-Validation

```bash
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/recipe-rules
# Verify: rules array with type (INCLUDE/EXCLUDE), target_name, frequency, meal_slot, is_active
```

---

## Screen 13: dietary-restrictions

**Navigation:** Home → Profile icon → Settings → tap "Dietary Restrictions"
**Last Verified:** — | **Definition Confidence:** MEDIUM

**Primary Identifier:** text="Dietary Restrictions" as screen title OR radio buttons with diet type names

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Screen title | text | "Dietary Restrictions" |
| Back button | content-desc | "Back" or "Navigate up" |
| Vegetarian radio | text | "Vegetarian" |
| Non-Vegetarian radio | text | "Non-Vegetarian" |
| Vegan radio | text | "Vegan" |
| Eggetarian radio | text | "Eggetarian" |
| Jain checkbox | text | "Jain" |
| Sattvic checkbox | text | "Sattvic" |
| Halal checkbox | text | "Halal" |
| Save button | text | "Save" or "Apply" |

### Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap diet radio button | text=diet type name | Radio button selected, others deselected | Already on screen |
| Tap Jain checkbox | text="Jain" checkbox | Checkbox toggles | Already on screen |
| Tap Sattvic checkbox | text="Sattvic" checkbox | Checkbox toggles | Already on screen |
| Tap Halal checkbox | text="Halal" checkbox | Checkbox toggles | Already on screen |
| Tap Save | text="Save" | Preferences saved, navigates back or snackbar | Settings screen |
| Tap Back | content-desc="Back" | Return to Settings | Already returned |

### Data Validation

- Selected radio should match user's current dietary_type
- Checkboxes should reflect current modifier flags

### Backend Cross-Validation

```bash
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/users/me | python -c "import sys,json;d=json.load(sys.stdin);print(f'dietary_type={d.get(\"dietary_type\")}')"
# Verify: dietary_type matches selected radio
```

### Known Issues

- Radio buttons may use Compose RadioButton — look for `class` containing "Button" near diet text
- Save may auto-navigate back (no explicit Save button needed in some implementations)

---

## Screen 14: disliked-ingredients

**Navigation:** Home → Profile icon → Settings → tap "Disliked Ingredients"
**Last Verified:** — | **Definition Confidence:** MEDIUM

**Primary Identifier:** text="Disliked Ingredients" as screen title

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Screen title | text | "Disliked Ingredients" |
| Back button | content-desc | "Back" or "Navigate up" |
| Search/add field | class | `android.widget.EditText` or text="Add ingredient" |
| Ingredient chips | text | Ingredient names (e.g., "Karela", "Lauki") |
| Save button | text | "Save" or "Apply" |

### Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap search field, type ingredient | EditText | Suggestions appear | Already on screen |
| Tap suggestion or press Enter | Suggestion text or keyboard enter | Chip added to disliked list | Already on screen |
| Tap chip X (remove) | Close icon on chip | Ingredient removed from disliked list | Already on screen |
| Tap Save | text="Save" | Preferences saved | Settings screen |
| Tap Back | content-desc="Back" | Return to Settings | Already returned |

### Data Validation

- Chips should match user's current disliked_ingredients list
- Adding a duplicate should show warning or be ignored

### Backend Cross-Validation

```bash
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/users/me | python -c "import sys,json;d=json.load(sys.stdin);print(f'disliked={d.get(\"disliked_ingredients\")}')"
```

### Known Issues

- Chip close button may be small — look for "X" or close content-desc near chip

---

## Screen 15: cuisine-preferences

**Navigation:** Home → Profile icon → Settings → tap "Cuisine Preferences"
**Last Verified:** — | **Definition Confidence:** MEDIUM

**Primary Identifier:** text="Cuisine Preferences" as screen title

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Screen title | text | "Cuisine Preferences" |
| Back button | content-desc | "Back" or "Navigate up" |
| North Indian card | text | "North Indian" |
| South Indian card | text | "South Indian" |
| East Indian card | text | "East Indian" |
| West Indian card | text | "West Indian" |
| Save button | text | "Save" or "Apply" |

### Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap cuisine card | text=cuisine name | Card toggles selected (checkmark appears/disappears) | Already on screen |
| Tap Save | text="Save" | Preferences saved | Settings screen |
| Tap Back | content-desc="Back" | Return to Settings | Already returned |

### Data Validation

- Selected cuisines should match user's current cuisines list
- At least one cuisine should be selected (validation if trying to deselect all)

### Backend Cross-Validation

```bash
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/users/me | python -c "import sys,json;d=json.load(sys.stdin);print(f'cuisines={d.get(\"cuisines\")}')"
```

### Known Issues

- Cuisine cards may use custom composable — checkmark may be overlay or icon inside card

---

## Screen 16: spice-level

**Navigation:** Home → Profile icon → Settings → tap "Spice Level"
**Last Verified:** — | **Definition Confidence:** MEDIUM

**Primary Identifier:** text="Spice Level" as screen title

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Screen title | text | "Spice Level" |
| Back button | content-desc | "Back" or "Navigate up" |
| Spice dropdown or radio | text | "Mild", "Medium", "Spicy", "Very Spicy" |
| Save button | text | "Save" or "Apply" |

### Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap spice option | text=spice level name | Option selected | Already on screen |
| Tap Save | text="Save" | Preferences saved | Settings screen |
| Tap Back | content-desc="Back" | Return to Settings | Already returned |

### Data Validation

- Selected spice level should match user's current spice_level

### Backend Cross-Validation

```bash
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/users/me | python -c "import sys,json;d=json.load(sys.stdin);print(f'spice_level={d.get(\"spice_level\")}')"
```

### Known Issues

- If implemented as dropdown, ADB cannot interact with Compose popup items (Pattern 14)
- If implemented as radio buttons or cards, ADB tap works normally

---

## Screen 17: cooking-time

**Navigation:** Home → Profile icon → Settings → tap "Cooking Time"
**Last Verified:** — | **Definition Confidence:** MEDIUM

**Primary Identifier:** text="Cooking Time" as screen title

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Screen title | text | "Cooking Time" |
| Back button | content-desc | "Back" or "Navigate up" |
| Weekday cooking time | text | Contains "Weekday" + time value (e.g., "30 minutes") |
| Weekend cooking time | text | Contains "Weekend" + time value (e.g., "60 minutes") |
| Busy day chips | text | Day names: "Monday", "Tuesday", etc. |
| Save button | text | "Save" or "Apply" |

### Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap weekday time dropdown | text near "Weekday" | Dropdown or selector appears (may be Pattern 14 limited) | Already on screen |
| Tap weekend time dropdown | text near "Weekend" | Dropdown or selector appears (may be Pattern 14 limited) | Already on screen |
| Tap busy day chip | text=day name | Day toggles busy/not-busy state | Already on screen |
| Tap Save | text="Save" | Preferences saved | Settings screen |
| Tap Back | content-desc="Back" | Return to Settings | Already returned |

### Data Validation

- Weekday/weekend times should match user's current cooking times
- Busy days should match user's busy_days list

### Backend Cross-Validation

```bash
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/users/me | python -c "import sys,json;d=json.load(sys.stdin);print(f'weekday={d.get(\"weekday_cooking_time\")}, weekend={d.get(\"weekend_cooking_time\")}, busy={d.get(\"busy_days\")}')"
```

### Known Issues

- Cooking time dropdowns may be Pattern 14 limited — use backend API fallback if needed
- Busy day chips should be direct taps (not dropdowns), so ADB should work

---

## Screen 18: family-members

**Navigation:** Home → Profile icon → Settings → tap "Family Members"
**Last Verified:** — | **Definition Confidence:** MEDIUM

**Primary Identifier:** text="Family Members" as screen title

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Screen title | text | "Family Members" |
| Back button | content-desc | "Back" or "Navigate up" |
| Add member FAB | content-desc | "Add" or "Add member" or "+" |
| Member cards | text | Member names (if any exist) |
| Member type | text | "Adult", "Child", "Senior", "Toddler" |
| Member age | text | Age number |

### Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap Add member FAB | content-desc="Add" or "+" | Add Member dialog/sheet: Name, Type, Age, Special Needs | Cancel or BACK |
| Fill member name | EditText in dialog | Name entered | Already in dialog |
| Select member type | text=type name (Adult/Child/Senior) | Type selected | Already in dialog |
| Enter age | EditText for age | Age entered | Already in dialog |
| Save member | text="Save" or "Add" in dialog | Member appears in list | Already on screen |
| Tap member card Edit | content-desc="Edit" on card | Edit dialog with current values | Cancel or BACK |
| Tap member card Delete | content-desc="Delete" on card | Confirmation dialog | Tap "Cancel" |
| Confirm Delete member | text="Delete" in dialog | Member removed from list | Already on screen |

### Data Validation

- Member list should match backend /api/v1/family-members response
- Member types should be valid: ADULT, CHILD, SENIOR, TODDLER

### Backend Cross-Validation

```bash
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/family-members
# Verify: array of members with name, member_type, age, special_needs
```

### Known Issues

- Empty state expected for fresh test accounts
- Member type selector may be dropdown (Pattern 14 limited) or radio buttons

---

## Screen 19: notification-settings

**Navigation:** Home → Profile icon → Settings → tap "Notification Settings"
**Last Verified:** — | **Definition Confidence:** MEDIUM

**Primary Identifier:** text="Notification Settings" or "Notifications" as screen title (distinct from Notifications screen by context)

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Screen title | text | "Notification Settings" |
| Back button | content-desc | "Back" or "Navigate up" |
| Meal plan reminders toggle | text + class | "Meal plan reminders" near Switch |
| Grocery reminders toggle | text + class | "Grocery reminders" near Switch |
| Cooking reminders toggle | text + class | "Cooking reminders" near Switch |
| Achievement notifications toggle | text + class | "Achievement" near Switch |
| Save button | text | "Save" or "Apply" (if not auto-save) |

### Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap meal plan toggle | Switch near "Meal plan" | Toggle on/off | Already on screen |
| Tap grocery toggle | Switch near "Grocery" | Toggle on/off | Already on screen |
| Tap cooking toggle | Switch near "Cooking" | Toggle on/off | Already on screen |
| Tap achievement toggle | Switch near "Achievement" | Toggle on/off | Already on screen |
| Tap Save | text="Save" | Settings saved | Settings screen |
| Tap Back | content-desc="Back" | Return to Settings | Already returned |

### Data Validation

- Toggle states should match local DataStore preferences
- No backend validation needed (notification settings are local-only)

### Known Issues

- Notification settings stored in local DataStore only — no backend sync
- Toggle auto-saves may eliminate need for Save button

---

## Screen 20: units

**Navigation:** Home → Profile icon → Settings → tap "Units"
**Last Verified:** — | **Definition Confidence:** MEDIUM

**Primary Identifier:** text="Units" as screen title

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Screen title | text | "Units" |
| Back button | content-desc | "Back" or "Navigate up" |
| Weight unit section | text | "Weight" section header |
| Volume unit section | text | "Volume" section header |
| Metric option | text | "Metric" |
| US option | text | "US" |
| Indian option | text | "Indian" |
| Save button | text | "Save" or "Apply" |

### Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap Weight: Metric | text="Metric" in Weight section | Radio selected | Already on screen |
| Tap Weight: US | text="US" in Weight section | Radio selected | Already on screen |
| Tap Weight: Indian | text="Indian" in Weight section | Radio selected | Already on screen |
| Tap Volume: Metric | text="Metric" in Volume section | Radio selected | Already on screen |
| Tap Volume: US | text="US" in Volume section | Radio selected | Already on screen |
| Tap Volume: Indian | text="Indian" in Volume section | Radio selected | Already on screen |
| Tap Save | text="Save" | Preferences saved | Settings screen |
| Tap Back | content-desc="Back" | Return to Settings | Already returned |

### Data Validation

- Selected units should match user's current weight_unit and volume_unit

### Backend Cross-Validation

```bash
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/users/me | python -c "import sys,json;d=json.load(sys.stdin);print(f'weight_unit={d.get(\"weight_unit\")}, volume_unit={d.get(\"volume_unit\")}')"
```

### Known Issues

- Units are METRIC, US, or INDIAN (not gram/ml/imperial)

---

## Screen 21: edit-profile

**Navigation:** Home → Profile icon → Settings → tap "Edit Profile"
**Last Verified:** — | **Definition Confidence:** MEDIUM

**Primary Identifier:** text="Edit Profile" as screen title

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Screen title | text | "Edit Profile" |
| Back button | content-desc | "Back" or "Navigate up" |
| Name field | class + text | EditText with current display name |
| Email field | class + text | EditText with current email (may be read-only) |
| Avatar/Profile image | content-desc | "Profile picture" or "Avatar" |
| Save button | text | "Save" or "Update" |

### Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap name field, edit text | EditText with name | Name updated | Already on screen |
| Tap avatar | content-desc="Profile picture" | Image picker or change photo option | BACK |
| Tap Save | text="Save" | Profile updated, snackbar confirmation | Settings screen |
| Tap Back | content-desc="Back" | Return to Settings | Already returned |

### Data Validation

- Name should match user's display name
- Email should match test account (`abhayfaircent@gmail.com` or `zmphzc@gmail.com`)

### Backend Cross-Validation

```bash
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/users/me | python -c "import sys,json;d=json.load(sys.stdin);print(f'name={d.get(\"display_name\")}, email={d.get(\"email\")}')"
```

### Known Issues

- Email may be read-only (sourced from Firebase Auth)

---

## Screen 22: friends-leaderboard

**Navigation:** Home → Profile icon → Settings → tap "Friends & Leaderboard"
**Last Verified:** — | **Definition Confidence:** LOW

**Primary Identifier:** text="Friends & Leaderboard" or "Friends" as screen title

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Screen title | text | "Friends & Leaderboard" or "Friends" |
| Back button | content-desc | "Back" or "Navigate up" |
| Friends list OR empty state | text | Friend names OR "No friends yet" |
| Leaderboard section | text | "Leaderboard" or ranking numbers |

### Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap Back | content-desc="Back" | Return to Settings | Already returned |

### Data Validation

- Empty state expected for test accounts (no social connections)
- Leaderboard may show placeholder or single-user data

### Known Issues

- Feature may be placeholder/minimal — social features are future roadmap

---

## Screen 23: connected-accounts

**Navigation:** Home → Profile icon → Settings → tap "Connected Accounts"
**Last Verified:** — | **Definition Confidence:** LOW

**Primary Identifier:** text="Connected Accounts" as screen title

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Screen title | text | "Connected Accounts" |
| Back button | content-desc | "Back" or "Navigate up" |
| Google account status | text | Contains "Google" + connection status |
| Disconnect button | text | "Disconnect" (if connected) |

### Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap Disconnect (if shown) | text="Disconnect" | Confirmation dialog | Tap "Cancel" |
| Tap Back | content-desc="Back" | Return to Settings | Already returned |

### Data Validation

- Google should show as connected (user signed in via Google OAuth)
- Other providers may show as "Not connected"

### Known Issues

- Disconnect may have serious implications — confirmation dialog should warn

---

## Screen 24: dark-mode-dialog

**Navigation:** Home → Profile icon → Settings → tap "Dark Mode" item
**Last Verified:** — | **Definition Confidence:** MEDIUM

**Primary Identifier:** Dialog with text "Light", "Dark", "System" options

**Note:** This is a dialog, not a full screen. It appears as an overlay on Settings.

### Required Elements

| Element | Search By | Expected Value |
|---------|-----------|----------------|
| Light option | text | "Light" |
| Dark option | text | "Dark" |
| System option | text | "System" or "System default" |
| Current selection indicator | class or content-desc | Radio button or checkmark on current choice |

### Interactive Elements

| Action | Target | Expected Result | Return Method |
|--------|--------|-----------------|---------------|
| Tap "Light" | text="Light" | Light theme applied, dialog closes | Already on Settings |
| Tap "Dark" | text="Dark" | Dark theme applied, dialog closes | Already on Settings |
| Tap "System" | text="System" | System theme applied, dialog closes | Already on Settings |
| Tap outside dialog | Tap outside dialog bounds | Dialog dismissed, no change | Already on Settings |

### Data Validation

- Selected option should match current theme setting in DataStore
- Theme change should take effect immediately (no app restart needed)

### Known Issues

- Theme preference stored in local DataStore only — no backend sync
- Dialog may use AlertDialog or BottomSheet pattern

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
| Auth | text | "Continue with Google" |
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
| Dietary Restrictions | text | "Dietary Restrictions" (title) |
| Disliked Ingredients | text | "Disliked Ingredients" (title) |
| Cuisine Preferences | text | "Cuisine Preferences" (title) |
| Spice Level | text | "Spice Level" (title) |
| Cooking Time | text | "Cooking Time" (title) |
| Family Members | text | "Family Members" (title) |
| Notification Settings | text | "Notification Settings" (title) |
| Units | text | "Units" (title) |
| Edit Profile | text | "Edit Profile" (title) |
| Friends & Leaderboard | text | "Friends & Leaderboard" (title) |
| Connected Accounts | text | "Connected Accounts" (title) |
| Dark Mode Dialog | text | "Light" + "Dark" + "System" in dialog |

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
