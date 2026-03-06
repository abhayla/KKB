"""Tests for the meal generation tracking system (per-file JSON logging)."""

import json
import uuid
from datetime import date, timedelta
from unittest.mock import AsyncMock, MagicMock, patch

from app.services.generation_tracker import (
    MealGenerationContext,
    emit_structured_log,
)


class TestMealGenerationContext:
    """Tests for the MealGenerationContext dataclass."""

    def test_initialization(self):
        ctx = MealGenerationContext(
            user_id="user-123",
            week_start_date="2026-03-02",
        )
        assert ctx.user_id == "user-123"
        assert ctx.week_start_date == "2026-03-02"
        assert ctx.trigger_source == "api"
        assert ctx.success is False
        assert ctx.items_removed == []
        assert ctx.retry_count == 0
        assert ctx.enforced_plan_data is None
        assert ctx.client_response_data is None

    def test_timing_properties(self):
        ctx = MealGenerationContext(
            user_id="user-123",
            week_start_date="2026-03-02",
            start_time=100.0,
        )
        ctx.ai_done_time = 145.0
        ctx.save_done_time = 146.5

        assert ctx.ai_duration_ms == 45000
        assert ctx.save_duration_ms == 1500
        assert ctx.total_duration_ms == 46500

    def test_timing_none_when_not_set(self):
        ctx = MealGenerationContext(
            user_id="user-123",
            week_start_date="2026-03-02",
        )
        assert ctx.ai_duration_ms is None
        assert ctx.save_duration_ms is None
        assert ctx.total_duration_ms is None


