# CarryBabyAnimals Lessons Learned

These notes capture implementation/review lessons from the first full CarryBabyAnimals build-out. They are meant to help future Codex/Claude work stay focused and avoid repeating subtle mistakes.

## Agent Workflow

- Subagents are useful for high-value spec and code review, but they can create permission friction and may hang during long review loops.
- For this repo, prefer subagents for bounded review questions and use the main thread for routine Gradle, Git, and verification commands.
- Keep review prompts narrow: ask for release-blocking and important issues first, not broad style cleanup.

## Config Safety

- "Ignore unknown config names" needs a safety interpretation.
- Unknown names should be ignored individually, but an unknown-only `allowedAnimals` list must not fail open into the broad default animal set.
- If config validation mutates effective config, the method name should say so. `filterAndLogUnknownAnimalNames` is intentionally clearer than `logUnknownAnimalNames`.

## Interaction Results

- `InteractionResult.SUCCESS`, `FAIL`, and `PASS` are gameplay behavior, not just callback boilerplate.
- Returning `SUCCESS` after stale carry cleanup can eat the player's original click. When cleanup means "we are no longer carrying," prefer `PASS` so vanilla can handle the action.
- Carrying occupies hands, but deliberate drop is a special case: empty hands plus sneak-use on air/block drops the baby, while entity interaction remains consumed to avoid switching targets.

## Cleanup And Level Boundaries

- Cleanup code must respect Minecraft level/dimension boundaries.
- During dimension-change cleanup, do not fall back to "drop in front of the player" if the carried entity is not present in the origin level. That can place or detach the animal in an unexpected destination.
- When an entity is already elsewhere, remove passenger attachment if possible, restore AI if applicable, log the odd state, and clear carry state without world placement.

## Manual Test Plans Find Bugs

- The manual test matrix exposed a real implementation gap: there was no deliberate player-triggered drop path.
- Documentation that describes expected player behavior should be treated as executable review input, not just release paperwork.

## Fabric 26.1.2 API Verification

- Fabric/Minecraft 26.1.2 APIs differ enough from older examples that local verification is worth the time.
- `javap` and local cached jars confirmed the exact available hooks for death cleanup, dimension-change cleanup, block attack interception, and block-break prevention.
- Record verified hook names in `docs/implementation-notes/2026-05-17-fabric-26-api-surfaces.md` as implementation proceeds.

## Commit Hygiene

- Keep dirty support files out of feature commits until they are intentionally reviewed.
- `.claude/`, workflow/changelog scripts, generated logs, and release automation files may be useful, but they should not get swept into gameplay commits.
