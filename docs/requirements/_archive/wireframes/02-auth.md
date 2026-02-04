# Screen 2: Auth Screen

```
┌─────────────────────────────────────┐
│                                     │
│                                     │
│                                     │
│         ┌───────────────┐           │
│         │    🍳         │           │
│         │   RASOIAI     │           │
│         └───────────────┘           │
│                                     │
│            Welcome!                 │
│                                     │
│      AI Meal Planning for           │
│         Indian Families             │
│                                     │
│                                     │
│   ┌─────────────────────────────┐   │
│   │  G   Continue with Google   │   │
│   └─────────────────────────────┘   │
│                                     │
│                                     │
│                                     │
│                                     │
│   By continuing, you agree to our   │
│   Terms of Service • Privacy Policy │
│                                     │
└─────────────────────────────────────┘
```

## Design Notes

| Element | Specification |
|---------|---------------|
| Auth Method | Google OAuth only |
| Backend | Firebase Auth |
| Button Style | Google branded button |
| Flow | Single tap → Google picker → Home/Onboarding |
