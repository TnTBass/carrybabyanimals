# CarryBabyAnimals NeoForge Adapter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a separate NeoForge adapter/build on top of the accepted Phase 1 Fabric-preserving multiloader baseline while keeping the existing Fabric behavior and Fabric jar output stable.

**Architecture:** Keep the repository as one Gradle build, but split loader builds into separate loader-specific Gradle projects so Fabric Loom and NeoForge tooling do not fight inside one project. The existing Phase 1 `common`, `commonClient`, `fabric`, and `fabricClient` source roots remain the behavioral baseline; Phase 2 adds `neoforge` and `neoforgeClient` adapter roots plus loader-specific jar tasks, with no universal jar and no reusable library extraction.

**Tech Stack:** Java 25, Gradle 9.1.0 or newer, Fabric Loom `1.16-SNAPSHOT`, Minecraft `26.1.2`, Fabric Loader/API, NeoForge `26.1.2.74` as the current Phase 2 planning target, ModDevGradle `2.0.141` as the initial NeoForge Gradle plugin target, JUnit 5, embedded ModStatusKit helpers, optional Fabric Permissions API, optional ModMenu on Fabric.

---

## Scope Boundaries

Phase 2 includes:

- Adding a NeoForge adapter/build after the accepted Phase 1 baseline commit `4bb64bd2a55b3d857772774d42d96c67e9286e58`.
- Preserving the existing Fabric adapter behavior, Fabric metadata, Fabric mixins, Fabric assets, generated `BuildInfo`, and optional ModMenu behavior while intentionally moving Fabric output to the loader-suffixed `carrybabyanimals-${project.version}-fabric.jar` naming scheme.
- Reusing common payload semantics, carry behavior, config parsing, permissions defaults, ModStatus display logic, and render math.
- Adding NeoForge-owned metadata, entrypoints/events, networking transport, config path lookup, permissions fallback/integration, metadata lookup, render hook registration, mixins or equivalent hooks, assets, generated `BuildInfo`, and loader-specific jar output.
- Extending automated and manual verification so Fabric remains stable and NeoForge proves loader parity before any release prep.

Phase 2 excludes:

- A universal jar.
- A reusable extracted library.
- Public `CHANGELOG.md` notes during planning.
- Version `0.2.1` release notes until a multiloader build is implemented, running, and verified.
- Push, PR, tag, publish, or release automation.

## Target Gradle Shape

Use one repository and one Gradle build, but loader-specific Gradle projects:

```text
settings.gradle
build.gradle                  # root conventions, shared version/group/archive metadata, shared verification
fabric.gradle or build-logic   # optional extracted Fabric project conventions
neoforge.gradle or build-logic # optional extracted NeoForge project conventions

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
  neoforge/
    AGENTS.md
    java/dev/jasmine/carrybabyanimals/neoforge/...
    resources/META-INF/neoforge.mods.toml
    resources/carrybabyanimals.neoforge.mixins.json if mixins are required
    resources/assets/carrybabyanimals/icon_128.png
  neoforgeClient/
    AGENTS.md
    java/dev/jasmine/carrybabyanimals/neoforge/client/...
    resources/carrybabyanimals.neoforge.client.mixins.json if client mixins are required
  test/
    java/dev/jasmine/carrybabyanimals/...
```

`settings.gradle` target:

```gradle
pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = "https://maven.fabricmc.net/"
        }
        maven {
            name = "NeoForge"
            url = "https://maven.neoforged.net/releases"
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "CarryBabyAnimals"
include("fabric")
include("neoforge")
```

Rationale:

- Keep this as one Gradle build so version, group, changelog gates, common tests, and release discipline stay centralized.
- Use separate loader-specific projects/tasks because Fabric Loom and NeoForge Gradle plugins both own Minecraft dependency setup, remapping, runs, resource processing, and jar production.
- Keep source ownership physically under the existing `src/<area>` roots to preserve the accepted Phase 1 layout and avoid extracting a library.
- Wire `:fabric` to compile/package `src/common`, `src/commonClient`, `src/fabric`, and `src/fabricClient`.
- Wire `:neoforge` to compile/package `src/common`, `src/commonClient`, `src/neoforge`, and `src/neoforgeClient`.
- Keep root tasks such as `test`, `checkChangelog`, `checkCommonSourceSetsLoaderNeutral`, artifact inspection, and packet compatibility checks as orchestration gates.

Required loader-specific output names:

```text
build/libs/carrybabyanimals-${project.version}-fabric.jar
build/libs/carrybabyanimals-${project.version}-neoforge.jar
```

Phase 2 intentionally renames loader outputs to the suffix form above. Update release/upload scripts and artifact inspection commands in the same Gradle slice so Fabric publishing still points at the Fabric jar intentionally before any release prep.

## Ownership Boundaries

Common main owns:

- `dev.jasmine.carrybabyanimals.CarryBabyAnimals` constants and loader-neutral logger identity.
- `dev.jasmine.carrybabyanimals.BuildInfo` generation contract, with each loader jar receiving its own generated class.
- `dev.jasmine.carrybabyanimals.carry`
- `dev.jasmine.carrybabyanimals.config`
- `dev.jasmine.carrybabyanimals.cozy`
- `dev.jasmine.carrybabyanimals.internal.modstatus`
- `dev.jasmine.carrybabyanimals.modstatus`
- `dev.jasmine.carrybabyanimals.network` packet channel names, payload field contracts, byte limits, and transport-neutral sender interface.
- `dev.jasmine.carrybabyanimals.nursery`
- `dev.jasmine.carrybabyanimals.permissions` node names and fallback/default semantics.
- `dev.jasmine.carrybabyanimals.reunion`

Common client owns:

