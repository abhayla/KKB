# Family/Personal Context UI Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add Family/Personal toggle to 5 screens, dual-section layout to 5 Settings screens, and grocery suggestion/approval flow — all as HTML UI design files.

**Architecture:** Each toggle screen uses the existing `renderPlanToggle()` from `shared.js` and conditionally renders different content based on `getState('active_plan_view_<screen>', 'family')`. Settings sub-screens split into two sections: "Household" (read-only for non-owners) and "My Preferences" (always editable). A new `renderScopeToggle(screenKey)` helper in `shared.js` extends the pattern with per-screen state keys.

**Tech Stack:** HTML, CSS, vanilla JS (shared.js/shared.css pattern), localStorage for state

**Design Doc:** `docs/requirements/User_Management/Family-Personal-Context-Design.md`

---

## Task 1: Extend shared.js with per-screen toggle helpers

**Files:**
- Modify: `docs/UI Designs/UI-UX-Material3/shared.js`

**Step 1: Add per-screen scope toggle helper**

Add after the existing `renderPlanToggle()` function (line ~526):

```javascript
// --- Per-Screen Scope Toggle ---
function renderScopeToggle(screenKey, familyLabel, personalLabel) {
  familyLabel = familyLabel || 'Family';
  personalLabel = personalLabel || 'Personal';
  const active = getState('scope_' + screenKey, 'family');
  return `<div class="plan-toggle" style="margin:0 var(--sp-md) var(--sp-md);">
    <button class="plan-toggle-btn ${active === 'family' ? 'active' : ''}" onclick="setScopeView('${screenKey}', 'family')">${familyLabel}</button>
    <button class="plan-toggle-btn ${active === 'personal' ? 'active' : ''}" onclick="setScopeView('${screenKey}', 'personal')">${personalLabel}</button>
  </div>`;
}

function setScopeView(screenKey, view) {
  setState('scope_' + screenKey, view);
  if (typeof render === 'function') render();
}

function getScopeView(screenKey) {
  return getState('scope_' + screenKey, 'family');
}

function isOwner() {
  const hh = getHousehold();
  if (!hh) return false;
  // In the mock, owner is user-1 (Ramesh). Current user is always user-1 in this prototype.
  return true;
}

function isSoloUser() {
  return !getHousehold();
}
```

**Step 2: Add mock data for household-scoped content**

Add after existing mock data sections:

