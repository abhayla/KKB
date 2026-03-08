"""
Tests for Household and HouseholdMember SQLAlchemy models.
"""

import uuid

import pytest
from sqlalchemy import select

from app.models.household import Household, HouseholdMember
from app.models.user import User
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


# ──────────────────────────────────────────────
# SCOPE COLUMNS ON EXISTING MODELS
# ──────────────────────────────────────────────


async def test_recipe_rule_household_scope(db_session):
    """RecipeRule can be scoped to a household."""
    from app.models.recipe_rule import RecipeRule

    user = make_user()
    db_session.add(user)
    await db_session.flush()

    household = make_household(owner_id=user.id)
    db_session.add(household)
    await db_session.flush()

    rule = RecipeRule(
        id=str(uuid.uuid4()),
        user_id=user.id,
        target_type="INGREDIENT",
        action="EXCLUDE",
        target_name="Mushroom",
        frequency_type="NEVER",
        enforcement="REQUIRED",
        is_active=True,
        household_id=household.id,
        scope="HOUSEHOLD",
    )
    db_session.add(rule)
    await db_session.commit()

    result = await db_session.execute(
        select(RecipeRule).where(RecipeRule.id == rule.id)
    )
    r = result.scalar_one()
    assert r.scope == "HOUSEHOLD"
    assert r.household_id == household.id


async def test_recipe_rule_default_scope_personal(db_session):
    """RecipeRule defaults to PERSONAL scope."""
    from app.models.recipe_rule import RecipeRule

    user = make_user()
    db_session.add(user)
    await db_session.flush()

    rule = RecipeRule(
        id=str(uuid.uuid4()),
        user_id=user.id,
        target_type="INGREDIENT",
        action="INCLUDE",
        target_name="Dal",
        frequency_type="DAILY",
        enforcement="PREFERRED",
        is_active=True,
    )
    db_session.add(rule)
    await db_session.commit()

    result = await db_session.execute(
        select(RecipeRule).where(RecipeRule.id == rule.id)
    )
    r = result.scalar_one()
    assert r.scope == "PERSONAL"
    assert r.household_id is None


async def test_meal_plan_household_scope(db_session):
    """MealPlan can be linked to a household with slot_scope."""
    from app.models.meal_plan import MealPlan
    from datetime import date

    user = make_user()
    db_session.add(user)
    await db_session.flush()

    household = make_household(owner_id=user.id)
    db_session.add(household)
    await db_session.flush()

    plan = MealPlan(
        id=str(uuid.uuid4()),
        user_id=user.id,
        household_id=household.id,
        week_start_date=date(2026, 3, 9),
        week_end_date=date(2026, 3, 15),
        is_active=True,
        slot_scope="BREAKFAST_ONLY",
    )
    db_session.add(plan)
    await db_session.commit()

    result = await db_session.execute(
        select(MealPlan).where(MealPlan.id == plan.id)
    )
    p = result.scalar_one()
    assert p.household_id == household.id
    assert p.slot_scope == "BREAKFAST_ONLY"


async def test_meal_plan_item_scope_and_status(db_session):
    """MealPlanItem has scope, for_user_id, and meal_status fields."""
    from app.models.meal_plan import MealPlan, MealPlanItem
    from datetime import date

    user = make_user()
    db_session.add(user)
    await db_session.flush()

    plan = MealPlan(
        id=str(uuid.uuid4()),
        user_id=user.id,
        week_start_date=date(2026, 3, 9),
        week_end_date=date(2026, 3, 15),
        is_active=True,
    )
    db_session.add(plan)
    await db_session.flush()

    item = MealPlanItem(
        id=str(uuid.uuid4()),
        meal_plan_id=plan.id,
        date=date(2026, 3, 9),
        meal_type="breakfast",
        recipe_name="Masala Dosa",
        scope="PERSONAL",
        for_user_id=user.id,
        meal_status="COOKED",
    )
    db_session.add(item)
    await db_session.commit()

    result = await db_session.execute(
        select(MealPlanItem).where(MealPlanItem.id == item.id)
    )
    i = result.scalar_one()
    assert i.scope == "PERSONAL"
    assert i.for_user_id == user.id
    assert i.meal_status == "COOKED"


async def test_notification_household_scope(db_session):
    """Notification can be linked to a household with metadata_json."""
    import json
    from app.models.notification import Notification

    user = make_user()
    db_session.add(user)
    await db_session.flush()

    household = make_household(owner_id=user.id)
    db_session.add(household)
    await db_session.flush()

    notif = Notification(
        id=str(uuid.uuid4()),
        user_id=user.id,
        household_id=household.id,
        type="JOIN",
        title="New member",
        body="Someone joined",
        metadata_json=json.dumps({"member_name": "Sunita"}),
    )
    db_session.add(notif)
    await db_session.commit()

    result = await db_session.execute(
        select(Notification).where(Notification.id == notif.id)
    )
    n = result.scalar_one()
    assert n.household_id == household.id
    assert '"member_name"' in n.metadata_json


async def test_user_household_columns(db_session):
    """User has active_household_id and passive_household_id."""
    user = make_user()
    db_session.add(user)
    await db_session.flush()

    household = make_household(owner_id=user.id)
    db_session.add(household)
    await db_session.flush()

    user.active_household_id = household.id
    await db_session.commit()

    result = await db_session.execute(select(User).where(User.id == user.id))
    u = result.scalar_one()
    assert u.active_household_id == household.id
    assert u.passive_household_id is None
