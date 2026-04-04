# AI-Ready 服务健康检查脚本
# 作者: devops-engineer
# 日期: 2026-04-01
# 用途: 日常巡检 - 数据库/Redis/API/定时任务状态检查

param(
    [string]$ConfigFile = "health-check-config.json",
    [string]$ReportDir = "reports",
    [bool]$Verbose = $false
)

# 配置参数
$Config = @{
    Database = @{
        Host = "localhost"
        Port = 5432
        Database = "devdb"
        User = "devuser"
    }
    Redis = @{
        Host = "localhost"
        Port = 6379
    }
    API = @{
        BaseUrl = "http://localhost:8080"
        HealthEndpoint = "/actuator/health"
        TimeoutSeconds = 10
    }
    ScheduledTasks = @{
        CheckIntervalMinutes = 5
        MaxDelayedMinutes = 30
    }
}

# 健康检查结果
$HealthResults = @{
    Timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Status = "unknown"
    Checks = @()
    Summary = @{
        Total = 0
        Passed = 0
        Failed = 0
        Warning = 0
    }
}

function Add-CheckResult {
    param($Name, $Status, $Message, $Details)
    
    $HealthResults.Checks += @{
        Name = $Name
        Status = $Status
        Message = $Message
        Details = $Details
        Timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    }
    
    $HealthResults.Summary.Total++
    switch ($Status) {
        "pass" { $HealthResults.Summary.Passed++ }
        "fail" { $HealthResults.Summary.Failed++ }
        "warn" { $HealthResults.Summary.Warning++ }
    }
}

# 1. 数据库连接检查
function Check-Database {
    Write-Host "检查数据库连接..." -ForegroundColor Cyan
    
    try {
        $Port = $Config.Database.Port
        $Host = $Config.Database.Host
        
        # 检查端口是否可连接
        $TcpTest = Test-NetConnection -ComputerName $Host -Port $Port -WarningAction SilentlyContinue
        
        if ($TcpTest.TcpTestSucceeded) {
            Add-CheckResult -Name "数据库连接" -Status "pass" -Message "PostgreSQL端口$Port可访问" -Details @{
                Host = $Host
                Port = $Port
                Database = $Config.Database.Database
            }
            
            # 尝试简单查询（需要psql客户端）
            $PsqlPath = "psql"
            if (Get-Command $PsqlPath -ErrorAction SilentlyContinue) {
                $QueryResult = & $PsqlPath -h $Host -p $Port -U $Config.Database.User -d $Config.Database.Database -c "SELECT 1" 2>&1
                if ($LASTEXITCODE -eq 0) {
                    Add-CheckResult -Name "数据库查询" -Status "pass" -Message "数据库查询正常" -Details @{ Query = "SELECT 1" }
                } else {
                    Add-CheckResult -Name "数据库查询" -Status "fail" -Message "数据库查询失败: $QueryResult" -Details @{ ExitCode = $LASTEXITCODE }
                }
            }
        } else {
            Add-CheckResult -Name "数据库连接" -Status "fail" -Message "PostgreSQL端口$Port不可访问" -Details @{
                Host = $Host
                Port = $Port
                Error = "Connection refused"
            }
        }
    } catch {
        Add-CheckResult -Name "数据库连接" -Status "fail" -Message "数据库检查异常: $_" -Details @{ Exception = $_.Exception.Message }
    }
}

