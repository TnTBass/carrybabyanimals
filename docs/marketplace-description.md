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

Players can also install the mod on their clients for the held-in-arms rendering and first-person petting feedback. Players without the mod can still join and use the carry behavior, but they will see the baby animal through Minecraft's normal passenger rendering.

## Requirements

- Minecraft 26.1.2
- Fabric Loader 0.19.2 or newer
- Java 25 or newer
- Fabric API
- Fabric Permissions API

## Configuration

Server owners can configure allowed and blocked baby animals in `config/carrybabyanimals.json`. New default config files include a comment listing every supported animal name.

The mod also supports Fabric Permissions API for carry permissions, including separate rules for tamed baby animals and other players' tamed baby animals.
