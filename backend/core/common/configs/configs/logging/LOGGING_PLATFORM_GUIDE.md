# AI-Ready 日志分析平台使用文档

**版本**: v1.0  
**日期**: 2026-04-12  
**作者**: devops-engineer  

---

## 一、平台架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        AI-Ready 日志分析平台                      │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐     │
│  │ Filebeat │──▶│ Logstash │──▶│Elasticsearch│──▶│  Kibana  │     │
│  │ (采集)   │   │ (处理)   │   │  (存储)    │   │ (可视化) │     │
│  └──────────┘   └──────────┘   └──────────┘   └──────────┘     │
│       │                              │               │         │
│       ▼                              ▼               ▼         │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Alertmanager (告警管理)                      │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 二、快速启动

### 2.1 启动ELK Stack

```bash
cd I:\AI-Ready\configs\logging

# 启动所有服务
docker-compose -f docker-compose-elk.yml up -d

# 查看服务状态
docker-compose -f docker-compose-elk.yml ps

# 查看日志
docker-compose -f docker-compose-elk.yml logs -f
```

### 2.2 访问服务

| 服务 | URL | 说明 |
|------|-----|------|
| Elasticsearch | http://localhost:9200 | 日志存储API |
| Kibana | http://localhost:5601 | 日志可视化界面 |
| Logstash | http://localhost:5044 | 日志接收端口 |
| Alertmanager | http://localhost:9093 | 告警管理界面 |

---

## 三、日志采集配置

### 3.1 Filebeat配置

配置文件: `filebeat.yml`

```yaml
filebeat.inputs:
  # Docker容器日志
  - type: container
    enabled: true
    paths:
      - /var/lib/docker/containers/*/*.log
    
  # 应用日志
  - type: filestream
    enabled: true
    paths:
      - /app/logs/*.log
    fields:
      log_type: application
      service: ai-ready-api
```

### 3.2 日志格式规范

推荐使用JSON格式日志，便于解析：

```json
{
  "timestamp": "2026-04-12T10:30:00.123Z",
  "level": "INFO",
  "service": "ai-ready-api",
  "trace_id": "abc123",
  "message": "用户登录成功",
  "extra": {
    "user_id": "12345",
    "ip": "192.168.1.1"
  }
}
```

---

## 四、日志索引策略

### 4.1 索引生命周期管理 (ILM)

```json
{
  "policy": {
    "phases": {
      "hot": {
        "min_age": "0ms",
        "actions": {
          "rollover": {
            "max_size": "50GB",
            "max_age": "7d"
          }
        }
      },
      "warm": { "min_age": "7d" },
      "cold": { "min_age": "30d" },
      "delete": { "min_age": "90d" }
    }
  }
}
```

### 4.2 索引命名规范

| 索引名称 | 说明 | 保留周期 |
|----------|------|----------|
| ai-ready-logs-YYYY.MM.DD | 应用日志 | 90天 |
| ai-ready-api-logs-* | API服务日志 | 60天 |
| ai-ready-error-logs-* | 错误日志 | 180天 |

---

## 五、告警规则配置

### 5.1 告警规则列表

| 规则名称 | 触发条件 | 级别 | 通知方式 |
|----------|----------|------|----------|
| high_error_rate | 5分钟内错误>10条 | Critical | 邮件+钉钉 |
| exception_spike | 异常数量增长>100% | Warning | 邮件 |
| slow_query | 慢查询>5条/5分钟 | Warning | 邮件 |
| login_failure_spike | 登录失败激增 | Critical | 邮件+钉钉 |

### 5.2 配置告警通知

编辑 `alertmanager.yml`：

```yaml
receivers:
  - name: 'critical-alerts'
    email_configs:
      - to: 'oncall@ai-ready.com'
    webhook_configs:
      - url: '${DINGTALK_WEBHOOK_URL}'
```

### 5.3 环境变量配置

```bash
# 创建 .env 文件
SMTP_USER=your-email@gmail.com
SMTP_PASS=your-app-password
DINGTALK_WEBHOOK_URL=https://oapi.dingtalk.com/robot/send?access_token=xxx
```

---

## 六、Kibana使用指南

### 6.1 创建索引模式

