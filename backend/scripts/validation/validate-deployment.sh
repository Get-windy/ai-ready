#!/bin/bash
# =============================================================================
# Sprint 27+1 - 测试环境部署验证脚本
# =============================================================================
# 功能：一键验证测试环境部署状态
# 用法：./validate-deployment.sh [options]
# 选项：
#   -m, --mode <docker|manual>   验证模式 (默认: docker)
#   -s, --service <name>         指定验证服务 (默认: all)
#   -o, --output <file>          输出报告路径 (默认: stdout)
#   -q, --quiet                  静默模式，仅输出结果
#   -h, --help                   显示帮助
# =============================================================================

set -euo pipefail

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 全局变量
MODE="docker"
SERVICE="all"
OUTPUT=""
QUIET=false
FAILED=0
PASSED=0
WARNINGS=0
REPORT_DATA=""
TIMESTAMP=$(date +"%Y-%m-%d %H:%M:%S")

# 服务配置
SERVICES=(
    "postgres:5432"
    "redis:6379"
    "rabbitmq:5672"
    "app:8080"
    "nginx:80"
    "prometheus:9090"
    "grafana:3000"
)

# 帮助信息
show_help() {
    cat << EOF
Sprint 27+1 测试环境部署验证脚本

用法: $(basename "$0") [options]

选项:
  -m, --mode <docker|manual>   验证模式 (默认: docker)
  -s, --service <name>         指定验证服务 (默认: all)
  -o, --output <file>          输出报告路径 (默认: stdout)
  -q, --quiet                  静默模式，仅输出结果
  -h, --help                   显示帮助信息

示例:
  $(basename "$0")                          # 验证所有Docker服务
  $(basename "$0") -m manual                # 验证手动部署环境
  $(basename "$0") -s postgres              # 仅验证PostgreSQL
  $(basename "$0") -o report.md             # 输出Markdown报告

验证项:
  1. 容器/服务运行状态
  2. 端口连通性
  3. 依赖服务健康检查
  4. API接口可用性
  5. 数据库连接
  6. 日志检查
  7. 资源配置检查

退出码:
  0  - 所有验证通过
  1  - 存在验证失败
  2  - 脚本执行错误
EOF
}

# 日志函数
log() {
    if [ "$QUIET" = false ]; then
        echo -e "${BLUE}[$(date +%H:%M:%S)]${NC} $1"
    fi
}

log_ok() {
    if [ "$QUIET" = false ]; then
        echo -e "${GREEN}[✓]${NC} $1"
    fi
    ((PASSED++)) || true
}

log_fail() {
    if [ "$QUIET" = false ]; then
        echo -e "${RED}[✗]${NC} $1"
    fi
    ((FAILED++)) || true
}

log_warn() {
    if [ "$QUIET" = false ]; then
        echo -e "${YELLOW}[!]${NC} $1"
    fi
    ((WARNINGS++)) || true
}

# 解析参数
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -m|--mode)
                MODE="$2"
                shift 2
                ;;
            -s|--service)
                SERVICE="$2"
                shift 2
                ;;
            -o|--output)
                OUTPUT="$2"
                shift 2
                ;;
            -q|--quiet)
                QUIET=true
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            *)
                echo "未知选项: $1"
                show_help
                exit 2
                ;;
        esac
    done
}

# 检查前置条件
check_prerequisites() {
    log "检查前置条件..."
    
    if [ "$MODE" = "docker" ]; then
        if ! command -v docker &> /dev/null; then
            log_fail "Docker 未安装"
            return 1
        fi
        if ! docker info &> /dev/null; then
            log_fail "Docker 守护进程未运行"
            return 1
        fi
        log_ok "Docker 环境正常"
    fi
    
    if ! command -v curl &> /dev/null; then
        log_fail "curl 未安装"
        return 1
    fi
    log_ok "curl 已安装"
    
    if ! command -v nc &> /dev/null; then
        log_warn "nc (netcat) 未安装，端口检查将使用备用方法"
    fi
    
    return 0
}

