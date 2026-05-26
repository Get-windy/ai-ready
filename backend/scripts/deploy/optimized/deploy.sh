#!/bin/bash
# ============================================================
# 一键部署脚本 - sprint-27-1 优化版
# Sprint 27+1测试环境容器化部署
# 最后更新: 2026-04-27
# ============================================================

set -e  # 遇到错误立即退出

# ---- 配置变量 ----
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="$PROJECT_DIR/infra/docker/docker-compose.optimized.yml"
LOG_DIR="$PROJECT_DIR/logs/deploy"
DEPLOY_TIME=$(date +%Y%m%d_%H%M%S)

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        log_error "$1 未安装，请先安装 $1"
        exit 1
    fi
}

# ---- 依赖检查 ----
log_info "检查依赖..."
check_command docker
check_command docker-compose

# ---- 参数解析 ----
DEPLOY_ENV="${1:-test}"
ACTION="${2:-deploy}"

usage() {
    echo "使用方法: $0 [environment] [action]"
    echo ""
    echo "环境参数: test (默认) | prod"
    echo "动作参数: deploy (默认) | up | down | restart | status | backup | rollback"
    echo ""
    echo "示例:"
    echo "  $0                      # 部署到测试环境"
    echo "  $0 prod                 # 部署到生产环境"
    echo "  $0 test restart         # 重启测试环境"
    echo "  $0 test backup          # 备份测试环境数据"
    echo "  $0 prod rollback 20240427_120000  # 回滚到指定版本"
    exit 1
}

# 参数验证
if [ "$ACTION" == "rollback" ]; then
    if [ -z "$3" ]; then
        log_error "回滚操作需要指定时间戳参数"
        usage
    fi
    BACKUP_TIME="$3"
fi

# ---- 目录准备 ----
mkdir -p "$LOG_DIR"
log_info "日志目录: $LOG_DIR"

# ---- 环境特定配置加载 ----
load_env_config() {
    local env_file="$PROJECT_DIR/config/deploy/${DEPLOY_ENV}.env"
    
    if [ -f "$env_file" ]; then
        log_info "加载环境配置: $env_file"
        export $(grep -v '^#' "$env_file" | xargs)
    else
        log_warn "环境配置文件不存在: $env_file，使用默认配置"
    fi
}

# ---- 备份功能 ----
backup_data() {
    local backup_dir="$PROJECT_DIR/backups/${DEPLOY_ENV}/$DEPLOY_TIME"
    mkdir -p "$backup_dir"
    
    log_info "开始备份数据到: $backup_dir"
    
    #备份PostgreSQL
    if docker-compose -f "$COMPOSE_FILE" ps -q postgres &> /dev/null; then
        log_info "备份PostgreSQL数据..."
        docker-compose -f "$COMPOSE_FILE" exec -T postgres pg_dump -U appuser qizhilian > "$backup_dir/postgres_$DEPLOY_TIME.sql" 2>/dev/null || log_warn "PostgreSQL备份失败"
    fi
    
    #备份Redis
    if docker-compose -f "$COMPOSE_FILE" ps -q redis &> /dev/null; then
        log_info "备份Redis数据..."
        docker-compose -f "$COMPOSE_FILE" exec -T redis redis-cli BGSAVE > /dev/null 2>&1 || log_warn "Redis BGSAVE失败"
        docker-compose -f "$COMPOSE_FILE" cp redis:/data/dump.rdb "$backup_dir/redis_dump.rdb" 2>/dev/null || log_warn "Redis备份失败"
    fi
    
    log_info "备份完成: $backup_dir"
}

# ---- 部署功能 ----
deploy() {
    log_info "开始部署到 $DEPLOY_ENV 环境..."
    
    # 加载环境配置
    load_env_config
    
    # 备份现有数据
    log_info "备份现有数据..."
    backup_data
    
    # 停止旧服务（保留数据）
    log_info "停止旧服务..."
    docker-compose -f "$COMPOSE_FILE" down 2>/dev/null || log_warn "停止服务失败（可能未运行）"
    
    # 拉取最新镜像
    log_info "拉取最新镜像..."
    docker-compose -f "$COMPOSE_FILE" pull || log_warn "镜像拉取失败"
    
    # 启动服务
    log_info "启动新服务..."
    docker-compose -f "$COMPOSE_FILE" up -d || {
        log_error "服务启动失败"
        log_info "自动回滚到备份版本..."
        rollback "$DEPLOY_TIME"
        exit 1
    }
    
    # 等待服务就绪
    log_info "等待服务就绪..."
    sleep 10
    
    # 健康检查
    log_info "执行健康检查..."
    check_health
    
    # 验证部署
    log_info "验证部署..."
    verify_deploy
    
    # 生成部署报告
    generate_deploy_report
    
    log_info "部署完成！"
}

