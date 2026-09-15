-- ============================================================
-- V5.0.0: DDL 整合 + 基础种子数据
--
-- 合并自 schema.sql (已废弃):
--   - 所有核心框架表 (sys_user/role/permission/menu/tenant/…)
--   - 业务表 (erp_product, erp_warehouse, crm_quotation/item)
--   - 缺失列补丁 (sys_user/sys_role/sys_tenant/erp_supplier/fin_payable)
--   - 基础种子数据 (默认租户/管理员/角色)
--   - 从 schema.sql 新补充 → users/roles/permissions/sys_department
--   - 从 code 新补充 → sys_dict_type/sys_dict_item
--
-- 所有 DDL 使用 IF NOT EXISTS，种子数据使用 ON CONFLICT，
-- 保证幂等（可重复执行不报错）。
-- ============================================================

-- ===============================================
-- 核心框架表
-- ===============================================

CREATE TABLE IF NOT EXISTS sys_user (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT NOT NULL DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            BIGINT,
    update_by            BIGINT,
    username             VARCHAR(100) NOT NULL,
    password             VARCHAR(200) NOT NULL,
    nickname             VARCHAR(100),
    real_name            VARCHAR(100),
    email                VARCHAR(200),
    phone                VARCHAR(50),
    avatar               VARCHAR(500),
    gender               INTEGER DEFAULT 0,
    user_type            INTEGER DEFAULT 0,
    is_super_admin       BOOLEAN DEFAULT FALSE,
    is_tenant_admin      BOOLEAN DEFAULT FALSE,
    status               INTEGER DEFAULT 0,
    dept_id              BIGINT,
    post_id              BIGINT,
    last_login_time      TIMESTAMP,
    last_login_ip        VARCHAR(50),
    data_scope           VARCHAR(50),
    login_count          INTEGER DEFAULT 0,
    password_update_time TIMESTAMP,
    ext_info             TEXT,
    remark               VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS sys_role (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT NOT NULL DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            BIGINT,
    update_by            BIGINT,
    role_code            VARCHAR(100) NOT NULL,
    role_name            VARCHAR(100) NOT NULL,
    role_type            VARCHAR(50),
    scope                VARCHAR(50) DEFAULT 'TENANT',
    data_scope           INTEGER DEFAULT 0,
    parent_id            BIGINT,
    sort                 INTEGER DEFAULT 0,
    status               INTEGER DEFAULT 0,
    remark               VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id                   BIGINT PRIMARY KEY,
    user_id              BIGINT NOT NULL,
    role_id              BIGINT NOT NULL,
    tenant_id            BIGINT DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            BIGINT
);

CREATE TABLE IF NOT EXISTS sys_permission (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT NOT NULL DEFAULT 0,
    parent_id            BIGINT DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            BIGINT,
    update_by            BIGINT,
    permission_name      VARCHAR(100) NOT NULL,
    permission_code      VARCHAR(200) NOT NULL,
    permission_type      INTEGER DEFAULT 1,
    path                 VARCHAR(500),
    component            VARCHAR(500),
    icon                 VARCHAR(200),
    api_path             VARCHAR(500),
    method               VARCHAR(20),
    sort                 INTEGER DEFAULT 0,
    visible              INTEGER DEFAULT 1,
    status               INTEGER DEFAULT 0,
    remark               VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id                   BIGINT PRIMARY KEY,
    role_id              BIGINT NOT NULL,
    permission_id        BIGINT NOT NULL,
    tenant_id            BIGINT DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            BIGINT
);

CREATE TABLE IF NOT EXISTS sys_menu (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    parent_id            BIGINT DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    menu_name            VARCHAR(100) NOT NULL,
    menu_code            VARCHAR(100),
    menu_type            INTEGER DEFAULT 1,
    path                 VARCHAR(500),
    component            VARCHAR(500),
    route_name           VARCHAR(200),
    redirect             VARCHAR(500),
    icon                 VARCHAR(200),
    sort                 INTEGER DEFAULT 0,
    is_external          INTEGER DEFAULT 0,
    is_cache             INTEGER DEFAULT 1,
    visible              INTEGER DEFAULT 1,
    status               INTEGER DEFAULT 0,
    client_type          VARCHAR(50),
    remark               VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS sys_role_menu (
    id                   BIGINT PRIMARY KEY,
    role_id              BIGINT NOT NULL,
    menu_id              BIGINT NOT NULL,
    tenant_id            BIGINT DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_role_bill_type (
    id                   BIGINT PRIMARY KEY,
    role_id              BIGINT NOT NULL,
    bill_type            VARCHAR(50) NOT NULL,
    permission_level     INTEGER DEFAULT 0,
    tenant_id            BIGINT DEFAULT 0,
    created_by           BIGINT,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_tenant (
    id                   BIGINT PRIMARY KEY,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tenant_name          VARCHAR(200) NOT NULL,
    tenant_code          VARCHAR(100) NOT NULL,
    contact_person       VARCHAR(100),
    contact_phone        VARCHAR(50),
    contact_email        VARCHAR(200),
    address              VARCHAR(500),
    admin_user_id        BIGINT,
    level                VARCHAR(50),
    expire_time          TIMESTAMP,
    status               INTEGER DEFAULT 0,
    remark               VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS sys_tenant_module (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT NOT NULL DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            BIGINT,
    update_by            BIGINT,
    module_code          VARCHAR(100) NOT NULL,
    module_name          VARCHAR(200),
    purchase_type        VARCHAR(50),
    expire_time          TIMESTAMP,
    status               INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_user_tenant (
    id                   BIGINT PRIMARY KEY,
    user_id              BIGINT NOT NULL,
    tenant_id            BIGINT NOT NULL,
    is_default           BOOLEAN DEFAULT FALSE,
    status               INTEGER DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            BIGINT,
    update_by            BIGINT
);

CREATE TABLE IF NOT EXISTS sys_dept (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT NOT NULL DEFAULT 0,
    parent_id            BIGINT DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    dept_name            VARCHAR(200) NOT NULL,
    dept_code            VARCHAR(100),
    leader               VARCHAR(100),
    leader_id            BIGINT,
    phone                VARCHAR(50),
    email                VARCHAR(200),
    sort                 INTEGER DEFAULT 0,
    status               INTEGER DEFAULT 0,
    remark               VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS sys_project_config (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    config_key           VARCHAR(200) NOT NULL,
    config_value         TEXT,
    config_type          VARCHAR(50),
    config_group         VARCHAR(100),
    description          VARCHAR(500),
    status               INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_oper_log (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    user_id              BIGINT,
    username             VARCHAR(100),
    module               VARCHAR(100),
    action               VARCHAR(200),
    method               VARCHAR(200),
    request_url          VARCHAR(500),
    request_method       VARCHAR(20),
    request_params       TEXT,
    response_result      TEXT,
    status               INTEGER DEFAULT 0,
    error_msg            TEXT,
    oper_time            TIMESTAMP,
    cost_time            BIGINT,
    diff_data            TEXT,
    oper_ip              VARCHAR(50),
    oper_location        VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS sys_login_log (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    user_id              BIGINT,
    username             VARCHAR(100),
    login_type           INTEGER,
    login_result         INTEGER,
    fail_reason          VARCHAR(500),
    login_ip             VARCHAR(50),
    login_location       VARCHAR(200),
    browser              VARCHAR(200),
    os                   VARCHAR(200),
    device_type          VARCHAR(50),
    login_time           TIMESTAMP,
    logout_time          TIMESTAMP,
    token_id             VARCHAR(200),
    remark               VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS sys_message (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    msg_type             INTEGER,
    title                VARCHAR(200),
    content              TEXT,
    receiver_id          BIGINT,
    receiver_name        VARCHAR(100),
    receiver_contact     VARCHAR(200),
    template_code        VARCHAR(100),
    template_params      TEXT,
    send_status          INTEGER,
    send_time            TIMESTAMP,
    retry_count          INTEGER DEFAULT 0,
    fail_reason          VARCHAR(500),
    business_type        VARCHAR(100),
    business_id          BIGINT,
    is_read              INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_message_template (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            BIGINT,
    update_by            BIGINT,
    template_code        VARCHAR(100) NOT NULL,
    template_name        VARCHAR(200),
    msg_type             INTEGER,
    title                VARCHAR(200),
    content              TEXT,
    content_type         VARCHAR(50),
    description          VARCHAR(500),
    status               INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_permission_template (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            BIGINT,
    update_by            BIGINT,
    template_name        VARCHAR(200) NOT NULL,
    template_code        VARCHAR(100) NOT NULL,
    description          VARCHAR(500),
    template_type        INTEGER DEFAULT 1,
    permission_config    TEXT,
    status               INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_role_inheritance (
    id                   BIGINT PRIMARY KEY,
    parent_role_id       BIGINT NOT NULL,
    child_role_id        BIGINT NOT NULL,
    tenant_id            BIGINT DEFAULT 0,
    inheritance_type     INTEGER DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            BIGINT
);

CREATE TABLE IF NOT EXISTS sys_data_permission (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            BIGINT,
    update_by            BIGINT,
    permission_name      VARCHAR(200),
    permission_code      VARCHAR(100),
    data_scope           INTEGER,
    role_id              BIGINT,
    user_id              BIGINT,
    scope_type           INTEGER,
    status               INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS workflow_definition (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            BIGINT,
    update_by            BIGINT,
    process_code         VARCHAR(100) NOT NULL,
    process_name         VARCHAR(200),
    process_type         INTEGER,
    description          VARCHAR(500),
    process_config       TEXT,
    version              INTEGER DEFAULT 1,
    is_default           INTEGER DEFAULT 0,
    status               INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS workflow_instance (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    definition_id        BIGINT NOT NULL,
    business_id          BIGINT,
    business_type        VARCHAR(100),
    title                VARCHAR(200),
    applicant_id         BIGINT,
    applicant_name       VARCHAR(100),
    current_node_id      BIGINT,
    current_node_name    VARCHAR(200),
    status               INTEGER DEFAULT 0,
    result               INTEGER,
    comment              TEXT,
    finish_time          TIMESTAMP
);

CREATE TABLE IF NOT EXISTS workflow_task (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    instance_id          BIGINT NOT NULL,
    node_id              BIGINT,
    node_name            VARCHAR(200),
    task_type            INTEGER,
    assignee_id          BIGINT,
    assignee_name        VARCHAR(100),
    status               INTEGER DEFAULT 0,
    action               INTEGER,
    comment              TEXT,
    handle_time          TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_notification_template (
    id                   BIGINT PRIMARY KEY,
    template_code        VARCHAR(100) NOT NULL,
    template_name        VARCHAR(200),
    template_type        VARCHAR(50),
    content              TEXT,
    enabled              INTEGER DEFAULT 1,
    tenant_id            BIGINT DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===============================================
-- 字典表（原 schema.sql 中缺失）
-- ===============================================

CREATE TABLE IF NOT EXISTS sys_dict_type (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    dict_code            VARCHAR(100) NOT NULL,
    dict_name            VARCHAR(200) NOT NULL,
    description          VARCHAR(500),
    parent_id            BIGINT DEFAULT 0,
    sort_order           INTEGER DEFAULT 0,
    status               VARCHAR(20) DEFAULT 'ENABLED',
    is_built_in          VARCHAR(1) DEFAULT 'N',
    remark               VARCHAR(500),
    create_by            BIGINT,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by            BIGINT,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_dict_item (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    dict_type_id         BIGINT NOT NULL,
    item_value           VARCHAR(200) NOT NULL,
    item_text            VARCHAR(200) NOT NULL,
    description          VARCHAR(500),
    parent_id            BIGINT DEFAULT 0,
    sort_order           INTEGER DEFAULT 0,
    status               VARCHAR(20) DEFAULT 'ENABLED',
    is_default           VARCHAR(1) DEFAULT 'N',
    extra_attrs          TEXT,
    remark               VARCHAR(500),
    create_by            BIGINT,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by            BIGINT,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER DEFAULT 0
);

-- ===============================================
-- JPA 用户模块表
-- ===============================================

CREATE TABLE IF NOT EXISTS users (
    id                   BIGSERIAL PRIMARY KEY,
    username             VARCHAR(50) NOT NULL,
    password             VARCHAR(255) NOT NULL,
    email                VARCHAR(100),
    full_name            VARCHAR(100),
    phone                VARCHAR(20),
    status               VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    avatar_url           VARCHAR(500),
    department           VARCHAR(100),
    position             VARCHAR(100),
    last_login_at        TIMESTAMP,
    last_login_ip        VARCHAR(50),
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until         TIMESTAMP,
    password_changed_at  TIMESTAMP,
    email_verified       BOOLEAN DEFAULT FALSE,
    enabled              BOOLEAN DEFAULT TRUE,
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS roles (
    id                   BIGSERIAL PRIMARY KEY,
    name                 VARCHAR(50) NOT NULL,
    code                 VARCHAR(50) NOT NULL,
    description          VARCHAR(200),
    type                 VARCHAR(20) NOT NULL DEFAULT 'SYSTEM',
    level                INTEGER DEFAULT 1,
    enabled              BOOLEAN DEFAULT TRUE,
    is_default           BOOLEAN DEFAULT FALSE,
    sort_order           INTEGER DEFAULT 0,
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS permissions (
    id                   BIGSERIAL PRIMARY KEY,
    name                 VARCHAR(100) NOT NULL,
    code                 VARCHAR(100) NOT NULL,
    description          VARCHAR(200),
    type                 VARCHAR(20) NOT NULL DEFAULT 'MENU',
    resource             VARCHAR(200) NOT NULL,
    action               VARCHAR(20) NOT NULL DEFAULT 'READ',
    parent_id            BIGINT,
    level                INTEGER DEFAULT 1,
    sort_order           INTEGER DEFAULT 0,
    enabled              BOOLEAN DEFAULT TRUE,
    visible              BOOLEAN DEFAULT TRUE,
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id              BIGINT NOT NULL REFERENCES users(id),
    role_id              BIGINT NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id              BIGINT NOT NULL REFERENCES roles(id),
    permission_id        BIGINT NOT NULL REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

-- ===============================================
-- DepartmentMapper → sys_department（与 sys_dept 独立）
-- ===============================================

CREATE TABLE IF NOT EXISTS sys_department (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    parent_id            BIGINT DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            BIGINT,
    update_by            BIGINT,
    dept_code            VARCHAR(100),
    dept_name            VARCHAR(200) NOT NULL,
    ancestors            VARCHAR(500),
    leader_id            BIGINT,
    leader_name          VARCHAR(100),
    phone                VARCHAR(50),
    email                VARCHAR(200),
    sort                 INTEGER DEFAULT 0,
    status               INTEGER DEFAULT 0,
    remark               VARCHAR(500),
    version              INTEGER DEFAULT 0
);

-- ===============================================
-- 业务表
-- ===============================================

CREATE TABLE IF NOT EXISTS erp_product (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    product_code VARCHAR(50) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    unit VARCHAR(20),
    category VARCHAR(100),
    spec VARCHAR(200),
    status VARCHAR(20) DEFAULT 'ENABLED',
    remark TEXT,
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS erp_warehouse (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    warehouse_code VARCHAR(50) NOT NULL,
    warehouse_name VARCHAR(200) NOT NULL,
    address VARCHAR(500),
    contact_person VARCHAR(100),
    contact_phone VARCHAR(20),
    status VARCHAR(20) DEFAULT 'ENABLED',
    remark TEXT,
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS crm_quotation (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    quotation_no VARCHAR(50),
    version INT DEFAULT 1,
    parent_id BIGINT,
    customer_id BIGINT,
    customer_name VARCHAR(200),
    opportunity_id BIGINT,
    opportunity_name VARCHAR(200),
    contact_id BIGINT,
    contact_name VARCHAR(100),
    quotation_date DATE,
    valid_from DATE,
    valid_to DATE,
    status INT DEFAULT 0,
    quotation_type INT DEFAULT 1,
    title VARCHAR(300),
    description TEXT,
    total_amount DECIMAL(20,2) DEFAULT 0,
    discount_rate DECIMAL(10,4) DEFAULT 0,
    discount_amount DECIMAL(20,2) DEFAULT 0,
    tax_rate DECIMAL(10,4) DEFAULT 0,
    tax_amount DECIMAL(20,2) DEFAULT 0,
    final_amount DECIMAL(20,2) DEFAULT 0,
    currency VARCHAR(10) DEFAULT 'CNY',
    exchange_rate DECIMAL(10,6) DEFAULT 1,
    sales_person_id BIGINT,
    sales_person_name VARCHAR(100),
    department_id BIGINT,
    department_name VARCHAR(100),
    payment_terms VARCHAR(200),
    payment_days INT,
    delivery_terms VARCHAR(200),
    delivery_days INT,
    delivery_address VARCHAR(500),
    receiver_name VARCHAR(100),
    receiver_phone VARCHAR(20),
    remark TEXT,
    internal_note TEXT,
    approved_by BIGINT,
    approved_time TIMESTAMP,
    approved_note VARCHAR(500),
    sent_by BIGINT,
    sent_time TIMESTAMP,
    sent_method VARCHAR(20),
    accepted_by BIGINT,
    accepted_time TIMESTAMP,
    accepted_note VARCHAR(500),
    rejected_by BIGINT,
    rejected_time TIMESTAMP,
    rejected_reason TEXT,
    converted_by BIGINT,
    converted_time TIMESTAMP,
    order_id BIGINT,
    order_no VARCHAR(50),
    win_probability INT,
    competitor_quote VARCHAR(500),
    competitor_price DECIMAL(20,2),
    ext_info TEXT,
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    version_no INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS crm_quotation_item (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    quotation_id BIGINT,
    line_no INT,
    product_id BIGINT,
    product_code VARCHAR(50),
    product_name VARCHAR(200),
    product_spec VARCHAR(200),
    product_unit VARCHAR(20),
    product_category_id BIGINT,
    category_name VARCHAR(100),
    quantity DECIMAL(20,4) DEFAULT 1,
    unit_price DECIMAL(20,4) DEFAULT 0,
    cost_price DECIMAL(20,4) DEFAULT 0,
    discount_rate DECIMAL(10,4) DEFAULT 0,
    discount_amount DECIMAL(20,2) DEFAULT 0,
    line_amount DECIMAL(20,2) DEFAULT 0,
    tax_rate DECIMAL(10,4) DEFAULT 0,
    tax_amount DECIMAL(20,2) DEFAULT 0,
    line_total DECIMAL(20,2) DEFAULT 0,
    description TEXT,
    remark TEXT,
    delivery_days INT,
    warranty_terms VARCHAR(500),
    service_terms VARCHAR(500),
    ext_info TEXT,
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

-- ===============================================
-- 缺失列补丁（兼容已有表缺少列的情况）
-- ===============================================

ALTER TABLE IF EXISTS sys_user ADD COLUMN IF NOT EXISTS is_super_admin BOOLEAN DEFAULT FALSE;
ALTER TABLE IF EXISTS sys_user ADD COLUMN IF NOT EXISTS is_tenant_admin BOOLEAN DEFAULT FALSE;
ALTER TABLE IF EXISTS sys_user ADD COLUMN IF NOT EXISTS dept_id BIGINT;
ALTER TABLE IF EXISTS sys_user ADD COLUMN IF NOT EXISTS post_id BIGINT;
ALTER TABLE IF EXISTS sys_user ADD COLUMN IF NOT EXISTS last_login_time TIMESTAMP;
ALTER TABLE IF EXISTS sys_user ADD COLUMN IF NOT EXISTS last_login_ip VARCHAR(50);
ALTER TABLE IF EXISTS sys_user ADD COLUMN IF NOT EXISTS data_scope VARCHAR(50);
ALTER TABLE IF EXISTS sys_user ADD COLUMN IF NOT EXISTS login_count INTEGER DEFAULT 0;
ALTER TABLE IF EXISTS sys_user ADD COLUMN IF NOT EXISTS password_update_time TIMESTAMP;
ALTER TABLE IF EXISTS sys_user ADD COLUMN IF NOT EXISTS ext_info TEXT;
ALTER TABLE IF EXISTS sys_user ADD COLUMN IF NOT EXISTS remark VARCHAR(500);
ALTER TABLE IF EXISTS sys_user ADD COLUMN IF NOT EXISTS update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE IF EXISTS sys_user ADD COLUMN IF NOT EXISTS update_by BIGINT;

ALTER TABLE IF EXISTS sys_role ADD COLUMN IF NOT EXISTS role_type VARCHAR(50);
ALTER TABLE IF EXISTS sys_role ADD COLUMN IF NOT EXISTS scope VARCHAR(50) DEFAULT 'TENANT';
ALTER TABLE IF EXISTS sys_role ADD COLUMN IF NOT EXISTS parent_id BIGINT;
ALTER TABLE IF EXISTS sys_role ADD COLUMN IF NOT EXISTS sort INTEGER DEFAULT 0;
ALTER TABLE IF EXISTS sys_role ADD COLUMN IF NOT EXISTS remark VARCHAR(500);
ALTER TABLE IF EXISTS sys_role ADD COLUMN IF NOT EXISTS update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE IF EXISTS sys_role ADD COLUMN IF NOT EXISTS update_by BIGINT;

ALTER TABLE IF EXISTS sys_tenant ADD COLUMN IF NOT EXISTS admin_user_id BIGINT;
ALTER TABLE IF EXISTS sys_tenant ADD COLUMN IF NOT EXISTS level VARCHAR(50);
ALTER TABLE IF EXISTS sys_tenant ADD COLUMN IF NOT EXISTS expire_time TIMESTAMP;
ALTER TABLE IF EXISTS sys_tenant ADD COLUMN IF NOT EXISTS remark VARCHAR(500);

-- erp_supplier 缺失列（与 SupplierEntity 对齐）
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS supplier_type INTEGER DEFAULT NULL;
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS enterprise_nature INTEGER DEFAULT NULL;
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS credit_code VARCHAR(500) DEFAULT NULL;
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS business_license VARCHAR(500) DEFAULT NULL;
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS legal_person VARCHAR(500) DEFAULT NULL;
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS registered_capital DOUBLE PRECISION DEFAULT NULL;
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS establishment_date TIMESTAMP DEFAULT NULL;
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS business_scope TEXT DEFAULT NULL;
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS company_address VARCHAR(500) DEFAULT NULL;
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS postal_code VARCHAR(50) DEFAULT NULL;
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS website VARCHAR(500) DEFAULT NULL;
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS invoice_type INTEGER DEFAULT NULL;
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS payment_method INTEGER DEFAULT NULL;
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS payment_period INTEGER DEFAULT NULL;
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS transport_method INTEGER DEFAULT NULL;
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS cooperation_status INTEGER DEFAULT 1;
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS supplier_level VARCHAR(50) DEFAULT NULL;
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS comprehensive_score DOUBLE PRECISION DEFAULT 0;
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS certification_status INTEGER DEFAULT 0;
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS portal_status INTEGER DEFAULT 0;
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS portal_account_id VARCHAR(200) DEFAULT NULL;
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS category_tags TEXT DEFAULT NULL;
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS location_info TEXT DEFAULT NULL;
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS attachment_info TEXT DEFAULT NULL;
ALTER TABLE IF EXISTS erp_supplier ADD COLUMN IF NOT EXISTS extend_info TEXT DEFAULT NULL;

-- fin_payable 缺失列
ALTER TABLE IF EXISTS fin_payable ADD COLUMN IF NOT EXISTS supplier_name VARCHAR(500) DEFAULT NULL;
ALTER TABLE IF EXISTS fin_payable ADD COLUMN IF NOT EXISTS contract_no VARCHAR(200) DEFAULT NULL;

-- ===============================================
-- 约束和索引（来自原有 V3.x 迁移，确保幂等）
-- ===============================================

-- sys_role.scope CHECK 约束
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_role_scope') THEN
    ALTER TABLE sys_role ADD CONSTRAINT ck_role_scope CHECK (scope IN ('PLATFORM', 'TENANT'));
  END IF;
END $$;

-- sys_tenant.level CHECK 约束
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_tenant_level') THEN
    ALTER TABLE sys_tenant ADD CONSTRAINT ck_tenant_level CHECK (level IN ('basic', 'professional', 'enterprise'));
  END IF;
END $$;

-- sys_tenant_module.purchase_type CHECK 约束
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_module_purchase_type') THEN
    ALTER TABLE sys_tenant_module ADD CONSTRAINT ck_module_purchase_type CHECK (purchase_type IN ('permanent', 'auto_renew', 'manual'));
  END IF;
END $$;

-- 超级管理员唯一索引（is_super_admin = TRUE 最多一条）
CREATE UNIQUE INDEX IF NOT EXISTS uk_super_admin ON sys_user (is_super_admin) WHERE is_super_admin = TRUE;

-- 租户管理员唯一索引（每租户 is_tenant_admin = TRUE 最多一条）
CREATE UNIQUE INDEX IF NOT EXISTS uk_tenant_admin ON sys_user (tenant_id, is_tenant_admin) WHERE is_tenant_admin = TRUE;

-- sys_oper_log.diff_data 补丁（V3.8.0 已加过，幂等）
ALTER TABLE IF EXISTS sys_oper_log ADD COLUMN IF NOT EXISTS diff_data TEXT;

-- ===============================================
-- 基础种子数据
-- ===============================================

-- 默认租户（ID=1）
INSERT INTO sys_tenant (id, tenant_name, tenant_code, status, create_time)
    VALUES (1, '系统租户', 'SYSTEM', 1, CURRENT_TIMESTAMP)
    ON CONFLICT (id) DO UPDATE SET
        tenant_name = EXCLUDED.tenant_name,
        tenant_code = EXCLUDED.tenant_code,
        status = EXCLUDED.status;

-- 超级管理员（密码: admin123，BCrypt加密）
INSERT INTO sys_user (id, tenant_id, username, password, nickname, is_super_admin, status, deleted, create_time)
    VALUES (1, 1, 'admin', '$2b$12$JlapdfR3RttFEaOGAJhRye9uAk0tVdUAcMQ0daAao2vhqjWDXgDlu', '超级管理员', TRUE, 1, 0, CURRENT_TIMESTAMP)
    ON CONFLICT (id) DO UPDATE SET
        tenant_id = EXCLUDED.tenant_id,
        username = EXCLUDED.username,
        password = EXCLUDED.password,
        nickname = EXCLUDED.nickname,
        is_super_admin = EXCLUDED.is_super_admin,
        status = EXCLUDED.status,
        deleted = EXCLUDED.deleted;

-- 超级管理员角色
INSERT INTO sys_role (id, tenant_id, role_code, role_name, role_type, data_scope, status, deleted, create_time)
    VALUES (1, 1, 'SUPER_ADMIN', '超级管理员', 0, 0, 0, 0, CURRENT_TIMESTAMP)
    ON CONFLICT (id) DO UPDATE SET
        tenant_id = EXCLUDED.tenant_id,
        role_code = EXCLUDED.role_code,
        role_name = EXCLUDED.role_name,
        role_type = EXCLUDED.role_type,
        data_scope = EXCLUDED.data_scope,
        status = EXCLUDED.status,
        deleted = EXCLUDED.deleted;

-- 分配超级管理员角色
INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
    VALUES (1, 1, 1, 1, CURRENT_TIMESTAMP)
    ON CONFLICT (id) DO UPDATE SET
        user_id = EXCLUDED.user_id,
        role_id = EXCLUDED.role_id,
        tenant_id = EXCLUDED.tenant_id;

-- 关联 admin 用户与系统租户
DELETE FROM sys_user_tenant WHERE user_id = 1 AND tenant_id = 1;
INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time)
    VALUES (1, 1, 1, TRUE, 1, CURRENT_TIMESTAMP)
    ON CONFLICT (id) DO UPDATE SET
        user_id = EXCLUDED.user_id,
        tenant_id = EXCLUDED.tenant_id,
        is_default = EXCLUDED.is_default,
        status = EXCLUDED.status;

-- 默认系统配置
INSERT INTO sys_project_config (id, tenant_id, config_key, config_value, config_group, status, deleted, create_time)
    VALUES (1, 1, 'system.title', '企智连·AI-Ready', 'system', 0, 0, CURRENT_TIMESTAMP)
    ON CONFLICT (id) DO UPDATE SET
        tenant_id = EXCLUDED.tenant_id,
        config_key = EXCLUDED.config_key,
        config_value = EXCLUDED.config_value,
        config_group = EXCLUDED.config_group,
        status = EXCLUDED.status,
        deleted = EXCLUDED.deleted;
