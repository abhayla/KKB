"""Recipe schemas matching Android DTOs."""

from datetime import datetime
from typing import Optional

from pydantic import BaseModel, Field


class IngredientDto(BaseModel):
    """Ingredient matching Android IngredientDto."""

    id: str
    name: str
    quantity: str  # String in Android DTO
    unit: str
    category: str  # vegetables, dairy, grains, spices, etc.
    is_optional: bool = False
    substitute_for: Optional[str] = None

    class Config:
        from_attributes = True


class InstructionDto(BaseModel):
    """Instruction matching Android InstructionDto."""

    step_number: int
    instruction: str
    duration_minutes: Optional[int] = None
    timer_required: bool = False
    tips: Optional[str] = None

    class Config:
        from_attributes = True


class NutritionDto(BaseModel):
    """Nutrition info matching Android NutritionDto."""

    calories: int
    protein: int  # grams
    carbohydrates: int  # grams
    fat: int  # grams
    fiber: int  # grams
    sugar: int = 0  # grams
    sodium: int = 0  # mg

    class Config:
        from_attributes = True


class RecipeResponse(BaseModel):
    """Recipe response matching Android RecipeResponse."""

    id: str
    name: str
    description: str
    image_url: Optional[str] = None
    prep_time_minutes: int
    cook_time_minutes: int
    servings: int
    difficulty: str  # easy, medium, hard
    cuisine_type: str  # north, south, east, west
    meal_types: list[str]  # breakfast, lunch, dinner, snacks
    dietary_tags: list[str]  # vegetarian, vegan, etc.
    ingredients: list[IngredientDto]
    instructions: list[InstructionDto]
    nutrition: Optional[NutritionDto] = None

    class Config:
        from_attributes = True


class RecipeSearchParams(BaseModel):
    """Query parameters for recipe search."""

    q: str = ""
    cuisine: Optional[str] = None
    dietary: Optional[str] = None
    meal_type: Optional[str] = None
    page: int = Field(default=1, ge=1)
    limit: int = Field(default=20, ge=1, le=100)


class AiRecipeCatalogResponse(BaseModel):
    """Response for AI recipe catalog search results."""

    id: str
    display_name: str
    normalized_name: str
    dietary_tags: list[str] = []
    cuisine_type: Optional[str] = None
    meal_types: list[str] = []
    category: Optional[str] = None
    prep_time_minutes: Optional[int] = None
    calories: Optional[int] = None
    ingredients: Optional[list[dict]] = None
    nutrition: Optional[dict] = None
    usage_count: int = 1

    class Config:
        from_attributes = True


class RecipeRatingRequest(BaseModel):
    """Request to rate a recipe."""

    rating: float = Field(..., ge=1.0, le=5.0, description="Rating from 1.0 to 5.0")
    feedback: Optional[str] = Field(None, max_length=1000, description="Optional feedback text")


class RecipeRatingResponse(BaseModel):
    """Response for a recipe rating."""

    id: str
    recipe_id: str
    rating: float
    feedback: Optional[str] = None
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


class RecipeCreate(BaseModel):
    """Create a new recipe (admin)."""

    name: str = Field(..., max_length=255)
    description: Optional[str] = None
    image_url: Optional[str] = None
    cuisine_type: str
    meal_types: list[str]
    dietary_tags: list[str]
    prep_time_minutes: int = Field(ge=0)
    cook_time_minutes: int = Field(ge=0)
    servings: int = Field(default=4, ge=1)
    difficulty_level: str = "medium"
    is_festive: bool = False
    is_fasting_friendly: bool = False
    is_quick_meal: bool = False
    is_kid_friendly: bool = False
