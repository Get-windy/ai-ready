#!/bin/bash
# monitor-performance.sh
# 性能测试监控脚本

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$SCRIPT_DIR/../logs"
MONITOR_DIR="$SCRIPT_DIR/../monitoring"
CONFIG_DIR="$SCRIPT_DIR/../config"

# 创建监控目录
mkdir -p "$LOG_DIR" "$MONITOR_DIR"

# 日志函数
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_DIR/performance-monitor.log"
}

# 检查监控服务状态
check_monitoring_services() {
    log "检查监控服务状态..."
    
    # 检查Prometheus
    if curl -s http://localhost:9090/-/healthy > /dev/null 2>&1; then
        log "✓ Prometheus服务正常"
    else
        log "✗ Prometheus服务异常"
        return 1
    fi
    
    # 检查Grafana
    if curl -s http://localhost:3000/api/health > /dev/null 2>&1; then
        log "✓ Grafana服务正常"
    else
        log "✗ Grafana服务异常"
        return 1
    fi
    
    # 检查Node Exporter
    if curl -s http://localhost:9100/metrics > /dev/null 2>&1; then
        log "✓ Node Exporter服务正常"
    else
        log "✗ Node Exporter服务异常"
        return 1
    fi
    
    return 0
}

# 收集系统性能指标
collect_system_metrics() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    local metrics_file="$MONITOR_DIR/system-metrics-$(date '+%Y%m%d_%H%M%S').csv"
    
    # 创建CSV文件头
    echo "timestamp,cpu_usage_percent,memory_usage_percent,disk_usage_percent,network_rx_kbps,network_tx_kbps" > "$metrics_file"
    
    log "收集系统性能指标..."
    
    # 收集CPU使用率
    local cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)
    
    # 收集内存使用率
    local memory_total=$(free -m | awk '/^Mem:/{print $2}')
    local memory_used=$(free -m | awk '/^Mem:/{print $3}')
    local memory_usage=$((memory_used * 100 / memory_total))
    
    # 收集磁盘使用率
    local disk_usage=$(df -h / | awk 'NR==2 {print $5}' | cut -d'%' -f1)
    
    # 收集网络流量
    local network_rx=$(cat /proc/net/dev | grep eth0 | awk '{print $2}')
    local network_tx=$(cat /proc/net/dev | grep eth0 | awk '{print $10}')
    sleep 1
    local network_rx2=$(cat /proc/net/dev | grep eth0 | awk '{print $2}')
    local network_tx2=$(cat /proc/net/dev | grep eth0 | awk '{print $10}')
    
    local network_rx_kbps=$(((network_rx2 - network_rx) / 1024))
    local network_tx_kbps=$(((network_tx2 - network_tx) / 1024))
    
    # 写入CSV文件
    echo "$timestamp,$cpu_usage,$memory_usage,$disk_usage,$network_rx_kbps,$network_tx_kbps" >> "$metrics_file"
    
    log "✓ 系统指标收集完成: CPU=$cpu_usage%, 内存=$memory_usage%, 磁盘=$disk_usage%"
}

# 收集应用性能指标
collect_application_metrics() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    local metrics_file="$MONITOR_DIR/application-metrics-$(date '+%Y%m%d_%H%M%S').csv"
    
    # 创建CSV文件头
    echo "timestamp,service_name,response_time_ms,request_count,error_count,throughput_rps,active_connections" > "$metrics_file"
    
    log "收集应用性能指标..."
    
    # 从Prometheus获取应用指标
    local prometheus_url="http://localhost:9090/api/v1/query"
    
    # 检查各个服务的健康状态
    local services=("user-service" "order-service" "inventory-service" "finance-service")
    
    for service in "${services[@]}"; do
        # 获取响应时间
        local response_time_query="http_response_time_milliseconds{service=\"$service\"}"
        local response_time_result=$(curl -s "$prometheus_url" --data-urlencode "query=$response_time_query" | jq -r '.data.result[0].value[1]' 2>/dev/null || echo "0")
        
        # 获取请求数量
        local request_count_query="http_requests_total{service=\"$service\"}"
        local request_count_result=$(curl -s "$prometheus_url" --data-urlencode "query=$request_count_query" | jq -r '.data.result[0].value[1]' 2>/dev/null || echo "0")
        
        # 获取错误数量
        local error_count_query="http_errors_total{service=\"$service\"}"
        local error_count_result=$(curl -s "$prometheus_url" --data-urlencode "query=$error_count_query" | jq -r '.data.result[0].value[1]' 2>/dev/null || echo "0")
        
        # 计算吞吐量（假设1秒窗口）
        local throughput_query="rate(http_requests_total{service=\"$service\"}[1s])"
        local throughput_result=$(curl -s "$prometheus_url" --data-urlencode "query=$throughput_query" | jq -r '.data.result[0].value[1]' 2>/dev/null || echo "0")
        
        # 获取活跃连接数
        local connections_query="http_active_connections{service=\"$service\"}"
        local connections_result=$(curl -s "$prometheus_url" --data-urlencode "query=$connections_query" | jq -r '.data.result[0].value[1]' 2>/dev/null || echo "0")
        
        # 写入CSV文件
        echo "$timestamp,$service,$response_time_result,$request_count_result,$error_count_result,$throughput_result,$connections_result" >> "$metrics_file"
        
        log "  - $service: 响应时间=${response_time_result}ms, 请求数=$request_count_result, 错误数=$error_count_result"
    done
    
    log "✓ 应用指标收集完成"
}

