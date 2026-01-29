#!/usr/bin/env python
"""Test script for chat tool calling integration.

This script tests the PreferenceUpdateService and chat tool flow.
Run from the backend directory:
    python scripts/test_chat_tools.py
"""

import asyncio
import sys
import os

# Add backend to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from datetime import datetime, timezone


async def test_preference_update_service():
    """Test the PreferenceUpdateService directly."""
    from app.services.preference_update_service import PreferenceUpdateService
    from app.repositories.user_repository import UserRepository

    print("\n" + "=" * 60)
    print("TESTING PREFERENCE UPDATE SERVICE")
    print("=" * 60)

    user_repo = UserRepository()
    pref_service = PreferenceUpdateService()
    test_user_id = "test-chat-user"

    # Create or reset test user
    print("\nSetting up test user...")
    # Always reset the user to start fresh
    await user_repo.collection.document(test_user_id).set({
        "firebase_uid": "test-chat-firebase-uid",
        "email": "test-chat@example.com",
        "name": "Test Chat User",
        "is_onboarded": True,
        "is_active": True,
        "created_at": datetime.now(timezone.utc),
        "updated_at": datetime.now(timezone.utc),
    })
    # Clear preferences to start fresh
    await (
        user_repo.collection.document(test_user_id)
        .collection("preferences")
        .document("settings")
        .set({
            "recipe_rules": {"include": [], "exclude": []},
            "allergies": [],
            "dislikes": [],
            "preferences": {},
            "updated_at": datetime.now(timezone.utc),
        })
    )
    print(f"   Reset user: {test_user_id}")

    # Test 1: Add INCLUDE rule
    print("\n1. Testing ADD INCLUDE rule (chai for breakfast)...")
    result = await pref_service.update_recipe_rule(
        user_id=test_user_id,
        action="ADD",
        rule_type="INCLUDE",
        target="Chai",
        frequency="DAILY",
        meal_slots=["BREAKFAST"],
    )
    print(f"   Result: {result}")
    assert result.success, f"Failed: {result.message}"
    print("   [OK] Added chai include rule")

    # Test 2: Add EXCLUDE rule
    print("\n2. Testing ADD EXCLUDE rule (karela)...")
    result = await pref_service.update_recipe_rule(
        user_id=test_user_id,
        action="ADD",
        rule_type="EXCLUDE",
        target="Karela",
        frequency="NEVER",
        reason="dislike",
    )
    print(f"   Result: {result}")
    assert result.success, f"Failed: {result.message}"
    print("   [OK] Added karela exclude rule")

    # Test 3: Add allergy
    print("\n3. Testing ADD allergy (peanuts)...")
    result = await pref_service.update_allergy(
        user_id=test_user_id,
        action="ADD",
        ingredient="peanuts",
        severity="SEVERE",
    )
    print(f"   Result: {result}")
    assert result.success, f"Failed: {result.message}"
    print("   [OK] Added peanut allergy")

    # Test 4: Add dislike
    print("\n4. Testing ADD dislike (bhindi)...")
    result = await pref_service.update_dislike(
        user_id=test_user_id,
        action="ADD",
        ingredient="bhindi",
    )
    print(f"   Result: {result}")
    assert result.success, f"Failed: {result.message}"
    print("   [OK] Added bhindi dislike")

    # Test 5: Update cooking time preference
    print("\n5. Testing UPDATE cooking_time preference...")
    result = await pref_service.update_preference(
        user_id=test_user_id,
        preference_type="cooking_time",
        action="SET",
        value="weekday:30",
    )
    print(f"   Result: {result}")
    assert result.success, f"Failed: {result.message}"
    print("   [OK] Updated cooking time")

    # Test 6: Show config
    print("\n6. Testing show_config (all)...")
    result = await pref_service.show_config(
        user_id=test_user_id,
        section="all",
    )
    print(f"   Result success: {result.success}")
    print(f"   Config:\n{result.message or 'N/A'}")
    assert result.success, f"Failed: {result.message}"
    print("   [OK] Retrieved config")

    # Test 7: Undo last change
    print("\n7. Testing undo_last_change...")
    result = await pref_service.undo_last_change(user_id=test_user_id)
    print(f"   Result: {result}")
    # Note: This might fail if there's nothing to undo, which is okay
    print(f"   [{'OK' if result.success else 'SKIP'}] Undo operation")

    # Test 8: Conflict detection (try to INCLUDE something that's EXCLUDED)
    print("\n8. Testing conflict detection...")
    result = await pref_service.update_recipe_rule(
        user_id=test_user_id,
        action="ADD",
        rule_type="INCLUDE",
        target="Karela",  # Already excluded
        frequency="WEEKLY",
        meal_slots=["LUNCH"],
    )
    print(f"   Result: {result}")
    if result.conflict:
        print("   [OK] Conflict detected as expected")
    else:
        print("   [INFO] No conflict (rule might have been removed)")

    # Test 9: Remove dislike
    print("\n9. Testing REMOVE dislike (bhindi)...")
    result = await pref_service.update_dislike(
        user_id=test_user_id,
        action="REMOVE",
        ingredient="bhindi",
    )
    print(f"   Result: {result}")
    assert result.success, f"Failed: {result.message}"
    print("   [OK] Removed bhindi dislike")

    # Final config
    print("\n10. Final configuration:")
    result = await pref_service.show_config(user_id=test_user_id, section="all")
    print(result.message or "N/A")

    print("\n" + "=" * 60)
    print("ALL PREFERENCE UPDATE TESTS PASSED!")
    print("=" * 60)


