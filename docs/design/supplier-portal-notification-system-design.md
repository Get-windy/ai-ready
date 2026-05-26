# 供应商门户消息通知系统设计方案

## 1. 项目背景与目标

### 1.1 项目背景
- 供应商门户是AI-Ready ERP系统的核心协同模块
- 供应商需要及时了解采购订单、付款、质量反馈等业务动态
- 现有通知系统已具备基础功能，但缺乏针对供应商场景的优化

### 1.2 设计目标
- **多通道支持**: 支持站内信、邮件、短信、钉钉/企业微信、App推送
- **智能触发**: 基于业务规则自动触发通知
- **个性化配置**: 供应商可自定义通知偏好
- **高可用性**: 确保关键业务通知的可靠送达
- **可扩展性**: 便于未来增加新的通知渠道和业务类型

## 2. 系统架构设计

### 2.1 整体架构
```
┌─────────────────────────────────────────────────────────┐
│                    应用层 (Application Layer)            │
├─────────────────────────────────────────────────────────┤
│ 1. 供应商门户模块                                       │
│ 2. 通知事件生产者 (Event Producers)                      │
│    - 采购订单创建/更新                                  │
│    - 付款状态变更                                       │
│    - 质量检查结果                                       │
│    - 交货提醒                                           │
│    - 供应商绩效评估                                     │
└─────────────────────────────────────────────────────────┘
                               ↓
┌─────────────────────────────────────────────────────────┐
│                   通知服务层 (Notification Service)      │
├─────────────────────────────────────────────────────────┤
│ 1. 通知事件处理器                                       │
│ 2. 规则引擎 (Rule Engine)                               │
│ 3. 消息队列 (RabbitMQ/Kafka)                            │
│ 4. 模板引擎 (Velocity/FreeMarker)                       │
│ 5. 渠道适配器 (Channel Adapters)                        │
└─────────────────────────────────────────────────────────┘
                               ↓
┌─────────────────────────────────────────────────────────┐
│                   渠道服务层 (Channel Service)           │
├─────────────────────────────────────────────────────────┤
│ 1. 站内信服务 (In-App)                                  │
│ 2. 邮件服务 (SMTP/SendGrid)                             │
│ 3. 短信服务 (阿里云/腾讯云)                              │
│ 4. 即时通讯 (钉钉/企业微信/飞书)                          │
│ 5. App推送 (个推/极光)                                  │
│ 6. 语音通知 (电话)                                      │
└─────────────────────────────────────────────────────────┘
```

### 2.2 核心组件
1. **通知事件生产者**: 各业务模块产生通知事件
2. **事件处理器**: 消费事件，应用规则，生成通知
3. **渠道管理器**: 管理多个通知渠道，实现降级策略
4. **模板管理器**: 多语言、多场景的模板管理
5. **供应商偏好管理**: 存储供应商的通知偏好设置
6. **通知历史与统计**: 跟踪通知发送状态，生成报表

## 3. 消息渠道设计

### 3.1 支持渠道列表
| 渠道类型 | 适用场景 | 优先级 | 响应时间要求 | 成本 |
|---------|---------|-------|-------------|------|
| 站内信 | 所有通知 | 最高 | 实时 | 低 |
| 邮件 | 正式通知、报告 | 高 | 5分钟内 | 低 |
| 短信 | 紧急提醒 | 中 | 1分钟内 | 中 |
| 钉钉/企业微信 | 日常沟通 | 中 | 实时 | 低 |
| App推送 | 移动端通知 | 中 | 实时 | 低 |
| 语音通知 | 紧急付款 | 高 | 立即 | 高 |

### 3.2 渠道优先级与降级策略
```yaml
notification-channels:
  priority-strategy: hierarchical
  fallback-rules:
    - primary: in-app
      secondary: email
      tertiary: sms
    - primary: sms
      secondary: app-push
      tertiary: voice-call
  
  threshold-config:
    retry-attempts: 3
    retry-delay: 5000ms
    timeout: 30000ms
    rate-limits:
      sms: 10/min
      email: 100/min
      in-app: 1000/min
```

### 3.3 通道管理设计
```java
/**
 * 供应商通知渠道配置实体
 */
@Entity
@Table(name = "supplier_notification_config")
public class SupplierNotificationConfig {
    @Id
    private Long supplierId;
    
    // 按业务类型配置渠道偏好
    @Column(name = "purchase_order_channels")
    private String purchaseOrderChannels; // "in-app,email,sms"
    
    @Column(name = "payment_channels")
    private String paymentChannels; // "in-app,email,sms,voice"
    
    @Column(name = "quality_channels")
    private String qualityChannels; // "in-app,email"
    
    @Column(name = "performance_channels")
    private String performanceChannels; // "email"
    
    // 时间限制
    @Column(name = "quiet_hours_start")
    private String quietHoursStart; // "22:00"
    
    @Column(name = "quiet_hours_end")
    private String quietHoursEnd; // "08:00"
    
    // 语言偏好
    @Column(name = "preferred_language")
    private String preferredLanguage; // "zh-CN"
}
```

