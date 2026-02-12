"""
Tests for Meal Plan API endpoints.

Covers:
- POST /api/v1/meal-plans/generate — generate a new meal plan
- GET /api/v1/meal-plans/current — get current week's plan
- GET /api/v1/meal-plans/{plan_id} — get plan by ID
- POST /api/v1/meal-plans/{plan_id}/items/{item_id}/swap — swap a meal item
- PUT /api/v1/meal-plans/{plan_id}/items/{item_id}/lock — toggle lock
- DELETE /api/v1/meal-plans/{plan_id}/items/{item_id} — remove item

Note: Endpoints use MealPlanRepository and RecipeRepository directly (not via DI),
so we mock at the repository method level.
"""

import asyncio

import pytest
from datetime import date, datetime, timedelta, timezone
from unittest.mock import AsyncMock, MagicMock, patch
from uuid import uuid4

import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.db.database import get_db
from app.main import app
from app.models.user import User


# ==================== Test Data Builders ====================


def _make_meal_item(
    item_id: str = None,
    recipe_name: str = "Test Recipe",
    is_locked: bool = False,
    recipe_id: str = None,
) -> dict:
    """Build a mock meal item dict."""
    return {
        "id": item_id or str(uuid4()),
        "recipe_id": recipe_id or str(uuid4()),
        "recipe_name": recipe_name,
        "recipe_image_url": None,
        "prep_time_minutes": 30,
        "calories": 250,
        "is_locked": is_locked,
        "dietary_tags": ["vegetarian"],
        "servings": 2,
        "is_swapped": False,
    }


def _make_plan(
    plan_id: str = None,
    user_id: str = "test-user",
    num_days: int = 7,
    locked_item_id: str = None,
) -> dict:
    """Build a mock meal plan dict as returned by MealPlanRepository."""
    plan_id = plan_id or str(uuid4())
    today = date.today()
    week_start = today - timedelta(days=today.weekday())
    now = datetime.now(timezone.utc)

    days = []
    for i in range(num_days):
        day_date = week_start + timedelta(days=i)
        breakfast_item = _make_meal_item(recipe_name=f"Breakfast Day {i+1}")
        lunch_item = _make_meal_item(recipe_name=f"Lunch Day {i+1}")
        dinner_item = _make_meal_item(recipe_name=f"Dinner Day {i+1}")
        snacks_item = _make_meal_item(recipe_name=f"Snack Day {i+1}")

        # Make the first lunch item locked if requested
        if i == 0 and locked_item_id:
            lunch_item["id"] = locked_item_id
            lunch_item["is_locked"] = True

        days.append({
            "date": day_date.isoformat(),
            "day_name": day_date.strftime("%A"),
            "festival": None,
            "meals": {
                "breakfast": [breakfast_item],
                "lunch": [lunch_item],
                "dinner": [dinner_item],
                "snacks": [snacks_item],
            },
        })

    return {
        "id": plan_id,
        "user_id": user_id,
        "week_start_date": week_start,
        "week_end_date": week_start + timedelta(days=6),
        "is_active": True,
        "days": days,
        "created_at": now,
        "updated_at": now,
    }


# ==================== Fixtures ====================


@pytest_asyncio.fixture
async def mp_user(db_session: AsyncSession) -> User:
    """Create a user for meal plan tests."""
    user_id = str(uuid4())
    user = User(
        id=user_id,
        firebase_uid=f"firebase-mp-{user_id}",
        email=f"mp-{user_id}@example.com",
        name="MealPlan Test User",
        is_onboarded=True,
        is_active=True,
    )
    db_session.add(user)
    await db_session.commit()
    await db_session.refresh(user)
    return user


@pytest_asyncio.fixture
async def mp_client(db_session: AsyncSession, mp_user: User):
    """Authenticated client for meal plan user."""

    async def override_get_db():
        yield db_session

    async def override_get_current_user():
        return mp_user

    app.dependency_overrides[get_db] = override_get_db
    app.dependency_overrides[get_current_user] = override_get_current_user

    async with AsyncClient(
        transport=ASGITransport(app=app),
        base_url="http://test",
    ) as ac:
        yield ac

    app.dependency_overrides.clear()


