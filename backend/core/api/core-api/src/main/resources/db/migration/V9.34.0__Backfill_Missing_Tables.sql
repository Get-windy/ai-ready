-- V9.34.0: 补齐已存在但缺少迁移脚本的表
-- 使用 IF NOT EXISTS 确保幂等：老库跳过，新库创建

-- ── batch_flow_record ──
CREATE TABLE IF NOT EXISTS public.batch_flow_record (
    id bigint NOT NULL,
    batch_id bigint,
    batch_no character varying(100),
    product_id bigint,
    product_code character varying(64),
    product_name character varying(200),
    flow_type character varying(30),
    flow_no character varying(100),
    flow_id bigint,
    quantity_change numeric(18,4) DEFAULT 0,
    before_quantity numeric(18,4) DEFAULT 0,
    after_quantity numeric(18,4) DEFAULT 0,
    from_warehouse_id bigint,
    from_warehouse_name character varying(100),
    to_warehouse_id bigint,
    to_warehouse_name character varying(100),
    from_location_id bigint,
    to_location_id bigint,
    operator_id character varying(50),
    operator_name character varying(50),
    operator_ip character varying(50),
    remark character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    version integer DEFAULT 1,
    tenant_id bigint DEFAULT 1
);


-- ── batch_rule ──
CREATE TABLE IF NOT EXISTS public.batch_rule (
    id bigint NOT NULL,
    rule_name character varying(100),
    rule_code character varying(50),
    prefix character varying(20),
    date_format character varying(20),
    seq_length integer DEFAULT 4,
    seq_start integer DEFAULT 1,
    default_expiry_days integer,
    auto_expiry boolean DEFAULT true,
    expiry_warning_days integer DEFAULT 30,
    require_quality_check boolean DEFAULT false,
    quality_check_interval_days integer,
    status_flow text,
    product_category_ids character varying(500),
    enable boolean DEFAULT true,
    created_by character varying(50),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by character varying(50),
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    tenant_id bigint DEFAULT 1
);


-- ── batch_snapshot_cache ──
CREATE TABLE IF NOT EXISTS public.batch_snapshot_cache (
    id bigint NOT NULL,
    batch_id bigint,
    snapshot_date date,
    product_id bigint,
    batch_no character varying(100),
    total_quantity numeric(18,4) DEFAULT 0,
    available_quantity numeric(18,4) DEFAULT 0,
    reserved_quantity numeric(18,4) DEFAULT 0,
    quality_status character varying(30),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    tenant_id bigint DEFAULT 1
);


-- ── batchsn_audit_log ──
CREATE TABLE IF NOT EXISTS public.batchsn_audit_log (
    id bigint NOT NULL,
    table_name character varying(100),
    record_id bigint,
    operation_type character varying(20),
    old_data text,
    new_data text,
    operator_id character varying(50),
    operator_name character varying(50),
    operator_ip character varying(50),
    operation_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    tenant_id bigint DEFAULT 1
);


-- ── erp_business_metric ──
CREATE TABLE IF NOT EXISTS public.erp_business_metric (
    id bigint DEFAULT nextval('public.erp_business_metric_id_seq'::regclass) NOT NULL,
    tenant_id bigint DEFAULT 1,
    metric_code character varying(100),
    metric_name character varying(200),
    metric_type character varying(50),
    metric_value numeric(18,4) DEFAULT 0,
    unit character varying(20),
    period character varying(20),
    stat_time timestamp without time zone,
    dimension1_type character varying(50),
    dimension1_value character varying(100),
    dimension2_type character varying(50),
    dimension2_value character varying(100),
    dimension3_type character varying(50),
    dimension3_value character varying(100),
    chain_ratio numeric(10,4),
    year_ratio numeric(10,4),
    target_value numeric(18,4),
    completion_rate numeric(10,4),
    status character varying(20) DEFAULT 'normal'::character varying,
    remark character varying(500),
    deleted integer DEFAULT 0,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    create_by bigint,
    update_by bigint
);


