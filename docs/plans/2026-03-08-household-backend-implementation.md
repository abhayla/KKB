# Household Backend Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build the backend foundation for multi-user household management — 4 layers of models, services, endpoints, and tests.

**Architecture:** New `Household` and `HouseholdMember` models with a `HouseholdService` handling CRUD, membership flows, and permission checks. Existing models (`User`, `RecipeRule`, `MealPlanItem`) get nullable columns for household scoping. All endpoints use FastAPI dependency injection for auth/permission checks. Zero breaking changes to existing APIs.

**Tech Stack:** Python 3.11+, FastAPI, SQLAlchemy async, Pydantic v2, Alembic, pytest + httpx (SQLite in-memory)

**Design Doc:** `docs/plans/2026-03-08-household-backend-design.md`

---

## Task 1: Create Household and HouseholdMember Models

**Files:**
- Create: `backend/app/models/household.py`
- Modify: `backend/app/models/__init__.py`
- Modify: `backend/app/db/postgres.py` (3 import blocks)
- Modify: `backend/tests/conftest.py` (model import)

**Step 1: Create the model file**

Create `backend/app/models/household.py`:

```python
"""Household and membership database models."""

import uuid
from datetime import date
from typing import TYPE_CHECKING, Optional

from sqlalchemy import Boolean, Date, DateTime, ForeignKey, Integer, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base, TimestampMixin
from app.models.user import JSONList

if TYPE_CHECKING:
    from app.models.user import FamilyMember, User


class Household(Base, TimestampMixin):
    """Household model — a family unit that shares meal plans."""

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
    invite_code_expires_at: Mapped[Optional] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    owner_id: Mapped[str] = mapped_column(
        String(36),
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
    )
    slot_config: Mapped[Optional[list]] = mapped_column(
        JSONList,
        nullable=True,
        default=None,
    )
    max_members: Mapped[int] = mapped_column(Integer, default=6, nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)

    # Relationships
    owner: Mapped["User"] = relationship(
        "User",
        foreign_keys=[owner_id],
        backref="owned_households",
    )
    members: Mapped[list["HouseholdMember"]] = relationship(
        "HouseholdMember",
        back_populates="household",
        cascade="all, delete-orphan",
    )


class HouseholdMember(Base, TimestampMixin):
    """Membership linking a user to a household with role and permissions."""

    __tablename__ = "household_members"

    id: Mapped[str] = mapped_column(
        String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4()),
    )
    household_id: Mapped[str] = mapped_column(
        String(36),
        ForeignKey("households.id", ondelete="CASCADE"),
        nullable=False,
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

    # Role and permissions
    role: Mapped[str] = mapped_column(
        String(20), nullable=False, default="MEMBER"
    )  # OWNER, MEMBER, GUEST
    can_edit_shared_plan: Mapped[bool] = mapped_column(
        Boolean, default=False, nullable=False
    )
    is_temporary: Mapped[bool] = mapped_column(
        Boolean, default=False, nullable=False
    )

    # Dates
    join_date: Mapped[Optional[date]] = mapped_column(Date, nullable=True)
    leave_date: Mapped[Optional[date]] = mapped_column(Date, nullable=True)

    # Guest return tracking
    previous_household_id: Mapped[Optional[str]] = mapped_column(
        String(36), nullable=True
    )

    # Per-member customization
    portion_size: Mapped[str] = mapped_column(
        String(20), default="REGULAR", nullable=False
    )  # SMALL, REGULAR, LARGE
    active_meal_slots: Mapped[Optional[list]] = mapped_column(
        JSONList, nullable=True
    )

    # Status
    status: Mapped[str] = mapped_column(
        String(20), default="ACTIVE", nullable=False
    )  # ACTIVE, PAUSED, LEFT

    # Relationships
    household: Mapped["Household"] = relationship(
        "Household", back_populates="members"
    )
    user: Mapped[Optional["User"]] = relationship(
        "User",
        foreign_keys=[user_id],
    )
    family_member: Mapped[Optional["FamilyMember"]] = relationship(
        "FamilyMember",
        foreign_keys=[family_member_id],
    )
```

**Step 2: Register in 5 import locations**

Add to `backend/app/models/__init__.py` — add import and `__all__` entries:

```python
from app.models.household import Household, HouseholdMember
# Add to __all__:
"Household",
"HouseholdMember",
```

Add `household,` to ALL THREE import blocks in `backend/app/db/postgres.py` (`init_db`, `create_tables`, `drop_tables`):

```python
from app.models import (  # noqa: F401
    ai_recipe_catalog,
    chat,
    config,
    festival,
    grocery,
    household,  # <-- ADD THIS LINE
    meal_plan,
    ...
)
```

Add to `backend/tests/conftest.py` model imports:

```python
from app.models import (  # noqa: F401
    ai_recipe_catalog,
    chat,
    config,
    festival,
    grocery,
    household,  # <-- ADD THIS LINE
    meal_plan,
    ...
)
```

**Step 3: Run existing tests to verify no breakage**

Run: `cd backend && PYTHONPATH=. pytest --collect-only -q 2>&1 | tail -5`
Expected: All tests collected, no import errors

**Step 4: Commit**

```bash
git add backend/app/models/household.py backend/app/models/__init__.py backend/app/db/postgres.py backend/tests/conftest.py
git commit -m "feat: add Household and HouseholdMember models"
```

---

## Task 2: Create Household Pydantic Schemas

**Files:**
- Create: `backend/app/schemas/household.py`

**Step 1: Create the schema file**

Create `backend/app/schemas/household.py`:

```python
"""Pydantic schemas for household management."""

from datetime import date, datetime
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field, field_validator


# --- Request Schemas ---

class HouseholdCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=100, description="Household name")


class HouseholdUpdate(BaseModel):
    name: Optional[str] = Field(None, min_length=1, max_length=100)
    max_members: Optional[int] = Field(None, ge=2, le=20)


class JoinHouseholdRequest(BaseModel):
    invite_code: str = Field(..., min_length=8, max_length=8)

    @field_validator("invite_code", mode="before")
    @classmethod
    def normalize_code(cls, v: str) -> str:
        return v.strip().upper() if v else v


class AddMemberByPhoneRequest(BaseModel):
    phone_number: str = Field(..., min_length=10, max_length=20)
    is_temporary: bool = Field(default=False)
    leave_date: Optional[date] = Field(None)


class UpdateMemberRequest(BaseModel):
    can_edit_shared_plan: Optional[bool] = None
    portion_size: Optional[str] = Field(None, pattern="^(SMALL|REGULAR|LARGE)$")
    is_temporary: Optional[bool] = None
    leave_date: Optional[date] = None
    role: Optional[str] = Field(None, pattern="^(MEMBER|GUEST)$")


class TransferOwnershipRequest(BaseModel):
    new_owner_member_id: str = Field(..., description="HouseholdMember ID of the new owner")


# --- Response Schemas ---

class HouseholdMemberResponse(BaseModel):
    id: str
    household_id: str
    user_id: Optional[str] = None
    family_member_id: Optional[str] = None
    name: Optional[str] = None
    role: str
    can_edit_shared_plan: bool
    is_temporary: bool
    join_date: Optional[date] = None
    leave_date: Optional[date] = None
    portion_size: str = "REGULAR"
    status: str

    model_config = ConfigDict(from_attributes=True)


class HouseholdResponse(BaseModel):
    id: str
    name: str
    invite_code: Optional[str] = None
    owner_id: str
    max_members: int
    member_count: int = 0
    is_active: bool
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class HouseholdDetailResponse(BaseModel):
    household: HouseholdResponse
    members: list[HouseholdMemberResponse]


class InviteCodeResponse(BaseModel):
    invite_code: str
    expires_at: datetime
```

**Step 2: Commit**

```bash
git add backend/app/schemas/household.py
git commit -m "feat: add household Pydantic schemas"
```

---

## Task 3: Create Household Test Factories

