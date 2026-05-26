#!/bin/bash
# PostgreSQL 备份脚本
# 版本: 1.0
# 作者: 运维工程师 (devops-engineer)
# 最后更新: 2026-05-01

set -e

# 配置变量
BACKUP_DIR="/backup/postgresql"
LOG_DIR="/var/log/backup"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_NAME="postgresql_backup_${TIMESTAMP}"
RETENTION_DAYS=30

# ERP 数据库列表
ERP_DATABASES=(
    "erp_core"
    "erp_purchase"
    "erp_sales"
    "erp_inventory"
    "erp_finance"
    "erp_supplier"
)

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
    echo "[INFO] $(date '+%Y-%m-%d %H:%M:%S') - $1" >> "${LOG_DIR}/backup_${TIMESTAMP}.log"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
    echo "[WARNING] $(date '+%Y-%m-%d %H:%M:%S') - $1" >> "${LOG_DIR}/backup_${TIMESTAMP}.log"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
    echo "[ERROR] $(date '+%Y-%m-%d %H:%M:%S') - $1" >> "${LOG_DIR}/backup_${TIMESTAMP}.log"
}

# 检查依赖
check_dependencies() {
    log_info "检查依赖工具..."
    
    if ! command -v pg_dump &> /dev/null; then
        log_error "pg_dump 命令未找到，请安装 PostgreSQL 客户端"
        exit 1
    fi
    
    if ! command -v pg_dumpall &> /dev/null; then
        log_error "pg_dumpall 命令未找到"
        exit 1
    fi
    
    if ! command -v gzip &> /dev/null; then
        log_error "gzip 命令未找到"
        exit 1
    fi
    
    log_info "所有依赖工具检查通过"
}

# 检查目录
check_directories() {
    log_info "检查备份目录..."
    
    if [ ! -d "$BACKUP_DIR" ]; then
        log_warning "备份目录不存在，正在创建: $BACKUP_DIR"
        mkdir -p "$BACKUP_DIR"
        chmod 750 "$BACKUP_DIR"
    fi
    
    if [ ! -d "$LOG_DIR" ]; then
        log_warning "日志目录不存在，正在创建: $LOG_DIR"
        mkdir -p "$LOG_DIR"
        chmod 750 "$LOG_DIR"
    fi
    
    # 创建本次备份的子目录
    mkdir -p "${BACKUP_DIR}/${BACKUP_NAME}"
    mkdir -p "${BACKUP_DIR}/${BACKUP_NAME}/full"
    mkdir -p "${BACKUP_DIR}/${BACKUP_NAME}/incremental"
    mkdir -p "${BACKUP_DIR}/${BACKUP_NAME}/config"
    
    log_info "目录检查完成"
}

# 备份 PostgreSQL 配置
backup_config() {
    log_info "开始备份 PostgreSQL 配置..."
    
    CONFIG_DIR="${BACKUP_DIR}/${BACKUP_NAME}/config"
    
    # 备份 postgresql.conf
    if [ -f "/var/lib/postgresql/data/postgresql.conf" ]; then
        cp "/var/lib/postgresql/data/postgresql.conf" "${CONFIG_DIR}/postgresql.conf"
        log_info "已备份 postgresql.conf"
    fi
    
    # 备份 pg_hba.conf
    if [ -f "/var/lib/postgresql/data/pg_hba.conf" ]; then
        cp "/var/lib/postgresql/data/pg_hba.conf" "${CONFIG_DIR}/pg_hba.conf"
        log_info "已备份 pg_hba.conf"
    fi
    
    # 备份扩展配置
    psql -U postgres -d postgres -c "SELECT * FROM pg_extension;" > "${CONFIG_DIR}/extensions.txt" 2>/dev/null || true
    log_info "已备份扩展信息"
    
    # 备份用户和角色
    psql -U postgres -d postgres -c "\du" > "${CONFIG_DIR}/users_roles.txt" 2>/dev/null || true
    log_info "已备份用户和角色信息"
    
    log_info "PostgreSQL 配置备份完成"
}

# 备份单个数据库
backup_database() {
    local db_name="$1"
    local backup_type="$2"  # full 或 incremental
    
    log_info "开始备份数据库: $db_name (类型: $backup_type)"
    
    local backup_file="${BACKUP_DIR}/${BACKUP_NAME}/${backup_type}/${db_name}_${TIMESTAMP}.sql.gz"
    
    # 设置备份参数
    local backup_options="--clean --if-exists --create --no-password"
    
    if [ "$backup_type" = "full" ]; then
        backup_options="$backup_options --schema-only"
    fi
    
    # 执行备份
    if pg_dump -U postgres "$db_name" $backup_options | gzip > "$backup_file"; then
        local file_size=$(du -h "$backup_file" | cut -f1)
        log_info "数据库 $db_name 备份成功: $backup_file ($file_size)"
        
        # 验证备份文件
        if gzip -t "$backup_file"; then
            log_info "备份文件验证通过"
        else
            log_error "备份文件验证失败: $backup_file"
            return 1
        fi
    else
        log_error "数据库 $db_name 备份失败"
        return 1
    fi
    
    return 0
}

