#!/bin/bash
# ERP系统文件备份脚本
# 支持配置文件、上传文件、日志文件备份
# 版本: v1.0
# 创建日期: 2026-05-01

set -euo pipefail

# 配置参数
BACKUP_TYPE="${1:-full}"  # full/incremental
BACKUP_DIR="/backup/files"
LOG_DIR="/var/log/backup"
CONFIG_FILE="/etc/erp/backup/files.conf"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=90

# 加载配置
if [ -f "$CONFIG_FILE" ]; then
    source "$CONFIG_FILE"
fi

# 备份源配置
CONFIG_SOURCE="${CONFIG_SOURCE:-/opt/erp/config}"
UPLOAD_SOURCE="${UPLOAD_SOURCE:-/opt/erp/uploads}"
LOG_SOURCE="${LOG_SOURCE:-/var/log/erp}"
DATA_SOURCE="${DATA_SOURCE:-/opt/erp/data}"

# 日志函数
log_message() {
    local level="$1"
    local message="$2"
    local log_file="$LOG_DIR/file-backup-$(date +%Y%m%d).log"
    
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
    mkdir -p "$BACKUP_DIR/config" "$BACKUP_DIR/uploads" "$BACKUP_DIR/logs" "$BACKUP_DIR/data"
}

# 检查备份源
check_sources() {
    log_message "INFO" "检查备份源..."
    
    local missing_sources=()
    
    for source in "$CONFIG_SOURCE" "$UPLOAD_SOURCE" "$LOG_SOURCE" "$DATA_SOURCE"; do
        if [ ! -d "$source" ] && [ ! -f "$source" ]; then
            missing_sources+=("$source")
        fi
    done
    
    if [ ${#missing_sources[@]} -gt 0 ]; then
        log_message "WARNING" "以下备份源不存在: ${missing_sources[*]}"
    else
        log_message "INFO" "所有备份源检查通过"
    fi
}

# 备份配置文件
backup_config_files() {
    local backup_dir="$BACKUP_DIR/config"
    local timestamp_file="$backup_dir/.last_backup_timestamp"
    
    log_message "INFO" "开始备份配置文件..."
    
    if [ ! -d "$CONFIG_SOURCE" ]; then
        log_message "WARNING" "配置文件源目录不存在: $CONFIG_SOURCE"
        return 0
    fi
    
    case "$BACKUP_TYPE" in
        "full")
            # 全量备份
            local backup_file="$backup_dir/config_full_${TIMESTAMP}.tar.gz"
            
            tar -czf "$backup_file" \
                -C "$(dirname "$CONFIG_SOURCE")" \
                "$(basename "$CONFIG_SOURCE")"
            
            local exit_code=$?
            
            if [ $exit_code -eq 0 ]; then
                log_message "INFO" "配置文件全量备份完成: $backup_file"
                
                # 更新时间戳
                echo "$TIMESTAMP" > "$timestamp_file"
                
                # 生成校验和
                md5sum "$backup_file" > "${backup_file}.md5"
                
                return 0
            else
                log_message "ERROR" "配置文件备份失败"
                return 1
            fi
            ;;
            
        "incremental")
            # 增量备份
            local last_timestamp=""
            if [ -f "$timestamp_file" ]; then
                last_timestamp=$(cat "$timestamp_file")
            fi
            
            if [ -z "$last_timestamp" ]; then
                log_message "INFO" "未找到上次备份时间戳，执行全量备份"
                backup_config_files "full"
                return $?
            fi
            
            # 查找自上次备份以来修改的文件
            local changed_files=$(find "$CONFIG_SOURCE" -type f -newer "$timestamp_file" 2>/dev/null)
            
            if [ -z "$changed_files" ]; then
                log_message "INFO" "配置文件无变更，跳过备份"
                return 0
            fi
            
            local backup_file="$backup_dir/config_incr_${TIMESTAMP}.tar.gz"
            
            echo "$changed_files" | tar -czf "$backup_file" -T -
            
            local exit_code=$?
            
            if [ $exit_code -eq 0 ]; then
                log_message "INFO" "配置文件增量备份完成: $backup_file"
                
                # 更新时间戳
                echo "$TIMESTAMP" > "$timestamp_file"
                
                return 0
            else
                log_message "ERROR" "配置文件增量备份失败"
                return 1
            fi
            ;;
    esac
}

