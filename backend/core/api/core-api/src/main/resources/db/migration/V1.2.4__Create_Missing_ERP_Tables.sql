-- V1.2.4: Create missing ERP tables that were never defined in any migration
-- These tables exist in MyBatis Plus / JPA entities but were missing from DDL.
-- V1.2.1 and V1.2.2 attempted ALTER TABLE on fin_payable but the base table was never created.

-- ============================================================================
-- fin_payable: core-api finance module (MyBatis Plus entity)
-- ============================================================================
CREATE TABLE IF NOT EXISTS fin_payable (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT DEFAULT NULL,
    payable_no      VARCHAR(100) DEFAULT NULL,
    supplier_id     BIGINT DEFAULT NULL,
    supplier_name   VARCHAR(500) DEFAULT NULL,
    contract_id     BIGINT DEFAULT NULL,
    contract_no     VARCHAR(200) DEFAULT NULL,
    original_amount DECIMAL(20,2) DEFAULT 0,
    paid_amount     DECIMAL(20,2) DEFAULT 0,
    remaining_amount DECIMAL(20,2) DEFAULT 0,
    bill_date       DATE DEFAULT NULL,
    due_date        DATE DEFAULT NULL,
    status          INTEGER DEFAULT 0,
    overdue_days    INTEGER DEFAULT 0,
    remark          VARCHAR(500) DEFAULT NULL,
    create_by       VARCHAR(50) DEFAULT NULL,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(50) DEFAULT NULL,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER DEFAULT 0
);

COMMENT ON TABLE fin_payable IS '应付账款';
COMMENT ON COLUMN fin_payable.status IS '0-未付款 1-部分付款 2-已付款 3-已核销';
COMMENT ON COLUMN fin_payable.deleted IS '逻辑删除 0-正常 1-删除';

-- ============================================================================
-- erp_metric_definition: erp-monitor module (MyBatis Plus entity)
-- ============================================================================
CREATE TABLE IF NOT EXISTS erp_metric_definition (
    id                 BIGSERIAL PRIMARY KEY,
    tenant_id          BIGINT DEFAULT 0,
    metric_code        VARCHAR(100) DEFAULT NULL,
    metric_name        VARCHAR(200) NOT NULL,
    metric_type        VARCHAR(50) DEFAULT NULL,
    description        VARCHAR(500) DEFAULT NULL,
    unit               VARCHAR(50) DEFAULT NULL,
    data_type          VARCHAR(50) DEFAULT NULL,
    calc_method        VARCHAR(50) DEFAULT NULL,
    calc_formula       TEXT DEFAULT NULL,
    source_table       VARCHAR(200) DEFAULT NULL,
    source_field       VARCHAR(200) DEFAULT NULL,
    filter_condition   VARCHAR(500) DEFAULT NULL,
    group_by_fields    VARCHAR(500) DEFAULT NULL,
    warning_threshold  DECIMAL(20,4) DEFAULT NULL,
    critical_threshold DECIMAL(20,4) DEFAULT NULL,
    target_value       DECIMAL(20,4) DEFAULT NULL,
    enabled            INTEGER DEFAULT 1,
    sort_order         INTEGER DEFAULT 0,
    deleted            INTEGER DEFAULT 0,
    create_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by          BIGINT DEFAULT NULL,
    update_by          BIGINT DEFAULT NULL
);

COMMENT ON TABLE erp_metric_definition IS '业务指标定义';
COMMENT ON COLUMN erp_metric_definition.metric_type IS '指标类型: order-订单, inventory-库存, user-用户, sales-销售, purchase-采购, finance-财务, system-系统';
COMMENT ON COLUMN erp_metric_definition.data_type IS '数据类型: integer-整数, decimal-小数, percent-百分比, amount-金额, count-数量';
COMMENT ON COLUMN erp_metric_definition.calc_method IS '计算方式: sum-求和, count-计数, avg-平均, max-最大值, min-最小值, custom-自定义';
