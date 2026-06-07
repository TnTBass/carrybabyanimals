# CarryBabyAnimals Multiloader Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete Phase 1 of the CarryBabyAnimals multiloader refactor by extracting loader-neutral common/platform boundaries and moving the repo into a physical multiloader-shaped Fabric-preserving Gradle/source layout.

**Architecture:** Keep this as one Gradle project using Fabric Loom, with explicit `commonMain`, `commonClient`, `fabricMain`, and `fabricClient` source sets wired into the single Fabric mod output. Common source sets own behavior and payload semantics; Fabric source sets own Fabric entrypoints, resources, mixins, networking transport, optional Fabric integrations, config paths, metadata lookup, and render hook registration.

**Tech Stack:** Java 25, Gradle, Fabric Loom `1.16-SNAPSHOT`, Minecraft `26.1.2`, Fabric Loader/API, JUnit 5, embedded ModStatusKit helpers, ModMenu compile-only integration, optional Fabric Permissions API.

---

## Scope Boundaries

This plan covers Phase 1 only.

Phase 1 includes:

- Fabric-preserving common/platform boundary extraction.
- Physical multiloader-shaped Gradle/source layout.
- A single release-ready Fabric jar with existing Fabric behavior preserved.
- Static scans proving common source sets do not import Fabric, ModMenu, Fabric Permissions API, or NeoForge APIs.
- Automated and manual verification before any Phase 2 work.

Phase 1 excludes:

- NeoForge dependencies, metadata, entrypoints, resources, mixins, or jar output.
- A universal jar.
- A reusable extracted library.
- Player-facing behavior changes.
- Public `CHANGELOG.md` entries unless implementation discovers an actual player/admin behavior change.

## Target Layout

Create this source shape during Phase 1:

```text
src/
  common/
    AGENTS.md
    java/dev/jasmine/carrybabyanimals/...
  commonClient/
    AGENTS.md
    java/dev/jasmine/carrybabyanimals/client/...
  fabric/
    AGENTS.md
    java/dev/jasmine/carrybabyanimals/fabric/...
    resources/fabric.mod.json
    resources/carrybabyanimals.mixins.json
    resources/assets/carrybabyanimals/icon_128.png
  fabricClient/
    AGENTS.md
    java/dev/jasmine/carrybabyanimals/fabric/client/...
    resources/carrybabyanimals.client.mixins.json
  test/
    java/dev/jasmine/carrybabyanimals/...
```

Keep `src/test/java` as the shared JUnit test source set. Tests should import common packages directly and Fabric adapter packages only in adapter-specific tests.

Use one Gradle project and configure Loom to package `fabricMain` and `fabricClient`:

```gradle
sourceSets {
    commonMain {
        java.srcDir "src/common/java"
    }
    commonClient {
        java.srcDir "src/commonClient/java"
        compileClasspath += sourceSets.commonMain.output
        runtimeClasspath += sourceSets.commonMain.output
    }
    fabricMain {
        java.srcDir "src/fabric/java"
        resources.srcDir "src/fabric/resources"
        compileClasspath += sourceSets.commonMain.output
        runtimeClasspath += sourceSets.commonMain.output
    }
    fabricClient {
        java.srcDir "src/fabricClient/java"
        resources.srcDir "src/fabricClient/resources"
        compileClasspath += sourceSets.commonMain.output + sourceSets.commonClient.output + sourceSets.fabricMain.output
        runtimeClasspath += sourceSets.commonMain.output + sourceSets.commonClient.output + sourceSets.fabricMain.output
    }
    test {
        compileClasspath += sourceSets.commonMain.output + sourceSets.commonClient.output + sourceSets.fabricMain.output + sourceSets.fabricClient.output
        runtimeClasspath += sourceSets.commonMain.output + sourceSets.commonClient.output + sourceSets.fabricMain.output + sourceSets.fabricClient.output
    }
}

loom {
    splitEnvironmentSourceSets()

    mods {
        "carrybabyanimals" {
            sourceSet sourceSets.commonMain
            sourceSet sourceSets.commonClient
            sourceSet sourceSets.fabricMain
            sourceSet sourceSets.fabricClient
        }
    }
}

tasks.named("compileCommonClientJava") {
    dependsOn tasks.named("compileCommonMainJava")
}

tasks.named("compileFabricMainJava") {
    dependsOn tasks.named("compileCommonMainJava")
}

tasks.named("compileFabricClientJava") {
    dependsOn tasks.named("compileCommonMainJava")
    dependsOn tasks.named("compileCommonClientJava")
    dependsOn tasks.named("compileFabricMainJava")
}
```

Adjust task names if Gradle source-set task names differ, but preserve the four-source-set ownership above. Do not create Gradle subprojects in Phase 1.

## Package Ownership

Common main owns:

- `dev.jasmine.carrybabyanimals.CarryBabyAnimalsCore`
- `dev.jasmine.carrybabyanimals.BuildInfo`
- `dev.jasmine.carrybabyanimals.carry`
- `dev.jasmine.carrybabyanimals.config`
- `dev.jasmine.carrybabyanimals.cozy`
- `dev.jasmine.carrybabyanimals.internal.modstatus`
- `dev.jasmine.carrybabyanimals.modstatus` except Fabric metadata lookup
- `dev.jasmine.carrybabyanimals.network` payload contracts and send intents only
- `dev.jasmine.carrybabyanimals.nursery`
- `dev.jasmine.carrybabyanimals.permissions` permission nodes/default decisions only
- `dev.jasmine.carrybabyanimals.platform` narrow interfaces
- `dev.jasmine.carrybabyanimals.reunion`

Common client owns:

- `dev.jasmine.carrybabyanimals.client.config` except Fabric config path lookup and ModMenu entrypoint
- `dev.jasmine.carrybabyanimals.client.modstatus`
- `dev.jasmine.carrybabyanimals.client.render` render math, visual frames, size buckets, reactions, sleepy visual phases, first-person visibility, and loader-neutral render bookkeeping
- `dev.jasmine.carrybabyanimals.client.ClientCarryInteractionIntent` or equivalent no-Fabric intent helper

Fabric main owns:

- `dev.jasmine.carrybabyanimals.fabric.CarryBabyAnimalsFabric`
- `dev.jasmine.carrybabyanimals.fabric.event`
- `dev.jasmine.carrybabyanimals.fabric.network`
- `dev.jasmine.carrybabyanimals.fabric.permissions`
- `dev.jasmine.carrybabyanimals.fabric.platform`
- `dev.jasmine.carrybabyanimals.fabric.mixin`
- `src/fabric/resources/fabric.mod.json`
- `src/fabric/resources/carrybabyanimals.mixins.json`
- `src/fabric/resources/assets/carrybabyanimals/icon_128.png`

Fabric client owns:

- `dev.jasmine.carrybabyanimals.fabric.client.CarryBabyAnimalsFabricClient`
- `dev.jasmine.carrybabyanimals.fabric.client.config.CarryBabyAnimalsModMenuIntegration`
- `dev.jasmine.carrybabyanimals.fabric.client.network`
- `dev.jasmine.carrybabyanimals.fabric.client.platform`
- `dev.jasmine.carrybabyanimals.fabric.client.render`
- `dev.jasmine.carrybabyanimals.fabric.client.mixin`
- `src/fabricClient/resources/carrybabyanimals.client.mixins.json`

## Scoped AGENTS.md Files

Add these files when their source roots are created.

`src/common/AGENTS.md`:

```markdown
# Agent Notes for Common Code

This source root is loader-neutral CarryBabyAnimals code.

- Do not import Fabric, ModMenu, Fabric Permissions API, NeoForge, or future loader APIs here.
- Platform behavior must enter through narrow interfaces under `dev.jasmine.carrybabyanimals.platform`.
- Common code may use Minecraft classes only when the behavior is loader-neutral for the target Minecraft version.
- Payload semantics, config parsing, carry behavior, permissions defaults, ModStatus display rules, and testable render math belong here.
```

`src/commonClient/AGENTS.md`:

```markdown
# Agent Notes for Common Client Code

This source root is loader-neutral client logic for CarryBabyAnimals.

- Do not import Fabric, ModMenu, Fabric Permissions API, NeoForge, or future loader APIs here.
- Keep render math, visual-frame evaluation, client config parsing, ModStatus client state, and interaction intents reusable.
- Loader callback registration, networking send/receive calls, config path lookup, render events, render-state keys, and optional config-entry integrations belong in loader adapter roots.
```

`src/fabric/AGENTS.md`:

```markdown
# Agent Notes for Fabric Main Adapter

This source root owns Fabric server/common adapter code and Fabric jar resources.

- Fabric entrypoints, lifecycle events, payload registration, recipient discovery, config path lookup, metadata lookup, Fabric Permissions API checks, mixin classes, mixin JSON, `fabric.mod.json`, and Fabric assets belong here.
- Keep Fabric-specific code small and delegate behavior to common services.
- Do not add NeoForge code or dependencies here.
```

`src/fabricClient/AGENTS.md`:

```markdown
# Agent Notes for Fabric Client Adapter

This source root owns Fabric client adapter code.

- Fabric client entrypoints, client networking receivers, ModMenu integration, client config path lookup, render hook registration, Fabric render-state keys, client mixins, and client mixin JSON belong here.
- Keep Fabric-specific code small and delegate render math, config state, and ModStatus display behavior to common client code.
- Do not add NeoForge code or dependencies here.
```

Do not add a NeoForge `AGENTS.md` in Phase 1 because no NeoForge root should exist yet.

## Implementation Tasks

### Task 1: Establish Baseline and Layout Guardrails

**Files:**

- Modify: `build.gradle`
- Create: `src/common/AGENTS.md`
- Create: `src/commonClient/AGENTS.md`
- Create: `src/fabric/AGENTS.md`
- Create: `src/fabricClient/AGENTS.md`
- Modify: `INTERNAL_CHANGELOG.md`

- [ ] **Step 1: Verify repository root**

Run:

```powershell
git rev-parse --show-toplevel
```

Expected: `C:/Users/tyler/AI Projects/CarryBabyAnimals` or the same absolute path with backslashes.

- [ ] **Step 2: Inspect current staged and unstaged work**

Run:

```powershell
git status --short
```

Expected: existing design/spec/migration packet work is preserved. Do not revert existing staged docs.

- [ ] **Step 3: Add the four scoped `AGENTS.md` files**

Use the exact file contents from the "Scoped AGENTS.md Files" section.

- [ ] **Step 4: Add source sets without moving Java yet**

Modify `build.gradle` so `commonMain`, `commonClient`, `fabricMain`, and `fabricClient` exist, but leave the current `src/main` and `src/client` source sets temporarily in the build for this task only. This lets the layout compile before file movement.

Required Gradle intent:

