# AI-Ready 日志告警规则配置文档

> **项目**: 智企连·AI-Ready  
> **版本**: v1.0  
> **最后更新**: 2026-03-30  
> **维护者**: devops-engineer

---

## 目录

1. [概述](#概述)
2. [告警规则配置](#告警规则配置)
3. [告警阈值设置](#告警阈值设置)
4. [通知渠道配置](#通知渠道配置)
5. [告警抑制规则](#告警抑制规则)
6. [测试指南](#测试指南)

---

## 概述

### 告警系统架构

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   日志源    │────▶│  告警规则   │────▶│  告警路由   │
│ (ELK Stack) │     │ (Log Rules) │     │ (Router)    │
└─────────────┘     └─────────────┘     └─────────────┘
                                                │
                                                ▼
                                        ┌─────────────┐
                                        │  通知渠道   │
                                        │ (Channels)  │
                                        └─────────────┘
                                                │
                    ┌───────────────────────────┼───────────────────────────┐
                    │                           │                           │
                    ▼                           ▼                           ▼
             ┌──────────┐               ┌──────────┐               ┌──────────┐
             │ 企业微信  │               │  钉钉    │               │  邮件    │
             │ WeWork   │               │ DingTalk │               │  Email  │
             └──────────┘               └──────────┘               └──────────┘
```

### 告警级别

| 级别 | 颜色 | 响应时间 | 通知渠道 |
|------|------|----------|----------|
| Critical | 🔴 红色 | 5分钟 | 企业微信+钉钉+邮件+短信+电话 |
| Warning | 🟠 橙色 | 30分钟 | 企业微信+邮件 |
| Info | 🔵 蓝色 | 2小时 | 企业微信 |

---

## 告警规则配置

### 配置文件位置

```
I:\AI-Ready\configs\alerting\
├── log-alert-rules.yml        # 告警规则 (9KB)
├── log-alert-thresholds.yml   # 告警阈值 (4KB)
├── log-alert-inhibition.yml   # 抑制规则 (4KB)
└── log-notification-channels.yml # 通知渠道 (5KB)
```

### 告警规则分类

#### 1. 错误日志告警 (log-error-alerts)

| 告警名称 | 触发条件 | 等待时间 | 级别 |
|---------|---------|---------|------|
| HighErrorLogRate | 错误日志 >10条/秒 | 2分钟 | Critical |
| ErrorLogSpike | 错误日志 >50条/秒 | 1分钟 | Critical |
| CriticalErrorDetected | 严重错误 >5个/5分钟 | 1分钟 | Critical |

#### 2. 异常日志告警 (log-exception-alerts)

| 告警名称 | 触发条件 | 等待时间 | 级别 |
|---------|---------|---------|------|
| NullPointerExceptionDetected | 检测到NPE | 1分钟 | Warning |
| DatabaseConnectionError | 数据库连接失败 | 1分钟 | Critical |
| RedisConnectionError | Redis连接失败 | 1分钟 | Critical |

#### 3. 安全日志告警 (log-security-alerts)

| 告警名称 | 触发条件 | 等待时间 | 级别 |
|---------|---------|---------|------|
| AuthenticationFailure | 认证失败 >5次/秒 | 3分钟 | Warning |
| SQLInjectionAttempt | SQL注入模式 | 1分钟 | Critical |
| XSSAttempt | XSS模式 | 1分钟 | Warning |

#### 4. 性能日志告警 (log-performance-alerts)

| 告警名称 | 触发条件 | 等待时间 | 级别 |
|---------|---------|---------|------|
| SlowAPIDetected | 响应 >10秒 | 3分钟 | Warning |
| HighMemoryUsageLog | OOM错误 | 1分钟 | Critical |
| ThreadPoolExhausted | 线程池耗尽 | 1分钟 | Warning |

#### 5. 业务日志告警 (log-business-alerts)

| 告警名称 | 触发条件 | 等待时间 | 级别 |
|---------|---------|---------|------|
| OrderProcessingFailure | 订单失败 >3次/5分钟 | 2分钟 | Warning |
| PaymentProcessingFailure | 支付失败 >1次/5分钟 | 2分钟 | Critical |
| AIModelInferenceFailure | AI推理失败 >5次/5分钟 | 2分钟 | Warning |

#### 6. 第三方集成告警 (log-integration-alerts)

| 告警名称 | 触发条件 | 等待时间 | 级别 |
|---------|---------|---------|------|
| DingTalkAPIFailure | 钉钉API失败 >3次/5分钟 | 2分钟 | Warning |
| ExternalAPITimeout | 外部API超时 >5次/5分钟 | 3分钟 | Warning |

---

## 告警阈值设置

### 错误日志阈值

```yaml
error-logs:
  error_rate_per_minute:
    warning: 10
    critical: 50
    
  critical_errors:
    NullPointerException: 1
    OutOfMemoryError: 1
    DatabaseConnectionError: 3
```

### 性能日志阈值

```yaml
performance-logs:
  response_time:
    warning: 3000    # 3秒
    critical: 10000  # 10秒
    
  memory_usage_percent:
    warning: 80
    critical: 90
```

### 业务日志阈值

```yaml
business-logs:
  order_failures_5m:
    warning: 5
    critical: 20
    
  payment_failures_5m:
    warning: 1
    critical: 5
```

---

## 通知渠道配置

### 企业微信

```yaml
wechat_work:
  enabled: true
  webhook_url: "${WECHAT_WORK_WEBHOOK_URL}"
  templates:
    critical: |
      🔴 **严重告警**
      **告警名称**: {{ .AlertName }}
      **服务**: {{ .Labels.service }}
```

### 钉钉

```yaml
dingtalk:
  enabled: true
  webhook_url: "${DINGTALK_WEBHOOK_URL}"
  secret: "${DINGTALK_SECRET}"
```

### 邮件

```yaml
email:
  enabled: true
  smtp_host: "smtp.exmail.qq.com"
  smtp_port: 465
  smtp_user: "${EMAIL_USER}"
  smtp_password: "${EMAIL_PASSWORD}"
```

### 短信

```yaml
sms:
  enabled: true
  provider: "aliyun"
  recipients:
    critical:
      - "13800138000"
      - "13800138001"
```

### 电话

```yaml
phone:
  enabled: true
  provider: "aliyun"
  recipients:
    critical:
      - "13800138000"  # 值班人员
```

---

## 告警抑制规则

### 全局抑制规则

1. **服务宕机抑制**: 当服务宕机时，抑制该服务的其他告警
2. **数据库连接抑制**: 当数据库连接失败时，抑制数据库相关告警
3. **严重告警抑制警告**: 当严重告警触发时，抑制同类型的警告告警

### 时间窗口抑制

```yaml
time_inhibit_rules:
  - name: "after_hours_inhibit"
    enabled: true
    match:
      severity: "warning"
    time_intervals:
      - weekdays: ["monday", "tuesday", "wednesday", "thursday", "friday"]
        times:
          - start_time: "22:00"
            end_time: "09:00"
```

### 告警去重

```yaml
deduplication_rules:
  - name: "dedup_by_alertname_service"
    enabled: true
    group_by: ["alertname", "service"]
    dedup_interval: "5m"
```

---

## 测试指南

### 测试步骤

#### 1. 测试企业微信通知

```bash
curl -X POST "${WECHAT_WORK_WEBHOOK_URL}" \
  -H "Content-Type: application/json" \
  -d '{
    "msgtype": "markdown",
    "markdown": {
      "content": "## 🔴 测试告警\n\n**测试企业微信通知**\n\n**时间**: 2026-03-30 03:15:00"
    }
  }'
```

#### 2. 测试钉钉通知

```bash
# 生成签名
timestamp=$(date +%s%3N)
sign=$(echo -n "${timestamp}\n${DINGTALK_SECRET}" | openssl dgst -sha256 -hmac "${DINGTALK_SECRET}" -binary | base64)

curl -X POST "${DINGTALK_WEBHOOK_URL}&timestamp=${timestamp}&sign=${sign}" \
  -H "Content-Type: application/json" \
  -d '{
    "msgtype": "markdown",
    "markdown": {
      "title": "测试告警",
      "text": "### 🔴 测试告警\n\n测试钉钉通知"
    }
  }'
```

#### 3. 测试邮件通知

```bash
# 使用 telnet 测试 SMTP
telnet smtp.exmail.qq.com 465

# 或使用 swaks 工具
swaks --to devops@ai-ready.cn \
  --from alert@ai-ready.cn \
  --server smtp.exmail.qq.com \
  --port 465 \
  --auth LOGIN \
  --auth-user "${EMAIL_USER}" \
  --auth-password "${EMAIL_PASSWORD}" \
  --header "Subject: Test Alert" \
  --body "This is a test alert."
```

#### 4. 模拟告警触发

```bash
# 模拟错误日志
curl -X POST "http://localhost:8080/api/test/error" \
  -H "Content-Type: application/json" \
  -d '{"message": "Test error log for alert testing"}'

# 模拟慢请求
curl -X POST "http://localhost:8080/api/test/slow" \
  -H "Content-Type: application/json" \
  -d '{"delay": 5000}'
```

### 验证检查清单

```markdown
## 告警测试验证

### 企业微信
- [ ] 机器人Webhook URL正确
- [ ] 消息格式正确
- [ ] @人员正确

### 钉钉
- [ ] 机器人Webhook URL正确
- [ ] 加签Secret正确
- [ ] 消息格式正确

### 邮件
- [ ] SMTP服务器连接正常
- [ ] 认证信息正确
- [ ] 收件人地址正确

### 短信/电话
- [ ] 阿里云Access Key正确
- [ ] 模板ID正确
- [ ] 手机号码正确
```

---

## 运维手册

### 日常维护

1. **检查告警规则**: 每月检查一次告警规则有效性
2. **更新通知人员**: 人员变动时及时更新通知配置
3. **调整阈值**: 根据业务变化调整告警阈值

### 故障排查

#### 告警未触发

1. 检查告警规则是否启用
2. 检查日志是否正常采集
3. 检查告警阈值是否合理

#### 告警未通知

1. 检查通知渠道配置
2. 检查抑制规则是否生效
3. 检查网络连接

### 配置更新流程

1. 修改配置文件
2. 提交代码审查
3. 合并到主分支
4. 部署配置更新
5. 验证告警功能

---

## 附录

### 配置文件清单

| 文件 | 大小 | 说明 |
|------|------|------|
| log-alert-rules.yml | 9KB | 告警规则配置 |
| log-alert-thresholds.yml | 4KB | 告警阈值配置 |
| log-alert-inhibition.yml | 4KB | 告警抑制规则 |
| log-notification-channels.yml | 5KB | 通知渠道配置 |
| Log-Alert-Configuration.md | 5KB | 配置文档 |

### 常用命令

```bash
# 查看告警规则状态
curl http://localhost:9090/api/v1/rules

# 查看告警状态
curl http://localhost:9093/api/v2/alerts

# 静默告警
curl -X POST http://localhost:9093/api/v2/silences \
  -H "Content-Type: application/json" \
  -d '{"matchers":[{"name":"alertname","value":"HighErrorLogRate","isRegex":false}],"startsAt":"2026-03-30T00:00:00Z","endsAt":"2026-03-30T01:00:00Z","createdBy":"admin","comment":"Maintenance window"}'
```

---

*文档由 devops-engineer 自动生成和维护*
*最后更新: 2026-03-30*