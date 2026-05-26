#!/bin/bash
# 测试环境服务验证脚本
# 用途：快速检查测试环境服务可用性
# 执行：./verify-test-environment.sh

set -e

echo "=========================================="
echo "    测试环境服务可用性验证脚本"
echo "=========================================="
echo "启动时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

# 定义服务列表
declare -A SERVICES=(
    ["数据库服务"]="mysql -h localhost -P 3306 -u root -p'password123' -e 'SELECT 1' 2>/dev/null || echo 'MySQL not available'"
    ["PostgreSQL"]="pg_isready -h localhost -p 5432 2>/dev/null || echo 'PostgreSQL not available'"
    ["Redis"]="redis-cli -h localhost -p 6379 ping 2>/dev/null || echo 'Redis not available'"
    ["API网关"]="curl -f -s http://localhost:8080/actuator/health 2>/dev/null || echo 'API Gateway not available'"
    ["用户服务"]="curl -f -s http://localhost:8081/actuator/health 2>/dev/null || echo 'User Service not available'"
    ["订单服务"]="curl -f -s http://localhost:8082/actuator/health 2>/dev/null || echo 'Order Service not available'"
    ["库存服务"]="curl -f -s http://localhost:8083/actuator/health 2>/dev/null || echo 'Inventory Service not available'"
    ["消息队列"]="curl -f -s http://localhost:15672/api/overview 2>/dev/null || echo 'RabbitMQ not available'"
    ["Prometheus"]="curl -f -s http://localhost:9090/-/ready 2>/dev/null || echo 'Prometheus not available'"
    ["Grafana"]="curl -f -s http://localhost:3000/api/health 2>/dev/null || echo 'Grafana not available'"
)

# 验证服务可用性
AVAILABLE_SERVICES=0
TOTAL_SERVICES=${#SERVICES[@]}

for SERVICE_NAME in "${!SERVICES[@]}"; do
    echo -n "检查 ${SERVICE_NAME}... "
    if eval "${SERVICES[$SERVICE_NAME]}" >/dev/null 2>&1; then
        echo "✅ 可用"
        ((AVAILABLE_SERVICES++))
    else
        echo "❌ 不可用"
    fi
    sleep 0.5
done

echo ""
echo "=========================================="
echo "服务可用性统计:"
echo "总服务数: ${TOTAL_SERVICES}"
echo "可用服务数: ${AVAILABLE_SERVICES}"
echo "可用率: $((AVAILABLE_SERVICES * 100 / TOTAL_SERVICES))%"
echo ""

if [ "${AVAILABLE_SERVICES}" -eq "${TOTAL_SERVICES}" ]; then
    echo "🎉 所有测试环境服务都已就绪！可以开始性能测试。"
    echo "预计开始时间: $(date '+%H:%M')"
    echo ""
    echo "建议执行以下步骤:"
    echo "1. 生成测试数据: ./scripts/setup-test-data.sh"
    echo "2. 启动性能监控: ./scripts/monitor-performance.sh start"
    echo "3. 执行性能测试: jmeter -n -t scripts/Login_Test.jmx -l results/login.jtl"
else
    echo "⚠️  部分服务不可用，性能测试无法开始。"
    echo ""
    echo "已确认以下服务需要启动:"
    for SERVICE_NAME in "${!SERVICES[@]}"; do
        if ! eval "${SERVICES[$SERVICE_NAME]}" >/dev/null 2>&1; then
            echo "  - ${SERVICE_NAME}"
        fi
    done
    echo ""
    echo "请通知 devops-engineer 启动缺失的服务。"
fi

echo "=========================================="
echo "验证完成时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="