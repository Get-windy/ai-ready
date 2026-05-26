#!/bin/bash

# PostgreSQL 全量备份脚本
# 用法: ./backup-postgres-full.sh [数据库名]
# 默认备份所有数据库

set -euo pipefail

# 配置参数
BACKUP_DIR="/data/backups/postgresql/full"
LOG_DIR="/data/backups/logs"
RETENTION_DAYS=7
COMPRESS_LEVEL=6
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
HOSTNAME=$(hostname)

# PostgreSQL 连接参数
PG_HOST="${PG_HOST:-localhost}"
PG_PORT="${PG_PORT:-5432}"
PG_USER="${PG_USER:-postgres}"
PG_PASSWORD="${PG_PASSWORD:-}"
PG_DATABASE="${1:-all}"  # 支持指定单个数据库或所有数据库

# 导出密码用于 pg_dump
export PGPASSWORD="$PG_PASSWORD"

# 创建目录
mkdir -p "$BACKUP_DIR"
mkdir -p "$LOG_DIR"

# 日志函数
log() {
    local level="$1"
    local message="$2"
    local log_file="$LOG_DIR/postgres-backup-$(date +"%Y%m%d").log"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [$level] $message" | tee -a "$log_file"
}

# 错误处理函数
error_exit() {
    log "ERROR" "备份失败: $1"
    exit 1
}

# 清理旧备份
cleanup_old_backups() {
    log "INFO" "清理超过 $RETENTION_DAYS 天的旧备份..."
    find "$BACKUP_DIR" -name "*.sql.gz" -mtime +$RETENTION_DAYS -type f -delete 2>/dev/null || true
    log "INFO" "旧备份清理完成"
}

# 验证备份文件
validate_backup() {
    local backup_file="$1"
    
    log "INFO" "验证备份文件: $backup_file"
    
    # 检查文件是否存在且大小合理
    if [[ ! -f "$backup_file" ]]; then
        error_exit "备份文件不存在: $backup_file"
    fi
    
    local file_size=$(stat -c%s "$backup_file")
    if [[ $file_size -lt 1024 ]]; then  # 小于1KB视为无效
        error_exit "备份文件过小(${file_size}字节)，可能备份失败"
    fi
    
    # 验证gzip文件完整性
    if ! gzip -t "$backup_file" 2>/dev/null; then
        error_exit "备份文件gzip压缩损坏"
    fi
    
    log "INFO" "备份文件验证通过，大小: $(numfmt --to=iec-i $file_size)"
}

# 备份单个数据库
backup_single_database() {
    local db_name="$1"
    local backup_file="$BACKUP_DIR/backup_full_${db_name}_${TIMESTAMP}.sql.gz"
    
    log "INFO" "开始备份数据库: $db_name"
    
    # 执行备份
    if ! pg_dump \
        --host="$PG_HOST" \
        --port="$PG_PORT" \
        --username="$PG_USER" \
        --format=plain \
        --encoding=UTF8 \
        --no-password \
        --verbose \
        "$db_name" \
        | gzip -c -$COMPRESS_LEVEL > "$backup_file"
    then
        error_exit "数据库 $db_name 备份失败"
    fi
    
    # 验证备份
    validate_backup "$backup_file"
    
    # 生成checksum
    md5sum "$backup_file" > "$backup_file.md5"
    
    log "INFO" "数据库 $db_name 备份完成: $backup_file"
    echo "$backup_file"
}

# 备份所有数据库
backup_all_databases() {
    local backup_file="$BACKUP_DIR/backup_full_all_${TIMESTAMP}.sql.gz"
    
    log "INFO" "开始备份所有数据库"
    
    # 获取数据库列表（排除template和postgres系统库）
    DATABASES=$(psql \
        --host="$PG_HOST" \
        --port="$PG_PORT" \
        --username="$PG_USER" \
        --no-password \
        --tuples-only \
        --command="SELECT datname FROM pg_database WHERE datistemplate = false AND datname NOT IN ('postgres');")
    
    if [[ -z "$DATABASES" ]]; then
        error_exit "未找到可备份的数据库"
    fi
    
    log "INFO" "找到数据库: $(echo $DATABASES | tr '\n' ' ')"
    
    # 执行全库备份
    if ! pg_dumpall \
        --host="$PG_HOST" \
        --port="$PG_PORT" \
        --username="$PG_USER" \
        --no-password \
        --verbose \
        --clean \
        | gzip -c -$COMPRESS_LEVEL > "$backup_file"
    then
        error_exit "全库备份失败"
    fi
    
    # 验证备份
    validate_backup "$backup_file"
    
    # 生成checksum
    md5sum "$backup_file" > "$backup_file.md5"
    
    log "INFO" "全库备份完成: $backup_file"
    echo "$backup_file"
}

# 生成备份报告
generate_report() {
    local backup_files=("$@")
    local report_file="$LOG_DIR/backup-report-${TIMESTAMP}.json"
    
    cat > "$report_file" << EOF
{
  "backup_report": {
    "timestamp": "$TIMESTAMP",
    "hostname": "$HOSTNAME",
    "type": "full",
    "database": "$PG_DATABASE",
    "status": "success",
    "backup_files": [
EOF
    
    for ((i=0; i<${#backup_files[@]}; i++)); do
        local file="${backup_files[$i]}"
        local size=$(stat -c%s "$file" 2>/dev/null || echo "0")
        local md5=$(cat "$file.md5" 2>/dev/null | cut -d' ' -f1 || echo "")
        
        if [[ $i -ne 0 ]]; then
            echo "      ," >> "$report_file"
        fi
        
        cat >> "$report_file" << EOF
      {
        "filename": "$(basename "$file")",
        "path": "$file",
        "size_bytes": $size,
        "size_human": "$(numfmt --to=iec-i $size 2>/dev/null || echo "N/A")",
        "md5": "$md5",
        "created_at": "$(date -r "$file" '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo "N/A")"
      }
EOF
    done
    
    cat >> "$report_file" << EOF
    ],
    "retention_days": $RETENTION_DAYS,
    "compression_level": $COMPRESS_LEVEL,
    "duration_seconds": $SECONDS
  }
}
EOF
    
    log "INFO" "备份报告生成: $report_file"
}

# 主函数
main() {
    log "INFO" "================================================"
    log "INFO" "PostgreSQL 全量备份开始"
    log "INFO" "主机: $HOSTNAME"
    log "INFO" "数据库: $PG_DATABASE"
    log "INFO" "时间: $TIMESTAMP"
    log "INFO" "================================================"
    
    # 记录开始时间
    local start_time=$SECONDS
    local backup_files=()
    
    # 清理旧备份
    cleanup_old_backups
    
    # 执行备份
    if [[ "$PG_DATABASE" == "all" ]]; then
        local backup_file=$(backup_all_databases)
        backup_files+=("$backup_file")
    else
        local backup_file=$(backup_single_database "$PG_DATABASE")
        backup_files+=("$backup_file")
    fi
    
    # 计算耗时
    local duration=$((SECONDS - start_time))
    
    # 生成报告
    generate_report "${backup_files[@]}"
    
    log "INFO" "================================================"
    log "INFO" "PostgreSQL 全量备份完成"
    log "INFO" "总耗时: ${duration} 秒"
    log "INFO" "备份文件:"
    for file in "${backup_files[@]}"; do
        log "INFO" "  - $file"
    done
    log "INFO" "================================================"
    
    # 发送成功通知（可选）
    # send_notification "success" "PostgreSQL全量备份成功" "$(cat "$report_file")"
    
    return 0
}

# 执行主函数
main "$@"