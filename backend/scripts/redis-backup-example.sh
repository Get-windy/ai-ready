#!/bin/bash
# Redis备份脚本示例
# 用于演示和验证Redis缓存备份功能

set -euo pipefail

# 配置
REDIS_HOST="${REDIS_HOST:-localhost}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
BACKUP_DIR="${BACKUP_DIR:-/data/backups/redis}"
LOG_DIR="${LOG_DIR:-/var/log/redis/backup}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
DATE=$(date +%Y%m%d_%H%M%S)
RDB_BACKUP_FILE="redis_rdb_${DATE}.rdb"
AOF_BACKUP_FILE="redis_aof_${DATE}.aof"
CONFIG_BACKUP_FILE="redis_config_${DATE}.conf"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warning() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_debug() {
    echo -e "${BLUE}[DEBUG]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# 检查目录
check_directories() {
    log_info "检查目录..."
    
    for dir in "$BACKUP_DIR" "$LOG_DIR"; do
        if [ ! -d "$dir" ]; then
            log_warning "目录不存在: $dir，尝试创建..."
            mkdir -p "$dir" || {
                log_error "创建目录失败: $dir"
                return 1
            }
            log_info "目录创建成功: $dir"
        fi
    done
    
    return 0
}

# 检查Redis连接
check_redis_connection() {
    log_info "检查Redis连接..."
    
    local redis_cmd="redis-cli"
    local connection_options="-h $REDIS_HOST -p $REDIS_PORT"
    
    # 添加密码
    if [ -n "$REDIS_PASSWORD" ]; then
        connection_options="$connection_options -a $REDIS_PASSWORD"
    fi
    
    # 检查redis-cli命令
    if ! command -v redis-cli &> /dev/null; then
        log_error "redis-cli命令未找到"
        return 1
    fi
    
    # 测试连接
    if redis-cli $connection_options ping 2>/dev/null | grep -q "PONG"; then
        log_info "Redis连接成功"
        return 0
    else
        log_error "Redis连接失败"
        return 1
    fi
}

# 获取Redis信息
get_redis_info() {
    local redis_cmd="redis-cli"
    local connection_options="-h $REDIS_HOST -p $REDIS_PORT"
    
    if [ -n "$REDIS_PASSWORD" ]; then
        connection_options="$connection_options -a $REDIS_PASSWORD"
    fi
    
    log_info "获取Redis信息..."
    
    # 获取基本信息
    local info_output
    info_output=$(redis-cli $connection_options info 2>/dev/null)
    
    if [ $? -eq 0 ]; then
        echo "$info_output" | grep -E "(redis_version|used_memory|connected_clients|rdb_last_save_time|aof_enabled)"
    else
        log_error "获取Redis信息失败"
        return 1
    fi
}

# 备份RDB文件
backup_rdb() {
    log_info "开始备份RDB文件..."
    
    # 获取RDB文件路径
    local rdb_path
    rdb_path=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT ${REDIS_PASSWORD:+-a $REDIS_PASSWORD} config get dir 2>/dev/null | tail -1)
    
    if [ -z "$rdb_path" ]; then
        log_error "无法获取RDB文件路径"
        return 1
    fi
    
    rdb_path="${rdb_path}/dump.rdb"
    
    # 检查RDB文件是否存在
    if [ ! -f "$rdb_path" ]; then
        log_warning "RDB文件不存在: $rdb_path"
        log_info "尝试触发BGSAVE..."
        
        if redis-cli -h $REDIS_HOST -p $REDIS_PORT ${REDIS_PASSWORD:+-a $REDIS_PASSWORD} bgsave 2>/dev/null | grep -q "Background saving started"; then
            log_info "BGSAVE已启动，等待完成..."
            sleep 5
            
            # 等待BGSAVE完成
            local max_wait=30
            local wait_count=0
            
            while [ $wait_count -lt $max_wait ]; do
                if redis-cli -h $REDIS_HOST -p $REDIS_PORT ${REDIS_PASSWORD:+-a $REDIS_PASSWORD} info persistence 2>/dev/null | grep -q "rdb_bgsave_in_progress:0"; then
                    log_info "BGSAVE完成"
                    break
                fi
                sleep 1
                ((wait_count++))
            done
            
            if [ $wait_count -ge $max_wait ]; then
                log_error "BGSAVE超时"
                return 1
            fi
        else
            log_error "BGSAVE启动失败"
            return 1
        fi
    fi
    
    # 再次检查RDB文件
    if [ ! -f "$rdb_path" ]; then
        log_error "RDB文件仍然不存在: $rdb_path"
        return 1
    fi
    
    # 备份RDB文件
    local backup_path="${BACKUP_DIR}/${RDB_BACKUP_FILE}"
    
    if cp "$rdb_path" "$backup_path"; then
        log_info "RDB备份成功: $(basename "$backup_path")"
        
        # 检查文件
        if [ -f "$backup_path" ] && [ -s "$backup_path" ]; then
            local file_size=$(du -h "$backup_path" | cut -f1)
            log_info "RDB文件大小: $file_size"
            return 0
        else
            log_error "RDB备份文件创建失败或为空"
            return 1
        fi
    else
        log_error "RDB备份失败"
        return 1
    fi
}