# 2. Redis缓存检查
function Check-Redis {
    Write-Host "检查Redis缓存..." -ForegroundColor Cyan
    
    try {
        $Host = $Config.Redis.Host
        $Port = $Config.Redis.Port
        
        $TcpTest = Test-NetConnection -ComputerName $Host -Port $Port -WarningAction SilentlyContinue
        
        if ($TcpTest.TcpTestSucceeded) {
            Add-CheckResult -Name "Redis连接" -Status "pass" -Message "Redis端口$Port可访问" -Details @{
                Host = $Host
                Port = $Port
            }
            
            # 尝试Redis命令（需要redis-cli）
            $RedisCliPath = "redis-cli"
            if (Get-Command $RedisCliPath -ErrorAction SilentlyContinue) {
                $PingResult = & $RedisCliPath -h $Host -p $Port PING 2>&1
                if ($PingResult -eq "PONG") {
                    Add-CheckResult -Name "Redis响应" -Status "pass" -Message "Redis PING正常" -Details @{ Response = "PONG" }
                    
                    # 检查Redis信息
                    $InfoResult = & $RedisCliPath -h $Host -p $Port INFO server 2>&1
                    $MemoryMatch = [regex]::Match($InfoResult, "used_memory_human:(\S+)")
                    if ($MemoryMatch.Success) {
                        Add-CheckResult -Name "Redis内存" -Status "pass" -Message "Redis内存使用: $($MemoryMatch.Groups[1].Value)" -Details @{ Memory = $MemoryMatch.Groups[1].Value }
                    }
                } else {
                    Add-CheckResult -Name "Redis响应" -Status "fail" -Message "Redis PING失败: $PingResult" -Details @{ Response = $PingResult }
                }
            }
        } else {
            Add-CheckResult -Name "Redis连接" -Status "fail" -Message "Redis端口$Port不可访问" -Details @{
                Host = $Host
                Port = $Port
                Error = "Connection refused"
            }
        }
    } catch {
        Add-CheckResult -Name "Redis连接" -Status "fail" -Message "Redis检查异常: $_" -Details @{ Exception = $_.Exception.Message }
    }
}

# 3. API服务可用性检查
function Check-API {
    Write-Host "检查API服务..." -ForegroundColor Cyan
    
    try {
        $BaseUrl = $Config.API.BaseUrl
        $HealthEndpoint = $Config.API.HealthEndpoint
        $Url = "$BaseUrl$HealthEndpoint"
        
        # 检查端口是否开放
        $ApiPort = 8080
        $TcpTest = Test-NetConnection -ComputerName "localhost" -Port $ApiPort -WarningAction SilentlyContinue
        
        if (-not $TcpTest.TcpTestSucceeded) {
            Add-CheckResult -Name "API端口" -Status "fail" -Message "API端口$ApiPort不可访问" -Details @{
                Port = $ApiPort
                Status = "not listening"
            }
            return
        }
        
        Add-CheckResult -Name "API端口" -Status "pass" -Message "API端口$ApiPort可访问" -Details @{ Port = $ApiPort }
        
        # 尝试HTTP健康检查
        try {
            $Response = Invoke-WebRequest -Uri $Url -TimeoutSec $Config.API.TimeoutSeconds -UseBasicParsing
            
            if ($Response.StatusCode -eq 200) {
                $HealthData = $Response.Content | ConvertFrom-Json
                
                Add-CheckResult -Name "API健康检查" -Status "pass" -Message "API健康检查正常" -Details @{
                    Url = $Url
                    Status = $HealthData.status
                    StatusCode = $Response.StatusCode
                }
                
                # 检查各组件状态
                if ($HealthData.components) {
                    foreach ($Component in $HealthData.components.PSObject.Properties) {
                        $CompStatus = $Component.Value.status
                        $CheckStatus = if ($CompStatus -eq "UP") { "pass" } elseif ($CompStatus -eq "DOWN") { "fail" } else { "warn" }
                        Add-CheckResult -Name "API组件: $($Component.Name)" -Status $CheckStatus -Message "组件状态: $CompStatus" -Details @{ Status = $CompStatus }
                    }
                }
            } else {
                Add-CheckResult -Name "API健康检查" -Status "fail" -Message "API返回非200状态码" -Details @{
                    Url = $Url
                    StatusCode = $Response.StatusCode
                }
            }
        } catch {
            Add-CheckResult -Name "API健康检查" -Status "fail" -Message "API请求失败: $_" -Details @{
                Url = $Url
                Exception = $_.Exception.Message
            }
        }
    } catch {
        Add-CheckResult -Name "API服务" -Status "fail" -Message "API检查异常: $_" -Details @{ Exception = $_.Exception.Message }
    }
}

