# Agent Notes for Common Code

This source root is loader-neutral CarryBabyAnimals code.

- Do not import Fabric, ModMenu, Fabric Permissions API, NeoForge, or future loader APIs here.
- Platform behavior must enter through narrow interfaces under `dev.jasmine.carrybabyanimals.platform`.
- Common code may use Minecraft classes only when the behavior is loader-neutral for the target Minecraft version.
- Payload semantics, config parsing, carry behavior, permissions defaults, ModStatus display rules, and testable render math belong here.
