# Family vs Personal Context — Screen-Level Design

**Status:** Design Complete (Brainstorming Approved)
**Last Updated:** 2026-03-08
**Depends On:** [User-Management-Design.md](./User-Management-Design.md)
**Approach:** Screen-Level Toggle (per-screen independent `Family | Personal` toggle)

---

## 1. Problem Statement

With the Household-Centric user management model, many screens need to distinguish between family (household) context and personal context. Currently, most screens have no concept of "who" is using them or whether the data shown is family-shared or individually owned.

**Key questions this design answers:**
- Which screens need a Family/Personal toggle?
- Which screens are always personal or always household?
- What can each role (Owner/Member/Guest) do in each view?
- How do Settings screens show both household and personal preferences?
- How does the AI chat adapt to the user's role?
- How does grocery list collaboration work with owner approval?
- How do favorites work across personal and family contexts?

---

## 2. Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Toggle pattern | Screen-level (per-screen independent toggle) | Matches Home screen pattern; each screen is self-contained; simpler ViewModels |
| Settings model | Tab toggle (Household \| My Preferences, same pattern as other screens) | Consistent UX across all screens; less scrolling |
| Chat behavior | Role-aware AI (context injected into prompt) | Owner gets direct actions; Members get "suggest to owner" |
| Favorites model | Two categories (My Favorites + Family Favorites) | Personal favorites are portable when leaving household; family favorites are auto-aggregated |
| Grocery permissions | Owner edits directly; Members suggest (owner approves); All can check off | Balances owner control with collaborative shopping |
| Solo user toggle | Hidden (not shown) | No UI clutter for users without a household |
| Toggle default | FAMILY when user has household | Family context is primary use case |
| Toggle persistence | DataStore per-screen key | Persists across app restarts |

---

## 3. Screen Categorization

### 3.1 Toggle Screens (Family | Personal)

These screens show the `[Family] | [Personal]` toggle in the top bar. Toggle only renders when `active_household_id` is set.

