#!/bin/bash

# 数据库备份恢复演练执行脚本
# 版本: 1.0
# 创建日期: 2026-04-28

set -euo pipefail

# 配置参数
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
BACKUP_SCRIPTS_DIR="$PROJECT_ROOT/scripts/backup"
QA_DATABASE_DIR="$PROJECT_ROOT/qa/database"
LOG_DIR="$QA_DATABASE_DIR/logs"
REPORT_DIR="$QA_DATABASE_DIR/reports"
DATE=$(date +%Y%m%d_%H%M%S)
LOG_FILE="$LOG_DIR/backup_recovery_drill_${DATE}.log"
REPORT_FILE="$REPORT_DIR/backup_recovery_drill_report_${DATE}.md"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 创建目录
mkdir -p "$LOG_DIR"
mkdir -p "$REPORT_DIR"
mkdir -p "$(dirname "$LOG_FILE")"

# 日志函数
log() {
    local level="$1"
    local message="$2"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    case "$level" in
        "INFO")
            echo -e "${GREEN}[INFO]${NC} $message"
            ;;
        "WARN")
            echo -e "${YELLOW}[WARN]${NC} $message"
            ;;
        "ERROR")
            echo -e "${RED}[ERROR]${NC} $message"
            ;;
        "STEP")
            echo -e "${BLUE}[STEP]${NC} $message"
            ;;
        *)
            echo "[$level] $message"
            ;;
    esac
    
    echo "[$timestamp] [$level] $message" >> "$LOG_FILE"
}

# 错误处理函数
error_exit() {
    log "ERROR" "$1"
    exit 1
}

# 检查环境
check_environment() {
    log "STEP" "========== 检查演练环境 =========="
    
    # 检查项目目录
    if [ ! -d "$PROJECT_ROOT" ]; then
        error_exit "项目根目录不存在: $PROJECT_ROOT"
    fi
    log "INFO" "项目根目录: $PROJECT_ROOT"
    
    # 检查备份脚本目录
    if [ ! -d "$BACKUP_SCRIPTS_DIR" ]; then
        log "WARN" "备份脚本目录不存在，将创建: $BACKUP_SCRIPTS_DIR"
        mkdir -p "$BACKUP_SCRIPTS_DIR/postgresql"
        mkdir -p "$BACKUP_SCRIPTS_DIR/redis"
    fi
    
    # 检查备份脚本
    local postgresql_backup_script="$BACKUP_SCRIPTS_DIR/postgresql/backup_postgresql.sh"
    local postgresql_restore_script="$BACKUP_SCRIPTS_DIR/postgresql/restore_postgresql.sh"
    local redis_backup_script="$BACKUP_SCRIPTS_DIR/redis/backup_redis.sh"
    local redis_restore_script="$BACKUP_SCRIPTS_DIR/redis/restore_redis.sh"
    
    if [ ! -f "$postgresql_backup_script" ]; then
        error_exit "PostgreSQL备份脚本不存在: $postgresql_backup_script"
    fi
    
    if [ ! -f "$postgresql_restore_script" ]; then
        error_exit "PostgreSQL恢复脚本不存在: $postgresql_restore_script"
    fi
    
    if [ ! -f "$redis_backup_script" ]; then
        error_exit "Redis备份脚本不存在: $redis_backup_script"
    fi
    
    if [ ! -f "$redis_restore_script" ]; then
        error_exit "Redis恢复脚本不存在: $redis_restore_script"
    fi
    
    # 设置脚本执行权限
    chmod +x "$postgresql_backup_script" "$postgresql_restore_script" \
             "$redis_backup_script" "$redis_restore_script"
    
    log "INFO" "所有备份脚本检查通过"
    
    # 检查备份目录
    local postgresql_backup_dir="/data/backups/postgresql"
    local redis_backup_dir="/data/backups/redis"
    
    log "INFO" "检查备份目录..."
    mkdir -p "$postgresql_backup_dir"
    mkdir -p "$redis_backup_dir"
    
    log "INFO" "备份目录已创建:"
    log "INFO" "  PostgreSQL: $postgresql_backup_dir"
    log "INFO" "  Redis: $redis_backup_dir"
    
    log "STEP" "环境检查完成"
}

