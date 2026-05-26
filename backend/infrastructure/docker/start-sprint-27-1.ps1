# Sprint 27+1测试环境启动脚本
# 用于启动数据库性能测试环境

Write-Host "=========================================" -ForegroundColor Green
Write-Host "   Sprint 27+1测试环境启动脚本" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green

# 检查Docker是否运行
Write-Host "`n[1/6] 检查Docker状态..." -ForegroundColor Yellow
try {
    $dockerInfo = docker info 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Docker未运行，请启动Docker服务" -ForegroundColor Red
        exit 1
    }
    Write-Host "✅ Docker运行正常" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker检查失败: $_" -ForegroundColor Red
    exit 1
}

# 检查Docker Compose文件
Write-Host "`n[2/6] 检查配置文件..." -ForegroundColor Yellow
$composeFile = "docker-compose-sprint-27-1.yml"
if (-not (Test-Path $composeFile)) {
    Write-Host "❌ Docker Compose文件不存在: $composeFile" -ForegroundColor Red
    exit 1
}
Write-Host "✅ 配置文件检查完成" -ForegroundColor Green

# 创建必要的目录
Write-Host "`n[3/6] 创建数据目录..." -ForegroundColor Yellow
$directories = @(
    "init-scripts/sprint/main",
    "init-scripts/sprint/inventory", 
    "init-scripts/sprint/finance",
    "init-scripts/sprint/ai",
    "prometheus-sprint",
    "grafana-sprint",
    "logs"
)

foreach ($dir in $directories) {
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        Write-Host "  创建目录: $dir" -ForegroundColor Gray
    }
}
Write-Host "✅ 目录结构准备完成" -ForegroundColor Green

# 创建初始化脚本
Write-Host "`n[4/6] 创建数据库初始化脚本..." -ForegroundColor Yellow

# 主数据库初始化脚本
$mainInitScript = @"
-- Sprint 27+1 主数据库初始化脚本
-- 创建时间: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')

-- 创建性能监控表
CREATE TABLE IF NOT EXISTS performance_metrics (
    id SERIAL PRIMARY KEY,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(15,4),
    metric_unit VARCHAR(50),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tags JSONB
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_performance_metric_name ON performance_metrics(metric_name);
CREATE INDEX IF NOT EXISTS idx_performance_timestamp ON performance_metrics(timestamp);

-- 创建慢查询日志表
CREATE TABLE IF NOT EXISTS slow_query_log (
    id SERIAL PRIMARY KEY,
    query_id VARCHAR(100),
    query_text TEXT,
    execution_time_ms DECIMAL(10,2),
    rows_returned INTEGER,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    database_name VARCHAR(100),
    username VARCHAR(100),
    client_address VARCHAR(50)
);

-- 插入初始化数据
INSERT INTO performance_metrics (metric_name, metric_value, metric_unit, tags) VALUES
    ('database_start_time', EXTRACT(EPOCH FROM NOW()), 'seconds', '{"type": "system", "component": "database"}'),
    ('connection_pool_initialized', 1, 'count', '{"type": "status", "component": "connection_pool"}');

-- 创建监控用户
CREATE USER monitor_user WITH PASSWORD 'monitor_pass_2026';
GRANT CONNECT ON DATABASE sprint_gateway_db TO monitor_user;
GRANT USAGE ON SCHEMA public TO monitor_user;
GRANT SELECT ON performance_metrics TO monitor_user;
GRANT SELECT ON slow_query_log TO monitor_user;

-- 输出初始化完成信息
SELECT 'Sprint 27+1主数据库初始化完成' as message, NOW() as timestamp;
"@

Set-Content -Path "init-scripts/sprint/main/01-init.sql" -Value $mainInitScript -Encoding UTF8
Write-Host "  创建主数据库初始化脚本" -ForegroundColor Gray

# 停止可能存在的旧容器
Write-Host "`n[5/6] 清理旧容器..." -ForegroundColor Yellow
Write-Host "  停止并移除旧容器..." -ForegroundColor Gray
docker-compose -f $composeFile down --remove-orphans 2>$null

# 启动新容器
Write-Host "`n[6/6] 启动Sprint 27+1测试环境..." -ForegroundColor Yellow
Write-Host "  正在启动容器，这可能需要几分钟..." -ForegroundColor Gray

docker-compose -f $composeFile up -d

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✅ Sprint 27+1测试环境启动成功！" -ForegroundColor Green
    
    # 显示服务状态
    Write-Host "`n📊 服务状态:" -ForegroundColor Cyan
    docker-compose -f $composeFile ps
    
    # 显示访问信息
    Write-Host "`n🔗 访问信息:" -ForegroundColor Cyan
    Write-Host "  API Gateway: http://localhost:8080" -ForegroundColor White
    Write-Host "  Prometheus:  http://localhost:9090" -ForegroundColor White  
    Write-Host "  Grafana:     http://localhost:3000 (admin/sprint_admin_2026)" -ForegroundColor White
    
    # 显示数据库信息
    Write-Host "`n🗄️ 数据库信息:" -ForegroundColor Cyan
    Write-Host "  主数据库: localhost:5432 (sprint_gateway_user/sprint_gateway_pass_2026)" -ForegroundColor White
    Write-Host "  库存数据库: localhost:5434 (sprint_inventory_user/sprint_inventory_pass_2026)" -ForegroundColor White
    Write-Host "  财务数据库: localhost:5433 (sprint_finance_user/sprint_finance_pass_2026)" -ForegroundColor White
    Write-Host "  AI数据库: localhost:5435 (sprint_ai_user/sprint_ai_pass_2026)" -ForegroundColor White
    
    Write-Host "`n⚠️  注意: 首次启动需要等待数据库初始化完成" -ForegroundColor Yellow
    Write-Host "   可以使用以下命令查看日志: docker-compose -f $composeFile logs -f" -ForegroundColor Gray
    
} else {
    Write-Host "`n❌ 启动失败，请检查日志" -ForegroundColor Red
    Write-Host "  查看错误日志: docker-compose -f $composeFile logs" -ForegroundColor Gray
    exit 1
}

Write-Host "`n=========================================" -ForegroundColor Green
Write-Host "   环境启动完成!" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green