```gradle
sourceSets {
    commonMain {
        java.srcDir "src/common/java"
    }
    commonClient {
        java.srcDir "src/commonClient/java"
        compileClasspath += sourceSets.commonMain.output
        runtimeClasspath += sourceSets.commonMain.output
    }
    fabricMain {
        java.srcDir "src/fabric/java"
        resources.srcDir "src/fabric/resources"
        compileClasspath += sourceSets.commonMain.output
        runtimeClasspath += sourceSets.commonMain.output
    }
    fabricClient {
        java.srcDir "src/fabricClient/java"
        resources.srcDir "src/fabricClient/resources"
        compileClasspath += sourceSets.commonMain.output + sourceSets.commonClient.output + sourceSets.fabricMain.output
        runtimeClasspath += sourceSets.commonMain.output + sourceSets.commonClient.output + sourceSets.fabricMain.output
    }
    main {
        java.srcDir "src/main/java"
        java.srcDir generatedBuildInfoDir
    }
}
```

Keep the existing `loom { splitEnvironmentSourceSets(); mods { ... sourceSets.main ... sourceSets.client ... } }` unchanged until source movement starts.
During this transitional task, `BuildInfo` remains owned only by `main`; `commonMain` starts owning generated `BuildInfo` in Task 2.

- [ ] **Step 5: Run baseline tests**

Run:

```powershell
.\gradlew.bat test
```

Expected: PASS.

- [ ] **Step 6: Run baseline build**

Run:

```powershell
.\gradlew.bat build
```

Expected: PASS and produces the current Fabric jar.

- [ ] **Step 7: Run changelog gate**

Run:

```powershell
.\gradlew.bat checkChangelog
```

Expected: PASS. This Phase 1 planning/build-layout work is maintainer-only unless implementation changes player behavior, so record it in `INTERNAL_CHANGELOG.md`.

- [ ] **Step 8: Commit the guardrail slice**

Run:

```powershell
git add build.gradle src/common/AGENTS.md src/commonClient/AGENTS.md src/fabric/AGENTS.md src/fabricClient/AGENTS.md INTERNAL_CHANGELOG.md
git commit -m "chore: add multiloader layout guardrails"
```

### Task 2: Move Loader-Neutral Main Code to `commonMain`

**Files:**

- Move: most files from `src/main/java/dev/jasmine/carrybabyanimals/` to `src/common/java/dev/jasmine/carrybabyanimals/`
- Keep Fabric-owned for later movement: `CarryBabyAnimals.java`, `mixin/EntityStartRidingMixin.java`, `network/CarryNetworking.java`, `permissions/CarryPermissions.java`, `modstatus/CarryBabyAnimalsModStatus.java`
- Modify: `build.gradle`

- [ ] **Step 1: Move pure main packages**

Move these directories into `src/common/java/dev/jasmine/carrybabyanimals/`:

```text
carry
config
cozy
internal
nursery
reunion
```

Move only pure files from `modstatus`, `network`, and `permissions` after creating common replacements in later tasks.

- [ ] **Step 2: Move generated `BuildInfo` output to `commonMain`**

Keep the generated file package as `dev.jasmine.carrybabyanimals.BuildInfo`, but make `compileCommonMainJava` depend on `generateBuildInfo`:

```gradle
sourceSets {
    commonMain {
        java.srcDirs = ["src/common/java", generatedBuildInfoDir]
    }
    main {
        java.srcDirs = ["src/main/java"]
    }
}

tasks.named("compileCommonMainJava") {
    dependsOn tasks.named("generateBuildInfo")
}

tasks.named("sourcesJar") {
    dependsOn tasks.named("generateBuildInfo")
}
```

Remove the old `compileJava` dependency only after the old `main` source set no longer owns generated common classes. This step must remove `generatedBuildInfoDir` from `main.srcDirs` before any jar assembly so `BuildInfo.class` is not compiled by both `main` and `commonMain`.

- [ ] **Step 3: Wire `main` to depend on `commonMain` while Fabric entrypoint remains in old `src/main`**

```gradle
sourceSets {
    main {
        compileClasspath += sourceSets.commonMain.output
        runtimeClasspath += sourceSets.commonMain.output
    }
    test {
        compileClasspath += sourceSets.commonMain.output
        runtimeClasspath += sourceSets.commonMain.output
    }
}
```

- [ ] **Step 4: Run common import scan**

Run:

```powershell
rg -n "net\.fabricmc|fabric\.api|com\.terraformersmc\.modmenu|me\.lucko\.fabric|net\.neoforged|neoforge" src/common src/commonClient
```

Expected: no matches.

- [ ] **Step 5: Run unit tests**

Run:

```powershell
.\gradlew.bat test
```

Expected: PASS.

- [ ] **Step 6: Commit the common-main move**

Run:

```powershell
git add build.gradle src/common src/main src/test
git commit -m "refactor: move loader-neutral main logic to common source"
```

### Task 3: Split Packet Semantics from Fabric Networking

**Files:**

- Create: `src/common/java/dev/jasmine/carrybabyanimals/network/CarryPayloads.java`
- Create: `src/common/java/dev/jasmine/carrybabyanimals/network/CarryPacketChannels.java`
- Create: `src/common/java/dev/jasmine/carrybabyanimals/network/CarryNetworkIntents.java`
- Move/modify: `src/main/java/dev/jasmine/carrybabyanimals/network/CarryNetworking.java` to `src/fabric/java/dev/jasmine/carrybabyanimals/fabric/network/FabricCarryNetworking.java`
- Modify: `src/test/java/dev/jasmine/carrybabyanimals/network/CarryNetworkingTest.java`

- [ ] **Step 1: Add common channel constants**

Create `CarryPacketChannels`:

```java
package dev.jasmine.carrybabyanimals.network;

public final class CarryPacketChannels {
    public static final String SET_CARRIED = "set_carried";
    public static final String CLEAR_CARRIED = "clear_carried";
    public static final String PET_CARRIED = "pet_carried";
    public static final String PET_FEEDBACK = "pet_feedback";
    public static final String SERVER_VERSION = "server_version";

    private CarryPacketChannels() {
    }
}
```

