# Expanded Modded Animal Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let server owners opt into additional baby animal entity types from other mods by listing full entity IDs in the existing `allowedAnimals` and `blockedAnimals` config lists.

**Architecture:** Keep Phase 4 server-owned and config-only by extending the existing animal resolver instead of adding new gameplay systems, packets, rendering, or permissions. `AnimalAliasRegistry` remains the single place that turns config entries into `Identifier` values, while `CarryEligibility` continues to enforce baby-animal checks, default vanilla support, block precedence, allow-list restriction, and tamed permission rules. The first Phase 4 pass does not add admin-defined alias maps because full entity IDs meet the opt-in goal with less config surface and fewer ambiguous ownership rules.

**Tech Stack:** Java 25, Fabric 26.1.2, Minecraft entity registry IDs, Gson config parsing, SLF4J logging, JUnit 5, Gradle.

---

## Scope Boundaries

Implement only Phase 4 Expanded Modded Animal Support.

Do not implement:

- Phase 5 Client Polish
- release, push, tag, or publish workflow
- new gameplay effects
- new carried rendering
- lineage tracking
- parent-reunion variant matching
- custom compatibility claims for other mods' entities

Phase 4 behavior:

- `allowedAnimals` and `blockedAnimals` accept existing friendly vanilla aliases and full entity IDs such as `examplemod:duck`.
- Friendly aliases like `cow`, `cat`, `dog`, and `wolf` keep their current behavior.
- No modded entity is allowed by default. A modded entity is allowed only when its full ID appears in `allowedAnimals`.
- `blockedAnimals` wins over `allowedAnimals` for both aliases and IDs.
- Unknown aliases and malformed IDs are logged clearly and ignored.
- Unknown-only allow lists keep `restrictToAllowedAnimals=true`, so they deny pickup instead of falling back to every default animal.
- Existing carry permissions continue to govern all allowed entities.
- CarryBabyAnimals only allows carrying an entity that already exists on the server; it does not decide whether another mod requires matching client mods.

## File Structure

- Modify `src/main/java/dev/jasmine/carrybabyanimals/config/AnimalAliasRegistry.java`: resolve full entity IDs in addition to built-in aliases, while preserving alias order for config comments.
- Modify `src/test/java/dev/jasmine/carrybabyanimals/config/AnimalAliasRegistryTest.java`: cover full IDs, malformed IDs, vanilla aliases, dog tamed policy, and case/whitespace normalization.
- Modify `src/main/java/dev/jasmine/carrybabyanimals/config/CarryConfigManager.java`: use the extended resolver for unknown-name filtering and improve warning language from "name" to "name or entity ID".
- Modify `src/test/java/dev/jasmine/carrybabyanimals/config/CarryConfigManagerTest.java`: prove valid full IDs survive filtering, malformed/unknown entries log and filter out, and unknown-only allow lists preserve the restrictive flag.
- Modify `src/main/java/dev/jasmine/carrybabyanimals/carry/CarryEligibility.java`: reuse the resolver so allowed/block full IDs participate in pickup decisions while defaults remain alias-only.
- Modify `src/test/java/dev/jasmine/carrybabyanimals/carry/CarryEligibilityTest.java`: cover modded ID allow, blocked ID precedence, default modded denial, and existing permission enforcement for tamed allowed IDs.
- Modify `README.md`: document full entity ID config examples, no default modded support, permission behavior, and vanilla-client compatibility boundary.
- Modify `docs/manual-test-plan.md`: add manual checks for full IDs, malformed IDs, unknown-only allow lists, and modded-client responsibility.
- Modify `CHANGELOG.md`: add a public Unreleased bullet for server-admin-visible Phase 4 config support.

## Task 1: Resolver Accepts Full Entity IDs

**Files:**
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/config/AnimalAliasRegistry.java`
- Test: `src/test/java/dev/jasmine/carrybabyanimals/config/AnimalAliasRegistryTest.java`

- [ ] **Step 1: Write failing resolver tests**

Add tests:

```java
@Test
void resolvesFullEntityIdsWithoutAddingThemToDefaultAliases() {
    AnimalAliasRegistry aliases = AnimalAliasRegistry.createDefault();

    AnimalAliasRegistry.ResolvedAnimal duck = aliases.resolve(" examplemod:duck ").orElseThrow();

    assertEquals(Identifier.parse("examplemod:duck"), duck.id());
    assertFalse(duck.requiresTamed());
    assertFalse(aliases.aliases().containsKey("examplemod:duck"));
}

