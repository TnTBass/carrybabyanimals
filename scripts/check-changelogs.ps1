param(
    [string] $RepoRoot = (Split-Path -Parent $PSScriptRoot),
    [switch] $ReleasePrep,
    [string] $Version
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-RepoPath {
    param([string] $Path)

    return $Path.Replace('\', '/').Trim()
}

function Get-GitChangedFiles {
    param([string] $Root)

    $status = git -C $Root status --porcelain
    $files = New-Object System.Collections.Generic.List[string]
    foreach ($line in $status) {
        if ([string]::IsNullOrWhiteSpace($line) -or $line.Length -lt 4) {
            continue
        }
        $path = $line.Substring(3).Trim()
        if ($path.Contains(' -> ')) {
            $parts = $path.Split(' -> ', [System.StringSplitOptions]::None)
            $path = $parts[$parts.Length - 1]
        }
        $files.Add((Get-RepoPath $path))
    }
    return $files
}

function Test-HasUnreleasedSection {
    param(
        [string] $Root,
        [string] $RelativePath
    )

    $path = Join-Path $Root $RelativePath
    if (-not (Test-Path $path)) {
        Write-Error "$RelativePath is missing."
        return $false
    }
    $content = Get-Content -Raw $path
    if ($content -notmatch '(?m)^##\s+Unreleased\s*$') {
        Write-Error "$RelativePath must contain a '## Unreleased' section."
        return $false
    }
    return $true
}

function Test-ReleasablePath {
    param([string] $Path)

    if ($Path -in @('CHANGELOG.md', 'INTERNAL_CHANGELOG.md')) {
        return $false
    }
    if ($Path -match '^docs/superpowers/') {
        return $false
    }
    return $Path -match '^(src/|gradle/|scripts/|\.github/workflows/|build\.gradle$|settings\.gradle$|gradle\.properties$|fabric\.mod\.json$|AGENTS\.md$|CLAUDE\.md$)'
}

function Get-PublicReleaseSection {
    param(
        [string] $Root,
        [string] $ReleaseVersion
    )

    $path = Join-Path $Root 'CHANGELOG.md'
    if (-not (Test-Path $path)) {
        throw 'CHANGELOG.md is missing. Public release notes must come from CHANGELOG.md.'
    }
    $content = Get-Content -Raw $path
    $escaped = [regex]::Escape($ReleaseVersion)
    $pattern = "(?ms)^##\s+(?:\[$escaped\]|$escaped)\s*\r?\n(?<body>.*?)(?=^##\s+|\z)"
    $match = [regex]::Match($content, $pattern)
    if (-not $match.Success) {
        throw "CHANGELOG.md does not contain a public version section for '$ReleaseVersion'."
    }
    $body = $match.Groups['body'].Value.Trim()
    if ([string]::IsNullOrWhiteSpace($body)) {
        throw "CHANGELOG.md section '$ReleaseVersion' is empty."
    }
    return "## $ReleaseVersion`n`n$body"
}

if (-not (Test-Path $RepoRoot)) {
    throw "Repository root does not exist: $RepoRoot"
}

if (-not (Test-HasUnreleasedSection $RepoRoot 'CHANGELOG.md')) {
    exit 1
}
if (-not (Test-HasUnreleasedSection $RepoRoot 'INTERNAL_CHANGELOG.md')) {
    exit 1
}

if ($ReleasePrep) {
    if ([string]::IsNullOrWhiteSpace($Version)) {
        Write-Error 'Release prep requires -Version.'
        exit 1
    }
    $section = Get-PublicReleaseSection $RepoRoot $Version
    Write-Output 'Public release notes from CHANGELOG.md:'
    Write-Output ''
    Write-Output $section
    Write-Output ''
    Write-Output 'Show this exact public section before pushing/tagging and wait for user approval.'
    exit 0
}

$changedFiles = @(Get-GitChangedFiles $RepoRoot)
$releasableChanges = @($changedFiles | Where-Object { Test-ReleasablePath $_ })
$changelogChanged = @($changedFiles | Where-Object { $_ -in @('CHANGELOG.md', 'INTERNAL_CHANGELOG.md') })

if ($releasableChanges.Count -gt 0 -and $changelogChanged.Count -eq 0) {
    Write-Error "Releasable changes require CHANGELOG.md or INTERNAL_CHANGELOG.md to change. Relevant files: $($releasableChanges -join ', ')"
    exit 1
}

Write-Output 'Changelog gate passed.'
