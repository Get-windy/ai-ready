#!/bin/bash

# Sprint 27+1 测试环境服务集群部署脚本
# 部署完整的微服务集群，支持多服务并发测试和集成测试

set -e  # 遇到错误立即退出

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
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        log_error "命令 $1 未找到，请先安装"
        exit 1
    fi
}

# 显示帮助信息
show_help() {
    echo "Sprint 27+1 测试环境服务集群部署脚本"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -h, --help             显示此帮助信息"
    echo "  -a, --all              部署所有组件（默认）"
    echo "  -s, --services-only    仅部署微服务"
    echo "  -d, --database-only    仅部署数据库集群"
    echo "  -m, --mq-only          仅部署消息队列"
    echo "  -i, --infra-only       仅部署基础设施"
    echo "  -t, --test-only        仅部署测试环境"
    echo "  -c, --clean            清理环境后重新部署"
    echo "  -v, --validate         验证部署结果"
    echo "  --skip-deps            跳过依赖检查"
    echo ""
}

# 解析命令行参数
parse_args() {
    DEPLOY_ALL=true
    DEPLOY_SERVICES=false
    DEPLOY_DATABASE=false
    DEPLOY_MQ=false
    DEPLOY_INFRA=false
    DEPLOY_TEST=false
    CLEAN_BEFORE=false
    VALIDATE_AFTER=false
    SKIP_DEPS=false
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -a|--all)
                DEPLOY_ALL=true
                shift
                ;;
            -s|--services-only)
                DEPLOY_ALL=false
                DEPLOY_SERVICES=true
                shift
                ;;
            -d|--database-only)
                DEPLOY_ALL=false
                DEPLOY_DATABASE=true
                shift
                ;;
            -m|--mq-only)
                DEPLOY_ALL=false
                DEPLOY_MQ=true
                shift
                ;;
            -i|--infra-only)
                DEPLOY_ALL=false
                DEPLOY_INFRA=true
                shift
                ;;
            -t|--test-only)
                DEPLOY_ALL=false
                DEPLOY_TEST=true
                shift
                ;;
            -c|--clean)
                CLEAN_BEFORE=true
                shift
                ;;
            -v|--validate)
                VALIDATE_AFTER=true
                shift
                ;;
            --skip-deps)
                SKIP_DEPS=true
                shift
                ;;
            *)
                log_error "未知选项: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # 如果指定了--all，则设置所有标志为true
    if [ "$DEPLOY_ALL" = true ]; then
        DEPLOY_SERVICES=true
        DEPLOY_DATABASE=true
        DEPLOY_MQ=true
        DEPLOY_INFRA=true
        DEPLOY_TEST=true
    fi
}

# 检查依赖
check_dependencies() {
    if [ "$SKIP_DEPS" = true ]; then
        log_warning "跳过依赖检查"
        return
    fi
    
    log_info "检查系统依赖..."
    
    # 检查Docker
    check_command docker
    docker_version=$(docker --version | awk '{print $3}' | tr -d ',')
    log_success "Docker 版本: $docker_version"
    
    # 检查Docker Compose
    check_command docker-compose
    docker_compose_version=$(docker-compose --version | awk '{print $3}' | tr -d ',')
    log_success "Docker Compose 版本: $docker_compose_version"
    
    # 检查Java
    check_command java
    java_version=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
    log_success "Java 版本: $java_version"
    
    # 检查Maven
    check_command mvn
    maven_version=$(mvn --version | head -n 1 | awk '{print $3}')
    log_success "Maven 版本: $maven_version"
    
    # 检查Git
    check_command git
    git_version=$(git --version | awk '{print $3}')
    log_success "Git 版本: $git_version"
    
    # 检查curl
    check_command curl
    
    # 检查Python
    check_command python3
    python_version=$(python3 --version | awk '{print $2}')
    log_success "Python 版本: $python_version"
    
    log_success "所有依赖检查通过"
}

