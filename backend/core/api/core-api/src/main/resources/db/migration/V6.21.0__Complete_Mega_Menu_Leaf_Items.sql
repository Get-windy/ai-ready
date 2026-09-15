-- ============================================================
-- V6.21.0: 补全所有 Mega Menu 叶子菜单项
--
-- 基于 mega-menu-redesign.md 设计文档，补全所有缺失的叶子菜单项
-- 并设置 displayMode / listPath / tagLabel
-- ============================================================

-- ============================================================
-- Part 1: 插入缺失的叶子菜单项
-- ============================================================

-- ── 销售: 外勤拜访 (60101) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (70001, 1, 60101, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '拜访规划', 'sales:visit-plan', 1, 'sales/visit-plan', 'views/common/placeholder/index.vue', 'ScheduleOutlined', 1, 1, 1, 'pc-admin', 0, 0, 0),
  (70002, 1, 60101, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '拜访执行', 'sales:visit-exec', 1, 'sales/visit-exec', 'views/common/placeholder/index.vue', 'FormOutlined', 2, 1, 1, 'pc-admin', 0, 0, 0),
  (70003, 1, 60101, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '拜访检视', 'sales:visit-review', 1, 'sales/visit-review', 'views/common/placeholder/index.vue', 'EyeOutlined', 3, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 销售: 订货业务 (60102) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, list_path, tag_label, display_group, menu_level)
VALUES
  (70010, 1, 60102, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '销售订单', 'sales:order', 1, 'sales/order/form', 'views/sales/order/index.vue', 'ShoppingOutlined', 1, 1, 1, 'pc-admin', 1, 'sales/order', '历史', 0, 0),
  (70011, 1, 60102, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '销售退货申请', 'sales:return-apply', 1, 'sales/return-apply/form', 'views/common/placeholder/index.vue', 'RollbackOutlined', 2, 1, 1, 'pc-admin', 1, 'sales/return-apply', '历史', 0, 0),
  (70012, 1, 60102, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '预订货单', 'sales:pre-order', 1, 'sales/pre-order/form', 'views/common/placeholder/index.vue', 'ScheduleOutlined', 3, 1, 1, 'pc-admin', 1, 'sales/pre-order', '历史', 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 销售: 销售业务 (60103) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, list_path, tag_label, display_group, menu_level)
