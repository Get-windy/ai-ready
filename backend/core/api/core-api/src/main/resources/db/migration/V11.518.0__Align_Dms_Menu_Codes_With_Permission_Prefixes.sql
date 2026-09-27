-- 2026-09-27 配送模块审计：7 条菜单码对齐其页面实际使用的权限码前缀（方案 B）
-- 报告：DMS_MODULE_AUDIT_20260923.md §4.2 / §10.1
--
-- ── 背景 ────────────────────────────────────────────────────────────────
-- `MenuPermissionDeriver` 的派生规则：「菜单码 C 命中 ⇔ 权限码库里存在 P = C 或 P 以 C + ':' 开头」，
-- 对不上时按过渡口径 **fail-open**（对所有角色可见）。这 7 条的 `menu_code` 与其页面实际调用的
-- 权限码**不同源**，导致菜单可见性无法由权限派生（配送域 14 条里的一批）。
--
-- ── 范围（方案 B，2026-09-27 用户拍板）─────────────────────────────────
-- 只改 **7 条「纯配送自有」**的。另 5 条跨域复用页（物流发货 70155 / 发货查询 70156 /
-- 物流运费对账 70162 / 物流退货收货 70160 / 采购订货收货 70161）**保持 fail-open 不动** ——
-- 它们本质是「销售/采购功能的配送入口」，权限码来自 `sale:` / `purchase:` 域，
-- 用一个配送菜单去对齐目标域前缀语义别扭，由目标模块管更自然。
--
-- ── ⚠️ 必须同步补 route_name ────────────────────────────────────────────
-- 前端路由名 = `route_name || menu_code`（`dynamicRoutes.ts`）。改名后会出现**同码对**：
--   80740 实时跟踪 与 80870 配送跟踪   → 同为 `dms:tracking`
--   80750 配送参数 与 80890 配送配置   → 同为 `dms:config`
-- 两条菜单同名会「后者覆盖前者」⇒ path 不同的那条**永远没有路由**，静默落到 catch-all 404
-- （见记忆 `erp-menu-code-route-collision`，V11.499.0 处理采购冲突时用的就是这个办法）。
-- ⇒ 给**双方**都补上唯一的 `route_name`。
-- 注：同码本身是**正确**的 —— 这两对页面本来就该由同一组权限码（`dms:tracking:*` / `dms:config:*`）控制。

-- ── 一、7 条菜单码对齐页面实际使用的权限码前缀 ──────────────────────────
UPDATE sys_menu SET menu_code = 'dms:task'           WHERE id = 80730 AND deleted = 0;  -- 调度任务   → dms:task:list/save/…
UPDATE sys_menu SET menu_code = 'dms:tracking'       WHERE id = 80740 AND deleted = 0;  -- 实时跟踪   → dms:tracking:list/view/…
UPDATE sys_menu SET menu_code = 'dms:config'         WHERE id = 80750 AND deleted = 0;  -- 配送参数   → dms:config:list/detail/update
UPDATE sys_menu SET menu_code = 'dms:route'          WHERE id = 80830 AND deleted = 0;  -- 路线规划   → dms:route:view/update
UPDATE sys_menu SET menu_code = 'delivery:route'     WHERE id = 80700 AND deleted = 0;  -- 配送路线单 → delivery:route:list/create/…
UPDATE sys_menu SET menu_code = 'dms:vehicle-energy' WHERE id = 80840 AND deleted = 0;  -- 用车管理   → 双前缀(vehicle-energy/verification)，选主
UPDATE sys_menu SET menu_code = 'md:route-master'    WHERE id = 70530 AND deleted = 0;  -- 线路       → md:route-master:list/…

-- ── 二、补唯一 route_name ───────────────────────────────────────────────
-- （7 条改名的 + 2 条「同码伙伴」80870 / 80890；全部仅在为空时写入，幂等）
UPDATE sys_menu SET route_name = 'DmsDispatchTask'    WHERE id = 80730 AND deleted = 0 AND (route_name IS NULL OR route_name = '');
UPDATE sys_menu SET route_name = 'DmsRealtimeTracking' WHERE id = 80740 AND deleted = 0 AND (route_name IS NULL OR route_name = '');
UPDATE sys_menu SET route_name = 'DmsConfigParams'    WHERE id = 80750 AND deleted = 0 AND (route_name IS NULL OR route_name = '');
UPDATE sys_menu SET route_name = 'DmsRoutePlan'       WHERE id = 80830 AND deleted = 0 AND (route_name IS NULL OR route_name = '');
UPDATE sys_menu SET route_name = 'DmsRouteList'       WHERE id = 80700 AND deleted = 0 AND (route_name IS NULL OR route_name = '');
UPDATE sys_menu SET route_name = 'DmsVehicleUsage'    WHERE id = 80840 AND deleted = 0 AND (route_name IS NULL OR route_name = '');
UPDATE sys_menu SET route_name = 'MdRouteMaster'      WHERE id = 70530 AND deleted = 0 AND (route_name IS NULL OR route_name = '');

-- 同码伙伴（menu_code 未变，但必须与改名方区分路由名）
UPDATE sys_menu SET route_name = 'DmsTracking'        WHERE id = 80870 AND deleted = 0 AND (route_name IS NULL OR route_name = '');
UPDATE sys_menu SET route_name = 'DmsConfig'          WHERE id = 80890 AND deleted = 0 AND (route_name IS NULL OR route_name = '');

-- ── 三、自检（人工执行时核对）────────────────────────────────────────────
-- 1) 7 条应各自命中权限码前缀（修复前这 7 条均为 0 命中）：
--   SELECT m.id, m.menu_code,
--          EXISTS(SELECT 1 FROM sys_permission p WHERE p.permission_code = m.menu_code
--                  OR p.permission_code LIKE m.menu_code || ':%') AS derived
--     FROM sys_menu m WHERE m.id IN (80730,80740,80750,80830,80700,80840,70530);
-- 2) 配送域内不应再有同 menu_code 且 route_name 皆空的对：
--   SELECT menu_code, count(*) FROM sys_menu WHERE deleted = 0 AND menu_code LIKE 'dms:%'
--    GROUP BY menu_code, route_name HAVING count(*) > 1;
