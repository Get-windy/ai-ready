#!/bin/bash
# 监控告警模块启动脚本
# 使用: ./start-monitoring.sh [环境]

set -e

ENVIRONMENT=${1:-"development"}
COMPOSE_FILE="monitoring-alerting-deployment.yml"

echo "=========================================="
echo "启动 AI-Ready 监控告警模块"
echo "环境: $ENVIRONMENT"
echo "时间: $(date)"
echo "=========================================="

# 检查Docker Compose文件
if [ ! -f "$COMPOSE_FILE" ]; then
    echo "错误: 找不到Docker Compose文件: $COMPOSE_FILE"
    exit 1
fi

# 根据环境设置变量
case "$ENVIRONMENT" in
    "development")
        export COMPOSE_PROJECT_NAME="ai-ready-monitoring-dev"
        export NODE_ENV="development"
        ;;
    "staging")
        export COMPOSE_PROJECT_NAME="ai-ready-monitoring-staging"
        export NODE_ENV="staging"
        ;;
    "production")
        export COMPOSE_PROJECT_NAME="ai-ready-monitoring-prod"
        export NODE_ENV="production"
        ;;
    *)
        echo "错误: 未知环境 '$ENVIRONMENT'，可用环境: development, staging, production"
        exit 1
        ;;
esac

echo "项目名称: $COMPOSE_PROJECT_NAME"
echo "Node环境: $NODE_ENV"

# 启动服务
echo "启动Docker Compose服务..."
docker-compose -f "$COMPOSE_FILE" up -d

# 等待服务启动
echo "等待服务启动..."
sleep 10

# 检查服务状态
echo "检查服务状态..."
docker-compose -f "$COMPOSE_FILE" ps

# 显示访问信息
echo ""
echo "=========================================="
echo "监控告警模块启动完成！"
echo "=========================================="
echo ""
echo "访问地址:"
echo "  Prometheus:      http://localhost:9090"
echo "  Grafana:         http://localhost:3000 (admin/admin123)"
echo "  AlertManager:    http://localhost:9093"
echo "  监控API:         http://localhost:8081"
echo "  Nginx门户:       http://localhost"
echo ""
echo "数据库连接:"
echo "  PostgreSQL:      localhost:5433 (monitoring_user/monitoring_pass_123)"
echo "  Redis:           localhost:6380 (密码: redis_monitoring_pass_123)"
echo "  RabbitMQ管理:    http://localhost:15673 (monitoring_rabbit/rabbit_monitoring_pass_123)"
echo ""
echo "管理命令:"
echo "  查看日志:        docker-compose -f $COMPOSE_FILE logs -f"
echo "  停止服务:        docker-compose -f $COMPOSE_FILE down"
echo "  重启服务:        docker-compose -f $COMPOSE_FILE restart"
echo "  查看状态:        docker-compose -f $COMPOSE_FILE ps"
echo "  清理数据:        docker-compose -f $COMPOSE_FILE down -v"

# 健康检查
check_health() {
    echo ""
    echo "执行健康检查..."
    
    local services=("prometheus" "grafana" "alertmanager" "postgres-monitoring" "redis-monitoring" "rabbitmq-monitoring" "nginx")
    local healthy=true
    
    for service in "${services[@]}"; do
        if docker-compose -f "$COMPOSE_FILE" ps | grep -q "${service}.*Up"; then
            echo "  ✓ ${service}: 运行正常"
        else
            echo "  ✗ ${service}: 运行异常"
            healthy=false
        fi
    done
    
    if [ "$healthy" = true ]; then
        echo ""
        echo "✅ 所有服务健康检查通过！"
    else
        echo ""
        echo "⚠️  部分服务运行异常，请检查日志"
        echo "查看日志命令: docker-compose -f $COMPOSE_FILE logs"
    fi
}

check_health

echo ""
echo "部署完成！系统将在30秒后完全就绪。"
echo "请访问上述地址验证部署结果。"

# 等待完全启动
sleep 20

# 最终状态检查
echo ""
echo "最终状态检查..."
docker-compose -f "$COMPOSE_FILE" ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"