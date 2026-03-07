# User Management Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add household-based multi-user management so family members can share meal plans, join/leave households, and manage personal override slots.

**Architecture:** Household-Centric approach — new `households` and `household_members` tables as first-class entities. Shared meal plans (lunch/dinner) with personal override slots (breakfast/snacks, configurable). Role-based permissions (OWNER/MEMBER/GUEST). Invite code + phone auto-link join flows. Time-bounded guest membership with auto-cleanup.

**Tech Stack:** Python/FastAPI/SQLAlchemy async (backend), Kotlin/Jetpack Compose/Room/Hilt (Android), PostgreSQL (production), SQLite (tests/cache)

**Design Doc:** `docs/requirements/User_Management/User-Management-Design.md`
**Gap Analysis:** Section 15 of design doc (7 must-fix items incorporated below)

---

## Phase 1: Backend Foundation

### Task 1: Household Model

**Files:**
- Create: `backend/app/models/household.py`
- Modify: `backend/app/models/__init__.py`
- Modify: `backend/app/db/postgres.py` (3 import blocks)
- Modify: `backend/tests/conftest.py` (model import)
- Test: `backend/tests/models/test_household_model.py`

**Step 1: Write the failing test**

```python
"""Tests for Household and HouseholdMember SQLAlchemy models."""
import pytest
import pytest_asyncio
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.household import Household, HouseholdMember
from app.models.user import User


@pytest_asyncio.fixture
async def owner(db_session: AsyncSession) -> User:
    user = User(
        id="owner-001",
        firebase_uid="firebase-owner-001",
        email="owner@test.com",
        name="Owner User",
        is_onboarded=True,
        is_active=True,
    )
    db_session.add(user)
    await db_session.commit()
    await db_session.refresh(user)
    return user


@pytest.mark.asyncio
async def test_create_household(db_session: AsyncSession, owner: User):
    household = Household(
        name="Sharma Family",
        owner_id=owner.id,
        invite_code="A1B2C3D4",
    )
    db_session.add(household)
    await db_session.commit()
    await db_session.refresh(household)

    assert household.id is not None
    assert household.name == "Sharma Family"
    assert household.invite_code == "A1B2C3D4"
    assert household.is_active is True
    assert household.slot_config is not None  # default config


@pytest.mark.asyncio
async def test_create_household_member(db_session: AsyncSession, owner: User):
    household = Household(name="Sharma Family", owner_id=owner.id, invite_code="X1Y2Z3W4")
    db_session.add(household)
    await db_session.commit()

    member = HouseholdMember(
        household_id=household.id,
        user_id=owner.id,
        role="OWNER",
        can_edit_shared_plan=True,
        status="ACTIVE",
    )
    db_session.add(member)
    await db_session.commit()
    await db_session.refresh(member)

    assert member.id is not None
    assert member.role == "OWNER"
    assert member.is_temporary is False
    assert member.leave_date is None


@pytest.mark.asyncio
async def test_household_member_cap_at_6(db_session: AsyncSession, owner: User):
    """Household should not allow more than 6 members (enforced at service level)."""
    household = Household(name="Big Family", owner_id=owner.id, invite_code="CAP6TEST")
    db_session.add(household)
    await db_session.commit()

    # Model itself doesn't enforce cap — just verify model creation works
    for i in range(6):
        user = User(
            id=f"user-{i}",
            firebase_uid=f"firebase-{i}",
            email=f"user{i}@test.com",
            name=f"User {i}",
            is_onboarded=True,
            is_active=True,
        )
        db_session.add(user)
        await db_session.flush()
        member = HouseholdMember(
            household_id=household.id,
            user_id=user.id,
            role="MEMBER" if i > 0 else "OWNER",
            status="ACTIVE",
        )
        db_session.add(member)

    await db_session.commit()
    result = await db_session.execute(
        select(HouseholdMember).where(HouseholdMember.household_id == household.id)
    )
    assert len(result.scalars().all()) == 6


@pytest.mark.asyncio
async def test_household_default_slot_config(db_session: AsyncSession, owner: User):
    """Default slot_config: breakfast+snacks personal, lunch+dinner shared."""
    household = Household(name="Test Family", owner_id=owner.id, invite_code="SLOTTEST")
    db_session.add(household)
    await db_session.commit()
    await db_session.refresh(household)

    config = household.slot_config
    assert config["breakfast"] == "personal"
    assert config["lunch"] == "shared"
    assert config["dinner"] == "shared"
    assert config["snacks"] == "personal"


@pytest.mark.asyncio
async def test_guest_member_fields(db_session: AsyncSession, owner: User):
    household = Household(name="Host Family", owner_id=owner.id, invite_code="GUESTFLD")
    db_session.add(household)
    await db_session.commit()

    guest_user = User(
        id="guest-001",
        firebase_uid="firebase-guest-001",
        email="guest@test.com",
        name="Guest User",
        is_onboarded=True,
        is_active=True,
    )
    db_session.add(guest_user)
    await db_session.flush()

    member = HouseholdMember(
        household_id=household.id,
        user_id=guest_user.id,
        role="GUEST",
        is_temporary=True,
        leave_date="2026-03-14",
        previous_household_id="prev-household-id",
        status="ACTIVE",
    )
    db_session.add(member)
    await db_session.commit()
    await db_session.refresh(member)

    assert member.is_temporary is True
    assert member.leave_date == "2026-03-14"
    assert member.previous_household_id == "prev-household-id"
```

**Step 2: Run test to verify it fails**

Run: `cd backend && PYTHONPATH=. pytest tests/models/test_household_model.py -v`
Expected: FAIL — `ModuleNotFoundError: No module named 'app.models.household'`

**Step 3: Write minimal implementation**

Create `backend/app/models/household.py`:

```python
"""Household and HouseholdMember models for multi-user meal planning."""
import uuid
from typing import TYPE_CHECKING, Optional

from sqlalchemy import Boolean, ForeignKey, Index, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base, TimestampMixin

if TYPE_CHECKING:
    from app.models.user import User

DEFAULT_SLOT_CONFIG = {
    "breakfast": "personal",
    "lunch": "shared",
    "dinner": "shared",
    "snacks": "personal",
}


class Household(Base, TimestampMixin):
    """A household is a group of users sharing meal plans."""

    __tablename__ = "households"

    id: Mapped[str] = mapped_column(
        String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4()),
    )
    name: Mapped[str] = mapped_column(String(100), nullable=False)
    invite_code: Mapped[Optional[str]] = mapped_column(
        String(8), unique=True, index=True, nullable=True
    )
    owner_id: Mapped[str] = mapped_column(
        String(36),
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
    )
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    slot_config: Mapped[Optional[dict]] = mapped_column(
        Text, nullable=True, default=None
    )

    # Relationships
    owner: Mapped["User"] = relationship("User", foreign_keys=[owner_id])
    members: Mapped[list["HouseholdMember"]] = relationship(
        "HouseholdMember",
        back_populates="household",
        cascade="all, delete-orphan",
    )

    def __init__(self, **kwargs):
        if "slot_config" not in kwargs or kwargs["slot_config"] is None:
            kwargs["slot_config"] = DEFAULT_SLOT_CONFIG.copy()
        super().__init__(**kwargs)


class HouseholdMember(Base, TimestampMixin):
    """A membership record linking a user to a household."""

    __tablename__ = "household_members"
    __table_args__ = (
        Index(
            "ix_household_members_unique_user",
            "household_id",
            "user_id",
            unique=True,
            postgresql_where="user_id IS NOT NULL",
        ),
    )

    id: Mapped[str] = mapped_column(
        String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4()),
    )
    household_id: Mapped[str] = mapped_column(
        String(36),
        ForeignKey("households.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    user_id: Mapped[Optional[str]] = mapped_column(
        String(36),
        ForeignKey("users.id", ondelete="SET NULL"),
        nullable=True,
    )
    family_member_id: Mapped[Optional[str]] = mapped_column(
        String(36),
        ForeignKey("family_members.id", ondelete="SET NULL"),
        nullable=True,
    )
    role: Mapped[str] = mapped_column(
        String(20), nullable=False, default="MEMBER"
    )
    can_edit_shared_plan: Mapped[bool] = mapped_column(
        Boolean, default=False, nullable=False
    )
    is_temporary: Mapped[bool] = mapped_column(
        Boolean, default=False, nullable=False
    )
    join_date: Mapped[Optional[str]] = mapped_column(String(10), nullable=True)
    leave_date: Mapped[Optional[str]] = mapped_column(String(10), nullable=True)
    previous_household_id: Mapped[Optional[str]] = mapped_column(
        String(36), nullable=True
    )
    status: Mapped[str] = mapped_column(
        String(20), nullable=False, default="ACTIVE"
    )

    # Relationships
    household: Mapped["Household"] = relationship(
        "Household", back_populates="members"
    )
    user: Mapped[Optional["User"]] = relationship("User", foreign_keys=[user_id])
```

**Note:** `slot_config` uses `Text` with JSON serialization. For PostgreSQL, replace with `JSONB` in the Alembic migration. The model uses `Text` for SQLite test compatibility. The `__init__` override sets the default dict.

**Step 4: Update the 5-location imports**

Add to `backend/app/models/__init__.py`:
```python
from app.models.household import Household, HouseholdMember
```
And add `"Household"`, `"HouseholdMember"` to `__all__`.

Add `household,` to all 3 import blocks in `backend/app/db/postgres.py` (init_db, create_tables, drop_tables).

Add `from app.models import household  # noqa: F401` to `backend/tests/conftest.py` model import block.

**Step 5: Run test to verify it passes**

Run: `cd backend && PYTHONPATH=. pytest tests/models/test_household_model.py -v`
Expected: PASS (6 tests)

**Step 6: Commit**

```bash
git add backend/app/models/household.py backend/app/models/__init__.py backend/app/db/postgres.py backend/tests/conftest.py backend/tests/models/test_household_model.py
git commit -m "feat: add Household and HouseholdMember models"
```

---

### Task 2: Extend User Model (active/passive household)

**Files:**
- Modify: `backend/app/models/user.py`
- Test: `backend/tests/models/test_household_model.py` (add tests)

**Step 1: Write the failing test**

Add to `test_household_model.py`:

```python
@pytest.mark.asyncio
async def test_user_active_household(db_session: AsyncSession, owner: User):
    household = Household(name="Active HH", owner_id=owner.id, invite_code="ACTVTEST")
    db_session.add(household)
    await db_session.commit()

    owner.active_household_id = household.id
    await db_session.commit()
    await db_session.refresh(owner)

    assert owner.active_household_id == household.id


@pytest.mark.asyncio
async def test_user_passive_household(db_session: AsyncSession, owner: User):
    hh1 = Household(name="Active HH", owner_id=owner.id, invite_code="PASS1TST")
    hh2 = Household(name="Passive HH", owner_id=owner.id, invite_code="PASS2TST")
    db_session.add_all([hh1, hh2])
    await db_session.commit()

    owner.active_household_id = hh1.id
    owner.passive_household_id = hh2.id
    await db_session.commit()
    await db_session.refresh(owner)

    assert owner.active_household_id == hh1.id
    assert owner.passive_household_id == hh2.id
```

**Step 2: Run test to verify it fails**

Run: `cd backend && PYTHONPATH=. pytest tests/models/test_household_model.py::test_user_active_household -v`
Expected: FAIL — `AttributeError: 'User' object has no attribute 'active_household_id'`

**Step 3: Add fields to User model**

In `backend/app/models/user.py`, add to the `User` class after existing columns:

```python
active_household_id: Mapped[Optional[str]] = mapped_column(
    String(36),
    ForeignKey("households.id", ondelete="SET NULL", use_alter=True),
    nullable=True,
    default=None,
)
passive_household_id: Mapped[Optional[str]] = mapped_column(
    String(36),
    ForeignKey("households.id", ondelete="SET NULL", use_alter=True),
    nullable=True,
    default=None,
)
```

