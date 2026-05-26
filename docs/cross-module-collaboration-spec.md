# ERP供应商门户跨模块协同机制设计规范

**文档版本**: v1.0  
**创建日期**: 2026年5月1日  
**创建者**: coordinator (项目协调员)  
**关联任务**: task_1777592789156_gut7nj432  
**适用范围**: 供应商门户、批次管理、价格策略、采购合同、库存管理等模块

## 1. 协同机制概述

### 1.1 设计目标
建立统一、高效、可靠的跨模块协同工作机制，确保供应商门户相关各模块之间的：
- **数据一致性**: 确保跨模块数据流转准确无误
- **业务流程连贯性**: 实现端到端业务流程无缝对接
- **系统性能稳定性**: 保证协同操作不影响系统性能
- **故障恢复能力**: 建立完善的错误处理和恢复机制

### 1.2 适用范围
本规范适用于以下核心模块间的协同工作：
1. **供应商门户模块** (product-analyst)
2. **批次管理模块** (team-member)
3. **价格策略模块** (coordinator)
4. **采购合同模块** (ai-mnj0haev)
5. **库存管理模块** (mnj006mb)
6. **供应商协同流程模块** (待分配)
7. **AI智能客服模块** (待分配)

### 1.3 协同原则
1. **松耦合高内聚**: 模块间接口清晰，内部实现独立
2. **事件驱动优先**: 优先使用异步事件驱动协同
3. **数据最终一致性**: 业务场景允许时采用最终一致性
4. **可追溯可监控**: 所有协同操作可追溯、可监控
5. **容错与降级**: 协同失败时系统仍能部分工作

## 2. 跨模块接口规范设计

### 2.1 接口标准

#### 2.1.1 RESTful API 规范
```yaml
标准格式:
  - 协议: HTTPS
  - 数据格式: JSON
  - 字符编码: UTF-8
  - 版本控制: URL路径版本化 (/api/v1/)

请求头:
  - Content-Type: application/json
  - Authorization: Bearer {token}
  - X-Request-ID: {uuid}
  - X-Module-Source: {module_name}

响应格式:
  - 成功: { "code": 200, "message": "success", "data": {...} }
  - 失败: { "code": {error_code}, "message": "error_message", "data": null }
```

#### 2.1.2 核心接口定义

**接口1: 供应商资质验证接口**
```yaml
接口: GET /api/v1/suppliers/{supplierId}/qualifications
描述: 获取供应商资质信息用于批次管理决策
提供者: 供应商门户模块
消费者: 批次管理模块
请求参数:
  - supplierId: 供应商ID (路径参数)
响应数据:
  - qualifications: 资质列表
  - verificationStatus: 验证状态
  - validUntil: 有效期
调用时机: 批次创建前、批次分配时
```

**接口2: 供应商报价同步接口**
```yaml
接口: POST /api/v1/prices/supplier-quotes/sync
描述: 同步供应商报价到价格策略模块
提供者: 供应商门户模块  
消费者: 价格策略模块
请求数据:
  - supplierId: 供应商ID
  - quoteItems: 报价项列表
  - validFrom: 有效期开始
  - validTo: 有效期结束
响应数据:
  - syncStatus: 同步状态
  - processedCount: 处理数量
  - errors: 错误信息
调用时机: 供应商更新报价时、定时同步任务
```

**接口3: 批次库存关联接口**
```yaml
接口: POST /api/v1/batches/{batchId}/inventory-link
描述: 将批次与库存管理模块关联
提供者: 批次管理模块
消费者: 库存管理模块
请求数据:
  - batchId: 批次ID (路径参数)
  - inventoryLocation: 库存位置
  - quantity: 数量
  - unit: 计量单位
响应数据:
  - inventoryId: 库存记录ID
  - locationStatus: 位置状态
  - linkedAt: 关联时间
调用时机: 批次创建完成时、库存位置变更时
```

**接口4: 合同供应商信息查询接口**
```yaml
接口: GET /api/v1/contracts/supplier-info
描述: 采购合同模块查询供应商详细信息
提供者: 供应商门户模块
消费者: 采购合同模块
请求参数:
  - supplierIds: 供应商ID列表
  - fields: 需要返回的字段
响应数据:
  - suppliers: 供应商列表
  - statistics: 统计信息
  - lastUpdated: 最后更新时间
调用时机: 合同创建时、合同评审时
```

### 2.2 消息协议与事件定义

#### 2.2.1 事件驱动架构
```
事件发布 → 消息队列 → 事件订阅 → 业务处理
```

#### 2.2.2 核心事件定义

**事件1: 供应商资质更新事件**
```json
{
  "eventId": "supplier-qualification-updated",
  "version": "1.0",
  "timestamp": "2026-05-01T08:00:00Z",
  "source": "supplier-portal",
  "data": {
    "supplierId": "SUP001",
    "qualificationType": "ISO9001",
    "oldStatus": "pending",
    "newStatus": "approved",
    "validFrom": "2026-05-01",
    "validTo": "2027-05-01"
  }
}
```

