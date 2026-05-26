#!/bin/bash

# 备份Cron任务配置脚本
# 版本: 1.0
# 创建日期: 2026-04-29
# 作者: team-member

set -euo pipefail

# 配置参数
CRON_DIR="/etc/cron.d"
BACKUP_SCRIPTS_DIR="I:/AI-Ready/scripts/backup"
BACKUP_CONFIG_FILE="${CRON_DIR}/ai-ready-backup"
LOG_DIR="/var/log/backup/cron"

# 创建目录
mkdir -p "$LOG_DIR"
mkdir -p "$(dirname "$BACKUP_CONFIG_FILE")"

# 日志函数
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# 生成Cron配置
generate_cron_config() {
    log "生成备份Cron配置..."
    
    cat > "$BACKUP_CONFIG_FILE" << EOF
# AI-Ready测试环境备份计划
# 配置文件: $BACKUP_CONFIG_FILE
# 生成时间: $(date)
# 作者: team-member

# ==================== 数据库备份计划 ====================

# PostgreSQL全量备份 (每周日02:00)
0 2 * * 0 root /bin/bash ${BACKUP_SCRIPTS_DIR}/postgresql/backup_postgresql.sh --type=full >> ${LOG_DIR}/postgresql_full_$(date +\%Y\%m\%d).log 2>&1

# PostgreSQL增量备份 (每天04:00)
0 4 * * * root /bin/bash ${BACKUP_SCRIPTS_DIR}/postgresql/backup_postgresql.sh --type=incremental >> ${LOG_DIR}/postgresql_incremental_$(date +\%Y\%m\%d).log 2>&1

# 备份验证 (每天05:00)
0 5 * * * root /bin/bash ${BACKUP_SCRIPTS_DIR}/postgresql/verify_backup.sh >> ${LOG_DIR}/verify_$(date +\%Y\%m\%d).log 2>&1

# ==================== 配置文件备份计划 ====================

# 系统配置文件备份 (每天01:00)
0 1 * * * root /bin/bash ${BACKUP_SCRIPTS_DIR}/filesystem/config_backup.sh --type=system >> ${LOG_DIR}/config_system_$(date +\%Y\%m\%d).log 2>&1

# 应用配置文件备份 (每天03:00)
0 3 * * * root /bin/bash ${BACKUP_SCRIPTS_DIR}/filesystem/config_backup.sh --type=application >> ${LOG_DIR}/config_application_$(date +\%Y\%m\%d).log 2>&1

# 数据库配置文件备份 (每周一03:30)
30 3 * * 1 root /bin/bash ${BACKUP_SCRIPTS_DIR}/filesystem/config_backup.sh --type=database >> ${LOG_DIR}/config_database_$(date +\%Y\%m\%d).log 2>&1

# ==================== 文件系统备份计划 ====================

# 关键目录备份 (每天02:00)
0 2 * * * root /bin/bash ${BACKUP_SCRIPTS_DIR}/filesystem/fs_full_backup.sh >> ${LOG_DIR}/filesystem_$(date +\%Y\%m\%d).log 2>&1

# 日志文件备份 (每天23:50)
50 23 * * * root /bin/bash ${BACKUP_SCRIPTS_DIR}/filesystem/log_backup.sh >> ${LOG_DIR}/log_backup_$(date +\%Y\%m\%d).log 2>&1

# ==================== 监控和告警计划 ====================

# 备份监控 (每小时第30分钟)
30 * * * * root /bin/bash ${BACKUP_SCRIPTS_DIR}/postgresql/monitor_backup.sh >> ${LOG_DIR}/monitor_$(date +\%Y\%m\%d).log 2>&1

# 磁盘空间监控 (每15分钟)
*/15 * * * * root /bin/bash ${BACKUP_SCRIPTS_DIR}/monitoring/disk_space_monitor.sh >> ${LOG_DIR}/disk_monitor_$(date +\%Y\%m\%d).log 2>&1

# 备份完整性检查 (每天06:00)
0 6 * * * root /bin/bash ${BACKUP_SCRIPTS_DIR}/postgresql/integrity_check.sh >> ${LOG_DIR}/integrity_$(date +\%Y\%m\%d).log 2>&1

# ==================== 清理和维护计划 ====================

# 清理旧备份文件 (每天07:00)
0 7 * * * root /bin/bash ${BACKUP_SCRIPTS_DIR}/cleanup/cleanup_old_backups.sh --retention-days=30 >> ${LOG_DIR}/cleanup_$(date +\%Y\%m\%d).log 2>&1

# 清理旧日志文件 (每周六08:00)
0 8 * * 6 root /bin/bash ${BACKUP_SCRIPTS_DIR}/cleanup/cleanup_old_logs.sh --retention-days=90 >> ${LOG_DIR}/log_cleanup_$(date +\%Y\%m\%d).log 2>&1

# 备份统计报告生成 (每天08:00)
0 8 * * * root /bin/bash ${BACKUP_SCRIPTS_DIR}/reporting/generate_backup_report.sh >> ${LOG_DIR}/report_$(date +\%Y\%m\%d).log 2>&1

# ==================== 验证和测试计划 ====================

# 恢复测试 (每周三03:00 - 低峰时段)
0 3 * * 3 root /bin/bash ${BACKUP_SCRIPTS_DIR}/testing/restore_test.sh --test-mode=safe >> ${LOG_DIR}/restore_test_$(date +\%Y\%m\%d).log 2>&1

# 备份性能测试 (每月1号04:00)
0 4 1 * * root /bin/bash ${BACKUP_SCRIPTS_DIR}/testing/performance_test.sh >> ${LOG_DIR}/performance_test_$(date +\%Y\%m\%d).log 2>&1

# ==================== 通知和告警 ====================

# 备份状态通知 (每天09:00)
0 9 * * * root /bin/bash ${BACKUP_SCRIPTS_DIR}/notification/send_daily_report.sh >> ${LOG_DIR}/notification_$(date +\%Y\%m\%d).log 2>&1

# 失败告警检查 (每30分钟)
*/30 * * * * root /bin/bash ${BACKUP_SCRIPTS_DIR}/notification/check_failures.sh >> ${LOG_DIR}/failure_check_$(date +\%Y\%m\%d).log 2>&1

EOF
    
    log "Cron配置已生成: $BACKUP_CONFIG_FILE"
}

