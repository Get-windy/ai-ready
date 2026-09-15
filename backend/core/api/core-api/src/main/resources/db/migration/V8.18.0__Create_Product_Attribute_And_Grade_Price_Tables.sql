-- V8.18.0__Create_Product_Attribute_And_Grade_Price_Tables.sql
-- 创建产品属性定义、属性值、属性选项和等级价格表

-- 1. 产品属性定义表
CREATE TABLE IF NOT EXISTS erp_product_attribute_def (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 1,
    attr_name       VARCHAR(100) NOT NULL,
    attr_type       VARCHAR(20) NOT NULL DEFAULT 'text',
    sort_order      INTEGER DEFAULT 0,
    status          INTEGER DEFAULT 1,
    remark          VARCHAR(500),
    deleted         INTEGER DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE erp_product_attribute_def IS '产品辅助属性定义表';
COMMENT ON COLUMN erp_product_attribute_def.id IS '主键ID';
COMMENT ON COLUMN erp_product_attribute_def.tenant_id IS '租户ID';
COMMENT ON COLUMN erp_product_attribute_def.attr_name IS '属性名称';
COMMENT ON COLUMN erp_product_attribute_def.attr_type IS '属性类型(text/select/multi_select/date/number)';
COMMENT ON COLUMN erp_product_attribute_def.sort_order IS '排序号';
COMMENT ON COLUMN erp_product_attribute_def.status IS '状态(1启用 0停用)';
COMMENT ON COLUMN erp_product_attribute_def.deleted IS '删除标记';

-- 2. 产品属性选项表
CREATE TABLE IF NOT EXISTS erp_product_attribute_option (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 1,
    attr_def_id     BIGINT NOT NULL,
    option_value    VARCHAR(200) NOT NULL,
    sort_order      INTEGER DEFAULT 0,
    deleted         INTEGER DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE erp_product_attribute_option IS '产品属性选项表';
COMMENT ON COLUMN erp_product_attribute_option.attr_def_id IS '属性定义ID';
COMMENT ON COLUMN erp_product_attribute_option.option_value IS '选项值';

-- 3. 产品属性值表
CREATE TABLE IF NOT EXISTS erp_product_attribute_value (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 1,
    product_id      BIGINT NOT NULL,
    attr_def_id     BIGINT NOT NULL,
    attr_value      VARCHAR(500),
    sort_order      INTEGER DEFAULT 0,
    deleted         INTEGER DEFAULT 0,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE erp_product_attribute_value IS '产品属性值表';
COMMENT ON COLUMN erp_product_attribute_value.product_id IS '产品ID';
COMMENT ON COLUMN erp_product_attribute_value.attr_def_id IS '属性定义ID';
COMMENT ON COLUMN erp_product_attribute_value.attr_value IS '属性值';

-- 4. 产品等级价格表
CREATE TABLE IF NOT EXISTS erp_product_grade_price (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 1,
    product_id      BIGINT NOT NULL,
    grade_code      VARCHAR(50),
    grade_name      VARCHAR(100),
    price_type      VARCHAR(20) DEFAULT 'fixed',
    base_price      DECIMAL(18,4),
    grade_price     DECIMAL(18,4),
    discount_rate   DECIMAL(10,4),
    discount_amount DECIMAL(18,4),
    min_order_qty   INTEGER,
    max_order_qty   INTEGER,
    effective_date  TIMESTAMP,
    expire_date     TIMESTAMP,
    is_active       INTEGER DEFAULT 1,
    remark          VARCHAR(500),
    deleted         INTEGER DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE erp_product_grade_price IS '产品等级价格表';
COMMENT ON COLUMN erp_product_grade_price.product_id IS '产品ID';
COMMENT ON COLUMN erp_product_grade_price.grade_code IS '等级代码';
COMMENT ON COLUMN erp_product_grade_price.grade_name IS '等级名称';
COMMENT ON COLUMN erp_product_grade_price.price_type IS '价格类型(fixed/discount)';
COMMENT ON COLUMN erp_product_grade_price.base_price IS '基准价格';
COMMENT ON COLUMN erp_product_grade_price.grade_price IS '等级价格';
COMMENT ON COLUMN erp_product_grade_price.discount_rate IS '折扣率';
COMMENT ON COLUMN erp_product_grade_price.is_active IS '是否启用';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_product_attr_def_tenant ON erp_product_attribute_def(tenant_id);
CREATE INDEX IF NOT EXISTS idx_product_attr_def_deleted ON erp_product_attribute_def(deleted);
CREATE INDEX IF NOT EXISTS idx_product_attr_option_def ON erp_product_attribute_option(attr_def_id);
CREATE INDEX IF NOT EXISTS idx_product_attr_value_product ON erp_product_attribute_value(product_id);
CREATE INDEX IF NOT EXISTS idx_product_grade_price_product ON erp_product_grade_price(product_id);
CREATE INDEX IF NOT EXISTS idx_product_grade_price_tenant ON erp_product_grade_price(tenant_id);

-- 插入Flyway执行记录
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
SELECT
    COALESCE(MAX(installed_rank), 0) + 1,
    '8.18.0',
    'Create Product Attribute And Grade Price Tables',
    'SQL',
    'V8.18.0__Create_Product_Attribute_And_Grade_Price_Tables.sql',
    NULL,
    'system',
    CURRENT_TIMESTAMP,
    0,
    TRUE
FROM flyway_schema_history WHERE version = '8.18.0' OR version IS NULL
ON CONFLICT DO NOTHING;