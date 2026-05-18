# Internal Changelog

Maintainer-only notes for repo, build, workflow, release-process, and other internal project changes. This changelog is not published to GitHub Releases, Modrinth, CurseForge, or other public marketplace pages.

## Unreleased

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