```javascript
// --- Mock Household Notifications ---
const MOCK_HOUSEHOLD_NOTIFICATIONS = [
  { id: 'hn1', icon: '\uD83D\uDC65', title: 'Sunita joined the household', body: 'Sunita Sharma has joined Sharma Family via invite code.', time: '1 day ago', read: false, type: 'household' },
  { id: 'hn2', icon: '\uD83C\uDF7D', title: 'Meal plan regenerated', body: 'Ramesh regenerated the family meal plan for Mar 2-8.', time: '2 days ago', read: true, type: 'household' },
  { id: 'hn3', icon: '\uD83D\uDED2', title: 'Grocery suggestion', body: 'Sunita suggested adding "Paneer (500g)" to the grocery list.', time: '3 hours ago', read: false, type: 'grocery' },
  { id: 'hn4', icon: '\uD83D\uDD04', title: 'Aarav swapped a meal', body: 'Aarav swapped Wednesday dinner from Palak Paneer to Chole.', time: '1 day ago', read: true, type: 'household' }
];

// --- Mock Household Recipe Rules ---
const MOCK_HOUSEHOLD_RULES = [
  { id: 'hr1', type: 'INCLUDE', target: 'Chai', frequency: 'DAILY', meals: ['breakfast', 'snacks'], enforcement: 'STRICT', scope: 'household' },
  { id: 'hr2', type: 'INCLUDE', target: 'Dal', frequency: 'TIMES_PER_WEEK', times: 4, meals: ['lunch', 'dinner'], enforcement: 'STRICT', scope: 'household' },
  { id: 'hr3', type: 'EXCLUDE', target: 'Mushroom', frequency: 'NEVER', meals: [], enforcement: 'STRICT', scope: 'household' }
];

const MOCK_PERSONAL_RULES = [
  { id: 'pr1', type: 'INCLUDE', target: 'Egg Bhurji', frequency: 'TIMES_PER_WEEK', times: 3, meals: ['breakfast'], enforcement: 'FLEXIBLE', scope: 'personal' },
  { id: 'pr2', type: 'EXCLUDE', target: 'Onion', frequency: 'SPECIFIC_DAYS', specific_days: ['TUESDAY'], meals: [], enforcement: 'STRICT', scope: 'personal' }
];

// --- Mock Grocery Suggestions ---
const MOCK_GROCERY_SUGGESTIONS = [
  { id: 'gs1', name: 'Paneer', qty: '500', unit: 'g', suggested_by: 'Sunita', status: 'pending' },
  { id: 'gs2', name: 'Coconut Milk', qty: '400', unit: 'ml', suggested_by: 'Aarav', status: 'pending' },
  { id: 'gs3', name: 'Saffron', qty: '1', unit: 'g', suggested_by: 'Sunita', status: 'approved' }
];

// --- Mock Family Favorites ---
const MOCK_FAMILY_FAVORITES = [
  { recipe_id: 'dal-fry', recipe_name: 'Dal Fry', category: 'dal', prep_time: 20, favorited_by: ['Ramesh', 'Sunita', 'Aarav'], count: 3 },
  { recipe_id: 'aloo-paratha', recipe_name: 'Aloo Paratha', category: 'paratha', prep_time: 25, favorited_by: ['Ramesh', 'Sunita'], count: 2 },
  { recipe_id: 'masala-chai', recipe_name: 'Masala Chai', category: 'chai', prep_time: 10, favorited_by: ['Ramesh', 'Aarav'], count: 2 },
  { recipe_id: 'paneer-tikka', recipe_name: 'Paneer Tikka Masala', category: 'curry', prep_time: 30, favorited_by: ['Sunita'], count: 1 }
];

// --- Mock Household Stats ---
const MOCK_HOUSEHOLD_STATS = {
  members: [
    { name: 'Ramesh', meals_cooked: 12, streak: 7, avatar_color: 'primary' },
    { name: 'Sunita', meals_cooked: 18, streak: 7, avatar_color: 'secondary' },
    { name: 'Aarav', meals_cooked: 5, streak: 3, avatar_color: 'tertiary' }
  ],
  total_meals: 35,
  total_recipes_tried: 22,
  most_popular: 'Dal Fry'
};

// --- Mock Household Preferences (for Settings dual-section) ---
const MOCK_HOUSEHOLD_PREFERENCES = {
  dietary_restrictions: ['sattvic'],
  dislikes: ['Karela', 'Baingan', 'Mushroom'],
  cuisines: ['north', 'south'],
  spice_level: 'medium',
  weekday_time: 30,
  weekend_time: 60,
  busy_days: ['MONDAY', 'WEDNESDAY', 'FRIDAY']
};

const MOCK_PERSONAL_PREFERENCES = {
  dietary_restrictions: [],
  dislikes: ['Lauki'],
  cuisines: ['north', 'west'],
  spice_level: 'spicy',
  weekday_time: 20,
  weekend_time: 45,
  busy_days: ['MONDAY']
};
```

**Step 3: Add dual-section render helper**

```javascript
// --- Dual Section Helper for Settings ---
function renderDualSectionHeader(title, isHousehold) {
  const hh = getHousehold();
  if (isHousehold) {
    const canEdit = isOwner();
    return `<div class="section-header" style="margin-top:var(--sp-md);">
      <span class="section-title">${hh ? hh.name : 'Household'}</span>
      ${!canEdit ? '<span class="scope-label personal" style="background:var(--secondary-container);color:var(--on-secondary-container);">READ ONLY</span>' : ''}
    </div>`;
  }
  return `<div class="section-header" style="margin-top:var(--sp-lg);">
    <span class="section-title">My Preferences</span>
  </div>`;
}
```

