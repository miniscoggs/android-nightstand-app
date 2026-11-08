# Stop hook: a runner session (PLAN_STEP set) may only end once verify.ps1 passes or its step is Blocked.
$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
. (Join-Path $repoRoot 'scripts\plan-lib.ps1')

$planStep = $env:PLAN_STEP
if ([string]::IsNullOrWhiteSpace($planStep)) {
    exit 0
}

$hookInput = [Console]::In.ReadToEnd()
$stopHookActive = $false
if (-not [string]::IsNullOrWhiteSpace($hookInput)) {
    try {
        $stopHookActive = [bool]($hookInput | ConvertFrom-Json).stop_hook_active
    } catch {
        $stopHookActive = $false # malformed stdin only affects logging
    }
}

$planContent = Read-PlanFile -Path (Join-Path $repoRoot 'NIGHTSTAND-PLAN.md')
if (Test-StopAllowed -PlanContent $planContent -PlanStep $planStep -VerifyResult 1) { # 1 so only Blocked can pass here
    exit 0
}

$runsDir = Join-Path $repoRoot '.plan-runs'
New-Item -ItemType Directory -Force -Path $runsDir | Out-Null
$safeStep = $planStep -replace '[^\w.-]', '_'
$logPath = Join-Path $runsDir ("{0}-{1}-verify.log" -f (Get-Date -Format 'yyyyMMdd-HHmmss'), $safeStep)
"PLAN_STEP=$planStep stop_hook_active=$stopHookActive" | Out-File -Encoding UTF8 -FilePath $logPath

$verifyScript = Join-Path $repoRoot 'scripts\verify.ps1'
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $verifyScript *>> $logPath
$verifyExit = $LASTEXITCODE

if (Test-StopAllowed -PlanContent $planContent -PlanStep $planStep -VerifyResult $verifyExit) {
    exit 0
}

$tail = (Get-Content -Encoding UTF8 -LiteralPath $logPath -Tail 40) -join "`n"
$decision = @{
    decision = 'block'
    reason = "verify.ps1 failed (exit $verifyExit). Fix the build, then stop again. Last 40 lines:`n$tail"
}
[Console]::Out.Write(($decision | ConvertTo-Json -Compress))
exit 0
