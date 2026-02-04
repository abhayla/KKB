# Screen 7: Favorites

## Summary Table

| ID | Element | Behavior | Status | Test Reference |
|----|---------|----------|--------|----------------|
| FAV-001 | Favorites Screen | Display saved recipes | Implemented | `FavoritesScreenTest.kt` |
| FAV-002 | Search Button | Filter favorites | Implemented | `FavoritesScreenTest.kt` |
| FAV-003 | Collections Section | Collection cards | Implemented | `FavoritesScreenTest.kt` |
| FAV-004 | All Collection | Default collection | Implemented | `FavoritesScreenTest.kt` |
| FAV-005 | Recently Viewed | Auto-populated collection | Implemented | `FavoritesScreenTest.kt` |
| FAV-006 | Custom Collection | User-created collection | Implemented | `FavoritesScreenTest.kt` |
| FAV-007 | Create Collection | Add new collection | Implemented | `FavoritesScreenTest.kt` |
| FAV-008 | Collection Card | Display with cover | Implemented | `FavoritesScreenTest.kt` |
| FAV-009 | Collection Selection | Filter by collection | Implemented | `FavoritesScreenTest.kt` |
| FAV-010 | Filter Bar | Diet/Cuisine/Time filters | Implemented | `FavoritesScreenTest.kt` |
| FAV-011 | All Filter | Show all recipes | Implemented | `FavoritesScreenTest.kt` |
| FAV-012 | Cuisine Filter | Filter by region | Implemented | `FavoritesScreenTest.kt` |
| FAV-013 | Time Filter | Filter by prep time | Implemented | `FavoritesScreenTest.kt` |
| FAV-014 | Recipe Grid | 2-column layout | Implemented | `FavoritesScreenTest.kt` |
| FAV-015 | Recipe Card | Display recipe info | Implemented | `FavoritesScreenTest.kt` |
| FAV-016 | Recipe Image | Show recipe photo | Implemented | `FavoritesScreenTest.kt` |
| FAV-017 | Veg Indicator | Show dietary type | Implemented | `FavoritesScreenTest.kt` |
| FAV-018 | Favorite Icon | Toggle favorite | Implemented | `FavoritesScreenTest.kt` |
| FAV-019 | More Menu | Additional actions | Implemented | `FavoritesScreenTest.kt` |
| FAV-020 | Recipe Tap | Navigate to detail | Implemented | `FavoritesScreenTest.kt` |
| FAV-021 | Reorder Button | Enter reorder mode | Implemented | `FavoritesScreenTest.kt` |
| FAV-022 | Reorder Mode | Drag to reorder | Implemented | `FavoritesScreenTest.kt` |
| FAV-023 | Done Reorder | Exit reorder mode | Implemented | `FavoritesScreenTest.kt` |
| FAV-024 | Empty State | No favorites message | Implemented | `FavoritesScreenTest.kt` |
| FAV-025 | Bottom Navigation | 5 nav items | Implemented | `FavoritesScreenTest.kt` |

---

## Detailed Requirements

### FAV-001: Favorites Screen Display

| Field | Value |
|-------|-------|
| **Screen** | Favorites |
| **Element** | Full screen |
| **Trigger** | Navigate from bottom nav |
| **Status** | Implemented |
| **Test** | `FavoritesScreenTest.kt:favoritesScreen_displaysCorrectly` |

**Acceptance Criteria:**
- Given: User navigates to Favorites
- When: Screen displays
- Then: Header shows "Favorites"
- And: Collections section at top
- And: Filter bar below
- And: Recipe grid fills remaining space
- And: Bottom navigation visible

---

### FAV-003: Collections Section

| Field | Value |
|-------|-------|
| **Screen** | Favorites |
| **Element** | Horizontal scroll of collections |
| **Trigger** | Screen display |
| **Status** | Implemented |
| **Test** | `FavoritesScreenTest.kt:collectionsSection_displaysCollections` |

