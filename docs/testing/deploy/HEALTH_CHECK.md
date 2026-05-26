# Sprint 27+1 测试环境配置文档 - 健康检查指南

## 概述

本文档提供Sprint 27+1测试环境的健康检查操作指南，包括服务健康检查配置、健康检查脚本和健康检查命令。确保测试环境的稳定性和可靠性。

## 1. 服务健康检查配置

### 1.1 PostgreSQL健康检查配置

#### 1.1.1 基础健康检查配置

**配置文件**: `/etc/postgresql/15/main/postgresql.conf`

```ini
# 启用统计信息收集
track_activities = on
track_counts = on
track_io_timing = on
track_functions = all

# 设置连接超时
tcp_keepalives_idle = 60
tcp_keepalives_interval = 10
tcp_keepalives_count = 5

# 监控配置
log_connections = on
log_disconnections = on
log_duration = on
```

#### 1.1.2 监控用户配置

```sql
-- 创建监控专用用户
CREATE USER monitor WITH PASSWORD '$(openssl rand -base64 24)';
GRANT pg_monitor TO monitor;
GRANT SELECT ON pg_stat_database TO monitor;
GRANT SELECT ON pg_stat_activity TO monitor;
GRANT SELECT ON pg_stat_user_tables TO monitor;

-- 创建健康检查函数
CREATE OR REPLACE FUNCTION check_postgres_health()
RETURNS TABLE (
    database_name text,
    active_connections integer,
    idle_connections integer,
    total_transactions bigint,
    database_size text
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        datname::text,
        (SELECT count(*) FROM pg_stat_activity WHERE datname = d.datname AND state = 'active')::integer,
        (SELECT count(*) FROM pg_stat_activity WHERE datname = d.datname AND state = 'idle')::integer,
        xact_commit + xact_rollback,
        pg_size_pretty(pg_database_size(datname))
    FROM pg_stat_database d
    WHERE datname NOT IN ('template0', 'template1', 'postgres');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION check_postgres_health() TO monitor;
```

#### 1.1.3 Prometheus PostgreSQL Exporter配置

**配置文件**: `/etc/postgresql-exporter/postgresql-exporter.yml`

```yaml
# PostgreSQL Exporter配置
data_source_names:
  - postgresql://monitor:PASSWORD@localhost:5432/ai_ready_test?sslmode=disable

# 收集指标配置
disable_default_metrics: false
disable_settings_metrics: false
auto_discover_databases: true

# 自定义查询
custom_queries:
  - query: "SELECT datname, xact_commit, xact_rollback, numbackends, deadlocks FROM pg_stat_database WHERE datname NOT IN ('template0', 'template1', 'postgres')"
    metrics:
      - datname:
          usage: "LABEL"
          description: "Database name"
      - xact_commit:
          usage: "COUNTER"
          description: "Number of committed transactions"
      - xact_rollback:
          usage: "COUNTER"
          description: "Number of rolled back transactions"
      - numbackends:
          usage: "GAUGE"
          description: "Number of active connections"
      - deadlocks:
          usage: "COUNTER"
          description: "Number of deadlocks"
```

### 1.2 Redis健康检查配置

#### 1.2.1 Redis测试实例健康检查配置

**配置文件**: `/etc/redis/ai-ready-test.conf`

```bash
# 健康监控配置
latency-monitor-threshold 100
slowlog-log-slower-than 10000
slowlog-max-len 128

# 统计信息配置
statistics-interval 10

# 内存监控
maxmemory 2gb
maxmemory-policy allkeys-lru
```

#### 1.2.2 Redis监控脚本

**脚本文件**: `/usr/local/bin/redis-health-check.sh`