# ==================== POST /generate Tests ====================


@pytest.mark.asyncio
async def test_generate_meal_plan_success(mp_client: AsyncClient, mp_user: User):
    """POST /generate with mocked AI returns 7-day plan."""
    plan_id = str(uuid4())
    mock_plan = _make_plan(plan_id=plan_id, user_id=mp_user.id)

    # Mock the AI service's generate method
    mock_generated = MagicMock()
    mock_generated.days = []
    mock_generated.week_start_date = date.today().isoformat()
    mock_generated.week_end_date = (date.today() + timedelta(days=6)).isoformat()
    mock_generated.rules_applied = []

    with (
        patch(
            "app.api.v1.endpoints.meal_plans.AIMealService"
        ) as MockAI,
        patch(
            "app.api.v1.endpoints.meal_plans.MealPlanRepository"
        ) as MockRepo,
        patch(
            "app.api.v1.endpoints.meal_plans.UserRepository"
        ) as MockUserRepo,
    ):
        mock_ai_instance = MockAI.return_value
        mock_ai_instance.generate_meal_plan = AsyncMock(return_value=mock_generated)

        mock_repo_instance = MockRepo.return_value
        mock_repo_instance.deactivate_old_plans = AsyncMock(return_value=0)
        mock_repo_instance.create = AsyncMock(return_value=mock_plan)

        mock_user_repo_instance = MockUserRepo.return_value
        mock_user_repo_instance.get_preferences = AsyncMock(
            return_value={"cuisine_preferences": ["north"]}
        )

        response = await mp_client.post(
            "/api/v1/meal-plans/generate",
            json={"week_start_date": date.today().isoformat()},
        )

    assert response.status_code == 200
    data = response.json()
    assert data["id"] == plan_id
    assert len(data["days"]) == 7


@pytest.mark.asyncio
async def test_generate_deactivates_old_plans(mp_client: AsyncClient, mp_user: User):
    """POST /generate deactivates previous plans."""
    mock_plan = _make_plan(user_id=mp_user.id)

    mock_generated = MagicMock()
    mock_generated.days = []
    mock_generated.week_start_date = date.today().isoformat()
    mock_generated.week_end_date = (date.today() + timedelta(days=6)).isoformat()
    mock_generated.rules_applied = []

    with (
        patch(
            "app.api.v1.endpoints.meal_plans.AIMealService"
        ) as MockAI,
        patch(
            "app.api.v1.endpoints.meal_plans.MealPlanRepository"
        ) as MockRepo,
        patch(
            "app.api.v1.endpoints.meal_plans.UserRepository"
        ) as MockUserRepo,
    ):
        mock_ai_instance = MockAI.return_value
        mock_ai_instance.generate_meal_plan = AsyncMock(return_value=mock_generated)

        mock_repo_instance = MockRepo.return_value
        mock_repo_instance.deactivate_old_plans = AsyncMock(return_value=2)
        mock_repo_instance.create = AsyncMock(return_value=mock_plan)

        mock_user_repo_instance = MockUserRepo.return_value
        mock_user_repo_instance.get_preferences = AsyncMock(return_value=None)

        await mp_client.post(
            "/api/v1/meal-plans/generate",
            json={"week_start_date": date.today().isoformat()},
        )

        mock_repo_instance.deactivate_old_plans.assert_called_once()


