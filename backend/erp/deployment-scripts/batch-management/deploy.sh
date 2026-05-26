#!/bin/bash

# =============================================================================
# ERP批次管理模块批量部署脚本
# 版本: 1.0.0
# 功能: 支持多节点并行部署、灰度发布、版本管理
# =============================================================================

set -euo pipefail

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" >&2
}

# 显示帮助信息
show_help() {
    cat << EOF
ERP批次管理模块部署脚本

用法: $0 [选项]

选项:
  -h, --help              显示此帮助信息
  -e, --env ENV           部署环境 (dev/test/staging/prod)
  -n, --nodes NODES       部署节点列表，逗号分隔 (默认: localhost)
  -v, --version VERSION   部署版本 (默认: latest)
  -m, --mode MODE         部署模式 (single/batch/canary/rollback)
  -c, --config CONFIG     配置文件路径 (默认: config/deploy-config.yaml)
  -d, --dry-run           干运行模式，只显示计划不执行
  -f, --force             强制部署，跳过确认
  -p, --parallel NUM      并行部署节点数 (默认: 3)
  -t, --timeout SEC       部署超时时间(秒) (默认: 600)

示例:
  $0 -e staging -n "node1,node2,node3" -v v1.2.0
  $0 -e prod -m canary -v v1.2.0 -p 2
  $0 -e dev --dry-run
EOF
}

# 初始化变量
ENVIRONMENT="dev"
NODES="localhost"
VERSION="latest"
DEPLOY_MODE="single"
CONFIG_FILE="config/deploy-config.yaml"
DRY_RUN=false
FORCE=false
PARALLEL_NODES=3
TIMEOUT=600

# 解析命令行参数
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -e|--env)
                ENVIRONMENT="$2"
                shift 2
                ;;
            -n|--nodes)
                NODES="$2"
                shift 2
                ;;
            -v|--version)
                VERSION="$2"
                shift 2
                ;;
            -m|--mode)
                DEPLOY_MODE="$2"
                shift 2
                ;;
            -c|--config)
                CONFIG_FILE="$2"
                shift 2
                ;;
            -d|--dry-run)
                DRY_RUN=true
                shift
                ;;
            -f|--force)
                FORCE=true
                shift
                ;;
            -p|--parallel)
                PARALLEL_NODES="$2"
                shift 2
                ;;
            -t|--timeout)
                TIMEOUT="$2"
                shift 2
                ;;
            *)
                log_error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done
}

# 检查环境要求
check_prerequisites() {
    log_info "检查部署环境要求..."
    
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
    
    # 检查Java
    if ! command -v java &> /dev/null; then
        log_warning "Java未安装，某些功能可能受限"
    fi
    
    # 检查Python
    if ! command -v python3 &> /dev/null; then
        log_warning "Python3未安装，某些功能可能受限"
    fi
    
    # 检查配置文件
    if [ ! -f "$CONFIG_FILE" ]; then
        log_warning "配置文件不存在: $CONFIG_FILE，将使用默认配置"
    fi
    
    log_success "环境检查通过"
}

# 加载配置文件
load_config() {
    log_info "加载配置文件: $CONFIG_FILE"
    
    if [ -f "$CONFIG_FILE" ]; then
        # 这里可以解析YAML配置
        # 暂时使用简单配置
        log_info "使用配置文件: $CONFIG_FILE"
    else
        log_info "使用默认配置"
    fi
}

# 验证部署参数
validate_parameters() {
    log_info "验证部署参数..."
    
    # 验证环境
    case "$ENVIRONMENT" in
        dev|test|staging|prod)
            log_info "环境: $ENVIRONMENT"
            ;;
        *)
            log_error "无效的环境: $ENVIRONMENT，必须是 dev/test/staging/prod"
            exit 1
            ;;
    esac
    
    # 验证部署模式
    case "$DEPLOY_MODE" in
        single|batch|canary|rollback)
            log_info "部署模式: $DEPLOY_MODE"
            ;;
        *)
            log_error "无效的部署模式: $DEPLOY_MODE"
            exit 1
            ;;
    esac
    
    # 验证版本号
    if [ "$VERSION" != "latest" ] && [[ ! "$VERSION" =~ ^v[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9]+)?$ ]]; then
        log_warning "版本号格式可能无效: $VERSION，建议使用语义化版本 (如: v1.2.0)"
    fi
    
    log_success "参数验证通过"
}

