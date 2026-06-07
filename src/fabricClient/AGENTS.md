# Agent Notes for Fabric Client Adapter

This source root owns Fabric client adapter code.

- Fabric client entrypoints, client networking receivers, ModMenu integration, client config path lookup, render hook registration, Fabric render-state keys, client mixins, and client mixin JSON belong here.
- Keep Fabric-specific code small and delegate render math, config state, and ModStatus display behavior to common client code.
- Do not add NeoForge code or dependencies here.
