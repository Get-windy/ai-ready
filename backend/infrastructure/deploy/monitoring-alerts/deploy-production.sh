#!/bin/bash
# 监控告警模块生产环境部署脚本
# 文件名: deploy-production.sh
# 版本: v1.0.0
# 创建日期: 2026-04-29
# 描述: 部署监控告警模块到生产环境

set -e  # 遇到错误立即退出

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$SCRIPT_DIR"
BACKEND_DIR="$(dirname "$DEPLOY_DIR")/../backend"

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

# 检查Maven（用于构建Java应用）
check_maven() {
    log_info "检查Maven环境..."
    
    if ! mvn --version &> /dev/null; then
        log_warning "Maven未安装，跳过构建步骤"
        return 1
    fi
    
    log_success "Maven环境检查通过"
    return 0
}

# 检查Java
check_java() {
    log_info "检查Java环境..."
    
    if ! java -version &> /dev/null; then
        log_error "Java未安装，请先安装Java 17+"
        exit 1
    fi
    
    # 检查Java版本
    java_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$java_version" -lt 17 ]; then
        log_error "Java版本过低，需要Java 17或更高版本"
        exit 1
    fi
    
    log_success "Java环境检查通过"
}

# 创建必要的目录
create_directories() {
    log_info "创建必要的目录..."
    
    mkdir -p "$DEPLOY_DIR/logs/monitoring-api"
    mkdir -p "$DEPLOY_DIR/nginx/conf.d"
    mkdir -p "$DEPLOY_DIR/nginx/ssl"
    mkdir -p "$DEPLOY_DIR/nginx/html"
    mkdir -p "$DEPLOY_DIR/custom-exporter/config"
    mkdir -p "$DEPLOY_DIR/init-scripts"
    
    log_success "目录创建完成"
}

# 构建监控模块
build_monitoring_module() {
    log_info "开始构建监控模块..."
    
    monitoring_dir="$BACKEND_DIR/monitoring"
    
    if [ -d "$monitoring_dir" ]; then
        cd "$monitoring_dir"
        if [ -f "pom.xml" ]; then
            log_info "构建监控模块..."
            mvn clean package -DskipTests -Pprod
            log_success "监控模块构建完成"
        else
            log_warning "监控模块缺少pom.xml，跳过构建"
        fi
    else
        log_warning "监控模块目录不存在，跳过构建"
    fi
    
    cd "$DEPLOY_DIR"
}

# 构建Docker镜像
build_docker_images() {
    log_info "开始构建Docker镜像..."
    
    # 构建自定义导出器
    if [ -f "$DEPLOY_DIR/../backend/monitoring/src/main/docker/custom-exporter/Dockerfile" ]; then
        log_info "构建自定义导出器镜像..."
        docker build -t ai-ready/custom-exporter:latest \
            -f "$DEPLOY_DIR/../backend/monitoring/src/main/docker/custom-exporter/Dockerfile" \
            "$DEPLOY_DIR/../backend/monitoring"
        log_success "自定义导出器镜像构建完成"
    else
        log_warning "自定义导出器Dockerfile不存在，跳过构建"
    fi
    
    # 构建监控API服务
    if [ -f "$DEPLOY_DIR/../backend/monitoring/src/main/docker/custom-exporter/Dockerfile" ]; then
        log_info "构建监控API服务镜像..."
        docker build -t ai-ready/monitoring-api:latest \
            -f "$DEPLOY_DIR/../backend/monitoring/src/main/docker/custom-exporter/Dockerfile" \
            "$DEPLOY_DIR/../backend/monitoring"
        log_success "监控API服务镜像构建完成"
    else
        log_warning "监控API服务Dockerfile不存在，跳过构建"
    fi
    
    log_success "Docker镜像构建完成"
}

# 备份旧版本
backup_old_version() {
    log_info "备份旧版本..."
    
    timestamp=$(date +"%Y%m%d_%H%M%S")
    backup_dir="$DEPLOY_DIR/backups/$timestamp"
    mkdir -p "$backup_dir"
    
    # 备份Docker镜像
    if docker images | grep -q "ai-ready/custom-exporter"; then
        log_info "备份自定义导出器镜像"
        docker save "ai-ready/custom-exporter:latest" -o "$backup_dir/custom-exporter-latest.tar"
    fi
    
    if docker images | grep -q "ai-ready/monitoring-api"; then
        log_info "备份监控API服务镜像"
        docker save "ai-ready/monitoring-api:latest" -o "$backup_dir/monitoring-api-latest.tar"
    fi
    
    log_success "备份完成"
}

# 启动服务
start_services() {
    log_info "启动监控告警服务..."
    
    # 使用生产环境配置启动
    docker-compose --env-file .env.production up -d
    
    log_success "服务启动完成"
}

# 验证部署
validate_deployment() {
    log_info "验证部署..."
    
    # 检查容器状态
    if docker-compose --env-file .env.production ps | grep -q "Up"; then
        log_success "所有容器正常运行"
    else
        log_error "部分容器未正常运行"
        return 1
    fi
    
    # 检查健康端点
    services=("prometheus" "alertmanager" "grafana" "monitoring-api" "node-exporter")
    ports=(9090 9093 3000 8081 9100)
    
    for i in "${!services[@]}"; do
        service="${services[$i]}"
        port="${ports[$i]}"
        
        if curl -f "http://localhost:$port" > /dev/null 2>&1; then
            log_success "$service 服务健康检查通过"
        else
            log_warning "$service 服务健康检查失败"
        fi
    done
    
    log_success "部署验证完成"
}

# 主函数
main() {
    log_info "开始监控告警模块生产环境部署..."
    
    # 环境检查
    check_docker
    check_java
    maven_available=$(check_maven)
    
    # 创建目录
    create_directories
    
    # 构建模块（如果Maven可用）
    if [ $? -eq 0 ]; then
        build_monitoring_module
    fi
    
    # 构建Docker镜像
    build_docker_images
    
    # 备份旧版本
    backup_old_version
    
    # 启动服务
    start_services
    
    # 验证部署
    validate_deployment
    
    log_success "监控告警模块生产环境部署完成！"
    log_info "访问地址:"
    log_info "  - Prometheus: http://localhost:9090"
    log_info "  - AlertManager: http://localhost:9093"
    log_info "  - Grafana: http://localhost:3000 (用户名: admin, 密码: 请查看.env.production文件)"
    log_info "  - 监控API: http://localhost:8081"
}

# 执行主函数
main "$@"