**Files:**
- Modify: `backend/tests/factories.py`

**Step 1: Add household factories**

Add to the bottom of `backend/tests/factories.py`:

```python
from app.models.household import Household, HouseholdMember


def make_household(owner_id: str, **overrides) -> Household:
    """Create a Household instance with sensible defaults."""
    defaults = {
        "id": str(uuid.uuid4()),
        "name": "Test Household",
        "invite_code": None,
        "invite_code_expires_at": None,
        "owner_id": owner_id,
        "max_members": 6,
        "is_active": True,
    }
    defaults.update(overrides)
    return Household(**defaults)


def make_household_member(household_id: str, **overrides) -> HouseholdMember:
    """Create a HouseholdMember instance with sensible defaults."""
    from datetime import date as date_type

    defaults = {
        "id": str(uuid.uuid4()),
        "household_id": household_id,
        "user_id": None,
        "family_member_id": None,
        "role": "MEMBER",
        "can_edit_shared_plan": False,
        "is_temporary": False,
        "join_date": date_type.today(),
        "leave_date": None,
        "previous_household_id": None,
        "portion_size": "REGULAR",
        "active_meal_slots": None,
        "status": "ACTIVE",
    }
    defaults.update(overrides)
    return HouseholdMember(**defaults)
```

**Step 2: Commit**

```bash
git add backend/tests/factories.py
git commit -m "feat: add household test factories"
```

---

## Task 4: Write Failing Tests for Household Model

**Files:**
- Create: `backend/tests/test_household_model.py`

**Step 1: Write the test file**

Create `backend/tests/test_household_model.py`:

```python
"""Tests for Household and HouseholdMember SQLAlchemy models."""

import uuid

import pytest
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.household import Household, HouseholdMember
from tests.factories import make_household, make_household_member, make_user


@pytest.mark.asyncio
async def test_create_household(db_session: AsyncSession):
    """Test creating a household with owner."""
    user = make_user()
    db_session.add(user)
    await db_session.commit()

    household = make_household(owner_id=user.id, name="Sharma Family")
    db_session.add(household)
    await db_session.commit()

    result = await db_session.execute(
        select(Household).where(Household.id == household.id)
    )
    saved = result.scalar_one()
    assert saved.name == "Sharma Family"
    assert saved.owner_id == user.id
    assert saved.is_active is True
    assert saved.max_members == 6


@pytest.mark.asyncio
async def test_create_household_member(db_session: AsyncSession):
    """Test creating a household member linked to a user."""
    user = make_user()
    db_session.add(user)
    await db_session.commit()

    household = make_household(owner_id=user.id)
    db_session.add(household)
    await db_session.commit()

    member = make_household_member(
        household_id=household.id,
        user_id=user.id,
        role="OWNER",
        can_edit_shared_plan=True,
    )
    db_session.add(member)
    await db_session.commit()

    result = await db_session.execute(
        select(HouseholdMember).where(HouseholdMember.id == member.id)
    )
    saved = result.scalar_one()
    assert saved.role == "OWNER"
    assert saved.can_edit_shared_plan is True
    assert saved.user_id == user.id
    assert saved.status == "ACTIVE"


@pytest.mark.asyncio
async def test_household_member_nullable_user(db_session: AsyncSession):
    """Test creating a metadata-only member (no linked user)."""
    user = make_user()
    db_session.add(user)
    await db_session.commit()

    household = make_household(owner_id=user.id)
    db_session.add(household)
    await db_session.commit()

    member = make_household_member(
        household_id=household.id,
        user_id=None,
        role="MEMBER",
    )
    db_session.add(member)
    await db_session.commit()

    result = await db_session.execute(
        select(HouseholdMember).where(HouseholdMember.id == member.id)
    )
    saved = result.scalar_one()
    assert saved.user_id is None


@pytest.mark.asyncio
async def test_household_member_guest_fields(db_session: AsyncSession):
    """Test guest-specific fields (is_temporary, leave_date, previous_household_id)."""
    from datetime import date

    user = make_user()
    db_session.add(user)
    await db_session.commit()

    household = make_household(owner_id=user.id)
    db_session.add(household)
    await db_session.commit()

    prev_hh_id = str(uuid.uuid4())
    member = make_household_member(
        household_id=household.id,
        user_id=user.id,
        role="GUEST",
        is_temporary=True,
        leave_date=date(2026, 3, 14),
        previous_household_id=prev_hh_id,
    )
    db_session.add(member)
    await db_session.commit()

    result = await db_session.execute(
        select(HouseholdMember).where(HouseholdMember.id == member.id)
    )
    saved = result.scalar_one()
    assert saved.is_temporary is True
    assert saved.leave_date == date(2026, 3, 14)
    assert saved.previous_household_id == prev_hh_id


@pytest.mark.asyncio
async def test_household_defaults(db_session: AsyncSession):
    """Test default values for household fields."""
    user = make_user()
    db_session.add(user)
    await db_session.commit()

    household = make_household(owner_id=user.id)
    db_session.add(household)
    await db_session.commit()

    assert household.is_active is True
    assert household.max_members == 6
    assert household.invite_code is None


@pytest.mark.asyncio
async def test_household_member_portion_size(db_session: AsyncSession):
    """Test portion_size default and custom values."""
    user = make_user()
    db_session.add(user)
    await db_session.commit()

    household = make_household(owner_id=user.id)
    db_session.add(household)
    await db_session.commit()

    # Default portion
    member1 = make_household_member(household_id=household.id, user_id=user.id)
    db_session.add(member1)
    await db_session.commit()
    assert member1.portion_size == "REGULAR"

    # Custom portion
    user2 = make_user()
    db_session.add(user2)
    await db_session.commit()

    member2 = make_household_member(
        household_id=household.id,
        user_id=user2.id,
        portion_size="SMALL",
    )
    db_session.add(member2)
    await db_session.commit()
    assert member2.portion_size == "SMALL"


@pytest.mark.asyncio
async def test_household_cascade_delete(db_session: AsyncSession):
    """Test that deleting a household cascades to members."""
    user = make_user()
    db_session.add(user)
    await db_session.commit()

    household = make_household(owner_id=user.id)
    db_session.add(household)
    await db_session.commit()

    member = make_household_member(
        household_id=household.id, user_id=user.id, role="OWNER"
    )
    db_session.add(member)
    await db_session.commit()

    member_id = member.id

    await db_session.delete(household)
    await db_session.commit()

    result = await db_session.execute(
        select(HouseholdMember).where(HouseholdMember.id == member_id)
    )
    assert result.scalar_one_or_none() is None
```

**Step 2: Run tests to verify they pass**

Run: `cd backend && PYTHONPATH=. pytest tests/test_household_model.py -v`
Expected: 7 tests PASS

**Step 3: Commit**

```bash
git add backend/tests/test_household_model.py
git commit -m "test: add household model tests (7 tests)"
```

---

## Task 5: Create Household Service (CRUD)

**Files:**
- Create: `backend/app/services/household_service.py`

**Step 1: Create the service file**

Create `backend/app/services/household_service.py`:

```python
"""Household management service."""

import logging
import secrets
import string
import uuid
from datetime import date, datetime, timedelta, timezone
from typing import Optional

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.core.exceptions import ConflictError, NotFoundError
from app.models.household import Household, HouseholdMember
from app.models.user import FamilyMember, User

logger = logging.getLogger(__name__)


def _generate_invite_code() -> str:
    """Generate an 8-character alphanumeric invite code."""
    chars = string.ascii_uppercase + string.digits
    return "".join(secrets.choice(chars) for _ in range(8))


async def create_household(
    db: AsyncSession,
    name: str,
    user: User,
) -> Household:
    """Create a new household with the user as OWNER."""
    now = datetime.now(timezone.utc)

    household = Household(
        id=str(uuid.uuid4()),
        name=name,
        owner_id=user.id,
        invite_code=_generate_invite_code(),
        invite_code_expires_at=now + timedelta(days=7),
        created_at=now,
        updated_at=now,
    )
    db.add(household)

    # Create owner membership
    member = HouseholdMember(
        id=str(uuid.uuid4()),
        household_id=household.id,
        user_id=user.id,
        role="OWNER",
        can_edit_shared_plan=True,
        join_date=date.today(),
        status="ACTIVE",
        created_at=now,
        updated_at=now,
    )
    db.add(member)

    await db.commit()
    await db.refresh(household)

    logger.info(f"Created household '{name}' for user {user.id}")
    return household


async def get_household(
    db: AsyncSession,
    household_id: str,
) -> Household:
    """Get household by ID with members loaded."""
    result = await db.execute(
        select(Household)
        .options(selectinload(Household.members))
        .where(Household.id == household_id, Household.is_active == True)
    )
    household = result.scalar_one_or_none()

    if not household:
        raise NotFoundError("Household not found")

    return household


async def get_member_count(db: AsyncSession, household_id: str) -> int:
    """Get count of ACTIVE members in a household."""
    result = await db.execute(
        select(func.count(HouseholdMember.id)).where(
            HouseholdMember.household_id == household_id,
            HouseholdMember.status == "ACTIVE",
        )
    )
    return result.scalar_one()


async def get_membership(
    db: AsyncSession,
    household_id: str,
    user_id: str,
) -> Optional[HouseholdMember]:
    """Get a user's membership in a household."""
    result = await db.execute(
        select(HouseholdMember).where(
            HouseholdMember.household_id == household_id,
            HouseholdMember.user_id == user_id,
            HouseholdMember.status == "ACTIVE",
        )
    )
    return result.scalar_one_or_none()


async def require_membership(
    db: AsyncSession,
    household_id: str,
    user_id: str,
) -> HouseholdMember:
    """Get membership or raise 404."""
    member = await get_membership(db, household_id, user_id)
    if not member:
        raise NotFoundError("Not a member of this household")
    return member


async def require_owner(
    db: AsyncSession,
    household_id: str,
    user_id: str,
) -> HouseholdMember:
    """Get membership and verify OWNER role, or raise 403."""
    from fastapi import HTTPException, status

    member = await require_membership(db, household_id, user_id)
    if member.role != "OWNER":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only the household owner can perform this action",
        )
    return member


async def update_household(
    db: AsyncSession,
    household_id: str,
    user: User,
    name: Optional[str] = None,
    max_members: Optional[int] = None,
) -> Household:
    """Update household settings. Owner only."""
    await require_owner(db, household_id, user.id)
    household = await get_household(db, household_id)

    if name is not None:
        household.name = name
    if max_members is not None:
        household.max_members = max_members

    household.updated_at = datetime.now(timezone.utc)
    await db.commit()
    await db.refresh(household)

    logger.info(f"Updated household {household_id}")
    return household


async def deactivate_household(
    db: AsyncSession,
    household_id: str,
    user: User,
) -> None:
    """Deactivate a household. Owner only."""
    await require_owner(db, household_id, user.id)
    household = await get_household(db, household_id)

    # Check if there are other linked members
    active_linked = await db.execute(
        select(func.count(HouseholdMember.id)).where(
            HouseholdMember.household_id == household_id,
            HouseholdMember.status == "ACTIVE",
            HouseholdMember.user_id.isnot(None),
            HouseholdMember.user_id != user.id,
        )
    )
    if active_linked.scalar_one() > 0:
        raise ConflictError(
            "Cannot deactivate household with active linked members. Transfer ownership first."
        )

    household.is_active = False
    household.updated_at = datetime.now(timezone.utc)
    await db.commit()

    logger.info(f"Deactivated household {household_id}")


async def list_members(
    db: AsyncSession,
    household_id: str,
    user: User,
) -> list[HouseholdMember]:
    """List all active members of a household. Requires membership."""
    await require_membership(db, household_id, user.id)

    result = await db.execute(
        select(HouseholdMember)
        .where(
            HouseholdMember.household_id == household_id,
            HouseholdMember.status == "ACTIVE",
        )
        .order_by(HouseholdMember.join_date)
    )
    return list(result.scalars().all())


async def add_member_by_phone(
    db: AsyncSession,
    household_id: str,
    phone_number: str,
    user: User,
    is_temporary: bool = False,
    leave_date: Optional[date] = None,
) -> HouseholdMember:
    """Add a member by phone number. Owner only.

    If a user with that phone exists, links them.
    Otherwise creates a metadata-only membership.
    """
    await require_owner(db, household_id, user.id)
    household = await get_household(db, household_id)

    # Check member cap
    count = await get_member_count(db, household_id)
    if count >= household.max_members:
        raise ConflictError(
            f"Household is full ({household.max_members} members max)"
        )

    # Look up user by phone
    result = await db.execute(
        select(User).where(User.phone_number == phone_number, User.is_active == True)
    )
    existing_user = result.scalar_one_or_none()

    now = datetime.now(timezone.utc)

    if existing_user:
        # Check not already a member
        existing_member = await get_membership(db, household_id, existing_user.id)
        if existing_member:
            raise ConflictError("User is already a member of this household")

        member = HouseholdMember(
            id=str(uuid.uuid4()),
            household_id=household_id,
            user_id=existing_user.id,
            role="GUEST" if is_temporary else "MEMBER",
            is_temporary=is_temporary,
            join_date=date.today(),
            leave_date=leave_date,
            status="ACTIVE",
            created_at=now,
            updated_at=now,
        )
    else:
        # Create metadata-only family member + household member
        fm = FamilyMember(
            id=str(uuid.uuid4()),
            user_id=user.id,
            name=f"Phone: {phone_number}",
            created_at=now,
            updated_at=now,
        )
        db.add(fm)

        member = HouseholdMember(
            id=str(uuid.uuid4()),
            household_id=household_id,
            user_id=None,
            family_member_id=fm.id,
            role="GUEST" if is_temporary else "MEMBER",
            is_temporary=is_temporary,
            join_date=date.today(),
            leave_date=leave_date,
            status="ACTIVE",
            created_at=now,
            updated_at=now,
        )

    db.add(member)
    await db.commit()
    await db.refresh(member)

    logger.info(f"Added member to household {household_id} via phone {phone_number}")
    return member
```

**Step 2: Commit**

```bash
git add backend/app/services/household_service.py
git commit -m "feat: add household service (CRUD + add_member_by_phone)"
```

---

## Task 6: Create Household Router (Layer 1 Endpoints)

**Files:**
- Create: `backend/app/api/v1/endpoints/households.py`
- Modify: `backend/app/api/v1/router.py`

**Step 1: Create the router file**

Create `backend/app/api/v1/endpoints/households.py`:

