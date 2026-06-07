# Agent Notes for Fabric Main Adapter

This source root owns Fabric server/common adapter code and Fabric jar resources.

- Fabric entrypoints, lifecycle events, payload registration, recipient discovery, config path lookup, metadata lookup, Fabric Permissions API checks, mixin classes, mixin JSON, `fabric.mod.json`, and Fabric assets belong here.
- Keep Fabric-specific code small and delegate behavior to common services.
- Do not add NeoForge code or dependencies here.
