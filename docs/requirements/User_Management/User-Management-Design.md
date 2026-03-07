# User Management Design — RasoiAI

**Status:** Brainstorming Complete + Gap Analysis Done (12 Themes Approved, 10 Open Questions Resolved, 7 Must-Fix Items Identified)
**Last Updated:** 2026-03-07
**Approach:** Household-Centric (Approach 1 — recommended)

---

## 1. Problem Statement

RasoiAI currently treats each user as fully isolated. Family members are metadata-only — used for allergy/dietary constraint checking but with no concept of shared meal plans or household coordination.

**Current limitations:**
- If Mom and Dad both install the app, they get completely independent meal plans with zero overlap handling
- No way to share a meal plan across family members
- No guest/temporary membership concept
- Family members who install the app cannot see the family's meal plan

---

## 2. Requirements Summary

### Decisions Made

| Aspect | Decision |
|--------|----------|
| **Meal plan model** | Shared family plan (lunch/dinner) + personal override slots (breakfast/snacks) |
| **Permissions** | Owner has full control. Can grant members role-based edit: swap/lock individual meals but not regenerate. Personal slots are individually managed. |
| **Joining mechanism** | Invite code OR phone number auto-link (both supported) |
| **Guest/temporary membership** | Time-bounded. Dual view (toggle family vs personal plan). Guest constraints merge into family plan during stay. |
| **Leaving** | Guest's constraints removed, family plan regenerates. Guest returns to previous state. |
| **Family member toggle** | Any family member can toggle between "Family Plan" and "My Own Plan" in the app |

### User States

A user can exist in one of these states:

```
SOLO ──── (creates household) ───> OWNER
  ^                                   |
  |                                   | (invites others)
  |                                   v
  |                                MEMBER (permanent)
  |                                GUEST  (temporary, time-bounded)
  |                                   |
  +──── (leaves household) ───────────+
```

| State | Description |
|-------|-------------|
| **SOLO** | Not part of any household. Manages own meals independently (current behavior). |
| **OWNER** | Created a household. Manages shared family plan. Full control over household settings and member permissions. |
| **MEMBER** | Joined a household permanently. Sees shared family plan + personal overrides. Can swap/lock if owner allows. Cannot regenerate. |
| **GUEST** | Joined a household temporarily with a leave date. Dual view: can toggle between host family plan and their own plan. Constraints merge into family plan. On departure, constraints removed and family plan regenerates. |

---

## 3. Recommended Approach: Household-Centric

### Why This Approach

1. **Clean data model** — A household is a first-class entity, not an implicit link through a user ID. Survives owner phone changes, account deletion.
2. **Right complexity** — Simpler than a full membership graph, but avoids the fragility of using owner's user_id as the household key.
3. **Natural guest flow** — `household_members(is_temporary=true, leave_date='2026-03-14')` is straightforward.
4. **Contained meal generation change** — AI prompt already merges family constraints. Change is: fetch from `household_members` (linked users) instead of `family_members` (metadata).
5. **Scales** — Multiple households, household settings, household grocery lists are natural extensions.

### Approaches Considered

| # | Approach | Summary | Why Not |
|---|----------|---------|---------|
| 1 | **Household-Centric** | New `households` table as center of gravity | **Selected** |
| 2 | User-Linked | Extend `family_members` + use owner's user_id as household key | Fragile if owner changes; implicit household identity |
| 3 | Membership Graph | Flexible relationship model with JSON permissions | Over-engineered for MVP; too many permission combinations to test |

---

## 4. Data Model

### New Tables

```
households
├── id (UUID, PK)
├── name (String 100)          -- "Sharma Family"
├── invite_code (String 8, UNIQUE, INDEXED)  -- shareable join code
├── owner_id (FK -> users)     -- who created it
├── is_active (Boolean, default=True)
├── created_at (DateTime)
└── updated_at (DateTime)

household_members
├── id (UUID, PK)
├── household_id (FK -> households)
├── user_id (FK -> users, nullable)           -- linked app user (null = metadata-only member)
├── family_member_id (FK -> family_members, nullable) -- link to existing metadata
├── role (String 20)           -- OWNER, MEMBER, GUEST
├── can_edit_shared_plan (Boolean, default=False) -- swap/lock permission
├── is_temporary (Boolean, default=False)
├── join_date (Date)
├── leave_date (Date, nullable)  -- null = permanent
├── previous_household_id (UUID, nullable)  -- for guest return
├── status (String 20)        -- ACTIVE, PAUSED, LEFT
├── created_at (DateTime)
└── updated_at (DateTime)

UNIQUE(household_id, user_id)  -- a user can only be in a household once
```