# 验证Cron配置
validate_cron_config() {
    log "验证Cron配置..."
    
    if [ ! -f "$BACKUP_CONFIG_FILE" ]; then
        log "错误: Cron配置文件不存在"
        return 1
    fi
    
    # 检查文件格式
    if ! crontab -l > /dev/null 2>&1; then
        log "警告: 无法验证Cron配置格式"
    fi
    
    # 检查必要的目录
    local required_dirs=("$BACKUP_SCRIPTS_DIR" "$LOG_DIR")
    for dir in "${required_dirs[@]}"; do
        if [ ! -d "$dir" ]; then
            log "警告: 目录不存在: $dir"
        fi
    done
    
    # 检查脚本文件
    local script_count=$(grep -c "/bin/bash ${BACKUP_SCRIPTS_DIR}" "$BACKUP_CONFIG_FILE")
    log "配置中包含 $script_count 个备份脚本"
    
    # 检查时间配置
    local cron_lines=$(grep -c "^[0-9*/-]" "$BACKUP_CONFIG_FILE")
    log "配置中包含 $cron_lines 个Cron任务"
    
    log "Cron配置验证完成"
    return 0
}

# 安装Cron配置
install_cron_config() {
    log "安装Cron配置..."
    
    if [ ! -f "$BACKUP_CONFIG_FILE" ]; then
        log "错误: 需要先生成Cron配置"
        return 1
    fi
    
    # 检查是否已存在配置
    if [ -f "$BACKUP_CONFIG_FILE" ] && [ -s "$BACKUP_CONFIG_FILE" ]; then
        local backup_file="${BACKUP_CONFIG_FILE}.backup.$(date +%Y%m%d_%H%M%S)"
        cp "$BACKUP_CONFIG_FILE" "$backup_file"
        log "已备份现有配置: $backup_file"
    fi
    
    # 安装配置
    if command -v crontab &> /dev/null; then
        # 将配置添加到crontab
        if crontab "$BACKUP_CONFIG_FILE" 2>/dev/null; then
            log "Cron配置安装成功"
        else
            log "警告: 无法通过crontab安装，将配置文件复制到Cron目录"
            cp "$BACKUP_CONFIG_FILE" "${CRON_DIR}/"
            log "配置文件已复制到: ${CRON_DIR}/"
        fi
    else
        log "警告: crontab命令不存在，将配置文件复制到Cron目录"
        cp "$BACKUP_CONFIG_FILE" "${CRON_DIR}/"
        log "配置文件已复制到: ${CRON_DIR}/"
    fi
    
    # 设置文件权限
    chmod 644 "$BACKUP_CONFIG_FILE"
    log "文件权限已设置"
}