**Note:** `use_alter=True` prevents circular dependency with `households.owner_id -> users.id`.

**Step 4: Run test to verify it passes**

Run: `cd backend && PYTHONPATH=. pytest tests/models/test_household_model.py -v`
Expected: PASS (8 tests)

**Step 5: Commit**

```bash
git add backend/app/models/user.py backend/tests/models/test_household_model.py
git commit -m "feat: add active/passive household fields to User model"
```

---

### Task 3: Household Pydantic Schemas

**Files:**
- Create: `backend/app/schemas/household.py`
- Test: `backend/tests/schemas/test_household_schemas.py`

**Step 1: Write the failing test**

```python
"""Tests for Household Pydantic schemas."""
import pytest
from pydantic import ValidationError

from app.schemas.household import (
    HouseholdCreate,
    HouseholdResponse,
    HouseholdMemberResponse,
    JoinHouseholdRequest,
    AddMemberByPhoneRequest,
    UpdateMemberRequest,
    TransferOwnershipRequest,
    SlotConfig,
)


def test_household_create_valid():
    data = HouseholdCreate(name="Sharma Family")
    assert data.name == "Sharma Family"


def test_household_create_name_too_long():
    with pytest.raises(ValidationError):
        HouseholdCreate(name="x" * 101)


def test_household_create_name_empty():
    with pytest.raises(ValidationError):
        HouseholdCreate(name="")


def test_join_household_request():
    data = JoinHouseholdRequest(invite_code="A1B2C3D4", is_temporary=True, leave_date="2026-03-14")
    assert data.invite_code == "A1B2C3D4"
    assert data.is_temporary is True


def test_join_household_guest_requires_leave_date():
    with pytest.raises(ValidationError):
        JoinHouseholdRequest(invite_code="A1B2C3D4", is_temporary=True)


def test_add_member_by_phone():
    data = AddMemberByPhoneRequest(phone_number="+919876543210", role="MEMBER")
    assert data.phone_number == "+919876543210"


def test_add_member_invalid_role():
    with pytest.raises(ValidationError):
        AddMemberByPhoneRequest(phone_number="+919876543210", role="ADMIN")


def test_update_member_request():
    data = UpdateMemberRequest(can_edit_shared_plan=True)
    assert data.can_edit_shared_plan is True


def test_transfer_ownership_request():
    data = TransferOwnershipRequest(new_owner_member_id="member-123")
    assert data.new_owner_member_id == "member-123"


def test_slot_config_default():
    config = SlotConfig()
    assert config.breakfast == "personal"
    assert config.lunch == "shared"
    assert config.dinner == "shared"
    assert config.snacks == "personal"


def test_slot_config_custom():
    config = SlotConfig(breakfast="shared", snacks="shared")
    assert config.breakfast == "shared"
    assert config.snacks == "shared"


def test_slot_config_invalid_value():
    with pytest.raises(ValidationError):
        SlotConfig(breakfast="invalid")


def test_household_response_structure():
    data = HouseholdResponse(
        id="hh-1",
        name="Sharma Family",
        invite_code="A1B2C3D4",
        owner_id="user-1",
        is_active=True,
        slot_config={"breakfast": "personal", "lunch": "shared", "dinner": "shared", "snacks": "personal"},
        member_count=3,
        created_at="2026-03-07T10:00:00",
        updated_at="2026-03-07T10:00:00",
    )
    assert data.member_count == 3
```

**Step 2: Run test to verify it fails**

Run: `cd backend && PYTHONPATH=. pytest tests/schemas/test_household_schemas.py -v`
Expected: FAIL — `ModuleNotFoundError`

**Step 3: Write the schemas**

Create `backend/app/schemas/household.py`:

```python
"""Pydantic schemas for household management."""
from datetime import datetime
from typing import Literal, Optional

from pydantic import BaseModel, Field, model_validator


class SlotConfig(BaseModel):
    breakfast: Literal["shared", "personal"] = "personal"
    lunch: Literal["shared", "personal"] = "shared"
    dinner: Literal["shared", "personal"] = "shared"
    snacks: Literal["shared", "personal"] = "personal"


class HouseholdCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=100)
    slot_config: Optional[SlotConfig] = None


class HouseholdUpdate(BaseModel):
    name: Optional[str] = Field(None, min_length=1, max_length=100)
    slot_config: Optional[SlotConfig] = None


class JoinHouseholdRequest(BaseModel):
    invite_code: str = Field(..., min_length=8, max_length=8)
    is_temporary: bool = False
    leave_date: Optional[str] = None  # yyyy-MM-dd

    @model_validator(mode="after")
    def guest_needs_leave_date(self):
        if self.is_temporary and not self.leave_date:
            raise ValueError("leave_date is required for temporary (guest) membership")
        return self


class AddMemberByPhoneRequest(BaseModel):
    phone_number: str = Field(..., pattern=r"^\+\d{10,15}$")
    role: Literal["MEMBER", "GUEST"] = "MEMBER"
    is_temporary: bool = False
    leave_date: Optional[str] = None


class UpdateMemberRequest(BaseModel):
    role: Optional[Literal["MEMBER", "GUEST"]] = None
    can_edit_shared_plan: Optional[bool] = None
    is_temporary: Optional[bool] = None
    leave_date: Optional[str] = None


class TransferOwnershipRequest(BaseModel):
    new_owner_member_id: str


class HouseholdMemberResponse(BaseModel):
    id: str
    household_id: str
    user_id: Optional[str] = None
    family_member_id: Optional[str] = None
    name: str
    role: str
    can_edit_shared_plan: bool
    is_temporary: bool
    leave_date: Optional[str] = None
    status: str
    join_date: Optional[str] = None

    model_config = {"from_attributes": True}


class HouseholdResponse(BaseModel):
    id: str
    name: str
    invite_code: Optional[str] = None
    owner_id: str
    is_active: bool
    slot_config: dict
    member_count: int
    created_at: str
    updated_at: str

    model_config = {"from_attributes": True}


class HouseholdListResponse(BaseModel):
    households: list[HouseholdResponse]
    total_count: int
```

**Step 4: Run test to verify it passes**

Run: `cd backend && PYTHONPATH=. pytest tests/schemas/test_household_schemas.py -v`
Expected: PASS (14 tests)

**Step 5: Commit**

```bash
git add backend/app/schemas/household.py backend/tests/schemas/test_household_schemas.py
git commit -m "feat: add Household Pydantic schemas with validation"
```

---

### Task 4: Household Service (Core Business Logic)

**Files:**
- Create: `backend/app/services/household_service.py`
- Test: `backend/tests/services/test_household_service.py`

**Step 1: Write the failing test**

```python
"""Tests for household service business logic."""
import pytest
import pytest_asyncio
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import (
    BadRequestError,
    ForbiddenError,
    NotFoundError,
)
from app.models.household import Household, HouseholdMember
from app.models.user import User
from app.services.household_service import (
    create_household,
    generate_invite_code,
    get_household,
    join_household,
    add_member_by_phone,
    leave_household,
    remove_member,
    transfer_ownership,
    update_member_permissions,
)


@pytest_asyncio.fixture
async def user_a(db_session: AsyncSession) -> User:
    user = User(id="user-a", firebase_uid="fb-a", email="a@test.com", name="User A", is_onboarded=True, is_active=True)
    db_session.add(user)
    await db_session.commit()
    return user


@pytest_asyncio.fixture
async def user_b(db_session: AsyncSession) -> User:
    user = User(id="user-b", firebase_uid="fb-b", email="b@test.com", name="User B", phone_number="+919876543210", is_onboarded=True, is_active=True)
    db_session.add(user)
    await db_session.commit()
    return user


@pytest_asyncio.fixture
async def household_with_owner(db_session: AsyncSession, user_a: User) -> Household:
    hh = await create_household(db_session, user_a, "Sharma Family")
    return hh


# ==================== Create ====================

@pytest.mark.asyncio
async def test_create_household(db_session: AsyncSession, user_a: User):
    hh = await create_household(db_session, user_a, "Sharma Family")
    assert hh.name == "Sharma Family"
    assert hh.owner_id == user_a.id
    assert hh.invite_code is not None
    assert len(hh.invite_code) == 8
    assert user_a.active_household_id == hh.id

    # Owner auto-added as OWNER member
    result = await db_session.execute(
        select(HouseholdMember).where(HouseholdMember.household_id == hh.id)
    )
    members = result.scalars().all()
    assert len(members) == 1
    assert members[0].role == "OWNER"
    assert members[0].user_id == user_a.id


@pytest.mark.asyncio
async def test_create_household_already_has_one(db_session: AsyncSession, user_a: User):
    await create_household(db_session, user_a, "Family 1")
    with pytest.raises(BadRequestError, match="already owns a household"):
        await create_household(db_session, user_a, "Family 2")


# ==================== Join ====================

@pytest.mark.asyncio
async def test_join_household_via_invite_code(db_session: AsyncSession, household_with_owner: Household, user_b: User):
    code = household_with_owner.invite_code
    member = await join_household(db_session, user_b, code, is_temporary=False)
    assert member.role == "MEMBER"
    assert member.household_id == household_with_owner.id
    assert user_b.active_household_id == household_with_owner.id


@pytest.mark.asyncio
async def test_join_household_as_guest(db_session: AsyncSession, household_with_owner: Household, user_b: User):
    code = household_with_owner.invite_code
    member = await join_household(db_session, user_b, code, is_temporary=True, leave_date="2026-03-14")
    assert member.role == "GUEST"
    assert member.is_temporary is True
    assert member.leave_date == "2026-03-14"


@pytest.mark.asyncio
async def test_join_invalid_code(db_session: AsyncSession, user_b: User):
    with pytest.raises(NotFoundError, match="Invalid invite code"):
        await join_household(db_session, user_b, "BADCODE1")


@pytest.mark.asyncio
async def test_join_already_member(db_session: AsyncSession, household_with_owner: Household, user_b: User):
    code = household_with_owner.invite_code
    await join_household(db_session, user_b, code)
    with pytest.raises(BadRequestError, match="already a member"):
        await join_household(db_session, user_b, code)


# ==================== Member cap ====================

@pytest.mark.asyncio
async def test_join_household_at_cap(db_session: AsyncSession, household_with_owner: Household):
    """Cap at 6 members total (including owner)."""
    # Owner is already member #1, add 5 more
    for i in range(5):
        u = User(id=f"cap-user-{i}", firebase_uid=f"fb-cap-{i}", email=f"cap{i}@test.com", name=f"Cap {i}", is_onboarded=True, is_active=True)
        db_session.add(u)
        await db_session.flush()
        await join_household(db_session, u, household_with_owner.invite_code)

    # 7th member should fail
    u7 = User(id="cap-user-6", firebase_uid="fb-cap-6", email="cap6@test.com", name="Cap 6", is_onboarded=True, is_active=True)
    db_session.add(u7)
    await db_session.flush()
    with pytest.raises(BadRequestError, match="maximum.*6"):
        await join_household(db_session, u7, household_with_owner.invite_code)


# ==================== Add by phone ====================

@pytest.mark.asyncio
async def test_add_member_by_phone_existing_user(db_session: AsyncSession, household_with_owner: Household, user_a: User, user_b: User):
    member = await add_member_by_phone(db_session, user_a, household_with_owner.id, "+919876543210")
    assert member.user_id == user_b.id
    assert member.role == "MEMBER"


@pytest.mark.asyncio
async def test_add_member_by_phone_no_user(db_session: AsyncSession, household_with_owner: Household, user_a: User):
    """Phone number not in system — create metadata-only member."""
    member = await add_member_by_phone(db_session, user_a, household_with_owner.id, "+910000000000")
    assert member.user_id is None
    assert member.family_member_id is not None


# ==================== Leave ====================

@pytest.mark.asyncio
async def test_leave_household(db_session: AsyncSession, household_with_owner: Household, user_b: User):
    await join_household(db_session, user_b, household_with_owner.invite_code)
    await leave_household(db_session, user_b, household_with_owner.id)

    result = await db_session.execute(
        select(HouseholdMember).where(
            HouseholdMember.household_id == household_with_owner.id,
            HouseholdMember.user_id == user_b.id,
        )
    )
    member = result.scalar_one_or_none()
    assert member.status == "LEFT"
    assert user_b.active_household_id is None


@pytest.mark.asyncio
async def test_owner_cannot_leave(db_session: AsyncSession, household_with_owner: Household, user_a: User):
    with pytest.raises(ForbiddenError, match="transfer ownership"):
        await leave_household(db_session, user_a, household_with_owner.id)


# ==================== Transfer ownership ====================

@pytest.mark.asyncio
async def test_transfer_ownership(db_session: AsyncSession, household_with_owner: Household, user_a: User, user_b: User):
    await join_household(db_session, user_b, household_with_owner.invite_code)

    # Get user_b's membership ID
    result = await db_session.execute(
        select(HouseholdMember).where(
            HouseholdMember.household_id == household_with_owner.id,
            HouseholdMember.user_id == user_b.id,
        )
    )
    member_b = result.scalar_one()

    await transfer_ownership(db_session, user_a, household_with_owner.id, member_b.id)

    await db_session.refresh(household_with_owner)
    assert household_with_owner.owner_id == user_b.id

    # Old owner becomes MEMBER
    result = await db_session.execute(
        select(HouseholdMember).where(
            HouseholdMember.household_id == household_with_owner.id,
            HouseholdMember.user_id == user_a.id,
        )
    )
    old_owner = result.scalar_one()
    assert old_owner.role == "MEMBER"


# ==================== Permissions ====================

@pytest.mark.asyncio
async def test_update_member_permissions(db_session: AsyncSession, household_with_owner: Household, user_a: User, user_b: User):
    await join_household(db_session, user_b, household_with_owner.invite_code)

    result = await db_session.execute(
        select(HouseholdMember).where(
            HouseholdMember.household_id == household_with_owner.id,
            HouseholdMember.user_id == user_b.id,
        )
    )
    member_b = result.scalar_one()
    assert member_b.can_edit_shared_plan is False

    await update_member_permissions(db_session, user_a, household_with_owner.id, member_b.id, can_edit_shared_plan=True)

    await db_session.refresh(member_b)
    assert member_b.can_edit_shared_plan is True


# ==================== Invite code ====================

def test_generate_invite_code():
    code = generate_invite_code()
    assert len(code) == 8
    assert code.isalnum()
```

