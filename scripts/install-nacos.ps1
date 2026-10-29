$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$toolsDir = Join-Path $root "tools\nacos-server"
$nacosHome = Join-Path $toolsDir "nacos"
$version = "2.3.2"
$zipName = "nacos-server-$version.zip"
$zipPath = Join-Path $toolsDir $zipName
$url = "https://github.com/alibaba/nacos/releases/download/$version/$zipName"

if (Test-Path (Join-Path $nacosHome "bin\startup.cmd")) {
    Write-Host "Nacos 已存在于 $nacosHome，跳过下载。"
    exit 0
}

New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
Write-Host "正在下载 Nacos $version ..."
Invoke-WebRequest -Uri $url -OutFile $zipPath -UseBasicParsing
Write-Host "正在解压到 $toolsDir ..."
Expand-Archive -Path $zipPath -DestinationPath $toolsDir -Force
Remove-Item $zipPath
Write-Host "完成。启动：.\scripts\start-nacos.ps1"
