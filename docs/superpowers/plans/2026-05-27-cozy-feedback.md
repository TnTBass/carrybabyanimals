# Phase 1 Cozy Feedback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Phase 1 Cozy Feedback only: carried idle sounds, variant petting messages, name-aware messages, sleepy carried-baby moments, and gentle server-visible particles with conservative config defaults.

**Architecture:** Gameplay stays server-owned. New cozy behavior is split into a small message catalog, a deterministic scheduler, and a server-side emitter that is called from the existing carry tick and petting paths. Babies remain real entities, vanilla clients keep the passenger fallback, and no new permissions or required client payloads are introduced.

**Tech Stack:** Minecraft 26.1.2, Java 25, Fabric Loader 0.19.2, Fabric API 0.149.0+26.1.2, JUnit Jupiter, Gradle, existing `dev.jasmine.carrybabyanimals` package layout.

---

## Reference Inputs

- Roadmap spec: `docs/superpowers/specs/2026-05-25-lovable-expansion-roadmap-design.md`
- Existing agent instructions: `AGENTS.md`
- Existing config model: `src/main/java/dev/jasmine/carrybabyanimals/config/CarryConfig.java`
- Existing config load/save: `src/main/java/dev/jasmine/carrybabyanimals/config/CarryConfigManager.java`
- Existing carry tick path: `src/main/java/dev/jasmine/carrybabyanimals/carry/CarryTicker.java`
- Existing petting path: `src/main/java/dev/jasmine/carrybabyanimals/carry/CarryInteractionHandler.java`
- Existing optional client payload checks: `src/main/java/dev/jasmine/carrybabyanimals/network/CarryNetworking.java`

## Scope Boundary

Implement only Phase 1 Cozy Feedback:

- Soft idle sounds while carried.
- Variant petting action-bar messages.
- Name-aware message variants for custom-named babies.
- Sleepy carried-baby moments after carried duration.
- Gentle server-visible particles for sleepy or affectionate reactions.
- Config switches, timing, and cooldowns for the Phase 1 features.

Do not implement Nursery Mode, Parent Reunion, Expanded Modded Animal Support, Client Polish, new set-down validation, new entity-ID resolving, new client rendering work, or new permissions. Existing carry permissions continue to govern who may carry.

## File Structure

- Modify `src/main/java/dev/jasmine/carrybabyanimals/config/CarryConfig.java`: add Phase 1 config fields, compact canonical validation, and conservative defaults.
- Modify `src/main/java/dev/jasmine/carrybabyanimals/config/CarryConfigManager.java`: parse/save Phase 1 fields, clamp invalid timing ranges, and keep unknown older configs loading safely.
- Modify `src/main/java/dev/jasmine/carrybabyanimals/carry/CarryState.java`: store the server tick when the carry started.
- Modify `src/main/java/dev/jasmine/carrybabyanimals/carry/CarryManager.java`: add `beginCarry(UUID, int, long)` and keep the existing two-argument overload for tests or call sites that do not care about elapsed time.
- Modify `src/main/java/dev/jasmine/carrybabyanimals/carry/CarryInteractionHandler.java`: pass pickup game time into `CarryManager`, move petting text selection to the catalog, honor `pettingMessagesEnabled`, `nameAwareMessagesEnabled`, and `cozyParticlesEnabled`.
- Modify `src/main/java/dev/jasmine/carrybabyanimals/carry/CarryTicker.java`: after existing stale-state cleanup, invoke the cozy scheduler for valid carried baby animals.
- Modify `src/main/java/dev/jasmine/carrybabyanimals/CarryBabyAnimals.java`: construct and wire the cozy scheduler into `CarryTicker`.
- Create `src/main/java/dev/jasmine/carrybabyanimals/cozy/CozyFeedbackMessageCatalog.java`: deterministic message-pool selection for petting and sleepy moments, with named and unnamed variants.
- Create `src/main/java/dev/jasmine/carrybabyanimals/cozy/CozyFeedbackScheduler.java`: per-carry timing state for idle sounds, sleepy messages, and sleepy particles.
- Create `src/main/java/dev/jasmine/carrybabyanimals/cozy/CozyFeedbackCarrySnapshot.java`: lightweight testable view of one active carry, separate from Minecraft entity instances.
- Create `src/main/java/dev/jasmine/carrybabyanimals/cozy/CozyFeedbackDecision.java`: package-private record co-located with the scheduler for pure timing decisions.
- Create `src/main/java/dev/jasmine/carrybabyanimals/cozy/CozyFeedbackEmitter.java`: small server-side boundary for sounds, particles, and action-bar messages.
- Create `src/main/java/dev/jasmine/carrybabyanimals/cozy/CozyFeedbackRandom.java`: wrapper around `RandomSource` so scheduler tests can use deterministic rolls.
- Create `src/test/java/dev/jasmine/carrybabyanimals/cozy/CozyFeedbackMessageCatalogTest.java`: message selection and name-aware behavior.
- Create `src/test/java/dev/jasmine/carrybabyanimals/cozy/CozyFeedbackSchedulerTest.java`: idle/sleepy timing, cooldowns, disabled switches, and state cleanup.
- Modify `src/test/java/dev/jasmine/carrybabyanimals/config/CarryConfigManagerTest.java`: defaults, parsing, clamping, generated config text, and migration behavior.
- Modify `src/test/java/dev/jasmine/carrybabyanimals/carry/CarryManagerTest.java`: started-at tick behavior.
- Modify `src/test/java/dev/jasmine/carrybabyanimals/carry/CarryInteractionHandlerTest.java`: catalog-backed petting messages and disabled message behavior.
- Modify `README.md`: document new public config fields, defaults, and cosmetic-only vanilla-client behavior during the implementation phase.
- Modify `CHANGELOG.md`: add public player/server-admin release notes during the implementation phase.

