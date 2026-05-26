# Sprint 27+1 测试环境监控告警配置优化

## 文档概述

本文档为 Sprint 27+1 测试环境提供完整的监控告警配置方案，基于已开发的前端监控面板，优化监控指标和告警规则。

## 一、基础设施监控指标配置

### 1.1 CPU使用率监控

| 指标名称 | 告警阈值 | 告警级别 | 检测周期 | 说明 |
|---------|---------|---------|---------|------|
| CPU使用率 > 80% | 80% | Warning | 30s | 高负载警告 |
| CPU使用率 > 90% | 90% | Critical | 30s | 严重高负载 |
| CPU使用率 > 95% | 95% | Emergency | 15s | 紧急处理 |

**配置代码示例：**
```yaml
cpu_monitor:
  enabled: true
  check_interval: 30s
  thresholds:
    warning: 80
    critical: 90
    emergency: 95
```

### 1.2 内存使用率监控

| 指标名称 | 告警阈值 | 告警级别 | 检测周期 | 说明 |
|---------|---------|---------|---------|------|
| 内存使用率 > 75% | 75% | Warning | 30s | 内存不足警告 |
| 内存使用率 > 85% | 85% | Critical | 30s | 严重内存不足 |
| 内存使用率 > 90% | 90% | Emergency | 15s | 紧急内存不足 |

**配置代码示例：**
```yaml
memory_monitor:
  enabled: true
  check_interval: 30s
  thresholds:
    warning: 75
    critical: 85
    emergency: 90
```

### 1.3 磁盘空间监控

| 指标名称 | 告警阈值 | 告警级别 | 检测周期 | 说明 |
|---------|---------|---------|---------|------|
| 磁盘使用率 > 70% | 70% | Warning | 60s | 磁盘空间警告 |
| 磁盘使用率 > 85% | 85% | Critical | 60s | 严重磁盘不足 |
| 磁盘使用率 > 90% | 90% | Emergency | 30s | 紧急磁盘不足 |

**配置代码示例：**
```yaml
disk_monitor:
  enabled: true
  check_interval: 60s
  paths:
    - path: /
      name: 系统盘
    - path: /data
      name: 数据盘
    - path: /logs
      name: 日志盘
  thresholds:
    warning: 70
    critical: 85
    emergency: 90
```

### 1.4 网络带宽监控

| 指标名称 | 告警阈值 | 告警级别 | 检测周期 | 说明 |
|---------|---------|---------|---------|------|
| 入站流量 > 500 Mbps | 500 Mbps | Warning | 30s | 入站流量警告 |
| 出站流量 > 500 Mbps | 500 Mbps | Warning | 30s | 出站流量警告 |
| 网络丢包率 > 1% | 1% | Warning | 30s | 网络丢包警告 |
| 网络延迟 > 100ms | 100ms | Warning | 30s | 网络延迟警告 |

**配置代码示例：**
```yaml
network_monitor:
  enabled: true
  check_interval: 30s
  thresholds:
    inbound_traffic_warning: 500  # Mbps
    outbound_traffic_warning: 500  # Mbps
    packet_loss_warning: 1  # %
    latency_warning: 100  # ms
```

## 二、服务健康监控配置

### 2.1 API服务响应时间监控

| 指标名称 | 告警阈值 | 告警级别 | 检测周期 | 说明 |
|---------|---------|---------|---------|------|
| P95响应时间 > 1s | 1000ms | Warning | 30s | API响应变慢 |
| P95响应时间 > 2s | 2000ms | Critical | 30s | API响应严重变慢 |
| P99响应时间 > 3s | 3000ms | Emergency | 15s | API响应紧急变慢 |
| 错误率 > 5% | 5% | Warning | 30s | 错误率上升 |
| 错误率 > 10% | 10% | Critical | 30s | 错误率严重上升 |

**配置代码示例：**
```yaml
api_monitor:
  enabled: true
  check_interval: 30s
  endpoints:
    - name: 用户API
      url: http://localhost:3000/api/users
    - name: 订单API
      url: http://localhost:3000/api/orders
    - name: 商品API
      url: http://localhost:3000/api/products
  thresholds:
    p95_warning: 1000  # ms
    p95_critical: 2000  # ms
    p99_emergency: 3000  # ms
    error_rate_warning: 5  # %
    error_rate_critical: 10  # %
```

### 2.2 数据库连接池监控

| 指标名称 | 告警阈值 | 告警级别 | 检测周期 | 说明 |
|---------|---------|---------|---------|------|
| 连接池使用率 > 70% | 70% | Warning | 30s | 连接池使用过高 |
| 连接池使用率 > 85% | 85% | Critical | 30s | 连接池使用严重过高 |
| 查询平均时间 > 500ms | 500ms | Warning | 30s | 查询变慢 |
| 慢查询数量 > 10/min | 10 | Warning | 30s | 慢查询增加 |