- `dev.jasmine.carrybabyanimals.client.config` config model, layout, edit state, save/load behavior once a loader path is supplied.
- `dev.jasmine.carrybabyanimals.client.modstatus`
- `dev.jasmine.carrybabyanimals.client.render` visual frame math, size buckets, placement, reactions, sleepy visual phases, first-person large-baby visibility policy, and loader-neutral render state helpers.

Fabric adapter owns:

- Fabric `ModInitializer`.
- Fabric server/common lifecycle event registration.
- Fabric payload registration, codecs, send/can-send checks, `PlayerLookup`, tracking replay, stale clear, and vanilla passenger sync recipients.
- Fabric config path lookup through `FabricLoader`.
- Fabric metadata lookup through `FabricLoader`.
- Fabric Permissions API optional integration.
- Fabric main mixins and `src/fabric/resources/fabric.mod.json`.
- Fabric assets and generated Fabric `BuildInfo`.

Fabric client adapter owns:

- Fabric `ClientModInitializer`.
- Fabric client networking receivers and serverbound send calls.
- Fabric client config path setup.
- ModMenu integration.
- Fabric render hook registration, Fabric render context extraction, Fabric render-state keys, and Fabric client mixins.
- `src/fabricClient/resources/carrybabyanimals.client.mixins.json`.

NeoForge adapter owns:

- NeoForge mod entrypoint class under `dev.jasmine.carrybabyanimals.neoforge.CarryBabyAnimalsNeoForge`.
- NeoForge event-bus registration for server/common lifecycle hooks.
- NeoForge payload registration, codecs, send/can-send checks, tracking-recipient discovery, stale clear, and vanilla passenger sync recipients.
- NeoForge config path lookup through the NeoForge/FML loading context.
- NeoForge metadata lookup for current mod version.
- NeoForge permission integration or explicit vanilla fallback behavior with the same default semantics as common.
- NeoForge metadata under `META-INF/neoforge.mods.toml`.
- NeoForge server/common mixins or equivalent hooks.
- NeoForge assets and generated NeoForge `BuildInfo`.

NeoForge client adapter owns:

- NeoForge client setup/event registration.
- NeoForge client payload receivers and serverbound send calls.
- NeoForge client config path setup.
- No optional NeoForge in-game config UI for `0.2.1`. NeoForge still owns client config path setup for `config/carrybabyanimals-client.json`.
- NeoForge render hook registration, render context extraction, render-state key equivalent, and client mixins or events for render suppression and carrier arm pose.

## Scoped AGENTS.md Files To Add

Add these only when the NeoForge roots are created.

`src/neoforge/AGENTS.md`:

```markdown
# Agent Notes for NeoForge Main Adapter

This source root owns NeoForge server/common adapter code and NeoForge jar resources.

- NeoForge entrypoints, lifecycle events, payload registration, recipient discovery, config path lookup, metadata lookup, permission-provider integration or explicit fallback, mixin classes, mixin JSON, `META-INF/neoforge.mods.toml`, and NeoForge assets belong here.
- Keep NeoForge-specific code small and delegate behavior to common services.
- Do not change common behavior or packet semantics to paper over a NeoForge adapter gap.
- Do not add Fabric, ModMenu, or Fabric Permissions API code here.
```

`src/neoforgeClient/AGENTS.md`:

```markdown
# Agent Notes for NeoForge Client Adapter

This source root owns NeoForge client adapter code.

- NeoForge client setup, client networking receivers, client config path lookup, render hook registration, NeoForge render-state equivalents, and client mixins or event hooks belong here.
- Keep NeoForge-specific code small and delegate render math, config state, and ModStatus display behavior to common client code.
- Do not change common behavior or Fabric adapter behavior to make NeoForge compile.
- Do not add Fabric, ModMenu, or Fabric Permissions API code here.
```

## Implementation Tasks

### Task 1: Reconfirm Baseline And Add Planning Guardrails

**Files:**

- Modify: `INTERNAL_CHANGELOG.md`
- Do not modify: production Java, resources, `build.gradle`, or `settings.gradle` in this task.

- [ ] **Step 1: Verify repository root**

Run:

```powershell
git rev-parse --show-toplevel
```

Expected: `C:/Users/tyler/AI Projects/CarryBabyAnimals` or the same absolute path with backslashes.

- [ ] **Step 2: Verify baseline commit**

Run:

```powershell
git rev-parse HEAD
```

Expected: either `4bb64bd2a55b3d857772774d42d96c67e9286e58` or a later local commit Tyler explicitly accepted for Phase 2 planning/implementation.

- [ ] **Step 3: Inspect worktree**

Run:

```powershell
git status --short
```

Expected: no unrelated dirty files. Preserve any Tyler-authored work if present; do not revert it.

- [ ] **Step 4: Add maintainer-only changelog note if this plan has not already done it**

Add an `INTERNAL_CHANGELOG.md` entry under `## Unreleased`:

```markdown
- Added a Phase 2 implementation plan for adding a separate NeoForge adapter/build on top of the accepted Fabric-preserving multiloader baseline.
```

Do not edit `CHANGELOG.md` because planning produces no public player/server-admin behavior change.

- [ ] **Step 5: Run planning verification**

Run:

```powershell
git diff --check
.\gradlew.bat checkChangelog
```

Expected: whitespace check passes and the changelog gate passes.

### Task 2: Split Gradle Build Without Adding NeoForge Code

**Files:**

- Modify: `settings.gradle`
- Modify: `build.gradle`
- Create: `fabric/build.gradle` or an equivalent loader-specific Gradle script
- Create: `neoforge/build.gradle` or an equivalent loader-specific Gradle script
- Modify only Gradle files and generated-task wiring in this task.

