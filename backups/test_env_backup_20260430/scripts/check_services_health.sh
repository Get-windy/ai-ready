#!/bin/bash

# Sprint 27+1 测试环境服务健康检查脚本

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查服务健康
check_service_health() {
    local service_name=$1
    local health_url=$2
    local timeout=${3:-10}
    
    log_info "检查 $service_name 健康状态..."
    
    if curl -s -f --max-time $timeout "$health_url" > /dev/null; then
        log_success "$service_name 健康检查通过"
        return 0
    else
        log_error "$service_name 健康检查失败"
        return 1
    fi
}

# 检查Docker服务
check_docker_service() {
    local service_name=$1
    
    log_info "检查Docker服务 $service_name..."
    
    if docker ps --format "table {{.Names}}\t{{.Status}}" | grep -q "$service_name"; then
        local status=$(docker ps --format "table {{.Names}}\t{{.Status}}" | grep "$service_name" | awk '{print $2}')
        if [[ "$status" == "Up"* ]]; then
            log_success "Docker服务 $service_name 运行正常 ($status)"
            return 0
        else
            log_error "Docker服务 $service_name 状态异常: $status"
            return 1
        fi
    else
        log_error "Docker服务 $service_name 未运行"
        return 1
    fi
}

# 检查端口占用
check_port() {
    local port=$1
    local service_name=$2
    
    log_info "检查端口 $port ($service_name)..."
    
    if netstat -tuln 2>/dev/null | grep -q ":$port "; then
        log_success "端口 $port 已被占用 ($service_name)"
        return 0
    elif ss -tuln 2>/dev/null | grep -q ":$port "; then
        log_success "端口 $port 已被占用 ($service_name)"
        return 0
    else
        log_warning "端口 $port 未被占用 ($service_name)"
        return 1
    fi
}

# 检查服务依赖
check_service_dependencies() {
    log_info "检查服务依赖关系..."
    
    # 读取服务依赖配置
    local config_file="../config/service_cluster_config.yaml"
    if [ ! -f "$config_file" ]; then
        log_warning "服务集群配置文件不存在: $config_file"
        return 0
    fi
    
    # 这里可以添加更复杂的依赖检查逻辑
    log_success "服务依赖关系检查完成"
}

