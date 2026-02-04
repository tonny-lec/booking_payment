---
name: gradle-troubleshooter
description: Diagnose and resolve common Gradle build failures documented in the repo. Use when Gradle build or test tasks fail.
---

# Gradle Troubleshooter

## Scope
- Resolve common Gradle dependency and configuration failures.

## Inputs
- Provide the exact error message.
- Provide module name and task.
- Provide relevant dependency versions.

## Outputs
- Produce concrete remediation steps tied to the failure cause.

## Procedure
1. Classify the failure category from the error output.
2. Apply the matching fix from the knowledge base.
3. Re-run the failing task with `--stacktrace` and confirm resolution.

## Evaluation
- Define must-pass checks for build success.
- Capture error logs and fix steps as artifacts.
- Score unresolved errors as failures.

## Do Not
- Change versions without confirming compatibility.

## References
- `agents/knowledge/gradle-build-issues.md`
