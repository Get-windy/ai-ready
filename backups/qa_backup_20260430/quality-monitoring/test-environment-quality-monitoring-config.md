# 测试环境质量监控配置

**版本**: 1.0  
**创建日期**: 2026-04-27  
**作者**: qa-lead  
**项目**: AI-Ready企业级ERP系统  
**Sprint**: Sprint 27+1  

---

## 一、监控架构设计

### 1.1 监控架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    测试环境质量监控系统                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐       │
│  │   数据采集层   │  │   数据存储层   │  │   数据展示层   │       │
│  │ • Prometheus │  │ • Prometheus │  │ • Grafana   │       │
│  │ • Node Exporter│  │ • InfluxDB  │  │ • Kibana    │       │
│  │ • Blackbox    │  │ • Elasticsearch│ │ • AlertManager│    │
│  │ • JMX Exporter│  │ • PostgreSQL │  │ • 告警通知    │       │
│  └─────────────┘  └─────────────┘  └─────────────┘       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 监控组件说明

| 组件名称 | 版本 | 作用 | 部署位置 |
|---------|------|------|---------|
| Prometheus | 2.45+ | 指标采集和存储 | 监控服务器 |
| Grafana | 10.2+ | 数据可视化和仪表盘 | 监控服务器 |
| AlertManager | 0.25+ | 告警管理和通知 | 监控服务器 |
| Node Exporter | 1.6+ | 系统指标采集 | 所有服务器 |
| Blackbox Exporter | 0.24+ | 网络探测和HTTP检查 | 监控服务器 |
| JMX Exporter | 0.19+ | Java应用指标采集 | 应用服务器 |
| PostgreSQL | 14.2+ | 告警历史和配置存储 | 数据库服务器 |
| Elasticsearch | 8.11+ | 日志存储和分析 | 日志服务器 |
| Kibana | 8.11+ | 日志可视化和分析 | 日志服务器 |

---

## 二、质量指标监控配置

### 2.1 可用性指标监控

#### 2.1.1 环境可用率监控
```yaml
# prometheus/rules/availability.rules.yml
groups:
  - name: environment_availability
    rules:
      - record: environment_availability
        expr: |
          avg_over_time(
            up{job=~".*"}
            [5m]
          ) * 100
        labels:
          metric_type: "availability"
          
      - alert: EnvironmentAvailabilityLow
        expr: environment_availability < 99.5
        for: 5m
        labels:
          severity: warning
          service: environment
        annotations:
          summary: "环境可用率低于阈值"
          description: "环境可用率当前为 {{ $value }}%，低于阈值 99.5%"
```

#### 2.1.2 服务可用率监控
```yaml
# prometheus/rules/service_availability.rules.yml
groups:
  - name: service_availability
    rules:
      - record: service_availability
        expr: |
          avg_over_time(
            up{job=~"api.*|web.*|db.*|mq.*|cache.*"}
            [5m]
          ) * 100
        labels:
          metric_type: "service_availability"
          
      - alert: ServiceAvailabilityLow
        expr: service_availability < 99
        for: 3m
        labels:
          severity: critical
          service: "{{ $labels.job }}"
        annotations:
          summary: "服务 {{ $labels.job }} 可用率低"
          description: "服务 {{ $labels.job }} 可用率当前为 {{ $value }}%，低于阈值 99%"
```

### 2.2 性能指标监控

#### 2.2.1 API响应时间监控
```yaml
# prometheus/rules/api_performance.rules.yml
groups:
  - name: api_performance
    rules:
      - record: api_response_time_p95
        expr: |
          histogram_quantile(0.95,
            sum(rate(http_request_duration_seconds_bucket[5m]))
            by (le, method, path, status)
          )
        labels:
          metric_type: "response_time"
          
      - alert: APIResponseTimeHigh
        expr: api_response_time_p95 > 0.5
        for: 2m
        labels:
          severity: warning
          api: "{{ $labels.method }} {{ $labels.path }}"
        annotations:
          summary: "API响应时间过高"
          description: "API {{ $labels.method }} {{ $labels.path }} 响应时间P95为 {{ $value }}s，超过阈值 0.5s"
```