@Test
void malformedFullEntityIdsDoNotResolve() {
    AnimalAliasRegistry aliases = AnimalAliasRegistry.createDefault();

    assertTrue(aliases.resolve("examplemod:bad id").isEmpty());
    assertTrue(aliases.resolve("examplemod:").isEmpty());
    assertTrue(aliases.resolve(":duck").isEmpty());
}
```

The test fixture may use `Identifier.parse(...)` for hard-coded valid IDs. Production resolution must use null-returning `Identifier.tryParse(...)` and treat `null` as an invalid or unknown config entry.

- [ ] **Step 2: Run resolver tests and verify failure**

Run:

```powershell
.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.config.AnimalAliasRegistryTest"
```

Expected: the new full-ID test fails because `examplemod:duck` does not resolve yet.

- [ ] **Step 3: Implement ID parsing**

In `AnimalAliasRegistry.resolve(String name)`, after alias lookup misses, parse normalized strings containing `:` with `Identifier.tryParse(normalized)`. Return `new ResolvedAnimal(id, false)` for valid IDs. Keep bare names alias-only, so `duck` remains unknown unless an alias exists.

- [ ] **Step 4: Run resolver tests and verify pass**

Run:

```powershell
.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.config.AnimalAliasRegistryTest"
```

Expected: all resolver tests pass.

## Task 2: Config Filtering And Logging

**Files:**
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/config/CarryConfigManager.java`
- Test: `src/test/java/dev/jasmine/carrybabyanimals/config/CarryConfigManagerTest.java`

- [ ] **Step 1: Write failing config tests**

Add tests that use the existing test logger helper or a minimal logger test double:

```java
@Test
void fullEntityIdsSurviveUnknownFiltering() {
    CarryConfig config = new CarryConfig(List.of("examplemod:duck"), List.of("examplemod:goose"), false, 20);
    CarryConfigManager manager = new CarryConfigManager();
    manager.replaceForTest(config);

    manager.filterAndLogUnknownAnimalNames(AnimalAliasRegistry.createDefault(), logger);

    assertEquals(List.of("examplemod:duck"), manager.config().allowedAnimals());
    assertEquals(List.of("examplemod:goose"), manager.config().blockedAnimals());
}

@Test
void malformedIdsAreLoggedAndRemovedWithoutClearingAllowListRestriction() {
    CarryConfig config = new CarryConfig(List.of("examplemod:bad id"), List.of("bad:id value"), false, 20, true);
    CarryConfigManager manager = new CarryConfigManager();
    manager.replaceForTest(config);

    manager.filterAndLogUnknownAnimalNames(AnimalAliasRegistry.createDefault(), logger);

    assertTrue(manager.config().allowedAnimals().isEmpty());
    assertTrue(manager.config().blockedAnimals().isEmpty());
    assertTrue(manager.config().restrictToAllowedAnimals());
    assertTrue(logger.warnMessages().stream().anyMatch(message -> message.contains("Unknown allowed animal name or entity ID")));
    assertTrue(logger.warnMessages().stream().anyMatch(message -> message.contains("Unknown blocked animal name or entity ID")));
}
```

If `CarryConfigManagerTest` does not already have a replace helper, keep the production change minimal by testing `CarryConfigManager.unknownAnimalNames(...)` and `knownNames(...)` through package-visible helpers instead of adding a public test-only method.

The five-argument `CarryConfig(List<String>, List<String>, boolean, int, boolean)` constructor exists in this repo and sets `restrictToAllowedAnimals` explicitly. Use it only when a test must represent a previously configured allow list after filtering removes every effective entry.

- [ ] **Step 2: Run config tests and verify failure**

Run:

```powershell
.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.config.CarryConfigManagerTest"
```

Expected: full IDs are treated as unknown and/or warning text still says only "name".

