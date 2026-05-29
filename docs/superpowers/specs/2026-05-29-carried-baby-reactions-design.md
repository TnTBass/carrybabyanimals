# Carried Baby Reactions Design

## Summary

This spec explores the next lovable client-polish layer after Phase 5 Client Polish. Phase 5 is treated as conceptually complete: modded clients already have an optional carried-baby render path, while vanilla clients continue to see the safe passenger fallback.

The next layer should make carried babies feel more creature-specific without changing server gameplay. Large babies should stay visible without blocking first-person play, and animal reactions should remain cosmetic, optional, and safe to disable. The server remains authoritative for carry state, pickup, set-down, permissions, config, particles, sounds, and messages. The client may render nicer poses and local reactions, but gameplay must never depend on those poses.

## Compatibility Contract

- Vanilla clients continue to join supported servers and see real carried baby entities through the passenger fallback.
- Babies remain real entities. This design does not turn babies into inventory items, fake client entities, or server-owned animation props.
- Server gameplay remains authoritative. The client must not decide carry eligibility, set-down safety, parent reunion behavior, animal age, ownership, taming, breeding, health, or AI.
- Modded-client reactions are optional and cosmetic.
- Any server-to-client hint must be sent only after a supported-client capability check such as `ServerPlayNetworking.canSend(...)`.
- Client-only render state must not feed back into server gameplay.
- Cosmetic reactions must not use real sleeping, breeding, sitting, trusting, panic, love, or taming state unless a later design proves the state is visual-only for that exact entity type and Minecraft version.

## Candidate Phase Shapes

### Recommended: Phase 6 Creature Personality Polish

This phase combines size-aware large-baby placement with a small first set of per-animal carried reactions. It is the best next slice because placement and personality share the same client render path, and the first implementation can stay local to modded-client rendering.

Client-only work:

- Size buckets for carried render placement: small, medium, tall, and bulky.
- Large-baby under-arm or tucked-side placement for horses, camels, llamas, and other tall babies.
- Conservative first-person offsets that move tall babies lower and to the side instead of in front of the camera.
- Per-animal idle reaction clips that reuse vanilla rendering: chicken flap, rabbit wiggle, fox curl, panda sneeze, turtle hide, and generic nuzzle or settle fallback.
- Sleepy local visual variants, such as a lower head angle, curled body offset, stillness, or slow breathing where vanilla rendering allows it.

Server hints:

- None required for the first version if reactions are derived from local carried render state, entity type, local time since the client observed the carry render state, and existing server-owned feedback events. Local render time must not be treated as authoritative gameplay carry duration.
- Optional future hints may be useful for exact petting, sleepy, or reunion trigger timing. Those hints should be compact event packets, capability-gated with `ServerPlayNetworking.canSend(...)`, and safe to drop.

Vanilla-client degradation:

- Vanilla clients see the current passenger fallback and any server-owned sounds, particles, or action-bar messages already implemented by earlier phases.
- No vanilla client is expected to render tucked poses, flaps, wiggles, curls, sneezes, hides, or breathing.

Config:

- Add client-side config for `carriedBabyReactionsEnabled`, `largeBabyTuckedPoseEnabled`, `firstPersonLargeBabyVisibilityMode`, `sleepyCarryVisualsEnabled`, and `animalReactionIntensity`.
- Use server config only if a future server-owned hint controls event frequency or shared behavior.
- Client defaults should be enabled but conservative.

Permissions:

- No new permissions. This phase grants no gameplay power.

Risks:

- Vanilla entity renderers may not expose enough safe pose controls for every reaction.
- Entity renderers may assume normal world movement, which can make forced offsets or rotations look awkward.
- Some reactions may read as clipping or jitter if they stack with vanilla animation.
- First-person placement is the highest-risk visibility area and needs explicit manual testing with tall babies.

### Alternative: Phase 6 Large Baby Carry Visibility

