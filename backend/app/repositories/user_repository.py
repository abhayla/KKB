"""User repository for Firestore operations."""

import logging
import uuid
from datetime import datetime, timezone
from typing import Any, Optional

from app.db.firestore import Collections, get_firestore_client, doc_to_dict

logger = logging.getLogger(__name__)


class UserRepository:
    """Repository for user-related Firestore operations."""

    def __init__(self):
        self.db = get_firestore_client()
        self.collection = self.db.collection(Collections.USERS)

    async def get_by_id(self, user_id: str) -> Optional[dict[str, Any]]:
        """Get user by ID."""
        doc = await self.collection.document(user_id).get()
        if doc.exists:
            return doc_to_dict(doc)
        return None

    async def get_by_firebase_uid(self, firebase_uid: str) -> Optional[dict[str, Any]]:
        """Get user by Firebase UID."""
        query = self.collection.where("firebase_uid", "==", firebase_uid).limit(1)
        docs = await query.get()
        for doc in docs:
            return doc_to_dict(doc)
        return None

    async def create(
        self,
        firebase_uid: str,
        email: Optional[str] = None,
        name: Optional[str] = None,
        profile_picture_url: Optional[str] = None,
    ) -> dict[str, Any]:
        """Create a new user."""
        user_id = str(uuid.uuid4())
        now = datetime.now(timezone.utc)

        user_data = {
            "firebase_uid": firebase_uid,
            "email": email,
            "name": name,
            "profile_picture_url": profile_picture_url,
            "is_onboarded": False,
            "is_active": True,
            "created_at": now,
            "updated_at": now,
        }

        await self.collection.document(user_id).set(user_data)

        user_data["id"] = user_id
        logger.info(f"Created user: {user_id}")
        return user_data

    async def update(self, user_id: str, data: dict[str, Any]) -> Optional[dict[str, Any]]:
        """Update user data."""
        data["updated_at"] = datetime.now(timezone.utc)
        await self.collection.document(user_id).update(data)
        return await self.get_by_id(user_id)

    async def delete(self, user_id: str) -> bool:
        """Delete user and all subcollections."""
        doc_ref = self.collection.document(user_id)

        # Delete subcollections first
        for subcol in [Collections.USER_PREFERENCES, Collections.FAMILY_MEMBERS]:
            subcol_ref = doc_ref.collection(subcol)
            async for doc in subcol_ref.stream():
                await doc.reference.delete()

        await doc_ref.delete()
        logger.info(f"Deleted user: {user_id}")
        return True

    # Preferences subcollection
    async def get_preferences(self, user_id: str) -> Optional[dict[str, Any]]:
        """Get user preferences."""
        doc = await (
            self.collection.document(user_id)
            .collection(Collections.USER_PREFERENCES)
            .document("settings")
            .get()
        )
        if doc.exists:
            return doc_to_dict(doc)
        return None

    async def save_preferences(self, user_id: str, preferences: dict[str, Any]) -> dict[str, Any]:
        """Save user preferences."""
        preferences["updated_at"] = datetime.now(timezone.utc)
        await (
            self.collection.document(user_id)
            .collection(Collections.USER_PREFERENCES)
            .document("settings")
            .set(preferences, merge=True)
        )
        # Also mark user as onboarded
        await self.update(user_id, {"is_onboarded": True})
        return preferences

    # Family members subcollection
    async def get_family_members(self, user_id: str) -> list[dict[str, Any]]:
        """Get all family members for a user."""
        members = []
        async for doc in (
            self.collection.document(user_id)
            .collection(Collections.FAMILY_MEMBERS)
            .stream()
        ):
            members.append(doc_to_dict(doc))
        return members

    async def add_family_member(self, user_id: str, member_data: dict[str, Any]) -> dict[str, Any]:
        """Add a family member."""
        member_id = str(uuid.uuid4())
        member_data["created_at"] = datetime.now(timezone.utc)

        await (
            self.collection.document(user_id)
            .collection(Collections.FAMILY_MEMBERS)
            .document(member_id)
            .set(member_data)
        )

        member_data["id"] = member_id
        return member_data

    async def update_family_member(
        self, user_id: str, member_id: str, data: dict[str, Any]
    ) -> Optional[dict[str, Any]]:
        """Update a family member."""
        data["updated_at"] = datetime.now(timezone.utc)
        await (
            self.collection.document(user_id)
            .collection(Collections.FAMILY_MEMBERS)
            .document(member_id)
            .update(data)
        )
        doc = await (
            self.collection.document(user_id)
            .collection(Collections.FAMILY_MEMBERS)
            .document(member_id)
            .get()
        )
        return doc_to_dict(doc) if doc.exists else None

    async def delete_family_member(self, user_id: str, member_id: str) -> bool:
        """Delete a family member."""
        await (
            self.collection.document(user_id)
            .collection(Collections.FAMILY_MEMBERS)
            .document(member_id)
            .delete()
        )
        return True
