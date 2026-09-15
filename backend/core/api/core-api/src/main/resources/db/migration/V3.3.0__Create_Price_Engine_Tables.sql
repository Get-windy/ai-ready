-- =====================================================
-- V3.3.0: 销售/采购取价逻辑 + 价格记忆
-- =====================================================

-- 1. 交易价格记忆表(记录历史交易价格用于定价参考)
CREATE TABLE IF NOT EXISTS erp_price_memory (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    biz_type        VARCHAR(20) NOT NULL COMMENT '业务类型 SALE销售/PURCHASE采购',
    product_id      BIGINT NOT NULL REFERENCES erp_product(id),
    partner_id      BIGINT NOT NULL REFERENCES erp_partner(id) COMMENT '往来单位(客户或供应商)',
    unit_price      DECIMAL(20,6) NOT NULL COMMENT '单价(含税)',
    unit_price_ex_tax DECIMAL(20,6) COMMENT '单价(不含税)',
    quantity        DECIMAL(20,6) DEFAULT 0 COMMENT '数量',
    total_amount    DECIMAL(20,2) DEFAULT 0 COMMENT '总金额',
    tax_rate        DECIMAL(5,2) DEFAULT 13.00 COMMENT '税率',
    currency        VARCHAR(10) DEFAULT 'CNY' COMMENT '币种',
    order_id        BIGINT COMMENT '关联订单ID',
    order_no        VARCHAR(100) COMMENT '关联订单号',
    order_date      DATE COMMENT '订单日期',
    price_source    VARCHAR(30) DEFAULT 'MANUAL'
                    COMMENT '价格来源 MANUAL手工/GRADE等级价/CUSTOMER客户价/RULE规则价',
    is_latest       INTEGER NOT NULL DEFAULT 1 COMMENT '是否最新价格',
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_price_memory IS '交易价格记忆表(取价参考)';
CREATE INDEX IF NOT EXISTS idx_pm_tenant ON erp_price_memory(tenant_id);
CREATE INDEX IF NOT EXISTS idx_pm_product ON erp_price_memory(product_id);
CREATE INDEX IF NOT EXISTS idx_pm_partner ON erp_price_memory(partner_id);
CREATE INDEX IF NOT EXISTS idx_pm_biz ON erp_price_memory(biz_type);
CREATE INDEX IF NOT EXISTS idx_pm_latest ON erp_price_memory(product_id, partner_id, biz_type) WHERE is_latest = 1;

-- 2. 取价规则配置表(定义取价优先级链路)
CREATE TABLE IF NOT EXISTS erp_pricing_rule_config (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    config_name     VARCHAR(100) NOT NULL COMMENT '配置名称',
    biz_type        VARCHAR(20) NOT NULL COMMENT '业务类型 SALE/PURCHASE',
    priority_chain  TEXT NOT NULL COMMENT '取价优先级链(JSON数组)',
    default_price_type VARCHAR(30) NOT NULL DEFAULT 'STANDARD'
                    COMMENT '默认价格类型 STANDARD/WHOLESALE/COST/PURCHASE',
    is_active       INTEGER NOT NULL DEFAULT 1,
    remark          VARCHAR(500),
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_pricing_rule_config IS '取价规则配置表';
COMMENT ON COLUMN erp_pricing_rule_config.priority_chain IS '格式: [{"source":"CUSTOMER_PRICE","priority":1},{"source":"GRADE_PRICE","priority":2},{"source":"STANDARD_PRICE","priority":3}]';

-- 插入默认取价配置
INSERT INTO erp_pricing_rule_config (tenant_id, config_name, biz_type, priority_chain, default_price_type, remark) VALUES
(0, '销售取价(默认)', 'SALE',
 '[{"source":"CUSTOMER_PRICE","label":"客户特定价","priority":1},{"source":"GRADE_PRICE","label":"等级价格","priority":2},{"source":"PRICE_MEMORY","label":"最近交易价","priority":3},{"source":"STANDARD_PRICE","label":"标准售价","priority":4}]',
 'STANDARD', '销售取价优先级: 客户特定价 > 等级价 > 最近交易价 > 标准售价'),
(0, '采购取价(默认)', 'PURCHASE',
 '[{"source":"SUPPLIER_PRICE","label":"供应商报价","priority":1},{"source":"PRICE_MEMORY","label":"最近采购价","priority":2},{"source":"PURCHASE_PRICE","label":"采购价","priority":3},{"source":"COST_PRICE","label":"成本价","priority":4}]',
 'PURCHASE', '采购取价优先级: 供应商报价 > 最近采购价 > 采购价 > 成本价')
ON CONFLICT DO NOTHING;

-- 3. 客户等级产品价格表(关联erp_product_grade_price的伙伴等级映射)
-- 注意: erp_product_grade_price 已经存在,此处建立客户等级与产品等级价格的关联
CREATE TABLE IF NOT EXISTS erp_partner_grade_product_price (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL DEFAULT 0,
    partner_grade_id    BIGINT NOT NULL REFERENCES erp_partner_grade(id) COMMENT '客户等级ID',
    product_grade_price_id BIGINT NOT NULL REFERENCES erp_product_grade_price(id) COMMENT '产品等级价格ID',
    price               DECIMAL(20,2) NOT NULL DEFAULT 0 COMMENT '最终售价',
    is_active           INTEGER NOT NULL DEFAULT 1,
    deleted             INTEGER NOT NULL DEFAULT 0,
    create_by           BIGINT,
    create_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_partner_grade_product_price IS '客户等级-产品等级价格关联表';
CREATE INDEX IF NOT EXISTS idx_pgpp_grade ON erp_partner_grade_product_price(partner_grade_id);
CREATE INDEX IF NOT EXISTS idx_pgpp_price ON erp_partner_grade_product_price(product_grade_price_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_pgpp_unique ON erp_partner_grade_product_price(partner_grade_id, product_grade_price_id);
