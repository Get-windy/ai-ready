# ERP系统API端点自动化测试框架设计文档

## 文档信息
- **项目**: AI-Ready ERP系统
- **模块**: API测试框架
- **版本**: 1.0.0
- **创建日期**: 2026-05-01
- **创建人**: test-agent-2

## 1. 概述

### 1.1 项目背景
ERP系统采用微服务架构，包含20+业务模块，每个模块都提供RESTful API接口。随着系统复杂度增加，需要一个统一的自动化测试框架来保证API接口的质量和稳定性。

### 1.2 目标
设计并实施一个统一的API端点自动化测试框架，支持：
- 功能测试：验证API业务逻辑正确性
- 性能测试：确保API在高并发场景下的稳定性
- 安全测试：检测API安全漏洞
- 兼容性测试：验证API版本兼容性

## 2. 技术栈分析

### 2.1 当前系统技术栈
基于分析结果：
- **后端框架**: Spring Boot 3.x
- **API规范**: RESTful API
- **认证授权**: Sa-Token
- **API文档**: OpenAPI 3.0 (Swagger)
- **数据库**: PostgreSQL + MyBatis-Plus
- **构建工具**: Maven/Gradle
- **部署环境**: Docker + Kubernetes

### 2.2 测试框架技术选型

#### 2.2.1 功能测试
| 工具 | 版本 | 选择理由 |
|------|------|----------|
| JUnit 5 | 5.10.x | Java标准测试框架，支持参数化测试 |
| TestNG | 7.8.0 | 强大的测试套件管理，支持并发测试 |
| RestAssured | 5.3.2 | 专门用于REST API测试，语法简洁 |
| MockMvc | Spring内置 | Spring Boot应用测试 |
| WireMock | 3.0.0 | 外部依赖服务Mock |

#### 2.2.2 性能测试
| 工具 | 版本 | 选择理由 |
|------|------|----------|
| JMeter | 5.6.3 | 企业级性能测试工具，支持分布式 |
| Gatling | 3.9.5 | 基于Scala的高性能测试工具 |
| Locust | 2.17.0 | Python分布式性能测试工具 |
| k6 | 0.48.0 | Go语言编写，适合云原生环境 |

#### 2.2.3 安全测试
| 工具 | 版本 | 选择理由 |
|------|------|----------|
| OWASP ZAP | 2.14.0 | 开源安全测试工具，支持API安全扫描 |
| Burp Suite | 社区版 | 专业Web安全测试工具 |
| Postman Security Tests | 内置 | 集合安全测试脚本 |

#### 2.2.4 集成与报告
| 工具 | 版本 | 选择理由 |
|------|------|----------|
| Allure Report | 2.24.0 | 美观的测试报告框架 |
| Maven Surefire | 3.1.2 | Maven测试执行插件 |
| Gradle Test | 8.5 | Gradle测试执行 |
| Jenkins/GitHub Actions | - | CI/CD集成 |

## 3. 框架架构设计

### 3.1 整体架构
```
┌─────────────────────────────────────────────────────┐
│                 API测试框架                          │
├─────────────────────────────────────────────────────┤
│  1. 配置管理层    │  2. 测试执行层   │  3. 数据管理层 │
│  - 环境配置      │  - 功能测试     │  - 测试数据     │
│  - 用户认证      │  - 性能测试     │  - Mock数据     │
│  - 测试计划      │  - 安全测试     │  - 数据工厂     │
└─────────────────────────────────────────────────────┘
```

### 3.2 模块设计

#### 3.2.1 Core模块 (核心模块)
- **TestBase**: 基础测试类，封装通用测试逻辑
- **HttpClient**: HTTP客户端封装，支持RestAssured、OkHttp
- **Assertion**: 断言工具集，扩展验证逻辑
- **TestContext**: 测试上下文管理，跨测试用例共享数据

#### 3.2.2 Config模块 (配置模块)
- **EnvironmentConfig**: 环境配置管理（开发/测试/生产）
- **UserConfig**: 用户认证配置，支持多角色测试
- **TestPlanConfig**: 测试计划配置，支持并行执行

#### 3.2.3 Data模块 (数据模块)
- **DataFactory**: 测试数据工厂，生成业务数据
- **MockServer**: Mock服务，模拟外部依赖
- **DatabaseUtil**: 数据库工具，支持数据准备和清理

