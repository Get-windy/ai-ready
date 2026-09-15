-- ============================================================
-- V11.30.0: 创建商城营销三页缺失表（预售/弹窗广告/加价购）
--
-- 背景：商城预售/弹窗广告/加价购三页此前为降级态（a-alert 只读展示），
-- DB 无对应表。V11.9-11.11 仅建了 mkt_flash_sale/mall_notice/mall_keyword。
-- 本批补齐三组主表，B2B商城+营销模块打通。
--
-- 幂等：IF NOT EXISTS + DO $$ ... END $$ 守卫。
-- ============================================================

-- ═══════════════════════════════════════════════════════════════
-- 1. 预售活动表 mkt_presale
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS mkt_presale (
    id                  BIGINT          PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL DEFAULT 1,

    -- 活动基本信息
    activity_name       VARCHAR(200)    NOT NULL,
    product_id          BIGINT          NOT NULL,
    product_name        VARCHAR(200),
    product_code        VARCHAR(100),

    -- 金额
    deposit_amount      NUMERIC(18,2)   NOT NULL DEFAULT 0,
    final_amount        NUMERIC(18,2)   NOT NULL DEFAULT 0,

    -- 时间
    start_time          TIMESTAMP       NOT NULL,
    end_time            TIMESTAMP       NOT NULL,
    deposit_end_time    TIMESTAMP,
    final_start_time    TIMESTAMP,

    -- 库存控制
    stock_limit         INTEGER         DEFAULT 0,
    sold_count          INTEGER         DEFAULT 0,

    -- 状态：0=未开始 1=进行中 2=已结束 3=已取消
    status              INTEGER         NOT NULL DEFAULT 0,

    sort                INTEGER         DEFAULT 0,
    remark              VARCHAR(500),

    -- 系统字段
    deleted             INTEGER         NOT NULL DEFAULT 0,
    create_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT
);

COMMENT ON TABLE mkt_presale IS '预售活动表';
COMMENT ON COLUMN mkt_presale.activity_name IS '活动名称';
COMMENT ON COLUMN mkt_presale.product_id IS '预售商品ID';
COMMENT ON COLUMN mkt_presale.deposit_amount IS '定金金额';
COMMENT ON COLUMN mkt_presale.final_amount IS '尾款金额';
COMMENT ON COLUMN mkt_presale.start_time IS '活动开始时间';
COMMENT ON COLUMN mkt_presale.end_time IS '活动结束时间';
COMMENT ON COLUMN mkt_presale.deposit_end_time IS '定金支付截止时间';
COMMENT ON COLUMN mkt_presale.final_start_time IS '尾款支付开始时间';
COMMENT ON COLUMN mkt_presale.stock_limit IS '库存限制（0=不限）';
COMMENT ON COLUMN mkt_presale.sold_count IS '已售数量';
COMMENT ON COLUMN mkt_presale.status IS '状态：0=未开始 1=进行中 2=已结束 3=已取消';

CREATE INDEX IF NOT EXISTS idx_mkt_presale_product ON mkt_presale(product_id);
CREATE INDEX IF NOT EXISTS idx_mkt_presale_time ON mkt_presale(start_time, end_time);

-- ═══════════════════════════════════════════════════════════════
-- 2. 预售订单关联表 mkt_presale_order
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS mkt_presale_order (
    id                  BIGINT          PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL DEFAULT 1,
    presale_id          BIGINT          NOT NULL,
    mall_order_id       BIGINT,
    customer_id         BIGINT,

    -- 支付
    paid_deposit        INTEGER         DEFAULT 0,
    paid_final          INTEGER         DEFAULT 0,

    -- 状态：0=已付定金 1=已付尾款 2=已取消
    status              INTEGER         DEFAULT 0,

    -- 系统字段
    deleted             INTEGER         NOT NULL DEFAULT 0,
    create_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT
);

COMMENT ON TABLE mkt_presale_order IS '预售订单关联表';
COMMENT ON COLUMN mkt_presale_order.presale_id IS '关联预售活动ID → mkt_presale.id';
COMMENT ON COLUMN mkt_presale_order.mall_order_id IS '商城订单ID';
COMMENT ON COLUMN mkt_presale_order.paid_deposit IS '定金已支付：0=否 1=是';
COMMENT ON COLUMN mkt_presale_order.paid_final IS '尾款已支付：0=否 1=是';
COMMENT ON COLUMN mkt_presale_order.status IS '状态：0=已付定金 1=已付尾款 2=已取消';

