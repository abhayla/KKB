"""Tests for PreferenceUpdateService pure helpers.

Covers the class's private helpers (name-prefixed with underscore) that
are pure functions over dicts — no DB I/O.

The async update_* methods perform DB round-trips on Firestore and are
covered by the existing test_preference_service.py / chat integration tests.
"""

import pytest

from app.services.preference_update_service import PreferenceUpdateService


@pytest.fixture
def service():
    return PreferenceUpdateService()


# ==================== _find_existing_rule ====================


class TestFindExistingRule:
    def test_returns_none_when_target_absent(self, service):
        rules = [{"target": "paneer", "type": "INCLUDE"}]
        assert service._find_existing_rule(rules, "onion") is None

    def test_returns_matching_rule_case_insensitively(self, service):
        rules = [{"target": "Paneer", "type": "INCLUDE"}]
        result = service._find_existing_rule(rules, "PANEER")
        assert result is not None
        assert result["target"] == "Paneer"

    def test_empty_list_returns_none(self, service):
        assert service._find_existing_rule([], "paneer") is None

    def test_rule_type_filter_matches(self, service):
        rules = [
            {"target": "onion", "type": "INCLUDE"},
            {"target": "onion", "type": "EXCLUDE"},
        ]
        result = service._find_existing_rule(rules, "onion", rule_type="EXCLUDE")
        assert result["type"] == "EXCLUDE"

    def test_rule_type_filter_skips_wrong_type(self, service):
        rules = [{"target": "onion", "type": "INCLUDE"}]
        # Looking for EXCLUDE, only INCLUDE present -> None.
        result = service._find_existing_rule(rules, "onion", rule_type="EXCLUDE")
        assert result is None


# ==================== _check_rule_conflict_nested ====================


class TestCheckRuleConflictNested:
    def test_no_conflict_when_lists_empty(self, service):
        assert service._check_rule_conflict_nested([], {"target": "paneer", "type": "INCLUDE"}) is None

    def test_no_conflict_when_targets_differ(self, service):
        opposite = [{"target": "onion", "type": "EXCLUDE"}]
        new = {"target": "paneer", "type": "INCLUDE"}
        assert service._check_rule_conflict_nested(opposite, new) is None

    def test_detects_include_vs_exclude_on_same_target(self, service):
        opposite = [{"target": "onion", "type": "EXCLUDE", "reason": "allergy"}]
        new = {"target": "onion", "type": "INCLUDE"}
        conflict = service._check_rule_conflict_nested(opposite, new)

        assert conflict is not None
        assert conflict["conflict_type"] == "include_exclude"
        assert conflict["existing_rule"]["target"] == "onion"
        assert "onion" in conflict["message"].lower()

    def test_case_insensitive_target_match(self, service):
        opposite = [{"target": "Onion", "type": "EXCLUDE"}]
        new = {"target": "ONION", "type": "INCLUDE"}
        conflict = service._check_rule_conflict_nested(opposite, new)
        assert conflict is not None


# ==================== _summarize_rules ====================


class TestSummarizeRules:
    def test_handles_legacy_flat_list(self, service):
        """Legacy format: flat list with 'type' field on each rule."""
        rules = [
            {"target": "paneer", "type": "INCLUDE", "frequency": "WEEKLY", "meal_slot": "DINNER"},
            {"target": "onion", "type": "EXCLUDE", "reason": "allergy"},
        ]
        summary = service._summarize_rules(rules)

        assert summary["total"] == 2
        assert len(summary["include"]) == 1
        assert summary["include"][0]["target"] == "paneer"
        assert summary["include"][0]["frequency"] == "WEEKLY"
        assert summary["include"][0]["meal_slots"] == "DINNER"
        assert len(summary["exclude"]) == 1
        assert summary["exclude"][0]["target"] == "onion"
        assert summary["exclude"][0]["reason"] == "allergy"

    def test_handles_nested_dict_format(self, service):
        rules = {
            "include": [{"target": "paneer", "frequency": "DAILY"}],
            "exclude": [{"target": "onion", "reason": "dislike"}],
        }
        summary = service._summarize_rules(rules)

        assert summary["total"] == 2
        assert summary["include"][0]["target"] == "paneer"
        assert summary["exclude"][0]["target"] == "onion"

    def test_empty_rules_produces_zero_total(self, service):
        assert service._summarize_rules({"include": [], "exclude": []})["total"] == 0
        assert service._summarize_rules([])["total"] == 0


# ==================== _summarize_preferences ====================


class TestSummarizePreferences:
    def test_maps_all_known_keys(self, service):
        prefs = {
            "dietary_tags": ["vegetarian"],
            "cuisine_preferences": ["north", "south"],
            "allergies": ["peanuts"],
            "disliked_ingredients": ["karela"],
            "weekday_cooking_time_minutes": 25,
            "weekend_cooking_time_minutes": 55,
            "busy_days": ["MONDAY", "WEDNESDAY"],
        }
        summary = service._summarize_preferences(prefs)

        assert summary["dietary_tags"] == ["vegetarian"]
        assert summary["cuisine_preferences"] == ["north", "south"]
        assert summary["allergies"] == ["peanuts"]
        assert summary["dislikes"] == ["karela"]
        assert summary["cooking_time"] == {"weekday": 25, "weekend": 55}
        assert summary["busy_days"] == ["MONDAY", "WEDNESDAY"]

    def test_empty_prefs_applies_defaults(self, service):
        summary = service._summarize_preferences({})
        assert summary["dietary_tags"] == []
        assert summary["cuisine_preferences"] == []
        assert summary["cooking_time"] == {"weekday": 30, "weekend": 60}
        assert summary["busy_days"] == []


# ==================== _get_pref_key ====================


class TestGetPrefKey:
    @pytest.mark.parametrize(
        "pref_type,expected_key",
        [
            ("cooking_time", "weekday_cooking_time_minutes"),
            ("busy_days", "busy_days"),
            ("dietary_tags", "dietary_tags"),
            ("spice_level", "spice_level"),
            ("cuisine", "cuisine_preferences"),
        ],
    )
    def test_known_types_mapped_correctly(self, service, pref_type, expected_key):
        assert service._get_pref_key(pref_type) == expected_key

    def test_unknown_type_falls_through(self, service):
        """Unknown preference types pass through unchanged."""
        assert service._get_pref_key("unknown_xyz") == "unknown_xyz"