#### 2.2.2 数据库性能监控
```yaml
# prometheus/rules/database_performance.rules.yml
groups:
  - name: database_performance
    rules:
      - record: db_query_time_p95
        expr: |
          histogram_quantile(0.95,
            sum(rate(pg_stat_statements_total_time_seconds_bucket[5m]))
            by (le, queryid)
          )
        labels:
          metric_type: "query_time"
          
      - alert: DatabaseQueryTimeHigh
        expr: db_query_time_p95 > 0.1
        for: 2m
        labels:
          severity: warning
          database: postgresql
        annotations:
          summary: "数据库查询时间过高"
          description: "数据库查询时间P95为 {{ $value }}s，超过阈值 0.1s"
```

### 2.3 稳定性指标监控

#### 2.3.1 服务重启监控
```yaml
# prometheus/rules/service_stability.rules.yml
groups:
  - name: service_stability
    rules:
      - record: service_restart_count
        expr: |
          changes(process_start_time_seconds[1h])
        labels:
          metric_type: "restart_count"
          
      - alert: ServiceRestartFrequent
        expr: service_restart_count > 3
        for: 1h
        labels:
          severity: warning
          service: "{{ $labels.job }}"
        annotations:
          summary: "服务重启频繁"
          description: "服务 {{ $labels.job }} 在过去1小时内重启了 {{ $value }} 次"
```

#### 2.3.2 错误率监控
```yaml
# prometheus/rules/error_rate.rules.yml
groups:
  - name: error_rate
    rules:
      - record: http_error_rate
        expr: |
          sum(rate(http_requests_total{status=~"5.."}[5m]))
          /
          sum(rate(http_requests_total[5m]))
          * 100
        labels:
          metric_type: "error_rate"
          
      - alert: HTTPErrorRateHigh
        expr: http_error_rate > 1
        for: 2m
        labels:
          severity: critical
          service: "{{ $labels.job }}"
        annotations:
          summary: "HTTP错误率过高"
          description: "服务 {{ $labels.job }} HTTP错误率当前为 {{ $value }}%，超过阈值 1%"
```

### 2.4 资源指标监控

#### 2.4.1 CPU使用率监控
```yaml
# prometheus/rules/cpu_usage.rules.yml
groups:
  - name: cpu_usage
    rules:
      - record: cpu_usage_percent
        expr: |
          100 - (avg by(instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)
        labels:
          metric_type: "cpu_usage"
          
      - alert: CPUUsageHigh
        expr: cpu_usage_percent > 80
        for: 5m
        labels:
          severity: warning
          instance: "{{ $labels.instance }}"
        annotations:
          summary: "CPU使用率过高"
          description: "实例 {{ $labels.instance }} CPU使用率当前为 {{ $value }}%，超过阈值 80%"
```

#### 2.4.2 内存使用率监控
```yaml
# prometheus/rules/memory_usage.rules.yml
groups:
  - name: memory_usage
    rules:
      - record: memory_usage_percent
        expr: |
          (node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes)
          / node_memory_MemTotal_bytes
          * 100
        labels:
          metric_type: "memory_usage"
          
      - alert: MemoryUsageHigh
        expr: memory_usage_percent > 85
        for: 5m
        labels:
          severity: warning
          instance: "{{ $labels.instance }}"
        annotations:
          summary: "内存使用率过高"
          description: "实例 {{ $labels.instance }} 内存使用率当前为 {{ $value }}%，超过阈值 85%"
```

---

## 三、告警规则配置

### 3.1 告警级别定义

| 级别 | 颜色 | 响应时间 | 处理时间 | 通知方式 | 升级机制 |
|------|------|---------|---------|---------|---------|
| P0 | 红色 | 5分钟 | 30分钟 | 电话+短信+邮件+钉钉 | 1小时未解决升级技术总监 |
| P1 | 橙色 | 15分钟 | 2小时 | 短信+邮件+钉钉 | 2小时未解决升级部门经理 |
| P2 | 黄色 | 30分钟 | 4小时 | 邮件+钉钉 | 4小时未解决升级团队负责人 |
| P3 | 蓝色 | 2小时 | 8小时 | 邮件 | 24小时未解决升级团队负责人 |

