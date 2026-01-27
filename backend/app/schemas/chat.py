"""Chat schemas."""

from typing import Optional

from pydantic import BaseModel, Field


class ChatMessageRequest(BaseModel):
    """Chat message request."""

    message: str = Field(..., min_length=1, max_length=2000)


class ChatMessageResponse(BaseModel):
    """Single chat message response."""

    id: str
    role: str  # user, assistant
    content: str
    message_type: str = "text"  # text, recipe_suggestion, etc.
    created_at: str  # ISO datetime
    recipe_suggestions: Optional[list[str]] = None  # Recipe IDs if applicable

    class Config:
        from_attributes = True


class ChatResponse(BaseModel):
    """Response from sending a chat message."""

    message: ChatMessageResponse
    has_recipe_suggestions: bool = False
    recipe_ids: list[str] = Field(default_factory=list)


class ChatHistoryResponse(BaseModel):
    """Chat history response."""

    messages: list[ChatMessageResponse]
    total_count: int

    class Config:
        from_attributes = True
