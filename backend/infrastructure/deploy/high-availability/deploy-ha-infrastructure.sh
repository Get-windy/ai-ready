#!/bin/bash

# AI-Ready 测试环境高可用架构部署脚本
# 版本: 1.0.0
# 描述: 一键部署测试环境高可用架构

set -euo pipefail

# 颜色输出
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
    if ! command -v "$1" &> /dev/null; then
        log_error "命令 '$1' 未找到，请先安装"
        exit 1
    fi
}

# 显示帮助信息
show_help() {
    cat << EOF
AI-Ready 测试环境高可用架构部署脚本

用法: $0 [选项] [操作]

选项:
  -h, --help      显示此帮助信息
  -e, --env ENV   指定部署环境 (dev/test/prod，默认: test)
  -v, --verbose   显示详细输出

操作:
  setup          安装所有高可用组件
  start          启动所有高可用服务
  stop           停止所有高可用服务
  restart        重启所有高可用服务
  status         查看服务状态
  health         执行健康检查
  cleanup        清理所有服务和数据
  validate       验证架构配置

示例:
  $0 setup
  $0 start
  $0 status
  $0 validate
EOF
}

# 检查依赖
check_dependencies() {
    log_info "检查系统依赖..."
    
    local required_commands=("docker" "docker-compose" "curl" "jq")
    
    for cmd in "${required_commands[@]}"; do
        check_command "$cmd"
    done
    
    # 检查Docker Compose版本
    local compose_version
    compose_version=$(docker-compose --version | grep -oP '\d+\.\d+\.\d+')
    if [ "$(echo "$compose_version" | cut -d. -f1)" -lt 1 ] || [ "$(echo "$compose_version" | cut -d. -f2)" -lt 28 ]; then
        log_warning "Docker Compose 版本过低 (当前: $compose_version)，建议升级到 1.28+"
    fi
    
    # 检查Docker是否运行
    if ! docker info &> /dev/null; then
        log_error "Docker 守护进程未运行"
        exit 1
    fi
    
    log_success "所有依赖检查通过"
}

# 创建网络
create_networks() {
    log_info "创建Docker网络..."
    
    # 创建高可用网络
    if ! docker network ls | grep -q "ai-ready-ha-network"; then
        docker network create \
            --driver bridge \
            --subnet 172.31.0.0/16 \
            --gateway 172.31.0.1 \
            ai-ready-ha-network
        log_success "创建网络: ai-ready-ha-network"
    else
        log_info "网络已存在: ai-ready-ha-network"
    fi
    
    # 检查测试环境网络是否存在
    if ! docker network ls | grep -q "ai-ready-test-network"; then
        log_warning "测试环境网络不存在，请先部署测试环境基础设施"
    fi
    
    log_success "网络创建完成"
}

