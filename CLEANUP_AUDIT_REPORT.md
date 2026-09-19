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

---

# 治理机制落地（2026-09-19 第二轮）

> 死代码治理是机制而不是项目：上面删掉的 330 个文件只是结果，防止再生才是收益。
> 本节记录本轮落地的两条门禁与三份待裁决清单。

## ② 装配门禁测试（已落地）

**文件**：`backend/core/api/core-api/src/test/java/cn/aiedge/architecture/ComponentScanCoverageTest.java`
**基线**：`backend/core/api/core-api/src/test/resources/known-unscanned-controller-packages.txt`

**它断言什么**
1. 任何含 `@RestController`/`@Controller` 的包，必须落在「`AiReadyApplication.scanBasePackages` + 被覆盖的 `@ComponentScan` 闭包」内，或在基线里显式豁免；
2. 基线不得过期（已装配/已无控制器的包必须从基线删除）——保证清单只会缩小；
3. 扫到的控制器数量不得低于 300（实测 394）——**防止门禁因 classpath 不完整而空跑成装饰**。

**为什么落在 core-api**：`core-api` 是唯一依赖全部 23 个业务模块的模块，其测试 classpath 能看见所有控制器。
（对照：`backend/tests` 模块的 `ArchitectureComplianceTest` 只依赖 `core-base`，其 `importPackages("cn.aiedge.erp")` 扫不到任何类 → 规则全部空跑通过，属于"从来没红过的守卫"。建议把该测试迁到能看见全量 classpath 的模块，或给它补一条与本门禁相同的数量下限断言。）

**当前状态**：13 个包 / 19 个控制器在基线内（即已知未装配），其余全部装配。新增漏配会直接 CI 红。

**实现注记（踩过的坑）**：`@ConditionalOnProperty` 的类会被父类扫描器在**扫描阶段**就用 `ConditionEvaluator` 跳过。
实测：`cn.aiedge.mq.controller` 的两个控制器带 `@ConditionalOnProperty(mq.rabbit.enabled=true)`，在无条件环境下根本扫不到，
于是"基线过期检查"把它误判成"包里已没有控制器"。装配门禁要回答的是静态问题（这个包在不在扫描范围内），
与运行期开关无关，故测试内用 `StaticAnnotationScanner` 子类绕开了条件评估（Spring 6.1 未提供公开开关）。

**验证（守卫有牙齿）**——不是"跑通了"，而是"该红时确实红"：
| 探针 | 期望 | 实测 |
|------|------|------|
| 从基线里删掉 `cn.aiedge.report.controller` | 断言 1 失败并列出未装配控制器 | ✅ 红：逐条列出 `ReportController` / `ReportAnalyticsController` / `ReportScheduleController` |
| 把控制器数量下限临时改成 99999 | 断言 3 失败 | ✅ 红：`只扫描到 396 个控制器（期望 ≥ 99999）` |
| 还原后重跑 | 3/3 通过 | ✅ 绿（BUILD SUCCESS） |

（顺带确认：算上被条件过滤的类，运行期实际扫到 396 个控制器，源码口径 394 —— 差值即上述 mq 两个控制器。）

## ③ 仓库产物门禁（已落地）

**文件**：`tools/check-repo-hygiene.sh`（本地可跑）
**接入**：`.github/workflows/ci-optimized.yml` 新增 `repo-hygiene` 作业（阶段 0，独立于变更检测，秒级）

**规则**：`git ls-files` 命中 `*.log|*.bak|*.tmp|*.pyc|*.pyo|*.swp|*.orig|*.rej`、`__pycache__/`、`*.tsbuildinfo`、`vite.config.ts.timestamp-*`、`node_modules/`、`dist/`、`playwright-report/`、`test-results/`、`.DS_Store` 即失败；另拒绝 >5MB 的已提交文件。

**落地时它立刻清出的历史欠账**（说明规则不是空转）：
- 34 个 Python 字节码（`backend/sync-engine/**`、`backend/tests/additional-tests/**`、`tools/__pycache__/`）此前一直入库；
- `frontend/playwright-report/index.html`（0.5MB 的 HTML 报告）、`frontend/test-results/.last-run.json`、`frontend/apps/pc-admin/test-results/.last-run.json`。
以上已 `git rm --cached`（工作区文件保留）+ 补 `.gitignore`。

**注意（反例）**：`frontend/apps/pc-admin/package-lock.json` 看似 npm 残留，实际被 `ci-optimized.yml:255-266` 的 `npm ci` 使用——**不能删**。这就是"删除前必须验证引用"的具体代价。

## ④ 接口命中审计（运行期证据，已落地）

