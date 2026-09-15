-- 零售单表
CREATE TABLE IF NOT EXISTS erp_retail_order (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL DEFAULT 1,
    retail_no           VARCHAR(100)    NOT NULL,               -- 零售单号
    customer_id         BIGINT,                                 -- 客户ID（关联 biz_party）
    customer_name       VARCHAR(200),                           -- 客户名称
    amount              NUMERIC(18,2)   NOT NULL DEFAULT 0,     -- 零售金额
    payment_method      VARCHAR(20),                            -- 支付方式（CASH/ALIPAY/WECHAT/CARD）
    status              INTEGER         NOT NULL DEFAULT 0,     -- 状态：0=待处理 1=已完成 2=已取消
    remark              VARCHAR(500),                           -- 备注
    deleted             INTEGER         NOT NULL DEFAULT 0,
    create_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT
);

COMMENT ON TABLE erp_retail_order IS '零售单';
COMMENT ON COLUMN erp_retail_order.retail_no IS '零售单号';
COMMENT ON COLUMN erp_retail_order.customer_id IS '客户ID';
COMMENT ON COLUMN erp_retail_order.customer_name IS '客户名称';
COMMENT ON COLUMN erp_retail_order.amount IS '零售金额';
COMMENT ON COLUMN erp_retail_order.payment_method IS '支付方式：CASH=现金 ALIPAY=支付宝 WECHAT=微信 CARD=银行卡';
COMMENT ON COLUMN erp_retail_order.status IS '状态：0=待处理 1=已完成 2=已取消';

CREATE INDEX IF NOT EXISTS idx_retail_order_no ON erp_retail_order(retail_no);
CREATE INDEX IF NOT EXISTS idx_retail_order_customer ON erp_retail_order(customer_id);
CREATE INDEX IF NOT EXISTS idx_retail_order_create_time ON erp_retail_order(create_time);
