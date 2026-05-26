# Sprint 27+1 测试环境配置文档 - 健康检查指南（完整版）

## 概述

本文档提供Sprint 27+1测试环境的完整健康检查操作指南，包括服务健康检查配置、健康检查脚本和健康检查命令。确保测试环境的稳定性和可靠性。

## 4. 健康检查结果解读

### 4.1 检查结果状态说明

#### 4.1.1 状态标识

- **✅ 正常 (Normal)**: 服务/组件运行正常，无需干预
- **⚠️  警告 (Warning)**: 存在潜在问题，建议关注但无需立即处理
- **❌ 错误 (Error)**: 存在严重问题，需要立即处理
- **📊 信息 (Info)**: 提供统计信息，帮助了解系统状态

#### 4.1.2 健康状态级别

1. **健康 (Healthy)**
   - 所有关键服务运行正常
   - 系统资源使用率在正常范围内
   - 无触发告警
   - 网络连接正常

2. **需要注意 (Needs Attention)**
   - 部分非关键服务异常
   - 系统资源使用率较高但未超过阈值
   - 存在警告级别的告警
   - 部分监控目标离线

3. **不健康 (Unhealthy)**
   - 关键服务异常
   - 系统资源使用率超过阈值
   - 存在错误级别的告警
   - 网络连接异常

### 4.2 关键指标阈值

#### 4.2.1 系统资源阈值

| 指标 | 正常范围 | 警告范围 | 错误范围 | 检查频率 |
|------|----------|----------|----------|----------|
| CPU使用率 | < 70% | 70-90% | > 90% | 5分钟 |
| 内存使用率 | < 70% | 70-90% | > 90% | 5分钟 |
| 磁盘使用率 | < 80% | 80-95% | > 95% | 15分钟 |
| 系统负载 | < CPU核心数 | 1-1.5倍核心数 | > 1.5倍核心数 | 5分钟 |
| 网络延迟 | < 100ms | 100-500ms | > 500ms | 1分钟 |

#### 4.2.2 服务特定阈值

**PostgreSQL:**
- 连接数: < 最大连接数的80%
- 死锁数: 0 (24小时内)
- 查询响应时间: < 100ms (95%分位)
- 复制延迟: < 1秒

**Redis:**
- 内存使用率: < 80%
- 连接数: < 10000
- 命中率: > 95%
- 持久化延迟: < 5秒

**RabbitMQ:**
- 队列深度: < 1000
- 连接数: < 1000
- 通道数: < 5000
- 消息积压: 0

### 4.3 常见问题及解决方案

#### 4.3.1 PostgreSQL问题

**问题1: 连接数过高**
```
症状: 数据库连接数接近或超过最大限制
解决方案:
1. 检查连接池配置
2. 优化应用程序连接管理
3. 增加max_connections参数
4. 杀死空闲连接
```

**问题2: 查询性能下降**
```
症状: 查询响应时间变长
解决方案:
1. 分析慢查询日志
2. 创建合适的索引
3. 优化查询语句
4. 增加硬件资源
```

#### 4.3.2 Redis问题

**问题1: 内存使用率过高**
```
症状: Redis内存接近或超过限制
解决方案:
1. 分析内存使用模式
2. 优化数据结构和存储
3. 设置适当的过期时间
4. 增加maxmemory配置
```

**问题2: 连接失败**
```
症状: 无法连接到Redis
解决方案:
1. 检查Redis服务状态
2. 验证密码和端口配置
3. 检查防火墙规则
4. 查看Redis日志
```

#### 4.3.3 RabbitMQ问题

**问题1: 队列积压**
```
症状: 消息在队列中堆积
解决方案:
1. 增加消费者数量
2. 优化消息处理逻辑
3. 设置适当的队列TTL
4. 监控消费者状态
```

**问题2: 连接断开**
```
症状: 频繁的连接断开和重连
解决方案:
1. 检查网络稳定性
2. 调整心跳配置
3. 优化连接池设置
4. 监控连接状态
```

### 4.4 健康检查报告

#### 4.4.1 日报生成

**脚本文件**: `/usr/local/bin/generate-health-report.sh`

