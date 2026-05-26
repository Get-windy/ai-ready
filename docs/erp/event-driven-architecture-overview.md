# ERP事件驱动架构概述

## 1. 架构目标

### 1.1 核心目标
- **高可扩展性**：支持水平扩展，应对ERP系统高并发业务场景
- **松耦合架构**：降低模块间依赖，提高系统可维护性
- **领域模型清晰度**：通过事件明确业务边界和职责划分
- **异步处理能力**：提升系统吞吐量和响应性能

### 1.2 业务价值
- **实时业务响应**：事件驱动的实时业务处理
- **系统可观察性**：完整的事件跟踪和审计能力
- **灵活集成**：易于与外部系统集成
- **容错能力**：事件重放和故障恢复机制

## 2. 核心架构组件

### 2.1 事件基础设施层
```
┌─────────────────────────────────────────┐
│           事件基础设施层                 │
├─────────────────────────────────────────┤
│  • 事件存储（Event Store）              │
│  • 消息队列（Message Queue）            │
│  • 事件总线（Event Bus）                │
│  • 事件注册表（Event Registry）         │
└─────────────────────────────────────────┘
```

### 2.2 事件处理层
```
┌─────────────────────────────────────────┐
│           事件处理层                     │
├─────────────────────────────────────────┤
│  • 事件发布器（Event Publisher）        │
│  • 事件订阅器（Event Subscriber）       │
│  • 事件处理器（Event Handler）          │
│  • 事件路由（Event Router）             │
└─────────────────────────────────────────┘
```

### 2.3 业务领域层
```
┌─────────────────────────────────────────┐
│           业务领域层                     │
├─────────────────────────────────────────┤
│  • 采购领域（Purchase Domain）          │
│  • 销售领域（Sales Domain）             │
│  • 库存领域（Inventory Domain）         │
│  • 财务领域（Finance Domain）           │
│  • 生产领域（Manufacturing Domain）     │
└─────────────────────────────────────────┘
```

## 3. 事件分类体系

### 3.1 按业务领域分类
| 领域 | 核心事件 | 描述 |
|------|----------|------|
| **采购** | PurchaseOrderCreated | 采购订单创建事件 |
|  | PurchaseOrderApproved | 采购订单批准事件 |
|  | PurchaseOrderReceived | 采购订单收货事件 |
|  | PurchaseOrderPaid | 采购订单支付事件 |
| **销售** | SalesOrderCreated | 销售订单创建事件 |
|  | SalesOrderShipped | 销售订单发货事件 |
|  | SalesOrderInvoiced | 销售订单开票事件 |
|  | SalesOrderPaid | 销售订单收款事件 |
| **库存** | StockIncremented | 库存增加事件 |
|  | StockDecremented | 库存减少事件 |
|  | StockAdjusted | 库存调整事件 |
|  | StockReserved | 库存预留事件 |
| **财务** | InvoiceIssued | 发票开具事件 |
|  | PaymentReceived | 收款事件 |
|  | ExpenseRecorded | 费用记录事件 |
|  | FinancialReportGenerated | 财务报表生成事件 |

### 3.2 按事件重要性分类
- **关键事件（Critical）**：直接影响业务流程，必须保证投递
- **重要事件（Important）**：影响业务逻辑，需要及时处理
- **普通事件（Normal）**：记录性事件，可延迟处理
- **审计事件（Audit）**：用于审计跟踪，长期存储

### 3.3 按事件处理方式分类
- **命令事件（Command Events）**：触发业务操作
- **状态事件（State Events）**：反映业务状态变化
- **集成事件（Integration Events）**：跨服务通信
- **领域事件（Domain Events）**：领域内业务状态变更

## 4. 事件数据格式标准

### 4.1 事件元数据格式
```json
{
  "eventId": "evt_1234567890abcdef",
  "eventType": "PurchaseOrderCreated",
  "eventVersion": "1.0",
  "timestamp": "2026-05-04T16:48:00Z",
  "source": "purchase-service",
  "correlationId": "corr_1234567890abcdef",
  "causationId": "cause_1234567890abcdef",
  "tenantId": "tenant_001",
  "userId": "user_001",
  "metadata": {
    "traceId": "trace_1234567890abcdef",
    "spanId": "span_1234567890abcdef"
  }
}
```

### 4.2 事件数据格式
```json
{
  "eventId": "evt_1234567890abcdef",
  "eventType": "PurchaseOrderCreated",
  "timestamp": "2026-05-04T16:48:00Z",
  "payload": {
    "purchaseOrderId": "PO-2026-001",
    "supplierId": "SUP-001",
    "orderDate": "2026-05-04",
    "totalAmount": 15000.00,
    "currency": "CNY",
    "items": [
      {
        "itemId": "ITEM-001",
        "quantity": 100,
        "unitPrice": 100.00,
        "totalPrice": 10000.00
      },
      {
        "itemId": "ITEM-002",
        "quantity": 50,
        "unitPrice": 100.00,
        "totalPrice": 5000.00
      }
    ]
  },
  "metadata": {
    "source": "purchase-service",
    "version": "1.0",
    "tenantId": "tenant_001"
  }
}
```

### 4.3 事件版本控制
- **Major版本**：不兼容的变更（如字段删除、类型变更）
- **Minor版本**：向后兼容的功能新增（如新增可选字段）
- **Patch版本**：向后兼容的错误修复

## 5. 事件处理流程

