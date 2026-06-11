# Internal Changelog

Maintainer-only notes for repo, build, workflow, release-process, and other internal project changes. This changelog is not published to GitHub Releases, Modrinth, CurseForge, or other public marketplace pages.

## Unreleased

## 0.3.0

- Updated the GitHub, Modrinth, and CurseForge release paths for loader-suffixed Fabric and NeoForge artifacts, including loader-specific CurseForge retry support.
- Adjusted combined-loader Modrinth metadata so Fabric API is optional rather than globally required for NeoForge installs, and split CurseForge retry file-ID reporting by loader.
- Staged GitHub Release assets with unique loader-suffixed sources filenames so Fabric and NeoForge sources jars do not collide during release upload.
- Kept CurseForge relation metadata array-shaped for single Fabric dependency uploads.
- Omitted CurseForge relation metadata for loaders without dependency relations so NeoForge uploads do not send an empty projects list.
- Removed Gradle execution-time `Task.project` usage from release and artifact verification tasks so release prep no longer emits Gradle 10 deprecation warnings.
- Replumbed Fabric permissions to prefer Fabric API `0.149.0+26.1.2` / `fabric-permission-api-v1:1.0.0+f3e738be4c`, retained the legacy `me.lucko:fabric-permissions-api:0.7.0` compatibility path with a one-time deprecation warning, and wired NeoForge permissions through registered `PermissionNode`s plus `PermissionAPI`.
- Actioned Revue findings on the NeoForge adapter by centralizing MSK build-number generation, deduplicating same-tick NeoForge entity interaction events, and sending carry payloads to carrier trackers as well as baby trackers.
- Local dirty Fabric and NeoForge builds now append a UTC timestamp to the generated MSK build metadata, so repeated playtest jars from the same commit can be distinguished without changing the public mod version.
- Implemented the Phase 2 NeoForge adapter/build foundation, including loader-suffixed Fabric and NeoForge jars, NeoForge metadata, config paths, permissions fallback, networking, client ModStatus/config wiring, render hooks, artifact verification gates, and completed manual NeoForge verification.
- Added a Phase 2 implementation plan for adding a separate NeoForge adapter/build on top of the accepted Fabric-preserving multiloader baseline.
- Accepted the Phase 1 Fabric-preserving multiloader refactor baseline after a clean general playtest found no issues in the exercised paths; remaining untested manual-test-plan items stay as residual risk for later verification.
- Implemented the Phase 1 Fabric-preserving multiloader source layout, moving loader-neutral code into common/commonClient roots and Fabric entrypoints, resources, mixins, networking, permissions, ModMenu, config path, and render hooks into Fabric adapter roots.
- Added a Phase 1 implementation plan for the Fabric-preserving multiloader refactor, covering common/Fabric source layout, adapter boundaries, verification gates, and Revue-reviewed handoff scope.
- Added a design-only multiloader refactor spec, reusable migration packet, and scoped agent guidance for a Fabric-preserving CarryBabyAnimals pilot before any NeoForge adapter work.
- Added a public changelog style gate, modeled on MultiGolem, so release notes stay framed for players and server admins instead of implementation history.
- Updated the embedded ModStatusKit helper to 0.1.8, added Gradle-generated build metadata stamping without changing the base jar filename, wired structured WARN severity status payloads, adopted the teal diagnostic tone for different builds of the same public version, and aligned the Fabric reference status UI/networking helpers.
- Embedded ModStatusKit 0.1.1 from `TnTBass/ModStatusKit` commit `06f37b9` under this mod's internal package and wired passive, capability-gated client/server version status display.
- Added a Phase 5 extension sleepy carried-baby visuals implementation plan for clearer client-only sleepy/asleep presentation.
- Added a Phase 5 extension sleepy carried-baby visuals design spec for clearer client-only sleepy/asleep presentation while preserving vanilla fallback.
- Added a Phase 5 extension carried baby reactions implementation plan for large-baby visibility and optional client-only creature personality polish.
- Added a post-Phase-5 carried baby reactions design spec for large-baby visibility and optional creature personality polish.
- Renamed generated jar artifacts from `carry-baby-animals-*` to `carrybabyanimals-*` and aligned release upload paths.
- Hardened Phase 1 Cozy Feedback after implementation review by rejecting non-server pickup timing fallbacks, varying sleepy message selection, and expanding scheduler feature-flag tests.
- Added a Phase 1 Cozy Feedback implementation plan for the lovable expansion roadmap.
- Actioned Revue design-spec findings on the lovable expansion roadmap by tightening config names, permission fallback wording, optional payload capability wording, particle config coverage, client cleanup language, and version-context wording.
- Added a maintainer roadmap design spec for future cozy feedback, nursery safety, parent reunion, modded animal support, and client polish phases.
- Hardened CurseForge upload verification to require and print the returned file ID, detect CurseForge `errorCode` responses, remove the invalid optional Fabric Permissions API project relation, and added a CurseForge-only retry workflow.
- Tightened README and marketplace-description wording, plus the release-source gate, so Fabric Permissions API is explicitly described as optional and not required.
- Added a release workflow gate that prints the manual CurseForge project description update instructions and exact `docs/marketplace-description.md` contents after publishing, and documented the 0.1.1 CurseForge follow-up lessons.
- Documented release and marketplace publishing lessons learned for future 0.1.x release work.
- Made Fabric Permissions API optional and documented the vanilla permission fallback.
- Expanded public configuration docs with every option, defaults, supported names, and example configs.
- Added a manual marketplace metadata sync workflow for Modrinth description-only updates.
- Added a shared marketplace description and wired Modrinth release automation to sync it.
- Switched CurseForge uploads to resolve numeric game version IDs and send metadata as a multipart JSON file part.
- Made the Modrinth release step tolerate reruns when a version was already created.
- Switched Modrinth version uploads to send the version metadata as a multipart JSON file part.
- Corrected the Modrinth release workflow slug to match the live project page.
- Made the GitHub release workflow safe to rerun for an existing tag by updating notes and replacing assets.
- Fixed the release workflow to make the Gradle wrapper executable on Linux runners before building.
- Added release publishing automation for GitHub Releases, Modrinth, and CurseForge, including a source gate for public release notes and Modrinth server-required/client-optional metadata.
- Added server-side pickup diagnostic logging for baby-animal interaction troubleshooting.
- Hardened the scoped player-passenger attachment mixin and client carry-render state after code review.
- Added a mechanical public/internal changelog split with a Gradle changelog gate and release-prep public-notes check.
