"""Festival schemas."""

from typing import Optional

from pydantic import BaseModel, ConfigDict


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


class UpcomingFestivalResponse(BaseModel):
    """Simplified festival info for list display."""

    id: str
    name: str
    date: str
    days_away: int
    is_fasting_day: bool
    special_foods: Optional[list[str]] = None

    model_config = ConfigDict(from_attributes=True)
