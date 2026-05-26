#!/bin/bash
# 监控告警模块健康检查脚本
# 使用: ./health-check.sh [环境]

set -e

ENVIRONMENT=${1:-"development"}
COMPOSE_FILE="monitoring-alerting-deployment.yml"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "=========================================="
echo "AI-Ready 监控告警模块健康检查"
echo "环境: $ENVIRONMENT"
echo "时间: $(date)"
echo "=========================================="

# 检查Docker Compose文件
if [ ! -f "$COMPOSE_FILE" ]; then
    echo -e "${RED}错误: 找不到Docker Compose文件: $COMPOSE_FILE${NC}"
    exit 1
fi

# 检查服务状态
check_service_status() {
    echo ""
    echo -e "${BLUE}1. 检查服务运行状态${NC}"
    echo "------------------------------------------"
    
    local services=(
        "postgres-monitoring:PostgreSQL数据库"
        "redis-monitoring:Redis缓存"
        "rabbitmq-monitoring:RabbitMQ消息队列"
        "prometheus-monitoring:Prometheus监控"
        "alertmanager-monitoring:AlertManager告警"
        "grafana-monitoring:Grafana可视化"
        "custom-exporter-monitoring:自定义导出器"
        "monitoring-api-service:监控API服务"
        "nginx-monitoring:Nginx负载均衡"
    )
    
    local all_running=true
    
    for service_info in "${services[@]}"; do
        IFS=':' read -r service_name service_desc <<< "$service_info"
        
        if docker-compose -f "$COMPOSE_FILE" ps | grep -q "${service_name}.*Up"; then
            echo -e "  ${GREEN}✓${NC} ${service_desc}: 运行正常"
        else
            echo -e "  ${RED}✗${NC} ${service_desc}: 未运行"
            all_running=false
        fi
    done
    
    if [ "$all_running" = true ]; then
        echo -e "${GREEN}✅ 所有服务运行正常${NC}"
    else
        echo -e "${YELLOW}⚠️  部分服务未运行${NC}"
    fi
}

# 检查服务健康状态
check_service_health() {
    echo ""
    echo -e "${BLUE}2. 检查服务健康状态${NC}"
    echo "------------------------------------------"
    
    local endpoints=(
        "http://localhost:9090/-/healthy:Prometheus健康检查"
        "http://localhost:9093/-/healthy:AlertManager健康检查"
        "http://localhost:3000/api/health:Grafana健康检查"
        "http://localhost:8081/actuator/health:监控API健康检查"
        "http://localhost/health:Nginx健康检查"
    )
    
    local all_healthy=true
    
    for endpoint_info in "${endpoints[@]}"; do
        IFS=':' read -r endpoint endpoint_desc <<< "$endpoint_info"
        
        if curl -s -f "$endpoint" > /dev/null 2>&1; then
            echo -e "  ${GREEN}✓${NC} ${endpoint_desc}: 健康"
        else
            echo -e "  ${RED}✗${NC} ${endpoint_desc}: 不健康"
            all_healthy=false
        fi
    done
    
    if [ "$all_healthy" = true ]; then
        echo -e "${GREEN}✅ 所有服务健康检查通过${NC}"
    else
        echo -e "${YELLOW}⚠️  部分服务健康检查失败${NC}"
    fi
}

# 检查数据库连接
check_database_connections() {
    echo ""
    echo -e "${BLUE}3. 检查数据库连接${NC}"
    echo "------------------------------------------"
    
    # 检查PostgreSQL
    if docker exec postgres-monitoring pg_isready -U monitoring_user -d monitoring_db > /dev/null 2>&1; then
        echo -e "  ${GREEN}✓${NC} PostgreSQL: 连接正常"
    else
        echo -e "  ${RED}✗${NC} PostgreSQL: 连接失败"
    fi
    
    # 检查Redis
    if docker exec redis-monitoring redis-cli --raw incr ping > /dev/null 2>&1; then
        echo -e "  ${GREEN}✓${NC} Redis: 连接正常"
    else
        echo -e "  ${RED}✗${NC} Redis: 连接失败"
    fi
    
    # 检查RabbitMQ
    if docker exec rabbitmq-monitoring rabbitmq-diagnostics -q ping > /dev/null 2>&1; then
        echo -e "  ${GREEN}✓${NC} RabbitMQ: 连接正常"
    else
        echo -e "  ${RED}✗${NC} RabbitMQ: 连接失败"
    fi
}

# 检查资源使用情况
check_resource_usage() {
    echo ""
    echo -e "${BLUE}4. 检查资源使用情况${NC}"
    echo "------------------------------------------"
    
    local containers=$(docker-compose -f "$COMPOSE_FILE" ps -q)
    
    for container in $containers; do
        local name=$(docker inspect --format '{{.Name}}' "$container" | sed 's/^\///')
        local stats=$(docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}\t{{.NetIO}}\t{{.BlockIO}}" "$container" | tail -n1)
        
        if [ -n "$stats" ]; then
            echo "  $stats"
        fi
    done
}

