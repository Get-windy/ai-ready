-- =============================================================================
-- 设置模块 → 支付配置（菜单 80623 / set:payment-config）· 权限码种子
-- （2026-09-18，依据《设置模块/支付配置开发文档.md》§5.4 / §12-③）
--
-- 【修的是什么】
--   开发文档 §12-③ 登记的 P0（越权面）：
--     · `PaymentController`（前缀 /api/payment，11 个端点）**全部无权限注解** →
--       任何登录用户都能创建 / 取消 / 确认他人的支付请求；
--     · 本页前端**零 v-permission**；
--     · 实测（devdb 2026-09-18）：`sys_permission` 中 `payment%` 与 `set%` 前缀**均为 0 行** →
--       不补种子，则除超管（走 `*` 通配）外所有账号访问本页会 403。
--
-- 【本轮落库的 8 个码，与控制器注解一一对应】
--   ── 支付配置（本页自有端点，PaymentConfigController /api/payment/config/*）──
--   payment:config:list    支付配置查询   GET  /api/payment/config/channels、/channels/{code}、
--                                              /items、/scenes、/refund-notes
--   payment:config:update  支付配置保存   POST /api/payment/config/channels/{code}、/items、/scenes/{code}
--   ── 支付业务（PaymentController /api/payment/*，本页与交易/财务的支付台账页共用）──
--   payment:channel:list   支付渠道查询   GET  /api/payment/channels
--   payment:request:list   支付请求查询   GET  /api/payment/request/page、/request/stat、/request/{id}
--   payment:request:create 支付请求创建   POST /api/payment/request
--   payment:request:cancel 支付请求取消   POST /api/payment/request/{id}/cancel
--   payment:request:confirm 支付请求确认  POST /api/payment/request/{id}/confirm
--   payment:record:list    支付记录查询   GET  /api/payment/record/page、/record/stat
--
-- 【有意**不**加权限注解的端点】
--   POST /api/payment/callback/{channel} —— 渠道侧服务器调用的回调入口，调用方没有 Sa-Token 会话，
--   加权限注解没有意义（它本就先被登录拦截器挡下）。该端点的验签 / 防重放属另一议题，
--   开发文档 §12-⑰ 已如实登记为「未核对」，不在本次改动范围。
--
-- 【命名口径】
--   权限码第一段沿用**业务域**（与 `finance:` / `hr:` / `crm:` / `log:` 同风格），
--   本页控制器都属支付域，故统一 `payment:` 前缀；不新造 `set:` 前缀（该前缀在 sys_permission 中
--   至今 0 行，避免出现两套并存的第一段）。
--
-- 【id 区间】
--   权限 91301–91308、角色授权 9130701–9130708。
--   已核 devdb 该区间未被占用；⚠️ 勿用 91201–91206 —— 该区间已被并行会话的
--   V11.394.0__Seed_Operation_Log_Permissions.sql（log:oper:* / log:login:*）占用。
--
-- 【授权说明】
--   按 V11.362.0 / V11.394.0 的既有做法，同时授权给超级管理员角色（role_id = 1）；
--   超管另有 `*` 通配，这里只为让「权限清单」完整可审计。
--   普通租户角色请由租户管理员在「系统 → 角色管理」中按需勾选（本迁移不越权代配）。
--
-- 幂等：全部 WHERE NOT EXISTS，可重复执行。
-- =============================================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
SELECT v.id, 1, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
    (91301::BIGINT, '支付配置查询',   'payment:config:list',     '/api/payment/config/*',                  'GET',    801),
    (91302::BIGINT, '支付配置保存',   'payment:config:update',   '/api/payment/config/*',                  'POST',   802),
    (91303::BIGINT, '支付渠道查询',   'payment:channel:list',    '/api/payment/channels',                  'GET',    803),
    (91304::BIGINT, '支付请求查询',   'payment:request:list',    '/api/payment/request/page',              'GET',    804),
    (91305::BIGINT, '支付请求创建',   'payment:request:create',  '/api/payment/request',                   'POST',   805),
    (91306::BIGINT, '支付请求取消',   'payment:request:cancel',  '/api/payment/request/{id}/cancel',       'POST',   806),
    (91307::BIGINT, '支付请求确认',   'payment:request:confirm', '/api/payment/request/{id}/confirm',      'POST',   807),
    (91308::BIGINT, '支付记录查询',   'payment:record:list',     '/api/payment/record/page',               'GET',    808)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

-- 授权给超级管理员角色（role_id = 1），已存在则跳过
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9130700 + (p.sort - 800), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.permission_code IN ('payment:config:list', 'payment:config:update',
                            'payment:channel:list', 'payment:request:list',
                            'payment:request:create', 'payment:request:cancel',
                            'payment:request:confirm', 'payment:record:list')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
