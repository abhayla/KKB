"""Grocery list schemas."""

from typing import Optional

from pydantic import BaseModel, ConfigDict, Field


class GroceryItemResponse(BaseModel):
    """Single grocery item."""

    id: str
    name: str
    quantity: float
    unit: str
    category: str
    notes: Optional[str] = None
    is_checked: bool = False
    is_in_pantry: bool = False

    model_config = ConfigDict(from_attributes=True)


class GroceryCategoryResponse(BaseModel):
    """Grocery items grouped by category."""

    category: str
    items: list[GroceryItemResponse]


class GroceryListResponse(BaseModel):
    """Complete grocery list response."""

    id: str
    name: str
    meal_plan_id: Optional[str] = None
    categories: list[GroceryCategoryResponse]
    total_items: int
    checked_items: int

    model_config = ConfigDict(from_attributes=True)


class GroceryItemUpdate(BaseModel):
    """Update a grocery item."""

    is_checked: Optional[bool] = None
    quantity: Optional[float] = Field(default=None, gt=0)
    notes: Optional[str] = None


class WhatsAppGroceryResponse(BaseModel):
    """WhatsApp formatted grocery list."""

    formatted_text: str
    item_count: int