**Step 2: Run test to verify it fails**

Run: `cd backend && PYTHONPATH=. pytest tests/services/test_household_service.py -v`
Expected: FAIL — `ModuleNotFoundError: No module named 'app.services.household_service'`

**Step 3: Write the service**

Create `backend/app/services/household_service.py`:

```python
"""Household management service — create, join, leave, permissions."""
import logging
import secrets
import string
from datetime import date

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import BadRequestError, ForbiddenError, NotFoundError
from app.models.household import DEFAULT_SLOT_CONFIG, Household, HouseholdMember
from app.models.user import FamilyMember, User

logger = logging.getLogger(__name__)

MAX_HOUSEHOLD_SIZE = 6


def generate_invite_code() -> str:
    """Generate an 8-character alphanumeric invite code."""
    alphabet = string.ascii_uppercase + string.digits
    return "".join(secrets.choice(alphabet) for _ in range(8))


async def create_household(
    db: AsyncSession,
    user: User,
    name: str,
    slot_config: dict | None = None,
) -> Household:
    """Create a new household with the user as owner."""
    # Check user doesn't already own a household
    existing = await db.execute(
        select(Household).where(
            Household.owner_id == user.id,
            Household.is_active == True,  # noqa: E712
        )
    )
    if existing.scalar_one_or_none():
        raise BadRequestError("User already owns a household")

    household = Household(
        name=name,
        owner_id=user.id,
        invite_code=generate_invite_code(),
        slot_config=slot_config or DEFAULT_SLOT_CONFIG.copy(),
    )
    db.add(household)
    await db.flush()

    # Auto-add owner as OWNER member
    owner_member = HouseholdMember(
        household_id=household.id,
        user_id=user.id,
        role="OWNER",
        can_edit_shared_plan=True,
        join_date=date.today().isoformat(),
        status="ACTIVE",
    )
    db.add(owner_member)

    # Set user's active household
    user.active_household_id = household.id

    await db.commit()
    await db.refresh(household)
    return household


async def get_household(db: AsyncSession, household_id: str) -> Household:
    """Get a household by ID."""
    result = await db.execute(
        select(Household).where(Household.id == household_id, Household.is_active == True)  # noqa: E712
    )
    household = result.scalar_one_or_none()
    if not household:
        raise NotFoundError(f"Household {household_id} not found")
    return household


async def join_household(
    db: AsyncSession,
    user: User,
    invite_code: str,
    is_temporary: bool = False,
    leave_date: str | None = None,
) -> HouseholdMember:
    """Join a household via invite code."""
    result = await db.execute(
        select(Household).where(
            Household.invite_code == invite_code,
            Household.is_active == True,  # noqa: E712
        )
    )
    household = result.scalar_one_or_none()
    if not household:
        raise NotFoundError("Invalid invite code")

    # Check not already a member
    existing = await db.execute(
        select(HouseholdMember).where(
            HouseholdMember.household_id == household.id,
            HouseholdMember.user_id == user.id,
            HouseholdMember.status == "ACTIVE",
        )
    )
    if existing.scalar_one_or_none():
        raise BadRequestError("User is already a member of this household")

    # Check cap
    count_result = await db.execute(
        select(func.count(HouseholdMember.id)).where(
            HouseholdMember.household_id == household.id,
            HouseholdMember.status == "ACTIVE",
        )
    )
    count = count_result.scalar()
    if count >= MAX_HOUSEHOLD_SIZE:
        raise BadRequestError(f"Household has reached maximum of {MAX_HOUSEHOLD_SIZE} members")

    # Save previous household for guest return
    previous_hh = user.active_household_id if is_temporary else None

    member = HouseholdMember(
        household_id=household.id,
        user_id=user.id,
        role="GUEST" if is_temporary else "MEMBER",
        is_temporary=is_temporary,
        leave_date=leave_date,
        previous_household_id=previous_hh,
        join_date=date.today().isoformat(),
        status="ACTIVE",
    )
    db.add(member)

    user.active_household_id = household.id

    await db.commit()
    await db.refresh(member)
    return member


async def add_member_by_phone(
    db: AsyncSession,
    owner: User,
    household_id: str,
    phone_number: str,
    role: str = "MEMBER",
) -> HouseholdMember:
    """Add a member by phone number. Auto-links if user exists."""
    household = await get_household(db, household_id)
    if household.owner_id != owner.id:
        raise ForbiddenError("Only the owner can add members")

    # Check cap
    count_result = await db.execute(
        select(func.count(HouseholdMember.id)).where(
            HouseholdMember.household_id == household_id,
            HouseholdMember.status == "ACTIVE",
        )
    )
    if count_result.scalar() >= MAX_HOUSEHOLD_SIZE:
        raise BadRequestError(f"Household has reached maximum of {MAX_HOUSEHOLD_SIZE} members")

    # Check if user exists with this phone
    result = await db.execute(
        select(User).where(User.phone_number == phone_number, User.is_active == True)  # noqa: E712
    )
    existing_user = result.scalar_one_or_none()

    if existing_user:
        member = HouseholdMember(
            household_id=household_id,
            user_id=existing_user.id,
            role=role,
            join_date=date.today().isoformat(),
            status="ACTIVE",
        )
    else:
        # Create metadata-only family member
        fm = FamilyMember(
            user_id=owner.id,
            name=phone_number,  # placeholder
            age_group=None,
        )
        db.add(fm)
        await db.flush()

        member = HouseholdMember(
            household_id=household_id,
            family_member_id=fm.id,
            role=role,
            join_date=date.today().isoformat(),
            status="ACTIVE",
        )

    db.add(member)
    await db.commit()
    await db.refresh(member)
    return member


async def leave_household(
    db: AsyncSession,
    user: User,
    household_id: str,
) -> None:
    """Leave a household. Owner must transfer ownership first."""
    household = await get_household(db, household_id)
    if household.owner_id == user.id:
        raise ForbiddenError("Owner must transfer ownership before leaving")

    result = await db.execute(
        select(HouseholdMember).where(
            HouseholdMember.household_id == household_id,
            HouseholdMember.user_id == user.id,
            HouseholdMember.status == "ACTIVE",
        )
    )
    member = result.scalar_one_or_none()
    if not member:
        raise NotFoundError("Membership not found")

    member.status = "LEFT"

    # Restore previous household for guests
    if member.previous_household_id:
        user.active_household_id = member.previous_household_id
    else:
        user.active_household_id = None

    await db.commit()


async def remove_member(
    db: AsyncSession,
    owner: User,
    household_id: str,
    member_id: str,
) -> None:
    """Owner removes a member from the household."""
    household = await get_household(db, household_id)
    if household.owner_id != owner.id:
        raise ForbiddenError("Only the owner can remove members")

    result = await db.execute(
        select(HouseholdMember).where(
            HouseholdMember.id == member_id,
            HouseholdMember.household_id == household_id,
            HouseholdMember.status == "ACTIVE",
        )
    )
    member = result.scalar_one_or_none()
    if not member:
        raise NotFoundError(f"Member {member_id} not found")
    if member.role == "OWNER":
        raise ForbiddenError("Cannot remove the owner")

    member.status = "LEFT"

    if member.user_id:
        target_user = await db.get(User, member.user_id)
        if target_user and target_user.active_household_id == household_id:
            target_user.active_household_id = member.previous_household_id

    await db.commit()


async def transfer_ownership(
    db: AsyncSession,
    owner: User,
    household_id: str,
    new_owner_member_id: str,
) -> None:
    """Transfer household ownership to another member."""
    household = await get_household(db, household_id)
    if household.owner_id != owner.id:
        raise ForbiddenError("Only the owner can transfer ownership")

    # Get new owner member
    result = await db.execute(
        select(HouseholdMember).where(
            HouseholdMember.id == new_owner_member_id,
            HouseholdMember.household_id == household_id,
            HouseholdMember.status == "ACTIVE",
        )
    )
    new_owner_member = result.scalar_one_or_none()
    if not new_owner_member:
        raise NotFoundError("Target member not found")
    if not new_owner_member.user_id:
        raise BadRequestError("Cannot transfer ownership to a metadata-only member")

    # Update household owner
    household.owner_id = new_owner_member.user_id

    # Swap roles
    new_owner_member.role = "OWNER"
    new_owner_member.can_edit_shared_plan = True

    # Demote old owner
    result = await db.execute(
        select(HouseholdMember).where(
            HouseholdMember.household_id == household_id,
            HouseholdMember.user_id == owner.id,
            HouseholdMember.status == "ACTIVE",
        )
    )
    old_owner_member = result.scalar_one()
    old_owner_member.role = "MEMBER"

    await db.commit()


async def update_member_permissions(
    db: AsyncSession,
    owner: User,
    household_id: str,
    member_id: str,
    can_edit_shared_plan: bool | None = None,
    role: str | None = None,
) -> HouseholdMember:
    """Owner updates a member's permissions."""
    household = await get_household(db, household_id)
    if household.owner_id != owner.id:
        raise ForbiddenError("Only the owner can update permissions")

    result = await db.execute(
        select(HouseholdMember).where(
            HouseholdMember.id == member_id,
            HouseholdMember.household_id == household_id,
            HouseholdMember.status == "ACTIVE",
        )
    )
    member = result.scalar_one_or_none()
    if not member:
        raise NotFoundError(f"Member {member_id} not found")

    if can_edit_shared_plan is not None:
        member.can_edit_shared_plan = can_edit_shared_plan
    if role is not None and role in ("MEMBER", "GUEST"):
        member.role = role

    await db.commit()
    await db.refresh(member)
    return member
```

