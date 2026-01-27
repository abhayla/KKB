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
│   └── wireframes/
│
├── research/              # Reference research and analysis
│   └── Ollie App Research.md
│
├── testing/               # Testing documentation and artifacts
│   ├── E2E-Testing-Prompt.md
│   └── screenshots/       # Test screenshots (gitignored)
│
├── claude-docs/           # Claude-generated documents
│   ├── Android-Best-Practices-Audit-Guide.md
│   ├── RasoiAI-Codebase-Audit-Report.md
│   └── Feature Comparison - Ollie vs RasoiAI.md
│
└── CONTINUE_PROMPT.md     # Session continuation prompt
```

## Folder Descriptions

| Folder | Purpose |
|--------|---------|
| `requirements/` | Product Requirements Document (PRD), user stories, feature specs |
| `design/` | Technical Design Document (TDD), architecture decisions, wireframes |
| `research/` | Market research, competitor analysis, reference materials |
| `testing/` | E2E testing guides, test plans, and screenshots (artifacts gitignored) |
| `claude-docs/` | Claude-generated documents: audit reports, comparisons, guides |

## Document Output Rules

When Claude generates documents:

| Document Type | Save Location |
|---------------|---------------|
| Generated docs, reports | `docs/claude-docs/` |
| Test screenshots/artifacts | `docs/testing/screenshots/` (gitignored) |
| Audit reports | `docs/claude-docs/` |

## Key Documents

| Document | Description |
|----------|-------------|
| [RasoiAI Requirements](requirements/RasoiAI%20Requirements.md) | Full PRD with features, user stories, and acceptance criteria |
| [RasoiAI Technical Design](design/RasoiAI%20Technical%20Design.md) | Architecture, database schema, API contracts, screen flows |
| [Android Architecture Decisions](design/Android%20Architecture%20Decisions.md) | Key architecture decisions with rationale |
| [E2E Testing Guide](testing/E2E-Testing-Prompt.md) | Espresso-based E2E testing guide |
| [Audit Guide](claude-docs/Android-Best-Practices-Audit-Guide.md) | Android best practices audit checklist |
| [Audit Report](claude-docs/RasoiAI-Codebase-Audit-Report.md) | Codebase audit results |

## Testing

- **Framework**: Espresso for UI/E2E tests
- **Guide**: See [E2E-Testing-Prompt.md](testing/E2E-Testing-Prompt.md)
- **Screenshots**: Saved to `testing/screenshots/` (gitignored, not committed)
