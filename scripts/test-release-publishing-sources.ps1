Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$RepoRoot = Split-Path -Parent $PSScriptRoot

function Assert-Contains {
    param(
        [string] $Text,
        [string] $Needle,
        [string] $Message
    )

    if (-not $Text.Contains($Needle)) {
        throw $Message
    }
}

function Assert-NotContains {
    param(
        [string] $Text,
        [string] $Needle,
        [string] $Message
    )

    if ($Text.Contains($Needle)) {
        throw $Message
    }
}

function Assert-True {
    param(
        [bool] $Condition,
        [string] $Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Get-Text {
    param([string] $RelativePath)

    $path = Join-Path $RepoRoot $RelativePath
    if (-not (Test-Path -LiteralPath $path)) {
        throw "$RelativePath is missing."
    }
    return Get-Content -Raw -LiteralPath $path
}

function Get-GradleProperty {
    param([string] $Name)

    $propertiesPath = Join-Path $RepoRoot 'gradle.properties'
    $match = Get-Content -LiteralPath $propertiesPath | Where-Object { $_ -match "^$([regex]::Escape($Name))=(.+)$" } | Select-Object -First 1
    if (-not $match) {
        throw "gradle.properties is missing $Name."
    }
    return ($match -replace "^$([regex]::Escape($Name))=", "").Trim()
}

function Test-ReleaseWorkflowPublishesPublicNotesOnly {
    $workflow = Get-Text '.github/workflows/release.yml'

    Assert-Contains $workflow 'CHANGELOG.md' 'Release workflow must extract public notes from CHANGELOG.md.'
    Assert-Contains $workflow 'docs/marketplace-description.md' 'Release workflow must pass the public marketplace description to upload scripts.'
    Assert-Contains $workflow 'chmod +x ./gradlew' 'Release workflow must make gradlew executable on Linux runners.'
    Assert-Contains $workflow 'gh release view "${GITHUB_REF_NAME}"' 'Release workflow must handle reruns when the GitHub release already exists.'
    Assert-Contains $workflow 'gh release upload "${GITHUB_REF_NAME}" --clobber' 'Release workflow must replace release assets on reruns.'
    Assert-Contains $workflow '-ChangelogPath "release-notes.md"' 'Release workflow must pass public release-notes.md to marketplace scripts.'
    Assert-Contains $workflow 'artifact_version="${version}+mc${minecraft_version}"' 'Release workflow must derive artifact names from mod version plus Minecraft version.'
    Assert-Contains $workflow 'fabric/build/libs/carrybabyanimals-fabric-${artifact_version}.jar' 'Release workflow must publish the normalized Fabric jar.'
    Assert-Contains $workflow 'fabric/build/libs/carrybabyanimals-fabric-${artifact_version}-sources.jar' 'Release workflow must publish the normalized Fabric sources jar.'
    Assert-Contains $workflow 'neoforge/build/libs/carrybabyanimals-neoforge-${artifact_version}.jar' 'Release workflow must publish the normalized NeoForge jar.'
    Assert-Contains $workflow 'neoforge/build/libs/carrybabyanimals-neoforge-${artifact_version}-sources.jar' 'Release workflow must publish the normalized NeoForge sources jar.'
    Assert-NotContains $workflow '"build/libs/carrybabyanimals-${version}.jar"' 'Release workflow must not publish the old unsuffixed root jar.'
    Assert-NotContains $workflow '"build/libs/carrybabyanimals-${version}-sources.jar"' 'Release workflow must not publish the old unsuffixed root sources jar.'
    Assert-NotContains $workflow 'INTERNAL_CHANGELOG.md' 'Release workflow must not publish maintainer-only INTERNAL_CHANGELOG.md.'
}

function Test-MarketplaceMetadataSyncWorkflow {
    $workflow = Get-Text '.github/workflows/sync-marketplace-metadata.yml'

    Assert-Contains $workflow 'workflow_dispatch' 'Marketplace metadata sync workflow must be manually runnable.'
    Assert-Contains $workflow '-SyncDescriptionOnly' 'Marketplace metadata sync workflow must update Modrinth metadata without uploading files.'
    Assert-Contains $workflow 'docs/marketplace-description.md' 'Marketplace metadata sync workflow must use the shared marketplace description.'
    Assert-Contains $workflow 'CurseForge upload API does not expose project-page description updates' 'Marketplace metadata sync workflow must document the CurseForge limitation.'
}

function Test-ModrinthUploadEnforcesRequiredServerOptionalClient {
    $script = Get-Text 'scripts/upload-modrinth.ps1'
    $workflow = Get-Text '.github/workflows/release.yml'

    Assert-Contains $workflow '-Slug "carrybabyanimals"' 'Release workflow must publish to the real Modrinth slug.'
    Assert-Contains $workflow '-DescriptionPath "docs/marketplace-description.md"' 'Release workflow must sync Modrinth project description from the public marketplace description.'
    Assert-Contains $script '[string] $Slug = "carrybabyanimals"' 'Modrinth upload default slug must match the real project slug.'
    Assert-Contains $script '[string] $DescriptionPath = "docs/marketplace-description.md"' 'Modrinth upload must default to the shared marketplace description.'
    Assert-Contains $script '[switch] $SyncDescriptionOnly' 'Modrinth upload must support description-only sync.'
    Assert-Contains $script '$projectPatch.body' 'Modrinth upload must patch the project body/description.'
    Assert-Contains $script 'DescriptionSynced' 'Modrinth upload must report description sync status.'
    Assert-Contains $script 'server_only_client_optional' 'Modrinth upload must document the new environment mapping.'
    Assert-Contains $script 'client_side = "optional"' 'Modrinth upload must set client_side optional.'
    Assert-Contains $script 'server_side = "required"' 'Modrinth upload must set server_side required.'
    Assert-Contains $script '[ValidateSet("fabric", "neoforge")]' 'Modrinth upload must require an explicit supported loader.'
    Assert-Contains $script '[string] $Loader = "fabric"' 'Modrinth upload must default to Fabric for backwards-compatible manual reruns.'
    Assert-Contains $script '$artifactVersion = "$Version+mc$minecraftVersion"' 'Modrinth upload must derive normalized Modrinth version numbers.'
    Assert-Contains $script '$modrinthVersionNumber = $artifactVersion' 'Modrinth upload must use the normalized artifact version as the public Modrinth version number.'
    Assert-Contains $script 'name = "Carry Baby Animals $Version for Minecraft $minecraftVersion"' 'Modrinth upload must display the shared friendly version name for each loader row.'
    Assert-NotContains $script '$modrinthVersionNumber = "$Version-$Loader"' 'Modrinth upload must not create loader-suffixed Modrinth version numbers.'
    Assert-NotContains $script '$loaderDisplayName = if ($Loader -eq "neoforge") { "NeoForge" } else { "Fabric" }' 'Modrinth upload must not add loader text to public version names.'
    Assert-NotContains $script 'name = $modrinthVersionNumber' 'Modrinth upload must not display the version number as the public version name.'
    Assert-Contains $script 'version_number = $modrinthVersionNumber' 'Modrinth upload must publish normalized version numbers.'
    Assert-Contains $script 'if ($Loader -eq "fabric")' 'Modrinth upload must scope Fabric-only metadata to Fabric uploads.'
    Assert-Contains $script 'P7dR8mSH' 'Modrinth Fabric upload must include Fabric API as a dependency.'
    Assert-Contains $script 'dependency_type = "required"' 'Modrinth Fabric upload must mark Fabric API as required.'
    Assert-NotContains $script 'lzVo0Dll' 'Modrinth upload must not advertise the legacy Fabric Permissions API fallback.'
    Assert-Contains $script '$matchingVersions = foreach ($projectVersion in $projectVersions)' 'Modrinth upload must enumerate project versions before duplicate detection.'
    Assert-Contains $script '($projectVersion.version_number -eq $modrinthVersionNumber) -and ($projectVersion.loaders -contains $Loader)' 'Modrinth upload must skip only an already-created release row for the same loader.'
    Assert-Contains $script 'loaders = @($Loader)' 'Modrinth upload must publish exactly one loader per Modrinth version.'
    Assert-Contains $script 'file_parts = @("mod")' 'Modrinth upload must attach only the runtime mod jar.'
    Assert-Contains $script 'primary_file = "mod"' 'Modrinth upload must mark the runtime mod jar as primary.'
    Assert-Contains $workflow 'Publish Fabric to Modrinth' 'Release workflow must publish a separate Fabric Modrinth version.'
    Assert-Contains $workflow 'Publish NeoForge to Modrinth' 'Release workflow must publish a separate NeoForge Modrinth version.'
    Assert-Contains $workflow '-Loader "fabric"' 'Release workflow must pass the Fabric loader to Modrinth.'
    Assert-Contains $workflow '-Loader "neoforge"' 'Release workflow must pass the NeoForge loader to Modrinth.'
    Assert-Contains $workflow '$artifactVersion = "$version+mc$minecraftVersion"' 'Release workflow PowerShell steps must derive normalized artifact versions.'
    Assert-Contains $workflow '-JarPath "fabric/build/libs/carrybabyanimals-fabric-$artifactVersion.jar"' 'Release workflow must pass the Fabric runtime jar path to Modrinth.'
    Assert-Contains $workflow '-JarPath "neoforge/build/libs/carrybabyanimals-neoforge-$artifactVersion.jar"' 'Release workflow must pass the NeoForge runtime jar path to Modrinth.'
    Assert-NotContains $script 'SourcesJarPath' 'Modrinth upload must not accept or publish sources jars.'
    Assert-NotContains $script 'FabricSourcesJarPath' 'Modrinth upload must not accept Fabric sources jars.'
    Assert-NotContains $script 'NeoForgeSourcesJarPath' 'Modrinth upload must not accept NeoForge sources jars.'
    Assert-NotContains $script 'fabric-sources' 'Modrinth upload must not attach Fabric sources jars.'
    Assert-NotContains $script 'neoforge-sources' 'Modrinth upload must not attach NeoForge sources jars.'
    Assert-NotContains $workflow '-FabricSourcesJarPath' 'Release workflow must not pass Fabric sources jars to Modrinth.'
    Assert-NotContains $workflow '-NeoForgeSourcesJarPath' 'Release workflow must not pass NeoForge sources jars to Modrinth.'
    Assert-NotContains $script 'loaders = @("fabric", "neoforge")' 'Modrinth upload must not publish a combined Fabric and NeoForge version.'
    Assert-Contains $script 'PSObject.Properties.Name -contains "error"' 'Modrinth upload must check API error payloads safely under StrictMode.'
    Assert-Contains $script 'data=<' 'Modrinth upload must send version JSON as a multipart JSON file part.'
    Assert-Contains $script 'curl' 'Modrinth upload must use curl for multipart version upload.'
    Assert-Contains $script 'EnvironmentSynced' 'Modrinth upload must report environment sync status.'
}

function Test-CurseForgeUploadDocumentsSideLimitAndDependencies {
    $script = Get-Text 'scripts/upload-curseforge.ps1'
    $workflow = Get-Text '.github/workflows/release.yml'

    Assert-Contains $workflow '-DescriptionPath "docs/marketplace-description.md"' 'Release workflow must pass the public marketplace description to CurseForge upload script.'
    Assert-Contains $workflow 'Manual CurseForge project description update' 'Release workflow must end by flagging the manual CurseForge project description update.'
    Assert-Contains $workflow 'Copy the full contents of docs/marketplace-description.md' 'Release workflow must tell the maintainer exactly what to copy into CurseForge.'
    Assert-Contains $workflow 'cat docs/marketplace-description.md' 'Release workflow must print the exact CurseForge description source text.'
    Assert-Contains $script '[string] $DescriptionPath = "docs/marketplace-description.md"' 'CurseForge upload must point at the shared marketplace description.'
    Assert-Contains $script '[ValidateSet("fabric", "neoforge")]' 'CurseForge upload must require an explicit supported loader.'
    Assert-Contains $script '[string] $Loader = "fabric"' 'CurseForge upload must default to Fabric for backwards-compatible manual reruns.'
    Assert-Contains $script 'CurseForge upload API does not expose project-page description updates' 'CurseForge upload script must document the project description limitation.'
    Assert-Contains $script 'CurseForge upload API does not expose Modrinth-style client/server side fields' 'CurseForge upload script must document the side metadata limitation.'
    Assert-Contains $script 'Get-CurseForgeGameVersionId' 'CurseForge upload must resolve numeric game version IDs.'
    Assert-Contains $script 'metadata=<' 'CurseForge upload must send metadata as a multipart JSON file part.'
    Assert-Contains $script 'slug = "fabric-api"' 'CurseForge upload must include Fabric API as a required relation.'
    Assert-Contains $script '$relations = @()' 'CurseForge upload must keep project relations as an array even when there is only one dependency.'
    Assert-Contains $script 'if ($relations.Count -gt 0)' 'CurseForge upload must omit relation metadata when a loader has no dependencies.'
    Assert-Contains $script 'Get-CurseForgeGameVersionId -GameVersions $gameVersions -Name "NeoForge"' 'CurseForge upload must resolve the NeoForge loader game version.'
    Assert-Contains $workflow '-Loader "fabric"' 'Release workflow must upload the Fabric CurseForge file.'
    Assert-Contains $workflow '-Loader "neoforge"' 'Release workflow must upload the NeoForge CurseForge file.'
    Assert-Contains $script 'type = "requiredDependency"' 'CurseForge upload must mark supported dependencies required.'
    Assert-NotContains $script 'slug = "fabric-permissions-api"' 'CurseForge upload must not reference a non-existent Fabric Permissions API project relation.'
    Assert-Contains $script 'errorCode' 'CurseForge upload must detect CurseForge API errorCode payloads.'
    Assert-Contains $script 'CurseForgeFileId' 'CurseForge upload must print the returned CurseForge file ID for verification.'
    Assert-Contains $script '$curseForgeFile.id' 'CurseForge upload must require the returned CurseForge file ID before reporting success.'
}

function Test-CurseForgeOnlyRetryWorkflow {
    $workflow = Get-Text '.github/workflows/retry-curseforge-upload.yml'

    Assert-Contains $workflow 'workflow_dispatch' 'CurseForge retry workflow must be manually runnable.'
    Assert-Contains $workflow 'mod_version=${{ inputs.version }}' 'CurseForge retry workflow must verify the checked-out branch matches the requested version.'
    Assert-Contains $workflow 'loader:' 'CurseForge retry workflow must let the maintainer choose which loader to retry.'
    Assert-Contains $workflow 'fabric,neoforge,both' 'CurseForge retry workflow must support retrying either loader or both.'
    Assert-Contains $workflow './scripts/upload-curseforge.ps1' 'CurseForge retry workflow must use the shared CurseForge upload script.'
    Assert-Contains $workflow '$artifactVersion = "$version+mc$minecraftVersion"' 'CurseForge retry workflow must derive normalized artifact versions.'
    Assert-Contains $workflow '-JarPath "fabric/build/libs/carrybabyanimals-fabric-$artifactVersion.jar"' 'CurseForge retry workflow must upload the Fabric release jar.'
    Assert-Contains $workflow '-JarPath "neoforge/build/libs/carrybabyanimals-neoforge-$artifactVersion.jar"' 'CurseForge retry workflow must upload the NeoForge release jar.'
    Assert-Contains $workflow '-Loader "fabric"' 'CurseForge retry workflow must pass the Fabric loader.'
    Assert-Contains $workflow '-Loader "neoforge"' 'CurseForge retry workflow must pass the NeoForge loader.'
    Assert-Contains $workflow 'Report Fabric CurseForge file ID' 'CurseForge retry workflow must report Fabric uploads separately.'
    Assert-Contains $workflow 'Report NeoForge CurseForge file ID' 'CurseForge retry workflow must report NeoForge uploads separately.'
    Assert-Contains $workflow 'steps.curseforge_fabric.outputs.curseforge_file_id' 'CurseForge retry workflow must report the Fabric file ID only from the Fabric upload step.'
    Assert-Contains $workflow 'steps.curseforge_neoforge.outputs.curseforge_file_id' 'CurseForge retry workflow must report the NeoForge file ID only from the NeoForge upload step.'
    Assert-NotContains $workflow 'upload-modrinth.ps1' 'CurseForge retry workflow must not republish Modrinth.'
    Assert-NotContains $workflow 'gh release create' 'CurseForge retry workflow must not create another GitHub release.'
}

function Test-GradleRunsReleasePublishingSourceGate {
    $build = Get-Text 'build.gradle'

    Assert-Contains $build 'tasks.register("checkReleasePublishingSources"' 'Gradle must register checkReleasePublishingSources.'
    Assert-Contains $build 'test-release-publishing-sources.ps1' 'Gradle release publishing gate must run the PowerShell test script.'
    Assert-Contains $build 'dependsOn tasks.named("checkReleasePublishingSources")' 'Gradle check must depend on checkReleasePublishingSources.'
}

function Test-GradleRunsReleaseNotesStyleGate {
    $build = Get-Text 'build.gradle'

    Assert-Contains $build 'tasks.register("checkReleaseNotesStyle"' 'Gradle must register checkReleaseNotesStyle.'
    Assert-Contains $build 'scripts/check-changelog-style.py' 'Release notes style gate must run the changelog style checker.'
    Assert-Contains $build 'dependsOn tasks.named("checkReleaseNotesStyle")' 'Gradle check must depend on checkReleaseNotesStyle.'
}

function Test-BuildMetadataDoesNotExposeDirtyTimestamp {
    $build = Get-Text 'build.gradle'

    Assert-NotContains $build '.dirty.' 'Generated build metadata must not expose a dirty marker or timestamp in ModStatus displays.'
    Assert-NotContains $build 'git status --porcelain' 'Generated build metadata must not vary only because the worktree has uncommitted files.'
}

function Test-ChangelogStyleScriptFlagsDevelopmentLogEntries {
    $script = Join-Path $RepoRoot 'scripts/check-changelog-style.py'
    if (-not (Test-Path -LiteralPath $script)) {
        throw 'scripts/check-changelog-style.py is missing.'
    }

    $caseRoot = Join-Path $RepoRoot 'build/release-notes-style-tests'
    New-Item -ItemType Directory -Force $caseRoot | Out-Null
    $caseChangelog = Join-Path $caseRoot 'CHANGELOG.md'
    @'
# CarryBabyAnimals Changelog

## Unreleased

- Added Nursery Mode safety checks so carried babies are not set down in lava.
- Fixed CarryStateMixin TAIL inject handling for task 5.
'@ | Set-Content -Encoding UTF8 $caseChangelog

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $output = & python $script --changelog $caseChangelog --section Unreleased 2>&1
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    Assert-True ($LASTEXITCODE -ne 0) 'Development-log changelog entry unexpectedly passed style gate.'
    Assert-Contains ($output -join "`n") 'players/server admins' 'Style gate failure should explain the public changelog audience.'
    Assert-Contains ($output -join "`n") 'mixin implementation detail' 'Style gate should flag mixin implementation details.'
}

function Test-ChangelogStyleScriptAllowsPlayerAdminReleaseNotes {
    $script = Join-Path $RepoRoot 'scripts/check-changelog-style.py'
    if (-not (Test-Path -LiteralPath $script)) {
        throw 'scripts/check-changelog-style.py is missing.'
    }

    $caseRoot = Join-Path $RepoRoot 'build/release-notes-style-tests'
    New-Item -ItemType Directory -Force $caseRoot | Out-Null
    $caseChangelog = Join-Path $caseRoot 'CHANGELOG.md'
    @'
# CarryBabyAnimals Changelog

## Unreleased

- Added Nursery Mode safety checks so carried babies are not set down in lava, fire, cactus, cramped spaces, or unsafe drops.
- Added server config support for full entity IDs in `allowedAnimals` and `blockedAnimals`.
'@ | Set-Content -Encoding UTF8 $caseChangelog

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $output = & python $script --changelog $caseChangelog --section Unreleased 2>&1
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    Assert-True ($LASTEXITCODE -eq 0) "Player/admin changelog entry failed style gate: $($output -join "`n")"
}

function Test-FabricPermissionRequirementsUseModernFabricApi {
    $modJson = Get-Text 'src/fabric/resources/fabric.mod.json' | ConvertFrom-Json

    if ($modJson.depends.PSObject.Properties.Name -contains 'fabric-permissions-api-v0') {
        throw 'Fabric Permissions API must not be a required dependency.'
    }
    if ($modJson.suggests.PSObject.Properties.Name -contains 'fabric-permissions-api-v0') {
        throw 'Legacy Fabric Permissions API must not be advertised as an optional integration.'
    }
    if ($modJson.depends.'fabric-api' -ne '>=${fabricVersion}') {
        throw 'Fabric API dependency must declare the verified modern permission API baseline.'
    }
}

function Test-LoaderMetadataUsesOpenMinecraftFloor {
    $modrinthUpload = Get-Text 'scripts/upload-modrinth.ps1'
    $curseForgeUpload = Get-Text 'scripts/upload-curseforge.ps1'
    $fabricBuild = Get-Text 'fabric/build.gradle'
    $modJson = Get-Text 'src/fabric/resources/fabric.mod.json' | ConvertFrom-Json
    $neoForgeToml = Get-Text 'src/neoforge/resources/META-INF/neoforge.mods.toml'

    if ($modJson.depends.minecraft -ne '>=${minecraftVersion}') {
        throw 'Fabric metadata must use the pinned build Minecraft version as an open lower bound.'
    }
    Assert-NotContains (Get-Text 'src/fabric/resources/fabric.mod.json') '"minecraft": "26.' 'Fabric metadata must not hard-pin Minecraft to an exact 26.x version.'
    Assert-Contains $fabricBuild 'inputs.property "minecraftVersion", rootProject.minecraft_version' 'Fabric resource processing must derive the Minecraft metadata floor from gradle.properties.'
    Assert-Contains $fabricBuild 'inputs.property "fabricVersion", rootProject.fabric_version' 'Fabric resource processing must derive the Fabric API metadata floor from gradle.properties.'
    Assert-Contains $neoForgeToml 'versionRange="[${minecraftVersion},)"' 'NeoForge metadata must use minecraftVersion as an open lower-bound range.'
    Assert-Contains $modrinthUpload '$minecraftVersion = Get-GradleProperty "minecraft_version"' 'Modrinth upload must resolve the pinned build target for exact tested game-version metadata.'
    Assert-Contains $modrinthUpload 'game_versions = @($minecraftVersion)' 'Modrinth upload must keep the pinned build target as the exact tested game version.'
    Assert-Contains $curseForgeUpload '$minecraftVersion = Get-GradleProperty "minecraft_version"' 'CurseForge upload must resolve the pinned build target for exact tested game-version metadata.'
}

function Test-MarketplaceDescriptionExists {
    $description = Get-Text 'docs/marketplace-description.md'

    Assert-Contains $description 'Carry Baby Animals' 'Marketplace description must name the mod.'
    Assert-Contains $description 'father-daughter project by Tyler and Jasmine' 'Marketplace description must include the father-daughter project note.'
    Assert-Contains $description 'server' 'Marketplace description must explain server setup.'
    Assert-Contains $description 'client' 'Marketplace description must explain optional client setup.'
    Assert-Contains $description 'server-required and the client mod is highly recommended' 'Marketplace description must describe the player-facing setup as server-required with the client mod highly recommended.'
    Assert-Contains $description 'Marketplace environment metadata may list the client as optional' 'Marketplace description must explain why marketplace environment metadata can still list the client as optional.'
    Assert-Contains $description 'Fabric API 0.152.2+26.2 or newer when running on Fabric' 'Marketplace description must document the verified Fabric API baseline.'
    Assert-Contains $description 'NeoForge servers use NeoForge''s built-in permission API' 'Marketplace description must document NeoForge permission provider behavior.'
}

function Test-ReadmeContainsReleaseCriticalFacts {
    $readme = Get-Text 'README.md'

    Assert-Contains $readme 'Carry Baby Animals' 'README must name the mod.'
    Assert-Contains $readme 'father-daughter project by Tyler and Jasmine' 'README must include the father-daughter project note.'
    Assert-Contains $readme 'server-required and the client mod is highly recommended' 'README must describe the player-facing setup as server-required with the client mod highly recommended.'
    Assert-Contains $readme 'Marketplace environment metadata may list the client as optional' 'README must explain why marketplace environment metadata can still list the client as optional.'
    Assert-Contains $readme 'Players without the mod can still connect to a modded server' 'README must explain the vanilla-client fallback.'
    Assert-Contains $readme 'allowedAnimals' 'README must document the allowedAnimals configuration option.'
    Assert-Contains $readme 'blockedAnimals' 'README must document the blockedAnimals configuration option.'
    Assert-Contains $readme 'allowCarryingOtherPlayersTamedAnimals' 'README must document the tamed-animal ownership configuration option.'
    Assert-Contains $readme 'pettingCooldownTicks' 'README must document the petting cooldown configuration option.'
    Assert-Contains $readme 'Supported animal names:' 'README must document supported config animal names.'
    Assert-Contains $readme 'Fabric API 0.152.2+26.2 or newer when running on Fabric' 'README must document the verified Fabric API baseline.'
    Assert-Contains $readme 'NeoForge servers use NeoForge''s built-in permission API' 'README must document NeoForge permission provider behavior.'
    Assert-Contains $readme 'If no loader permission provider is active' 'README must document permission behavior without a loader permission provider.'
    Assert-Contains $readme 'Players cannot carry another player''s tamed baby animals.' 'README must document the no-permissions fallback for other players'' tamed animals.'
}

function Test-PowerShellPublishingScriptsParse {
    [scriptblock]::Create((Get-Text 'scripts/upload-modrinth.ps1')) | Out-Null
    [scriptblock]::Create((Get-Text 'scripts/upload-curseforge.ps1')) | Out-Null
}

Test-ReleaseWorkflowPublishesPublicNotesOnly
Test-MarketplaceMetadataSyncWorkflow
Test-ModrinthUploadEnforcesRequiredServerOptionalClient
Test-CurseForgeUploadDocumentsSideLimitAndDependencies
Test-CurseForgeOnlyRetryWorkflow
Test-GradleRunsReleasePublishingSourceGate
Test-GradleRunsReleaseNotesStyleGate
Test-BuildMetadataDoesNotExposeDirtyTimestamp
Test-ChangelogStyleScriptFlagsDevelopmentLogEntries
Test-ChangelogStyleScriptAllowsPlayerAdminReleaseNotes
Test-FabricPermissionRequirementsUseModernFabricApi
Test-LoaderMetadataUsesOpenMinecraftFloor
Test-MarketplaceDescriptionExists
Test-ReadmeContainsReleaseCriticalFacts
Test-PowerShellPublishingScriptsParse

Write-Host 'release publishing source tests passed'
