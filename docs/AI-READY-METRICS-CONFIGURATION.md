# AI-Ready 服务监控指标配置文档

## 📋 文档概述

| 项目 | 内容 |
|------|------|
| 项目名称 | AI-Ready (智企连) |
| 文档版本 | v1.0 |
| 创建日期 | 2026-04-01 |
| 创建者 | devops-engineer |
| 文档目的 | 配置服务监控指标采集，支持Prometheus集成 |

---

## 1️⃣ 应用层监控指标配置

### 1.1 ApiMetricsConfig 配置

**代码位置**: `core-base/src/main/java/cn/aiedge/base/config/metrics/ApiMetricsConfig.java`

| 指标名称 | 类型 | 说明 | 标签 |
|----------|------|------|------|
| `ai_ready_api_request_total` | Counter | API请求总量 | service, endpoint, method, status |
| `ai_ready_api_response_time` | Timer | API响应时间 | service, endpoint, method |
| `ai_ready_api_error_total` | Counter | API错误总数 | service, endpoint, error_type |
| `ai_ready_api_active_requests` | Gauge | 活跃请求数 | service |

### 1.2 Prometheus查询示例

```promql
# 请求速率 (每秒请求量)
rate(ai_ready_api_request_total[5m])

# 平均响应时间
rate(ai_ready_api_response_time_sum[5m]) / rate(ai_ready_api_response_time_count[5m])

# 错误率
rate(ai_ready_api_error_total[5m]) / rate(ai_ready_api_request_total[5m])

# 活跃请求数
ai_ready_api_active_requests
```

---

## 2️⃣ JVM监控指标配置

### 2.1 JvmMetricsConfig 配置

**代码位置**: `core-base/src/main/java/cn/aiedge/base/config/metrics/JvmMetricsConfig.java`

| 指标名称 | 类型 | 说明 |
|----------|------|------|
| `jvm_heap_used_bytes` | Gauge | JVM堆内存使用量 |
| `jvm_heap_max_bytes` | Gauge | JVM堆内存最大值 |
| `jvm_heap_usage_ratio` | Gauge | JVM堆内存使用率(%) |
| `jvm_nonheap_used_bytes` | Gauge | JVM非堆内存使用量 |
| `jvm_threads_active` | Gauge | 活跃线程数 |
| `jvm_threads_peak` | Gauge | 峰值线程数 |
| `jvm_threads_daemon` | Gauge | Daemon线程数 |

### 2.2 Prometheus查询示例

```promql
# JVM堆内存使用率
jvm_heap_usage_ratio

# JVM堆内存使用量(MB)
jvm_heap_used_bytes / 1024 / 1024

# 活跃线程数趋势
jvm_threads_active

# 内存使用趋势(1小时)
rate(jvm_heap_used_bytes[1h])
```

---

## 3️⃣ 数据库连接池监控指标

### 3.1 DatabaseMetricsConfig 配置

**代码位置**: `core-base/src/main/java/cn/aiedge/base/config/metrics/DatabaseMetricsConfig.java`

| 指标名称 | 类型 | 说明 |
|----------|------|------|
| `ai_ready_db_pool_active_connections` | Gauge | 活跃数据库连接数 |
| `ai_ready_db_pool_idle_connections` | Gauge | 空闲数据库连接数 |
| `ai_ready_db_pool_total_connections` | Gauge | 总数据库连接数 |
| `ai_ready_db_pool_max_connections` | Gauge | 最大数据库连接数 |
| `ai_ready_db_pool_pending_connections` | Gauge | 等待连接的线程数 |

### 3.2 Prometheus查询示例

```promql
# 连接池使用率
ai_ready_db_pool_active_connections / ai_ready_db_pool_max_connections * 100

# 空闲连接数
ai_ready_db_pool_idle_connections

# 等待连接线程数(阻塞告警)
ai_ready_db_pool_pending_connections > 0
```

### 3.3 连接池配置参考

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 30000
      pool-name: AiReadyHikariPool
      max-lifetime: 1800000
      connection-timeout: 30000
```

---

## 4️⃣ Redis监控指标配置

### 4.1 RedisMetricsConfig 配置

**代码位置**: `core-base/src/main/java/cn/aiedge/base/config/metrics/RedisMetricsConfig.java`

| 指标名称 | 类型 | 说明 |
|----------|------|------|
| `ai_ready_redis_connection_status` | Gauge | Redis连接状态(1/0) |
| `ai_ready_redis_memory_used_bytes` | Gauge | Redis内存使用量 |
| `ai_ready_redis_memory_max_bytes` | Gauge | Redis最大内存 |
| `ai_ready_redis_connected_clients` | Gauge | 连接客户端数 |
| `ai_ready_redis_key_count` | Gauge | 键总数 |
| `ai_ready_redis_response_time_ms` | Gauge | 响应时间(ms) |
| `ai_ready_redis_hit_total` | Counter | 缓存命中次数 |
| `ai_ready_redis_miss_total` | Counter | 缓存未命中次数 |

### 4.2 Prometheus查询示例

```promql
# Redis连接状态
ai_ready_redis_connection_status

