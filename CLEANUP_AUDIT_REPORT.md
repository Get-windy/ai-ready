# AI-Ready 项目冗余与废弃代码审计报告

> 生成时间: 2026-06-25
> 审计范围: 前端(pc-admin) + 后端(Java) + 数据库(SQL)
> **执行状态: P0/P1/P2 + wh/清理 + 关联表实体去重 + 孤立页面清理 + 菜单注册已完成**

---

## 已完成清理

### P0 — 已释放空间: >7.7GB
- [x] 删除 `backend_new.log` (7.2GB)
- [x] 删除 `backend.log` (1.5MB)
- [x] 清理所有Maven `target/` 目录 (~325MB)
- [x] 删除前端 `.png`、`.log`、`vite.config.ts.timestamp-*` 文件

### P1 — 废弃模块/目录清理
- [x] 删除 `infrastructure/` 目录（62文件）
- [x] 删除 `backend/schema/` 目录（h2-create-tables.sql过时）
- [x] 删除 `backend/sql/` 中6个已被Flyway替代的SQL文件
- [x] 将 `alter_print_client_add_machine_id.sql` 迁移为Flyway格式 `V9.3.0__Add_Print_Client_Machine_Id.sql`
- [x] 删除前端 `views/sale/` 目录（仅1个constants文件）
- [x] 删除前端 `views/stock/` 目录（仅1个文件）
- [x] 更新 `dynamicRoutes.ts` 中对已删除目录的2处引用

### P2 — 空壳模块/重复实体清理（第一轮）
- [x] 删除 `erp-purchase-contract` 空壳模块目录
- [x] 删除重复实体 `party/entity/PartyContact.java`（0引用，与partner/entity/完全重复）
- [x] 删除重复实体 `pricing/entity/PriceStrategy.java` 和 `PriceStrategyMapper.java`（无实际使用）

### sync-engine 构建集成
- [x] 添加 `sync-engine/Dockerfile`
- [x] 创建根目录 `docker-compose.yml`（postgres + backend + sync-engine）
- [x] 在 `pom.xml` 添加 `with-sync-engine` profile 支持 Docker 构建

### wh/ 死代码清理（2026-06-25 新增）
- [x] 删除 `views/wh/` 目录（30个Vue文件，调用不存在的 `/api/wh/*` 后端接口）
- [x] 删除 `dynamicRoutes.ts` 中 44 处 wh/ componentMap 映射
- [x] 创建 Flyway 迁移 `V9.4.0__Remove_Wh_Dead_Menu_Items.sql` 清理 33 个 wh/ 菜单项
  - 删除 V6.13.0 的 mega:wh:* 父目录菜单（10个，ID 60301-60310）
  - 删除 V6.21.0 的叶子菜单（15个，ID 70101-70142）
  - 删除 V6.22.0 的叶子菜单（8个，ID 80010-80017）

### 关联表实体去重（2组，2026-06-25 新增）
- [x] **sys_user_role**: 删除 `UserRole.java` + `UserRoleMapper.java` + XML，统一使用 `SysUserRole`
  - 更新 `UserServiceImpl`、`UserServiceOptimizedImpl`、`RoleServiceImpl` 中的引用
- [x] **sys_role_permission**: 删除 `RolePermission.java` + `RolePermissionMapper.java` + XML，统一使用 `SysRolePermission`
  - 更新 `RoleServiceImpl`、`PermissionServiceImpl` 中的引用
- [x] 共删除 6 个文件，更新 4 个 service 文件

### 孤立页面与分析产物清理（2026-06-25 第三轮）
- [x] 删除 `workflow/definition/` 子目录（2个旧版文件，已被顶级 instance-monitor/process-analysis/task-management 替代）
- [x] 删除 `workflow/task/` 子目录（2个旧版文件）
- [x] 删除 `workflow/instance/` 子目录（1个旧版文件）
- [x] 删除 `finance/account-subject/`（被 finance/subject/ 替代）
- [x] 删除 `finance/cost/list.vue`（无路由引用）
- [x] 删除 `finance/profit/list.vue`（被 finance/profit-report/ 替代）
- [x] 删除 `finance/trial-balance/`（无路由引用）
- [x] 删除 `admin/dev/codegen/`、`admin/module/error-stats/`、`admin/platform/dict/`（无路由引用）
- [x] 删除 `erp/column-config/SaleOrderFormConfig.vue`（被 SaleOrderItemColumnConfig.vue 替代）
- [x] 删除 `sales/order/index.vue`（被 erp/sale/index.vue 替代）
- [x] 删除 `system/department/personnel.vue`（无路由引用）
- [x] 删除 `purchase/order/index.vue`（被 erp/purchase/index.vue 替代）
- [x] 删除 views/ 根目录下 7 个分析产物 .txt 文件和 1 个 compare_files.py

