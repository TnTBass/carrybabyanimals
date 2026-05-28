# Nursery Mode Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Phase 2 Nursery Mode so player-triggered set-downs keep carried babies away from obvious hazards while preserving server-owned gameplay and vanilla-client compatibility.

**Architecture:** Add a focused `nursery` package with a world-backed safety checker, hazard result model, and refusal message catalog. `CarryAttachment` will expose a previewable drop candidate, and `CarryInteractionHandler` will refuse only deliberate player set-downs before restoring AI or clearing carry state. Cleanup drops such as death, logout, growth, and dimension handling keep the existing best-effort safe drop path.

**Tech Stack:** Java 25, Fabric/NeoForm Minecraft 26.1.2 APIs through Loom, JUnit 5, existing Fabric Permissions API optional integration.

---

## File Structure

- Create `src/main/java/dev/jasmine/carrybabyanimals/nursery/NurseryHazard.java`: enum of refusal reasons.
- Create `src/main/java/dev/jasmine/carrybabyanimals/nursery/NurserySafetyDecision.java`: immutable allow/refuse result.
- Create `src/main/java/dev/jasmine/carrybabyanimals/nursery/NurserySafetyChecker.java`: server-side hazard evaluation for destination positions.
- Create `src/main/java/dev/jasmine/carrybabyanimals/nursery/NurseryMessageCatalog.java`: playful action-bar refusal messages.
- Create `src/test/java/dev/jasmine/carrybabyanimals/nursery/NurserySafetyCheckerTest.java`: pure predicate and threshold tests for hazards that can be checked without a full game world.
- Create `src/test/java/dev/jasmine/carrybabyanimals/nursery/NurseryMessageCatalogTest.java`: message variation and naming tests.
- Modify `src/main/java/dev/jasmine/carrybabyanimals/carry/CarryAttachment.java`: expose `chooseDropPosition(...)` package/public enough for preflight safety checks without detaching.
- Modify `src/main/java/dev/jasmine/carrybabyanimals/carry/CarryInteractionHandler.java`: refuse unsafe deliberate drops before `attachment.dropInFront(...)`.
- Modify `src/main/java/dev/jasmine/carrybabyanimals/config/CarryConfig.java` and `CarryConfigManager.java`: add Nursery Mode defaults and parsing.
- Modify `src/main/java/dev/jasmine/carrybabyanimals/permissions/CarryPermissions.java`: add `carrybabyanimals.nursery.bypass` with game-master fallback when Fabric Permissions API is absent.
- Modify `src/test/java/dev/jasmine/carrybabyanimals/config/CarryConfigManagerTest.java`, `src/test/java/dev/jasmine/carrybabyanimals/permissions/CarryPermissionsTest.java`, and `src/test/java/dev/jasmine/carrybabyanimals/carry/CarryInteractionHandlerTest.java`.
- Modify `README.md`, `docs/manual-test-plan.md`, and `CHANGELOG.md`.

## Task 1: Config And Permission Surface

**Files:**
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/config/CarryConfig.java`
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/config/CarryConfigManager.java`
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/permissions/CarryPermissions.java`
- Test: `src/test/java/dev/jasmine/carrybabyanimals/config/CarryConfigManagerTest.java`
- Test: `src/test/java/dev/jasmine/carrybabyanimals/permissions/CarryPermissionsTest.java`

- [ ] **Step 1: Write failing config tests**

Add tests proving default Nursery Mode values are enabled, explicit JSON values are honored, invalid fall thresholds normalize to `4`, and older configs default missing booleans to enabled.

Run: `.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.config.CarryConfigManagerTest"`

Expected: FAIL because `CarryConfig` has no nursery accessors.

- [ ] **Step 2: Write failing permission test**

Add a test for a new package-visible helper such as `CarryPermissions.nurseryBypassFallback(boolean permissionsApiPresent, BooleanSupplier permissionCheck, boolean gameMasterFallback)` so absent Fabric Permissions API uses the game-master fallback and present API uses the provider result.

Run: `.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.permissions.CarryPermissionsTest"`

Expected: FAIL because the helper and permission constant do not exist.

- [ ] **Step 3: Implement config fields**

Add these `CarryConfig` record fields after Phase 1 fields:

```java
boolean nurseryModeEnabled,
boolean nurseryBlockLava,
boolean nurseryBlockFire,
boolean nurseryBlockCactusAndDamage,
boolean nurseryBlockSuffocation,
boolean nurseryBlockDangerousFalls,
int nurseryDangerousFallDistanceBlocks,
boolean nurseryMessagesEnabled
```

Defaults: all booleans `true`, fall distance `4`. Update constructors, `defaultConfig()`, `CarryConfigManager.parse(...)`, `filterAndLogUnknownAnimalNames(...)`, `RawConfig`, and `RawConfig.from(...)`.

- [ ] **Step 4: Implement permission node**