```bash
#!/bin/bash
# Redis健康检查脚本

set -e

REDIS_PORT=6380
REDIS_PASS=$(sudo grep "^requirepass" /etc/redis/ai-ready-test.conf | awk '{print $2}')

# 检查Redis服务状态
check_redis_service() {
    if ! systemctl is-active --quiet redis-ai-ready-test.service; then
        echo "ERROR: Redis测试实例服务未运行"
        return 1
    fi
    echo "OK: Redis测试实例服务运行正常"
    return 0
}

# 检查Redis连接
check_redis_connection() {
    if ! redis-cli -p "$REDIS_PORT" -a "$REDIS_PASS" ping | grep -q "PONG"; then
        echo "ERROR: Redis连接失败"
        return 1
    fi
    echo "OK: Redis连接正常"
    return 0
}

# 检查Redis内存使用
check_redis_memory() {
    MEMORY_INFO=$(redis-cli -p "$REDIS_PORT" -a "$REDIS_PASS" info memory)
    USED_MEMORY=$(echo "$MEMORY_INFO" | grep "^used_memory:" | cut -d: -f2)
    MAX_MEMORY=$(echo "$MEMORY_INFO" | grep "^maxmemory:" | cut -d: -f2)
    
    if [ "$MAX_MEMORY" -eq 0 ]; then
        echo "WARNING: Redis未设置内存限制"
        return 0
    fi
    
    MEMORY_PERCENT=$((USED_MEMORY * 100 / MAX_MEMORY))
    
    if [ "$MEMORY_PERCENT" -gt 90 ]; then
        echo "ERROR: Redis内存使用率过高: ${MEMORY_PERCENT}%"
        return 1
    elif [ "$MEMORY_PERCENT" -gt 70 ]; then
        echo "WARNING: Redis内存使用率较高: ${MEMORY_PERCENT}%"
        return 0
    else
        echo "OK: Redis内存使用率正常: ${MEMORY_PERCENT}%"
        return 0
    fi
}

# 检查Redis持久化
check_redis_persistence() {
    PERSISTENCE_INFO=$(redis-cli -p "$REDIS_PORT" -a "$REDIS_PASS" info persistence)
    RDB_LAST_SAVE=$(echo "$PERSISTENCE_INFO" | grep "^rdb_last_save_time:" | cut -d: -f2)
    CURRENT_TIME=$(date +%s)
    TIME_DIFF=$((CURRENT_TIME - RDB_LAST_SAVE))
    
    if [ "$TIME_DIFF" -gt 3600 ]; then
        echo "WARNING: Redis RDB持久化超过1小时未执行"
        return 0
    else
        echo "OK: Redis RDB持久化正常"
        return 0
    fi
}

# 检查Redis复制状态
check_redis_replication() {
    REPLICATION_INFO=$(redis-cli -p "$REDIS_PORT" -a "$REDIS_PASS" info replication)
    ROLE=$(echo "$REPLICATION_INFO" | grep "^role:" | cut -d: -f2)
    
    if [ "$ROLE" = "master" ]; then
        CONNECTED_SLAVES=$(echo "$REPLICATION_INFO" | grep "^connected_slaves:" | cut -d: -f2)
        echo "OK: Redis角色为master，连接从节点数: $CONNECTED_SLAVES"
    else
        MASTER_LINK_STATUS=$(echo "$REPLICATION_INFO" | grep "^master_link_status:" | cut -d: -f2)
        if [ "$MASTER_LINK_STATUS" = "up" ]; then
            echo "OK: Redis角色为slave，主从连接正常"
        else
            echo "ERROR: Redis角色为slave，主从连接断开"
            return 1
        fi
    fi
    return 0
}

# 执行所有检查
main() {
    echo "=== Redis健康检查开始 ==="
    echo "检查时间: $(date)"
    echo ""
    
    local exit_code=0
    
    # 执行检查
    for check_func in check_redis_service check_redis_connection check_redis_memory check_redis_persistence check_redis_replication; do
        if ! $check_func; then
            exit_code=1
        fi
    done
    
    echo ""
    echo "=== Redis健康检查结束 ==="
    
    if [ $exit_code -eq 0 ]; then
        echo "✅ 所有检查通过"
    else
        echo "❌ 部分检查失败"
    fi
    
    return $exit_code
}

# 执行主函数
main "$@"
```

#### 1.2.3 Prometheus Redis Exporter配置

**配置文件**: `/etc/redis-exporter/redis-exporter.yml`

