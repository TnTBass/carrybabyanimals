param(
    [Parameter(Mandatory = $false)]
    [string] $Slug = "carry-baby-animals",

    [Parameter(Mandatory = $true)]
    [string] $Version,

    [Parameter(Mandatory = $false)]
    [string] $JarPath,

    [Parameter(Mandatory = $false)]
    [ValidateSet("fabric", "neoforge")]
    [string] $Loader = "fabric",

    [Parameter(Mandatory = $false)]
    [string] $ChangelogPath = "release-notes.md",

    [Parameter(Mandatory = $false)]
    [string] $DescriptionPath = "docs/marketplace-description.md"
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

function Assert-PublicDescriptionExists {
    param([string] $Path)

    $resolvedPath = Resolve-RepoPath $Path
    if (-not (Test-Path -LiteralPath $resolvedPath)) {
        throw "Description path does not exist: $Path"
    }
    if ([string]::IsNullOrWhiteSpace((Get-Content -Raw -LiteralPath $resolvedPath))) {
        throw "Description path is empty: $Path"
    }
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
Assert-PublicDescriptionExists $DescriptionPath

if ([string]::IsNullOrWhiteSpace($JarPath)) {
    $artifactVersion = "$Version+mc$minecraftVersion"
    $JarPath = if ($Loader -eq "neoforge") {
        "neoforge/build/libs/carrybabyanimals-neoforge-$artifactVersion.jar"
    } else {
        "fabric/build/libs/carrybabyanimals-fabric-$artifactVersion.jar"
    }
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
# CurseForge upload API does not expose project-page description updates.
# Copy docs/marketplace-description.md into the CurseForge description field manually.
# Fabric permission-provider integration is covered by the required Fabric API dependency.
# CurseForge does not accept a separate legacy permission relation for this root category.
$loaderGameVersionId = if ($Loader -eq "neoforge") {
    Get-CurseForgeGameVersionId -GameVersions $gameVersions -Name "NeoForge"
} else {
    Get-CurseForgeGameVersionId -GameVersions $gameVersions -Name "Fabric"
}
$relations = @()
if ($Loader -eq "fabric") {
    $relations += @{
        slug = "fabric-api"
        type = "requiredDependency"
    }
}
$metadataPayload = @{
    changelog = Read-PublicChangelog $ChangelogPath
    changelogType = "markdown"
    displayName = "Carry Baby Animals $Version $Loader for Minecraft $minecraftVersion"
    gameVersions = @(
        Get-CurseForgeGameVersionId -GameVersions $gameVersions -Name $minecraftVersion
        $loaderGameVersionId
    )
    releaseType = "release"
}
if ($relations.Count -gt 0) {
    $metadataPayload.relations = @{
        projects = $relations
    }
}
$metadata = $metadataPayload | ConvertTo-Json -Depth 20
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
    ($curseForgeFile.PSObject.Properties.Name -contains "errors") -or
    ($curseForgeFile.PSObject.Properties.Name -contains "errorCode") -or
    ($curseForgeFile.PSObject.Properties.Name -contains "errorMessage")
) {
    throw "CurseForge file upload failed: $uploadResponse"
}

if (
    ($curseForgeFile.PSObject.Properties.Name -notcontains "id") -or
    [string]::IsNullOrWhiteSpace([string] $curseForgeFile.id)
) {
    throw "CurseForge file upload did not return a file ID: $uploadResponse"
}

$curseForgeFileId = [string] $curseForgeFile.id
if (-not [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable("GITHUB_OUTPUT"))) {
    Add-Content -LiteralPath $env:GITHUB_OUTPUT -Value "curseforge_file_id=$curseForgeFileId"
}

Write-Host "Published CurseForge $Slug $Version $Loader. CurseForgeFileId=$curseForgeFileId. Verify project listing side metadata remains server required / client optional. Copy $DescriptionPath into the CurseForge project description manually."
