"""Meal plan schemas matching Android DTOs."""

from typing import Optional

from pydantic import BaseModel, Field


class FestivalDto(BaseModel):
    """Festival info matching Android FestivalDto."""

    id: str
    name: str
    is_fasting_day: bool
    suggested_dishes: Optional[list[str]] = None

    class Config:
        from_attributes = True


class MealItemDto(BaseModel):
    """Meal item matching Android MealItemDto."""

    id: str
    recipe_id: str
    recipe_name: str
    recipe_image_url: Optional[str] = None
    prep_time_minutes: int
    calories: int = 0
    is_locked: bool
    order: int
    dietary_tags: list[str]

    class Config:
        from_attributes = True


class MealsByTypeDto(BaseModel):
    """Meals grouped by type matching Android MealsByTypeDto."""

    breakfast: list[MealItemDto] = Field(default_factory=list)
    lunch: list[MealItemDto] = Field(default_factory=list)
    dinner: list[MealItemDto] = Field(default_factory=list)
    snacks: list[MealItemDto] = Field(default_factory=list)

    class Config:
        from_attributes = True


class MealPlanDayDto(BaseModel):
    """Single day in meal plan matching Android MealPlanDayDto."""

    date: str  # yyyy-MM-dd
    day_name: str  # Monday, Tuesday, etc.
    meals: MealsByTypeDto
    festival: Optional[FestivalDto] = None

    class Config:
        from_attributes = True


class MealPlanResponse(BaseModel):
    """Meal plan response matching Android MealPlanResponse."""

    id: str
    week_start_date: str  # yyyy-MM-dd
    week_end_date: str  # yyyy-MM-dd
    days: list[MealPlanDayDto]
    created_at: str  # ISO datetime
    updated_at: str  # ISO datetime

    class Config:
        from_attributes = True


class GenerateMealPlanRequest(BaseModel):
    """Request to generate meal plan matching Android GenerateMealPlanRequest."""

    week_start_date: str  # yyyy-MM-dd
    regenerate_days: Optional[list[str]] = None  # Specific days to regenerate
    exclude_recipe_ids: Optional[list[str]] = None


class SwapMealRequest(BaseModel):
    """Request to swap a meal matching Android SwapMealRequest."""

    exclude_recipe_ids: Optional[list[str]] = None
    specific_recipe_id: Optional[str] = None  # If user wants a specific recipe
