-- =============================================================================
-- 补齐「操作日志」（设置 → 账套操作 → 操作日志，菜单 80630）的权限码种子
-- 2026-09-18
--
-- 背景（《操作日志开发文档》§5.5 / §9.2-P0 / §11）：
--   页面在用的 SysLogStubController（/api/log，6 个端点）**零权限注解** ——
--   任何登录用户都能清空 / 导出他人租户的日志（越权面 P0）。
--   本轮给该控制器补上 @SaCheckPermission 注解，权限码**沿用平行实现
--   LogManageController 已在用、但从未落库的 log:oper:* / log:login:* 码，不新造码**。
--
--   实测（devdb 2026-09-18）：`sys_permission` 中 `log:oper%` 与 `log:login%` 均为 0 行
--   （`log:*` 前缀共 6 行，全部是 `log:audit:*`）→ 不补种子，则除超管（走 `*` 通配）
--   以外所有账号访问该页会 403。
--
-- 落库清单（共 6 个码，与控制器实际注解一一对应）：
--   log:oper:list     系统日志查询    GET    /api/log/page、/api/log/modules、/api/log/operation-types
--   log:oper:detail   系统日志详情    GET    /api/log/{id}
--   log:oper:export   系统日志导出    GET    /api/log/export
--   log:oper:delete   历史日志清理    DELETE /api/log/clear、GET /api/log/clear/preview
--   log:login:list    登录日志查询    GET    /api/log/login/page
--   log:login:export  登录日志导出    GET    /api/log/login/export
--
-- 授权说明：按 V11.362.0 的既有做法，同时授权给超管角色（role_id = 1）。
--   超管另有 `*` 通配，这里只为让「权限清单」完整可审计；
--   普通租户角色请由租户管理员在「系统 → 角色管理」中按需勾选（本迁移不越权代配）。
--
-- id 取 91201–91206 / 9120701–9120706：已核 devdb 该区间未被占用
--   （⚠️ 勿用 9119x —— 该区间已被 hr-recruitment:* 占用）。
--
-- 幂等：全部 WHERE NOT EXISTS，可重复执行。
-- =============================================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
SELECT v.id, 1, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
    (91201::BIGINT, '系统日志查询', 'log:oper:list',    '/api/log/page',         'GET',    391),
    (91202::BIGINT, '系统日志详情', 'log:oper:detail',  '/api/log/{id}',         'GET',    392),
    (91203::BIGINT, '系统日志导出', 'log:oper:export',  '/api/log/export',       'GET',    393),
    (91204::BIGINT, '历史日志清理', 'log:oper:delete',  '/api/log/clear',        'DELETE', 394),
    (91205::BIGINT, '登录日志查询', 'log:login:list',   '/api/log/login/page',   'GET',    395),
    (91206::BIGINT, '登录日志导出', 'log:login:export', '/api/log/login/export', 'GET',    396)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

-- 授权给超级管理员角色（role_id = 1），已存在则跳过
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9120700 + (p.sort - 390), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.permission_code IN ('log:oper:list', 'log:oper:detail', 'log:oper:export',
                            'log:oper:delete', 'log:login:list', 'log:login:export')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