```bash
#!/bin/bash
# 生成健康检查日报

set -e

REPORT_DIR="/var/reports/health"
DATE=$(date +%Y%m%d)
REPORT_FILE="$REPORT_DIR/health_report_$DATE.md"

# 创建报告目录
mkdir -p "$REPORT_DIR"

# 生成报告
cat > "$REPORT_FILE" << EOF
# 测试环境健康检查日报
## 报告日期: $(date '+%Y年%m月%d日')
## 生成时间: $(date '+%H:%M:%S')

---

## 1. 执行摘要

### 1.1 整体健康状态
$(/usr/local/bin/all-health-check.sh 2>&1 | grep "测试环境健康状态:")

### 1.2 关键发现
- 检查总数: TODO
- 通过检查: TODO
- 失败检查: TODO
- 警告检查: TODO

---

## 2. 详细检查结果

### 2.1 系统资源
\`\`\`
$(/usr/local/bin/all-health-check.sh 2>&1 | grep -A20 "=== 系统资源检查 ===")
\`\`\`

### 2.2 服务状态
\`\`\`
$(/usr/local/bin/all-health-check.sh 2>&1 | grep -A30 "=== 服务检查 ===")
\`\`\`

### 2.3 端口状态
\`\`\`
$(/usr/local/bin/all-health-check.sh 2>&1 | grep -A20 "=== 端口检查 ===")
\`\`\`

---

## 3. 趋势分析

### 3.1 资源使用趋势
- CPU使用率: 稳定/上升/下降
- 内存使用率: 稳定/上升/下降
- 磁盘使用率: 稳定/上升/下降

### 3.2 服务可用性
- PostgreSQL: 99.9%
- Redis: 99.9%
- RabbitMQ: 99.9%
- Prometheus: 99.9%

---

## 4. 建议措施

### 4.1 立即处理
1. TODO

### 4.2 短期计划
1. TODO

### 4.3 长期改进
1. TODO

---

## 5. 附录

### 5.1 检查脚本版本
- all-health-check.sh: v1.0
- postgresql-health-check.sh: v1.0
- redis-health-check.sh: v1.0
- rabbitmq-health-check.sh: v1.0

### 5.2 环境信息
- 主机名: $(hostname)
- 操作系统: $(cat /etc/os-release | grep PRETTY_NAME | cut -d= -f2 | tr -d '"')
- 内核版本: $(uname -r)
- 检查时间: $(date)
EOF

echo "健康检查日报已生成: $REPORT_FILE"
```

#### 4.4.2 周报生成

**脚本文件**: `/usr/local/bin/generate-weekly-report.sh`

