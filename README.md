# Carry Baby Animals

Carry Baby Animals is a Fabric mod for Minecraft 26.1.2 that lets players pick up, carry, pet, and safely put down baby animals.

The mod is built around a simple idea: baby animals stay as real world entities, not inventory items. A carried baby remains attached to the player on the server, so the behavior is visible, multiplayer-safe, and tolerant of clients that do not have the mod installed.

## Features

- Pick up one baby animal at a time with empty hands.
- Carry the baby while walking, sprinting, jumping, and swimming.
- Put the baby down again with empty hands and sneak-use.
- Pet the carried baby with left-click for heart particles.
- Block normal hand actions while carrying, so carrying feels like it occupies both hands.
- Drop the baby safely if the carry state becomes invalid, including logout, death, dimension changes, growth, or server shutdown.
- Keep a vanilla-compatible fallback by using the passenger system.
- Show a held-in-hands style render on modded clients.

## Client Compatibility

The server owns the gameplay state.

Players with the mod installed get the nicer client-side presentation: the normal passenger render is hidden and the baby is drawn near the carrier's hands.

Players without the mod can still connect to a modded server. They see the carried baby through Minecraft's normal passenger rendering, usually above the player. It is less polished, but the animal remains real, visible, and safe.

## Supported Animals

By default, Carry Baby Animals supports the normal passive baby animals that are reasonable to carry, including animals such as cows, pigs, sheep, chickens, goats, rabbits, cats, foxes, horses, donkeys, llamas, camels, pandas, turtles, and wolves.

Tamed animals follow ownership rules. Players can carry their own tamed baby animals by default. Carrying another player's tamed baby animals is disabled unless the server explicitly allows it.

## Configuration

Server owners can configure friendly animal names in:

```json
config/carrybabyanimals.json
```

Example:

```json
{
  "allowedAnimals": ["cow", "pig", "sheep", "chicken", "goat", "cat", "dog"],
  "blockedAnimals": [],
  "allowCarryingOtherPlayersTamedAnimals": false,
  "pettingCooldownTicks": 20
}
```

Notes:

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

## Project Status

Carry Baby Animals is early and should be tested on a non-production world before being used on a long-running survival server. The core design prioritizes animal safety, server authority, and graceful fallback behavior.
