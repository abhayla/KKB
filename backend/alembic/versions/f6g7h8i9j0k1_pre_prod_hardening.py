"""Pre-production hardening: user soft-delete, usage logs, performance indexes

Revision ID: f6g7h8i9j0k1
Revises: e5f6g7h8i9j0
Create Date: 2026-02-23 10:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = "f6g7h8i9j0k1"
down_revision: Union[str, None] = "e5f6g7h8i9j0"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Add deleted_at column to users table (soft delete support)
    op.add_column("users", sa.Column("deleted_at", sa.DateTime(timezone=True), nullable=True))

    # 2. Create usage_logs table (AI usage tracking)
    op.create_table(
        "usage_logs",
        sa.Column("id", sa.String(36), primary_key=True),
        sa.Column("user_id", sa.String(36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("action", sa.String(50), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )
    op.create_index("ix_usage_logs_user_id", "usage_logs", ["user_id"])
    op.create_index("ix_usage_logs_action_created", "usage_logs", ["user_id", "action", "created_at"])

    # 3. Performance indexes
    op.create_index("ix_meal_plans_user_active", "meal_plans", ["user_id", "is_active"])
    op.create_index("ix_recipe_rules_user_id", "recipe_rules", ["user_id"])
    op.create_index("ix_chat_messages_user_created", "chat_messages", ["user_id", "created_at"])

    # 4. Create refresh_tokens table (token rotation)
    op.create_table(
        "refresh_tokens",
        sa.Column("id", sa.String(36), primary_key=True),
        sa.Column("user_id", sa.String(36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("token_hash", sa.String(128), nullable=False, unique=True),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("is_revoked", sa.Boolean(), default=False, nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )
    op.create_index("ix_refresh_tokens_user_id", "refresh_tokens", ["user_id"])
    op.create_index("ix_refresh_tokens_token_hash", "refresh_tokens", ["token_hash"])


def downgrade() -> None:
    op.drop_table("refresh_tokens")
    op.drop_index("ix_chat_messages_user_created", table_name="chat_messages")
    op.drop_index("ix_recipe_rules_user_id", table_name="recipe_rules")
    op.drop_index("ix_meal_plans_user_active", table_name="meal_plans")
    op.drop_index("ix_usage_logs_action_created", table_name="usage_logs")
    op.drop_index("ix_usage_logs_user_id", table_name="usage_logs")
    op.drop_table("usage_logs")
    op.drop_column("users", "deleted_at")
