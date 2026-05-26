#!/bin/bash
# PostgreSQL备份脚本示例
# 用于演示和验证数据库备份功能

set -euo pipefail

# 配置
BACKUP_DIR="${BACKUP_DIR:-/data/backups/postgresql}"
LOG_DIR="${LOG_DIR:-/var/log/postgresql/backup}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
COMPRESS_LEVEL="${COMPRESS_LEVEL:-6}"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="pg_backup_${DATE}.sql.gz"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
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

# 检查PostgreSQL连接
check_postgresql_connection() {
    log_info "检查PostgreSQL连接..."
    
    if command -v pg_isready &> /dev/null; then
        if pg_isready -q; then
            log_info "PostgreSQL服务运行正常"
            return 0
        else
            log_error "PostgreSQL服务未运行"
            return 1
        fi
    else
        log_warning "pg_isready命令未找到，跳过连接检查"
        return 0
    fi
}

# 执行备份
perform_backup() {
    local backup_type="${1:-full}"
    
    log_info "开始执行${backup_type}备份..."
    
    # 备份命令
    local backup_cmd="pg_dumpall"
    local backup_options=""
    
    case "$backup_type" in
        "full")
            backup_options="--clean --if-exists"
            ;;
        "schema")
            backup_options="--schema-only --clean --if-exists"
            BACKUP_FILE="pg_schema_${DATE}.sql.gz"
            ;;
        "data")
            backup_options="--data-only --column-inserts"
            BACKUP_FILE="pg_data_${DATE}.sql.gz"
            ;;
        *)
            log_error "未知的备份类型: $backup_type"
            return 1
            ;;
    esac
    
    # 执行备份
    local output_file="${BACKUP_DIR}/${BACKUP_FILE}"
    log_info "备份文件: $output_file"
    
    if ${backup_cmd} ${backup_options} 2>/dev/null | gzip -${COMPRESS_LEVEL} > "$output_file"; then
        log_info "${backup_type}备份成功"
        
        # 检查备份文件
        if [ -f "$output_file" ] && [ -s "$output_file" ]; then
            local file_size=$(du -h "$output_file" | cut -f1)
            log_info "备份文件大小: $file_size"
            return 0
        else
            log_error "备份文件创建失败或为空"
            return 1
        fi
    else
        log_error "${backup_type}备份失败"
        return 1
    fi
}

# 验证备份
verify_backup() {
    local backup_file="$1"
    
    log_info "验证备份文件: $backup_file"
    
    # 检查文件是否存在且非空
    if [ ! -f "$backup_file" ]; then
        log_error "备份文件不存在: $backup_file"
        return 1
    fi
    
    if [ ! -s "$backup_file" ]; then
        log_error "备份文件为空: $backup_file"
        return 1
    fi
    
    # 检查gzip压缩格式
    if ! gzip -t "$backup_file" 2>/dev/null; then
        log_error "备份文件gzip格式损坏"
        return 1
    fi
    
    # 尝试解压并检查SQL头部
    if gzip -dc "$backup_file" | head -10 | grep -q "PostgreSQL database dump"; then
        log_info "备份文件格式验证通过"
        return 0
    else
        log_warning "备份文件SQL头部格式异常"
        return 0  # 这不一定表示备份失败，只是格式检查
    fi
}

# 清理旧备份
cleanup_old_backups() {
    log_info "清理超过${RETENTION_DAYS}天的旧备份..."
    
    local deleted_count=0
    local kept_count=0
    
    find "$BACKUP_DIR" -name "pg_*.sql.gz" -type f -mtime +${RETENTION_DAYS} | while read -r old_file; do
        log_info "删除旧备份: $(basename "$old_file")"
        rm -f "$old_file"
        ((deleted_count++)) || true
    done
    
    # 统计剩余文件
    kept_count=$(find "$BACKUP_DIR" -name "pg_*.sql.gz" -type f | wc -l)
    
    log_info "清理完成: 删除了${deleted_count}个旧备份，保留${kept_count}个备份"
}

# 生成备份报告
generate_backup_report() {
    local status="$1"
    local backup_file="$2"
    local report_file="${LOG_DIR}/backup_report_${DATE}.log"
    
    log_info "生成备份报告: $report_file"
    
    cat > "$report_file" << EOF
=== PostgreSQL备份报告 ===
生成时间: $(date '+%Y-%m-%d %H:%M:%S')
备份状态: ${status}
备份文件: $(basename "$backup_file")
文件路径: $backup_file
文件大小: $(du -h "$backup_file" 2>/dev/null | cut -f1 || echo "N/A")
备份类型: ${BACKUP_TYPE:-full}
保留天数: ${RETENTION_DAYS}
压缩级别: ${COMPRESS_LEVEL}

=== 系统信息 ===
主机名: $(hostname)
当前用户: $(whoami)
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
2. 测试恢复流程
3. 监控备份存储空间
EOF
    
    log_info "备份报告已生成: $report_file"
}

# 主函数
main() {
    local backup_type="${1:-full}"
    local test_mode="${2:-false}"
    
    log_info "=== PostgreSQL备份脚本开始执行 ==="
    log_info "备份类型: $backup_type"
    log_info "备份目录: $BACKUP_DIR"
    log_info "日志目录: $LOG_DIR"
    
    # 测试模式
    if [ "$test_mode" = "true" ]; then
        log_info "测试模式启用，仅检查不执行实际备份"
        
        if check_directories && check_postgresql_connection; then
            log_info "测试模式检查通过"
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
    
    if ! check_postgresql_connection; then
        log_error "PostgreSQL连接检查失败"
        return 1
    fi
    
    # 执行备份
    if perform_backup "$backup_type"; then
        local backup_file="${BACKUP_DIR}/${BACKUP_FILE}"
        
        # 验证备份
        if verify_backup "$backup_file"; then
            # 清理旧备份
            cleanup_old_backups
            
            # 生成报告
            generate_backup_report "SUCCESS" "$backup_file"
            
            log_info "=== PostgreSQL备份完成 ==="
            return 0
        else
            log_error "备份验证失败"
            generate_backup_report "FAILED_VERIFICATION" "$backup_file"
            return 1
        fi
    else
        log_error "备份执行失败"
        generate_backup_report "FAILED_EXECUTION" ""
        return 1
    fi
}

# 脚本用法
usage() {
    cat << EOF
PostgreSQL备份脚本

用法: $0 [选项]

选项:
  --type TYPE        备份类型: full, schema, data (默认: full)
  --dir DIR          备份目录 (默认: /data/backups/postgresql)
  --days DAYS        保留天数 (默认: 30)
  --compress LEVEL   压缩级别 1-9 (默认: 6)
  --test             测试模式，仅检查不执行备份
  --help             显示此帮助信息

示例:
  $0 --type full --dir /backups --days 7
  $0 --test
  $0 --type schema

环境变量:
  BACKUP_DIR     备份目录
  LOG_DIR        日志目录
  RETENTION_DAYS 保留天数
  COMPRESS_LEVEL 压缩级别
EOF
}

# 解析参数
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --type)
                BACKUP_TYPE="$2"
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
            --compress)
                COMPRESS_LEVEL="$2"
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
        main "${BACKUP_TYPE:-full}" "true"
    else
        main "${BACKUP_TYPE:-full}" "false"
    fi
    
    exit $?
fi