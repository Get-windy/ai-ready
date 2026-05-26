#!/bin/bash
# AI-Ready 服务管理脚本
# 版本: v2.0
# 功能: 统一的服务启停/状态检查/日志管理

set -e

# ============ 配置参数 ============
APP_NAME="ai-ready"
APP_HOME="${APP_HOME:-/opt/ai-ready}"
LOG_DIR="${LOG_DIR:-/var/log/ai-ready}"
PID_DIR="${PID_DIR:-/var/run/ai-ready}"

# 服务依赖顺序
SERVICES=("postgresql" "redis" "nacos" "ai-ready-api")

# 超时配置
START_TIMEOUT=120
STOP_TIMEOUT=60
HEALTH_CHECK_INTERVAL=5

# ============ 颜色输出 ============
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# ============ 服务状态检查 ============
check_port() {
    local port=$1
    local host=${2:-localhost}
    nc -z "$host" "$port" 2>/dev/null
}

wait_for_port() {
    local port=$1
    local timeout=$2
    local count=0
    while ! check_port "$port" && [ $count -lt $timeout ]; do
        sleep 1
        ((count++))
    done
    [ $count -lt $timeout ]
}

check_service_health() {
    local service=$1
    case $service in
        postgresql) check_port 5432 ;;
        redis) check_port 6379 ;;
        nacos) check_port 8848 ;;
        ai-ready-api) 
            curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1
            ;;
    esac
}

# ============ 服务启停函数 ============
start_postgresql() {
    log_info "启动 PostgreSQL..."
    systemctl start postgresql || {
        log_error "PostgreSQL 启动失败"
        return 1
    }
    wait_for_port 5432 30 && log_info "PostgreSQL 启动成功" || {
        log_error "PostgreSQL 启动超时"
        return 1
    }
}

start_redis() {
    log_info "启动 Redis..."
    systemctl start redis || {
        log_error "Redis 启动失败"
        return 1
    }
    wait_for_port 6379 15 && log_info "Redis 启动成功" || {
        log_error "Redis 启动超时"
        return 1
    }
}

start_nacos() {
    log_info "启动 Nacos..."
    systemctl start nacos || docker start nacos 2>/dev/null || {
        log_warn "Nacos 启动失败，尝试跳过..."
        return 0
    }
    wait_for_port 8848 30 && log_info "Nacos 启动成功" || log_warn "Nacos 启动超时"
}

start_api() {
    log_info "启动 AI-Ready API..."
    
    # 检查依赖
    if ! check_service_health postgresql; then
        log_error "PostgreSQL 未就绪，无法启动 API"
        return 1
    fi
    if ! check_service_health redis; then
        log_error "Redis 未就绪，无法启动 API"
        return 1
    fi
    
    # 启动API服务
    if [ -f "${APP_HOME}/bin/startup.sh" ]; then
        ${APP_HOME}/bin/startup.sh
    elif command -v systemctl &> /dev/null; then
        systemctl start ai-ready-api
    else
        cd ${APP_HOME} && nohup java -jar ai-ready-api.jar > ${LOG_DIR}/api.log 2>&1 &
        echo $! > ${PID_DIR}/api.pid
    fi
    
    # 等待健康检查通过
    local count=0
    while ! check_service_health ai-ready-api && [ $count -lt $START_TIMEOUT ]; do
        sleep $HEALTH_CHECK_INTERVAL
        ((count+=$HEALTH_CHECK_INTERVAL))
        log_info "等待 API 启动... (${count}s)"
    done
    
    if check_service_health ai-ready-api; then
        log_info "AI-Ready API 启动成功"
        return 0
    else
        log_error "AI-Ready API 启动超时"
        return 1
    fi
}

# ============ 停止服务 ============
stop_api() {
    log_info "停止 AI-Ready API..."
    
    if [ -f "${PID_DIR}/api.pid" ]; then
        local pid=$(cat ${PID_DIR}/api.pid)
        if kill -0 $pid 2>/dev/null; then
            # 优雅关闭
            kill -TERM $pid
            local count=0
            while kill -0 $pid 2>/dev/null && [ $count -lt $STOP_TIMEOUT ]; do
                sleep 1
                ((count++))
            done
            # 强制关闭
            if kill -0 $pid 2>/dev/null; then
                log_warn "强制关闭 API..."
                kill -9 $pid
            fi
        fi
        rm -f ${PID_DIR}/api.pid
    elif command -v systemctl &> /dev/null; then
        systemctl stop ai-ready-api
    fi
    
    log_info "AI-Ready API 已停止"
}

stop_nacos() {
    log_info "停止 Nacos..."
    systemctl stop nacos 2>/dev/null || docker stop nacos 2>/dev/null || true
    log_info "Nacos 已停止"
}

stop_redis() {
    log_info "停止 Redis..."
    systemctl stop redis 2>/dev/null || true
    log_info "Redis 已停止"
}

stop_postgresql() {
    log_info "停止 PostgreSQL..."
    systemctl stop postgresql 2>/dev/null || true
    log_info "PostgreSQL 已停止"
}

# ============ 状态检查 ============
status_all() {
    echo "=========================================="
    echo "        AI-Ready 服务状态"
    echo "=========================================="
    
    for service in "${SERVICES[@]}"; do
        if check_service_health "$service"; then
            echo -e "$service: ${GREEN}运行中${NC}"
        else
            echo -e "$service: ${RED}已停止${NC}"
        fi
    done
    
    echo "=========================================="
}

# ============ 主流程 ============
main() {
    local action=${1:-help}
    
    case "$action" in
        start)
            log_info "========== 启动 AI-Ready 服务 =========="
            mkdir -p ${LOG_DIR} ${PID_DIR}
            
            for service in "${SERVICES[@]}"; do
                if ! check_service_health "$service"; then
                    start_$service || {
                        log_error "启动 $service 失败，终止操作"
                        exit 1
                    }
                else
                    log_info "$service 已在运行"
                fi
            done
            
            log_info "========== 所有服务启动完成 =========="
            ;;
        stop)
            log_info "========== 停止 AI-Ready 服务 =========="
            
            # 反序停止
            for ((i=${#SERVICES[@]}-1; i>=0; i--)); do
                stop_${SERVICES[$i]}
            done
            
            log_info "========== 所有服务已停止 =========="
            ;;
        restart)
            $0 stop
            sleep 3
            $0 start
            ;;
        status)
            status_all
            ;;
        *)
            echo "用法: $0 {start|stop|restart|status}"
            exit 1
            ;;
    esac
}

main "$@"