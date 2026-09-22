-- 通知服务域（core-notification）权限码种子（E-01 corenotify 批次，2026-09-21）
--
-- 【与"零码模块"种子的区别】core-notification **是零码模块**：`notification:` 前缀在
--   sys_permission 里一条都没有（`webhook:` 同理）。本迁移 22 个码全部是新建。
--   VALUES 占满 123000~123021 共 22 个槽位，实际落库 22 个。
--
-- 【为什么必须补】E-01 要给四个裸控制器补 @SaCheckPermission，而
--   **没有码就补注解 ⇒ 该接口对所有非超管一律 403**（本仓铁律/历史事故）。
--
-- 【口径要点】全部写在 `tools/gen-module-permission-seed.py` 的 `MODULES['corenotify']`：
--   · 四个控制器**共用 `notification:` 一个域**（同一 Maven 模块、同一功能域）。不指定的话
--     路径推导会产出 `notification-templates:` / `notification-stats:` / `webhook:` 三个一级域，
--     `/api/core/notification` 更会退化成 `core:notification:*`（`core` 是路径首段）。
--   · `/api/core/notification` 的资源名取 `message` 而**不是** `record` ——
--     `record` 已被 signature 批次占用为「签收记录」，复用会拼出「通知服务签收记录导出」。
--   · webhook 的**推送日志是独立资源**（`notification:webhook-log:list`），
--     不并进 `webhook`（那是钩子配置本身）。
--   · 动作词：预览→view（同 print:template:view 先例）、校验变量→check、刷新缓存→refresh、
--     重试失败→retry、手动触发→execute、验签→check；均取自库中已有动作词。
--
-- 【本批有意排除的端点 —— 共 8 个，不加任何权限码】
--   · `/api/core/notification/{unread,list,unread-count,{id}/read,read-all,{id} DELETE,batch,read DELETE}`（8）：
--     这是 core-base `/api/notification` 的**并行实现**，且 userId 取自
--     `@RequestHeader X-User-Id` 而**不是会话** ⇒ ① 属自助接口（同 /api/notification 口径，
--     顶栏铃铛语义）；② 加注解也保护不了它（调用方可以自己伪造 X-User-Id 读别人的通知
--     —— 这是**既有越权缺陷**，本批只报告、不改业务代码）。
--     管理侧（export / send / send-template / templates GET+POST）仍补码。
--
-- 【id 号段】权限码 123000 起（槽位 23）、角色关联 9730000 起，执行前实测两段均为空。
--   ⚠️ 该模块的 `cn.aiedge.webhook.controller` **不在** AiReadyApplication.scanBasePackages 里
--      ⇒ WebhookController 的 14 个端点在运行时一律 **404**（既有状况，见
--      `known-unscanned-controller-packages.txt`）。码照建（控制器一旦接线即生效），
--      但运行时无从验证 —— 探针里不做 webhook 的断言。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。
-- =============================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (123000, '通知服务消息导出', 'notification:message:export', '/api/core/notification/export', 'GET', 1200),
 (123001, '通知服务消息发送', 'notification:message:send', '/api/core/notification/send', 'POST', 1201),
 (123002, '通知服务统计重试', 'notification:stats:retry', '/api/notification-stats/retry-failed', 'POST', 1202),
 (123003, '通知服务统计查看', 'notification:stats:view', '/api/notification-stats/channels', 'GET', 1203),
 (123004, '通知服务模板校验', 'notification:template:check', '/api/notification-templates/{id}/validate', 'POST', 1204),
 (123005, '通知服务模板新增', 'notification:template:create', '/api/core/notification/templates', 'POST', 1205),
 (123006, '通知服务模板删除', 'notification:template:delete', '/api/notification-templates/{id}', 'DELETE', 1206),
 (123007, '通知服务模板详情', 'notification:template:detail', '/api/notification-templates/{id}', 'GET', 1207),
 (123008, '通知服务模板导出', 'notification:template:export', '/api/notification-templates/export', 'GET', 1208),
 (123009, '通知服务模板查询', 'notification:template:list', '/api/core/notification/templates', 'GET', 1209),
 (123010, '通知服务模板刷新', 'notification:template:refresh', '/api/notification-templates/cache/refresh', 'POST', 1210),
 (123011, '通知服务模板编辑', 'notification:template:update', '/api/notification-templates/{id}', 'PUT', 1211),
 (123012, '通知服务模板查看', 'notification:template:view', '/api/notification-templates/{id}/preview', 'POST', 1212),
 (123013, '通知服务钩子日志查询', 'notification:webhook-log:list', '/api/webhook/{webhookId}/logs', 'GET', 1213),
 (123014, '通知服务事件钩子校验', 'notification:webhook:check', '/api/webhook/verify-signature', 'POST', 1214),
 (123015, '通知服务事件钩子新增', 'notification:webhook:create', '/api/webhook', 'POST', 1215),
 (123016, '通知服务事件钩子删除', 'notification:webhook:delete', '/api/webhook/{id}', 'DELETE', 1216),
 (123017, '通知服务事件钩子详情', 'notification:webhook:detail', '/api/webhook/{id}', 'GET', 1217),
 (123018, '通知服务事件钩子执行', 'notification:webhook:execute', '/api/webhook/trigger', 'POST', 1218),
 (123019, '通知服务事件钩子查询', 'notification:webhook:list', '/api/webhook/model/{modelName}', 'GET', 1219),
 (123020, '通知服务事件钩子重试', 'notification:webhook:retry', '/api/webhook/retry', 'POST', 1220),
 (123021, '通知服务事件钩子编辑', 'notification:webhook:update', '/api/webhook/{id}', 'PUT', 1221)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9730000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 123000 AND 123022
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
