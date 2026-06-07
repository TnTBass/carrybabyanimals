# Fabric Touchpoints

This inventory is specific to CarryBabyAnimals as of 2026-06-07.

## Build and Resources

- `build.gradle`: Fabric Loom, Fabric Loader/API dependencies, split environment source sets, generated `BuildInfo`, Fabric release/changelog gates.
- `gradle.properties`: Minecraft, Fabric Loader, Fabric API, ModMenu, mod version, Maven group, archive name.
- `src/main/resources/fabric.mod.json`: Fabric metadata, entrypoints, dependency declarations, optional suggestions, mixin config declarations.
- `src/main/resources/carrybabyanimals.mixins.json`: server/common mixin declaration.
- `src/client/resources/carrybabyanimals.client.mixins.json`: client mixin declarations.

## Entrypoints and Lifecycle

- `CarryBabyAnimals.java`: Fabric `ModInitializer`, server config path lookup, event registrations, networking registration, tracking replay, connection lifecycle, shutdown cleanup.
- `CarryBabyAnimalsClient.java`: Fabric `ClientModInitializer`, client networking receivers, client tick, connection lifecycle, pre-attack hook, render registration.

## Networking

- `CarryNetworking.java`: Fabric payload registration, Fabric payload records/codecs, serverbound receiver, send/can-send logic, Fabric `PlayerLookup`, vanilla passenger packet recipients.
- `CarryBabyAnimalsClient.java`: Fabric client global receivers for set/clear carried state, pet feedback, and server version payloads.

## Permissions and Optional Integrations

- `CarryPermissions.java`: Fabric Loader optional mod detection and Fabric Permissions API calls.
- `CarryBabyAnimalsModMenuIntegration.java`: ModMenu entrypoint and config screen factory.
- `CarryBabyAnimalsModStatus.java`: Fabric Loader mod metadata lookup for current version.
- `ClientCarryVisualConfigManager.java`: Fabric Loader config directory lookup.

## Rendering and Mixins

- `CarriedBabyRenderer.java`: Fabric `LevelRenderEvents.COLLECT_SUBMITS` registration and Fabric render context extraction.
- `CarriedBabyRenderState.java`: Fabric client render state data key import.
- `EntityStartRidingMixin.java`: loader-declared mixin for passenger attachment compatibility.
- `LivingEntityRendererMixin.java`: loader-declared mixin for carried-baby render suppression.
- `PlayerModelMixin.java`: loader-declared mixin for carrier arm pose.

## Common-Looking Areas

These areas appear reusable after adapter boundaries are introduced:

- Carry state and interaction services.
- Config parsing and validation.
- Nursery safety and parent reunion.
- Cozy feedback.
- ModStatus helper logic and display state.
- Client render math, placement, size classification, reactions, and sleepy visual decisions.
- Unit tests around pure logic.