# 备份AOF文件
backup_aof() {
    log_info "检查AOF配置..."
    
    # 检查AOF是否启用
    local aof_enabled
    aof_enabled=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT ${REDIS_PASSWORD:+-a $REDIS_PASSWORD} config get appendonly 2>/dev/null | tail -1)
    
    if [ "$aof_enabled" != "yes" ]; then
        log_info "AOF未启用，跳过AOF备份"
        return 0
    fi
    
    log_info "开始备份AOF文件..."
    
    # 获取AOF文件路径
    local aof_path
    aof_path=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT ${REDIS_PASSWORD:+-a $REDIS_PASSWORD} config get appendfilename 2>/dev/null | tail -1)
    
    if [ -z "$aof_path" ]; then
        aof_path="appendonly.aof"
    fi
    
    local dir_path
    dir_path=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT ${REDIS_PASSWORD:+-a $REDIS_PASSWORD} config get dir 2>/dev/null | tail -1)
    
    aof_path="${dir_path}/${aof_path}"
    
    # 检查AOF文件是否存在
    if [ ! -f "$aof_path" ]; then
        log_warning "AOF文件不存在: $aof_path"
        return 0
    fi
    
    # 备份AOF文件
    local backup_path="${BACKUP_DIR}/${AOF_BACKUP_FILE}"
    
    if cp "$aof_path" "$backup_path"; then
        log_info "AOF备份成功: $(basename "$backup_path")"
        
        # 检查文件
        if [ -f "$backup_path" ] && [ -s "$backup_path" ]; then
            local file_size=$(du -h "$backup_path" | cut -f1)
            log_info "AOF文件大小: $file_size"
            return 0
        else
            log_warning "AOF备份文件创建失败或为空"
            return 1
        fi
    else
        log_error "AOF备份失败"
        return 1
    fi
}

# 备份Redis配置
backup_config() {
    log_info "开始备份Redis配置..."
    
    # 获取所有配置
    local config_output
    config_output=$(redis-cli -h $REDIS_HOST -p $REDIS_PORT ${REDIS_PASSWORD:+-a $REDIS_PASSWORD} config get "*" 2>/dev/null)
    
    if [ $? -eq 0 ] && [ -n "$config_output" ]; then
        local backup_path="${BACKUP_DIR}/${CONFIG_BACKUP_FILE}"
        
        echo "$config_output" > "$backup_path"
        
        if [ -f "$backup_path" ] && [ -s "$backup_path" ]; then
            log_info "配置备份成功: $(basename "$backup_path")"
            return 0
        else
            log_error "配置备份文件创建失败"
            return 1
        fi
    else
        log_error "获取Redis配置失败"
        return 1
    fi
}

# 验证备份
verify_backup() {
    local backup_file="$1"
    local backup_type="$2"
    
    log_info "验证${backup_type}备份文件: $(basename "$backup_file")"
    
    # 检查文件是否存在且非空
    if [ ! -f "$backup_file" ]; then
        log_error "备份文件不存在: $backup_file"
        return 1
    fi
    
    if [ ! -s "$backup_file" ]; then
        log_error "备份文件为空: $backup_file"
        return 1
    fi
    
    # 根据备份类型进行验证
    case "$backup_type" in
        "RDB")
            # 检查RDB文件头部魔数
            if head -c 5 "$backup_file" | xxd | grep -q "5245444953"; then
                log_info "RDB文件格式验证通过 (REDIS魔数)"
                return 0
            else
                log_warning "RDB文件魔数验证失败"
                return 1
            fi
            ;;
        "AOF")
            # 检查AOF文件头部
            if head -n 1 "$backup_file" | grep -q "^\\*"; then
                log_info "AOF文件格式验证通过 (Redis协议格式)"
                return 0
            else
                log_warning "AOF文件格式验证失败"
                return 1
            fi
            ;;
        "CONFIG")
            # 检查配置文件
            if grep -q "^[a-zA-Z]" "$backup_file"; then
                log_info "配置文件验证通过"
                return 0
            else
                log_warning "配置文件格式异常"
                return 1
            fi
            ;;
        *)
            log_warning "未知备份类型: $backup_type，跳过详细验证"
            return 0
            ;;
    esac
}