### Extended Tables

```
meal_plans (extended)
├── existing fields...
├── household_id (FK -> households, nullable)  -- if set, this is a family plan
└── slot_scope (String 20, default='ALL')      -- ALL, SHARED, PERSONAL

meal_plan_items (extended)
├── existing fields...
├── scope (String 20, default='FAMILY')  -- FAMILY or PERSONAL
└── for_user_id (FK -> users, nullable)  -- only set for PERSONAL items

users (extended)
├── existing fields...
└── active_household_id (FK -> households, nullable)  -- current household
```

### Entity Relationship

```
users ──1:1──> user_preferences
  |
  ├──1:N──> family_members (metadata, existing)
  |
  ├──1:N──> meal_plans (personal plans)
  |
  ├──N:1──> households (via active_household_id)
  |
  └──1:N──> household_members (memberships)
                |
                └──N:1──> households
                              |
                              └──1:N──> meal_plans (family plans, via household_id)
```

---

## 5. Meal Plan Structure

### Shared vs Personal Slots

```
┌─────────────────────────────────────────────────────┐
│  FAMILY MEAL PLAN (household_id = h1)               │
│  Generated by: Owner (considers all members)         │
│                                                      │
│  Monday:                                             │
│  ├── Breakfast: [PERSONAL per member]                │
│  ├── Lunch:     [SHARED - Dal Fry + Jeera Rice]     │
│  ├── Dinner:    [SHARED - Paneer + Naan]            │
│  └── Snacks:    [PERSONAL per member]                │
│                                                      │
│  Shared slots: Anyone with can_edit_shared_plan      │
│  can swap/lock. Only OWNER can regenerate.           │
│                                                      │
│  Personal slots: Each member manages their own.      │
└─────────────────────────────────────────────────────┘
```

### How Generation Works

```
1. Owner triggers POST /api/v1/households/{id}/meal-plans/generate
2. Backend loads:
   ├── Household preferences (owner's base preferences)
   ├── All ACTIVE household_members
   │   ├── Linked users -> merge their allergies, dislikes
   │   └── Metadata members -> merge their health_conditions
   └── Recipe rules (owner's rules, shared scope)
3. AI generates 7-day plan with SHARED slots (lunch, dinner)
4. Plan saved with household_id, slot_scope='SHARED'
5. Each member's personal slots generated separately (or manually managed)
```

### Constraint Merging (for AI prompt)

```
Owner: allergies=[peanuts], dislikes=[karela]
Member (Dad): allergies=[cashews], dislikes=[baingan]
Guest (Uncle): allergies=[shellfish], dislikes=[mushroom]

Merged for AI prompt:
  Allergies (NEVER): peanuts, cashews, shellfish
  Dislikes (AVOID):  karela, baingan, mushroom
  Dietary:           intersection of all members' dietary types
                     (if any member is vegetarian, shared meals are vegetarian)
```

---

## 6. Join Flows

### Flow A: Invite Code

```
Owner                                    New Member
  |                                          |
  |-- POST /households/{id}/invite-code ---->|
  |   Response: { code: "A1B2C3D4" }        |
  |                                          |
  |   (shares code via WhatsApp/SMS)         |
  |                                          |
  |                    POST /households/join  |
  |                    { invite_code: "A1B2C3D4" }
  |                                          |
  |<---- Backend links user to household ----|
  |      Sets role=MEMBER or GUEST           |
  |      Sets can_edit_shared_plan=false      |
  |                                          |
  |-- Owner can update permissions later ---->|
```

### Flow B: Phone Number Auto-Link

