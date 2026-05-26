#Requires -Version 5.1
<#
.SYNOPSIS
    服务启动检查脚本
.DESCRIPTION
    检查测试环境各服务是否正常启动和运行
#>

param(
    [string[]]$RequiredServices = @("postgresql", "redis", "nginx", "rabbitmq"),
    [string]$ReportPath = ""
)

$ErrorActionPreference = "Continue"
$results = @{
    timestamp = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
    category = "services"
    status = "PASS"
    checks = @()
    errors = @()
}

function Add-CheckResult {
    param($Name, $Status, $Message)
    $results.checks += [PSCustomObject]@{ name=$Name; status=$Status; message=$Message }
    if ($Status -eq "FAIL") { $results.status = "FAIL"; $results.errors += $Message }
}

Write-Host "=== 服务启动检查 ===" -ForegroundColor Cyan

# 检查Windows服务
$allServices = Get-Service -ErrorAction SilentlyContinue
foreach ($svcName in $RequiredServices) {
    $svc = $allServices | Where-Object { $_.Name -like "*$svcName*" } | Select-Object -First 1
    if ($svc) {
        if ($svc.Status -eq "Running") {
            Add-CheckResult -Name "服务: $($svc.Name)" -Status "PASS" -Message "运行中"
        } else {
            Add-CheckResult -Name "服务: $($svc.Name)" -Status "FAIL" -Message "状态: $($svc.Status)"
        }
    } else {
        Add-CheckResult -Name "服务: $svcName" -Status "FAIL" -Message "未安装"
    }
}

# 检查Java/Spring Boot进程
$javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue
if ($javaProcesses) {
    foreach ($proc in $javaProcesses) {
        try {
            $cmdLine = (Get-CimInstance Win32_Process -Filter "ProcessId=$($proc.Id)" -ErrorAction SilentlyContinue).CommandLine
            if ($cmdLine -match "spring|boot|application") {
                Add-CheckResult -Name "Java进程: $($proc.Id)" -Status "PASS" -Message "Spring Boot应用运行中 (内存: $([math]::Round($proc.WorkingSet64/1MB,1)) MB)"
            } else {
                Add-CheckResult -Name "Java进程: $($proc.Id)" -Status "PASS" -Message "Java应用运行中"
            }
        } catch {
            Add-CheckResult -Name "Java进程: $($proc.Id)" -Status "PASS" -Message "Java进程运行中"
        }
    }
} else {
    Add-CheckResult -Name "Java进程" -Status "FAIL" -Message "未找到Java进程"
}

# 检查Node.js进程（前端）
$nodeProcesses = Get-Process -Name "node" -ErrorAction SilentlyContinue
if ($nodeProcesses) {
    Add-CheckResult -Name "Node.js进程" -Status "PASS" -Message "Node.js运行中 ($($nodeProcesses.Count)个进程)"
} else {
    Add-CheckResult -Name "Node.js进程" -Status "WARN" -Message "未找到Node.js进程"
}

# 检查Docker Desktop（如果使用）
$dockerProcess = Get-Process -Name "Docker Desktop" -ErrorAction SilentlyContinue
if ($dockerProcess) {
    Add-CheckResult -Name "Docker Desktop" -Status "PASS" -Message "Docker Desktop运行中"
} else {
    Add-CheckResult -Name "Docker Desktop" -Status "WARN" -Message "Docker Desktop未运行"
}

# 输出结果
Write-Host "`n检查结果:" -ForegroundColor Cyan
foreach ($check in $results.checks) {
    $color = if ($check.status -eq "PASS") { "Green" } else { "Red" }
    Write-Host "  [$($check.status)] $($check.name): $($check.message)" -ForegroundColor $color
}

if ($ReportPath) {
    $results | ConvertTo-Json -Depth 3 | Out-File -FilePath $ReportPath -Encoding UTF8
    Write-Host "`n报告已保存: $ReportPath" -ForegroundColor Cyan
}

if ($results.status -eq "FAIL") { exit 1 } else { exit 0 }
