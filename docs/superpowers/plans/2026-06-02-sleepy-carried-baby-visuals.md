# Sleepy Carried Baby Visuals Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make small carried babies visibly transition from alert to sleepy to asleep using client-only render state and frame adjustments.

**Architecture:** Extend the existing `CarriedBabyRenderState`, `CarriedBabyVisualFrame`, and `CarriedBabyRenderer` path. The server remains authoritative for carried state; sleepy/asleep timing is local render-only state keyed by baby entity id.

**Tech Stack:** Java 21, Fabric client render events, JUnit 5, Gradle.

---

## File Structure

- Modify `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyRenderState.java`: store local sleepy/asleep phase timing and expose deterministic phase lookup.
- Create `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabySleepyVisualPhase.java`: enum for `ALERT`, `SLEEPY`, and `ASLEEP`.
- Modify `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyVisualFrame.java`: apply sleepy/asleep frame adjustments even when no petting reaction is active.
- Modify `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyRenderer.java`: request the local sleepy/asleep phase and pass it into frame evaluation.
- Modify `src/test/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyVisualFrameTest.java`: add failing frame tests first.
- Modify `src/test/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyRenderStateTest.java`: add failing timing/idempotence/cleanup tests first.
- Modify `README.md` and `docs/manual-test-plan.md`: explain what players/testers should see.
- Modify `CHANGELOG.md`: public player-visible visual behavior.

---

### Task 1: Sleepy/Asleep Render State Timing

**Files:**
- Create: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabySleepyVisualPhase.java`
- Modify: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyRenderState.java`
- Test: `src/test/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyRenderStateTest.java`

- [ ] **Step 1: Write failing tests**

Add tests to `CarriedBabyRenderStateTest`:

```java
@Test
void localSleepyVisualPhaseTransitionsFromAlertToSleepyToAsleep() {
    CarriedBabyRenderState.set(10, 20);
    CarriedBabyRenderState.ensureLocalSleepyVisual(10, 100L, 60, 40);

    assertEquals(CarriedBabySleepyVisualPhase.ALERT, CarriedBabyRenderState.sleepyVisualPhaseFor(10, 99L, true));
    assertEquals(CarriedBabySleepyVisualPhase.SLEEPY, CarriedBabyRenderState.sleepyVisualPhaseFor(10, 100L, true));
    assertEquals(CarriedBabySleepyVisualPhase.SLEEPY, CarriedBabyRenderState.sleepyVisualPhaseFor(10, 139L, true));
    assertEquals(CarriedBabySleepyVisualPhase.ASLEEP, CarriedBabyRenderState.sleepyVisualPhaseFor(10, 140L, true));
    assertEquals(CarriedBabySleepyVisualPhase.ASLEEP, CarriedBabyRenderState.sleepyVisualPhaseFor(10, 200L, true));
}

@Test
void disabledSleepyVisualConfigReportsAlertPhase() {
    CarriedBabyRenderState.set(10, 20);
    CarriedBabyRenderState.ensureLocalSleepyVisual(10, 100L, 60, 40);

    assertEquals(CarriedBabySleepyVisualPhase.ALERT, CarriedBabyRenderState.sleepyVisualPhaseFor(10, 140L, false));
}

@Test
void ensuringLocalSleepyVisualDoesNotRestartExistingPhaseTiming() {
    CarriedBabyRenderState.set(10, 20);

    CarriedBabyRenderState.ensureLocalSleepyVisual(10, 100L, 60, 40);
    CarriedBabyRenderState.ensureLocalSleepyVisual(10, 500L, 60, 40);

    CarriedBabyRenderState.LocalSleepyVisualState sleepy = CarriedBabyRenderState.localSleepyVisualFor(10).orElseThrow();
    assertEquals(100L, sleepy.sleepyStartTick());
    assertEquals(140L, sleepy.asleepStartTick());
}
```

- [ ] **Step 2: Run the tests and verify RED**

Run:

```powershell
.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.render.CarriedBabyRenderStateTest
```

