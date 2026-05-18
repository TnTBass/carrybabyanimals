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

function Get-Text {
    param([string] $RelativePath)

    $path = Join-Path $RepoRoot $RelativePath
    if (-not (Test-Path -LiteralPath $path)) {
        throw "$RelativePath is missing."
    }
    return Get-Content -Raw -LiteralPath $path
}

function Test-ReleaseWorkflowPublishesPublicNotesOnly {
    $workflow = Get-Text '.github/workflows/release.yml'

    Assert-Contains $workflow 'CHANGELOG.md' 'Release workflow must extract public notes from CHANGELOG.md.'
    Assert-Contains $workflow 'docs/marketplace-description.md' 'Release workflow must pass the public marketplace description to upload scripts.'
    Assert-Contains $workflow 'chmod +x ./gradlew' 'Release workflow must make gradlew executable on Linux runners.'
    Assert-Contains $workflow 'gh release view "${GITHUB_REF_NAME}"' 'Release workflow must handle reruns when the GitHub release already exists.'
    Assert-Contains $workflow 'gh release upload "${GITHUB_REF_NAME}" --clobber' 'Release workflow must replace release assets on reruns.'
    Assert-Contains $workflow '-ChangelogPath "release-notes.md"' 'Release workflow must pass public release-notes.md to marketplace scripts.'
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
    Assert-Contains $script 'P7dR8mSH' 'Modrinth upload must include Fabric API as a required dependency.'
    Assert-Contains $script 'lzVo0Dll' 'Modrinth upload must include fabric-permissions-api as an optional dependency.'
    Assert-Contains $script 'dependency_type = "optional"' 'Modrinth upload must mark optional integrations optional.'
    Assert-Contains $script 'version_number -eq $Version' 'Modrinth upload must skip an already-created release version on reruns.'
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
    Assert-Contains $script 'CurseForge upload API does not expose project-page description updates' 'CurseForge upload script must document the project description limitation.'
    Assert-Contains $script 'CurseForge upload API does not expose Modrinth-style client/server side fields' 'CurseForge upload script must document the side metadata limitation.'
    Assert-Contains $script 'Get-CurseForgeGameVersionId' 'CurseForge upload must resolve numeric game version IDs.'
    Assert-Contains $script 'metadata=<' 'CurseForge upload must send metadata as a multipart JSON file part.'
    Assert-Contains $script 'slug = "fabric-api"' 'CurseForge upload must include Fabric API as a required relation.'
    Assert-Contains $script 'type = "requiredDependency"' 'CurseForge upload must mark supported dependencies required.'
}

function Test-GradleRunsReleasePublishingSourceGate {
    $build = Get-Text 'build.gradle'

    Assert-Contains $build 'tasks.register("checkReleasePublishingSources"' 'Gradle must register checkReleasePublishingSources.'
    Assert-Contains $build 'test-release-publishing-sources.ps1' 'Gradle release publishing gate must run the PowerShell test script.'
    Assert-Contains $build 'dependsOn tasks.named("checkReleasePublishingSources")' 'Gradle check must depend on checkReleasePublishingSources.'
}

function Test-FabricPermissionsApiIsOptional {
    $modJson = Get-Text 'src/main/resources/fabric.mod.json' | ConvertFrom-Json

    if ($modJson.depends.PSObject.Properties.Name -contains 'fabric-permissions-api-v0') {
        throw 'Fabric Permissions API must not be a required dependency.'
    }
    if ($modJson.suggests.PSObject.Properties.Name -notcontains 'fabric-permissions-api-v0') {
        throw 'Fabric Permissions API should remain suggested for permission-plugin integration.'
    }
}

function Test-MarketplaceDescriptionExists {
    $description = Get-Text 'docs/marketplace-description.md'

    Assert-Contains $description 'Carry Baby Animals' 'Marketplace description must name the mod.'
    Assert-Contains $description 'father-daughter project by Tyler and Jasmine' 'Marketplace description must include the father-daughter project note.'
    Assert-Contains $description 'server' 'Marketplace description must explain server setup.'
    Assert-Contains $description 'client' 'Marketplace description must explain optional client setup.'
    Assert-Contains $description 'server-required and the client mod is highly recommended' 'Marketplace description must describe the player-facing setup as server-required with the client mod highly recommended.'
    Assert-Contains $description 'Marketplace environment metadata may list the client as optional' 'Marketplace description must explain why marketplace environment metadata can still list the client as optional.'
    Assert-Contains $description 'Fabric Permissions API, if you want permission-plugin integration' 'Marketplace description must present Fabric Permissions API as optional.'
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
    Assert-Contains $readme 'Fabric Permissions API, if you want permission-plugin integration' 'README must present Fabric Permissions API as optional.'
    Assert-Contains $readme 'If Fabric Permissions API is not installed' 'README must document permission behavior without Fabric Permissions API.'
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
Test-GradleRunsReleasePublishingSourceGate
Test-FabricPermissionsApiIsOptional
Test-MarketplaceDescriptionExists
Test-ReadmeContainsReleaseCriticalFacts
Test-PowerShellPublishingScriptsParse

Write-Host 'release publishing source tests passed'
