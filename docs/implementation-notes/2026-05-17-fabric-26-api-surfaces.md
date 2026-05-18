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

## Task 4 Carry Eligibility Substitutions

- Use `net.minecraft.resources.Identifier` instead of the plan's `net.minecraft.resources.ResourceLocation`.
- `EntityType.getKey(entity.getType())` returns `Identifier` in this repo's Minecraft 26.1.2 generated sources.
- Use `TamableAnimal#isOwnedBy(LivingEntity)` instead of the older `getOwnerUUID()` owner check.
- Use `ServerPlayer#permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)` instead of the older `hasPermissions(2)` command-level check.

## Task 5 Passenger Attachment Substitutions

- Use `Entity#startRiding(Entity, boolean force, boolean sendEventAndTriggers)` instead of the plan's two-argument `startRiding(Entity, boolean)` overload.
- Use `Entity#snapTo(double, double, double, float, float)` instead of the older `moveTo(double, double, double, float, float)` repositioning call.
