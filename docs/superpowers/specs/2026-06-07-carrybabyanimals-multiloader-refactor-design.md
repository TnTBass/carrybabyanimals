# CarryBabyAnimals Multiloader Refactor Design

Date: 2026-06-07
Status: Design/spec artifact only; do not implement until Tyler approves this reviewed spec.
Repository root: `C:\Users\tyler\AI Projects\CarryBabyAnimals`

## Goal

Refactor CarryBabyAnimals in two phases so Fabric remains the first-class, behavior-preserving build while the codebase gains clean common/platform boundaries that make a later NeoForge adapter practical.

Phase 1 is a Fabric-preserving refactor. It isolates Fabric-only code behind boundaries, moves the project into a multiloader-shaped Gradle/source layout, keeps the Fabric jar behavior, and proves the refactor did not change gameplay, networking compatibility, rendering, permissions, config, ModMenu, or ModStatus behavior.

Phase 2 is a later NeoForge adapter/build. It starts only after Phase 1 is stable and verified. This design does not implement NeoForge, does not move to a single universal jar, and does not create a reusable library.

## Non-Goals

- Do not implement production code in this design session.
- Do not restructure Gradle, move source files, add NeoForge dependencies, or create platform modules in this design session.
- Do not design a single jar that runs on both Fabric and NeoForge.
- Do not extract a shared library. Capture reusable rules and templates in `docs/multiloader/` first.
- Do not change player-facing behavior, config defaults, packet channel names, permission defaults, ModMenu behavior, or ModStatus wording in Phase 1.

## Current Fabric Touchpoints

The current project already separates many domain classes from Fabric APIs, but several files mix reusable behavior with Fabric-specific wiring:

- `build.gradle` uses Fabric Loom, `splitEnvironmentSourceSets()`, Fabric dependencies, generated `BuildInfo`, and Fabric release/changelog gates.
- `src/main/resources/fabric.mod.json` owns Fabric metadata, entrypoints, Fabric mixin configs, optional ModMenu, optional Fabric Permissions API, and dependency declarations.
- `src/main/resources/carrybabyanimals.mixins.json` and `src/client/resources/carrybabyanimals.client.mixins.json` are Fabric-loader mixin declarations.
- `CarryBabyAnimals.java` combines mod service construction, config path lookup through `FabricLoader`, Fabric event registration, Fabric networking setup, and lifecycle cleanup hooks.
- `CarryBabyAnimalsClient.java` combines client config loading, Fabric client networking receivers, Fabric connection/tick/pre-attack hooks, ModStatus client status updates, and carried-baby render registration.
- `CarryNetworking.java` combines reusable packet data, Fabric payload registration, Fabric send/can-send logic, `PlayerLookup` recipient discovery, and vanilla passenger packet sync.
- `CarryPermissions.java` uses Fabric Loader to detect Fabric Permissions API and calls `me.lucko.fabric.api.permissions.v0.Permissions`.
- `CarriedBabyRenderer.java` combines Fabric `LevelRenderEvents.COLLECT_SUBMITS` registration with reusable carried-baby frame extraction, visual placement, sleepy pose, and render-state mutation.
- `CarryBabyAnimalsModMenuIntegration.java` is Fabric ModMenu-only.
- `CarryBabyAnimalsModStatus.java` uses Fabric Loader to read the current mod version.
- `ClientCarryVisualConfigManager.java` uses Fabric Loader to resolve `config/carrybabyanimals-client.json`.
- Mixins under `src/main/java/.../mixin` and `src/client/java/.../client/mixin` are loader-declared hooks that should stay adapter-owned even when their helper logic remains common.

## Selected Approach

Use a Fabric-preserving multiloader-shaped refactor in Phase 1.

Phase 1 should include both adapter-boundary extraction and the physical Gradle/source layout needed for long-term multiloader work. The implementation plan should sequence this conservatively: first define the target layout and local `AGENTS.md` guidance, then move common and Fabric-owned code into the new shape, keeping only the Fabric adapter buildable and shippable. The Fabric entrypoints continue to exist and delegate to common bootstrap/services.

Rejected alternatives:

