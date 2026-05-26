#!/bin/bash
# 测试环境网络连通性验证脚本
# 用法: ./validate-network.sh [all|gateway|dns|ports]

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 网络配置
GATEWAYS=("10.0.0.1" "10.0.1.1" "10.0.2.1" "10.0.3.1")
DNS_SERVER="10.0.0.2"
DOMAINS=("test.ai-ready.local" "test-api.ai-ready.local" "test-db.ai-ready.local" "test-redis.ai-ready.local")
SERVICES=(
    "10.0.1.100:80:Web服务"
    "10.0.2.10:8080:用户服务"
    "10.0.2.20:8081:订单服务"
    "10.0.3.10:5432:PostgreSQL"
    "10.0.3.20:6379:Redis"
    "10.0.3.30:5672:RabbitMQ"
    "10.0.0.10:3000:Grafana"
    "10.0.0.11:9090:Prometheus"
)

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        log_error "命令 $1 未找到，请先安装"
        return 1
    fi
    return 0
}

# 测试网关连通性
test_gateways() {
    log_info "=== 测试网关连通性 ==="
    local all_pass=true
    
    for gateway in "${GATEWAYS[@]}"; do
        if ping -c 2 -W 1 $gateway &> /dev/null; then
            log_info "✓ 网关 $gateway 可达"
        else
            log_error "✗ 网关 $gateway 不可达"
            all_pass=false
        fi
    done
    
    if $all_pass; then
        log_info "所有网关测试通过"
    else
        log_warn "部分网关测试失败"
    fi
}

# 测试DNS解析
test_dns() {
    log_info "=== 测试DNS解析 ==="
    check_command nslookup || return 1
    
    local all_pass=true
    
    for domain in "${DOMAINS[@]}"; do
        if nslookup $domain $DNS_SERVER &> /dev/null; then
            log_info "✓ 域名 $domain 解析成功"
        else
            log_error "✗ 域名 $domain 解析失败"
            all_pass=false
        fi
    done
    
    if $all_pass; then
        log_info "所有DNS解析测试通过"
    else
        log_warn "部分DNS解析测试失败"
    fi
}

# 测试服务端口
test_ports() {
    log_info "=== 测试服务端口 ==="
    check_command nc || return 1
    
    local all_pass=true
    
    for service in "${SERVICES[@]}"; do
        IFS=':' read -r ip port name <<< "$service"
        
        if timeout 2 nc -zv $ip $port &> /dev/null; then
            log_info "✓ $name ($ip:$port) 端口正常"
        else
            log_error "✗ $name ($ip:$port) 端口异常"
            all_pass=false
        fi
    done
    
    if $all_pass; then
        log_info "所有服务端口测试通过"
    else
        log_warn "部分服务端口测试失败"
    fi
}

# 测试网络延迟
test_latency() {
    log_info "=== 测试网络延迟 ==="
    
    for gateway in "${GATEWAYS[@]}"; do
        if ping -c 4 $gateway &> /dev/null; then
            local avg_latency=$(ping -c 4 $gateway | tail -1 | awk -F '/' '{print $5}')
            log_info "网关 $gateway 平均延迟: ${avg_latency}ms"
        fi
    done
}

# 测试网络带宽（简化版）
test_bandwidth() {
    log_info "=== 测试网络带宽 ==="
    log_warn "网络带宽测试需要特定工具，跳过..."
}

# 生成测试报告
generate_report() {
    local report_file="network-validation-report-$(date +%Y%m%d-%H%M%S).md"
    
    cat > $report_file << EOF
# 网络验证报告

## 测试信息
- **测试时间**: $(date)
- **测试环境**: AI-Ready测试环境
- **测试脚本版本**: 1.0

## 测试结果

### 网关连通性
EOF
    
    for gateway in "${GATEWAYS[@]}"; do
        if ping -c 2 -W 1 $gateway &> /dev/null; then
            echo "- ✅ $gateway: 正常" >> $report_file
        else
            echo "- ❌ $gateway: 异常" >> $report_file
        fi
    done
    
    cat >> $report_file << EOF

### DNS解析
EOF
    
    for domain in "${DOMAINS[@]}"; do
        if nslookup $domain $DNS_SERVER &> /dev/null; then
            echo "- ✅ $domain: 解析成功" >> $report_file
        else
            echo "- ❌ $domain: 解析失败" >> $report_file
        fi
    done
    
    cat >> $report_file << EOF

### 服务端口
EOF
    
    for service in "${SERVICES[@]}"; do
        IFS=':' read -r ip port name <<< "$service"
        
        if timeout 2 nc -zv $ip $port &> /dev/null; then
            echo "- ✅ $name ($ip:$port): 正常" >> $report_file
        else
            echo "- ❌ $name ($ip:$port): 异常" >> $report_file
        fi
    done
    
    cat >> $report_file << EOF

## 总结
- **测试完成时间**: $(date)
- **建议**: 请根据测试结果修复异常项

---
**生成脚本**: validate-network.sh
EOF
    
    log_info "测试报告已生成: $report_file"
}

# 主函数
main() {
    local test_type=${1:-"all"}
    
    log_info "开始网络验证测试..."
    log_info "测试类型: $test_type"
    
    case $test_type in
        "gateway")
            test_gateways
            ;;
        "dns")
            test_dns
            ;;
        "ports")
            test_ports
            ;;
        "latency")
            test_latency
            ;;
        "bandwidth")
            test_bandwidth
            ;;
        "all")
            test_gateways
            test_dns
            test_ports
            test_latency
            test_bandwidth
            generate_report
            ;;
        *)
            log_error "未知的测试类型: $test_type"
            log_info "可用类型: all, gateway, dns, ports, latency, bandwidth"
            exit 1
            ;;
    esac
    
    log_info "网络验证测试完成"
}

# 执行主函数
main "$@"