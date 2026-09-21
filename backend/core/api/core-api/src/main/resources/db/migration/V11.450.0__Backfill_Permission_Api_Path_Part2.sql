-- ═══════════════════════════════════════════════════════════════════════
-- E-03 续：为「已有码但 api_path 为空」的 47 条补上「权限码 ↔ 接口」映射（2026-09-21）
--
-- 【这些码从哪来】它们是**历史手写码**（不是 E-04 生成器产出），集中在
--   crm / dms / finance(erp-finance 其余部分) / sale / purchase / md 六个域。
--   E-04 那批 485 个新码在建码时就顺带写了 api_path，所以剩下没写的就是这批老码。
--
-- 【值是怎么得到的】用 `tools/gen-module-permission-seed.py` 的**路径推导规则**
--   扫控制器得出「码 → 端点」，再**只**回填那些「推导出的码在库中存在」的条目
--   （其余推导结果不会写库 —— 见下方"为什么只回填 47 条"）。
--
-- 【为什么只回填 47 条】实测这几个域的**推导码与库中历史码逐字不符**：
--     hr        92 端点 → 推导 18 码，命中库中已有 8
--     crm      185 端点 → 推导 67 码，命中 8
--     core-api 530 端点 → 推导 242 码，命中 16
--     erp-finance 405 端点 → 推导 216 码，命中 85
--     sale/purchase/dms 同理（命中 10 / 18 / 13）
--   原因是**历史码粒度更细**（`hr:leave:approve`、`crm:contract:batchapprove`），
--   而推导规则只能给到 `<域>:<资源>:<动作>`（`hr:update`、`crm:contract:update`）。
--   两者不可自动对齐 ⇒ 只回填能对齐的，**不伪造**未对齐的映射。
--
-- 【为什么安全】`sys_permission.api_path` 的唯一消费方是
--   `PermissionServiceImpl.checkApiPermission`（只经 `PermissionController` 暴露、
--   无内部调用方，且 fail-closed）；本迁移**只改元数据**，不新增码、不动角色授权、
--   不动任何 `@SaCheckPermission` 注解 ⇒ 不改变任何接口的可访问性。
--
-- 【幂等】每条都带 `AND (api_path IS NULL OR api_path = '')`，重复执行不改变已填行。
-- ═══════════════════════════════════════════════════════════════════════

-- ── CRM ──
UPDATE sys_permission SET api_path = '/api/crm/contract/{id}/approve', method = 'POST' WHERE permission_code = 'crm:contract:approve' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/crm/contract', method = 'POST' WHERE permission_code = 'crm:contract:create' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/crm/contract/expiring', method = 'GET' WHERE permission_code = 'crm:contract:view' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/crm/lead', method = 'POST' WHERE permission_code = 'crm:lead:create' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/crm/opportunity', method = 'POST' WHERE permission_code = 'crm:opportunity:create' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/crm/opportunity/statistics', method = 'GET' WHERE permission_code = 'crm:opportunity:view' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/crm/quotation', method = 'POST' WHERE permission_code = 'crm:quotation:create' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/crm/quotation/{id}/versions', method = 'GET' WHERE permission_code = 'crm:quotation:view' AND (api_path IS NULL OR api_path = '');

-- ── DMS ──
UPDATE sys_permission SET api_path = '/api/dms/channel', method = 'POST' WHERE permission_code = 'dms:channel:create' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/dms/channel/{id}', method = 'DELETE' WHERE permission_code = 'dms:channel:delete' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/dms/channel/{id}', method = 'PUT' WHERE permission_code = 'dms:channel:update' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/dms/config/{key}', method = 'PUT' WHERE permission_code = 'dms:config:update' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/dms/dispatch/strategy', method = 'GET' WHERE permission_code = 'dms:dispatch:view' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/dms/rider/{id}/approve', method = 'POST' WHERE permission_code = 'dms:rider:approve' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/dms/rider', method = 'POST' WHERE permission_code = 'dms:rider:create' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/dms/rider/{id}', method = 'DELETE' WHERE permission_code = 'dms:rider:delete' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/dms/rider/{id}', method = 'PUT' WHERE permission_code = 'dms:rider:update' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/dms/sign/submit', method = 'POST' WHERE permission_code = 'dms:sign:submit' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/dms/vehicle', method = 'POST' WHERE permission_code = 'dms:vehicle:create' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/dms/vehicle/{id}', method = 'DELETE' WHERE permission_code = 'dms:vehicle:delete' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/dms/vehicle/{id}', method = 'PUT' WHERE permission_code = 'dms:vehicle:update' AND (api_path IS NULL OR api_path = '');