@pytest.mark.asyncio
async def test_generate_invalid_date_fallback(mp_client: AsyncClient, mp_user: User):
    """POST /generate with invalid date falls back to current Monday."""
    mock_plan = _make_plan(user_id=mp_user.id)

    mock_generated = MagicMock()
    mock_generated.days = []
    mock_generated.week_start_date = date.today().isoformat()
    mock_generated.week_end_date = (date.today() + timedelta(days=6)).isoformat()
    mock_generated.rules_applied = []

    with (
        patch(
            "app.api.v1.endpoints.meal_plans.AIMealService"
        ) as MockAI,
        patch(
            "app.api.v1.endpoints.meal_plans.MealPlanRepository"
        ) as MockRepo,
        patch(
            "app.api.v1.endpoints.meal_plans.UserRepository"
        ) as MockUserRepo,
    ):
        mock_ai_instance = MockAI.return_value
        mock_ai_instance.generate_meal_plan = AsyncMock(return_value=mock_generated)

        mock_repo_instance = MockRepo.return_value
        mock_repo_instance.deactivate_old_plans = AsyncMock(return_value=0)
        mock_repo_instance.create = AsyncMock(return_value=mock_plan)

        mock_user_repo_instance = MockUserRepo.return_value
        mock_user_repo_instance.get_preferences = AsyncMock(return_value=None)

        # Pass an invalid date string
        response = await mp_client.post(
            "/api/v1/meal-plans/generate",
            json={"week_start_date": "not-a-date"},
        )

    # Should still succeed (falls back to current Monday)
    assert response.status_code == 200


@pytest.mark.asyncio
async def test_generate_timeout_returns_504(mp_client: AsyncClient, mp_user: User):
    """POST /generate returns 504 when AI generation times out."""
    with (
        patch(
            "app.api.v1.endpoints.meal_plans.AIMealService"
        ) as MockAI,
        patch(
            "app.api.v1.endpoints.meal_plans.UserRepository"
        ) as MockUserRepo,
    ):
        mock_ai_instance = MockAI.return_value
        mock_ai_instance.generate_meal_plan = AsyncMock(
            side_effect=asyncio.TimeoutError()
        )

        mock_user_repo_instance = MockUserRepo.return_value
        mock_user_repo_instance.get_preferences = AsyncMock(return_value=None)

        response = await mp_client.post(
            "/api/v1/meal-plans/generate",
            json={"week_start_date": date.today().isoformat()},
        )

    assert response.status_code == 504
    assert "timed out" in response.json()["detail"].lower()


@pytest.mark.asyncio
async def test_generate_succeeds_when_recipe_creation_fails(
    mp_client: AsyncClient, mp_user: User
):
    """POST /generate still returns 200 when recipe creation fails (degraded)."""
    plan_id = str(uuid4())
    mock_plan = _make_plan(plan_id=plan_id, user_id=mp_user.id)

    mock_generated = MagicMock()
    mock_generated.days = []
    mock_generated.week_start_date = date.today().isoformat()
    mock_generated.week_end_date = (date.today() + timedelta(days=6)).isoformat()
    mock_generated.rules_applied = []

    with (
        patch(
            "app.api.v1.endpoints.meal_plans.AIMealService"
        ) as MockAI,
        patch(
            "app.api.v1.endpoints.meal_plans.MealPlanRepository"
        ) as MockRepo,
        patch(
            "app.api.v1.endpoints.meal_plans.UserRepository"
        ) as MockUserRepo,
        patch(
            "app.services.recipe_creation_service.create_recipes_for_meal_plan",
            new_callable=AsyncMock,
            side_effect=Exception("DB connection failed"),
        ),
    ):
        mock_ai_instance = MockAI.return_value
        mock_ai_instance.generate_meal_plan = AsyncMock(return_value=mock_generated)

        mock_repo_instance = MockRepo.return_value
        mock_repo_instance.deactivate_old_plans = AsyncMock(return_value=0)
        mock_repo_instance.create = AsyncMock(return_value=mock_plan)

        mock_user_repo_instance = MockUserRepo.return_value
        mock_user_repo_instance.get_preferences = AsyncMock(
            return_value={"cuisine_preferences": ["south"]}
        )

        response = await mp_client.post(
            "/api/v1/meal-plans/generate",
            json={"week_start_date": date.today().isoformat()},
        )

    # Plan is still created even though recipe creation failed
    assert response.status_code == 200
    data = response.json()
    assert data["id"] == plan_id


