# CarryBabyAnimals Changelog

Public release notes for players and server admins. These notes are safe to publish to GitHub Releases, Modrinth, CurseForge, and other public marketplace pages.

## Unreleased

## 0.3.0

- Added NeoForge modloader support with the same Carry Baby Animals gameplay as Fabric.
- On NeoForge, Carry Baby Animals version and build status from [ModStatusKit](https://github.com/TnTBass/ModStatusKit) appear in NeoForge's built-in Mods screen. On Fabric, this is located in ModMenu.
- Permission checks now use each loader's current permission-provider support: modern Fabric API on Fabric and NeoForge's built-in permission API on NeoForge.
- Nursery Mode refusal messages now capitalize "Baby" when an unnamed baby animal starts the sentence.

## 0.2.0

- Client-side carried-baby visuals now look better, with gentler arm placement, safer first-person positioning for large babies, subtle movement, animal-specific reactions, and clearer sleepy/asleep poses.
- Carried baby cats and ocelots now curl up into a more recognizable sleeping pose when they get sleepy.
- Nursery Mode now helps keep carried babies out of trouble by refusing set-downs in unsafe spots like lava, fire, cactus, cramped spaces, and dangerous drops.
- Nursery Mode bypass now defaults to off when Fabric Permissions API is installed, so permission providers must explicitly grant `carrybabyanimals.nursery.bypass`.
- Setting a carried baby down near a matching adult can now show a little Parent Reunion moment with hearts and a warm action-bar message.
- Server owners can now use full entity IDs in `allowedAnimals` and `blockedAnimals`, making it easier to opt into compatible baby animals from other mods without enabling modded animals by default.
- With Carry Baby Animals installed on the client, players can now edit visual settings directly through ModMenu.
- ModMenu now includes a connection status indicator that shows whether the server also has Carry Baby Animals installed, including version and build details when available.

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
