$ErrorActionPreference = "Stop"
$procs = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" |
    Where-Object { $_.CommandLine -match "map-(ingest|geo|ai|gateway)-0\.1\.0\.jar" }
foreach ($proc in $procs) {
    Stop-Process -Id $proc.ProcessId -Force
    Write-Host "已停止 $($proc.ProcessId) $($proc.CommandLine)"
}
if (-not $procs) {
    Write-Host "没有发现本项目的 Java 服务"
}

$node = Get-CimInstance Win32_Process -Filter "Name = 'node.exe'" |
    Where-Object { $_.CommandLine -match "ai-open-map\\frontend" -and $_.CommandLine -match "vite" }
foreach ($proc in $node) {
    Stop-Process -Id $proc.ProcessId -Force
    Write-Host "已停止前端 $($proc.ProcessId)"
}
