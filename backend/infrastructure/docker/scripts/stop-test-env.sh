#!/bin/bash
# AI-Ready 测试环境停止脚本
# 创建日期: 2026-04-24

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/../docker-compose-test.yml"

echo "=== AI-Ready Docker 测试环境停止 ==="
echo "配置文件: ${COMPOSE_FILE}"
echo ""

# 检查配置文件
if [ ! -f "${COMPOSE_FILE}" ]; then
    echo "错误: 配置文件不存在: ${COMPOSE_FILE}"
    exit 1
fi

echo ">>> 停止所有服务..."
docker-compose -f "${COMPOSE_FILE}" down

echo ""
echo "=== 服务已停止 ==="
echo ""
echo "如需清理数据卷: docker-compose -f ${COMPOSE_FILE} down -v"
echo ""