<#
.SYNOPSIS
    Samples backend CPU/memory and PostgreSQL connection usage during a load test.

.DESCRIPTION
    k6 reports what the clients experienced. This sampler records what the server was
    doing at the same time, which is what turns "it was slow" into "the connection
    pool was exhausted":

      * the backend JVM: CPU seconds used, working set (RSS), thread count, handles;
      * PostgreSQL: how many backends are connected, how many are active versus idle,
        how many are waiting on a lock, and the longest running query.

    One CSV row is appended per interval. The database is only ever read
    (pg_stat_activity / pg_stat_database), so this is safe to run against any
    database, including production.

.PARAMETER BackendProcessId
    PID of the backend JVM to sample. Defaults to the newest java process whose
    command line mentions the RPIS jar or backend application.

.PARAMETER Database
    Database to inspect for connection counts. Default rpis_test.

.EXAMPLE
    .\sample-resources.ps1 -BackendProcessId 1292 -DurationSeconds 300 -OutputCsv .\results\resources.csv
#>
[CmdletBinding()]
param(
    [int]$BackendProcessId = 0,
    [string]$Database = 'rpis_test',
    [int]$DurationSeconds = 300,
    [int]$IntervalSeconds = 5,
    [string]$OutputCsv = 'load-tests/results/resources.csv',
    [string]$PsqlPath = 'C:\Program Files\PostgreSQL\18\bin\psql.exe',
    [string]$PgUser = 'postgres',
    [string]$PgPassword = '1234',
    [string]$PgHost = 'localhost'
)

$ErrorActionPreference = 'Stop'

function Resolve-BackendProcessId {
    $candidates = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" |
        Where-Object { $_.CommandLine -match 'backend-0\.0\.1-SNAPSHOT\.jar|RpisBackendApplication' }
    if (-not $candidates) {
        throw 'No RPIS backend JVM found. Start the backend before sampling.'
    }
    return ($candidates | Sort-Object CreationDate -Descending | Select-Object -First 1).ProcessId
}

if ($BackendProcessId -le 0) {
    $BackendProcessId = Resolve-BackendProcessId
}

$backend = Get-Process -Id $BackendProcessId -ErrorAction SilentlyContinue
if (-not $backend) {
    throw "Backend process $BackendProcessId is not running."
}

$outputDirectory = Split-Path -Parent $OutputCsv
if ($outputDirectory -and -not (Test-Path $outputDirectory)) {
    New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
}

"timestamp,elapsed_s,jvm_cpu_seconds,jvm_working_set_mb,jvm_threads,jvm_handles,pg_total,pg_active,pg_idle,pg_idle_in_transaction,pg_waiting_on_lock,pg_longest_query_s" |
    Set-Content -Path $OutputCsv

$env:PGPASSWORD = $PgPassword
$startedAt = Get-Date
$previousCpu = $backend.TotalProcessorTime.TotalSeconds
$previousSampleAt = $startedAt

Write-Host "Sampling backend PID $BackendProcessId and database '$Database' every $IntervalSeconds s for $DurationSeconds s."
Write-Host "Writing to $OutputCsv"

while (((Get-Date) - $startedAt).TotalSeconds -lt $DurationSeconds) {
    Start-Sleep -Seconds $IntervalSeconds

    $now = Get-Date
    $process = Get-Process -Id $BackendProcessId -ErrorAction SilentlyContinue
    if (-not $process) {
        Write-Warning 'The backend process stopped; ending the sample.'
        break
    }

    $cpuSeconds = $process.TotalProcessorTime.TotalSeconds
    $elapsed = [math]::Round(($now - $startedAt).TotalSeconds, 1)

    # One read-only query that returns every connection figure at once.
    # clock_timestamp() (not now()) is used because now() is the transaction start
    # time, which makes a query that began during the transaction look like it has
    # negative duration.
    $sql = @"
SELECT
  (SELECT count(*) FROM pg_stat_activity WHERE datname = '$Database')::text || ',' ||
  (SELECT count(*) FROM pg_stat_activity WHERE datname = '$Database' AND state = 'active')::text || ',' ||
  (SELECT count(*) FROM pg_stat_activity WHERE datname = '$Database' AND state = 'idle')::text || ',' ||
  (SELECT count(*) FROM pg_stat_activity WHERE datname = '$Database' AND state = 'idle in transaction')::text || ',' ||
  (SELECT count(*) FROM pg_stat_activity WHERE datname = '$Database' AND wait_event_type = 'Lock')::text || ',' ||
  COALESCE((SELECT greatest(round(max(extract(epoch from clock_timestamp() - query_start))::numeric, 2), 0)::text
            FROM pg_stat_activity
            WHERE datname = '$Database' AND state = 'active'), '0');
"@

    $row = & $PsqlPath -h $PgHost -U $PgUser -d postgres -t -A -c $sql 2>$null
    if (-not $row) {
        $row = '0,0,0,0,0,0'
    }

    $timestamp = $now.ToString('yyyy-MM-dd HH:mm:ss')
    $line = @(
        $timestamp,
        $elapsed,
        [math]::Round($cpuSeconds, 2),
        [math]::Round($process.WorkingSet64 / 1MB, 1),
        $process.Threads.Count,
        $process.HandleCount,
        $row
    ) -join ','

    Add-Content -Path $OutputCsv -Value $line
    $previousCpu = $cpuSeconds
    $previousSampleAt = $now
}

Write-Host "Sampling finished. Rows: $((Get-Content $OutputCsv).Count - 1)"
