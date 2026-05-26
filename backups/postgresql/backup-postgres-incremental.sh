#!/bin/bash

# PostgreSQL 增量备份脚本
# 基于WAL日志归档的增量备份
# 用法: ./backup-postgres-incremental.sh

set -euo pipefail

# 配置参数
BACKUP_DIR="/data/backups/postgresql/incremental"
WAL_DIR="/data/backups/postgresql/wal"
LOG_DIR="/data/backups/logs"
RETENTION_DAYS=30
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
HOSTNAME=$(hostname)

# PostgreSQL 连接参数
PG_HOST="${PG_HOST:-localhost}"
PG_PORT="${PG_PORT:-5432}"
PG_USER="${PG_USER:-postgres}"
PG_PASSWORD="${PG_PASSWORD:-}"
PG_DATA_DIR="${PG_DATA_DIR:-/var/lib/postgresql/data}"

# 导出密码用于psql
export PGPASSWORD="$PG_PASSWORD"

# 创建目录
mkdir -p "$BACKUP_DIR"
mkdir -p "$WAL_DIR"
mkdir -p "$LOG_DIR"

# 日志函数
log() {
    local level="$1"
    local message="$2"
    local log_file="$LOG_DIR/postgres-incremental-$(date +"%Y%m%d").log"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [$level] $message" | tee -a "$log_file"
}

# 错误处理函数
error_exit() {
    log "ERROR" "增量备份失败: $1"
    exit 1
}

# 检查PostgreSQL是否运行
check_postgres_running() {
    if ! pg_isready --host="$PG_HOST" --port="$PG_PORT" --username="$PG_USER" --quiet; then
        error_exit "PostgreSQL服务未运行或无法连接"
    fi
    log "INFO" "PostgreSQL服务运行正常"
}

# 检查WAL归档配置
check_wal_archiving() {
    log "INFO" "检查WAL归档配置..."
    
    # 检查archive_mode是否开启
    local archive_mode=$(psql \
        --host="$PG_HOST" \
        --port="$PG_PORT" \
        --username="$PG_USER" \
        --no-password \
        --tuples-only \
        --command="SHOW archive_mode;" 2>/dev/null || echo "off")
    
    if [[ "$archive_mode" != "on" ]] && [[ "$archive_mode" != "always" ]]; then
        log "WARN" "WAL归档未启用(archive_mode=$archive_mode)，建议启用以获得完整PITR能力"
    else
        log "INFO" "WAL归档已启用(archive_mode=$archive_mode)"
    fi
    
    # 检查当前WAL位置
    local current_wal=$(psql \
        --host="$PG_HOST" \
        --port="$PG_PORT" \
        --username="$PG_USER" \
        --no-password \
        --tuples-only \
        --command="SELECT pg_walfile_name(pg_current_wal_lsn());" 2>/dev/null || echo "unknown")
    
    log "INFO" "当前WAL文件: $current_wal"
}

# 执行增量备份（basebackup）
perform_incremental_backup() {
    log "INFO" "开始创建增量备份基准备份..."
    
    local backup_name="incremental_base_${TIMESTAMP}"
    local backup_path="$BACKUP_DIR/$backup_name"
    
    # 创建备份目录
    mkdir -p "$backup_path"
    
    # 使用pg_basebackup创建基准备份
    if ! pg_basebackup \
        --host="$PG_HOST" \
        --port="$PG_PORT" \
        --username="$PG_USER" \
        --no-password \
        --pgdata="$backup_path" \
        --format=plain \
        --progress \
        --verbose \
        --write-recovery-conf \
        --label="$backup_name" \
        --wal-method=stream
    then
        error_exit "pg_basebackup失败"
    fi
    
    # 压缩备份目录
    log "INFO" "压缩备份目录..."
    local tar_file="$BACKUP_DIR/${backup_name}.tar.gz"
    if ! tar -czf "$tar_file" -C "$BACKUP_DIR" "$backup_name"; then
        error_exit "备份目录压缩失败"
    fi
    
    # 删除原始目录，保留压缩文件
    rm -rf "$backup_path"
    
    # 验证备份文件
    validate_backup_file "$tar_file"
    
    log "INFO" "增量基准备份创建完成: $tar_file"
    echo "$tar_file"
}

