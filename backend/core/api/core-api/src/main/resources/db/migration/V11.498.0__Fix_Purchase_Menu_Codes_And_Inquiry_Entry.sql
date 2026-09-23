-- 采购域菜单：menu_code 前缀对齐 + 补「采购询价」入口（2026-09-23）
--
-- ── 为什么要改 menu_code ──────────────────────────────────────
-- menu_code 参与菜单可见性派生（SysMenuServiceImpl + MenuPermissionDeriver）：
--   ① 库中存在以 menu_code 为前缀的权限码，且用户一个都不持有 ⇒ 菜单隐藏；
--   ② 库中**不存在**任何以该 menu_code 为前缀的权限码 ⇒ 菜单保持可见（fail-open）。
-- 所以 menu_code 写错的后果不是"少一个菜单"，而是「菜单人人可见、点进去 403」。
-- 本迁移把 7 条前缀写错的菜单改回该页真实调用的接口所需的权限码前缀。
--
-- ── 逐条依据（均已核对「页面实际请求的接口」+「该接口上的 @SaCheckPermission」）──
--   70071 采购明细查询     : 调 /api/erp/purchase/order/detail-query/page → 需 purchase:order:list
--                            原 menu_code=purchase:detail-query，库中无此前缀的码
--   70051/70052/70053 补货  : 调 /api/erp/stock/{alert,shortage,smart}-replenish/** → 需 stock:replenishment:*
--                            原 menu_code=purchase:*-replenish，库中无此前缀的码
--   81006 供应商询价        : 需 supplier:inquiry:*      （原 purchase:supplier-inquiry 前缀不匹配）
--   81007 供应商绩效评估    : 需 supplier:performance:*  （原 purchase:supplier-performance 前缀不匹配）
--   80421/80422 采购分析|准备: 需 purchase:analytics:list（原 ana:purchase-* 前缀不匹配）
--
--   ⚠️ 70070 采购单据查询的 menu_code=purchase:doc-query **是正确的**（该页调
--      /api/purchase/doc-query/page，UnifiedPurchaseDocQueryController 用 purchase:doc-query:list）。
--      审计报告里把这条列为错配属误判，本次刻意不改。
--   ⚠️ 70072 采购价格跟踪的 menu_code=purchase:price-track **也是正确的**（库中有该前缀的码）；
--      错的是后端注解用了粗粒度遗留码 purchase:price:edit —— 已在代码侧改注解，本迁移不动菜单。
--
-- ── menu_level 3 → 0 ─────────────────────────────────────────
-- SysMenuServiceImpl 对「非系统租户且非超管」强制 eq(menu_level, 0)；全库 3/4 级共 129 条，
-- 采购域只有 80421/80422 两条非 0 ⇒ 租户普通用户看不到这两个菜单。已核 3/4 级无文档化语义，
-- 按同级菜单口径统一为 0。
--
-- ── 新增 70073「采购询价」──────────────────────────────────────
-- 页面 views/purchase/inquiry/index.vue、静态路由、13 个后端端点、10 个 purchase:inquiry:* 权限码
-- **全部齐备**，唯独没有 sys_menu 行 —— 功能齐全却无入口。挂到「采购 → 采购查询」下（与供应商询价同级）。
-- 不删任何菜单、不改任何 path/component，只改 menu_code 与 menu_level 并新增一行。

-- ── ① menu_code 前缀对齐 ────────────────────────────────────
UPDATE sys_menu SET menu_code = 'purchase:order', update_time = CURRENT_TIMESTAMP
 WHERE id = 70071 AND deleted = 0;

UPDATE sys_menu SET menu_code = 'stock:replenishment', update_time = CURRENT_TIMESTAMP
 WHERE id IN (70051, 70052, 70053) AND deleted = 0;

UPDATE sys_menu SET menu_code = 'supplier:inquiry', update_time = CURRENT_TIMESTAMP
 WHERE id = 81006 AND deleted = 0;

UPDATE sys_menu SET menu_code = 'supplier:performance', update_time = CURRENT_TIMESTAMP
 WHERE id = 81007 AND deleted = 0;

UPDATE sys_menu SET menu_code = 'purchase:analytics', menu_level = 0, update_time = CURRENT_TIMESTAMP
 WHERE id IN (80421, 80422) AND deleted = 0;

-- ── ② 补「采购询价」菜单（幂等：已存在则不动）──────────────────
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, icon, sort,
                      is_external, is_cache, visible, status, client_type,
                      display_group, menu_level)
SELECT 70073, 0, 60203, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
       '采购询价', 'purchase:inquiry', 1, 'purchase/inquiry', 'views/purchase/inquiry/index.vue',
       'ProfileOutlined', 6, 0, 1, 1, 1, 'tenant-admin', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 70073);
