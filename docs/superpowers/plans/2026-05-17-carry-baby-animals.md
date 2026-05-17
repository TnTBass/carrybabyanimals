# CarryBabyAnimals Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Fabric mod for Minecraft 26.1.2 that lets players carry one baby animal, pet it for heart particles, and show a held-in-hands render on modded clients with a passenger fallback for vanilla clients.

**Architecture:** The server is authoritative: it validates pickup, tracks one carried baby per player, uses the passenger system as the vanilla fallback, suppresses AI while carried, blocks hand actions, and drops safely on invalid state. Client code only upgrades presentation by suppressing the passenger render for carried babies and rendering the baby at the player's hand position.

**Tech Stack:** Minecraft 26.1.2, Java 25, Fabric Loader 0.19.2, Fabric API 0.149.0+26.1.2, Fabric Permissions API, Gradle 9.4.0, Fabric Loom 1.16-SNAPSHOT or the current official Fabric 26.1 template plugin.

---

## Reference Inputs

- Spec: `docs/superpowers/specs/2026-05-17-carry-baby-animals-design.md`
- Official Fabric 26.1 note: `https://fabricmc.net/2026/03/14/261.html`
- Fabric API Maven index for 26.1.2 artifacts: `https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/`
- Fabric example mod 26.1 branch: `https://github.com/FabricMC/fabric-example-mod/tree/26.1`

## File Structure

- `settings.gradle`: Gradle project name and plugin repositories.
- `build.gradle`: Fabric 26.1 build, Java 25 toolchain, common/client source sets, dependency wiring, and publishing metadata.
- `gradle.properties`: Minecraft, Loader, Fabric API, mod version, maven group, and archives base name.
- `src/main/resources/fabric.mod.json`: Mod metadata, entrypoints, mixins, and dependency declarations.
- `src/main/resources/carrybabyanimals.mixins.json`: Common/server-side mixins for interaction, attack, and lifecycle hooks.
- `src/client/resources/carrybabyanimals.client.mixins.json`: Client-only mixins for entity render suppression.
- `src/main/java/com/explosive/carrybabyanimals/CarryBabyAnimals.java`: Common initializer and shared constants.
- `src/client/java/com/explosive/carrybabyanimals/client/CarryBabyAnimalsClient.java`: Client initializer and renderer registration.
- `src/main/java/com/explosive/carrybabyanimals/config/CarryConfig.java`: Immutable config model.
- `src/main/java/com/explosive/carrybabyanimals/config/CarryConfigManager.java`: JSON load, save, validation, and logging.
- `src/main/java/com/explosive/carrybabyanimals/config/AnimalAliasRegistry.java`: Friendly-name alias mapping for vanilla animals.
- `src/main/java/com/explosive/carrybabyanimals/carry/CarryManager.java`: One-carried-animal-per-player state and public carry/drop API.
- `src/main/java/com/explosive/carrybabyanimals/carry/CarryState.java`: Small value object for carried entity id, carrier id, and timestamps.
- `src/main/java/com/explosive/carrybabyanimals/carry/CarryEligibility.java`: Entity, age, config, ownership, and permission checks.
- `src/main/java/com/explosive/carrybabyanimals/carry/CarryAttachment.java`: Passenger attach/drop placement and safe dismount handling.
- `src/main/java/com/explosive/carrybabyanimals/carry/CarryAiController.java`: Disable/restore carried animal navigation and AI goals.
- `src/main/java/com/explosive/carrybabyanimals/carry/CarryInteractionHandler.java`: Pickup/drop interactions, hand blocking, and petting.
- `src/main/java/com/explosive/carrybabyanimals/carry/CarryTicker.java`: Cleanup on invalid state, growth checks, and fallback safety.
- `src/main/java/com/explosive/carrybabyanimals/permissions/CarryPermissions.java`: Fabric Permissions API wrapper with default-true carry behavior.
- `src/main/java/com/explosive/carrybabyanimals/network/CarryNetworking.java`: Minimal server-to-client carry state sync for rendering.
- `src/client/java/com/explosive/carrybabyanimals/client/render/CarriedBabyRenderState.java`: Client cache of carried entity ids and carriers.
- `src/client/java/com/explosive/carrybabyanimals/client/render/CarriedBabyRenderer.java`: Held-in-hands render positioning.
- `src/client/java/com/explosive/carrybabyanimals/client/mixin/LivingEntityRendererMixin.java`: Suppress vanilla passenger-position render for carried babies.
- `src/test/java/com/explosive/carrybabyanimals/config/CarryConfigManagerTest.java`: Config parsing tests.
- `src/test/java/com/explosive/carrybabyanimals/config/AnimalAliasRegistryTest.java`: Friendly-name and wolf/dog alias tests.
- `src/test/java/com/explosive/carrybabyanimals/carry/CarryManagerTest.java`: One-at-a-time and cooldown logic tests with lightweight fakes.
- `docs/implementation-notes/2026-05-17-fabric-26-api-surfaces.md`: API reconnaissance notes for hooks that must be verified against 26.1.2.

## Task 1: Scaffold Fabric 26.1.2 Project

**Files:**
- Create: `settings.gradle`
- Create: `build.gradle`
- Create: `gradle.properties`
- Create: `src/main/resources/fabric.mod.json`
- Create: `src/main/java/com/explosive/carrybabyanimals/CarryBabyAnimals.java`
- Create: `src/client/java/com/explosive/carrybabyanimals/client/CarryBabyAnimalsClient.java`

- [ ] **Step 1: Write the Gradle project metadata**

Create `settings.gradle`:

```groovy
pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = "https://maven.fabricmc.net/"
        }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven {
            name = "Fabric"
            url = "https://maven.fabricmc.net/"
        }
    }
}

rootProject.name = "CarryBabyAnimals"
```

Create `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2G
org.gradle.parallel=true

minecraft_version=26.1.2
loader_version=0.19.2
fabric_version=0.149.0+26.1.2

mod_version=0.1.0
maven_group=com.explosive
archives_base_name=carry-baby-animals
```

- [ ] **Step 2: Write the Fabric build**

Create `build.gradle`:

```groovy
plugins {
    id "fabric-loom" version "1.16-SNAPSHOT"
    id "maven-publish"
}

version = project.mod_version
group = project.maven_group

base {
    archivesName = project.archives_base_name
}

loom {
    splitEnvironmentSourceSets()

    mods {
        "carrybabyanimals" {
            sourceSet sourceSets.main
            sourceSet sourceSets.client
        }
    }
}

repositories {
    maven {
        name = "Fabric"
        url = "https://maven.fabricmc.net/"
    }
    mavenCentral()
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings loom.officialMojangMappings()
    implementation "net.fabricmc:fabric-loader:${project.loader_version}"
    implementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
}

tasks.withType(JavaCompile).configureEach {
    it.options.release = 25
}

processResources {
    inputs.property "version", project.version
    filesMatching("fabric.mod.json") {
        expand "version": inputs.properties.version
    }
}
```

