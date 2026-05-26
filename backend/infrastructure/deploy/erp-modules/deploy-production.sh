#!/bin/bash
# ERP Modules Production Deployment Script
# 用于生产环境部署所有P0 ERP模块

set -e  # 遇到错误立即退出

# 配置变量
PROJECT_NAME="ai-ready-erp"
DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(dirname "$DEPLOY_DIR")/backend/erp"
LOG_DIR="/var/log/ai-ready/erp"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查环境
check_environment() {
    log_info "检查部署环境..."
    
    # 检查Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker未安装，请先安装Docker"
        exit 1
    fi
    
    # 检查Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose未安装，请先安装Docker Compose"
        exit 1
    fi
    
    # 检查Maven
    if ! command -v mvn &> /dev/null; then
        log_error "Maven未安装，请先安装Maven"
        exit 1
    fi
    
    # 检查Java
    if ! command -v java &> /dev/null; then
        log_error "Java未安装，请先安装Java 17"
        exit 1
    fi
    
    # 检查环境变量文件
    if [ ! -f "$DEPLOY_DIR/.env.production" ]; then
        log_warn "生产环境配置文件不存在，创建默认配置"
        create_env_file
    fi
    
    log_info "环境检查通过"
}

# 创建环境配置文件
create_env_file() {
    cat > "$DEPLOY_DIR/.env.production" <<EOF
# 生产环境配置
# 数据库配置
DB_USER=erpadmin
DB_PASSWORD=<CHANGE_ME>
DB_HOST=postgres
DB_PORT=5432
DB_NAME=ai_ready_erp

# Redis配置
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=<CHANGE_ME>

# Grafana配置
GRAFANA_USER=admin
GRAFANA_PASSWORD=<CHANGE_ME>

# JVM配置
JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC

# Spring配置
SPRING_PROFILES_ACTIVE=prod
EOF
    log_info "环境配置文件已创建: $DEPLOY_DIR/.env.production"
    log_warn "请修改配置文件中的密码和敏感信息"
}

# 构建模块
build_modules() {
    log_info "开始构建ERP模块..."
    
    modules=("erp-batch-sn" "erp-invoice" "erp-purchase" "erp-sale" "erp-supplier-portal")
    
    for module in "${modules[@]}"; do
        # 模块名称映射
        case "$module" in
            "erp-purchase")
                actual_module="erp-purchase"
                ;;
            "erp-sale")
                actual_module="erp-sale"
                ;;
            "erp-supplier-portal")
                actual_module="erp-supplier-portal"
                ;;
            *)
                actual_module="$module"
                ;;
        esac
        module_dir="$BACKEND_DIR/$actual_module"
        
        if [ -d "$module_dir" ]; then
            log_info "构建模块: $module"
            
            # 检查pom.xml是否存在
            if [ -f "$module_dir/pom.xml" ]; then
                cd "$module_dir"
                
                # Maven构建
                mvn clean package -DskipTests -Pprod
                
                if [ $? -eq 0 ]; then
                    log_info "模块 $module 构建成功"
                else
                    log_error "模块 $module 构建失败"
                    exit 1
                fi
            else
                log_warn "模块 $module 缺少pom.xml，跳过构建"
            fi
        else
            log_warn "模块目录不存在: $module_dir"
        fi
    done
    
    log_info "所有模块构建完成"
}

# 构建Docker镜像
build_docker_images() {
    log_info "开始构建Docker镜像..."
    
    modules=("erp-batch-sn" "erp-invoice" "erp-purchase" "erp-sale" "erp-supplier-portal")
    
    for module in "${modules[@]}"; do
        # 模块名称映射
        case "$module" in
            "erp-purchase")
                docker_module="erp-purchase-exchange"
                actual_module="erp-purchase"
                ;;
            "erp-sale")
                docker_module="erp-sales-exchange"
                actual_module="erp-sale"
                ;;
            "erp-supplier-portal")
                docker_module="supplier-portal"
                actual_module="erp-supplier-portal"
                ;;
            *)
                docker_module="$module"
                actual_module="$module"
                ;;
        esac
        dockerfile="$DEPLOY_DIR/$docker_module/Dockerfile"
        module_dir="$BACKEND_DIR/$actual_module"
        
        if [ -f "$dockerfile" ] && [ -d "$module_dir" ]; then
            log_info "构建Docker镜像: $module"
            
            docker build \
                -t "ai-ready/$module:latest" \
                -t "ai-ready/$module:$TIMESTAMP" \
                -f "$dockerfile" \
                "$module_dir"
            
            if [ $? -eq 0 ]; then
                log_info "Docker镜像 $module 构建成功"
            else
                log_error "Docker镜像 $module 构建失败"
                exit 1
            fi
        else
            log_warn "跳过模块 $module (Dockerfile或代码目录不存在)"
        fi
    done
    
    log_info "所有Docker镜像构建完成"
}

# 停止现有服务
stop_services() {
    log_info "停止现有服务..."
    
    cd "$DEPLOY_DIR"
    
    # 使用生产环境配置
    docker-compose --env-file .env.production down
    
    log_info "现有服务已停止"
}

