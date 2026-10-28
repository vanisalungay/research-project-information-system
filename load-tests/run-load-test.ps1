<#
.SYNOPSIS
    Runs the RPIS load tests end to end against the isolated test database.

.DESCRIPTION
    One command that does the whole measured run:

      1. refuses to continue unless the backend it is about to test is actually
         pointed at the isolated rpis_test database (see -ExpectedDatabase);
      2. optionally starts the backend with the `loadtest` profile, which also
         generates the synthetic data set on first run;
      3. starts the resource sampler (backend CPU/RSS, PostgreSQL connections);
      4. runs the k6 scenarios;
      5. stops the sampler and reports where every artefact was written.

    Nothing here ever touches a production database: the guard in step 1 reads the
    database the running backend is connected to, and the synthetic data seeder
    itself refuses to run outside rpis_test.

.PARAMETER Scenario
    '500u'      the mixed 500-concurrent-user test (default)
    'login'     500 simultaneous logins
    'both'      both of the above, one after the other

.PARAMETER BaseUrl
    Base URL of the backend under test. Default http://localhost:8082 (the port the
    `loadtest` profile uses, so a developer instance on 8081 is left alone).

.PARAMETER SkipDatabaseCheck
    Skip the safety check. Only for the case where the backend is already known to
    point at the test database and cannot be probed.

.EXAMPLE
    .\run-load-test.ps1 -Scenario both
