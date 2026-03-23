"""Unicode and Hindi text handling tests for text-based endpoints.

Verifies that the API gracefully handles non-ASCII input including Hindi
(Devanagari), mixed scripts, emoji, special characters, and edge cases.
These are boundary tests — they verify no 500 errors occur, not that
specific results are found (the test DB may not have Hindi-named recipes).
"""

import uuid

import pytest
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.recipe import Recipe


async def _create_recipe(
    db: AsyncSession,
    name: str,
    description: str = "",
    cuisine_type: str = "NORTH",
) -> Recipe:
    """Create a test recipe with the given name and description."""
    recipe = Recipe(
        id=str(uuid.uuid4()),
        name=name,
        description=description or f"A delicious {name}",
        cuisine_type=cuisine_type,
        dietary_tags=["VEGETARIAN"],
        meal_types=["LUNCH"],
        prep_time_minutes=20,
        cook_time_minutes=15,
        total_time_minutes=35,
        servings=2,
    )
    db.add(recipe)
    await db.commit()
    await db.refresh(recipe)
    return recipe


# ==================== Recipe Search — Hindi / Devanagari ====================


@pytest.mark.asyncio
async def test_search_recipes_hindi_name(
    client: AsyncClient, db_session: AsyncSession
):
    """Search with Hindi query 'पनीर' returns 200 and valid JSON."""
    await _create_recipe(db_session, name="पनीर बटर मसाला", description="Paneer dish")

    response = await client.get(
        "/api/v1/recipes/search", params={"q": "पनीर"}
    )

    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, list)
    # The Hindi-named recipe should match via ilike
    assert any("पनीर" in r["name"] for r in data)


@pytest.mark.asyncio
async def test_search_recipes_hindi_description(
    client: AsyncClient, db_session: AsyncSession
):
    """Search with Hindi query 'दाल' matching a description returns 200."""
    await _create_recipe(
        db_session,
        name="Yellow Dal",
        description="पीली दाल — एक स्वादिष्ट दाल",
    )

    response = await client.get(
        "/api/v1/recipes/search", params={"q": "दाल"}
    )

    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, list)
    # Should match via description ilike
    assert any("दाल" in r.get("description", "") for r in data)


@pytest.mark.asyncio
async def test_search_recipes_mixed_unicode(
    client: AsyncClient, db_session: AsyncSession
):
    """Search with mixed English + Hindi 'Paneer पनीर' returns 200."""
    await _create_recipe(db_session, name="Paneer पनीर Tikka")

    response = await client.get(
        "/api/v1/recipes/search", params={"q": "Paneer पनीर"}
    )

    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, list)


@pytest.mark.asyncio
async def test_search_recipes_devanagari_digits(
    client: AsyncClient, db_session: AsyncSession
):
    """Search with Devanagari digits '२ कप चावल' returns 200 without crash."""
    await _create_recipe(db_session, name="चावल", description="२ कप चावल बनाएं")

    response = await client.get(
        "/api/v1/recipes/search", params={"q": "२ कप चावल"}
    )

    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, list)


# ==================== Recipe Search — Emoji and Special Characters ====================


@pytest.mark.asyncio
async def test_search_recipes_emoji(
    client: AsyncClient, db_session: AsyncSession
):
    """Search with emoji '🍛 curry' returns 200 without crash."""
    await _create_recipe(db_session, name="Chicken Curry")

    response = await client.get(
        "/api/v1/recipes/search", params={"q": "🍛 curry"}
    )

    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, list)


@pytest.mark.asyncio
async def test_search_recipes_special_chars(
    client: AsyncClient, db_session: AsyncSession
):
    """Search with apostrophe and ampersand returns 200 without crash."""
    await _create_recipe(db_session, name="Dal's Special")
    await _create_recipe(db_session, name="Rice & Beans")

    # Apostrophe
    resp1 = await client.get(
        "/api/v1/recipes/search", params={"q": "dal's"}
    )
    assert resp1.status_code == 200
    assert isinstance(resp1.json(), list)

    # Ampersand
    resp2 = await client.get(
        "/api/v1/recipes/search", params={"q": "rice & beans"}
    )
    assert resp2.status_code == 200
    assert isinstance(resp2.json(), list)


# ==================== Recipe Search — Edge Cases ====================


@pytest.mark.asyncio
async def test_search_empty_string(
    client: AsyncClient, db_session: AsyncSession
):
    """Empty query string returns all recipes (paginated default)."""
    await _create_recipe(db_session, name="Recipe A")
    await _create_recipe(db_session, name="Recipe B")

    response = await client.get(
        "/api/v1/recipes/search", params={"q": ""}
    )

    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, list)
    assert len(data) >= 2