## Config Defaults And Migration

Add these fields to `CarryConfig.defaultConfig()`:

```java
cozyFeedbackEnabled = true
carriedIdleSoundsEnabled = true
carriedIdleSoundMinTicks = 160
carriedIdleSoundMaxTicks = 360
pettingMessagesEnabled = true
nameAwareMessagesEnabled = true
cozyParticlesEnabled = true
sleepyBabiesEnabled = true
sleepyAfterTicks = 1200
sleepyMessageCooldownTicks = 600
sleepyParticleCooldownTicks = 200
```

Canonical `CarryConfig` field order after this phase:

```java
CarryConfig(
    allowedAnimals,
    blockedAnimals,
    allowCarryingOtherPlayersTamedAnimals,
    pettingCooldownTicks,
    restrictToAllowedAnimals,
    cozyFeedbackEnabled,
    carriedIdleSoundsEnabled,
    carriedIdleSoundMinTicks,
    carriedIdleSoundMaxTicks,
    pettingMessagesEnabled,
    nameAwareMessagesEnabled,
    cozyParticlesEnabled,
    sleepyBabiesEnabled,
    sleepyAfterTicks,
    sleepyMessageCooldownTicks,
    sleepyParticleCooldownTicks
)
```

Migration behavior:

- Existing config files that omit Phase 1 fields load with the defaults above because `CarryConfigManager.RawConfig` uses boxed `Boolean` fields for all new booleans and `parse(...)` replaces `null` with the intended default.
- `cozyFeedbackEnabled == null` normalizes to `true`.
- `carriedIdleSoundsEnabled == null` normalizes to `true`.
- `pettingMessagesEnabled == null` normalizes to `true`.
- `nameAwareMessagesEnabled == null` normalizes to `true`.
- `cozyParticlesEnabled == null` normalizes to `true`.
- `sleepyBabiesEnabled == null` normalizes to `true`.
- `carriedIdleSoundMinTicks <= 0` normalizes to `160`.
- `carriedIdleSoundMaxTicks <= 0` normalizes to `360`.
- If `carriedIdleSoundMaxTicks < carriedIdleSoundMinTicks`, normalize max to min so interval generation is stable.
- `sleepyAfterTicks <= 0` normalizes to `1200`.
- `sleepyMessageCooldownTicks <= 0` normalizes to `600`.
- `sleepyParticleCooldownTicks <= 0` normalizes to `200`.
- `cozyFeedbackEnabled=false` disables all Phase 1 idle/sleepy/petting message upgrades while preserving baseline carry behavior and the existing pet cooldown.
- `cozyParticlesEnabled=false` disables only Phase 1 cozy particles; the existing pet heart effect remains tied to current petting behavior unless the implementation deliberately routes pet hearts through the new flag and tests that public behavior.

## Message Catalog Strategy

Create a server-side `CozyFeedbackMessageCatalog` with pure methods:

```java
public final class CozyFeedbackMessageCatalog {
    public String petMessage(String displayName, boolean hasCustomName, CarryConfig config, int variantIndex)
    public String sleepyMessage(String displayName, boolean hasCustomName, CarryConfig config, int variantIndex)
    public static String feedbackName(String displayName, boolean hasCustomName, boolean nameAwareMessagesEnabled)
}
```

Rules:

- If `pettingMessagesEnabled=false`, keep the existing single pet message: `<name> loves you.` or `Baby <type> loves you.`
- If `nameAwareMessagesEnabled=false`, custom-named babies use the unnamed animal-type form for Phase 1 variants.
- Named pet variants include the exact custom display name.
- Unnamed pet variants use `Baby <type>`.
- Sleepy messages use the same naming rules and action-bar delivery.
- Variant selection is deterministic from the caller-provided `variantIndex`, using `Math.floorMod(variantIndex, pool.length)` to make tests stable.
- Keep messages short enough for action bar use and avoid any mechanical gameplay promises.

Initial message pools:

```java
private static final String[] UNNAMED_PET_MESSAGES = {
    "Baby %s loves you.",
    "Baby %s snuggles closer.",
    "Baby %s makes a happy little sound."
};

private static final String[] NAMED_PET_MESSAGES = {
    "%s loves you.",
    "%s snuggles closer.",
    "%s makes a happy little sound."
};

private static final String[] UNNAMED_SLEEPY_MESSAGES = {
    "Baby %s is getting sleepy.",
    "Baby %s settles into your arms.",
    "Baby %s lets out a tiny yawn."
};

private static final String[] NAMED_SLEEPY_MESSAGES = {
    "%s is getting sleepy.",
    "%s settles into your arms.",
    "%s lets out a tiny yawn."
};
```

## Idle Sound And Sleepy Scheduler Design

Create `CozyFeedbackScheduler` as the only owner of Phase 1 per-carry timing state:

```java
public class CozyFeedbackScheduler {
    public void tick(ServerPlayer carrier, Animal baby, CarryState carryState, long gameTime, CarryConfig config)
    CozyFeedbackDecision tickSnapshot(CozyFeedbackCarrySnapshot snapshot, CarryConfig config)
    public void clear(UUID carrierId)
}
```

Internal state:

```java
private final Map<UUID, State> statesByCarrier = new HashMap<>();

private record State(
    int carriedEntityId,
    long nextIdleSoundTick,
    long lastSleepyMessageTick,
    long lastSleepyParticleTick
) {}
```

Scheduler rules:

- The key is carrier UUID; the state also stores carried entity id so a new baby resets timing even if carried by the same player.
- `clear(UUID)` is called whenever carry state ends or becomes stale.
- If `cozyFeedbackEnabled=false`, do not emit idle sounds, sleepy messages, or sleepy particles, and do not create new scheduler state.
- First scheduler tick for a carry initializes `nextIdleSoundTick` to `gameTime + randomInterval(min, max)` and sets sleepy cooldown ticks so the first sleepy message/particle can happen no earlier than `startedAtTick + sleepyAfterTicks`.
- Idle sound emits only when `carriedIdleSoundsEnabled=true` and `gameTime >= nextIdleSoundTick`, then schedules the next idle sound using the configured inclusive range.
- Use quiet vanilla baby-animal sounds by calling `baby.getAmbientSound()` when non-null and `baby.playSound(...)` at low volume/pitch. If the sound is null, skip without logging.
- Sleepy messages emit only when `sleepyBabiesEnabled=true`, `pettingMessagesEnabled=true`, and `gameTime - carryState.startedAtTick() >= sleepyAfterTicks`.
- Sleepy particles emit only when `sleepyBabiesEnabled=true`, `cozyParticlesEnabled=true`, and `gameTime - carryState.startedAtTick() >= sleepyAfterTicks`.
- Sleepy message cooldown is per carried baby via `(carrier UUID, carried entity id)` state.
- Sleepy particle cooldown is per carried baby via the same state.
- Particle effects are server-visible and gentle, using a small count such as one to three `ParticleTypes.HAPPY_VILLAGER` or `ParticleTypes.HEART` around the baby. Use the player-targeted `ServerLevel.sendParticles(...)` overload only for first-person carrier-only effects; Phase 1 sleepy particles should be normal server-visible particles.

## Particle Cooldown Behavior

Particle cooldowns are independent from message cooldowns:

- Petting uses the existing `pettingCooldownTicks`.
- Sleepy message cooldown uses `sleepyMessageCooldownTicks`.
- Sleepy particle cooldown uses `sleepyParticleCooldownTicks`.
- Cooldown checks use `>=`: `gameTime - lastTick >= cooldownTicks`.
- A sleepy message may occur without a particle if particles are disabled or still cooling down.
- A sleepy particle may occur without a message if messages are disabled or still cooling down.
- `dropCurrent(...)`, stale cleanup, logout, death, dimension-change cleanup, and server stopping all clear scheduler state through the interaction path so the next carry starts fresh.

## Task 1: Config Model And Migration

**Files:**
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/config/CarryConfig.java`
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/config/CarryConfigManager.java`
- Modify: `src/test/java/dev/jasmine/carrybabyanimals/config/CarryConfigManagerTest.java`

- [ ] **Step 1: Write failing config default and parse tests**

Add tests:

```java
@Test
void defaultConfigEnablesCozyFeedbackConservatively() {
    CarryConfig config = CarryConfig.defaultConfig();

    assertTrue(config.cozyFeedbackEnabled());
    assertTrue(config.carriedIdleSoundsEnabled());
    assertEquals(160, config.carriedIdleSoundMinTicks());
    assertEquals(360, config.carriedIdleSoundMaxTicks());
    assertTrue(config.pettingMessagesEnabled());
    assertTrue(config.nameAwareMessagesEnabled());
    assertTrue(config.cozyParticlesEnabled());
    assertTrue(config.sleepyBabiesEnabled());
    assertEquals(1200, config.sleepyAfterTicks());
    assertEquals(600, config.sleepyMessageCooldownTicks());
    assertEquals(200, config.sleepyParticleCooldownTicks());
}

@Test
void parsedCozyConfigUsesExplicitValues() {
    String json = """
        {
          "cozyFeedbackEnabled": false,
          "carriedIdleSoundsEnabled": false,
          "carriedIdleSoundMinTicks": 40,
          "carriedIdleSoundMaxTicks": 80,
          "pettingMessagesEnabled": false,
          "nameAwareMessagesEnabled": false,
          "cozyParticlesEnabled": false,
          "sleepyBabiesEnabled": false,
          "sleepyAfterTicks": 200,
          "sleepyMessageCooldownTicks": 100,
          "sleepyParticleCooldownTicks": 50
        }
        """;

    CarryConfig config = CarryConfigManager.parse(json);

    assertFalse(config.cozyFeedbackEnabled());
    assertFalse(config.carriedIdleSoundsEnabled());
    assertEquals(40, config.carriedIdleSoundMinTicks());
    assertEquals(80, config.carriedIdleSoundMaxTicks());
    assertFalse(config.pettingMessagesEnabled());
    assertFalse(config.nameAwareMessagesEnabled());
    assertFalse(config.cozyParticlesEnabled());
    assertFalse(config.sleepyBabiesEnabled());
    assertEquals(200, config.sleepyAfterTicks());
    assertEquals(100, config.sleepyMessageCooldownTicks());
    assertEquals(50, config.sleepyParticleCooldownTicks());
}

@Test
void parsedCozyConfigNormalizesInvalidTimingValues() {
    String json = """
        {
          "carriedIdleSoundMinTicks": -1,
          "carriedIdleSoundMaxTicks": 20,
          "sleepyAfterTicks": 0,
          "sleepyMessageCooldownTicks": -5,
          "sleepyParticleCooldownTicks": -10
        }
        """;

    CarryConfig config = CarryConfigManager.parse(json);

    assertEquals(160, config.carriedIdleSoundMinTicks());
    assertEquals(160, config.carriedIdleSoundMaxTicks());
    assertEquals(1200, config.sleepyAfterTicks());
    assertEquals(600, config.sleepyMessageCooldownTicks());
    assertEquals(200, config.sleepyParticleCooldownTicks());
}

@Test
void parsedOlderConfigDefaultsMissingCozyBooleansToEnabled() {
    CarryConfig config = CarryConfigManager.parse("{}");

    assertTrue(config.cozyFeedbackEnabled());
    assertTrue(config.carriedIdleSoundsEnabled());
    assertTrue(config.pettingMessagesEnabled());
    assertTrue(config.nameAwareMessagesEnabled());
    assertTrue(config.cozyParticlesEnabled());
    assertTrue(config.sleepyBabiesEnabled());
}
```

- [ ] **Step 2: Run config tests and verify expected failure**

Run:

```powershell
.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.config.CarryConfigManagerTest
```

Expected: FAIL because the new `CarryConfig` accessors do not exist.

- [ ] **Step 3: Implement config fields and parser normalization**

Update `CarryConfig` record fields and constructors. Update `CarryConfigManager.RawConfig`, `RawConfig.from(...)`, and `parse(...)` so all new booleans are boxed in `RawConfig`, missing booleans default to enabled, and missing or invalid timing values normalize as described in this plan.

- [ ] **Step 4: Run config tests and verify pass**

Run:

```powershell
.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.config.CarryConfigManagerTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/dev/jasmine/carrybabyanimals/config/CarryConfig.java src/main/java/dev/jasmine/carrybabyanimals/config/CarryConfigManager.java src/test/java/dev/jasmine/carrybabyanimals/config/CarryConfigManagerTest.java
git commit -m "Add cozy feedback config defaults"
```

## Task 2: Carry Start Tick Tracking

**Files:**
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/carry/CarryState.java`
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/carry/CarryManager.java`
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/carry/CarryInteractionHandler.java`
- Modify: `src/test/java/dev/jasmine/carrybabyanimals/carry/CarryManagerTest.java`

- [ ] **Step 1: Write failing carry start tick tests**

Add tests:

```java
@Test
void beginCarryRecordsStartedAtTick() {
    CarryManager manager = new CarryManager();
    UUID playerId = UUID.randomUUID();

    assertTrue(manager.beginCarry(playerId, 42, 1234L));

    assertEquals(1234L, manager.activeCarries().get(playerId).startedAtTick());
}