- [ ] **Step 2: Add common payload records**

Create `CarryPayloads`:

```java
package dev.jasmine.carrybabyanimals.network;

import dev.jasmine.carrybabyanimals.internal.modstatus.ModStatusServerStatus;
import dev.jasmine.carrybabyanimals.internal.modstatus.ModStatusVersionPayload;
import java.util.Arrays;

public final class CarryPayloads {
    private CarryPayloads() {
    }

    public record SetCarried(int babyEntityId, int carrierEntityId) {
    }

    public record ClearCarried(int babyEntityId) {
    }

    public record PetCarried() {
        public static final PetCarried INSTANCE = new PetCarried();
    }

    public record PetFeedback(int babyEntityId) {
    }

    public record ServerVersion(byte[] encodedVersion) {
        public static final int MAX_ENCODED_BYTES = 256;

        public ServerVersion {
            encodedVersion = Arrays.copyOf(encodedVersion, encodedVersion.length);
        }

        public ServerVersion(String serverVersion) {
            this(ModStatusVersionPayload.encodeServerVersion(serverVersion));
        }

        @Override
        public byte[] encodedVersion() {
            return Arrays.copyOf(encodedVersion, encodedVersion.length);
        }

        public String serverVersion() {
            return ModStatusVersionPayload.decodeServerVersion(encodedVersion);
        }

        public ModStatusServerStatus serverStatus() {
            return ModStatusVersionPayload.decodeServerStatus(encodedVersion);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ServerVersion that
                    && Arrays.equals(encodedVersion, that.encodedVersion);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(encodedVersion);
        }
    }
}
```

- [ ] **Step 3: Add common network intents**

Create `CarryNetworkIntents`:

```java
package dev.jasmine.carrybabyanimals.network;

import java.util.Arrays;

public sealed interface CarryNetworkIntents {
    record SetCarriedToCarrierAndTracking(int babyEntityId, int carrierEntityId) implements CarryNetworkIntents {
    }

    /**
     * recipientEntityId is the ServerPlayer entity ID for the specific stale-clear recipient.
     */
    record ClearCarriedForRecipient(int babyEntityId, int recipientEntityId) implements CarryNetworkIntents {
    }

    record PetFeedbackToCarrier(int babyEntityId, int carrierEntityId) implements CarryNetworkIntents {
    }

    record ServerVersionToRecipient(byte[] encodedVersion, int recipientEntityId) implements CarryNetworkIntents {
        public ServerVersionToRecipient {
            encodedVersion = Arrays.copyOf(encodedVersion, encodedVersion.length);
        }

        @Override
        public byte[] encodedVersion() {
            return Arrays.copyOf(encodedVersion, encodedVersion.length);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ServerVersionToRecipient that
                    && recipientEntityId == that.recipientEntityId
                    && Arrays.equals(encodedVersion, that.encodedVersion);
        }

        @Override
        public int hashCode() {
            int result = Arrays.hashCode(encodedVersion);
            result = 31 * result + Integer.hashCode(recipientEntityId);
            return result;
        }
    }

    record PetCarriedToServer() implements CarryNetworkIntents {
        public static final PetCarriedToServer INSTANCE = new PetCarriedToServer();
    }
}
```

These records describe transport-independent send intentions only. Fabric transport still owns recipient discovery, support checks, codecs, and actual sends.

- [ ] **Step 4: Add common packet and send-intent tests**

Modify `CarryNetworkingTest` to assert:

```java
assertEquals("set_carried", CarryPacketChannels.SET_CARRIED);
assertEquals("clear_carried", CarryPacketChannels.CLEAR_CARRIED);
assertEquals("pet_carried", CarryPacketChannels.PET_CARRIED);
assertEquals("pet_feedback", CarryPacketChannels.PET_FEEDBACK);
assertEquals("server_version", CarryPacketChannels.SERVER_VERSION);
assertEquals(256, CarryPayloads.ServerVersion.MAX_ENCODED_BYTES);
assertEquals(new CarryPayloads.ServerVersion(new byte[] {1, 2}), new CarryPayloads.ServerVersion(new byte[] {1, 2}));
assertEquals(new CarryNetworkIntents.SetCarriedToCarrierAndTracking(12, 34), new CarryNetworkIntents.SetCarriedToCarrierAndTracking(12, 34));
assertEquals(new CarryNetworkIntents.ClearCarriedForRecipient(12, 99).recipientEntityId(), 99);
assertEquals(new CarryNetworkIntents.ServerVersionToRecipient(new byte[] {1, 2}, 99), new CarryNetworkIntents.ServerVersionToRecipient(new byte[] {1, 2}, 99));
assertSame(CarryNetworkIntents.PetCarriedToServer.INSTANCE, CarryNetworkIntents.PetCarriedToServer.INSTANCE);
```

Keep the existing ModStatus round-trip assertions, but point them at `CarryPayloads.ServerVersion`.

- [ ] **Step 5: Move Fabric transport**

Move `CarryNetworking.java` to `FabricCarryNetworking.java` under `src/fabric/java/dev/jasmine/carrybabyanimals/fabric/network/`. Its Fabric payload records may remain Fabric-only wrappers, but they must delegate channel names and encoded server-version bytes to the common classes.

- [ ] **Step 6: Preserve capability gating and vanilla fallback**

In the Fabric transport, keep:

```java
ServerPlayNetworking.canSend(player, payload.type())
ServerPlayNetworking.send(player, payload)
PlayerLookup.tracking(...)
player.connection.send(new ClientboundSetPassengersPacket(carrier))
```