- [ ] **Step 3: Write mod metadata and entrypoints**

Create `src/main/resources/fabric.mod.json`:

```json
{
  "schemaVersion": 1,
  "id": "carrybabyanimals",
  "version": "${version}",
  "name": "Carry Baby Animals",
  "description": "Carry and pet baby animals without putting them in your inventory.",
  "authors": ["Tyler", "Jasmine"],
  "contact": {},
  "license": "MIT",
  "environment": "*",
  "entrypoints": {
    "main": [
      "com.explosive.carrybabyanimals.CarryBabyAnimals"
    ],
    "client": [
      "com.explosive.carrybabyanimals.client.CarryBabyAnimalsClient"
    ]
  },
  "mixins": [
    "carrybabyanimals.mixins.json",
    {
      "config": "carrybabyanimals.client.mixins.json",
      "environment": "client"
    }
  ],
  "depends": {
    "fabricloader": ">=0.19.2",
    "minecraft": "26.1.2",
    "java": ">=25",
    "fabric-api": "*"
  }
}
```

Create `src/main/java/com/explosive/carrybabyanimals/CarryBabyAnimals.java`:

```java
package com.explosive.carrybabyanimals;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CarryBabyAnimals implements ModInitializer {
    public static final String MOD_ID = "carrybabyanimals";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Carry Baby Animals initialized");
    }
}
```

Create `src/client/java/com/explosive/carrybabyanimals/client/CarryBabyAnimalsClient.java`:

```java
package com.explosive.carrybabyanimals.client;

import net.fabricmc.api.ClientModInitializer;

public final class CarryBabyAnimalsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
    }
}
```

- [ ] **Step 4: Generate the Gradle wrapper**

Run:

```powershell
gradle wrapper --gradle-version 9.4.0
```

Expected: `gradlew`, `gradlew.bat`, and `gradle/wrapper/*` are created.

- [ ] **Step 5: Verify the scaffold compiles**

Run:

```powershell
.\gradlew.bat build
```

Expected: Gradle resolves Minecraft 26.1.2, Fabric Loader, and Fabric API, then finishes with `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```powershell
git add settings.gradle build.gradle gradle.properties gradlew gradlew.bat gradle src/main/resources/fabric.mod.json src/main/java/com/explosive/carrybabyanimals/CarryBabyAnimals.java src/client/java/com/explosive/carrybabyanimals/client/CarryBabyAnimalsClient.java
git commit -m "chore: scaffold Fabric mod"
```

## Task 2: Record Fabric 26.1.2 API Reconnaissance

**Files:**
- Create: `docs/implementation-notes/2026-05-17-fabric-26-api-surfaces.md`

- [ ] **Step 1: Inspect the generated dependency sources and mappings**

Run:

```powershell
.\gradlew.bat genSources
```

Expected: Minecraft sources are generated successfully for IDE/API inspection.

- [ ] **Step 2: Write the reconnaissance note**

Create `docs/implementation-notes/2026-05-17-fabric-26-api-surfaces.md`:

```markdown
# Fabric 26.1.2 API Surfaces

## Confirmed Build Inputs

- Minecraft: 26.1.2
- Java: 25
- Fabric Loader: 0.19.2
- Fabric API: 0.149.0+26.1.2
- Mappings: official Mojang mappings through Loom

## Passenger Fallback

Use the vanilla passenger system. The carried baby rides the player while carried. Vanilla clients see the baby above the player's head. Modded clients suppress the vanilla render and render the baby at the player's hand position.

## Growth Detection

Record whether Fabric 26.x exposes a baby-to-adult growth event here during implementation. If no event exists, use the `CarryTicker` tick-check: every server tick, inspect carried animals and drop them when `AgeableMob#isBaby()` becomes false. This can allow a one-tick race window between growth and drop; the race is acceptable because the next tick drops the animal safely.

## Interaction Hooks

Record the final hook names used for:

- Sneak-right-click entity interaction.
- Left-click attack interception while carrying.
- Use-item/block interaction blocking while carrying.
- Logout, death, dimension change, and server stop cleanup.

## Renderer Hooks