**事件2: 批次状态变更事件**
```json
{
  "eventId": "batch-status-changed",
  "version": "1.0",
  "timestamp": "2026-05-01T08:15:00Z",
  "source": "batch-management",
  "data": {
    "batchId": "BATCH001",
    "oldStatus": "created",
    "newStatus": "processing",
    "operatorId": "USER001",
    "changeReason": "开始处理"
  }
}
```

**事件3: 价格策略计算完成事件**
```json
{
  "eventId": "price-calculation-completed",
  "version": "1.0",
  "timestamp": "2026-05-01T08:30:00Z",
  "source": "price-strategy",
  "data": {
    "calculationId": "CALC001",
    "batchId": "BATCH001",
    "supplierId": "SUP001",
    "totalPrice": 150000.00,
    "currency": "CNY",
    "calculationTime": 2350
  }
}
```

## 3. 业务流程协同设计

### 3.1 端到端业务流程

#### 3.1.1 供应商报价到批次分配流程
```
1. 供应商提交报价 (供应商门户)
   ↓
2. 报价验证与同步 (事件: supplier-quote-updated)
   ↓
3. 价格策略计算 (价格策略模块)
   ↓
4. 批次创建准备 (批次管理模块)
   ↓
5. 供应商资质验证 (接口: GET /suppliers/{id}/qualifications)
   ↓
6. 批次创建与分配 (批次管理模块)
   ↓
7. 通知供应商 (事件: batch-assigned-to-supplier)
```

#### 3.1.2 批次完成到库存入库流程
```
1. 供应商交付完成 (供应商门户)
   ↓
2. 批次状态更新 (事件: batch-delivery-completed)
   ↓
3. 库存位置分配 (库存管理模块)
   ↓
4. 批次与库存关联 (接口: POST /batches/{id}/inventory-link)
   ↓
5. 入库确认与库存更新 (库存管理模块)
   ↓
6. 支付触发 (事件: inventory-updated-ready-for-payment)
```

### 3.2 数据一致性保障机制

#### 3.2.1 强一致性场景 (分布式事务)
```java
// 示例: 批次创建与库存分配事务
@GlobalTransactional
public BatchCreateResult createBatchWithInventory(BatchCreateRequest request) {
    // 1. 创建批次
    Batch batch = batchService.create(request);
    
    // 2. 分配库存位置
    InventoryLocation location = inventoryService.allocate(batch);
    
    // 3. 关联批次与库存
    batch.setInventoryLocation(location);
    batchService.update(batch);
    
    // 4. 发送通知事件
    eventPublisher.publish(new BatchCreatedEvent(batch));
    
    return new BatchCreateResult(batch, location);
}
```

#### 3.2.2 最终一致性场景 (事件驱动)
```java
// 示例: 供应商报价同步
@Transactional
public void syncSupplierQuote(SupplierQuote quote) {
    // 1. 本地保存报价
    supplierQuoteRepository.save(quote);
    
    // 2. 发布报价更新事件
    SupplierQuoteUpdatedEvent event = new SupplierQuoteUpdatedEvent(quote);
    messageQueue.publish("supplier-quote-updated", event);
    
    // 3. 返回成功，不等待下游处理
    // 下游价格策略模块监听事件并异步处理
}
```

## 4. 系统集成架构设计

### 4.1 微服务通信机制

#### 4.1.1 服务注册与发现
```yaml
# Consul/Nacos 配置
services:
  - name: supplier-portal
    port: 8080
    health-check: /actuator/health
    tags: [portal, supplier]
    
  - name: batch-management
    port: 8081
    health-check: /actuator/health
    tags: [batch, management]
    
  - name: price-strategy
    port: 8082
    health-check: /actuator/health
    tags: [price, strategy]
```

#### 4.1.2 负载均衡策略
```java
// Feign客户端配置
@FeignClient(
    name = "supplier-portal",
    fallback = SupplierPortalFallback.class,
    configuration = LoadBalancerConfiguration.class
)
public interface SupplierPortalClient {
    @GetMapping("/api/v1/suppliers/{id}")
    Supplier getSupplier(@PathVariable("id") String id);
    
    @GetMapping("/api/v1/suppliers/{id}/qualifications")
    SupplierQualifications getQualifications(@PathVariable("id") String id);
}
```

### 4.2 分布式事务处理

#### 4.2.1 Seata分布式事务配置
```yaml
# seata配置
seata:
  enabled: true
  application-id: supplier-portal
  tx-service-group: default_tx_group
  service:
    vgroup-mapping:
      default_tx_group: default
    grouplist:
      default: 127.0.0.1:8091
  config:
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
  registry:
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
```

#### 4.2.2 Saga模式实现
```java
// 长事务流程的Saga实现
@Component
public class BatchCreationSaga {
    
    @SagaStart
    public BatchCreationSagaResult startBatchCreation(BatchCreationRequest request) {
        // 步骤1: 创建批次
        Batch batch = batchService.createBatch(request);
        
        // 步骤2: 分配库存（可补偿操作）
        InventoryLocation location = inventoryService.allocateLocation(batch);
        
        // 步骤3: 通知供应商（可补偿操作）
        notificationService.notifySupplier(batch);
        
        return new BatchCreationSagaResult(batch, location);
    }
    
    @Compensate
    public void compensateBatchCreation(BatchCreationRequest request) {
        // 补偿逻辑
        inventoryService.releaseLocation(request.getBatchId());
        notificationService.cancelNotification(request.getBatchId());
    }
}
```

