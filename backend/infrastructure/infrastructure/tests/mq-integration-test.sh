#!/bin/bash

# AI-Ready 消息队列集成验证脚本
# 验证消息队列与监控告警系统的集成

set -e

echo "=========================================="
echo "AI-Ready 消息队列集成验证测试"
echo "=========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试计数器
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_TOTAL=0

# 测试函数
run_test() {
    local test_name="$1"
    local test_command="$2"
    
    TESTS_TOTAL=$((TESTS_TOTAL + 1))
    
    echo -n "[$TESTS_TOTAL] $test_name... "
    
    if eval "$test_command" > /dev/null 2>&1; then
        echo -e "${GREEN}PASS${NC}"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        return 0
    else
        echo -e "${RED}FAIL${NC}"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        return 1
    fi
}

# 检查命令是否存在
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

echo "Step 1: 环境检查"
echo "----------------"

# 检查必要命令
run_test "Docker 已安装" "command_exists docker"
run_test "Docker Compose 已安装" "command_exists docker-compose"
run_test "curl 已安装" "command_exists curl"

echo ""
echo "Step 2: RabbitMQ 服务检查"
echo "--------------------------"

# 检查 RabbitMQ 容器
run_test "RabbitMQ 容器运行中" "docker ps | grep -q rabbitmq"

# 检查端口
run_test "RabbitMQ AMQP 端口 (5672) 可访问" "nc -z localhost 5672"
run_test "RabbitMQ 管理端口 (15672) 可访问" "nc -z localhost 15672"

# 检查 RabbitMQ 健康
run_test "RabbitMQ 健康检查" "curl -s -u guest:guest http://localhost:15672/api/healthchecks/node | grep -q 'status.*ok'"

echo ""
echo "Step 3: 队列配置验证"
echo "--------------------"

# 检查预定义队列
run_test "email 队列存在" "curl -s -u guest:guest http://localhost:15672/api/queues/%2f/ai.ready.email | grep -q 'name'"
run_test "sms 队列存在" "curl -s -u guest:guest http://localhost:15672/api/queues/%2f/ai.ready.sms | grep -q 'name'"
run_test "notification 队列存在" "curl -s -u guest:guest http://localhost:15672/api/queues/%2f/ai.ready.notification | grep -q 'name'"
run_test "data.sync 队列存在" "curl -s -u guest:guest http://localhost:15672/api/queues/%2f/ai.ready.data.sync | grep -q 'name'"
run_test "dead.letter 队列存在" "curl -s -u guest:guest http://localhost:15672/api/queues/%2f/ai.ready.dead.letter | grep -q 'name'"

echo ""
echo "Step 4: 交换机配置验证"
echo "----------------------"

# 检查交换机
run_test "默认交换机存在" "curl -s -u guest:guest http://localhost:15672/api/exchanges/%2f/ai-ready-exchange | grep -q 'name'"
run_test "死信交换机存在" "curl -s -u guest:guest http://localhost:15672/api/exchanges/%2f/ai-ready-exchange.dlx | grep -q 'name'"

echo ""
echo "Step 5: 消息发送测试"
echo "--------------------"

# 发送测试消息
TEST_MESSAGE='{"messageId":"test-'$(date +%s)'","messageType":"TEST","topic":"email","payload":{"test":true},"createTime":"'$(date -Iseconds)'","retryCount":0,"maxRetry":3}'

run_test "发送测试消息到 email 队列" "curl -s -u guest:guest -X POST -H 'Content-Type: application/json' -d '$TEST_MESSAGE' http://localhost:15672/api/exchanges/%2f/ai-ready-exchange/publish -d '{\"routing_key\":\"email\",\"payload\":\"$TEST_MESSAGE\",\"payload_encoding\":\"string\"}'"

echo ""
echo "Step 6: Prometheus 集成检查"
echo "---------------------------"

# 检查 Prometheus 配置
run_test "Prometheus 配置文件存在" "test -f ../configs/monitoring/prometheus.yml"
run_test "RabbitMQ Exporter 配置存在" "grep -q 'rabbitmq' ../configs/monitoring/prometheus.yml"

# 检查 Alert Rules
run_test "MQ 告警规则文件存在" "test -f ../configs/alerting/mq_alert_rules.yml"
run_test "RabbitMQDown 告警规则存在" "grep -q 'RabbitMQDown' ../configs/alerting/mq_alert_rules.yml"
run_test "RabbitMQQueueMessagesHigh 告警规则存在" "grep -q 'RabbitMQQueueMessagesHigh' ../configs/alerting/mq_alert_rules.yml"

echo ""
echo "Step 7: Grafana 集成检查"
echo "------------------------"

# 检查 Grafana 数据源配置
run_test "Grafana 数据源配置存在" "test -f ../configs/monitoring/grafana/provisioning/datasources/datasources.yml"
run_test "Prometheus 数据源配置存在" "grep -q 'prometheus' ../configs/monitoring/grafana/provisioning/datasources/datasources.yml"

echo ""
echo "Step 8: 应用层集成检查"
echo "----------------------"

# 检查应用配置
run_test "应用 MQ 配置文件存在" "test -f ../../backend/core/api/core-api/src/main/java/cn/aiedge/mq/config/RabbitMQConfig.java"
run_test "MQ 监控服务存在" "test -f ../../backend/core/api/core-api/src/main/java/cn/aiedge/mq/service/MessageQueueMonitor.java"

echo ""
echo "Step 9: 文档完整性检查"
echo "----------------------"

run_test "MQ 模块文档存在" "test -f ../../docs/MESSAGE_QUEUE_MODULE.md"
run_test "MQ 集成文档存在" "test -f ../../docs/message-queue-guide.md"
run_test "MQ 监控告警指南存在" "test -f ../../docs/mq-monitoring-alerting-guide.md"
run_test "MQ 运维指南存在" "test -f ../../docs/mq-operations-guide.md"

echo ""
echo "=========================================="
echo "测试结果汇总"
echo "=========================================="
echo -e "总测试数: $TESTS_TOTAL"
echo -e "通过: ${GREEN}$TESTS_PASSED${NC}"
echo -e "失败: ${RED}$TESTS_FAILED${NC}"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}所有测试通过! 消息队列集成验证成功。${NC}"
    exit 0
else
    echo -e "${RED}有 $TESTS_FAILED 项测试失败，请检查配置。${NC}"
    exit 1
fi