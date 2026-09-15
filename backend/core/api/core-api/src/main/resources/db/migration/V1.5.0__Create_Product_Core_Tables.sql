-- =====================================================
-- V1.5.0: 创建产品核心模块表结构
-- 包含: 产品分类、产品等级、产品等级价格
-- 扩展: erp_product 表增加分类/等级/价格字段
-- =====================================================

-- 1. 产品分类表
CREATE TABLE IF NOT EXISTS erp_product_category (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    category_code   VARCHAR(50) NOT NULL,
    category_name   VARCHAR(200) NOT NULL,
    parent_id       BIGINT NOT NULL DEFAULT 0,
    category_level  INTEGER NOT NULL DEFAULT 1,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    icon            VARCHAR(200),
    status          INTEGER NOT NULL DEFAULT 1,
    description     VARCHAR(500),
    remark          VARCHAR(500),
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE erp_product_category IS '产品分类表';
COMMENT ON COLUMN erp_product_category.category_code IS '分类编码';
COMMENT ON COLUMN erp_product_category.category_name IS '分类名称';
COMMENT ON COLUMN erp_product_category.parent_id IS '父级ID(0=根节点)';
COMMENT ON COLUMN erp_product_category.category_level IS '层级';

CREATE INDEX IF NOT EXISTS idx_pcat_tenant ON erp_product_category(tenant_id);
CREATE INDEX IF NOT EXISTS idx_pcat_parent ON erp_product_category(parent_id);
CREATE INDEX IF NOT EXISTS idx_pcat_status ON erp_product_category(status);

-- 2. 产品等级表
CREATE TABLE IF NOT EXISTS erp_product_grade (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    grade_code      VARCHAR(50) NOT NULL,
    grade_name      VARCHAR(100) NOT NULL,
    grade_level     INTEGER NOT NULL DEFAULT 0,
    sort_order      INTEGER DEFAULT 0,
    status          INTEGER NOT NULL DEFAULT 1,
    description     VARCHAR(500),
    remark          VARCHAR(500),
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE erp_product_grade IS '产品等级表';
COMMENT ON COLUMN erp_product_grade.grade_code IS '等级编码(A/B/C)';
COMMENT ON COLUMN erp_product_grade.grade_name IS '等级名称';
COMMENT ON COLUMN erp_product_grade.grade_level IS '等级数值(越大越高)';

CREATE UNIQUE INDEX IF NOT EXISTS idx_pgrade_tenant_code ON erp_product_grade(tenant_id, grade_code);

-- 3. 扩展 erp_product 表(增加新字段)
ALTER TABLE erp_product
    ADD COLUMN IF NOT EXISTS category_id BIGINT,
    ADD COLUMN IF NOT EXISTS product_grade_id BIGINT,
    ADD COLUMN IF NOT EXISTS cost_price DECIMAL(20,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS standard_price DECIMAL(20,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS wholesale_price DECIMAL(20,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS barcode VARCHAR(100),
    ADD COLUMN IF NOT EXISTS product_type VARCHAR(50) DEFAULT 'SINGLE',
    ADD COLUMN IF NOT EXISTS has_grade_price INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS update_by BIGINT,
    ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT 0;

COMMENT ON COLUMN erp_product.category_id IS '分类ID,关联erp_product_category';
COMMENT ON COLUMN erp_product.product_grade_id IS '默认产品等级ID,关联erp_product_grade';
COMMENT ON COLUMN erp_product.cost_price IS '成本价';
COMMENT ON COLUMN erp_product.standard_price IS '标准售价';
COMMENT ON COLUMN erp_product.wholesale_price IS '批发价';
COMMENT ON COLUMN erp_product.product_type IS 'SINGLE单品 KIT套件 SERVICE服务';
COMMENT ON COLUMN erp_product.has_grade_price IS '是否已配置等级价格(1=是)';

CREATE INDEX IF NOT EXISTS idx_prod_category ON erp_product(category_id);
CREATE INDEX IF NOT EXISTS idx_prod_code_tenant ON erp_product(tenant_id, product_code);

-- 4. 产品等级价格表
CREATE TABLE IF NOT EXISTS erp_product_grade_price (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL DEFAULT 0,
    product_id          BIGINT NOT NULL,
    product_grade_id    BIGINT NOT NULL,
    price               DECIMAL(20,2) NOT NULL DEFAULT 0,
    min_order_qty       INTEGER DEFAULT 0,
    is_active           INTEGER NOT NULL DEFAULT 1,
    effective_date      TIMESTAMP,
    expire_date         TIMESTAMP,
    remark              VARCHAR(500),
    deleted             INTEGER NOT NULL DEFAULT 0,
    create_by           BIGINT,
    create_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE erp_product_grade_price IS '产品等级价格表-每个产品在不同等级下的价格';
COMMENT ON COLUMN erp_product_grade_price.product_id IS '产品ID';
COMMENT ON COLUMN erp_product_grade_price.product_grade_id IS '产品等级ID';
COMMENT ON COLUMN erp_product_grade_price.price IS '该等级对应价格';

CREATE INDEX IF NOT EXISTS idx_pgp_product ON erp_product_grade_price(product_id);
CREATE INDEX IF NOT EXISTS idx_pgp_grade ON erp_product_grade_price(product_grade_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_pgp_unique ON erp_product_grade_price(tenant_id, product_id, product_grade_id);

-- 5. 插入默认产品等级数据(所有租户共享)
INSERT INTO erp_product_grade (tenant_id, grade_code, grade_name, grade_level, sort_order) VALUES
(0, 'A', 'A级(批发价)', 3, 1),
(0, 'B', 'B级(经销商价)', 2, 2),
(0, 'C', 'C级(零售价)', 1, 3)
ON CONFLICT (tenant_id, grade_code) DO NOTHING;

-- 6. 插入默认产品分类
INSERT INTO erp_product_category (tenant_id, category_code, category_name, parent_id, category_level, sort_order) VALUES
(0, 'ROOT', '全部产品', 0, 1, 1),
(0, 'FINISHED', '成品', 1, 2, 2),
(0, 'ELECTRONIC', '电子类', 2, 3, 3),
(0, 'FURNITURE', '家具类', 2, 3, 4),
(0, 'RAW', '原材料', 1, 2, 5),
(0, 'SERVICE', '服务类', 1, 2, 6)
ON CONFLICT DO NOTHING;
