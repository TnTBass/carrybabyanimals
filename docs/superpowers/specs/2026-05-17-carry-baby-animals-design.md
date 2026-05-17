# CarryBabyAnimals Design

## Summary

CarryBabyAnimals is a Fabric mod for Minecraft 26.1.2 on Java 25. It lets players pick up and carry baby animals without turning them into inventory items. The mod should feel playful and kid-friendly while still respecting survival gameplay and server rules.

The target fantasy is simple: a player can scoop up a baby animal, run and jump around with it, pet it, and safely put it back down. Modded clients should see the baby held in the player's hands. Vanilla clients should still see a real baby animal attached near the player, even if the fallback pose is less polished.

## Goals

- Let players carry one baby animal at a time.
- Keep the carried baby as a real entity, not an item or inventory stack.
- Make carrying visible to other players.
- Prefer a held-in-hands render for modded clients.
- Provide a vanilla-client fallback that is visible, safe, and non-duplicating.
- Keep running, jumping, and normal movement available while carrying.
- Treat carrying as occupying the player's hands.
- Allow petting the carried baby for cosmetic heart particles.
- Support all technically reasonable vanilla passive baby mobs by default.
- Provide human-readable JSON config for limiting or changing behavior.
- Integrate with Fabric Permissions API so LuckPerms can manage server rules.

## Non-Goals

- No hostile mobs, villagers, or complex non-animal entities in v1.
- No carrying multiple animals at once.
- No inventory storage for animals.
- No mechanical benefits from petting.
- No guaranteed perfect visual pose for vanilla clients.
- No reconnect/session persistence for an animal still being carried.

## Gameplay

Players pick up an eligible baby animal by sneak-right-clicking it with empty hands. Players put the animal down by sneak-right-clicking again while carrying. Drop placement should be safe and predictable, preferably just in front of the player. If a player is already carrying a baby and sneak-right-clicks another eligible baby, the new pickup is ignored. The player must drop the current animal first.

Movement remains normal while carrying. Players can run, jump, swim, and travel naturally. Carrying occupies the player's hands, so normal hand actions are blocked while carrying. Attacking, placing blocks, eating, using bows, and similar item actions should not work until the animal is dropped.

All left-clicks while carrying are intercepted. Attacks on any target are blocked, and petting fires instead. Petting has no effect on health, taming, breeding, loyalty, age, AI, or other gameplay state. It only creates affectionate feedback:

- Server-side heart particles spawn around the carried baby.
- Nearby players can see the hearts.
- A short cooldown, around one second, prevents spam.
- A soft vanilla sound can be added if it feels good, but particles alone are enough for v1.

The carried animal automatically drops when the state becomes unsafe or invalid. Auto-drop cases include player death, logout, dimension change, animal growth, entity removal, invalid carry state, or a rule change that no longer allows the carry.

## Compatibility Model

The server owns the gameplay truth. It tracks which player is carrying which baby, validates every pickup and drop, blocks hand actions, and keeps the baby attached to the player.

The visual model is hybrid:

- Modded clients render the baby in a held-in-hands position relative to the player model.
- Vanilla clients see the real baby riding the player through Minecraft's passenger system.
- The fallback prioritizes correctness over beauty: the animal must be real, visible, not duplicated, and not lost.

The passenger system is the committed vanilla fallback. While carried, the baby rides the player, which suppresses the baby's normal AI through passenger mechanics. Vanilla clients are expected to see the baby above the player's head. This is not a perfect carrying visual, but it is acceptable because it is safe, visible, server-authoritative, and non-duplicating.

The server should not require every connecting client to have the mod. Client-side rendering is an upgrade, not the source of truth. Gameplay must remain correct when some players use vanilla clients.

## Animal Eligibility

By default, the mod supports all vanilla passive baby mobs that are technically reasonable in Minecraft 26.1.2. The implementation plan should verify the final list against the actual entity classes and behavior for that version instead of relying on older-version assumptions.

Eligibility checks should require that the entity:

- Is a baby.
- Is passive or otherwise explicitly allowed.
- Is in the configured allowed set.
- Is not in the configured blocked set.
- Is not already being carried.
- Is not an excluded edge case for v1.
- Satisfies tamed ownership rules, if applicable.
- Passes permission checks for the player.

Tamed animals are allowed when they belong to the carrying player. Carrying another player's tamed baby animals is disabled by default and configurable.

## Configuration

Config should be human-readable JSON. Server owners should be able to use friendly names such as `cow`, `pig`, `sheep`, `chicken`, `goat`, `cat`, and `dog`, rather than needing obscure or full internal entity IDs for normal vanilla animals.

