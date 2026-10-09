$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$psql = if ($env:PSQL) { $env:PSQL } else { "C:\Program Files\PostgreSQL\16\bin\psql.exe" }
if (-not (Test-Path $psql)) {
    throw "未找到 psql：$psql"
}
if (-not $env:PGPASSWORD) {
    $env:PGPASSWORD = "post"
}
$env:PGCLIENTENCODING = "UTF8"
$exists = & $psql -U postgres -h localhost -p 5432 -tAc "SELECT 1 FROM pg_database WHERE datname = 'ai_open_map'"
if ($exists.Trim() -ne "1") {
    & $psql -U postgres -h localhost -p 5432 -c "CREATE DATABASE ai_open_map ENCODING 'UTF8'"
}
& $psql -U postgres -h localhost -p 5432 -d ai_open_map -v ON_ERROR_STOP=1 -f (Join-Path $root "sql\schema.sql")
Write-Host "数据库 ai_open_map 已就绪"
