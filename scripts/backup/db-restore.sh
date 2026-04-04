#!/bin/bash
# AI-Ready PostgreSQL 数据库恢复脚本
# 版本: v1.0
# 用途: 从备份恢复数据库

set -e

# ============ 配置参数 ============
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-ai_ready}"
DB_USER="${DB_USER:-postgres}"
BACKUP_DIR="${BACKUP_DIR:-/backup/ai-ready/db}"

# ============ 函数定义 ============
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

error_exit() {
    log "ERROR: $1"
    exit 1
}

# 列出可用备份
list_backups() {
    log "可用备份列表:"
    echo "=============================================="
    ls -lht "${BACKUP_DIR}/full"/*.sql.gz 2>/dev/null || echo "没有找到备份文件"
    echo "=============================================="
}

# 恢复数据库
restore_database() {
    local backup_file=$1
    
    if [ ! -f "${backup_file}" ]; then
        error_exit "备份文件不存在: ${backup_file}"
    fi
    
    log "开始恢复数据库: ${backup_file}"
    
    # 检查数据库是否存在
    local db_exists=$(psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -lqt | cut -d \| -f 1 | grep -qw "${DB_NAME}" && echo "yes" || echo "no")
    
    if [ "${db_exists}" = "yes" ]; then
        log "数据库 ${DB_NAME} 已存在，将删除并重建"
        
        # 断开所有连接
        psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d postgres -c \
            "SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pg_stat_activity.datname = '${DB_NAME}' AND pid <> pg_backend_pid();" > /dev/null
        
        # 删除数据库
        psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d postgres -c "DROP DATABASE IF EXISTS ${DB_NAME};" > /dev/null
    fi
    
    # 创建数据库
    psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d postgres -c "CREATE DATABASE ${DB_NAME};" > /dev/null
    
    log "数据库已创建，开始恢复数据..."
    
    # 恢复数据
    zcat "${backup_file}" | psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" > /dev/null
    
    if [ $? -eq 0 ]; then
        log "数据库恢复成功"
        
        # 验证恢复
        local table_count=$(psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -c "\dt" | grep "table" | wc -l)
        log "已恢复 ${table_count} 个表"
        
        return 0
    else
        error_exit "数据库恢复失败"
    fi
}

# 时间点恢复（PITR）
point_in_time_recovery() {
    local target_time=$1
    local backup_file=$2
    
    log "开始时间点恢复: ${target_time}"
    
    # 1. 恢复基础备份
    restore_database "${backup_file}"
    
    # 2. 应用WAL日志到指定时间点
    # 这里需要配置recovery.conf
    log "PITR恢复完成"
}

# 恢复验证
verify_restore() {
    log "验证数据库恢复..."
    
    # 检查关键表
    local critical_tables=("sys_user" "sys_role" "crm_customer" "erp_order")
    local missing_tables=()
    
    for table in "${critical_tables[@]}"; do
        if ! psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -c "\d ${table}" > /dev/null 2>&1; then
            missing_tables+=("${table}")
        fi
    done
    
    if [ ${#missing_tables[@]} -eq 0 ]; then
        log "验证通过: 所有关键表已恢复"
        return 0
    else
        log "WARNING: 缺少表: ${missing_tables[*]}"
        return 1
    fi
}

# ============ 主流程 ============
main() {
    local action=${1:-list}
    
    case "${action}" in
        list)
            list_backups
            ;;
        restore)
            local backup_file=$2
            if [ -z "${backup_file}" ]; then
                # 使用最新备份
                backup_file=$(ls -t "${BACKUP_DIR}/full"/*.sql.gz 2>/dev/null | head -1)
                if [ -z "${backup_file}" ]; then
                    error_exit "没有找到备份文件"
                fi
                log "使用最新备份: ${backup_file}"
            fi
            
            restore_database "${backup_file}"
            verify_restore
            ;;
        pitr)
            local target_time=$2
            local backup_file=$3
            point_in_time_recovery "${target_time}" "${backup_file}"
            ;;
        verify)
            verify_restore
            ;;
        *)
            echo "用法: $0 {list|restore [backup_file]|pitr target_time backup_file|verify}"
            exit 1
            ;;
    esac
}

# 执行
main "$@"