@pytest.mark.asyncio
async def test_generate_unauthorized(unauthenticated_client: AsyncClient):
    """POST /generate returns 401 without auth."""
    response = await unauthenticated_client.post(
        "/api/v1/meal-plans/generate",
        json={"week_start_date": date.today().isoformat()},
    )
    assert response.status_code == 401


# ==================== GET /current Tests ====================


@pytest.mark.asyncio
async def test_get_current_plan(mp_client: AsyncClient, mp_user: User):
    """GET /current returns active plan for current week."""
    mock_plan = _make_plan(user_id=mp_user.id)

    with patch(
        "app.api.v1.endpoints.meal_plans.MealPlanRepository"
    ) as MockRepo:
        mock_repo_instance = MockRepo.return_value
        mock_repo_instance.get_current_for_user = AsyncMock(return_value=mock_plan)

        response = await mp_client.get("/api/v1/meal-plans/current")

    assert response.status_code == 200
    data = response.json()
    assert len(data["days"]) == 7
    assert "id" in data


@pytest.mark.asyncio
async def test_get_current_plan_not_found(mp_client: AsyncClient):
    """GET /current returns 404 when no plan exists."""
    with patch(
        "app.api.v1.endpoints.meal_plans.MealPlanRepository"
    ) as MockRepo:
        mock_repo_instance = MockRepo.return_value
        mock_repo_instance.get_current_for_user = AsyncMock(return_value=None)

        response = await mp_client.get("/api/v1/meal-plans/current")

    assert response.status_code == 404


@pytest.mark.asyncio
async def test_get_current_unauthorized(unauthenticated_client: AsyncClient):
    """GET /current returns 401 without auth."""
    response = await unauthenticated_client.get("/api/v1/meal-plans/current")
    assert response.status_code == 401


# ==================== GET /{plan_id} Tests ====================


@pytest.mark.asyncio
async def test_get_plan_by_id(mp_client: AsyncClient, mp_user: User):
    """GET /{plan_id} returns specific plan."""
    plan_id = str(uuid4())
    mock_plan = _make_plan(plan_id=plan_id, user_id=mp_user.id)

    with patch(
        "app.api.v1.endpoints.meal_plans.MealPlanRepository"
    ) as MockRepo:
        mock_repo_instance = MockRepo.return_value
        mock_repo_instance.get_by_id = AsyncMock(return_value=mock_plan)

        response = await mp_client.get(f"/api/v1/meal-plans/{plan_id}")

    assert response.status_code == 200
    assert response.json()["id"] == plan_id


@pytest.mark.asyncio
async def test_get_plan_by_id_not_found(mp_client: AsyncClient):
    """GET /{plan_id} returns 404 for invalid ID."""
    with patch(
        "app.api.v1.endpoints.meal_plans.MealPlanRepository"
    ) as MockRepo:
        mock_repo_instance = MockRepo.return_value
        mock_repo_instance.get_by_id = AsyncMock(return_value=None)

        response = await mp_client.get(f"/api/v1/meal-plans/{uuid4()}")

    assert response.status_code == 404


@pytest.mark.asyncio
async def test_get_plan_by_id_wrong_user(mp_client: AsyncClient, mp_user: User):
    """GET /{plan_id} returns 404 for another user's plan."""
    plan_id = str(uuid4())
    mock_plan = _make_plan(plan_id=plan_id, user_id="other-user-id")

    with patch(
        "app.api.v1.endpoints.meal_plans.MealPlanRepository"
    ) as MockRepo:
        mock_repo_instance = MockRepo.return_value
        mock_repo_instance.get_by_id = AsyncMock(return_value=mock_plan)

        response = await mp_client.get(f"/api/v1/meal-plans/{plan_id}")

    assert response.status_code == 404


