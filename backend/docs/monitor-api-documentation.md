# Sprint 27+1 测试环境监控数据API服务文档

## 概述

本文档描述了为Sprint 27+1测试环境开发的监控数据API服务，提供统一的监控数据查询、分析和告警管理接口。

## API架构

### 基础路径

所有监控API的基础路径为：`/api/monitor`

### 模块划分

1. **基础设施监控** (`/api/monitor/infrastructure`)
   - 服务器、网络、存储等基础设施监控
   
2. **服务健康监控** (`/api/monitor/health`)
   - 服务健康状态、依赖服务状态监控
   
3. **性能指标分析** (`/api/monitor/performance`)
   - 性能指标聚合、分析和趋势预测
   
4. **告警管理** (`/api/monitor/alerts`)
   - 告警管理、通知配置和告警历史

## API端点详情

### 1. 基础设施监控 API

#### 1.1 服务器信息
```
GET /api/monitor/infrastructure/server/info
```
返回服务器基本信息，包括主机名、IP地址、操作系统、Java版本等。

#### 1.2 CPU详情
```
GET /api/monitor/infrastructure/cpu/detail
```
返回CPU详细信息，包括核心数、负载、使用率等。

#### 1.3 磁盘信息
```
GET /api/monitor/infrastructure/disk/info
```
返回磁盘信息，包括总空间、已用空间、可用空间、使用率等。

#### 1.4 网络接口
```
GET /api/monitor/infrastructure/network/interfaces
```
返回网络接口信息，包括接口名称、IP地址、状态等。

#### 1.5 网络统计
```
GET /api/monitor/infrastructure/network/stats
```
返回网络统计信息。

#### 1.6 进程信息
```
GET /api/monitor/infrastructure/process/info
```
返回进程信息，包括进程ID、启动时间、运行时间等。

#### 1.7 环境信息
```
GET /api/monitor/infrastructure/environment
```
返回环境信息，包括系统环境变量和系统属性。

### 2. 服务健康监控 API

#### 2.1 综合健康状态
```
GET /api/monitor/health/status
```
返回服务综合健康状态，包括各组件状态和健康分数。

#### 2.2 数据库健康
```
GET /api/monitor/health/database
```
返回数据库健康状态，包括连接状态、响应时间等。

#### 2.3 JVM健康
```
GET /api/monitor/health/jvm
```
返回JVM健康状态，包括堆内存、非堆内存、线程状态等。

#### 2.4 磁盘健康
```
GET /api/monitor/health/disk
```
返回磁盘健康状态。

#### 2.5 依赖服务健康
```
GET /api/monitor/health/dependencies
```
返回依赖服务（数据库、Redis、消息队列等）健康状态。

#### 2.6 就绪状态（K8s Readiness Probe）
```
GET /api/monitor/health/ready
```
返回服务就绪状态，用于Kubernetes Readiness Probe。

#### 2.7 存活状态（K8s Liveness Probe）
```
GET /api/monitor/health/live
```
返回服务存活状态，用于Kubernetes Liveness Probe。

### 3. 性能指标分析 API

#### 3.1 实时性能指标
```
GET /api/monitor/performance/realtime
```
返回实时性能指标，包括CPU、内存、GC、线程、类加载等。

#### 3.2 性能聚合统计
```
GET /api/monitor/performance/aggregate?windowMinutes=60
```
返回指定时间窗口内的性能指标聚合统计。

参数：
- `windowMinutes`: 时间窗口（分钟），默认60

#### 3.3 性能趋势
```
GET /api/monitor/performance/trend/{metricType}?minutes=60
```
返回指定指标的趋势数据。

参数：
- `metricType`: 指标类型（如 cpu.usage, memory.usage）
- `minutes`: 时间范围（分钟），默认60

#### 3.4 性能预测
```
GET /api/monitor/performance/predict/{metricType}?predictMinutes=30
```
返回指定指标的性能预测。

参数：
- `metricType`: 指标类型
- `predictMinutes`: 预测时间（分钟），默认30

#### 3.5 性能瓶颈分析
```
GET /api/monitor/performance/bottleneck
```
返回性能瓶颈分析结果。

#### 3.6 性能对比
```
GET /api/monitor/performance/compare?window1Minutes=60&window2Minutes=60&offsetMinutes=60
```
返回两个时间窗口的性能指标对比。

参数：
- `window1Minutes`: 窗口1时长（分钟）
- `window2Minutes`: 窗口2时长（分钟）
- `offsetMinutes`: 窗口2偏移（分钟）

### 4. 告警管理 API

#### 4.1 创建告警规则
```
POST /api/monitor/alerts/rules
Content-Type: application/json

{
  "name": "CPU High Usage",
  "description": "CPU使用率超过80%",
  "metric": "cpu.usage",
  "operator": ">",
  "threshold": 80,
  "duration": 300,
  "severity": "warning",
  "notification": {
    "channels": ["email", "webhook"],
    "targets": ["admin@example.com"]
  }
}
```

#### 4.2 获取告警规则列表
```
GET /api/monitor/alerts/rules?metric=cpu.usage&severity=warning&enabled=true
```