**Default Collections:**
| Collection | Description | Auto-Populated |
|------------|-------------|----------------|
| All | All favorited recipes | Yes |
| Recently Viewed | Last 12 viewed recipes | Yes |

**Acceptance Criteria:**
- Given: Favorites screen displayed
- When: Collections render
- Then: Horizontal scrollable row
- And: Each collection shows cover image
- And: Name and count displayed
- And: "+ New" card at end

---

### FAV-005: Recently Viewed Collection

| Field | Value |
|-------|-------|
| **Screen** | Favorites |
| **Element** | Auto collection |
| **Trigger** | User views recipes |
| **Status** | Implemented |
| **Test** | `FavoritesScreenTest.kt:recentlyViewed_autoPopulates` |

**Acceptance Criteria:**
- Given: User views recipe details
- When: Favorites screen opens
- Then: "Recently Viewed" shows last 12 viewed
- And: Most recent first
- And: Updates automatically
- And: Does not require explicit favoriting

---

### FAV-007: Create New Collection

| Field | Value |
|-------|-------|
| **Screen** | Favorites |
| **Element** | [+New] card |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `FavoritesScreenTest.kt:createCollection_opensDialog` |

**Acceptance Criteria:**
- Given: Collections section displayed
- When: User taps "+ New"
- Then: Create Collection dialog opens
- And: Name input field
- And: Optional cover image selection
- And: Create and Cancel buttons

---

### FAV-008: Collection Card Display

| Field | Value |
|-------|-------|
| **Screen** | Favorites |
| **Element** | Collection card |
| **Trigger** | Collections render |
| **Status** | Implemented |
| **Test** | `FavoritesScreenTest.kt:collectionCard_displaysCorrectly` |

**Card Elements:**
| Element | Description |
|---------|-------------|
| Cover Image | First recipe or custom image |
| Name | Collection name |
| Count | Number of recipes |
| Checkmark | Selected indicator |

**Acceptance Criteria:**
- Given: Collection exists
- When: Card renders
- Then: Cover image from recipes
- And: Name visible below
- And: "(X)" count shown
- And: ✓ if currently selected

---

### FAV-010: Filter Bar

| Field | Value |
|-------|-------|
| **Screen** | Favorites |
| **Element** | Filter dropdowns |
| **Trigger** | Screen display |
| **Status** | Implemented |
| **Test** | `FavoritesScreenTest.kt:filterBar_displaysFilters` |

**Filters:**
| Filter | Options |
|--------|---------|
| All | Show all (default) |
| Cuisine | North, South, East, West |
| Time | <15 min, <30 min, <45 min, <60 min |

**Acceptance Criteria:**
- Given: Favorites screen displayed
- When: Filter bar renders
- Then: Three dropdown buttons
- And: Tap opens filter options
- And: Selection filters recipe grid

---

### FAV-014: Recipe Grid (2-Column)

| Field | Value |
|-------|-------|
| **Screen** | Favorites |
| **Element** | Recipe grid |
| **Trigger** | Collection/filter selection |
| **Status** | Implemented |
| **Test** | `FavoritesScreenTest.kt:recipeGrid_displays2Columns` |

**Acceptance Criteria:**
- Given: Favorites with recipes
- When: Grid renders
- Then: 2-column layout
- And: Cards equal width
- And: Consistent spacing
- And: Scrollable vertically

---

### FAV-015: Recipe Card

| Field | Value |
|-------|-------|
| **Screen** | Favorites |
| **Element** | Recipe grid item |
| **Trigger** | Grid render |
| **Status** | Implemented |
| **Test** | `FavoritesScreenTest.kt:recipeCard_displaysAllElements` |

**Card Layout:**
```
┌───────────────┐
│    [Image]    │
│               │
│ ● Dal Tadka   │
│   North       │
│   35m • 180cal│
│          ♥  ⋮ │
└───────────────┘
```

