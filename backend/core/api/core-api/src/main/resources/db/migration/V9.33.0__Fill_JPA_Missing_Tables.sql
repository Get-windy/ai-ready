-- ============================================================
-- V9.33.0 Fill JPA entity missing tables (38)
-- ============================================================

-- source: /erp/erp-budget/src/main/java/cn/aiedge/erp/budget/model/AnnualBudget.java
CREATE TABLE IF NOT EXISTS annual_budget (
    budget_no VARCHAR(50),
    template_id BIGINT,
    template_name VARCHAR(200),
    fiscal_year INTEGER,
    department_id VARCHAR(50),
    department_name VARCHAR(200),
    total_amount NUMERIC(18,4),
    status VARCHAR(20),
    total_approved_amount NUMERIC(18,4),
    total_used_amount NUMERIC(18,4),
    total_remaining_amount NUMERIC(18,4),
    execution_rate NUMERIC(18,4),
    description TEXT,
    remark VARCHAR(500)
);

-- source: /erp/erp-budget/src/main/java/cn/aiedge/erp/budget/model/BudgetAdjustment.java
CREATE TABLE IF NOT EXISTS budget_adjustment (
    adjustment_no VARCHAR(50),
    budget_id BIGINT NOT NULL,
    adjustment_type VARCHAR(20),
    amount NUMERIC(18,4),
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
    approval_date DATE
);

-- source: /erp/erp-budget/src/main/java/cn/aiedge/erp/budget/model/BudgetExecutionLog.java
CREATE TABLE IF NOT EXISTS budget_execution_log (
    budget_id BIGINT NOT NULL,
    budget_item_id BIGINT,
    source_type VARCHAR(20),
    source_no VARCHAR(50),
    source_id BIGINT,
    amount NUMERIC(18,4),
    execution_type VARCHAR(20),
    execution_date DATE,
    description VARCHAR(500)
);

-- source: /erp/erp-budget/src/main/java/cn/aiedge/erp/budget/model/BudgetItem.java
CREATE TABLE IF NOT EXISTS budget_item (
    budget_id BIGINT NOT NULL,
    subject_code VARCHAR(50),
    subject_name VARCHAR(200),
    budget_amount NUMERIC(18,4),
    used_amount NUMERIC(18,4),
    remaining_amount NUMERIC(18,4),
    frozen_amount NUMERIC(18,4),
    execution_rate NUMERIC(18,4),
    sort_order INTEGER
);

-- source: /erp/erp-budget/src/main/java/cn/aiedge/erp/budget/model/BudgetTemplate.java
CREATE TABLE IF NOT EXISTS budget_template (
    template_code VARCHAR(50),
    template_name VARCHAR(200) NOT NULL,
    fiscal_year INTEGER NOT NULL,
    total_amount NUMERIC(18,4),
    status VARCHAR(20),
    description TEXT
);

-- source: /erp/erp-budget/src/main/java/cn/aiedge/erp/budget/model/BudgetTemplateItem.java
CREATE TABLE IF NOT EXISTS budget_template_item (
    template_id BIGINT NOT NULL,
    subject_code VARCHAR(50),
    subject_name VARCHAR(200),
    budget_amount NUMERIC(18,4),
    sort_order INTEGER
);

