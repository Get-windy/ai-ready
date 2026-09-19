-- 营销模块 → 积分兑换（80301）+ 营销推广（80330 我要推广 / 80331 推广历史查询）
-- 口径：
--   · mkt_points_exchange_product：可兑换商品目录（商品 × 兑换所需积分）。商品名称/货号/单位/规格/型号/产地
--     与 6 个价格列一律**实时取自商品主数据**（erp_product），本表只存「兑换所需积分」这一营销域事实，不冗余商品字段。
--   · mkt_share_record：推广分享触达台账。同时服务「我要推广」6 个 Tab 的分享统计列（最近分享时间/分享次数/
--     浏览人数/浏览次数/分享领取数）与「推广历史查询」整页（分享时间/分享人/分享类型/分享概要/浏览与下单 5 个指标）。
CREATE TABLE IF NOT EXISTS mkt_points_exchange_product (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    product_id      BIGINT NOT NULL,
    exchange_points NUMERIC(18, 2),
    sort            INTEGER,
    status          INTEGER NOT NULL DEFAULT 1,
    remark          VARCHAR(255),
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mkt_points_exchange_product
    ON mkt_points_exchange_product (tenant_id, product_id);

COMMENT ON TABLE mkt_points_exchange_product IS '可兑换商品目录（营销→会员中心→积分兑换）';
COMMENT ON COLUMN mkt_points_exchange_product.exchange_points IS '兑换所需积分（该商品的积分定价）';

CREATE TABLE IF NOT EXISTS mkt_share_record (
    id               BIGINT PRIMARY KEY,
    tenant_id        BIGINT NOT NULL DEFAULT 0,
    share_type       VARCHAR(32),
    target_id        BIGINT,
    target_name      VARCHAR(255),
    share_summary    VARCHAR(255),
    sharer_id        BIGINT,
    sharer_name      VARCHAR(64),
    share_time       TIMESTAMP,
    view_count       INTEGER,
    viewer_count     INTEGER,
    receive_count    INTEGER,
    order_user_count INTEGER,
    order_count      INTEGER,
    order_amount     NUMERIC(18, 2),
    deleted          INTEGER NOT NULL DEFAULT 0,
    create_time      TIMESTAMP,
    update_time      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mkt_share_record_target
    ON mkt_share_record (tenant_id, share_type, target_id);

COMMENT ON TABLE mkt_share_record IS '推广分享触达台账（我要推广 / 推广历史查询）';
COMMENT ON COLUMN mkt_share_record.share_type IS '分享类型：PRODUCT 商品 / COUPON 优惠券 / PROMOTION 促销 / GROUP_BUY 拼团 / FLASH_SALE 秒杀';
COMMENT ON COLUMN mkt_share_record.receive_count IS '分享领取数';
COMMENT ON COLUMN mkt_share_record.view_count IS '浏览次数';
COMMENT ON COLUMN mkt_share_record.viewer_count IS '浏览人数';
