#!/bin/bash
# AI-Ready 综合备份脚本
# 支持: PostgreSQL数据库、Redis、文件存储
# Version: v2.0

set -e

# ============================================
# 全局配置
# ============================================
BACKUP_ROOT="${BACKUP_ROOT:-/backup/ai-ready}"
LOG_FILE="${BACKUP_ROOT}/logs/backup-$(date +%Y%m%d).log"
NOTIFY_WEBHOOK="${NOTIFY_WEBHOOK:-}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DATE=$(date +%Y-%m-%d)

# 创建目录
mkdir -p "${BACKUP_ROOT}"/{database,redis,files,logs}

# ============================================
# 日志函数
# ============================================
log() {
    local level="$1"
    local message="$2"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [$level] $message" | tee -a "$LOG_FILE"
}

log_info() { log "INFO" "$1"; }
log_warn() { log "WARN" "$1"; }
log_error() { log "ERROR" "$1"; }

# ============================================
# PostgreSQL 数据库备份
# ============================================
backup_postgresql() {
    log_info "开始PostgreSQL备份..."
    
    local db_host="${DB_HOST:-localhost}"
    local db_port="${DB_PORT:-5432}"
    local db_name="${DB_NAME:-ai_ready_prod}"
    local db_user="${DB_USER:-postgres}"
    local backup_dir="${BACKUP_ROOT}/database/${DATE}"
    
    mkdir -p "$backup_dir"
    
    # 全量备份
    local backup_file="${backup_dir}/${db_name}_full_${TIMESTAMP}.dump"
    
    PGPASSWORD="${DB_PASSWORD}" pg_dump \
        -h "$db_host" \
        -p "$db_port" \
        -U "$db_user" \
        -d "$db_name" \
        -Fc \
        -Z6 \
        -f "$backup_file"
    
    # 生成校验和
    sha256sum "$backup_file" > "${backup_file}.sha256"
    
    local size=$(du -h "$backup_file" | cut -f1)
    log_info "PostgreSQL备份完成: $backup_file ($size)"
    
    # 保留最近30天的备份
    find "${BACKUP_ROOT}/database" -name "*.dump" -mtime +30 -delete 2>/dev/null || true
    find "${BACKUP_ROOT}/database" -name "*.sha256" -mtime +30 -delete 2>/dev/null || true
}

# ============================================
# Redis 备份
# ============================================
backup_redis() {
    log_info "开始Redis备份..."
    
    local redis_host="${REDIS_HOST:-localhost}"
    local redis_port="${REDIS_PORT:-6379}"
    local backup_dir="${BACKUP_ROOT}/redis/${DATE}"
    
    mkdir -p "$backup_dir"
    
    # 触发RDB快照
    redis-cli -h "$redis_host" -p "$redis_port" -a "${REDIS_PASSWORD}" BGSAVE 2>/dev/null || true
    
    # 等待快照完成
    sleep 5
    
    # 复制RDB文件
    local rdb_file="${backup_dir}/redis_${TIMESTAMP}.rdb"
    
    if [[ -f "/var/lib/redis/dump.rdb" ]]; then
        cp /var/lib/redis/dump.rdb "$rdb_file"
        gzip -f "$rdb_file"
        log_info "Redis备份完成: ${rdb_file}.gz"
    else
        log_warn "Redis RDB文件不存在，跳过备份"
    fi
    
    # 保留最近7天的备份
    find "${BACKUP_ROOT}/redis" -name "*.rdb.gz" -mtime +7 -delete 2>/dev/null || true
}

# ============================================
# 文件存储备份
# ============================================
backup_files() {
    log_info "开始文件存储备份..."
    
    local source_dirs=("/var/lib/ai-ready/uploads" "/var/lib/ai-ready/documents")
    local backup_dir="${BACKUP_ROOT}/files/${DATE}"
    
    mkdir -p "$backup_dir"
    
    for src_dir in "${source_dirs[@]}"; do
        if [[ -d "$src_dir" ]]; then
            local dir_name=$(basename "$src_dir")
            local backup_file="${backup_dir}/${dir_name}_${TIMESTAMP}.tar.gz"
            
            tar -czf "$backup_file" -C "$(dirname "$src_dir")" "$dir_name"
            
            local size=$(du -h "$backup_file" | cut -f1)
            log_info "文件备份完成: $backup_file ($size)"
        fi
    done
    
    # 保留最近30天的备份
    find "${BACKUP_ROOT}/files" -name "*.tar.gz" -mtime +30 -delete 2>/dev/null || true
}

# ============================================
# 远程同步
# ============================================
sync_to_remote() {
    if [[ "${REMOTE_BACKUP_ENABLED:-false}" != "true" ]]; then
        return
    fi
    
    log_info "同步备份到远程服务器..."
    
    local remote_host="${REMOTE_HOST:-backup.example.com}"
    local remote_user="${REMOTE_USER:-backup}"
    local remote_path="${REMOTE_PATH:-/data/backups/ai-ready}"
    
    # 同步到远程
    rsync -avz --delete \
        "${BACKUP_ROOT}/" \
        "${remote_user}@${remote_host}:${remote_path}/${DATE}/"
    
    log_info "远程同步完成"
}

# ============================================
# 云存储上传
# ============================================
upload_to_cloud() {
    if [[ "${CLOUD_BACKUP_ENABLED:-false}" != "true" ]]; then
        return
    fi
    
    log_info "上传备份到云存储..."
    
    local bucket="${CLOUD_BUCKET:-ai-ready-backup}"
    
    # 上传到OSS
    if command -v ossutil &> /dev/null; then
        ossutil cp -r "${BACKUP_ROOT}/database/${DATE}" "oss://${bucket}/database/" --config ~/.ossutilconfig
        log_info "OSS上传完成"
    fi
    
    # 上传到S3
    if command -v aws &> /dev/null; then
        aws s3 sync "${BACKUP_ROOT}/database/${DATE}" "s3://${bucket}/database/${DATE}/" --storage-class GLACIER
        log_info "S3上传完成"
    fi
}

# ============================================
# 发送通知
# ============================================
send_notification() {
    local status="$1"
    local message="$2"
    
    if [[ -n "$NOTIFY_WEBHOOK" ]]; then
        curl -s -X POST "$NOTIFY_WEBHOOK" \
            -H "Content-Type: application/json" \
            -d "{\"status\":\"$status\",\"message\":\"$message\",\"timestamp\":\"$(date -Iseconds)\"}" \
            > /dev/null 2>&1 || true
    fi
}

# ============================================
# 主函数
# ============================================
main() {
    log_info "========== 开始AI-Ready自动化备份 =========="
    
    local start_time=$(date +%s)
    local errors=0
    
    # 执行备份
    backup_postgresql || { log_error "PostgreSQL备份失败"; ((errors++)); }
    backup_redis || { log_error "Redis备份失败"; ((errors++)); }
    backup_files || { log_error "文件备份失败"; ((errors++)); }
    
    # 同步到远程
    sync_to_remote || { log_warn "远程同步失败"; }
    
    # 上传到云存储
    upload_to_cloud || { log_warn "云存储上传失败"; }
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    # 汇总
    if [[ $errors -eq 0 ]]; then
        log_info "备份完成，耗时: ${duration}秒"
        send_notification "success" "AI-Ready备份完成，耗时${duration}秒"
    else
        log_error "备份完成，但有${errors}个错误"
        send_notification "warning" "AI-Ready备份完成，但有${errors}个错误"
    fi
    
    log_info "========== 备份结束 =========="
    
    return $errors
}

# 执行
main "$@"
