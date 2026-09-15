-- =====================================================
-- V9.31.0: 营销忠诚体系 + 分佣体系 (对标 Odoo loyalty)
--
-- 1. erp_product_grade_price 增加时间窗口和数量阶梯
--    （支持限时特价/秒杀）
-- 2. erp_loyalty_program    促销/忠诚程序主表（活动容器）
-- 3. erp_loyalty_card       客户忠诚卡/积分账户
-- 4. erp_loyalty_coupon     优惠券/兑换码
-- 5. erp_commission_record  佣金记录（分佣/裂变）
--
-- 注：营销规则使用已有的 erp_marketing_rule 体系（从 erp-stock 迁移到 erp-loyalty 模块）
--     不再重复建 erp_loyalty_rule / erp_loyalty_reward
-- =====================================================

-- ═══════════════════════════════════════════════════════
-- 1. 等级价格表增加时间窗口和数量阶梯
-- ═══════════════════════════════════════════════════════
ALTER TABLE erp_product_grade_price
    ADD COLUMN IF NOT EXISTS date_start TIMESTAMP,
    ADD COLUMN IF NOT EXISTS date_end   TIMESTAMP,
    ADD COLUMN IF NOT EXISTS min_quantity NUMERIC(12, 2);

COMMENT ON COLUMN erp_product_grade_price.date_start IS '生效时间（NULL=立即生效），配合 date_end 实现限时特价/秒杀';
COMMENT ON COLUMN erp_product_grade_price.date_end IS '失效时间（NULL=永久有效）';
COMMENT ON COLUMN erp_product_grade_price.min_quantity IS '最小数量（NULL=不限），实现数量阶梯定价';

CREATE INDEX IF NOT EXISTS idx_grade_price_date ON erp_product_grade_price(date_start, date_end);

