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
- Nursery Mode refuses dangerous set-downs near lava, fire, damaging blocks, cramped spaces, and unsafe drops.
- Parent Reunion adds cosmetic hearts and a warm action-bar message when a safely set-down baby is returned near a matching adult.
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

If Nursery Mode refuses a set-down, the baby stays carried and the player sees a playful action-bar message when Nursery Mode messages are enabled.

If Parent Reunion finds a compatible adult after a safe set-down, the baby and adult get heart particles and the player sees a short reunion message when those options are enabled.

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
  "pettingCooldownTicks": 20,
  "cozyFeedbackEnabled": true,
  "carriedIdleSoundsEnabled": true,
  "carriedIdleSoundMinTicks": 160,
  "carriedIdleSoundMaxTicks": 360,
  "pettingMessagesEnabled": true,
  "nameAwareMessagesEnabled": true,
  "cozyParticlesEnabled": true,
  "sleepyBabiesEnabled": true,
  "sleepyAfterTicks": 1200,
  "sleepyMessageCooldownTicks": 600,
  "sleepyParticleCooldownTicks": 200,
  "nurseryModeEnabled": true,
  "nurseryBlockLava": true,
  "nurseryBlockFire": true,
  "nurseryBlockCactusAndDamage": true,
  "nurseryBlockSuffocation": true,
  "nurseryBlockDangerousFalls": true,
  "nurseryDangerousFallDistanceBlocks": 4,
  "nurseryMessagesEnabled": true,
  "parentReunionEnabled": true,
  "parentReunionRadiusBlocks": 8,
  "parentReunionCooldownTicks": 100,
  "parentReunionMessagesEnabled": true,
  "parentReunionParticlesEnabled": true
}
```

Options:

- `allowedAnimals`: List of animal names that are allowed to be carried. Leave this empty to allow the full default supported set.
- `blockedAnimals`: List of animal names to block. This removes animals from either the default supported set or from `allowedAnimals`.
- `allowCarryingOtherPlayersTamedAnimals`: Allows players to carry another player's tamed baby animal when the permission node also allows it. Default: `false`.
- `pettingCooldownTicks`: Cooldown between successful petting effects, in server ticks. Default: `20`, which is about one second. Values of `0` or lower reset to the default.
- `cozyFeedbackEnabled`: Master switch for cosmetic carried-baby feedback. Default: `true`.
- `carriedIdleSoundsEnabled`: Allows occasional carried-baby ambient sounds. Default: `true`.
- `carriedIdleSoundMinTicks`: Minimum delay between carried idle sounds. Default: `160`, which is about eight seconds.
- `carriedIdleSoundMaxTicks`: Maximum delay between carried idle sounds. Default: `360`, which is about eighteen seconds. Values below the minimum are raised to the minimum.
- `pettingMessagesEnabled`: Enables varied petting and sleepy action-bar messages. Default: `true`.
- `nameAwareMessagesEnabled`: Uses a baby's custom name in cozy feedback messages when it has one. Default: `true`.
- `cozyParticlesEnabled`: Enables gentle cosmetic carried-baby feedback particles. Default: `true`.
- `sleepyBabiesEnabled`: Enables sleepy carried-baby moments after a baby has been held for a while. Default: `true`.
- `sleepyAfterTicks`: Minimum carried duration before sleepy moments can start. Default: `1200`, which is about one minute.
- `sleepyMessageCooldownTicks`: Minimum spacing between sleepy action-bar messages for one carried baby. Default: `600`, which is about thirty seconds.
- `sleepyParticleCooldownTicks`: Minimum spacing between sleepy particle effects for one carried baby. Default: `200`, which is about ten seconds.
- `nurseryModeEnabled`: Master switch for refusing unsafe player set-downs. Default: `true`.
- `nurseryBlockLava`: Refuses set-downs in or next to lava. Default: `true`.
- `nurseryBlockFire`: Refuses set-downs on fire, campfires, soul campfires, magma blocks, and similar burning hazards. Default: `true`.
- `nurseryBlockCactusAndDamage`: Refuses set-downs on cactus and other obvious damaging blocks. Default: `true`.
- `nurseryBlockSuffocation`: Refuses cramped set-downs where the baby would collide or suffocate. Default: `true`.
- `nurseryBlockDangerousFalls`: Refuses set-downs over unsafe drops. Default: `true`.
- `nurseryDangerousFallDistanceBlocks`: Drop distance that counts as unsafe for Nursery Mode. Default: `4`.
- `nurseryMessagesEnabled`: Enables playful Nursery Mode refusal action-bar messages. Default: `true`.
- `parentReunionEnabled`: Master switch for cosmetic Parent Reunion feedback after safe player set-downs. Default: `true`.
- `parentReunionRadiusBlocks`: Search radius for a compatible adult near the set-down baby. Default: `8`.
- `parentReunionCooldownTicks`: Minimum spacing between reunion feedback for the same baby or carrier. Default: `100`, which is about five seconds.
- `parentReunionMessagesEnabled`: Enables warm Parent Reunion action-bar messages. Default: `true`.
- `parentReunionParticlesEnabled`: Enables heart particles around the baby and adult. Default: `true`.

Cozy Feedback is cosmetic and server-owned. It uses ordinary Minecraft sounds, particles, and action-bar messages, so vanilla clients can see or hear the feedback without installing the mod.

Nursery Mode is also server-owned and vanilla-client compatible. It validates the server's planned set-down position before the baby is detached. If the spot is unsafe, the carry state remains active and no client-side mod is required for the refusal to work.

Parent Reunion is server-owned, cosmetic, and vanilla-client compatible. It only runs after Nursery Mode allows a deliberate set-down and never moves the baby or adult. Compatible adults are the same animal type, adult, alive, nearby, and in the same loaded server level. Tamed animals only reunite with adults that have the same owner identity.

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
  "pettingCooldownTicks": 20,
  "cozyFeedbackEnabled": true,
  "carriedIdleSoundsEnabled": true,
  "carriedIdleSoundMinTicks": 160,
  "carriedIdleSoundMaxTicks": 360,
  "pettingMessagesEnabled": true,
  "nameAwareMessagesEnabled": true,
  "cozyParticlesEnabled": true,
  "sleepyBabiesEnabled": true,
  "sleepyAfterTicks": 1200,
  "sleepyMessageCooldownTicks": 600,
  "sleepyParticleCooldownTicks": 200,
  "nurseryModeEnabled": true,
  "nurseryBlockLava": true,
  "nurseryBlockFire": true,
  "nurseryBlockCactusAndDamage": true,
  "nurseryBlockSuffocation": true,
  "nurseryBlockDangerousFalls": true,
  "nurseryDangerousFallDistanceBlocks": 4,
  "nurseryMessagesEnabled": true,
  "parentReunionEnabled": true,
  "parentReunionRadiusBlocks": 8,
  "parentReunionCooldownTicks": 100,
  "parentReunionMessagesEnabled": true,
  "parentReunionParticlesEnabled": true
}
```