| Screen | Family View | Personal View |
|--------|------------|---------------|
| **Home** | Shared lunch/dinner + personal breakfast/snacks unified | Fully independent solo plan |
| **Stats** | Household leaderboard, combined cooking count, family achievements | Personal streak, personal history, personal achievements |
| **Grocery** | Household grocery list (owner edits, members suggest + check off) | Personal grocery list (from personal meal plan) |
| **Favorites** | Family Favorites (auto-calculated from members' individual favorites) | My Favorites (personal bookmarks) |
| **Recipe Rules** | Household rules (owner edits, members read-only) | Personal rules (affect personal slots only) |
| **Notifications** | Household notifications (joins, leaves, plan changes, suggestions) | Personal notifications (reminders, achievements) |

### 3.2 Always Personal (no toggle)

| Screen | Why |
|--------|-----|
| Chat | Role-aware AI but always your conversation |
| Edit Profile | Your account |
| Units | Display preference |
| Dietary Profiles | Personal contexts (home/hostel) |
| Connected Accounts | Your accounts |

### 3.3 Always Household (only visible when in household, no toggle)

| Screen | Why |
|--------|-----|
| Household Settings | Governance |
| Household Members | Roster management |
| Member Detail | Per-member config |
| Invite Code | Joining mechanism |
| Transfer Ownership | Governance |
| Leave Household | Membership action |
| Guest Duration | Temporal membership |
| Constraint Merge | Household aggregation view |

### 3.4 Settings Sub-Screens (Toggle — same pattern as other screens)

These use the same `[Household] | [My Preferences]` toggle as other screens. Only one scope visible at a time.

| Screen | Household Tab | My Preferences Tab |
|--------|---------------------|--------------------------|
| Dietary Restrictions | Household dietary type (owner edits, members read-only) | Your personal restrictions |
| Disliked Ingredients | Household dislikes (owner edits, members read-only) | Your personal dislikes |
| Cuisine Preferences | Household cuisines (owner edits, members read-only) | Your personal cuisine preferences |
| Spice Level | Household spice level (owner edits, members read-only) | Your personal spice tolerance |
| Cooking Schedule | Household cooking times + busy days (owner edits) | Your personal time constraints |

**For SOLO users:** Toggle hidden. Only personal settings shown. **Non-owners:** Household tab shows info banner ("Only the household owner can edit...") with disabled controls.

---

## 4. Permissions Matrix

### 4.1 Toggle Screens — Family View Permissions

| Screen | Action | Owner | Member | Guest |
|--------|--------|:-----:|:------:|:-----:|
| **Home** | View shared meals | Yes | Yes | Yes |
| | Swap shared meal | Yes | If `can_edit=true` | If `can_edit=true` |
| | Lock shared meal | Yes | If `can_edit=true` | If `can_edit=true` |
| | Regenerate family plan | Yes | No | No |
| **Grocery** | View family list | Yes | Yes | Yes |
| | Add/edit/delete items | Yes (direct) | Suggest → owner approves | Suggest → owner approves |
| | Check off / uncheck items | Yes | Yes | Yes |
| | Approve/reject suggestions | Yes | No | No |
| **Stats** | View family leaderboard | Yes | Yes | Yes |
| **Favorites** | View family favorites | Yes | Yes | Yes |
| | Favorite a recipe (personal action, feeds into family aggregate) | Yes | Yes | Yes |
| **Recipe Rules** | View household rules | Yes | Yes (read-only) | Yes (read-only) |
| | Add/edit/delete household rules | Yes | No | No |
| | Force override family conflicts | Yes | No | No |
| **Notifications** | View household notifications | Yes | Yes | Yes |
| | Dismiss household notifications | Yes | Yes (own only) | Yes (own only) |

### 4.2 Settings Sub-Screens — Toggle Permissions

| Screen Section | Owner | Member | Guest |
|---------------|:-----:|:------:|:-----:|
| Household section — view | Yes | Yes (read-only) | Yes (read-only) |
| Household section — edit | Yes | No | No |
| My Preferences section — view | Yes | Yes | Yes |
| My Preferences section — edit | Yes | Yes | Yes |

### 4.3 Always-Household Screens

| Action | Owner | Member | Guest |
|--------|:-----:|:------:|:-----:|
| View household details | Yes | Yes | Yes |
| Edit household name | Yes | No | No |
| Generate/share invite code | Yes | No | No |
| Add members by phone | Yes | No | No |
| Update member roles/permissions | Yes | No | No |
| Remove members | Yes | No | No |
| Transfer ownership | Yes | No | No |
| Leave household | No (must transfer first) | Yes | Yes |
| Create/update dietary profiles | Yes (own) | Yes (own) | Yes (own) |

### 4.4 Chat — Role-Aware Responses

| User asks... | Owner gets | Member/Guest gets |
|-------------|-----------|-------------------|
| "What's for dinner?" | Family dinner details | Same — shared meal is same for all |
| "Add chicken to recipe rules" | Executes via tool calling | "I can suggest this to [Owner name]. Want me to send the suggestion?" |
| "Regenerate my meal plan" | "Regenerate family plan or your personal plan?" | Only regenerates personal plan |
| "Change spice level to high" | "For the household or just for you?" | Changes personal preference only |
| "Who's in my family?" | Full member list with roles | Member list (without permission details) |

---

## 5. Data Flow & API Patterns

### 5.1 Toggle Architecture (Android)

```
┌─────────────────────────────────────────────────┐
│  Any Toggle Screen (e.g., GroceryScreen)         │
│                                                   │
│  ┌─────────────────────────────────────────────┐ │
│  │  [Family]  |  [Personal]    ← ViewScope     │ │
│  └─────────────────────────────────────────────┘ │
│                    │                              │
│                    ▼                              │
│  ┌─────────────────────────────────────────────┐ │
│  │  ViewModel                                   │ │
│  │  ├── viewScope: StateFlow<FAMILY|PERSONAL>   │ │
│  │  ├── userRole: StateFlow<OWNER|MEMBER|GUEST> │ │
│  │  ├── hasHousehold: Boolean                   │ │
│  │  │                                           │ │
│  │  │  when (viewScope) {                       │ │
│  │  │    FAMILY -> loadFromHouseholdRepo()      │ │
│  │  │    PERSONAL -> loadFromPersonalRepo()     │ │
│  │  │  }                                        │ │
│  │  │                                           │ │
│  │  │  // UI permissions derived from role      │ │
│  │  │  canEdit = viewScope == PERSONAL          │ │
│  │  │           || role == OWNER                │ │
│  │  └─────────────────────────────────────────┘ │
│                    │                              │
│          ┌────────┴────────┐                     │
│          ▼                 ▼                     │
│  ┌──────────────┐  ┌──────────────┐             │
│  │ Household    │  │ Personal     │             │
│  │ Repository   │  │ Repository   │             │
│  │ (API: /hh/)  │  │ (API: /me/)  │             │
│  └──────────────┘  └──────────────┘             │
└─────────────────────────────────────────────────┘
```

### 5.2 API Pattern for Toggle Screens

Each toggle screen maps to two API paths. All existing personal endpoints remain unchanged. Family endpoints are new, namespaced under `/households/{id}/`.

| Screen | Family View API | Personal View API |
|--------|----------------|-------------------|
| Home | `GET /households/{id}/meal-plans/current` | `GET /meal-plans/current` (existing) |
| Grocery | `GET /households/{id}/grocery` | `GET /grocery` (existing) |
| Stats | `GET /households/{id}/stats/monthly` | `GET /stats/monthly` (existing) |
| Favorites | `GET /households/{id}/favorites` | `GET /favorites` (existing) |
| Recipe Rules | `GET /households/{id}/recipe-rules` | `GET /recipe-rules` (existing) |
| Notifications | `GET /households/{id}/notifications` | `GET /notifications` (existing) |

**Zero breaking changes to current API.**

### 5.3 Grocery Suggestion Flow

```
Member taps "Add Item"               Owner sees notification
        │                                     │
        ▼                                     ▼
POST /households/{id}/grocery/suggest   GET /households/{id}/grocery/suggestions
  { "name": "Paneer", "quantity": 2 }    → [{ id, name, qty, suggested_by, status }]
        │                                     │
        │                              Owner taps Approve/Reject
        │                                     │
        │                                     ▼
        │                              PUT /households/{id}/grocery/suggestions/{sid}
        │                                { "status": "APPROVED" or "REJECTED" }
        │                                     │
        └─────── if APPROVED ─────────────────┘
                      │
                      ▼
              Item added to family grocery list
              Notification sent to suggesting member
```

### 5.4 Settings Dual-Section Data Flow

```
Settings Sub-Screen (e.g., Dietary Restrictions)
        │
        ├── Household Section
        │   └── GET /households/{id}/preferences
        │       → { dietary_type: "VEGETARIAN", restrictions: ["SATTVIC"] }
        │       → Read-only for MEMBER/GUEST
        │       → Editable for OWNER → PUT /households/{id}/preferences
        │
        └── My Preferences Section
            └── GET /users/me/preferences (existing)
                → { dietary_type: "EGGETARIAN", restrictions: [] }
                → Always editable → PUT /users/me/preferences (existing)
```

### 5.5 Favorites Two-Tier Model

```
Personal Favorite Action:
  POST /favorites/{recipe_id}  (existing)
  └── Saves to user's personal favorites
  └── Side effect: increments household favorite count

Family Favorites Aggregation:
  GET /households/{id}/favorites
  └── Returns recipes sorted by member favorite count
  └── { recipe_id, recipe_name, favorited_by: ["Sunita", "Aarav"], count: 2 }
```

---

## 6. Chat Role-Awareness

### Context Injection into AI Prompt

```
You are RasoiAI assistant for {user_name}.
Role: {OWNER|MEMBER|GUEST} in "{household_name}" household.
Household owner: {owner_name}.
Active view: {FAMILY|PERSONAL} plan.

Permissions:
- Can modify household rules: {yes/no}
- Can regenerate family plan: {yes/no}
- Can swap shared meals: {yes/no}

When user requests actions beyond their permissions,
offer to send a suggestion to {owner_name} instead.
```

### Behavior Examples

| Scenario | AI Response |
|----------|-------------|
| OWNER: "Add chicken to recipe rules" | Executes tool call to add rule |
| MEMBER: "Add chicken to recipe rules" | "I can suggest adding chicken to the household rules. Want me to send this suggestion to Sunita?" |
| GUEST: "What's for lunch?" | Shows shared family lunch for today |
| OWNER: "Regenerate meal plan" | "Regenerate the family plan or your personal plan?" |
| MEMBER: "Regenerate meal plan" | Only regenerates personal plan (no family option) |
| OWNER: "Change spice level" | "For the household or just for you?" |
| MEMBER: "Change spice level" | Changes personal spice preference only |

---

## 7. UI Changes Summary

| Screen | Current State | Change Required |
|--------|--------------|-----------------|
| Home | Toggle exists | No change (already implemented in HTML design) |
| Stats | No toggle, shows family leaderboard | Add toggle. Family: leaderboard + household stats. Personal: my streak + my history |
| Grocery | No toggle, no scope indicator | Add toggle. Family: shared list with suggest/approve flow. Personal: my items |
| Favorites | No toggle, personal only | Add toggle. Family: aggregated favorites with member attribution. Personal: my bookmarks |
| Recipe Rules | No toggle, no scope | Add toggle. Family: household rules (read-only for members). Personal: my rules |
| Notifications | No toggle | Add toggle. Family: household events. Personal: reminders/achievements |
| Chat | No role awareness | Inject role context into AI prompt. No UI toggle needed |
| Settings sub-screens | Single flat list | Add `[Household] \| [My Preferences]` toggle (same pattern as other toggle screens — one scope visible at a time) |
| Dietary Profiles | Personal only | No change (always personal) |
| Units | Personal only | No change |

### Toggle Persistence

- Toggle state stored in **DataStore** per screen key (e.g., `view_scope_grocery=FAMILY`)
- Defaults to `FAMILY` when user first joins a household
- Defaults to `PERSONAL` (and toggle hidden) for SOLO users
- Persists across app restarts

---

## 7B. Pending UI Designs

The following UI elements are referenced in this design but do not yet have HTML prototype files in `docs/UI Designs/UI-UX-Material3/`:

| Design Needed | Referenced By | Priority |
|---------------|---------------|----------|
| Grocery suggestion modal (`modal-suggest-grocery.html`) | Section 5.3 grocery suggestion flow | High — core multi-user interaction |
| Grocery suggestion approval list (inline in `main-grocery.html`) | Section 5.3 owner approval flow | High — owner sees pending suggestions |
| Family favorites card with `favorited_by` attribution | Section 5.5 two-tier model | Medium — shows `"Favorited by Sunita, Aarav"` |
| Household leaderboard card for Stats family view | Section 3.1 Stats toggle | Medium — member rankings, combined cooking count |
| Meal status indicator (`modal-meal-status.html` exists but needs cooked/skipped/ordered_out states) | `meal_plan_items.meal_status` field | Low — deferred to Theme 11 |
| Passive household read-only view | Section 9, Q2 dual context | Low — deferred to Phase 4 |

---

## 8. Constraint Interaction

### How Personal vs Household Preferences Affect Meal Generation

```
FAMILY PLAN generation uses:
├── Household preferences (dietary type, cuisines, spice level, cooking time)
├── Household recipe rules (INCLUDE/EXCLUDE for shared meals)
├── Merged allergies from ALL members (union — safety critical)
├── Merged dislikes from ALL members (union — avoid when possible)
└── Festival calendar

PERSONAL PLAN generation uses:
├── User's personal preferences (My Preferences section)
├── User's personal recipe rules
├── User's personal allergies only
├── User's personal dislikes only
└── Festival calendar
```

### Personal Preferences That Feed Into Household

Even in personal view, some data flows into the household:

| Personal Data | Household Impact |
|--------------|-----------------|
| Allergies | Always merged into household constraints (safety) |
| Favorites | Feeds into Family Favorites aggregate count |
| Meal status (cooked/skipped) | Visible in family activity feed |

---

## 9. Edge Cases

| Scenario | Behavior |
|----------|----------|
| SOLO user opens any toggle screen | No toggle shown. Sees personal view only (current behavior) |
| User creates household | Toggle appears on all toggle screens. Defaults to FAMILY |
| User leaves household | Toggle disappears. Falls back to personal view |
| GUEST with dual context (visiting + monitoring home) | Toggle shows host family. Home family visible via passive household (read-only) |
| Owner transfers ownership | New owner gains edit permissions on all household sections. Old owner becomes MEMBER with read-only household sections |
| Last linked member leaves | Household deactivated. All members become SOLO |
| Member's personal preference conflicts with household | No conflict — personal preferences only affect personal slots. Household preferences govern shared slots |

---

## 10. HTML Design Files Affected

### Files Needing Toggle Addition

| File | Change |
|------|--------|
| `main-stats.html` | Add Family/Personal toggle |
| `main-grocery.html` | Add Family/Personal toggle + suggestion/approve flow |
| `main-favorites.html` | Add Family/Personal toggle + family favorites view |
| `feature-recipe-rules.html` | Add Family/Personal toggle |
| `feature-notifications.html` | Add Family/Personal toggle |

### Files Needing Dual-Section Redesign

| File | Change |
|------|--------|
| `settings-dietary-restrictions.html` | Split into Household + My Preferences sections |
| `settings-disliked-ingredients.html` | Split into Household + My Preferences sections |
| `settings-cuisine-preferences.html` | Split into Household + My Preferences sections |
| `settings-spice-level.html` | Split into Household + My Preferences sections |
| `settings-cooking-time.html` | Split into Household + My Preferences sections |

### Files Already Compatible (no changes needed)

| File | Status |
|------|--------|
| `main-home.html` | Already has household toggle |
| `main-chat.html` | Role awareness is backend/prompt change, not UI |
| `settings-units.html` | Always personal |
| `feature-dietary-profiles.html` | Always personal |
| `settings-household.html` | Already household-scoped |
| `settings-household-members.html` | Already household-scoped |
| `settings-household-member-detail.html` | Already household-scoped |
| `modal-conflict-dialog.html` | Already household-scoped |
| `modal-invite-code-share.html` | Already household-scoped |
| `modal-constraint-merge.html` | Already household-scoped |
| `modal-transfer-ownership.html` | Already household-scoped |
| `modal-leave-household.html` | Already household-scoped |
| `modal-guest-duration.html` | Already household-scoped |

---

---

## 11. Open Design Items

| # | Item | Status | Notes |
|---|------|--------|-------|
| 1 | `modal-suggest-grocery.html` prototype | Not started | Needed for member suggestion flow (Section 5.3) |
| 2 | Family favorites card with attribution | Not started | Shows `"Favorited by Sunita, Aarav"` (Section 5.5) |
| 3 | Stats household leaderboard card | Not started | Member rankings in family view (Section 3.1) |
| 4 | Passive household read-only view | Deferred to Phase 4 | Q2: 1 active + 1 passive |
| 5 | Auto-departure scheduler design | Deferred to Phase 4 | Cron vs background worker vs on-request check |
| 6 | Real-time sync mechanism | Deferred | WebSocket vs polling vs FCM for multi-user updates |

---

*This is a companion document to [User-Management-Design.md](./User-Management-Design.md). It defines HOW existing and new screens adapt to the household model.*