#### 3.2.4 Report模块 (报告模块)
- **AllureReport**: Allure报告生成和定制
- **JUnitReport**: JUnit报告格式化
- **PerformanceReport**: 性能测试报告分析

## 4. API测试需求分析

### 4.1 ERP系统API分类
基于ERP模块分析，API分为以下类别：

#### 4.1.1 核心业务API
| 模块 | API数量 | 主要接口 | 测试重点 |
|------|---------|----------|----------|
| 采购管理 | 15+ | 采购订单、采购合同、采购询价 | 业务逻辑、数据一致性 |
| 销售管理 | 12+ | 销售订单、客户管理、报价 | 流程完整性、权限控制 |
| 库存管理 | 10+ | 库存查询、出入库管理 | 实时性、并发控制 |
| 财务管理 | 8+ | 应收应付、凭证管理 | 数据准确性、事务处理 |

#### 4.1.2 基础服务API
| 类型 | API数量 | 主要接口 | 测试重点 |
|------|---------|----------|----------|
| 用户认证 | 5+ | 登录、注册、权限验证 | 安全性、性能 |
| 文件管理 | 6+ | 上传、下载、预览 | 大文件处理、稳定性 |
| 消息通知 | 4+ | 邮件、短信、站内信 | 异步处理、可靠性 |

### 4.2 测试类型需求

#### 4.2.1 功能测试
- **业务逻辑验证**: 验证API业务规则正确性
- **异常处理**: 验证边界条件、错误情况处理
- **数据验证**: 验证请求/响应数据结构
- **权限验证**: 验证角色权限控制

#### 4.2.2 性能测试
- **单接口性能**: 响应时间、吞吐量测试
- **并发性能**: 高并发场景下的稳定性测试
- **稳定性测试**: 长时间运行的压力测试
- **资源监控**: CPU、内存、数据库连接监控

#### 4.2.3 安全测试
- **认证授权**: 未授权访问、权限提升测试
- **输入验证**: SQL注入、XSS、CSRF攻击测试
- **敏感数据**: 数据泄露、加密传输测试
- **API安全**: API密钥管理、访问频率限制测试

## 5. 测试框架实施计划

### 5.1 第一阶段：基础框架搭建 (2天)
1. 创建项目结构
2. 实现Core模块基础类
3. 集成RestAssured和JUnit
4. 实现基础测试用例

### 5.2 第二阶段：功能扩展 (3天)
1. 实现配置管理模块
2. 实现数据管理模块
3. 集成Allure报告
4. 实现常用测试工具类

### 5.3 第三阶段：测试用例开发 (5天)
1. 设计测试用例模板
2. 实现核心业务API测试
3. 实现性能测试用例
4. 实现安全测试用例

### 5.4 第四阶段：CI/CD集成 (2天)
1. 集成Maven/Gradle
2. 配置Jenkins/GitHub Actions
3. 实现自动化测试流水线
4. 文档编写和培训

## 6. 目录结构设计

```
erp-api-test-framework/
├── src/
│   ├── main/java/cn/aiedge/erp/test/
│   │   ├── core/                    # 核心模块
│   │   │   ├── TestBase.java
│   │   │   ├── HttpClient.java
│   │   │   ├── Assertion.java
│   │   │   └── TestContext.java
│   │   ├── config/                  # 配置模块
│   │   │   ├── EnvironmentConfig.java
│   │   │   ├── UserConfig.java
│   │   │   └── TestPlanConfig.java
│   │   ├── data/                    # 数据模块
│   │   │   ├── DataFactory.java
│   │   │   ├── MockServer.java
│   │   │   └── DatabaseUtil.java
│   │   └── report/                  # 报告模块
│   │       ├── AllureReport.java
│   │       ├── JUnitReport.java
│   │       └── PerformanceReport.java
│   └── test/java/cn/aiedge/erp/test/
│       ├── functional/              # 功能测试
│       │   ├── purchase/            # 采购模块测试
│       │   ├── sale/                # 销售模块测试
│       │   └── inventory/           # 库存模块测试
│       ├── performance/             # 性能测试
│       │   ├── jmeter/              # JMeter测试
│       │   └── gatling/             # Gatling测试
│       └── security/                # 安全测试
│           ├── owasp/               # OWASP测试
│           └── authentication/      # 认证测试
├── config/
│   ├── environments/
│   │   ├── dev.yaml
│   │   ├── test.yaml
│   │   └── prod.yaml
│   └── users/
│       ├── admin.yaml
│       └── user.yaml
├── scripts/                         # 测试脚本
│   ├── run_functional_tests.sh
│   ├── run_performance_tests.sh
│   └── run_security_tests.sh
└── docs/                            # 文档
    ├── usage.md
    ├── api_test_spec.md
    └── faq.md
```

