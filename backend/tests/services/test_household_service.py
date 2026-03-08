"""
Tests for HouseholdService — notification and constraint methods.

Covers: create_notification, notify_household_members, get_merged_constraints.
"""

import json
import uuid

from sqlalchemy import select

from app.models.household import HouseholdMember
from app.models.notification import Notification
from app.models.user import UserPreferences
from app.services.household_service import HouseholdService
from tests.factories import make_household, make_household_member, make_user


# ──────────────────────────────────────────────
# create_notification
# ──────────────────────────────────────────────


async def test_create_notification_basic(db_session):
    """Creates a notification linked to household and user."""
    user = make_user()
    db_session.add(user)
    await db_session.flush()

    household = make_household(owner_id=user.id)
    db_session.add(household)
    await db_session.flush()

    await HouseholdService.create_notification(
        household_id=household.id,
        user_id=user.id,
        notification_type="JOIN",
        title="Welcome",
        body="You joined the household",
        db=db_session,
        metadata={"role": "OWNER"},
    )
    await db_session.commit()

    result = await db_session.execute(
        select(Notification).where(Notification.household_id == household.id)
    )
    notif = result.scalar_one()
    assert notif.type == "JOIN"
    assert notif.user_id == user.id
    assert notif.title == "Welcome"
    assert '"role"' in notif.metadata_json


async def test_create_notification_no_metadata(db_session):
    """Creates a notification without metadata_json."""
    user = make_user()
    db_session.add(user)
    await db_session.flush()

    household = make_household(owner_id=user.id)
    db_session.add(household)
    await db_session.flush()

    await HouseholdService.create_notification(
        household_id=household.id,
        user_id=user.id,
        notification_type="LEAVE",
        title="Goodbye",
        body="Member left",
        db=db_session,
    )
    await db_session.commit()

    result = await db_session.execute(
        select(Notification).where(Notification.household_id == household.id)
    )
    notif = result.scalar_one()
    assert notif.metadata_json is None


# ──────────────────────────────────────────────
# notify_household_members
# ──────────────────────────────────────────────


async def test_notify_all_members(db_session):
    """Sends notification to all active members."""
    owner = make_user(name="Owner")
    member_user = make_user(name="Member", phone_number="+911111100040")
    db_session.add(owner)
    db_session.add(member_user)
    await db_session.flush()

    household = make_household(owner_id=owner.id)
    db_session.add(household)
    await db_session.flush()

    m1 = make_household_member(
        household_id=household.id, user_id=owner.id, role="OWNER"
    )
    m2 = make_household_member(
        household_id=household.id, user_id=member_user.id, role="MEMBER"
    )
    db_session.add(m1)
    db_session.add(m2)
    await db_session.flush()

    await HouseholdService.notify_household_members(
        household_id=household.id,
        notification_type="MEAL_PLAN",
        title="New plan",
        body="A new meal plan was generated",
        db=db_session,
    )
    await db_session.commit()

    result = await db_session.execute(
        select(Notification).where(Notification.household_id == household.id)
    )
    notifications = list(result.scalars().all())
    assert len(notifications) == 2
    user_ids = {n.user_id for n in notifications}
    assert owner.id in user_ids
    assert member_user.id in user_ids


async def test_notify_excludes_user(db_session):
    """Notification excludes the specified user."""
    owner = make_user(name="Owner")
    member_user = make_user(name="Member", phone_number="+911111100041")
    db_session.add(owner)
    db_session.add(member_user)
    await db_session.flush()

    household = make_household(owner_id=owner.id)
    db_session.add(household)
    await db_session.flush()

    m1 = make_household_member(
        household_id=household.id, user_id=owner.id, role="OWNER"
    )
    m2 = make_household_member(
        household_id=household.id, user_id=member_user.id, role="MEMBER"
    )
    db_session.add(m1)
    db_session.add(m2)
    await db_session.flush()

    await HouseholdService.notify_household_members(
        household_id=household.id,
        notification_type="LEAVE",
        title="Member left",
        body="A member left",
        db=db_session,
        exclude_user_id=owner.id,
    )
    await db_session.commit()

    result = await db_session.execute(
        select(Notification).where(Notification.household_id == household.id)
    )
    notifications = list(result.scalars().all())
    assert len(notifications) == 1
    assert notifications[0].user_id == member_user.id