```python
"""Household management API endpoints."""

import logging
from datetime import datetime, timezone

from fastapi import APIRouter, status

from app.api.deps import CurrentUser, DbSession
from app.schemas.household import (
    AddMemberByPhoneRequest,
    HouseholdCreate,
    HouseholdDetailResponse,
    HouseholdMemberResponse,
    HouseholdResponse,
    HouseholdUpdate,
)
from app.services import household_service

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/households", tags=["households"])


@router.post("", response_model=HouseholdResponse, status_code=status.HTTP_201_CREATED)
async def create_household(
    db: DbSession,
    current_user: CurrentUser,
    body: HouseholdCreate,
) -> HouseholdResponse:
    """Create a new household. Caller becomes OWNER."""
    household = await household_service.create_household(
        db=db, name=body.name, user=current_user
    )
    return HouseholdResponse(
        id=household.id,
        name=household.name,
        invite_code=household.invite_code,
        owner_id=household.owner_id,
        max_members=household.max_members,
        member_count=1,
        is_active=household.is_active,
        created_at=household.created_at,
        updated_at=household.updated_at,
    )


@router.get("/{household_id}", response_model=HouseholdDetailResponse)
async def get_household(
    household_id: str,
    db: DbSession,
    current_user: CurrentUser,
) -> HouseholdDetailResponse:
    """Get household details with members. Requires membership."""
    await household_service.require_membership(db, household_id, current_user.id)
    household = await household_service.get_household(db, household_id)
    count = await household_service.get_member_count(db, household_id)

    members = [
        HouseholdMemberResponse(
            id=m.id,
            household_id=m.household_id,
            user_id=m.user_id,
            family_member_id=m.family_member_id,
            name=m.user.name if m.user else (m.family_member.name if m.family_member else None),
            role=m.role,
            can_edit_shared_plan=m.can_edit_shared_plan,
            is_temporary=m.is_temporary,
            join_date=m.join_date,
            leave_date=m.leave_date,
            portion_size=m.portion_size,
            status=m.status,
        )
        for m in household.members
        if m.status == "ACTIVE"
    ]

    return HouseholdDetailResponse(
        household=HouseholdResponse(
            id=household.id,
            name=household.name,
            invite_code=household.invite_code,
            owner_id=household.owner_id,
            max_members=household.max_members,
            member_count=count,
            is_active=household.is_active,
            created_at=household.created_at,
            updated_at=household.updated_at,
        ),
        members=members,
    )


@router.put("/{household_id}", response_model=HouseholdResponse)
async def update_household(
    household_id: str,
    db: DbSession,
    current_user: CurrentUser,
    body: HouseholdUpdate,
) -> HouseholdResponse:
    """Update household settings. Owner only."""
    household = await household_service.update_household(
        db=db,
        household_id=household_id,
        user=current_user,
        name=body.name,
        max_members=body.max_members,
    )
    count = await household_service.get_member_count(db, household_id)

    return HouseholdResponse(
        id=household.id,
        name=household.name,
        invite_code=household.invite_code,
        owner_id=household.owner_id,
        max_members=household.max_members,
        member_count=count,
        is_active=household.is_active,
        created_at=household.created_at,
        updated_at=household.updated_at,
    )


@router.delete("/{household_id}", status_code=status.HTTP_204_NO_CONTENT)
async def deactivate_household(
    household_id: str,
    db: DbSession,
    current_user: CurrentUser,
) -> None:
    """Deactivate a household. Owner only. Must transfer ownership first if linked members exist."""
    await household_service.deactivate_household(db, household_id, current_user)


@router.get("/{household_id}/members", response_model=list[HouseholdMemberResponse])
async def list_members(
    household_id: str,
    db: DbSession,
    current_user: CurrentUser,
) -> list[HouseholdMemberResponse]:
    """List all active household members. Requires membership."""
    members = await household_service.list_members(db, household_id, current_user)

    return [
        HouseholdMemberResponse(
            id=m.id,
            household_id=m.household_id,
            user_id=m.user_id,
            family_member_id=m.family_member_id,
            name=m.user.name if m.user else (m.family_member.name if m.family_member else None),
            role=m.role,
            can_edit_shared_plan=m.can_edit_shared_plan,
            is_temporary=m.is_temporary,
            join_date=m.join_date,
            leave_date=m.leave_date,
            portion_size=m.portion_size,
            status=m.status,
        )
        for m in members
    ]


@router.post(
    "/{household_id}/members",
    response_model=HouseholdMemberResponse,
    status_code=status.HTTP_201_CREATED,
)
async def add_member_by_phone(
    household_id: str,
    db: DbSession,
    current_user: CurrentUser,
    body: AddMemberByPhoneRequest,
) -> HouseholdMemberResponse:
    """Add a member by phone number. Owner only."""
    member = await household_service.add_member_by_phone(
        db=db,
        household_id=household_id,
        phone_number=body.phone_number,
        user=current_user,
        is_temporary=body.is_temporary,
        leave_date=body.leave_date,
    )

    return HouseholdMemberResponse(
        id=member.id,
        household_id=member.household_id,
        user_id=member.user_id,
        family_member_id=member.family_member_id,
        name=None,
        role=member.role,
        can_edit_shared_plan=member.can_edit_shared_plan,
        is_temporary=member.is_temporary,
        join_date=member.join_date,
        leave_date=member.leave_date,
        portion_size=member.portion_size,
        status=member.status,
    )
```

**Step 2: Register router in `backend/app/api/v1/router.py`**

Add import and include:

```python
from app.api.v1.endpoints import households
# ...
api_router.include_router(households.router)
```

**Step 3: Commit**

```bash
git add backend/app/api/v1/endpoints/households.py backend/app/api/v1/router.py
git commit -m "feat: add household API router (6 endpoints)"
```

---

## Task 7: Write API Tests for Household CRUD

**Files:**
- Create: `backend/tests/api/test_household_crud.py`

**Step 1: Write the test file**

Create `backend/tests/api/test_household_crud.py`:

