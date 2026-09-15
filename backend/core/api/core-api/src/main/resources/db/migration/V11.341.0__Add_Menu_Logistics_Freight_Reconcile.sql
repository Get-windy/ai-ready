-- =============================================================================
-- 新增菜单：配发收 → 发货业务 → 物流运费对账（P2-4/P2-6 的前端落点）
--
-- 依据：《物流发货-业界做法调研.md》§7 —— 承运商运费规则 + 运费对账（对齐用友 U8「运费维护/分摊」
--       与 TMS「FreightBill 对账」）、发货通知（ASN）台账，需要一个工作台页面承载三件事：
--       ① 运费规则（承运商×区域×重量区间 首重/续重）
--       ② 运费对账（我方计费 vs 承运商账单，差异清单）
--       ③ 发货通知 ASN（台账 / 重新发送）
--
-- 本系统新增能力（ql361 无对应菜单），沿用「配发收 → 发货业务」(60402) 分组。
-- 幂等：ON CONFLICT (id) DO NOTHING
-- =============================================================================

INSERT INTO sys_menu (
    id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, path, component, route_name, redirect,
    icon, sort, is_external, is_cache, visible, status, client_type,
    remark, biz_flow_tag, display_group, link_icon, display_mode, list_path, tag_label, menu_level
) VALUES (
    70162, 0, 60402, 0, now(), now(),
    '物流运费对账', 'dispatch:freight-reconcile', 1,
    'dispatch/freight-reconcile', 'views/dispatch/freight-reconcile/index.vue', NULL, NULL,
    'AccountBookOutlined', 3, 0, 1, 1, 1, 'tenant-admin',
    '承运商运费规则 / 运费对账（我方计费 vs 账单）/ 发货通知 ASN', NULL, 0, NULL, 0, NULL, NULL, 0
) ON CONFLICT (id) DO NOTHING;
