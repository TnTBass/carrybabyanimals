# Agent Notes for Common Client Code

This source root is loader-neutral client logic for CarryBabyAnimals.

- Do not import Fabric, ModMenu, Fabric Permissions API, NeoForge, or future loader APIs here.
- Keep render math, visual-frame evaluation, client config parsing, ModStatus client state, and interaction intents reusable.
- Loader callback registration, networking send/receive calls, config path lookup, render events, render-state keys, and optional config-entry integrations belong in loader adapter roots.
