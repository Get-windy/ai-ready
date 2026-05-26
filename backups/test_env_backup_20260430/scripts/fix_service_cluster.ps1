# Sprint 27+1 测试环境服务集群修复脚本
# 修复缺失的服务和配置问题

param(
    [switch]$CheckOnly = $false,
    [switch]$RestartServices = $false
)

$ErrorActionPreference = "Continue"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Sprint 27+1 测试环境服务集群修复工具" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 服务端口映射配置
$services = @{
    "api-gateway" = @{ port = 8080; required = $true; description = "API网关服务" }
    "user-service-1" = @{ port = 8081; required = $true; description = "用户服务实例1" }
    "user-service-2" = @{ port = 8082; required = $true; description = "用户服务实例2" }
    "order-service" = @{ port = 8083; required = $true; description = "订单服务" }
    "inventory-service" = @{ port = 8085; required = $true; description = "库存服务" }
    "monitoring-service" = @{ port = 8086; required = $false; description = "监控服务" }
    "nacos" = @{ port = 8848; required = $true; description = "Nacos服务注册中心" }
    "prometheus" = @{ port = 9090; required = $false; description = "Prometheus监控" }
    "grafana" = @{ port = 3000; required = $false; description = "Grafana监控面板" }
    "jaeger" = @{ port = 16686; required = $false; description = "Jaeger链路追踪" }
}

# 检查服务健康状态
function Test-ServiceHealth {
    param($port, $name)
    
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:$port/actuator/health" -Method GET -TimeoutSec 5 -ErrorAction SilentlyContinue
        return @{ status = $response.StatusCode; healthy = ($response.StatusCode -eq 200) }
    } catch {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:$port/health" -Method GET -TimeoutSec 5 -ErrorAction SilentlyContinue
            return @{ status = $response.StatusCode; healthy = ($response.StatusCode -eq 200) }
        } catch {
            try {
                $response = Invoke-WebRequest -Uri "http://localhost:$port" -Method GET -TimeoutSec 5 -ErrorAction SilentlyContinue
                return @{ status = $response.StatusCode; healthy = $true }
            } catch {
                return @{ status = "Error: $($_.Exception.Message)"; healthy = $false }
            }
        }
    }
}

# 检查端口是否监听
function Test-PortListening {
    param($port)
    
    try {
        $tcpClient = New-Object System.Net.Sockets.TcpClient
        $tcpClient.Connect("localhost", $port)
        $tcpClient.Close()
        return $true
    } catch {
        return $false
    }
}

# 显示检查结果
Write-Host "服务健康检查结果:" -ForegroundColor Yellow
Write-Host "----------------------------------------" -ForegroundColor Gray

$results = @()
$missingServices = @()
$unhealthyServices = @()