Expected: compile failure because `CarriedBabySleepyVisualPhase`, the four-argument `ensureLocalSleepyVisual`, and `sleepyVisualPhaseFor` do not exist.

- [ ] **Step 3: Implement minimal render-state phase support**

Create `CarriedBabySleepyVisualPhase.java`:

```java
package dev.jasmine.carrybabyanimals.client.render;

public enum CarriedBabySleepyVisualPhase {
    ALERT,
    SLEEPY,
    ASLEEP
}
```

Update `CarriedBabyRenderState`:

```java
public static Optional<LocalSleepyVisualState> ensureLocalSleepyVisual(
        int babyEntityId,
        long sleepyStartTick,
        int asleepDelayTicks,
        int legacyDurationTicks
) {
    if (!isCarriedBaby(babyEntityId)) {
        return Optional.empty();
    }
    return Optional.of(LOCAL_SLEEPY_VISUALS.computeIfAbsent(
            babyEntityId,
            ignored -> LocalSleepyVisualState.fromDelay(sleepyStartTick, asleepDelayTicks)
    ));
}

public static CarriedBabySleepyVisualPhase sleepyVisualPhaseFor(
        int babyEntityId,
        long currentTick,
        boolean sleepyCarryVisualsEnabled
) {
    if (!sleepyCarryVisualsEnabled) {
        return CarriedBabySleepyVisualPhase.ALERT;
    }
    return localSleepyVisualFor(babyEntityId)
            .map(sleepyVisual -> sleepyVisual.phaseAt(currentTick))
            .orElse(CarriedBabySleepyVisualPhase.ALERT);
}
```

Keep the existing three-argument methods as compatibility wrappers:

```java
public static boolean startLocalSleepyVisual(int babyEntityId, long startTick, int durationTicks) {
    if (!isCarriedBaby(babyEntityId)) {
        return false;
    }
    LOCAL_SLEEPY_VISUALS.put(
            babyEntityId,
            LocalSleepyVisualState.fromDelay(startTick, durationTicks)
    );
    return true;
}

public static Optional<LocalSleepyVisualState> ensureLocalSleepyVisual(
        int babyEntityId,
        long startTick,
        int durationTicks
) {
    return ensureLocalSleepyVisual(babyEntityId, startTick, durationTicks, durationTicks);
}
```

Replace `LocalSleepyVisualState` with:

```java
public record LocalSleepyVisualState(long sleepyStartTick, long asleepStartTick) {
    public LocalSleepyVisualState {
        asleepStartTick = Math.max(sleepyStartTick + 1L, asleepStartTick);
    }

    static LocalSleepyVisualState fromDelay(long sleepyStartTick, int asleepDelayTicks) {
        return new LocalSleepyVisualState(
                sleepyStartTick,
                sleepyStartTick + Math.max(1, asleepDelayTicks)
        );
    }

    public long startTick() {
        return sleepyStartTick;
    }

    public int durationTicks() {
        long duration = asleepStartTick - sleepyStartTick;
        return duration > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) duration;
    }

    public boolean activeAt(long currentTick) {
        return phaseAt(currentTick) != CarriedBabySleepyVisualPhase.ALERT;
    }

    public CarriedBabySleepyVisualPhase phaseAt(long currentTick) {
        if (currentTick < sleepyStartTick) {
            return CarriedBabySleepyVisualPhase.ALERT;
        }
        return currentTick < asleepStartTick
                ? CarriedBabySleepyVisualPhase.SLEEPY
                : CarriedBabySleepyVisualPhase.ASLEEP;
    }
}
```

- [ ] **Step 4: Run the render-state tests and verify GREEN**

Run:

```powershell
.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.render.CarriedBabyRenderStateTest
```

Expected: pass.

---

### Task 2: Frame-Level Hybrid Visuals

