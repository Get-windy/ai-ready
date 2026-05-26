#!/bin/bash
# 🚀 ERP批次管理模块自动化故障恢复脚本
# 版本: 1.0.0
# 创建时间: 2026-05-05
# 紧急优化: 自动处理常见故障，减少人工干预

set -euo pipefail

# 配置变量
NAMESPACE="erp-prod"
DEPLOYMENT="erp-batch-sn-deployment"
SERVICE="erp-batch-sn-service"
CONTAINER="erp-batch-sn"
LOG_DIR="/app/logs"
BACKUP_DIR="/backup/erp-batch-sn"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

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

# 检查Kubernetes连接
check_k8s_connection() {
    log_info "检查Kubernetes集群连接..."
    if kubectl cluster-info >/dev/null 2>&1; then
        log_success "Kubernetes集群连接正常"
    else
        log_error "无法连接到Kubernetes集群"
        exit 1
    fi
}

# 检查Pod状态
check_pod_status() {
    log_info "检查Pod状态..."
    
    # 获取Pod列表
    local pods=$(kubectl get pods -n "$NAMESPACE" -l app="$DEPLOYMENT" -o jsonpath='{.items[*].metadata.name}')
    
    if [ -z "$pods" ]; then
        log_error "未找到任何Pod"
        return 1
    fi
    
    for pod in $pods; do
        log_info "检查Pod: $pod"
        
        # 检查Pod状态
        local pod_status=$(kubectl get pod "$pod" -n "$NAMESPACE" -o jsonpath='{.status.phase}')
        local ready=$(kubectl get pod "$pod" -n "$NAMESPACE" -o jsonpath='{.status.containerStatuses[0].ready}')
        local restart_count=$(kubectl get pod "$pod" -n "$NAMESPACE" -o jsonpath='{.status.containerStatuses[0].restartCount}')
        
        echo "  - 状态: $pod_status"
        echo "  - 就绪: $ready"
        echo "  - 重启次数: $restart_count"
        
        if [ "$pod_status" != "Running" ] || [ "$ready" != "true" ]; then
            log_warn "Pod $pod 状态异常"
            return 1
        fi
        
        if [ "$restart_count" -gt 10 ]; then
            log_warn "Pod $pod 重启次数过多: $restart_count"
        fi
    done
    
    log_success "所有Pod状态正常"
    return 0
}

# 检查应用健康状态
check_app_health() {
    log_info "检查应用健康状态..."
    
    local pods=$(kubectl get pods -n "$NAMESPACE" -l app="$DEPLOYMENT" -o jsonpath='{.items[0].metadata.name}')
    
    if [ -z "$pods" ]; then
        log_error "未找到Pod"
        return 1
    fi
    
    local pod=$(echo "$pods" | awk '{print $1}')
    
    # 检查健康端点
    local health_url="http://$SERVICE.$NAMESPACE.svc.cluster.local:8080/actuator/health"
    local health_response=$(kubectl exec "$pod" -n "$NAMESPACE" -- curl -s -o /dev/null -w "%{http_code}" "$health_url" || echo "ERROR")
    
    if [ "$health_response" == "200" ]; then
        log_success "应用健康检查通过"
        return 0
    else
        log_warn "应用健康检查失败，HTTP状态码: $health_response"
        return 1
    fi
}

# 检查数据库连接
check_database_connection() {
    log_info "检查数据库连接..."
    
    local pods=$(kubectl get pods -n "$NAMESPACE" -l app="$DEPLOYMENT" -o jsonpath='{.items[0].metadata.name}')
    
    if [ -z "$pods" ]; then
        log_error "未找到Pod"
        return 1
    fi
    
    local pod=$(echo "$pods" | awk '{print $1}')
    
    # 执行数据库连接测试
    local db_test_result=$(kubectl exec "$pod" -n "$NAMESPACE" -- \
        bash -c 'timeout 10s java -cp /app/BOOT-INF/lib/*:/app/BOOT-INF/classes:/app org.springframework.boot.loader.JarLauncher --spring.profiles.active=test --check-database 2>&1 | grep -i "connection" | head -5' || true)
    
    if echo "$db_test_result" | grep -q -i "success\|ok\|connected"; then
        log_success "数据库连接正常"
        return 0
    else
        log_warn "数据库连接可能存在问题"
        echo "测试输出: $db_test_result"
        return 1
    fi
}

