# Flow 04: Chat AI

## Metadata
- **Flow Name:** `chat-ai`
- **Goal:** Test AI chat queries, tool calling, contradictions C6-C12, and verify changes persist
- **Preconditions:** Authenticated user with preferences and meal plan
- **Estimated Duration:** 8-15 minutes (AI responses take 5-30s each)
- **Screens Covered:** Chat, Home, Settings
- **Depends On:** none (needs authenticated user with preferences)
- **State Produced:** Chat history, potential preference/rule changes via tool calling

## Prerequisites

Beyond standard D1-D7 prerequisites:
- [ ] User authenticated with meal plan
- [ ] Backend running with Claude API key configured (chat uses Claude)
- [ ] Allow 30 seconds per AI response

## Test User Persona

Uses existing Sharma family data. Chat will attempt preference modifications.

## Steps

### Phase A: Basic Chat Interaction (Steps 1-5)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| A1 | Tap bottom nav "Chat" | Chat screen with input field | `flow04_chat_screen.png` | — |
| A2 | Verify welcome/intro message | "Hi", "Hello", or assistant greeting in chat | — | — |
| A2a | Verify attachment button exists | content-desc "Attach photo" or "Attachment" in XML | — | — |
| A2b | Tap attachment button | Image Source Dialog (Camera/Gallery) appears | — | — |
| A2c | Press BACK to dismiss | Return to Chat | — | — |
| A2d | Verify voice input button exists | content-desc "Voice input" or "Voice" in XML | — | — |
| A2e | Tap voice input button | Speech recognizer intent launches (may show permission) | — | — |
| A2f | Press BACK to dismiss | Return to Chat | — | — |
| A2g | Verify quick action chips | Suggestion chips visible below welcome message | — | — |
| A2h | Tap a quick action chip | Message auto-sent or auto-filled | `flow04_chip_action.png` | — |
| A3 | Tap input field, type "What's for breakfast tomorrow?" | Text appears in input | — | — |
| A4 | Dismiss keyboard if blocking Send, tap Send | Message sent, appears in chat | — | — |
| A5 | Wait up to 30s for AI response | AI responds with breakfast information | `flow04_chat_response.png` | — |

### Phase B: Tool Calling — Rule Addition (Steps 6-8)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| B1 | Type "Add Paneer to dinner" and send | AI acknowledges, uses `update_recipe_rule` tool | — | — |
| B2 | Wait for response (up to 30s) | AI confirms rule added (INCLUDE Paneer for dinner) | `flow04_add_paneer.png` | — |
| B3 | Verify rule created: navigate to Recipe Rules later | Rule should appear in list | — | Deferred |

### Phase C: Contradictions C6-C12 (Steps 9-20)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| C1 | **C6:** Type "I'm allergic to Paneer" and send | AI should warn: "You just added Paneer as an INCLUDE rule" or remove it | `flow04_c6_conflict.png` | — |
| C2 | Wait for response | AI handles conflict (warns, asks for clarification, or auto-resolves) | — | — |
| C3 | **C7:** Type "Make my meals extra spicy" and send | AI updates spice preference | — | — |
| C4 | Wait for response | AI confirms spice change | — | — |
| C5 | Type "Actually, make everything mild" and send | AI reverses spice to Mild | `flow04_c7_reversal.png` | — |
| C6 | Wait for response | Second update wins — confirms Mild | — | — |
| C7 | **C8:** Type "Add Chicken to lunch" and send (diet=Vegetarian) | AI should warn about vegetarian conflict | `flow04_c8_diet.png` | — |
| C8 | Wait for response | AI flags contradiction or asks confirmation | — | — |
| C9 | **C9:** Type "I'm vegan now" and send | AI updates dietary preference | — | — |
| C10 | Type "Add butter chicken to my meals" and send | AI should flag vegan + non-veg contradiction | `flow04_c9_vegan.png` | — |
| C11 | Wait for response | AI warns about contradiction | — | — |
| C12 | **C10:** Type "Add daal/lentil (mom's recipe)" and send | AI parses intent despite special chars | `flow04_c10_special.png` | — |
| C13 | Wait for response | No crash, AI responds meaningfully | — | — |
| C14 | **C11:** Clear input, leave empty, verify Send state | Send button should be disabled or show validation | — | — |
| C15 | **C12:** Type a 200+ word message about meal preferences | Long text accepted, no truncation | — | — |
| C16 | Tap Send | Message sent successfully | `flow04_c12_long.png` | — |
| C17 | Wait for AI response | AI responds to the full message | — | — |

