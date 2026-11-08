# Runs NIGHTSTAND-PLAN.md unattended: one fresh claude -p session per step or phase review.
param(
    [string]$Plan = 'NIGHTSTAND-PLAN.md',
    [switch]$DryRun,
    [switch]$Once,
    [string]$Until
)

. (Join-Path $PSScriptRoot 'plan-lib.ps1')

$script:UsageLimitPattern = 'usage limit|rate limit|hit your limit|try again at|resets'
$script:DefaultLimitWaitSeconds = 30 * 60
$script:MaxAttemptsPerAction = 2

function New-PlanAction {
    param([string]$Kind, [string]$Id, [int]$Phase, [string]$Message)
    [pscustomobject]@{ Kind = $Kind; Id = $Id; Phase = $Phase; Message = $Message }
}

function Get-NextAction {
    param(
        [Parameter(Mandatory)][AllowEmptyCollection()][object[]]$Steps,
        [string]$Until
    )

    $unreviewedPhase = $Steps | Group-Object Phase |
        Where-Object { -not $_.Group[0].PhaseReviewed -and -not ($_.Group | Where-Object { $_.Status -ne 'Done' }) } |
        Sort-Object { [int]$_.Name } | Select-Object -First 1
    if ($null -ne $unreviewedPhase) {
        $phaseNumber = [int]$unreviewedPhase.Name
        return New-PlanAction -Kind 'review' -Id "review:$phaseNumber" -Phase $phaseNumber -Message "Review Phase $phaseNumber"
    }

    $candidate = $Steps | Where-Object { $_.Status -ne 'Done' } | Select-Object -First 1
    if ($null -eq $candidate) {
        return New-PlanAction -Kind 'stop' -Message 'Every step is Done.'
    }
    $label = "Step $($candidate.Id)"
    if ($candidate.Status -eq 'Blocked') {
        return New-PlanAction -Kind 'stop' -Id $candidate.Id -Message "$label is Blocked; see its Summary and Open Questions."
    }
    if ($candidate.NeedsOwner) {
        return New-PlanAction -Kind 'stop' -Id $candidate.Id -Message "$label needs the owner."
    }
    if ($candidate.Interactive) {
        return New-PlanAction -Kind 'stop' -Id $candidate.Id -Message "$label is interactive; run it with the owner."
    }
    $isFirstStepOfPhase = ($Steps | Where-Object { $_.Phase -eq $candidate.Phase } | Select-Object -First 1).Id -eq $candidate.Id
    $previousPhaseStep = $Steps | Where-Object { $_.Phase -lt $candidate.Phase } | Select-Object -Last 1
    if ($isFirstStepOfPhase -and $null -ne $previousPhaseStep -and $previousPhaseStep.PhaseCheckpoint -and $previousPhaseStep.PhaseReviewed) {
        return New-PlanAction -Kind 'stop' -Id $candidate.Id -Message "Phase $($previousPhaseStep.Phase) is a checkpoint; the owner reviews it before $label."
    }
    if (-not [string]::IsNullOrWhiteSpace($Until) -and [version]$candidate.Id -gt [version]$Until) {
        return New-PlanAction -Kind 'stop' -Id $candidate.Id -Message "Reached -Until $Until; next would be $label."
    }
    New-PlanAction -Kind 'step' -Id $candidate.Id -Phase $candidate.Phase -Message $label
}

function Test-UsageLimitText {
    param([AllowEmptyString()][string]$Text)
    $Text -match $script:UsageLimitPattern
}

function Get-LimitWaitSeconds {
    param([AllowEmptyString()][string]$Text, [datetime]$Now = (Get-Date))

    $hour = $null
    $minute = 0
    if ($Text -match '\b(\d{1,2})(?::(\d{2}))?\s*(am|pm)\b') {
        $hour = [int]$Matches[1] % 12
        if ($Matches[2]) { $minute = [int]$Matches[2] }
        if ($Matches[3] -eq 'pm') { $hour += 12 }
    } elseif ($Text -match '\b(\d{1,2}):(\d{2})\b') {
        $hour = [int]$Matches[1]
        $minute = [int]$Matches[2]
    }
    if ($null -eq $hour -or $hour -gt 23 -or $minute -gt 59) {
        return $script:DefaultLimitWaitSeconds
    }
    $resetAt = $Now.Date.AddHours($hour).AddMinutes($minute + 1)
    if ($resetAt -le $Now) {
        $resetAt = $resetAt.AddDays(1)
    }
    [int][math]::Ceiling(($resetAt - $Now).TotalSeconds)
}

function Test-ActionSucceeded {
    param([Parameter(Mandatory)][object]$Action, [Parameter(Mandatory)][AllowEmptyCollection()][object[]]$Steps)
    if ($Action.Kind -eq 'review') {
        return [bool]($Steps | Where-Object { $_.Phase -eq $Action.Phase } | Select-Object -First 1).PhaseReviewed
    }
    ($Steps | Where-Object { $_.Id -eq $Action.Id }).Status -eq 'Done'
}

