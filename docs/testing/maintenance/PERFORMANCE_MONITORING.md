# 性能监控配置说明

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**作者**: doc-writer  
**最后更新**: 2026-04-27

## 目录

1. [概述](#概述)
2. [监控架构](#监控架构)
3. [监控指标配置](#监控指标配置)
4. [告警规则配置](#告警规则配置)
5. [可视化配置](#可视化配置)
6. [性能测试监控](#性能测试监控)
7. [容量规划监控](#容量规划监控)
8. [监控优化建议](#监控优化建议)
9. [监控工具使用](#监控工具使用)

---

## 概述

### 监控目标

测试环境性能监控系统的目标是：

1. **实时监控**: 实时跟踪系统性能状态
2. **问题预警**: 提前发现性能问题并告警
3. **性能分析**: 分析性能瓶颈和优化点
4. **容量规划**: 为容量规划提供数据支持
5. **趋势分析**: 分析性能趋势和变化规律

### 监控范围

| 监控维度 | 监控内容 | 监控工具 |
|----------|----------|----------|
| **基础设施** | CPU、内存、磁盘、网络 | Prometheus Node Exporter |
| **应用性能** | JVM、GC、线程池、连接池 | Micrometer、JMX Exporter |
| **业务指标** | 请求量、响应时间、成功率 | 自定义指标、业务埋点 |
| **数据库** | 查询性能、连接数、锁等待 | PostgreSQL Exporter |
| **缓存** | 命中率、内存使用、连接数 | Redis Exporter |
| **消息队列** | 队列长度、消费延迟、错误率 | Kafka Exporter |

### 监控指标层级

#### Level 1: 黄金信号 (Golden Signals)
- **延迟**: 请求处理时间
- **流量**: 请求量/QPS
- **错误**: 错误率/失败率
- **饱和度**: 资源使用率

#### Level 2: 用户体验指标 (User Experience)
- **页面加载时间**
- **API响应时间**
- **事务成功率**
- **关键业务流程耗时**

#### Level 3: 业务指标 (Business Metrics)
- **订单创建成功率**
- **支付处理时间**
- **库存查询性能**
- **用户注册耗时**

#### Level 4: 系统指标 (System Metrics)
- **CPU使用率**
- **内存使用率**
- **磁盘IO**
- **网络带宽**

## 监控架构

### 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                   监控数据源 (Metrics Sources)                │
├──────────────┬──────────────┬──────────────┬───────────────┤
│ 应用服务     │ 数据库       │ 缓存         │ 基础设施      │
│ (Micrometer) │ (PostgreSQL) │ (Redis)      │ (Node Export)│
└──────────────┴──────────────┴──────────────┴───────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   监控收集层 (Metrics Collection)             │
├─────────────────────────────────────────────────────────────┤
│                   Prometheus Server                          │
│  • 定时拉取指标                                             │
│  • 指标存储                                                 │
│  • 告警规则计算                                             │
└─────────────────────────────────────────────────────────────┘
                              │
                ┌─────────────┴─────────────┐
                ▼                           ▼
┌─────────────────────────┐   ┌─────────────────────────┐
│  告警处理层 (Alerting)   │   │  可视化层 (Visualization) │
├─────────────────────────┤   ├─────────────────────────┤
│ AlertManager            │   │ Grafana                 │
│ • 告警路由              │   │ • 监控面板              │
│ • 通知发送              │   │ • 数据探索              │
│ • 告警抑制              │   │ • 报表生成              │
└─────────────────────────┘   └─────────────────────────┘
```

### 组件说明

#### Prometheus
- **版本**: 2.45.0
- **端口**: 9090
- **数据保留**: 15天
- **抓取间隔**: 15s

#### AlertManager
- **版本**: 0.25.0
- **端口**: 9093
- **通知渠道**: 企业微信、邮件、Webhook
- **告警分组**: 按服务、按严重级别

#### Grafana
- **版本**: 10.0.0
- **端口**: 3000
- **数据源**: Prometheus
- **认证**: LDAP集成

#### Exporters
- **Node Exporter**: 1.6.0 (基础设施监控)
- **PostgreSQL Exporter**: 0.13.0 (数据库监控)
- **Redis Exporter**: 1.54.0 (Redis监控)
- **JMX Exporter**: 0.19.0 (JVM监控)
- **Blackbox Exporter**: 0.24.0 (网络探测)

### 部署配置

#### Docker Compose配置
```yaml
version: '3.8'
services:
  prometheus:
    image: prom/prometheus:v2.45.0
    container_name: test-env-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./prometheus/alert_rules.yml:/etc/prometheus/alert_rules.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=15d'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.enable-lifecycle'
    restart: unless-stopped

  alertmanager:
    image: prom/alertmanager:v0.25.0
    container_name: test-env-alertmanager
    ports:
      - "9093:9093"
    volumes:
      - ./alertmanager/alertmanager.yml:/etc/alertmanager/alertmanager.yml
      - alertmanager_data:/alertmanager
    command:
      - '--config.file=/etc/alertmanager/alertmanager.yml'
      - '--storage.path=/alertmanager'
    restart: unless-stopped

  grafana:
    image: grafana/grafana:10.0.0
    container_name: test-env-grafana
    ports:
      - "3000:3000"
    volumes:
      - ./grafana/provisioning:/etc/grafana/provisioning
      - grafana_data:/var/lib/grafana
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin123
      - GF_INSTALL_PLUGINS=grafana-piechart-panel
    restart: unless-stopped

  node-exporter:
    image: prom/node-exporter:v1.6.0
    container_name: test-env-node-exporter
    ports:
      - "9100:9100"
    volumes:
      - /proc:/host/proc:ro
      - /sys:/host/sys:ro
      - /:/rootfs:ro
    command:
      - '--path.procfs=/host/proc'
      - '--path.sysfs=/host/sys'
      - '--collector.filesystem.mount-points-exclude=^/(sys|proc|dev|host|etc)($$|/)'
    restart: unless-stopped

volumes:
  prometheus_data:
  alertmanager_data:
  grafana_data:
```

#### Prometheus配置
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']

rule_files:
  - "alert_rules.yml"

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'node'
    static_configs:
      - targets: ['node-exporter:9100']

  - job_name: 'api-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['api:8080']
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        regex: '([^:]+)(?::\d+)?'
        replacement: '${1}'

  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']

  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
```

## 监控指标配置

### 应用服务指标

#### Spring Boot Actuator配置
```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
    tags:
      application: ai-ready-api
      environment: test
    enable:
      jvm: true
      logback: true
      process: true
      system: true
```

#### 自定义业务指标
```java
// 自定义指标注册
@Component
public class BusinessMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter orderCreatedCounter;
    private final Timer orderProcessingTimer;
    private final Gauge inventoryLevelGauge;
    
    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // 订单创建计数器
        this.orderCreatedCounter = Counter.builder("business.order.created")
            .description("订单创建数量")
            .tag("environment", "test")
            .register(meterRegistry);
            
        // 订单处理计时器
        this.orderProcessingTimer = Timer.builder("business.order.processing.time")
            .description("订单处理时间")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
            
        // 库存水平指标
        this.inventoryLevelGauge = Gauge.builder("business.inventory.level", 
                () -> getInventoryLevel())
            .description("库存水平")
            .register(meterRegistry);
    }
    
    public void incrementOrderCount() {
        orderCreatedCounter.increment();
    }
    
    public Timer.Sample startOrderProcessing() {
        return Timer.start(meterRegistry);
    }
    
    public void stopOrderProcessing(Timer.Sample sample, String orderType) {
        sample.stop(Timer.builder("business.order.processing.time")
            .tag("order_type", orderType)
            .register(meterRegistry));
    }
    
    private Double getInventoryLevel() {
        // 从数据库获取库存水平
        return inventoryService.getTotalInventoryLevel();
    }
}
```

#### 关键应用指标清单

| 指标名称 | 类型 | 描述 | 告警阈值 |
|----------|------|------|----------|
| `http_server_requests_seconds` | Histogram | HTTP请求耗时 | P95 > 2s |
| `jvm_memory_used_bytes` | Gauge | JVM内存使用 | > 80% |
| `jvm_gc_pause_seconds` | Summary | GC停顿时间 | > 1s |
| `tomcat_threads_busy` | Gauge | Tomcat繁忙线程 | > 80% |
| `hikaricp_connections_active` | Gauge | 数据库活跃连接 | > 80% |
| `cache_gets_total` | Counter | 缓存获取次数 | - |
| `cache_hits_total` | Counter | 缓存命中次数 | 命中率 < 80% |
| `business_order_created_total` | Counter | 订单创建数量 | - |
| `business_order_processing_time_seconds` | Timer | 订单处理时间 | P95 > 5s |

### 数据库指标

#### PostgreSQL监控配置
```sql
-- 启用统计收集
ALTER SYSTEM SET track_activities = on;
ALTER SYSTEM SET track_counts = on;
ALTER SYSTEM SET track_io_timing = on;
ALTER SYSTEM SET track_functions = all;
ALTER SYSTEM SET shared_preload_libraries = 'pg_stat_statements';

-- 重启PostgreSQL生效
SELECT pg_reload_conf();

-- 创建监控用户
CREATE USER prometheus WITH PASSWORD 'prometheus_password';
GRANT pg_monitor TO prometheus;
```

#### PostgreSQL Exporter配置
```yaml
# postgres-exporter配置
DATA_SOURCE_NAME: "postgresql://prometheus:prometheus_password@postgres:5432/ai_ready?sslmode=disable"
PG_EXPORTER_EXTEND_QUERY_PATH: /etc/postgres-exporter/queries.yaml
```

#### 关键数据库指标

| 指标名称 | 描述 | 告警阈值 |
|----------|------|----------|
| `pg_stat_database_xact_commit` | 事务提交数 | - |
| `pg_stat_database_xact_rollback` | 事务回滚数 | 回滚率 > 5% |
| `pg_stat_database_tup_fetched` | 行获取数 | - |
| `pg_stat_database_tup_inserted` | 行插入数 | - |
| `pg_stat_database_tup_updated` | 行更新数 | - |
| `pg_stat_database_tup_deleted` | 行删除数 | - |
| `pg_stat_user_tables_n_live_tup` | 表活跃行数 | - |
| `pg_stat_user_tables_n_dead_tup` | 表死行数 | 死行率 > 20% |
| `pg_stat_user_indexes_idx_scan` | 索引扫描数 | - |
| `pg_stat_activity_count` | 活跃连接数 | > 最大连接数80% |
| `pg_stat_statements_total_time` | SQL总耗时 | Top 10慢查询 > 1s |
| `pg_stat_statements_calls` | SQL调用次数 | - |
| `pg_stat_statements_mean_time` | SQL平均耗时 | > 100ms |

### 缓存指标

#### Redis监控配置
```bash
# 启用Redis监控
redis-cli CONFIG SET slowlog-log-slower-than 10000
redis-cli CONFIG SET slowlog-max-len 128
redis-cli CONFIG SET latency-monitor-threshold 100
```

#### Redis Exporter配置
```bash
# 启动Redis Exporter
redis_exporter \
  -redis.addr redis://redis:6379 \
  -redis.password "" \
  -web.listen-address :9121 \
  -check-keys "user:*","order:*","product:*"
```

#### 关键缓存指标

| 指标名称 | 描述 | 告警阈值 |
|----------|------|----------|
| `redis_up` | Redis服务状态 | 0 |
| `redis_connected_clients` | 连接客户端数 | > 最大连接数80% |
| `redis_memory_used_bytes` | 内存使用量 | > 最大内存80% |
| `redis_memory_max_bytes` | 最大内存配置 | - |
| `redis_keyspace_hits_total` | 键空间命中数 | - |
| `redis_keyspace_misses_total` | 键空间未命中数 | 命中率 < 80% |
| `redis_instantaneous_ops_per_sec` | 每秒操作数 | - |
| `redis_connected_slaves` | 连接从节点数 | - |
| `redis_uptime_in_seconds` | 运行时间 | - |
| `redis_cpu_sys` | 系统CPU使用 | > 80% |
| `redis_cpu_user` | 用户CPU使用 | > 80% |
| `redis_expired_keys_total` | 过期键数量 | - |
| `redis_evicted_keys_total` | 淘汰键数量 | > 0 |

### 基础设施指标

#### Node Exporter指标

| 指标名称 | 描述 | 告警阈值 |
|----------|------|----------|
| `node_cpu_seconds_total` | CPU使用时间 | user > 80% |
| `node_memory_MemTotal_bytes` | 总内存 | - |
| `node_memory_MemAvailable_bytes` | 可用内存 | < 总内存20% |
| `node_memory_MemFree_bytes` | 空闲内存 | - |
| `node_memory_Buffers_bytes` | 缓冲区内存 | - |
| `node_memory_Cached_bytes` | 缓存内存 | - |
| `node_filesystem_size_bytes` | 文件系统大小 | - |
| `node_filesystem_avail_bytes` | 文件系统可用空间 | < 总空间20% |
| `node_filesystem_files_free` | 文件系统可用inode | < 总inode20% |
| `node_network_receive_bytes_total` | 网络接收字节 | - |
| `node_network_transmit_bytes_total` | 网络发送字节 | - |
| `node_disk_read_bytes_total` | 磁盘读取字节 | - |
| `node_disk_written_bytes_total` | 磁盘写入字节 | - |
| `node_disk_reads_completed_total` | 磁盘读取完成数 | - |
| `node_disk_writes_completed_total` | 磁盘写入完成数 | - |

## 告警规则配置

### 告警规则文件
```yaml
# alert_rules.yml
groups:
  - name: infrastructure
    rules:
      - alert: HighCPUUsage
        expr: 100 - (avg by(instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 80
        for: 5m
        labels:
          severity: warning
          service: infrastructure
        annotations:
          summary: "CPU使用率过高"
          description: "实例 {{ $labels.instance }} 的CPU使用率超过80% (当前值: {{ $value }}%)"
          
      - alert: HighMemoryUsage
        expr: (node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100 > 80
        for: 5m
        labels:
          severity: warning
          service: infrastructure
        annotations:
          summary: "内存使用率过高"
          description: "实例 {{ $labels.instance }} 的内存使用率超过80% (当前值: {{ $value }}%)"
          
      - alert: LowDiskSpace
        expr: node_filesystem_avail_bytes / node_filesystem_size_bytes * 100 < 20
        for: 5m
        labels:
          severity: warning
          service: infrastructure
        annotations:
          summary: "磁盘空间不足"
          description: "实例 {{ $labels.instance }} 的磁盘可用空间低于20% (当前值: {{ $value }}%)"

  - name: application
    rules:
      - alert: HighAPIResponseTime
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 2
        for: 2m
        labels:
          severity: warning
          service: api
        annotations:
          summary: "API响应时间过高"
          description: "API的95%响应时间超过2秒 (当前值: {{ $value }}秒)"
          
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m]) * 100 > 5
        for: 2m
        labels:
          severity: critical
          service: api
        annotations:
          summary: "API错误率过高"
          description: "API错误率超过5% (当前值: {{ $value }}%)"
          
      - alert: HighJVMMemoryUsage
        expr: jvm_memory_used_bytes / jvm_memory_max_bytes * 100 > 80
        for: 5m
        labels:
          severity: warning
          service: api
        annotations:
          summary: "JVM内存使用率过高"
          description: "JVM内存使用率超过80% (当前值: {{ $value }}%)"

  - name: database
    rules:
      - alert: HighDatabaseConnections
        expr: pg_stat_activity_count / pg_settings_max_connections * 100 > 80
        for: 5m
        labels:
          severity: warning
          service: database
        annotations:
          summary: "数据库连接数过高"
          description: "数据库连接数超过最大连接数的80% (当前值: {{ $value }}%)"
          
      - alert: SlowQueries
        expr: rate(pg_stat_statements_total_time[5m]) / rate(pg_stat_statements_calls[5m]) > 0.1
        for: 2m
        labels:
          severity: warning
          service: database
        annotations:
          summary: "数据库慢查询"
          description: "数据库查询平均耗时超过100ms (当前值: {{ $value }}秒)"

  - name: cache
    rules:
      - alert: LowCacheHitRate
        expr: redis_keyspace_hits_total / (redis_keyspace_hits_total + redis_keyspace_misses_total) * 100 < 80
        for: 5m
        labels:
          severity: warning
          service: cache
        annotations:
          summary: "缓存命中率过低"
          description: "缓存命中率低于80% (当前值: {{ $value }}%)"
          
      - alert: HighRedisMemoryUsage
        expr: redis_memory_used_bytes / redis_memory_max_bytes * 100 > 80
        for: 5m
        labels:
          severity: warning
          service: cache
        annotations:
          summary: "Redis内存使用率过高"
          description: "Redis内存使用率超过80% (当前值: {{ $value }}%)"
```

### 告警通知配置
```yaml
# alertmanager.yml
global:
  smtp_smarthost: 'smtp.example.com:587'
  smtp_from: 'alerts@ai-ready.local'
  smtp_auth_username: 'alertmanager'
  smtp_auth_password: 'password'
  wechat_api_url: 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send'
  wechat_api_secret: 'your-wechat-secret'

route:
  group_by: ['alertname', 'service']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'default-receiver'
  
  routes:
    - match:
        severity: critical
      receiver: 'critical-receiver'
      group_wait: 5s
      
    - match:
        service: database
      receiver: 'dba-receiver'
      group_wait: 10s

receivers:
  - name: 'default-receiver'
    email_configs:
      - to: 'team@ai-ready.local'
        subject: '[Alert] {{ .GroupLabels.alertname }}'
        
  - name: 'critical-receiver'
    wechat_configs:
      - corp_id: 'your-corp-id'
        agent_id: '1000002'
        secret: 'your-secret'
        to_user: '@all'
        message: '{{ template "wechat.default.message" . }}'
        
  - name: 'dba-receiver'
    email_configs:
      - to: 'dba@ai-ready.local'
    webhook_configs:
      - url: 'http://dba-alerts.ai-ready.local/hook'
        send_resolved: true

inhibit_rules:
  - source_match:
      severity: 'critical'
    target_match:
      severity: 'warning'
    equal: ['alertname', 'service']
```

## 可视化配置

### Grafana数据源配置
```yaml
# datasources.yml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
    jsonData:
      timeInterval: 15s
      queryTimeout: 60s
      httpMethod: POST
```

### 监控面板配置

#### 1. 系统概览面板
- **CPU使用率**: 按核心显示使用率
- **内存使用**: 堆叠显示内存使用分布
- **磁盘IO**: 读写速度和IOPS
- **网络流量**: 进出流量和连接数

#### 2. 应用性能面板
- **API响应时间**: P50/P95/P99响应时间
- **请求量**: QPS和请求类型分布
- **错误率**: HTTP状态码分布
- **JVM状态**: 堆内存、GC情况、线程数

#### 3. 数据库性能面板
- **查询性能**: 慢查询TOP 10
- **连接池**: 活跃/空闲连接数
- **锁等待**: 锁等待时间和数量
- **缓存命中率**: 查询缓存命中率

#### 4. 业务指标面板
- **订单处理**: 创建成功率、处理时间
- **用户活跃**: 活跃用户数、会话时长
- **库存变化**: 库存水平、预警状态
- **支付处理**: 支付成功率、处理时间

### 面板导入示例
```json
{
  "dashboard": {
    "title": "AI-Ready测试环境监控",
    "tags": ["ai-ready", "test", "monitoring"],
    "timezone": "browser",
    "panels": [
      {
        "title": "CPU使用率",
        "type": "graph",
        "targets": [
          {
            "expr": "100 - (avg by(instance) (rate(node_cpu_seconds_total{mode=\"idle\"}[5m])) * 100)",
            "legendFormat": "{{instance}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 0}
      },
      {
        "title": "内存使用",
        "type": "graph",
        "targets": [
          {
            "expr": "node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes",
            "legendFormat": "已用内存"
          },
          {
            "expr": "node_memory_MemAvailable_bytes",
            "legendFormat": "可用内存"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 0}
      }
    ]
  }
}
```

## 性能测试监控

### 性能测试指标

#### 1. 负载测试监控
```yaml
# 负载测试监控配置
load_test_monitoring:
  concurrent_users: 100
  ramp_up_time: 60s
  duration: 10m
  metrics:
    - response_time_p95 < 2s
    - error_rate < 1%
    - throughput > 1000 req/s
    - cpu_usage < 80%
    - memory_usage < 80%
```

#### 2. 压力测试监控
```yaml
# 压力测试监控配置
stress_test_monitoring:
  concurrent_users: [100, 200, 500, 1000]
  step_duration: 5m
  metrics:
    - response_time_degradation < 50%
    - error_rate_breakpoint: 找出错误率超过5%的拐点
    - resource_saturation: 识别资源饱和点
    - system_recovery: 测试后恢复时间 < 2m
```

#### 3. 稳定性测试监控
```yaml
# 稳定性测试监控配置
stability_test_monitoring:
  concurrent_users: 50
  duration: 24h
  metrics:
    - memory_leak: 内存增长 < 10%/24h
    - response_time_stability: P95波动 < 20%
    - error_rate_stability: 错误率波动 < 1%
    - resource_usage_stability: 资源使用波动 < 10%
```

### 性能测试报告

#### 测试报告模板
```markdown
# 性能测试报告

## 测试概述
- **测试类型**: 负载测试
- **测试时间**: 2026-04-27 14:00-15:00
- **测试环境**: 测试环境
- **测试工具**: JMeter + Prometheus + Grafana

## 测试结果

### 关键指标
| 指标 | 目标值 | 实际值 | 是否达标 |
|------|--------|--------|----------|
| 平均响应时间 | < 1s | 0.8s | ✅ |
| P95响应时间 | < 2s | 1.5s | ✅ |
| 错误率 | < 1% | 0.2% | ✅ |
| 吞吐量 | > 1000 req/s | 1200 req/s | ✅ |
| CPU使用率 | < 80% | 65% | ✅ |
| 内存使用率 | < 80% | 70% | ✅ |

### 性能图表
1. **响应时间趋势图**: [图表链接]
2. **吞吐量趋势图**: [图表链接]
3. **错误率趋势图**: [图表链接]
4. **资源使用趋势图**: [图表链接]

### 问题发现
1. **问题1**: 数据库连接池在100并发时出现等待
   - **影响**: 响应时间增加20%
   - **建议**: 增加连接池大小从50到100
   
2. **问题2**: Redis缓存命中率从85%下降到75%
   - **影响**: 数据库压力增加
   - **建议**: 优化缓存策略，增加热门数据缓存

### 建议和优化
1. **立即优化**:
   - 调整数据库连接池配置
   - 优化Redis缓存策略
   
2. **短期优化**:
   - 添加数据库查询缓存
   - 优化慢查询SQL
   
3. **长期优化**:
   - 考虑数据库读写分离
   - 引入CDN缓存静态资源

## 附录
- [详细测试数据](./performance-data.csv)
- [监控图表导出](./monitoring-charts.zip)
- [测试脚本](./jmeter-test-plan.jmx)
```

## 容量规划监控

### 容量指标

#### 1. 用户容量
```yaml
user_capacity:
  current_active_users: 1000
  peak_concurrent_users: 200
  user_growth_rate: 10%每月
  capacity_thresholds:
    warning: 70%
    critical: 85%
  scaling_indicators:
    - concurrent_users > 500
    - api_response_time_p95 > 2s
    - database_connections > 80%
```

#### 2. 数据容量
```yaml
data_capacity:
  database_size: 50GB
  daily_growth: 1GB
  retention_policy: 90天
  capacity_thresholds:
    warning: 60%
    critical: 80%
  cleanup_strategies:
    - 归档历史数据
    - 清理测试数据
    - 压缩数据库表
```

#### 3. 资源容量
```yaml
resource_capacity:
  cpu_cores: 8
  memory_gb: 16
  storage_gb: 200
  current_utilization:
    cpu: 40%
    memory: 60%
    storage: 30%
  scaling_triggers:
    - cpu > 70%持续10分钟
    - memory > 80%持续10分钟
    - storage > 70%
```

### 容量预测模型

#### 线性回归预测
```python
# 容量预测脚本
import pandas as pd
from sklearn.linear_model import LinearRegression
import numpy as np

# 加载历史数据
historical_data = pd.read_csv('historical_capacity.csv')

# 训练预测模型
model = LinearRegression()
X = historical_data[['days', 'user_growth']]
y = historical_data['resource_usage']

model.fit(X, y)

# 预测未来30天
future_days = np.arange(historical_data['days'].max() + 1, 
                       historical_data['days'].max() + 31).reshape(-1, 1)
future_growth = historical_data['user_growth'].iloc[-1] * 1.1  # 10%增长

predictions = model.predict(pd.DataFrame({
    'days': future_days.flatten(),
    'user_growth': future_growth
}))
```

#### 容量预警规则
```yaml
capacity_alert_rules:
  - alert: ApproachingUserCapacity
    expr: current_active_users / max_supported_users * 100 > 70
    for: 7d
    annotations:
      summary: "用户容量接近上限"
      description: "当前用户数达到最大容量的{{ $value }}%，建议扩容"
      
  - alert: ApproachingDataCapacity
    expr: database_size_bytes / max_database_size_bytes * 100 > 60
    for: 3d
    annotations:
      summary: "数据容量接近上限"
      description: "数据库大小达到最大容量的{{ $value }}%，建议清理数据或扩容"
      
  - alert: HighResourceUtilization
    expr: avg_over_time(resource_usage_percentage[7d]) > 70
    for: 1d
    annotations:
      summary: "资源使用率持续高位"
      description: "资源使用率连续7天超过70%，建议扩容"
```

## 监控优化建议

### 监控数据优化

#### 1. 指标采样优化
```yaml
# Prometheus配置优化
global:
  scrape_interval: 30s  # 从15s调整为30s
  evaluation_interval: 30s
  
# 按重要性调整抓取间隔
scrape_configs:
  - job_name: 'critical'
    scrape_interval: 15s
    static_configs:
      - targets: ['api:8080', 'postgres:5432']
      
  - job_name: 'important'
    scrape_interval: 30s
    static_configs:
      - targets: ['redis:6379', 'cache:11211']
      
  - job_name: 'normal'
    scrape_interval: 60s
    static_configs:
      - targets: ['node-exporter:9100']
```

#### 2. 数据保留策略
```yaml
# 分层数据保留
retention:
  raw_data: 15d      # 原始数据保留15天
  hourly_aggregates: 30d  # 小时聚合数据保留30天
  daily_aggregates: 90d   # 天聚合数据保留90天
  monthly_aggregates: 1y  # 月聚合数据保留1年
  
# 使用Prometheus远程存储
remote_write:
  - url: "http://thanos-receive:10908/api/v1/receive"
    remote_timeout: 30s
    write_relabel_configs:
      - action: keep
        regex: "important.*"
```

### 告警优化

#### 1. 告警分级
```yaml
alert_severity_levels:
  critical:
    - 服务不可用
    - 数据丢失
    - 安全漏洞
    - 响应时间: P95 > 5s
    - 错误率: > 10%
    
  warning:
    - 性能下降
    - 资源使用率高
    - 容量接近上限
    - 响应时间: P95 > 2s
    - 错误率: > 5%
    
  info:
    - 配置变更
    - 部署完成
    - 维护通知
    - 趋势预警
```

#### 2. 告警降噪
```yaml
# 告警抑制规则
inhibit_rules:
  # 当有critical告警时，抑制相关的warning告警
  - source_match:
      severity: 'critical'
    target_match:
      severity: 'warning'
    equal: ['alertname', 'instance']
    
  # 当数据库宕机时，抑制依赖数据库的告警
  - source_match:
      alertname: 'DatabaseDown'
    target_match_re:
      service: 'api|cache|queue'
```

