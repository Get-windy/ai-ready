#!/bin/bash
# AI-Ready 优化版镜像构建脚本
# 版本: v3.0
# 特性: 多平台构建、缓存优化、漏洞扫描

set -euo pipefail

# ============ 配置参数 ============
REGISTRY="${REGISTRY:-ghcr.io}"
IMAGE_NAME="${IMAGE_NAME:-ai-ready/ai-ready-api}"
VERSION="${VERSION:-$(date +%Y%m%d-%H%M%S)}"
GIT_SHA="${GIT_SHA:-$(git rev-parse --short HEAD 2>/dev/null || echo 'unknown')}"
BUILD_DATE="${BUILD_DATE:-$(date -u +'%Y-%m-%dT%H:%M:%SZ')}"

# 构建参数
PLATFORMS="${PLATFORMS:-linux/amd64,linux/arm64}"
CACHE_DIR="${CACHE_DIR:-/tmp/docker-build-cache}"
PUSH="${PUSH:-false}"
SCAN="${SCAN:-false}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# ============ 函数定义 ============
log() {
    echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[$(date '+%Y-%m-%d %H:%M:%S')] WARNING:${NC} $1"
}

error() {
    echo -e "${RED}[$(date '+%Y-%m-%d %H:%M:%S')] ERROR:${NC} $1"
}

# 检查依赖
check_dependencies() {
    log "检查构建依赖..."
    
    local deps=("docker" "git")
    for dep in "${deps[@]}"; do
        if ! command -v "$dep" &> /dev/null; then
            error "$dep 未安装"
            exit 1
        fi
    done
    
    # 检查docker buildx
    if ! docker buildx version &> /dev/null; then
        warn "Docker Buildx 未安装，将使用标准构建"
        USE_BUILDX=false
    else
        USE_BUILDX=true
        log "Docker Buildx 已启用"
    fi
    
    # 检查Trivy
    if command -v trivy &> /dev/null; then
        HAS_TRIVY=true
    else
        HAS_TRIVY=false
        warn "Trivy 未安装，跳过漏洞扫描"
    fi
}

# 创建构建器
create_builder() {
    if [[ "$USE_BUILDX" == "true" ]]; then
        log "创建多平台构建器..."
        docker buildx create --use --name ai-ready-builder 2>/dev/null || \
            docker buildx use ai-ready-builder
        docker buildx inspect --bootstrap
    fi
}

# 构建镜像
build_image() {
    log "开始构建镜像..."
    log "镜像: ${REGISTRY}/${IMAGE_NAME}"
    log "版本: ${VERSION}"
    log "Git SHA: ${GIT_SHA}"
    
    local tags=()
    tags+=("-t" "${REGISTRY}/${IMAGE_NAME}:${VERSION}")
    tags+=("-t" "${REGISTRY}/${IMAGE_NAME}:${GIT_SHA}")
    tags+=("-t" "${REGISTRY}/${IMAGE_NAME}:latest")
    
    local build_args=()
    build_args+=("--build-arg" "BUILD_DATE=${BUILD_DATE}")
    build_args+=("--build-arg" "VERSION=${VERSION}")
    build_args+=("--build-arg" "GIT_SHA=${GIT_SHA}")
    
    if [[ "$USE_BUILDX" == "true" ]]; then
        # 使用Buildx多平台构建
        local cache_args=()
        cache_args+=("--cache-from" "type=local,src=${CACHE_DIR}")
        cache_args+=("--cache-to" "type=local,dest=${CACHE_DIR},mode=max")
        
        if [[ "$PUSH" == "true" ]]; then
            log "启用多平台构建并推送..."
            docker buildx build \
                --platform "${PLATFORMS}" \
                "${tags[@]}" \
                "${build_args[@]}" \
                "${cache_args[@]}" \
                --push \
                -f Dockerfile.optimized \
                ..
        else
            log "多平台构建（本地加载）..."
            docker buildx build \
                --platform "${PLATFORMS}" \
                "${tags[@]}" \
                "${build_args[@]}" \
                "${cache_args[@]}" \
                --load \
                -f Dockerfile.optimized \
                ..
        fi
    else
        # 标准构建
        log "使用标准Docker构建..."
        docker build \
            "${tags[@]}" \
            "${build_args[@]}" \
            -f Dockerfile.optimized \
            ..
    fi
    
    log "镜像构建完成"
}

# 推送镜像
push_image() {
    if [[ "$PUSH" != "true" ]]; then
        log "跳过推送（使用 --push 启用）"
        return
    fi
    
    log "推送镜像到仓库..."
    
    if [[ "$USE_BUILDX" == "true" ]]; then
        log "镜像已在构建时推送"
    else
        docker push "${REGISTRY}/${IMAGE_NAME}:${VERSION}"
        docker push "${REGISTRY}/${IMAGE_NAME}:${GIT_SHA}"
        docker push "${REGISTRY}/${IMAGE_NAME}:latest"
    fi
    
    log "镜像推送完成"
}

