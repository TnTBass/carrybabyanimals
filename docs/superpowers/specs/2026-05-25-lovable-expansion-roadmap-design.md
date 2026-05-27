# CarryBabyAnimals Lovable Expansion Roadmap Design

## Summary

This roadmap expands CarryBabyAnimals with more affectionate feedback, safer set-down behavior, family reunion moments, modpack-friendly animal support, and optional client polish. The guiding idea is simple: make carrying babies feel more alive without weakening the current compatibility contract.

The server must remain authoritative. Carried babies stay real world entities, not inventory items or client-only illusions. Vanilla clients must continue to join supported servers and see a safe passenger fallback. Modded clients can receive richer rendering and first-person polish, but client polish must never become required for correct gameplay.

## Compatibility Contract

- The server owns pickup, carry state, petting, set-down validation, cleanup, permissions, and config.
- Vanilla clients see normal Minecraft entities and vanilla-compatible packets.
- Modded clients may hide the passenger render and draw a nicer carried pose.
- Custom payloads must remain optional and only be sent after a Fabric networking capability check such as `ServerPlayNetworking.canSend(...)`.
- Any later phase that adds new custom payload types must define the registration or capability contract before sending them.
- New gameplay must not depend on a client-only render state, keybind, local animation, or custom client entity.
- Expanded modded animal support must not create new client requirements by itself. If a custom animal entity comes from another mod, that other mod still controls whether vanilla clients can join that server.

## Roadmap Phases

### Phase 1: Cozy Feedback

Add more personality to the current carry and pet loop.

Features:

- Soft idle sounds while a baby is being carried.
- Variant petting messages.
- Name-aware message variants for custom-named babies.
- Sleepy baby moments after a baby has been carried for a while.
- Gentle server-visible particles for sleepy or affectionate reactions.

Default behavior:

- Enabled by default, with conservative frequency.
- Cosmetic only.
- No gameplay effects on age, health, breeding, taming, trust, or movement.

Config:

- `cozyFeedbackEnabled`: master switch.
- `carriedIdleSoundsEnabled`: enables occasional soft carried-baby sounds.
- `carriedIdleSoundMinTicks` and `carriedIdleSoundMaxTicks`: randomized idle sound interval.
- `pettingMessagesEnabled`: enables variant petting action-bar messages.
- `nameAwareMessagesEnabled`: enables custom-name-aware message variants.
- `cozyParticlesEnabled`: enables gentle carried-baby feedback particles.
- `sleepyBabiesEnabled`: enables sleepy carried-baby moments.
- `sleepyAfterTicks`: minimum carried duration before sleepy moments can start.
- `sleepyMessageCooldownTicks`: minimum spacing between sleepy messages for one carried baby.
- `sleepyParticleCooldownTicks`: minimum spacing between sleepy particle effects for one carried baby.

Permissions:

- No new permissions by default.
- Existing carry permissions still determine whether the player may carry the baby at all.

### Phase 2: Nursery Mode

Prevent players from setting down carried babies in dangerous places, with playful refusal messages.

Features:

- Block unsafe set-downs into lava, fire, cactus, damaging blocks, suffocation spaces, or dangerous drops.
- Keep the baby carried if the target location is unsafe.
- Show a funny action-bar message for the hazard, for example `You tried to put <name> in lava! You monster!`
- Include message variants so the joke stays cute instead of becoming a single repeated line.

Default behavior:

- Enabled by default for obvious high-danger hazards.
- Conservative checks should prefer safe refusal over risky placement.
- Refusal messages should be playful, not mean-spirited.

Config:

- `nurseryModeEnabled`: master switch.
- `nurseryBlockLava`: refuses lava and lava-adjacent set-downs.
- `nurseryBlockFire`: refuses fire, campfires, and similar burning hazards.
- `nurseryBlockCactusAndDamage`: refuses cactus and other obvious damaging blocks.
- `nurseryBlockSuffocation`: refuses solid or cramped set-down spaces.
- `nurseryBlockDangerousFalls`: refuses set-downs with unsafe fall distance below.
- `nurseryDangerousFallDistanceBlocks`: integer fall-distance threshold measured in blocks.
- `nurseryMessagesEnabled`: enables refusal action-bar messages.