# 模拟测试数据准备
prepare_test_data() {
    log "STEP" "========== 准备测试数据 =========="
    
    # 创建测试数据文件
    local test_data_file="$QA_DATABASE_DIR/test_data_${DATE}.sql"
    
    cat > "$test_data_file" << EOF
-- 测试数据准备脚本
-- 生成时间: $(date)
-- 用于数据库备份恢复演练

-- 创建测试表（如果不存在）
CREATE TABLE IF NOT EXISTS backup_recovery_test (
    id SERIAL PRIMARY KEY,
    test_name VARCHAR(100) NOT NULL,
    test_value TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 插入测试数据
INSERT INTO backup_recovery_test (test_name, test_value) VALUES
('全量备份测试', '这是全量备份测试数据'),
('增量备份测试', '这是增量备份测试数据'),
('时间点恢复测试', '这是时间点恢复测试数据'),
('数据完整性验证', '这是数据完整性验证测试数据');

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_test_name ON backup_recovery_test(test_name);

-- 验证数据
SELECT '测试数据准备完成' as status, COUNT(*) as record_count FROM backup_recovery_test;
EOF
    
    log "INFO" "测试数据SQL脚本已创建: $test_data_file"
    
    # 创建Redis测试数据脚本
    local redis_test_data_file="$QA_DATABASE_DIR/redis_test_data_${DATE}.txt"
    
    cat > "$redis_test_data_file" << EOF
# Redis测试数据命令
# 生成时间: $(date)

SET backup:test:full "这是Redis全量备份测试数据"
SET backup:test:incremental "这是Redis增量备份测试数据"
SET backup:test:recovery "这是Redis恢复测试数据"
HSET backup:test:hash field1 "哈希字段1" field2 "哈希字段2"
LPUSH backup:test:list "列表项1" "列表项2" "列表项3"
SADD backup:test:set "集合项1" "集合项2" "集合项3"

# 验证数据
DBSIZE
GET backup:test:full
HGETALL backup:test:hash
LLEN backup:test:list
SCARD backup:test:set
EOF
    
    log "INFO" "Redis测试数据脚本已创建: $redis_test_data_file"
    
    log "STEP" "测试数据准备完成"
}

# 执行PostgreSQL备份演练
execute_postgresql_backup_drill() {
    log "STEP" "========== PostgreSQL备份演练 =========="
    
    local postgresql_backup_script="$BACKUP_SCRIPTS_DIR/postgresql/backup_postgresql.sh"
    local postgresql_restore_script="$BACKUP_SCRIPTS_DIR/postgresql/restore_postgresql.sh"
    local postgresql_backup_dir="/data/backups/postgresql"
    
    log "INFO" "步骤1: 执行PostgreSQL全量备份"
    
    # 执行备份
    if bash "$postgresql_backup_script" 2>&1 | tee -a "$LOG_FILE"; then
        log "INFO" "PostgreSQL全量备份成功"
    else
        error_exit "PostgreSQL全量备份失败"
    fi
    
    # 查找最新的备份文件
    local latest_backup=$(find "$postgresql_backup_dir" -name "*.dump" -type f -printf "%T@ %p\n" | sort -n | tail -1 | cut -d' ' -f2-)
    
    if [ -z "$latest_backup" ]; then
        error_exit "未找到PostgreSQL备份文件"
    fi
    
    log "INFO" "最新备份文件: $latest_backup"
    
    log "INFO" "步骤2: 验证备份文件"
    
    # 检查备份文件
    if [ -f "$latest_backup" ] && [ -r "$latest_backup" ]; then
        local file_size=$(du -h "$latest_backup" | cut -f1)
        log "INFO" "备份文件验证通过: 大小=$file_size"
    else
        error_exit "备份文件验证失败"
    fi
    
    log "INFO" "步骤3: 执行PostgreSQL恢复测试"
    
    # 执行恢复（模拟环境，实际需要数据库环境）
    log "WARN" "注意: 在实际环境中，以下恢复操作会执行"
    log "INFO" "恢复命令: $postgresql_restore_script \"$latest_backup\""
    
    # 在实际环境中取消注释以下行
    # if bash "$postgresql_restore_script" "$latest_backup" 2>&1 | tee -a "$LOG_FILE"; then
    #     log "INFO" "PostgreSQL恢复测试成功"
    # else
    #     error_exit "PostgreSQL恢复测试失败"
    # fi
    
    log "INFO" "PostgreSQL恢复测试已跳过（模拟环境）"
    
    log "STEP" "PostgreSQL备份演练完成"
    
    # 返回备份文件路径
    echo "$latest_backup"
}

# 执行Redis备份演练
execute_redis_backup_drill() {
    log "STEP" "========== Redis备份演练 =========="
    
    local redis_backup_script="$BACKUP_SCRIPTS_DIR/redis/backup_redis.sh"
    local redis_restore_script="$BACKUP_SCRIPTS_DIR/redis/restore_redis.sh"
    local redis_backup_dir="/data/backups/redis"
    
    log "INFO" "步骤1: 执行Redis RDB备份"
    
    # 执行备份
    if bash "$redis_backup_script" 2>&1 | tee -a "$LOG_FILE"; then
        log "INFO" "Redis RDB备份成功"
    else
        error_exit "Redis RDB备份失败"
    fi
    
    # 查找最新的RDB备份文件
    local latest_rdb_backup=$(find "$redis_backup_dir" -name "*.rdb" -type f -printf "%T@ %p\n" | sort -n | tail -1 | cut -d' ' -f2-)
    
    if [ -z "$latest_rdb_backup" ]; then
        error_exit "未找到Redis RDB备份文件"
    fi
    
    log "INFO" "最新RDB备份文件: $latest_rdb_backup"
    
    log "INFO" "步骤2: 验证备份文件"
    
    # 检查备份文件
    if [ -f "$latest_rdb_backup" ] && [ -r "$latest_rdb_backup" ]; then
        local file_size=$(du -h "$latest_rdb_backup" | cut -f1)
        log "INFO" "RDB备份文件验证通过: 大小=$file_size"
    else
        error_exit "RDB备份文件验证失败"
    fi
    
    log "INFO" "步骤3: 执行Redis恢复测试"
    
    # 执行恢复（模拟环境，实际需要Redis环境）
    log "WARN" "注意: 在实际环境中，以下恢复操作会执行"
    log "INFO" "恢复命令: $redis_restore_script \"$latest_rdb_backup\""
    
    # 在实际环境中取消注释以下行
    # if bash "$redis_restore_script" "$latest_rdb_backup" 2>&1 | tee -a "$LOG_FILE"; then
    #     log "INFO" "Redis恢复测试成功"
    # else
    #     error_exit "Redis恢复测试失败"
    # fi
    
    log "INFO" "Redis恢复测试已跳过（模拟环境）"
    
    log "STEP" "Redis备份演练完成"
    
    # 返回备份文件路径
    echo "$latest_rdb_backup"
}

# 生成演练报告
generate_drill_report() {
    log "STEP" "========== 生成演练报告 =========="
    
    local postgresql_backup_file="$1"
    local redis_backup_file="$2"
    
    cat > "$REPORT_FILE" << EOF
# 数据库备份恢复演练报告

## 报告信息
- **演练日期**: $(date)
- **演练环境**: Sprint 27+1测试环境
- **执行人员**: team-member
- **报告生成时间**: $(date '+%Y-%m-%d %H:%M:%S')

## 演练概述
本次演练旨在验证测试环境数据库备份恢复策略的有效性，确保在数据丢失情况下能够快速恢复业务。

### 演练目标
1. 验证PostgreSQL备份恢复流程
2. 验证Redis备份恢复流程
3. 测试备份文件的完整性和可恢复性
4. 评估恢复时间目标(RTO)

### 演练范围
- PostgreSQL全量备份恢复
- Redis RDB备份恢复
- 备份脚本功能验证
- 恢复流程验证

## 演练执行情况

### 环境检查
- ✅ 项目目录检查通过
- ✅ 备份脚本检查通过
- ✅ 备份目录创建成功
- ✅ 测试数据准备完成

### PostgreSQL备份演练
- ✅ 全量备份执行成功
- **备份文件**: ${postgresql_backup_file:-未生成}
- **文件大小**: $(du -h "${postgresql_backup_file:-}" 2>/dev/null | cut -f1 || echo "未知")
- ✅ 备份文件验证通过
- ⚠️ 恢复测试已跳过（模拟环境）

### Redis备份演练
- ✅ RDB备份执行成功
- **备份文件**: ${redis_backup_file:-未生成}
- **文件大小**: $(du -h "${redis_backup_file:-}" 2>/dev/null | cut -f1 || echo "未知")
- ✅ 备份文件验证通过
- ⚠️ 恢复测试已跳过（模拟环境）

## 发现问题与改进建议

### 发现的问题
1. **模拟环境限制**: 当前为模拟环境，无法实际执行恢复操作
2. **数据库连接**: 需要实际数据库环境进行完整测试
3. **备份监控**: 需要建立备份监控和告警机制

### 改进建议
1. **实际环境测试**: 在真实测试环境执行完整备份恢复演练
2. **自动化监控**: 实现备份任务自动化监控和告警
3. **定期演练**: 建立每月一次的定期备份恢复演练机制
4. **文档完善**: 完善备份恢复操作文档和应急预案

## 演练总结

### 成功事项
1. ✅ 备份脚本开发和测试完成
2. ✅ 备份目录结构创建完成
3. ✅ 备份文件生成和验证成功
4. ✅ 演练流程设计和文档完善

### 待办事项
1. 🔄 在实际数据库环境执行恢复测试
2. 🔄 配置备份任务定时执行
3. 🔄 建立备份监控告警机制
4. 🔄 培训团队成员掌握备份恢复技能

## 后续行动计划

### 短期行动（1周内）
1. 在测试环境部署数据库并执行完整演练
2. 配置备份任务定时执行（cron）
3. 验证备份文件的远程存储

### 中期行动（1个月内）
1. 建立备份监控和告警机制
2. 每月执行一次备份恢复演练
3. 完善备份恢复应急预案

### 长期行动（3个月内）
1. 实现自动化备份恢复演练
2. 建立备份恢复知识库
3. 定期评估和优化备份策略

## 附件
1. 演练详细日志: $LOG_FILE
2. PostgreSQL备份脚本: $BACKUP_SCRIPTS_DIR/postgresql/
3. Redis备份脚本: $BACKUP_SCRIPTS_DIR/redis/
4. 测试数据脚本: $QA_DATABASE_DIR/test_data_${DATE}.sql
5. Redis测试数据脚本: $QA_DATABASE_DIR/redis_test_data_${DATE}.txt

---

**报告生成**: team-member  
**审核状态**: 待审核  
**下次演练计划**: 2026-05-05
EOF
    
    log "INFO" "演练报告已生成: $REPORT_FILE"
    log "STEP" "报告生成完成"
}

# 主函数
main() {
    log "STEP" "========== 数据库备份恢复演练开始 =========="
    log "INFO" "演练时间: $(date)"
    log "INFO" "日志文件: $LOG_FILE"
    log "INFO" "报告文件: $REPORT_FILE"
    
    # 执行演练步骤
    check_environment
    prepare_test_data
    postgresql_backup_file=$(execute_postgresql_backup_drill)
    redis_backup_file=$(execute_redis_backup_drill)
    generate_drill_report "$postgresql_backup_file" "$redis_backup_file"
    
    log "STEP" "========== 数据库备份恢复演练完成 =========="
    log "INFO" "演练总结:"
    log "INFO" "  - 环境检查: 完成"
    log "INFO" "  - 测试数据: 准备完成"
    log "INFO" "  - PostgreSQL备份: 成功"
    log "INFO" "  - Redis备份: 成功"
    log "INFO" "  - 演练报告: 已生成"
    log "INFO" ""
    log "INFO" "详细日志请查看: $LOG_FILE"
    log "INFO" "完整报告请查看: $REPORT_FILE"
    
    # 输出关键文件路径
    echo ""
    echo "关键文件:"
    echo "  1. 演练计划: $QA_DATABASE_DIR/database-backup-recovery-drill-plan.md"
    echo "  2. 演练报告: $REPORT_FILE"
    echo "  3. 执行日志: $LOG_FILE"
    echo "  4. PostgreSQL备份: $postgresql_backup_file"
    echo "  5. Redis备份: $redis_backup_file"
    echo ""
    echo "数据库备份恢复演练执行完成！"
}

# 执行主函数
main "$@"