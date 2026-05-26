# ERP系统架构设计规范

## 📐 总体架构原则

### 1. 统一性原则
- **所有ERP模块统一组织**：所有核心业务模块必须放在统一的目录结构下
- **命名规范统一**：遵循一致的命名约定和编码规范
- **技术栈统一**：使用统一的技术框架和工具链

### 2. 模块化原则
- **模块边界清晰**：每个模块有明确的业务边界和技术边界
- **职责单一**：每个模块承担单一的业务职责
- **接口明确**：模块间通过明确定义的接口进行通信

### 3. 可维护性原则
- **目录结构清晰**：目录结构直观，便于理解和维护
- **文档完善**：每个模块有完整的文档和API说明
- **测试完备**：每个模块有完整的测试覆盖

---

## 📁 目录结构规范

### 一级目录结构
```
backend/
├── erp/                    # ERP核心业务模块（统一管理）
│   ├── purchase/          # 采购管理模块
│   ├── sales/             # 销售管理模块
│   ├── inventory/         # 库存管理模块
│   ├── finance/           # 财务管理模块
│   ├── batch/             # 批次管理模块
│   ├── supplier/          # 供应商管理模块
│   └── customer/          # 客户管理模块
├── infrastructure/        # 基础设施层
│   ├── deploy/            # 部署配置
│   ├── monitoring/        # 监控系统
│   ├── security/          # 安全组件
│   └── common/            # 公共组件
└── tests/                 # 测试相关
```

### 模块目录结构规范
```
erp/
├── {module-name}/         # 模块名（英文小写，单数形式）
│   ├── src/main/java/cn/aiedge/erp/{module}/
│   │   ├── controller/    # 控制器层
│   │   ├── service/       # 服务层
│   │   ├── repository/    # 数据访问层
│   │   ├── model/         # 模型层
│   │   └── config/        # 配置类
│   ├── src/test/java/cn/aiedge/erp/{module}/
│   ├── docs/             # 模块文档
│   └── pom.xml           # Maven配置
```

---

## 🏷️ 命名规范

### 包命名规范
- **根包名**: `cn.aiedge.erp`
- **模块包名**: `cn.aiedge.erp.{module}`
- **子包名**: `controller`, `service`, `repository`, `model`, `config`

### 类命名规范
- **控制器类**: `{Entity}Controller` (如 `PurchaseOrderController`)
- **服务类**: `{Entity}Service` 和 `{Entity}ServiceImpl`
- **仓库类**: `{Entity}Repository`
- **模型类**: `{Entity}` (如 `PurchaseOrder`, `BatchRecord`)
- **DTO类**: `{Entity}DTO`, `Create{Entity}Request`, `Update{Entity}Request`

### 接口命名规范
- **服务接口**: `I{Entity}Service`
- **数据接口**: `I{Entity}Repository`

---

## 🔗 API路径规范

### REST API路径格式
```
/api/erp/{version}/{module}/{resource}
```

### 版本控制
- **API版本**: v1, v2 (通过URL路径区分)
- **响应版本**: 通过Content-Type指定

### 示例API路径
```
# 采购模块
GET    /api/erp/v1/purchase/orders          # 查询采购订单列表
GET    /api/erp/v1/purchase/orders/{id}     # 查询采购订单详情
POST   /api/erp/v1/purchase/orders          # 创建采购订单
PUT    /api/erp/v1/purchase/orders/{id}     # 更新采购订单
DELETE /api/erp/v1/purchase/orders/{id}     # 删除采购订单

# 批次管理模块
GET    /api/erp/v1/batch/records            # 查询批次记录
POST   /api/erp/v1/batch/records/trace      # 批次追溯查询
```

---

## 🛠️ 技术栈规范

### 后端技术栈
- **框架**: Spring Boot 3.2.x
- **语言**: Java 17
- **数据库**: PostgreSQL 16
- **构建工具**: Maven 3.9+
- **测试框架**: JUnit 5, Mockito 5
- **API文档**: OpenAPI 3.0 (Swagger)

### 前端技术栈
- **框架**: Vue 3.x
- **UI库**: Element Plus
- **状态管理**: Pinia
- **构建工具**: Vite

### 基础设施
- **容器化**: Docker + Docker Compose
- **编排**: Kubernetes (可选)
- **CI/CD**: GitHub Actions
- **监控**: Prometheus + Grafana
- **日志**: ELK Stack

---

## 🧪 质量保障规范

### 测试规范
- **单元测试**: 覆盖核心业务逻辑，覆盖率 ≥80%
- **集成测试**: 验证模块间集成和API功能
- **端到端测试**: 验证完整业务流程
- **性能测试**: 关键路径性能基准测试

### 代码质量
- **代码审查**: 所有提交必须经过代码审查
- **静态分析**: SonarQube扫描，无严重安全漏洞
- **依赖管理**: 定期更新依赖，漏洞修复

---

## 🔄 开发流程规范

### 分支策略
- **main**: 生产环境分支，受保护
- **develop**: 开发主干分支
- **feature/**: 功能开发分支
- **bugfix/**: 问题修复分支
- **release/**: 发布分支

### 提交规范
```
feat: 添加新功能
fix: 修复bug
docs: 文档更新
style: 代码格式化
refactor: 代码重构
test: 测试相关
chore: 构建过程或辅助工具的变动
```

### 发布流程
1. 开发完成 → 创建Pull Request
2. 代码审查 → 通过审查
3. 合并到develop → 触发CI/CD
4. 测试验证 → 测试通过
5. 创建release分支 → 版本发布
6. 合并到main → 部署生产

---

## 📊 监控与运维

### 健康检查
- **健康检查端点**: `/actuator/health`
- **就绪检查端点**: `/actuator/readiness`
- **存活检查端点**: `/actuator/liveness`

### 监控指标
- **应用指标**: JVM内存、GC、线程池
- **业务指标**: API响应时间、错误率、吞吐量
- **系统指标**: CPU、内存、磁盘、网络

### 日志规范
- **日志级别**: ERROR > WARN > INFO > DEBUG > TRACE
- **结构化日志**: JSON格式，便于解析
- **关键日志**: 操作日志、错误日志、性能日志

---

## 📚 文档要求

### 模块文档
每个模块必须包含：
- **README.md**: 模块概述、功能说明、快速开始
- **API文档**: OpenAPI规范或Swagger文档
- **架构文档**: 模块架构设计、数据模型
- **部署文档**: 部署步骤和配置说明

### 项目文档
- **开发指南**: 开发环境搭建、代码规范
- **部署指南**: 生产环境部署说明
- **运维指南**: 监控、告警、故障处理
- **用户手册**: 最终用户操作指南

---

## 🚨 紧急情况处理

### 架构问题上报
发现以下情况必须立即上报：
1. 目录结构违反规范
2. 模块边界不清晰
3. 技术栈不一致
4. API路径混乱

### 制动流程
1. 上报coordinator确认问题
2. 执行紧急制动停止相关开发
3. 修复架构问题
4. 重新规划任务
5. 恢复开发

---

*架构设计文档版本: 1.0*
*生效日期: 2026-05-05*
*维护者: coordinator (main)*