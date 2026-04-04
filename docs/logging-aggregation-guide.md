# AI-Ready 日志聚合配置文档

**项目**: 智企连·AI-Ready  
**版本**: v1.0  
**日期**: 2026-04-03  
**负责人**: devops-engineer

---

## 一、日志架构

### 1.1 整体架构

```
┌──────────────────────────────────────────────────────────────────┐
│                       日志聚合架构                                │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐                 │
│  │ App Pod 1  │  │ App Pod 2  │  │ App Pod N  │                 │
│  │ logback    │  │ logback    │  │ logback    │                 │
│  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘                 │
│        │               │               │                         │
│        └───────────────┼───────────────┘                         │
│                        ▼                                         │
│              ┌─────────────────┐                                 │
│              │   Filebeat     │ ◄── 日志采集                     │
│              │  (DaemonSet)   │                                  │
│              └────────┬────────┘                                 │
│                       │                                          │
│                       ▼                                          │
│              ┌─────────────────┐                                 │
│              │   Logstash     │ ◄── 日志处理                     │
│              │  (解析/过滤)    │                                  │
│              └────────┬────────┘                                 │
│                       │                                          │
│                       ▼                                          │
│              ┌─────────────────┐                                 │
│              │ Elasticsearch   │ ◄── 日志存储                     │
│              │  (索引/检索)    │                                  │
│              └────────┬────────┘                                 │
│                       │                                          │
│                       ▼                                          │
│              ┌─────────────────┐                                 │
│              │    Kibana      │ ◄── 可视化                       │
│              │  (仪表盘/告警)  │                                  │
│              └─────────────────┘                                 │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### 1.2 组件说明

| 组件 | 版本 | 用途 | 资源配置 |
|------|------|------|----------|
| Filebeat | 8.10+ | 日志采集 | 500m CPU, 512Mi 内存 |
| Logstash | 8.10+ | 日志处理 | 1 CPU, 2Gi 内存 |
| Elasticsearch | 8.10+ | 日志存储 | 2 CPU, 4Gi 内存 x3 |
| Kibana | 8.10+ | 可视化 | 500m CPU, 1Gi 内存 |

---

## 二、日志规范

### 2.1 日志格式标准

**JSON格式要求**：

```json
{
  "timestamp": "2026-04-03T19:30:00.123+08:00",
  "level": "INFO",
  "thread": "http-nio-8080-exec-1",
  "logger": "com.ai.ready.controller.UserController",
  "message": "用户登录成功",
  "trace_id": "abc123def456",
  "service": "ai-ready-api",
  "environment": "prod",
  "user_id": "10001",
  "extra": {
    "ip": "192.168.1.100",
    "duration_ms": 45
  }
}
```

### 2.2 必需字段

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| timestamp | string | ISO8601时间戳 | 2026-04-03T19:30:00.123+08:00 |
| level | string | 日志级别 | INFO/WARN/ERROR |
| message | string | 日志内容 | 用户登录成功 |
| service | string | 服务名称 | ai-ready-api |
| environment | string | 环境 | dev/staging/prod |
| trace_id | string | 链路追踪ID | abc123def456 |

### 2.3 日志级别规范

| 级别 | 使用场景 | 示例 |
|------|----------|------|
| DEBUG | 开发调试信息 | SQL语句、参数详情 |
| INFO | 关键业务流程 | 用户登录、订单创建 |
| WARN | 潜在问题 | 慢查询、资源紧张 |
| ERROR | 错误异常 | 异常堆栈、外部服务不可用 |
| FATAL | 严重错误 | 服务启动失败、数据库连接断开 |

---

## 三、日志收集配置

### 3.1 应用端配置

**logback-spring.xml 配置要点**：

```xml
<!-- 使用LogstashEncoder输出JSON -->
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <customFields>{"service":"ai-ready-api","environment":"${ENV}"}</customFields>
    <includeMdcKeyName>traceId</includeMdcKeyName>
</encoder>

<!-- 异步日志提升性能 -->
<appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
    <queueSize>10000</queueSize>
    <discardingThreshold>0</discardingThreshold>
    <appender-ref ref="FILE"/>
