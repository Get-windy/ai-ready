-- =====================================================================
-- 商品价格管理（资料 → 商品管理 → 商品价格管理）金标准
-- 对标 ql361「商品价格管理」4 子标签：
--   1) 商品价格批量修改（读 erp_product + erp_product_unit，批量改价写回 erp_product_unit）
--   2) 客户级别折扣设置  → 新表 erp_customer_grade_discount
--   3) 级别指定价设置    → 新表 erp_customer_grade_price
--   4) 客户指定价设置    → 复用 erp_customer_product_price（补展示/规则列）
-- 价格单一口径：商品价格来源始终是 erp_product / erp_product_unit，禁止另建重复价格表。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 客户级别折扣设置（级别默认价 = 基础价 × 计算符 计算数）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS erp_customer_grade_discount (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          DEFAULT 1,
    grade_id        BIGINT,
    grade_name      VARCHAR(100)    NOT NULL,
    base_price_type VARCHAR(50)     NOT NULL DEFAULT '批发价',
    calc_operator   VARCHAR(4)      NOT NULL DEFAULT '*',
    calc_value      NUMERIC(18, 4)  NOT NULL DEFAULT 1,
    preview_text    VARCHAR(300),
    remark          VARCHAR(500),
    status          INTEGER         DEFAULT 1,
    create_by       BIGINT,
    create_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER         DEFAULT 0,
    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_erp_customer_grade_discount_tenant
    ON erp_customer_grade_discount (tenant_id, deleted);
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_customer_grade_discount_grade
    ON erp_customer_grade_discount (tenant_id, grade_name)
    WHERE deleted = 0;

COMMENT ON TABLE erp_customer_grade_discount IS '客户级别折扣设置（商品价格管理-子标签2）';
COMMENT ON COLUMN erp_customer_grade_discount.base_price_type IS '基础价类型：零售价/批发价/最低售价/8 个价格等级名';
COMMENT ON COLUMN erp_customer_grade_discount.calc_operator IS '计算符：* + - /';
COMMENT ON COLUMN erp_customer_grade_discount.calc_value IS '计算数';

-- ---------------------------------------------------------------------
-- 2. 级别指定价设置（客户级别 × 商品/分类 → 价格规则）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS erp_customer_grade_price (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          DEFAULT 1,
    grade_id        BIGINT,
    grade_name      VARCHAR(100),
    product_id      BIGINT,
    product_code    VARCHAR(60),
    product_name    VARCHAR(200),
    category_id     BIGINT,
    category_name   VARCHAR(120),
    unit_id         BIGINT,
    unit_name       VARCHAR(50),
    base_price_type VARCHAR(50)     NOT NULL DEFAULT '零售价',
    calc_operator   VARCHAR(4)      NOT NULL DEFAULT '+',
    calc_value      NUMERIC(18, 4)  NOT NULL DEFAULT 0,
    price_rule      VARCHAR(120),
    remark          VARCHAR(500),
    status          INTEGER         DEFAULT 1,
    create_by       BIGINT,
    create_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER         DEFAULT 0,
    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_erp_customer_grade_price_tenant
    ON erp_customer_grade_price (tenant_id, deleted);
CREATE INDEX IF NOT EXISTS idx_erp_customer_grade_price_grade
    ON erp_customer_grade_price (tenant_id, grade_name, deleted);
CREATE INDEX IF NOT EXISTS idx_erp_customer_grade_price_product
    ON erp_customer_grade_price (tenant_id, product_id, deleted);

COMMENT ON TABLE erp_customer_grade_price IS '级别指定价设置（商品价格管理-子标签3）';
COMMENT ON COLUMN erp_customer_grade_price.price_rule IS '价格规则展示文本，如「餐饮店-2」「指定价 3.34」';

-- ---------------------------------------------------------------------
-- 3. 客户指定价设置：复用 erp_customer_product_price，补规则/展示列
-- ---------------------------------------------------------------------
ALTER TABLE erp_customer_product_price ADD COLUMN IF NOT EXISTS product_code    VARCHAR(60);
ALTER TABLE erp_customer_product_price ADD COLUMN IF NOT EXISTS product_name    VARCHAR(200);
ALTER TABLE erp_customer_product_price ADD COLUMN IF NOT EXISTS category_id     BIGINT;
ALTER TABLE erp_customer_product_price ADD COLUMN IF NOT EXISTS category_name   VARCHAR(120);
ALTER TABLE erp_customer_product_price ADD COLUMN IF NOT EXISTS unit_id         BIGINT;
ALTER TABLE erp_customer_product_price ADD COLUMN IF NOT EXISTS unit_name       VARCHAR(50);
ALTER TABLE erp_customer_product_price ADD COLUMN IF NOT EXISTS base_price_type VARCHAR(50);
ALTER TABLE erp_customer_product_price ADD COLUMN IF NOT EXISTS calc_operator   VARCHAR(4);
ALTER TABLE erp_customer_product_price ADD COLUMN IF NOT EXISTS calc_value      NUMERIC(18, 4);
ALTER TABLE erp_customer_product_price ADD COLUMN IF NOT EXISTS price_rule      VARCHAR(120);
CREATE INDEX IF NOT EXISTS idx_erp_customer_product_price_customer
    ON erp_customer_product_price (tenant_id, customer_id, deleted);

COMMENT ON COLUMN erp_customer_product_price.price_rule IS '价格规则展示文本，如「指定价 3.34」';
