#!/bin/bash
# Sprint 29: 部署回滚脚本
# 功能: 快速回滚到指定版本
# 版本: v1.0

set -e

# 配置
NAMESPACE=${1:-"ai-ready"}
DEPLOYMENT_NAME=${2:-"ai-ready-api"}
REVISION=${3:-""}  # 为空则回滚到上一个版本

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

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

log_debug() {
    echo -e "${BLUE}[DEBUG]${NC} $1"
}

# 检查kubectl
if ! command -v kubectl &> /dev/null; then
    log_error "kubectl 未安装"
    exit 1
fi

# 显示回滚历史
show_history() {
    log_info "部署历史:"
    echo "========================================"
    kubectl rollout history deployment/"$DEPLOYMENT_NAME" -n "$NAMESPACE"
    echo "========================================"
}

# 获取当前版本
get_current_revision() {
    kubectl get deployment "$DEPLOYMENT_NAME" -n "$NAMESPACE" -o jsonpath='{.metadata.annotations.deployment\.kubernetes\.io/revision}'
}

# 获取指定版本的镜像
get_revision_image() {
    local rev=$1
    kubectl rollout history deployment/"$DEPLOYMENT_NAME" -n "$NAMESPACE" --revision="$rev" -o jsonpath='{.spec.template.spec.containers[0].image}'
}

# 执行回滚
perform_rollback() {
    local target_revision=$1
    
    if [ -n "$target_revision" ]; then
        log_info "回滚到版本: $target_revision"
        kubectl rollout undo deployment/"$DEPLOYMENT_NAME" -n "$NAMESPACE" --to-revision="$target_revision"
    else
        log_info "回滚到上一个版本"
        kubectl rollout undo deployment/"$DEPLOYMENT_NAME" -n "$NAMESPACE"
    fi
}

# 等待回滚完成
wait_for_rollback() {
    log_info "等待回滚完成..."
    
    if kubectl rollout status deployment/"$DEPLOYMENT_NAME" -n "$NAMESPACE" --timeout=300s; then
        log_info "回滚成功完成"
        return 0
    else
        log_error "回滚超时或失败"
        return 1
    fi
}

# 验证回滚
verify_rollback() {
    log_info "验证回滚结果..."
    
    local new_revision
    new_revision=$(get_current_revision)
    log_info "当前版本: $new_revision"
    
    local image
    image=$(kubectl get deployment "$DEPLOYMENT_NAME" -n "$NAMESPACE" -o jsonpath='{.spec.template.spec.containers[0].image}')
    log_info "当前镜像: $image"
    
    # 检查Pod状态
    local ready_pods
    ready_pods=$(kubectl get pods -n "$NAMESPACE" -l app="$DEPLOYMENT_NAME" --field-selector=status.phase=Running -o jsonpath='{.items[*].metadata.name}' | wc -w)
    
    local desired_replicas
    desired_replicas=$(kubectl get deployment "$DEPLOYMENT_NAME" -n "$NAMESPACE" -o jsonpath='{.spec.replicas}')
    
    if [ "$ready_pods" -ge "$desired_replicas" ]; then
        log_info "所有Pod已就绪: $ready_pods/$desired_replicas"
        return 0
    else
        log_error "Pod未全部就绪: $ready_pods/$desired_replicas"
        return 1
    fi
}

# 主函数
main() {
    log_info "开始回滚操作..."
    log_info "命名空间: $NAMESPACE"
    log_info "部署名称: $DEPLOYMENT_NAME"
    
    # 显示当前状态
    local current_revision
    current_revision=$(get_current_revision)
    log_info "当前版本: $current_revision"
    
    show_history
    
    # 确认回滚
    if [ -z "$REVISION" ]; then
        log_warn "未指定版本，将回滚到上一个版本"
        read -p "确认回滚? (y/N): " confirm
        if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
            log_info "取消回滚"
            exit 0
        fi
    else
        log_info "目标版本: $REVISION"
        local target_image
        target_image=$(get_revision_image "$REVISION")
        log_info "目标镜像: $target_image"
        
        read -p "确认回滚到版本 $REVISION? (y/N): " confirm
        if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
            log_info "取消回滚"
            exit 0
        fi
    fi
    
    # 执行回滚
    if perform_rollback "$REVISION"; then
        log_info "回滚命令已执行"
    else
        log_error "回滚命令执行失败"
        exit 1
    fi
    
    # 等待回滚完成
    if wait_for_rollback; then
        log_info "回滚完成"
    else
        log_error "回滚失败"
        exit 1
    fi
    
    # 验证回滚
    if verify_rollback; then
        log_info "回滚验证通过"
    else
        log_error "回滚验证失败"
        exit 1
    fi
    
    # 显示最终状态
    echo ""
    log_info "最终部署状态:"
    kubectl get deployment "$DEPLOYMENT_NAME" -n "$NAMESPACE" -o wide
    echo ""
    kubectl get pods -n "$NAMESPACE" -l app="$DEPLOYMENT_NAME" -o wide
    
    log_info "回滚操作完成！"
}

# 显示帮助
show_help() {
    cat << EOF
Sprint 29 部署回滚脚本

用法: $0 [NAMESPACE] [DEPLOYMENT_NAME] [REVISION]

参数:
  NAMESPACE       Kubernetes命名空间 (默认: ai-ready)
  DEPLOYMENT_NAME 部署名称 (默认: ai-ready-api)
  REVISION        目标版本号 (默认: 上一个版本)

示例:
  $0                                    # 回滚到上一个版本
  $0 ai-ready ai-ready-api              # 自定义命名空间和部署
  $0 ai-ready ai-ready-api 3            # 回滚到版本3

EOF
}

# 处理帮助参数
if [ "$1" == "-h" ] || [ "$1" == "--help" ]; then
    show_help
    exit 0
fi

# 运行主函数
main