</appender>
```

### 3.2 Filebeat配置

**关键配置项**：

```yaml
filebeat.inputs:
  - type: log
    paths: ["/var/log/ai-ready/*.log"]
    json.keys_under_root: true  # 解析JSON
    fields:
      service: ai-ready-api
    fields_under_root: true

output.logstash:
  hosts: ["logstash:5044"]
  bulk_max_size: 2048
```

### 3.3 Logstash Pipeline

**处理流程**：

```
Input → JSON解析 → 字段提取 → 错误标记 → Output
```

```ruby
filter {
  json { source => "message" }
  
  if [level] in ["ERROR", "FATAL"] {
    mutate { add_tag => ["alert"] }
  }
}

output {
  elasticsearch {
    index => "ai-ready-%{[log_type]}-%{+YYYY.MM.dd}"
  }
}
```

---

## 四、日志存储策略

### 4.1 索引模板

| 索引模式 | 保留周期 | 分片数 | 副本数 |
|----------|----------|--------|--------|
| ai-ready-application-* | 30天 | 3 | 1 |
| ai-ready-error-* | 60天 | 2 | 1 |
| ai-ready-access-* | 14天 | 3 | 1 |
| ai-ready-gc-* | 7天 | 1 | 0 |

### 4.2 ILM策略

```json
{
  "policy": {
    "phases": {
      "hot": {
        "min_age": "0ms",
        "actions": {
          "rollover": { "max_size": "50gb", "max_age": "1d" }
        }
      },
      "warm": {
        "min_age": "7d",
        "actions": {
          "shrink": { "number_of_shards": 1 },
          "forcemerge": { "max_num_segments": 1 }
        }
      },
      "cold": {
        "min_age": "30d",
        "actions": {
          "freeze": {}
        }
      },
      "delete": {
        "min_age": "60d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}
```

---

## 五、日志检索

### 5.1 Kibana查询语法

| 查询场景 | KQL语法 |
|----------|---------|
| 查找ERROR日志 | `level: ERROR` |
| 按服务查询 | `service: "ai-ready-api"` |
| 按trace_id查询 | `trace_id: "abc123"` |
| 组合查询 | `level: ERROR AND service: "ai-ready-api"` |
| 时间范围 | `@timestamp >= "2026-04-03" AND @timestamp < "2026-04-04"` |

### 5.2 常用仪表盘

| 仪表盘名称 | 用途 |
|------------|------|
| 应用日志总览 | 错误趋势、日志量统计 |
| 错误分析 | ERROR/FATAL详情、堆栈追踪 |
| 性能分析 | 慢请求、响应时间分布 |
| 安全审计 | 登录日志、敏感操作 |

---

## 六、告警配置

### 6.1 Kibana告警规则

| 规则名称 | 触发条件 | 通知方式 |
|----------|----------|----------|
| 高错误率 | 5分钟内ERROR > 100 | 钉钉+邮件 |
| 服务异常 | 服务无日志输出超过5分钟 | 电话 |
| 敏感操作 | 检测到敏感数据访问 | 邮件 |

---

## 七、运维手册

### 7.1 日常运维

```bash
# 检查ES集群健康
curl -X GET "localhost:9200/_cluster/health?pretty"

# 查看索引状态
curl -X GET "localhost:9200/_cat/indices?v&health=yellow"

# 检查Logstash状态
curl -X GET "localhost:9600/_node/stats/pipelines?pretty"

# 查看Filebeat注册状态
curl -X GET "localhost:5066/inputs?pretty"
```

### 7.2 故障排查

| 问题 | 可能原因 | 解决方案 |
|------|----------|----------|
| 日志延迟 | ES写入瓶颈 | 扩容ES节点、优化索引 |
| 日志丢失 | Filebeat队列满 | 增加queue_size、扩容 |
| 检索慢 | 索引过大 | 配置ILM、优化查询 |

---

## 八、文件清单

| 文件路径 | 说明 |
|----------|------|
| `logging/filebeat/filebeat.yml` | Filebeat配置 |
| `logging/logstash/pipeline/ai-ready.conf` | Logstash Pipeline |
| `logging/logback-spring.xml` | 应用日志配置 |
| `docs/logging-aggregation-guide.md` | 本文档 |

---

**文档生成**: devops-engineer  
**版本**: v1.0
