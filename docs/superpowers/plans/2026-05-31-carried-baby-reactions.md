# Carried Baby Reactions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend Phase 5 Client Polish with Creature Personality Polish by making large carried babies readable without blocking first-person play, then layering optional client-only creature reactions on the modded-client render path.

**Architecture:** Keep the first implementation slice client-render-only: size classification, tucked placement, first-person visibility rules, reaction selection, sleepy visual variants, and client visual config all live in client-side render/config helpers. The server remains authoritative for carry state, permissions, config, petting, sleepy feedback, reunion behavior, animal state, and payload delivery. This phase adds no new payload; a future timing-hint phase would need a separate plan and capability gating with `ServerPlayNetworking.canSend(...)`.

**Tech Stack:** Java 25, Fabric 26.1.2, existing Fabric client render hooks, vanilla Minecraft entity renderers, JUnit 5, Gson-style client config parsing.

---

## Compatibility And Scope Guardrails

- Preserve vanilla-client compatibility. Vanilla clients continue to see real passenger entities and vanilla-safe server effects.
- Babies remain real entities with the passenger fallback. Do not convert carried babies into items, fake client entities, or server-owned animation props.
- Keep server gameplay authoritative. Do not let client render state decide pickup eligibility, set-down safety, parent reunion behavior, permissions, age, breeding, taming, ownership, sleeping, sitting, panic, trust, health, or AI.
- Keep modded-client polish optional and cosmetic.
- Add no new clientbound payloads in this implementation. A worker that finds a specific event-timing gap must stop and write a follow-up plan for compact optional event hints gated by `ServerPlayNetworking.canSend(...)`.
- Do not add GeckoLib, custom animation libraries, new permissions, inventory storage, multiplayer synchronization for cosmetic reactions, or renderer support for arbitrary third-party modded entity models beyond safe fallback placement.

## File Structure

- Create: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabySizeBucket.java`
  - Defines `SMALL`, `MEDIUM`, `TALL`, and `BULKY`.
- Create: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabySizeClassifier.java`
  - Classifies carried babies from entity type ID, baby hitbox height, and width, with explicit vanilla overrides for horse, camel, llama, panda, turtle, chicken, rabbit, and fox.
- Modify: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyPlacement.java`
  - Adds size-bucket-aware placement, tucked-side offsets, and local-player first-person visibility placement while keeping the existing overloads for Phase 5 callers until renderer wiring is updated.
- Create: `src/client/java/dev/jasmine/carrybabyanimals/client/render/FirstPersonLargeBabyVisibilityMode.java`
  - Defines `TUCKED`, `LOWERED`, and `HIDE_WHEN_OBSTRUCTING` client visibility behavior.
- Create: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyReactionType.java`
  - Defines `GENERIC_SETTLE`, `CHICKEN_FLAP`, `RABBIT_WIGGLE`, `FOX_CURL`, `PANDA_SNEEZE`, and `TURTLE_HIDE`.
- Create: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyReaction.java`
  - Immutable reaction descriptor with type, duration ticks, amplitude, side offset, vertical offset, pitch/yaw/roll offsets, and sleepy eligibility.
- Create: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyReactionRegistry.java`
  - Maps entity type IDs or vanilla animal families to reaction descriptors, with a generic fallback for every unsupported entity.
- Create: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyVisualFrame.java`
  - Combines placement and reaction offsets into one render-only frame consumed by `CarriedBabyRenderer`.
- Create: `src/client/java/dev/jasmine/carrybabyanimals/client/config/ClientCarryVisualConfig.java`
  - Client-only record for `carriedBabyReactionsEnabled`, `largeBabyTuckedPoseEnabled`, `firstPersonLargeBabyVisibilityMode`, `sleepyCarryVisualsEnabled`, `animalReactionIntensity`, and `disabledCarriedReactionAnimals`.
- Create: `src/client/java/dev/jasmine/carrybabyanimals/client/config/ClientCarryVisualConfigManager.java`
  - Loads/saves `config/carrybabyanimals-client.json` through Fabric's client-safe config directory and exposes defaults plus parsing helpers without touching server config.
- Modify: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyRenderer.java`
  - Classifies carried baby size, applies first-person large-baby placement before any personality reaction, selects the reaction frame, and keeps vanilla render suppression behavior unchanged.