Add `public static final String NURSERY_BYPASS = "carrybabyanimals.nursery.bypass";` and `public static boolean canBypassNursery(ServerPlayer player)`, using `player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)` as the fallback.

- [ ] **Step 5: Verify task**

Run:

```powershell
.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.config.CarryConfigManagerTest" --tests "dev.jasmine.carrybabyanimals.permissions.CarryPermissionsTest"
```

Expected: PASS.

## Task 2: Nursery Hazard Model And Messages

**Files:**
- Create: `src/main/java/dev/jasmine/carrybabyanimals/nursery/NurseryHazard.java`
- Create: `src/main/java/dev/jasmine/carrybabyanimals/nursery/NurserySafetyDecision.java`
- Create: `src/main/java/dev/jasmine/carrybabyanimals/nursery/NurseryMessageCatalog.java`
- Test: `src/test/java/dev/jasmine/carrybabyanimals/nursery/NurseryMessageCatalogTest.java`

- [ ] **Step 1: Write failing message tests**

Test that `message(hazard, "baby Pig", false, 0)` returns a lava/fire/cactus/suffocation/fall-specific playful message, that custom names are preserved, and that variant indexes rotate through at least two different messages for lava.

Run: `.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.nursery.NurseryMessageCatalogTest"`

Expected: FAIL because the package does not exist.

- [ ] **Step 2: Implement models**

`NurseryHazard` values: `LAVA`, `FIRE`, `CACTUS_OR_DAMAGE`, `SUFFOCATION`, `DANGEROUS_FALL`.

`NurserySafetyDecision` exposes `allowed()`, `hazard()`, static `allow()`, and static `refuse(NurseryHazard hazard)`.

- [ ] **Step 3: Implement message catalog**

Use short action-bar strings. Examples:

```text
Not in lava, baby Pig. Nice try, chaos goblin.
baby Pig is staying up here. That drop is a nope.
```

The catalog must be deterministic by variant index, not random, so tests and gameplay stay stable.

- [ ] **Step 4: Verify task**

Run: `.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.nursery.NurseryMessageCatalogTest"`

Expected: PASS.

## Task 3: Safety Checker

**Files:**
- Create: `src/main/java/dev/jasmine/carrybabyanimals/nursery/NurserySafetyChecker.java`
- Test: `src/test/java/dev/jasmine/carrybabyanimals/nursery/NurserySafetyCheckerTest.java`

- [ ] **Step 1: Write failing pure tests**

Test helper-level behavior for config-gated decisions:

- Disabled `nurseryModeEnabled` allows all hazards.
- Disabled hazard switches allow their corresponding hazard.
- Dangerous fall distance at or above threshold refuses.
- Dangerous fall distance below threshold allows.
- Bypass allows even when the checker would otherwise refuse.

Run: `.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.nursery.NurserySafetyCheckerTest"`

Expected: FAIL because the checker does not exist.

- [ ] **Step 2: Write failing world-backed or mock-level hazard tests**

Add tests that exercise the same `evaluate(...)` path used by gameplay, not only pure helper methods. Use the lightest repo-compatible level test double available; if Minecraft `Level` is too heavy to instantiate directly, split block/fluid/collision access behind a package-private `NurserySafetyChecker.WorldAccess` adapter and test the adapter-facing scanner with a fake world.

Required test cases:

- Lava refuses when the baby bounds intersect lava fluid.
- Lava refuses when lava is adjacent to the candidate feet.
- Fire refuses for fire, campfire, soul campfire, and magma block states.
- Cactus/damage refuses for cactus, sweet berry bush, pointed dripstone, wither rose, and powder snow.
- Suffocation refuses when collision at the candidate bounds fails.
- Dangerous fall refuses only when the downward open distance reaches `nurseryDangerousFallDistanceBlocks`.
- Safe flat ground with collision and a nearby floor allows.

Run: `.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.nursery.NurserySafetyCheckerTest"`

Expected: FAIL because the real hazard scanner does not exist yet.

- [ ] **Step 3: Implement checker API**

Expose:

```java
public NurserySafetyDecision evaluate(Level level, Entity baby, Vec3 bottomCenter, CarryConfig config, boolean bypass)
```

If bypass or `!config.nurseryModeEnabled()`, return allow. Otherwise, evaluate enabled hazard families in this order: lava, fire, cactus/damage, suffocation, dangerous fall.

- [ ] **Step 4: Implement hazard detection**

Use the baby bounding box at the candidate bottom center.

- Lava: refuse if intersecting lava fluid, lava block, or a neighboring lava fluid/block around the candidate feet when `nurseryBlockLava` is enabled.
- Fire: refuse fire blocks, campfires, soul campfires, magma blocks, and other blocks tagged/recognized by Minecraft as burning damage where available when `nurseryBlockFire` is enabled.
- Cactus/damage: refuse cactus, berry bushes, pointed dripstone, sweet berry bush, wither rose, powder snow, and block states exposing obvious entity damage behavior through known vanilla classes when `nurseryBlockCactusAndDamage` is enabled.
- Suffocation: refuse if `level.noCollision(baby, bounds)` is false or if head/feet blocks are solid enough to suffocate when `nurseryBlockSuffocation` is enabled.
- Dangerous fall: scan downward from the candidate feet until a safe standing block, fluid, world bottom, or `nurseryDangerousFallDistanceBlocks` is reached. Refuse when the open drop is at least the configured threshold and no safe floor exists sooner.