Do not move these Fabric/transport operations into common code.

- [ ] **Step 7: Run packet tests**

Run:

```powershell
.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.network.CarryNetworkingTest
```

Expected: PASS and covers channel names, payload fields, byte limit, ModStatus payload encoding, build metadata, structured WARN status, and capability-gated server-version sends.

- [ ] **Step 8: Run common import scan**

Run:

```powershell
rg -n "net\.fabricmc|fabric\.api|com\.terraformersmc\.modmenu|me\.lucko\.fabric|net\.neoforged|neoforge" src/common src/commonClient
```

Expected: no matches.

- [ ] **Step 9: Commit the networking slice**

Run:

```powershell
git add src/common src/fabric src/main src/test build.gradle
git commit -m "refactor: split payload semantics from Fabric networking"
```

### Task 4: Move Fabric Entrypoint, Events, Permissions, Paths, and Metadata Lookup

**Files:**

- Move/modify: `src/main/java/dev/jasmine/carrybabyanimals/CarryBabyAnimals.java` to `src/fabric/java/dev/jasmine/carrybabyanimals/fabric/CarryBabyAnimalsFabric.java`
- Move/modify: `src/main/java/dev/jasmine/carrybabyanimals/permissions/CarryPermissions.java` to `src/fabric/java/dev/jasmine/carrybabyanimals/fabric/permissions/FabricCarryPermissions.java`
- Move/modify: Fabric metadata lookup from `CarryBabyAnimalsModStatus.java` into `src/fabric/java/dev/jasmine/carrybabyanimals/fabric/platform/FabricModMetadata.java`
- Create: `src/common/java/dev/jasmine/carrybabyanimals/platform/PlatformPaths.java`
- Create: `src/common/java/dev/jasmine/carrybabyanimals/platform/PlatformModMetadata.java`
- Create: `src/common/java/dev/jasmine/carrybabyanimals/platform/PlatformPermissions.java`
- Create: `src/common/java/dev/jasmine/carrybabyanimals/CarryBabyAnimalsCore.java`
- Modify: `src/main/resources/fabric.mod.json`

- [ ] **Step 1: Add narrow platform interfaces**

Add:

```java
package dev.jasmine.carrybabyanimals.platform;

import java.nio.file.Path;

public interface PlatformPaths {
    Path serverConfigPath();
    Path clientConfigPath();
}
```

```java
package dev.jasmine.carrybabyanimals.platform;

public interface PlatformModMetadata {
    String currentVersion();
    String currentBuild();
}
```

```java
package dev.jasmine.carrybabyanimals.platform;

import net.minecraft.server.level.ServerPlayer;

public interface PlatformPermissions {
    boolean canCarry(ServerPlayer player);
    boolean canCarryOwnTamed(ServerPlayer player);
    boolean canCarryOthersTamed(ServerPlayer player);
    boolean canBypassNursery(ServerPlayer player);
    boolean canReload(ServerPlayer player);
}
```

- [ ] **Step 2: Extract common bootstrap**

Create `CarryBabyAnimalsCore` to own constants, service construction, and common handler fields. It must not import Fabric.

Required constants:

```java
public static final String MOD_ID = "carrybabyanimals";
```

Keep existing logger behavior available from common code.

- [ ] **Step 3: Move Fabric permissions**

Move Fabric Loader detection and Lucko `Permissions.check(...)` calls into `FabricCarryPermissions`. Common permission node names and fallback defaults remain in common code.

- [ ] **Step 4: Move Fabric metadata lookup**

`FabricModMetadata` owns `FabricLoader.getInstance().getModContainer(...)`. Common ModStatus code receives `PlatformModMetadata` or receives version/build strings from Fabric bootstrap.

- [ ] **Step 5: Move server entrypoint**

Rename the Fabric entrypoint class to `dev.jasmine.carrybabyanimals.fabric.CarryBabyAnimalsFabric`. It owns Fabric event registration, Fabric config-path lookup, Fabric networking registration, tracking replay, connection lifecycle, and shutdown cleanup. It delegates carry behavior to common services.

- [ ] **Step 6: Update `fabric.mod.json` server entrypoint**

At this point in the sequence, edit `src/main/resources/fabric.mod.json`. Task 7 moves the already-updated resource file to `src/fabric/resources/fabric.mod.json`.

Set:

```json
"main": [
  "dev.jasmine.carrybabyanimals.fabric.CarryBabyAnimalsFabric"
]
```

Do not change mod ID, dependencies, suggestions, mixin declarations, or icon path.

- [ ] **Step 7: Run tests and scan**

Run:

```powershell
.\gradlew.bat test
rg -n "net\.fabricmc|fabric\.api|com\.terraformersmc\.modmenu|me\.lucko\.fabric|net\.neoforged|neoforge" src/common src/commonClient
```

Expected: tests pass; scan has no matches.

- [ ] **Step 8: Commit the server adapter slice**

Run:

```powershell
git add src/common src/fabric src/main src/test build.gradle src/main/resources/fabric.mod.json
git commit -m "refactor: isolate Fabric server adapter boundaries"
```

### Task 5: Move Loader-Neutral Client Code to `commonClient`

**Files:**

- Move: client config state/layout files to `src/commonClient/java/dev/jasmine/carrybabyanimals/client/config/`
- Move: client ModStatus tracker files to `src/commonClient/java/dev/jasmine/carrybabyanimals/client/modstatus/`
- Move: render math/state files to `src/commonClient/java/dev/jasmine/carrybabyanimals/client/render/`
- Keep Fabric-owned for later movement: `CarryBabyAnimalsClient.java`, `ClientCarryInteractionHandler.java` until Fabric networking send is isolated, `CarryBabyAnimalsModMenuIntegration.java`, `CarriedBabyRenderer.java` render event registration, `CarriedBabyRenderState.java` if it still imports `RenderStateDataKey`, client mixins
- Modify: client tests as needed

