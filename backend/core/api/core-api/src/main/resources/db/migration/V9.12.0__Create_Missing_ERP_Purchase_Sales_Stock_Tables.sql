-- V9.12.0 创建ERP缺失业务表：采购、销售退货、库存调拨/盘点、批次/序列号
-- 补全实体已经定义但无对应DDL的数据表

-- ============================================================
-- 采购管理
-- ============================================================

-- 采购订单主表
CREATE TABLE IF NOT EXISTS erp_purchase_order (
    id                    BIGSERIAL    PRIMARY KEY,
    tenant_id             BIGINT,
    order_no              VARCHAR(64)  NOT NULL,
    supplier_id           BIGINT,
    supplier_name         VARCHAR(200),
    order_date            TIMESTAMP,
    delivery_date         TIMESTAMP,
    total_amount          DECIMAL(18,2) DEFAULT 0,
    tax_amount            DECIMAL(18,2) DEFAULT 0,
    discount_amount       DECIMAL(18,2) DEFAULT 0,
    paid_amount           DECIMAL(18,2) DEFAULT 0,
    status                INTEGER      DEFAULT 0,
    approval_status       INTEGER      DEFAULT 0,
    approval_user_id      BIGINT,
    approval_time         TIMESTAMP,
    warehouse_id          BIGINT,
    payment_method        VARCHAR(50),
    payment_status        INTEGER      DEFAULT 0,
    delivery_status       INTEGER      DEFAULT 0,
    remark                VARCHAR(500),
    purchaser_id          BIGINT,
    purchaser_name        VARCHAR(100),
    dept_id               BIGINT,
    currency_id           BIGINT,
    exchange_rate         DECIMAL(10,4) DEFAULT 1,
    total_amount_with_tax DECIMAL(18,2) DEFAULT 0,
    total_quantity        DECIMAL(18,4) DEFAULT 0,
    received_amount       DECIMAL(18,2) DEFAULT 0,
    fulfillment_percent   DECIMAL(5,2)  DEFAULT 0,
    contract_id           BIGINT,
    source_type           INTEGER,
    source_id             BIGINT,
    source_bill_no        VARCHAR(64),
    children_flag         INTEGER      DEFAULT 0,
    sale_order_no         VARCHAR(64),
    multi_check_level1    BIGINT,
    multi_check_level2    BIGINT,
    multi_check_level3    BIGINT,
    multi_check_level4    BIGINT,
    multi_check_level5    BIGINT,
    multi_check_level6    BIGINT,
    multi_check_date1     TIMESTAMP,
    multi_check_date2     TIMESTAMP,
    multi_check_date3     TIMESTAMP,
    multi_check_date4     TIMESTAMP,
    multi_check_date5     TIMESTAMP,
    multi_check_date6     TIMESTAMP,
    cur_check_level       INTEGER      DEFAULT 0,
    closed_flag           INTEGER      DEFAULT 0,
    cancellation_flag     INTEGER      DEFAULT 0,
    tran_status           INTEGER      DEFAULT 0,
    order_affirm          INTEGER      DEFAULT 0,
    payment_method_id     BIGINT,
    require_provide       VARCHAR(500),
    cash_discount         VARCHAR(100),
    settle_date           TIMESTAMP,
    settle_method_id      BIGINT,
    delivery_address      VARCHAR(500),
    last_modify_date      TIMESTAMP,
    supplier_confirmed    BOOLEAN      DEFAULT FALSE,
    supplier_confirm_time TIMESTAMP,
    shipped               BOOLEAN      DEFAULT FALSE,
    ship_time             TIMESTAMP,
    tracking_number       VARCHAR(100),
    estimated_arrival_time TIMESTAMP,
    received              BOOLEAN      DEFAULT FALSE,
    receive_time          TIMESTAMP,
    received_quantity     DECIMAL(18,4) DEFAULT 0,
    quality_check_result  VARCHAR(50),
    invoice_status        INTEGER      DEFAULT 0,
    invoice_number        VARCHAR(64),
    invoice_amount        DECIMAL(18,2) DEFAULT 0,
    invoice_date          TIMESTAMP,
    ext_info              TEXT,
    deleted               INTEGER      NOT NULL DEFAULT 0,
    create_time           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    create_by             BIGINT,
    update_by             BIGINT
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_purchase_order_no ON erp_purchase_order(order_no);
CREATE INDEX IF NOT EXISTS idx_erp_purchase_order_supplier ON erp_purchase_order(supplier_id);
CREATE INDEX IF NOT EXISTS idx_erp_purchase_order_tenant ON erp_purchase_order(tenant_id);

-- 采购订单明细表
CREATE TABLE IF NOT EXISTS purchase_order_item (
    id                    BIGSERIAL    PRIMARY KEY,
    order_id              BIGINT       NOT NULL,
    material_name         VARCHAR(200),
    specification         VARCHAR(200),
    unit                  VARCHAR(50),
    quantity              DECIMAL(18,4) DEFAULT 0,
    unit_price            DECIMAL(18,4) DEFAULT 0,
    amount                DECIMAL(18,2) DEFAULT 0,
    tax_rate              DECIMAL(10,4) DEFAULT 0,
    tax_amount            DECIMAL(18,2) DEFAULT 0,
    brand                 VARCHAR(100),
    model                 VARCHAR(100),
    quality_level         VARCHAR(50),
    origin_country        VARCHAR(100),
    lead_time             INTEGER,
    delivery_location     VARCHAR(200),
    received_quantity     DECIMAL(18,4) DEFAULT 0,
    fulfillment_percent   DECIMAL(5,2)  DEFAULT 0,
    item_note             VARCHAR(500),
    created_at            TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_purchase_order_item_order ON purchase_order_item(order_id);

-- 采购退货单表
CREATE TABLE IF NOT EXISTS erp_purchase_return (
    id                  BIGSERIAL    PRIMARY KEY,
    tenant_id           BIGINT,
    return_no           VARCHAR(64)  NOT NULL,
    purchase_order_id   BIGINT,
    purchase_order_no   VARCHAR(64),
    supplier_id         BIGINT,
    supplier_name       VARCHAR(200),
    return_type         INTEGER,
    total_quantity      DECIMAL(18,4) DEFAULT 0,
    total_amount        DECIMAL(18,2) DEFAULT 0,
    status              INTEGER      DEFAULT 0,
    applicant_id        BIGINT,
    applicant_name      VARCHAR(100),
    apply_time          TIMESTAMP,
    approved_by         BIGINT,
    approved_time       TIMESTAMP,
    approved_note       VARCHAR(500),
    reason              VARCHAR(500),
    remark              VARCHAR(500),
    deleted             INTEGER      NOT NULL DEFAULT 0,
    version             INTEGER      NOT NULL DEFAULT 0,
    create_by           BIGINT,
    create_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_purchase_return_no ON erp_purchase_return(return_no);
CREATE INDEX IF NOT EXISTS idx_erp_purchase_return_order ON erp_purchase_return(purchase_order_id);

-- 采购退货明细表
CREATE TABLE IF NOT EXISTS erp_purchase_return_item (
    id                  BIGSERIAL    PRIMARY KEY,
    tenant_id           BIGINT,
    return_id           BIGINT       NOT NULL,
    line_no             INTEGER,
    product_id          BIGINT,
    product_code        VARCHAR(100),
    product_name        VARCHAR(200),
    product_spec        VARCHAR(200),
    product_unit        VARCHAR(50),
    return_quantity     DECIMAL(18,4) DEFAULT 0,
    unit_price          DECIMAL(18,4) DEFAULT 0,
    line_amount         DECIMAL(18,2) DEFAULT 0,
    reason              VARCHAR(500),
    remark              VARCHAR(500),
    deleted             INTEGER      NOT NULL DEFAULT 0,
    create_by           BIGINT,
    create_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_erp_purchase_return_item_parent ON erp_purchase_return_item(return_id);

-- 采购入库单主表
CREATE TABLE IF NOT EXISTS erp_purchase_inbound (
    id                      BIGSERIAL    PRIMARY KEY,
    tenant_id               BIGINT,
    inbound_no              VARCHAR(64)  NOT NULL,
    order_id                BIGINT,
    order_no                VARCHAR(64),
    supplier_id             BIGINT,
    supplier_name           VARCHAR(200),
    contract_id             BIGINT,
    contract_no             VARCHAR(64),
    inbound_date            DATE,
    inbound_type            INTEGER,
    status                  INTEGER      DEFAULT 0,
    total_quantity          DECIMAL(18,4) DEFAULT 0,
    total_amount            DECIMAL(18,2) DEFAULT 0,
    tax_amount              DECIMAL(18,2) DEFAULT 0,
    total_amount_with_tax   DECIMAL(18,2) DEFAULT 0,
    warehouse_id            BIGINT,
    warehouse_name          VARCHAR(200),
    purchaser_id            BIGINT,
    purchaser_name          VARCHAR(100),
    department_id           BIGINT,
    department_name         VARCHAR(100),
    tracking_number         VARCHAR(100),
    logistics_company       VARCHAR(200),
    expected_arrival_time   TIMESTAMP,
    actual_arrival_time     TIMESTAMP,
    received_by             BIGINT,
    received_time           TIMESTAMP,
    quality_checked_by      BIGINT,
    quality_checked_time    TIMESTAMP,
    quality_check_result    VARCHAR(50),
    approved_by             BIGINT,
    approved_time           TIMESTAMP,
    approved_note           VARCHAR(500),
    warehouse_confirmed_by  BIGINT,
    warehouse_confirmed_time TIMESTAMP,
    completed_by            BIGINT,
    completed_time          TIMESTAMP,
    remark                  VARCHAR(500),
    internal_note           VARCHAR(500),
    ext_info                TEXT,
    deleted                 INTEGER      NOT NULL DEFAULT 0,
    version_no              INTEGER      NOT NULL DEFAULT 0,
    create_time             TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time             TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    create_by               BIGINT,
    update_by               BIGINT
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_purchase_inbound_no ON erp_purchase_inbound(inbound_no);
CREATE INDEX IF NOT EXISTS idx_erp_purchase_inbound_order ON erp_purchase_inbound(order_id);
CREATE INDEX IF NOT EXISTS idx_erp_purchase_inbound_tenant ON erp_purchase_inbound(tenant_id);

-- 采购入库明细表
CREATE TABLE IF NOT EXISTS erp_purchase_inbound_item (
    id                      BIGSERIAL    PRIMARY KEY,
    tenant_id               BIGINT,
    inbound_id              BIGINT       NOT NULL,
    line_no                 INTEGER,
    product_id              BIGINT,
    product_code            VARCHAR(100),
    product_name            VARCHAR(200),
    product_spec            VARCHAR(200),
    product_unit            VARCHAR(50),
    order_item_id           BIGINT,
    order_quantity          DECIMAL(18,4) DEFAULT 0,
    inbound_quantity        DECIMAL(18,4) DEFAULT 0,
    pending_quantity        DECIMAL(18,4) DEFAULT 0,
    unit_price              DECIMAL(18,4) DEFAULT 0,
    unit_cost               DECIMAL(18,4) DEFAULT 0,
    line_amount             DECIMAL(18,2) DEFAULT 0,
    tax_rate                DECIMAL(10,4) DEFAULT 0,
    tax_amount              DECIMAL(18,2) DEFAULT 0,
    line_total              DECIMAL(18,2) DEFAULT 0,
    batch_no                VARCHAR(100),
    production_date         TIMESTAMP,
    validity_date           TIMESTAMP,
    warehouse_location_id   BIGINT,
    warehouse_location_code VARCHAR(100),
    quality_status          VARCHAR(50),
    quality_note            VARCHAR(500),
    remark                  VARCHAR(500),
    deleted                 INTEGER      NOT NULL DEFAULT 0,
    create_time             TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time             TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    create_by               BIGINT,
    update_by               BIGINT
);
CREATE INDEX IF NOT EXISTS idx_erp_purchase_inbound_item_parent ON erp_purchase_inbound_item(inbound_id);

-- ============================================================
-- 销售退货管理
-- ============================================================

-- 销售退货单表
CREATE TABLE IF NOT EXISTS erp_sale_return (
    id                  BIGSERIAL    PRIMARY KEY,
    tenant_id           BIGINT,
    return_no           VARCHAR(64)  NOT NULL,
    sale_order_id       BIGINT,
    sale_order_no       VARCHAR(64),
    customer_id         BIGINT,
    customer_name       VARCHAR(200),
    return_type         INTEGER,
    total_quantity      DECIMAL(18,4) DEFAULT 0,
    total_amount        DECIMAL(18,2) DEFAULT 0,
    status              INTEGER      DEFAULT 0,
    applicant_id        BIGINT,
    applicant_name      VARCHAR(100),
    apply_time          TIMESTAMP,
    approved_by         BIGINT,
    approved_time       TIMESTAMP,
    approved_note       VARCHAR(500),
    reason              VARCHAR(500),
    remark              VARCHAR(500),
    deleted             INTEGER      NOT NULL DEFAULT 0,
    version             INTEGER      NOT NULL DEFAULT 0,
    create_by           BIGINT,
    create_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_sale_return_no ON erp_sale_return(return_no);
CREATE INDEX IF NOT EXISTS idx_erp_sale_return_order ON erp_sale_return(sale_order_id);

-- 销售退货明细表
CREATE TABLE IF NOT EXISTS erp_sale_return_item (
    id                  BIGSERIAL    PRIMARY KEY,
    tenant_id           BIGINT,
    return_id           BIGINT       NOT NULL,
    line_no             INTEGER,
    product_id          BIGINT,
    product_code        VARCHAR(100),
    product_name        VARCHAR(200),
    product_spec        VARCHAR(200),
    product_unit        VARCHAR(50),
    return_quantity     DECIMAL(18,4) DEFAULT 0,
    unit_price          DECIMAL(18,4) DEFAULT 0,
    line_amount         DECIMAL(18,2) DEFAULT 0,
    reason              VARCHAR(500),
    remark              VARCHAR(500),
    deleted             INTEGER      NOT NULL DEFAULT 0,
    create_by           BIGINT,
    create_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_erp_sale_return_item_parent ON erp_sale_return_item(return_id);

-- ============================================================
-- 库存调拨管理
-- ============================================================

-- 调拨单主表
CREATE TABLE IF NOT EXISTS erp_stock_transfer (
    id                  BIGSERIAL    PRIMARY KEY,
    tenant_id           BIGINT,
    transfer_no         VARCHAR(64)  NOT NULL,
    transfer_type       INTEGER,
    from_warehouse_id   BIGINT,
    from_warehouse_name VARCHAR(200),
    to_warehouse_id     BIGINT,
    to_warehouse_name   VARCHAR(200),
    total_quantity      DECIMAL(18,4) DEFAULT 0,
    total_amount        DECIMAL(18,2) DEFAULT 0,
    status              INTEGER      DEFAULT 0,
    applicant_id        BIGINT,
    applicant_name      VARCHAR(100),
    apply_time          TIMESTAMP,
    approved_by         BIGINT,
    approved_time       TIMESTAMP,
    approved_note       VARCHAR(500),
    execute_by          BIGINT,
    execute_time        TIMESTAMP,
    total_items         INTEGER      DEFAULT 0,
    remark              VARCHAR(500),
    ext_info            TEXT,
    deleted             INTEGER      NOT NULL DEFAULT 0,
    version             INTEGER      NOT NULL DEFAULT 0,
    create_by           BIGINT,
    create_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_stock_transfer_no ON erp_stock_transfer(transfer_no);
CREATE INDEX IF NOT EXISTS idx_erp_stock_transfer_from_wh ON erp_stock_transfer(from_warehouse_id);
CREATE INDEX IF NOT EXISTS idx_erp_stock_transfer_tenant ON erp_stock_transfer(tenant_id);

-- 调拨明细表
CREATE TABLE IF NOT EXISTS erp_stock_transfer_item (
    id                  BIGSERIAL    PRIMARY KEY,
    tenant_id           BIGINT,
    transfer_id         BIGINT       NOT NULL,
    line_no             INTEGER,
    product_id          BIGINT,
    product_code        VARCHAR(100),
    product_name        VARCHAR(200),
    product_spec        VARCHAR(200),
    product_unit        VARCHAR(50),
    plan_quantity       DECIMAL(18,4) DEFAULT 0,
    actual_quantity     DECIMAL(18,4) DEFAULT 0,
    quantity            DECIMAL(18,4) DEFAULT 0,
    unit_cost           DECIMAL(18,4) DEFAULT 0,
    unit_price          DECIMAL(18,4) DEFAULT 0,
    status              INTEGER      DEFAULT 0,
    line_amount         DECIMAL(18,2) DEFAULT 0,
    batch_no            VARCHAR(100),
    production_date     TIMESTAMP,
    validity_date       TIMESTAMP,
    remark              VARCHAR(500),
    deleted             INTEGER      NOT NULL DEFAULT 0,
    create_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT
);
CREATE INDEX IF NOT EXISTS idx_erp_stock_transfer_item_parent ON erp_stock_transfer_item(transfer_id);

-- ============================================================
-- 库存盘点管理
-- ============================================================

-- 盘点单主表
CREATE TABLE IF NOT EXISTS erp_stock_check (
    id                      BIGSERIAL    PRIMARY KEY,
    tenant_id               BIGINT,
    check_no                VARCHAR(64)  NOT NULL,
    check_type              INTEGER,
    warehouse_id            BIGINT,
    warehouse_name          VARCHAR(200),
    check_date              TIMESTAMP,
    status                  INTEGER      DEFAULT 0,
    total_items             INTEGER      DEFAULT 0,
    checked_items           INTEGER      DEFAULT 0,
    diff_items              INTEGER      DEFAULT 0,
    total_book_quantity     DECIMAL(18,4) DEFAULT 0,
    total_actual_quantity   DECIMAL(18,4) DEFAULT 0,
    total_diff_quantity     DECIMAL(18,4) DEFAULT 0,
    total_book_amount       DECIMAL(18,2) DEFAULT 0,
    total_actual_amount     DECIMAL(18,2) DEFAULT 0,
    total_diff_amount       DECIMAL(18,2) DEFAULT 0,
    checker_id              BIGINT,
    checker_name            VARCHAR(100),
    supervisor_id           BIGINT,
    supervisor_name         VARCHAR(100),
    approved_by             BIGINT,
    approved_time           TIMESTAMP,
    approved_note           VARCHAR(500),
    adjusted_by             BIGINT,
    adjusted_time           TIMESTAMP,
    remark                  VARCHAR(500),
    ext_info                TEXT,
    deleted                 INTEGER      NOT NULL DEFAULT 0,
    version_no              INTEGER      NOT NULL DEFAULT 0,
    create_time             TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time             TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    create_by               BIGINT,
    update_by               BIGINT
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_stock_check_no ON erp_stock_check(check_no);
CREATE INDEX IF NOT EXISTS idx_erp_stock_check_warehouse ON erp_stock_check(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_erp_stock_check_tenant ON erp_stock_check(tenant_id);

-- 盘点明细表
CREATE TABLE IF NOT EXISTS erp_stock_check_item (
    id                  BIGSERIAL    PRIMARY KEY,
    tenant_id           BIGINT,
    check_id            BIGINT       NOT NULL,
    line_no             INTEGER,
    product_id          BIGINT,
    product_code        VARCHAR(100),
    product_name        VARCHAR(200),
    product_spec        VARCHAR(200),
    product_unit        VARCHAR(50),
    location_id         BIGINT,
    location_code       VARCHAR(100),
    batch_no            VARCHAR(100),
    book_quantity       DECIMAL(18,4) DEFAULT 0,
    actual_quantity     DECIMAL(18,4) DEFAULT 0,
    diff_quantity       DECIMAL(18,4) DEFAULT 0,
    diff_type           INTEGER,
    unit_cost           DECIMAL(18,4) DEFAULT 0,
    book_amount         DECIMAL(18,2) DEFAULT 0,
    actual_amount       DECIMAL(18,2) DEFAULT 0,
    diff_amount         DECIMAL(18,2) DEFAULT 0,
    check_status        INTEGER      DEFAULT 0,
    status              INTEGER      DEFAULT 0,
    check_note          VARCHAR(500),
    note                VARCHAR(500),
    checked_time        TIMESTAMP,
    checked_by          BIGINT,
    remark              VARCHAR(500),
    deleted             INTEGER      NOT NULL DEFAULT 0,
    create_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT
);
CREATE INDEX IF NOT EXISTS idx_erp_stock_check_item_parent ON erp_stock_check_item(check_id);

-- ============================================================
-- 批次号管理
-- ============================================================

-- 批次号主表
CREATE TABLE IF NOT EXISTS batch_number (
    id                      BIGSERIAL    PRIMARY KEY,
    batch_no                VARCHAR(100) NOT NULL,
    product_id              BIGINT,
    product_code            VARCHAR(100),
    product_name            VARCHAR(200),
    specification           VARCHAR(200),
    unit                    VARCHAR(50),
    production_date         DATE,
    expiration_date         DATE,
    batch_status            VARCHAR(30)   DEFAULT 'ACTIVE',
    total_quantity          DECIMAL(18,4) DEFAULT 0,
    available_quantity      DECIMAL(18,4) DEFAULT 0,
    reserved_quantity       DECIMAL(18,4) DEFAULT 0,
    source_type             VARCHAR(30),
    source_ref_id           BIGINT,
    source_ref_no           VARCHAR(100),
    warehouse_id            BIGINT,
    warehouse_name          VARCHAR(200),
    location_id             BIGINT,
    quality_status          VARCHAR(30)   DEFAULT 'NORMAL',
    quality_inspector_id    VARCHAR(64),
    quality_inspector_name  VARCHAR(100),
    quality_inspection_date TIMESTAMP,
    batch_rule_id           BIGINT,
    batch_rule_name         VARCHAR(100),
    created_by              VARCHAR(64),
    created_by_name         VARCHAR(100),
    created_at              TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by              VARCHAR(64),
    updated_by_name         VARCHAR(100),
    updated_at              TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted_at              TIMESTAMP,
    version                 INTEGER       NOT NULL DEFAULT 0,
    is_deleted              INTEGER       NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_batch_number_batch_no ON batch_number(batch_no);
CREATE INDEX IF NOT EXISTS idx_batch_number_product ON batch_number(product_id);
CREATE INDEX IF NOT EXISTS idx_batch_number_warehouse ON batch_number(warehouse_id);

-- ============================================================
-- 序列号管理
-- ============================================================

-- 序列号主表
CREATE TABLE IF NOT EXISTS serial_number (
    id                      BIGSERIAL    PRIMARY KEY,
    serial_no               VARCHAR(100) NOT NULL,
    product_id              BIGINT,
    product_code            VARCHAR(100),
    product_name            VARCHAR(200),
    specification           VARCHAR(200),
    batch_id                BIGINT,
    batch_no                VARCHAR(100),
    sn_status               VARCHAR(30)   DEFAULT 'AVAILABLE',
    sn_stage                VARCHAR(30)   DEFAULT 'WAREHOUSE',
    manufacturer            VARCHAR(200),
    manufacturing_date      DATE,
    warranty_period         INTEGER,
    warranty_start_date     DATE,
    warranty_end_date       DATE,
    current_location        VARCHAR(200),
    location_id             BIGINT,
    warehouse_id            BIGINT,
    purchase_order_id       BIGINT,
    purchase_order_no       VARCHAR(64),
    sale_order_id           BIGINT,
    sale_order_no           VARCHAR(64),
    quality_status          VARCHAR(30)   DEFAULT 'NORMAL',
    last_inspection_date    TIMESTAMP,
    next_inspection_date    TIMESTAMP,
    maintenance_count       INTEGER       DEFAULT 0,
    last_maintenance_date   TIMESTAMP,
    created_by              VARCHAR(64),
    created_by_name         VARCHAR(100),
    created_at              TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by              VARCHAR(64),
    updated_by_name         VARCHAR(100),
    updated_at              TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    deleted_at              TIMESTAMP,
    version                 INTEGER       NOT NULL DEFAULT 0,
    is_deleted              INTEGER       NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_serial_number_serial_no ON serial_number(serial_no);
CREATE INDEX IF NOT EXISTS idx_serial_number_product ON serial_number(product_id);
CREATE INDEX IF NOT EXISTS idx_serial_number_batch ON serial_number(batch_id);
CREATE INDEX IF NOT EXISTS idx_serial_number_status ON serial_number(sn_status);