**Step 4: Verify shared.js loads without errors**

Open any existing HTML file (e.g., `main-home.html`) in a browser and check the console for JS errors.

**Step 5: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/shared.js"
git commit -m "feat: add per-screen scope toggle and household mock data to shared.js"
```

---

## Task 2: Add toggle to main-stats.html

**Files:**
- Modify: `docs/UI Designs/UI-UX-Material3/main-stats.html`

**Step 1: Read the current file**

Read `main-stats.html` fully to understand the existing structure.

**Step 2: Add toggle and dual-view rendering**

Replace the content rendering (after `initPage()`) with scope-aware rendering. The toggle goes after the top bar. When `scope === 'family'`, show:
- Household leaderboard (member cards with avatars, meals cooked, streaks)
- Household total stats
- Most popular recipe

When `scope === 'personal'`, show the existing content (streak, calendar, monthly summary, achievements).

Key changes:
1. After `renderTopBar()`, insert: `${!isSoloUser() ? renderScopeToggle('stats') : ''}`
2. Wrap existing content in a `if (getScopeView('stats') === 'personal' || isSoloUser())` check
3. Add family stats view with `MOCK_HOUSEHOLD_STATS` data showing:
   - Member leaderboard cards (avatar, name, meals cooked, streak)
   - Total household meals stat card
   - Most popular recipe stat card
   - Family achievements section

**Step 3: Test in browser**

Open `main-stats.html`, verify:
- Toggle appears at top (Family | Personal)
- Family view shows leaderboard with Ramesh, Sunita, Aarav
- Personal view shows existing streak/calendar/achievements
- Toggle state persists on page reload

**Step 4: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/main-stats.html"
git commit -m "feat: add Family/Personal toggle to Stats screen"
```

---

## Task 3: Add toggle to main-grocery.html with suggestion/approval flow

**Files:**
- Modify: `docs/UI Designs/UI-UX-Material3/main-grocery.html`

**Step 1: Read the current file fully**

**Step 2: Add toggle and dual-view rendering**

Key changes:
1. After top bar, insert scope toggle: `${!isSoloUser() ? renderScopeToggle('grocery') : ''}`
2. Family view:
   - Shows existing grocery list (from shared meal plan)
   - FAB changes behavior: non-owner taps "+" to open a "Suggest Item" modal (inline form or `modal-suggest-grocery.html`)
   - Add a "Suggestions" section at the bottom showing `MOCK_GROCERY_SUGGESTIONS` with Approve/Reject buttons (visible only to owner)
   - Each suggestion shows: item name, qty, "Suggested by [name]", and status badge
   - All members can check off / uncheck items
3. Personal view:
   - Shows a simpler personal grocery list (can be empty with "Your personal grocery items" message)
   - FAB adds items directly (no approval needed)

**Step 3: Test in browser**

Open `main-grocery.html`, verify:
- Toggle appears
- Family view shows grocery list + suggestions section
- Suggestions show Approve/Reject buttons
- Personal view shows personal items with direct add

