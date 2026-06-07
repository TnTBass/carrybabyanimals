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

## Phase 1 Follow-Up Questions

Answer these after the Fabric-preserving implementation is complete:

- Which boundary introduced the most churn?
- Which Fabric behavior was hardest to prove unchanged?
- Which static scans or tests caught real mistakes?
- Which parts of this packet copied cleanly to the other mods?
- Which parts needed per-repo tailoring?
