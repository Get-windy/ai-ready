-- 给 E-01 最后一批（b2b / supplier / deliveryroute / partnerlevel / mktanalytics /
-- stockextra）引入的权限码前缀补「模块归属」映射（2026-09-21）
--
-- 【为什么必须补】`sys_module_permission` 是「模块 → 权限码前缀」的对照表，
--   模块 entitlement 门（`ModuleEntitlementInterceptor`）靠它把"接口需要的权限码"
--   解析成"该租户有没有开通这个模块"。新域名一旦没有映射行：
--     ① `tools/verify-module-mapping.cjs` 的「在役码必须 100% 有归属」断言会红；
--     ② 模块门对这批码**不生效**（解析不出模块 ⇒ 不拦），即"按模块关掉一个域"关不掉它们。
--
-- 【本批只新增 2 个一级前缀】其余 6 个域全部落在**既有**前缀上，无需登记：
--   · `mall:`（b2b，库里已有 mall:tag:*，V11.454.0 已登记 → trade）
--   · `party:`（客户等级 party:customer-level:*，与 party:customer-region:* 同族 → master-data）
--   · `marketing:`（提成分析 marketing:commission-analytics:* → marketing）
--   · `set:`（期初库存 set:initial-stock:*，与 set:initial-finance:* 同族 → settings）
--   · `stock:`（进销存分析 stock:analytics:list → warehouse）
--   · `md:`（线路档案 md:route-master:* → master-data）
--
-- 【归属口径（人工裁定，逐条给理由）】
--   · `supplier:`  → **master-data**（资料）。供应商是主数据对象：档案页的菜单码就是
--                     `md:supplier`（资料 → 供应商），与 `md:customer` / `party:*` 同族。
--                     ⚠️ 该模块还含"供应商询价单/绩效评估"（页面挂在采购菜单
--                     `purchase:supplier-inquiry` / `purchase:supplier-performance` 下），
--                     但那些端点操作的是**供应商自身的属性**（等级/绩效/积分/询价记录），
--                     与档案同住一个 Maven 模块与同一个 `supplier:` 域；拆成两个模块前缀
--                     会让"资料"与"采购"各关一半、说不清关掉了什么。故整域归资料。
--   · `delivery:`  → **dms**（配送）。配送路线单（执行单）是配送作业的一部分，
--                     前端菜单 80700 就在「配送 → 配送路线」下（菜单码 `dms:route-list`）。
--                     与既有的 `dms:route:{update,view}`（`/api/dms/route/plan|geocode`，
--                     路线规划）**同模块、不同前缀**：不复用那两条码是因为它们指的是
--                     另一件事（规划/地理编码），复用会让"配送路线单的查询"和"路线规划"
--                     共用一个开关。
--
-- 【sort】只用于「同长前缀并列」时的决胜；本批 2 个前缀互不重叠，取 162~163 续 V11.475.0 的 161。
--   ⚠️ 特意检查过歧义：`delivery:`（9 字符）与既有 `dms:` / `doc:` / `log:` 等**不同长**，
--     不会被"同长前缀指向不同模块"的断言判为歧义。
--
-- 幂等：`NOT EXISTS` 守卫，重复执行不产生第二行。
-- =============================================================

INSERT INTO sys_module_permission (tenant_id, deleted, module_code, permission_prefix, sort, remark, create_time, update_time)
SELECT 0, 0, v.module_code, v.prefix, v.sort, v.remark, now(), now()
FROM (VALUES
  ('master-data', 'supplier:', 162, 'E-01 最后一批登记：供应商档案/等级/绩效/询价报价/积分（资料域主数据，菜单 md:supplier）'),
  ('dms',         'delivery:', 163, 'E-01 最后一批登记：配送路线单（执行单），与 dms:route:*（路线规划）分开——同一模块，不同前缀')
) AS v(module_code, prefix, sort, remark)
WHERE NOT EXISTS (
  SELECT 1 FROM sys_module_permission mp
  WHERE mp.deleted = 0 AND mp.module_code = v.module_code AND mp.permission_prefix = v.prefix
);
