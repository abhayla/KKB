# Flow 15: Notifications Lifecycle

## Metadata

| Attribute | Value |
|-----------|-------|
| **Flow Name** | notifications-lifecycle |
| **Flow ID** | FLOW-15 |
| **Goal** | Test full notification lifecycle — creation, display, read, filter, delete |
| **Priority** | P1 |
| **Complexity** | Medium |
| **Estimated Duration** | 8-12 minutes |
| **Last Updated** | 2026-02-14 |

## Prerequisites

- Backend API running (`uvicorn app.main:app --reload`)
- PostgreSQL database with schema applied (`alembic upgrade head`)
- Android emulator running (API 34)
- Backend notification triggers implemented (Phase 2)
- User authenticated via fake-firebase-token
- At least one meal plan generated (for "meal plan ready" notification)

## Depends On

- **Flow 01** (new-user-journey) — Must complete authentication, onboarding, and generate at least one meal plan

## Test User Persona

**Sharma Family — Post-Meal Plan Generation**

| Field | Value |
|-------|-------|
| Email | `e2e-test@rasoiai.test` |
| Auth Token | `fake-firebase-token` |
| Display Name | Abhay Sharma |
| State | Has completed onboarding, has 1 active meal plan |

**Expected Notifications:**
- "Your weekly meal plan is ready!" (after meal plan generation)
- Cooking streak milestone notifications (after logging cooked recipes)
- Achievement unlock notifications (optional, depends on Phase 3 implementation)

## Test Phases

### Phase A: Verify Initial Notification After Meal Plan

| Step | Action | Verification | Type |
|------|--------|--------------|------|
| A1 | Verify precondition: meal plan exists | Home screen shows meal cards for current week | UI |
| A2 | Navigate to Home screen | Home displays | UI |
| A3 | Verify notification badge visible on Home app bar | Badge count >= 1 (indicates unread notification) | UI |
| A4 | Screenshot: `flow15_home_with_badge.png` | Badge present | UI |

### Phase B: Navigate to Notifications Screen

| Step | Action | Verification | Type |
|------|--------|--------------|------|
| B1 | Tap notification bell icon in app bar | Notifications screen displays | UI |
| B2 | Verify screen title: "Notifications" | Title visible | UI |
| B3 | Verify filter tabs present: "All" and "Unread" | Both tabs visible | UI |
| B4 | Verify at least 1 notification present | Notification list has >= 1 item | UI |
| B5 | Verify notification card has: title, body, timestamp | All 3 fields visible on notification card | UI |
| B6 | Verify first notification is "Your weekly meal plan is ready!" | Title matches | UI |
| B7 | Verify notification has visual unread indicator (bold text or colored background) | Unread state visible | UI |
| B8 | Screenshot: `flow15_notifications_screen.png` | Screen captured | UI |

### Phase C: Mark Notification as Read

| Step | Action | Verification | Type |
|------|--------|--------------|------|
| C1 | Tap on the "meal plan ready" notification card | Notification detail appears OR notification marked as read | UI |
| C2 | Verify notification visual state changes (no longer bold/highlighted) | Read state applied | UI |
| C3 | Tap "Unread" filter tab | Unread list updates (should be empty or have fewer items) | UI |
| C4 | Verify "meal plan ready" notification NOT present in Unread list | Notification filtered out | UI |
| C5 | Tap "All" filter tab | All notifications visible again, including the now-read notification | UI |
| C6 | Verify notification badge count on Home decreased by 1 | Navigate to Home → verify badge count reduced | UI |
| C7 | Screenshot: `flow15_notification_marked_read.png` | Read state visible | UI |

### Phase D: Create Additional Notifications via Cooking

