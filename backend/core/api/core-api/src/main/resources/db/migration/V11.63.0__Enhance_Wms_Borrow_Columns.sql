-- ───────────────────────────────────────────────────────────
-- V11.63.0 借进单对标复刻
-- wms_borrow_order / wms_borrow_order_item 补充借进单业务字段
-- 对齐「借进单开发文档」列表页（按单据24列/按明细51列）与表单页（明细44列）。
-- ───────────────────────────────────────────────────────────

-- ==================== 1. 借进借出单（头表）补充列 ====================
ALTER TABLE wms_borrow_order ADD COLUMN IF NOT EXISTS partner_code VARCHAR(100);
ALTER TABLE wms_borrow_order ADD COLUMN IF NOT EXISTS handler_id BIGINT;
ALTER TABLE wms_borrow_order ADD COLUMN IF NOT EXISTS handler_name VARCHAR(100);
ALTER TABLE wms_borrow_order ADD COLUMN IF NOT EXISTS dept_id BIGINT;
ALTER TABLE wms_borrow_order ADD COLUMN IF NOT EXISTS dept_name VARCHAR(100);
ALTER TABLE wms_borrow_order ADD COLUMN IF NOT EXISTS creator_name VARCHAR(100);
ALTER TABLE wms_borrow_order ADD COLUMN IF NOT EXISTS bookkeeper_id BIGINT;
ALTER TABLE wms_borrow_order ADD COLUMN IF NOT EXISTS bookkeeper_name VARCHAR(100);
ALTER TABLE wms_borrow_order ADD COLUMN IF NOT EXISTS bookkeeping_time TIMESTAMP;
ALTER TABLE wms_borrow_order ADD COLUMN IF NOT EXISTS summary VARCHAR(500);
ALTER TABLE wms_borrow_order ADD COLUMN IF NOT EXISTS attachment VARCHAR(500);
ALTER TABLE wms_borrow_order ADD COLUMN IF NOT EXISTS print_count INTEGER DEFAULT 0;
ALTER TABLE wms_borrow_order ADD COLUMN IF NOT EXISTS borrow_amount DECIMAL(18,2) DEFAULT 0;
ALTER TABLE wms_borrow_order ADD COLUMN IF NOT EXISTS borrow_quantity DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order ADD COLUMN IF NOT EXISTS non_processed_quantity DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order ADD COLUMN IF NOT EXISTS non_processed_amount DECIMAL(18,2) DEFAULT 0;
ALTER TABLE wms_borrow_order ADD COLUMN IF NOT EXISTS convert_purchase_quantity DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order ADD COLUMN IF NOT EXISTS convert_purchase_amount DECIMAL(18,2) DEFAULT 0;
ALTER TABLE wms_borrow_order ADD COLUMN IF NOT EXISTS total_weight DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order ADD COLUMN IF NOT EXISTS total_volume DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order ADD COLUMN IF NOT EXISTS red_flag INTEGER DEFAULT 0;

COMMENT ON COLUMN wms_borrow_order.borrow_amount IS '借进/借出金额';
COMMENT ON COLUMN wms_borrow_order.non_processed_quantity IS '未处理数量';
COMMENT ON COLUMN wms_borrow_order.convert_purchase_quantity IS '借转采购数量';

-- 明细表补充乐观锁（实体继承 BaseEntity，若列缺失会导致查询失败）
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 1;

-- ==================== 2. 借进借出单明细补充分列 ====================
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS barcode VARCHAR(100);
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS model VARCHAR(100);
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS origin VARCHAR(100);
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS region VARCHAR(100);
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS location VARCHAR(100);
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS taste VARCHAR(100);
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS production_date DATE;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS shelf_life VARCHAR(100);
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS expiry_date DATE;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS batch_code VARCHAR(100);
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS conversion_relation VARCHAR(100);
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS conversion_result DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS piece_quantity DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS big_pack DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS mid_pack DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS small_pack DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS small_unit VARCHAR(50);
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS small_unit_quantity DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS small_unit_price DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS retail_price DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS wholesale_price DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS min_price DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS available_stock DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS book_stock DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS weight DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS volume DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS processed_return_quantity DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS processed_purchase_quantity DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS non_processed_quantity DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS non_processed_amount DECIMAL(18,2) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS price_level1 DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS price_level2 DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS price_level3 DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS price_level4 DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS price_level5 DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS price_level6 DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS price_level7 DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS price_level8 DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS item_ext_num1 DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS item_ext_num2 DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS item_ext_num3 DECIMAL(18,4) DEFAULT 0;
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS item_ext_text1 VARCHAR(200);
ALTER TABLE wms_borrow_order_item ADD COLUMN IF NOT EXISTS item_ext_text2 VARCHAR(200);

COMMENT ON COLUMN wms_borrow_order_item.processed_return_quantity IS '已处理数量-还出';
COMMENT ON COLUMN wms_borrow_order_item.processed_purchase_quantity IS '已处理数量-借转采购';
COMMENT ON COLUMN wms_borrow_order_item.non_processed_quantity IS '未处理数量';

-- ==================== 3. 菜单双入口配置修正 ====================
-- 确保 80010「借进单」双入口：主菜单->新增表单(wh/borrow-in/form)，历史标签->列表(wh/borrow-in/index)
UPDATE sys_menu SET
    menu_code   = 'wh:borrow-in',
    path        = 'wh/borrow-in/form',
    list_path   = 'wh/borrow-in/index',
    component   = 'views/wh/borrow-in/form/index.vue',
    display_mode = 1,
    tag_label   = '历史',
    update_time = CURRENT_TIMESTAMP
WHERE id = 80010;
