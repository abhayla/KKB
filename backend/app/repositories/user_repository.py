"""User repository for PostgreSQL operations."""

import json
import logging
import uuid
from datetime import datetime, timezone
from typing import Any, Optional

from sqlalchemy import func, select, delete
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.postgres import async_session_maker
from app.models.user import FamilyMember, User, UserPreferences

logger = logging.getLogger(__name__)


class UserRepository:
    """Repository for user-related PostgreSQL operations."""

    def __init__(self, session: Optional[AsyncSession] = None):
        """Initialize repository with optional session for dependency injection."""
        self._external_session = session

    async def _get_session(self) -> AsyncSession:
        """Get database session."""
        if self._external_session:
            return self._external_session
        return async_session_maker()

    async def get_by_id(self, user_id: str) -> Optional[dict[str, Any]]:
        """Get user by ID."""
        async with async_session_maker() as session:
            result = await session.execute(
                select(User).where(User.id == user_id)
            )
            user = result.scalar_one_or_none()
            if user:
                return self._user_to_dict(user)
            return None

    async def get_by_firebase_uid(self, firebase_uid: str) -> Optional[dict[str, Any]]:
        """Get user by Firebase UID."""
        async with async_session_maker() as session:
            result = await session.execute(
                select(User).where(User.firebase_uid == firebase_uid)
            )
            user = result.scalar_one_or_none()
            if user:
                return self._user_to_dict(user)
            return None

    async def get_by_email(self, email: str) -> Optional[dict[str, Any]]:
        """Get user by email (case-insensitive)."""
        async with async_session_maker() as session:
            result = await session.execute(
                select(User).where(func.lower(User.email) == email.strip().lower())
            )
            user = result.scalar_one_or_none()
            if user:
                return self._user_to_dict(user)
            return None

    async def create(
        self,
        firebase_uid: str,
        email: Optional[str] = None,
        name: Optional[str] = None,
        profile_picture_url: Optional[str] = None,
    ) -> dict[str, Any]:
        """Create a new user."""
        async with async_session_maker() as session:
            user = User(
                id=str(uuid.uuid4()),
                firebase_uid=firebase_uid,
                email=email.strip().lower() if email else None,
                name=name,
                profile_picture_url=profile_picture_url,
                is_onboarded=False,
                is_active=True,
            )
            session.add(user)
            await session.commit()
            await session.refresh(user)

            logger.info(f"Created user: {user.id}")
            return self._user_to_dict(user)

    async def update(self, user_id: str, data: dict[str, Any]) -> Optional[dict[str, Any]]:
        """Update user data."""
        async with async_session_maker() as session:
            result = await session.execute(
                select(User).where(User.id == user_id)
            )
            user = result.scalar_one_or_none()
            if not user:
                return None

            # Normalize email before saving
            if 'email' in data and data['email']:
                data['email'] = data['email'].strip().lower()

            # Update allowed fields
            allowed_fields = ['name', 'email', 'profile_picture_url', 'is_onboarded', 'is_active']
            for field in allowed_fields:
                if field in data:
                    setattr(user, field, data[field])

            user.updated_at = datetime.now(timezone.utc)
            await session.commit()
            await session.refresh(user)

            return self._user_to_dict(user)

    async def delete(self, user_id: str) -> bool:
        """Delete user and all related data (cascades)."""
        async with async_session_maker() as session:
            result = await session.execute(
                select(User).where(User.id == user_id)
            )
            user = result.scalar_one_or_none()
            if not user:
                return False

            await session.delete(user)
            await session.commit()
            logger.info(f"Deleted user: {user_id}")
            return True

    # Preferences
    async def get_preferences(self, user_id: str) -> Optional[dict[str, Any]]:
        """Get user preferences."""
        async with async_session_maker() as session:
            result = await session.execute(
                select(UserPreferences).where(UserPreferences.user_id == user_id)
            )
            prefs = result.scalar_one_or_none()
            if prefs:
                return self._preferences_to_dict(prefs)
            return None

    async def save_preferences(self, user_id: str, preferences: dict[str, Any]) -> dict[str, Any]:
        """Save user preferences (upsert)."""
        async with async_session_maker() as session:
            result = await session.execute(
                select(UserPreferences).where(UserPreferences.user_id == user_id)
            )
            prefs = result.scalar_one_or_none()

            if prefs:
                # Update existing preferences
                self._update_preferences_from_dict(prefs, preferences)
                prefs.updated_at = datetime.now(timezone.utc)
            else:
                # Create new preferences
                prefs = UserPreferences(
                    id=str(uuid.uuid4()),
                    user_id=user_id,
                )
                self._update_preferences_from_dict(prefs, preferences)
                session.add(prefs)

            await session.commit()
            await session.refresh(prefs)

            # Mark user as onboarded
            await session.execute(
                select(User).where(User.id == user_id)
            )
            user_result = await session.execute(
                select(User).where(User.id == user_id)
            )
            user = user_result.scalar_one_or_none()
            if user:
                user.is_onboarded = True
                await session.commit()

            return self._preferences_to_dict(prefs)

    # Family members
    async def get_family_members(self, user_id: str) -> list[dict[str, Any]]:
        """Get all family members for a user."""
        async with async_session_maker() as session:
            result = await session.execute(
                select(FamilyMember).where(FamilyMember.user_id == user_id)
            )
            members = result.scalars().all()
            return [self._family_member_to_dict(m) for m in members]

    async def add_family_member(self, user_id: str, member_data: dict[str, Any]) -> dict[str, Any]:
        """Add a family member."""
        async with async_session_maker() as session:
            member = FamilyMember(
                id=str(uuid.uuid4()),
                user_id=user_id,
                name=member_data.get("name", ""),
                age_group=member_data.get("age_group"),
                dietary_restrictions=member_data.get("dietary_restrictions"),
                health_conditions=member_data.get("health_conditions"),
            )
            session.add(member)
            await session.commit()
            await session.refresh(member)

            return self._family_member_to_dict(member)

    async def update_family_member(
        self, user_id: str, member_id: str, data: dict[str, Any]
    ) -> Optional[dict[str, Any]]:
        """Update a family member."""
        async with async_session_maker() as session:
            result = await session.execute(
                select(FamilyMember).where(
                    FamilyMember.id == member_id,
                    FamilyMember.user_id == user_id
                )
            )
            member = result.scalar_one_or_none()
            if not member:
                return None

            # Update allowed fields
            allowed_fields = ['name', 'age_group', 'dietary_restrictions', 'health_conditions']
            for field in allowed_fields:
                if field in data:
                    setattr(member, field, data[field])

            member.updated_at = datetime.now(timezone.utc)
            await session.commit()
            await session.refresh(member)

            return self._family_member_to_dict(member)

    async def delete_family_member(self, user_id: str, member_id: str) -> bool:
        """Delete a family member."""
        async with async_session_maker() as session:
            result = await session.execute(
                select(FamilyMember).where(
                    FamilyMember.id == member_id,
                    FamilyMember.user_id == user_id
                )
            )
            member = result.scalar_one_or_none()
            if not member:
                return False

            await session.delete(member)
            await session.commit()
            return True

    # Helper methods
    def _user_to_dict(self, user: User) -> dict[str, Any]:
        """Convert User model to dictionary."""
        return {
            "id": user.id,
            "firebase_uid": user.firebase_uid,
            "email": user.email,
            "name": user.name,
            "profile_picture_url": user.profile_picture_url,
            "is_onboarded": user.is_onboarded,
            "is_active": user.is_active,
            "created_at": user.created_at,
            "updated_at": user.updated_at,
        }

    def _preferences_to_dict(self, prefs: UserPreferences) -> dict[str, Any]:
        """Convert UserPreferences model to dictionary."""
        return {
            "id": prefs.id,
            "user_id": prefs.user_id,
            "dietary_type": prefs.dietary_type,
            "dietary_tags": prefs.dietary_tags,
            "allergies": prefs.allergies,
            "disliked_ingredients": prefs.disliked_ingredients,
            "cuisine_preferences": prefs.cuisine_preferences,
            "cooking_time_preference": prefs.cooking_time_preference,
            "spice_level": prefs.spice_level,
            "weekday_cooking_time_minutes": prefs.weekday_cooking_time_minutes,
            "weekend_cooking_time_minutes": prefs.weekend_cooking_time_minutes,
            "busy_days": prefs.busy_days,
            "recipe_rules": prefs.recipe_rules,
            "last_change": prefs.last_change,
            "family_size": prefs.family_size,
            "cooking_skill_level": prefs.cooking_skill_level,
            "created_at": prefs.created_at,
            "updated_at": prefs.updated_at,
        }

    def _update_preferences_from_dict(self, prefs: UserPreferences, data: dict[str, Any]) -> None:
        """Update UserPreferences model from dictionary."""
        # Map of dict keys to model attributes
        field_map = {
            "dietary_type": "dietary_type",
            "dietary_tags": "dietary_tags",
            "allergies": "allergies",
            "disliked_ingredients": "disliked_ingredients",
            "cuisine_preferences": "cuisine_preferences",
            "cooking_time_preference": "cooking_time_preference",
            "spice_level": "spice_level",
            "weekday_cooking_time_minutes": "weekday_cooking_time_minutes",
            "weekend_cooking_time_minutes": "weekend_cooking_time_minutes",
            "busy_days": "busy_days",
            "recipe_rules": "recipe_rules",
            "last_change": "last_change",
            "family_size": "family_size",
            "cooking_skill_level": "cooking_skill_level",
        }

        for key, attr in field_map.items():
            if key in data:
                setattr(prefs, attr, data[key])

    def _family_member_to_dict(self, member: FamilyMember) -> dict[str, Any]:
        """Convert FamilyMember model to dictionary."""
        return {
            "id": member.id,
            "user_id": member.user_id,
            "name": member.name,
            "age_group": member.age_group,
            "dietary_restrictions": member.dietary_restrictions,
            "health_conditions": member.health_conditions,
            "created_at": member.created_at,
            "updated_at": member.updated_at,
        }
