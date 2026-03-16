---
description: >
  Enforce the offline-first mutation pattern: all state-changing operations go through
  OfflineQueueDao and SyncWorker, never directly to the API without network checking.
globs: ["android/data/src/main/java/**/repository/**/*.kt", "android/data/src/main/java/**/sync/**/*.kt"]
synthesized: true
private: false
---

# Offline Sync Queue Convention

## The Pattern

All state-changing operations (create, update, delete) MUST go through the offline queue system:

```
User action → Repository → Room (immediate local write)
                         → OfflineQueueDao (queue API call)
                         → SyncWorker (process queue when online)
```

The user sees the change immediately (Room is source of truth). The API sync happens in the background via `SyncWorker`.

## OfflineQueueDao

Queue entries use `OfflineActionType` enum to track what needs syncing:

```kotlin
enum class OfflineActionType {
    SWAP_MEAL,
    UPDATE_RECIPE_RULE,
    UPDATE_NUTRITION_GOAL,
    REGISTER_FCM_TOKEN,
    // Add new action types here
}
```

Each queued action stores: `actionType`, `payload` (JSON), `createdAt`, `retryCount`.

## SyncWorker

`SyncWorker` is a `CoroutineWorker` registered with WorkManager:
- Runs when network is available (`NetworkType.CONNECTED` constraint)
- Processes queue entries by `OfflineActionType`, dispatching to the correct API call
- Retries failed entries with backoff
- Periodic sync runs every 15 minutes when online

## MUST DO

- Queue mutations via `OfflineQueueDao.insert()` with the correct `OfflineActionType`
- Write to Room FIRST, then queue the sync — user sees changes immediately
- Check `NetworkMonitor.isOnline` before attempting direct API calls for read operations
- Add new `OfflineActionType` enum values when adding new syncable mutations
- Handle the new action type in `SyncWorker.doWork()` switch statement

## MUST NOT

- NEVER call the API directly for mutations without going through the offline queue
- NEVER skip the Room write — Room is the source of truth, not the API
- NEVER use `runBlocking` in SyncWorker — it's already a `CoroutineWorker`
- NEVER delete queue entries before confirming API success — retry on failure