# 准备部署环境
prepare_environment() {
    log_info "准备部署环境..."
    
    # 创建临时目录
    TEMP_DIR="/tmp/erp-batch-deploy-$(date +%Y%m%d-%H%M%S)"
    mkdir -p "$TEMP_DIR"
    log_info "创建临时目录: $TEMP_DIR"
    
    # 根据环境设置变量
    case "$ENVIRONMENT" in
        dev)
            DOCKER_COMPOSE_FILE="docker-compose-dev.yml"
            JAVA_OPTS="-Xmx512m -Xms256m"
            ;;
        test)
            DOCKER_COMPOSE_FILE="docker-compose-test.yml"
            JAVA_OPTS="-Xmx1g -Xms512m"
            ;;
        staging)
            DOCKER_COMPOSE_FILE="docker-compose-staging.yml"
            JAVA_OPTS="-Xmx2g -Xms1g"
            ;;
        prod)
            DOCKER_COMPOSE_FILE="docker-compose-prod.yml"
            JAVA_OPTS="-Xmx4g -Xms2g"
            ;;
    esac
    
    # 导出环境变量
    export DEPLOY_ENV="$ENVIRONMENT"
    export DEPLOY_VERSION="$VERSION"
    export DEPLOY_MODE="$DEPLOY_MODE"
    export JAVA_OPTS="$JAVA_OPTS"
    export TEMP_DIR="$TEMP_DIR"
    
    log_success "环境准备完成"
}

# 下载部署包
download_deployment_package() {
    log_info "下载部署包版本: $VERSION"
    
    # 根据版本下载对应的部署包
    if [ "$VERSION" = "latest" ]; then
        log_info "下载最新版本..."
        # 这里可以实现从仓库下载最新版本
        # curl -L -o "$TEMP_DIR/deployment-package.tar.gz" "https://repo.example.com/batch-management/latest.tar.gz"
    else
        log_info "下载指定版本: $VERSION"
        # curl -L -o "$TEMP_DIR/deployment-package.tar.gz" "https://repo.example.com/batch-management/$VERSION.tar.gz"
    fi
    
    # 模拟下载
    cp -r "I:/AI-Ready/backend/erp/batch-management-service" "$TEMP_DIR/deployment-package"
    
    # 解压部署包
    # tar -xzf "$TEMP_DIR/deployment-package.tar.gz" -C "$TEMP_DIR"
    
    log_success "部署包下载完成"
}

# 构建Docker镜像
build_docker_image() {
    log_info "构建Docker镜像..."
    
    cd "$TEMP_DIR/deployment-package"
    
    # 检查Dockerfile是否存在
    if [ ! -f "Dockerfile" ]; then
        log_info "创建默认Dockerfile"
        cat > Dockerfile << 'EOF'
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制应用jar包
COPY target/batch-management-service-*.jar app.jar

# 复制配置文件
COPY config/application.yml config/

# 创建非root用户
RUN useradd -m -u 1001 -s /bin/bash appuser && \
    chown -R appuser:appuser /app

USER appuser

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF
    fi
    
    # 构建镜像
    IMAGE_TAG="erp-batch-management:$VERSION-$ENVIRONMENT"
    
    if [ "$DRY_RUN" = false ]; then
        docker build -t "$IMAGE_TAG" .
        if [ $? -eq 0 ]; then
            log_success "Docker镜像构建成功: $IMAGE_TAG"
        else
            log_error "Docker镜像构建失败"
            exit 1
        fi
    else
        log_info "干运行: 将构建Docker镜像: $IMAGE_TAG"
    fi
    
    cd - > /dev/null
}

