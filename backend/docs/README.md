# AI-Ready 后端文档

## 项目定位

AI-Ready 是一套面向**中小型商贸企业**的 ERP/CRM/WMS/DMS 一体化系统，对标 Odoo 商贸版 + 金蝶云星空（页面级对标系统为「来肯企汇 ql361 v2.2」），适配国内财务、发票、税务等业务场景。

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
backend/
│
├── core/                         # 核心基础模块群 (6子模块)
│   ├── base/core-base/           # 用户权限 + 系统管理 + 审计日志 + 工作流基础
│   │   └─ 34个实体: User, Role, Permission, SysTenant, SysDept, SysMenu,
│   │            SysOperLog, SysLoginLog, WorkflowDefinition, WorkflowInstance...
│   ├── api/core-api/             # API启动入口 + 全局配置 + 工作流引擎
│   ├── agent/core-agent/         # AI智能代理模块 (接口预留)
│   ├── platform/core-platform/   # 平台级功能
│   ├── payment/core-payment/     # 支付服务
│   └ notification/core-notification/ # 通知模块
│
├── erp/                          # ERP业务模块群 (14个子模块)
│   ├── erp-purchase/             # 采购管理 (采购订单、入库、退货、换货、询价、合同)
│   ├── erp-sales/                # 销售管理 (销售订单、出库、退货、换货、零售、预订货、促销、组装拆卸)
│   ├── erp-stock/                # 库存管理 (商品档案、库存、盘点、调拨、批次序列号)
│   ├── erp-finance/              # 财务管理 (应收应付、凭证、收付款、发票、费用报销)
│   ├── erp-partner/              # 往来单位管理 (客户/供应商档案、联系人、等级)
│   ├── erp-pricing/              # 定价管理
│   ├── erp-supplier-portal/      # 供应商门户 (询价报价、绩效、积分)
│   ├── erp-printing/             # 打印服务 (模板、任务、日志、签名)
│   ├── erp-delivery-route/       # 配送路线 (高德导航)
│   ├── erp-observability/        # 可观测性 (业务指标、系统监控)
│   ├── erp-budget/               # 预算管理 (Spring Data JPA)
│   ├── erp-mall/                 # B2B商城
│   ├── erp-marketing/            # 营销中心
│   └── erp-fixed-asset/          # 固定资产 (登记、折旧、转移、处置、盘点; Spring Data JPA)
│
├── crm/                          # 客户关系管理 (单模块，6业务包)
│   ├── customer/                 # 客户管理 (线索、机会、跟进、公海池)
│   ├── quotation/                # 报价管理 (报价单、报价模板)
│   ├── contract/                 # 合同管理 (合同、条款、变更、付款计划)
│   ├── marketing/                # 营销活动 (活动、渠道、内容、目标)
│   └ visit/                      # 拜访管理 (拜访规划/执行/检视)
│
├── wms/                          # 仓储管理系统 (22个实体)
│   └─ 仓库、库位、收货、上架、拣货、发货、移库、盘点、库存日志
│
├── dms/                          # 配送管理系统 (16个实体)
│   └─ 配送渠道、订单池、调度、骑手、车辆、任务、签收、收款、轨迹
│
├── hr/                           # 人力资源管理系统 (hr-base，10个实体)
│   └─ 组织、员工、考勤、请假、薪酬、绩效、招聘
│
├── sync-engine/                  # 同步引擎 (Python)
├── sql/                          # 数据库初始化脚本
└── tests/                        # 测试模块
```

## 实体统计

> 口径：MyBatis-Plus `@TableName` 实体数（源码唯一表名）；`erp-budget` 与 `erp-fixed-asset` 使用 Spring Data JPA `model`（非 @TableName，已单独计入）。

| 模块 | 实体数 | 核心实体 |
|------|--------|----------|
| core-base | 34 | User, Role, Permission, SysTenant, SysDept, SysMenu, Workflow* |
| core-其他 (payment/platform/agent/notification) | 21 | PaymentRecord, PlatformConfig, AgentRegistry, NotificationRecord |
| erp-purchase | 21 | PurchaseOrder, PurchaseInbound, PurchaseReturn, PurchaseExchange, PurchaseContract |
| erp-sales | 35 | SaleOrder, SaleOutbound, SaleReturn, SaleExchange, RetailOrder, SalePreOrder, PromotionActivity |
| erp-stock | 50 | Product, Stock, StockCheck, StockTransfer, BatchNumber, SerialNumber |
| erp-finance | 31 | Receivable, Payable, Voucher, Payment, Receipt, Invoice, Expense |
| erp-partner | 10 | Partner, PartyContact, CustomerLevel |
| erp-pricing | 10 | PriceRule, PriceTier |
| erp-supplier-portal | 10 | 询价/报价/绩效/积分 |
| erp-printing | 13 | 打印模板/任务/日志/签名 |
| erp-mall | 16 | 商城商品/订单/会员 |
| erp-marketing | 17 | 优惠券/促销/积分 |
| erp-budget | 6 | 预算(JPA) |
| erp-fixed-asset | 7 | FixedAsset, FixedAssetDepreciation, FixedAssetTransfer (JPA) |
| crm | 21 | Customer, CustomerLead, Quotation, Contract, MarketingCampaign |
| wms | 22 | WmsWarehouse, WmsLocation, WmsReceipt, WmsPick, WmsShip |
| dms | 16 | DmsChannel, DmsOrderPool, DmsRider, DmsVehicle, DmsTask |
| hr-base | 10 | 组织/员工/考勤/请假/薪酬/绩效/招聘 |
| **总计** | **355** | |

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
- **多租户**: AiReadyTenantLineInnerInterceptor 自动注入 tenant_id
- **SQL注入防护**: AOP 切面 + Filter 双重防护
- **XSS 防护**: 请求参数清洗 + HTML 转义
- **密码加密**: BCrypt (Spring Security PasswordEncoder)
- **操作审计**: @OperLog 注解 + AOP 自动记录
- **乐观锁**: BaseEntity @Version 并发控制

## 构建规范

- **胖 JAR 红线**: 仅 `core/api/core-api` 可声明 `spring-boot-maven-plugin`，纯库模块禁止，防止产生 28 个 ~110MB 胖 JAR（详见 `backend/AGENTS.md`）
- **AI 设计原则**: AI 是外部服务，只保留接口不实现算法
- **主键策略**: 新实体主键对齐所属模块基类（营销/采购用 `ASSIGN_ID` 雪花，勿混用 `AUTO`）

## 对标分析

| 系统 | 覆盖率 | 定位对比 |
|------|--------|----------|
| 管家婆 | 95% | ✅ 已超越 |
| Odoo商贸版 | 90% | ✅ 基本达标（HRM/BPM/POS 均已实现） |
| 金蝶云星空 | 88% | ✅ 接近达标 |
| 用友U8 | 88% | ✅ 接近达标 |
| SAP S/4HANA | 60% | ❌ 定位不同（大型制造业旗舰） |

## 相关文档

- [API文档](api/erp-all-modules-api.md)
- [对标执行手册](../../docs/Yh-Spec/ql361对标/README.md)
- [页面映射总表](../../docs/Yh-Spec/ql361对标/page-mapping.md)
- [归档文档](archive/) - 历史阶段性报告