| Step | Action | Verification | Type |
|------|--------|--------------|------|
| D1 | Navigate to Home screen | Home displays | UI |
| D2 | Tap on a breakfast meal card → "View Recipe" | Recipe Detail screen displays | UI |
| D3 | Tap "Start Cooking" | Cooking Mode screen displays | UI |
| D4 | Complete all cooking steps (tap Next until "Finish") | All steps completed | UI |
| D5 | Tap "Finish Cooking" | Cooking completion dialog appears | UI |
| D6 | Confirm cooking completion | Navigate back to Home | UI |
| D7 | Wait 2-3 seconds for backend notification trigger | Backend creates streak milestone notification | UI |
| D8 | Verify notification badge count increased | Badge count >= 1 | UI |
| D9 | Navigate to Notifications screen | New notification appears: "You've cooked your first meal!" OR "Cooking streak: 1 day" | UI |
| D10 | Screenshot: `flow15_new_notification_after_cooking.png` | New notification visible | UI |

### Phase E: Mark All as Read

| Step | Action | Verification | Type |
|------|--------|--------------|------|
| E1 | Verify multiple unread notifications present (at least 2) | Unread count >= 2 | UI |
| E2 | Tap "Mark all as read" button (in app bar overflow menu or action button) | All notifications marked as read | UI |
| E3 | Verify all notification cards show read state (no bold text) | All read | UI |
| E4 | Tap "Unread" filter tab | Empty state: "No unread notifications" | UI |
| E5 | Verify notification badge count on Home = 0 | Navigate to Home → verify badge gone or shows 0 | UI |
| E6 | Screenshot: `flow15_all_marked_read.png` | All read state visible | UI |

### Phase F: Filter All vs Unread

| Step | Action | Verification | Type |
|------|--------|--------------|------|
| F1 | Navigate to Notifications screen | Notifications screen displays | UI |
| F2 | Verify "All" tab is selected by default | All tab highlighted | UI |
| F3 | Verify all notifications visible (both read and unread) | List has all notification items | UI |
| F4 | Tap "Unread" tab | Only unread notifications visible | UI |
| F5 | Verify read notifications NOT present in Unread list | List filtered correctly | UI |
| F6 | Create a new notification (cook another recipe OR regenerate meal plan) | New unread notification appears | UI |
| F7 | Tap "Unread" tab → verify new notification appears | New item visible in Unread | UI |
| F8 | Tap "All" tab → verify new notification + old read notifications | All items visible | UI |
| F9 | Screenshot: `flow15_filter_unread.png` | Unread filter active | UI |

### Phase G: Delete a Notification

| Step | Action | Verification | Type |
|------|--------|--------------|------|
| G1 | Navigate to Notifications → "All" tab | All notifications visible | UI |
| G2 | Long-press OR swipe left on a notification card | Delete action appears (trash icon or swipe-to-delete) | UI |
| G3 | Tap delete icon OR complete swipe | Confirmation dialog appears: "Delete this notification?" | UI |
| G4 | Confirm deletion | Dialog closes, notification removed from list | UI |
| G5 | Verify notification no longer present in "All" tab | Item removed | UI |
| G6 | Verify notification badge count updated (if deleted notification was unread) | Badge count decremented if unread | UI |
| G7 | Screenshot: `flow15_notification_deleted.png` | Notification removed | UI |

### Phase H: Verify Notification Badge Count Accuracy