### 3.2 告警路由配置

```yaml
# alertmanager/config.yml
route:
  group_by: ['alertname', 'severity']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h
  receiver: 'default'
  routes:
    - match:
        severity: critical
      receiver: 'p0-team'
      group_wait: 10s
      repeat_interval: 5m
      continue: true
      
    - match:
        severity: warning
      receiver: 'p1-team'
      group_wait: 30s
      repeat_interval: 15m
      continue: true
      
    - match:
        severity: info
      receiver: 'p2-team'
      group_wait: 1m
      repeat_interval: 30m
      continue: true

receivers:
  - name: 'default'
    email_configs:
      - to: 'qa-team@company.com'
        from: 'alert@monitoring.company.com'
        smarthost: 'smtp.company.com:587'
        auth_username: 'alert'
        auth_password: 'password'
        
  - name: 'p0-team'
    email_configs:
      - to: 'p0-team@company.com'
    webhook_configs:
      - url: 'https://hooks.slack.com/services/xxx/yyy/zzz'
        send_resolved: true
    pagerduty_configs:
      - service_key: 'pagerduty-key'
    wechat_configs:
      - corp_id: 'corp-id'
        agent_id: 'agent-id'
        secret: 'secret'
        to_user: '@all'
        message_type: 'text'
        
  - name: 'p1-team'
    email_configs:
      - to: 'p1-team@company.com'
    webhook_configs:
      - url: 'https://hooks.slack.com/services/xxx/yyy/zzz'
        
  - name: 'p2-team'
    email_configs:
      - to: 'p2-team@company.com'
```

### 3.3 告警模板配置

```yaml
# alertmanager/templates/custom.tmpl
{{ define "email.default.subject" }}
[{{ .Status | toUpper }}] {{ .GroupLabels.alertname }}
{{ end }}

{{ define "email.default.html" }}
<!DOCTYPE html>
<html>
<head>
    <title>{{ .GroupLabels.alertname }}</title>
    <style>
        body { font-family: Arial, sans-serif; }
        .alert { border: 1px solid #ccc; padding: 15px; margin: 10px; }
        .critical { background-color: #ffcccc; border-color: #ff0000; }
        .warning { background-color: #fff3cd; border-color: #ffc107; }
        .info { background-color: #d1ecf1; border-color: #0c5460; }
        .labels { margin: 10px 0; }
        .label { display: inline-block; background: #eee; padding: 2px 5px; margin: 2px; }
    </style>
</head>
<body>
    <h2>告警详情</h2>
    
    <div class="alert {{ .Status }}">
        <h3>{{ .GroupLabels.alertname }}</h3>
        <p><strong>状态:</strong> {{ .Status | toUpper }}</p>
        <p><strong>严重程度:</strong> {{ .CommonLabels.severity }}</p>
        <p><strong>发生时间:</strong> {{ .StartsAt.Format "2006-01-02 15:04:05" }}</p>
        
        <div class="labels">
            <strong>标签:</strong>
            {{ range .CommonLabels.SortedPairs }}
            <span class="label">{{ .Name }}: {{ .Value }}</span>
            {{ end }}
        </div>
        
        <div>
            <strong>告警详情:</strong>
            <pre>{{ range .Annotations.SortedPairs }}{{ .Name }}: {{ .Value }}
{{ end }}</pre>
        </div>
        
        <div>
            <strong>受影响的实例:</strong>
            <ul>
                {{ range .Alerts }}
                <li>{{ .Labels.instance }}</li>
                {{ end }}
            </ul>
        </div>
    </div>
    
    <p><small>此邮件由监控系统自动发送，请勿直接回复。</small></p>
</body>
</html>
{{ end }}
```

---

## 四、监控仪表盘配置

### 4.1 环境健康仪表盘