```python
"""Tests for household CRUD API endpoints."""

import uuid

import pytest
import pytest_asyncio
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.user import User
from tests.api.conftest import make_api_client
from tests.factories import make_user, make_preferences


@pytest_asyncio.fixture
async def test_user(db_session: AsyncSession) -> User:
    """Create a test user with preferences."""
    user = make_user(name="Ramesh Sharma")
    db_session.add(user)
    prefs = make_preferences(user.id)
    db_session.add(prefs)
    await db_session.commit()
    await db_session.refresh(user)
    return user


@pytest_asyncio.fixture
async def authenticated_client(
    db_session: AsyncSession, test_user: User
) -> AsyncClient:
    async with make_api_client(db_session, test_user) as c:
        yield c


@pytest_asyncio.fixture
async def second_user(db_session: AsyncSession) -> User:
    """Create a second user for multi-user tests."""
    user = make_user(name="Sunita Sharma", phone_number="+919876543210")
    db_session.add(user)
    prefs = make_preferences(user.id)
    db_session.add(prefs)
    await db_session.commit()
    await db_session.refresh(user)
    return user


@pytest_asyncio.fixture
async def second_client(
    db_session: AsyncSession, second_user: User
) -> AsyncClient:
    async with make_api_client(db_session, second_user) as c:
        yield c


# --- Create Household ---

@pytest.mark.asyncio
async def test_create_household(authenticated_client: AsyncClient):
    """Test creating a household."""
    response = await authenticated_client.post(
        "/api/v1/households",
        json={"name": "Sharma Family"},
    )

    assert response.status_code == 201
    data = response.json()
    assert data["name"] == "Sharma Family"
    assert data["member_count"] == 1
    assert data["is_active"] is True
    assert data["max_members"] == 6
    assert data["invite_code"] is not None
    assert len(data["invite_code"]) == 8
    assert "id" in data


@pytest.mark.asyncio
async def test_create_household_empty_name_fails(authenticated_client: AsyncClient):
    """Test that empty name is rejected."""
    response = await authenticated_client.post(
        "/api/v1/households",
        json={"name": ""},
    )
    assert response.status_code == 422


# --- Get Household ---

@pytest.mark.asyncio
async def test_get_household(authenticated_client: AsyncClient):
    """Test getting household details."""
    create_resp = await authenticated_client.post(
        "/api/v1/households",
        json={"name": "Sharma Family"},
    )
    household_id = create_resp.json()["id"]

    response = await authenticated_client.get(f"/api/v1/households/{household_id}")

    assert response.status_code == 200
    data = response.json()
    assert data["household"]["name"] == "Sharma Family"
    assert len(data["members"]) == 1
    assert data["members"][0]["role"] == "OWNER"


@pytest.mark.asyncio
async def test_get_household_not_found(authenticated_client: AsyncClient):
    """Test getting non-existent household."""
    fake_id = str(uuid.uuid4())
    response = await authenticated_client.get(f"/api/v1/households/{fake_id}")
    assert response.status_code == 404


@pytest.mark.asyncio
async def test_get_household_non_member_rejected(
    authenticated_client: AsyncClient, second_client: AsyncClient
):
    """Test that non-members cannot see household details."""
    create_resp = await authenticated_client.post(
        "/api/v1/households",
        json={"name": "Private Family"},
    )
    household_id = create_resp.json()["id"]

    response = await second_client.get(f"/api/v1/households/{household_id}")
    assert response.status_code == 404


# --- Update Household ---

@pytest.mark.asyncio
async def test_update_household_name(authenticated_client: AsyncClient):
    """Test updating household name."""
    create_resp = await authenticated_client.post(
        "/api/v1/households",
        json={"name": "Old Name"},
    )
    household_id = create_resp.json()["id"]

    response = await authenticated_client.put(
        f"/api/v1/households/{household_id}",
        json={"name": "New Name"},
    )

    assert response.status_code == 200
    assert response.json()["name"] == "New Name"


@pytest.mark.asyncio
async def test_update_household_non_owner_rejected(
    authenticated_client: AsyncClient, second_client: AsyncClient, second_user: User, db_session: AsyncSession
):
    """Test that non-owners cannot update household."""
    from app.models.household import HouseholdMember
    from datetime import date, datetime, timezone

    create_resp = await authenticated_client.post(
        "/api/v1/households",
        json={"name": "Owner Only"},
    )
    household_id = create_resp.json()["id"]

    # Manually add second_user as MEMBER
    now = datetime.now(timezone.utc)
    member = HouseholdMember(
        id=str(uuid.uuid4()),
        household_id=household_id,
        user_id=second_user.id,
        role="MEMBER",
        join_date=date.today(),
        status="ACTIVE",
        created_at=now,
        updated_at=now,
    )
    db_session.add(member)
    await db_session.commit()

    response = await second_client.put(
        f"/api/v1/households/{household_id}",
        json={"name": "Hacked Name"},
    )
    assert response.status_code == 403


# --- Deactivate Household ---

@pytest.mark.asyncio
async def test_deactivate_household(authenticated_client: AsyncClient):
    """Test deactivating a household with only the owner."""
    create_resp = await authenticated_client.post(
        "/api/v1/households",
        json={"name": "To Delete"},
    )
    household_id = create_resp.json()["id"]

    response = await authenticated_client.delete(f"/api/v1/households/{household_id}")
    assert response.status_code == 204

    # Verify it's gone
    get_resp = await authenticated_client.get(f"/api/v1/households/{household_id}")
    assert get_resp.status_code == 404


@pytest.mark.asyncio
async def test_deactivate_with_linked_members_blocked(
    authenticated_client: AsyncClient, second_user: User, db_session: AsyncSession
):
    """Test that deactivation is blocked when linked members exist."""
    from app.models.household import HouseholdMember
    from datetime import date, datetime, timezone

    create_resp = await authenticated_client.post(
        "/api/v1/households",
        json={"name": "Has Members"},
    )
    household_id = create_resp.json()["id"]

    now = datetime.now(timezone.utc)
    member = HouseholdMember(
        id=str(uuid.uuid4()),
        household_id=household_id,
        user_id=second_user.id,
        role="MEMBER",
        join_date=date.today(),
        status="ACTIVE",
        created_at=now,
        updated_at=now,
    )
    db_session.add(member)
    await db_session.commit()

    response = await authenticated_client.delete(f"/api/v1/households/{household_id}")
    assert response.status_code == 409


# --- List Members ---

@pytest.mark.asyncio
async def test_list_members(authenticated_client: AsyncClient):
    """Test listing household members."""
    create_resp = await authenticated_client.post(
        "/api/v1/households",
        json={"name": "Family"},
    )
    household_id = create_resp.json()["id"]

    response = await authenticated_client.get(
        f"/api/v1/households/{household_id}/members"
    )

    assert response.status_code == 200
    data = response.json()
    assert len(data) == 1
    assert data[0]["role"] == "OWNER"


# --- Add Member by Phone ---

@pytest.mark.asyncio
async def test_add_member_by_phone_existing_user(
    authenticated_client: AsyncClient, second_user: User
):
    """Test adding a member by phone when user exists."""
    create_resp = await authenticated_client.post(
        "/api/v1/households",
        json={"name": "Family"},
    )
    household_id = create_resp.json()["id"]

    response = await authenticated_client.post(
        f"/api/v1/households/{household_id}/members",
        json={"phone_number": second_user.phone_number},
    )

    assert response.status_code == 201
    data = response.json()
    assert data["user_id"] == second_user.id
    assert data["role"] == "MEMBER"


@pytest.mark.asyncio
async def test_add_member_by_phone_unknown_user(authenticated_client: AsyncClient):
    """Test adding a member by phone when user doesn't exist (metadata-only)."""
    create_resp = await authenticated_client.post(
        "/api/v1/households",
        json={"name": "Family"},
    )
    household_id = create_resp.json()["id"]

    response = await authenticated_client.post(
        f"/api/v1/households/{household_id}/members",
        json={"phone_number": "+911234567890"},
    )

    assert response.status_code == 201
    data = response.json()
    assert data["user_id"] is None
    assert data["family_member_id"] is not None


@pytest.mark.asyncio
async def test_add_member_duplicate_rejected(
    authenticated_client: AsyncClient, second_user: User
):
    """Test that adding the same user twice is rejected."""
    create_resp = await authenticated_client.post(
        "/api/v1/households",
        json={"name": "Family"},
    )
    household_id = create_resp.json()["id"]

    # Add once
    await authenticated_client.post(
        f"/api/v1/households/{household_id}/members",
        json={"phone_number": second_user.phone_number},
    )

    # Add again
    response = await authenticated_client.post(
        f"/api/v1/households/{household_id}/members",
        json={"phone_number": second_user.phone_number},
    )
    assert response.status_code == 409


@pytest.mark.asyncio
async def test_add_member_max_cap(
    authenticated_client: AsyncClient, db_session: AsyncSession, test_user: User
):
    """Test that member cap is enforced."""
    # Create household with max_members=2 (owner + 1)
    create_resp = await authenticated_client.post(
        "/api/v1/households",
        json={"name": "Small Family"},
    )
    household_id = create_resp.json()["id"]

    # Update max to 2
    await authenticated_client.put(
        f"/api/v1/households/{household_id}",
        json={"max_members": 2},
    )

    # Add one member (should succeed — owner=1, now 2)
    resp1 = await authenticated_client.post(
        f"/api/v1/households/{household_id}/members",
        json={"phone_number": "+911111111111"},
    )
    assert resp1.status_code == 201

    # Add another (should fail — already at cap)
    resp2 = await authenticated_client.post(
        f"/api/v1/households/{household_id}/members",
        json={"phone_number": "+912222222222"},
    )
    assert resp2.status_code == 409


@pytest.mark.asyncio
async def test_add_member_non_owner_rejected(
    authenticated_client: AsyncClient,
    second_client: AsyncClient,
    second_user: User,
    db_session: AsyncSession,
):
    """Test that non-owners cannot add members."""
    from app.models.household import HouseholdMember
    from datetime import date, datetime, timezone

    create_resp = await authenticated_client.post(
        "/api/v1/households",
        json={"name": "Family"},
    )
    household_id = create_resp.json()["id"]

    # Add second_user as MEMBER
    now = datetime.now(timezone.utc)
    member = HouseholdMember(
        id=str(uuid.uuid4()),
        household_id=household_id,
        user_id=second_user.id,
        role="MEMBER",
        join_date=date.today(),
        status="ACTIVE",
        created_at=now,
        updated_at=now,
    )
    db_session.add(member)
    await db_session.commit()

    # Second user tries to add someone
    response = await second_client.post(
        f"/api/v1/households/{household_id}/members",
        json={"phone_number": "+913333333333"},
    )
    assert response.status_code == 403


@pytest.mark.asyncio
async def test_add_temporary_member(authenticated_client: AsyncClient):
    """Test adding a temporary (guest) member."""
    create_resp = await authenticated_client.post(
        "/api/v1/households",
        json={"name": "Family"},
    )
    household_id = create_resp.json()["id"]

    response = await authenticated_client.post(
        f"/api/v1/households/{household_id}/members",
        json={
            "phone_number": "+914444444444",
            "is_temporary": True,
            "leave_date": "2026-03-14",
        },
    )

    assert response.status_code == 201
    data = response.json()
    assert data["role"] == "GUEST"
    assert data["is_temporary"] is True
    assert data["leave_date"] == "2026-03-14"
```

