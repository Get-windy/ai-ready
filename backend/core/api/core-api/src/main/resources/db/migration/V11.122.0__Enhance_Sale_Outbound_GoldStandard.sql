-- ═══════════════════════════════════════════════════════════════════════════
-- 销售出库单金标准增强（2026-09-10）
-- 1) 物流备注：列表页「物流备注」批量写入的目标字段（此前无落库字段，功能为空壳）
-- 2) 单据级查询性能索引：按单据/按明细多条件分页（40 项查询条件）
-- ═══════════════════════════════════════════════════════════════════════════

ALTER TABLE erp_sale_outbound
    ADD COLUMN IF NOT EXISTS logistics_remark VARCHAR(500);

COMMENT ON COLUMN erp_sale_outbound.logistics_remark IS '物流备注（列表页批量写入）';

-- 多条件分页索引（与 SaleOutboundQueryDTO 查询项对齐）
CREATE INDEX IF NOT EXISTS idx_sale_outbound_date_status
    ON erp_sale_outbound (outbound_date, status);

CREATE INDEX IF NOT EXISTS idx_sale_outbound_customer
    ON erp_sale_outbound (customer_id);

CREATE INDEX IF NOT EXISTS idx_sale_outbound_warehouse
    ON erp_sale_outbound (warehouse_id);

CREATE INDEX IF NOT EXISTS idx_sale_outbound_item_outbound
    ON erp_sale_outbound_item (outbound_id);

-- 明细维度筛选（按明细 Tab：商品/明细备注/赠品/商品行属性）
CREATE INDEX IF NOT EXISTS idx_sale_outbound_item_product
    ON erp_sale_outbound_item (product_id);
