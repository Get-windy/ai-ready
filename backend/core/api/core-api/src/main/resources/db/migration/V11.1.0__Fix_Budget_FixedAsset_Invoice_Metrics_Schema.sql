-- ============================================================
-- V11.1.0 修复 budget / fixed-asset / invoice / metrics 四域 schema 残缺
--
-- 背景：V9.33.0 用 CREATE TABLE IF NOT EXISTS，而相关表已被手工建过（结构残缺），
--       CREATE 被静默跳过，导致 JPA 查询报 "字段 bt1_0.id 不存在" 等 500 错误。
--
-- 修复策略：
--   A. 16 张 0 行空表 → DROP 后按实体定义重建（含 BaseEntity 审计列）
--   B. erp_business_metric（866,904 行，"指标值"结构）→ 仅 ALTER 补"指标定义"列，不重建
--   C. erp_metric_data / erp_metric_aggregation（V9.33.0 已建对）→ 幂等补缺失索引
-- ============================================================

-- ------------------------------------------------------------
-- A1. budget 域：DROP + REBUILD（BaseEntity: id/created_by/created_at/updated_by/updated_at/deleted/tenant_id）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS budget_template CASCADE;
CREATE TABLE budget_template (
    id BIGSERIAL PRIMARY KEY,
    template_code VARCHAR(50),
    template_name VARCHAR(200) NOT NULL,
    fiscal_year INTEGER NOT NULL,
    total_amount NUMERIC(15,2),
    status VARCHAR(20),
    description TEXT,
    created_by VARCHAR(50),
    created_at TIMESTAMP,
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT false,
    tenant_id VARCHAR(50)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_budget_template_code ON budget_template (template_code);

DROP TABLE IF EXISTS budget_template_item CASCADE;
CREATE TABLE budget_template_item (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL,
    subject_code VARCHAR(50),
    subject_name VARCHAR(200),
    budget_amount NUMERIC(15,2),
    sort_order INTEGER,
    created_by VARCHAR(50),
    created_at TIMESTAMP,
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT false,
    tenant_id VARCHAR(50)
);

DROP TABLE IF EXISTS budget_item CASCADE;
CREATE TABLE budget_item (
    id BIGSERIAL PRIMARY KEY,
    budget_id BIGINT NOT NULL,
    subject_code VARCHAR(50),
    subject_name VARCHAR(200),
    budget_amount NUMERIC(15,2),
    used_amount NUMERIC(15,2),
    remaining_amount NUMERIC(15,2),
    frozen_amount NUMERIC(15,2),
    execution_rate NUMERIC(5,2),
    sort_order INTEGER,
    created_by VARCHAR(50),
    created_at TIMESTAMP,
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT false,
    tenant_id VARCHAR(50)
);

DROP TABLE IF EXISTS budget_adjustment CASCADE;
CREATE TABLE budget_adjustment (
    id BIGSERIAL PRIMARY KEY,
    adjustment_no VARCHAR(50),
    budget_id BIGINT NOT NULL,
    adjustment_type VARCHAR(20),
    amount NUMERIC(15,2),
    source_subject_id BIGINT,
    target_subject_id BIGINT,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20),
    applicant_id VARCHAR(50),
    applicant_name VARCHAR(100),
    approver_id VARCHAR(50),
    approver_name VARCHAR(100),
    approval_comment VARCHAR(500),
    apply_date DATE,
    approval_date DATE,
    created_by VARCHAR(50),
    created_at TIMESTAMP,
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT false,
    tenant_id VARCHAR(50)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_budget_adjustment_no ON budget_adjustment (adjustment_no);

DROP TABLE IF EXISTS budget_execution_log CASCADE;
CREATE TABLE budget_execution_log (
    id BIGSERIAL PRIMARY KEY,
    budget_id BIGINT NOT NULL,
    budget_item_id BIGINT,
    source_type VARCHAR(20),
    source_no VARCHAR(50),
    source_id BIGINT,
    amount NUMERIC(15,2),
    execution_type VARCHAR(20),
    execution_date DATE,
    description VARCHAR(500),
    created_by VARCHAR(50),
    created_at TIMESTAMP,
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT false,
    tenant_id VARCHAR(50)
);

DROP TABLE IF EXISTS annual_budget CASCADE;
CREATE TABLE annual_budget (
    id BIGSERIAL PRIMARY KEY,
    budget_no VARCHAR(50),
    template_id BIGINT,
    template_name VARCHAR(200),
    fiscal_year INTEGER,
    department_id VARCHAR(50),
    department_name VARCHAR(200),
    total_amount NUMERIC(15,2),
    status VARCHAR(20),
    total_approved_amount NUMERIC(15,2),
    total_used_amount NUMERIC(15,2),
    total_remaining_amount NUMERIC(15,2),
    execution_rate NUMERIC(5,2),
    description TEXT,
    remark VARCHAR(500),
    created_by VARCHAR(50),
    created_at TIMESTAMP,
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT false,
    tenant_id VARCHAR(50)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_annual_budget_no ON annual_budget (budget_no);

-- ------------------------------------------------------------
-- A2. fixed-asset 域：DROP + REBUILD
-- （BaseEntity: id/created_by/created_at/updated_by/updated_at/deleted/version/tenant_id/remark）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS fixed_asset CASCADE;
CREATE TABLE fixed_asset (
    id BIGSERIAL PRIMARY KEY,
    asset_code VARCHAR(50) NOT NULL,
    asset_name VARCHAR(200) NOT NULL,
    category_id BIGINT,
    category_name VARCHAR(100),
    purchase_date DATE,
    original_value NUMERIC(15,2),
    net_value NUMERIC(15,2),
    depreciation_method VARCHAR(30),
    useful_life INTEGER,
    salvage_value NUMERIC(15,2),
    salvage_rate NUMERIC(5,2),
    monthly_depreciation NUMERIC(15,2),
    accumulated_depreciation NUMERIC(15,2),
    status VARCHAR(20) NOT NULL,
    location VARCHAR(200),
    department_id VARCHAR(50),
    department_name VARCHAR(100),
    custodian_id VARCHAR(50),
    custodian_name VARCHAR(100),
    specification VARCHAR(200),
    brand VARCHAR(100),
    supplier_name VARCHAR(200),
    invoice_no VARCHAR(100),
    warranty_end_date DATE,
    description TEXT,
    use_status VARCHAR(20),
    asset_photo VARCHAR(500),
    created_by VARCHAR(50),
    created_at TIMESTAMP,
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT false,
    version INTEGER NOT NULL DEFAULT 0,
    tenant_id VARCHAR(50),
    remark VARCHAR(500)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_fixed_asset_code ON fixed_asset (asset_code);
CREATE INDEX IF NOT EXISTS idx_fixed_asset_status ON fixed_asset (status);
CREATE INDEX IF NOT EXISTS idx_fixed_asset_category_id ON fixed_asset (category_id);
CREATE INDEX IF NOT EXISTS idx_fixed_asset_department_id ON fixed_asset (department_id);
CREATE INDEX IF NOT EXISTS idx_fixed_asset_custodian_id ON fixed_asset (custodian_id);

DROP TABLE IF EXISTS fixed_asset_category CASCADE;
CREATE TABLE fixed_asset_category (
    id BIGSERIAL PRIMARY KEY,
    category_code VARCHAR(50),
    category_name VARCHAR(100) NOT NULL,
    parent_id BIGINT,
    sort_order INTEGER,
    default_depreciation_method VARCHAR(30),
    default_useful_life INTEGER,
    description VARCHAR(500),
    created_by VARCHAR(50),
    created_at TIMESTAMP,
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT false,
    version INTEGER NOT NULL DEFAULT 0,
    tenant_id VARCHAR(50),
    remark VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_fac_category_code ON fixed_asset_category (category_code);
CREATE INDEX IF NOT EXISTS idx_fac_parent_id ON fixed_asset_category (parent_id);

DROP TABLE IF EXISTS fixed_asset_depreciation CASCADE;
CREATE TABLE fixed_asset_depreciation (
    id BIGSERIAL PRIMARY KEY,
    asset_id BIGINT NOT NULL,
    period VARCHAR(7) NOT NULL,
    depreciation_date DATE,
    period_amount NUMERIC(15,2),
    accumulated_depreciation NUMERIC(15,2),
    net_value NUMERIC(15,2),
    asset_original_value NUMERIC(15,2),
    asset_name VARCHAR(200),
    asset_code VARCHAR(50),
    status VARCHAR(20),
    created_by VARCHAR(50),
    created_at TIMESTAMP,
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT false,
    version INTEGER NOT NULL DEFAULT 0,
    tenant_id VARCHAR(50),
    remark VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_fad_asset_id ON fixed_asset_depreciation (asset_id);
CREATE INDEX IF NOT EXISTS idx_fad_period ON fixed_asset_depreciation (period);
CREATE INDEX IF NOT EXISTS idx_fad_asset_code ON fixed_asset_depreciation (asset_code);

DROP TABLE IF EXISTS fixed_asset_disposal CASCADE;
CREATE TABLE fixed_asset_disposal (
    id BIGSERIAL PRIMARY KEY,
    disposal_no VARCHAR(50),
    asset_id BIGINT,
    asset_code VARCHAR(50),
    asset_name VARCHAR(200),
    disposal_date DATE,
    disposal_type VARCHAR(20),
    disposal_amount NUMERIC(15,2),
    net_value NUMERIC(15,2),
    gain_loss NUMERIC(15,2),
    reason VARCHAR(500),
    status VARCHAR(20),
    approval_comment VARCHAR(500),
    created_by VARCHAR(50),
    created_at TIMESTAMP,
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT false,
    version INTEGER NOT NULL DEFAULT 0,
    tenant_id VARCHAR(50),
    remark VARCHAR(500)
);

DROP TABLE IF EXISTS fixed_asset_inventory CASCADE;
CREATE TABLE fixed_asset_inventory (
    id BIGSERIAL PRIMARY KEY,
    inventory_no VARCHAR(50),
    inventory_date DATE,
    department_id VARCHAR(50),
    department_name VARCHAR(100),
    asset_id BIGINT,
    asset_code VARCHAR(50),
    asset_name VARCHAR(200),
    expected_location VARCHAR(200),
    actual_location VARCHAR(200),
    expected_status VARCHAR(20),
    actual_status VARCHAR(20),
    expected_custodian VARCHAR(100),
    actual_custodian VARCHAR(100),
    check_result VARCHAR(20),
    status VARCHAR(20),
    created_by VARCHAR(50),
    created_at TIMESTAMP,
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT false,
    version INTEGER NOT NULL DEFAULT 0,
    tenant_id VARCHAR(50),
    remark VARCHAR(500)
);

DROP TABLE IF EXISTS fixed_asset_purchase CASCADE;
CREATE TABLE fixed_asset_purchase (
    id BIGSERIAL PRIMARY KEY,
    purchase_no VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    applicant_id VARCHAR(50),
    applicant_name VARCHAR(100),
    department_id VARCHAR(50),
    department_name VARCHAR(100),
    asset_name VARCHAR(200),
    category_id BIGINT,
    category_name VARCHAR(100),
    quantity INTEGER NOT NULL,
    estimated_amount NUMERIC(15,2),
    actual_amount NUMERIC(15,2),
    apply_date DATE,
    expected_delivery_date DATE,
    actual_delivery_date DATE,
    purchase_reason VARCHAR(500),
    status VARCHAR(30),
    approval_comment VARCHAR(500),
    generated_asset_id BIGINT,
    specification VARCHAR(200),
    supplier_name VARCHAR(200),
    created_by VARCHAR(50),
    created_at TIMESTAMP,
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT false,
    version INTEGER NOT NULL DEFAULT 0,
    tenant_id VARCHAR(50),
    remark VARCHAR(500)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_fixed_asset_purchase_no ON fixed_asset_purchase (purchase_no);

DROP TABLE IF EXISTS fixed_asset_transfer CASCADE;
CREATE TABLE fixed_asset_transfer (
    id BIGSERIAL PRIMARY KEY,
    transfer_no VARCHAR(50),
    asset_id BIGINT,
    asset_code VARCHAR(50),
    asset_name VARCHAR(200),
    from_department_id VARCHAR(50),
    from_department_name VARCHAR(100),
    to_department_id VARCHAR(50),
    to_department_name VARCHAR(100),
    from_custodian_id VARCHAR(50),
    from_custodian_name VARCHAR(100),
    to_custodian_id VARCHAR(50),
    to_custodian_name VARCHAR(100),
    transfer_date DATE,
    reason VARCHAR(500),
    status VARCHAR(20),
    approval_comment VARCHAR(500),
    transfer_time TIMESTAMP,
    created_by VARCHAR(50),
    created_at TIMESTAMP,
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT false,
    version INTEGER NOT NULL DEFAULT 0,
    tenant_id VARCHAR(50),
    remark VARCHAR(500)
);

-- ------------------------------------------------------------
-- A3. invoice 域：DROP + REBUILD
-- （BaseEntity: id/created_at/updated_at/created_by/updated_by/tenant_id/version/is_deleted/deleted_at/deleted_by/extensions）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS invoice CASCADE;
CREATE TABLE invoice (
    id BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL,
    application_id BIGINT NOT NULL,
    invoice_type VARCHAR(20) NOT NULL,
    invoice_status VARCHAR(20) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    matching_status VARCHAR(20) NOT NULL,
    matched_amount NUMERIC(15,2) NOT NULL,
    matched_date DATE,
    matched_at TIMESTAMP,
    matched_by BIGINT,
    matched_by_name VARCHAR(100),
    matching_batch_number VARCHAR(50),
    matching_notes TEXT,
    invoice_date DATE NOT NULL,
    due_date DATE NOT NULL,
    customer_id BIGINT,
    customer_name VARCHAR(200),
    customer_tax_number VARCHAR(50),
    customer_address VARCHAR(500),
    customer_phone VARCHAR(50),
    customer_bank_account VARCHAR(100),
    supplier_id BIGINT,
    supplier_name VARCHAR(200),
    supplier_tax_number VARCHAR(50),
    supplier_address VARCHAR(500),
    supplier_phone VARCHAR(50),
    supplier_bank_account VARCHAR(100),
    currency_code VARCHAR(3) NOT NULL,
    exchange_rate NUMERIC(12,6),
    subtotal_amount NUMERIC(15,2) NOT NULL,
    tax_amount NUMERIC(15,2) NOT NULL,
    discount_amount NUMERIC(15,2),
    shipping_amount NUMERIC(15,2),
    other_amount NUMERIC(15,2),
    total_amount NUMERIC(15,2) NOT NULL,
    paid_amount NUMERIC(15,2),
    unpaid_amount NUMERIC(15,2),
    overdue_days INTEGER,
    late_fee_amount NUMERIC(15,2),
    issued_by BIGINT NOT NULL,
    issued_by_name VARCHAR(100),
    issued_at TIMESTAMP NOT NULL,
    reviewed_by BIGINT,
    reviewed_by_name VARCHAR(100),
    reviewed_at TIMESTAMP,
    review_notes TEXT,
    sent_at TIMESTAMP,
    send_method VARCHAR(20),
    sent_by BIGINT,
    send_status VARCHAR(20),
    send_error_message TEXT,
    document_path VARCHAR(500),
    document_format VARCHAR(20),
    document_size BIGINT,
    qrcode_data TEXT,
    qrcode_image_path VARCHAR(500),
    digital_signature TEXT,
    signed_at TIMESTAMP,
    signed_by BIGINT,
    tax_region VARCHAR(100),
    tax_type VARCHAR(50),
    is_tax_inclusive BOOLEAN,
    is_credit_note BOOLEAN,
    credit_reason VARCHAR(200),
    original_invoice_id BIGINT,
    notes TEXT,
    attachment_paths TEXT,
    business_region VARCHAR(50),
    business_department VARCHAR(100),
    project_code VARCHAR(50),
    contract_number VARCHAR(50),
    order_number VARCHAR(50),
    delivery_number VARCHAR(50),
    payment_terms VARCHAR(200),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    tenant_id VARCHAR(50),
    version INTEGER,
    is_deleted BOOLEAN,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(50),
    extensions TEXT
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_invoice_number ON invoice (invoice_number);

DROP TABLE IF EXISTS invoice_application CASCADE;
CREATE TABLE invoice_application (
    id BIGSERIAL PRIMARY KEY,
    application_number VARCHAR(50) NOT NULL,
    order_id BIGINT NOT NULL,
    order_type VARCHAR(20) NOT NULL,
    customer_id BIGINT,
    customer_name VARCHAR(200),
    customer_tax_number VARCHAR(50),
    customer_address VARCHAR(500),
    customer_phone VARCHAR(50),
    customer_bank_account VARCHAR(100),
    supplier_id BIGINT,
    supplier_name VARCHAR(200),
    supplier_tax_number VARCHAR(50),
    supplier_address VARCHAR(500),
    supplier_phone VARCHAR(50),
    supplier_bank_account VARCHAR(100),
    invoice_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    exchange_rate NUMERIC(12,6),
    subtotal_amount NUMERIC(15,2) NOT NULL,
    tax_amount NUMERIC(15,2) NOT NULL,
    discount_amount NUMERIC(15,2),
    shipping_amount NUMERIC(15,2),
    other_amount NUMERIC(15,2),
    total_amount NUMERIC(15,2) NOT NULL,
    applicant_id BIGINT NOT NULL,
    applicant_name VARCHAR(100),
    applicant_department VARCHAR(100),
    application_date DATE NOT NULL,
    expected_invoice_date DATE,
    expected_payment_date DATE,
    payment_terms VARCHAR(200),
    delivery_terms VARCHAR(200),
    shipping_method VARCHAR(100),
    shipping_address VARCHAR(500),
    billing_address VARCHAR(500),
    contact_person VARCHAR(100),
    contact_phone VARCHAR(50),
    contact_email VARCHAR(100),
    notes TEXT,
    attachment_paths TEXT,
    submitted_at TIMESTAMP,
    submitted_by BIGINT,
    approved_at TIMESTAMP,
    approved_by BIGINT,
    approval_notes TEXT,
    rejected_at TIMESTAMP,
    rejected_by BIGINT,
    rejection_reason TEXT,
    business_region VARCHAR(50),
    business_department VARCHAR(100),
    project_code VARCHAR(50),
    contract_number VARCHAR(50),
    is_urgent BOOLEAN,
    priority_level INTEGER,
    is_internal BOOLEAN,
    invoice_generated BOOLEAN,
    invoice_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    tenant_id VARCHAR(50),
    version INTEGER,
    is_deleted BOOLEAN,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(50),
    extensions TEXT
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_invoice_application_number ON invoice_application (application_number);

DROP TABLE IF EXISTS invoice_application_item CASCADE;
CREATE TABLE invoice_application_item (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL,
    line_number INTEGER NOT NULL,
    item_code VARCHAR(50),
    item_name VARCHAR(200) NOT NULL,
    specification VARCHAR(200),
    unit VARCHAR(20) NOT NULL,
    quantity NUMERIC(15,4) NOT NULL,
    unit_price NUMERIC(15,4) NOT NULL,
    tax_rate NUMERIC(5,2),
    tax_amount NUMERIC(15,2),
    amount NUMERIC(15,2) NOT NULL,
    discount_rate NUMERIC(5,2),
    discount_amount NUMERIC(15,2),
    notes VARCHAR(500),
    project_code VARCHAR(50),
    cost_center VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    tenant_id VARCHAR(50),
    version INTEGER,
    is_deleted BOOLEAN,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(50),
    extensions TEXT
);

DROP TABLE IF EXISTS invoice_item CASCADE;
CREATE TABLE invoice_item (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    line_number INTEGER NOT NULL,
    item_code VARCHAR(50),
    item_name VARCHAR(200) NOT NULL,
    specification VARCHAR(200),
    unit VARCHAR(20) NOT NULL,
    quantity NUMERIC(15,4) NOT NULL,
    unit_price NUMERIC(15,4) NOT NULL,
    tax_rate NUMERIC(5,2),
    tax_amount NUMERIC(15,2),
    amount NUMERIC(15,2) NOT NULL,
    discount_rate NUMERIC(5,2),
    discount_amount NUMERIC(15,2),
    notes VARCHAR(500),
    project_code VARCHAR(50),
    cost_center VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    tenant_id VARCHAR(50),
    version INTEGER,
    is_deleted BOOLEAN,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(50),
    extensions TEXT
);

DROP TABLE IF EXISTS invoice_tax CASCADE;
CREATE TABLE invoice_tax (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    tax_type VARCHAR(20) NOT NULL,
    tax_rate NUMERIC(5,2) NOT NULL,
    tax_base NUMERIC(15,2) NOT NULL,
    tax_amount NUMERIC(15,2) NOT NULL,
    tax_name VARCHAR(100),
    tax_code VARCHAR(50),
    is_deductible BOOLEAN,
    deduct_status VARCHAR(20),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    tenant_id VARCHAR(50),
    version INTEGER,
    is_deleted BOOLEAN,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(50),
    extensions TEXT
);

DROP TABLE IF EXISTS invoice_payment CASCADE;
CREATE TABLE invoice_payment (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    payment_number VARCHAR(50),
    amount NUMERIC(15,2) NOT NULL,
    payment_date TIMESTAMP NOT NULL,
    payment_method VARCHAR(30),
    status VARCHAR(20) NOT NULL,
    bank_name VARCHAR(100),
    bank_account VARCHAR(50),
    transaction_number VARCHAR(100),
    paid_by BIGINT,
    paid_by_name VARCHAR(100),
    notes VARCHAR(500),
    voucher_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    tenant_id VARCHAR(50),
    version INTEGER,
    is_deleted BOOLEAN,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(50),
    extensions TEXT
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_invoice_payment_number ON invoice_payment (payment_number);

DROP TABLE IF EXISTS invoice_workflow CASCADE;
CREATE TABLE invoice_workflow (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL,
    step INTEGER NOT NULL,
    step_name VARCHAR(100) NOT NULL,
    approver_id BIGINT NOT NULL,
    approver_name VARCHAR(100),
    approver_department VARCHAR(100),
    approver_role VARCHAR(50),
    action VARCHAR(20) NOT NULL,
    comments TEXT,
    approved_at TIMESTAMP NOT NULL,
    is_auto BOOLEAN,
    duration_ms BIGINT,
    next_approver_id BIGINT,
    next_approver_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    tenant_id VARCHAR(50),
    version INTEGER,
    is_deleted BOOLEAN,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(50),
    extensions TEXT
);

-- ------------------------------------------------------------
-- B. erp_business_metric：866,904 行有数据，只 ALTER 补列，不重建
--    现有结构是"指标值"，JPA 实体期望"指标定义"列
-- ------------------------------------------------------------
ALTER TABLE erp_business_metric ADD COLUMN IF NOT EXISTS description VARCHAR(512);
ALTER TABLE erp_business_metric ADD COLUMN IF NOT EXISTS calculation_formula VARCHAR(1024);
ALTER TABLE erp_business_metric ADD COLUMN IF NOT EXISTS data_source VARCHAR(256);
ALTER TABLE erp_business_metric ADD COLUMN IF NOT EXISTS threshold_warning DOUBLE PRECISION;
ALTER TABLE erp_business_metric ADD COLUMN IF NOT EXISTS threshold_critical DOUBLE PRECISION;
ALTER TABLE erp_business_metric ADD COLUMN IF NOT EXISTS refresh_interval_seconds INTEGER;
ALTER TABLE erp_business_metric ADD COLUMN IF NOT EXISTS retention_days INTEGER;
ALTER TABLE erp_business_metric ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE erp_business_metric ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE erp_business_metric ADD COLUMN IF NOT EXISTS created_by VARCHAR(64);
ALTER TABLE erp_business_metric ADD COLUMN IF NOT EXISTS updated_by VARCHAR(64);

-- 实体 @Column(nullable=false) 列需要默认值回填，避免现有 86 万行违反约束
UPDATE erp_business_metric SET created_at = COALESCE(create_time, NOW()) WHERE created_at IS NULL;
UPDATE erp_business_metric SET updated_at = COALESCE(update_time, NOW()) WHERE updated_at IS NULL;
ALTER TABLE erp_business_metric ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE erp_business_metric ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE erp_business_metric ALTER COLUMN created_at SET DEFAULT NOW();
ALTER TABLE erp_business_metric ALTER COLUMN updated_at SET DEFAULT NOW();

-- ------------------------------------------------------------
-- C. erp_metric_data / erp_metric_aggregation：V9.33.0 已建对，幂等补索引
-- ------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_metric_code_time ON erp_metric_data (metric_code, metric_time);
CREATE INDEX IF NOT EXISTS idx_metric_time ON erp_metric_data (metric_time);
CREATE INDEX IF NOT EXISTS idx_agg_metric_period ON erp_metric_aggregation (metric_code, period_code, aggregation_time);
CREATE INDEX IF NOT EXISTS idx_agg_time ON erp_metric_aggregation (aggregation_time);
