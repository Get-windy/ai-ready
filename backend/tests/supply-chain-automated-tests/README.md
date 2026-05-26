# 供应链业务流程自动化测试框架

## 项目概述
本测试框架用于ERP系统的供应链业务流程自动化测试，覆盖供应商协同、采购管理、库存管理、批次管理等核心业务流程。

## 目录结构
```
supply-chain-automated-tests/
├── README.md                          # 项目说明
├── pom.xml                            # Maven配置文件
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── aiedge/
│   │   │           └── erp/
│   │   │               └── test/
│   │   │                   ├── framework/           # 测试框架核心
│   │   │                   ├── data/                # 测试数据工厂
│   │   │                   ├── utils/               # 测试工具类
│   │   │                   └── config/              # 测试配置
│   │   └── resources/
│   │       ├── application-test.yml    # 测试配置文件
│   │       ├── db/
│   │       │   └── migration/          # 数据库迁移脚本
│   │       └── test-data/              # 测试数据文件
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── aiedge/
│       │           └── erp/
│       │               └── test/
│       │                   ├── supplier/            # 供应商测试
│       │                   ├── purchase/            # 采购测试
│       │                   ├── inventory/           # 库存测试
│       │                   └── batch/               # 批次测试
│       └── resources/
│           ├── features/               # BDD特性文件
│           └── test-suites/            # 测试套件配置
└── docker/                             # Docker配置
    ├── docker-compose-test.yml         # 测试环境Docker配置
    └── scripts/                        # 测试脚本
```

## 技术栈
- **Java 17**: 开发语言
- **Spring Boot 3.2.5**: 应用框架
- **JUnit 5**: 测试框架
- **RestAssured**: API测试
- **TestContainers**: 集成测试容器
- **Cucumber**: BDD测试
- **Lombok**: 代码简化
- **Maven**: 构建工具

## 快速开始

### 1. 环境要求
- Java 17+
- Maven 3.8+
- Docker (用于TestContainers)

### 2. 运行测试
```bash
# 运行所有测试
mvn test

# 运行特定测试类别
mvn test -Dtest="Supplier*Test"
mvn test -Dtest="Purchase*Test"

# 运行BDD测试
mvn test -Dcucumber.filter.tags="@supplier-registration"

# 生成测试报告
mvn surefire-report:report
```

### 3. 测试覆盖率
```bash
# 生成覆盖率报告
mvn jacoco:report
```

## 测试框架设计

### 3.1 分层测试架构
- **单元测试**: 业务逻辑验证
- **集成测试**: API接口验证
- **端到端测试**: 业务流程验证
- **性能测试**: 系统性能验证

### 3.2 测试数据管理
- 使用工厂模式创建测试数据
- 支持数据隔离和清理
- 支持数据快照和恢复

### 3.3 测试环境
- 本地开发环境
- CI/CD集成环境
- 生产仿真环境

## 业务流程覆盖

### 4.1 供应商协同流程
- 供应商注册流程
- 资质审核流程
- 报价管理流程
- 供应商状态管理

### 4.2 采购管理流程
- 采购申请流程
- 询价比价流程
- 采购订单流程
- 收货验收流程

### 4.3 库存管理流程
- 入库流程
- 出库流程
- 盘点流程
- 调拨流程

### 4.4 批次管理流程
- 批次生成流程
- 批次跟踪流程
- 批次溯源流程
- 批次质量管理

## 持续集成

### 5.1 GitHub Actions配置
测试框架已集成到CI/CD流水线，支持：
- 自动运行测试套件
- 生成测试报告
- 质量门禁检查
- 代码覆盖率检查

### 5.2 质量门禁
- 单元测试覆盖率 ≥ 80%
- 集成测试通过率 100%
- API响应时间 < 500ms
- 无重大安全漏洞

## 扩展和自定义

### 6.1 添加新测试
1. 在相应包下创建测试类
2. 使用测试数据工厂创建测试数据
3. 实现测试逻辑
4. 添加到测试套件

### 6.2 自定义断言
框架提供了丰富的断言工具，支持：
- API响应断言
- 数据库状态断言
- 业务规则断言
- 性能指标断言

## 故障排除

### 常见问题
1. **数据库连接失败**: 检查TestContainers配置
2. **测试数据冲突**: 使用UUID作为唯一标识
3. **测试超时**: 调整测试超时时间
4. **环境配置问题**: 检查application-test.yml配置

### 调试建议
- 启用详细日志输出
- 使用断点调试
- 检查测试数据状态
- 验证环境配置

## 贡献指南
1. Fork项目
2. 创建功能分支
3. 提交更改
4. 创建Pull Request
5. 确保所有测试通过

## 许可证
本项目采用MIT许可证。