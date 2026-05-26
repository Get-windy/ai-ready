# Sprint 28 监控告警模块 Prometheus 监控配置

## 项目信息

- **项目名称**: AI-Ready 企智连ERP系统
- **模块**: Sprint 28 监控告警模块
- **版本**: v1.0
- **最后更新**: 2026-04-26
- **作者**: devops-engineer

## 目录结构

```
sprint28-alerting/
├── prometheus.yml                    # Prometheus 主配置文件
├── alertmanager.yml                  # AlertManager 告警管理配置
├── docker-compose.yml                # Docker Compose 编排配置
├── blackbox.yml                      # Blackbox Exporter 探测配置
├── grafana-datasources.yml           # Grafana 数据源配置
├── deploy.ps1                        # PowerShell 部署脚本
├── README.md                         # 本文档
├── rules/                            # 告警规则目录
│   ├── alerting-engine-rules.yml     # 告警规则引擎监控规则
│   ├── metrics-collection-rules.yml  # 指标采集监控规则
│   ├── notification-rules.yml        # 告警通知监控规则
│   └── database-rules.yml            # 数据库监控规则
└── grafana-dashboards/               # Grafana 面板配置
    ├── alerting-rule-engine-dashboard.json
    ├── metrics-collector-dashboard.json
    └── notification-service-dashboard.json
```

## 功能特性

### 1. Prometheus 监控指标配置

#### 告警规则引擎监控
- 服务健康状态监控
- 规则评估吞吐量（目标：≥1000规则/秒）
- 规则评估延迟（P50/P95/P99）
- 规则缓存命中率
- 规则评估错误率
- JVM 内存和 GC 监控

#### 指标采集服务监控
- 指标采集吞吐量（目标：≥10000指标/秒）
- 指标采集延迟
- 指标队列积压监控
- 指标写入数据库延迟
- 批量处理效率
- 指标压缩率

#### 告警通知服务监控
- 通知发送延迟（目标：P95 ≤ 3秒）
- 通知发送成功率
- 通知队列积压
- 各通道（企业微信/邮件/Webhook）健康状态
- 通知重试统计

#### 数据库监控
- PostgreSQL 连接数和性能
- Redis 内存和连接数
- RabbitMQ 队列积压
- 慢查询检测
- 缓存命中率

### 2. Grafana 监控面板

| 面板名称 | 功能描述 |
|---------|---------|
| 告警规则引擎监控 | 规则评估性能、缓存命中率、JVM状态 |
| 指标采集监控 | 采集吞吐量、队列状态、批量处理效率 |
| 告警通知监控 | 通知延迟、成功率、各通道分布 |

### 3. 告警规则

#### 告警等级划分

| 等级 | 名称 | 响应时间 | 通知方式 |
|------|------|----------|----------|
| Critical | 严重 | 立即 | 邮件 + 企业微信 |
| Warning | 警告 | 5分钟 | 企业微信 |

#### 关键告警指标

| 组件 | 告警项 | 阈值 | 等级 |
|------|--------|------|------|
| 规则引擎 | 服务宕机 | up == 0 | Critical |
| 规则引擎 | 吞吐量低 | < 1000规则/秒 | Warning |
| 规则引擎 | 错误率高 | > 1% | Critical |
| 指标采集 | 服务宕机 | up == 0 | Critical |
| 指标采集 | 队列积压 | > 10000 | Warning |
| 指标采集 | 错误率高 | > 0.1% | Critical |
| 通知服务 | 服务宕机 | up == 0 | Critical |
| 通知服务 | 延迟高 | P95 > 3秒 | Warning |
| 数据库 | 连接数高 | > 80 | Warning |
| 数据库 | 复制延迟 | > 10秒 | Critical |

## 快速开始

### 前置要求

- Docker Engine 20.10+
- Docker Compose 2.0+
- PowerShell 5.1+ (Windows) 或 Bash (Linux/Mac)

### 部署步骤

#### 1. 启动监控服务

```powershell
# Windows PowerShell
.\deploy.ps1 -Action start

# 或使用默认参数
.\deploy.ps1
```

#### 2. 查看服务状态

```powershell
.\deploy.ps1 -Action status
```

#### 3. 查看日志

```powershell
# 查看所有服务日志
.\deploy.ps1 -Action logs

# 查看特定服务日志
.\deploy.ps1 -Action logs -Service prometheus
```

#### 4. 停止服务

```powershell
.\deploy.ps1 -Action stop
```

#### 5. 重启服务

```powershell
.\deploy.ps1 -Action restart
```

### 访问地址

| 服务 | 地址 | 默认凭据 |
|------|------|----------|
| Prometheus | http://localhost:9090 | - |
| Grafana | http://localhost:3000 | admin / admin123 |
| AlertManager | http://localhost:9093 | - |

## 配置说明

### Prometheus 配置 (prometheus.yml)