- Boundary-only refactor with the current Fabric project layout: lower short-term churn, but it defers the structural work that future agents need to understand before NeoForge starts.
- NeoForge-first spike: risks designing around an unverified second loader while Fabric regressions are still possible.
- Shared library extraction: premature; this pilot should produce reusable rules/templates first.

## Phase 1 Architecture

Phase 1 should create loader-neutral common code and Fabric adapters inside a multiloader-shaped project while preserving the Fabric jar.

Common/shared should own:

- Mod constants, logger access, service construction, and the domain bootstrap sequence.
- Carry state, carry eligibility, attachment intent, interaction handling, ticking, AI cleanup, cozy feedback, nursery safety, parent reunion, animal alias/config parsing, and most unit-tested logic.
- Packet data semantics: channel identifiers, payload field names, byte limits, encode/decode rules, ModStatus payload encoding, and transport-independent send intents.
- Client render math and state: carried-baby placement, size classification, reactions, sleepy visual phase, effective first-person visibility mode, render-state bookkeeping where it does not require Fabric-only APIs.
- Client config parsing/saving once a platform config-path provider supplies the path.
- ModStatus state/display logic and CarryBabyAnimals-specific ModStatus config once a platform version provider supplies the current mod version/build.

Fabric-specific code should own:

- Fabric `ModInitializer` and `ClientModInitializer` entrypoints.
- Fabric event registration for server tick, player interaction, block break prevention, entity tracking replay/clear, connection join/disconnect, death cleanup, dimension change cleanup, server stopping cleanup, client tick, client connection events, and client pre-attack.
- Fabric custom payload registration, global receivers, `ServerPlayNetworking.canSend/send`, `ClientPlayNetworking.registerGlobalReceiver`, and `PlayerLookup.tracking`.
- Fabric config root lookup through `FabricLoader.getInstance().getConfigDir()`.
- Fabric mod version lookup through `FabricLoader.getInstance().getModContainer(...)`.
- Optional Fabric Permissions API detection and permission checks.
- Optional ModMenu entrypoint and config screen factory.
- Fabric/loader mixin JSON declarations and mixin hook classes.
- Fabric render-event registration and any Fabric-specific render state data key usage that cannot be represented behind a common helper.

## Platform Boundaries

Introduce small boundaries only where the current Fabric imports leak into reusable logic.

Required platform abstractions before NeoForge work:

- `PlatformPaths`: returns server/common config path and client visual config path.
- `PlatformModMetadata`: returns the current mod version and build metadata source needed by ModStatus.
- `PlatformPermissions`: resolves permission nodes and fallback defaults, including the nursery bypass default.
- `PlatformNetworking`: registers payloads/receivers, checks recipient support, sends clientbound/serverbound packets, and discovers loader-specific tracking recipients.
- `PlatformEvents`: registers server/client lifecycle hooks and delegates to common handlers.
- `PlatformRendering`: registers carried-baby render collection and exposes any loader-specific render-state key or submit hook.
- `PlatformOptionalIntegrations`: isolates ModMenu/config UI and optional platform mod detection.

These interfaces should be boring and narrow. They should avoid a generic "platform service locator" that grows into a second framework. Each common service should receive only the dependency it needs.

## Networking Design

Separate payload data from transport.

Common packet/payload layer:

- Defines logical packet names: `set_carried`, `clear_carried`, `pet_carried`, `pet_feedback`, and `server_version`.
- Defines field contracts: baby entity ID, carrier entity ID, no-field pet request, encoded ModStatus server version/status bytes, and the 256-byte ModStatus limit.
- Defines encode/decode behavior and unit tests for payload round trips, byte limits, old/new ModStatus compatibility, and malformed status handling.
- Defines send intents such as "send set-carried to carrier and tracking players", "clear stale carried state for this recipient", "send pet feedback to carrier", and "send server version if supported".

Fabric adapter layer:

- Owns `CustomPacketPayload.Type`, `StreamCodec`, `PayloadTypeRegistry`, `ServerPlayNetworking`, `ClientPlayNetworking`, and `ServerPlayNetworking.canSend`.
- Wraps logical payloads into Fabric payload records without changing channel names.
- Keeps `PlayerLookup.tracking` and connection support checks out of common logic.

