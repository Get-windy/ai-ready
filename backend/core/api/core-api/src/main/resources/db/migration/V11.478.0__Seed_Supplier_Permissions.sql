-- 供应商（supplier）权限码种子（E-01 supplier 批次，2026-09-21）
--
-- 【认证模型已核实：员工会话，不是"对外供应商门户"】模块名像对外门户，实测**不是**：
--   · 三个控制器（SupplierController / SupplierPortalController / SupplierPortalInquiryController）
--     没有任何设备令牌或独立账号体系；`SupplierPortalController` 的写端点语义全是**采购方**动作
--     （注释原文就是"采购方接受报价 / 采购方拒绝报价"）；
--   · 唯一前端调用方是 pc-admin（`views/supplier/inquiry/index.vue`、`views/supplier/detail.vue`、
--     `api/supplier.ts`），走 `utils/request.ts` 的 `Authorization: Bearer` ——
--     与后台**同一套 Sa-Token 会话**（不是设备身份）；
--   · 供应商档案页的菜单码是 `md:supplier`（资料 → 供应商），询价/绩效页挂在
--     `purchase:supplier-inquiry` / `purchase:supplier-performance` 菜单下，都是后台页面。
--   ⇒ **不排除**，整模块补码。（若将来真做"外部供应商登录"，应另立账号体系与网关头，而不是撤掉这批码。）
--
-- 【域口径】统一到新一级域 `supplier:`（模块归属 master-data，见 V11.483.0）：
--   两个门户 base（`/api/v1/supplier-portal`、`/api/supplier-portal`）若按路径推导会得到
--   `supplier-portal:` 这个**新一级域**，与 `/api/supplier` 分裂成两个域；而它们同属
--   "供应商"这一个业务对象（等级/绩效/询价/报价/积分都是供应商的属性）⇒ 归一到一个域。
--
-- 【只补不删 / 复用】本文件 47 端点 → 27 码，**全部新增**（库中 `supplier%` 零条），
--   没有被复用的历史码。NOT EXISTS 守卫保证重复执行不产生第二行。
--
-- 【口径要点】见 `MODULES['supplier']`：
--   · 主 CRUD 走**两段码** `supplier:<动作>`（与库中既有的 `party:create` / `purchase:manage`
--     同款写法；资源位取的是域自身，故 RESOURCE_LABEL 里把 `supplier` 映射成空串）；
--     子对象（level / performance / inquiry / quotation / points / dashboard）走三段码；
--   · 8 个 POST 会被通用兜底判成 `create`，已逐条改判：门户账号的激活/禁用/同步、
--     等级与合作状态更新 → `update`；导出 → `export`；验校 → `check`；绩效评估 → `performance:create`；
--   · `POST /inquiries/{id}/quotation`（供应商提交报价）= 新建一条报价 → `quotation:create`；
--     `POST /quotations/{id}/{accept,reject}`（采购方表态）= `quotation:approve`（不新造 accept 词）；
--   · 积分的加/减都是"改这个供应商的积分" → `points:update`（通用规则会把 consume 判成
--     动作词 `consume`=「占用」、把 add 判成 create，两者都是误义）。
--
-- 【id 号段】权限码 **126000 起**（slot=26）、角色关联 **9760000 起** —— 执行前实测两段 count(*) = 0。
--
-- 【遗留】`GET /api/supplier/list`（控制器注释"供下拉选择器使用"）**当前没有任何前端调用方** ——
--   采购类页面的供应商下拉已改走 `/erp/md/customer/list`（`api/options.ts:26-30` 有说明）。
--   本批照常给它补码（无调用方 ⇒ 不会造成回归）；将来若真有页面用它当跨页下拉源，
--   要按"数据源不是可勾选功能"的口径重评。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。
-- =============================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (126000, '供应商校验', 'supplier:check', '/api/supplier/validate', 'POST', 1200),
 (126001, '供应商新增', 'supplier:create', '/api/supplier', 'POST', 1201),
 (126002, '供应商工作台查看', 'supplier:dashboard:view', '/api/v1/supplier-portal/dashboard/{supplierId}', 'GET', 1202),
 (126003, '供应商删除', 'supplier:delete', '/api/supplier/{id}', 'DELETE', 1203),
 (126004, '供应商详情', 'supplier:detail', '/api/supplier/{id:\\d+}', 'GET', 1204),
 (126005, '供应商导出', 'supplier:export', '/api/supplier/export', 'POST', 1205),
 (126006, '供应商导入', 'supplier:import', '/api/supplier/import', 'POST', 1206),
 (126007, '供应商询价单新增', 'supplier:inquiry:create', '/api/v1/supplier-portal/inquiries', 'POST', 1207),
 (126008, '供应商询价单详情', 'supplier:inquiry:detail', '/api/v1/supplier-portal/inquiries/{id}', 'GET', 1208),
 (126009, '供应商询价单查询', 'supplier:inquiry:list', '/api/v1/supplier-portal/inquiries/supplier/{supplierId}', 'GET', 1209),
 (126010, '供应商等级新增', 'supplier:level:create', '/api/v1/supplier-portal/levels', 'POST', 1210),
 (126011, '供应商等级删除', 'supplier:level:delete', '/api/v1/supplier-portal/levels/{id}', 'DELETE', 1211),
 (126012, '供应商等级查询', 'supplier:level:list', '/api/v1/supplier-portal/levels', 'GET', 1212),
 (126013, '供应商等级编辑', 'supplier:level:update', '/api/v1/supplier-portal/levels/{id}', 'PUT', 1213),
 (126014, '供应商等级查看', 'supplier:level:view', '/api/v1/supplier-portal/levels/score/{score}', 'GET', 1214),
 (126015, '供应商查询', 'supplier:list', '/api/supplier/page', 'POST', 1215),
 (126016, '供应商绩效新增', 'supplier:performance:create', '/api/supplier/performance/evaluate', 'POST', 1216),
 (126017, '供应商绩效详情', 'supplier:performance:detail', '/api/v1/supplier-portal/performances/{id}', 'GET', 1217),
 (126018, '供应商绩效查询', 'supplier:performance:list', '/api/supplier/{supplierId}/performance/history', 'GET', 1218),
 (126019, '供应商绩效查看', 'supplier:performance:view', '/api/supplier/{supplierId}/comprehensive-score', 'GET', 1219),
 (126020, '供应商积分查询', 'supplier:points:list', '/api/v1/supplier-portal/points/{supplierId}/records', 'GET', 1220),
 (126021, '供应商积分编辑', 'supplier:points:update', '/api/v1/supplier-portal/points/{supplierId}/add', 'POST', 1221),
 (126022, '供应商积分查看', 'supplier:points:view', '/api/v1/supplier-portal/points/{supplierId}/total', 'GET', 1222),
 (126023, '供应商报价审批', 'supplier:quotation:approve', '/api/v1/supplier-portal/quotations/{quotationId}/accept', 'POST', 1223),
 (126024, '供应商报价新增', 'supplier:quotation:create', '/api/v1/supplier-portal/inquiries/{inquiryId}/quotation', 'POST', 1224),
 (126025, '供应商编辑', 'supplier:update', '/api/supplier', 'PUT', 1225),
 (126026, '供应商查看', 'supplier:view', '/api/supplier/statistics', 'GET', 1226)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9760000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 126000 AND 126027
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
