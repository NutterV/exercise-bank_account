# Lessons: Collaboration And Scope

- Trigger: User says a task is done or asks to move to the next task for work tracked in `.ai/tasks/current.md`, `.ai/tasks/subTask.md`, or `.ai/tasks/epic.md`.
  Rule: Immediately mark the corresponding task-file items complete, then continue.
  Expected validation: Task files reflect user-confirmed completion state in the same turn.

- Trigger: User corrects naming after implementation starts.
  Rule: Adopt the requested naming exactly across files, exports, and call sites.
  Expected validation: Filenames, symbols, and references match the requested naming.

- Trigger: Creating commits in a repo that uses conventional commits.
  Rule: Keep commit messages in conventional commit format.
  Expected validation: Commit subjects follow the conventional commit pattern.

- Trigger: A change set starts to mix unrelated concerns or partial work that is hard to validate.
  Rule: Split the work into smaller testable commits and move the off-track portion into a separate tracked task instead of forcing it into the current commit.
  Expected validation: Each commit represents a coherent, testable slice and can be reverted without collateral damage where practical.

- Trigger: A commit includes both structural refactoring and runtime behavior changes.
  Rule: Separate refactors from behavior changes into distinct commits whenever they can be validated independently.
  Expected validation: Refactor-only commits preserve behavior, and behavior-change commits remain easy to review and revert.

- Trigger: User redirects the approach mid-stream.
  Rule: Pause, re-scope the plan, complete only the requested approach, and clean up any abandoned path.
  Expected validation: The final diff reflects the requested path only.

- Trigger: User explicitly de-scopes remaining work.
  Rule: Stop the de-scoped work immediately and mark it deferred in the plan.
  Expected validation: No code changes land for deferred scope and the plan reflects the deferral.

- Trigger: User has open editor tabs and asks for amendments.
  Rule: Never delete files to rewrite them; update them in place.
  Expected validation: File paths stay stable throughout the edit.

- Trigger: User asks for file edits in a repo with ignored paths.
  Rule: Ignore directories and files matched by `.gitignore` during file-edit discovery, except `.ai/tasks/epic.md` and files explicitly provided as context in the current request.
  Expected validation: Search and edit scope excludes ignored paths unless explicitly allowed.
