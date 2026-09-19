-- 营销模块 P0：促销引擎（服务端优惠计算）与订单优惠分摊明细
--
-- 背景：此前 `SaleOrderServiceImpl.calculateAmount` 的 promoDiscount / couponAmount 全部由**前端传参**，
--       后端不计算、不校验 —— 即「商品促销 / 整单促销 / 特价 / 优惠券」配置完不对任何单据生效。
--       本轮补服务端促销引擎：命中判定 → 优先级 → 叠加/互斥 → 条目消耗 → 行级分摊 → 服务端重算。
--
-- 一、促销活动结构化配置（复用既有 discount_rate / min_amount / reduction_amount，补 7 列）
--     · priority            优先级（唯一性由业务保证；同优先级时按 id 兜底，避免"随机选择"——SAP 明确要求避免同优先级）
--     · stack_policy        STACK 可叠加 / EXCLUSIVE 独占（命中后不再计算更低优先级活动）
--     · max_discount_amount 单条活动在本单的最大优惠（封顶）
--     · promo_price         特价单价（SPECIAL_PRICE 用；差价 = (行成交价 - 特价) × 数量）
--     · quota_total/quota_used/quota_per_customer  活动总次数 / 已用 / 每客户次数上限
ALTER TABLE erp_promotion_activity ADD COLUMN IF NOT EXISTS priority INTEGER;
ALTER TABLE erp_promotion_activity ADD COLUMN IF NOT EXISTS stack_policy VARCHAR(16);
ALTER TABLE erp_promotion_activity ADD COLUMN IF NOT EXISTS max_discount_amount NUMERIC(18, 2);
ALTER TABLE erp_promotion_activity ADD COLUMN IF NOT EXISTS promo_price NUMERIC(18, 2);
ALTER TABLE erp_promotion_activity ADD COLUMN IF NOT EXISTS quota_total INTEGER;
ALTER TABLE erp_promotion_activity ADD COLUMN IF NOT EXISTS quota_used INTEGER;
ALTER TABLE erp_promotion_activity ADD COLUMN IF NOT EXISTS quota_per_customer INTEGER;

COMMENT ON COLUMN erp_promotion_activity.priority IS '促销优先级（数值越大越先算；建议唯一）';
COMMENT ON COLUMN erp_promotion_activity.stack_policy IS '叠加策略：STACK 可叠加 / EXCLUSIVE 独占';
COMMENT ON COLUMN erp_promotion_activity.max_discount_amount IS '本单最大优惠（封顶）';
COMMENT ON COLUMN erp_promotion_activity.promo_price IS '特价单价（促销方式=特价时使用）';
COMMENT ON COLUMN erp_promotion_activity.quota_total IS '活动总次数上限（NULL=不限）';
COMMENT ON COLUMN erp_promotion_activity.quota_used IS '活动已用次数';
COMMENT ON COLUMN erp_promotion_activity.quota_per_customer IS '每客户次数上限（NULL=不限）';

-- 二、订单优惠分摊明细：一行 = 订单某个优惠（活动/券）在某个商品行上的分摊额
--     口径：产品级/特价优惠按"匹配行金额占比"分摊到行；整单级优惠按"全单行金额占比"分摊（SAP/Dynamicweb 同法）
CREATE TABLE IF NOT EXISTS erp_sale_order_promo_detail (
    id               BIGINT PRIMARY KEY,
    tenant_id        BIGINT         NOT NULL DEFAULT 0,
    order_id         BIGINT,
    order_no         VARCHAR(64),
    promotion_id     BIGINT,
    promotion_name   VARCHAR(255),
    promo_mode       VARCHAR(64),
    scope_type       VARCHAR(16),
    line_no          INTEGER,
    product_id       BIGINT,
    discount_amount  NUMERIC(18, 2),
    coupon_id        BIGINT,
    coupon_code      VARCHAR(64),
    gift_product_id  BIGINT,
    gift_quantity    NUMERIC(18, 2),
    remark           VARCHAR(255),
    deleted          INTEGER        NOT NULL DEFAULT 0,
    create_time      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sale_order_promo_detail_order
    ON erp_sale_order_promo_detail (order_id);

COMMENT ON TABLE erp_sale_order_promo_detail IS '订单优惠分摊明细（促销引擎产出，一优惠 × 一商品行 = 一行）';
COMMENT ON COLUMN erp_sale_order_promo_detail.scope_type IS 'ORDER 整单级 / ITEM 商品行级';
COMMENT ON COLUMN erp_sale_order_promo_detail.discount_amount IS '该优惠在该行分摊到的优惠额';
COMMENT ON COLUMN erp_sale_order_promo_detail.gift_quantity IS '赠品数量（满赠类，不计金额优惠）';
