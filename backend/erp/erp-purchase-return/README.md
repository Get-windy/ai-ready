# ERP 采购换货管理模块 (erp-purchase-return)

## 模块概述

采购换货管理模块是ERP系统中的核心模块，负责处理采购订单收货后的异常处理需求，包括质量换货、数量补货、规格更换等场景，为采购管理提供完整的售后支持能力。

## 功能特性

### 1. 换货申请管理
- **多场景支持**: 支持质量换货、数量补货、规格更换三种主要场景
- **智能表单**: 自动关联原采购订单信息，减少重复输入
- **审批流程**: 支持多级审批工作流，灵活配置
- **附件管理**: 支持质量检测报告、图片等附件上传

### 2. 换货执行管理
- **供应商协同**: 供应商在线确认换货方案，记录沟通历史
- **物流跟踪**: 退货物流和补货物流的全程跟踪管理
- **状态监控**: 实时监控换货执行状态，支持状态流转
- **成本核算**: 自动计算换货相关费用，支持费用分摊

### 3. 质量换货处理
- **问题记录**: 详细记录质量问题类型、影响程度
- **证据管理**: 支持检测报告和照片证据上传
- **退货管理**: 管理不合格货品的退货流程
- **索赔管理**: 支持质量索赔申请和处理

### 4. 换货成本管理
- **费用核算**: 自动计算运输、处理、时间等成本
- **分摊机制**: 支持供应商承担、采购方承担、双方分摊
- **成本分析**: 分析换货对采购成本、库存成本的影响
- **绩效考核**: 换货信息自动同步到供应商绩效考核

### 5. 数据分析与报表
- **统计报表**: 换货频率、原因、成本的统计分析
- **供应商排名**: 各供应商换货率和换货质量排名
- **趋势分析**: 换货的季节性和周期性规律分析
- **影响评估**: 换货对供应链稳定性的影响评估

## 技术架构

### 技术栈
- **后端框架**: Spring Boot 3.2.x
- **ORM框架**: MyBatis-Plus 3.5.x
- **数据库**: MySQL 8.0+
- **缓存**: Redis 6.0+
- **消息队列**: RabbitMQ 3.12+
- **工作流**: Activiti 7.1.x
- **安全**: Spring Security + JWT
- **API文档**: Knife4j 4.3.x
- **监控**: Spring Boot Actuator + Prometheus

### 系统集成
- **采购管理系统**: 换货单与原采购订单深度关联
- **库存管理系统**: 换货过程中的库存调整和记录
- **供应商管理系统**: 换货信息同步到供应商档案
- **质量管理系统**: 质量换货信息同步到质量管理系统

## 数据库设计

### 核心数据表
1. **purchase_return_order** - 采购换货单主表
2. **purchase_return_order_item** - 采购换货单明细表
3. **purchase_return_approval** - 换货审批记录表
4. **purchase_return_logistics** - 换货物流跟踪表
5. **purchase_return_attachment** - 换货附件表
6. **purchase_return_cost** - 换货成本记录表
7. **purchase_return_statistics** - 换货统计汇总表

### 数据表关系
```
采购换货单 (1) → (n) 换货单明细
采购换货单 (1) → (n) 审批记录
采购换货单 (1) → (n) 物流跟踪
采购换货单 (1) → (n) 附件记录
采购换货单 (1) → (1) 成本记录
```

## API接口

### 基础接口
- `POST /api/erp/purchase/return` - 创建换货单
- `PUT /api/erp/purchase/return/{id}` - 更新换货单
- `GET /api/erp/purchase/return/{id}` - 获取换货单详情
- `DELETE /api/erp/purchase/return/{id}` - 删除换货单
- `GET /api/erp/purchase/return/list` - 分页查询换货单

### 审批流程接口
- `POST /api/erp/purchase/return/{id}/submit` - 提交换货审批
- `POST /api/erp/purchase/return/{id}/approve` - 审批通过
- `POST /api/erp/purchase/return/{id}/reject` - 审批拒绝

### 供应商协同接口
- `POST /api/erp/purchase/return/{id}/supplier/confirm` - 供应商确认
- `POST /api/erp/purchase/return/{id}/supplier/reject` - 供应商拒绝

### 物流管理接口
- `PUT /api/erp/purchase/return/{id}/logistics/return` - 更新退货物流
- `PUT /api/erp/purchase/return/{id}/logistics/replacement` - 更新换货物流

### 状态管理接口
- `POST /api/erp/purchase/return/{id}/return/complete` - 标记退货完成
- `POST /api/erp/purchase/return/{id}/replacement/complete` - 标记换货完成
- `POST /api/erp/purchase/return/{id}/complete` - 完成换货单
- `POST /api/erp/purchase/return/{id}/cancel` - 取消换货单