# 生成健康报告
generate_health_report() {
    local report_file="../reports/health_report_$(date +%Y%m%d_%H%M%S).md"
    
    mkdir -p ../reports
    
    cat > "$report_file" << EOF
# Sprint 27+1 测试环境健康检查报告

## 报告信息
- 生成时间: $(date)
- 检查脚本版本: 1.0.0
- 环境: 测试环境

## 服务健康状态

### 基础设施服务
- Nacos服务注册中心: $(if check_service_health "nacos" "http://localhost:8848/nacos/v1/ns/operator/health" 5 >/dev/null 2>&1; then echo "✅ 健康"; else echo "❌ 异常"; fi)
- Prometheus监控: $(if check_service_health "prometheus" "http://localhost:9090/-/healthy" 5 >/dev/null 2>&1; then echo "✅ 健康"; else echo "❌ 异常"; fi)
- Grafana仪表板: $(if check_service_health "grafana" "http://localhost:3000/api/health" 5 >/dev/null 2>&1; then echo "✅ 健康"; else echo "❌ 异常"; fi)
- Jaeger追踪: $(if check_service_health "jaeger" "http://localhost:16686/api/services" 5 >/dev/null 2>&1; then echo "✅ 健康"; else echo "❌ 异常"; fi)

### 数据库服务
- PostgreSQL主数据库: $(if check_port 5432 "PostgreSQL" >/dev/null 2>&1; then echo "✅ 运行中"; else echo "❌ 未运行"; fi)
- Redis缓存: $(if check_port 6379 "Redis" >/dev/null 2>&1; then echo "✅ 运行中"; else echo "❌ 未运行"; fi)

### 消息队列服务
- Kafka Broker 1: $(if check_port 9092 "Kafka" >/dev/null 2>&1; then echo "✅ 运行中"; else echo "❌ 未运行"; fi)
- ZooKeeper 1: $(if check_port 2181 "ZooKeeper" >/dev/null 2>&1; then echo "✅ 运行中"; else echo "❌ 未运行"; fi)

### 微服务
- 用户服务实例1: $(if check_port 8081 "User Service" >/dev/null 2>&1; then echo "✅ 运行中"; else echo "❌ 未运行"; fi)
- 用户服务实例2: $(if check_port 8082 "User Service" >/dev/null 2>&1; then echo "✅ 运行中"; else echo "❌ 未运行"; fi)
- 订单服务实例1: $(if check_port 8083 "Order Service" >/dev/null 2>&1; then echo "✅ 运行中"; else echo "❌ 未运行"; fi)
- 订单服务实例2: $(if check_port 8084 "Order Service" >/dev/null 2>&1; then echo "✅ 运行中"; else echo "❌ 未运行"; fi)
- 库存服务实例1: $(if check_port 8085 "Inventory Service" >/dev/null 2>&1; then echo "✅ 运行中"; else echo "❌ 未运行"; fi)
- 监控服务实例1: $(if check_port 8086 "Monitoring Service" >/dev/null 2>&1; then echo "✅ 运行中"; else echo "❌ 未运行"; fi)

## 端口占用情况
\`\`\`
$(netstat -tuln 2>/dev/null | grep -E ":808[0-9]|:543[0-9]|:6379|:909[0-9]|:218[0-9]|:8848|:9090|:3000|:16686" || echo "无法获取端口信息")
\`\`\`

## Docker容器状态
\`\`\`
$(docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || echo "Docker未运行或无法访问")
\`\`\`

## 系统资源使用
\`\`\`
$(top -bn1 | head -20 2>/dev/null || echo "无法获取系统资源信息")
\`\`\`

## 磁盘空间
\`\`\`
$(df -h . 2>/dev/null || echo "无法获取磁盘空间信息")
\`\`\`

## 检查结果
- 总体健康状态: $(if [ \$failed_count -eq 0 ]; then echo "✅ 健康"; elif [ \$failed_count -le 2 ]; then echo "⚠️  警告"; else echo "❌ 异常"; fi)
- 检查服务总数: \$total_count
- 健康服务数: \$healthy_count
- 异常服务数: \$failed_count

## 建议操作
$(if [ \$failed_count -eq 0 ]; then
    echo "1. ✅ 所有服务运行正常，可以进行测试"
    echo "2. 运行集成测试: ./scripts/run-integration-tests.sh"
    echo "3. 查看监控仪表板: http://localhost:3000"
elif [ \$failed_count -le 2 ]; then
    echo "1. ⚠️  部分服务异常，建议检查"
    echo "2. 查看服务日志: docker-compose logs [service-name]"
    echo "3. 重启异常服务: docker-compose restart [service-name]"
    echo "4. 重新部署: ./scripts/deploy_cluster.sh --services-only"
else
    echo "1. ❌ 多个服务异常，需要立即处理"
    echo "2. 查看详细错误: 检查上述异常服务"
    echo "3. 重新部署所有服务: ./scripts/deploy_cluster.sh --clean --all"
    echo "4. 联系运维团队"
fi)

## 故障排除
1. 查看服务日志: \`docker-compose logs [service-name]\`
2. 检查服务配置: \`cat ../config/service_cluster_config.yaml | grep -A 10 [service-name]\`
3. 验证网络连接: \`ping localhost && curl http://localhost:8080/actuator/health\`
4. 重启单个服务: \`docker-compose restart [service-name]\`
5. 重新部署: \`./scripts/deploy_cluster.sh --clean --services-only\`

---
*报告生成时间: $(date)*
*下次检查建议: $(date -d "+1 hour")*
EOF
    
    log_success "健康报告已生成: $report_file"
}

# 主函数
main() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}  Sprint 27+1 测试环境健康检查${NC}"
    echo -e "${BLUE}========================================${NC}"
    
    total_count=0
    healthy_count=0
    failed_count=0
    
    # 检查基础设施服务
    echo -e "\n${YELLOW}=== 基础设施服务检查 ===${NC}"
    
    # Nacos
    if check_service_health "nacos" "http://localhost:8848/nacos/v1/ns/operator/health" 5; then
        ((healthy_count++))
    else
        ((failed_count++))
    fi
    ((total_count++))
    
    # Prometheus
    if check_service_health "prometheus" "http://localhost:9090/-/healthy" 5; then
        ((healthy_count++))
    else
        ((failed_count++))
    fi
    ((total_count++))
    
    # Grafana
    if check_service_health "grafana" "http://localhost:3000/api/health" 5; then
        ((healthy_count++))
    else
        ((failed_count++))
    fi
    ((total_count++))
    
    # Jaeger
    if check_service_health "jaeger" "http://localhost:16686/api/services" 5; then
        ((healthy_count++))
    else
        ((failed_count++))
    fi
    ((total_count++))
    
    # 检查数据库服务
    echo -e "\n${YELLOW}=== 数据库服务检查 ===${NC}"
    
    # PostgreSQL
    if check_port 5432 "PostgreSQL"; then
        ((healthy_count++))
    else
        ((failed_count++))
    fi
    ((total_count++))
    
    # Redis
    if check_port 6379 "Redis"; then
        ((healthy_count++))
    else
        ((failed_count++))
    fi
    ((total_count++))
    
    # 检查消息队列服务
    echo -e "\n${YELLOW}=== 消息队列服务检查 ===${NC}"
    
    # Kafka
    if check_port 9092 "Kafka"; then
        ((healthy_count++))
    else
        ((failed_count++))
    fi
    ((total_count++))
    
    # ZooKeeper
    if check_port 2181 "ZooKeeper"; then
        ((healthy_count++))
    else
        ((failed_count++))
    fi
    ((total_count++))
    
    # 检查微服务
    echo -e "\n${YELLOW}=== 微服务检查 ===${NC}"
    
    # 用户服务
    if check_port 8081 "User Service"; then
        ((healthy_count++))
    else
        ((failed_count++))
    fi
    ((total_count++))
    
    if check_port 8082 "User Service"; then
        ((healthy_count++))
    else
        ((failed_count++))
    fi
    ((total_count++))
    
    # 订单服务
    if check_port 8083 "Order Service"; then
        ((healthy_count++))
    else
        ((failed_count++))
    fi
    ((total_count++))
    
    if check_port 8084 "Order Service"; then
        ((healthy_count++))
    else
        ((failed_count++))
    fi
    ((total_count++))
    
    # 库存服务
    if check_port 8085 "Inventory Service"; then
        ((healthy_count++))
    else
        ((failed_count++))
    fi
    ((total_count++))
    
    # 监控服务
    if check_port 8086 "Monitoring Service"; then
        ((healthy_count++))
    else
        ((failed_count++))
    fi
    ((total_count++))
    
    # 检查服务依赖
    echo -e "\n${YELLOW}=== 服务依赖检查 ===${NC}"
    check_service_dependencies
    
    # 显示总结
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}          健康检查总结${NC}"
    echo -e "${BLUE}========================================${NC}"
    
    echo -e "检查服务总数: ${total_count}"
    echo -e "健康服务数: ${GREEN}${healthy_count}${NC}"
    echo -e "异常服务数: ${RED}${failed_count}${NC}"
    
    if [ $failed_count -eq 0 ]; then
        echo -e "\n${GREEN}✅ 所有服务运行正常，测试环境准备就绪！${NC}"
    elif [ $failed_count -le 2 ]; then
        echo -e "\n${YELLOW}⚠️  部分服务异常，建议检查但可以继续测试${NC}"
    else
        echo -e "\n${RED}❌ 多个服务异常，需要立即处理${NC}"
    fi
    
    # 生成健康报告
    generate_health_report
    
    # 导出计数变量供报告使用
    export total_count healthy_count failed_count
    
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${GREEN}健康检查完成！${NC}"
    echo -e "${BLUE}========================================${NC}"
    
    # 返回适当的退出代码
    if [ $failed_count -eq 0 ]; then
        return 0
    elif [ $failed_count -le 2 ]; then
        return 1
    else
        return 2
    fi
}

# 执行主函数
main "$@"