@pytest.mark.asyncio
async def test_get_plan_unauthorized(unauthenticated_client: AsyncClient):
    """GET /{plan_id} returns 401 without auth."""
    response = await unauthenticated_client.get(f"/api/v1/meal-plans/{uuid4()}")
    assert response.status_code == 401


# ==================== POST /swap Tests ====================


@pytest.mark.asyncio
async def test_swap_item_with_specific_recipe(mp_client: AsyncClient, mp_user: User):
    """POST /swap replaces item with specified recipe."""
    plan_id = str(uuid4())
    mock_plan = _make_plan(plan_id=plan_id, user_id=mp_user.id)
    item_id = mock_plan["days"][0]["meals"]["lunch"][0]["id"]
    new_recipe_id = str(uuid4())

    # After swap, the plan is returned updated
    updated_plan = _make_plan(plan_id=plan_id, user_id=mp_user.id)

    new_recipe = {
        "id": new_recipe_id,
        "name": "Paneer Tikka",
        "image_url": None,
        "prep_time_minutes": 25,
        "nutrition": {"calories": 300},
        "dietary_tags": ["vegetarian"],
    }

    with (
        patch(
            "app.api.v1.endpoints.meal_plans.MealPlanRepository"
        ) as MockRepo,
        patch(
            "app.api.v1.endpoints.meal_plans.RecipeRepository"
        ) as MockRecipeRepo,
    ):
        mock_repo = MockRepo.return_value
        mock_repo.get_by_id = AsyncMock(return_value=mock_plan)
        mock_repo.update = AsyncMock(return_value=updated_plan)

        mock_recipe_repo = MockRecipeRepo.return_value
        mock_recipe_repo.get_by_id = AsyncMock(return_value=new_recipe)

        response = await mp_client.post(
            f"/api/v1/meal-plans/{plan_id}/items/{item_id}/swap",
            json={"specific_recipe_id": new_recipe_id},
        )

    assert response.status_code == 200


@pytest.mark.asyncio
async def test_swap_item_random_recipe(mp_client: AsyncClient, mp_user: User):
    """POST /swap replaces item with random alternative."""
    plan_id = str(uuid4())
    mock_plan = _make_plan(plan_id=plan_id, user_id=mp_user.id)
    item_id = mock_plan["days"][0]["meals"]["lunch"][0]["id"]

    updated_plan = _make_plan(plan_id=plan_id, user_id=mp_user.id)

    random_recipes = [
        {"id": str(uuid4()), "name": "Alt Recipe 1", "image_url": None,
         "prep_time_minutes": 20, "nutrition": {"calories": 200},
         "dietary_tags": ["vegetarian"]},
        {"id": str(uuid4()), "name": "Alt Recipe 2", "image_url": None,
         "prep_time_minutes": 30, "nutrition": None,
         "dietary_tags": ["vegetarian"]},
    ]

    with (
        patch(
            "app.api.v1.endpoints.meal_plans.MealPlanRepository"
        ) as MockRepo,
        patch(
            "app.api.v1.endpoints.meal_plans.RecipeRepository"
        ) as MockRecipeRepo,
    ):
        mock_repo = MockRepo.return_value
        mock_repo.get_by_id = AsyncMock(return_value=mock_plan)
        mock_repo.update = AsyncMock(return_value=updated_plan)

        mock_recipe_repo = MockRecipeRepo.return_value
        mock_recipe_repo.search = AsyncMock(return_value=random_recipes)

        response = await mp_client.post(
            f"/api/v1/meal-plans/{plan_id}/items/{item_id}/swap",
            json={},
        )

    assert response.status_code == 200