function Get-ActionPrompt {
    param([Parameter(Mandatory)][object]$Action, [Parameter(Mandatory)][string]$PlanName)
    if ($Action.Kind -eq 'review') {
        return "Review Phase $($Action.Phase) of $PlanName, following the 'Phase reviews' paragraph of 'How agents run this plan'."
    }
    "Execute Step $($Action.Id) of $PlanName, following 'How agents run this plan'."
}

function Invoke-ClaudeSession {
    param([Parameter(Mandatory)][object]$Action, [Parameter(Mandatory)][string]$PlanName, [Parameter(Mandatory)][string]$RunsDir)

    $safeId = $Action.Id -replace '[^\w.-]', '_'
    $logPath = Join-Path $RunsDir ("{0}-{1}.json" -f (Get-Date -Format 'yyyyMMdd-HHmmss'), $safeId)
    $previousEncoding = [Console]::OutputEncoding
    [Console]::OutputEncoding = [Text.Encoding]::UTF8 # 5.1 decodes native stdout as OEM otherwise
    $env:PLAN_STEP = $Action.Id
    try {
        $raw = (& claude -p (Get-ActionPrompt -Action $Action -PlanName $PlanName) --permission-mode auto --permission-prompts none --output-format json 2>&1) -join "`n"
    } finally {
        Remove-Item Env:PLAN_STEP -ErrorAction SilentlyContinue
        [Console]::OutputEncoding = $previousEncoding
    }
    [IO.File]::WriteAllText($logPath, $raw)

    $parsed = $null
    try {
        $parsed = $raw | ConvertFrom-Json
    } catch {
        $parsed = $null # crashed sessions print plain text; treated as a failed attempt below
    }
    [pscustomobject]@{
        LogPath = $logPath
        Raw = $raw
        IsError = if ($null -ne $parsed) { [bool]$parsed.is_error } else { $true }
        Result = if ($null -ne $parsed) { [string]$parsed.result } else { $raw }
        CostUsd = if ($null -ne $parsed) { $parsed.total_cost_usd } else { $null }
        DurationMs = if ($null -ne $parsed) { $parsed.duration_ms } else { $null }
    }
}

function Invoke-PlanRunner {
    param([string]$Plan, [switch]$DryRun, [switch]$Once, [string]$Until)

    $repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
    Set-Location $repoRoot
    $planPath = Join-Path $repoRoot $Plan
    $runsDir = Join-Path $repoRoot '.plan-runs'
    New-Item -ItemType Directory -Force -Path $runsDir | Out-Null

    $attemptsById = @{}
    while ($true) {
        $steps = @(Get-PlanSteps -Content (Read-PlanFile -Path $planPath))
        $action = Get-NextAction -Steps $steps -Until $Until
        if ($action.Kind -eq 'stop') {
            Write-Host "Stopping: $($action.Message)"
            return 0
        }
        if ($DryRun) {
            Write-Host "Next: $($action.Message)"
            return 0
        }

        Write-Host ("[{0}] Starting {1}" -f (Get-Date -Format 'HH:mm:ss'), $action.Message)
        $session = Invoke-ClaudeSession -Action $action -PlanName $Plan -RunsDir $runsDir
        $durationText = if ($null -ne $session.DurationMs) { '{0:N1} min' -f ($session.DurationMs / 60000) } else { '?' }
        $costText = if ($null -ne $session.CostUsd) { '${0:N2}' -f $session.CostUsd } else { '?' }

        $steps = @(Get-PlanSteps -Content (Read-PlanFile -Path $planPath))
        if (Test-ActionSucceeded -Action $action -Steps $steps) {
            Write-Host "$($action.Message): done, cost $costText, $durationText"
            $attemptsById.Remove($action.Id)
            if ($Once) {
                return 0
            }
            continue
        }
        if ($action.Kind -eq 'step' -and ($steps | Where-Object { $_.Id -eq $action.Id }).Status -eq 'Blocked') {
            Write-Host "$($action.Message): Blocked, cost $costText, $durationText. See its Summary (log $($session.LogPath))."
            return 1
        }
        if (Test-UsageLimitText -Text $session.Result) { # not counted as an attempt
            $waitSeconds = Get-LimitWaitSeconds -Text $session.Result
            Write-Host ("{0}: usage limit, waiting {1:N0} min until {2:HH:mm} (log {3})" -f $action.Message, ($waitSeconds / 60), (Get-Date).AddSeconds($waitSeconds), $session.LogPath)
            Start-Sleep -Seconds $waitSeconds
            continue
        }

        $attemptsById[$action.Id] =1 + [int]$attemptsById[$action.Id]
        Write-Host "$($action.Message): not finished (attempt $($attemptsById[$action.Id]) of $script:MaxAttemptsPerAction), cost $costText, $durationText, log $($session.LogPath)"
        if ($attemptsById[$action.Id] -ge $script:MaxAttemptsPerAction) {
            Write-Host "Stopping: $($action.Message) did not finish after $script:MaxAttemptsPerAction sessions. Last log: $($session.LogPath)"
            return 1
        }
    }
}

if ($MyInvocation.InvocationName -ne '.') {
    exit (Invoke-PlanRunner -Plan $Plan -DryRun:$DryRun -Once:$Once -Until $Until)
}
