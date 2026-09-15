-- ============================================================
-- V11.17.0: erp_sale_order_item 补齐明细扩展列（孤儿脚本差额收编）
--
-- 背景：
--   根目录孤儿脚本 db/migration/V20260622100000__add_sale_order_item_extended_fields.sql
--   不在应用 Flyway locations 内，从未执行；且为 MySQL 语法（COMMENT），无法在 PG 运行。
--   经逐列比对 devdb，下列列在 erp_sale_order_item 中缺失，而 SaleOrderItem 实体
--   已映射这些字段（MP 默认 insert/select 全字段，非空时直接 SQL 报错），属于真实差额。
--   已存在列不重复添加：restaurant/canteen/vip_self/large_group/special_customer/
--   out_restaurant（V9.52.0 已建），vip_level1/vip_level2（孤儿脚本名为 vip_level_1/2，
--   实际库与实体均为 vip_level1/2，按现有命名为准）。
-- ============================================================

-- 商品相关
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS image VARCHAR(255);
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS pre_order_no VARCHAR(50);
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS small_unit_barcode VARCHAR(50);
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS use_pre_order_amount DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS model VARCHAR(100);
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS area VARCHAR(100);
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS origin VARCHAR(100);
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS brand VARCHAR(100);

-- 单据自定义字段（1-10）
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS custom_field_1 DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS custom_field_2 DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS custom_field_3 DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS custom_field_4 VARCHAR(255);
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS custom_field_5 VARCHAR(255);
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS custom_field_6 DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS custom_field_7 DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS custom_field_8 BIGINT;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS custom_field_9 BIGINT;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS custom_field_10 BIGINT;

-- 小单位
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS small_unit VARCHAR(50);
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS small_unit_price DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS small_unit_quantity DECIMAL(18,2) DEFAULT 0;

-- 价格
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS latest_sale_date DATE;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS latest_sale_price DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS retail_price DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS wholesale_price DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS lowest_price DECIMAL(18,2) DEFAULT 0;

-- 库存快照
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS available_stock DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS available_stock_converted DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS book_stock DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS conversion_relation VARCHAR(100);
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS unshipped_quantity DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS shipped_quantity_detail DECIMAL(18,2) DEFAULT 0;

-- 成本/毛利
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS cost_price DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS cost_amount DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS gross_profit DECIMAL(18,2) DEFAULT 0;

-- 折扣
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS discount_percent DECIMAL(5,2) DEFAULT 0;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS discounted_unit_price DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS original_price DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS discounted_amount DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS favorable_unit_price DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS favorable_amount DECIMAL(18,2) DEFAULT 0;

-- 积分/礼品
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS gift_item VARCHAR(100);
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS exchange_points DECIMAL(10,2) DEFAULT 0;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS used_points DECIMAL(10,2) DEFAULT 0;

-- 物理属性/赠品
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS volume DECIMAL(12,4) DEFAULT 0;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS weight DECIMAL(12,4) DEFAULT 0;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS gift BOOLEAN DEFAULT FALSE;