- [ ] **Step 3: Implement filtering/logging update**

Because `AnimalAliasRegistry.resolve(...)` now resolves full IDs, keep `unknownNames(...)` and `knownNames(...)` using that resolver. Change warning text to:

```java
logger.warn("Unknown allowed animal name or entity ID in Carry Baby Animals config: {}", name);
logger.warn("Unknown blocked animal name or entity ID in Carry Baby Animals config: {}", name);
```

When rebuilding `CarryConfig`, preserve `config.restrictToAllowedAnimals()` exactly as the current code does. This is the safety mechanism for unknown-only allow lists: parsing or constructing a config with a non-empty allow list sets the restriction flag, and later filtering malformed or unknown entries must not clear that flag just because the effective allowed list becomes empty.

- [ ] **Step 4: Run config tests and verify pass**

Run:

```powershell
.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.config.CarryConfigManagerTest"
```

Expected: all config manager tests pass.

## Task 3: Pickup Eligibility Uses Full IDs Safely

**Files:**
- Modify: `src/main/java/dev/jasmine/carrybabyanimals/carry/CarryEligibility.java`
- Test: `src/test/java/dev/jasmine/carrybabyanimals/carry/CarryEligibilityTest.java`

- [ ] **Step 1: Write failing eligibility tests**

Add tests:

```java
private static final Identifier MODDED_DUCK = Identifier.parse("examplemod:duck");

@Test
void allowedFullEntityIdAllowsModdedAnimalCandidate() {
    CarryConfig config = config(List.of("examplemod:duck"), List.of(), true);

    assertTrue(eligibility.canPickUpResolved(wild(MODDED_DUCK), config, ALL_TAMED_PERMISSIONS));
}

@Test
void moddedAnimalCandidateIsNotAllowedByDefault() {
    CarryConfig config = CarryConfig.defaultConfig();

    assertFalse(eligibility.canPickUpResolved(wild(MODDED_DUCK), config, ALL_TAMED_PERMISSIONS));
}

@Test
void blockedFullEntityIdWinsOverAllowedFullEntityId() {
    CarryConfig config = config(List.of("examplemod:duck"), List.of("examplemod:duck"), true);

    assertFalse(eligibility.canPickUpResolved(wild(MODDED_DUCK), config, ALL_TAMED_PERMISSIONS));
    assertEquals(
            CarryEligibility.PickupDecision.BLOCKED_BY_CONFIG,
            eligibility.pickupDecision(wild(MODDED_DUCK), config, ALL_TAMED_PERMISSIONS)
    );
}

@Test
void tamedPermissionRulesStillApplyToAllowedFullEntityIds() {
    CarryConfig config = config(List.of("examplemod:duck"), List.of(), true);

    assertFalse(eligibility.canPickUpResolved(
            ownedTamed(MODDED_DUCK),
            config,
            new CarryEligibility.PermissionSnapshot(false, true)
    ));
    assertTrue(eligibility.canPickUpResolved(
            ownedTamed(MODDED_DUCK),
            config,
            ALL_TAMED_PERMISSIONS
    ));
}
```

- [ ] **Step 2: Run eligibility tests and verify failure**

Run:

```powershell
.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.carry.CarryEligibilityTest"
```

Expected: the allowed full-ID test fails until resolver support is used by eligibility.

- [ ] **Step 3: Confirm minimal production behavior**

If Task 1 already made `AnimalAliasRegistry.resolve(...)` return full IDs, `CarryEligibility.matchesAny(...)` should pass without further production changes. Do not change `isDefaultSupported(...)`; it must continue checking only built-in alias values so modded IDs are not allowed by default.

Verify this against the current code before deciding no production edit is needed: `matchesAny(...)` must resolve each configured entry through `AnimalAliasRegistry` and compare the resolved `Identifier` to the candidate entity's `EntityType.getKey(...)` ID. If it does not already compare by `Identifier`, update it to do so. Keep `isDefaultSupported(...)` alias-value-only rather than broadening it to all resolvable full IDs.

- [ ] **Step 4: Run eligibility tests and verify pass**

Run:

```powershell
.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.carry.CarryEligibilityTest"
```

