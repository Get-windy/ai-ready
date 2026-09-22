-- 配送路线（deliveryroute）权限码种子（E-01 批次，2026-09-21）
--
-- 【认证模型已核实：司机端持有员工会话，不是"设备身份"】
--   · 司机端 App（`frontend/apps/driver-delivery`）的登录端点是 `src/api/index.ts:68` 的
--     `POST /auth/login` —— 即**主站员工登录**（sys_user + BCrypt），token 由
--     `src/utils/request.ts:32` 以 `Authorization: Bearer` 发出，就是 Sa-Token 的员工会话；
--   · PC 端「配送 → 配送路线 → 配送路线单」（菜单 80700 / `dms:route-list`）与司机端
--     **共用同一批端点**（`api/dms/route.ts` 与 `api/delivery-route.ts`），都是登录用户；
--   · 库中 `erp_delivery_route.delivery_person_id` 取值与 `sys_user.id` 同型（1/3/64/269…），不是设备号。
--   ⇒ 与 wms 的 PDA 同口径（真实 sys_user + 共用会话）⇒ **补码，不排除**。
--
-- 【域口径 + 同名消歧】两个控制器推导出的资源名都是 `route`：
--   · RouteController（执行单，`/api/delivery/route`）→ `delivery:route:*`（**新一级域** `delivery:`，
--     模块归属 dms，见 V11.483.0）。**不复用**库里既有的 `dms:route:{update,view}` ——
--     那两条挂在 `/api/dms/route/plan|geocode`（路线规划/地理编码），与"配送路线单"不是一回事；
--   · RouteMasterController（线路档案，`/api/erp/md/route`）→ `md:route-master:*`
--     （用 `resource_overrides` 改名：否则两条码的资源名同为 `route`，而权限名只能由
--     「模块名+资源名+动作」拼 ⇒ 会出现两行完全同名的条目）。
--
-- 【只补不删】本文件 38 端点 → 23 码，**全部新增**（库中 `delivery%` 与 `md:route%` 均为零条）。
--
-- 【有意排除的端点 —— 1 个】`GET /api/erp/md/route/options`（启用线路下拉）：
--   被 pc-admin **8 个页面**当「配送线路」下拉源引用（dispatch-order、ship-query、dispatch-task、
--   order-pool、dms/route、route-list、settlement…，均调 `mdRouteApi.options()`），
--   属"跨模块基础数据源，不是可勾选功能"（同 `/api/erp/basic`、`/department/list` 口径）：
--   加码会让这些页面的下拉在非超管手里变空。`next-code` / `import-template` 仍补码。
--
-- 【id 号段】权限码 **127000 起**（slot=27）、角色关联 **9770000 起** —— 执行前实测两段 count(*) = 0。
--
-- 【遗留（交付后必须做，否则司机端会 403）】本批给 7 个司机端端点（`/active/{id}`、`/{id}`、
--   `/{id}/start`、`/{id}/complete`、`/{id}/point/{pid}/sign`、`/{id}/plan-order`、`/{id}/eta`）
--   也补了码 —— 它们走的是**员工会话**，但"司机"角色必须被授予
--   `delivery:route:{list,detail,execute,complete,update,sign}` 才能继续用司机端。
--   这是 E-01 的既定代价（前几批的 PDA/采购/销售同理），不在本批范围内自动完成授权。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。
-- =============================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (127000, '配送需求查询', 'delivery:demand:list', '/api/delivery/route/demands', 'GET', 1200),
 (127001, '配送ETA通知执行', 'delivery:eta-notify:execute', '/api/delivery/route/eta-notify/dispatch', 'POST', 1201),
 (127002, '配送ETA通知查询', 'delivery:eta-notify:list', '/api/delivery/route/eta-notify/page', 'GET', 1202),
 (127003, '配送ETA通知编辑', 'delivery:eta-notify:update', '/api/delivery/route/eta-notify/status', 'POST', 1203),
 (127004, '配送路线取消', 'delivery:route:cancel', '/api/delivery/route/{routeId}/cancel', 'POST', 1204),
 (127005, '配送路线完成', 'delivery:route:complete', '/api/delivery/route/{routeId}/complete', 'POST', 1205),
 (127006, '配送路线新增', 'delivery:route:create', '/api/delivery/route', 'POST', 1206),
 (127007, '配送路线详情', 'delivery:route:detail', '/api/delivery/route/{routeId}', 'GET', 1207),
 (127008, '配送路线执行', 'delivery:route:execute', '/api/delivery/route/{routeId}/start', 'POST', 1208),
 (127009, '配送路线导出', 'delivery:route:export', '/api/delivery/route/export', 'GET', 1209),
 (127010, '配送路线查询', 'delivery:route:list', '/api/delivery/route/page', 'GET', 1210),
 (127011, '配送路线签署', 'delivery:route:sign', '/api/delivery/route/{routeId}/point/{pointId}/sign', 'POST', 1211),
 (127012, '配送路线编辑', 'delivery:route:update', '/api/delivery/route/{routeId}', 'PUT', 1212),
 (127013, '配送路线查看', 'delivery:route:view', '/api/delivery/route/auto-collect/preview', 'POST', 1213),
 (127014, '配送线路新增', 'md:route-master:create', '/api/erp/md/route', 'POST', 1214),
 (127015, '配送线路删除', 'md:route-master:delete', '/api/erp/md/route/{id}', 'DELETE', 1215),
 (127016, '配送线路详情', 'md:route-master:detail', '/api/erp/md/route/{id}', 'GET', 1216),
 (127017, '配送线路导出', 'md:route-master:export', '/api/erp/md/route/export', 'GET', 1217),
 (127018, '配送线路导入', 'md:route-master:import', '/api/erp/md/route/import-excel', 'POST', 1218),
 (127019, '配送线路查询', 'md:route-master:list', '/api/erp/md/route/page', 'GET', 1219),
 (127020, '配送线路状态', 'md:route-master:status', '/api/erp/md/route/{id}/status', 'PUT', 1220),
 (127021, '配送线路编辑', 'md:route-master:update', '/api/erp/md/route/{id}', 'PUT', 1221),
 (127022, '配送线路查看', 'md:route-master:view', '/api/erp/md/route/next-code', 'GET', 1222)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9770000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 127000 AND 127023
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
