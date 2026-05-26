#Requires -Version 5.1
<#
.SYNOPSIS
    安全配置检查脚本
.DESCRIPTION
    检查数据库连接安全、密码策略、认证配置等安全相关设置
#>

param([string]$ReportPath = "")

$ErrorActionPreference = "Continue"
$results = @{
    timestamp = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
    category = "security"
    status = "PASS"
    checks = @()
    errors = @()
}

function Add-CheckResult { param($Name, $Status, $Message)
    $results.checks += [PSCustomObject]@{ name=$Name; status=$Status; message=$Message }
    if ($Status -eq "FAIL") { $results.status = "FAIL"; $results.errors += $Message }
}

Write-Host "=== 安全配置检查 ===" -ForegroundColor Cyan

# 1. 检查密码策略
$secpol = secedit /export /areas SECURITYPOLICY /cfg "$env:TEMP\secpol.cfg" 2>&1 | Out-Null
if (Test-Path "$env:TEMP\secpol.cfg") {
    $policy = Get-Content "$env:TEMP\secpol.cfg" -Raw
    if ($policy -match "PasswordComplexity\s*=\s*1") {
        Add-CheckResult -Name "密码复杂度策略" -Status "PASS" -Message "密码复杂度已启用"
    } else {
        Add-CheckResult -Name "密码复杂度策略" -Status "WARN" -Message "密码复杂度未启用"
    }
    Remove-Item "$env:TEMP\secpol.cfg" -Force
}

# 2. 检查数据库连接加密
$configFiles = @(
    "I:\AI-Ready\backend\config\application.yml",
    "I:\AI-Ready\backend\config\application-test.yml"
)
foreach ($file in $configFiles) {
    if (Test-Path $file) {
        $content = Get-Content $file -Raw
        if ($content -match "ssl.*true|sslmode.*require|useSSL.*true") {
            Add-CheckResult -Name "数据库SSL配置($file)" -Status "PASS" -Message "SSL连接已配置"
        } else {
            Add-CheckResult -Name "数据库SSL配置($file)" -Status "WARN" -Message "未启用SSL连接"
        }
    }
}

# 3. 检查JWT/认证配置
$appConfig = "I:\AI-Ready\backend\config\application.yml"
if (Test-Path $appConfig) {
    $content = Get-Content $appConfig -Raw
    if ($content -match "jwt.*secret|sa-token") {
        Add-CheckResult -Name "JWT/认证配置" -Status "PASS" -Message "发现JWT/Sa-Token配置"
        # 检查是否硬编码secret
        if ($content -match "secret.*=.*[a-zA-Z0-9]{20,}") {
            Add-CheckResult -Name "JWT Secret安全" -Status "WARN" -Message "可能硬编码secret，建议使用环境变量"
        }
    } else {
        Add-CheckResult -Name "JWT/认证配置" -Status "FAIL" -Message "未找到认证配置"
    }
}

# 4. 检查API鉴权
$backendDir = "I:\AI-Ready\backend"
if (Test-Path $backendDir) {
    $hasAuth = Get-ChildItem -Path $backendDir -Filter "*.java" -Recurse -ErrorAction SilentlyContinue | 
        Select-Object -First 30 | ForEach-Object { 
            $c = Get-Content $_.FullName -Raw -ErrorAction SilentlyContinue
            if ($c -match "@SaCheckLogin|@PreAuthorize|@Secured") { return $true }
        }
    if ($hasAuth) {
        Add-CheckResult -Name "API鉴权注解" -Status "PASS" -Message "发现鉴权注解配置"
    } else {
        Add-CheckResult -Name "API鉴权注解" -Status "WARN" -Message "未发现API鉴权注解"
    }
}

# 5. 检查端口暴露风险
$publicPorts = @(80, 443, 8080, 3000)
$internalPorts = @(5432, 6379, 5672, 15672)
$listeners = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue

$exposedInternal = $internalPorts | Where-Object { 
    $port = $_; $listeners | Where-Object { $_.LocalPort -eq $port }
}
if ($exposedInternal) {
    Add-CheckResult -Name "内部端口暴露" -Status "WARN" -Message "内部服务端口可能暴露: $($exposedInternal -join ',')"
}

# 6. 检查防火墙规则
$blockRules = Get-NetFirewallRule -Enabled True -Action Block -ErrorAction SilentlyContinue | Measure-Object
if ($blockRules.Count -gt 10) {
    Add-CheckResult -Name "防火墙阻断规则" -Status "PASS" -Message "$($blockRules.Count) 条阻断规则已启用"
} else {
    Add-CheckResult -Name "防火墙阻断规则" -Status "WARN" -Message "阻断规则较少($($blockRules.Count))"
}

# 输出结果
Write-Host "`n检查结果:" -ForegroundColor Cyan
foreach ($check in $results.checks) {
    $color = switch ($check.status) { "PASS" { "Green" } "FAIL" { "Red" } default { "Yellow" } }
    Write-Host "  [$($check.status)] $($check.name): $($check.message)" -ForegroundColor $color
}

if ($ReportPath) { $results | ConvertTo-Json -Depth 3 | Out-File -FilePath $ReportPath -Encoding UTF8 }
if ($results.status -eq "FAIL") { exit 1 } else { exit 0 }