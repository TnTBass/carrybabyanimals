# Carry Baby Animals Manual Test Plan

Use this matrix before release candidates and after changes that touch carry state, client rendering, config, permissions, or interaction hooks. Record the Minecraft version, Fabric Loader version, Fabric API version, mod version, test world seed, client mix, and permission setup used for each run.

## Test Environments

| Environment | Purpose | Required setup |
| --- | --- | --- |
| Singleplayer modded client | Validate the normal player experience and local integrated-server behavior. | Fabric client with Carry Baby Animals installed. |
| Multiplayer, all modded clients | Validate server authority, tracking replay, held render, and render suppression for nearby players. | Dedicated Fabric server and at least two Fabric clients with the mod installed. |
| Multiplayer, vanilla-compatible client mix | Validate graceful fallback for clients that do not support the custom carry packets. | Dedicated Fabric server with the mod installed, one modded client, and one compatible client without this mod. |
| Permissions server | Validate Fabric Permissions API behavior through a provider such as LuckPerms. | Dedicated Fabric server with Fabric Permissions API, Carry Baby Animals, and the permission provider installed. |

## Singleplayer Modded Client

| Check | Steps | Expected result |
| --- | --- | --- |
| Empty-hand pickup | Spawn or breed a baby cow. Empty both hands, sneak-right-click the baby. | The baby starts riding the player as the server-authoritative carry state. The player is now carrying exactly one baby. |
| Non-empty hands do not pick up | Hold any item in either hand and sneak-right-click an eligible baby. | Pickup does not start. Normal interaction behavior is not replaced by carrying. |
| Adult animals are ignored | Sneak-right-click an adult animal with empty hands. | Pickup does not start. |
| One carried baby at a time | While already carrying one baby, sneak-right-click another baby. | The second pickup is ignored and the current carried baby remains attached. |
| Manual drop | While carrying, empty both hands and sneak-right-click air or a block. | The carried baby drops safely in front of the player and carry state clears. |
| Movement | Carry a baby while walking, sprinting, jumping, swimming, and climbing short terrain. | Movement remains usable and the baby remains attached unless an invalid-state cleanup is expected. |
| Petting | Left-click while carrying and repeat before and after the configured cooldown. | The attack is blocked, heart particles spawn around the carried baby when the cooldown allows, and no damage is dealt. |
| Hand blocking | While carrying, try placing blocks, using blocks, eating, drawing a bow, and using normal items. | Use-item and use-block actions are blocked while carrying. |
| Growth cleanup | Carry a baby close to adulthood or speed up age progression in a controlled test. | When the animal is no longer a baby, it is safely dropped and the carry state clears. |
| Safe drop placement | Trigger a drop near flat ground, walls, short ledges, shallow hazards, and tight spaces. | The baby is placed in a nearby collision-free position with a safe floor when one is available. It is not lost or duplicated. |
| Nursery lava refusal | Try to set down a carried baby in or directly beside lava. | The set-down is refused, the baby remains carried, and an action-bar refusal appears when Nursery Mode messages are enabled. |
| Nursery fire refusal | Try to set down a carried baby on fire, campfire, soul campfire, or magma block. | The set-down is refused and the baby remains carried. |
| Nursery damage refusal | Try to set down a carried baby on cactus, sweet berry bush, pointed dripstone, wither rose, or powder snow. | The set-down is refused and the baby remains carried. |
| Nursery cramped refusal | Try to set down a carried baby into a cramped or colliding space. | The set-down is refused and the baby remains carried. |
| Nursery fall refusal | Try to set down a carried baby over a drop at or above `nurseryDangerousFallDistanceBlocks`. | The set-down is refused and the baby remains carried. |
| Parent reunion success | Set down a carried baby within `parentReunionRadiusBlocks` of a matching adult on safe ground. | The baby is set down, hearts appear around the baby and adult when particles are enabled, and a warm action-bar message appears when messages are enabled. |
| Parent reunion radius limit | Set down a carried baby just outside `parentReunionRadiusBlocks` from the matching adult. | The baby is set down normally and no reunion feedback appears. |
| Parent reunion type mismatch | Set down a baby near an adult of a different animal type. | The baby is set down normally and no reunion feedback appears. |
| Parent reunion blocked by Nursery Mode | Try to set down a carried baby near a matching adult but in a spot Nursery Mode refuses. | The baby remains carried, the Nursery refusal appears as configured, and no reunion feedback appears. |
| Parent reunion tamed ownership | Test a tamed baby near same-owner and different-owner adult tamed animals. | Reunion feedback appears only for the same owner identity. |
| Logout and shutdown cleanup | Carry a baby, leave the world, then reload. Repeat with server/world shutdown. | The baby is dropped safely rather than being persisted in a fragile carried state. |

