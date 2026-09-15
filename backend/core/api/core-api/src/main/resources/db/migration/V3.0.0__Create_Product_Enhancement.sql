-- =====================================================
-- V3.0.0: 产品资料完善 - 属性/多单位/多条形码/附件/关联
-- =====================================================

-- 1. erp_product 增加字段
ALTER TABLE erp_product
    ADD COLUMN IF NOT EXISTS weight DECIMAL(12,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS volume DECIMAL(12,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS origin VARCHAR(100),
    ADD COLUMN IF NOT EXISTS brand VARCHAR(200),
    ADD COLUMN IF NOT EXISTS tax_rate DECIMAL(5,2) DEFAULT 13.00,
    ADD COLUMN IF NOT EXISTS purchase_price DECIMAL(20,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS retail_price DECIMAL(20,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS shelf_life_days INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_batch_managed INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_serial_managed INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS approval_status VARCHAR(20) DEFAULT 'DRAFT',
    ADD COLUMN IF NOT EXISTS approval_by BIGINT,
    ADD COLUMN IF NOT EXISTS approval_time TIMESTAMP;

COMMENT ON COLUMN erp_product.weight IS '重量(kg)';
COMMENT ON COLUMN erp_product.volume IS '体积(m³)';
COMMENT ON COLUMN erp_product.origin IS '产地';
COMMENT ON COLUMN erp_product.brand IS '品牌';
COMMENT ON COLUMN erp_product.tax_rate IS '税率%';
COMMENT ON COLUMN erp_product.purchase_price IS '采购价';
COMMENT ON COLUMN erp_product.retail_price IS '零售价';
COMMENT ON COLUMN erp_product.shelf_life_days IS '保质期(天)';
COMMENT ON COLUMN erp_product.is_batch_managed IS '是否启用批次管理';
COMMENT ON COLUMN erp_product.is_serial_managed IS '是否启用序列号管理';
COMMENT ON COLUMN erp_product.approval_status IS '审批状态 DRAFT/PENDING/APPROVED/REJECTED';
COMMENT ON COLUMN erp_product.approval_by IS '审批人';
COMMENT ON COLUMN erp_product.approval_time IS '审批时间';

-- 2. 产品辅助属性定义表(如颜色、尺寸、材质等)
CREATE TABLE IF NOT EXISTS erp_product_attribute_def (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    attr_name       VARCHAR(100) NOT NULL COMMENT '属性名称(如颜色、尺寸)',
    attr_type       VARCHAR(30) NOT NULL DEFAULT 'TEXT' COMMENT '属性类型 TEXT/SELECT/COLOR/NUMBER',
    sort_order      INTEGER DEFAULT 0,
    status          INTEGER NOT NULL DEFAULT 1,
    remark          VARCHAR(500),
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_product_attribute_def IS '产品辅助属性定义表';

-- 3. 产品属性值选项(如下拉选项的值)
CREATE TABLE IF NOT EXISTS erp_product_attribute_option (
    id              BIGSERIAL PRIMARY KEY,
    attr_def_id     BIGINT NOT NULL REFERENCES erp_product_attribute_def(id),
    option_value    VARCHAR(200) NOT NULL COMMENT '选项值',
    option_label    VARCHAR(200) COMMENT '选项标签',
    color_hex       VARCHAR(20) COMMENT '颜色十六进制值',
    sort_order      INTEGER DEFAULT 0,
    status          INTEGER NOT NULL DEFAULT 1,
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_product_attribute_option IS '产品属性值选项表';
CREATE INDEX IF NOT EXISTS idx_attr_opt_def ON erp_product_attribute_option(attr_def_id);

-- 4. 产品-属性值关联表
CREATE TABLE IF NOT EXISTS erp_product_attribute_value (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    product_id      BIGINT NOT NULL REFERENCES erp_product(id),
    attr_def_id     BIGINT NOT NULL REFERENCES erp_product_attribute_def(id),
    attr_value      VARCHAR(500) NOT NULL COMMENT '属性值(文本或选项ID)',
    sort_order      INTEGER DEFAULT 0,
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_product_attribute_value IS '产品属性值关联表';
CREATE INDEX IF NOT EXISTS idx_pav_product ON erp_product_attribute_value(product_id);
CREATE INDEX IF NOT EXISTS idx_pav_attr ON erp_product_attribute_value(attr_def_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_pav_unique ON erp_product_attribute_value(product_id, attr_def_id);

-- 5. 产品多单位换算表
CREATE TABLE IF NOT EXISTS erp_product_unit (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    product_id      BIGINT NOT NULL REFERENCES erp_product(id),
    unit_name       VARCHAR(50) NOT NULL COMMENT '单位名称(如箱/包/盒)',
    is_base_unit    INTEGER NOT NULL DEFAULT 0 COMMENT '是否基本单位',
    conversion_rate DECIMAL(20,6) NOT NULL DEFAULT 1 COMMENT '换算率(相对基本单位)',
    barcode         VARCHAR(100) COMMENT '该单位的条码',
    sort_order      INTEGER DEFAULT 0,
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_product_unit IS '产品多单位换算表';
CREATE INDEX IF NOT EXISTS idx_pu_product ON erp_product_unit(product_id);

-- 6. 产品多条形码表
CREATE TABLE IF NOT EXISTS erp_product_barcode (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    product_id      BIGINT NOT NULL REFERENCES erp_product(id),
    barcode         VARCHAR(100) NOT NULL COMMENT '条形码',
    barcode_type    VARCHAR(20) DEFAULT 'EAN13' COMMENT '条形码类型 EAN13/CODE128/QR',
    is_default      INTEGER NOT NULL DEFAULT 0 COMMENT '是否默认条码',
    unit_id         BIGINT REFERENCES erp_product_unit(id) COMMENT '对应单位',
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_product_barcode IS '产品多条形码表';
CREATE INDEX IF NOT EXISTS idx_pb_product ON erp_product_barcode(product_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_pb_barcode ON erp_product_barcode(tenant_id, barcode);

-- 7. 产品附件表
CREATE TABLE IF NOT EXISTS erp_product_attachment (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    product_id      BIGINT NOT NULL REFERENCES erp_product(id),
    file_name       VARCHAR(200) NOT NULL COMMENT '文件名',
    file_url        VARCHAR(500) NOT NULL COMMENT '文件URL',
    file_size       BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
    file_type       VARCHAR(50) COMMENT 'MIME类型',
    category        VARCHAR(20) DEFAULT 'IMAGE' COMMENT '附件分类 IMAGE/DOC/VIDEO/OTHER',
    sort_order      INTEGER DEFAULT 0,
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_product_attachment IS '产品附件表';
CREATE INDEX IF NOT EXISTS idx_pattach_product ON erp_product_attachment(product_id);

-- 8. 产品关联表(推荐/替代/配件)
CREATE TABLE IF NOT EXISTS erp_product_related (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    product_id      BIGINT NOT NULL REFERENCES erp_product(id),
    related_product_id BIGINT NOT NULL REFERENCES erp_product(id),
    relation_type   VARCHAR(30) NOT NULL COMMENT '关联类型 UPSELL/CROSSSELL/ALTERNATIVE/ACCESSORY/BUNDLE',
    sort_order      INTEGER DEFAULT 0,
    remark          VARCHAR(500),
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_product_related IS '产品关联表';
CREATE INDEX IF NOT EXISTS idx_prel_product ON erp_product_related(product_id);
CREATE INDEX IF NOT EXISTS idx_prel_related ON erp_product_related(related_product_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_prel_unique ON erp_product_related(product_id, related_product_id, relation_type);

-- 9. 产品SKU生成规则表
CREATE TABLE IF NOT EXISTS erp_product_sku_rule (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    rule_name       VARCHAR(100) NOT NULL COMMENT '规则名称',
    rule_format     VARCHAR(200) NOT NULL COMMENT 'SKU格式模板(如 {CATEGORY}-{ATTR_COLOR}-{SEQ})',
    separator       VARCHAR(10) DEFAULT '-' COMMENT '分隔符',
    seq_length      INTEGER DEFAULT 4 COMMENT '序号长度',
    seq_start       INTEGER DEFAULT 1 COMMENT '序号起始值',
    is_default      INTEGER NOT NULL DEFAULT 0,
    status          INTEGER NOT NULL DEFAULT 1,
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_product_sku_rule IS '产品SKU生成规则表';