- [ ] **Step 5: Verify task**

Run: `.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.nursery.NurserySafetyCheckerTest"`

Expected: PASS.

## Task 4: Drop Refusal Integration

**Files:**
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/carry/CarryAttachment.java`
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/carry/CarryInteractionHandler.java`
- Test: `src/test/java/dev/jasmine/carrybabyanimals/carry/CarryInteractionHandlerTest.java`

- [ ] **Step 1: Write failing handler tests**

Add tests around a package-visible pure decision helper such as:

```java
static DropAttemptDecision dropAttemptDecision(
        boolean currentlyCarrying,
        NurserySafetyDecision safetyDecision,
        boolean messagesEnabled
)
```

Expected behavior: unsafe deliberate drop is refused and keeps carrying; safe drop proceeds; disabled messages still refuse without action-bar text.

Run: `.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.carry.CarryInteractionHandlerTest"`

Expected: FAIL because the helper does not exist.

- [ ] **Step 2: Write failing cleanup-bypass regression test**

Add a test proving non-player cleanup paths are not blocked by Nursery Mode. Use a recording attachment or package-visible handler seam to call `dropCurrent(...)` or `dropCurrentInLevel(...)` while a safety checker would refuse the previewed position, then assert the cleanup path still drops, ends carry state, clears cozy feedback state, and sends the existing clear-carried packet behavior.

Run: `.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.carry.CarryInteractionHandlerTest"`

Expected: FAIL until deliberate-drop refusal is separated from cleanup drops.

- [ ] **Step 3: Expose drop preview**

Change `CarryAttachment.chooseDropPosition(...)` from private to package-visible or public as `previewDropPosition(ServerPlayer carrier, Entity baby)`. `dropInFront(...)` should use the same method so preview and actual placement cannot drift.

- [ ] **Step 4: Wire Nursery Mode into deliberate drops**

In `dropCurrentWithFeedback(...)`, before restoring AI or calling `dropCurrent(...)`, find the carried baby, preview the drop position, call `NurserySafetyChecker.evaluate(...)`, and if refused:

- leave the baby riding the player;
- leave `CarryManager` state intact;
- do not restore AI;
- do not send clear-carried packets;
- send a `NurseryMessageCatalog` action-bar message only when `config.nurseryMessagesEnabled()` is true.

Automatic cleanup paths through `dropCurrent(...)` and `dropCurrentInLevel(...)` keep existing behavior.

- [ ] **Step 5: Verify task**

Run: `.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.carry.CarryInteractionHandlerTest"`

Expected: PASS.

## Task 5: Documentation And Changelog

**Files:**
- Modify: `README.md`
- Modify: `docs/manual-test-plan.md`
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Update public docs**

Document Nursery Mode behavior, config fields and defaults, bypass permission, vanilla-client compatibility, and manual tests for each hazard class.

- [ ] **Step 2: Update public changelog**

Add an `Unreleased` bullet because Nursery Mode is player/server-admin visible.

- [ ] **Step 3: Run doc/changelog checks**

Run: `.\gradlew.bat checkChangelog`

Expected: PASS.

## Task 6: Final Verification

**Files:** all Phase 2 files.

- [ ] **Step 1: Run targeted Phase 2 tests**

Run:

```powershell
.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.nursery.*" --tests "dev.jasmine.carrybabyanimals.config.CarryConfigManagerTest" --tests "dev.jasmine.carrybabyanimals.permissions.CarryPermissionsTest" --tests "dev.jasmine.carrybabyanimals.carry.CarryInteractionHandlerTest"
```

Expected: PASS.

- [ ] **Step 2: Run full verification**

Run:

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat checkChangelog
git diff --check
git status --short
```

Expected: tests/build/checkChangelog/diff-check PASS; status shows only intentional Phase 2 files until commit.

- [ ] **Step 3: Commit**

Commit with message:

```text
feat: add nursery mode safety checks
```

Do not release, push, tag, publish, or start Phase 3.

## Self-Review

- Spec coverage: covers every Phase 2 requirement from the roadmap: hazard refusal, keeping baby carried, playful messages, config switches/defaults, bypass permission, docs, changelog, and vanilla-client/server-owned behavior.
- Scope: excludes Parent Reunion, Expanded Modded Animal Support, Client Polish, release, push, tag, and publishing.
- Test strategy: config and permission tests are pure; catalog tests are deterministic; handler tests verify refusal state without requiring a full server; safety checker has focused helper tests plus compile-time coverage against real Minecraft APIs.
