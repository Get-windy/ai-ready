#!/bin/bash
# AI-Ready测试环境部署脚本
# 文件: deploy-test.sh
# 环境: 测试环境

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

# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        log_error "命令 '$1' 未安装，请先安装"
        exit 1
    fi
}

# 检查Docker和Docker Compose
check_docker() {
    log_info "检查Docker环境..."
    
    if ! docker --version &> /dev/null; then
        log_error "Docker未安装，请先安装Docker"
        exit 1
    fi
    
    if ! docker-compose --version &> /dev/null; then
        log_error "Docker Compose未安装，请先安装Docker Compose"
        exit 1
    fi
    
    log_success "Docker环境检查通过"
}

# 检查端口占用
check_ports() {
    log_info "检查端口占用情况..."
    
    local ports=("5432" "5434" "6379" "6374" "9092" "8080" "8082" "8085" "8086" "8087" "8088" "9090" "3000" "9093")
    local occupied_ports=()
    
    for port in "${ports[@]}"; do
        if lsof -i :$port &> /dev/null; then
            occupied_ports+=($port)
        fi
    done
    
    if [ ${#occupied_ports[@]} -gt 0 ]; then
        log_warning "以下端口已被占用: ${occupied_ports[*]}"
        read -p "是否继续部署？(y/n): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "用户取消部署"
            exit 0
        fi
    else
        log_success "端口检查通过"
    fi
}

# 创建目录结构
create_directories() {
    log_info "确保目录结构存在..."
    
    local directories=(
        "../docker/init-scripts/main"
        "../docker/init-scripts/inventory"
        "../docker/prometheus/rules"
        "../docker/grafana/provisioning/datasources"
        "../docker/alertmanager"
        "../logs/user-service"
        "../logs/order-service"
        "../logs/inventory-service"
        "../logs/crm-service"
        "../logs/erp-service"
    )
    
    for dir in "${directories[@]}"; do
        if [ ! -d "$dir" ]; then
            mkdir -p "$dir"
            log_info "创建目录: $dir"
        fi
    done
    
    log_success "目录结构检查完成"
}

# 启动服务
start_services() {
    log_info "启动AI-Ready测试环境..."
    
    cd ../docker
    
    # 使用测试环境配置
    export SPRING_PROFILES_ACTIVE=test
    
    # 启动所有服务
    docker-compose -f docker-compose.test.yml up -d
    
    log_success "服务启动命令已执行"
}

# 等待服务就绪
wait_for_services() {
    log_info "等待服务就绪..."
    
    local services=("user-service" "order-service" "inventory-service" "crm-service" "erp-service" "postgres-main" "postgres-inventory" "redis-main" "redis-inventory" "kafka" "prometheus" "grafana" "alertmanager")
    local timeout=120
    local interval=10
    local elapsed=0
    
    while [ $elapsed -lt $timeout ]; do
        local ready_count=0
        local total_count=${#services[@]}
        
        for service in "${services[@]}"; do
            if docker-compose -f docker-compose.test.yml ps | grep -q "${service}.*Up"; then
                ((ready_count++))
            fi
        done
        
        if [ $ready_count -eq $total_count ]; then
            log_success "所有服务已就绪 ($ready_count/$total_count)"
            return 0
        fi
        
        log_info "服务就绪状态: $ready_count/$total_count (等待中...)"
        sleep $interval
        elapsed=$((elapsed + interval))
    done
    
    log_warning "超时等待，部分服务可能未完全就绪"
    return 1
}

# 执行健康检查
run_health_check() {
    log_info "执行健康检查..."
    
    local endpoints=(
        "http://localhost:8085/actuator/health:user-service"
        "http://localhost:8086/actuator/health:order-service"
        "http://localhost:8082/actuator/health:inventory-service"
        "http://localhost:8087/actuator/health:crm-service"
        "http://localhost:8088/actuator/health:erp-service"
        "http://localhost:9090/-/healthy:prometheus"
        "http://localhost:3000/api/health:grafana"
        "http://localhost:9093/-/healthy:alertmanager"
    )
    
    local healthy_count=0
    local total_count=${#endpoints[@]}
    
    for endpoint_info in "${endpoints[@]}"; do
        IFS=':' read -r endpoint service_name <<< "$endpoint_info"
        
        if curl -s -f "$endpoint" > /dev/null 2>&1; then
            log_success "✓ $service_name: 健康"
            ((healthy_count++))
        else
            log_error "✗ $service_name: 不健康"
        fi
    done
    
    if [ $healthy_count -eq $total_count ]; then
        log_success "所有服务健康检查通过"
        return 0
    else
        log_warning "部分服务健康检查失败 ($healthy_count/$total_count)"
        return 1
    fi
}

# 显示访问信息
show_access_info() {
    log_info "AI-Ready测试环境部署完成！"
    echo ""
    echo "=========================================="
    echo "访问地址:"
    echo "  用户服务:      http://localhost:8085"
    echo "  订单服务:      http://localhost:8086"
    echo "  库存服务:      http://localhost:8082"
    echo "  CRM服务:       http://localhost:8087"
    echo "  ERP服务:       http://localhost:8088"
    echo "  Prometheus:    http://localhost:9090"
    echo "  Grafana:       http://localhost:3000 (admin/admin_test_2026)"
    echo "  AlertManager:   http://localhost:9093"
    echo ""
    echo "数据库连接:"
    echo "  主数据库:      localhost:5432 (gateway_user/gateway_pass_test_2026)"
    echo "  库存数据库:    localhost:5434 (inventory_user/inventory_pass_test_2026)"
    echo "  Redis主缓存:   localhost:6379"
    echo "  Redis库存缓存: localhost:6374"
    echo "  Kafka:         localhost:9092"
    echo ""
    echo "管理命令:"
    echo "  查看日志:      docker-compose -f docker-compose.test.yml logs -f"
    echo "  停止服务:      docker-compose -f docker-compose.test.yml down"
    echo "  重启服务:      docker-compose -f docker-compose.test.yml restart"
    echo "  查看状态:      docker-compose -f docker-compose.test.yml ps"
    echo "=========================================="
}

# 主函数
main() {
    log_info "开始部署AI-Ready测试环境..."
    
    check_docker
    check_ports
    create_directories
    start_services
    wait_for_services
    run_health_check
    show_access_info
    
    log_success "AI-Ready测试环境部署完成！"
}

# 执行主函数
main "$@"