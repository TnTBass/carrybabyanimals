# Parent Reunion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Phase 3 Parent Reunion so safely set-down carried babies get cosmetic hearts and a warm action-bar message when returned near a compatible adult.

**Architecture:** Keep the feature server-owned and vanilla-client compatible by running reunion detection only after a successful deliberate server-side drop. Add a focused `reunion` package for compatibility, cooldown, message, and particle decisions, then wire it into `CarryInteractionHandler` after Nursery Mode has allowed the drop. Compatible adult means same `EntityType`, adult `Animal`, alive, inside a conservative radius, in the same loaded server level, and tamed-ownership-compatible when the baby and adult are tamed animals.

**Tech Stack:** Java 25, Fabric 26.1.2, Minecraft server entity APIs, JUnit 5, Gradle.

---

## Scope Boundaries

Implement only Phase 3 Parent Reunion.

Do not implement Phase 4 Expanded Modded Animal Support, Phase 5 Client Polish, release, push, tag, or publish workflow.

Reunion is cosmetic only:

- no breeding
- no growth acceleration
- no taming
- no ownership transfer
- no AI rewrites
- no client-required packets
- no movement into unsafe positions

## File Structure

- Create `src/main/java/dev/jasmine/carrybabyanimals/reunion/ParentReunionMatch.java`: immutable result describing the baby, matched adult, and feedback name.
- Create `src/main/java/dev/jasmine/carrybabyanimals/reunion/ParentReunionFinder.java`: server-side adult search and compatibility checks.
- Create `src/main/java/dev/jasmine/carrybabyanimals/reunion/ParentReunionCooldowns.java`: per-baby and per-carrier cooldown guard.
- Create `src/main/java/dev/jasmine/carrybabyanimals/reunion/ParentReunionMessageCatalog.java`: small warm action-bar message variants.
- Create `src/main/java/dev/jasmine/carrybabyanimals/reunion/ParentReunionFeedback.java`: sends heart particles around baby/adult and returns the selected message.
- Create `src/test/java/dev/jasmine/carrybabyanimals/reunion/ParentReunionFinderTest.java`: pure compatibility and radius tests using package-visible helper seams.
- Create `src/test/java/dev/jasmine/carrybabyanimals/reunion/ParentReunionCooldownsTest.java`: cooldown tests.
- Create `src/test/java/dev/jasmine/carrybabyanimals/reunion/ParentReunionMessageCatalogTest.java`: message tests.
- Modify `src/main/java/dev/jasmine/carrybabyanimals/config/CarryConfig.java`: add Phase 3 defaults and accessors.
- Modify `src/main/java/dev/jasmine/carrybabyanimals/config/CarryConfigManager.java`: parse and save Phase 3 config fields.
- Modify `src/test/java/dev/jasmine/carrybabyanimals/config/CarryConfigManagerTest.java`: config default, explicit value, and normalization tests.
- Modify `src/main/java/dev/jasmine/carrybabyanimals/carry/CarryInteractionHandler.java`: inject reunion services and trigger feedback only after a successful safe deliberate drop.
- Modify `src/test/java/dev/jasmine/carrybabyanimals/carry/CarryInteractionHandlerTest.java`: helper-decision tests proving reunion is skipped on Nursery refusal and disabled config.
- Modify `README.md`: document behavior, config defaults, vanilla-client compatibility, and ownership safety.
- Modify `docs/manual-test-plan.md`: add parent reunion manual test rows.
- Modify `CHANGELOG.md`: add public Unreleased Phase 3 bullet.

## Task 1: Config Defaults And Parsing

**Files:**
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/config/CarryConfig.java`
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/config/CarryConfigManager.java`
- Test: `src/test/java/dev/jasmine/carrybabyanimals/config/CarryConfigManagerTest.java`

- [ ] **Step 1: Write failing config tests**

Add tests:

```java
@Test
void defaultConfigEnablesParentReunionConservatively() {
    CarryConfig config = CarryConfig.defaultConfig();

    assertTrue(config.parentReunionEnabled());
    assertEquals(8, config.parentReunionRadiusBlocks());
    assertEquals(200, config.parentReunionCooldownTicks());
    assertTrue(config.parentReunionMessagesEnabled());
    assertTrue(config.parentReunionParticlesEnabled());
}

@Test
void parsedParentReunionConfigUsesExplicitValues() {
    CarryConfig config = CarryConfigManager.parse("""
        {
          "parentReunionEnabled": false,
          "parentReunionRadiusBlocks": 5,
          "parentReunionCooldownTicks": 400,
          "parentReunionMessagesEnabled": false,
          "parentReunionParticlesEnabled": false
        }
        """);

    assertFalse(config.parentReunionEnabled());
    assertEquals(5, config.parentReunionRadiusBlocks());
    assertEquals(400, config.parentReunionCooldownTicks());
    assertFalse(config.parentReunionMessagesEnabled());
    assertFalse(config.parentReunionParticlesEnabled());
}

@Test
void parsedParentReunionConfigNormalizesInvalidTimingAndRadius() {
    CarryConfig config = CarryConfigManager.parse("""
        {
          "parentReunionRadiusBlocks": 0,
          "parentReunionCooldownTicks": -1
        }
        """);

    assertEquals(8, config.parentReunionRadiusBlocks());
    assertEquals(200, config.parentReunionCooldownTicks());
}
```

- [ ] **Step 2: Run config tests and verify RED**

Run: `.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.config.CarryConfigManagerTest"`

Expected: FAIL because `CarryConfig` has no `parentReunion*` accessors yet.

- [ ] **Step 3: Add config fields**

Add to the `CarryConfig` record after Nursery fields:

```java
boolean parentReunionEnabled,
int parentReunionRadiusBlocks,
int parentReunionCooldownTicks,
boolean parentReunionMessagesEnabled,
boolean parentReunionParticlesEnabled
```

Thread defaults through constructors and `defaultConfig()`:

```java
true,
8,
200,
true,
true
```

In `CarryConfigManager.parse(...)`, append:

```java
enabledByDefault(raw.parentReunionEnabled),
positiveOrDefault(raw.parentReunionRadiusBlocks, 8),
positiveOrDefault(raw.parentReunionCooldownTicks, 200),
enabledByDefault(raw.parentReunionMessagesEnabled),
enabledByDefault(raw.parentReunionParticlesEnabled)
```

In `filterAndLogUnknownAnimalNames(...)`, preserve the new config fields when rebuilding `CarryConfig`.

In `RawConfig`, add:

```java
Boolean parentReunionEnabled;
Integer parentReunionRadiusBlocks;
Integer parentReunionCooldownTicks;
Boolean parentReunionMessagesEnabled;
Boolean parentReunionParticlesEnabled;
```

In `RawConfig.from(...)`, copy each new field from `config`.

- [ ] **Step 4: Run config tests and verify GREEN**

Run: `.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.config.CarryConfigManagerTest"`

Expected: PASS.

## Task 2: Reunion Matching And Cooldown Model

**Files:**
- Create: `src/main/java/dev/jasmine/carrybabyanimals/reunion/ParentReunionMatch.java`
- Create: `src/main/java/dev/jasmine/carrybabyanimals/reunion/ParentReunionFinder.java`
- Create: `src/main/java/dev/jasmine/carrybabyanimals/reunion/ParentReunionCooldowns.java`
- Test: `src/test/java/dev/jasmine/carrybabyanimals/reunion/ParentReunionFinderTest.java`
- Test: `src/test/java/dev/jasmine/carrybabyanimals/reunion/ParentReunionCooldownsTest.java`

- [ ] **Step 1: Write failing model tests**

Add `ParentReunionCooldownsTest`:

```java
@Test
void missingCooldownAllowsReunion() {
    ParentReunionCooldowns cooldowns = new ParentReunionCooldowns();

    assertTrue(cooldowns.canReunite(UUID.randomUUID(), 10, 100L, 200));
}

@Test
void rememberedBabyOrCarrierBlocksUntilCooldownExpires() {
    ParentReunionCooldowns cooldowns = new ParentReunionCooldowns();
    UUID carrierId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    cooldowns.remember(carrierId, 42, 100L);

    assertFalse(cooldowns.canReunite(carrierId, 99, 299L, 200));
    assertFalse(cooldowns.canReunite(UUID.randomUUID(), 42, 299L, 200));
    assertTrue(cooldowns.canReunite(carrierId, 42, 300L, 200));
}

@Test
void clearingCarrierRemovesCarrierCooldownOnly() {
    ParentReunionCooldowns cooldowns = new ParentReunionCooldowns();
    UUID carrierId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    cooldowns.remember(carrierId, 42, 100L);
    cooldowns.clearCarrier(carrierId);

    assertTrue(cooldowns.canReunite(carrierId, 99, 101L, 200));
    assertFalse(cooldowns.canReunite(UUID.randomUUID(), 42, 101L, 200));
}
```

Add `ParentReunionFinderTest` with helper-seam tests:

```java
@Test
void sameTypeAdultInsideRadiusIsCompatible() {
    assertTrue(ParentReunionFinder.compatibleCandidate(
            EntityType.PIG,
            false,
            Optional.empty(),
            EntityType.PIG,
            false,
            Optional.empty(),
            6.0D,
            8
    ));
}

@Test
void differentTypeOrBabyAdultCandidateIsRejected() {
    assertFalse(ParentReunionFinder.compatibleCandidate(
            EntityType.PIG, false, Optional.empty(),
            EntityType.COW, false, Optional.empty(),
            3.0D, 8
    ));
    assertFalse(ParentReunionFinder.compatibleCandidate(
            EntityType.PIG, false, Optional.empty(),
            EntityType.PIG, true, Optional.empty(),
            3.0D, 8
    ));
}

@Test
void candidateOutsideRadiusIsRejected() {
    assertFalse(ParentReunionFinder.compatibleCandidate(
            EntityType.PIG, false, Optional.empty(),
            EntityType.PIG, false, Optional.empty(),
            8.01D, 8
    ));
}

@Test
void tamedAnimalsRequireSameOwner() {
    UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID other = UUID.fromString("00000000-0000-0000-0000-000000000002");

    assertTrue(ParentReunionFinder.compatibleCandidate(
            EntityType.WOLF, false, Optional.of(owner),
            EntityType.WOLF, false, Optional.of(owner),
            2.0D, 8
    ));
    assertFalse(ParentReunionFinder.compatibleCandidate(
            EntityType.WOLF, false, Optional.of(owner),
            EntityType.WOLF, false, Optional.of(other),
            2.0D, 8
    ));
}
```

- [ ] **Step 2: Run model tests and verify RED**

Run:

`.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.reunion.ParentReunionCooldownsTest" --tests "dev.jasmine.carrybabyanimals.reunion.ParentReunionFinderTest"`

Expected: FAIL because the `reunion` package does not exist.

- [ ] **Step 3: Implement the model**

Create `ParentReunionMatch`:

```java
public record ParentReunionMatch(Animal baby, Animal adult, String babyFeedbackName) {
}
```

`babyFeedbackName` must be derived the same way as existing carry feedback: custom-named babies use `baby.getDisplayName().getString()`, and unnamed babies use `"baby " + baby.getType().toShortString()`. This keeps reunion messages consistent with the current set-down text without adding client-side localisation requirements.

Create `ParentReunionCooldowns` with `Map<UUID, Long> lastCarrierReunionTick` and `Map<UUID, Long> lastBabyReunionTick`. Cooldown expiry is inclusive: a reunion is allowed when `gameTime >= lastRecordedTick + cooldownTicks`. Implement:

```java
public boolean canReunite(UUID carrierId, UUID babyId, long gameTime, int cooldownTicks)
public void remember(UUID carrierId, UUID babyId, long gameTime)
public void clearCarrier(UUID carrierId)
```

Create `ParentReunionFinder` with:

```java
public Optional<ParentReunionMatch> find(ServerLevel level, Animal baby, Vec3 dropPosition, CarryConfig config)
```

Implementation details:

- Return empty when `!config.parentReunionEnabled()`.
- Clamp radius through config parsing, then build `AABB.ofSize(dropPosition, radius * 2.0D, radius * 2.0D, radius * 2.0D)`.
- Use `level.getEntitiesOfClass(Animal.class, box, candidate -> candidate != baby && candidate.isAlive())`.
- Accept the first candidate where the helper says it is compatible.
- Do not call chunk-loading APIs. The server-level entity query should observe currently loaded entities only.
- Do not move the baby or adult.

Add package-visible static helper:

```java
static boolean compatibleCandidate(
        EntityType<?> babyType,
        boolean babyIsBaby,
        Optional<UUID> babyOwner,
        EntityType<?> adultType,
        boolean candidateIsBaby,
        Optional<UUID> adultOwner,
        double distance,
        int radiusBlocks
)
```

Rules:

- same entity type
- candidate is not a baby
- distance <= radius
- if either side has an owner, both owner UUIDs must be present and equal

- [ ] **Step 4: Run model tests and verify GREEN**

Run:

`.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.reunion.ParentReunionCooldownsTest" --tests "dev.jasmine.carrybabyanimals.reunion.ParentReunionFinderTest"`

Expected: PASS.

## Task 3: Reunion Messages And Feedback Decision

**Files:**
- Create: `src/main/java/dev/jasmine/carrybabyanimals/reunion/ParentReunionMessageCatalog.java`
- Create: `src/main/java/dev/jasmine/carrybabyanimals/reunion/ParentReunionFeedback.java`
- Test: `src/test/java/dev/jasmine/carrybabyanimals/reunion/ParentReunionMessageCatalogTest.java`
- Test: `src/test/java/dev/jasmine/carrybabyanimals/carry/CarryInteractionHandlerTest.java`

- [ ] **Step 1: Write failing message and decision tests**

Add `ParentReunionMessageCatalogTest`:

```java
@Test
void messageIncludesBabyName() {
    ParentReunionMessageCatalog catalog = new ParentReunionMessageCatalog();

    assertTrue(catalog.message("baby Pig", 0).contains("baby Pig"));
}

@Test
void messagesVaryByIndex() {
    ParentReunionMessageCatalog catalog = new ParentReunionMessageCatalog();

    assertNotEquals(catalog.message("baby Pig", 0), catalog.message("baby Pig", 1));
}
```

Add helper-decision tests to `CarryInteractionHandlerTest`:

```java
@Test
void reunionFeedbackRunsOnlyAfterAllowedDropWithMatchAndEnabledConfig() {
    CarryInteractionHandler.ReunionAttemptDecision decision = CarryInteractionHandler.reunionAttemptDecision(
            true,
            true,
            true,
            true,
            true
    );

    assertTrue(decision.shouldSendParticles());
    assertTrue(decision.shouldShowMessage());
    assertTrue(decision.shouldRememberCooldown());
}

@Test
void reunionFeedbackSkipsWhenDropWasRefusedOrNoMatchExists() {
    assertFalse(CarryInteractionHandler.reunionAttemptDecision(false, true, true, true, true).shouldRememberCooldown());
    assertFalse(CarryInteractionHandler.reunionAttemptDecision(true, false, true, true, true).shouldRememberCooldown());
    assertFalse(CarryInteractionHandler.reunionAttemptDecision(true, true, false, true, true).shouldRememberCooldown());
}

@Test
void reunionFeedbackHonorsMessageAndParticleSwitches() {
    CarryInteractionHandler.ReunionAttemptDecision decision = CarryInteractionHandler.reunionAttemptDecision(
            true,
            true,
            true,
            false,
            false
    );

    assertFalse(decision.shouldSendParticles());
    assertFalse(decision.shouldShowMessage());
    assertTrue(decision.shouldRememberCooldown());
}
```