```json
{
  "dashboard": {
    "title": "测试环境健康监控",
    "panels": [
      {
        "title": "环境可用率",
        "type": "stat",
        "targets": [
          {
            "expr": "environment_availability",
            "legendFormat": "可用率: {{value}}%"
          }
        ],
        "thresholds": {
          "steps": [
            {"color": "red", "value": 0},
            {"color": "yellow", "value": 99.5},
            {"color": "green", "value": 99.9}
          ]
        }
      },
      {
        "title": "服务状态",
        "type": "heatmap",
        "targets": [
          {
            "expr": "up",
            "legendFormat": "{{job}}"
          }
        ]
      }
    ]
  }
}
```

### 4.2 性能监控仪表盘

```json
{
  "dashboard": {
    "title": "测试环境性能监控",
    "panels": [
      {
        "title": "API响应时间",
        "type": "graph",
        "targets": [
          {
            "expr": "api_response_time_p95",
            "legendFormat": "{{method}} {{path}}"
          }
        ],
        "yaxes": [
          {"format": "s", "min": 0}
        ]
      },
      {
        "title": "数据库性能",
        "type": "graph",
        "targets": [
          {
            "expr": "db_query_time_p95",
            "legendFormat": "查询时间"
          }
        ]
      }
    ]
  }
}
```

### 4.3 资源监控仪表盘

```json
{
  "dashboard": {
    "title": "测试环境资源监控",
    "panels": [
      {
        "title": "CPU使用率",
        "type": "gauge",
        "targets": [
          {
            "expr": "cpu_usage_percent",
            "legendFormat": "{{instance}}"
          }
        ],
        "thresholds": {
          "steps": [
            {"color": "green", "value": 0},
            {"color": "yellow", "value": 70},
            {"color": "red", "value": 80}
          ]
        }
      },
      {
        "title": "内存使用率",
        "type": "gauge",
        "targets": [
          {
            "expr": "memory_usage_percent",
            "legendFormat": "{{instance}}"
          }
        ],
        "thresholds": {
          "steps": [
            {"color": "green", "value": 0},
            {"color": "yellow", "value": 75},
            {"color": "red", "value": 85}
          ]
        }
      }
    ]
  }
}
```

---

## 五、监控数据保留策略

### 5.1 数据保留周期

| 数据类型 | 保留周期 | 存储位置 | 压缩策略 | 备份策略 |
|---------|---------|---------|---------|---------|
| 实时指标数据 | 30天 | Prometheus TSDB | 自动压缩 | 每日备份 |
| 历史指标数据 | 1年 | InfluxDB | 自动压缩 | 每周备份 |
| 告警历史数据 | 180天 | PostgreSQL | 分区存储 | 每日备份 |
| 日志数据 | 90天 | Elasticsearch | 索引轮转 | 每日快照 |
| 监控配置数据 | 永久 | Git仓库 | 版本控制 | 实时同步 |

### 5.2 数据清理策略

```yaml
# prometheus/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  
rule_files:
  - "rules/*.yml"
  
storage:
  tsdb:
    retention:
      time: 30d
      size: 100GB
    wal:
      segment_size: 128MB
    block:
      max_chunk_size: 512MB
      
alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093
```

### 5.3 数据备份策略

```bash
#!/bin/bash
# backup_monitoring_data.sh

# 备份Prometheus数据
prometheus_backup() {
  local backup_dir="/backup/prometheus/$(date +%Y%m%d)"
  mkdir -p "$backup_dir"
  
  # 停止Prometheus
  systemctl stop prometheus
  
  # 备份数据目录
  tar -czf "$backup_dir/prometheus-data.tar.gz" -C /var/lib/prometheus .
  
  # 启动Prometheus
  systemctl start prometheus
  
  echo "Prometheus备份完成: $backup_dir/prometheus-data.tar.gz"
}

# 备份告警历史
alert_history_backup() {
  local backup_file="/backup/alerts/alert_history_$(date +%Y%m%d).sql"
  
  pg_dump -U postgres -d alertdb -t alerts -f "$backup_file"
  
  echo "告警历史备份完成: $backup_file"
}

# 执行备份
prometheus_backup
alert_history_backup

# 清理旧备份（保留最近30天）
find /backup -type f -mtime +30 -delete
```