- Modify: `src/client/java/dev/jasmine/carrybabyanimals/client/CarryBabyAnimalsClient.java`
  - Initializes client visual config defaults if file-backed client config is implemented.
- Modify: `src/client/java/dev/jasmine/carrybabyanimals/client/ClientCarryInteractionHandler.java`
  - Reuses existing `PetFeedbackPayload` client receipt to trigger local reaction timing only when the client already knows the baby is carried by the local player; do not send any new payload.
- Test: `src/test/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabySizeClassifierTest.java`
  - Covers small, medium, tall, bulky, and explicit vanilla overrides.
- Modify/Test: `src/test/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyPlacementTest.java`
  - Adds tucked-side placement and first-person visibility tests.
- Test: `src/test/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyReactionRegistryTest.java`
  - Covers initial reaction candidates, generic fallback, disabled animal IDs, and intensity scaling.
- Test: `src/test/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyVisualFrameTest.java`
  - Covers reaction frame duration, clamped offsets, sleepy visual handling, non-stacking, and carry-end reset behavior.
- Test: `src/test/java/dev/jasmine/carrybabyanimals/client/config/ClientCarryVisualConfigManagerTest.java`
  - Covers default values, parsing, invalid visibility mode fallback, intensity clamping, and disabled animal normalization.
- Modify: `README.md`
  - Documents client-only cosmetic config and vanilla-client fallback after implementation, because this is player/admin-visible optional behavior.
- Modify: `CHANGELOG.md`
  - Adds a public Unreleased note only when implementation changes player-visible client polish behavior.

## Task 1: Size Bucket Classification

**Files:**
- Create: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabySizeBucket.java`
- Create: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabySizeClassifier.java`
- Test: `src/test/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabySizeClassifierTest.java`

- [ ] **Step 1: Write failing size bucket tests**

Create `CarriedBabySizeClassifierTest` with tests named:

```java
classifiesSmallMediumTallAndBulkyByDimensions()
classifiesHorseCamelAndLlamaAsTallOverrides()
classifiesPandaAsBulkyOverride()
classifiesChickenRabbitFoxAndTurtleAsSafeSpecificOverrides()
fallsBackToDimensionThresholdsForUnknownEntityIds()
```

Use string entity IDs and dimensions so tests do not require full Minecraft entity bootstrap. Expected initial result: compile failure because `CarriedBabySizeBucket` and `CarriedBabySizeClassifier` do not exist.

- [ ] **Step 2: Run the red classifier test**

Run: `.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.render.CarriedBabySizeClassifierTest`

Expected: FAIL with missing class errors.

- [ ] **Step 3: Add the bucket enum**

Create `CarriedBabySizeBucket` with:

```java
public enum CarriedBabySizeBucket {
    SMALL,
    MEDIUM,
    TALL,
    BULKY
}
```

- [ ] **Step 4: Add the classifier**

Create `CarriedBabySizeClassifier` with:

```java
static CarriedBabySizeBucket classify(String entityTypeId, double babyHeight, double babyWidth)
```

Rules:
- `minecraft:horse`, `minecraft:camel`, `minecraft:llama`, `minecraft:trader_llama`, `minecraft:donkey`, and `minecraft:mule` return `TALL`.
- `minecraft:panda` returns `BULKY`.
- `minecraft:turtle` returns `BULKY` only when width is at least `0.85D`; otherwise `MEDIUM`.
- `minecraft:chicken` and `minecraft:rabbit` return `SMALL`.
- `minecraft:fox` returns `MEDIUM`.
- Unknown IDs use dimensions: height `>= 1.05D` returns `TALL`; width `>= 0.9D` returns `BULKY`; height `<= 0.55D` and width `<= 0.55D` returns `SMALL`; otherwise `MEDIUM`.

- [ ] **Step 5: Run the classifier test green**

