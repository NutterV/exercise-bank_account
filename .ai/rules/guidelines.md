## Workflow Orchestration

### 0. Mechanics
- If the chat history is getting too long for a single session, create `.ai/rollover.md` with the minimum required handover for a new session.
- Read `.ai/rules/lessons/index.md` every session.
- Load only lesson category files relevant to the actions you are about to perform.

### 0.1 Quick-Load Checklist (Run Every Session)
- Re-read `.ai/rules/guidelines.md` whenever it changes before continuing work.
- Run a quick lessons hygiene pass when reading guidelines: remove duplicates, merge overlapping rules, and prune stale historical one-offs in `.ai/rules/lessons/*`.
- If any tool or test run is interrupted or fails unexpectedly, re-read affected files before continuing edits.
- Never delete and recreate files for rewrites when an in-place update will do.
- After non-trivial changes, run a build or targeted verification command.
- After bug fixes, add or update a focused test when practical.

### 0.2 Project Anchors
#### Repo Map
- Build file: `pom.xml`
- Main Java source: `src/main/java`
- Main resources: `src/main/resources`
- Test source: `src/test/java`
- Maven support files: `.mvn/`
- AI guidance: `.ai/`

#### Intended Module Shape
- Multi-module Maven structure is preferred for this exercise.
- Core modules:
  - `common`
  - `common-service`
  - `producer`
  - `tracker`
- Optional module:
  - `broker`

#### Command Defaults
- Use `mvn` by default in this repo because `mvnw.cmd` is not currently present.
- If a Maven wrapper script is added later, prefer `mvnw.cmd` in PowerShell.
- Common verification commands:
  - `mvn test`
  - `mvn package`
  - `mvn -Dtest=<ClassName> test`

#### Runtime Boundaries
- Treat the Producer and Balance Tracker as separate runnable JVM applications.
- Keep any integration choices aligned with the exercise requirement that the applications can be released independently while remaining compatible.
- When adding instructions or automation, avoid assuming a frontend, npm workspace, or browser-based runtime unless the repo later adds one explicitly.

#### Module Responsibilities
- `common`
  - Shared contracts only.
  - Allowed: DTOs, message payload models, enums, value objects, and true cross-module interfaces.
  - Not allowed: Spring configuration, broker-specific code, business-service implementations, or application wiring.
- `common-service`
  - Shared technical and framework support only.
  - Allowed: reusable Spring configuration, shared serialization setup, shared configuration properties, logging helpers, and generic infrastructure support that does not encode app-specific business behavior.
  - Not allowed: producer-specific business rules, tracker-specific business rules, batching logic, or convenience abstractions that create semantic coupling.
- `producer`
  - Producer-specific generation, publishing orchestration, and producer-side infrastructure.
- `tracker`
  - Tracker-specific consumption, balance tracking, audit batching, and REST exposure.
- `broker` when present
  - Broker bootstrap and runtime support only.
  - No domain logic.

#### Layering Defaults
- In `producer` and `tracker`, prefer a formal package structure with clear boundaries:
  - `domain`
  - `application`
  - `infrastructure`
- In `tracker`, `api` is also appropriate for REST-facing code.
- Keep domain and application logic independent from broker and REST framework details wherever practical.

#### Testing Defaults
- Prefer focused automated tests around domain and service behavior before relying on broad end-to-end style verification.
- Add or update tests for:
  - balance tracking correctness
  - transaction processing under concurrent or asynchronous flow
  - audit submission rules, especially exact transaction counts and batch-value thresholds
- Use targeted test execution while iterating, then run a broader Maven verification command before calling work complete.

### 1. Plan Mode Default
- Enter plan mode for any non-trivial task.
- If something goes sideways, stop and re-plan instead of continuing blindly.
- Include verification steps in the plan, not just implementation steps.

### 2. Self-Improvement Loop
- After any correction from the user, update the matching lesson category file under `.ai/rules/lessons/`.
- Write rules that prevent the same mistake.
- Keep lessons lean: if an existing lesson is duplicated or obsolete, update or delete it in the same turn instead of only appending.

