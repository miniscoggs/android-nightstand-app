# Plan parsing shared by the Stop hook, the runner and the runner tests. Dot-source it.

$script:EmDash = [string][char]0x2014 # the plan's heading separators, kept out of this file so it stays ASCII
$script:MiddleDot = [string][char]0x00B7
$script:PhaseHeadingPattern = "^## Phase (\d+) $script:EmDash (.+?) $script:MiddleDot Status: (.+?)\s*$"
$script:StepHeadingPattern = "^### Step (\d+\.\d+) $script:EmDash (.+?)\s*$"
$script:StepStatusPattern = '^\s*-\s+\*\*Status:\*\*\s*(.+?)\s*$'
$script:MarkerPattern = '\s*\*\*\((needs owner|checkpoint|interactive|own window)\)\*\*'

function Read-PlanFile {
    param([Parameter(Mandatory)][string]$Path)
    Get-Content -Raw -Encoding UTF8 -LiteralPath $Path # 5.1 defaults to ANSI, which mangles the plan's dashes
}

function Get-PlanSteps {
    param([Parameter(Mandatory)][AllowEmptyString()][string]$Content)

    $lines = $Content -split "\r?\n"
    $phases = @()
    $currentPhase = $null
    $currentStep = $null
    $inPhasePreamble = $false

    foreach ($line in $lines) {
        if ($line -match $script:PhaseHeadingPattern) {
            $headingName = $Matches[2]
            $currentPhase = [pscustomobject]@{
                Number = [int]$Matches[1]
                Name = ($headingName -replace $script:MarkerPattern, '').Trim()
                Status = $Matches[3]
                Checkpoint = $headingName -match '\*\*\(checkpoint\)\*\*'
                Reviewed = $false
                Steps = New-Object System.Collections.ArrayList
            }
            $phases += $currentPhase
            $currentStep = $null
            $inPhasePreamble = $true
            continue
        }
        if ($line -match '^## ') {
            $currentPhase = $null
            $currentStep = $null
            $inPhasePreamble = $false
            continue
        }
        if ($null -eq $currentPhase) {
            continue
        }
        if ($line -match $script:StepHeadingPattern) {
            $heading = $Matches[2]
            $currentStep = [pscustomobject]@{
                Id = $Matches[1]
                Title = ($heading -replace $script:MarkerPattern, '').Trim()
                Status = $null
                NeedsOwner = $heading -match '\*\*\(needs owner\)\*\*'
                Interactive = $heading -match '\*\*\(interactive\)\*\*'
            }
            if ($heading -match '\*\*\(checkpoint\)\*\*') {
                $currentPhase.Checkpoint = $true
            }
            [void]$currentPhase.Steps.Add($currentStep)
            $inPhasePreamble = $false
            continue
        }
        if ($inPhasePreamble -and $line -match 'Reviewed:') { # summary region runs until the phase's first step
            $currentPhase.Reviewed = $true
            continue
        }
        if ($null -ne $currentStep -and $null -eq $currentStep.Status -and $line -match $script:StepStatusPattern) {
            $currentStep.Status = $Matches[1]
        }
    }

    foreach ($phase in $phases) {
        foreach ($step in $phase.Steps) {
            [pscustomobject]@{
                Phase = $phase.Number
                PhaseName = $phase.Name
                PhaseStatus = $phase.Status
                PhaseCheckpoint = $phase.Checkpoint
                PhaseReviewed = $phase.Reviewed
                Id = $step.Id
                Title = $step.Title
                Status = $step.Status
                NeedsOwner = $step.NeedsOwner
                Interactive = $step.Interactive
            }
        }
    }
}

function Get-StepStatus {
    param(
        [Parameter(Mandatory)][AllowEmptyString()][string]$Content,
        [Parameter(Mandatory)][string]$Id
    )
    $step = Get-PlanSteps -Content $Content | Where-Object { $_.Id -eq $Id } | Select-Object -First 1
    if ($null -eq $step) {
        return $null
    }
    $step.Status
}

function Test-StopAllowed {
    param(
        [Parameter(Mandatory)][AllowEmptyString()][string]$PlanContent,
        [AllowEmptyString()][string]$PlanStep,
        [int]$VerifyResult
    )
    if ([string]::IsNullOrWhiteSpace($PlanStep)) {
        return $true
    }
    if ((Get-StepStatus -Content $PlanContent -Id $PlanStep) -eq 'Blocked') {
        return $true
    }
    $VerifyResult -eq 0
}