Example shape:

```json
{
  "allowedAnimals": ["cow", "pig", "sheep", "chicken", "goat", "cat", "dog"],
  "blockedAnimals": [],
  "allowCarryingOtherPlayersTamedAnimals": false,
  "pettingCooldownTicks": 20
}
```

Internally, the mod maps friendly aliases to canonical entity IDs. `dog` maps to tamed wolves only and `wolf` maps to all wolves including wild wolves; both resolve to `minecraft:wolf` internally, and tamed ownership rules determine final eligibility. Unknown names should be logged clearly and ignored rather than crashing the server.

`carryingOccupiesHands` is not configurable. It is a core invariant of the mod design.

The config should support both broad defaults and server-specific limitations. If `allowedAnimals` is empty or omitted, the mod can use its default supported set. `blockedAnimals` should remove names from that default or allowed set.

## Permissions

Permissions integrate through Fabric Permissions API so LuckPerms can manage server behavior. Carrying is allowed by default when no permission provider overrides it.

Suggested permission nodes:

```text
carrybabyanimals.carry
carrybabyanimals.carry.tamed
carrybabyanimals.carry.others_tamed
carrybabyanimals.reload
```

Default behavior:

- Normal players can carry allowed wild or passive baby animals.
- Players can carry their own tamed baby animals.
- Players cannot carry another player's tamed baby animals unless config and/or permission allows it.
- Servers without a permissions provider still get sensible defaults.

## Technical Architecture

The mod should separate server gameplay logic from client rendering.

Server-side components:

- Carry manager: tracks one carried baby per player.
- Interaction handler: handles sneak-right-click pickup and drop.
- Validity checker: evaluates age, entity type, config, ownership, current carry state, and permissions.
- Attachment updater: starts and maintains the carried baby as a passenger riding the player for the vanilla-visible fallback.
- AI suppressor: disables the carried baby's pathfinding and wandering AI on pickup, then re-enables pathfinding on drop.
- Cleanup hooks: force safe drops on death, logout, dimension change, growth, removal, or invalid state.
- Hand-action blocker: blocks or replaces normal hand actions while carrying.
- Petting handler: converts left-click while carrying into cosmetic heart feedback with cooldown.

Client-side components:

- Renderer hook: detects carried baby entities for players.
- Held-pose renderer: draws the baby in a hands-carried position for modded clients.
- Vanilla render suppressor: suppresses vanilla entity rendering of the carried baby on modded clients so it appears only at the player's hand position, not both at the passenger position and the hand position.
- Fallback tolerance: if custom rendering is unavailable or unsupported for an entity, the passenger fallback still shows the real animal.

Networking should be minimal. Vanilla entity tracking may provide enough information for the fallback. Custom packets should only carry visual polish or compact carry-state hints needed by the client renderer. The server remains authoritative.

## Persistence And Failure Handling

Carried animals should be safely dropped on logout and server stop instead of trying to preserve a fragile carried pose across sessions. v1 should avoid long-lived saved player-to-entity carry links unless a later version explicitly adds reconnect restoration.

Failure handling should prefer safe drops over crashes or vanished animals. If config reloads, permission changes, growth, entity removal, or dimension changes make the carry invalid, the animal should be placed safely into the world where possible.

## Testing

Testing should focus on the behavior that can lose animals, duplicate animals, or break servers:

- Pickup and drop happy path.
- One baby per player.
- Empty-hand and sneak-right-click requirements.
- Left-click petting while carrying.
- Petting cooldown and visible heart particles.
- Hand-action blocking while carrying.
- Config friendly-name parsing.
- Unknown config names log clearly and do not crash.
- Allowed and blocked animal behavior.
- Tamed ownership restrictions.
- Configurable carrying of other players' tamed baby animals.
- Permission defaults without LuckPerms.
- Permission behavior with Fabric Permissions API.
- Auto-drop on death, logout, dimension change, growth, removal, and invalid state.
- Vanilla-client fallback visibility.
- Modded-client held-in-hands rendering path.

## Open Implementation Questions

These should be answered during implementation planning by inspecting the actual Minecraft 26.1.2 and Fabric API surfaces:

- Which exact entity types qualify as technically reasonable passive baby animals.
- Which interaction hooks best block hand actions while still allowing petting and dropping.
- Which exact API should spawn server-side heart particles around the carried baby.
- Verify that Fabric 26.x exposes a baby-to-adult growth event; if not, document a tick-check approach and acknowledge the one-tick race window.