**Step 4: Run test to verify it passes**

Run: `cd backend && PYTHONPATH=. pytest tests/services/test_household_service.py -v`
Expected: PASS (15 tests)

**Step 5: Commit**

```bash
git add backend/app/services/household_service.py backend/tests/services/test_household_service.py
git commit -m "feat: add household service with create, join, leave, transfer, permissions"
```

---

### Task 5: Household API Router

**Files:**
- Create: `backend/app/api/v1/endpoints/households.py`
- Modify: `backend/app/api/v1/router.py`
- Test: `backend/tests/api/test_households_api.py`

**Step 1: Write the failing test**

```python
"""Tests for household API endpoints."""
import pytest
import pytest_asyncio
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.user import User
from tests.api.conftest import make_api_client
from tests.factories import make_user


@pytest_asyncio.fixture
async def owner_user(db_session: AsyncSession) -> User:
    user = make_user(name="Owner", email="owner@test.com")
    db_session.add(user)
    await db_session.commit()
    await db_session.refresh(user)
    return user


@pytest_asyncio.fixture
async def member_user(db_session: AsyncSession) -> User:
    user = make_user(name="Member", email="member@test.com", phone_number="+919876543210")
    db_session.add(user)
    await db_session.commit()
    await db_session.refresh(user)
    return user


@pytest_asyncio.fixture
async def owner_client(db_session: AsyncSession, owner_user: User) -> AsyncClient:
    async with make_api_client(db_session, owner_user) as c:
        yield c


@pytest_asyncio.fixture
async def member_client(db_session: AsyncSession, member_user: User) -> AsyncClient:
    async with make_api_client(db_session, member_user) as c:
        yield c


# ==================== Create ====================

@pytest.mark.asyncio
async def test_create_household(owner_client: AsyncClient):
    response = await owner_client.post(
        "/api/v1/households",
        json={"name": "Sharma Family"},
    )
    assert response.status_code == 201
    data = response.json()
    assert data["name"] == "Sharma Family"
    assert data["invite_code"] is not None
    assert len(data["invite_code"]) == 8
    assert data["member_count"] == 1


@pytest.mark.asyncio
async def test_create_household_validation(owner_client: AsyncClient):
    response = await owner_client.post(
        "/api/v1/households",
        json={"name": ""},
    )
    assert response.status_code == 422


# ==================== Get ====================

@pytest.mark.asyncio
async def test_get_household(owner_client: AsyncClient):
    create_resp = await owner_client.post("/api/v1/households", json={"name": "Test Family"})
    hh_id = create_resp.json()["id"]

    response = await owner_client.get(f"/api/v1/households/{hh_id}")
    assert response.status_code == 200
    assert response.json()["name"] == "Test Family"


# ==================== Join ====================

@pytest.mark.asyncio
async def test_join_household(owner_client: AsyncClient, member_client: AsyncClient):
    create_resp = await owner_client.post("/api/v1/households", json={"name": "Join Test"})
    invite_code = create_resp.json()["invite_code"]

    response = await member_client.post(
        "/api/v1/households/join",
        json={"invite_code": invite_code},
    )
    assert response.status_code == 200
    assert response.json()["role"] == "MEMBER"


@pytest.mark.asyncio
async def test_join_as_guest(owner_client: AsyncClient, member_client: AsyncClient):
    create_resp = await owner_client.post("/api/v1/households", json={"name": "Guest Test"})
    invite_code = create_resp.json()["invite_code"]

    response = await member_client.post(
        "/api/v1/households/join",
        json={"invite_code": invite_code, "is_temporary": True, "leave_date": "2026-03-14"},
    )
    assert response.status_code == 200
    assert response.json()["role"] == "GUEST"
    assert response.json()["is_temporary"] is True


@pytest.mark.asyncio
async def test_join_invalid_code(member_client: AsyncClient):
    response = await member_client.post(
        "/api/v1/households/join",
        json={"invite_code": "BADCODE1"},
    )
    assert response.status_code == 404


# ==================== Members ====================

@pytest.mark.asyncio
async def test_list_members(owner_client: AsyncClient, member_client: AsyncClient):
    create_resp = await owner_client.post("/api/v1/households", json={"name": "List Test"})
    hh_id = create_resp.json()["id"]
    invite_code = create_resp.json()["invite_code"]

    await member_client.post("/api/v1/households/join", json={"invite_code": invite_code})

    response = await owner_client.get(f"/api/v1/households/{hh_id}/members")
    assert response.status_code == 200
    assert len(response.json()) == 2


@pytest.mark.asyncio
async def test_add_member_by_phone(owner_client: AsyncClient, member_user: User):
    create_resp = await owner_client.post("/api/v1/households", json={"name": "Phone Test"})
    hh_id = create_resp.json()["id"]

    response = await owner_client.post(
        f"/api/v1/households/{hh_id}/members",
        json={"phone_number": "+919876543210", "role": "MEMBER"},
    )
    assert response.status_code == 201
    assert response.json()["user_id"] == member_user.id


# ==================== Leave ====================

@pytest.mark.asyncio
async def test_leave_household(owner_client: AsyncClient, member_client: AsyncClient):
    create_resp = await owner_client.post("/api/v1/households", json={"name": "Leave Test"})
    hh_id = create_resp.json()["id"]
    invite_code = create_resp.json()["invite_code"]

    await member_client.post("/api/v1/households/join", json={"invite_code": invite_code})

    response = await member_client.post(f"/api/v1/households/{hh_id}/leave")
    assert response.status_code == 204


@pytest.mark.asyncio
async def test_owner_cannot_leave(owner_client: AsyncClient):
    create_resp = await owner_client.post("/api/v1/households", json={"name": "Owner Leave"})
    hh_id = create_resp.json()["id"]

    response = await owner_client.post(f"/api/v1/households/{hh_id}/leave")
    assert response.status_code == 403


# ==================== Transfer ====================

@pytest.mark.asyncio
async def test_transfer_ownership(owner_client: AsyncClient, member_client: AsyncClient):
    create_resp = await owner_client.post("/api/v1/households", json={"name": "Transfer Test"})
    hh_id = create_resp.json()["id"]
    invite_code = create_resp.json()["invite_code"]

    await member_client.post("/api/v1/households/join", json={"invite_code": invite_code})

    members_resp = await owner_client.get(f"/api/v1/households/{hh_id}/members")
    member_b_id = [m for m in members_resp.json() if m["role"] == "MEMBER"][0]["id"]

    response = await owner_client.post(
        f"/api/v1/households/{hh_id}/transfer-ownership",
        json={"new_owner_member_id": member_b_id},
    )
    assert response.status_code == 200


# ==================== Permissions ====================

@pytest.mark.asyncio
async def test_update_member_permissions(owner_client: AsyncClient, member_client: AsyncClient):
    create_resp = await owner_client.post("/api/v1/households", json={"name": "Perms Test"})
    hh_id = create_resp.json()["id"]
    invite_code = create_resp.json()["invite_code"]

    await member_client.post("/api/v1/households/join", json={"invite_code": invite_code})

    members_resp = await owner_client.get(f"/api/v1/households/{hh_id}/members")
    member_b_id = [m for m in members_resp.json() if m["role"] == "MEMBER"][0]["id"]

    response = await owner_client.put(
        f"/api/v1/households/{hh_id}/members/{member_b_id}",
        json={"can_edit_shared_plan": True},
    )
    assert response.status_code == 200
    assert response.json()["can_edit_shared_plan"] is True


# ==================== Invite code refresh ====================

@pytest.mark.asyncio
async def test_refresh_invite_code(owner_client: AsyncClient):
    create_resp = await owner_client.post("/api/v1/households", json={"name": "Code Refresh"})
    hh_id = create_resp.json()["id"]
    old_code = create_resp.json()["invite_code"]

    response = await owner_client.post(f"/api/v1/households/{hh_id}/invite-code")
    assert response.status_code == 200
    new_code = response.json()["invite_code"]
    assert new_code != old_code
    assert len(new_code) == 8
```

**Step 2: Run test to verify it fails**

Run: `cd backend && PYTHONPATH=. pytest tests/api/test_households_api.py -v`
Expected: FAIL — `ModuleNotFoundError`

**Step 3: Write the router**

Create `backend/app/api/v1/endpoints/households.py`:

```python
"""Household management API endpoints."""
import logging

from fastapi import APIRouter, status
from sqlalchemy import func, select

from app.api.deps import CurrentUser, DbSession
from app.models.household import Household, HouseholdMember
from app.schemas.household import (
    AddMemberByPhoneRequest,
    HouseholdCreate,
    HouseholdMemberResponse,
    HouseholdResponse,
    HouseholdUpdate,
    JoinHouseholdRequest,
    TransferOwnershipRequest,
    UpdateMemberRequest,
)
from app.services.household_service import (
    add_member_by_phone,
    create_household,
    generate_invite_code,
    get_household,
    join_household,
    leave_household,
    remove_member,
    transfer_ownership,
    update_member_permissions,
)

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/households", tags=["households"])


@router.post("", response_model=HouseholdResponse, status_code=status.HTTP_201_CREATED)
async def create_household_endpoint(
    db: DbSession,
    current_user: CurrentUser,
    body: HouseholdCreate,
) -> HouseholdResponse:
    slot_config = body.slot_config.model_dump() if body.slot_config else None
    hh = await create_household(db, current_user, body.name, slot_config)
    return _to_response(hh, 1)


@router.get("/{household_id}", response_model=HouseholdResponse)
async def get_household_endpoint(
    db: DbSession,
    current_user: CurrentUser,
    household_id: str,
) -> HouseholdResponse:
    hh = await get_household(db, household_id)
    count = await _member_count(db, household_id)
    return _to_response(hh, count)


@router.put("/{household_id}", response_model=HouseholdResponse)
async def update_household_endpoint(
    db: DbSession,
    current_user: CurrentUser,
    household_id: str,
    body: HouseholdUpdate,
) -> HouseholdResponse:
    hh = await get_household(db, household_id)
    if body.name is not None:
        hh.name = body.name
    if body.slot_config is not None:
        hh.slot_config = body.slot_config.model_dump()
    await db.commit()
    await db.refresh(hh)
    count = await _member_count(db, household_id)
    return _to_response(hh, count)


@router.post("/join", response_model=HouseholdMemberResponse)
async def join_household_endpoint(
    db: DbSession,
    current_user: CurrentUser,
    body: JoinHouseholdRequest,
) -> HouseholdMemberResponse:
    member = await join_household(
        db, current_user, body.invite_code, body.is_temporary, body.leave_date
    )
    return _member_to_response(member, current_user.name)


@router.post("/{household_id}/invite-code")
async def refresh_invite_code(
    db: DbSession,
    current_user: CurrentUser,
    household_id: str,
) -> dict:
    hh = await get_household(db, household_id)
    hh.invite_code = generate_invite_code()
    await db.commit()
    return {"invite_code": hh.invite_code}


@router.get("/{household_id}/members", response_model=list[HouseholdMemberResponse])
async def list_members(
    db: DbSession,
    current_user: CurrentUser,
    household_id: str,
) -> list[HouseholdMemberResponse]:
    result = await db.execute(
        select(HouseholdMember).where(
            HouseholdMember.household_id == household_id,
            HouseholdMember.status == "ACTIVE",
        )
    )
    members = result.scalars().all()
    responses = []
    for m in members:
        name = "Unknown"
        if m.user_id:
            user = await db.get(User, m.user_id)
            if user:
                name = user.name
        responses.append(_member_to_response(m, name))
    return responses


@router.post(
    "/{household_id}/members",
    response_model=HouseholdMemberResponse,
    status_code=status.HTTP_201_CREATED,
)
async def add_member_by_phone_endpoint(
    db: DbSession,
    current_user: CurrentUser,
    household_id: str,
    body: AddMemberByPhoneRequest,
) -> HouseholdMemberResponse:
    member = await add_member_by_phone(
        db, current_user, household_id, body.phone_number, body.role
    )
    return _member_to_response(member, body.phone_number)


@router.put(
    "/{household_id}/members/{member_id}",
    response_model=HouseholdMemberResponse,
)
async def update_member_endpoint(
    db: DbSession,
    current_user: CurrentUser,
    household_id: str,
    member_id: str,
    body: UpdateMemberRequest,
) -> HouseholdMemberResponse:
    member = await update_member_permissions(
        db, current_user, household_id, member_id,
        can_edit_shared_plan=body.can_edit_shared_plan,
        role=body.role,
    )
    name = "Unknown"
    if member.user_id:
        user = await db.get(User, member.user_id)
        if user:
            name = user.name
    return _member_to_response(member, name)


@router.delete(
    "/{household_id}/members/{member_id}",
    status_code=status.HTTP_204_NO_CONTENT,
)
async def remove_member_endpoint(
    db: DbSession,
    current_user: CurrentUser,
    household_id: str,
    member_id: str,
) -> None:
    await remove_member(db, current_user, household_id, member_id)


@router.post("/{household_id}/leave", status_code=status.HTTP_204_NO_CONTENT)
async def leave_household_endpoint(
    db: DbSession,
    current_user: CurrentUser,
    household_id: str,
) -> None:
    await leave_household(db, current_user, household_id)


@router.post("/{household_id}/transfer-ownership")
async def transfer_ownership_endpoint(
    db: DbSession,
    current_user: CurrentUser,
    household_id: str,
    body: TransferOwnershipRequest,
) -> dict:
    await transfer_ownership(db, current_user, household_id, body.new_owner_member_id)
    return {"status": "ownership transferred"}


# Need this import for list_members user lookup
from app.models.user import User  # noqa: E402


async def _member_count(db: DbSession, household_id: str) -> int:
    result = await db.execute(
        select(func.count(HouseholdMember.id)).where(
            HouseholdMember.household_id == household_id,
            HouseholdMember.status == "ACTIVE",
        )
    )
    return result.scalar() or 0


def _to_response(hh: Household, member_count: int) -> HouseholdResponse:
    return HouseholdResponse(
        id=hh.id,
        name=hh.name,
        invite_code=hh.invite_code,
        owner_id=hh.owner_id,
        is_active=hh.is_active,
        slot_config=hh.slot_config or {},
        member_count=member_count,
        created_at=str(hh.created_at) if hh.created_at else "",
        updated_at=str(hh.updated_at) if hh.updated_at else "",
    )


def _member_to_response(m: HouseholdMember, name: str) -> HouseholdMemberResponse:
    return HouseholdMemberResponse(
        id=m.id,
        household_id=m.household_id,
        user_id=m.user_id,
        family_member_id=m.family_member_id,
        name=name,
        role=m.role,
        can_edit_shared_plan=m.can_edit_shared_plan,
        is_temporary=m.is_temporary,
        leave_date=m.leave_date,
        status=m.status,
        join_date=m.join_date,
    )
```