**配置代码示例：**
```yaml
database_monitor:
  enabled: true
  check_interval: 30s
  databases:
    - name: 主数据库
      host: localhost
      port: 5432
      type: postgresql
    - name: 从数据库
      host: localhost
      port: 5433
      type: postgresql
  thresholds:
    pool_usage_warning: 70  # %
    pool_usage_critical: 85  # %
    query_avg_time_warning: 500  # ms
    slow_query_warning: 10  # per minute
```

### 2.3 缓存服务命中率监控

| 指标名称 | 告警阈值 | 告警级别 | 检测周期 | 说明 |
|---------|---------|---------|---------|------|
| 命中率 < 80% | 80% | Warning | 30s | 缓存命中率下降 |
| 命中率 < 70% | 70% | Critical | 30s | 缓存命中率严重下降 |
| 内存使用率 > 80% | 80% | Warning | 30s | 缓存内存使用过高 |

**配置代码示例：**
```yaml
cache_monitor:
  enabled: true
  check_interval: 30s
  caches:
    - name: Redis主缓存
      host: localhost
      port: 6379
    - name: Redis会话缓存
      host: localhost
      port: 6380
  thresholds:
    hit_rate_warning: 80  # %
    hit_rate_critical: 70  # %
    memory_usage_warning: 80  # %
```

### 2.4 消息队列积压监控

| 指标名称 | 告警阈值 | 告警级别 | 检测周期 | 说明 |
|---------|---------|---------|---------|------|
| 队列积压 > 1000 | 1000 | Warning | 30s | 队列积压警告 |
| 队列积压 > 5000 | 5000 | Critical | 30s | 队列积压严重 |
| 消费延迟 > 30s | 30s | Warning | 30s | 消费延迟警告 |
| 消费延迟 > 60s | 60s | Critical | 30s | 消费延迟严重 |

**配置代码示例：**
```yaml
message_queue_monitor:
  enabled: true
  check_interval: 30s
  queues:
    - name: 订单队列
      type: rabbitmq
    - name: 通知队列
      type: rabbitmq
    - name: 日志队列
      type: rabbitmq
  thresholds:
    backlog_warning: 1000
    backlog_critical: 5000
    consumer_delay_warning: 30  # seconds
    consumer_delay_critical: 60  # seconds
```

## 三、告警规则配置

### 3.1 关键告警规则

#### 服务宕机告警
```yaml
- name: 服务宕机检测
  type: service_down
  severity: Critical
  enabled: true
  condition: service.status == 'down'
  actions:
    - type: email
      recipients: ['ops@example.com', 'dev@example.com']
    - type: webhook
      url: http://alert-webhook.example.com
    - type: slack
      channel: '#alerts'
  cooldown: 300  # 5分钟冷却期
```

#### 数据库连接失败告警
```yaml
- name: 数据库连接失败
  type: database_connection_failed
  severity: Critical
  enabled: true
  condition: database.status == 'disconnected'
  actions:
    - type: email
      recipients: ['ops@example.com', 'db-admin@example.com']
    - type: sms
      recipients: ['+8613800000000']
  cooldown: 600  # 10分钟冷却期
```

#### 磁盘空间不足告警
```yaml
- name: 磁盘空间不足
  type: disk_space_low
  severity: Warning
  enabled: true
  condition: disk.usage > 70
  actions:
    - type: email
      recipients: ['ops@example.com']
    - type: dingtalk
      webhook: https://oapi.dingtalk.com/robot/send
  cooldown: 3600  # 1小时冷却期
```

### 3.2 告警级别定义

| 级别 | 颜色 | 响应时间 | 通知方式 |
|-----|------|---------|---------|
| Emergency | 红色闪烁 | < 5分钟 | 邮件 + 短信 + 电话 + 群聊 |
| Critical | 红色 | < 15分钟 | 邮件 + 短信 + 群聊 |
| Warning | 黄色 | < 1小时 | 邮件 + 群聊 |
| Info | 蓝色 | < 4小时 | 邮件 |

### 3.3 告警抑制规则

为了避免告警风暴，配置以下抑制规则：

```yaml
alert_suppression:
  # 同一服务在5分钟内只发送一次相同级别的告警
  same_service_cooldown: 300

  # 父级告警触发时，抑制子级告警
  parent_suppression:
    - parent: service_down
      children: [api_slow, database_slow, cache_slow]

  # 维护窗口期不发送告警
  maintenance_windows:
    - name: 每日维护窗口
      start: "02:00"
      end: "03:00"
      timezone: "Asia/Shanghai"
```

## 四、通知渠道配置

### 4.1 邮件通知
```yaml
email_notification:
  enabled: true
  smtp_server: smtp.example.com
  smtp_port: 587
  username: alerts@example.com
  password: ${SMTP_PASSWORD}
  from: "监控系统 <alerts@example.com>"
  default_recipients:
    critical: ['ops@example.com', 'dev@example.com']
    warning: ['ops@example.com']
    info: ['ops@example.com']
```

