# Core-MQ 消息队列监控模块

## 概述

Core-MQ 是企智连项目的消息队列核心模块，提供消息队列的监控、管理和告警功能。

## 功能特性

### 1. 消息队列状态监控
- 实时队列状态查询
- 消息积压监控
- 消费者状态追踪

### 2. Prometheus 指标集成
- 消息发布/消费计数器
- 处理时间计时器
- 队列积压仪表盘

### 3. 积压告警
- 可配置积压阈值
- 告警冷却机制
- 多渠道告警通知（待实现）

## API 端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/mq/monitor/queues` | GET | 获取所有队列状态 |
| `/api/mq/monitor/queues/{queueName}` | GET | 获取指定队列状态 |
| `/api/mq/monitor/metrics/overview` | GET | 获取监控指标概览 |
| `/api/mq/monitor/health` | GET | 健康检查 |

## Prometheus 指标

模块暴露以下 Prometheus 指标：

| 指标名称 | 类型 | 说明 |
|----------|------|------|
| `aiedge_mq_messages_published` | Counter | 已发布消息数量 |
| `aiedge_mq_messages_consumed` | Counter | 已消费消息数量 |
| `aiedge_mq_messages_failed` | Counter | 处理失败消息数量 |
| `aiedge_mq_processing_time` | Timer | 消息处理时间 |
| `aiedge_mq_queue_backlog` | Gauge | 队列积压消息数量 |
| `aiedge_mq_queue_backlog_total` | Gauge | 队列总积压数量 |

## 配置

在 `application.yml` 中添加以下配置：

```yaml
# 消息队列配置
aiedge:
  mq:
    type: rabbitmq
    alert:
      enabled: true
      backlog-threshold: 100  # 积压告警阈值

# Prometheus 端点配置
management:
  endpoints:
    web:
      exposure:
        include: prometheus,health,info,metrics
  endpoint:
    prometheus:
      enabled: true

# RabbitMQ 配置
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

## 使用示例

### 1. 记录消息指标

```java
@Autowired
private MqMetricsService mqMetricsService;

// 发布消息后记录
mqMetricsService.recordPublished("transaction.queue");

// 消费消息后记录
mqMetricsService.recordConsumed("transaction.queue");

// 处理失败时记录
mqMetricsService.recordFailed("transaction.queue", "PROCESSING_ERROR");

// 记录处理时间
mqMetricsService.recordProcessingTime("transaction.queue", 150); // 150ms
```

### 2. 获取队列状态

```java
@Autowired
private MqMetricsService mqMetricsService;

// 获取单个队列状态
Map<String, Object> queueInfo = mqMetricsService.getQueueInfo("transaction.queue");

// 获取所有队列状态
Map<String, Map<String, Object>> allQueues = mqMetricsService.getAllQueuesStatus();
```

## Grafana 面板

推荐使用以下 Grafana 面板模板：
- RabbitMQ Overview Dashboard
- Prometheus MQ Metrics Dashboard

## 告警规则示例 (Prometheus AlertManager)

```yaml
groups:
  - name: mq_alerts
    rules:
      - alert: MQQueueBacklogHigh
        expr: aiedge_mq_queue_backlog > 100
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "消息队列积压过高"
          description: "队列 {{ $labels.queue }} 积压 {{ $value }} 条消息"
```

## 依赖

- Spring Boot 3.x
- Spring AMQP (RabbitMQ)
- Micrometer (Prometheus)
- Spring Boot Actuator
