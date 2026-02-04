# RasoiAI Requirements Documentation

> **Single Source of Truth** for all RasoiAI functional requirements

## Overview

This documentation system provides comprehensive requirements for every screen, button, and interactive element in RasoiAI. Each requirement follows BDD-style format (Given/When/Then) with direct links to test files.

## Quick Stats

| Metric | Count |
|--------|-------|
| Total Screens | 15 |
| Total Requirements | ~525 |
| Implemented | ~85% |
| Test Coverage | ~90% |

## Navigation

### Screen Requirements

| # | Screen | File | Requirements | Status |
|---|--------|------|--------------|--------|
| 1 | Splash & Auth | [01-splash-auth.md](screens/01-splash-auth.md) | ~25 | Implemented |
| 2 | Onboarding | [02-onboarding.md](screens/02-onboarding.md) | ~60 | Implemented |
| 3 | Home | [03-home.md](screens/03-home.md) | ~80 | Implemented |
| 4 | Recipe Detail | [04-recipe-detail.md](screens/04-recipe-detail.md) | ~50 | Implemented |
| 5 | Grocery | [05-grocery.md](screens/05-grocery.md) | ~35 | Implemented |
| 6 | Chat | [06-chat.md](screens/06-chat.md) | ~30 | Implemented |
| 7 | Favorites | [07-favorites.md](screens/07-favorites.md) | ~35 | Implemented |
| 8 | Recipe Rules | [08-recipe-rules.md](screens/08-recipe-rules.md) | ~40 | Implemented |
| 9 | Settings | [09-settings.md](screens/09-settings.md) | ~45 | Implemented |
| 10 | Stats | [10-stats.md](screens/10-stats.md) | ~30 | Implemented |
| 11 | Notifications | [11-notifications.md](screens/11-notifications.md) | ~20 | Implemented |
| 12 | Common Components | [12-common-components.md](screens/12-common-components.md) | ~25 | Implemented |

### API Requirements

| Category | File | Endpoints |
|----------|------|-----------|
| Backend API | [backend-api.md](api/backend-api.md) | 27 |

## Status Legend

| Icon | Meaning |
|------|---------|
| Implemented | Requirement fully implemented and tested |
| Partial | Partially implemented or missing tests |
| Planned | Designed but not yet implemented |
| Deferred | Moved to future phase |

## Requirement ID Format

Each requirement has a unique ID following this pattern:

```
{SCREEN}-{NUMBER}

Examples:
- SPLASH-001: First requirement for Splash screen
- HOME-042: 42nd requirement for Home screen
- API-015: 15th API endpoint requirement
```

## How to Read Requirements

Each requirement includes:

1. **Summary Table** - Quick reference at top of each file
2. **Detailed Specification** - Full BDD-style acceptance criteria
3. **Test Reference** - Link to test file that validates the requirement
4. **Notes** - Implementation notes, post-MVP additions marked with `[Added Post-MVP]`

### Example Requirement Format

```markdown
### HOME-003: Meal Card Swap Button

| Field | Value |
|-------|-------|
| **Screen** | Home |
| **Element** | Swap icon on meal item |
| **Trigger** | User taps swap icon |
| **Status** | Implemented |
| **Test** | `HomeScreenTest.kt:swapButton_whenTapped_showsSwapSheet` |

**Preconditions:**
- Meal item is NOT locked
- Day is NOT locked

**Acceptance Criteria:**
- Given: User is viewing an unlocked meal item
- When: User taps the swap icon
- Then: Swap Recipe Bottom Sheet appears with alternatives
```

## Cross-Cutting Concerns

These requirements apply across multiple screens:

### Offline Support
- All screens must display offline banner when network unavailable
- Cached data must be accessible offline
- Actions requiring network show appropriate feedback

### Authentication
- Protected screens require valid JWT token
- Token refresh handled automatically
- Expired sessions redirect to Auth screen

### Error Handling
- Network errors show retry option
- Validation errors show inline feedback
- Critical errors show error dialog with support contact

### Accessibility
- All interactive elements have content descriptions
- Minimum touch target: 48dp
- Support for TalkBack screen reader

## Updating This Documentation

When adding new features:

1. Create GitHub Issue with acceptance criteria
2. Add requirement to appropriate screen file
3. Tag with `[Added Post-MVP]` if after initial release
4. Update test file reference after implementation
5. Update status when complete

## Related Documentation

| Document | Location | Purpose |
|----------|----------|---------|
| Technical Design | `docs/design/RasoiAI Technical Design.md` | Architecture details |
| Design System | `docs/design/Design-System.md` | UI components, colors |
| E2E Test Plan | `docs/testing/E2E-Test-Plan.md` | Test coverage |
| Functional Requirements Matrix | `docs/testing/Functional-Requirement-Rule.md` | Test traceability |

## Archive

Original documentation preserved for reference:

| Document | Location |
|----------|----------|
| Original PRD | `_archive/RasoiAI Requirements.md` |
| Original Wireframes | `_archive/wireframes/` |

---

*Last Updated: February 2026*
*Version: 1.0*