# 启动服务
start_services() {
    log_info "启动生产环境服务..."
    
    cd "$DEPLOY_DIR"
    
    # 创建日志目录
    mkdir -p "$LOG_DIR"
    
    # 使用生产环境配置启动
    docker-compose --env-file .env.production up -d
    
    log_info "服务启动完成"
    
    # 等待服务健康检查
    wait_for_health_check
}

# 等待健康检查
wait_for_health_check() {
    log_info "等待服务健康检查..."
    
    max_wait=300  # 最大等待时间5分钟
    wait_interval=10
    elapsed=0
    
    modules=("erp-batch-sn" "erp-invoice" "erp-purchase-exchange" "erp-sales-exchange")
    
    for module in "${modules[@]}"; do
        container_name="ai-ready-$module"
        
        while [ $elapsed -lt $max_wait ]; do
            status=$(docker inspect --format='{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "not_found")
            
            if [ "$status" == "healthy" ]; then
                log_info "模块 $module 健康检查通过"
                break
            elif [ "$status" == "not_found" ]; then
                log_warn "容器 $container_name 未找到，跳过健康检查"
                break
            fi
            
            sleep $wait_interval
            elapsed=$((elapsed + wait_interval))
            
            if [ $elapsed -ge $max_wait ]; then
                log_error "模块 $module 健康检查超时"
            fi
        done
    done
    
    log_info "健康检查完成"
}

# 验证部署
verify_deployment() {
    log_info "验证部署状态..."
    
    # 检查容器状态
    containers=$(docker-compose --env-file .env.production ps -q)
    
    for container in $containers; do
        name=$(docker inspect --format='{{.Name}}' "$container" | sed 's/^///')
        status=$(docker inspect --format='{{.State.Status}}' "$container")
        
        if [ "$status" == "running" ]; then
            log_info "容器 $name 运行正常"
        else
            log_error "容器 $name 状态异常: $status"
        fi
    done
    
    # 测试API端点
    test_api_endpoints
}

# 测试API端点
test_api_endpoints() {
    log_info "测试API端点..."
    
    modules=("erp-batch-sn:8081" "erp-invoice:8082" "erp-purchase-exchange:8083" "erp-sales-exchange:8084")
    
    for module_port in "${modules[@]}"; do
        module=$(echo "$module_port" | cut -d':' -f1)
        port=$(echo "$module_port" | cut -d':' -f2)
        
        # 测试健康检查端点
        health_url="http://localhost:$port/actuator/health"
        response=$(curl -s -o /dev/null -w "%{http_code}" "$health_url" 2>/dev/null || echo "000")
        
        if [ "$response" == "200" ]; then
            log_info "模块 $module API健康检查通过 (HTTP $response)"
        else
            log_warn "模块 $module API健康检查失败 (HTTP $response)"
        fi
    done
}

# 备份旧版本
backup_old_version() {
    log_info "备份旧版本..."
    
    backup_dir="$DEPLOY_DIR/backups/$TIMESTAMP"
    mkdir -p "$backup_dir"
    
    # 备份Docker镜像
    modules=("erp-batch-sn" "erp-invoice" "erp-purchase" "erp-sale" "erp-supplier-portal")
    
    for module in "${modules[@]}"; do
        # 模块名称映射
        case "$module" in
            "erp-purchase")
                docker_module="erp-purchase-exchange"
                ;;
            "erp-sale")
                docker_module="erp-sales-exchange"
                ;;
            "erp-supplier-portal")
                docker_module="supplier-portal"
                ;;
            *)
                docker_module="$module"
                ;;
        esac
        # 检查镜像是否存在
        if docker images | grep -q "ai-ready/$docker_module"; then
            log_info "备份镜像: $docker_module"
            docker save "ai-ready/$docker_module:latest" -o "$backup_dir/$docker_module-latest.tar"
        fi
    done
    
    log_info "备份完成: $backup_dir"
}

# 主部署流程
main() {
    log_info "========================================="
    log_info "开始ERP模块生产环境部署"
    log_info "部署时间: $TIMESTAMP"
    log_info "========================================="
    
    # 1. 检查环境
    check_environment
    
    # 2. 备份旧版本
    backup_old_version
    
    # 3. 停止现有服务
    stop_services
    
    # 4. 构建模块
    build_modules
    
    # 5. 构建Docker镜像
    build_docker_images
    
    # 6. 启动服务
    start_services
    
    # 7. 验证部署
    verify_deployment
    
    log_info "========================================="
    log_info "ERP模块生产环境部署完成"
    log_info "========================================="
    
    # 输出部署摘要
    echo ""
    log_info "部署摘要:"
    log_info "  - PostgreSQL: localhost:5432"
    log_info "  - Redis: localhost:6379"
    log_info "  - Batch/SN: localhost:8081"
    log_info "  - Invoice: localhost:8082"
    log_info "  - Purchase Exchange: localhost:8083"
    log_info "  - Sales Exchange: localhost:8084"
    log_info "  - Prometheus: localhost:9090"
    log_info "  - Grafana: localhost:3000"
    log_info "  - 日志目录: $LOG_DIR"
    log_info "  - 备份目录: $DEPLOY_DIR/backups/$TIMESTAMP"
    echo ""
    log_warn "请检查配置文件: $DEPLOY_DIR/.env.production"
    log_warn "请修改所有敏感信息和密码"
}

# 执行主流程
main