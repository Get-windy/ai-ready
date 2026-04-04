#!/bin/bash
# AI-Ready PostgreSQL 数据库备份脚本
# Version: v2.0 (支持全量+增量备份)
# Usage: ./pg-backup.sh [--full|--incremental|--wal]

set -e

# ============================================
# 配置变量
# ============================================
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-ai_ready_prod}"
DB_USER="${DB_USER:-postgres}"
BACKUP_DIR="${BACKUP_DIR:-/backup/postgresql}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
RETENTION_WEEKS="${RETENTION_WEEKS:-12}"
RETENTION_MONTHS="${RETENTION_MONTHS:-12}"

# 远程备份配置
REMOTE_BACKUP_ENABLED="${REMOTE_BACKUP_ENABLED:-true}"
REMOTE_HOST="${REMOTE_HOST:-backup-server.example.com}"
REMOTE_USER="${REMOTE_USER:-backup}"
REMOTE_PATH="${REMOTE_PATH:-/data/backups/ai-ready}"

# 压缩配置
COMPRESSION="${COMPRESSION:-zstd}"  # gzip|zstd|lz4
COMPRESSION_LEVEL="${COMPRESSION_LEVEL:-6}"

# 通知配置
NOTIFY_WEBHOOK="${NOTIFY_WEBHOOK:-}"

# ============================================
# 颜色输出
# ============================================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') $1"; }

# ============================================
# 工具函数
# ============================================
get_timestamp() { date '+%Y%m%d_%H%M%S'; }
get_date() { date '+%Y-%m-%d'; }

# 获取压缩扩展名
get_extension() {
    case $COMPRESSION in
        zstd) echo ".zst" ;;
        lz4)  echo ".lz4" ;;
        *)    echo ".gz" ;;
    esac
}

# 压缩文件
compress_file() {
    local input="$1"
    local output="${input}$(get_extension)"
    
    case $COMPRESSION in
        zstd)
            zstd -${COMPRESSION_LEVEL} -f "$input" -o "$output"
            ;;
        lz4)
            lz4 -${COMPRESSION_LEVEL} "$input" "$output"
            ;;
        *)
            gzip -${COMPRESSION_LEVEL} -f "$input" -c > "$output"
            ;;
    esac
    
    rm -f "$input"
    echo "$output"
}

# 发送通知
send_notification() {
    local status="$1"
    local message="$2"
    
    if [[ -n "$NOTIFY_WEBHOOK" ]]; then
        curl -s -X POST "$NOTIFY_WEBHOOK" \
            -H "Content-Type: application/json" \
            -d "{\"status\":\"$status\",\"message\":\"$message\",\"timestamp\":\"$(date -Iseconds)\"}" \
            > /dev/null 2>&1 || true
    fi
}

# ============================================
# 全量备份
# ============================================
full_backup() {
    log_info "开始全量备份: $DB_NAME"
    
    local timestamp=$(get_timestamp)
    local backup_name="${DB_NAME}_full_${timestamp}"
    local backup_path="${BACKUP_DIR}/full/${backup_name}"
    
    mkdir -p "${BACKUP_DIR}/full"
    
    # 使用 pg_dump 进行全量备份
    # -Fc: 自定义格式 (可并行恢复)
    # -Z: 压缩级别
    log_info "执行 pg_dump..."
    PGPASSWORD="${DB_PASSWORD}" pg_dump \
        -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$DB_USER" \
        -d "$DB_NAME" \
        -Fc \
        -Z${COMPRESSION_LEVEL} \
        -f "${backup_path}.dump"
    
    # 生成备份元数据
    local size=$(du -h "${backup_path}.dump" | cut -f1)
    local checksum=$(sha256sum "${backup_path}.dump" | cut -d' ' -f1)
    
    cat > "${backup_path}.meta" << EOF
backup_type=full
database=${DB_NAME}
timestamp=${timestamp}
size=${size}
checksum=${checksum}
host=${DB_HOST}
port=${DB_PORT}
EOF
    
    log_info "全量备份完成: ${backup_path}.dump (${size})"
    
    # 远程同步
    if [[ "$REMOTE_BACKUP_ENABLED" == "true" ]]; then
        sync_to_remote "${backup_path}.dump" "${backup_path}.meta"
    fi
    
    send_notification "success" "全量备份完成: ${backup_name}"
    echo "${backup_path}.dump"
}

