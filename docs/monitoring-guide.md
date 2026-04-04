# AI-Ready 监控体系文档

**项目**: 智企连·AI-Ready  
**版本**: v1.0  
**日期**: 2026-04-03  
**负责人**: devops-engineer

---

## 一、监控架构

### 1.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        监控数据流                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐                  │
│  │ App      │    │ Node     │    │ DB       │                  │
│  │ Metrics  │    │ Exporter │    │ Exporter │                  │
│  └────┬─────┘    └────┬─────┘    └────┬─────┘                  │
│       │              │              │                          │
│       └──────────────┼──────────────┘                          │
│                      ▼                                          │
│              ┌───────────────┐                                  │
│              │  Prometheus   │ ◄── 告警规则                     │
│              │  (采集&存储)   │                                  │
│              └───────┬───────┘                                  │
│                      │                                          │
│         ┌────────────┼────────────┐                            │
│         ▼            ▼            ▼                            │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                       │
│  │ Grafana  │ │ Alertmgr │ │ LongTerm │                       │
│  │ (可视化) │ │ (告警)   │ │ (存储)   │                       │
│  └──────────┘ └──────────┘ └──────────┘                       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 组件说明

| 组件 | 版本 | 用途 | 端口 |
|------|------|------|------|
| Prometheus | 2.45+ | 指标采集存储 | 9090 |
| Grafana | 10.0+ | 可视化仪表盘 | 3000 |
| Alertmanager | 0.26+ | 告警路由通知 | 9093 |
| Node Exporter | 1.6+ | 系统指标采集 | 9100 |
| PostgreSQL Exporter | 0.12+ | 数据库指标 | 9187 |
| Redis Exporter | 1.55+ | Redis指标 | 9121 |

---

## 二、监控指标体系

### 2.1 应用层指标

| 指标名称 | PromQL | 说明 | 告警阈值 |
|----------|--------|------|----------|
| 在线实例数 | `count(up{job="ai-ready-api"}==1)` | 服务实例数量 | <1 |
| 请求QPS | `sum(rate(http_server_requests_seconds_count[5m]))` | 每秒请求数 | 异常低 |
| 错误率 | `sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(...)) * 100` | 5xx错误占比 | >5% |
| P95响应时间 | `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))` | 响应时间分位 | >2s |
| P99响应时间 | `histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))` | 响应时间分位 | >5s |

### 2.2 JVM层指标

| 指标名称 | PromQL | 说明 | 告警阈值 |
|----------|--------|------|----------|
| 堆内存使用率 | `jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100` | 堆内存占比 | >85% 警告, >95% 严重 |
| GC频率 | `rate(jvm_gc_pause_seconds_count[5m])` | GC次数/秒 | >10 |
| GC暂停时间 | `histogram_quantile(0.95, rate(jvm_gc_pause_seconds_bucket[5m]))` | P95 GC暂停 | >0.5s |
| 线程数 | `jvm_threads_current` | 当前线程数 | 监控 |

### 2.3 数据库层指标

| 指标名称 | PromQL | 说明 | 告警阈值 |
|----------|--------|------|----------|
| 连接池使用率 | `hikaricp_connections_active / hikaricp_connections_max * 100` | 活动连接占比 | >90% |
| 等待连接数 | `hikaricp_connections_pending` | 等待获取连接的请求 | >5 |
| 查询延迟 | `histogram_quantile(0.95, rate(spring_data_repository_invocations_seconds_bucket[5m]))` | P95查询时间 | >1s |

### 2.4 系统层指标

| 指标名称 | PromQL | 说明 | 告警阈值 |
|----------|--------|------|----------|
| CPU使用率 | `100 - avg(irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100` | CPU占用 | >80% 警告, >95% 严重 |
| 内存使用率 | `(1 - node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes) * 100` | 内存占用 | >85% |
| 磁盘使用率 | `(node_filesystem_size_bytes - node_filesystem_avail_bytes) / node_filesystem_size_bytes * 100` | 磁盘占用 | >85% 警告, >95% 严重 |
| 网络IO | `rate(node_network_receive_bytes_total[5m])` | 网络流量 | 监控 |

---

## 三、Grafana仪表盘

### 3.1 仪表盘列表

| 仪表盘 | 文件路径 | 用途 |
|--------|----------|------|
| AI-Ready Overview | `monitoring/grafana/dashboards/ai-ready-overview.json` | 应用总览 |
| JVM Details | (待创建) | JVM详细监控 |
| Database Details | (待创建) | 数据库详细监控 |
| Infrastructure | (待创建) | 基础设施监控 |