## Multiplayer With Modded Clients

| Check | Steps | Expected result |
| --- | --- | --- |
| Nearby player sees pickup | Player A picks up a baby while Player B watches. | Player B sees the baby carried by Player A. |
| Held render | Player A carries a baby while standing, walking, turning, jumping, and changing view direction. | Modded clients render the baby once at the held-in-hands position. |
| Passenger render suppression | Watch Player A from Player B's modded client. | The vanilla passenger-position render is suppressed for carried babies, so there is no duplicate above the player's head. |
| Tracking replay | Player B moves far enough away to stop tracking, then returns. Also test Player B joining while Player A is already carrying. | The carried state replays to Player B when tracking starts or when joining. |
| Clear on tracking stop/drop | Player B stops tracking Player A or the carried baby, then returns after the baby is dropped. | Stale held-render state clears and does not leave a ghost baby. |
| Petting visibility | Player A pets the carried baby while Player B watches nearby. | Heart particles are visible to nearby players and attacks remain blocked. |
| Disconnect cleanup | Player A disconnects while carrying. | The baby drops safely and Player B no longer sees carried render state. |

## Vanilla-Compatible Fallback

| Check | Steps | Expected result |
| --- | --- | --- |
| Modded carrier, unmodded observer | Player A with the mod picks up a baby while Player B is connected without this mod. | Player B remains connected and sees the real baby through the vanilla passenger fallback. |
| No custom-payload requirement | Repeat pickup, movement, petting, and drop/cleanup while the unmodded observer is online. | The server does not require the observer to support Carry Baby Animals custom packets. |
| Fallback is non-duplicating | Watch from the unmodded observer during carry and cleanup. | The baby appears as a single vanilla passenger, typically above the player, and clears when dropped. |
| Parent reunion fallback | Trigger Parent Reunion while the unmodded observer is nearby. | The observer remains connected and sees ordinary server-side heart particles without installing the client mod. |
| Modded renderer remains upgraded | Watch the same carry from a modded observer at the same time. | The modded observer sees the held render instead of the passenger-position fallback. |

### Phase 5 Extension: Large Baby First-Person Visibility

- Start a dedicated or integrated test world with CarryBabyAnimals installed on server and client.
- Carry a baby horse at default FOV on a 16:9 display.
- Switch to first person.
- Verify the crosshair and horizon line remain unobstructed.
- Verify the carried baby is limited to the lower-left or lower-right quadrant depending on arm side.
- Repeat with a baby camel, then repeat with a baby llama.
- Toggle `firstPersonLargeBabyVisibilityMode` through `TUCKED`, `LOWERED`, and `HIDE_WHEN_OBSTRUCTING`.
- Verify `HIDE_WHEN_OBSTRUCTING` hides only the local first-person carried render and does not drop the real baby or affect third-person/other-player views.

### Phase 5 Extension: Vanilla-Client Fallback

- Start a server with CarryBabyAnimals installed.
- Join with a vanilla-compatible client profile that does not install the CarryBabyAnimals client mod.
- Carry a baby cow and a baby horse.
- Verify the baby remains a real passenger entity and no custom client payload is required.
- Verify pickup, petting, set-down, Nursery Mode, and Parent Reunion behavior remain server-owned.

### Phase 5 Extension: Sleepy Carried-Baby Visuals

- Start a dedicated or integrated test world with CarryBabyAnimals installed on server and client.
- Carry a baby with a visible named carried reaction, such as a chicken or rabbit.
- Keep carrying it until the local sleepy visual window is reached.
- Pet the carried baby during that window.
- Verify eligible reactions use gentler motion than an immediate non-sleepy pet reaction.
- Repeat with `sleepyCarryVisualsEnabled` set to `false`.
- Verify disabling the client visual setting prevents sleepy softening while pickup, petting, set-down, Nursery Mode, Parent Reunion, and vanilla passenger fallback behavior remain unchanged.

## Config

The config file is `config/carrybabyanimals.json`.

