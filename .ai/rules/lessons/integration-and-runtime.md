# Lessons: Integration And Runtime

- Trigger: Running project verification commands from PowerShell.
  Rule: Use `mvn` by default in this repo and switch to `mvnw.cmd` only if the wrapper script is actually added.
  Expected validation: Commands and task guidance match the executables that exist in the repository.

- Trigger: The exercise requires the Producer and Balance Tracker to run separately.
  Rule: Preserve separate JVM process boundaries in code structure and run instructions instead of collapsing both roles into one process for convenience.
  Expected validation: Runtime guidance and implementation keep producer and tracker independently runnable.

- Trigger: Changing communication between Producer and Balance Tracker.
  Rule: Treat the integration contract as a compatibility boundary because the exercise expects independent release of both applications.
  Expected validation: Message or API changes are explicit and do not depend on hidden in-process coupling.

- Trigger: Inherited guidance references tooling not present in the repo.
  Rule: Anchor command and runtime instructions to the actual build and execution tooling in this repository before adding project-specific detail.
  Expected validation: AI guidance references Maven and current repo paths rather than unrelated stack conventions.

- Trigger: Shared Spring setup is introduced to avoid duplication across modules.
  Rule: Put only technical cross-cutting configuration in `common-service` and keep runtime-specific bootstrap in the owning module or dedicated `broker` module.
  Expected validation: Shared configuration reduces duplication without hiding producer, tracker, or broker-specific runtime behavior.

- Trigger: An embedded broker is introduced as part of local runtime support.
  Rule: Keep broker bootstrap isolated from producer and tracker business logic, ideally in a dedicated `broker` module when the setup is substantial.
  Expected validation: Producer and tracker depend on broker contracts and connectivity, not on broker bootstrap internals.