## 4. 业务场景与消息模板

### 4.1 核心业务场景
| 业务类型 | 触发条件 | 消息类型 | 关键信息 |
|---------|---------|---------|---------|
| 采购订单 | 订单创建、状态变更 | 业务通知 | 订单号、金额、交货期 |
| 付款 | 付款发起、到账确认 | 财务通知 | 付款金额、付款方、预计到账时间 |
| 质量检查 | 检验结果、不合格品 | 质量通知 | 批次号、检验结果、处理建议 |
| 交货提醒 | 临近交货期 | 提醒通知 | 订单号、剩余天数、联系方式 |
| 绩效评估 | 月度/季度评估完成 | 绩效通知 | 评分、排名、改进建议 |
| 供应商认证 | 认证状态变更 | 认证通知 | 认证结果、有效期、联系人 |

### 4.2 消息模板设计
```yaml
templates:
  - code: SUPPLIER_PURCHASE_ORDER_CREATED
    name: 采购订单创建通知
    channels: [in-app, email, sms]
    languages:
      zh-CN:
        title: "您有新采购订单，订单号：{orderNo}"
        content: "尊敬的{contactName}，采购订单{orderNo}已创建，金额：{amount}元，交货期：{deliveryDate}。请及时确认。"
      en-US:
        title: "New Purchase Order: {orderNo}"
        content: "Dear {contactName}, Purchase Order {orderNo} has been created. Amount: {amount}, Delivery: {deliveryDate}. Please confirm."
    variables: [orderNo, contactName, amount, deliveryDate]
  
  - code: SUPPLIER_PAYMENT_SENT
    name: 付款已发起通知
    channels: [in-app, email, sms, voice]
    languages:
      zh-CN:
        title: "付款已发起：{paymentNo}"
        content: "付款单{paymentNo}已发起，金额：{amount}元，预计到账时间：{estimatedDate}。"
    variables: [paymentNo, amount, estimatedDate]
```

### 4.3 模板变量系统
```java
public class TemplateVariable {
    // 系统变量
    private Map<String, Object> systemVars = Map.of(
        "currentDate", LocalDate.now(),
        "currentTime", LocalDateTime.now(),
        "systemName", "AI-Ready ERP"
    );
    
    // 业务变量
    private Map<String, Object> businessVars;
    
    // 供应商变量
    private Map<String, Object> supplierVars;
}
```

## 5. 智能触发机制

### 5.1 规则引擎设计
```yaml
rules:
  - name: "采购订单创建规则"
    trigger: "purchase_order.created"
    conditions:
      - "order.amount > 10000"
      - "supplier.notification_preference.purchase_order = 'high'"
    actions:
      - "notify_channels: [in-app, email, sms]"
      - "priority: high"
      - "immediate: true"
  
  - name: "交货提醒规则"
    trigger: "purchase_order.near_delivery"
    conditions:
      - "days_before_delivery <= 3"
      - "order.status = 'confirmed'"
    actions:
      - "notify_channels: [in-app, email]"
      - "priority: medium"
      - "schedule_time: '09:00'"
```

### 5.2 事件监听机制
```java
@Component
public class SupplierNotificationListener {
    
    @EventListener
    public void handlePurchaseOrderCreated(PurchaseOrderCreatedEvent event) {
        // 获取供应商信息
        Supplier supplier = supplierService.getById(event.getSupplierId());
        
        // 检查供应商通知配置
        SupplierNotificationConfig config = configService.getConfig(supplier.getId());
        
        // 应用规则
        List<Rule> rules = ruleEngine.match(event, config);
        
        // 生成通知
        rules.forEach(rule -> {
            Notification notification = buildNotification(event, rule, supplier, config);
            notificationService.send(notification);
        });
    }
    
    @EventListener
    public void handlePaymentSent(PaymentSentEvent event) {
        // 类似处理付款事件
    }
}
```

## 6. 技术实现方案

### 6.1 数据库设计
```sql
-- 供应商通知配置表
CREATE TABLE supplier_notification_config (
    supplier_id BIGINT PRIMARY KEY,
    config_json JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 供应商联系人表
CREATE TABLE supplier_contact (
    id BIGSERIAL PRIMARY KEY,
    supplier_id BIGINT NOT NULL,
    contact_name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    wechat_id VARCHAR(100),
    dingtalk_userid VARCHAR(100),
    is_primary BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 通知发送记录表
CREATE TABLE supplier_notification_record (
    id BIGSERIAL PRIMARY KEY,
    supplier_id BIGINT NOT NULL,
    template_code VARCHAR(100) NOT NULL,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    channels VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    send_time TIMESTAMP,
    read_time TIMESTAMP,
    business_type VARCHAR(50),
    business_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 6.2 核心服务接口
```java
public interface SupplierNotificationService {
    