**文件**：`tools/audit-endpoint-hits.py`（自检：`python tools/audit-endpoint-hits.py --self-test`）

**为什么需要**：`tools/audit-api-usage.py` 只能证明"前端源码里没搜到调用"，证明不了"生产上没人调"。公网链接（扫码评价/签收）、外部系统回调（WMS→ERP）、移动端旧版本、第三方集成都不会出现在本仓库里。

**做法**：把访问日志（nginx combined / Tomcat access log 均可）与静态候选清单 `tools/audit-api-usage.json`（1118 + 104 个"前端从未调用"接口）对上，输出：
- ① 观察窗口内**零命中** → 可进入弃用流程（`@Deprecated` + 文档标注 + `Sunset` 响应头 → 再观察一个发布周期 → 删除）；
- ② **有命中**（静态漏判）→ 禁止删除，附命中次数与 UA。

**现状**：运行期调用日志目前只覆盖 `/api/open/**`（`ApiCallLogInterceptor` + `api_access_log` 表），**不覆盖** 11 个契约控制器所在路径。因此在观察期开始前，需要按脚本头部注释在 nginx 打开访问日志（或临时把拦截器扩到所需前缀）。观察窗口建议 2–4 周，覆盖月结/对账这类低频周期。

## ⑤ 未装配功能包决策清单（待裁决）

口径：`scanBasePackages` 闭包外、含控制器的包。共同特征——**代码在、表建了、控制器没装配、没有菜单、没有前端页面**（迁移里 `component` 引用为 0，`dynamicRoutes` 无对应键）。

| 包 | 类数 | 控制器 | 建表迁移 | 前端调用 | 文档提及 | 建议 |
|----|-----|-------|---------|---------|---------|------|
| `cn.aiedge.storage.*` | 29 | 3 | `sys_file`、`sys_file_permission` | 无（前端 `/api/file` 走 `cn.aiedge.common.file`，已装配） | 《存储配置开发文档》 | **删除**：与 `platform.StorageConfigController` + `common.file.FileUploadController` 重复 |
| `cn.aiedge.report.*` | 22 | 3 | `report_definition/schedule/schedule_log` | 无 | 无 | 删除（未接线；注意 `ErrorReportController` 在 `base.log`，那条链是通的） |
| `cn.aiedge.search.*` | 22 | 2 | 无 | 1 处 `searchHotApi`，但该 api 函数无人调用 | 无 | 删除（两侧都没接线） |
| `cn.aiedge.knowledge.*` | 17 | 1 | `kb_knowledge_base/document/document_chunk` | 无 | 3 篇（含 gap-analysis） | 裁决：推荐删实现，只按 `AGENTS.md`「AI 只保留接口」在 `core-agent` 保留契约 |
| `cn.aiedge.gateway.*` | 15 | 2 | `gateway_log` | 无 | 19 篇（多为通用提及） | **删除**：`GatewayAutoConfiguration` 已被主应用 `exclude`，微服务网关方案已废弃 |
| `cn.aiedge.mq.*` | 13 | 2 | 无 | 无 | 1 篇 | 删除（`@ConditionalOnProperty(mq.rabbit)` 未开启） |
| `cn.aiedge.recommendation.*` | 10 | 1 | `rec_recommendation`、`rec_user_behavior` | 无 | 2 篇 | 删除或按产品决定 |
| `cn.aiedge.webhook.*` | 10 | 1 | `sys_webhook`、`sys_webhook_log` | 无 | 1 篇 | 删除 |
| `cn.aiedge.agent.*` | 29 | 2 | `ai_agent*` | 无 | `AGENTS.md` 声明「接口预留」 | **保留接口、删实现**（需与 `AGENTS.md` 的预留范围对齐） |
| `cn.aiedge.assistant.*` | 8 | 1 | `assistant_conversation` | 无 | 无 | 删除（与 `core-agent` 定位重叠） |
| `cn.aiedge.feedback.*` | 6 | 1 | 无表 | 无（mobile-admin 只 `push('/feedback')`，该路由不存在） | 35 处「反馈」多为泛述 | 删除，或补齐 mobile 反馈页 |

**每条裁决三选一**（都要带 owner 与期限，不能停在中间）：
- **a) 要** → 加进 `scanBasePackages`（或某个被扫描的 `@ComponentScan`）→ 从基线清单移除该行 → 补一条能打到端点的冒烟。**没有冒烟就不算"要"**。
- **b) 不要** → 删代码 → 表按下方 ⑥ 的流程处理。
- **c) 暂缓** → 实现收进 `cn.aiedge.experimental.*`，加 `@ConditionalOnProperty` 默认关，并在 CI 里禁止新代码依赖。

