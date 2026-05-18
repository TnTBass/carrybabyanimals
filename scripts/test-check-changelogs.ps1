Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$RepoRoot = Split-Path -Parent $PSScriptRoot
$ScriptUnderTest = Join-Path $PSScriptRoot 'check-changelogs.ps1'
$SandboxRoot = Join-Path $RepoRoot 'build/changelog-policy-tests'

function Assert-True {
    param(
        [bool] $Condition,
        [string] $Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function New-CaseRepo {
    param([string] $Name)

    $caseRoot = Join-Path $SandboxRoot $Name
    if (Test-Path $caseRoot) {
        Remove-Item -Recurse -Force $caseRoot
    }
    New-Item -ItemType Directory -Force $caseRoot | Out-Null
    Push-Location $caseRoot
    try {
        git init -b main | Out-Null
        git config user.email 'test@example.invalid'
        git config user.name 'Changelog Policy Test'
        @'
# CarryBabyAnimals Changelog

## Unreleased

- Added baseline.
'@ | Set-Content -Encoding UTF8 CHANGELOG.md
        @'
# Internal Changelog

This changelog is for maintainer-only repo, build, workflow, and release-process notes. It is not published to GitHub Releases, Modrinth, CurseForge, or other public marketplace pages.

## Unreleased

- Added baseline.
'@ | Set-Content -Encoding UTF8 INTERNAL_CHANGELOG.md
        git add CHANGELOG.md INTERNAL_CHANGELOG.md | Out-Null
        git commit -m 'baseline changelogs' | Out-Null
    } finally {
        Pop-Location
    }
    return $caseRoot
}

function Invoke-Policy {
    param(
        [string] $CaseRoot,
        [string[]] $Arguments = @()
    )

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $output = & pwsh -NoProfile -ExecutionPolicy Bypass -File $ScriptUnderTest -RepoRoot $CaseRoot @Arguments 2>&1
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    return @{
        ExitCode = $LASTEXITCODE
        Output = ($output -join "`n")
    }
}

function Test-PublicChangelogSatisfiesSourceChange {
    $caseRoot = New-CaseRepo 'public-source-change'
    New-Item -ItemType Directory -Force (Join-Path $caseRoot 'src/main/java/example') | Out-Null
    'class Example {}' | Set-Content -Encoding UTF8 (Join-Path $caseRoot 'src/main/java/example/Example.java')
    @'
# CarryBabyAnimals Changelog

## Unreleased

- Added visible baby carrying behavior for players.
'@ | Set-Content -Encoding UTF8 (Join-Path $caseRoot 'CHANGELOG.md')

    $result = Invoke-Policy $caseRoot
    Assert-True ($result.ExitCode -eq 0) "Public changelog source-change case failed: $($result.Output)"
}

function Test-InternalChangelogSatisfiesBuildChange {
    $caseRoot = New-CaseRepo 'internal-build-change'
    'plugins { id "base" }' | Set-Content -Encoding UTF8 (Join-Path $caseRoot 'build.gradle')
    @'
# Internal Changelog

This changelog is for maintainer-only repo, build, workflow, and release-process notes. It is not published to GitHub Releases, Modrinth, CurseForge, or other public marketplace pages.

## Unreleased

- Added a build gate for changelog policy.
'@ | Set-Content -Encoding UTF8 (Join-Path $caseRoot 'INTERNAL_CHANGELOG.md')

    $result = Invoke-Policy $caseRoot
    Assert-True ($result.ExitCode -eq 0) "Internal changelog build-change case failed: $($result.Output)"
}

function Test-MissingChangelogFailsRelevantChange {
    $caseRoot = New-CaseRepo 'missing-changelog'
    New-Item -ItemType Directory -Force (Join-Path $caseRoot 'src/main/java/example') | Out-Null
    'class Example {}' | Set-Content -Encoding UTF8 (Join-Path $caseRoot 'src/main/java/example/Example.java')

    $result = Invoke-Policy $caseRoot
    Assert-True ($result.ExitCode -ne 0) 'Missing changelog case unexpectedly passed.'
    Assert-True ($result.Output.Contains('CHANGELOG.md or INTERNAL_CHANGELOG.md')) "Missing changelog case had wrong output: $($result.Output)"
}

function Test-ReleasePrepReadsOnlyPublicChangelog {
    $caseRoot = New-CaseRepo 'release-prep-public-only'
    @'
# CarryBabyAnimals Changelog

## Unreleased

## 0.1.0

- Public player note.
'@ | Set-Content -Encoding UTF8 (Join-Path $caseRoot 'CHANGELOG.md')
    @'
# Internal Changelog

This changelog is for maintainer-only repo, build, workflow, and release-process notes. It is not published to GitHub Releases, Modrinth, CurseForge, or other public marketplace pages.

## Unreleased

## 0.1.0

- Secret build note.
'@ | Set-Content -Encoding UTF8 (Join-Path $caseRoot 'INTERNAL_CHANGELOG.md')

    $result = Invoke-Policy $caseRoot -Arguments @('-ReleasePrep', '-Version', '0.1.0')
    Assert-True ($result.ExitCode -eq 0) "Release prep case failed: $($result.Output)"
    Assert-True ($result.Output.Contains('Public player note')) "Release prep did not print public note: $($result.Output)"
    Assert-True (-not $result.Output.Contains('Secret build note')) "Release prep leaked internal note: $($result.Output)"
}

Test-PublicChangelogSatisfiesSourceChange
Test-InternalChangelogSatisfiesBuildChange
Test-MissingChangelogFailsRelevantChange
Test-ReleasePrepReadsOnlyPublicChangelog

Write-Host 'changelog policy tests passed'