Common/server layer:

- Preserves vanilla fallback by keeping passenger sync behavior loader-neutral and independent from optional custom packet support.
- Calls platform networking only for loader-specific tracking-recipient discovery and custom payload support/send operations.

Phase 1 acceptance requires that modded clients still see held renders, clients without the mod stay connected through vanilla passenger fallback, and server-version status remains capability-gated.

## Rendering Design

Rendering should split into event hook, render orchestration, and visual math.

Common/client-shared render logic should own:

- Carried-baby render state lifecycle where possible.
- Size classification, placement, reaction selection, sleepy phase, first-person visibility decisions, and render frame evaluation.
- Tests that do not require a Fabric render event.

Fabric-specific rendering should own:

- `LevelRenderEvents.COLLECT_SUBMITS.register(...)`.
- The Fabric callback signature and `LevelRenderContext` extraction.
- Any Fabric-specific `RenderStateDataKey` handling if a cross-loader equivalent is not available.
- Fabric mixins that mark carried babies and suppress vanilla passenger rendering.

The carried-baby render path is the highest-risk visual area. Phase 1 should keep the existing behavior and move only enough logic to make the boundary obvious. Any future NeoForge render hook should call the same visual-frame service instead of copying the math.

## Config, Permissions, ModMenu, and ModStatus

Server config:

- Common parsing and defaults stay shared.
- Platform adapter supplies the config path. Fabric continues to use `FabricLoader.getConfigDir().resolve("carrybabyanimals.json")`.

Client visual config:

- Common parsing, defaults, and save behavior stay shared.
- Platform adapter supplies `carrybabyanimals-client.json` under the loader config directory.

Permissions:

- Permission node names and default/fallback semantics stay common.
- Fabric adapter keeps Fabric Permissions API detection and Lucko API calls.
- NeoForge later must implement equivalent checks or explicit fallback behavior without changing defaults.

ModMenu/config UI:

- Config screen layout, edit state, and save behavior can stay client common where Minecraft client APIs permit.
- `CarryBabyAnimalsModMenuIntegration` stays Fabric-only.
- NeoForge later should add its own optional config-entry integration only if the target loader ecosystem has a supported equivalent.

ModStatus:

- Internal ModStatusKit helpers remain embedded and common.
- Platform metadata provider supplies current mod version. Generated build metadata remains part of the Fabric build until a later build split defines equivalent generation for NeoForge.
- Networking transport for `server_version` stays platform-specific; payload semantics stay common.

## Gradle and Source Shape

Phase 1 should move to the long-term source/build shape before NeoForge implementation starts. This creates more implementation work in Phase 1, but it prevents a second disruptive move after the boundaries are already in use.

Recommended Phase 1 sequence:

1. Define the target multiloader layout in the implementation plan before moving files.
2. Add scoped `AGENTS.md` files at the roots of the new layout so future agents understand ownership and why the split exists.
3. Create a `common` area for loader-neutral main/client shared logic.
4. Create a `fabric` area for Fabric entrypoints, resources, mixins, networking adapters, permissions adapter, config path provider, render hook registration, and ModMenu adapter.
5. Keep NeoForge as a later adapter/build. Do not add NeoForge dependencies or implement NeoForge entrypoints in Phase 1.
6. Keep the Fabric jar buildable throughout the move, preserving the existing `fabric.mod.json` entrypoint class names unless the implementation plan explicitly updates metadata and tests the jar contents.
7. Use tests and dependency scans to drive Fabric imports out of common packages.
8. Keep separate output jars for Fabric and NeoForge in Phase 2. Do not plan a universal jar.

The Phase 1 implementation plan must define the exact package/source-set or module names before code moves begin. Acceptable shapes include Gradle source sets or Gradle subprojects, but the selected shape must satisfy the same rule: common code cannot depend on Fabric, Fabric owns Fabric metadata/resources, and the only release-ready output after Phase 1 is the Fabric jar.

## AGENTS.md Placement

