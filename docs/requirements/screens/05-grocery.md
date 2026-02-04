# Screen 5: Grocery List

## Summary Table

| ID | Element | Behavior | Status | Test Reference |
|----|---------|----------|--------|----------------|
| GRO-001 | Grocery Screen | Display grocery list | Implemented | `GroceryScreenTest.kt` |
| GRO-002 | Back Navigation | Return to Home | Implemented | `GroceryScreenTest.kt` |
| GRO-003 | More Options Menu | Additional actions | Implemented | `GroceryScreenTest.kt` |
| GRO-004 | Week Date Header | Show plan week | Implemented | `GroceryScreenTest.kt` |
| GRO-005 | Item Count | Total items display | Implemented | `GroceryScreenTest.kt` |
| GRO-006 | WhatsApp Share Button | Share to WhatsApp | Implemented | `GroceryScreenTest.kt` |
| GRO-007 | Category Section | Collapsible category | Implemented | `GroceryScreenTest.kt` |
| GRO-008 | Category Header | Name and count | Implemented | `GroceryScreenTest.kt` |
| GRO-009 | Category Expand/Collapse | Toggle visibility | Implemented | `GroceryScreenTest.kt` |
| GRO-010 | Grocery Item Row | Checkbox, name, quantity | Implemented | `GroceryScreenTest.kt` |
| GRO-011 | Item Checkbox | Toggle purchased | Implemented | `GroceryScreenTest.kt` |
| GRO-012 | Item Name | Ingredient name | Implemented | `GroceryScreenTest.kt` |
| GRO-013 | Item Quantity | Amount and unit | Implemented | `GroceryScreenTest.kt` |
| GRO-014 | Item Swipe Edit | Reveal edit action | Implemented | `GroceryScreenTest.kt` |
| GRO-015 | Item Swipe Delete | Reveal delete action | Implemented | `GroceryScreenTest.kt` |
| GRO-016 | Edit Quantity Dialog | Modify amount | Implemented | `GroceryScreenTest.kt` |
| GRO-017 | Add Custom Item | Manual add button | Implemented | `GroceryScreenTest.kt` |
| GRO-018 | Add Item Dialog | Enter new item | Implemented | `GroceryScreenTest.kt` |
| GRO-019 | WhatsApp Share Sheet | Preview and share | Implemented | `GroceryScreenTest.kt` |
| GRO-020 | Share Full List | All items option | Implemented | `GroceryScreenTest.kt` |
| GRO-021 | Share Unpurchased | Unchecked only option | Implemented | `GroceryScreenTest.kt` |
| GRO-022 | Bottom Navigation | 5 nav items | Implemented | `GroceryScreenTest.kt` |
| GRO-023 | Auto-Aggregation | Combine duplicates | Implemented | `GroceryViewModelTest.kt` |
| GRO-024 | Meal Plan Sync | Update with plan changes | Implemented | `GroceryViewModelTest.kt` |
| GRO-025 | Offline Persistence | Work without network | Implemented | `GroceryViewModelTest.kt` |

---

## Detailed Requirements

### GRO-001: Grocery Screen Display

| Field | Value |
|-------|-------|
| **Screen** | Grocery |
| **Element** | Full screen |
| **Trigger** | Navigate from bottom nav |
| **Status** | Implemented |
| **Test** | `GroceryScreenTest.kt:groceryScreen_displaysCorrectly` |

**Acceptance Criteria:**
- Given: User has meal plan generated
- When: Grocery screen displays
- Then: Header shows "Grocery List"
- And: Week date range displayed
- And: Total item count shown
- And: Categorized items below
- And: Bottom navigation visible

---

### GRO-004: Week Date Header

| Field | Value |
|-------|-------|
| **Screen** | Grocery |
| **Element** | Week info text |
| **Trigger** | Screen display |
| **Status** | Implemented |
| **Test** | `GroceryScreenTest.kt:weekHeader_displaysDateRange` |

**Acceptance Criteria:**
- Given: Grocery list from meal plan
- When: Header displays
- Then: Shows "Week of [Start] - [End]"
- And: Example: "Week of Jan 20-26"

---

### GRO-006: WhatsApp Share Button

| Field | Value |
|-------|-------|
| **Screen** | Grocery |
| **Element** | Share button |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `GroceryScreenTest.kt:whatsappButton_opensShareSheet` |

**Acceptance Criteria:**
- Given: Grocery list displayed
- When: User taps "📱 Share via WhatsApp"
- Then: WhatsApp Share Sheet opens
- And: Shows formatted preview
- And: Share options displayed

---

### GRO-007: Category Section