# 备份所有数据库
backup_all_databases() {
    log_info "开始备份所有 ERP 数据库..."
    
    local success_count=0
    local failure_count=0
    
    for db in "${ERP_DATABASES[@]}"; do
        if backup_database "$db" "full"; then
            success_count=$((success_count + 1))
        else
            failure_count=$((failure_count + 1))
        fi
    done
    
    log_info "数据库备份完成: 成功 $success_count 个，失败 $failure_count 个"
    
    if [ $failure_count -gt 0 ]; then
        return 1
    fi
    
    return 0
}

# 备份全局对象
backup_global_objects() {
    log_info "开始备份全局对象..."
    
    local global_file="${BACKUP_DIR}/${BACKUP_NAME}/full/global_objects_${TIMESTAMP}.sql.gz"
    
    # 备份全局对象（角色、表空间等）
    if pg_dumpall -U postgres --globals-only --no-password | gzip > "$global_file"; then
        local file_size=$(du -h "$global_file" | cut -f1)
        log_info "全局对象备份成功: $global_file ($file_size)"
    else
        log_error "全局对象备份失败"
        return 1
    fi
    
    return 0
}

# 清理旧备份
cleanup_old_backups() {
    log_info "开始清理超过 ${RETENTION_DAYS} 天的旧备份..."
    
    local deleted_count=0
    local backup_dirs=($(find "$BACKUP_DIR" -maxdepth 1 -type d -name "postgresql_backup_*" -mtime +$RETENTION_DAYS))
    
    for dir in "${backup_dirs[@]}"; do
        if [ -d "$dir" ]; then
            log_info "删除旧备份: $dir"
            rm -rf "$dir"
            deleted_count=$((deleted_count + 1))
        fi
    done
    
    # 清理旧日志
    local old_logs=($(find "$LOG_DIR" -name "backup_*.log" -mtime +$RETENTION_DAYS))
    for log in "${old_logs[@]}"; do
        rm -f "$log"
    done
    
    log_info "清理完成: 删除了 $deleted_count 个旧备份"
}

# 生成备份报告
generate_backup_report() {
    log_info "生成备份报告..."
    
    local report_file="${LOG_DIR}/backup_report_${TIMESTAMP}.txt"
    
    cat > "$report_file" << EOF
PostgreSQL 备份报告
===================

备份时间: $(date '+%Y-%m-%d %H:%M:%S')
备份名称: $BACKUP_NAME
备份类型: 全量备份

备份目录: $BACKUP_DIR/$BACKUP_NAME
日志目录: $LOG_DIR

数据库备份情况:
EOF
    
    # 统计备份文件
    for db in "${ERP_DATABASES[@]}"; do
        local backup_file="${BACKUP_DIR}/${BACKUP_NAME}/full/${db}_${TIMESTAMP}.sql.gz"
        if [ -f "$backup_file" ]; then
            local file_size=$(du -h "$backup_file" | cut -f1)
            echo "- $db: 成功 ($file_size)" >> "$report_file"
        else
            echo "- $db: 失败" >> "$report_file"
        fi
    done
    
    echo "" >> "$report_file"
    echo "存储使用情况:" >> "$report_file"
    echo "- 本次备份大小: $(du -sh "${BACKUP_DIR}/${BACKUP_NAME}" | cut -f1)" >> "$report_file"
    echo "- 总备份目录大小: $(du -sh "$BACKUP_DIR" | cut -f1)" >> "$report_file"
    
    echo "" >> "$report_file"
    echo "清理情况:" >> "$report_file"
    echo "- 保留策略: ${RETENTION_DAYS} 天" >> "$report_file"
    
    log_info "备份报告已生成: $report_file"
}

# 发送通知
send_notification() {
    local status="$1"
    local message="$2"
    
    log_info "发送备份通知..."
    
    # 这里可以集成企业微信、钉钉、邮件等通知方式
    # 示例：发送到企业微信机器人
    # curl -s 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=YOUR_KEY' \
    #   -H 'Content-Type: application/json' \
    #   -d "{\"msgtype\":\"text\",\"text\":{\"content\":\"$message\"}}"
    
    echo "通知内容: $message" >> "${LOG_DIR}/backup_${TIMESTAMP}.log"
}

# 主函数
main() {
    log_info "开始 PostgreSQL 备份任务"
    
    # 检查依赖
    check_dependencies
    
    # 检查目录
    check_directories
    
    # 备份配置
    backup_config
    
    # 备份全局对象
    if ! backup_global_objects; then
        log_error "全局对象备份失败，继续备份数据库"
    fi
    
    # 备份所有数据库
    if backup_all_databases; then
        log_info "所有数据库备份成功"
        
        # 清理旧备份
        cleanup_old_backups
        
        # 生成报告
        generate_backup_report
        
        # 发送成功通知
        send_notification "success" "PostgreSQL 备份成功: ${BACKUP_NAME}"
        
        log_info "PostgreSQL 备份任务完成"
        exit 0
    else
        log_error "数据库备份失败"
        
        # 发送失败通知
        send_notification "failure" "PostgreSQL 备份失败: ${BACKUP_NAME}"
        
        log_info "PostgreSQL 备份任务失败"
        exit 1
    fi
}

# 执行主函数
main "$@"