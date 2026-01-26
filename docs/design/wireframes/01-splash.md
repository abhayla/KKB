# Screen 1: Splash Screen

```
┌─────────────────────────────────────┐
│                                     │
│                                     │
│                                     │
│                                     │
│         ┌───────────────┐           │
│         │    🍳         │           │
│         │   RASOIAI     │           │
│         └───────────────┘           │
│                                     │
│      AI Meal Planning for           │
│         Indian Families             │
│                                     │
│                                     │
│                                     │
│          ◐ Loading...               │
│                                     │
│                                     │
│                                     │
│                                     │
└─────────────────────────────────────┘
```

## Design Notes

| Element | Specification |
|---------|---------------|
| Background | Cream `#FDFAF4` |
| Logo | Orange `#FF6838` cooking pot icon |
| Duration | 2-3 seconds |
| Behavior | Check auth state → route to Auth or Home |
| Offline | Show offline banner if no network |
