"""User schemas matching Android DTOs."""

from typing import Optional

from pydantic import BaseModel, EmailStr, Field


class UserPreferencesDto(BaseModel):
    """User preferences matching Android UserPreferencesDto."""

    household_size: int = Field(default=2, ge=1, le=20)
    dietary_type: Optional[str] = None  # vegetarian, non_vegetarian, vegan, etc.
    dietary_restrictions: list[str] = Field(default_factory=list)
    cuisine_preferences: list[str] = Field(default_factory=list)
    disliked_ingredients: list[str] = Field(default_factory=list)
    cooking_time_preference: str = "moderate"  # quick, moderate, elaborate
    spice_level: str = "medium"  # mild, medium, spicy
    busy_days: list[str] = Field(default_factory=list)  # MONDAY, TUESDAY, etc.
    weekday_cooking_time_minutes: Optional[int] = None
    weekend_cooking_time_minutes: Optional[int] = None
    # Meal generation settings
    items_per_meal: int = Field(default=2, ge=1, le=4)
    strict_allergen_mode: bool = True
    strict_dietary_mode: bool = True
    allow_recipe_repeat: bool = False

    class Config:
        from_attributes = True


class UserPreferencesUpdate(BaseModel):
    """User preferences update request."""

    household_size: Optional[int] = Field(default=None, ge=1, le=20)
    primary_diet: Optional[str] = None  # Maps to dietary_type column
    dietary_restrictions: Optional[list[str]] = None
    cuisine_preferences: Optional[list[str]] = None
    disliked_ingredients: Optional[list[str]] = None
    cooking_time_preference: Optional[str] = None
    spice_level: Optional[str] = None
    busy_days: Optional[list[str]] = None  # MONDAY, TUESDAY, etc.
    weekday_cooking_time: Optional[int] = None  # Minutes
    weekend_cooking_time: Optional[int] = None  # Minutes
    # Meal generation settings
    items_per_meal: Optional[int] = Field(default=None, ge=1, le=4)
    strict_allergen_mode: Optional[bool] = None
    strict_dietary_mode: Optional[bool] = None
    allow_recipe_repeat: Optional[bool] = None


class UserResponse(BaseModel):
    """User response matching Android UserResponse."""

    id: str
    email: str
    name: str
    profile_image_url: Optional[str] = None
    is_onboarded: bool
    preferences: Optional[UserPreferencesDto] = None

    class Config:
        from_attributes = True


class FamilyMemberCreate(BaseModel):
    """Create a family member."""

    name: str = Field(..., max_length=100)
    age_group: Optional[str] = None  # child, teen, adult, senior
    dietary_restrictions: list[str] = Field(default_factory=list)
    health_conditions: list[str] = Field(default_factory=list)


class FamilyMemberUpdate(BaseModel):
    """Update a family member."""

    name: Optional[str] = Field(None, max_length=100)
    age_group: Optional[str] = None
    dietary_restrictions: Optional[list[str]] = None
    health_conditions: Optional[list[str]] = None


class FamilyMemberResponse(BaseModel):
    """Family member response."""

    id: str
    name: str
    age_group: Optional[str] = None
    dietary_restrictions: list[str] = Field(default_factory=list)
    health_conditions: list[str] = Field(default_factory=list)

    class Config:
        from_attributes = True


class FamilyMembersListResponse(BaseModel):
    """Response containing a list of family members."""

    members: list[FamilyMemberResponse]
    total_count: int
