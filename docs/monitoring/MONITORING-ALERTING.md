# AI-Ready 监控告警系统文档

> **项目**: 智企连·AI-Ready  
> **版本**: v1.0  
> **最后更新**: 2026-03-30  
> **维护者**: devops-engineer

---

## 目录

1. [概述](#概述)
2. [监控架构](#监控架构)
3. [告警规则](#告警规则)
4. [告警路由](#告警路由)
5. [Grafana 仪表盘](#grafana-仪表盘)
6. [ exporter配置](#exporter-配置)
7. [最佳实践](#最佳实践)

---

## 概述

### 监控目标

- ✅ **全面覆盖**: 应用、系统、数据库、缓存、消息队列
- ✅ **AI 性能监控**: GPU、内存、推理时间、吞吐量
- ✅ **异常告警**: 实时告警，多通道通知
- ✅ **可视化**: Grafana 仪表盘，实时监控

### 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| Prometheus | v2.45.0 | 指标收集与存储 |
| AlertManager | v0.26.0 | 告警路由与通知 |
| Grafana | v9.5.0 | 可视化仪表盘 |
| Node Exporter | v1.6.1 | 系统指标 |
| PostgreSQL Exporter | v0.15.0 | 数据库指标 |
| Redis Exporter | v1.55.0 | 缓存指标 |
| RocketMQ Exporter | latest | 消息队列指标 |

---

## 监控架构

```
┌─────────────────────────────────────────────────────────────────┐
│                    AI-Ready Monitoring Stack                    │
└─────────────────────────────────────────────────────────────────┘

┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Services  │────▶│  Prometheus │────▶│ AlertManager│
│  (Metrics)  │     │   (SCrape)  │     │  (Routing)  │
└─────────────┘     └─────────────┘     └─────────────┘
      │                     │                   │
      ▼                     ▼                   ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Exporters  │     │   Rules     │     │  Receivers  │
│  (HTTP/TCP) │     │  (Alerts)   │     │  (WeCom/EMail)│
└─────────────┘     └─────────────┘     └─────────────┘
                                         │         │
                                         ▼         ▼
                                  ┌─────────┐  ┌─────────┐
                                  │ Grafana │  │ PagerDuty│
                                  │ Dashboard│  │  (Site) │
                                  └─────────┘  └─────────┘
```

### 监控指标分类

#### 系统指标 (System)
- CPU 使用率、内存使用率
- 磁盘使用率、网络流量
- 进程数、上下文切换

#### 应用指标 (Application)
- API 响应时间 (p95/p99)
- 请求率、错误率
- 线程池、数据库连接池

#### 持久化指标 (Persistence)
- PostgreSQL 连接数、复制延迟
- Redis 内存、连接数
- RocketMQ 队列积压

#### AI 性能指标 (AI)
- GPU 利用率、内存使用
- 推理时间、吞吐量
- 缓存命中率、队列长度

---

## 告警规则

### 系统告警 (system-alerts)

| 告警名称 | 指标 | 阈值 | 等待时间 | 级别 |
|---------|------|------|---------|------|
| HighCPUUsage | CPU > 80% | 5分钟 | warning |
| HighMemoryUsage | 内存 > 85% | 5分钟 | warning |
| HighDiskUsage | 磁盘 > 80% | 10分钟 | critical |
| HighNetworkTraffic | 网络 > 100MB/s | 5分钟 | warning |

### 应用告警 (application-alerts)

| 告警名称 | 指标 | 阈值 | 等待时间 | 级别 |
|---------|------|------|---------|------|
| HighAPILatency | P99 > 2s | 5分钟 | warning |
| HighAPIErrorRate | 错误率 > 5% | 5分钟 | critical |
| HighThreadPoolUsage | 线程池 > 500 | 5分钟 | warning |
| HighDatabaseConnectionUsage | DB连接 > 80% | 5分钟 | warning |

### AI 性能告警 (ai-performance-alerts)

| 告警名称 | 指标 | 阈值 | 等待时间 | 级别 |
|---------|------|------|---------|------|
| HighAIGPUUtilization | GPU > 90% | 10分钟 | warning |
| HighAI MemoryUtilization | GPU内存 > 85% | 10分钟 | warning |
| HighAI InferenceTime | 推理时间 > 5s | 5分钟 | warning |
| LowAI Throughput | 吞吐量 < 10req/s | 15分钟 | warning |
| HighAICacheMissRate | 缓存未命中 > 20% | 10分钟 | warning |

### 业务告警 (business-alerts)

| 告警名称 | 指标 | 阈值 | 等待时间 | 级别 |
|---------|------|------|---------|------|
| LowOrderSuccessRate | 订单成功率 < 99% | 5分钟 | critical |
| LowPaymentSuccessRate | 支付成功率 < 95% | 5分钟 | critical |
| LowAgentCallSuccessRate | Agent成功率 < 99% | 5分钟 | critical |

---

## 告警路由

### AlertManager 配置

```yaml
route:
  group_by: ['alertname', 'cluster', 'service']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'wehcom-default'
  
  routes:
    - match:
        severity: critical
      receiver: 'wehcom-critical'  # WeCom + Email
      continue: true
    - match:
        severity: warning
      receiver: 'wehcom-warning'   # WeCom
    
    - match:
        team: business
      receiver: 'wehcom-business'  # 业务团队
    
    - match:
        team: devops
      receiver: 'wehcom-devops'    # 运维团队
    
    - match:
        team: backend
      receiver: 'wehcom-backend'   # 后端团队
    
    - match:
        team: ai
      receiver: 'wehcom-ai'        # AI 团队
```

### 接收器配置

| 接收器 | 通知方式 | 受众 | 适用场景 |
|--------|---------|------|---------|
| wehcom-default | 企业微信 | 全员 | 默认通知 |
| wehcom-critical | 企业微信+邮件 | 运维/后端 | P0/P1告警 |
| wehcom-warning | 企业微信 | 相关团队 | P2/P3告警 |
| wehcom-business | 企业微信 | 业务团队 | 业务告警 |
| wehcom-devops | 企业微信 | 运维团队 | 系统告警 |
| wehcom-backend | 企业微信 | 后端团队 | 应用告警 |
| wehcom-ai | 企业微信 | AI团队 | AI性能告警 |
| email-critical | 邮件 | devops@, backend@ | P0紧急告警 |

### 通知策略

- ** group_wait: 10s** - 等待10秒收集同一组告警
- ** group_interval: 10s** - 每10秒发送一次聚合通知
- ** repeat_interval: 1h** - 重复通知间隔1小时

---

## Grafana 仪表盘

### 仪表盘清单

| 仪表盘 | UID | 用途 | 更新间隔 |
|-------|-----|------|---------|
| AI Model Performance Monitor | ai-model-performance | AI性能监控 | 30秒 |
| AI-Ready System Overview | ai-ready-system-overview | 系统总览 | 30秒 |

### AI Model Performance Monitor 仪表盘

包含面板:
1. **AI GPU Utilization** - GPU利用率 (Gauge)
2. **AI GPU Memory Usage** - GPU内存使用 (Gauge)
3. **AI Inference P95 Latency** - 推理P95延迟 (Stat)
4. **AI Requests/s** - 请求率 (Stat)
5. **AI Request Queue Length** - 请求队列长度 (Stat)
6. **AI Model Replicas** - 模型副本数 (Stat)
7. **AI Resource Usage** - 资源使用 (Timeseries)
8. **AI Request Rate** - 请求率 (Timeseries)
9. **AI Inference Latency Distribution** - 推理延迟分布 (Timeseries)
10. **AI Cache Miss Rate** - 缓存未命中率 (Timeseries)

### AI-Ready System Overview 仪表盘

包含面板:
1. **API Service Status** - API服务状态 (Stat)
2. **PostgreSQL Status** - PostgreSQL状态 (Stat)
3. **Redis Status** - Redis状态 (Stat)
4. **RocketMQ Status** - RocketMQ状态 (Stat)
5. **System Resource Usage** - 系统资源 (Timeseries)
6. **Redis Command Rate** - Redis命令率 (Timeseries)
7. **PostgreSQL Connections** - PostgreSQL连接数 (Timeseries)
8. **API Response Time** - API响应时间 (Timeseries)
9. **API Error Rate** - API错误率 (Timeseries)

---

## Exporter 配置

### Exporter 列表

| Exporter | 端口 | 指标 | 配置文件 |
|---------|------|------|---------|
| Postgres Exporter | 9187 | PostgreSQL指标 | docker-compose-exporters.yml |
| Redis Exporter | 9121 | Redis指标 | docker-compose-exporters.yml |
| Node Exporter | 9100 | 系统指标 | docker-compose-exporters.yml |
| RocketMQ Exporter | 50070 | RocketMQ指标 | docker-compose-exporters.yml |
| AI Metrics Exporter | 9200 | AI性能指标 | custom |

### Docker Compose exporters

```yaml
services:
  postgres_exporter:
    image: wrouesnel/postgres_exporter:v0.15.0
    ports: ["9187:9187"]
    environment:
      DATA_SOURCE_NAME: "postgresql://..."

  redis_exporter:
    image: oliver006/redis_exporter:v1.55.0
    ports: ["9121:9121"]
    environment:
      REDIS_ADDR: "redis://redis:6379"

  node_exporter:
    image: prom/node-exporter:v1.6.1
    ports: ["9100:9100"]
    volumes: ["/proc:/host/proc:ro", "/sys:/host/sys:ro", "/:/rootfs:ro"]
```

---

## 最佳实践

### 监控最佳实践

1. **分层监控**: 系统→应用→业务→AI性能
2. **多维度指标**: latency/error/throughput
3. **及时告警**: 阈值设置合理，避免告警风暴
4. **可视化**: Grafana仪表盘实时监控
5. **历史分析**: Prometheus备份与分析

### 告警最佳实践

1. **分级告警**: P0/P1/P2/P3不同优先级
2. **路由分发**: 按团队/级别路由到不同receiver
3. **抑制规则**: 避免重复告警
4. **时间窗口**: 设置group_wait/group_interval
5. **运行手册**: 每个告警关联runbook_url

### 性能优化建议

1. **GPU监控**: 关注利用率>90%的长时间运行
2. **缓存监控**: 缓存命中率<80%需要优化
3. **队列监控**: 队列长度>100需要扩容
4. **推理时间**: P99>3s需要优化模型
5. **副本数**: 监控HPA扩缩容行为

---

## 告警响应流程

### P0/P1 告警响应

```
1. 接收告警 (企业微信/邮件)
   │
   ▼
2. 确认告警等级 (5分钟内)
   │
   ▼
3. 查看运行手册 (runbook_url)
   │
   ▼
4. 执行应急响应
   │
   ▼
5. 通知相关方 (团队/负责人)
   │
   ▼
6. 问题解决后关闭告警
```

### 运行手册示例

| 告警 | 原因 | 解决方案 |
|------|------|----------|
| HighCPUUsage | 进程异常、资源不足 | 1. 查看top进程<br>2. 重启异常进程<br>3. 扩容 |
| HighAPILatency | DB慢查询、网络延迟 | 1. 查看慢查询日志<br>2. 优化SQL<br>3. 检查网络 |
| HighAIGPUUtilization | 模型推理压力大 | 1. 扩容GPU节点<br>2. 优化模型<br>3. 增加缓存 |

---

## 配置文件清单

| 文件 | 位置 | 大小 |
|------|------|------|
| prometheus.yml | configs/alerting/ | 2.8KB |
| rules.yml | configs/alerting/ | 15KB |
| alertmanager.yml | configs/alerting/ | 6KB |
| ai-performance-alerts.yml | configs/alerting/ | 9KB |
| docker-compose-exporters.yml | configs/exporters/ | 2KB |
| ai-performance.json | configs/alerting/dashboards/ | 13KB |
| ai-ready-overview.json | configs/alerting/dashboards/ | 10KB |
| MONITORING-ALERTING.md | docs/monitoring/ | 7KB |

---

## 常用查询

### Prometheus 查询

```sql
# API错误率
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m])) * 100

# API响应时间P99
histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))

# GPU利用率
ai_ready_gpu_utilization

# 缓存命中率
rate(ai_ready_cache_hits_total[5m]) / rate(ai_ready_cache_requests_total[5m]) * 100

# AI吞吐量
rate(ai_ready_model_requests_total[5m])
```

### Grafana 变量

```yaml
# 环境变量
env = dev|staging|prod

# 服务变量
service = ai-ready-api|ai-ready-agent|ai-ready-crm|ai-ready-erp
```

---

*文档由 devops-engineer 自动生成和维护*
*最后更新: 2026-03-30*