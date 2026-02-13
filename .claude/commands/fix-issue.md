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

6b. **Fix Loop (via /fix-loop Skill)**

   If any verification tests fail — **regardless of whether the failure is known or pre-existing** — **use the Skill tool** to invoke `/fix-loop` in Full Loop mode. Do NOT read fix-loop.md and follow it inline.

   Invoke: `skill: "fix-loop"` with arguments:
   ```
   failure_output:         {raw test failure output from Step 6}
   failure_context:        {description of the fix and what tests verify}
   files_of_interest:      {files modified in Step 5}
   build_command:          {build command if Android, null if backend-only}
   retest_command:         {same verification command from Step 6}
   retest_timeout:         300
   max_iterations:         6
   max_attempts_per_issue: 3
   prohibited_actions:     ["@Ignore", "weaken assertions"]
   fix_target:             "production"
   log_dir:                ".claude/logs/fix-loop/"
   ```
   Budget rationale: `max_iterations: 6` — smaller budget than /implement since issue fixes are typically more focused.

   The /fix-loop Skill will iterate until all tests pass or budget is exhausted.

   **CRITICAL:** Do NOT proceed to Step 7 until /fix-loop returns **RESOLVED**.

7. **Post-Fix Pipeline (via /post-fix-pipeline Skill)**

   **Use the Skill tool** to invoke `/post-fix-pipeline`. Do NOT commit manually.

   Invoke: `skill: "post-fix-pipeline"` with arguments:
   ```
   fixes_applied:            {list of changes from Steps 5+6b}
   files_changed:            {all modified file paths}
   session_summary:          "Fix #$ARGUMENTS: {brief description}"
   test_suite_commands:      [
     { name: "backend", command: "cd backend && PYTHONPATH=. pytest --tb=short -q", timeout: 300 },
     { name: "android-unit", command: "cd android && ./gradlew test --console=plain", timeout: 600 }
   ]
   test_suite_max_fix_attempts: 2
   docs_instructions:        "Update any related TODO comments to reference the fix."
   commit_format:            "fix({scope}): {summary}\n\nFix #$ARGUMENTS"
   commit_scope:             "{affected-area}"
   push:                     false
   ```

   The /post-fix-pipeline Skill handles: test suite verification gate, documentation updates, and git commit with Co-Authored-By tag.

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

# 3. /post-fix-pipeline Skill handles commit automatically with format:
# "fix(home): implement Add Recipe button
#
# Fix #42
#
# Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```
