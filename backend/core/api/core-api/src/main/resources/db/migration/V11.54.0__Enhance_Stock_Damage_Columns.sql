-- ═══════════════════════════════════════════════════════════════════
-- V11.54.0 报损单补列（对标其他入库单/其他出库单结构）
-- 报损单：单号前缀 BSD-，按批次报损出库，与报溢单互为反向单据
-- ═══════════════════════════════════════════════════════════════════

-- ── 主表：补经手人/部门/重量/体积/摘要/附件/记账/制单/打印/取消原因 ──
ALTER TABLE erp_stock_damage ADD COLUMN IF NOT EXISTS handler_id BIGINT;
ALTER TABLE erp_stock_damage ADD COLUMN IF NOT EXISTS handler_name VARCHAR(64);
ALTER TABLE erp_stock_damage ADD COLUMN IF NOT EXISTS dept_id BIGINT;
ALTER TABLE erp_stock_damage ADD COLUMN IF NOT EXISTS dept_name VARCHAR(100);
ALTER TABLE erp_stock_damage ADD COLUMN IF NOT EXISTS total_weight NUMERIC(18,4) DEFAULT 0;
ALTER TABLE erp_stock_damage ADD COLUMN IF NOT EXISTS total_volume NUMERIC(18,4) DEFAULT 0;
ALTER TABLE erp_stock_damage ADD COLUMN IF NOT EXISTS summary VARCHAR(500);
ALTER TABLE erp_stock_damage ADD COLUMN IF NOT EXISTS attachment VARCHAR(1000);
ALTER TABLE erp_stock_damage ADD COLUMN IF NOT EXISTS bookkeeper_id BIGINT;
ALTER TABLE erp_stock_damage ADD COLUMN IF NOT EXISTS bookkeeper_name VARCHAR(64);
ALTER TABLE erp_stock_damage ADD COLUMN IF NOT EXISTS bookkeeping_time TIMESTAMP;
ALTER TABLE erp_stock_damage ADD COLUMN IF NOT EXISTS creator_name VARCHAR(64);
ALTER TABLE erp_stock_damage ADD COLUMN IF NOT EXISTS print_count INT DEFAULT 0;
ALTER TABLE erp_stock_damage ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(200);

CREATE INDEX IF NOT EXISTS idx_stock_damage_handler  ON erp_stock_damage (handler_id);
CREATE INDEX IF NOT EXISTS idx_stock_damage_dept     ON erp_stock_damage (dept_id);
CREATE INDEX IF NOT EXISTS idx_stock_damage_no       ON erp_stock_damage (damage_no);

-- ── 明细表：补型号/产地/品牌/区域/批次条码/换算结果/小单位/重量体积/自定义/图片 ──
ALTER TABLE erp_stock_damage_item ADD COLUMN IF NOT EXISTS model VARCHAR(64);
ALTER TABLE erp_stock_damage_item ADD COLUMN IF NOT EXISTS origin VARCHAR(64);
ALTER TABLE erp_stock_damage_item ADD COLUMN IF NOT EXISTS brand VARCHAR(64);
ALTER TABLE erp_stock_damage_item ADD COLUMN IF NOT EXISTS region VARCHAR(64);
ALTER TABLE erp_stock_damage_item ADD COLUMN IF NOT EXISTS batch_code VARCHAR(64);
ALTER TABLE erp_stock_damage_item ADD COLUMN IF NOT EXISTS conversion_result NUMERIC(18,4);
ALTER TABLE erp_stock_damage_item ADD COLUMN IF NOT EXISTS small_unit VARCHAR(20);
ALTER TABLE erp_stock_damage_item ADD COLUMN IF NOT EXISTS small_unit_quantity NUMERIC(18,4);
ALTER TABLE erp_stock_damage_item ADD COLUMN IF NOT EXISTS small_unit_price NUMERIC(18,2);
ALTER TABLE erp_stock_damage_item ADD COLUMN IF NOT EXISTS weight NUMERIC(18,4);
ALTER TABLE erp_stock_damage_item ADD COLUMN IF NOT EXISTS volume NUMERIC(18,4);
ALTER TABLE erp_stock_damage_item ADD COLUMN IF NOT EXISTS item_ext_num1 NUMERIC(18,4);
ALTER TABLE erp_stock_damage_item ADD COLUMN IF NOT EXISTS item_ext_num2 NUMERIC(18,4);
ALTER TABLE erp_stock_damage_item ADD COLUMN IF NOT EXISTS item_ext_num3 NUMERIC(18,4);
ALTER TABLE erp_stock_damage_item ADD COLUMN IF NOT EXISTS item_ext_text1 VARCHAR(200);
ALTER TABLE erp_stock_damage_item ADD COLUMN IF NOT EXISTS item_ext_text2 VARCHAR(200);
ALTER TABLE erp_stock_damage_item ADD COLUMN IF NOT EXISTS image VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_stock_damage_item_parent  ON erp_stock_damage_item (damage_id);
CREATE INDEX IF NOT EXISTS idx_stock_damage_item_product ON erp_stock_damage_item (product_id);
