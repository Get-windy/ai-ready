-- 提成分析（marketing:commission-analytics）权限码种子（E-01 mktanalytics 批次，2026-09-21）
--
-- 【本批范围】`erp-marketing` 的 `cn.aiedge.erp.marketing.analytics` 包 —— 在子包里，
--   从未被 marketing 批次覆盖（那批的 dirs 是 `.../marketing/controller`）。
--
-- 【交付判断】控制器方法体用 `StpUtil.isLogin() / getLoginId()` 取操作人 ⇒ 依赖会话，
--   是后台功能，正常补码。
--
-- 【域口径】类级路径推导出 `marketing:commission-analytics`，与库中既有的
--   `marketing:commission-rule:*` / `marketing:commission-record:*`（同住 commission 子路径）同族。
--   ⚠️ 前端菜单码是 `ana:commission-center`（分析中心），**不许**照它造 `ana:` / `analytics:`
--   前缀：那会撞 `verify-module-mapping.cjs` 的「analytics 映射数必须为 0」硬断言（已知缺口）。
--
-- 【只补 2 个码 + 复用 1 个既有码】本文件 7 端点 → 3 码：
--   · 新增 `marketing:commission-analytics:list`（5 个 `GET …/page`）与
--     `marketing:commission-analytics:settle`（`POST /settle` 批量结算；`settle` 为本批新增动作词，
--     不并进 update —— 让"能编辑提成配置"与"能结算"可以分开授权）；
--   · **复用** `marketing:commission-rule:list` 给 `GET /plans`：它读的就是 `erp_commission_rule`
--     表（`CommissionAnalyticsServiceImpl#planList`），与提成规则页是同一个对象 ⇒ 不另造同义码。
--     该码已在库，本文件的 NOT EXISTS 会跳过它，**实际新增只有 2 条**（id 号段内留一个空位）。
--
-- 【id 号段】权限码 **129000 起**（slot=29）、角色关联 **9790000 起** —— 执行前实测两段 count(*) = 0。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。
-- =============================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (129000, '营销提成分析查询', 'marketing:commission-analytics:list', '/api/erp/marketing/commission/analytics/rider-matrix/page', 'GET', 1200),
 (129001, '营销提成分析结算', 'marketing:commission-analytics:settle', '/api/erp/marketing/commission/analytics/settle', 'POST', 1201),
 (129002, '营销commission-rule查询', 'marketing:commission-rule:list', '/api/erp/marketing/commission/analytics/plans', 'GET', 1202)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9790000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 129000 AND 129003
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
