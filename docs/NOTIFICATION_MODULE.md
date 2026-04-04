# AI-Ready 消息通知模块文档

## 概述

AI-Ready 消息通知模块提供统一的通知能力，支持站内信、邮件、短信三种通知渠道，包含模板管理和通知记录功能。

## 模块结构

```
cn.aiedge.notification
├── channel/                      # 通知渠道
│   ├── NotificationChannel.java  # 渠道接口
│   ├── SiteMessageChannel.java   # 站内信
│   ├── EmailChannel.java         # 邮件
│   └── SmsChannel.java           # 短信
├── controller/                   # 控制器
│   └── NotificationController.java
├── entity/                       # 实体类
│   ├── NotificationTemplate.java # 通知模板
│   └── NotificationRecord.java   # 通知记录
├── service/                      # 服务层
│   ├── NotificationService.java
│   └── impl/
│       └── NotificationServiceImpl.java
└── template/                     # 模板渲染
    └── TemplateRenderer.java
```

## 核心功能

### 1. 通知模板

使用变量占位符 `${var}` 定义模板：

```java
NotificationTemplate template = new NotificationTemplate();
template.setTemplateCode("ORDER_SHIPPED");
template.setTemplateName("订单发货通知");
template.setNotifyType("email");
template.setTitle("您的订单已发货 - ${orderId}");
template.setContent("尊敬的${customerName}，您的订单${orderId}已发货，预计${deliveryDate}送达。");
template.setStatus(1);
```

### 2. 发送通知

```java
@Autowired
private NotificationService notificationService;

// 使用模板发送
Map<String, Object> vars = Map.of(
    "customerName", "张三",
    "orderId", "ORD-12345",
    "deliveryDate", "2024-01-20"
);
notificationService.send("ORDER_SHIPPED", userId, "user", "zhangsan@example.com", vars);

// 直接发送站内信
notificationService.sendSiteMessage(userId, "系统通知", "您有新的待办任务");

// 发送邮件
notificationService.sendEmail("user@example.com", "验证码", "您的验证码是：123456");

// 发送短信
notificationService.sendSms("13800138000", "您的验证码是123456，5分钟内有效");
```

### 3. 用户通知管理

```java
// 获取用户通知列表
List<NotificationRecord> notifications = notificationService.getUserNotifications(userId, null, 20);

// 获取未读数量
int unreadCount = notificationService.getUnreadCount(userId);

// 标记已读
notificationService.markAsRead(recordId);

// 全部标记已读
notificationService.markAllAsRead(userId);
```

## 通知渠道配置

### 邮件配置 (application.yml)

```yaml
spring:
  mail:
    host: smtp.example.com
    port: 465
    username: noreply@example.com
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          ssl:
            enable: true
```

### 短信配置

```yaml
notification:
  sms:
    enabled: true
    provider: aliyun  # aliyun/tencent/custom
```

## API 接口

### 发送通知

```
POST /api/notifications/send
{
    "templateCode": "ORDER_SHIPPED",
    "receiverId": 123,
    "receiverType": "user",
    "receiverAddress": "user@example.com",
    "variables": {
        "customerName": "张三",
        "orderId": "ORD-12345"
    }
}
```

### 获取用户通知

```
GET /api/notifications/user/{userId}?readStatus=0&limit=20
```

### 标记已读

```
PUT /api/notifications/{recordId}/read
```

## 数据库表结构

### sys_notification_template（通知模板表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 模板ID |
| template_code | VARCHAR(50) | 模板编码 |
| template_name | VARCHAR(100) | 模板名称 |
| notify_type | VARCHAR(20) | 通知类型 |
| title | VARCHAR(200) | 模板标题 |
| content | TEXT | 模板内容 |
| status | INT | 状态 |

### sys_notification_record（通知记录表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 记录ID |
| template_id | BIGINT | 模板ID |
| notify_type | VARCHAR(20) | 通知类型 |
| receiver_id | BIGINT | 接收者ID |
| receiver_address | VARCHAR(200) | 接收地址 |
| title | VARCHAR(200) | 通知标题 |
| content | TEXT | 通知内容 |
| status | INT | 发送状态 |
| send_time | DATETIME | 发送时间 |
| read_status | INT | 阅读状态 |

## 扩展新渠道

1. 实现 `NotificationChannel` 接口：

```java
@Component
public class WechatChannel implements NotificationChannel {
    
    @Override
    public SendResult send(NotificationRecord record) {
        // 实现微信消息发送逻辑
        return SendResult.success();
    }
    
    @Override
    public String getChannelType() {
        return "wechat";
    }
    
    @Override
    public boolean isAvailable() {
        return true;
    }
}
```

2. 在模板中添加对应类型

## 单元测试

```bash
mvn test -Dtest=TemplateRendererTest
```

## 版本历史

- v1.0.0 (2026-04-03)
  - 初始版本
  - 支持站内信、邮件、短信三种渠道
  - 模板系统支持变量渲染
  - 通知记录与阅读状态管理
