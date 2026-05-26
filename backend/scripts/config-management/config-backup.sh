#!/bin/bash
# 配置文件备份脚本
# 支持增量备份、差异备份、版本管理和恢复

set -euo pipefail

# 颜色输出
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

# 路径配置
readonly CONFIG_DIR="${CONFIG_DIR:-./config}"
readonly BACKUP_DIR="${BACKUP_DIR:-./backups}"
readonly SNAPSHOT_DIR="${SNAPSHOT_DIR:-${BACKUP_DIR}/snapshots}"
readonly INCREMENTAL_DIR="${INCREMENTAL_DIR:-${BACKUP_DIR}/incremental}"
readonly ARCHIVE_DIR="${ARCHIVE_DIR:-${BACKUP_DIR}/archive}"

# 备份策略配置
readonly MAX_DAILY_BACKUPS=${MAX_DAILY_BACKUPS:-7}
readonly MAX_WEEKLY_BACKUPS=${MAX_WEEKLY_BACKUPS:-4}
readonly MAX_MONTHLY_BACKUPS=${MAX_MONTHLY_BACKUPS:-12}
readonly COMPRESSION_LEVEL=${COMPRESSION_LEVEL:-6}

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# 显示帮助信息
show_help() {
    cat << EOF
配置文件备份脚本 v1.0

用法: ./config-backup.sh <command> [options]

命令:
  init             初始化备份目录结构
  full             执行完整备份
  incremental      执行增量备份
  differential     执行差异备份
  list             列出所有备份
  restore <id>     恢复指定备份
  verify <id>      验证备份完整性
  cleanup          清理旧备份
  status           显示备份状态
  schedule         设置备份计划

备份类型:
  full: 完整备份 - 备份所有配置文件
  incremental: 增量备份 - 备份自上次备份以来的变化
  differential: 差异备份 - 备份自上次完整备份以来的变化

备份策略:
  每日备份: 保留最近 ${MAX_DAILY_BACKUPS} 天
  每周备份: 保留最近 ${MAX_WEEKLY_BACKUPS} 周
  每月备份: 保留最近 ${MAX_MONTHLY_BACKUPS} 月

示例:
  ./config-backup.sh init
  ./config-backup.sh full
  ./config-backup.sh incremental
  ./config-backup.sh list
  ./config-backup.sh restore backup_20240101_120000
  ./config-backup.sh cleanup

配置目录: ${CONFIG_DIR}
备份目录: ${BACKUP_DIR}
EOF
}