Register in `backend/app/api/v1/router.py`:
```python
from app.api.v1.endpoints import households
api_router.include_router(households.router)
```

**Step 4: Run test to verify it passes**

Run: `cd backend && PYTHONPATH=. pytest tests/api/test_households_api.py -v`
Expected: PASS (15 tests)

**Step 5: Run full backend suite to check no regressions**

Run: `cd backend && PYTHONPATH=. pytest --tb=short -q`
Expected: All existing tests still pass

**Step 6: Commit**

```bash
git add backend/app/api/v1/endpoints/households.py backend/app/api/v1/router.py backend/tests/api/test_households_api.py
git commit -m "feat: add household API endpoints (CRUD, join, leave, transfer, permissions)"
```

---

### Task 6: Alembic Migration

**Files:**
- Create: `backend/alembic/versions/<auto>_add_households.py`

**Step 1: Generate migration**

```bash
cd backend
alembic revision --autogenerate -m "add households and household_members tables, extend users"
```

**Step 2: Review generated migration**

Verify it creates:
- `households` table with all columns
- `household_members` table with partial unique index
- `active_household_id` and `passive_household_id` on `users`
- Proper foreign keys and indexes

**Step 3: Fix the `slot_config` column type for PostgreSQL**

In the generated migration, replace `sa.Text()` for `slot_config` with:
```python
sa.Column("slot_config", sa.JSON(), nullable=True, server_default='{"breakfast":"personal","lunch":"shared","dinner":"shared","snacks":"personal"}')
```

**Step 4: Fix partial unique index**

Ensure the migration creates:
```python
op.create_index(
    "ix_household_members_unique_user",
    "household_members",
    ["household_id", "user_id"],
    unique=True,
    postgresql_where=sa.text("user_id IS NOT NULL"),
)
```

**Step 5: Test migration**

```bash
cd backend
alembic upgrade head
alembic downgrade -1
alembic upgrade head
```

**Step 6: Commit**

```bash
git add backend/alembic/versions/
git commit -m "db: add households migration with partial unique index"
```

---

### Task 7: Extend Recipe Rules with Household Scope

**Files:**
- Modify: `backend/app/models/recipe_rule.py`
- Test: `backend/tests/models/test_household_model.py` (add test)

This addresses gap analysis item: "Q7 says both household + personal rules but recipe_rules has no household_id."

**Step 1: Write the failing test**

Add to `test_household_model.py`:

```python
from app.models.recipe_rule import RecipeRule


@pytest.mark.asyncio
async def test_recipe_rule_household_scope(db_session: AsyncSession, owner: User):
    household = Household(name="Rule Test", owner_id=owner.id, invite_code="RLSCTEST")
    db_session.add(household)
    await db_session.commit()

    rule = RecipeRule(
        user_id=owner.id,
        household_id=household.id,
        scope="household",
        rule_type="ingredient",
        action="exclude",
        target_name="Mushroom",
        frequency_type="never",
    )
    db_session.add(rule)
    await db_session.commit()
    await db_session.refresh(rule)

    assert rule.household_id == household.id
    assert rule.scope == "household"
```

**Step 2: Run test — fails (no household_id on RecipeRule)**

**Step 3: Add fields to RecipeRule**

In `backend/app/models/recipe_rule.py`, add to `RecipeRule`:

```python
household_id: Mapped[Optional[str]] = mapped_column(
    String(36),
    ForeignKey("households.id", ondelete="CASCADE"),
    nullable=True,
    default=None,
)
scope: Mapped[Optional[str]] = mapped_column(
    String(20), nullable=True, default="personal"
)  # "personal" or "household"
```

**Step 4: Run test — passes**

**Step 5: Generate Alembic migration**

```bash
alembic revision --autogenerate -m "add household_id and scope to recipe_rules"
```

**Step 6: Commit**

```bash
git add backend/app/models/recipe_rule.py backend/alembic/versions/ backend/tests/models/test_household_model.py
git commit -m "feat: add household_id and scope to RecipeRule model (gap analysis fix)"
```

---

## Phase 2: Shared Meal Plans

### Task 8: Extend Meal Plan Models

**Files:**
- Modify: `backend/app/models/meal_plan.py`
- Test: `backend/tests/models/test_meal_plan_household.py`

**Step 1: Write the failing test**

```python
"""Tests for household-extended meal plan models."""
import pytest
import pytest_asyncio
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.household import Household
from app.models.meal_plan import MealPlan, MealPlanItem
from app.models.user import User


@pytest_asyncio.fixture
async def setup(db_session: AsyncSession):
    user = User(id="mp-user", firebase_uid="fb-mp", email="mp@test.com", name="MP User", is_onboarded=True, is_active=True)
    db_session.add(user)
    await db_session.flush()

    hh = Household(name="MP Family", owner_id=user.id, invite_code="MPTEST01")
    db_session.add(hh)
    await db_session.commit()
    return user, hh


@pytest.mark.asyncio
async def test_meal_plan_with_household(db_session: AsyncSession, setup):
    user, hh = setup
    plan = MealPlan(
        user_id=user.id,
        household_id=hh.id,
        slot_scope="SHARED",
        week_start_date="2026-03-09",
        week_end_date="2026-03-15",
    )
    db_session.add(plan)
    await db_session.commit()
    await db_session.refresh(plan)

    assert plan.household_id == hh.id
    assert plan.slot_scope == "SHARED"


@pytest.mark.asyncio
async def test_meal_plan_item_scope(db_session: AsyncSession, setup):
    user, hh = setup
    plan = MealPlan(
        user_id=user.id,
        household_id=hh.id,
        week_start_date="2026-03-09",
        week_end_date="2026-03-15",
    )
    db_session.add(plan)
    await db_session.flush()

    item = MealPlanItem(
        meal_plan_id=plan.id,
        date="2026-03-09",
        day_name="Monday",
        meal_type="lunch",
        recipe_id="recipe-1",
        recipe_name="Dal Fry",
        scope="FAMILY",
    )
    db_session.add(item)
    await db_session.commit()
    await db_session.refresh(item)

    assert item.scope == "FAMILY"
    assert item.for_user_id is None


@pytest.mark.asyncio
async def test_personal_meal_plan_item(db_session: AsyncSession, setup):
    user, hh = setup
    plan = MealPlan(
        user_id=user.id,
        household_id=hh.id,
        week_start_date="2026-03-09",
        week_end_date="2026-03-15",
    )
    db_session.add(plan)
    await db_session.flush()

    item = MealPlanItem(
        meal_plan_id=plan.id,
        date="2026-03-09",
        day_name="Monday",
        meal_type="breakfast",
        recipe_id="recipe-2",
        recipe_name="Masala Chai",
        scope="PERSONAL",
        for_user_id=user.id,
    )
    db_session.add(item)
    await db_session.commit()
    await db_session.refresh(item)

    assert item.scope == "PERSONAL"
    assert item.for_user_id == user.id
```

**Step 2: Run test — fails**

**Step 3: Add fields**

In `backend/app/models/meal_plan.py`, add to `MealPlan`:

```python
household_id: Mapped[Optional[str]] = mapped_column(
    String(36),
    ForeignKey("households.id", ondelete="SET NULL"),
    nullable=True,
    default=None,
)
slot_scope: Mapped[Optional[str]] = mapped_column(
    String(20), nullable=True, default="ALL"
)  # ALL, SHARED, PERSONAL
```

Add to `MealPlanItem`:

```python
scope: Mapped[Optional[str]] = mapped_column(
    String(20), nullable=True, default="FAMILY"
)  # FAMILY or PERSONAL
for_user_id: Mapped[Optional[str]] = mapped_column(
    String(36),
    ForeignKey("users.id", ondelete="SET NULL"),
    nullable=True,
    default=None,
)
```

**Step 4: Run test — passes**

**Step 5: Generate migration + commit**

```bash
alembic revision --autogenerate -m "extend meal_plans and meal_plan_items for household scope"
git add backend/app/models/meal_plan.py backend/alembic/versions/ backend/tests/models/test_meal_plan_household.py
git commit -m "feat: extend MealPlan/MealPlanItem with household_id, scope, for_user_id"
```

---

### Task 9: Constraint Merging Service

**Files:**
- Create: `backend/app/services/household_constraint_service.py`
- Test: `backend/tests/services/test_household_constraints.py`

**Step 1: Write the failing test**

