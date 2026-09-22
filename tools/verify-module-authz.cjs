#!/usr/bin/env node
/**
 * 模块鉴权注解验证（E-01 / E-04）· 重启后运行（2026-09-21）
 *
 * 用途：验证「先补权限码种子 → 再补 @SaCheckPermission」这一对改动的**运行时**效果。
 *
 * ⚠️ 为什么必须跑真机：补注解有两类静默失败，编译期完全看不出来 ——
 *   ① **码不在库中** ⇒ 该接口对**所有非超管一律 403**（含本应有权限的角色）；
 *   ② 注解**没真正插到方法上**（如建在错误位置）⇒ 看起来改了、实际没生效，**静默 fail-open**。
 *   只测「超管返回 200」两条都发现不了（超管走 ["*"] 通配）——
 *   必须同时断言**非超管被拒**（证明注解生效）与**超管放行**（证明码存在）。
 *
 * 用法：
 *   node tools/verify-module-authz.cjs budget
 *   node tools/verify-module-authz.cjs budget --port 5655
 *
 * 只读验证：仅发 GET / 无副作用的探测请求；control:check 用业务上必然被拒的入参，
 * 目的是观察**鉴权层**是否放行，不实际改动预算数据。
 */
const fs = require('fs')
const path = require('path')

const moduleName = process.argv[2]
if (!moduleName) {
  console.error('用法: node tools/verify-module-authz.cjs <module>')
  process.exit(2)
}
const portArg = process.argv.indexOf('--port')
const PORT = portArg > -1 ? process.argv[portArg + 1] : (process.env.PORT || '5655')
const BASE = `http://localhost:${PORT}/api`

const ADMIN = { u: 'admin', p: 'admin123', t: '系统租户' }
// 租户 2 的非超管账号：无任何 budget:* 码
const OUTSIDER = { u: 'e2e_hr_t2', p: 'admin123', t: 'E2E验收租户2' }

