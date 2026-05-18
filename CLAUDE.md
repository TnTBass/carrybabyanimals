# Claude Notes

## Changelog Policy

Choose public vs internal changelog entries deliberately.

- `CHANGELOG.md` is for public player and server-admin release notes. These notes may be published to GitHub Releases, Modrinth, CurseForge, and other marketplace pages.
- `INTERNAL_CHANGELOG.md` is for maintainer-only repo, build, workflow, release-process, and other internal notes. These notes must not be published to GitHub Releases, Modrinth, CurseForge, or marketplace pages.
- Releasable changes must update either `CHANGELOG.md` for public behavior/admin impact or `INTERNAL_CHANGELOG.md` for maintainer-only changes.
- Before a release push or tag, show Tyler the exact public versioned `CHANGELOG.md` section that will be published and wait for approval.

Use `.\gradlew.bat checkChangelog` to run the changelog gate. Use `.\gradlew.bat showPublicReleaseNotes -PreleaseVersion=<version>` during release prep to print the exact public section from `CHANGELOG.md`.
