"""Stats and gamification schemas."""

from typing import Optional

from pydantic import BaseModel, Field


class CookingStreakResponse(BaseModel):
    """Cooking streak response."""

    current_streak: int
    longest_streak: int
    total_meals_cooked: int
    last_cooking_date: Optional[str] = None  # yyyy-MM-dd
    streak_start_date: Optional[str] = None
    days_this_week: int = 0

    class Config:
        from_attributes = True


class DailyCookingRecord(BaseModel):
    """Daily cooking record."""

    date: str  # yyyy-MM-dd
    meals_cooked: int
    breakfast_cooked: bool
    lunch_cooked: bool
    dinner_cooked: bool


class AchievementResponse(BaseModel):
    """Achievement response."""

    id: str
    name: str
    description: str
    icon: str
    category: str
    is_unlocked: bool
    unlocked_at: Optional[str] = None
    progress: float = 0.0  # 0.0 to 1.0

    class Config:
        from_attributes = True


class CuisineBreakdown(BaseModel):
    """Cuisine type breakdown for stats."""

    cuisine_type: str
    count: int
    percentage: float


class MonthlyStatsResponse(BaseModel):
    """Monthly cooking stats response."""

    month: str  # yyyy-MM
    total_meals_cooked: int
    unique_recipes_tried: int
    total_cooking_days: int
    favorite_cuisine: Optional[str] = None
    cuisine_breakdown: list[CuisineBreakdown] = Field(default_factory=list)
    daily_records: list[DailyCookingRecord] = Field(default_factory=list)
    achievements_unlocked: list[AchievementResponse] = Field(default_factory=list)

    class Config:
        from_attributes = True


class LogCookingRequest(BaseModel):
    """Request to log a cooking session."""

    recipe_id: str
    meal_type: str  # breakfast, lunch, dinner
    date: Optional[str] = None  # yyyy-MM-dd, defaults to today
