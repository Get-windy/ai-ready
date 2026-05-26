#!/bin/bash
# AI-Ready Canary Rollout Script
# 金丝雀发布自动化脚本

set -euo pipefail

# 配置
NAMESPACE="ai-ready"
DEPLOYMENT_STABLE="ai-ready-api-stable"
DEPLOYMENT_CANARY="ai-ready-api-canary"
VIRTUAL_SERVICE="ai-ready-api"
HEALTH_THRESHOLD=95
ERROR_THRESHOLD=5

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
    
    local deps=("kubectl" "istioctl")
    for dep in "${deps[@]}"; do
        if ! command -v "$dep" &> /dev/null; then
            error "$dep 未安装"
            exit 1
        fi
    done
    
    # 检查kubectl连接
    if ! kubectl cluster-info &> /dev/null; then
        error "无法连接到Kubernetes集群"
        exit 1
    fi
    
    log "依赖检查通过"
}

# 获取金丝雀健康评分
get_canary_health() {
    local health_score=$(kubectl exec -n istio-system deploy/prometheus -- \
        wget -qO- 'http://localhost:9090/api/v1/query?query=canary:health_score' 2>/dev/null | \
        jq -r '.data.result[0].value[1] // 0')
    
    if [[ -z "$health_score" || "$health_score" == "null" ]]; then
        echo "0"
    else
        echo "$health_score"
    fi
}

# 获取金丝雀错误率
get_canary_error_rate() {
    local error_rate=$(kubectl exec -n istio-system deploy/prometheus -- \
        wget -qO- 'http://localhost:9090/api/v1/query?query=sum(rate(http_requests_total{version=\"canary\",status=~\"5..\"}[5m]))/sum(rate(http_requests_total{version=\"canary\"}[5m]))' 2>/dev/null | \
        jq -r '.data.result[0].value[1] // 0')
    
    if [[ -z "$error_rate" || "$error_rate" == "null" ]]; then
        echo "0"
    else
        # 转换为百分比
        echo "$(echo "$error_rate * 100" | bc -l)"
    fi
}

# 更新流量权重
update_traffic_weight() {
    local canary_weight=$1
    local stable_weight=$((100 - canary_weight))
    
    log "更新流量权重: Canary ${canary_weight}%, Stable ${stable_weight}%"
    
    kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: ${VIRTUAL_SERVICE}
  namespace: ${NAMESPACE}
spec:
  hosts:
    - ai-ready-api.ai-ready.svc.cluster.local
  http:
    - route:
        - destination:
            host: ${DEPLOYMENT_STABLE}
            port:
              number: 8080
          weight: ${stable_weight}
        - destination:
            host: ${DEPLOYMENT_CANARY}
            port:
              number: 8080
          weight: ${canary_weight}
EOF
    
    log "流量权重更新完成"
}

# 检查金丝雀Pod状态
check_canary_pods() {
    log "检查金丝雀Pod状态..."
    
    local ready_replicas=$(kubectl get deployment "${DEPLOYMENT_CANARY}" -n "${NAMESPACE}" -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
    local desired_replicas=$(kubectl get deployment "${DEPLOYMENT_CANARY}" -n "${NAMESPACE}" -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "0")
    
    if [[ "$ready_replicas" -ge "$desired_replicas" ]]; then
        log "金丝雀Pod全部就绪: ${ready_replicas}/${desired_replicas}"
        return 0
    else
        warn "金丝雀Pod未全部就绪: ${ready_replicas}/${desired_replicas}"
        return 1
    fi
}

# 等待金丝雀就绪
wait_for_canary_ready() {
    log "等待金丝雀Pod就绪..."
    
    local retries=0
    local max_retries=30
    
    while [[ $retries -lt $max_retries ]]; do
        if check_canary_pods; then
            return 0
        fi
        
        retries=$((retries + 1))
        sleep 10
    done
    
    error "金丝雀Pod未能在规定时间内就绪"
    return 1
}

# 分析金丝雀指标
analyze_canary() {
    log "分析金丝雀指标..."
    
    local health_score=$(get_canary_health)
    local error_rate=$(get_canary_error_rate)
    
    info "健康评分: ${health_score}"
    info "错误率: ${error_rate}%"
    
    # 健康评分 > 阈值 且 错误率 < 阈值
    if (( $(echo "$health_score > $HEALTH_THRESHOLD" | bc -l) )) && \
       (( $(echo "$error_rate < $ERROR_THRESHOLD" | bc -l) )); then
        log "金丝雀分析通过 ✓"
        return 0
    else
        warn "金丝雀分析未通过 ✗"
        return 1
    fi
}

# 金丝雀发布主流程
canary_rollout() {
    local new_version=$1
    local steps=${2:-"5,25,50,75,100"}
    local step_duration=${3:-300}  # 每步持续时间(秒)
    
    log "=========================================="
    log "开始金丝雀发布"
    log "新版本: ${new_version}"
    log "步骤: ${steps}"
    log "每步持续时间: ${step_duration}秒"
    log "=========================================="
    
    # 1. 部署金丝雀版本
    log "步骤 1: 部署金丝雀版本..."
    kubectl set image deployment/"${DEPLOYMENT_CANARY}" \
        api="ghcr.io/ai-ready/ai-ready-api:${new_version}" \
        -n "${NAMESPACE}"
    
    # 2. 等待金丝雀就绪
    if ! wait_for_canary_ready; then
        error "金丝雀部署失败，执行回滚"
        rollback_canary
        exit 1
    fi
    
    # 3. 渐进式流量切换
    IFS=',' read -ra WEIGHTS <<< "$steps"
    
    for weight in "${WEIGHTS[@]}"; do
        log "=========================================="
        log "切换流量: ${weight}% -> Canary"
        log "=========================================="
        
        update_traffic_weight "$weight"
        
        log "等待 ${step_duration} 秒进行观察..."
        sleep "$step_duration"
        
        # 分析指标
        if ! analyze_canary; then
            warn "金丝雀指标异常，暂停发布"
            
            read -p "是否继续? (y/n/rollback): " choice
            case "$choice" in
                y|Y)
                    log "继续发布..."
                    ;;
                n|N)
                    log "暂停发布"
                    exit 0
                    ;;
                rollback)
                    rollback_canary
                    exit 1
                    ;;
                *)
                    rollback_canary
                    exit 1
                    ;;
            esac
        fi
    done
    
    # 4. 完成发布
    log "=========================================="
    log "金丝雀发布完成!"
    log "=========================================="
    log "新版本已接管全部流量"
    
    # 5. 更新稳定版本
    log "更新稳定版本镜像..."
    kubectl set image deployment/"${DEPLOYMENT_STABLE}" \
        api="ghcr.io/ai-ready/ai-ready-api:${new_version}" \
        -n "${NAMESPACE}"
    
    # 6. 重置流量
    log "重置流量分配..."
    update_traffic_weight 0
    
    # 7. 缩容金丝雀
    log "缩容金丝雀版本..."
    kubectl scale deployment "${DEPLOYMENT_CANARY}" --replicas=0 -n "${NAMESPACE}"
    
    log "发布流程完成!"
}

