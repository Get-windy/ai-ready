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
