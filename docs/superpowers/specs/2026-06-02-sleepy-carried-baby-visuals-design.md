# Sleepy Carried Baby Visuals Design

## Goal

Make sleepy and asleep carried-baby visuals readable at a glance for small baby animals while preserving the mod's server-authoritative gameplay and vanilla-client passenger fallback.

## Scope

This is a Phase 5 client polish extension, not Phase 6. The work is limited to client-side carried-baby presentation:

- Add clearer sleepy and asleep render states for eligible small carried babies.
- Keep pickup, carry, drop, petting, Cozy Feedback, Parent Reunion, large-baby placement, and vanilla fallback behavior intact.
- Do not add ModMenu support in this session.
- Do not add new payloads.
- Do not change real sleeping, sitting, AI, health, age, ownership, breeding, trust, panic, movement, or server gameplay state.

## Recommended Visual Direction

Use the approved hybrid direction:

- Sleepy: reduce normal carried bobbing and lower/tuck the baby slightly toward the carrier's arms.
- Asleep: deepen the tuck, keep the body still except for a very small breathing-style vertical pulse, and apply a small head-lowered pitch where renderer-safe.
- Optional cue: add only a tiny restrained client-only sleep cue during the asleep window if pose and motion alone are not readable enough in the existing renderer path.

The cue must feel cozy, not stunned or damaged. It must not obstruct first-person view more than the existing carried render and must respect `sleepyCarryVisualsEnabled`.

## Architecture

Extend the existing client render path rather than adding a new system:

- `CarriedBabyRenderState` continues to own local cosmetic timing keyed by baby entity id.
- Add a deterministic sleepy visual phase model such as `ALERT`, `SLEEPY`, and `ASLEEP` for render-only evaluation.
- `CarriedBabyVisualFrame` combines base placement, optional petting reaction, and sleepy/asleep phase adjustments into one frame.
- `CarriedBabyRenderer` schedules and reads the local sleepy/asleep state during collection, but never sends data to the server.

The current issue is that sleepy visuals only soften an active reaction. A baby that is sleepy without an active reaction returns the base placement unchanged, making the state too subtle. The new design makes sleepy/asleep readable even when no petting reaction is active.

## Data Flow

1. The server remains authoritative for carried state and only the existing carry state reaches the client.
2. The client render state observes that a baby is carried and schedules local visual timing.
3. The client derives a render-only visual phase from local time and config.
4. The renderer evaluates placement, reaction, and sleepy/asleep presentation into a frame.
5. Clearing or replacing a carried baby clears local reaction and sleepy/asleep visual timing for that baby.

## Compatibility

Vanilla clients continue to see the normal passenger fallback and do not need any new client payload. Modded-client visuals stay optional and cosmetic. If `sleepyCarryVisualsEnabled` is disabled, carried babies should use existing placement and reaction behavior without sleepy/asleep pose adjustments.

## Testing

Use TDD for implementation. Add focused tests before production code for:

- Transition timing from alert to sleepy to asleep.
- A sleepy/asleep frame differs from base placement even without an active petting reaction.
- Asleep breathing stays restrained and deterministic.
- Sleepy/asleep scheduling is idempotent and does not restart every render frame.
- Clearing or replacing carried state clears local sleepy/asleep visual state.
- Petting reactions still work and do not stack with asleep visuals into noisy motion.
- Config-disabled behavior keeps existing visuals.

Run focused render/config tests, `git diff --check`, `.\gradlew.bat checkChangelog`, and `.\gradlew.bat build` before completion.

## Documentation And Changelog

Update `README.md` or `docs/manual-test-plan.md` so playtesting knows how to identify sleepy versus asleep carried babies. Because this is player-visible visual behavior, update `CHANGELOG.md`.

## Review Gate

After implementation and focused verification, run a bounded Revue implementation review over the touched source, tests, and docs. Evaluate all findings with `superpowers:receiving-code-review`, fix valid findings, mark false positives only with evidence, and leave no unresolved findings unless explicitly deferred.
