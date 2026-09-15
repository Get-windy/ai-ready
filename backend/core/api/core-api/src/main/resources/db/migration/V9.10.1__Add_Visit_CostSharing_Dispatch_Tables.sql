-- V9.10.0 新增 CRM拜访、采购费用分摊/以销定购、DMS收发货查询 数据表

-- ============================================================
-- CRM 拜访管理
-- ============================================================

-- 拜访规划表
CREATE TABLE IF NOT EXISTS crm_visit_plan (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT,
    plan_no         VARCHAR(64)  NOT NULL,
    salesman_id     BIGINT,
    salesman_name   VARCHAR(100),
    customer_id     BIGINT,
    customer_name   VARCHAR(200),
    plan_date       DATE,
    purpose         VARCHAR(200),
    content         TEXT,
    status          INTEGER      NOT NULL DEFAULT 0,
    remark          VARCHAR(500),
    deleted         INTEGER      NOT NULL DEFAULT 0,
    version         INTEGER      NOT NULL DEFAULT 0,
    create_by       VARCHAR(64),
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64),
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_crm_visit_plan_no ON crm_visit_plan(plan_no);

-- 拜访执行表
CREATE TABLE IF NOT EXISTS crm_visit_execution (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT,
    exec_no         VARCHAR(64)  NOT NULL,
    plan_id         BIGINT,
    salesman_id     BIGINT,
    salesman_name   VARCHAR(100),
    customer_id     BIGINT,
    customer_name   VARCHAR(200),
    visit_date      DATE,
    visit_result    VARCHAR(500),
    follow_up       VARCHAR(500),
    status          INTEGER      NOT NULL DEFAULT 0,
    remark          VARCHAR(500),
    deleted         INTEGER      NOT NULL DEFAULT 0,
    version         INTEGER      NOT NULL DEFAULT 0,
    create_by       VARCHAR(64),
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64),
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_crm_visit_exec_no ON crm_visit_execution(exec_no);

-- 拜访检视表
CREATE TABLE IF NOT EXISTS crm_visit_review (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT,
    review_no       VARCHAR(64)  NOT NULL,
    execution_id    BIGINT,
    salesman_id     BIGINT,
    salesman_name   VARCHAR(100),
    customer_id     BIGINT,
    customer_name   VARCHAR(200),
    review_date     DATE,
    review_comment  TEXT,
    score           INTEGER,
    status          INTEGER      NOT NULL DEFAULT 0,
    remark          VARCHAR(500),
    deleted         INTEGER      NOT NULL DEFAULT 0,
    version         INTEGER      NOT NULL DEFAULT 0,
    create_by       VARCHAR(64),
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64),
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_crm_visit_review_no ON crm_visit_review(review_no);

-- ============================================================
-- ERP 采购扩展
-- ============================================================

-- 采购费用分摊表
CREATE TABLE IF NOT EXISTS erp_purchase_cost_sharing (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT,
    sharing_no      VARCHAR(64)  NOT NULL,
    supplier_id     BIGINT,
    supplier_name   VARCHAR(200),
    order_id        BIGINT,
    order_no        VARCHAR(64),
    expense_type    VARCHAR(100),
    amount          DECIMAL(18,2) NOT NULL DEFAULT 0,
    sharing_method  INTEGER      DEFAULT 1,
    status          INTEGER      NOT NULL DEFAULT 0,
    remark          VARCHAR(500),
    deleted         INTEGER      NOT NULL DEFAULT 0,
    version         INTEGER      NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_erp_purchase_cost_sharing_no ON erp_purchase_cost_sharing(sharing_no);

-- 以销定购表
CREATE TABLE IF NOT EXISTS erp_purchase_sales_driven (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT,
    product_id      BIGINT,
    product_code    VARCHAR(100),
    product_name    VARCHAR(200),
    specification   VARCHAR(200),
    unit            VARCHAR(50),
    sales_qty       DECIMAL(18,4) NOT NULL DEFAULT 0,
    current_stock   DECIMAL(18,4) NOT NULL DEFAULT 0,
    suggest_qty     DECIMAL(18,4) NOT NULL DEFAULT 0,
    last_date       DATE,
    status          INTEGER      NOT NULL DEFAULT 0,
    remark          VARCHAR(500),
    deleted         INTEGER      NOT NULL DEFAULT 0,
    version         INTEGER      NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- DMS 收发货查询
-- ============================================================

-- 发货单表
CREATE TABLE IF NOT EXISTS dms_ship_order (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT,
    ship_no         VARCHAR(64)  NOT NULL,
    customer_id     BIGINT,
    customer_name   VARCHAR(200),
    amount          DECIMAL(18,2) NOT NULL DEFAULT 0,
    logistics_company VARCHAR(200),
    logistics_no    VARCHAR(100),
    status          INTEGER      NOT NULL DEFAULT 0,
    remark          VARCHAR(500),
    deleted         INTEGER      NOT NULL DEFAULT 0,
    version         INTEGER      NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_dms_ship_no ON dms_ship_order(ship_no);

-- 物流退货收货表
CREATE TABLE IF NOT EXISTS dms_return_receive (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT,
    return_no       VARCHAR(64)  NOT NULL,
    customer_id     BIGINT,
    customer_name   VARCHAR(200),
    reason          VARCHAR(500),
    amount          DECIMAL(18,2) NOT NULL DEFAULT 0,
    status          INTEGER      NOT NULL DEFAULT 0,
    remark          VARCHAR(500),
    deleted         INTEGER      NOT NULL DEFAULT 0,
    version         INTEGER      NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_dms_return_no ON dms_return_receive(return_no);

-- 配送记录表
CREATE TABLE IF NOT EXISTS dms_dispatch_record (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT,
    dispatch_no     VARCHAR(64)  NOT NULL,
    route_name      VARCHAR(200),
    driver_id       BIGINT,
    driver_name     VARCHAR(100),
    customer_count  INTEGER       NOT NULL DEFAULT 0,
    amount          DECIMAL(18,2) NOT NULL DEFAULT 0,
    status          INTEGER      NOT NULL DEFAULT 0,
    remark          VARCHAR(500),
    deleted         INTEGER      NOT NULL DEFAULT 0,
    version         INTEGER      NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_dms_dispatch_no ON dms_dispatch_record(dispatch_no);

-- 采购订货收货表
CREATE TABLE IF NOT EXISTS dms_purchase_receive (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT,
    receive_no      VARCHAR(64)  NOT NULL,
    supplier_id     BIGINT,
    supplier_name   VARCHAR(200),
    amount          DECIMAL(18,2) NOT NULL DEFAULT 0,
    warehouse_id    BIGINT,
    warehouse_name  VARCHAR(200),
    status          INTEGER      NOT NULL DEFAULT 0,
    remark          VARCHAR(500),
    deleted         INTEGER      NOT NULL DEFAULT 0,
    version         INTEGER      NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_dms_receive_no ON dms_purchase_receive(receive_no);

-- 物流发货表
CREATE TABLE IF NOT EXISTS dms_logistics_ship (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT,
    ship_no         VARCHAR(64)  NOT NULL,
    logistics_company VARCHAR(200),
    logistics_no    VARCHAR(100),
    receiver_name   VARCHAR(100),
    amount          DECIMAL(18,2) NOT NULL DEFAULT 0,
    status          INTEGER      NOT NULL DEFAULT 0,
    remark          VARCHAR(500),
    deleted         INTEGER      NOT NULL DEFAULT 0,
    version         INTEGER      NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_dms_logistics_ship_no ON dms_logistics_ship(ship_no);