```yaml
# Redis Exporter配置
redis_address: "redis://localhost:6380"
redis_password: "PASSWORD"
namespace: "redis_test"

# 检查配置
check_keys: "ai_ready:*"
check_single_keys: "ai_ready:config"
check_streams: "ai_ready:events"
check_single_streams: "ai_ready:important_events"

# 导出指标
export_client_list: true
export_client_port: true
export_duplicate_metrics: false

# 连接池配置
connection_timeout: "10s"
tls_client_key_file: ""
tls_client_cert_file: ""
tls_ca_cert_file: ""
```

### 1.3 RabbitMQ健康检查配置

#### 1.3.1 RabbitMQ健康检查插件配置

启用RabbitMQ管理插件：

```bash
# 启用管理插件
sudo rabbitmq-plugins enable rabbitmq_management

# 启用健康检查插件
sudo rabbitmq-plugins enable rabbitmq_management_agent
```

#### 1.3.2 RabbitMQ监控用户配置

```bash
# 创建监控用户
sudo rabbitmqctl add_user monitor "$(openssl rand -base64 24)"
sudo rabbitmqctl set_user_tags monitor monitoring
sudo rabbitmqctl set_permissions -p /ai_ready_test monitor "" "" ""

# 配置监控权限
sudo rabbitmqctl set_permissions -p / monitor "^aliveness-test$" "^amq\.default$" "^amq\.default$"
```

#### 1.3.3 RabbitMQ健康检查脚本

**脚本文件**: `/usr/local/bin/rabbitmq-health-check.sh`

```bash
#!/bin/bash
# RabbitMQ健康检查脚本

set -e

RABBITMQ_USER="monitor"
RABBITMQ_PASS=$(sudo grep "^default_pass" /etc/rabbitmq/rabbitmq.config 2>/dev/null | awk -F'"' '{print $2}' || echo "guest")
RABBITMQ_VHOST="/ai_ready_test"
RABBITMQ_API="http://localhost:15672/api"

# 检查RabbitMQ服务状态
check_rabbitmq_service() {
    if ! systemctl is-active --quiet rabbitmq-server; then
        echo "ERROR: RabbitMQ服务未运行"
        return 1
    fi
    echo "OK: RabbitMQ服务运行正常"
    return 0
}

# 检查RabbitMQ API访问
check_rabbitmq_api() {
    if ! curl -s -u "$RABBITMQ_USER:$RABBITMQ_PASS" "$RABBITMQ_API/healthchecks/node" | grep -q '"status":"ok"'; then
        echo "ERROR: RabbitMQ API访问失败"
        return 1
    fi
    echo "OK: RabbitMQ API访问正常"
    return 0
}

# 检查虚拟主机状态
check_rabbitmq_vhost() {
    VHOST_INFO=$(curl -s -u "$RABBITMQ_USER:$RABBITMQ_PASS" "$RABBITMQ_API/vhosts")
    
    if ! echo "$VHOST_INFO" | grep -q "\"name\":\"$RABBITMQ_VHOST\""; then
        echo "ERROR: 虚拟主机 $RABBITMQ_VHOST 不存在"
        return 1
    fi
    
    echo "OK: 虚拟主机 $RABBITMQ_VHOST 存在"
    return 0
}

# 检查队列状态
check_rabbitmq_queues() {
    QUEUES_INFO=$(curl -s -u "$RABBITMQ_USER:$RABBITMQ_PASS" "$RABBITMQ_API/queues/$RABBITMQ_VHOST")
    
    # 检查关键队列
    declare -a critical_queues=("ai_ready.task_queue" "ai_ready.event_queue")
    
    for queue in "${critical_queues[@]}"; do
        if ! echo "$QUEUES_INFO" | grep -q "\"name\":\"$queue\""; then
            echo "WARNING: 关键队列 $queue 不存在"
        else
            # 检查队列深度
            QUEUE_DEPTH=$(echo "$QUEUES_INFO" | grep -A5 "\"name\":\"$queue\"" | grep '"messages"' | cut -d: -f2 | tr -d ', ')
            
            if [ "$QUEUE_DEPTH" -gt 1000 ]; then
                echo "ERROR: 队列 $queue 深度过高: $QUEUE_DEPTH"
                return 1
            elif [ "$QUEUE_DEPTH" -gt 100 ]; then
                echo "WARNING: 队列 $queue 深度较高: $QUEUE_DEPTH"
            else
                echo "OK: 队列 $queue 深度正常: $QUEUE_DEPTH"
            fi
        fi
    done
    
    return 0
}

# 检查连接数
check_rabbitmq_connections() {
    CONNECTIONS_INFO=$(curl -s -u "$RABBITMQ_USER:$RABBITMQ_PASS" "$RABBITMQ_API/connections")
    CONNECTION_COUNT=$(echo "$CONNECTIONS_INFO" | grep -c '"name":' || echo 0)
    
    if [ "$CONNECTION_COUNT" -gt 100 ]; then
        echo "WARNING: RabbitMQ连接数过高: $CONNECTION_COUNT"
    else
        echo "OK: RabbitMQ连接数正常: $CONNECTION_COUNT"
    fi
    
    return 0
}

# 检查通道数
check_rabbitmq_channels() {
    CHANNELS_INFO=$(curl -s -u "$RABBITMQ_USER:$RABBITMQ_PASS" "$RABBITMQ_API/channels")
    CHANNEL_COUNT=$(echo "$CHANNELS_INFO" | grep -c '"name":' || echo 0)
    
    if [ "$CHANNEL_COUNT" -gt 500 ]; then
        echo "WARNING: RabbitMQ通道数过高: $CHANNEL_COUNT"
    else
        echo "OK: RabbitMQ通道数正常: $CHANNEL_COUNT"
    fi
    
    return 0
}

# 执行所有检查
main() {
    echo "=== RabbitMQ健康检查开始 ==="
    echo "检查时间: $(date)"
    echo ""
    
    local exit_code=0
    
    # 执行检查
    for check_func in check_rabbitmq_service check_rabbitmq_api check_rabbitmq_vhost check_rabbitmq_queues check_rabbitmq_connections check_rabbitmq_channels; do
        if ! $check_func; then
            exit_code=1
        fi
    done
    
    echo ""
    echo "=== RabbitMQ健康检查结束 ==="
    
    if [ $exit_code -eq 0 ]; then
        echo "✅ 所有检查通过"
    else
        echo "❌ 部分检查失败"
    fi
    
    return $exit_code
}

# 执行主函数
main "$@"
```

