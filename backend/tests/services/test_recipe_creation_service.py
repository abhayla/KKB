"""Tests for recipe_creation_service pure helpers.

Covers _normalize_name and _generate_placeholder_instructions. The DB-backed
find_or_create_recipe / create_recipes_for_meal_plan paths are integration-
level and are covered by the AI meal generation tests.
"""

from app.services.recipe_creation_service import (
    _generate_placeholder_instructions,
    _normalize_name,
)


class TestNormalizeName:
    def test_lowercases(self):
        assert _normalize_name("Paneer Butter Masala") == "paneer butter masala"

    def test_strips_whitespace(self):
        assert _normalize_name("  Dal Tadka  ") == "dal tadka"

    def test_preserves_internal_whitespace(self):
        assert _normalize_name("  Aloo  Paratha  ") == "aloo  paratha"

    def test_empty_string(self):
        assert _normalize_name("") == ""

    def test_idempotent(self):
        once = _normalize_name("Samosa")
        assert _normalize_name(once) == once


class TestGeneratePlaceholderInstructions:
    def test_returns_four_steps(self):
        steps = _generate_placeholder_instructions("Dal Tadka", prep_time=30)
        assert len(steps) == 4

    def test_steps_are_numbered_sequentially(self):
        steps = _generate_placeholder_instructions("Dal Tadka", prep_time=30)
        assert [s["step_number"] for s in steps] == [1, 2, 3, 4]

    def test_recipe_name_embedded_in_instructions(self):
        steps = _generate_placeholder_instructions("Paneer Tikka", prep_time=30)
        joined = " ".join(s["instruction"] for s in steps)
        assert "Paneer Tikka" in joined

    def test_duration_scales_with_prep_time(self):
        short = _generate_placeholder_instructions("X", prep_time=20)
        long = _generate_placeholder_instructions("X", prep_time=60)
        # Step 1 duration ~ prep_time // 4; step 3 duration ~ prep_time // 2
        assert long[0]["duration_minutes"] > short[0]["duration_minutes"]
        assert long[2]["duration_minutes"] > short[2]["duration_minutes"]

    def test_duration_has_sensible_minimum_floor(self):
        """Very small prep_time values shouldn't produce zero/negative durations."""
        steps = _generate_placeholder_instructions("X", prep_time=2)
        for s in steps:
            assert s["duration_minutes"] >= 2

    def test_each_step_has_expected_keys(self):
        steps = _generate_placeholder_instructions("X", prep_time=30)
        for s in steps:
            assert set(s.keys()) == {"step_number", "instruction", "duration_minutes", "tips"}
