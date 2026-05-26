# 批次/序列号管理模块 (ERP Batch Management Module)

## 模块概述
批次/序列号管理模块提供ERP系统核心的批次追踪、序列号管理、有效期控制、批次查询等功能。支持产品批次的全生命周期管理，确保库存管理的可追溯性和质量控制。

## 主要功能
- ✅ 批次记录管理（创建、查询、更新、删除）
- ✅ 序列号管理（分配、追踪、状态管理）
- ✅ 批次库存管理（库存数量、预留数量、可用数量）
- ✅ 有效期控制（临期预警、过期批次处理）
- ✅ 质量等级管理（合格、不合格、待检验）
- ✅ 批次追溯查询（从原材料到成品）

## 目录结构
```
batch/
├── src/main/java/cn/aiedge/erp/batch/
│   ├── controller/    # 控制器层 (待实现)
│   ├── service/       # 服务层接口 (已创建)
│   ├── repository/    # 数据访问层 (已创建)
│   ├── model/         # 模型层 (已创建)
│   │   └── dto/       # 数据传输对象 (部分创建)
│   └── config/        # 配置类 (待实现)
├── src/test/java/cn/aiedge/erp/batch/  # 测试代码 (待实现)
├── docs/             # 模块文档
└── pom.xml          # Maven配置 (已创建)
```

## 技术栈
- **Java**: 17
- **Spring Boot**: 3.2.x
- **数据库**: PostgreSQL 16
- **构建工具**: Maven 3.9+
- **持久层**: Spring Data JPA
- **文档**: OpenAPI 3.0 (Swagger)

## API接口
### REST API路径格式
```
GET    /api/erp/v1/batch/records            # 查询批次记录列表
GET    /api/erp/v1/batch/records/{id}       # 查询批次记录详情
POST   /api/erp/v1/batch/records           # 创建批次记录
PUT    /api/erp/v1/batch/records/{id}      # 更新批次记录
DELETE /api/erp/v1/batch/records/{id}      # 删除批次记录

GET    /api/erp/v1/batch/serial-numbers    # 查询序列号列表
GET    /api/erp/v1/batch/serial-numbers/{serialNumber}  # 查询序列号详情
POST   /api/erp/v1/batch/serial-numbers    # 创建序列号
PUT    /api/erp/v1/batch/serial-numbers/{id}  # 更新序列号
POST   /api/erp/v1/batch/serial-numbers/scan  # 扫描序列号
POST   /api/erp/v1/batch/serial-numbers/trace # 序列号追溯
```

## 数据模型
### 核心实体
1. **BatchRecord** (批次记录)
   - 批次号、产品ID、生产日期、有效期
   - 批次状态（ACTIVE, EXPIRED, QUARANTINED, CANCELLED, CONSUMED）
   - 质量等级（QUALIFIED, REJECTED, PENDING_INSPECTION）

2. **SerialNumber** (序列号)
   - 序列号、批次ID、产品ID
   - 序列号状态（AVAILABLE, IN_USE, MAINTAINED, SCRAP, LOST, SOLD）
   - 序列号类型（PRODUCTION, REPAIR, REFURBISHED）

3. **BatchInventory** (批次库存)
   - 批次ID、产品ID、仓库ID
   - 库存数量、预留数量、可用数量
   - 库存状态（AVAILABLE, RESERVED, LOCKED, QUALITY_CHECK, QUARANTINE）

## 已实现功能
- ✅ 基础实体类（BaseEntity, BatchRecord, SerialNumber, BatchInventory）
- ✅ 数据访问层（Repository接口）
- ✅ 服务层接口定义
- ✅ 通用DTO类
- ✅ Maven配置文件（pom.xml）
- ✅ 应用主类（BatchApplication）
- ✅ 项目文档和架构说明

## 待实现功能
- 🔄 Service层实现类
- 🔄 Controller层实现
- 🔄 配置类和工具类
- 🔄 数据库迁移脚本
- 🔄 单元测试和集成测试
- 🔄 API文档（Swagger）

## 依赖关系
- **依赖模块**: spring-boot-starter-web, spring-boot-starter-data-jpa, springdoc-openapi-starter-webmvc-ui
- **被依赖模块**: 无（独立可运行模块）

## 开发进度
**当前进度**: 数据模型和基础设施已完成（60%）
**下一步**: 实现Service层和Controller层
**预计完成时间**: 剩余开发约需2-3小时