- [ ] **Step 2: Run tests and verify RED**

Run:

`.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.reunion.ParentReunionMessageCatalogTest" --tests "dev.jasmine.carrybabyanimals.carry.CarryInteractionHandlerTest"`

Expected: FAIL because message catalog and `ReunionAttemptDecision` do not exist.

- [ ] **Step 3: Implement messages and helper decision**

Create `ParentReunionMessageCatalog`:

```java
public final class ParentReunionMessageCatalog {
    private static final List<String> MESSAGES = List.of(
            "%s found family nearby.",
            "%s is back with a grown-up friend.",
            "%s has company again."
    );

    public String message(String babyName, int variantIndex) {
        String template = MESSAGES.get(Math.floorMod(variantIndex, MESSAGES.size()));
        return template.formatted(babyName);
    }
}
```

Create `ParentReunionFeedback`:

```java
public void emit(ServerLevel level, ParentReunionMatch match, boolean particlesEnabled)
```

When particles are enabled, call `level.sendParticles(ParticleTypes.HEART, ...)` for the baby and adult using their current server positions and bounding-box heights.

In `CarryInteractionHandler`, add:

```java
static ReunionAttemptDecision reunionAttemptDecision(
        boolean dropSucceeded,
        boolean matchFound,
        boolean cooldownReady,
        boolean messagesEnabled,
        boolean particlesEnabled
)
```

Return no feedback when the drop did not happen, no match exists, or cooldown is not ready. Otherwise remember cooldown even if both message and particle switches are disabled, so repeatedly setting down near parents does not immediately fire when a switch is re-enabled.

- [ ] **Step 4: Run tests and verify GREEN**

Run:

`.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.reunion.ParentReunionMessageCatalogTest" --tests "dev.jasmine.carrybabyanimals.carry.CarryInteractionHandlerTest"`

Expected: PASS.

## Task 4: Deliberate Drop Wiring

**Files:**
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/carry/CarryInteractionHandler.java`
- Test: targeted tests from Tasks 2 and 3

- [ ] **Step 1: Wire services through constructors**

Add fields:

```java
private final ParentReunionFinder parentReunionFinder;
private final ParentReunionCooldowns parentReunionCooldowns;
private final ParentReunionFeedback parentReunionFeedback;
private final ParentReunionMessageCatalog parentReunionMessageCatalog;
```

Default constructors instantiate the new services. The package-visible test constructor accepts the new services.

- [ ] **Step 2: Trigger reunion after a successful deliberate drop**

In `dropCurrentWithFeedback(...)`:

1. Resolve carried baby before dropping.
2. Preview the drop position once.
3. Run Nursery Mode refusal using that preview.
4. If refused, return before any reunion work.
5. Call `dropCurrent(player)`.
6. Show the existing set-down message.
7. Call a private `tryParentReunion(...)` only when the baby is an `Animal`, the player level is a `ServerLevel`, and the baby remains alive after drop.

Pass `baby.position()` read after `dropCurrent(player)` returns as the reunion search position. Do not reuse the Nursery Mode preview vector for the parent search, because the actual safe placement may adjust the final baby position.

`tryParentReunion(...)` should:

- check `config.parentReunionEnabled()`
- find a match in the same `ServerLevel`
- check cooldown using `player.getUUID()`, `baby.getId()`, and `serverLevel.getGameTime()`
- call `reunionAttemptDecision(...)`
- send heart particles if enabled
- show reunion action-bar message if enabled, after the set-down message
- remember cooldown when a match is found and cooldown is ready

Keep cleanup paths through `dropCurrent(...)` and `dropCurrentInLevel(...)` unchanged so logout, growth, death, server stop, and level-change cleanup do not emit reunion feedback.

- [ ] **Step 3: Run targeted tests**

Run:

`.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.carry.CarryInteractionHandlerTest" --tests "dev.jasmine.carrybabyanimals.reunion.*"`