@Test
void legacyBeginCarryDefaultsStartedAtTickToZero() {
    CarryManager manager = new CarryManager();
    UUID playerId = UUID.randomUUID();

    assertTrue(manager.beginCarry(playerId, 42));

    assertEquals(0L, manager.activeCarries().get(playerId).startedAtTick());
}
```

- [ ] **Step 2: Run carry manager tests and verify expected failure**

Run:

```powershell
.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.carry.CarryManagerTest
```

Expected: FAIL because `beginCarry(UUID, int, long)` does not exist.

- [ ] **Step 3: Implement start tick storage**

Add `beginCarry(UUID, int, long)` to `CarryManager`, have the existing overload call it with `0L`, and update `CarryInteractionHandler.onEntityInteract(...)` to pass `((ServerLevel) player.level()).getGameTime()` when pickup begins.

- [ ] **Step 4: Run carry manager and interaction tests**

Run:

```powershell
.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.carry.CarryManagerTest --tests dev.jasmine.carrybabyanimals.carry.CarryInteractionHandlerTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/dev/jasmine/carrybabyanimals/carry/CarryState.java src/main/java/dev/jasmine/carrybabyanimals/carry/CarryManager.java src/main/java/dev/jasmine/carrybabyanimals/carry/CarryInteractionHandler.java src/test/java/dev/jasmine/carrybabyanimals/carry/CarryManagerTest.java
git commit -m "Track carry start ticks"
```

## Task 3: Cozy Message Catalog

**Files:**
- Create: `src/main/java/dev/jasmine/carrybabyanimals/cozy/CozyFeedbackMessageCatalog.java`
- Create: `src/test/java/dev/jasmine/carrybabyanimals/cozy/CozyFeedbackMessageCatalogTest.java`
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/carry/CarryInteractionHandler.java`
- Modify: `src/test/java/dev/jasmine/carrybabyanimals/carry/CarryInteractionHandlerTest.java`

- [ ] **Step 1: Write failing catalog tests**

Create `CozyFeedbackMessageCatalogTest` with tests:

```java
@Test
void petMessagesUseUnnamedBabyTypeVariants() {
    CozyFeedbackMessageCatalog catalog = new CozyFeedbackMessageCatalog();

    assertEquals("Baby Pig loves you.", catalog.petMessage("Pig", false, CarryConfig.defaultConfig(), 0));
    assertEquals("Baby Pig snuggles closer.", catalog.petMessage("Pig", false, CarryConfig.defaultConfig(), 1));
    assertEquals("Baby Pig makes a happy little sound.", catalog.petMessage("Pig", false, CarryConfig.defaultConfig(), 2));
}

@Test
void petMessagesUseCustomNameWhenNameAwareMessagesEnabled() {
    CozyFeedbackMessageCatalog catalog = new CozyFeedbackMessageCatalog();

    assertEquals("Shelly snuggles closer.", catalog.petMessage("Shelly", true, CarryConfig.defaultConfig(), 1));
}

@Test
void disabledNameAwareMessagesUseBabyTypeStyle() {
    // Field order after existing config fields:
    // cozyFeedbackEnabled, carriedIdleSoundsEnabled, idle min, idle max,
    // pettingMessagesEnabled, nameAwareMessagesEnabled, cozyParticlesEnabled,
    // sleepyBabiesEnabled, sleepyAfterTicks, sleepyMessageCooldownTicks,
    // sleepyParticleCooldownTicks.
    CarryConfig config = new CarryConfig(List.of(), List.of(), false, 20, false, true, true, 160, 360, true, false, true, true, 1200, 600, 200);
    CozyFeedbackMessageCatalog catalog = new CozyFeedbackMessageCatalog();

    assertEquals("Baby Shelly snuggles closer.", catalog.petMessage("Shelly", true, config, 1));
}

@Test
void disabledVariantMessagesUseExistingSinglePetMessage() {
    // Field order after existing config fields:
    // cozyFeedbackEnabled, carriedIdleSoundsEnabled, idle min, idle max,
    // pettingMessagesEnabled, nameAwareMessagesEnabled, cozyParticlesEnabled,
    // sleepyBabiesEnabled, sleepyAfterTicks, sleepyMessageCooldownTicks,
    // sleepyParticleCooldownTicks.
    CarryConfig config = new CarryConfig(List.of(), List.of(), false, 20, false, true, true, 160, 360, false, true, true, true, 1200, 600, 200);
    CozyFeedbackMessageCatalog catalog = new CozyFeedbackMessageCatalog();

    assertEquals("Shelly loves you.", catalog.petMessage("Shelly", true, config, 2));
}

@Test
void sleepyMessagesUseConfiguredNameRules() {
    CozyFeedbackMessageCatalog catalog = new CozyFeedbackMessageCatalog();

    assertEquals("Baby Pig is getting sleepy.", catalog.sleepyMessage("Pig", false, CarryConfig.defaultConfig(), 0));
    assertEquals("Shelly settles into your arms.", catalog.sleepyMessage("Shelly", true, CarryConfig.defaultConfig(), 1));
}
```

- [ ] **Step 2: Run catalog tests and verify expected failure**

Run:

```powershell
.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.cozy.CozyFeedbackMessageCatalogTest
```