Run: `.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.render.CarriedBabySizeClassifierTest`

Expected: PASS.

## Task 2: Large Baby Tucked-Side Placement Before Reactions

**Files:**
- Modify: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyPlacement.java`
- Create: `src/client/java/dev/jasmine/carrybabyanimals/client/render/FirstPersonLargeBabyVisibilityMode.java`
- Modify: `src/test/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyPlacementTest.java`

- [ ] **Step 1: Write failing placement tests**

Add tests named:

```java
tallBabiesUseLowerTuckedSidePlacement()
bulkyBabiesStayBesideCarrierInsteadOfCenteredForward()
firstPersonTallBabyKeepsCrosshairCorridorClear()
hideWhenObstructingModeReturnsSuppressedFrameForTallLocalPlayerCarry()
existingMediumPlacementRemainsNearPhaseFiveBaseline()
```

The tests should compare vectors and a suppression boolean returned by a new placement method. Expected initial result: compile failure because the first-person mode and new placement result do not exist.

- [ ] **Step 2: Run the red placement test**

Run: `.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.render.CarriedBabyPlacementTest`

Expected: FAIL with missing method/type errors.

- [ ] **Step 3: Add first-person mode enum**

Create `FirstPersonLargeBabyVisibilityMode`:

```java
public enum FirstPersonLargeBabyVisibilityMode {
    TUCKED,
    LOWERED,
    HIDE_WHEN_OBSTRUCTING
}
```

- [ ] **Step 4: Add placement result and size-aware placement method**

Add a nested or top-level immutable result type with:

```java
Vec3 position()
boolean suppressForLocalFirstPerson()
double yawDegrees()
double pitchDegrees()
double rollDegrees()
```

Add a method that accepts carrier position, normalized horizontal forward, carrier height, baby height, baby width, arm side, animation ticks, size bucket, first-person flag, and visibility mode. Rules:
- Run this placement before any personality reaction.
- `SMALL` and `MEDIUM` preserve the Phase 5 forward/arm placement within `0.06D` on X/Z and `0.08D` on Y.
- `TALL` lowers the baby at least `0.22D` compared with medium placement, moves it at least `0.18D` toward the arm side, moves it at least `0.08D` backward toward the carrier side, and rotates yaw toward the carrier by `15.0D` to `35.0D`.
- `BULKY` moves the baby at least `0.20D` toward the arm side, limits forward distance to `<= 0.36D`, and uses lower vertical placement than medium.
- First-person `TUCKED` uses the tall/bulky side placement.
- First-person `LOWERED` applies an additional `0.18D` downward offset for `TALL` and `BULKY`.
- First-person `HIDE_WHEN_OBSTRUCTING` returns `suppressForLocalFirstPerson = true` only for `TALL` or `BULKY`; it must not clear render state or affect other players.

- [ ] **Step 5: Run placement tests green**

Run: `.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.render.CarriedBabyPlacementTest`

Expected: PASS.

## Task 3: First-Person Visibility Wiring And Manual Acceptance

**Files:**
- Modify: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyRenderer.java`
- Modify: `src/test/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyPlacementTest.java`
- Create or update: `docs/manual-test-plan.md`

- [ ] **Step 1: Add renderer-adjacent placement assertions**

Keep JVM tests focused on pure placement math. Add assertions that tall first-person placement leaves a center corridor clear by keeping the baby position outside a narrow forward-center region:

```java
assertTrue(Math.abs(localFirstPerson.position().x) >= 0.24D);
assertTrue(localFirstPerson.position().y <= mediumBaseline.position().y - 0.18D);
```

- [ ] **Step 2: Wire renderer after placement tests pass**

In `CarriedBabyRenderer`, classify each carried baby from `baby.getType()` ID, `baby.getBbHeight()`, and `baby.getBbWidth()`. Detect local-player first-person render only for the local carrier and apply first-person placement before reaction frames. Never write the placement decision back into `CarriedBabyRenderState`.

- [ ] **Step 3: Preserve vanilla passenger fallback**

