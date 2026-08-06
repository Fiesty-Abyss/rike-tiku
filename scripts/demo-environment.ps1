param(
    [Parameter(Position = 0)]
    [ValidateSet('create', 'reset', 'seed', 'validate', 'clean', 'backend', 'frontend')]
    [string]$Action = 'validate',
    [string]$Database = 'rike_tiku_demo'
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
$OutputEncoding = [System.Text.UTF8Encoding]::new()
$protectedDatabases = @('rike_tiku', 'mysql', 'information_schema', 'performance_schema', 'sys')
if ($Database -notmatch '^[A-Za-z0-9_]+$' -or $protectedDatabases -contains $Database.ToLowerInvariant()) {
    throw "拒绝对受保护或非法数据库执行演示环境操作: $Database"
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$backendRoot = Join-Path $repoRoot 'rike-tiku-backend'
$frontendRoot = Join-Path $repoRoot 'rike-tiku-frontend'
$dbHost = if ($env:RIKE_TIKU_DB_HOST) { $env:RIKE_TIKU_DB_HOST } else { 'localhost' }
$dbPort = if ($env:RIKE_TIKU_DB_PORT) { $env:RIKE_TIKU_DB_PORT } else { '3306' }
$dbUser = if ($env:RIKE_TIKU_DB_USERNAME) { $env:RIKE_TIKU_DB_USERNAME } else { 'root' }
if (-not $env:RIKE_TIKU_DB_PASSWORD) {
    throw '请先在当前PowerShell设置 RIKE_TIKU_DB_PASSWORD。脚本不会读取或提交明文数据库密码。'
}

Write-Host "演示环境目标数据库: $Database ($dbHost`:$dbPort)" -ForegroundColor Cyan
Write-Host '该工具仅用于独立本地演示库，不会操作正式 rike_tiku。' -ForegroundColor Yellow

function Invoke-MySql([string]$Sql) {
    $oldMysqlPassword = $env:MYSQL_PWD
    try {
        $env:MYSQL_PWD = $env:RIKE_TIKU_DB_PASSWORD
        & mysql --host=$dbHost --port=$dbPort --user=$dbUser --protocol=TCP --execute=$Sql
        if ($LASTEXITCODE -ne 0) { throw "MySQL命令失败，退出码: $LASTEXITCODE" }
    } finally {
        $env:MYSQL_PWD = $oldMysqlPassword
    }
}

function Invoke-DemoTool([string]$ToolAction) {
    $oldDatabase = $env:RIKE_TIKU_DB_NAME
    try {
        $env:RIKE_TIKU_DB_NAME = $Database
        Push-Location $backendRoot
        & mvn spring-boot:run "-Dspring-boot.run.main-class=com.neu.riketiku.demo.DemoEnvironmentCommand" "-Dspring-boot.run.arguments=--demo.action=$ToolAction --server.port=0 --springdoc.api-docs.enabled=false"
        if ($LASTEXITCODE -ne 0) { throw "演示数据命令失败，退出码: $LASTEXITCODE" }
    } finally {
        Pop-Location
        $env:RIKE_TIKU_DB_NAME = $oldDatabase
    }
}

switch ($Action) {
    'create' {
        Invoke-MySql "CREATE DATABASE IF NOT EXISTS ``$Database`` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
        Invoke-DemoTool 'migrate'
    }
    'reset' {
        Invoke-MySql "DROP DATABASE IF EXISTS ``$Database``; CREATE DATABASE ``$Database`` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
        Invoke-DemoTool 'migrate'
    }
    'seed' { Invoke-DemoTool 'seed' }
    'validate' { Invoke-DemoTool 'validate' }
    'clean' { Invoke-DemoTool 'clean' }
    'backend' {
        $env:RIKE_TIKU_DB_NAME = $Database
        $env:RIKE_TIKU_SERVER_PORT = '18081'
        $env:RIKE_TIKU_CORS_ALLOWED_ORIGIN = 'http://localhost:18080'
        Push-Location $backendRoot
        try { & mvn spring-boot:run } finally { Pop-Location }
    }
    'frontend' {
        $env:VITE_API_BASE_URL = 'http://localhost:18081'
        Push-Location $frontendRoot
        try { & npm run dev -- --host localhost --port 18080 } finally { Pop-Location }
    }
}