@pytest.mark.asyncio
async def test_search_very_long_string(
    client: AsyncClient, db_session: AsyncSession
):
    """1000-character query does not crash — returns empty list gracefully."""
    long_query = "a" * 1000

    response = await client.get(
        "/api/v1/recipes/search", params={"q": long_query}
    )

    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, list)
    assert len(data) == 0


# ==================== Family Members — Unicode Names ====================


@pytest.mark.asyncio
async def test_create_family_member_hindi_name(client: AsyncClient):
    """Create a family member with Hindi name 'राम शर्मा' succeeds."""
    response = await client.post(
        "/api/v1/family-members",
        json={
            "name": "राम शर्मा",
            "age_group": "adult",
            "dietary_restrictions": [],
            "health_conditions": [],
        },
    )

    assert response.status_code == 201
    data = response.json()
    assert data["name"] == "राम शर्मा"
    assert "id" in data


@pytest.mark.asyncio
async def test_create_family_member_emoji_name(client: AsyncClient):
    """Create a family member with emoji in name does not crash."""
    response = await client.post(
        "/api/v1/family-members",
        json={
            "name": "Papa 👨‍🍳",
            "age_group": "adult",
            "dietary_restrictions": [],
            "health_conditions": [],
        },
    )

    assert response.status_code == 201
    data = response.json()
    assert "Papa" in data["name"]


@pytest.mark.asyncio
async def test_create_family_member_mixed_script_name(client: AsyncClient):
    """Create a family member with mixed Devanagari + Latin name succeeds."""
    response = await client.post(
        "/api/v1/family-members",
        json={
            "name": "Aarav आरव",
            "age_group": "child",
            "dietary_restrictions": ["vegetarian"],
            "health_conditions": [],
        },
    )

    assert response.status_code == 201
    data = response.json()
    assert data["name"] == "Aarav आरव"


# ==================== Recipe Rules — Unicode Target Names ====================


@pytest.mark.asyncio
async def test_create_recipe_rule_hindi_target(client: AsyncClient):
    """Create a recipe rule with Hindi target_name 'पनीर बटर मसाला' succeeds."""
    response = await client.post(
        "/api/v1/recipe-rules",
        json={
            "target_type": "RECIPE",
            "action": "INCLUDE",
            "target_name": "पनीर बटर मसाला",
            "frequency_type": "TIMES_PER_WEEK",
            "frequency_count": 2,
            "enforcement": "PREFERRED",
        },
    )

    assert response.status_code == 201
    data = response.json()
    assert data["target_name"] == "पनीर बटर मसाला"
    assert "id" in data


@pytest.mark.asyncio
async def test_create_recipe_rule_special_chars_target(client: AsyncClient):
    """Create a recipe rule with special characters in target_name."""
    response = await client.post(
        "/api/v1/recipe-rules",
        json={
            "target_type": "INGREDIENT",
            "action": "EXCLUDE",
            "target_name": "Dal's & Tadka (Special)",
            "frequency_type": "NEVER",
            "enforcement": "REQUIRED",
        },
    )

    assert response.status_code == 201
    data = response.json()
    # target_name is stored as-is (only enum fields are uppercased)
    assert data["target_name"] == "Dal's & Tadka (Special)"


@pytest.mark.asyncio
async def test_recipe_rule_duplicate_check_hindi(client: AsyncClient):
    """Duplicate detection works correctly with Hindi target names."""
    rule_payload = {
        "target_type": "INGREDIENT",
        "action": "INCLUDE",
        "target_name": "दाल तड़का",
        "frequency_type": "DAILY",
        "enforcement": "REQUIRED",
    }

    # First creation should succeed
    resp1 = await client.post("/api/v1/recipe-rules", json=rule_payload)
    assert resp1.status_code == 201

    # Duplicate should return 409
    resp2 = await client.post("/api/v1/recipe-rules", json=rule_payload)
    assert resp2.status_code == 409


# ==================== AI Catalog Search — Unicode ====================


@pytest.mark.asyncio
async def test_ai_catalog_search_hindi(client: AsyncClient):
    """AI catalog search with Hindi query does not crash."""
    response = await client.get(
        "/api/v1/recipes/ai-catalog/search", params={"q": "बिरयानी"}
    )

    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, list)


@pytest.mark.asyncio
async def test_ai_catalog_search_emoji(client: AsyncClient):
    """AI catalog search with emoji does not crash."""
    response = await client.get(
        "/api/v1/recipes/ai-catalog/search", params={"q": "🍚 rice"}
    )

    assert response.status_code == 200
    data = response.json()
    assert isinstance(data, list)
