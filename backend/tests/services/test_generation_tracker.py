"""Tests for generation_tracker pure helpers.

Covers:
- MealGenerationContext duration properties (monotonic time math)
- _parse_response_json defensive parser

File-writing (emit_structured_log) is integration-level and intentionally
skipped here — it writes to disk and is exercised by real meal-generation
test runs.
"""

import time

from app.services.generation_tracker import (
    MealGenerationContext,
    _parse_response_json,
)


# ==================== MealGenerationContext durations ====================


class TestDurationProperties:
    def test_total_duration_none_when_no_checkpoints(self):
        ctx = MealGenerationContext(user_id="u", week_start_date="2026-05-11")
        assert ctx.total_duration_ms is None
        assert ctx.ai_duration_ms is None
        assert ctx.save_duration_ms is None

    def test_ai_duration_computed_from_ai_done(self):
        ctx = MealGenerationContext(user_id="u", week_start_date="2026-05-11")
        # Force start_time to a known value, then set ai_done 500ms later.
        ctx.start_time = 1000.0
        ctx.ai_done_time = 1000.5
        assert ctx.ai_duration_ms == 500

    def test_save_duration_bridges_ai_and_save(self):
        ctx = MealGenerationContext(user_id="u", week_start_date="2026-05-11")
        # Use exactly-representable binary fractions to avoid float drift
        # (1000.8 - 1000.5 is 0.29999… in double, giving 299ms not 300).
        ctx.start_time = 1000.0
        ctx.ai_done_time = 1000.5
        ctx.save_done_time = 1000.75  # 250ms later
        assert ctx.save_duration_ms == 250

    def test_total_uses_save_when_both_checkpoints_present(self):
        ctx = MealGenerationContext(user_id="u", week_start_date="2026-05-11")
        ctx.start_time = 1000.0
        ctx.ai_done_time = 1000.5
        ctx.save_done_time = 1001.0
        assert ctx.total_duration_ms == 1000  # 1s total

    def test_total_falls_back_to_ai_done_when_save_missing(self):
        """Handy when the save step errors out and the overall tracker still
        wants a total-duration number."""
        ctx = MealGenerationContext(user_id="u", week_start_date="2026-05-11")
        ctx.start_time = 2000.0
        ctx.ai_done_time = 2000.75  # 750ms
        # save_done_time remains None
        assert ctx.total_duration_ms == 750

    def test_save_duration_none_when_either_checkpoint_missing(self):
        ctx = MealGenerationContext(user_id="u", week_start_date="2026-05-11")
        ctx.ai_done_time = 1000.0
        # save_done_time is None -> save_duration_ms is None
        assert ctx.save_duration_ms is None

        ctx2 = MealGenerationContext(user_id="u", week_start_date="2026-05-11")
        ctx2.save_done_time = 1000.0
        # ai_done_time is None -> still None
        assert ctx2.save_duration_ms is None


# ==================== _parse_response_json ====================


class TestParseResponseJson:
    def test_returns_none_for_none_input(self):
        assert _parse_response_json(None) is None

    def test_returns_none_for_empty_string(self):
        assert _parse_response_json("") is None

    def test_parses_valid_json_object(self):
        result = _parse_response_json('{"days": [], "tokens": 42}')
        assert result == {"days": [], "tokens": 42}

    def test_returns_none_for_malformed_json(self):
        """Trailing comma is invalid JSON — expect graceful None, not raise."""
        assert _parse_response_json('{"days": [],}') is None

    def test_returns_none_for_plain_text(self):
        assert _parse_response_json("sorry, I don't know") is None

    def test_parses_json_array(self):
        """Non-dict top-level JSON still parses cleanly."""
        result = _parse_response_json("[1, 2, 3]")
        assert result == [1, 2, 3]


# ==================== MealGenerationContext defaults ====================


class TestContextDefaults:
    def test_required_fields_set_on_construction(self):
        ctx = MealGenerationContext(user_id="u-42", week_start_date="2026-05-11")
        assert ctx.user_id == "u-42"
        assert ctx.week_start_date == "2026-05-11"

    def test_optional_fields_default_to_none_or_empty(self):
        ctx = MealGenerationContext(user_id="u", week_start_date="2026-05-11")
        assert ctx.prompt_text is None
        assert ctx.response_text is None
        assert ctx.token_usage is None
        assert ctx.items_removed == []
        assert ctx.retry_count == 0
        assert ctx.trigger_source == "api"
        assert ctx.success is False

    def test_start_time_is_monotonic(self):
        """start_time must come from time.monotonic(), not wall-clock."""
        before = time.monotonic()
        ctx = MealGenerationContext(user_id="u", week_start_date="2026-05-11")
        after = time.monotonic()
        assert before <= ctx.start_time <= after
