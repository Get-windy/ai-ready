-- ═══════════════════════════════════════════════════════════════════
-- V11.61.0 拆分单补列（对齐组装单/其他入库单结构，对齐拆分单开发文档）
-- 拆分单：单号前缀 CXD-，成品出库 + 原料入库同单，双明细复合单
-- ═══════════════════════════════════════════════════════════════════

-- ── 主表：补部门/重量/体积/摘要/附件/记账人/记账时间/制单人/本单金额/取消原因 ──
ALTER TABLE erp_stock_split ADD COLUMN IF NOT EXISTS dept_id BIGINT;
ALTER TABLE erp_stock_split ADD COLUMN IF NOT EXISTS dept_name VARCHAR(100);
ALTER TABLE erp_stock_split ADD COLUMN IF NOT EXISTS total_weight NUMERIC(18,4) DEFAULT 0;
ALTER TABLE erp_stock_split ADD COLUMN IF NOT EXISTS total_volume NUMERIC(18,4) DEFAULT 0;
ALTER TABLE erp_stock_split ADD COLUMN IF NOT EXISTS summary VARCHAR(500);
ALTER TABLE erp_stock_split ADD COLUMN IF NOT EXISTS attachment VARCHAR(1000);
ALTER TABLE erp_stock_split ADD COLUMN IF NOT EXISTS bookkeeper_id BIGINT;
ALTER TABLE erp_stock_split ADD COLUMN IF NOT EXISTS bookkeeper_name VARCHAR(64);
ALTER TABLE erp_stock_split ADD COLUMN IF NOT EXISTS bookkeeping_time TIMESTAMP;
ALTER TABLE erp_stock_split ADD COLUMN IF NOT EXISTS creator_name VARCHAR(64);
ALTER TABLE erp_stock_split ADD COLUMN IF NOT EXISTS total_cost NUMERIC(18,4) DEFAULT 0;
ALTER TABLE erp_stock_split ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(200);

CREATE INDEX IF NOT EXISTS idx_stock_split_dept     ON erp_stock_split (dept_id);
CREATE INDEX IF NOT EXISTS idx_stock_split_no       ON erp_stock_split (split_no);
CREATE INDEX IF NOT EXISTS idx_stock_split_handler  ON erp_stock_split (handler_name);

-- ── 明细表：补租户/条码/型号/产地/品牌/区域/货位/自定义/批次/保质/换算/包装/价格/库存/重量体积/图片 ──
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS barcode VARCHAR(64);
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS model VARCHAR(64);
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS origin VARCHAR(64);
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS brand VARCHAR(64);
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS region VARCHAR(64);
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS location VARCHAR(64);
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS item_ext_num1 NUMERIC(18,4);
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS item_ext_num2 NUMERIC(18,4);
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS item_ext_num3 NUMERIC(18,4);
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS item_ext_text1 VARCHAR(200);
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS item_ext_text2 VARCHAR(200);
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS batch_code VARCHAR(64);
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS batch_no VARCHAR(64);
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS production_date DATE;
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS shelf_life VARCHAR(100);
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS expiry_date DATE;
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS conversion_relation VARCHAR(100);
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS conversion_result NUMERIC(18,4) DEFAULT 0;
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS piece_quantity NUMERIC(18,4) DEFAULT 0;
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS big_pack NUMERIC(18,4) DEFAULT 0;
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS mid_pack NUMERIC(18,4) DEFAULT 0;
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS small_pack NUMERIC(18,4) DEFAULT 0;
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS small_unit VARCHAR(20);
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS small_unit_quantity NUMERIC(18,4) DEFAULT 0;
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS small_unit_price NUMERIC(18,2) DEFAULT 0;
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS wholesale_price NUMERIC(18,2) DEFAULT 0;
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS retail_price NUMERIC(18,2) DEFAULT 0;
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS unit_price NUMERIC(18,2) DEFAULT 0;
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS available_stock NUMERIC(18,4) DEFAULT 0;
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS available_stock_converted NUMERIC(18,4) DEFAULT 0;
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS image VARCHAR(500);
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS weight NUMERIC(18,4) DEFAULT 0;
ALTER TABLE erp_stock_split_item ADD COLUMN IF NOT EXISTS volume NUMERIC(18,4) DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_stock_split_item_split   ON erp_stock_split_item (split_id);
CREATE INDEX IF NOT EXISTS idx_stock_split_item_product ON erp_stock_split_item (product_id);
