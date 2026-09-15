-- 质量管理 QMS 表
-- V8.13.0__Quality_Management_Tables.sql

-- 质检标准表
CREATE TABLE IF NOT EXISTS quality_standard (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    standard_code VARCHAR(50) NOT NULL,
    standard_name VARCHAR(100) NOT NULL,
    inspection_type VARCHAR(20) NOT NULL, -- INBOUND, OUTBOUND, PROCESS
    inspection_items TEXT, -- JSON数组
    sample_rate DECIMAL(5,2) DEFAULT 100.00,
    pass_threshold DECIMAL(5,2) DEFAULT 95.00,
    description VARCHAR(200),
    status INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

COMMENT ON TABLE quality_standard IS '质检标准表';
COMMENT ON COLUMN quality_standard.inspection_type IS '检验类型: INBOUND入库, OUTBOUND出库, PROCESS过程';
COMMENT ON COLUMN quality_standard.inspection_items IS '检验项目JSON数组';

-- 检验记录表
CREATE TABLE IF NOT EXISTS quality_inspection (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    biz_id BIGINT NOT NULL,
    biz_type VARCHAR(50) NOT NULL, -- PURCHASE_ORDER, SALE_ORDER, STOCK_IN, STOCK_OUT
    biz_no VARCHAR(50),
    product_id BIGINT NOT NULL,
    product_name VARCHAR(100),
    batch_no VARCHAR(50),
    quantity DECIMAL(12,2),
    sample_quantity DECIMAL(12,2),
    inspection_result VARCHAR(20) NOT NULL, -- PASS, FAIL, PENDING
    pass_quantity DECIMAL(12,2) DEFAULT 0,
    fail_quantity DECIMAL(12,2) DEFAULT 0,
    inspection_items TEXT, -- JSON检验项目结果
    inspector_id BIGINT,
    inspector_name VARCHAR(100),
    inspection_time TIMESTAMP,
    remark VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

COMMENT ON TABLE quality_inspection IS '检验记录表';
COMMENT ON COLUMN quality_inspection.inspection_result IS '检验结果: PASS合格, FAIL不合格, PENDING待检';
COMMENT ON COLUMN quality_inspection.biz_type IS '业务类型: PURCHASE_ORDER采购, SALE_ORDER销售, STOCK_IN入库, STOCK_OUT出库';

-- 不合格处理表
CREATE TABLE IF NOT EXISTS quality_defect_handle (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    inspection_id BIGINT NOT NULL,
    defect_type VARCHAR(50), -- QUALITY, PACKAGING, LABELING
    defect_desc VARCHAR(500),
    handle_type VARCHAR(20), -- RETURN, REWORK, SCRAP, SPECIAL_RELEASE
    handle_quantity DECIMAL(12,2),
    handler_id BIGINT,
    handler_name VARCHAR(100),
    handle_time TIMESTAMP,
    handle_result VARCHAR(200),
    status INT DEFAULT 0, -- 0待处理, 1已处理
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

COMMENT ON TABLE quality_defect_handle IS '不合格处理表';
COMMENT ON COLUMN quality_defect_handle.handle_type IS '处理方式: RETURN退货, REWORK返工, SCRAP报废, SPECIAL_RELEASE特采';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_quality_standard_code ON quality_standard(standard_code);
CREATE INDEX IF NOT EXISTS idx_quality_inspection_biz ON quality_inspection(biz_type, biz_id);
CREATE INDEX IF NOT EXISTS idx_quality_inspection_product ON quality_inspection(product_id);
CREATE INDEX IF NOT EXISTS idx_quality_defect_inspection ON quality_defect_handle(inspection_id);

-- 菜单数据
INSERT INTO sys_menu (id, parent_id, menu_name, path, menu_type, component, icon, sort, visible)
VALUES
(500, 0, '质量管理', 'quality', 0, 'Layout', 'experiment', 1, 1),
(501, 500, '质检标准', 'quality-standard', 1, 'views/quality/standard/list', NULL, 1, 1),
(502, 500, '检验记录', 'quality-inspection', 1, 'views/quality/inspection/list', NULL, 2, 1),
(503, 500, '不合格处理', 'quality-defect', 1, 'views/quality/defect/list', NULL, 3, 1)
ON CONFLICT (id) DO NOTHING;