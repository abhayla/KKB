# Fix GitHub Issue

Analyze and implement a fix for GitHub issue: $ARGUMENTS

## Steps

1. **Fetch Issue Details**
   ```bash
   gh issue view $ARGUMENTS
   ```

2. **Understand the Problem**
   - Read the issue description and acceptance criteria
   - Note the code location if provided
   - Identify the area (Android/Backend)

3. **Explore the Codebase**
   - Search for relevant files using the code location
   - Read existing implementations and patterns
   - Check for related TODOs or comments

4. **Plan the Implementation**
   - Determine which files need changes
   - Consider existing patterns from CLAUDE.md
   - Check for reusable components

5. **Implement the Fix**
   - Make minimal, focused changes
   - Follow existing code patterns
   - Add appropriate error handling

6. **Verify the Fix**
   - For Android changes: `cd android && ./gradlew :app:testDebugUnitTest`
   - For Backend changes: `cd backend && PYTHONPATH=. pytest`
   - Run relevant UI tests if applicable

7. **Create a Commit**
   - Stage only the relevant files
   - Commit with message: `Fix #$ARGUMENTS: <brief summary>`

## Guidelines

- Follow existing patterns documented in CLAUDE.md
- Use the project's architecture (Hilt, StateFlow, Room offline-first)
- Do NOT push unless explicitly asked
- If the issue is unclear, ask for clarification before implementing
- Reference the issue number in the commit message to auto-link
- Update any related TODO comments to reference the fix

## Example Workflow

```bash
# 1. Get issue details
gh issue view 42

# 2. After implementing the fix, run tests
cd android && ./gradlew :app:testDebugUnitTest

# 3. Commit the changes
git add <files>
git commit -m "Fix #42: Implement Add Recipe button on Home screen

- Added showAddRecipeSheet state to HomeViewModel
- Wired up onAddClick handlers in MealSlotCard
- AddRecipeSheet now displays for selected meal type

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```
