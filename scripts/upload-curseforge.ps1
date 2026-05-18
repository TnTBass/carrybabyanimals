param(
    [Parameter(Mandatory = $false)]
    [string] $Slug = "carry-baby-animals",

    [Parameter(Mandatory = $true)]
    [string] $Version,

    [Parameter(Mandatory = $false)]
    [string] $JarPath,

    [Parameter(Mandatory = $false)]
    [string] $ChangelogPath = "release-notes.md"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$RepoRoot = Split-Path -Parent $PSScriptRoot

function Get-RequiredEnv {
    param([string] $Name)

    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "$Name is required."
    }
    return $value
}

function Get-GradleProperty {
    param([string] $Name)

    $propertiesPath = Join-Path $RepoRoot "gradle.properties"
    $match = Get-Content -LiteralPath $propertiesPath | Where-Object { $_ -match "^$([regex]::Escape($Name))=(.+)$" } | Select-Object -First 1
    if (-not $match) {
        throw "gradle.properties is missing $Name."
    }
    return ($match -replace "^$([regex]::Escape($Name))=", "").Trim()
}

function Resolve-RepoPath {
    param([string] $Path)

    if ([System.IO.Path]::IsPathRooted($Path)) {
        return $Path
    }
    return Join-Path $RepoRoot $Path
}

function Assert-PublicChangelogPath {
    param([string] $Path)

    $fileName = [System.IO.Path]::GetFileName($Path)
    if ($fileName -eq "INTERNAL_CHANGELOG.md") {
        throw "INTERNAL_CHANGELOG.md is maintainer-only and must not be published."
    }
}

function Read-PublicChangelog {
    param([string] $Path)

    Assert-PublicChangelogPath $Path
    $resolvedPath = Resolve-RepoPath $Path
    if (-not (Test-Path -LiteralPath $resolvedPath)) {
        throw "Changelog path does not exist: $Path"
    }
    return (Get-Content -Raw -LiteralPath $resolvedPath).Trim()
}

$token = Get-RequiredEnv "CURSEFORGE_TOKEN"
$projectId = Get-RequiredEnv "CURSEFORGE_PROJECT_ID"
$minecraftVersion = Get-GradleProperty "minecraft_version"

if ([string]::IsNullOrWhiteSpace($JarPath)) {
    $JarPath = "build/libs/carry-baby-animals-$Version.jar"
}

$jarFullPath = Resolve-RepoPath $JarPath
if (-not (Test-Path -LiteralPath $jarFullPath)) {
    throw "Jar not found: $JarPath"
}

# CurseForge upload API does not expose Modrinth-style client/server side fields.
# Keep the CurseForge project listing set to server required / client optional in the site UI.
$metadata = @{
    changelog = Read-PublicChangelog $ChangelogPath
    changelogType = "markdown"
    displayName = "Carry Baby Animals $Version for Minecraft $minecraftVersion"
    gameVersions = @($minecraftVersion, "Fabric")
    releaseType = "release"
    relations = @{
        projects = @(
            @{
                slug = "fabric-api"
                type = "requiredDependency"
            }
        )
    }
}

$headers = @{
    "X-Api-Token" = $token
    "User-Agent" = "TnTBass/carrybabyanimals"
}

$form = @{
    metadata = ($metadata | ConvertTo-Json -Depth 20 -Compress)
    file = Get-Item -LiteralPath $jarFullPath
}

Invoke-RestMethod -Method "POST" -Uri "https://minecraft.curseforge.com/api/projects/$projectId/upload-file" -Headers $headers -Form $form | Out-Null
Write-Host "Published CurseForge $Slug $Version. Verify project listing side metadata remains server required / client optional."
