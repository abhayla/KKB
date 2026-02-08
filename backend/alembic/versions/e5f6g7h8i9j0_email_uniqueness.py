"""Email uniqueness enforcement: delete duplicate, normalize, add unique index

Revision ID: e5f6g7h8i9j0
Revises: d4e5f6g7h8i9
Create Date: 2026-02-08 10:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'e5f6g7h8i9j0'
down_revision: Union[str, None] = 'd4e5f6g7h8i9'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Delete duplicate User 4 (0 child records, safe to delete)
    op.execute(
        "DELETE FROM users WHERE id = 'f76086d9-5be2-47a6-9d03-a6715c49bfb0'"
    )

    # 2. Normalize existing emails to lowercase/trimmed
    op.execute(
        "UPDATE users SET email = LOWER(TRIM(email)) WHERE email IS NOT NULL"
    )

    # 3. Create partial unique index (case-insensitive, allows NULL)
    op.execute(
        "CREATE UNIQUE INDEX uq_users_email_lower ON users (LOWER(email)) WHERE email IS NOT NULL"
    )


def downgrade() -> None:
    op.execute("DROP INDEX IF EXISTS uq_users_email_lower")
