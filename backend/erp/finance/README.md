# ERP Finance Module - 财务管理模块

## 模块概述

ERP Finance Module 是一个统一的财务管理系统，整合了企业财务管理的各个方面，包括账户管理、交易管理、报表生成和税务申报等功能。

## 功能特性

### 核心功能

- **账户管理** - 管理企业 GetAll财务账户信息，包括银行账户、现金账户、内部账户等
- **交易管理** - 记录和管理企业.GetAll财务交易，支持收入、支出、转账、退款等类型
- **报表管理** - 生成和管理财务报表，包括资产负债表、利润表、现金流量表等
- **税务管理** - 管理税务申报和缴纳，支持增值税、企业所得税、个人所得税等税种

### 技术特性

- 基于 Spring Boot 3.x
- JPA/Hibernate 数据持久化
- Redis 缓存支持
- RabbitMQ 消息队列
- OpenFeign 服务调用
- SaToken 认证授权
- Swagger API 文档

## 项目结构

```
erp-finance/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── cn/
│   │   │       └── aiedge/
│   │   │           └── erp/
│   │   │               └── finance/
│   │   │                   ├── FinanceApplication.java          # 主应用类
│   │   │                   ├── controller/                      # 控制器层
│   │   │                   │   ├── FinanceAccountController.java
│   │   │                   │   ├── FinanceTransactionController.java
│   │   │                   │   ├── FinanceReportController.java
│   │   │                   │   └── FinanceTaxDeclarationController.java
│   │   │                   ├── service/                         # 服务层
│   │   │                   │   ├── FinanceAccountService.java
│   │   │                   │   ├── FinanceTransactionService.java
│   │   │                   │   ├── FinanceReportService.java
│   │   │                   │   └── FinanceTaxDeclarationService.java
│   │   │                   ├── model/                           # 数据模型
│   │   │                   │   └── entity/                      # 实体类
│   │   │                   │       ├── BaseEntity.java
│   │   │                   │       ├── FinanceAccount.java
│   │   │                   │       ├── FinanceTransaction.java
│   │   │                   │       ├── FinanceReport.java
│   │   │                   │       └── FinanceTaxDeclaration.java
│   │   │                   └── repository/                      # 数据访问层
│   │   └── resources/
│   │       ├── application.yml                                  # 主配置文件
│   │       ├── application-dev.yml                              # 开发环境配置
│   │       └── sql/                                             # SQL脚本
│   └── test/
│       └── java/
│           └── cn/
│               └── aiedge/
│                   └── erp/
│                       └── finance/
│                           └── FinanceApplicationTests.java     # 单元测试
├── pom.xml                                                      # Maven配置
└── README.md                                                    # 模块说明
```

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | Java开发语言 |
| Spring Boot | 3.x | 应用框架 |
| MyBatis-Plus | Latest | 数据访问层 |
| PostgreSQL | Latest | 数据库 |
| Redis | Latest | 缓存 |
| RabbitMQ | Latest | 消息队列 |

## API接口

### 财务账户管理

| 接口 | 方法 | 路径 | 描述 |
|------|------|------|------|
| 查询账户列表 | GET | /api/erp/finance/account/list | 查询财务账户列表 |
| 查询账户统计 | GET | /api/erp/finance/account/statistics | 查询账户统计信息 |
| 更新账户状态 | PUT | /api/erp/finance/account/status/{id} | 启用/停用账户 |
| 更新账户余额 | PUT | /api/erp/finance/account/balance/{accountId} | 更新账户余额 |

### 财务交易管理

| 接口 | 方法 | 路径 | 描述 |
|------|------|------|------|
| 创建交易 | POST | /api/erp/finance/transaction/ | 创建交易记录 |
| 查询交易列表 | GET | /api/erp/finance/transaction/list | 查询交易列表 |
| 查询交易统计 | GET | /api/erp/finance/transaction/statistics | 查询交易统计 |
| 审核交易 | PUT | /api/erp/finance/transaction/approve/{id} | 审核交易 |
| 撤销交易 | DELETE | /api/erp/finance/transaction/revoke/{id} | 撤销交易 |

### 财务报表管理

| 接口 | 方法 | 路径 | 描述 |
|------|------|------|------|
| 生成报表 | POST | /api/erp/finance/report/generate | 生成财务报表 |
| 查询报表列表 | GET | /api/erp/finance/report/list | 查询报表列表 |
| 查询报表详情 | GET | /api/erp/finance/report/detail/{reportNo} | 查询报表详情 |
| 审核报表 | PUT | /api/erp/finance/report/approve/{reportNo} | 审核报表 |

### 税务申报管理

| 接口 | 方法 | 路径 | 描述 |
|------|------|------|------|
| 创建申报 | POST | /api/erp/finance/tax/declaration | 创建税务申报 |
| 查询申报列表 | GET | /api/erp/finance/tax/declaration/list | 查询申报列表 |
| 查询申报详情 | GET | /api/erp/finance/tax/declaration/detail/{declarationNo} | 查询申报详情 |
| 更新申报状态 | PUT | /api/erp/finance/tax/declaration/status | 更新申报状态 |
| 查询应缴税款 | GET | /api/erp/finance/tax/payable | 查询应缴税款统计 |

## 数据库设计

### 主要表结构

1. **finance_account** - 财务账户表
2. **finance_transaction** - 财务交易表
3. **finance_report** - 财务报表表
4. **finance_tax_declaration** - 税务申报表

详细数据库设计文档请参考 `src/main/resources/sql/` 目录。

## 部署说明

### 环境要求

- JDK 17+
- Maven 3.6+
- PostgreSQL 12+
- Redis 6+
- RabbitMQ 3.8+

### 构建部署

```bash
# 构建项目
mvn clean package -DskipTests

# 运行应用
java -jar target/erp-finance-1.0.0-SNAPSHOT.jar
```

### Docker部署

```bash
# 构建镜像
docker build -t erp-finance:1.0.0-SNAPSHOT .

# 运行容器
docker run -d -p 8095:8095 --name erp-finance erp-finance:1.0.0-SNAPSHOT
```

## 测试

```bash
# 执行单元测试
mvn test

# 执行测试并生成报告
mvn test surefire-report:report
```

## 开发规范

### 代码规范

- 遵循阿里巴巴Java开发手册
- 使用Spring Boot最佳实践
- 代码注释完整
- 单元测试覆盖率≥70%

### 提交规范

```
feat: 新功能
fix: 修复bug
docs: 文档更新
style: 代码格式
refactor: 重构
test: 测试相关
chore: 构建过程或辅助工具变动
```

## 维护说明

### 日志管理

- 应用日志: `logs/erp-finance.log`
- 日志级别: debug (开发环境), info (生产环境)
- 日志轮转: 单文件最大10MB，保留30天

### 监控指标

- 应用健康检查: `/actuator/health`
- 应用信息: `/actuator/info`
- 指标数据: `/actuator/metrics`

## 联系方式

- 开发团队: AI-Ready Team
- 邮箱: support@ai-ready.com
- 版本: 1.0.0-SNAPSHOT

## 许可证

Copyright © 2024 AI-Ready Team. All rights reserved.