**Files:**
- Modify: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyVisualFrame.java`
- Test: `src/test/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyVisualFrameTest.java`

- [ ] **Step 1: Write failing tests**

Add tests to `CarriedBabyVisualFrameTest`:

```java
@Test
void sleepyPhaseLowersAndTucksFrameWithoutPettingReaction() {
    CarriedBabyPlacement.PlacementResult placement = placement();

    CarriedBabyVisualFrame sleepy = CarriedBabyVisualFrame.evaluate(
            placement,
            null,
            0,
            120,
            CarriedBabySleepyVisualPhase.SLEEPY,
            true
    );

    assertTrue(sleepy.position().y < placement.position().y);
    assertTrue(sleepy.pitchDegrees() < placement.pitchDegrees());
    assertEquals(placement.suppressForLocalFirstPerson(), sleepy.suppressForLocalFirstPerson());
}

@Test
void asleepPhaseIsMoreReadableThanSleepyAndAddsOnlyTinyBreathingMotion() {
    CarriedBabyPlacement.PlacementResult placement = placement();

    CarriedBabyVisualFrame sleepy = CarriedBabyVisualFrame.evaluate(
            placement,
            null,
            0,
            140,
            CarriedBabySleepyVisualPhase.SLEEPY,
            true
    );
    CarriedBabyVisualFrame asleep = CarriedBabyVisualFrame.evaluate(
            placement,
            null,
            0,
            140,
            CarriedBabySleepyVisualPhase.ASLEEP,
            true
    );

    assertTrue(asleep.position().y <= sleepy.position().y + 0.012D);
    assertTrue(asleep.pitchDegrees() < sleepy.pitchDegrees());
    assertTrue(Math.abs(asleep.position().y - placement.position().y) <= 0.08D);
}

@Test
void alertPhasePreservesBasePlacementWithoutReaction() {
    CarriedBabyPlacement.PlacementResult placement = placement();

    CarriedBabyVisualFrame alert = CarriedBabyVisualFrame.evaluate(
            placement,
            null,
            0,
            120,
            CarriedBabySleepyVisualPhase.ALERT,
            true
    );

    assertEquals(placement.position(), alert.position());
    assertEquals(placement.pitchDegrees(), alert.pitchDegrees(), 1.0E-6D);
}
```

- [ ] **Step 2: Run the tests and verify RED**

Run:

```powershell
.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.render.CarriedBabyVisualFrameTest
```

Expected: compile failure because the phase overload does not exist.

- [ ] **Step 3: Implement minimal frame adjustments**

Update `CarriedBabyVisualFrame.evaluate` to accept `CarriedBabySleepyVisualPhase` and keep the old boolean overload as a wrapper. Apply base phase adjustments even when no reaction is active:

```java
CarriedBabySleepyVisualPhase phase = sleepyVisualPhase == null
        ? CarriedBabySleepyVisualPhase.ALERT
        : sleepyVisualPhase;
CarriedBabyVisualFrame phaseBase = applySleepyPhase(base, phase, currentTick, rendererSpecificPoseSafe);
```

Use this helper:

```java
private static CarriedBabyVisualFrame applySleepyPhase(
        CarriedBabyPlacement.PlacementResult placement,
        CarriedBabySleepyVisualPhase phase,
        long currentTick,
        boolean rendererSpecificPoseSafe
) {
    if (phase == CarriedBabySleepyVisualPhase.ALERT) {
        return fromPlacement(placement);
    }
    double breathing = phase == CarriedBabySleepyVisualPhase.ASLEEP
            ? Math.sin(currentTick * 0.12D) * 0.006D
            : 0.0D;
    double lower = phase == CarriedBabySleepyVisualPhase.ASLEEP ? -0.045D : -0.025D;
    double pitch = rendererSpecificPoseSafe
            ? (phase == CarriedBabySleepyVisualPhase.ASLEEP ? -10.0D : -5.0D)
            : 0.0D;
    double roll = rendererSpecificPoseSafe
            ? (phase == CarriedBabySleepyVisualPhase.ASLEEP ? 3.0D : 1.5D)
            : 0.0D;
    return new CarriedBabyVisualFrame(
            placement.position().add(0.0D, lower + breathing, 0.0D),
            placement.suppressForLocalFirstPerson(),
            placement.yawDegrees(),
            placement.pitchDegrees() + pitch,
            placement.rollDegrees() + roll
    );
}
```

When a reaction is active, layer the reaction over `phaseBase` with a sleepy scale of `0.35D` for `SLEEPY` and `0.18D` for `ASLEEP`.

- [ ] **Step 4: Run frame tests and verify GREEN**

Run:

```powershell
.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.render.CarriedBabyVisualFrameTest
```

Expected: pass.

---

### Task 3: Renderer Integration

**Files:**
- Modify: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyRenderer.java`
- Test: `src/test/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyVisualFrameTest.java`

