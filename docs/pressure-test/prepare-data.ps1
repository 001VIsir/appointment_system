param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$MerchantUsername = "merchant-load",
    [string]$MerchantPassword = "Secret123!",
    [string]$MerchantEmail = "merchant-load@example.com",
    [int]$TasksCount = 5,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

function Invoke-JsonPost {
    param(
        [string]$Url,
        [object]$Body,
        [Microsoft.PowerShell.Commands.WebRequestSession]$Session,
        [switch]$NoSession
    )
    $json = $Body | ConvertTo-Json -Depth 6 -Compress
    if ($DryRun) {
        Write-Host "[DRYRUN] POST $Url"
        Write-Host $json
        return $null
    }
    if ($NoSession) {
        return Invoke-RestMethod -Method Post -Uri $Url -ContentType "application/json" -Body $json
    }
    return Invoke-RestMethod -Method Post -Uri $Url -ContentType "application/json" -Body $json -WebSession $Session
}

function Ensure-Dir {
    param([string]$Path)
    if (-not (Test-Path $Path)) {
        New-Item -ItemType Directory -Path $Path | Out-Null
    }
}

$root = Split-Path -Parent $PSCommandPath
$dataDir = Join-Path $root "data"
Ensure-Dir $dataDir

$tokensPath = Join-Path $dataDir "signed.csv"
$slotsPath = Join-Path $dataDir "slots.csv"

if ($DryRun) {
    Write-Host "[DRYRUN] will write: $tokensPath"
    Write-Host "[DRYRUN] will write: $slotsPath"
}

# 1) Register merchant (ignore error if exists)
$registerUrl = "$BaseUrl/api/auth/register"
$registerBody = @{ username = $MerchantUsername; password = $MerchantPassword; email = $MerchantEmail; role = "MERCHANT" }
try {
    Invoke-JsonPost -Url $registerUrl -Body $registerBody -NoSession | Out-Null
} catch {
    if (-not $DryRun) {
        Write-Host "Register skipped: $($_.Exception.Message)"
    }
}

# 2) Login merchant
$loginUrl = "$BaseUrl/api/auth/login"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$loginBody = @{ username = $MerchantUsername; password = $MerchantPassword }
Invoke-JsonPost -Url $loginUrl -Body $loginBody -Session $session | Out-Null

# 3) Create tasks and collect signed links/slotIds
$tokens = @("signed")
$slots = @("slotId")

for ($i = 0; $i -lt $TasksCount; $i++) {
    $now = Get-Date
    $taskStart = $now.AddHours(1)
    $taskEnd = $now.AddHours(6)
    $slotStart = $now.AddHours(2)
    $slotEnd = $now.AddHours(3)

    $taskBody = @{
        title = "压测任务-$i"
        category = "LOAD"
        startTime = $taskStart.ToString("o")
        endTime = $taskEnd.ToString("o")
        description = "压力测试任务"
        active = $true
        slots = @(
            @{ title = "时段-$i"; startTime = $slotStart.ToString("o"); endTime = $slotEnd.ToString("o"); capacity = 500; location = "A" }
        )
    }

    $taskUrl = "$BaseUrl/api/tasks"
    $resp = Invoke-JsonPost -Url $taskUrl -Body $taskBody -Session $session

    if ($DryRun) {
        $tokens += "REPLACE_WITH_SIGNED_$i"
        $slots += "REPLACE_WITH_SLOT_$i"
    } else {
        $taskId = $resp.data.id
        $slotId = $resp.data.slots[0].id
        $signedResp = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/tasks/$taskId/signed-link?expiresInSeconds=3600" -WebSession $session
        $tokens += $signedResp.data.signed
        $slots += $slotId
    }
}

if (-not $DryRun) {
    $tokens | Set-Content -Path $tokensPath -Encoding UTF8
    $slots | Set-Content -Path $slotsPath -Encoding UTF8
    Write-Host "Wrote signed links to $tokensPath"
    Write-Host "Wrote slotIds to $slotsPath"
}
