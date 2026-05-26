#!/bin/bash

# Redis 数据备份脚本
# 支持RDB快照和AOF持久化备份
# 用法: ./backup-redis.sh [backup_type]
# backup_type: rdb (默认), aof, both

set -euo pipefail

# 配置参数
BACKUP_DIR="/data/backups/redis"
RDB_DIR="$BACKUP_DIR/rdb"
AOF_DIR="$BACKUP_DIR/aof"
LOG_DIR="/data/backups/logs"
RETENTION_DAYS=7
COMPRESS_LEVEL=6
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
HOSTNAME=$(hostname)

# Redis 连接参数
REDIS_HOST="${REDIS_HOST:-localhost}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
REDIS_CLI="redis-cli"

# 备份类型
BACKUP_TYPE="${1:-rdb}"  # rdb, aof, both

# 创建目录
mkdir -p "$BACKUP_DIR"
mkdir -p "$RDB_DIR"
mkdir -p "$AOF_DIR"
mkdir -p "$LOG_DIR"

# 日志函数
log() {
    local level="$1"
    local message="$2"
    local log_file="$LOG_DIR/redis-backup-$(date +"%Y%m%d").log"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [$level] $message" | tee -a "$log_file"
}

# 错误处理函数
error_exit() {
    log "ERROR" "Redis备份失败: $1"
    exit 1
}

# 检查Redis是否运行
check_redis_running() {
    log "INFO" "检查Redis服务状态..."
    
    local redis_cmd="$REDIS_CLI"
    [[ -n "$REDIS_HOST" ]] && redis_cmd="$redis_cmd -h $REDIS_HOST"
    [[ -n "$REDIS_PORT" ]] && redis_cmd="$redis_cmd -p $REDIS_PORT"
    [[ -n "$REDIS_PASSWORD" ]] && redis_cmd="$redis_cmd -a $REDIS_PASSWORD"
    
    if ! $redis_cmd ping 2>/dev/null | grep -q "PONG"; then
        error_exit "Redis服务未运行或无法连接"
    fi
    
    log "INFO" "Redis服务运行正常"
    
    # 获取Redis信息
    local redis_info=$($redis_cmd info 2>/dev/null)
    local redis_version=$(echo "$redis_info" | grep "^redis_version:" | cut -d: -f2)
    local used_memory=$(echo "$redis_info" | grep "^used_memory_human:" | cut -d: -f2)
    local total_keys=$(echo "$redis_info" | grep "^db0:keys=" | cut -d= -f2 | cut -d, -f1 || echo "0")
    
    log "INFO" "Redis版本: $redis_version"
    log "INFO" "内存使用: $used_memory"
    log "INFO" "键数量: $total_keys"
}

# 获取Redis配置
get_redis_config() {
    local redis_cmd="$REDIS_CLI"
    [[ -n "$REDIS_HOST" ]] && redis_cmd="$redis_cmd -h $REDIS_HOST"
    [[ -n "$REDIS_PORT" ]] && redis_cmd="$redis_cmd -p $REDIS_PORT"
    [[ -n "$REDIS_PASSWORD" ]] && redis_cmd="$redis_cmd -a $REDIS_PASSWORD"
    
    local config=$($redis_cmd config get dir 2>/dev/null)
    REDIS_DATA_DIR=$(echo "$config" | grep -v "^dir$" | head -1)
    REDIS_DATA_DIR="${REDIS_DATA_DIR:-/var/lib/redis}"
    
    log "INFO" "Redis数据目录: $REDIS_DATA_DIR"
    
    # 获取RDB和AOF配置
    local save_config=$($redis_cmd config get save 2>/dev/null)
    local appendonly=$($redis_cmd config get appendonly 2>/dev/null | grep -v "^appendonly$" | head -1)
    local appendfilename=$($redis_cmd config get appendfilename 2>/dev/null | grep -v "^appendfilename$" | head -1)
    
    log "INFO" "RDB保存配置: $save_config"
    log "INFO" "AOF启用: $appendonly"
    log "INFO" "AOF文件名: $appendfilename"
}

