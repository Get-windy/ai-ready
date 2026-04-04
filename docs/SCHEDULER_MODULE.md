# AI-Ready 定时任务调度模块文档

## 版本信息
- 版本：v1.1.0
- 更新日期：2026-04-04
- 开发人员：team-member

## 一、模块架构

```
cn.aiedge.scheduler/
├── config/                         # 配置类
│   ├── QuartzConfig.java          # Quartz配置
│   ├── SchedulerConfig.java       # 调度器配置
│   └── TaskExecutionListener.java # 执行监听器
├── controller/                     # 控制器
│   ├── JobSchedulerController.java
│   ├── TaskSchedulerController.java
│   └── TaskMonitorController.java # 监控控制器
├── entity/                         # 实体类
│   ├── ScheduledTask.java         # 定时任务
│   └── TaskExecuteLog.java        # 执行日志
├── job/                            # 任务定义
│   ├── ScheduledJob.java          # 任务接口
│   ├── QuartzJobWrapper.java      # Quartz包装器
│   └── SampleJob.java             # 示例任务
├── mapper/                         # 数据访问
│   ├── JobConfigMapper.java
│   └── JobLogMapper.java
├── model/                          # 模型类
│   ├── JobConfig.java
│   └── JobLog.java
├── monitor/                        # 监控模块
│   └── TaskExecutionTracker.java  # 执行追踪器
├── retry/                          # 重试模块
│   ├── RetryPolicy.java           # 重试策略
│   └── TaskRetryManager.java      # 重试管理器
└── service/                        # 服务层
    ├── TaskSchedulerService.java
    └── impl/TaskSchedulerServiceImpl.java
```

## 二、核心功能

### 2.1 任务调度
- 基于Quartz实现
- 支持Cron表达式
- 支持任务启动/暂停/恢复/触发
- 任务持久化

### 2.2 重试机制
- 多种重试策略：固定间隔、线性递增、指数退避、随机间隔
- 可配置最大重试次数
- 可配置可重试异常类型
- 重试计数持久化

### 2.3 执行日志
- 完整的执行记录
- 执行时间统计
- 异常信息记录
- 日志清理机制

### 2.4 监控统计
- 实时执行统计
- 成功率计算
- 运行状态监控
- 健康检查

## 三、API接口

### 3.1 任务管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/scheduler/tasks | 创建任务 |
| PUT | /api/scheduler/tasks/{id} | 更新任务 |
| DELETE | /api/scheduler/tasks/{id} | 删除任务 |
| GET | /api/scheduler/tasks/{id} | 获取任务详情 |
| GET | /api/scheduler/tasks | 获取所有任务 |
| POST | /api/scheduler/tasks/{id}/start | 启动任务 |
| POST | /api/scheduler/tasks/{id}/pause | 暂停任务 |
| POST | /api/scheduler/tasks/{id}/resume | 恢复任务 |
| POST | /api/scheduler/tasks/{id}/trigger | 立即执行 |

### 3.2 任务监控

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/scheduler/monitor/summary | 执行摘要 |
| GET | /api/scheduler/monitor/global-stats | 全局统计 |
| GET | /api/scheduler/monitor/task-stats/{taskId} | 任务统计 |
| GET | /api/scheduler/monitor/running | 运行中任务 |
| GET | /api/scheduler/monitor/scheduler-status | 调度器状态 |
| GET | /api/scheduler/monitor/health | 健康检查 |

### 3.3 重试管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/scheduler/monitor/retry/{taskId}/policy | 获取重试策略 |
| PUT | /api/scheduler/monitor/retry/{taskId}/policy | 设置重试策略 |
| GET | /api/scheduler/monitor/retry/{taskId}/count | 获取重试计数 |
| POST | /api/scheduler/monitor/retry/{taskId}/reset | 重置重试计数 |
| POST | /api/scheduler/monitor/retry/{taskId}/manual | 手动触发重试 |

## 四、使用示例

### 4.1 创建定时任务

```java
// 实现ScheduledJob接口
@Component
public class DataSyncJob implements ScheduledJob {
    
    @Override
    public String execute(Map<String, Object> params) throws Exception {
        String source = (String) params.get("source");
        // 执行同步逻辑
        return "同步完成: " + source + " 条数据";
    }
    
    @Override
    public String getTaskName() {
        return "数据同步任务";
    }
}

// 创建任务
ScheduledTask task = ScheduledTask.builder()
    .taskName("数据同步任务")
    .taskGroup("sync")
    .taskClass("com.example.DataSyncJob")
    .cronExpression("0 0 2 * * ?")  // 每天凌晨2点执行
    .taskParams("{\"source\":\"erp\"}")
    .status(ScheduledTask.STATUS_RUNNING)
    .build();

taskSchedulerService.createTask(task);
```

### 4.2 配置重试策略

```java
// 指数退避策略
RetryPolicy policy = RetryPolicy.builder()
    .enabled(true)
    .maxRetries(5)
    .intervalStrategy(RetryPolicy.RetryIntervalStrategy.EXPONENTIAL)
    .initialIntervalMs(1000)
    .multiplier(2.0)
    .maxIntervalMs(60000)
    .retryableExceptions(new String[]{"IOException", "TimeoutException"})
    .build();

retryManager.setRetryPolicy(taskId, policy);
```