# 回滚金丝雀
rollback_canary() {
    log "=========================================="
    warn "执行金丝雀回滚"
    log "=========================================="
    
    # 1. 将所有流量切回稳定版本
    update_traffic_weight 0
    
    # 2. 缩容金丝雀
    kubectl scale deployment "${DEPLOYMENT_CANARY}" --replicas=0 -n "${NAMESPACE}"
    
    log "回滚完成"
}

# 快速回滚
quick_rollback() {
    log "=========================================="
    warn "执行快速回滚"
    log "=========================================="
    
    rollback_canary
    
    # 可选：回滚稳定版本
    read -p "是否回滚稳定版本? (y/n): " choice
    if [[ "$choice" == "y" || "$choice" == "Y" ]]; then
        read -p "输入要回滚的版本: " version
        kubectl set image deployment/"${DEPLOYMENT_STABLE}" \
            api="ghcr.io/ai-ready/ai-ready-api:${version}" \
            -n "${NAMESPACE}"
        log "稳定版本已回滚到 ${version}"
    fi
}

# 显示状态
show_status() {
    log "当前发布状态:"
    echo "=========================================="
    
    # 显示Deployment状态
    echo "Deployments:"
    kubectl get deployments -n "${NAMESPACE}" -l app=ai-ready-api
    
    echo ""
    echo "Pods:"
    kubectl get pods -n "${NAMESPACE}" -l app=ai-ready-api
    
    echo ""
    echo "VirtualService:"
    kubectl get virtualservice "${VIRTUAL_SERVICE}" -n "${NAMESPACE}" -o yaml | grep -A 10 "route:"
    
    echo ""
    echo "金丝雀健康评分: $(get_canary_health)"
    echo "金丝雀错误率: $(get_canary_error_rate)%"
    echo "=========================================="
}

# 显示帮助
show_help() {
    cat << EOF
AI-Ready 金丝雀发布脚本

用法: $0 [命令] [参数]

命令:
    rollout <version> [steps] [duration]  执行金丝雀发布
    rollback                              回滚金丝雀
    status                                显示当前状态
    help                                  显示帮助

参数:
    version     新版本号 (如: v1.1.0)
    steps       流量切换步骤 (默认: 5,25,50,75,100)
    duration    每步持续时间秒数 (默认: 300)

示例:
    # 执行标准金丝雀发布
    $0 rollout v1.1.0

    # 自定义步骤和持续时间
    $0 rollout v1.1.0 "10,30,50,100" 600

    # 查看状态
    $0 status

    # 回滚
    $0 rollback
EOF
}

# 主函数
main() {
    local command=${1:-help}
    
    case "$command" in
        rollout)
            if [[ -z "${2:-}" ]]; then
                error "请指定版本号"
                show_help
                exit 1
            fi
            check_prerequisites
            canary_rollout "$2" "${3:-}" "${4:-}"
            ;;
        rollback)
            check_prerequisites
            quick_rollback
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
