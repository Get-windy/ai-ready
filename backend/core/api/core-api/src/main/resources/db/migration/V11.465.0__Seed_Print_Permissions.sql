-- 打印域权限码**补齐**（E-01 erpprinting 批次，2026-09-21）
--
-- 【与其它 E-04 种子的区别】打印域**不是零码模块** —— 库里原有 1 条历史码 `print:draft`
--   （无 api_path、不对应任何现存端点，本批不动它）。本迁移**只补库里确实没有的码，
--   不重建、不改名、不删除**任何既有码：82 个待补端点共 62 个码，其中 **1 个命中已存在的码**
--   （`set:print-config:view` —— PrintConfigController 的 `GET /behavior`，该控制器另 3 个端点
--   早已注解同一个 `set:print-config:` 码族，故这里接过去而**不新造**同义码），
--   SQL 的 NOT EXISTS 会跳过它 ⇒ 实际新增 **61 个**（VALUES 占 116000~116061 共 62 个槽位）。
--
-- 【覆盖范围】模块根目录 `cn/aiedge`（不只是 `printing/controller`）：
--   · v1 打印：`/api/v1/print/{logs,tasks,templates,printers}`（含打印机分组）；
--   · v2 打印：`/api/v2/print/{chains,clients,tasks,templates,screenshots,format,messages}`；
--   · 电子签收 / 配送评价：`/api/signature`、`/api/rating`（同属 erp-printing 模块的
--     `cn.aiedge.erp.signature` 包，表 erp_signature_record / erp_delivery_rating）。
--
-- 【为什么必须补】E-01 要给打印域的裸控制器补 @SaCheckPermission，而
--   **没有码就补注解 ⇒ 该接口对所有非超管一律 403**（本仓铁律/历史事故）。
--
-- 【口径要点】全部写在 `tools/gen-module-permission-seed.py` 的 `MODULES['erpprinting']`：
--   · 域按**类级路径**推导 = `print`（与库中既有码 `print:draft` 同前缀）。签收/评价两个
--     控制器的类级路径**没有资源段** ⇒ 用 base_overrides 显式指定 `signature:record`/
--     `signature:rating`，否则会退化成 `signature:photo:*`、`rating:list` 这种碎片码。
--   · v1 与 v2 是同一业务对象的**两代实现** ⇒ 共用 `print:template:*` / `print:task:*`
--     （同 `/api/import` 与 `/api/import/v2` 共用 `system:import:*` 的先例）。
--   · 状态推进类 POST/PUT 不落 create 兜底：批量打印→print、按链路执行→execute、
--     确认截图→confirm、取消/重试→cancel/retry、发布→publish、复制→copy、校验公式→check；
--     链路/客户端/打印机"改状态"沿用 RULES 中既有动作词 `status`（库中已有 7 条）。
--   · 打印机分组是独立对象（组本身有增删改查）⇒ 单列 `print:printer-group:*`，
--     不与 `print:printer:*` 合并（否则"删打印机"与"删分组"共用一个码）。
--
-- 【id 号段】权限码 116000 起（工具槽位 slot=16）、角色关联 9660000 起。
--   实测 116000~116999 = 0 行、9660000~9669999 = 0 行，均空闲。
--
-- 【本批有意排除的端点 —— 共 9 个，不加任何权限码】
--   · `/api/v2/print/client/**`（4 个）—— ClientApiController：类注释写明「由 Windows 打印
--     客户端直接调用，使用 auth_key 认证」，方法体逐调用 `clientService.authenticate(
--     clientId, authKey)`，**没有用户会话**。加用户权限码 ⇒ 客户端永远打不通
--     （与 wms 的 ErpCallbackController 同类，正确修法是服务间鉴权，不是塞权限码）。
--   · `/api/v2/print/client/auth/**`（5 个）—— ClientAuthController：客户端登录/注册/自省/登出。
--     `register`/`me`/`check`/`logout` 是客户端会话自助（任何登录用户都该能用，同 `/api/profile`
--     口径）；`login` 是登录端点，未登录必须可达。
--     ⚠️ 既有缺陷（只报告，本批不修）：`POST /api/v2/print/client/auth/login` **不在**
--     SaTokenConfig 的两处 excludePathPatterns 里（backend/core/base/core-base/src/main/java/
--     cn/aiedge/base/config/SaTokenConfig.java:33-80 与 :86-133），而 Electron 客户端
--     （frontend/apps/print-client/src/main/services/auth.ts）正是调它登录 ⇒ 该客户端今天
--     实际登不进来（未登录被 StpUtil.checkLogin 拦下）。补权限码前需先解决这一条。
--
-- 【遗留 / 拿不准】
--   · 前端 pc-admin 打印控制台用的是**另一套字符串** `printing:*`（views/printing/**，
--     路由已注册见 router/dynamicRoutes.ts:116-120，共 17 条，如 `printing:template:publish`、
--     `printing:task:canceltask`、`printing:client:resetkey`），与库中既有前缀 `print:` 不同名，
--     且其动作词是 `deleteconfirm`/`canceltask` 这类**本仓未使用的写法**。本批按库中既有前缀
--     建码（不新造同义动作词）⇒ 那些按钮需要前端做一次字符串改名才能与权限矩阵对上；
--     属前端接线，不在本批范围。
--   · `print:draft` 是孤儿码（无 api_path、无消费方），建议随僵尸码批次处理，本批不动。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。
-- =============================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (116000, '打印链路新增', 'print:chain:create', '/api/v2/print/chains', 'POST', 1200),
 (116001, '打印链路删除', 'print:chain:delete', '/api/v2/print/chains/{id}', 'DELETE', 1201),
 (116002, '打印链路详情', 'print:chain:detail', '/api/v2/print/chains/{id}', 'GET', 1202),
 (116003, '打印链路查询', 'print:chain:list', '/api/v2/print/chains', 'GET', 1203),
 (116004, '打印链路状态', 'print:chain:status', '/api/v2/print/chains/{id}/status', 'PUT', 1204),
 (116005, '打印链路编辑', 'print:chain:update', '/api/v2/print/chains/{id}', 'PUT', 1205),
 (116006, '打印客户端新增', 'print:client:create', '/api/v2/print/clients/register', 'POST', 1206),
 (116007, '打印客户端删除', 'print:client:delete', '/api/v2/print/clients/{id}', 'DELETE', 1207),
 (116008, '打印客户端详情', 'print:client:detail', '/api/v2/print/clients/{id}', 'GET', 1208),
 (116009, '打印客户端查询', 'print:client:list', '/api/v2/print/clients', 'GET', 1209),
 (116010, '打印客户端状态', 'print:client:status', '/api/v2/print/clients/{id}/status', 'PUT', 1210),
 (116011, '打印客户端编辑', 'print:client:update', '/api/v2/print/clients/{id}', 'PUT', 1211),
 (116012, '打印格式化校验', 'print:format:check', '/api/v2/print/format/validate-formula', 'POST', 1212),
 (116013, '打印格式化查看', 'print:format:view', '/api/v2/print/format/render', 'POST', 1213),
 (116014, '打印日志导出', 'print:log:export', '/api/v1/print/logs/export', 'GET', 1214),
 (116015, '打印日志查询', 'print:log:list', '/api/v1/print/logs', 'GET', 1215),
 (116016, '打印日志查看', 'print:log:view', '/api/v1/print/logs/statistics', 'GET', 1216),
 (116017, '打印消息发送', 'print:message:send', '/api/v2/print/messages/send', 'POST', 1217),
 (116018, '打印设备分组分配', 'print:printer-group:assign', '/api/v1/print/printers/groups/{groupId}/assign', 'POST', 1218),
 (116019, '打印设备分组新增', 'print:printer-group:create', '/api/v1/print/printers/groups', 'POST', 1219),
 (116020, '打印设备分组删除', 'print:printer-group:delete', '/api/v1/print/printers/groups/{id}', 'DELETE', 1220),
 (116021, '打印设备分组详情', 'print:printer-group:detail', '/api/v1/print/printers/groups/{groupId}/printers', 'GET', 1221),
 (116022, '打印设备分组查询', 'print:printer-group:list', '/api/v1/print/printers/groups', 'GET', 1222),
 (116023, '打印设备分组编辑', 'print:printer-group:update', '/api/v1/print/printers/groups/{id}', 'PUT', 1223),
 (116024, '打印设备新增', 'print:printer:create', '/api/v1/print/printers', 'POST', 1224),
 (116025, '打印设备删除', 'print:printer:delete', '/api/v1/print/printers/{id}', 'DELETE', 1225),
 (116026, '打印设备详情', 'print:printer:detail', '/api/v1/print/printers/{id}', 'GET', 1226),
 (116027, '打印设备查询', 'print:printer:list', '/api/v1/print/printers', 'GET', 1227),
 (116028, '打印设备状态', 'print:printer:status', '/api/v1/print/printers/{id}/status', 'PUT', 1228),
 (116029, '打印设备编辑', 'print:printer:update', '/api/v1/print/printers/{id}', 'PUT', 1229),
 (116030, '打印设备查看', 'print:printer:view', '/api/v1/print/printers/{id}/status', 'GET', 1230),
 (116031, '打印截图完成', 'print:screenshot:complete', '/api/v2/print/screenshots/{id}/complete', 'PUT', 1231),
 (116032, '打印截图新增', 'print:screenshot:create', '/api/v2/print/screenshots', 'POST', 1232),
 (116033, '打印截图详情', 'print:screenshot:detail', '/api/v2/print/screenshots/{id}', 'GET', 1233),
 (116034, '打印截图查询', 'print:screenshot:list', '/api/v2/print/screenshots', 'GET', 1234),
 (116035, '打印截图重试', 'print:screenshot:retry', '/api/v2/print/screenshots/{id}/retry', 'POST', 1235),
 (116036, '打印任务取消', 'print:task:cancel', '/api/v1/print/tasks/{id}/cancel', 'POST', 1236),
 (116037, '打印任务确认', 'print:task:confirm', '/api/v2/print/tasks/{id}/confirm-screenshot', 'POST', 1237),
 (116038, '打印任务新增', 'print:task:create', '/api/v1/print/tasks', 'POST', 1238),
 (116039, '打印任务详情', 'print:task:detail', '/api/v1/print/tasks/{id}', 'GET', 1239),
 (116040, '打印任务执行', 'print:task:execute', '/api/v2/print/tasks/by-chain', 'POST', 1240),
 (116041, '打印任务查询', 'print:task:list', '/api/v1/print/tasks', 'GET', 1241),
 (116042, '打印任务打印', 'print:task:print', '/api/v1/print/tasks/batch', 'POST', 1242),
 (116043, '打印任务重试', 'print:task:retry', '/api/v1/print/tasks/{id}/retry', 'POST', 1243),
 (116044, '打印任务查看', 'print:task:view', '/api/v1/print/tasks/queue/length', 'GET', 1244),
 (116045, '打印模板复制', 'print:template:copy', '/api/v1/print/templates/{id}/copy', 'POST', 1245),
 (116046, '打印模板新增', 'print:template:create', '/api/v1/print/templates', 'POST', 1246),
 (116047, '打印模板删除', 'print:template:delete', '/api/v1/print/templates/{id}', 'DELETE', 1247),
 (116048, '打印模板详情', 'print:template:detail', '/api/v1/print/templates/{id}', 'GET', 1248),
 (116049, '打印模板查询', 'print:template:list', '/api/v1/print/templates', 'GET', 1249),
 (116050, '打印模板发布', 'print:template:publish', '/api/v2/print/templates/{id}/publish', 'PUT', 1250),
 (116051, '打印模板编辑', 'print:template:update', '/api/v1/print/templates/{id}', 'PUT', 1251),
 (116052, '打印模板查看', 'print:template:view', '/api/v1/print/templates/{id}/preview', 'POST', 1252),
 (116053, '打印print-config查看', 'set:print-config:view', '/api/set/print-config/behavior', 'GET', 1253),
 (116054, '打印配送评价新增', 'signature:rating:create', '/api/rating', 'POST', 1254),
 (116055, '打印配送评价详情', 'signature:rating:detail', '/api/rating/{id}', 'GET', 1255),
 (116056, '打印配送评价查询', 'signature:rating:list', '/api/rating/list', 'GET', 1256),
 (116057, '打印配送评价查看', 'signature:rating:view', '/api/rating/stats/{deliveryPersonId}', 'GET', 1257),
 (116058, '打印签收记录校验', 'signature:record:check', '/api/signature/{id}/verify', 'GET', 1258),
 (116059, '打印签收记录新增', 'signature:record:create', '/api/signature/photo', 'POST', 1259),
 (116060, '打印签收记录详情', 'signature:record:detail', '/api/signature/{id}', 'GET', 1260),
 (116061, '打印签收记录查询', 'signature:record:list', '/api/signature/list', 'GET', 1261)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9660000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 116000 AND 116062
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