### 3.2 导入仪表盘

```bash
# 方式1: 通过API导入
curl -X POST http://grafana:3000/api/dashboards/db \
  -H "Authorization: Bearer ${GRAFANA_API_KEY}" \
  -H "Content-Type: application/json" \
  -d @monitoring/grafana/dashboards/ai-ready-overview.json

# 方式2: 通过UI导入
# 访问 Grafana -> Dashboards -> Import -> 上传JSON文件
```

### 3.3 变量说明

仪表盘支持以下变量动态筛选：

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `$instance` | 实例地址 | 10.0.0.1:8080 |
| `$environment` | 环境 | dev/staging/prod |

---

## 四、告警规则

### 4.1 告警级别定义

| 级别 | 触发条件 | 通知方式 | 响应时间 |
|------|----------|----------|----------|
| **Critical** | 服务不可用、资源耗尽 | 电话+钉钉+邮件 | 5分钟内 |
| **Warning** | 性能下降、资源紧张 | 钉钉+邮件 | 30分钟内 |
| **Info** | 信息提示 | 邮件 | 24小时内 |

### 4.2 核心告警规则

| 告警名称 | 级别 | 触发条件 | 持续时间 |
|----------|------|----------|----------|
| AIReadyServiceDown | Critical | 服务实例不可达 | 1分钟 |
| AIReadyHighErrorRate | Critical | 5xx错误率 > 5% | 5分钟 |
| AIReadyHeapMemoryCritical | Critical | 堆内存 > 95% | 2分钟 |
| AIReadyConnectionPoolExhausted | Critical | 连接池使用率 > 90% | 5分钟 |
| AIReadyCPUHigh | Warning | CPU > 80% | 5分钟 |
| AIReadySlowResponse | Warning | P95响应时间 > 2s | 5分钟 |

### 4.3 告警通知配置

```yaml
# 钉钉Webhook配置
receivers:
  - name: 'critical-alerts'
    webhook_configs:
      - url: 'http://dingtalk-webhook/critical'
        send_resolved: true
```

---

## 五、部署配置

### 5.1 Prometheus配置

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'ai-ready-api'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['ai-ready-api:8080']

  - job_name: 'node-exporter'
    static_configs:
      - targets: ['node-exporter:9100']

  - job_name: 'postgres-exporter'
    static_configs:
      - targets: ['postgres-exporter:9187']

  - job_name: 'redis-exporter'
    static_configs:
      - targets: ['redis-exporter:9121']

rule_files:
  - '/etc/prometheus/rules/*.yml'

alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']
```

### 5.2 Kubernetes部署

```yaml
# 监控组件已通过Helm部署
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install prometheus prometheus-community/kube-prometheus-stack \
  -n monitoring --create-namespace \
  -f monitoring/prometheus/values.yaml
```

---

## 六、运维手册

### 6.1 日常检查清单

- [ ] 检查Grafana仪表盘是否有异常指标
- [ ] 检查Alertmanager是否有未处理告警
- [ ] 检查Prometheus数据采集是否正常
- [ ] 检查存储空间使用情况

### 6.2 故障排查流程

1. **收到告警** → 登录Grafana查看详情
2. **定位问题** → 查看相关指标和日志
3. **分析根因** → 应用/数据库/系统层面
4. **采取行动** → 重启/扩容/修复配置
5. **验证恢复** → 确认指标恢复正常
6. **记录总结** → 更新运维文档

### 6.3 常见问题处理

| 问题 | 可能原因 | 处理方式 |
|------|----------|----------|
| CPU使用率高 | 请求量大/死循环 | 扩容/代码优化 |
| 内存使用率高 | 内存泄漏/堆不足 | 分析heap dump/调整JVM参数 |
| 响应时间慢 | 数据库慢查询/网络延迟 | SQL优化/网络排查 |
| 连接池耗尽 | 请求量突增/连接泄漏 | 扩大连接池/排查泄漏 |

---

## 七、文件清单

| 文件路径 | 说明 |
|----------|------|
| `monitoring/grafana/dashboards/ai-ready-overview.json` | Grafana仪表盘配置 |
| `monitoring/prometheus/rules/ai-ready-alerts.yml` | 告警规则 |
| `monitoring/alertmanager/config.yml` | 告警路由配置 |
| `docs/monitoring-guide.md` | 本文档 |

---

**文档生成**: devops-engineer  
**版本**: v1.0
