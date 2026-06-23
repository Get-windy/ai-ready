# 销售管理模块结构

## 模块概述

销售管理模块是企智连ERP系统的核心销售业务模块，采用模块化设计，支持功能扩展和独立部署。

## 模块结构

```
erp/sales/
├── erp-sale/                     # 销售主模块
│   ├── src/main/java/           # 源代码
│   ├── src/main/resources/      # 资源文件
│   ├── src/test/java/           # 测试代码
│   └── pom.xml                  # 销售主模块配置
└── pricing/                     # 价格策略子模块
    ├── src/main/java/           # 源代码
    ├── src/main/resources/      # 资源文件
    ├── src/test/java/           # 测试代码
    └── pom.xml                  # 价格策略模块配置
```

## 模块说明

### 1. erp-sale (销售主模块)
- **功能**：客户管理、销售订单、销售出库、销售退货等核心销售功能
- **状态**：已存在，基础功能已实现
- **技术栈**：Spring Boot + MyBatis-Plus + MySQL

### 2. pricing (价格策略子模块)
- **功能**：销售价格策略管理、Drools规则引擎集成、多维度价格计算
- **状态**：本次开发完成
- **技术栈**：Spring Boot + Drools + MyBatis-Plus + MySQL

## 模块依赖关系

```
pricing模块 → erp-sale模块 → core-base模块
```

## 部署方式

### 独立部署
每个模块可以独立部署：
```bash
# 部署销售主模块
cd erp/sales/erp-sale/
mvn spring-boot:run

# 部署价格策略模块
cd erp/sales/pricing/
mvn spring-boot:run
```

### 集成部署
两个模块可以集成部署，通过API网关统一访问。

## API路径

### 销售主模块
- 基础路径：`/api/erp/sales/`
- 示例：`http://localhost:8080/api/erp/sales/customers`

### 价格策略模块
- 基础路径：`/api/erp/sales/pricing/`
- 示例：`http://localhost:8081/api/erp/sales/pricing/strategies`

## 数据库设计

### 销售主模块表
- `erp_customer` - 客户信息表
- `erp_sales_order` - 销售订单表
- `erp_sales_order_item` - 销售订单明细表
- `erp_sales_delivery` - 销售出库表
- `erp_sales_return` - 销售退货表

### 价格策略模块表
- `erp_pricing_strategy` - 价格策略表
- `erp_pricing_rule` - 价格规则表

## 开发规范

### 1. 包命名规范
- 销售主模块：`cn.aiedge.erp.sales.*`
- 价格策略模块：`cn.aiedge.erp.sales.pricing.*`

### 2. 代码规范
- 遵循项目统一的代码规范
- 使用Lombok减少样板代码
- 统一异常处理机制
- 完整的API文档

### 3. 测试规范
- 单元测试覆盖率 ≥ 80%
- 集成测试覆盖核心业务流程
- API接口测试完整

## 扩展指南

### 添加新子模块
1. 在 `erp/sales/` 目录下创建新模块
2. 配置独立的 `pom.xml`
3. 实现模块功能
4. 更新API网关路由配置

### 模块间通信
- 通过REST API进行模块间通信
- 使用消息队列进行异步通信
- 共享数据库进行数据同步

## 版本管理

每个模块独立版本管理：
- 销售主模块：1.0.0-SNAPSHOT
- 价格策略模块：1.0.0-SNAPSHOT

## 监控和运维

### 健康检查
- 销售主模块：`/actuator/health`
- 价格策略模块：`/actuator/health`

### 性能监控
- 响应时间监控
- 错误率监控
- 资源使用率监控

### 日志管理
- 统一日志格式
- 分布式日志收集
- 关键操作审计日志