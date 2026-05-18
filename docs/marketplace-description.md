# Carry Baby Animals

Carry Baby Animals is a Fabric mod for Minecraft 26.1.2 that lets players pick up, carry, pet, and safely put down baby animals.

This is a father-daughter project by Tyler and Jasmine, built around the kind of small Minecraft moment that should feel gentle, useful, and a little bit magical.

## What It Does

- Pick up one baby animal at a time by sneak-right-clicking it with empty hands.
- Carry the baby while walking, sprinting, jumping, and swimming.
- Put the baby down again by sneak-right-clicking while carrying with empty hands.
- Pet the carried baby with left-click for heart particles and a little action-bar message.
- Keep baby animals as real world entities instead of inventory items.
- Let vanilla clients still use the server-side carry behavior through the passenger fallback.
- Show a nicer held-in-arms render for players who install the mod on their client.

## Server And Client Setup

Install Carry Baby Animals on the server to enable the gameplay.

For players and server admins, Carry Baby Animals is server-required and the client mod is highly recommended.

Marketplace environment metadata may list the client as optional because vanilla clients can still join and use the server-side carry behavior.

Players can also install the mod on their clients for the held-in-arms rendering and first-person petting feedback. Players without the mod can still join and use the carry behavior, but they will see the baby animal through Minecraft's normal passenger rendering.

## Requirements

- Minecraft 26.1.2
- Fabric Loader 0.19.2 or newer
- Java 25 or newer
- Fabric API

Optional:

- Fabric Permissions API, if you want permission-plugin integration through tools such as LuckPerms.

## Configuration

Carry Baby Animals creates `config/carrybabyanimals.json` the first time the server starts.

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

Configuration options:

- `allowedAnimals`: List of animal names that are allowed to be carried. Leave this empty to allow the full default supported set.
- `blockedAnimals`: List of animal names to block. This removes animals from either the default supported set or from `allowedAnimals`.
- `allowCarryingOtherPlayersTamedAnimals`: Allows players to carry another player's tamed baby animal when the permission node also allows it. Default: `false`.
- `pettingCooldownTicks`: Cooldown between successful petting effects, in server ticks. Default: `20`, which is about one second. Values of `0` or lower reset to the default.

Supported animal names:

```text
cow, pig, sheep, chicken, goat, rabbit, cat, fox, horse, donkey, mule, llama, trader_llama, camel, panda, turtle, wolf, dog
```

`dog` means tamed wolves only. `wolf` means wolves generally, with normal tamed ownership rules still applied.

The mod can also use Fabric Permissions API for carry permissions, including separate rules for tamed baby animals and other players' tamed baby animals.

If Fabric Permissions API is not installed:

- All players can carry untamed baby animals.
- Players can carry their own tamed baby animals.
- Players cannot carry another player's tamed baby animals.
- The reserved reload permission falls back to vanilla game-master command permission when exposed.