# 扫描漏洞
scan_image() {
    if [[ "$SCAN" != "true" ]] || [[ "$HAS_TRIVY" == "false" ]]; then
        log "跳过漏洞扫描"
        return
    fi
    
    log "扫描镜像漏洞..."
    
    trivy image \
        --severity HIGH,CRITICAL \
        --exit-code 0 \
        --format table \
        "${REGISTRY}/${IMAGE_NAME}:${VERSION}"
    
    # 生成JSON报告
    trivy image \
        --severity HIGH,CRITICAL \
        --exit-code 0 \
        --format json \
        -o "trivy-report-${VERSION}.json" \
        "${REGISTRY}/${IMAGE_NAME}:${VERSION}"
    
    log "漏洞扫描完成，报告: trivy-report-${VERSION}.json"
}

# 显示镜像信息
show_info() {
    log "镜像信息:"
    echo "=========================================="
    docker images "${REGISTRY}/${IMAGE_NAME}" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
    echo "=========================================="
    
    # 显示镜像历史
    log "镜像构建历史:"
    docker history "${REGISTRY}/${IMAGE_NAME}:${VERSION}" --format "table {{.CreatedBy}}\t{{.Size}}" | head -20
}

# 测试镜像
test_image() {
    log "测试镜像启动..."
    
    local container_name="ai-ready-test-${VERSION}"
    
    # 启动容器
    docker run -d \
        --name "${container_name}" \
        -p 8080:8080 \
        -e SPRING_PROFILES_ACTIVE=test \
        "${REGISTRY}/${IMAGE_NAME}:${VERSION}"
    
    # 等待启动
    log "等待服务启动..."
    sleep 30
    
    # 健康检查
    if curl -fs http://localhost:8080/actuator/health | grep -q '"status":"UP"'; then
        log "✓ 健康检查通过"
    else
        warn "✗ 健康检查失败"
        docker logs "${container_name}" --tail 50
    fi
    
    # 清理
    docker stop "${container_name}" &> /dev/null || true
    docker rm "${container_name}" &> /dev/null || true
}

# 清理缓存
cleanup() {
    log "清理构建缓存..."
    docker buildx prune -f 2>/dev/null || true
    docker system prune -f --volumes 2>/dev/null || true
}

# 显示帮助
show_help() {
    cat << EOF
AI-Ready Docker镜像构建脚本

用法: $0 [选项] [命令]

命令:
    build       构建镜像 (默认)
    push        构建并推送镜像
    scan        构建并扫描漏洞
    test        构建并测试镜像
    all         执行完整流程
    cleanup     清理缓存

选项:
    --push      推送镜像到仓库
    --scan      扫描镜像漏洞
    --platform  指定目标平台 (默认: linux/amd64,linux/arm64)
    --version   指定版本号
    --help      显示帮助

环境变量:
    REGISTRY        镜像仓库 (默认: ghcr.io)
    IMAGE_NAME      镜像名称 (默认: ai-ready/ai-ready-api)
    VERSION         版本号
    CACHE_DIR       缓存目录 (默认: /tmp/docker-build-cache)

示例:
    # 本地构建
    $0 build

    # 构建并推送
    $0 build --push

    # 多平台构建并推送
    $0 build --push --platform linux/amd64,linux/arm64

    # 完整流程
    $0 all
EOF
}

# ============ 主流程 ============
main() {
    local action="build"
    
    # 解析参数
    while [[ $# -gt 0 ]]; do
        case "$1" in
            build|push|scan|test|all|cleanup)
                action="$1"
                shift
                ;;
            --push)
                PUSH=true
                shift
                ;;
            --scan)
                SCAN=true
                shift
                ;;
            --platform)
                PLATFORMS="$2"
                shift 2
                ;;
            --version)
                VERSION="$2"
                shift 2
                ;;
            --help|-h)
                show_help
                exit 0
                ;;
            *)
                error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # 设置特殊action的参数
    case "$action" in
        push)
            PUSH=true
            ;;
        scan)
            SCAN=true
            ;;
        all)
            PUSH=true
            SCAN=true
            ;;
        cleanup)
            cleanup
            exit 0
            ;;
    esac
    
    log "=========================================="
    log "AI-Ready Docker镜像构建"
    log "=========================================="
    log "Action: $action"
    log "Push: $PUSH"
    log "Scan: $SCAN"
    log "Platforms: $PLATFORMS"
    
    # 执行流程
    check_dependencies
    create_builder
    build_image
    push_image
    scan_image
    show_info
    
    if [[ "$action" == "test" ]] || [[ "$action" == "all" ]]; then
        test_image
    fi
    
    log "=========================================="
    log "构建流程完成!"
    log "=========================================="
    log "镜像: ${REGISTRY}/${IMAGE_NAME}:${VERSION}"
    log "标签: ${VERSION}, ${GIT_SHA}, latest"
}

# 执行主流程
main "$@"
