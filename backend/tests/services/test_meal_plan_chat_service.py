"""Tests for meal_plan_chat_service — pure helpers.

Covers parse_date. The async tool-calling functions (query_meals, swap_recipe,
add_recipe, remove_recipe) need substantial fixture setup and are covered by
the existing chat integration tests.
"""

from datetime import date, timedelta

import pytest

from app.services.meal_plan_chat_service import parse_date


class TestParseDate:
    def test_parses_today_keyword(self):
        result = parse_date("today")
        assert result == date.today()

    def test_today_is_case_insensitive(self):
        assert parse_date("TODAY") == date.today()
        assert parse_date("Today") == date.today()
        assert parse_date(" today ") == date.today()

    def test_parses_tomorrow_keyword(self):
        result = parse_date("tomorrow")
        assert result == date.today() + timedelta(days=1)

    def test_tomorrow_is_case_insensitive(self):
        assert parse_date("TOMORROW") == date.today() + timedelta(days=1)

    def test_parses_iso_format(self):
        assert parse_date("2026-05-11") == date(2026, 5, 11)

    def test_parses_iso_format_leap_day(self):
        assert parse_date("2024-02-29") == date(2024, 2, 29)

    def test_raises_on_invalid_format(self):
        with pytest.raises(ValueError, match="Invalid date format"):
            parse_date("not-a-date")

    def test_raises_on_us_format(self):
        # US date format (MM/DD/YYYY) is explicitly not supported.
        with pytest.raises(ValueError, match="Invalid date format"):
            parse_date("05/11/2026")

    def test_raises_on_partial_date(self):
        with pytest.raises(ValueError, match="Invalid date format"):
            parse_date("2026-05")

    def test_raises_on_empty_string(self):
        with pytest.raises(ValueError, match="Invalid date format"):
            parse_date("")
