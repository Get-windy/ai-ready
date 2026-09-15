-- 商品单位字典表
CREATE TABLE IF NOT EXISTS erp_product_unit_dict (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    unit_name       VARCHAR(100) NOT NULL,
    unit_type       VARCHAR(50),
    conversion_rate DECIMAL(10,4) NOT NULL DEFAULT 1,
    remark          VARCHAR(500),
    sort_order      INT NOT NULL DEFAULT 0,
    status          INT NOT NULL DEFAULT 1,
    deleted         INT NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE erp_product_unit_dict IS '商品单位字典表';
COMMENT ON COLUMN erp_product_unit_dict.id IS '主键ID(雪花算法)';
COMMENT ON COLUMN erp_product_unit_dict.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_product_unit_dict.unit_name IS '单位名称';
COMMENT ON COLUMN erp_product_unit_dict.unit_type IS '单位类型(基本单位/辅助单位)';
COMMENT ON COLUMN erp_product_unit_dict.conversion_rate IS '换算率(相对于基本单位)';
COMMENT ON COLUMN erp_product_unit_dict.remark IS '备注';
COMMENT ON COLUMN erp_product_unit_dict.sort_order IS '排序';
COMMENT ON COLUMN erp_product_unit_dict.status IS '状态: 1启用 0停用';
COMMENT ON COLUMN erp_product_unit_dict.deleted IS '逻辑删除: 0正常 1已删除';

-- 单位名称+租户唯一索引
CREATE UNIQUE INDEX IF NOT EXISTS uk_unit_dict_tenant ON erp_product_unit_dict(tenant_id, unit_name) WHERE deleted = 0;