### 4.3 Cron表达式说明

```
┌───────────── 秒 (0-59)
│ ┌───────────── 分钟 (0-59)
│ │ ┌───────────── 小时 (0-23)
│ │ │ ┌───────────── 日期 (1-31)
│ │ │ │ ┌───────────── 月份 (1-12)
│ │ │ │ │ ┌───────────── 星期几 (0-6, 0=周日)
│ │ │ │ │ │
* * * * * *

示例:
"0 0 12 * * ?"    每天12:00
"0 30 10 * * ?"   每天10:30
"0 0 2 * * ?"     每天凌晨2:00
"0 0/30 * * * ?"  每30分钟
"0 0 9-17 * * 1-5" 工作日9-17点整点
```

## 五、重试策略详解

### 5.1 固定间隔
每次重试间隔固定时间。

```java
RetryPolicy.builder()
    .intervalStrategy(RetryIntervalStrategy.FIXED)
    .fixedIntervalMs(5000)  // 每次间隔5秒
    .build();
```

### 5.2 线性递增
间隔时间线性增加。

```java
RetryPolicy.builder()
    .intervalStrategy(RetryIntervalStrategy.LINEAR)
    .fixedIntervalMs(1000)  // 基础间隔1秒
    .build();
// 第1次重试: 1秒, 第2次: 2秒, 第3次: 3秒...
```

### 5.3 指数退避
间隔时间指数增长，适合避免系统过载。

```java
RetryPolicy.builder()
    .intervalStrategy(RetryIntervalStrategy.EXPONENTIAL)
    .initialIntervalMs(1000)  // 初始1秒
    .multiplier(2.0)          // 每次翻倍
    .maxIntervalMs(60000)     // 最大60秒
    .build();
// 第1次: 1秒, 第2次: 2秒, 第3次: 4秒, 第4次: 8秒...
```

### 5.4 随机间隔
随机间隔避免多个任务同时重试。

```java
RetryPolicy.builder()
    .intervalStrategy(RetryIntervalStrategy.RANDOM)
    .fixedIntervalMs(1000)  // 基础间隔
    .maxIntervalMs(5000)    // 最大间隔
    .build();
// 随机返回1-5秒之间的值
```

## 六、监控指标

### 6.1 任务统计
- 成功次数
- 失败次数
- 重试次数
- 平均执行时长
- 最大/最小执行时长
- 成功率

### 6.2 全局统计
- 总执行次数
- 成功总数
- 失败总数
- 重试总数
- 当前运行任务数
- 全局成功率

### 6.3 健康检查

```json
{
  "status": "UP",
  "schedulerRunning": true,
  "totalExecutions": 1523,
  "successRate": 98.5,
  "currentlyRunning": 3,
  "healthy": true,
  "issues": []
}
```

## 七、数据库表结构

### sys_scheduled_task（定时任务表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 任务ID |
| task_name | VARCHAR(100) | 任务名称 |
| task_group | VARCHAR(50) | 任务分组 |
| task_class | VARCHAR(255) | 任务类名 |
| cron_expression | VARCHAR(50) | Cron表达式 |
| task_params | TEXT | 任务参数 |
| status | INT | 状态（0暂停/1运行） |
| last_execute_time | DATETIME | 上次执行时间 |
| next_execute_time | DATETIME | 下次执行时间 |
| execute_count | BIGINT | 执行次数 |
| fail_count | BIGINT | 失败次数 |

### sys_task_execute_log（执行日志表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 日志ID |
| task_id | BIGINT | 任务ID |
| task_name | VARCHAR(100) | 任务名称 |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| duration | BIGINT | 耗时（毫秒） |
| status | INT | 状态 |
| result_message | TEXT | 结果消息 |
| exception_info | TEXT | 异常信息 |
| server_ip | VARCHAR(50) | 服务器IP |

## 八、配置说明

```yaml
# application.yml
spring:
  quartz:
    job-store-type: jdbc          # 持久化存储
    jdbc:
      initialize-schema: never
    properties:
      org:
        quartz:
          scheduler:
            instanceName: AI-Ready-Scheduler
            instanceId: AUTO
          threadPool:
            threadCount: 10
          jobStore:
            class: org.quartz.impl.jdbcjobstore.JobStoreTX
            driverDelegateClass: org.quartz.impl.jdbcjobstore.StdJDBCDelegate

# 调度器配置
scheduler:
  retry:
    enabled: true
    default-max-retries: 3
    default-interval-ms: 5000
  log:
    retention-days: 30
    clean-enabled: true
```

## 九、最佳实践

1. **任务幂等性**：任务执行应支持重复执行
2. **超时处理**：长时间任务应设置超时
3. **异常处理**：捕获并记录所有异常
4. **资源清理**：任务结束清理临时资源
5. **日志记录**：记录关键执行步骤
6. **监控告警**：配置失败告警通知

---
*文档更新时间：2026-04-04*