# 收集数据库性能指标
collect_database_metrics() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    local metrics_file="$MONITOR_DIR/database-metrics-$(date '+%Y%m%d_%H%M%S').csv"
    
    # 创建CSV文件头
    echo "timestamp,db_type,active_connections,queries_per_second,slow_queries,buffer_hit_ratio,disk_io_kbps" > "$metrics_file"
    
    log "收集数据库性能指标..."
    
    # MySQL性能指标
    local mysql_connections=$(mysql -h localhost -P 3306 -u root -p'TestEnv@2026' -N -e "SHOW STATUS LIKE 'Threads_connected'" | awk '{print $2}')
    local mysql_queries=$(mysql -h localhost -P 3306 -u root -p'TestEnv@2026' -N -e "SHOW STATUS LIKE 'Queries'" | awk '{print $2}')
    sleep 1
    local mysql_queries2=$(mysql -h localhost -P 3306 -u root -p'TestEnv@2026' -N -e "SHOW STATUS LIKE 'Queries'" | awk '{print $2}')
    local mysql_qps=$((mysql_queries2 - mysql_queries))
    
    local mysql_slow_queries=$(mysql -h localhost -P 3306 -u root -p'TestEnv@2026' -N -e "SHOW STATUS LIKE 'Slow_queries'" | awk '{print $2}')
    
    echo "$timestamp,MySQL,$mysql_connections,$mysql_qps,$mysql_slow_queries,0,0" >> "$metrics_file"
    
    # PostgreSQL性能指标
    local pg_connections=$(psql -h localhost -p 5432 -U postgres -d testdb -t -c "SELECT COUNT(*) FROM pg_stat_activity WHERE state = 'active'" | tr -d ' ')
    local pg_qps=$(psql -h localhost -p 5432 -U postgres -d testdb -t -c "SELECT SUM(xact_commit + xact_rollback) FROM pg_stat_database WHERE datname = 'testdb'" | tr -d ' ')
    
    echo "$timestamp,PostgreSQL,$pg_connections,$pg_qps,0,0,0" >> "$metrics_file"
    
    log "✓ 数据库指标收集完成: MySQL连接数=$mysql_connections, PostgreSQL连接数=$pg_connections"
}

# 检查性能阈值
check_performance_thresholds() {
    log "检查性能阈值..."
    
    local threshold_file="$CONFIG_DIR/performance-thresholds.json"
    
    if [ ! -f "$threshold_file" ]; then
        log "⚠️ 阈值配置文件不存在，使用默认阈值"
        cat > "$threshold_file" << 'EOF'
{
  "response_time": {
    "acceptable": 200,
    "warning": 500,
    "critical": 1000
  },
  "error_rate": {
    "acceptable": 0.1,
    "warning": 1,
    "critical": 5
  },
  "throughput": {
    "acceptable": 100,
    "warning": 50,
    "critical": 10
  },
  "resource_usage": {
    "cpu_acceptable": 60,
    "cpu_warning": 80,
    "cpu_critical": 90,
    "memory_acceptable": 70,
    "memory_warning": 85,
    "memory_critical": 95
  }
}
EOF
    fi
    
    # 加载阈值配置
    local thresholds=$(cat "$threshold_file")
    
    # 检查系统资源
    local cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)
    local memory_total=$(free -m | awk '/^Mem:/{print $2}')
    local memory_used=$(free -m | awk '/^Mem:/{print $3}')
    local memory_usage=$((memory_used * 100 / memory_total))
    
    # CPU检查
    if [ "$cpu_usage" -gt 90 ]; then
        log "🔴 CPU使用率超过临界阈值: ${cpu_usage}% > 90%"
    elif [ "$cpu_usage" -gt 80 ]; then
        log "🟡 CPU使用率超过警告阈值: ${cpu_usage}% > 80%"
    elif [ "$cpu_usage" -gt 60 ]; then
        log "🟢 CPU使用率可接受但偏高: ${cpu_usage}% > 60%"
    else
        log "✅ CPU使用率正常: ${cpu_usage}%"
    fi
    
    # 内存检查
    if [ "$memory_usage" -gt 95 ]; then
        log "🔴 内存使用率超过临界阈值: ${memory_usage}% > 95%"
    elif [ "$memory_usage" -gt 85 ]; then
        log "🟡 内存使用率超过警告阈值: ${memory_usage}% > 85%"
    elif [ "$memory_usage" -gt 70 ]; then
        log "🟢 内存使用率可接受但偏高: ${memory_usage}% > 70%"
    else
        log "✅ 内存使用率正常: ${memory_usage}%"
    fi
    
    log "✓ 性能阈值检查完成"
}

