# Sprint 28 监控告警模块 - Prometheus 监控部署脚本
# 版本: v1.0
# 最后更新: 2026-04-26
# 使用方法: .\deploy.ps1

param(
    [Parameter()]
    [ValidateSet("start", "stop", "restart", "status", "logs", "update")]
    [string]$Action = "start",

    [Parameter()]
    [string]$Service = "all"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ComposeFile = Join-Path $ScriptDir "docker-compose.yml"

# 颜色输出函数
function Write-ColorOutput {
    param(
        [string]$Message,
        [string]$Color = "White"
    )
    Write-Host $Message -ForegroundColor $Color
}

# 检查 Docker 环境
function Test-DockerEnvironment {
    Write-ColorOutput "检查 Docker 环境..." "Yellow"
    
    try {
        $dockerVersion = docker version --format '{{.Server.Version}}' 2>$null
        if ($LASTEXITCODE -ne 0) {
            throw "Docker 服务未运行"
        }
        Write-ColorOutput "Docker 版本: $dockerVersion" "Green"
        
        $composeVersion = docker-compose version --short 2>$null
        Write-ColorOutput "Docker Compose 版本: $composeVersion" "Green"
        
        return $true
    }
    catch {
        Write-ColorOutput "错误: $_" "Red"
        return $false
    }
}

# 启动监控服务
function Start-Monitoring {
    Write-ColorOutput "启动 Sprint 28 监控告警模块监控栈..." "Cyan"
    
    # 检查配置文件
    if (-not (Test-Path $ComposeFile)) {
        Write-ColorOutput "错误: 找不到 docker-compose.yml 文件" "Red"
        exit 1
    }
    
    # 创建必要的目录
    $directories = @(
        "grafana-dashboards"
    )
    
    foreach ($dir in $directories) {
        $path = Join-Path $ScriptDir $dir
        if (-not (Test-Path $path)) {
            New-Item -ItemType Directory -Path $path -Force | Out-Null
            Write-ColorOutput "创建目录: $dir" "Green"
        }
    }
    
    # 启动服务
    Write-ColorOutput "正在启动服务..." "Yellow"
    docker-compose -f $ComposeFile up -d
    
    if ($LASTEXITCODE -ne 0) {
        Write-ColorOutput "启动失败!" "Red"
        exit 1
    }
    
    Write-ColorOutput "服务启动成功!" "Green"
    
    # 等待服务就绪
    Write-ColorOutput "等待服务就绪..." "Yellow"
    Start-Sleep -Seconds 10
    
    # 显示服务状态
    Show-ServiceStatus
    
    # 显示访问地址
    Write-ColorOutput "" "White"
    Write-ColorOutput "=== 监控服务访问地址 ===" "Cyan"
    Write-ColorOutput "Prometheus:  http://localhost:9090" "Green"
    Write-ColorOutput "Grafana:     http://localhost:3000 (admin/admin123)" "Green"
    Write-ColorOutput "AlertManager: http://localhost:9093" "Green"
    Write-ColorOutput "========================" "Cyan"
}

# 停止监控服务
function Stop-Monitoring {
    Write-ColorOutput "停止 Sprint 28 监控告警模块监控栈..." "Cyan"
    
    docker-compose -f $ComposeFile down
    
    if ($LASTEXITCODE -eq 0) {
        Write-ColorOutput "服务已停止" "Green"
    } else {
        Write-ColorOutput "停止服务时出错" "Red"
    }
}

# 重启监控服务
function Restart-Monitoring {
    Write-ColorOutput "重启 Sprint 28 监控告警模块监控栈..." "Cyan"
    Stop-Monitoring
    Start-Sleep -Seconds 2
    Start-Monitoring
}

# 显示服务状态
function Show-ServiceStatus {
    Write-ColorOutput "" "White"
    Write-ColorOutput "=== 服务状态 ===" "Cyan"
    
    $services = docker-compose -f $ComposeFile ps --format json 2>$null | ConvertFrom-Json
    
    if ($services) {
        foreach ($svc in $services) {
            $status = $svc.State
            $name = $svc.Name
            $health = $svc.Health
            
            if ($status -eq "running") {
                Write-ColorOutput "[运行中] $name (健康: $health)" "Green"
            } else {
                Write-ColorOutput "[$status] $name" "Red"
            }
        }
    } else {
        Write-ColorOutput "没有运行中的服务" "Yellow"
    }
    
    Write-ColorOutput "================" "Cyan"
}

# 显示日志
function Show-Logs {
    param([string]$ServiceName = "")
    
    Write-ColorOutput "显示日志..." "Cyan"
    
    if ($ServiceName -and $ServiceName -ne "all") {
        docker-compose -f $ComposeFile logs -f $ServiceName
    } else {
        docker-compose -f $ComposeFile logs -f --tail=100
    }
}

# 更新配置
function Update-Configuration {
    Write-ColorOutput "更新 Prometheus 配置..." "Cyan"
    
    # 重新加载 Prometheus 配置
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:9090/-/reload" -Method POST -UseBasicParsing
        Write-ColorOutput "Prometheus 配置已重新加载" "Green"
    }
    catch {
        Write-ColorOutput "重新加载 Prometheus 配置失败: $_" "Red"
    }
    
    # 重新加载 AlertManager 配置
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:9093/-/reload" -Method POST -UseBasicParsing
        Write-ColorOutput "AlertManager 配置已重新加载" "Green"
    }
    catch {
        Write-ColorOutput "重新加载 AlertManager 配置失败: $_" "Yellow"
    }
}

# 主逻辑
Write-ColorOutput "=== Sprint 28 监控告警模块监控部署脚本 ===" "Cyan"
Write-ColorOutput "操作: $Action" "White"
Write-ColorOutput "==========================================" "Cyan"

# 检查 Docker 环境（除了 status 操作）
if ($Action -ne "status") {
    if (-not (Test-DockerEnvironment)) {
        exit 1
    }
}

# 执行操作
switch ($Action) {
    "start" { Start-Monitoring }
    "stop" { Stop-Monitoring }
    "restart" { Restart-Monitoring }
    "status" { Show-ServiceStatus }
    "logs" { Show-Logs -ServiceName $Service }
    "update" { Update-Configuration }
}

Write-ColorOutput "" "White"
Write-ColorOutput "脚本执行完成" "Green"
