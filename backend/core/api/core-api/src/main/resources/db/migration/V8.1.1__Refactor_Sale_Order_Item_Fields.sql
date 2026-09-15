-- =====================================================
-- V8.1.0: 销售订单明细表生产级重构 - 字段分布优化
-- 将76字段宽表模式重构为分布式存储模式
-- 参考 PRODUCTION_SALE_ORDER_SCHEMA.md 设计方案
-- =====================================================

-- 1. 添加价格等级相关字段
ALTER TABLE erp_sale_order_item
ADD COLUMN IF NOT EXISTS customer_grade_code VARCHAR(20),
ADD COLUMN IF NOT EXISTS customer_grade_name VARCHAR(100),
ADD COLUMN IF NOT EXISTS price_grade_code VARCHAR(20),
ADD COLUMN IF NOT EXISTS price_source VARCHAR(50),
ADD COLUMN IF NOT EXISTS calculated_price DECIMAL(18,6),
ADD COLUMN IF NOT EXISTS discount_applied JSON;

-- 2. 添加索引以优化查询性能
CREATE INDEX IF NOT EXISTS idx_soi_customer_grade ON erp_sale_order_item(customer_grade_code);
CREATE INDEX IF NOT EXISTS idx_soi_price_grade ON erp_sale_order_item(price_grade_code);
CREATE INDEX IF NOT EXISTS idx_soi_product_id ON erp_sale_order_item(product_id);
CREATE INDEX IF NOT EXISTS idx_soi_order_id ON erp_sale_order_item(order_id);

-- 3. 更新表注释
COMMENT ON TABLE erp_sale_order_item IS '销售订单明细表 - 生产级分布式存储';

-- 4. 为新增字段添加注释
COMMENT ON COLUMN erp_sale_order_item.customer_grade_code IS '客户等级代码 - 快照存储';
COMMENT ON COLUMN erp_sale_order_item.customer_grade_name IS '客户等级名称 - 快照存储';
COMMENT ON COLUMN erp_sale_order_item.price_grade_code IS '价格等级代码 - 快照存储';
COMMENT ON COLUMN erp_sale_order_item.price_source IS '价格来源 - 快照存储';
COMMENT ON COLUMN erp_sale_order_item.calculated_price IS '计算得出的单价 - 快照存储';
COMMENT ON COLUMN erp_sale_order_item.discount_applied IS '应用的折扣信息(JSON) - 快照存储';

-- 5. 为现有重要字段添加注释（补充说明分布策略）
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='erp_sale_order_item' AND column_name='image') THEN
        COMMENT ON COLUMN erp_sale_order_item.image IS '图片 - 通过产品ID关联获取';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='erp_sale_order_item' AND column_name='barcode') THEN
        COMMENT ON COLUMN erp_sale_order_item.barcode IS '条码 - 通过产品ID关联获取';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='erp_sale_order_item' AND column_name='origin') THEN
        COMMENT ON COLUMN erp_sale_order_item.origin IS '产地 - 通过产品ID关联获取';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='erp_sale_order_item' AND column_name='brand') THEN
        COMMENT ON COLUMN erp_sale_order_item.brand IS '品牌 - 通过产品ID关联获取';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='erp_sale_order_item' AND column_name='retail_price') THEN
        COMMENT ON COLUMN erp_sale_order_item.retail_price IS '零售价 - 通过产品ID关联获取';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='erp_sale_order_item' AND column_name='wholesale_price') THEN
        COMMENT ON COLUMN erp_sale_order_item.wholesale_price IS '批发价 - 通过产品ID关联获取';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='erp_sale_order_item' AND column_name='available_stock') THEN
        COMMENT ON COLUMN erp_sale_order_item.available_stock IS '可用库存 - 通过库存服务API实时获取';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='erp_sale_order_item' AND column_name='book_stock') THEN
        COMMENT ON COLUMN erp_sale_order_item.book_stock IS '账面库存 - 通过库存服务API实时获取';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='erp_sale_order_item' AND column_name='cost_amount') THEN
        COMMENT ON COLUMN erp_sale_order_item.cost_amount IS '成本金额 - 通过计算(quantity * cost_price)得出';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='erp_sale_order_item' AND column_name='gross_profit') THEN
        COMMENT ON COLUMN erp_sale_order_item.gross_profit IS '参考毛利 - 通过计算(amount - cost_amount)得出';
    END IF;
END $$;