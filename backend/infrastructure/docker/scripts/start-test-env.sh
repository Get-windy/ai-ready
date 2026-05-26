#!/bin/bash
# AI-Ready 测试环境快速启动脚本
# 创建日期: 2026-04-24

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/../docker-compose-test.yml"

echo "=== AI-Ready Docker 测试环境启动 ==="
echo "配置文件: ${COMPOSE_FILE}"
echo ""

# 检查 Docker 是否可用
if ! command -v docker &> /dev/null; then
    echo "错误: Docker 未安装"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "错误: Docker Compose 未安装"
    exit 1
fi

# 检查配置文件
if [ ! -f "${COMPOSE_FILE}" ]; then
    echo "错误: 配置文件不存在: ${COMPOSE_FILE}"
    exit 1
fi

# 停止已有服务（可选）
echo ">>> 检查已有服务..."
docker-compose -f "${COMPOSE_FILE}" ps 2>/dev/null || true

echo ""
echo ">>> 启动核心服务..."
docker-compose -f "${COMPOSE_FILE}" up -d \
    postgres-main redis-main zookeeper kafka \
    postgres-inventory postgres-finance postgres-ai postgres-data \
    redis-inventory redis-finance redis-ai redis-data

echo ""
echo ">>> 等待基础设施就绪（30秒）..."
sleep 30

echo ""
echo ">>> 启动应用服务..."
docker-compose -f "${COMPOSE_FILE}" up -d \
    api-gateway inventory-service finance-service ai-service data-service frontend

echo ""
echo ">>> 启动监控服务..."
docker-compose -f "${COMPOSE_FILE}" up -d prometheus grafana alertmanager

echo ""
echo "=== 服务启动完成 ==="
echo ""
echo "服务访问地址:"
echo "  前端:         http://localhost:3000"
echo "  API Gateway:  http://localhost:8080"
echo "  库存管理:     http://localhost:8082"
echo "  财务管理:     http://localhost:8081"
echo "  AI智能:       http://localhost:8083"
echo "  数据管理:     http://localhost:8084"
echo "  Prometheus:   http://localhost:9090"
echo "  Grafana:      http://localhost:3000 (admin/admin_test_2026)"
echo ""
echo "查看服务状态: docker-compose -f ${COMPOSE_FILE} ps"
echo "查看服务日志: docker-compose -f ${COMPOSE_FILE} logs -f"
echo ""