// 每个模块的探测端点：至少覆盖「读」与「写」各一个资源，
// 证明注解是按方法粒度插的，而不是只落在某一个上。
const PROBES = {
  budget: [
    { m: 'GET', p: '/erp/budget/annual/page?current=1&size=1', code: 'budget:annual:list' },
    { m: 'GET', p: '/erp/budget/adjustment/page?current=1&size=1', code: 'budget:adjustment:list' },
    { m: 'GET', p: '/erp/budget/template/page?current=1&size=1', code: 'budget:template:list' },
    { m: 'GET', p: '/erp/budget/execution/rows', code: 'budget:execution:list' },
    { m: 'GET', p: '/erp/budget/report/trend', code: 'budget:report:view' },
    { m: 'GET', p: '/erp/budget/item/list-by-budget/1', code: 'budget:item:list' },
    // 写接口：用一个必然不存在的预算去校验，观察是否停在鉴权层（期望超管非 403）
    { m: 'POST', p: '/erp/budget/control/check', code: 'budget:control:check',
      body: { budgetId: -1, amount: 1 } },
  ],
  // ⚠️ 以下 p 是**不含 /api 前缀**的路径（BASE 里已含 /api）。
  //    路径与码均取自 `tools/gen-module-permission-seed.py <模块>` 的真实产出，不要手猜 ——
  //    猜错探针会把「入参缺失 400」误判成失败（本项目踩过一次）。
  fixedasset: [
    { m: 'GET', p: '/erp/fixed-asset/asset/page', code: 'fixed-asset:asset:list' },
    { m: 'GET', p: '/erp/fixed-asset/category/list', code: 'fixed-asset:category:list' },
  ],
  stock: [
    { m: 'GET', p: '/erp/stock/in/page', code: 'stock:in:list' },
    { m: 'GET', p: '/erp/stock/take/page', code: 'stock:take:list' },
    { m: 'GET', p: '/erp/product-category/tree', code: 'product:category:list' },
    { m: 'GET', p: '/erp/md/image/page', code: 'md:image:list' },
  ],
  invoice: [
    { m: 'GET', p: '/erp/invoice/application/list', code: 'invoice:application:list' },
  ],
  payment: [
    { m: 'GET', p: '/erp/capital-flow/page', code: 'finance:capital-flow:list' },
    // 2026-09-21 域名归一：`/api/erp/payment`（ERP 付款单）原落到 `payment:*`，
    // 与历史「第三方支付网关」域同名不同物，已改名 `finance:payment:*`
    { m: 'GET', p: '/erp/payment/page', code: 'finance:payment:list' },
  ],
  party: [
    { m: 'GET', p: '/erp/contact/page', code: 'party:contact:list' },
    // 2026-09-21 域名归一：`/api/erp/partner/*` 原落到 `partner:*`，与 `/api/erp/party` 是
    // 同一业务对象被拆到两个域，已统一到 `party:*`
    { m: 'GET', p: '/erp/partner/roles/page', code: 'party:roles:list' },
  ],
  marketing: [
    { m: 'GET', p: '/erp/marketing/addon-rule/page', code: 'marketing:addon-rule:list' },
    { m: 'GET', p: '/erp/marketing/auto-campaign/page', code: 'marketing:auto-campaign:list' },
  ],
  // 2026-09-21 E-01 crm 批次（185 端点 / 10 控制器）。
  // ⚠️ **故意不探 contract**：E2E_T2_ADMIN 这个"非超管"账号被
  // `tools/verify-crm-tenant-fix.cjs` 用着，且为了让那个脚本能验证"租户 2 建的合同
  // 落进租户 2"，已给它授予 `crm:contract:create` / `crm:contract:view`
  // ⇒ 它对 contract 探针会返回 200，当成"应当被拒"的样本会误报。
  // contract 那两条码的**双向证据**改由 `verify-crm-tenant-fix.cjs` 承担：
  // create/view 放行 + 未授的 approve 被拒。
  crm: [
    { m: 'GET', p: '/customer/page', code: 'crm:customer:list' },
    { m: 'GET', p: '/crm/followUp/page', code: 'crm:follow-up:list' },
    { m: 'GET', p: '/crm/lead/page', code: 'crm:lead:view' },
    { m: 'GET', p: '/crm/opportunity/page', code: 'crm:opportunity:view' },
    { m: 'GET', p: '/crm/customer-pool/page', code: 'crm:customer-pool:list' },
    { m: 'GET', p: '/crm/marketing/page', code: 'crm:marketing:list' },
    { m: 'GET', p: '/crm/quotation/page', code: 'crm:quotation:view' },
    { m: 'GET', p: '/crm/quotation-template/page', code: 'crm:quotation-template:list' },
    { m: 'GET', p: '/crm/visit/plan/page', code: 'crm:visit:list' },
  ],
  // 2026-09-21 E-01 wms 批次（20 个控制器 / 162 端点补注解，10 个端点按配置排除）。
  // 每个资源取一个读端点。⚠️ 探针**不能**取这几个（它们被显式排除、本来就不该有权限码）：
  //   /v1/warehouse/auth/login、/v1/warehouse/auth/logout、
  //   /erp/wms/**（ErpCallbackController）、/wms/erp/**（ErpIntegrationController）。
  // 2026-09-21 E-01 erpfinance 批次（18 个控制器 / 124 处注解，新增迁移 V11.463.0 补 72 个码）。
  // 路径取自迁移 `api_path` 列。
  erpfinance: [
    { m: 'GET', p: '/erp/finance/account/page', code: 'finance:account:list' },
    { m: 'GET', p: '/erp/finance/bank-account/page', code: 'finance:bank-account:list' },
    { m: 'GET', p: '/erp/finance/cash-transfer/page', code: 'finance:cash-transfer:list' },
    { m: 'GET', p: '/erp/finance/ar-ap-adjust/page', code: 'finance:ar-ap-adjust:list' },
    { m: 'GET', p: '/erp/finance/expense-doc/page', code: 'finance:expense-doc:list' },
    { m: 'GET', p: '/erp/finance/expense-approval/pending', code: 'finance:expense-approval:view' },
    { m: 'GET', p: '/erp/finance/expense-stats/summary', code: 'finance:expense-stats:view' },
    { m: 'GET', p: '/erp/finance/analytics/collection-stats/page', code: 'finance:analytics-collection-stats:list' },
    { m: 'GET', p: '/erp/expense/type/list', code: 'erp:expense:type:list' },
    { m: 'GET', p: '/erp/md/expense-type/tree', code: 'md:expense-type:list' },
    { m: 'GET', p: '/erp/md/other-income/page', code: 'md:other-income:list' },
  ],
  // 2026-09-21 E-01 dms 批次（20 个控制器 / 210 处注解，新增迁移 V11.462.0 补 80 个码）。
  // 路径全部取自迁移 `api_path` 列。⚠️ 不探 `/dms/channel/callback`（白名单、无会话，有意不加码）。
  dms: [
    { m: 'GET', p: '/dms/channel/page', code: 'dms:channel:list' },
    { m: 'GET', p: '/dms/config/list', code: 'dms:config:list' },
    { m: 'GET', p: '/dms/dashboard/stats', code: 'dms:dashboard:view' },
    { m: 'GET', p: '/dms/order-pool/page', code: 'dms:order-pool:list' },
    { m: 'GET', p: '/dms/payment/page', code: 'dms:payment:list' },
    { m: 'GET', p: '/dms/rider/page', code: 'dms:rider:list' },
    { m: 'GET', p: '/dms/route/fence/page', code: 'dms:route-fence:list' },
    { m: 'GET', p: '/dms/settlement/rule/page', code: 'dms:settlement:list' },
    { m: 'GET', p: '/dms/sign/page', code: 'dms:sign:list' },
    { m: 'GET', p: '/dms/task/next-no', code: 'dms:task:list' },
    { m: 'GET', p: '/dms/tracking/page', code: 'dms:tracking:list' },
    { m: 'GET', p: '/dms/vehicle/page', code: 'dms:vehicle:list' },
    { m: 'GET', p: '/dms/vehicle/energy/page', code: 'dms:vehicle-energy:list' },
    { m: 'GET', p: '/dms/vehicle/maintenance/page', code: 'dms:vehicle-maintenance:list' },
    { m: 'GET', p: '/dms/verification/binding/page', code: 'dms:verification:list' },
    { m: 'GET', p: '/dms/event/pending', code: 'dms:event:view' },
  ],
  // 2026-09-21 E-01 sales 批次（19 个控制器 / 197 处注解，新增迁移 V11.461.0 补 95 个码）。
  // 路径取自迁移 `api_path` 列（不是手猜 —— 猜错会把「入参缺失 400」误判成失败）。
  sales: [
    { m: 'GET', p: '/erp/sale/order/page', code: 'sale:order:list' },
    { m: 'GET', p: '/erp/sale/outbound/page', code: 'sale:outbound:list' },
    { m: 'GET', p: '/sales/retail/page/doc', code: 'sale:retail:list' },
    { m: 'GET', p: '/sales/retail/shift/page', code: 'sale:retail-shift:list' },
    { m: 'GET', p: '/erp/sale/return/page', code: 'sale:return:list' },
    { m: 'GET', p: '/erp/sale/return-doc/page', code: 'sale:return-doc:list' },
    { m: 'GET', p: '/erp/sale/exchange/page', code: 'sale:exchange:list' },
    { m: 'GET', p: '/erp/sale/pre-order/page', code: 'sale:pre-order:list' },
    { m: 'GET', p: '/erp/sale/pre-order/analysis/page', code: 'sale:pre-order-analysis:list' },
    { m: 'GET', p: '/erp/sale/logistics/shipment-notify/page', code: 'sale:logistics:list' },
    { m: 'GET', p: '/erp/sale/analysis/customer-active/page', code: 'sale:analysis:list' },
    { m: 'GET', p: '/erp/sale/analysis/promotion-funnel/page', code: 'sale:analysis-promotion-funnel:list' },
    { m: 'GET', p: '/erp/product-kit/page', code: 'product:kit:list' },
    { m: 'GET', p: '/erp/finance/account-delivery/doc-page', code: 'finance:account-delivery:view' },
  ],
  // 2026-09-21 E-01 purchase 批次（10 个控制器 / 97 处注解，新增迁移 V11.460.0 补 69 个码）
  purchase: [
    { m: 'GET', p: '/erp/purchase/order/page', code: 'purchase:order:list' },
    { m: 'GET', p: '/erp/purchase/inbound/page', code: 'purchase:inbound:list' },
    { m: 'GET', p: '/erp/purchase/return/page', code: 'purchase:return:list' },
    { m: 'GET', p: '/erp/purchase/exchange/page', code: 'purchase:exchange:list' },
    { m: 'GET', p: '/erp/purchase/inquiry/page', code: 'purchase:inquiry:list' },
    { m: 'GET', p: '/erp/purchase/contract/page', code: 'purchase:contract:list' },
    { m: 'GET', p: '/erp/purchase/cost-sharing/page', code: 'purchase:cost-sharing:list' },
    { m: 'GET', p: '/erp/purchase/analytics/page', code: 'purchase:analytics:list' },
    { m: 'GET', p: '/purchase/price-track/page', code: 'purchase:price-track:list' },
    { m: 'GET', p: '/purchase/doc-query/page', code: 'purchase:doc-query:list' },
    // 订单的两个子查询控制器：资源名归并到 order，故与主订单共用一个码
    { m: 'GET', p: '/erp/purchase/order/detail-query/page', code: 'purchase:order:list' },
  ],
  wms: [
    { m: 'GET', p: '/wms/receipt/page', code: 'wms:receipt:list' },
    { m: 'GET', p: '/wms/putaway/page', code: 'wms:putaway:list' },
    { m: 'GET', p: '/wms/pick/wave/page', code: 'wms:pick:list' },
    { m: 'GET', p: '/wms/ship/task/page', code: 'wms:ship:list' },
    { m: 'GET', p: '/wms/move/page', code: 'wms:move:list' },
    { m: 'GET', p: '/wms/check/page', code: 'wms:check:list' },
    { m: 'GET', p: '/wms/inventory/page', code: 'wms:inventory:list' },
    { m: 'GET', p: '/wms/location/page', code: 'wms:location:list' },
    { m: 'GET', p: '/wms/warehouse/page', code: 'wms:warehouse:list' },
    { m: 'GET', p: '/wms/borrow/page', code: 'wms:borrow:list' },
    { m: 'GET', p: '/wms/event/outbox/page', code: 'wms:event:list' },
  ],
  // 2026-09-21 E-01 core-api 批次（38 个控制器 / 302 处注解，新增迁移 V11.464.0 补 97 个码、
  // 复用 21 个既有码）。路径取自迁移 `api_path` 列（不是手猜 —— 猜错会把「入参缺失 400」
  // 误判成失败）。每个有码的资源取一个**读**端点。
  // ⚠️ 探针**不能**取这些（它们被有意排除、本来就不该有权限码）：
  //   /dashboard/**、/profile/**、/dict/{code}、/dict/batch、/dict/item/code/{dictCode}、
  //   /file/view/**、/file/upload、/department/list|tree|options、/user-permission/current/**、
  //   /tenant/current、/admin/fix/**、/integration/webhook/{configId}、
  //   /tenant-registration/register、/feedback/submit*、/feedback/my/**。
  // ⚠️ `system:tenant:update`（PUT /tenant/{id}/config）没有读端点，且它是**写**接口
  //   （发了就是真改租户配置），故不做探针 —— 该码的"存在性"由迁移与库比对保证。
  // ⚠️ 下面这些资源**不做探针**，因为它们的控制器在
  //   `src/test/resources/known-unscanned-controller-packages.txt` 里 —— 没进
  //   `scanBasePackages` 扫描闭包，运行时**端点一律 404**（实测：超管/非超管都 404，
  //   不是本批造成的）：assistant、feedback、gateway、knowledge、mq、mq-enhanced、
  //   recommendation、report、report-schedule、report-analytics、search、storage。
  //   它们的码照建（控制器一旦接线即生效），但运行时无从验证，测不出"403 生效"。
  coreapi: [
    { m: 'GET', p: '/config/map', code: 'system:config:list' },
    { m: 'GET', p: '/cache/status', code: 'system:cache:view' },
    { m: 'GET', p: '/export/template/customer', code: 'system:dataexport:view' },
    { m: 'GET', p: '/v1/sync-history', code: 'system:dataimport:list' },
    { m: 'GET', p: '/dict/item/tree?dictTypeId=1', code: 'system:dict:list' },
    { m: 'GET', p: '/import/datatypes', code: 'system:import:view' },
    { m: 'GET', p: '/import-templates', code: 'system:import-template:view' },
    { m: 'GET', p: '/integration/configs', code: 'system:integration:view' },
    { m: 'GET', p: '/monitor/overview', code: 'system:monitor:view' },
    { m: 'GET', p: '/monitor/alerts/rules', code: 'system:monitor-alert:view' },
    { m: 'GET', p: '/monitor/health/status', code: 'system:monitor-health:view' },
    { m: 'GET', p: '/monitor/infrastructure/server/info', code: 'system:monitor-infrastructure:view' },
    { m: 'GET', p: '/monitor/performance/realtime', code: 'system:monitor-performance:view' },
    { m: 'GET', p: '/tenant-module/valid-codes', code: 'system:tenant-module:view' },
    { m: 'GET', p: '/docquery/business-history/page', code: 'doc:docquery:list' },
    // 复用既有码的三条：证明 code_rules 把裸端点接到了历史码上，而不是另建同义码。
    { m: 'GET', p: '/department/page?current=1&size=1', code: 'tenant-admin:department:list' },
    { m: 'GET', p: '/position/page?current=1&size=1', code: 'tenant-admin:position:list' },
    { m: 'GET', p: '/workflow/pending', code: 'workflow:task:view' },
  ],
  // 2026-09-21 E-01 erpprinting 批次（v1+v2 打印 12 控制器 / 82 处注解 + 签收评价 12 处，
  // 新增迁移 V11.465.0 补 61 个码、复用 1 个既有码）。路径取自迁移 `api_path` 列。
  // ⚠️ 探针**不能**取这些（有意排除、本来就不该有权限码）：
  //   /v2/print/client/**（Windows 打印客户端 auth_key 设备侧 API，无用户会话）、
  //   /v2/print/client/auth/**（客户端登录/注册/自省/登出）。
  // ⚠️ 有码但**不做探针**的资源：print:format:*（只有 3 个纯计算 POST）、
  //   print:message:send（「发送消息，功能即将开放」的写端点）—— 无读端点。
  erpprinting: [
    { m: 'GET', p: '/v1/print/logs', code: 'print:log:list' },
    { m: 'GET', p: '/v1/print/logs/statistics', code: 'print:log:view' },
    { m: 'GET', p: '/v1/print/tasks', code: 'print:task:list' },
    { m: 'GET', p: '/v1/print/tasks/queue/length', code: 'print:task:view' },
    { m: 'GET', p: '/v1/print/templates', code: 'print:template:list' },
    { m: 'GET', p: '/v1/print/printers', code: 'print:printer:list' },
    { m: 'GET', p: '/v1/print/printers/groups', code: 'print:printer-group:list' },
    { m: 'GET', p: '/v2/print/chains', code: 'print:chain:list' },
    { m: 'GET', p: '/v2/print/clients', code: 'print:client:list' },
    // ScreenshotController 的每个方法都声明了 `@RequestHeader Long tenantId`（必填）
    // ⇒ 不补这个头会被 Spring 判 500（MissingRequestHeaderException），与鉴权无关、
    // 也**不是本批造成的**（无注解时同样 500）。这里用新增的 `headers` 口子补上。
    { m: 'GET', p: '/v2/print/screenshots', code: 'print:screenshot:list', headers: { tenantId: '1' } },
    { m: 'GET', p: '/signature/list', code: 'signature:record:list' },
    { m: 'GET', p: '/rating/list', code: 'signature:rating:list' },
    // 复用既有码的证据：`set:print-config:view` 是库中**原有**的码（PrintConfigController
    // 另 3 个端点早已注解同一码族），本批只把裸的 `/behavior` 接上去、没有新造同义码。
    { m: 'GET', p: '/set/print-config/behavior', code: 'set:print-config:view' },
  ],
  // 2026-09-21 E-01 erppricing 批次（5 控制器 / 42 处注解，新增迁移 V11.466.0 补 19 个码）。
  // 路径取自迁移 `api_path` 列；两处例外已注明：`metrics`/`pricing` 那几条的代表端点带路径变量
  // 或需要请求体（POST），故改取**同一个码**的无参读端点，避免把「入参缺失 400」误判成失败。
  // ⚠️ PriceApprovalController（8 端点）**不在本批**：生成器把 Javadoc 里的
  //    `{@code @SaCheckPermission}` 字样误判成类级真注解而跳过（详见迁移 V11.466.0 头部），
  //    故它没有探针，基线也保留了该行。
  erppricing: [
    { m: 'GET', p: '/v1/price-engine/history/1', code: 'pricing:engine:list' },
    { m: 'GET', p: '/v1/price-engine/statistics/1', code: 'pricing:engine:view' },
    { m: 'GET', p: '/v1/price-engine/tiers/tiers', code: 'pricing:tier:list' },
    { m: 'GET', p: '/v1/price-strategy/config/supported-formats', code: 'pricing:strategy:view' },
    { m: 'GET', p: '/erp/pricing/configs', code: 'pricing:price:view' },
    { m: 'GET', p: '/erp/pricing/price-memory/page', code: 'pricing:price:list' },
    { m: 'GET', p: '/erp/product-grade-price/by-product/1', code: 'product:grade-price:list' },
  ],
  // 2026-09-21 E-01 erpobserv 批次（2 控制器 / 24 处注解，新增迁移 V11.467.0 补 9 个码）。
  // api_path 列的 `metrics:metric:list` 代表端点是 `GET /erp/metrics/type/{type}`（带变量），
  // `monitor:business-metric:list` 代表端点是 `POST /erp/monitor/query`（要请求体）⇒
  // 探针改取**同一个码**的无参读端点 `/erp/metrics/list`、`/erp/monitor/definitions`。
  erpobserv: [
    { m: 'GET', p: '/erp/metrics/dashboard', code: 'metrics:metric:view' },
    { m: 'GET', p: '/erp/metrics/list', code: 'metrics:metric:list' },
    { m: 'GET', p: '/erp/monitor/realtime', code: 'monitor:business-metric:view' },
    { m: 'GET', p: '/erp/monitor/definitions', code: 'monitor:business-metric:list' },
  ],
  // 2026-09-21 E-01 erpbatchsn 批次（2 控制器 / 28 处注解，新增迁移 V11.468.0 补 22 个码）。
  // 路径取自迁移 `api_path` 列（域口径见该迁移头部：`erp:batch:*` / `erp:serial:*`，
  // 与前端 views/erp/batch|serial 的既有 v-permission 字符串一致）。
  // ⚠️ 该模块有 3 个端点在超管侧**必然 500**，原因是**既有的 Mapper 缺陷**（与本批无关，
  //    无注解时同样 500）：`BatchNumberMapper` / `SerialNumberMapper` 声明了自定义方法却
  //    既没有 `@Select` 注解、erp-stock 里也**没有 mapper XML**（`backend/erp/erp-stock/
  //    src/main/resources` 目录不存在）⇒ MyBatis 抛
  //    `BindingException: Invalid bound statement (not found)`：
  //      · GET /erp/batch-sn/batches/expiring-warning   → BatchNumberMapper.selectExpiringBatches
  //      · GET /erp/batch-sn/batches/stock-summary      → BatchNumberMapper.selectStockSummary
  //      · GET /erp/batch-sn/batches/validate-batch-no  → BatchNumberMapper.selectByBatchNo
  //      · GET /erp/batch-sn/serials/warranty-warning   → SerialNumberMapper.selectWarrantyExpiring
  //    故探针改取**同一个码**下不依赖这些方法的端点（cache-stats / full-history），
  //    `erp:batch:check` 因为**只有** validate-batch-no 一个端点、无法回避，本批不做探针
  //    （它的"注解已生效"由非超管侧那条 403 之外的证据缺失，见报告）。
  erpbatchsn: [
    { m: 'GET', p: '/erp/batch-sn/batches/page', code: 'erp:batch:list' },
    { m: 'GET', p: '/erp/batch-sn/batches/cache-stats', code: 'erp:batch:view' },
    { m: 'GET', p: '/erp/batch-sn/serials', code: 'erp:serial:list' },
    { m: 'GET', p: '/erp/batch-sn/serials/1/full-history', code: 'erp:serial:view' },
  ],
  // 2026-09-21 E-01「core 底座」批次（5 个模块 / 195 处注解，新增迁移
  // V11.471.0~V11.474.0 补 108 个码、复用 9 个既有码；前缀归属迁移 V11.475.0）。
  // 路径全部取自迁移 `api_path` 列（不是手猜 —— 猜错会把「入参缺失 400」误判成失败）。
  // ⚠️ 探针**不能**取这些（有意排除、本来就不该有权限码）：
  //   /auth/**、/notification/**（core-base 的"我的通知"）、/system/user-config/**、
  //   /sys/region/**、/open/**、/sse/notifications、/session/current、
  //   /menu/user/{tree,client/*,mega/*}、/role/list、/user/{login,logout}、
  //   /user/{id}/password/change、/role-bill-type/validate、POST /error-report、
  //   /automation/trigger/*、/core/notification/{unread,list,...}（8 个自助端点）、
  //   /payment/callback/*、/refund/callback/*、/agent/{heartbeat,validate}、/agent/invoke/**。
  corebase: [
    { m: 'GET', p: '/quality/certificate/page?current=1&size=1', code: 'quality:certificate:list' },
    { m: 'GET', p: '/quality/inspection/page?current=1&size=1', code: 'quality:inspection:list' },
    { m: 'GET', p: '/quality/standard/page?current=1&size=1', code: 'quality:standard:list' },
    { m: 'GET', p: '/quality/defect/page?current=1&size=1', code: 'quality:defect:list' },
    { m: 'GET', p: '/trade/channel/page?current=1&size=1', code: 'trade:channel:list' },
    { m: 'GET', p: '/trade/external-order/page?current=1&size=1', code: 'trade:external-order:list' },
    { m: 'GET', p: '/trade/inventory-sync/page?current=1&size=1', code: 'trade:inventory-sync:list' },
    { m: 'GET', p: '/trade/api-monitor/stat', code: 'trade:api-monitor:view' },
    { m: 'GET', p: '/set/menu-config/list', code: 'set:menu-config:list' },
    { m: 'GET', p: '/system/log/structured/query', code: 'log:structured:list' },
    { m: 'GET', p: '/error-report/recent', code: 'log:error-report:list' },
    // 复用既有码的三条（证明 code_rules 把裸端点接到了历史码上，而不是另建同义码）
    { m: 'GET', p: '/logs/system/page?current=1&size=1', code: 'log:oper:list' },
    { m: 'GET', p: '/system/log/advanced/summary', code: 'log:oper:stats' },
    // 该端点有**必填** `@RequestParam Long tenantId`（不带会 400，与鉴权无关）⇒ 显式补参
    { m: 'GET', p: '/menu/client/tenant-admin?tenantId=1', code: 'system:menu:list' },
    { m: 'GET', p: '/role/1', code: 'tenant-admin:role:detail' },
    // check-code 两个端点有**必填** RequestParam，不带会 400（与鉴权无关）⇒ 显式补参
    { m: 'GET', p: '/menu/check-code?menuCode=__probe__&tenantId=1', code: 'system:menu:check' },
    { m: 'GET', p: '/permission/check-code?permissionCode=__probe__&tenantId=1', code: 'tenant-admin:permission:check' },
  ],
  // ⚠️ 本模块的四个 base **全配了 base_overrides**，探针路径同样取自迁移 api_path。
  //    kanban 的三条资源（board/column/card）各取一个读端点；
  //    custom-field-value 的端点都要必填参数，取 /all 并补参。
  coreplatform: [
    { m: 'GET', p: '/automation/list?current=1&size=1', code: 'automation:rule:list' },
    { m: 'GET', p: '/custom-field/list?current=1&size=1', code: 'custom-field:field:list' },
    { m: 'GET', p: '/custom-field/validate', code: 'custom-field:field:check' },
    { m: 'GET', p: '/custom-field-value/all?modelName=__probe__&recordId=1', code: 'custom-field:value:view' },
    { m: 'GET', p: '/kanban/__probe__/columns', code: 'kanban:column:list' },
    { m: 'GET', p: '/kanban/card/list?page=1&size=1', code: 'kanban:card:list' },
  ],
  // ⚠️ **不探 webhook**：`cn.aiedge.webhook.controller` 不在 AiReadyApplication.scanBasePackages
  //    里（见 known-unscanned-controller-packages.txt）⇒ 它的 14 个端点运行时一律 **404**，
  //    不是本批造成的。码照建（控制器一旦接线即生效），但运行时无从验证。
  //    探针只取真正装配了的三个控制器。
  corenotify: [
    { m: 'GET', p: '/notification-templates', code: 'notification:template:list' },
    { m: 'GET', p: '/notification-stats/channels', code: 'notification:stats:view' },
    { m: 'GET', p: '/core/notification/export', code: 'notification:message:export' },
  ],
  corepayment: [
    { m: 'GET', p: '/reconciliation/page?current=1&size=1', code: 'payment:reconciliation:list' },
    { m: 'GET', p: '/refund/request/page?current=1&size=1', code: 'payment:refund:list' },
    { m: 'GET', p: '/refund/request/stat', code: 'payment:refund:view' },
    { m: 'GET', p: '/reconciliation/stat', code: 'payment:reconciliation:view' },
  ],
  // ⚠️ coreagent **没有可用探针**（故本数组为空，脚本将以 PASS 0 / FAIL 0 退出）：
  //    · 唯一的裸控制器 AgentInvokeController（2 端点）+ AgentController 的
  //      /heartbeat、/validate 都按配置**有意不加码**（agent 客户端无用户会话）；
  //    · AgentController 里唯一补了码的 `/api/agent/active`（复用 system:agent:list）
  //      所在的包 `cn.aiedge.agent.controller` **不在 scanBasePackages 内**
  //      ⇒ 运行时一律 404，超管侧也测不出"放行"。
  //    该端点的注解已由源码 grep 复核（见报告）。另：本模块**未新增任何权限码**。
  coreagent: [],

  // ══════════════════════════════════════════════════════════════════════════
  // 2026-09-21 E-01 **最后一批**：b2b（后台侧）/ supplier / deliveryroute /
  // partnerlevel / mktanalytics / stockextra。路径全部取自迁移 `api_path` 列
  // （不是手猜 —— 猜错会把「入参缺失 400」误判成失败）。
  // ⚠️ 每个模块的"非超管被拒"侧要求返回 403：Sa-Token 的注解校验在
  //    HandlerInterceptor.preHandle 里跑，**早于**参数绑定 ⇒ 缺参数也不会把 403 冲掉。
  // ══════════════════════════════════════════════════════════════════════════

  // E-01 b2b 批次（后台管理侧 5 控制器 / 60 处注解，迁移 V11.477.0 补 47 个码；C 端 53 个端点整类排除）。
  // ⚠️ 探针**不能**取这些（有意排除、本来就不该有权限码）：
  //   /v1/mall/**（auth/products/cart/orders/party-link/payments/user）、/erp/mall/notice。
  //   理由：C 端登录主体是 shop_user（买家），没有任何权限码，补码即 403，见 V11.477.0 头部。
  b2b: [
    { m: 'GET', p: '/erp/mall/admin/config', code: 'mall:config:view' },
    { m: 'GET', p: '/erp/mall/admin/banner', code: 'mall:banner:list' },
    { m: 'GET', p: '/erp/mall/admin/user/page', code: 'mall:user:list' },
    { m: 'GET', p: '/erp/mall/admin/product/page', code: 'mall:product:list' },
    { m: 'GET', p: '/erp/mall/admin/order/page', code: 'mall:order:list' },
    { m: 'GET', p: '/erp/mall/admin/order/stats', code: 'mall:order:view' },
    { m: 'GET', p: '/erp/mall/admin/decoration', code: 'mall:decoration:list' },
    { m: 'GET', p: '/erp/mall/admin/template/list', code: 'mall:template:list' },
    { m: 'GET', p: '/erp/mall/admin/keyword/page', code: 'mall:keyword:list' },
    { m: 'GET', p: '/erp/mall/admin/notice/page', code: 'mall:notice:list' },
    { m: 'GET', p: '/erp/mall/admin/popup-ad/page', code: 'mall:popup-ad:list' },
    { m: 'GET', p: '/erp/mall/admin/trade-analysis', code: 'mall:trade-analysis:view' },
  ],

  // E-01 supplier 批次（3 控制器 / 47 处注解，迁移 V11.478.0 补 27 个码，域 `supplier:`）。
  // ⚠️ **不探 `supplier:points:list`**：该码的两个端点
  //    （`GET /v1/supplier-portal/points/{id}/records`、`GET /supplier-portal/points/{id}/records`）
  //    在**超管侧必然 500**，原因是**既有的 Mapper 与表结构漂移**（与本批无关，无注解时同样 500）：
  //    `SupplierPointsRecordMapper.java:14` 写 `ORDER BY create_time`，而
  //    `erp_supplier_points_record` 表的列叫 `created_time`（实测 information_schema），
  //    PostgreSQL 报 `字段 "create_time" 不存在`。同文件的 `SUM(points)` / `points_type`
  //    也对不上（表里是 `points_amount` / `record_type`）⇒ `points:total`、`dashboard` 同样会 500。
  //    该码的"注解已生效"仍有**一半证据**：非超管侧实测 403 且文案带码名（说明注解插上了）；
  //    另一半（超管 200）因端点本身坏掉无法取证 —— 与 erpbatchsn 批次同一处理口径。
  supplier: [
    { m: 'GET', p: '/supplier/statistics', code: 'supplier:view' },
    { m: 'GET', p: '/supplier/list', code: 'supplier:list' },
    { m: 'GET', p: '/v1/supplier-portal/levels', code: 'supplier:level:list' },
    { m: 'GET', p: '/v1/supplier-portal/performances/supplier/1', code: 'supplier:performance:list' },
  ],

  // E-01 deliveryroute 批次（2 控制器 / 38 处注解，迁移 V11.479.0 补 23 个码）。
  // ⚠️ 不探 `GET /erp/md/route/options`（有意排除的跨模块下拉源，见 V11.479.0 头部）。
  deliveryroute: [
    { m: 'GET', p: '/delivery/route/page', code: 'delivery:route:list' },
    { m: 'GET', p: '/delivery/route/next-no', code: 'delivery:route:list' },
    { m: 'GET', p: '/delivery/route/demands', code: 'delivery:demand:list' },
    { m: 'GET', p: '/erp/md/route/page', code: 'md:route-master:list' },
  ],

  // E-01 partnerlevel 批次（1 控制器 / 12 处注解，迁移 V11.480.0 补 7 个码 `party:customer-level:*`）。
  partnerlevel: [
    { m: 'GET', p: '/erp/customer/level/list', code: 'party:customer-level:list' },
    { m: 'GET', p: '/erp/customer/level/page', code: 'party:customer-level:list' },
  ],

  // E-01 mktanalytics 批次（1 控制器 / 7 处注解，迁移 V11.481.0 补 2 个码 + 复用 1 个既有码）。
  mktanalytics: [
    { m: 'GET', p: '/erp/marketing/commission/analytics/rider-matrix/page',
      code: 'marketing:commission-analytics:list' },
    // 复用既有码的证据：`marketing:commission-rule:list` 是库中**原有**的码，
    // 本批把 `/plans`（读同一张 erp_commission_rule 表）接上去，没有新造同义码。
    { m: 'GET', p: '/erp/marketing/commission/analytics/plans', code: 'marketing:commission-rule:list' },
  ],

  // E-01 stockextra 批次（2 控制器 / 6 处注解，迁移 V11.482.0 补 6 个码）。
  stockextra: [
    { m: 'GET', p: '/set/initial-stock/page', code: 'set:initial-stock:list' },
    { m: 'GET', p: '/erp/stock/analytics/page', code: 'stock:analytics:list' },
  ],
}

