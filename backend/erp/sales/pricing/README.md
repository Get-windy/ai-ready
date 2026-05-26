# 销售价格策略管理模块

## 概述

销售价格策略管理模块是企智连ERP系统的核心定价引擎，集成了Drools规则引擎，支持复杂多维度定价场景。该模块提供了灵活的定价策略配置、实时价格计算和定价策略管理功能。

## 功能特性

### 核心功能
- ✅ **价格策略管理**：完整的CRUD操作，支持策略创建、更新、删除、激活/停用
- ✅ **Drools规则引擎集成**：支持动态规则加载、语法验证、规则管理
- ✅ **多维度定价**：支持客户等级、区域、产品类别、时间、数量等多维度价格策略
- ✅ **实时价格计算**：基于Drools规则引擎的实时价格计算服务
- ✅ **策略优先级系统**：支持策略和规则的多级优先级管理
- ✅ **缓存机制**：规则缓存和价格计算结果缓存，提高性能

### 扩展功能
- 🔄 **批量操作**：支持价格策略批量导入导出
- 🔄 **历史记录**：价格计算历史查询
- 🔄 **模拟计算**：价格模拟计算功能
- 🔄 **统计监控**：价格计算性能统计和监控
- 🔄 **API文档**：完整的Swagger API文档

## 技术栈

### 后端技术
- **框架**：Spring Boot 3.x
- **规则引擎**：Drools 9.44.0.Final
- **数据库**：MySQL 8.0 + MyBatis-Plus
- **缓存**：Spring Cache + Redis（预留）
- **API文档**：SpringDoc OpenAPI 3.0 + Knife4j
- **构建工具**：Maven
- **Java版本**：Java 17

### 架构设计
- **分层架构**：Controller → Service → Repository → Entity
- **规则分离**：业务逻辑与计算规则分离，支持动态规则更新
- **插件化设计**：支持自定义策略类型和计算规则
- **高可用**：支持集群部署，规则引擎独立部署

## 模块结构

```
erp-sale-pricing/
├── src/main/java/cn/aiedge/erp/sales/pricing/
│   ├── PricingApplication.java            # 主启动类
│   ├── config/                           # 配置类
│   │   └── DroolsConfig.java             # Drools规则引擎配置
│   ├── controller/                       # 控制器层
│   │   ├── PricingController.java        # 价格策略管理控制器
│   │   └── DroolsRuleController.java     # Drools规则管理控制器
│   ├── service/                          # 服务层接口
│   │   ├── IPriceStrategyService.java    # 价格策略服务接口
│   │   ├── IPriceCalculationService.java # 价格计算服务接口
│   │   └── IDroolsRuleService.java       # Drools规则服务接口
│   ├── service/impl/                     # 服务层实现
│   │   ├── PriceStrategyServiceImpl.java
│   │   ├── PriceCalculationServiceImpl.java
│   │   └── DroolsRuleServiceImpl.java
│   ├── repository/                       # 数据访问层
│   │   ├── PriceStrategyRepository.java  # 价格策略Repository
│   │   └── PriceRuleRepository.java      # 价格规则Repository
│   ├── entity/                           # 实体类
│   │   ├── PriceStrategy.java            # 价格策略实体
│   │   └── PriceRule.java                # 价格规则实体
│   └── dto/                              # 数据传输对象
│       ├── PriceStrategyDTO.java
│       ├── PriceRuleDTO.java
│       ├── PriceCalculationRequest.java
│       └── PriceCalculationResult.java
├── src/main/resources/
│   ├── application.yml                   # 应用配置
│   └── rules/                            # Drools规则文件
│       └── basic-pricing-rules.drl       # 基础价格计算规则
└── src/test/java/                        # 测试代码
    └── cn/aiedge/erp/sales/pricing/
        ├── PricingApplicationTest.java
        └── service/
            ├── PriceStrategyServiceTest.java
            └── DroolsRuleServiceTest.java
```

## 数据库设计

### 核心表结构

#### 价格策略表 (erp_pricing_strategy)
```sql
CREATE TABLE erp_pricing_strategy (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    strategy_type VARCHAR(50),
    customer_level VARCHAR(50),
    region_code VARCHAR(50),
    product_category_id BIGINT,
    base_price DECIMAL(20,6),
    price_factor DECIMAL(10,4),
    discount_rate DECIMAL(10,4),
    discount_amount DECIMAL(20,6),
    min_quantity INT,
    formula_config JSON,
    effective_start_time DATETIME,
    effective_end_time DATETIME,
    priority INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'draft',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='价格策略表';
```

#### 价格规则表 (erp_pricing_rule)
```sql
CREATE TABLE erp_pricing_rule (
    id BIGINT PRIMARY KEY,
    strategy_id BIGINT NOT NULL,
    rule_name VARCHAR(100),
    rule_type VARCHAR(50),
    condition_type VARCHAR(50),
    condition_value VARCHAR(500),
    calculation_type VARCHAR(50),
    price_factor DECIMAL(10,4),
    discount_rate DECIMAL(10,4),
    discount_amount DECIMAL(20,6),
    min_quantity INT,
    max_quantity INT,
    min_amount DECIMAL(20,6),
    max_amount DECIMAL(20,6),
    priority INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'active',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (strategy_id) REFERENCES erp_pricing_strategy(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='价格规则表';
```

