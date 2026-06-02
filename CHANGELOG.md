# CarryBabyAnimals Changelog

Public release notes for players and server admins. These notes are safe to publish to GitHub Releases, Modrinth, CurseForge, and other public marketplace pages.

## Unreleased

- Added optional ModMenu integration so modded clients can edit Carry Baby Animals client visual settings without hand-editing `config/carrybabyanimals-client.json`.
- Fixed sleepy/asleep carried-baby visuals so the readable tucked pose is applied to the actual client render state instead of only the internal visual frame.
- Made optional modded-client sleepy carried-baby visuals easier to read for small babies by adding clearer sleepy/asleep presentation while preserving vanilla-client passenger fallback.
- Added optional modded-client creature personality polish for carried babies, including safer large-baby first-person placement, gentle animal-specific carried reactions, and render-only sleepy reaction softening while preserving vanilla-client passenger fallback.
- Improved the optional modded-client carried-baby render with gentler arm placement, subtle motion, and better cleanup of stale carried visuals after client world changes.
- Added server config support for full entity IDs in `allowedAnimals` and `blockedAnimals`, letting server owners opt into compatible baby animal entities from other mods without enabling any modded animals by default.
- Fixed startup config filtering so valid modded full entity IDs are not removed before pickup-time entity matching can use them.
- Added Parent Reunion feedback so safely setting down a carried baby near a matching adult can show cosmetic hearts and a warm action-bar message, with a five-second default cooldown.
- Added Nursery Mode safety checks that refuse dangerous player set-downs near lava, fire, damaging blocks, cramped spaces, and unsafe drops while keeping the baby carried.
- Fixed Nursery Mode hazard checks for magma floors, lava edges, cactus-adjacent spots, and other damaging floor/neighbor blocks.
- Changed Nursery Mode bypass to default to disabled when Fabric Permissions API is installed, so permission providers must explicitly grant `carrybabyanimals.nursery.bypass`.

## 0.1.3

- Added optional Cozy Feedback for carried babies, including softer carried idle sounds, varied petting messages, sleepy moments, and gentle cosmetic particles.

## 0.1.2

- Mod listings now show the Carry Baby Animals icon and link to the Modrinth page, source code, and issue tracker.

## 0.1.1

- Changing Fabric Permissions API to be optional, not required. Servers without it use the mod's vanilla permission defaults.

## 0.1.0

- Initial release for Minecraft 26.1.2.
- Pick up baby animals by sneak-right-clicking them with empty hands, carry them with you, and set them back down when you are ready.
- Supported modded clients show carried babies tucked into the player's arms with a matching carry pose.
- Vanilla clients can still use the mod, with carried babies shown as passengers and action-bar messages that name the carried baby or animal type.
- Carried babies can be petted, with heart particles and action-bar messages that use the baby's name or animal type.
- Doors and trapdoors remain usable while carrying a baby animal.
- Server config includes a comment listing every supported animal name for allow/block lists.
