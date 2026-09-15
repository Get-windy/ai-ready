-- 创建自定义报表定义表
-- 用于持久化用户自定义的报表，补充内置报表

CREATE TABLE IF NOT EXISTS report_definition (
    id BIGSERIAL PRIMARY KEY,
    report_id VARCHAR(64) NOT NULL,
    report_name VARCHAR(200) NOT NULL,
    report_code VARCHAR(100),
    report_type VARCHAR(20) DEFAULT 'custom',
    category VARCHAR(50),
    data_source_type VARCHAR(20) DEFAULT 'sql',
    definition_json TEXT,
    tenant_id BIGINT,
    enabled BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    create_by VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    CONSTRAINT uk_report_id UNIQUE (report_id)
);

COMMENT ON TABLE report_definition IS '自定义报表定义';
COMMENT ON COLUMN report_definition.id IS '主键';
COMMENT ON COLUMN report_definition.report_id IS '报表ID';
COMMENT ON COLUMN report_definition.report_name IS '报表名称';
COMMENT ON COLUMN report_definition.report_code IS '报表编码';
COMMENT ON COLUMN report_definition.report_type IS '报表类型: summary/detail/chart/pivot';
COMMENT ON COLUMN report_definition.category IS '报表分类';
COMMENT ON COLUMN report_definition.data_source_type IS '数据源类型: sql/api/custom';
COMMENT ON COLUMN report_definition.definition_json IS '报表定义完整JSON（含列定义、参数、图表配置等）';
COMMENT ON COLUMN report_definition.tenant_id IS '租户ID';
COMMENT ON COLUMN report_definition.enabled IS '是否启用';
COMMENT ON COLUMN report_definition.sort_order IS '排序号';
COMMENT ON COLUMN report_definition.create_by IS '创建人';
COMMENT ON COLUMN report_definition.create_time IS '创建时间';
COMMENT ON COLUMN report_definition.update_by IS '更新人';
COMMENT ON COLUMN report_definition.update_time IS '更新时间';
COMMENT ON COLUMN report_definition.deleted IS '逻辑删除标记（0-未删除，1-已删除）';

CREATE INDEX IF NOT EXISTS idx_report_def_tenant ON report_definition(tenant_id);
CREATE INDEX IF NOT EXISTS idx_report_def_category ON report_definition(category);
CREATE INDEX IF NOT EXISTS idx_report_def_enabled ON report_definition(enabled);