```
Owner                                    Existing User
  |                                          |
  |-- POST /households/{id}/members          |
  |   { phone_number: "+919876543210" }      |
  |                                          |
  |   Backend checks: user with this phone?  |
  |   ├── YES: Creates household_member      |
  |   │        with linked user_id           |
  |   │        Sends notification to user    |
  |   └── NO:  Creates family_member         |
  |            (metadata only, like today)    |
  |                                          |
  |            (User installs app later)      |
  |            Auth with same phone number    |
  |            Backend auto-links to          |
  |            existing household_member      |
```

### Flow C: Guest (Temporary)

```
Guest                        Host Family Owner
  |                               |
  |-- Joins via code/phone ------>|
  |   { is_temporary: true,       |
  |     leave_date: "2026-03-14" }|
  |                               |
  |   Backend:                    |
  |   1. Saves guest's current    |
  |      active_household_id as   |
  |      previous_household_id    |
  |   2. Sets guest's             |
  |      active_household_id =    |
  |      host household           |
  |   3. status = ACTIVE          |
  |                               |
  |   Guest now sees:             |
  |   [Toggle: Family | My Plan]  |
  |                               |
  |-- On leave_date (or manual)-->|
  |                               |
  |   Backend:                    |
  |   1. Set membership           |
  |      status = LEFT            |
  |   2. Restore guest's          |
  |      active_household_id =    |
  |      previous_household_id    |
  |   3. Remove guest constraints |
  |      from host family plan    |
  |   4. Trigger family plan      |
  |      regeneration             |
```

---

## 7. Permissions Model

```
┌──────────────┬──────────────┬──────────────┬──────────────┐
│ Action       │ OWNER        │ MEMBER       │ GUEST        │
│              │              │ (if allowed) │ (if allowed) │
├──────────────┼──────────────┼──────────────┼──────────────┤
│ View family  │ Yes          │ Yes          │ Yes          │
│ meal plan    │              │              │              │
├──────────────┼──────────────┼──────────────┼──────────────┤
│ Swap shared  │ Yes          │ If           │ If           │
│ meal item    │              │ can_edit=true│ can_edit=true│
├──────────────┼──────────────┼──────────────┼──────────────┤
│ Lock shared  │ Yes          │ If           │ If           │
│ meal item    │              │ can_edit=true│ can_edit=true│
├──────────────┼──────────────┼──────────────┼──────────────┤
│ Regenerate   │ Yes          │ No           │ No           │
│ family plan  │              │              │              │
├──────────────┼──────────────┼──────────────┼──────────────┤
│ Manage own   │ Yes          │ Yes          │ Yes          │
│ personal     │              │              │              │
│ slots        │              │              │              │
├──────────────┼──────────────┼──────────────┼──────────────┤
│ Invite       │ Yes          │ No           │ No           │
│ members      │              │              │              │
├──────────────┼──────────────┼──────────────┼──────────────┤
│ Remove       │ Yes          │ No           │ No           │
│ members      │              │              │              │
├──────────────┼──────────────┼──────────────┼──────────────┤
│ Grant edit   │ Yes          │ No           │ No           │
│ permission   │              │              │              │
├──────────────┼──────────────┼──────────────┼──────────────┤
│ Toggle       │ Yes          │ Yes          │ Yes          │
│ Family/My    │              │              │              │
│ Plan view    │              │              │              │
├──────────────┼──────────────┼──────────────┼──────────────┤
│ Leave        │ No (must     │ Yes          │ Yes (or      │
│ household    │ transfer     │              │ auto on       │
│              │ ownership)   │              │ leave_date)  │
└──────────────┴──────────────┴──────────────┴──────────────┘
```

---

## 8. API Endpoints (Proposed)