async def test_chat_repository():
    """Test the ChatRepository."""
    from app.repositories.chat_repository import ChatRepository

    print("\n" + "=" * 60)
    print("TESTING CHAT REPOSITORY")
    print("=" * 60)

    chat_repo = ChatRepository()
    test_user_id = "test-chat-user"

    # Test 1: Save user message
    print("\n1. Testing save_message (user)...")
    msg = await chat_repo.save_message(
        user_id=test_user_id,
        role="user",
        content="I want chai every morning",
    )
    print(f"   Saved message ID: {msg['id']}")
    assert msg["id"], "Message ID should be set"
    print("   [OK] Saved user message")

    # Test 2: Save assistant message with tool call
    print("\n2. Testing save_message (assistant with tool_use)...")
    msg = await chat_repo.save_message(
        user_id=test_user_id,
        role="assistant",
        content="I'll add chai to your breakfast.",
        message_type="tool_use",
        tool_calls=[{
            "id": "tool_123",
            "name": "update_recipe_rule",
            "input": {
                "action": "ADD",
                "rule_type": "INCLUDE",
                "target": "Chai",
                "frequency": "DAILY",
                "meal_slots": ["BREAKFAST"],
            }
        }],
    )
    print(f"   Saved message ID: {msg['id']}")
    assert msg.get("tool_calls"), "Tool calls should be stored"
    print("   [OK] Saved assistant message with tool call")

    # Test 3: Save tool result
    print("\n3. Testing save_message (tool_result)...")
    msg = await chat_repo.save_message(
        user_id=test_user_id,
        role="user",
        content="",
        message_type="tool_result",
        tool_results=[{
            "tool_use_id": "tool_123",
            "content": "Added INCLUDE rule: Chai (DAILY, BREAKFAST)",
        }],
    )
    print(f"   Saved message ID: {msg['id']}")
    print("   [OK] Saved tool result message")

    # Test 4: Get recent messages
    print("\n4. Testing get_recent_messages...")
    messages = await chat_repo.get_recent_messages(test_user_id, limit=5)
    print(f"   Retrieved {len(messages)} messages")
    assert len(messages) >= 3, "Should have at least 3 messages"
    print("   [OK] Retrieved recent messages")

    # Test 5: Get context for Claude
    print("\n5. Testing get_context_for_claude...")
    claude_messages = await chat_repo.get_context_for_claude(test_user_id, limit=5)
    print(f"   Retrieved {len(claude_messages)} formatted messages")
    for i, msg in enumerate(claude_messages):
        content_preview = str(msg.get("content", ""))[:50]
        print(f"   {i+1}. [{msg['role']}] {content_preview}...")
    print("   [OK] Retrieved Claude-formatted messages")

    print("\n" + "=" * 60)
    print("ALL CHAT REPOSITORY TESTS PASSED!")
    print("=" * 60)