# 4. 定时任务执行状态检查
function Check-ScheduledTasks {
    Write-Host "检查定时任务状态..." -ForegroundColor Cyan
    
    try {
        # 检查Quartz定时任务表（通过API）
        $BaseUrl = $Config.API.BaseUrl
        
        # 检查API是否可用
        $TcpTest = Test-NetConnection -ComputerName "localhost" -Port 8080 -WarningAction SilentlyContinue
        
        if (-not $TcpTest.TcpTestSucceeded) {
            Add-CheckResult -Name "定时任务检查" -Status "warn" -Message "API不可用，无法检查定时任务状态" -Details @{
                Reason = "API服务未启动"
            }
            return
        }
        
        # 通过API检查定时任务状态
        try {
            $TaskUrl = "$BaseUrl/api/scheduler/tasks/status"
            $Response = Invoke-WebRequest -Uri $TaskUrl -TimeoutSec 10 -UseBasicParsing
            
            if ($Response.StatusCode -eq 200) {
                $TaskData = $Response.Content | ConvertFrom-Json
                
                $RunningTasks = $TaskData.running | Measure-Object | Select-Object -ExpandProperty Count
                $DelayedTasks = $TaskData.delayed | Measure-Object | Select-Object -ExpandProperty Count
                
                Add-CheckResult -Name "定时任务状态" -Status "pass" -Message "定时任务运行正常" -Details @{
                    Running = $RunningTasks
                    Delayed = $DelayedTasks
                }
                
                if ($DelayedTasks -gt 0) {
                    Add-CheckResult -Name "定时任务延迟" -Status "warn" -Message "有$DelayedTasks个任务延迟" -Details @{ DelayedCount = $DelayedTasks }
                }
            }
        } catch {
            Add-CheckResult -Name "定时任务状态" -Status "warn" -Message "无法获取定时任务状态: $_" -Details @{
                Exception = $_.Exception.Message
            }
        }
    } catch {
        Add-CheckResult -Name "定时任务检查" -Status "fail" -Message "定时任务检查异常: $_" -Details @{ Exception = $_.Exception.Message }
    }
}

# 生成健康检查报告
function Generate-Report {
    Write-Host "生成健康检查报告..." -ForegroundColor Cyan
    
    # 计算总体状态
    if ($HealthResults.Summary.Failed -gt 0) {
        $HealthResults.Status = "unhealthy"
    } elseif ($HealthResults.Summary.Warning -gt 0) {
        $HealthResults.Status = "degraded"
    } else {
        $HealthResults.Status = "healthy"
    }
    
    # 生成JSON报告
    $ReportFile = "$ReportDir/health-report-$(Get-Date -Format 'yyyyMMdd-HHmmss').json"
    $HealthResults | ConvertTo-Json -Depth 10 | Out-File -FilePath $ReportFile -Encoding UTF8
    
    Write-Host "报告已保存: $ReportFile" -ForegroundColor Green
    
    # 生成摘要
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Yellow
    Write-Host "        AI-Ready 健康检查报告" -ForegroundColor Yellow
    Write-Host "========================================" -ForegroundColor Yellow
    Write-Host "检查时间: $($HealthResults.Timestamp)" -ForegroundColor White
    Write-Host "总体状态: $($HealthResults.Status)" -ForegroundColor $(switch ($HealthResults.Status) { "healthy" { "Green" } "degraded" { "Yellow" } default { "Red" } })
    Write-Host ""
    Write-Host "检查项统计:" -ForegroundColor White
    Write-Host "  总计: $($HealthResults.Summary.Total)" -ForegroundColor White
    Write-Host "  通过: $($HealthResults.Summary.Passed)" -ForegroundColor Green
    Write-Host "  失败: $($HealthResults.Summary.Failed)" -ForegroundColor Red
    Write-Host "  警告: $($HealthResults.Summary.Warning)" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "详细检查结果:" -ForegroundColor White
    foreach ($Check in $HealthResults.Checks) {
        $Color = switch ($Check.Status) { "pass" { "Green" } "fail" { "Red" } default { "Yellow" } }
        Write-Host "  [$($Check.Status.ToUpper())] $($Check.Name): $($Check.Message)" -ForegroundColor $Color
    }
    Write-Host "========================================" -ForegroundColor Yellow
    
    return $ReportFile
}

# 主执行流程
Write-Host "AI-Ready 服务健康检查开始..." -ForegroundColor Green
Write-Host "时间: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor White

Check-Database
Check-Redis
Check-API
Check-ScheduledTasks

$ReportFile = Generate-Report

Write-Host ""
Write-Host "健康检查完成。" -ForegroundColor Green

# 返回状态码
exit $(if ($HealthResults.Status -eq "healthy") { 0 } elseif ($HealthResults.Status -eq "degraded") { 1 } else { 2 })