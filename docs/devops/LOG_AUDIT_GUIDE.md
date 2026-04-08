# AI-Ready 日志审计方案

> **项目**: 智企连·AI-Ready  
> **版本**: v1.0  
> **最后更新**: 2026-04-08  
> **维护者**: devops-engineer

---

## 一、日志审计需求分析

### 1.1 审计目标

| 目标 | 描述 | 优先级 |
|------|------|--------|
| 操作审计 | 所有用户操作留痕，可追溯 | P0 |
| 安全审计 | 安全事件实时发现、响应 | P0 |
| 性能审计 | 关键接口性能统计分析 | P1 |
| 合规审计 | 满足等保/ HIPAA/ GDPR审计要求 | P1 |

### 1.2 审计范围

| 类别 | 审计内容 | 保留期 |
|------|----------|--------|
| 用户操作 | 登录/登出、CRUD操作、权限变更 | 180天 |
| 系统事件 | 服务启停、配置变更、备份恢复 | 365天 |
| 安全事件 | 认证失败、SQL注入、权限越界 | 730天 |
| API调用 | All API调用记录（调用方、参数、结果） | 90天 |
| 数据变更 | 数据库核心表变更（增删改） | 180天 |

---

## 二、日志采集架构

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                    AI-Ready Log Audit Architecture              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐          │
│  │  应用日志   │   │  系统日志   │   │  操作日志   │          │
│  │ Spring Boot │   │   Linux   │   │  Action Log │          │
│  └──────┬──────┘   └──────┬──────┘   └──────┬──────┘          │
│         │                 │                 │                   │
│         └─────────────────┴─────────────────┘                   │
│                             │                                    │
│                             ▼                                    │
│                   ┌─────────────────────┐                       │
│                   │   日志采集层         │                       │
│                   │ ┌─────────────────┐ │                       │
│                   │ │  Filebeat       │ │                       │
│                   │ │  (收集+过滤)     │ │                       │
│                   │ └────────┬────────┘ │                       │
│                   └──────────┼──────────┘                       │
│                              │                                  │
│                              ▼                                  │
│                   ┌─────────────────────┐                       │
│                   │   日志处理层         │                       │
│                   │ ┌─────────────────┐ │                       │
│                   │ │ Logstash        │ │                       │
│                   │ │ (解析+ enrich)  │ │                       │
│                   │ └────────┬────────┘ │                       │
│                   └──────────┼──────────┘                       │
│                              │                                  │
│                              ▼                                  │
│                   ┌─────────────────────┐                       │
│                   │   日志存储层         │                       │
│                   │ ┌─────────────────┐ │                       │
│                   │ │ Elasticsearch   │ │                       │
│                   │ │ (索引+存储)     │ │                       │
│                   │ └────────┬────────┘ │                       │
│                   └──────────┼──────────┘                       │
│                              │                                  │
│          ┌───────────────────┼───────────────────┐            │
│          │                   │                   │            │
│          ▼                   ▼                   ▼            │
│   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐     │
│   │   Kibana    │    │  Fluentd    │    │  Log Viewer │     │
│   │  (可视化)   │    │ (边缘收集)  │    │  (简易查看) │     │
│   └─────────────┘    └─────────────┘    └─────────────┘     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 日志分类策略

| 日志类型 | 采集方式 | 处理规则 | 存储策略 |
|----------|----------|----------|----------|
| 应用日志 | Filebeat + 日志目录 | JSON解析+字段提取 | 索引：ai-ready-app-* |
| 系统日志 | Filebeat + journalctl | Syslog解析 | 索引：ai-ready-system-* |
| 操作日志 | 数据库触发器 + Binlog | 结构化存储 | 索引：ai-ready-audit-* |
| API日志 | 拦截器 + MDC | 用户ID+TraceID关联 | 索引：ai-ready-api-* |

### 2.3 日志索引策略

```yaml
# 索引生命周期管理
ilm_policy:
  name: ai-ready-logs-lifecycle
  phases:
    hot:
      actions:
        rollover:
          max_size: "50GB"
          max_age: "7d"
    warm:
      actions:
        readonly: {}
    cold:
      actions:
        freeze: {}
    delete:
      actions:
        delete:
          min_age: "90d"
```

---

## 三、审计规则引擎

### 3.1 规则引擎架构

