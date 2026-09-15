-- 支付结算中心表
-- V8.10.0__Payment_Center_Tables.sql

-- 支付请求表
CREATE TABLE IF NOT EXISTS payment_request (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    biz_type VARCHAR(50) NOT NULL,
    biz_id BIGINT NOT NULL,
    biz_no VARCHAR(50),
    amount DECIMAL(12,2) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    status INT DEFAULT 0,
    channel_order_no VARCHAR(100),
    channel_trade_no VARCHAR(100),
    expire_time TIMESTAMP,
    paid_time TIMESTAMP,
    remark VARCHAR(500),
    payer_id BIGINT,
    payer_name VARCHAR(100),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

COMMENT ON TABLE payment_request IS '支付请求表';
COMMENT ON COLUMN payment_request.biz_type IS '业务类型: SALE_ORDER, TRADE_ORDER, DMS_DELIVERY';
COMMENT ON COLUMN payment_request.channel IS '支付渠道: ALIPAY, WECHAT, UNIONPAY, BANK, CASH';
COMMENT ON COLUMN payment_request.status IS '状态: 0待支付, 1支付中, 2已支付, 3已取消, 4已失败';

-- 支付记录表
CREATE TABLE IF NOT EXISTS payment_record (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    request_id BIGINT,
    channel VARCHAR(20) NOT NULL,
    channel_order_no VARCHAR(100),
    channel_trade_no VARCHAR(100),
    amount DECIMAL(12,2),
    status INT DEFAULT 0,
    callback_time TIMESTAMP,
    callback_data TEXT,
    error_code VARCHAR(50),
    error_msg VARCHAR(200),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

COMMENT ON TABLE payment_record IS '支付记录表(第三方返回)';
COMMENT ON COLUMN payment_record.status IS '状态: 0待支付, 1支付中, 2成功, 3失败';

-- 退款请求表
CREATE TABLE IF NOT EXISTS refund_request (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    reason VARCHAR(200),
    status INT DEFAULT 0,
    channel_refund_no VARCHAR(100),
    refunded_time TIMESTAMP,
    applicant_id BIGINT,
    applicant_name VARCHAR(100),
    approver_id BIGINT,
    approve_remark VARCHAR(200),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

COMMENT ON TABLE refund_request IS '退款请求表';
COMMENT ON COLUMN refund_request.status IS '状态: 0待处理, 1处理中, 2已退款, 3已拒绝';

-- 退款记录表
CREATE TABLE IF NOT EXISTS refund_record (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    request_id BIGINT,
    channel VARCHAR(20),
    channel_refund_no VARCHAR(100),
    amount DECIMAL(12,2),
    status INT DEFAULT 0,
    callback_time TIMESTAMP,
    callback_data TEXT,
    error_code VARCHAR(50),
    error_msg VARCHAR(200),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

COMMENT ON TABLE refund_record IS '退款记录表';
COMMENT ON COLUMN refund_record.status IS '状态: 0处理中, 1成功, 2失败';

-- 日对账表
CREATE TABLE IF NOT EXISTS payment_reconciliation (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    reconcile_date DATE NOT NULL,
    channel VARCHAR(20) NOT NULL,
    total_count INT DEFAULT 0,
    total_amount DECIMAL(14,2) DEFAULT 0,
    success_count INT DEFAULT 0,
    success_amount DECIMAL(14,2) DEFAULT 0,
    diff_count INT DEFAULT 0,
    diff_amount DECIMAL(14,2) DEFAULT 0,
    status INT DEFAULT 0,
    reconciled_time DATE,
    remark VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

COMMENT ON TABLE payment_reconciliation IS '日对账表';
COMMENT ON COLUMN payment_reconciliation.status IS '状态: 0待对账, 1已对账, 2有差异';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_payment_request_biz ON payment_request(biz_type, biz_id);
CREATE INDEX IF NOT EXISTS idx_payment_request_channel ON payment_request(channel);
CREATE INDEX IF NOT EXISTS idx_payment_request_status ON payment_request(status);
CREATE INDEX IF NOT EXISTS idx_payment_record_request ON payment_record(request_id);
CREATE INDEX IF NOT EXISTS idx_refund_request_payment ON refund_request(payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_recon_date ON payment_reconciliation(reconcile_date);

-- 菜单数据
INSERT INTO sys_menu (id, parent_id, menu_name, path, menu_type, component, icon, sort, visible)
VALUES
(700, 0, '支付结算', 'payment', 0, 'Layout', 'transaction', 1, 1),
(701, 700, '支付请求', 'payment-request', 1, 'views/payment/request/list', NULL, 1, 1),
(702, 700, '支付记录', 'payment-record', 1, 'views/payment/record/list', NULL, 2, 1),
(703, 700, '退款管理', 'payment-refund', 1, 'views/payment/refund/list', NULL, 3, 1),
(704, 700, '支付对账', 'payment-reconciliation', 1, 'views/payment/reconciliation/daily', NULL, 4, 1)
ON CONFLICT (id) DO NOTHING;