Expected: FAIL because `CozyFeedbackMessageCatalog` does not exist.

- [ ] **Step 3: Implement catalog**

Create the catalog with the message pools and `Math.floorMod(...)` selection described in this plan.

- [ ] **Step 4: Wire petting text through catalog**

Add a `CozyFeedbackMessageCatalog` dependency to `CarryInteractionHandler`, preserve a package-private constructor for lightweight tests, and replace `petFeedbackText(...)` calls with `catalog.petMessage(...)`.

- [ ] **Step 5: Run catalog and interaction tests**

Run:

```powershell
.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.cozy.CozyFeedbackMessageCatalogTest --tests dev.jasmine.carrybabyanimals.carry.CarryInteractionHandlerTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add src/main/java/dev/jasmine/carrybabyanimals/cozy/CozyFeedbackMessageCatalog.java src/test/java/dev/jasmine/carrybabyanimals/cozy/CozyFeedbackMessageCatalogTest.java src/main/java/dev/jasmine/carrybabyanimals/carry/CarryInteractionHandler.java src/test/java/dev/jasmine/carrybabyanimals/carry/CarryInteractionHandlerTest.java
git commit -m "Add cozy feedback message variants"
```

## Task 4: Scheduler Unit And Emitter Boundary

**Files:**
- Create: `src/main/java/dev/jasmine/carrybabyanimals/cozy/CozyFeedbackScheduler.java`
- Create: `src/main/java/dev/jasmine/carrybabyanimals/cozy/CozyFeedbackCarrySnapshot.java`
- Create: `src/main/java/dev/jasmine/carrybabyanimals/cozy/CozyFeedbackEmitter.java`
- Create: `src/main/java/dev/jasmine/carrybabyanimals/cozy/CozyFeedbackRandom.java`
- Create: `src/test/java/dev/jasmine/carrybabyanimals/cozy/CozyFeedbackSchedulerTest.java`

- [ ] **Step 1: Write failing scheduler tests with a fake emitter**

Create tests against the package-private snapshot seam and deterministic random:

```java
@Test
void disabledMasterSwitchEmitsNothing() {
    CozyFeedbackScheduler scheduler = new CozyFeedbackScheduler(new CozyFeedbackMessageCatalog(), fixedRandom(0));
    CarryConfig config = new CarryConfig(List.of(), List.of(), false, 20, false, false, true, 160, 360, true, true, true, true, 1200, 600, 200);

    CozyFeedbackDecision decision = scheduler.tickSnapshot(snapshot(7, "Pig", false, 0L, 2000L), config);

    assertFalse(decision.playIdleSound());
    assertTrue(decision.sleepyMessage().isEmpty());
    assertFalse(decision.spawnSleepyParticles());
}

@Test
void idleSoundWaitsUntilScheduledTickThenReschedules() {
    CozyFeedbackScheduler scheduler = new CozyFeedbackScheduler(new CozyFeedbackMessageCatalog(), fixedRandom(0));

    CozyFeedbackDecision first = scheduler.tickSnapshot(snapshot(7, "Pig", false, 0L, 100L), CarryConfig.defaultConfig());
    CozyFeedbackDecision beforeDue = scheduler.tickSnapshot(snapshot(7, "Pig", false, 0L, 259L), CarryConfig.defaultConfig());
    CozyFeedbackDecision due = scheduler.tickSnapshot(snapshot(7, "Pig", false, 0L, 260L), CarryConfig.defaultConfig());

    assertFalse(first.playIdleSound());
    assertFalse(beforeDue.playIdleSound());
    assertTrue(due.playIdleSound());
}

@Test
void sleepyFeedbackWaitsForCarryDurationAndCooldowns() {
    CozyFeedbackScheduler scheduler = new CozyFeedbackScheduler(new CozyFeedbackMessageCatalog(), fixedRandom(0));

    CozyFeedbackDecision beforeDue = scheduler.tickSnapshot(snapshot(7, "Pig", false, 100L, 1299L), CarryConfig.defaultConfig());
    CozyFeedbackDecision due = scheduler.tickSnapshot(snapshot(7, "Pig", false, 100L, 1300L), CarryConfig.defaultConfig());
    CozyFeedbackDecision particleOnly = scheduler.tickSnapshot(snapshot(7, "Pig", false, 100L, 1500L), CarryConfig.defaultConfig());

    assertTrue(beforeDue.sleepyMessage().isEmpty());
    assertFalse(beforeDue.spawnSleepyParticles());
    assertEquals(Optional.of("Baby Pig is getting sleepy."), due.sleepyMessage());
    assertTrue(due.spawnSleepyParticles());
    assertTrue(particleOnly.sleepyMessage().isEmpty());
    assertTrue(particleOnly.spawnSleepyParticles());
}

@Test
void newCarriedEntityResetsSchedulerStateForSameCarrier() {
    CozyFeedbackScheduler scheduler = new CozyFeedbackScheduler(new CozyFeedbackMessageCatalog(), fixedRandom(0));

    CozyFeedbackDecision oldCarry = scheduler.tickSnapshot(snapshot(7, "Pig", false, 0L, 260L), CarryConfig.defaultConfig());
    CozyFeedbackDecision newCarry = scheduler.tickSnapshot(snapshot(8, "Cow", false, 260L, 261L), CarryConfig.defaultConfig());

    assertTrue(oldCarry.playIdleSound());
    assertFalse(newCarry.playIdleSound());
}
```