### 5.1 事件生命周期
```
1. 事件产生 → 2. 事件发布 → 3. 事件存储 → 4. 事件路由
     ↓                                           ↓
5. 事件订阅 ← 6. 事件处理 ← 7. 事件确认 ← 8. 事件完成
```

### 5.2 事件处理模式

#### 5.2.1 简单处理模式
```java
// 简单事件处理器示例
@Component
public class PurchaseOrderCreatedHandler implements EventHandler<PurchaseOrderCreated> {
    
    @Override
    public void handle(PurchaseOrderCreated event) {
        // 1. 验证事件
        validateEvent(event);
        
        // 2. 执行业务逻辑
        processPurchaseOrder(event);
        
        // 3. 更新相关状态
        updateInventory(event);
        
        // 4. 发送通知
        sendNotification(event);
    }
}
```

#### 5.2.2 Saga模式（长流程）
```java
// Saga协调器示例
@Component
public class PurchaseOrderSaga {
    
    @StartSaga
    public void handlePurchaseOrderCreated(PurchaseOrderCreated event) {
        // 开始采购订单处理Saga
    }
    
    @SagaEventHandler(associationProperty = "purchaseOrderId")
    public void handleInventoryReserved(InventoryReserved event) {
        // 库存预留成功后，触发供应商通知
    }
    
    @SagaEventHandler(associationProperty = "purchaseOrderId")
    public void handleSupplierNotified(SupplierNotified event) {
        // 供应商通知成功后，触发物流安排
    }
    
    @EndSaga
    public void handleOrderFulfilled(OrderFulfilled event) {
        // 订单完成，结束Saga
    }
}
```

## 6. 技术选型建议

### 6.1 消息队列
| 技术 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| **Apache Kafka** | 高吞吐、持久化、分布式 | 运维复杂、配置复杂 | 大规模事件流、日志收集 |
| **RabbitMQ** | 协议丰富、管理方便、成熟稳定 | 单点性能有限 | 业务消息、RPC调用 |
| **Apache RocketMQ** | 低延迟、高可用、事务消息 | 社区相对较小 | 电商、金融交易 |
| **Apache Pulsar** | 云原生、分层存储、多租户 | 相对较新 | 多租户、云环境 |

### 6.2 事件存储
- **EventStoreDB**：专门的事件存储数据库
- **PostgreSQL + JSONB**：传统关系数据库 + JSON存储
- **MongoDB**：文档数据库，灵活存储事件
- **Apache Cassandra**：分布式NoSQL，适合大规模事件

### 6.3 框架支持
- **Spring Cloud Stream**：Spring生态系统集成
- **Axon Framework**：专业CQRS/EventSourcing框架
- **Eventuate**：微服务事件驱动框架
- **自定义框架**：基于Spring Boot + Kafka/RabbitMQ

## 7. 实施建议

### 7.1 分阶段实施
1. **阶段1（基础）**：核心业务事件建模和发布
2. **阶段2（扩展）**：事件存储和重放机制
3. **阶段3（优化）**：事件溯源和CQRS集成
4. **阶段4（完善）**：监控、告警和运维工具

### 7.2 迁移策略
- **并行运行**：新旧系统并行，逐步迁移
- **事件驱动包装**：将现有API包装为事件
- **增量迁移**：按业务模块逐步迁移
- **数据同步**：保持新旧系统数据一致性

### 7.3 性能考虑
- **事件压缩**：压缩重复或相似事件
- **批量处理**：批量处理提高吞吐量
- **异步处理**：非关键路径异步化
- **缓存策略**：热点事件数据缓存

## 8. 监控和运维

### 8.1 关键指标
- **事件吞吐量**：每秒处理事件数
- **事件延迟**：事件产生到处理完成时间
- **处理成功率**：成功处理的事件比例
- **事件积压**：未处理事件队列长度

### 8.2 监控工具
- **Prometheus + Grafana**：指标监控和可视化
- **Jaeger/Zipkin**：分布式追踪
- **ELK Stack**：日志收集和分析
- **自定义监控**：业务特定监控

### 8.3 运维工具
- **事件重放工具**：重放历史事件
- **事件修复工具**：修复损坏事件
- **事件归档工具**：归档历史事件
- **事件审计工具**：事件审计和合规

## 9. 风险评估和缓解

### 9.1 技术风险
| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 事件丢失 | 数据不一致、业务中断 | 持久化存储、确认机制、重试策略 |
| 事件重复 | 重复业务操作、数据错误 | 幂等性设计、去重机制 |
| 处理延迟 | 业务响应慢、用户体验差 | 性能监控、自动扩容、异步处理 |
| 系统故障 | 服务不可用、数据丢失 | 高可用架构、备份恢复、容灾设计 |

### 9.2 业务风险
- **业务逻辑复杂度增加**：需要详细的事件建模
- **团队技能要求提高**：需要掌握事件驱动架构
- **系统调试难度增加**：分布式调试更复杂
- **数据一致性挑战**：最终一致性需要业务适应

## 10. 总结

ERP事件驱动架构通过将业务状态变更建模为事件，实现了系统的高可扩展性、松耦合和领域模型清晰度。本方案提供了完整的事件分类体系、数据格式标准、处理流程和技术选型建议，为ERP系统的现代化改造提供了可行的技术路径。

**下一步行动**：
1. 完成事件分类体系详细设计
2. 设计事件存储方案
3. 设计消息队列集成方案
4. 制定分阶段实施计划