-- ── 财务域（erp-finance 其余部分）──
UPDATE sys_permission SET api_path = '/api/erp/finance/auxiliary/type', method = 'POST' WHERE permission_code = 'finance:auxiliary:create' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/erp/finance/auxiliary/type/{id}', method = 'DELETE' WHERE permission_code = 'finance:auxiliary:delete' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/erp/finance/auxiliary/type/{id}', method = 'PUT' WHERE permission_code = 'finance:auxiliary:update' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/erp/finance/collection-stats', method = 'GET' WHERE permission_code = 'finance:collection-stats:view' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/erp/finance/payable', method = 'POST' WHERE permission_code = 'finance:payable:create' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/erp/finance/payable/batch', method = 'DELETE' WHERE permission_code = 'finance:payable:delete' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/erp/finance/payable/aging', method = 'GET' WHERE permission_code = 'finance:payable:view' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/erp/finance/receivable/aging', method = 'GET' WHERE permission_code = 'finance:receivable:view' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/erp/finance/subject', method = 'POST' WHERE permission_code = 'finance:subject:create' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/erp/finance/subject/{id}', method = 'DELETE' WHERE permission_code = 'finance:subject:delete' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/erp/finance/tax/declaration', method = 'POST' WHERE permission_code = 'finance:tax:create' AND (api_path IS NULL OR api_path = '');
-- 注：下面这条的路径里 "tax/tax" 不是笔误 —— 该控制器的类级路径是 /api/erp/finance/tax，
-- 而方法上是 @GetMapping("/tax/payable")，Spring 拼出来就是 /api/erp/finance/tax/tax/payable
UPDATE sys_permission SET api_path = '/api/erp/finance/tax/tax/payable', method = 'GET' WHERE permission_code = 'finance:tax:view' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/erp/finance/transaction', method = 'POST' WHERE permission_code = 'finance:transaction:create' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/erp/finance/transaction/statistics', method = 'GET' WHERE permission_code = 'finance:transaction:view' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/erp/finance/voucher', method = 'POST' WHERE permission_code = 'finance:voucher:create' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/erp/finance/voucher/batch', method = 'DELETE' WHERE permission_code = 'finance:voucher:delete' AND (api_path IS NULL OR api_path = '');

-- ── 主数据（支付渠道）/ 采购 / 销售 ──
UPDATE sys_permission SET api_path = '/api/erp/md/payment-channel/import-template', method = 'GET' WHERE permission_code = 'md:payment-channel:view' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/erp/purchase/cost-sharing', method = 'POST' WHERE permission_code = 'purchase:cost-sharing:create' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/erp/purchase/cost-sharing/{id}', method = 'GET' WHERE permission_code = 'purchase:cost-sharing:detail' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/erp/purchase/cost-sharing/page', method = 'GET' WHERE permission_code = 'purchase:cost-sharing:list' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/erp/purchase/cost-sharing/{id}', method = 'PUT' WHERE permission_code = 'purchase:cost-sharing:update' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/erp/sale/order/page', method = 'GET' WHERE permission_code = 'sale:order:list' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/sale/promotion', method = 'POST' WHERE permission_code = 'sale:promotion:create' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/sale/promotion/{id}', method = 'DELETE' WHERE permission_code = 'sale:promotion:delete' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/sale/promotion/{id}/publish', method = 'POST' WHERE permission_code = 'sale:promotion:publish' AND (api_path IS NULL OR api_path = '');
UPDATE sys_permission SET api_path = '/api/sale/promotion/{id}', method = 'PUT' WHERE permission_code = 'sale:promotion:update' AND (api_path IS NULL OR api_path = '');
