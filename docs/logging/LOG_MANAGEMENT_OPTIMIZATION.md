# AI-Ready 日志管理优化方案

> **项目**: 智企连·AI-Ready  
> **版本**: v1.0  
> **最后更新**: 2026-04-08  
> **维护者**: devops-engineer

---

## 一、日志管理现状分析

### 1.1 现有日志体系

| 组件 | 日志方式 | 格式 | 问题 |
|------|----------|------|------|
| Spring Boot应用 | Logback → 文件 | 文本/JSON混合 | 格式不统一 |
| PostgreSQL | pg_log | CSV | 未集中采集 |
| Redis | stdout | 文本 | 无结构化 |
| Nginx | access.log/error.log | Combined格式 | 未关联TraceID |
| RocketMQ | 文件日志 | 文本 | 量大无轮转 |

### 1.2 核心问题

| 问题 | 影响 | 严重程度 |
|------|------|----------|
| 日志格式不统一 | 检索困难，解析效率低 | 🔴 高 |
| 缺少TraceID关联 | 无法追踪跨服务调用链 | 🔴 高 |
| 日志无分级存储 | 存储成本高，查询慢 | 🟡 中 |
| 无自动轮转清理 | 磁盘空间风险 | 🟡 中 |
| 敏感信息未脱敏 | 安全合规风险 | 🔴 高 |
| 日志采集延迟 | 故障发现慢 | 🟡 中 |

---

## 二、日志采集策略优化

### 2.1 统一日志格式规范

```json
{
  "timestamp": "2026-04-08T22:30:00.000+08:00",
  "level": "INFO",
  "service": "ai-ready-api",
  "module": "user-service",
  "trace_id": "abc123def456",
  "span_id": "span789",
  "user_id": "u_001",
  "thread": "http-nio-8080-exec-1",
  "class": "com.aiready.controller.UserController",
  "method": "getUser",
  "message": "Query user successfully",
  "extra": {
    "request_path": "/api/v1/users/001",
    "http_method": "GET",
    "status_code": 200,
    "duration_ms": 45
  }
}
```

**必填字段规范**：

| 字段 | 类型 | 说明 | 必填 |
|------|------|------|------|
| timestamp | ISO8601 | 时间戳（含时区） | ✅ |
| level | ENUM | DEBUG/INFO/WARN/ERROR/FATAL | ✅ |
| service | string | 服务名称 | ✅ |
| trace_id | string | 链路追踪ID | ✅ |
| message | string | 日志内容 | ✅ |

### 2.2 采集架构优化

