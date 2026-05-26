#!/bin/bash
# Sprint 29: 部署健康检查脚本
# 功能: 验证部署是否成功，失败时自动回滚
# 版本: v1.0

set -e

# 配置
NAMESPACE=${1:-"ai-ready"}
DEPLOYMENT_NAME=${2:-"ai-ready-api"}
TIMEOUT=${3:-300}
HEALTH_ENDPOINT=${4:-"/actuator/health"}
SERVICE_URL=${5:-""}

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

# 检查kubectl
if ! command -v kubectl &> /dev/null; then
    log_error "kubectl 未安装"
    exit 1
fi

# 获取当前镜像版本 (用于回滚)
get_current_image() {
    kubectl get deployment "$DEPLOYMENT_NAME" -n "$NAMESPACE" -o jsonpath='{.spec.template.spec.containers[0].image}'
}

# 等待部署完成
wait_for_deployment() {
    log_info "等待部署完成 (超时: ${TIMEOUT}s)..."
    
    if kubectl rollout status deployment/"$DEPLOYMENT_NAME" -n "$NAMESPACE" --timeout="${TIMEOUT}s"; then
        log_info "部署成功完成"
        return 0
    else
        log_error "部署超时或失败"
        return 1
    fi
}

# 检查Pod就绪状态
check_pods_ready() {
    log_info "检查Pod就绪状态..."
    
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

# 健康检查
health_check() {
    log_info "执行健康检查..."
    
    local retries=5
    local delay=10
    
    for i in $(seq 1 $retries); do
        log_info "健康检查尝试 $i/$retries..."
        
        # 使用kubectl port-forward进行内部检查
        local pod_name
        pod_name=$(kubectl get pods -n "$NAMESPACE" -l app="$DEPLOYMENT_NAME" -o jsonpath='{.items[0].metadata.name}')
        
        if kubectl exec -n "$NAMESPACE" "$pod_name" -- curl -fs "http://localhost:8080$HEALTH_ENDPOINT" > /dev/null 2>&1; then
            log_info "健康检查通过"
            return 0
        fi
        
        if [ $i -lt $retries ]; then
            log_warn "健康检查失败，${delay}秒后重试..."
            sleep $delay
        fi
    done
    
    log_error "健康检查失败"
    return 1
}

# 执行回滚
rollback() {
    log_error "执行回滚操作..."
    
    if kubectl rollout undo deployment/"$DEPLOYMENT_NAME" -n "$NAMESPACE"; then
        log_info "回滚命令已执行"
        
        # 等待回滚完成
        if kubectl rollout status deployment/"$DEPLOYMENT_NAME" -n "$NAMESPACE" --timeout="${TIMEOUT}s"; then
            log_info "回滚成功"
            return 0
        else
            log_error "回滚失败"
            return 1
        fi
    else
        log_error "回滚命令执行失败"
        return 1
    fi
}

# 获取部署状态
get_deployment_status() {
    log_info "获取部署状态..."
    
    echo "========================================"
    echo "部署: $DEPLOYMENT_NAME"
    echo "命名空间: $NAMESPACE"
    echo "========================================"
    
    kubectl get deployment "$DEPLOYMENT_NAME" -n "$NAMESPACE" -o wide
    echo ""
    
    echo "Pod状态:"
    kubectl get pods -n "$NAMESPACE" -l app="$DEPLOYMENT_NAME" -o wide
    echo ""
    
    echo "事件:"
    kubectl get events -n "$NAMESPACE" --field-selector involvedObject.name="$DEPLOYMENT_NAME" --sort-by='.lastTimestamp' | tail -10
    echo ""
    
    echo " rollout历史:"
    kubectl rollout history deployment/"$DEPLOYMENT_NAME" -n "$NAMESPACE"
}

# 主函数
main() {
    log_info "开始健康检查..."
    log_info "命名空间: $NAMESPACE"
    log_info "部署名称: $DEPLOYMENT_NAME"
    
    # 保存当前镜像版本
    CURRENT_IMAGE=$(get_current_image)
    log_info "当前镜像: $CURRENT_IMAGE"
    
    # 执行检查
    local failed=0
    
    if ! wait_for_deployment; then
        failed=1
    fi
    
    if ! check_pods_ready; then
        failed=1
    fi
    
    if ! health_check; then
        failed=1
    fi
    
    # 输出状态
    get_deployment_status
    
    if [ $failed -eq 1 ]; then
        log_error "健康检查失败，准备回滚..."
        rollback
        exit 1
    fi
    
    log_info "所有检查通过，部署成功！"
    exit 0
}

# 显示帮助
show_help() {
    cat << EOF
Sprint 29 部署健康检查脚本

用法: $0 [NAMESPACE] [DEPLOYMENT_NAME] [TIMEOUT] [HEALTH_ENDPOINT] [SERVICE_URL]

参数:
  NAMESPACE       Kubernetes命名空间 (默认: ai-ready)
  DEPLOYMENT_NAME 部署名称 (默认: ai-ready-api)
  TIMEOUT         超时时间秒数 (默认: 300)
  HEALTH_ENDPOINT 健康检查端点 (默认: /actuator/health)
  SERVICE_URL     服务URL (可选)

示例:
  $0                                    # 使用默认参数
  $0 ai-ready-staging ai-ready-api 600  # 自定义参数

EOF
}

# 处理帮助参数
if [ "$1" == "-h" ] || [ "$1" == "--help" ]; then
    show_help
    exit 0
fi

# 运行主函数
main
