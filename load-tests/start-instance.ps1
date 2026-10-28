<#
.SYNOPSIS
    Starts one RPIS backend instance for a multi-instance verification run.

.DESCRIPTION
    Starts the backend with the `loadtest` profile, which is the only profile whose
    datasource points at the isolated `rpis_test` database, and with an explicit
    per-instance port so several instances can run side by side on one host.

    Two settings are deliberately NOT taken from the profile, because they must be
    identical across instances and are therefore the caller's responsibility:

      * RPIS_TOKEN_SECRET - tokens are HMAC-signed, so an instance with a different
        secret would reject a token minted by its neighbour. Passed in, never written to
        a file in the repository.
      * RPIS_UPLOAD_DIR - the storage root must be shared, or a document uploaded
        through one instance would be invisible to the other.

    Everything else comes from application.properties and application-loadtest.properties,
    so there is only one place that states the database, the pool rule and the DDL mode.

.PARAMETER Port
    Port this instance listens on. Also names the log files.

.PARAMETER TokenSecret
    Shared HMAC signing key. Defaults to $env:RPIS_TOKEN_SECRET, and the script fails if
    neither is set. A throwaway value is fine for a load test; a real deployment takes it
    from a secret store (see backend/DEPLOYMENT.md).

.PARAMETER PoolSize
    Hikari maximum pool size for THIS instance. The server-wide rule is
    instances x pool <= max_connections - reserved, so this must be chosen per instance
    count. Defaults to 30, the documented two-instance value.

.EXAMPLE
    $env:RPIS_TOKEN_SECRET = [guid]::NewGuid().ToString('N')
    .\start-instance.ps1 -Port 8082
    .\start-instance.ps1 -Port 8083
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][int]$Port,
    [string]$TokenSecret = '',
    [int]$PoolSize = 30,
    [string]$UploadDir = 'C:\rpis-data\uploads',
    [string]$LogDirectory = ''
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path (Join-Path $repoRoot 'package.json'))) { $repoRoot = (Get-Location).Path }
if (-not $LogDirectory) { $LogDirectory = Join-Path $repoRoot 'load-tests/results' }
New-Item -ItemType Directory -Force -Path $LogDirectory | Out-Null

if (-not $TokenSecret) { $TokenSecret = $env:RPIS_TOKEN_SECRET }
if (-not $TokenSecret) {
    throw 'No token secret. Set RPIS_TOKEN_SECRET in the environment (a throwaway value is fine for a load test) or pass -TokenSecret.'
}

$jar = Join-Path $repoRoot 'backend/target/backend-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path $jar)) { throw "The backend jar is not built: $jar" }

$java = Join-Path $repoRoot 'backend/jdk-17.0.11+9/bin/java.exe'
if (-not (Test-Path $java)) { $java = 'java' }

# The child inherits this process environment, so these are the instance settings.
$env:SPRING_PROFILES_ACTIVE = 'loadtest'
$env:RPIS_SERVER_PORT       = "$Port"
$env:RPIS_DB_POOL_SIZE      = "$PoolSize"
$env:RPIS_TOKEN_SECRET      = $TokenSecret
$env:RPIS_UPLOAD_DIR        = $UploadDir
# Stated explicitly rather than inherited, so an ambient RPIS_PASSWORD_MIGRATION=true
# cannot make a load-test instance start converting passwords.
$env:RPIS_PASSWORD_MIGRATION = 'false'

$outLog = Join-Path $LogDirectory "inst-$Port-out.log"
$errLog = Join-Path $LogDirectory "inst-$Port-err.log"

Write-Host "Starting instance on port $Port (pool $PoolSize, profile loadtest)"
Write-Host "  upload dir : $UploadDir"
Write-Host "  stdout     : $outLog"

$process = Start-Process -FilePath $java -ArgumentList @('-jar', $jar) `
    -RedirectStandardOutput $outLog -RedirectStandardError $errLog `
    -NoNewWindow -PassThru

Write-Host "  pid        : $($process.Id)"
$process.Id