Permissions:

- Add `carrybabyanimals.nursery.bypass`.
- Default fallback without Fabric Permissions API should match the existing reload fallback: `ServerPlayer#permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)`.
- Bypass should be useful for admins, testing, and unusual server setups, not normal play.

### Phase 3: Parent Reunion

Reward players for returning babies near matching adults.

Features:

- When a baby is set down near a compatible adult, show hearts around the baby and adult.
- Send a warm action-bar message to the carrier.
- Keep the effect cosmetic in the first version.

Default behavior:

- Enabled by default.
- Cosmetic only.
- No breeding, growth acceleration, taming, ownership transfer, or AI rewrites.

Config:

- `parentReunionEnabled`: master switch.
- `parentReunionRadiusBlocks`: search radius around the set-down position.
- `parentReunionCooldownTicks`: cooldown per baby or carrier to prevent particle spam.
- `parentReunionMessagesEnabled`: enables action-bar messages.
- `parentReunionParticlesEnabled`: enables heart particles.

Permissions:

- No new permissions by default.
- Existing carry permissions are enough because reunion only happens after an allowed carry.

### Phase 4: Expanded Modded Animal Support

Let server owners opt into additional baby animal entity types without code changes.

Features:

- Allow `allowedAnimals` and `blockedAnimals` to accept full entity IDs such as `examplemod:duck`.
- Keep existing friendly aliases for vanilla animals.
- Optionally add admin-defined aliases for long modded entity IDs.
- Log unknown names clearly and keep the current safe behavior where unknown-only allow lists do not accidentally allow everything.

Default behavior:

- Vanilla supported animals remain unchanged.
- No modded entity is allowed by default unless it is added through config.
- Unknown IDs should not crash the server.

Config:

- Extend `allowedAnimals` and `blockedAnimals` to accept either aliases or full entity IDs.
- Consider `animalAliases` for optional custom aliases, for example mapping `duck` to `examplemod:duck`.
- Consider `tamedAnimalAliases` only if a supported modded entity needs ownership-sensitive handling similar to `dog`.

Permissions:

- No new permissions by default.
- Existing `carrybabyanimals.carry`, `carrybabyanimals.carry.tamed`, and `carrybabyanimals.carry.others_tamed` continue to govern allowed entities.

Compatibility note:

- This phase does not make vanilla clients understand another mod's custom entity. It only lets CarryBabyAnimals carry entities that already exist on the server. If the custom animal mod requires matching clients, that requirement remains outside CarryBabyAnimals.

### Phase 5: Client Polish

Improve the optional modded-client experience while preserving the vanilla passenger fallback.

Features:

- Improve carried-baby placement and pose tuning.
- Add gentle first-person polish for petting and sleepy feedback.
- Add client-side cosmetic reactions such as small wiggles, flaps, curls, or sleepy offsets when they can reuse vanilla entity rendering.
- Keep client-side carried-render state pruned when the baby or carrier entity disappears, changes level, or otherwise becomes unavailable to the client.
- Keep GeckoLib or custom model dependencies out unless a later design intentionally moves beyond vanilla entity reuse.

Default behavior:

- Modded clients receive nicer presentation.
- Vanilla clients continue to see the real passenger fallback.
- Client effects must be cosmetic and optional.

Config:

- Prefer client-side options for purely local visual polish.
- Use server config only for shared behavior or server-controlled feedback frequency.
- If server hints are needed, send compact optional payloads only to modded clients.

Permissions:

- No new permissions by default.
- Client polish should not create gameplay powers.

## Config And Permissions Matrix

| Phase | Config Needed | Permission Needed | Default |
| --- | --- | --- | --- |
| Cozy Feedback | Yes, for feature switches and frequency | No | Enabled, conservative frequency |
| Nursery Mode | Yes, for hazard classes and message behavior | Yes, `carrybabyanimals.nursery.bypass` | Enabled for obvious hazards |
| Parent Reunion | Yes, for radius, cooldown, particles, messages | No | Enabled, cosmetic |
| Expanded Modded Animal Support | Yes, for entity IDs and optional aliases | No new permission | Opt-in for modded entities |
| Client Polish | Mostly client config, optional server hints | No | Enabled on modded clients |

