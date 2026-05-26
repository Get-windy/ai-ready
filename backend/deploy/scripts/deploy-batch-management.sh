#!/bin/bash

# ERP批次管理服务部署脚本
# 版本: 1.0.0
# 功能: 自动化部署批次管理Spring Boot应用

set -e

# 配置变量
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"
BATCH_MODULE_DIR="$PROJECT_ROOT/erp/batch-management-service"
DEPLOY_CONFIG="$SCRIPT_DIR/deployment-config.yaml"
LOG_FILE="/var/log/batch-deploy-$(date +%Y%m%d-%H%M%S).log"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
    exit 1
}

# 检查依赖
check_dependencies() {
    log_info "检查系统依赖..."
    
    local missing_deps=()
    
    # 检查Java
    if ! command -v java &> /dev/null; then
        missing_deps+=("Java 17+")
    else
        JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d '"' -f2)
        log_info "Java版本: $JAVA_VERSION"
    fi
    
    # 检查Maven
    if ! command -v mvn &> /dev/null; then
        missing_deps+=("Maven 3.6+")
    else
        MAVEN_VERSION=$(mvn -v 2>&1 | grep "Apache Maven" | cut -d ' ' -f3)
        log_info "Maven版本: $MAVEN_VERSION"
    fi
    
    # 检查Docker
    if ! command -v docker &> /dev/null; then
        missing_deps+=("Docker")
    else
        DOCKER_VERSION=$(docker --version | cut -d ' ' -f3 | tr -d ',')
        log_info "Docker版本: $DOCKER_VERSION"
    fi
    
    if [ ${#missing_deps[@]} -gt 0 ]; then
        log_error "缺少依赖: ${missing_deps[*]}"
    fi
    
    log_success "所有依赖检查通过"
}

# 验证项目结构
validate_project_structure() {
    log_info "验证项目结构..."
    
    if [ ! -d "$BATCH_MODULE_DIR" ]; then
        log_error "批次管理模块目录不存在: $BATCH_MODULE_DIR"
    fi
    
    if [ ! -f "$BATCH_MODULE_DIR/pom.xml" ]; then
        log_error "批次管理模块pom.xml不存在"
    fi
    
    if [ ! -d "$BATCH_MODULE_DIR/src/main/java" ]; then
        log_error "批次管理模块源码目录不存在"
    fi
    
    log_success "项目结构验证通过"
}

# 编译项目
compile_project() {
    log_info "开始编译批次管理服务..."
    
    cd "$BATCH_MODULE_DIR"
    
    # 清理并编译
    mvn clean compile -q 2>&1 | tee -a "$LOG_FILE"
    
    if [ ${PIPESTATUS[0]} -ne 0 ]; then
        log_error "Maven编译失败，请检查日志: $LOG_FILE"
    fi
    
    log_success "编译完成"
}

# 运行单元测试
run_tests() {
    log_info "运行单元测试..."
    
    cd "$BATCH_MODULE_DIR"
    
    mvn test -q 2>&1 | tee -a "$LOG_FILE"
    
    if [ ${PIPESTATUS[0]} -ne 0 ]; then
        log_warning "单元测试失败，但继续部署流程"
    else
        log_success "所有单元测试通过"
    fi
}

# 打包应用
package_application() {
    log_info "打包应用程序..."
    
    cd "$BATCH_MODULE_DIR"
    
    # 跳过测试打包
    mvn package -DskipTests -q 2>&1 | tee -a "$LOG_FILE"
    
    if [ ${PIPESTATUS[0]} -ne 0 ]; then
        log_error "打包失败"
    fi
    
    # 检查生成的jar文件
    JAR_FILE=$(find target -name "*.jar" ! -name "*sources.jar" ! -name "*javadoc.jar" | head -n 1)
    
    if [ -z "$JAR_FILE" ]; then
        log_error "未找到生成的jar文件"
    fi
    
    log_info "生成的文件: $JAR_FILE"
    log_success "打包完成"
}

# 构建Docker镜像
build_docker_image() {
    log_info "构建Docker镜像..."
    
    cd "$BATCH_MODULE_DIR"
    
    # 检查Dockerfile
    if [ ! -f "Dockerfile" ]; then
        log_info "创建默认Dockerfile..."
        cat > Dockerfile << 'EOF'
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF
    fi
    
    # 构建镜像
    docker build -t batch-management-service:latest . 2>&1 | tee -a "$LOG_FILE"
    
    if [ ${PIPESTATUS[0]} -ne 0 ]; then
        log_error "Docker构建失败"
    fi
    
    log_success "Docker镜像构建完成: batch-management-service:latest"
}

# 部署应用
deploy_application() {
    log_info "部署应用程序..."
    
    # 停止并删除旧容器
    if docker ps -a | grep -q "batch-management"; then
        log_info "停止旧容器..."
        docker stop batch-management 2>/dev/null || true
        docker rm batch-management 2>/dev/null || true
    fi
    
    # 运行新容器
    log_info "启动新容器..."
    docker run -d \
        --name batch-management \
        --restart unless-stopped \
        -p 8080:8080 \
        -e SPRING_PROFILES_ACTIVE=prod \
        -v /var/log/batch-management:/app/logs \
        batch-management-service:latest 2>&1 | tee -a "$LOG_FILE"
    
    if [ ${PIPESTATUS[0]} -ne 0 ]; then
        log_error "容器启动失败"
    fi
    
    log_success "容器启动成功"
}

# 健康检查
health_check() {
    log_info "执行健康检查..."
    
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s -f "http://localhost:8080/actuator/health" > /dev/null 2>&1; then
            log_success "健康检查通过 (尝试 $attempt/$max_attempts)"
            return 0
        fi
        
        log_info "等待应用启动... (尝试 $attempt/$max_attempts)"
        sleep 2
        ((attempt++))
    done
    
    log_error "健康检查失败，应用未在60秒内启动"
}

# 显示部署信息
show_deployment_info() {
    echo ""
    echo "========================================="
    echo "       批次管理服务部署完成"
    echo "========================================="
    echo ""
    echo "服务信息:"
    echo "  - 服务名称: 批次管理服务"
    echo "  - 容器名称: batch-management"
    echo "  - 访问地址: http://localhost:8080"
    echo "  - 健康检查: http://localhost:8080/actuator/health"
    echo "  - 日志文件: $LOG_FILE"
    echo ""
    echo "管理命令:"
    echo "  - 查看日志: docker logs batch-management"
    echo "  - 停止服务: docker stop batch-management"
    echo "  - 重启服务: docker restart batch-management"
    echo "  - 进入容器: docker exec -it batch-management bash"
    echo ""
    echo "监控信息:"
    echo "  - Prometheus指标: http://localhost:8080/actuator/prometheus"
    echo "  - 应用信息: http://localhost:8080/actuator/info"
    echo ""
    echo "========================================="
}

# 主函数
main() {
    echo ""
    echo "========================================="
    echo "  ERP批次管理服务自动化部署脚本"
    echo "========================================="
    echo ""
    
    log_info "开始部署流程..."
    log_info "日志文件: $LOG_FILE"
    
    # 执行部署步骤
    check_dependencies
    validate_project_structure
    compile_project
    run_tests
    package_application
    build_docker_image
    deploy_application
    health_check
    
    show_deployment_info
    
    log_success "部署流程完成"
}

# 执行主函数
main "$@"