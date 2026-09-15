-- 商品品牌管理表
CREATE TABLE IF NOT EXISTS erp_product_brand (
    id          BIGINT PRIMARY KEY,
    tenant_id   BIGINT NOT NULL DEFAULT 0,
    brand_name  VARCHAR(200) NOT NULL,
    mnemonic_code VARCHAR(50),
    remark      VARCHAR(500),
    sort_order  INT NOT NULL DEFAULT 0,
    status      INT NOT NULL DEFAULT 1,
    deleted     INT NOT NULL DEFAULT 0,
    create_by   BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE erp_product_brand IS '商品品牌管理表';
COMMENT ON COLUMN erp_product_brand.id IS '主键ID(雪花算法)';
COMMENT ON COLUMN erp_product_brand.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_product_brand.brand_name IS '品牌名称';
COMMENT ON COLUMN erp_product_brand.mnemonic_code IS '助记码(拼音首字母)';
COMMENT ON COLUMN erp_product_brand.remark IS '备注';
COMMENT ON COLUMN erp_product_brand.sort_order IS '排序';
COMMENT ON COLUMN erp_product_brand.status IS '状态: 1启用 0停用';
COMMENT ON COLUMN erp_product_brand.deleted IS '逻辑删除: 0正常 1已删除';

-- 品牌名称+租户唯一索引
CREATE UNIQUE INDEX IF NOT EXISTS uk_brand_tenant ON erp_product_brand(tenant_id, brand_name) WHERE deleted = 0;
-- 助记码索引
CREATE INDEX IF NOT EXISTS idx_brand_mnemonic ON erp_product_brand(tenant_id, mnemonic_code) WHERE deleted = 0;
