# Agent Notes for NeoForge Client Adapter

This source root owns NeoForge client adapter code.

- NeoForge client setup, client networking receivers, client config path lookup, render hook registration, NeoForge render-state equivalents, and client mixins or event hooks belong here.
- Keep NeoForge-specific code small and delegate render math, config state, and ModStatus display behavior to common client code.
- Do not change common behavior or Fabric adapter behavior to make NeoForge compile.
- Do not add Fabric, ModMenu, or Fabric Permissions API code here.