---

## 六、监控系统维护

### 6.1 日常维护任务

| 任务 | 频率 | 执行时间 | 负责人 | 检查项 |
|------|------|---------|--------|--------|
| 监控系统健康检查 | 每日 | 09:00 | 监控运维 | 所有组件状态、数据采集、告警发送 |
| 告警规则验证 | 每周 | 周一10:00 | QA工程师 | 告警阈值、通知渠道、处理流程 |
| 监控仪表盘更新 | 每月 | 每月1日 | 监控运维 | 仪表盘数据、可视化效果、告警集成 |
| 监控数据清理 | 每月 | 每月最后一天 | 监控运维 | 数据保留策略执行、存储空间清理 |
| 监控系统升级 | 每季度 | 季度末 | 技术团队 | 组件版本升级、配置更新、兼容性测试 |

### 6.2 故障处理流程

1. **故障发现**: 通过监控告警或用户反馈发现故障
2. **故障确认**: 确认故障范围和影响程度
3. **故障诊断**: 分析故障原因和定位问题
4. **故障处理**: 实施故障修复措施
5. **故障恢复**: 验证故障已修复，系统恢复正常
6. **故障复盘**: 分析故障原因，制定预防措施
7. **故障记录**: 记录故障处理过程和经验教训

### 6.3 性能优化建议

1. **数据采集优化**: 调整采集频率，优化查询语句
2. **存储优化**: 优化数据存储策略，减少存储空间
3. **查询优化**: 优化仪表盘查询，提高查询效率
4. **告警优化**: 优化告警规则，减少误报和漏报
5. **可视化优化**: 优化仪表盘布局，提高可读性

---

## 七、附录

### 7.1 监控指标清单

| 指标类别 | 指标名称 | 指标描述 | 监控频率 | 告警阈值 |
|---------|---------|---------|---------|---------|
| 可用性 | environment_availability | 环境可用率 | 每分钟 | < 99.5% |
| 可用性 | service_availability | 服务可用率 | 每分钟 | < 99% |
| 性能 | api_response_time_p95 | API响应时间P95 | 每分钟 | > 500ms |
| 性能 | db_query_time_p95 | 数据库查询时间P95 | 每分钟 | > 100ms |
| 稳定性 | service_restart_count | 服务重启次数 | 每小时 | > 3次 |
| 稳定性 | http_error_rate | HTTP错误率 | 每分钟 | > 1% |
| 资源 | cpu_usage_percent | CPU使用率 | 每分钟 | > 80% |
| 资源 | memory_usage_percent | 内存使用率 | 每分钟 | > 85% |
| 资源 | disk_usage_percent | 磁盘使用率 | 每分钟 | > 90% |
| 安全 | failed_login_attempts | 登录失败次数 | 每分钟 | > 10次 |

### 7.2 监控配置检查清单

- [ ] Prometheus配置正确
- [ ] Grafana配置正确
- [ ] AlertManager配置正确
- [ ] 告警规则配置正确
- [ ] 通知渠道配置正确
- [ ] 仪表盘配置正确
- [ ] 数据采集配置正确
- [ ] 数据存储配置正确
- [ ] 数据备份配置正确
- [ ] 监控系统安全配置正确

### 7.3 常见问题排查

1. **数据采集失败**: 检查Exporter状态、网络连通性、防火墙配置
2. **告警不发送**: 检查AlertManager配置、通知渠道、网络连接
3. **仪表盘无数据**: 检查数据源配置、查询语句、权限设置
4. **性能问题**: 检查资源使用、查询优化、存储优化
5. **安全问题**: 检查访问控制、数据加密、日志审计

---

**文档版本**: v1.0  
**最后更新**: 2026-04-27  
**更新说明**: 初始版本，创建完整的测试环境质量监控配置文档