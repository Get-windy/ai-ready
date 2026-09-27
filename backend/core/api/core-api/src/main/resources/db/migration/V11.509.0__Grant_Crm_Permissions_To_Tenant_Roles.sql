-- 2026-09-26 给租户角色授予 CRM / 发票权限码
--
-- 背景：CRM 的权限码此前**只授给了 SUPER_ADMIN**（`sys_role_permission` 里 crm:* 全部 role_id=1）。
-- 叠加两处既有事实，租户侧 CRM 完全不可用：
--   ① `V11.458.0` 之后 CRM 的 185 个端点全部带 `@SaCheckPermission` ⇒ 无码即 403；
--   ② 菜单可见性由权限码派生（平台-AUTHZ-01）⇒ 无码则菜单也不下发。
-- 而 `sys_tenant_module` 里租户 1/2 都已启用 `crm`（模块权益门放行），
-- 于是现状是「买了、能进、但除超管外全是 403 / 看不到入口」。
--
-- 本迁移给**租户内的管理角色**授码，遵循两条业界惯例：
--   · 职责分离：日常业务操作（录入/跟进/拜访）与敏感动作（删除 / 审批 / 转订单 / 批量）分开授权，
--     部门管理员拿前者，系统管理员拿全部；
--   · 模块权益与角色权限分开：这里只是"人能不能做"，"租户买没买"仍由 sys_tenant_module 表达。
--
-- ⚠️ 刻意**不动** `E2E_T2_ADMIN`（租户 2 的测试账号）：`tools/verify-module-authz.cjs`
--    用它当"无码的非超管"探针来验证「注解已生效 ⇒ 403」，给它授码会让该断言失效。
--    该账号只在 `verify-crm-tenant-fix.cjs` 里被补过 2 条合同码，维持原样。

-- ── ① 系统管理员（SYSTEM_ADMIN，租户 1）：CRM 全部码 + 发票全部码 ────────────
-- 发票虽挂在财务模块下（见 V11.508.0），但租户系统管理员应能管本租户的开票链路。
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9700000 + row_number() OVER (ORDER BY p.id), r.id, p.id, 1, now()
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'SYSTEM_ADMIN'
  AND p.deleted = 0
  AND (p.permission_code LIKE 'crm:%' OR p.permission_code LIKE 'invoice:%')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ── ② 部门管理员（DEPT_ADMIN，租户 1）：日常业务操作集合 ──────────────────
-- 原则：能录入、能跟进、能拜访、能查看与导出；**不含**删除、审批、转订单、续签、批量刷新。
-- 逐条给出理由，便于后续按需增删。
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9750000 + row_number() OVER (ORDER BY p.id), r.id, p.id, 1, now()
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'DEPT_ADMIN'
  AND p.deleted = 0
  AND p.permission_code IN (
      -- 客户：查看 / 新建 / 编辑 / 导入 / 跟进（不含 delete）
      'crm:customer:list', 'crm:customer:view', 'crm:customer:create',
      'crm:customer:update', 'crm:customer:import', 'crm:customer:follow',
      -- 客户公海：查看 / 领取 / 退回（update 即领取与退回）
      'crm:customer-pool:list', 'crm:customer-pool:view', 'crm:customer-pool:update',
      -- 跟进记录：全量（日常最高频动作）
      'crm:follow-up:list', 'crm:follow-up:detail', 'crm:follow-up:create',
      'crm:follow-up:update', 'crm:follow-up:delete',
      -- 线索：录入 / 编辑 / 转化 / 导入导出（不含 delete / 批量分配）
      'crm:lead:view', 'crm:lead:create', 'crm:lead:edit',
      'crm:lead:convert', 'crm:lead:import', 'crm:lead:export',
      -- 商机：录入 / 编辑 / 导出（不含 delete / convert 转报价）
      'crm:opportunity:view', 'crm:opportunity:create', 'crm:opportunity:edit',
      'crm:opportunity:export',
      -- 报价单：报价 / 发送 / 下载（不含 delete / approve / convert 转订单）
      'crm:quotation:view', 'crm:quotation:create', 'crm:quotation:edit',
      'crm:quotation:send', 'crm:quotation:downloadpdf',
      -- 合同：只读（签署/审批/终止属管理动作）
      'crm:contract:view',
      -- 外勤拜访：全量（计划 / 打卡 / 取消）
      'crm:visit:list', 'crm:visit:view', 'crm:visit:create',
      'crm:visit:update', 'crm:visit:delete', 'crm:visit:cancel'
  )
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ── ③ 自检 ───────────────────────────────────────────────────────────────
-- 各角色的 crm 码数（期望：SUPER_ADMIN = CRM 全量；SYSTEM_ADMIN = CRM 全量 + invoice；
--                       DEPT_ADMIN = 33 条；E2E_T2_ADMIN = 2 条）
--   SELECT r.role_code, count(*) FROM sys_role_permission rp
--     JOIN sys_role r ON r.id = rp.role_id
--     JOIN sys_permission p ON p.id = rp.permission_id
--    WHERE p.permission_code LIKE 'crm:%' GROUP BY r.role_code ORDER BY 1;