**Step 2: Run tests**

Run: `cd backend && PYTHONPATH=. pytest tests/api/test_household_crud.py -v`
Expected: 16 tests PASS

**Step 3: Run full test suite to verify no regression**

Run: `cd backend && PYTHONPATH=. pytest --tb=short -q`
Expected: All existing tests still pass

**Step 4: Commit**

```bash
git add backend/tests/api/test_household_crud.py
git commit -m "test: add household CRUD API tests (16 tests)"
```

---

## Task 8: Generate Alembic Migration for Layer 1

**Files:**
- Create: `backend/alembic/versions/xxxx_add_households.py` (auto-generated)

**Step 1: Generate migration**

Run: `cd backend && alembic revision --autogenerate -m "add households and household_members tables"`

**Step 2: Review the generated migration**

Open the generated file and verify it creates:
- `households` table with all columns
- `household_members` table with all columns and foreign keys
- Indexes on `invite_code` (unique) and foreign keys

**Step 3: Apply migration**

Run: `cd backend && alembic upgrade head`

**Step 4: Commit**

```bash
git add backend/alembic/versions/
git commit -m "migration: add households and household_members tables"
```

---

## Task 9: Add Membership Flow Endpoints (Layer 2)

**Files:**
- Modify: `backend/app/services/household_service.py` (add invite, join, leave, transfer, update_member, remove_member)
- Modify: `backend/app/api/v1/endpoints/households.py` (add 6 endpoints)

**Step 1: Add membership methods to household_service.py**

Append to `backend/app/services/household_service.py`:

```python
async def refresh_invite_code(
    db: AsyncSession,
    household_id: str,
    user: User,
) -> tuple[str, datetime]:
    """Generate a new invite code. Owner only. Invalidates old code."""
    await require_owner(db, household_id, user.id)
    household = await get_household(db, household_id)

    new_code = _generate_invite_code()
    expires = datetime.now(timezone.utc) + timedelta(days=7)

    household.invite_code = new_code
    household.invite_code_expires_at = expires
    household.updated_at = datetime.now(timezone.utc)
    await db.commit()

    logger.info(f"Refreshed invite code for household {household_id}")
    return new_code, expires


async def join_by_invite_code(
    db: AsyncSession,
    invite_code: str,
    user: User,
) -> HouseholdMember:
    """Join a household via invite code."""
    now = datetime.now(timezone.utc)

    result = await db.execute(
        select(Household).where(
            Household.invite_code == invite_code,
            Household.is_active == True,
        )
    )
    household = result.scalar_one_or_none()

    if not household:
        raise NotFoundError("Invalid invite code")

    if household.invite_code_expires_at and household.invite_code_expires_at < now:
        raise ConflictError("Invite code has expired")

    # Check member cap
    count = await get_member_count(db, household.id)
    if count >= household.max_members:
        raise ConflictError(f"Household is full ({household.max_members} members max)")

    # Check not already a member
    existing = await get_membership(db, household.id, user.id)
    if existing:
        raise ConflictError("Already a member of this household")

    member = HouseholdMember(
        id=str(uuid.uuid4()),
        household_id=household.id,
        user_id=user.id,
        role="MEMBER",
        can_edit_shared_plan=False,
        join_date=date.today(),
        status="ACTIVE",
        created_at=now,
        updated_at=now,
    )
    db.add(member)
    await db.commit()
    await db.refresh(member)

    logger.info(f"User {user.id} joined household {household.id} via invite code")
    return member


async def leave_household(
    db: AsyncSession,
    household_id: str,
    user: User,
) -> None:
    """Leave a household. Owner must transfer first."""
    member = await require_membership(db, household_id, user.id)

    if member.role == "OWNER":
        raise ConflictError(
            "Owner cannot leave. Transfer ownership first."
        )

    member.status = "LEFT"
    member.updated_at = datetime.now(timezone.utc)
    await db.commit()

    logger.info(f"User {user.id} left household {household_id}")


async def transfer_ownership(
    db: AsyncSession,
    household_id: str,
    new_owner_member_id: str,
    user: User,
) -> None:
    """Transfer ownership to another linked member. Owner only."""
    old_owner = await require_owner(db, household_id, user.id)

    result = await db.execute(
        select(HouseholdMember).where(
            HouseholdMember.id == new_owner_member_id,
            HouseholdMember.household_id == household_id,
            HouseholdMember.status == "ACTIVE",
        )
    )
    new_owner = result.scalar_one_or_none()

    if not new_owner:
        raise NotFoundError("Member not found")
    if new_owner.user_id is None:
        raise ConflictError("Cannot transfer to a metadata-only member")

    now = datetime.now(timezone.utc)

    # Swap roles
    old_owner.role = "MEMBER"
    old_owner.updated_at = now

    new_owner.role = "OWNER"
    new_owner.can_edit_shared_plan = True
    new_owner.updated_at = now

    # Update household owner
    household = await get_household(db, household_id)
    household.owner_id = new_owner.user_id
    household.updated_at = now

    await db.commit()

    logger.info(
        f"Transferred ownership of household {household_id} "
        f"from {user.id} to {new_owner.user_id}"
    )


async def update_member(
    db: AsyncSession,
    household_id: str,
    member_id: str,
    user: User,
    can_edit_shared_plan: Optional[bool] = None,
    portion_size: Optional[str] = None,
    is_temporary: Optional[bool] = None,
    leave_date: Optional[date] = None,
    role: Optional[str] = None,
) -> HouseholdMember:
    """Update a member's settings. Owner only."""
    await require_owner(db, household_id, user.id)

    result = await db.execute(
        select(HouseholdMember).where(
            HouseholdMember.id == member_id,
            HouseholdMember.household_id == household_id,
            HouseholdMember.status == "ACTIVE",
        )
    )
    member = result.scalar_one_or_none()

    if not member:
        raise NotFoundError("Member not found")

    if can_edit_shared_plan is not None:
        member.can_edit_shared_plan = can_edit_shared_plan
    if portion_size is not None:
        member.portion_size = portion_size
    if is_temporary is not None:
        member.is_temporary = is_temporary
    if leave_date is not None:
        member.leave_date = leave_date
    if role is not None and role in ("MEMBER", "GUEST"):
        member.role = role

    member.updated_at = datetime.now(timezone.utc)
    await db.commit()
    await db.refresh(member)

    return member


async def remove_member(
    db: AsyncSession,
    household_id: str,
    member_id: str,
    user: User,
) -> None:
    """Remove a member from household. Owner only. Cannot remove self."""
    owner = await require_owner(db, household_id, user.id)

    if owner.id == member_id:
        raise ConflictError("Cannot remove yourself. Transfer ownership first.")

    result = await db.execute(
        select(HouseholdMember).where(
            HouseholdMember.id == member_id,
            HouseholdMember.household_id == household_id,
            HouseholdMember.status == "ACTIVE",
        )
    )
    member = result.scalar_one_or_none()

    if not member:
        raise NotFoundError("Member not found")

    member.status = "LEFT"
    member.updated_at = datetime.now(timezone.utc)
    await db.commit()

    logger.info(f"Removed member {member_id} from household {household_id}")
```

**Step 2: Add Layer 2 endpoints to households.py router**

Append to `backend/app/api/v1/endpoints/households.py`:

```python
from app.schemas.household import (
    # ... existing imports plus:
    InviteCodeResponse,
    JoinHouseholdRequest,
    TransferOwnershipRequest,
    UpdateMemberRequest,
)


@router.post("/{household_id}/invite-code", response_model=InviteCodeResponse)
async def refresh_invite_code(
    household_id: str,
    db: DbSession,
    current_user: CurrentUser,
) -> InviteCodeResponse:
    """Generate a new invite code. Owner only."""
    code, expires = await household_service.refresh_invite_code(
        db, household_id, current_user
    )
    return InviteCodeResponse(invite_code=code, expires_at=expires)


@router.post("/join", response_model=HouseholdMemberResponse, status_code=status.HTTP_201_CREATED)
async def join_household(
    db: DbSession,
    current_user: CurrentUser,
    body: JoinHouseholdRequest,
) -> HouseholdMemberResponse:
    """Join a household via invite code."""
    member = await household_service.join_by_invite_code(
        db, body.invite_code, current_user
    )
    return HouseholdMemberResponse(
        id=member.id,
        household_id=member.household_id,
        user_id=member.user_id,
        role=member.role,
        can_edit_shared_plan=member.can_edit_shared_plan,
        is_temporary=member.is_temporary,
        join_date=member.join_date,
        leave_date=member.leave_date,
        portion_size=member.portion_size,
        status=member.status,
    )


@router.post("/{household_id}/leave", status_code=status.HTTP_204_NO_CONTENT)
async def leave_household(
    household_id: str,
    db: DbSession,
    current_user: CurrentUser,
) -> None:
    """Leave a household. Owner must transfer ownership first."""
    await household_service.leave_household(db, household_id, current_user)


@router.post("/{household_id}/transfer-ownership", status_code=status.HTTP_204_NO_CONTENT)
async def transfer_ownership(
    household_id: str,
    db: DbSession,
    current_user: CurrentUser,
    body: TransferOwnershipRequest,
) -> None:
    """Transfer household ownership. Owner only."""
    await household_service.transfer_ownership(
        db, household_id, body.new_owner_member_id, current_user
    )


@router.put("/{household_id}/members/{member_id}", response_model=HouseholdMemberResponse)
async def update_member(
    household_id: str,
    member_id: str,
    db: DbSession,
    current_user: CurrentUser,
    body: UpdateMemberRequest,
) -> HouseholdMemberResponse:
    """Update a member's permissions/settings. Owner only."""
    member = await household_service.update_member(
        db=db,
        household_id=household_id,
        member_id=member_id,
        user=current_user,
        can_edit_shared_plan=body.can_edit_shared_plan,
        portion_size=body.portion_size,
        is_temporary=body.is_temporary,
        leave_date=body.leave_date,
        role=body.role,
    )
    return HouseholdMemberResponse(
        id=member.id,
        household_id=member.household_id,
        user_id=member.user_id,
        family_member_id=member.family_member_id,
        role=member.role,
        can_edit_shared_plan=member.can_edit_shared_plan,
        is_temporary=member.is_temporary,
        join_date=member.join_date,
        leave_date=member.leave_date,
        portion_size=member.portion_size,
        status=member.status,
    )


@router.delete("/{household_id}/members/{member_id}", status_code=status.HTTP_204_NO_CONTENT)
async def remove_member(
    household_id: str,
    member_id: str,
    db: DbSession,
    current_user: CurrentUser,
) -> None:
    """Remove a member from household. Owner only."""
    await household_service.remove_member(db, household_id, member_id, current_user)
```

**Step 3: Commit**

```bash
git add backend/app/services/household_service.py backend/app/api/v1/endpoints/households.py
git commit -m "feat: add membership flow endpoints (invite, join, leave, transfer)"
```

---

## Task 10: Write API Tests for Membership Flows

**Files:**
- Create: `backend/tests/api/test_household_membership.py`

**Step 1: Write the test file**

Create `backend/tests/api/test_household_membership.py`:

