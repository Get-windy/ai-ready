# AI-Ready 服务健康检查报告模板

**报告编号**: {{REPORT_ID}}  
**检查时间**: {{TIMESTAMP}}  
**执行者**: {{EXECUTOR}}  
**总体状态**: {{STATUS}}

---

## 一、检查概览

| 检查项 | 状态 | 结果 |
|--------|------|------|
| 数据库连接 | {{DB_STATUS}} | {{DB_MESSAGE}} |
| Redis缓存 | {{REDIS_STATUS}} | {{REDIS_MESSAGE}} |
| API服务 | {{API_STATUS}} | {{API_MESSAGE}} |
| 定时任务 | {{TASK_STATUS}} | {{TASK_MESSAGE}} |

**统计汇总**:
- 总检查项: {{TOTAL}}
- 通过: {{PASSED}}
- 失败: {{FAILED}}
- 警告: {{WARNING}}

---

## 二、数据库检查详情

### 2.1 PostgreSQL连接状态

| 指标 | 值 | 状态 |
|------|------|------|
| 主机 | {{DB_HOST}} | - |
| 端口 | {{DB_PORT}} | {{DB_PORT_STATUS}} |
| 数据库 | {{DB_NAME}} | - |
| 连接池状态 | {{POOL_STATUS}} | {{POOL_STATUS_RESULT}} |

### 2.2 数据库性能指标

| 指标 | 当前值 | 阈值 | 状态 |
|------|--------|------|------|
| 活跃连接数 | {{ACTIVE_CONNECTIONS}} | <20 | {{CONN_STATUS}} |
| 等待连接数 | {{WAITING_CONNECTIONS}} | <5 | {{WAIT_STATUS}} |
| 查询响应时间 | {{QUERY_TIME}}ms | <100ms | {{QUERY_STATUS}} |
| 数据库大小 | {{DB_SIZE}} | - | - |

---

## 三、Redis缓存检查详情

### 3.1 Redis连接状态

| 指标 | 值 | 状态 |
|------|------|------|
| 主机 | {{REDIS_HOST}} | - |
| 端口 | {{REDIS_PORT}} | {{REDIS_PORT_STATUS}} |
| 版本 | {{REDIS_VERSION}} | - |
| 运行模式 | {{REDIS_MODE}} | - |

### 3.2 Redis性能指标

| 指标 | 当前值 | 阈值 | 状态 |
|------|--------|------|------|
| 内存使用 | {{REDIS_MEMORY}} | <80% | {{MEM_STATUS}} |
| 键数量 | {{KEY_COUNT}} | - | - |
| 连接客户端 | {{CLIENT_COUNT}} | <100 | {{CLIENT_STATUS}} |
| 命令处理速率 | {{CMD_RATE}}/s | - | - |
| 缓存命中率 | {{CACHE_HIT_RATE}}% | >90% | {{HIT_STATUS}} |

---

## 四、API服务检查详情

### 4.1 API端口状态

| 指标 | 值 | 状态 |
|------|------|------|
| 服务端口 | 8080 | {{API_PORT_STATUS}} |
| 响应时间 | {{API_RESPONSE_TIME}}ms | {{TIME_STATUS}} |
| 健康检查端点 | /actuator/health | {{HEALTH_ENDPOINT_STATUS}} |

### 4.2 API组件状态

| 组件 | 状态 | 详情 |
|------|------|------|
| 数据库组件 | {{DB_COMP_STATUS}} | {{DB_COMP_DETAIL}} |
| Redis组件 | {{REDIS_COMP_STATUS}} | {{REDIS_COMP_DETAIL}} |
| 磁盘空间 | {{DISK_COMP_STATUS}} | {{DISK_COMP_DETAIL}} |
| 服务发现 | {{DISCOVERY_STATUS}} | {{DISCOVERY_DETAIL}} |

### 4.3 API性能指标