**Card Elements:**
| Element | Position | Description |
|---------|----------|-------------|
| Image | Top | Recipe photo |
| Veg indicator | On image or name | ● or 🔴 |
| Name | Below image | Recipe title |
| Cuisine | Below name | Region/type |
| Stats | Bottom-left | Time and calories |
| Favorite | Bottom-right | ♥ filled |
| More | Bottom-right | ⋮ menu |

---

### FAV-018: Favorite Toggle

| Field | Value |
|-------|-------|
| **Screen** | Favorites |
| **Element** | Heart icon on card |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `FavoritesScreenTest.kt:favoriteIcon_removesFromFavorites` |

**Acceptance Criteria:**
- Given: Recipe card displayed
- When: User taps ♥ icon
- Then: Recipe removed from favorites
- And: Card animates out of grid
- And: Undo option in snackbar

---

### FAV-019: More Menu

| Field | Value |
|-------|-------|
| **Screen** | Favorites |
| **Element** | ⋮ button on card |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `FavoritesScreenTest.kt:moreMenu_showsOptions` |

**Menu Options:**
| Option | Action |
|--------|--------|
| Add to Collection | Move/copy to collection |
| Add to Meal Plan | Add to specific meal |
| Share Recipe | Share via Android intent |
| Remove from Favorites | Delete from favorites |

---

### FAV-021: Reorder Button

| Field | Value |
|-------|-------|
| **Screen** | Favorites |
| **Element** | [Reorder] button |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `FavoritesScreenTest.kt:reorderButton_entersReorderMode` |

**Acceptance Criteria:**
- Given: Recipe grid displayed
- When: User taps "Reorder"
- Then: Enters reorder mode
- And: Button changes to "Done"
- And: Drag handles appear on cards

---

### FAV-022: Reorder Mode

| Field | Value |
|-------|-------|
| **Screen** | Favorites |
| **Element** | Drag and drop |
| **Trigger** | Reorder mode active |
| **Status** | Implemented |
| **Test** | `FavoritesScreenTest.kt:reorderMode_allowsDragDrop` |

**Reorder Mode Cards:**
```
┌───────────────┐
│ ≡  [Image]    │
│ ● Dal Tadka   │
└───────────────┘
     ↕
```

**Acceptance Criteria:**
- Given: Reorder mode active
- When: User drags card
- Then: Card follows finger
- And: Other cards reflow
- And: ≡ handle indicates draggable
- And: Order saved on Done

---

### FAV-024: Empty State

| Field | Value |
|-------|-------|
| **Screen** | Favorites |
| **Element** | Empty message |
| **Trigger** | No favorites |
| **Status** | Implemented |
| **Test** | `FavoritesScreenTest.kt:emptyState_showsMessage` |

**Empty State Content:**
```
❤️
No favorites yet

Start adding recipes you love
to see them here!

[Browse Recipes]
```

**Acceptance Criteria:**
- Given: User has no favorites
- When: Favorites screen opens
- Then: Empty state displayed
- And: Icon, message, and CTA
- And: Button navigates to Home/Recipes

---

## Implementation Files

| Component | File Path |
|-----------|-----------|
| Favorites Screen | `presentation/favorites/FavoritesScreen.kt` |
| Favorites ViewModel | `presentation/favorites/FavoritesViewModel.kt` |
| Collection Card | `presentation/favorites/components/CollectionCard.kt` |
| Recipe Grid Item | `presentation/favorites/components/RecipeGridItem.kt` |
| Filter Bar | `presentation/favorites/components/FilterBar.kt` |

## Test Files

| Test Type | File Path |
|-----------|-----------|
| UI Tests | `app/src/androidTest/java/com/rasoiai/app/presentation/favorites/FavoritesScreenTest.kt` |
| Unit Tests | `app/src/test/java/com/rasoiai/app/presentation/favorites/FavoritesViewModelTest.kt` |
| E2E Flow | `app/src/androidTest/java/com/rasoiai/app/e2e/flows/FavoritesFlowTest.kt` |

---

*Requirements derived from wireframe: `08-favorites.md`*