| Field | Value |
|-------|-------|
| **Screen** | Grocery |
| **Element** | Category container |
| **Trigger** | Screen display |
| **Status** | Implemented |
| **Test** | `GroceryScreenTest.kt:categorySections_displayCorrectly` |

**Categories:**
| Category | Icon | Hindi |
|----------|------|-------|
| Vegetables | 🥬 | सब्जी |
| Dairy | 🥛 | दूध/दही |
| Pulses & Grains | 🌾 | दाल/अनाज |
| Spices & Masala | 🌶️ | मसाले |
| Other | 🥫 | अन्य |

**Acceptance Criteria:**
- Given: Grocery list has items
- When: Categories render
- Then: Items grouped by category
- And: Each category collapsible
- And: Item count shown per category

---

### GRO-009: Category Expand/Collapse

| Field | Value |
|-------|-------|
| **Screen** | Grocery |
| **Element** | Category toggle |
| **Trigger** | Tap on category header |
| **Status** | Implemented |
| **Test** | `GroceryScreenTest.kt:categoryToggle_expandsCollapses` |

**Acceptance Criteria:**
- Given: Category is expanded
- When: User taps category header
- Then: Items collapse (hide)
- And: Arrow icon rotates (▼ → ▶)
- And: Tap again expands

---

### GRO-010: Grocery Item Row

| Field | Value |
|-------|-------|
| **Screen** | Grocery |
| **Element** | Item row |
| **Trigger** | Category expanded |
| **Status** | Implemented |
| **Test** | `GroceryScreenTest.kt:itemRow_displaysCorrectly` |

**Row Layout:**
```
□ Onion                    1 kg
```

| Element | Position | Description |
|---------|----------|-------------|
| Checkbox | Left | Purchase status |
| Name | Center-left | Ingredient name |
| Quantity | Right | Amount + unit |

---

### GRO-011: Item Checkbox Toggle

| Field | Value |
|-------|-------|
| **Screen** | Grocery |
| **Element** | Checkbox |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `GroceryScreenTest.kt:itemCheckbox_togglesPurchased` |

**Acceptance Criteria:**
- Given: Item row displayed
- When: User taps checkbox
- Then: Checkbox toggles (□ ↔ ☑)
- And: Item text may strikethrough
- And: State persists locally
- And: Syncs when online

---

### GRO-014: Swipe to Edit

| Field | Value |
|-------|-------|
| **Screen** | Grocery |
| **Element** | Swipe action - Edit |
| **Trigger** | Swipe left on item |
| **Status** | Implemented |
| **Test** | `GroceryScreenTest.kt:swipeLeft_revealsEdit` |

**Acceptance Criteria:**
- Given: Item row displayed
- When: User swipes left
- Then: Edit (✏️) button revealed
- And: Tap opens Edit Quantity Dialog

---

### GRO-015: Swipe to Delete

| Field | Value |
|-------|-------|
| **Screen** | Grocery |
| **Element** | Swipe action - Delete |
| **Trigger** | Swipe left on item |
| **Status** | Implemented |
| **Test** | `GroceryScreenTest.kt:swipeLeft_revealsDelete` |

**Acceptance Criteria:**
- Given: Item row displayed
- When: User swipes left fully
- Then: Delete (🗑️) button revealed
- And: Tap removes item
- And: Undo option in snackbar

---

### GRO-016: Edit Quantity Dialog

| Field | Value |
|-------|-------|
| **Screen** | Grocery |
| **Element** | Edit modal |
| **Trigger** | Edit action tapped |
| **Status** | Implemented |
| **Test** | `GroceryScreenTest.kt:editDialog_updatesQuantity` |

**Dialog Fields:**
| Field | Type | Default |
|-------|------|---------|
| Quantity | Number input | Current value |
| Unit | Dropdown | Current unit |

**Acceptance Criteria:**
- Given: Edit dialog open
- When: User changes quantity
- Then: New value saved on confirm
- And: Item row updates
- And: Cancel discards changes

---

### GRO-017: Add Custom Item Button

| Field | Value |
|-------|-------|
| **Screen** | Grocery |
| **Element** | Add button |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `GroceryScreenTest.kt:addCustomItem_opensDialog` |

**Acceptance Criteria:**
- Given: Grocery screen displayed
- When: User taps "+ Add custom item"
- Then: Add Item Dialog opens
- And: Button at bottom of list

---

### GRO-018: Add Item Dialog

| Field | Value |
|-------|-------|
| **Screen** | Grocery |
| **Element** | Add modal |
| **Trigger** | Add button tapped |
| **Status** | Implemented |
| **Test** | `GroceryScreenTest.kt:addDialog_createsItem` |

**Dialog Fields:**
| Field | Type | Required |
|-------|------|----------|
| Item Name | Text input | Yes |
| Quantity | Number input | Yes |
| Unit | Dropdown | Yes |
| Category | Dropdown | No (auto-detect) |