# 生成性能监控报告
generate_monitoring_report() {
    log "生成性能监控报告..."
    
    local report_file="$MONITOR_DIR/performance-monitoring-report-$(date '+%Y%m%d_%H%M%S').md"
    
    cat > "$report_file" << 'EOF'
# 性能监控报告

## 报告信息
- **报告时间**: REPLACE_TIMESTAMP
- **监控周期**: 实时监控
- **环境**: Sprint 27+1 测试环境
- **生成者**: 性能监控脚本

## 1. 系统资源概览

### 1.1 CPU使用率
- **当前值**: REPLACE_CPU_USAGE%
- **状态**: REPLACE_CPU_STATUS
- **趋势**: REPLACE_CPU_TREND

### 1.2 内存使用率
- **当前值**: REPLACE_MEMORY_USAGE%
- **状态**: REPLACE_MEMORY_STATUS
- **趋势**: REPLACE_MEMORY_TREND

### 1.3 磁盘使用率
- **当前值**: REPLACE_DISK_USAGE%
- **状态**: REPLACE_DISK_STATUS

### 1.4 网络流量
- **接收速率**: REPLACE_NETWORK_RX_KBPS KB/s
- **发送速率**: REPLACE_NETWORK_TX_KBPS KB/s

## 2. 应用服务性能

### 2.1 用户服务
- **响应时间**: REPLACE_USER_RESPONSE_TIME ms
- **请求数**: REPLACE_USER_REQUEST_COUNT
- **错误数**: REPLACE_USER_ERROR_COUNT
- **吞吐量**: REPLACE_USER_THROUGHPUT RPS
- **活跃连接**: REPLACE_USER_CONNECTIONS

### 2.2 订单服务
- **响应时间**: REPLACE_ORDER_RESPONSE_TIME ms
- **请求数**: REPLACE_ORDER_REQUEST_COUNT
- **错误数**: REPLACE_ORDER_ERROR_COUNT
- **吞吐量**: REPLACE_ORDER_THROUGHPUT RPS
- **活跃连接**: REPLACE_ORDER_CONNECTIONS

### 2.3 库存服务
- **响应时间**: REPLACE_INVENTORY_RESPONSE_TIME ms
- **请求数**: REPLACE_INVENTORY_REQUEST_COUNT
- **错误数**: REPLACE_INVENTORY_ERROR_COUNT
- **吞吐量**: REPLACE_INVENTORY_THROUGHPUT RPS
- **活跃连接**: REPLACE_INVENTORY_CONNECTIONS

### 2.4 财务服务
- **响应时间**: REPLACE_FINANCE_RESPONSE_TIME ms
- **请求数**: REPLACE_FINANCE_REQUEST_COUNT
- **错误数**: REPLACE_FINANCE_ERROR_COUNT
- **吞吐量**: REPLACE_FINANCE_THROUGHPUT RPS
- **活跃连接**: REPLACE_FINANCE_CONNECTIONS

## 3. 数据库性能

### 3.1 MySQL数据库
- **活跃连接数**: REPLACE_MYSQL_CONNECTIONS
- **查询速率**: REPLACE_MYSQL_QPS QPS
- **慢查询数**: REPLACE_MYSQL_SLOW_QUERIES

### 3.2 PostgreSQL数据库
- **活跃连接数**: REPLACE_POSTGRESQL_CONNECTIONS
- **查询速率**: REPLACE_POSTGRESQL_QPS QPS

## 4. 性能阈值检查结果

### 4.1 阈值符合情况
- **CPU使用率**: REPLACE_CPU_THRESHOLD_CHECK
- **内存使用率**: REPLACE_MEMORY_THRESHOLD_CHECK
- **响应时间**: REPLACE_RESPONSE_TIME_CHECK
- **错误率**: REPLACE_ERROR_RATE_CHECK

### 4.2 告警情况
- **严重告警**: REPLACE_CRITICAL_ALERTS
- **警告告警**: REPLACE_WARNING_ALERTS
- **正常指标**: REPLACE_NORMAL_METRICS

## 5. 建议与行动计划

### 5.1 立即行动项
1. REPLACE_IMMEDIATE_ACTION_1
2. REPLACE_IMMEDIATE_ACTION_2
3. REPLACE_IMMEDIATE_ACTION_3

### 5.2 优化建议
1. REPLACE_OPTIMIZATION_SUGGESTION_1
2. REPLACE_OPTIMIZATION_SUGGESTION_2
3. REPLACE_OPTIMIZATION_SUGGESTION_3

## 6. 监控数据文件

### 6.1 原始数据文件
- 系统指标: system-metrics-*.csv
- 应用指标: application-metrics-*.csv
- 数据库指标: database-metrics-*.csv

### 6.2 可视化图表
- Grafana仪表盘: http://localhost:3000
- Prometheus查询: http://localhost:9090

## 附录

### A. 监控配置
- **监控频率**: 每分钟一次
- **数据保留**: 7天
- **告警通知**: 邮件/钉钉

### B. 性能阈值配置
```json
REPLACE_THRESHOLD_CONFIG
```

### C. 联系方式
- **测试负责人**: test-agent-2
- **运维负责人**: devops-engineer
- **质量保证**: qa-lead

---

**报告生成时间**: REPLACE_GENERATE_TIME  
**下次监控时间**: REPLACE_NEXT_MONITOR_TIME  
**监控状态**: ✅ 正常 / ⚠️ 警告 / 🔴 异常
EOF
    
    log "✓ 性能监控报告生成完成: $report_file"
}

