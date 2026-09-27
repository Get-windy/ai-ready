<#
.SYNOPSIS
  mvn clean with automatic unlock (Windows only).

.DESCRIPTION
  Before running "mvn clean", detects java processes launched from this
  project's **/target directories (typically dev services started with
  "java -jar .../target/xxx-exec.jar") that lock jars and cause
  "Failed to delete ...target\xxx.jar", then terminates them precisely.

  Safety rules:
    - IDE language servers (jdt.ls / equinox.launcher) are NEVER killed.
    - A running Maven build (classworlds.launcher.Launcher) is NEVER killed;
      the script aborts and asks you to wait for it.
    - Only java processes whose command line references THIS backend root
      (absolute or "backend/.../target/..." relative form) are killed.

.PARAMETER DryRun
  Only detect and report; do not kill and do not run mvn clean.

.PARAMETER MvnArgs
  Extra arguments passed through to mvn after "clean", e.g. -pl core/api/core-api

.EXAMPLE
  .\scripts\mvn-clean-unlock.ps1
  .\scripts\mvn-clean-unlock.ps1 -DryRun
  .\scripts\mvn-clean-unlock.ps1 -pl core/api/core-api -am
#>
[CmdletBinding()]
param(
    [switch]$DryRun,

    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MvnArgs
)

$ErrorActionPreference = 'Stop'

# This script lives in <backend-root>/scripts
$root = Split-Path -Parent $PSScriptRoot
$rootAlt = ($root -replace '\\', '/').TrimEnd('/')

function Get-JavaProcs {
    Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue
}

function Test-RefsProjectTarget {
    param([string]$CmdLine)
    if (-not $CmdLine) { return $false }
    if ($CmdLine -notmatch 'target[\\/]') { return $false }
    # Absolute path form (both slash directions), e.g. I:/AI-Ready/backend/.../target/
    if ($CmdLine -like "*$rootAlt*") { return $true }
    # Relative form used from the workspace root, e.g. -jar backend/core/.../target/x.jar
    if ($CmdLine -match '(?:^|\s)(?:-jar|-cp)\s+backend[\\/][^\s]*[\\/]target[\\/]') { return $true }
    return $false
}

Write-Host "=== mvn-clean-unlock ===" -ForegroundColor Cyan
Write-Host "Backend root: $root"
Write-Host ""

# 1) Classify java processes
$builds = @()
$lockers = @()

foreach ($proc in (Get-JavaProcs)) {
    $cmd = $proc.CommandLine
    if (-not $cmd) { continue }

    # Never touch IDE language servers
    if ($cmd -match 'jdt\.ls|equinox\.launcher|redhat\.java') { continue }

    # Never kill an in-progress Maven build of this project
    if ($cmd -match 'classworlds\.launcher\.Launcher') {
        if ($cmd -like "*$rootAlt*") { $builds += $proc }
        continue
    }

    if (Test-RefsProjectTarget -CmdLine $cmd) {
        $lockers += $proc
    }
}

# 2) A Maven build is running -> abort without touching anything
if ($builds.Count -gt 0) {
    Write-Host "[ABORT] A Maven build is in progress. Wait for it to finish before clean:" -ForegroundColor Yellow
    $builds | Select-Object ProcessId, CreationDate, CommandLine | Format-List
    exit 2
}

# 3) Report / kill target-locking processes
if ($lockers.Count -eq 0) {
    Write-Host "[OK] No java process is locking this project's target directories." -ForegroundColor Green
}
else {
    Write-Host "[FOUND] $($lockers.Count) java process(es) locking target jars:" -ForegroundColor Yellow
    foreach ($proc in $lockers) {
        $preview = $proc.CommandLine
        if ($preview.Length -gt 200) { $preview = $preview.Substring(0, 200) + '...' }
        Write-Host ("  PID {0}  started {1}" -f $proc.ProcessId, $proc.CreationDate)
        Write-Host "    $preview"
    }

    if ($DryRun) {
        Write-Host ""
        Write-Host "[DryRun] No process killed, mvn clean not executed." -ForegroundColor Yellow
        exit 0
    }

    Write-Host ""
    Write-Host "[UNLOCK] Terminating process(es): $($lockers.ProcessId -join ', ')" -ForegroundColor Yellow
    Stop-Process -Id ($lockers.ProcessId) -Force -ErrorAction SilentlyContinue
    Start-Sleep -Milliseconds 800

    # Re-verify
    $remaining = @()
    foreach ($proc in (Get-JavaProcs)) {
        if ($proc.CommandLine -and (Test-RefsProjectTarget -CmdLine $proc.CommandLine)) {
            $remaining += $proc
        }
    }
    if ($remaining.Count -gt 0) {
        Write-Host "[WARN] Process(es) still holding target: $($remaining.ProcessId -join ', ')" -ForegroundColor Red
        exit 1
    }
    Write-Host "[OK] Locks released." -ForegroundColor Green
}

if ($DryRun) {
    Write-Host "[DryRun] mvn clean not executed." -ForegroundColor Yellow
    exit 0
}

# 4) Run mvn clean at the backend root, passing extra args through
Write-Host ""
Write-Host "[RUN] mvn clean $($MvnArgs -join ' ')" -ForegroundColor Cyan
Push-Location $root
try {
    & mvn clean @MvnArgs
    $code = $LASTEXITCODE
}
finally {
    Pop-Location
}

exit $code
