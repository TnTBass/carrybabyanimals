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
$UploadDir = Join-Path $RepoRoot "build/curseforge-upload"
$MetadataPath = Join-Path $UploadDir "curseforge-metadata.json"
$Curl = if ($IsWindows) { "curl.exe" } else { "curl" }

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

function Get-CurseForgeGameVersionId {
    param(
        [object[]] $GameVersions,
        [string] $Name
    )

    $match = $GameVersions | Where-Object { $_.name -ieq $Name } | Select-Object -First 1
    if ($null -eq $match) {
        $match = $GameVersions | Where-Object { $_.name -ilike "*$Name*" } | Select-Object -First 1
    }
    if ($null -eq $match) {
        $available = ($GameVersions | Select-Object -First 20 | ForEach-Object { $_.name }) -join ", "
        throw "CurseForge game version '$Name' was not found. Available sample: $available"
    }

    return $match.id
}

$token = Get-RequiredEnv "CURSEFORGE_TOKEN"
$projectId = Get-RequiredEnv "CURSEFORGE_PROJECT_ID"
$minecraftVersion = Get-GradleProperty "minecraft_version"
New-Item -ItemType Directory -Force -Path $UploadDir | Out-Null

if ([string]::IsNullOrWhiteSpace($JarPath)) {
    $JarPath = "build/libs/carry-baby-animals-$Version.jar"
}

$jarFullPath = Resolve-RepoPath $JarPath
if (-not (Test-Path -LiteralPath $jarFullPath)) {
    throw "Jar not found: $JarPath"
}

$headers = @{
    "X-Api-Token" = $token
    "Accept" = "application/json"
}
$gameVersions = Invoke-RestMethod -Uri "https://minecraft.curseforge.com/api/game/versions" -Headers $headers

# CurseForge upload API does not expose Modrinth-style client/server side fields.
# Keep the CurseForge project listing set to server required / client optional in the site UI.
$metadata = @{
    changelog = Read-PublicChangelog $ChangelogPath
    changelogType = "markdown"
    displayName = "Carry Baby Animals $Version for Minecraft $minecraftVersion"
    gameVersions = @(
        Get-CurseForgeGameVersionId -GameVersions $gameVersions -Name $minecraftVersion
        Get-CurseForgeGameVersionId -GameVersions $gameVersions -Name "Fabric"
    )
    releaseType = "release"
    relations = @{
        projects = @(
            @{
                slug = "fabric-api"
                type = "requiredDependency"
            }
        )
    }
} | ConvertTo-Json -Depth 20
$metadata | Set-Content -LiteralPath $MetadataPath -Encoding UTF8

$uploadResponse = & $Curl -sS `
    -X POST "https://minecraft.curseforge.com/api/projects/$projectId/upload-file" `
    -H "X-Api-Token: $token" `
    -H "Accept: application/json" `
    -F "metadata=<$MetadataPath;type=application/json" `
    -F "file=@$jarFullPath"

if ($LASTEXITCODE -ne 0) {
    throw "curl failed while uploading CurseForge file."
}

$curseForgeFile = $uploadResponse | ConvertFrom-Json
if (
    ($curseForgeFile.PSObject.Properties.Name -contains "error") -or
    ($curseForgeFile.PSObject.Properties.Name -contains "errors")
) {
    throw "CurseForge file upload failed: $uploadResponse"
}

Write-Host "Published CurseForge $Slug $Version. Verify project listing side metadata remains server required / client optional."
