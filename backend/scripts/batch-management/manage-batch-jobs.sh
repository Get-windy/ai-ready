#!/bin/bash

# ERP批次作业管理脚本
# 功能: 管理批次作业的启停、状态监控、日志查看
# 版本: 1.0.0

set -e

# 配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_DIR="$SCRIPT_DIR/config"
LOG_DIR="/var/log/batch-management"
PID_FILE="/var/run/batch-management.pid"
API_URL="http://localhost:8080/api/batch"

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

# 日志函数
log() {
    echo -e "${BLUE}[BATCH]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
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

# 显示使用说明
show_usage() {
    echo "ERP批次作业管理脚本"
    echo ""
    echo "使用方法:"
    echo "  $0 <命令> [参数]"
    echo ""
    echo "命令列表:"
    echo "  start          启动批次管理服务"
    echo "  stop           停止批次管理服务"
    echo "  restart        重启批次管理服务"
    echo "  status         查看服务状态"
    echo "  list-jobs      列出所有批次作业"
    echo "  start-job      启动特定作业"
    echo "  stop-job       停止特定作业"
    echo "  logs           查看服务日志"
    echo "  health         健康检查"
    echo "  metrics        查看性能指标"
    echo ""
    echo "示例:"
    echo "  $0 start"
    echo "  $0 list-jobs"
    echo "  $0 start-job --job-id=invoice-batch"
    echo ""
}

# 检查服务是否运行
check_service_running() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            return 0
        fi
    fi
    return 1
}

# 启动服务
start_service() {
    log "启动批次管理服务..."
    
    if check_service_running; then
        warning "服务已经在运行中 (PID: $PID)"
        return 0
    fi
    
    # 检查Docker容器
    if docker ps --format '{{.Names}}' | grep -q "batch-management"; then
        log "检测到Docker容器正在运行"
        success "服务已在Docker容器中运行"
        return 0
    fi
    
    # 如果没有Docker容器，尝试启动jar
    JAR_FILE=$(find ../erp/batch-management-service/target -name "*.jar" ! -name "*sources.jar" ! -name "*javadoc.jar" | head -n 1)
    
    if [ -z "$JAR_FILE" ]; then
        error "未找到可执行的jar文件，请先编译项目"
    fi
    
    # 启动Java应用
    nohup java -jar "$JAR_FILE" \
        --spring.profiles.active=prod \
        --server.port=8080 \
        --logging.file.path="$LOG_DIR" \
        > "$LOG_DIR/application.log" 2>&1 &
    
    APP_PID=$!
    echo "$APP_PID" > "$PID_FILE"
    
    sleep 2
    
    if check_service_running; then
        success "服务启动成功 (PID: $APP_PID)"
    else
        error "服务启动失败，请检查日志: $LOG_DIR/application.log"
    fi
}

# 停止服务
stop_service() {
    log "停止批次管理服务..."
    
    if ! check_service_running; then
        warning "服务未在运行"
        return 0
    fi
    
    # 优雅停止
    kill -15 "$PID" 2>/dev/null || true
    
    # 等待最多10秒
    for i in {1..10}; do
        if ! check_service_running; then
            rm -f "$PID_FILE"
            success "服务已停止"
            return 0
        fi
        sleep 1
    done
    
    # 强制停止
    log "优雅停止失败，尝试强制停止..."
    kill -9 "$PID" 2>/dev/null || true
    rm -f "$PID_FILE"
    
    success "服务已强制停止"
}

# 重启服务
restart_service() {
    stop_service
    sleep 2
    start_service
}

# 检查服务状态
service_status() {
    log "检查服务状态..."
    
    if check_service_running; then
        echo "服务状态: ${GREEN}运行中${NC}"
        echo "进程ID: $PID"
        echo "启动时间: $(ps -p "$PID" -o lstart=)"
        echo "运行时间: $(ps -p "$PID" -o etime=)"
        
        # 检查API健康
        if curl -s -f "$API_URL/health" > /dev/null 2>&1; then
            echo "API健康: ${GREEN}正常${NC}"
        else
            echo "API健康: ${RED}异常${NC}"
        fi
        
        # 检查内存使用
        MEMORY_USAGE=$(ps -p "$PID" -o rss=)
        echo "内存使用: $((MEMORY_USAGE / 1024)) MB"
        
    else
        echo "服务状态: ${RED}未运行${NC}"
    fi
}

# 列出所有作业
list_jobs() {
    log "获取批次作业列表..."
    
    if ! check_service_running; then
        error "服务未运行，无法获取作业列表"
    fi
    
    RESPONSE=$(curl -s -f "$API_URL/jobs" || echo "[]")
    
    if [ "$RESPONSE" = "[]" ]; then
        echo "暂无批次作业"
        return 0
    fi
    
    # 解析JSON响应
    echo "批次作业列表:"
    echo ""
    echo "ID | 名称 | 状态 | 下次执行时间 | 上次执行结果"
    echo "---|------|------|------------|------------"
    
    echo "$RESPONSE" | jq -r '.[] | "\(.id) | \(.name) | \(.status) | \(.nextExecutionTime) | \(.lastExecutionResult)"' 2>/dev/null || \
        echo "$RESPONSE"
}

