$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
New-Item -ItemType Directory -Force -Path (Join-Path $root "logs") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $root "data\osm") | Out-Null

$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\java.exe" } else { "D:\java\jdk-17.0.10\bin\java.exe" }
if (-not (Test-Path $java)) {
    $found = Get-Command java -ErrorAction SilentlyContinue
    if (-not $found) { throw "未找到 java，请安装 JDK 17 或设置 JAVA_HOME" }
    $java = $found.Source
}

function Test-Listening([int] $port) {
    return [bool](Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
}

function Start-ServiceJar([string] $name, [int] $port) {
    if (Test-Listening $port) {
        Write-Host "$name 已在 $port 监听，跳过"
        return
    }
    $jar = Join-Path $root "backend\$name\target\$name-0.1.0.jar"
    if (-not (Test-Path $jar)) {
        throw "未找到 $jar。请先在仓库根目录执行：mvn -DskipTests package"
    }
    Start-Process -FilePath $java -ArgumentList @("-jar", $jar) `
        -WorkingDirectory $root `
        -RedirectStandardOutput (Join-Path $root "logs\$name.log") `
        -RedirectStandardError (Join-Path $root "logs\$name.err") `
        -WindowStyle Hidden
    Write-Host "已启动 $name  http://127.0.0.1:$port"
}

$env:OSM_DATA_DIR = Join-Path $root "data\osm"
Start-ServiceJar "map-ingest" 8081
Start-ServiceJar "map-geo" 8082
Start-ServiceJar "map-ai" 8083
Start-ServiceJar "map-gateway" 8080

if (Test-Listening 5173) {
    Write-Host "前端已在 5173 监听，跳过"
} else {
    $npm = (Get-Command npm.cmd -ErrorAction SilentlyContinue).Source
    if (-not $npm) { throw "未找到 npm.cmd，后端已启动，前端请到 frontend 目录手动 npm run dev" }
    Start-Process -FilePath $npm -ArgumentList @("run", "dev", "--", "--host", "127.0.0.1") `
        -WorkingDirectory (Join-Path $root "frontend") `
        -RedirectStandardOutput (Join-Path $root "logs\frontend.log") `
        -RedirectStandardError (Join-Path $root "logs\frontend.err") `
        -WindowStyle Hidden
    Write-Host "已启动前端  http://127.0.0.1:5173"
}

Write-Host "网关 http://127.0.0.1:8080    Druid 监控见各服务 /druid/ （admin / admin）"
