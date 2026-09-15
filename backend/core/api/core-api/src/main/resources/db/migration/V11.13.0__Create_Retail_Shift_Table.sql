-- ============================================================
-- V11.13.0: 创建零售交班（班次）表 erp_retail_shift
--
-- 背景：
--   POS 收银交班管理缺失。交班时按班次时段汇总已结算零售单，
--   得出应收现金（期初+现金销售-现金退款）与长短款。
--   status: 1=营业中 2=已交班
-- ============================================================

CREATE TABLE IF NOT EXISTS erp_retail_shift (
    id              BIGINT          PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL DEFAULT 1,

    -- 班次信息
    shift_no        VARCHAR(50)     NOT NULL,
    cashier_id      BIGINT          NOT NULL,
    cashier_name    VARCHAR(100),
    warehouse_id    BIGINT,

    -- 开班
    open_time       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    opening_cash    NUMERIC(18,2)   NOT NULL DEFAULT 0,

    -- 交班
    close_time      TIMESTAMP,
    closing_cash    NUMERIC(18,2),
    expected_cash   NUMERIC(18,2),
    difference      NUMERIC(18,2),

    -- 汇总（交班时自动统计该班次时段内已结算零售单）
    order_count     INTEGER         DEFAULT 0,
    total_amount    NUMERIC(18,2)   DEFAULT 0,
    cash_amount     NUMERIC(18,2)   DEFAULT 0,
    qr_amount       NUMERIC(18,2)   DEFAULT 0,
    other_amount    NUMERIC(18,2)   DEFAULT 0,

    -- 状态：1=营业中 2=已交班
    status          INTEGER         NOT NULL DEFAULT 1,
    remark          VARCHAR(500),

    -- 系统字段
    deleted         INTEGER         NOT NULL DEFAULT 0,
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by       BIGINT,
    update_by       BIGINT,
    version_no      INTEGER         DEFAULT 0
);

COMMENT ON TABLE erp_retail_shift IS '零售收银交班（班次）表';
COMMENT ON COLUMN erp_retail_shift.shift_no IS '班次号（BC+yyyyMMdd+随机）';
COMMENT ON COLUMN erp_retail_shift.opening_cash IS '期初现金（开班备用金）';
COMMENT ON COLUMN erp_retail_shift.closing_cash IS '实点现金（交班时录入）';
COMMENT ON COLUMN erp_retail_shift.expected_cash IS '应收现金=期初现金+现金销售-现金退款';
COMMENT ON COLUMN erp_retail_shift.difference IS '长短款=实点现金-应收现金';
COMMENT ON COLUMN erp_retail_shift.order_count IS '班次内已结算零售单数';
COMMENT ON COLUMN erp_retail_shift.total_amount IS '班次内实收总额';
COMMENT ON COLUMN erp_retail_shift.cash_amount IS '现金支付合计';
COMMENT ON COLUMN erp_retail_shift.qr_amount IS '扫码支付合计（支付宝+微信+聚合）';
COMMENT ON COLUMN erp_retail_shift.other_amount IS '其他支付合计（银行卡/预收/转账等）';
COMMENT ON COLUMN erp_retail_shift.status IS '状态：1=营业中 2=已交班';

CREATE UNIQUE INDEX IF NOT EXISTS uk_retail_shift_no ON erp_retail_shift(shift_no);
CREATE INDEX IF NOT EXISTS idx_retail_shift_cashier ON erp_retail_shift(cashier_id, status);
CREATE INDEX IF NOT EXISTS idx_retail_shift_open_time ON erp_retail_shift(open_time);
