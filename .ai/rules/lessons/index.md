# Lessons Index

## Session Load Policy
- Always read this file at session start.
- Read only the category files needed for the next action or actions.
- After user correction, add or update a lesson in the matching category file in the format: `Trigger -> Rule -> Expected validation`.
- If a new lesson does not fit an existing category, create the new category file and add it to this index.

## Category Map
- `collaboration-and-scope.md`
  - Task tracking behavior, naming corrections, scope changes, commit conventions, and file-edit scope rules.
- `architecture-and-data-flow.md`
  - Data-flow correctness, shared derivation, contract ownership, and compatibility-safe refactors.
- `integration-and-runtime.md`
  - Maven command defaults, process boundaries, runtime startup assumptions, and cross-process compatibility rules.
- `testing-and-verification.md`
  - Automated test scope, boundary coverage, regression protection, and verification defaults.

## Action-To-Category Lookup
- Starting a new task or changing requested scope:
  - Read `collaboration-and-scope.md`.
- Refactoring or changing data contracts or data flow:
  - Read `architecture-and-data-flow.md`.
- Changing build, startup, messaging, REST exposure, or runtime wiring:
  - Read `integration-and-runtime.md`.
- Adding or updating automated tests, or verifying bug fixes:
  - Read `testing-and-verification.md`.