Define the test helper in the same test class:

```java
private static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

private static CozyFeedbackCarrySnapshot snapshot(
        int carriedEntityId,
        String displayName,
        boolean hasCustomName,
        long startedAtTick,
        long gameTime
) {
    return new CozyFeedbackCarrySnapshot(PLAYER_ID, carriedEntityId, displayName, hasCustomName, startedAtTick, gameTime);
}

private static CozyFeedbackRandom fixedRandom(int value) {
    return (inclusiveMin, inclusiveMax) -> inclusiveMin + Math.floorMod(value, inclusiveMax - inclusiveMin + 1);
}
```

Do not add Mockito for this phase. Keep entity-specific emission behind `CozyFeedbackEmitter` and keep timing tests pure.

- [ ] **Step 2: Run scheduler tests and verify expected failure**

Run:

```powershell
.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.cozy.CozyFeedbackSchedulerTest
```

Expected: FAIL because scheduler classes do not exist.

- [ ] **Step 3: Implement scheduler and emitter boundary**

Implement the scheduler rules from this plan. `tickSnapshot(...)` returns the package-private `CozyFeedbackDecision` record:

```java
record CozyFeedbackDecision(boolean playIdleSound, Optional<String> sleepyMessage, boolean spawnSleepyParticles) {}
```

`tick(ServerPlayer, Animal, CarryState, long, CarryConfig)` builds the snapshot, asks `tickSnapshot(...)` for a decision, and then delegates real Minecraft calls to `CozyFeedbackEmitter`:

```java
public void playIdleSound(Animal baby)
public void showSleepyMessage(ServerPlayer carrier, String message)
public void spawnSleepyParticles(ServerLevel level, Animal baby)
```

Use low-volume ambient sounds and small visible particle counts. Keep the scheduler free of networking payloads.

- [ ] **Step 4: Run scheduler tests**

Run:

```powershell
.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.cozy.CozyFeedbackSchedulerTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/dev/jasmine/carrybabyanimals/cozy src/test/java/dev/jasmine/carrybabyanimals/cozy/CozyFeedbackSchedulerTest.java
git commit -m "Add cozy feedback scheduler"
```

## Task 5: Tick And Cleanup Integration