```bash
#!/bin/bash
# 生成健康检查周报

set -e

REPORT_DIR="/var/reports/health/weekly"
WEEK=$(date +%Y-W%U)
REPORT_FILE="$REPORT_DIR/weekly_health_report_$WEEK.md"

# 创建报告目录
mkdir -p "$REPORT_DIR"

# 收集本周日报
WEEKLY_SUMMARY=""
for i in {0..6}; do
    DATE=$(date -d "$i days ago" +%Y%m%d)
    DAILY_REPORT="/var/reports/health/health_report_$DATE.md"
    
    if [ -f "$DAILY_REPORT" ]; then
        SUMMARY=$(grep -A2 "### 1.1 整体健康状态" "$DAILY_REPORT" | tail -1)
        WEEKLY_SUMMARY="$WEEKLY_SUMMARY- $(date -d "$i days ago" +%Y-%m-%d): $SUMMARY\n"
    fi
done

# 生成周报
cat > "$REPORT_FILE" << EOF
# 测试环境健康检查周报
## 报告周期: 第 $WEEK 周
## 生成时间: $(date '+%Y年%m月%d日 %H:%M:%S')

---

## 1. 本周摘要

### 1.1 整体健康趋势
\`\`\`
$(echo -e "$WEEKLY_SUMMARY")
\`\`\`

### 1.2 关键指标
- 平均可用性: TODO%
- 最大停机时间: TODO分钟
- 告警总数: TODO
- 解决时间: TODO分钟

---

## 2. 详细分析

### 2.1 服务可用性分析
| 服务 | 周一 | 周二 | 周三 | 周四 | 周五 | 周六 | 周日 | 平均 |
|------|------|------|------|------|------|------|------|------|
| PostgreSQL | TODO | TODO | TODO | TODO | TODO | TODO | TODO | TODO |
| Redis | TODO | TODO | TODO | TODO | TODO | TODO | TODO | TODO |
| RabbitMQ | TODO | TODO | TODO | TODO | TODO | TODO | TODO | TODO |
| Prometheus | TODO | TODO | TODO | TODO | TODO | TODO | TODO | TODO |

### 2.2 资源使用分析
#### CPU使用率
- 周平均值: TODO%
- 周峰值: TODO%
- 趋势: 稳定/上升/下降

#### 内存使用率
- 周平均值: TODO%
- 周峰值: TODO%
- 趋势: 稳定/上升/下降

#### 磁盘使用率
- 周平均值: TODO%
- 周峰值: TODO%
- 趋势: 稳定/上升/下降

---

## 3. 事件回顾

### 3.1 本周事件
| 时间 | 事件 | 影响 | 解决时间 | 根本原因 |
|------|------|------|----------|----------|
| TODO | TODO | TODO | TODO | TODO |

### 3.2 告警统计
| 告警级别 | 数量 | 平均响应时间 | 平均解决时间 |
|----------|------|--------------|--------------|
| 紧急 | TODO | TODO | TODO |
| 警告 | TODO | TODO | TODO |
| 信息 | TODO | TODO | TODO |

---

## 4. 改进建议

### 4.1 本周完成改进
1. TODO

### 4.2 下周计划
1. TODO

### 4.3 长期规划
1. TODO

---

## 5. 附录

### 5.1 检查统计
- 总检查次数: TODO
- 自动化检查占比: TODO%
- 手动检查占比: TODO%

### 5.2 环境变化
- 本周新增: TODO
- 本周变更: TODO
- 本周修复: TODO

### 5.3 团队反馈
- 正面反馈: TODO
- 改进建议: TODO
EOF

echo "健康检查周报已生成: $REPORT_FILE"
```

## 5. 自动化监控配置

### 5.1 Prometheus告警规则

#### 5.1.1 系统级别告警

**配置文件**: `/etc/prometheus/rules/system.rules.yml`

```yaml
groups:
  - name: system_alerts
    rules:
      # CPU使用率告警
      - alert: HighCpuUsage
        expr: 100 - (avg by(instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 90
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "CPU使用率过高"
          description: "实例 {{ $labels.instance }} 的CPU使用率超过90%，当前值为 {{ $value }}%"
      
      # 内存使用率告警
      - alert: HighMemoryUsage
        expr: (node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100 > 90
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "内存使用率过高"
          description: "实例 {{ $labels.instance }} 的内存使用率超过90%，当前值为 {{ $value }}%"
      
      # 磁盘使用率告警
      - alert: HighDiskUsage
        expr: (node_filesystem_size_bytes{fstype!="tmpfs"} - node_filesystem_free_bytes{fstype!="tmpfs"}) / node_filesystem_size_bytes{fstype!="tmpfs"} * 100 > 90
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "磁盘使用率过高"
          description: "实例 {{ $labels.instance }} 的磁盘使用率超过90%，当前值为 {{ $value }}%"
      
      # 系统负载告警
      - alert: HighLoadAverage
        expr: node_load1 > count by(instance) (node_cpu_seconds_total{mode="system"}) * 1.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "系统负载过高"
          description: "实例 {{ $labels.instance }} 的1分钟负载超过CPU核心数的1.5倍，当前值为 {{ $value }}"
```

#### 5.1.2 服务级别告警

**配置文件**: `/etc/prometheus/rules/service.rules.yml`

