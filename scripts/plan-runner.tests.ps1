# Plain-PowerShell tests for plan-lib.ps1 and plan-runner.ps1 (no Pester). Exits 1 if any case fails.
. (Join-Path $PSScriptRoot 'plan-lib.ps1')
. (Join-Path $PSScriptRoot 'plan-runner.ps1')

$script:FailureCount = 0

function Assert-Case {
    param([string]$Name, [scriptblock]$Check)
    $passed = $false
    try {
        $passed = [bool](& $Check)
    } catch {
        Write-Host "  error: $_"
    }
    if ($passed) {
        Write-Host "PASS $Name"
    } else {
        Write-Host "FAIL $Name"
        $script:FailureCount++
    }
}

# {D} and {M} stand in for the plan's em dash and middot so this file stays ASCII
function New-FixturePlan {
    param([hashtable]$Status = @{}, [string]$Phase1Summary = 'Pending.', [string]$Phase1Marker = '', [string]$Step21Marker = '')
    $statusOf = { param($id) if ($Status.ContainsKey($id)) { $Status[$id] } else { 'Not started' } }
    $text = @"
# Fixture plan

## Phase 1 {D} Setup$Phase1Marker {M} Status: Not started

**Phase summary:** $Phase1Summary

### Step 1.1 {D} First thing

- **Status:** $(& $statusOf '1.1')
- **Summary:** _Pending._

### Step 1.2 {D} Second thing

- **Status:** $(& $statusOf '1.2')

## Phase 2 {D} Build {M} Status: Not started

**Phase summary:** _Pending._

### Step 2.1 {D} Third thing$Step21Marker

- **Status:** $(& $statusOf '2.1')

### Step 2.2 {D} Owner thing **(needs owner)**

- **Status:** $(& $statusOf '2.2')

## Session Log

### Step 9.9 {D} Not a real step

- **Status:** Not started
"@
    $text.Replace('{D}', [string][char]0x2014).Replace('{M}', [string][char]0x00B7)
}

function Get-FixtureAction {
    param([hashtable]$Status = @{}, [string]$Phase1Summary = 'Pending.', [string]$Phase1Marker = '', [string]$Step21Marker = '', [string]$Until)
    $plan = New-FixturePlan -Status $Status -Phase1Summary $Phase1Summary -Phase1Marker $Phase1Marker -Step21Marker $Step21Marker
    Get-NextAction -Steps @(Get-PlanSteps -Content $plan) -Until $Until
}

$parsed = @(Get-PlanSteps -Content (New-FixturePlan -Status @{ '1.1' = 'Done'; '1.2' = 'In progress' } -Step21Marker ' **(interactive)**' -Phase1Marker ' **(checkpoint)**' -Phase1Summary 'All built. Reviewed: none'))
Assert-Case 'parses every step and ignores headings outside phases' { $parsed.Count -eq 4 -and ($parsed.Id -join ',') -eq '1.1,1.2,2.1,2.2' }
Assert-Case 'parses step statuses' { $parsed[0].Status -eq 'Done' -and $parsed[1].Status -eq 'In progress' -and $parsed[2].Status -eq 'Not started' }
Assert-Case 'parses phase name, status and number' { $parsed[0].Phase -eq 1 -and $parsed[0].PhaseName -eq 'Setup' -and $parsed[0].PhaseStatus -eq 'Not started' -and $parsed[2].Phase -eq 2 }
Assert-Case 'parses markers' { $parsed[3].NeedsOwner -and -not $parsed[2].NeedsOwner -and $parsed[2].Interactive -and -not $parsed[3].Interactive -and $parsed[2].Title -eq 'Third thing' }
Assert-Case 'parses checkpoint and reviewed' { $parsed[0].PhaseCheckpoint -and $parsed[0].PhaseReviewed -and -not $parsed[2].PhaseCheckpoint -and -not $parsed[2].PhaseReviewed }
Assert-Case 'Get-StepStatus finds a step and returns null for an unknown one' {
    $plan = New-FixturePlan -Status @{ '1.2' = 'Blocked' }
    (Get-StepStatus -Content $plan -Id '1.2') -eq 'Blocked' -and $null -eq (Get-StepStatus -Content $plan -Id '7.7')
}

