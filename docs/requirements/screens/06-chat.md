# Screen 6: AI Chat

## Summary Table

| ID | Element | Behavior | Status | Test Reference |
|----|---------|----------|--------|----------------|
| CHAT-001 | Chat Screen | Display AI assistant | Implemented | `ChatScreenTest.kt` |
| CHAT-002 | Back Navigation | Return to previous | Implemented | `ChatScreenTest.kt` |
| CHAT-003 | More Options Menu | Chat settings | Implemented | `ChatScreenTest.kt` |
| CHAT-004 | AI Welcome Message | Initial greeting | Implemented | `ChatScreenTest.kt` |
| CHAT-005 | Quick Actions | Suggested prompts | Implemented | `ChatScreenTest.kt` |
| CHAT-006 | Chat History | Previous messages | Implemented | `ChatScreenTest.kt` |
| CHAT-007 | User Message Bubble | Display user input | Implemented | `ChatScreenTest.kt` |
| CHAT-008 | AI Message Bubble | Display AI response | Implemented | `ChatScreenTest.kt` |
| CHAT-009 | Recipe Links | Clickable recipe refs | Implemented | `ChatScreenTest.kt` |
| CHAT-010 | Message Input Field | Text entry | Implemented | `ChatScreenTest.kt` |
| CHAT-011 | Send Button | Submit message | Implemented | `ChatScreenTest.kt` |
| CHAT-012 | Attachment Button | Photo upload | Implemented | `ChatScreenTest.kt` |
| CHAT-013 | Voice Input Button | Speech-to-text | Implemented | `ChatScreenTest.kt` |
| CHAT-014 | Loading Indicator | AI thinking | Implemented | `ChatScreenTest.kt` |
| CHAT-015 | Clear Chat Option | Delete history | Implemented | `ChatScreenTest.kt` |
| CHAT-016 | Chat Settings Option | Configure chat | Implemented | `ChatScreenTest.kt` |
| CHAT-017 | Image Analysis | Analyze food photo | Implemented | `ChatScreenTest.kt` |
| CHAT-018 | Tool Calling | Update preferences | Implemented | `ChatViewModelTest.kt` |
| CHAT-019 | Bottom Navigation | 5 nav items | Implemented | `ChatScreenTest.kt` |
| CHAT-020 | Time-Based Suggestions | Context-aware prompts | Implemented | `ChatViewModelTest.kt` |

---

## Detailed Requirements

### CHAT-001: Chat Screen Display

| Field | Value |
|-------|-------|
| **Screen** | Chat |
| **Element** | Full screen |
| **Trigger** | Navigate from bottom nav |
| **Status** | Implemented |
| **Test** | `ChatScreenTest.kt:chatScreen_displaysCorrectly` |

**Acceptance Criteria:**
- Given: User navigates to Chat
- When: Screen displays
- Then: Header shows "RasoiAI Assistant"
- And: Chat message area fills screen
- And: Input field at bottom
- And: Bottom navigation visible

---

### CHAT-004: AI Welcome Message

| Field | Value |
|-------|-------|
| **Screen** | Chat |
| **Element** | Initial AI message |
| **Trigger** | First load / cleared chat |
| **Status** | Implemented |
| **Test** | `ChatScreenTest.kt:welcomeMessage_displaysOnFirstLoad` |

**Welcome Message:**
```
🤖 RasoiAI

Hi! I'm your AI cooking assistant. How can I help you today?

Quick actions:
[Suggest dinner] [Swap a meal] [What can I cook?] [Diet tips]
```

**Acceptance Criteria:**
- Given: Chat screen opens (no history)
- When: Screen renders
- Then: Welcome message from AI displays
- And: Quick action buttons below message
- And: Message has AI icon indicator

---

### CHAT-005: Quick Actions

| Field | Value |
|-------|-------|
| **Screen** | Chat |
| **Element** | Action chips |
| **Trigger** | Welcome message display |
| **Status** | Implemented |
| **Test** | `ChatScreenTest.kt:quickActions_sendMessageOnTap` |

**Quick Action Options:**
| Action | Sends Message |
|--------|---------------|
| Suggest dinner | "Suggest dinner for tonight" |
| Swap a meal | "I want to swap a meal" |
| What can I cook? | "What can I cook with ingredients I have?" |
| Diet tips | "Give me healthy eating tips" |

