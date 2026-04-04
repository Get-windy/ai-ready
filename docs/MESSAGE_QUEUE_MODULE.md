# AI-Ready 消息队列模块文档

## 版本信息
- 版本：v1.0.0
- 更新日期：2026-04-04
- 开发人员：team-member

## 一、模块架构

```
cn.aiedge.mq/
├── config/
│   └── RabbitMQConfig.java         # RabbitMQ配置
├── model/
│   └── MessageEntity.java           # 通用消息实体
├── producer/
│   └── MessageProducer.java         # 消息生产者
├── consumer/
│   ├── BaseMessageConsumer.java     # 消费者基类
│   └── MessageConsumerHandler.java  # 消费者处理器
└── controller/
    └── MessageQueueController.java  # 管理接口
```

## 二、核心功能

### 2.1 消息生产
- 支持普通消息发送
- 支持延迟消息发送
- 支持批量消息发送
- 消息持久化

### 2.2 消息消费
- 自动监听队列
- 手动确认模式
- 失败重试机制
- 死信队列处理

### 2.3 预定义队列
| 队列 | 路由键 | 说明 |
|------|--------|------|
| ai.ready.email | email | 邮件发送队列 |
| ai.ready.sms | sms | 短信发送队列 |
| ai.ready.notification | notification | 系统通知队列 |
| ai.ready.data.sync | sync | 数据同步队列 |
| ai.ready.dead.letter | - | 死信队列 |

## 三、配置说明

```yaml
# application.yml
mq:
  rabbit:
    enabled: true
    default-exchange: ai-ready-exchange
    queue-prefix: ai.ready.
    concurrent-consumers: 3
    max-concurrent-consumers: 10
    prefetch-count: 10

spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    publisher-confirm-type: correlated
    publisher-returns: true
```

## 四、使用示例

### 4.1 发送消息

```java
@Autowired
private MessageProducer messageProducer;

// 发送邮件
messageProducer.sendEmail("user@example.com", "主题", "内容");

// 发送短信
messageProducer.sendSms("13800138000", "验证码：123456");

// 发送通知
messageProducer.sendNotification(userId, "系统通知", "您有新的消息");

// 发送自定义消息
MessageEntity message = MessageEntity.of("sync", "DATA_SYNC", data);
messageProducer.send("sync", message);

// 发送延迟消息（30秒后执行）
messageProducer.sendDelayed("notification", message, 30000);
```

### 4.2 消费消息

```java
@RabbitListener(queues = "#{rabbitMQConfig.queuePrefix + 'email'}")
public void handleEmailMessage(MessageEntity message, Message rawMessage, Channel channel) 
        throws IOException {
    long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
    
    try {
        // 处理消息
        EmailPayload email = convertPayload(message.getPayload(), EmailPayload.class);
        emailService.send(email);
        
        // 确认消息
        channel.basicAck(deliveryTag, false);
    } catch (Exception e) {
        // 拒绝消息，重新入队
        channel.basicNack(deliveryTag, false, true);
    }
}
```

### 4.3 API接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/mq/send/email | 发送邮件消息 |
| POST | /api/mq/send/sms | 发送短信消息 |
| POST | /api/mq/send/notification | 发送通知消息 |
| POST | /api/mq/send | 发送自定义消息 |
| POST | /api/mq/send-delayed | 发送延迟消息 |
| GET | /api/mq/queues | 获取队列信息 |
| GET | /api/mq/health | 健康检查 |
| GET | /api/mq/stats | 消息统计 |

## 五、消息格式

```json
{
  "messageId": "uuid",
  "messageType": "EMAIL_SEND",
  "topic": "email",
  "payload": {
    "to": "user@example.com",
    "subject": "主题",
    "content": "内容"
  },
  "createTime": "2026-04-04T10:00:00",
  "retryCount": 0,
  "maxRetry": 3
}
```

## 六、消息持久化

### 6.1 队列持久化
所有预定义队列都配置为持久化（durable=true）

### 6.2 消息持久化
消息默认使用 PERSISTENT 投递模式

### 6.3 死信队列
处理失败的消息会被路由到死信队列：
- 交换机：ai-ready-exchange.dlx
- 队列：ai.ready.dead.letter

## 七、最佳实践

1. **消息幂等性**：消费逻辑应支持重复消费
2. **异常处理**：捕获所有异常，避免消息丢失
3. **确认机制**：使用手动确认模式
4. **重试策略**：合理设置重试次数
5. **监控告警**：监控队列积压情况
6. **死信处理**：定期处理死信队列消息

---
*文档更新时间：2026-04-04*
