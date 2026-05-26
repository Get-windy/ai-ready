#!/bin/bash
# AI-Ready Blue-Green Deployment Switch Script
# 蓝绿部署流量切换脚本

set -euo pipefail

# 配置
NAMESPACE="ai-ready"
SERVICE_ACTIVE="ai-ready-api-active"
DEPLOYMENT_BLUE="ai-ready-api-blue"
DEPLOYMENT_GREEN="ai-ready-api-green"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log() {
    echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[$(date '+%Y-%m-%d %H:%M:%S')] WARNING:${NC} $1"
}

error() {
    echo -e "${RED}[$(date '+%Y-%m-%d %H:%M:%S')] ERROR:${NC} $1"
}

info() {
    echo -e "${BLUE}[$(date '+%Y-%m-%d %H:%M:%S')] INFO:${NC} $1"
}

# 检查依赖
check_prerequisites() {
    log "检查依赖..."
    
    if ! command -v kubectl &> /dev/null; then
        error "kubectl 未安装"
        exit 1
    fi
    
    if ! kubectl cluster-info &> /dev/null; then
        error "无法连接到Kubernetes集群"
        exit 1
    fi
    
    log "依赖检查通过"
}

# 获取当前活跃颜色
get_active_color() {
    local selector=$(kubectl get service "${SERVICE_ACTIVE}" -n "${NAMESPACE}" -o jsonpath='{.spec.selector.version}' 2>/dev/null)
    
    if [[ "$selector" == "blue" ]]; then
        echo "blue"
    elif [[ "$selector" == "green" ]]; then
        echo "green"
    else
        echo "unknown"
    fi
}

# 获取非活跃颜色
get_inactive_color() {
    local active=$(get_active_color)
    
    if [[ "$active" == "blue" ]]; then
        echo "green"
    elif [[ "$active" == "green" ]]; then
        echo "blue"
    else
        echo "unknown"
    fi
}