### 1.4 Prometheus健康检查配置

#### 1.4.1 Prometheus配置

**配置文件**: `/etc/prometheus/prometheus.yml`

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

# 告警规则文件
rule_files:
  - "/etc/prometheus/rules/*.yml"

# 抓取配置
scrape_configs:
  # Prometheus自身监控
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
    metrics_path: '/api/v1/status/config'
    scheme: http
    
  # PostgreSQL监控
  - job_name: 'postgresql'
    static_configs:
      - targets: ['localhost:9187']
    params:
      collect[]:
        - standard
        - database
        - bgwriter
        - archiver
    
  # Redis监控
  - job_name: 'redis'
    static_configs:
      - targets: ['localhost:9121']
    params:
      check-keys: ['ai_ready:*']
    
  # RabbitMQ监控
  - job_name: 'rabbitmq'
    static_configs:
      - targets: ['localhost:9419']
    
  # Node Exporter
  - job_name: 'node'
    static_configs:
      - targets: ['localhost:9100']
    
  # Blackbox Exporter
  - job_name: 'blackbox'
    metrics_path: /probe
    params:
      module: [http_2xx]
    static_configs:
      - targets:
        - http://localhost:9090  # Prometheus
        - http://localhost:3000  # Grafana
        - http://localhost:9093  # AlertManager
    relabel_configs:
      - source_labels: [__address__]
        target_label: __param_target
      - source_labels: [__param_target]
        target_label: instance
      - target_label: __address__
        replacement: localhost:9115
```

#### 1.4.2 Prometheus健康检查脚本

**脚本文件**: `/usr/local/bin/prometheus-health-check.sh`

```bash
#!/bin/bash
# Prometheus健康检查脚本