- [ ] **Step 1: Move Fabric build ownership into `:fabric` while preserving source roots**

Configure `:fabric` to compile/package the accepted Phase 1 source roots by declaring cross-root source directories directly in the `:fabric` source sets. Do not create `:common` as a project dependency and do not copy sources into the subproject.

Use this `:fabric` source-set shape:

```gradle
sourceSets {
    main {
        java {
            srcDirs(
                    "../src/common/java",
                    "../src/commonClient/java",
                    "../src/fabric/java",
                    "../src/fabricClient/java"
            )
        }
        resources {
            srcDirs(
                    "../src/common/resources",
                    "../src/commonClient/resources",
                    "../src/fabric/resources",
                    "../src/fabricClient/resources"
            )
        }
    }
    test {
        java {
            srcDir "../src/test/java"
        }
    }
}
```

Expected: `:fabric:build` still produces the same Fabric metadata, mixins, assets, entrypoints, generated `BuildInfo`, and optional integration declarations currently produced by the root build.

This cross-root `srcDirs` approach is intentionally transitional for Phase 2. It preserves the accepted source layout without extracting a library, but the implementer must verify IDE indexing, Loom remapping, source jar contents, and duplicate class handling before accepting the split.

- [ ] **Step 2: Keep root verification orchestration**

Root `check` must depend on:

```text
:fabric:test
:fabric:build
checkChangelog
checkCommonSourceSetsLoaderNeutral
checkReleasePublishingSources
checkReleaseNotesStyle
```

Do not wire NeoForge into root `check` until the empty NeoForge project exists and its dependencies resolve.

- [ ] **Step 3: Add empty `:neoforge` project shell**

Create an empty loader project with plugin repositories and Java 25 toolchain only. Do not add NeoForge dependencies or production code yet.

Also add the target NeoForge source-set wiring so Task 4 can add files without guessing how cross-root sources enter the loader project:

```gradle
sourceSets {
    main {
        java {
            srcDirs(
                    "../src/common/java",
                    "../src/commonClient/java",
                    "../src/neoforge/java",
                    "../src/neoforgeClient/java"
            )
        }
        resources {
            srcDirs(
                    "../src/common/resources",
                    "../src/commonClient/resources",
                    "../src/neoforge/resources",
                    "../src/neoforgeClient/resources"
            )
        }
    }
    test {
        java {
            srcDir "../src/test/java"
        }
    }
}
```

The NeoForge project must not include `../src/fabric/java`, `../src/fabricClient/java`, `../src/fabric/resources`, or `../src/fabricClient/resources`.

- [ ] **Step 4: Preserve Fabric jar output**

Run:

```powershell
.\gradlew.bat :fabric:build
$fabricJar = Get-ChildItem fabric\build\libs\*.jar |
    Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-dev.jar" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if ($null -eq $fabricJar) { throw "No primary Fabric jar found under fabric\build\libs" }
$fabricEntries = jar tf $fabricJar.FullName
$requiredFabricEntries = @(
    "fabric.mod.json",
    "carrybabyanimals.mixins.json",
    "carrybabyanimals.client.mixins.json",
    "assets/carrybabyanimals/icon_128.png",
    "dev/jasmine/carrybabyanimals/BuildInfo.class"
)
foreach ($entry in $requiredFabricEntries) {
    if (-not ($fabricEntries -contains $entry)) { throw "Missing Fabric jar entry: $entry" }
}
```

Expected: Fabric build passes and jar inspection includes the Fabric metadata, both Fabric mixin configs, icon, and generated `BuildInfo`.

- [ ] **Step 5: Run common import scan**

Run:

```powershell
rg -n "^\s*import\s+(net\.fabricmc|com\.terraformersmc\.modmenu|me\.lucko\.fabric|net\.neoforged)\." src/common src/commonClient
```

Expected: no matches.

### Task 3: Normalize Fabric Adapter Package Names

**Files:**

- Modify if renaming: `src/fabric/java/dev/jasmine/carrybabyanimals/network/CarryNetworking.java`
- Modify if renaming: `src/fabricClient/java/dev/jasmine/carrybabyanimals/client/CarryBabyAnimalsClient.java`
- Modify if renaming: `src/fabricClient/java/dev/jasmine/carrybabyanimals/client/config/CarryBabyAnimalsModMenuIntegration.java`
- Modify if renaming: `src/fabricClient/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyRenderer.java`
- Modify if renaming: `src/fabricClient/java/dev/jasmine/carrybabyanimals/client/render/CarriedBabyRenderState.java`
- Modify if renaming: `src/fabric/resources/fabric.mod.json`
- Modify if renaming: tests that import legacy adapter package names.

- [ ] **Step 1: Apply the adapter-package rule before adding NeoForge**

Use this rule:

```text
Fabric adapter packages use dev.jasmine.carrybabyanimals.fabric...
Fabric client adapter packages use dev.jasmine.carrybabyanimals.fabric.client...
NeoForge adapter packages use dev.jasmine.carrybabyanimals.neoforge...
NeoForge client adapter packages use dev.jasmine.carrybabyanimals.neoforge.client...
Common packages keep dev.jasmine.carrybabyanimals... and dev.jasmine.carrybabyanimals.client...
```

Do not freeze the current legacy Fabric client package names. Tyler chose explicit Fabric naming because it will be easier to maintain once multiple loaders exist.

- [ ] **Step 2: If renaming, update Fabric metadata and tests in the same slice**

`src/fabric/resources/fabric.mod.json` entrypoints must match the final classes:

```json
"main": [
  "dev.jasmine.carrybabyanimals.fabric.CarryBabyAnimalsFabric"
],
"client": [
  "dev.jasmine.carrybabyanimals.fabric.client.CarryBabyAnimalsFabricClient"
],
"modmenu": [
  "dev.jasmine.carrybabyanimals.fabric.client.config.CarryBabyAnimalsModMenuIntegration"
]
```

- [ ] **Step 3: Verify Fabric package decision**

Run:

```powershell
.\gradlew.bat :fabric:test
.\gradlew.bat :fabric:build
$fabricJar = Get-ChildItem fabric\build\libs\*.jar |
    Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-dev.jar" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if ($null -eq $fabricJar) { throw "No primary Fabric jar found under fabric\build\libs" }
$fabricEntries = jar tf $fabricJar.FullName
$requiredFabricEntries = @(
    "fabric.mod.json",
    "dev/jasmine/carrybabyanimals/fabric/CarryBabyAnimalsFabric.class",
    "dev/jasmine/carrybabyanimals/fabric/client/CarryBabyAnimalsFabricClient.class",
    "dev/jasmine/carrybabyanimals/fabric/client/config/CarryBabyAnimalsModMenuIntegration.class"
)
foreach ($entry in $requiredFabricEntries) {
    if (-not ($fabricEntries -contains $entry)) { throw "Missing Fabric jar entry after package normalization: $entry" }
}
```

Expected: tests and build pass; jar contains the entrypoint classes named by Fabric metadata.

### Task 4: Add NeoForge Dependencies, Metadata, And Empty Entrypoints

**Files:**

- Modify: `gradle.properties`
- Modify: `neoforge/build.gradle`
- Create: `src/neoforge/AGENTS.md`
- Create: `src/neoforgeClient/AGENTS.md`
- Create: `src/neoforge/resources/META-INF/neoforge.mods.toml`
- Create: `src/neoforge/resources/assets/carrybabyanimals/icon_128.png` by copying the existing Fabric icon.
- Create: `src/neoforge/java/dev/jasmine/carrybabyanimals/neoforge/CarryBabyAnimalsNeoForge.java`
- Create: `src/neoforgeClient/java/dev/jasmine/carrybabyanimals/neoforge/client/CarryBabyAnimalsNeoForgeClient.java`

- [ ] **Step 1: Refresh and add version properties**

Start from the current planning target below, then refresh against the official NeoForge changelog and Maven metadata before implementation in case a newer compatible `26.1.2` build exists:

```properties
neoforge_version=26.1.2.74
neoforge_loader_version_range=[26.1.2.74,)
neoforge_minecraft_version_range=[26.1.2,26.2)
moddevgradle_version=2.0.141
```

Do not change Fabric `loader_version`, `fabric_version`, `modmenu_version`, `minecraft_version`, or `mod_version` in this task.

- [ ] **Step 2: Add scoped NeoForge `AGENTS.md` files**

Use the exact contents from "Scoped AGENTS.md Files To Add".

- [ ] **Step 3: Wire NeoForge `BuildInfo` generation**

Add a NeoForge equivalent of the existing generated build metadata. The generated source must compile into the NeoForge main source set and jar at:

```text
dev/jasmine/carrybabyanimals/BuildInfo.class
```

Use the same package and field contract as Fabric:

```java
package dev.jasmine.carrybabyanimals;

/**
 * Build metadata generated by Gradle.
 */
public final class BuildInfo {
    public static final String BUILD_NUMBER = "dev";

    private BuildInfo() {
    }
}
```

Before wiring generated sources, confirm there is no checked-in `BuildInfo.java` that would collide with generated output:

```powershell
$buildInfoSearchRoots = @("src/common/java", "src/commonClient/java", "src/fabric/java", "src/neoforge/java") | Where-Object { Test-Path $_ }
Get-ChildItem -Recurse $buildInfoSearchRoots -Filter BuildInfo.java
```

Expected before the slice is complete: no static `src/common/java/dev/jasmine/carrybabyanimals/BuildInfo.java`, `src/commonClient/java/dev/jasmine/carrybabyanimals/BuildInfo.java`, `src/fabric/java/dev/jasmine/carrybabyanimals/BuildInfo.java`, or `src/neoforge/java/dev/jasmine/carrybabyanimals/BuildInfo.java` remains. If an existing stub is present, remove or move it in this same slice so both `:fabric` and `:neoforge` compile with exactly one generated `dev.jasmine.carrybabyanimals.BuildInfo` class.

In `neoforge/build.gradle`, wire the generated directory into the NeoForge main source set before compile:

```gradle
def generatedBuildInfoDir = layout.buildDirectory.dir("generated/sources/buildInfo/java/main")

tasks.register("generateBuildInfo") {
    group = "build"
    description = "Generates build metadata embedded in the NeoForge mod jar."
    def buildNumberProvider = providers.provider { rootProject.ext.resolveBuildNumber() }
    inputs.property "buildNumber", buildNumberProvider
    outputs.dir generatedBuildInfoDir

    doLast {
        String buildNumber = buildNumberProvider.get()
        File outputFile = generatedBuildInfoDir.get().file("dev/jasmine/carrybabyanimals/BuildInfo.java").asFile
        outputFile.parentFile.mkdirs()
        outputFile.text = """package dev.jasmine.carrybabyanimals;

/**
 * Build metadata generated by Gradle.
 */
public final class BuildInfo {
    public static final String BUILD_NUMBER = "${buildNumber.replace("\\", "\\\\").replace("\"", "\\\"")}";

    private BuildInfo() {
    }
}
"""
    }
}

sourceSets {
    main {
        java.srcDir generatedBuildInfoDir
    }
}

tasks.named("compileJava") {
    dependsOn tasks.named("generateBuildInfo")
}
```

