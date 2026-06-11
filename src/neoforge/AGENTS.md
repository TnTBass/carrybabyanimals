# Agent Notes for NeoForge Main Adapter

This source root owns NeoForge server/common adapter code and NeoForge jar resources.

- NeoForge entrypoints, lifecycle events, payload registration, recipient discovery, config path lookup, metadata lookup, permission-provider integration or explicit fallback, mixin classes, mixin JSON, `META-INF/neoforge.mods.toml`, and NeoForge assets belong here.
- Keep NeoForge-specific code small and delegate behavior to common services.
- Do not change common behavior or packet semantics to paper over a NeoForge adapter gap.
- Do not add Fabric, ModMenu, or Fabric Permissions API code here.
