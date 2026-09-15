-- V11.70.0: 质检单按来源单号+产品+结果查询索引
-- 供"未检不入库"门禁按 biz_no+product_id 快速定位质检结果
CREATE INDEX IF NOT EXISTS idx_quality_inspection_source
    ON quality_inspection(biz_no, product_id, inspection_result);