VALUES
  (70020, 1, 60103, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '零售单', 'sales:retail', 1, 'sales/retail/form', 'views/common/placeholder/index.vue', 'ShopOutlined', 1, 1, 1, 'pc-admin', 1, 'sales/retail', '历史', 0, 0),
  (70021, 1, 60103, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '销售出库单', 'sales:outbound', 1, 'sales/outbound/form', 'views/common/placeholder/index.vue', 'SendOutlined', 2, 1, 1, 'pc-admin', 1, 'sales/outbound', '历史', 0, 0),
  (70022, 1, 60103, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '销售退货单', 'sales:return-doc', 1, 'sales/return-doc/form', 'views/common/placeholder/index.vue', 'RollbackOutlined', 3, 1, 1, 'pc-admin', 1, 'sales/return-doc', '历史', 0, 0),
  (70023, 1, 60103, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '销售换货单', 'sales:exchange', 1, 'sales/exchange/form', 'views/common/placeholder/index.vue', 'SwapOutlined', 4, 1, 1, 'pc-admin', 1, 'sales/exchange', '历史', 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 销售: 销售查询 (60104) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (70030, 1, 60104, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '销售单据查询', 'sales:doc-query', 1, 'sales/doc-query', 'views/common/placeholder/index.vue', 'SearchOutlined', 10, 1, 1, 'pc-admin', 0, 0, 0),
  (70031, 1, 60104, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '销售明细查询', 'sales:detail-query', 1, 'sales/detail-query', 'views/common/placeholder/index.vue', 'FileTextOutlined', 11, 1, 1, 'pc-admin', 0, 0, 0),
  (70032, 1, 60104, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '销售价格跟踪', 'sales:price-track', 1, 'sales/price-track', 'views/common/placeholder/index.vue', 'LineChartOutlined', 12, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 采购: 采购准备 (60201) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (70051, 1, 60201, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '库存预警补货', 'purchase:alert-replenish', 1, 'purchase/alert-replenish', 'views/common/placeholder/index.vue', 'AlertOutlined', 1, 1, 1, 'pc-admin', 0, 0, 0),
  (70052, 1, 60201, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '缺货补货', 'purchase:shortage-replenish', 1, 'purchase/shortage-replenish', 'views/common/placeholder/index.vue', 'WarningOutlined', 2, 1, 1, 'pc-admin', 0, 0, 0),
  (70053, 1, 60201, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '智能补货', 'purchase:smart-replenish', 1, 'purchase/smart-replenish', 'views/common/placeholder/index.vue', 'RobotOutlined', 3, 1, 1, 'pc-admin', 0, 0, 0),
  (70054, 1, 60201, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '以销定购', 'purchase:sales-driven', 1, 'purchase/sales-driven', 'views/common/placeholder/index.vue', 'ShoppingCartOutlined', 4, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 采购: 采购业务 (60202) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, list_path, tag_label, display_group, menu_level)
VALUES
  (70060, 1, 60202, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '采购订单', 'purchase:order', 1, 'purchase/order/form', 'views/purchase/order/index.vue', 'ShoppingCartOutlined', 1, 1, 1, 'pc-admin', 1, 'purchase/order', '历史', 0, 0),
  (70061, 1, 60202, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '采购入库单', 'purchase:inbound', 1, 'purchase/inbound/form', 'views/common/placeholder/index.vue', 'InboxOutlined', 2, 1, 1, 'pc-admin', 1, 'purchase/inbound', '历史', 0, 0),
  (70062, 1, 60202, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '采购退货单', 'purchase:return', 1, 'purchase/return/form', 'views/common/placeholder/index.vue', 'RollbackOutlined', 3, 1, 1, 'pc-admin', 1, 'purchase/return', '历史', 0, 0),
  (70063, 1, 60202, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '采购换货单', 'purchase:exchange', 1, 'purchase/exchange/form', 'views/common/placeholder/index.vue', 'SwapOutlined', 4, 1, 1, 'pc-admin', 1, 'purchase/exchange', '历史', 0, 0),
  (70064, 1, 60202, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '采购费用分摊', 'purchase:cost-sharing', 1, 'purchase/cost-sharing/form', 'views/common/placeholder/index.vue', 'DollarOutlined', 5, 1, 1, 'pc-admin', 1, 'purchase/cost-sharing', '历史', 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 采购: 采购查询 (60203) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (70070, 1, 60203, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '采购单据查询', 'purchase:doc-query', 1, 'purchase/doc-query', 'views/common/placeholder/index.vue', 'SearchOutlined', 1, 1, 1, 'pc-admin', 0, 0, 0),
  (70071, 1, 60203, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '采购明细查询', 'purchase:detail-query', 1, 'purchase/detail-query', 'views/common/placeholder/index.vue', 'FileTextOutlined', 2, 1, 1, 'pc-admin', 0, 0, 0),
  (70072, 1, 60203, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '采购价格跟踪', 'purchase:price-track', 1, 'purchase/price-track', 'views/common/placeholder/index.vue', 'LineChartOutlined', 3, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 仓储: 其他出入库 (60301) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, list_path, tag_label, display_group, menu_level)
VALUES
  (70101, 1, 60301, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '其他出库单', 'wh:other-outbound', 1, 'wh/other-outbound/form', 'views/common/placeholder/index.vue', 'ExportOutlined', 1, 1, 1, 'pc-admin', 1, 'wh/other-outbound', '历史', 0, 0),
  (70102, 1, 60301, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '其他入库单', 'wh:other-inbound', 1, 'wh/other-inbound/form', 'views/common/placeholder/index.vue', 'ImportOutlined', 2, 1, 1, 'pc-admin', 1, 'wh/other-inbound', '历史', 0, 0),
  (70103, 1, 60301, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '调拨单', 'wh:transfer', 1, 'wh/transfer/form', 'views/common/placeholder/index.vue', 'SwapOutlined', 3, 1, 1, 'pc-admin', 1, 'wh/transfer', '历史', 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 仓储: 盘点 (60302) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, list_path, tag_label, display_group, menu_level)
VALUES
  (70110, 1, 60302, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '报损单', 'wh:damage', 1, 'wh/damage/form', 'views/common/placeholder/index.vue', 'MinusCircleOutlined', 1, 1, 1, 'pc-admin', 1, 'wh/damage', '历史', 0, 0),
  (70111, 1, 60302, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '报溢单', 'wh:overflow', 1, 'wh/overflow/form', 'views/common/placeholder/index.vue', 'PlusCircleOutlined', 2, 1, 1, 'pc-admin', 1, 'wh/overflow', '历史', 0, 0),
  (70112, 1, 60302, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '盘点单', 'wh:stocktake', 1, 'wh/stocktake/form', 'views/common/placeholder/index.vue', 'CheckSquareOutlined', 3, 1, 1, 'pc-admin', 1, 'wh/stocktake', '历史', 0, 0),
  (70113, 1, 60302, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '成本调价单', 'wh:cost-adjust', 1, 'wh/cost-adjust/form', 'views/common/placeholder/index.vue', 'DollarOutlined', 4, 1, 1, 'pc-admin', 1, 'wh/cost-adjust', '历史', 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 仓储: 生产 (60303) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, list_path, tag_label, display_group, menu_level)
VALUES
  (70120, 1, 60303, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '生产模板', 'wh:production-template', 1, 'wh/production-template/form', 'views/common/placeholder/index.vue', 'FileTextOutlined', 1, 1, 1, 'pc-admin', 1, 'wh/production-template', '列表', 0, 0),
  (70121, 1, 60303, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '组装单', 'wh:assemble', 1, 'wh/assemble/form', 'views/common/placeholder/index.vue', 'ToolOutlined', 2, 1, 1, 'pc-admin', 1, 'wh/assemble', '历史', 0, 0),
  (70122, 1, 60303, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '拆卸单', 'wh:disassemble', 1, 'wh/disassemble/form', 'views/common/placeholder/index.vue', 'ScissorOutlined', 3, 1, 1, 'pc-admin', 1, 'wh/disassemble', '历史', 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 仓储: 库存预警 (60304) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (70130, 1, 60304, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '预警查询', 'wh:alert-query', 1, 'wh/alert-query', 'views/common/placeholder/index.vue', 'SearchOutlined', 1, 1, 1, 'pc-admin', 0, 0, 0),
  (70131, 1, 60304, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '预警设置', 'wh:alert-config', 1, 'wh/alert-config', 'views/common/placeholder/index.vue', 'SettingOutlined', 2, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 仓储: 借进借出 (60305) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, list_path, tag_label, display_group, menu_level)
VALUES
  (70140, 1, 60305, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '借进单', 'wh:borrow-in', 1, 'wh/borrow-in/form', 'views/common/placeholder/index.vue', 'ImportOutlined', 1, 1, 1, 'pc-admin', 1, 'wh/borrow-in', '历史', 0, 0),
  (70141, 1, 60305, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '借出单', 'wh:borrow-out', 1, 'wh/borrow-out/form', 'views/common/placeholder/index.vue', 'ExportOutlined', 2, 1, 1, 'pc-admin', 1, 'wh/borrow-out', '历史', 0, 0),
  (70142, 1, 60305, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '借进借出查询', 'wh:borrow-query', 1, 'wh/borrow-query', 'views/common/placeholder/index.vue', 'SearchOutlined', 3, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 配发收: 配送业务 (60401) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (70150, 1, 60401, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '配送查询', 'dispatch:query', 1, 'dispatch/query', 'views/common/placeholder/index.vue', 'SearchOutlined', 1, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 配发收: 发货业务 (60402) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (70155, 1, 60402, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '物流发货', 'dispatch:logistics-ship', 1, 'dispatch/logistics-ship', 'views/common/placeholder/index.vue', 'SendOutlined', 1, 1, 1, 'pc-admin', 0, 0, 0),
  (70156, 1, 60402, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '发货查询', 'dispatch:ship-query', 1, 'dispatch/ship-query', 'views/common/placeholder/index.vue', 'SearchOutlined', 2, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 配发收: 收货业务 (60403) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (70160, 1, 60403, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '物流退货收货', 'dispatch:return-receive', 1, 'dispatch/return-receive', 'views/common/placeholder/index.vue', 'RollbackOutlined', 1, 1, 1, 'pc-admin', 0, 0, 0),
  (70161, 1, 60403, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '采购订货收货', 'dispatch:purchase-receive', 1, 'dispatch/purchase-receive', 'views/common/placeholder/index.vue', 'ImportOutlined', 2, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 财务: 收入支出 (60603) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, list_path, tag_label, display_group, menu_level)
VALUES
  (70201, 1, 60603, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '费用单', 'finance:expense-doc', 1, 'finance/expense-doc/form', 'views/common/placeholder/index.vue', 'FileTextOutlined', 1, 1, 1, 'pc-admin', 1, 'finance/expense-doc', '历史', 0, 0),
  (70202, 1, 60603, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '其他收入', 'finance:other-income', 1, 'finance/other-income/form', 'views/common/placeholder/index.vue', 'PlusCircleOutlined', 2, 1, 1, 'pc-admin', 1, 'finance/other-income', '历史', 0, 0),
  (70203, 1, 60603, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '应收应付调整', 'finance:ar-ap-adjust', 1, 'finance/ar-ap-adjust/form', 'views/common/placeholder/index.vue', 'EditOutlined', 3, 1, 1, 'pc-admin', 1, 'finance/ar-ap-adjust', '历史', 0, 0),
  (70204, 1, 60603, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '账款交账', 'finance:account-delivery', 1, 'finance/account-delivery', 'views/common/placeholder/index.vue', 'SwapOutlined', 4, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 财务: 账簿 (60605) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (70220, 1, 60605, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '总账', 'finance:general-ledger', 1, 'finance/general-ledger', 'views/common/placeholder/index.vue', 'BookOutlined', 1, 1, 1, 'pc-admin', 0, 0, 0),
  (70221, 1, 60605, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '明细账', 'finance:detail-ledger', 1, 'finance/detail-ledger', 'views/common/placeholder/index.vue', 'FileTextOutlined', 2, 1, 1, 'pc-admin', 0, 0, 0),
  (70222, 1, 60605, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '科目余额表', 'finance:balance-sheet', 1, 'finance/balance-sheet', 'views/common/placeholder/index.vue', 'TableOutlined', 3, 1, 1, 'pc-admin', 0, 0, 0),
  (70223, 1, 60605, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '辅助核算余额表', 'finance:aux-balance', 1, 'finance/aux-balance', 'views/common/placeholder/index.vue', 'TableOutlined', 4, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 财务: 财务报表 (60606) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (70230, 1, 60606, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '资产负债表', 'finance:balance-report', 1, 'finance/balance-report', 'views/common/placeholder/index.vue', 'BarChartOutlined', 1, 1, 1, 'pc-admin', 0, 0, 0),
  (70231, 1, 60606, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '利润表', 'finance:profit-report', 1, 'finance/profit-report', 'views/common/placeholder/index.vue', 'LineChartOutlined', 2, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 财务: 费用管理 (60609) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, list_path, tag_label, display_group, menu_level)
VALUES
  (70240, 1, 60609, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '费用申请', 'finance:expense-apply', 1, 'finance/expense-apply/form', 'views/common/placeholder/index.vue', 'FileAddOutlined', 1, 1, 1, 'pc-admin', 1, 'finance/expense-apply', '历史', 0, 0),
  (70241, 1, 60609, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '费用报销', 'finance:expense-reimburse', 1, 'finance/expense-reimburse/form', 'views/common/placeholder/index.vue', 'DollarOutlined', 2, 1, 1, 'pc-admin', 1, 'finance/expense-reimburse', '历史', 0, 0),
  (70242, 1, 60609, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '费用审批', 'finance:expense-approval', 1, 'finance/expense-approval', 'views/common/placeholder/index.vue', 'AuditOutlined', 3, 1, 1, 'pc-admin', 0, 0, 0),
  (70243, 1, 60609, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '费用支付', 'finance:expense-pay', 1, 'finance/expense-pay/form', 'views/common/placeholder/index.vue', 'DollarOutlined', 4, 1, 1, 'pc-admin', 1, 'finance/expense-pay', '历史', 0, 0),
  (70244, 1, 60609, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '费用统计', 'finance:expense-stats', 1, 'finance/expense-stats', 'views/common/placeholder/index.vue', 'BarChartOutlined', 5, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── CRM: 客户管理 (60701) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, list_path, tag_label, display_group, menu_level)
VALUES
  (70301, 1, 60701, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '客户', 'crm:customer', 1, 'crm/customer', 'views/crm/customer/index.vue', 'TeamOutlined', 1, 1, 1, 'pc-admin', 1, 'crm/customer/add', '添加', 0, 0),
  (70302, 1, 60701, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '客户跟进', 'crm:customer-follow', 1, 'crm/customer-follow', 'views/common/placeholder/index.vue', 'UserOutlined', 2, 1, 1, 'pc-admin', 1, 'crm/customer-follow', '历史', 0, 0),
  (70303, 1, 60701, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '客户分级', 'crm:customer-grade', 1, 'crm/customer-grade', 'views/common/placeholder/index.vue', 'StarOutlined', 3, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── CRM: 线索管理 (60702) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, list_path, tag_label, display_group, menu_level)
VALUES
  (70310, 1, 60702, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '线索', 'crm:lead', 1, 'crm/lead', 'views/crm/lead/index.vue', 'FireOutlined', 1, 1, 1, 'pc-admin', 1, 'crm/lead/add', '添加', 0, 0),
  (70311, 1, 60702, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '线索转化', 'crm:lead-convert', 1, 'crm/lead-convert', 'views/common/placeholder/index.vue', 'SwapOutlined', 2, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── CRM: 商机管理 (60703) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, list_path, tag_label, display_group, menu_level)
VALUES
  (70320, 1, 60703, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '商机', 'crm:opportunity', 1, 'crm/opportunity', 'views/crm/opportunity/index.vue', 'BulbOutlined', 1, 1, 1, 'pc-admin', 1, 'crm/opportunity/add', '添加', 0, 0),
  (70321, 1, 60703, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '商机阶段', 'crm:opportunity-stage', 1, 'crm/opportunity-stage', 'views/common/placeholder/index.vue', 'FlagOutlined', 2, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── CRM: 报价管理 (60704) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, list_path, tag_label, display_group, menu_level)
VALUES
  (70330, 1, 60704, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '报价单', 'crm:quotation', 1, 'crm/quotation', 'views/crm/quotation/index.vue', 'DollarOutlined', 1, 1, 1, 'pc-admin', 1, 'crm/quotation', '历史', 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── CRM: 合同管理 (60705) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, list_path, tag_label, display_group, menu_level)
VALUES
  (70340, 1, 60705, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '合同', 'crm:contract', 1, 'crm/contract', 'views/crm/contract/index.vue', 'FileTextOutlined', 1, 1, 1, 'pc-admin', 1, 'crm/contract', '历史', 0, 0),
  (70341, 1, 60705, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '合同审批', 'crm:contract-approval', 1, 'crm/contract-approval', 'views/common/placeholder/index.vue', 'AuditOutlined', 2, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── CRM: 发票管理 (60706) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, list_path, tag_label, display_group, menu_level)
VALUES
  (70350, 1, 60706, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '发票', 'crm:invoice', 1, 'crm/invoice', 'views/crm/invoice/index.vue', 'FileTextOutlined', 1, 1, 1, 'pc-admin', 1, 'crm/invoice', '历史', 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── CRM: CRM报表 (60707) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (70360, 1, 60707, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '销售漏斗', 'crm:funnel', 1, 'crm/funnel', 'views/common/placeholder/index.vue', 'FunnelPlotOutlined', 1, 1, 1, 'pc-admin', 0, 0, 0),
  (70361, 1, 60707, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '客户分析', 'crm:customer-analysis', 1, 'crm/customer-analysis', 'views/common/placeholder/index.vue', 'BarChartOutlined', 2, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 资料: 商品管理 (61101) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, list_path, tag_label, display_group, menu_level)
VALUES
  (70501, 1, 61101, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '商品', 'md:product', 1, 'md/product', 'views/erp/product/index.vue', 'AppstoreOutlined', 1, 1, 1, 'pc-admin', 1, 'md/product/add', '添加', 0, 0),
  (70502, 1, 61101, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '商品条码', 'md:barcode', 1, 'md/barcode', 'views/common/placeholder/index.vue', 'BarcodeOutlined', 2, 1, 1, 'pc-admin', 0, 0, 0),
  (70503, 1, 61101, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '商品价格管理', 'md:product-price', 1, 'md/product-price', 'views/common/placeholder/index.vue', 'DollarOutlined', 3, 1, 1, 'pc-admin', 0, 0, 0),
  (70504, 1, 61101, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '商品辅助资料', 'md:product-aux', 1, 'md/product-aux', 'views/common/placeholder/index.vue', 'FileTextOutlined', 4, 1, 1, 'pc-admin', 0, 0, 0),
  (70505, 1, 61101, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '图片管理', 'md:image', 1, 'md/image', 'views/common/placeholder/index.vue', 'PictureOutlined', 5, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 资料: 往来单位 (61102) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, list_path, tag_label, display_group, menu_level)
VALUES
  (70510, 1, 61102, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '客户', 'md:customer', 1, 'md/customer', 'views/common/placeholder/index.vue', 'TeamOutlined', 1, 1, 1, 'pc-admin', 1, 'md/customer/add', '添加', 0, 0),
  (70511, 1, 61102, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '供应商', 'md:supplier', 1, 'md/supplier', 'views/common/placeholder/index.vue', 'ShopOutlined', 2, 1, 1, 'pc-admin', 1, 'md/supplier/add', '添加', 0, 0),
  (70512, 1, 61102, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '物流公司', 'md:logistics', 1, 'md/logistics', 'views/common/placeholder/index.vue', 'CarOutlined', 3, 1, 1, 'pc-admin', 1, 'md/logistics/add', '添加', 0, 0),
  (70513, 1, 61102, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '互联账号', 'md:linked-account', 1, 'md/linked-account', 'views/common/placeholder/index.vue', 'LinkOutlined', 4, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 资料: 仓库管理 (61103) - 补充缺失项 ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (70520, 1, 61103, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '商品货位设置', 'md:location', 1, 'md/location', 'views/common/placeholder/index.vue', 'EnvironmentOutlined', 2, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 资料: 配送管理 (61104) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (70530, 1, 61104, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '线路', 'md:route', 1, 'md/route', 'views/common/placeholder/index.vue', 'NodeIndexOutlined', 1, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 资料: 财务账户 (61106) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (70540, 1, 61106, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '银行账户', 'md:bank-account', 1, 'md/bank-account', 'views/common/placeholder/index.vue', 'BankOutlined', 1, 1, 1, 'pc-admin', 0, 0, 0),
  (70541, 1, 61106, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '费用类型', 'md:expense-type', 1, 'md/expense-type', 'views/common/placeholder/index.vue', 'TagsOutlined', 2, 1, 1, 'pc-admin', 0, 0, 0),
  (70542, 1, 61106, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '其他收入', 'md:other-income', 1, 'md/other-income', 'views/common/placeholder/index.vue', 'PlusCircleOutlined', 3, 1, 1, 'pc-admin', 0, 0, 0),
  (70543, 1, 61106, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '会计科目', 'md:accounting-subject', 1, 'md/accounting-subject', 'views/common/placeholder/index.vue', 'BookOutlined', 4, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 设置: 期初录入 (61202) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (70550, 1, 61202, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '库存期初', 'set:initial-stock', 1, 'set/initial-stock', 'views/common/placeholder/index.vue', 'ContainerOutlined', 1, 1, 1, 'pc-admin', 0, 0, 0),
  (70551, 1, 61202, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '财务期初', 'set:initial-finance', 1, 'set/initial-finance', 'views/common/placeholder/index.vue', 'DollarOutlined', 2, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 设置: 账套操作 (61203) - 补充缺失项 ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (70560, 1, 61203, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '系统重建', 'set:rebuild', 1, 'set/rebuild', 'views/common/placeholder/index.vue', 'ReloadOutlined', 2, 1, 1, 'pc-admin', 0, 0, 0),
  (70561, 1, 61203, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '系统任务', 'set:system-task', 1, 'set/system-task', 'views/common/placeholder/index.vue', 'ClockCircleOutlined', 3, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ── 设置: 财务设置 (61204) ──
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time, menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (70570, 1, 61204, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '会计期间', 'set:accounting-period', 1, 'set/accounting-period', 'views/common/placeholder/index.vue', 'CalendarOutlined', 1, 1, 1, 'pc-admin', 0, 0, 0) ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- Part 2: 更新已有叶子菜单项的 display_mode / list_path / tag_label
-- ============================================================

-- 销售: 订货业务 - 已有项
UPDATE sys_menu SET display_mode = 1, list_path = 'sales/return', tag_label = '历史', update_time = CURRENT_TIMESTAMP WHERE menu_code = 'sales:return' AND parent_id = 60102 AND deleted = 0;

-- 销售: 销售业务 - 已有项
UPDATE sys_menu SET display_mode = 1, list_path = 'sales/shipment', tag_label = '历史', update_time = CURRENT_TIMESTAMP WHERE menu_code = 'sales:shipment' AND parent_id = 60103 AND deleted = 0;

-- 配送: 车辆管理
UPDATE sys_menu SET display_mode = 1, list_path = 'dms/vehicle', tag_label = '添加', update_time = CURRENT_TIMESTAMP WHERE menu_code = 'delivery:vehicle' AND deleted = 0;
-- 配送: 骑手管理
UPDATE sys_menu SET display_mode = 1, list_path = 'dms/rider', tag_label = '添加', update_time = CURRENT_TIMESTAMP WHERE menu_code = 'delivery:rider' AND deleted = 0;
-- 配送: 签收管理 (如有)
UPDATE sys_menu SET tag_label = '历史', update_time = CURRENT_TIMESTAMP WHERE menu_code = 'delivery:sign' AND deleted = 0;
-- 配送: 异常处理 (如有)
UPDATE sys_menu SET tag_label = '历史', update_time = CURRENT_TIMESTAMP WHERE menu_code = 'delivery:exception' AND deleted = 0;

-- 仓储: 收货管理
UPDATE sys_menu SET display_mode = 1, list_path = 'wms/receipt', tag_label = '历史', update_time = CURRENT_TIMESTAMP WHERE menu_code = 'wms:receipt' AND deleted = 0;
-- 仓储: 上架管理
UPDATE sys_menu SET display_mode = 1, list_path = 'wms/putaway', tag_label = '历史', update_time = CURRENT_TIMESTAMP WHERE menu_code = 'wms:putaway' AND deleted = 0;
-- 仓储: 拣货管理
UPDATE sys_menu SET display_mode = 1, list_path = 'wms/pick', tag_label = '历史', update_time = CURRENT_TIMESTAMP WHERE menu_code = 'wms:pick' AND deleted = 0;
-- 仓储: 波次管理
UPDATE sys_menu SET display_mode = 1, list_path = 'wms/wave', tag_label = '历史', update_time = CURRENT_TIMESTAMP WHERE menu_code = 'wms:wave' AND deleted = 0;
-- 仓储: 发货管理
UPDATE sys_menu SET display_mode = 1, list_path = 'wms/ship', tag_label = '历史', update_time = CURRENT_TIMESTAMP WHERE menu_code = 'wms:ship' AND deleted = 0;
-- 仓储: 移库单 (库内移库)
UPDATE sys_menu SET display_mode = 1, list_path = 'wms/move', tag_label = '历史', update_time = CURRENT_TIMESTAMP WHERE menu_code = 'wms:move' AND deleted = 0;
-- 仓储: 盘点管理
UPDATE sys_menu SET display_mode = 1, list_path = 'wms/check', tag_label = '历史', update_time = CURRENT_TIMESTAMP WHERE menu_code = 'wms:check' AND deleted = 0;

-- 商城: 用户审核 → tag_label
UPDATE sys_menu SET display_mode = 1, list_path = 'mall/user-audit', tag_label = '历史', update_time = CURRENT_TIMESTAMP WHERE menu_code = 'mall:user-audit' AND deleted = 0;

-- ============================================================
-- Part 3: 更新 flyway_schema_history 记录
-- ============================================================
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
SELECT COALESCE(MAX(installed_rank), 0) + 1, '6.21.0', 'Complete Mega Menu Leaf Items', 'SQL', 'V6.21.0__Complete_Mega_Menu_Leaf_Items.sql', NULL, 'manual', CURRENT_TIMESTAMP, 0, true
FROM flyway_schema_history
WHERE NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '6.21.0');

-- 同时补齐中间缺失的 flyway 记录
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
SELECT COALESCE(MAX(installed_rank), 0) + 1, v.ver, v.descr, 'SQL', v.script, NULL, 'manual', CURRENT_TIMESTAMP, 0, true
FROM (VALUES
  ('6.11.0', 'Fix Business Metric Id And Print Chain Item', 'V6.11.0__Fix_Business_Metric_Id_And_Print_Chain_Item.sql'),
  ('6.12.0', 'Mega Menu Schema Extension', 'V6.12.0__Mega_Menu_Schema_Extension.sql'),
  ('6.13.0', 'Mega Menu Seed Data', 'V6.13.0__Mega_Menu_Seed_Data.sql'),
  ('6.14.0', 'Complete Admin Management Tables', 'V6.14.0__Complete_Admin_Management_Tables.sql'),
  ('6.15.0', 'Fix System Menu Component Paths', 'V6.15.0__Fix_System_Menu_Component_Paths.sql'),
  ('6.17.0', 'Create Tenant Package Quota Template Tables', 'V6.17.0__Create_Tenant_Package_Quota_Template_Tables.sql'),
  ('6.18.0', 'Create Audit Log Table', 'V6.18.0__Create_Audit_Log_Table.sql'),
  ('6.19.0', 'Create Alert Rule Table', 'V6.19.0__Create_Alert_Rule_Table.sql'),
  ('6.20.0', 'Create Report Schedule Tables', 'V6.20.0__Create_Report_Schedule_Tables.sql')
) AS v(ver, descr, script)
WHERE NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = v.ver);
