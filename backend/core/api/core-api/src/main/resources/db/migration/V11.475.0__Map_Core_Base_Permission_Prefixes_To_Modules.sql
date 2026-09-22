-- 给 E-01「core 底座」批次引入的权限码前缀补「模块归属」映射（2026-09-21）
--
-- 【为什么必须补】`sys_module_permission` 是「模块 → 权限码前缀」的对照表，
--   模块 entitlement 门（`ModuleEntitlementInterceptor`）靠它把"接口需要的权限码"
--   解析成"该租户有没有开通这个模块"。新前缀一旦没有映射行：
--     ① `tools/verify-module-mapping.cjs` 的「在役码必须 100% 有归属」断言会红
--        （实测：V11.471~V11.474 落库后未归属 **91 条** = 下面 6 个前缀的全部码）；
--     ② 模块门对这批码**不生效**（解析不出模块 ⇒ 不拦），即"按模块关掉一个域"关不掉它们。
--   本轮新批次引入了 6 个此前不存在的前缀，故在此登记。
--
-- 【归属口径（人工裁定，逐条给理由）】
--   · `quality:` → **warehouse**（仓储）。质量管理（质检单/质量标准/合格证/缺陷记录）在菜单里
--                      就挂在「仓储 → 质量管理」（sys_menu 60311 的 parent 是 60003 仓储），
--                      质检本身就是收货/入库作业的一环。
--   · `trade:`   → **trade**（交易）。这 20 条码挂在 `/api/trade/*`（渠道配置/外部订单/
--                      库存同步/接口监控），对应菜单「交易 → 外部平台 / API监控」
--                      （61511 / 61506 的 parent 是 60015 交易）。注意**不要**因为该模块
--                      已有 `mall:` 前缀就另找模块 —— 交易模块本就同时含"商城"与"外部平台"两块。
--   · `notification:`
--                → **settings**（设置）。通知模板/通知统计/事件钩子与 `workflow:` / `print:` /
--   · `automation:`    `signature:` 同族：都是**租户级的平台能力**，一直归「设置」。
--   · `custom-field:`  自定义字段 / 自动化规则 / 看板 / 通知服务都是租户自己配置的通用能力。
--   · `kanban:`
--   ✅ **刻意不归 `system`**：①「系统」是**平台级**模块，用户已裁定只开给系统租户（迁移
--      V11.454.0 的回填口径 + `verify-module-mapping.cjs` 第⑥节断言"业务租户(2) 拥有 12 个模块
--      = 13 减去系统"）⇒ 把租户级功能归到 system 会让**业务租户被模块门整块拦掉**；
--      ② 归「设置」后，业务租户 2 已开通该模块（同为第⑥节断言），行为是"现状不降级"。
--
-- 【为什么用新前缀而不是并进 `system:` / `set:`】`system:` 的码只给系统租户（已拍板），
--   租户级功能不能借用；`set:` 是"设置"模块的**页面级**能力前缀（set:print-config 等），
--   把可独立开关的四个功能面（自定义字段/自动化/看板/通知）塞进 `set:` 会让模块门
--   无法按功能粒度开关。故四个域各自成前缀，但归属同一模块。
--
-- 【sort】只用于「同长前缀并列」时的决胜；本批 6 个前缀互不重叠，取 156~161 续 V11.470.0 的 155。
--
-- 幂等：`NOT EXISTS` 守卫，重复执行不产生第二行。
-- =============================================================

INSERT INTO sys_module_permission (tenant_id, deleted, module_code, permission_prefix, sort, remark, create_time, update_time)
SELECT 0, 0, v.module_code, v.prefix, v.sort, v.remark, now(), now()
FROM (VALUES
  ('warehouse', 'quality:',      156, 'E-01 core 底座批次登记：质量管理（菜单挂在「仓储 → 质量管理」，属仓储作业）'),
  ('trade',     'trade:',        157, 'E-01 core 底座批次登记：外部渠道/外部订单/库存同步/接口监控（菜单在「交易 → 外部平台 / API监控」）'),
  ('settings',  'notification:', 158, 'E-01 core 底座批次登记：通知模板/统计/事件钩子（租户级平台能力，与 workflow:/print: 同族归设置）'),
  ('settings',  'automation:',   159, 'E-01 core 底座批次登记：自动化规则（同上）。不归 system —— 该模块只开给系统租户，归它会让业务租户整块 403'),
  ('settings',  'custom-field:', 160, 'E-01 core 底座批次登记：自定义字段（同上）'),
  ('settings',  'kanban:',       161, 'E-01 core 底座批次登记：看板（同上）')
) AS v(module_code, prefix, sort, remark)
WHERE NOT EXISTS (
  SELECT 1 FROM sys_module_permission mp
  WHERE mp.deleted = 0 AND mp.module_code = v.module_code AND mp.permission_prefix = v.prefix
);