# 启动特定作业
start_job() {
    local job_id="$1"
    
    if [ -z "$job_id" ]; then
        error "请提供作业ID (使用 --job-id=<id>)"
    fi
    
    log "启动批次作业: $job_id"
    
    if ! check_service_running; then
        error "服务未运行，无法启动作业"
    fi
    
    RESPONSE=$(curl -s -X POST "$API_URL/jobs/$job_id/start")
    
    if echo "$RESPONSE" | grep -q "success\|started"; then
        success "作业 $job_id 启动成功"
    else
        error "作业启动失败: $RESPONSE"
    fi
}

# 停止特定作业
stop_job() {
    local job_id="$1"
    
    if [ -z "$job_id" ]; then
        error "请提供作业ID (使用 --job-id=<id>)"
    fi
    
    log "停止批次作业: $job_id"
    
    if ! check_service_running; then
        error "服务未运行，无法停止作业"
    fi
    
    RESPONSE=$(curl -s -X POST "$API_URL/jobs/$job_id/stop")
    
    if echo "$RESPONSE" | grep -q "success\|stopped"; then
        success "作业 $job_id 停止成功"
    else
        error "作业停止失败: $RESPONSE"
    fi
}

# 查看日志
view_logs() {
    local lines="${1:-50}"
    local follow="${2:-false}"
    
    log "查看服务日志..."
    
    if [ ! -f "$LOG_DIR/application.log" ]; then
        error "日志文件不存在: $LOG_DIR/application.log"
    fi
    
    if [ "$follow" = "true" ]; then
        tail -f -n "$lines" "$LOG_DIR/application.log"
    else
        tail -n "$lines" "$LOG_DIR/application.log"
    fi
}

# 健康检查
health_check() {
    log "执行健康检查..."
    
    if ! check_service_running; then
        error "服务未运行"
    fi
    
    # 检查应用健康端点
    HEALTH_RESPONSE=$(curl -s "$API_URL/actuator/health" || echo "{}")
    
    echo "应用健康状态:"
    echo "$HEALTH_RESPONSE" | jq '.' 2>/dev/null || echo "$HEALTH_RESPONSE"
    
    # 检查数据库连接
    DB_RESPONSE=$(curl -s "$API_URL/actuator/health/db" || echo "{}")
    
    echo ""
    echo "数据库健康状态:"
    echo "$DB_RESPONSE" | jq '.' 2>/dev/null || echo "$DB_RESPONSE"
    
    # 检查磁盘空间
    DISK_USAGE=$(df -h / | tail -1 | awk '{print $5}')
    echo ""
    echo "磁盘使用率: $DISK_USAGE"
}

# 查看性能指标
view_metrics() {
    log "获取性能指标..."
    
    if ! check_service_running; then
        error "服务未运行"
    fi
    
    # 获取Prometheus指标
    METRICS_RESPONSE=$(curl -s "$API_URL/actuator/prometheus" || echo "# 无法获取指标")
    
    # 提取关键指标
    echo "关键性能指标:"
    echo ""
    
    # JVM内存
    echo "$METRICS_RESPONSE" | grep -E "jvm_memory_used_bytes|jvm_memory_max_bytes" | head -4
    
    # 线程数
    echo "$METRICS_RESPONSE" | grep -E "jvm_threads_live_threads|jvm_threads_daemon_threads"
    
    # GC信息
    echo "$METRICS_RESPONSE" | grep -E "jvm_gc_.*_seconds_count" | head -2
    
    # HTTP请求
    echo "$METRICS_RESPONSE" | grep -E "http_server_requests_seconds_count|http_server_requests_seconds_sum"
    
    # 批次作业统计
    echo "$METRICS_RESPONSE" | grep -E "batch_job_.*_total|batch_job_.*_seconds"
}

# 主函数
main() {
    local command="$1"
    local arg1="$2"
    local arg2="$3"
    
    # 创建必要目录
    mkdir -p "$LOG_DIR"
    mkdir -p "$CONFIG_DIR"
    
    case "$command" in
        start)
            start_service
            ;;
        stop)
            stop_service
            ;;
        restart)
            restart_service
            ;;
        status)
            service_status
            ;;
        list-jobs)
            list_jobs
            ;;
        start-job)
            if [[ "$arg1" == --job-id=* ]]; then
                start_job "${arg1#*=}"
            else
                error "请使用 --job-id=<id> 参数"
            fi
            ;;
        stop-job)
            if [[ "$arg1" == --job-id=* ]]; then
                stop_job "${arg1#*=}"
            else
                error "请使用 --job-id=<id> 参数"
            fi
            ;;
        logs)
            if [ "$arg1" = "-f" ]; then
                view_logs "${arg2:-50}" "true"
            else
                view_logs "${arg1:-50}" "false"
            fi
            ;;
        health)
            health_check
            ;;
        metrics)
            view_metrics
            ;;
        help|--help|-h)
            show_usage
            ;;
        *)
            if [ -z "$command" ]; then
                show_usage
            else
                error "未知命令: $command"
            fi
            ;;
    esac
}

# 执行主函数
main "$@"