Keep the existing `CarriedBabyRenderState.SUPPRESS_VANILLA_RENDER` behavior limited to modded-client render suppression. Do not change server attachment, passenger logic, or `CarryNetworking.sendIfSupported(...)`.

- [ ] **Step 4: Document first-person manual acceptance**

Add manual acceptance to `docs/manual-test-plan.md`:

```markdown
### Phase 5 Extension: Large Baby First-Person Visibility

- Start a dedicated or integrated test world with CarryBabyAnimals installed on server and client.
- Carry a baby horse at default FOV on a 16:9 display.
- Switch to first person.
- Verify the crosshair and horizon line remain unobstructed.
- Verify the carried baby is limited to the lower-left or lower-right quadrant depending on arm side.
- Repeat with a baby camel, then repeat with a baby llama.
- Toggle `firstPersonLargeBabyVisibilityMode` through `TUCKED`, `LOWERED`, and `HIDE_WHEN_OBSTRUCTING`.
- Verify `HIDE_WHEN_OBSTRUCTING` hides only the local first-person carried render and does not drop the real baby or affect third-person/other-player views.
```

- [ ] **Step 5: Run focused render math tests**

Run: `.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.render.CarriedBabyPlacementTest --tests dev.jasmine.carrybabyanimals.client.render.CarriedBabySizeClassifierTest`

Expected: PASS.

## Task 4: Client Visual Config Shape

**Files:**
- Create: `src/client/java/dev/jasmine/carrybabyanimals/client/config/ClientCarryVisualConfig.java`
- Create: `src/client/java/dev/jasmine/carrybabyanimals/client/config/ClientCarryVisualConfigManager.java`
- Test: `src/test/java/dev/jasmine/carrybabyanimals/client/config/ClientCarryVisualConfigManagerTest.java`
- Modify: `src/client/java/dev/jasmine/carrybabyanimals/client/CarryBabyAnimalsClient.java`

- [ ] **Step 1: Write failing client config tests**

Create tests named:

```java
defaultsEnableConservativeClientVisualPolish()
parsesDisabledReactionsAndLoweredVisibilityMode()
invalidVisibilityModeFallsBackToTucked()
intensityIsClampedToConservativeRange()
disabledAnimalIdsAreTrimmedAndLowercased()
```

Expected initial result: compile failure because client config classes do not exist.

- [ ] **Step 2: Run the red client config test**

Run: `.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.config.ClientCarryVisualConfigManagerTest`

Expected: FAIL with missing class errors.

- [ ] **Step 3: Add client config record**

Create `ClientCarryVisualConfig` with defaults:
- `carriedBabyReactionsEnabled = true`
- `largeBabyTuckedPoseEnabled = true`
- `firstPersonLargeBabyVisibilityMode = FirstPersonLargeBabyVisibilityMode.TUCKED`
- `sleepyCarryVisualsEnabled = true`
- `animalReactionIntensity = 0.75D`
- `disabledCarriedReactionAnimals = List.of()`

Clamp intensity into `0.0D` through `1.0D`.

- [ ] **Step 4: Add manager parse/default behavior**

Implement parse helpers with Gson-style JSON matching the server config style. Add file IO for `config/carrybabyanimals-client.json` through Fabric's client-safe config directory. The file must be a separate client visual config and must not alter `CarryConfig`, `CarryConfigManager`, or server config defaults.

- [ ] **Step 5: Wire client initialization**

Initialize the client visual config in `CarryBabyAnimalsClient.onInitializeClient()`. The client config must never be read from server gameplay code.

- [ ] **Step 6: Run client config tests green**

Run: `.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.config.ClientCarryVisualConfigManagerTest`

Expected: PASS.

## Task 5: Reaction Registry And Initial Reaction Candidates