```
┌─────────────────────────────────────────────────────────────────┐
│                   审计规则引擎架构                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐          │
│  │  规则定义   │   │  规则存储   │   │  规则执行   │          │
│  │  YAML/JSON  │──▶│  Redis DB   │──▶│  Flink streaming │      │
│  └─────────────┘   └─────────────┘   └─────────────┘          │
│                                          │                      │
│                                          ▼                      │
│                                    ┌─────────────┐              │
│                                    │  审计结果   │              │
│                                    │  存储+告警  │              │
│                                    └─────────────┘              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 核心审计规则

#### 3.2.1 安全审计规则

| 规则ID | 规则名称 | 规则描述 | 动作 |
|--------|---------|---------|------|
| SEC-001 | 异常登录检测 | 同一用户多地登录/非常用设备登录 | 告警+阻断 |
| SEC-002 | SQL注入检测 | 请求参数包含SQL注入模式 | 阻断+告警 |
| SEC-003 | XSS攻击检测 | 请求参数包含XSS代码 | 阻断+告警 |
| SEC-004 | 权限越界检测 | 用户访问未授权资源 | 告警+记录 |
| SEC-005 | 敏感操作检测 | 删除/批量修改等高危操作 | 告警+二次验证 |

#### 3.2.2 操作审计规则

| 规则ID | 规则名称 | 规则描述 | 保留期 |
|--------|---------|---------|--------|
| OP-001 | 数据变更审计 | 用户对核心表的增删改 | 180天 |
| OP-002 | 权限变更审计 | 用户权限/角色变更 | 365天 |
| OP-003 | 配置变更审计 | 系统配置修改 | 365天 |
| OP-004 | 文件操作审计 | 重要文件上传/下载 | 180天 |

#### 3.2.3 合规审计规则

| 规则ID | 规则名称 | 规则描述 | 符合标准 |
|--------|---------|---------|----------|
| COM-001 | 操作可追溯 | 所有操作留痕 | 等保2.0 |
| COM-002 | 数据最小化 | 最小权限原则 | GDPR |
| COM-003 | 加密传输 | 敏感数据加密传输 | PCI-DSS |
| COM-004 | 访问控制 | RBAC访问控制 | HIPAA |

### 3.3 规则配置示例

```yaml
# 审计规则配置示例
rules:
  # 安全规则: SQL注入检测
  - id: SEC-002
    name: SQL Injection Detection
    type: pattern_match
    patterns:
      - keyword: "SELECT.*FROM"
      - keyword: "INSERT.*INTO"
      - keyword: "DROP.*TABLE"
      - keyword: "UNION.*SELECT"
    severity: critical
    action:
      - block_request: true
      - log_request: true
      - alert: true
      - notify_channels: ["we_work", "ding_talk"]
      
  # 操作规则: 数据变更审计
  - id: OP-001
    name: Data Change Audit
    type: database_audit
    tables:
      - users
      - orders
      - products
      - customers
    operations:
      - insert
      - update
      - delete
    retention_days: 180
    action:
      - log: true
      - notify: false
      
  # 合规规则: 操作可追溯
  - id: COM-001
    name: Operation Traceability
    type: all_operations
    fields:
      - user_id
      - user_ip
      - operation_type
      - target_resource
      - timestamp
      - before_value
      - after_value
    retention_days: 180
    action:
      - log: true
      - store_index: ai-ready-audit-trace
```

---

## 四、审计日志存储

### 4.1 存储架构

```
┌─────────────────────────────────────────────────────────────────┐
│                   审计日志存储架构                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐      │
│  │  热数据层  │     │  温数据层  │     │  冷数据层  │      │
│  │  (ES集群)  │────▶│  (ES集群)  │────▶│  (OSS/S3)  │      │
│  │   7天      │     │   90天     │     │   365天    │      │
│  └─────────────┘     └─────────────┘     └─────────────┘      │
│                                                                 │
│  ┌───────────────────────────────────────────────────────┐     │
│  │                审计数据导出                            │     │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐            │     │
│  │  │ Excel    │  │  CSV     │  │  JSON    │            │     │
│  │  │  导出    │  │   导出   │  │   导出   │            │     │
│  │  └──────────┘  └──────────┘  └──────────┘            │     │
│  └───────────────────────────────────────────────────────┘     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 数据字段规范

```json
{
  "_index": "ai-ready-audit-2026.04.08",
  "_id": "audit_1775278687726_ow2fi886j",
  "_source": {
    "audit_id": "audit_1775278687726_ow2fi886j",
    "timestamp": "2026-04-08T20:00:38+08:00",
    "timestamp_ms": 1775278687726,
    
    # 用户信息
    "user_id": "user_001",
    "user_name": "张三",
    "user_ip": "192.168.1.100",
    "user_agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
    
    # 操作信息
    "operation_type": "UPDATE",
    "operation_resource": "users",
    "operation_target_id": "user_123",
    
    # 变更内容
    "before_value": {
      "status": "active",
      "role": "user"
    },
    "after_value": {
      "status": "active",
      "role": "admin"
    },
    
    # 安全信息
    "security_check": {
      "sql_injection_detected": false,
      "xss_detected": false,
      "permission_granted": true
    },
    
    # 关联信息
    "trace_id": "trace_1775278687726_abc123",
    "request_id": "req_1775278687726_def456",
    
    # 元数据
    "audit_source": "database_trigger",
    "audit_rule_id": "OP-001",
    "audit_severity": "info",
    "audit_operator": "system"
  }
}
```