# 备份上传文件
backup_upload_files() {
    local backup_dir="$BACKUP_DIR/uploads"
    local timestamp_file="$backup_dir/.last_backup_timestamp"
    
    log_message "INFO" "开始备份上传文件..."
    
    if [ ! -d "$UPLOAD_SOURCE" ]; then
        log_message "WARNING" "上传文件源目录不存在: $UPLOAD_SOURCE"
        return 0
    fi
    
    local backup_file="$backup_dir/uploads_${TIMESTAMP}.tar.gz"
    
    # 使用rsync风格的增量备份
    if [ -f "$timestamp_file" ] && [ "$BACKUP_TYPE" = "incremental" ]; then
        # 增量备份：只备份新文件
        local new_files=$(find "$UPLOAD_SOURCE" -type f -newer "$timestamp_file" 2>/dev/null)
        
        if [ -z "$new_files" ]; then
            log_message "INFO" "上传文件无新增，跳过备份"
            return 0
        fi
        
        echo "$new_files" | tar -czf "$backup_file" -T -
    else
        # 全量备份
        tar -czf "$backup_file" \
            -C "$(dirname "$UPLOAD_SOURCE")" \
            "$(basename "$UPLOAD_SOURCE")"
    fi
    
    local exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        log_message "INFO" "上传文件备份完成: $backup_file"
        
        # 更新时间戳
        echo "$TIMESTAMP" > "$timestamp_file"
        
        # 生成校验和
        md5sum "$backup_file" > "${backup_file}.md5"
        
        return 0
    else
        log_message "ERROR" "上传文件备份失败"
        return 1
    fi
}

# 备份日志文件
backup_log_files() {
    local backup_dir="$BACKUP_DIR/logs"
    
    log_message "INFO" "开始备份日志文件..."
    
    if [ ! -d "$LOG_SOURCE" ]; then
        log_message "WARNING" "日志文件源目录不存在: $LOG_SOURCE"
        return 0
    fi
    
    # 先进行日志轮转
    logrotate -f /etc/logrotate.d/erp 2>/dev/null || true
    
    # 备份昨天的日志文件
    local yesterday=$(date -d "yesterday" +%Y%m%d)
    local backup_file="$backup_dir/logs_${yesterday}.tar.gz"
    
    # 查找所有以日期命名的日志文件
    find "$LOG_SOURCE" -name "*${yesterday}*" -type f | \
        tar -czf "$backup_file" -T -
    
    local exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        local file_count=$(tar -tzf "$backup_file" | wc -l)
        log_message "INFO" "日志文件备份完成: $backup_file (${file_count}个文件)"
        
        # 清理原日志文件
        find "$LOG_SOURCE" -name "*${yesterday}*" -type f -delete
        
        return 0
    else
        log_message "ERROR" "日志文件备份失败"
        return 1
    fi
}

# 备份应用数据
backup_application_data() {
    local backup_dir="$BACKUP_DIR/data"
    
    log_message "INFO" "开始备份应用数据..."
    
    if [ ! -d "$DATA_SOURCE" ]; then
        log_message "WARNING" "应用数据源目录不存在: $DATA_SOURCE"
        return 0
    fi
    
    local backup_file="$backup_dir/data_${TIMESTAMP}.tar.gz"
    
    # 排除缓存和临时文件
    tar -czf "$backup_file" \
        -C "$(dirname "$DATA_SOURCE")" \
        "$(basename "$DATA_SOURCE")" \
        --exclude="*.tmp" \
        --exclude="*.cache" \
        --exclude="*.log" \
        --exclude="temp/*" \
        --exclude="cache/*"
    
    local exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        log_message "INFO" "应用数据备份完成: $backup_file"
        
        # 生成校验和
        md5sum "$backup_file" > "${backup_file}.md5"
        
        return 0
    else
        log_message "ERROR" "应用数据备份失败"
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
        log_message "WARNING" "备份文件较小: ${file_size}字节"
        # 对于某些类型的备份（如增量备份），小文件可能是正常的
    fi
    
    # 验证tar文件完整性
    if ! tar -tzf "$backup_file" >/dev/null 2>&1; then
        log_message "ERROR" "tar文件完整性检查失败"
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
    
    return 0
}

# 清理旧备份
cleanup_old_backups() {
    log_message "INFO" "清理超过${RETENTION_DAYS}天的旧备份..."
    
    # 清理配置文件备份
    find "$BACKUP_DIR/config" -name "*.tar.gz" -mtime +$RETENTION_DAYS -delete
    find "$BACKUP_DIR/config" -name "*.md5" -mtime +$RETENTION_DAYS -delete
    
    # 清理上传文件备份
    find "$BACKUP_DIR/uploads" -name "*.tar.gz" -mtime +180 -delete
    find "$BACKUP_DIR/uploads" -name "*.md5" -mtime +180 -delete
    
    # 清理日志文件备份
    find "$BACKUP_DIR/logs" -name "*.tar.gz" -mtime +30 -delete
    
    # 清理应用数据备份
    find "$BACKUP_DIR/data" -name "*.tar.gz" -mtime +$RETENTION_DAYS -delete
    find "$BACKUP_DIR/data" -name "*.md5" -mtime +$RETENTION_DAYS -delete
    
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
            ossutil cp -r "$BACKUP_DIR" "oss://$cloud_bucket/files/" \
                --update \
                --parallel=5 \
                --bigfile-threshold=5120
            ;;
        "tencent")
            # 腾讯云COS同步
            coscli cp -r "$BACKUP_DIR" "cos://$cloud_bucket/files/" \
                --thread-num=5
            ;;
        "aws")
            # AWS S3同步
            aws s3 sync "$BACKUP_DIR" "s3://$cloud_bucket/files/" \
                --follow-symlinks
            ;;
        *)
            log_message "WARNING" "未知的云提供商: $cloud_provider"
            return 1
            ;;
    esac
    
    local sync_exit=$?
    
    if [ $sync_exit -eq 0 ]; then
        log_message "INFO" "文件云同步完成"
        return 0
    else
        log_message "ERROR" "文件云同步失败"
        return 1
    fi
}

