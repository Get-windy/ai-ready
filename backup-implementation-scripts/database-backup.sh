#!/bin/bash
# ERP系统数据库备份脚本
# 支持全量备份、增量备份、差异备份
# 版本: v1.0
# 创建日期: 2026-05-01

set -euo pipefail

# 配置参数
BACKUP_TYPE="${1:-full}"  # full/incremental/differential
BACKUP_DIR="/backup/database"
LOG_DIR="/var/log/backup"
CONFIG_FILE="/etc/erp/backup/database.conf"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30

# 加载配置
if [ -f "$CONFIG_FILE" ]; then
    source "$CONFIG_FILE"
fi

# 数据库配置
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-erp_prod}"
DB_USER="${DB_USER:-backup_user}"
DB_PASSWORD="${DB_PASSWORD:-}"
PGPASSWORD="$DB_PASSWORD"

# 日志函数
log_message() {
    local level="$1"
    local message="$2"
    local log_file="$LOG_DIR/database-backup-$(date +%Y%m%d).log"
    
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [$level] $message" | tee -a "$log_file"
}

# 检查目录
check_directories() {
    for dir in "$BACKUP_DIR" "$LOG_DIR"; do
        if [ ! -d "$dir" ]; then
            mkdir -p "$dir"
            log_message "INFO" "创建目录: $dir"
        fi
    done
    
    # 创建子目录
    mkdir -p "$BACKUP_DIR/full" "$BACKUP_DIR/incremental" "$BACKUP_DIR/differential" "$BACKUP_DIR/wal"
}

# 检查数据库连接
check_database_connection() {
    log_message "INFO" "检查数据库连接..."
    
    if pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" >/dev/null 2>&1; then
        log_message "INFO" "数据库连接正常"
        return 0
    else
        log_message "ERROR" "数据库连接失败"
        return 1
    fi
}

# 执行全量备份
perform_full_backup() {
    local backup_file="$BACKUP_DIR/full/${DB_NAME}_full_${TIMESTAMP}.dump"
    
    log_message "INFO" "开始执行全量备份..."
    
    # 使用pg_dump进行逻辑备份
    pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
        --format=custom \
        --compress=9 \
        --jobs=4 \
        --verbose \
        --file="$backup_file"
    
    local exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        log_message "INFO" "全量备份完成: $backup_file"
        
        # 计算备份文件大小
        local file_size=$(du -h "$backup_file" | cut -f1)
        log_message "INFO" "备份文件大小: $file_size"
        
        # 生成校验和
        md5sum "$backup_file" > "${backup_file}.md5"
        
        # 更新最新备份标记
        echo "$backup_file" > "$BACKUP_DIR/latest_full_backup"
        
        return 0
    else
        log_message "ERROR" "全量备份失败，退出码: $exit_code"
        return 1
    fi
}

# 执行增量备份（基于WAL）
perform_incremental_backup() {
    log_message "INFO" "开始执行增量备份..."
    
    # 检查是否有未归档的WAL文件
    local wal_files=$(find /var/lib/postgresql/wal_archive -name "*.wal" -mmin -15 2>/dev/null | wc -l)
    
    if [ "$wal_files" -eq 0 ]; then
        log_message "WARNING" "没有新的WAL文件需要备份"
        return 0
    fi
    
    # 打包最近的WAL文件
    local wal_backup_file="$BACKUP_DIR/incremental/${DB_NAME}_incr_${TIMESTAMP}.tar.gz"
    
    find /var/lib/postgresql/wal_archive -name "*.wal" -mmin -15 | \
        tar -czf "$wal_backup_file" -T -
    
    local exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        log_message "INFO" "增量备份完成: $wal_backup_file"
        
        # 清理已备份的WAL文件
        find /var/lib/postgresql/wal_archive -name "*.wal" -mmin -15 -delete
        
        return 0
    else
        log_message "ERROR" "增量备份失败"
        return 1
    fi
}

