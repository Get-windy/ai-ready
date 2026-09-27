-- 财务岗位角色与权限落地（方案 B · FINANCE_PERMISSION_PLAN_20260927.md）
--
-- 目标：把财务域的权限按「岗位职责」拆给四个新角色，并落实**职责分离（SoD）**：
--   财务主管 = 审批/过账类（approve/audit/post/reverse/reconcile/close/execute）+ 只读
--   会计     = 制单类（create/update/submit/delete/edit）+ 只读
--   出纳     = 收付款与资金类（receipt/payment/cash-transfer/pre-*) + 只读
--   预算管理员 = 预算域全部 + 只读
--
-- ⚠️ SoD 的硬约束（本脚本已按此筛选，勿改）：
--   财务主管 **不含** create/update/submit/delete/edit（审批人 ≠ 制单人）
--   会计     **不含** approve/audit/post/reverse（制单人 ≠ 审批人）
--   出纳     **不含** finance:voucher:audit（资金岗不得审凭证）
--
-- 幂等：角色按 role_code 判重；授权按 (role_id, permission_id) 判重。可重复执行。
-- 主键：授权用「当前 max(id) + row_number()」动态生成 —— **不要硬编码 id**
--       （本仓已两次踩硬编码 id 撞主键：V11.496.0 → sys_permission_pkey、V11.512.0 → sys_role_permission_pkey）
--
-- ⚠️ 执行前请备份：CREATE TABLE sys_role_permission_backup_YYYYMMDD AS SELECT * FROM sys_role_permission;
-- 用法：psql -h localhost -p 5432 -U devuser -d devdb -f tools/seed-finance-role-positions.sql

BEGIN;

-- ── ① 新建四个岗位角色（tenant_id=1 系统租户；scope=TENANT 与其他租户角色一致）──
INSERT INTO sys_role (id, tenant_id, role_code, role_name, role_type, scope, data_scope,
                      status, deleted, create_time, update_time, sort)
SELECT v.id, 1, v.code, v.name, NULL, 'TENANT', 0, 0, 0, now(), now(), 0
FROM (VALUES
  (2100000000000000001::bigint, 'FINANCE_MANAGER',    '财务主管'),
  (2100000000000000002::bigint, 'FINANCE_ACCOUNTANT', '会计'),
  (2100000000000000003::bigint, 'FINANCE_CASHIER',    '出纳'),
  (2100000000000000004::bigint, 'BUDGET_ADMIN',       '预算管理员')
) AS v(id, code, name)
WHERE NOT EXISTS (SELECT 1 FROM sys_role r WHERE r.role_code = v.code AND r.deleted = 0);

-- ── ② 按岗责授权（财务域 = finance: / receipt: / payment: / budget: 前缀）──
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time, create_by)
SELECT (SELECT COALESCE(MAX(id), 0) FROM sys_role_permission)
       + ROW_NUMBER() OVER (ORDER BY r.id, p.id),
       r.id, p.id, 1, now(), 1
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.deleted = 0
  AND p.deleted = 0
  AND r.role_code IN ('FINANCE_MANAGER', 'FINANCE_ACCOUNTANT', 'FINANCE_CASHIER', 'BUDGET_ADMIN')
  AND (p.permission_code LIKE 'finance:%' OR p.permission_code LIKE 'receipt:%'
       OR p.permission_code LIKE 'payment:%' OR p.permission_code LIKE 'budget:%')
  AND (
        -- (a) 通用只读类：四个角色都给
        p.permission_code ~ ':(view|list|detail|export)$'

        -- (b) 财务主管：审批 / 过账 / 对账 / 结账类（不含制单类）
        OR (r.role_code = 'FINANCE_MANAGER'
            AND p.permission_code ~ ':(approve|audit|post|reverse|reconcile|close|execute|check|handleDifference)$')

        -- (c) 会计：制单 / 修改 / 提交 / 删除（不含审批与过账）
        OR (r.role_code = 'FINANCE_ACCOUNTANT'
            AND p.permission_code ~ ':(create|update|submit|delete|edit)$')

        -- (d) 出纳：收付款单、提存、预收预付、资金流水的经办类
        OR (r.role_code = 'FINANCE_CASHIER'
            AND p.permission_code ~ '^(finance|receipt|payment):(receipt|payment|cash-transfer|pre-receipt|pre-payment|capital-flow):'
            AND p.permission_code ~ ':(create|update|submit|confirm|cancel)$')

        -- (e) 预算管理员：预算域全部动作
        OR (r.role_code = 'BUDGET_ADMIN' AND p.permission_code LIKE 'budget:%')
      )
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission x
                  WHERE x.role_id = r.id AND x.permission_id = p.id);

COMMIT;

-- ── 自检：各角色码数与 SoD 断言 ──
SELECT r.role_code,
       COUNT(*) AS codes,
       COUNT(*) FILTER (WHERE p.permission_code ~ ':(create|update|submit|delete|edit)$') AS 制单类,
       COUNT(*) FILTER (WHERE p.permission_code ~ ':(approve|audit|post|reverse)$')        AS 审批类
FROM sys_role r
JOIN sys_role_permission rp ON rp.role_id = r.id
JOIN sys_permission p ON p.id = rp.permission_id
WHERE r.role_code IN ('FINANCE_MANAGER', 'FINANCE_ACCOUNTANT', 'FINANCE_CASHIER', 'BUDGET_ADMIN')
GROUP BY r.role_code ORDER BY 1;
-- 期望：FINANCE_MANAGER 的「制单类」= 0；FINANCE_ACCOUNTANT 的「审批类」= 0
