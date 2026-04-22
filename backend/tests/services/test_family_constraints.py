"""Tests for family_constraints.get_family_forbidden_keywords.

Pure function: takes a list of family-member dicts, returns a dict
mapping member names to their union of forbidden ingredient keywords.
"""

import pytest

from app.services.family_constraints import (
    FAMILY_CONSTRAINT_MAP,
    get_family_forbidden_keywords,
)


class TestGetFamilyForbiddenKeywords:
    def test_empty_input_returns_empty_dict(self):
        assert get_family_forbidden_keywords([]) == {}

    def test_member_with_no_restrictions_is_excluded(self):
        """A member with no matching health/diet entries should NOT appear in
        the result (forbidden set would be empty)."""
        members = [{"name": "Alice", "health_conditions": [], "dietary_restrictions": []}]
        assert get_family_forbidden_keywords(members) == {}

    def test_jain_member_gets_jain_forbidden_set(self):
        members = [{"name": "Amit", "dietary_restrictions": ["jain"]}]
        result = get_family_forbidden_keywords(members)

        assert "Amit" in result
        # jain forbids onion + Hindi aliases
        assert "onion" in result["Amit"]
        assert "pyaaz" in result["Amit"]
        assert "garlic" in result["Amit"]
        assert "lahsun" in result["Amit"]

    def test_case_insensitive_matching(self):
        """Restriction strings are normalised to lowercase before lookup."""
        members = [{"name": "Maya", "dietary_restrictions": ["JAIN"]}]
        result = get_family_forbidden_keywords(members)
        assert "Maya" in result
        assert "onion" in result["Maya"]

    def test_health_conditions_and_restrictions_are_unioned(self):
        """If a member has both a health condition and a dietary restriction,
        their forbidden set is the union of both."""
        members = [{
            "name": "Priya",
            "health_conditions": ["diabetic"],
            "dietary_restrictions": ["vegan"],
        }]
        result = get_family_forbidden_keywords(members)

        assert "sugar" in result["Priya"]  # from diabetic
        assert "milk" in result["Priya"]   # from vegan
        assert "paneer" in result["Priya"] # from vegan

    def test_unknown_constraint_is_ignored(self):
        """Constraints that don't appear in FAMILY_CONSTRAINT_MAP are skipped
        (not an error)."""
        members = [{
            "name": "Ravi",
            "health_conditions": ["unknown_condition"],
            "dietary_restrictions": ["also_unknown"],
        }]
        assert get_family_forbidden_keywords(members) == {}

    def test_multiple_members_kept_separate(self):
        members = [
            {"name": "Amit", "dietary_restrictions": ["jain"]},
            {"name": "Priya", "dietary_restrictions": ["vegan"]},
            {"name": "Ravi"},  # no restrictions -> absent
        ]
        result = get_family_forbidden_keywords(members)

        assert set(result.keys()) == {"Amit", "Priya"}
        assert "onion" in result["Amit"]
        assert "onion" not in result["Priya"]  # vegan doesn't forbid onion
        assert "milk" in result["Priya"]
        assert "milk" not in result["Amit"]  # jain doesn't forbid milk

    def test_member_without_name_uses_default_member(self):
        """Members without an explicit name fall back to 'Member'."""
        members = [{"dietary_restrictions": ["jain"]}]
        result = get_family_forbidden_keywords(members)
        assert "Member" in result

    @pytest.mark.parametrize(
        "constraint",
        ["jain", "sattvic", "vegan", "diabetic", "low_salt", "no_spicy", "soft_food", "low_oil"],
    )
    def test_every_mapped_constraint_produces_non_empty_set(self, constraint):
        """Sanity check on the map: every declared constraint must have at
        least one keyword — otherwise the map entry is dead code."""
        assert len(FAMILY_CONSTRAINT_MAP[constraint]) > 0

    def test_hindi_aliases_are_present_for_jain(self):
        """Regression guard: Hindi aliases must stay in the jain set so that
        'Aloo Paratha' gets caught by downstream keyword matching."""
        jain = FAMILY_CONSTRAINT_MAP["jain"]
        assert "aloo" in jain
        assert "pyaaz" in jain
        assert "lahsun" in jain