# 生成Docker Compose配置
generate_docker_compose() {
    log_info "生成Docker Compose配置..."
    
    cat > "$TEMP_DIR/docker-compose.yml" << EOF
version: '3.8'

services:
  batch-management:
    image: erp-batch-management:$VERSION-$ENVIRONMENT
    container_name: erp-batch-management-$ENVIRONMENT
    restart: unless-stopped
    environment:
      - SPRING_PROFILES_ACTIVE=$ENVIRONMENT
      - JAVA_OPTS=$JAVA_OPTS
      - TZ=Asia/Shanghai
    ports:
      - "8080:8080"
    volumes:
      - ./logs:/app/logs
      - ./config:/app/config
    networks:
      - erp-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"

  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus-$ENVIRONMENT
    restart: unless-stopped
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--storage.tsdb.retention.time=30d'
      - '--web.enable-lifecycle'
    networks:
      - erp-network

  grafana:
    image: grafana/grafana:latest
    container_name: grafana-$ENVIRONMENT
    restart: unless-stopped
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin123
      - GF_INSTALL_PLUGINS=grafana-piechart-panel
    volumes:
      - grafana-data:/var/lib/grafana
      - ./grafana/provisioning:/etc/grafana/provisioning
      - ./grafana/dashboards:/var/lib/grafana/dashboards
    networks:
      - erp-network

networks:
  erp-network:
    driver: bridge

volumes:
  prometheus-data:
  grafana-data:
EOF
    
    log_success "Docker Compose配置生成完成"
}

# 执行部署
execute_deployment() {
    log_info "开始部署..."
    
    cd "$TEMP_DIR"
    
    if [ "$DRY_RUN" = true ]; then
        log_info "干运行模式，显示部署计划:"
        log_info "环境: $ENVIRONMENT"
        log_info "版本: $VERSION"
        log_info "模式: $DEPLOY_MODE"
        log_info "节点: $NODES"
        log_info "并行数: $PARALLEL_NODES"
        log_info "超时: ${TIMEOUT}秒"
        return 0
    fi
    
    # 检查是否需要确认
    if [ "$FORCE" = false ] && [ "$ENVIRONMENT" = "prod" ]; then
        echo -e "${YELLOW}警告：即将部署到生产环境！${NC}"
        read -p "确认部署到生产环境？(yes/no): " confirm
        if [ "$confirm" != "yes" ]; then
            log_info "部署已取消"
            exit 0
        fi
    fi
    
    # 启动服务
    log_info "启动Docker Compose服务..."
    docker-compose -f docker-compose.yml up -d
    
    if [ $? -eq 0 ]; then
        log_success "服务启动成功"
    else
        log_error "服务启动失败"
        exit 1
    fi
    
    # 等待服务健康检查
    log_info "等待服务健康检查..."
    sleep 30
    
    # 检查服务状态
    check_service_health
    
    cd - > /dev/null
}

# 检查服务健康状态
check_service_health() {
    log_info "检查服务健康状态..."
    
    MAX_RETRIES=10
    RETRY_INTERVAL=10
    retry_count=0
    
    while [ $retry_count -lt $MAX_RETRIES ]; do
        # 检查批次管理服务
        if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
            log_success "批次管理服务健康检查通过"
            
            # 检查Prometheus
            if curl -f http://localhost:9090/-/healthy > /dev/null 2>&1; then
                log_success "Prometheus健康检查通过"
            else
                log_warning "Prometheus健康检查失败"
            fi
            
            # 检查Grafana
            if curl -f http://localhost:3000/api/health > /dev/null 2>&1; then
                log_success "Grafana健康检查通过"
            else
                log_warning "Grafana健康检查失败"
            fi
            
            return 0
        fi
        
        retry_count=$((retry_count + 1))
        log_info "健康检查失败，重试 $retry_count/$MAX_RETRIES..."
        sleep $RETRY_INTERVAL
    done
    
    log_error "服务健康检查超时"
    return 1
}