**Acceptance Criteria:**
- Given: Quick actions displayed
- When: User taps action chip
- Then: Corresponding message sent
- And: AI responds to query

---

### CHAT-006: Chat History

| Field | Value |
|-------|-------|
| **Screen** | Chat |
| **Element** | Message list |
| **Trigger** | Screen display |
| **Status** | Implemented |
| **Test** | `ChatScreenTest.kt:chatHistory_persistsAcrossSessions` |

**Acceptance Criteria:**
- Given: User has previous chat messages
- When: Chat screen opens
- Then: Previous messages displayed
- And: Ordered chronologically (oldest first)
- And: Auto-scrolls to most recent
- And: Can scroll up to see history

---

### CHAT-007: User Message Bubble

| Field | Value |
|-------|-------|
| **Screen** | Chat |
| **Element** | User message |
| **Trigger** | User sends message |
| **Status** | Implemented |
| **Test** | `ChatScreenTest.kt:userMessage_displaysOnRight` |

**Styling:**
| Property | Value |
|----------|-------|
| Alignment | Right-aligned |
| Background | Primary color (muted) |
| Icon | 👤 You |

**Acceptance Criteria:**
- Given: User sends a message
- When: Message added to chat
- Then: Message bubble on right side
- And: Shows "👤 You" label
- And: Timestamp visible

---

### CHAT-008: AI Message Bubble

| Field | Value |
|-------|-------|
| **Screen** | Chat |
| **Element** | AI response |
| **Trigger** | AI responds |
| **Status** | Implemented |
| **Test** | `ChatScreenTest.kt:aiMessage_displaysOnLeft` |

**Styling:**
| Property | Value |
|----------|-------|
| Alignment | Left-aligned |
| Background | Surface color |
| Icon | 🤖 RasoiAI |

**Acceptance Criteria:**
- Given: AI generates response
- When: Response added to chat
- Then: Message bubble on left side
- And: Shows "🤖 RasoiAI" label
- And: May include recipe links

---

### CHAT-009: Recipe Links in Messages

| Field | Value |
|-------|-------|
| **Screen** | Chat |
| **Element** | Clickable recipe buttons |
| **Trigger** | AI mentions recipes |
| **Status** | Implemented |
| **Test** | `ChatScreenTest.kt:recipeLinks_navigateToDetail` |

**Example Response:**
```
Great ingredients! Here are some recipes you can make:

1. Palak Paneer (40 min)
2. Paneer Tikka Masala (35 min)
3. Paneer Bhurji (20 min)

[View Palak Paneer]
[View Paneer Tikka Masala]
[View Paneer Bhurji]
```

**Acceptance Criteria:**
- Given: AI response contains recipe suggestions
- When: Response renders
- Then: Recipes shown as clickable buttons
- And: Tap navigates to Recipe Detail
- And: Recipe name and time displayed

---

### CHAT-010: Message Input Field

| Field | Value |
|-------|-------|
| **Screen** | Chat |
| **Element** | Text input |
| **Trigger** | User types |
| **Status** | Implemented |
| **Test** | `ChatScreenTest.kt:inputField_acceptsText` |

**Acceptance Criteria:**
- Given: Chat screen displayed
- When: User focuses input
- Then: Keyboard appears
- And: Placeholder: "Type a message..."
- And: Multi-line support
- And: Max 500 characters

---

### CHAT-012: Attachment Button (Photo Upload)

| Field | Value |
|-------|-------|
| **Screen** | Chat |
| **Element** | 📎 button |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `ChatScreenTest.kt:attachmentButton_opensPhotoPicker` |

**Acceptance Criteria:**
- Given: Input area displayed
- When: User taps 📎 button
- Then: Photo picker / camera opens
- And: User can select or take photo
- And: Photo uploaded for analysis

---

### CHAT-013: Voice Input Button

| Field | Value |
|-------|-------|
| **Screen** | Chat |
| **Element** | 🎤 button |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `ChatScreenTest.kt:voiceButton_startsRecognition` |

**Acceptance Criteria:**
- Given: Input area displayed
- When: User taps 🎤 button
- Then: Speech recognition starts
- And: Visual indicator shows listening
- And: Transcribed text fills input
- And: User can edit before sending

---

### CHAT-014: Loading Indicator

| Field | Value |
|-------|-------|
| **Screen** | Chat |
| **Element** | Thinking indicator |
| **Trigger** | Awaiting AI response |
| **Status** | Implemented |
| **Test** | `ChatScreenTest.kt:loadingIndicator_showsDuringResponse` |

