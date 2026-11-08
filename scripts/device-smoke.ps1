# Device smoke test: installs the debug build, enables and starts the dream, taps, screenshots, exits.
# Usage: .\scripts\device-smoke.ps1 [-WhatIf]
# -WhatIf prints the command list and exits 0 without running Gradle or adb.
param(
    [switch]$WhatIf
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location $repoRoot

$dreamComponent = 'dev.lewisscoging.nightstand/.dream.NightstandDreamService'
$screenCentreX = 540   # half of the 1080 px portrait width from the facts table
$screenCentreY = 1170  # half of the 2340 px portrait height from the facts table
$dreamStartWaitSeconds = 2
$tapWaitSeconds = 1
$deviceScreenshot = '/sdcard/ns.png'
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$runDir = Join-Path $repoRoot '.plan-runs'
$localScreenshot = Join-Path $runDir "smoke-$timestamp.png"

$startDreamPrimary = @('shell', 'cmd', 'dreams', 'start')
$startDreamFallback = @('shell', 'am', 'start', '-n', 'com.android.systemui/.Somnambulator')

# Each step is a command (gradle, adb, or a wait). The 'start-dream' step tries the primary then the fallback.
$steps = @(
    @{ Kind = 'gradle'; Args = @('installDebug') },
    @{ Kind = 'adb'; Args = @('shell', 'settings', 'put', 'secure', 'screensaver_components', $dreamComponent) },
    @{ Kind = 'adb'; Args = @('shell', 'settings', 'put', 'secure', 'screensaver_enabled', '1') },
    @{ Kind = 'adb'; Args = @('shell', 'settings', 'put', 'secure', 'screensaver_activate_on_sleep', '1') },
    @{ Kind = 'start-dream' },
    @{ Kind = 'wait'; Seconds = $dreamStartWaitSeconds },
    @{ Kind = 'adb'; Args = @('shell', 'input', 'tap', "$screenCentreX", "$screenCentreY") },
    @{ Kind = 'wait'; Seconds = $tapWaitSeconds },
    @{ Kind = 'adb'; Args = @('shell', 'screencap', '-p', $deviceScreenshot) },
    @{ Kind = 'adb'; Args = @('pull', $deviceScreenshot, $localScreenshot) },
    @{ Kind = 'adb'; Args = @('shell', 'rm', $deviceScreenshot) },
    @{ Kind = 'adb'; Args = @('shell', 'input', 'keyevent', 'KEYCODE_POWER') }
)

function Format-Command([string]$exe, [string[]]$arguments) {
    return "$exe $($arguments -join ' ')"
}

function Invoke-Adb([string[]]$arguments) {
    # adb writes progress to stderr; keep it from becoming a terminating error and judge by the exit code.
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $output = & adb @arguments 2>&1 | ForEach-Object { "$_" }
    $code = $LASTEXITCODE
    $ErrorActionPreference = $previous
    return @{ Code = $code; Output = ($output -join "`n") }
}

function Test-AdbSucceeded($result) {
    # Some shells report a missing command on stdout with exit code 0, so check the text as well.
    return ($result.Code -eq 0) -and ($result.Output -notmatch 'Unknown command|not found|Error|Exception')
}

if ($WhatIf) {
    Write-Host 'device-smoke.ps1 -WhatIf: commands that would run'
    $number = 1
    foreach ($step in $steps) {
        switch ($step.Kind) {
            'gradle' { $line = Format-Command '.\gradlew.bat' $step.Args }
            'adb' { $line = Format-Command 'adb' $step.Args }
            'wait' { $line = "wait $($step.Seconds) s" }
            'start-dream' {
                $line = (Format-Command 'adb' $startDreamPrimary) + '   (fallback: ' + (Format-Command 'adb' $startDreamFallback) + ')'
            }
        }
        Write-Host ("{0,2}. {1}" -f $number, $line)
        $number++
    }
    exit 0
}

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    Write-Error 'adb is not on PATH (expected in %ANDROID_HOME%\platform-tools).'
    exit 1
}
if (-not (Test-Path $runDir)) {
    New-Item -ItemType Directory -Path $runDir | Out-Null
}

foreach ($step in $steps) {
    switch ($step.Kind) {
        'gradle' {
            Write-Host "> $(Format-Command '.\gradlew.bat' $step.Args)"
            & .\gradlew.bat @($step.Args)
            if ($LASTEXITCODE -ne 0) {
                Write-Error "Gradle failed with exit code $LASTEXITCODE."
                exit 1
            }
        }
        'adb' {
            Write-Host "> $(Format-Command 'adb' $step.Args)"
            $result = Invoke-Adb $step.Args
            if ($result.Output) { Write-Host $result.Output }
            if ($result.Code -ne 0) {
                Write-Error "adb failed with exit code $($result.Code)."
                exit 1
            }
        }
        'wait' {
            Write-Host "> wait $($step.Seconds) s"
            Start-Sleep -Seconds $step.Seconds
        }
        'start-dream' {
            Write-Host "> $(Format-Command 'adb' $startDreamPrimary)"
            $result = Invoke-Adb $startDreamPrimary
            if ($result.Output) { Write-Host $result.Output }
            if (-not (Test-AdbSucceeded $result)) {
                Write-Host "Primary start command failed; trying the fallback."
                Write-Host "> $(Format-Command 'adb' $startDreamFallback)"
                $result = Invoke-Adb $startDreamFallback
                if ($result.Output) { Write-Host $result.Output }
                if (-not (Test-AdbSucceeded $result)) {
                    Write-Error 'Neither dream start command worked. Record a working one in the plan facts table (Step 8.1).'
                    exit 1
                }
            }
        }
    }
}

Write-Host "Screenshot saved to $localScreenshot"
exit 0
