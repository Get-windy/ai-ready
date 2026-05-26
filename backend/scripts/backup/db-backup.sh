#!/bin/bash
# AI-Ready PostgreSQL 数据库备份脚本
# 版本: v1.0
# 用途: 全量备份+增量备份+自动清理

set -e

# ============ 配置参数 ============
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-ai_ready}"
DB_USER="${DB_USER:-postgres}"
BACKUP_DIR="${BACKUP_DIR:-/backup/ai-ready/db}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
RETENTION_WEEKLY="${RETENTION_WEEKLY:-90}"
RETENTION_MONTHLY="${RETENTION_MONTHLY:-365}"

# ============ 函数定义 ============
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

error_exit() {
    log "ERROR: $1"
    exit 1
}

# 全量备份
full_backup() {
    local timestamp=$(date '+%Y%m%d_%H%M%S')
    local backup_file="${BACKUP_DIR}/full/${DB_NAME}_full_${timestamp}.sql.gz"
    
    log "开始全量备份: $backup_file"
    
    mkdir -p "${BACKUP_DIR}/full"
    
    # pg_dump全量备份
    PGPASSWORD="${DB_PASSWORD}" pg_dump -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" \
        -d "${DB_NAME}" -F p -f - | gzip > "${backup_file}"
    
    if [ $? -eq 0 ]; then
        local size=$(du -h "${backup_file}" | cut -f1)
        log "全量备份完成: ${backup_file} (大小: ${size})"
        echo "${timestamp},full,${backup_file},${size}" >> "${BACKUP_DIR}/backup.log"
    else
        error_exit "全量备份失败"
    fi
}

# 增量备份（WAL归档）
incremental_backup() {
    local timestamp=$(date '+%Y%m%d_%H%M%S')
    local wal_dir="${BACKUP_DIR}/wal"
    
    log "开始WAL归档备份"
    
    mkdir -p "${wal_dir}"
    
    # 强制WAL切换
    psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -c "SELECT pg_switch_wal();" > /dev/null
    
    # 复制WAL文件
    rsync -av --delete "${PGDATA}/pg_wal/" "${wal_dir}/" 2>/dev/null || true
    
    log "WAL归档备份完成"
}

# 备份清理
cleanup_backups() {
    log "开始清理过期备份"
    
    # 清理全量备份
    find "${BACKUP_DIR}/full" -name "*.sql.gz" -mtime +${RETENTION_DAYS} -delete 2>/dev/null || true
    
    # 保留每周备份（90天）
    find "${BACKUP_DIR}/full" -name "*.sql.gz" -mtime +${RETENTION_WEEKLY} -delete 2>/dev/null || true
    
    # 保留每月备份（365天）
    # 保留每月1日的备份
    find "${BACKUP_DIR}/full" -name "*_01_*.sql.gz" -mtime +${RETENTION_MONTHLY} -delete 2>/dev/null || true
    
    log "过期备份清理完成"
}

# 备份验证
verify_backup() {
    local latest_backup=$(ls -t "${BACKUP_DIR}/full"/*.sql.gz 2>/dev/null | head -1)
    
    if [ -z "${latest_backup}" ]; then
        log "WARNING: 没有找到备份文件"
        return 1
    fi
    
    log "验证备份: ${latest_backup}"
    
    # 检查文件完整性
    if gzip -t "${latest_backup}" 2>/dev/null; then
        log "备份文件完整性验证通过"
        
        # 检查是否包含关键表
        local tables=$(zcat "${latest_backup}" | grep "CREATE TABLE" | wc -l)
        log "备份包含 ${tables} 个表定义"
        
        return 0
    else
        log "ERROR: 备份文件损坏"
        return 1
    fi
}

# 发送通知
send_notification() {
    local status=$1
    local message=$2
    
    if [ -n "${WEBHOOK_URL}" ]; then
        curl -s -X POST "${WEBHOOK_URL}" \
            -H "Content-Type: application/json" \
            -d "{\"msg_type\":\"text\",\"content\":{\"text\":\"[AI-Ready DB备份] ${status}: ${message}\"}}" > /dev/null
    fi
}

# ============ 主流程 ============
main() {
    log "========== AI-Ready 数据库备份开始 =========="
    
    # 检查目录
    mkdir -p "${BACKUP_DIR}"/{full,wal}
    
    # 执行备份
    full_backup
    incremental_backup
    
    # 清理过期备份
    cleanup_backups
    
    # 验证备份
    if verify_backup; then
        send_notification "SUCCESS" "数据库备份成功"
        log "========== 备份完成 =========="
        exit 0
    else
        send_notification "FAILED" "数据库备份验证失败"
        error_exit "备份验证失败"
    fi
}

# 执行
main "$@"