@pytest.mark.asyncio
async def test_swap_item_locked_fails(mp_client: AsyncClient, mp_user: User):
    """POST /swap returns 404 when item is locked."""
    plan_id = str(uuid4())
    locked_item_id = str(uuid4())
    mock_plan = _make_plan(
        plan_id=plan_id,
        user_id=mp_user.id,
        locked_item_id=locked_item_id,
    )

    with patch(
        "app.api.v1.endpoints.meal_plans.MealPlanRepository"
    ) as MockRepo:
        mock_repo = MockRepo.return_value
        mock_repo.get_by_id = AsyncMock(return_value=mock_plan)

        response = await mp_client.post(
            f"/api/v1/meal-plans/{plan_id}/items/{locked_item_id}/swap",
            json={},
        )

    assert response.status_code == 404
    assert "locked" in response.json()["detail"].lower()


@pytest.mark.asyncio
async def test_swap_item_not_found(mp_client: AsyncClient, mp_user: User):
    """POST /swap returns 404 for invalid item_id."""
    plan_id = str(uuid4())
    mock_plan = _make_plan(plan_id=plan_id, user_id=mp_user.id)

    with patch(
        "app.api.v1.endpoints.meal_plans.MealPlanRepository"
    ) as MockRepo:
        mock_repo = MockRepo.return_value
        mock_repo.get_by_id = AsyncMock(return_value=mock_plan)

        response = await mp_client.post(
            f"/api/v1/meal-plans/{plan_id}/items/nonexistent-item/swap",
            json={},
        )

    assert response.status_code == 404


@pytest.mark.asyncio
async def test_swap_item_wrong_user(mp_client: AsyncClient, mp_user: User):
    """POST /swap returns 404 for another user's plan."""
    plan_id = str(uuid4())
    mock_plan = _make_plan(plan_id=plan_id, user_id="other-user")

    with patch(
        "app.api.v1.endpoints.meal_plans.MealPlanRepository"
    ) as MockRepo:
        mock_repo = MockRepo.return_value
        mock_repo.get_by_id = AsyncMock(return_value=mock_plan)

        response = await mp_client.post(
            f"/api/v1/meal-plans/{plan_id}/items/{uuid4()}/swap",
            json={},
        )

    assert response.status_code == 404


@pytest.mark.asyncio
async def test_swap_unauthorized(unauthenticated_client: AsyncClient):
    """POST /swap returns 401 without auth."""
    response = await unauthenticated_client.post(
        f"/api/v1/meal-plans/{uuid4()}/items/{uuid4()}/swap",
        json={},
    )
    assert response.status_code == 401


# ==================== PUT /lock Tests ====================


@pytest.mark.asyncio
async def test_lock_item(mp_client: AsyncClient, mp_user: User):
    """PUT /lock sets is_locked=True."""
    plan_id = str(uuid4())
    mock_plan = _make_plan(plan_id=plan_id, user_id=mp_user.id)
    item_id = mock_plan["days"][0]["meals"]["breakfast"][0]["id"]

    updated_plan = _make_plan(plan_id=plan_id, user_id=mp_user.id)

    with patch(
        "app.api.v1.endpoints.meal_plans.MealPlanRepository"
    ) as MockRepo:
        mock_repo = MockRepo.return_value
        mock_repo.get_by_id = AsyncMock(return_value=mock_plan)
        mock_repo.update = AsyncMock(return_value=updated_plan)

        response = await mp_client.put(
            f"/api/v1/meal-plans/{plan_id}/items/{item_id}/lock"
        )

    assert response.status_code == 200


@pytest.mark.asyncio
async def test_unlock_item(mp_client: AsyncClient, mp_user: User):
    """PUT /lock toggles is_locked back to False."""
    plan_id = str(uuid4())
    locked_item_id = str(uuid4())
    mock_plan = _make_plan(
        plan_id=plan_id,
        user_id=mp_user.id,
        locked_item_id=locked_item_id,
    )

    updated_plan = _make_plan(plan_id=plan_id, user_id=mp_user.id)

    with patch(
        "app.api.v1.endpoints.meal_plans.MealPlanRepository"
    ) as MockRepo:
        mock_repo = MockRepo.return_value
        mock_repo.get_by_id = AsyncMock(return_value=mock_plan)
        mock_repo.update = AsyncMock(return_value=updated_plan)

        response = await mp_client.put(
            f"/api/v1/meal-plans/{plan_id}/items/{locked_item_id}/lock"
        )

    assert response.status_code == 200