# 发送通知
send_notification() {
    local status="$1"
    local message="$2"
    
    # 发送邮件通知
    if [ -n "$NOTIFICATION_EMAIL" ]; then
        echo -e "Subject: ERP文件备份${status}\n\n${message}" | \
            sendmail "$NOTIFICATION_EMAIL"
    fi
    
    # 发送Webhook通知
    if [ -n "$WEBHOOK_URL" ]; then
        curl -X POST "$WEBHOOK_URL" \
            -H "Content-Type: application/json" \
            -d "{\"status\":\"$status\",\"message\":\"$message\",\"timestamp\":\"$(date -Iseconds)\",\"type\":\"file_backup\"}"
    fi
}

# 生成备份报告
generate_backup_report() {
    local report_file="/tmp/file-backup-report-${TIMESTAMP}.json"
    
    cat > "$report_file" << EOF
{
  "backup_type": "$BACKUP_TYPE",
  "timestamp": "$TIMESTAMP",
  "start_time": "$(date -Iseconds)",
  "end_time": "$(date -Iseconds)",
  "config_backup": {
    "source": "$CONFIG_SOURCE",
    "files_count": $(find "$CONFIG_SOURCE" -type f 2>/dev/null | wc -l),
    "backup_size": "$(du -sh "$BACKUP_DIR/config" 2>/dev/null | cut -f1 || echo "0")"
  },
  "upload_backup": {
    "source": "$UPLOAD_SOURCE",
    "files_count": $(find "$UPLOAD_SOURCE" -type f 2>/dev/null | wc -l),
    "backup_size": "$(du -sh "$BACKUP_DIR/uploads" 2>/dev/null | cut -f1 || echo "0")"
  },
  "log_backup": {
    "source": "$LOG_SOURCE",
    "files_count": $(find "$LOG_SOURCE" -type f 2>/dev/null | wc -l),
    "backup_size": "$(du -sh "$BACKUP_DIR/logs" 2>/dev/null | cut -f1 || echo "0")"
  },
  "status": "success"
}
EOF
    
    echo "$report_file"
}

# 主函数
main() {
    log_message "INFO" "=== ERP文件备份开始 ==="
    log_message "INFO" "备份类型: $BACKUP_TYPE"
    log_message "INFO" "时间戳: $TIMESTAMP"
    
    # 检查目录
    check_directories
    
    # 检查备份源
    check_sources
    
    # 执行备份
    local backup_results=()
    
    backup_config_files
    backup_results+=($?)
    
    backup_upload_files
    backup_results+=($?)
    
    backup_log_files
    backup_results+=($?)
    
    backup_application_data
    backup_results+=($?)
    
    # 检查备份结果
    local overall_result=0
    for result in "${backup_results[@]}"; do
        if [ $result -ne 0 ]; then
            overall_result=1
        fi
    done
    
    # 验证最新备份文件
    if [ $overall_result -eq 0 ]; then
        local latest_backup=$(find "$BACKUP_DIR" -name "*.tar.gz" -type f -newer "/tmp/file-backup-start" 2>/dev/null | head -1)
        
        if [ -n "$latest_backup" ]; then
            if verify_backup "$latest_backup"; then
                log_message "INFO" "备份验证通过"
            else
                log_message "WARNING" "备份验证发现警告"
            fi
        fi
    fi
    
    # 清理旧备份
    cleanup_old_backups
    
    # 同步到云存储
    if [ "$CLOUD_SYNC_ENABLED" = "true" ] && [ $overall_result -eq 0 ]; then
        sync_to_cloud
    fi
    
    # 生成报告
    local report_file=$(generate_backup_report)
    
    # 发送通知
    if [ $overall_result -eq 0 ]; then
        log_message "INFO" "=== 文件备份成功完成 ==="
        send_notification "成功" "ERP文件${BACKUP_TYPE}备份完成于$(date '+%Y-%m-%d %H:%M:%S')，报告: $report_file"
    else
        log_message "ERROR" "=== 文件备份失败 ==="
        send_notification "失败" "ERP文件${BACKUP_TYPE}备份失败，请检查日志"
    fi
    
    exit $overall_result
}

# 创建开始时间标记
touch /tmp/file-backup-start

# 执行主函数
main "$@"