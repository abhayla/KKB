"""
Tests for Household and HouseholdMember SQLAlchemy models.
"""

import pytest
from sqlalchemy import select

from app.models.household import Household, HouseholdMember
from tests.factories import make_user, make_household, make_household_member


async def test_create_household(db_session):
    """Create household with owner, verify name/owner_id/is_active/max_members."""
    user = make_user()
    db_session.add(user)
    await db_session.flush()

    household = make_household(owner_id=user.id, name="Sharma Family", max_members=6)
    db_session.add(household)
    await db_session.commit()

    result = await db_session.execute(select(Household).where(Household.id == household.id))
    h = result.scalar_one()

    assert h.name == "Sharma Family"
    assert h.owner_id == user.id
    assert h.is_active is True
    assert h.max_members == 6


async def test_create_household_member(db_session):
    """Create member linked to user with role=OWNER, verify fields."""
    user = make_user()
    db_session.add(user)
    await db_session.flush()

    household = make_household(owner_id=user.id)
    db_session.add(household)
    await db_session.flush()

    member = make_household_member(household_id=household.id, user_id=user.id, role="OWNER")
    db_session.add(member)
    await db_session.commit()

    result = await db_session.execute(
        select(HouseholdMember).where(HouseholdMember.id == member.id)
    )
    m = result.scalar_one()

    assert m.household_id == household.id
    assert m.user_id == user.id
    assert m.role == "OWNER"


async def test_household_member_nullable_user(db_session):
    """Create member with user_id=None (metadata-only), verify."""
    user = make_user()
    db_session.add(user)
    await db_session.flush()

    household = make_household(owner_id=user.id)
    db_session.add(household)
    await db_session.flush()

    member = make_household_member(household_id=household.id, user_id=None)
    db_session.add(member)
    await db_session.commit()

    result = await db_session.execute(
        select(HouseholdMember).where(HouseholdMember.id == member.id)
    )
    m = result.scalar_one()

    assert m.user_id is None
    assert m.household_id == household.id


async def test_household_member_guest_fields(db_session):
    """Create GUEST member with is_temporary=True, leave_date, previous_household_id, verify all."""
    user = make_user()
    db_session.add(user)
    await db_session.flush()

    household = make_household(owner_id=user.id)
    db_session.add(household)
    await db_session.flush()

    from datetime import date

    member = make_household_member(
        household_id=household.id,
        user_id=None,
        role="GUEST",
        is_temporary=True,
        leave_date=date(2026, 4, 1),
        previous_household_id="some-previous-id",
    )
    db_session.add(member)
    await db_session.commit()

    result = await db_session.execute(
        select(HouseholdMember).where(HouseholdMember.id == member.id)
    )
    m = result.scalar_one()

    assert m.role == "GUEST"
    assert m.is_temporary is True
    assert m.leave_date == date(2026, 4, 1)
    assert m.previous_household_id == "some-previous-id"


async def test_household_cascade_delete(db_session):
    """Create household with member, delete household, verify member is also deleted."""
    user = make_user()
    db_session.add(user)
    await db_session.flush()

    household = make_household(owner_id=user.id)
    db_session.add(household)
    await db_session.flush()

    member = make_household_member(household_id=household.id, user_id=user.id, role="OWNER")
    db_session.add(member)
    await db_session.commit()

    member_id = member.id

    await db_session.delete(household)
    await db_session.commit()

    result = await db_session.execute(
        select(HouseholdMember).where(HouseholdMember.id == member_id)
    )
    assert result.scalar_one_or_none() is None


async def test_household_member_portion_size(db_session):
    """Create members with SMALL/REGULAR/LARGE portion_size, verify."""
    user = make_user()
    db_session.add(user)
    await db_session.flush()

    household = make_household(owner_id=user.id)
    db_session.add(household)
    await db_session.flush()

    sizes = ["SMALL", "REGULAR", "LARGE"]
    member_ids = []
    for size in sizes:
        m = make_household_member(household_id=household.id, portion_size=size)
        db_session.add(m)
        await db_session.flush()
        member_ids.append(m.id)

    await db_session.commit()

    for mid, expected_size in zip(member_ids, sizes):
        result = await db_session.execute(
            select(HouseholdMember).where(HouseholdMember.id == mid)
        )
        m = result.scalar_one()
        assert m.portion_size == expected_size


async def test_household_member_active_meal_slots(db_session):
    """Create member with active_meal_slots=["breakfast","lunch"], verify JSON storage."""
    user = make_user()
    db_session.add(user)
    await db_session.flush()

    household = make_household(owner_id=user.id)
    db_session.add(household)
    await db_session.flush()

    member = make_household_member(
        household_id=household.id,
        active_meal_slots=["breakfast", "lunch"],
    )
    db_session.add(member)
    await db_session.commit()

    result = await db_session.execute(
        select(HouseholdMember).where(HouseholdMember.id == member.id)
    )
    m = result.scalar_one()

    assert m.active_meal_slots == ["breakfast", "lunch"]
