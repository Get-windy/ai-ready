#!/bin/bash
# AI-Ready 部署回滚脚本
# 版本: v1.0.0

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_DIR="$SCRIPT_DIR/../config"
BACKUP_DIR="$SCRIPT_DIR/../backups"
LOG_DIR="$SCRIPT_DIR/../logs"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() {
    local level=$1
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo -e "${timestamp} [$level] $message" | tee -a "$LOG_DIR/rollback-$(date +%Y%m%d).log"
}

info() { log "INFO" "$@"; }
warn() { log "WARN" "${YELLOW}$@${NC}"; }
error() { log "ERROR" "${RED}$@${NC}"; }
success() { log "SUCCESS" "${GREEN}$@${NC}"; }

# 显示帮助
show_help() {
    cat << EOF
AI-Ready 部署回滚脚本

用法: $(basename $0) [选项] [备份ID]

选项:
    -e, --env ENV           部署环境 (dev|staging|prod) [默认: dev]
    -l, --list              列出可用备份
    -f, --force             强制回滚，不提示确认
    -h, --help              显示帮助信息

示例:
    $(basename $0) -e prod                    # 回滚到最新备份
    $(basename $0) -e prod 20260409_120000    # 回滚到指定备份
    $(basename $0) -e prod -l                 # 列出所有备份

EOF
}

# 解析参数
ENV="dev"
LIST_BACKUPS=false
FORCE_ROLLBACK=false
BACKUP_ID=""

parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -e|--env)
                ENV="$2"
                shift 2
                ;;
            -l|--list)
                LIST_BACKUPS=true
                shift
                ;;
            -f|--force)
                FORCE_ROLLBACK=true
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            -*)
                error "未知参数: $1"
                show_help
                exit 1
                ;;
            *)
                BACKUP_ID="$1"
                shift
                ;;
        esac
    done
}

# 加载环境配置
load_env_config() {
    local env_file="$CONFIG_DIR/$ENV.env"
    
    if [[ ! -f "$env_file" ]]; then
        error "环境配置文件不存在: $env_file"
        exit 1
    fi
    
    source "$env_file"
    info "加载环境配置: $ENV"
}

# 列出可用备份
list_backups() {
    info "可用备份列表:"
    
    if [[ ! -d "$BACKUP_DIR" ]] || [[ -z "$(ls -A "$BACKUP_DIR" 2>/dev/null)" ]]; then
        warn "没有可用的备份"
        return 1
    fi
    
    printf "%-20s %-15s %-30s\n" "备份ID" "大小" "创建时间"
    printf "%-20s %-15s %-30s\n" "--------------------" "---------------" "------------------------------"
    
    for backup in "$BACKUP_DIR"/*/; do
        if [[ -d "$backup" ]]; then
            local backup_name=$(basename "$backup")
            local backup_size=$(du -sh "$backup" 2>/dev/null | cut -f1)
            local create_time=$(stat -c %y "$backup" 2>/dev/null | cut -d'.' -f1)
            printf "%-20s %-15s %-30s\n" "$backup_name" "$backup_size" "$create_time"
        fi
    done
}

# 获取最新备份
get_latest_backup() {
    if [[ ! -d "$BACKUP_DIR" ]]; then
        return 1
    fi
    
    ls -1 "$BACKUP_DIR" | sort -r | head -1
}

# 确认回滚
confirm_rollback() {
    if [[ "$FORCE_ROLLBACK" == true ]]; then
        return 0
    fi
    
    warn "即将回滚到备份: $BACKUP_ID"
    warn "环境: $ENV"
    warn "此操作将替换当前运行的版本！"
    
    read -p "确认回滚? (yes/no): " confirm
    
    if [[ "$confirm" != "yes" ]]; then
        info "回滚已取消"
        exit 0
    fi
}

# 执行回滚
execute_rollback() {
    info "开始回滚到备份: $BACKUP_ID"
    
    # 检查备份是否存在
    if [[ ! -d "$BACKUP_DIR/$BACKUP_ID" ]]; then
        error "备份不存在: $BACKUP_ID"
        exit 1
    fi
    
    # 检查远程备份
    local remote_backup_exists=$(ssh "$DEPLOY_USER@$DEPLOY_HOST" "
        if [[ -f '$REMOTE_BACKUP_DIR/$BACKUP_ID.tar.gz' ]]; then
            echo 'true'
        else
            echo 'false'
        fi
    ")
    
    if [[ "$remote_backup_exists" != "true" ]]; then
        error "远程备份不存在: $BACKUP_ID"
        exit 1
    fi
    
    # 执行回滚
    ssh "$DEPLOY_USER@$DEPLOY_HOST" "
        set -e
        
        cd '$REMOTE_APP_DIR'
        
        info() { echo "[INFO] \$*"; }
        
        info '停止当前服务...'
        docker-compose down --remove-orphans 2>/dev/null || true
        
        info '清理当前版本...'
        rm -rf ./*
        
        info '恢复备份...'
        tar -xzf '$REMOTE_BACKUP_DIR/$BACKUP_ID.tar.gz' -C .
        
        info '启动服务...'
        docker-compose up -d
        
        info '回滚完成'
    "
    
    success "回滚执行成功"
    
    # 健康检查
    info "执行健康检查..."
    sleep 10
    
    local health_status=$(ssh "$DEPLOY_USER@$DEPLOY_HOST" "
        curl -s -o /dev/null -w '%{http_code}' \
            http://localhost:$APP_PORT/actuator/health 2>/dev/null || echo '000'
    ")
    
    if [[ "$health_status" == "200" ]]; then
        success "健康检查通过，回滚成功"
    else
        warn "健康检查返回状态: $health_status"
        warn "请手动检查服务状态"
    fi
}

# 主函数
main() {
    parse_args "$@"
    
    mkdir -p "$LOG_DIR"
    
    if [[ "$LIST_BACKUPS" == true ]]; then
        load_env_config
        list_backups
        exit 0
    fi
    
    # 如果没有指定备份ID，使用最新的
    if [[ -z "$BACKUP_ID" ]]; then
        BACKUP_ID=$(get_latest_backup)
        if [[ -z "$BACKUP_ID" ]]; then
            error "没有可用的备份"
            exit 1
        fi
        info "使用最新备份: $BACKUP_ID"
    fi
    
    load_env_config
    confirm_rollback
    execute_rollback
}

main "$@"
