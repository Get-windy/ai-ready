#!/bin/bash
# Test Environment Health Check Script
# This script verifies that all test environment services are running and healthy

echo "=== 企智连测试环境健康检查 ==="
echo "检查时间: $(date)"
echo ""

# Function to check if a port is open
check_port() {
    local host=$1
    local port=$2
    local service=$3
    
    if nc -z -w5 $host $port; then
        echo "✅ $service ($host:$port) - 正常"
        return 0
    else
        echo "❌ $service ($host:$port) - 异常"
        return 1
    fi
}

# Check PostgreSQL test database
check_port "localhost" "5433" "PostgreSQL测试数据库"

# Check Redis test cache
check_port "localhost" "6380" "Redis测试缓存"

# Check RabbitMQ test message queue (AMQP port)
check_port "localhost" "5673" "RabbitMQ测试消息队列(AMQP)"

# Check RabbitMQ management interface
check_port "localhost" "15673" "RabbitMQ管理界面"

# Check pgAdmin test management
check_port "localhost" "8081" "pgAdmin测试管理界面"

echo ""
echo "=== 健康检查API测试 ==="

# Test health check API endpoints
if command -v curl &> /dev/null; then
    echo "测试整体健康状态..."
    curl -s -o /dev/null -w "✅ 整体健康状态API - HTTP %{http_code}\n" http://localhost:8080/api/health/test-overview
    
    echo "测试服务健康状态..."
    curl -s -o /dev/null -w "✅ 服务健康状态API - HTTP %{http_code}\n" http://localhost:8080/api/health/test-services
    
    echo "测试基础设施健康状态..."
    curl -s -o /dev/null -w "✅ 基础设施健康状态API - HTTP %{http_code}\n" http://localhost:8080/api/health/test-infrastructure
else
    echo "⚠️  curl未安装，跳过API测试"
fi

echo ""
echo "=== Docker容器状态检查 ==="

if command -v docker &> /dev/null; then
    echo "检查Docker容器状态..."
    docker ps --filter "name=ai-ready.*test" --format "✅ {{.Names}} - {{.Status}}"
else
    echo "⚠️  Docker未安装或不可用，跳过容器检查"
fi

echo ""
echo "=== 测试完成 ==="