# 备份RDB文件
backup_rdb() {
    log "INFO" "开始备份RDB快照..."
    
    # 触发BGSAVE
    local redis_cmd="$REDIS_CLI"
    [[ -n "$REDIS_HOST" ]] && redis_cmd="$redis_cmd -h $REDIS_HOST"
    [[ -n "$REDIS_PORT" ]] && redis_cmd="$redis_cmd -p $REDIS_PORT"
    [[ -n "$REDIS_PASSWORD" ]] && redis_cmd="$redis_cmd -a $REDIS_PASSWORD"
    
    log "INFO" "触发BGSAVE..."
    if ! $redis_cmd bgsave 2>/dev/null | grep -q "Background saving started"; then
        error_exit "BGSAVE触发失败"
    fi
    
    # 等待BGSAVE完成
    local max_wait=60
    local wait_interval=5
    local waited=0
    
    log "INFO" "等待BGSAVE完成..."
    while [[ $waited -lt $max_wait ]]; do
        local save_status=$($redis_cmd info persistence 2>/dev/null | grep "^rdb_bgsave_in_progress:" | cut -d: -f2)
        
        if [[ "$save_status" == "0" ]]; then
            log "INFO" "BGSAVE完成"
            break
        fi
        
        log "INFO" "BGSAVE进行中... 等待${wait_interval}秒"
        sleep $wait_interval
        waited=$((waited + wait_interval))
    done
    
    if [[ $waited -ge $max_wait ]]; then
        log "WARN" "BGSAVE超时，继续备份现有RDB文件"
    fi
    
    # 查找最新的RDB文件
    local rdb_file=$(find "$REDIS_DATA_DIR" -name "*.rdb" -type f -printf "%T@ %p\n" 2>/dev/null | sort -nr | head -1 | cut -d' ' -f2-)
    
    if [[ -z "$rdb_file" ]] || [[ ! -f "$rdb_file" ]]; then
        error_exit "未找到RDB文件"
    fi
    
    # 复制RDB文件到备份目录
    local backup_filename="redis_rdb_${TIMESTAMP}.rdb"
    local backup_path="$RDB_DIR/$backup_filename"
    
    log "INFO" "复制RDB文件: $rdb_file -> $backup_path"
    cp "$rdb_file" "$backup_path"
    
    # 压缩备份文件
    log "INFO" "压缩RDB备份文件..."
    local compressed_file="${backup_path}.gz"
    if ! gzip -c -$COMPRESS_LEVEL "$backup_path" > "$compressed_file"; then
        error_exit "RDB文件压缩失败"
    fi
    
    # 删除未压缩的副本
    rm -f "$backup_path"
    
    # 验证备份文件
    validate_backup_file "$compressed_file"
    
    log "INFO" "RDB备份完成: $compressed_file"
    echo "$compressed_file"
}

# 备份AOF文件
backup_aof() {
    log "INFO" "开始备份AOF文件..."
    
    # 检查AOF是否启用
    local redis_cmd="$REDIS_CLI"
    [[ -n "$REDIS_HOST" ]] && redis_cmd="$redis_cmd -h $REDIS_HOST"
    [[ -n "$REDIS_PORT" ]] && redis_cmd="$redis_cmd -p $REDIS_PORT"
    [[ -n "$REDIS_PASSWORD" ]] && redis_cmd="$redis_cmd -a $REDIS_PASSWORD"
    
    local appendonly=$($redis_cmd config get appendonly 2>/dev/null | grep -v "^appendonly$" | head -1)
    
    if [[ "$appendonly" != "yes" ]]; then
        log "WARN" "AOF未启用，跳过AOF备份"
        return
    fi
    
    # 获取AOF文件名
    local appendfilename=$($redis_cmd config get appendfilename 2>/dev/null | grep -v "^appendfilename$" | head -1)
    appendfilename="${appendfilename:-appendonly.aof}"
    
    local aof_file="$REDIS_DATA_DIR/$appendfilename"
    
    if [[ ! -f "$aof_file" ]]; then
        error_exit "AOF文件不存在: $aof_file"
    fi
    
    # 执行BGREWRITEAOF
    log "INFO" "执行BGREWRITEAOF..."
    if ! $redis_cmd bgrewriteaof 2>/dev/null | grep -q "Background append only file rewriting started"; then
        log "WARN" "BGREWRITEAOF触发失败，备份当前AOF文件"
    else
        # 等待BGREWRITEAOF完成
        local max_wait=120
        local wait_interval=5
        local waited=0
        
        log "INFO" "等待BGREWRITEAOF完成..."
        while [[ $waited -lt $max_wait ]]; do
            local aof_status=$($redis_cmd info persistence 2>/dev/null | grep "^aof_rewrite_in_progress:" | cut -d: -f2)
            
            if [[ "$aof_status" == "0" ]]; then
                log "INFO" "BGREWRITEAOF完成"
                break
            fi
            
            log "INFO" "BGREWRITEAOF进行中... 等待${wait_interval}秒"
            sleep $wait_interval
            waited=$((waited + wait_interval))
        done
    fi
    
    # 复制AOF文件到备份目录
    local backup_filename="redis_aof_${TIMESTAMP}.aof"
    local backup_path="$AOF_DIR/$backup_filename"
    
    log "INFO" "复制AOF文件: $aof_file -> $backup_path"
    cp "$aof_file" "$backup_path"
    
    # 压缩备份文件
    log "INFO" "压缩AOF备份文件..."
    local compressed_file="${backup_path}.gz"
    if ! gzip -c -$COMPRESS_level "$backup_path" > "$compressed_file"; then
        error_exit "AOF文件压缩失败"
    fi
    
    # 删除未压缩的副本
    rm -f "$backup_path"
    
    # 验证备份文件
    validate_backup_file "$compressed_file"
    
    log "INFO" "AOF备份完成: $compressed_file"
    echo "$compressed_file"
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
    if [[ $file_size -lt 1024 ]]; then  # 小于1KB视为无效
        error_exit "备份文件过小(${file_size}字节)，可能备份失败"
    fi
    
    # 验证gzip文件完整性
    if ! gzip -t "$backup_file" 2>/dev/null; then
        error_exit "备份文件gzip压缩损坏"
    fi
    
    log "INFO" "备份文件验证通过，大小: $(numfmt --to=iec-i $file_size)"
    
    # 生成checksum
    md5sum "$backup_file" > "$backup_file.md5"
}