```
┌──────────────────────────────────────────────────────────┐
│              日志采集优化架构                              │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐           │
│  │App日志 │ │PG日志  │ │Redis   │ │Nginx   │           │
│  │JSON    │ │CSV     │ │文本    │ │Combined│           │
│  └───┬────┘ └───┬────┘ └───┬────┘ └───┬────┘           │
│      │          │          │          │                   │
│      ▼          ▼          ▼          ▼                   │
│  ┌────────────────────────────────────────────┐          │
│  │           Filebeat (每节点)                 │          │
│  │  - 多输入源配置                             │          │
│  │  - JSON/CSV/Multiline解析                  │          │
│  │  - 字段enrichment (host/service/env)       │          │
│  │  - 敏感字段脱敏                             │          │
│  └──────────────────┬─────────────────────────┘          │
│                     │                                    │
│                     ▼                                    │
│  ┌────────────────────────────────────────────┐          │
│  │           Logstash (集中处理)               │          │
│  │  - Grok解析非结构化日志                     │          │
│  │  - TraceID注入/关联                         │          │
│  │  - 日志级别标准化                           │          │
│  │  - GeoIP enrichment                        │          │
│  └──────────────────┬─────────────────────────┘          │
│                     │                                    │
│                     ▼                                    │
│  ┌────────────────────────────────────────────┐          │
│  │           Elasticsearch (存储+索引)         │          │
│  │  - ILM生命周期管理                          │          │
│  │  - 按服务/级别/日期分索引                    │          │
│  │  - 冷热数据分层                             │          │
│  └──────────────────┬─────────────────────────┘          │
│                     │                                    │
│          ┌──────────┴──────────┐                         │
│          ▼                     ▼                         │
│  ┌─────────────┐       ┌─────────────┐                  │
│  │   Kibana    │       │  Alerting   │                  │
│  │  可视化查询 │       │  日志告警   │                  │
│  └─────────────┘       └─────────────┘                  │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

### 2.3 Filebeat采集配置优化

```yaml
# filebeat.yml 优化配置
filebeat.inputs:
  # 应用日志 - JSON格式
  - type: log
    enabled: true
    paths:
      - /var/log/ai-ready/*.log
    json.keys_under_root: true
    json.add_error_key: true
    json.message_key: message
    fields:
      service: ai-ready-api
      env: ${ENV:prod}
    fields_under_root: true
    processors:
      - drop_fields:
          fields: ["agent", "ecs", "input", "log.offset"]
      - decode_json_fields:
          fields: ["message"]
          target: ""
          overwrite_keys: true

  # PostgreSQL慢查询日志
  - type: log
    enabled: true
    paths:
      - /var/log/postgresql/postgresql-*.log
    multiline:
      pattern: '^\d{4}-\d{2}-\d{2}'
      negate: true
      match: after
    fields:
      service: postgresql
      log_type: slow_query
    processors:
      - grok:
          patterns:
            - "%{TIMESTAMP_ISO8601:pg_timestamp} %{WORD:pg_level}.*duration: %{NUMBER:duration_ms} ms"

  # Nginx访问日志
  - type: log
    enabled: true
    paths:
      - /var/log/nginx/access.log
    fields:
      service: nginx
      log_type: access
    processors:
      - grok:
          patterns:
            - '%{IP:client_ip} - - \\[%{HTTPDATE:timestamp}\\] "%{WORD:method} %{URIPATH:path} HTTP/%{NUMBER:http_version}" %{NUMBER:status_code} %{NUMBER:bytes}'

# 输出到Logstash
output.logstash:
  hosts: ["logstash:5044"]
  loadbalance: true
  worker: 4
  bulk_max_size: 2048

# 性能优化
queue.mem:
  events: 4096
  flush.min_events: 2048
  flush.timeout: 1s

logging.level: info
logging.to_files: true
logging.files:
  path: /var/log/filebeat
  name: filebeat
  keepfiles: 7
  rotateeverybytes: 10485760  # 10MB
```

### 2.4 Logstash处理管道优化

```ruby
# logstash.conf
input {
  beats {
    port => 5044
    codec => json {
      charset => "UTF-8"
    }
  }
}

filter {
  # TraceID注入 - 如果缺失则生成
  if ![trace_id] {
    mutate {
      add_field => { "trace_id" => "%{[host][name]}-%{[@timestamp]}-%{[sequence]}" }
    }
  }

  # 敏感信息脱敏
  mutate {
    gsub => [
      "message", "(password|passwd|pwd)\s*[:=]\s*\S+", "\1=***",
      "message", "(token|secret|key)\s*[:=]\s*\S+", "\1=***",
      "message", "(phone|mobile)\s*[:=]\s*\d{11}", "\1=***",
      "message", "(id_card|idcard)\s*[:=]\s*\d{17}[\dXx]", "\1=***"
    ]
  }

  # 日志级别标准化
  mutate {
    gsub => [
      "level", "WARNING", "WARN",
      "level", "FATAL", "ERROR"
    ]
  }

  # 添加环境标签
  mutate {
    add_field => {
      "[@metadata][target_index]" => "ai-ready-%{service}-%{+YYYY.MM.dd}"
    }
  }

  # 去除冗余字段
  mutate {
    remove_field => ["[agent]", "[ecs]", "[input]", "[log][offset]"]
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "%{[@metadata][target_index]}"
    ilm_enabled => true
    ilm_rollover_alias => "ai-ready-logs"
    ilm_pattern => "000001"
    ilm_policy => "ai-ready-lifecycle"
  }
}
```

---

## 三、日志存储方案优化

### 3.1 索引生命周期管理 (ILM)

```json
{
  "policy": {
    "phases": {
      "hot": {
        "min_age": "0ms",
        "actions": {
          "rollover": {
            "max_size": "50GB",
            "max_age": "1d",
            "max_docs": 100000000
          },
          "set_priority": { "priority": 100 }
        }
      },
      "warm": {
        "min_age": "7d",
        "actions": {
          "allocate": {
            "number_of_replicas": 1
          },
          "shrink": { "number_of_shards": 1 },
          "forcemerge": { "max_num_segments": 1 },
          "set_priority": { "priority": 50 }
        }
      },
      "cold": {
        "min_age": "30d",
        "actions": {
          "freeze": {},
          "set_priority": { "priority": 0 }
        }
      },
      "delete": {
        "min_age": "90d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}
```

### 3.2 分级存储策略

| 存储层 | 时间范围 | 节点类型 | 副本数 | 查询性能 | 存储成本 |
|--------|----------|----------|--------|----------|----------|
| Hot | 0-7天 | SSD | 2 | <1s | 高 |
| Warm | 7-30天 | HDD | 1 | <5s | 中 |
| Cold | 30-90天 | 归档 | 0 | <30s | 低 |
| 归档 | >90天 | OSS/S3 | 0 | 分钟级 | 极低 |

### 3.3 索引模板

```json
{
  "index_patterns": ["ai-ready-*"],
  "template": {
    "settings": {
      "number_of_shards": 3,
      "number_of_replicas": 1,
      "refresh_interval": "5s",
      "analysis": {
        "analyzer": {
          "log_analyzer": {
            "type": "pattern",
            "pattern": "\\W+"
          }
        }
      }
    },
    "mappings": {
      "properties": {
        "timestamp": { "type": "date", "format": "strict_date_optional_time||epoch_millis" },
        "level": { "type": "keyword" },
        "service": { "type": "keyword" },
        "trace_id": { "type": "keyword" },
        "span_id": { "type": "keyword" },
        "user_id": { "type": "keyword" },
        "message": { "type": "text", "analyzer": "log_analyzer" },
        "duration_ms": { "type": "float" },
        "status_code": { "type": "integer" },
        "host": { "type": "keyword" },
        "env": { "type": "keyword" }
      }
    }
  }
}
```

---

## 四、日志轮转与清理

### 4.1 应用日志轮转 (Logback)

```xml
<configuration>
  <appender name="ROLLING" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>/var/log/ai-ready/application.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
      <fileNamePattern>/var/log/ai-ready/application.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
      <maxFileSize>200MB</maxFileSize>
      <maxHistory>30</maxHistory>
      <totalSizeCap>10GB</totalSizeCap>
      <cleanHistoryOnStart>true</cleanHistoryOnStart>
    </rollingPolicy>
    <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
      <layout class="ch.qos.logback.classic.PatternLayout">
        <pattern>${LOG_FORMAT:-%d{yyyy-MM-dd'T'HH:mm:ss.SSSZ}|%level|%thread|%logger{36}|%msg%n}</pattern>
      </layout>
    </encoder>
  </appender>
</configuration>
```

### 4.2 系统日志清理

```bash
#!/bin/bash
# log-cleanup.sh - 日志自动清理脚本

LOG_DIR="/var/log/ai-ready"
RETENTION_DAYS=30
COMPRESS_DAYS=7

# 清理过期日志
find ${LOG_DIR} -name "*.log" -mtime +${RETENTION_DAYS} -delete
find ${LOG_DIR} -name "*.gz" -mtime +${RETENTION_DAYS} -delete

# 压缩旧日志
find ${LOG_DIR} -name "*.log" -mtime +${COMPRESS_DAYS} ! -name "*.gz" -exec gzip {} \;

# 清理空文件
find ${LOG_DIR} -type f -empty -delete

echo "[$(date)] Log cleanup completed"
```

---

## 五、日志告警优化

### 5.1 基于日志的告警规则

| 规则 | 条件 | 级别 | 通知渠道 |
|------|------|------|----------|
| ERROR激增 | 5分钟内ERROR>50 | P0 | 钉钉+短信 |
| 慢查询 | 单次SQL>5s | P1 | 钉钉 |
| 登录异常 | 同IP 5分钟失败>10次 | P0 | 钉钉+短信 |
| OOM预警 | heap使用>90% | P0 | 钉钉+短信 |
| 线程池耗尽 | 活跃线程>95% | P1 | 钉钉 |

### 5.2 ElastAlert配置示例

```yaml
name: error_spike_alert
type: frequency
index: ai-ready-*
num_events: 50
timeframe:
  minutes: 5
filter:
  - term:
      level: "ERROR"
alert:
  - dingtalk
  - command
dingtalk:
  webhook: "https://oapi.dingtalk.com/robot/send?access_token=xxx"
  message: "⚠️ AI-Ready ERROR激增: 5分钟内{num_hits}个ERROR，请检查!"
command: ["/opt/scripts/auto-diagnose.sh"]
```

---

## 六、优化实施计划

| 阶段 | 时间 | 内容 | 目标 |
|------|------|------|------|
| 第一阶段 | 第1周 | 日志格式统一 + Filebeat配置优化 | 格式统一率100% |
| 第二阶段 | 第2周 | Logstash管道 + 敏感信息脱敏 | 脱敏覆盖率100% |
| 第三阶段 | 第3周 | ILM存储策略 + 索引模板 | 存储成本降低40% |
| 第四阶段 | 第4周 | 日志告警 + 清理脚本 | 告警响应<1min |

### 验收标准

- [ ] 日志格式统一为JSON，必填字段覆盖率100%
- [ ] TraceID关联覆盖率>95%
- [ ] 敏感信息脱敏率100%（密码/Token/手机号/身份证）
- [ ] 日志采集延迟<10s
- [ ] 存储成本降低40%（ILM分层）
- [ ] 磁盘空间告警0次（自动轮转+清理）

---

## 附录：相关文档

- [日志采集系统](./Log-Collection-System.md)
- [日志告警配置](../alerting/Log-Alert-Configuration.md)
- [日志审计方案](../devops/LOG_AUDIT_GUIDE.md)
- [监控告警系统](../monitoring/MONITORING-ALERTING.md)
