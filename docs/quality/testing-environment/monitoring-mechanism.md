# 测试环境质量监控机制

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**适用项目**: AI-Ready (企智连)  
**适用范围**: Sprint 27+1 测试环境配置

---

## 1. 监控架构

```
┌──────────────────────────────────────────────────────────────────┐
│                     质量监控架构                                 │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ 数据采集层   │  │ 数据分析层   │  │ 告警通知层   │          │
│  │              │  │              │  │              │          │
│  │ • 健康检查   │  │ • 质量评分   │  │ • 邮件通知   │          │
│  │ • 性能指标   │  │ • 趋势分析   │  │ • 企业微信   │          │
│  │ • 日志采集   │  │ • 异常检测   │  │ • 短信通知   │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 2. 监控指标

### 2.1 基础设施指标

| 指标名称 | 采集方式 | 阈值 | 告警级别 |
|---------|---------|------|----------|
| CPU使用率 | Docker Stats | > 80% | P1 |
| 内存使用率 | Docker Stats | > 85% | P1 |
| 磁盘使用率 | Docker Stats | > 80% | P1 |
| 容器状态 | Docker API | 非运行 | P0 |
| 网络连通性 | Ping/TCP | 不可达 | P0 |

### 2.2 服务性能指标

| 指标名称 | 采集方式 | 阈值 | 告警级别 |
|---------|---------|------|----------|
| 数据库P95 | PostgreSQL | > 50ms | P1 |
| Redis P95 | Redis INFO | > 1ms | P1 |
| API P95 | Prometheus | > 500ms | P1 |
| 消息队列P95 | RabbitMQ | > 10ms | P2 |
| 缓存命中率 | Redis INFO | < 95% | P2 |

### 2.3 业务指标

| 指标名称 | 采集方式 | 阈值 | 告警级别 |
|---------|---------|------|----------|
| 服务可用性 | 健康检查 | < 99.9% | P0 |
| 错误率 | 日志分析 | > 1% | P1 |
| 响应时间 | APM | > 2s | P1 |
| 吞吐量 | 监控指标 | < 基准 | P2 |

---

## 3. 监控频率

| 监控类型 | 频率 | 保留时间 | 工具 |
|---------|------|----------|------|
| 健康检查 | 每5分钟 | 7天 | pytest + cron |
| 性能指标 | 每1分钟 | 30天 | Prometheus |
| 日志监控 | 实时 | 90天 | ELK Stack |
| 安全扫描 | 每周 | 1年 | 安全工具 |
| 质量评估 | 每周 | 1年 | 自定义脚本 |

---

## 4. 告警规则

### 4.1 P0 告警（严重）

- 服务完全不可用
- 数据库连接失败
- 核心服务崩溃
- 安全漏洞发现

**通知方式**: 立即通知，电话/短信 + 企业微信 + 邮件

### 4.2 P1 告警（重要）

- 性能指标超过阈值
- 资源使用率过高
- 错误率上升
- 监控数据缺失

**通知方式**: 5分钟内通知，企业微信 + 邮件

### 4.3 P2 告警（一般）

- 非核心服务异常
- 性能轻微下降
- 配置变更
- 备份失败

**通知方式**: 30分钟内通知，邮件

---

## 5. 监控脚本

### 5.1 健康检查监控脚本

```bash
#!/bin/bash
# monitoring/health_check_monitor.sh

SERVICES=(
  "http://localhost:9090/-/healthy"
  "http://localhost:9093/-/healthy"
  "http://localhost:3000/api/health"
  "http://localhost:9101/health"
  "http://localhost:8081/actuator/health"
)

for service in "${SERVICES[@]}"; do
  status=$(curl -s -o /dev/null -w "%{http_code}" "$service")
  if [ "$status" != "200" ]; then
    echo "[ERROR] $service 异常 (HTTP $status)"
    # 发送告警通知
  else
    echo "[OK] $service 正常"
  fi
done
```

### 5.2 性能监控脚本

```bash
#!/bin/bash
# monitoring/performance_monitor.sh

# 数据库性能检查
db_response=$(psql -h localhost -p 5433 -U monitoring_user -c "SELECT 1" -t -A 2>/dev/null)
if [ "$db_response" != "1" ]; then
  echo "[ERROR] 数据库连接异常"
fi

# Redis性能检查
redis_response=$(redis-cli -h localhost -p 6380 ping 2>/dev/null)
if [ "$redis_response" != "PONG" ]; then
  echo "[ERROR] Redis连接异常"
fi

# API性能检查
api_response=$(curl -s -o /dev/null -w "%{time_total}" http://localhost:8081/actuator/health)
if (( $(echo "$api_response > 0.5" | bc -l) )); then
  echo "[WARNING] API响应时间: ${api_response}s"
fi
```

### 5.3 资源监控脚本

```bash
#!/bin/bash
# monitoring/resource_monitor.sh

# CPU使用率
cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)
if (( $(echo "$cpu_usage > 80" | bc -l) )); then
  echo "[WARNING] CPU使用率: ${cpu_usage}%"
fi