CREATE INDEX IF NOT EXISTS idx_mkt_presale_order_presale ON mkt_presale_order(presale_id);
CREATE INDEX IF NOT EXISTS idx_mkt_presale_order_customer ON mkt_presale_order(customer_id);

-- ═══════════════════════════════════════════════════════════════
-- 3. 弹窗广告表 mall_popup_ad
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS mall_popup_ad (
    id                  BIGINT          PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL DEFAULT 1,

    -- 广告内容
    title               VARCHAR(200)    NOT NULL,
    image_url           VARCHAR(500),
    link_url            VARCHAR(500),

    -- 展示策略
    show_type           VARCHAR(20)     DEFAULT 'once',
    -- show_type: once=仅首次 everyday=每日 once_per_session=每次会话

    target_user         VARCHAR(20)     DEFAULT 'all',
    -- target_user: all=全部 member=会员 new=新用户

    -- 投放时间
    start_time          TIMESTAMP,
    end_time            TIMESTAMP,

    -- 排序/状态
    sort                INTEGER         DEFAULT 0,
    status              INTEGER         DEFAULT 0,
    -- status: 0=草稿 1=投放中 2=已结束 3=已下架

    remark              VARCHAR(500),

    -- 系统字段
    deleted             INTEGER         NOT NULL DEFAULT 0,
    create_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT
);

COMMENT ON TABLE mall_popup_ad IS '商城弹窗广告表';
COMMENT ON COLUMN mall_popup_ad.title IS '广告标题';
COMMENT ON COLUMN mall_popup_ad.image_url IS '弹窗图片URL';
COMMENT ON COLUMN mall_popup_ad.link_url IS '点击跳转链接';
COMMENT ON COLUMN mall_popup_ad.show_type IS '展示频次：once=仅首次 everyday=每日 once_per_session=每次会话';
COMMENT ON COLUMN mall_popup_ad.target_user IS '目标用户：all=全部 member=会员 new=新用户';
COMMENT ON COLUMN mall_popup_ad.start_time IS '投放开始时间';
COMMENT ON COLUMN mall_popup_ad.end_time IS '投放结束时间';
COMMENT ON COLUMN mall_popup_ad.status IS '状态：0=草稿 1=投放中 2=已结束 3=已下架';

-- ═══════════════════════════════════════════════════════════════
-- 4. 加价购规则表 mkt_addon_rule
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS mkt_addon_rule (
    id                  BIGINT          PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL DEFAULT 1,

    -- 规则信息
    rule_name           VARCHAR(200)    NOT NULL,
    main_product_id     BIGINT          NOT NULL,
    main_product_name   VARCHAR(200),
    addon_product_id    BIGINT          NOT NULL,
    addon_product_name  VARCHAR(200),
    addon_price         NUMERIC(18,2)   NOT NULL DEFAULT 0,
    max_per_order       INTEGER         DEFAULT 1,

    -- 时间
    start_time          TIMESTAMP       NOT NULL,
    end_time            TIMESTAMP       NOT NULL,

    -- 状态：0=停用 1=启用
    status              INTEGER         DEFAULT 0,
    sort                INTEGER         DEFAULT 0,
    remark              VARCHAR(500),

    -- 系统字段
    deleted             INTEGER         NOT NULL DEFAULT 0,
    create_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT
);

COMMENT ON TABLE mkt_addon_rule IS '加价购规则表';
COMMENT ON COLUMN mkt_addon_rule.rule_name IS '规则名称';
COMMENT ON COLUMN mkt_addon_rule.main_product_id IS '主商品ID（购主品）';
COMMENT ON COLUMN mkt_addon_rule.addon_product_id IS '加价商品ID';
COMMENT ON COLUMN mkt_addon_rule.addon_price IS '加价金额';
COMMENT ON COLUMN mkt_addon_rule.max_per_order IS '每单限购数量';
COMMENT ON COLUMN mkt_addon_rule.start_time IS '规则生效开始时间';
COMMENT ON COLUMN mkt_addon_rule.end_time IS '规则生效结束时间';
COMMENT ON COLUMN mkt_addon_rule.status IS '状态：0=停用 1=启用';

CREATE INDEX IF NOT EXISTS idx_mkt_addon_main ON mkt_addon_rule(main_product_id);
