# Definition-of-done gate: runs every quality task that exists in the build, exits with Gradle's code.
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')

$rootBuild = Get-Content -Raw -Encoding UTF8 'build.gradle.kts'
$tasks = @()
if ($rootBuild -match 'spotless') {
    $tasks += 'spotlessApply', 'spotlessCheck' # apply first so the check only fails on what apply cannot fix
}
if ($rootBuild -match 'detekt') {
    $tasks += 'detekt'
}
$tasks += 'lint', 'testDebugUnitTest', 'assembleDebug'

Write-Host "verify.ps1 tasks: $($tasks -join ' ')"
& .\gradlew.bat @tasks
exit $LASTEXITCODE