- [ ] **Step 1: Write or update a focused test if renderer-facing behavior is not covered**

If Task 2 does not already cover no-reaction sleepy/asleep behavior, add a `CarriedBabyVisualFrameTest` proving `ASLEEP` without a reaction is readable. Do not add Minecraft client mocks.

- [ ] **Step 2: Implement renderer integration**

Change constants:

```java
private static final int LOCAL_SLEEPY_VISUAL_DELAY_TICKS = 1200;
private static final int LOCAL_ASLEEP_VISUAL_DELAY_TICKS = 160;
```

Change scheduling and lookup:

```java
CarriedBabyRenderState.ensureLocalSleepyVisual(
        baby.getId(),
        baby.tickCount + LOCAL_SLEEPY_VISUAL_DELAY_TICKS,
        LOCAL_ASLEEP_VISUAL_DELAY_TICKS,
        LOCAL_ASLEEP_VISUAL_DELAY_TICKS
);
CarriedBabySleepyVisualPhase sleepyVisualPhase = CarriedBabyRenderState.sleepyVisualPhaseFor(
        baby.getId(),
        baby.tickCount,
        visualConfig.sleepyCarryVisualsEnabled()
);
```

Pass `sleepyVisualPhase` to `CarriedBabyVisualFrame.evaluate`.

- [ ] **Step 3: Run focused render tests**

Run:

```powershell
.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.render.*
```

Expected: pass.

---

### Task 4: Docs And Public Changelog

**Files:**
- Modify: `README.md`
- Modify: `docs/manual-test-plan.md`
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Update player/admin docs**

Update the client visual section of `README.md` to say sleepy carry visuals now progress through clearer sleepy/asleep client-only presentation with tucked posture, calmer motion, and a tiny restrained cue where safe.

Update `docs/manual-test-plan.md` with a playtest check:

```markdown
- Hold a small baby animal long enough for sleepy carry visuals to begin. Confirm it first looks tucked and drowsy, then settles into a more readable asleep pose with restrained motion. Confirm pickup, petting, drop, and vanilla passenger fallback behavior still work.
```

- [ ] **Step 2: Update public changelog**

Add an Unreleased `CHANGELOG.md` bullet:

```markdown
- Made optional modded-client sleepy carried-baby visuals easier to read for small babies by adding clearer sleepy/asleep presentation while preserving vanilla-client passenger fallback.
```

- [ ] **Step 3: Run changelog gate**

Run:

```powershell
.\gradlew.bat checkChangelog
```

Expected: pass.

---

### Task 5: Verification And Revue Review

**Files:**
- Review all touched source, tests, and docs.

- [ ] **Step 1: Run final local verification**

Run:

```powershell
git diff --check
.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.render.*
.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.config.*
.\gradlew.bat checkChangelog
.\gradlew.bat build
```

Expected: all pass.

- [ ] **Step 2: Run bounded Revue implementation review**

Use `superpowers-review-gates` and `revue-bridge` for a bounded review over the touched implementation files, tests, and docs. Prefer a scope that avoids packetization where reasonable.

- [ ] **Step 3: Action Revue findings**

Use `superpowers:receiving-code-review`. Fix valid findings, mark false positives only with evidence, and rerun relevant tests.

- [ ] **Step 4: Final report**

Report changed visual states, files changed, Revue review ID and final finding state, verification command outcomes, build artifact path, playtest notes, and confirm no release/tag/publish/deploy was performed.