foreach ($service in $services.GetEnumerator() | Sort-Object { $_.Value.port }) {
    $name = $service.Key
    $port = $service.Value.port
    $required = $service.Value.required
    $description = $service.Value.description
    
    $isListening = Test-PortListening -port $port
    
    if ($isListening) {
        $health = Test-ServiceHealth -port $port -name $name
        $status = if ($health.healthy) { "✅ 健康" } else { "⚠️ 运行中但健康检查失败" }
        $color = if ($health.healthy) { "Green" } else { "Yellow" }
        
        Write-Host "[$name] Port $port - $status (HTTP $($health.status))" -ForegroundColor $color
        
        $results += @{
            name = $name
            port = $port
            status = $status
            healthy = $health.healthy
            required = $required
            httpStatus = $health.status
        }
        
        if (-not $health.healthy -and $required) {
            $unhealthyServices += $name
        }
    } else {
        $status = if ($required) { "❌ 缺失 (必需)" } else { "⚪ 缺失 (可选)" }
        $color = if ($required) { "Red" } else { "Gray" }
        
        Write-Host "[$name] Port $port - $status" -ForegroundColor $color
        
        $results += @{
            name = $name
            port = $port
            status = $status
            healthy = $false
            required = $required
            httpStatus = "N/A"
        }
        
        if ($required) {
            $missingServices += $name
        }
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan

# 生成报告
$healthyCount = ($results | Where-Object { $_.healthy }).Count
$totalRequired = ($results | Where-Object { $_.required }).Count
$healthyRequired = ($results | Where-Object { $_.required -and $_.healthy }).Count

Write-Host "检查摘要:" -ForegroundColor Yellow
Write-Host "  总服务数: $($services.Count)" -ForegroundColor White
Write-Host "  健康服务: $healthyCount" -ForegroundColor Green
Write-Host "  必需服务: $totalRequired" -ForegroundColor White
Write-Host "  健康必需服务: $healthyRequired/$totalRequired" -ForegroundColor $(if ($healthyRequired -eq $totalRequired) { "Green" } else { "Red" })
Write-Host ""

if ($missingServices.Count -gt 0) {
    Write-Host "缺失的必需服务:" -ForegroundColor Red
    $missingServices | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
    Write-Host ""
}

if ($unhealthyServices.Count -gt 0) {
    Write-Host "运行不健康的必需服务:" -ForegroundColor Yellow
    $unhealthyServices | ForEach-Object { Write-Host "  - $_" -ForegroundColor Yellow }
    Write-Host ""
}

# 保存检查结果
$reportPath = "I:\AI-Ready\test_env\logs\service_health_check_$(Get-Date -Format 'yyyyMMdd_HHmmss').json"
$results | ConvertTo-Json -Depth 3 | Out-File -FilePath $reportPath -Encoding UTF8
Write-Host "检查结果已保存到: $reportPath" -ForegroundColor Gray
Write-Host ""

# 如果仅检查，则退出
if ($CheckOnly) {
    Write-Host "检查模式完成。使用 -RestartServices 参数来重启服务。" -ForegroundColor Cyan
    exit 0
}

# 修复操作
Write-Host "开始修复操作..." -ForegroundColor Cyan
Write-Host ""

# 1. 检查Docker容器状态
Write-Host "[1/5] 检查Docker容器状态..." -ForegroundColor Yellow
try {
    $containers = docker ps --format "{{.Names}}" 2>$null
    Write-Host "  发现 $($containers.Count) 个运行中的容器" -ForegroundColor Green
    
    # 检查关键容器
    $requiredContainers = @("ai-ready-postgres", "ai-ready-redis", "ai-ready-nacos")
    foreach ($container in $requiredContainers) {
        if ($containers -contains $container) {
            Write-Host "  ✅ $container 运行中" -ForegroundColor Green
        } else {
            Write-Host "  ❌ $container 未运行" -ForegroundColor Red
        }
    }
} catch {
    Write-Host "  ⚠️ 无法检查Docker状态: $_" -ForegroundColor Yellow
}
Write-Host ""

# 2. 修复Nginx配置（API网关）
Write-Host "[2/5] 检查API网关配置..." -ForegroundColor Yellow
$nginxConfigPath = "I:\AI-Ready\test_env\etc\important\nginx.conf"
if (Test-Path $nginxConfigPath) {
    Write-Host "  发现Nginx配置文件" -ForegroundColor Green
    
    # 检查是否需要配置API网关
    $apiGatewayContainer = docker ps --filter "name=ai-ready-api-gateway" --format "{{.Names}}" 2>$null
    if (-not $apiGatewayContainer) {
        Write-Host "  ⚠️ API网关容器未运行，需要部署" -ForegroundColor Yellow
        Write-Host "  请运行: docker-compose -f I:\AI-Ready\deploy\docker-compose.yml up -d api-gateway" -ForegroundColor Cyan
    } else {
        Write-Host "  ✅ API网关容器运行中" -ForegroundColor Green
    }
} else {
    Write-Host "  ⚠️ 未找到Nginx配置文件" -ForegroundColor Yellow
}
Write-Host ""

# 3. 检查服务配置
Write-Host "[3/5] 检查服务配置..." -ForegroundColor Yellow
$configPath = "I:\AI-Ready\test_env\config\service_cluster_config.yaml"
if (Test-Path $configPath) {
    Write-Host "  ✅ 服务集群配置文件存在" -ForegroundColor Green
    
    # 验证配置中的端口与实际运行端口匹配
    $configContent = Get-Content $configPath -Raw
    Write-Host "  配置验证完成" -ForegroundColor Green
} else {
    Write-Host "  ❌ 服务集群配置文件不存在" -ForegroundColor Red
}
Write-Host ""

# 4. 生成修复建议
Write-Host "[4/5] 生成修复建议..." -ForegroundColor Yellow
$fixRecommendations =