class TestEmitStructuredLog:
    """Tests for emit_structured_log per-file JSON output."""

    def test_emit_log_format(self, caplog):
        ctx = MealGenerationContext(
            user_id="user-456",
            week_start_date="2026-03-02",
            trigger_source="regenerate",
            start_time=100.0,
        )
        ctx.meal_plan_id = "plan-xyz"
        ctx.success = True
        ctx.items_generated = 56
        ctx.items_removed = [{"recipe": "X", "reason": "allergen"}]
        ctx.token_usage = {"prompt_tokens": 1000, "total_tokens": 3000}
        ctx.ai_done_time = 150.0
        ctx.save_done_time = 151.0

        with caplog.at_level("INFO", logger="app.services.generation_tracker"):
            emit_structured_log(ctx)

        assert "meal_generation_tracking" in caplog.text

    def test_emit_handles_no_token_usage(self):
        ctx = MealGenerationContext(
            user_id="user-789",
            week_start_date="2026-03-02",
        )
        # Should not raise
        emit_structured_log(ctx)

    def test_emit_writes_json_file(self, tmp_path):
        """Verify emit_structured_log writes a valid JSON file with 4 sections."""
        ctx = MealGenerationContext(
            user_id="user-jsonl-test",
            week_start_date="2026-03-02",
            trigger_source="test",
            start_time=100.0,
        )
        ctx.prompt_text = "Generate a meal plan for vegetarian family"
        ctx.response_text = '{"days": []}'
        ctx.token_usage = {
            "prompt_tokens": 1500,
            "completion_tokens": 3000,
            "total_tokens": 4500,
            "thinking_tokens": 200,
            "model_name": "gemini-2.5-flash",
        }
        ctx.model_name = "gemini-2.5-flash"
        ctx.temperature = 0.8
        ctx.meal_plan_id = "plan-abc"
        ctx.items_generated = 56
        ctx.items_removed = [
            {"recipe": "Peanut Chutney", "reason": "allergen"},
        ]
        ctx.preferences_snapshot = {"dietary_tags": ["vegetarian"]}
        ctx.rules_applied = {"include_rules": 2, "exclude_rules": 1}
        ctx.enforced_plan_data = {"week_start_date": "2026-03-02", "days": []}
        ctx.client_response_data = {"id": "plan-abc", "days": []}
        ctx.success = True
        ctx.ai_done_time = 145.0
        ctx.save_done_time = 146.5

        with patch("app.services.generation_tracker.LOGS_DIR", tmp_path):
            emit_structured_log(ctx)

        # Find the written file
        json_files = list(tmp_path.glob("MEAL_PLAN-*.json"))
        assert len(json_files) == 1

        data = json.loads(json_files[0].read_text(encoding="utf-8"))

        # Verify metadata
        meta = data["metadata"]
        assert meta["user_id"] == "user-jsonl-test"
        assert meta["week_start_date"] == "2026-03-02"
        assert meta["trigger_source"] == "test"
        assert meta["success"] is True
        assert meta["meal_plan_id"] == "plan-abc"
        assert meta["items_generated"] == 56
        assert meta["model_name"] == "gemini-2.5-flash"
        assert meta["temperature"] == 0.8
        assert meta["total_duration_ms"] == 46500
        assert meta["ai_duration_ms"] == 45000
        assert meta["save_duration_ms"] == 1500

        # Verify section 1: prompt
        s1 = data["section_1_prompt"]
        assert s1["prompt_text"] == "Generate a meal plan for vegetarian family"
        assert s1["preferences_snapshot"] == {"dietary_tags": ["vegetarian"]}

        # Verify section 2: Gemini response
        s2 = data["section_2_gemini_response"]
        assert s2["raw_response"] == {"days": []}
        assert s2["raw_response_text"] is None  # parsed OK, so text not stored
        assert s2["token_usage"]["prompt_tokens"] == 1500
        assert s2["token_usage"]["total_tokens"] == 4500
        assert s2["token_usage"]["thinking_tokens"] == 200
        assert s2["retry_count"] == 0

        # Verify section 3: post-processing
        s3 = data["section_3_post_processing"]
        assert s3["enforced_plan"] == {"week_start_date": "2026-03-02", "days": []}
        assert len(s3["items_removed"]) == 1
        assert s3["items_removed"][0]["recipe"] == "Peanut Chutney"
        assert s3["rules_applied"] == {"include_rules": 2, "exclude_rules": 1}

        # Verify section 4: client response
        assert data["section_4_client_response"] == {"id": "plan-abc", "days": []}

    def test_emit_creates_separate_files(self, tmp_path):
        """Verify multiple calls create separate JSON files."""
        with patch("app.services.generation_tracker.LOGS_DIR", tmp_path):
            for i in range(3):
                ctx = MealGenerationContext(
                    user_id=f"user-{i}",
                    week_start_date="2026-03-02",
                )
                ctx.success = i % 2 == 0
                emit_structured_log(ctx)

        json_files = sorted(tmp_path.glob("MEAL_PLAN-*.json"))
        assert len(json_files) == 3

        for i, f in enumerate(json_files):
            data = json.loads(f.read_text(encoding="utf-8"))
            assert data["metadata"]["user_id"] == f"user-{i}"
            assert data["metadata"]["success"] == (i % 2 == 0)

    def test_emit_failed_generation(self, tmp_path):
        """Verify error details are captured with null sections."""
        ctx = MealGenerationContext(
            user_id="user-fail",
            week_start_date="2026-03-02",
            start_time=100.0,
        )
        ctx.error_message = "Timeout after 120s"
        ctx.retry_count = 3
        ctx.ai_done_time = 220.0

        with patch("app.services.generation_tracker.LOGS_DIR", tmp_path):
            emit_structured_log(ctx)

        json_files = list(tmp_path.glob("MEAL_PLAN-*.json"))
        assert len(json_files) == 1

        data = json.loads(json_files[0].read_text(encoding="utf-8"))
        assert data["metadata"]["success"] is False
        assert data["metadata"]["error"] == "Timeout after 120s"
        assert data["metadata"]["retry_count"] == 3
        assert data["metadata"]["ai_duration_ms"] == 120000
        assert data["section_1_prompt"]["prompt_text"] is None
        assert data["section_2_gemini_response"]["raw_response"] is None
        assert data["section_3_post_processing"]["enforced_plan"] is None
        assert data["section_4_client_response"] is None

    def test_emit_unparseable_response_stores_text(self, tmp_path):
        """When Gemini returns invalid JSON, raw_response_text preserves it."""
        ctx = MealGenerationContext(
            user_id="user-bad-json",
            week_start_date="2026-03-02",
        )
        ctx.response_text = "This is not valid JSON {{"
        ctx.success = False

        with patch("app.services.generation_tracker.LOGS_DIR", tmp_path):
            emit_structured_log(ctx)

        json_files = list(tmp_path.glob("MEAL_PLAN-*.json"))
        data = json.loads(json_files[0].read_text(encoding="utf-8"))

        s2 = data["section_2_gemini_response"]
        assert s2["raw_response"] is None
        assert s2["raw_response_text"] == "This is not valid JSON {{"