**Files:**
- Create: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyReactionType.java`
- Create: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyReaction.java`
- Create: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyReactionRegistry.java`
- Test: `src/test/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyReactionRegistryTest.java`

- [ ] **Step 1: Write failing registry tests**

Create tests named:

```java
returnsChickenFlapForChicken()
returnsRabbitWiggleForRabbit()
returnsFoxCurlForFox()
returnsPandaSneezeForPanda()
returnsTurtleHideForTurtle()
returnsGenericFallbackForUnsupportedEntity()
disabledAnimalUsesGenericFallback()
reactionIntensityScalesAmplitudeWithoutChangingDuration()
```

Expected initial result: compile failure because registry classes do not exist.

- [ ] **Step 2: Run the red registry test**

Run: `.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.render.CarriedBabyReactionRegistryTest`

Expected: FAIL with missing class errors.

- [ ] **Step 3: Add reaction type enum**

Create `CarriedBabyReactionType` with the six named values in the file structure section.

- [ ] **Step 4: Add reaction descriptor**

Create `CarriedBabyReaction` with safe bounds:
- Duration between `8` and `40` ticks.
- Vertical offset clamped to `[-0.08D, 0.08D]`.
- Side offset clamped to `[-0.08D, 0.08D]`.
- Pitch/yaw/roll clamped to `[-18.0D, 18.0D]`.
- `sleepyEligible` boolean for reactions that can be stilled or softened during sleepy visuals.

- [ ] **Step 5: Add registry defaults**

Map:
- `minecraft:chicken` -> `CHICKEN_FLAP`, duration `12`, low roll/pitch emphasis, amplitude `0.65D`.
- `minecraft:rabbit` -> `RABBIT_WIGGLE`, duration `10`, small vertical/side wiggle, amplitude `0.5D`.
- `minecraft:fox` -> `FOX_CURL`, duration `28`, inward side offset and slight yaw, amplitude `0.45D`.
- `minecraft:panda` -> `PANDA_SNEEZE`, duration `14`, rare head/body bob descriptor, amplitude `0.35D`.
- `minecraft:turtle` -> `TURTLE_HIDE`, duration `24`, lower/head-tucked style offset, amplitude `0.4D`.
- Fallback -> `GENERIC_SETTLE`, duration `32`, very small settle/breathing offset, amplitude `0.25D`.

The registry must return fallback when reactions are disabled globally or the entity ID appears in `disabledCarriedReactionAnimals`.

- [ ] **Step 6: Run registry tests green**

Run: `.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.render.CarriedBabyReactionRegistryTest`

Expected: PASS.

## Task 6: Reaction Frame Evaluation And Sleepy Visual Handling

**Files:**
- Create: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyVisualFrame.java`
- Modify: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyRenderState.java`
- Modify: `src/client/java/dev/jasmine/carrybabyanimals/client/ClientCarryInteractionHandler.java`
- Test: `src/test/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyVisualFrameTest.java`
- Modify: `src/test/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyRenderStateTest.java`

- [ ] **Step 1: Write failing frame tests**

Create tests named:

```java
reactionFrameNeverExceedsDescriptorBounds()
petFeedbackStartsShortLocalReactionForKnownCarriedBaby()
reactionStopsWhenCarryStateClears()
sleepyVisualSoftensEligibleReactionWithoutChangingGameplayState()
sleepyVisualFallsBackToStillnessWhenRendererSpecificPoseIsUnsafe()
multipleReactionTriggersDoNotStackAmplitude()
```

Expected initial result: compile failure because frame evaluation and reaction timers do not exist.

- [ ] **Step 2: Run the red frame test**

Run: `.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.render.CarriedBabyVisualFrameTest --tests dev.jasmine.carrybabyanimals.client.render.CarriedBabyRenderStateTest`

Expected: FAIL with missing methods/classes.

- [ ] **Step 3: Add render-only reaction timing to render state**

Extend `CarriedBabyRenderState` with client-only local timing data keyed by baby entity ID. Store only cosmetic state such as last local reaction tick, active reaction type, and local sleepy visual window. Do not send these values to the server and do not use them for carry eligibility or cleanup beyond clearing them when the baby carry pair is cleared.

- [ ] **Step 4: Reuse existing pet feedback payload as a local trigger**

In `ClientCarryInteractionHandler.onPetFeedback`, trigger a local cosmetic reaction only if the baby ID is known in `CarriedBabyRenderState` and `carriedBabyReactionsEnabled` is true. Do not add a new payload or change server petting behavior.

- [ ] **Step 5: Add visual frame evaluator**

`CarriedBabyVisualFrame` should combine placement output and reaction descriptor output. Rules:
- Large-baby placement remains the base frame and cannot be overridden by reaction offsets.
- Reaction offsets are multiplied by clamped intensity.
- Reaction offsets do not stack; a new trigger restarts or replaces the active reaction.
- Carry clear removes reaction state for that baby.
- Sleepy visuals are render-only: stillness, reduced amplitude, small lower-head/body offset, or slow breathing. They must not call sleeping, sitting, breeding, taming, panic, trust, age, ownership, or AI APIs.

- [ ] **Step 6: Run frame tests green**

Run: `.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.render.CarriedBabyVisualFrameTest --tests dev.jasmine.carrybabyanimals.client.render.CarriedBabyRenderStateTest`

Expected: PASS.

## Task 7: Renderer Integration After Visibility And Registry Tests

**Files:**
- Modify: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyRenderer.java`
- Modify: `src/client/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyRenderState.java`
- Modify: `src/client/java/dev/jasmine/carrybabyanimals/client/CarryBabyAnimalsClient.java`
- Modify: `src/client/java/dev/jasmine/carrybabyanimals/client/ClientCarryInteractionHandler.java`

