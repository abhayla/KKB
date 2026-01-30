"""Chat message database model."""

import uuid
from typing import TYPE_CHECKING, Optional

from sqlalchemy import ForeignKey, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base, TimestampMixin
from app.models.user import JSONList

if TYPE_CHECKING:
    from app.models.user import User


class ChatMessage(Base, TimestampMixin):
    """Chat conversation message."""

    __tablename__ = "chat_messages"

    id: Mapped[str] = mapped_column(
        String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4()),
    )
    user_id: Mapped[str] = mapped_column(
        String(36),
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )

    role: Mapped[str] = mapped_column(
        String(20), nullable=False
    )  # user, assistant
    content: Mapped[str] = mapped_column(Text, nullable=False)
    message_type: Mapped[str] = mapped_column(
        String(20), nullable=False, default="text"
    )  # text, tool_use, tool_result

    # Tool calling support
    tool_calls: Mapped[Optional[list[dict]]] = mapped_column(
        JSONList, nullable=True
    )  # Tool calls made by assistant
    tool_results: Mapped[Optional[list[dict]]] = mapped_column(
        JSONList, nullable=True
    )  # Tool results from execution

    # Relationships
    user: Mapped["User"] = relationship("User", back_populates="chat_messages")