@pytest.mark.asyncio
async def test_lock_item_not_found(mp_client: AsyncClient, mp_user: User):
    """PUT /lock returns 404 for invalid item_id."""
    plan_id = str(uuid4())
    mock_plan = _make_plan(plan_id=plan_id, user_id=mp_user.id)

    with patch(
        "app.api.v1.endpoints.meal_plans.MealPlanRepository"
    ) as MockRepo:
        mock_repo = MockRepo.return_value
        mock_repo.get_by_id = AsyncMock(return_value=mock_plan)

        response = await mp_client.put(
            f"/api/v1/meal-plans/{plan_id}/items/nonexistent/lock"
        )

    assert response.status_code == 404


@pytest.mark.asyncio
async def test_lock_unauthorized(unauthenticated_client: AsyncClient):
    """PUT /lock returns 401 without auth."""
    response = await unauthenticated_client.put(
        f"/api/v1/meal-plans/{uuid4()}/items/{uuid4()}/lock"
    )
    assert response.status_code == 401


# ==================== DELETE /remove Tests ====================


@pytest.mark.asyncio
async def test_remove_item(mp_client: AsyncClient, mp_user: User):
    """DELETE /remove removes item from days array."""
    plan_id = str(uuid4())
    mock_plan = _make_plan(plan_id=plan_id, user_id=mp_user.id)
    item_id = mock_plan["days"][0]["meals"]["snacks"][0]["id"]

    updated_plan = _make_plan(plan_id=plan_id, user_id=mp_user.id)

    with patch(
        "app.api.v1.endpoints.meal_plans.MealPlanRepository"
    ) as MockRepo:
        mock_repo = MockRepo.return_value
        mock_repo.get_by_id = AsyncMock(return_value=mock_plan)
        mock_repo.update = AsyncMock(return_value=updated_plan)

        response = await mp_client.delete(
            f"/api/v1/meal-plans/{plan_id}/items/{item_id}"
        )

    assert response.status_code == 200


@pytest.mark.asyncio
async def test_remove_locked_item_fails(mp_client: AsyncClient, mp_user: User):
    """DELETE /remove returns 404 when item is locked."""
    plan_id = str(uuid4())
    locked_item_id = str(uuid4())
    mock_plan = _make_plan(
        plan_id=plan_id,
        user_id=mp_user.id,
        locked_item_id=locked_item_id,
    )

    with patch(
        "app.api.v1.endpoints.meal_plans.MealPlanRepository"
    ) as MockRepo:
        mock_repo = MockRepo.return_value
        mock_repo.get_by_id = AsyncMock(return_value=mock_plan)

        response = await mp_client.delete(
            f"/api/v1/meal-plans/{plan_id}/items/{locked_item_id}"
        )

    assert response.status_code == 404
    assert "locked" in response.json()["detail"].lower()


@pytest.mark.asyncio
async def test_remove_item_not_found(mp_client: AsyncClient, mp_user: User):
    """DELETE /remove returns 404 for invalid item_id."""
    plan_id = str(uuid4())
    mock_plan = _make_plan(plan_id=plan_id, user_id=mp_user.id)

    with patch(
        "app.api.v1.endpoints.meal_plans.MealPlanRepository"
    ) as MockRepo:
        mock_repo = MockRepo.return_value
        mock_repo.get_by_id = AsyncMock(return_value=mock_plan)

        response = await mp_client.delete(
            f"/api/v1/meal-plans/{plan_id}/items/nonexistent"
        )

    assert response.status_code == 404


@pytest.mark.asyncio
async def test_remove_unauthorized(unauthenticated_client: AsyncClient):
    """DELETE /remove returns 401 without auth."""
    response = await unauthenticated_client.delete(
        f"/api/v1/meal-plans/{uuid4()}/items/{uuid4()}"
    )
    assert response.status_code == 401
