#!/bin/bash
# AI-Ready 自动化部署脚本
# 版本: v1.0.0
# 支持环境: dev, staging, prod

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_DIR="$SCRIPT_DIR/../config"
BACKUP_DIR="$SCRIPT_DIR/../backups"
LOG_DIR="$SCRIPT_DIR/../logs"

ENV="dev"
VERSION="latest"
SKIP_HEALTH_CHECK=false
FORCE_DEPLOY=false
ROLLBACK_ON_FAILURE=true

mkdir -p "$BACKUP_DIR" "$LOG_DIR"

log() {
    local level=$1
    shift
    echo "$(date '+%Y-%m-%d %H:%M:%S') [$level] $*" | tee -a "$LOG_DIR/deploy-$(date +%Y%m%d).log"
}

info() { log "INFO" "$@"; }
warn() { log "WARN" "$@"; }
error() { log "ERROR" "$@"; }
success() { log "SUCCESS" "$@"; }

show_help() {
    echo "AI-Ready 自动化部署脚本"
    echo "用法: $0 [选项]"
    echo "  -e, --env ENV         部署环境 (dev|staging|prod)"
    echo "  -v, --version VER     部署版本号"
    echo "  -s, --skip-health     跳过健康检查"
    echo "  -f, --force           强制部署"
    echo "  --no-rollback         禁用自动回滚"
    echo "  -h, --help            显示帮助"
}

parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -e|--env) ENV="$2"; shift 2 ;;
            -v|--version) VERSION="$2"; shift 2 ;;
            -s|--skip-health) SKIP_HEALTH_CHECK=true; shift ;;
            -f|--force) FORCE_DEPLOY=true; shift ;;
            --no-rollback) ROLLBACK_ON_FAILURE=false; shift ;;
            -h|--help) show_help; exit 0 ;;
            *) error "未知参数: $1"; exit 1 ;;
        esac
    done
}

load_env_config() {
    local env_file="$CONFIG_DIR/$ENV.env"
    if [[ ! -f "$env_file" ]]; then
        error "环境配置文件不存在: $env_file"
        exit 1
    fi
    source "$env_file"
    info "加载环境配置: $ENV"
}

pre_deploy_check() {
    info "执行部署前检查..."
    for cmd in ssh scp docker git; do
        if ! command -v "$cmd" &> /dev/null; then
            error "缺少必要命令: $cmd"
            exit 1
        fi
    done
    success "部署前检查通过"
}

backup_current() {
    info "备份当前版本..."
    local backup_id="$(date +%Y%m%d_%H%M%S)"
    ssh "$DEPLOY_USER@$DEPLOY_HOST" "
        if [[ -d '$REMOTE_APP_DIR' ]]; then
            mkdir -p '$REMOTE_BACKUP_DIR'
            tar -czf '$REMOTE_BACKUP_DIR/$backup_id.tar.gz' -C '$REMOTE_APP_DIR' .
            echo '$backup_id'
        fi
    " 2>/dev/null || warn "备份失败或无现有版本"
}

deploy_app() {
    info "开始部署到 $ENV 环境..."
    ssh "$DEPLOY_USER@$DEPLOY_HOST" "mkdir -p '$REMOTE_APP_DIR' '$REMOTE_LOG_DIR'"
    
    if [[ -f "$PROJECT_ROOT/docker-compose.full.yml" ]]; then
        scp "$PROJECT_ROOT/docker-compose.full.yml" "$DEPLOY_USER@$DEPLOY_HOST:$REMOTE_APP_DIR/docker-compose.yml"
    fi
    
    ssh "$DEPLOY_USER@$DEPLOY_HOST" "
        cd '$REMOTE_APP_DIR'
        export VERSION='$VERSION' SPRING_PROFILES_ACTIVE='$ENV'
        [[ -f docker-compose.yml ]] && docker-compose down --remove-orphans 2>/dev/null || true
        docker-compose up -d
        docker image prune -f
    "
    success "应用部署完成"
}

health_check() {
    [[ "$SKIP_HEALTH_CHECK" == true ]] && return 0
    info "执行健康检查..."
    for i in {1..30}; do
        local status=$(ssh "$DEPLOY_USER@$DEPLOY_HOST" "curl -s -o /dev/null -w '%{http_code}' http://localhost:$APP_PORT/actuator/health 2>/dev/null || echo '000'")
        [[ "$status" == "200" ]] && { success "健康检查通过"; return 0; }
        info "等待服务就绪... ($i/30)"
        sleep 10
    done
    error "健康检查失败"
    return 1
}

main() {
    parse_args "$@"
    load_env_config
    pre_deploy_check
    backup_current
    deploy_app
    health_check || {
        [[ "$ROLLBACK_ON_FAILURE" == true ]] && { warn "部署失败，执行回滚"; ./rollback.sh -e "$ENV" -f; }
        exit 1
    }
    success "部署成功完成"
}

main "$@"
