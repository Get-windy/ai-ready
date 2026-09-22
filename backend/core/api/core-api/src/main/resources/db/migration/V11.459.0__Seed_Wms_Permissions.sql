-- wms 模块（仓储作业）权限码种子（E-01 wms 批次，2026-09-21）
--
-- 【与「库里已有 11 条 wms:* 码」的关系 —— 别被前缀骗了】
--   `sys_permission` 里确实有 11 条 `wms:` 开头的码，但它们的 `api_path` 全部是
--   `/api/erp/warehouse*`（**erp-stock 模块的仓库/仓库分类控制器**，见 V11.443.0），
--   与 `backend/wms` 的 172 个端点**零交集**（已用 api_path 反查确认）。
--   也不能复用：复用会让「改 ERP 仓库档案」和「改 WMS 仓库作业」共用一个开关。
--   ⇒ 对 `backend/wms` 而言这是**零码模块**，必须现场建码。
--
-- 【为什么必须先建码】E-01 要给 wms 的裸端点补 @SaCheckPermission，而
--   **没有码就补注解 ⇒ 该接口对所有非超管一律 403**（本仓铁律/历史事故）。
--
-- 【粒度】一码 = 一「资源 × 动作」。本批：**162 端点 → 86 码**（172 个端点中
--   10 个被显式排除，见下）。资源 = 收货 receipt / 上架 putaway / 拣货 pick /
--   发货 ship / 移库 move / 盘点 check / 库存 inventory / 库位 location /
--   仓库 warehouse / 借还 borrow / 事件 event / PDA 任务 task。
--
-- 【PDA 口径（本轮定稿，解除了原 `manual_only` 的阻塞）】PDA 端**复用后台同名资源的码**，
--   不另建 `wms:pda:*`。依据是代码实测：PDA 用真实 `sys_user` + BCrypt 登录，
--   且与后台共用同一套 Sa-Token 会话/token/权限链路（`PdaAuthController` 直接用 `StpUtil`；
--   `SaTokenJwtConfig` 整文件被注释、无自定义 StpLogic；token-name 同为 Authorization）。
--   故「设备令牌」在本仓不存在、也不需要 —— 设备身份是**角色策略**问题
--   （给设备角色勾哪些码），不是技术缺失。
--   注意 PDA 的收货控制器类名是 `receive`、后台是 `receipt`，本批统一到 `receipt`
--   （同一业务对象两种拼写不该变成两套码）。
--
-- 【显式排除的 10 个端点 —— 绝对不能加权限注解】
--   · `POST /api/v1/warehouse/auth/login`（在 SaTokenConfig 两处白名单里，未登录必须可达）
--   · `POST /api/v1/warehouse/auth/logout`（加码 ⇒ 没该码的用户无法登出）
--   · `ErpCallbackController`（`/api/erp/wms/**`，4 端点）与 `ErpIntegrationController`
--     （`/api/wms/erp/**`，4 端点）—— **无用户会话**的机器对机器接口，加码必然打不通。
--     ⚠️ 这两个前缀**都不在白名单里**，且代码里**没有任何验签实现**
--     （`ErpCallbackServiceImpl` 四个方法全是 log + return ok），属"应当补服务间鉴权"的
--     独立事项 —— 靠加白名单让它跑通等于把库存变更接口变成匿名可达，需单独拍板。
--
-- 【id 号段】权限码 107000 起（本模块独占槽位 slot=7）、角色关联 9570000 起。
--   实测 107000~107999 与 9570000~9579999 均空闲；各模块用不同槽位避免撞主键
--   （本仓 sys_permission.id 无序列默认值）。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。
-- =============================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (107000, '仓储借还单审批', 'wms:borrow:approve', '/api/wms/borrow/approve', 'POST', 1200),
 (107001, '仓储借还单取消', 'wms:borrow:cancel', '/api/wms/borrow/cancel', 'POST', 1201),
 (107002, '仓储借还单转换', 'wms:borrow:convert', '/api/wms/borrow/convert-purchase', 'POST', 1202),
 (107003, '仓储借还单新增', 'wms:borrow:create', '/api/wms/borrow/create', 'POST', 1203),
 (107004, '仓储借还单删除', 'wms:borrow:delete', '/api/wms/borrow/{id}', 'DELETE', 1204),
 (107005, '仓储借还单详情', 'wms:borrow:detail', '/api/wms/borrow/{id}', 'GET', 1205),
 (107006, '仓储借还单查询', 'wms:borrow:list', '/api/wms/borrow/page', 'GET', 1206),
 (107007, '仓储借还单过账', 'wms:borrow:post', '/api/wms/borrow/post', 'POST', 1207),
 (107008, '仓储借还单归还', 'wms:borrow:return', '/api/wms/borrow/return', 'POST', 1208),
 (107009, '仓储借还单提交', 'wms:borrow:submit', '/api/wms/borrow/submit', 'POST', 1209),
 (107010, '仓储借还单查看', 'wms:borrow:view', '/api/wms/borrow/page-detail', 'GET', 1210),
 (107011, '仓储盘点单审批', 'wms:check:approve', '/api/wms/check/approve', 'POST', 1211),
 (107012, '仓储盘点单新增', 'wms:check:create', '/api/wms/check/save', 'POST', 1212),
 (107013, '仓储盘点单删除', 'wms:check:delete', '/api/wms/check/{id}', 'DELETE', 1213),
 (107014, '仓储盘点单详情', 'wms:check:detail', '/api/wms/check/{id}', 'GET', 1214),
 (107015, '仓储盘点单执行', 'wms:check:execute', '/api/wms/check/start', 'POST', 1215),
 (107016, '仓储盘点单查询', 'wms:check:list', '/api/wms/check/page', 'GET', 1216),
 (107017, '仓储盘点单提交', 'wms:check:submit', '/api/wms/check/submit', 'POST', 1217),
 (107018, '仓储盘点单查看', 'wms:check:view', '/api/v1/warehouse/check', 'GET', 1218),
 (107019, '仓储事件执行', 'wms:event:execute', '/api/wms/event/outbox/process', 'POST', 1219),
 (107020, '仓储事件查询', 'wms:event:list', '/api/wms/event/outbox/page', 'GET', 1220),
 (107021, '仓储事件重试', 'wms:event:retry', '/api/wms/event/outbox/retry', 'POST', 1221),
 (107022, '仓储库存冻结', 'wms:inventory:freeze', '/api/wms/inventory/freeze', 'POST', 1222),
 (107023, '仓储库存查询', 'wms:inventory:list', '/api/wms/inventory/page', 'GET', 1223),
 (107024, '仓储库存解冻', 'wms:inventory:release', '/api/wms/inventory/unfreeze', 'POST', 1224),
 (107025, '仓储库存编辑', 'wms:inventory:update', '/api/wms/inventory/increase', 'POST', 1225),
 (107026, '仓储库存查看', 'wms:inventory:view', '/api/wms/inventory/query', 'GET', 1226),
 (107027, '仓储库位新增', 'wms:location:create', '/api/wms/location/save', 'POST', 1227),
 (107028, '仓储库位删除', 'wms:location:delete', '/api/wms/location/{id}', 'DELETE', 1228),
 (107029, '仓储库位详情', 'wms:location:detail', '/api/wms/location/{id}', 'GET', 1229),
 (107030, '仓储库位导出', 'wms:location:export', '/api/wms/location/export', 'GET', 1230),
 (107031, '仓储库位生成', 'wms:location:generate', '/api/wms/location/generate', 'POST', 1231),
 (107032, '仓储库位查询', 'wms:location:list', '/api/wms/location/page', 'GET', 1232),
 (107033, '仓储库位查看', 'wms:location:view', '/api/wms/location/recommend', 'GET', 1233),
 (107034, '仓储移库单取消', 'wms:move:cancel', '/api/wms/move/cancel', 'POST', 1234),
 (107035, '仓储移库单新增', 'wms:move:create', '/api/wms/move/save', 'POST', 1235),
 (107036, '仓储移库单删除', 'wms:move:delete', '/api/wms/move/batch', 'DELETE', 1236),
 (107037, '仓储移库单详情', 'wms:move:detail', '/api/wms/move/{id}', 'GET', 1237),
 (107038, '仓储移库单执行', 'wms:move:execute', '/api/wms/move/start', 'POST', 1238),
 (107039, '仓储移库单查询', 'wms:move:list', '/api/wms/move/page', 'GET', 1239),
 (107040, '仓储移库单查看', 'wms:move:view', '/api/wms/move/page-detail', 'GET', 1240),
 (107041, '仓储拣货单取消', 'wms:pick:cancel', '/api/wms/pick/task/cancel', 'POST', 1241),
 (107042, '仓储拣货单完成', 'wms:pick:complete', '/api/v1/warehouse/pick/{id}/complete', 'PUT', 1242),
 (107043, '仓储拣货单确认', 'wms:pick:confirm', '/api/v1/warehouse/pick/{id}/verify', 'POST', 1243),
 (107044, '仓储拣货单新增', 'wms:pick:create', '/api/wms/pick/wave/create', 'POST', 1244),
 (107045, '仓储拣货单删除', 'wms:pick:delete', '/api/wms/pick/wave/{id}', 'DELETE', 1245),
 (107046, '仓储拣货单详情', 'wms:pick:detail', '/api/v1/warehouse/pick/{id}', 'GET', 1246),
 (107047, '仓储拣货单执行', 'wms:pick:execute', '/api/wms/pick/task/start', 'POST', 1247),
 (107048, '仓储拣货单查询', 'wms:pick:list', '/api/wms/pick/wave/page', 'GET', 1248),
 (107049, '仓储拣货单编辑', 'wms:pick:update', '/api/v1/warehouse/pick/item/{itemId}', 'PUT', 1249),
 (107050, '仓储拣货单查看', 'wms:pick:view', '/api/v1/warehouse/pick', 'GET', 1250),
 (107051, '仓储上架单取消', 'wms:putaway:cancel', '/api/wms/putaway/cancel', 'POST', 1251),
 (107052, '仓储上架单确认', 'wms:putaway:confirm', '/api/v1/warehouse/putaway/{id}/confirm', 'POST', 1252),
 (107053, '仓储上架单新增', 'wms:putaway:create', '/api/wms/putaway/save', 'POST', 1253),
 (107054, '仓储上架单删除', 'wms:putaway:delete', '/api/wms/putaway/{id}', 'DELETE', 1254),
 (107055, '仓储上架单详情', 'wms:putaway:detail', '/api/v1/warehouse/putaway/{id}', 'GET', 1255),
 (107056, '仓储上架单执行', 'wms:putaway:execute', '/api/wms/putaway/start', 'POST', 1256),
 (107057, '仓储上架单查询', 'wms:putaway:list', '/api/wms/putaway/page', 'GET', 1257),
 (107058, '仓储上架单编辑', 'wms:putaway:update', '/api/v1/warehouse/putaway/{id}/scan', 'POST', 1258),
 (107059, '仓储上架单查看', 'wms:putaway:view', '/api/v1/warehouse/putaway', 'GET', 1259),
 (107060, '仓储收货单取消', 'wms:receipt:cancel', '/api/wms/receipt/cancel', 'POST', 1260),
 (107061, '仓储收货单确认', 'wms:receipt:confirm', '/api/v1/warehouse/receive/{id}/confirm', 'POST', 1261),
 (107062, '仓储收货单新增', 'wms:receipt:create', '/api/wms/receipt/save', 'POST', 1262),
 (107063, '仓储收货单删除', 'wms:receipt:delete', '/api/wms/receipt/{id}', 'DELETE', 1263),
 (107064, '仓储收货单详情', 'wms:receipt:detail', '/api/v1/warehouse/receive/{id}', 'GET', 1264),
 (107065, '仓储收货单执行', 'wms:receipt:execute', '/api/wms/receipt/start', 'POST', 1265),
 (107066, '仓储收货单查询', 'wms:receipt:list', '/api/wms/receipt/page', 'GET', 1266),
 (107067, '仓储收货单编辑', 'wms:receipt:update', '/api/v1/warehouse/receive/{id}/scan', 'POST', 1267),
 (107068, '仓储收货单查看', 'wms:receipt:view', '/api/v1/warehouse/receive', 'GET', 1268),
 (107069, '仓储发货单确认', 'wms:ship:confirm', '/api/v1/warehouse/ship/{id}/confirm', 'PUT', 1269),
 (107070, '仓储发货单新增', 'wms:ship:create', '/api/wms/ship/task/save', 'POST', 1270),
 (107071, '仓储发货单删除', 'wms:ship:delete', '/api/wms/ship/task/{id}', 'DELETE', 1271),
 (107072, '仓储发货单详情', 'wms:ship:detail', '/api/v1/warehouse/ship/{id}', 'GET', 1272),
 (107073, '仓储发货单执行', 'wms:ship:execute', '/api/wms/ship/start', 'POST', 1273),
 (107074, '仓储发货单查询', 'wms:ship:list', '/api/wms/ship/task/page', 'GET', 1274),
 (107075, '仓储发货单编辑', 'wms:ship:update', '/api/v1/warehouse/ship/{id}/scan', 'POST', 1275),
 (107076, '仓储发货单查看', 'wms:ship:view', '/api/v1/warehouse/ship', 'GET', 1276),
 (107077, '仓储任务完成', 'wms:task:complete', '/api/v1/warehouse/tasks/{id}/complete', 'PUT', 1277),
 (107078, '仓储任务详情', 'wms:task:detail', '/api/v1/warehouse/tasks/{id}', 'GET', 1278),
 (107079, '仓储任务执行', 'wms:task:execute', '/api/v1/warehouse/tasks/{id}/start', 'PUT', 1279),
 (107080, '仓储任务查看', 'wms:task:view', '/api/v1/warehouse/tasks', 'GET', 1280),
 (107081, '仓储仓库新增', 'wms:warehouse:create', '/api/wms/warehouse/save', 'POST', 1281),
 (107082, '仓储仓库删除', 'wms:warehouse:delete', '/api/wms/warehouse/{id}', 'DELETE', 1282),
 (107083, '仓储仓库详情', 'wms:warehouse:detail', '/api/wms/warehouse/{id}', 'GET', 1283),
 (107084, '仓储仓库查询', 'wms:warehouse:list', '/api/wms/warehouse/page', 'GET', 1284),
 (107085, '仓储仓库查看', 'wms:warehouse:view', '/api/wms/warehouse/list-all', 'GET', 1285)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9570000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 107000 AND 107086
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