set -e

PROMETHEUS_URL="http://localhost:9090"
PROMETHEUS_API="$PROMETHEUS_URL/api/v1"

# 检查Prometheus服务状态
check_prometheus_service() {
    if ! systemctl is-active --quiet prometheus; then
        echo "ERROR: Prometheus服务未运行"
        return 1
    fi
    echo "OK: Prometheus服务运行正常"
    return 0
}

# 检查Prometheus API访问
check_prometheus_api() {
    if ! curl -s "$PROMETHEUS_API/query?query=up" | grep -q '"result":'; then
        echo "ERROR: Prometheus API访问失败"
        return 1
    fi
    echo "OK: Prometheus API访问正常"
    return 0
}

# 检查目标状态
check_prometheus_targets() {
    TARGETS_INFO=$(curl -s "$PROMETHEUS_API/targets")
    UP_TARGETS=$(echo "$TARGETS_INFO" | grep -o '"health":"up"' | wc -l)
    DOWN_TARGETS=$(echo "$TARGETS_INFO" | grep -o '"health":"down"' | wc -l)
    
    if [ "$DOWN_TARGETS" -gt 0 ]; then
        echo "ERROR: 有 $DOWN_TARGETS 个监控目标离线"
        
        # 获取离线的目标
        echo "离线的目标:"
        echo "$TARGETS_INFO" | grep -B5 '"health":"down"' | grep '"labels"' | sed 's/.*"instance":"\([^"]*\)".*/\1/'
        return 1
    else
        echo "OK: 所有 $UP_TARGETS 个监控目标在线"
        return 0
    fi
}

# 检查规则状态
check_prometheus_rules() {
    RULES_INFO=$(curl -s "$PROMETHEUS_API/rules")
    
    if echo "$RULES_INFO" | grep -q '"health":"unknown"'; then
        echo "WARNING: 有规则状态未知"
        return 0
    fi
    
    echo "OK: 所有规则状态正常"
    return 0
}

# 检查存储状态
check_prometheus_storage() {
    TSDB_STATUS=$(curl -s "$PROMETHEUS_API/status/tsdb")
    
    # 检查头块数
    HEAD_SAMPLES=$(echo "$TSDB_STATUS" | grep '"headStats"' -A10 | grep '"numSeries"' | cut -d: -f2 | tr -d ', ')
    
    if [ "$HEAD_SAMPLES" -gt 100000 ]; then
        echo "WARNING: Prometheus头块样本数较高: $HEAD_SAMPLES"
    else
        echo "OK: Prometheus头块样本数正常: $HEAD_SAMPLES"
    fi
    
    return 0
}

# 检查告警状态
check_prometheus_alerts() {
    ALERTS_INFO=$(curl -s "$PROMETHEUS_API/alerts")
    FIRING_ALERTS=$(echo "$ALERTS_INFO" | grep -o '"state":"firing"' | wc -l)
    
    if [ "$FIRING_ALERTS" -gt 0 ]; then
        echo "ERROR: 有 $FIRING_ALERTS 个告警处于触发状态"
        
        # 获取触发的告警
        echo "触发的告警:"
        echo "$ALERTS_INFO" | grep -B10 '"state":"firing"' | grep '"alertname"' | sed 's/.*"alertname":"\([^"]*\)".*/\1/'
        return 1
    else
        echo "OK: 没有触发的告警"
        return 0
    fi
}

# 执行所有检查
main() {
    echo "=== Prometheus健康检查开始 ==="
    echo "检查时间: $(date)"
    echo ""
    
    local exit_code=0
    
    # 执行检查
    for check_func in check_prometheus_service check_prometheus_api check_prometheus_targets check_prometheus_rules check_prometheus_storage check_prometheus_alerts; do
        if ! $check_func; then
            exit_code=1
        fi
    done
    
    echo ""
    echo "=== Prometheus健康检查结束 ==="
    
    if [ $exit_code -eq 0 ]; then
        echo "✅ 所有检查通过"
    else
        echo "❌ 部分检查失败"
    fi
    
    return $exit_code
}