### 占位符页面清理（2026-06-25 第四轮）
- [x] 确认 `finance/other-income-doc/form.vue` 不存在（文件已删除）
- [x] 删除 `dynamicRoutes.ts` 中 `finance/other-income-doc/form` 死 componentMap 条目

### 待激活页面注册（2026-06-25 第四轮）
- [x] 创建 Flyway 迁移 `V9.5.0__Register_HR_Trade_Quality_Payment_Menus.sql`
  - 新增 4 个一级 Mega 菜单（60014-60017）
  - 新增 17 个列分组目录节点（61401-61704）
  - 新增 20 个叶子菜单（90001-90304）
- [x] 在 `dynamicRoutes.ts` componentMap 中注册 20 个 Vue 组件映射
- [x] 在 `dynamicRoutes.ts` MODULE_ROUTE_MAP 中新增 hr/trade/quality/payment 4 个模块
- [x] 所有菜单使用 `client_type='tenant-admin'`（V6.23.0 后标准值）

**注册详情**:

| 模块 | 一级菜单ID | 页面数 | 叶子菜单ID范围 |
|------|-----------|--------|--------------|
| HR（人力资源）| 60014 | 6 | 90001-90006 |
| Trade（交易）| 60015 | 7 | 90101-90107 |
| Quality（质量）| 60016 | 3 | 90201-90203 |
| Payment（支付）| 60017 | 4 | 90301-90304 |