Record the final hook used to suppress vanilla rendering for carried baby passengers and the renderer path used for the held-in-hands replacement.
```

- [ ] **Step 3: Commit**

```powershell
git add docs/implementation-notes/2026-05-17-fabric-26-api-surfaces.md
git commit -m "docs: record Fabric API reconnaissance checklist"
```

## Task 3: Add Config Model And Friendly Animal Names

**Files:**
- Create: `src/main/java/com/explosive/carrybabyanimals/config/CarryConfig.java`
- Create: `src/main/java/com/explosive/carrybabyanimals/config/AnimalAliasRegistry.java`
- Create: `src/main/java/com/explosive/carrybabyanimals/config/CarryConfigManager.java`
- Test: `src/test/java/com/explosive/carrybabyanimals/config/AnimalAliasRegistryTest.java`
- Test: `src/test/java/com/explosive/carrybabyanimals/config/CarryConfigManagerTest.java`

- [ ] **Step 1: Write alias tests**

Create `src/test/java/com/explosive/carrybabyanimals/config/AnimalAliasRegistryTest.java`:

```java
package com.explosive.carrybabyanimals.config;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class AnimalAliasRegistryTest {
    @Test
    void resolvesCommonFriendlyNames() {
        AnimalAliasRegistry aliases = AnimalAliasRegistry.createDefault();

        assertEquals(ResourceLocation.withDefaultNamespace("cow"), aliases.resolve("cow").orElseThrow().id());
        assertEquals(ResourceLocation.withDefaultNamespace("pig"), aliases.resolve("pig").orElseThrow().id());
        assertEquals(ResourceLocation.withDefaultNamespace("cat"), aliases.resolve("cat").orElseThrow().id());
    }

    @Test
    void dogAndWolfResolveToWolfWithDifferentTamedPolicy() {
        AnimalAliasRegistry aliases = AnimalAliasRegistry.createDefault();

        AnimalAliasRegistry.ResolvedAnimal dog = aliases.resolve("dog").orElseThrow();
        AnimalAliasRegistry.ResolvedAnimal wolf = aliases.resolve("wolf").orElseThrow();

        assertEquals(ResourceLocation.withDefaultNamespace("wolf"), dog.id());
        assertEquals(ResourceLocation.withDefaultNamespace("wolf"), wolf.id());
        assertTrue(dog.requiresTamed());
        assertFalse(wolf.requiresTamed());
    }

    @Test
    void ignoresCaseAndWhitespace() {
        AnimalAliasRegistry aliases = AnimalAliasRegistry.createDefault();

        assertEquals(ResourceLocation.withDefaultNamespace("sheep"), aliases.resolve(" Sheep ").orElseThrow().id());
    }
}
```

- [ ] **Step 2: Run the alias tests to verify they fail**

Run:

```powershell
.\gradlew.bat test --tests com.explosive.carrybabyanimals.config.AnimalAliasRegistryTest
```

Expected: FAIL because `AnimalAliasRegistry` does not exist.

- [ ] **Step 3: Implement the alias registry**

Create `src/main/java/com/explosive/carrybabyanimals/config/AnimalAliasRegistry.java`:

```java
package com.explosive.carrybabyanimals.config;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class AnimalAliasRegistry {
    private final Map<String, ResolvedAnimal> aliases;

    private AnimalAliasRegistry(Map<String, ResolvedAnimal> aliases) {
        this.aliases = Map.copyOf(aliases);
    }

    public static AnimalAliasRegistry createDefault() {
        Map<String, ResolvedAnimal> aliases = new LinkedHashMap<>();
        add(aliases, "cow", "cow", false);
        add(aliases, "pig", "pig", false);
        add(aliases, "sheep", "sheep", false);
        add(aliases, "chicken", "chicken", false);
        add(aliases, "goat", "goat", false);
        add(aliases, "rabbit", "rabbit", false);
        add(aliases, "cat", "cat", false);
        add(aliases, "fox", "fox", false);
        add(aliases, "horse", "horse", false);
        add(aliases, "donkey", "donkey", false);
        add(aliases, "mule", "mule", false);
        add(aliases, "llama", "llama", false);
        add(aliases, "trader_llama", "trader_llama", false);
        add(aliases, "camel", "camel", false);
        add(aliases, "panda", "panda", false);
        add(aliases, "turtle", "turtle", false);
        add(aliases, "wolf", "wolf", false);
        add(aliases, "dog", "wolf", true);
        return new AnimalAliasRegistry(aliases);
    }

    private static void add(Map<String, ResolvedAnimal> aliases, String alias, String vanillaId, boolean requiresTamed) {
        aliases.put(normalize(alias), new ResolvedAnimal(ResourceLocation.withDefaultNamespace(vanillaId), requiresTamed));
    }

    public Optional<ResolvedAnimal> resolve(String name) {
        return Optional.ofNullable(aliases.get(normalize(name)));
    }

    public Map<String, ResolvedAnimal> aliases() {
        return aliases;
    }

    private static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public record ResolvedAnimal(ResourceLocation id, boolean requiresTamed) {
    }
}
```

- [ ] **Step 4: Write config parsing tests**

Create `src/test/java/com/explosive/carrybabyanimals/config/CarryConfigManagerTest.java`:

```java
package com.explosive.carrybabyanimals.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class CarryConfigManagerTest {
    @Test
    void defaultConfigAllowsDefaultSetAndBlocksNobody() {
        CarryConfig config = CarryConfig.defaultConfig();

        assertTrue(config.allowedAnimals().isEmpty());
        assertTrue(config.blockedAnimals().isEmpty());
        assertFalse(config.allowCarryingOtherPlayersTamedAnimals());
        assertEquals(20, config.pettingCooldownTicks());
    }

    @Test
    void parsedConfigUsesFriendlyNames() {
        String json = """
            {
              "allowedAnimals": ["cow", "dog"],
              "blockedAnimals": ["panda"],
              "allowCarryingOtherPlayersTamedAnimals": true,
              "pettingCooldownTicks": 40
            }
            """;

        CarryConfig config = CarryConfigManager.parse(json);

        assertEquals(List.of("cow", "dog"), config.allowedAnimals());
        assertEquals(List.of("panda"), config.blockedAnimals());
        assertTrue(config.allowCarryingOtherPlayersTamedAnimals());
        assertEquals(40, config.pettingCooldownTicks());
    }
}
```

- [ ] **Step 5: Run config tests to verify they fail**

Run:

```powershell
.\gradlew.bat test --tests com.explosive.carrybabyanimals.config.CarryConfigManagerTest
```

Expected: FAIL because config classes do not exist.

- [ ] **Step 6: Implement config model and parser**

Create `src/main/java/com/explosive/carrybabyanimals/config/CarryConfig.java`:

```java
package com.explosive.carrybabyanimals.config;

import java.util.List;

