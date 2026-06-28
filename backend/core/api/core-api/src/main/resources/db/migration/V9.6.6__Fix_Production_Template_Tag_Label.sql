-- ============================================================
-- V9.6.6: 修复双入口菜单缺失的 tag_label
--
-- 背景：
--   多次迁移（V9.4/V9.6.3）重建菜单项时遗漏了 tag_label 字段。
--   设计文档定义了 3 种标签：
--     [历史] = 有新建+列表双入口的单据类业务（销售订单、采购订单等）
--     [列表] = 同[历史]，文案不同（生产模板）
--     [添加] = 有详情+新增双入口的基础资料（商品、客户等）
--
-- 修复范围：
--   1. 生产模板(5014): 遗漏 tag_label='列表'
--   2. 其他出库单(5001): 遗漏 tag_label='历史' + list_path
--   3. WMS 作业单据(80010-80017): 遗漏 tag_label='历史' + list_path
-- ============================================================

-- Step 1: 生产模板 → [列表]
UPDATE sys_menu
SET tag_label = '列表', list_path = 'wh/production-template', update_time = CURRENT_TIMESTAMP
WHERE id = 5014 AND display_mode = 1;

-- Step 2: 其他出库单 → [历史]
UPDATE sys_menu
SET tag_label = '历史', list_path = 'erp/stock-out', update_time = CURRENT_TIMESTAMP
WHERE id = 5001 AND display_mode = 1;

-- Step 3: WMS 作业单据(80010-80017) → [历史]
UPDATE sys_menu SET tag_label = '历史', list_path = 'wms/borrow-in',   update_time = CURRENT_TIMESTAMP WHERE id = 80010;
UPDATE sys_menu SET tag_label = '历史', list_path = 'wms/borrow-out',  update_time = CURRENT_TIMESTAMP WHERE id = 80011;
UPDATE sys_menu SET tag_label = '历史', list_path = 'wms/receive',     update_time = CURRENT_TIMESTAMP WHERE id = 80012;
UPDATE sys_menu SET tag_label = '历史', list_path = 'wms/putaway',     update_time = CURRENT_TIMESTAMP WHERE id = 80013;
UPDATE sys_menu SET tag_label = '历史', list_path = 'wms/pick',        update_time = CURRENT_TIMESTAMP WHERE id = 80014;
UPDATE sys_menu SET tag_label = '历史', list_path = 'wms/ship',        update_time = CURRENT_TIMESTAMP WHERE id = 80015;
UPDATE sys_menu SET tag_label = '历史', list_path = 'wms/move',        update_time = CURRENT_TIMESTAMP WHERE id = 80016;
UPDATE sys_menu SET tag_label = '历史', list_path = 'wms/check',       update_time = CURRENT_TIMESTAMP WHERE id = 80017;