**Files:**
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/carry/CarryTicker.java`
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/carry/CarryInteractionHandler.java`
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/CarryBabyAnimals.java`
- Modify: `src/test/java/dev/jasmine/carrybabyanimals/carry/CarryInteractionHandlerTest.java`

- [ ] **Step 1: Write failing cleanup and wiring tests**

Add tests or package-private verification seams that prove:

```java
@Test
void droppingCurrentCarryClearsCozySchedulerState() {
    RecordingCozyFeedbackScheduler scheduler = new RecordingCozyFeedbackScheduler();
    CarryInteractionHandler handler = handlerWithScheduler(scheduler);

    handler.clearCarryFeedbackState(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    assertEquals(List.of(UUID.fromString("00000000-0000-0000-0000-000000000001")), scheduler.clearedCarrierIds());
}

private static CarryInteractionHandler handlerWithScheduler(CozyFeedbackScheduler scheduler) {
    return new CarryInteractionHandler(null, null, null, null, null, new CozyFeedbackMessageCatalog(), scheduler);
}

private static final class RecordingCozyFeedbackScheduler extends CozyFeedbackScheduler {
    private final List<UUID> clearedCarrierIds = new ArrayList<>();

    RecordingCozyFeedbackScheduler() {
        super(new CozyFeedbackMessageCatalog(), (inclusiveMin, inclusiveMax) -> inclusiveMin);
    }

    @Override
    public void clear(UUID carrierId) {
        clearedCarrierIds.add(carrierId);
    }

    List<UUID> clearedCarrierIds() {
        return List.copyOf(clearedCarrierIds);
    }
}
```

If direct interaction tests cannot instantiate Minecraft server entities cleanly, keep the test at the helper-method seam that `dropCurrent(...)`, stale cleanup, and level-change cleanup all call.

- [ ] **Step 2: Run interaction tests and verify expected failure**

Run:

```powershell
.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.carry.CarryInteractionHandlerTest
```

Expected: FAIL because cozy scheduler cleanup wiring does not exist.

- [ ] **Step 3: Integrate scheduler into tick and cleanup paths**

Wire `CarryTicker` constructor to accept `CozyFeedbackScheduler`. For each active carry, keep the existing invalid-state drop behavior. For valid `(ServerPlayer, Animal, CarryState)` tuples, call scheduler `tick(...)` with `server.overworld().getGameTime()` or the player level game time. Add one cleanup helper in `CarryInteractionHandler` so every carry end path clears pet cooldown and cozy scheduler state together.

- [ ] **Step 4: Run targeted carry tests**

Run:

```powershell
.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.carry.CarryInteractionHandlerTest --tests dev.jasmine.carrybabyanimals.carry.CarryManagerTest --tests dev.jasmine.carrybabyanimals.cozy.CozyFeedbackSchedulerTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/dev/jasmine/carrybabyanimals/carry/CarryTicker.java src/main/java/dev/jasmine/carrybabyanimals/carry/CarryInteractionHandler.java src/main/java/dev/jasmine/carrybabyanimals/CarryBabyAnimals.java src/test/java/dev/jasmine/carrybabyanimals/carry/CarryInteractionHandlerTest.java
git commit -m "Wire cozy feedback into carry ticks"
```

## Task 6: Public Docs And Changelog For Implementation

**Files:**
- Modify: `README.md`
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Update README config documentation**

Add the Phase 1 fields to the default config example and options list. State that Cozy Feedback is cosmetic, enabled by default, server-owned, and visible to vanilla clients through ordinary sounds, particles, and action-bar messages.

- [ ] **Step 2: Update public changelog**

Add a `CHANGELOG.md` Unreleased entry:

```markdown
- Added optional Cozy Feedback for carried babies, including softer carried idle sounds, varied petting messages, sleepy moments, and gentle cosmetic particles controlled by server config.
```

- [ ] **Step 3: Run changelog gate**

Run:

```powershell
.\gradlew.bat checkChangelog
```

Expected: PASS.

- [ ] **Step 4: Commit**

```powershell
git add README.md CHANGELOG.md
git commit -m "Document cozy feedback config"
```

## Task 7: Full Verification

**Files:**
- No new source files.

- [ ] **Step 1: Run targeted tests**

Run:

```powershell
.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.config.CarryConfigManagerTest --tests dev.jasmine.carrybabyanimals.carry.CarryManagerTest --tests dev.jasmine.carrybabyanimals.carry.CarryInteractionHandlerTest --tests dev.jasmine.carrybabyanimals.cozy.CozyFeedbackMessageCatalogTest --tests dev.jasmine.carrybabyanimals.cozy.CozyFeedbackSchedulerTest
```

Expected: PASS.

- [ ] **Step 2: Run full tests**

Run:

```powershell
.\gradlew.bat test
```

Expected: PASS.

- [ ] **Step 3: Run full build**

Run:

```powershell
.\gradlew.bat build
```

Expected: PASS.

- [ ] **Step 4: Run changelog gate**

Run:

```powershell
.\gradlew.bat checkChangelog
```

Expected: PASS.

- [ ] **Step 5: Inspect final diff**

Run:

```powershell
git diff --check
git status --short
```

Expected: `git diff --check` exits 0, and `git status --short` lists only intentional Phase 1 implementation, docs, and changelog files before final commit.

## Compatibility Contracts To Preserve During Implementation

- Do not add Phase 1 permissions. `carrybabyanimals.carry`, `carrybabyanimals.carry.tamed`, and `carrybabyanimals.carry.others_tamed` remain the carry gate.
- Do not require CarryBabyAnimals on the client. Sounds, particles, and action-bar messages are vanilla-compatible server effects.
- Do not add custom payloads for idle or sleepy feedback in Phase 1. If a small optional client hint is considered during implementation, it must be rejected from Phase 1 unless it is proven necessary and guarded with `ServerPlayNetworking.canSend(...)`.
- Do not change baby age, health, breeding, taming, trust, movement speed, AI goals beyond the existing carried suppression, or ownership.
- Do not change set-down safety rules in Phase 1.

## Self-Review Checklist

- Phase 1 features from the roadmap are covered by Tasks 1 through 5.
- Config switches, timing values, cooldowns, defaults, and migration behavior are covered by Task 1.
- Message catalog and name-aware variants are covered by Task 3.
- Idle sound and sleepy feedback scheduling are covered by Task 4 and Task 5.
- Particle cooldown behavior is specified separately from message and petting cooldowns.
- Public docs and release notes for the eventual implementation are covered by Task 6.
- Verification commands include targeted tests, full tests, full build, `git diff --check`, and `.\gradlew.bat checkChangelog`.
- Nursery Mode, Parent Reunion, Expanded Modded Animal Support, and Client Polish are excluded except for explicit compatibility contracts.
- No new Phase 1 permissions are planned.
- Vanilla-client compatibility is preserved by using server-visible vanilla sounds, particles, action-bar messages, and the existing optional payload guard pattern.