public record CarryConfig(
        List<String> allowedAnimals,
        List<String> blockedAnimals,
        boolean allowCarryingOtherPlayersTamedAnimals,
        int pettingCooldownTicks
) {
    public static CarryConfig defaultConfig() {
        return new CarryConfig(List.of(), List.of(), false, 20);
    }
}
```

Create `src/main/java/com/explosive/carrybabyanimals/config/CarryConfigManager.java`:

```java
package com.explosive.carrybabyanimals.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class CarryConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private CarryConfig config = CarryConfig.defaultConfig();

    public CarryConfig config() {
        return config;
    }

    public void load(Path path) throws IOException {
        if (Files.notExists(path)) {
            saveDefault(path);
        }
        config = parse(Files.readString(path));
    }

    public void saveDefault(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(CarryConfig.defaultConfig()));
    }

    public static CarryConfig parse(String json) {
        RawConfig raw = GSON.fromJson(json, RawConfig.class);
        if (raw == null) {
            return CarryConfig.defaultConfig();
        }
        return new CarryConfig(
                raw.allowedAnimals == null ? List.of() : List.copyOf(raw.allowedAnimals),
                raw.blockedAnimals == null ? List.of() : List.copyOf(raw.blockedAnimals),
                raw.allowCarryingOtherPlayersTamedAnimals,
                raw.pettingCooldownTicks <= 0 ? 20 : raw.pettingCooldownTicks
        );
    }

    private static final class RawConfig {
        List<String> allowedAnimals;
        List<String> blockedAnimals;
        boolean allowCarryingOtherPlayersTamedAnimals;
        int pettingCooldownTicks;
    }
}
```

- [ ] **Step 7: Run tests**

Run:

```powershell
.\gradlew.bat test --tests com.explosive.carrybabyanimals.config.*
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add src/main/java/com/explosive/carrybabyanimals/config src/test/java/com/explosive/carrybabyanimals/config
git commit -m "feat: add readable animal config"
```

## Task 4: Add Carry State, Eligibility, And Permissions

**Files:**
- Create: `src/main/java/com/explosive/carrybabyanimals/carry/CarryState.java`
- Create: `src/main/java/com/explosive/carrybabyanimals/carry/CarryManager.java`
- Create: `src/main/java/com/explosive/carrybabyanimals/carry/CarryEligibility.java`
- Create: `src/main/java/com/explosive/carrybabyanimals/permissions/CarryPermissions.java`
- Test: `src/test/java/com/explosive/carrybabyanimals/carry/CarryManagerTest.java`

- [ ] **Step 1: Write carry manager tests**

Create `src/test/java/com/explosive/carrybabyanimals/carry/CarryManagerTest.java`:

```java
package com.explosive.carrybabyanimals.carry;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class CarryManagerTest {
    @Test
    void tracksOneAnimalPerPlayer() {
        CarryManager manager = new CarryManager();
        UUID player = UUID.randomUUID();
        int firstEntityId = 10;
        int secondEntityId = 11;

        assertTrue(manager.beginCarry(player, firstEntityId));
        assertFalse(manager.beginCarry(player, secondEntityId));
        assertEquals(firstEntityId, manager.carriedEntityId(player).orElseThrow());
    }

    @Test
    void dropClearsCarriedAnimal() {
        CarryManager manager = new CarryManager();
        UUID player = UUID.randomUUID();

        assertTrue(manager.beginCarry(player, 10));
        assertTrue(manager.endCarry(player).isPresent());
        assertTrue(manager.carriedEntityId(player).isEmpty());
    }
}
```

- [ ] **Step 2: Run manager tests to verify they fail**

Run:

```powershell
.\gradlew.bat test --tests com.explosive.carrybabyanimals.carry.CarryManagerTest
```

Expected: FAIL because `CarryManager` does not exist.

- [ ] **Step 3: Implement carry state and manager**

Create `src/main/java/com/explosive/carrybabyanimals/carry/CarryState.java`:

```java
package com.explosive.carrybabyanimals.carry;

import java.util.UUID;

public record CarryState(UUID carrierId, int carriedEntityId, long startedAtTick) {
}
```

Create `src/main/java/com/explosive/carrybabyanimals/carry/CarryManager.java`:

```java
package com.explosive.carrybabyanimals.carry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class CarryManager {
    private final Map<UUID, CarryState> carriedByPlayer = new HashMap<>();

    public boolean beginCarry(UUID playerId, int entityId) {
        if (carriedByPlayer.containsKey(playerId)) {
            return false;
        }
        carriedByPlayer.put(playerId, new CarryState(playerId, entityId, 0L));
        return true;
    }

    public Optional<CarryState> endCarry(UUID playerId) {
        return Optional.ofNullable(carriedByPlayer.remove(playerId));
    }

    public Optional<Integer> carriedEntityId(UUID playerId) {
        return Optional.ofNullable(carriedByPlayer.get(playerId)).map(CarryState::carriedEntityId);
    }

    public boolean isCarrying(UUID playerId) {
        return carriedByPlayer.containsKey(playerId);
    }
}
```

- [ ] **Step 4: Add permission wrapper**

Create `src/main/java/com/explosive/carrybabyanimals/permissions/CarryPermissions.java`:

```java
package com.explosive.carrybabyanimals.permissions;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.level.ServerPlayer;

public final class CarryPermissions {
    public static final String CARRY = "carrybabyanimals.carry";
    public static final String CARRY_TAMED = "carrybabyanimals.carry.tamed";
    public static final String CARRY_OTHERS_TAMED = "carrybabyanimals.carry.others_tamed";
    public static final String RELOAD = "carrybabyanimals.reload";

    private CarryPermissions() {
    }

    public static boolean canCarry(ServerPlayer player) {
        return Permissions.check(player, CARRY, true);
    }

    public static boolean canCarryTamed(ServerPlayer player) {
        return Permissions.check(player, CARRY_TAMED, true);
    }

    public static boolean canCarryOthersTamed(ServerPlayer player) {
        return Permissions.check(player, CARRY_OTHERS_TAMED, false);
    }

    public static boolean canReload(ServerPlayer player) {
        return Permissions.check(player, RELOAD, player.hasPermissions(2));
    }
}
```

- [ ] **Step 5: Add eligibility service**

Create `src/main/java/com/explosive/carrybabyanimals/carry/CarryEligibility.java`:

```java
package com.explosive.carrybabyanimals.carry;

import com.explosive.carrybabyanimals.config.AnimalAliasRegistry;
import com.explosive.carrybabyanimals.config.CarryConfig;
import com.explosive.carrybabyanimals.permissions.CarryPermissions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;

import java.util.HashSet;
import java.util.Set;

public final class CarryEligibility {
    private final AnimalAliasRegistry aliases;

    public CarryEligibility(AnimalAliasRegistry aliases) {
        this.aliases = aliases;
    }

    public boolean canPickUp(ServerPlayer player, Entity entity, CarryConfig config) {
        if (!CarryPermissions.canCarry(player)) {
            return false;
        }
        if (!(entity instanceof Animal animal)) {
            return false;
        }
        if (!animal.isBaby()) {
            return false;
        }
        ResourceLocation entityId = EntityType.getKey(entity.getType());
        if (!isAllowed(entityId, animal, config)) {
            return false;
        }
        return passesTamedRules(player, animal, config);
    }

    private boolean isAllowed(ResourceLocation entityId, Animal animal, CarryConfig config) {
        Set<ResourceLocation> blocked = resolve(config.blockedAnimals(), false);
        if (blocked.contains(entityId)) {
            return false;
        }
        Set<ResourceLocation> allowed = resolve(config.allowedAnimals(), true);
        return allowed.isEmpty() || allowed.contains(entityId);
    }

    private Set<ResourceLocation> resolve(Iterable<String> names, boolean includeDogAlias) {
        Set<ResourceLocation> result = new HashSet<>();
        for (String name : names) {
            aliases.resolve(name).ifPresent(resolved -> {
                if (includeDogAlias || !resolved.requiresTamed()) {
                    result.add(resolved.id());
                }
            });
        }
        return result;
    }