# ============================================
# 增量备份 (基于WAL归档)
# ============================================
incremental_backup() {
    log_info "开始增量备份: $DB_NAME"
    
    local timestamp=$(get_timestamp)
    local backup_name="${DB_NAME}_incr_${timestamp}"
    local backup_path="${BACKUP_DIR}/incremental/${backup_name}"
    
    mkdir -p "${BACKUP_DIR}/incremental"
    
    # 使用 pgBackRest 或 Barman 进行增量备份
    # 这里使用 WAL 切换方式模拟增量备份
    
    # 1. 强制WAL切换
    log_info "强制WAL切换..."
    PGPASSWORD="${DB_PASSWORD}" psql \
        -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$DB_USER" \
        -d "$DB_NAME" \
        -c "SELECT pg_switch_wal();" > /dev/null
    
    # 2. 收集自上次全量备份以来的WAL文件
    local last_full=$(ls -t ${BACKUP_DIR}/full/*.dump 2>/dev/null | head -1)
    
    if [[ -z "$last_full" ]]; then
        log_warn "未找到全量备份，执行全量备份"
        full_backup
        return
    fi
    
    # 3. 打包WAL文件
    local wal_archive="/var/lib/postgresql/wal_archive"
    if [[ -d "$wal_archive" ]]; then
        tar -cf "${backup_path}.tar" -C "$wal_archive" .
        local compressed_file=$(compress_file "${backup_path}.tar")
        
        local size=$(du -h "$compressed_file" | cut -f1)
        log_info "增量备份完成: ${compressed_file} (${size})"
        
        # 远程同步
        if [[ "$REMOTE_BACKUP_ENABLED" == "true" ]]; then
            sync_to_remote "$compressed_file"
        fi
    else
        log_warn "WAL归档目录不存在，跳过增量备份"
    fi
}

# ============================================
# WAL归档备份
# ============================================
wal_backup() {
    log_info "开始WAL归档备份"
    
    local timestamp=$(get_timestamp)
    local backup_name="wal_archive_${timestamp}"
    local backup_path="${BACKUP_DIR}/wal/${backup_name}"
    
    mkdir -p "${BACKUP_DIR}/wal"
    
    local wal_archive="/var/lib/postgresql/wal_archive"
    
    if [[ -d "$wal_archive" ]]; then
        # 打包并压缩WAL文件
        tar -cf "${backup_path}.tar" -C "$wal_archive" .
        local compressed_file=$(compress_file "${backup_path}.tar")
        
        local size=$(du -h "$compressed_file" | cut -f1)
        local wal_count=$(ls "$wal_archive" | wc -l)
        
        log_info "WAL归档备份完成: ${compressed_file} (${size}, ${wal_count} files)"
        
        # 清理已备份的WAL文件
        rm -rf "$wal_archive"/*
        
        # 远程同步
        if [[ "$REMOTE_BACKUP_ENABLED" == "true" ]]; then
            sync_to_remote "$compressed_file"
        fi
    else
        log_warn "WAL归档目录不存在"
    fi
}

# ============================================
# 远程同步
# ============================================
sync_to_remote() {
    local files=("$@")
    
    log_info "同步备份到远程服务器: $REMOTE_HOST"
    
    # 创建远程目录
    ssh "${REMOTE_USER}@${REMOTE_HOST}" "mkdir -p ${REMOTE_PATH}/$(get_date)"
    
    # 同步文件
    for file in "${files[@]}"; do
        if [[ -f "$file" ]]; then
            rsync -avz --progress "$file" "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH}/$(get_date)/"
        fi
    done
    
    log_info "远程同步完成"
}

# ============================================
# 清理过期备份
# ============================================
cleanup_old_backups() {
    log_info "清理过期备份..."
    
    # 清理本地备份
    # 每日备份保留30天
    find "${BACKUP_DIR}/full" -name "*.dump" -mtime +${RETENTION_DAYS} -delete 2>/dev/null || true
    find "${BACKUP_DIR}/full" -name "*.meta" -mtime +${RETENTION_DAYS} -delete 2>/dev/null || true
    find "${BACKUP_DIR}/incremental" -name "*.tar.*" -mtime +${RETENTION_DAYS} -delete 2>/dev/null || true
    find "${BACKUP_DIR}/wal" -name "*.tar.*" -mtime +7 -delete 2>/dev/null || true
    
    # 清理远程备份 (保留每周一个全量备份)
    if [[ "$REMOTE_BACKUP_ENABLED" == "true" ]]; then
        ssh "${REMOTE_USER}@${REMOTE_HOST}" \
            "find ${REMOTE_PATH} -name '*.dump' -mtime +$((RETENTION_WEEKS * 7)) -delete" 2>/dev/null || true
    fi
    
    log_info "过期备份清理完成"
}

# ============================================
# 验证备份完整性
# ============================================
verify_backup() {
    local backup_file="$1"
    
    log_info "验证备份完整性: $backup_file"
    
    if [[ ! -f "$backup_file" ]]; then
        log_error "备份文件不存在: $backup_file"
        return 1
    fi
    
    # 检查文件完整性
    local meta_file="${backup_file%.dump}.meta"
    if [[ -f "$meta_file" ]]; then
        local expected_checksum=$(grep "checksum=" "$meta_file" | cut -d= -f2)
        local actual_checksum=$(sha256sum "$backup_file" | cut -d' ' -f1)
        
        if [[ "$expected_checksum" == "$actual_checksum" ]]; then
            log_info "校验和验证通过"
        else
            log_error "校验和不匹配! 预期: $expected_checksum, 实际: $actual_checksum"
            return 1
        fi
    fi
    
    # 检查备份可读性
    if [[ "$backup_file" == *.dump ]]; then
        PGPASSWORD="${DB_PASSWORD}" pg_restore --list "$backup_file" > /dev/null 2>&1
        if [[ $? -eq 0 ]]; then
            log_info "备份文件可读性验证通过"
        else
            log_error "备份文件损坏"
            return 1
        fi
    fi
    
    log_info "备份验证完成"
}

# ============================================
# 主程序
# ============================================
main() {
    local mode="${1:---full}"
    
    # 创建备份目录
    mkdir -p "${BACKUP_DIR}"/{full,incremental,wal}
    
    case "$mode" in
        --full|-f)
            full_backup
            ;;
        --incremental|-i)
            incremental_backup
            ;;
        --wal|-w)
            wal_backup
            ;;
        --all|-a)
            full_backup
            incremental_backup
            wal_backup
            ;;
        --cleanup|-c)
            cleanup_old_backups
            ;;
        --verify|-v)
            verify_backup "$2"
            ;;
        *)
            echo "用法: $0 [--full|--incremental|--wal|--all|--cleanup|--verify <file>]"
            echo ""
            echo "选项:"
            echo "  --full, -f        执行全量备份"
            echo "  --incremental, -i 执行增量备份"
            echo "  --wal, -w         执行WAL归档备份"
            echo "  --all, -a         执行所有备份"
            echo "  --cleanup, -c     清理过期备份"
            echo "  --verify, -v      验证备份文件"
            exit 1
            ;;
    esac
    
    # 清理过期备份
    cleanup_old_backups
}

# 执行
main "$@"
