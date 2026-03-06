"""Chat schemas."""

from typing import Optional

from pydantic import BaseModel, ConfigDict, Field


class ChatMessageRequest(BaseModel):
    """Chat message request."""

    message: str = Field(..., min_length=1, max_length=2000)


class ChatImageRequest(BaseModel):
    """Chat image request for food photo analysis."""

    message: str = Field(
        default="Please analyze this food image",
        max_length=500,
        description="Optional message to accompany the image",
    )
    image_base64: str = Field(
        ..., min_length=100, description="Base64 encoded image data"
    )
    media_type: str = Field(
        default="image/jpeg",
        pattern=r"^image/(jpeg|png|webp)$",
        description="Image MIME type",
    )


class ChatMessageResponse(BaseModel):
    """Single chat message response."""

    id: str
    role: str  # user, assistant
    content: str
    message_type: str = "text"  # text, recipe_suggestion, etc.
    created_at: str  # ISO datetime
    recipe_suggestions: Optional[list[str]] = None  # Recipe IDs if applicable

    model_config = ConfigDict(from_attributes=True)


class ChatResponse(BaseModel):
    """Response from sending a chat message."""

    message: ChatMessageResponse
    has_recipe_suggestions: bool = False
    recipe_ids: list[str] = Field(default_factory=list)


class ChatHistoryResponse(BaseModel):
    """Chat history response."""

    messages: list[ChatMessageResponse]
    total_count: int

    model_config = ConfigDict(from_attributes=True)
