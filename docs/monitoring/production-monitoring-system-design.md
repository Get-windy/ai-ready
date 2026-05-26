# ERP系统生产环境监控告警配置与实施

## 1. 监控体系设计

### 1.1 监控指标定义

#### 系统级指标
- **CPU使用率**: 核心系统资源使用情况
- **内存使用率**: 内存占用和交换空间使用
- **磁盘使用率**: 根分区、数据分区、日志分区
- **网络带宽**: 入站/出站流量，连接数
- **系统负载**: 1分钟、5分钟、15分钟平均负载

#### 应用级指标
- **应用响应时间**: API接口响应时间（P50/P95/P99）
- **错误率**: HTTP 4xx/5xx错误率
- **吞吐量**: 请求数/秒，事务数/秒
- **JVM指标**: 堆内存使用、GC时间、线程数
- **数据库连接池**: 活跃连接、空闲连接、等待连接

#### 业务级指标
- **用户活跃度**: 在线用户数、会话数
- **订单处理**: 订单创建成功率、处理时间
- **库存管理**: 库存更新延迟、库存准确率
- **财务处理**: 对账成功率、支付处理时间
- **供应商协同**: 询价响应率、报价处理时间

#### 数据库指标
- **PostgreSQL性能**: 查询响应时间、连接数、锁等待
- **Redis性能**: 命中率、内存使用、连接数
- **Kafka性能**: 消息积压、消费延迟

### 1.2 告警策略制定

#### 告警级别定义
- **P0（紧急）**: 系统不可用，核心功能中断
- **P1（严重）**: 核心功能降级，影响用户体验
- **P2（警告）**: 非核心功能异常，需要关注
- **P3（提示）**: 系统指标异常，需要观察

#### 阈值告警策略
```yaml
cpu_usage:
  warning: 80%
  critical: 90%
  duration: 5m

memory_usage:
  warning: 85%
  critical: 95%
  duration: 5m

disk_usage:
  warning: 85%
  critical: 95%
  duration: 5m

api_response_time:
  p95_warning: 2000ms
  p95_critical: 5000ms
  duration: 10m

error_rate:
  warning: 1%
  critical: 5%
  duration: 5m
```

#### 趋势告警策略
- **连续上升趋势**: 连续3个采样点上升超过10%
- **异常波动**: 指标波动超过历史平均值的3倍标准差
- **周期性异常**: 与历史同期对比异常

### 1.3 通知渠道配置

#### 告警通知渠道
1. **邮件通知**
   - 运维团队邮箱列表
   - 值班人员邮箱
   - 管理层邮箱（仅P0/P1）

2. **即时通讯**
   - 飞书机器人通知
   - 钉钉机器人通知
   - 企业微信通知

3. **短信通知**
   - 值班人员手机
   - 紧急联系人

4. **电话通知**
   - P0级别告警自动电话通知

#### 通知模板
```yaml
email_template:
  subject: "[{level}] {service}告警 - {alert_name}"
  body: |
    告警级别: {level}
    告警名称: {alert_name}
    告警时间: {alert_time}
    当前值: {current_value}
    阈值: {threshold}
    实例: {instance}
    详情: {details}
    链接: {dashboard_url}

chat_template:
  title: "🚨 [{level}] {alert_name}"
  content: |
    服务: {service}
    级别: {level}
    时间: {alert_time}
    当前值: {current_value}
    阈值: {threshold}
    详情: {details}
    处理链接: {dashboard_url}
```

### 1.4 分级响应机制

#### P0级响应机制
- **自动响应**: 自动重启服务，切换流量
- **人工响应**: 5分钟内必须响应，15分钟内必须处理
- **升级机制**: 30分钟未解决升级到技术总监
- **后续跟进**: 必须编写事故报告

#### P1级响应机制
- **人工响应**: 15分钟内必须响应，1小时内必须处理
- **升级机制**: 2小时未解决升级到团队负责人
- **后续跟进**: 需要分析报告

