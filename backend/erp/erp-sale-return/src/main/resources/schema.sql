-- Sale Return module schema init
-- Runs at startup when spring.sql.init.mode=always

CREATE TABLE IF NOT EXISTS erp_sale_return (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT DEFAULT 0,
    return_no       VARCHAR(64) DEFAULT NULL,
    sale_order_id   BIGINT DEFAULT NULL,
    sale_order_no   VARCHAR(64) DEFAULT NULL,
    customer_id     BIGINT DEFAULT NULL,
    customer_name   VARCHAR(200) DEFAULT NULL,
    return_type     INTEGER DEFAULT 0,
    total_quantity  DECIMAL(20,2) DEFAULT 0,
    total_amount    DECIMAL(20,2) DEFAULT 0,
    status          INTEGER DEFAULT 0,
    applicant_id    BIGINT DEFAULT NULL,
    applicant_name  VARCHAR(100) DEFAULT NULL,
    apply_time      TIMESTAMP DEFAULT NULL,
    approved_by     BIGINT DEFAULT NULL,
    approved_time   TIMESTAMP DEFAULT NULL,
    approved_note   VARCHAR(500) DEFAULT NULL,
    reason          VARCHAR(500) DEFAULT NULL,
    remark          VARCHAR(500) DEFAULT NULL,
    deleted         INTEGER DEFAULT 0,
    create_by       BIGINT DEFAULT NULL,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT DEFAULT NULL,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version         INTEGER DEFAULT 0
);

COMMENT ON TABLE erp_sale_return IS '销售退货单';
