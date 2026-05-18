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
    Assert-Contains $script 'lzVo0Dll' 'Modrinth upload must include fabric-permissions-api as a required dependency.'
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

function Test-MarketplaceDescriptionExists {
    $description = Get-Text 'docs/marketplace-description.md'

    Assert-Contains $description 'Carry Baby Animals' 'Marketplace description must name the mod.'
    Assert-Contains $description 'father-daughter project by Tyler and Jasmine' 'Marketplace description must include the father-daughter project note.'
    Assert-Contains $description 'server' 'Marketplace description must explain server setup.'
    Assert-Contains $description 'client' 'Marketplace description must explain optional client setup.'
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
Test-MarketplaceDescriptionExists
Test-PowerShellPublishingScriptsParse

Write-Host 'release publishing source tests passed'
