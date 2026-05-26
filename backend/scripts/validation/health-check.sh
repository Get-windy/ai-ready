#!/bin/bash
# =============================================================================
# Sprint 27+1 - 服务健康检查脚本
# =============================================================================
# 功能：对测试环境各服务执行健康检查
# 用法：./health-check.sh [service_name|all]
# =============================================================================

set -euo pipefail

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

TARGET="${1:-all}"
FAILED=0

log() { echo -e "${BLUE}[$(date +%H:%M:%S)]${NC} $1"; }
ok() { echo -e "${GREEN}[✓]${NC} $1"; }
fail() { echo -e "${RED}[✗]${NC} $1"; ((FAILED++)) || true; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }

check_postgres() {
    log "检查 PostgreSQL..."
    if docker exec postgres pg_isready -U test_user &>/dev/null; then
        ok "PostgreSQL 服务正常"
        local db_count
        db_count=$(docker exec postgres psql -U test_user -t -c "SELECT count(*) FROM pg_database WHERE datistemplate = false;" 2>/dev/null | xargs)
        ok "数据库数量: ${db_count}"
    else
        fail "PostgreSQL 服务异常"
    fi
}

check_redis() {
    log "检查 Redis..."
    if docker exec redis redis-cli ping | grep -q "PONG"; then
        ok "Redis 服务正常"
        local info
        info=$(docker exec redis redis-cli info stats 2>/dev/null | grep total_connections_received | cut -d: -f2 | tr -d '\r')
        ok "总连接数: ${info:-N/A}"
    else
        fail "Redis 服务异常"
    fi
}

check_rabbitmq() {
    log "检查 RabbitMQ..."
    if curl -s -u guest:guest http://localhost:15672/api/overview | grep -q "rabbitmq_version"; then
        ok "RabbitMQ 管理界面正常"
    else
        fail "RabbitMQ 管理界面异常"
    fi
}

check_app() {
    log "检查应用服务..."
    local response
    response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null || echo "000")
    if [ "$response" = "200" ]; then
        ok "应用服务健康检查通过 (HTTP 200)"
    else
        fail "应用服务健康检查失败 (HTTP ${response})"
    fi
}

check_nginx() {
    log "检查 Nginx..."
    local response
    response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost 2>/dev/null || echo "000")
    if [ "$response" = "200" ] || [ "$response" = "301" ] || [ "$response" = "302" ]; then
        ok "Nginx 服务正常 (HTTP ${response})"
    else
        fail "Nginx 服务异常 (HTTP ${response})"
    fi
}

check_prometheus() {
    log "检查 Prometheus..."
    local response
    response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:9090/-/healthy 2>/dev/null || echo "000")
    if [ "$response" = "200" ]; then
        ok "Prometheus 服务正常"
    else
        fail "Prometheus 服务异常"
    fi
}

check_grafana() {
    log "检查 Grafana..."
    local response
    response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/api/health 2>/dev/null || echo "000")
    if [ "$response" = "200" ]; then
        ok "Grafana 服务正常"
    else
        fail "Grafana 服务异常"
    fi
}

# 主逻辑
log "========================================"
log "Sprint 27+1 服务健康检查"
log "目标: ${TARGET}"
log "========================================"

case "$TARGET" in
    all)
        check_postgres
        check_redis
        check_rabbitmq
        check_app
        check_nginx
        check_prometheus
        check_grafana
        ;;
    postgres) check_postgres ;;
    redis) check_redis ;;
    rabbitmq) check_rabbitmq ;;
    app) check_app ;;
    nginx) check_nginx ;;
    prometheus) check_prometheus ;;
    grafana) check_grafana ;;
    *)
        echo "未知服务: $TARGET"
        echo "可用服务: all, postgres, redis, rabbitmq, app, nginx, prometheus, grafana"
        exit 1
        ;;
esac

log "========================================"
if [ "$FAILED" -eq 0 ]; then
    ok "所有健康检查通过"
    exit 0
else
    fail "存在 ${FAILED} 个检查失败"
    exit 1
fi
