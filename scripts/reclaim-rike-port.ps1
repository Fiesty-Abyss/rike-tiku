[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateSet(8080, 8081)]
    [int]$Port
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..')).TrimEnd('\')

function Test-RikePortOwner {
    param(
        [int]$TargetPort,
        [string]$ProcessName,
        [string]$CommandLine
    )

    if ([string]::IsNullOrWhiteSpace($CommandLine)) {
        return $false
    }

    $normalizedCommand = $CommandLine.Replace('/', '\')
    if ($normalizedCommand.IndexOf($repositoryRoot, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
        return $false
    }

    if ($TargetPort -eq 8080) {
        return $ProcessName -ieq 'node.exe' -and
            ($normalizedCommand -match '(?i)rike-tiku-frontend\\node_modules\\vite' -or
             $normalizedCommand -match '(?i)rike-tiku-frontend.*\\vite(?:\.js)?(?:\s|"|$)')
    }

    return $ProcessName -ieq 'java.exe' -and
        ($normalizedCommand -match '(?i)com\.neu\.riketiku\.RikeTikuBackendApplication' -or
         $normalizedCommand -match '(?i)rike-tiku-backend\\target\\rike-tiku-backend-[^\s"]*\.jar')
}

$listeners = @(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue |
    Sort-Object OwningProcess -Unique)

if ($listeners.Count -eq 0) {
    Write-Host "RIKE 端口 $Port 当前未占用。"
    exit 0
}

foreach ($listener in $listeners) {
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)"
    if (-not (Test-RikePortOwner -TargetPort $Port -ProcessName $process.Name -CommandLine $process.CommandLine)) {
        Write-Error "端口 $Port 被非 RIKE 进程占用：$($process.Name)（PID $($process.ProcessId)）。脚本不会停止该进程，请人工核对后处理。" -ErrorAction Continue
        exit 2
    }

    Write-Host "正在停止 RIKE 旧实例：$($process.Name)（PID $($process.ProcessId)），端口 $Port。"
    Stop-Process -Id $process.ProcessId
}

$deadline = (Get-Date).AddSeconds(10)
do {
    Start-Sleep -Milliseconds 250
    $remaining = @(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
} while ($remaining.Count -gt 0 -and (Get-Date) -lt $deadline)

if ($remaining.Count -gt 0) {
    Write-Error "RIKE 旧实例收到停止请求后仍占用端口 $Port。请在对应 IDE Run 窗口停止实例后重试。" -ErrorAction Continue
    exit 3
}

Write-Host "RIKE 端口 $Port 已安全释放。"