# 验证容器状态
validate_containers() {
    log "验证容器运行状态..."
    
    local containers
    containers=$(docker ps --format "{{.Names}}")
    
    if [ -z "$containers" ]; then
        log_fail "没有运行中的容器"
        return 1
    fi
    
    for service_pair in "${SERVICES[@]}"; do
        local svc_name="${service_pair%%:*}"
        if echo "$containers" | grep -q "\b${svc_name}\b"; then
            log_ok "容器 ${svc_name} 运行中"
        else
            log_fail "容器 ${svc_name} 未运行"
        fi
    done
}

# 验证端口连通性
validate_ports() {
    log "验证端口连通性..."
    
    for service_pair in "${SERVICES[@]}"; do
        local svc_name="${service_pair%%:*}"
        local svc_port="${service_pair##*:}"
        
        if [ "$SERVICE" != "all" ] && [ "$SERVICE" != "$svc_name" ]; then
            continue
        fi
        
        if command -v nc &> /dev/null; then
            if nc -z localhost "$svc_port" 2>/dev/null; then
                log_ok "端口 ${svc_port} (${svc_name}) 可连通"
            else
                log_fail "端口 ${svc_port} (${svc_name}) 不可连通"
            fi
        else
            if timeout 2 bash -c "echo > /dev/tcp/localhost/${svc_port}" 2>/dev/null; then
                log_ok "端口 ${svc_port} (${svc_name}) 可连通"
            else
                log_fail "端口 ${svc_port} (${svc_name}) 不可连通"
            fi
        fi
    done
}

# 验证服务健康检查端点
validate_health_endpoints() {
    log "验证健康检查端点..."
    
    local endpoints=(
        "http://localhost:8080/actuator/health:app"
        "http://localhost:9090/-/healthy:prometheus"
        "http://localhost:3000/api/health:grafana"
    )
    
    for endpoint_pair in "${endpoints[@]}"; do
        local url="${endpoint_pair%%:*}"
        local svc="${endpoint_pair##*:}"
        
        if [ "$SERVICE" != "all" ] && [ "$SERVICE" != "$svc" ]; then
            continue
        fi
        
        local response
        response=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "000")
        
        if [ "$response" = "200" ]; then
            log_ok "健康检查端点 ${svc} 正常 (HTTP ${response})"
        else
            log_fail "健康检查端点 ${svc} 异常 (HTTP ${response})"
        fi
    done
}

# 验证数据库连接
validate_database() {
    log "验证数据库连接..."
    
    if [ "$SERVICE" != "all" ] && [ "$SERVICE" != "postgres" ]; then
        return 0
    fi
    
    # 检查PostgreSQL
    if docker exec postgres pg_isready -U test_user &> /dev/null; then
        log_ok "PostgreSQL 数据库可连接"
    else
        log_fail "PostgreSQL 数据库连接失败"
    fi
    
    # 检查Redis
    if [ "$SERVICE" = "all" ] || [ "$SERVICE" = "redis" ]; then
        if docker exec redis redis-cli ping | grep -q "PONG"; then
            log_ok "Redis 可连接"
        else
            log_fail "Redis 连接失败"
        fi
    fi
}

# 验证日志
validate_logs() {
    log "验证服务日志..."
    
    for service_pair in "${SERVICES[@]}"; do
        local svc_name="${service_pair%%:*}"
        
        if [ "$SERVICE" != "all" ] && [ "$SERVICE" != "$svc_name" ]; then
            continue
        fi
        
        if [ "$MODE" = "docker" ]; then
            local log_lines
            log_lines=$(docker logs --tail 10 "$svc_name" 2>/dev/null | wc -l)
            if [ "$log_lines" -gt 0 ]; then
                log_ok "${svc_name} 日志正常 (${log_lines} 行)"
            else
                log_warn "${svc_name} 无日志输出"
            fi
        fi
    done
}

