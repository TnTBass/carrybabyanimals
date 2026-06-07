# Agent Notes

## Changelog Policy

Choose public vs internal changelog entries deliberately.

- `CHANGELOG.md` is for public player and server-admin release notes. These notes may be published to GitHub Releases, Modrinth, CurseForge, and other marketplace pages.
- `INTERNAL_CHANGELOG.md` is for maintainer-only repo, build, workflow, release-process, and other internal notes. These notes must not be published to GitHub Releases, Modrinth, CurseForge, or marketplace pages.
- Releasable changes must update either `CHANGELOG.md` for public behavior/admin impact or `INTERNAL_CHANGELOG.md` for maintainer-only changes.
- Before a release push or tag, show Tyler the exact public versioned `CHANGELOG.md` section that will be published and wait for approval.

Use `.\gradlew.bat checkChangelog` to run the changelog gate. Use `.\gradlew.bat showPublicReleaseNotes -PreleaseVersion=<version>` during release prep to print the exact public section from `CHANGELOG.md`.

## Multiloader Layout Notes

The multiloader refactor is intended to preserve Fabric behavior first while creating a long-term common/platform source layout.

- Common code must stay loader-neutral and must not import Fabric, ModMenu, Fabric Permissions API, or future NeoForge APIs directly.
- Fabric adapter code owns Fabric entrypoints, Fabric metadata/resources, mixin declarations, Fabric networking registration, Fabric permission integration, Fabric config-path lookup, Fabric render hook registration, and ModMenu integration.
- NeoForge work is a later adapter/build after the Fabric-preserving refactor is stable and verified.
- Add narrower `AGENTS.md` files in future common, Fabric, and NeoForge source roots when those directories are created so ownership rules are visible at edit time.
