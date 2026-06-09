-- Sale Exchange module schema init
-- Runs at startup when spring.sql.init.mode=always

CREATE TABLE IF NOT EXISTS erp_sale_exchange (
    id                 BIGSERIAL PRIMARY KEY,
    tenant_id          BIGINT DEFAULT 0,
    exchange_no        VARCHAR(64) DEFAULT NULL,
    original_order_id  BIGINT DEFAULT NULL,
    original_order_no  VARCHAR(64) DEFAULT NULL,
    customer_id        BIGINT DEFAULT NULL,
    customer_name      VARCHAR(200) DEFAULT NULL,
    exchange_date      TIMESTAMP DEFAULT NULL,
    exchange_reason    VARCHAR(500) DEFAULT NULL,
    exchange_type      INTEGER DEFAULT 0,
    status             INTEGER DEFAULT 0,
    remark             VARCHAR(500) DEFAULT NULL,
    total_amount       DECIMAL(20,2) DEFAULT 0,
    created_by         BIGINT DEFAULT NULL,
    created_by_name    VARCHAR(100) DEFAULT NULL,
    approved_by        BIGINT DEFAULT NULL,
    approved_by_name   VARCHAR(100) DEFAULT NULL,
    approved_time      TIMESTAMP DEFAULT NULL,
    completed_time     TIMESTAMP DEFAULT NULL,
    deleted            INTEGER DEFAULT 0,
    create_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version            INTEGER DEFAULT 0
);

COMMENT ON TABLE erp_sale_exchange IS '销售换货单';