#>
[CmdletBinding()]
param(
    [ValidateSet('500u', 'login', 'both')]
    [string]$Scenario = '500u',
    [string]$BaseUrl = 'http://localhost:8082',
    [string]$ExpectedDatabase = 'rpis_test',
    [int]$SamplerDurationSeconds = 420,
    [string[]]$InstanceUrls = @('http://localhost:8082', 'http://localhost:8083'),
    [string]$LbControlUrl = 'http://localhost:8081',
    [switch]$SkipDatabaseCheck,
    [string]$K6Path = ''
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
if (-not (Test-Path (Join-Path $repoRoot 'package.json'))) {
    $repoRoot = (Get-Location).Path
}
$resultsDirectory = Join-Path $repoRoot 'load-tests/results'
New-Item -ItemType Directory -Force -Path $resultsDirectory | Out-Null

function Resolve-K6 {
    if ($K6Path -and (Test-Path $K6Path)) { return $K6Path }
    $onPath = Get-Command k6 -ErrorAction SilentlyContinue
    if ($onPath) { return $onPath.Source }
    $local = Get-ChildItem -Path (Join-Path $repoRoot 'tools') -Recurse -Filter 'k6.exe' -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($local) { return $local.FullName }
    throw 'k6 was not found. Install it (winget install k6) or put the portable binary under tools/.'
}

Write-Host '=== RPIS load test ==='
Write-Host "Target       : $BaseUrl"
Write-Host "Expected DB  : $ExpectedDatabase"
Write-Host "Scenario     : $Scenario"

# ---- 1. Safety: is the backend actually pointing at the test database? ---------
function Assert-Database([string]$Url, [string]$Label) {
    try {
        $ready = Invoke-RestMethod -Uri "$Url/api/health/ready" -TimeoutSec 10
    } catch {
        throw "$Label at $Url did not answer /api/health/ready. Start it with the 'loadtest' profile first. ($($_.Exception.Message))"
    }
    Write-Host ("  {0,-34} -> {1}" -f $Label, $ready.database)
    if ($ready.database -ne $ExpectedDatabase) {
        throw "Refusing to run: $Label is connected to '$($ready.database)', not '$ExpectedDatabase'. " +
              'Load-test writes must only ever reach the isolated test database.'
    }
}

if (-not $SkipDatabaseCheck) {
    # Every instance is asked DIRECTLY, not only through the load balancer. Asking the
    # balancer would check whichever instance round-robin happened to pick, so one instance
    # left pointing at the wrong database could pass unnoticed until the run was underway.
    Write-Host 'Verifying the database of every instance that will serve traffic:'
    foreach ($instanceUrl in $InstanceUrls) { Assert-Database $instanceUrl "instance $instanceUrl" }
    Assert-Database $BaseUrl 'load balancer'
}

# ---- 2. Resource sampler -------------------------------------------------------
# EVERY backend JVM is sampled, not just the newest. A two-instance run whose report covers
# one instance is not a two-instance measurement.
$jvms = @(Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" |
    Where-Object { $_.CommandLine -match 'backend-0\.0\.1-SNAPSHOT\.jar|RpisBackendApplication' } |
    Sort-Object CreationDate)

$samplers = @()
foreach ($jvm in $jvms) {
    $csv = Join-Path $resultsDirectory ("resources-{0}.csv" -f $jvm.ProcessId)
    Write-Host "Starting resource sampler for backend PID $($jvm.ProcessId) -> $csv"
    $samplers += Start-Process -FilePath 'powershell.exe' -ArgumentList @(
        '-NoProfile', '-ExecutionPolicy', 'Bypass',
        '-File', (Join-Path $repoRoot 'load-tests/monitor/sample-resources.ps1'),
        '-BackendProcessId', "$($jvm.ProcessId)",
        '-Database', $ExpectedDatabase,
        '-DurationSeconds', "$SamplerDurationSeconds",
        '-OutputCsv', $csv
    ) -NoNewWindow -PassThru
}
if ($samplers.Count -eq 0) { Write-Warning 'No backend JVM found; running without resource sampling.' }

# ---- 2b. Load-balancer distribution before the run -----------------------------
function Get-LbStatus {
    if (-not $LbControlUrl) { return $null }
    try { return Invoke-RestMethod -Uri "$LbControlUrl/lb/status" -TimeoutSec 5 } catch { return $null }
}

$lbBefore = Get-LbStatus
if ($lbBefore) {
    Write-Host 'Load-balancer targets before the run:'
    foreach ($t in $lbBefore.targets) {
        Write-Host ("  {0,-28} healthy={1} served={2}" -f $t.target, $t.healthy, $t.served)
    }
    if (@($lbBefore.targets | Where-Object { -not $_.healthy }).Count -gt 0) {
        throw 'Refusing to run: the load balancer reports an unhealthy target.'
    }
} else {
    Write-Warning "No load-balancer status at $LbControlUrl/lb/status; the traffic split will not be recorded."
}

# ---- 3. k6 ---------------------------------------------------------------------
$k6 = Resolve-K6
$scenarios = if ($Scenario -eq 'both') { @('500u', 'login') } else { @($Scenario) }

$failed = @()
foreach ($name in $scenarios) {
    $script = if ($name -eq '500u') { 'load-tests/k6/rpis-500u.js' } else { 'load-tests/k6/rpis-login-burst.js' }
    $log = Join-Path $resultsDirectory "k6-$name-run.log"
    Write-Host ''
    Write-Host "--- k6 scenario '$name' -> $log"
    Push-Location $repoRoot
    try {
        & $k6 run -e "BASE_URL=$BaseUrl" $script 2>&1 | Tee-Object -FilePath $log
        if ($LASTEXITCODE -ne 0) { $failed += $name }
    } finally {
        Pop-Location
    }
}

foreach ($s in $samplers) {
    if (-not $s.HasExited) { $s.WaitForExit() }
}

# ---- 3b. Load-balancer distribution after the run ------------------------------
$lbAfter = Get-LbStatus
if ($lbAfter) {
    Write-Host ''
    Write-Host 'Load-balancer distribution after the run:'
    foreach ($t in $lbAfter.targets) {
        Write-Host ("  {0,-28} healthy={1} served={2} errors={3} meanMs={4} maxMs={5}" -f
            $t.target, $t.healthy, $t.served, $t.errors, $t.meanMs, $t.maxMs)
    }
    $lbAfter | ConvertTo-Json -Depth 5 | Set-Content -Path (Join-Path $resultsDirectory 'lb-status-after.json')
}

# ---- 4. Report -----------------------------------------------------------------
Write-Host ''
Write-Host '=== Artefacts ==='
Get-ChildItem $resultsDirectory -File | Sort-Object Name | ForEach-Object {
    Write-Host ('  {0,-40} {1,10:N0} bytes' -f $_.Name, $_.Length)
}

if ($failed.Count -gt 0) {
    Write-Warning "Thresholds were breached by: $($failed -join ', '). See the k6 logs above."
    exit 1
}
Write-Host 'All thresholds passed.'