Assert-Case 'chooses the first Not started step after Done ones' {
    $action = Get-FixtureAction -Status @{ '1.1' = 'Done' }
    $action.Kind -eq 'step' -and $action.Id -eq '1.2' -and $action.Message -eq 'Step 1.2'
}
Assert-Case 'chooses an In progress step over later ones' {
    $action = Get-FixtureAction -Status @{ '1.1' = 'In progress' }
    $action.Kind -eq 'step' -and $action.Id -eq '1.1'
}
Assert-Case 'stops on a Blocked step' {
    $action = Get-FixtureAction -Status @{ '1.1' = 'Done'; '1.2' = 'Blocked' }
    $action.Kind -eq 'stop' -and $action.Id -eq '1.2'
}
Assert-Case 'stops on a needs-owner step' {
    $action = Get-FixtureAction -Status @{ '1.1' = 'Done'; '1.2' = 'Done'; '2.1' = 'Done' } -Phase1Summary 'Reviewed: none'
    $action.Kind -eq 'stop' -and $action.Id -eq '2.2'
}
Assert-Case 'stops on an interactive step' {
    $action = Get-FixtureAction -Status @{ '1.1' = 'Done'; '1.2' = 'Done' } -Phase1Summary 'Reviewed: none' -Step21Marker ' **(interactive)**'
    $action.Kind -eq 'stop' -and $action.Id -eq '2.1'
}
Assert-Case 'a completed unreviewed phase yields a review' {
    $action = Get-FixtureAction -Status @{ '1.1' = 'Done'; '1.2' = 'Done' }
    $action.Kind -eq 'review' -and $action.Id -eq 'review:1' -and $action.Phase -eq 1
}
Assert-Case 'a completed reviewed phase moves on to the next step' {
    $action = Get-FixtureAction -Status @{ '1.1' = 'Done'; '1.2' = 'Done' } -Phase1Summary 'Done. Reviewed: none'
    $action.Kind -eq 'step' -and $action.Id -eq '2.1'
}
Assert-Case 'a reviewed checkpoint phase stops' {
    $action = Get-FixtureAction -Status @{ '1.1' = 'Done'; '1.2' = 'Done' } -Phase1Summary 'Done. Reviewed: none' -Phase1Marker ' **(checkpoint)**'
    $action.Kind -eq 'stop' -and $action.Id -eq '2.1'
}
Assert-Case '-Until stops past the given step and allows it' {
    $stopped = Get-FixtureAction -Status @{ '1.1' = 'Done' } -Until '1.1'
    $allowed = Get-FixtureAction -Status @{ '1.1' = 'Done' } -Until '1.2'
    $stopped.Kind -eq 'stop' -and $allowed.Kind -eq 'step' -and $allowed.Id -eq '1.2'
}
Assert-Case 'the last phase is reviewed once all its steps are Done' {
    $action = Get-FixtureAction -Status @{ '1.1' = 'Done'; '1.2' = 'Done'; '2.1' = 'Done'; '2.2' = 'Done' } -Phase1Summary 'Reviewed: none'
    $action.Kind -eq 'review' -and $action.Phase -eq 2
}
Assert-Case 'a plan with nothing left stops' { (Get-NextAction -Steps @()).Kind -eq 'stop' }

$blockedPlan = New-FixturePlan -Status @{ '1.2' = 'Blocked' }
Assert-Case 'Test-StopAllowed allows when PLAN_STEP is empty' { Test-StopAllowed -PlanContent $blockedPlan -PlanStep '' -VerifyResult 1 }
Assert-Case 'Test-StopAllowed allows a Blocked step' { Test-StopAllowed -PlanContent $blockedPlan -PlanStep '1.2' -VerifyResult 1 }
Assert-Case 'Test-StopAllowed allows when verify exits 0' { Test-StopAllowed -PlanContent $blockedPlan -PlanStep '1.1' -VerifyResult 0 }
Assert-Case 'Test-StopAllowed blocks otherwise' { -not (Test-StopAllowed -PlanContent $blockedPlan -PlanStep '1.1' -VerifyResult 1) -and -not (Test-StopAllowed -PlanContent $blockedPlan -PlanStep 'review:1' -VerifyResult 1) }

Assert-Case 'usage-limit text matches' {
    (Test-UsageLimitText -Text "Claude AI usage limit reached|1762600000") -and (Test-UsageLimitText -Text "You've hit your limit - resets 3pm (Europe/London)") -and (Test-UsageLimitText -Text 'Rate limit exceeded, try again at 14:05')
}
Assert-Case 'a normal result does not match the usage-limit pattern' { -not (Test-UsageLimitText -Text 'Step 2.1 is Done; TimeWindowTest has 14 passing tests and verify exits 0.') }
Assert-Case 'reset time is parsed and waits until a minute after it' {
    $now = Get-Date '2026-11-08 13:00:00'
    (Get-LimitWaitSeconds -Text 'resets 3pm' -Now $now) -eq (2 * 3600 + 60) -and (Get-LimitWaitSeconds -Text 'try again at 14:05' -Now $now) -eq (3600 + 6 * 60) -and (Get-LimitWaitSeconds -Text 'resets 12:30am' -Now $now) -eq (11 * 3600 + 31 * 60)
}
Assert-Case 'no reset time falls back to 30 minutes' { (Get-LimitWaitSeconds -Text 'usage limit reached' -Now (Get-Date)) -eq 1800 }

if ($script:FailureCount -gt 0) {
    Write-Host "$script:FailureCount case(s) failed"
    exit 1
}
Write-Host 'All cases passed'
exit 0
