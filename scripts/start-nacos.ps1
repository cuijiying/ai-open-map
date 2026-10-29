$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$nacosHome = Join-Path $root "tools\nacos-server\nacos"
$startup = Join-Path $nacosHome "bin\startup.cmd"

if (-not (Test-Path $startup)) {
    Write-Host "Nacos not found. Run: .\scripts\install-nacos.ps1"
    exit 1
}

function Test-Listening([int] $port) {
    return [bool](Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
}

if (Test-Listening 8848) {
    Write-Host "Port 8848 is listening. Nacos console: http://127.0.0.1:8848/nacos"
    exit 0
}

$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\java.exe" } else { $null }
if (-not $java -or -not (Test-Path $java)) {
    $found = Get-Command java -ErrorAction SilentlyContinue
    if (-not $found) { throw "java not found (JDK 17+ required for Nacos 2.x)" }
    $java = $found.Source
}

New-Item -ItemType Directory -Force -Path (Join-Path $root "logs") | Out-Null

$binDir = Join-Path $nacosHome "bin"
Start-Process -FilePath "cmd.exe" -ArgumentList @("/c", "startup.cmd", "-m", "standalone") `
    -WorkingDirectory $binDir `
    -WindowStyle Hidden

Write-Host "Starting Nacos standalone, waiting for port 8848 ..."
$deadline = (Get-Date).AddSeconds(90)
while ((Get-Date) -lt $deadline) {
    if (Test-Listening 8848) {
        Write-Host "Nacos ready: http://127.0.0.1:8848/nacos"
        exit 0
    }
    Start-Sleep -Seconds 2
}
Write-Host "Port 8848 not open. Check logs under tools/nacos-server/nacos/logs"
exit 1