This phase focuses only on large-baby placement and first-person safety. It is the smallest and safest implementation path, but it delays the animal personality work that makes the feature feel alive.

Client-only work:

- Height-aware placement for tall and bulky baby animals.
- Tucked-side or under-arm third-person placement.
- Local-player first-person visibility rules.
- A last-resort local-player render suppression for cases where placement cannot avoid camera obstruction.

Server hints:

- None.

Vanilla-client degradation:

- Vanilla clients continue seeing the passenger fallback.

Config:

- Client-only visibility mode: `auto`, `tucked`, `lowered`, or `hideForLocalPlayerWhenObstructing`.
- Client-only first-person toggle for tall-baby suppression.

Permissions:

- None.

Risks:

- This path solves an ergonomic problem but may feel like a polish fix rather than a new lovable phase.
- Hiding the baby locally can make the player wonder whether the carry state was lost, so suppression should be brief or clearly scoped to first-person local-player rendering.

### Alternative: Phase 6 Per-Animal Affection Reactions

This phase focuses on petting and carried idle reactions without changing large-baby placement. It has strong charm, but it risks building animations before solving first-person readability.

Client-only work:

- Petting wiggles, happy bounces, cuddle/settle animations, and animal-specific variants.
- Reaction selection by entity type family with a generic fallback.
- Sleepy carried baby visual variants.

Server hints:

- Optional petting and sleepy event hints may improve timing, but the first version should avoid new payloads unless existing client state cannot identify events accurately.

Vanilla-client degradation:

- Vanilla clients keep the server-owned action-bar messages, particles, and sounds.

Config:

- Client toggles for reactions, petting reactions, sleepy visuals, and intensity.
- Possible per-family disable list if a vanilla renderer looks bad.

Permissions:

- None.

Risks:

- Without large-baby placement work, tall animals can still block the first-person view.
- Event timing may feel less responsive if the client infers petting indirectly instead of receiving a server hint.

## Recommended Scope

Use the recommended combined slice: Phase 6 Creature Personality Polish. It should be a client-render phase with two linked goals:

1. Make tall and bulky babies readable without blocking first-person play.
2. Add a small, testable set of creature-specific cosmetic reactions.

Large-baby visibility is the higher-risk design constraint because it protects playability. Any later implementation plan must account for that risk before treating personality reactions as shippable. Sleepy visuals should be included only if they can be expressed as safe render offsets or stillness in the same framework.

## Large Baby Under-Arm And Tucked-Side Design

Size classification should be based on baby hitbox height plus an explicit override registry for animals whose renderer reads taller, wider, or bulkier than the hitbox alone suggests. Horses, camels, and llamas should be treated as tall by default. The later implementation plan should choose exact numeric thresholds, but it must include at least one tested example for small, medium, tall, and bulky placement.

Third-person placement should treat tall babies as held alongside the carrier's ribs, not centered in front of the face. The baby's root position should move:

- lower than small babies,
- slightly outward toward the carrier's arm side,
- slightly backward toward the carrier's side,
- rotated toward the carrier enough to read as tucked rather than floating.

First-person placement should be stricter than third-person. The local player should see only a small readable part of the baby, such as shoulder, neck, head edge, or shell edge, and never a full body filling the camera. The first version should prefer lower and side placement over scaling. Scaling should remain out of scope unless a later renderer-specific spike proves that scaling does not break vanilla entity rendering, shadows, hitbox perception, or animation readability.

If an entity remains obstructive after placement tuning, a local-player-only suppression mode may hide or fade the carried render in first person while keeping third-person render and server passenger state intact. This must be client config controlled and should never affect other players.

## Reaction Model

Reactions should be selected from a client-side registry keyed by vanilla entity type or broad animal family. Each reaction describes render-only offsets, rotations, visibility preferences, and timing. The registry should provide a generic fallback so unsupported animals still render normally.

Initial reaction candidates:

- Chicken: brief wing-flap emphasis during idle or petting.
- Rabbit: small wiggle or bounce, with restrained vertical motion.
- Fox: curl or tuck closer to the carrier.
- Panda: rare sneeze-like bob or head twitch.
- Turtle: brief hide-in-shell pose if the vanilla renderer supports a readable tucked look.
- Generic baby: tiny settle, nuzzle, or breathing offset.

Reactions should have short durations and low amplitude. They should not stack into large motion, and they should be interruptible when carry state ends or render state is pruned.

## Server Hints

The first implementation should avoid new server hints unless the implementation plan proves a specific client-only timing gap. If hints are needed later, they should be event hints rather than gameplay state:

- `PETTING_REACTION`: the server says a pet action happened.
- `SLEEPY_VISUAL`: the server says the baby is in a sleepy feedback window.
- `REUNION_REACTION`: the server says a parent reunion cosmetic event happened.

These hints must be optional, compact, and safe to ignore. They must be sent only to clients that advertise support for the payload. Vanilla clients should receive no custom payload and should continue seeing vanilla-safe feedback.

## Config And Permissions

Client config should own purely local visual preferences:

- `carriedBabyReactionsEnabled`: master switch.
- `largeBabyTuckedPoseEnabled`: enables large-baby side placement.
- `firstPersonLargeBabyVisibilityMode`: `tucked`, `lowered`, or `hideWhenObstructing`.
- `sleepyCarryVisualsEnabled`: enables local sleepy visual variants.
- `animalReactionIntensity`: conservative numeric multiplier or enum.
- `disabledCarriedReactionAnimals`: optional list for renderers that look wrong in a modpack.

Server config should remain limited to server-owned behavior and optional hint frequency if future hints exist.

No new permissions make sense. Permissions should continue to guard the ability to carry, not the ability to see cosmetic client reactions.

## Out Of Scope

- No real server sleeping state for carried babies.
- No changes to age, breeding, taming, ownership, trust, panic, sitting, or AI.
- No mechanical benefits from reactions.
- No new required client dependency.
- No GeckoLib or custom animation library adoption in this phase.
- No gameplay dependence on first-person render state.
- No automatic support for another mod's custom entity renderer beyond safe fallback placement.
- No server requirement that every client installs CarryBabyAnimals.
- No implementation plan or code changes in this design pass.

## Acceptance Criteria

- The spec preserves vanilla-client compatibility and server-authoritative gameplay.
- Large-baby visibility is addressed before or alongside personality reactions.
- Third-person placement reads as tucked under or beside the carrier's arm for tall babies.
- First-person placement avoids blocking the local player's view: with a baby horse carried at default FOV on a standard 16:9 display, the crosshair and horizon line must remain unobstructed, and the carried baby should not occupy more than the lower-left or lower-right quadrant depending on held side.
- Reactions are cosmetic, optional, and client-render-only unless a later plan proves a specific server hint is required.
- Each named reaction candidate, chicken, rabbit, fox, panda, and turtle, must render without obvious clipping at the default carry pose or fall back to the generic reaction for that entity type.
- The generic fallback must render safely for any supported carried entity that has no specific reaction entry.
- Any future payload is capability-gated and safe to ignore.
- Client config controls local visual intensity and fallback behavior.
- No new permission is introduced.
- No client-side reaction timing or state derived from local render duration is sent to the server or used to determine carry eligibility, set-down safety, parent reunion behavior, permissions, or any other gameplay outcome.
- The implementation plan that follows this spec must include render-registry tests, placement tests for tall babies, first-person manual verification, and vanilla-client fallback verification.

## Review Notes

This design intentionally recommends one combined Phase 6 because the user-facing result is strongest when visibility and personality polish ship together. If implementation risk is higher than expected, the phase can be split with Large Baby Carry Visibility first and Per-Animal Affection Reactions second. That split should happen in the implementation plan, not by broadening this design into multiple simultaneous code tracks.