## 5. 用户操作体验协同

### 5.1 统一操作界面规范

#### 5.1.1 界面组件标准
```yaml
组件标准:
  - 数据表格: 统一分页、排序、筛选
  - 表单验证: 统一验证规则和提示
  - 操作按钮: 统一状态和样式
  - 消息提示: 统一成功/错误提示
  
状态同步:
  - 操作进度显示: 跨模块操作进度统一显示
  - 数据实时刷新: 关键数据变化实时更新
  - 错误状态同步: 错误信息跨模块同步显示
```

#### 5.1.2 用户权限统一管理
```java
// 统一权限检查
@Component
public class UnifiedPermissionChecker {
    
    public boolean checkModulePermission(String userId, String module, String action) {
        // 1. 检查全局权限
        boolean globalPermission = authService.checkGlobalPermission(userId, action);
        
        // 2. 检查模块特定权限
        boolean modulePermission = modulePermissionService.check(userId, module, action);
        
        // 3. 检查数据权限
        boolean dataPermission = dataPermissionService.check(userId, module);
        
        return globalPermission && modulePermission && dataPermission;
    }
}
```

## 6. 监控与运维协同

### 6.1 系统性能监控

#### 6.1.1 跨模块调用监控
```yaml
监控指标:
  - 接口响应时间: 各模块接口平均响应时间
  - 调用成功率: 跨模块调用成功比例
  - 错误率统计: 按模块、接口统计错误率
  - 并发调用数: 实时并发调用数量
  
告警规则:
  - 响应时间>2s: 警告级别
  - 错误率>5%: 严重级别
  - 调用成功率<95%: 警告级别
```

#### 6.1.2 分布式链路追踪
```java
// Sleuth + Zipkin 配置
@Configuration
public class TracingConfiguration {
    
    @Bean
    public Sampler defaultSampler() {
        return Sampler.ALWAYS_SAMPLE;
    }
    
    @Bean
    public Brave brave() {
        return new Brave.Builder("supplier-portal")
                .traceSampler(Sampler.ALWAYS_SAMPLE)
                .build();
    }
}
```

### 6.2 故障诊断与恢复

#### 6.2.1 故障诊断流程
```
1. 监控告警触发
   ↓
2. 调用链分析 (Zipkin/Kibana)
   ↓
3. 日志聚合分析 (ELK Stack)
   ↓
4. 模块健康检查 (Spring Boot Actuator)
   ↓
5. 根因定位与修复
   ↓
6. 恢复验证与监控
```

#### 6.2.2 自动化恢复策略
```yaml
恢复策略:
  - 熔断降级: Hystrix/Sentinel熔断配置
  - 自动重试: 可重试错误的自动重试机制
  - 数据补偿: 事务失败后的数据补偿
  - 服务切换: 主备服务自动切换
```

## 7. 实施计划与验收标准

### 7.1 实施阶段

#### 阶段1: 基础设施建立 (5月1日-5月5日)
- ✅ 接口规范文档完成
- 🔄 消息队列配置
- ⏳ 服务注册中心部署
- ⏳ 监控系统搭建

#### 阶段2: 核心接口实现 (5月6日-5月12日)
- ⏳ 供应商相关接口实现
- ⏳ 批次管理接口实现
- ⏳ 价格策略接口实现
- ⏳ 采购合同接口实现

#### 阶段3: 集成测试验证 (5月13日-5月15日)
- ⏳ 端到端业务流程测试
- ⏳ 性能与压力测试
- ⏳ 故障恢复测试
- ⏳ 用户体验测试

### 7.2 验收标准

#### 7.2.1 技术验收标准
- [ ] 完成≥10个核心跨模块接口的设计与文档
- [ ] 实现事件驱动架构，支持≥5种核心业务事件
- [ ] 建立分布式事务处理机制，支持强一致性和最终一致性
- [ ] 实现统一监控和链路追踪，监控覆盖率≥90%

#### 7.2.2 业务验收标准
- [ ] 支持≥5个端到端业务流程的无缝协同
- [ ] 实现跨模块数据一致性，数据同步延迟<5秒
- [ ] 用户操作界面统一，操作体验一致
- [ ] 系统故障恢复时间<10分钟

#### 7.2.3 运维验收标准
- [ ] 建立完善的监控告警体系
- [ ] 实现自动化故障诊断和恢复
- [ ] 系统可用性≥99.9%
- [ ] 平均故障恢复时间(MTTR)<30分钟

---

**文档维护说明**:
1. 本规范为供应商门户跨模块协同的指导性文档
2. 各模块开发需遵循本规范进行接口设计和实现
3. 规范变更需经过技术评审委员会审批
4. 定期审查和更新本规范以适应业务发展

**生效日期**: 2026年5月1日  
**评审周期**: 每月一次  
**责任人**: coordinator (项目协调员)