# 执行差异备份
perform_differential_backup() {
    local backup_file="$BACKUP_DIR/differential/${DB_NAME}_diff_${TIMESTAMP}.dump"
    
    log_message "INFO" "开始执行差异备份..."
    
    # 获取上次全量备份的时间
    local last_full_backup=""
    if [ -f "$BACKUP_DIR/latest_full_backup" ]; then
        last_full_backup=$(cat "$BACKUP_DIR/latest_full_backup")
    fi
    
    if [ -z "$last_full_backup" ]; then
        log_message "WARNING" "未找到全量备份，将执行全量备份"
        perform_full_backup
        return $?
    fi
    
    # 执行差异备份（备份自上次全量备份以来的变更）
    pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
        --format=custom \
        --compress=9 \
        --jobs=2 \
        --verbose \
        --file="$backup_file"
    
    local exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        log_message "INFO" "差异备份完成: $backup_file"
        
        # 更新最新差异备份标记
        echo "$backup_file" > "$BACKUP_DIR/latest_diff_backup"
        
        return 0
    else
        log_message "ERROR" "差异备份失败"
        return 1
    fi
}

# 验证备份文件
verify_backup() {
    local backup_file="$1"
    
    log_message "INFO" "验证备份文件: $backup_file"
    
    # 检查文件是否存在
    if [ ! -f "$backup_file" ]; then
        log_message "ERROR" "备份文件不存在: $backup_file"
        return 1
    fi
    
    # 检查文件大小
    local file_size=$(stat -c%s "$backup_file")
    if [ "$file_size" -lt 1024 ]; then
        log_message "ERROR" "备份文件过小: ${file_size}字节"
        return 1
    fi
    
    # 验证MD5校验和
    if [ -f "${backup_file}.md5" ]; then
        if md5sum -c "${backup_file}.md5" >/dev/null 2>&1; then
            log_message "INFO" "MD5校验通过"
        else
            log_message "ERROR" "MD5校验失败"
            return 1
        fi
    fi
    
    # 对于全量备份，尝试恢复测试
    if [[ "$backup_file" == *full* ]] && [ "$VERIFY_RESTORE" = "true" ]; then
        verify_backup_restore "$backup_file"
    fi
    
    return 0
}

# 验证备份恢复
verify_backup_restore() {
    local backup_file="$1"
    local test_db="backup_test_$(date +%s)"
    
    log_message "INFO" "执行恢复测试到临时数据库: $test_db"
    
    # 创建测试数据库
    createdb -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" "$test_db"
    
    # 恢复备份
    pg_restore -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$test_db" \
        --jobs=4 \
        --verbose \
        "$backup_file"
    
    local restore_exit=$?
    
    # 删除测试数据库
    dropdb -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" "$test_db"
    
    if [ $restore_exit -eq 0 ]; then
        log_message "INFO" "恢复测试通过"
        return 0
    else
        log_message "ERROR" "恢复测试失败"
        return 1
    fi
}

# 清理旧备份
cleanup_old_backups() {
    log_message "INFO" "清理超过${RETENTION_DAYS}天的旧备份..."
    
    # 清理全量备份
    find "$BACKUP_DIR/full" -name "*.dump" -mtime +$RETENTION_DAYS -delete
    find "$BACKUP_DIR/full" -name "*.md5" -mtime +$RETENTION_DAYS -delete
    
    # 清理增量备份
    find "$BACKUP_DIR/incremental" -name "*.tar.gz" -mtime +7 -delete
    
    # 清理差异备份
    find "$BACKUP_DIR/differential" -name "*.dump" -mtime +30 -delete
    
    log_message "INFO" "旧备份清理完成"
}