-- ── erp_capital_flow ──
CREATE TABLE IF NOT EXISTS public.erp_capital_flow (
    id bigint NOT NULL,
    tenant_id bigint,
    flow_no character varying(50) NOT NULL,
    flow_type character varying(30) NOT NULL,
    direction character varying(10) NOT NULL,
    ref_id bigint,
    ref_no character varying(50) DEFAULT NULL::character varying,
    ref_type character varying(30) DEFAULT NULL::character varying,
    amount numeric(15,2) DEFAULT 0.00 NOT NULL,
    balance numeric(15,2) DEFAULT 0.00,
    party_type character varying(20) DEFAULT NULL::character varying,
    party_id bigint,
    party_name character varying(100) DEFAULT NULL::character varying,
    business_type character varying(50) DEFAULT NULL::character varying,
    occur_date timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    payment_method integer,
    bank_account character varying(50) DEFAULT NULL::character varying,
    bank_name character varying(100) DEFAULT NULL::character varying,
    transaction_no character varying(100) DEFAULT NULL::character varying,
    remark character varying(500) DEFAULT NULL::character varying,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


-- ── erp_discount_rule ──
CREATE TABLE IF NOT EXISTS public.erp_discount_rule (
    rule_id character varying(64) NOT NULL,
    rule_name character varying(200),
    discount_type character varying(50),
    description character varying(500),
    discount_rate numeric(10,4) DEFAULT 0,
    fixed_discount_amount numeric(18,2) DEFAULT 0,
    min_discount_amount numeric(18,2) DEFAULT 0,
    max_discount_amount numeric(18,2) DEFAULT 999999.99,
    condition_expression character varying(500),
    priority integer DEFAULT 100,
    enabled boolean DEFAULT true,
    effective_from timestamp without time zone,
    effective_to timestamp without time zone,
    min_purchase_quantity integer,
    min_purchase_amount numeric(18,2) DEFAULT 0,
    stackable boolean DEFAULT true,
    max_stack_count integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by character varying(64),
    updated_by character varying(64),
    version integer DEFAULT 1
);


-- ── erp_price_calculation_request ──
CREATE TABLE IF NOT EXISTS public.erp_price_calculation_request (
    request_id character varying(64) NOT NULL,
    product_id character varying(100),
    sku character varying(100),
    product_name character varying(200),
    category character varying(100),
    cost_price numeric(18,2),
    base_price numeric(18,2),
    market_reference_price numeric(18,2),
    customer_id character varying(100),
    customer_name character varying(200),
    customer_level character varying(50),
    quantity integer DEFAULT 1,
    purchase_amount numeric(18,2),
    sales_channel character varying(50),
    region character varying(50),
    country character varying(50),
    calculation_time timestamp without time zone,
    order_type character varying(50),
    order_id character varying(100),
    request_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    request_source character varying(50)
);


-- ── erp_price_calculation_result ──
CREATE TABLE IF NOT EXISTS public.erp_price_calculation_result (
    result_id character varying(64) NOT NULL,
    request_id character varying(64),
    original_base_price numeric(18,2),
    base_price numeric(18,2),
    primary_pricing_strategy_id character varying(64),
    primary_pricing_strategy_name character varying(200),
    total_discount_amount numeric(18,2) DEFAULT 0,
    total_discount_rate numeric(10,4) DEFAULT 0,
    discounted_price numeric(18,2),
    final_price numeric(18,2),
    unit_price numeric(18,2),
    total_price numeric(18,2),
    cost_price numeric(18,2),
    gross_profit_margin numeric(10,4),
    calculation_explanation character varying(2000),
    success boolean DEFAULT true,
    error_message character varying(500),
    error_code character varying(50),
    calculation_start_time timestamp without time zone,
    calculation_end_time timestamp without time zone,
    calculation_duration_ms bigint,
    engine_version character varying(50),
    cached boolean DEFAULT false,
    cache_key character varying(200),
    suggestion character varying(500)
);


-- ── erp_pricing_strategy ──
CREATE TABLE IF NOT EXISTS public.erp_pricing_strategy (
    strategy_id character varying(64) NOT NULL,
    strategy_name character varying(200),
    strategy_type character varying(50),
    description character varying(500),
    priority integer DEFAULT 100,
    enabled boolean DEFAULT true,
    effective_from timestamp without time zone,
    effective_to timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by character varying(64),
    updated_by character varying(64),
    version integer DEFAULT 1
);


-- ── erp_purchase_exchange ──
CREATE TABLE IF NOT EXISTS public.erp_purchase_exchange (
    id bigint NOT NULL,
    tenant_id bigint DEFAULT 1,
    exchange_no character varying(100),
    original_order_id bigint,
    original_order_no character varying(100),
    supplier_id bigint,
    supplier_name character varying(200),
    exchange_date timestamp without time zone,
    exchange_reason character varying(500),
    exchange_type integer DEFAULT 0,
    status integer DEFAULT 0,
    remark character varying(500),
    total_amount numeric(18,2) DEFAULT 0,
    created_by bigint,
    created_by_name character varying(100),
    approved_by bigint,
    approved_by_name character varying(100),
    approved_time timestamp without time zone,
    completed_time timestamp without time zone,
    deleted integer DEFAULT 0,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    version integer DEFAULT 0
);


-- ── erp_purchase_exchange_item ──
CREATE TABLE IF NOT EXISTS public.erp_purchase_exchange_item (
    id bigint NOT NULL,
    exchange_id bigint,
    original_item_id bigint,
    tenant_id bigint,
    product_id bigint,
    product_name character varying(200),
    product_code character varying(100),
    product_spec character varying(200),
    original_quantity numeric(19,4),
    exchange_quantity numeric(19,4),
    original_price numeric(19,4),
    exchange_price numeric(19,4),
    unit character varying(50),
    batch_no character varying(100),
    warehouse_id bigint,
    warehouse_name character varying(200),
    remark character varying(500),
    deleted integer DEFAULT 0,
    create_by bigint,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_by bigint,
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


-- ── erp_sale_exchange ──
CREATE TABLE IF NOT EXISTS public.erp_sale_exchange (
    id bigint NOT NULL,
    tenant_id bigint,
    exchange_no character varying(64),
    original_order_id bigint,
    original_order_no character varying(64),
    customer_id bigint,
    customer_name character varying(200),
    exchange_date timestamp without time zone,
    exchange_reason character varying(500),
    exchange_type integer,
    status integer,
    remark character varying(500),
    total_amount numeric(18,4),
    created_by bigint,
    created_by_name character varying(100),
    approved_by bigint,
    approved_by_name character varying(100),
    approved_time timestamp without time zone,
    completed_time timestamp without time zone,
    deleted integer DEFAULT 0,
    create_time timestamp without time zone,
    update_time timestamp without time zone,
    version integer DEFAULT 0
);


-- ── erp_sale_exchange_approval_record ──
CREATE TABLE IF NOT EXISTS public.erp_sale_exchange_approval_record (
    id bigint NOT NULL,
    exchange_id bigint,
    action character varying(64),
    action_name character varying(100),
    operator_id bigint,
    operator_name character varying(100),
    remark character varying(500),
    deleted integer DEFAULT 0,
    create_by bigint,
    create_time timestamp without time zone
);


-- ── erp_sale_exchange_item ──
CREATE TABLE IF NOT EXISTS public.erp_sale_exchange_item (
    id bigint NOT NULL,
    exchange_id bigint,
    original_item_id bigint,
    product_id bigint,
    product_name character varying(200),
    product_code character varying(64),
    product_spec character varying(200),
    original_quantity numeric(18,4),
    exchange_quantity numeric(18,4),
    original_price numeric(18,4),
    exchange_price numeric(18,4),
    unit character varying(32),
    batch_no character varying(64),
    warehouse_id bigint,
    warehouse_name character varying(200),
    remark character varying(500),
    deleted integer DEFAULT 0,
    create_by bigint,
    create_time timestamp without time zone,
    update_by bigint,
    update_time timestamp without time zone
);


-- ── erp_sale_outbound ──
CREATE TABLE IF NOT EXISTS public.erp_sale_outbound (
    id bigint NOT NULL,
    tenant_id bigint DEFAULT 1,
    outbound_no character varying(100),
    order_id bigint,
    order_no character varying(100),
    customer_id bigint,
    customer_name character varying(200),
    contact_id bigint,
    contact_name character varying(100),
    outbound_date date,
    outbound_type integer DEFAULT 0,
    status integer DEFAULT 0,
    total_quantity numeric(18,2) DEFAULT 0,
    total_amount numeric(18,2) DEFAULT 0,
    warehouse_id bigint,
    warehouse_name character varying(100),
    sales_person_id bigint,
    sales_person_name character varying(100),
    department_id bigint,
    department_name character varying(100),
    shipping_address character varying(500),
    receiver_name character varying(100),
    receiver_phone character varying(50),
    remark character varying(500),
    deleted integer DEFAULT 0,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    create_by bigint,
    update_by bigint
);


-- ── erp_stock ──
CREATE TABLE IF NOT EXISTS public.erp_stock (
    id bigint NOT NULL,
    tenant_id bigint DEFAULT 1,
    product_id bigint,
    product_name character varying(200),
    product_code character varying(100),
    warehouse_id bigint,
    warehouse_name character varying(100),
    quantity numeric(18,2) DEFAULT 0,
    deleted integer DEFAULT 0,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    serial_no character varying(100),
    sku character varying(100),
    available_quantity numeric(18,4),
    frozen_quantity numeric(18,4) DEFAULT 0,
    safety_stock numeric(18,2) DEFAULT 0,
    min_stock numeric(18,2) DEFAULT 0,
    max_stock numeric(18,2) DEFAULT 0,
    unit character varying(50),
    batch_no character varying(100),
    production_date timestamp without time zone,
    validity_date timestamp without time zone,
    supplier_id bigint,
    supplier_name character varying(200),
    remark text,
    create_by bigint,
    update_by bigint
);


-- ── erp_stock_alert_config ──
CREATE TABLE IF NOT EXISTS public.erp_stock_alert_config (
    id bigint NOT NULL,
    product_id bigint,
    warehouse_id bigint,
    min_quantity integer DEFAULT 0,
    max_quantity integer DEFAULT 0,
    enabled integer DEFAULT 1,
    remark character varying(500),
    tenant_id bigint DEFAULT 0,
    deleted integer DEFAULT 0,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    create_by character varying(50),
    update_by character varying(50),
    product_code character varying(64),
    product_name character varying(200),
    warehouse_name character varying(100),
    min_stock numeric(18,4) DEFAULT 0,
    max_stock numeric(18,4) DEFAULT 0,
    safety_stock numeric(18,4) DEFAULT 0,
    alert_type integer DEFAULT 1,
    expiry_alert_days integer DEFAULT 30,
    alert_receiver character varying(500),
    alert_email character varying(500),
    alert_phone character varying(100),
    alert_frequency integer DEFAULT 1,
    active integer DEFAULT 1,
    enable_low_stock_alert integer DEFAULT 1,
    enable_over_stock_alert integer DEFAULT 0,
    enable_expiry_alert integer DEFAULT 0
);


-- ── erp_supplier ──
CREATE TABLE IF NOT EXISTS public.erp_supplier (
    id bigint NOT NULL,
    tenant_id character varying(50),
    supplier_code character varying(100),
    supplier_name character varying(200),
    short_name character varying(100),
    supplier_type integer DEFAULT 0,
    enterprise_nature integer DEFAULT 0,
    credit_code character varying(100),
    business_license character varying(200),
    legal_person character varying(100),
    registered_capital double precision DEFAULT 0,
    establishment_date timestamp without time zone,
    business_scope text,
    contact_person character varying(100),
    contact_phone character varying(50),
    contact_email character varying(100),
    company_address character varying(500),
    postal_code character varying(20),
    website character varying(200),
    bank_name character varying(200),
    bank_account character varying(100),
    tax_number character varying(50),
    invoice_type integer DEFAULT 0,
    payment_method integer DEFAULT 0,
    payment_period integer DEFAULT 0,
    transport_method integer DEFAULT 0,
    cooperation_status integer DEFAULT 1,
    supplier_level character varying(50),
    comprehensive_score double precision DEFAULT 0,
    certification_status integer DEFAULT 0,
    portal_status integer DEFAULT 0,
    portal_account_id character varying(100),
    remark character varying(500),
    status integer DEFAULT 1,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    create_by character varying(64),
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_by character varying(64),
    version integer DEFAULT 0,
    deleted integer DEFAULT 0,
    category_tags text,
    location_info text,
    attachment_info text,
    extend_info text
);


-- ── fin_financial_report ──
CREATE TABLE IF NOT EXISTS public.fin_financial_report (
    id bigint NOT NULL,
    tenant_id bigint DEFAULT 1,
    report_name character varying(255),
    report_type character varying(32),
    report_data text,
    period character varying(32),
    status integer DEFAULT 0,
    remark text,
    create_by character varying(64),
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_by character varying(64),
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


-- ── fin_payment ──
CREATE TABLE IF NOT EXISTS public.fin_payment (
    id bigint NOT NULL,
    tenant_id bigint DEFAULT 1,
    payment_no character varying(64),
    payable_id bigint,
    supplier_id bigint,
    supplier_name character varying(255),
    amount numeric(18,2) DEFAULT 0,
    payment_date date,
    payment_method character varying(32),
    bank_account character varying(64),
    voucher_no character varying(64),
    remark text,
    status integer DEFAULT 0,
    create_by character varying(64),
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_by character varying(64),
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


-- ── fin_receipt ──
CREATE TABLE IF NOT EXISTS public.fin_receipt (
    id bigint NOT NULL,
    tenant_id bigint DEFAULT 1,
    receipt_no character varying(64),
    receivable_id bigint,
    customer_id bigint,
    customer_name character varying(255),
    amount numeric(18,2) DEFAULT 0,
    receipt_date date,
    payment_method character varying(32),
    bank_account character varying(64),
    voucher_no character varying(64),
    remark text,
    status integer DEFAULT 0,
    create_by character varying(64),
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_by character varying(64),
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


-- ── fin_receivable ──
CREATE TABLE IF NOT EXISTS public.fin_receivable (
    id bigint NOT NULL,
    tenant_id bigint DEFAULT 1,
    receivable_no character varying(64),
    customer_id bigint,
    customer_name character varying(255),
    contract_id bigint,
    contract_no character varying(64),
    original_amount numeric(18,2) DEFAULT 0,
    received_amount numeric(18,2) DEFAULT 0,
    remaining_amount numeric(18,2) DEFAULT 0,
    bill_date date,
    due_date date,
    status integer DEFAULT 0,
    overdue_days integer DEFAULT 0,
    remark text,
    create_by character varying(64),
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_by character varying(64),
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


-- ── fin_reconciliation ──
CREATE TABLE IF NOT EXISTS public.fin_reconciliation (
    id bigint NOT NULL,
    tenant_id bigint DEFAULT 1,
    reconciliation_no character varying(64),
    reconciliation_date date,
    total_amount numeric(18,2) DEFAULT 0,
    status integer DEFAULT 0,
    remark text,
    create_by character varying(64),
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_by character varying(64),
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0,
    reconciliation_type character varying(50),
    target_id bigint,
    target_name character varying(200),
    start_date timestamp without time zone,
    end_date timestamp without time zone,
    system_balance numeric(20,2) DEFAULT 0,
    actual_balance numeric(20,2) DEFAULT 0,
    difference numeric(20,2) DEFAULT 0,
    difference_reason character varying(500),
    handler_id character varying(100),
    handler_name character varying(200),
    party_a character varying(200),
    party_b character varying(200)
);


-- ── fin_reconciliation_item ──
CREATE TABLE IF NOT EXISTS public.fin_reconciliation_item (
    id bigint NOT NULL,
    reconciliation_id bigint,
    source_type character varying(32),
    source_id bigint,
    amount numeric(18,2) DEFAULT 0,
    is_matched integer DEFAULT 0,
    remark text,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


-- ── finance_account ──
CREATE TABLE IF NOT EXISTS public.finance_account (
    id bigint NOT NULL,
    account_name character varying(255),
    account_type integer,
    bank_name character varying(255),
    bank_account character varying(255),
    balance numeric(18,2) DEFAULT 0,
    status integer DEFAULT 1,
    currency character varying(50),
    account_level integer,
    deleted_flag integer DEFAULT 0,
    tenant_id bigint,
    remark character varying(500),
    created_by character varying(255),
    created_at timestamp without time zone,
    updated_by character varying(255),
    updated_at timestamp without time zone
);


-- ── finance_account_subject ──
CREATE TABLE IF NOT EXISTS public.finance_account_subject (
    id bigint NOT NULL,
    subject_code character varying(255),
    subject_name character varying(255),
    parent_id bigint,
    level integer,
    subject_type integer,
    direction integer,
    is_leaf integer DEFAULT 1,
    is_enabled integer DEFAULT 1,
    deleted_flag integer DEFAULT 0,
    tenant_id bigint,
    remark character varying(500),
    created_by character varying(255),
    created_at timestamp without time zone,
    updated_by character varying(255),
    updated_at timestamp without time zone
);


-- ── finance_ledger ──
CREATE TABLE IF NOT EXISTS public.finance_ledger (
    id bigint NOT NULL,
    tenant_id bigint,
    fiscal_year integer,
    fiscal_period integer,
    subject_id bigint,
    subject_code character varying(255),
    subject_name character varying(255),
    opening_debit numeric(18,2) DEFAULT 0,
    opening_credit numeric(18,2) DEFAULT 0,
    period_debit numeric(18,2) DEFAULT 0,
    period_credit numeric(18,2) DEFAULT 0,
    closing_debit numeric(18,2) DEFAULT 0,
    closing_credit numeric(18,2) DEFAULT 0,
    closing_balance numeric(18,2) DEFAULT 0,
    balance_direction integer DEFAULT 0,
    deleted_flag integer DEFAULT 0,
    created_by character varying(255),
    created_at timestamp without time zone,
    updated_by character varying(255),
    updated_at timestamp without time zone,
    remark character varying(500)
);


-- ── finance_payable ──
CREATE TABLE IF NOT EXISTS public.finance_payable (
    id bigint NOT NULL,
    source_type character varying(50),
    source_id bigint,
    source_no character varying(100),
    supplier_id character varying(100),
    supplier_name character varying(255),
    total_amount numeric(18,2) DEFAULT 0,
    paid_amount numeric(18,2) DEFAULT 0,
    remaining_amount numeric(18,2) DEFAULT 0,
    due_date date,
    invoice_date date,
    invoice_no character varying(100),
    status character varying(50),
    deleted_flag integer DEFAULT 0,
    tenant_id bigint,
    remark character varying(500),
    created_by character varying(255),
    created_at timestamp without time zone,
    updated_by character varying(255),
    updated_at timestamp without time zone
);


-- ── finance_receivable ──
CREATE TABLE IF NOT EXISTS public.finance_receivable (
    id bigint NOT NULL,
    source_type character varying(50),
    source_id bigint,
    source_no character varying(100),
    customer_id character varying(100),
    customer_name character varying(255),
    total_amount numeric(18,2) DEFAULT 0,
    paid_amount numeric(18,2) DEFAULT 0,
    remaining_amount numeric(18,2) DEFAULT 0,
    due_date date,
    invoice_date date,
    invoice_no character varying(100),
    status character varying(50),
    deleted_flag integer DEFAULT 0,
    tenant_id bigint,
    remark character varying(500),
    created_by character varying(255),
    created_at timestamp without time zone,
    updated_by character varying(255),
    updated_at timestamp without time zone
);


-- ── finance_report ──
CREATE TABLE IF NOT EXISTS public.finance_report (
    id bigint NOT NULL,
    report_no character varying(100),
    report_name character varying(255),
    report_type integer,
    report_period character varying(50),
    status integer DEFAULT 1,
    report_data text,
    file_url character varying(500),
    generated_by character varying(255),
    generated_at timestamp without time zone,
    approved_by character varying(255),
    approved_at timestamp without time zone,
    deleted_flag integer DEFAULT 0,
    tenant_id bigint,
    remark character varying(500),
    created_by character varying(255),
    created_at timestamp without time zone,
    updated_by character varying(255),
    updated_at timestamp without time zone
);


-- ── finance_tax_declaration ──
CREATE TABLE IF NOT EXISTS public.finance_tax_declaration (
    id bigint NOT NULL,
    declaration_no character varying(100),
    taxpayer_id character varying(100),
    taxpayer_name character varying(255),
    tax_type integer,
    declaration_period character varying(50),
    tax_amount numeric(18,2) DEFAULT 0,
    paid_amount numeric(18,2) DEFAULT 0,
    declaration_date timestamp without time zone,
    payment_date timestamp without time zone,
    status integer DEFAULT 1,
    declaration_file character varying(500),
    remark character varying(500),
    deleted_flag integer DEFAULT 0,
    tenant_id bigint,
    created_by character varying(255),
    created_at timestamp without time zone,
    updated_by character varying(255),
    updated_at timestamp without time zone
);


-- ── finance_transaction ──
CREATE TABLE IF NOT EXISTS public.finance_transaction (
    id bigint NOT NULL,
    transaction_no character varying(100),
    transaction_type integer,
    amount numeric(18,2) DEFAULT 0,
    transaction_time timestamp without time zone,
    credit_account_id bigint,
    debit_account_id bigint,
    biz_type integer,
    biz_id bigint,
    description character varying(500),
    status integer DEFAULT 1,
    voucher_no character varying(100),
    attachment_url character varying(500),
    approved_by character varying(255),
    approved_at timestamp without time zone,
    deleted_flag integer DEFAULT 0,
    tenant_id bigint,
    remark character varying(500),
    created_by character varying(255),
    created_at timestamp without time zone,
    updated_by character varying(255),
    updated_at timestamp without time zone
);


-- ── finance_voucher ──
CREATE TABLE IF NOT EXISTS public.finance_voucher (
    id bigint NOT NULL,
    voucher_no character varying(100),
    voucher_date date,
    fiscal_year integer,
    fiscal_period integer,
    attachments integer DEFAULT 0,
    prep_by character varying(255),
    prep_at timestamp without time zone,
    audit_by character varying(255),
    audit_at timestamp without time zone,
    post_by character varying(255),
    post_at timestamp without time zone,
    status character varying(50),
    total_debit numeric(18,2) DEFAULT 0,
    total_credit numeric(18,2) DEFAULT 0,
    deleted_flag integer DEFAULT 0,
    tenant_id bigint,
    remark character varying(500),
    created_by character varying(255),
    created_at timestamp without time zone,
    updated_by character varying(255),
    updated_at timestamp without time zone
);


-- ── finance_voucher_item ──
CREATE TABLE IF NOT EXISTS public.finance_voucher_item (
    id bigint NOT NULL,
    voucher_id bigint,
    summary character varying(500),
    subject_id bigint,
    subject_code character varying(255),
    subject_name character varying(255),
    debit_amount numeric(18,2) DEFAULT 0,
    credit_amount numeric(18,2) DEFAULT 0,
    source_type character varying(50),
    source_id bigint,
    source_no character varying(100),
    deleted_flag integer DEFAULT 0,
    tenant_id bigint,
    remark character varying(500),
    created_by character varying(255),
    created_at timestamp without time zone,
    updated_by character varying(255),
    updated_at timestamp without time zone
);


-- ── mall_address ──
CREATE TABLE IF NOT EXISTS public.mall_address (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    user_id bigint NOT NULL,
    consignee character varying(100),
    phone character varying(20),
    region character varying(200),
    address character varying(500),
    is_default integer DEFAULT 0,
    deleted integer DEFAULT 0,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp without time zone
);


-- ── mall_cart ──
CREATE TABLE IF NOT EXISTS public.mall_cart (
    id bigint NOT NULL,
    tenant_id bigint NOT NULL,
    user_id bigint NOT NULL,
    product_id character varying(50),
    product_name character varying(200),
    product_image character varying(500),
    price numeric(12,2),
    quantity integer DEFAULT 1,
    deleted integer DEFAULT 0,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp without time zone
);


-- ── serial_flow_record ──
CREATE TABLE IF NOT EXISTS public.serial_flow_record (
    id bigint NOT NULL,
    serial_id bigint,
    serial_no character varying(100),
    product_id bigint,
    product_code character varying(64),
    product_name character varying(200),
    flow_type character varying(30),
    flow_no character varying(100),
    flow_id bigint,
    from_status character varying(30),
    to_status character varying(30),
    from_stage character varying(30),
    to_stage character varying(30),
    from_location character varying(200),
    to_location character varying(200),
    operator_id character varying(50),
    operator_name character varying(50),
    remark character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    version integer DEFAULT 1,
    tenant_id bigint DEFAULT 1
);


-- ── serial_status_cache ──
CREATE TABLE IF NOT EXISTS public.serial_status_cache (
    id bigint NOT NULL,
    serial_id bigint,
    snapshot_date date,
    serial_no character varying(100),
    product_id bigint,
    sn_status character varying(30),
    sn_stage character varying(30),
    current_location character varying(200),
    warranty_end_date date,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    tenant_id bigint DEFAULT 1
);


-- ── sys_data_scope ──
CREATE TABLE IF NOT EXISTS public.sys_data_scope (
    id bigint NOT NULL,
    tenant_id bigint DEFAULT 1,
    role_id bigint NOT NULL,
    rule_type character varying(50) DEFAULT 'DEPT'::character varying NOT NULL,
    target_table character varying(100),
    target_field character varying(100) DEFAULT 'dept_id'::character varying,
    dept_ids text,
    custom_sql character varying(1000),
    remark character varying(500),
    status integer DEFAULT 1,
    deleted integer DEFAULT 0,
    version integer DEFAULT 0,
    create_by bigint,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_by bigint,
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


-- ── sys_field_permission ──
CREATE TABLE IF NOT EXISTS public.sys_field_permission (
    id bigint NOT NULL,
    tenant_id bigint DEFAULT 1,
    role_id bigint NOT NULL,
    target_table character varying(100) NOT NULL,
    target_field character varying(100) NOT NULL,
    visible integer DEFAULT 1,
    mask_type character varying(50),
    mask_char character varying(10) DEFAULT '*'::character varying,
    mask_prefix_len integer DEFAULT 0,
    mask_suffix_len integer DEFAULT 0,
    status integer DEFAULT 1,
    deleted integer DEFAULT 0,
    version integer DEFAULT 0,
    create_by bigint,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_by bigint,
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


-- ── sys_position ──
CREATE TABLE IF NOT EXISTS public.sys_position (
    id bigint NOT NULL,
    tenant_id bigint DEFAULT 1,
    position_code character varying(50),
    position_name character varying(100),
    category_id bigint,
    dept_id bigint,
    level integer DEFAULT 0,
    sort integer DEFAULT 0,
    status integer DEFAULT 1,
    description character varying(500),
    remark character varying(255),
    create_by character varying(64),
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_by character varying(64),
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


-- ── sys_position_category ──
CREATE TABLE IF NOT EXISTS public.sys_position_category (
    id bigint NOT NULL,
    tenant_id bigint DEFAULT 1,
    category_code character varying(50),
    category_name character varying(100),
    parent_id bigint DEFAULT 0,
    ancestors character varying(500) DEFAULT '0'::character varying,
    sort integer DEFAULT 0,
    status integer DEFAULT 1,
    description character varying(255),
    create_by character varying(64),
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_by character varying(64),
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted integer DEFAULT 0
);


-- ── sys_sod_rule ──
CREATE TABLE IF NOT EXISTS public.sys_sod_rule (
    id bigint NOT NULL,
    tenant_id bigint DEFAULT 1,
    rule_name character varying(100) NOT NULL,
    description character varying(500),
    conflict_role_ids text NOT NULL,
    status integer DEFAULT 1,
    deleted integer DEFAULT 0,
    version integer DEFAULT 0,
    create_by bigint,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_by bigint,
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


-- ── sys_user_position ──
CREATE TABLE IF NOT EXISTS public.sys_user_position (
    id bigint NOT NULL,
    user_id bigint,
    position_id bigint,
    is_primary integer DEFAULT 0,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


-- ── traceability_log ──
CREATE TABLE IF NOT EXISTS public.traceability_log (
    id bigint NOT NULL,
    trace_type character varying(20),
    trace_id bigint,
    trace_code character varying(100),
    trace_name character varying(200),
    query_type character varying(20),
    query_time_range_start timestamp without time zone,
    query_time_range_end timestamp without time zone,
    result_count integer,
    query_duration_ms integer,
    query_user_id character varying(50),
    query_user_name character varying(50),
    query_ip character varying(50),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    tenant_id bigint DEFAULT 1
);



-- ── 索引补全 ──
CREATE UNIQUE INDEX IF NOT EXISTS idx_metric_unique ON public.erp_business_metric USING btree (tenant_id, metric_code, period, stat_time);
CREATE INDEX IF NOT EXISTS idx_purchase_exchange_no ON public.erp_purchase_exchange USING btree (exchange_no);
CREATE INDEX IF NOT EXISTS idx_purchase_exchange_supplier_id ON public.erp_purchase_exchange USING btree (supplier_id);
CREATE INDEX IF NOT EXISTS idx_purchase_exchange_tenant_id ON public.erp_purchase_exchange USING btree (tenant_id);
CREATE INDEX IF NOT EXISTS idx_purchase_exchange_item_exchange ON public.erp_purchase_exchange_item USING btree (exchange_id);