# 清理旧备份
cleanup_old_backups() {
    log_info "清理超过${RETENTION_DAYS}天的旧备份..."
    
    local deleted_files=0
    
    # 清理RDB备份
    find "$BACKUP_DIR" -name "redis_rdb_*.rdb" -type f -mtime +${RETENTION_DAYS} | while read -r old_file; do
        log_info "删除旧RDB备份: $(basename "$old_file")"
        rm -f "$old_file"
        ((deleted_files++)) || true
    done
    
    # 清理AOF备份
    find "$BACKUP_DIR" -name "redis_aof_*.aof" -type f -mtime +${RETENTION_DAYS} | while read -r old_file; do
        log_info "删除旧AOF备份: $(basename "$old_file")"
        rm -f "$old_file"
        ((deleted_files++)) || true
    done
    
    # 清理配置备份
    find "$BACKUP_DIR" -name "redis_config_*.conf" -type f -mtime +${RETENTION_DAYS} | while read -r old_file; do
        log_info "删除旧配置备份: $(basename "$old_file")"
        rm -f "$old_file"
        ((deleted_files++)) || true
    done
    
    # 统计剩余文件
    local remaining_files
    remaining_files=$(find "$BACKUP_DIR" -name "redis_*" -type f | wc -l)
    
    log_info "清理完成: 删除了${deleted_files}个旧备份，保留${remaining_files}个备份"
}

# 生成备份报告
generate_backup_report() {
    local status="$1"
    local report_file="${LOG_DIR}/redis_backup_report_${DATE}.log"
    
    log_info "生成备份报告: $report_file"
    
    # 收集信息
    local redis_info=""
    if command -v redis-cli &> /dev/null; then
        redis_info=$(get_redis_info 2>/dev/null || echo "无法获取Redis信息")
    fi
    
    # 统计备份文件
    local rdb_backup=""
    local aof_backup=""
    local config_backup=""
    
    if [ -f "${BACKUP_DIR}/${RDB_BACKUP_FILE}" ]; then
        rdb_backup="${BACKUP_DIR}/${RDB_BACKUP_FILE} ($(du -h "${BACKUP_DIR}/${RDB_BACKUP_FILE}" 2>/dev/null | cut -f1 || echo "N/A"))"
    fi
    
    if [ -f "${BACKUP_DIR}/${AOF_BACKUP_FILE}" ]; then
        aof_backup="${BACKUP_DIR}/${AOF_BACKUP_FILE} ($(du -h "${BACKUP_DIR}/${AOF_BACKUP_FILE}" 2>/dev/null | cut -f1 || echo "N/A"))"
    fi
    
    if [ -f "${BACKUP_DIR}/${CONFIG_BACKUP_FILE}" ]; then
        config_backup="${BACKUP_DIR}/${CONFIG_BACKUP_FILE} ($(du -h "${BACKUP_DIR}/${CONFIG_BACKUP_FILE}" 2>/dev/null | cut -f1 || echo "N/A"))"
    fi
    
    cat > "$report_file" << EOF
=== Redis备份报告 ===
生成时间: $(date '+%Y-%m-%d %H:%M:%S')
备份状态: ${status}
备份类型: 全量 (RDB + AOF + 配置)
保留天数: ${RETENTION_DAYS}

=== 备份文件 ===
RDB备份: ${rdb_backup:-未生成}
AOF备份: ${aof_backup:-未生成}
配置备份: ${config_backup:-未生成}

=== Redis信息 ===
${redis_info:-未获取到Redis信息}

=== 系统信息 ===
主机名: $(hostname)
当前用户: $(whoami)
Redis主机: ${REDIS_HOST}:${REDIS_PORT}
脚本版本: 1.0

=== 验证结果 ===
$(if [ "$status" = "SUCCESS" ]; then
    echo "✓ 备份成功"
    echo "✓ 文件验证通过"
    echo "✓ 清理作业完成"
else
    echo "✗ 备份失败"
    echo "请查看错误日志"
fi)

=== 下一步 ===
1. 定期检查备份文件完整性
2. 测试Redis恢复流程
3. 监控备份存储空间
4. 验证备份恢复时间目标(RTO)
EOF
    
    log_info "备份报告已生成: $report_file"
}