-- ═══════════════════════════════════════════════════════
-- 2. 促销/忠诚程序主表 (对标 Odoo loyalty.program)
--    程序是营销活动的容器，具体规则通过 erp_marketing_rule 管理
-- ═══════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS erp_loyalty_program (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT,
    program_type    VARCHAR(30)  NOT NULL,
    -- program_type 枚举:
    --   PROMOTION       满减/满赠（自动触发）
    --   COUPON          一次性优惠券
    --   DISCOUNT_CODE   优惠码
    --   LOYALTY         会员积分卡
    --   GIFT_CARD       礼品卡储值
    --   EWALLET         电子钱包
    --   NEXT_ORDER      下单后返券
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    trigger_type    VARCHAR(20),
    -- trigger_type 枚举:
    --   AUTO   自动触发（满足条件即生效）
    --   CODE   需要输入优惠码/兑换码
    start_date      TIMESTAMP,
    end_date        TIMESTAMP,
    is_active       INTEGER DEFAULT 1,
    max_usage       INTEGER,
    usage_count     INTEGER DEFAULT 0,
    apply_scope     VARCHAR(20),
    -- apply_scope 枚举:
    --   ON_ORDER    整单适用
    --   ON_PRODUCT  指定产品适用
    --   ON_CATEGORY 指定分类适用
    pricelist_id    BIGINT,
    -- 关联价格表ID（可选，用于在价格表基础上做促销）
    sort_order      INTEGER DEFAULT 100,
    remark          VARCHAR(500),
    deleted         INTEGER DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT NOW(),
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE erp_loyalty_program IS '促销/忠诚程序（对标 Odoo loyalty.program）';
COMMENT ON COLUMN erp_loyalty_program.program_type IS '程序类型: PROMOTION/COUPON/DISCOUNT_CODE/LOYALTY/GIFT_CARD/EWALLET/NEXT_ORDER';
COMMENT ON COLUMN erp_loyalty_program.trigger_type IS '触发方式: AUTO=自动触发, CODE=需输入码';

CREATE INDEX IF NOT EXISTS idx_loyalty_program_type ON erp_loyalty_program(program_type);
CREATE INDEX IF NOT EXISTS idx_loyalty_program_active ON erp_loyalty_program(is_active, deleted);

-- ═══════════════════════════════════════════════════════
-- 3. 客户忠诚卡/积分账户 (对标 Odoo loyalty.card)
-- ═══════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS erp_loyalty_card (
    id               BIGINT PRIMARY KEY,
    tenant_id        BIGINT,
    program_id       BIGINT NOT NULL,
    partner_id       BIGINT NOT NULL,
    -- 关联往来单位联系人
    card_code        VARCHAR(100),
    -- 卡号/会员卡编号
    points           NUMERIC(14, 4) DEFAULT 0,
    -- 当前积分/余额
    total_earned     NUMERIC(14, 4) DEFAULT 0,
    -- 累计获得
    total_redeemed   NUMERIC(14, 4) DEFAULT 0,
    -- 累计兑换
    expiration_date  TIMESTAMP,
    -- 积分/余额有效期
    is_active        INTEGER DEFAULT 1,
    remark           VARCHAR(500),
    deleted          INTEGER DEFAULT 0,
    create_by        BIGINT,
    create_time      TIMESTAMP DEFAULT NOW(),
    update_by        BIGINT,
    update_time      TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE erp_loyalty_card IS '客户忠诚卡/积分账户（对标 Odoo loyalty.card）';

CREATE UNIQUE INDEX IF NOT EXISTS uk_loyalty_card_code ON erp_loyalty_card(card_code);
CREATE INDEX IF NOT EXISTS idx_loyalty_card_partner ON erp_loyalty_card(partner_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_card_program ON erp_loyalty_card(program_id);

-- ═══════════════════════════════════════════════════════
-- 4. 优惠券/兑换码 (唯一码管理)
-- ═══════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS erp_loyalty_coupon (
    id               BIGINT PRIMARY KEY,
    tenant_id        BIGINT,
    program_id       BIGINT NOT NULL,
    partner_id       BIGINT,
    -- 使用人（NULL=未绑定）
    code             VARCHAR(100) NOT NULL,
    -- 优惠码/兑换码（唯一）
    status           VARCHAR(20) DEFAULT 'UNUSED',
    -- UNUSED / USED / EXPIRED / CANCELLED
    used_time        TIMESTAMP,
    -- 使用时间
    used_order_id    BIGINT,
    -- 关联订单ID
    face_value       NUMERIC(12, 2),
    -- 面额（礼品卡/电子钱包用）
    balance          NUMERIC(12, 2),
    -- 当前余额
    expiration_date  TIMESTAMP,
    remark           VARCHAR(500),
    deleted          INTEGER DEFAULT 0,
    create_by        BIGINT,
    create_time      TIMESTAMP DEFAULT NOW(),
    update_by        BIGINT,
    update_time      TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE erp_loyalty_coupon IS '优惠券/兑换码/礼品卡（唯一码管理）';
COMMENT ON COLUMN erp_loyalty_coupon.status IS '状态: UNUSED=未使用, USED=已使用, EXPIRED=已过期, CANCELLED=已取消';

CREATE UNIQUE INDEX IF NOT EXISTS uk_loyalty_coupon_code ON erp_loyalty_coupon(code);
CREATE INDEX IF NOT EXISTS idx_loyalty_coupon_program ON erp_loyalty_coupon(program_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_coupon_partner ON erp_loyalty_coupon(partner_id);

-- ═══════════════════════════════════════════════════════
-- 5. 佣金记录表（分佣/裂变）
-- ═══════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS erp_commission_record (
    id               BIGINT PRIMARY KEY,
    tenant_id        BIGINT,
    partner_id       BIGINT NOT NULL,
    -- 佣金归属的联系人（分销员/推荐人）
    order_id         BIGINT,
    -- 关联的订单ID
    order_amount     NUMERIC(14, 2),
    -- 订单金额
    commission_rate  NUMERIC(8, 4),
    -- 佣金比例(%)
    commission_amount NUMERIC(14, 2),
    -- 佣金金额
    tier_level       INTEGER DEFAULT 1,
    -- 层级：1=一级, 2=二级, 3=三级
    referrer_id      BIGINT,
    -- 直接推荐人ID（多级分佣链路中的上一级）
    status           VARCHAR(20) DEFAULT 'DRAFT',
    -- DRAFT / CONFIRMED / PAID / CANCELLED
    confirm_time     TIMESTAMP,
    pay_time         TIMESTAMP,
    remark           VARCHAR(500),
    deleted          INTEGER DEFAULT 0,
    create_by        BIGINT,
    create_time      TIMESTAMP DEFAULT NOW(),
    update_by        BIGINT,
    update_time      TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE erp_commission_record IS '佣金记录（分佣/裂变）';
COMMENT ON COLUMN erp_commission_record.status IS '状态: DRAFT=待确认, CONFIRMED=已确认, PAID=已结算, CANCELLED=已取消';

CREATE INDEX IF NOT EXISTS idx_commission_partner ON erp_commission_record(partner_id);
CREATE INDEX IF NOT EXISTS idx_commission_order ON erp_commission_record(order_id);
CREATE INDEX IF NOT EXISTS idx_commission_status ON erp_commission_record(status);

-- ═══════════════════════════════════════════════════════
-- 6. 营销规则体系（从 erp-stock 迁移，补建表+关联）
--    如果表已存在则跳过（IF NOT EXISTS）
-- ═══════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS erp_marketing_rule (
    id                          BIGINT PRIMARY KEY,
    tenant_id                   BIGINT,
    rule_code                   VARCHAR(100),
    rule_name                   VARCHAR(200),
    rule_type                   VARCHAR(50),
    rule_subtype                VARCHAR(50),
    priority                    INTEGER DEFAULT 100,
    is_stackable                INTEGER DEFAULT 0,
    start_time                  TIMESTAMP,
    end_time                    TIMESTAMP,
    time_limit_type             VARCHAR(20),
    weekly_bits                 INTEGER,
    daily_start                 TIME,
    daily_end                   TIME,
    min_order_amount            NUMERIC(14, 2),
    max_order_amount            NUMERIC(14, 2),
    min_quantity                INTEGER,
    max_quantity                INTEGER,
    applicable_partner_types    VARCHAR(500),
    applicable_partner_grade_ids VARCHAR(500),
    applicable_region_ids       VARCHAR(500),
    usage_limit_total           INTEGER,
    usage_limit_per_customer    INTEGER,
    use_count                   INTEGER DEFAULT 0,
    max_discount_amount         NUMERIC(14, 2),
    status                      VARCHAR(20) DEFAULT 'active',
    remark                      VARCHAR(500),
    program_id                  BIGINT,
    deleted                     INTEGER DEFAULT 0,
    create_by                   BIGINT,
    create_time                 TIMESTAMP DEFAULT NOW(),
    update_by                   BIGINT,
    update_time                 TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE erp_marketing_rule IS '营销规则主表（满减/满赠/折扣/特价/运费优惠）';

CREATE TABLE IF NOT EXISTS erp_marketing_discount (
    id              BIGINT PRIMARY KEY,
    rule_id         BIGINT NOT NULL,
    discount_type   VARCHAR(50),
    discount_value  NUMERIC(14, 4),
    is_product_level INTEGER DEFAULT 0,
    deleted         INTEGER DEFAULT 0,
    create_time     TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS erp_marketing_threshold (
    id              BIGINT PRIMARY KEY,
    rule_id         BIGINT NOT NULL,
    threshold_type  VARCHAR(50),
    threshold_value NUMERIC(14, 2),
    benefit_type    VARCHAR(50),
    benefit_value   NUMERIC(14, 2),
    is_multi_grade  INTEGER DEFAULT 0,
    next_rule_id    BIGINT,
    deleted         INTEGER DEFAULT 0,
    create_time     TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS erp_marketing_gift (
    id              BIGINT PRIMARY KEY,
    rule_id         BIGINT NOT NULL,
    gift_type       VARCHAR(50),
    gift_product_id BIGINT,
    gift_quantity   INTEGER DEFAULT 1,
    max_gifts       INTEGER,
    min_order_amount NUMERIC(14, 2),
    deleted         INTEGER DEFAULT 0,
    create_time     TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS erp_marketing_freight (
    id              BIGINT PRIMARY KEY,
    rule_id         BIGINT NOT NULL,
    freight_type    VARCHAR(50),
    freight_value   NUMERIC(14, 2),
    min_order_amount NUMERIC(14, 2),
    max_reduce_amount NUMERIC(14, 2),
    deleted         INTEGER DEFAULT 0,
    create_time     TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS erp_marketing_rule_product (
    id              BIGINT PRIMARY KEY,
    rule_id         BIGINT NOT NULL,
    scope_type      VARCHAR(20),
    product_id      BIGINT,
    category_id     BIGINT,
    deleted         INTEGER DEFAULT 0,
    create_time     TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_marketing_discount_rule ON erp_marketing_discount(rule_id);
CREATE INDEX IF NOT EXISTS idx_marketing_threshold_rule ON erp_marketing_threshold(rule_id);
CREATE INDEX IF NOT EXISTS idx_marketing_gift_rule ON erp_marketing_gift(rule_id);
CREATE INDEX IF NOT EXISTS idx_marketing_freight_rule ON erp_marketing_freight(rule_id);
CREATE INDEX IF NOT EXISTS idx_marketing_rule_product_rule ON erp_marketing_rule_product(rule_id);
CREATE INDEX IF NOT EXISTS idx_marketing_rule_program ON erp_marketing_rule(program_id);