- [ ] **Step 1: Confirm prerequisite tests are green**

Run:

```powershell
.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.render.CarriedBabySizeClassifierTest --tests dev.jasmine.carrybabyanimals.client.render.CarriedBabyPlacementTest --tests dev.jasmine.carrybabyanimals.client.render.CarriedBabyReactionRegistryTest --tests dev.jasmine.carrybabyanimals.client.render.CarriedBabyVisualFrameTest --tests dev.jasmine.carrybabyanimals.client.config.ClientCarryVisualConfigManagerTest
```

Expected: PASS.

- [ ] **Step 2: Integrate visual frame into render submit**

In `CarriedBabyRenderer.collectSubmits`, compute the base placement from size bucket and first-person mode, then compute the reaction frame. Apply render-state `x`, `y`, `z`, optional yaw/pitch/roll fields only through vanilla-safe render state mutations already supported by the extracted entity render state. For entity render states without safe pose controls, use only position offsets and fallback reactions.

- [ ] **Step 3: Keep suppression local and cosmetic**

When first-person `HIDE_WHEN_OBSTRUCTING` suppresses a local carried render, skip only the extra modded-client carried render for that frame. Do not call `CarriedBabyRenderState.clear`, do not send packets, and do not affect other players' third-person view.

- [ ] **Step 4: Run focused client render tests**

Run: `.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.render.* --tests dev.jasmine.carrybabyanimals.client.config.*`

Expected: PASS.

## Task 8: Vanilla-Client Fallback And Payload Guard Verification

**Files:**
- Do not modify: `src/main/java/dev/jasmine/carrybabyanimals/network/CarryNetworking.java`
- Inspect: `src/main/java/dev/jasmine/carrybabyanimals/network/CarryNetworking.java`

- [ ] **Step 1: Inspect payload changes**

Run: `git diff -- src/main/java/dev/jasmine/carrybabyanimals/network/CarryNetworking.java src/client/java/dev/jasmine/carrybabyanimals/client src/client/java/dev/jasmine/carrybabyanimals/client/render`

Expected: No new clientbound payloads for this Phase 5 extension. Existing server sends still use `sendIfSupported`, which checks `ServerPlayNetworking.canSend(player, payload.type())`.

- [ ] **Step 2: Verify no new gameplay permissions**

Run: `rg -n "carrybabyanimals\\.|Permissions|permission" src/main/java src/client/java`

Expected: No new permission node for this Phase 5 extension. Existing carry and nursery permissions remain unchanged.

- [ ] **Step 3: Verify no client render state feeds server gameplay**

Run: `rg -n "CarriedBabyRenderState|CarriedBabyVisual|CarriedBabyReaction|ClientCarryVisual" src/main/java`

