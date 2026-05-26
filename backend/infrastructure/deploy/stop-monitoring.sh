#!/bin/bash
# 监控告警模块停止脚本
# 使用: ./stop-monitoring.sh [选项]

set -e

COMPOSE_FILE="monitoring-alerting-deployment.yml"
ENVIRONMENT=${1:-"development"}

# 根据环境设置变量
case "$ENVIRONMENT" in
    "development")
        export COMPOSE_PROJECT_NAME="ai-ready-monitoring-dev"
        ;;
    "staging")
        export COMPOSE_PROJECT_NAME="ai-ready-monitoring-staging"
        ;;
    "production")
        export COMPOSE_PROJECT_NAME="ai-ready-monitoring-prod"
        ;;
    *)
        echo "错误: 未知环境 '$ENVIRONMENT'，可用环境: development, staging, production"
        exit 1
        ;;
esac

echo "=========================================="
echo "停止 AI-Ready 监控告警模块"
echo "环境: $ENVIRONMENT"
echo "时间: $(date)"
echo "=========================================="

# 检查Docker Compose文件
if [ ! -f "$COMPOSE_FILE" ]; then
    echo "错误: 找不到Docker Compose文件: $COMPOSE_FILE"
    exit 1
fi

# 显示当前状态
echo "当前运行状态:"
docker-compose -f "$COMPOSE_FILE" ps

# 确认停止
read -p "确定要停止监控告警模块吗？(y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "操作已取消"
    exit 0
fi

# 停止选项
echo ""
echo "请选择停止方式:"
echo "  1) 正常停止 (保留数据)"
echo "  2) 停止并清理数据"
echo "  3) 停止并删除所有资源"
read -p "请选择 (1-3): " option

case $option in
    1)
        echo "执行正常停止..."
        docker-compose -f "$COMPOSE_FILE" down
        echo "✅ 服务已正常停止，数据已保留"
        ;;
    2)
        echo "执行停止并清理数据..."
        docker-compose -f "$COMPOSE_FILE" down -v
        echo "✅ 服务已停止，数据已清理"
        ;;
    3)
        echo "执行停止并删除所有资源..."
        docker-compose -f "$COMPOSE_FILE" down -v --rmi all --remove-orphans
        echo "✅ 服务已停止，所有资源已清理"
        ;;
    *)
        echo "错误: 无效选项"
        exit 1
        ;;
esac

# 验证停止结果
echo ""
echo "验证停止结果..."
if docker-compose -f "$COMPOSE_FILE" ps | grep -q "Up"; then
    echo "⚠️  仍有服务在运行:"
    docker-compose -f "$COMPOSE_FILE" ps | grep "Up"
else
    echo "✅ 所有服务已停止"
fi

# 清理网络（如果存在）
echo ""
echo "清理网络资源..."
docker network prune -f

echo ""
echo "=========================================="
echo "监控告警模块已停止"
echo "=========================================="
echo ""
echo "如需重新启动，请运行: ./start-monitoring.sh $ENVIRONMENT"
echo "如需查看日志备份，请检查 logs/ 目录"