| Check | Config | Expected result |
| --- | --- | --- |
| Default config | Delete the config and start the game/server. | A default config is written. Carrying is broadly available for supported baby animals, subject to permissions and tamed rules. |
| Allowed list | Set `"allowedAnimals": ["cow"]` and leave `"blockedAnimals": []`. | Baby cows are eligible. Other baby animals are denied because a non-empty allow list restricts eligibility. |
| Blocked list | Set `"allowedAnimals": []` and `"blockedAnimals": ["cow"]`. | Baby cows are denied while other supported babies remain eligible. |
| Blocked wins over allowed | Set both lists to include `"cow"`. | Baby cows are denied. |
| Dog alias | Use `"dog"` in an allow or block list. | The rule applies to tamed wolves only. |
| Wolf alias | Use `"wolf"` in an allow or block list. | The rule applies to wolves generally, with tamed ownership rules still enforced. |
| Modded entity default denied | With a compatible animal mod installed on the server, use the default config with no `allowedAnimals` or `blockedAnimals` entries for that modded entity. | The third-party baby entity is not carriable by default. Only the vanilla babies supported by Carry Baby Animals are eligible. |
| Full modded entity ID allow list | With a compatible animal mod installed on the server, set `"allowedAnimals": ["examplemod:duck"]` using the real entity ID from that mod. | Baby entities with that exact ID are eligible, default vanilla animals are denied by the non-empty allow list, and normal carry permissions still apply. |
| Full modded entity ID block list | With a compatible animal mod installed on the server, set `"allowedAnimals": ["examplemod:duck"]` and `"blockedAnimals": ["examplemod:duck"]`. | The configured baby entity is denied because blocked entries win over allowed entries. |
| Unknown names | Add unknown entries such as `"not_real"` to allowed and blocked lists. | Startup logs clear warnings and ignores unknown names for matching. The server does not crash. |
| Malformed entity IDs | Add malformed entries such as `"examplemod:bad id"` to allowed and blocked lists. | Startup logs clear warnings for unknown animal names or entity IDs, ignores the malformed entries for matching, and the server does not crash. |
| Typo-only allowlist safety | Set `"allowedAnimals": ["not_real"]` or only malformed IDs. | The configured allow-list restriction remains active after unknown entries are ignored, so pickup is denied rather than accidentally allowing everything. |
| Modded entity client responsibility | No Carry Baby Animals config changes needed. | On a server with a custom animal mod that requires matching clients, Carry Baby Animals does not change that mod's client requirement; client compatibility for the custom entity remains owned by the source mod and server setup. |
| Petting cooldown | Set `"pettingCooldownTicks": 1`, then `40`, then `0` or a negative value. | Positive values change heart-particle cadence. Non-positive values fall back to the default cooldown. |
| Other players' tamed animals | Toggle `"allowCarryingOtherPlayersTamedAnimals"`. | Another player's tamed baby can only be carried when both config and permission allow it. |
| Nursery Mode toggles | Toggle each `nurseryBlock*` option and `nurseryMessagesEnabled`. | Disabled hazard toggles allow only their matching hazard class, and disabled messages still refuse unsafe set-downs silently. |
| Parent Reunion toggles | Toggle `parentReunionEnabled`, `parentReunionMessagesEnabled`, and `parentReunionParticlesEnabled`; test small and large `parentReunionRadiusBlocks` and `parentReunionCooldownTicks`. | Disabled master switch suppresses reunion feedback. Message and particle switches affect only their own cosmetic output. Radius and cooldown values change when feedback can trigger. |

## Permissions

Permissions are checked through Fabric Permissions API. With no permission provider overriding values, ordinary carrying and carrying the player's own tamed babies are allowed by default, carrying another player's tamed baby is denied by default, and reload permission defaults to vanilla game-master command permission.

| Permission node | Default | Manual check |
| --- | --- | --- |
| `carrybabyanimals.carry` | `true` | Set to false for a player and verify all pickup attempts are denied. |
| `carrybabyanimals.carry.tamed` | `true` | Set to false and verify the player cannot carry their own tamed baby animals. |
| `carrybabyanimals.carry.others_tamed` | `false` | Set true, enable `allowCarryingOtherPlayersTamedAnimals`, and verify carrying another player's tamed baby becomes possible. |
| `carrybabyanimals.nursery.bypass` | Game-master command permission | Deny for normal players. Grant it to allow admin or test set-downs that Nursery Mode would otherwise refuse. |
| `carrybabyanimals.reload` | Game-master command permission | If a reload command exists in the build under test, verify only authorized players can use it. If no reload command exists, record this node as reserved/not exposed for that release. |

## Release Readiness

Before pushing or tagging a release:

- Confirm the exact mod version and target Minecraft/Fabric versions from the release candidate build.
- Run `.\gradlew.bat test`.
- Run `.\gradlew.bat build`.
- Run `.\gradlew.bat checkChangelog`.
- If preparing a release version, run `.\gradlew.bat showPublicReleaseNotes -PreleaseVersion=<version>` and show the exact public `CHANGELOG.md` section to Tyler for approval before the release push or tag.
- Run at least one singleplayer modded-client manual pass.
- Run at least one dedicated-server multiplayer pass with two modded clients.
- Run at least one vanilla-compatible fallback pass with an observer client that does not have this mod installed.
- Run at least one permissions pass using Fabric Permissions API and a real permission provider.
- Treat any animal loss, duplication, crash, disconnect, stale ghost render, impossible manual drop, or permission bypass as release-blocking.