# 主函数
main() {
    local test_mode="${1:-false}"
    
    log_info "=== Redis备份脚本开始执行 ==="
    log_info "Redis主机: ${REDIS_HOST}:${REDIS_PORT}"
    log_info "备份目录: $BACKUP_DIR"
    log_info "日志目录: $LOG_DIR"
    
    # 测试模式
    if [ "$test_mode" = "true" ]; then
        log_info "测试模式启用，仅检查不执行实际备份"
        
        if check_directories && check_redis_connection; then
            log_info "测试模式检查通过"
            get_redis_info
            return 0
        else
            log_error "测试模式检查失败"
            return 1
        fi
    fi
    
    # 实际备份流程
    if ! check_directories; then
        log_error "目录检查失败"
        return 1
    fi
    
    if ! check_redis_connection; then
        log_error "Redis连接检查失败"
        return 1
    fi
    
    # 显示Redis信息
    get_redis_info
    
    # 执行备份
    local backup_success=true
    local backup_errors=()
    
    # 备份RDB
    if backup_rdb; then
        verify_backup "${BACKUP_DIR}/${RDB_BACKUP_FILE}" "RDB"
    else
        backup_success=false
        backup_errors+=("RDB备份失败")
    fi
    
    # 备份AOF
    if backup_aof; then
        if [ -f "${BACKUP_DIR}/${AOF_BACKUP_FILE}" ]; then
            verify_backup "${BACKUP_DIR}/${AOF_BACKUP_FILE}" "AOF"
        fi
    else
        backup_success=false
        backup_errors+=("AOF备份失败")
    fi
    
    # 备份配置
    if backup_config; then
        verify_backup "${BACKUP_DIR}/${CONFIG_BACKUP_FILE}" "CONFIG"
    else
        log_warning "配置备份失败，继续执行"
    fi
    
    # 清理旧备份
    cleanup_old_backups
    
    # 生成报告
    if [ "$backup_success" = true ]; then
        generate_backup_report "SUCCESS"
        log_info "=== Redis备份完成 ==="
        return 0
    else
        generate_backup_report "FAILED"
        log_error "备份过程中出现错误:"
        for error in "${backup_errors[@]}"; do
            log_error "  - $error"
        done
        return 1
    fi
}

# 脚本用法
usage() {
    cat << EOF
Redis备份脚本

用法: $0 [选项]

选项:
  --host HOST        Redis主机 (默认: localhost)
  --port PORT        Redis端口 (默认: 6379)
  --password PASS    Redis密码 (默认: 空)
  --dir DIR         备份目录 (默认: /data/backups/redis)
  --days DAYS       保留天数 (默认: 7)
  --test            测试模式，仅检查不执行备份
  --help            显示此帮助信息

示例:
  $0 --host redis.example.com --port 6379
  $0 --password secret --dir /backups/redis
  $0 --test

环境变量:
  REDIS_HOST     Redis主机
  REDIS_PORT     Redis端口
  REDIS_PASSWORD Redis密码
  BACKUP_DIR     备份目录
  LOG_DIR        日志目录
  RETENTION_DAYS 保留天数
EOF
}

# 解析参数
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --host)
                REDIS_HOST="$2"
                shift 2
                ;;
            --port)
                REDIS_PORT="$2"
                shift 2
                ;;
            --password)
                REDIS_PASSWORD="$2"
                shift 2
                ;;
            --dir)
                BACKUP_DIR="$2"
                shift 2
                ;;
            --days)
                RETENTION_DAYS="$2"
                shift 2
                ;;
            --test)
                TEST_MODE="true"
                shift
                ;;
            --help)
                usage
                exit 0
                ;;
            *)
                log_error "未知参数: $1"
                usage
                exit 1
                ;;
        esac
    done
}

# 入口点
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    parse_args "$@"
    
    if [ "${TEST_MODE:-false}" = "true" ]; then
        main "true"
    else
        main "false"
    fi
    
    exit $?
fi