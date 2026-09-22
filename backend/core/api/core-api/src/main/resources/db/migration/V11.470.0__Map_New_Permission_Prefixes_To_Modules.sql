-- 给 E-01 新批次引入的权限码前缀补「模块归属」映射（2026-09-21）
--
-- 【为什么必须补】`sys_module_permission` 是「模块 → 权限码前缀」的对照表，
--   模块 entitlement 门（`ModuleEntitlementInterceptor`）靠它把"接口需要的权限码"
--   解析成"该租户有没有开通这个模块"。新域名一旦没有映射行：
--     ① `tools/verify-module-mapping.cjs` 的「在役码必须 100% 有归属」断言会红；
--     ② 模块门对这批码**不生效**（解析不出模块 ⇒ 不拦），即"按模块关掉一个域"关不掉它们。
--   本轮 E-01 新批次引入了 6 个此前不存在的前缀，故在此登记。
--
-- 【归属口径（人工裁定，逐条给理由）】
--   · `signature:`   → **settings**（设置）。签收/评价与打印同属交付动作，代码同住
--                      `erp-printing` 模块，且 `print:` 前缀本就归 settings。
--   · `metrics:`     → **system**（平台/运维）。这是 ERP 侧的业务指标采集与查询，
--   · `monitor:`     → **system**   属可观测性/运维范畴；**不归 analytics** ——
--                      `verify-module-mapping.cjs` 有一条硬断言「analytics 映射的码数必须为 0」
--                      （分析模块整域尚无码族，是已知缺口），往那里塞码会让该断言失败，
--                      且会掩盖那个缺口。
--   · `pricing:`     → **master-data**（资料）。价格引擎/价格策略/等级价属主数据，
--                      与同族的 `product:grade:*`（也归 master-data）保持一致。
--   · `erp:batch:`   → **warehouse**（仓储）。批次号是库存主数据。
--   · `erp:serial:`  → **warehouse**。序列号同理。
--      （注意用**带资源的长前缀**而不是裸 `erp:`：裸前缀会连 `erp:expense:*`（财务域）
--        一起吃掉，把财务的码错划到仓储模块。）
--
-- 【sort】只用于「同长前缀并列」时的决胜；本批 6 个前缀互不重叠，取 150~155 不与他人冲突。
--
-- 幂等：`NOT EXISTS` 守卫，重复执行不产生第二行。
-- =============================================================

INSERT INTO sys_module_permission (tenant_id, deleted, module_code, permission_prefix, sort, remark, create_time, update_time)
SELECT 0, 0, v.module_code, v.prefix, v.sort, v.remark, now(), now()
FROM (VALUES
  ('settings',    'signature:',  150, 'E-01 批次登记：签收/评价（与打印同属交付动作，同住 erp-printing）'),
  ('system',      'metrics:',    151, 'E-01 批次登记：ERP 业务指标（可观测性，归系统模块；不归 analytics 以免破坏其"必须为 0"的缺口断言）'),
  ('system',      'monitor:',    152, 'E-01 批次登记：ERP 运行监控（同上）'),
  ('master-data', 'pricing:',    153, 'E-01 批次登记：价格引擎/策略/等级价（主数据，与 product:grade 同族）'),
  ('warehouse',   'erp:batch:',  154, 'E-01 批次登记：批次号（库存主数据）。用长前缀而非裸 erp:，避免吃掉 erp:expense:*（财务域）'),
  ('warehouse',   'erp:serial:', 155, 'E-01 批次登记：序列号（库存主数据）')
) AS v(module_code, prefix, sort, remark)
WHERE NOT EXISTS (
  SELECT 1 FROM sys_module_permission mp
  WHERE mp.deleted = 0 AND mp.module_code = v.module_code AND mp.permission_prefix = v.prefix
);
