# Lessons: Architecture And Data Flow

- Trigger: Multiple call sites derive the same value.
  Rule: Centralize the derivation in one shared helper or service.
  Expected validation: A single derivation source is used across relevant call sites.

- Trigger: Refactoring shared helpers risks breakage.
  Rule: Preserve backward compatibility where needed, or update all call sites atomically.
  Expected validation: No stale imports or references remain and behavior stays consistent.

- Trigger: Related transfer or domain shapes are locally redeclared.
  Rule: Promote the shared shape into one authoritative definition instead of duplicating it.
  Expected validation: Duplicate shape declarations are removed and all consumers use the same definition.

- Trigger: Business rules drift into transport or UI-facing layers.
  Rule: Keep core business logic in dedicated domain or service code and keep adapters thin.
  Expected validation: Core logic remains testable without transport-specific setup.

- Trigger: A class is being moved into `common` or `common-service` to reduce duplication.
  Rule: Put it in `common` only if it is a shared contract; put it in `common-service` only if it is shared technical support. Keep business behavior inside the owning application module.
  Expected validation: Shared modules contain contracts or technical support only, and producer/tracker business logic stays local.

- Trigger: Formal layering is introduced inside `producer` or `tracker`.
  Rule: Keep `domain` and `application` independent from transport, broker bootstrap, and REST controller concerns; place those details under `infrastructure` or `api`.
  Expected validation: Domain/application code remains testable without framework-heavy setup and adapters stay at the boundary.

- Trigger: Adding or reshaping Java runtime/bootstrap classes, configuration helpers, or non-obvious value objects.
  Rule: Add concise Javadocs to classes and public or package-level methods when their role, lifecycle, or configuration contract is not immediately obvious from the signature alone.
  Expected validation: Java source explains the purpose of bootstrap/configuration code without requiring readers to infer behavior from implementation details.

- Trigger: The user asks for Java documentation on an existing utility they already own.
  Rule: Limit the edit to Javadocs or other requested documentation unless the user explicitly asks for behavioral changes too.
  Expected validation: The utility's implementation stays intact and only the requested documentation changes are introduced.

- Trigger: A user asks to document or amend a file after prior edits or rejected changes.
  Rule: Re-read the current on-disk file immediately before patching and base the edit on that exact content.
  Expected validation: The patch matches the live file structure and does not describe or modify stale content.