### 3. Verification
- Never mark a task complete without proving it works.
- Diff behavior between the old and new implementation when relevant.
- Run tests, check logs, and demonstrate correctness.
- For changes involving batching, thresholds, or concurrency, verify boundary cases explicitly rather than only nominal examples.

### 4. Capture Lessons Learned
- Capture relevant lessons in the matching category file under `.ai/rules/lessons/`.
- Record concrete patterns and prevention rules from the current task.
- Keep entries specific, actionable, and reusable.
- Use the format: `Trigger -> Rule -> Expected validation`.
- If a lesson would not help make a future request faster, safer, or more accurate, do not store it.
- Keep lessons indexed: when adding a new durable lesson type, update `.ai/rules/lessons/index.md` with its category mapping.

### 5. Demand Elegance
- For non-trivial changes, ask whether there is a simpler or more coherent approach.
- Skip this for obvious fixes; do not over-engineer.

### 6. Autonomous Bug Fixing
- When given a bug report, investigate the root cause and fix it directly.
- Use logs, errors, and failing tests to guide the work.

### 7. Commit Message Policy
- All commits must use conventional commit format.
- Prefer small commits that group one testable change at a time.
- If work starts to branch into a different concern or can no longer be kept cleanly revertible, stop and track the extra work as a separate task to resume later.
- Aim to keep each commit safely revertible without negative impact on unrelated behavior wherever practical.
- Avoid mixing refactors and behavior changes in the same commit when they can be separated cleanly.

## Task Management

1. **Session Handover Check**
   - If `.ai/rollover.md` exists, consume it into the current session and then delete it.

2. **Side-Task First Check**
   - Before starting any new work, check `.ai/tasks/subTask.md`.
   - If `.ai/tasks/subTask.md` exists and is incomplete, ask the user what to do with it before proceeding.
   - If the user requests a side task, create or use `.ai/tasks/subTask.md` and apply the same workflow rules below to it.

3. **Current Task Gate**
   - Before starting a new task, check `.ai/tasks/current.md`.
   - If `.ai/tasks/current.md` is incomplete, ask the user what to do with it before proceeding.

4. **Task Placement Rule**
   - New default task scope goes in `.ai/tasks/current.md`.
   - If the task is large, store it in `.ai/tasks/epic.md`.
   - If `.ai/tasks/epic.md` has an incomplete task and a new large task is requested, ask the user what to do before proceeding.

5. **Task File Structure Rule**
   - For active execution files (`.ai/tasks/current.md`, `.ai/tasks/subTask.md`), use:
     - Task Name (h1)
     - Plan (h2)
       - Remaining (sorted by priority) (h3)
       - Completed (h3)
     - Review (h2)
   - For larger planning context in `.ai/tasks/epic.md`, keep:
     - Task Name (h1)
     - Plan (h2)
       - Remaining (sorted by priority) (h3)
       - Completed (h3)
     - Review (h2)

6. **Execution Hygiene**
   - Keep tasks checkable, prioritized, and broken into small completable chunks.
   - Verify plan with the user before significant implementation.
   - Mark items complete as you go.
   - If the user confirms work is done or asks to proceed to the next task for work tracked in a task file, treat that as completion confirmation and immediately tick off the relevant task-file items.
   - Use strikethrough when marking completed items.
   - If all child tasks are complete, mark the parent complete too.
   - Move fully completed items into the `Plan > Completed` section.

7. **Completion And Cleanup**
   - On completion, add or update the Review section in the active task file.
   - Run appropriate validation before calling work done.
   - Capture reusable lessons in the matching category file under `.ai/rules/lessons/` after corrections.

## Core Principles

- **Simplicity First**: Make every change as simple as possible.
- **No Laziness**: Find root causes. Avoid temporary fixes.
- **Minimal Impact**: Change only what is necessary.
- **Shared-Module Discipline**: Keep `common` strict and keep `common-service` technical; do not use shared modules as dumping grounds for app-specific behavior.
- **Search/Edit Scope Discipline**: Ignore directories and files matched by `.gitignore` during file-edit discovery, except `.ai/tasks/*.md` and files explicitly provided as context in the current request.