## Architecture

Server-side additions should be split by responsibility:

- Feedback scheduler: tracks carried duration, idle sound timing, sleepy timing, and message cooldowns.
- Message catalog: provides variant text for petting, sleepy moments, nursery refusals, and reunions.
- Set-down safety checker: evaluates candidate drop positions before `CarryAttachment` places the baby.
- Reunion detector: scans nearby entities after a successful safe set-down.
- Animal resolver: extends alias and entity ID resolution while preserving current safe unknown-name behavior.

Client-side additions should stay presentation-only:

- First-person feedback effects triggered by optional supported payloads.
- Carry pose tuning inside the existing carried-baby render path.
- Optional local animation offsets that reuse vanilla entity rendering.

## Data Flow

Pickup remains unchanged: the player must pass eligibility, config, and permission checks before carry state begins.

While carried:

1. The server tracks elapsed carried time.
2. Cozy feedback may emit sounds, particles, or messages on cooldown.
3. Petting uses the existing left-click flow, then selects a message variant.
4. Optional client packets are sent only after the same supported-client capability check used by the existing carry payloads.

Set-down:

1. The requested drop position is calculated.
2. Nursery Mode checks the destination.
3. If unsafe, the baby remains carried and the player receives a refusal message.
4. If safe, the existing drop flow places the baby.
5. Parent Reunion scans for nearby compatible adults and emits cosmetic feedback.

## Error Handling

- If feedback scheduling loses track of a carried entity, clear that feedback state and keep the existing carry cleanup behavior.
- If a message catalog has no specific variant for an animal, fall back to a generic baby animal message.
- If Nursery Mode cannot confidently classify a destination, prefer existing safe drop behavior and avoid crashing.
- If a configured modded entity ID is unknown, log it and ignore it.
- If a configured allow list contains only unknown names, carrying must remain restricted instead of broadening to all defaults.

## Testing

Roadmap-wide tests:

- Vanilla-compatible carry behavior still works without client support.
- Optional client payloads are only sent to clients that support them.
- Existing carry permission behavior remains unchanged.
- Config defaults preserve current behavior except where a phase intentionally adds enabled cosmetic behavior.

Phase-specific tests:

- Cozy Feedback: message selection, name-aware variants, cooldowns, disabled switches, sleepy timing, sound interval bounds.
- Nursery Mode: each hazard class, safe destination pass-through, refusal keeps the baby carried, bypass permission fallback, disabled mode.
- Parent Reunion: matching adult detection, no adult case, radius limit, cooldown, cosmetic-only behavior.
- Expanded Modded Animal Support: full entity ID parsing, aliases, blocked ID precedence, unknown IDs, unknown-only allow lists.
- Client Polish: carried render fallback remains intact, optional visual payload handling, and carried-render state cleanup when the baby or carrier entity is missing.

## Documentation And Release Notes

Each implemented phase must update public or internal changelogs deliberately:

- Player-visible or server-admin-visible behavior belongs in `CHANGELOG.md`.
- Maintainer-only plans, tests, and release workflow notes belong in `INTERNAL_CHANGELOG.md`.
- Public docs must document new config fields, defaults, examples, permission nodes, and vanilla-client fallback behavior before release.

## Non-Goals

- No inventory storage for babies.
- No multiple carried babies per player.
- No hostile mobs or villagers from this roadmap alone.
- No mechanical benefits from petting, sleeping, or reunion effects.
- No requirement that every connecting client install CarryBabyAnimals.
- No custom model or animation library adoption without a separate design.

## Open Questions For Phase Specs

- Which vanilla sounds best fit idle carried babies without becoming noisy?
- Should sleepy babies have per-animal message pools or start with generic variants?
- Which exact block and fluid APIs should Nursery Mode use for each hazard class in this repo's Minecraft 26.1.2 target?
- Which phase owns any new custom payload capability contract: reuse Fabric channel registration plus `canSend`, or introduce a versioned capability payload?
- Should Parent Reunion require exact entity type matches, or should closely related animals have explicit family mappings?
- Should modded entity aliases be a simple object map or a list with future metadata?
- Should client polish include per-animal placement tuning in the first pass, or only global pose improvements?
