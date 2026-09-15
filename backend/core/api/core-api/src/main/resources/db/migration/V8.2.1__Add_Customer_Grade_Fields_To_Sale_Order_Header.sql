-- 添加销售订单表头的客户等级字段
-- 用于存储实体客户的等级信息，支持基于客户等级的价格计算
-- Author: AI-Ready System
-- Description: 为销售订单表头添加客户等级相关字段

-- 1. 为销售订单表头添加客户等级相关字段
ALTER TABLE erp_sale_order
ADD COLUMN IF NOT EXISTS customer_grade_code VARCHAR(20),
ADD COLUMN IF NOT EXISTS customer_grade_name VARCHAR(100);

-- 2. 为新增字段添加注释
COMMENT ON COLUMN erp_sale_order.customer_grade_code IS '客户等级代码 - 快照存储';
COMMENT ON COLUMN erp_sale_order.customer_grade_name IS '客户等级名称 - 快照存储';

-- 3. 为customer_grade_code字段添加索引以优化查询性能
CREATE INDEX IF NOT EXISTS idx_so_customer_grade ON erp_sale_order(customer_grade_code) WHERE deleted = 0;

-- 4. 更新表注释
COMMENT ON TABLE erp_sale_order IS '销售订单表 - 生产级分布式存储';