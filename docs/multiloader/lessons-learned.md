# Lessons Learned

Capture lessons from the CarryBabyAnimals pilot here so the next two mod migrations start faster.

## Initial Pilot Lessons

- Inventory loader imports before designing boundaries. In CarryBabyAnimals, the risky Fabric clusters are entrypoints/events, networking, render hook registration, permissions, config paths, ModMenu, ModStatus version lookup, and mixin declarations.
- Preserve Fabric behavior before adding NeoForge. The existing Fabric player experience is the regression baseline.
- Do not let Gradle churn hide behavior changes. Define the target layout first, move files in small steps, and include the physical layout in Phase 1 before second-loader work begins.
- Treat networking as payload semantics plus platform transport. The payload names and fields are reusable; registration, codecs, recipient discovery, and can-send checks are loader-owned.
- Treat rendering as visual math plus platform hook. The carried-baby visual frame logic is reusable; Fabric `LevelRenderEvents` and render-state/mixin mechanics are adapter-owned.
- Keep optional integrations optional. Fabric Permissions API and ModMenu should not become hard requirements during boundary extraction.
- Verify with real multiplayer and vanilla-compatible fallback. Unit tests cannot prove the carried render, passenger fallback, tracking replay, or client capability behavior.
- Send the spec and packet through Revue before implementation planning so hidden assumptions are caught while changes are still cheap.

## Release-Day Lessons For MultiGolem

- Treat permission-provider work as a cross-loader modernization slice, not just a NeoForge task. When porting MultiGolem, update Fabric permissions to Fabric API's current permission API at the same time NeoForge permissions are added.
- Do not build MultiGolem directly against LuckPerms. Integrate through the loader permission APIs: Fabric API permissions on Fabric and NeoForge's built-in permission API on NeoForge. LuckPerms should remain an external provider behind those loader APIs.
- Register NeoForge permission nodes with explicit defaults, then resolve permissions through NeoForge's `PermissionAPI`. Do not treat OP status as an automatic substitute for a provider-granted bypass unless the desired fallback is deliberately documented for that loader.
- Keep common code permission-neutral. Permission node names and default policy can live in common code, but Fabric/NeoForge provider calls and fallback mechanics belong in loader adapters.
- Release artifacts need loader-specific names all the way through CI and publishing. Stage GitHub Release assets with unique names, including sources jars such as `modid-version-fabric-sources.jar` and `modid-version-neoforge-sources.jar`, so same-named `sources.jar` outputs do not collide.
- Modrinth dependencies are version-wide, not loader-scoped. If Fabric and NeoForge are published as one Modrinth version, a Fabric-only dependency such as Fabric API cannot be marked globally required without polluting NeoForge installs. Either publish separate versions per loader or mark Fabric-only dependencies optional and document the Fabric runtime requirement.
- CurseForge relation metadata is picky. Keep `relations.projects` array-shaped when a loader has dependencies, but omit `relations` entirely when a loader has no dependency relations; an empty `projects` list is rejected.
- Split CurseForge retry reporting by loader. A retry workflow that can upload Fabric, NeoForge, or both should report each file ID only when that loader upload actually ran.
- Add release-source gates for marketplace scripts. Static checks should cover loader-suffixed artifact paths, public changelog source, retry workflow behavior, Modrinth dependency tradeoffs, and CurseForge relation metadata shape.
- Revue cost gates are useful, but broad multiloader reviews can preflight into many planned calls. Start with targeted packets for the highest-risk slices: publishing paths, permissions, networking, render hooks, and metadata.
- Keep `main` as the release branch of record. After a successful release from a temporary branch, fast-forward `main` to the released commit before deleting the release branch.

## Phase 1 Follow-Up Questions

Answer these after the Fabric-preserving implementation is complete:

- Which boundary introduced the most churn?
- Which Fabric behavior was hardest to prove unchanged?
- Which static scans or tests caught real mistakes?
- Which parts of this packet copied cleanly to the other mods?
- Which parts needed per-repo tailoring?