# 执行主函数
main "$@"
```

### 1.5 AlertManager健康检查配置

#### 1.5.1 AlertManager配置

**配置文件**: `/etc/alertmanager/alertmanager.yml`

```yaml
global:
  smtp_smarthost: 'localhost:25'
  smtp_from: 'alertmanager@ai-ready-test.local'
  smtp_auth_username: ''
  smtp_auth_password: ''

route:
  group_by: ['alertname', 'cluster', 'service']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'web.hook'
  routes:
    - match:
        severity: 'critical'
      receiver: 'critical-alerts'
    - match:
        severity: 'warning'
      receiver: 'warning-alerts'

receivers:
  - name: 'web.hook'
    webhook_configs:
      - url: 'http://localhost:5001/'
  
  - name: 'critical-alerts'
    email_configs:
      - to: 'admin@ai-ready-test.local'
        headers:
          Subject: '[CRITICAL] Alert: {{ .GroupLabels.alertname }}'
    
  - name: 'warning-alerts'
    email_configs:
      - to: 'monitor@ai-ready-test.local'
        headers:
          Subject: '[WARNING] Alert: {{ .GroupLabels.alertname }}'

inhibit_rules:
  - source_match:
      severity: 'critical'
    target_match:
      severity: 'warning'
    equal: ['alertname', 'cluster', 'service']
```

#### 1.5.2 AlertManager健康检查脚本

**脚本文件**: `/usr/local/bin/alertmanager-health-check.sh`

```bash
#!/bin/bash
# AlertManager健康检查脚本

set -e

ALERTMANAGER_URL="http://localhost:9093"
ALERTMANAGER_API="$ALERTMANAGER_URL/api/v2"

# 检查AlertManager服务状态
check_alertmanager_service() {
    if ! systemctl is-active --quiet alertmanager; then
        echo "ERROR: AlertManager服务未运行"
        return 1
    fi
    echo "OK: AlertManager服务运行正常"
    return 0
}

# 检查AlertManager API访问
check_alertmanager_api() {
    if ! curl -s "$ALERTMANAGER_API/status" | grep -q '"cluster"'; then
        echo "ERROR: AlertManager API访问失败"
        return 1
    fi
    echo "OK: AlertManager API访问正常"
    return 0
}

# 检查集群状态
check_alertmanager_cluster() {
    CLUSTER_INFO=$(curl -s "$ALERTMANAGER_API/status")
    PEER_COUNT=$(echo "$CLUSTER_INFO" | grep -o '"name"' | wc -l)
    
    if [ "$PEER_COUNT" -eq 0 ]; then
        echo "WARNING: AlertManager运行在单机模式"
    else
        echo "OK: AlertManager集群有 $PEER_COUNT 个节点"
    fi
    
    return 0
}

# 检查告警状态
check_alertmanager_alerts() {
    ALERTS_INFO=$(curl -s "$ALERTMANAGER_API/alerts")
    ACTIVE_ALERTS=$(echo "$ALERTS_INFO" | grep -o '"status":"active"' | wc -l)
    SUPPRESSED_ALERTS=$(echo "$ALERTS_INFO" | grep -o '"status":"suppressed"' | wc -l)
    
    echo "告警状态统计:"
    echo "  活跃告警: $ACTIVE_ALERTS"
    echo "  抑制告警: $SUPPRESSED_ALERTS"
    
    if [ "$ACTIVE_ALERTS" -gt 10 ]; then
        echo "WARNING: 活跃告警数量较多"
    fi
    
    return 0
}

# 检查静默规则
check_alertmanager_silences() {
    SILENCES_INFO=$(curl -s "$ALERTMANAGER_API/silences")
    ACTIVE_SILENCES=$(echo "$SILENCES_INFO" | grep -o '"status":{"state":"active"' | wc -l)
    PENDING_SILENCES=$(echo "$SILENCES_INFO" | grep -o '"status":{"state":"pending"' | wc -l)
    
    echo "静默规则统计:"
    echo "  活跃静默: $ACTIVE_SILENCES"
    echo "  待定静默: $PENDING_SILENCES"
    
    return 0
}