### 数据查询接口
- `GET /api/erp/purchase/return/order/{purchaseOrderCode}` - 按采购订单查询
- `GET /api/erp/purchase/return/pending/approval` - 待审批列表
- `GET /api/erp/purchase/return/statistics` - 统计信息
- `GET /api/erp/purchase/return/export` - 数据导出

## 部署配置

### 1. 环境要求
- Java 17+
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 3.12+

### 2. 配置文件
```yaml
spring:
  application:
    name: erp-purchase-return
  datasource:
    url: jdbc:mysql://localhost:3306/erp_purchase_return
    username: root
    password: root
  redis:
    host: localhost
    port: 6379
  rabbitmq:
    host: localhost
    port: 5672

server:
  port: 8086
  servlet:
    context-path: /api/erp/purchase/return
```

### 3. 数据库初始化
```sql
-- 创建数据库
CREATE DATABASE erp_purchase_return CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建核心表结构（参见 resources/db/schema.sql）
```

### 4. 启动应用
```bash
mvn clean install
java -jar target/erp-purchase-return-1.0.0-SNAPSHOT.jar
```

## 开发规范

### 包结构
```
cn.aiedge.erp.purchase.return
├── controller    # 控制器层
├── service       # 服务层
│   ├── impl      # 服务实现
│   └── dto       # 数据传输对象
├── entity        # 数据库实体
├── mapper        # MyBatis Mapper
├── repository    # 数据访问层
├── config        # 配置类
├── exception     # 异常处理
└── aspect        # 切面编程
```

### 代码规范
1. **命名规范**: 遵循Java命名规范，使用有意义的名称
2. **接口设计**: RESTful API设计，统一错误处理
3. **安全控制**: 使用Spring Security进行权限控制
4. **日志记录**: 使用SLF4J进行日志记录
5. **异常处理**: 统一的异常处理机制
6. **参数验证**: 使用JSR-303进行参数验证

### 测试规范
1. **单元测试**: 所有服务方法都需要单元测试
2. **集成测试**: 测试系统集成功能
3. **API测试**: 测试RESTful API接口
4. **性能测试**: 测试系统性能指标

## 权限管理

### 角色权限
- **采购专员**: 创建、更新、查询换货单
- **采购经理**: 审批、查询、统计分析
- **质量专员**: 质量换货处理、索赔管理
- **供应商**: 确认换货方案、查询相关换货单
- **系统管理员**: 所有权限

### 权限控制
- 基于角色的访问控制 (RBAC)
- 数据权限控制 (部门级、个人级)
- 操作权限控制 (创建、读取、更新、删除)
- 审批权限控制 (多级审批)

## 监控与运维

### 健康检查
- 数据库连接状态监控
- Redis缓存状态监控
- RabbitMQ消息队列监控
- 应用性能指标监控

### 性能指标
- 接口响应时间
- 系统吞吐量
- 并发用户数
- 错误率统计

### 日志管理
- 访问日志记录
- 操作日志记录
- 错误日志记录
- 性能日志记录

## 故障处理

### 常见问题
1. **数据库连接失败**: 检查数据库服务状态和连接配置
2. **缓存服务异常**: 检查Redis服务状态和连接配置
3. **消息队列异常**: 检查RabbitMQ服务状态和连接配置
4. **权限验证失败**: 检查用户权限配置和JWT令牌

### 故障恢复
1. **数据库故障**: 使用主从复制或备份恢复
2. **缓存故障**: 清除缓存重新加载
3. **消息队列故障**: 消息重试和死信队列处理
4. **应用故障**: 重启应用或切换到备用实例

## 版本历史

### v1.0.0 (2026-05-01)
- 初始版本发布
- 实现换货申请管理
- 实现审批工作流
- 实现供应商协同
- 实现成本核算
- 实现数据统计分析

## 后续规划

### v1.1.0 (计划)
- 移动端支持
- 智能换货建议
- 供应商绩效自动评分
- 批量换货处理
- 多语言支持

### v1.2.0 (计划)
- AI换货原因分析
- 换货预测模型
- 智能费用分摊
- 供应链风险预警
- 区块链溯源

## 联系我们

如有问题或建议，请联系:
- 模块负责人: ai-mnj0haev
- 技术支持: 开发团队
- 业务咨询: 采购管理部门

---

**最后更新**: 2026-05-01  
**当前版本**: v1.0.0  
**状态**: ✅ 开发完成，可部署使用