- [ ] **Step 1: Move pure client config files**

Move these to `src/commonClient/java`:

```text
client/config/ClientCarryVisualConfig.java
client/config/ClientCarryVisualConfigEditState.java
client/config/CarryBabyAnimalsConfigScreen.java
client/config/CarryBabyAnimalsConfigScreenLayout.java
```

Keep `ClientCarryVisualConfigManager.java` in Fabric client until config path lookup is split.

- [ ] **Step 2: Move pure client ModStatus files**

Move:

```text
client/modstatus/ClientModStatusTracker.java
```

- [ ] **Step 3: Move pure render math files**

Move:

```text
client/render/CarriedBabyPlacement.java
client/render/CarriedBabyReaction.java
client/render/CarriedBabyReactionRegistry.java
client/render/CarriedBabyReactionType.java
client/render/CarriedBabySizeBucket.java
client/render/CarriedBabySizeClassifier.java
client/render/CarriedBabySleepyVisualPhase.java
client/render/CarriedBabyVisualFrame.java
client/render/FirstPersonLargeBabyVisibilityMode.java
```

Keep Fabric render hook and Fabric render-state-key files in Fabric client until Task 7.

- [ ] **Step 4: Run focused client tests**

Run:

```powershell
.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.config.* --tests dev.jasmine.carrybabyanimals.client.modstatus.* --tests dev.jasmine.carrybabyanimals.client.render.*
```

Expected: PASS. These tests cover config edit state/layout, ModStatus state, placement, reactions, render-state behavior, size classification, sleepy visuals, and visual frames.

- [ ] **Step 5: Run common import scan**

Run:

```powershell
rg -n "net\.fabricmc|fabric\.api|com\.terraformersmc\.modmenu|me\.lucko\.fabric|net\.neoforged|neoforge" src/common src/commonClient
```

Expected: no matches. If `CarriedBabyRenderState.java` imports Fabric `RenderStateDataKey`, leave it in Fabric client and extract only the loader-neutral state data first.

- [ ] **Step 6: Commit the common-client move**

Run:

```powershell
git add src/commonClient src/client src/test build.gradle
git commit -m "refactor: move loader-neutral client logic to common client source"
```

### Task 6: Move Fabric Client Entrypoint, Networking, Config Path, and ModMenu

**Files:**

- Move/modify: `src/client/java/dev/jasmine/carrybabyanimals/client/CarryBabyAnimalsClient.java` to `src/fabricClient/java/dev/jasmine/carrybabyanimals/fabric/client/CarryBabyAnimalsFabricClient.java`
- Move/modify: `src/client/java/dev/jasmine/carrybabyanimals/client/ClientCarryInteractionHandler.java` to Fabric client or split intent from send transport
- Move/modify: `ClientCarryVisualConfigManager.java` path lookup into Fabric client adapter
- Move: `CarryBabyAnimalsModMenuIntegration.java` to `src/fabricClient/java/dev/jasmine/carrybabyanimals/fabric/client/config/`
- Modify: `src/main/resources/fabric.mod.json`

- [ ] **Step 1: Split client carry interaction intent from Fabric send**

Common client owns "pet carried requested" intent logic. Fabric client owns:

```java
ClientPlayNetworking.canSend(FabricCarryNetworking.PetCarriedPayload.TYPE)
ClientPlayNetworking.send(FabricCarryNetworking.PetCarriedPayload.INSTANCE)
```

- [ ] **Step 2: Split client config path lookup**

Common client config manager accepts a `Path`. Fabric client supplies:

```java
FabricLoader.getInstance().getConfigDir().resolve("carrybabyanimals-client.json")
```

- [ ] **Step 3: Move ModMenu integration**

Move `CarryBabyAnimalsModMenuIntegration` to the Fabric client adapter package. It continues to implement `ModMenuApi` and return the same config screen factory.

- [ ] **Step 4: Move client entrypoint**

Rename the Fabric client entrypoint to `dev.jasmine.carrybabyanimals.fabric.client.CarryBabyAnimalsFabricClient`. It owns Fabric client global receivers, client tick, connection lifecycle, pre-attack hook, and render registration.

- [ ] **Step 5: Update `fabric.mod.json` client and ModMenu entrypoints**

At this point in the sequence, edit `src/main/resources/fabric.mod.json`. Task 7 moves the already-updated resource file to `src/fabric/resources/fabric.mod.json`.

Set:

```json
"client": [
  "dev.jasmine.carrybabyanimals.fabric.client.CarryBabyAnimalsFabricClient"
],
"modmenu": [
  "dev.jasmine.carrybabyanimals.fabric.client.config.CarryBabyAnimalsModMenuIntegration"
]
```

- [ ] **Step 6: Verify optional integrations remain optional**

Run:

```powershell
.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.config.*
.\gradlew.bat build
```

Expected: PASS. Absence of ModMenu remains allowed by `fabric.mod.json` suggestions and compile-only dependency, not by hard runtime requirement.

- [ ] **Step 7: Commit the Fabric client adapter slice**

Run:

```powershell
git add src/commonClient src/fabricClient src/client src/main/resources/fabric.mod.json src/test build.gradle
git commit -m "refactor: isolate Fabric client adapter boundaries"
```

### Task 7: Move Resources, Mixins, and Render Hook Registration

**Files:**

