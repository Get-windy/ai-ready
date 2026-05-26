#!/bin/bash
# 🚀 ERP批次管理模块Docker镜像构建和推送脚本
# 版本: 1.0.0
# 功能: 构建Docker镜像、打标签、推送到镜像仓库

set -euo pipefail

# 配置变量
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"
BATCHSN_MODULE_DIR="$PROJECT_ROOT/backend/erp/erp-batch-sn"
TARGET_JAR="target/erp-batch-sn-1.0.0-SNAPSHOT.jar"
DOCKERFILE="$BATCHSN_MODULE_DIR/Dockerfile"

# Docker配置
DOCKER_REGISTRY="registry.ai-ready.cn"
DOCKER_NAMESPACE="erp"
DOCKER_REPO="erp-batch-sn"
DOCKER_USERNAME="${DOCKER_USERNAME:-}"
DOCKER_PASSWORD="${DOCKER_PASSWORD:-}"

# 镜像标签配置
GIT_COMMIT=$(git rev-parse --short HEAD)
GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
BUILD_TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BUILD_VERSION="${BUILD_VERSION:-1.0.0}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# 显示脚本配置
show_config() {
    echo ""
    echo "========================================="
    echo "🚀 Docker镜像构建配置"
    echo "========================================="
    echo "项目根目录: $PROJECT_ROOT"
    echo "模块目录: $BATCHSN_MODULE_DIR"
    echo "Dockerfile: $DOCKERFILE"
    echo "Docker仓库: $DOCKER_REGISTRY/$DOCKER_NAMESPACE/$DOCKER_REPO"
    echo "Git提交: $GIT_COMMIT"
    echo "Git分支: $GIT_BRANCH"
    echo "构建版本: $BUILD_VERSION"
    echo "构建时间: $BUILD_TIMESTAMP"
    echo "========================================="
    echo ""
}

# 检查前置条件
check_prerequisites() {
    log_info "检查前置条件..."
    
    # 检查Git
    if ! command -v git &> /dev/null; then
        log_error "Git未安装"
        exit 1
    fi
    
    # 检查Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker未安装"
        exit 1
    fi
    
    # 检查Docker是否运行
    if ! docker info &> /dev/null; then
        log_error "Docker守护进程未运行"
        exit 1
    fi
    
    # 检查Dockerfile是否存在
    if [ ! -f "$DOCKERFILE" ]; then
        log_error "Dockerfile不存在: $DOCKERFILE"
        exit 1
    fi
    
    # 检查是否在Git仓库中
    if [ ! -d "$PROJECT_ROOT/.git" ]; then
        log_error "不在Git仓库中: $PROJECT_ROOT"
        exit 1
    fi
    
    log_success "前置条件检查通过"
}

# 构建Java应用
build_java_application() {
    log_info "构建Java应用..."
    
    cd "$BATCHSN_MODULE_DIR"
    
    # 检查是否已有构建产物
    if [ -f "$TARGET_JAR" ]; then
        log_warn "目标JAR已存在，将重新构建"
        rm -f "$TARGET_JAR"
    fi
    
    # 使用Maven构建
    if [ -f "pom.xml" ]; then
        log_info "使用Maven构建..."
        mvn clean package -DskipTests
    else
        log_error "未找到pom.xml文件"
        exit 1
    fi
    
    # 验证构建产物
    if [ ! -f "$TARGET_JAR" ]; then
        log_error "构建失败，未找到目标JAR: $TARGET_JAR"
        exit 1
    fi
    
    local jar_size=$(ls -lh "$TARGET_JAR" | awk '{print $5}')
    log_success "Java应用构建完成，JAR大小: $jar_size"
    
    cd - > /dev/null
}

# 构建Docker镜像
build_docker_image() {
    local tag=$1
    local context=$2
    
    log_info "构建Docker镜像，标签: $tag"
    
    # 构建镜像
    docker build \
        --build-arg BUILD_VERSION="$BUILD_VERSION" \
        --build-arg GIT_COMMIT="$GIT_COMMIT" \
        --build-arg BUILD_TIMESTAMP="$BUILD_TIMESTAMP" \
        -t "$tag" \
        -f "$DOCKERFILE" \
        "$context"
    
    if [ $? -ne 0 ]; then
        log_error "Docker镜像构建失败"
        exit 1
    fi
    
    # 验证镜像
    local image_id=$(docker images -q "$tag")
    if [ -z "$image_id" ]; then
        log_error "Docker镜像验证失败"
        exit 1
    fi
    
    log_success "Docker镜像构建完成，镜像ID: $image_id"
}

