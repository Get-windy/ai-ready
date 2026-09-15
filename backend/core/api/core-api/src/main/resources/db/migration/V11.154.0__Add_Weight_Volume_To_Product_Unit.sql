-- ============================================================================
-- V11.154.0  商品单位补「重量（kg）」「体积（m³）」
--   对标 ql361 商品表单「基本信息 → 商品单位」明细表最后一组列（21→22 列）：
--     类型 / 单位名称 / 单位关系 / 换算关系 / 条码 / 预设进价 / 参考成本 / 最近进价 /
--     批发价 / 零售价 / 最低售价 / 最低折扣 / 8 个价格等级 / 重量（kg） / 体积（m³）
--   重量、体积是**单位级**属性（大包装单位更重/更大），此前只落在 erp_product 主表，
--   拆包/整箱换算的物流计费口径无法按单位取值。
-- ============================================================================

ALTER TABLE erp_product_unit ADD COLUMN IF NOT EXISTS weight numeric(18,4) DEFAULT 0;
ALTER TABLE erp_product_unit ADD COLUMN IF NOT EXISTS volume numeric(18,6) DEFAULT 0;

COMMENT ON COLUMN erp_product_unit.weight IS '重量（kg，单位级）';
COMMENT ON COLUMN erp_product_unit.volume IS '体积（m³，单位级）';