Example: allow the default set except turtles and pandas:

```jsonc
// Supported animal names: cow, pig, sheep, chicken, goat, rabbit, cat, fox, horse, donkey, mule, llama, trader_llama, camel, panda, turtle, wolf, dog
{
  "allowedAnimals": [],
  "blockedAnimals": ["turtle", "panda"],
  "allowCarryingOtherPlayersTamedAnimals": false,
  "pettingCooldownTicks": 20,
  "cozyFeedbackEnabled": true,
  "carriedIdleSoundsEnabled": true,
  "carriedIdleSoundMinTicks": 160,
  "carriedIdleSoundMaxTicks": 360,
  "pettingMessagesEnabled": true,
  "nameAwareMessagesEnabled": true,
  "cozyParticlesEnabled": true,
  "sleepyBabiesEnabled": true,
  "sleepyAfterTicks": 1200,
  "sleepyMessageCooldownTicks": 600,
  "sleepyParticleCooldownTicks": 200,
  "nurseryModeEnabled": true,
  "nurseryBlockLava": true,
  "nurseryBlockFire": true,
  "nurseryBlockCactusAndDamage": true,
  "nurseryBlockSuffocation": true,
  "nurseryBlockDangerousFalls": true,
  "nurseryDangerousFallDistanceBlocks": 4,
  "nurseryMessagesEnabled": true,
  "parentReunionEnabled": true,
  "parentReunionRadiusBlocks": 8,
  "parentReunionCooldownTicks": 100,
  "parentReunionMessagesEnabled": true,
  "parentReunionParticlesEnabled": true
}
```

