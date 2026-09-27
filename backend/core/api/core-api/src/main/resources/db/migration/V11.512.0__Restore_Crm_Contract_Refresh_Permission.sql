-- 2026-09-26 恢复被误删的权限码 crm:contract:refresh（该端点对非超管一直 403）
--
-- ── 症状 ──────────────────────────────────────────────────────────────────
-- `ContractController#markExpiredContracts`（`POST /api/crm/contract/mark-expired`，ContractController:236）
-- 标着 `@SaCheckPermission("crm:contract:refresh")`，但该码在 `sys_permission` 里只剩一行 **deleted = 1**
-- ⇒ 用户永远拿不到这个码 ⇒ 该接口对**除超管（通配符 `*`）以外的所有人**都是 403。
-- 本地用超管调试完全看不出来。
--
-- ── 根因：两次迁移之间的"软删行挡住重新插入" ──────────────────────────────
--   ① `V11.379.0` 首次播种该码（id 91059）；
--   ② `V11.452.0__Remove_Zombie_Core_And_Crm_Permissions` 把它当僵尸码**软删**（当时确实无人引用）；
--   ③ E-01 批次给合同端点补注解后，`V11.458.0__Seed_Crm_Permissions` 又试图补种它
--      （id 110005、api_path `/api/crm/contract/mark-expired`），
--      但该种子用的是 `WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code)`
--      —— **没有 `deleted = 0` 条件**，于是被 ② 留下的软删行判定为"已存在"，**静默跳过**。
--   ⇒ 结果是一次"看起来补了、其实没补"的迁移。
--
-- 这与本仓 CRM 文档登记过的另一条同源：软删行仍占着唯一性/存在性判断
-- （见 CRM_MODULE_AUDIT_20260923.md §2.6 的"软删后重号"）。
--
-- ── 顺带订正 ──────────────────────────────────────────────────────────────
-- 该码名称原为「CRM合同刷新」，但端点语义是"批量标记到期合同"，故订正名称并补 api_path/method。
-- 它属**系统级维护动作**（同 `ContractController.markExpiredContracts`），只授系统管理员，不下发部门管理员。
--
-- 复核命令（应无输出）：
--   python tools/audit-permission-codes.py   → 「代码引用了但库中没有」应为 0 个

-- ── ① 取消软删并补齐元数据（就地复用原行，不新插一行，避免同码多行）────────
UPDATE sys_permission
   SET deleted = 0,
       permission_name = 'CRM合同标记到期',
       permission_type = 3,
       status = 0,
       api_path = '/api/crm/contract/mark-expired',
       method = 'POST',
       update_time = CURRENT_TIMESTAMP
 WHERE permission_code = 'crm:contract:refresh'
   AND deleted <> 0;

-- ── ② 授给超管与系统管理员（幂等）────────────────────────────────────────
-- ⚠️ 主键**不能写死基数**：`sys_role_permission.id` 没有序列，各批迁移各自挑区间，
--    `9800000` 已被其它迁移占用（首次执行即撞 `sys_role_permission_pkey`）。
--    这里以「当前最大 id」为基数顺延，多会话并行也不会撞。
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT (SELECT COALESCE(MAX(id), 0) FROM sys_role_permission) + row_number() OVER (ORDER BY r.id),
       r.id, p.id, 1, now()
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code IN ('SUPER_ADMIN', 'SYSTEM_ADMIN')
  AND p.permission_code = 'crm:contract:refresh'
  AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ── ③ 自检 ───────────────────────────────────────────────────────────────
-- 1) 该码应为 1 行、deleted = 0、有 api_path
--   SELECT id, permission_code, deleted, api_path FROM sys_permission WHERE permission_code = 'crm:contract:refresh';
-- 2) 授码：SUPER_ADMIN / SYSTEM_ADMIN 各 1 行
--   SELECT r.role_code, count(*) FROM sys_role_permission rp
--     JOIN sys_role r ON r.id = rp.role_id JOIN sys_permission p ON p.id = rp.permission_id
--    WHERE p.permission_code = 'crm:contract:refresh' GROUP BY r.role_code;
