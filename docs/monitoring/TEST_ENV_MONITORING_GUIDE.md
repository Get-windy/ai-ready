# AI-Ready 测试环境监控告警系统配置文档

## 文档信息

- **项目**: AI-Ready 企业级ERP系统
- **版本**: v2.0.0
- **创建日期**: 2026-04-28
- **作者**: devops-engineer
- **适用环境**: 测试环境

---

## 📋 目录

1. [系统概述](#系统概述)
2. [监控架构](#监控架构)
3. [监控组件](#监控组件)
4. [告警规则](#告警规则)
5. [日志收集](#日志收集)
6. [部署步骤](#部署步骤)
7. [使用指南](#使用指南)
8. [故障排查](#故障排查)

---

## 系统概述

### 🎯 目标

为AI-Ready测试环境建立完整的监控告警体系，实现：

- ✅ 实时监控系统资源（CPU、内存、磁盘、网络）
- ✅ 实时监控应用服务状态和性能
- ✅ 实时监控数据库和中间件状态
- ✅ 实时监控容器运行状态
- ✅ 自动化告警通知（邮件、企业微信）
- ✅ 完整的日志收集和分析能力
- ✅ 可视化的监控大盘

### 📊 监控范围

| 监控类型 | 监控对象 | 数据采集方式 |
|---------|---------|------------|
| 系统资源 | CPU、内存、磁盘、网络 | Node Exporter |
| 容器状态 | Docker容器 | cAdvisor |
| 应用服务 | API Gateway、业务服务 | Prometheus JMX Exporter |
| 数据库 | PostgreSQL | PostgreSQL Exporter |
| 缓存 | Redis | Redis Exporter |
| 消息队列 | Kafka | Kafka Exporter |
| 日志 | 应用日志、容器日志 | Filebeat + ELK Stack |

---

## 监控架构

### 🔍 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      监控数据采集层                          │
├─────────────────────────────────────────────────────────────┤
│ Node Exporter    cAdvisor     Exporters     Filebeat       │
│ (系统资源)        (容器)      (DB/Redis)    (日志采集)      │
└──────────────────┬──────────────┬──────────────┬────────────┘
                   │              │              │
                   ▼              ▼              ▼
┌─────────────────────────────────────────────────────────────┐
│                      数据存储层                              │
├─────────────────────────────────────────────────────────────┤
│ Prometheus       Elasticsearch      (时序数据库 + 日志)     │
└──────────────────┬──────────────┬──────────────┬────────────┘
                   │              │              │
                   ▼              ▼              ▼
┌─────────────────────────────────────────────────────────────┐
│                      可视化展示层                            │
├─────────────────────────────────────────────────────────────┤
│ Grafana Dashboards   Kibana Dashboards                    │
└──────────────────┬──────────────┬──────────────┬────────────┘
                   │              │              │
                   ▼              ▼              ▼
┌─────────────────────────────────────────────────────────────┐
│                      告警通知层                              │
├─────────────────────────────────────────────────────────────┤
│ AlertManager → Email / WeCom / Webhook                     │
└─────────────────────────────────────────────────────────────┘
```

### 🔄 数据流

1. **监控数据流**: Exporter → Prometheus → Grafana → AlertManager
2. **日志数据流**: Filebeat → Logstash → Elasticsearch → Kibana

---

## 监控组件

### 📦 组件清单

| 组件 | 版本 | 端口 | 用途 |
|------|------|------|------|
| Prometheus | v2.45.0 | 9090 | 时序数据存储和查询 |
| Grafana | v10.2.0 | 3000 | 监控可视化仪表板 |
| AlertManager | v0.26.0 | 9093 | 告警管理和通知 |
| Node Exporter | v1.6.1 | 9100 | 系统资源数据采集 |
| cAdvisor | v0.47.2 | 8085 | 容器监控数据采集 |
| PostgreSQL Exporter | v0.12.1 | 9187 | PostgreSQL监控 |
| Redis Exporter | v1.55.0 | 9121 | Redis监控 |
| Kafka Exporter | v1.7.2 | 9308 | Kafka监控 |
| Elasticsearch | v7.10.2 | 9200 | 日志存储和检索 |
| Logstash | v7.10.2 | 9600 | 日志处理和转换 |
| Kibana | v7.10.2 | 5601 | 日志可视化分析 |
| Filebeat | v7.10.2 | - | 日志采集代理 |

### 📁 配置文件结构

```
backend/infrastructure/docker/
├── docker-compose-test.yml          # Docker Compose主配置
├── prometheus-test/
│   ├── prometheus.yml               # Prometheus配置
│   └── rules/
│       ├── ai-ready-test-rules.yml  # 基础告警规则
│       └── enhanced-alert-rules.yml # 增强告警规则
├── grafana-test/
│   ├── provisioning/
│   │   ├── datasources/
│   │   │   └ datasources.yml        # 数据源配置
│   │   └── dashboards/
│   │       └ dashboards.yml         # Dashboard配置
│   └── dashboards/
│       ├── ai-ready/
│       │   └ system-overview.json   # 系统总览Dashboard
│       └── system/
│       │   └ system-resources.json  # 系统资源Dashboard
├── alertmanager-test/
│   └── alertmanager.yml             # AlertManager配置
├── elasticsearch/
│   └── elasticsearch.yml            # Elasticsearch配置
├── logstash/
│   └── logstash.conf                # Logstash管道配置
├── kibana/
│   └── kibana.yml                   # Kibana配置
├── filebeat/
│   └── filebeat.yml                 # Filebeat采集配置
└── scripts/
    └── deploy-monitoring-alerting.sh # 部署脚本
```

---

## 告警规则

### 🚨 告警规则分类

#### 1. 服务可用性告警

| 告警名称 | 触发条件 | 严重程度 | 通知渠道 |
|---------|---------|---------|---------|
| APIGatewayDown | 服务停止响应 > 1分钟 | critical | WeCom + Email |
| InventoryServiceDown | 服务停止响应 > 1分钟 | critical | WeCom + Email |
| FinanceServiceDown | 服务停止响应 > 1分钟 | critical | WeCom + Email |
| AIServiceDown | 服务停止响应 > 1分钟 | critical | WeCom + Email |

#### 2. 系统资源告警

| 告警名称 | 触发条件 | 严重程度 | 处理建议 |
|---------|---------|---------|---------|
| HighCPUUsage | CPU > 80% > 5分钟 | warning | 检查应用性能 |
| CriticalCPUUsage | CPU > 95% > 2分钟 | critical | 立即扩容或重启 |
| HighMemoryUsage | 内存 > 85% > 5分钟 | warning | 检查内存泄漏 |
| CriticalMemoryUsage | 内存 > 95% > 2分钟 | critical | 立即扩容或重启 |
| HighDiskUsage | 磁盘 > 85% > 5分钟 | warning | 清理磁盘空间 |
| CriticalDiskUsage | 磁盘 > 95% > 2分钟 | critical | 立即清理磁盘 |

#### 3. 性能告警

| 告警名称 | 触发条件 | 严重程度 | 处理建议 |
|---------|---------|---------|---------|
| HighErrorRate | 错误率 > 5% > 5分钟 | warning | 检查应用日志 |
| HighResponseTime | 响应时间 > 2s > 5分钟 | warning | 性能优化 |
| HighJVMMemoryUsage | JVM内存 > 85% > 5分钟 | warning | JVM调优 |

#### 4. 数据库告警

| 告警名称 | 触发条件 | 严重程度 | 处理建议 |
|---------|---------|---------|---------|
| HighPostgresConnections | 连接数 > 100 > 5分钟 | warning | 连接池优化 |
| PostgresConnectionPoolExhausted | 连接池使用率 > 90% > 5分钟 | critical | 扩容连接池 |
| RedisHighMemory | Redis内存 > 85% > 5分钟 | warning | 内存淘汰策略 |
| KafkaMessageLag | 消息堆积 > 10000 > 5分钟 | warning | 消费者优化 |

#### 5. 容器告警

| 告警名称 | 触发条件 | 严重程度 | 处理建议 |
|---------|---------|---------|---------|
| ContainerHighCPU | 容器CPU > 80% > 5分钟 | warning | 容器扩容 |
| ContainerHighMemory | 容器内存 > 85% > 5分钟 | warning | 容器扩容 |
| ContainerRestartingFrequently | 重启次数 > 5次/小时 | warning | 应用稳定性检查 |

### 🔔 告警通知配置

#### 企业微信通知

1. 获取企业微信机器人Webhook URL
2. 设置环境变量:
   ```bash
   export WECOM_WEBHOOK_URL='https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=YOUR_KEY'
   ```

3. 部署脚本会自动配置

#### 邮件通知

- SMTP服务器: localhost:587
- 发送者: alert@ai-ready.com
- 接收者: admin@ai-ready.com

---

## 日志收集

### 📝 日志采集配置

#### 采集对象

- 应用日志: `/var/log/{service}/` 目录下的日志文件
- 容器日志: `/var/lib/docker/containers/` 目录下的Docker日志
- 系统日志: 容器标准输出

#### 日志处理

- Logstash解析JSON日志字段
- 提取服务名称、日志级别、跟踪ID等
- 按日志级别分类存储

#### 日志存储

- Elasticsearch索引: `ai-ready-logs-{YYYY.MM.dd}`
- 错误日志专用索引: `ai-ready-errors-{YYYY.MM.dd}`
- 索引生命周期: 30天自动删除

---

## 部署步骤

### 🚀 快速部署

#### 1. 准备配置文件

```bash
cd I:\AI-Ready\backend\infrastructure\docker\scripts
```

#### 2. 设置企业微信通知（可选）

```bash
export WECOM_WEBHOOK_URL='https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=YOUR_KEY'
```

#### 3. 执行部署脚本

```bash
chmod +x deploy-monitoring-alerting.sh
./deploy-monitoring-alerting.sh
```

#### 4. 查看部署结果

```bash
docker-compose -f docker-compose-test.yml ps
```

### ⚙️ 分步部署

#### 步骤1: 启动监控系统

```bash
docker-compose -f docker-compose-test.yml up -d prometheus node-exporter cadvisor
docker-compose -f docker-compose-test.yml up -d postgres-exporter redis-exporter kafka-exporter
docker-compose -f docker-compose-test.yml up -d grafana alertmanager
```

#### 步骤2: 启动日志系统

```bash
docker-compose -f docker-compose-test.yml up -d elasticsearch
sleep 30
docker-compose -f docker-compose-test.yml up -d logstash kibana
docker-compose -f docker-compose-test.yml up -d filebeat
```

#### 步骤3: 验证部署

```bash
curl http://localhost:9090/-/healthy
curl http://localhost:3000/api/health
curl http://localhost:9093/-/healthy
curl http://localhost:9200/_cluster/health
curl http://localhost:5601/api/status
```

---

## 使用指南

### 📊 Grafana使用

#### 访问Grafana

1. 打开浏览器访问: http://localhost:3000
2. 登录账号: admin
3. 登录密码: admin_test_2026

#### 查看Dashboard

- **系统总览**: AI-Ready → 系统总览
- **系统资源**: System → 系统资源监控
- **服务性能**: AI-Ready → 服务性能监控
- **数据库监控**: AI-Ready → 数据库监控

#### 创建自定义Dashboard

1. 点击 "+" → "New Dashboard"
2. 选择数据源: Prometheus
3. 添加查询语句
4. 配置可视化类型
5. 保存Dashboard

### 📝 Kibana使用

#### 访问Kibana

1. 打开浏览器访问: http://localhost:5601
2. 首次访问会自动创建索引模式

#### 查看日志

1. 点击 "Discover"
2. 选择索引模式: `ai-ready-logs-*`
3. 添加过滤条件:
   - 搜索特定服务日志: `service: "api-gateway"`
   - 搜索错误日志: `log.level: "ERROR"`
   - 搜索特定跟踪ID: `trace.id: "YOUR_TRACE_ID"`

#### 创建Dashboard

1. 点击 "Dashboard" → "Create new dashboard"
2. 添加可视化组件
3. 配置搜索查询
4. 保存Dashboard

### 🔔 告警管理

#### 查看告警

1. 打开Prometheus: http://localhost:9090/alerts
2. 查看当前告警状态

#### 配置告警抑制

在AlertManager配置中可以设置抑制规则，防止告警风暴。

---

## 故障排查

### 🔍 常见问题

#### 1. Prometheus无法启动

**症状**: Prometheus容器启动失败

**排查步骤**:
```bash
docker logs ai-ready-prometheus-test
```

**可能原因**:
- 配置文件路径错误
- 告警规则语法错误
- 端口冲突

**解决方案**:
```bash
# 检查配置文件
docker-compose -f docker-compose-test.yml config

# 验证告警规则
promtool check rules prometheus-test/rules/*.yml
```

#### 2. Grafana无法连接数据源

**症状**: Grafana Dashboard显示"No data"

**排查步骤**:
```bash
curl http://localhost:9090/api/v1/query?query=up
```

**可能原因**:
- Prometheus未启动
- 数据源配置错误
- 网络连接问题

**解决方案**:
```bash
# 重启Prometheus
docker-compose -f docker-compose-test.yml restart prometheus

# 检查数据源配置
curl http://localhost:3000/api/datasources
```

#### 3. Elasticsearch启动失败

**症状**: Elasticsearch容器启动失败

**排查步骤**:
```bash
docker logs ai-ready-elasticsearch-test
```

**可能原因**:
- 内存不足
- 端口冲突
- 磁盘空间不足

**解决方案**:
```bash
# 检查内存使用
docker stats

# 清理磁盘空间
docker system prune -a
```

#### 4. 告警通知未发送

**症状**: 告警触发但未收到通知

**排查步骤**:
```bash
curl http://localhost:9093/api/v2/alerts
```

**可能原因**:
- AlertManager配置错误
- 企业微信Webhook URL无效
- SMTP配置错误

**解决方案**:
```bash
# 测试企业微信Webhook
curl -X POST "${WECOM_WEBHOOK_URL}" \
  -H 'Content-Type: application/json' \
  -d '{"msgtype":"text","text":{"content":"测试告警"}}'

# 检查AlertManager配置
amtool check-config alertmanager-test/alertmanager.yml
```

### 📊 监控指标查询

#### 常用PromQL查询

```promql
# 查看CPU使用率
100 - (avg by(instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)

# 查看内存使用率
(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100

# 查看磁盘使用率
(1 - ((node_filesystem_avail_bytes{mountpoint="/",fstype!="tmpfs"} / node_filesystem_size_bytes{mountpoint="/",fstype!="tmpfs"}))) * 100

# 查看服务状态
up{job=~"api-gateway|inventory-service|finance-service|ai-service|data-service"}

# 查看HTTP请求速率
rate(http_server_requests_seconds_count[5m])

# 查看错误率
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))

# 查看JVM内存使用率
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}

# 查看容器CPU使用率
rate(container_cpu_usage_seconds_total{container_label!="",container_label!="POD"}[5m]) * 100

# 查看容器内存使用率
container_memory_usage_bytes{container_label!="",container_label!="POD"} / container_spec_memory_limit_bytes{container_label!="",container_label!="POD"} * 100
```

---

## 📚 参考资源

- [Prometheus官方文档](https://prometheus.io/docs/)
- [Grafana官方文档](https://grafana.com/docs/)
- [AlertManager官方文档](https://prometheus.io/docs/alerting/latest/alertmanager/)
- [Elastic Stack官方文档](https://www.elastic.co/guide/index.html)
- [Node Exporter GitHub](https://github.com/prometheus/node_exporter)
- [cAdvisor GitHub](https://github.com/google/cadvisor)

---

## 🔄 更新记录

- **2026-04-28 00:35**: 创建完整的监控告警系统配置文档 (devops-engineer)
- **2026-04-28 00:30**: 补充企业微信通知配置和ELK日志系统配置 (devops-engineer)
- **2026-04-28 00:25**: 创建基础监控架构和告警规则配置 (devops-engineer)

---

## ⚙️ 运维工程师签名

**文档创建**: devops-engineer  
**创建时间**: 2026-04-28 00:35  
**文档版本**: v2.0.0  
**文档状态**: 完成