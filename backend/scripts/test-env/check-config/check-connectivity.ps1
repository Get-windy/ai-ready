#Requires -Version 5.1
<#
.SYNOPSIS
    依赖服务连通性检查脚本
.DESCRIPTION
    检查测试环境各依赖服务之间的网络连通性
#>

param(
    [string]$ReportPath = ""
)

$ErrorActionPreference = "Continue"
$results = @{
    timestamp = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
    category = "connectivity"
    status = "PASS"
    checks = @()
    errors = @()
}

function Add-CheckResult {
    param($Name, $Status, $Message)
    $results.checks += [PSCustomObject]@{ name=$Name; status=$Status; message=$Message }
    if ($Status -eq "FAIL") { $results.status = "FAIL"; $results.errors += $Message }
}

function Test-TcpConnection {
    param($HostName, $Port, $TimeoutMs = 3000)
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $connection = $client.BeginConnect($HostName, $Port, $null, $null)
        $success = $connection.AsyncWaitHandle.WaitOne($TimeoutMs, $false)
        if ($success -and $client.Connected) {
            $client.Close()
            return $true
        }
        $client.Close()
        return $false
    } catch {
        return $false
    }
}

Write-Host "=== 依赖服务连通性检查 ===" -ForegroundColor Cyan

# 定义要检查的服务端点
$endpoints = @(
    @{Name="本地PostgreSQL"; Host="localhost"; Port=5432; Critical=$true},
    @{Name="本地Redis"; Host="localhost"; Port=6379; Critical=$true},
    @{Name="本地HTTP"; Host="localhost"; Port=80; Critical=$false},
    @{Name="本地HTTPS"; Host="localhost"; Port=443; Critical=$false},
    @{Name="本地Spring-Boot"; Host="localhost"; Port=8080; Critical=$true},
    @{Name="本地RabbitMQ"; Host="localhost"; Port=5672; Critical=$false},
    @{Name="本地RabbitMQ-Mgmt"; Host="localhost"; Port=15672; Critical=$false},
    @{Name="外部Internet"; Host="8.8.8.8"; Port=53; Critical=$false}
)

foreach ($ep in $endpoints) {
    $reachable = Test-TcpConnection -HostName $ep.Host -Port $ep.Port
    if ($reachable) {
        Add-CheckResult -Name $ep.Name -Status "PASS" -Message "$($ep.Host):$($ep.Port) 可连通"
    } else {
        $status = if ($ep.Critical) { "FAIL" } else { "WARN" }
        Add-CheckResult -Name $ep.Name -Status $status -Message "$($ep.Host):$($ep.Port) 无法连通"
    }
}

# 测试DNS解析
$dnsTests = @("localhost", "baidu.com")
foreach ($dns in $dnsTests) {
    try {
        $resolved = [System.Net.Dns]::GetHostAddresses($dns)
        if ($resolved) {
            Add-CheckResult -Name "DNS解析: $dns" -Status "PASS" -Message "解析到: $($resolved[0].IPAddressToString)"
        }
    } catch {
        Add-CheckResult -Name "DNS解析: $dns" -Status "WARN" -Message "解析失败: $_"
    }
}

# 检查防火墙状态
$firewallProfiles = Get-NetFirewallProfile -ErrorAction SilentlyContinue
$anyEnabled = $firewallProfiles | Where-Object { $_.Enabled -eq "True" }
if ($anyEnabled) {
    $profileNames = ($anyEnabled | ForEach-Object { $_.Name }) -join ", "
    Add-CheckResult -Name "Windows防火墙" -Status "WARN" -Message "防火墙已启用: $profileNames"
} else {
    Add-CheckResult -Name "Windows防火墙" -Status "PASS" -Message "防火墙未启用"
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
