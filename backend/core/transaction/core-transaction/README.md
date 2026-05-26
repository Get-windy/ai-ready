# 企智连分布式事务模块 (Core-Transaction)

## 概述

企智连分布式事务模块是AI-Ready项目的核心组件之一，提供可靠的分布式事务管理能力。该模块支持多种事务模式，包括TCC、Saga和最大努力通知，能够有效解决微服务架构下的数据一致性问题。

## 功能特性

- **多种事务模式**：支持TCC、Saga、最大努力通知等多种分布式事务模式
- **事务协调器**：提供全局事务管理和分支事务管理
- **事务日志存储**：持久化事务状态和执行历史
- **事务状态管理**：实时监控和管理事务状态
- **事务超时处理**：自动检测和处理超时事务
- **事务异常恢复**：支持事务补偿和恢复机制
- **AOP集成**：通过注解方式透明地使用分布式事务

## 事务模式说明

### 1. TCC模式 (Try-Confirm-Cancel)
- **适用场景**：需要强一致性的短事务
- **特点**：两阶段提交，需要业务方实现Try、Confirm、Cancel三个方法
- **优势**：性能较好，一致性强
- **劣势**：对业务侵入性较大

### 2. Saga模式
- **适用场景**：长事务、业务流程复杂的场景
- **特点**：正向执行事务，反向补偿异常情况
- **优势**：对业务侵入性较小，适合长事务
- **劣势**：补偿逻辑设计复杂

### 3. 最大努力通知模式
- **适用场景**：最终一致性要求的异步场景
- **特点**：通过不断重试确保消息到达
- **优势**：实现简单，适合异步场景
- **劣势**：不能保证强一致性

## 快速开始

### 1. 添加依赖

在项目的 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>cn.aiedge</groupId>
    <artifactId>core-transaction</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 2. 启用分布式事务

在Spring Boot启动类上添加注解：

```java
@SpringBootApplication
@EnableDistributedTransaction  // 启用分布式事务
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. 使用分布式事务

#### TCC模式示例：

```java
@Service
public class OrderService {

    @DistributedTransaction(
        name = "order-process-transaction",
        mode = TransactionMode.TCC,
        timeout = 60,
        maxRetries = 3
    )
    public boolean processOrder(Order order) {
        // 业务逻辑
        return true;
    }
}
```

#### Saga模式示例：

```java
@Service
public class CustomerService {

    @DistributedTransaction(
        name = "customer-register-saga",
        mode = TransactionMode.SAGA,
        timeout = 120,
        maxRetries = 2
    )
    public boolean registerCustomer(Customer customer) {
        // 业务逻辑
        return true;
    }
}
```

## 核心组件

### 1. 事务管理器 (TransactionManager)
- 负责全局事务的生命周期管理
- 提供事务的开始、提交、回滚等操作

### 2. TCC事务管理器 (TccTransactionManager)
- 实现TCC模式的事务管理
- 提供Try、Confirm、Cancel阶段的操作

### 3. Saga事务管理器 (SagaTransactionManager)
- 实现Saga模式的事务管理
- 提供正向执行和反向补偿操作

### 4. 最大努力通知事务管理器 (BestEffortNotifyTransactionManager)
- 实现最大努力通知模式
- 提供重试和通知机制

### 5. 事务日志 (DistributedTransactionLog)
- 记录事务的执行状态和历史
- 支持事务恢复和监控

## 配置说明

### application.yml 配置示例：

```yaml
aiedge:
  transaction:
    # TCC模式配置
    tcc:
      default-timeout: 60s
      max-retries: 3
    # Saga模式配置
    saga:
      default-timeout: 300s
      max-retries: 5
    # 最大努力通知配置
    best-effort:
      default-timeout: 600s
      max-retries: 10
      retry-interval: 1000ms
      backoff-multiplier: 2.0
```

## 最佳实践

1. **选择合适的事务模式**：根据业务场景选择最适合的事务模式
2. **合理设置超时时间**：避免长时间持有事务资源
3. **设计幂等的补偿逻辑**：确保补偿操作可以重复执行
4. **监控事务状态**：及时发现和处理异常事务
5. **合理使用重试机制**：避免无效重试造成的资源浪费

## 监控和运维

### 事务状态查询
- 通过API查询事务的当前状态
- 监控事务的成功率和失败率

### 异常事务处理
- 提供手动重试失败事务的功能
- 支持超时事务的自动处理

### 性能监控
- 监控事务的执行时间
- 统计事务的吞吐量和延迟

## 注意事项

1. 事务注解只能用于public方法
2. 避免在事务方法中调用同类的事务方法（可能导致AOP失效）
3. 合理设计补偿逻辑，确保数据一致性
4. 定期清理事务日志，避免数据膨胀

## 扩展性

模块设计具有良好的扩展性：
- 支持新增事务模式
- 支持自定义事务管理器
- 支持自定义事务日志存储
- 支持自定义监控和告警

## 未来发展

- 集成更多的分布式事务协议
- 提供可视化的事务管理界面
- 增强事务性能分析能力
- 集成AI驱动的事务优化