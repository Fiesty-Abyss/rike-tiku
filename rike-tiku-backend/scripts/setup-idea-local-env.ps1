$ErrorActionPreference = "Stop"

$variableName = "RIKE_TIKU_DB_PASSWORD"
$jwtVariableName = "RIKE_TIKU_JWT_SECRET"
$securePassword = Read-Host "请输入本机 MySQL 密码（输入内容不会显示）" -AsSecureString

if ($securePassword.Length -eq 0) {
    throw "密码不能为空，未修改 Windows 用户环境变量。"
}

$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)

try {
    $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    [Environment]::SetEnvironmentVariable($variableName, $plainPassword, "User")
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    $plainPassword = $null
}

Write-Host "已设置 Windows 用户环境变量 $variableName。"

if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($jwtVariableName, "User"))) {
    $randomBytes = [byte[]]::new(48)
    [Security.Cryptography.RandomNumberGenerator]::Fill($randomBytes)
    $jwtSecret = [Convert]::ToBase64String($randomBytes)
    [Environment]::SetEnvironmentVariable($jwtVariableName, $jwtSecret, "User")
    $jwtSecret = $null
    [Array]::Clear($randomBytes, 0, $randomBytes.Length)
    Write-Host "已生成并设置本机 JWT 密钥环境变量 $jwtVariableName。"
}

Write-Host "请完全退出并重新打开 IDEA，然后运行 RikeTikuBackendApplication。"