async def test_notify_skips_metadata_only_members(db_session):
    """Members with user_id=None are skipped."""
    owner = make_user(name="Owner")
    db_session.add(owner)
    await db_session.flush()

    household = make_household(owner_id=owner.id)
    db_session.add(household)
    await db_session.flush()

    m1 = make_household_member(
        household_id=household.id, user_id=owner.id, role="OWNER"
    )
    # Metadata-only member (no user_id)
    m2 = make_household_member(
        household_id=household.id, user_id=None, role="MEMBER"
    )
    db_session.add(m1)
    db_session.add(m2)
    await db_session.flush()

    await HouseholdService.notify_household_members(
        household_id=household.id,
        notification_type="JOIN",
        title="New member",
        body="Someone joined",
        db=db_session,
    )
    await db_session.commit()

    result = await db_session.execute(
        select(Notification).where(Notification.household_id == household.id)
    )
    notifications = list(result.scalars().all())
    assert len(notifications) == 1
    assert notifications[0].user_id == owner.id


# ──────────────────────────────────────────────
# get_merged_constraints
# ──────────────────────────────────────────────


async def test_merged_constraints_multiple_members(db_session):
    """Constraints merge allergies (union) and dietary_tags (intersection)."""
    user1 = make_user(name="User 1", phone_number="+911111100042")
    user2 = make_user(name="User 2", phone_number="+911111100043")
    db_session.add(user1)
    db_session.add(user2)
    await db_session.flush()

    household = make_household(owner_id=user1.id)
    db_session.add(household)
    await db_session.flush()

    m1 = make_household_member(
        household_id=household.id, user_id=user1.id, role="OWNER"
    )
    m2 = make_household_member(
        household_id=household.id, user_id=user2.id, role="MEMBER"
    )
    db_session.add(m1)
    db_session.add(m2)
    await db_session.flush()

    # User 1: peanut allergy, vegetarian
    prefs1 = UserPreferences(
        id=str(uuid.uuid4()),
        user_id=user1.id,
        dietary_type="vegetarian",
        allergies=[{"ingredient": "peanuts", "severity": "SEVERE"}],
        disliked_ingredients=["karela"],
        dietary_tags=["vegetarian", "sattvic"],
    )
    # User 2: cashew allergy, vegetarian only (no sattvic)
    prefs2 = UserPreferences(
        id=str(uuid.uuid4()),
        user_id=user2.id,
        dietary_type="vegetarian",
        allergies=[{"ingredient": "cashew", "severity": "MILD"}],
        disliked_ingredients=["baingan"],
        dietary_tags=["vegetarian"],
    )
    db_session.add(prefs1)
    db_session.add(prefs2)
    await db_session.commit()

    constraints = await HouseholdService.get_merged_constraints(
        household_id=household.id, db=db_session
    )

    # Union of allergies
    assert "peanuts" in constraints["allergies"]
    assert "cashew" in constraints["allergies"]

    # Union of dislikes
    assert "karela" in constraints["dislikes"]
    assert "baingan" in constraints["dislikes"]

    # Intersection of dietary_tags (only "vegetarian" is shared)
    assert "vegetarian" in constraints["dietary_tags"]
    assert "sattvic" not in constraints["dietary_tags"]

    assert constraints["member_count"] == 2


async def test_merged_constraints_with_household_rules(db_session):
    """Constraints include household-scoped INCLUDE and EXCLUDE rules."""
    from app.models.recipe_rule import RecipeRule

    user = make_user(name="Rule User", phone_number="+911111100044")
    db_session.add(user)
    await db_session.flush()

    household = make_household(owner_id=user.id)
    db_session.add(household)
    await db_session.flush()

    m1 = make_household_member(
        household_id=household.id, user_id=user.id, role="OWNER"
    )
    db_session.add(m1)
    await db_session.flush()

    # Create household-scoped rules
    include_rule = RecipeRule(
        id=str(uuid.uuid4()),
        user_id=user.id,
        household_id=household.id,
        scope="HOUSEHOLD",
        target_type="INGREDIENT",
        action="INCLUDE",
        target_name="Dal",
        frequency_type="DAILY",
        meal_slot="LUNCH",
        enforcement="PREFERRED",
        is_active=True,
    )
    exclude_rule = RecipeRule(
        id=str(uuid.uuid4()),
        user_id=user.id,
        household_id=household.id,
        scope="HOUSEHOLD",
        target_type="INGREDIENT",
        action="EXCLUDE",
        target_name="Mushroom",
        frequency_type="NEVER",
        enforcement="REQUIRED",
        is_active=True,
    )
    db_session.add(include_rule)
    db_session.add(exclude_rule)
    await db_session.commit()

    constraints = await HouseholdService.get_merged_constraints(
        household_id=household.id, db=db_session
    )

    assert len(constraints["include_rules"]) == 1
    assert constraints["include_rules"][0]["target"] == "Dal"
    assert constraints["include_rules"][0]["meal_slot"] == "LUNCH"
    assert len(constraints["exclude_rules"]) == 1
    assert constraints["exclude_rules"][0]["target"] == "Mushroom"
