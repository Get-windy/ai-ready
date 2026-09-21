-- =============================================================================
-- 补齐「租户管理」读接口的权限码（2026-09-20）
--
-- 【为什么】`TenantController` 的写接口早已有权限码
--   （`system:tenant:create` / `update` / `delete` / `approve`，见 V11.xxx 种子），
--   但**读接口没有任何权限注解**（类级只有 `@SaCheckLogin`）：
--     · GET /api/tenant/page    —— getPage   分页枚举全部租户
--     · GET /api/tenant/{id}    —— getById   逐个读取租户档案
--     · GET /api/tenant/{id}/config —— getConfig
--   而 `sys_tenant` 在多租户忽略清单里（查询不带 tenant_id 条件）
--   ⇒ **任意登录用户（含租户管理员）可枚举全部租户的档案、状态、联系方式、admin_user_id、到期时间**。
--   属「平台侧数据被租户侧越权读」。
--
-- 【铁律：先补种子，再补注解】本仓历史事故：注解引用了库中不存在的权限码 ⇒
--   该接口对**所有非超管用户一律 403**。故本迁移是给 `TenantController` 三个读接口
--   加 `@SaCheckPermission` 的**前置动作，顺序不能反**；注解由同批次代码改动加上。
--
-- 【口径】对齐同域既有码（`system:tenant:*`）与 V11.434.0 的写法：
--   tenant_id=0、parent_id=0、permission_type=3、status=0（0=正常）、visible=1。
-- 【id 依据】实测 2026-09-20：`SELECT count(*) FROM sys_permission WHERE id BETWEEN 90066 AND 90100` = 0（整段空闲）。
--
-- 【必须同时关联超管角色】本仓的超管权限**不是**硬编码放行，而是来自 `sys_role_permission`
--   （`StpInterfaceImpl#getPermissionList` → `permissionCacheService.getPermissions`）。
--   每个权限种子迁移都要显式把新码关联给 `role_id = 1`（SUPER_ADMIN），
--   否则连平台管理员自己都会被拒。写法沿用 V11.394.0 的相对偏移模式。
-- =============================================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
VALUES
 (90066, 0, 0, 0, now(), now(), '租户列表查询', 'system:tenant:list',  3, NULL, NULL, 1090, 1, 0),
 (90067, 0, 0, 0, now(), now(), '租户详情查询', 'system:tenant:query', 3, NULL, NULL, 1091, 1, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9400000 + (p.sort - 1090), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.permission_code IN ('system:tenant:list', 'system:tenant:query')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
