# 功能使用手册

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**作者**: mnj0j12k  
**最后更新**: 2026-04-27  

---

## 目录

1. [系统监控](#系统监控)
2. [数据库监控](#数据库监控)
3. [缓存监控](#缓存监控)
4. [消息队列监控](#消息队列监控)
5. [告警管理](#告警管理)
6. [可视化仪表盘](#可视化仪表盘)

---

## 系统监控

### CPU使用率监控

1. **查看CPU使用率**
   - 打开Grafana：http://localhost:3000
   - 导航到"System Monitor"仪表盘
   - 查看CPU使用率 trends

2. **配置CPU告警**
   - 打开Prometheus：http://localhost:9090
   - 导航到"Status" > "Rules"
   - 编辑告警规则
   - 设置CPU使用率阈值

3. **告警规则示例**
```yaml
- alert: HighCPUUsage
  expr: 100 - (avg by(instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 80
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "High CPU usage on {{ $labels.instance }}"
    description: "CPU usage is above 80% (current value: {{ $value }}%)"
```

### 内存使用率监控

1. **查看内存使用率**
   - 打开Grafana
   - 导航到"System Monitor"仪表盘
   - 查看内存使用情况

2. **配置内存告警**
   - 打开Prometheus
   - 编辑告警规则
   - 设置内存使用率阈值

3. **告警规则示例**
```yaml
- alert: HighMemoryUsage
  expr: (node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100 > 80
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "High memory usage on {{ $labels.instance }}"
    description: "Memory usage is above 80% (current value: {{ $value }}%)"
```

### 磁盘使用率监控

1. **查看磁盘使用率**
   - 打开Grafana
   - 导航到"System Monitor"仪表盘
   - 查看磁盘使用情况

2. **配置磁盘告警**
   - 打开Prometheus
   - 编辑告警规则
   - 设置磁盘使用率阈值

3. **告警规则示例**
```yaml
- alert: HighDiskUsage
  expr: (node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"} * 100) < 15
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "Low disk space on {{ $labels.instance }}"
    description: "Disk usage is above 85% (current value: {{ $value }}%)"
```

---

## 数据库监控

### PostgreSQL连接池监控

1. **查看连接池状态**
   - 打开Grafana
   - 导航到"PostgreSQL Monitor"仪表盘
   - 查看活跃连接数、空闲连接数

2. **配置连接池告警**
   - 打开Prometheus
   - 编辑告警规则
   - 设置连接池阈值

3. **告警规则示例**
```yaml
- alert: HighPostgreSQLConnections
  expr: pg_stat_activity_count > 100
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "High PostgreSQL connections on {{ $labels.instance }}"
    description: "Active connections: {{ $value }} (threshold: 100)"
```

### 查询性能监控

1. **查看慢查询**
   - 打开Grafana
   - 导航到"PostgreSQL Monitor"仪表盘
   - 查看慢查询统计

2. **优化建议**
   - 添加索引
   - 优化查询语句
   - 调整连接池大小

### 数据库资源监控

1. **查看数据库资源**
   - 打开Grafana
   - 查看数据库CPU、内存、IO使用情况

2. **配置资源告警**
   - 打开Prometheus
   - 编辑告警规则

---

## 缓存监控

### Redis命中率监控

1. **查看缓存命中率**
   - 打开Grafana
   - 导航到"Redis Monitor"仪表盘
   - 查看缓存命中率趋势

2. **配置命中率告警**
   - 打开Prometheus
   - 编辑告警规则
   - 设置命中率阈值

3. **告警规则示例**
```yaml
- alert: LowRedisHitRate
  expr: redis_keyspace_hits_total / (redis_keyspace_hits_total + redis_keyspace_misses_total) < 0.8
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "Low Redis hit rate on {{ $labels.instance }}"
    description: "Hit rate: {{ $value }} (threshold: 80%)"
```

### 内存使用监控

1. **查看内存使用**
   - 打开Grafana
   - 导航到"Redis Monitor"仪表盘
   - 查看内存使用趋势

2. **配置内存告警**
   - 打开Prometheus
   - 编辑告警规则
   - 设置内存使用阈值

---

## 消息队列监控

### 队列深度监控

1. **查看队列深度**
   - 打开Grafana
   - 导航到"RabbitMQ Monitor"仪表盘
   - 查看队列深度趋势

2. **配置队列告警**
   - 打开Prometheus
   - 编辑告警规则
   - 设置队列深度阈值

3. **告警规则示例**
```yaml
- alert: HighRabbitMQQueue
  expr: rabbitmq_queue_messages_ready > 1000
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "High RabbitMQ queue on {{ $labels.instance }}"
    description: "Queue depth: {{ $value }} (threshold: 1000)"
```

### 消费者监控

1. **查看消费者状态**
   - 打开Grafana
   - 导航到"RabbitMQ Monitor"仪表盘
   - 查看消费者连接状态

2. **配置消费者告警**
   - 打开Prometheus
   - 编辑告警规则
   - 设置消费者告警规则

---

## 告警管理

### 告警规则配置

1. **配置告警规则**
   - 打开Prometheus：http://localhost:9090
   - 导航到"Status" > "Rules"
   - 创建新的告警规则

2. **告警规则结构**
```yaml
groups:
  - name: monitoring-alerts
    rules:
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Service {{ $labels.instance }} is down"
          description: "{{ $labels.instance }} of job {{ $labels.job }} has been down for more than 1 minute."
```

### 告警路由配置

1. **配置AlertManager**
   - 打开AlertManager：http://localhost:9093
   - 编辑`alertmanager.yml`配置文件

2. **告警路由示例**
```yaml
route:
  receiver: 'dingtalk-webhook'
  group_by: ['alertname', 'severity']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 1h
  routes:
    - match:
        severity: critical
      receiver: 'wechat-webhook'
```

### 告警通知配置

1. **配置钉钉通知**
   - 在AlertManager配置中添加钉钉webhook
   - 配置钉钉机器人

2. **配置企业微信通知**
   - 在AlertManager配置中添加企业微信webhook
   - 配置企业微信机器人

---

## 可视化仪表盘

### 预定义仪表盘

1. **系统监控仪表盘**
   - CPU使用率
   - 内存使用率
   - 磁盘使用率
   - 网络流量

2. **数据库监控仪表盘**
   - 连接池状态
   - 查询性能
   - 数据库资源

3. **缓存监控仪表盘**
   - 缓存命中率
   - 内存使用
   - 键值对统计

4. **消息队列监控仪表盘**
   - 队列深度
   - 消费者状态
   - 消息吞吐量

### 自定义仪表盘

1. **创建仪表盘**
   - 打开Grafana
   - 点击"Create" > "New Dashboard"
   - 点击"Add visualization"
   - 配置查询表达式
   - 保存仪表盘

2. **仪表盘分享**
   - 点击"Share"按钮
   - 选择"Direct Link"
   - 复制链接分享

---

## 下一步

- 阅读[管理员配置手册](user-manual-admin.md)了解高级配置
- 阅读[常见问题解答](user-manual-faq.md)获取更多帮助
- 观看[功能演示视频](videos/features-script.md)了解详细功能

---

## 联系我们

如有问题，请联系：

- **技术支援**: devops-engineer
- **文档反馈**: doc-writer
- **项目管理**: coordinator