1. 打开 Kibana → Stack Management → Index Patterns
2. 点击 "Create index pattern"
3. 输入: `ai-ready-logs-*`
4. 选择时间字段: `@timestamp`

### 6.2 常用搜索查询

```
# 搜索ERROR级别日志
log_level:ERROR

# 搜索特定服务日志
service:ai-ready-api

# 搜索特定时间段
@timestamp:[now-1h TO now]

# 复合查询
log_level:ERROR AND service:ai-ready-api AND @timestamp:[now-1h TO now]

# 模糊搜索
message:*Exception*

# 正则搜索
message:/.*timeout.*/
```

### 6.3 导入仪表板

```bash
# 导入预置仪表板
curl -X POST http://localhost:5601/api/saved_objects/_import \
  -H "kbn-xsrf: true" \
  --form file=@kibana-dashboards/log-analysis.json
```

---

## 七、日常运维

### 7.1 查看索引状态

```bash
# 查看所有索引
curl http://localhost:9200/_cat/indices?v

# 查看索引大小
curl http://localhost:9200/_cat/indices/ai-ready-logs-*?v&s=store.size:desc

# 查看集群健康状态
curl http://localhost:9200/_cluster/health?pretty
```

### 7.2 清理旧数据

```bash
# 删除30天前的索引
curl -X DELETE "http://localhost:9200/ai-ready-logs-$(date -d '30 days ago' +%Y.%m.%d)"

# 手动执行ILM策略
curl -X POST "http://localhost:9200/ai-ready-logs-*/_ilm/retry"
```

### 7.3 备份与恢复

```bash
# 创建快照仓库
curl -X PUT "http://localhost:9200/_snapshot/backup" -H 'Content-Type: application/json' -d'
{
  "type": "fs",
  "settings": {
    "location": "/usr/share/elasticsearch/backup"
  }
}'

# 创建快照
curl -X PUT "http://localhost:9200/_snapshot/backup/snapshot_$(date +%Y%m%d)"

# 恢复快照
curl -X POST "http://localhost:9200/_snapshot/backup/snapshot_20260412/_restore"
```

---

## 八、故障排查

### 8.1 Elasticsearch启动失败

```bash
# 检查内存锁定
docker logs ai-ready-elasticsearch | grep -i memory

# 增加虚拟内存
sudo sysctl -w vm.max_map_count=262144
```

### 8.2 Filebeat无法连接

```bash
# 检查Filebeat配置
docker exec ai-ready-filebeat filebeat test config

# 检查输出连接
docker exec ai-ready-filebeat filebeat test output
```

### 8.3 Kibana无法访问

```bash
# 检查Kibana状态
curl http://localhost:5601/api/status

# 查看Kibana日志
docker logs ai-ready-kibana
```

---

## 九、性能优化

### 9.1 Elasticsearch优化

```yaml
# elasticsearch.yml
indices.memory.index_buffer_size: 20%
index.refresh_interval: 30s
index.translog.durability: async
```

### 9.2 Logstash优化

```yaml
# logstash.conf
pipeline.workers: 4
pipeline.batch.size: 250
pipeline.batch.delay: 50
```

### 9.3 Filebeat优化

```yaml
# filebeat.yml
filebeat.inputs:
  - type: filestream
    harvester_buffer_size: 16384
    max_bytes: 10485760
```

---

## 十、附录

### 10.1 端口清单

| 服务 | 端口 | 协议 | 说明 |
|------|------|------|------|
| Elasticsearch | 9200 | HTTP | REST API |
| Elasticsearch | 9300 | TCP | 节点通信 |
| Logstash | 5044 | Beats | Filebeat输入 |
| Logstash | 5000 | TCP/UDP | 通用日志输入 |
| Kibana | 5601 | HTTP | Web界面 |
| Alertmanager | 9093 | HTTP | 告警管理 |

### 10.2 资源需求

| 服务 | CPU | 内存 | 磁盘 |
|------|-----|------|------|
| Elasticsearch | 2核 | 4GB | 100GB+ |
| Logstash | 1核 | 2GB | 10GB |
| Kibana | 1核 | 1GB | 5GB |
| Filebeat | 0.5核 | 512MB | 1GB |

---

**文档版本**: v1.0  
**最后更新**: 2026-04-12
