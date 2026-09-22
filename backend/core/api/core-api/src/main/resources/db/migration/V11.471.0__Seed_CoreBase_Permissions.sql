-- 平台基础域（core-base）权限码种子（E-01 corebase 批次，2026-09-21）
--
-- 【与"零码模块"种子的区别】core-base **不是零码模块**：库里 `log:` 15 条、`system:` 147 条、
--   `tenant-admin:` 72 条、`set:` 11 条等前缀的码早就存在，其中 8 个**正落在本次要补注解的端点上**
--   （`log:oper:{list,detail,stats,export,delete}`、`system:menu:list`、
--    `tenant-admin:permission:check`、`tenant-admin:role:detail`）。
--   所以本迁移**只补库里确实没有的码，不重建、不改名、不删除**任何既有码：
--   VALUES 占满 121000~121055 共 56 个槽位（按码名排序**位置化**分配），
--   被 NOT EXISTS 跳过的 8 个不回填 ⇒ 实际落库 48 个，id 在该区间内不连续。
--
-- 【为什么必须补】E-01 要给 core-base 的裸控制器补 @SaCheckPermission，而
--   **没有码就补注解 ⇒ 该接口对所有非超管一律 403**（本仓铁律/历史事故）。
--
-- 【口径要点】全部写在 `tools/gen-module-permission-seed.py` 的 `MODULES['corebase']`：
--   · **复用优先**：`/api/logs/system/*` 与 `/api/system/log/advanced/*` 都并入既有 `log:oper:*`
--     —— 前者（SystemLogController，表 sys_system_log）与 LogManageController 的
--     `/api/system/log/oper/*` 是**同一功能**（系统日志 分页/详情/统计/导出/清理）的并行实现，
--     后者（AdvancedLogController）直接查同一个 `SysOperLog` 实体；不另立 `log:system:*` 平行命名空间。
--     `log:structured:*`（读 logs/structured/app.jsonl **文件**）与 `log:error-report:*` 在 `log:` 域下
--     新开资源，同族先例是 log:audit / log:login / log:oper（一类日志一个资源）。
--   · 新增的一级前缀只有两个：`quality:`（质检证书/质检单/质量标准/缺陷记录）与
--     `trade:`（外部渠道配置/外部订单/库存同步/接口监控），其余全部接既有码族。
--   · 编码唯一性校验用动作词 check（`system:menu:check` 为本批新建；
--     `tenant-admin:permission:check` 库中**已存在**，直接复用不新建）。
--   · 域归位：`/api/menu` `/api/permission` `/api/role` `/api/user` 的裸端点与它们**同类已注解**的
--     端点共域（system / tenant-admin），不按路径推导成 `menu:` / `role:` / `permission:` / `user:` 一级域。
--
-- 【本批有意排除的端点 —— 共 43 个，不加任何权限码】
--   （判断标准：加码会让"非超管的通用能力"整块 403，或该接口根本没有用户会话）
--   · `/api/auth/**`（8）        login/captcha/check 在 SaTokenConfig **两处**白名单里（未登录必须可达）；
--                               logout 加码后"没有该码的用户无法登出"；userinfo/tenants/login-history/refresh
--                               是当前登录用户的自助接口（pc-admin api/user.ts:93,112 登录后必调）。
--   · `/api/notification/**`（8）顶栏通知铃铛（BasicLayout.vue:532 → useNotification.ts:53,56 无条件轮询
--                               unread-count/list），端点全部按会话 receiverId 过滤 ⇒ "我的通知"自助。
--   · `/api/system/user-config/**`（2）个人页面配置（列显隐/查询条件），配置键含会话 userId。
--   · `/api/sys/region/**`（2）  行政区划三级联动：往来单位等表单共用的公共下拉数据源。
--   · `/api/open/**`（12）      对外开放 API（外部平台订单回调/库存查询/商品同步），调用方无用户会话。
--                               ⚠️ 顺带查实（只报告，不改业务代码）：类注释声称"安全验证-签名校验"，
--                                  但 `signature` 被接收后**未传给** `orderService.receiveCallback(channelCode, data)`。
--   · `/api/sse/notifications`（1）SSE 推送通道（EventSource 以 ?token= 直连），推的是权限缓存失效事件。
--   · `/api/session/current`（1）当前会话自省（该类其余 8 个会话管理端点早已挂 system:session:*）。
--   · `/api/menu/user/{tree,client/*,mega/*}`（3）当前用户菜单树，前端动态路由必调（自省类）。
--   · `/api/role/list`（1）      用户管理页"角色 id→名"映射用的跨页下拉源。
--   · `/api/user/login|logout`（2）+ `/api/user/{id}/password/change`（1）白名单 / 改密自助。
--   · `/api/role-bill-type/validate`（1）校验"**当前用户**"对单据类型的访问级别（自省类）。
--   · `POST /api/error-report`（1）前端**全局错误上报**（main.ts:59 / utils/errorReporter.ts:29）。
--
-- 【id 号段】权限码 121000 起（槽位 21）、角色关联 9710000 起，执行前实测两段均为空；
--   9700000 段被 e2e 夹具占过，故本批从 9710000 续。sort 从 1200 起（沿用各批次的相对序号）。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。
-- =============================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (121000, '平台基础错误报告查询', 'log:error-report:list', '/api/error-report/recent', 'GET', 1200),
 (121001, '平台基础错误报告统计', 'log:error-report:stats', '/api/error-report/statistics', 'GET', 1201),
 (121002, '平台基础oper删除', 'log:oper:delete', '/api/logs/system/clean', 'DELETE', 1202),
 (121003, '平台基础oper详情', 'log:oper:detail', '/api/logs/system/{id}', 'GET', 1203),
 (121004, '平台基础oper导出', 'log:oper:export', '/api/system/log/advanced/export/excel', 'GET', 1204),
 (121005, '平台基础oper查询', 'log:oper:list', '/api/system/log/advanced/query', 'POST', 1205),
 (121006, '平台基础oper统计', 'log:oper:stats', '/api/system/log/advanced/summary', 'GET', 1206),
 (121007, '平台基础结构化日志查询', 'log:structured:list', '/api/system/log/structured/query', 'GET', 1207),
 (121008, '平台基础结构化日志统计', 'log:structured:stats', '/api/system/log/structured/stats', 'GET', 1208),
 (121009, '平台基础质量证书新增', 'quality:certificate:create', '/api/quality/certificate', 'POST', 1209),
 (121010, '平台基础质量证书删除', 'quality:certificate:delete', '/api/quality/certificate/{id}', 'DELETE', 1210),
 (121011, '平台基础质量证书详情', 'quality:certificate:detail', '/api/quality/certificate/{id}', 'GET', 1211),
 (121012, '平台基础质量证书查询', 'quality:certificate:list', '/api/quality/certificate/page', 'GET', 1212),
 (121013, '平台基础质量证书编辑', 'quality:certificate:update', '/api/quality/certificate/{id}', 'PUT', 1213),
 (121014, '平台基础缺陷记录新增', 'quality:defect:create', '/api/quality/defect', 'POST', 1214),
 (121015, '平台基础缺陷记录详情', 'quality:defect:detail', '/api/quality/defect/{id}', 'GET', 1215),
 (121016, '平台基础缺陷记录查询', 'quality:defect:list', '/api/quality/defect/page', 'GET', 1216),
 (121017, '平台基础缺陷记录编辑', 'quality:defect:update', '/api/quality/defect/{id}/handle', 'POST', 1217),
 (121018, '平台基础质检单取消', 'quality:inspection:cancel', '/api/quality/inspection/{id}/cancel', 'POST', 1218),
 (121019, '平台基础质检单完成', 'quality:inspection:complete', '/api/quality/inspection/{id}/complete', 'POST', 1219),
 (121020, '平台基础质检单新增', 'quality:inspection:create', '/api/quality/inspection', 'POST', 1220),
 (121021, '平台基础质检单删除', 'quality:inspection:delete', '/api/quality/inspection/{id}', 'DELETE', 1221),
 (121022, '平台基础质检单详情', 'quality:inspection:detail', '/api/quality/inspection/{id}', 'GET', 1222),
 (121023, '平台基础质检单查询', 'quality:inspection:list', '/api/quality/inspection/page', 'GET', 1223),
 (121024, '平台基础质检单编辑', 'quality:inspection:update', '/api/quality/inspection/{id}', 'PUT', 1224),
 (121025, '平台基础质量标准新增', 'quality:standard:create', '/api/quality/standard', 'POST', 1225),
 (121026, '平台基础质量标准删除', 'quality:standard:delete', '/api/quality/standard/{id}', 'DELETE', 1226),
 (121027, '平台基础质量标准详情', 'quality:standard:detail', '/api/quality/standard/{id}', 'GET', 1227),
 (121028, '平台基础质量标准查询', 'quality:standard:list', '/api/quality/standard/page', 'GET', 1228),
 (121029, '平台基础质量标准编辑', 'quality:standard:update', '/api/quality/standard/{id}', 'PUT', 1229),
 (121030, '平台基础菜单配置查询', 'set:menu-config:list', '/api/set/menu-config/list', 'GET', 1230),
 (121031, '平台基础菜单配置编辑', 'set:menu-config:update', '/api/set/menu-config/visible', 'PUT', 1231),
 (121032, '平台基础menu校验', 'system:menu:check', '/api/menu/check-code', 'GET', 1232),
 (121033, '平台基础menu查询', 'system:menu:list', '/api/menu/client/{clientType}', 'GET', 1233),
 (121034, '平台基础permission校验', 'tenant-admin:permission:check', '/api/permission/check-code', 'GET', 1234),
 (121035, '平台基础role详情', 'tenant-admin:role:detail', '/api/role/{id}', 'GET', 1235),
 (121036, '平台基础接口监控清理', 'trade:api-monitor:clear', '/api/trade/api-monitor/clean-expired', 'POST', 1236),
 (121037, '平台基础接口监控执行', 'trade:api-monitor:execute', '/api/trade/api-monitor/sandbox/invoke', 'POST', 1237),
 (121038, '平台基础接口监控导出', 'trade:api-monitor:export', '/api/trade/api-monitor/calls/export', 'GET', 1238),
 (121039, '平台基础接口监控查询', 'trade:api-monitor:list', '/api/trade/api-monitor/calls/page', 'GET', 1239),
 (121040, '平台基础接口监控重试', 'trade:api-monitor:retry', '/api/trade/api-monitor/sync/{id}/retry', 'POST', 1240),
 (121041, '平台基础接口监控查看', 'trade:api-monitor:view', '/api/trade/api-monitor/stat', 'GET', 1241),
 (121042, '平台基础渠道新增', 'trade:channel:create', '/api/trade/channel', 'POST', 1242),
 (121043, '平台基础渠道删除', 'trade:channel:delete', '/api/trade/channel/{id}', 'DELETE', 1243),
 (121044, '平台基础渠道详情', 'trade:channel:detail', '/api/trade/channel/{id}', 'GET', 1244),
 (121045, '平台基础渠道执行', 'trade:channel:execute', '/api/trade/channel/{id}/initialize', 'POST', 1245),
 (121046, '平台基础渠道查询', 'trade:channel:list', '/api/trade/channel/enabled', 'GET', 1246),
 (121047, '平台基础渠道编辑', 'trade:channel:update', '/api/trade/channel/{id}', 'PUT', 1247),
 (121048, '平台基础渠道查看', 'trade:channel:view', '/api/trade/channel/stat', 'GET', 1248),
 (121049, '平台基础外部订单查询', 'trade:external-order:list', '/api/trade/external-order/page', 'GET', 1249),
 (121050, '平台基础外部订单重试', 'trade:external-order:retry', '/api/trade/external-order/{id}/retry', 'POST', 1250),
 (121051, '平台基础外部订单查看', 'trade:external-order:view', '/api/trade/external-order/stat', 'GET', 1251),
 (121052, '平台基础库存同步导出', 'trade:inventory-sync:export', '/api/trade/inventory-sync/export', 'GET', 1252),
 (121053, '平台基础库存同步查询', 'trade:inventory-sync:list', '/api/trade/inventory-sync/page', 'GET', 1253),
 (121054, '平台基础库存同步重试', 'trade:inventory-sync:retry', '/api/trade/inventory-sync/{id}/retry', 'POST', 1254),
 (121055, '平台基础库存同步查看', 'trade:inventory-sync:view', '/api/trade/inventory-sync/stat', 'GET', 1255)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9710000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 121000 AND 121056
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
