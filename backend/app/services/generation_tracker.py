"""Meal generation tracking for debugging.

Writes one JSON file per meal generation call to logs/
(project root: C:\\Abhay\\VibeCoding\\KKB\\logs\\) with four sections:
  1. Prompt text + preferences snapshot
  2. Raw Gemini response + token usage
  3. Post-processed plan + items removed + rules applied
  4. Final client response (MealPlanResponse)

Filename pattern: MEAL_PLAN-20260306T113533Z.json
"""

import json
import logging
import time
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional

logger = logging.getLogger(__name__)

# Per-file JSON logs at project root: KKB/logs/
LOGS_DIR = Path(__file__).parent.parent.parent.parent / "logs"


@dataclass
class MealGenerationContext:
    """Mutable container passed through the meal generation pipeline.

    Populated incrementally as the generation progresses:
    - Endpoint creates it with user_id + week_start_date
    - ai_meal_service records preferences, prompt, response, tokens, enforced plan
    - Endpoint records meal_plan_id, timing, client response, success/error
    """

    user_id: str
    week_start_date: str
    trigger_source: str = "api"
    start_time: float = field(default_factory=time.monotonic)

    # Populated during generation
    prompt_text: Optional[str] = None
    response_text: Optional[str] = None
    token_usage: Optional[dict] = None
    preferences_snapshot: Optional[dict] = None
    rules_applied: Optional[dict] = None
    items_removed: list = field(default_factory=list)
    retry_count: int = 0
    model_name: Optional[str] = None
    temperature: Optional[float] = None

    # Section 3: enforced plan (after _enforce_rules)
    enforced_plan_data: Optional[dict] = None

    # Section 4: final client response (MealPlanResponse)
    client_response_data: Optional[dict] = None

    # Populated after generation
    meal_plan_id: Optional[str] = None
    items_generated: Optional[int] = None
    success: bool = False
    error_message: Optional[str] = None

    # Timing checkpoints (monotonic)
    ai_done_time: Optional[float] = None
    save_done_time: Optional[float] = None

    def _ms_since_start(self, checkpoint: Optional[float]) -> Optional[int]:
        if checkpoint is None:
            return None
        return int((checkpoint - self.start_time) * 1000)

    def _ms_between(
        self, start: Optional[float], end: Optional[float]
    ) -> Optional[int]:
        if start is None or end is None:
            return None
        return int((end - start) * 1000)

    @property
    def total_duration_ms(self) -> Optional[int]:
        end = self.save_done_time or self.ai_done_time
        return self._ms_since_start(end)

    @property
    def ai_duration_ms(self) -> Optional[int]:
        return self._ms_since_start(self.ai_done_time)

    @property
    def save_duration_ms(self) -> Optional[int]:
        return self._ms_between(self.ai_done_time, self.save_done_time)


def _parse_response_json(response_text: Optional[str]) -> Optional[dict]:
    """Parse response_text as JSON, returning dict or None on failure."""
    if not response_text:
        return None
    try:
        return json.loads(response_text)
    except (json.JSONDecodeError, TypeError):
        return None


def emit_structured_log(context: MealGenerationContext) -> None:
    """Write full generation data to a per-call JSON file.

    File: logs/MEAL_PLAN-<timestamp>.json (one file per generation).
    Contains 4 sections: prompt, Gemini response, post-processing, client response.
    """
    token_usage = context.token_usage or {}
    now = datetime.now(timezone.utc)
    timestamp_str = now.strftime("%Y%m%dT%H%M%SZ")

    log_data = {
        "metadata": {
            "timestamp": now.isoformat(),
            "user_id": context.user_id,
            "meal_plan_id": context.meal_plan_id,
            "week_start_date": context.week_start_date,
            "trigger_source": context.trigger_source,
            "success": context.success,
            "error": context.error_message,
            "model_name": context.model_name or token_usage.get("model_name"),
            "temperature": context.temperature,
            "retry_count": context.retry_count,
            "items_generated": context.items_generated,
            "total_duration_ms": context.total_duration_ms,
            "ai_duration_ms": context.ai_duration_ms,
            "save_duration_ms": context.save_duration_ms,
        },
        "section_1_prompt": {
            "prompt_text": context.prompt_text,
            "preferences_snapshot": context.preferences_snapshot,
        },
        "section_2_gemini_response": {
            "raw_response": _parse_response_json(context.response_text),
            "raw_response_text": (
                context.response_text
                if _parse_response_json(context.response_text) is None
                and context.response_text
                else None
            ),
            "token_usage": {
                "prompt_tokens": token_usage.get("prompt_tokens"),
                "completion_tokens": token_usage.get("completion_tokens"),
                "total_tokens": token_usage.get("total_tokens"),
                "thinking_tokens": token_usage.get("thinking_tokens"),
            },
            "retry_count": context.retry_count,
        },
        "section_3_post_processing": {
            "enforced_plan": context.enforced_plan_data,
            "items_removed": context.items_removed,
            "rules_applied": context.rules_applied,
        },
        "section_4_client_response": context.client_response_data,
    }

    # Write per-call JSON file (add microseconds to avoid collisions)
    usec = now.strftime("%f")[:4]
    filename = f"MEAL_PLAN-{timestamp_str}-{usec}.json"
    filepath = LOGS_DIR / filename
    try:
        LOGS_DIR.mkdir(parents=True, exist_ok=True)
        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(log_data, f, indent=2, ensure_ascii=False)
    except Exception:
        logger.warning("Failed to write generation log %s", filepath, exc_info=True)

    # Also emit summary to Python logger
    logger.info(
        "meal_generation_tracking user=%s plan=%s success=%s tokens=%s duration=%sms file=%s",
        context.user_id,
        context.meal_plan_id,
        context.success,
        token_usage.get("total_tokens"),
        context.total_duration_ms,
        filename,
    )
