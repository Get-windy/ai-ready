#!/bin/bash
# AI-Ready 备份恢复验证脚本
# Version: v1.0

set -e

# ============================================
# 配置
# ============================================
BACKUP_ROOT="${BACKUP_ROOT:-/backup/ai-ready}"
TEST_DB_SUFFIX="_test_$(date +%Y%m%d%H%M%S)"
LOG_FILE="${BACKUP_ROOT}/logs/restore-test-$(date +%Y%m%d).log"

# ============================================
# 日志函数
# ============================================
log() {
    local level="$1"
    local message="$2"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [$level] $message" | tee -a "$LOG_FILE"
}

log_info() { log "INFO" "$1"; }
log_error() { log "ERROR" "$1"; }
log_success() { log "SUCCESS" "$1"; }

# ============================================
# PostgreSQL 恢复验证
# ============================================
test_postgresql_restore() {
    log_info "========== PostgreSQL 恢复验证 =========="
    
    local backup_file="$1"
    local test_db="${DB_NAME}${TEST_DB_SUFFIX}"
    
    if [[ ! -f "$backup_file" ]]; then
        log_error "备份文件不存在: $backup_file"
        return 1
    fi
    
    log_info "使用备份文件: $backup_file"
    log_info "测试数据库: $test_db"
    
    # 1. 创建测试数据库
    log_info "创建测试数据库..."
    PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -U "${DB_USER}" -d postgres \
        -c "CREATE DATABASE ${test_db};" || {
        log_error "创建测试数据库失败"
        return 1
    }
    
    # 2. 恢复数据
    log_info "恢复数据到测试数据库..."
    PGPASSWORD="${DB_PASSWORD}" pg_restore \
        -h "${DB_HOST}" \
        -U "${DB_USER}" \
        -d "$test_db" \
        --no-owner \
        --no-acl \
        "$backup_file" || {
        log_error "数据恢复失败"
        # 清理
        PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -U "${DB_USER}" -d postgres -c "DROP DATABASE IF EXISTS ${test_db};" 2>/dev/null
        return 1
    }
    
    # 3. 数据完整性检查
    log_info "执行数据完整性检查..."
    
    # 检查表数量
    local table_count=$(PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -U "${DB_USER}" -d "$test_db" -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';")
    log_info "表数量: $(echo $table_count | tr -d ' ')"
    
    # 检查关键表
    local critical_tables=("users" "organizations" "documents" "audit_logs")
    for table in "${critical_tables[@]}"; do
        local count=$(PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -U "${DB_USER}" -d "$test_db" -t -c "SELECT COUNT(*) FROM ${table};" 2>/dev/null || echo "N/A")
        log_info "表 ${table} 记录数: $(echo $count | tr -d ' ')"
    done
    
    # 4. 执行随机查询测试
    log_info "执行查询测试..."
    PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -U "${DB_USER}" -d "$test_db" \
        -c "SELECT u.id, u.username, COUNT(o.id) as org_count FROM users u LEFT JOIN organizations o ON u.id = o.owner_id GROUP BY u.id LIMIT 10;" > /dev/null 2>&1 && \
        log_info "关联查询测试通过" || log_error "关联查询测试失败"
    
    # 5. 清理测试数据库
    log_info "清理测试数据库..."
    PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -U "${DB_USER}" -d postgres \
        -c "DROP DATABASE ${test_db};" || {
        log_error "清理测试数据库失败"
        return 1
    }
    
    log_success "PostgreSQL恢复验证完成"
    return 0
}

# ============================================
# Redis 恢复验证
# ============================================
test_redis_restore() {
    log_info "========== Redis 恢复验证 =========="
    
    local backup_file="$1"
    
    if [[ ! -f "$backup_file" ]]; then
        log_error "备份文件不存在: $backup_file"
        return 1
    fi
    
    log_info "使用备份文件: $backup_file"
    
    # 1. 解压RDB文件
    local temp_dir=$(mktemp -d)
    gunzip -c "$backup_file" > "${temp_dir}/dump.rdb"
    
    # 2. 启动临时Redis实例
    local test_port=6380
    log_info "启动临时Redis实例 (端口: $test_port)..."
    redis-server --port $test_port --daemonize yes --dir "$temp_dir" --dbfilename dump.rdb
    
    sleep 3
    
    # 3. 验证数据
    log_info "验证Redis数据..."
    
    # 检查Key数量
    local key_count=$(redis-cli -p $test_port DBSIZE | grep -oE '[0-9]+')
    log_info "Key数量: $key_count"
    
    # 随机检查几个Key
    local sample_keys=$(redis-cli -p $test_port RANDOMKEY)
    if [[ -n "$sample_keys" ]]; then
        log_info "示例Key: $sample_keys"
        log_info "Key类型: $(redis-cli -p $test_port TYPE $sample_keys)"
    fi
    
    # 4. 清理
    log_info "清理临时Redis实例..."
    redis-cli -p $test_port SHUTDOWN NOSAVE 2>/dev/null || true
    rm -rf "$temp_dir"
    
    log_success "Redis恢复验证完成"
    return 0
}

# ============================================
# PITR 时间点恢复测试
# ============================================
test_pitr_restore() {
    log_info "========== PITR 时间点恢复测试 =========="
    
    local target_time="$1"
    local test_db="pitr_test_${TEST_DB_SUFFIX}"
    
    log_info "目标恢复时间: $target_time"
    
    # 使用pgBackRest进行PITR
    if command -v pgbackrest &> /dev/null; then
        log_info "使用pgBackRest执行PITR..."
        
        pgbackrest --stanza=main \
            --delta \
            --type=time \
            --target="$target_time" \
            restore
        
        log_success "PITR恢复完成"
    else
        log_error "pgBackRest未安装，跳过PITR测试"
        return 1
    fi
}

# ============================================
# 生成验证报告
# ============================================
generate_report() {
    local report_file="${BACKUP_ROOT}/logs/verification-report-$(date +%Y%m%d).json"
    
    cat > "$report_file" << EOF
{
    "timestamp": "$(date -Iseconds)",
    "backup_root": "$BACKUP_ROOT",
    "postgresql": {
        "status": "$1",
        "backup_file": "$2",
        "test_database": "${DB_NAME}${TEST_DB_SUFFIX}"
    },
    "redis": {
        "status": "$3",
        "backup_file": "$4"
    },
    "overall_status": "$5"
}
EOF
    
    log_info "验证报告已生成: $report_file"
}

# ============================================
# 主函数
# ============================================
main() {
    log_info "========== 开始备份恢复验证 =========="
    
    local pg_status="unknown"
    local redis_status="unknown"
    local overall_status="success"
    
    # 查找最新备份
    local latest_pg_backup=$(ls -t ${BACKUP_ROOT}/database/*/*.dump 2>/dev/null | head -1)
    local latest_redis_backup=$(ls -t ${BACKUP_ROOT}/redis/*/*.rdb.gz 2>/dev/null | head -1)
    
    # 测试PostgreSQL恢复
    if [[ -n "$latest_pg_backup" ]]; then
        if test_postgresql_restore "$latest_pg_backup"; then
            pg_status="passed"
        else
            pg_status="failed"
            overall_status="failed"
        fi
    else
        log_error "未找到PostgreSQL备份文件"
        pg_status="not_found"
        overall_status="failed"
    fi
    
    # 测试Redis恢复
    if [[ -n "$latest_redis_backup" ]]; then
        if test_redis_restore "$latest_redis_backup"; then
            redis_status="passed"
        else
            redis_status="failed"
            overall_status="failed"
        fi
    else
        log_info "未找到Redis备份文件，跳过验证"
        redis_status="not_found"
    fi
    
    # 生成报告
    generate_report "$pg_status" "$latest_pg_backup" "$redis_status" "$latest_redis_backup" "$overall_status"
    
    log_info "========== 验证完成: $overall_status =========="
    
    if [[ "$overall_status" == "success" ]]; then
        return 0
    else
        return 1
    fi
}

# 执行
main "$@"