-- source: /erp/erp-observability/src/main/java/cn/aiedge/erp/metrics/entity/MetricAggregation.java
CREATE TABLE IF NOT EXISTS erp_metric_aggregation (
    id BIGSERIAL,
    metric_code VARCHAR(64) NOT NULL,
    period_code VARCHAR(16) NOT NULL,
    aggregation_time TIMESTAMP NOT NULL,
    avg_value NUMERIC(18,4),
    min_value NUMERIC(18,4),
    max_value NUMERIC(18,4),
    sum_value NUMERIC(18,4),
    count_value BIGINT,
    dimension_key VARCHAR(128),
    dimension_value VARCHAR(256),
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

-- source: /erp/erp-observability/src/main/java/cn/aiedge/erp/metrics/entity/MetricData.java
CREATE TABLE IF NOT EXISTS erp_metric_data (
    id BIGSERIAL,
    metric_code VARCHAR(64) NOT NULL,
    metric_value NUMERIC(18,4) NOT NULL,
    metric_time TIMESTAMP NOT NULL,
    dimension_key VARCHAR(128),
    dimension_value VARCHAR(256),
    period_code VARCHAR(16),
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

-- source: /erp/erp-finance/src/main/java/cn/aiedge/erp/expense/model/ExpenseApplication.java
CREATE TABLE IF NOT EXISTS expense_application (
    application_code VARCHAR(50) NOT NULL,
    applicant_id VARCHAR(50) NOT NULL,
    applicant_name VARCHAR(100) NOT NULL,
    department_id VARCHAR(50),
    department_name VARCHAR(100),
    apply_date DATE NOT NULL,
    expense_type VARCHAR(20) NOT NULL,
    expense_type_desc VARCHAR(100),
    total_amount NUMERIC(18,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    budget_subject_id VARCHAR(50),
    budget_subject_name VARCHAR(200),
    budget_amount NUMERIC(18,4),
    used_budget_amount NUMERIC(18,4),
    budget_usage_rate NUMERIC(18,4),
    purpose VARCHAR(500) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL,
    status_desc VARCHAR(100),
    current_approver_id VARCHAR(50),
    current_approver_name VARCHAR(100),
    current_approval_level INTEGER,
    total_approval_level INTEGER,
    process_instance_id VARCHAR(100),
    process_definition_id VARCHAR(100),
    task_id VARCHAR(100),
    payment_method VARCHAR(20),
    payment_account VARCHAR(100),
    payment_date DATE,
    payment_voucher_no VARCHAR(100),
    reimbursement_date DATE,
    reimbursement_voucher_no VARCHAR(100),
    attachment_count INTEGER,
    is_urgent BOOLEAN,
    urgent_reason VARCHAR(500),
    expected_completion_date DATE,
    actual_completion_date DATE,
    exceed_budget BOOLEAN,
    exceed_amount NUMERIC(18,4),
    exceed_reason VARCHAR(500),
    approval_comment VARCHAR(1000),
    reject_reason VARCHAR(1000),
    cancel_reason VARCHAR(1000),
    expense_items TEXT,
    approval_records TEXT,
    attachments TEXT
);

-- source: /erp/erp-finance/src/main/java/cn/aiedge/erp/expense/model/ExpenseApproval.java
CREATE TABLE IF NOT EXISTS expense_approval (
    application_id VARCHAR(50) NOT NULL,
    expense_application VARCHAR(255),
    approval_level INTEGER NOT NULL,
    approver_id VARCHAR(50) NOT NULL,
    approver_name VARCHAR(100) NOT NULL,
    approver_department_id VARCHAR(50),
    approver_department_name VARCHAR(100),
    approval_action VARCHAR(20) NOT NULL,
    approval_comment VARCHAR(1000),
    approval_time TIMESTAMP NOT NULL,
    previous_status VARCHAR(30),
    current_status VARCHAR(30),
    remark VARCHAR(500)
);

-- source: /erp/erp-finance/src/main/java/cn/aiedge/erp/expense/model/ExpenseAttachment.java
CREATE TABLE IF NOT EXISTS expense_attachment (
    application_id VARCHAR(50) NOT NULL,
    expense_application VARCHAR(255),
    file_name VARCHAR(200) NOT NULL,
    original_file_name VARCHAR(200) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    file_type VARCHAR(100),
    file_extension VARCHAR(20),
    file_md5 VARCHAR(32),
    attachment_type VARCHAR(20),
    sort_order INTEGER,
    upload_time TIMESTAMP NOT NULL,
    uploader_id VARCHAR(50),
    uploader_name VARCHAR(100),
    is_valid BOOLEAN,
    remark VARCHAR(500)
);

-- source: /erp/erp-finance/src/main/java/cn/aiedge/erp/expense/model/ExpenseItem.java
CREATE TABLE IF NOT EXISTS expense_item (
    expense_application VARCHAR(255),
    item_name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    expense_date DATE NOT NULL,
    amount NUMERIC(18,4) NOT NULL,
    quantity NUMERIC(18,4),
    unit_price NUMERIC(18,4),
    unit VARCHAR(20),
    vendor_name VARCHAR(200),
    tax_rate NUMERIC(18,4),
    tax_amount NUMERIC(18,4),
    total_amount_with_tax NUMERIC(18,4),
    has_invoice BOOLEAN,
    invoice_number VARCHAR(100),
    invoice_date DATE,
    payment_method VARCHAR(20),
    account_code VARCHAR(50),
    budget_code VARCHAR(50),
    project_code VARCHAR(50),
    cost_center VARCHAR(50),
    is_personal BOOLEAN,
    is_reimbursable BOOLEAN,
    receipt_required BOOLEAN,
    receipt_attached BOOLEAN,
    attachment_id VARCHAR(100),
    is_verified BOOLEAN,
    verified_by VARCHAR(50),
    verified_date DATE,
    verification_comment VARCHAR(500),
    sequence_number INTEGER
);

-- source: /erp/erp-finance/src/main/java/cn/aiedge/erp/expense/model/ExpensePayment.java
CREATE TABLE IF NOT EXISTS expense_payment (
    application_id BIGINT NOT NULL,
    amount NUMERIC(18,4) NOT NULL,
    payment_method VARCHAR(20),
    payment_status VARCHAR(30) NOT NULL,
    payment_date DATE,
    payer_id VARCHAR(50),
    payer_name VARCHAR(100),
    voucher_no VARCHAR(100),
    bank_transaction_no VARCHAR(100)
);

-- source: /erp/erp-finance/src/main/java/cn/aiedge/erp/expense/model/ExpenseReimbursement.java
CREATE TABLE IF NOT EXISTS expense_reimbursement (
    application_id BIGINT NOT NULL,
    amount NUMERIC(18,4) NOT NULL,
    reimbursement_type VARCHAR(50),
    status VARCHAR(30) NOT NULL,
    bank_account VARCHAR(100),
    bank_name VARCHAR(200),
    applicant_id VARCHAR(50) NOT NULL,
    applicant_name VARCHAR(100) NOT NULL,
    department_id VARCHAR(50),
    department_name VARCHAR(100),
    apply_date DATE,
    voucher_no VARCHAR(100),
    payment_method VARCHAR(20)
);

-- source: /erp/erp-fixed-asset/src/main/java/cn/aiedge/erp/fixedasset/model/FixedAsset.java
CREATE TABLE IF NOT EXISTS fixed_asset (
    asset_code VARCHAR(50) NOT NULL,
    asset_name VARCHAR(200) NOT NULL,
    category_id BIGINT,
    category_name VARCHAR(100),
    purchase_date DATE,
    original_value NUMERIC(18,4),
    net_value NUMERIC(18,4),
    depreciation_method VARCHAR(30),
    useful_life INTEGER,
    salvage_value NUMERIC(18,4),
    salvage_rate NUMERIC(18,4),
    monthly_depreciation NUMERIC(18,4),
    accumulated_depreciation NUMERIC(18,4),
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
    asset_photo VARCHAR(500)
);

-- source: /erp/erp-fixed-asset/src/main/java/cn/aiedge/erp/fixedasset/model/FixedAssetCategory.java
CREATE TABLE IF NOT EXISTS fixed_asset_category (
    category_code VARCHAR(50),
    category_name VARCHAR(100) NOT NULL,
    parent_id BIGINT,
    sort_order INTEGER,
    default_depreciation_method VARCHAR(30),
    default_useful_life INTEGER,
    description VARCHAR(500)
);

-- source: /erp/erp-fixed-asset/src/main/java/cn/aiedge/erp/fixedasset/model/FixedAssetDepreciation.java
CREATE TABLE IF NOT EXISTS fixed_asset_depreciation (
    asset_id BIGINT NOT NULL,
    period VARCHAR(7) NOT NULL,
    depreciation_date DATE,
    period_amount NUMERIC(18,4),
    accumulated_depreciation NUMERIC(18,4),
    net_value NUMERIC(18,4),
    asset_original_value NUMERIC(18,4),
    asset_name VARCHAR(200),
    asset_code VARCHAR(50),
    status VARCHAR(20)
);

-- source: /erp/erp-fixed-asset/src/main/java/cn/aiedge/erp/fixedasset/model/FixedAssetDisposal.java
CREATE TABLE IF NOT EXISTS fixed_asset_disposal (
    disposal_no VARCHAR(50),
    asset_id BIGINT,
    asset_code VARCHAR(50),
    asset_name VARCHAR(200),
    disposal_date DATE,
    disposal_type VARCHAR(20),
    disposal_amount NUMERIC(18,4),
    net_value NUMERIC(18,4),
    gain_loss NUMERIC(18,4),
    reason VARCHAR(500),
    status VARCHAR(20),
    approval_comment VARCHAR(500)
);

-- source: /erp/erp-fixed-asset/src/main/java/cn/aiedge/erp/fixedasset/model/FixedAssetInventory.java
CREATE TABLE IF NOT EXISTS fixed_asset_inventory (
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
    remark VARCHAR(500),
    status VARCHAR(20)
);

-- source: /erp/erp-fixed-asset/src/main/java/cn/aiedge/erp/fixedasset/model/FixedAssetPurchase.java
CREATE TABLE IF NOT EXISTS fixed_asset_purchase (
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
    estimated_amount NUMERIC(18,4),
    actual_amount NUMERIC(18,4),
    apply_date DATE,
    expected_delivery_date DATE,
    actual_delivery_date DATE,
    purchase_reason VARCHAR(500),
    status VARCHAR(30),
    approval_comment VARCHAR(500),
    generated_asset_id BIGINT,
    specification VARCHAR(200),
    supplier_name VARCHAR(200),
    remark VARCHAR(500)
);

-- source: /erp/erp-fixed-asset/src/main/java/cn/aiedge/erp/fixedasset/model/FixedAssetTransfer.java
CREATE TABLE IF NOT EXISTS fixed_asset_transfer (
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
    transfer_time TIMESTAMP
);

-- source: /core/api/core-api/src/main/java/cn/aiedge/inventory/model/InventoryReportItem.java
CREATE TABLE IF NOT EXISTS inventory_report_items (
    id BIGSERIAL,
    inventory_report VARCHAR(255),
    product_id BIGINT NOT NULL,
    product_code VARCHAR(50),
    product_name VARCHAR(200),
    specification VARCHAR(100),
    unit VARCHAR(20),
    book_quantity NUMERIC(18,4),
    actual_quantity NUMERIC(18,4),
    diff_quantity NUMERIC(18,4),
    unit_price NUMERIC(18,4),
    diff_amount NUMERIC(18,4),
    diff_reason VARCHAR(500),
    status INTEGER,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    PRIMARY KEY (id)
);

-- source: /core/api/core-api/src/main/java/cn/aiedge/inventory/model/InventoryReport.java
CREATE TABLE IF NOT EXISTS inventory_reports (
    id BIGSERIAL,
    report_no VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    warehouse_id BIGINT NOT NULL,
    warehouse_name VARCHAR(100) NOT NULL,
    report_type INTEGER NOT NULL,
    report_time TIMESTAMP NOT NULL,
    period_start_time TIMESTAMP NOT NULL,
    period_end_time TIMESTAMP NOT NULL,
    total_product_types INTEGER,
    total_sku_count INTEGER,
    total_quantity INTEGER,
    total_value NUMERIC(18,4),
    available_value NUMERIC(18,4),
    safety_stock_ratio NUMERIC(18,4),
    turnover_days NUMERIC(18,4),
    stockout_rate NUMERIC(18,4),
    expired_product_count INTEGER,
    near_expiry_product_count INTEGER,
    status INTEGER NOT NULL,
    auditor_id BIGINT,
    auditor_name VARCHAR(50),
    audit_time TIMESTAMP,
    audit_opinion VARCHAR(500),
    publisher_id BIGINT,
    publisher_name VARCHAR(50),
    publish_time TIMESTAMP,
    archiver_id BIGINT,
    archiver_name VARCHAR(50),
    archive_time TIMESTAMP,
    last_data_time TIMESTAMP,
    report_file_path VARCHAR(500),
    report_file_size BIGINT,
    remark VARCHAR(1000),
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    create_user_id BIGINT,
    create_user_name VARCHAR(50),
    update_user_id BIGINT,
    update_user_name VARCHAR(50),
    inventory_report_items TEXT,
    PRIMARY KEY (id)
);

-- source: /erp/erp-finance/src/main/java/cn/aiedge/erp/invoice/model/entity/Invoice.java
CREATE TABLE IF NOT EXISTS invoice (
    invoice_number VARCHAR(50) NOT NULL,
    application_id BIGINT NOT NULL,
    invoice_type VARCHAR(20) NOT NULL,
    invoice_status VARCHAR(20) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    matching_status VARCHAR(20) NOT NULL,
    matched_amount NUMERIC(18,4) NOT NULL,
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
    exchange_rate NUMERIC(18,4),
    subtotal_amount NUMERIC(18,4) NOT NULL,
    tax_amount NUMERIC(18,4) NOT NULL,
    discount_amount NUMERIC(18,4),
    shipping_amount NUMERIC(18,4),
    other_amount NUMERIC(18,4),
    total_amount NUMERIC(18,4) NOT NULL,
    paid_amount NUMERIC(18,4),
    unpaid_amount NUMERIC(18,4),
    overdue_days INTEGER,
    late_fee_amount NUMERIC(18,4),
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
    items TEXT,
    taxes TEXT,
    payments TEXT
);

-- source: /erp/erp-finance/src/main/java/cn/aiedge/erp/invoice/model/entity/InvoiceApplication.java
CREATE TABLE IF NOT EXISTS invoice_application (
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
    exchange_rate NUMERIC(18,4),
    subtotal_amount NUMERIC(18,4) NOT NULL,
    tax_amount NUMERIC(18,4) NOT NULL,
    discount_amount NUMERIC(18,4),
    shipping_amount NUMERIC(18,4),
    other_amount NUMERIC(18,4),
    total_amount NUMERIC(18,4) NOT NULL,
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
    items TEXT,
    workflows TEXT
);

-- source: /erp/erp-finance/src/main/java/cn/aiedge/erp/invoice/model/entity/InvoiceApplicationItem.java
CREATE TABLE IF NOT EXISTS invoice_application_item (
    invoice_application VARCHAR(255),
    line_number INTEGER NOT NULL,
    item_code VARCHAR(50),
    item_name VARCHAR(200) NOT NULL,
    specification VARCHAR(200),
    unit VARCHAR(20) NOT NULL,
    quantity NUMERIC(18,4) NOT NULL,
    unit_price NUMERIC(18,4) NOT NULL,
    tax_rate NUMERIC(18,4),
    tax_amount NUMERIC(18,4),
    amount NUMERIC(18,4) NOT NULL,
    discount_rate NUMERIC(18,4),
    discount_amount NUMERIC(18,4),
    notes VARCHAR(500),
    project_code VARCHAR(50),
    cost_center VARCHAR(50)
);

-- source: /erp/erp-finance/src/main/java/cn/aiedge/erp/invoice/model/entity/InvoiceItem.java
CREATE TABLE IF NOT EXISTS invoice_item (
    invoice VARCHAR(255),
    line_number INTEGER NOT NULL,
    item_code VARCHAR(50),
    item_name VARCHAR(200) NOT NULL,
    specification VARCHAR(200),
    unit VARCHAR(20) NOT NULL,
    quantity NUMERIC(18,4) NOT NULL,
    unit_price NUMERIC(18,4) NOT NULL,
    tax_rate NUMERIC(18,4),
    tax_amount NUMERIC(18,4),
    amount NUMERIC(18,4) NOT NULL,
    discount_rate NUMERIC(18,4),
    discount_amount NUMERIC(18,4),
    notes VARCHAR(500),
    project_code VARCHAR(50),
    cost_center VARCHAR(50)
);

-- source: /erp/erp-finance/src/main/java/cn/aiedge/erp/invoice/model/entity/InvoicePayment.java
CREATE TABLE IF NOT EXISTS invoice_payment (
    invoice VARCHAR(255),
    payment_number VARCHAR(50),
    amount NUMERIC(18,4) NOT NULL,
    payment_date TIMESTAMP NOT NULL,
    payment_method VARCHAR(30),
    status VARCHAR(20) NOT NULL,
    bank_name VARCHAR(100),
    bank_account VARCHAR(50),
    transaction_number VARCHAR(100),
    paid_by BIGINT,
    paid_by_name VARCHAR(100),
    notes VARCHAR(500),
    voucher_id BIGINT
);

-- source: /erp/erp-finance/src/main/java/cn/aiedge/erp/invoice/model/entity/InvoiceTax.java
CREATE TABLE IF NOT EXISTS invoice_tax (
    invoice VARCHAR(255),
    tax_type VARCHAR(20) NOT NULL,
    tax_rate NUMERIC(18,4) NOT NULL,
    tax_base NUMERIC(18,4) NOT NULL,
    tax_amount NUMERIC(18,4) NOT NULL,
    tax_name VARCHAR(100),
    tax_code VARCHAR(50),
    is_deductible BOOLEAN,
    deduct_status VARCHAR(20)
);

-- source: /erp/erp-finance/src/main/java/cn/aiedge/erp/invoice/model/entity/InvoiceWorkflow.java
CREATE TABLE IF NOT EXISTS invoice_workflow (
    invoice_application VARCHAR(255),
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
    next_approver_name VARCHAR(100)
);

-- source: /core/api/core-api/src/main/java/cn/aiedge/order/model/OrderItem.java
CREATE TABLE IF NOT EXISTS order_items (
    id BIGSERIAL,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sku_code VARCHAR(100),
    product_name VARCHAR(200) NOT NULL,
    specification VARCHAR(500),
    unit_price NUMERIC(18,4) NOT NULL,
    quantity INTEGER NOT NULL,
    total_amount NUMERIC(18,4) NOT NULL,
    discount_amount NUMERIC(18,4),
    actual_amount NUMERIC(18,4) NOT NULL,
    tax_rate NUMERIC(18,4),
    tax_amount NUMERIC(18,4),
    weight NUMERIC(18,4),
    volume NUMERIC(18,4),
    batch_number VARCHAR(100),
    production_date TIMESTAMP,
    shelf_life_days INTEGER,
    status INTEGER NOT NULL,
    delivery_time TIMESTAMP,
    receipt_time TIMESTAMP,
    remark VARCHAR(500),
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    create_user_id BIGINT,
    update_user_id BIGINT,
    PRIMARY KEY (id)
);

-- source: /erp/erp-purchase/src/main/java/cn/aiedge/erp/purchase/entity/PurchaseInquiry.java
CREATE TABLE IF NOT EXISTS purchase_inquiry (
    id BIGSERIAL,
    inquiry_no VARCHAR(32) NOT NULL,
    title VARCHAR(200) NOT NULL,
    inquiry_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    requirement_desc TEXT,
    urgency_level VARCHAR(10),
    deadline_date TIMESTAMP NOT NULL,
    publish_date TIMESTAMP,
    close_date TIMESTAMP,
    department_id BIGINT,
    requester_id BIGINT,
    purchaser_id BIGINT NOT NULL,
    invited_supplier_ids TEXT,
    quote_count INTEGER,
    budget NUMERIC(18,4),
    approval_status VARCHAR(20),
    approved_by BIGINT,
    approval_date TIMESTAMP,
    approval_comment TEXT,
    related_order_id BIGINT,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted BOOLEAN NOT NULL,
    items TEXT,
    PRIMARY KEY (id)
);

-- source: /erp/erp-purchase/src/main/java/cn/aiedge/erp/purchase/entity/PurchaseInquiryItem.java
CREATE TABLE IF NOT EXISTS purchase_inquiry_item (
    id BIGSERIAL,
    inquiry_id BIGINT NOT NULL,
    item_seq INTEGER NOT NULL,
    material_id BIGINT,
    material_code VARCHAR(50),
    material_name VARCHAR(200) NOT NULL,
    specification TEXT,
    unit VARCHAR(20) NOT NULL,
    quantity NUMERIC(18,4) NOT NULL,
    min_quantity NUMERIC(18,4),
    quality_requirement TEXT,
    delivery_requirement TEXT,
    brand_requirement VARCHAR(200),
    estimated_price NUMERIC(18,4),
    estimated_amount NUMERIC(18,4),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    PRIMARY KEY (id)
);

-- source: /erp/erp-purchase/src/main/java/cn/aiedge/erp/purchase/entity/PurchaseQuoteComparison.java
CREATE TABLE IF NOT EXISTS purchase_quote_comparison (
    id BIGSERIAL,
    inquiry_id BIGINT NOT NULL,
    comparison_date TIMESTAMP NOT NULL,
    comparator_id BIGINT NOT NULL,
    comparison_method VARCHAR(20) NOT NULL,
    winning_quote_id BIGINT,
    winning_supplier_id BIGINT,
    winning_amount NUMERIC(18,4),
    savings_amount NUMERIC(18,4),
    savings_rate NUMERIC(18,4),
    comparison_detail TEXT,
    price_analysis TEXT,
    quality_analysis TEXT,
    service_analysis TEXT,
    recommendation TEXT,
    decision_basis TEXT,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

-- source: /erp/erp-purchase/src/main/java/cn/aiedge/erp/purchase/entity/PurchaseQuoteContract.java
CREATE TABLE IF NOT EXISTS purchase_quote_contract (
    id BIGSERIAL,
    quote_id BIGINT NOT NULL,
    contract_id BIGINT NOT NULL,
    conversion_date TIMESTAMP NOT NULL,
    converter_id BIGINT NOT NULL,
    contract_no VARCHAR(50),
    contract_amount NUMERIC(18,4),
    contract_status VARCHAR(20),
    fulfillment_status VARCHAR(20),
    fulfillment_progress NUMERIC(18,4),
    performance_score NUMERIC(18,4),
    performance_date TIMESTAMP,
    performance_comment TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    PRIMARY KEY (id)
);

-- source: /erp/erp-purchase/src/main/java/cn/aiedge/erp/purchase/entity/PurchaseQuoteItem.java
CREATE TABLE IF NOT EXISTS purchase_quote_item (
    id BIGSERIAL,
    quote_id BIGINT NOT NULL,
    inquiry_item_id BIGINT NOT NULL,
    material_name VARCHAR(200) NOT NULL,
    specification TEXT,
    unit VARCHAR(20) NOT NULL,
    quantity NUMERIC(18,4) NOT NULL,
    unit_price NUMERIC(18,4) NOT NULL,
    amount NUMERIC(18,4) NOT NULL,
    tax_rate NUMERIC(18,4),
    tax_amount NUMERIC(18,4),
    brand VARCHAR(200),
    model VARCHAR(200),
    quality_level VARCHAR(20),
    origin_country VARCHAR(50),
    lead_time INTEGER,
    delivery_location VARCHAR(200),
    item_note TEXT,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

-- source: /erp/erp-purchase/src/main/java/cn/aiedge/erp/purchase/entity/PurchaseSupplierQuote.java
CREATE TABLE IF NOT EXISTS purchase_supplier_quote (
    id BIGSERIAL,
    quote_no VARCHAR(32) NOT NULL,
    inquiry_id BIGINT NOT NULL,
    supplier_id BIGINT NOT NULL,
    supplier_code VARCHAR(50),
    supplier_name VARCHAR(200),
    contact_person VARCHAR(100),
    contact_phone VARCHAR(20),
    notes TEXT,
    quote_status VARCHAR(20) NOT NULL,
    quote_date TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    total_amount NUMERIC(18,4) NOT NULL,
    tax_rate NUMERIC(18,4),
    tax_amount NUMERIC(18,4),
    payment_terms TEXT,
    delivery_terms TEXT,
    warranty_terms TEXT,
    supplier_note TEXT,
    competitive_advantage TEXT,
    attachment_urls TEXT,
    price_score NUMERIC(18,4),
    quality_score NUMERIC(18,4),
    service_score NUMERIC(18,4),
    total_score NUMERIC(18,4),
    is_recommended BOOLEAN,
    recommend_reason TEXT,
    review_status VARCHAR(20),
    reviewed_by BIGINT,
    review_date TIMESTAMP,
    review_comment TEXT,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    PRIMARY KEY (id)
);

-- source: /erp/erp-supplier-portal/src/main/java/cn/aiedge/erp/supplier/entity/Supplier.java
CREATE TABLE IF NOT EXISTS supplier (
    id BIGSERIAL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    contact_person VARCHAR(100),
    contact_person_id BIGINT,
    email VARCHAR(255),
    phone VARCHAR(50),
    emergency_phone VARCHAR(50),
    address VARCHAR(500),
    category VARCHAR(100),
    status INTEGER,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    PRIMARY KEY (id)
);
