# Phase 5 Client Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Improve optional modded-client carried baby presentation while preserving the vanilla passenger fallback and server-owned gameplay.

**Architecture:** Keep Phase 5 entirely in the existing client render/state path. Tune held placement with a small client-only bob that reuses vanilla entity rendering, and clear stale render state when the client world identity changes or either carried entity is unavailable. No new custom payloads, permissions, server gameplay, inventory storage, dependencies, GeckoLib, deployment, release, tags, or publishing.

**Tech Stack:** Java 25, Fabric 26.1.2, JUnit 5, existing Fabric client render hooks.

---

## File Structure

- Modify: `src/test/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyPlacementTest.java`
  - Adds failing tests for size-aware placement and cosmetic bob bounds.
- Modify: `src/test/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyRenderStateTest.java`
  - Adds failing tests for client-level identity cleanup and unchanged-level preservation.
- Modify: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyPlacement.java`
  - Adds the tuned overload used by render code while keeping the existing overload for current callers.
- Modify: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyRenderState.java`
  - Tracks the last client level identity and clears stale carried-render maps when it changes.
- Modify: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyRenderer.java`
  - Calls the level identity cleanup and passes animation time into placement.
- Modify: `CHANGELOG.md`
  - Adds public Unreleased note for modded-client carried render polish and stale render cleanup.

## Conservative Design Choice

Use the existing render-state payloads and vanilla entity renderer. Do not add any custom payload or server hint because the first pass can satisfy the roadmap with local client state and the current supported-client capability pattern remains untouched.

## Task 1: Placement Tuning Tests

**Files:**
- Modify: `src/test/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyPlacementTest.java`
- Modify: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyPlacement.java`

- [ ] **Step 1: Write failing tests**

Add tests that call a new overload:

```java
Vec3 held = CarriedBabyPlacement.heldPosition(
        Vec3.ZERO,
        new Vec3(0.0D, 0.0D, 1.0D),
        1.8D,
        0.3D,
        false,
        5.0D
);
```

Expected: this does not compile until the overload exists. Tests should assert that small babies sit slightly higher/closer to the arms, tall babies sit lower/closer than tiny babies, and the cosmetic bob stays within a small vertical range.

- [ ] **Step 2: Run focused red test**

Run: `.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.render.CarriedBabyPlacementTest`

Expected: FAIL because the new `heldPosition(..., double animationTicks)` overload is missing.

- [ ] **Step 3: Implement minimal placement code**

Add an overload to `CarriedBabyPlacement` that:

```java
double sizeFactor = Math.clamp(babyHeight / Math.max(0.1D, carrierHeight), 0.15D, 0.65D);
double forwardDistance = 0.52D - (sizeFactor * 0.18D);
double carriedHeight = Math.min(1.16D, carrierHeight * (0.62D - sizeFactor * 0.10D));
double babyLowering = Math.min(0.18D, babyHeight * 0.16D);
double gentleBob = Math.sin(animationTicks * 0.25D) * 0.018D;
```

Then return `carrierPosition + forward * forwardDistance + right * handSide + y(carriedHeight - babyLowering + gentleBob)`. Keep the existing overload delegating to animation tick `0.0D`.

- [ ] **Step 4: Run focused green test**

Run: `.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.render.CarriedBabyPlacementTest`

Expected: PASS.

## Task 2: Render-State Cleanup Tests

**Files:**
- Modify: `src/test/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyRenderStateTest.java`
- Modify: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyRenderState.java`
- Modify: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyRenderer.java`

- [ ] **Step 1: Write failing tests**

Add tests for:

```java
Object firstLevel = new Object();
Object secondLevel = new Object();
CarriedBabyRenderState.rememberLevel(firstLevel);
CarriedBabyRenderState.set(10, 20);
CarriedBabyRenderState.rememberLevel(secondLevel);
assertFalse(CarriedBabyRenderState.isCarriedBaby(10));
```

and:

```java
Object level = new Object();
CarriedBabyRenderState.rememberLevel(level);
CarriedBabyRenderState.set(10, 20);
CarriedBabyRenderState.rememberLevel(level);
assertTrue(CarriedBabyRenderState.isCarriedBaby(10));
```

Expected: this does not compile until `rememberLevel(Object levelIdentity)` exists.

- [ ] **Step 2: Run focused red test**

Run: `.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.render.CarriedBabyRenderStateTest`

Expected: FAIL because `rememberLevel` is missing.

- [ ] **Step 3: Implement minimal cleanup code**

Add a volatile `Object CURRENT_LEVEL_IDENTITY` field. `rememberLevel(Object levelIdentity)` should return immediately for `null`, initialize the field on first call, and call `clearAll()` when a non-identical level object appears.

In `CarriedBabyRenderer.collectSubmits`, call:

```java
CarriedBabyRenderState.rememberLevel(client.level);
```

after confirming `client.level` is not null.

- [ ] **Step 4: Run focused green test**

Run: `.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.render.CarriedBabyRenderStateTest`

Expected: PASS.

## Task 3: Wire Animation Into Renderer

**Files:**
- Modify: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyRenderer.java`

- [ ] **Step 1: Use existing focused tests**

No additional production behavior should be added before Tasks 1 and 2 are green. Renderer wiring is compile-verified by the focused tests and final build because it touches Fabric client types.

- [ ] **Step 2: Pass animation time**

Change the renderer placement call to:

```java
return CarriedBabyPlacement.heldPosition(
        base,
        horizontalForward,
        carrier.getBbHeight(),
        baby.getBbHeight(),
        leftMainArm,
        baby.tickCount + tickDelta
);
```

- [ ] **Step 3: Run client render tests**

Run: `.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.render.*`

Expected: PASS.

## Task 4: Docs And Changelog

**Files:**
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Add public Unreleased note**

Add one player-facing line under `## Unreleased`:

```markdown
- Improved the optional modded-client carried-baby render with gentler arm placement, subtle motion, and better cleanup of stale carried visuals after client world changes.
```

- [ ] **Step 2: Run changelog gate later**

Run during final verification: `.\gradlew.bat checkChangelog`

Expected: PASS.

## Task 5: Review And Verification

**Files:**
- Review changed files only.

- [ ] **Step 1: Request review**

Use `superpowers:requesting-code-review`, then `superpowers-review-gates` and `revue-bridge` if the gate routes the durable artifact through Revue.

- [ ] **Step 2: Triage findings**

For each finding, either action it with tests or reject it with code-level evidence. Re-run focused tests and the review gate after valid in-scope fixes.

- [ ] **Step 3: Final verification**

Run:

```powershell
git diff --check
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat checkChangelog
git status --short
```

Expected: all verification commands pass. `git status --short` should show only Phase 5 source/test/docs/changelog files.