# 检查监控数据收集
check_monitoring_data() {
    echo ""
    echo -e "${BLUE}5. 检查监控数据收集${NC}"
    echo "------------------------------------------"
    
    # 检查Prometheus目标
    local targets=$(curl -s "http://localhost:9090/api/v1/targets" | jq -r '.data.activeTargets[] | "\(.labels.job): \(.health)"' 2>/dev/null || echo "需要安装jq命令")
    
    if [ -n "$targets" ]; then
        echo "  Prometheus监控目标:"
        echo "$targets" | while read -r target; do
            if [[ "$target" == *"up"* ]]; then
                echo -e "    ${GREEN}✓${NC} $target"
            else
                echo -e "    ${RED}✗${NC} $target"
            fi
        done
    else
        echo -e "  ${YELLOW}⚠️  无法获取监控目标信息${NC}"
    fi
    
    # 检查告警规则
    local rules=$(curl -s "http://localhost:9090/api/v1/rules" | jq -r '.data.groups[] | "\(.name): \(.rules | length) 条规则"' 2>/dev/null || echo "")
    
    if [ -n "$rules" ]; then
        echo ""
        echo "  告警规则组:"
        echo "$rules" | while read -r rule; do
            echo "    $rule"
        done
    fi
}

# 检查日志错误
check_log_errors() {
    echo ""
    echo -e "${BLUE}6. 检查日志错误${NC}"
    echo "------------------------------------------"
    
    local services=("prometheus-monitoring" "grafana-monitoring" "alertmanager-monitoring" "monitoring-api-service")
    local has_errors=false
    
    for service in "${services[@]}"; do
        local error_count=$(docker-compose -f "$COMPOSE_FILE" logs "$service" 2>/dev/null | grep -i "error\|exception\|fail" | wc -l)
        
        if [ "$error_count" -gt 0 ]; then
            echo -e "  ${YELLOW}⚠️  ${service}: 发现 $error_count 个错误/异常${NC}"
            has_errors=true
        else
            echo -e "  ${GREEN}✓${NC} ${service}: 无错误日志"
        fi
    done
    
    if [ "$has_errors" = true ]; then
        echo -e "${YELLOW}⚠️  发现错误日志，建议检查详细日志${NC}"
    else
        echo -e "${GREEN}✅ 日志检查正常${NC}"
    fi
}

# 执行所有检查
check_service_status
check_service_health
check_database_connections
check_resource_usage
check_monitoring_data
check_log_errors

echo ""
echo "=========================================="
echo -e "${GREEN}健康检查完成${NC}"
echo "=========================================="

# 生成报告
generate_report() {
    echo ""
    echo -e "${BLUE}📊 健康检查报告${NC}"
    echo "=========================================="
    
    local timestamp=$(date "+%Y-%m-%d %H:%M:%S")
    local total_services=9
    local running_services=$(docker-compose -f "$COMPOSE_FILE" ps | grep "Up" | wc -l)
    local healthy_endpoints=0
    
    # 计算健康端点数量
    local endpoints=(
        "http://localhost:9090/-/healthy"
        "http://localhost:9093/-/healthy"
        "http://localhost:3000/api/health"
        "http://localhost:8081/actuator/health"
        "http://localhost/health"
    )
    
    for endpoint in "${endpoints[@]}"; do
        if curl -s -f "$endpoint" > /dev/null 2>&1; then
            ((healthy_endpoints++))
        fi
    done
    
    echo "检查时间: $timestamp"
    echo "运行环境: $ENVIRONMENT"
    echo "服务总数: $total_services"
    echo "运行服务: $running_services/$total_services"
    echo "健康端点: $healthy_endpoints/5"
    echo ""
    
    if [ "$running_services" -eq "$total_services" ] && [ "$healthy_endpoints" -eq 5 ]; then
        echo -e "${GREEN}✅ 系统状态: 优秀${NC}"
        echo "所有服务和端点均正常运行"
    elif [ "$running_services" -ge $(($total_services - 2)) ] && [ "$healthy_endpoints" -ge 3 ]; then
        echo -e "${YELLOW}⚠️  系统状态: 良好${NC}"
        echo "大部分服务和端点正常运行"
    else
        echo -e "${RED}❌ 系统状态: 需要关注${NC}"
        echo "多个服务或端点存在问题，建议立即检查"
    fi
}

generate_report

echo ""
echo "管理命令:"
echo "  查看详细日志: docker-compose -f $COMPOSE_FILE logs"
echo "  重启服务: docker-compose -f $COMPOSE_FILE restart"
echo "  停止服务: docker-compose -f $COMPOSE_FILE down"
echo ""
echo "如需定期检查，建议配置cron任务:"
echo "  */15 * * * * cd $(pwd) && ./health-check.sh $ENVIRONMENT >> health-check.log"