# 归档WAL日志
archive_wal_logs() {
    log "INFO" "归档WAL日志..."
    
    # 检查pg_wal目录
    local wal_source_dir="$PG_DATA_DIR/pg_wal"
    if [[ ! -d "$wal_source_dir" ]]; then
        wal_source_dir="$PG_DATA_DIR/pg_xlog"  # PostgreSQL 9.x
    fi
    
    if [[ ! -d "$wal_source_dir" ]]; then
        log "WARN" "无法找到WAL目录，跳过WAL归档"
        return 0
    fi
    
    # 复制最新的WAL文件（排除当前正在使用的）
    local wal_files_count=0
    for wal_file in "$wal_source_dir"/*; do
        if [[ -f "$wal_file" ]] && [[ "$wal_file" != *.partial ]]; then
            local filename=$(basename "$wal_file")
            if [[ ! -f "$WAL_DIR/$filename" ]]; then
                cp "$wal_file" "$WAL_DIR/"
                wal_files_count=$((wal_files_count + 1))
            fi
        fi
    done
    
    log "INFO" "已归档 $wal_files_count 个WAL文件到 $WAL_DIR"
    
    # 清理旧WAL文件（保留48小时）
    find "$WAL_DIR" -name "*.wal" -o -name "*[0-9A-F]*" -mtime +2 -type f -delete 2>/dev/null || true
}

# 验证备份文件
validate_backup_file() {
    local backup_file="$1"
    
    log "INFO" "验证备份文件: $backup_file"
    
    # 检查文件是否存在且大小合理
    if [[ ! -f "$backup_file" ]]; then
        error_exit "备份文件不存在: $backup_file"
    fi
    
    local file_size=$(stat -c%s "$backup_file")
    if [[ $file_size -lt 10240 ]]; then  # 小于10KB视为无效
        error_exit "备份文件过小(${file_size}字节)，可能备份失败"
    fi
    
    # 验证tar.gz文件完整性
    if ! gzip -t "$backup_file" 2>/dev/null; then
        error_exit "备份文件gzip压缩损坏"
    fi
    
    # 验证tar文件结构
    if ! tar -tzf "$backup_file" >/dev/null 2>&1; then
        error_exit "备份文件tar格式损坏"
    fi
    
    log "INFO" "备份文件验证通过，大小: $(numfmt --to=iec-i $file_size)"
    
    # 生成checksum
    md5sum "$backup_file" > "$backup_file.md5"
}

# 清理旧备份
cleanup_old_backups() {
    log "INFO" "清理超过 $RETENTION_DAYS 天的旧增量备份..."
    find "$BACKUP_DIR" -name "*.tar.gz" -mtime +$RETENTION_DAYS -type f -delete 2>/dev/null || true
    log "INFO" "旧增量备份清理完成"
}

# 检查备份完整性
check_backup_integrity() {
    log "INFO" "检查备份完整性..."
    
    # 检查是否有完整的备份链
    local base_backups=($(find "$BACKUP_DIR" -name "incremental_base_*.tar.gz" -type f | sort))
    local wal_files=($(find "$WAL_DIR" -type f | wc -l))
    
    if [[ ${#base_backups[@]} -eq 0 ]]; then
        log "WARN" "未找到基准备份，建议先运行全量备份"
    else
        log "INFO" "找到 ${#base_backups[@]} 个基准备份"
        
        # 检查最新的基准备份
        local latest_backup="${base_backups[-1]}"
        local backup_age=$(($(date +%s) - $(date -r "$latest_backup" +%s)))
        local backup_age_hours=$((backup_age / 3600))
        
        if [[ $backup_age_hours -gt 24 ]]; then
            log "WARN" "最新的基准备份已超过24小时(${backup_age_hours}小时)，建议更新"
        fi
    fi
    
    log "INFO" "WAL归档目录有 $wal_files 个文件"
    
    # 检查存储空间
    check_storage_space
}

# 检查存储空间
check_storage_space() {
    local backup_dir_space=$(df "$BACKUP_DIR" | awk 'NR==2 {print $5}' | sed 's/%//')
    local wal_dir_space=$(df "$WAL_DIR" | awk 'NR==2 {print $5}' | sed 's/%//')
    
    if [[ $backup_dir_space -gt 80 ]]; then
        log "WARN" "备份目录存储空间使用率超过80%: ${backup_dir_space}%"
    fi
    
    if [[ $wal_dir_space -gt 80 ]]; then
        log "WARN" "WAL目录存储空间使用率超过80%: ${wal_dir_space}%"
    fi
    
    log "INFO" "存储空间状态: 备份目录${backup_dir_space}%, WAL目录${wal_dir_space}%"
}

# 生成备份报告
generate_report() {
    local backup_file="$1"
    local report_file="$LOG_DIR/incremental-report-${TIMESTAMP}.json"
    
    local file_size=$(stat -c%s "$backup_file" 2>/dev/null || echo "0")
    local md5=$(cat "$backup_file.md5" 2>/dev/null | cut -d' ' -f1 || echo "")
    
    # 获取数据库信息
    local db_version=$(psql \
        --host="$PG_HOST" \
        --port="$PG_PORT" \
        --username="$PG_USER" \
        --no-password \
        --tuples-only \
        --command="SELECT version();" 2>/dev/null | head -1 || echo "unknown")
    
    local db_count=$(psql \
        --host="$PG_HOST" \
        --port="$PG_PORT" \
        --username="$PG_USER" \
        --no-password \
        --tuples-only \
        --command="SELECT COUNT(*) FROM pg_database WHERE datistemplate = false;" 2>/dev/null || echo "0")
    
    cat > "$report_file" << EOF
{
  "incremental_backup_report": {
    "timestamp": "$TIMESTAMP",
    "hostname": "$HOSTNAME",
    "type": "incremental",
    "status": "success",
    "database_info": {
      "version": "$db_version",
      "database_count": $db_count
    },
    "backup_file": {
      "filename": "$(basename "$backup_file")",
      "path": "$backup_file",
      "size_bytes": $file_size,
      "size_human": "$(numfmt --to=iec-i $file_size 2>/dev/null || echo "N/A")",
      "md5": "$md5"
    },
    "wal_archive": {
      "directory": "$WAL_DIR",
      "file_count": $(find "$WAL_DIR" -type f | wc -l)
    },
    "retention_days": $RETENTION_DAYS,
    "duration_seconds": $SECONDS
  }
}
EOF
    
    log "INFO" "增量备份报告生成: $report_file"
}

# 主函数
main() {
    log "INFO" "================================================"
    log "INFO" "PostgreSQL 增量备份开始"
    log "INFO" "主机: $HOSTNAME"
    log "INFO" "时间: $TIMESTAMP"
    log "INFO" "================================================"
    
    # 记录开始时间
    local start_time=$SECONDS
    
    # 检查PostgreSQL状态
    check_postgres_running
    check_wal_archiving
    
    # 检查备份完整性
    check_backup_integrity
    
    # 执行增量备份
    local backup_file=$(perform_incremental_backup)
    
    # 归档WAL日志
    archive_wal_logs
    
    # 清理旧备份
    cleanup_old_backups
    
    # 计算耗时
    local duration=$((SECONDS - start_time))
    
    # 生成报告
    generate_report "$backup_file"
    
    log "INFO" "================================================"
    log "INFO" "PostgreSQL 增量备份完成"
    log "INFO" "总耗时: ${duration} 秒"
    log "INFO" "备份文件: $backup_file"
    log "INFO" "WAL归档: $WAL_DIR ($(find "$WAL_DIR" -type f | wc -l) 个文件)"
    log "INFO" "================================================"
    
    return 0
}

# 执行主函数
main "$@"