# 检查接收器配置
check_alertmanager_receivers() {
    CONFIG_INFO=$(curl -s "$ALERTMANAGER_API/status")
    RECEIVER_COUNT=$(echo "$CONFIG_INFO" | grep -o '"name"' | wc -l)
    
    if [ "$RECEIVER_COUNT" -eq 0 ]; then
        echo "ERROR: 没有配置接收器"
        return 1
    fi
    
    echo "OK: 配置了 $RECEIVER_COUNT 个接收器"
    return 0
}

# 执行所有检查
main() {
    echo "=== AlertManager健康检查开始 ==="
    echo "检查时间: $(date)"
    echo ""
    
    local exit_code=0
    
    # 执行检查
    for check_func in check_alertmanager_service check_alertmanager_api check_alertmanager_cluster check_alertmanager_alerts check_alertmanager_silences check_alertmanager_receivers; do
        if ! $check_func; then
            exit_code=1
        fi
    done
    
    echo ""
    echo "=== AlertManager健康检查结束 ==="
    
    if [ $exit_code -eq 0 ]; then
        echo "✅ 所有检查通过"
    else
        echo "❌ 部分检查失败"
    fi
    
    return $exit_code
}

# 执行主函数
main "$@"
```

### 1.6 Grafana健康检查配置

#### 1.6.1 Grafana配置

**配置文件**: `/etc/grafana/grafana.ini`

```ini
[server]
domain = localhost
root_url = %(protocol)s://%(domain)s:%(http_port)s/
serve_from_sub_path = false

[database]
type = sqlite3
path = grafana.db

[security]
admin_user = admin
admin_password = $(openssl rand -base64 24)

[analytics]
check_for_updates = true

[log]
mode = console file
level = info

[grafana_net]
url = https://grafana.net

[smtp]
enabled = false

[alerting]
enabled = true
execute_alerts = true

[panels]
enable_alpha = false

[plugins]
enable_alpha = false

[auth]
disable_login_form = false
disable_signout_menu = false

[auth.anonymous]
enabled = true
org_name = Main Org.
org_role = Viewer
```

#### 1.6.2 Grafana健康检查脚本

**脚本文件**: `/usr/local/bin/grafana-health-check.sh`

```bash
#!/bin/bash
# Grafana健康检查脚本

set -e

GRAFANA_URL="http://localhost:3000"
GRAFANA_API="$GRAFANA_URL/api"
GRAFANA_USER="admin"
GRAFANA_PASS=$(sudo grep "^admin_password" /etc/grafana/grafana.ini | cut -d= -f2 | tr -d ' ')

# 检查Grafana服务状态
check_grafana_service() {
    if ! systemctl is-active --quiet grafana-server; then
        echo "ERROR: Grafana服务未运行"
        return 1
    fi
    echo "OK: Grafana服务运行正常"
    return 0
}

# 检查Grafana API访问
check_grafana_api() {
    if ! curl -s -u "$GRAFANA_USER:$GRAFANA_PASS" "$GRAFANA_API/health" | grep -q '"database":"ok"'; then
        echo "ERROR: Grafana API访问失败"
        return 1
    fi
    echo "OK: Grafana API访问正常"
    return 0
}

# 检查数据源状态
check_grafana_datasources() {
    DATASOURCES_INFO=$(curl -s -u "$GRAFANA_USER:$GRAFANA_PASS" "$GRAFANA_API/datasources")
    DATASOURCE_COUNT=$(echo "$DATASOURCES_INFO" | grep -o '"name"' | wc -l)
    
    if [ "$DATASOURCE_COUNT" -eq 0 ]; then
        echo "ERROR: 没有配置数据源"
        return 1
    fi
    
    echo "OK: 配置了 $DATASOURCE_COUNT 个数据源"
    
    # 检查关键数据源
    declare -a critical_datasources=("Prometheus" "PostgreSQL" "MySQL")
    
    for ds in "${critical_datasources[@]}"; do
        if echo "$DATASOURCES_INFO" | grep -q "\"name\":\"$ds\""; then
            echo "  ✓ 关键数据源 $ds 已配置"
        else
            echo "  ⚠  关键数据源 $ds 未配置"
        fi
    done
    
    return 0
}

