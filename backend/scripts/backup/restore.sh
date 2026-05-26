#!/bin/bash
# AI-Ready 数据恢复脚本
# Version: v2.0.0
# 支持PostgreSQL/Redis/文件存储恢复

set -e

# ============================================
# 全局配置
# ============================================
BACKUP_ROOT="${BACKUP_ROOT:-/backup/ai-ready}"
LOG_FILE="${BACKUP_ROOT}/logs/restore-$(date +%Y%m%d_%H%M%S).log"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# ============================================
# 日志函数
# ============================================
log() {
    local level="$1"
    local message="$2"
    echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] [${level}] ${message}" | tee -a "$LOG_FILE"
}

log_info() { log "${GREEN}INFO${NC}" "$1"; }
log_warn() { log "${YELLOW}WARN${NC}" "$1"; }
log_error() { log "${RED}ERROR${NC}" "$1"; }

# ============================================
# 列出可用备份
# ============================================
list_backups() {
    echo "========== 可用备份列表 =========="
    echo ""
    
    echo "PostgreSQL备份:"
    echo "----------------------------------------"
    for dir in $(ls -1t "${BACKUP_ROOT}/database" 2>/dev/null); do
        echo "日期: $dir"
        ls -lh "${BACKUP_ROOT}/database/$dir"/*.dump 2>/dev/null | awk '{print "  - "$9" ("$5")"}'
    done
    echo ""
    
    echo "Redis备份:"
    echo "----------------------------------------"
    for dir in $(ls -1t "${BACKUP_ROOT}/redis" 2>/dev/null); do
        echo "日期: $dir"
        ls -lh "${BACKUP_ROOT}/redis/$dir"/*.rdb.gz 2>/dev/null | awk '{print "  - "$9" ("$5")"}'
    done
    echo ""
    
    echo "文件备份:"
    echo "----------------------------------------"
    for dir in $(ls -1t "${BACKUP_ROOT}/files" 2>/dev/null); do
        echo "日期: $dir"
        ls -lh "${BACKUP_ROOT}/files/$dir"/*.tar.gz 2>/dev/null | awk '{print "  - "$9" ("$5")"}'
    done
}

# ============================================
# PostgreSQL 恢复
# ============================================
restore_postgresql() {
    local backup_file="$1"
    local target_db="${2:-ai_ready_prod}"
    
    log_info "开始PostgreSQL恢复..."
    log_info "备份文件: $backup_file"
    log_info "目标数据库: $target_db"
    
    # 验证备份文件
    if [[ ! -f "$backup_file" ]]; then
        log_error "备份文件不存在: $backup_file"
        return 1
    fi
    
    # 验证校验和
    if [[ -f "${backup_file}.sha256" ]]; then
        log_info "验证备份文件完整性..."
        sha256sum -c "${backup_file}.sha256" || {
            log_error "校验和验证失败！"
            return 1
        }
        log_info "校验和验证通过"
    fi
    
    # 连接数据库
    local db_host="${DB_HOST:-localhost}"
    local db_port="${DB_PORT:-5432}"
    local db_user="${DB_USER:-postgres}"
    
    log_info "测试数据库连接..."
    PGPASSWORD="${DB_PASSWORD}" psql -h "$db_host" -p "$db_port" -U "$db_user" -d postgres -c "SELECT 1" || {
        log_error "数据库连接失败"
        return 1
    }
    
    # 检查目标数据库是否存在
    local db_exists=$(PGPASSWORD="${DB_PASSWORD}" psql -h "$db_host" -p "$db_port" -U "$db_user" -d postgres -tAc \
        "SELECT 1 FROM pg_database WHERE datname='$target_db'")
    
    if [[ "$db_exists" == "1" ]]; then
        # 备份当前数据库
        local pre_restore_backup="${BACKUP_ROOT}/database/pre-restore/$(date +%Y%m%d_%H%M%S)"
        mkdir -p "$pre_restore_backup"
        
        log_warn "目标数据库已存在，创建恢复前备份..."
        PGPASSWORD="${DB_PASSWORD}" pg_dump -h "$db_host" -p "$db_port" -U "$db_user" -d "$target_db" -Fc \
            -f "${pre_restore_backup}/${target_db}_pre_restore.dump"
        
        # 断开所有连接
        log_info "断开现有数据库连接..."
        PGPASSWORD="${DB_PASSWORD}" psql -h "$db_host" -p "$db_port" -U "$db_user" -d postgres -c \
            "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='$target_db' AND pid <> pg_backend_pid()"
        
        # 删除数据库
        log_info "删除现有数据库..."
        PGPASSWORD="${DB_PASSWORD}" psql -h "$db_host" -p "$db_port" -U "$db_user" -d postgres -c \
            "DROP DATABASE IF EXISTS $target_db"
    fi
    
    # 创建数据库
    log_info "创建数据库: $target_db"
    PGPASSWORD="${DB_PASSWORD}" psql -h "$db_host" -p "$db_port" -U "$db_user" -d postgres -c \
        "CREATE DATABASE $target_db"
    
    # 恢复数据
    log_info "开始恢复数据..."
    PGPASSWORD="${DB_PASSWORD}" pg_restore -h "$db_host" -p "$db_port" -U "$db_user" -d "$target_db" \
        --no-owner --no-acl --verbose "$backup_file" 2>&1 | tee -a "$LOG_FILE"
    
    # 验证恢复
    local table_count=$(PGPASSWORD="${DB_PASSWORD}" psql -h "$db_host" -p "$db_port" -U "$db_user" -d "$target_db" -tAc \
        "SELECT count(*) FROM information_schema.tables WHERE table_schema='public'")
    
    log_info "恢复完成，共恢复 $table_count 张表"
    
    return 0
}

# ============================================
# Redis 恢复
# ============================================
restore_redis() {
    local backup_file="$1"
    
    log_info "开始Redis恢复..."
    log_info "备份文件: $backup_file"
    
    local redis_host="${REDIS_HOST:-localhost}"
    local redis_port="${REDIS_PORT:-6379}"
    
    # 解压备份文件
    local temp_rdb="/tmp/redis_restore_$(date +%s).rdb"
    
    if [[ "$backup_file" == *.gz ]]; then
        log_info "解压备份文件..."
        gunzip -c "$backup_file" > "$temp_rdb"
    else
        cp "$backup_file" "$temp_rdb"
    fi
    
    # 停止Redis
    log_warn "停止Redis服务..."
    redis-cli -h "$redis_host" -p "$redis_port" -a "${REDIS_PASSWORD}" SHUTDOWN NOSAVE 2>/dev/null || true
    sleep 5
    
    # 替换RDB文件
    log_info "替换RDB文件..."
    cp "$temp_rdb" /var/lib/redis/dump.rdb
    chown redis:redis /var/lib/redis/dump.rdb
    
    # 启动Redis
    log_info "启动Redis服务..."
    systemctl start redis || docker start redis || true
    sleep 10
    
    # 验证恢复
    local key_count=$(redis-cli -h "$redis_host" -p "$redis_port" -a "${REDIS_PASSWORD}" DBSIZE 2>/dev/null | awk '{print $2}')
    
    log_info "Redis恢复完成，共 $key_count 个键"
    
    # 清理临时文件
    rm -f "$temp_rdb"
    
    return 0
}

# ============================================
# 文件存储恢复
# ============================================
restore_files() {
    local backup_file="$1"
    local target_dir="${2:-/var/lib/ai-ready}"
    
    log_info "开始文件存储恢复..."
    log_info "备份文件: $backup_file"
    log_info "目标目录: $target_dir"
    
    if [[ ! -f "$backup_file" ]]; then
        log_error "备份文件不存在: $backup_file"
        return 1
    fi
    
    # 创建目标目录
    mkdir -p "$target_dir"
    
    # 备份现有文件
    if [[ -d "$target_dir" && "$(ls -A $target_dir 2>/dev/null)" ]]; then
        local pre_restore="${BACKUP_ROOT}/files/pre-restore/$(date +%Y%m%d_%H%M%S)"
        mkdir -p "$pre_restore"
        
        log_warn "备份现有文件到 $pre_restore ..."
        tar -czf "${pre_restore}/existing_files.tar.gz" -C "$target_dir" .
    fi
    
    # 恢复文件
    log_info "解压备份文件..."
    tar -xzf "$backup_file" -C "$(dirname "$target_dir")"
    
    # 设置权限
    chown -R ai-ready:ai-ready "$target_dir"
    
    log_info "文件恢复完成"
    
    return 0
}

# ============================================
# 完整恢复
# ============================================
full_restore() {
    local backup_date="$1"
    
    log_info "========== 开始完整恢复 =========="
    log_info "恢复日期: $backup_date"
    
    local errors=0
    
    # PostgreSQL恢复
    local pg_backup=$(ls -t "${BACKUP_ROOT}/database/${backup_date}"/*.dump 2>/dev/null | head -1)
    if [[ -n "$pg_backup" ]]; then
        restore_postgresql "$pg_backup" || ((errors++))
    else
        log_warn "未找到PostgreSQL备份: $backup_date"
    fi
    
    # Redis恢复
    local redis_backup=$(ls -t "${BACKUP_ROOT}/redis/${backup_date}"/*.rdb.gz 2>/dev/null | head -1)
    if [[ -n "$redis_backup" ]]; then
        restore_redis "$redis_backup" || ((errors++))
    else
        log_warn "未找到Redis备份: $backup_date"
    fi
    
    # 文件恢复
    for file_backup in "${BACKUP_ROOT}/files/${backup_date}"/*.tar.gz; do
        if [[ -f "$file_backup" ]]; then
            restore_files "$file_backup" || ((errors++))
        fi
    done
    
    if [[ $errors -eq 0 ]]; then
        log_info "完整恢复成功！"
    else
        log_error "完整恢复完成，但有 $errors 个错误"
    fi
    
    log_info "========== 恢复结束 =========="
    
    return $errors
}

# ============================================
# 使用帮助
# ============================================
show_usage() {
    cat << EOF
AI-Ready 数据恢复工具 v2.0.0

用法: $0 <命令> [参数]

命令:
    list                    列出所有可用备份
    pg <备份文件> [数据库]   恢复PostgreSQL
    redis <备份文件>        恢复Redis
    files <备份文件> [目录] 恢复文件存储
    full <日期>             完整恢复(格式: YYYY-MM-DD)
    help                    显示帮助信息

环境变量:
    DB_HOST                 数据库主机 (默认: localhost)
    DB_PORT                 数据库端口 (默认: 5432)
    DB_USER                 数据库用户 (默认: postgres)
    DB_PASSWORD             数据库密码
    REDIS_HOST              Redis主机 (默认: localhost)
    REDIS_PORT              Redis端口 (默认: 6379)
    REDIS_PASSWORD          Redis密码
    BACKUP_ROOT             备份根目录 (默认: /backup/ai-ready)

示例:
    # 列出备份
    $0 list

    # 恢复PostgreSQL
    $0 pg /backup/ai-ready/database/2026-04-04/ai_ready_prod_full_20260404_020000.dump

    # 完整恢复
    $0 full 2026-04-04

EOF
}

# ============================================
# 主入口
# ============================================
case "$1" in
    list)
        list_backups
        ;;
    pg)
        restore_postgresql "$2" "$3"
        ;;
    redis)
        restore_redis "$2"
        ;;
    files)
        restore_files "$2" "$3"
        ;;
    full)
        full_restore "$2"
        ;;
    help|--help|-h)
        show_usage
        ;;
    *)
        show_usage
        exit 1
        ;;
esac