### Household Management

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/v1/households` | Create household | Owner |
| GET | `/api/v1/households/{id}` | Get household details | Member |
| PUT | `/api/v1/households/{id}` | Update household name/settings | Owner |
| DELETE | `/api/v1/households/{id}` | Deactivate household | Owner |

### Membership

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/v1/households/{id}/invite-code` | Generate/refresh invite code | Owner |
| POST | `/api/v1/households/join` | Join via invite code | Any user |
| POST | `/api/v1/households/{id}/members` | Add member by phone number | Owner |
| GET | `/api/v1/households/{id}/members` | List all members | Member |
| PUT | `/api/v1/households/{id}/members/{mid}` | Update role/permissions | Owner |
| DELETE | `/api/v1/households/{id}/members/{mid}` | Remove member | Owner |
| POST | `/api/v1/households/{id}/leave` | Leave household | Member/Guest |

### Family Meal Plans

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/v1/households/{id}/meal-plans/generate` | Generate shared plan | Owner |
| GET | `/api/v1/households/{id}/meal-plans/current` | Get current family plan | Member |
| POST | `/api/v1/households/{id}/meal-plans/{pid}/items/{iid}/swap` | Swap shared meal | Owner or can_edit |
| PUT | `/api/v1/households/{id}/meal-plans/{pid}/items/{iid}/lock` | Lock shared meal | Owner or can_edit |

### Personal Overrides

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/v1/meal-plans/current` | Get personal plan (existing, unchanged) | Self |
| POST | `/api/v1/meal-plans/generate` | Generate personal plan (existing) | Self |

---

## 9. Android Changes (High-Level)

### New Domain Models

```kotlin
data class Household(
    val id: String,
    val name: String,
    val inviteCode: String?,
    val ownerId: String,
    val members: List<HouseholdMember>
)

data class HouseholdMember(
    val id: String,
    val userId: String?,          // null = metadata-only
    val familyMemberId: String?,  // link to existing FamilyMember
    val name: String,
    val role: HouseholdRole,      // OWNER, MEMBER, GUEST
    val canEditSharedPlan: Boolean,
    val isTemporary: Boolean,
    val leaveDate: LocalDate?
)

enum class HouseholdRole { OWNER, MEMBER, GUEST }
```

### Home Screen Changes

```
┌─────────────────────────────────────────┐
│  [Family Plan]  |  [My Plan]   ← Toggle │
├─────────────────────────────────────────┤
│                                         │
│  Monday - Sharma Family                 │
│                                         │
│  Breakfast: (personal - your own)       │
│  ├── Masala Chai                        │
│  └── Aloo Paratha                       │
│                                         │
│  Lunch: (shared)                        │
│  ├── Dal Fry          [Swap] [Lock]     │
│  └── Jeera Rice                         │
│                                         │
│  Dinner: (shared)                       │
│  ├── Paneer Masala    [Swap] [Lock]     │
│  └── Butter Naan                        │
│                                         │
│  Snacks: (personal - your own)          │
│  └── Samosa                             │
│                                         │
└─────────────────────────────────────────┘
```

### New Room Tables

```kotlin
@Entity(tableName = "households")
data class HouseholdEntity(
    @PrimaryKey val id: String,
    val name: String,
    val inviteCode: String?,
    val ownerId: String,
    val isActive: Boolean
)

@Entity(tableName = "household_members")
data class HouseholdMemberEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val userId: String?,
    val name: String,
    val role: String,
    val canEditSharedPlan: Boolean,
    val isTemporary: Boolean,
    val leaveDate: String?,
    val status: String
)
```

---

## 10. Migration Strategy

### Phase 1: Backend Foundation
- Create `households` and `household_members` tables (Alembic)
- Household CRUD endpoints
- Invite code generation + join flow
- Phone number auto-link

### Phase 2: Shared Meal Plans
- Extend `meal_plans` with `household_id` and `slot_scope`
- Extend `meal_plan_items` with `scope` and `for_user_id`
- Family meal generation endpoint (constraint merging)
- Permission checks on swap/lock

### Phase 3: Android Integration
- Room v13 migration (new tables)
- Household management screens
- Home screen toggle (Family/My Plan)
- Join household flow (invite code entry)

### Phase 4: Guest & Temporal
- Temporary membership with leave_date
- Dual view for guests
- Auto-departure (cron/scheduled check)
- Constraint removal + regeneration on guest departure

---

## 11. Impact Assessment