### 前端目录分析结论
- [x] **supplier/** vs **md/supplier/** vs **crm/supplier/** — 确认各有分工，不冗余
  - `supplier/`: ERP 供应商运营（创建/编辑/详情/询价/绩效）
  - `md/supplier/`: 主数据管理（合作伙伴视角，多角色）
  - `crm/supplier/`: CRM 供应商关系评估
- [x] **sales/** vs **erp/sale/** — 确认各有分工，不冗余
  - `sales/`: 简易操作视图（`/api/sales/order`）
  - `erp/sale/`: 完整 ERP 销售流程（审批/统计/工作流，`/api/erp/sale/order`）
- [x] **purchase/** vs **erp/purchase/** — 同上分工

---

## 待处理项（需专门重构项目）

### 1. ~~有完整实现但未注册路由的页面（待激活）~~ ✅ 已完成

已通过 V9.5.0 Flyway 迁移 + dynamicRoutes.ts 更新完成注册。详见"已完成清理 > 待激活页面注册"章节。

### 2. ~~占位符页面（待实现）~~ ✅ 已确认

`finance/other-income-doc/form.vue` 文件已不存在，对应的 componentMap 死条目已清理。

### 3. 平行服务栈实体重复（5组）— 需架构级重构

**问题根因**: 每个核心实体（User/Role/Permission）存在两套完整的平行实现：

| 实体 | 旧版服务栈 | 新版服务栈 | 引用对比 | 设计差异 |
|------|-----------|-----------|----------|---------|
| sys_user | User→UserServiceImpl→UserController | SysUser→SysUserServiceImpl→SysUserController | 122 vs 59 | BaseEntity继承 vs 独立定义 |
| sys_role | Role→RoleServiceImpl→RoleController | SysRole→SysRoleServiceImpl→SysRoleController | 105 vs 59 | 同上 |
| sys_permission | Permission→PermissionServiceImpl→PermissionController | SysPermission→SysPermissionServiceImpl→SysPermissionController | 185 vs 33 | 同上 |
| sys_user_role | ~~UserRole~~ → UserServiceImpl | SysUserRole → SysUserServiceImpl | **已合并** | ✅ |
| sys_role_permission | ~~RolePermission~~ → RoleServiceImpl | SysRolePermission → SysRolePermissionServiceImpl | **已合并** | ✅ |

**字段差异**（旧版 vs 新版）:

| 实体 | 旧版独有字段 | 新版独有字段 | 类型差异 |
|------|-------------|-------------|---------|
| User/SysUser | realName, version(BaseEntity) | userType, isTenantAdmin, loginCount, passwordUpdateTime, extInfo | 无 |
| Role/SysRole | parentId, version(BaseEntity) | 无（SysRole已包含version） | roleType: String vs Integer |
| Permission/SysPermission | remark, version(BaseEntity) | 无 | permissionType: String vs Integer, visible: Boolean vs Integer |

**重构方案**:
1. 选定标准版本（建议 Sys* 版本，支持多租户）
2. 将旧版独有字段（realName, parentId, remark, version）加入 Sys* 实体
3. 逐个迁移服务实现，合并两套 Service/Controller/Mapper
4. 更新所有引用并删除旧版实体及服务
5. 完整回归测试

**风险**: 高。涉及 400+ 引用、12+ 个 service 文件、12+ 个 controller 文件。建议作为独立重构项目。

### 4. 跨模块实体重复（2组）— 需业务决策

| 表名 | 位置A | 位置B | 分析结论 |
|------|-------|-------|---------|
| erp_purchase_order | erp-purchase(79字段，采购视角) | erp-sales(64字段，销售视角) | **真正重复**，同表两实体，需确定统一归属模块 |
| sys_field_permission | core/base(4引用，角色级字段可见性) | core/platform(4引用，用户/组级权限) | **分层设计**，但同表有冲突风险 |

**建议**:
- `erp_purchase_order`: 统一到共享模块（如 core 或新建 erp-common），两模块引用同一实体
- `sys_field_permission`: 确认是否为有意的分层设计，若是则应分表；否则统一

---

## 清理统计汇总

| 维度 | 清理前 | 清理后 | 变化 |
|------|--------|--------|------|
| 后端顶层模块 | 10个 | 8个 | -2(infrastructure+schema) |
| ERP子模块 | 14个 | 13个 | -1(erp-purchase-contract) |
| 重复实体定义 | 9组 | 4组 | -5组(2关联表+2首+1字段) |
| 前端views冗余目录 | 3个(wh/+sale/+stock/) | 0个 | -3个 |
| 前端Vue死代码 | ~56个 | 0个 | -56个(30 wh/ + 13 孤立 + 5 workflow旧版 + 7 txt + 1 py) |
| SQL冗余文件 | 7个 | 0个 | -7个 |
| wh/数据库菜单项 | 33个 | 0个 | -33个 |
| 磁盘空间释放 | — | >7.7GB | ✅ |
| 删除的Java文件 | — | 6个 | 旧实体/mapper/xml |
| Flyway新增迁移 | — | V9.3.0, V9.4.0, V9.5.0 | +3 |
| sync-engine Docker | 未集成 | Dockerfile+compose+profile | ✅ |
| 未注册功能页面 | 20个 | 0个 | -20个(6 HR + 7 Trade + 3 Quality + 4 Payment) |
| 新增Mega菜单 | — | 4个一级 + 17个列分组 + 20个叶子 | +41菜单项 |

---

## 后续建议

### 短期（本周）— 已完成 ✅
1. [x] 为 `sync-engine` 添加Docker配置
2. [x] 删除 `wh/` 死代码（30 Vue + 33 菜单项）
3. [x] 合并关联表重复实体（UserRole/RolePermission）
4. [x] 确认前端 supplier/sales/purchase 目录各有分工
5. [x] 注册 20 个待激活页面到菜单系统（V9.5.0 + dynamicRoutes.ts）
6. [x] 清理 `finance/other-income-doc/form` 死 componentMap 条目

### 中期（本月）
1. [ ] 规划 base 模块平行服务栈合并重构（User/Role/Permission 三组）
2. [ ] 确认 `erp_purchase_order` 跨模块归属
3. [ ] 确认 `sys_field_permission` 分层设计意图

### 长期（下季度）
1. [ ] 执行 base 模块实体合并重构
2. [ ] 建立代码冗余检测机制（CI检查重复实体定义）
3. [ ] 完善模块文档，明确各模块职责边界
4. [ ] 定期执行冗余审计（建议每季度一次）

---

# 第二轮死代码清理（2026-09-19）

> 范围：backend（3514 个主源码 java）+ frontend（apps/packages）
> **执行状态：已删除 330 个文件（后端 258 / 前端 72）；其中约 120 个已被并行提交 `6acdb6374` 一并带入版本库（4 个被 git 识别为重命名），其余 210 个留在工作区待提交**

## 判定方法（每条结论都可复核）

| 判据 | 做法 | 关键否证 |
|------|------|----------|
| ① 全仓库文本零引用 | 对每个类名在全仓库（java/xml/yml/ts/vue/cjs/sql/md，排除 node_modules/target/dist）统计"除定义文件外是否还有文件提及" | ❌ 不能只看"名字出现一次"：`@Service`/`@Controller`/`@Configuration` 靠注解装配，`*Impl` 经接口注入，名字天然只出现一次（首轮 817 个此类候选里绝大多数是活的） |
| ② Spring 装配可达性 | 以 `AiReadyApplication` 的 `scanBasePackages` / `@MapperScan` / `@EntityScan` / `@EnableJpaRepositories` + 各 `@ComponentScan` 求闭包，再对 bean 做「接口是否有人注入」判定 | ❌ `implements` 写在续行会被漏读（`FeeApplicationServiceImpl` 即此坑） |
| ③ HTTP 端点调用方 | 类级+方法级映射拼出真实路径，在前端 6 端 + `tools/` + `scripts/` + `docs/` 搜三种形态（完整路径 / 去 `/api` 前缀 / 辨识段） | ❌ 简单名 grep 分不清同名类（`InvoiceApplication` 既是实体又是模块入口）；包级引用（扫描配置）在类名搜索中**不可见** |
| ④ 契约与在写工作排除 | 文档/Javadoc 声明对外契约的、`git status` 为 M/A（并行会话正在改）的一律不删 | ❌ 目录里已有 `libre` 之外的东西——`supplier/notification` 被显式登记在 `@EntityScan`/`@EnableJpaRepositories`，属"已登记未接线"，不是死代码 |

## 已清除的表面

**1) 零引用末端类型（161 个）**：无任何生产消费者的 DTO/VO/枚举/实体/Mapper/工具类/注解/占位类。
- 典型：`crm/customer/dto/*`（8，被新 CRM 体系取代）、`erp-marketing/{Discount,Freight,Gift,RuleProduct,Threshold,PresaleOrder}` 实体+Mapper（促销老模型，随对应 Mapper 一起成簇死亡）、`erp-finance/invoice/mapper/*`（5）、`erp-pricing/strategy/mapper/*`（4）、`erp-purchase/{SupplierManagement,SupplierSelection}Service` 等。
- 工具残骸：`common/cache/{Dict,Report,Stock,UserPermission}CacheService`（手写 TTL 缓存，已被 Spring Cache 取代）、`IdGenerator`（已被 MyBatis-Plus `ASSIGN_ID` 取代）、`SystemConstants`（其中 `status` 语义与项目规范相反，佐证其已被弃用）、`BaseMessageConsumer`（无子类的 MQ 基类）、`common/cache/annotation/CachePut`（自定义缓存注解，无人使用）。

**2) 被取代的平行实现（46 个）**
- `core-api / cn.aiedge.erp.expense` 整套 `Fee*`（5 接口 + 5 实现 + 7 实体 + 7 Mapper + 17 DTO = 41）：被 `erp-finance` 的 `Expense*`（JPA + `/erp/expense/**` 端点，前端 `api/erp/expense/index.ts` 实际调用）取代。
- `erp-pricing / pricing.execution`（5）：`IPriceStrategyExecutor` 及其 DTO 被 `PriceEngineService`（`/erp/pricing/**` 活端点）取代。
- 装配/拆分单（20）：`erp-sales / product.kit` 里的 `KitAssembly*`/`KitDisassembly*`（控制器+服务+DTO+实体+Mapper+枚举，全簇自洽零外引）被 erp-stock 的 `/erp/stock/assemble`、`/erp/stock/split` 取代。
  - ⚠️ 同目录的 `ProductKit*`（套件）**是活的**（前端 `api/erp/mall.ts:1109` 调 `/erp/product-kit/*`），已保留。

**3) 废弃的架构层（模块级独立入口，24 个）**：`@SpringBootApplication`/`@Configuration` 逐模块入口（`DmsApplication`、`BudgetApplication`、`MallApplication`、`GatewayApplication`、`MinimalApplication`、`BatchSnApplication`、`SaleReturnApplication` …）。这些是「每模块一个微服务」方案被模块化单体取代后的残留：其自身包已被主应用 `scanBasePackages` 覆盖（重复扫描），或根本不在任何扫描范围内（永不注册）。主入口 `AiReadyApplication` 保留。

**4) 未接线簇（7 个）**：`CommissionSettlementConfig{实体,Mapper,Service,Impl}`（空实现壳，前端无任何页面/端点）、`WebSocketAuthInterceptor`（`HandshakeInterceptor` 但没有任何 `WebSocketConfigurer` 注册它）、`BalanceController`（`/api/erp/marketing/balances` 全仓零调用方，其数据层被别处使用故仅删控制器）、`ProductRecommendController`（前端用的是 `/erp/product/recommends`，服务被 `ProductController` 复用故仅删控制器）。

**5) 前端（72 个）**
- pc-admin 未可达页面：`views/wms/{move,pick,putaway,ship}/*`（8，`componentMap` 已把 `wms/*` 指向 `wh/*-order`）、`finance/trial-balance`（被 `finance/balance-report` 取代）、`finance/subsidiary-balance`（被 `finance/aux-balance` 取代；文档《辅助核算余额表开发文档》自己标注其为"无路由、无菜单的孤儿页，建议后续清理"）。
- pc-admin 未接线组件：`erp/product/components/Product{Attachments,Attributes,Barcodes,Related,Units}Panel.vue`（5）、`md/components/PartnerFormLayout.vue`、`purchase/exchange/components/Exchange{Approve,Detail,Track}Modal.vue`（3）。
- 未使用工具库：`utils/{logger,cache,worker,preload,performance,image,responsive,accessibility,errorHandler,batchOperations,formPersistence,formValidation,asyncComponent}`、`composables/useIntervalRefresh`、`layouts/components/{Breadcrumb,RouterCache}`（布局内已用 antd `a-breadcrumb` 与内联 `keep-alive`）。
- 死 barrel/入口：`components/index.ts`、`{BillTableList,CategoryListLayout,FormField,SearchFieldsGrid,SettingsLayout,Skeleton}/index.ts`（7 个纯 re-export 入口，兄弟组件另有直接引用/自动导入）、`LazyImage.tsx`、`VirtualScroll.tsx`、`api/retail.ts`。
- 其它端：mobile-admin `{Contract,Invoice,Quotation}Form.vue`（3）、pda-warehouse `stores/task.ts`、print-client `main/services/apiClient.ts` + `renderer/stores/printer.ts`（均经 dist 产物反证未进 bundle）。
- 共享包 `packages/components`：`mobile/*`（4，且 `mobile/index.ts` 的相对路径 `./mobile/ARMobileScanner.vue` 解析后不存在，一旦被 import 即构建失败）、`base/{picker/ARDatePicker,upload/ARUpload}.vue`（仅被上述坏入口引用）、`feedback/{ARAlert,AREmpty,ARError,ARLoading}.vue`、`business/erp-inventory/batch-management/{BatchList.vue,types.ts}`、`business/erp-purchase/PurchaseOrderForm.vue`。
- 遗留产物：`_probe_{cfg,dbg,trade_recon}.js`（无文档引用的探针残次品，其余 11 个探针仍被对标文档引用，保留）、`page-list.json`、`scan-results.json`、`pnpm-8.15.9.tgz`（3.8MB，CI/Docker/文档零引用）、2 个 `*.vue.bak`。

**6) 安全项（1 个）**：`core-api / cn.aiedge.runner / PasswordResetRunner` —— `@Component implements CommandLineRunner`，每次启动把 `admin` 用户密码重置为硬编码 `admin123`。当前因 `cn.aiedge.runner` 未进 `scanBasePackages` 而处于休眠；但本项目正在批量补扫描包（见 0.3.21 提交），一旦补上即成为后门。已删除。

## 验证（带牙齿的门禁）

| 门禁 | 结果 |
|------|------|
| `mvn -o -DskipTests test-compile`（每次删除后重跑，共 4 次） | BUILD SUCCESS / EXIT=0 |
| `npx vite build` — pc-admin | EXIT=0 |
| `npx vite build` — mobile-admin / pda-warehouse / packages/components | 全部 OK |

删除前逐文件校验：受 git 跟踪 + 无未提交改动（`git status` 不含 M/A）——保证任何一笔都可用 `git checkout` 恢复。

## 复活条件

- `cn.aiedge.erp.expense` 的 `Fee*`：当出现真实产出方（控制器/前端页面）时，从 `git show 6acdb6374^:backend/core/api/...` 取回。
- 装配/拆分簇：当 `/erp/kit-assembly`、`/erp/kit-disassembly` 确有调用方时再取回（当前前端一律走 `/erp/stock/assemble|split`）。
- 前端未接线组件（Product*Panel / Exchange*Modal / PartnerFormLayout）：若属于规划中的商品表单 Tab、换货审批弹窗，请从 git 恢复并接线；否则保持删除。
- `packages/components/mobile/*`：若要在移动端复用，需要重写入口（现入口路径本身是坏的）。
- `PasswordResetRunner`：**不要复活**。如需重置密码，走运维脚本或带鉴权的管理端点。

## 范围外发现（未处理，建议单独立项）

1. **已实现但运行时永不装配的功能包**（不在 `scanBasePackages`，也不被任何被扫描的 `@ComponentScan` 覆盖 → 控制器 404、`@Scheduled` 不跑、`@PostConstruct` 不执行）。0.3.21 已补 `module/monitor/platform/datasource/export/devtool`，以下仍未装配：
   `cn.aiedge.{gateway.*, webhook.*, assistant.*, knowledge.*, mq.*, feedback.*, report.*, search.*, storage.*, recommendation.*, runner, inventory.repository, agent.*, erp.supplier.notification.service|config}`
   典型症状就是项目自己记录的「表能建、点不动」。**建议先决定每个包是「补扫描」还是「删代码」，不要靠逐个 404 排查。**
2. **无调用方但有对外契约声明的控制器（11 个，不可轻删）**：`BusinessAccountingController`（注释：供采购/销售/费用模块调用）、`ExpenseTypeController`（费用类型文档"保留原状"）、`IntegrationController`（第三方集成 + 接收 WebHook）、`SignatureController`/`RatingController`（签收/评价，疑公网）、`MallNoticeController`/`MallPartyLinkController`（商城端公开）、`wms/ErpCallbackController`（WMS 架构文档明列回调）、`GatewayManagementController`、`mq/*` 两个。
3. **已登记未接线**：`erp.supplier.notification`（实体与 JPA 仓库已进 `@EntityScan`/`@EnableJpaRepositories`，但 service 包未扫描、无消费者）→ 属"待接线"，不是死代码。
4. **契约文件未被采用**：`pc-admin/src/utils/priceLevelConfig.ts` 自称「全系统共享，勿重复定义」，但零引用；`api/erp.ts`、`api/erp/mall.ts`、`api/purchase-exchange.ts`、`api/wms/borrow.ts` 等各自内联了价格等级字段。文件已保留，建议改为全站唯一来源。
5. **设计文档描述了从未接线的组件库**：`frontend/docs/ui-components/feedback-components-usage.md` 通篇用 `@/components/@ai-ready/common/components/feedback` 这一不存在的路径，且 `ARSkeleton` 连文件都没有 → 建议删除或重写。
6. **遗留运行产物（未删，仅报告）**：`backend/*.log` 共 299MB（`dq-start.log` 72MB、`preorder-backend-5777.log` 63MB …，均未跟踪）、`frontend/.._tool-results_*.png`（3）、`backend/cols.tmp`、`backend/.atcode`+`frontend/**/.atcode` 下 285 个运行时文件（57 个目录）。
7. **孤儿数据库对象**：本轮删除的实体对应表（促销老模型 `marketing_discount/freight/gift/threshold`、装配拆分、`finance_auxiliary_balance`、`fee_*` 旧费用栈、`commission_settlement_config` 等）仍留在库中。删表属于格式决策，未处理。
8. **并行会话提交混入**：`6acdb6374 "feat: 营销模块全栈开发 + … + 死代码清理 + …"` 把本轮的 121 个删除混进了功能提交。清理与功能混在一起会让回溯困难，建议后续拆分提交。
