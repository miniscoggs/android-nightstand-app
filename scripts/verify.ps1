# Definition-of-done gate: runs every quality task, exits with Gradle's code.
# -Check skips spotlessApply so formatting drift fails the run instead of being rewritten (for CI).
param([switch]$Check)

$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')

$tasks = @()
if (-not $Check) {
    $tasks += 'spotlessApply' # apply first so the check only fails on what apply cannot fix
}
$tasks += 'spotlessCheck', 'detekt', 'lint', 'testDebugUnitTest', 'assembleDebug'

Write-Host "verify.ps1 tasks: $($tasks -join ' ')"
& .\gradlew.bat @tasks
exit $LASTEXITCODE
