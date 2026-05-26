#!/bin/bash

# Docker镜像构建脚本 - 批次管理服务
# 版本: 1.0.0

set -e

# 配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"
BATCH_MODULE_DIR="$PROJECT_ROOT/erp/batch-management-service"
DOCKER_REGISTRY="registry.aiedge.cn"
IMAGE_NAME="batch-management-service"
VERSION="${1:-latest}"
TAG="$DOCKER_REGISTRY/$IMAGE_NAME:$VERSION"

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

log() {
    echo -e "${BLUE}[DOCKER BUILD]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
    exit 1
}

# 检查依赖
check_dependencies() {
    log "检查Docker依赖..."
    
    if ! command -v docker &> /dev/null; then
        error "Docker未安装"
    fi
    
    DOCKER_VERSION=$(docker --version | cut -d ' ' -f3 | tr -d ',')
    log "Docker版本: $DOCKER_VERSION"
    
    # 检查Docker守护进程是否运行
    if ! docker info > /dev/null 2>&1; then
        error "Docker守护进程未运行"
    fi
    
    success "Docker依赖检查通过"
}

# 验证项目
validate_project() {
    log "验证项目结构..."
    
    if [ ! -d "$BATCH_MODULE_DIR" ]; then
        error "批次管理模块目录不存在: $BATCH_MODULE_DIR"
    fi
    
    if [ ! -f "$BATCH_MODULE_DIR/pom.xml" ]; then
        error "pom.xml不存在"
    fi
    
    # 检查是否有可用的jar文件
    if [ ! -d "$BATCH_MODULE_DIR/target" ]; then
        warning "target目录不存在，需要先编译项目"
        return 1
    fi
    
    JAR_COUNT=$(find "$BATCH_MODULE_DIR/target" -name "*.jar" ! -name "*sources.jar" ! -name "*javadoc.jar" | wc -l)
    
    if [ "$JAR_COUNT" -eq 0 ]; then
        warning "未找到可执行的jar文件"
        return 1
    fi
    
    success "项目验证通过"
}

# 创建Dockerfile
create_dockerfile() {
    log "创建/更新Dockerfile..."
    
    cd "$BATCH_MODULE_DIR"
    
    cat > Dockerfile << 'EOF'
# ERP批次管理服务Docker镜像
# 构建阶段：使用Maven构建
FROM maven:3.8.7-eclipse-temurin-17 AS builder

WORKDIR /build
COPY pom.xml .
# 下载依赖（利用Docker缓存）
RUN mvn dependency:go-offline -B

# 复制源码并构建
COPY src ./src
RUN mvn clean package -DskipTests

# 运行阶段：使用轻量级JRE
FROM eclipse-temurin:17-jre-alpine

# 安装必要的工具
RUN apk add --no-cache \
    tzdata \
    curl \
    bash

# 设置时区
ENV TZ=Asia/Shanghai

# 创建应用用户（非root运行）
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 创建工作目录
WORKDIR /app

# 从构建阶段复制jar文件
COPY --from=builder /build/target/*.jar app.jar

# 复制配置文件（如果需要）
COPY --from=builder /build/src/main/resources/application*.yml ./config/

# 设置权限
RUN chown -R appuser:appgroup /app
USER appuser

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF
    
    success "Dockerfile创建完成"
}

# 构建Docker镜像
build_image() {
    log "构建Docker镜像: $IMAGE_NAME:$VERSION"
    
    cd "$BATCH_MODULE_DIR"
    
    # 构建参数
    BUILD_ARGS=""
    
    # 添加构建参数（如果提供）
    if [ -n "$BUILD_NUMBER" ]; then
        BUILD_ARGS="$BUILD_ARGS --build-arg BUILD_NUMBER=$BUILD_NUMBER"
    fi
    
    if [ -n "$GIT_COMMIT" ]; then
        BUILD_ARGS="$BUILD_ARGS --build-arg GIT_COMMIT=$GIT_COMMIT"
    fi
    
    # 执行构建
    docker build \
        --tag "$IMAGE_NAME:$VERSION" \
        --tag "$IMAGE_NAME:latest" \
        $BUILD_ARGS \
        . 2>&1 | while IFS= read -r line; do
            echo "  $line"
        done
    
    if [ ${PIPESTATUS[0]} -ne 0 ]; then
        error "Docker构建失败"
    fi
    
    success "镜像构建完成: $IMAGE_NAME:$VERSION"
}

# 安全扫描
security_scan() {
    log "执行安全扫描..."
    
    # 检查是否有trivy
    if command -v trivy &> /dev/null; then
        log "使用trivy扫描镜像..."
        trivy image --exit-code 0 --severity HIGH,CRITICAL "$IMAGE_NAME:$VERSION"
    else
        warning "trivy未安装，跳过安全扫描"
    fi
    
    success "安全扫描完成"
}

# 标记镜像
tag_image() {
    log "标记镜像到注册表: $TAG"
    
    docker tag "$IMAGE_NAME:$VERSION" "$TAG"
    
    if [ $? -ne 0 ]; then
        error "镜像标记失败"
    fi
    
    success "镜像标记完成: $TAG"
}

# 推送镜像
push_image() {
    local push=${PUSH_IMAGE:-false}
    
    if [ "$push" != "true" ]; then
        log "跳过镜像推送 (设置 PUSH_IMAGE=true 启用推送)"
        return 0
    fi
    
    log "推送镜像到注册表..."
    
    # 登录注册表（如果需要）
    if [ -n "$DOCKER_USERNAME" ] && [ -n "$DOCKER_PASSWORD" ]; then
        log "登录Docker注册表..."
        echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin "$DOCKER_REGISTRY"
    fi
    
    docker push "$TAG"
    
    if [ $? -ne 0 ]; then
        error "镜像推送失败"
    fi
    
    success "镜像推送完成"
}

# 清理中间镜像
cleanup() {
    log "清理中间构建镜像..."
    
    # 删除<none>标签的中间镜像
    docker image prune -f 2>/dev/null || true
    
    success "清理完成"
}

# 显示镜像信息
show_image_info() {
    echo ""
    echo "========================================="
    echo "        Docker镜像构建完成"
    echo "========================================="
    echo ""
    echo "镜像信息:"
    echo "  - 镜像名称: $IMAGE_NAME"
    echo "  - 版本标签: $VERSION"
    echo "  - 完整标签: $TAG"
    echo "  - 镜像大小: $(docker images --format "{{.Size}}" "$IMAGE_NAME:$VERSION")"
    echo ""
    echo "运行命令:"
    echo "  docker run -d -p 8080:8080 --name batch-management $IMAGE_NAME:$VERSION"
    echo ""
    echo "查看镜像:"
    echo "  docker images $IMAGE_NAME"
    echo ""
    echo "检查镜像:"
    echo "  docker inspect $IMAGE_NAME:$VERSION"
    echo ""
    echo "========================================="
}

# 主函数
main() {
    echo ""
    echo "========================================="
    echo "  批次管理服务Docker镜像构建"
    echo "========================================="
    echo ""
    
    check_dependencies
    
    # 尝试验证项目，如果失败则只是警告
    validate_project || warning "项目验证发现问题，继续构建..."
    
    create_dockerfile
    build_image
    security_scan
    tag_image
    push_image
    cleanup
    
    show_image_info
    
    success "Docker镜像构建流程完成"
}

# 执行主函数
main "$@"