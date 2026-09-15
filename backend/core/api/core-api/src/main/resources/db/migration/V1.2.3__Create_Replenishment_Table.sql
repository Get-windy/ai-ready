-- 创建智能补货建议表
CREATE TABLE IF NOT EXISTS erp_stock_replenishment (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    product_code VARCHAR(50) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    product_spec VARCHAR(200),
    product_unit VARCHAR(20),
    warehouse_id BIGINT,
    warehouse_name VARCHAR(200),
    current_qty DECIMAL(18,2) DEFAULT 0,
    safety_stock DECIMAL(18,2) DEFAULT 0,
    shortage_qty DECIMAL(18,2) DEFAULT 0,
    avg_daily_sales DECIMAL(18,2) DEFAULT 0,
    days_of_stock DECIMAL(10,2) DEFAULT 0,
    lead_time INT DEFAULT 0,
    suggested_qty DECIMAL(18,2) DEFAULT 0,
    priority VARCHAR(10) DEFAULT 'MEDIUM',
    reason VARCHAR(500),
    status VARCHAR(20) DEFAULT 'PENDING',
    supplier_id BIGINT,
    supplier_name VARCHAR(200),
    created_order_no VARCHAR(100),
    remark TEXT,
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    version_no INT DEFAULT 0
);

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_replenishment_status ON erp_stock_replenishment(status);
CREATE INDEX IF NOT EXISTS idx_replenishment_priority ON erp_stock_replenishment(priority);
CREATE INDEX IF NOT EXISTS idx_replenishment_product_code ON erp_stock_replenishment(product_code);
CREATE INDEX IF NOT EXISTS idx_replenishment_tenant ON erp_stock_replenishment(tenant_id);
