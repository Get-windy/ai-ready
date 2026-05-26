# ERP采购询价模块集成测试设计方案

## 1. 项目背景
- **项目**: AI-Ready ERP系统
- **模块**: 采购询价/报价管理模块
- **Sprint**: Sprint 28 - 核心ERP功能补齐专项
- **优先级**: High
- **故事点**: 5 SP
- **预计工时**: 12小时

## 2. 测试目标
开发完整的集成测试套件，覆盖采购询价模块所有端到端业务流程，确保：
1. 模块内组件协同工作的正确性
2. 跨模块集成的数据一致性
3. API接口的稳定性和可靠性
4. 业务流程的完整性和合规性

## 3. 测试范围

### 3.1 业务场景覆盖
1. **采购询价单创建到报价接收完整流程**
   - 创建询价单
   - 添加询价项
   - 发布询价单
   - 供应商报价接收

2. **多供应商报价比较与分析流程**
   - 多供应商报价录入
   - 报价自动比较
   - 最优报价推荐
   - 报价历史记录

3. **采购决策到采购订单生成流程**
   - 报价审批流程
   - 采购决策审批
   - 自动生成采购订单
   - 采购订单状态跟踪

4. **与库存管理模块的集成**
   - 库存可用性检查
   - 自动库存预留
   - 采购到库存入库流程

5. **与财务管理模块的集成**
   - 预算控制检查
   - 付款申请生成
   - 发票匹配流程

### 3.2 技术集成点
1. **数据库层集成** (PostgreSQL)
2. **消息队列集成** (RabbitMQ/Redis)
3. **缓存服务集成** (Redis)
4. **外部服务集成** (供应商系统Mock)
5. **认证授权集成** (JWT/OAuth2)

## 4. 测试架构设计

### 4.1 测试框架选型
```yaml
主要框架:
  - Spring Boot Test: 2.7.x
  - TestContainers: 1.18.x
  - RestAssured: 5.3.x
  - JUnit 5
  - AssertJ
  - Mockito (有限使用)

数据库测试:
  - PostgreSQL TestContainers
  - Flyway/Liquibase 数据库迁移测试

消息队列测试:
  - 嵌入式RabbitMQ
  - Redis嵌入式测试容器

缓存测试:
  - Redis TestContainers
```

### 4.2 测试环境配置
```java
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
public abstract class BaseIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14-alpine");
    
    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3-management-alpine");
    
    @Container
    static RedisContainer redis = new RedisContainer("redis:7-alpine");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // 其他配置...
    }
}
```

## 5. 端到端测试场景设计

### 5.1 场景1: 完整询价-报价-采购流程
```java
@Test
@DisplayName("完整采购流程端到端测试")
void testCompletePurchaseWorkflow() {
    // 1. 创建询价单
    PurchaseInquiry inquiry = createPurchaseInquiry();
    
    // 2. 发布询价单
    publishInquiry(inquiry.getId());
    
    // 3. 供应商报价
    SupplierQuote quote1 = submitSupplierQuote(inquiry.getId(), "supplier1");
    SupplierQuote quote2 = submitSupplierQuote(inquiry.getId(), "supplier2");
    
    // 4. 报价比较分析
    QuoteComparisonResult comparison = compareQuotes(inquiry.getId());
    
    // 5. 选择最优报价
    PurchaseDecision decision = makePurchaseDecision(comparison.getBestQuoteId());
    
    // 6. 生成采购订单
    PurchaseOrder order = generatePurchaseOrder(decision);
    
    // 7. 验证跨模块集成
    verifyInventoryReservation(order);
    verifyBudgetControl(order);
    verifyPaymentApplication(order);
}
```

### 5.2 场景2: 多供应商报价分析流程
```java
@Test
@DisplayName("多供应商报价分析与决策测试")
void testMultiSupplierQuoteAnalysis() {
    // 创建询价单
    PurchaseInquiry inquiry = createInquiryWithMultipleItems();
    
    // 多个供应商提交报价
    List<SupplierQuote> quotes = Arrays.asList(
        submitQuote(inquiry.getId(), "supplierA", 100.00),
        submitQuote(inquiry.getId(), "supplierB", 95.50),
        submitQuote(inquiry.getId(), "supplierC", 102.30)
    );
    
    // 获取报价分析结果
    QuoteAnalysisResult analysis = analyzeQuotes(inquiry.getId());
    
    // 验证分析结果
    assertThat(analysis.getRecommendedSupplier())
        .isEqualTo("supplierB");
    assertThat(analysis.getTotalSavings())
        .isEqualTo(BigDecimal.valueOf(450.00));
    
    // 验证报价历史记录
    verifyQuoteHistory(inquiry.getId(), 3);
}
```

