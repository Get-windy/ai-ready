# AI-Ready 定时任务模块文档

## 概述

本模块提供完整的定时任务管理能力，基于 Quartz 调度器实现。

## 模块结构

```
cn.aiedge.scheduler
├── config/
│   └── SchedulerConfig.java      # 调度器配置
├── controller/
│   └── JobSchedulerController.java # 任务管理接口
├── mapper/
│   ├── JobConfigMapper.java      # 任务配置Mapper
│   └── JobLogMapper.java         # 执行日志Mapper
├── model/
│   ├── JobConfig.java            # 任务配置实体
│   └── JobLog.java               # 执行日志实体
└── service/
    └── JobSchedulerService.java  # 调度服务
```

## API接口

### 任务管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/scheduler | 创建定时任务 |
| PUT | /api/scheduler | 更新定时任务 |
| DELETE | /api/scheduler/{jobId} | 删除定时任务 |
| GET | /api/scheduler/{jobId} | 获取任务详情 |
| GET | /api/scheduler/list | 获取所有任务 |
| GET | /api/scheduler/page | 分页查询任务 |

### 任务控制

| 方法 | 路径 | 说明 |
|------|------|------|
| PUT | /api/scheduler/pause/{jobId} | 暂停任务 |
| PUT | /api/scheduler/resume/{jobId} | 恢复任务 |
| POST | /api/scheduler/run/{jobId} | 立即执行一次 |

### 日志管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/scheduler/log/page | 分页查询日志 |
| DELETE | /api/scheduler/log/clean | 清理日志 |
| GET | /api/scheduler/stats | 获取任务统计 |

## 快速开始

### 1. 创建定时任务

```json
POST /api/scheduler
{
  "jobName": "订单超时检查",
  "jobGroup": "ORDER",
  "description": "每5分钟检查超时订单",
  "beanName": "orderTaskService",
  "methodName": "checkTimeout",
  "cronExpression": "0 */5 * * * ?",
  "status": 1,
  "concurrent": 0,
  "misfirePolicy": 2
}
```

### 2. 任务执行类

```java
@Service("orderTaskService")
public class OrderTaskService {
    
    public void checkTimeout(String params) {
        // 检查超时订单逻辑
        List<Order> timeoutOrders = orderMapper.selectTimeout();
        for (Order order : timeoutOrders) {
            // 处理超时订单
        }
    }
}
```

### 3. Cron表达式说明

```
┌───────────── 秒 (0-59)
│ ┌───────────── 分钟 (0-59)
│ │ ┌───────────── 小时 (0-23)
│ │ │ ┌───────────── 日期 (1-31)
│ │ │ │ ┌───────────── 月份 (1-12)
│ │ │ │ │ ┌───────────── 星期 (0-7, 0和7都是周日)
│ │ │ │ │ │
* * * * * ?

示例：
0 */5 * * * ?    每5分钟执行一次
0 0 2 * * ?      每天凌晨2点执行
0 0 0 1 * ?      每月1日0点执行
```

## 任务配置字段

| 字段 | 说明 | 必填 |
|------|------|------|
| jobName | 任务名称 | 是 |
| jobGroup | 任务分组 | 是 |
| description | 任务描述 | 否 |
| beanName | 执行类名（Spring Bean名称） | 是 |
| methodName | 执行方法名 | 是 |
| params | 方法参数 | 否 |
| cronExpression | Cron表达式 | 是 |
| status | 状态：0-暂停，1-正常 | 是 |
| concurrent | 是否并发：0-禁止，1-允许 | 否 |
| misfirePolicy | 错过策略：1-立即，2-执行一次，3-放弃 | 否 |

## 最佳实践

1. **任务幂等性**：确保任务可重复执行不会产生副作用
2. **异常处理**：任务中捕获并记录异常，避免影响后续执行
3. **执行时间**：任务执行时间不宜过长，超过间隔应考虑并发设置
4. **日志清理**：定期清理历史日志，避免数据积累
5. **监控告警**：对失败任务配置告警通知

## 数据库表

```sql
-- 任务配置表
CREATE TABLE sys_job (
    id BIGINT PRIMARY KEY,
    job_name VARCHAR(100) NOT NULL,
    job_group VARCHAR(50) NOT NULL,
    bean_name VARCHAR(100) NOT NULL,
    method_name VARCHAR(100) NOT NULL,
    params VARCHAR(500),
    cron_expression VARCHAR(100) NOT NULL,
    status INT DEFAULT 1,
    concurrent INT DEFAULT 0,
    misfire_policy INT DEFAULT 2,
    next_time DATETIME,
    prev_time DATETIME,
    create_time DATETIME,
    update_time DATETIME,
    deleted INT DEFAULT 0
);

-- 执行日志表
CREATE TABLE sys_job_log (
    id BIGINT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    job_name VARCHAR(100),
    job_group VARCHAR(50),
    bean_name VARCHAR(100),
    method_name VARCHAR(100),
    params VARCHAR(500),
    status INT,
    message VARCHAR(500),
    exception TEXT,
    start_time DATETIME,
    end_time DATETIME,
    duration BIGINT,
    server_ip VARCHAR(50),
    create_time DATETIME
);
```