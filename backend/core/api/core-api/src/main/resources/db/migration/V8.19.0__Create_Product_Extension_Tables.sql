-- V8.19.0__Create_Product_Extension_Tables.sql
-- 创建产品条码、附件、关联产品扩展表

-- 1. 产品多条码表
CREATE TABLE IF NOT EXISTS erp_product_barcode (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 1,
    product_id      BIGINT NOT NULL,
    barcode         VARCHAR(100) NOT NULL,
    barcode_type    VARCHAR(20) DEFAULT 'EAN13',
    is_default      INTEGER DEFAULT 0,
    unit_id         BIGINT,
    deleted         INTEGER DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE erp_product_barcode IS '产品多条码表';
COMMENT ON COLUMN erp_product_barcode.product_id IS '产品ID';
COMMENT ON COLUMN erp_product_barcode.barcode IS '条形码';
COMMENT ON COLUMN erp_product_barcode.barcode_type IS '条码类型(EAN13/CODE128/QR等)';
COMMENT ON COLUMN erp_product_barcode.is_default IS '是否默认条码';
COMMENT ON COLUMN erp_product_barcode.unit_id IS '关联单位ID';

-- 2. 产品附件表
CREATE TABLE IF NOT EXISTS erp_product_attachment (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 1,
    product_id      BIGINT NOT NULL,
    file_name       VARCHAR(200) NOT NULL,
    file_url        VARCHAR(500) NOT NULL,
    file_size       BIGINT,
    file_type       VARCHAR(50),
    category        VARCHAR(20) DEFAULT 'OTHER',
    sort_order      INTEGER DEFAULT 0,
    deleted         INTEGER DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE erp_product_attachment IS '产品附件表';
COMMENT ON COLUMN erp_product_attachment.product_id IS '产品ID';
COMMENT ON COLUMN erp_product_attachment.file_name IS '文件名';
COMMENT ON COLUMN erp_product_attachment.file_url IS '文件URL';
COMMENT ON COLUMN erp_product_attachment.file_size IS '文件大小(字节)';
COMMENT ON COLUMN erp_product_attachment.file_type IS '文件类型(MIME)';
COMMENT ON COLUMN erp_product_attachment.category IS '分类(IMAGE/DOC/OTHER)';
COMMENT ON COLUMN erp_product_attachment.sort_order IS '排序号';

-- 3. 产品关联表
CREATE TABLE IF NOT EXISTS erp_product_related (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 1,
    product_id      BIGINT NOT NULL,
    related_product_id BIGINT NOT NULL,
    relation_type   VARCHAR(20) DEFAULT 'CROSSSELL',
    sort_order      INTEGER DEFAULT 0,
    remark          VARCHAR(500),
    deleted         INTEGER DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE erp_product_related IS '产品关联表';
COMMENT ON COLUMN erp_product_related.product_id IS '主产品ID';
COMMENT ON COLUMN erp_product_related.related_product_id IS '关联产品ID';
COMMENT ON COLUMN erp_product_related.relation_type IS '关联类型(UPSELL/CROSSSELL/ALTERNATIVE/ACCESSORY/BUNDLE)';
COMMENT ON COLUMN erp_product_related.sort_order IS '排序号';
COMMENT ON COLUMN erp_product_related.remark IS '备注';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_product_barcode_product ON erp_product_barcode(product_id);
CREATE INDEX IF NOT EXISTS idx_product_barcode_barcode ON erp_product_barcode(barcode);
CREATE INDEX IF NOT EXISTS idx_product_attachment_product ON erp_product_attachment(product_id);
CREATE INDEX IF NOT EXISTS idx_product_related_product ON erp_product_related(product_id);
CREATE INDEX IF NOT EXISTS idx_product_related_related ON erp_product_related(related_product_id);

-- 插入Flyway执行记录
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
SELECT
    COALESCE(MAX(installed_rank), 0) + 1,
    '8.19.0',
    'Create Product Extension Tables',
    'SQL',
    'V8.19.0__Create_Product_Extension_Tables.sql',
    NULL,
    'system',
    CURRENT_TIMESTAMP,
    0,
    TRUE
FROM flyway_schema_history WHERE version = '8.19.0' OR version IS NULL
ON CONFLICT DO NOTHING;