### 5.3 场景3: 跨模块集成验证
```java
@Test
@DisplayName("库存与财务跨模块集成测试")
void testCrossModuleIntegration() {
    // 创建包含库存检查的询价
    PurchaseInquiry inquiry = createInquiryWithInventoryCheck();
    
    // 模拟库存不足场景
    mockInventoryService.stockShortage();
    
    // 验证系统正确处理库存不足
    assertThatThrownBy(() -> publishInquiry(inquiry.getId()))
        .isInstanceOf(InsufficientStockException.class);
    
    // 更新库存后重新测试
    mockInventoryService.restock();
    publishInquiry(inquiry.getId());
    
    // 验证财务预算检查
    mockFinanceService.insufficientBudget();
    assertThatThrownBy(() -> makePurchaseDecision(quoteId))
        .isInstanceOf(BudgetExceededException.class);
}
```

## 6. 测试数据管理

### 6.1 测试数据工厂
```java
public class TestDataFactory {
    
    public PurchaseInquiry createStandardInquiry() {
        return PurchaseInquiry.builder()
            .title("2024年度服务器采购")
            .department("IT部")
            .budget(BigDecimal.valueOf(50000))
            .deadline(LocalDateTime.now().plusDays(30))
            .status(InquiryStatus.DRAFT)
            .build();
    }
    
    public PurchaseInquiryItem createInquiryItem() {
        return PurchaseInquiryItem.builder()
            .productCode("SERVER-X1")
            .productName("企业级服务器")
            .quantity(5)
            .unit("台")
            .estimatedPrice(BigDecimal.valueOf(8000))
            .specifications("配置要求: 32核/128GB/2TB SSD")
            .build();
    }
}
```

### 6.2 数据库初始化和清理
```java
@BeforeEach
void setupDatabase() {
    flyway.clean();
    flyway.migrate();
    insertTestData();
}

@AfterEach
void cleanupDatabase() {
    testDataCleaner.cleanAll();
}

@AfterAll
static void stopContainers() {
    postgres.stop();
    rabbitMQ.stop();
    redis.stop();
}
```

## 7. 集成测试实现计划

### 7.1 Phase 1: 基础设施搭建 (2小时)
1. 配置TestContainers环境
2. 配置数据库迁移测试
3. 配置消息队列测试环境
4. 配置缓存服务测试

### 7.2 Phase 2: 核心业务流程测试 (4小时)
1. 实现询价单生命周期测试
2. 实现报价管理测试
3. 实现采购决策流程测试
4. 实现跨模块集成测试

### 7.3 Phase 3: 异常场景测试 (3小时)
1. 业务异常场景测试
2. 系统异常场景测试
3. 边界条件测试
4. 并发场景测试

### 7.4 Phase 4: 性能与稳定性测试 (3小时)
1. 并发用户测试
2. 大数据量测试
3. 长时间运行测试
4. 资源使用监控

## 8. 验收标准检查清单

- [ ] **测试覆盖率**: 核心业务流程覆盖率 ≥ 95%
- [ ] **集成测试通过率**: 100%
- [ ] **跨模块集成**: 验证所有关键集成点
- [ ] **异常处理**: 覆盖所有业务异常场景
- [ ] **性能要求**: 单业务场景响应时间 < 2秒
- [ ] **数据一致性**: 跨模块数据一致性验证通过
- [ ] **测试报告**: 生成完整的集成测试执行报告
- [ ] **持续集成**: 测试可集成到CI/CD流水线

## 9. 风险与应对措施

### 9.1 技术风险
- **风险**: TestContainers环境配置复杂
- **应对**: 提前验证容器配置，准备备选方案

### 9.2 业务风险
- **风险**: 业务流程理解偏差
- **应对**: 与业务分析师确认测试场景

### 9.3 时间风险
- **风险**: 集成点测试耗时超出预期
- **应对**: 优先实现核心业务流程测试

## 10. 交付物清单

1. **集成测试源代码**: 完整的测试类实现
2. **测试配置文件**: 测试环境配置和容器定义
3. **测试数据**: 可复用的测试数据工厂
4. **测试报告模板**: 集成测试执行报告模板
5. **CI/CD配置**: 集成到CI流水线的测试配置
6. **文档**: 测试设计文档和执行指南

## 11. 后续步骤

1. **立即开始**: 搭建TestContainers测试环境
2. **今日完成**: 实现第一个端到端测试场景
3. **明日目标**: 完成核心业务流程测试
4. **本周完成**: 所有集成测试开发和验证

---

*文档版本: v1.0*
*创建时间: 2026-05-05*
*负责人: test-agent-2*