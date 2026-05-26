#Requires -Version 5.1
<#
.SYNOPSIS
    性能配置检查脚本
.DESCRIPTION
    检查系统性能配置、资源使用、数据库连接池、缓存配置等
#>

param([string]$ReportPath = "")

$ErrorActionPreference = "Continue"
$results = @{
    timestamp = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
    category = "performance"
    status = "PASS"
    checks = @()
    errors = @()
}

function Add-CheckResult { param($Name, $Status, $Message)
    $results.checks += [PSCustomObject]@{ name=$Name; status=$Status; message=$Message }
    if ($Status -eq "FAIL") { $results.status = "FAIL"; $results.errors += $Message }
}

Write-Host "=== 性能配置检查 ===" -ForegroundColor Cyan

# 1. 检查CPU/内存使用
$cpu = Get-WmiObject Win32_Processor | Measure-Object -Property LoadPercentage -Average
$mem = Get-WmiObject Win32_OperatingSystem
$memPercent = ($mem.TotalVisibleMemorySize - $mem.FreePhysicalMemory) / $mem.TotalVisibleMemorySize * 100

Add-CheckResult -Name "CPU使用率" -Status $(if($cpu.Average<80){'PASS'}else{'WARN'}) -Message "$([math]::Round($cpu.Average,1))%"
Add-CheckResult -Name "内存使用率" -Status $(if($memPercent<85){'PASS'}else{'WARN'}) -Message "$([math]::Round($memPercent,1))%"

# 2. 检查数据库连接池配置
$appConfig = "I:\AI-Ready\backend\config\application.yml"
if (Test-Path $appConfig) {
    $content = Get-Content $appConfig -Raw
    if ($content -match "maximum-pool-size.*(\d+)") {
        $poolSize = [int]$Matches[1]
        Add-CheckResult -Name "数据库连接池大小" -Status "PASS" -Message "最大连接数: $poolSize"
    }
    if ($content -match "minimum-idle.*(\d+)") {
        $minIdle = [int]$Matches[1]
        Add-CheckResult -Name "最小空闲连接" -Status "PASS" -Message "最小空闲: $minIdle"
    }
}

# 3. 检查Redis配置
$redisConfig = "I:\AI-Ready\backend\config\redis.yml"
if (Test-Path $redisConfig) {
    $content = Get-Content $redisConfig -Raw
    if ($content -match "timeout.*(\d+)") {
        Add-CheckResult -Name "Redis超时配置" -Status "PASS" -Message "超时: $($Matches[1])ms"
    }
    if ($content -match "pool.*max-active.*(\d+)") {
        Add-CheckResult -Name "Redis连接池" -Status "PASS" -Message "最大活跃: $($Matches[1])"
    }
}

# 4. 检查磁盘I/O性能
$drive = Get-PSDrive -Name "I" -ErrorAction SilentlyContinue
if ($drive) {
    $freeGB = [math]::Round($drive.Free / 1GB, 2)
    Add-CheckResult -Name "磁盘可用空间(I盘)" -Status $(if($freeGB>10){'PASS'}else{'WARN'}) -Message "$freeGB GB"
}

# 5. 检查Java进程内存
$javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue
if ($javaProcesses) {
    foreach ($proc in $javaProcesses | Select-Object -First 3) {
        $memMB = [math]::Round($proc.WorkingSet64 / 1MB, 1)
        Add-CheckResult -Name "Java进程内存($($proc.Id))" -Status $(if($memMB<2048){'PASS'}else{'WARN'}) -Message "$memMB MB"
    }
}

# 6. 检查网络延迟
$pingResult = Test-Connection -ComputerName localhost -Count 1 -ErrorAction SilentlyContinue
if ($pingResult) {
    Add-CheckResult -Name "本地网络延迟" -Status "PASS" -Message "$($pingResult.ResponseTime)ms"
}

# 输出结果
Write-Host "`n检查结果:" -ForegroundColor Cyan
foreach ($check in $results.checks) {
    $color = switch ($check.status) { "PASS" { "Green" } "FAIL" { "Red" } default { "Yellow" } }
    Write-Host "  [$($check.status)] $($check.name): $($check.message)" -ForegroundColor $color
}

if ($ReportPath) { $results | ConvertTo-Json -Depth 3 | Out-File -FilePath $ReportPath -Encoding UTF8 }
if ($results.status -eq "FAIL") { exit 1 } else { exit 0 }