# 内存使用率
mem_usage=$(free | grep Mem | awk '{printf "%.2f", $3/$2 * 100.0}')
if (( $(echo "$mem_usage > 85" | bc -l) )); then
  echo "[WARNING] 内存使用率: ${mem_usage}%"
fi

# 磁盘使用率
disk_usage=$(df / | tail -1 | awk '{print $5}' | cut -d'%' -f1)
if [ "$disk_usage" -gt 80 ]; then
  echo "[WARNING] 磁盘使用率: ${disk_usage}%"
fi
```

---

## 6. 监控配置

### 6.1 Prometheus 监控规则

```yaml
# monitoring/prometheus-rules.yml
groups:
  - name: test-environment-rules
    interval: 30s
    rules:
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "服务 {{ $labels.instance }} 不可用"
          
      - alert: HighCPUUsage
        expr: 100 - (avg by(instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "CPU使用率超过80%"
          
      - alert: HighMemoryUsage
        expr: (node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100 > 85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "内存使用率超过85%"
          
      - alert: DatabaseSlowQuery
        expr: histogram_quantile(0.95, rate(postgresql_query_duration_seconds_bucket[5m])) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "数据库P95查询时间超过50ms"
```

### 6.2 Grafana 仪表盘配置

```json
{
  "dashboard": {
    "title": "测试环境质量监控",
    "panels": [
      {
        "title": "服务健康状态",
        "type": "stat",
        "targets": [
          {
            "expr": "up",
            "legendFormat": "{{ instance }}"
          }
        ]
      },
      {
        "title": "CPU使用率",
        "type": "graph",
        "targets": [
          {
            "expr": "100 - (avg by(instance) (irate(node_cpu_seconds_total{mode=\"idle\"}[5m])) * 100)",
            "legendFormat": "{{ instance }}"
          }
        ]
      },
      {
        "title": "内存使用率",
        "type": "graph",
        "targets": [
          {
            "expr": "(node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100",
            "legendFormat": "{{ instance }}"
          }
        ]
      }
    ]
  }
}
```

---

## 7. 质量监控流程

### 7.1 日常监控流程

```
每日 09:00
  ↓
执行健康检查脚本
  ↓
检查所有服务状态
  ↓
记录检查结果
  ↓
如发现异常 → 立即告警
  ↓
生成日常检查报告
```

### 7.2 周度评估流程

```
每周一 09:00
  ↓
执行质量评估脚本
  ↓
收集一周监控数据
  ↓
计算质量评分
  ↓
生成质量评估报告
  ↓
发送给相关团队
```

### 7.3 Sprint验收流程

```
Sprint结束前2天
  ↓
执行完整质量检查
  ↓
运行自动化测试套件
  ↓
生成质量验收报告
  ↓
评估是否满足验收标准
  ↓
如不通过 → 修复问题
  ↓
重新评估
```

---

## 8. 监控工具

| 工具 | 版本 | 用途 | 配置路径 |
|------|------|------|----------|
| Prometheus | v2.45.0 | 指标采集 | monitoring/prometheus.yml |
| Grafana | 10.0.0 | 可视化 | monitoring/grafana/ |
| AlertManager | v0.25.0 | 告警管理 | monitoring/alertmanager.yml |
| pytest | 8.x | 自动化测试 | qa/automation/testing/ |
| Allure | 2.x | 报告生成 | qa/reports/ |

---

## 9. 监控数据管理

### 9.1 数据保留策略

| 数据类型 | 保留时间 | 存储位置 |
|---------|----------|----------|
| 监控指标 | 30天 | Prometheus |
| 日志数据 | 90天 | ELK Stack |
| 检查报告 | 1年 | 文件系统 |
| 告警记录 | 1年 | 数据库 |

### 9.2 数据备份

```bash
# 监控数据备份脚本
#!/bin/bash
# monitoring/backup_monitoring_data.sh

BACKUP_DIR="/backup/monitoring/$(date +%Y%m%d)"
mkdir -p "$BACKUP_DIR"

# 备份Prometheus数据
tar czf "$BACKUP_DIR/prometheus.tar.gz" /var/lib/prometheus/

# 备份Grafana配置
tar czf "$BACKUP_DIR/grafana.tar.gz" /var/lib/grafana/

# 备份告警规则
cp monitoring/prometheus-rules.yml "$BACKUP_DIR/"

echo "监控数据备份完成: $BACKUP_DIR"
```

---

## 10. 监控维护

### 10.1 定期维护任务

| 任务 | 频率 | 负责人 | 说明 |
|------|------|--------|------|
| 检查监控状态 | 每日 | QA工程师 | 确认监控正常运行 |
| 清理过期数据 | 每周 | DevOps | 清理超过保留期的数据 |
| 更新告警规则 | 每月 | QA Lead | 根据需求更新规则 |
| 评估监控效果 | 每季度 | 团队 | 评估监控体系有效性 |

### 10.2 监控优化

```bash
# 监控性能优化
# 1. 调整采集频率
# 2. 优化查询性能
# 3. 清理无用指标
# 4. 更新仪表盘
```

---

**文档维护**: QA Team  
**最后更新**: 2026-04-27
