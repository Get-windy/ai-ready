#!/bin/bash
# AI-Ready 健康检查脚本
# 版本: v1.0.0

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_DIR="$SCRIPT_DIR/../config"
LOG_DIR="$SCRIPT_DIR/../logs"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() {
    local level=$1
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo -e "${timestamp} [$level] $message" | tee -a "$LOG_DIR/health-$(date +%Y%m%d).log"
}

info() { log "INFO" "$@"; }
warn() { log "WARN" "${YELLOW}$@${NC}"; }
error() { log "ERROR" "${RED}$@${NC}"; }
success() { log "SUCCESS" "${GREEN}$@${NC}"; }

# 显示帮助
show_help() {
    cat << EOF
AI-Ready 健康检查脚本

用法: $(basename $0) [选项]

选项:
    -e, --env ENV           部署环境 (dev|staging|prod) [默认: dev]
    -h, --host HOST         目标主机 [默认: 从配置读取]
    -p, --port PORT         服务端口 [默认: 从配置读取]
    -t, --timeout SEC       超时时间(秒) [默认: 30]
    -v, --verbose           显示详细信息
    -q, --quiet             静默模式，只返回状态码
    --help                  显示帮助信息

退出码:
    0 - 所有检查通过
    1 - 健康检查失败
    2 - 配置错误

EOF
}

# 解析参数
ENV="dev"
HOST=""
PORT=""
TIMEOUT=30
VERBOSE=false
QUIET=false

parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -e|--env)
                ENV="$2"
                shift 2
                ;;
            -h|--host)
                HOST="$2"
                shift 2
                ;;
            -p|--port)
                PORT="$2"
                shift 2
                ;;
            -t|--timeout)
                TIMEOUT="$2"
                shift 2
                ;;
            -v|--verbose)
                VERBOSE=true
                shift
                ;;
            -q|--quiet)
                QUIET=true
                shift
                ;;
            --help)
                show_help
                exit 0
                ;;
            *)
                error "未知参数: $1"
                show_help
                exit 2
                ;;
        esac
    done
}

# 加载环境配置
load_env_config() {
    local env_file="$CONFIG_DIR/$ENV.env"
    
    if [[ ! -f "$env_file" ]]; then
        error "环境配置文件不存在: $env_file"
        exit 2
    fi
    
    source "$env_file"
    
    # 使用命令行参数覆盖配置
    [[ -z "$HOST" ]] && HOST="$DEPLOY_HOST"
    [[ -z "$PORT" ]] && PORT="$APP_PORT"
}

# 检查HTTP端点
check_http_endpoint() {
    local endpoint=$1
    local expected_code=${2:-200}
    local url="http://$HOST:$PORT$endpoint"
    
    [[ "$VERBOSE" == true ]] && info "检查端点: $url"
    
    local response_code=$(curl -s -o /dev/null -w '%{http_code}' \
        --max-time "$TIMEOUT" \
        "$url" 2>/dev/null || echo "000")
    
    if [[ "$response_code" == "$expected_code" ]]; then
        [[ "$VERBOSE" == true ]] && success "端点检查通过: $endpoint ($response_code)"
        return 0
    else
        [[ "$VERBOSE" == true ]] && error "端点检查失败: $endpoint (期望: $expected_code, 实际: $response_code)"
        return 1
    fi
}

# 检查服务健康状态
check_service_health() {
    local service_name=$1
    
    [[ "$QUIET" == false ]] && info "检查服务: $service_name"
    
    case $service_name in
        api)
            check_http_endpoint "/actuator/health" 200
            ;;
        agent)
            check_http_endpoint "/health" 200
            ;;
        nlp)
            check_http_endpoint "/api/nlp/health" 200
            ;;
        *)
            warn "未知服务: $service_name"
            return 1
            ;;
    esac
}

# 主检查流程
main() {
    parse_args "$@"
    
    mkdir -p "$LOG_DIR"
    
    load_env_config
    
    [[ "$QUIET" == false ]] && info "开始健康检查 - 环境: $ENV, 主机: $HOST:$PORT"
    
    local failed=0
    
    # 检查API服务
    if ! check_service_health "api"; then
        ((failed++))
    fi
    
    # 检查Agent服务
    if ! check_service_health "agent"; then
        ((failed++))
    fi
    
    # 检查NLP服务
    if ! check_service_health "nlp"; then
        ((failed++))
    fi
    
    if [[ $failed -eq 0 ]]; then
        [[ "$QUIET" == false ]] && success "所有健康检查通过"
        exit 0
    else
        [[ "$QUIET" == false ]] && error "健康检查失败: $failed 个服务未通过"
        exit 1
    fi
}

main "$@"
