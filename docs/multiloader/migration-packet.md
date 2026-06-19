# Multiloader Migration Packet

Use this packet to repeat the CarryBabyAnimals pilot in other mod repos. It is a practical checklist, not a reusable library design.

## Migration Shape

1. Preserve the current loader first.
2. Identify all loader API imports and resource declarations.
3. Define the target multiloader-shaped Gradle/source layout before moving files.
4. Add scoped `AGENTS.md` files where the new layout needs ownership rules.
5. Split reusable domain behavior from loader registration, transport, config-path, permissions, metadata, optional UI, and render hooks.
6. Keep the existing loader jar behavior unchanged.
7. Verify the existing loader before adding the second loader.
8. Add the second loader as a separate adapter/build later.

## Phase 1: Existing-Loader Preservation

- Keep the current loader's entrypoints and metadata working.
- Move into the long-term common/current-loader source layout during this phase, not after second-loader work begins.
- Introduce narrow platform interfaces only where loader APIs leak into otherwise reusable logic.
- Move common behavior behind those boundaries in small steps.
- Preserve packet names, config file names, permission nodes, public behavior, and optional integration semantics.
- Modernize permission-provider integration while boundaries are already open: use Fabric API's current permission API on Fabric, keep provider-specific libraries such as LuckPerms out of the compile-time integration, and leave provider calls out of common code.
- Add static scans that prevent loader imports from creeping back into common packages.
- Add local `AGENTS.md` files to explain common versus loader-adapter ownership.
- Run automated and manual verification before any second-loader work.

## Phase 2: Second Loader Adapter

- Add a separate loader adapter/build after Phase 1 is stable.
- Do not make a universal jar.
- Reuse common payload semantics, config parsing, domain services, render math, and tests.
- Implement loader-native entrypoints, metadata, networking registration, lifecycle hooks, permissions checks, config path lookup, and render hooks.
- Use NeoForge's permission API and registered permission nodes for NeoForge permissions. Do not build directly against LuckPerms or any provider-specific API.
- Keep release notes and marketplace metadata loader-specific where needed.

## Copy Checklist for Other Mods

- Replace mod ID, package name, current loader, target second loader, and Minecraft version.
- Replace `<repo root>` in `verification-matrix.md` with the target repo's absolute root path.
- Decide the exact source-set or module names for `common`, the current loader adapter, and the future second-loader adapter.
- Add or tailor `AGENTS.md` files at the roots of those areas.
- Re-run touchpoint discovery using `rg`.
- Fill in the repo-specific networking channels and payload contracts.
- Fill in the repo-specific render hooks and mixins.
- Fill in optional integrations and permission defaults.
- Decide whether Modrinth will publish one combined loader version or separate loader versions. If combined, document that loader-specific dependencies are version-wide and may need to be optional.
- Plan loader-suffixed GitHub Release and marketplace artifact names, including unique sources-jar names.
- Include CurseForge retry and relation-metadata cases for both loaders in release-source checks.
- Replace verification commands with the repo's gates.
- Send the repo-specific spec and packet through Revue before implementation planning.

## Review Gate

Before implementation planning, review:

- The repo-specific design/spec.
- `fabric-touchpoints.md` or equivalent current-loader touchpoint inventory.
- `boundary-rules.md`.
- `verification-matrix.md`.
- `lessons-learned.md`.

Use a bounded `design-spec-review` with explicit files.
