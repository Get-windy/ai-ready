-- 营销模块 → 营销活动 → 优惠券（菜单 80311）
-- 对标 ql361 双视图 Tab：`优惠券设置`（制券，15 列）/ `领用明细`（领用核销流水，13 列）
-- 口径：
--   · 制券视图 = mkt_coupon_template；「未领取」不落列，由 总数 - 已领取（未使用） - 已使用 实时推导
--   · 领用明细 = 复用既有 erp_loyalty_coupon（券实例），本迁移仅补 3 列：
--     template_id（归属券模板）/ receive_time（领取时间）/ source_bill_no（来源单据）
--   · 「领用状态」读 erp_loyalty_coupon.status（UNUSED/USED/EXPIRED/CANCELLED → 已领取/已使用/已过期/已作废）
--     「状态」读券模板状态（NORMAL 正常 / VOID 已作废）—— 两列语义不同，不合并
CREATE TABLE IF NOT EXISTS mkt_coupon_template (
    id               BIGINT PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL DEFAULT 0,
    coupon_name      VARCHAR(128) NOT NULL,
    open_receive     INTEGER,
    coupon_type      VARCHAR(32),
    use_rule         VARCHAR(64),
    face_value       NUMERIC(18, 2),
    total_count      INTEGER,
    received_count   INTEGER,
    used_count       INTEGER,
    customer_scope   VARCHAR(32),
    start_time       TIMESTAMP,
    end_time         TIMESTAMP,
    status           VARCHAR(32),
    mall_enabled     INTEGER,
    offline_enabled  INTEGER,
    remark           VARCHAR(255),
    deleted          INTEGER      NOT NULL DEFAULT 0,
    create_by        BIGINT,
    create_time      TIMESTAMP,
    update_by        BIGINT,
    update_time      TIMESTAMP
);

COMMENT ON TABLE mkt_coupon_template IS '优惠券模板（营销→营销活动→优惠券→优惠券设置 Tab）';
COMMENT ON COLUMN mkt_coupon_template.open_receive IS '开放领取：1 是（客户商城自助领取）/ 0 否（后台定向发放）';
COMMENT ON COLUMN mkt_coupon_template.coupon_type IS '券类型：CASH 现金券 / DISCOUNT 折扣券 / FULL_CUT 满减券';
COMMENT ON COLUMN mkt_coupon_template.use_rule IS '使用规则：UNLIMITED 无限制 / 满 N 元可用';
COMMENT ON COLUMN mkt_coupon_template.received_count IS '已领取（未使用）';
COMMENT ON COLUMN mkt_coupon_template.used_count IS '已使用';
COMMENT ON COLUMN mkt_coupon_template.customer_scope IS '指定客户：ALL 全部客户 / SPECIFIED 指定客户';
COMMENT ON COLUMN mkt_coupon_template.status IS '状态：NORMAL 正常 / VOID 已作废';
COMMENT ON COLUMN mkt_coupon_template.mall_enabled IS '商城使用：1 允许 / 0 禁止';
COMMENT ON COLUMN mkt_coupon_template.offline_enabled IS '线下使用：1 允许 / 0 禁止';

-- 指定客户（customer_scope = SPECIFIED 时生效）
CREATE TABLE IF NOT EXISTS mkt_coupon_customer (
    id           BIGINT PRIMARY KEY,
    tenant_id    BIGINT NOT NULL DEFAULT 0,
    template_id  BIGINT NOT NULL,
    partner_id   BIGINT NOT NULL,
    partner_name VARCHAR(255),
    deleted      INTEGER NOT NULL DEFAULT 0,
    create_time  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mkt_coupon_customer_tpl ON mkt_coupon_customer (template_id);

COMMENT ON TABLE mkt_coupon_customer IS '优惠券指定客户（优惠券模板 × 往来单位）';

-- 领用明细：三个补列（券实例归属模板 / 领取时间 / 来源单据）
ALTER TABLE erp_loyalty_coupon ADD COLUMN IF NOT EXISTS template_id BIGINT;
ALTER TABLE erp_loyalty_coupon ADD COLUMN IF NOT EXISTS receive_time TIMESTAMP;
ALTER TABLE erp_loyalty_coupon ADD COLUMN IF NOT EXISTS source_bill_no VARCHAR(64);

COMMENT ON COLUMN erp_loyalty_coupon.template_id IS '归属券模板（mkt_coupon_template.id）';
COMMENT ON COLUMN erp_loyalty_coupon.receive_time IS '领取时间';
COMMENT ON COLUMN erp_loyalty_coupon.source_bill_no IS '来源单据号';

CREATE INDEX IF NOT EXISTS idx_erp_loyalty_coupon_template ON erp_loyalty_coupon (template_id);
