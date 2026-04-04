# AI-Ready Log Collection System Documentation

> **项目**: 智企连·AI-Ready  
> **版本**: v1.0  
> **最后更新**: 2026-03-30  
> **维护者**: devops-engineer

---

## 目录

1. [概述](#概述)
2. [日志收集架构](#日志收集架构)
3. [配置文件](#配置文件)
4. [部署指南](#部署指南)
5. [使用说明](#使用说明)

---

## 概述

### 日志系统目标

- **全面采集**: 应用日志、系统日志、容器日志
- **集中存储**: Elasticsearch 集中式存储
- **实时分析**: Kibana 实时可视化
- **快速检索**: 结构化日志查询

### 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| Filebeat | 8.7.0 | 日志采集 |
| Logstash | 8.7.0 | 日志处理 |
| Elasticsearch | 8.7.0 | 日志存储 |
| Kibana | 8.7.0 | 日志可视化 |
| Log Viewer | latest | 简易日志查看 |

---

## 日志收集架构

```
┌─────────────────────────────────────────────────────────────────┐
│                    AI-Ready Log Collection Stack                │
└─────────────────────────────────────────────────────────────────┘

┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Services   │────▶│  Filebeat   │────▶│  Logstash   │────▶
│  (Logs)     │     │ (Collection│     │ (Processing│
└─────────────┘     └─────────────┘     └─────────────┘
      │                   │                   │
      ▼                   ▼                   ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ Application │     │  System     │     │  Container  │
│   Logs      │     │   Logs      │     │   Logs      │
└─────────────┘     └─────────────┘     └─────────────┘
                                  │
                                  ▼
                           ┌─────────────┐
                           │ ElasticSearch│
                           │  (Storage)   │
                           └─────────────┘
                                  │
                                  ▼
                           ┌─────────────┐
                           │   Kibana    │
                           │ (Dashboard) │
                           └─────────────┘
```

### 数据流

```
1. 应用产生日志 → /app/logs/*.log
2. Filebeat 采集 → Docker容器日志 + 应用日志 + 系统日志
3. Logstash 处理 → 解析JSON、提取字段、格式化
4. Elasticsearch 存储 → 索引 ai-ready-logs-YYYY.MM.DD
5. Kibana 可视化 → 仪表盘、搜索、分析
```

---

## 配置文件

### 配置文件清单

| 文件 | 位置 | 大小 | 说明 |
|------|------|------|------|
| application-logging.yml | configs/logging/ | 1KB | Spring Boot日志配置 |
| filebeat.yml | configs/logging/ | 3KB | Filebeat采集配置 |
| logstash.conf | configs/logging/ | 2KB | Logstash处理配置 |
| elasticsearch.yml | configs/logging/ | 3KB | Elasticsearch配置 |
| kibana.yml | configs/logging/ | 1KB | Kibana配置 |
| docker-compose-logging.yml | docker/logging/ | 3KB | ELK部署配置 |

### 核心配置

#### Filebeat 输入配置

```yaml
filebeat.inputs:
  - type: container
    enabled: true
    paths:
      - /var/lib/docker/containers/*/*.log
  
  - type: filestream
    enabled: true
    paths:
      - /app/logs/*.log
    fields:
      log_type: application
      service: ai-ready-api
```

#### Logstash 过滤配置

```conf
filter {
  json {
    source => "message"
    target => "parsed_json"
  }
  
  date {
    match => [ "timestamp", "yyyy-MM-dd HH:mm:ss.SSS" ]
    target => "@timestamp"
  }
  
  mutate {
    add_field => { "service" => "ai-ready-api" }
  }
}
```

#### Elasticsearch 索引模板

```json
{
  "index_patterns": ["ai-ready-logs-*"],
  "template": {
    "settings": {
      "number_of_shards": 3,
      "number_of_replicas": 1,
      "index.lifecycle.name": "ai-ready-logs-policy"
    }
  }
}
```

### 索引生命周期策略

```json
{
  "phases": {
    "hot": {
      "min_age": "0ms",
      "actions": {
        "rollover": {"max_size": "50GB", "max_age": "7d"}
      }
    },
    "warm": {
      "min_age": "7d",
      "actions": {"shrink": {"number_of_shards": 1}}
    },
    "cold": {
      "min_age": "30d",
      "actions": {"readonly": {}}
    },
    "delete": {
      "min_age": "90d",
      "actions": {"delete": {}}
    }
  }
}
```

---

## 部署指南

### Docker Compose 部署

```bash
# 启动日志系统
cd I:\AI-Ready\docker\logging
docker-compose -f docker-compose-logging.yml up -d

# 查看服务状态
docker-compose -f docker-compose-logging.yml ps

# 查看服务日志
docker-compose -f docker-compose-logging.yml logs -f
```

### 启动顺序

1. **Elasticsearch** - 首先启动
2. **Logstash** - 等待 Elasticsearch
3. **Kibana** - 等待 Elasticsearch
4. **Filebeat** - 最后启动

### 健康检查

```bash
# 检查 Elasticsearch
curl http://localhost:9200/_cluster/health

# 检查 Kibana
curl http://localhost:5601/api/status

# 检查 Logstash
curl http://localhost:5044
```

---

## 使用说明

### 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| Elasticsearch | http://localhost:9200 | 日志存储 |
| Logstash | http://localhost:5044 | 日志接收 |
| Kibana | http://localhost:5601 | 可视化界面 |
| Log Viewer | http://localhost:8000 | 简易查看器 |

### Kibana 使用

#### 1. 创建索引模式

```
Management → Stack Management → Kibana → Index Patterns
Index pattern: ai-ready-logs-*
Time field: @timestamp
```

#### 2. 查看日志

```
Discover → 选择索引模式 → 左侧选择服务/日志级别
搜索: service:"ai-ready-api" AND log_level:ERROR
```

#### 3. 分析日志

```
Dashboard → 选择仪表盘 → 查看实时指标
图表: API请求率、错误率、延迟分布
```

#### 4. 创建仪表盘

```
Dashboard → Create Dashboard → Add panel
选择维度: 时间、服务、日志级别、响应时间
```

### 查询语法

#### Kibana QL

```
service:ai-ready-api AND log_level:ERROR
response.status >= 500
response.time > 1000
trace_id: "abc123"
```

#### Elasticsearch Query

```json
{
  "query": {
    "bool": {
      "must": [
        {"term": {"service": "ai-ready-api"}},
        {"range": {"response.time": {"gt": 1000}}}
      ]
    }
  }
}
```

### 日志格式

#### Json格式

```json
{
  "timestamp": "2026-03-30 01:45:00.123",
  "thread": "http-nio-8080-exec-1",
  "level": "INFO",
  "class": "cn.aiedge.api.UserController",
  "message": "User login successful",
  "trace_id": "abc123def456",
  "request_id": "req-123456",
  "user_id": "user-789",
  "request": {
    "method": "POST",
    "path": "/api/auth/login"
  },
  "response": {
    "status": 200,
    "time": 150
  }
}
```

---

## 最佳实践

### 日志最佳实践

1. **结构化日志**: 使用JSON格式
2. **trace_id**: 每个请求唯一标识
3. **日志级别**: ERROR/WARN/INFO/DEBUG
4. **日志分级**: 开发DEBUG, 生产INFO
5. **日志保留**: 90天热温冷归档

### 性能优化

1. **Filebeat 批量处理**: batch.size=125
2. **Logstash Worker**: pipeline.workers=2
3. **Elasticsearch 索引**: 索引分片3, 副本1
4. **索引生命周期**: 7天热数据→30天冷数据→90天删除
5. **内存配置**: Logstash 512M, Elasticsearch 1G

### 监控告警

| 指标 | 阈值 | 告警级别 |
|------|------|----------|
| Error Rate | > 5% | Warning |
| Slow Requests | > 1s | Warning |
| Log Delay | > 5min | Critical |
| Disk Usage | > 80% | Critical |

---

## 故障排查

### 常见问题

#### 1. Logstash 无法连接 Elasticsearch

```
# 检查 Elasticsearch 状态
curl http://localhost:9200/_cluster/health

# 检查 Logstash 日志
docker logs ai-ready-logstash
```

#### 2. Kibana 无法启动

```
# 检查 Elasticsearch 地址配置
# 查看 Kibana 日志
docker logs ai-ready-kibana
```

#### 3. Filebeat 无法采集日志

```
# 检查日志文件路径
ls -la /app/logs/

# 检查 Filebeat 状态
docker logs ai-ready-filebeat
```

### 调试命令

```bash
# 查看所有服务日志
docker-compose -f docker-compose-logging.yml logs -f

# 重新启动服务
docker-compose -f docker-compose-logging.yml restart

# 停止服务
docker-compose -f docker-compose-logging.yml down

# 清理数据
docker-compose -f docker-compose-logging.yml down -v
```

---

## 扩展建议

### 未来扩展

1. **ELK Cluster**: 多节点集群部署
2. **Redis 缓冲**: 减少 Elasticsearch 压力
3. **Metrics 采集**: 敏捷指标集成
4. **Alerting**: Kibana 告警配置
5. **APM**: 分布式追踪集成

### 监控集成

- Prometheus 监控 ELK 组件
- AlertManager 告警日志延迟
- Grafana ELK 可视化

---

## 配置文件清单

| 文件 | 位置 | 尺寸 |
|------|------|------|
| application-logging.yml | configs/logging/ | 1KB |
| filebeat.yml | configs/logging/ | 3KB |
| logstash.conf | configs/logging/ | 2KB |
| elasticsearch.yml | configs/logging/ | 3KB |
| kibana.yml | configs/logging/ | 1KB |
| docker-compose-logging.yml | docker/logging/ | 3KB |
| log-analysis.json | configs/logging/kibana-dashboards/ | 3KB |
| api-logs.json | configs/logging/kibana-dashboards/ | 3KB |

---

*文档由 devops-engineer 自动生成和维护*
*最后更新: 2026-03-30*