# ---- 健康检查 ----
check_health() {
    local max_attempts=10
    local attempt=0
    
    log_info "检查服务健康状态..."
    
    # 检查后端服务
    while [ $attempt -lt $max_attempts ]; do
        attempt=$((attempt + 1))
        log_info "健康检查尝试 $attempt/$max_attempts..."
        
        if curl -s http://localhost:80/health > /dev/null; then
            log_info "后端服务健康检查通过"
            break
        fi
        
        sleep 5
    done
    
    if [ $attempt -eq $max_attempts ]; then
        log_error "健康检查超时"
        exit 1
    fi
}

# ---- 验证部署 ----
verify_deploy() {
    log_info "验证部署..."
    
    # 检查所有容器状态
    log_info "检查容器状态..."
    docker-compose -f "$COMPOSE_FILE" ps
    
    # 检查关键服务
    log_info "检查关键服务..."
    
    # 检查数据库连接
    if docker-compose -f "$COMPOSE_FILE" exec -T postgres pg_isready -U appuser -d qizhilian > /dev/null 2>&1; then
        log_info "PostgreSQL 连接正常"
    else
        log_error "PostgreSQL 连接失败"
        exit 1
    fi
    
    # 检查Redis连接
    if docker-compose -f "$COMPOSE_FILE" exec -T redis redis-cli ping | grep -q PONG; then
        log_info "Redis 连接正常"
    else
        log_error "Redis 连接失败"
        exit 1
    fi
    
    log_info "所有关键服务验证通过"
}

# ---- 回滚功能 ----
rollback() {
    local backup_time="${1:-$(ls -t "$PROJECT_DIR/backups/$DEPLOY_ENV" | head -1)}"
    
    if [ -z "$backup_time" ]; then
        log_error "未找到可回滚的备份"
        exit 1
    fi
    
    log_info "回滚到备份: $backup_time"
    
    local backup_dir="$PROJECT_DIR/backups/$DEPLOY_ENV/$backup_time"
    
    # 恢复PostgreSQL
    if [ -f "$backup_dir/postgres_$backup_time.sql" ]; then
        log_info "恢复PostgreSQL数据..."
        docker-compose -f "$COMPOSE_FILE" exec -T postgres psql -U appuser -d qizhilian < "$backup_dir/postgres_$backup_time.sql" 2>/dev/null || log_warn "PostgreSQL恢复失败"
    fi
    
    # 恢复Redis
    if [ -f "$backup_dir/redis_dump.rdb" ]; then
        log_info "恢复Redis数据..."
        docker-compose -f "$COMPOSE_FILE" cp "$backup_dir/redis_dump.rdb" redis:/data/dump.rdb
    fi
    
    # 重启服务
    log_info "重启服务..."
    docker-compose -f "$COMPOSE_FILE" restart
    
    log_info "回滚完成"
}

# ---- 部署状态 ----
status() {
    log_info "服务状态..."
    docker-compose -f "$COMPOSE_FILE" ps
    
    log_info "服务日志（最新100行）..."
    docker-compose -f "$COMPOSE_FILE" logs --tail=100
    
    log_info "系统资源使用情况..."
    docker system df
    
    # 检查磁盘空间
    log_info "磁盘空间..."
    df -h
}

# ---- 生成部署报告 ----
generate_deploy_report() {
    local report_file="$LOG_DIR/deploy_$DEPLOY_TIME.md"
    
    cat > "$report_file" << EOF
# 部署报告

## 基本信息
- 环境: $DEPLOY_ENV
- 时间: $(date '+%Y-%m-%d %H:%M:%S')
- 版本: 1.0.0
- 操作: $ACTION

## 部署服务
EOF
    
    docker-compose -f "$COMPOSE_FILE" ps >> "$report_file" 2>/dev/null || true
    
    cat >> "$report_file" << EOF

## 服务状态
EOF
    
    for service in backend postgres redis rabbitmq; do
        local status=$(docker-compose -f "$COMPOSE_FILE" ps -q $service 2>/dev/null && echo "running" || echo "stopped")
        echo "- $service: $status" >> "$report_file"
    done
    
    log_info "部署报告生成: $report_file"
}

# ---- 主程序 ----
main() {
    log_info "========== Docker Compose 一键部署脚本 =========="
    log_info "环境: $DEPLOY_ENV"
    log_info "动作: $ACTION"
    log_info "================================================"
    
    case $ACTION in
        deploy)
            deploy
            ;;
        up)
            docker-compose -f "$COMPOSE_FILE" up -d
            log_info "服务已启动"
            ;;
        down)
            docker-compose -f "$COMPOSE_FILE" down
            log_info "服务已停止"
            ;;
        restart)
            docker-compose -f "$COMPOSE_FILE" restart
            log_info "服务已重启"
            ;;
        status)
            status
            ;;
        backup)
            backup_data
            ;;
        rollback)
            rollback "$BACKUP_TIME"
            ;;
        *)
            usage
            ;;
    esac
    
    log_info "================================================"
    log_info "操作完成!"
}

# 执行主程序
main