# 连续监控模式
continuous_monitoring() {
    local interval=${1:-60}  # 默认60秒间隔
    local duration=${2:-3600}  # 默认监控1小时
    
    log "开始连续性能监控..."
    log "监控间隔: ${interval}秒"
    log "监控时长: ${duration}秒"
    
    local start_time=$(date +%s)
    local end_time=$((start_time + duration))
    local iteration=1
    
    while [ $(date +%s) -lt $end_time ]; do
        log ""
        log "=== 监控迭代 #$iteration ==="
        
        # 检查监控服务
        if ! check_monitoring_services; then
            log "监控服务异常，等待恢复..."
            sleep 10
            continue
        fi
        
        # 收集指标
        collect_system_metrics
        collect_application_metrics
        collect_database_metrics
        
        # 检查阈值
        check_performance_thresholds
        
        # 生成报告（每小时生成一次完整报告）
        if [ $((iteration % 60)) -eq 0 ]; then
            generate_monitoring_report
        fi
        
        log "等待 ${interval} 秒后继续监控..."
        sleep "$interval"
        iteration=$((iteration + 1))
    done
    
    log "连续监控完成，共执行 $((iteration - 1)) 次监控"
}

# 单次监控模式
single_monitoring() {
    log "执行单次性能监控..."
    
    # 检查监控服务
    if ! check_monitoring_services; then
        log "错误: 监控服务异常，无法执行监控"
        exit 1
    fi
    
    # 收集指标
    collect_system_metrics
    collect_application_metrics
    collect_database_metrics
    
    # 检查阈值
    check_performance_thresholds
    
    # 生成报告
    generate_monitoring_report
    
    log "✅ 单次监控完成"
}

# 显示帮助信息
show_help() {
    cat << 'EOF'
性能测试监控脚本

用法:
  ./monitor-performance.sh [模式] [选项]

模式:
  single     单次监控（默认）
  continuous 连续监控

选项:
  -i, --interval <秒>   连续监控间隔（默认: 60）
  -d, --duration <秒>   连续监控时长（默认: 3600）
  -h, --help           显示此帮助信息

示例:
  # 单次监控
  ./monitor-performance.sh single
  
  # 连续监控，每30秒一次，持续2小时
  ./monitor-performance.sh continuous -i 30 -d 7200
  
  # 连续监控，使用默认参数
  ./monitor-performance.sh continuous
EOF
}

# 主函数
main() {
    local mode="single"
    local interval=60
    local duration=3600
    
    # 解析参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            single|continuous)
                mode="$1"
                shift
                ;;
            -i|--interval)
                interval="$2"
                shift 2
                ;;
            -d|--duration)
                duration="$2"
                shift 2
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            *)
                echo "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    log "性能监控脚本启动..."
    log "模式: $mode"
    
    if [ "$mode" = "continuous" ]; then
        continuous_monitoring "$interval" "$duration"
    else
        single_monitoring
    fi
}

# 执行主函数
main "$@"