class TestFullPipelineTracking:
    """End-to-end test: generate_meal_plan() with context populates all fields."""

    async def test_generate_with_context_tracks_everything(self):
        """Call generate_meal_plan with a context and verify all fields logged."""
        from app.services.ai_meal_service import AIMealService, UserPreferences

        user_id = str(uuid.uuid4())
        week_start = date.today() - timedelta(days=date.today().weekday())

        # Build a valid AI response with an allergen to trigger post-processing
        days = []
        for i in range(7):
            current = week_start + timedelta(days=i)
            days.append(
                {
                    "date": current.isoformat(),
                    "day_name": current.strftime("%A"),
                    "breakfast": [
                        {
                            "recipe_name": "Aloo Paratha",
                            "prep_time_minutes": 25,
                            "dietary_tags": ["vegetarian"],
                            "category": "paratha",
                        },
                        {
                            "recipe_name": "Masala Chai",
                            "prep_time_minutes": 10,
                            "dietary_tags": ["vegetarian"],
                            "category": "chai",
                        },
                    ],
                    "lunch": [
                        {
                            "recipe_name": "Dal Tadka",
                            "prep_time_minutes": 30,
                            "dietary_tags": ["vegetarian"],
                            "category": "dal",
                        },
                        {
                            "recipe_name": "Jeera Rice",
                            "prep_time_minutes": 20,
                            "dietary_tags": ["vegetarian"],
                            "category": "rice",
                        },
                    ],
                    "dinner": [
                        {
                            "recipe_name": "Paneer Tikka",
                            "prep_time_minutes": 30,
                            "dietary_tags": ["vegetarian"],
                            "category": "curry",
                        },
                        {
                            "recipe_name": "Peanut Chutney",
                            "prep_time_minutes": 10,
                            "dietary_tags": ["vegetarian"],
                            "category": "chutney",
                        },
                    ],
                    "snacks": [
                        {
                            "recipe_name": "Samosa",
                            "prep_time_minutes": 20,
                            "dietary_tags": ["vegetarian"],
                            "category": "snack",
                        },
                        {
                            "recipe_name": "Masala Chai",
                            "prep_time_minutes": 10,
                            "dietary_tags": ["vegetarian"],
                            "category": "chai",
                        },
                    ],
                }
            )
        ai_response_text = json.dumps({"days": days})

        mock_metadata = {
            "model_name": "gemini-2.5-flash",
            "prompt_tokens": 2500,
            "completion_tokens": 5000,
            "total_tokens": 7500,
            "thinking_tokens": 300,
        }

        prefs = UserPreferences(
            dietary_tags=["vegetarian"],
            cuisine_preferences=["north"],
            allergies=[{"ingredient": "peanuts", "severity": "SEVERE"}],
            dislikes=["karela"],
            weekday_cooking_time=30,
            weekend_cooking_time=60,
            busy_days=["MONDAY"],
            include_rules=[],
            exclude_rules=[],
            family_size=3,
            spice_level="medium",
        )

        service = AIMealService()
        ctx = MealGenerationContext(
            user_id=user_id,
            week_start_date=week_start.isoformat(),
            trigger_source="test",
        )

        with (
            patch.object(
                service,
                "_load_user_preferences",
                new_callable=AsyncMock,
                return_value=prefs,
            ),
            patch.object(
                service, "_load_festivals", new_callable=AsyncMock, return_value={}
            ),
            patch.object(
                service.config_service,
                "get_config",
                new_callable=AsyncMock,
                return_value=MagicMock(),
            ),
            patch(
                "app.services.ai_meal_service.generate_text_with_metadata",
                new_callable=AsyncMock,
                return_value=(ai_response_text, mock_metadata),
            ),
        ):
            result = await service.generate_meal_plan(user_id, week_start, context=ctx)

        # Verify context was populated by the pipeline
        assert ctx.prompt_text is not None
        assert len(ctx.prompt_text) > 100
        assert ctx.response_text == ai_response_text
        assert ctx.token_usage == mock_metadata
        assert ctx.preferences_snapshot is not None
        assert ctx.preferences_snapshot["dietary_tags"] == ["vegetarian"]
        assert ctx.preferences_snapshot["family_size"] == 3

        # Peanut Chutney should have been removed (allergen enforcement)
        assert len(ctx.items_removed) > 0
        peanut_removals = [
            r for r in ctx.items_removed if "Peanut" in r.get("recipe", "")
        ]
        assert len(peanut_removals) == 7  # one per day (dinner slot)
        assert all(r["reason"] == "allergen" for r in peanut_removals)

        assert ctx.rules_applied is not None

        # Verify enforced_plan_data was captured (Section 3)
        assert ctx.enforced_plan_data is not None
        assert "days" in ctx.enforced_plan_data
        assert len(ctx.enforced_plan_data["days"]) == 7