# 验证资源使用
validate_resources() {
    log "验证资源使用..."
    
    if [ "$MODE" = "docker" ]; then
        local stats
        stats=$(docker stats --no-stream --format "{{.Name}}: CPU {{.CPUPerc}} MEM {{.MemPerc}}" 2>/dev/null)
        
        while IFS= read -r line; do
            local svc_name="${line%%:*}"
            if [ "$SERVICE" != "all" ] && [ "$SERVICE" != "$svc_name" ]; then
                continue
            fi
            
            local mem_pct
            mem_pct=$(echo "$line" | grep -oP 'MEM \K[0-9.]+' || echo "0")
            
            if (( $(echo "$mem_pct > 90" | bc -l 2>/dev/null || echo "0") )); then
                log_warn "${svc_name} 内存使用率过高: ${mem_pct}%"
            else
                log_ok "${svc_name} 资源使用正常"
            fi
        done <<< "$stats"
    fi
}

# 验证服务间通信
validate_inter_service() {
    log "验证服务间通信..."
    
    # 验证App能否连接数据库
    if [ "$MODE" = "docker" ]; then
        if docker exec app curl -s http://postgres:5432 &> /dev/null || \
           docker exec app nc -z postgres 5432 &> /dev/null; then
            log_ok "App → PostgreSQL 通信正常"
        else
            log_fail "App → PostgreSQL 通信异常"
        fi
        
        # 验证App能否连接Redis
        if docker exec app nc -z redis 6379 &> /dev/null; then
            log_ok "App → Redis 通信正常"
        else
            log_fail "App → Redis 通信异常"
        fi
    fi
}

# 生成Markdown报告
generate_report() {
    local total=$((PASSED + FAILED))
    local pass_rate=0
    if [ "$total" -gt 0 ]; then
        pass_rate=$(echo "scale=1; $PASSED * 100 / $total" | bc)
    fi
    
    local status="通过"
    if [ "$FAILED" -gt 0 ]; then
        status="失败"
    elif [ "$WARNINGS" -gt 0 ]; then
        status="通过(有警告)"
    fi
    
    cat << EOF
# 部署验证报告

| 项目 | 值 |
|------|-----|
| 验证时间 | ${TIMESTAMP} |
| 验证模式 | ${MODE} |
| 验证服务 | ${SERVICE} |
| 总检查项 | ${total} |
| 通过 | ${PASSED} |
| 失败 | ${FAILED} |
| 警告 | ${WARNINGS} |
| 通过率 | ${pass_rate}% |
| 总体状态 | ${status} |

## 验证结果汇总

- 通过项: ${PASSED}
- 失败项: ${FAILED}
- 警告项: ${WARNINGS}

## 结论

$(if [ "$FAILED" -eq 0 ]; then echo "✅ 所有验证项通过，部署成功。"; else echo "❌ 存在 ${FAILED} 个验证失败项，请检查并修复。"; fi)

$(if [ "$WARNINGS" -gt 0 ]; then echo "⚠️ 存在 ${WARNINGS} 个警告项，建议关注。"; fi)

---
*报告由 validate-deployment.sh 自动生成*
EOF
}

# 主函数
main() {
    parse_args "$@"
    
    log "========================================"
    log "Sprint 27+1 测试环境部署验证"
    log "模式: ${MODE}"
    log "服务: ${SERVICE}"
    log "时间: ${TIMESTAMP}"
    log "========================================"
    
    # 检查前置条件
    if ! check_prerequisites; then
        log_fail "前置条件检查失败"
        exit 2
    fi
    
    # 执行验证
    if [ "$MODE" = "docker" ]; then
        validate_containers
    fi
    validate_ports
    validate_health_endpoints
    validate_database
    validate_logs
    validate_resources
    validate_inter_service
    
    # 输出结果
    log "========================================"
    log "验证完成"
    log "通过: ${PASSED}"
    log "失败: ${FAILED}"
    log "警告: ${WARNINGS}"
    log "========================================"
    
    # 生成报告
    local report
    report=$(generate_report)
    
    if [ -n "$OUTPUT" ]; then
        echo "$report" > "$OUTPUT"
        log "报告已保存到: ${OUTPUT}"
    else
        echo ""
        echo "$report"
    fi
    
    # 返回退出码
    if [ "$FAILED" -gt 0 ]; then
        exit 1
    fi
    exit 0
}

# 执行
main "$@"