### Phase D: Verify Changes Persisted (Steps 21-25)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| D1 | Tap bottom nav "Home" | Home screen | — | — |
| D2 | Navigate to Settings (Profile icon) | Settings screen | — | — |
| D3 | Check dietary preference | Should reflect latest chat change (may be Vegan from C9) | `flow04_settings_after_chat.png` | — |
| D4 | Check spice level | Should be Mild (from C5/C6 reversal) | — | — |
| D5 | Navigate to Recipe Rules | Check for Paneer rule (may have been removed by C6 allergy conflict) | `flow04_rules_after_chat.png` | — |

### Backend API Cross-Validation: Chat Effects

```bash
# 1. Verify chat history persisted
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/chat/history | \
  python -c "
import sys, json
d = json.load(sys.stdin)
messages = d if isinstance(d, list) else d.get('messages', [])
print(f'Chat history: {len(messages)} messages')
user_msgs = [m for m in messages if m.get('role') == 'user']
asst_msgs = [m for m in messages if m.get('role') == 'assistant']
print(f'  User messages: {len(user_msgs)}')
print(f'  Assistant messages: {len(asst_msgs)}')
tool_msgs = [m for m in messages if m.get('tool_calls')]
print(f'  Messages with tool calls: {len(tool_msgs)}')
"

# 2. Verify rule changes from chat
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/recipe-rules | \
  python -c "
import sys, json
d = json.load(sys.stdin)
rules = d if isinstance(d, list) else d.get('rules', [])
print(f'Total rules: {len(rules)}')
for r in rules:
    print(f'  {r.get(\"action\")}: {r.get(\"target_name\")} for {r.get(\"meal_slot\", \"ANY\")}')
"

# 3. Verify preference changes from chat
curl -s -H "Authorization: Bearer $JWT" http://localhost:8000/api/v1/users/me | \
  python -c "
import sys, json
d = json.load(sys.stdin)
print(f'dietary_type: {d.get(\"dietary_type\")}')
print(f'spice_level: {d.get(\"spice_level\")}')
print(f'allergies: {d.get(\"allergies\")}')
"
```

## Validation Checkpoints

No `validate_meal_plan.py` checkpoints — validation is chat response quality:
- AI responses are contextual (about food/meals)
- Tool calling works (rules/preferences updated)
- Contradictions handled gracefully (warning, not crash)
- Changes persist in Settings/Rules after chat

## Fix Strategy

**Relevant files for this flow:**
- Chat screen: `app/presentation/chat/ChatViewModel.kt`, `ChatScreen.kt`
- Chat backend: `backend/app/ai/chat_assistant.py`, `backend/app/api/v1/endpoints/chat.py`
- Tool calling: `backend/app/ai/chat_assistant.py` (update_recipe_rule, update_allergy, etc.)
- Rule creation: `backend/app/api/v1/endpoints/recipe_rules.py`
- Preference updates: `backend/app/services/user_service.py`

**Common issues:**
- AI timeout → Claude API takes 5-30s, ensure 30s wait
- Tool calling fails silently → check backend logs for tool execution errors
- Chat history lost on navigation → verify Room ChatDao persistence
- Keyboard covers Send button → dismiss keyboard, scroll to bottom

## Screen Coverage

| Screen | Step(s) | Verification |
|--------|---------|-------------|
| Chat | A1-A5, B1-B2, C1-C17 | Messages, AI responses, tool calling, contradictions |
| Home | D1 | Navigation return |
| Settings | D2-D4 | Preference persistence |
| Recipe Rules | D5 | Rule persistence from chat |