| Area | Files Affected | Complexity |
|------|---------------|------------|
| Backend models | 2 new + 2 extended | Medium |
| Backend API | 3 new router files | Medium |
| Backend services | 2 new + 3 modified | High (meal gen merging) |
| Backend tests | ~10 new test files | High |
| Alembic migrations | 2-3 new | Low |
| Android domain | 3 new models | Low |
| Android data (Room) | 2 new entities, v13 migration | Medium |
| Android data (API) | 1 new API service section | Low |
| Android presentation | 2 new screens, 1 modified (Home) | High |
| Android tests | 5-8 new test files | High |

**Estimated total:** ~15 new backend files, ~10 new Android files, 2-3 migrations

---

## 12. Open Questions

> All questions resolved during brainstorming session (2026-03-07).

| # | Question | Options | Decision |
|---|----------|---------|----------|
| 1 | Can a user own multiple households? | Yes / No (MVP: No) | **No (MVP)** |
| 2 | Can a user be a member of multiple households simultaneously? | Yes / No (MVP: No) | **Yes: 1 active + 1 passive (read-only)** |
| 3 | What happens if the owner deletes their account? | Transfer ownership / Deactivate household | **Mandatory transfer to another app-user member. If none exist, deactivate household.** |
| 4 | Should the invite code expire? | Never / 24h / 7 days / Custom | **7 days, owner can regenerate (invalidates old code)** |
| 5 | Which slots are shared vs personal? | Configurable per household / Fixed (lunch+dinner shared, breakfast+snacks personal) | **Configurable per household. Default: lunch+dinner shared, breakfast+snacks personal. Owner can change.** |
| 6 | Should grocery list be household-scoped? | Yes (shared grocery) / No (per user) | **Yes. Shared meal ingredients visible to all; personal meal ingredients visible to that member only.** |
| 7 | Should recipe rules be household-scoped or user-scoped? | Household / User / Both | **Both. Household rules (owner manages) apply to shared meals. Personal rules (each member) apply to personal slots. Allergies always merged regardless.** |
| 8 | Max household size? | Unlimited / Cap (e.g., 10) | **Cap at 6 members (including owner). Can be raised later.** |
| 9 | Should notifications be sent when someone joins/leaves? | Yes / No | **Yes. In-app notifications for join/leave/auto-depart/ownership transfer. FCM push deferred.** |
| 10 | How does "My Plan" work for a MEMBER? | Generate independently / Empty until they create one / Copy of family plan | **"Family Plan" view: shared lunch/dinner + personal breakfast/snacks unified. "My Plan" view: fully independent solo plan. Personal slots auto-generate on join.** |

---

## 13. Current State vs Future State

```
CURRENT (Single User):
┌─────────┐     ┌──────────────┐     ┌───────────┐
│  User   │────>│ Preferences  │────>│ Meal Plan │
│         │     │ + family_    │     │ (user_id) │
│         │     │   members    │     │           │
└─────────┘     │ (metadata)   │     └───────────┘
                └──────────────┘

FUTURE (Household):
┌─────────┐     ┌──────────────┐     ┌───────────────┐
│  User   │────>│ Preferences  │     │ Personal Plan │
│         │     └──────────────┘     │ (user_id)     │
│         │                          └───────────────┘
│         │──┐
└─────────┘  │  ┌─────────────┐     ┌───────────────┐
             ├─>│ Household   │────>│ Family Plan   │
┌─────────┐  │  │  Members    │     │(household_id) │
│  User   │──┤  │ (linked     │     │               │
│ (Dad)   │  │  │  users)     │     │ SHARED slots: │
└─────────┘  │  └─────────────┘     │ lunch, dinner │
             │                      └───────────────┘
┌─────────┐  │
│  User   │──┘
│ (Guest) │
└─────────┘
```

---

## 14. Persona Brainstorm Findings

> Full analysis: [Persona-Brainstorm.md](./Persona-Brainstorm.md) (100 pain points, 10 personas, 3-pass chain of density)

### 12 Consolidated Themes