# 同步到云存储
sync_to_cloud() {
    local cloud_provider="${CLOUD_PROVIDER:-alicloud}"
    local cloud_bucket="${CLOUD_BUCKET:-erp-backup}"
    
    log_message "INFO" "开始同步到云存储: $cloud_provider"
    
    case "$cloud_provider" in
        "alicloud")
            # 阿里云OSS同步
            ossutil cp -r "$BACKUP_DIR" "oss://$cloud_bucket/database/" \
                --update \
                --parallel=10 \
                --bigfile-threshold=10240
            ;;
        "tencent")
            # 腾讯云COS同步
            coscli cp -r "$BACKUP_DIR" "cos://$cloud_bucket/database/" \
                --thread-num=10
            ;;
        "aws")
            # AWS S3同步
            aws s3 sync "$BACKUP_DIR" "s3://$cloud_bucket/database/" \
                --follow-symlinks
            ;;
        *)
            log_message "WARNING" "未知的云提供商: $cloud_provider"
            return 1
            ;;
    esac
    
    local sync_exit=$?
    
    if [ $sync_exit -eq 0 ]; then
        log_message "INFO" "云同步完成"
        return 0
    else
        log_message "ERROR" "云同步失败"
        return 1
    fi
}

# 发送通知
send_notification() {
    local status="$1"
    local message="$2"
    
    # 发送邮件通知
    if [ -n "$NOTIFICATION_EMAIL" ]; then
        echo -e "Subject: ERP数据库备份${status}\n\n${message}" | \
            sendmail "$NOTIFICATION_EMAIL"
    fi
    
    # 发送Webhook通知（如钉钉、Slack）
    if [ -n "$WEBHOOK_URL" ]; then
        curl -X POST "$WEBHOOK_URL" \
            -H "Content-Type: application/json" \
            -d "{\"status\":\"$status\",\"message\":\"$message\",\"timestamp\":\"$(date -Iseconds)\"}"
    fi
}

# 主函数
main() {
    log_message "INFO" "=== ERP数据库备份开始 ==="
    log_message "INFO" "备份类型: $BACKUP_TYPE"
    log_message "INFO" "时间戳: $TIMESTAMP"
    
    # 检查目录
    check_directories
    
    # 检查数据库连接
    if ! check_database_connection; then
        send_notification "失败" "数据库连接失败"
        exit 1
    fi
    
    # 执行备份
    local backup_result=0
    case "$BACKUP_TYPE" in
        "full")
            perform_full_backup
            backup_result=$?
            ;;
        "incremental")
            perform_incremental_backup
            backup_result=$?
            ;;
        "differential")
            perform_differential_backup
            backup_result=$?
            ;;
        *)
            log_message "ERROR" "未知的备份类型: $BACKUP_TYPE"
            send_notification "失败" "未知的备份类型: $BACKUP_TYPE"
            exit 1
            ;;
    esac
    
    # 验证备份
    if [ $backup_result -eq 0 ]; then
        local latest_backup=""
        case "$BACKUP_TYPE" in
            "full")
                latest_backup=$(cat "$BACKUP_DIR/latest_full_backup" 2>/dev/null)
                ;;
            "differential")
                latest_backup=$(cat "$BACKUP_DIR/latest_diff_backup" 2>/dev/null)
                ;;
            *)
                latest_backup=$(ls -t "$BACKUP_DIR/$BACKUP_TYPE"/* 2>/dev/null | head -1)
                ;;
        esac
        
        if [ -n "$latest_backup" ]; then
            if verify_backup "$latest_backup"; then
                log_message "INFO" "备份验证通过"
            else
                log_message "ERROR" "备份验证失败"
                backup_result=1
            fi
        fi
    fi
    
    # 清理旧备份
    cleanup_old_backups
    
    # 同步到云存储
    if [ "$CLOUD_SYNC_ENABLED" = "true" ] && [ $backup_result -eq 0 ]; then
        sync_to_cloud
    fi
    
    # 发送通知
    if [ $backup_result -eq 0 ]; then
        log_message "INFO" "=== 数据库备份成功完成 ==="
        send_notification "成功" "ERP数据库${BACKUP_TYPE}备份完成于$(date '+%Y-%m-%d %H:%M:%S')"
    else
        log_message "ERROR" "=== 数据库备份失败 ==="
        send_notification "失败" "ERP数据库${BACKUP_TYPE}备份失败，请检查日志"
    fi
    
    exit $backup_result
}

# 执行主函数
main "$@"