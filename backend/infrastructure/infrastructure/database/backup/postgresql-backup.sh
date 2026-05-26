#!/bin/bash

# PostgreSQL 自动备份脚本
# 项目: AI-Ready
# 版本: v1.0
# 创建时间: 2026-04-26
# 作者: devops-engineer

# 配置变量
BACKUP_DIR="/backup/postgresql"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/ai-ready_${DATE}.sql"
LOG_FILE="${BACKUP_DIR}/backup.log"

# 数据库连接参数 (从环境变量读取)
DB_HOST=${DB_HOST:-"localhost"}
DB_PORT=${DB_PORT:-"5432"}
DB_NAME=${DB_NAME:-"devdb"}
DB_USER=${DB_USER:-"devuser"}
DB_PASSWORD=${DB_PASSWORD:-"Dev@2026#Local"}

# 备份保留策略 (天数)
RETENTION_DAYS=${RETENTION_DAYS:-7}

# 日志函数
log_message() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

# 检查备份目录
if [ ! -d "$BACKUP_DIR" ]; then
    log_message "创建备份目录: $BACKUP_DIR"
    mkdir -p "$BACKUP_DIR"
fi

# 设置数据库密码环境变量
export PGPASSWORD="$DB_PASSWORD"

# 执行pg_dump备份
log_message "开始备份数据库: $DB_NAME"
pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -F c -f "$BACKUP_FILE"

# 检查备份是否成功
if [ $? -eq 0 ]; then
    log_message "✅ 备份成功: $BACKUP_FILE"
    
    # 备份完整性检查
    log_message "执行备份完整性检查..."
    pg_restore -l "$BACKUP_FILE" > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        log_message "✅ 备份完整性检查通过"
    else
        log_message "❌ 备份完整性检查失败"
        exit 1
    fi
    
    # 清理旧备份文件
    log_message "清理 ${RETENTION_DAYS} 天前的旧备份..."
    find "$BACKUP_DIR" -name "ai-ready_*.sql" -type f -mtime +$RETENTION_DAYS -delete
    log_message "旧备份清理完成"
    
else
    log_message "❌ 备份失败"
    exit 1
fi

log_message "备份任务完成"
exit 0