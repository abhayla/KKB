"""Festival schemas."""

from datetime import date
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field, field_validator


class FestivalCreate(BaseModel):
    """Request schema for creating a test festival (DEBUG mode only)."""

    name: str = Field(..., min_length=1, max_length=100)
    name_hindi: Optional[str] = None
    description: Optional[str] = None
    date: date
    regions: Optional[list[str]] = None
    is_fasting_day: bool = False
    fasting_type: Optional[str] = Field(None, pattern="^(complete|partial|specific)$")
    special_foods: Optional[list[str]] = None
    avoided_foods: Optional[list[str]] = None


class FestivalResponse(BaseModel):
    """Festival response for upcoming festivals endpoint."""

    id: str
    name: str
    name_hindi: Optional[str] = None
    description: Optional[str] = None
    date: str  # yyyy-MM-dd
    regions: list[str]
    is_fasting_day: bool
    fasting_type: Optional[str] = None
    special_foods: Optional[list[str]] = None
    avoided_foods: Optional[list[str]] = None

    model_config = ConfigDict(from_attributes=True)

    @field_validator("date", mode="before")
    @classmethod
    def coerce_date_to_str(cls, v: object) -> str:
        if isinstance(v, date):
            return v.isoformat()
        return v


class UpcomingFestivalResponse(BaseModel):
    """Simplified festival info for list display."""

    id: str
    name: str
    date: str
    days_away: int
    is_fasting_day: bool
    special_foods: Optional[list[str]] = None

    model_config = ConfigDict(from_attributes=True)
