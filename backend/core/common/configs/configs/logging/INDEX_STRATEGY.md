# AI-Ready 日志索引和存储策略

**版本**: v1.0  
**日期**: 2026-04-12  
**作者**: devops-engineer  

---

## 一、索引设计策略

### 1.1 索引命名规范

```
ai-ready-logs-{YYYY.MM.DD}
ai-ready-api-{YYYY.MM.DD}
ai-ready-error-{YYYY.MM}
ai-ready-audit-{YYYY.MM}
```

### 1.2 索引分类

| 索引类型 | 命名模式 | 保留周期 | 分片数 |
|----------|----------|----------|--------|
| 应用日志 | ai-ready-logs-* | 30天 | 3 |
| API日志 | ai-ready-api-* | 60天 | 3 |
| 错误日志 | ai-ready-error-* | 180天 | 1 |
| 审计日志 | ai-ready-audit-* | 365天 | 1 |

---

## 二、索引生命周期管理 (ILM)

### 2.1 策略配置

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
        "min_age": "3d",
        "actions": {
          "set_priority": { "priority": 50 },
          "shrink": { "number_of_shards": 1 },
          "forcemerge": { "max_num_segments": 1 },
          "allocate": {
            "require": { "data": "warm" }
          }
        }
      },
      "cold": {
        "min_age": "30d",
        "actions": {
          "set_priority": { "priority": 0 },
          "freeze": {},
          "allocate": {
            "require": { "data": "cold" }
          }
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

### 2.2 索引模板

```json
{
  "index_patterns": ["ai-ready-logs-*"],
  "template": {
    "settings": {
      "number_of_shards": 3,
      "number_of_replicas": 1,
      "index.lifecycle.name": "ai-ready-logs-policy",
      "index.lifecycle.rollover_alias": "ai-ready-logs",
      "index.codec": "best_compression",
      "index.refresh_interval": "30s",
      "index.translog.durability": "async",
      "index.mapping.total_fields.limit": 1000
    },
    "mappings": {
      "dynamic_templates": [
        {
          "strings_as_keywords": {
            "match_mapping_type": "string",
            "mapping": {
              "type": "keyword",
              "ignore_above": 256
            }
          }
        }
      ],
      "properties": {
        "@timestamp": { "type": "date" },
        "message": {
          "type": "text",
          "fields": {
            "keyword": { "type": "keyword", "ignore_above": 32766 }
          }
        },
        "log_level": { "type": "keyword" },
        "service": { "type": "keyword" },
        "environment": { "type": "keyword" },
        "trace_id": { "type": "keyword" },
        "request_id": { "type": "keyword" },
        "user_id": { "type": "keyword" },
        "request.method": { "type": "keyword" },
        "request.path": { "type": "keyword" },
        "response.status": { "type": "integer" },
        "response.time": { "type": "integer" },
        "host.name": { "type": "keyword" },
        "container.name": { "type": "keyword" },
        "kubernetes.pod": { "type": "keyword" }
      }
    }
  }
}
```

---

## 三、存储容量规划

### 3.1 容量估算

| 日志类型 | 日产量 | 平均大小 | 30天容量 | 90天容量 |
|----------|--------|----------|----------|----------|
| 应用日志 | 10GB/天 | 2KB/条 | 300GB | 900GB |
| API日志 | 5GB/天 | 1.5KB/条 | 150GB | 450GB |
| 错误日志 | 500MB/天 | 5KB/条 | 15GB | 45GB |
| 审计日志 | 200MB/天 | 1KB/条 | 6GB | 18GB |
| **总计** | **15.7GB/天** | - | **471GB** | **1.4TB** |

### 3.2 节点配置建议

| 环境 | 节点数 | 单节点配置 | 总容量 |
|------|--------|------------|--------|
| 开发 | 1 | 4C/8G/200G | 200GB |
| 测试 | 3 | 4C/8G/200G | 600GB |
| 生产 | 5 | 8C/16G/500G | 2.5TB |

---

## 四、性能优化

### 4.1 写入优化

```yaml
# elasticsearch.yml
# 批量写入设置
indices.memory.index_buffer_size: 20%
index.refresh_interval: 30s
index.translog.durability: async
index