# 清理旧备份
cleanup_old_backups() {
    log "INFO" "清理超过 $RETENTION_DAYS 天的旧备份..."
    
    # 清理RDB备份
    find "$RDB_DIR" -name "*.rdb.gz" -mtime +$RETENTION_DAYS -type f -delete 2>/dev/null || true
    
    # 清理AOF备份
    find "$AOF_DIR" -name "*.aof.gz" -mtime +$RETENTION_DAYS -type f -delete 2>/dev/null || true
    
    log "INFO" "旧备份清理完成"
}

# 生成备份报告
generate_report() {
    local rdb_backup="$1"
    local aof_backup="$2"
    local report_file="$LOG_DIR/redis-backup-report-${TIMESTAMP}.json"
    
    cat > "$report_file" << EOF
{
  "redis_backup_report": {
    "timestamp": "$TIMESTAMP",
    "hostname": "$HOSTNAME",
    "type": "$BACKUP_TYPE",
    "status": "success",
    "backup_files": {
EOF
    
    # RDB备份信息
    if [[ -n "$rdb_backup" ]] && [[ -f "$rdb_backup" ]]; then
        local rdb_size=$(stat -c%s "$rdb_backup" 2>/dev/null || echo "0")
        local rdb_md5=$(cat "$rdb_backup.md5" 2>/dev/null | cut -d' ' -f1 || echo "")
        
        cat >> "$report_file" << EOF
      "rdb": {
        "filename": "$(basename "$rdb_backup")",
        "path": "$rdb_backup",
        "size_bytes": $rdb_size,
        "size_human": "$(numfmt --to=iec-i $rdb_size 2>/dev/null || echo "N/A")",
        "md5": "$rdb_md5"
      }
EOF
        
        if [[ -n "$aof_backup" ]] && [[ -f "$aof_backup" ]]; then
            echo "      ," >> "$report_file"
        fi
    fi
    
    # AOF备份信息
    if [[ -n "$aof_backup" ]] && [[ -f "$aof_backup" ]]; then
        local aof_size=$(stat -c%s "$aof_backup" 2>/dev/null || echo "0")
        local aof_md5=$(cat "$aof_backup.md5" 2>/dev/null | cut -d' ' -f1 || echo "")
        
        cat >> "$report_file" << EOF
      "aof": {
        "filename": "$(basename "$aof_backup")",
        "path": "$aof_backup",
        "size_bytes": $aof_size,
        "size_human": "$(numfmt --to=iec-i $aof_size 2>/dev/null || echo "N/A")",
        "md5": "$aof_md5"
      }
EOF
    fi
    
    cat >> "$report_file" << EOF
    },
    "retention_days": $RETENTION_DAYS,
    "compression_level": $COMPRESS_LEVEL,
    "duration_seconds": $SECONDS
  }
}
EOF
    
    log "INFO" "Redis备份报告生成: $report_file"
}

# 主函数
main() {
    log "INFO" "================================================"
    log "INFO" "Redis 数据备份开始"
    log "INFO" "主机: $HOSTNAME"
    log "INFO" "备份类型: $BACKUP_TYPE"
    log "INFO" "时间: $TIMESTAMP"
    log "INFO" "================================================"
    
    # 记录开始时间
    local start_time=$SECONDS
    local rdb_backup=""
    local aof_backup=""
    
    # 检查Redis状态
    check_redis_running
    get_redis_config
    
    # 执行备份
    case "$BACKUP_TYPE" in
        rdb)
            rdb_backup=$(backup_rdb)
            ;;
        aof)
            aof_backup=$(backup_aof)
            ;;
        both)
            rdb_backup=$(backup_rdb)
            aof_backup=$(backup_aof)
            ;;
        *)
            error_exit "未知的备份类型: $BACKUP_TYPE (支持: rdb, aof, both)"
            ;;
    esac
    
    # 清理旧备份
    cleanup_old_backups
    
    # 计算耗时
    local duration=$((SECONDS - start_time))
    
    # 生成报告
    generate_report "$rdb_backup" "$aof_backup"
    
    log "INFO" "================================================"
    log "INFO" "Redis 数据备份完成"
    log "INFO" "总耗时: ${duration} 秒"
    
    if [[ -n "$rdb_backup" ]]; then
        log "INFO" "RDB备份文件: $rdb_backup"
    fi
    
    if [[ -n "$aof_backup" ]]; then
        log "INFO" "AOF备份文件: $aof_backup"
    fi
    
    log "INFO" "================================================"
    
    return 0
}

# 执行主函数
main "$@"