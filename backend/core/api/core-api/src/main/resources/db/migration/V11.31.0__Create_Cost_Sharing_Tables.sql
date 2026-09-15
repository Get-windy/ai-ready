-- ============================================================
-- V11.31.0: 创建采购费用分摊表
-- 幂等：IF NOT EXISTS 守卫
-- ============================================================

CREATE TABLE IF NOT EXISTS erp_cost_sharing (
    id                BIGINT          PRIMARY KEY,
    tenant_id         BIGINT          NOT NULL DEFAULT 1,
    sharing_no        VARCHAR(64)     NOT NULL,
    sharing_date      DATE,
    allocation_method VARCHAR(32),
    expense_type      VARCHAR(64),
    supplier_id       BIGINT,
    supplier_name     VARCHAR(255),
    status            INTEGER         NOT NULL DEFAULT 0,
    total_amount      NUMERIC(18,2)   DEFAULT 0,
    remark            VARCHAR(500),
    deleted           INTEGER         NOT NULL DEFAULT 0,
    create_time       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by         BIGINT,
    update_by         BIGINT
);

CREATE INDEX IF NOT EXISTS idx_cost_sharing_no ON erp_cost_sharing(sharing_no);
CREATE INDEX IF NOT EXISTS idx_cost_sharing_status ON erp_cost_sharing(status);

CREATE TABLE IF NOT EXISTS erp_cost_sharing_item (
    id                BIGINT          PRIMARY KEY,
    cost_sharing_id   BIGINT          NOT NULL,
    inbound_order_id  BIGINT,
    product_id        BIGINT,
    product_name      VARCHAR(255),
    quantity          NUMERIC(18,4)   DEFAULT 0,
    amount            NUMERIC(18,2)   DEFAULT 0,
    weight            NUMERIC(18,4),
    volume            NUMERIC(18,4),
    allocated_cost    NUMERIC(18,2)   DEFAULT 0,
    deleted           INTEGER         NOT NULL DEFAULT 0,
    create_time       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by         BIGINT,
    update_by         BIGINT
);

CREATE INDEX IF NOT EXISTS idx_cost_sharing_item_parent ON erp_cost_sharing_item(cost_sharing_id);
CREATE INDEX IF NOT EXISTS idx_cost_sharing_item_inbound ON erp_cost_sharing_item(inbound_order_id);
