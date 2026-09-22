-- 平台与系统域（core-api）权限码**补齐**（E-01 coreapi 批次，2026-09-21）
--
-- 【与其它 E-04 种子的区别】coreapi **不是零码模块** —— 库里 `system:` 56 / `tenant-admin:`
--   72 / `workflow:` 14 / `log:` 15 / `datasource:` 16 / `platform:` 19 / `doc:` 5 等前缀的码
--   早就存在，其中相当一部分**正挂在本次要补注解的端点上**。所以本迁移
--   **只补库里确实没有的码，不重建、不改名、不删除**任何既有码：
--   302 个待补端点共对应 118 个码，其中 **21 个命中已存在的码**（SQL 的 NOT EXISTS 会跳过，
--   例如 `tenant-admin:department:*`、`tenant-admin:position:*`、`system:dataimport:*`、
--   `workflow:task:*`、`system:config:list`、`system:tenant:update`），
--   真正新增 **97 个**（本文件 VALUES 占满 111000~111117 共 118 个槽位，id 按码名排序**位置化**分配，
--   被 NOT EXISTS 跳过的 21 个不回填，故实际落库的 97 个 id 在该区间内不连续）。
--
-- 【为什么必须补】E-01 要给 core-api 的裸控制器补 @SaCheckPermission，而
--   **没有码就补注解 ⇒ 该接口对所有非超管一律 403**（本仓铁律/历史事故）。
--
-- 【口径要点】全部写在 `tools/gen-module-permission-seed.py` 的 `MODULES['coreapi']`：
--   · 域：`system:` 的码只给系统租户（已拍板），故**租户级管理功能**（部门/岗位/岗位分类）
--     一律 `tenant-admin:`（沿用 V11.456.0 改名后的历史码，动作词是 list/query/create/edit/delete，
--     不是 detail/update）；单据查询中心沿用既有 `doc:` 域；其余平台/运维功能归 `system:`。
--   · 资源名：class-level 路径没有资源段的控制器用 `base_overrides` 逐个指定
--     （`/api/assistant` `/api/cache` `/api/config` `/api/department` `/api/import` `/api/export`
--     `/api/file` `/api/monitor/**` `/api/mq` `/api/search` `/api/storage` …），
--     否则生成器会拿方法路径第一段当资源名，产出 `system:session:*`、`system:map:*`、
--     甚至 `file:**:view` 这种坏码。
--   · 动作词修正：POST 兜底是 create，下列语义会被它判错，已在 `code_rules` 里逐条改为
--     导出→export、导入/预览/校验、同步/测试→execute、重试→retry、启用停用/确认解决→update、
--     取推荐/搜索/报表预览/分析→list|view 等。
--   · ⚠️ `@RequiresPermission`（**另一套**注解，由 core-base 的 `PermissionAspect` 真实执行）
--     早就写在部门/岗位/岗位分类三个控制器的方法上。生成器的判据只认 `@RequirePermission`
--     （少一个 s），会把这些端点误判成"裸端点" ⇒ 本批给它们补的码**逐条对齐了既有注解里的码**
--     （岗位分类复用 `tenant-admin:position:*`，**不另立** `position-category`），
--     否则两套注解叠加会变成"两个码都必须满足"，把有 position 权限的角色挡在门外。
--     注：这是生成器判据的缺口，按纪律只报告、不修改公共逻辑。
--   · **不复用同义新码**：能对上历史码的一律用 `code_rules` 接过去（21 个，见下）；
--     尤其 `/api/v1/sync-config` 的字段映射与 `/api/v1/sync-history` 全部并入既有
--     `system:dataimport:*`，不另造 `system:sync-*` 平行命名空间。
--
-- 【本批有意排除的端点 —— 共 34 个，不加任何权限码】
--   （判断标准：加码会让**非超管的通用能力整块 403**，或该接口根本没有用户会话）
--   · `/api/dashboard/**`（4）    工作台首页：pc-admin 路由默认落地页（只 requiresAuth），
--                                加码 ⇒ 所有非超管一登录首页就报错。
--   · `/api/profile/**`（6）      个人中心自助（改资料/密码/头像/偏好），任何登录用户必须可用。
--   · `/api/dict`（2）+ `/api/dict/item/code/{dictCode}`（1）
--                                全站下拉字典数据源（`api/options.ts` 的 getDict/getDicts、
--                                `getByDictCode` 被用户/角色/岗位/数据导入四个管理页引用）。
--   · `/api/file/**`（2）         ① `GET /file/view/**` 在 SaTokenConfig 两处白名单里
--                                （<img> 直引，未登录必须可达）；② `POST /file/upload` 是平台
--                                通用上传契约，12+ 处表单/附件/企业 LOGO 复用。
--   · `/api/admin/fix/**`（2）    已用 `@SaCheckRole("admin")` 做角色门禁，加权限码是二次门禁
--                                （⚠️ 库中并无 role_code='admin' 的角色，该注解当前会挡住所有人，
--                                属既有缺陷，本批只报告不修）。
--   · `/api/erp/basic/**`（2）    跨模块基础数据选择器（业务员/仓库下拉）。
--   · `/api/department/list|tree|options`（3）
--                                全站部门下拉源（`/department/options` 的接口说明自己写着
--                                "供全站部门下拉使用"），20+ 业务表单引用。
--   · `/api/user-permission/current/**`（4）
--                                当前用户**自身**权限/角色自省，前端登录后必调它构建菜单；
--                                套上 `tenant-admin:permission:view` 会让所有非超管拿不到菜单。
--   · `/api/feedback/submit*`、`/my/*`（5）  反馈的自助侧（提交、我的反馈）。
--   · `/api/tenant/current`（1）  当前租户信息（顶部栏/启动时读取）。
--   · `/api/integration/webhook/{configId}`（1）
--                                外部系统**入站** webhook：只记日志、无验签、无会话依赖，
--                                正确做法是补服务间鉴权而不是权限码。
--   · `/api/tenant-registration/register`（1）
--                                控制器注释写明「公开接口，无需登录」，但白名单里**没有**它，
--                                现状是"登录后可达"，与设计不符（既有缺陷，只报告）；加码会让它
--                                彻底不可用。
--
-- 【id 号段】权限码 111000 起（工具槽位 slot=11）、角色关联 9610000 起。
--   实测 111000~111999 与 9610000~9619999 均空闲（迁移前置校验）。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。
-- =============================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (111000, '平台与系统docquery查询', 'doc:docquery:list', '/api/docquery/business-history/page', 'GET', 1200),
 (111001, '平台与系统assistant新增', 'system:assistant:create', '/api/assistant/session/create', 'POST', 1201),
 (111002, '平台与系统assistant删除', 'system:assistant:delete', '/api/assistant/session', 'DELETE', 1202),
 (111003, '平台与系统assistant执行', 'system:assistant:execute', '/api/assistant/chat', 'POST', 1203),
 (111004, '平台与系统assistant查看', 'system:assistant:view', '/api/assistant/history', 'GET', 1204),
 (111005, '平台与系统cache删除', 'system:cache:delete', '/api/cache/region/{name}', 'DELETE', 1205),
 (111006, '平台与系统cache查看', 'system:cache:view', '/api/cache/status', 'GET', 1206),
 (111007, '平台与系统config查询', 'system:config:list', '/api/config/map', 'GET', 1207),
 (111008, '平台与系统dataexport导出', 'system:dataexport:export', '/api/export/excel/export', 'POST', 1208),
 (111009, '平台与系统dataexport导入', 'system:dataexport:import', '/api/export/excel/import', 'POST', 1209),
 (111010, '平台与系统dataexport查看', 'system:dataexport:view', '/api/export/template/download', 'POST', 1210),
 (111011, '平台与系统dataimport新增', 'system:dataimport:create', '/api/v1/sync-config/{configId}/field-mappings', 'POST', 1211),
 (111012, '平台与系统dataimport删除', 'system:dataimport:delete', '/api/v1/sync-config/{configId}/field-mappings/{id}', 'DELETE', 1212),
 (111013, '平台与系统dataimport查询', 'system:dataimport:list', '/api/v1/sync-config/{configId}/field-mappings', 'GET', 1213),
 (111014, '平台与系统dataimport编辑', 'system:dataimport:update', '/api/v1/sync-config/{configId}/field-mappings/{id}', 'PUT', 1214),
 (111015, '平台与系统dict详情', 'system:dict:detail', '/api/dict/item/{id}', 'GET', 1215),
 (111016, '平台与系统dict查询', 'system:dict:list', '/api/dict/item/type/{dictTypeId}', 'GET', 1216),
 (111017, '平台与系统dict查看', 'system:dict:view', '/api/dict/item/value', 'GET', 1217),
 (111018, '平台与系统feedback详情', 'system:feedback:detail', '/api/feedback/{feedbackId}', 'GET', 1218),
 (111019, '平台与系统feedback查询', 'system:feedback:list', '/api/feedback/list', 'GET', 1219),
 (111020, '平台与系统feedback查看', 'system:feedback:view', '/api/feedback/{feedbackId}/replies', 'GET', 1220),
 (111021, '平台与系统gateway新增', 'system:gateway:create', '/api/gateway/route', 'POST', 1221),
 (111022, '平台与系统gateway删除', 'system:gateway:delete', '/api/gateway/route/{id}', 'DELETE', 1222),
 (111023, '平台与系统gateway编辑', 'system:gateway:update', '/api/gateway/route', 'PUT', 1223),
 (111024, '平台与系统gateway查看', 'system:gateway:view', '/api/gateway/routes', 'GET', 1224),
 (111025, '平台与系统import-template校验', 'system:import-template:check', '/api/import-templates/{templateId}/validate', 'POST', 1225),
 (111026, '平台与系统import-template新增', 'system:import-template:create', '/api/import-templates', 'POST', 1226),
 (111027, '平台与系统import-template删除', 'system:import-template:delete', '/api/import-templates/{templateId}', 'DELETE', 1227),
 (111028, '平台与系统import-template详情', 'system:import-template:detail', '/api/import-templates/{templateId}', 'GET', 1228),
 (111029, '平台与系统import-template导出', 'system:import-template:export', '/api/import-templates/{templateId}/download', 'GET', 1229),
 (111030, '平台与系统import-template编辑', 'system:import-template:update', '/api/import-templates/{templateId}', 'PUT', 1230),
 (111031, '平台与系统import-template查看', 'system:import-template:view', '/api/import-templates', 'GET', 1231),
 (111032, '平台与系统import校验', 'system:import:check', '/api/import/v2/validate/{dataType}', 'POST', 1232),
 (111033, '平台与系统import新增', 'system:import:create', '/api/import/upload', 'POST', 1233),
 (111034, '平台与系统import详情', 'system:import:detail', '/api/import/progress/{taskId}', 'GET', 1234),
 (111035, '平台与系统import编辑', 'system:import:update', '/api/import/cancel/{taskId}', 'POST', 1235),
 (111036, '平台与系统import查看', 'system:import:view', '/api/import/template/{dataType}/config', 'GET', 1236),
 (111037, '平台与系统integration新增', 'system:integration:create', '/api/integration/configs', 'POST', 1237),
 (111038, '平台与系统integration删除', 'system:integration:delete', '/api/integration/configs/{configId}', 'DELETE', 1238),
 (111039, '平台与系统integration详情', 'system:integration:detail', '/api/integration/configs/{configId}', 'GET', 1239),
 (111040, '平台与系统integration执行', 'system:integration:execute', '/api/integration/sync/user', 'POST', 1240),
 (111041, '平台与系统integration重试', 'system:integration:retry', '/api/integration/sync/records/{recordId}/retry', 'POST', 1241),
 (111042, '平台与系统integration编辑', 'system:integration:update', '/api/integration/configs/{configId}', 'PUT', 1242),
 (111043, '平台与系统integration查看', 'system:integration:view', '/api/integration/configs', 'GET', 1243),
 (111044, '平台与系统knowledge新增', 'system:knowledge:create', '/api/knowledge/base', 'POST', 1244),
 (111045, '平台与系统knowledge删除', 'system:knowledge:delete', '/api/knowledge/base/{id}', 'DELETE', 1245),
 (111046, '平台与系统knowledge详情', 'system:knowledge:detail', '/api/knowledge/base/{id}', 'GET', 1246),
 (111047, '平台与系统knowledge执行', 'system:knowledge:execute', '/api/knowledge/embedding', 'POST', 1247),
 (111048, '平台与系统knowledge查询', 'system:knowledge:list', '/api/knowledge/base/list', 'GET', 1248),
 (111049, '平台与系统knowledge编辑', 'system:knowledge:update', '/api/knowledge/document/{id}/parse', 'POST', 1249),
 (111050, '平台与系统monitor-alert新增', 'system:monitor-alert:create', '/api/monitor/alerts/rules', 'POST', 1250),
 (111051, '平台与系统monitor-alert删除', 'system:monitor-alert:delete', '/api/monitor/alerts/rules/{ruleId}', 'DELETE', 1251),
 (111052, '平台与系统monitor-alert详情', 'system:monitor-alert:detail', '/api/monitor/alerts/rules/{ruleId}', 'GET', 1252),
 (111053, '平台与系统monitor-alert执行', 'system:monitor-alert:execute', '/api/monitor/alerts/notification/test', 'POST', 1253),
 (111054, '平台与系统monitor-alert编辑', 'system:monitor-alert:update', '/api/monitor/alerts/rules/{ruleId}', 'PUT', 1254),
 (111055, '平台与系统monitor-alert查看', 'system:monitor-alert:view', '/api/monitor/alerts/rules', 'GET', 1255),
 (111056, '平台与系统monitor-health查看', 'system:monitor-health:view', '/api/monitor/health/status', 'GET', 1256),
 (111057, '平台与系统monitor-infrastructure查看', 'system:monitor-infrastructure:view', '/api/monitor/infrastructure/server/info', 'GET', 1257),
 (111058, '平台与系统monitor-performance查看', 'system:monitor-performance:view', '/api/monitor/performance/realtime', 'GET', 1258),
 (111059, '平台与系统monitor执行', 'system:monitor:execute', '/api/monitor/gc', 'POST', 1259),
 (111060, '平台与系统monitor查看', 'system:monitor:view', '/api/monitor/metrics', 'GET', 1260),
 (111061, '平台与系统mq-enhanced详情', 'system:mq-enhanced:detail', '/api/mq/enhanced/retry/status/{messageId}', 'GET', 1261),
 (111062, '平台与系统mq-enhanced执行', 'system:mq-enhanced:execute', '/api/mq/enhanced/send/batch', 'POST', 1262),
 (111063, '平台与系统mq-enhanced重试', 'system:mq-enhanced:retry', '/api/mq/enhanced/retry/resend/{messageId}', 'POST', 1263),
 (111064, '平台与系统mq-enhanced查看', 'system:mq-enhanced:view', '/api/mq/enhanced/monitor/metrics', 'GET', 1264),
 (111065, '平台与系统mq执行', 'system:mq:execute', '/api/mq/send/email', 'POST', 1265),
 (111066, '平台与系统mq查看', 'system:mq:view', '/api/mq/queues', 'GET', 1266),
 (111067, '平台与系统recommendation新增', 'system:recommendation:create', '/api/recommendation/behavior', 'POST', 1267),
 (111068, '平台与系统recommendation删除', 'system:recommendation:delete', '/api/recommendation/cache', 'DELETE', 1268),
 (111069, '平台与系统recommendation查询', 'system:recommendation:list', '/api/recommendation/get', 'POST', 1269),
 (111070, '平台与系统recommendation查看', 'system:recommendation:view', '/api/recommendation/personalized', 'GET', 1270),
 (111071, '平台与系统report-analytics查看', 'system:report-analytics:view', '/api/report/analytics/{reportId}/yoy', 'POST', 1271),
 (111072, '平台与系统report-schedule新增', 'system:report-schedule:create', '/api/report/schedule', 'POST', 1272),
 (111073, '平台与系统report-schedule删除', 'system:report-schedule:delete', '/api/report/schedule/{scheduleId}', 'DELETE', 1273),
 (111074, '平台与系统report-schedule执行', 'system:report-schedule:execute', '/api/report/schedule/{scheduleId}/trigger', 'POST', 1274),
 (111075, '平台与系统report-schedule暂停', 'system:report-schedule:pause', '/api/report/schedule/{scheduleId}/pause', 'POST', 1275),
 (111076, '平台与系统report-schedule恢复', 'system:report-schedule:resume', '/api/report/schedule/{scheduleId}/resume', 'POST', 1276),
 (111077, '平台与系统report-schedule编辑', 'system:report-schedule:update', '/api/report/schedule/{scheduleId}', 'PUT', 1277),
 (111078, '平台与系统report-schedule查看', 'system:report-schedule:view', '/api/report/schedule', 'GET', 1278),
 (111079, '平台与系统报表新增', 'system:report:create', '/api/report', 'POST', 1279),
 (111080, '平台与系统报表删除', 'system:report:delete', '/api/report/{reportId}', 'DELETE', 1280),
 (111081, '平台与系统报表详情', 'system:report:detail', '/api/report/{reportId}', 'GET', 1281),
 (111082, '平台与系统报表导出', 'system:report:export', '/api/report/{reportId}/export/excel', 'POST', 1282),
 (111083, '平台与系统报表生成', 'system:report:generate', '/api/report/{reportId}/generate', 'POST', 1283),
 (111084, '平台与系统报表查询', 'system:report:list', '/api/report/list', 'GET', 1284),
 (111085, '平台与系统报表编辑', 'system:report:update', '/api/report/{reportId}', 'PUT', 1285),
 (111086, '平台与系统报表查看', 'system:report:view', '/api/report/{reportId}/preview', 'POST', 1286),
 (111087, '平台与系统search删除', 'system:search:delete', '/api/search/history', 'DELETE', 1287),
 (111088, '平台与系统search查询', 'system:search:list', '/api/search', 'POST', 1288),
 (111089, '平台与系统search编辑', 'system:search:update', '/api/search/index/customer/{customerId}', 'POST', 1289),
 (111090, '平台与系统storage校验', 'system:storage:check', '/api/storage/chunk/check', 'POST', 1290),
 (111091, '平台与系统storage新增', 'system:storage:create', '/api/storage/chunk/init', 'POST', 1291),
 (111092, '平台与系统storage删除', 'system:storage:delete', '/api/storage/chunk/abort/{uploadId}', 'DELETE', 1292),
 (111093, '平台与系统storage详情', 'system:storage:detail', '/api/storage/chunk/info/{uploadId}', 'GET', 1293),
 (111094, '平台与系统storage导出', 'system:storage:export', '/api/storage/download/{fileId}', 'GET', 1294),
 (111095, '平台与系统storage查看', 'system:storage:view', '/api/storage/chunk/pending', 'GET', 1295),
 (111096, '平台与系统tenant-module查看', 'system:tenant-module:view', '/api/tenant-module/valid-codes', 'GET', 1296),
 (111097, '平台与系统tenant编辑', 'system:tenant:update', '/api/tenant/{id}/config', 'PUT', 1297),
 (111098, '平台与系统department新增', 'tenant-admin:department:create', '/api/department', 'POST', 1298),
 (111099, '平台与系统department删除', 'tenant-admin:department:delete', '/api/department/{id}', 'DELETE', 1299),
 (111100, '平台与系统department编辑', 'tenant-admin:department:edit', '/api/department/{id}', 'PUT', 1300),
 (111101, '平台与系统department查询', 'tenant-admin:department:list', '/api/department/page', 'GET', 1301),
 (111102, '平台与系统departmentquery', 'tenant-admin:department:query', '/api/department/{id}', 'GET', 1302),
 (111103, '平台与系统position分配', 'tenant-admin:position:assign', '/api/position/assign', 'POST', 1303),
 (111104, '平台与系统position新增', 'tenant-admin:position:create', '/api/position', 'POST', 1304),
 (111105, '平台与系统position删除', 'tenant-admin:position:delete', '/api/position/{id}', 'DELETE', 1305),
 (111106, '平台与系统position编辑', 'tenant-admin:position:edit', '/api/position/{id}', 'PUT', 1306),
 (111107, '平台与系统position查询', 'tenant-admin:position:list', '/api/position/page', 'GET', 1307),
 (111108, '平台与系统positionquery', 'tenant-admin:position:query', '/api/position/{id}', 'GET', 1308),
 (111109, '平台与系统callback-log查询', 'workflow:callback-log:list', '/api/workflow/callback-log/page', 'GET', 1309),
 (111110, '平台与系统callback-log重试', 'workflow:callback-log:retry', '/api/workflow/callback-log/{id}/retry', 'POST', 1310),
 (111111, '平台与系统instance取消', 'workflow:instance:cancel', '/api/workflow/{instanceId}/cancel', 'POST', 1311),
 (111112, '平台与系统instancestart', 'workflow:instance:start', '/api/workflow/start', 'POST', 1312),
 (111113, '平台与系统instance查看', 'workflow:instance:view', '/api/workflow/instances/{instanceId}', 'GET', 1313),
 (111114, '平台与系统instancewithdraw', 'workflow:instance:withdraw', '/api/workflow/{instanceId}/withdraw', 'POST', 1314),
 (111115, '平台与系统任务审批', 'workflow:task:approve', '/api/workflow/{instanceId}/approve', 'POST', 1315),
 (111116, '平台与系统任务transfer', 'workflow:task:transfer', '/api/workflow/{instanceId}/transfer', 'POST', 1316),
 (111117, '平台与系统任务查看', 'workflow:task:view', '/api/workflow/pending', 'GET', 1317)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9610000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 111000 AND 111118
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