# 生成镜像标签列表
generate_image_tags() {
    local base_tag="$DOCKER_REGISTRY/$DOCKER_NAMESPACE/$DOCKER_REPO"
    
    # 主要标签
    local tags=(
        "${base_tag}:latest"
        "${base_tag}:${BUILD_VERSION}"
        "${base_tag}:${BUILD_VERSION}-${GIT_COMMIT}"
        "${base_tag}:${BUILD_VERSION}-${BUILD_TIMESTAMP}"
    )
    
    # 如果当前分支是master/main，添加稳定标签
    if [[ "$GIT_BRANCH" == "master" || "$GIT_BRANCH" == "main" ]]; then
        tags+=("${base_tag}:stable")
        
        # 提取主版本号
        local major_version=$(echo "$BUILD_VERSION" | cut -d'.' -f1)
        tags+=("${base_tag}:v${major_version}")
    fi
    
    echo "${tags[@]}"
}

# 标记Docker镜像
tag_docker_images() {
    local source_tag=$1
    shift
    local target_tags=("$@")
    
    log_info "标记Docker镜像..."
    
    for target_tag in "${target_tags[@]}"; do
        log_info "标记镜像: $source_tag -> $target_tag"
        docker tag "$source_tag" "$target_tag"
        
        if [ $? -ne 0 ]; then
            log_error "镜像标记失败: $target_tag"
            exit 1
        fi
    done
    
    log_success "镜像标记完成，共标记 ${#target_tags[@]} 个标签"
}

# 登录Docker仓库
login_docker_registry() {
    if [ -z "$DOCKER_USERNAME" ] || [ -z "$DOCKER_PASSWORD" ]; then
        log_warn "Docker凭证未设置，跳过登录（仅本地构建）"
        return 0
    fi
    
    log_info "登录Docker仓库: $DOCKER_REGISTRY"
    
    echo "$DOCKER_PASSWORD" | docker login "$DOCKER_REGISTRY" \
        --username "$DOCKER_USERNAME" \
        --password-stdin
    
    if [ $? -ne 0 ]; then
        log_error "Docker仓库登录失败"
        exit 1
    fi
    
    log_success "Docker仓库登录成功"
}

