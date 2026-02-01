# RasoiAI Documentation

This folder contains all project documentation for RasoiAI.

## Folder Structure

```
docs/
├── requirements/          # Product requirements and PRD
│   └── RasoiAI Requirements.md
│
├── design/                # Technical design documents
│   ├── RasoiAI Technical Design.md
│   ├── Android Architecture Decisions.md
│   ├── Meal-Generation-Algorithm.md
│   ├── Meal-Generation-Config-Architecture.md
│   └── wireframes/
│
├── research/              # Reference research and analysis
│   └── Ollie App Research.md
│
├── testing/               # Testing documentation and artifacts
│   ├── E2E-Testing-Prompt.md
│   ├── E2E-Test-Status.md
│   └── screenshots/       # Test screenshots (gitignored)
│
├── claude-docs/           # Claude-generated documents
│   ├── Android-Best-Practices-Audit-Guide.md
│   └── RasoiAI-Codebase-Audit-Report.md
│
└── CONTINUE_PROMPT.md     # Session continuation prompt
```

## Key Documents

| Document | Description |
|----------|-------------|
| [RasoiAI Requirements](requirements/RasoiAI%20Requirements.md) | Full PRD with features, user stories, and acceptance criteria |
| [RasoiAI Technical Design](design/RasoiAI%20Technical%20Design.md) | Architecture, database schema, API contracts |
| [Android Architecture Decisions](design/Android%20Architecture%20Decisions.md) | Key architecture decisions with rationale |
| [Meal Generation Algorithm](design/Meal-Generation-Algorithm.md) | 2-item pairing logic and rule enforcement |
| [E2E Testing Guide](testing/E2E-Testing-Prompt.md) | Compose UI Testing guide |
| [Session Context](CONTINUE_PROMPT.md) | For continuing work between sessions |

## Document Output Rules

When Claude generates documents:

| Document Type | Save Location |
|---------------|---------------|
| Generated docs, reports | `docs/claude-docs/` |
| Test screenshots/artifacts | `docs/testing/screenshots/` (gitignored) |

## Testing

- **Framework**: Compose UI Testing for Android UI/E2E tests
- **Guide**: See [E2E-Testing-Prompt.md](testing/E2E-Testing-Prompt.md)
- **Screenshots**: Saved to `testing/screenshots/` (gitignored)