# 检查必需命令
check_prerequisites() {
    local required_commands=(tar gzip find rsync)
    local missing_commands=()
    
    for cmd in "${required_commands[@]}"; do
        if ! command -v "$cmd" &> /dev/null; then
            missing_commands+=("$cmd")
        fi
    done
    
    if [ ${#missing_commands[@]} -gt 0 ]; then
        log_error "缺少必需命令: ${missing_commands[*]}"
        log_info "请安装:"
        log_info "  - tar: 归档工具"
        log_info "  - gzip: 压缩工具"
        log_info "  - find: 文件查找"
        log_info "  - rsync: 文件同步"
        exit 1
    fi
}

# 初始化备份目录
init_backup_structure() {
    log_info "初始化备份目录结构..."
    
    mkdir -p "${BACKUP_DIR}"
    mkdir -p "${SNAPSHOT_DIR}"
    mkdir -p "${INCREMENTAL_DIR}"
    mkdir -p "${ARCHIVE_DIR}"
    mkdir -p "${BACKUP_DIR}/logs"
    mkdir -p "${BACKUP_DIR}/metadata"
    
    # 创建备份索引文件
    local index_file="${BACKUP_DIR}/backup_index.json"
    if [ ! -f "$index_file" ]; then
        cat > "$index_file" << 'EOF'
{
  "backups": [],
  "last_full_backup": null,
  "last_incremental_backup": null,
  "backup_count": 0,
  "total_size": 0
}
EOF
        log_info "创建备份索引文件: $index_file"
    fi
    
    # 创建备份策略文件
    local policy_file="${BACKUP_DIR}/backup_policy.json"
    if [ ! -f "$policy_file" ]; then
        cat > "$policy_file" << 'EOF'
{
  "retention": {
    "daily": 7,
    "weekly": 4,
    "monthly": 12
  },
  "compression": {
    "enabled": true,
    "level": 6,
    "algorithm": "gzip"
  },
  "encryption": {
    "enabled": false,
    "algorithm": "aes-256-gcm"
  },
  "verification": {
    "enabled": true,
    "checksum": "sha256"
  },
  "notification": {
    "enabled": false,
    "email": "",
    "webhook": ""
  }
}
EOF
        log_info "创建备份策略文件: $policy_file"
    fi
    
    log_success "备份目录结构初始化完成"
}

# 生成备份ID
generate_backup_id() {
    local backup_type="$1"
    local timestamp=$(date +%Y%m%d_%H%M%S)
    echo "${backup_type}_${timestamp}"
}

# 计算目录大小
calculate_dir_size() {
    local dir="$1"
    if [ -d "$dir" ]; then
        du -sb "$dir" | cut -f1
    else
        echo "0"
    fi
}

# 计算文件校验和
calculate_checksum() {
    local file="$1"
    if [ -f "$file" ]; then
        sha256sum "$file" | cut -d' ' -f1
    else
        echo ""
    fi
}

# 更新备份索引
update_backup_index() {
    local backup_id="$1"
    local backup_type="$2"
    local backup_path="$3"
    local backup_size="$4"
    
    local index_file="${BACKUP_DIR}/backup_index.json"
    local metadata_file="${BACKUP_DIR}/metadata/${backup_id}.json"
    
    # 创建备份元数据
    local metadata=$(cat << EOF
{
  "id": "${backup_id}",
  "type": "${backup_type}",
  "timestamp": "$(date -Iseconds)",
  "path": "${backup_path}",
  "size": ${backup_size},
  "checksum": "$(calculate_checksum "${backup_path}.tar.gz")",
  "config_count": $(find "${CONFIG_DIR}" -type f -name "*.yml" -o -name "*.yaml" -o -name "*.json" -o -name "*.properties" | wc -l),
  "status": "completed"
}
EOF
)
    
    # 保存元数据
    echo "$metadata" | jq '.' > "$metadata_file"
    
    # 更新索引
    local current_index=$(cat "$index_file")
    local updated_index=$(echo "$current_index" | jq \
        --arg id "$backup_id" \
        --arg type "$backup_type" \
        --argjson size "$backup_size" \
        '.backups += [{"id": $id, "type": $type, "timestamp": "'$(date -Iseconds)'", "size": $size}] |
         .backup_count = (.backups | length) |
         .total_size = (.backups | map(.size) | add) |
         if $type == "full" then .last_full_backup = $id else . end |
         if $type == "incremental" then .last_incremental_backup = $id else . end')
    
    echo "$updated_index" | jq '.' > "$index_file"
    
    log_info "备份索引已更新: $backup_id"
}

# 执行完整备份
perform_full_backup() {
    local backup_id=$(generate_backup_id "full")
    local backup_file="${SNAPSHOT_DIR}/${backup_id}.tar.gz"
    local log_file="${BACKUP_DIR}/logs/${backup_id}.log"
    
    log_info "开始完整备份: $backup_id"
    log_info "备份目录: ${CONFIG_DIR}"
    log_info "备份文件: $backup_file"
    
    # 检查配置目录
    if [ ! -d "${CONFIG_DIR}" ]; then
        log_error "配置目录不存在: ${CONFIG_DIR}"
        return 1
    fi
    
    # 计算备份前大小
    local config_size=$(calculate_dir_size "${CONFIG_DIR}")
    log_info "配置目录大小: $((config_size / 1024))KB"
    
    # 创建备份清单
    local manifest_file="${BACKUP_DIR}/metadata/${backup_id}.manifest"
    find "${CONFIG_DIR}" -type f -name "*.yml" -o -name "*.yaml" -o -name "*.json" -o -name "*.properties" | sort > "$manifest_file"
    local file_count=$(wc -l < "$manifest_file")
    log_info "配置文件数量: $file_count"
    
    # 执行备份
    log_info "正在创建备份归档..."
    if tar -czf "$backup_file" -C "${CONFIG_DIR}" . 2>> "$log_file"; then
        local backup_size=$(stat -c%s "$backup_file" 2>/dev/null || stat -f%z "$backup_file")
        log_success "完整备份创建成功"
        log_info "备份文件: $backup_file"
        log_info "备份大小: $((backup_size / 1024))KB"
        log_info "压缩率: $((backup_size * 100 / config_size))%"
        
        # 更新备份索引
        update_backup_index "$backup_id" "full" "$backup_file" "$backup_size"
        
        # 验证备份
        if verify_backup "$backup_id"; then
            log_success "备份验证通过"
        else
            log_error "备份验证失败"
            return 1
        fi
    else
        log_error "备份创建失败"
        return 1
    fi
}

# 执行增量备份
perform_incremental_backup() {
    local backup_id=$(generate_backup_id "incremental")
    local backup_file="${INCREMENTAL_DIR}/${backup_id}.tar.gz"
    local log_file="${BACKUP_DIR}/logs/${backup_id}.log"
    
    log_info "开始增量备份: $backup_id"
    
    # 检查上次备份时间
    local index_file="${BACKUP_DIR}/backup_index.json"
    local last_backup_time=$(jq -r '.last_incremental_backup // .last_full_backup' "$index_file")
    
    if [ "$last_backup_time" = "null" ]; then
        log_warn "没有找到上次备份，执行完整备份"
        perform_full_backup
        return $?
    fi
    
    log_info "上次备份: $last_backup_time"
    
    # 查找自上次备份以来修改的文件
    local changed_files=$(find "${CONFIG_DIR}" -type f -newer "${BACKUP_DIR}/metadata/${last_backup_time}.json" 2>/dev/null || find "${CONFIG_DIR}" -type f -mtime -1)
    
    if [ -z "$changed_files" ]; then
        log_info "没有发现修改的文件，跳过备份"
        return 0
    fi
    
    local file_count=$(echo "$changed_files" | wc -l)
    log_info "发现 $file_count 个修改的文件"
    
    # 创建增量备份清单
    local manifest_file="${BACKUP_DIR}/metadata/${backup_id}.manifest"
    echo "$changed_files" | sort > "$manifest_file"
    
    # 执行增量备份
    log_info "正在创建增量备份..."
    if tar -czf "$backup_file" -T "$manifest_file" 2>> "$log_file"; then
        local backup_size=$(stat -c%s "$backup_file" 2>/dev/null || stat -f%z "$backup_file")
        log_success "增量备份创建成功"
        log_info "备份文件: $backup_file"
        log_info "备份大小: $((backup_size / 1024))KB"
        
        # 更新备份索引
        update_backup_index "$backup_id" "incremental" "$backup_file" "$backup_size"
        
        # 验证备份
        if verify_backup "$backup_id"; then
            log_success "备份验证通过"
        else
            log_error "备份验证失败"
            return 1
        fi
    else
        log_error "增量备份创建失败"
        return 1
    fi
}

# 执行差异备份
perform_differential_backup() {
    local backup_id=$(generate_backup_id "differential")
    local backup_file="${INCREMENTAL_DIR}/${backup_id}.tar.gz"
    local log_file="${BACKUP_DIR}/logs/${backup_id}.log"
    
    log_info "开始差异备份: $backup_id"
    
    # 检查上次完整备份时间
    local index_file="${BACKUP_DIR}/backup_index.json"
    local last_full_backup=$(jq -r '.last_full_backup' "$index_file")
    
    if [ "$last_full_backup" = "null" ]; then
        log_warn "没有找到完整备份，执行完整备份"
        perform_full_backup
        return $?
    fi
    
    log_info "上次完整备份: $last_full_backup"
    
    # 查找自上次完整备份以来修改的文件
    local changed_files=$(find "${CONFIG_DIR}" -type f -newer "${BACKUP_DIR}/metadata/${last_full_backup}.json" 2>/dev/null)
    
    if [ -z "$changed_files" ]; then
        log_info "没有发现修改的文件，跳过备份"
        return 0
    fi
    
    local file_count=$(echo "$changed_files" | wc -l)
    log_info "发现 $file_count 个修改的文件"
    
    # 创建差异备份清单
    local manifest_file="${BACKUP_DIR}/metadata/${backup_id}.manifest"
    echo "$changed_files" | sort > "$manifest_file"
    
    # 执行差异备份
    log_info "正在创建差异备份..."
    if tar -czf "$backup_file" -T "$manifest_file" 2>> "$log_file"; then
        local backup_size=$(stat -c%s "$backup_file" 2>/dev/null || stat -f%z "$backup_file")
        log_success "差异备份创建成功"
        log_info "备份文件: $backup_file"
        log_info "备份大小: $((backup_size / 1024))KB"
        
        # 更新备份索引
        update_backup_index "$backup_id" "differential" "$backup_file" "$backup_size"
        
        # 验证备份
        if verify_backup "$backup_id"; then
            log_success "备份验证通过"
        else
            log_error "备份验证失败"
            return 1
        fi
    else
        log_error "差异备份创建失败"
        return 1
    fi
}

# 验证备份完整性
verify_backup() {
    local backup_id="$1"
    local backup_file=""
    local metadata_file="${BACKUP_DIR}/metadata/${backup_id}.json"
    
    if [ ! -f "$metadata_file" ]; then
        log_error "备份元数据不存在: $metadata_file"
        return 1
    fi
    
    # 获取备份文件路径
    backup_file=$(jq -r '.path' "$metadata_file")
    
    if [ ! -f "$backup_file" ]; then
        log_error "备份文件不存在: $backup_file"
        return 1
    fi
    
    log_info "验证备份完整性: $backup_id"
    
    # 检查文件大小
    local expected_size=$(jq -r '.size' "$metadata_file")
    local actual_size=$(stat -c%s "$backup_file" 2>/dev/null || stat -f%z "$backup_file")
    
    if [ "$expected_size" != "$actual_size" ]; then
        log_error "文件大小不匹配: 期望 ${expected_size}, 实际 ${actual_size}"
        return 1
    fi
    
    # 检查校验和
    local expected_checksum=$(jq -r '.checksum' "$metadata_file")
    local actual_checksum=$(calculate_checksum "$backup_file")
    
    if [ "$expected_checksum" != "$actual_checksum" ]; then
        log_error "校验和不匹配"
        return 1
    fi
    
    # 测试解压
    log_info "测试备份文件解压..."
    if tar -tzf "$backup_file" > /dev/null 2>&1; then
        log_success "备份文件可正常解压"
        return 0
    else
        log_error "备份文件解压失败"
        return 1
    fi
}

# 列出所有备份
list_backups() {
    local index_file="${BACKUP_DIR}/backup_index.json"
    
    if [ ! -f "$index_file" ]; then
        log_error "备份索引不存在: $index_file"
        return 1
    fi
    
    log_info "备份列表:"
    
    local total_backups=$(jq '.backup_count' "$index_file")
    local total_size=$(jq '.total_size' "$index_file")
    
    echo "总备份数: $total_backups"
    echo "总大小: $((total_size / 1024 / 1024))MB"
    echo ""
    
    # 显示备份详情
    jq -r '.backups[] | "\(.id) | \(.type) | \(.timestamp) | \(.size / 1024 | floor)KB"' "$index_file" | \
    while read line; do
        echo "  $line"
    done
    
    # 显示统计信息
    local full_count=$(jq '[.backups[] | select(.type == "full")] | length' "$index_file")
    local incremental_count=$(jq '[.backups[] | select(.type == "incremental")] | length' "$index_file")
    local differential_count=$(jq '[.backups[] | select(.type == "differential")] | length' "$index_file")
    
    echo ""
    echo "备份类型统计:"
    echo "  完整备份: $full_count"
    echo "  增量备份: $incremental_count"
    echo "  差异备份: $differential_count"
}

# 恢复备份
restore_backup() {
    local backup_id="$1"
    local restore_dir="${CONFIG_DIR}_restored_$(date +%Y%m%d_%H%M%S)"
    local metadata_file="${BACKUP_DIR}/metadata/${backup_id}.json"
    
    if [ ! -f "$metadata_file" ]; then
        log_error "备份元数据不存在: $metadata_file"
        return 1
    fi
    
    local backup_file=$(jq -r '.path' "$metadata_file")
    local backup_type=$(jq -r '.type' "$metadata_file")
    
    if [ ! -f "$backup_file" ]; then
        log_error "备份文件不存在: $backup_file"
        return 1
    fi
    
    log_info "恢复备份: $backup_id (类型: $backup_type)"
    log_info "备份文件: $backup_file"
    log_info "恢复目录: $restore_dir"
    
    # 验证备份
    if ! verify_backup "$backup_id"; then
        log_error "备份验证失败，无法恢复"
        return 1
    fi
    
    # 创建恢复目录
    mkdir -p "$restore_dir"
    
    # 执行恢复
    log_info "正在恢复备份..."
    if tar -xzf "$backup_file" -C "$restore_dir" 2>&1; then
        local restored_count=$(find "$restore_dir" -type f | wc -l)
        log_success "备份恢复成功"
        log_info "恢复文件数: $restored_count"
        log_info "恢复目录: $restore_dir"
        
        # 显示恢复的文件
        log_info "恢复的文件列表:"
        find "$restore_dir" -type f | head -10 | while read file; do
            echo "  $(basename "$file")"
        done
        
        if [ "$restored_count" -gt 10 ]; then
            echo "  ... 还有 $((restored_count - 10)) 个文件"
        fi
        
        log_info "请检查恢复的文件，然后手动复制到 ${CONFIG_DIR}"
    else
        log_error "备份恢复失败"
        rm -rf "$restore_dir"
        return 1
    fi
}

# 清理旧备份
cleanup_old_backups() {
    log_info "清理旧备份..."
    
    local index_file="${BACKUP_DIR}/backup_index.json"
    if [ ! -f "$index_file" ]; then
        log_error "备份索引不存在: $index_file"
        return 1
    fi
    
    # 获取所有备份
    local backups=$(jq -c '.backups[]' "$index_file")
    local deleted_count=0
    local freed_space=0
    
    # 按时间清理
    while read backup; do
        local backup_id=$(echo "$backup" | jq -r '.id')
        local backup_timestamp=$(echo "$backup" | jq -r '.timestamp')
        local backup_size=$(echo "$backup" | jq -r '.size')
        local backup_age=$(date -d "$backup_timestamp" +%s 2>/dev/null || echo 0)
        local current_age=$(date +%s)
        local age_days=$(( (current_age - backup_age) / 86400 ))
        
        # 根据备份类型和年龄决定是否删除
        local delete_backup=false
        
        if [[ "$backup_id" == *"full"* ]]; then
            if [ "$age_days" -gt $((MAX_MONTHLY_BACKUPS * 30)) ]; then
                delete_backup=true
            fi
        elif [[ "$backup_id" == *"incremental"* ]] || [[ "$backup_id" == *"differential"* ]]; then
            if [ "$age_days" -gt $MAX_DAILY_BACKUPS ]; then
                delete_backup=true
            fi
        fi
        
        if [ "$delete_backup" = true ]; then
            log_info "删除旧备份: $backup_id (年龄: ${age_days}天)"
            
            # 删除备份文件
            local backup_file=$(jq -r '.path' <<< "$backup")
            if [ -f "$backup_file" ]; then
                rm -f "$backup_file"
                freed_space=$((freed_space + backup_size))
            fi
            
            # 删除元数据文件
            local metadata_file="${BACKUP_DIR}/metadata/${backup_id}.json"
            local manifest_file="${BACKUP_DIR}/metadata/${backup_id}.manifest"
            local log_file="${BACKUP_DIR}/logs/${backup_id}.log"
            
            rm -f "$metadata_file" "$manifest_file" "$log_file"
            
            deleted_count=$((deleted_count + 1))
        fi
    done <<< "$backups"
    
    if [ "$deleted_count" -gt 0 ]; then
        log_success "清理完成"
        log_info "删除备份数: $deleted_count"
        log_info "释放空间: $((freed_space / 1024 / 1024))MB"
        
        # 重新生成索引
        regenerate_backup_index
    else
        log_info "没有需要清理的旧备份"
    fi
}

# 重新生成备份索引
regenerate_backup_index() {
    log_info "重新生成备份索引..."
    
    local index_file="${BACKUP_DIR}/backup_index.json"
    local new_index='{"backups": [], "last_full_backup": null, "last_incremental_backup": null, "backup_count": 0, "total_size": 0}'
    
    # 查找所有元数据文件
    for metadata_file in "${BACKUP_DIR}/metadata"/*.json; do
        [ -f "$metadata_file" ] || continue
        
        local backup_data=$(cat "$metadata_file")
        local backup_id=$(jq -r '.id' <<< "$backup_data")
        local backup_type=$(jq -r '.type' <<< "$backup_data")
        local backup_size=$(jq -r '.size' <<< "$backup_data")
        
        # 检查备份文件是否存在
        local backup_file=$(jq -r '.path' <<< "$backup_data")
        if [ ! -f "$backup_file" ]; then
            log_warn "备份文件不存在: $backup_file，跳过"
            continue
        fi
        
        # 添加到新索引
        new_index=$(jq --arg id "$backup_id" \
            --arg type "$backup_type" \
            --argjson size "$backup_size" \
            --arg timestamp "$(jq -r '.timestamp' <<< "$backup_data")" \
            '.backups += [{"id": $id, "type": $type, "timestamp": $timestamp, "size": $size}] |
             .backup_count = (.backups | length) |
             .total_size = (.backups | map(.size) | add) |
             if $type == "full" and (.last_full_backup == null or $timestamp > .last_full_backup) then .last_full_backup = $id else . end |
             if $type == "incremental" and (.last_incremental_backup == null or $timestamp > .last_incremental_backup) then .last_incremental_backup = $id else . end' \
            <<< "$new_index")
    done
    
    echo "$new_index" | jq '.' > "$index_file"
    log_success "备份索引已重新生成"
}

# 显示备份状态
show_backup_status() {
    local index_file="${BACKUP_DIR}/backup_index.json"
    
    if [ ! -f "$index_file" ]; then
        log_error "备份索引不存在"
        return 1
    fi
    
    log_info "备份系统状态:"
    
    local total_backups=$(jq '.backup_count' "$index_file")
    local total_size=$(jq '.total_size' "$index_file")
    local last_full=$(jq -r '.last_full_backup // "无"' "$index_file")
    local last_incremental=$(jq -r '.last_incremental_backup // "无"' "$index_file")
    
    echo "总备份数: $total_backups"
    echo "总大小: $((total_size / 1024 / 1024))MB"
    echo "最近完整备份: $last_full"
    echo "最近增量备份: $last_incremental"
    echo ""
    
    # 磁盘使用情况
    local disk_total=$(df -h "${BACKUP_DIR}" | awk 'NR==2 {print $2}')
    local disk_used=$(df -h "${BACKUP_DIR}" | awk 'NR==2 {print $3}')
    local disk_used_percent=$(df -h "${BACKUP_DIR}" | awk 'NR==2 {print $5}')
    
    echo "磁盘使用情况:"
    echo "  总空间: $disk_total"
    echo "  已用空间: $disk_used ($disk_used_percent)"
    echo ""
    
    # 备份健康状态
    local last_backup_age=0
    if [ "$last_full" != "无" ]; then
        local last_timestamp=$(jq -r --arg id "$last_full" '.backups[] | select(.id == $id) | .timestamp' "$index_file")
        last_backup_age=$(($(date +%s) - $(date -d "$last_timestamp" +%s)))
    fi
    
    if [ "$last_backup_age" -lt 86400 ]; then
        echo -e "${GREEN}✓ 备份状态: 健康${NC}"
    elif [ "$last_backup_age" -lt 172800 ]; then
        echo -e "${YELLOW}⚠ 备份状态: 警告 (超过24小时)${NC}"
    else
        echo -e "${RED}✗ 备份状态: 危险 (超过48小时)${NC}"
    fi
}

# 设置备份计划
setup_backup_schedule() {
    log_info "设置备份计划..."
    
    local cron_file="${BACKUP_DIR}/backup.cron"
    
    cat > "$cron_file" << 'EOF'
# 配置文件备份计划
# 每天凌晨2点执行完整备份
0 2 * * * /bin/bash /path/to/config-backup.sh full >> /var/log/config-backup.log 2>&1

# 每小时执行增量备份
0 * * * * /bin/bash /path/to/config-backup.sh incremental >> /var/log/config-backup-incremental.log 2>&1

# 每周日凌晨3点清理旧备份
0 3 * * 0 /bin/bash /path/to/config-backup.sh cleanup >> /var/log/config-backup-cleanup.log 2>&1
EOF
    
    log_success "备份计划已生成: $cron_file"
    log_info "请将文件复制到 /etc/cron.d/ 目录并修改路径"
}

# 主函数
main() {
    check_prerequisites
    
    local command="${1:-help}"
    
    case "$command" in
        init)
            init_backup_structure
            ;;
        full)
            perform_full_backup
            ;;
        incremental)
            perform_incremental_backup
            ;;
        differential)
            perform_differential_backup
            ;;
        list)
            list_backups
            ;;
        restore)
            if [ -z "${2:-}" ]; then
                log_error "请指定备份ID"
                show_help
                exit 1
            fi
            restore_backup "$2"
            ;;
        verify)
            if [ -z "${2:-}" ]; then
                log_error "请指定备份ID"
                show_help
                exit 1
            fi
            verify_backup "$2"
            ;;
        cleanup)
            cleanup_old_backups
            ;;
        status)
            show_backup_status
            ;;
        schedule)
            setup_backup_schedule
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            log_error "未知命令: $command"
            show_help
            exit 1
            ;;
    esac
}

# 执行主函数
if [ "${BASH_SOURCE[0]}" = "$0" ]; then
    main "$@"
fi