## 7. 关键实现细节

### 7.1 测试基类设计
```java
public abstract class ApiTestBase {
    // 环境配置
    protected EnvironmentConfig envConfig;
    
    // HTTP客户端
    protected HttpClient httpClient;
    
    // 测试数据
    protected DataFactory dataFactory;
    
    // 前置条件
    @BeforeEach
    void setUp() {
        envConfig = new EnvironmentConfig("test");
        httpClient = new HttpClient(envConfig);
        dataFactory = new DataFactory(envConfig);
    }
    
    // 通用断言方法
    protected void assertStatusCode(int expected) {
        // 状态码断言
    }
    
    protected void assertResponseSchema(String schemaPath) {
        // JSON Schema验证
    }
}
```

### 7.2 测试用例示例
```java
@Test
@DisplayName("创建采购订单 - 成功场景")
public void createPurchaseOrder_success() {
    // 1. 准备测试数据
    PurchaseOrder order = dataFactory.createPurchaseOrder();
    
    // 2. 执行API调用
    ApiResponse<Long> response = httpClient
        .post("/api/erp/purchase/order")
        .body(order)
        .execute();
    
    // 3. 验证结果
    assertStatusCode(200);
    assertNotNull(response.getData());
    assertTrue(response.getData() > 0);
    
    // 4. 验证数据库
    PurchaseOrder savedOrder = databaseUtil
        .getById(PurchaseOrder.class, response.getData());
    assertEquals(order.getSupplierId(), savedOrder.getSupplierId());
}
```

## 8. 验收标准

### 8.1 功能验收标准
1. ✅ 支持所有ERP模块的API测试
2. ✅ 测试用例执行成功率 ≥ 95%
3. ✅ 测试执行时间 ≤ 30分钟（全量测试）
4. ✅ 测试报告可读性高，问题定位清晰

### 8.2 性能验收标准
1. ✅ 支持并发用户数 ≥ 1000
2. ✅ 性能测试结果准确性 ≥ 95%
3. ✅ 资源监控覆盖CPU、内存、数据库

### 8.3 安全验收标准
1. ✅ 覆盖OWASP Top 10安全风险
2. ✅ 安全漏洞检测准确率 ≥ 90%
3. ✅ 敏感数据保护测试完整

## 9. 风险评估与应对

### 9.1 技术风险
| 风险项 | 概率 | 影响 | 应对措施 |
|--------|------|------|----------|
| 技术栈不兼容 | 中 | 高 | 前期技术验证，原型开发 |
| 性能测试准确性 | 中 | 中 | 多工具对比验证 |
| 测试环境稳定性 | 高 | 高 | 环境隔离，备份恢复机制 |

### 9.2 实施风险
| 风险项 | 概率 | 影响 | 应对措施 |
|--------|------|------|----------|
| 开发周期延长 | 中 | 中 | 分阶段实施，定期评审 |
| 团队学习成本 | 高 | 中 | 培训文档，代码示例 |
| 与现有系统集成 | 高 | 高 | 渐进式集成，兼容性测试 |

## 10. 后续规划

### 10.1 短期规划 (1-2周)
1. 完成基础框架开发
2. 实现核心模块测试用例
3. 集成CI/CD流水线

### 10.2 中期规划 (1个月)
1. 完善测试用例覆盖
2. 优化测试执行性能
3. 扩展测试报告功能

### 10.3 长期规划 (3个月)
1. 实现AI智能测试
2. 集成混沌工程测试
3. 建立测试质量指标体系

---

**文档版本历史**
| 版本 | 日期 | 修改内容 | 修改人 |
|------|------|----------|--------|
| 1.0.0 | 2026-05-01 | 初始版本 | test-agent-2 |