    // 发送通知
    NotificationResult sendNotification(SupplierNotificationRequest request);
    
    // 批量发送
    BatchNotificationResult sendBatchNotifications(List<SupplierNotificationRequest> requests);
    
    // 获取供应商配置
    SupplierNotificationConfig getConfig(Long supplierId);
    
    // 更新供应商配置
    void updateConfig(Long supplierId, SupplierNotificationConfig config);
    
    // 获取通知历史
    List<NotificationRecord> getHistory(Long supplierId, DateRange range);
    
    // 统计报表
    NotificationStatistics getStatistics(Long supplierId, DateRange range);
}

// 通知请求对象
@Data
public class SupplierNotificationRequest {
    private Long supplierId;
    private String templateCode;
    private Map<String, Object> variables;
    private BusinessContext businessContext;
    private boolean async = true;
    private LocalDateTime scheduledTime;
}
```

### 6.3 渠道适配器实现
```java
@Component
@RequiredArgsConstructor
public class MultiChannelNotificationService {
    
    private final Map<String, NotificationChannel> channels;
    
    @Async
    public CompletableFuture<SendResult> send(Notification notification) {
        List<String> channelList = parseChannels(notification.getChannels());
        
        // 按优先级顺序发送
        for (String channelType : channelList) {
            NotificationChannel channel = channels.get(channelType);
            if (channel != null && channel.isAvailable()) {
                try {
                    SendResult result = channel.send(notification);
                    if (result.success()) {
                        return CompletableFuture.completedFuture(result);
                    }
                } catch (Exception e) {
                    log.warn("Channel {} failed: {}", channelType, e.getMessage());
                }
            }
        }
        
        // 所有渠道都失败
        return CompletableFuture.failedFuture(new NotificationFailedException("All channels failed"));
    }
}
```

## 7. 监控与运维

### 7.1 监控指标
| 指标 | 描述 | 告警阈值 |
|-----|------|---------|
| 发送成功率 | 成功发送通知的比例 | < 95% |
| 平均响应时间 | 从事件到通知的时间 | > 30秒 |
| 渠道可用性 | 各渠道的可用状态 | < 99% |
| 供应商覆盖率 | 已配置通知的供应商比例 | < 90% |
| 送达率 | 通知被阅读的比例 | < 60% |

### 7.2 日志与审计
- 记录所有通知发送操作
- 跟踪通知生命周期状态
- 审计供应商配置变更
- 监控异常和失败情况

## 8. 实施计划

### 8.1 第一阶段（1-2周）：基础功能
- 扩展现有通知系统，增加供应商相关模板
- 实现供应商通知配置管理
- 集成站内信和邮件渠道

### 8.2 第二阶段（2-3周）：高级功能
- 实现多通道降级策略
- 集成短信和即时通讯渠道
- 开发智能触发规则引擎
- 实现通知历史查询

### 8.3 第三阶段（1-2周）：优化与监控
- 性能优化和缓存策略
- 监控和告警系统
- 供应商自助配置页面
- 统计报表功能

## 9. 预期效果

### 9.1 业务效益
- 采购订单确认时间减少50%
- 供应商满意度提升30%
- 付款问题响应时间减少70%
- 质量反馈处理效率提升40%

### 9.2 技术效益
- 通知送达率>99%
- 平均响应时间<5秒
- 系统可用性>99.9%
- 支持每秒1000+通知发送

## 10. 风险评估与应对

### 10.1 技术风险
| 风险 | 影响 | 应对措施 |
|-----|------|---------|
| 第三方服务不稳定 | 通知延迟或失败 | 多通道降级，本地队列缓冲 |
| 高并发压力 | 系统性能下降 | 异步处理，水平扩展 |
| 数据不一致 | 通知发送重复或丢失 | 幂等性设计，事务补偿 |

### 10.2 业务风险
| 风险 | 影响 | 应对措施 |
|-----|------|---------|
| 供应商信息不完整 | 通知无法送达 | 信息验证机制，默认渠道 |
| 沟通内容敏感 | 信息泄露风险 | 内容审核，权限控制 |
| 通知频率过高 | 供应商投诉 | 频率限制，智能合并 |

---

**文档版本**: v1.0  
**创建日期**: 2026-05-01  
**创建人**: Team Member Agent  
**审核状态**: 待审核