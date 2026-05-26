# 企智连分布式事务模块配置说明

## 配置概述

企智连分布式事务模块提供了丰富的配置选项，可以通过 `application.yml` 或 `application.properties` 文件进行配置。

## 配置项详解

### 基础配置

```yaml
aiedge:
  transaction:
    enabled: true                    # 是否启用分布式事务，默认true
    default-mode: TCC               # 默认事务模式，可选TCC/SAGA/BEST_EFFORT
    max-global-transactions: 1000   # 最大全局事务数
    max-branch-transactions: 10000  # 最大分支事务数
```

### TCC模式配置

```yaml
aiedge:
  transaction:
    tcc:
      default-timeout: 60           # 默认超时时间（秒）
      max-retries: 3                # 最大重试次数
      retry-interval: 1000          # 重试间隔（毫秒）
      backoff-multiplier: 2.0       # 退避倍数
      concurrent-execution-limit: 100 # 并发执行限制
```

### Saga模式配置

```yaml
aiedge:
  transaction:
    saga:
      default-timeout: 300          # 默认超时时间（秒）
      max-retries: 5                # 最大重试次数
      retry-interval: 2000          # 重试间隔（毫秒）
      backoff-multiplier: 1.5       # 退避倍数
      max-steps: 20                 # 最大步骤数
      enable-auto-compensation: true # 是否启用自动补偿
```

### 最大努力通知配置

```yaml
aiedge:
  transaction:
    best-effort:
      default-timeout: 600          # 默认超时时间（秒）
      max-retries: 10               # 最大重试次数
      retry-interval: 1000          # 重试间隔（毫秒）
      backoff-multiplier: 2.0       # 退避倍数
      notify-threads: 10            # 通知线程数
      batch-size: 50                # 批量处理大小
```

### 事务日志配置

```yaml
aiedge:
  transaction:
    log:
      retention-days: 30            # 日志保留天数
      cleanup-cron: "0 0 2 * * ?"  # 清理任务执行时间（每天凌晨2点）
      max-log-size: 100MB           # 单个日志文件最大大小
      enable-archive: true          # 是否启用归档
      archive-retention-days: 90    # 归档日志保留天数
```

### 数据库配置

```yaml
spring:
  datasource:
    # 主数据源（用于业务数据）
    primary:
      url: jdbc:mysql://localhost:3306/ai_ready_db
      username: root
      password: password
      driver-class-name: com.mysql.cj.jdbc.Driver
    
    # 事务日志数据源（可选，用于事务日志分离）
    transaction-log:
      url: jdbc:mysql://localhost:3306/transaction_log_db
      username: root
      password: password
      driver-class-name: com.mysql.cj.jdbc.Driver
```

### Redis配置（用于事务状态缓存）

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: 
    database: 0
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: 1000ms

aiedge:
  transaction:
    cache:
      enabled: true                 # 是否启用缓存
      ttl: 3600s                    # 缓存TTL（秒）
      prefix: "dist_tx:"           # 缓存键前缀
```

### 监控配置

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,dist-tx-stats  # 暴露的监控端点
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true
```

### 事务协调器配置

```yaml
aiedge:
  transaction:
    coordinator:
      heartbeat-interval: 10000     # 心跳间隔（毫秒）
      failure-threshold: 3          # 失败阈值
      recovery-interval: 30000      # 恢复检查间隔（毫秒）
      cleanup-interval: 60000       # 清理间隔（毫秒）
```

## 高级配置

### 自定义事务管理器

```yaml
aiedge:
  transaction:
    custom:
      managers:
        - name: custom-tcc-manager
          class: com.example.CustomTccTransactionManager
          properties:
            custom-property: value
```

### 事件监听器配置

```yaml
aiedge:
  transaction:
    listeners:
      - type: TRANSACTION_STARTED
        class: com.example.TransactionStartedListener
      - type: TRANSACTION_COMPLETED
        class: com.example.TransactionCompletedListener
```

## 集群配置

```yaml
spring:
  redis:
    sentinel:
      master: mymaster
      nodes: 
        - sentinel1:26379
        - sentinel2:26379
        - sentinel3:26379

aiedge:
  transaction:
    cluster:
      enabled: true                 # 启用集群模式
      node-id: node-1               # 节点ID
      election-timeout: 5000        # 选举超时时间（毫秒）
      sync-mode: ASYNC              # 同步模式：SYNC/ASYNC
```

## 安全配置

```yaml
aiedge:
  transaction:
    security:
      encrypt-data: true            # 是否加密敏感数据
      audit-enabled: true           # 是否启用审计
      access-control: true          # 是否启用访问控制
```

## 性能调优配置

```yaml
aiedge:
  transaction:
    performance:
      thread-pool:
        core-size: 10               # 核心线程数
        max-size: 50                # 最大线程数
        queue-capacity: 200         # 队列容量
        keep-alive: 60s             # 空闲线程存活时间
      
      batch-processing:
        enabled: true               # 启用批处理
        batch-size: 100             # 批处理大小
        flush-interval: 1000ms      # 刷新间隔
```

## 配置验证

配置完成后，可以通过以下方式进行验证：

1. 检查应用启动日志，确认分布式事务模块加载成功
2. 查看健康检查端点 `/actuator/health`
3. 执行一个简单的分布式事务测试

## 注意事项

1. 生产环境中建议使用独立的事务日志数据库
2. 根据业务量调整线程池和连接池大小
3. 合理设置超时时间避免长时间阻塞
4. 定期监控事务日志表大小
5. 配置适当的日志级别以便问题排查
6. 在高并发场景下，适当调整并发控制参数