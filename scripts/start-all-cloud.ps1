$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

& (Join-Path $PSScriptRoot "start-nacos.ps1")
if ($LASTEXITCODE -ne 0) { throw "Nacos not ready; see start-nacos.ps1" }

New-Item -ItemType Directory -Force -Path (Join-Path $root "logs") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $root "data\osm") | Out-Null

$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\java.exe" } else { "D:\java\jdk-17.0.10\bin\java.exe" }
if (-not (Test-Path $java)) {
    $found = Get-Command java -ErrorAction SilentlyContinue
    if (-not $found) { throw "java not found; install JDK 17 or set JAVA_HOME" }
    $java = $found.Source
}

function Test-Listening([int] $port) {
    return [bool](Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
}

function Start-ServiceJar([string] $name, [int] $port) {
    if (Test-Listening $port) {
        Write-Host "$name already listening on ${port}, skip"
        return
    }
    $jar = Join-Path $root "backend\$name\target\$name-0.1.0.jar"
    if (-not (Test-Path $jar)) {
        throw "Missing $jar. From repo root run: mvn -DskipTests package"
    }
    Start-Process -FilePath $java -ArgumentList @("-jar", $jar, "--spring.profiles.active=cloud") `
        -WorkingDirectory $root `
        -RedirectStandardOutput (Join-Path $root "logs\$name.log") `
        -RedirectStandardError (Join-Path $root "logs\$name.err") `
        -WindowStyle Hidden
    Write-Host "Started $name (cloud) http://127.0.0.1:${port}"
}

$env:OSM_DATA_DIR = Join-Path $root "data\osm"
if (-not $env:NACOS_ADDR) { $env:NACOS_ADDR = "127.0.0.1:8848" }

Start-ServiceJar "map-ingest" 8081
Start-ServiceJar "map-geo" 8082
Start-ServiceJar "map-ai" 8083
Start-ServiceJar "map-gateway" 8080

Write-Host 'Gateway http://127.0.0.1:8080  |  Nacos http://127.0.0.1:8848/nacos'
Write-Host 'Check Nacos service list for map-ingest, map-geo, map-ai, map-gateway'