If the shared `resolveBuildNumber` helper remains in root `build.gradle`, expose it through `rootProject.ext.resolveBuildNumber`. If Gradle script extraction moves it elsewhere, keep one shared helper and wire both loader projects to it.

- [ ] **Step 4: Add NeoForge metadata**

Create `META-INF/neoforge.mods.toml` with:

```toml
modLoader = "javafml"
loaderVersion = "[26.1.2.74,)"
license = "MIT"

[[mods]]
modId = "carrybabyanimals"
version = "${file.jarVersion}"
displayName = "Carry Baby Animals"
authors = "Tyler, Jasmine"
description = '''
Carry and pet baby animals without putting them in your inventory.
'''
logoFile = "assets/carrybabyanimals/icon_128.png"

[[dependencies.carrybabyanimals]]
modId = "neoforge"
type = "required"
versionRange = "[26.1.2.74,)"
ordering = "NONE"
side = "BOTH"

[[dependencies.carrybabyanimals]]
modId = "minecraft"
type = "required"
versionRange = "[26.1.2,26.2)"
ordering = "NONE"
side = "BOTH"
```

Use `${file.jarVersion}` for the mod version because ModDevGradle supports that jar-manifest backed substitution convention. Keep the loader and Minecraft version ranges literal in the TOML unless the implementation also adds an explicit, verified ModDevGradle resource-substitution step for those properties. If the implementation refresh finds a newer official `26.1.2` NeoForge build, update the version property and these literal ranges together in the same slice.

- [ ] **Step 5: Add no-op NeoForge entrypoints**

Create minimal entrypoints that log startup and do not register behavior yet:

```java
package dev.jasmine.carrybabyanimals.neoforge;

import dev.jasmine.carrybabyanimals.CarryBabyAnimals;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CarryBabyAnimals.MOD_ID)
public final class CarryBabyAnimalsNeoForge {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarryBabyAnimals.MOD_ID);

    public CarryBabyAnimalsNeoForge() {
        LOGGER.info("Carry Baby Animals NeoForge adapter loaded");
    }
}
```

Create the client setup class but do not wire render/network behavior yet:

```java
package dev.jasmine.carrybabyanimals.neoforge.client;

public final class CarryBabyAnimalsNeoForgeClient {
    private CarryBabyAnimalsNeoForgeClient() {
    }
}
```

- [ ] **Step 6: Build both loaders**

Run:

```powershell
.\gradlew.bat :fabric:build
.\gradlew.bat :neoforge:build
```

Expected: Fabric build still passes; NeoForge build produces a loader-specific jar with metadata, icon, generated `BuildInfo`, common classes, and no gameplay registrations yet.

### Task 5: Implement NeoForge Server Lifecycle, Config Path, Metadata, And Permissions

**Files:**

- Modify: `src/neoforge/java/dev/jasmine/carrybabyanimals/neoforge/CarryBabyAnimalsNeoForge.java`
- Create: `src/neoforge/java/dev/jasmine/carrybabyanimals/neoforge/NeoForgeCarryPermissions.java`
- Create: `src/neoforge/java/dev/jasmine/carrybabyanimals/neoforge/NeoForgePaths.java` if path lookup is split out.
- Create: `src/neoforge/java/dev/jasmine/carrybabyanimals/neoforge/NeoForgeModMetadata.java` if metadata lookup is split out.
- Modify common code only if the existing Fabric bootstrap cannot be reused without Fabric imports.

- [ ] **Step 1: Register server/common lifecycle hooks**

Wire NeoForge events equivalent to the Fabric adapter's:

```text
server tick
entity interact
attack entity
attack block
block break prevention
use item
use block
entity tracking start
entity tracking stop
player join
player disconnect
player death cleanup
player dimension change cleanup
server stopping cleanup
```

Each event must delegate to the same common managers and interaction handlers used by Fabric.

- [ ] **Step 2: Load server config from the NeoForge config directory**

Use the same filename:

```text
config/carrybabyanimals.json
```

Expected: config defaults, parsing, unknown animal warnings, allow/block rules, Nursery Mode, and Parent Reunion behavior remain common.

- [ ] **Step 3: Supply current version/build metadata**

NeoForge metadata lookup must call:

```java
CarryBabyAnimalsModStatus.useCurrentVersion(currentVersionFromNeoForgeMetadata);
```

The generated `BuildInfo.BUILD_NUMBER` must be present in the NeoForge jar before the adapter is release-ready.

- [ ] **Step 4: Implement permissions fallback**

Preserve these common default semantics:

```text
carrybabyanimals.carry -> true
carrybabyanimals.carry.tamed -> true
carrybabyanimals.carry.others_tamed -> false
carrybabyanimals.nursery.bypass -> game-master command permission fallback
carrybabyanimals.reload -> game-master command permission fallback
```

If NeoForge exposes a stable permission provider API for Minecraft `26.1.2`, integrate it here. If the target NeoForge build has no stable permission provider API, keep explicit vanilla fallback behavior and document "NeoForge permission-provider integration not available on the target loader build" as an open release-parity risk.

- [ ] **Step 5: Verify server/common behavior**

Run:

```powershell
.\gradlew.bat :neoforge:test --tests dev.jasmine.carrybabyanimals.carry.* --tests dev.jasmine.carrybabyanimals.config.* --tests dev.jasmine.carrybabyanimals.nursery.* --tests dev.jasmine.carrybabyanimals.permissions.* --tests dev.jasmine.carrybabyanimals.reunion.*
.\gradlew.bat :neoforge:build
.\gradlew.bat :fabric:build
```

Expected: NeoForge common tests pass, NeoForge build passes, and Fabric build still passes.

### Task 6: Implement NeoForge Networking Transport

**Files:**

