$ErrorActionPreference = "Stop"

$variableName = "RIKE_TIKU_DB_PASSWORD"
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
Write-Host "请完全退出并重新打开 IDEA，然后运行 RikeTikuBackendApplication。"