### 4.2 群聊通知（钉钉/企业微信/飞书）
```yaml
group_chat_notification:
  dingtalk:
    enabled: true
    webhook: ${DINGTALK_WEBHOOK}
    secret: ${DINGTALK_SECRET}
    critical_channel: '#alerts'
    warning_channel: '#alerts'

  wechat:
    enabled: true
    webhook: ${WECHAT_WEBHOOK}
    critical_channel: 'alerts'
    warning_channel: 'alerts'

  feishu:
    enabled: true
    webhook: ${FEISHU_WEBHOOK}
    critical_channel: 'alerts'
    warning_channel: 'alerts'
```

### 4.3 短信通知
```yaml
sms_notification:
  enabled: true
  provider: aliyun
  access_key: ${SMS_ACCESS_KEY}
  access_secret: ${SMS_ACCESS_SECRET}
  sign_name: "系统告警"
  template_code: "SMS_ALERT_TEMPLATE"
  emergency_contacts:
    - name: 运维负责人
      phone: "+8613800000000"
    - name: 技术负责人
      phone: "+8613900000000"
```

## 五、完整配置文件示例

```yaml
# 监控告警主配置文件
version: "1.0.0"
environment: "test"
project: "ai-ready"

# 基础设施监控
infrastructure:
  cpu:
    enabled: true
    check_interval: 30s
    thresholds:
      warning: 80
      critical: 90
      emergency: 95

  memory:
    enabled: true
    check_interval: 30s
    thresholds:
      warning: 75
      critical: 85
      emergency: 90

  disk:
    enabled: true
    check_interval: 60s
    paths:
      - path: /
        name: 系统盘
      - path: /data
        name: 数据盘
    thresholds:
      warning: 70
      critical: 85
      emergency: 90

  network:
    enabled: true
    check_interval: 30s
    thresholds:
      inbound_traffic_warning: 500
      outbound_traffic_warning: 500
      packet_loss_warning: 1
      latency_warning: 100

# 服务健康监控
services:
  api:
    enabled: true
    check_interval: 30s
    endpoints:
      - name: 用户API
        url: http://localhost:3000/api/users
      - name: 订单API
        url: http://localhost:3000/api/orders
    thresholds:
      p95_warning: 1000
      p95_critical: 2000
      p99_emergency: 3000
      error_rate_warning: 5
      error_rate_critical: 10

  database:
    enabled: true
    check_interval: 30s
    databases:
      - name: 主数据库
        host: localhost
        port: 5432
        type: postgresql
    thresholds:
      pool_usage_warning: 70
      pool_usage_critical: 85
      query_avg_time_warning: 500
      slow_query_warning: 10

  cache:
    enabled: true
    check_interval: 30s
    caches:
      - name: Redis主缓存
        host: localhost
        port: 6379
    thresholds:
      hit_rate_warning: 80
      hit_rate_critical: 70
      memory_usage_warning: 80

  message_queue:
    enabled: true
    check_interval: 30s
    queues:
      - name: 订单队列
        type: rabbitmq
    thresholds:
      backlog_warning: 1000
      backlog_critical: 5000
      consumer_delay_warning: 30
      consumer_delay_critical: 60

# 告警规则
alerts:
  - name: 服务宕机检测
    type: service_down
    severity: Critical
    enabled: true
    actions:
      - type: email
        recipients: ['ops@example.com', 'dev@example.com']
      - type: dingtalk
      - type: sms
    cooldown: 300

# 通知渠道
notifications:
  email:
    enabled: true
    smtp_server: smtp.example.com
    smtp_port: 587
    from: "监控系统 <alerts@example.com>"
    default_recipients:
      critical: ['ops@example.com', 'dev@example.com']
      warning: ['ops@example.com']

  group_chat:
    dingtalk:
      enabled: true
      webhook: ${DINGTALK_WEBHOOK}
    wechat:
      enabled: true
      webhook: ${WECHAT_WEBHOOK}
    feishu:
      enabled: true
      webhook: ${FEISHU_WEBHOOK}

  sms:
    enabled: true
    provider: aliyun
    emergency_contacts:
      - phone: "+8613800000000"
```

## 六、实施检查清单

- [ ] 1. 安装监控代理组件
- [ ] 2. 配置监控数据采集
- [ ] 3. 应用告警配置文件
- [ ] 4. 配置通知渠道（邮件、群聊、短信）
- [ ] 5. 测试告警触发
- [ ] 6. 验证告警接收
- [ ] 7. 配置前端监控面板数据源
- [ ] 8. 设置维护窗口
- [ ] 9. 配置告警抑制规则
- [ ] 10. 文档归档和团队培训

## 版本信息

- 创建日期: 2026-04-29
- Sprint: Sprint 27+1
- 版本: v1.0.0
- 作者: 前端开发工程师 (mnj006mb)