-- V9.11.0 创建CRM核心业务表（客户/合同/报价/营销）
-- 补充V9.10.0已建拜访管理表之后的全部缺失CRM数据表

-- ============================================================
-- CRM 客户管理
-- ============================================================

-- 客户主表
CREATE TABLE IF NOT EXISTS crm_customer (
    id                       BIGSERIAL    PRIMARY KEY,
    customer_code            VARCHAR(50)  NOT NULL,
    customer_name            VARCHAR(200) NOT NULL,
    short_name               VARCHAR(100),
    customer_type            INTEGER,
    customer_source          INTEGER,
    industry_type            INTEGER,
    province                 VARCHAR(50),
    city                     VARCHAR(50),
    district                 VARCHAR(50),
    address                  VARCHAR(200),
    phone                    VARCHAR(50),
    fax                      VARCHAR(50),
    email                    VARCHAR(100),
    website                  VARCHAR(200),
    legal_person             VARCHAR(100),
    business_contact         VARCHAR(100),
    business_contact_phone   VARCHAR(50),
    finance_contact          VARCHAR(100),
    finance_contact_phone    VARCHAR(50),
    tax_number               VARCHAR(50),
    bank_name                VARCHAR(200),
    bank_account             VARCHAR(100),
    customer_level           INTEGER,
    customer_level_desc      VARCHAR(100),
    credit_limit             DECIMAL(18,2) DEFAULT 0,
    current_debt             DECIMAL(18,2) DEFAULT 0,
    settlement_type          INTEGER,
    settlement_days          INTEGER,
    status                   INTEGER      DEFAULT 0,
    status_desc              VARCHAR(100),
    sales_person_id          BIGINT,
    sales_person_name        VARCHAR(100),
    department_id            BIGINT,
    department_name          VARCHAR(100),
    first_trade_date         DATE,
    last_trade_date          DATE,
    trade_count              INTEGER      DEFAULT 0,
    trade_amount             DECIMAL(18,2) DEFAULT 0,
    potential_amount         DECIMAL(18,2) DEFAULT 0,
    remark                   VARCHAR(500),
    created_by               VARCHAR(64),
    created_at               TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by               VARCHAR(64),
    updated_at               TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted                  INTEGER      NOT NULL DEFAULT 0,
    version                  INTEGER      NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_crm_customer_code ON crm_customer(customer_code);
CREATE INDEX IF NOT EXISTS idx_crm_customer_name ON crm_customer(customer_name);
CREATE INDEX IF NOT EXISTS idx_crm_customer_sales ON crm_customer(sales_person_id);

-- 客户线索表
CREATE TABLE IF NOT EXISTS crm_customer_lead (
    id                       BIGSERIAL    PRIMARY KEY,
    lead_code                VARCHAR(50)  NOT NULL,
    lead_name                VARCHAR(200) NOT NULL,
    contact_name             VARCHAR(100),
    contact_phone            VARCHAR(50),
    contact_email            VARCHAR(100),
    company_name             VARCHAR(200),
    industry_type            INTEGER,
    lead_source              INTEGER,
    lead_status              INTEGER      DEFAULT 0,
    lead_status_desc         VARCHAR(100),
    lead_level               INTEGER,
    estimated_amount         DECIMAL(18,2) DEFAULT 0,
    province                 VARCHAR(50),
    city                     VARCHAR(50),
    address                  VARCHAR(200),
    requirement              TEXT,
    remark                   VARCHAR(500),
    sales_person_id          BIGINT,
    sales_person_name        VARCHAR(100),
    department_id            BIGINT,
    department_name          VARCHAR(100),
    expected_close_date      DATE,
    actual_close_date        DATE,
    converted_customer_id    BIGINT,
    converted_time           TIMESTAMP,
    created_by               VARCHAR(64),
    created_at               TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by               VARCHAR(64),
    updated_at               TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted                  INTEGER      NOT NULL DEFAULT 0,
    version                  INTEGER      NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_crm_lead_code ON crm_customer_lead(lead_code);
CREATE INDEX IF NOT EXISTS idx_crm_lead_sales ON crm_customer_lead(sales_person_id);
CREATE INDEX IF NOT EXISTS idx_crm_lead_converted ON crm_customer_lead(converted_customer_id);

-- 客户商机表
CREATE TABLE IF NOT EXISTS crm_customer_opportunity (
    id                       BIGSERIAL    PRIMARY KEY,
    opportunity_code         VARCHAR(50)  NOT NULL,
    opportunity_name         VARCHAR(200) NOT NULL,
    customer_id              BIGINT       NOT NULL,
    customer_name            VARCHAR(200),
    lead_id                  BIGINT,
    opportunity_stage        INTEGER      DEFAULT 0,
    opportunity_stage_desc   VARCHAR(100),
    estimated_amount         DECIMAL(18,2) DEFAULT 0,
    actual_amount            DECIMAL(18,2) DEFAULT 0,
    probability              INTEGER      DEFAULT 0,
    opportunity_type         INTEGER,
    opportunity_source       INTEGER,
    product_interest         VARCHAR(500),
    requirement              TEXT,
    competitor               VARCHAR(500),
    win_reason               VARCHAR(500),
    lose_reason              VARCHAR(500),
    status                   INTEGER      DEFAULT 0,
    status_desc              VARCHAR(100),
    sales_person_id          BIGINT,
    sales_person_name        VARCHAR(100),
    department_id            BIGINT,
    department_name          VARCHAR(100),
    expected_close_date      DATE,
    actual_close_date        DATE,
    remark                   VARCHAR(500),
    created_by               VARCHAR(64),
    created_at               TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by               VARCHAR(64),
    updated_at               TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted                  INTEGER      NOT NULL DEFAULT 0,
    version                  INTEGER      NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_crm_opportunity_code ON crm_customer_opportunity(opportunity_code);
CREATE INDEX IF NOT EXISTS idx_crm_opportunity_customer ON crm_customer_opportunity(customer_id);
CREATE INDEX IF NOT EXISTS idx_crm_opportunity_sales ON crm_customer_opportunity(sales_person_id);

-- 客户跟进记录表
CREATE TABLE IF NOT EXISTS crm_customer_follow_up (
    id                       BIGSERIAL    PRIMARY KEY,
    follow_up_code           VARCHAR(50)  NOT NULL,
    customer_id              BIGINT       NOT NULL,
    customer_name            VARCHAR(200),
    opportunity_id           BIGINT,
    opportunity_name         VARCHAR(200),
    lead_id                  BIGINT,
    lead_name                VARCHAR(200),
    follow_up_type           INTEGER,
    follow_up_type_desc      VARCHAR(100),
    contact_name             VARCHAR(100),
    contact_phone            VARCHAR(50),
    follow_up_date           DATE,
    content                  TEXT,
    next_action              VARCHAR(500),
    next_follow_up_date      DATE,
    follow_up_result         INTEGER,
    follow_up_result_desc    VARCHAR(100),
    sales_person_id          BIGINT,
    sales_person_name        VARCHAR(100),
    department_id            BIGINT,
    department_name          VARCHAR(100),
    remark                   VARCHAR(500),
    created_by               VARCHAR(64),
    created_at               TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by               VARCHAR(64),
    updated_at               TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted                  INTEGER      NOT NULL DEFAULT 0,
    version                  INTEGER      NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_crm_follow_up_code ON crm_customer_follow_up(follow_up_code);
CREATE INDEX IF NOT EXISTS idx_crm_follow_up_customer ON crm_customer_follow_up(customer_id);
CREATE INDEX IF NOT EXISTS idx_crm_follow_up_opportunity ON crm_customer_follow_up(opportunity_id);

-- 客户公海表
CREATE TABLE IF NOT EXISTS crm_customer_pool (
    id                            BIGSERIAL    PRIMARY KEY,
    tenant_id                     BIGINT,
    customer_id                   BIGINT       NOT NULL,
    customer_code                 VARCHAR(50),
    customer_name                 VARCHAR(200),
    pool_type                     INTEGER,
    pool_type_desc                VARCHAR(100),
    pool_reason                   INTEGER,
    pool_reason_desc              VARCHAR(200),
    original_sales_person_id      BIGINT,
    original_sales_person_name    VARCHAR(100),
    original_department_id        BIGINT,
    original_department_name      VARCHAR(100),
    pool_time                     TIMESTAMP,
    pool_days                     INTEGER      DEFAULT 0,
    expire_time                   TIMESTAMP,
    status                        INTEGER      DEFAULT 0,
    status_desc                   VARCHAR(100),
    claim_sales_person_id         BIGINT,
    claim_sales_person_name       VARCHAR(100),
    claim_department_id           BIGINT,
    claim_department_name         VARCHAR(100),
    claim_time                    TIMESTAMP,
    remark                        VARCHAR(500),
    created_by                    VARCHAR(64),
    created_at                    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by                    VARCHAR(64),
    updated_at                    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted                       INTEGER      NOT NULL DEFAULT 0,
    version                       INTEGER      NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_crm_pool_customer ON crm_customer_pool(customer_id);
CREATE INDEX IF NOT EXISTS idx_crm_pool_tenant ON crm_customer_pool(tenant_id);
CREATE INDEX IF NOT EXISTS idx_crm_pool_status ON crm_customer_pool(status);

-- ============================================================
-- CRM 合同管理
-- ============================================================

-- 合同主表
CREATE TABLE IF NOT EXISTS crm_contract (
    id                 BIGSERIAL    PRIMARY KEY,
    tenant_id          BIGINT,
    contract_no        VARCHAR(64)  NOT NULL,
    contract_name      VARCHAR(200) NOT NULL,
    contract_type      INTEGER,
    customer_id        BIGINT,
    customer_name      VARCHAR(200),
    opportunity_id     BIGINT,
    opportunity_name   VARCHAR(200),
    quotation_id       BIGINT,
    quotation_no       VARCHAR(64),
    contact_id         BIGINT,
    contact_name       VARCHAR(100),
    sign_date          DATE,
    start_date         DATE,
    end_date           DATE,
    status             INTEGER      DEFAULT 0,
    sign_method        INTEGER,
    contract_amount    DECIMAL(18,2) DEFAULT 0,
    paid_amount        DECIMAL(18,2) DEFAULT 0,
    pending_amount     DECIMAL(18,2) DEFAULT 0,
    currency           VARCHAR(10)   DEFAULT 'CNY',
    exchange_rate      DECIMAL(10,4) DEFAULT 1,
    payment_terms      VARCHAR(500),
    payment_days       INTEGER,
    payment_method     INTEGER,
    delivery_terms     VARCHAR(500),
    delivery_days      INTEGER,
    warranty_terms     VARCHAR(500),
    warranty_months    INTEGER,
    service_terms      VARCHAR(500),
    service_months     INTEGER,
    title              VARCHAR(200),
    description        TEXT,
    remark             VARCHAR(500),
    internal_note      VARCHAR(500),
    sales_person_id    BIGINT,
    sales_person_name  VARCHAR(100),
    department_id      BIGINT,
    department_name    VARCHAR(100),
    approved_by        BIGINT,
    approved_time      TIMESTAMP,
    approved_note      VARCHAR(500),
    signed_by          BIGINT,
    signed_time        TIMESTAMP,
    signed_location    VARCHAR(200),
    effective_by       BIGINT,
    effective_time     TIMESTAMP,
    terminated_by      BIGINT,
    terminated_time    TIMESTAMP,
    terminated_reason  VARCHAR(500),
    renewed_by         BIGINT,
    renewed_time       TIMESTAMP,
    renewed_contract_id   BIGINT,
    renewed_contract_no   VARCHAR(64),
    renewal_count      INTEGER      DEFAULT 0,
    auto_renewal       BOOLEAN      DEFAULT FALSE,
    renewal_notice_days   INTEGER   DEFAULT 30,
    need_review        BOOLEAN      DEFAULT FALSE,
    reviewed_by        BIGINT,
    reviewed_time      TIMESTAMP,
    reviewed_note      VARCHAR(500),
    execution_progress INTEGER      DEFAULT 0,
    execution_amount   DECIMAL(18,2) DEFAULT 0,
    risk_level         INTEGER      DEFAULT 0,
    risk_note          VARCHAR(500),
    archive_location   VARCHAR(500),
    archive_time       TIMESTAMP,
    archived_by        BIGINT,
    ext_info           TEXT,
    deleted            INTEGER      NOT NULL DEFAULT 0,
    version_no         INTEGER      NOT NULL DEFAULT 0,
    create_by          BIGINT,
    create_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by          BIGINT,
    update_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_crm_contract_no ON crm_contract(contract_no);
CREATE INDEX IF NOT EXISTS idx_crm_contract_customer ON crm_contract(customer_id);
CREATE INDEX IF NOT EXISTS idx_crm_contract_tenant ON crm_contract(tenant_id);

-- 合同条款表
CREATE TABLE IF NOT EXISTS crm_contract_clause (
    id              BIGSERIAL    PRIMARY KEY,
    tenant_id       BIGINT,
    contract_id     BIGINT       NOT NULL,
    clause_no       INTEGER,
    clause_title    VARCHAR(200),
    clause_content  TEXT,
    clause_type     INTEGER,
    mandatory       BOOLEAN      DEFAULT FALSE,
    editable        BOOLEAN      DEFAULT TRUE,
    sort_order      INTEGER      DEFAULT 0,
    remark          VARCHAR(500),
    deleted         INTEGER      NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_crm_clause_contract ON crm_contract_clause(contract_id);

-- 合同付款计划表
CREATE TABLE IF NOT EXISTS crm_contract_payment (
    id              BIGSERIAL    PRIMARY KEY,
    tenant_id       BIGINT,
    contract_id     BIGINT       NOT NULL,
    payment_no      INTEGER,
    payment_name    VARCHAR(200),
    payment_stage   INTEGER,
    stage_desc      VARCHAR(200),
    plan_amount     DECIMAL(18,2) DEFAULT 0,
    actual_amount   DECIMAL(18,2) DEFAULT 0,
    plan_date       TIMESTAMP,
    actual_date     TIMESTAMP,
    status          INTEGER      DEFAULT 0,
    invoice_id      BIGINT,
    invoice_no      VARCHAR(64),
    remark          VARCHAR(500),
    approved_by     BIGINT,
    approved_time   TIMESTAMP,
    deleted         INTEGER      NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_crm_payment_contract ON crm_contract_payment(contract_id);

-- 合同变更记录表
CREATE TABLE IF NOT EXISTS crm_contract_change (
    id              BIGSERIAL    PRIMARY KEY,
    tenant_id       BIGINT,
    contract_id     BIGINT       NOT NULL,
    change_no       VARCHAR(64)  NOT NULL,
    change_type     INTEGER,
    change_title    VARCHAR(200),
    change_content  TEXT,
    change_reason   VARCHAR(500),
    status          INTEGER      DEFAULT 0,
    proposed_by     BIGINT,
    proposed_time   TIMESTAMP,
    approved_by     BIGINT,
    approved_time   TIMESTAMP,
    approved_note   VARCHAR(500),
    executed_by     BIGINT,
    executed_time   TIMESTAMP,
    remark          VARCHAR(500),
    deleted         INTEGER      NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_crm_contract_change_no ON crm_contract_change(change_no);
CREATE INDEX IF NOT EXISTS idx_crm_change_contract ON crm_contract_change(contract_id);

-- 合同附件表
CREATE TABLE IF NOT EXISTS crm_contract_attachment (
    id                BIGSERIAL    PRIMARY KEY,
    tenant_id         BIGINT,
    contract_id       BIGINT       NOT NULL,
    file_name         VARCHAR(200) NOT NULL,
    file_path         VARCHAR(500) NOT NULL,
    file_type         VARCHAR(50),
    file_size         BIGINT,
    attachment_type   INTEGER,
    description       VARCHAR(500),
    uploaded_by       BIGINT,
    uploaded_time     TIMESTAMP,
    signed            BOOLEAN      DEFAULT FALSE,
    signed_time       TIMESTAMP,
    signed_by         VARCHAR(100),
    signature_location VARCHAR(200),
    deleted           INTEGER      NOT NULL DEFAULT 0,
    create_by         BIGINT,
    create_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by         BIGINT,
    update_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_crm_attachment_contract ON crm_contract_attachment(contract_id);

-- ============================================================
-- CRM 报价管理
-- ============================================================

-- 报价模板表
CREATE TABLE IF NOT EXISTS crm_quotation_template (
    id                    BIGSERIAL    PRIMARY KEY,
    tenant_id             BIGINT,
    template_code         VARCHAR(64)  NOT NULL,
    template_name         VARCHAR(200) NOT NULL,
    description           VARCHAR(500),
    template_type         INTEGER,
    customer_id           BIGINT,
    customer_name         VARCHAR(200),
    product_category_id   BIGINT,
    category_name         VARCHAR(200),
    default_discount_rate DECIMAL(10,4),
    default_tax_rate      DECIMAL(10,4),
    payment_terms         VARCHAR(500),
    payment_days          INTEGER,
    delivery_terms        VARCHAR(500),
    delivery_days         INTEGER,
    terms_and_conditions  TEXT,
    footer_note           VARCHAR(500),
    active                BOOLEAN      DEFAULT TRUE,
    usage_count           INTEGER      DEFAULT 0,
    last_used_by          BIGINT,
    last_used_time        TIMESTAMP,
    ext_info              TEXT,
    deleted               INTEGER      NOT NULL DEFAULT 0,
    create_by             BIGINT,
    create_time           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by             BIGINT,
    update_time           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_crm_quotation_template_code ON crm_quotation_template(template_code);
CREATE INDEX IF NOT EXISTS idx_crm_template_tenant ON crm_quotation_template(tenant_id);

-- 报价模板明细表
CREATE TABLE IF NOT EXISTS crm_quotation_template_item (
    id                    BIGSERIAL    PRIMARY KEY,
    tenant_id             BIGINT,
    template_id           BIGINT       NOT NULL,
    line_no               INTEGER,
    product_id            BIGINT,
    product_code          VARCHAR(100),
    product_name          VARCHAR(200),
    product_spec          VARCHAR(200),
    product_unit          VARCHAR(50),
    default_quantity      DECIMAL(18,4) DEFAULT 1,
    default_price         DECIMAL(18,4) DEFAULT 0,
    default_discount_rate DECIMAL(10,4),
    optional              BOOLEAN      DEFAULT FALSE,
    description           VARCHAR(500),
    remark                VARCHAR(500),
    deleted               INTEGER      NOT NULL DEFAULT 0,
    create_by             BIGINT,
    create_time           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by             BIGINT,
    update_time           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_crm_template_item_template ON crm_quotation_template_item(template_id);

-- 报价单主表
CREATE TABLE IF NOT EXISTS crm_quotation (
    id                  BIGSERIAL    PRIMARY KEY,
    tenant_id           BIGINT,
    quotation_no        VARCHAR(64)  NOT NULL,
    version             INTEGER      DEFAULT 1,
    parent_id           BIGINT,
    customer_id         BIGINT,
    customer_name       VARCHAR(200),
    opportunity_id      BIGINT,
    opportunity_name    VARCHAR(200),
    contact_id          BIGINT,
    contact_name        VARCHAR(100),
    quotation_date      DATE,
    valid_from          DATE,
    valid_to            DATE,
    status              INTEGER      DEFAULT 0,
    quotation_type      INTEGER,
    title               VARCHAR(200),
    description         TEXT,
    total_amount        DECIMAL(18,2) DEFAULT 0,
    discount_rate       DECIMAL(10,4) DEFAULT 1,
    discount_amount     DECIMAL(18,2) DEFAULT 0,
    tax_rate            DECIMAL(10,4) DEFAULT 0,
    tax_amount          DECIMAL(18,2) DEFAULT 0,
    final_amount        DECIMAL(18,2) DEFAULT 0,
    currency            VARCHAR(10)   DEFAULT 'CNY',
    exchange_rate       DECIMAL(10,4) DEFAULT 1,
    sales_person_id     BIGINT,
    sales_person_name   VARCHAR(100),
    department_id       BIGINT,
    department_name     VARCHAR(100),
    payment_terms       VARCHAR(500),
    payment_days        INTEGER,
    delivery_terms      VARCHAR(500),
    delivery_days       INTEGER,
    delivery_address    VARCHAR(200),
    receiver_name       VARCHAR(100),
    receiver_phone      VARCHAR(50),
    remark              VARCHAR(500),
    internal_note       VARCHAR(500),
    approved_by         BIGINT,
    approved_time       TIMESTAMP,
    approved_note       VARCHAR(500),
    sent_by             BIGINT,
    sent_time           TIMESTAMP,
    sent_method         VARCHAR(50),
    accepted_by         BIGINT,
    accepted_time       TIMESTAMP,
    accepted_note       VARCHAR(500),
    rejected_by         BIGINT,
    rejected_time       TIMESTAMP,
    rejected_reason     VARCHAR(500),
    converted_by        BIGINT,
    converted_time      TIMESTAMP,
    order_id            BIGINT,
    order_no            VARCHAR(64),
    win_probability     INTEGER      DEFAULT 0,
    competitor_quote    VARCHAR(500),
    competitor_price    DECIMAL(18,2),
    ext_info            TEXT,
    deleted             INTEGER      NOT NULL DEFAULT 0,
    version_no          INTEGER      NOT NULL DEFAULT 0,
    create_by           BIGINT,
    create_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_crm_quotation_no ON crm_quotation(quotation_no);
CREATE INDEX IF NOT EXISTS idx_crm_quotation_customer ON crm_quotation(customer_id);
CREATE INDEX IF NOT EXISTS idx_crm_quotation_tenant ON crm_quotation(tenant_id);

-- 报价单明细表
CREATE TABLE IF NOT EXISTS crm_quotation_item (
    id                    BIGSERIAL    PRIMARY KEY,
    tenant_id             BIGINT,
    quotation_id          BIGINT       NOT NULL,
    line_no               INTEGER,
    product_id            BIGINT,
    product_code          VARCHAR(100),
    product_name          VARCHAR(200),
    product_spec          VARCHAR(200),
    product_unit          VARCHAR(50),
    product_category_id   BIGINT,
    category_name         VARCHAR(200),
    quantity              DECIMAL(18,4) DEFAULT 1,
    unit_price            DECIMAL(18,4) DEFAULT 0,
    cost_price            DECIMAL(18,4) DEFAULT 0,
    discount_rate         DECIMAL(10,4) DEFAULT 1,
    discount_amount       DECIMAL(18,2) DEFAULT 0,
    line_amount           DECIMAL(18,2) DEFAULT 0,
    tax_rate              DECIMAL(10,4) DEFAULT 0,
    tax_amount            DECIMAL(18,2) DEFAULT 0,
    line_total            DECIMAL(18,2) DEFAULT 0,
    description           VARCHAR(500),
    remark                VARCHAR(500),
    delivery_days         INTEGER,
    warranty_terms        VARCHAR(500),
    service_terms         VARCHAR(500),
    ext_info              TEXT,
    deleted               INTEGER      NOT NULL DEFAULT 0,
    create_by             BIGINT,
    create_time           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by             BIGINT,
    update_time           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_crm_quotation_item_parent ON crm_quotation_item(quotation_id);

-- ============================================================
-- CRM 营销管理
-- ============================================================

-- 营销活动主表
CREATE TABLE IF NOT EXISTS crm_marketing_campaign (
    id                       BIGSERIAL    PRIMARY KEY,
    tenant_id                BIGINT,
    campaign_code            VARCHAR(64)  NOT NULL,
    campaign_name            VARCHAR(200) NOT NULL,
    campaign_type            INTEGER,
    campaign_category        INTEGER,
    description              TEXT,
    objective                TEXT,
    start_date               DATE,
    end_date                 DATE,
    status                   INTEGER      DEFAULT 0,
    budget                   DECIMAL(18,2) DEFAULT 0,
    actual_cost              DECIMAL(18,2) DEFAULT 0,
    expected_revenue         DECIMAL(18,2) DEFAULT 0,
    actual_revenue           DECIMAL(18,2) DEFAULT 0,
    expected_leads           INTEGER      DEFAULT 0,
    actual_leads             INTEGER      DEFAULT 0,
    expected_opportunities   INTEGER      DEFAULT 0,
    actual_opportunities     INTEGER      DEFAULT 0,
    expected_orders          INTEGER      DEFAULT 0,
    actual_orders            INTEGER      DEFAULT 0,
    target_customer_count    INTEGER      DEFAULT 0,
    reached_customer_count   INTEGER      DEFAULT 0,
    responded_customer_count INTEGER      DEFAULT 0,
    converted_customer_count INTEGER      DEFAULT 0,
    target_audience          VARCHAR(500),
    target_region            VARCHAR(200),
    target_industry          INTEGER,
    target_customer_level    INTEGER,
    target_product_category  VARCHAR(500),
    owner_id                 BIGINT,
    owner_name               VARCHAR(100),
    department_id            BIGINT,
    department_name          VARCHAR(100),
    approved_by              BIGINT,
    approved_time            TIMESTAMP,
    approved_note            VARCHAR(500),
    started_by               BIGINT,
    started_time             TIMESTAMP,
    completed_by             BIGINT,
    completed_time           TIMESTAMP,
    completed_note           VARCHAR(500),
    roi                      INTEGER,
    conversion_rate          INTEGER,
    response_rate            INTEGER,
    remark                   VARCHAR(500),
    internal_note            VARCHAR(500),
    ext_info                 TEXT,
    deleted                  INTEGER      NOT NULL DEFAULT 0,
    version_no               INTEGER      NOT NULL DEFAULT 0,
    create_by                BIGINT,
    create_time              TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by                BIGINT,
    update_time              TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_crm_campaign_code ON crm_marketing_campaign(campaign_code);
CREATE INDEX IF NOT EXISTS idx_crm_campaign_tenant ON crm_marketing_campaign(tenant_id);
CREATE INDEX IF NOT EXISTS idx_crm_campaign_owner ON crm_marketing_campaign(owner_id);

-- 营销活动目标客户表
CREATE TABLE IF NOT EXISTS crm_marketing_target (
    id                  BIGSERIAL    PRIMARY KEY,
    tenant_id           BIGINT,
    campaign_id         BIGINT       NOT NULL,
    customer_id         BIGINT,
    customer_name       VARCHAR(200),
    contact_id          BIGINT,
    contact_name        VARCHAR(100),
    contact_phone       VARCHAR(50),
    contact_email       VARCHAR(100),
    customer_level      INTEGER,
    customer_industry   VARCHAR(100),
    customer_region     VARCHAR(100),
    target_priority     INTEGER      DEFAULT 0,
    target_status       INTEGER      DEFAULT 0,
    reach_status        INTEGER      DEFAULT 0,
    reach_time          TIMESTAMP,
    reach_channel       INTEGER,
    response_status     INTEGER      DEFAULT 0,
    response_time       TIMESTAMP,
    response_content    TEXT,
    conversion_status   INTEGER      DEFAULT 0,
    conversion_time     TIMESTAMP,
    lead_id             BIGINT,
    opportunity_id      BIGINT,
    order_id            BIGINT,
    order_amount        DECIMAL(18,2) DEFAULT 0,
    score               INTEGER,
    tags                VARCHAR(500),
    remark              VARCHAR(500),
    deleted             INTEGER      NOT NULL DEFAULT 0,
    create_by           BIGINT,
    create_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_crm_target_campaign ON crm_marketing_target(campaign_id);
CREATE INDEX IF NOT EXISTS idx_crm_target_customer ON crm_marketing_target(customer_id);

-- 营销活动执行记录表
CREATE TABLE IF NOT EXISTS crm_marketing_execution (
    id                  BIGSERIAL    PRIMARY KEY,
    tenant_id           BIGINT,
    campaign_id         BIGINT       NOT NULL,
    target_id           BIGINT,
    customer_id         BIGINT,
    customer_name       VARCHAR(200),
    contact_id          BIGINT,
    contact_name        VARCHAR(100),
    execution_type      INTEGER,
    execution_channel   INTEGER,
    execution_content   TEXT,
    execution_time      TIMESTAMP,
    executed_by         BIGINT,
    executed_by_name    VARCHAR(100),
    execution_result    INTEGER,
    result_note         VARCHAR(500),
    response_status     INTEGER      DEFAULT 0,
    response_time       TIMESTAMP,
    response_content    TEXT,
    lead_id             BIGINT,
    opportunity_id      BIGINT,
    cost                DECIMAL(18,2) DEFAULT 0,
    remark              VARCHAR(500),
    deleted             INTEGER      NOT NULL DEFAULT 0,
    create_by           BIGINT,
    create_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_crm_execution_campaign ON crm_marketing_execution(campaign_id);
CREATE INDEX IF NOT EXISTS idx_crm_execution_target ON crm_marketing_execution(target_id);

-- 营销内容素材表
CREATE TABLE IF NOT EXISTS crm_marketing_content (
    id                  BIGSERIAL    PRIMARY KEY,
    tenant_id           BIGINT,
    content_code        VARCHAR(64)  NOT NULL,
    content_name        VARCHAR(200) NOT NULL,
    content_type        INTEGER,
    content_title       VARCHAR(200),
    content_body        TEXT,
    content_url         VARCHAR(500),
    content_image       VARCHAR(500),
    content_video       VARCHAR(500),
    content_attachment  VARCHAR(500),
    content_category    INTEGER,
    description         VARCHAR(500),
    active              BOOLEAN      DEFAULT TRUE,
    usage_count         INTEGER      DEFAULT 0,
    last_used_time      TIMESTAMP,
    last_used_by        BIGINT,
    remark              VARCHAR(500),
    deleted             INTEGER      NOT NULL DEFAULT 0,
    create_by           BIGINT,
    create_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_crm_content_code ON crm_marketing_content(content_code);

-- 营销渠道配置表
CREATE TABLE IF NOT EXISTS crm_marketing_channel (
    id                  BIGSERIAL    PRIMARY KEY,
    tenant_id           BIGINT,
    channel_code        VARCHAR(64)  NOT NULL,
    channel_name        VARCHAR(200) NOT NULL,
    channel_type        INTEGER,
    description         VARCHAR(500),
    active              BOOLEAN      DEFAULT TRUE,
    cost_per_reach      DECIMAL(18,4),
    cost_per_conversion DECIMAL(18,4),
    avg_response_rate   INTEGER,
    avg_conversion_rate INTEGER,
    config              TEXT,
    remark              VARCHAR(500),
    deleted             INTEGER      NOT NULL DEFAULT 0,
    create_by           BIGINT,
    create_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_crm_channel_code ON crm_marketing_channel(channel_code);
