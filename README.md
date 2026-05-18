# Carry Baby Animals

Carry Baby Animals is a Fabric mod for Minecraft 26.1.2 that lets players pick up, carry, pet, and safely put down baby animals.

This is a father-daughter project by Tyler and Jasmine, built around the kind of small Minecraft moment that should feel gentle, useful, and a little bit magical.

The mod is built around a simple idea: baby animals stay as real world entities, not inventory items. A carried baby remains attached to the player on the server, so the behavior is visible, multiplayer-safe, and tolerant of clients that do not have the mod installed.

## Requirements

- Minecraft 26.1.2
- Fabric Loader 0.19.2 or newer
- Java 25 or newer
- Fabric API
- Fabric Permissions API

## Installation

Install Carry Baby Animals on the server to enable the gameplay.

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

Server owners can configure friendly animal names in:

```text
config/carrybabyanimals.json
```

Example:

```jsonc
// Supported animal names: cow, pig, sheep, chicken, goat, rabbit, cat, fox, horse, donkey, mule, llama, trader_llama, camel, panda, turtle, wolf, dog
{
  "allowedAnimals": ["cow", "pig", "sheep", "chicken", "goat", "cat", "dog"],
  "blockedAnimals": [],
  "allowCarryingOtherPlayersTamedAnimals": false,
  "pettingCooldownTicks": 20
}
```

Notes:

- New default config files include a comment listing every supported animal name.
- `allowedAnimals` restricts carrying to the listed animals. Leave it empty to use the default supported set.
- `blockedAnimals` removes animals from the default or allowed set.
- `dog` means tamed wolves only.
- `wolf` means wolves generally, with normal tamed ownership rules still applied.
- Unknown names are logged and ignored. If an allow list contains only unknown names, carrying stays restricted instead of accidentally allowing everything.
- Carrying occupying the player's hands is part of the mod design and is not configurable.

## Permissions

Carry Baby Animals integrates with Fabric Permissions API, so permission providers such as LuckPerms can manage server rules.

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
