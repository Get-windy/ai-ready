-- ═══════════════════════════════════════════════════════════════════
-- V11.58.0 生产模板（BOM）补列（对标 ql361 生产模板页面）
-- 生产模板：成品物料清单主数据，无单据编号/无状态流，保存即生效
-- 模板头字段：模板名称/成品名称/成品单位/成品数量/口味/型号
-- 明细列字段：图片/商品名称/货号/条码/规格/型号/产地/品牌/计价单位/
--             数量/成本均价/成本金额/小单位/小单位数量/小单位单价/备注/
--             单据自定义1-3(数字)/单据自定义4-5(文本)
-- ═══════════════════════════════════════════════════════════════════

-- ── 主表：补口味/成品型号/条码/产地/品牌（成品商品快照，供列表展示） ──
ALTER TABLE erp_stock_bom ADD COLUMN IF NOT EXISTS taste VARCHAR(64);
ALTER TABLE erp_stock_bom ADD COLUMN IF NOT EXISTS model VARCHAR(64);
ALTER TABLE erp_stock_bom ADD COLUMN IF NOT EXISTS barcode VARCHAR(64);
ALTER TABLE erp_stock_bom ADD COLUMN IF NOT EXISTS origin VARCHAR(64);
ALTER TABLE erp_stock_bom ADD COLUMN IF NOT EXISTS brand VARCHAR(64);

-- ── 明细表：补图片/型号/产地/品牌/条码/小单位/自定义字段 ──
ALTER TABLE erp_stock_bom_item ADD COLUMN IF NOT EXISTS image_url VARCHAR(255);
ALTER TABLE erp_stock_bom_item ADD COLUMN IF NOT EXISTS model VARCHAR(64);
ALTER TABLE erp_stock_bom_item ADD COLUMN IF NOT EXISTS origin VARCHAR(64);
ALTER TABLE erp_stock_bom_item ADD COLUMN IF NOT EXISTS brand VARCHAR(64);
ALTER TABLE erp_stock_bom_item ADD COLUMN IF NOT EXISTS barcode VARCHAR(64);
ALTER TABLE erp_stock_bom_item ADD COLUMN IF NOT EXISTS small_unit VARCHAR(64);
ALTER TABLE erp_stock_bom_item ADD COLUMN IF NOT EXISTS small_unit_qty DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_stock_bom_item ADD COLUMN IF NOT EXISTS small_unit_price DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_stock_bom_item ADD COLUMN IF NOT EXISTS ext_num1 DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_stock_bom_item ADD COLUMN IF NOT EXISTS ext_num2 DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_stock_bom_item ADD COLUMN IF NOT EXISTS ext_num3 DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_stock_bom_item ADD COLUMN IF NOT EXISTS ext_text4 VARCHAR(255);
ALTER TABLE erp_stock_bom_item ADD COLUMN IF NOT EXISTS ext_text5 VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_stock_bom_bom_name     ON erp_stock_bom (bom_name);
CREATE INDEX IF NOT EXISTS idx_stock_bom_item_model   ON erp_stock_bom_item (model);
CREATE INDEX IF NOT EXISTS idx_stock_bom_item_barcode ON erp_stock_bom_item (barcode);
