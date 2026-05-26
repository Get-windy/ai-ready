# 测试环境监控告警基础配置检查清单

**文档版本**: v1.0
**创建日期**: 2026-04-27
**创建者**: qa-lead
**项目**: AI-Ready ERP System
**Sprint**: Sprint 27+1

---

## 1. Prometheus配置检查清单

### 1.1 基础配置检查

| 检查项 | 检查内容 | 检查方法 | 检查结果 | 备注 |
|-------|---------|---------|---------|------|
| ✅ | 配置文件存在 | 检查 prometheus.yml 文件 | 通过 | infra/docker/prometheus/prometheus.yml |
| ✅ | 全局抓取间隔设置 | 验证 scrape_interval 值 | 通过 | 30s |
| ✅ | 全局评估间隔设置 | 验证 evaluation_interval 值 | 通过 | 30s |
| ✅ | 抓取超时设置 | 验证 scrape_timeout 值 | 通过 | 25s |
| ✅ | 外部标签设置 | 验证 external_labels 配置 | 通过 | cluster: ai-ready-test |
| ❌ | 告警规则文件存在 | 检查 rules/*.yml 文件 | 未通过 | 目录为空 |
| ✅ | Alertmanager集成 | 验证 alerting 配置 | 通过 | alertmanager:9093 |

### 1.2 监控目标检查

| 检查项 | 服务名称 | 目标地址 | 检查结果 | 备注 |
|-------|---------|---------|---------|------|
| ✅ | prometheus | localhost:9090 | 通过 | 自身监控 |
| ✅ | user-service | user-service:8085 | 通过 | Spring Actuator |
| ✅ | order-service | order-service:8086 | 通过 | Spring Actuator |
| ✅ | inventory-service | inventory-service:8082 | 通过 | Spring Actuator |
| ✅ | crm-service | crm-service:8087 | 通过 | Spring Actuator |
| ✅ | erp-service | erp-service:8088 | 通过 | Spring Actuator |
| ⚠️ | postgres-main | postgres-exporter-main:9187 | 部分通过 | Exporter未部署 |
| ⚠️ | postgres-inventory | postgres-exporter-inventory:9187 | 部分通过 | Exporter未部署 |
| ⚠️ | redis-main | redis-exporter-main:9121 | 部分通过 | Exporter未部署 |
| ⚠️ | redis-inventory | redis-exporter-inventory:9121 | 部分通过 | Exporter未部署 |
| ⚠️ | kafka | kafka-exporter:9308 | 部分通过 | Exporter未部署 |
| ⚠️ | node | node-exporter:9100 | 部分通过 | Exporter未部署 |
| ✅ | grafana | grafana:3000 | 通过 | Grafana监控 |
| ✅ | alertmanager | alertmanager:9093 | 通过 | Alertmanager监控 |

---

## 2. Grafana配置检查清单

### 2.1 数据源配置检查

| 检查项 | 数据源名称 | 类型 | URL | 检查结果 |
|-------|-----------|------|-----|---------|
| ✅ | Prometheus | prometheus | http://prometheus:9090 | 通过 |
| ✅ | PostgreSQL-Main | postgres | postgres-main:5432 | 通过 |
| ✅ | PostgreSQL-Inventory | postgres | postgres-inventory:5432 | 通过 |
| ✅ | Redis-Main | redis-datasource | redis-main:6379 | 通过 |
| ✅ | Redis-Inventory | redis-datasource | redis-inventory:6379 | 通过 |
| ✅ | Kafka | kafka-datasource | kafka:9092 | 通过 |

### 2.2 Grafana服务配置检查

| 检查项 | 检查内容 | 检查方法 | 检查结果 |
|-------|---------|---------|---------|
| ✅ | Grafana版本 | 验证镜像版本 | 通过 (v10.2.0) |
| ✅ | 端口配置 | 验证端口映射 | 通过 (3000) |
| ✅ | 管理员密码 | 验证密码配置 | 通过 |
| ✅ | 插件安装 | 验证插件配置 | 通过 (redis-datasource) |
| ✅ | 健康检查 | 验证健康检查配置 | 通过 (/api/health) |
| ✅ | 资源限制 | 验证资源限制配置 | 通过 (0.125 CPU, 128M) |

---

## 3. Alertmanager配置检查清单

### 3.1 基础配置检查

| 检查项 | 检查内容 | 检查方法 | 检查结果 |
|-------|---------|---------|---------|
| ✅ | 配置文件存在 | 检查 alertmanager.yml 文件 | 通过 |
| ✅ | SMTP配置 | 验证邮件通知配置 | 通过 |
| ✅ | Slack配置 | 验证Slack通知配置 | 通过 |
| ❌ | 钉钉配置 | 检查钉钉Webhook配置 | 未通过 (未配置) |
| ❌ | 企业微信配置 | 检查企业微信配置 | 未通过 (未配置) |
| ❌ | 短信配置 | 检查短信通知配置 | 未通过 (未配置) |

### 3.2 路由配置检查

| 检查项 | 路由名称 | 分组等待 | 重复间隔 | 检查结果 |
|-------|---------|---------|---------|---------|
| ✅ | default-receiver | 30s | 4h | 通过 |
| ✅ | critical-alerts | 10s | 30m | 通过 |
| ✅ | warning-alerts | 1m | 2h | 通过 |
| ✅ | database-team | 30s | 1h | 通过 |
| ✅ | application-team | 30s | 1h | 通过 |
| ✅ | infrastructure-team | 30s | 1h | 通过 |

### 3.3 抑制规则检查

| 检查项 | 源告警 | 目标告警 | 检查结果 |
|-------|---------|---------|---------|
| ✅ | NodeDown | 所有服务告警 | 通过 |
| ✅ | DatabaseDown | 依赖服务告警 | 通过 |

---

## 4. Docker Compose配置检查清单

### 4.1 监控服务配置检查

| 检查项 | 服务名称 | 镜像版本 | 端口 | 健康检查 | 检查结果 |
|-------|---------|---------|------|---------|---------|
| ✅ | prometheus | v2.45.0 | 9090 | ✅ | 通过 |
| ✅ | grafana | v10.2.0 | 3000 | ✅ | 通过 |
| ✅ | alertmanager | v0.26.0 | 9093 | ✅ | 通过 |

### 4.2 Exporter服务配置检查

| 检查项 | Exporter名称 | 是否定义 | 检查结果 | 备注 |
|-------|-------------|---------|---------|------|
| ❌ | postgres-exporter-main | 未定义 | 未通过 | 需添加 |
| ❌ | postgres-exporter-inventory | 未定义 | 未通过 | 需添加 |
| ❌ | redis-exporter-main | 未定义 | 未通过 | 需添加 |
| ❌ | redis-exporter-inventory | 未定义 | 未通过 | 需添加 |
| ❌ | kafka-exporter | 未定义 | 未通过 | 需添加 |
| ❌ | node-exporter | 未定义 | 未通过 | 需添加 |

### 4.3 资源限制配置检查

| 检查项 | 服务名称 | CPU限制 | 内存限制 | 检查结果 |
|-------|---------|---------|---------|---------|
| ✅ | prometheus | 0.25 | 256M | 通过 |
| ✅ | grafana | 0.125 | 128M | 通过 |
| ✅ | alertmanager | 0.125 | 64M | 通过 |

---

## 5. 告警规则配置检查清单

### 5.1 告警规则文件检查

| 检查项 | 规则文件 | 文件状态 | 检查结果 | 备注 |
|-------|---------|---------|---------|------|
| ❌ | service-availability.yml | 不存在 | 未通过 | 需创建 |
| ❌ | system-resources.yml | 不存在 | 未通过 | 需创建 |
| ❌ | database.yml | 不存在 | 未通过 | 需创建 |
| ❌ | cache.yml | 不存在 | 未通过 | 需创建 |
| ❌ | application.yml | 不存在 | 未通过 | 需创建 |

### 5.2 建议创建的告警规则

#### 服务可用性告警规则

| 规则名称 | 表达式 | 持续时间 | 严重级别 | 状态 |
|---------|-------|---------|---------|------|
| ServiceDown | up == 0 | 1m | critical | 需创建 |
| ServiceHighErrorRate | rate(http_requests_total{status=~"5.."}[5m]) > 0.1 | 5m | warning | 需创建 |
| ServiceSlowResponse | histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 1 | 5m | warning | 需创建 |

#### 系统资源告警规则

| 规则名称 | 表达式 | 持续时间 | 严重级别 | 状态 |
|---------|-------|---------|---------|------|
| HighCPUUsage | 100 - avg(irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100 > 85 | 5m | warning | 需创建 |
| HighMemoryUsage | (1 - node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes) * 100 > 90 | 5m | warning | 需创建 |
| HighDiskUsage | (1 - node_filesystem_avail_bytes / node_filesystem_size_bytes) * 100 > 85 | 5m | warning | 需创建 |

---

## 6. 验证总结

### 6.1 检查项统计

| 类别 | 总项数 | 通过项 | 未通过项 | 通过率 |
|-----|-------|-------|---------|--------|
| Prometheus配置 | 7 | 6 | 1 | 86% |
| Prometheus监控目标 | 14 | 6 | 8 | 43% |
| Grafana数据源 | 6 | 6 | 0 | 100% |
| Grafana服务 | 6 | 6 | 0 | 100% |
| Alertmanager配置 | 6 | 2 | 4 | 33% |
| Alertmanager路由 | 6 | 6 | 0 | 100% |
| Docker Compose监控 | 3 | 3 | 0 | 100% |
| Docker Compose Exporter | 6 | 0 | 6 | 0% |
| 告警规则 | 5 | 0 | 5 | 0% |
| **总计** | **53** | **29** | **24** | **55%** |

### 6.2 问题优先级分类

| 优先级 | 问题数量 | 问题类型 |
|-------|---------|---------|
| 高 | 4 | 告警规则缺失、Exporter缺失、钉钉配置缺失 |
| 中 | 3 | Exporter服务配置 |
| 低 | 3 | 企业微信、短信通知配置 |

### 6.3 建议执行顺序

| 步骤 | 内容 | 预计时间 | 优先级 |
|-----|------|---------|-------|
| 1 | 创建告警规则文件 | 30分钟 | 高 |
| 2 | 添加钉钉Webhook配置 | 15分钟 | 高 |
| 3 | 添加Exporter服务配置 | 1小时 | 高 |
| 4 | 添加企业微信配置 | 30分钟 | 低 |

---

## 7. 检查签字

| 角色 | 姓名 | 签字 | 日期 |
|-----|------|------|------|
| 检查人员 | qa-lead | | 2026-04-27 |
| 审核人员 | coordinator | | 待审核 |

---

**文档状态**: 已完成
**审批状态**: 待审批