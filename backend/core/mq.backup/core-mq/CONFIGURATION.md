# 企智连消息队列模块配置说明

## 配置概述

企智连消息队列模块提供了丰富的配置选项，可以通过 `application.yml` 或 `application.properties` 文件进行配置。

## 配置项详解

### 基础配置

```yaml
aiedge:
  mq:
    type: rabbitmq  # 消息队列类型，目前支持rabbitmq
```

### RabbitMQ 配置

```yaml
aiedge:
  mq:
    rabbitmq:
      host: localhost                    # RabbitMQ 服务器地址
      port: 5672                         # RabbitMQ 端口
      username: guest                    # 用户名
      password: guest                    # 密码
      virtual-host: /                    # 虚拟主机
      publisher-confirms: true           # 是否启用发布确认
      publisher-returns: true            # 是否启用发布返回
      connection-timeout: 30000          # 连接超时时间（毫秒）
      requested-heartbeat: 60            # 心跳间隔（秒）
      automatic-recovery-enabled: true   # 是否启用自动恢复
      network-recovery-interval: 10000   # 网络恢复间隔（毫秒）
```

### 连接池配置

```yaml
spring:
  rabbitmq:
    template:
      retry:
        enabled: true                    # 是否启用重试
        initial-interval: 1000           # 初始重试间隔（毫秒）
        max-attempts: 3                  # 最大重试次数
        multiplier: 2                    # 重试间隔倍数
      mandatory: true                    # 是否强制要求消息被路由
    listener:
      simple:
        concurrency: 5                   # 最小消费者数量
        max-concurrency: 10              # 最大消费者数量
        prefetch: 1                      # 预取数量
        acknowledge-mode: manual         # 确认模式
        retry:
          enabled: true                  # 是否启用消费者重试
          initial-interval: 1000         # 初始重试间隔
          max-attempts: 3                # 最大重试次数
          multiplier: 2                  # 重试间隔倍数
```

## 死信队列配置

```yaml
# 队列参数配置示例
spring:
  rabbitmq:
    template:
      exchange: default_exchange         # 默认交换机
      routing-key: default_routing_key   # 默认路由键
```

## 高级配置

### 消息序列化配置

```yaml
# Jackson 配置
spring:
  jackson:
    serialization:
      write-dates-as-timestamps: false
    deserialization:
      fail-on-unknown-properties: false
```

### SSL 配置（如果需要）

```yaml
spring:
  rabbitmq:
    ssl:
      enabled: false                     # 是否启用SSL
      algorithm: TLS                     # SSL算法
      trust-store:                       # 信任库路径
      trust-store-password:              # 信任库密码
      key-store:                         # 密钥库路径
      key-store-password:                # 密钥库密码
```

## 监控配置

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,mq-stats    # 暴露的监控端点
  endpoint:
    health:
      show-details: when-authorized      # 显示健康详情
```

## 集群配置示例

```yaml
spring:
  rabbitmq:
    addresses: rabbit1:5672,rabbit2:5672,rabbit3:5672  # 集群节点地址
    username: cluster-user
    password: cluster-password
```

## 配置验证

配置完成后，可以通过以下方式进行验证：

1. 检查应用启动日志，确认消息队列连接成功
2. 查看健康检查端点 `/actuator/health`
3. 发送测试消息验证功能正常

## 注意事项

1. 生产环境中建议使用独立的 RabbitMQ 用户和虚拟主机
2. 根据业务量调整连接池和消费者数量
3. 合理设置消息确认和重试机制
4. 定期监控队列长度和消费者状态
5. 配置适当的日志级别以便问题排查