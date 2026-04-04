#!/bin/bash
# AI-Ready 容器镜像构建脚本
# 版本: v2.0

set -e

# ============ 配置参数 ============
REGISTRY="${REGISTRY:-ghcr.io}"
IMAGE_NAME="${IMAGE_NAME:-ai-ready/ai-ready-api}"
VERSION="${VERSION:-$(date +%Y%m%d-%H%M%S)}"
GIT_SHA="${GIT_SHA:-$(git rev-parse --short HEAD 2>/dev/null || echo 'unknown')}"

# 标签策略
TAGS=(
    "${VERSION}"
    "${GIT_SHA}"
    "latest"
)

# ============ 函数定义 ============
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# 构建镜像
build_image() {
    log "开始构建镜像: ${IMAGE_NAME}"
    
    # 构建参数
    local build_args=(
        "--build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
        "--build-arg VERSION=${VERSION}"
        "--build-arg GIT_SHA=${GIT_SHA}"
    )
    
    # 多平台构建
    if command -v docker buildx &> /dev/null; then
        docker buildx build \
            --platform linux/amd64,linux/arm64 \
            "${build_args[@]}" \
            -t "${REGISTRY}/${IMAGE_NAME}:${VERSION}" \
            -t "${REGISTRY}/${IMAGE_NAME}:${GIT_SHA}" \
            -t "${REGISTRY}/${IMAGE_NAME}:latest" \
            --push \
            .
    else
        docker build \
            "${build_args[@]}" \
            -t "${REGISTRY}/${IMAGE_NAME}:${VERSION}" \
            -t "${REGISTRY}/${IMAGE_NAME}:${GIT_SHA}" \
            -t "${REGISTRY}/${IMAGE_NAME}:latest" \
            .
    fi
    
    log "镜像构建完成"
}

# 推送镜像
push_image() {
    log "推送镜像到仓库: ${REGISTRY}"
    
    for tag in "${TAGS[@]}"; do
        docker push "${REGISTRY}/${IMAGE_NAME}:${tag}"
    done
    
    log "镜像推送完成"
}

# 扫描漏洞
scan_image() {
    log "扫描镜像漏洞..."
    
    if command -v trivy &> /dev/null; then
        trivy image --severity HIGH,CRITICAL "${REGISTRY}/${IMAGE_NAME}:${VERSION}"
    else
        log "trivy 未安装，跳过漏洞扫描"
    fi
}

# 显示镜像信息
show_info() {
    log "镜像信息:"
    docker images "${REGISTRY}/${IMAGE_NAME}" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"
}

# ============ 主流程 ============
main() {
    local action=${1:-build}
    
    case "$action" in
        build)
            build_image
            show_info
            ;;
        push)
            build_image
            push_image
            ;;
        scan)
            build_image
            scan_image
            ;;
        all)
            build_image
            push_image
            scan_image
            show_info
            ;;
        *)
            echo "用法: $0 {build|push|scan|all}"
            exit 1
            ;;
    esac
}

main "$@"