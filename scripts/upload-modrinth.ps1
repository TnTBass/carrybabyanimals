param(
    [Parameter(Mandatory = $false)]
    [string] $Slug = "carrybabyanimals",

    [Parameter(Mandatory = $false)]
    [string] $Version = "",

    [Parameter(Mandatory = $false)]
    [ValidateSet("fabric", "neoforge")]
    [string] $Loader = "fabric",

    [Parameter(Mandatory = $false)]
    [string] $JarPath,

    [Parameter(Mandatory = $false)]
    [string] $ChangelogPath = "release-notes.md",

    [Parameter(Mandatory = $false)]
    [string] $DescriptionPath = "docs/marketplace-description.md",

    [switch] $SyncDescriptionOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$RepoRoot = Split-Path -Parent $PSScriptRoot
$UploadDir = Join-Path $RepoRoot "build/modrinth-upload"
$VersionDataPath = Join-Path $UploadDir "modrinth-$Loader-version-data.json"
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

function Read-PublicDescription {
    param([string] $Path)

    $resolvedPath = Resolve-RepoPath $Path
    if (-not (Test-Path -LiteralPath $resolvedPath)) {
        throw "Description path does not exist: $Path"
    }
    $content = (Get-Content -Raw -LiteralPath $resolvedPath).Trim()
    if ([string]::IsNullOrWhiteSpace($content)) {
        throw "Description path is empty: $Path"
    }
    return $content
}

function Invoke-ModrinthApi {
    param(
        [string] $Method,
        [string] $Uri,
        [hashtable] $Headers,
        [object] $Body = $null
    )

    if ($null -eq $Body) {
        return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $Headers
    }

    return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $Headers -ContentType "application/json" -Body ($Body | ConvertTo-Json -Depth 20)
}

$token = Get-RequiredEnv "MODRINTH_TOKEN"
$minecraftVersion = Get-GradleProperty "minecraft_version"
New-Item -ItemType Directory -Force -Path $UploadDir | Out-Null

if ([string]::IsNullOrWhiteSpace($Version) -and -not $SyncDescriptionOnly) {
    throw "Version is required unless -SyncDescriptionOnly is used."
}

$headers = @{
    Authorization = $token
    "User-Agent" = "TnTBass/carrybabyanimals"
}

$project = Invoke-ModrinthApi -Method "GET" -Uri "https://api.modrinth.com/v2/project/$Slug" -Headers $headers
$projectId = $project.id

# Modrinth's new environment label for this project is server_only_client_optional.
# API v2 still exposes this as client_side=optional and server_side=required.
$projectPatch = @{
    client_side = "optional"
    server_side = "required"
}
$projectPatch.body = Read-PublicDescription $DescriptionPath
Invoke-ModrinthApi -Method "PATCH" -Uri "https://api.modrinth.com/v2/project/$projectId" -Headers $headers -Body $projectPatch | Out-Null
$EnvironmentSynced = $true
$DescriptionSynced = $true

if ($SyncDescriptionOnly) {
    Write-Host "Synced Modrinth $Slug project metadata. EnvironmentSynced=$EnvironmentSynced DescriptionSynced=$DescriptionSynced"
    return
}

$artifactVersion = "$Version+mc$minecraftVersion"
if ([string]::IsNullOrWhiteSpace($JarPath)) {
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

$modrinthVersionNumber = $artifactVersion
$existingVersion = Invoke-ModrinthApi -Method "GET" -Uri "https://api.modrinth.com/v2/project/$projectId/version" -Headers $headers |
    Where-Object { $_.version_number -eq $modrinthVersionNumber -and $_.loaders -contains $Loader } |
    Select-Object -First 1
if ($null -ne $existingVersion) {
    Write-Host "Modrinth $Slug $modrinthVersionNumber already exists. EnvironmentSynced=$EnvironmentSynced DescriptionSynced=$DescriptionSynced"
    return
}

$dependencies = @()
if ($Loader -eq "fabric") {
    $dependencies += @{
        project_id = "P7dR8mSH"
        version_id = $null
        file_name = $null
        dependency_type = "required"
    }
}
$versionData = @{
    name = "Carry Baby Animals $Version for Minecraft $minecraftVersion"
    version_number = $modrinthVersionNumber
    changelog = Read-PublicChangelog $ChangelogPath
    dependencies = $dependencies
    game_versions = @($minecraftVersion)
    version_type = "release"
    loaders = @($Loader)
    featured = $true
    status = "listed"
    requested_status = "listed"
    project_id = $projectId
    file_parts = @("mod")
    primary_file = "mod"
} | ConvertTo-Json -Depth 20
$versionData | Set-Content -LiteralPath $VersionDataPath -Encoding UTF8

$versionResponse = & $Curl -sS `
    -X POST "https://api.modrinth.com/v2/version" `
    -H "Authorization: $token" `
    -H "User-Agent: TnTBass/carrybabyanimals" `
    -H "Accept: application/json" `
    -F "data=<$VersionDataPath;type=application/json" `
    -F "mod=@$jarFullPath"

if ($LASTEXITCODE -ne 0) {
    throw "curl failed while creating Modrinth version."
}

$modrinthVersion = $versionResponse | ConvertFrom-Json
if ($modrinthVersion.PSObject.Properties.Name -contains "error") {
    throw "Modrinth version creation failed: $($modrinthVersion.error): $($modrinthVersion.description)"
}

Write-Host "Published Modrinth $Slug $modrinthVersionNumber. EnvironmentSynced=$EnvironmentSynced DescriptionSynced=$DescriptionSynced"