# 生成SSL证书
generate_ssl_certs() {
    log_info "生成SSL证书..."
    
    local cert_dir="./ssl-certs"
    mkdir -p "$cert_dir"
    
    if [ ! -f "$cert_dir/ai-ready-test.crt" ] || [ ! -f "$cert_dir/ai-ready-test.key" ]; then
        log_info "生成自签名SSL证书..."
        
        # 生成CA私钥
        openssl genrsa -out "$cert_dir/ca.key" 4096 2>/dev/null || {
            log_error "生成CA私钥失败"
            return 1
        }
        
        # 生成CA证书
        openssl req -new -x509 -days 3650 -key "$cert_dir/ca.key" -out "$cert_dir/ca.crt" \
            -subj "/C=CN/ST=Shanghai/L=Shanghai/O=AI-Ready/CN=AI-Ready CA" 2>/dev/null
        
        # 生成服务器私钥
        openssl genrsa -out "$cert_dir/ai-ready-test.key" 2048 2>/dev/null
        
        # 生成证书签名请求
        openssl req -new -key "$cert_dir/ai-ready-test.key" -out "$cert_dir/ai-ready-test.csr" \
            -subj "/C=CN/ST=Shanghai/L=Shanghai/O=AI-Ready/CN=ai-ready-test.local" 2>/dev/null
        
        # 生成服务器证书
        openssl x509 -req -days 365 -in "$cert_dir/ai-ready-test.csr" -CA "$cert_dir/ca.crt" -CAkey "$cert_dir/ca.key" \
            -CAcreateserial -out "$cert_dir/ai-ready-test.crt" 2>/dev/null
        
        # 设置权限
        chmod 644 "$cert_dir"/*.crt "$cert_dir"/*.key
        
        log_success "SSL证书生成完成"
    else
        log_info "SSL证书已存在"
    fi
}

# 部署负载均衡器
deploy_loadbalancer() {
    log_info "部署负载均衡器..."
    
    cd "$SCRIPT_DIR"
    
    log_info "启动Nginx负载均衡器..."
    docker-compose -f docker-compose-loadbalancer.yml up -d
    
    # 等待服务启动
    sleep 10
    
    # 检查服务状态
    if curl -f http://localhost:80/health > /dev/null 2>&1; then
        log_success "负载均衡器部署成功"
    else
        log_error "负载均衡器健康检查失败"
        return 1
    fi
    
    cd - > /dev/null
}

# 部署数据库高可用
deploy_database_ha() {
    log_info "部署数据库高可用..."
    
    cd "$SCRIPT_DIR/postgresql-ha"
    
    log_info "启动PostgreSQL高可用集群..."
    docker-compose -f docker-compose-postgresql-ha.yml up -d
    
    # 等待服务启动
    sleep 15
    
    # 检查主节点
    if docker exec ai-ready-postgres-master pg_isready -U ai_ready_user -d ai_ready > /dev/null 2>&1; then
        log_success "PostgreSQL主节点启动成功"
    else
        log_error "PostgreSQL主节点启动失败"
        return 1
    fi
    
    cd - > /dev/null
}

# 部署Redis高可用
deploy_redis_ha() {
    log_info "部署Redis高可用..."
    
    cd "$SCRIPT_DIR/redis-ha"
    
    log_info "启动Redis哨兵集群..."
    docker-compose -f docker-compose-redis-ha.yml up -d
    
    # 等待服务启动
    sleep 10
    
    # 检查主节点
    if docker exec ai-ready-redis-master redis-cli -a redis_password_2026 ping | grep -q PONG; then
        log_success "Redis主节点启动成功"
    else
        log_error "Redis主节点启动失败"
        return 1
    fi
    
    cd - > /dev/null
}

# 启动所有服务
start_all_services() {
    log_info "启动所有高可用服务..."
    
    create_networks
    generate_ssl_certs
    
    # 按顺序启动服务
    deploy_database_ha
    deploy_redis_ha
    deploy_loadbalancer
    
    log_success "所有高可用服务启动完成"
}

# 停止所有服务
stop_all_services() {
    log_info "停止所有高可用服务..."
    
    log_info "停止负载均衡器..."
    cd "$SCRIPT_DIR"
    docker-compose -f docker-compose-loadbalancer.yml down
    
    log_info "停止Redis高可用集群..."
    cd "$SCRIPT_DIR/redis-ha"
    docker-compose -f docker-compose-redis-ha.yml down
    
    log_info "停止数据库高可用集群..."
    cd "$SCRIPT_DIR/postgresql-ha"
    docker-compose -f docker-compose-postgresql-ha.yml down
    
    cd - > /dev/null
    
    log_success "所有高可用服务已停止"
}

# 重启所有服务
restart_all_services() {
    log_info "重启所有高可用服务..."
    
    stop_all_services
    sleep 5
    start_all_services
    
    log_success "所有高可用服务重启完成"
}

# 检查服务状态
check_service_status() {
    log_info "检查服务状态..."
    
    echo -e "\n${BLUE}=== 负载均衡器状态 ===${NC}"
    if curl -f http://localhost:80/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Nginx负载均衡器: 运行正常${NC}"
    else
        echo -e "${RED}✗ Nginx负载均衡器: 未运行${NC}"
    fi
    
    echo -e "\n${BLUE}=== 数据库状态 ===${NC}"
    if docker exec ai-ready-postgres-master pg_isready -U ai_ready_user -d ai_ready > /dev/null 2>&1; then
        echo -e "${GREEN}✓ PostgreSQL主节点: 运行正常${NC}"
    else
        echo -e "${RED}✗ PostgreSQL主节点: 未运行${NC}"
    fi
    
    echo -e "\n${BLUE}=== Redis状态 ===${NC}"
    if docker exec ai-ready-redis-master redis-cli -a redis_password_2026 ping | grep -q PONG; then
        echo -e "${GREEN}✓ Redis主节点: 运行正常${NC}"
    else
        echo -e "${RED}✗ Redis主节点: 未运行${NC}"
    fi
    
    echo -e "\n${BLUE}=== 网络状态 ===${NC}"
    docker network ls | grep -E "(ai-ready-ha|ai-ready-test)"
    
    echo -e "\n${BLUE}=== 容器状态 ===${NC}"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep "ai-ready"
}

# 执行健康检查
perform_health_check() {
    log_info "执行健康检查..."
    
    local health_passed=true
    
    # 负载均衡器健康检查
    if curl -f http://localhost:80/health > /dev/null 2>&1; then
        log_success "负载均衡器健康检查通过"
    else
        log_error "负载均衡器健康检查失败"
        health_passed=false
    fi
    
    # 数据库健康检查
    if docker exec ai-ready-postgres-master pg_isready -U ai_ready_user -d ai_ready > /dev/null 2>&1; then
        log_success "PostgreSQL健康检查通过"
    else
        log_error "PostgreSQL健康检查失败"
        health_passed=false
    fi
    
    # Redis健康检查
    if docker exec ai-ready-redis-master redis-cli -a redis_password_2026 ping | grep -q PONG; then
        log_success "Redis健康检查通过"
    else
        log_error "Redis健康检查失败"
        health_passed=false
    fi
    
    # 监控系统健康检查
    if curl -f http://localhost:9090/-/healthy > /dev/null 2>&1; then
        log_success "Prometheus健康检查通过"
    else
        log_warning "Prometheus健康检查失败（可能是未部署）"
    fi
    
    if $health_passed; then
        log_success "所有健康检查通过"
    else
        log_error "部分健康检查失败"
        return 1
    fi
}

# 验证架构配置
validate_architecture() {
    log_info "验证高可用架构配置..."
    
    local validation_passed=true
    
    # 验证配置文件存在
    local required_files=(
        "docker-compose-loadbalancer.yml"
        "postgresql-ha/docker-compose-postgresql-ha.yml"
        "redis-ha/docker-compose-redis-ha.yml"
        "nginx-loadbalancer.conf"
        "nginx-loadbalancer-backup.conf"
    )
    
    for file in "${required_files[@]}"; do
        if [ -f "$SCRIPT_DIR/$file" ]; then
            log_success "配置文件存在: $file"
        else
            log_error "配置文件缺失: $file"
            validation_passed=false
        fi
    done
    
    # 验证Docker Compose配置语法
    log_info "验证Docker Compose配置语法..."
    if cd "$SCRIPT_DIR" && docker-compose -f docker-compose-loadbalancer.yml config > /dev/null 2>&1; then
        log_success "负载均衡器配置语法正确"
    else
        log_error "负载均衡器配置语法错误"
        validation_passed=false
    fi
    
    if cd "$SCRIPT_DIR/postgresql-ha" && docker-compose -f docker-compose-postgresql-ha.yml config > /dev/null 2>&1; then
        log_success "数据库配置语法正确"
    else
        log_error "数据库配置语法错误"
        validation_passed=false
    fi
    
    if cd "$SCRIPT_DIR/redis-ha" && docker-compose -f docker-compose-redis-ha.yml config > /dev/null 2>&1; then
        log_success "Redis配置语法正确"
    else
        log_error "Redis配置语法错误"
        validation_passed=false
    fi
    
    if $validation_passed; then
        log_success "架构配置验证通过"
    else
        log_error "架构配置验证失败"
        return 1
    fi
}

# 清理所有服务
cleanup_all() {
    log_warning "这将清理所有高可用服务和数据，是否继续？ (y/N)"
    read -r response
    if [[ "$response" =~ ^[Yy]$ ]]; then
        log_info "开始清理..."
        
        stop_all_services
        
        # 删除网络
        log_info "删除Docker网络..."
        docker network rm ai-ready-ha-network 2>/dev/null || true
        
        # 删除数据卷
        log_info "删除数据卷..."
        docker volume rm \
            redis-master-data redis-slave1-data redis-slave2-data \
            postgres-master-data postgres-replica1-data postgres-replica2-data \
            postgres-backup nginx-logs nginx-config nginx-static 2>/dev/null || true
        
        # 清理本地文件
        log_info "清理本地文件..."
        rm -rf ./ssl-certs
        
        log_success "清理完成"
    else
        log_info "清理操作已取消"
    fi
}

# 主函数
main() {
    # 获取脚本目录
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    
    # 解析参数
    local operation=""
    local verbose=false
    local environment="test"
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -e|--env)
                environment="$2"
                shift 2
                ;;
            -v|--verbose)
                verbose=true
                shift
                ;;
            *)
                operation="$1"
                shift
                ;;
        esac
    done
    
    # 设置详细模式
    if $verbose; then
        set -x
    fi
    
    # 检查依赖
    check_dependencies
    
    # 执行操作
    case "$operation" in
        setup)
            log_info "开始部署高可用架构..."
            check_dependencies
            start_all_services
            ;;
        start)
            log_info "启动高可用服务..."
            start_all_services
            ;;
        stop)
            log_info "停止高可用服务..."
            stop_all_services
            ;;
        restart)
            log_info "重启高可用服务..."
            restart_all_services
            ;;
        status)
            check_service_status
            ;;
        health)
            perform_health_check
            ;;
        cleanup)
            cleanup_all
            ;;
        validate)
            validate_architecture
            ;;
        "")
            log_error "未指定操作"
            show_help
            exit 1
            ;;
        *)
            log_error "未知操作: $operation"
            show_help
            exit 1
            ;;
    esac
}

# 运行主函数
main "$@"