**Acceptance Criteria:**
- Given: Add dialog open
- When: User fills fields and confirms
- Then: New item added to list
- And: Marked as manually added
- And: Item appears in appropriate category

---

### GRO-019: WhatsApp Share Sheet

| Field | Value |
|-------|-------|
| **Screen** | Grocery |
| **Element** | Share bottom sheet |
| **Trigger** | Share button tapped |
| **Status** | Implemented |
| **Test** | `GroceryScreenTest.kt:shareSheet_displaysPreview` |

**Preview Format:**
```
🛒 *Grocery List*
Week: Jan 20-26

*🥬 Vegetables*
• Onion - 1 kg
• Tomato - 500g
• Potato - 1 kg
...

*🥛 Dairy*
• Milk - 2 L
• Dahi - 500g
...

_Sent from RasoiAI_ 🍳
```

**Acceptance Criteria:**
- Given: Share sheet open
- When: Preview renders
- Then: Formatted list shown
- And: Markdown for WhatsApp bold/italic
- And: Categories as headers

---

### GRO-020: Share Full List Option

| Field | Value |
|-------|-------|
| **Screen** | Grocery |
| **Element** | Radio option |
| **Trigger** | Share sheet display |
| **Status** | Implemented |
| **Test** | `GroceryScreenTest.kt:shareFullList_includesAllItems` |

**Acceptance Criteria:**
- Given: Share sheet open
- When: "Full list (X items)" selected
- Then: All items included in share
- And: Both purchased and unpurchased

---

### GRO-021: Share Unpurchased Only Option

| Field | Value |
|-------|-------|
| **Screen** | Grocery |
| **Element** | Radio option |
| **Trigger** | Share sheet display |
| **Status** | Implemented |
| **Test** | `GroceryScreenTest.kt:shareUnpurchased_excludesChecked` |

**Acceptance Criteria:**
- Given: Share sheet open
- When: "Unpurchased only (Y items)" selected
- Then: Only unchecked items included
- And: Count reflects unchecked count

---

### GRO-023: Auto-Aggregation

| Field | Value |
|-------|-------|
| **Screen** | Grocery |
| **Element** | List generation logic |
| **Trigger** | Meal plan generation |
| **Status** | Implemented |
| **Test** | `GroceryViewModelTest.kt:aggregation_combinesDuplicates` |

**Acceptance Criteria:**
- Given: Multiple recipes use same ingredient
- When: Grocery list generated
- Then: Ingredients combined with total quantity
- And: Example: Onion (2 recipes) → Onion - 1.5 kg
- And: Duplicates removed

---

### GRO-024: Meal Plan Sync

| Field | Value |
|-------|-------|
| **Screen** | Grocery |
| **Element** | Sync logic |
| **Trigger** | Meal plan changes |
| **Status** | Implemented |
| **Test** | `GroceryViewModelTest.kt:mealPlanChange_updatesGroceryList` |

**Acceptance Criteria:**
- Given: Meal plan modified (swap, add, remove)
- When: Change saved
- Then: Grocery list recalculates
- And: Checked items preserved if still needed
- And: Removed items flagged (keep if purchased)

---

### GRO-025: Offline Persistence

| Field | Value |
|-------|-------|
| **Screen** | Grocery |
| **Element** | Local storage |
| **Trigger** | Network unavailable |
| **Status** | Implemented |
| **Test** | `GroceryViewModelTest.kt:offlineMode_persists` |

**Acceptance Criteria:**
- Given: Device is offline
- When: User views/modifies grocery list
- Then: All data available from Room cache
- And: Checkbox changes saved locally
- And: Sync to server when online

---

## Implementation Files

| Component | File Path |
|-----------|-----------|
| Grocery Screen | `presentation/grocery/GroceryScreen.kt` |
| Grocery ViewModel | `presentation/grocery/GroceryViewModel.kt` |
| Category Section | `presentation/grocery/components/CategorySection.kt` |
| Grocery Item Row | `presentation/grocery/components/GroceryItemRow.kt` |
| Share Sheet | `presentation/grocery/components/WhatsAppShareSheet.kt` |

## Test Files

| Test Type | File Path |
|-----------|-----------|
| UI Tests | `app/src/androidTest/java/com/rasoiai/app/presentation/grocery/GroceryScreenTest.kt` |
| Unit Tests | `app/src/test/java/com/rasoiai/app/presentation/grocery/GroceryViewModelTest.kt` |
| E2E Flow | `app/src/androidTest/java/com/rasoiai/app/e2e/flows/GroceryFlowTest.kt` |

---

*Requirements derived from wireframe: `07-grocery-list.md`*
