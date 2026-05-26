#!/bin/bash
# 环境变量管理脚本
# 支持环境变量的创建、读取、验证和导出

set -euo pipefail

# 颜色输出
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

# 配置文件路径
readonly ENV_DIR="${ENV_DIR:-./env}"
readonly TEMPLATES_DIR="${TEMPLATES_DIR:-./env/templates}"

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
环境变量管理脚本 v1.0

用法: ./env-var-manager.sh <command> [options]

命令:
  init             初始化环境变量目录结构
  create <env>     创建新的环境配置文件
  validate <env>   验证环境配置文件
  export <env>     导出环境变量到当前shell
  show <env>       显示环境配置内容
  list             列出所有可用环境配置
  backup <env>     备份环境配置文件
  restore <env>    恢复环境配置文件

示例:
  ./env-var-manager.sh init
  ./env-var-manager.sh create development
  ./env-var-manager.sh validate production
  ./env-var-manager.sh export development

环境目录: ${ENV_DIR}
模板目录: ${TEMPLATES_DIR}
EOF
}

# 检查必需命令
check_prerequisites() {
    local required_commands=(jq yq)
    local missing_commands=()
    
    for cmd in "${required_commands[@]}"; do
        if ! command -v "$cmd" &> /dev/null; then
            missing_commands+=("$cmd")
        fi
    done
    
    if [ ${#missing_commands[@]} -gt 0 ]; then
        log_error "缺少必需命令: ${missing_commands[*]}"
        log_info "请安装:"
        log_info "  - jq: JSON处理器 (apt-get install jq)"
        log_info "  - yq: YAML处理器 (pip install yq)"
        exit 1
    fi
}

# 初始化目录结构
init_environment() {
    log_info "初始化环境变量目录结构..."
    
    mkdir -p "${ENV_DIR}"
    mkdir -p "${TEMPLATES_DIR}"
    
    # 创建基础模板
    cat > "${TEMPLATES_DIR}/base.env.template" << 'EOF'
# 数据库配置
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-ai_ready}
DB_USER=${DB_USER:-postgres}
DB_PASSWORD=${DB_PASSWORD:-password}

# Redis配置
REDIS_HOST=${REDIS_HOST:-localhost}
REDIS_PORT=${REDIS_PORT:-6379}
REDIS_PASSWORD=${REDIS_PASSWORD:-}

# 应用配置
APP_NAME=${APP_NAME:-AI-Ready}
APP_ENV=${APP_ENV:-development}
APP_PORT=${APP_PORT:-8080}
APP_LOG_LEVEL=${APP_LOG_LEVEL:-INFO}

# 安全配置
JWT_SECRET=${JWT_SECRET:-your-secret-key}
ENCRYPTION_KEY=${ENCRYPTION_KEY:-your-encryption-key}

# 外部服务
API_TIMEOUT=${API_TIMEOUT:-30}
MAX_RETRIES=${MAX_RETRIES:-3}
EOF

    # 创建开发环境模板
    cat > "${TEMPLATES_DIR}/development.env.template" << 'EOF'
# 开发环境配置
# 继承自 base.env.template

APP_ENV=development
APP_LOG_LEVEL=DEBUG

DB_HOST=localhost
DB_PORT=5432
DB_NAME=ai_ready_dev

# 开发环境特定配置
DEBUG_MODE=true
ENABLE_SWAGGER=true
CORS_ALLOWED_ORIGINS=*

# 开发工具
ENABLE_HOT_RELOAD=true
ENABLE_DEBUG_TOOLS=true
EOF

    # 创建示例环境
    cat > "${ENV_DIR}/.env.example" << 'EOF'
# 环境配置文件示例
# 复制此文件为 .env.development 并修改值

DB_HOST=localhost
DB_PORT=5432
DB_NAME=ai_ready_dev
DB_USER=postgres
DB_PASSWORD=your_password_here

APP_PORT=8080
APP_LOG_LEVEL=INFO
EOF

    log_success "环境变量目录结构初始化完成"
    log_info "模板文件已创建:"
    log_info "  - ${TEMPLATES_DIR}/base.env.template"
    log_info "  - ${TEMPLATES_DIR}/development.env.template"
    log_info "  - ${ENV_DIR}/.env.example"
}

# 创建环境配置文件
create_environment() {
    local env_name="$1"
    local env_file="${ENV_DIR}/.env.${env_name}"
    
    if [ -f "$env_file" ]; then
        log_warn "环境配置文件已存在: $env_file"
        read -p "是否覆盖? (y/n): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "操作取消"
            return 1
        fi
    fi
    
    log_info "创建环境配置文件: $env_file"
    
    # 选择模板
    local template_file="${TEMPLATES_DIR}/${env_name}.env.template"
    if [ ! -f "$template_file" ]; then
        template_file="${TEMPLATES_DIR}/base.env.template"
        log_info "使用基础模板: $template_file"
    else
        log_info "使用环境专用模板: $template_file"
    fi
    
    # 复制模板
    cp "$template_file" "$env_file"
    
    # 替换占位符
    sed -i "s/\${DB_NAME:-ai_ready}/ai_ready_${env_name}/g" "$env_file"
    sed -i "s/\${APP_NAME:-AI-Ready}/AI-Ready ${env_name}/g" "$env_file"
    
    log_success "环境配置文件已创建: $env_file"
    log_info "请编辑此文件并设置实际的值"
}

# 验证环境配置文件
validate_environment() {
    local env_name="$1"
    local env_file="${ENV_DIR}/.env.${env_name}"
    
    if [ ! -f "$env_file" ]; then
        log_error "环境配置文件不存在: $env_file"
        return 1
    fi
    
    log_info "验证环境配置文件: $env_file"
    
    local validation_errors=0
    
    # 检查必需变量
    local required_vars=("DB_HOST" "DB_PORT" "DB_NAME" "DB_USER" "APP_PORT")
    for var in "${required_vars[@]}"; do
        if ! grep -q "^${var}=" "$env_file"; then
            log_error "缺失必需变量: $var"
            validation_errors=$((validation_errors + 1))
        fi
    done
    
    # 检查敏感变量是否使用默认值
    if grep -q "password_here\|your-secret-key\|your-encryption-key" "$env_file"; then
        log_warn "检测到默认值的安全变量，请修改"
    fi
    
    # 检查端口范围
    local app_port=$(grep "^APP_PORT=" "$env_file" | cut -d'=' -f2)
    if [ -n "$app_port" ] && ([ "$app_port" -lt 1024 ] || [ "$app_port" -gt 65535 ]); then
        log_error "APP_PORT 必须在 1024-65535 范围内"
        validation_errors=$((validation_errors + 1))
    fi
    
    # 检查URL格式
    if grep -q "http://\|https://" "$env_file"; then
        local urls=$(grep -o "https\?://[^ ]*" "$env_file")
        for url in $urls; do
            if ! [[ $url =~ ^https?://[a-zA-Z0-9.-]+\.[a-zA-Z]{2,} ]]; then
                log_warn "URL格式可能不正确: $url"
            fi
        done
    fi
    
    if [ $validation_errors -eq 0 ]; then
        log_success "环境配置文件验证通过"
        return 0
    else
        log_error "环境配置文件验证失败，发现 $validation_errors 个问题"
        return 1
    fi
}

# 导出环境变量
export_environment() {
    local env_name="$1"
    local env_file="${ENV_DIR}/.env.${env_name}"
    
    if [ ! -f "$env_file" ]; then
        log_error "环境配置文件不存在: $env_file"
        return 1
    fi
    
    log_info "导出环境变量: $env_file"
    
    # 验证环境配置
    if ! validate_environment "$env_name"; then
        log_error "环境配置验证失败，无法导出"
        return 1
    fi
    
    # 导出变量到当前shell
    set -a
    source "$env_file"
    set +a
    
    log_success "环境变量已导出到当前shell"
    log_info "当前环境: APP_ENV=${APP_ENV:-未设置}, APP_PORT=${APP_PORT:-未设置}"
}

# 显示环境配置内容
show_environment() {
    local env_name="$1"
    local env_file="${ENV_DIR}/.env.${env_name}"
    
    if [ ! -f "$env_file" ]; then
        log_error "环境配置文件不存在: $env_file"
        return 1
    fi
    
    log_info "环境配置内容: $env_file"
    echo "========================================="
    
    # 按类别显示
    echo -e "${BLUE}数据库配置:${NC}"
    grep -E "^(DB_|POSTGRES_|MYSQL_)" "$env_file" || echo "无"
    echo
    
    echo -e "${GREEN}应用配置:${NC}"
    grep -E "^(APP_|SERVER_)" "$env_file" || echo "无"
    echo
    
    echo -e "${YELLOW}安全配置:${NC}"
    grep -E "^(JWT_|SECRET_|ENCRYPTION_)" "$env_file" | sed 's/=.*/=[HIDDEN]/' || echo "无"
    echo
    
    echo -e "${BLUE}外部服务:${NC}"
    grep -E "^(API_|REDIS_|RABBITMQ_|AWS_)" "$env_file" || echo "无"
    echo "========================================="
}

# 列出所有环境配置
list_environments() {
    log_info "可用环境配置列表:"
    
    if [ ! -d "$ENV_DIR" ]; then
        log_error "环境目录不存在: $ENV_DIR"
        return 1
    fi
    
    local env_files=$(find "$ENV_DIR" -name ".env.*" -type f | sort)
    
    if [ -z "$env_files" ]; then
        log_info "没有找到环境配置文件"
        return 0
    fi
    
    echo -e "${BLUE}环境文件:${NC}"
    for env_file in $env_files; do
        local env_name=$(basename "$env_file" | sed 's/^\.env\.//')
        local file_size=$(stat -c%s "$env_file" 2>/dev/null || stat -f%z "$env_file")
        local modified=$(stat -c%y "$env_file" 2>/dev/null || stat -f%Sm "$env_file")
        
        echo -e "  ${GREEN}${env_name}${NC}:"
        echo "    文件: $env_file"
        echo "    大小: $((file_size / 1024))KB"
        echo "    修改: $modified"
        
        # 检查验证状态
        if validate_environment "$env_name" &> /dev/null; then
            echo -e "    状态: ${GREEN}✓ 验证通过${NC}"
        else
            echo -e "    状态: ${RED}✗ 验证失败${NC}"
        fi
        echo
    done
    
    echo -e "${BLUE}模板文件:${NC}"
    local templates=$(find "$TEMPLATES_DIR" -name "*.template" -type f | sort)
    for template in $templates; do
        echo "  $(basename "$template")"
    done
}

# 备份环境配置
backup_environment() {
    local env_name="$1"
    local env_file="${ENV_DIR}/.env.${env_name}"
    local backup_dir="${ENV_DIR}/backup"
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local backup_file="${backup_dir}/${env_name}_${timestamp}.env.bak"
    
    if [ ! -f "$env_file" ]; then
        log_error "环境配置文件不存在: $env_file"
        return 1
    fi
    
    mkdir -p "$backup_dir"
    
    # 创建备份
    cp "$env_file" "$backup_file"
    
    # 加密敏感信息
    if command -v gpg &> /dev/null; then
        log_info "加密备份文件..."
        echo "password" | gpg --batch --yes --passphrase-fd 0 -c "$backup_file"
        rm "$backup_file"
        backup_file="${backup_file}.gpg"
        log_info "备份已加密: $backup_file"
    else
        log_warn "未找到gpg，备份文件未加密"
    fi
    
    log_success "环境配置已备份: $backup_file"
    
    # 清理旧备份（保留最近10个）
    find "$backup_dir" -name "*${env_name}*.bak*" -type f | sort -r | tail -n +11 | xargs rm -f 2>/dev/null || true
}

# 恢复环境配置
restore_environment() {
    local env_name="$1"
    local backup_dir="${ENV_DIR}/backup"
    local env_file="${ENV_DIR}/.env.${env_name}"
    
    if [ ! -d "$backup_dir" ]; then
        log_error "备份目录不存在: $backup_dir"
        return 1
    fi
    
    # 查找最新的备份文件
    local latest_backup=$(find "$backup_dir" -name "*${env_name}*.bak*" -type f | sort -r | head -1)
    
    if [ -z "$latest_backup" ]; then
        log_error "未找到备份文件: ${backup_dir}/*${env_name}*.bak*"
        return 1
    fi
    
    log_info "找到备份文件: $latest_backup"
    
    # 解密和恢复
    if [[ "$latest_backup" == *.gpg ]]; then
        log_info "解密备份文件..."
        gpg --decrypt "$latest_backup" > "${env_file}.restored"
        mv "${env_file}.restored" "$env_file"
    else
        cp "$latest_backup" "$env_file"
    fi
    
    log_success "环境配置已从备份恢复: $latest_backup"
    log_info "恢复的文件: $env_file"
    
    # 验证恢复的配置
    if validate_environment "$env_name"; then
        log_success "恢复的环境配置验证通过"
    else
        log_warn "恢复的环境配置验证失败，请检查"
    fi
}

# 主函数
main() {
    check_prerequisites
    
    local command="${1:-help}"
    
    case "$command" in
        init)
            init_environment
            ;;
        create)
            if [ -z "${2:-}" ]; then
                log_error "请指定环境名称"
                show_help
                exit 1
            fi
            create_environment "$2"
            ;;
        validate)
            if [ -z "${2:-}" ]; then
                log_error "请指定环境名称"
                show_help
                exit 1
            fi
            validate_environment "$2"
            ;;
        export)
            if [ -z "${2:-}" ]; then
                log_error "请指定环境名称"
                show_help
                exit 1
            fi
            export_environment "$2"
            ;;
        show)
            if [ -z "${2:-}" ]; then
                log_error "请指定环境名称"
                show_help
                exit 1
            fi
            show_environment "$2"
            ;;
        list)
            list_environments
            ;;
        backup)
            if [ -z "${2:-}" ]; then
                log_error "请指定环境名称"
                show_help
                exit 1
            fi
            backup_environment "$2"
            ;;
        restore)
            if [ -z "${2:-}" ]; then
                log_error "请指定环境名称"
                show_help
                exit 1
            fi
            restore_environment "$2"
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