**Acceptance Criteria:**
- Given: User sent message
- When: AI is processing
- Then: Typing indicator shows
- And: "RasoiAI is typing..." text
- And: Send button disabled
- And: Input field disabled

---

### CHAT-015: Clear Chat History

| Field | Value |
|-------|-------|
| **Screen** | Chat |
| **Element** | Menu option |
| **Trigger** | More menu → Clear Chat |
| **Status** | Implemented |
| **Test** | `ChatScreenTest.kt:clearChat_removesAllMessages` |

**Acceptance Criteria:**
- Given: Chat has message history
- When: User taps ⋮ → "🗑️ Clear Chat History"
- Then: Confirmation dialog appears
- And: On confirm, all messages deleted
- And: Welcome message re-displays
- And: Action is local (doesn't affect server)

---

### CHAT-017: Image Analysis (Gemini Vision)

| Field | Value |
|-------|-------|
| **Screen** | Chat |
| **Element** | Photo analysis |
| **Trigger** | User uploads photo |
| **Status** | Implemented |
| **Test** | `ChatScreenTest.kt:imageUpload_triggersAnalysis` |

**Acceptance Criteria:**
- Given: User uploads food photo
- When: Photo sent to backend
- Then: Gemini Vision API analyzes image
- And: AI responds with:
  - Identified ingredients
  - Suggested recipes
  - Nutritional estimate (if food)
- And: User can ask follow-up questions

---

### CHAT-018: Tool Calling for Preferences

| Field | Value |
|-------|-------|
| **Screen** | Chat |
| **Element** | Backend tool execution |
| **Trigger** | User requests preference change |
| **Status** | Implemented |
| **Test** | `ChatViewModelTest.kt:toolCalling_updatesPreferences` |

**Available Tools:**
| Tool | Function |
|------|----------|
| `update_recipe_rule` | Add/remove include/exclude rules |
| `update_allergy` | Manage food allergies |
| `update_dislike` | Manage disliked ingredients |
| `update_preference` | Update cooking time, dietary, cuisine |

**Example Interaction:**
```
User: "Add chai to my breakfast every day"
AI: [Calls update_recipe_rule with INCLUDE for chai, breakfast slot]
AI: "Done! I've added Chai as a required item for your breakfast."
```

**Acceptance Criteria:**
- Given: User requests preference change via chat
- When: AI determines tool call needed
- Then: Appropriate tool called via backend
- And: Confirmation message shown
- And: Changes reflected in app

---

### CHAT-020: Time-Based Quick Actions

| Field | Value |
|-------|-------|
| **Screen** | Chat |
| **Element** | Context-aware suggestions |
| **Trigger** | Screen display |
| **Status** | Implemented |
| **Test** | `ChatViewModelTest.kt:quickActions_changeByTime` |

**Time-Based Suggestions:**
| Time Range | Suggestions |
|------------|-------------|
| 6-11 AM | "Quick breakfast", "Healthy start" |
| 11 AM-4 PM | "Lunch ideas", "Light meal" |
| 4-9 PM | "Dinner suggestions", "Family meal" |
| 9 PM+ | "Light snack", "Quick bite" |

**Acceptance Criteria:**
- Given: Chat screen opens
- When: Quick actions render
- Then: Suggestions match current time of day
- And: More relevant to meal context

---

## Implementation Files

| Component | File Path |
|-----------|-----------|
| Chat Screen | `presentation/chat/ChatScreen.kt` |
| Chat ViewModel | `presentation/chat/ChatViewModel.kt` |
| Message Bubble | `presentation/chat/components/MessageBubble.kt` |
| Quick Actions | `presentation/chat/components/QuickActions.kt` |
| Chat Input | `presentation/chat/components/ChatInput.kt` |

## Test Files

| Test Type | File Path |
|-----------|-----------|
| UI Tests | `app/src/androidTest/java/com/rasoiai/app/presentation/chat/ChatScreenTest.kt` |
| Unit Tests | `app/src/test/java/com/rasoiai/app/presentation/chat/ChatViewModelTest.kt` |
| E2E Flow | `app/src/androidTest/java/com/rasoiai/app/e2e/flows/ChatFlowTest.kt` |

---

*Requirements derived from wireframe: `09-chat.md`*