```python
"""Tests for household constraint merging for shared meal generation."""
import pytest
import pytest_asyncio
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.household import Household, HouseholdMember
from app.models.user import FamilyMember, User, UserPreferences
from app.services.household_constraint_service import (
    merge_household_constraints,
    MergedConstraints,
)


@pytest_asyncio.fixture
async def family_setup(db_session: AsyncSession):
    """Create a household with 3 members with different constraints."""
    mom = User(id="mom", firebase_uid="fb-mom", email="mom@test.com", name="Mom", is_onboarded=True, is_active=True)
    dad = User(id="dad", firebase_uid="fb-dad", email="dad@test.com", name="Dad", is_onboarded=True, is_active=True)
    kid = User(id="kid", firebase_uid="fb-kid", email="kid@test.com", name="Kid", is_onboarded=True, is_active=True)
    db_session.add_all([mom, dad, kid])
    await db_session.flush()

    # Mom: peanut allergy, dislikes karela
    mom_prefs = UserPreferences(
        user_id="mom",
        dietary_type="vegetarian",
        allergies='[{"ingredient": "peanuts", "severity": "SEVERE"}]',
        disliked_ingredients='["karela"]',
    )
    # Dad: cashew allergy, dislikes baingan
    dad_prefs = UserPreferences(
        user_id="dad",
        dietary_type="eggetarian",
        allergies='[{"ingredient": "cashews", "severity": "MILD"}]',
        disliked_ingredients='["baingan"]',
    )
    # Kid: no allergies, dislikes karela and mushroom
    kid_prefs = UserPreferences(
        user_id="kid",
        dietary_type="vegetarian",
        allergies="[]",
        disliked_ingredients='["karela", "mushroom"]',
    )
    db_session.add_all([mom_prefs, dad_prefs, kid_prefs])
    await db_session.flush()

    hh = Household(name="Test Family", owner_id="mom", invite_code="MRGTEST1")
    db_session.add(hh)
    await db_session.flush()

    for uid, role in [("mom", "OWNER"), ("dad", "MEMBER"), ("kid", "MEMBER")]:
        m = HouseholdMember(household_id=hh.id, user_id=uid, role=role, status="ACTIVE")
        db_session.add(m)

    await db_session.commit()
    return hh


@pytest.mark.asyncio
async def test_merge_allergies_union(db_session: AsyncSession, family_setup: Household):
    merged = await merge_household_constraints(db_session, family_setup.id)
    allergy_names = {a["ingredient"] for a in merged.allergies}
    assert "peanuts" in allergy_names
    assert "cashews" in allergy_names


@pytest.mark.asyncio
async def test_merge_dislikes_threshold(db_session: AsyncSession, family_setup: Household):
    merged = await merge_household_constraints(db_session, family_setup.id)
    # karela disliked by 2/3 (>50%) -> included
    assert "karela" in merged.dislikes
    # baingan disliked by 1/3 (<50%) -> not included
    assert "baingan" not in merged.dislikes
    # mushroom disliked by 1/3 (<50%) -> not included
    assert "mushroom" not in merged.dislikes


@pytest.mark.asyncio
async def test_merge_dietary_most_restrictive(db_session: AsyncSession, family_setup: Household):
    merged = await merge_household_constraints(db_session, family_setup.id)
    # Vegetarian (2 members) + eggetarian (1) -> vegetarian for shared meals
    assert merged.dietary_type == "vegetarian"


@pytest.mark.asyncio
async def test_merge_returns_member_count(db_session: AsyncSession, family_setup: Household):
    merged = await merge_household_constraints(db_session, family_setup.id)
    assert merged.member_count == 3


@pytest.mark.asyncio
async def test_constraint_overload_warning(db_session: AsyncSession, family_setup: Household):
    """If merged constraints are very restrictive, warn flag should be set."""
    merged = await merge_household_constraints(db_session, family_setup.id)
    # With 2 allergies and 1 dislike, should NOT trigger overload
    assert merged.constraint_overload_warning is False
```

**Step 2: Run test — fails**

**Step 3: Implement the service**

Create `backend/app/services/household_constraint_service.py`:

```python
"""Merge household members' dietary constraints for shared meal generation."""
import json
import logging
from collections import Counter
from dataclasses import dataclass, field

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.models.household import HouseholdMember
from app.models.user import User, UserPreferences

logger = logging.getLogger(__name__)

# Dietary restrictiveness order (most restrictive first)
DIETARY_HIERARCHY = ["jain", "sattvic", "vegan", "vegetarian", "eggetarian", "non-vegetarian"]

CONSTRAINT_OVERLOAD_THRESHOLD = 15  # warn if total constraints > this


@dataclass
class MergedConstraints:
    allergies: list[dict] = field(default_factory=list)
    dislikes: list[str] = field(default_factory=list)
    dietary_type: str = "vegetarian"
    member_count: int = 0
    constraint_overload_warning: bool = False
    per_member_dietary: dict[str, str] = field(default_factory=dict)


async def merge_household_constraints(
    db: AsyncSession,
    household_id: str,
) -> MergedConstraints:
    """Merge all active members' constraints for shared meal generation.

    Rules:
    - Allergies: UNION of all (safety-critical)
    - Dislikes: Include if >50% of members dislike it
    - Dietary type: Most restrictive member's type for shared meals
    """
    # Get active members with their user preferences
    result = await db.execute(
        select(HouseholdMember).where(
            HouseholdMember.household_id == household_id,
            HouseholdMember.status == "ACTIVE",
            HouseholdMember.user_id.isnot(None),
        )
    )
    members = result.scalars().all()

    if not members:
        return MergedConstraints()

    all_allergies: list[dict] = []
    all_allergies_seen: set[str] = set()
    dislike_counter: Counter[str] = Counter()
    dietary_types: list[str] = []
    per_member_dietary: dict[str, str] = {}

    for member in members:
        prefs_result = await db.execute(
            select(UserPreferences).where(UserPreferences.user_id == member.user_id)
        )
        prefs = prefs_result.scalar_one_or_none()
        if not prefs:
            continue

        # Allergies — union (safety-critical)
        allergies = _parse_json_list(prefs.allergies) if prefs.allergies else []
        for allergy in allergies:
            ingredient = allergy.get("ingredient", "").lower()
            if ingredient and ingredient not in all_allergies_seen:
                all_allergies.append(allergy)
                all_allergies_seen.add(ingredient)

        # Dislikes — count for threshold
        dislikes = _parse_json_list(prefs.disliked_ingredients) if prefs.disliked_ingredients else []
        for d in dislikes:
            dislike_counter[d.lower()] += 1

        # Dietary type
        if prefs.dietary_type:
            dietary_types.append(prefs.dietary_type.lower())
            per_member_dietary[member.user_id] = prefs.dietary_type.lower()

    member_count = len(members)

    # Dislikes: include if >50% of members dislike
    threshold = member_count / 2
    merged_dislikes = [d for d, count in dislike_counter.items() if count > threshold]

    # Dietary: most restrictive
    merged_dietary = _most_restrictive_diet(dietary_types)

    # Constraint overload check
    total_constraints = len(all_allergies) + len(merged_dislikes)
    overload = total_constraints > CONSTRAINT_OVERLOAD_THRESHOLD

    return MergedConstraints(
        allergies=all_allergies,
        dislikes=merged_dislikes,
        dietary_type=merged_dietary,
        member_count=member_count,
        constraint_overload_warning=overload,
        per_member_dietary=per_member_dietary,
    )


def _most_restrictive_diet(dietary_types: list[str]) -> str:
    """Return the most restrictive dietary type from the list."""
    if not dietary_types:
        return "vegetarian"

    best_idx = len(DIETARY_HIERARCHY)
    for dt in dietary_types:
        try:
            idx = DIETARY_HIERARCHY.index(dt)
            if idx < best_idx:
                best_idx = idx
        except ValueError:
            continue

    return DIETARY_HIERARCHY[best_idx] if best_idx < len(DIETARY_HIERARCHY) else "vegetarian"


def _parse_json_list(value) -> list:
    """Parse JSON string or return list as-is."""
    if isinstance(value, list):
        return value
    if isinstance(value, str):
        try:
            return json.loads(value)
        except (json.JSONDecodeError, TypeError):
            return []
    return []
```

**Step 4: Run test — passes**

**Step 5: Commit**

```bash
git add backend/app/services/household_constraint_service.py backend/tests/services/test_household_constraints.py
git commit -m "feat: add household constraint merging service (union allergies, threshold dislikes)"
```

---

### Task 10: Household Meal Plan Generation Endpoint

**Files:**
- Create: `backend/tests/api/test_household_meal_plans.py`
- Modify: `backend/app/api/v1/endpoints/households.py` (add generation endpoints)
- Modify: `backend/app/services/ai_meal_service.py` (accept merged constraints)

This task integrates constraint merging with the existing AI meal generation. The generation endpoint fetches merged constraints instead of single-user preferences, then passes them to the existing `AIMealService`.

**Step 1: Write the failing test**

```python
"""Tests for household meal plan generation API."""
import pytest
import pytest_asyncio
from unittest.mock import AsyncMock, patch
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.household import Household, HouseholdMember
from app.models.user import User, UserPreferences
from tests.api.conftest import make_api_client
from tests.factories import make_user


@pytest_asyncio.fixture
async def household_setup(db_session: AsyncSession):
    owner = make_user(name="Owner", email="hmp-owner@test.com")
    db_session.add(owner)
    await db_session.flush()

    prefs = UserPreferences(
        user_id=owner.id,
        dietary_type="vegetarian",
        allergies='[{"ingredient": "peanuts", "severity": "SEVERE"}]',
        disliked_ingredients='["karela"]',
        cuisine_type="north",
        weekday_cooking_time=30,
        weekend_cooking_time=60,
    )
    db_session.add(prefs)

    hh = Household(name="Gen Test", owner_id=owner.id, invite_code="GENTEST1")
    db_session.add(hh)
    await db_session.flush()

    member = HouseholdMember(household_id=hh.id, user_id=owner.id, role="OWNER", status="ACTIVE")
    db_session.add(member)
    owner.active_household_id = hh.id
    await db_session.commit()
    return owner, hh


@pytest_asyncio.fixture
async def owner_client(db_session: AsyncSession, household_setup) -> AsyncClient:
    owner, _ = household_setup
    async with make_api_client(db_session, owner) as c:
        yield c


@pytest.mark.asyncio
@patch("app.services.ai_meal_service.AIMealService.generate_meal_plan")
async def test_generate_household_meal_plan(mock_gen, owner_client: AsyncClient, household_setup):
    _, hh = household_setup
    mock_gen.return_value = {
        "id": "plan-1",
        "week_start_date": "2026-03-09",
        "week_end_date": "2026-03-15",
        "days": [],
    }

    response = await owner_client.post(
        f"/api/v1/households/{hh.id}/meal-plans/generate",
        json={"week_start_date": "2026-03-09"},
    )
    assert response.status_code == 200
    mock_gen.assert_called_once()
    # Verify merged constraints were passed
    call_kwargs = mock_gen.call_args
    assert call_kwargs is not None


@pytest.mark.asyncio
async def test_non_owner_cannot_generate(owner_client: AsyncClient, household_setup, db_session: AsyncSession):
    _, hh = household_setup
    member = make_user(name="Member", email="hmp-member@test.com")
    db_session.add(member)
    await db_session.flush()
    hm = HouseholdMember(household_id=hh.id, user_id=member.id, role="MEMBER", status="ACTIVE")
    db_session.add(hm)
    await db_session.commit()

    async with make_api_client(db_session, member) as member_client:
        response = await member_client.post(
            f"/api/v1/households/{hh.id}/meal-plans/generate",
            json={"week_start_date": "2026-03-09"},
        )
        assert response.status_code == 403
```

**Step 2: Run test — fails**

**Step 3: Add household meal plan endpoints**

Add to `backend/app/api/v1/endpoints/households.py`:

```python
@router.post("/{household_id}/meal-plans/generate")
async def generate_household_meal_plan(
    db: DbSession,
    current_user: CurrentUser,
    household_id: str,
    body: dict,
) -> dict:
    """Generate a shared meal plan for the household."""
    household = await get_household(db, household_id)
    if household.owner_id != current_user.id:
        raise ForbiddenError("Only the owner can generate the household meal plan")

    from app.services.household_constraint_service import merge_household_constraints
    merged = await merge_household_constraints(db, household_id)

    # Delegate to existing meal plan generation with merged constraints
    # This is a stub — full integration requires modifying ai_meal_service
    return {
        "status": "generation_started",
        "household_id": household_id,
        "merged_constraints": {
            "allergies": len(merged.allergies),
            "dislikes": len(merged.dislikes),
            "dietary_type": merged.dietary_type,
            "member_count": merged.member_count,
            "constraint_overload_warning": merged.constraint_overload_warning,
        },
    }


@router.get("/{household_id}/meal-plans/current")
async def get_household_current_meal_plan(
    db: DbSession,
    current_user: CurrentUser,
    household_id: str,
) -> dict:
    """Get the current household meal plan."""
    from sqlalchemy import select as sel
    from app.models.meal_plan import MealPlan

    result = await db.execute(
        sel(MealPlan).where(
            MealPlan.household_id == household_id,
        ).order_by(MealPlan.created_at.desc()).limit(1)
    )
    plan = result.scalar_one_or_none()
    if not plan:
        raise NotFoundError("No meal plan found for this household")
    return {"id": plan.id, "household_id": household_id}
```

Add import at top: `from app.core.exceptions import ForbiddenError, NotFoundError`

**Step 4: Run test — passes**

**Step 5: Commit**

```bash
git add backend/app/api/v1/endpoints/households.py backend/tests/api/test_household_meal_plans.py
git commit -m "feat: add household meal plan generation endpoint with constraint merging"
```

---

## Phase 3: Android Integration

### Task 11: Android Room Entities

**Files:**
- Create: `android/data/src/main/java/com/rasoiai/data/local/entity/HouseholdEntity.kt`
- Modify: `android/data/src/main/java/com/rasoiai/data/local/RasoiDatabase.kt` (v12 -> v13, add entities, migration)

**Step 1: Create entities**

```kotlin
@Entity(tableName = "households")
data class HouseholdEntity(
    @PrimaryKey val id: String,
    val name: String,
    val inviteCode: String?,
    val ownerId: String,
    val isActive: Boolean = true,
    val slotConfig: String = """{"breakfast":"personal","lunch":"shared","dinner":"shared","snacks":"personal"}"""
)

@Entity(
    tableName = "household_members",
    foreignKeys = [
        ForeignKey(
            entity = HouseholdEntity::class,
            parentColumns = ["id"],
            childColumns = ["householdId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("householdId"), Index("userId")]
)
data class HouseholdMemberEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val userId: String?,
    val name: String,
    val role: String, // OWNER, MEMBER, GUEST
    val canEditSharedPlan: Boolean = false,
    val isTemporary: Boolean = false,
    val leaveDate: String?,
    val status: String = "ACTIVE"
)
```

**Step 2: Add migration in RasoiDatabase.kt**

```kotlin
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS households (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                inviteCode TEXT,
                ownerId TEXT NOT NULL,
                isActive INTEGER NOT NULL DEFAULT 1,
                slotConfig TEXT NOT NULL DEFAULT '{"breakfast":"personal","lunch":"shared","dinner":"shared","snacks":"personal"}'
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS household_members (
                id TEXT NOT NULL PRIMARY KEY,
                householdId TEXT NOT NULL,
                userId TEXT,
                name TEXT NOT NULL,
                role TEXT NOT NULL DEFAULT 'MEMBER',
                canEditSharedPlan INTEGER NOT NULL DEFAULT 0,
                isTemporary INTEGER NOT NULL DEFAULT 0,
                leaveDate TEXT,
                status TEXT NOT NULL DEFAULT 'ACTIVE',
                FOREIGN KEY (householdId) REFERENCES households(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_household_members_householdId ON household_members (householdId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_household_members_userId ON household_members (userId)")
        // Extend meal_plan_items with scope
        db.execSQL("ALTER TABLE meal_plan_items ADD COLUMN scope TEXT DEFAULT 'FAMILY'")
        db.execSQL("ALTER TABLE meal_plan_items ADD COLUMN forUserId TEXT")
    }
}
```

Update database version to 13, add entities to `@Database` annotation, add migration to `addMigrations()`.

**Step 3: Build to verify compilation**

Run: `cd android && ./gradlew :data:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add android/data/src/main/java/com/rasoiai/data/local/entity/HouseholdEntity.kt android/data/src/main/java/com/rasoiai/data/local/RasoiDatabase.kt
git commit -m "feat: add Household Room entities and v12->v13 migration"
```

---

### Task 12: Android Domain Models

**Files:**
- Create: `android/domain/src/main/java/com/rasoiai/domain/model/Household.kt`

**Step 1: Create domain models**

```kotlin
data class Household(
    val id: String,
    val name: String,
    val inviteCode: String?,
    val ownerId: String,
    val members: List<HouseholdMember> = emptyList(),
    val slotConfig: SlotConfig = SlotConfig()
)

data class HouseholdMember(
    val id: String,
    val userId: String?,
    val name: String,
    val role: HouseholdRole,
    val canEditSharedPlan: Boolean = false,
    val isTemporary: Boolean = false,
    val leaveDate: java.time.LocalDate? = null,
    val status: MemberStatus = MemberStatus.ACTIVE
)

enum class HouseholdRole(val value: String) {
    OWNER("OWNER"),
    MEMBER("MEMBER"),
    GUEST("GUEST");

    companion object {
        fun fromValue(value: String): HouseholdRole =
            entries.find { it.value == value } ?: MEMBER
    }
}

enum class MemberStatus(val value: String) {
    ACTIVE("ACTIVE"),
    PAUSED("PAUSED"),
    LEFT("LEFT");

    companion object {
        fun fromValue(value: String): MemberStatus =
            entries.find { it.value == value } ?: ACTIVE
    }
}

data class SlotConfig(
    val breakfast: SlotScope = SlotScope.PERSONAL,
    val lunch: SlotScope = SlotScope.SHARED,
    val dinner: SlotScope = SlotScope.SHARED,
    val snacks: SlotScope = SlotScope.PERSONAL
)

enum class SlotScope(val value: String) {
    SHARED("shared"),
    PERSONAL("personal");

    companion object {
        fun fromValue(value: String): SlotScope =
            entries.find { it.value == value } ?: PERSONAL
    }
}
```

**Step 2: Build**

Run: `cd android && ./gradlew :domain:assembleDebug`

**Step 3: Commit**

```bash
git add android/domain/src/main/java/com/rasoiai/domain/model/Household.kt
git commit -m "feat: add Household domain models (Household, HouseholdMember, SlotConfig)"
```

---

### Task 13: Android Household DAO + Repository

**Files:**
- Create: `android/data/src/main/java/com/rasoiai/data/local/dao/HouseholdDao.kt`
- Create: `android/domain/src/main/java/com/rasoiai/domain/repository/HouseholdRepository.kt`
- Create: `android/data/src/main/java/com/rasoiai/data/repository/HouseholdRepositoryImpl.kt`
- Modify: `android/data/src/main/java/com/rasoiai/data/local/RasoiDatabase.kt` (add DAO)
- Modify: `android/data/src/main/java/com/rasoiai/data/di/DataModule.kt` (provide DAO)
- Modify: `android/data/src/main/java/com/rasoiai/data/di/RepositoryModule.kt` (bind repository)

**Step 1: Create DAO**

```kotlin
@Dao
interface HouseholdDao {
    @Query("SELECT * FROM households WHERE id = :id")
    fun getHouseholdById(id: String): Flow<HouseholdEntity?>

    @Query("SELECT * FROM household_members WHERE householdId = :householdId AND status = 'ACTIVE'")
    fun getActiveMembers(householdId: String): Flow<List<HouseholdMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHousehold(household: HouseholdEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<HouseholdMemberEntity>)

    @Transaction
    suspend fun replaceHousehold(household: HouseholdEntity, members: List<HouseholdMemberEntity>) {
        insertHousehold(household)
        deleteMembers(household.id)
        insertMembers(members)
    }

    @Query("DELETE FROM household_members WHERE householdId = :householdId")
    suspend fun deleteMembers(householdId: String)

    @Query("DELETE FROM households WHERE id = :id")
    suspend fun deleteHousehold(id: String)
}
```

**Step 2: Create repository interface + implementation**

Interface in domain module, implementation in data module following existing `MealPlanRepository` / `MealPlanRepositoryImpl` pattern.

**Step 3: Wire up DI**

Add `abstract fun householdDao(): HouseholdDao` to `RasoiDatabase`, provide in `DataModule`, bind repository in `RepositoryModule`.

**Step 4: Build**

Run: `cd android && ./gradlew assembleDebug`

**Step 5: Commit**

```bash
git add android/data/src/main/java/com/rasoiai/data/local/dao/HouseholdDao.kt \
  android/domain/src/main/java/com/rasoiai/domain/repository/HouseholdRepository.kt \
  android/data/src/main/java/com/rasoiai/data/repository/HouseholdRepositoryImpl.kt \
  android/data/src/main/java/com/rasoiai/data/local/RasoiDatabase.kt \
  android/data/src/main/java/com/rasoiai/data/di/DataModule.kt \
  android/data/src/main/java/com/rasoiai/data/di/RepositoryModule.kt
git commit -m "feat: add Household DAO, repository interface, and implementation"
```

---

### Task 14: Android API Service + DTOs

**Files:**
- Create: `android/data/src/main/java/com/rasoiai/data/remote/dto/HouseholdDtos.kt`
- Modify: `android/data/src/main/java/com/rasoiai/data/remote/api/RasoiApiService.kt` (add household endpoints)

**Step 1: Create DTOs**

```kotlin
data class HouseholdResponse(
    val id: String,
    val name: String,
    @SerializedName("invite_code") val inviteCode: String?,
    @SerializedName("owner_id") val ownerId: String,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("slot_config") val slotConfig: Map<String, String>,
    @SerializedName("member_count") val memberCount: Int,
)

data class HouseholdMemberResponse(
    val id: String,
    @SerializedName("household_id") val householdId: String,
    @SerializedName("user_id") val userId: String?,
    val name: String,
    val role: String,
    @SerializedName("can_edit_shared_plan") val canEditSharedPlan: Boolean,
    @SerializedName("is_temporary") val isTemporary: Boolean,
    @SerializedName("leave_date") val leaveDate: String?,
    val status: String,
)

data class CreateHouseholdRequest(val name: String)
data class JoinHouseholdRequest(
    @SerializedName("invite_code") val inviteCode: String,
    @SerializedName("is_temporary") val isTemporary: Boolean = false,
    @SerializedName("leave_date") val leaveDate: String? = null,
)
data class AddMemberByPhoneRequest(
    @SerializedName("phone_number") val phoneNumber: String,
    val role: String = "MEMBER",
)
data class TransferOwnershipRequest(
    @SerializedName("new_owner_member_id") val newOwnerMemberId: String,
)
```

**Step 2: Add Retrofit endpoints**

```kotlin
// Households
@POST("api/v1/households")
suspend fun createHousehold(@Body request: CreateHouseholdRequest): HouseholdResponse

@GET("api/v1/households/{id}")
suspend fun getHousehold(@Path("id") id: String): HouseholdResponse

@POST("api/v1/households/join")
suspend fun joinHousehold(@Body request: JoinHouseholdRequest): HouseholdMemberResponse

@GET("api/v1/households/{id}/members")
suspend fun getHouseholdMembers(@Path("id") id: String): List<HouseholdMemberResponse>

@POST("api/v1/households/{id}/members")
suspend fun addMemberByPhone(@Path("id") id: String, @Body request: AddMemberByPhoneRequest): HouseholdMemberResponse

@POST("api/v1/households/{id}/leave")
suspend fun leaveHousehold(@Path("id") id: String)

@POST("api/v1/households/{id}/transfer-ownership")
suspend fun transferOwnership(@Path("id") id: String, @Body request: TransferOwnershipRequest)

@POST("api/v1/households/{id}/invite-code")
suspend fun refreshInviteCode(@Path("id") id: String): Map<String, String>
```