Example: allow trusted servers to carry other players' tamed babies and slow petting feedback:

```jsonc
// Supported animal names: cow, pig, sheep, chicken, goat, rabbit, cat, fox, horse, donkey, mule, llama, trader_llama, camel, panda, turtle, wolf, dog
{
  "allowedAnimals": [],
  "blockedAnimals": [],
  "allowCarryingOtherPlayersTamedAnimals": true,
  "pettingCooldownTicks": 60,
  "cozyFeedbackEnabled": true,
  "carriedIdleSoundsEnabled": true,
  "carriedIdleSoundMinTicks": 160,
  "carriedIdleSoundMaxTicks": 360,
  "pettingMessagesEnabled": true,
  "nameAwareMessagesEnabled": true,
  "cozyParticlesEnabled": true,
  "sleepyBabiesEnabled": true,
  "sleepyAfterTicks": 1200,
  "sleepyMessageCooldownTicks": 600,
  "sleepyParticleCooldownTicks": 200,
  "nurseryModeEnabled": true,
  "nurseryBlockLava": true,
  "nurseryBlockFire": true,
  "nurseryBlockCactusAndDamage": true,
  "nurseryBlockSuffocation": true,
  "nurseryBlockDangerousFalls": true,
  "nurseryDangerousFallDistanceBlocks": 4,
  "nurseryMessagesEnabled": true,
  "parentReunionEnabled": true,
  "parentReunionRadiusBlocks": 8,
  "parentReunionCooldownTicks": 100,
  "parentReunionMessagesEnabled": true,
  "parentReunionParticlesEnabled": true
}
```

## Permissions

Carry Baby Animals can integrate with Fabric Permissions API, but Fabric Permissions API is not required. If it is installed, permission providers such as LuckPerms can manage server rules.

If Fabric Permissions API is installed:

- `carrybabyanimals.nursery.bypass` defaults to disabled unless a permission provider explicitly grants it.
- All other Carry Baby Animals permissions delegate to your permission provider and use the defaults listed below when the provider has no explicit rule.

If Fabric Permissions API is not installed:

- All players can carry untamed baby animals.
- Players can carry their own tamed baby animals.
- Players cannot carry another player's tamed baby animals.
- The reserved reload permission falls back to vanilla game-master command permission when exposed.
- The Nursery Mode bypass permission falls back to vanilla game-master command permission.

Permission nodes:

```text
carrybabyanimals.carry
carrybabyanimals.carry.tamed
carrybabyanimals.carry.others_tamed
carrybabyanimals.nursery.bypass
carrybabyanimals.reload
```

Default behavior:

- `carrybabyanimals.carry`: allowed by default.
- `carrybabyanimals.carry.tamed`: allowed by default for the player's own tamed baby animals.
- `carrybabyanimals.carry.others_tamed`: denied by default.
- `carrybabyanimals.nursery.bypass`: defaults to disabled with Fabric Permissions API installed; without Fabric Permissions API it falls back to vanilla game-master command permission.
- `carrybabyanimals.reload`: reserved for reload support and defaults to vanilla game-master command permission when exposed.
