-- 支付扩展域（core-payment）权限码种子（E-01 corepayment 批次，2026-09-21）
--
-- 【与"零码模块"种子的区别】core-payment **不是零码模块**：`payment:` 前缀在 sys_permission 里
--   已有 9 条（request/record/channel/config），但**只覆盖 PaymentController 与
--   PaymentConfigController**；本次要补的两个控制器（退款、对账）**一条码都没有**。
--   故本迁移只补缺的 10 个码，全部挂在既有 `payment:` 前缀下 —— **不新增一级前缀**，
--   也不需要新的模块归属映射（`payment:` → finance 早已登记）。
--   VALUES 占满 124000~124009 共 10 个槽位，实际落库 10 个。
--
-- 【为什么必须补】E-01 要给两个裸控制器补 @SaCheckPermission，而
--   **没有码就补注解 ⇒ 该接口对所有非超管一律 403**（本仓铁律/历史事故）。
--
-- 【口径要点】全部写在 `tools/gen-module-permission-seed.py` 的 `MODULES['corepayment']`：
--   · `/api/refund` 与 `/api/reconciliation` 的类级路径会被推成**新的一级域**
--     （`refund:` / `reconciliation:`）⇒ 用 base_overrides 显式归到 `payment:` 域，
--     资源分别为 `refund` / `reconciliation`。
--   · ⚠️ **不复用** `finance:reconciliation:*`（库中已有 5 条，api_path 全是
--     `/api/erp/finance/reconciliation/*`，是 **ERP 财务对账**）：本控制器对的是
--     **第三方支付渠道**与支付记录的账，同名不同物（同 PATH_OVERRIDE 里 payment 那条的先例）。
--   · ⚠️ 同样**不复用** `payment:request:*`：退款申请（/api/refund/request）与支付请求
--     （/api/payment/request）是两次不同的业务动作，共用码会让"能发起支付的人顺便能发起退款"。
--   · 动作词修正："处理对账差异"→update（不是 create）；"待对账日期"→list（不是 view/detail）。
--
-- 【本批有意排除的端点 —— 共 2 个，不加任何权限码】
--   · `POST /api/payment/callback/{channel}`、`POST /api/refund/callback/{channel}`：
--     渠道侧服务器回调入口，**没有 Sa-Token 会话**（PaymentController 的类注释明确写着
--     "加权限注解没有意义，它会先被登录拦截器挡下"）。正确修法是补服务间验签/防重放，
--     不是权限码、更不是白名单。
--
-- 【id 号段】权限码 124000 起（槽位 24）、角色关联 9740000 起，执行前实测两段均为空。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。
-- =============================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (124000, '支付对账详情', 'payment:reconciliation:detail', '/api/reconciliation/{id}', 'GET', 1200),
 (124001, '支付对账执行', 'payment:reconciliation:execute', '/api/reconciliation/execute', 'POST', 1201),
 (124002, '支付对账查询', 'payment:reconciliation:list', '/api/reconciliation/page', 'GET', 1202),
 (124003, '支付对账编辑', 'payment:reconciliation:update', '/api/reconciliation/{id}/handle', 'POST', 1203),
 (124004, '支付对账查看', 'payment:reconciliation:view', '/api/reconciliation/stat', 'GET', 1204),
 (124005, '支付退款申请审批', 'payment:refund:approve', '/api/refund/request/{id}/approve', 'POST', 1205),
 (124006, '支付退款申请新增', 'payment:refund:create', '/api/refund/request', 'POST', 1206),
 (124007, '支付退款申请详情', 'payment:refund:detail', '/api/refund/request/{id}', 'GET', 1207),
 (124008, '支付退款申请查询', 'payment:refund:list', '/api/refund/request/page', 'GET', 1208),
 (124009, '支付退款申请查看', 'payment:refund:view', '/api/refund/request/stat', 'GET', 1209)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9740000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 124000 AND 124010
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