# 推送Docker镜像
push_docker_images() {
    local tags=("$@")
    
    if [ ${#tags[@]} -eq 0 ]; then
        log_error "没有可推送的镜像标签"
        return 1
    fi
    
    log_info "推送Docker镜像到仓库..."
    
    local push_count=0
    for tag in "${tags[@]}"; do
        log_info "推送镜像: $tag"
        
        # 最大重试3次
        local retry_count=0
        local max_retries=3
        local push_success=false
        
        while [ $retry_count -lt $max_retries ] && [ "$push_success" = false ]; do
            docker push "$tag"
            
            if [ $? -eq 0 ]; then
                push_success=true
                ((push_count++))
                log_success "镜像推送成功: $tag"
            else
                ((retry_count++))
                if [ $retry_count -lt $max_retries ]; then
                    log_warn "镜像推送失败，重试第 $retry_count 次: $tag"
                    sleep 5
                else
                    log_error "镜像推送失败，已达最大重试次数: $tag"
                fi
            fi
        done
    done
    
    log_success "镜像推送完成，成功推送 $push_count 个镜像"
}

# 清理本地镜像
cleanup_local_images() {
    local tags=("$@")
    local keep_latest=${1:-false}
    
    log_info "清理本地Docker镜像..."
    
    # 如果指定保留latest，则排除latest标签
    if [ "$keep_latest" = true ]; then
        for tag in "${tags[@]}"; do
            if [[ "$tag" != *":latest" ]]; then
                log_info "删除本地镜像: $tag"
                docker rmi -f "$tag" 2>/dev/null || true
            fi
        done
    else
        for tag in "${tags[@]}"; do
            log_info "删除本地镜像: $tag"
            docker rmi -f "$tag" 2>/dev/null || true
        done
    fi
    
    # 清理悬空镜像
    docker image prune -f 2>/dev/null || true
    
    log_success "本地镜像清理完成"
}

# 生成构建报告
generate_build_report() {
    local tags=("$@")
    local report_file="/tmp/docker-build-report-${BUILD_TIMESTAMP}.md"
    
    cat > "$report_file" << EOF
# Docker镜像构建报告

## 构建信息
- **构建时间**: $(date '+%Y-%m-%d %H:%M:%S')
- **构建版本**: $BUILD_VERSION
- **Git提交**: $GIT_COMMIT
- **Git分支**: $GIT_BRANCH
- **构建人员**: $(whoami)@$(hostname)

## 镜像详情
| 标签 | 状态 | 大小 | 推送时间 |
|------|------|------|----------|
EOF
    
    for tag in "${tags[@]}"; do
        local image_size=$(docker images --format "{{.Repository}}:{{.Tag}}\t{{.Size}}" | grep "^$tag" | cut -f2 || echo "N/A")
        echo "| $tag | ✅ 构建完成 | $image_size | $(date '+%H:%M:%S') |" >> "$report_file"
    done
    
    cat >> "$report_file" << EOF

## 构建日志摘要
\`\`\`
构建开始时间: $(date -d @$START_TIME '+%Y-%m-%d %H:%M:%S')
构建耗时: $(( $(date +%s) - START_TIME )) 秒
构建状态: 成功 ✅
\`\`\`

## 下一步操作
1. 验证镜像: \`docker pull $DOCKER_REGISTRY/$DOCKER_NAMESPACE/$DOCKER_REPO:latest\`
2. 部署测试: 使用最新镜像进行部署测试
3. 更新文档: 更新部署文档中的镜像版本

---
**报告生成时间**: $(date '+%Y-%m-%d %H:%M:%S')
EOF
    
    log_success "构建报告已生成: $report_file"
    cat "$report_file"
}

# 主构建流程
main_build_process() {
    local push_to_registry=${1:-false}
    local cleanup_after_push=${2:-false}
    
    log_info "开始Docker镜像构建流程..."
    START_TIME=$(date +%s)
    
    # 显示配置
    show_config
    
    # 检查前置条件
    check_prerequisites
    
    # 构建Java应用
    build_java_application
    
    # 生成镜像标签
    local all_tags=($(generate_image_tags))
    local primary_tag="${all_tags[0]}"
    
    # 构建主镜像
    build_docker_image "$primary_tag" "$BATCHSN_MODULE_DIR"
    
    # 标记其他标签
    tag_docker_images "$primary_tag" "${all_tags[@]}"
    
    # 如果需要推送
    if [ "$push_to_registry" = true ]; then
        # 登录仓库
        login_docker_registry
        
        # 推送镜像
        push_docker_images "${all_tags[@]}"
        
        # 生成推送报告
        generate_build_report "${all_tags[@]}"
        
        # 如果需要清理
        if [ "$cleanup_after_push" = true ]; then
            cleanup_local_images "${all_tags[@]}" true
        fi
    else
        log_warn "跳过镜像推送（仅本地构建）"
        generate_build_report "${all_tags[@]}"
    fi
    
    local end_time=$(date +%s)
    local duration=$((end_time - START_TIME))
    
    echo ""
    echo "========================================="
    echo "🏁 Docker镜像构建流程完成"
    echo "========================================="
    echo "总耗时: $duration 秒"
    echo "构建镜像数: ${#all_tags[@]}"
    echo "主镜像标签: $primary_tag"
    echo "推送状态: $( [ "$push_to_registry" = true ] && echo "已推送" || echo "未推送" )"
    echo "========================================="
}

# 脚本帮助
show_help() {
    echo "使用方式: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  --push              构建并推送到镜像仓库"
    echo "  --cleanup           推送后清理本地镜像（保留latest）"
    echo "  --version=VERSION   指定构建版本（默认: 1.0.0）"
    echo "  --registry=REGISTRY 指定Docker仓库（默认: registry.ai-ready.cn）"
    echo "  --username=USER     Docker仓库用户名"
    echo "  --password=PASS     Docker仓库密码"
    echo "  --help              显示帮助信息"
    echo ""
    echo "示例:"
    echo "  $0                          # 仅本地构建"
    echo "  $0 --push                   # 构建并推送"
    echo "  $0 --push --cleanup         # 构建、推送并清理"
    echo "  $0 --version=2.0.0 --push   # 指定版本并推送"
    echo ""
    echo "环境变量:"
    echo "  DOCKER_USERNAME      Docker仓库用户名"
    echo "  DOCKER_PASSWORD      Docker仓库密码"
    echo "  BUILD_VERSION        构建版本号"
}

# 解析命令行参数
parse_arguments() {
    local push=false
    local cleanup=false
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            --push)
                push=true
                shift
                ;;
            --cleanup)
                cleanup=true
                shift
                ;;
            --version=*)
                BUILD_VERSION="${1#*=}"
                shift
                ;;
            --registry=*)
                DOCKER_REGISTRY="${1#*=}"
                shift
                ;;
            --username=*)
                DOCKER_USERNAME="${1#*=}"
                shift
                ;;
            --password=*)
                DOCKER_PASSWORD="${1#*=}"
                shift
                ;;
            --help|-h)
                show_help
                exit 0
                ;;
            *)
                log_error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # 执行主构建流程
    main_build_process "$push" "$cleanup"
}

# 脚本入口
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    parse_arguments "$@"
fi