参数：
- `metric`: 按指标过滤
- `severity`: 按严重程度过滤
- `enabled`: 按启用状态过滤

#### 4.3 获取告警规则详情
```
GET /api/monitor/alerts/rules/{ruleId}
```

#### 4.4 更新告警规则
```
PUT /api/monitor/alerts/rules/{ruleId}
Content-Type: application/json

{
  "name": "Updated Rule",
  "threshold": 85
}
```

#### 4.5 删除告警规则
```
DELETE /api/monitor/alerts/rules/{ruleId}
```

#### 4.6 启用/禁用告警规则
```
POST /api/monitor/alerts/rules/{ruleId}/enable
POST /api/monitor/alerts/rules/{ruleId}/disable
```

#### 4.7 获取告警历史
```
GET /api/monitor/alerts/history?hours=24&severity=warning&limit=50
```

参数：
- `hours`: 时间范围（小时），默认24
- `severity`: 按严重程度过滤
- `limit`: 返回数量限制，默认50

#### 4.8 确认告警
```
POST /api/monitor/alerts/{alertId}/acknowledge?acknowledgedBy=user
```

#### 4.9 解决告警
```
POST /api/monitor/alerts/{alertId}/resolve?resolvedBy=user&resolution=fixed
```

#### 4.10 获取告警统计
```
GET /api/monitor/alerts/statistics?hours=24
```

#### 4.11 配置通知渠道
```
POST /api/monitor/alerts/notification/config
Content-Type: application/json

{
  "channel": "email",
  "enabled": true,
  "config": {
    "smtpHost": "smtp.example.com",
    "smtpPort": 587
  }
}
```

#### 4.12 测试通知
```
POST /api/monitor/alerts/notification/test?channel=email
```

### 5. 统一入口 API

#### 5.1 API概览
```
GET /api/monitor/info
```
返回所有监控API的概览信息。

#### 5.2 仪表盘数据
```
GET /api/monitor/dashboard
```
返回监控仪表盘数据汇总。

#### 5.3 批量查询
```
POST /api/monitor/batch
Content-Type: application/json

["system", "health", "performance", "alerts"]
```
批量查询多种监控数据。

#### 5.4 导出数据
```
GET /api/monitor/export?format=json&hours=24
```
导出监控数据。

参数：
- `format`: 导出格式（json/csv），默认json
- `hours`: 时间范围（小时），默认24

## 数据模型

### 系统指标 (SystemMetrics)

```java
{
  "collectTime": "2026-04-29T10:30:00",
  "cpuUsage": 45.5,
  "cpuCores": 8,
  "totalMemory": 16384,
  "usedMemory": 8192,
  "memoryUsage": 50.0,
  "totalDisk": 500,
  "usedDisk": 250,
  "diskUsage": 50.0,
  "threadCount": 150,
  "systemStatus": "healthy"
}
```

### 告警规则 (AlertRule)

```java
{
  "id": 1,
  "name": "CPU High Usage",
  "description": "CPU使用率超过80%",
  "metric": "cpu.usage",
  "operator": ">",
  "threshold": 80.0,
  "duration": 300,
  "severity": "warning",
  "enabled": true,
  "notifyChannels": ["email", "webhook"],
  "notifyTargets": ["admin@example.com"],
  "createTime": "2026-04-29T10:00:00",
  "updateTime": "2026-04-29T10:00:00"
}
```

### 告警记录

```java
{
  "id": "alert-001",
  "ruleId": 1,
  "ruleName": "CPU High Usage",
  "metric": "cpu.usage",
  "value": 85.5,
  "threshold": 80.0,
  "severity": "warning",
  "message": "warning: cpu.usage = 85.50 (threshold: > 80.00)",
  "timestamp": "2026-04-29T10:30:00",
  "acknowledged": false,
  "resolved": false
}
```

## 错误处理

所有API返回统一的错误格式：

```json
{
  "success": false,
  "message": "Error description",
  "timestamp": "2026-04-29T10:30:00"
}
```

HTTP状态码：
- 200: 成功
- 400: 请求参数错误
- 404: 资源不存在
- 500: 服务器内部错误

## 安全说明

1. 所有API应配置适当的权限控制
2. 敏感操作（如删除告警规则）需要管理员权限
3. 建议使用API密钥或OAuth2进行认证

## 性能说明

1. 实时指标查询响应时间 < 100ms
2. 历史数据查询支持分页
3. 告警历史默认保留7天

## 部署信息

- **环境**: Sprint 27+1 测试环境
- **服务**: ai-ready-core-api
- **版本**: 1.0.0
- **创建时间**: 2026-04-29

## 相关文件

- 控制器代码: `backend/core/api/core-api/src/main/java/cn/aiedge/monitor/controller/`
- 服务接口: `backend/core/api/core-api/src/main/java/cn/aiedge/monitor/service/`
- 数据模型: `backend/core/api/core-api/src/main/java/cn/aiedge/monitor/model/`

---

**文档版本**: 1.0.0  
**最后更新**: 2026-04-29  
**维护团队**: AI-Ready Team