- Move: `src/main/resources/fabric.mod.json` to `src/fabric/resources/fabric.mod.json`
- Move: `src/main/resources/carrybabyanimals.mixins.json` to `src/fabric/resources/carrybabyanimals.mixins.json`
- Move: `src/main/resources/assets/carrybabyanimals/icon_128.png` to `src/fabric/resources/assets/carrybabyanimals/icon_128.png`
- Move: `src/client/resources/carrybabyanimals.client.mixins.json` to `src/fabricClient/resources/carrybabyanimals.client.mixins.json`
- Move: `src/main/java/dev/jasmine/carrybabyanimals/mixin/EntityStartRidingMixin.java` to `src/fabric/java/dev/jasmine/carrybabyanimals/fabric/mixin/EntityStartRidingMixin.java`
- Move: client mixins to `src/fabricClient/java/dev/jasmine/carrybabyanimals/fabric/client/mixin/`
- Split/move: `CarriedBabyRenderer.java` and `CarriedBabyRenderState.java`

- [ ] **Step 1: Move resources to Fabric source roots**

Move Fabric resources exactly as listed above. Keep file names unchanged.

- [ ] **Step 2: Move mixin classes to Fabric packages**

Update mixin JSON class references from:

```text
dev.jasmine.carrybabyanimals.mixin.EntityStartRidingMixin
dev.jasmine.carrybabyanimals.client.mixin.LivingEntityRendererMixin
dev.jasmine.carrybabyanimals.client.mixin.PlayerModelMixin
```

to:

```text
dev.jasmine.carrybabyanimals.fabric.mixin.EntityStartRidingMixin
dev.jasmine.carrybabyanimals.fabric.client.mixin.LivingEntityRendererMixin
dev.jasmine.carrybabyanimals.fabric.client.mixin.PlayerModelMixin
```

- [ ] **Step 3: Split render hook from render math**

Fabric client owns `LevelRenderEvents.COLLECT_SUBMITS.register(...)`, `LevelRenderContext`, and any `RenderStateDataKey` usage. Common client owns frame evaluation, placement, size classification, reactions, sleepy phase, and first-person visibility decisions.

- [ ] **Step 4: Run render tests**

Run:

```powershell
.\gradlew.bat test --tests dev.jasmine.carrybabyanimals.client.render.*
```

Expected: PASS. Tests continue to cover carried-baby render path decisions, vanilla render suppression helper state where testable, first-person large baby visibility, sleepy visuals, reactions, placement, and multiplayer observer-facing frame decisions where represented in unit tests.

- [ ] **Step 5: Build and inspect jar resources**

Run:

```powershell
.\gradlew.bat build
jar tf build\libs\carrybabyanimals-*.jar | findstr /C:"fabric.mod.json" /C:"carrybabyanimals.mixins.json" /C:"carrybabyanimals.client.mixins.json" /C:"assets/carrybabyanimals/icon_128.png" /C:"dev/jasmine/carrybabyanimals/BuildInfo.class" /C:"CarryBabyAnimalsFabric.class" /C:"CarryBabyAnimalsFabricClient.class"
```

Expected: output includes Fabric metadata, both mixin configs, icon/assets, generated `BuildInfo`, and Fabric entrypoint classes.

- [ ] **Step 6: Verify Loom client source-set handling**

Because Phase 1 uses custom-named client source sets with `splitEnvironmentSourceSets()`, verify that Loom treats `commonClient` and `fabricClient` as client-only source sets before closing the layout move.

Run:

```powershell
.\gradlew.bat tasks --all | findstr /C:"commonClient" /C:"fabricClient" /C:"remap"
javap -classpath build\libs\carrybabyanimals-*.jar -v dev.jasmine.carrybabyanimals.fabric.client.CarryBabyAnimalsFabricClient | findstr /C:"EnvType" /C:"CLIENT" /C:"Environment"
```

Expected: Gradle exposes compile/remap tasks for both custom client source sets, and client adapter classes retain client environment metadata or equivalent Loom handling. If this fails, stop and adjust the Loom source-set declaration before continuing; do not accept a jar where custom client source sets are treated as server/common classes.

- [ ] **Step 7: Commit resource and render adapter slice**

Run:

```powershell
git add src/fabric src/fabricClient src/main src/client src/commonClient build.gradle
git commit -m "refactor: move Fabric resources and render hooks to adapters"
```

### Task 8: Remove Transitional `src/main` and `src/client` Production Ownership

**Files:**

- Modify: `build.gradle`
- Remove production Java/resource ownership from: `src/main`, `src/client`
- Keep: `src/test/java`

- [ ] **Step 1: Confirm no production files remain in old roots**

Run:

```powershell
Get-ChildItem -Recurse -File src/main, src/client -ErrorAction SilentlyContinue
```

Expected: empty output, or only files intentionally scheduled for deletion in this task. Missing `src/main` or `src/client` directories are acceptable at this point.

- [ ] **Step 2: Update Loom mod source sets**

Use the target `loom` block from the "Target Layout" section:

```gradle
loom {
    splitEnvironmentSourceSets()

    mods {
        "carrybabyanimals" {
            sourceSet sourceSets.commonMain
            sourceSet sourceSets.commonClient
            sourceSet sourceSets.fabricMain
            sourceSet sourceSets.fabricClient
        }
    }
}
```

- [ ] **Step 3: Remove old source-set production paths**

Remove `sourceSets.main` and `sourceSets.client` production Java/resource roots from active packaging, except where Gradle creates built-in source sets. Built-in `main` can remain empty; it must not own CarryBabyAnimals production code.

- [ ] **Step 4: Run full automated verification**

Run:

```powershell
git rev-parse --show-toplevel
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat checkChangelog
rg -n "net\.fabricmc|fabric\.api|com\.terraformersmc\.modmenu|me\.lucko\.fabric|net\.neoforged|neoforge" src/common src/commonClient
jar tf build\libs\carrybabyanimals-*.jar | findstr /C:"fabric.mod.json" /C:"carrybabyanimals.mixins.json" /C:"carrybabyanimals.client.mixins.json" /C:"assets/carrybabyanimals/icon_128.png" /C:"dev/jasmine/carrybabyanimals/BuildInfo.class" /C:"CarryBabyAnimalsFabric.class" /C:"CarryBabyAnimalsFabricClient.class"
.\gradlew.bat tasks --all | findstr /C:"commonClient" /C:"fabricClient" /C:"remap"
javap -classpath build\libs\carrybabyanimals-*.jar -v dev.jasmine.carrybabyanimals.fabric.client.CarryBabyAnimalsFabricClient | findstr /C:"EnvType" /C:"CLIENT" /C:"Environment"
```

Expected:

- Root resolves to `C:/Users/tyler/AI Projects/CarryBabyAnimals`.
- Tests pass.
- Build passes and produces the Fabric jar.
- Changelog gate passes.
- Common import scan has no matches.
- Jar inspection proves Fabric metadata, mixins, assets, generated `BuildInfo`, and entrypoints are present.
- Loom/client source-set inspection proves custom client source sets are treated as client-only before implementation closeout.

- [ ] **Step 5: Commit final layout slice**

Run:

```powershell
git add build.gradle src/common src/commonClient src/fabric src/fabricClient src/main src/client src/test INTERNAL_CHANGELOG.md
git commit -m "refactor: finalize Fabric-preserving multiloader source layout"
```

### Task 9: Record Manual Verification Before Phase 1 Closeout

**Files:**

- Modify: `docs/manual-test-plan.md` only if the refactor exposes a missing Phase 1 manual row
- Modify: `INTERNAL_CHANGELOG.md`
- Do not modify: `CHANGELOG.md` unless a public behavior change occurred

- [ ] **Step 1: Run singleplayer manual pass from `docs/manual-test-plan.md`**

Record:

```text
Minecraft version:
Fabric Loader version:
Fabric API version:
CarryBabyAnimals jar path:
World seed:
Result:
```

Required coverage: pickup, carry, petting, set-down, growth cleanup, Nursery Mode, Parent Reunion, config defaults.

- [ ] **Step 2: Run dedicated-server two-modded-client pass**

Required coverage: pickup visibility, carried-baby render path, vanilla passenger render suppression on modded clients, tracking replay, stale clear, pet feedback, disconnect cleanup, drop cleanup, and multiplayer observers.

- [ ] **Step 3: Run vanilla-compatible fallback pass**

Required coverage: one modded client and one client without CarryBabyAnimals; no custom payload requirement; vanilla passenger fallback remains connected and non-duplicating; ModStatus payload remains capability-gated.

- [ ] **Step 4: Run permissions pass**

Required coverage: Fabric Permissions API plus a provider such as LuckPerms; node defaults and overrides for:

```text
carrybabyanimals.carry
carrybabyanimals.carry.tamed
carrybabyanimals.carry.others_tamed
carrybabyanimals.nursery.bypass
carrybabyanimals.reload
```

- [ ] **Step 5: Run ModMenu/config pass**

Required coverage: optional ModMenu opens config screen; saves `config/carrybabyanimals-client.json`; removing ModMenu does not prevent startup.

- [ ] **Step 6: Run ModStatus pass**

Required coverage: matched, build-different, version-different, server-not-detected, unknown, and disconnected states display as before.

- [ ] **Step 7: Run rendering polish pass**

Required coverage: carried-baby render path, vanilla render suppression, first-person large baby visibility modes, sleepy visuals, reaction softening, and other-player observer views.

- [ ] **Step 8: Close Phase 1 only after evidence is recorded**

Do not start NeoForge planning or implementation. Do not publish, tag, or release unless Tyler explicitly starts a release session.

## Final Phase 1 Acceptance Checklist

- [ ] Fabric jar remains the only release-ready jar.
- [ ] No NeoForge dependencies, entrypoints, resources, mixins, or jar exist.
- [ ] No universal jar is planned or produced.
- [ ] No reusable library was extracted.
- [ ] `.\gradlew.bat test` passes.
- [ ] `.\gradlew.bat build` passes.
- [ ] `.\gradlew.bat checkChangelog` passes.
- [ ] Common import scan has no Fabric, ModMenu, Fabric Permissions API, or NeoForge matches.
- [ ] Jar inspection shows Fabric metadata, mixin configs, assets, generated `BuildInfo`, and entrypoint classes.
- [ ] Packet tests prove channel names, payload fields, byte limits, and ModStatus payload behavior remain stable.
- [ ] Manual checks from `docs/manual-test-plan.md` are recorded for singleplayer, multiplayer, vanilla-compatible fallback, permissions, ModMenu/config, ModStatus, and rendering.
- [ ] `INTERNAL_CHANGELOG.md` records maintainer-only refactor work.
- [ ] `CHANGELOG.md` remains unchanged unless implementation created a public behavior/admin impact.

## Open Questions for Tyler Before Implementation

- Should the Phase 1 implementer use one branch for the full refactor, or should each task be landed as separate reviewed commits before the next task starts?
- Should manual verification be recorded in a new implementation evidence doc under `docs/multiloader/`, or only in the implementation closeout message?
- Should the implementation create a small script for the common import scan and jar inspection, or keep those as explicit commands in the task checklist?
