#Requires -Version 5.1
<#
.SYNOPSIS
    应用配置校验脚本
.DESCRIPTION
    检查AI-Ready应用配置文件的正确性和完整性
#>

param(
    [string]$AppConfigDir = "I:\AI-Ready\backend\config",
    [string]$ReportPath = ""
)

$ErrorActionPreference = "Continue"
$results = @{
    timestamp = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
    category = "application"
    status = "PASS"
    checks = @()
    errors = @()
}

function Add-CheckResult {
    param($Name, $Status, $Message)
    $results.checks += [PSCustomObject]@{ name=$Name; status=$Status; message=$Message }
    if ($Status -eq "FAIL") { $results.status = "FAIL"; $results.errors += $Message }
}

Write-Host "=== 应用配置校验 ===" -ForegroundColor Cyan

# 1. 检查应用配置目录
if (Test-Path $AppConfigDir) {
    Add-CheckResult -Name "配置目录" -Status "PASS" -Message "配置目录存在: $AppConfigDir"
} else {
    Add-CheckResult -Name "配置目录" -Status "FAIL" -Message "配置目录不存在: $AppConfigDir"
}

# 2. 检查关键配置文件
$configFiles = @(
    @{Path="$AppConfigDir\application.yml"; Required=$true},
    @{Path="$AppConfigDir\application-dev.yml"; Required=$false},
    @{Path="$AppConfigDir\application-test.yml"; Required=$true},
    @{Path="$AppConfigDir\database.yml"; Required=$true},
    @{Path="$AppConfigDir\redis.yml"; Required=$true}
)

foreach ($file in $configFiles) {
    if (Test-Path $file.Path) {
        $content = Get-Content $file.Path -Raw -ErrorAction SilentlyContinue
        $size = (Get-Item $file.Path).Length
        Add-CheckResult -Name "配置: $(Split-Path $file.Path -Leaf)" -Status "PASS" -Message "文件存在 (${size} bytes)"
        
        # 检查关键配置项
        if ($content -match "server:\s*\n\s*port:\s*(\d+)") {
            $serverPort = $Matches[1]
            Add-CheckResult -Name "服务端口配置" -Status "PASS" -Message "配置端口: $serverPort"
        }
    } else {
        $status = if ($file.Required) { "FAIL" } else { "WARN" }
        Add-CheckResult -Name "配置: $(Split-Path $file.Path -Leaf)" -Status $status -Message "文件不存在"
    }
}

# 3. 检查日志配置
$logConfigs = @(
    "I:\AI-Ready\infra\logging\logback-spring.xml",
    "I:\AI-Ready\infra\logging\log4j2.xml"
)
$foundLogConfig = $false
foreach ($logConfig in $logConfigs) {
    if (Test-Path $logConfig) {
        $foundLogConfig = $true
        Add-CheckResult -Name "日志配置" -Status "PASS" -Message "日志配置存在: $(Split-Path $logConfig -Leaf)"
    }
}
if (-not $foundLogConfig) {
    Add-CheckResult -Name "日志配置" -Status "FAIL" -Message "未找到日志配置文件"
}

# 4. 检查Spring Boot应用属性
$appYml = "$AppConfigDir\application.yml"
if (Test-Path $appYml) {
    $content = Get-Content $appYml -Raw
    $requiredProps = @("spring", "server", "logging")
    foreach ($prop in $requiredProps) {
        if ($content -match $prop) {
            Add-CheckResult -Name "属性: $prop" -Status "PASS" -Message "$prop 配置节存在"
        } else {
            Add-CheckResult -Name "属性: $prop" -Status "FAIL" -Message "$prop 配置节缺失"
        }
    }
}

# 5. 检查API文档配置
$swaggerFound = $false
$backendDir = "I:\AI-Ready\backend"
if (Test-Path $backendDir) {
    $javaFiles = Get-ChildItem -Path $backendDir -Filter "*.java" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 50
    foreach ($file in $javaFiles) {
        $content = Get-Content $file.FullName -Raw -ErrorAction SilentlyContinue
        if ($content -match "@OpenAPIDefinition|@SwaggerDefinition|springdoc") {
            $swaggerFound = $true
            break
        }
    }
}
if ($swaggerFound) {
    Add-CheckResult -Name "API文档配置" -Status "PASS" -Message "发现Swagger/OpenAPI配置"
} else {
    Add-CheckResult -Name "API文档配置" -Status "WARN" -Message "未找到API文档注解"
}

# 6. 检查健康检查端点
if (Test-Path $appYml) {
    $content = Get-Content $appYml -Raw
    if ($content -match "management|actuator|health") {
        Add-CheckResult -Name "健康检查配置" -Status "PASS" -Message "发现Actuator/Health配置"
    } else {
        Add-CheckResult -Name "健康检查配置" -Status "WARN" -Message "未找到健康检查配置"
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