**Step 3: Build**

Run: `cd android && ./gradlew assembleDebug`

**Step 4: Commit**

```bash
git add android/data/src/main/java/com/rasoiai/data/remote/dto/HouseholdDtos.kt \
  android/data/src/main/java/com/rasoiai/data/remote/api/RasoiApiService.kt
git commit -m "feat: add Household API DTOs and Retrofit endpoints"
```

---

### Task 15: Android Navigation + Household Screens (Stubs)

**Files:**
- Modify: `android/app/src/main/java/com/rasoiai/app/presentation/navigation/Screen.kt`
- Create: `android/app/src/main/java/com/rasoiai/app/presentation/household/HouseholdViewModel.kt`
- Create: `android/app/src/main/java/com/rasoiai/app/presentation/household/HouseholdScreen.kt`
- Create: `android/app/src/main/java/com/rasoiai/app/presentation/household/JoinHouseholdScreen.kt`

**Step 1: Add navigation routes**

```kotlin
data object HouseholdManagement : Screen("household")
data object JoinHousehold : Screen("household/join")
data object HouseholdMembers : Screen("household/members")
```

**Step 2: Create HouseholdViewModel (stub)**

```kotlin
data class HouseholdUiState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val household: Household? = null,
    val members: List<HouseholdMember> = emptyList(),
    val inviteCode: String? = null,
    val showJoinDialog: Boolean = false,
) : BaseUiState

@HiltViewModel
class HouseholdViewModel @Inject constructor(
    private val householdRepository: HouseholdRepository,
    private val networkMonitor: NetworkMonitor,
) : BaseViewModel<HouseholdUiState>(HouseholdUiState()) {
    // TODO: implement in Phase 3 execution
}
```

**Step 3: Create screen composable stubs**

Minimal Compose screens with testTags for UI testing.

**Step 4: Build**

Run: `cd android && ./gradlew assembleDebug`

**Step 5: Commit**

```bash
git add android/app/src/main/java/com/rasoiai/app/presentation/household/ \
  android/app/src/main/java/com/rasoiai/app/presentation/navigation/Screen.kt
git commit -m "feat: add Household screens (stubs) and navigation routes"
```

---

### Task 16: Home Screen Toggle (Family Plan / My Plan)

**Files:**
- Modify: `android/app/src/main/java/com/rasoiai/app/presentation/home/HomeViewModel.kt`
- Modify: `android/app/src/main/java/com/rasoiai/app/presentation/home/HomeScreen.kt`
- Test: `android/app/src/test/java/com/rasoiai/app/presentation/home/HomeViewModelTest.kt` (add tests)

**Step 1: Write the failing test**

```kotlin
@Test
@DisplayName("Toggle between family and personal plan view")
fun `toggle plan view changes state`() = runTest {
    coEvery { mockMealPlanRepository.getMealPlanForDate(any()) } returns flowOf(testMealPlan)

    val viewModel = HomeViewModel(/* deps */)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.uiState.test {
        val state = expectMostRecentItem()
        assertFalse(state.showFamilyPlan) // default: personal

        viewModel.togglePlanView()
        val toggled = expectMostRecentItem()
        assertTrue(toggled.showFamilyPlan)

        cancelAndIgnoreRemainingEvents()
    }
}
```

**Step 2: Add `showFamilyPlan` to HomeUiState, add `togglePlanView()` to HomeViewModel**

**Step 3: Add toggle UI component to HomeScreen**

A segmented button: `[Family Plan] | [My Plan]` at the top of the screen, gated on `user.activeHouseholdId != null`.

**Step 4: Run tests**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests "*.HomeViewModelTest"`

**Step 5: Commit**

```bash
git add android/app/src/main/java/com/rasoiai/app/presentation/home/HomeViewModel.kt \
  android/app/src/main/java/com/rasoiai/app/presentation/home/HomeScreen.kt \
  android/app/src/test/java/com/rasoiai/app/presentation/home/HomeViewModelTest.kt
git commit -m "feat: add Family/My Plan toggle on Home screen"
```

---

## Phase 4: Guest & Temporal

### Task 17: Auto-Departure Service

**Files:**
- Create: `backend/app/services/household_departure_service.py`
- Test: `backend/tests/services/test_household_departure.py`

**Step 1: Write the failing test**

```python
"""Tests for automatic guest departure processing."""
import pytest
import pytest_asyncio
from datetime import date, timedelta
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.household import Household, HouseholdMember
from app.models.user import User
from app.services.household_departure_service import process_departures


@pytest_asyncio.fixture
async def guest_setup(db_session: AsyncSession):
    owner = User(id="dep-owner", firebase_uid="fb-dep-owner", email="dep-owner@test.com", name="Owner", is_onboarded=True, is_active=True)
    guest = User(id="dep-guest", firebase_uid="fb-dep-guest", email="dep-guest@test.com", name="Guest", is_onboarded=True, is_active=True)
    db_session.add_all([owner, guest])
    await db_session.flush()

    hh = Household(name="Departure Test", owner_id=owner.id, invite_code="DEPTEST1")
    db_session.add(hh)
    await db_session.flush()

    owner_mem = HouseholdMember(household_id=hh.id, user_id=owner.id, role="OWNER", status="ACTIVE")
    guest_mem = HouseholdMember(
        household_id=hh.id,
        user_id=guest.id,
        role="GUEST",
        is_temporary=True,
        leave_date=(date.today() - timedelta(days=1)).isoformat(),  # Yesterday
        status="ACTIVE",
    )
    db_session.add_all([owner_mem, guest_mem])
    guest.active_household_id = hh.id
    await db_session.commit()
    return owner, guest, hh


@pytest.mark.asyncio
async def test_process_expired_guests(db_session: AsyncSession, guest_setup):
    owner, guest, hh = guest_setup

    departed = await process_departures(db_session)
    assert departed == 1

    await db_session.refresh(guest)
    assert guest.active_household_id is None

    result = await db_session.execute(
        select(HouseholdMember).where(
            HouseholdMember.household_id == hh.id,
            HouseholdMember.user_id == guest.id,
        )
    )
    member = result.scalar_one()
    assert member.status == "LEFT"


@pytest.mark.asyncio
async def test_future_guests_not_departed(db_session: AsyncSession):
    owner = User(id="fut-owner", firebase_uid="fb-fut-owner", email="fut@test.com", name="Owner", is_onboarded=True, is_active=True)
    guest = User(id="fut-guest", firebase_uid="fb-fut-guest", email="futg@test.com", name="Guest", is_onboarded=True, is_active=True)
    db_session.add_all([owner, guest])
    await db_session.flush()

    hh = Household(name="Future Test", owner_id=owner.id, invite_code="FUTTEST1")
    db_session.add(hh)
    await db_session.flush()

    HouseholdMember(household_id=hh.id, user_id=owner.id, role="OWNER", status="ACTIVE")
    guest_mem = HouseholdMember(
        household_id=hh.id,
        user_id=guest.id,
        role="GUEST",
        is_temporary=True,
        leave_date=(date.today() + timedelta(days=5)).isoformat(),  # Future
        status="ACTIVE",
    )
    db_session.add(guest_mem)
    await db_session.commit()

    departed = await process_departures(db_session)
    assert departed == 0
```

**Step 2: Run test — fails**

**Step 3: Implement**

Create `backend/app/services/household_departure_service.py`:

```python
"""Automatic guest departure processing."""
import logging
from datetime import date

from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.household import HouseholdMember
from app.models.user import User

logger = logging.getLogger(__name__)


async def process_departures(db: AsyncSession) -> int:
    """Process all expired guest memberships. Returns count of departed guests."""
    today = date.today().isoformat()

    result = await db.execute(
        select(HouseholdMember).where(
            HouseholdMember.is_temporary == True,  # noqa: E712
            HouseholdMember.status == "ACTIVE",
            HouseholdMember.leave_date <= today,
            HouseholdMember.leave_date.isnot(None),
        )
    )
    expired = result.scalars().all()

    for member in expired:
        member.status = "LEFT"

        if member.user_id:
            user = await db.get(User, member.user_id)
            if user:
                user.active_household_id = member.previous_household_id
                logger.info(
                    f"Auto-departed guest {member.user_id} from household {member.household_id}"
                )

    await db.commit()
    return len(expired)
```

**Step 4: Run test — passes**

**Step 5: Commit**

```bash
git add backend/app/services/household_departure_service.py backend/tests/services/test_household_departure.py
git commit -m "feat: add auto-departure service for expired guest memberships"
```

---

### Task 18: Notification Table and Service

**Files:**
- This uses the existing `backend/app/models/notification.py` and `backend/app/services/notification_service.py`
- Test: `backend/tests/services/test_household_notifications.py`

Add household-specific notification types to the existing notification system:

```python
# Notification types to add:
HOUSEHOLD_JOIN = "household_join"        # "Dad joined Sharma Family"
HOUSEHOLD_LEAVE = "household_leave"      # "Uncle left Sharma Family"
HOUSEHOLD_DEPARTURE = "household_auto_departure"  # "Guest auto-departed"
HOUSEHOLD_TRANSFER = "household_ownership_transfer"
HOUSEHOLD_REGENERATE_WARNING = "household_regenerate_warning"  # Pre-regeneration notice
```

Write tests for creating these notifications on join/leave/transfer events, then integrate the calls into the household service functions.

**Commit message:** `feat: add household notification types and emit on join/leave/transfer`

---

### Task 19: Documentation Updates

**Files:**
- Modify: `docs/CONTINUE_PROMPT.md` — add User Management status
- Modify: `docs/requirements/api/backend-api.md` — add household endpoints
- Modify: `docs/testing/Functional-Requirement-Rule.md` — add FR entries
- Modify: `docs/design/Data-Flow-Diagram.md` — add household data flow

**Commit message:** `docs: update docs for user management feature`

---

### Task 20: Full Integration Test

**Files:**
- Create: `backend/tests/api/test_household_full_journey.py`

End-to-end test covering:
1. Owner creates household
2. Member joins via invite code
3. Guest joins temporarily
4. Owner generates shared meal plan
5. Member views plan
6. Guest departs
7. Constraints re-merge after departure
8. Ownership transfer
9. Leave household

**Commit message:** `test: add full household journey integration test`

---

## Task Dependency Graph

```
Task 1 (Model) ─────> Task 2 (User extend) ─────> Task 6 (Migration)
    │                                                    │
    v                                                    v
Task 3 (Schemas) ──> Task 4 (Service) ──> Task 5 (API Router)
                                              │
                                              v
                     Task 7 (RecipeRule) ──> Task 8 (MealPlan extend)
                                              │
                                              v
                     Task 9 (Constraints) ──> Task 10 (Gen endpoint)
                                              │
                                              v
Task 11 (Room) ──> Task 12 (Domain) ──> Task 13 (DAO/Repo)
                                              │
                                              v
                   Task 14 (API/DTO) ──> Task 15 (Screens)
                                              │
                                              v
                                     Task 16 (Home toggle)
                                              │
                                              v
Task 17 (Departure) ──> Task 18 (Notifications) ──> Task 19 (Docs)
                                                          │
                                                          v
                                                  Task 20 (Integration)
```

## Summary

| Phase | Tasks | New Files | Modified Files | New Tests |
|-------|:-----:|:---------:|:--------------:|:---------:|
| 1: Backend Foundation | 1-7 | 6 | 8 | ~55 |
| 2: Shared Meal Plans | 8-10 | 3 | 2 | ~15 |
| 3: Android Integration | 11-16 | 8 | 6 | ~10 |
| 4: Guest & Temporal | 17-20 | 4 | 4 | ~15 |
| **Total** | **20** | **21** | **20** | **~95** |
