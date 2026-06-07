# Verification Matrix

Use this matrix for the Fabric-preserving phase before any NeoForge implementation begins.

## Automated Verification

| Check | Command or method | Required result |
| --- | --- | --- |
| Repository root | `git rev-parse --show-toplevel` | Resolves to `<repo root>`, the absolute path of the target repository. |
| Unit tests | `.\gradlew.bat test` | Passes. |
| Full build | `.\gradlew.bat build` | Passes and produces the Fabric jar. |
| Changelog gate | `.\gradlew.bat checkChangelog` | Passes. |
| Common import scan | `rg -n "net\.fabricmc|fabric\.api|com\.terraformersmc\.modmenu|me\.lucko\.fabric" <common paths>` | No matches in common packages. |
| Fabric artifact inspection | Inspect built jar contents | Fabric metadata, mixin configs, assets, generated `BuildInfo`, and entrypoint classes are present. |
| Packet compatibility | Unit tests around packet semantics | Existing channel names, fields, byte limits, and ModStatus encoding remain stable. |

## Manual Fabric Verification

| Area | Required pass |
| --- | --- |
| Singleplayer | Pickup, carry, petting, set-down, growth cleanup, Nursery Mode, Parent Reunion, config defaults. |
| Multiplayer modded clients | Pickup visibility, held render, passenger render suppression, tracking replay, stale clear, pet feedback, disconnect cleanup. |
| Vanilla-compatible fallback | Client without this mod stays connected, sees vanilla passenger fallback, and does not require custom payloads. |
| Permissions | Fabric Permissions API provider overrides each permission node and vanilla fallback defaults still apply without the API. |
| ModMenu/config UI | Optional ModMenu opens config screen, saves client config, and absence of ModMenu does not block startup. |
| ModStatus | Matched, build-different, version-different, server-not-detected, unknown, and disconnected states behave as before. |
| Rendering polish | Large-baby first-person visibility, sleepy visuals, reaction softening, and other-player views remain correct. |

## Release Boundary

Phase 1 is not ready for implementation closeout until automated checks pass and the manual Fabric matrix has been recorded against the refactored build.

Phase 2 must not begin until Phase 1 verification is complete and Tyler approves moving to NeoForge adapter planning.