Expected: PASS.

## Task 5: Docs, Manual Tests, And Changelog

**Files:**
- Modify: `README.md`
- Modify: `docs/manual-test-plan.md`
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Update README**

Document:

- Parent Reunion is enabled by default.
- It triggers after a successful deliberate safe set-down near a compatible adult.
- Compatibility is same entity type and adult; tamed animals require matching owner identity.
- It is cosmetic only.
- It uses normal server particles and action-bar text, so vanilla clients remain supported.
- Config fields and defaults:
  - `parentReunionEnabled`: `true`
  - `parentReunionRadiusBlocks`: `8`
  - `parentReunionCooldownTicks`: `200`
  - `parentReunionMessagesEnabled`: `true`
  - `parentReunionParticlesEnabled`: `true`

- [ ] **Step 2: Update manual test plan**

Add rows for:

- successful same-type adult reunion with hearts/message
- no reunion outside radius
- no reunion with different animal type
- no reunion after Nursery Mode refuses unsafe set-down
- tamed animal owner compatibility
- vanilla observer sees normal server particles/text without installing the client mod

- [ ] **Step 3: Update public changelog**

Add an `Unreleased` public bullet:

```markdown
- Added Parent Reunion feedback so safely setting down a carried baby near a matching adult can show cosmetic hearts and a warm action-bar message.
```

- [ ] **Step 4: Run changelog gate**

Run: `.\gradlew.bat checkChangelog`

Expected: PASS.

## Task 6: Self-Review And Verification

**Files:**
- All changed files

- [ ] **Step 1: Self-review against roadmap and this plan**

Confirm:

- only Phase 3 was implemented
- no Phase 4 entity-ID alias expansion was added
- no Phase 5 client polish was added
- no release, push, tag, or publish workflow was started
- Nursery Mode refusal prevents reunion work
- cleanup drops do not emit reunion feedback
- no chunk-loading APIs were added for parent search
- tamed owner compatibility is enforced for reunion matching
- behavior is cosmetic only

- [ ] **Step 2: Run required verification**

Run:

```powershell
.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.reunion.*" --tests "dev.jasmine.carrybabyanimals.config.CarryConfigManagerTest" --tests "dev.jasmine.carrybabyanimals.carry.CarryInteractionHandlerTest"
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat checkChangelog
git diff --check
git status --short
```

Expected:

- all Gradle commands exit 0
- `git diff --check` exits 0
- `git status --short` shows only intended Phase 3 files before commit

- [ ] **Step 3: Commit final implementation**

Run:

```powershell
git add CHANGELOG.md README.md docs/manual-test-plan.md docs/superpowers/plans/2026-05-28-parent-reunion.md src/main/java/dev/jasmine/carrybabyanimals src/test/java/dev/jasmine/carrybabyanimals
git commit -m "feat: add parent reunion feedback"
```

Expected: commit succeeds. Do not push, tag, release, publish, or start Phase 4.

## Plan Self-Review

- Spec coverage: covers Phase 3 matching adult detection, no-adult/no-radius cases, cooldown, message/particle switches, cosmetic-only behavior, Nursery Mode refusal protection, ownership/tamed compatibility, docs, changelog, and verification.
- Placeholder scan: no placeholder implementation steps remain.
- Type consistency: all new classes use `ParentReunion*` naming and live in `dev.jasmine.carrybabyanimals.reunion`.
- Scope check: Phase 4 Expanded Modded Animal Support, Phase 5 Client Polish, and release/publish work are explicitly excluded.