### 4.3 数据保留策略

| 数据类型 | 保留期 | 存储位置 | 备注 |
|----------|--------|----------|------|
| 热数据 | 7天 | Elasticsearchhot节点 | 快速查询 |
| 温数据 | 90天 | Elasticsearchwarm节点 | 常规审计 |
| 冷数据 | 180天 | OSS/S3 | 合规审计 |
| 安全事件 | 730天 | OSS/S3 + 加密 | 安全审计 |

---

## 五、审计日志查看

### 5.1 Kibana审计仪表盘

```yaml
# Kibana审计仪表盘配置
dashboard:
  id: audit-001
  title: "AI-Ready 安全审计仪表盘"
 panels:
    - title: "操作趋势图"
      type: line_chart
      index: ai-ready-audit-*
      metric: "count() by operation_type"
      time_field: "@timestamp"
      
    - title: "异常操作Top10"
      type: bar_chart
      index: ai-ready-audit-*
      metric: "count() by user_id"
      filter: "audit_severity:(warning OR critical)"
      size: 10
      
    - title: "安全事件统计"
      type: pie_chart
      index: ai-ready-audit-*
      metric: "count() by audit_rule_id"
      filter: "security_check.security_issue:true"
      
    - title: "高危操作列表"
      type: table
      index: ai-ready-audit-*
      fields: ["timestamp", "user_name", "operation_type", "target_resource", "audit_severity"]
      filter: "audit_severity:critical"
      sort: "-timestamp"
```

### 5.2 简易日志查看器

```bash
# 日志审计命令行工具

# 1. 查询用户操作历史
ai-ready-audit query --user-id user_001 --start-time 2026-04-01 --end-time 2026-04-08

# 2. 查询高危操作
ai-ready-audit query --severity critical --type UPDATE,DELETE

# 3. 查询特定资源变更
ai-ready-audit query --resource users --target-id user_123

# 4. 导出审计报告
ai-ready-audit export --format excel --output /backup/audit/2026-04-08.xlsx
```

---

## 六、审计合规性检查

### 6.1 等保2.0合规性

| 合规项 | 要求 | 实现方式 | 状态 |
|--------|------|----------|------|
| 身份鉴别 | 登录日志记录 | 登录日志表 | ✅ |
| 安全审计 | 操作审计留痕 | Audit日志表 | ✅ |
| 访问控制 | 权限变更审计 | RBAC Audit日志 | ✅ |
| 入侵防范 | 安全事件检测 | SEC-*规则 | ✅ |
| 安全管控 | 审计日志保护 | RBAC保护审计表 | ✅ |

### 6.2 GDPR合规性

| 合规项 | 要求 | 实现方式 | 状态 |
|--------|------|----------|------|
| 数据最小化 | 最小权限 | RBAC权限控制 | ✅ |
| 访问控制 | 操作审计 | Audit日志 | ✅ |
| 数据删除 | 可追溯删除 | 软删除+审计 | ✅ |
| 数据_portability_ | 数据导出 | Audit数据导出 | ✅ |

---

## 七、实施计划

### 7.1 实施阶段

| 阶段 | 时间 | 内容 |
|------|------|------|
| 第一阶段 | 第1-2周 | 日志采集配置 + 架构部署 |
| 第二阶段 | 第3-4周 | 审计规则配置 + 规则引擎 |
| 第三阶段 | 第5-6周 | Kibana仪表盘开发 |
| 第四阶段 | 第7-8周 | 合规性验证 + 测试报告 |

### 7.2 验收标准

- [ ] 日志采集覆盖率100%（所有核心服务）
- [ ] 审计规则检测率>95%（模拟攻击测试）
- [ ] Kibana仪表盘响应时间<2s
- [ ] 审计数据查询性能<1s（100万条数据内）
- [ ] 满足等保2.0和GDPR审计要求

---

## 附录：相关文档

- [日志收集系统](../logging/Log-Collection-System.md)
- [告警规则配置](../alerting/Log-Alert-Configuration.md)
- [监控告警系统](../monitoring/MONITORING-ALERTING.md)
- [自动化运维方案](../devops/AUTOMATION_OPERATION_GUIDE.md)