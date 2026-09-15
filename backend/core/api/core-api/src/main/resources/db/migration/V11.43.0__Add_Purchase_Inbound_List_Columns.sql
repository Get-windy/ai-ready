-- 采购入库单主表补列表页/表单页所需字段
-- 对标开发文档：列表页 31 列、表单页 37 字段
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS settle_status         INTEGER;
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS settled_amount        DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS discount_amount       DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS fee                   DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS weight                DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS volume                DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS summary               VARCHAR(500);
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS ext_num1              DECIMAL(18,2);
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS ext_num2              DECIMAL(18,2);
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS ext_text1             VARCHAR(200);
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS ext_text2             VARCHAR(200);
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS ext_text3             VARCHAR(200);
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS create_by_name        VARCHAR(100);
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS poster_name           VARCHAR(100);
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS post_time             TIMESTAMP;
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS approved_by_name      VARCHAR(100);
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS attachment            VARCHAR(500);
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS print_count           INTEGER DEFAULT 0;
