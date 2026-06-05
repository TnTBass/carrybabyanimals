# ModStatusKit Build Metadata Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Embed a Gradle-stamped build number inside the Carry Baby Animals jar and expose it through the embedded ModStatusKit build metadata support without changing the jar filename.

**Architecture:** Update the embedded `dev.jasmine.carrybabyanimals.internal.modstatus` copy to the ModStatusKit 0.1.3 build metadata API. Generate a small `BuildInfo` Java class during Gradle builds, keep `fabric.mod.json` and artifact names on the base mod version, and pass the generated build value to `clientBuild(...)` for status payloads and diagnostics.

**Tech Stack:** Gradle/Fabric Loom, Java 25, JUnit 5, embedded dependency-free ModStatusKit sources.

---

## File Structure

- Create `src/main/java/dev/jasmine/carrybabyanimals/internal/modstatus/ModStatusVersion.java`: normalized base version plus optional build metadata.
- Modify `src/main/java/dev/jasmine/carrybabyanimals/internal/modstatus/ModStatusConfig.java`: store `ModStatusVersion`, expose `clientBuild()` and `clientVersionInfo()`.
- Modify `src/main/java/dev/jasmine/carrybabyanimals/internal/modstatus/ModStatusSnapshot.java`: store build-aware server version metadata.
- Modify `src/main/java/dev/jasmine/carrybabyanimals/internal/modstatus/ModStatusDisplay.java`: expose optional client/server build values while preserving the existing constructor.
- Modify `src/main/java/dev/jasmine/carrybabyanimals/internal/modstatus/ModStatusKit.java`: compare base versions and pass build metadata into display.
- Modify `src/main/java/dev/jasmine/carrybabyanimals/internal/modstatus/ModStatusVersionPayload.java`: add build-aware encode/decode helpers and send `version+build` when present.
- Modify `src/main/java/dev/jasmine/carrybabyanimals/modstatus/CarryBabyAnimalsModStatus.java`: pass generated build metadata into ModStatusKit.
- Modify `build.gradle`: generate `BuildInfo.java` from Gradle/project/CI properties and include generated source in `main`.
- Modify tests under `src/test/java/dev/jasmine/carrybabyanimals`: cover version parsing, base-version matching, payload round trips, generated build stamping, and unchanged jar naming.
- Modify changelogs: public note for visible build details and internal note for embedded ModStatusKit/build stamping.

## Task 1: Add Red Tests

**Files:**
- Modify `src/test/java/dev/jasmine/carrybabyanimals/modstatus/CarryBabyAnimalsModStatusTest.java`
- Modify `src/test/java/dev/jasmine/carrybabyanimals/network/CarryNetworkingTest.java`

- [ ] Add tests proving build metadata is exposed, base-version-only matching stays green, and configured server payloads include `version+build`.
- [ ] Run `.\gradlew.bat test` and confirm the new tests fail because the build metadata API does not exist yet.

## Task 2: Embed ModStatusKit 0.1.3 Build Metadata API

**Files:**
- Create `src/main/java/dev/jasmine/carrybabyanimals/internal/modstatus/ModStatusVersion.java`
- Modify `src/main/java/dev/jasmine/carrybabyanimals/internal/modstatus/ModStatusConfig.java`
- Modify `src/main/java/dev/jasmine/carrybabyanimals/internal/modstatus/ModStatusSnapshot.java`
- Modify `src/main/java/dev/jasmine/carrybabyanimals/internal/modstatus/ModStatusDisplay.java`
- Modify `src/main/java/dev/jasmine/carrybabyanimals/internal/modstatus/ModStatusKit.java`
- Modify `src/main/java/dev/jasmine/carrybabyanimals/internal/modstatus/ModStatusVersionPayload.java`

- [ ] Implement the embedded ModStatusKit 0.1.3 build metadata behavior.
- [ ] Run `.\gradlew.bat test` and confirm the build metadata tests now compile and pass except for Gradle-generated build stamping expectations.

## Task 3: Generate BuildInfo from Gradle

**Files:**
- Modify `build.gradle`
- Modify `src/main/java/dev/jasmine/carrybabyanimals/modstatus/CarryBabyAnimalsModStatus.java`
- Modify tests as needed

- [ ] Add a `generateBuildInfo` task that writes `dev.jasmine.carrybabyanimals.BuildInfo` under `build/generated/sources/buildInfo/java/main`.
- [ ] Resolve build metadata from `-PbuildNumber`, then `GITHUB_RUN_NUMBER`, then `git rev-parse --short HEAD`, then `dev`.
- [ ] Include the generated source set in `main` and make Java compilation depend on generation.
- [ ] Pass `BuildInfo.BUILD_NUMBER` to `ModStatusConfig.builder().clientBuild(...)`.
- [ ] Run `.\gradlew.bat test` and `.\gradlew.bat build -PbuildNumber=12345`.
- [ ] Inspect `build/libs/carrybabyanimals-0.1.3.jar` to confirm `BuildInfo.class` is present while the jar filename remains base-version-only.

## Task 4: Changelogs and Review

**Files:**
- Modify `CHANGELOG.md`
- Modify `INTERNAL_CHANGELOG.md`

- [ ] Add public release wording for ModMenu/status build detail.
- [ ] Add internal changelog wording for embedded ModStatusKit 0.1.3 and Gradle build stamping.
- [ ] Run `.\gradlew.bat checkChangelog`, `git diff --check`, and full verification.
- [ ] Request a Revue implementation review over the explicit touched files.
- [ ] Evaluate Revue findings with `superpowers:receiving-code-review`, fix valid findings, and re-run verification before completion.
