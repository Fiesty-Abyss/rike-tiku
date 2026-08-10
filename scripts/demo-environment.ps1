param(
    [Parameter(Position = 0)]
    [ValidateSet('create', 'reset', 'seed', 'validate', 'clean', 'final-acceptance', 'backend', 'smoke-backend', 'frontend', 'smoke', 'login-check')]
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
$databaseActions = @('create', 'reset', 'seed', 'validate', 'clean', 'final-acceptance', 'backend', 'smoke-backend', 'smoke', 'login-check')
if ($databaseActions -contains $Action -and -not $env:RIKE_TIKU_DB_PASSWORD) {
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

function Reset-DemoDatabase {
    Invoke-MySql "DROP DATABASE IF EXISTS ``$Database``; CREATE DATABASE ``$Database`` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
    Invoke-DemoTool 'migrate'
}

function Assert-FinalAcceptanceConfiguration {
    if ($Database -ne 'rike_tiku_demo') {
        throw "最终人工验收只允许使用 rike_tiku_demo，当前目标: $Database"
    }
    if ($backendUrl -ne 'http://localhost:18081' -or $frontendUrl -ne 'http://localhost:18080' -or $apiBaseUrl -ne 'http://localhost:18081/api/v1') {
        throw '最终人工验收端口配置必须为前端18080、后端18081。'
    }
    Write-Host '最终验收配置检查: PASS (rike_tiku_demo / frontend 18080 / backend 18081)' -ForegroundColor Green
}

function Invoke-LoginSmoke([string]$Username, [string]$Role) {
    $challenge = Invoke-RestMethod -Method Get -Uri "$apiBaseUrl/auth/captcha-challenge"
    if (-not $challenge.testCode) {
        throw 'Demo smoke 需要以 RIKE_TIKU_CAPTCHA_EXPOSE_TEST_CODE=true 启动本地演示后端。'
    }
    $payload = @{ username = $Username; password = 'a1234567'; expectedRole = $Role; challengeId = $challenge.challengeId; captchaCode = $challenge.testCode } | ConvertTo-Json -Compress
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

    $mismatchChallenge = Invoke-RestMethod -Method Get -Uri "$apiBaseUrl/auth/captcha-challenge"
    $mismatchPayload = @{ username = 'demo_admin'; password = 'a1234567'; expectedRole = 'STUDENT'; challengeId = $mismatchChallenge.challengeId; captchaCode = $mismatchChallenge.testCode } | ConvertTo-Json -Compress
    Add-Type -AssemblyName System.Net.Http
    $httpClient = [System.Net.Http.HttpClient]::new()
    try {
        $httpContent = [System.Net.Http.StringContent]::new($mismatchPayload, [System.Text.Encoding]::UTF8, 'application/json')
        $mismatchResponse = $httpClient.PostAsync("$apiBaseUrl/auth/login", $httpContent).GetAwaiter().GetResult()
        $mismatchBody = $mismatchResponse.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        if ([int]$mismatchResponse.StatusCode -ne 403 -or $mismatchBody -notmatch 'ROLE_MISMATCH') {
            throw '错误角色入口未按预期返回403 ROLE_MISMATCH'
        }
    } finally {
        $httpClient.Dispose()
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
        Reset-DemoDatabase
    }
    'seed' { Invoke-DemoTool 'seed' }
    'validate' { Invoke-DemoTool 'validate' }
    'clean' { Invoke-DemoTool 'clean' }
    'final-acceptance' {
        Assert-FinalAcceptanceConfiguration
        Reset-DemoDatabase
        Invoke-DemoTool 'seed'
        Invoke-DemoTool 'validate'
        Write-Host '最终人工验收数据准备完成。请在当前PowerShell启动后端：' -ForegroundColor Green
        Write-Host '.\scripts\demo-environment.ps1 backend'
        Write-Host '另开PowerShell启动前端：'
        Write-Host '.\scripts\demo-environment.ps1 frontend'
        Write-Host "浏览器入口: $frontendUrl"
    }
    'backend' {
        $env:RIKE_TIKU_DB_NAME = $Database
        $env:RIKE_TIKU_BACKEND_PORT = '18081'
        $env:RIKE_TIKU_CORS_ALLOWED_ORIGINS = 'http://localhost:18080'
        $env:RIKE_TIKU_CAPTCHA_EXPOSE_TEST_CODE = 'false'
        Write-Host "当前目标数据库: $Database"
        Write-Host "后端地址: $backendUrl"
        Write-Host "允许的前端地址: $frontendUrl"
        Write-Host '人工验收模式：CAPTCHA testCode未暴露，请在页面识别真实验证码。' -ForegroundColor Yellow
        Write-Host '验收账号: demo_admin / demo_199_01 / demo_teacher / demo_physics_admin。' -ForegroundColor Yellow
        Push-Location $backendRoot
        try { & mvn spring-boot:run } finally { Pop-Location }
    }
    'smoke-backend' {
        $env:RIKE_TIKU_DB_NAME = $Database
        $env:RIKE_TIKU_BACKEND_PORT = '18081'
        $env:RIKE_TIKU_CORS_ALLOWED_ORIGINS = 'http://localhost:18080'
        $env:RIKE_TIKU_CAPTCHA_EXPOSE_TEST_CODE = 'true'
        Write-Host '仅供脚本机器 smoke：CAPTCHA testCode已临时暴露，不得用于人工浏览器验收。' -ForegroundColor Yellow
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