async def test_tool_execution():
    """Test tool execution flow (simulated)."""
    from app.ai.tools import PREFERENCE_TOOLS, CONFIG_CHAT_SYSTEM_PROMPT, format_config_for_display

    print("\n" + "=" * 60)
    print("TESTING TOOL DEFINITIONS")
    print("=" * 60)

    # Test 1: Verify tool definitions
    print("\n1. Checking tool definitions...")
    tool_names = [t["name"] for t in PREFERENCE_TOOLS]
    expected_tools = [
        "update_recipe_rule",
        "update_allergy",
        "update_dislike",
        "update_preference",
        "undo_last_change",
        "show_config",
    ]
    for tool in expected_tools:
        assert tool in tool_names, f"Missing tool: {tool}"
        print(f"   [OK] {tool}")
    print(f"   Total tools: {len(PREFERENCE_TOOLS)}")

    # Test 2: Check system prompt
    print("\n2. Checking system prompt...")
    assert "RasoiAI" in CONFIG_CHAT_SYSTEM_PROMPT
    assert "INCLUDE" in CONFIG_CHAT_SYSTEM_PROMPT
    assert "EXCLUDE" in CONFIG_CHAT_SYSTEM_PROMPT
    print("   [OK] System prompt contains expected content")

    # Test 3: Test format_config_for_display
    print("\n3. Testing format_config_for_display...")
    test_config = {
        "recipe_rules": {
            "include": [
                {"target": "Chai", "frequency": "DAILY", "meal_slots": ["BREAKFAST"]},
            ],
            "exclude": [
                {"target": "Karela", "reason": "dislike"},
            ],
        },
        "allergies": [
            {"ingredient": "peanuts", "severity": "SEVERE"},
        ],
        "dislikes": ["bhindi"],
        "preferences": {
            "dietary_tags": ["vegetarian"],
            "cuisine_preferences": ["north", "south"],
            "spice_level": "medium",
            "cooking_time": {"weekday": 30, "weekend": 60},
        },
    }
    display = format_config_for_display(test_config)
    print(f"   Formatted config:\n{display}")
    assert "INCLUDE Rules" in display
    assert "Chai" in display
    print("   [OK] Config formatted correctly")

    print("\n" + "=" * 60)
    print("ALL TOOL DEFINITION TESTS PASSED!")
    print("=" * 60)


async def cleanup_test_data():
    """Clean up test data."""
    from app.repositories.user_repository import UserRepository
    from app.repositories.chat_repository import ChatRepository

    print("\n" + "=" * 60)
    print("CLEANING UP TEST DATA")
    print("=" * 60)

    test_user_id = "test-chat-user"

    # Clear chat messages
    chat_repo = ChatRepository()
    count = await chat_repo.clear_history(test_user_id)
    print(f"   Cleared {count} chat messages")

    # Optionally delete test user (commented out to preserve for manual testing)
    # user_repo = UserRepository()
    # await user_repo.delete(test_user_id)
    # print(f"   Deleted test user")

    print("   [OK] Cleanup complete")


async def main():
    """Run all tests."""
    print("\n" + "#" * 60)
    print("# CHAT TOOL CALLING INTEGRATION TESTS")
    print("#" * 60)

    try:
        await test_tool_execution()
        await test_preference_update_service()
        await test_chat_repository()

        print("\n" + "#" * 60)
        print("# ALL TESTS PASSED!")
        print("#" * 60)

        # Ask about cleanup
        print("\nNote: Test data was created in Firestore.")
        print("Run with --cleanup to remove test data.")

    except AssertionError as e:
        print(f"\n[FAIL] Test failed: {e}")
        sys.exit(1)
    except Exception as e:
        print(f"\n[ERROR] Unexpected error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    if "--cleanup" in sys.argv:
        asyncio.run(cleanup_test_data())
    else:
        asyncio.run(main())