**Step 4: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/main-grocery.html"
git commit -m "feat: add Family/Personal toggle to Grocery with suggestion/approval flow"
```

---

## Task 4: Add toggle to main-favorites.html with two-tier model

**Files:**
- Modify: `docs/UI Designs/UI-UX-Material3/main-favorites.html`

**Step 1: Read the current file fully**

**Step 2: Add toggle and dual-view rendering**

Key changes:
1. After top bar, insert scope toggle: `${!isSoloUser() ? renderScopeToggle('favorites', 'Family Favorites', 'My Favorites') : ''}`
2. Family view:
   - Shows `MOCK_FAMILY_FAVORITES` — recipe cards with member attribution
   - Each card shows: recipe name, emoji, prep time, AND "Liked by: Ramesh, Sunita, Aarav" with member avatars
   - Sorted by favorite count (most liked first)
   - Filter chips still work (all/breakfast/lunch/dinner/snacks)
3. Personal view:
   - Existing behavior (user's personal bookmarked recipes)
   - No changes needed to current rendering

**Step 3: Test in browser**

Open `main-favorites.html`, verify:
- Toggle appears (Family Favorites | My Favorites)
- Family view shows recipes with "Liked by" member attribution
- Personal view shows existing behavior
- Heart button on personal view works as before

**Step 4: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/main-favorites.html"
git commit -m "feat: add Family/Personal toggle to Favorites with two-tier model"
```

---

## Task 5: Add toggle to feature-recipe-rules.html

**Files:**
- Modify: `docs/UI Designs/UI-UX-Material3/feature-recipe-rules.html`

**Step 1: Read the current file fully**

**Step 2: Add toggle and dual-view rendering**

Key changes:
1. After top bar (before tab bar), insert scope toggle: `${!isSoloUser() ? renderScopeToggle('rules', 'Household Rules', 'My Rules') : ''}`
2. Family view:
   - Shows `MOCK_HOUSEHOLD_RULES` (Include/Exclude rules)
   - Edit/delete buttons only visible if `isOwner()`
   - FAB (add rule) only visible if `isOwner()`
   - Non-owners see a subtle info bar: "Household rules are managed by [owner name]. These apply to shared meals."
   - Tab bar (Rules | Nutrition Goals) still works within family scope
3. Personal view:
   - Shows `MOCK_PERSONAL_RULES`
   - All edit/delete/add actions available (your own rules)
   - Info bar: "Personal rules apply to your breakfast and snacks slots."

**Step 3: Test in browser**

Open `feature-recipe-rules.html`, verify:
- Toggle appears (Household Rules | My Rules)
- Family view shows household rules with edit controls (owner)
- Personal view shows personal rules with full edit access

**Step 4: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/feature-recipe-rules.html"
git commit -m "feat: add Household/Personal toggle to Recipe Rules screen"
```

---

## Task 6: Add toggle to feature-notifications.html

**Files:**
- Modify: `docs/UI Designs/UI-UX-Material3/feature-notifications.html`

**Step 1: Read the current file fully**

**Step 2: Add toggle and dual-view rendering**

Key changes:
1. After top bar (before filter tabs), insert scope toggle: `${!isSoloUser() ? renderScopeToggle('notifications') : ''}`
2. Family view:
   - Shows `MOCK_HOUSEHOLD_NOTIFICATIONS` (joins, leaves, plan changes, grocery suggestions, meal swaps)
   - Each notification has a household member avatar next to it
   - Filter tabs: All | Unread (same pattern, filtered from household notifications)
3. Personal view:
   - Shows existing `MOCK_NOTIFICATIONS` (festivals, streaks, achievements, reminders)
   - Existing behavior unchanged

**Step 3: Test in browser**

Open `feature-notifications.html`, verify:
- Toggle appears (Family | Personal)
- Family view shows household notifications with member context
- Personal view shows existing notifications

**Step 4: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/feature-notifications.html"
git commit -m "feat: add Family/Personal toggle to Notifications screen"
```

---

## Task 7: Add dual-section to settings-dietary-restrictions.html

**Files:**
- Modify: `docs/UI Designs/UI-UX-Material3/settings-dietary-restrictions.html`

**Step 1: Read the current file fully**

**Step 2: Redesign with two sections**

Key changes:
1. If `!isSoloUser()`, show two sections:
   - **Household section** (top): Shows `MOCK_HOUSEHOLD_PREFERENCES.dietary_restrictions` — the household's dietary restrictions. If `isOwner()`, checkboxes are editable. If not, checkboxes are disabled with a "READ ONLY" badge in the section header.
   - **My Preferences section** (bottom): Shows the user's personal dietary restrictions (current behavior). Always editable.
   - Separator between sections with a horizontal rule.