- Create: `src/neoforge/java/dev/jasmine/carrybabyanimals/neoforge/network/NeoForgeCarryNetworking.java`
- Modify: `src/neoforge/java/dev/jasmine/carrybabyanimals/neoforge/CarryBabyAnimalsNeoForge.java`
- Modify: tests under `src/test/java/dev/jasmine/carrybabyanimals/network/`

- [ ] **Step 1: Register payloads with NeoForge networking**

Implement NeoForge equivalents for the existing logical packets:

```text
carrybabyanimals:set_carried
carrybabyanimals:clear_carried
carrybabyanimals:pet_carried
carrybabyanimals:pet_feedback
carrybabyanimals:server_version
```

Fields must remain:

```text
set_carried -> babyEntityId, carrierEntityId
clear_carried -> babyEntityId
pet_carried -> no fields
pet_feedback -> babyEntityId
server_version -> encoded ModStatus status bytes, max 256 bytes
```

- [ ] **Step 2: Preserve capability-gated sends**

NeoForge sends must check recipient support before sending custom payloads. Vanilla-compatible fallback must not require custom payload support.

- [ ] **Step 3: Preserve passenger sync**

NeoForge server-side send logic must still send vanilla passenger sync packets to the carrier and all relevant tracking players so clients without CarryBabyAnimals remain connected and see the vanilla passenger fallback.

- [ ] **Step 4: Add transport parity tests where possible**

Tests must prove:

```text
channel names are unchanged
payload field order/meaning is unchanged
server_version rejects or caps payloads at 256 bytes
ModStatus legacy and structured payload decode behavior remains unchanged
server-version sends remain capability-gated
recipient set logic includes carrier, carrier trackers, and baby trackers
```

- [ ] **Step 5: Verify packet compatibility**

Run:

```powershell
.\gradlew.bat :fabric:test --tests dev.jasmine.carrybabyanimals.network.CarryNetworkingTest --tests dev.jasmine.carrybabyanimals.modstatus.CarryBabyAnimalsModStatusTest
.\gradlew.bat :neoforge:test --tests dev.jasmine.carrybabyanimals.network.CarryNetworkingTest --tests dev.jasmine.carrybabyanimals.modstatus.CarryBabyAnimalsModStatusTest
rg -n "set_carried|clear_carried|pet_carried|pet_feedback|server_version|MAX_ENCODED_BYTES|256" src/common src/fabric src/fabricClient src/neoforge src/neoforgeClient src/test
```

Expected: tests pass on both loader projects; scan shows the same five channel names and 256-byte limit with no loader-specific drift.

### Task 7: Implement NeoForge Client Networking, Config Path, And ModStatus

**Files:**

- Modify: `src/neoforgeClient/java/dev/jasmine/carrybabyanimals/neoforge/client/CarryBabyAnimalsNeoForgeClient.java`
- Create: `src/neoforgeClient/java/dev/jasmine/carrybabyanimals/neoforge/client/network/NeoForgeClientCarryNetworking.java`
- Modify only common client code if a path injection or state hook is missing.

- [ ] **Step 1: Load client visual config from the NeoForge config directory**

Use the same filename:

```text
config/carrybabyanimals-client.json
```

Expected: config defaults, normalization, save behavior, `firstPersonLargeBabyVisibilityMode`, `sleepyCarryVisualsEnabled`, and reaction settings remain common.

Do not add a NeoForge in-game config UI in Phase 2. ModMenu remains Fabric-only in this repo, and Tyler chose to avoid adding a new NeoForge config-screen dependency or design for `0.2.1`.

- [ ] **Step 2: Register clientbound receivers**

NeoForge client receivers must update the same common client state as Fabric:

```text
set_carried -> CarriedBabyRenderState.set(babyEntityId, carrierEntityId)
clear_carried -> CarriedBabyRenderState.clear(babyEntityId)
pet_feedback -> ClientCarryInteractionHandler or common equivalent reaction trigger
server_version -> ClientModStatusTracker.onServerStatus(payload.serverStatus())
```

- [ ] **Step 3: Register serverbound pet request**

NeoForge client pre-attack/input handling must send `pet_carried` only when the server supports the payload. It must preserve attack blocking and petting feedback behavior.

- [ ] **Step 4: Register join/disconnect client cleanup**

NeoForge client events must call:

```text
ClientModStatusTracker.onJoin()
CarriedBabyRenderState.clearAll()
ClientModStatusTracker.onDisconnect()
ClientModStatusTracker.tick()
```

- [ ] **Step 5: Verify client state and ModStatus tests**

Run:

```powershell
.\gradlew.bat :neoforge:test --tests dev.jasmine.carrybabyanimals.client.config.* --tests dev.jasmine.carrybabyanimals.client.modstatus.* --tests dev.jasmine.carrybabyanimals.modstatus.*
.\gradlew.bat :fabric:test --tests dev.jasmine.carrybabyanimals.client.config.* --tests dev.jasmine.carrybabyanimals.client.modstatus.* --tests dev.jasmine.carrybabyanimals.modstatus.*
```

Expected: tests pass on both loaders; ModStatus matched, build-different, version-different, server-not-detected, unknown, and disconnected states remain unchanged.

### Task 8: Implement NeoForge Rendering Hooks And Mixins

**Files:**

- Create/modify: `src/neoforgeClient/java/dev/jasmine/carrybabyanimals/neoforge/client/render/NeoForgeCarriedBabyRenderer.java`
- Create/modify: `src/neoforgeClient/java/dev/jasmine/carrybabyanimals/neoforge/client/render/NeoForgeCarriedBabyRenderState.java` only if NeoForge requires loader-specific render-state keys.
- Create: NeoForge client mixins or event-hook equivalents for vanilla render suppression and carrier arm pose.
- Create: `src/neoforgeClient/resources/carrybabyanimals.neoforge.client.mixins.json` if mixins are required.
- Create: `src/neoforge/resources/carrybabyanimals.neoforge.mixins.json` if server/common mixins are required.
- Modify: `src/neoforge/resources/META-INF/neoforge.mods.toml` to declare mixin configs if required.

