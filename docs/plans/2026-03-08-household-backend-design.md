# Household Backend Implementation — Design Document

**Status:** Approved
**Date:** 2026-03-08
**Approach:** Layered Build (4 layers, each independently testable)
**Depends On:** [User-Management-Design.md](../requirements/User_Management/User-Management-Design.md), [Family-Personal-Context-Design.md](../requirements/User_Management/Family-Personal-Context-Design.md)

---

## 1. Overview

Build the backend foundation for multi-user household management in RasoiAI. This is Phase 1 of a 3-phase plan:

```
Phase 1: Backend Household APIs ← THIS DOCUMENT
Phase 2: Android integration + Family/Personal toggle
Phase 3: UI visual refresh (warm-modern design system)
```

**Zero breaking changes** to existing single-user endpoints. All new columns are nullable or defaulted. Existing tests remain untouched.

---

## 2. Implementation Strategy

4 layers, each with its own Alembic migration, service logic, endpoints, and tests:

```
Layer 1: Core Tables (households, household_members) + CRUD
    ↓
Layer 2: User Extensions (active/passive_household_id) + Membership Flows
    ↓
Layer 3: Scope Extensions (recipe_rules, meal_plan_items, meal_plans) + Scoped Queries
    ↓
Layer 4: Household Meal Generation + Notifications
```

---

## 3. Data Model

### Layer 1: New Tables

**New file: `app/models/household.py`**

```
households
├── id (UUID, PK)
├── name (String 100)
├── invite_code (String 8, UNIQUE, INDEXED, nullable)
├── invite_code_expires_at (DateTime, nullable)
├── owner_id (FK → users, NOT NULL)
├── slot_config (JSON, default={"shared":["lunch","dinner"],"personal":["breakfast","snacks"]})
├── max_members (Integer, default=6)
├── is_active (Boolean, default=True)
├── created_at (DateTime)
└── updated_at (DateTime)

household_members
├── id (UUID, PK)
├── household_id (FK → households, NOT NULL)
├── user_id (FK → users, nullable)
├── family_member_id (FK → family_members, nullable)
├── role (String 20)  — OWNER, MEMBER, GUEST
├── can_edit_shared_plan (Boolean, default=False)
├── is_temporary (Boolean, default=False)
├── join_date (Date)
├── leave_date (Date, nullable)
├── previous_household_id (UUID, nullable)
├── portion_size (String 20, default='REGULAR')
├── active_meal_slots (JSON, nullable)
├── status (String 20, default='ACTIVE')  — ACTIVE, PAUSED, LEFT
├── created_at (DateTime)
└── updated_at (DateTime)

UNIQUE(household_id, user_id) WHERE user_id IS NOT NULL  — partial unique index
```

**5-location model import rule:** Both models need imports in `models/__init__.py`, `db/postgres.py` (3 blocks: init_db, create_tables, drop_tables), and `tests/conftest.py`.

### Layer 2: User Extensions

```
users (extended)
├── existing fields...
├── active_household_id (FK → households, nullable)
└── passive_household_id (FK → households, nullable)
```

### Layer 3: Scope Extensions

```
recipe_rules (extended)
├── existing fields...
├── household_id (FK → households, nullable)
└── scope (String 20, default='PERSONAL')  — PERSONAL or HOUSEHOLD

meal_plan_items (extended)
├── existing fields...
├── scope (String 20, default='FAMILY')  — FAMILY or PERSONAL
├── for_user_id (FK → users, nullable)
└── meal_status (String 20, default='PLANNED')  — PLANNED, COOKED, SKIPPED, ORDERED_OUT

meal_plans (extended)
├── existing fields...
├── household_id (FK → households, nullable)
└── slot_scope (String 20, default='ALL')  — ALL, SHARED, PERSONAL
```

### Layer 4: Notifications Extension

```
notifications (extended — model already exists)
├── existing fields (id, user_id, type, title, message, is_read, created_at)...
├── household_id (FK → households, nullable)
└── metadata (JSON, nullable)
New type values: JOIN, LEAVE, PLAN_REGENERATED, SUGGESTION, TRANSFER
```

---

## 4. API Endpoints

### Layer 1: Household CRUD (6 endpoints)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/households` | Any user | Create household, caller becomes OWNER |
| GET | `/api/v1/households/{id}` | Member | Get household details + members |
| PUT | `/api/v1/households/{id}` | Owner | Update name, slot_config, max_members |
| DELETE | `/api/v1/households/{id}` | Owner | Soft-deactivate (must transfer if members exist) |
| GET | `/api/v1/households/{id}/members` | Member | List all members with roles |
| POST | `/api/v1/households/{id}/members` | Owner | Add member by phone number |