| # | Theme | Key Insight | Personas | Approved |
|---|-------|-------------|----------|----------|
| 1 | Shared Plan Visibility | Real-time sync of changes to all members; grocery list visible to all | Mom, Dad, Guest Male | YES |
| 2 | Personal Override Slots | Per-person breakfast/snacks; shared meals default to most restrictive diet with per-person alternatives | Dad, Teenager, Grandparent | YES |
| 3 | Streamlined Joining | Skip full onboarding; only name + allergies + dietary type required | Dad, College Student, Guest Male | YES |
| 4 | Flexible Membership Duration | Time-bounded guest visits; long-stay (>2 weeks) prompted to become permanent | College Student, Guest Senior, Mom | YES |
| 5 | Constraint Merging with Conflict Detection | Union of all allergies; warn if merged constraints leave <10 viable recipes | Mom, Guest Child, Guest Female | YES |
| 6 | Role-Based Permissions with Notifications | Notify before regeneration; undo swaps within 1 hour | Mom, Dad, Teenager | YES |
| 7 | Dual Context: Family + Personal | Active membership in one household + passive read-only monitoring of another | College Student, Solo, Guest Female | YES |
| 8 | Per-Person Customization | Portion sizes (small/regular/large), meal slot opt-out, nutrition tracking level | Teenager, Grandparent, Guest Child | YES |
| 9 | Accessibility Modes | Simple view for seniors/children; larger text; regional language support | Grandparent, Guest Senior, Teenager | YES |
| 10 | Contextual Dietary Profiles | Multiple profiles per context (home vs hostel); temporary restrictions with expiry | College Student, Guest Female | YES |
| 11 | Family Communication Layer | Suggest recipes, react to meals, mark cooked/skipped; AI learns from family feedback | Teenager, Guest Male, Dad | YES |
| 12 | Data Lifecycle and Safety | Mandatory ownership transfer before deletion; auto-purge guest data on departure | Mom, College Student, Guest Child | YES |

### 8 Gaps Not Raised by Any Persona

| # | Gap | Impact |
|---|-----|--------|
| 1 | Real-time sync of meal plan changes | Same-day coordination breaks without it |
| 2 | Constraint overload (merged constraints leave no recipes) | System needs fallback strategy |
| 3 | Undo/notification before regeneration | Prevents loss of locked/liked meals |
| 4 | Multi-household passive membership | Viewing home family while visiting another |
| 5 | Ownership transfer before account deletion | Data loss prevention |
| 6 | Feedback loop to AI from family signals | Generation quality improvement |
| 7 | Multi-language support (Pan-India) | Senior citizens, Tier 2-3 cities |
| 8 | Per-person serving sizes by age/appetite | Teenager vs senior portions differ |

### Impact on Design

The persona brainstorm surfaced needs that extend beyond the initial household-sharing design:

- **Themes 1-7** are directly addressed by the Household-Centric approach (Sections 4-9 of this document)
- **Theme 8 (Per-Person Customization)** requires extending `household_members` with portion/timing/slot preferences -- not in current schema
- **Theme 9 (Accessibility)** is a UI concern independent of the data model -- can be implemented separately
- **Theme 10 (Contextual Dietary Profiles)** is a significant extension -- multiple dietary profiles per user, tied to household context
- **Theme 11 (Family Communication)** requires a new notification/suggestion system -- future phase
- **Theme 12 (Data Lifecycle)** partially addressed (guest cleanup) but ownership transfer needs explicit implementation

---

## 15. Gap Analysis

> Cross-reference of Persona-Brainstorm.md (100 pain points, 12 themes) against this design document (data model, API, permissions, migration). Generated 2026-03-07.

### 15.1 Pain Points Not Fully Covered

19 pain points from the brainstorm have no concrete representation in the data model or API:

| Pain Point | Persona | Category |
|------------|---------|----------|
| Mark meal as "cooked" or "skipped" | Dad #6 | Meal status tracking — no field on meal_plan_items |
| Travel days — exclude member from headcount | Dad #7 | No "away today" mechanism without leaving household |
| Scale servings for one day (friends visiting) | Teenager #8 | No ad-hoc headcount override per meal/day |
| Medical dietary schedules (protein-rich breakfast for meds) | Grandparent #5 | No time-of-day dietary rules |
| Budget-aware recipe suggestions | College Student #3, Solo #9 | Not addressed in any theme or design section |
| Roommate coordination (non-family household) | College Student #5 | Design assumes "family" — works but naming/UX may feel wrong |
| Lightweight web/link view (no app install) | Guest Male #3 | No web view or shareable link in design |
| Per-member cuisine preference | Guest Male #6 | No cuisine_preference field on household_members |
| Guest's own groceries — don't double-count | Guest Male #7 | Grocery list doesn't account for self-supplied meals |
| "No meal needed today" for outside dining | Guest Female #7 | No skip-day or skip-meal mechanism |
| Medicine-food interactions | Guest Senior #1 | Not addressed in any theme |
| Food texture preferences (soft/easy-to-chew) | Guest Senior #2 | Not in any theme |
| Regional language support | Guest Senior #5 | Theme 9 mentions it but no i18n design |
| Batch cooking / meal prep | Solo #2 | Not addressed anywhere |
| Cooking skill level filtering | Solo #7 | Not addressed anywhere |
| Grocery delivery integration (BigBasket/Zepto) | Solo #8 | Not addressed anywhere |
| Per-member portion sizes | Teenager #4, Guest Child #4, Guest Senior #8 | Theme 8 mentions it but no schema field |
| Per-member meal timing | Grandparent #4, Guest Senior #3 | Theme 8 mentions it but no schema field |
| Different meal timing per member | Grandparent #4 | No field in design |

### 15.2 Theme Coverage in Design

| Theme | Data Model | API | Migration Plan | Gap |
|-------|:-:|:-:|:-:|-----|
| 1. Shared Plan Visibility | Yes | Yes | Yes (Phase 2) | No real-time sync mechanism specified (WebSocket? Polling? FCM?) |
| 2. Personal Override Slots | Yes | Partial | Yes (Phase 2) | No endpoint to generate personal slots specifically; "separately generated or manually managed" is ambiguous |
| 3. Streamlined Joining | Partial | Yes | Yes (Phase 1) | No lightweight onboarding flow defined — what screens does a joining member see? |
| 4. Flexible Membership | Yes | Partial | Yes (Phase 4) | No auto-departure cron/scheduler design; "cron/scheduled check" mentioned but not specified |
| 5. Constraint Merging | Described | No endpoint | Implicit | No "constraint overload" warning endpoint or UI; the <10 viable recipes threshold has no API representation |
| 6. Permissions + Notifications | Yes | Yes | Yes (Phase 2) | No notification table, endpoint, or delivery mechanism designed despite Q9 deciding "in-app notifications" |
| 7. Dual Context | Partial | No | Phase 4 (partial) | Schema has `active_household_id` but no `passive_household_id` — cannot implement "1 active + 1 passive" |
| 8. Per-Person Customization | **No** | **No** | **No** | Entirely missing from schema (no portion_size, meal_timing, active_slots fields) |
| 9. Accessibility Modes | **No** | **No** | **No** | Acknowledged as UI concern — out of scope for data model |
| 10. Contextual Dietary Profiles | **No** | **No** | **No** | Acknowledged as "significant extension" — deferred |
| 11. Family Communication Layer | **No** | **No** | **No** | Acknowledged as "future phase" — deferred |
| 12. Data Lifecycle | Partial | Partial | Phase 4 (partial) | Ownership transfer decided in Q3 but no API endpoint designed |

### 15.3 Internal Inconsistencies

