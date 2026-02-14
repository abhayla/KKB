"""Tests for family-aware meal generation.

Verifies that the AI meal service includes family member health conditions
and dietary restrictions in the Gemini prompt for personalized meal planning.
"""

import pytest
from datetime import date, timedelta

from app.services.ai_meal_service import AIMealService, UserPreferences


@pytest.fixture
def service():
    """Create AIMealService instance."""
    return AIMealService()


@pytest.fixture
def week_start():
    """Next Monday."""
    today = date.today()
    return today - timedelta(days=today.weekday()) + timedelta(weeks=1)


@pytest.fixture
def base_prefs():
    """Base preferences without family members."""
    return UserPreferences(
        dietary_tags=["vegetarian"],
        cuisine_preferences=["north"],
        family_size=6,
    )


class TestFamilyAwareMealGeneration:
    """Test family member data is included in AI prompt."""

    def test_prompt_includes_family_member_names(self, service, base_prefs, week_start):
        """Family member names appear in the prompt."""
        base_prefs.family_members = [
            {"name": "Dadaji", "age_group": "senior", "health_conditions": ["diabetic"], "dietary_restrictions": ["vegetarian"]},
            {"name": "Chhoti", "age_group": "child", "health_conditions": ["no_spicy"], "dietary_restrictions": []},
        ]
        prompt = service._build_prompt(base_prefs, {}, None, week_start)

        assert "Dadaji" in prompt
        assert "Chhoti" in prompt

    def test_prompt_includes_health_conditions(self, service, base_prefs, week_start):
        """Health conditions appear in the prompt."""
        base_prefs.family_members = [
            {"name": "Grandfather", "age_group": "senior", "health_conditions": ["diabetic", "soft_food"], "dietary_restrictions": []},
        ]
        prompt = service._build_prompt(base_prefs, {}, None, week_start)

        assert "diabetic" in prompt
        assert "soft_food" in prompt

    def test_prompt_includes_per_member_dietary_restrictions(self, service, base_prefs, week_start):
        """Per-member dietary restrictions appear in the prompt."""
        base_prefs.family_members = [
            {"name": "Dadiji", "age_group": "senior", "health_conditions": [], "dietary_restrictions": ["jain"]},
        ]
        prompt = service._build_prompt(base_prefs, {}, None, week_start)

        assert "jain" in prompt
        assert "Dadiji" in prompt

    def test_empty_family_members_no_family_section(self, service, base_prefs, week_start):
        """Empty family members list produces no member-specific section."""
        base_prefs.family_members = []
        prompt = service._build_prompt(base_prefs, {}, None, week_start)

        assert "No individual family members specified" in prompt

    def test_senior_diabetic_constraints(self, service, base_prefs, week_start):
        """Senior + diabetic member produces appropriate constraints in prompt."""
        base_prefs.family_members = [
            {"name": "Dadaji", "age_group": "senior", "health_conditions": ["diabetic", "soft_food"], "dietary_restrictions": ["vegetarian", "low_salt"]},
        ]
        prompt = service._build_prompt(base_prefs, {}, None, week_start)

        assert "Dadaji" in prompt
        assert "senior" in prompt
        assert "diabetic" in prompt
        assert "soft_food" in prompt
        # Prompt should contain guidance about diabetic meals
        assert "sugar" in prompt.lower() or "diabetic" in prompt.lower()

    def test_child_no_spicy_constraints(self, service, base_prefs, week_start):
        """Child + no_spicy member produces appropriate constraints."""
        base_prefs.family_members = [
            {"name": "Chhoti", "age_group": "child", "health_conditions": ["no_spicy", "soft_food"], "dietary_restrictions": []},
        ]
        prompt = service._build_prompt(base_prefs, {}, None, week_start)

        assert "Chhoti" in prompt
        assert "child" in prompt
        assert "no_spicy" in prompt

    def test_multiple_family_members_all_included(self, service, base_prefs, week_start):
        """All 6 family members appear in prompt."""
        base_prefs.family_members = [
            {"name": "Dadaji", "age_group": "senior", "health_conditions": ["diabetic"], "dietary_restrictions": ["vegetarian"]},
            {"name": "Dadiji", "age_group": "senior", "health_conditions": ["low_salt"], "dietary_restrictions": ["jain"]},
            {"name": "Papa", "age_group": "adult", "health_conditions": ["high_protein"], "dietary_restrictions": []},
            {"name": "Mummy", "age_group": "adult", "health_conditions": ["low_oil"], "dietary_restrictions": ["vegetarian"]},
            {"name": "Rahul", "age_group": "adult", "health_conditions": [], "dietary_restrictions": []},
            {"name": "Chhoti", "age_group": "child", "health_conditions": ["no_spicy"], "dietary_restrictions": ["vegetarian"]},
        ]
        prompt = service._build_prompt(base_prefs, {}, None, week_start)

        for name in ["Dadaji", "Dadiji", "Papa", "Mummy", "Rahul", "Chhoti"]:
            assert name in prompt, f"Family member {name} not found in prompt"

    def test_family_member_without_health_conditions(self, service, base_prefs, week_start):
        """Member without health conditions still appears (age_group only)."""
        base_prefs.family_members = [
            {"name": "Rahul", "age_group": "adult", "health_conditions": [], "dietary_restrictions": []},
        ]
        prompt = service._build_prompt(base_prefs, {}, None, week_start)

        assert "Rahul" in prompt
        assert "adult" in prompt

    def test_family_members_field_defaults_to_empty_list(self):
        """UserPreferences.family_members defaults to empty list."""
        prefs = UserPreferences()
        assert prefs.family_members == []
