# RasoiAI Documentation

This folder contains all project documentation for RasoiAI.

## Folder Structure

```
docs/
├── requirements/          # Product requirements and PRD
│   └── RasoiAI Requirements.md
│
├── design/                # Technical design documents
│   └── RasoiAI Technical Design.md
│
├── research/              # Reference research and analysis
│   ├── Ollie App Research.md
│   └── CONTINUE_PROMPT.md
│
└── claude-docs/           # Claude-generated documents (staging area)
    └── Feature Comparison - Ollie vs RasoiAI.md
```

## Folder Descriptions

| Folder | Purpose |
|--------|---------|
| `requirements/` | Product Requirements Document (PRD), user stories, feature specs |
| `design/` | Technical Design Document (TDD), architecture, API contracts |
| `research/` | Market research, competitor analysis, reference materials |
| `claude-docs/` | **Staging area** for Claude-generated documents. Files here are auto-generated and should be reviewed before moving to appropriate folders. |

## Document Workflow

1. **Claude-generated docs** are saved to `claude-docs/` by default
2. After review, manually move approved docs to the appropriate folder
3. Update references in `CLAUDE.md` if document paths change

## Key Documents

| Document | Description |
|----------|-------------|
| [RasoiAI Requirements](requirements/RasoiAI%20Requirements.md) | Full PRD with features, user stories, and acceptance criteria |
| [RasoiAI Technical Design](design/RasoiAI%20Technical%20Design.md) | Architecture, database schema, API contracts, screen flows |
| [Ollie App Research](research/Ollie%20App%20Research.md) | Reference research on Ollie.ai (US meal planning app) |
| [Feature Comparison](claude-docs/Feature%20Comparison%20-%20Ollie%20vs%20RasoiAI.md) | Side-by-side feature comparison between Ollie and RasoiAI |
