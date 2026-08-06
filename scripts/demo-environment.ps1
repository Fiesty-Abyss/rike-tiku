param(
    [Parameter(Position = 0)]
    [ValidateSet('create', 'reset', 'seed', 'validate', 'clean', 'backend', 'frontend', 'smoke', 'login-check')]
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

$backendUrl = 'http://localhost:18081'
$frontendUrl = 'http://localhost:18080'
$apiBaseUrl = "$backendUrl/api/v1"

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

function Invoke-LoginSmoke([string]$Username, [string]$Role) {
    $payload = @{ username = $Username; password = 'a1234567'; expectedRole = $Role } | ConvertTo-Json -Compress
    $response = Invoke-RestMethod -Method Post -Uri "$apiBaseUrl/auth/login" -ContentType 'application/json' -Body $payload
    if (-not $response.accessToken -or $response.user.username -ne $Username -or $response.user.roles -notcontains $Role) {
        throw "$Role 演示账号登录响应不符合预期"
    }
    Write-Host "$Role 登录: PASS ($Username)" -ForegroundColor Green
}

function Invoke-SmokeCheck {
    Write-Host "后端地址: $backendUrl"
    Write-Host "前端地址: $frontendUrl"
    Write-Host "API基础地址: $apiBaseUrl"
    Write-Host '演示账号: demo_admin / demo_teacher / demo_student，固定密码仅限本地demo库。' -ForegroundColor Yellow

    $oldMysqlPassword = $env:MYSQL_PWD
    try {
        $env:MYSQL_PWD = $env:RIKE_TIKU_DB_PASSWORD
        $actualDatabase = & mysql --host=$dbHost --port=$dbPort --user=$dbUser --protocol=TCP --database=$Database --batch --skip-column-names --execute='SELECT DATABASE();'
        if ($LASTEXITCODE -ne 0 -or $actualDatabase.Trim() -ne $Database) { throw '演示数据库连接校验失败' }
        $demoAccountCount = & mysql --host=$dbHost --port=$dbPort --user=$dbUser --protocol=TCP --database=$Database --batch --skip-column-names --execute="SELECT COUNT(*) FROM yong_hu WHERE yong_hu_ming IN ('demo_admin','demo_teacher','demo_student');"
        if ($LASTEXITCODE -ne 0 -or [int]$demoAccountCount -ne 3) { throw '目标数据库未包含三个演示账号，请先执行seed' }
    } finally {
        $env:MYSQL_PWD = $oldMysqlPassword
    }
    Write-Host "实际数据库: $actualDatabase" -ForegroundColor Green

    $health = Invoke-WebRequest -UseBasicParsing -Uri "$apiBaseUrl/health"
    if ($health.StatusCode -ne 200) { throw "健康检查失败: HTTP $($health.StatusCode)" }
    Write-Host '健康接口: PASS (HTTP 200)' -ForegroundColor Green
    $frontend = Invoke-WebRequest -UseBasicParsing -Uri $frontendUrl
    if ($frontend.StatusCode -ne 200) { throw "前端页面检查失败: HTTP $($frontend.StatusCode)" }
    Write-Host '前端页面: PASS (HTTP 200)' -ForegroundColor Green
    Invoke-LoginSmoke 'demo_admin' 'ADMIN'
    Invoke-LoginSmoke 'demo_teacher' 'TEACHER'
    Invoke-LoginSmoke 'demo_student' 'STUDENT'

    $mismatchPayload = @{ username = 'demo_admin'; password = 'a1234567'; expectedRole = 'STUDENT' } | ConvertTo-Json -Compress
    try {
        Invoke-RestMethod -Method Post -Uri "$apiBaseUrl/auth/login" -ContentType 'application/json' -Body $mismatchPayload | Out-Null
        throw '错误角色入口未被拒绝'
    } catch {
        $errorBody = $_.ErrorDetails.Message
        if (-not $errorBody -or $errorBody -notmatch 'ROLE_MISMATCH') { throw }
    }
    Write-Host '错误角色入口: PASS (ROLE_MISMATCH)' -ForegroundColor Green
    Write-Host '演示环境smoke/login-check全部通过；JWT未输出。' -ForegroundColor Green
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
        $env:RIKE_TIKU_BACKEND_PORT = '18081'
        $env:RIKE_TIKU_CORS_ALLOWED_ORIGINS = 'http://localhost:18080'
        Write-Host "当前目标数据库: $Database"
        Write-Host "后端地址: $backendUrl"
        Write-Host "允许的前端地址: $frontendUrl"
        Write-Host '演示账号: demo_admin / demo_teacher / demo_student，固定密码仅限本地demo库。' -ForegroundColor Yellow
        Push-Location $backendRoot
        try { & mvn spring-boot:run } finally { Pop-Location }
    }
    'frontend' {
        $env:VITE_API_BASE_URL = 'http://localhost:18081/api/v1'
        Write-Host "前端地址: $frontendUrl"
        Write-Host "API基础地址: $apiBaseUrl"
        Write-Host '演示账号: demo_admin / demo_teacher / demo_student，固定密码仅限本地demo库。' -ForegroundColor Yellow
        Push-Location $frontendRoot
        try { & npm run dev -- --host localhost --port 18080 } finally { Pop-Location }
    }
    'smoke' { Invoke-SmokeCheck }
    'login-check' { Invoke-SmokeCheck }
}
