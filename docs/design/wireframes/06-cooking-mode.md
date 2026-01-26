# Screen 6: Cooking Mode

```
┌─────────────────────────────────────┐
│  ✕  Dal Tadka           Step 1 / 6  │
│  ━━━○○○○○○                          │
│─────────────────────────────────────│
│                                     │
│                                     │
│            STEP 1                   │
│                                     │
│                                     │
│   Wash and soak toor dal for        │
│   30 minutes.                       │
│                                     │
│   Pressure cook with turmeric       │
│   and salt for 3 whistles.          │
│                                     │
│                                     │
│        ┌───────────────┐            │
│        │  [Image/GIF]  │            │
│        │  Soaking dal  │            │
│        └───────────────┘            │
│                                     │
│                                     │
│   ┌─────────────────────────────┐   │
│   │      ⏱️  SET TIMER          │   │
│   │         30:00               │   │
│   └─────────────────────────────┘   │
│                                     │
│                                     │
│  ┌──────────────┐ ┌──────────────┐  │
│  │              │ │              │  │
│  │   ← PREV     │ │   NEXT →     │  │
│  │              │ │              │  │
│  └──────────────┘ └──────────────┘  │
│                                     │
│─────────────────────────────────────│
│        🔒 Screen stays ON           │
└─────────────────────────────────────┘
```

---

## Timer Running

```
┌─────────────────────────────────────┐
│   ┌─────────────────────────────┐   │
│   │                             │   │
│   │         ⏱️ 24:35            │   │
│   │                             │   │
│   │    [PAUSE]     [STOP]       │   │
│   │                             │   │
│   └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

---

## Timer Complete

```
┌─────────────────────────────────────┐
│   ┌─────────────────────────────┐   │
│   │       ⏱️ TIME'S UP!         │   │
│   │     Dal soaking complete    │   │
│   │   🔔 Vibrating + Sound      │   │
│   │      [DISMISS]              │   │
│   └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

---

## Cooking Complete (After Final Step)

```
┌─────────────────────────────────────┐
│                                     │
│            🎉                       │
│      Cooking Complete!              │
│      Dal Tadka is ready             │
│                                     │
│   ┌─────────────────────────────┐   │
│   │      Rate this dish         │   │
│   │   ☆    ☆    ☆    ☆    ☆    │   │
│   └─────────────────────────────┘   │
│                                     │
│   How did it turn out?              │
│   ┌─────────────────────────────┐   │
│   │ Add feedback (optional)...  │   │
│   └─────────────────────────────┘   │
│                                     │
│   ┌─────────────────────────────┐   │
│   │         DONE                │   │
│   └─────────────────────────────┘   │
│                                     │
│   [SKIP RATING]                     │
│                                     │
└─────────────────────────────────────┘
```

---

## Design Notes

| Feature | Description |
|---------|-------------|
| Screen Lock | FLAG_KEEP_SCREEN_ON enabled |
| Timer | Per-step timer with sound/vibration |
| Navigation | Swipe or tap buttons |
| Rating | Shown after completion, affects AI learning |
