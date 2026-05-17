# Fabric 26.1.2 API Surfaces

## Confirmed Build Inputs

- Minecraft: 26.1.2
- Java: 25
- Fabric Loader: 0.19.2
- Fabric API: 0.149.0+26.1.2
- Mappings: no explicit mappings dependency; Loom generated non-obfuscated Minecraft 26.1.2 sources

## Passenger Fallback

Use the vanilla passenger system. The carried baby rides the player while carried. Vanilla clients see the baby above the player's head. Modded clients suppress the vanilla render and render the baby at the player's hand position.

## Growth Detection

Record whether Fabric 26.x exposes a baby-to-adult growth event here during implementation. If no event exists, use the `CarryTicker` tick-check: every server tick, inspect carried animals and drop them when `AgeableMob#isBaby()` becomes false. This can allow a one-tick race window between growth and drop; the race is acceptable because the next tick drops the animal safely.

## Interaction Hooks

Record the final hook names used for:

- Sneak-right-click entity interaction.
- Left-click attack interception while carrying.
- Use-item/block interaction blocking while carrying.
- Logout, death, dimension change, and server stop cleanup.

## Renderer Hooks

Record the final hook used to suppress vanilla rendering for carried baby passengers and the renderer path used for the held-in-hands replacement.
