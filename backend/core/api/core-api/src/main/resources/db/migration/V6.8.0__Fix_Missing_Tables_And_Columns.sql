-- =====================================================
-- V6.8.0: 修复缺失的表和字段
-- =====================================================

-- 1. erp_product表缺少sku字段
ALTER TABLE erp_product ADD COLUMN IF NOT EXISTS sku VARCHAR(100);
COMMENT ON COLUMN erp_product.sku IS 'SKU(库存单位编码)';
CREATE INDEX IF NOT EXISTS idx_product_sku ON erp_product(tenant_id, sku);

-- 2. erp_stock表缺少serial_no和sku字段
ALTER TABLE erp_stock ADD COLUMN IF NOT EXISTS serial_no VARCHAR(100);
ALTER TABLE erp_stock ADD COLUMN IF NOT EXISTS sku VARCHAR(100);
COMMENT ON COLUMN erp_stock.serial_no IS '序列号';
COMMENT ON COLUMN erp_stock.sku IS 'SKU';

-- 3. sys_print_chain_item表
CREATE TABLE IF NOT EXISTS sys_print_chain_item (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    chain_id BIGINT NOT NULL,
    item_type VARCHAR(50) NOT NULL,
    item_key VARCHAR(100) NOT NULL,
    item_value TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    status INT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(100),
    update_by VARCHAR(100),
    deleted INT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_print_chain_item_chain ON sys_print_chain_item(tenant_id, chain_id);

-- 4. erp_receipt收款单表
CREATE TABLE IF NOT EXISTS erp_receipt (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    receipt_no VARCHAR(100),
    receipt_type INT DEFAULT 1,
    customer_id BIGINT,
    customer_name VARCHAR(200),
    order_id BIGINT,
    order_no VARCHAR(100),
    invoice_id BIGINT,
    invoice_no VARCHAR(100),
    receipt_date DATE,
    status INT DEFAULT 0,
    receipt_amount DECIMAL(20,2) DEFAULT 0,
    verified_amount DECIMAL(20,2) DEFAULT 0,
    pending_amount DECIMAL(20,2) DEFAULT 0,
    payment_method VARCHAR(50),
    bank_account VARCHAR(100),
    bank_name VARCHAR(200),
    check_no VARCHAR(100),
    transaction_no VARCHAR(100),
    sales_person_id BIGINT,
    sales_person_name VARCHAR(100),
    department_id BIGINT,
    department_name VARCHAR(100),
    approved_by BIGINT,
    approved_time TIMESTAMP,
    approved_note VARCHAR(500),
    verified_by BIGINT,
    verified_time TIMESTAMP,
    completed_by BIGINT,
    completed_time TIMESTAMP,
    remark VARCHAR(500),
    internal_note VARCHAR(500),
    source_type VARCHAR(50),
    source_id BIGINT,
    source_no VARCHAR(100),
    pre_receipt_id BIGINT,
    deposit_flag INT DEFAULT 0,
    ext_info TEXT,
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    version_no INT DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_receipt_tenant ON erp_receipt(tenant_id);
CREATE INDEX IF NOT EXISTS idx_receipt_no ON erp_receipt(receipt_no);
CREATE INDEX IF NOT EXISTS idx_receipt_customer ON erp_receipt(customer_id);

-- 5. erp_payment付款单表
CREATE TABLE IF NOT EXISTS erp_payment (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    payment_no VARCHAR(100),
    payment_type INT DEFAULT 1,
    supplier_id BIGINT,
    supplier_name VARCHAR(200),
    order_id BIGINT,
    order_no VARCHAR(100),
    invoice_id BIGINT,
    invoice_no VARCHAR(100),
    payment_date DATE,
    status INT DEFAULT 0,
    payment_amount DECIMAL(20,2) DEFAULT 0,
    verified_amount DECIMAL(20,2) DEFAULT 0,
    pending_amount DECIMAL(20,2) DEFAULT 0,
    payment_method VARCHAR(50),
    bank_account VARCHAR(100),
    bank_name VARCHAR(200),
    check_no VARCHAR(100),
    transaction_no VARCHAR(100),
    purchaser_id BIGINT,
    purchaser_name VARCHAR(100),
    department_id BIGINT,
    department_name VARCHAR(100),
    approved_by BIGINT,
    approved_time TIMESTAMP,
    approved_note VARCHAR(500),
    verified_by BIGINT,
    verified_time TIMESTAMP,
    completed_by BIGINT,
    completed_time TIMESTAMP,
    remark VARCHAR(500),
    internal_note VARCHAR(500),
    source_type VARCHAR(50),
    source_id BIGINT,
    source_no VARCHAR(100),
    pre_payment_id BIGINT,
    deposit_flag INT DEFAULT 0,
    ext_info TEXT,
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    version_no INT DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_payment_tenant ON erp_payment(tenant_id);
CREATE INDEX IF NOT EXISTS idx_payment_no ON erp_payment(payment_no);
CREATE INDEX IF NOT EXISTS idx_payment_supplier ON erp_payment(supplier_id);

-- 6. 预收款/预付款表
CREATE TABLE IF NOT EXISTS erp_pre_receipt (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    pre_receipt_no VARCHAR(100),
    customer_id BIGINT,
    customer_name VARCHAR(200),
    pre_receipt_amount DECIMAL(20,2) DEFAULT 0,
    used_amount DECIMAL(20,2) DEFAULT 0,
    remaining_amount DECIMAL(20,2) DEFAULT 0,
    receipt_date DATE,
    status INT DEFAULT 0,
    remark VARCHAR(500),
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);
CREATE INDEX IF NOT EXISTS idx_pre_receipt_tenant ON erp_pre_receipt(tenant_id);
CREATE INDEX IF NOT EXISTS idx_pre_receipt_customer ON erp_pre_receipt(customer_id);

CREATE TABLE IF NOT EXISTS erp_pre_payment (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    pre_payment_no VARCHAR(100),
    supplier_id BIGINT,
    supplier_name VARCHAR(200),
    pre_payment_amount DECIMAL(20,2) DEFAULT 0,
    used_amount DECIMAL(20,2) DEFAULT 0,
    remaining_amount DECIMAL(20,2) DEFAULT 0,
    payment_date DATE,
    status INT DEFAULT 0,
    remark VARCHAR(500),
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);
CREATE INDEX IF NOT EXISTS idx_pre_payment_tenant ON erp_pre_payment(tenant_id);
CREATE INDEX IF NOT EXISTS idx_pre_payment_supplier ON erp_pre_payment(supplier_id);

-- 7. 核销冲抵表
CREATE TABLE IF NOT EXISTS erp_write_off (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    write_off_no VARCHAR(100),
    write_off_type INT DEFAULT 1,
    receipt_id BIGINT,
    invoice_id BIGINT,
    write_off_amount DECIMAL(20,2) DEFAULT 0,
    write_off_date DATE,
    status INT DEFAULT 0,
    remark VARCHAR(500),
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);
CREATE INDEX IF NOT EXISTS idx_write_off_tenant ON erp_write_off(tenant_id);

CREATE TABLE IF NOT EXISTS erp_offset (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    offset_no VARCHAR(100),
    offset_type INT DEFAULT 1,
    source_id BIGINT,
    target_id BIGINT,
    offset_amount DECIMAL(20,2) DEFAULT 0,
    offset_date DATE,
    status INT DEFAULT 0,
    remark VARCHAR(500),
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);
CREATE INDEX IF NOT EXISTS idx_offset_tenant ON erp_offset(tenant_id);

-- 8. 会计科目表
CREATE TABLE IF NOT EXISTS erp_finance_subject (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    subject_code VARCHAR(50),
    subject_name VARCHAR(200),
    subject_type INT DEFAULT 1,
    parent_id BIGINT DEFAULT 0,
    level INT DEFAULT 1,
    is_leaf INT DEFAULT 0,
    balance_direction INT DEFAULT 1,
    status INT DEFAULT 1,
    remark VARCHAR(500),
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);
CREATE INDEX IF NOT EXISTS idx_subject_tenant ON erp_finance_subject(tenant_id);
CREATE INDEX IF NOT EXISTS idx_subject_code ON erp_finance_subject(subject_code);