# 检查内存泄漏
check_memory_leak() {
    log_info "检查内存泄漏..."
    
    local pods=$(kubectl get pods -n "$NAMESPACE" -l app="$DEPLOYMENT" -o jsonpath='{.items[0].metadata.name}')
    
    if [ -z "$pods" ]; then
        log_error "未找到Pod"
        return 1
    fi
    
    local pod=$(echo "$pods" | awk '{print $1}')
    
    # 获取JVM内存使用情况
    local heap_used=$(kubectl exec "$pod" -n "$NAMESPACE" -- \
        bash -c 'jcmd 1 GC.heap_info 2>/dev/null | grep -E "used\s+[0-9]" | head -1 | awk "{print \$2}"' || echo "0")
    
    local heap_committed=$(kubectl exec "$pod" -n "$NAMESPACE" -- \
        bash -c 'jcmd 1 GC.heap_info 2>/dev/null | grep -E "committed\s+[0-9]" | head -1 | awk "{print \$2}"' || echo "0")
    
    if [ "$heap_used" -gt 0 ] && [ "$heap_committed" -gt 0 ]; then
        local usage_percentage=$((heap_used * 100 / heap_committed))
        echo "  - 堆内存使用: $heap_used KB / $heap_committed KB ($usage_percentage%)"
        
        if [ "$usage_percentage" -gt 90 ]; then
            log_warn "堆内存使用率过高，可能存在内存泄漏"
            return 1
        fi
    fi
    
    log_success "内存使用正常"
    return 0
}

# 备份Pod日志
backup_pod_logs() {
    log_info "备份Pod日志..."
    
    mkdir -p "$BACKUP_DIR/logs/$TIMESTAMP"
    
    local pods=$(kubectl get pods -n "$NAMESPACE" -l app="$DEPLOYMENT" -o jsonpath='{.items[*].metadata.name}')
    
    for pod in $pods; do
        log_info "备份Pod日志: $pod"
        
        # 备份标准输出日志
        kubectl logs "$pod" -n "$NAMESPACE" --tail=10000 > "$BACKUP_DIR/logs/$TIMESTAMP/${pod}_stdout.log" 2>/dev/null || true
        
        # 备份应用日志文件
        kubectl cp "$NAMESPACE/$pod:$LOG_DIR" "$BACKUP_DIR/logs/$TIMESTAMP/${pod}_app_logs/" >/dev/null 2>&1 || true
        
        # 备份堆转储文件（如果存在）
        kubectl cp "$NAMESPACE/$pod:$LOG_DIR/heapdump.hprof" "$BACKUP_DIR/logs/$TIMESTAMP/${pod}_heapdump.hprof" >/dev/null 2>&1 || true
    done
    
    log_success "Pod日志备份完成: $BACKUP_DIR/logs/$TIMESTAMP"
}

# 重启Pod
restart_pod() {
    local pod=$1
    local reason=$2
    
    log_warn "重启Pod: $pod (原因: $reason)"
    
    # 删除Pod（由Deployment自动重建）
    kubectl delete pod "$pod" -n "$NAMESPACE"
    
    # 等待Pod重新启动
    log_info "等待Pod重新启动..."
    sleep 30
    
    # 检查新Pod状态
    local new_pod=$(kubectl get pods -n "$NAMESPACE" -l app="$DEPLOYMENT" --sort-by='{.metadata.creationTimestamp}' -o jsonpath='{.items[-1].metadata.name}')
    
    if [ -n "$new_pod" ]; then
        log_success "Pod已重新启动: $new_pod"
        return 0
    else
        log_error "Pod重启失败"
        return 1
    fi
}

# 扩容Pod
scale_up_pods() {
    local current_replicas=$(kubectl get deployment "$DEPLOYMENT" -n "$NAMESPACE" -o jsonpath='{.spec.replicas}')
    local new_replicas=$((current_replicas + 1))
    
    log_warn "扩容Pod: $current_replicas -> $new_replicas"
    
    kubectl scale deployment "$DEPLOYMENT" -n "$NAMESPACE" --replicas="$new_replicas"
    
    log_success "扩容完成，等待新Pod就绪..."
    sleep 60
    
    check_pod_status
}

# 执行滚动更新
perform_rolling_update() {
    log_warn "执行滚动更新..."
    
    # 更新镜像触发滚动更新
    kubectl set image deployment/"$DEPLOYMENT" "$CONTAINER"=registry.ai-ready.cn/erp/erp-batch-sn:latest -n "$NAMESPACE"
    
    log_info "等待滚动更新完成..."
    
    # 等待更新完成
    kubectl rollout status deployment/"$DEPLOYMENT" -n "$NAMESPACE" --timeout=300s
    
    if [ $? -eq 0 ]; then
        log_success "滚动更新完成"
        return 0
    else
        log_error "滚动更新失败"
        return 1
    fi
}

# 回滚到上一版本
rollback_deployment() {
    log_warn "执行回滚操作..."
    
    kubectl rollout undo deployment/"$DEPLOYMENT" -n "$NAMESPACE"
    
    log_info "等待回滚完成..."
    
    kubectl rollout status deployment/"$DEPLOYMENT" -n "$NAMESPACE" --timeout=300s
    
    if [ $? -eq 0 ]; then
        log_success "回滚完成"
        return 0
    else
        log_error "回滚失败"
        return 1
    fi
}

# 清理旧日志和备份
cleanup_old_backups() {
    log_info "清理旧备份文件..."
    
    # 保留最近7天的备份
    find "$BACKUP_DIR" -type f -name "*.log" -mtime +7 -delete 2>/dev/null || true
    find "$BACKUP_DIR" -type d -mtime +7 -empty -delete 2>/dev/null || true
    
    log_success "旧备份清理完成"
}