const probes = PROBES[moduleName]
if (!probes) {
  console.error(`未配置模块 ${moduleName} 的探测端点（在 PROBES 里补）`)
  process.exit(2)
}

let pass = 0, fail = 0
const ok = (n, c, e = '') => {
  if (c) { pass++; console.log(`  [PASS] ${n}${e ? ' — ' + e : ''}`) }
  else { fail++; console.log(`  [FAIL] ${n}${e ? ' — ' + e : ''}`) }
}
const section = (t) => console.log(`\n${t}`)
const warn = (t) => console.log(`  [WARN] ${t}`)

// `headers`（可选，2026-09-21 erpprinting 批次新增）：少数端点自己声明了
// `@RequestHeader Long tenantId` 这类**必填请求头**，不发会直接被 Spring 判 500
// （MissingRequestHeaderException → GlobalExceptionHandler 归成"系统异常"），
// 从而把"鉴权通过/未通过"的正确断言淹没掉。给探针留一个显式补头的口子。
async function req(method, p, { token, tenantHeader, body, headers } = {}) {
  const res = await fetch(BASE + p, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(headers || {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(tenantHeader ? { 'X-Tenant-Id': String(tenantHeader) } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await res.text()
  let json = null
  try { json = JSON.parse(text) } catch { /* 非 JSON */ }
  return { status: res.status, json, text }
}

async function login(user) {
  const cap = await req('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await req('POST', '/auth/login', {
    body: { username: user.u, password: user.p, tenantName: user.t, captcha: code, captchaKey: cap.json.data.uuid },
  })
  const token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) throw new Error(`${user.u} 登录失败: ${res.text.slice(0, 300)}`)
  return token
}

;(async () => {
  console.log(`验证目标: ${BASE}  模块: ${moduleName}`)

  const admin = await login(ADMIN)
  const outsider = await login(OUTSIDER)
  console.log(`登录成功: ${ADMIN.u}（超管 / 租户 1）、${OUTSIDER.u}（非超管 / 租户 2）`)

  const adminFails = [], outsiderLeaks = [], unknownCodes = []

  section('① 超管放行（证明权限码真的在库里 —— 码不存在会让所有人 403）')
  for (const pr of probes) {
    const r = await req(pr.m, pr.p, { token: admin, tenantHeader: 1, body: pr.body, headers: pr.headers })
    // 超管的判据不是「必须 200」：某些端点带业务前置校验（如必填入参），
    // 关键是**没被鉴权层挡下**（403）且不是服务端错误。
    const okStatus = r.status !== 403 && r.status < 500
    if (!okStatus) adminFails.push(`${pr.m} ${pr.p} → ${r.status} ${r.json?.message || ''}`)
    ok(`[${pr.code}] 超管未 403/5xx`, okStatus, `status=${r.status}`)
    if (r.status === 403 && /无权限访问/.test(r.json?.message || '')) {
      unknownCodes.push(`${pr.code}（${r.json.message}）`)
    }
  }

  section('② 非超管被拒（证明注解真的插到了方法上，而不是静默 fail-open）')
  for (const pr of probes) {
    const r = await req(pr.m, pr.p, { token: outsider, tenantHeader: 2, body: pr.body, headers: pr.headers })
    const denied = r.status === 403
    if (!denied) outsiderLeaks.push(`${pr.m} ${pr.p} → ${r.status}`)
    ok(`[${pr.code}] 非超管 403`, denied, `status=${r.status} ${r.json?.message || ''}`)
  }

  section('③ 小结')
  if (unknownCodes.length) {
    warn(`以下码可能在库中缺失（超管被 403）：${unknownCodes.join(', ')}`)
  }
  if (adminFails.length) {
    warn(`超管侧异常：\n    - ${adminFails.join('\n    - ')}`)
  }
  if (outsiderLeaks.length) {
    console.log(`  ⚠️ 以下端点非超管**未被拒**（注解可能没插上）：\n    - ${outsiderLeaks.join('\n    - ')}`)
  }
  if (!adminFails.length && !outsiderLeaks.length) {
    console.log('  ✅ 两向断言均满足：码在库中（超管放行）+ 注解生效（非超管被拒）')
  }

  console.log(`\n===== 结果：PASS ${pass} / FAIL ${fail} =====`)
  process.exit(fail === 0 ? 0 : 1)
})().catch(e => {
  console.error('\n验证脚本异常:', e.message)
  process.exit(2)
})