| # | Inconsistency | Locations | Resolution Needed |
|---|---------------|-----------|-------------------|
| 1 | Q5 says "configurable per household" but `households` table has no `slot_config` field | Q5 vs Section 4 | Add `slot_config` JSON field to `households` |
| 2 | Q7 says "both household + personal rules" but `recipe_rules` has no `household_id` or `scope` field | Q7 vs Section 4 | Add `household_id` + `scope` to `recipe_rules` |
| 3 | Q6 says "shared grocery visible to all" but no `household_id` on grocery endpoints or data | Q6 vs Section 8 | Extend grocery API with household scope |
| 4 | Q10 says "personal slots auto-generate on join" but no API trigger for this | Q10 vs Section 8 | Add auto-generation logic to join flow |
| 5 | Invite code is "8-char" in schema but "ABC123" (6-char) in join flow example | Section 4 vs Section 6 | Standardize to 8-char everywhere |
| 6 | `UNIQUE(household_id, user_id)` but `user_id` is nullable — standard unique won't work | Section 4 | Use partial unique index (WHERE user_id IS NOT NULL) |
| 7 | Q2 decided "1 active + 1 passive" but no `passive_household_id` field exists in schema | Q2 vs Section 4 | Add `passive_household_id` to `users` table |

### 15.4 Persona Coverage

| Persona | Addressed | Partial | Not Addressed | Coverage |
|---------|:-:|:-:|:-:|:-:|
| Mom (Owner) | 6/10 | 2/10 | 2/10 | 80% |
| Dad (Member) | 5/10 | 2/10 | 3/10 | 70% |
| Teenager | 3/10 | 3/10 | 4/10 | 60% |
| Grandparent | 2/10 | 3/10 | 5/10 | 50% |
| College Student | 4/10 | 3/10 | 3/10 | 70% |
| Guest Male | 4/10 | 2/10 | 4/10 | 60% |
| Guest Female | 4/10 | 3/10 | 3/10 | 70% |
| Guest Child | 3/10 | 3/10 | 4/10 | 60% |
| Guest Senior | 2/10 | 2/10 | 6/10 | 40% |
| Solo Professional | 3/10 | 2/10 | 5/10 | 50% |

**Least served:** Guest Senior (40%) and Solo Professional (50%) — needs around health/medical integrations, budget, cooking skill, and grocery delivery are all out of scope.

### 15.5 Open Question vs Theme Conflicts

| Question | Decision | Conflict |
|----------|----------|----------|
| Q2: Multi-household | 1 active + 1 passive | Theme 7 requires passive monitoring, but **no `passive_household_id`** in schema |
| Q5: Slot configurability | Configurable per household | **No configuration storage** in `households` table |
| Q7: Recipe rules scope | Both household + personal | **No `household_id`** on `recipe_rules` table |
| Q8: Cap at 6 | Hard cap | With 6 members' merged constraints (Theme 5), hitting <10 viable recipes is likely. **No feasibility analysis done** |

### 15.6 Recommendations

**Must-fix before implementation planning:**

1. Add `slot_config JSONB` field to `households` table (for Q5 configurability)
2. Add `passive_household_id` to `users` table (for Q2 dual context)
3. Add `household_id` + `scope` to `recipe_rules` table (for Q7 both scopes)
4. Add `POST /households/{id}/transfer-ownership` endpoint (for Q3 decision)
5. Fix invite code length — standardize to 8-char in all examples
6. Use partial unique index for `UNIQUE(household_id, user_id) WHERE user_id IS NOT NULL`
7. Design notification table: `notifications(id, user_id, household_id, type, message, is_read, created_at)`

**Should address in design (not necessarily MVP):**

8. Add per-member fields to `household_members`: `portion_size`, `active_meal_slots`, `meal_timing` (Theme 8)
9. Define "skip meal" / "away today" mechanism for travel days (Dad #7, Guest Female #7)
10. Specify real-time sync strategy (WebSocket vs polling vs FCM)
11. Define auto-departure scheduler (cron job, background worker, or on-request check)
12. Add `meal_status` field to `meal_plan_items` (cooked/skipped/ordered_out) for Theme 11

**Acceptable to defer:**

- Themes 9-11 (accessibility, contextual profiles, communication layer)
- Budget/cost features (College Student #3, Solo #9)
- Grocery delivery integration (Solo #8)
- Multi-language support (Guest Senior #5)
- Medicine-food interactions (Guest Senior #1)
- Batch cooking / meal prep (Solo #2)
- Food texture preferences (Guest Senior #2)
- Cooking skill level filtering (Solo #7)
- Lightweight web view for guests (Guest Male #3)

---

*This is a living document. Update as decisions are made during brainstorming and implementation.*
