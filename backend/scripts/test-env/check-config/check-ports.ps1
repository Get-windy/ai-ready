#Requires -Version 5.1
<#
.SYNOPSIS
    端口监听检查脚本
.DESCRIPTION
    检查测试环境各服务端口是否正常监听
#>

param(
    [hashtable]$ExpectedPorts = @{
        "PostgreSQL" = 5432
        "Redis" = 6379
        "HTTP" = 80
        "HTTPS" = 443
        "AMQP" = 5672
        "RabbitMQ-Mgmt" = 15672
        "Spring-Boot" = 8080
        "Frontend-Dev" = 3000
    },
    [string]$ReportPath = ""
)

$ErrorActionPreference = "Continue"
$results = @{
    timestamp = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
    category = "ports"
    status = "PASS"
    checks = @()
    errors = @()
}

function Add-CheckResult {
    param($Name, $Status, $Message)
    $results.checks += [PSCustomObject]@{ name=$Name; status=$Status; message=$Message }
    if ($Status -eq "FAIL") { $results.status = "FAIL"; $results.errors += $Message }
}

Write-Host "=== 端口监听检查 ===" -ForegroundColor Cyan

# 获取所有TCP连接
$tcpConnections = Get-NetTCPConnection -ErrorAction SilentlyContinue

foreach ($service in $ExpectedPorts.Keys) {
    $port = $ExpectedPorts[$service]
    $listeners = $tcpConnections | Where-Object { $_.LocalPort -eq $port -and $_.State -eq "Listen" }
    
    if ($listeners) {
        $count = ($listeners | Measure-Object).Count
        Add-CheckResult -Name "$service ($port)" -Status "PASS" -Message "$count 个监听进程"
    } else {
        # 对于非关键端口使用WARN
        $status = if ($service -in @("PostgreSQL", "Redis", "HTTP", "Spring-Boot")) { "FAIL" } else { "WARN" }
        Add-CheckResult -Name "$service ($port)" -Status $status -Message "端口未监听"
    }
}

# 检查端口冲突（同一端口多个监听者）
$portGroups = $tcpConnections | Where-Object { $_.State -eq "Listen" } | Group-Object -Property LocalPort
foreach ($group in $portGroups) {
    if ($group.Count -gt 1 -and $group.Name -in $ExpectedPorts.Values) {
        Add-CheckResult -Name "端口冲突 ($($group.Name))" -Status "WARN" -Message "$($group.Count) 个进程监听同一端口"
    }
}

# 输出结果
Write-Host "`n检查结果:" -ForegroundColor Cyan
foreach ($check in $results.checks) {
    $color = switch ($check.status) {
        "PASS"  { "Green" }
        "FAIL"  { "Red" }
        default { "Yellow" }
    }
    Write-Host "  [$($check.status)] $($check.name): $($check.message)" -ForegroundColor $color
}

if ($ReportPath) {
    $results | ConvertTo-Json -Depth 3 | Out-File -FilePath $ReportPath -Encoding UTF8
    Write-Host "`n报告已保存: $ReportPath" -ForegroundColor Cyan
}

if ($results.status -eq "FAIL") { exit 1 } else { exit 0 }
