# Carry Baby Animals

Carry Baby Animals is a Fabric mod for Minecraft 26.1.2 that lets players pick up, carry, pet, and safely put down baby animals.

This is a father-daughter project by Tyler and Jasmine, built around the kind of small Minecraft moment that should feel gentle, useful, and a little bit magical.

The mod is built around a simple idea: baby animals stay as real world entities, not inventory items. A carried baby remains attached to the player on the server, so the behavior is visible, multiplayer-safe, and tolerant of clients that do not have the mod installed.

## Requirements

- Minecraft 26.1.2
- Fabric Loader 0.19.2 or newer
- Java 25 or newer
- Fabric API

Optional:

- Fabric Permissions API is optional, not required. Install it only if you want permission-plugin integration through tools such as LuckPerms.

## Installation

Install Carry Baby Animals on the server to enable the gameplay.

For players and server admins, Carry Baby Animals is server-required and the client mod is highly recommended.

Marketplace environment metadata may list the client as optional because vanilla clients can still join and use the server-side carry behavior.

Players can also install the mod on their clients for the nicer held-in-arms rendering and first-person petting feedback. Players without the mod can still join a modded server and use the carry behavior through the vanilla passenger fallback.

## Features

- Pick up one baby animal at a time by sneak-right-clicking it with empty hands.
- Carry the baby while walking, sprinting, jumping, and swimming.
- Put the baby down again by sneak-right-clicking while carrying with empty hands.
- Pet the carried baby with left-click for heart particles.
- Block normal hand actions while carrying, so carrying feels like it occupies both hands.
- Drop the baby safely if the carry state becomes invalid, including logout, death, dimension changes, growth, or server shutdown.
- Keep a vanilla-compatible fallback by using the passenger system.
- Show a held-in-hands style render on modded clients.

## How To Use

1. Empty both hands.
2. Sneak-right-click a supported baby animal to pick it up.
3. Left-click while carrying to pet the baby.
4. Sneak-right-click again with empty hands to set the baby down.

Pickup and set-down messages use the animal's custom name when it has one, such as `Carrying KittyKat` and `Set down KittyKat`. Unnamed animals use the baby animal type, such as `Carrying baby Pig` and `Set down baby Pig`.

Doors and trapdoors can still be used while carrying a baby animal.

## Client Compatibility

The server owns the gameplay state.

Players with the mod installed get the nicer client-side presentation: the normal passenger render is hidden and the baby is drawn near the carrier's hands.

Players without the mod can still connect to a modded server. They see the carried baby through Minecraft's normal passenger rendering, usually above the player. It is less polished, but the animal remains real, visible, and safe.

## Supported Animals

By default, Carry Baby Animals supports the normal passive baby animals that are reasonable to carry, including animals such as cows, pigs, sheep, chickens, goats, rabbits, cats, foxes, horses, donkeys, llamas, camels, pandas, turtles, and wolves.

Tamed animals follow ownership rules. Players can carry their own tamed baby animals by default. Carrying another player's tamed baby animals is disabled unless the server explicitly allows it.

## Configuration

Carry Baby Animals creates this file the first time the server starts:

```text
config/carrybabyanimals.json
```

Default config:

```jsonc
// Supported animal names: cow, pig, sheep, chicken, goat, rabbit, cat, fox, horse, donkey, mule, llama, trader_llama, camel, panda, turtle, wolf, dog
{
  "allowedAnimals": [],
  "blockedAnimals": [],
  "allowCarryingOtherPlayersTamedAnimals": false,
  "pettingCooldownTicks": 20
}
```

Options:

- `allowedAnimals`: List of animal names that are allowed to be carried. Leave this empty to allow the full default supported set.
- `blockedAnimals`: List of animal names to block. This removes animals from either the default supported set or from `allowedAnimals`.
- `allowCarryingOtherPlayersTamedAnimals`: Allows players to carry another player's tamed baby animal when the permission node also allows it. Default: `false`.
- `pettingCooldownTicks`: Cooldown between successful petting effects, in server ticks. Default: `20`, which is about one second. Values of `0` or lower reset to the default.

Supported animal names:

```text
cow, pig, sheep, chicken, goat, rabbit, cat, fox, horse, donkey, mule, llama, trader_llama, camel, panda, turtle, wolf, dog
```

Animal name notes:

- `dog` means tamed wolves only.
- `wolf` means wolves generally, with normal tamed ownership rules still applied.
- Unknown names are logged and ignored. If an allow list contains only unknown names, carrying stays restricted instead of accidentally allowing everything.
- Carrying occupying the player's hands is part of the mod design and is not configurable.

Example: allow only common farm animals:

```jsonc
// Supported animal names: cow, pig, sheep, chicken, goat, rabbit, cat, fox, horse, donkey, mule, llama, trader_llama, camel, panda, turtle, wolf, dog
{
  "allowedAnimals": ["cow", "pig", "sheep", "chicken", "goat"],
  "blockedAnimals": [],
  "allowCarryingOtherPlayersTamedAnimals": false,
  "pettingCooldownTicks": 20
}
```

Example: allow the default set except turtles and pandas:

```jsonc
// Supported animal names: cow, pig, sheep, chicken, goat, rabbit, cat, fox, horse, donkey, mule, llama, trader_llama, camel, panda, turtle, wolf, dog
{
  "allowedAnimals": [],
  "blockedAnimals": ["turtle", "panda"],
  "allowCarryingOtherPlayersTamedAnimals": false,
  "pettingCooldownTicks": 20
}
```

Example: allow trusted servers to carry other players' tamed babies and slow petting feedback:

```jsonc
// Supported animal names: cow, pig, sheep, chicken, goat, rabbit, cat, fox, horse, donkey, mule, llama, trader_llama, camel, panda, turtle, wolf, dog
{
  "allowedAnimals": [],
  "blockedAnimals": [],
  "allowCarryingOtherPlayersTamedAnimals": true,
  "pettingCooldownTicks": 60
}
```

## Permissions

Carry Baby Animals can integrate with Fabric Permissions API, but Fabric Permissions API is not required. If it is installed, permission providers such as LuckPerms can manage server rules.

If Fabric Permissions API is not installed:

- All players can carry untamed baby animals.
- Players can carry their own tamed baby animals.
- Players cannot carry another player's tamed baby animals.
- The reserved reload permission falls back to vanilla game-master command permission when exposed.

Permission nodes:

```text
carrybabyanimals.carry
carrybabyanimals.carry.tamed
carrybabyanimals.carry.others_tamed
carrybabyanimals.reload
```

Default behavior:

- `carrybabyanimals.carry`: allowed by default.
- `carrybabyanimals.carry.tamed`: allowed by default for the player's own tamed baby animals.
- `carrybabyanimals.carry.others_tamed`: denied by default.
- `carrybabyanimals.reload`: reserved for reload support and defaults to vanilla game-master command permission when exposed.