### Layer 2: Membership Flows (6 endpoints)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/households/{id}/invite-code` | Owner | Generate/refresh 8-char invite code (7-day expiry) |
| POST | `/api/v1/households/join` | Any user | Join via invite code |
| POST | `/api/v1/households/{id}/leave` | Member/Guest | Leave household (owner must transfer first) |
| POST | `/api/v1/households/{id}/transfer-ownership` | Owner | Transfer to another linked member |
| PUT | `/api/v1/households/{id}/members/{mid}` | Owner | Update role, permissions, portion_size |
| DELETE | `/api/v1/households/{id}/members/{mid}` | Owner | Remove member (cannot remove self) |

### Layer 3: Scoped Queries (6 endpoints)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/v1/households/{id}/recipe-rules` | Member | Household-scoped rules |
| GET | `/api/v1/households/{id}/grocery` | Member | Grocery from household meal plan |
| POST | `/api/v1/households/{id}/grocery/suggest` | Member/Guest | Suggest grocery item |
| GET | `/api/v1/households/{id}/grocery/suggestions` | Owner | View pending suggestions |
| PUT | `/api/v1/households/{id}/grocery/suggestions/{sid}` | Owner | Approve/reject suggestion |
| GET | `/api/v1/households/{id}/stats/monthly` | Member | Aggregated household stats |

### Layer 4: Generation + Notifications (7 endpoints)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/households/{id}/meal-plans/generate` | Owner | Generate with merged constraints |
| GET | `/api/v1/households/{id}/meal-plans/current` | Member | Current family plan |
| POST | `/api/v1/households/{id}/meal-plans/{pid}/items/{iid}/swap` | Owner or can_edit | Swap shared meal |
| PUT | `/api/v1/households/{id}/meal-plans/{pid}/items/{iid}/lock` | Owner or can_edit | Lock shared meal |
| GET | `/api/v1/households/{id}/notifications` | Member | Household notifications |
| GET | `/api/v1/households/{id}/favorites` | Member | Aggregated favorites with attribution |
| PUT | `/api/v1/notifications/{nid}/read` | Self | Mark notification as read |

**Total: ~25 new endpoints. Existing ~44 endpoints unchanged.**

---

## 5. Service Layer

### New: `app/services/household_service.py`

```
class HouseholdService:
    create(name, user, db) → Household
        # Generate 8-char invite code, 7-day expiry
        # Create HouseholdMember(role=OWNER, can_edit=True)
        # Set user.active_household_id

    get(household_id, user, db) → Household
        # Verify membership, return with members

    update(household_id, data, user, db) → Household
        # Owner only. Update name, slot_config, max_members

    deactivate(household_id, user, db) → None
        # Owner only. Must have no other linked members (or transfer first)
        # Set is_active=False

    list_members(household_id, user, db) → List[HouseholdMember]
        # Verify membership

    add_member_by_phone(household_id, phone, user, db) → HouseholdMember
        # Owner only. Check count < max_members
        # If phone matches existing user → link user_id
        # If not → create FamilyMember (metadata-only) + HouseholdMember(user_id=None)

    refresh_invite_code(household_id, user, db) → str
        # Owner only. Invalidate old, generate new 8-char, 7-day expiry

    join(invite_code, user, db) → HouseholdMember
        # Validate: code exists, not expired, household active, count < max
        # Save previous_household_id if user has one
        # Set user.active_household_id
        # Create HouseholdMember(role=MEMBER, can_edit=False)

    leave(household_id, user, db) → None
        # Cannot leave if OWNER (must transfer first)
        # Set member status=LEFT
        # If guest: restore active_household_id = previous_household_id
        # Clear user.active_household_id

    transfer(household_id, new_owner_id, user, db) → None
        # Owner only. Target must be ACTIVE member with linked user_id
        # Old owner → role=MEMBER. New owner → role=OWNER
        # Update households.owner_id

    update_member(household_id, member_id, data, user, db) → HouseholdMember
        # Owner only. Update can_edit, portion_size, is_temporary, leave_date

    remove_member(household_id, member_id, user, db) → None
        # Owner only. Cannot remove self. Set status=LEFT
```

### Modified: `app/services/ai_meal_service.py`

New method alongside existing `generate_meal_plan()`:

```
generate_household_meal_plan(household_id, user, db) → GeneratedMealPlan
    # Owner only (checked by endpoint)
    # Load:
    #   Owner's base preferences (diet, cuisine, spice, cooking time)
    #   All ACTIVE members' allergies (UNION — safety critical)
    #   All ACTIVE members' dislikes (UNION)
    #   Household recipe rules (scope=HOUSEHOLD)
    #   Household slot_config (which slots are shared)
    #   Festival calendar
    # Build prompt with merged constraints
    # Generate via Gemini
    # Post-process: enforce allergens from ALL members
    # Save with household_id, slot_scope='SHARED'
```

### New: Permission Dependencies in `app/api/deps.py`

```
get_household_membership(household_id, current_user, db) → HouseholdMember
    # Verify user is ACTIVE member. 403 if not.

require_household_owner(household_id, current_user, db) → HouseholdMember
    # Verify user is OWNER. 403 if not.

require_household_edit(household_id, current_user, db) → HouseholdMember
    # Verify user is OWNER or has can_edit_shared_plan=True. 403 if not.
```

