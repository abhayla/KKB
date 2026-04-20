"""Tests for favorite_service — business logic for user recipe favorites.

Covers:
- add_favorite: happy path + ConflictError on duplicate
- remove_favorite: happy path + NotFoundError when missing
- get_favorites: empty + populated + user isolation
- is_favorite: positive + negative
"""

import pytest
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import ConflictError, NotFoundError
from app.models.user import User
from app.services.favorite_service import (
    add_favorite,
    get_favorites,
    is_favorite,
    remove_favorite,
)


# ==================== add_favorite ====================


@pytest.mark.asyncio
async def test_add_favorite_creates_row_and_returns_response(
    db_session: AsyncSession, test_user: User
):
    result = await add_favorite(db_session, test_user.id, "recipe-abc")

    assert result.user_id == test_user.id
    assert result.recipe_id == "recipe-abc"

    # Verify it's queryable back.
    assert await is_favorite(db_session, test_user.id, "recipe-abc")


@pytest.mark.asyncio
async def test_add_favorite_raises_conflict_on_duplicate(
    db_session: AsyncSession, test_user: User
):
    await add_favorite(db_session, test_user.id, "recipe-dup")

    with pytest.raises(ConflictError):
        await add_favorite(db_session, test_user.id, "recipe-dup")


# ==================== remove_favorite ====================


@pytest.mark.asyncio
async def test_remove_favorite_deletes_row(
    db_session: AsyncSession, test_user: User
):
    await add_favorite(db_session, test_user.id, "recipe-rm")
    assert await is_favorite(db_session, test_user.id, "recipe-rm")

    await remove_favorite(db_session, test_user.id, "recipe-rm")

    assert not await is_favorite(db_session, test_user.id, "recipe-rm")


@pytest.mark.asyncio
async def test_remove_favorite_raises_not_found_when_missing(
    db_session: AsyncSession, test_user: User
):
    with pytest.raises(NotFoundError):
        await remove_favorite(db_session, test_user.id, "recipe-never-added")


# ==================== get_favorites ====================


@pytest.mark.asyncio
async def test_get_favorites_empty_when_user_has_none(
    db_session: AsyncSession, test_user: User
):
    result = await get_favorites(db_session, test_user.id)

    assert result.total == 0
    assert result.favorites == []


@pytest.mark.asyncio
async def test_get_favorites_returns_all_user_favorites(
    db_session: AsyncSession, test_user: User
):
    await add_favorite(db_session, test_user.id, "recipe-1")
    await add_favorite(db_session, test_user.id, "recipe-2")
    await add_favorite(db_session, test_user.id, "recipe-3")

    result = await get_favorites(db_session, test_user.id)

    assert result.total == 3
    recipe_ids = {f.recipe_id for f in result.favorites}
    assert recipe_ids == {"recipe-1", "recipe-2", "recipe-3"}


@pytest.mark.asyncio
async def test_get_favorites_isolates_by_user(
    db_session: AsyncSession, test_user: User
):
    """User A's favorites must not leak into user B's result set."""
    import uuid

    from app.models.user import User as UserModel

    other_user = UserModel(
        id=str(uuid.uuid4()),
        firebase_uid=f"firebase-{uuid.uuid4().hex[:8]}",
        email=f"other-{uuid.uuid4().hex[:6]}@example.com",
        name="Other User",
        is_active=True,
    )
    db_session.add(other_user)
    await db_session.commit()

    await add_favorite(db_session, test_user.id, "shared-recipe")
    await add_favorite(db_session, other_user.id, "shared-recipe")
    await add_favorite(db_session, other_user.id, "other-only-recipe")

    a = await get_favorites(db_session, test_user.id)
    b = await get_favorites(db_session, other_user.id)

    assert a.total == 1
    assert b.total == 2
    assert {f.recipe_id for f in a.favorites} == {"shared-recipe"}
    assert {f.recipe_id for f in b.favorites} == {"shared-recipe", "other-only-recipe"}


# ==================== is_favorite ====================


@pytest.mark.asyncio
async def test_is_favorite_true_when_present(
    db_session: AsyncSession, test_user: User
):
    await add_favorite(db_session, test_user.id, "recipe-chk")
    assert await is_favorite(db_session, test_user.id, "recipe-chk") is True


@pytest.mark.asyncio
async def test_is_favorite_false_when_absent(
    db_session: AsyncSession, test_user: User
):
    assert await is_favorite(db_session, test_user.id, "nonexistent") is False