```yaml
groups:
  - name: service_alerts
    rules:
      # PostgreSQL告警
      - alert: PostgresqlDown
        expr: pg_up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "PostgreSQL服务异常"
          description: "PostgreSQL实例 {{ $labels.instance }} 已下线"
      
      - alert: PostgresqlHighConnections
        expr: pg_stat_database_numbackends > 100
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "PostgreSQL连接数过高"
          description: "PostgreSQL实例 {{ $labels.instance }} 的连接数超过100，当前值为 {{ $value }}"
      
      # Redis告警
      - alert: RedisDown
        expr: redis_up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Redis服务异常"
          description: "Redis实例 {{ $labels.instance }} 已下线"
      
      - alert: RedisHighMemoryUsage
        expr: redis_memory_used_bytes / redis_memory_max_bytes * 100 > 90
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Redis内存使用率过高"
          description: "Redis实例 {{ $labels.instance }} 的内存使用率超过90%，当前值为 {{ $value }}%"
      
      # RabbitMQ告警
      - alert: RabbitmqDown
        expr: rabbitmq_up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "RabbitMQ服务异常"
          description: "RabbitMQ实例 {{ $labels.instance }} 已下线"
      
      - alert: RabbitmqHighQueueDepth
        expr: rabbitmq_queue_messages > 1000
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "RabbitMQ队列深度过高"
          description: "RabbitMQ队列 {{ $labels.queue }} 的深度超过1000，当前值为 {{ $value }}"
```

### 5.2 Grafana仪表板

#### 5.2.1 系统监控仪表板

**JSON配置文件**: `/etc/grafana/dashboards/system-overview.json`

```json
{
  "dashboard": {
    "title": "系统监控概览",
    "panels": [
      {
        "title": "CPU使用率",
        "type": "graph",
        "targets": [
          {
            "expr": "100 - (avg by(instance) (rate(node_cpu_seconds_total{mode=\"idle\"}[5m])) * 100)",
            "legendFormat": "{{instance}}"
          }
        ]
      },
      {
        "title": "内存使用率",
        "type": "graph",
        "targets": [
          {
            "expr": "(node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100",
            "legendFormat": "{{instance}}"
          }
        ]
      }
    ]
  }
}
```

## 6. 最佳实践

### 6.1 健康检查策略

#### 6.1.1 检查频率
- **实时检查**: 关键服务（每1分钟）
- **频繁检查**: 重要服务（每5分钟）
- **常规检查**: 一般服务（每15分钟）
- **深度检查**: 全面检查（每天1次）

#### 6.1.2 检查范围
1. **基础层**: 硬件、网络、操作系统
2. **中间件层**: 数据库、缓存、消息队列
3. **应用层**: 业务应用、API服务
4. **监控层**: 监控系统自身

### 6.2 响应流程

#### 6.2.1 告警响应
1. **接收告警**: 监控系统发出告警
2. **确认告警**: 确认告警真实性
3. **分类定级**: 确定告警级别和影响范围
4. **分配处理**: 分配给相应团队或个人
5. **处理解决**: 执行解决方案
6. **验证关闭**: 验证解决效果并关闭告警
7. **总结复盘**: 分析根本原因并记录

#### 6.2.2 故障处理
1. **快速恢复**: 优先恢复服务
2. **问题定位**: 定位问题根本原因
3. **根本解决**: 实施长期解决方案
4. **预防措施**: 制定预防措施

### 6.3 持续改进

#### 6.3.1 指标优化
- 定期评估监控指标
- 优化告警阈值
- 减少误报和漏报
- 提高监控覆盖率

#### 6.3.2 流程优化
- 简化检查流程
- 自动化常规检查
- 优化响应流程
- 完善文档和培训

## 7. 总结

本健康检查指南为Sprint 27+1测试环境提供了完整的监控和检查方案，包括：

1. **全面的检查脚本**: 覆盖所有关键服务
2. **详细的配置指南**: 提供标准化配置
3. **自动化监控**: 集成Prometheus和Grafana
4. **最佳实践**: 基于行业标准的操作流程
5. **持续改进**: 支持持续优化和演进

通过实施本指南，可以确保测试环境的稳定性、可靠性和可维护性，为开发和测试工作提供坚实的基础。

---

**文档版本**: 1.0  
**最后更新**: 2026-04-27  
**作者**: AI-Ready团队  
**适用环境**: Sprint 27+1测试环境  

> **重要提示**: 
> 1. 定期更新检查脚本和配置
> 2. 根据实际环境调整阈值
> 3. 建立定期评审机制
> 4. 培训团队成员掌握健康检查技能