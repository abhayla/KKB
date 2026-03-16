---
description: >
  Enforce the Channel-based one-shot navigation event pattern across all ViewModels.
  Prevents duplicate navigation and ensures events are consumed exactly once.
globs: ["android/app/src/main/java/**/presentation/**/*ViewModel.kt"]
synthesized: true
private: false
---

# ViewModel Navigation Events via Channel

## Pattern

All ViewModels that trigger navigation MUST use `Channel<Event>` exposed as `Flow` via `receiveAsFlow()`. This ensures navigation events are consumed exactly once, even across configuration changes.

## Required Structure

```kotlin
// 1. Define a sealed class for navigation events
sealed class XxxNavigationEvent {
    data object NavigateToHome : XxxNavigationEvent()
    data class NavigateToDetail(val id: String) : XxxNavigationEvent()
}

// 2. In the ViewModel: Channel + Flow
private val _navigationEvent = Channel<XxxNavigationEvent>(Channel.BUFFERED)
val navigationEvent: Flow<XxxNavigationEvent> = _navigationEvent.receiveAsFlow()

// 3. Emit events via the channel
viewModelScope.launch {
    _navigationEvent.send(XxxNavigationEvent.NavigateToHome)
}
```

## MUST DO

- Use `Channel.BUFFERED` capacity — not `Channel.UNLIMITED` (prevents unbounded memory growth) and not `Channel.RENDEZVOUS` (can drop events if collector is slow)
- Collect in `LaunchedEffect` in the Composable:
  ```kotlin
  LaunchedEffect(Unit) {
      viewModel.navigationEvent.collect { event ->
          when (event) {
              is XxxNavigationEvent.NavigateToHome -> navController.navigate(Screen.Home.route)
          }
      }
  }
  ```

## MUST NOT

- NEVER use `SharedFlow` for one-shot navigation events — `SharedFlow` replays to new collectors, causing duplicate navigation on config change
- NEVER use `LiveData<Event<T>>` wrapper pattern — it's a workaround for LiveData's replay behavior that Kotlin Channels solve natively
- NEVER emit navigation events from `init {}` without checking current state first — the collector may not be ready yet