Expected: all eligibility tests pass.

## Task 4: Documentation, Manual Testing, And Changelog

**Files:**
- Modify: `README.md`
- Modify: `docs/manual-test-plan.md`
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Update public docs**

In `README.md`, update config text so `allowedAnimals` and `blockedAnimals` are documented as accepting:

- friendly vanilla aliases listed in the generated config comment
- full entity IDs such as `examplemod:duck`

Add a short example:

```json
{
  "allowedAnimals": ["cow", "examplemod:duck"],
  "blockedAnimals": ["examplemod:fragile_chick"]
}
```

State that no modded entities are enabled by default, existing carry permission nodes still apply, and CarryBabyAnimals does not make another mod's custom entity available to vanilla clients if that other mod requires clients to install it.

- [ ] **Step 2: Update manual test plan**

In `docs/manual-test-plan.md`, add config rows for:

- full modded entity ID in `allowedAnimals`
- full modded entity ID in `blockedAnimals`
- malformed ID logging without crash
- unknown-only allow list still denying pickup
- vanilla-client boundary with custom entities owned by their source mod

- [ ] **Step 3: Update changelog**

Add a public Unreleased bullet:

```markdown
- Added server config support for full entity IDs in `allowedAnimals` and `blockedAnimals`, letting server owners opt into compatible baby animal entities from other mods without enabling any modded animals by default.
```

- [ ] **Step 4: Run docs/changelog gate**

Run:

```powershell
.\gradlew.bat checkChangelog
```

Expected: changelog gate passes.

## Task 5: Final Review And Verification

**Files:**
- Review all changed Phase 4 files.

- [ ] **Step 1: Self-review against roadmap and this plan**

Check:

- full IDs work in allow and block lists
- aliases still work
- `dog` still requires tamed wolves
- unknown-only allow lists deny instead of widening to defaults
- no modded entity is allowed by default
- permissions still apply
- docs avoid promising vanilla-client support for custom entities from other mods
- no Phase 5 rendering/client polish slipped in

- [ ] **Step 2: Run targeted Phase 4 tests**

Run:

```powershell
.\gradlew.bat test --tests "dev.jasmine.carrybabyanimals.config.AnimalAliasRegistryTest" --tests "dev.jasmine.carrybabyanimals.config.CarryConfigManagerTest" --tests "dev.jasmine.carrybabyanimals.carry.CarryEligibilityTest"
```

Expected: targeted tests pass.

- [ ] **Step 3: Run required verification**

Run:

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat checkChangelog
git diff --check
git status --short
```

Expected: Gradle commands and diff check pass. `git status --short` shows only intentional Phase 4 changes before commit.

- [ ] **Step 4: Commit**

After plan and implementation review gates are complete and findings are actioned, commit:

```powershell
git add CHANGELOG.md README.md docs/manual-test-plan.md docs/superpowers/plans/2026-05-28-expanded-modded-animal-support.md src/main/java/dev/jasmine/carrybabyanimals/config/AnimalAliasRegistry.java src/main/java/dev/jasmine/carrybabyanimals/config/CarryConfigManager.java src/main/java/dev/jasmine/carrybabyanimals/carry/CarryEligibility.java src/test/java/dev/jasmine/carrybabyanimals/config/AnimalAliasRegistryTest.java src/test/java/dev/jasmine/carrybabyanimals/config/CarryConfigManagerTest.java src/test/java/dev/jasmine/carrybabyanimals/carry/CarryEligibilityTest.java
git commit -m "feat: add modded animal config IDs"
```

## Self-Review

- Spec coverage: covers full entity IDs, alias preservation, no default modded support, clear unknown logging, unknown-only allow-list safety, permission continuity, server-owned behavior, docs, changelog, and verification.
- Placeholder scan: no placeholders or deferred behavior remain.
- Type consistency: all new behavior is expressed through existing `AnimalAliasRegistry.ResolvedAnimal`, `CarryConfig`, and `CarryEligibility.CarryCandidate` types.
- Scope check: admin-defined aliases are deliberately deferred because full IDs satisfy Phase 4 with the existing config lists and avoid adding ambiguous alias ownership metadata in this pass.
