# Internal Changelog

Maintainer-only notes for repo, build, workflow, release-process, and other internal project changes. This changelog is not published to GitHub Releases, Modrinth, CurseForge, or other public marketplace pages.

## Unreleased

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
