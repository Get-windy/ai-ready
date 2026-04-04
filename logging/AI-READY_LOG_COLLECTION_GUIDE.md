# AI-Ready 日志收集系统配置文档

**版本**: v2.0.0  
**日期**: 2026-04-04  
**作者**: devops-engineer  
**项目**: AI-Ready

---

## 目录

- [一、系统概述](#一系统概述)
- [二、架构设计](#二架构设计)
- [三、日志采集配置](#三日志采集配置)
- [四、日志存储配置](#四日志存储配置)
- [五、日志分析配置](#五日志分析配置)
- [六、告警规则配置](#六告警规则配置)
- [七、部署指南](#七部署指南)
- [八、运维手册](#八运维手册)

---

## 一、系统概述

### 1.1 系统目标

本日志收集系统为AI-Ready项目提供完整的日志解决方案，实现：

1. **统一采集**: 集中采集所有服务的日志
2. **标准化处理**: 统一日志格式，便于检索分析
3. **高效存储**: 分级存储策略，平衡性能与成本
4. **智能分析**: 提供丰富的查询、统计、可视化功能
5. **主动告警**: 实时监控异常，及时通知

### 1.2 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| Filebeat | 7.17.0 | 日志采集 |
| Logstash | 7.17.0 | 日志处理管道 |
| Elasticsearch | 7.17.0 | 日志存储与检索 |
| Kibana | 7.17.0 | 可视化分析 |
| Curator | 5.8.3 | 日志清理 |

### 1.3 日志类型

| 日志类型 | 文件路径 | 索引前缀 | 保留期 |
|----------|----------|----------|--------|
| 应用日志 | /var/log/ai-ready/application.log | ai-ready-application | 90天 |
| 错误日志 | /var/log/ai-ready/error.log | ai-ready-error | 180天 |
| 访问日志 | /var/log/ai-ready/access.log | ai-ready-access | 30天 |
| 审计日志 | /var/log/ai-ready/audit.log | ai-ready-audit | 365天 |
| SQL日志 | /var/log/ai-ready/sql.log | ai-ready-sql | 30天 |
| GC日志 | /var/log/ai-ready/gc/*.log | ai-ready-gc | 30天 |
| Agent日志 | /var/log/ai-ready/agent/*.log | ai-ready-agent | 90天 |

---

## 二、架构设计

### 2.1 整体架构

```
┌──────────────────────────────────────────────────────────────────────┐
│                           AI-Ready 应用层                              │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐    │
│  │ Core API│  │ Agent   │  │ Nginx   │  │ Database│  │ Others  │    │
│  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘    │
└───────┼────────────┼────────────┼────────────┼────────────┼──────────┘
        │            │            │            │            │
        ▼            ▼            ▼            ▼            ▼
┌──────────────────────────────────────────────────────────────────────┐
│                           日志文件层                                   │
│  /var/log/ai-ready/*.log                                             │
└──────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────────────────┐
│                          Filebeat 采集层                               │
│  - 多日志源采集                                                        │
│  - JSON解析                                                            │
│  - 敏感信息过滤                                                        │
│  - 主机/容器元数据添加                                                  │
└──────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────────────────┐
│                         Logstash 处理层                                │
│  - JSON解析与字段提取                                                  │
│  - 多行日志合并                                                        │
│  - 日志级别分类                                                        │
│  - 异常堆栈解析                                                        │
│  - SQL/GC日志解析                                                      │
│  - 地理位置解析                                                        │
│  - 敏感信息脱敏                                                        │
└──────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────────────────┐
│                       Elasticsearch 存储层                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                   │
│  │ Hot (7天)   │→ │ Warm (23天) │→ │ Cold (60天) │→ Delete          │
│  │ SSD高性能   │  │ HDD低成本   │  │ 对象存储    │                   │
│  └─────────────┘  └─────────────┘  └─────────────┘                   │
│                                                                       │
│  - 索引生命周期管理(ILM)                                               │
│  - 冷热数据分离                                                        │
│  - 自动索引滚动                                                        │
└──────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────────────────┐
│                        Kibana 分析层                                   │
│  - 实时日志检索                                                        │
│  - 统计分析仪表板                                                      │
│  - 可视化图表                                                          │
│  - 告警规则配置                                                        │
└──────────────────────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────────────────┐
│                         告警通知层                                     │
│  - 钉钉机器人                                                          │
│  - 飞书机器人                                                          │
│  - 邮件通知                                                            │
└──────────────────────────────────────────────────────────────────────┘
```

### 2.2 数据流

```
应用日志 → Filebeat → Logstash → Elasticsearch → Kibana
                  │                      │
                  └──→ Kafka (可选) ─────┘
```

### 2.3 组件职责

| 组件 | 职责 | 关键配置 |
|------|------|----------|
| Filebeat | 日志采集 | worker=4, bulk_max_size=2048 |
| Logstash | 日志处理 | pipeline.workers=4 |
| Elasticsearch | 日志存储 | shards=2, replicas=1 |
| Kibana | 可视化 | 中文界面 |
| Curator | 日志清理 | 每天00:00执行 |

---

## 三、日志采集配置

### 3.1 Filebeat 配置

**配置文件**: `filebeat/filebeat-enhanced.yml`

#### 3.1.1 输入配置

```yaml
filebeat.inputs:
  # 应用日志
  - type: log
    id: ai-ready-application
    enabled: true
    paths:
      - /var/log/ai-ready/application.log
    json:
      keys_under_root: true
      add_error_key: true
    fields:
      project: ai-ready
      log_type: application
    fields_under_root: true
```

#### 3.1.2 性能配置

| 配置项 | 值 | 说明 |
|--------|------|------|
| worker | 4 | 并发工作线程 |
| bulk_max_size | 2048 | 批量发送大小 |
| queue.mem.events | 4096 | 内存队列大小 |
| flush.timeout | 1s | 刷新超时 |

#### 3.1.3 处理器配置

```yaml
processors:
  # 添加主机元数据
  - add_host_metadata: ~
  
  # 添加Docker元数据
  - add_docker_metadata: ~
  
  # 过滤敏感信息
  - script:
      lang: javascript
      source: >
        function process(event) {
          var msg = event.Get("message");
          msg = msg.replace(/(password|token|api_key)[=:]\s*[^\s]+/gi, "$1=***REDACTED***");
          event.Put("message", msg);
        }
```

### 3.2 Logstash 配置

**配置文件**: `logstash/pipeline/ai-ready-enhanced.conf`

#### 3.2.1 输入配置

```ruby
input {
  beats {
    port => 5044
    tags => ["filebeat"]
  }
  
  tcp {
    port => 5000
    codec => json_lines
  }
}
```

#### 3.2.2 过滤器配置

```ruby
filter {
  # JSON解析
  json {
    source => "message"
    target => "parsed"
  }
  
  # 日志级别提取
  if [parsed][level] {
    mutate { rename => { "[parsed][level]" => "log_level" } }
  }
  
  # 日期解析
  date {
    match => ["timestamp", "ISO8601"]
    target => "@timestamp"
  }
  
  # 地理位置解析
  geoip {
    source => "client_ip"
    target => "geoip"
  }
}
```

---

## 四、日志存储配置

### 4.1 索引模板

**配置文件**: `elasticsearch/index-template.json`

| 字段类型 | 字段名 | 类型 |
|----------|--------|------|
| 时间 | @timestamp | date |
| 文本 | message | text + keyword |
| 关键字 | log_level | keyword |
| 关键字 | service | keyword |
| IP | client_ip | ip |
| 地理 | geoip.location | geo_point |
| 数值 | response_time | float |
| 数值 | http_status | integer |

### 4.2 索引生命周期管理

**配置文件**: `elasticsearch/index-lifecycle-policy-enhanced.json`

| 阶段 | 时间 | 操作 | 存储 |
|------|------|------|------|
| Hot | 0-7天 | 读写、rollover | SSD |
| Warm | 7-30天 | 只读、shrink、forcemerge | HDD |
| Cold | 30-90天 | freeze | 对象存储 |
| Delete | 90天后 | 删除 | - |

### 4.3 存储容量估算

| 日志类型 | 日均量 | 热存储 | 温存储 | 冷存储 | 总存储 |
|----------|--------|--------|--------|--------|--------|
| 应用日志 | 5GB | 35GB | 115GB | 300GB | 450GB |
| 错误日志 | 0.5GB | 3.5GB | 11.5GB | 30GB | 45GB |
| 访问日志 | 10GB | 70GB | 230GB | - | 300GB |
| 审计日志 | 1GB | 7GB | 23GB | 335GB | 365GB |
| **总计** | **16.5GB** | **115.5GB** | **379.5GB** | **665GB** | **1160GB** |

---

## 五、日志分析配置

### 5.1 Kibana 仪表板

**配置文件**: `kibana/dashboards/ai-ready-dashboard.ndjson`

#### 5.1.1 总览仪表板

| 面板 | 类型 | 说明 |
|------|------|------|
| 日志量趋势 | 面积图 | 24小时日志量变化 |
| 日志级别分布 | 饼图 | ERROR/WARN/INFO占比 |
| 错误日志计数 | 指标 | 实时错误数量 |
| 服务日志分布 | 柱状图 | 各服务日志量 |
| 慢请求分析 | 表格 | 响应时间>1秒的请求 |
| 异常类型分析 | 表格 | Top 20异常类型 |

#### 5.1.2 常用查询

```bash
# 错误日志
log_level: "ERROR"

# 按追踪ID查询
trace_id: "abc123"

# 按服务查询
service: "core-api" AND log_level: "ERROR"

# 时间范围查询
@timestamp: [now-1h TO now]

# HTTP 5xx错误
http_status: [500 TO 599]

# 慢请求
response_time: >1000
```

### 5.2 日志检索 API

```bash
# 搜索错误日志
curl -X GET "http://localhost:9200/ai-ready-*/_search" -H 'Content-Type: application/json' -d'
{
  "query": {
    "bool": {
      "must": [
        { "match": { "log_level": "ERROR" } },
        { "range": { "@timestamp": { "gte": "now-1h" } } }
      ]
    }
  },
  "sort": [{ "@timestamp": "desc" }],
  "size": 100
}'

# 聚合统计
curl -X GET "http://localhost:9200/ai-ready-*/_search" -H 'Content-Type: application/json' -d'
{
  "size": 0,
  "aggs": {
    "by_service": {
      "terms": { "field": "service.keyword", "size": 10 }
    },
    "by_level": {
      "terms": { "field": "log_level.keyword" }
    }
  }
}'
```

---

## 六、告警规则配置

### 6.1 告警规则列表

**配置目录**: `elasticsearch/watcher/`

| 规则名称 | 条件 | 告警级别 | 通知方式 |
|----------|------|----------|----------|
| error-log-alert | 5分钟内ERROR>10条 | P1 | 钉钉+飞书 |
| http-5xx-alert | 5分钟内5xx>50次 | P1 | 钉钉 |
| slow-request-alert | 5分钟内>5秒请求>10次 | P2 | 钉钉 |
| exception-alert | 出现SQLException | P0 | 电话+飞书 |

### 6.2 告警规则示例

```json
{
  "trigger": { "schedule": { "interval": "1m" } },
  "input": {
    "search": {
      "request": {
        "indices": ["ai-ready-error-*"],
        "body": {
          "query": {
            "bool": {
              "must": [
                { "term": { "log_level": "ERROR" } },
                { "range": { "@timestamp": { "gte": "now-5m" } } }
              ]
            }
          }
        }
      }
    }
  },
  "condition": {
    "compare": { "ctx.payload.hits.total": { "gt": 10 } }
  },
  "actions": {
    "dingtalk_webhook": {
      "webhook": {
        "scheme": "https",
        "host": "oapi.dingtalk.com",
        "port": 443,
        "method": "post",
        "path": "/robot/send?access_token=xxx",
        "body": "{\"msgtype\":\"text\",\"text\":{\"content\":\"错误日志告警\"}}"
      }
    }
  }
}
```

### 6.3 告警阈值配置

| 指标 | 阈值 | 检测周期 |
|------|------|----------|
| ERROR日志数 | >10条 | 5分钟 |
| WARN日志数 | >50条 | 5分钟 |
| HTTP 5xx数 | >50次 | 5分钟 |
| 慢请求数 | >10次 | 5分钟 |
| SQL慢查询 | >5次 | 5分钟 |
| GC暂停时间 | >1秒 | 单次 |

---

## 七、部署指南

### 7.1 前置条件

- Docker 20.10+
- Docker Compose 2.0+
- 内存: 最低4GB，推荐8GB
- 磁盘: 最低100GB，推荐500GB

### 7.2 快速部署

```bash
# 1. 进入配置目录
cd I:\AI-Ready\logging

# 2. 启动服务
docker-compose up -d

# 3. 检查服务状态
docker-compose ps

# 4. 验证服务
curl http://localhost:9200/_cluster/health
curl http://localhost:5601/api/status
```

### 7.3 初始化配置

```bash
# 1. 创建ILM策略
curl -X PUT "http://localhost:9200/_ilm/policy/ai-ready-policy" \
  -H 'Content-Type: application/json' \
  -d @elasticsearch/index-lifecycle-policy-enhanced.json

# 2. 创建索引模板
curl -X PUT "http://localhost:9200/_index_template/ai-ready-template" \
  -H 'Content-Type: application/json' \
  -d @elasticsearch/index-template.json

# 3. 导入Kibana仪表板
curl -X POST "http://localhost:5601/api/saved_objects/_import" \
  -H "kbn-xsrf: true" \
  --form file=@kibana/dashboards/ai-ready-dashboard.ndjson

# 4. 创建告警规则
curl -X PUT "http://localhost:9200/_watcher/watch/error-log-alert" \
  -H 'Content-Type: application/json' \
  -d @elasticsearch/watcher/error-log-alert.json
```

### 7.4 配置验证

```bash
# 验证Filebeat配置
docker exec ai-ready-filebeat filebeat test config
docker exec ai-ready-filebeat filebeat test output

# 验证Logstash配置
docker exec ai-ready-logstash logstash --config.test_and_exit

# 验证Elasticsearch状态
curl http://localhost:9200/_cat/indices?v
curl http://localhost:9200/_cat/health?v
```

---

## 八、运维手册

### 8.1 日常运维

#### 8.1.1 服务管理

```bash
# 查看服务状态
docker-compose ps

# 查看服务日志
docker-compose logs -f elasticsearch
docker-compose logs -f logstash
docker-compose logs -f filebeat

# 重启服务
docker-compose restart logstash

# 停止服务
docker-compose down

# 启动服务
docker-compose up -d
```

#### 8.1.2 索引管理

```bash
# 查看索引列表
curl "http://localhost:9200/_cat/indices?v&s=store.size:desc"

# 查看索引设置
curl "http://localhost:9200/ai-ready-*/_settings"

# 手动滚动索引
curl -X POST "http://localhost:9200/ai-ready-application-000001/_rollover"

# 强制合并索引
curl -X POST "http://localhost:9200/ai-ready-*/_forcemerge?max_num_segments=1"
```

#### 8.1.3 存储监控

```bash
# 查看磁盘使用
curl "http://localhost:9200/_cat/allocation?v"

# 查看节点状态
curl "http://localhost:9200/_cat/nodes?v"

# 查看索引大小
curl "http://localhost:9200/_cat/indices?v&s=store.size:desc&bytes=gb"
```

### 8.2 故障排查

#### 8.2.1 日志未采集

```bash
# 1. 检查Filebeat状态
docker logs ai-ready-filebeat

# 2. 检查日志文件权限
ls -la /var/log/ai-ready/

# 3. 检查Filebeat配置
docker exec ai-ready-filebeat filebeat test config

# 4. 检查输出连接
docker exec ai-ready-filebeat filebeat test output
```

#### 8.2.2 日志延迟

```bash
# 1. 检查Logstash队列
curl "http://localhost:9600/_node/stats/pipelines?pretty"

# 2. 检查Elasticsearch写入速率
curl "http://localhost:9200/_cat/indices?v&s=pri.store.size:desc"

# 3. 增加处理线程
# 修改docker-compose.yml中LS_JAVA_OPTS
```

#### 8.2.3 存储空间不足

```bash
# 1. 查看索引占用
curl "http://localhost:9200/_cat/indices?v&s=store.size:desc"

# 2. 手动删除旧索引
curl -X DELETE "http://localhost:9200/ai-ready-application-2026.03.*"

# 3. 调整ILM策略
# 缩短delete阶段的min_age
```

### 8.3 性能调优

#### 8.3.1 Filebeat调优

| 参数 | 默认值 | 调优值 | 说明 |
|------|--------|--------|------|
| worker | 1 | 4 | 提升并发 |
| bulk_max_size | 1024 | 2048 | 批量发送 |
| queue.mem.events | 2048 | 4096 | 队列容量 |

#### 8.3.2 Logstash调优

| 参数 | 默认值 | 调优值 | 说明 |
|------|--------|--------|------|
| pipeline.workers | 2 | 4 | 处理线程 |
| pipeline.batch.size | 125 | 1000 | 批量大小 |
| pipeline.batch.delay | 50ms | 100ms | 批量延迟 |

#### 8.3.3 Elasticsearch调优

| 参数 | 默认值 | 调优值 | 说明 |
|------|--------|--------|------|
| number_of_shards | 5 | 2 | 减少分片 |
| number_of_replicas | 1 | 1 | 保持高可用 |
| refresh_interval | 1s | 5s | 降低刷新频率 |

---

## 附录

### A. 配置文件清单

```
I:\AI-Ready\logging\
├── docker-compose.yml                    # Docker Compose配置
├── filebeat\
│   ├── filebeat.yml                      # 基础配置
│   └── filebeat-enhanced.yml             # 增强配置
├── logstash\
│   └── pipeline\
│       ├── ai-ready.conf                 # 基础Pipeline
│       └── ai-ready-enhanced.conf        # 增强Pipeline
├── elasticsearch\
│   ├── index-template.json               # 索引模板
│   ├── index-lifecycle-policy.json       # ILM策略
│   ├── index-lifecycle-policy-enhanced.json
│   └── watcher\
│       ├── error-log-alert.json          # 错误日志告警
│       ├── http-5xx-alert.json           # HTTP 5xx告警
│       └── slow-request-alert.json       # 慢请求告警
├── kibana\
│   └── dashboards\
│       └── ai-ready-dashboard.ndjson     # Kibana仪表板
├── curator\
│   ├── curator.yml                       # Curator配置
│   └── actions.yml                       # 清理动作
├── logback-spring.xml                    # Spring Boot日志配置
└── AI-READY_LOG_COLLECTION_GUIDE.md      # 本文档
```

### B. 端口说明

| 端口 | 服务 | 说明 |
|------|------|------|
| 9200 | Elasticsearch | HTTP API |
| 9300 | Elasticsearch | 节点通信 |
| 5601 | Kibana | Web界面 |
| 5044 | Logstash | Beats输入 |
| 5000 | Logstash | TCP输入 |
| 5045 | Logstash | HTTP输入 |
| 5066 | Filebeat | 监控端点 |

### C. 参考文档

- [Elasticsearch官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/7.17/index.html)
- [Logstash配置指南](https://www.elastic.co/guide/en/logstash/7.17/configuration.html)
- [Filebeat配置指南](https://www.elastic.co/guide/en/beats/filebeat/7.17/configuring-howto-filebeat.html)
- [Kibana用户指南](https://www.elastic.co/guide/en/kibana/7.17/index.html)

---

**文档版本**: v2.0.0  
**最后更新**: 2026-04-04  
**维护人**: devops-engineer  
**项目**: AI-Ready
