-- 配送域权限码**补齐**（E-01 dms 批次，2026-09-21）
--
-- 【与其它 E-04 种子的区别】dms **不是零码模块** —— 库里已有 23 个历史码
--   （`dms:dispatch:*` 8 / `dms:rider:*` 4 / `dms:channel:*` 3 / `dms:vehicle:*` 3 /
--   `dms:sign:*` 2 / `dms:config:update` / `dms:event:retry` / `dms:execution:operate` /
--   `dms:tracking:report`），所以本迁移**只补历史词表确实没有的动作，
--   不重建、不改名、不删除**任何既有码。
--
-- 【为什么必须补】E-01 要给配送域裸控制器补 @SaCheckPermission，而
--   **没有码就补注解 ⇒ 该接口对所有非超管一律 403**（本仓铁律/历史事故）。
--
-- 【口径要点】写在 `tools/gen-module-permission-seed.py` 的 `MODULES['dms']`：
--   本域 211 个端点里有 60+ 处是**状态推进/业务动作**（抢单/竞价/核销/催收/指派/改派/
--   反审核/路线优化…），通用规则的 POST 兜底会把它们全判成 `create` ——
--   "确认收款"变成"新建收款单"。故对这些控制器统一用 `@update`
--   （只改动作词、域与资源仍按路径解析），并保留 `/save$`、`/rule$` 等少数真新建为 create。
--   规则里 `.+` 不匹配空路径 ⇒ 裸 `POST <资源>`（真正的新建）仍是 create。
--
-- 【排除的端点】`POST /api/dms/channel/callback` —— 外部运力平台回调，
--   在 `SaTokenConfig` 白名单里、无用户会话（安全由 HMAC 验签 + 时间戳容差 + nonce 保证），
--   加权限码没有意义。
--
-- 【id 号段】权限码 113000 起（工具槽位 slot=13）、角色关联 9630000 起。
--   实测 113000~113999 与 9630000~9639999 均空闲。
--
-- 【id 号段】权限码 113000 起（本模块独占槽位 slot=13）、角色关联 9630000 起。
--   9xxxx 段已被 V11.42x 系列的短块占满，故另开 100000 段；
--   各模块用不同槽位避免多迁移撞主键（本仓 sys_permission.id 无序列默认值）。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。
-- =============================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (113000, '配送channel详情', 'dms:channel:detail', '/api/dms/channel/{id}', 'GET', 1200),
 (113001, '配送channel查询', 'dms:channel:list', '/api/dms/channel/page', 'GET', 1201),
 (113002, '配送channel查看', 'dms:channel:view', '/api/dms/channel/{id}/orders', 'GET', 1202),
 (113003, '配送config详情', 'dms:config:detail', '/api/dms/config/{key}', 'GET', 1203),
 (113004, '配送config导出', 'dms:config:export', '/api/dms/config/export', 'GET', 1204),
 (113005, '配送config查询', 'dms:config:list', '/api/dms/config/list', 'GET', 1205),
 (113006, '配送config编辑', 'dms:config:update', '/api/dms/config/batch', 'PUT', 1206),
 (113007, '配送config查看', 'dms:config:view', '/api/dms/config/{key}/history', 'GET', 1207),
 (113008, '配送dashboard查看', 'dms:dashboard:view', '/api/dms/dashboard/stats', 'GET', 1208),
 (113009, '配送事件查看', 'dms:event:view', '/api/dms/event/pending', 'GET', 1209),
 (113010, '配送执行查看', 'dms:execution:view', '/api/dms/execution/{taskId}/route', 'GET', 1210),
 (113011, '配送order-pool详情', 'dms:order-pool:detail', '/api/dms/order-pool/{id}', 'GET', 1211),
 (113012, '配送order-pool导出', 'dms:order-pool:export', '/api/dms/order-pool/export', 'GET', 1212),
 (113013, '配送order-pool查询', 'dms:order-pool:list', '/api/dms/order-pool/page', 'GET', 1213),
 (113014, '配送order-pool编辑', 'dms:order-pool:update', '/api/dms/order-pool/publish', 'POST', 1214),
 (113015, '配送order-pool查看', 'dms:order-pool:view', '/api/dms/order-pool/{id}/bid-list', 'GET', 1215),
 (113016, '配送payment详情', 'dms:payment:detail', '/api/dms/payment/{taskId}', 'GET', 1216),
 (113017, '配送payment导出', 'dms:payment:export', '/api/dms/payment/export', 'GET', 1217),
 (113018, '配送payment查询', 'dms:payment:list', '/api/dms/payment/page', 'GET', 1218),
 (113019, '配送payment编辑', 'dms:payment:update', '/api/dms/payment/qrcode', 'POST', 1219),
 (113020, '配送payment查看', 'dms:payment:view', '/api/dms/payment/dict', 'GET', 1220),
 (113021, '配送rider详情', 'dms:rider:detail', '/api/dms/rider/{id}', 'GET', 1221),
 (113022, '配送rider导出', 'dms:rider:export', '/api/dms/rider/export', 'GET', 1222),
 (113023, '配送rider查询', 'dms:rider:list', '/api/dms/rider/page', 'GET', 1223),
 (113024, '配送rider编辑', 'dms:rider:update', '/api/dms/rider/location', 'POST', 1224),
 (113025, '配送rider查看', 'dms:rider:view', '/api/dms/rider/next-code', 'GET', 1225),
 (113026, '配送route-fence新增', 'dms:route-fence:create', '/api/dms/route/fence', 'POST', 1226),
 (113027, '配送route-fence删除', 'dms:route-fence:delete', '/api/dms/route/fence/{id}', 'DELETE', 1227),
 (113028, '配送route-fence详情', 'dms:route-fence:detail', '/api/dms/route/fence/{id}', 'GET', 1228),
 (113029, '配送route-fence查询', 'dms:route-fence:list', '/api/dms/route/fence/page', 'GET', 1229),
 (113030, '配送route-fence状态', 'dms:route-fence:status', '/api/dms/route/fence/{id}/status', 'PUT', 1230),
 (113031, '配送route-fence编辑', 'dms:route-fence:update', '/api/dms/route/fence/{id}', 'PUT', 1231),
 (113032, '配送route-fence查看', 'dms:route-fence:view', '/api/dms/route/fence/next-code', 'GET', 1232),
 (113033, '配送route编辑', 'dms:route:update', '/api/dms/route/plan', 'POST', 1233),
 (113034, '配送route查看', 'dms:route:view', '/api/dms/route/geocode', 'GET', 1234),
 (113035, '配送settlement新增', 'dms:settlement:create', '/api/dms/settlement/rule', 'POST', 1235),
 (113036, '配送settlement删除', 'dms:settlement:delete', '/api/dms/settlement/rule/{id}', 'DELETE', 1236),
 (113037, '配送settlement详情', 'dms:settlement:detail', '/api/dms/settlement/fee/{taskId}', 'GET', 1237),
 (113038, '配送settlement查询', 'dms:settlement:list', '/api/dms/settlement/rule/page', 'GET', 1238),
 (113039, '配送settlement编辑', 'dms:settlement:update', '/api/dms/settlement/rule/{id}', 'PUT', 1239),
 (113040, '配送settlement查看', 'dms:settlement:view', '/api/dms/settlement/rule', 'GET', 1240),
 (113041, '配送sign详情', 'dms:sign:detail', '/api/dms/sign/detail/{id}', 'GET', 1241),
 (113042, '配送sign导出', 'dms:sign:export', '/api/dms/sign/export', 'GET', 1242),
 (113043, '配送sign查询', 'dms:sign:list', '/api/dms/sign/page', 'GET', 1243),
 (113044, '配送sign查看', 'dms:sign:view', '/api/dms/sign/stat', 'GET', 1244),
 (113045, '配送任务新增', 'dms:task:create', '/api/dms/task/save', 'POST', 1245),
 (113046, '配送任务删除', 'dms:task:delete', '/api/dms/task/{id}', 'DELETE', 1246),
 (113047, '配送任务详情', 'dms:task:detail', '/api/dms/task/{id}', 'GET', 1247),
 (113048, '配送任务导出', 'dms:task:export', '/api/dms/task/export', 'GET', 1248),
 (113049, '配送任务查询', 'dms:task:list', '/api/dms/task/next-no', 'GET', 1249),
 (113050, '配送任务状态', 'dms:task:status', '/api/dms/task/{id}/status', 'PUT', 1250),
 (113051, '配送任务编辑', 'dms:task:update', '/api/dms/task/{id}/audit', 'POST', 1251),
 (113052, '配送任务查看', 'dms:task:view', '/api/dms/task/outbound-filter', 'GET', 1252),
 (113053, '配送tracking新增', 'dms:tracking:create', '/api/dms/tracking/clean-expired', 'POST', 1253),
 (113054, '配送tracking详情', 'dms:tracking:detail', '/api/dms/tracking/task-vo/{taskId}', 'GET', 1254),
 (113055, '配送tracking导出', 'dms:tracking:export', '/api/dms/tracking/export', 'GET', 1255),
 (113056, '配送tracking查询', 'dms:tracking:list', '/api/dms/tracking/page', 'GET', 1256),
 (113057, '配送tracking查看', 'dms:tracking:view', '/api/dms/tracking/mileage', 'GET', 1257),
 (113058, '配送vehicle-energy新增', 'dms:vehicle-energy:create', '/api/dms/vehicle/energy', 'POST', 1258),
 (113059, '配送vehicle-energy删除', 'dms:vehicle-energy:delete', '/api/dms/vehicle/energy/{id}', 'DELETE', 1259),
 (113060, '配送vehicle-energy详情', 'dms:vehicle-energy:detail', '/api/dms/vehicle/energy/{id}', 'GET', 1260),
 (113061, '配送vehicle-energy导出', 'dms:vehicle-energy:export', '/api/dms/vehicle/energy/export', 'GET', 1261),
 (113062, '配送vehicle-energy查询', 'dms:vehicle-energy:list', '/api/dms/vehicle/energy/page', 'GET', 1262),
 (113063, '配送vehicle-energy状态', 'dms:vehicle-energy:status', '/api/dms/vehicle/energy/card/{id}/status', 'PUT', 1263),
 (113064, '配送vehicle-energy编辑', 'dms:vehicle-energy:update', '/api/dms/vehicle/energy/{id}', 'PUT', 1264),
 (113065, '配送vehicle-energy查看', 'dms:vehicle-energy:view', '/api/dms/vehicle/energy/stats', 'GET', 1265),
 (113066, '配送vehicle-maintenance新增', 'dms:vehicle-maintenance:create', '/api/dms/vehicle/maintenance', 'POST', 1266),
 (113067, '配送vehicle-maintenance删除', 'dms:vehicle-maintenance:delete', '/api/dms/vehicle/maintenance/{id}', 'DELETE', 1267),
 (113068, '配送vehicle-maintenance详情', 'dms:vehicle-maintenance:detail', '/api/dms/vehicle/maintenance/{id}', 'GET', 1268),
 (113069, '配送vehicle-maintenance导出', 'dms:vehicle-maintenance:export', '/api/dms/vehicle/maintenance/export', 'GET', 1269),
 (113070, '配送vehicle-maintenance查询', 'dms:vehicle-maintenance:list', '/api/dms/vehicle/maintenance/page', 'GET', 1270),
 (113071, '配送vehicle-maintenance编辑', 'dms:vehicle-maintenance:update', '/api/dms/vehicle/maintenance/{id}', 'PUT', 1271),
 (113072, '配送vehicle-maintenance查看', 'dms:vehicle-maintenance:view', '/api/dms/vehicle/maintenance/vendor-options', 'GET', 1272),
 (113073, '配送vehicle详情', 'dms:vehicle:detail', '/api/dms/vehicle/{id}', 'GET', 1273),
 (113074, '配送vehicle导出', 'dms:vehicle:export', '/api/dms/vehicle/export', 'GET', 1274),
 (113075, '配送vehicle查询', 'dms:vehicle:list', '/api/dms/vehicle/page', 'GET', 1275),
 (113076, '配送vehicle查看', 'dms:vehicle:view', '/api/dms/vehicle/next-code', 'GET', 1276),
 (113077, '配送verification详情', 'dms:verification:detail', '/api/dms/verification/binding/{id}', 'GET', 1277),
 (113078, '配送verification导出', 'dms:verification:export', '/api/dms/verification/export', 'GET', 1278),
 (113079, '配送verification查询', 'dms:verification:list', '/api/dms/verification/binding/page', 'GET', 1279),
 (113080, '配送verification编辑', 'dms:verification:update', '/api/dms/verification/bind', 'POST', 1280),
 (113081, '配送verification查看', 'dms:verification:view', '/api/dms/verification/me/rider', 'GET', 1281)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9630000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 113000 AND 113082
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