| Step | Action | Verification | Type |
|------|--------|--------------|------|
| H1 | Navigate to Home screen | Home displays | UI |
| H2 | Note current notification badge count (let's say N) | Badge shows N | UI |
| H3 | Navigate to Notifications → tap "Unread" | Count unread notifications manually (should = N) | UI |
| H4 | Mark 1 notification as read (tap it) | Notification state changes to read | UI |
| H5 | Navigate to Home → verify badge count = N-1 | Badge count decreased | UI |
| H6 | Delete 1 unread notification | Notification removed | UI |
| H7 | Navigate to Home → verify badge count = N-2 | Badge count decreased again | UI |
| H8 | Create new notification (regenerate meal plan OR cook recipe) | New notification created | UI |
| H9 | Navigate to Home → verify badge count = N-1 | Badge count increased | UI |
| H10 | Screenshot: `flow15_badge_accuracy.png` | Badge count correct | UI |

## Backend API Verification

After Phase H, verify backend data persistence:

```bash
# Get JWT
JWT=$(curl -s -X POST http://localhost:8000/api/v1/auth/firebase \
  -H 'Content-Type: application/json' \
  -d '{"firebase_token":"fake-firebase-token"}' | \
  python -c 'import sys,json;print(json.load(sys.stdin).get("access_token",""))')

# Get all notifications
curl -s -H "Authorization: Bearer $JWT" \
  http://localhost:8000/api/v1/notifications | jq '.'

# Verify notifications match app state (count, read status)
curl -s -H "Authorization: Bearer $JWT" \
  http://localhost:8000/api/v1/notifications | \
  python -c "
import sys, json
notifs = json.load(sys.stdin)
total = len(notifs)
unread = sum(1 for n in notifs if not n.get('is_read', False))
print(f'Total: {total}, Unread: {unread}')
"

# Mark all as read via API
curl -s -X POST -H "Authorization: Bearer $JWT" \
  http://localhost:8000/api/v1/notifications/mark-all-read

# Delete a notification by ID
NOTIF_ID="<notification_id>"
curl -s -X DELETE -H "Authorization: Bearer $JWT" \
  http://localhost:8000/api/v1/notifications/$NOTIF_ID
```

## Contradictions

This flow tests notification-specific contradictions:

| ID | Contradiction | Setup | Expected Behavior | Type |
|----|---------------|-------|-------------------|------|
| **C40** | Notification already read | User taps same notification twice | Second tap does not re-mark as unread; notification stays in read state | UI |
| **C41** | Delete only notification | User has 1 notification, deletes it | Empty state appears: "No notifications yet", badge count = 0 | UI |
| **C42** | Filter with no unread | All notifications marked as read | "Unread" tab shows empty state: "No unread notifications" | UI |

## Fix Strategy

**Relevant files for this flow:**

- **Android:**
  - `app/presentation/notifications/NotificationsScreen.kt` — UI with tabs, list, filter
  - `app/presentation/notifications/NotificationsViewModel.kt` — State management
  - `domain/model/Notification.kt` — Domain model
  - `data/local/entity/NotificationEntity.kt` — Room entity
  - `data/local/dao/NotificationDao.kt` — Database queries
  - `data/repository/NotificationRepositoryImpl.kt` — Repository

- **Backend:**
  - `app/api/v1/endpoints/notifications.py` — CRUD endpoints
  - `app/models/notification.py` — SQLAlchemy model
  - `app/services/notification_service.py` — Business logic
  - `app/services/ai_meal_service.py` — Creates "meal plan ready" notification

**Common issues:**

- **Badge count mismatch:** Ensure `NotificationDao.getUnreadCount()` query is correct: `SELECT COUNT(*) FROM notifications WHERE is_read = 0`
- **Filter not working:** Verify `NotificationDao.getUnreadNotifications()` query excludes read notifications
- **Mark all as read fails:** Ensure backend endpoint `POST /api/v1/notifications/mark-all-read` updates all user notifications
- **Deletion fails:** Verify backend endpoint `DELETE /api/v1/notifications/{id}` returns 204 and removes from DB

## Screen Coverage

| Screen | Step(s) | Verification |
|--------|---------|-------------|
| Home | A1-A4, C6, E5, H1-H10 | Notification badge count |
| Notifications | B1-B8, C1-C7, D9-D10, E1-E6, F1-F9, G1-G7, H3-H4 | List, filter tabs, read/unread state, delete |
| Recipe Detail | D2 | Access Cooking Mode |
| Cooking Mode | D3-D6 | Complete cooking to trigger notification |

## Test Data Cleanup

After test completion:

```bash
# Remove test user and all notifications
PYTHONPATH=. python scripts/cleanup_user.py
```

## Notes

- Notification types: MEAL_PLAN_READY, COOKING_STREAK, ACHIEVEMENT_UNLOCKED, GROCERY_REMINDER (future)
- Notifications are created server-side via `notification_service.create_notification()` after specific events
- Android app fetches notifications via `GET /api/v1/notifications` on app start + periodic sync
- Badge count calculated from Room DB `notification` table (offline-first)
- Delete is local-first (immediate UI update) + backend sync (delete via API when online)