Expected: No matches outside existing client-only registration or shared payload types. Server gameplay must not import client render classes.

- [ ] **Step 4: Manual vanilla-client fallback check**

Add to `docs/manual-test-plan.md`:

```markdown
### Phase 5 Extension: Vanilla-Client Fallback

- Start a server with CarryBabyAnimals installed.
- Join with a vanilla-compatible client profile that does not install the CarryBabyAnimals client mod.
- Carry a baby cow and a baby horse.
- Verify the baby remains a real passenger entity and no custom client payload is required.
- Verify pickup, petting, set-down, Nursery Mode, and Parent Reunion behavior remain server-owned.
```

## Task 9: Documentation And Changelog

**Files:**
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Modify: `INTERNAL_CHANGELOG.md` only for maintainer workflow notes if the implementation worker adds internal-only artifacts

- [ ] **Step 1: Update public docs**

Document:
- Client-side visual config names and defaults.
- `firstPersonLargeBabyVisibilityMode` values.
- `disabledCarriedReactionAnimals` examples.
- Vanilla-client fallback: vanilla clients keep seeing real passenger entities and server-owned sounds/particles/messages.
- No new permissions.

- [ ] **Step 2: Add public changelog entry**

Add a public Unreleased note such as:

```markdown
- Added optional modded-client creature personality polish for carried babies, including safer large-baby first-person placement and gentle animal-specific carried reactions while preserving vanilla-client passenger fallback.
```

- [ ] **Step 3: Run changelog gate**

Run: `.\gradlew.bat checkChangelog`

Expected: PASS.

## Task 10: Final Focused Verification

**Files:**
- All changed implementation, test, docs, and changelog files from this plan

- [ ] **Step 1: Run whitespace diff check**

Run: `git diff --check`

Expected: no output and exit code `0`.

- [ ] **Step 2: Run focused Phase 5 extension tests**

Run:

```powershell
.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.render.* --tests dev.jasmine.carrybabyanimals.client.config.*
```

Expected: PASS.

- [ ] **Step 3: Run changelog gate**

Run: `.\gradlew.bat checkChangelog`

Expected: PASS.

- [ ] **Step 4: Run full build when focused tests are green**

Run: `.\gradlew.bat build`

Expected: PASS.

- [ ] **Step 5: Inspect final diff**

Run: `git status --short` and `git diff --stat`

Expected: changes are limited to Phase 5 extension client render/config implementation, focused tests, docs, and changelog entries.

## Manual Acceptance Checklist

- [ ] Baby horse first-person carry at default FOV on 16:9 leaves crosshair and horizon unobstructed.
- [ ] Tall baby is visible only in the lower-left or lower-right quadrant depending on held side.
- [ ] Baby camel or llama uses tucked-side placement and does not fill the camera.
- [ ] Third-person tall/bulky placement reads as under-arm or tucked beside the carrier's ribs.
- [ ] Chicken, rabbit, fox, panda, and turtle either show their named reaction or fall back to generic settle without obvious clipping.
- [ ] Generic fallback renders safely for an unsupported carried baby.
- [ ] Sleepy carried baby visuals, when enabled, are render-only stillness/softening and do not change actual sleeping or AI state.
- [ ] Disabling `carriedBabyReactionsEnabled` keeps large-baby placement but removes personality reactions.
- [ ] Disabling `largeBabyTuckedPoseEnabled` returns to conservative Phase 5 placement.
- [ ] A vanilla client can join and carry babies using the passenger fallback without requiring the client mod.

## Explicit Out Of Scope

- No new permissions.
- No new required client dependency.
- No GeckoLib or custom animation library.
- No gameplay effects from reactions.
- No changes to actual animal AI, age, breeding, taming, ownership, sleeping, sitting, panic, trust, health, or movement state.
- No server gameplay dependency on client render state, local animation timers, or first-person visibility mode.
- No required custom payload for the first Phase 5 extension implementation.
- No automatic support for arbitrary third-party modded animal renderers beyond safe generic fallback placement.
- No release push, tag, marketplace publishing, or server deployment as part of this plan.