```python
"""Tests for household membership flow API endpoints."""

import uuid
from datetime import datetime, timedelta, timezone

import pytest
import pytest_asyncio
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.household import Household, HouseholdMember
from app.models.user import User
from tests.api.conftest import make_api_client
from tests.factories import make_user, make_preferences


@pytest_asyncio.fixture
async def test_user(db_session: AsyncSession) -> User:
    user = make_user(name="Ramesh Sharma")
    db_session.add(user)
    prefs = make_preferences(user.id)
    db_session.add(prefs)
    await db_session.commit()
    await db_session.refresh(user)
    return user


@pytest_asyncio.fixture
async def authenticated_client(
    db_session: AsyncSession, test_user: User
) -> AsyncClient:
    async with make_api_client(db_session, test_user) as c:
        yield c


@pytest_asyncio.fixture
async def second_user(db_session: AsyncSession) -> User:
    user = make_user(name="Sunita Sharma", phone_number="+919876543210")
    db_session.add(user)
    prefs = make_preferences(user.id)
    db_session.add(prefs)
    await db_session.commit()
    await db_session.refresh(user)
    return user


@pytest_asyncio.fixture
async def second_client(
    db_session: AsyncSession, second_user: User
) -> AsyncClient:
    async with make_api_client(db_session, second_user) as c:
        yield c


async def _create_household(client: AsyncClient, name: str = "Test Family") -> dict:
    """Helper to create a household and return response data."""
    resp = await client.post("/api/v1/households", json={"name": name})
    assert resp.status_code == 201
    return resp.json()


# --- Invite Code ---

@pytest.mark.asyncio
async def test_refresh_invite_code(authenticated_client: AsyncClient):
    """Test generating a new invite code."""
    hh = await _create_household(authenticated_client)

    response = await authenticated_client.post(
        f"/api/v1/households/{hh['id']}/invite-code"
    )

    assert response.status_code == 200
    data = response.json()
    assert len(data["invite_code"]) == 8
    assert data["invite_code"] != hh["invite_code"]
    assert "expires_at" in data


@pytest.mark.asyncio
async def test_refresh_invite_code_non_owner_rejected(
    authenticated_client: AsyncClient,
    second_client: AsyncClient,
    second_user: User,
    db_session: AsyncSession,
):
    """Test that non-owners cannot refresh invite code."""
    from datetime import date

    hh = await _create_household(authenticated_client)
    now = datetime.now(timezone.utc)
    member = HouseholdMember(
        id=str(uuid.uuid4()),
        household_id=hh["id"],
        user_id=second_user.id,
        role="MEMBER",
        join_date=date.today(),
        status="ACTIVE",
        created_at=now,
        updated_at=now,
    )
    db_session.add(member)
    await db_session.commit()

    response = await second_client.post(
        f"/api/v1/households/{hh['id']}/invite-code"
    )
    assert response.status_code == 403


# --- Join ---

@pytest.mark.asyncio
async def test_join_by_invite_code(
    authenticated_client: AsyncClient, second_client: AsyncClient
):
    """Test joining a household via invite code."""
    hh = await _create_household(authenticated_client)
    invite_code = hh["invite_code"]

    response = await second_client.post(
        "/api/v1/households/join",
        json={"invite_code": invite_code},
    )

    assert response.status_code == 201
    data = response.json()
    assert data["role"] == "MEMBER"
    assert data["household_id"] == hh["id"]


@pytest.mark.asyncio
async def test_join_invalid_code(second_client: AsyncClient):
    """Test joining with an invalid code."""
    response = await second_client.post(
        "/api/v1/households/join",
        json={"invite_code": "ZZZZZZZZ"},
    )
    assert response.status_code == 404


@pytest.mark.asyncio
async def test_join_expired_code(
    authenticated_client: AsyncClient,
    second_client: AsyncClient,
    db_session: AsyncSession,
):
    """Test joining with an expired code."""
    hh = await _create_household(authenticated_client)

    # Manually expire the code
    from sqlalchemy import update
    await db_session.execute(
        update(Household)
        .where(Household.id == hh["id"])
        .values(invite_code_expires_at=datetime.now(timezone.utc) - timedelta(hours=1))
    )
    await db_session.commit()

    response = await second_client.post(
        "/api/v1/households/join",
        json={"invite_code": hh["invite_code"]},
    )
    assert response.status_code == 409


@pytest.mark.asyncio
async def test_join_already_member(authenticated_client: AsyncClient):
    """Test that joining twice is rejected."""
    hh = await _create_household(authenticated_client)

    response = await authenticated_client.post(
        "/api/v1/households/join",
        json={"invite_code": hh["invite_code"]},
    )
    assert response.status_code == 409


# --- Leave ---

@pytest.mark.asyncio
async def test_member_leaves(
    authenticated_client: AsyncClient,
    second_client: AsyncClient,
):
    """Test a member leaving a household."""
    hh = await _create_household(authenticated_client)

    await second_client.post(
        "/api/v1/households/join",
        json={"invite_code": hh["invite_code"]},
    )

    response = await second_client.post(
        f"/api/v1/households/{hh['id']}/leave"
    )
    assert response.status_code == 204


@pytest.mark.asyncio
async def test_owner_cannot_leave(authenticated_client: AsyncClient):
    """Test that owner cannot leave without transferring."""
    hh = await _create_household(authenticated_client)

    response = await authenticated_client.post(
        f"/api/v1/households/{hh['id']}/leave"
    )
    assert response.status_code == 409


# --- Transfer Ownership ---

@pytest.mark.asyncio
async def test_transfer_ownership(
    authenticated_client: AsyncClient,
    second_client: AsyncClient,
):
    """Test transferring ownership to another member."""
    hh = await _create_household(authenticated_client)

    # Second user joins
    join_resp = await second_client.post(
        "/api/v1/households/join",
        json={"invite_code": hh["invite_code"]},
    )
    new_owner_member_id = join_resp.json()["id"]

    # Transfer
    response = await authenticated_client.post(
        f"/api/v1/households/{hh['id']}/transfer-ownership",
        json={"new_owner_member_id": new_owner_member_id},
    )
    assert response.status_code == 204

    # Verify: old owner can now leave
    leave_resp = await authenticated_client.post(
        f"/api/v1/households/{hh['id']}/leave"
    )
    assert leave_resp.status_code == 204


@pytest.mark.asyncio
async def test_transfer_to_nonexistent_member(authenticated_client: AsyncClient):
    """Test transferring to a non-existent member."""
    hh = await _create_household(authenticated_client)

    response = await authenticated_client.post(
        f"/api/v1/households/{hh['id']}/transfer-ownership",
        json={"new_owner_member_id": str(uuid.uuid4())},
    )
    assert response.status_code == 404


# --- Update Member ---

@pytest.mark.asyncio
async def test_update_member_permissions(
    authenticated_client: AsyncClient,
    second_client: AsyncClient,
):
    """Test updating a member's edit permissions."""
    hh = await _create_household(authenticated_client)

    join_resp = await second_client.post(
        "/api/v1/households/join",
        json={"invite_code": hh["invite_code"]},
    )
    member_id = join_resp.json()["id"]

    response = await authenticated_client.put(
        f"/api/v1/households/{hh['id']}/members/{member_id}",
        json={"can_edit_shared_plan": True, "portion_size": "LARGE"},
    )

    assert response.status_code == 200
    data = response.json()
    assert data["can_edit_shared_plan"] is True
    assert data["portion_size"] == "LARGE"


# --- Remove Member ---

@pytest.mark.asyncio
async def test_remove_member(
    authenticated_client: AsyncClient,
    second_client: AsyncClient,
):
    """Test removing a member from household."""
    hh = await _create_household(authenticated_client)

    join_resp = await second_client.post(
        "/api/v1/households/join",
        json={"invite_code": hh["invite_code"]},
    )
    member_id = join_resp.json()["id"]

    response = await authenticated_client.delete(
        f"/api/v1/households/{hh['id']}/members/{member_id}"
    )
    assert response.status_code == 204


@pytest.mark.asyncio
async def test_remove_self_rejected(authenticated_client: AsyncClient):
    """Test that owner cannot remove themselves."""
    hh = await _create_household(authenticated_client)

    members_resp = await authenticated_client.get(
        f"/api/v1/households/{hh['id']}/members"
    )
    owner_member_id = members_resp.json()[0]["id"]

    response = await authenticated_client.delete(
        f"/api/v1/households/{hh['id']}/members/{owner_member_id}"
    )
    assert response.status_code == 409
```

**Step 2: Run tests**

Run: `cd backend && PYTHONPATH=. pytest tests/api/test_household_membership.py -v`
Expected: 15 tests PASS

**Step 3: Run full suite**

Run: `cd backend && PYTHONPATH=. pytest --tb=short -q`
Expected: No regressions

**Step 4: Commit**

```bash
git add backend/tests/api/test_household_membership.py
git commit -m "test: add household membership flow tests (15 tests)"
```

---

## Tasks 11-14: Layers 3-4 (Scope Extensions + Generation + Notifications)

> **Note:** Layers 3-4 follow the same pattern as Layers 1-2. Each layer requires:
>
> 1. Alembic migration (extend existing tables)
> 2. Service methods (scoped queries, constraint merging)
> 3. Router endpoints (extend existing routers)
> 4. Tests
>
> **These tasks should be planned in detail AFTER Layers 1-2 are implemented and verified**, because:
> - Layer 3 extends `RecipeRule`, `MealPlanItem`, `MealPlan` — the exact column names and defaults may need adjustment based on Layer 1-2 learnings
> - Layer 4 modifies `ai_meal_service.py` — the constraint merging logic depends on how household members are loaded (determined in Layer 2)
>
> **Placeholder tasks:**

### Task 11: Layer 3 Migration — Scope Extensions
- Add `household_id`, `scope` to `recipe_rules`
- Add `scope`, `for_user_id`, `meal_status` to `meal_plan_items`
- Add `household_id`, `slot_scope` to `meal_plans`
- Extend recipe_rules, grocery, stats routers with household-scoped endpoints
- ~20 tests

### Task 12: Layer 3 Tests — Scoped Queries
- Test household recipe rules CRUD
- Test grocery suggest/approve/reject flow
- Test household stats aggregation
- Test scope filtering (PERSONAL vs HOUSEHOLD)

### Task 13: Layer 4 — Household Meal Generation
- Extend `ai_meal_service.py` with `generate_household_meal_plan()`
- Constraint merging (allergy union, dislike union)
- Extend meal_plans router with household generation + swap/lock
- ~15 tests

### Task 14: Layer 4 — Notifications Extension
- Extend Notification model with `household_id`, `metadata`
- Extend notifications router with household-scoped queries
- Create notifications on join/leave/transfer/suggestion events
- ~10 tests

---

## Summary

| Task | What | Files | Tests |
|:----:|------|:-----:|:-----:|
| 1 | Household + HouseholdMember models | 4 modified, 1 created | 0 |
| 2 | Pydantic schemas | 1 created | 0 |
| 3 | Test factories | 1 modified | 0 |
| 4 | Model tests | 1 created | 7 |
| 5 | Household service (CRUD) | 1 created | 0 |
| 6 | Household router (6 endpoints) | 1 created, 1 modified | 0 |
| 7 | CRUD API tests | 1 created | 16 |
| 8 | Alembic migration (Layer 1) | 1 created | 0 |
| 9 | Membership flow service + endpoints | 2 modified | 0 |
| 10 | Membership flow tests | 1 created | 15 |
| 11-12 | Layer 3: Scope extensions | TBD | ~20 |
| 13-14 | Layer 4: Generation + notifications | TBD | ~25 |

**Total Layer 1-2: 38 tests, 12 endpoints, 2 new files, 1 migration**
**Total all layers: ~98 tests, ~25 endpoints, 4 migrations**