# Redis内存使用(MB)
ai_ready_redis_memory_used_bytes / 1024 / 1024

# 缓存命中率
rate(ai_ready_redis_hit_total[5m]) / (rate(ai_ready_redis_hit_total[5m]) + rate(ai_ready_redis_miss_total[5m]))

# Redis响应时间
ai_ready_redis_response_time_ms
```

---

## 5️⃣ Prometheus集成配置

### 5.1 Spring Boot配置

**配置文件**: `application.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    prometheus:
      enabled: true
  prometheus:
    metrics:
      export:
        enabled: true
  metrics:
    tags:
      application: ai-ready
      environment: ${spring.profiles.active}
```

### 5.2 Prometheus抓取配置

**prometheus.yml配置**:

```yaml
scrape_configs:
  - job_name: 'ai-ready'
    scrape_interval: 15s
    scrape_timeout: 10s
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
        labels:
          instance: 'ai-ready-local'
          environment: 'dev'
    relabel_configs:
      - source_labels: [__address__]
        target_label: hostname
        replacement: 'ai-ready-server'
```

### 5.3 Grafana Dashboard导入

推荐使用以下Dashboard:
- JVM Dashboard: ID 4701
- Spring Boot Dashboard: ID 12900
- HikariCP Dashboard: 自定义
- Redis Dashboard: ID 763

---

## 6️⃣ 告警规则配置

### 6.1 Prometheus告警规则

**alert_rules.yml**:

```yaml
groups:
  - name: ai-ready-api-alerts
    rules:
      # API错误率告警
      - alert: HighApiErrorRate
        expr: rate(ai_ready_api_error_total[5m]) / rate(ai_ready_api_request_total[5m]) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "API错误率超过5%"
          description: "API错误率: {{ $value | humanizePercentage }}"

      # API响应时间告警
      - alert: SlowApiResponseTime
        expr: rate(ai_ready_api_response_time_sum[5m]) / rate(ai_ready_api_response_time_count[5m]) > 2000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "API响应时间超过2秒"
          description: "平均响应时间: {{ $value }}ms"

  - name: ai-ready-jvm-alerts
    rules:
      # JVM内存告警
      - alert: HighJvmMemoryUsage
        expr: jvm_heap_usage_ratio > 80
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "JVM堆内存使用超过80%"
          description: "当前使用率: {{ $value }}%"

      # JVM内存严重告警
      - alert: CriticalJvmMemoryUsage
        expr: jvm_heap_usage_ratio > 95
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "JVM堆内存使用超过95%"
          description: "当前使用率: {{ $value }}%"

  - name: ai-ready-db-alerts
    rules:
      # 连接池告警
      - alert: HighDbPoolUsage
        expr: ai_ready_db_pool_active_connections / ai_ready_db_pool_max_connections > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "数据库连接池使用超过80%"

      # 连接等待告警
      - alert: DbPoolPendingConnections
        expr: ai_ready_db_pool_pending_connections > 5
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "有{{ $value }}个线程等待数据库连接"

  - name: ai-ready-redis-alerts
    rules:
      # Redis连接告警
      - alert: RedisConnectionDown
        expr: ai_ready_redis_connection_status == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Redis连接已断开"

      # Redis内存告警
      - alert: HighRedisMemoryUsage
        expr: ai_ready_redis_memory_used_bytes / ai_ready_redis_memory_max_bytes > 0.8
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Redis内存使用超过80%"
```

---

## 7️⃣ 文件清单

| 文件路径 | 类型 | 说明 |
|----------|------|------|
| `core-base/src/main/java/cn/aiedge/base/config/metrics/ApiMetricsConfig.java` | Java | API监控指标配置 |
| `core-base/src/main/java/cn/aiedge/base/config/metrics/JvmMetricsConfig.java` | Java | JVM监控指标配置 |
| `core-base/src/main/java/cn/aiedge/base/config/metrics/DatabaseMetricsConfig.java` | Java | 数据库连接池监控配置 |
| `core-base/src/main/java/cn/aiedge/base/config/metrics/RedisMetricsConfig.java` | Java | Redis监控指标配置 |
| `core-api/src/main/resources/application.yml` | YAML | Prometheus端点配置 |
| `docs/AI-READY-MONITORING.md` | Markdown | 本配置文档 |

---

## 8️⃣ 验证清单

- [x] Micrometer依赖已配置 (Spring Boot Actuator)
- [x] Prometheus端点已暴露 (/actuator/prometheus)
- [x] API监控指标配置完成
- [x] JVM监控指标配置完成
- [x] 数据库连接池监控配置完成
- [x] Redis监控指标配置完成
- [x] Prometheus告警规则已配置
- [x] 配置文档已输出

---

**文档完成日期**: 2026-04-01  
**作者**: devops-engineer (运维工程师)  
**审核状态**: 待审核