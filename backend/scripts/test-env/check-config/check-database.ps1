#Requires -Version 5.1
<#
.SYNOPSIS
    数据库配置检查脚本
.DESCRIPTION
    检查PostgreSQL数据库配置的正确性和可连接性
#>

param(
    [string]$ConfigPath = "I:\AI-Ready\backend\config\database.yml",
    [string]$PgHost = "localhost",
    [int]$PgPort = 5432,
    [string]$PgUser = "postgres",
    [string]$PgPassword = $env:PGPASSWORD,
    [string]$ReportPath = ""
)

$ErrorActionPreference = "Continue"
$results = @{
    timestamp = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
    category = "database"
    status = "PASS"
    checks = @()
    errors = @()
}

function Add-CheckResult {
    param($Name, $Status, $Message)
    $results.checks += [PSCustomObject]@{
        name = $Name
        status = $Status
        message = $Message
    }
    if ($Status -eq "FAIL") {
        $results.status = "FAIL"
        $results.errors += $Message
    }
}

Write-Host "=== 数据库配置检查 ===" -ForegroundColor Cyan

# 1. 检查配置文件存在性
if (Test-Path $ConfigPath) {
    Add-CheckResult -Name "配置文件存在性" -Status "PASS" -Message "配置文件存在: $ConfigPath"
    # 解析YAML（简化检查）
    $configContent = Get-Content $ConfigPath -Raw -ErrorAction SilentlyContinue
    if ($configContent -match "host:\s*(.+)") {
        $configHost = $Matches[1].Trim()
        Add-CheckResult -Name "配置主机地址" -Status "PASS" -Message "配置主机: $configHost"
    }
    if ($configContent -match "port:\s*(\d+)") {
        $configPort = [int]$Matches[1]
        Add-CheckResult -Name "配置端口" -Status "PASS" -Message "配置端口: $configPort"
    }
} else {
    Add-CheckResult -Name "配置文件存在性" -Status "FAIL" -Message "配置文件不存在: $ConfigPath"
}

# 2. 检查PostgreSQL服务
$pgService = Get-Service -Name "postgresql*" -ErrorAction SilentlyContinue
if ($pgService) {
    $serviceStatus = $pgService.Status
    if ($serviceStatus -eq "Running") {
        Add-CheckResult -Name "PostgreSQL服务" -Status "PASS" -Message "服务运行中: $($pgService.Name)"
    } else {
        Add-CheckResult -Name "PostgreSQL服务" -Status "FAIL" -Message "服务未运行: $($pgService.Name) 状态=$serviceStatus"
    }
} else {
    Add-CheckResult -Name "PostgreSQL服务" -Status "FAIL" -Message "未找到PostgreSQL服务"
}

# 3. 检查端口监听
$portListener = Get-NetTCPConnection -LocalPort $PgPort -ErrorAction SilentlyContinue | Select-Object -First 1
if ($portListener) {
    Add-CheckResult -Name "端口监听" -Status "PASS" -Message "端口 $PgPort 正在监听"
} else {
    Add-CheckResult -Name "端口监听" -Status "FAIL" -Message "端口 $PgPort 未监听"
}

# 4. 测试数据库连接（如果psql可用）
$psqlPath = Get-Command psql -ErrorAction SilentlyContinue
if ($psqlPath) {
    $env:PGPASSWORD = $PgPassword
    $testQuery = "SELECT 1 as connection_test;"
    try {
        $output = & psql -h $PgHost -p $PgPort -U $PgUser -c $testQuery -t -A 2>$null
        if ($output -match "1") {
            Add-CheckResult -Name "数据库连接" -Status "PASS" -Message "成功连接到 $PgHost`:$PgPort"
        } else {
            Add-CheckResult -Name "数据库连接" -Status "FAIL" -Message "连接测试查询失败"
        }
    } catch {
        Add-CheckResult -Name "数据库连接" -Status "FAIL" -Message "连接失败: $_"
    }
} else {
    Add-CheckResult -Name "数据库连接" -Status "FAIL" -Message "psql客户端未安装"
}

# 5. 检查关键数据库存在
$criticalDatabases = @("ai_ready", "ai_ready_test", "postgres")
$foundDbs = @()
if ($psqlPath) {
    try {
        $dbList = & psql -h $PgHost -p $PgPort -U $PgUser -c "\l" -t -A 2>$null
        foreach ($db in $criticalDatabases) {
            if ($dbList -match $db) {
                $foundDbs += $db
            }
        }
        if ($foundDbs.Count -gt 0) {
            Add-CheckResult -Name "关键数据库" -Status "PASS" -Message "找到数据库: $($foundDbs -join ', ')"
        } else {
            Add-CheckResult -Name "关键数据库" -Status "FAIL" -Message "未找到任何关键数据库"
        }
    } catch {
        Add-CheckResult -Name "关键数据库" -Status "FAIL" -Message "无法查询数据库列表"
    }
}

# 6. 检查磁盘空间
$drive = Get-PSDrive -Name (Split-Path -Qualifier $ConfigPath).TrimEnd(':')
if ($drive) {
    $freePercent = ($drive.Free / ($drive.Free + $drive.Used)) * 100
    if ($freePercent -gt 20) {
        Add-CheckResult -Name "磁盘空间" -Status "PASS" -Message "可用空间: $([math]::Round($freePercent,1))%"
    } else {
        Add-CheckResult -Name "磁盘空间" -Status "FAIL" -Message "可用空间不足: $([math]::Round($freePercent,1))%"
    }
}

# 输出结果
Write-Host "`n检查结果:" -ForegroundColor Cyan
foreach ($check in $results.checks) {
    $color = if ($check.status -eq "PASS") { "Green" } else { "Red" }
    Write-Host "  [$($check.status)] $($check.name): $($check.message)" -ForegroundColor $color
}

# 保存报告
if ($ReportPath) {
    $results | ConvertTo-Json -Depth 3 | Out-File -FilePath $ReportPath -Encoding UTF8
    Write-Host "`n报告已保存: $ReportPath" -ForegroundColor Cyan
}

# 返回退出码
if ($results.status -eq "FAIL") {
    exit 1
} else {
    exit 0
}
