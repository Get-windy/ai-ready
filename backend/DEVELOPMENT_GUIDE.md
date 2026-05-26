# ERP系统开发指南

## 📋 概述

本文档提供ERP系统的开发规范、流程和最佳实践，确保团队成员在统一的架构和规范下进行开发。

## 🏗️ 架构概览

### 项目结构
```
backend/
├── pom.xml                    # 父POM文件，统一依赖管理
├── erp-modules/               # ERP核心业务模块
│   ├── batch/                 # 批次管理模块
│   ├── customer/              # 客户管理模块
│   ├── expense/               # 费用管理模块
│   ├── finance/               # 财务管理模块
│   ├── inventory/             # 库存管理模块
│   ├── invoice/               # 发票管理模块
│   ├── metrics/               # 指标管理模块
│   ├── monitor/               # 监控管理模块
│   ├── order/                 # 订单管理模块
│   ├── purchase/              # 采购管理模块
│   ├── sales/                 # 销售管理模块
│   └── supplier/              # 供应商管理模块
├── infrastructure/            # 基础设施模块
│   └── pom.xml                # 公共模块POM
└── DEVELOPMENT_GUIDE.md       # 本开发指南
```

### 技术栈
- **后端**: Spring Boot 3.2.5 + Java 17
- **数据库**: PostgreSQL 16
- **构建工具**: Maven 3.9+
- **API文档**: SpringDoc OpenAPI 3.0
- **测试**: JUnit 5 + Mockito 5

## 📝 开发规范

### 1. 包命名规范
- **根包名**: `cn.aiedge.erp`
- **模块包名**: `cn.aiedge.erp.{module}`
- **分层包名**: `controller`, `service`, `repository`, `model`, `config`

### 2. 类命名规范
- **实体类**: `{Entity}` (如 `PurchaseOrder`)
- **控制器类**: `{Entity}Controller` (如 `PurchaseOrderController`)
- **服务类**: `{Entity}Service` 和 `{Entity}ServiceImpl`
- **仓库类**: `{Entity}Repository`
- **DTO类**: `{Entity}DTO`, `Create{Entity}Request`, `Update{Entity}Request`

### 3. API路径规范
```
# 标准API路径
GET    /api/erp/v1/{module}/{resource}          # 查询列表
GET    /api/erp/v1/{module}/{resource}/{id}     # 查询详情
POST   /api/erp/v1/{module}/{resource}          # 创建资源
PUT    /api/erp/v1/{module}/{resource}/{id}     # 更新资源
DELETE /api/erp/v1/{module}/{resource}/{id}     # 删除资源

# 示例：采购订单
GET    /api/erp/v1/purchase/orders
POST   /api/erp/v1/purchase/orders
```

## 🛠️ 开发流程

### 1. 环境准备
1. 安装JDK 17+
2. 安装Maven 3.9+
3. 安装PostgreSQL 16+
4. 安装Git

### 2. 创建新模块
如果需要创建新模块，请按以下步骤：
1. 在`erp-modules`目录下创建模块目录
2. 复制标准目录结构
3. 更新父POM中的模块配置
4. 创建模块的`pom.xml`
5. 创建基础代码结构

### 3. 添加新功能
1. 创建实体类（`model/{Entity}.java`）
2. 创建仓库接口（`repository/{Entity}Repository.java`）
3. 创建服务接口和实现（`service/{Entity}Service.java`）
4. 创建控制器（`controller/{Entity}Controller.java`）
5. 创建DTO类
6. 编写单元测试

### 4. 代码质量要求
- **单元测试覆盖率**: ≥80%
- **代码规范**: 遵循Google Java Style Guide
- **静态分析**: SonarQube扫描通过
- **API文档**: 所有API必须有Swagger注解

## 🔧 构建与部署

### 1. 构建项目
```bash
# 构建整个项目
mvn clean compile

# 运行测试
mvn test

# 打包项目
mvn clean package

# 跳过测试打包
mvn clean package -DskipTests
```

### 2. 运行项目
```bash
# 运行单个模块
cd erp-modules/batch
mvn spring-boot:run

# 运行所有模块（需要配置）
mvn spring-boot:run -pl batch
```

### 3. Docker构建
```bash
# 构建Docker镜像
docker build -t erp-batch:latest -f Dockerfile.batch .

# 运行容器
docker run -p 8080:8080 erp-batch:latest
```

## 📊 测试策略

### 1. 单元测试
- 测试服务层业务逻辑
- 使用Mockito模拟依赖
- 覆盖所有边界条件

### 2. 集成测试
- 测试API端点
- 测试数据库交互
- 测试模块间集成

### 3. 端到端测试
- 测试完整业务流程
- 模拟真实用户操作
- 验证系统整体功能

## 📖 文档要求

### 1. 模块文档
每个模块必须包含：
- `README.md`: 模块概述和快速开始
- API文档: Swagger/OpenAPI规范
- 数据库设计文档: ER图和表结构说明
- 部署文档: 部署步骤和配置说明

### 2. API文档
- 所有API必须有Swagger注解
- 提供请求/响应示例
- 说明错误码和异常处理
- 提供在线测试接口

## 🔄 版本控制

### 1. 分支策略
- `main`: 生产环境分支（受保护）
- `develop`: 开发主干分支
- `feature/`: 功能开发分支
- `bugfix/`: 问题修复分支
- `release/`: 发布分支

### 2. 提交规范
```
feat: 添加新功能
fix: 修复bug
docs: 文档更新
style: 代码格式化
refactor: 代码重构
test: 测试相关
chore: 构建过程或辅助工具的变动
```

### 3. 代码审查
- 所有代码必须经过审查
- 至少需要一名审查者批准
- 审查重点：代码质量、安全性、性能

## 🚨 紧急情况处理

### 1. 架构问题上报
发现以下情况必须立即上报coordinator：
1. 目录结构违反规范
2. 模块边界不清晰
3. 技术栈不一致
4. API路径混乱

### 2. 制动流程
1. 上报coordinator确认问题
2. 执行紧急制动停止相关开发
3. 修复架构问题
4. 重新规划任务
5. 恢复开发

## 📞 支持与帮助

### 技术问题
- 后端开发问题: 联系backend开发团队
- 数据库问题: 联系DBA团队
- 部署问题: 联系devops-engineer

### 流程问题
- 任务分配: 联系coordinator
- 进度跟踪: 使用任务系统
- 团队协作: 使用项目群组

---

*文档版本: 1.0*
*生效日期: 2026-05-05*
*维护者: devops-engineer*