# 检查Deployment状态
check_deployment() {
    local color=$1
    local deployment="ai-ready-api-${color}"
    
    log "检查 ${color} 版本状态..."
    
    local ready_replicas=$(kubectl get deployment "${deployment}" -n "${NAMESPACE}" -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
    local desired_replicas=$(kubectl get deployment "${deployment}" -n "${NAMESPACE}" -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "0")
    
    if [[ "$ready_replicas" -ge "$desired_replicas" && "$desired_replicas" -gt 0 ]]; then
        log "${color} 版本就绪: ${ready_replicas}/${desired_replicas}"
        return 0
    else
        warn "${color} 版本未就绪: ${ready_replicas}/${desired_replicas}"
        return 1
    fi
}

# 健康检查
health_check() {
    local color=$1
    local service="ai-ready-api-${color}"
    
    log "执行健康检查: ${color}..."
    
    # 获取Pod IP
    local pod_ip=$(kubectl get pods -n "${NAMESPACE}" -l "app=ai-ready-api,version=${color}" -o jsonpath='{.items[0].status.podIP}' 2>/dev/null)
    
    if [[ -z "$pod_ip" ]]; then
        error "无法获取 ${color} Pod IP"
        return 1
    fi
    
    # 执行健康检查
    local health_status=$(kubectl exec -n "${NAMESPACE}" "$(kubectl get pods -n "${NAMESPACE}" -l "app=ai-ready-api,version=${color}" -o jsonpath='{.items[0].metadata.name}')" -- \
        wget -qO- "http://localhost:8080/actuator/health" 2>/dev/null | jq -r '.status' || echo "DOWN")
    
    if [[ "$health_status" == "UP" ]]; then
        log "健康检查通过: ${color}"
        return 0
    else
        error "健康检查失败: ${color} (状态: ${health_status})"
        return 1
    fi
}

# 切换流量
switch_traffic() {
    local target_color=$1
    
    log "=========================================="
    log "切换流量到: ${target_color}"
    log "=========================================="
    
    # 更新Service selector
    kubectl patch service "${SERVICE_ACTIVE}" -n "${NAMESPACE}" -p "{\"spec\":{\"selector\":{\"version\":\"${target_color}\"}}}"
    
    log "流量已切换到 ${target_color}"
}

# 部署新版本
deploy_new_version() {
    local version=$1
    local target_color=$2
    local deployment="ai-ready-api-${target_color}"
    
    log "=========================================="
    log "部署新版本到 ${target_color}"
    log "版本: ${version}"
    log "=========================================="
    
    # 更新镜像
    kubectl set image deployment/"${deployment}" \
        api="ghcr.io/ai-ready/ai-ready-api:${version}" \
        -n "${NAMESPACE}"
    
    # 等待滚动更新完成
    log "等待滚动更新完成..."
    kubectl rollout status deployment/"${deployment}" -n "${NAMESPACE}" --timeout=300s
    
    log "部署完成"
}

# 蓝绿发布主流程
blue_green_deploy() {
    local new_version=$1
    
    log "=========================================="
    log "开始蓝绿部署"
    log "新版本: ${new_version}"
    log "=========================================="
    
    local active_color=$(get_active_color)
    local inactive_color=$(get_inactive_color)
    
    info "当前活跃版本: ${active_color}"
    info "目标版本: ${inactive_color}"
    
    # 1. 部署到非活跃环境
    deploy_new_version "$new_version" "$inactive_color"
    
    # 2. 健康检查
    if ! health_check "$inactive_color"; then
        error "新版本健康检查失败"
        read -p "是否继续? (y/n): " choice
        if [[ "$choice" != "y" && "$choice" != "Y" ]]; then
            log "取消部署"
            exit 1
        fi
    fi
    
    # 3. 切换流量
    read -p "确认切换流量到 ${inactive_color}? (y/n): " confirm
    if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
        log "取消切换"
        exit 0
    fi
    
    switch_traffic "$inactive_color"
    
    # 4. 验证
    log "验证新版本..."
    sleep 10
    
    if health_check "$inactive_color"; then
        log "=========================================="
        log "蓝绿部署成功!"
        log "=========================================="
        log "新版本 ${new_version} 已上线"
        log "旧版本 ${active_color} 保留用于回滚"
    else
        warn "新版本验证失败，建议回滚"
    fi
}

# 快速切换（不部署，仅切换）
quick_switch() {
    local target_color=$1
    
    if [[ "$target_color" != "blue" && "$target_color" != "green" ]]; then
        error "无效的颜色: ${target_color}"
        exit 1
    fi
    
    log "快速切换到: ${target_color}"
    
    if ! check_deployment "$target_color"; then
        error "目标版本未就绪，无法切换"
        exit 1
    fi
    
    switch_traffic "$target_color"
    log "切换完成"
}

# 回滚
rollback() {
    log "=========================================="
    warn "执行回滚"
    log "=========================================="
    
    local current=$(get_active_color)
    local previous=$(get_inactive_color)
    
    log "当前版本: ${current}"
    log "回滚到: ${previous}"
    
    if ! check_deployment "$previous"; then
        error "回滚目标未就绪"
        exit 1
    fi
    
    switch_traffic "$previous"
    
    log "回滚完成"
}

# 缩容旧版本
scale_down_old() {
    local active=$(get_active_color)
    local old=$(get_inactive_color)
    
    log "缩容旧版本: ${old}"
    
    kubectl scale deployment "ai-ready-api-${old}" --replicas=0 -n "${NAMESPACE}"
    
    log "旧版本已缩容"
}

# 显示状态
show_status() {
    log "当前蓝绿部署状态:"
    echo "=========================================="
    
    echo "Deployments:"
    kubectl get deployments -n "${NAMESPACE}" -l app=ai-ready-api
    
    echo ""
    echo "Services:"
    kubectl get services -n "${NAMESPACE}" -l app=ai-ready-api
    
    echo ""
    echo "当前活跃版本: $(get_active_color)"
    echo "=========================================="
}

# 显示帮助
show_help() {
    cat << EOF
AI-Ready 蓝绿部署切换脚本

用法: $0 [命令] [参数]

命令:
    deploy <version>      部署新版本并切换
    switch <blue|green>   快速切换到指定颜色
    rollback              回滚到上一个版本
    scale-down            缩容旧版本
    status                显示当前状态
    help                  显示帮助

示例:
    # 部署新版本
    $0 deploy v1.1.0

    # 快速切换到green
    $0 switch green

    # 回滚
    $0 rollback

    # 查看状态
    $0 status
EOF
}

# 主函数
main() {
    local command=${1:-help}
    
    case "$command" in
        deploy)
            if [[ -z "${2:-}" ]]; then
                error "请指定版本号"
                show_help
                exit 1
            fi
            check_prerequisites
            blue_green_deploy "$2"
            ;;
        switch)
            if [[ -z "${2:-}" ]]; then
                error "请指定颜色 (blue 或 green)"
                show_help
                exit 1
            fi
            check_prerequisites
            quick_switch "$2"
            ;;
        rollback)
            check_prerequisites
            rollback
            ;;
        scale-down)
            check_prerequisites
            scale_down_old
            ;;
        status)
            check_prerequisites
            show_status
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            error "未知命令: $command"
            show_help
            exit 1
            ;;
    esac
}

main "$@"