主要配置项：
- `scrape_interval`: 指标采集间隔（默认15秒）
- `evaluation_interval`: 告警规则评估间隔（默认15秒）
- `retention.time`: 数据保留时间（30天）
- `retention.size`: 数据保留大小（50GB）

### 告警规则配置

告警规则位于 `rules/` 目录下，按组件分类：

1. **alerting-engine-rules.yml**: 告警规则引擎监控
2. **metrics-collection-rules.yml**: 指标采集监控
3. **notification-rules.yml**: 告警通知监控
4. **database-rules.yml**: 数据库监控

### AlertManager 配置

支持的通知渠道：
- 邮件通知
- 企业微信
- Webhook

告警路由策略：
- Critical 级别：立即通知，30分钟重复
- Warning 级别：延迟1分钟通知，2小时重复

## 监控指标清单

### 应用指标 (Application Metrics)

| 指标名称 | 类型 | 说明 |
|---------|------|------|
| alerting_rules_evaluated_total | Counter | 规则评估总数 |
| alerting_rules_evaluation_errors_total | Counter | 规则评估错误数 |
| alerting_rule_evaluation_duration_seconds | Histogram | 规则评估耗时 |
| alerting_rule_cache_hits_total | Counter | 规则缓存命中数 |
| alerting_rule_cache_misses_total | Counter | 规则缓存未命中数 |
| alerting_alerts_firing_total | Counter | 告警触发总数 |
| alerting_alerts_suppressed_total | Counter | 告警抑制总数 |
| alerting_metrics_collected_total | Counter | 指标采集总数 |
| alerting_metrics_collection_errors_total | Counter | 指标采集错误数 |
| alerting_metrics_queue_size | Gauge | 指标队列大小 |
| alerting_notification_sent_total | Counter | 通知发送总数 |
| alerting_notification_send_errors_total | Counter | 通知发送错误数 |
| alerting_notification_send_duration_seconds | Histogram | 通知发送耗时 |
| alerting_notification_queue_size | Gauge | 通知队列大小 |

### 系统指标 (System Metrics)

| 指标名称 | 来源 | 说明 |
|---------|------|------|
| up | Prometheus | 服务存活状态 |
| jvm_memory_used_bytes | JMX | JVM内存使用 |
| jvm_gc_pause_seconds | JMX | GC暂停时间 |
| spring_thread_pool_active_threads | Spring | 线程池活跃线程 |
| spring_datasource_connections_active | Spring | 数据库连接数 |

## 故障排查

### 常见问题

#### 1. Prometheus 无法启动

```bash
# 检查配置文件语法
docker run --rm -v ${PWD}/prometheus.yml:/etc/prometheus/prometheus.yml prom/prometheus:v2.45.0 promtool check config /etc/prometheus/prometheus.yml

# 检查告警规则语法
docker run --rm -v ${PWD}/rules:/etc/prometheus/rules prom/prometheus:v2.45.0 promtool check rules /etc/prometheus/rules/*.yml
```

#### 2. Grafana 无法连接 Prometheus

1. 检查 Prometheus 服务是否运行：`docker-compose ps prometheus`
2. 检查数据源配置：`grafana-datasources.yml`
3. 检查网络连接：`docker network inspect sprint28-monitoring`

#### 3. 告警不触发

1. 检查告警规则语法
2. 检查 Prometheus 告警页面：http://localhost:9090/alerts
3. 检查 AlertManager 状态：http://localhost:9093/#/status

### 日志位置

```bash
# 查看 Prometheus 日志
docker-compose logs prometheus

# 查看 Grafana 日志
docker-compose logs grafana

# 查看 AlertManager 日志
docker-compose logs alertmanager
```

## 性能优化

### Prometheus 优化

1. **调整采集间隔**：根据实际需求调整 `scrape_interval`
2. **优化存储**：启用 WAL 压缩，调整保留策略
3. **使用 Recording Rules**：预计算常用查询

### Grafana 优化

1. **使用缓存**：启用查询缓存
2. **优化查询**：使用 Recording Rules 减少实时计算
3. **限制数据点**：设置查询时间范围限制

## 安全建议

1. **修改默认密码**：立即修改 Grafana 默认密码
2. **启用 HTTPS**：生产环境使用 HTTPS
3. **访问控制**：配置防火墙规则，限制访问IP
4. **敏感信息**：使用环境变量或密钥管理服务

## 更新日志

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| v1.0 | 2026-04-26 | 初始版本，完整的监控告警模块 Prometheus 配置 |

## 相关文档

- [Prometheus 官方文档](https://prometheus.io/docs/)
- [Grafana 官方文档](https://grafana.com/docs/)
- [AlertManager 官方文档](https://prometheus.io/docs/alerting/latest/alertmanager/)

## 联系方式

- **维护团队**: devops-engineer
- **项目地址**: AI-Ready 企智连ERP系统