# 检查仪表板状态
check_grafana_dashboards() {
    DASHBOARDS_INFO=$(curl -s -u "$GRAFANA_USER:$GRAFANA_PASS" "$GRAFANA_API/search?type=dash-db")
    DASHBOARD_COUNT=$(echo "$DASHBOARDS_INFO" | grep -o '"title"' | wc -l)
    
    if [ "$DASHBOARD_COUNT" -eq 0 ]; then
        echo "WARNING: 没有配置仪表板"
        return 0
    fi
    
    echo "OK: 配置了 $DASHBOARD_COUNT 个仪表板"
    
    # 检查关键仪表板
    declare -a critical_dashboards=("PostgreSQL Overview" "Redis Dashboard" "RabbitMQ Overview" "Node Exporter Full")
    
    for dashboard in "${critical_dashboards[@]}"; do
        if echo "$DASHBOARDS_INFO" | grep -q "\"title\":\"$dashboard\""; then
            echo "  ✓ 关键仪表板 $dashboard 已配置"
        else
            echo "  ⚠  关键仪表板 $dashboard 未配置"
        fi
    done
    
    return 0
}

# 检查告警规则
check_grafana_alerts() {
    ALERTS_INFO=$(curl -s -u "$GRAFANA_USER:$GRAFANA_PASS" "$GRAFANA_API/alerts")
    ALERT_COUNT=$(echo "$ALERTS_INFO" | grep -o '"name"' | wc -l)
    
    if [ "$ALERT_COUNT" -eq 0 ]; then
        echo "WARNING: 没有配置告警规则"
        return 0
    fi
    
    echo "OK: 配置了 $ALERT_COUNT 个告警规则"
    
    # 检查告警状态
    FIRING_ALERTS=$(echo "$ALERTS_INFO" | grep -o '"state":"alerting"' | wc -l)
    if [ "$FIRING_ALERTS" -gt 0 ]; then
        echo "ERROR: 有 $FIRING_ALERTS 个告警处于触发状态"
        return 1
    fi
    
    return 0
}

# 检查用户和权限
check_grafana_users() {
    USERS_INFO=$(curl -s -u "$GRAFANA_USER:$GRAFANA_PASS" "$GRAFANA_API/org/users")
    USER_COUNT=$(echo "$USERS_INFO" | grep -o '"login"' | wc -l)
    
    if [ "$USER_COUNT" -lt 2 ]; then
        echo "WARNING: 用户数量较少，建议配置多个用户"
    else
        echo "OK: 有 $USER_COUNT 个用户"
    fi
    
    return 0
}

# 执行所有检查
main() {
    echo "=== Grafana健康检查开始 ==="
    echo "检查时间: $(date)"
    echo ""
    
    local exit_code=0
    
    # 执行检查
    for check_func in check_grafana_service check_grafana_api check_grafana_datasources check_grafana_dashboards check_grafana_alerts check_grafana_users; do
        if ! $check_func; then
            exit_code=1
        fi
    done
    
    echo ""
    echo "=== Grafana健康检查结束 ==="
    
    if [ $exit_code -eq 0 ]; then
        echo "✅ 所有检查通过"
    else
        echo "❌ 部分检查失败"
    fi
    
    return $exit_code
}

# 执行主函数
main "$@"
```

### 1.7 custom-exporter健康检查配置

#### 1.7.1 自定义Exporter配置

**自定义应用监控Exporter**: `/usr/local/bin/custom-app-exporter.py`

```python
#!/usr/bin/env python3
"""
自定义应用监控Exporter
"""

import time
import psutil
from prometheus_client import start_http_server, Gauge, Counter, Histogram
import requests
import json

# 定义指标
app_uptime = Gauge('app_uptime_seconds', '应用运行时间')
app_memory_usage = Gauge('app_memory_usage_bytes', '应用内存使用量')
app_cpu_usage = Gauge('app_cpu_usage_percent', '应用CPU使用率')
app_request_count = Counter('app_requests_total', '应用请求总数')
app_request_duration = Histogram('app_request