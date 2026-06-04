# CarryBabyAnimals Changelog

Public release notes for players and server admins. These notes are safe to publish to GitHub Releases, Modrinth, CurseForge, and other public marketplace pages.

## Unreleased

- Added Nursery Mode safety checks that keep carried babies from being set down in dangerous spots such as lava, fire, cactus, damaging blocks, cramped spaces, and unsafe drops.
- Added Parent Reunion feedback so safely setting down a carried baby near a matching adult can show hearts and a warm action-bar message.
- Added server config support for full entity IDs in `allowedAnimals` and `blockedAnimals`, letting server owners opt into compatible baby animal entities from other mods without enabling modded animals by default.
- Added optional ModMenu integration so modded clients can edit Carry Baby Animals client visual settings without hand-editing `config/carrybabyanimals-client.json`.
- Added an optional ModMenu connection status indicator so players can see whether the server also has Carry Baby Animals installed.
- Improved modded-client carried-baby visuals with safer large-baby first-person placement, gentler arm positioning, subtle motion, animal-specific carried reactions, and clearer sleepy/asleep poses.
- Improved sleepy/asleep carried-cat visuals so supported feline babies use a curled lie-down pose while carried.
- Fixed stale carried-baby visuals after client world changes.
- Fixed the mod metadata icon so Minecraft and ModMenu show the Carry Baby Animals icon instead of the fallback question mark.
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
