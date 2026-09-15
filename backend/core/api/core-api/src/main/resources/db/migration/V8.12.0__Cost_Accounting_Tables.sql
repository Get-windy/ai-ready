-- 成本核算增强表
-- V8.12.0__Cost_Accounting_Tables.sql

-- 成本分摊规则表
CREATE TABLE IF NOT EXISTS erp_cost_allocation_rule (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    rule_code VARCHAR(50) NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    allocation_type VARCHAR(20) NOT NULL, -- DEPT, PRODUCT, ORDER
    allocation_ratio DECIMAL(5,2) DEFAULT 100.00,
    target_account VARCHAR(50),
    description VARCHAR(200),
    status INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

COMMENT ON TABLE erp_cost_allocation_rule IS '成本分摊规则表';
COMMENT ON COLUMN erp_cost_allocation_rule.allocation_type IS '分摊类型: DEPT部门, PRODUCT产品, ORDER订单';

-- 产品标准成本表
CREATE TABLE IF NOT EXISTS erp_product_cost_standard (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    material_cost DECIMAL(12,2) DEFAULT 0,
    labor_cost DECIMAL(12,2) DEFAULT 0,
    overhead_cost DECIMAL(12,2) DEFAULT 0,
    total_cost DECIMAL(12,2) DEFAULT 0,
    effective_date DATE NOT NULL,
    expire_date DATE,
    status INT DEFAULT 1,
    remark VARCHAR(200),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

COMMENT ON TABLE erp_product_cost_standard IS '产品标准成本表';

-- 毛利分析表
CREATE TABLE IF NOT EXISTS erp_profit_analysis (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    order_no VARCHAR(50),
    product_id BIGINT NOT NULL,
    product_name VARCHAR(100),
    sale_quantity DECIMAL(12,2),
    sale_price DECIMAL(12,2),
    sale_amount DECIMAL(12,2) NOT NULL,
    cost_amount DECIMAL(12,2) NOT NULL,
    gross_profit DECIMAL(12,2) NOT NULL,
    gross_margin DECIMAL(5,2) NOT NULL,
    analysis_date DATE NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

COMMENT ON TABLE erp_profit_analysis IS '毛利分析表';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_cost_rule_code ON erp_cost_allocation_rule(rule_code);
CREATE INDEX IF NOT EXISTS idx_product_cost_product ON erp_product_cost_standard(product_id);
CREATE INDEX IF NOT EXISTS idx_profit_analysis_order ON erp_profit_analysis(order_id);
CREATE INDEX IF NOT EXISTS idx_profit_analysis_date ON erp_profit_analysis(analysis_date);

-- 菜单数据
INSERT INTO sys_menu (id, parent_id, menu_name, path, menu_type, component, icon, sort, visible)
VALUES
(401, 400, '成本核算', 'finance-cost', 1, 'views/finance/cost/list', NULL, 2, 1),
(402, 400, '毛利分析', 'finance-profit', 1, 'views/finance/profit/list', NULL, 3, 1)
ON CONFLICT (id) DO NOTHING;