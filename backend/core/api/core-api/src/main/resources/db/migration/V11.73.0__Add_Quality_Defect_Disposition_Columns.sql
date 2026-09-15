-- ───────────────────────────────────────────────────────────
-- V11.73.0 缺陷记录跨模块处置闭环
-- 1) 缺陷主表补：缺陷等级 defect_level(S/Ma/Mi)、下游单号回填列
--    return_no(采购退货单号)/damage_no(报损单号)。
-- 2) 新建 quality_defect_handle_history 处理历史表，支撑多步处置/处理历史。
-- ───────────────────────────────────────────────────────────
ALTER TABLE quality_defect_handle ADD COLUMN IF NOT EXISTS defect_level VARCHAR(10);
ALTER TABLE quality_defect_handle ADD COLUMN IF NOT EXISTS return_no VARCHAR(64);
ALTER TABLE quality_defect_handle ADD COLUMN IF NOT EXISTS damage_no VARCHAR(64);

COMMENT ON COLUMN quality_defect_handle.defect_level IS '缺陷等级: S严重, Ma主要, Mi次要';
COMMENT ON COLUMN quality_defect_handle.return_no IS '处置生成的采购退货单号(RETURN)';
COMMENT ON COLUMN quality_defect_handle.damage_no IS '处置生成的报损单号(SCRAP)';

CREATE TABLE IF NOT EXISTS quality_defect_handle_history (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    defect_id BIGINT NOT NULL,
    handle_type VARCHAR(20),
    handle_quantity DECIMAL(12,2),
    handle_result VARCHAR(500),
    handler_id BIGINT,
    handler_name VARCHAR(100),
    handle_time TIMESTAMP,
    return_no VARCHAR(64),
    damage_no VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    create_by BIGINT,
    update_by BIGINT,
    version INT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_quality_defect_history_defect ON quality_defect_handle_history(defect_id);

COMMENT ON TABLE quality_defect_handle_history IS '缺陷处理历史(支撑多步处置/8D闭环)';
COMMENT ON COLUMN quality_defect_handle_history.defect_id IS '缺陷记录ID';
COMMENT ON COLUMN quality_defect_handle_history.return_no IS '该步处置生成的采购退货单号';
COMMENT ON COLUMN quality_defect_handle_history.damage_no IS '该步处置生成的报损单号';
