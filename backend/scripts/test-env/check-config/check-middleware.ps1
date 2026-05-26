#Requires -Version 5.1
<#
.SYNOPSIS
    中间件配置检查脚本
.DESCRIPTION
    检查Redis、RabbitMQ/Kafka、Nginx等中间件的配置和运行状态
#>

param(
    [string]$ConfigDir = "I:\AI-Ready\infra",
    [string]$ReportPath = ""
)

$ErrorActionPreference = "Continue"
$results = @{
    timestamp = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
    category = "middleware"
    status = "PASS"
    checks = @()
    errors = @()
}

function Add-CheckResult {
    param($Name, $Status, $Message)
    $results.checks += [PSCustomObject]@{ name=$Name; status=$Status; message=$Message }
    if ($Status -eq "FAIL") { $results.status = "FAIL"; $results.errors += $Message }
}

Write-Host "=== 中间件配置检查 ===" -ForegroundColor Cyan

# 1. Redis检查
$redisService = Get-Service -Name "redis*" -ErrorAction SilentlyContinue
if ($redisService) {
    if ($redisService.Status -eq "Running") {
        Add-CheckResult -Name "Redis服务" -Status "PASS" -Message "Redis运行中"
    } else {
        Add-CheckResult -Name "Redis服务" -Status "FAIL" -Message "Redis未运行: $($redisService.Status)"
    }
} else {
    Add-CheckResult -Name "Redis服务" -Status "FAIL" -Message "未找到Redis服务"
}

# 检查Redis端口
$redisPort = Get-NetTCPConnection -LocalPort 6379 -ErrorAction SilentlyContinue | Select-Object -First 1
if ($redisPort) {
    Add-CheckResult -Name "Redis端口(6379)" -Status "PASS" -Message "端口正在监听"
} else {
    Add-CheckResult -Name "Redis端口(6379)" -Status "FAIL" -Message "端口未监听"
}

# 2. 检查Nginx/反向代理
$nginxService = Get-Service -Name "nginx*" -ErrorAction SilentlyContinue
$nginxPath = "C:\nginx\nginx.exe"
if ($nginxService) {
    if ($nginxService.Status -eq "Running") {
        Add-CheckResult -Name "Nginx服务" -Status "PASS" -Message "Nginx运行中"
    } else {
        Add-CheckResult -Name "Nginx服务" -Status "FAIL" -Message "Nginx未运行"
    }
} elseif (Test-Path $nginxPath) {
    $nginxProcess = Get-Process -Name "nginx" -ErrorAction SilentlyContinue
    if ($nginxProcess) {
        Add-CheckResult -Name "Nginx进程" -Status "PASS" -Message "Nginx进程运行中"
    } else {
        Add-CheckResult -Name "Nginx进程" -Status "FAIL" -Message "Nginx未启动"
    }
} else {
    Add-CheckResult -Name "Nginx" -Status "FAIL" -Message "未找到Nginx安装"
}

# 检查80/443端口
foreach ($port in @(80, 443)) {
    $listener = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($listener) {
        Add-CheckResult -Name "Web端口($port)" -Status "PASS" -Message "端口 $port 正在监听"
    } else {
        Add-CheckResult -Name "Web端口($port)" -Status "FAIL" -Message "端口 $port 未监听"
    }
}

# 3. 检查消息队列 (RabbitMQ)
$rabbitService = Get-Service -Name "rabbitmq*" -ErrorAction SilentlyContinue
if ($rabbitService) {
    if ($rabbitService.Status -eq "Running") {
        Add-CheckResult -Name "RabbitMQ服务" -Status "PASS" -Message "RabbitMQ运行中"
    } else {
        Add-CheckResult -Name "RabbitMQ服务" -Status "FAIL" -Message "RabbitMQ未运行"
    }
} else {
    Add-CheckResult -Name "RabbitMQ服务" -Status "FAIL" -Message "未找到RabbitMQ服务"
}

# 检查5672端口 (AMQP)
$amqpPort = Get-NetTCPConnection -LocalPort 5672 -ErrorAction SilentlyContinue | Select-Object -First 1
if ($amqpPort) {
    Add-CheckResult -Name "AMQP端口(5672)" -Status "PASS" -Message "AMQP端口监听中"
} else {
    Add-CheckResult -Name "AMQP端口(5672)" -Status "FAIL" -Message "AMQP端口未监听"
}

# 4. 检查配置目录结构
$expectedDirs = @("docker", "network", "monitoring", "logging")
$infraPath = Join-Path $ConfigDir "docker"
if (Test-Path $infraPath) {
    Add-CheckResult -Name "Docker配置目录" -Status "PASS" -Message "目录存在: $infraPath"
} else {
    Add-CheckResult -Name "Docker配置目录" -Status "FAIL" -Message "目录不存在: $infraPath"
}

# 5. 检查Docker是否运行（如果在Windows上使用Docker Desktop）
$dockerProcess = Get-Process -Name "Docker Desktop" -ErrorAction SilentlyContinue
if ($dockerProcess) {
    Add-CheckResult -Name "Docker Desktop" -Status "PASS" -Message "Docker Desktop运行中"
} else {
    Add-CheckResult -Name "Docker Desktop" -Status "FAIL" -Message "Docker Desktop未运行"
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
