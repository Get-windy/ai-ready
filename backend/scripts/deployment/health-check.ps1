# Sprint 29: 部署健康检查脚本 (PowerShell版本)
# 功能: 验证部署是否成功，失败时自动回滚
# 版本: v1.0

param(
    [string]$Namespace = "ai-ready",
    [string]$DeploymentName = "ai-ready-api",
    [int]$Timeout = 300,
    [string]$HealthEndpoint = "/actuator/health",
    [string]$ServiceUrl = ""
)

# 颜色输出
function Write-Info { param($Message) Write-Host "[INFO] $Message" -ForegroundColor Green }
function Write-Warn { param($Message) Write-Host "[WARN] $Message" -ForegroundColor Yellow }
function Write-Error { param($Message) Write-Host "[ERROR] $Message" -ForegroundColor Red }

# 检查kubectl
if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
    Write-Error "kubectl 未安装"
    exit 1
}

# 获取当前镜像版本
function Get-CurrentImage {
    kubectl get deployment $DeploymentName -n $Namespace -o jsonpath='{.spec.template.spec.containers[0].image}'
}

# 等待部署完成
function Wait-ForDeployment {
    Write-Info "等待部署完成 (超时: ${Timeout}s)..."
    
    $result = kubectl rollout status deployment/$DeploymentName -n $Namespace --timeout="${Timeout}s" 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Info "部署成功完成"
        return $true
    } else {
        Write-Error "部署超时或失败"
        return $false
    }
}

# 检查Pod就绪状态
function Test-PodsReady {
    Write-Info "检查Pod就绪状态..."
    
    $readyPods = (kubectl get pods -n $Namespace -l app=$DeploymentName --field-selector=status.phase=Running -o jsonpath='{.items[*].metadata.name}').Split().Count
    $desiredReplicas = kubectl get deployment $DeploymentName -n $Namespace -o jsonpath='{.spec.replicas}'
    
    if ($readyPods -ge $desiredReplicas) {
        Write-Info "所有Pod已就绪: $readyPods/$desiredReplicas"
        return $true
    } else {
        Write-Error "Pod未全部就绪: $readyPods/$desiredReplicas"
        return $false
    }
}

# 健康检查
function Test-Health {
    Write-Info "执行健康检查..."
    
    $retries = 5
    $delay = 10
    
    for ($i = 1; $i -le $retries; $i++) {
        Write-Info "健康检查尝试 $i/$retries..."
        
        $podName = kubectl get pods -n $Namespace -l app=$DeploymentName -o jsonpath='{.items[0].metadata.name}'
        $result = kubectl exec -n $Namespace $podName -- curl -fs "http://localhost:8080$HealthEndpoint" 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Info "健康检查通过"
            return $true
        }
        
        if ($i -lt $retries) {
            Write-Warn "健康检查失败，${delay}秒后重试..."
            Start-Sleep -Seconds $delay
        }
    }
    
    Write-Error "健康检查失败"
    return $false
}

# 执行回滚
function Invoke-Rollback {
    Write-Error "执行回滚操作..."
    
    $result = kubectl rollout undo deployment/$DeploymentName -n $Namespace 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Info "回滚命令已执行"
        
        $result = kubectl rollout status deployment/$DeploymentName -n $Namespace --timeout="${Timeout}s" 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Info "回滚成功"
            return $true
        } else {
            Write-Error "回滚失败"
            return $false
        }
    } else {
        Write-Error "回滚命令执行失败"
        return $false
    }
}

# 获取部署状态
function Get-DeploymentStatus {
    Write-Info "获取部署状态..."
    
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "部署: $DeploymentName" -ForegroundColor Cyan
    Write-Host "命名空间: $Namespace" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    
    kubectl get deployment $DeploymentName -n $Namespace -o wide
    Write-Host ""
    
    Write-Host "Pod状态:" -ForegroundColor Cyan
    kubectl get pods -n $Namespace -l app=$DeploymentName -o wide
    Write-Host ""
    
    Write-Host "事件:" -ForegroundColor Cyan
    kubectl get events -n $Namespace --field-selector involvedObject.name=$DeploymentName --sort-by='.lastTimestamp' | Select-Object -Last 10
    Write-Host ""
    
    Write-Host "Rollout历史:" -ForegroundColor Cyan
    kubectl rollout history deployment/$DeploymentName -n $Namespace
}

# 主函数
function Main {
    Write-Info "开始健康检查..."
    Write-Info "命名空间: $Namespace"
    Write-Info "部署名称: $DeploymentName"
    
    # 保存当前镜像版本
    $script:CurrentImage = Get-CurrentImage
    Write-Info "当前镜像: $script:CurrentImage"
    
    # 执行检查
    $failed = $false
    
    if (-not (Wait-ForDeployment)) {
        $failed = $true
    }
    
    if (-not (Test-PodsReady)) {
        $failed = $true
    }
    
    if (-not (Test-Health)) {
        $failed = $true
    }
    
    # 输出状态
    Get-DeploymentStatus
    
    if ($failed) {
        Write-Error "健康检查失败，准备回滚..."
        Invoke-Rollback
        exit 1
    }
    
    Write-Info "所有检查通过，部署成功！"
    exit 0
}

# 显示帮助
if ($args -contains "-h" -or $args -contains "--help") {
    Write-Host @"
Sprint 29 部署健康检查脚本

用法: .\health-check.ps1 [-Namespace <name>] [-DeploymentName <name>] [-Timeout <seconds>]

参数:
  -Namespace       Kubernetes命名空间 (默认: ai-ready)
  -DeploymentName  部署名称 (默认: ai-ready-api)
  -Timeout         超时时间秒数 (默认: 300)
  -HealthEndpoint  健康检查端点 (默认: /actuator/health)

示例:
  .\health-check.ps1                                    # 使用默认参数
  .\health-check.ps1 -Namespace ai-ready-staging -Timeout 600  # 自定义参数

"@
    exit 0
}

# 运行主函数
Main
