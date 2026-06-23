# AI-Ready 后端文档

## 项目定位

AI-Ready 是一套面向**中小型商贸企业**的 ERP/CRM/WMS/DMS 一体化系统，对标 Odoo 商贸版 + 金蝶云星空，适配国内财务、发票、税务等业务场景。

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 运行环境 LTS |
| Spring Boot | 3.2.5 | 应用框架 |
| MyBatis-Plus | 3.5.10.1 | ORM 框架 |
| PostgreSQL | 15.x | 关系型数据库 |
| Redis | 7.x | 缓存 / Token / 分布式会话 |
| Sa-Token | 1.37.0 | 认证授权 |
| Flyway | 9.22.3 | 数据库版本管理 |
| XXL-Job | 2.4.1 | 分布式任务调度 |
| Knife4j | 4.5.0 | API 文档 |
| Hutool | 5.8.26 | 工具库 |

## 模块架构

```
backend/ (v0.3.3)
│
├── core/                         # 核心基础模块群
│   ├── base/core-base/           # 用户权限 + 系统管理 + 审计日志 + 工作流基础
│   │   └─ 34个实体: User, Role, Permission, SysTenant, SysDept, SysMenu,
│   │            SysOperLog, SysLoginLog, WorkflowDefinition, WorkflowInstance...
│   ├── api/core-api/             # API启动入口 + 全局配置
│   ├── agent/core-agent/         # AI智能代理模块
│   ├── platform/core-platform/   # 平台级功能
│   └ notification/core-notification/ # 通知模块
│
├── erp/                          # ERP业务模块群 (13个子模块)
│   ├── erp-purchase/             # 采购管理 (采购订单、入库、退货、询价、合同)
│   ├── erp-sales/                # 销售管理 (销售订单、出库、退货、促销、组装拆卸)
│   ├── erp-stock/                # 库存管理 (商品档案、库存、盘点、调拨、批次序列号)
│   ├── erp-finance/              # 财务管理 (应收应付、凭证、收付款、发票、费用报销)
│   ├── erp-partner/              # 往来单位管理 (客户/供应商档案、联系人、等级)
│   ├── erp-pricing/              # 定价管理
│   ├── erp-supplier-portal/      # 供应商门户 (询价报价、绩效、积分)
│   ├── erp-printing/             # 打印服务 (模板、任务、日志、签名)
│   ├── erp-delivery-route/       # 配送路线
│   ├── erp-observability/        # 可观测性 (业务指标、系统监控)
│   ├── erp-budget/               # 预算管理
│   ├── erp-b2b-mall/             # B2B商城
│   └── erp-fixed-asset/          # 固定资产 (登记、折旧、转移、处置、盘点)
│
├── crm/                          # 客户关系管理 (4个子模块)
│   ├── customer/                 # 客户管理 (线索、机会、跟进、公海池)
│   ├── quotation/                # 报价管理 (报价单、报价模板)
│   ├── contract/                 # 合同管理 (合同、条款、变更、付款计划)
│   └ marketing/                  # 营销活动 (活动、渠道、内容、目标)
│
├── wms/                          # 仓储管理系统 (18个实体)
│   └─ 仓库、库位、收货、上架、拣货、发货、移库、盘点、库存日志
│
├── dms/                          # 配送管理系统 (25+个实体)
│   └─ 配送渠道、订单池、调度、骑手、车辆、路线、任务、签收、收款、轨迹
│
├── sync-engine/                  # 同步引擎
├── infrastructure/               # 基础设施
└── tests/                        # 测试模块
```

## 实体统计

| 模块 | 实体数 | 核心实体 |
|------|--------|----------|
| core-base | 34 | User, Role, Permission, SysTenant, SysDept, SysMenu, Workflow* |
| erp-purchase | 20 | PurchaseOrder, PurchaseInbound, PurchaseReturn, PurchaseContract |
| erp-sales | 12+ | SaleOrder, SaleOutbound, SaleReturn, SaleExchange, PromotionActivity |
| erp-stock | 40+ | Product, Stock, StockCheck, StockTransfer, BatchNumber, SerialNumber |
| erp-finance | 35+ | Receivable, Payable, Voucher, Payment, Receipt, Invoice, Expense |
| erp-partner | 10+ | Partner, PartyContact, CustomerLevel |
| erp-fixed-asset | 8+ | FixedAsset, FixedAssetDepreciation, FixedAssetTransfer |
| crm | 20+ | Customer, CustomerLead, Quotation, Contract, MarketingCampaign |
| wms | 18 | WmsWarehouse, WmsLocation, WmsReceipt, WmsPick, WmsShip |
| dms | 25+ | DmsChannel, DmsOrderPool, DmsRider, DmsVehicle, DmsTask |
| **总计** | **220+** | |

## 快速启动

```bash
# 1. 初始化数据库
psql -U postgres -f backend/sql/00_init_all.sql

# 2. 启动应用
cd backend
mvn clean install -DskipTests
java -jar core/api/core-api/target/*-exec.jar --spring.profiles.active=dev

# 3. 访问 API 文档
open http://localhost:8080/doc.html
```

## 安全机制

- **认证**: Sa-Token JWT 模式
- **授权**: @SaCheckPermission 注解控制接口权限
- **SQL注入防护**: AOP 切面 + Filter 双重防护
- **XSS 防护**: 请求参数清洗 + HTML 转义
- **密码加密**: BCrypt (Spring Security PasswordEncoder)
- **操作审计**: @OperLog 注解 + AOP 自动记录
- **乐观锁**: BaseEntity @Version 并发控制

## 对标分析

| 系统 | 覆盖率 | 定位对比 |
|------|--------|----------|
| 管家婆 | 95% | ✅ 已超越 |
| Odoo商贸版 | 85% | ✅ 接近达标 |
| 金蝶云星空 | 83% | ⚠️ 缺HRM+审批流程 |
| 用友U8 | 83% | ⚠️ 同上 |
| SAP S/4HANA | 60% | ❌ 定位不同（大型制造业旗舰） |

## 相关文档

- [API文档](api/erp-all-modules-api.md)
- [下一阶段任务](../../docs/roadmap-next-phase.md)
- [归档文档](archive/) - 历史阶段性报告