# 主故障恢复流程
main_fault_recovery() {
    log_info "=== 开始ERP批次管理模块故障恢复流程 ==="
    
    # 1. 检查集群连接
    check_k8s_connection
    
    # 2. 备份当前日志
    backup_pod_logs
    
    # 3. 检查Pod状态
    if ! check_pod_status; then
        log_warn "Pod状态异常，尝试重启..."
        
        local pods=$(kubectl get pods -n "$NAMESPACE" -l app="$DEPLOYMENT" -o jsonpath='{.items[*].metadata.name}')
        for pod in $pods; do
            local pod_status=$(kubectl get pod "$pod" -n "$NAMESPACE" -o jsonpath='{.status.phase}')
            if [ "$pod_status" != "Running" ]; then
                restart_pod "$pod" "状态异常: $pod_status"
            fi
        done
        
        # 重新检查状态
        check_pod_status || {
            log_error "Pod状态修复失败"
            exit 1
        }
    fi
    
    # 4. 检查应用健康
    if ! check_app_health; then
        log_warn "应用健康检查失败，尝试滚动更新..."
        perform_rolling_update
        check_app_health || {
            log_error "应用健康修复失败，尝试回滚..."
            rollback_deployment
        }
    fi
    
    # 5. 检查数据库连接
    if ! check_database_connection; then
        log_warn "数据库连接问题，检查连接池配置..."
        # 这里可以添加连接池参数调整逻辑
    fi
    
    # 6. 检查内存泄漏
    if ! check_memory_leak; then
        log_warn "检测到可能的内存泄漏，重启Pod..."
        
        local pods=$(kubectl get pods -n "$NAMESPACE" -l app="$DEPLOYMENT" -o jsonpath='{.items[*].metadata.name}')
        for pod in $pods; do
            restart_pod "$pod" "内存泄漏检测"
        done
    fi
    
    # 7. 检查是否需要扩容
    local cpu_usage=$(kubectl top pods -n "$NAMESPACE" -l app="$DEPLOYMENT" --no-headers | awk '{print $2}' | sed 's/m//' | head -1)
    local pod_count=$(kubectl get pods -n "$NAMESPACE" -l app="$DEPLOYMENT" --no-headers | wc -l)
    
    if [ -n "$cpu_usage" ] && [ "$cpu_usage" -gt 800 ] && [ "$pod_count" -lt 5 ]; then
        log_warn "CPU使用率过高($cpu_usage m)，执行扩容..."
        scale_up_pods
    fi
    
    # 8. 清理旧备份
    cleanup_old_backups
    
    log_success "=== 故障恢复流程完成 ==="
    
    # 生成报告
    echo "========================================="
    echo "故障恢复报告 - $TIMESTAMP"
    echo "========================================="
    echo "状态: 完成"
    echo "处理的Pod数量: $pod_count"
    echo "备份位置: $BACKUP_DIR/logs/$TIMESTAMP"
    echo "建议后续检查:"
    echo "  1. 监控告警配置是否生效"
    echo "  2. 应用性能指标是否正常"
    echo "  3. 数据库连接池使用情况"
    echo "========================================="
}

# 监控模式：持续监控并自动恢复
monitor_mode() {
    log_info "启动监控模式，每5分钟检查一次..."
    
    while true; do
        echo ""
        log_info "=== 开始周期性检查 ==="
        
        if ! check_pod_status; then
            main_fault_recovery
        elif ! check_app_health; then
            log_warn "检测到应用健康问题，执行恢复流程..."
            main_fault_recovery
        else
            log_success "系统状态正常"
        fi
        
        log_info "等待5分钟后继续检查..."
        sleep 300
    done
}

# 脚本入口
case "${1:-}" in
    "monitor")
        monitor_mode
        ;;
    "recover")
        main_fault_recovery
        ;;
    "check")
        check_k8s_connection
        check_pod_status
        check_app_health
        check_database_connection
        check_memory_leak
        ;;
    "backup")
        backup_pod_logs
        ;;
    "restart")
        if [ -z "${2:-}" ]; then
            log_error "请指定要重启的Pod名称"
            exit 1
        fi
        restart_pod "$2" "手动重启"
        ;;
    "scale")
        scale_up_pods
        ;;
    "update")
        perform_rolling_update
        ;;
    "rollback")
        rollback_deployment
        ;;
    *)
        echo "使用方式: $0 {monitor|recover|check|backup|restart <pod>|scale|update|rollback}"
        echo ""
        echo "命令说明:"
        echo "  monitor    - 启动监控模式，自动检测和恢复"
        echo "  recover    - 执行完整的故障恢复流程"
        echo "  check      - 检查系统状态"
        echo "  backup     - 备份Pod日志"
        echo "  restart <pod> - 重启指定Pod"
        echo "  scale      - 扩容Pod"
        echo "  update     - 执行滚动更新"
        echo "  rollback   - 回滚到上一版本"
        exit 1
        ;;
esac