---

## 6. Pydantic Schemas

**New file: `app/schemas/household.py`**

```python
# Request schemas
HouseholdCreate(name: str)
HouseholdUpdate(name: Optional[str], slot_config: Optional[dict], max_members: Optional[int])
JoinHouseholdRequest(invite_code: str)
AddMemberByPhoneRequest(phone_number: str, is_temporary: bool = False, leave_date: Optional[date] = None)
UpdateMemberRequest(can_edit_shared_plan: Optional[bool], portion_size: Optional[str],
                    is_temporary: Optional[bool], leave_date: Optional[date])
TransferOwnershipRequest(new_owner_member_id: str)
GrocerySuggestionRequest(name: str, quantity: Optional[float], unit: Optional[str])
SuggestionActionRequest(status: str)  # APPROVED or REJECTED

# Response schemas
HouseholdResponse(id, name, invite_code, owner_id, slot_config, max_members, member_count, is_active, created_at)
HouseholdMemberResponse(id, user_id, family_member_id, name, role, can_edit_shared_plan,
                         is_temporary, join_date, leave_date, portion_size, status)
HouseholdDetailResponse(household: HouseholdResponse, members: List[HouseholdMemberResponse])
InviteCodeResponse(invite_code: str, expires_at: datetime)
GrocerySuggestionResponse(id, name, quantity, unit, suggested_by, status, created_at)
FamilyFavoriteResponse(recipe_id, recipe_name, favorited_by: List[str], count: int)
```

---

## 7. Testing Strategy

| Layer | Test File | Tests | What's Verified |
|-------|-----------|:-----:|-----------------|
| 1 | `tests/test_household_crud.py` | ~20 | Create/get/update/deactivate, add member by phone, list members, max cap, duplicate prevention |
| 1 | `tests/test_household_model.py` | ~8 | Relationships, partial unique index, JSON defaults, cascades |
| 2 | `tests/test_household_membership.py` | ~25 | Invite code gen/expiry/validation, join/leave/transfer, permission checks (403), guest restore |
| 3 | `tests/test_household_scoped_queries.py` | ~20 | Household recipe rules, grocery suggest/approve, stats aggregation, scope filtering |
| 4 | `tests/test_household_meal_generation.py` | ~15 | Constraint merging (allergy union, dislike union), owner-only gen, shared slot config |
| 4 | `tests/test_household_notifications.py` | ~10 | Notification on join/leave/transfer/suggestion, household vs personal filter |

**Total: ~98 new tests across 6 files.**

### Test Fixtures (in `tests/conftest.py`)

```python
@pytest.fixture
async def household(db_session, test_user):
    """Creates a household with test_user as OWNER."""

@pytest.fixture
async def household_with_members(db_session, household):
    """Household with OWNER + 2 MEMBERs + 1 GUEST."""

@pytest.fixture
async def second_user(db_session):
    """A second authenticated user for multi-user tests."""

@pytest.fixture
async def member_client(second_user):
    """API client authenticated as the second user (MEMBER role)."""
```

---

## 8. What Does NOT Change

- No Android/Compose changes (Phase 2)
- No Room migration (Phase 2)
- No UI toggle work (Phase 2)
- No visual refresh (Phase 3)
- Existing ~44 endpoints unchanged
- Existing ~580 backend tests unchanged
- All new columns nullable or defaulted

---

## 9. Implementation Order

```
Layer 1: Core Tables (~2-3 hours)
  ├── household.py model (Household, HouseholdMember)
  ├── 5-location imports (models/__init__, db/postgres x3, tests/conftest)
  ├── Alembic migration
  ├── schemas/household.py
  ├── services/household_service.py (create, get, update, deactivate, add_member, list_members)
  ├── api/v1/endpoints/households.py (6 endpoints)
  └── tests/test_household_crud.py + test_household_model.py (~28 tests)
       │
       ▼
Layer 2: Membership Flows (~2-3 hours)
  ├── Alembic migration (active/passive_household_id on users)
  ├── Extend household_service.py (invite, join, leave, transfer, update_member, remove_member)
  ├── Add 6 endpoints to households router
  ├── api/deps.py (get_household_membership, require_household_owner, require_household_edit)
  └── tests/test_household_membership.py (~25 tests)
       │
       ▼
Layer 3: Scope Extensions (~2-3 hours)
  ├── Alembic migration (recipe_rules, meal_plan_items, meal_plans extensions)
  ├── Extend recipe_rules, grocery, stats routers
  └── tests/test_household_scoped_queries.py (~20 tests)
       │
       ▼
Layer 4: Generation + Notifications (~3-4 hours)
  ├── Alembic migration (notifications extension)
  ├── Extend ai_meal_service.py (household constraint merging)
  ├── Extend meal_plans, notifications routers
  └── tests/test_household_meal_generation.py + test_household_notifications.py (~25 tests)
```

---

*Design approved 2026-03-08. Next step: implementation plan via writing-plans skill.*
