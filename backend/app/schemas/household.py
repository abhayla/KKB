"""Pydantic schemas for household management."""

from datetime import date, datetime
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field, field_validator


# --- Request Schemas ---


class HouseholdCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=100, description="Household name")


class HouseholdUpdate(BaseModel):
    name: Optional[str] = Field(None, min_length=1, max_length=100)
    max_members: Optional[int] = Field(None, ge=2, le=20)


class JoinHouseholdRequest(BaseModel):
    invite_code: str = Field(..., min_length=8, max_length=8)

    @field_validator("invite_code", mode="before")
    @classmethod
    def normalize_code(cls, v: str) -> str:
        return v.strip().upper() if v else v


class AddMemberByPhoneRequest(BaseModel):
    phone_number: str = Field(..., min_length=10, max_length=20)
    is_temporary: bool = Field(default=False)
    leave_date: Optional[date] = Field(None)


class UpdateMemberRequest(BaseModel):
    can_edit_shared_plan: Optional[bool] = None
    portion_size: Optional[str] = Field(None, pattern="^(SMALL|REGULAR|LARGE)$")
    is_temporary: Optional[bool] = None
    leave_date: Optional[date] = None
    role: Optional[str] = Field(None, pattern="^(MEMBER|GUEST)$")


class TransferOwnershipRequest(BaseModel):
    new_owner_member_id: str = Field(
        ..., description="HouseholdMember ID of the new owner"
    )


# --- Response Schemas ---


class HouseholdMemberResponse(BaseModel):
    id: str
    household_id: str
    user_id: Optional[str] = None
    family_member_id: Optional[str] = None
    name: Optional[str] = None
    role: str
    can_edit_shared_plan: bool
    is_temporary: bool
    join_date: Optional[date] = None
    leave_date: Optional[date] = None
    portion_size: str = "REGULAR"
    status: str

    model_config = ConfigDict(from_attributes=True)


class HouseholdResponse(BaseModel):
    id: str
    name: str
    invite_code: Optional[str] = None
    owner_id: str
    max_members: int
    member_count: int = 0
    is_active: bool
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class HouseholdDetailResponse(BaseModel):
    household: HouseholdResponse
    members: list[HouseholdMemberResponse]


class InviteCodeResponse(BaseModel):
    invite_code: str
    expires_at: datetime


class CreateHouseholdRecipeRuleRequest(BaseModel):
    target_type: str = Field(..., pattern="^(RECIPE|INGREDIENT|MEAL_SLOT)$")
    action: str = Field(..., pattern="^(INCLUDE|EXCLUDE)$")
    target_name: str = Field(..., min_length=1, max_length=255)
    frequency_type: str = Field(
        ..., pattern="^(DAILY|TIMES_PER_WEEK|SPECIFIC_DAYS|NEVER)$"
    )
    frequency_count: Optional[int] = Field(None, ge=1, le=7)
    frequency_days: Optional[str] = Field(None, max_length=100)
    enforcement: Optional[str] = Field("PREFERRED", pattern="^(REQUIRED|PREFERRED)$")
    meal_slot: Optional[str] = Field(None, pattern="^(BREAKFAST|LUNCH|DINNER|SNACKS)$")


class HouseholdRecipeRuleResponse(BaseModel):
    id: str
    household_id: Optional[str] = None
    scope: str = "HOUSEHOLD"
    target_type: str
    action: str
    target_name: str
    frequency_type: str
    frequency_count: Optional[int] = None
    frequency_days: Optional[str] = None
    enforcement: str
    meal_slot: Optional[str] = None
    is_active: bool
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)