#### P2级响应机制
- **人工响应**: 1小时内响应，4小时内处理
- **升级机制**: 8小时未解决升级到团队负责人
- **后续跟进**: 记录处理过程

#### P3级响应机制
- **人工响应**: 4小时内响应，24小时内处理
- **升级机制**: 48小时未解决升级到团队负责人

## 2. 监控工具部署

### 2.1 监控平台选型

#### 核心监控栈
- **指标收集**: Prometheus
- **可视化**: Grafana
- **日志收集**: ELK Stack (Elasticsearch + Logstash + Kibana)
- **分布式追踪**: Jaeger
- **告警管理**: Alertmanager
- **应用性能监控**: SkyWalking

#### 技术栈选择理由
1. **Prometheus**: 云原生标准，社区活跃，易于扩展
2. **Grafana**: 强大的可视化能力，丰富的仪表盘生态
3. **ELK**: 成熟的日志收集和分析方案
4. **Jaeger**: CNCF项目，与微服务架构兼容性好
5. **SkyWalking**: 国产APM工具，对Java应用支持好

### 2.2 数据收集架构

#### 数据收集层
```yaml
data_sources:
  - node_exporter: 系统指标
  - mysqld_exporter: MySQL指标
  - redis_exporter: Redis指标
  - kafka_exporter: Kafka指标
  - jmx_exporter: JVM指标
  - blackbox_exporter: 网络探测
  - spring_boot_actuator: 应用指标
```

#### 数据存储层
- **Prometheus**: 短期指标存储（15天）
- **Thanos/Cortex**: 长期指标存储
- **Elasticsearch**: 日志存储
- **ClickHouse**: 业务指标存储

#### 数据处理层
- **PromQL**: 指标查询语言
- **Logstash**: 日志处理管道
- **Fluentd**: 日志收集代理

### 2.3 部署架构

#### 生产环境部署拓扑
```
┌─────────────────────────────────────────────────┐
│                 负载均衡层                       │
│              (Nginx/Haproxy)                    │
└─────────────────┬─────────────┬─────────────────┘
                  │             │
    ┌─────────────▼─────┐ ┌─────▼─────────────┐
    │  应用服务器集群    │ │  数据库集群        │
    │  (ERP应用服务)    │ │  (PostgreSQL)     │
    └─────────────┬─────┘ └─────┬─────────────┘
                  │             │
    ┌─────────────▼─────────────▼─────────────┐
    │          监控服务器集群                   │
    │  (Prometheus + Grafana + Alertmanager)  │
    └─────────────┬─────────────┬─────────────┘
                  │             │
    ┌─────────────▼─────┐ ┌─────▼─────────────┐
    │  日志服务器集群    │ │  告警通知服务     │
    │  (ELK Stack)      │ │  (邮件/短信/IM)   │
    └───────────────────┘ └───────────────────┘
```

### 2.4 配置管理

#### Prometheus配置
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "rules/*.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

scrape_configs:
  - job_name: 'erp-application'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['erp-app-1:8080', 'erp-app-2:8080']
  
  - job_name: 'postgresql'
    static_configs:
      - targets: ['postgresql-master:9187']
  
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-master:9121']
  
  - job_name: 'node'
    static_configs:
      - targets: ['node-1:9100', 'node-2:9100']
```

#### Alertmanager配置
```yaml
global:
  smtp_smarthost: 'smtp.example.com:587'
  smtp_from: 'alert@erp.example.com'
  smtp_auth_username: 'alert'
  smtp_auth_password: 'password'

route:
  group_by: ['alertname', 'cluster', 'service']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'default-receiver'
  
  routes:
    - match:
        severity: 'critical'
      receiver: 'critical-receiver'
      group_wait: 0s
      repeat_interval: 5m
    
    - match:
        severity: 'warning'
      receiver: 'warning-receiver'