2. If `isSoloUser()`, show only "My Preferences" section (current behavior, no changes).
3. Description text updated:
   - Household section: "Dietary restrictions for shared meals (lunch & dinner). Applied during family meal plan generation."
   - Personal section: "Your personal dietary restrictions for breakfast & snacks."

**Step 3: Test in browser**

Open `settings-dietary-restrictions.html`, verify:
- Two sections visible: "Sharma Family" (with restrictions) and "My Preferences"
- Household section shows READ ONLY badge for non-owners
- Both sections have independent checkbox state
- Save button saves both sections

**Step 4: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/settings-dietary-restrictions.html"
git commit -m "feat: add dual-section layout to Dietary Restrictions settings"
```

---

## Task 8: Add dual-section to settings-disliked-ingredients.html

**Files:**
- Modify: `docs/UI Designs/UI-UX-Material3/settings-disliked-ingredients.html`

**Step 1: Read the current file fully**

**Step 2: Apply same dual-section pattern as Task 7**

Key changes:
1. Household section: Shows `MOCK_HOUSEHOLD_PREFERENCES.dislikes` — chips showing Karela, Baingan, Mushroom. Owner can remove/add. Members see read-only chips.
2. My Preferences section: User's personal dislikes (e.g., Lauki). Always editable.
3. Description text:
   - Household: "Ingredients avoided in shared meals for all family members."
   - Personal: "Ingredients you personally want to avoid in your meals."

**Step 3: Test in browser**

**Step 4: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/settings-disliked-ingredients.html"
git commit -m "feat: add dual-section layout to Disliked Ingredients settings"
```

---

## Task 9: Add dual-section to settings-cuisine-preferences.html

**Files:**
- Modify: `docs/UI Designs/UI-UX-Material3/settings-cuisine-preferences.html`

**Step 1: Read the current file fully**

**Step 2: Apply same dual-section pattern**

Key changes:
1. Household section: Shows `MOCK_HOUSEHOLD_PREFERENCES.cuisines` (north, south). Owner editable, members read-only.
2. My Preferences section: User's personal cuisine preferences (north, west). Always editable.
3. Description text:
   - Household: "Cuisine types for shared family meals."
   - Personal: "Your preferred cuisines for personal meal slots."

**Step 3: Test in browser**

**Step 4: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/settings-cuisine-preferences.html"
git commit -m "feat: add dual-section layout to Cuisine Preferences settings"
```

---

## Task 10: Add dual-section to settings-spice-level.html

**Files:**
- Modify: `docs/UI Designs/UI-UX-Material3/settings-spice-level.html`

**Step 1: Read the current file fully**

**Step 2: Apply same dual-section pattern**

Key changes:
1. Household section: Shows `MOCK_HOUSEHOLD_PREFERENCES.spice_level` (medium) — radio options. Owner editable, members see selected option with disabled radios.
2. My Preferences section: User's personal spice level (spicy). Always editable radio selection.
3. Description text:
   - Household: "Spice level for shared family meals."
   - Personal: "Your preferred spice level for personal meals."

**Step 3: Test in browser**

**Step 4: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/settings-spice-level.html"
git commit -m "feat: add dual-section layout to Spice Level settings"
```

---

## Task 11: Add dual-section to settings-cooking-time.html

**Files:**
- Modify: `docs/UI Designs/UI-UX-Material3/settings-cooking-time.html`

**Step 1: Read the current file fully**

**Step 2: Apply same dual-section pattern**

Key changes:
1. Household section: Shows household weekday/weekend cooking times and busy days. Owner editable, members read-only.
2. My Preferences section: User's personal cooking time constraints. Always editable.
3. Description text:
   - Household: "Cooking time limits for shared family meals."
   - Personal: "Your personal cooking time limits for breakfast & snacks."

**Step 3: Test in browser**

