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

## Release And Marketplace Publishing

- Optional dependencies need to be optional in every layer: `fabric.mod.json`, Gradle dependency scope, runtime code paths, Modrinth metadata, CurseForge metadata, README text, and marketplace description text should all tell the same story.
- Marketplace project descriptions are separate from release artifacts. A successful GitHub release, Modrinth version upload, or CurseForge file upload does not prove the project-page description changed.
- CurseForge project-page description updates are not covered by the same upload path as file changelogs. Treat CurseForge description changes as manual unless a supported authenticated update path is verified.
- Initial release notes should read like shipped product behavior for players and server admins, not like a development task log. Internal cleanup, debugging, and release workflow details belong in `INTERNAL_CHANGELOG.md`.
- After a public release exists, retagging is operationally risky because GitHub, Modrinth, and CurseForge each handle replacement or duplicate files differently. Do one final metadata and artifact readiness pass before the first public tag push.
- Server-required/client-optional support needs explicit plumbing and verification across Fabric metadata, README wording, Modrinth environment metadata, CurseForge relation metadata, and release scripts.
- Default config comments are public UX. Keep supported animal names deterministic and readable because server admins will copy from that file.
- Player feedback text should stay symmetric across pickup, drop, and pet actions. Named animals should use their custom names, and unnamed animals should use clear baby-animal wording.
- Public docs should explain behavior and defaults, not only show a sample file. Configuration and permission docs need options, defaults, supported values, fallback behavior, and practical examples.
- Keep public and internal release channels separate. `CHANGELOG.md` is marketplace-safe; `INTERNAL_CHANGELOG.md` is where publishing quirks, automation changes, and maintainer workflow notes belong.
