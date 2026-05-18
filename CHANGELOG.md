# CarryBabyAnimals Changelog

Public release notes for players and server admins. These notes are safe to publish to GitHub Releases, Modrinth, CurseForge, and other public marketplace pages.

## Unreleased

- Added client-side carry visuals for modded clients: carried babies stay server-authoritative as passengers, while supported clients hide the vanilla passenger render and show the baby near the carrier's hands.
- Improved carried-baby presentation and interactions: carriers now pose their arms around the baby, petting cancels local attacks before creative block-break prediction, and doors/trapdoors can still be used while carrying.
- Refined carried-baby placement so animals sit closer to the carrier's arms, and made petting hearts visible for the carrier in first-person view.
- Improved vanilla-client feedback: picking up or setting down a baby now shows action-bar text, and sneak-right-clicking an entity while already carrying sets the current baby down instead of silently consuming the click.
- Fixed vanilla-client passenger fallback syncing so carried babies visibly attach to the player immediately after pickup instead of appearing to remain on the ground until set down.