receivers:
  - name: 'default-receiver'
    email_configs:
      - to: 'ops-team@example.com'
  
  - name: 'critical-receiver'
    email_configs:
      - to: 'ops-team@example.com'
    webhook_configs:
      - url: 'http://feishu-webhook/alert'
      - url: 'http://dingtalk-webhook/alert'
    pagerduty_configs:
      - service_key: 'your-pagerduty-key'
  
  - name: 'warning-receiver'
    email_configs:
      - to: 'ops-team@example.com'
```

## 3. 实施计划

### 3.1 第一阶段：基础监控部署（第1周）
- [ ] 部署Prometheus + Grafana
- [ ] 配置系统级监控（Node Exporter）
- [ ] 配置数据库监控（PostgreSQL Exporter）
- [ ] 配置缓存监控（Redis Exporter）
- [ ] 部署Alertmanager
- [ ] 配置邮件告警

### 3.2 第二阶段：应用监控部署（第2周）
- [ ] 集成Spring Boot Actuator
- [ ] 配置应用指标收集
- [ ] 配置业务指标监控
- [ ] 部署ELK日志收集
- [ ] 配置日志告警

### 3.3 第三阶段：告警优化（第3周）
- [ ] 配置即时通讯告警（飞书/钉钉）
- [ ] 优化告警策略
- [ ] 配置告警降噪
- [ ] 建立告警值班制度
- [ ] 编写告警处理手册

### 3.4 第四阶段：高级监控（第4周）
- [ ] 部署分布式追踪（Jaeger）
- [ ] 配置APM监控（SkyWalking）
- [ ] 建立监控仪表盘
- [ ] 配置自动化巡检
- [ ] 建立监控报告机制

## 4. 运维手册

### 4.1 日常巡检清单
1. **系统健康检查**
   - 检查所有Prometheus Target状态
   - 检查Grafana仪表盘
   - 检查告警状态
   - 检查磁盘空间

2. **告警处理流程**
   - 确认告警真实性
   - 根据级别执行响应流程
   - 记录处理过程
   - 更新告警状态

3. **监控系统维护**
   - 定期清理过期数据
   - 更新监控规则
   - 优化告警策略
   - 备份监控配置

### 4.2 应急预案

#### 监控系统故障
1. **Prometheus故障**
   - 切换到备用Prometheus实例
   - 检查存储卷状态
   - 恢复数据备份

2. **Grafana故障**
   - 重启Grafana服务
   - 恢复配置文件
   - 检查数据库连接

3. **告警系统故障**
   - 临时启用备用通知渠道
   - 人工巡检关键指标
   - 优先恢复核心告警

## 5. 验收标准

### 5.1 技术验收标准
- [ ] 系统指标采集覆盖率 ≥ 95%
- [ ] 应用指标采集覆盖率 ≥ 90%
- [ ] 告警准确率 ≥ 98%
- [ ] 告警响应时间 ≤ 5分钟
- [ ] 监控数据保留时间 ≥ 30天

### 5.2 业务验收标准
- [ ] 核心业务流程监控覆盖率 100%
- [ ] 关键业务指标告警配置完成
- [ ] 告警通知渠道测试通过
- [ ] 运维团队培训完成
- [ ] 监控文档完整可用

## 6. 后续优化方向

### 6.1 智能化监控
- 基于机器学习的异常检测
- 根因分析自动化
- 智能告警降噪
- 预测性告警

### 6.2 监控即代码
- 监控配置版本化管理
- 自动化部署监控组件
- 监控配置CI/CD流水线
- 监控测试自动化

### 6.3 成本优化
- 监控数据生命周期管理
- 存储成本优化
- 计算资源优化
- 告警成本控制

---
**文档版本**: v1.0
**创建时间**: 2026-05-04
**创建人**: devops-engineer
**最后更新**: 2026-05-04
**状态**: 设计完成，待实施