    private boolean passesTamedRules(ServerPlayer player, LivingEntity entity, CarryConfig config) {
        if (!(entity instanceof TamableAnimal tamable) || !tamable.isTame()) {
            return true;
        }
        if (player.getUUID().equals(tamable.getOwnerUUID())) {
            return CarryPermissions.canCarryTamed(player);
        }
        return config.allowCarryingOtherPlayersTamedAnimals() && CarryPermissions.canCarryOthersTamed(player);
    }
}
```

- [ ] **Step 6: Run tests and compile**

Run:

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

Expected: tests PASS and build SUCCESSFUL. If `TamableAnimal` package names differ in official 26.1.2 mappings, update imports and record the exact class name in `docs/implementation-notes/2026-05-17-fabric-26-api-surfaces.md`.

- [ ] **Step 7: Commit**

```powershell
git add src/main/java/com/explosive/carrybabyanimals/carry src/main/java/com/explosive/carrybabyanimals/permissions src/test/java/com/explosive/carrybabyanimals/carry docs/implementation-notes/2026-05-17-fabric-26-api-surfaces.md
git commit -m "feat: add carry eligibility and permissions"
```

## Task 5: Implement Pickup, Passenger Attachment, AI Suppression, And Drop

**Files:**
- Create: `src/main/java/com/explosive/carrybabyanimals/carry/CarryAttachment.java`
- Create: `src/main/java/com/explosive/carrybabyanimals/carry/CarryAiController.java`
- Create: `src/main/java/com/explosive/carrybabyanimals/carry/CarryInteractionHandler.java`
- Modify: `src/main/java/com/explosive/carrybabyanimals/CarryBabyAnimals.java`

- [ ] **Step 1: Implement passenger attachment**

Create `src/main/java/com/explosive/carrybabyanimals/carry/CarryAttachment.java`:

```java
package com.explosive.carrybabyanimals.carry;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class CarryAttachment {
    public boolean attach(ServerPlayer carrier, Entity baby) {
        return baby.startRiding(carrier, true);
    }

    public void dropInFront(ServerPlayer carrier, Entity baby) {
        baby.stopRiding();
        Vec3 look = carrier.getLookAngle().normalize();
        Vec3 target = carrier.position().add(look.x * 1.25D, 0.0D, look.z * 1.25D);
        baby.moveTo(target.x, carrier.getY(), target.z, baby.getYRot(), baby.getXRot());
        if (carrier.level() instanceof ServerLevel serverLevel) {
            serverLevel.getChunk(baby.blockPosition());
        }
    }
}
```

- [ ] **Step 2: Implement AI suppression**

Create `src/main/java/com/explosive/carrybabyanimals/carry/CarryAiController.java`:

```java
package com.explosive.carrybabyanimals.carry;

import net.minecraft.world.entity.Mob;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class CarryAiController {
    private final Set<UUID> suppressed = new HashSet<>();

    public void suppress(Mob mob) {
        if (suppressed.add(mob.getUUID())) {
            mob.getNavigation().stop();
            mob.setNoAi(true);
        }
    }

    public void restore(Mob mob) {
        if (suppressed.remove(mob.getUUID())) {
            mob.setNoAi(false);
        }
    }
}
```

- [ ] **Step 3: Implement interaction handler**

Create `src/main/java/com/explosive/carrybabyanimals/carry/CarryInteractionHandler.java`:

```java
package com.explosive.carrybabyanimals.carry;

import com.explosive.carrybabyanimals.config.CarryConfigManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

public final class CarryInteractionHandler {
    private final CarryManager carryManager;
    private final CarryEligibility eligibility;
    private final CarryConfigManager configManager;
    private final CarryAttachment attachment;
    private final CarryAiController aiController;

    public CarryInteractionHandler(
            CarryManager carryManager,
            CarryEligibility eligibility,
            CarryConfigManager configManager,
            CarryAttachment attachment,
            CarryAiController aiController
    ) {
        this.carryManager = carryManager;
        this.eligibility = eligibility;
        this.configManager = configManager;
        this.attachment = attachment;
        this.aiController = aiController;
    }

    public InteractionResult onEntityInteract(ServerPlayer player, Entity target) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (carryManager.isCarrying(player.getUUID())) {
            dropCurrent(player);
            return InteractionResult.SUCCESS;
        }
        if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!eligibility.canPickUp(player, target, configManager.config())) {
            return InteractionResult.PASS;
        }
        if (carryManager.beginCarry(player.getUUID(), target.getId()) && attachment.attach(player, target)) {
            if (target instanceof Mob mob) {
                aiController.suppress(mob);
            }
            return InteractionResult.SUCCESS;
        }
        carryManager.endCarry(player.getUUID());
        return InteractionResult.PASS;
    }

    public void dropCurrent(ServerPlayer player) {
        carryManager.endCarry(player.getUUID()).ifPresent(state -> {
            Entity baby = player.level().getEntity(state.carriedEntityId());
            if (baby != null) {
                if (baby instanceof Mob mob) {
                    aiController.restore(mob);
                }
                attachment.dropInFront(player, baby);
            }
        });
    }
}
```

- [ ] **Step 4: Wire common initializer singletons**

Modify `src/main/java/com/explosive/carrybabyanimals/CarryBabyAnimals.java`:

```java
package com.explosive.carrybabyanimals;

import com.explosive.carrybabyanimals.carry.CarryAiController;
import com.explosive.carrybabyanimals.carry.CarryAttachment;
import com.explosive.carrybabyanimals.carry.CarryEligibility;
import com.explosive.carrybabyanimals.carry.CarryInteractionHandler;
import com.explosive.carrybabyanimals.carry.CarryManager;
import com.explosive.carrybabyanimals.config.AnimalAliasRegistry;
import com.explosive.carrybabyanimals.config.CarryConfigManager;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CarryBabyAnimals implements ModInitializer {
    public static final String MOD_ID = "carrybabyanimals";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final CarryConfigManager CONFIG = new CarryConfigManager();
    public static final CarryManager CARRY_MANAGER = new CarryManager();
    public static final CarryAiController AI_CONTROLLER = new CarryAiController();
    public static final CarryInteractionHandler INTERACTIONS = new CarryInteractionHandler(
            CARRY_MANAGER,
            new CarryEligibility(AnimalAliasRegistry.createDefault()),
            CONFIG,
            new CarryAttachment(),
            AI_CONTROLLER
    );

    @Override
    public void onInitialize() {
        LOGGER.info("Carry Baby Animals initialized");
    }
}
```

- [ ] **Step 5: Run build**

Run:

```powershell
.\gradlew.bat build
```

Expected: BUILD SUCCESSFUL after import/API corrections for official 26.1.2 mappings are reflected in the implementation notes.

- [ ] **Step 6: Commit**

```powershell
git add src/main/java/com/explosive/carrybabyanimals
git commit -m "feat: add carry pickup and passenger drop"
```

## Task 6: Register Server Events And Cleanup Ticker

**Files:**
- Create: `src/main/java/com/explosive/carrybabyanimals/carry/CarryTicker.java`
- Modify: `src/main/java/com/explosive/carrybabyanimals/CarryBabyAnimals.java`

- [ ] **Step 1: Implement ticker cleanup**

Create `src/main/java/com/explosive/carrybabyanimals/carry/CarryTicker.java`:

```java
package com.explosive.carrybabyanimals.carry;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;