**Step 4: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/settings-cooking-time.html"
git commit -m "feat: add dual-section layout to Cooking Schedule settings"
```

---

## Task 12: Update index.html with context annotations

**Files:**
- Modify: `docs/UI Designs/UI-UX-Material3/index.html`

**Step 1: Read index.html**

**Step 2: Add scope annotations to the screen index**

Add visual indicators next to each screen link showing its context type:
- Toggle screens: `[Family | Personal]` badge
- Dual-section screens: `[Dual Section]` badge
- Always-personal screens: `[Personal]` badge
- Always-household screens: `[Household]` badge

This helps designers quickly identify which screens have been updated for the household model.

**Step 3: Test in browser**

Open `index.html`, verify badges appear next to each screen link.

**Step 4: Commit**

```bash
git add "docs/UI Designs/UI-UX-Material3/index.html"
git commit -m "feat: add scope context badges to UI design index"
```

---

## Task 13: Final review and cross-screen navigation test

**Files:**
- All modified files

**Step 1: Open each modified screen in browser and verify**

Checklist:
- [ ] `main-stats.html` — Toggle works, family leaderboard shows, personal shows existing stats
- [ ] `main-grocery.html` — Toggle works, family shows suggestions, personal shows personal items
- [ ] `main-favorites.html` — Toggle works, family shows aggregated favorites, personal shows bookmarks
- [ ] `feature-recipe-rules.html` — Toggle works, family shows read-only for non-owners, personal shows editable rules
- [ ] `feature-notifications.html` — Toggle works, family shows household events, personal shows existing notifications
- [ ] `settings-dietary-restrictions.html` — Dual section: household + personal
- [ ] `settings-disliked-ingredients.html` — Dual section: household + personal
- [ ] `settings-cuisine-preferences.html` — Dual section: household + personal
- [ ] `settings-spice-level.html` — Dual section: household + personal
- [ ] `settings-cooking-time.html` — Dual section: household + personal
- [ ] `index.html` — Scope badges visible
- [ ] Navigation between screens preserves toggle states (localStorage)
- [ ] `main-home.html` — Existing toggle still works (regression check)

**Step 2: Test solo user mode**

In browser console: `localStorage.removeItem('rasoiai_household')` then reload each toggle screen. Verify:
- No toggle appears
- Screen shows personal-only content
- No "Household" section in settings sub-screens

Reset: `localStorage.removeItem('rasoiai__initialized')` and reload to restore Sharma defaults.

**Step 3: Commit all remaining changes**

```bash
git add "docs/UI Designs/UI-UX-Material3/"
git commit -m "feat: complete Family/Personal context UI for all 11 screens"
```

---

## Summary

| Task | Screen | Change Type | Estimated Steps |
|:----:|--------|:-----------:|:---------------:|
| 1 | shared.js | Helper functions + mock data | 5 |
| 2 | main-stats.html | Toggle (Family leaderboard / Personal stats) | 4 |
| 3 | main-grocery.html | Toggle + suggestion/approval flow | 4 |
| 4 | main-favorites.html | Toggle (Family favorites / My favorites) | 4 |
| 5 | feature-recipe-rules.html | Toggle (Household rules / My rules) | 4 |
| 6 | feature-notifications.html | Toggle (Family / Personal notifications) | 4 |
| 7 | settings-dietary-restrictions.html | Dual section | 4 |
| 8 | settings-disliked-ingredients.html | Dual section | 4 |
| 9 | settings-cuisine-preferences.html | Dual section | 4 |
| 10 | settings-spice-level.html | Dual section | 4 |
| 11 | settings-cooking-time.html | Dual section | 4 |
| 12 | index.html | Scope badges | 4 |
| 13 | All files | Cross-screen test + solo user test | 3 |

**Total: 13 tasks, ~48 steps**

**Dependencies:** Task 1 (shared.js) must complete first. Tasks 2-12 can be done in any order after Task 1. Task 13 is the final verification.
