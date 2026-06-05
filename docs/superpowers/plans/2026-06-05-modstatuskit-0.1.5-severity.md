# ModStatusKit 0.1.5 Severity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Embed ModStatusKit 0.1.5's server-declared version mismatch severity support while keeping Carry Baby Animals on `VersionMismatchSeverity.WARN`.

**Architecture:** Preserve the relocated dependency-free ModStatusKit package under `dev.jasmine.carrybabyanimals.internal.modstatus`. Add structured server status payload helpers and severity state, then have Carry Baby Animals send structured `WARN` status payloads from the server while the client decodes both structured and legacy payloads.

**Tech Stack:** Java 25, Fabric networking payloads, JUnit 5, embedded dependency-free ModStatusKit sources.

---

### Task 1: Add Failing Severity Tests

**Files:**
- Modify: `src/test/java/dev/jasmine/carrybabyanimals/modstatus/CarryBabyAnimalsModStatusTest.java`
- Modify: `src/test/java/dev/jasmine/carrybabyanimals/network/CarryNetworkingTest.java`

- [ ] **Step 1: Write failing tests for 0.1.5 behavior**

Add tests that prove:
- public version mismatch with `WARN` displays orange, not red
- public version mismatch with `BREAKING` can display red
- same public version with different builds remains compatible
- Carry Baby Animals sends structured server status payloads with `VersionMismatchSeverity.WARN`
- legacy version payload decoding still works

- [ ] **Step 2: Run focused tests and confirm RED**

Run:

```powershell
.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.modstatus.CarryBabyAnimalsModStatusTest --tests dev.jasmine.carrybabyanimals.network.CarryNetworkingTest
```

Expected: compilation or assertion failures because `VersionMismatchSeverity`, `ModStatusServerStatus`, structured payload helpers, and red tone handling are not implemented locally yet.

### Task 2: Embed ModStatusKit 0.1.5 Core Additions

**Files:**
- Create: `src/main/java/dev/jasmine/carrybabyanimals/internal/modstatus/VersionMismatchSeverity.java`
- Create: `src/main/java/dev/jasmine/carrybabyanimals/internal/modstatus/ModStatusServerStatus.java`
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/internal/modstatus/StatusTone.java`
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/internal/modstatus/ModStatusSnapshot.java`
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/internal/modstatus/ModStatusKit.java`
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/internal/modstatus/ModStatusVersionPayload.java`
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/internal/modstatus/ModStatusClientState.java`

- [ ] **Step 1: Implement the smallest source changes that satisfy the tests**

Add severity defaults to `WARN`, structured `MSK2` payload encode/decode, `sendServerStatusIfSupported(...)`, `connected(config, ModStatusServerStatus)`, and `StatusTone.RED`. Keep existing build-mismatch status text and behavior in this consuming mod.

- [ ] **Step 2: Run focused tests and confirm GREEN**

Run the focused Gradle test command from Task 1 again.

Expected: tests pass.

### Task 3: Wire Carry Baby Animals To WARN

**Files:**
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/network/CarryNetworking.java`
- Modify: `src/client/java/dev/jasmine/carrybabyanimals/client/config/CarryBabyAnimalsConfigScreen.java`
- Modify: `CHANGELOG.md`
- Modify: `INTERNAL_CHANGELOG.md`

- [ ] **Step 1: Send structured status payloads**

Change `sendConfiguredServerVersionIfSupported(...)` to call `ModStatusVersionPayload.sendServerStatusIfSupported(CarryBabyAnimalsModStatus.CONFIG, VersionMismatchSeverity.WARN, support, sender)`.

- [ ] **Step 2: Map red tone in the ModMenu indicator**

Add `StatusTone.RED` to the UI color switch, while Carry Baby Animals' normal `WARN` mismatch stays orange.

- [ ] **Step 3: Update changelogs**

Update public notes only if the visible player/admin behavior changed, and update internal notes for the embedded ModStatusKit 0.1.5 upgrade.

- [ ] **Step 4: Run full verification**

Run:

```powershell
.\gradlew.bat build
git diff --check
```

Expected: both pass.

### Task 4: Revue Review Gate

**Files:**
- Review the explicit implementation files changed by Tasks 1-3.

- [ ] **Step 1: Request implementation review through Revue**

Use `superpowers-review-gates` with `implementation-review`, explicit changed files, and requirements:
- upstream ModStatusKit 0.1.5 additions are embedded under the relocated package
- Carry Baby Animals sends `VersionMismatchSeverity.WARN`
- `WARN` mismatches render orange, `BREAKING` support renders red
- build metadata remains diagnostic
- legacy payloads still decode

- [ ] **Step 2: Evaluate findings with receiving-code-review**

Action valid findings, push back on invalid findings with code/test evidence, and rerun focused or full verification after fixes.
