$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$nacosHome = Join-Path $root "tools\nacos-server\nacos"
$shutdown = Join-Path $nacosHome "bin\shutdown.cmd"

if (-not (Test-Path $shutdown)) {
    Write-Host "未找到 Nacos 安装目录，无需停止。"
    exit 0
}

Push-Location (Join-Path $nacosHome "bin")
try {
    & cmd.exe /c shutdown.cmd
} finally {
    Pop-Location
}
Write-Host "已执行 Nacos shutdown。"
