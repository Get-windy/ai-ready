# AI-Ready 服务健康检查工具

## 概述

本工具用于 AI-Ready 服务的日常健康巡检，支持数据库、Redis、API服务和定时任务的状态检查。

## 文件结构

```
health-check/
├── health-check.ps1           # 主检查脚本 (PowerShell)
├── health-check-config.json   # 配置文件
├── health-check-report-template.md  # 报告模板
├── README.md                  # 本文件
└── reports/                   # 检查报告输出目录
```

## 使用方法

### 基本用法

```powershell
# 执行健康检查
.\health-check.ps1

# 指定配置文件
.\health-check.ps1 -ConfigFile "custom-config.json"

# 详细输出模式
.\health-check.ps1 -Verbose $true
```

### 参数说明

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| ConfigFile | string | health-check-config.json | 配置文件路径 |
| ReportDir | string | reports | 报告输出目录 |
| Verbose | bool | false | 是否输出详细信息 |

## 检查项说明

### 1. 数据库连接检查

- PostgreSQL 端口可达性检查
- 数据库连接池状态检查
- 简单查询测试

### 2. Redis缓存检查

- Redis 端口可达性检查
- PING 命令响应测试
- 内存使用状态检查

### 3. API服务检查

- 服务端口 8080 可达性检查
- `/actuator/health` 端点健康检查
- API组件状态检查

### 4. 定时任务检查

- Quartz 定时任务状态检查
- 延迟任务统计
- 失败任务统计

## 输出示例

```
========================================
        AI-Ready 健康检查报告
========================================
检查时间: 2026-04-01 14:15:00
总体状态: healthy

检查项统计:
  总计: 10
  通过: 9
  失败: 0
  警告: 1

详细检查结果:
  [PASS] 数据库连接: PostgreSQL端口5432可访问
  [PASS] Redis连接: Redis端口6379可访问
  [PASS] API端口: API端口8080可访问
  [WARN] 定时任务延迟: 有1个任务延迟
========================================
```

## 退出码

| 退出码 | 说明 |
|--------|------|
| 0 | 健康状态 (所有检查通过) |
| 1 | 降级状态 (存在警告项) |
| 2 | 不健康状态 (存在失败项) |

## 定时执行

可配置 Windows 任务计划程序定时执行：

```powershell
# 创建每日健康检查任务
$action = New-ScheduledTaskAction -Execute "PowerShell.exe" -Argument "-File I:\AI-Ready\health-check\health-check.ps1"
$trigger = New-ScheduledTaskTrigger -Daily -At 8am
Register-ScheduledTask -TaskName "AI-Ready-HealthCheck" -Action $action -Trigger $trigger
```

## 与监控系统集成

### Prometheus Pushgateway

检查结果可通过 Pushgateway 推送到 Prometheus：

```powershell
# 推送指标示例
$metrics = @"
ai_ready_health_status{check="database"} 1
ai_ready_health_status{check="redis"} 1
ai_ready_health_status{check="api"} 1
"@
Invoke-RestMethod -Method Post -Uri "http://prometheus:9091/metrics/job/ai-ready-health" -Body $metrics
```

### 飞书告警

检查失败时可发送飞书告警：

```powershell
# 发送飞书消息
$body = @{
    msg_type = "text"
    content = @{ text = "AI-Ready健康检查异常，请查看报告" }
} | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri $WebhookUrl -Body $body -ContentType "application/json"
```

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2026-04-01 | 初始版本，支持数据库/Redis/API/定时任务检查 |

## 作者

devops-engineer (运维工程师)