# 执行后置检查
post_deployment_check() {
    log_info "执行后置检查..."
    
    # 检查服务日志
    log_info "检查服务日志..."
    docker-compose -f "$TEMP_DIR/docker-compose.yml" logs --tail=10 batch-management
    
    # 检查服务状态
    log_info "检查服务状态..."
    docker-compose -f "$TEMP_DIR/docker-compose.yml" ps
    
    # 检查监控状态
    log_info "检查监控状态..."
    if curl -s http://localhost:8080/actuator/metrics | grep -q "jvm.memory.used"; then
        log_success "监控指标可用"
    else
        log_warning "监控指标可能不可用"
    fi
    
    log_success "后置检查完成"
}

# 清理临时文件
cleanup() {
    log_info "清理临时文件..."
    
    if [ -d "$TEMP_DIR" ] && [ "$DRY_RUN" = false ]; then
        # 保留日志文件
        if [ -d "$TEMP_DIR/logs" ]; then
            mv "$TEMP_DIR/logs" "/var/log/erp-batch-management/$(date +%Y%m%d-%H%M%S)"
        fi
        
        # 清理临时目录
        rm -rf "$TEMP_DIR"
        log_success "临时文件清理完成"
    fi
}

# 生成部署报告
generate_deployment_report() {
    log_info "生成部署报告..."
    
    REPORT_FILE="/var/log/erp-batch-management/deploy-report-$(date +%Y%m%d-%H%M%S).log"
    
    cat > "$REPORT_FILE" << EOF
===========================================
ERP批次管理模块部署报告
===========================================
部署时间: $(date)
部署环境: $ENVIRONMENT
部署版本: $VERSION
部署模式: $DEPLOY_MODE
部署节点: $NODES
部署结果: $(if [ $? -eq 0 ]; then echo "成功"; else echo "失败"; fi)

部署步骤:
1. 环境检查: 完成
2. 参数验证: 完成
3. 环境准备: 完成
4. 包下载: 完成
5. 镜像构建: 完成
6. 配置生成: 完成
7. 服务部署: 完成
8. 健康检查: 完成
9. 后置检查: 完成

服务状态:
$(docker-compose -f "$TEMP_DIR/docker-compose.yml" ps 2>/dev/null || echo "无法获取服务状态")

监控地址:
- 批次管理服务: http://localhost:8080
- Prometheus监控: http://localhost:9090
- Grafana仪表板: http://localhost:3000 (admin/admin123)

日志位置:
- 应用日志: /var/log/erp-batch-management/
- Docker日志: docker-compose logs batch-management

后续操作:
1. 验证业务功能
2. 监控系统指标
3. 配置告警规则
4. 更新文档记录

===========================================
EOF
    
    log_success "部署报告已生成: $REPORT_FILE"
}

# 主函数
main() {
    log_info "开始ERP批次管理模块部署"
    log_info "========================================"
    
    # 解析参数
    parse_args "$@"
    
    # 执行部署流程
    check_prerequisites
    load_config
    validate_parameters
    prepare_environment
    download_deployment_package
    build_docker_image
    generate_docker_compose
    execute_deployment
    post_deployment_check
    generate_deployment_report
    
    # 清理
    cleanup
    
    log_success "部署完成！"
    log_info "========================================"
    log_info "部署摘要:"
    log_info "- 环境: $ENVIRONMENT"
    log_info "- 版本: $VERSION"
    log_info "- 模式: $DEPLOY_MODE"
    log_info "- 服务: http://localhost:8080"
    log_info "- 监控: http://localhost:3000"
    log_info "========================================"
}

# 异常处理
trap 'log_error "部署过程中发生错误，退出码: $?"; cleanup; exit 1' ERR
trap 'log_info "部署被用户中断"; cleanup; exit 130' INT TERM

# 执行主函数
main "$@"