Phase 1 should add scoped `AGENTS.md` files where the new layout creates non-obvious ownership rules.

Required guidance:

- Root `AGENTS.md`: retain changelog policy and add a short multiloader layout section explaining that common code must stay loader-neutral, Fabric adapter code owns Fabric APIs/resources, NeoForge comes later, and release notes remain governed by the changelog policy.
- Common source root: explain that this area must not import Fabric, ModMenu, Fabric Permissions API, or NeoForge APIs; platform behavior must enter through narrow interfaces.
- Fabric source/resource root: explain that this area owns Fabric entrypoints, mixins, payload registration, optional Fabric integrations, and Fabric jar metadata.
- Future NeoForge source/resource root: add only when that adapter exists, and explain that it must not change common behavior to paper over NeoForge adapter gaps.
- `docs/multiloader/AGENTS.md`: explain that this directory contains reusable migration guidance and should use placeholders or repo-specific notes deliberately.

These files should document architectural ownership, not create broad Superpowers workflow rules.

## Verification Matrix

Fabric behavior is not considered preserved until all applicable checks pass:

- `git rev-parse --show-toplevel` resolves to `C:\Users\tyler\AI Projects\CarryBabyAnimals` before edits.
- `.\gradlew.bat test`
- `.\gradlew.bat build`
- `.\gradlew.bat checkChangelog`
- Static scan confirms common packages contain no imports of `net.fabricmc`, `fabric.api`, `com.terraformersmc.modmenu`, or `me.lucko.fabric`.
- Jar/resource inspection showing Fabric metadata, mixin configs, generated `BuildInfo`, icon/assets, and entrypoint classes are present in the Fabric jar.
- Packet compatibility tests proving channel names and payload contracts did not change.
- Manual singleplayer carry pass from `docs/manual-test-plan.md`.
- Manual dedicated-server two-modded-client pass covering pickup, tracking replay, held render, vanilla render suppression, pet feedback, disconnect cleanup, and drop cleanup.
- Manual server with one modded client and one client without this mod, proving no custom payload requirement and vanilla passenger fallback.
- Manual permissions pass with Fabric Permissions API and a provider such as LuckPerms.
- Manual ModMenu/config pass proving optional ModMenu still opens the config screen, saves client config, and absence of ModMenu does not prevent loading.
- Manual ModStatus pass proving matched, build-different, version-different, server-not-detected, and disconnect states still display as before.

## Migration Packet Use

The reusable packet under `docs/multiloader/` is part of this pilot. It should be copied into Tyler's other mod repos as a checklist, not treated as a library contract. Each repo must re-run touchpoint discovery and review its own networking, render, config, optional integrations, and verification matrix.

## Risks and Mitigations

- Risk: common packages accidentally keep Fabric imports. Mitigation: add a static scan gate before NeoForge work begins.
- Risk: packet channel names or support checks drift. Mitigation: lock channel names and byte limits in tests before moving transport code.
- Risk: carried-baby rendering regresses visually. Mitigation: treat render hook isolation as high risk and require manual multiplayer visual passes.
- Risk: optional integrations become required. Mitigation: keep Fabric Permissions API and ModMenu behind optional adapter checks.
- Risk: Gradle split churn masks behavior changes. Mitigation: sequence the Phase 1 implementation plan so the target layout, AGENTS guidance, dependency scans, jar inspection, and manual Fabric verification are explicit gates.

## Tyler Decisions Captured

- Phase 1 should include the physical multiloader-shaped Gradle/source layout, not just adapter boundaries inside the current Fabric project.
- Add scoped `AGENTS.md` files where the layout creates non-obvious ownership rules.

## Open Questions for Tyler Before Implementation Planning

- For the other two mods, should the migration packet be copied verbatim first, or should each repo get a tailored packet derived from this one during its own design session?
- Should NeoForge parity target the same Minecraft version as the current Fabric build immediately, or should the NeoForge phase wait until the next version alignment window?

## Stop Rule

After this spec and migration packet are reviewed and cleaned, stop. Do not invoke implementation planning and do not begin code changes until Tyler explicitly approves the reviewed spec.
