#!/bin/bash
# AI-Ready Log Metrics Exporter
# Exports log-based metrics for Prometheus
# Version: v1.0

set -e

ES_HOST="${ES_HOST:-http://elasticsearch:9200}"
INTERVAL="${INTERVAL:-60}"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

# 查询错误日志数量
query_error_count() {
    local time_range="$1"
    curl -s -X GET "${ES_HOST}/ai-ready-logs-*/_search" -H 'Content-Type: application/json' -d"
    {
        \"size\": 0,
        \"query\": {
            \"bool\": {
                \"must\": [
                    { \"range\": { \"@timestamp\": { \"gte\": \"now-${time_range}\" } } },
                    { \"terms\": { \"log_level\": [\"ERROR\", \"FATAL\"] } }
                ]
            }
        },
        \"aggs\": {
            \"error_count\": { \"value_count\": { \"field\": \"_id\" } }
        }
    }
    " | jq -r '.aggregations.error_count.value // 0'
}

# 查询慢查询数量
query_slow_query_count() {
    local time_range="$1"
    curl -s -X GET "${ES_HOST}/ai-ready-logs-*/_search" -H 'Content-Type: application/json' -d"
    {
        \"size\": 0,
        \"query\": {
            \"bool\": {
                \"must\": [
                    { \"range\": { \"@timestamp\": { \"gte\": \"now-${time_range}\" } } },
                    { \"match\": { \"message\": \"Slow query\" } },
                    { \"range\": { \"response.time\": { \"gte\": 2000 } } }
                ]
            }
        },
        \"aggs\": {
            \"slow_count\": { \"value_count\": { \"field\": \"_id\" } } }
        }
    }
    " | jq -r '.aggregations.slow_count.value // 0'
}

# 查询P95响应时间
query_p95_response_time() {
    curl -s -X GET "${ES_HOST}/ai-ready-logs-*/_search" -H 'Content-Type: application/json' -d'
    {
        "size": 0,
        "query": {
            "bool": {
                "must": [
                    { "range": { "@timestamp": { "gte": "now-5m" } } },
                    { "exists": { "field": "response.time" } }
                ]
            }
        },
        "aggs": {
            "p95_response_time": {
                "percentiles": {
                    "field": "response.time",
                    "percents": [95]
                }
            }
        }
    }
    ' | jq -r '.aggregations.p95_response_time.values["95.0"] // 0'
}

# 查询登录失败次数
query_login_failures() {
    curl -s -X GET "${ES_HOST}/ai-ready-logs-*/_search" -H 'Content-Type: application/json' -d'
    {
        "size": 0,
        "query": {
            "bool": {
                "must": [
                    { "range": { "@timestamp": { "gte": "now-5m" } } },
                    { "match": { "message": "登录失败" } }
                ]
            }
        },
        "aggs": {
            "failure_count": { "value_count": { "field": "_id" } }
        }
    }
    ' | jq -r '.aggregations.failure_count.value // 0'
}

# 导出Prometheus格式指标
export_metrics() {
    local timestamp=$(date +%s)
    
    # 错误日志指标
    local error_count_5m=$(query_error_count "5m")
    local error_count_1h=$(query_error_count "1h")
    
    # 慢查询指标
    local slow_query_count=$(query_slow_query_count "5m")
    
    # P95响应时间
    local p95_response=$(query_p95_response_time)
    
    # 登录失败
    local login_failures=$(query_login_failures)
    
    # 输出Prometheus格式
    cat << EOF
# HELP ai_ready_log_errors_total Total number of error logs
# TYPE ai_ready_log_errors_total gauge
ai_ready_log_errors_total{range="5m"} ${error_count_5m}
ai_ready_log_errors_total{range="1h"} ${error_count_1h}

# HELP ai_ready_slow_queries_total Total number of slow queries
# TYPE ai_ready_slow_queries_total gauge
ai_ready_slow_queries_total ${slow_query_count}

# HELP ai_ready_response_time_p95 P95 response time in milliseconds
# TYPE ai_ready_response_time_p95 gauge
ai_ready_response_time_p95 ${p95_response}

# HELP ai_ready_login_failures_total Total number of login failures
# TYPE ai_ready_login_failures_total gauge
ai_ready_login_failures_total ${login_failures}

# HELP ai_ready_log_exporter_timestamp Last export timestamp
# TYPE ai_ready_log_exporter_timestamp gauge
ai_ready_log_exporter_timestamp ${timestamp}
EOF
}

# 主循环
main() {
    log "Log Exporter started"
    log "Elasticsearch: ${ES_HOST}"
    log "Interval: ${INTERVAL}s"
    
    # 启动HTTP服务
    while true; do
        export_metrics > /tmp/metrics.txt
        sleep ${INTERVAL}
    done
}

# 启动HTTP服务（如果安装了nc）
start_http_server() {
    log "Starting HTTP server on port 9090"
    while true; do
        export_metrics | nc -l -p 9090 -q 1 2>/dev/null || true
    done
}

# 根据参数执行
case "${1:-export}" in
    export)
        export_metrics
        ;;
    server)
        start_http_server
        ;;
    loop)
        main
        ;;
    *)
        echo "Usage: $0 {export|server|loop}"
        exit 1
        ;;
esac