- [ ] **Step 1: Register a NeoForge render collection hook**

The NeoForge render hook must call the existing common frame evaluation path for:

```text
carried-baby held render
vanilla render suppression helper state
first-person large baby visibility
sleepy visual phases
reaction softening
other-player observer views
```

- [ ] **Step 2: Add vanilla render suppression**

Implement the NeoForge mixin or supported event equivalent so carried babies do not render twice on modded NeoForge clients.

- [ ] **Step 3: Add carrier arm pose hook**

Implement the NeoForge mixin or supported event equivalent for the carrier pose, preserving Fabric visual behavior.

- [ ] **Step 4: Verify render tests**

Run:

```powershell
.\gradlew.bat :neoforge:test --tests dev.jasmine.carrybabyanimals.client.render.*
.\gradlew.bat :fabric:test --tests dev.jasmine.carrybabyanimals.client.render.*
```

Expected: tests pass on both loaders and cover placement, size classification, first-person visibility, sleepy visuals, reactions, visual frame evaluation, and render-state cleanup.

- [ ] **Step 5: Inspect NeoForge jar resources**

Run:

```powershell
.\gradlew.bat :neoforge:build
$neoForgeJar = Get-ChildItem neoforge\build\libs\*.jar |
    Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-dev.jar" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if ($null -eq $neoForgeJar) { throw "No primary NeoForge jar found under neoforge\build\libs" }
$neoForgeEntries = jar tf $neoForgeJar.FullName
$requiredNeoForgeEntries = @(
    "META-INF/neoforge.mods.toml",
    "assets/carrybabyanimals/icon_128.png",
    "dev/jasmine/carrybabyanimals/BuildInfo.class",
    "dev/jasmine/carrybabyanimals/neoforge/CarryBabyAnimalsNeoForge.class"
)
foreach ($entry in $requiredNeoForgeEntries) {
    if (-not ($neoForgeEntries -contains $entry)) { throw "Missing NeoForge jar entry: $entry" }
}
```

Expected: NeoForge metadata, assets, generated `BuildInfo`, entrypoint, and any declared mixin configs are present.

### Task 9: Add Artifact And Static Verification Gates

**Files:**

- Modify: `build.gradle`
- Modify: `fabric/build.gradle`
- Modify: `neoforge/build.gradle`
- Optionally create: `scripts/inspect-loader-artifacts.ps1`

- [ ] **Step 1: Extend common source-set loader-neutral scan**

The gate must reject imports from:

```text
net.fabricmc
com.terraformersmc.modmenu
me.lucko.fabric
net.neoforged
```

Run:

```powershell
.\gradlew.bat checkCommonSourceSetsLoaderNeutral
rg -n "^\s*import\s+(net\.fabricmc|com\.terraformersmc\.modmenu|me\.lucko\.fabric|net\.neoforged)\." src/common src/commonClient
```

Expected: both checks pass with no matches.

- [ ] **Step 2: Add Fabric artifact inspection gate**

Fabric jar inspection must prove:

```text
fabric.mod.json
carrybabyanimals.mixins.json
carrybabyanimals.client.mixins.json
assets/carrybabyanimals/icon_128.png
dev/jasmine/carrybabyanimals/BuildInfo.class
Fabric entrypoint class
Fabric client entrypoint class
ModMenu integration class
```

- [ ] **Step 3: Add NeoForge artifact inspection gate**

NeoForge jar inspection must prove:

```text
META-INF/neoforge.mods.toml
NeoForge mixin configs if used
assets/carrybabyanimals/icon_128.png
dev/jasmine/carrybabyanimals/BuildInfo.class
NeoForge entrypoint class
NeoForge client setup/render/network classes
loader-specific jar filename/output
```

- [ ] **Step 4: Verify full automated matrix**

Run:

```powershell
git rev-parse --show-toplevel
.\gradlew.bat :fabric:test
.\gradlew.bat :fabric:build
.\gradlew.bat :neoforge:test
.\gradlew.bat :neoforge:build
.\gradlew.bat checkChangelog
.\gradlew.bat checkCommonSourceSetsLoaderNeutral
.\gradlew.bat check
```

Expected: all pass. If `test` and `build` collide on Gradle in-progress result files, rerun them serially and record that as tooling behavior, not a code failure.

### Task 10: Run Loader-Specific Manual Verification

**Files:**

- Modify: `docs/manual-test-plan.md` only if Phase 2 needs NeoForge rows added before implementation starts.
- Modify: `INTERNAL_CHANGELOG.md`
- Do not modify: `CHANGELOG.md` until verified public NeoForge/multiloader support exists.

- [ ] **Step 1: Add manual-test-plan NeoForge rows if missing**

Extend `docs/manual-test-plan.md` so every Fabric manual area has a NeoForge equivalent:

```text
Singleplayer NeoForge client
NeoForge dedicated server with two NeoForge modded clients
NeoForge dedicated server with one modded client and one client without CarryBabyAnimals
NeoForge permissions/fallback server
NeoForge config path and no in-game config UI status
NeoForge ModStatus
NeoForge rendering polish
Cross-loader compatibility declaration
```

Cross-loader compatibility declaration must state that `0.2.1` requires same-loader custom held rendering plus vanilla passenger fallback. Fabric-client-to-NeoForge-server and NeoForge-client-to-Fabric-server custom payload compatibility is exploratory, not release-blocking. If cross-loader custom packets work naturally after both loader transports preserve the same common protocol semantics, document the verified result; otherwise document same-loader custom rendering as the supported `0.2.1` behavior.

