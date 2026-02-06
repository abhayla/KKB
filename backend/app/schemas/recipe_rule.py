"""Recipe rule and nutrition goal Pydantic schemas."""

from datetime import datetime
from typing import Optional

from pydantic import BaseModel, Field


# ==================== Recipe Rule Schemas ====================


class RecipeRuleCreate(BaseModel):
    """Request schema for creating a recipe rule."""

    target_type: str = Field(
        ..., description="Type of target: RECIPE, INGREDIENT, MEAL_SLOT"
    )
    action: str = Field(..., description="Action: INCLUDE or EXCLUDE")
    target_id: Optional[str] = Field(None, description="Recipe ID (optional)")
    target_name: str = Field(..., description="Display name for the rule target")
    frequency_type: str = Field(
        ..., description="Frequency: DAILY, TIMES_PER_WEEK, SPECIFIC_DAYS, NEVER"
    )
    frequency_count: Optional[int] = Field(
        None, ge=1, le=7, description="Count for TIMES_PER_WEEK (1-7)"
    )
    frequency_days: Optional[str] = Field(
        None, description="Comma-separated days: MONDAY,WEDNESDAY"
    )
    enforcement: str = Field(default="REQUIRED", description="REQUIRED or PREFERRED")
    meal_slot: Optional[str] = Field(
        None, description="Meal slot: BREAKFAST, LUNCH, DINNER, SNACKS"
    )
    is_active: bool = Field(default=True)


class RecipeRuleUpdate(BaseModel):
    """Request schema for updating a recipe rule."""

    target_type: Optional[str] = None
    action: Optional[str] = None
    target_id: Optional[str] = None
    target_name: Optional[str] = None
    frequency_type: Optional[str] = None
    frequency_count: Optional[int] = Field(None, ge=1, le=7)
    frequency_days: Optional[str] = None
    enforcement: Optional[str] = None
    meal_slot: Optional[str] = None
    is_active: Optional[bool] = None


class RecipeRuleResponse(BaseModel):
    """Response schema for a recipe rule."""

    id: str
    user_id: str
    target_type: str
    action: str
    target_id: Optional[str] = None
    target_name: str
    frequency_type: str
    frequency_count: Optional[int] = None
    frequency_days: Optional[str] = None
    enforcement: str
    meal_slot: Optional[str] = None
    is_active: bool
    sync_status: str
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


# ==================== Nutrition Goal Schemas ====================


class NutritionGoalCreate(BaseModel):
    """Request schema for creating a nutrition goal."""

    food_category: str = Field(
        ..., description="Food category: LEAFY_GREENS, PROTEIN, FERMENTED, etc."
    )
    weekly_target: int = Field(default=3, ge=1, le=14, description="Weekly target (1-14)")
    enforcement: str = Field(default="PREFERRED", description="REQUIRED or PREFERRED")
    is_active: bool = Field(default=True)


class NutritionGoalUpdate(BaseModel):
    """Request schema for updating a nutrition goal."""

    food_category: Optional[str] = None
    weekly_target: Optional[int] = Field(None, ge=1, le=14)
    current_progress: Optional[int] = Field(None, ge=0)
    enforcement: Optional[str] = None
    is_active: Optional[bool] = None


class NutritionGoalResponse(BaseModel):
    """Response schema for a nutrition goal."""

    id: str
    user_id: str
    food_category: str
    weekly_target: int
    current_progress: int
    enforcement: str
    is_active: bool
    sync_status: str
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


# ==================== Sync Schemas ====================


class RecipeRuleSyncItem(BaseModel):
    """Single recipe rule for sync request."""

    id: str
    target_type: str
    action: str
    target_id: Optional[str] = None
    target_name: str
    frequency_type: str
    frequency_count: Optional[int] = None
    frequency_days: Optional[str] = None
    enforcement: str = "REQUIRED"
    meal_slot: Optional[str] = None
    is_active: bool = True
    local_updated_at: datetime  # Client's timestamp for conflict resolution


class NutritionGoalSyncItem(BaseModel):
    """Single nutrition goal for sync request."""

    id: str
    food_category: str
    weekly_target: int = 3
    current_progress: int = 0
    enforcement: str = "PREFERRED"
    is_active: bool = True
    local_updated_at: datetime  # Client's timestamp for conflict resolution


class SyncRequest(BaseModel):
    """Request schema for batch sync of rules and goals."""

    recipe_rules: list[RecipeRuleSyncItem] = Field(default_factory=list)
    nutrition_goals: list[NutritionGoalSyncItem] = Field(default_factory=list)
    last_sync_time: Optional[datetime] = Field(
        None, description="Last successful sync timestamp"
    )


class SyncResponse(BaseModel):
    """Response schema for batch sync."""

    # Server-side rules that client needs to update
    server_recipe_rules: list[RecipeRuleResponse] = Field(default_factory=list)
    server_nutrition_goals: list[NutritionGoalResponse] = Field(default_factory=list)

    # IDs of items that were successfully synced from client
    synced_rule_ids: list[str] = Field(default_factory=list)
    synced_goal_ids: list[str] = Field(default_factory=list)

    # IDs of items that had conflicts (server version newer)
    conflict_rule_ids: list[str] = Field(default_factory=list)
    conflict_goal_ids: list[str] = Field(default_factory=list)

    # IDs of items that were deleted on server
    deleted_rule_ids: list[str] = Field(default_factory=list)
    deleted_goal_ids: list[str] = Field(default_factory=list)

    # New sync timestamp
    sync_time: datetime


# ==================== List Response Schemas ====================


class RecipeRulesListResponse(BaseModel):
    """Response containing a list of recipe rules."""

    rules: list[RecipeRuleResponse]
    total_count: int


class NutritionGoalsListResponse(BaseModel):
    """Response containing a list of nutrition goals."""

    goals: list[NutritionGoalResponse]
    total_count: int