**顺带发现（文档与代码不一致，需更正）**：`docs/Yh-Spec/手动整理对标开发文档/系统模块/存储配置开发文档.md:54` 与 `:371-378` 断言「`/api/storage` 前缀是'活的'」——实际 `cn.aiedge.storage` 从未进入 `scanBasePackages`（同仓 `scheduler/controller/SystemTaskController.java:109-110` 的注释也确认了这一点），因此 `/api/storage/**` 整体 404，不是"活的前缀"。该文档据此得出的排查结论需要更正。

## ⑥ 孤儿数据库表清单与标记方案（只标记，未建迁移）

**口径**：本轮删除的实体所对应的表，且**当前源码零引用**（含 raw SQL）。

**确认为孤儿（30 张，均已有 CREATE TABLE 迁移）**：
`fee_application`、`fee_application_item`、`fee_approval_record`、`fee_payment_record`、`fee_reimbursement`、`fee_reimbursement_item`、`fee_statistics`、`finance_auxiliary_balance`、`erp_commission_settlement_config`、`erp_marketing_discount`、`erp_marketing_freight`、`erp_marketing_gift`、`erp_marketing_rule_product`、`erp_marketing_threshold`、`erp_marketing_tiered`、`erp_kit_assembly`、`erp_kit_assembly_item`、`erp_kit_disassembly`、`erp_kit_disassembly_item`、`purchase_supplier_candidate`、`purchase_supplier_evaluation`、`purchase_supplier_quotation`、`batch_rule`、`batchsn_audit_log`、`batch_snapshot_cache`、`serial_status_cache`、`traceability_log`、`erp_supplier_benefit`、`erp_supplier_notification`、`erp_supplier_points_rule`
（多数由 `V9.32.0__Create_All_Missing_Tables.sql`、`V9.34.0__Backfill_Missing_Tables.sql` 批量建出——**这正是"表能建、点不动"的源头**：按实体批量建表，与功能是否接线无关。）

**仍被使用、明确排除（2 张）**：
- `mkt_presale_order` —— 被 `erp-marketing/mapper/MarketingQueryMapper.java:161` 的 raw SQL 查询；
- `biz_party_transaction` —— 被 `erp-partner/party/mapper/PartyMapper.java:15` 查询，且列入 `base/config/MyBatisPlusConfig.java:97` 的多租户表清单。
（实体类被删≠表可删：这两张表证明必须按 SQL 引用而非实体是否存在来判断。）

**流程（不要与代码删除同批）**：
1. 先观察：`pg_stat_user_tables` 的 `seq_scan/idx_scan` + `pg_stat_statements` 观察 1–2 个发布周期，确认零访问；
2. 再改名：新增 Flyway 迁移 `ALTER TABLE x RENAME TO zz_deprecated_<x>_<date>`（或移入 `archive` schema），迁移注释写明"为什么可以 drop、观察了多久、证据是什么"；
3. 再 drop：下个发布周期无异常后，用 Flyway 迁移 drop，并同步删除本清单条目。
4. 前置：确认备份可用（`pg_dump` 或已有备份且验证过可恢复）——**数据不可回滚，代码可回滚**。

## ⑦ 日志归档与历史瘦身（方案，未执行）

**工作区产物（未跟踪，可先归档再删）**：
- `backend/*.log` 共 299MB（`dq-start.log` 72MB、`preorder-backend-5777.log` 63MB、`bank-run2.log` 43MB、`customer-api-5671.log` 18MB …）
- `frontend/.._tool-results_*.png`（3 个路径 glob 写错产生的文件名）、`backend/cols.tmp`
- `backend/**/.atcode` + `frontend/**/.atcode` 共 285 个文件 / 57 个目录（工具运行时状态，已在 `.gitignore`）

**建议顺序**：① 门禁已就位（防新增）→ ② 需要留档的压缩归档到仓库外（`tar -czf` 后校验可解压）→ ③ 删除工作区产物。
**历史瘦身**（把已入库的大文件从 `.git` 里真正去掉）需要 `git filter-repo` **改写历史**，会让所有人的本地克隆失效，必须在全团队知情的窗口做，且要配合 `git push --force-with-lease` 与重新克隆指引——**单独立项，不要在清理里顺手做**。
**执行前必做**：`git count-objects -vH` 与 `git rev-list --objects --all | git cat-file --batch-check` 找出历史大对象清单，用它决定值不值得瘦身（若历史里主要是 `.log`，收益明显；若只是几张截图，收益有限）。