- [ ] **Step 2: Run Fabric manual regression from `docs/manual-test-plan.md`**

Record:

```text
Minecraft version:
Fabric Loader version:
Fabric API version:
CarryBabyAnimals Fabric jar path:
World seed:
Result:
```

Required coverage: singleplayer, multiplayer modded clients, vanilla-compatible fallback, permissions, ModMenu/config UI, ModStatus, rendering polish.

- [ ] **Step 3: Run NeoForge singleplayer pass**

Required coverage: pickup, carry, petting, set-down, growth cleanup, Nursery Mode, Parent Reunion, config defaults, client config load/save, and local render state cleanup.

- [ ] **Step 4: Run NeoForge dedicated-server two-modded-client pass**

Required coverage: pickup visibility, carried-baby render path, vanilla render suppression on modded clients, tracking replay, stale clear, pet feedback, disconnect cleanup, drop cleanup, and multiplayer observers.

- [ ] **Step 5: Run NeoForge vanilla-compatible fallback pass**

Required coverage: one modded NeoForge client and one client without CarryBabyAnimals; no custom payload requirement; vanilla passenger fallback remains connected and non-duplicating; ModStatus payload remains capability-gated.

- [ ] **Step 6: Run NeoForge config, permissions, no-UI, and ModStatus pass**

Required coverage:

```text
server config path -> config/carrybabyanimals.json
client config path -> config/carrybabyanimals-client.json
permission node defaults and overrides or documented vanilla fallback
no in-game config UI for 0.2.1; JSON config remains supported
matched/build-different/version-different/server-not-detected/unknown/disconnected ModStatus states
```

- [ ] **Step 7: Run NeoForge rendering polish pass**

Required coverage: carried-baby render path, vanilla render suppression, first-person large baby visibility modes, sleepy visuals, reaction softening, and other-player observer views.

### Task 11: Close Phase 2 Without Release Actions

**Files:**

- Modify: `INTERNAL_CHANGELOG.md`
- Modify: `CHANGELOG.md` only if implementation produced verified public player/server-admin behavior and Tyler approves public release-note wording.
- Modify: release scripts only if loader-specific artifact names changed, and only after proving Fabric publish behavior remains intentional.

- [ ] **Step 1: Record maintainer implementation note**

Add an internal changelog entry for the implemented NeoForge adapter/build and verification status.

- [ ] **Step 2: Defer public release notes**

Do not add a public `0.2.1` section until:

```text
Fabric automated checks pass
NeoForge automated checks pass
Fabric manual regression is recorded
NeoForge manual verification is recorded
loader-specific artifact names and publish paths are approved
Tyler approves release prep
```

- [ ] **Step 3: Prepare implementation closeout only**

Closeout must include:

```text
changed files
Fabric verification commands and results
NeoForge verification commands and results
manual verification evidence paths or notes
Revue review IDs/status
accepted findings actioned
false positives with evidence
remaining risks
confirmation no push/PR/tag/publish/release was performed
```

## Final Acceptance Checklist

- [ ] Existing Fabric jar behavior is preserved.
- [ ] Fabric metadata, entrypoints, mixins, assets, generated `BuildInfo`, optional ModMenu/config UI behavior, and optional Fabric Permissions API behavior are verified.
- [ ] NeoForge jar contains correct metadata, entrypoints/events, mixins or equivalent hooks, assets, generated `BuildInfo`, and loader-specific jar naming/output.
- [ ] Common/commonClient source sets have no Fabric, ModMenu, Fabric Permissions API, or NeoForge imports.
- [ ] Packet channel names, payload fields, byte limits, ModStatus payload behavior, and vanilla fallback semantics remain stable.
- [ ] Common protocol semantics are shared across loaders while Fabric and NeoForge keep loader-specific networking transports.
- [ ] `0.2.1` supports same-loader custom held rendering on Fabric and NeoForge; cross-loader custom payload compatibility is exploratory and not release-blocking.
- [ ] Config paths are correct on both loaders.
- [ ] Permission defaults are equivalent on both loaders, with any NeoForge provider limitation documented.
- [ ] ModStatus states are equivalent on both loaders.
- [ ] Carried-baby render path, vanilla render suppression, first-person large baby visibility, sleepy visuals, and multiplayer observer views are manually verified on both loaders where applicable.
- [ ] `docs/manual-test-plan.md` covers NeoForge verification before release prep.
- [ ] `INTERNAL_CHANGELOG.md` records maintainer-only planning/implementation work.
- [ ] Public `CHANGELOG.md` waits for verified player/server-admin NeoForge support and Tyler approval.
- [ ] No universal jar exists.
- [ ] No reusable library is extracted.

## Tyler Decisions Captured

- Phase 2 should use loader-suffixed artifact names: `carrybabyanimals-${project.version}-fabric.jar` and `carrybabyanimals-${project.version}-neoforge.jar`.
- Normalize remaining Fabric adapter package names to `dev.jasmine.carrybabyanimals.fabric...` / `dev.jasmine.carrybabyanimals.fabric.client...` before adding NeoForge adapter code.
- Seed the plan with NeoForge `26.1.2.74`, and refresh against the official NeoForge changelog/Maven metadata at implementation time.
- Do not add a NeoForge in-game config UI for `0.2.1`; JSON config remains supported.
- Keep common protocol semantics shared and transports loader-specific. Same-loader custom held rendering plus vanilla passenger fallback is required for `0.2.1`; cross-loader custom payload compatibility is exploratory, not release-blocking.