# 清理环境
clean_environment() {
    log_info "开始清理测试环境..."
    
    # 停止并删除Docker容器
    if docker-compose -f docker-compose.yml down 2>/dev/null; then
        log_success "Docker容器已停止并删除"
    else
        log_warning "没有运行的Docker容器"
    fi
    
    # 清理Docker资源
    log_info "清理Docker资源..."
    docker system prune -f > /dev/null 2>&1
    
    # 清理临时文件
    log_info "清理临时文件..."
    rm -rf ./logs/* 2>/dev/null || true
    rm -rf ./data/* 2>/dev/null || true
    rm -rf ./tmp/* 2>/dev/null || true
    
    # 清理构建产物
    log_info "清理构建产物..."
    find . -name "target" -type d -exec rm -rf {} + 2>/dev/null || true
    find . -name "build" -type d -exec rm -rf {} + 2>/dev/null || true
    find . -name "*.jar" -type f -delete 2>/dev/null || true
    
    log_success "环境清理完成"
}

# 部署基础设施
deploy_infrastructure() {
    log_info "开始部署基础设施..."
    
    # 创建必要的目录
    log_info "创建目录结构..."
    mkdir -p ./logs
    mkdir -p ./data
    mkdir -p ./config
    mkdir -p ./certs
    
    # 复制配置文件
    log_info "复制配置文件..."
    cp -f ../config/*.yaml ./config/ 2>/dev/null || log_warning "没有找到配置文件"
    cp -f ../config/*.yml ./config/ 2>/dev/null || log_warning "没有找到配置文件"
    
    # 生成SSL证书（如果需要）
    log_info "生成SSL证书..."
    if [ ! -f ./certs/server.key ]; then
        openssl req -x509 -newkey rsa:4096 -keyout ./certs/server.key -out ./certs/server.crt \
            -days 365 -nodes -subj "/C=CN/ST=Beijing/L=Beijing/O=AI-Ready/CN=localhost" 2>/dev/null
        log_success "SSL证书已生成"
    else
        log_info "SSL证书已存在，跳过生成"
    fi
    
    log_success "基础设施部署完成"
}

# 部署数据库集群
deploy_database_cluster() {
    log_info "开始部署数据库集群..."
    
    # 检查PostgreSQL是否在运行
    if docker ps | grep -q postgres; then
        log_warning "PostgreSQL已在运行，跳过部署"
        return
    fi
    
    # 启动PostgreSQL主从集群
    log_info "启动PostgreSQL主从集群..."
    docker-compose -f docker-compose.db.yml up -d
    
    # 等待数据库启动
    log_info "等待数据库启动..."
    sleep 10
    
    # 检查数据库状态
    if docker-compose -f docker-compose.db.yml ps | grep -q "Up"; then
        log_success "数据库集群部署完成"
    else
        log_error "数据库集群启动失败"
        exit 1
    fi
    
    # 初始化数据库
    log_info "初始化数据库..."
    ./scripts/init-database.sh
    
    log_success "数据库集群部署完成"
}

# 部署消息队列集群
deploy_message_queue_cluster() {
    log_info "开始部署消息队列集群..."
    
    # 检查Kafka是否在运行
    if docker ps | grep -q kafka; then
        log_warning "Kafka已在运行，跳过部署"
        return
    fi
    
    # 启动ZooKeeper集群
    log_info "启动ZooKeeper集群..."
    docker-compose -f docker-compose.zookeeper.yml up -d
    
    # 等待ZooKeeper启动
    log_info "等待ZooKeeper启动..."
    sleep 15
    
    # 启动Kafka集群
    log_info "启动Kafka集群..."
    docker-compose -f docker-compose.kafka.yml up -d
    
    # 等待Kafka启动
    log_info "等待Kafka启动..."
    sleep 20
    
    # 检查Kafka状态
    if docker-compose -f docker-compose.kafka.yml ps | grep -q "Up"; then
        log_success "Kafka集群部署完成"
    else
        log_error "Kafka集群启动失败"
        exit 1
    fi
    
    # 创建Topic
    log_info "创建Kafka Topic..."
    ./scripts/create-kafka-topics.sh
    
    log_success "消息队列集群部署完成"
}

# 部署服务注册中心
deploy_service_registry() {
    log_info "开始部署服务注册中心..."
    
    # 检查Nacos是否在运行
    if docker ps | grep -q nacos; then
        log_warning "Nacos已在运行，跳过部署"
        return
    fi
    
    # 启动Nacos集群
    log_info "启动Nacos集群..."
    docker-compose -f docker-compose.nacos.yml up -d
    
    # 等待Nacos启动
    log_info "等待Nacos启动..."
    sleep 30
    
    # 检查Nacos状态
    if curl -s http://localhost:8848/nacos/ > /dev/null; then
        log_success "Nacos部署完成"
    else
        log_error "Nacos启动失败"
        exit 1
    fi
    
    # 导入配置
    log_info "导入Nacos配置..."
    ./scripts/import-nacos-config.sh
    
    log_success "服务注册中心部署完成"
}

# 部署微服务
deploy_microservices() {
    log_info "开始部署微服务..."
    
    # 构建微服务
    log_info "构建微服务..."
    ./scripts/build-services.sh
    
    # 启动微服务
    log_info "启动微服务..."
    docker-compose -f docker-compose.services.yml up -d
    
    # 等待服务启动
    log_info "等待微服务启动..."
    sleep 60
    
    # 检查服务状态
    log_info "检查微服务状态..."
    ./scripts/check-services-health.sh
    
    log_success "微服务部署完成"
}

# 部署监控系统
deploy_monitoring_system() {
    log_info "开始部署监控系统..."
    
    # 启动Prometheus
    log_info "启动Prometheus..."
    docker-compose -f docker-compose.prometheus.yml up -d
    
    # 启动Grafana
    log_info "启动Grafana..."
    docker-compose -f docker-compose.grafana.yml up -d
    
    # 启动Jaeger
    log_info "启动Jaeger..."
    docker-compose -f docker-compose.jaeger.yml up -d
    
    # 等待监控系统启动
    log_info "等待监控系统启动..."
    sleep 30
    
    # 检查监控系统状态
    log_info "检查监控系统状态..."
    if curl -s http://localhost:9090/-/healthy > /dev/null && \
       curl -s http://localhost:3000/api/health > /dev/null && \
       curl -s http://localhost:16686/api/services > /dev/null; then
        log_success "监控系统部署完成"
    else
        log_error "监控系统启动失败"
        exit 1
    fi
    
    # 导入Grafana仪表板
    log_info "导入Grafana仪表板..."
    ./scripts/import-grafana-dashboards.sh
    
    log_success "监控系统部署完成"
}

# 部署测试环境
deploy_test_environment() {
    log_info "开始部署测试环境..."
    
    # 启动测试工具
    log_info "启动测试工具..."
    docker-compose -f docker-compose.test.yml up -d
    
    # 等待测试工具启动
    log_info "等待测试工具启动..."
    sleep 20
    
    # 检查测试工具状态
    log_info "检查测试工具状态..."
    if docker-compose -f docker-compose.test.yml ps | grep -q "Up"; then
        log_success "测试环境部署完成"
    else
        log_error "测试工具启动失败"
        exit 1
    fi
    
    # 准备测试数据
    log_info "准备测试数据..."
    ./scripts/prepare-test-data.sh
    
    log_success "测试环境部署完成"
}

# 验证部署结果
validate_deployment() {
    log_info "开始验证部署结果..."
    
    # 验证服务健康状态
    log_info "验证服务健康状态..."
    ./scripts/validate-services.sh
    
    # 验证数据库连接
    log_info "验证数据库连接..."
    ./scripts/validate-database.sh
    
    # 验证消息队列
    log_info "验证消息队列..."
    ./scripts/validate-kafka.sh
    
    # 验证服务注册中心
    log_info "验证服务注册中心..."
    ./scripts/validate-nacos.sh
    
    # 验证监控系统
    log_info "验证监控系统..."
    ./scripts/validate-monitoring.sh
    
    # 运行基础测试
    log_info "运行基础测试..."
    ./scripts/run-basic-tests.sh
    
    log_success "部署验证完成"
}

# 生成部署报告
generate_deployment_report() {
    log_info "生成部署报告..."
    
    local report_file="./reports/deployment_report_$(date +%Y%m%d_%H%M%S).md"
    
    mkdir -p ./reports
    
    cat > "$report_file" << EOF
# Sprint 27+1 测试环境部署报告

## 部署信息
- 部署时间: $(date)
- 部署环境: 测试环境
- 部署模式: $(if [ "$DEPLOY_ALL" = true ]; then echo "完整部署"; else echo "部分部署"; fi)
- 清理操作: $(if [ "$CLEAN_BEFORE" = true ]; then echo "是"; else echo "否"; fi)

## 部署组件
- 基础设施: $(if [ "$DEPLOY_INFRA" = true ]; then echo "✅ 已部署"; else echo "❌ 未部署"; fi)
- 数据库集群: $(if [ "$DEPLOY_DATABASE" = true ]; then echo "✅ 已部署"; else echo "❌ 未部署"; fi)
- 消息队列: $(if [ "$DEPLOY_MQ" = true ]; then echo "✅ 已部署"; else echo "❌ 未部署"; fi)
- 服务注册中心: $(if [ "$DEPLOY_INFRA" = true ]; then echo "✅ 已部署"; else echo "❌ 未部署"; fi)
- 微服务: $(if [ "$DEPLOY_SERVICES" = true ]; then echo "✅ 已部署"; else echo "❌ 未部署"; fi)
- 监控系统: $(if [ "$DEPLOY_INFRA" = true ]; then echo "✅ 已部署"; else echo "❌ 未部署"; fi)
- 测试环境: $(if [ "$DEPLOY_TEST" = true ]; then echo "✅ 已部署"; else echo "❌ 未部署"; fi)

## 服务状态
\`\`\`
$(docker-compose ps 2>/dev/null || echo "无法获取服务状态")
\`\`\`

## 资源使用情况
\`\`\`
$(docker stats --no-stream 2>/dev/null || echo "无法获取资源使用情况")
\`\`\`

## 端点信息
- API网关: http://localhost:8080
- 服务注册中心: http://localhost:8848/nacos
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)
- Jaeger: http://localhost:16686
- 用户服务: http://localhost:8081, http://localhost:8082
- 订单服务: http://localhost:8083, http://localhost:8084
- 库存服务: http://localhost:8085
- 监控服务: http://localhost:8086

## 测试信息
- 测试配置文件: ./test_env/config/
- 测试脚本: ./test_env/scripts/
- 测试数据: ./test_env/data/
- 测试报告: ./test_env/reports/

## 下一步操作
1. 运行集成测试: \`./scripts/run-integration-tests.sh\`
2. 运行性能测试: \`./scripts/run-performance-tests.sh\`
3. 查看监控仪表板: http://localhost:3000
4. 验证服务健康: \`curl http://localhost:8080/actuator/health\`

## 故障排除
1. 查看日志: \`docker-compose logs [service-name]\`
2. 重启服务: \`docker-compose restart [service-name]\`
3. 清理环境: \`$0 --clean --all\`
4. 重新部署: \`$0 --clean --all --validate\`

## 部署总结
$(if [ "$VALIDATE_AFTER" = true ] && ./scripts/validate-deployment.sh > /dev/null 2>&1; then echo "✅ 所有组件验证通过，部署成功"; else echo "⚠️  部署完成，建议运行验证脚本确认状态"; fi)

---
*报告生成时间: $(date)*
*部署脚本版本: 1.0.0*
EOF
    
    log_success "部署报告已生成: $report_file"
}

# 主函数
main() {
    log_info "========================================"
    log_info "Sprint 27+1 测试环境服务集群部署"
    log_info "========================================"
    
    # 解析参数
    parse_args "$@"
    
    # 检查依赖
    check_dependencies
    
    # 切换到脚本目录
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    cd "$SCRIPT_DIR"
    
    # 清理环境（如果需要）
    if [ "$CLEAN_BEFORE" = true ]; then
        clean_environment
    fi
    
    # 部署基础设施
    if [ "$DEPLOY_INFRA" = true ]; then
        deploy_infrastructure
        deploy_service_registry
        deploy_monitoring_system
    fi
    
    # 部署数据库集群
    if [ "$DEPLOY_DATABASE" = true ]; then
        deploy_database_cluster
    fi
    
    # 部署消息队列
    if [ "$DEPLOY_MQ" = true ]; then
        deploy_message_queue_cluster
    fi
    
    # 部署微服务
    if [ "$DEPLOY_SERVICES" = true ]; then
        deploy_microservices
    fi
    
    # 部署测试环境
    if [ "$DEPLOY_TEST" = true ]; then
        deploy_test_environment
    fi
    
    # 验证部署结果
    if [ "$VALIDATE_AFTER" = true ]; then
        validate_deployment
    fi
    
    # 生成部署报告
    generate_deployment_report
    
    log_info "========================================"
    log_success "部署完成！"
    log_info "========================================"
    
    # 显示访问信息
    echo ""
    echo "📋 访问信息："
    echo "  API网关:        http://localhost:8080"
    echo "  Nacos控制台:    http://localhost:8848/nacos (nacos/nacos)"
    echo "  Grafana:        http://localhost:3000 (admin/admin)"
    echo "  Prometheus:     http://localhost:9090"
    echo "  Jaeger:         http://localhost:16686"
    echo ""
    echo "🔧 管理命令："
    echo "  查看服务状态:   docker-compose ps"
    echo "  查看服务日志:   docker-compose logs [service]"
    echo "  重启服务:       docker-compose restart [service]"
    echo "  停止所有服务:   docker-compose down"
    echo ""
    echo "🧪 测试命令："
    echo "  运行集成测试:   ./scripts/run-integration-tests.sh"
    echo "  运行性能测试:   ./scripts/run-performance-tests.sh"
    echo "  验证部署:       ./scripts/validate-deployment.sh"
    echo ""
}

# 执行主函数
main "$@"