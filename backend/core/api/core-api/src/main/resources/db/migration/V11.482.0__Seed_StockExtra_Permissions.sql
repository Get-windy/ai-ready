-- 库存杂项：期初库存 + 进销存分析（E-01 stockextra 批次，2026-09-21）
--
-- 【为什么单独一批】这两个控制器在**子包**里（`erp-stock/src/main/java/cn/aiedge/erp/stock/
--   controller/initial` 与 `.../stock/analytics`），而 `MODULES['stock']` 的 dirs 只给到
--   `.../erp/stock/controller`（生成器早期实现只扫一层目录；后来改成递归但那批已经跑完）
--   ⇒ 它们从未被任何批次覆盖。
--
-- 【域口径：两条路径推导出的域都已存在，不新建域、不新增映射行】
--   · `/api/set/initial-stock` → `set:initial-stock:*`，与库中既有的 `set:initial-finance:*`
--     （期初财务，同一"期初"功能族）逐字对称；前端菜单码也正是 `set:initial-stock`
--     （sys_menu 70550，路径 set/initial-stock）；
--   · `/api/erp/stock/analytics` → `stock:analytics:list`，与 `stock:take` / `stock:check` 同域。
--     ⚠️ 该页面的菜单码是 `ana:inventory-analysis`（分析中心），但**码必须落 `stock:`** ——
--     `ana:` / `analytics:` 前缀会撞 `verify-module-mapping.cjs` 的
--     「analytics 映射数必须为 0」硬断言（见该脚本 ⑤ 节）。
--
-- 【只补不删】本文件 6 端点 → 6 码，**全部新增**；6 条动作全部由通用规则判对（无需 code_rules）：
--   `/page`→list、`/save`→create（期初的"保存"就是录入，同 `set:initial-finance:create` 口径）、
--   `/update`→update、`DELETE /{id}`→delete、`/export`→export。
--
-- 【id 号段】权限码 **130000 起**（slot=30）、角色关联 **9800000 起** —— 执行前实测两段 count(*) = 0。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。
-- =============================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (130000, '库存期初新增', 'set:initial-stock:create', '/api/set/initial-stock/save', 'POST', 1200),
 (130001, '库存期初删除', 'set:initial-stock:delete', '/api/set/initial-stock/{id}', 'DELETE', 1201),
 (130002, '库存期初导出', 'set:initial-stock:export', '/api/set/initial-stock/export', 'GET', 1202),
 (130003, '库存期初查询', 'set:initial-stock:list', '/api/set/initial-stock/page', 'GET', 1203),
 (130004, '库存期初编辑', 'set:initial-stock:update', '/api/set/initial-stock/update', 'PUT', 1204),
 (130005, '库存分析查询', 'stock:analytics:list', '/api/erp/stock/analytics/page', 'GET', 1205)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9800000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 130000 AND 130006
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
