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
    Assert-Contains $workflow 'chmod +x ./gradlew' 'Release workflow must make gradlew executable on Linux runners.'
    Assert-Contains $workflow 'gh release view "${GITHUB_REF_NAME}"' 'Release workflow must handle reruns when the GitHub release already exists.'
    Assert-Contains $workflow 'gh release upload "${GITHUB_REF_NAME}" --clobber' 'Release workflow must replace release assets on reruns.'
    Assert-Contains $workflow '-ChangelogPath "release-notes.md"' 'Release workflow must pass public release-notes.md to marketplace scripts.'
    Assert-NotContains $workflow 'INTERNAL_CHANGELOG.md' 'Release workflow must not publish maintainer-only INTERNAL_CHANGELOG.md.'
}

function Test-ModrinthUploadEnforcesRequiredServerOptionalClient {
    $script = Get-Text 'scripts/upload-modrinth.ps1'
    $workflow = Get-Text '.github/workflows/release.yml'

    Assert-Contains $workflow '-Slug "carrybabyanimals"' 'Release workflow must publish to the real Modrinth slug.'
    Assert-Contains $script '[string] $Slug = "carrybabyanimals"' 'Modrinth upload default slug must match the real project slug.'
    Assert-Contains $script 'server_only_client_optional' 'Modrinth upload must document the new environment mapping.'
    Assert-Contains $script 'client_side = "optional"' 'Modrinth upload must set client_side optional.'
    Assert-Contains $script 'server_side = "required"' 'Modrinth upload must set server_side required.'
    Assert-Contains $script 'P7dR8mSH' 'Modrinth upload must include Fabric API as a required dependency.'
    Assert-Contains $script 'lzVo0Dll' 'Modrinth upload must include fabric-permissions-api as a required dependency.'
    Assert-Contains $script 'EnvironmentSynced' 'Modrinth upload must report environment sync status.'
}

function Test-CurseForgeUploadDocumentsSideLimitAndDependencies {
    $script = Get-Text 'scripts/upload-curseforge.ps1'

    Assert-Contains $script 'CurseForge upload API does not expose Modrinth-style client/server side fields' 'CurseForge upload script must document the side metadata limitation.'
    Assert-Contains $script 'slug = "fabric-api"' 'CurseForge upload must include Fabric API as a required relation.'
    Assert-Contains $script 'type = "requiredDependency"' 'CurseForge upload must mark supported dependencies required.'
}

function Test-GradleRunsReleasePublishingSourceGate {
    $build = Get-Text 'build.gradle'

    Assert-Contains $build 'tasks.register("checkReleasePublishingSources"' 'Gradle must register checkReleasePublishingSources.'
    Assert-Contains $build 'test-release-publishing-sources.ps1' 'Gradle release publishing gate must run the PowerShell test script.'
    Assert-Contains $build 'dependsOn tasks.named("checkReleasePublishingSources")' 'Gradle check must depend on checkReleasePublishingSources.'
}

function Test-PowerShellPublishingScriptsParse {
    [scriptblock]::Create((Get-Text 'scripts/upload-modrinth.ps1')) | Out-Null
    [scriptblock]::Create((Get-Text 'scripts/upload-curseforge.ps1')) | Out-Null
}

Test-ReleaseWorkflowPublishesPublicNotesOnly
Test-ModrinthUploadEnforcesRequiredServerOptionalClient
Test-CurseForgeUploadDocumentsSideLimitAndDependencies
Test-GradleRunsReleasePublishingSourceGate
Test-PowerShellPublishingScriptsParse

Write-Host 'release publishing source tests passed'
