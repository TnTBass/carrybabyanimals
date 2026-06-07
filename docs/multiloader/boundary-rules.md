# Boundary Rules

## General Rules

- Common packages must not import `net.fabricmc`, `fabric.api`, `com.terraformersmc.modmenu`, or `me.lucko.fabric`.
- Common packages may use Minecraft game classes when the logic is genuinely loader-neutral for the target Minecraft version.
- Platform adapters own loader entrypoints, metadata, event buses, networking registration, send/can-send checks, config roots, permissions provider integration, optional config UI registration, and render hook registration.
- Payload semantics are common; packet transport is platform-specific.
- Mixins and loader metadata stay platform-specific even when they delegate to common helpers.
- Optional integrations must remain optional.

## Naming and Ownership

- Keep common services named by behavior, not by loader.
- Keep adapter classes named by loader and responsibility, such as Fabric networking, Fabric paths, Fabric permissions, and Fabric rendering.
- Do not introduce a large global platform facade unless a smaller injected interface cannot express the dependency.
- Do not let the second loader change public behavior during Phase 1.

## Networking Rules

- Channel names must stay stable.
- Byte limits must stay stable.
- Common code defines payload fields and validation.
- Platform code defines loader codecs and registration.
- Platform code owns recipient discovery and capability checks.
- Vanilla fallback must not depend on custom payload support.

## Rendering Rules

- Common render code may calculate visual frames and state transitions.
- Platform render code registers callbacks and extracts loader-specific render context.
- Loader-specific render-state keys and mixin declarations stay adapter-owned unless a truly loader-neutral equivalent exists.
- Manual visual verification is required for carried-baby rendering changes.

## Config and Metadata Rules

- Common config code parses, validates, writes defaults, and normalizes values.
- Platform code supplies config paths.
- Common ModStatus code formats and evaluates status.
- Platform code supplies current mod version metadata.
- Generated build metadata must exist for each loader jar before that loader is release-ready.