public final class CarryTicker {
    private final CarryManager carryManager;
    private final CarryInteractionHandler interactions;

    public CarryTicker(CarryManager carryManager, CarryInteractionHandler interactions) {
        this.carryManager = carryManager;
        this.interactions = interactions;
    }

    public void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            carryManager.carriedEntityId(player.getUUID()).ifPresent(entityId -> {
                Entity entity = player.level().getEntity(entityId);
                if (!(entity instanceof Animal animal) || !animal.isBaby() || entity.getVehicle() != player) {
                    interactions.dropCurrent(player);
                }
            });
        }
    }
}
```

- [ ] **Step 2: Register interaction and tick events**

Modify `src/main/java/com/explosive/carrybabyanimals/CarryBabyAnimals.java` so `onInitialize()` registers events:

```java
@Override
public void onInitialize() {
    LOGGER.info("Carry Baby Animals initialized");
    CarryTicker ticker = new CarryTicker(CARRY_MANAGER, INTERACTIONS);

    ServerTickEvents.END_SERVER_TICK.register(ticker::tick);
    UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
        if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        return INTERACTIONS.onEntityInteract(serverPlayer, entity);
    });
}
```

Add these imports to the same file:

```java
import com.explosive.carrybabyanimals.carry.CarryTicker;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
```

- [ ] **Step 3: Add cleanup hooks for logout and server stop**

Extend `onInitialize()` with server lifecycle callbacks after the tick registration:

```java
ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
        INTERACTIONS.dropCurrent(handler.getPlayer())
);
ServerLifecycleEvents.SERVER_STOPPING.register(server ->
        server.getPlayerList().getPlayers().forEach(INTERACTIONS::dropCurrent)
);
```

Add imports:

```java
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
```

- [ ] **Step 4: Run build**

Run:

```powershell
.\gradlew.bat build
```

Expected: BUILD SUCCESSFUL. If event package names changed for Fabric 26.x, update imports and record the exact event names in `docs/implementation-notes/2026-05-17-fabric-26-api-surfaces.md`.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/com/explosive/carrybabyanimals docs/implementation-notes/2026-05-17-fabric-26-api-surfaces.md
git commit -m "feat: register carry events and cleanup"
```

## Task 7: Add Hand Blocking And Petting

**Files:**
- Modify: `src/main/java/com/explosive/carrybabyanimals/carry/CarryInteractionHandler.java`
- Modify: `src/main/java/com/explosive/carrybabyanimals/CarryBabyAnimals.java`

- [ ] **Step 1: Add petting cooldown and particle method**

Add fields and methods to `CarryInteractionHandler`:

```java
private final java.util.Map<java.util.UUID, Long> lastPetTick = new java.util.HashMap<>();

public InteractionResult onAttack(ServerPlayer player) {
    if (!carryManager.isCarrying(player.getUUID())) {
        return InteractionResult.PASS;
    }
    long gameTime = player.serverLevel().getGameTime();
    long last = lastPetTick.getOrDefault(player.getUUID(), -20L);
    if (gameTime - last >= configManager.config().pettingCooldownTicks()) {
        lastPetTick.put(player.getUUID(), gameTime);
        carryManager.carriedEntityId(player.getUUID()).ifPresent(entityId -> {
            Entity baby = player.level().getEntity(entityId);
            if (baby != null) {
                player.serverLevel().sendParticles(
                        net.minecraft.core.particles.ParticleTypes.HEART,
                        baby.getX(),
                        baby.getY() + baby.getBbHeight() * 0.75D,
                        baby.getZ(),
                        5,
                        0.25D,
                        0.25D,
                        0.25D,
                        0.0D
                );
            }
        });
    }
    return InteractionResult.SUCCESS;
}

public InteractionResult onUseWhileCarrying(ServerPlayer player) {
    return carryManager.isCarrying(player.getUUID()) ? InteractionResult.FAIL : InteractionResult.PASS;
}
```

- [ ] **Step 2: Register attack and use blockers**

In `CarryBabyAnimals.onInitialize()`, register attack/use callbacks:

```java
AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
    if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
        return InteractionResult.PASS;
    }
    return INTERACTIONS.onAttack(serverPlayer);
});

UseItemCallback.EVENT.register((player, world, hand) -> {
    if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
        return InteractionResult.PASS;
    }
    return INTERACTIONS.onUseWhileCarrying(serverPlayer);
});

UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
    if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
        return InteractionResult.PASS;
    }
    return INTERACTIONS.onUseWhileCarrying(serverPlayer);
});
```

Add imports:

```java
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
```

- [ ] **Step 3: Verify petting does not attack**

Run a local client/server test from the IDE or Gradle run config:

```powershell
.\gradlew.bat runClient
```

Expected manual result:

- Sneak-right-click a baby animal with empty hands picks it up.
- Left-click while carrying does not damage entities.
- Heart particles appear around the carried baby.
- Place/use actions are blocked while carrying.

- [ ] **Step 4: Commit**

```powershell
git add src/main/java/com/explosive/carrybabyanimals
git commit -m "feat: add petting and hand blocking"
```

## Task 8: Add Client Carry Sync, Render Suppression, And Held Render

**Files:**
- Create: `src/main/java/com/explosive/carrybabyanimals/network/CarryNetworking.java`
- Create: `src/client/java/com/explosive/carrybabyanimals/client/render/CarriedBabyRenderState.java`
- Create: `src/client/java/com/explosive/carrybabyanimals/client/render/CarriedBabyRenderer.java`
- Create: `src/client/java/com/explosive/carrybabyanimals/client/mixin/LivingEntityRendererMixin.java`
- Create: `src/client/resources/carrybabyanimals.client.mixins.json`
- Modify: `src/client/java/com/explosive/carrybabyanimals/client/CarryBabyAnimalsClient.java`
- Modify: `src/main/java/com/explosive/carrybabyanimals/CarryBabyAnimals.java`

- [ ] **Step 1: Add client render state**

Create `src/client/java/com/explosive/carrybabyanimals/client/render/CarriedBabyRenderState.java`:

```java
package com.explosive.carrybabyanimals.client.render;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class CarriedBabyRenderState {
    private static final Map<Integer, Integer> CARRIER_BY_BABY = new ConcurrentHashMap<>();

    private CarriedBabyRenderState() {
    }

    public static void setCarried(int babyEntityId, int carrierEntityId) {
        CARRIER_BY_BABY.put(babyEntityId, carrierEntityId);
    }

    public static void clear(int babyEntityId) {
        CARRIER_BY_BABY.remove(babyEntityId);
    }

    public static boolean isCarriedBaby(int babyEntityId) {
        return CARRIER_BY_BABY.containsKey(babyEntityId);
    }

    public static Optional<Integer> carrierFor(int babyEntityId) {
        return Optional.ofNullable(CARRIER_BY_BABY.get(babyEntityId));
    }
}
```

- [ ] **Step 2: Add networking payloads**

Create `src/main/java/com/explosive/carrybabyanimals/network/CarryNetworking.java`:

```java
package com.explosive.carrybabyanimals.network;

import com.explosive.carrybabyanimals.CarryBabyAnimals;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class CarryNetworking {
    public static final ResourceLocation SET_CARRIED = ResourceLocation.fromNamespaceAndPath(CarryBabyAnimals.MOD_ID, "set_carried");
    public static final ResourceLocation CLEAR_CARRIED = ResourceLocation.fromNamespaceAndPath(CarryBabyAnimals.MOD_ID, "clear_carried");

    private CarryNetworking() {
    }

    public record SetCarriedPayload(int babyEntityId, int carrierEntityId) implements CustomPacketPayload {
        public static final Type<SetCarriedPayload> TYPE = new Type<>(SET_CARRIED);
        public static final StreamCodec<RegistryFriendlyByteBuf, SetCarriedPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                SetCarriedPayload::babyEntityId,
                ByteBufCodecs.VAR_INT,
                SetCarriedPayload::carrierEntityId,
                SetCarriedPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ClearCarriedPayload(int babyEntityId) implements CustomPacketPayload {
        public static final Type<ClearCarriedPayload> TYPE = new Type<>(CLEAR_CARRIED);
        public static final StreamCodec<RegistryFriendlyByteBuf, ClearCarriedPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                ClearCarriedPayload::babyEntityId,
                ClearCarriedPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
```

- [ ] **Step 3: Send sync packets on pickup/drop**

In `CarryBabyAnimals.onInitialize()`, register server-to-client payload codecs before gameplay events:

```java
PayloadTypeRegistry.playS2C().register(CarryNetworking.SetCarriedPayload.TYPE, CarryNetworking.SetCarriedPayload.CODEC);
PayloadTypeRegistry.playS2C().register(CarryNetworking.ClearCarriedPayload.TYPE, CarryNetworking.ClearCarriedPayload.CODEC);
```

Add imports:

```java
import com.explosive.carrybabyanimals.network.CarryNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
```

In `CarryInteractionHandler`, after successful pickup send `SET_CARRIED` to tracking players:

```java
private void syncPickup(ServerPlayer carrier, Entity baby) {
    CarryNetworking.SetCarriedPayload payload = new CarryNetworking.SetCarriedPayload(baby.getId(), carrier.getId());
    ServerPlayNetworking.send(carrier, payload);
    PlayerLookup.tracking(baby).forEach(player -> ServerPlayNetworking.send(player, payload));
}
```

On drop send `CLEAR_CARRIED`:

```java
private void syncDrop(ServerPlayer carrier, Entity baby) {
    CarryNetworking.ClearCarriedPayload payload = new CarryNetworking.ClearCarriedPayload(baby.getId());
    ServerPlayNetworking.send(carrier, payload);
    PlayerLookup.tracking(baby).forEach(player -> ServerPlayNetworking.send(player, payload));
}
```

Add imports:

```java
import com.explosive.carrybabyanimals.network.CarryNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
```

- [ ] **Step 4: Add client packet handlers**

Modify `src/client/java/com/explosive/carrybabyanimals/client/CarryBabyAnimalsClient.java`:

```java
package com.explosive.carrybabyanimals.client;

import com.explosive.carrybabyanimals.client.render.CarriedBabyRenderState;
import com.explosive.carrybabyanimals.network.CarryNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class CarryBabyAnimalsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        registerCarryPacketHandlers();
    }

    private void registerCarryPacketHandlers() {
        PayloadTypeRegistry.playS2C().register(CarryNetworking.SetCarriedPayload.TYPE, CarryNetworking.SetCarriedPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CarryNetworking.ClearCarriedPayload.TYPE, CarryNetworking.ClearCarriedPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(CarryNetworking.SetCarriedPayload.TYPE, (payload, context) ->
                context.client().execute(() -> CarriedBabyRenderState.setCarried(payload.babyEntityId(), payload.carrierEntityId()))
        );
        ClientPlayNetworking.registerGlobalReceiver(CarryNetworking.ClearCarriedPayload.TYPE, (payload, context) ->
                context.client().execute(() -> CarriedBabyRenderState.clear(payload.babyEntityId()))
        );
    }
}
```

- [ ] **Step 5: Add vanilla render suppressor mixin**

Create `src/client/resources/carrybabyanimals.client.mixins.json`:

```json
{
  "required": true,
  "package": "com.explosive.carrybabyanimals.client.mixin",
  "compatibilityLevel": "JAVA_25",
  "client": [
    "LivingEntityRendererMixin"
  ],
  "injectors": {
    "defaultRequire": 1
  }
}
```

Create `src/client/java/com/explosive/carrybabyanimals/client/mixin/LivingEntityRendererMixin.java`:

```java
package com.explosive.carrybabyanimals.client.mixin;

import com.explosive.carrybabyanimals.client.render.CarriedBabyRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
abstract class LivingEntityRendererMixin<T extends LivingEntity> {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void carrybabyanimals$suppressCarriedBabyRender(T entity, CallbackInfo ci) {
        if (CarriedBabyRenderState.isCarriedBaby(entity.getId())) {
            ci.cancel();
        }
    }
}
```

- [ ] **Step 6: Add held renderer**

Create `src/client/java/com/explosive/carrybabyanimals/client/render/CarriedBabyRenderer.java`:

```java
package com.explosive.carrybabyanimals.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PoseStack;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

public final class CarriedBabyRenderer {
    private final Minecraft minecraft = Minecraft.getInstance();

    public void renderCarriedBaby(Entity baby, PoseStack poseStack, float tickDelta, MultiBufferSource buffers, int light) {
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        EntityRenderer<? super Entity, ? extends EntityRenderState> renderer = dispatcher.getRenderer(baby);
        if (renderer == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.45D, 0.85D, -0.35D);
        dispatcher.render(baby, 0.0D, 0.0D, 0.0D, baby.getYRot(), tickDelta, poseStack, buffers, light);
        poseStack.popPose();
    }
}
```

- [ ] **Step 7: Run client**

Run:

```powershell
.\gradlew.bat runClient
```

Expected manual result:

- Vanilla passenger position is suppressed on the modded client.
- The carried baby appears once at the held-in-hands position.
- A second vanilla client or unmodded view would still see the passenger fallback above the player.

- [ ] **Step 8: Commit**

```powershell
git add src/main/java/com/explosive/carrybabyanimals/network src/client src/main/java/com/explosive/carrybabyanimals docs/implementation-notes/2026-05-17-fabric-26-api-surfaces.md
git commit -m "feat: add carried baby client rendering"
```

## Task 9: Add Config Loading, Unknown Name Logging, And Reload Permission

**Files:**
- Modify: `src/main/java/com/explosive/carrybabyanimals/config/CarryConfigManager.java`
- Modify: `src/main/java/com/explosive/carrybabyanimals/CarryBabyAnimals.java`

- [ ] **Step 1: Load config on startup**

In `CarryBabyAnimals.onInitialize()`, load config from Fabric config dir:

```java
Path configPath = FabricLoader.getInstance()
        .getConfigDir()
        .resolve(MOD_ID + ".json");
try {
    CONFIG.load(configPath);
} catch (IOException exception) {
    LOGGER.error("Failed to load Carry Baby Animals config from {}", configPath, exception);
}
```

Add imports:

```java
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Path;
```

- [ ] **Step 2: Log unknown config names**

Add validation method to `CarryConfigManager`:

```java
public void logUnknownAnimalNames(AnimalAliasRegistry aliases, org.slf4j.Logger logger) {
    for (String name : config.allowedAnimals()) {
        if (aliases.resolve(name).isEmpty()) {
            logger.warn("Unknown allowed animal name in Carry Baby Animals config: {}", name);
        }
    }
    for (String name : config.blockedAnimals()) {
        if (aliases.resolve(name).isEmpty()) {
            logger.warn("Unknown blocked animal name in Carry Baby Animals config: {}", name);
        }
    }
}
```

Call it after config load:

```java
AnimalAliasRegistry aliases = AnimalAliasRegistry.createDefault();
CONFIG.logUnknownAnimalNames(aliases, LOGGER);
```

- [ ] **Step 3: Run build**

Run:

```powershell
.\gradlew.bat build
```

Expected: BUILD SUCCESSFUL and no config warnings with the default config.

- [ ] **Step 4: Commit**

```powershell
git add src/main/java/com/explosive/carrybabyanimals
git commit -m "feat: load and validate carry config"
```

## Task 10: Manual Verification Matrix And Release Readiness

**Files:**
- Create: `docs/manual-test-plan.md`

- [ ] **Step 1: Write manual test plan**

Create `docs/manual-test-plan.md`:

```markdown
# CarryBabyAnimals Manual Test Plan

## Singleplayer Modded Client

- Baby cow can be picked up by sneak-right-clicking with empty hands.
- Sneak-right-click while carrying drops the baby safely in front of the player.
- Sneak-right-clicking another baby while already carrying drops the current baby first only when the click targets empty/world; entity-targeted pickup of a new baby is ignored until the current baby is dropped.
- Running, jumping, and swimming work while carrying.
- Left-click while carrying spawns heart particles and does not attack.
- Placing blocks, eating, bows, and use-item actions are blocked while carrying.
- The baby drops when it grows up.

## Multiplayer With Modded Clients

- Other modded players see the baby held in the carrier's hands.
- Other modded players do not see a duplicate baby above the carrier's head.
- Heart particles are visible to nearby players.

## Multiplayer With Vanilla-Compatible Client View

- The server keeps the baby as a passenger of the carrier.
- A vanilla client sees the baby above the player's head.
- The passenger fallback remains visible and does not duplicate the animal.

## Config

- `allowedAnimals` limits pickup to named animals.
- `blockedAnimals` removes named animals from the default set.
- `dog` applies to tamed wolves only.
- `wolf` applies to all wolves, subject to tamed ownership rules.
- Unknown config names log warnings and do not crash startup.

## Permissions

- Carrying is allowed by default with no permission provider.
- `carrybabyanimals.carry=false` blocks pickup.
- `carrybabyanimals.carry.tamed=false` blocks carrying owned tamed babies.
- `carrybabyanimals.carry.others_tamed=true` plus config allows carrying another player's tamed baby.
```

- [ ] **Step 2: Run final checks**

Run:

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runClient
```

Expected:

- Tests PASS.
- Build SUCCESSFUL.
- Manual run confirms pickup, passenger fallback, held render, render suppression, petting, blocking, drop, config, and permissions.

- [ ] **Step 3: Commit**

```powershell
git add docs/manual-test-plan.md
git commit -m "docs: add manual test plan"
```

## Self-Review

Spec coverage:

- Passenger fallback: Task 5 implements passenger attachment; Task 8 keeps it visible for vanilla clients and suppressed only on modded clients.
- AI suppression: Task 5 adds `CarryAiController`.
- Render suppression: Task 8 adds `LivingEntityRendererMixin` and replacement renderer.
- Left-click petting: Task 7 intercepts attacks and sends server-side hearts.
- One-at-a-time: Task 4 enforces carry state; Task 5 ignores new pickup while already carrying.
- Config: Task 3 and Task 9 implement JSON, friendly names, `dog`/`wolf`, and unknown-name logging.
- Permissions: Task 4 adds Fabric Permissions API wrapper.
- Growth handling: Task 6 tick-checks `Animal#isBaby()` and Task 2 records event availability.
- Testing: Tasks 3, 4, 7, 8, and 10 cover automated and manual verification.

Placeholder scan:

- The plan contains no banned empty-detail tokens or deferred feature buckets.
- API reconnaissance is explicit work in Task 2 because Fabric 26.x hook names and render-state APIs must be verified against generated sources before code is committed.

Type consistency:

- `CarryManager`, `CarryState`, `CarryInteractionHandler`, `CarryAttachment`, `CarryAiController`, and `CarryTicker` use the same player UUID and entity id model.
- Config names match the spec: `allowedAnimals`, `blockedAnimals`, `allowCarryingOtherPlayersTamedAnimals`, and `pettingCooldownTicks`.