## API接口

### 价格策略管理API
- `POST /api/erp/sales/pricing/strategies` - 创建价格策略
- `PUT /api/erp/sales/pricing/strategies/{id}` - 更新价格策略
- `DELETE /api/erp/sales/pricing/strategies/{id}` - 删除价格策略
- `GET /api/erp/sales/pricing/strategies/{id}` - 获取策略详情
- `GET /api/erp/sales/pricing/strategies` - 分页查询策略列表
- `POST /api/erp/sales/pricing/strategies/{id}/activate` - 激活策略
- `POST /api/erp/sales/pricing/strategies/{id}/deactivate` - 停用策略

### 价格计算API
- `POST /api/erp/sales/pricing/calculate` - 计算商品价格
- `POST /api/erp/sales/pricing/calculate/batch` - 批量计算商品价格
- `POST /api/erp/sales/pricing/calculate/simulate` - 模拟价格计算
- `GET /api/erp/sales/pricing/calculations/{calculationId}` - 获取计算历史

### Drools规则管理API
- `POST /api/erp/sales/pricing/drools-rules/init` - 初始化规则引擎
- `POST /api/erp/sales/pricing/drools-rules/reload` - 重新加载规则
- `POST /api/erp/sales/pricing/drools-rules/validate-syntax` - 验证规则语法
- `POST /api/erp/sales/pricing/drools-rules/rules` - 添加新规则
- `PUT /api/erp/sales/pricing/drools-rules/rules/{ruleName}` - 更新规则
- `DELETE /api/erp/sales/pricing/drools-rules/rules/{ruleName}` - 删除规则
- `GET /api/erp/sales/pricing/drools-rules/rules` - 获取规则列表

## 部署指南

### 1. 环境要求
- Java 17+
- MySQL 8.0+
- Maven 3.8+

### 2. 数据库配置
1. 创建数据库：
   ```sql
   CREATE DATABASE erp_sales CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. 修改配置文件 `application.yml`：
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/erp_sales
       username: your_username
       password: your_password
   ```

### 3. 构建和运行
```bash
# 1. 编译项目
mvn clean compile

# 2. 运行测试
mvn test

# 3. 打包
mvn package -DskipTests

# 4. 运行应用
java -jar target/erp-sale-pricing-1.0.0-SNAPSHOT.jar
```

### 4. 访问地址
- 应用地址：http://localhost:8081
- API文档：http://localhost:8081/api/erp/sales/pricing/doc.html

## 开发指南

### 1. 添加新的价格策略类型
1. 在 `PriceStrategy` 实体中添加新字段
2. 更新 `PriceStrategyDTO` 和数据库表结构
3. 在 `PriceCalculationService` 中添加相应的计算逻辑
4. 在Drools规则文件中添加相应的规则

### 2. 添加新的Drools规则
1. 在 `src/main/resources/rules/` 目录下创建新的 `.drl` 文件
2. 规则文件需要符合Drools语法规范
3. 通过Drools管理API动态加载规则

### 3. 扩展价格计算维度
1. 在 `PriceCalculationRequest` 中添加新的维度字段
2. 更新 `PriceCalculationService` 中的策略匹配逻辑
3. 在Drools规则中添加相应的条件判断

## 测试覆盖

### 单元测试
```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=PriceStrategyServiceTest

# 生成测试覆盖率报告
mvn jacoco:report
```

### 测试要求
- 单元测试覆盖率 ≥ 80%
- 集成测试覆盖核心业务流程
- API接口测试完整

## 性能优化

### 1. 缓存策略
- **规则缓存**：Drools规则编译结果缓存
- **价格缓存**：常用价格计算结果缓存
- **策略缓存**：活跃价格策略缓存

### 2. 数据库优化
- 复合索引优化查询性能
- 分页查询避免大数据量查询
- 定期清理历史数据

### 3. 规则引擎优化
- 规则分组和优先级优化
- 规则执行顺序优化
- 批量规则执行优化

## 监控和运维

### 1. 监控指标
- 价格计算响应时间
- 规则执行性能
- 系统资源使用率
- 错误率和异常情况

### 2. 日志管理
- 价格计算详细日志
- 规则执行跟踪日志
- 系统操作审计日志

### 3. 告警机制
- 价格计算失败告警
- 规则引擎异常告警
- 系统性能告警

## 版本历史

### v1.0.0 (2026-05-05)
- 初始版本发布
- 价格策略管理基础功能
- Drools规则引擎集成
- 多维度价格计算
- 完整的API接口

### 后续版本计划
- v1.1.0：批量操作和导入导出功能
- v1.2.0：高级规则管理和可视化配置
- v1.3.0：性能监控和告警系统
- v2.0.0：分布式规则引擎支持

## 联系方式

- 项目负责人：AI-Ready Team
- 技术支持：tech-support@aiedge.cn
- 问题反馈：GitHub Issues

## 许可证

本项目采用 Apache License 2.0 开源协议。详见 [LICENSE](LICENSE) 文件。