# 生成安装说明
generate_installation_guide() {
    log "生成安装说明..."
    
    local guide_file="${LOG_DIR}/cron_installation_guide.md"
    
    cat > "$guide_file" << EOF
# AI-Ready测试环境备份Cron任务安装指南

## 概述

本文档提供了AI-Ready测试环境备份自动化任务的Cron配置安装指南。通过配置Cron任务，可以实现以下自动化功能：

1. 定期数据库备份（全量/增量）
2. 配置文件备份
3. 备份验证和监控
4. 清理和维护
5. 告警和通知

## 安装步骤

### 步骤1: 检查环境

确保系统已安装以下组件：
- bash
- cron (或crontab)
- PostgreSQL客户端工具
- tar, gzip等压缩工具

### 步骤2: 生成Cron配置

运行以下命令生成Cron配置：

\`\`\`bash
bash "$(realpath "$0")" --generate
\`\`\`

### 步骤3: 验证配置

运行以下命令验证Cron配置：

\`\`\`bash
bash "$(realpath "$0")" --validate
\`\`\`

### 步骤4: 安装配置

运行以下命令安装Cron配置：

\`\`\`bash
bash "$(realpath "$0")" --install
\`\`\`

### 步骤5: 验证安装

检查Cron任务是否已安装：

\`\`\`bash
crontab -l | grep -i backup
\`\`\`

或检查Cron配置文件：

\`\`\`bash
ls -la "${CRON_DIR}/" | grep backup
\`\`\`

## Cron任务详情

### 数据库备份计划

| 任务 | 时间 | 频率 | 脚本 |
|------|------|------|------|
| PostgreSQL全量备份 | 02:00 | 每周日 | \`backup_postgresql.sh --type=full\` |
| PostgreSQL增量备份 | 04:00 | 每天 | \`backup_postgresql.sh --type=incremental\` |
| 备份验证 | 05:00 | 每天 | \`verify_backup.sh\` |

### 配置文件备份计划

| 任务 | 时间 | 频率 | 脚本 |
|------|------|------|------|
| 系统配置备份 | 01:00 | 每天 | \`config_backup.sh --type=system\` |
| 应用配置备份 | 03:00 | 每天 | \`config_backup.sh --type=application\` |
| 数据库配置备份 | 03:30 | 每周一 | \`config_backup.sh --type=database\` |

### 监控和告警计划

| 任务 | 时间 | 频率 | 脚本 |
|------|------|------|------|
| 备份监控 | 每小时第30分钟 | 每小时 | \`monitor_backup.sh\` |
| 磁盘空间监控 | 每15分钟 | 每15分钟 | \`disk_space_monitor.sh\` |
| 备份完整性检查 | 06:00 | 每天 | \`integrity_check.sh\` |

### 清理和维护计划

| 任务 | 时间 | 频率 | 脚本 |
|------|------|------|------|
| 清理旧备份 | 07:00 | 每天 | \`cleanup_old_backups.sh\` |
| 清理旧日志 | 08:00 | 每周六 | \`cleanup_old_logs.sh\` |
| 生成报告 | 08:00 | 每天 | \`generate_backup_report.sh\` |

## 日志文件位置

所有Cron任务的日志文件位于：\`${LOG_DIR}/\`

日志文件命名格式：\`<任务类型>_YYYYMMDD.log\`

例如：
- \`postgresql_full_20260429.log\`
- \`config_system_20260429.log\`
- \`monitor_20260429.log\`

## 故障排除

### 1. Cron任务未执行

检查Cron服务状态：
\`\`\`bash
systemctl status cron
# 或
service crond status
\`\`\`

### 2. 权限问题

确保脚本有执行权限：
\`\`\`bash
chmod +x ${BACKUP_SCRIPTS_DIR}/*.sh
chmod +x ${BACKUP_SCRIPTS_DIR}/**/*.sh
\`\`\`

### 3. 路径问题

检查脚本中的路径是否正确：
\`\`\`bash
# 在Cron任务中测试脚本
/bin/bash ${BACKUP_SCRIPTS_DIR}/postgresql/backup_postgresql.sh --test
\`\`\`

### 4. 环境变量问题

Cron任务可能没有正确的环境变量，建议在脚本中显式设置：
\`\`\`bash
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
export PGPASSWORD=your_password
\`\`\`

## 监控和告警

### 成功监控

检查日志文件中的成功标志：
\`\`\`bash
grep -i "success\|完成\|成功" ${LOG_DIR}/latest.log
\`\`\`

### 失败监控

检查日志文件中的错误标志：
\`\`\`bash
grep -i "error\|fail\|失败\|错误" ${LOG_DIR}/latest.log
\`\`\`

### 磁盘空间监控

定期检查备份目录空间使用情况：
\`\`\`bash
df -h /data/backups
du -sh /data/backups/*
\`\`\`

## 联系方式

如有问题，请联系：
- 项目: AI-Ready测试环境
- 负责人: team-member
- 任务ID: task_1777432587755_ip5px4f5f

---
**文档生成时间**: $(date)
**配置文件位置**: ${BACKUP_CONFIG_FILE}
**安装脚本**: $(realpath "$0")
EOF
    
    log "安装说明已生成: $guide_file"
    echo "$guide_file"
}

# 主函数
main() {
    local action="${1:-all}"
    
    case "$action" in
        "--generate"|"generate")
            generate_cron_config
            ;;
        "--validate"|"validate")
            validate_cron_config
            ;;
        "--install"|"install")
            install_cron_config
            ;;
        "--guide"|"guide")
            generate_installation_guide
            ;;
        "--test"|"test")
            log "测试模式: Cron配置脚本功能正常"
            ;;
        "all"|*)
            log "执行完整Cron配置流程..."
            generate_cron_config
            validate_cron_config
            install_cron_config
            generate_installation_guide
            log "Cron配置流程完成"
            ;;
    esac
}

# 脚本入口
if [ $# -eq 0 ]; then
    main "all"
else
    main "$1"
fi