| 指标 | 当前值 | 阈值 | 状态 |
|------|--------|------|------|
| 平均响应时间 | {{AVG_RESPONSE_TIME}}ms | <200ms | {{AVG_TIME_STATUS}} |
| P95响应时间 | {{P95_RESPONSE_TIME}}ms | <500ms | {{P95_STATUS}} |
| 错误率 | {{ERROR_RATE}}% | <1% | {{ERROR_STATUS}} |
| QPS | {{QPS}} | - | - |

---

## 五、定时任务检查详情

### 5.1 Quartz任务状态

| 任务名称 | 状态 | 下次执行时间 | 上次执行结果 |
|----------|------|--------------|--------------|
| {{TASK_1_NAME}} | {{TASK_1_STATUS}} | {{TASK_1_NEXT}} | {{TASK_1_LAST}} |
| {{TASK_2_NAME}} | {{TASK_2_STATUS}} | {{TASK_2_NEXT}} | {{TASK_2_LAST}} |
| {{TASK_3_NAME}} | {{TASK_3_STATUS}} | {{TASK_3_NEXT}} | {{TASK_3_LAST}} |

### 5.2 任务执行统计

| 指标 | 当前值 | 阈值 | 状态 |
|------|--------|------|------|
| 正常运行任务 | {{RUNNING_TASKS}} | - | - |
| 延迟任务数 | {{DELAYED_TASKS}} | 0 | {{DELAYED_STATUS}} |
| 失败任务数 | {{FAILED_TASKS}} | 0 | {{FAILED_TASK_STATUS}} |
| 平均执行时间 | {{AVG_EXEC_TIME}}ms | - | - |

---

## 六、异常记录

### 6.1 发现的问题

{{#if HAS_ISSUES}}
| 问题编号 | 问题描述 | 严重级别 | 发现时间 | 建议措施 |
|----------|----------|----------|----------|----------|
{{#each ISSUES}}
| {{ISSUE_ID}} | {{ISSUE_DESC}} | {{ISSUE_LEVEL}} | {{ISSUE_TIME}} | {{ISSUE_ACTION}} |
{{/each}}
{{else}}
本次检查未发现异常问题。
{{/if}}

### 6.2 历史问题跟踪

| 问题编号 | 状态 | 处理进度 | 备注 |
|----------|------|----------|------|
{{#each HISTORICAL_ISSUES}}
| {{HIST_ID}} | {{HIST_STATUS}} | {{HIST_PROGRESS}} | {{HIST_NOTE}} |
{{/each}}

---

## 七、建议措施

### 7.1 立即处理

{{#each IMMEDIATE_ACTIONS}}
- **{{ACTION_ITEM}}**: {{ACTION_DESC}}
{{/each}}

### 7.2 近期优化

{{#each RECENT_OPTIMIZATIONS}}
- **{{OPT_ITEM}}**: {{OPT_DESC}}
{{/each}}

### 7.3 长期规划

{{#each LONG_TERM_PLANS}}
- **{{PLAN_ITEM}}**: {{PLAN_DESC}}
{{/each}}

---

## 八、检查结论

**总体评价**: {{OVERALL_EVALUATION}}

**健康度评分**: {{HEALTH_SCORE}}/100

**下一步行动**: {{NEXT_ACTIONS}}

---

## 附录

### A. 检查脚本版本

- 脚本名称: health-check.ps1
- 版本: 1.0
- 作者: devops-engineer
- 创建日期: 2026-04-01

### B. 配置参数

```json
{
  "Database": {
    "Host": "localhost",
    "Port": 5432,
    "Database": "devdb"
  },
  "Redis": {
    "Host": "localhost",
    "Port": 6379
  },
  "API": {
    "BaseUrl": "http://localhost:8080",
    "HealthEndpoint": "/actuator/health"
  }
}
```

### C. 参考文档

- AI-Ready运维手册: docs/devops/OPERATION_GUIDE.md
- 监控告警配置: docs/devops/MONITORING_GUIDE.md
- 性能优化指南: docs/devops/PERFORMANCE_TUNING.md

---

**报告生成时间**: {{GENERATED_TIME}}  
**报告有效期**: 24小时  
**下次检查建议时间**: {{NEXT_CHECK_TIME}}