-- 2026-09-27 分析模块权限补授（把分析码授给「租户超管」）
--
-- 【问题】
--   分析模块 31 页的取数接口共需 14 个权限码，此前**只授给了 SUPER_ADMIN**（平台超管）。
--   租户超管（SYSTEM_ADMIN）调这些接口一律 403 —— 实测 tools/verify-analytics-authz.cjs：
--   18 个代表接口中 **15 个被拒**（仅进销存/查库存/库存明细 3 个能访问，因为 stock:analytics:list 已授）。
--   症状是"菜单看得见、点进去提示无权限"，属典型的"权限配了但落不到人"。
--
-- 【授权对象】
--   SYSTEM_ADMIN（系统管理员）= 本系统的**租户超管**。
--   佐证：tools/e2e-hr-user.sql 里 e2e_hr_ta 的昵称即「租户管理员验收账号」，其角色就是 SYSTEM_ADMIN。
--   **不含 DEPT_ADMIN**（部门管理员权限面应更窄，是否授分析码另行评估）。
--
-- 【为什么包含 5 个非 analytics 命名空间的码】
--   分析页面的取数是跨域调用，权限码按"被调接口所属域"命名。例如：
--     「回款统计」→ finance:collection-stats:view     「发票统计」→ invoice:view（已持有）
--     「查应收」  → finance:report:view              「交易分析」→ mall:trade-analysis:view
--     「业务员提成」→ marketing:commission:list（已持有）
--   只授 analytics 自身的码，这些页面仍会 403。
--   这些码本身都是租户内的只读/分析能力，给"租户超管"不越界。
--
-- 【租户标记】
--   tenant_id 取**角色自身的 tenant_id**（r.tenant_id），不用硬编码 1 ——
--   本仓曾有 sys_role_permission 租户标记不一致的实证（16 行）。
--
-- 【回滚】
--   DELETE FROM sys_role_permission WHERE id BETWEEN 9900000 AND 9900999;

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9900000 + row_number() OVER (ORDER BY p.id), r.id, p.id, r.tenant_id, now()
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'SYSTEM_ADMIN'
  AND r.deleted = 0
  AND p.deleted = 0
  AND p.permission_code IN (
      'doc:docquery:list',
      'erp:expense:statistics:list',
      'finance:analytics-collection-stats:list',
      'finance:analytics-collection-stats:view',
      'finance:analytics-invoice-stats:list',
      'finance:analytics-partner-balance:reconcile',
      'finance:collection-stats:view',
      'finance:partner-balance:view',
      'finance:report:view',
      'mall:trade-analysis:view',
      'purchase:analytics:list',
      'sale:analysis-promotion-funnel:list',
      'sale:analysis:list',
      'sale:pre-order-analysis:list'
  )
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = r.id AND rp.permission_id = p.id);
