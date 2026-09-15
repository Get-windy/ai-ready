-- ============================================================
-- V6.22.0 Fill All Remaining Empty Mega Menu Columns
-- ============================================================

-- ============================================================
-- PART 1: INSERT missing leaf items
-- ============================================================

-- === 销售 > 订单中心 (60105) ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80050, 0, 60105, '订单处理中心', 'sales:order-center', 1, 'sales/order-center', 'views/common/placeholder/index.vue', NULL, 1, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 仓储 > 借进借出 (60305) - 补充借进单/借出单 ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80010, 0, 60305, '借进单', 'wh:borrow-in', 1, 'wh/borrow-in/form', 'views/common/placeholder/index.vue', NULL, 1, 1, 'wh/borrow-in', '历史', 0, NULL, NULL, 3, 1, 1),
(80011, 0, 60305, '借出单', 'wh:borrow-out', 1, 'wh/borrow-out/form', 'views/common/placeholder/index.vue', NULL, 2, 1, 'wh/borrow-out', '历史', 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 仓储 > 收货作业 (60306) ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80012, 0, 60306, '收货单', 'wh:receiving-order', 1, 'wh/receiving-order/form', 'views/common/placeholder/index.vue', NULL, 1, 1, 'wh/receiving-order', '历史', 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 仓储 > 上架作业 (60307) ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80013, 0, 60307, '上架单', 'wh:putaway-order', 1, 'wh/putaway-order/form', 'views/common/placeholder/index.vue', NULL, 1, 1, 'wh/putaway-order', '历史', 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 仓储 > 拣货作业 (60308) ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80014, 0, 60308, '拣货单', 'wh:picking-order', 1, 'wh/picking-order/form', 'views/common/placeholder/index.vue', NULL, 1, 1, 'wh/picking-order', '历史', 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 仓储 > 发货作业 (60309) ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80015, 0, 60309, '发货单', 'wh:shipping-order', 1, 'wh/shipping-order/form', 'views/common/placeholder/index.vue', NULL, 1, 1, 'wh/shipping-order', '历史', 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 仓储 > 库存作业 (60310) ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80016, 0, 60310, '移库单', 'wh:move-order', 1, 'wh/move-order/form', 'views/common/placeholder/index.vue', NULL, 1, 1, 'wh/move-order', '历史', 0, NULL, NULL, 3, 1, 1),
(80017, 0, 60310, '盘点作业单', 'wh:inventory-order', 1, 'wh/inventory-order/form', 'views/common/placeholder/index.vue', NULL, 2, 1, 'wh/inventory-order', '历史', 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 财务 > 收款 (60601) ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80100, 0, 60601, '按单收款', 'finance:receipt-by-doc', 1, 'finance/receipt-by-doc', 'views/common/placeholder/index.vue', NULL, 1, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80101, 0, 60601, '收款单', 'finance:receipt-doc', 1, 'finance/receipt-doc/form', 'views/common/placeholder/index.vue', NULL, 2, 1, 'finance/receipt-doc', '历史', 0, NULL, NULL, 3, 1, 1),
(80102, 0, 60601, '预收款单', 'finance:advance-receipt', 1, 'finance/advance-receipt/form', 'views/common/placeholder/index.vue', NULL, 3, 1, 'finance/advance-receipt', '历史', 0, NULL, NULL, 3, 1, 1),
(80103, 0, 60601, '提现存现转款', 'finance:cash-transfer', 1, 'finance/cash-transfer/form', 'views/common/placeholder/index.vue', NULL, 4, 1, 'finance/cash-transfer', '历史', 0, NULL, NULL, 3, 1, 1),
(80104, 0, 60601, '待确认款项', 'finance:pending-confirm', 1, 'finance/pending-confirm', 'views/common/placeholder/index.vue', NULL, 5, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80105, 0, 60601, '在线支付对账单', 'finance:online-payment-reconcile', 1, 'finance/online-payment-reconcile', 'views/common/placeholder/index.vue', NULL, 6, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 财务 > 付款 (60602) ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80110, 0, 60602, '按单付款', 'finance:payment-by-doc', 1, 'finance/payment-by-doc', 'views/common/placeholder/index.vue', NULL, 1, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80111, 0, 60602, '付款单', 'finance:payment-doc', 1, 'finance/payment-doc/form', 'views/common/placeholder/index.vue', NULL, 2, 1, 'finance/payment-doc', '历史', 0, NULL, NULL, 3, 1, 1),
(80112, 0, 60602, '预付款单', 'finance:advance-payment', 1, 'finance/advance-payment/form', 'views/common/placeholder/index.vue', NULL, 3, 1, 'finance/advance-payment', '历史', 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 财务 > 收入支出 (60603) - 补充 ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80115, 0, 60603, '费用单', 'finance:expense-doc', 1, 'finance/expense-doc/form', 'views/common/placeholder/index.vue', NULL, 1, 1, 'finance/expense-doc', '历史', 0, NULL, NULL, 3, 1, 1),
(80116, 0, 60603, '其他收入', 'finance:other-income-doc', 1, 'finance/other-income-doc/form', 'views/common/placeholder/index.vue', NULL, 2, 1, 'finance/other-income-doc', '历史', 0, NULL, NULL, 3, 1, 1),
(80117, 0, 60603, '应收应付调整', 'finance:ar-ap-adjust', 1, 'finance/ar-ap-adjust/form', 'views/common/placeholder/index.vue', NULL, 3, 1, 'finance/ar-ap-adjust', '历史', 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 财务 > 账务处理 (60604) ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80120, 0, 60604, '会计凭证', 'finance:voucher', 1, 'finance/voucher/form', 'views/common/placeholder/index.vue', NULL, 1, 1, 'finance/voucher', '历史', 0, NULL, NULL, 3, 1, 1),
(80121, 0, 60604, '月结', 'finance:month-closing', 1, 'finance/month-closing', 'views/common/placeholder/index.vue', NULL, 2, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 财务 > 资产管理 (60607) ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80125, 0, 60607, '资产列表', 'finance:asset-list', 1, 'finance/asset-list', 'views/common/placeholder/index.vue', NULL, 1, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80126, 0, 60607, '资产折旧', 'finance:asset-depreciation', 1, 'finance/asset-depreciation', 'views/common/placeholder/index.vue', NULL, 2, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 财务 > 预算管理 (60608) ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80130, 0, 60608, '预算编制', 'finance:budget-plan', 1, 'finance/budget-plan', 'views/common/placeholder/index.vue', NULL, 1, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80131, 0, 60608, '预算执行', 'finance:budget-exec', 1, 'finance/budget-exec', 'views/common/placeholder/index.vue', NULL, 2, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === CRM > 客户管理 (60701) - 补充 ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80200, 0, 60701, '客户', 'crm:customer', 1, 'crm/customer', 'views/common/placeholder/index.vue', NULL, 1, 1, 'crm/customer/add', '添加', 0, NULL, NULL, 3, 1, 1),
(80201, 0, 60701, '客户跟进', 'crm:customer-follow', 1, 'crm/customer-follow', 'views/common/placeholder/index.vue', NULL, 2, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === CRM > 线索管理 (60702) - 补充 ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80210, 0, 60702, '线索', 'crm:lead', 1, 'crm/lead', 'views/common/placeholder/index.vue', NULL, 1, 1, 'crm/lead/add', '添加', 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === CRM > 商机管理 (60703) - 补充 ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80220, 0, 60703, '商机', 'crm:opportunity', 1, 'crm/opportunity', 'views/common/placeholder/index.vue', NULL, 1, 1, 'crm/opportunity/add', '添加', 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === CRM > 合同管理 (60705) - 补充 ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80230, 0, 60705, '合同', 'crm:contract', 1, 'crm/contract', 'views/common/placeholder/index.vue', NULL, 1, 1, 'crm/contract/add', '添加', 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 营销 > 会员中心 (60801) ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80300, 0, 60801, '会员管理', 'mkt:member-manage', 1, 'marketing/member-manage', 'views/common/placeholder/index.vue', NULL, 1, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80301, 0, 60801, '积分兑换', 'mkt:points-exchange', 1, 'marketing/points-exchange', 'views/common/placeholder/index.vue', NULL, 2, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80302, 0, 60801, '会员设置', 'mkt:member-config', 1, 'marketing/member-config', 'views/common/placeholder/index.vue', NULL, 3, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 营销 > 营销活动 (60802) ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80310, 0, 60802, '发短信', 'mkt:sms-send', 1, 'marketing/sms-send', 'views/common/placeholder/index.vue', NULL, 1, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80311, 0, 60802, '优惠券', 'mkt:coupon', 1, 'marketing/coupon', 'views/common/placeholder/index.vue', NULL, 2, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80312, 0, 60802, '商品促销', 'mkt:product-promo', 1, 'marketing/product-promo', 'views/common/placeholder/index.vue', NULL, 3, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80313, 0, 60802, '整单促销', 'mkt:order-promo', 1, 'marketing/order-promo', 'views/common/placeholder/index.vue', NULL, 4, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80314, 0, 60802, '特价', 'mkt:special-price', 1, 'marketing/special-price', 'views/common/placeholder/index.vue', NULL, 5, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80315, 0, 60802, '套餐', 'mkt:package-deal', 1, 'marketing/package-deal', 'views/common/placeholder/index.vue', NULL, 6, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 营销 > 商城营销 (60803) ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80320, 0, 60803, '商城拼团', 'mkt:mall-group', 1, 'marketing/mall-group', 'views/common/placeholder/index.vue', NULL, 1, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80321, 0, 60803, '商城秒杀', 'mkt:mall-flash', 1, 'marketing/mall-flash', 'views/common/placeholder/index.vue', NULL, 2, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80322, 0, 60803, '商城预售', 'mkt:mall-presale', 1, 'marketing/mall-presale', 'views/common/placeholder/index.vue', NULL, 3, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80323, 0, 60803, '商城弹窗广告', 'mkt:mall-popup', 1, 'marketing/mall-popup', 'views/common/placeholder/index.vue', NULL, 4, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80324, 0, 60803, '加价购', 'mkt:add-on-deal', 1, 'marketing/add-on-deal', 'views/common/placeholder/index.vue', NULL, 5, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80325, 0, 60803, '热门搜索词推荐', 'mkt:hot-keywords', 1, 'marketing/hot-keywords', 'views/common/placeholder/index.vue', NULL, 6, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 营销 > 营销推广 (60804) ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80330, 0, 60804, '我要推广', 'mkt:promote-create', 1, 'marketing/promote-create', 'views/common/placeholder/index.vue', NULL, 1, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80331, 0, 60804, '推广历史查询', 'mkt:promote-history', 1, 'marketing/promote-history', 'views/common/placeholder/index.vue', NULL, 2, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 商城 > 订单处理 (60901) ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80350, 0, 60901, '订单处理', 'mall:order-process', 1, 'mall/order-process', 'views/common/placeholder/index.vue', NULL, 1, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80351, 0, 60901, '退货申请处理', 'mall:return-process', 1, 'mall/return-process', 'views/common/placeholder/index.vue', NULL, 2, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 商城 > 基础业务 (60902) ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80360, 0, 60902, '商品上架', 'mall:product-shelf', 1, 'mall/product-shelf', 'views/common/placeholder/index.vue', NULL, 1, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80361, 0, 60902, '单位显示', 'mall:unit-display', 1, 'mall/unit-display', 'views/common/placeholder/index.vue', NULL, 2, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80362, 0, 60902, '买家申请管理', 'mall:buyer-apply', 1, 'mall/buyer-apply', 'views/common/placeholder/index.vue', NULL, 3, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80363, 0, 60902, '买家账号', 'mall:buyer-account', 1, 'mall/buyer-account', 'views/common/placeholder/index.vue', NULL, 4, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80364, 0, 60902, '商品组合', 'mall:product-combo', 1, 'mall/product-combo', 'views/common/placeholder/index.vue', NULL, 5, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 商城 > 商城设置 (60903) ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80370, 0, 60903, '基础设置', 'mall:basic-config', 1, 'mall/basic-config', 'views/common/placeholder/index.vue', NULL, 1, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80371, 0, 60903, '店铺设置', 'mall:shop-config', 1, 'mall/shop-config', 'views/common/placeholder/index.vue', NULL, 2, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80372, 0, 60903, '运费设置', 'mall:freight-config', 1, 'mall/freight-config', 'views/common/placeholder/index.vue', NULL, 3, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80373, 0, 60903, '商城装修', 'mall:shop-decoration', 1, 'mall/shop-decoration', 'views/common/placeholder/index.vue', NULL, 4, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80374, 0, 60903, '公告设置', 'mall:notice-config', 1, 'mall/notice-config', 'views/common/placeholder/index.vue', NULL, 5, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80375, 0, 60903, '关键词库', 'mall:keyword-bank', 1, 'mall/keyword-bank', 'views/common/placeholder/index.vue', NULL, 6, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 分析 > 综合单据 (61001) - with subgroup header ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80400, 0, 61001, '单据中心', 'ana:doc-center-group', 0, NULL, NULL, NULL, 0, 0, NULL, NULL, 1, NULL, NULL, 3, 1, 1),
(80401, 0, 61001, '待审批单据', 'ana:pending-approval', 1, 'analytics/pending-approval', 'views/common/placeholder/index.vue', NULL, 1, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80402, 0, 61001, '业务草稿', 'ana:draft', 1, 'analytics/draft', 'views/common/placeholder/index.vue', NULL, 2, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80403, 0, 61001, '经营历程', 'ana:business-history', 1, 'analytics/business-history', 'views/common/placeholder/index.vue', NULL, 3, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 分析 > 采销分析 (61002) - with subgroup headers ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80410, 0, 61002, '销售业绩', 'ana:sales-perf-group', 0, NULL, NULL, NULL, 1, 0, NULL, NULL, 1, NULL, NULL, 3, 1, 1),
(80411, 0, 61002, '销售业绩', 'ana:sales-perf', 1, 'analytics/sales-performance', 'views/common/placeholder/index.vue', NULL, 2, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80412, 0, 61002, '销售分析', 'ana:sales-analysis-group', 0, NULL, NULL, NULL, 3, 0, NULL, NULL, 1, NULL, NULL, 3, 1, 1),
(80413, 0, 61002, '销售分析', 'ana:sales-analysis', 1, 'analytics/sales-analysis', 'views/common/placeholder/index.vue', NULL, 4, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80414, 0, 61002, '销售履约分析', 'ana:sales-fulfillment', 1, 'analytics/sales-fulfillment', 'views/common/placeholder/index.vue', NULL, 5, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80415, 0, 61002, '销售欠款分析', 'ana:sales-debt', 1, 'analytics/sales-debt', 'views/common/placeholder/index.vue', NULL, 6, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80416, 0, 61002, '销售费用分析', 'ana:sales-expense', 1, 'analytics/sales-expense', 'views/common/placeholder/index.vue', NULL, 7, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80417, 0, 61002, '客户活跃分析', 'ana:customer-active', 1, 'analytics/customer-active', 'views/common/placeholder/index.vue', NULL, 8, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80418, 0, 61002, '推广分析', 'ana:promotion-analysis', 1, 'analytics/promotion-analysis', 'views/common/placeholder/index.vue', NULL, 9, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80419, 0, 61002, '预订货查询', 'ana:pre-order-query', 1, 'analytics/pre-order-query', 'views/common/placeholder/index.vue', NULL, 10, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80420, 0, 61002, '采购分析', 'ana:purchase-analysis-group', 0, NULL, NULL, NULL, 11, 0, NULL, NULL, 1, NULL, NULL, 3, 1, 1),
(80421, 0, 61002, '采购分析', 'ana:purchase-analysis', 1, 'analytics/purchase-analysis', 'views/common/placeholder/index.vue', NULL, 12, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80422, 0, 61002, '采购/准备', 'ana:purchase-prep', 1, 'analytics/purchase-prep', 'views/common/placeholder/index.vue', NULL, 13, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 分析 > 仓配分析 (61003) - with subgroup header ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80430, 0, 61003, '仓储分析', 'ana:wh-analysis-group', 0, NULL, NULL, NULL, 1, 0, NULL, NULL, 1, NULL, NULL, 3, 1, 1),
(80431, 0, 61003, '查库存', 'ana:check-stock', 1, 'analytics/check-stock', 'views/common/placeholder/index.vue', NULL, 2, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80432, 0, 61003, '查批次', 'ana:check-batch', 1, 'analytics/check-batch', 'views/common/placeholder/index.vue', NULL, 3, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80433, 0, 61003, '进销存分析', 'ana:inventory-analysis', 1, 'analytics/inventory-analysis', 'views/common/placeholder/index.vue', NULL, 4, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80434, 0, 61003, '库存明细', 'ana:stock-detail', 1, 'analytics/stock-detail', 'views/common/placeholder/index.vue', NULL, 5, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 分析 > 提成分析 (61004) - with subgroup header ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80440, 0, 61004, '提成统计', 'ana:commission-group', 0, NULL, NULL, NULL, 1, 0, NULL, NULL, 1, NULL, NULL, 3, 1, 1),
(80441, 0, 61004, '业务员提成', 'ana:staff-commission', 1, 'analytics/staff-commission', 'views/common/placeholder/index.vue', NULL, 2, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80442, 0, 61004, '回款统计', 'ana:collection-stats', 1, 'analytics/collection-stats', 'views/common/placeholder/index.vue', NULL, 3, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80443, 0, 61004, '业绩提成中心', 'ana:commission-center', 1, 'analytics/commission-center', 'views/common/placeholder/index.vue', NULL, 4, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 分析 > 财务分析 (61005) - with subgroup headers ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80450, 0, 61005, '财务报表', 'ana:fin-report-group', 0, NULL, NULL, NULL, 1, 0, NULL, NULL, 1, NULL, NULL, 3, 1, 1),
(80451, 0, 61005, '经营分析', 'ana:business-analysis', 1, 'analytics/business-analysis', 'views/common/placeholder/index.vue', NULL, 2, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80452, 0, 61005, '资金费用', 'ana:fund-expense-group', 0, NULL, NULL, NULL, 3, 0, NULL, NULL, 1, NULL, NULL, 3, 1, 1),
(80453, 0, 61005, '查资金', 'ana:check-fund', 1, 'analytics/check-fund', 'views/common/placeholder/index.vue', NULL, 4, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80454, 0, 61005, '查费用', 'ana:check-expense', 1, 'analytics/check-expense', 'views/common/placeholder/index.vue', NULL, 5, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80455, 0, 61005, '发票统计', 'ana:invoice-stats', 1, 'analytics/invoice-stats', 'views/common/placeholder/index.vue', NULL, 6, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80456, 0, 61005, '往来经营', 'ana:ar-business-group', 0, NULL, NULL, NULL, 7, 0, NULL, NULL, 1, NULL, NULL, 3, 1, 1),
(80457, 0, 61005, '查应收', 'ana:check-receivable', 1, 'analytics/check-receivable', 'views/common/placeholder/index.vue', NULL, 8, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80458, 0, 61005, '查应付', 'ana:check-payable', 1, 'analytics/check-payable', 'views/common/placeholder/index.vue', NULL, 9, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80459, 0, 61005, '往来余额表', 'ana:ar-balance-sheet', 1, 'analytics/ar-balance-sheet', 'views/common/placeholder/index.vue', NULL, 10, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 分析 > 营销分析 (61006) - with subgroup headers ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80470, 0, 61006, '营销分析', 'ana:mkt-analysis-group', 0, NULL, NULL, NULL, 1, 0, NULL, NULL, 1, NULL, NULL, 3, 1, 1),
(80471, 0, 61006, '营销活动分析', 'ana:mkt-activity-analysis', 1, 'analytics/mkt-activity-analysis', 'views/common/placeholder/index.vue', NULL, 2, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80472, 0, 61006, '营销推广分析', 'ana:mkt-promote-analysis', 1, 'analytics/mkt-promote-analysis', 'views/common/placeholder/index.vue', NULL, 3, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80473, 0, 61006, '商城分析', 'ana:mall-analysis-group', 0, NULL, NULL, NULL, 4, 0, NULL, NULL, 1, NULL, NULL, 3, 1, 1),
(80474, 0, 61006, '交易分析', 'ana:trade-analysis', 1, 'analytics/trade-analysis', 'views/common/placeholder/index.vue', NULL, 5, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80475, 0, 61006, '商城客户列表', 'ana:mall-customer-list', 1, 'analytics/mall-customer-list', 'views/common/placeholder/index.vue', NULL, 6, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 资料 > 商品管理 (61101) - 补充商品主入口 ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80500, 0, 61101, '商品', 'md:product', 1, 'md/product', 'views/common/placeholder/index.vue', NULL, 0, 1, 'md/product/add', '添加', 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 资料 > 往来单位 (61102) - 补充客户/供应商/物流公司 ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80510, 0, 61102, '客户', 'md:customer', 1, 'md/customer', 'views/common/placeholder/index.vue', NULL, 1, 1, 'md/customer/add', '添加', 0, NULL, NULL, 3, 1, 1),
(80511, 0, 61102, '供应商', 'md:supplier', 1, 'md/supplier', 'views/common/placeholder/index.vue', NULL, 2, 1, 'md/supplier/add', '添加', 0, NULL, NULL, 3, 1, 1),
(80512, 0, 61102, '物流公司', 'md:logistics', 1, 'md/logistics', 'views/common/placeholder/index.vue', NULL, 3, 1, 'md/logistics/add', '添加', 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 资料 > 仓库管理 (61103) - 补充仓库规划 ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80520, 0, 61103, '仓库规划', 'md:warehouse-plan', 1, 'md/warehouse-plan', 'views/common/placeholder/index.vue', NULL, 1, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 资料 > 职员权限 (61105) ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80530, 0, 61105, '职员部门', 'md:staff-dept', 1, 'md/staff-dept', 'views/common/placeholder/index.vue', NULL, 1, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80531, 0, 61105, '岗位权限', 'md:staff-role', 1, 'md/staff-role', 'views/common/placeholder/index.vue', NULL, 2, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80532, 0, 61105, '全部操作员', 'md:staff-all', 1, 'md/staff-all', 'views/common/placeholder/index.vue', NULL, 3, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 设置 > 打印管理 (61205) ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80600, 0, 61205, '打印模板', 'set:print-template', 1, 'set/print-template', 'views/common/placeholder/index.vue', NULL, 1, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 设置 > 工作流 (61206) ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80610, 0, 61206, '流程设计', 'set:workflow-designer', 1, 'set/workflow-designer', 'views/common/placeholder/index.vue', NULL, 1, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 设置 > 系统配置 (61201) - 补充 ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80620, 0, 61201, '菜单配置', 'set:menu-config', 1, 'set/menu-config', 'views/common/placeholder/index.vue', NULL, 1, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80621, 0, 61201, '系统参数', 'set:sys-params', 1, 'set/sys-params', 'views/common/placeholder/index.vue', NULL, 2, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80622, 0, 61201, '审核设置', 'set:audit-config', 1, 'set/audit-config', 'views/common/placeholder/index.vue', NULL, 4, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80623, 0, 61201, '支付配置', 'set:payment-config', 1, 'set/payment-config', 'views/common/placeholder/index.vue', NULL, 5, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80624, 0, 61201, '企业信息', 'set:company-info', 1, 'set/company-info', 'views/common/placeholder/index.vue', NULL, 6, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1),
(80625, 0, 61201, '应用中心', 'set:app-center', 1, 'set/app-center', 'views/common/placeholder/index.vue', NULL, 7, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 设置 > 账套操作 (61203) - 补充操作日志 ===
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80630, 0, 61203, '操作日志', 'set:operation-log', 1, 'set/operation-log', 'views/common/placeholder/index.vue', NULL, 2, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 配送 (60005) modules ===
-- 线路管理 (60501)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80700, 0, 60501, '线路列表', 'dms:route-list', 1, 'dms/route-list', 'views/common/placeholder/index.vue', NULL, 1, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- 车辆管理 (60502)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80710, 0, 60502, '车辆列表', 'dms:vehicle-list', 1, 'dms/vehicle-list', 'views/common/placeholder/index.vue', NULL, 1, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- 骑手管理 (60503)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80720, 0, 60503, '骑手列表', 'dms:rider-list', 1, 'dms/rider-list', 'views/common/placeholder/index.vue', NULL, 1, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- 调度管理 (60504)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80730, 0, 60504, '调度任务', 'dms:dispatch-task', 1, 'dms/dispatch-task', 'views/common/placeholder/index.vue', NULL, 1, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- 配送跟踪 (60505)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80740, 0, 60505, '实时跟踪', 'dms:realtime-tracking', 1, 'dms/realtime-tracking', 'views/common/placeholder/index.vue', NULL, 1, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- 配送配置 (60506)
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80750, 0, 60506, '配送参数', 'dms:config-params', 1, 'dms/config-params', 'views/common/placeholder/index.vue', NULL, 1, 0, NULL, NULL, 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- === 配送 > 配发收模块 (60004) 补充 ===
-- 配送业务 (60401) 补充配送单
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80760, 0, 60401, '配送单', 'dispatch:dispatch-order', 1, 'dispatch/dispatch-order/form', 'views/common/placeholder/index.vue', NULL, 1, 1, 'dispatch/dispatch-order', '历史', 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;


-- ============================================================
-- PART 2: Verify counts
-- ============================================================
SELECT 'Column leaf counts after insert:' AS info;

SELECT
    p.id AS column_id,
    p.menu_name AS column_name,
    COUNT(c.id) AS leaf_count
FROM sys_menu p
LEFT JOIN sys_menu c ON c.parent_id = p.id AND c.menu_type = 1
WHERE p.id BETWEEN 60101 AND 61206
GROUP BY p.id, p.menu_name
ORDER BY p.id;
