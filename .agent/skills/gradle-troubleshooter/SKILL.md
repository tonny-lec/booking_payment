# SKILL: gradle-troubleshooter

**Purpose**
- Diagnose and resolve common Gradle build failures documented in the repo.

**Trigger**
- Gradle build or test fails with dependency or configuration errors.

**Inputs**
- Exact error message
- Module name and task
- Gradle dependency versions

**Outputs**
- Concrete remediation steps tied to the failure cause

**Procedure**
1. Identify the failure category from the error output:
   - Missing OpenTelemetry Instrumentation BOM
   - Spring Boot version incompatibility
   - DataSource auto-configuration failure
   - Gradle binary store/cache errors
   - BOM import not resolving versions
2. Apply the corresponding fix:
   - Separate OpenTelemetry core vs instrumentation versions.
   - Align OpenTelemetry Instrumentation with Spring Boot version.
   - Use Testcontainers for DataSource in tests, or temporarily exclude auto-config.
   - Clear Gradle cache or run without daemon/build cache when binary store errors occur.
   - Use `platform()` API or apply dependency-management plugin correctly.
3. Re-run the failing task with `--stacktrace` and confirm resolution.

**Do Not**
- Change versions without confirming the compatible ranges.

**References**
- `agents/knowledge/gradle-build-issues.md`
