-- =====================================================
-- V3.2.0: 营销管理模块 - 全部11种子类型 + 结算控制
-- =====================================================

-- ════════════════════════════════════════════════════
-- 一、营销规则主表(所有营销活动的基础)
-- ════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS erp_marketing_rule (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL DEFAULT 0,
    rule_code           VARCHAR(50) NOT NULL COMMENT '规则编码',
    rule_name           VARCHAR(200) NOT NULL COMMENT '规则名称',
    rule_type           VARCHAR(30) NOT NULL
                        COMMENT '规则类型 DISCOUNT折扣/THRESHOLD满减/FREIGHT免邮/GIFT赠品/TIERED阶梯价/GROUP_BUY团购/CUSTOM_PRICE客户定价/COMMISSION佣金/SHARE分享佣金/PROMOTION推广佣金',
    rule_subtype        VARCHAR(30) COMMENT '子类型(如满减的满X元减Y元)',
    priority            INTEGER DEFAULT 0 COMMENT '优先级(数字越小优先级越高)',
    is_stackable        INTEGER NOT NULL DEFAULT 0 COMMENT '是否可叠加其他规则',
    start_time          TIMESTAMP COMMENT '生效开始时间',
    end_time            TIMESTAMP COMMENT '生效结束时间',
    time_limit_type     VARCHAR(20) DEFAULT 'NONE' COMMENT '时间限制 NONE/RANGE/WEEKLY/DAILY',
    weekly_bits         INTEGER DEFAULT 0 COMMENT '星期位图(1=周日,2=周一...64=周六)',
    daily_start         TIME COMMENT '每日开始时间',
    daily_end           TIME COMMENT '每日结束时间',

    -- 适用条件
    min_order_amount    DECIMAL(20,2) DEFAULT 0 COMMENT '最小订单金额',
    max_order_amount    DECIMAL(20,2) DEFAULT 0 COMMENT '最大订单金额',
    min_quantity        INTEGER DEFAULT 0 COMMENT '最少数量',
    max_quantity        INTEGER DEFAULT 0 COMMENT '最多数量',
    applicable_partner_types VARCHAR(100) DEFAULT 'ALL' COMMENT '适用客户类型(ALL/CUSTOMER/SUPPLIER)',
    applicable_partner_grade_ids VARCHAR(500) COMMENT '适用客户等级ID列表(逗号分隔)',
    applicable_region_ids VARCHAR(500) COMMENT '适用地区ID列表',

    -- 使用限制
    usage_limit_total   INTEGER DEFAULT 0 COMMENT '总使用次数限制(0不限)',
    usage_limit_per_customer INTEGER DEFAULT 0 COMMENT '每客户使用次数限制',
    use_count           INTEGER DEFAULT 0 COMMENT '已使用次数',
    max_discount_amount DECIMAL(20,2) DEFAULT 0 COMMENT '最大优惠金额(0不限)',

    -- 状态
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                        COMMENT '状态 DRAFT/PUBLISHED/PAUSED/ENDED',
    remark              VARCHAR(500),
    deleted             INTEGER NOT NULL DEFAULT 0,
    create_by           BIGINT,
    create_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_marketing_rule IS '营销规则主表';
CREATE INDEX IF NOT EXISTS idx_mr_tenant ON erp_marketing_rule(tenant_id);
CREATE INDEX IF NOT EXISTS idx_mr_type ON erp_marketing_rule(rule_type);
CREATE INDEX IF NOT EXISTS idx_mr_status ON erp_marketing_rule(status);
CREATE INDEX IF NOT EXISTS idx_mr_time ON erp_marketing_rule(start_time, end_time);
CREATE UNIQUE INDEX IF NOT EXISTS idx_mr_code ON erp_marketing_rule(tenant_id, rule_code);

-- 2. 营销规则适用产品范围
CREATE TABLE IF NOT EXISTS erp_marketing_rule_product (
    id              BIGSERIAL PRIMARY KEY,
    rule_id         BIGINT NOT NULL REFERENCES erp_marketing_rule(id),
    scope_type      VARCHAR(20) NOT NULL DEFAULT 'INCLUDE'
                    COMMENT '范围类型 INCLUDE包含/EXCLUDE排除',
    product_id      BIGINT REFERENCES erp_product(id) COMMENT '产品ID(NULL表示全部)',
    category_id     BIGINT REFERENCES erp_product_category(id) COMMENT '分类ID(NULL表示全部)',
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_marketing_rule_product IS '营销规则适用产品范围';
CREATE INDEX IF NOT EXISTS idx_mrp_rule ON erp_marketing_rule_product(rule_id);
CREATE INDEX IF NOT EXISTS idx_mrp_product ON erp_marketing_rule_product(product_id);

-- 3. 营销规则适用客户范围
CREATE TABLE IF NOT EXISTS erp_marketing_rule_partner (
    id              BIGSERIAL PRIMARY KEY,
    rule_id         BIGINT NOT NULL REFERENCES erp_marketing_rule(id),
    scope_type      VARCHAR(20) NOT NULL DEFAULT 'INCLUDE'
                    COMMENT '范围类型 INCLUDE包含/EXCLUDE排除',
    partner_id      BIGINT REFERENCES erp_partner(id) COMMENT '客户ID(NULL表示全部)',
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_marketing_rule_partner IS '营销规则适用客户范围';
CREATE INDEX IF NOT EXISTS idx_mrpart_rule ON erp_marketing_rule_partner(rule_id);

-- ════════════════════════════════════════════════════
-- 二、各规则类型的特定配置
-- ════════════════════════════════════════════════════

-- 2.1 折扣规则(DISCOUNT)
CREATE TABLE IF NOT EXISTS erp_marketing_discount (
    id              BIGSERIAL PRIMARY KEY,
    rule_id         BIGINT NOT NULL UNIQUE REFERENCES erp_marketing_rule(id),
    discount_type   VARCHAR(20) NOT NULL COMMENT '折扣类型 PERCENT百分比/AMOUNT固定金额',
    discount_value  DECIMAL(20,6) NOT NULL COMMENT '折扣值(百分比为0-100的数字,金额为具体值)',
    is_product_level INTEGER NOT NULL DEFAULT 0 COMMENT '是否按单品折扣(否则按订单)',
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_marketing_discount IS '折扣规则配置';
CREATE INDEX IF NOT EXISTS idx_md_rule ON erp_marketing_discount(rule_id);

-- 2.2 满减规则(THRESHOLD)
CREATE TABLE IF NOT EXISTS erp_marketing_threshold (
    id              BIGSERIAL PRIMARY KEY,
    rule_id         BIGINT NOT NULL REFERENCES erp_marketing_rule(id),
    threshold_type  VARCHAR(20) NOT NULL COMMENT '门槛类型 AMOUNT金额/QUANTITY数量',
    threshold_value DECIMAL(20,2) NOT NULL COMMENT '门槛值',
    benefit_type    VARCHAR(20) NOT NULL COMMENT '优惠类型 REDUCE减金额/PERCENT打折/FIXED固定价',
    benefit_value   DECIMAL(20,6) NOT NULL COMMENT '优惠值',
    is_multi_grade  INTEGER NOT NULL DEFAULT 0 COMMENT '是否支持多级满减',
    next_rule_id    BIGINT REFERENCES erp_marketing_threshold(id) COMMENT '下一级规则ID(多级阶梯)',
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_marketing_threshold IS '满减阶梯规则配置';
CREATE INDEX IF NOT EXISTS idx_mth_rule ON erp_marketing_threshold(rule_id);

-- 2.3 免邮规则(FREIGHT_DISCOUNT)
CREATE TABLE IF NOT EXISTS erp_marketing_freight (
    id              BIGSERIAL PRIMARY KEY,
    rule_id         BIGINT NOT NULL UNIQUE REFERENCES erp_marketing_rule(id),
    freight_type    VARCHAR(20) NOT NULL DEFAULT 'FREE'
                    COMMENT '运费类型 FREE全免/REDUCE减固定/DISCOUNT打折',
    freight_value   DECIMAL(20,6) DEFAULT 0 COMMENT '减免值(减金额/打折比例)',
    min_order_amount DECIMAL(20,2) DEFAULT 0 COMMENT '最低订单金额(0不限)',
    max_reduce_amount DECIMAL(20,2) DEFAULT 0 COMMENT '最高减免金额(0不限)',
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_marketing_freight IS '免邮规则配置';
CREATE INDEX IF NOT EXISTS idx_mf_rule ON erp_marketing_freight(rule_id);

-- 2.4 赠品规则(GIFT)
CREATE TABLE IF NOT EXISTS erp_marketing_gift (
    id              BIGSERIAL PRIMARY KEY,
    rule_id         BIGINT NOT NULL REFERENCES erp_marketing_rule(id),
    gift_type       VARCHAR(20) NOT NULL COMMENT '赠品类型 FIXED固定赠品/CHOICE可选赠品/MULTI多件赠品',
    gift_product_id BIGINT NOT NULL REFERENCES erp_product(id),
    gift_quantity   INTEGER NOT NULL DEFAULT 1 COMMENT '赠送数量',
    max_gifts       INTEGER DEFAULT 1 COMMENT '最多可选赠品数(CHOICE类型)',
    min_order_amount DECIMAL(20,2) DEFAULT 0 COMMENT '最低订单金额',
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_marketing_gift IS '赠品规则配置';
CREATE INDEX IF NOT EXISTS idx_mg_rule ON erp_marketing_gift(rule_id);

-- 2.5 阶梯价规则(TIERED)
CREATE TABLE IF NOT EXISTS erp_marketing_tiered (
    id              BIGSERIAL PRIMARY KEY,
    rule_id         BIGINT NOT NULL REFERENCES erp_marketing_rule(id),
    tier_type       VARCHAR(20) NOT NULL COMMENT '阶梯类型 QUANTITY数量/AMOUNT金额',
    tier_min        DECIMAL(20,6) NOT NULL COMMENT '阶梯下限',
    tier_max        DECIMAL(20,6) DEFAULT 0 COMMENT '阶梯上限(0不设上限)',
    price_type      VARCHAR(20) NOT NULL COMMENT '定价类型 PERCENT打折/FIXED固定价/AMOUNT减金额',
    price_value     DECIMAL(20,6) NOT NULL COMMENT '定价值',
    sort_order      INTEGER DEFAULT 0,
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_marketing_tiered IS '阶梯价规则配置';
CREATE INDEX IF NOT EXISTS idx_mti_rule ON erp_marketing_tiered(rule_id);

-- ════════════════════════════════════════════════════
-- 三、团购模块
-- ════════════════════════════════════════════════════

-- 3.1 团购活动
CREATE TABLE IF NOT EXISTS erp_group_buy_activity (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL DEFAULT 0,
    activity_code       VARCHAR(50) NOT NULL COMMENT '活动编码',
    activity_name       VARCHAR(200) NOT NULL COMMENT '活动名称',
    product_id          BIGINT NOT NULL REFERENCES erp_product(id) COMMENT '团购产品',
    original_price      DECIMAL(20,2) NOT NULL COMMENT '原价',
    group_price         DECIMAL(20,2) NOT NULL COMMENT '团购价',
    min_group_size      INTEGER NOT NULL DEFAULT 2 COMMENT '最少成团人数',
    max_group_size      INTEGER DEFAULT 0 COMMENT '最多参团人数(0不限)',
    time_limit_minutes  INTEGER DEFAULT 0 COMMENT '限时分钟(0不限时)',
    quantity_limit      INTEGER DEFAULT 0 COMMENT '每人限购数量(0不限)',
    total_quantity      INTEGER DEFAULT 0 COMMENT '总库存(0不限)',
    sold_quantity       INTEGER DEFAULT 0 COMMENT '已售数量',

    -- 时间
    start_time          TIMESTAMP NOT NULL COMMENT '开始时间',
    end_time            TIMESTAMP NOT NULL COMMENT '结束时间',

    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                        COMMENT '状态 DRAFT/PUBLISHED/ONGOING/ENDED',
    remark              VARCHAR(500),
    deleted             INTEGER NOT NULL DEFAULT 0,
    create_by           BIGINT,
    create_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_group_buy_activity IS '团购活动表';
CREATE INDEX IF NOT EXISTS idx_gba_tenant ON erp_group_buy_activity(tenant_id);
CREATE INDEX IF NOT EXISTS idx_gba_product ON erp_group_buy_activity(product_id);
CREATE INDEX IF NOT EXISTS idx_gba_time ON erp_group_buy_activity(start_time, end_time);
CREATE UNIQUE INDEX IF NOT EXISTS idx_gba_code ON erp_group_buy_activity(tenant_id, activity_code);

-- 3.2 团购参与记录
CREATE TABLE IF NOT EXISTS erp_group_buy_participant (
    id              BIGSERIAL PRIMARY KEY,
    activity_id     BIGINT NOT NULL REFERENCES erp_group_buy_activity(id),
    group_id        VARCHAR(50) COMMENT '团编号(同一团相同编号)',
    partner_id      BIGINT REFERENCES erp_partner(id) COMMENT '参与客户',
    user_name       VARCHAR(100) COMMENT '用户名',
    quantity        INTEGER NOT NULL DEFAULT 1 COMMENT '购买数量',
    order_id        BIGINT COMMENT '关联订单ID',
    order_status    VARCHAR(20) COMMENT '订单状态',
    is_creator      INTEGER NOT NULL DEFAULT 0 COMMENT '是否开团人',
    group_status    VARCHAR(20) DEFAULT 'PENDING'
                    COMMENT '参团状态 PENDING待成团/SUCCESS已成团/FAILED已失败/REFUND已退款',
    join_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_group_buy_participant IS '团购参与记录表';
CREATE INDEX IF NOT EXISTS idx_gbp_activity ON erp_group_buy_participant(activity_id);
CREATE INDEX IF NOT EXISTS idx_gbp_group ON erp_group_buy_participant(group_id);
CREATE INDEX IF NOT EXISTS idx_gbp_partner ON erp_group_buy_participant(partner_id);

-- ════════════════════════════════════════════════════
-- 四、客户特定定价
-- ════════════════════════════════════════════════════

-- 客户特定产品价格
CREATE TABLE IF NOT EXISTS erp_customer_product_price (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    partner_id      BIGINT NOT NULL REFERENCES erp_partner(id) COMMENT '客户ID',
    product_id      BIGINT NOT NULL REFERENCES erp_product(id) COMMENT '产品ID',
    price_type      VARCHAR(20) NOT NULL DEFAULT 'SALE'
                    COMMENT '价格类型 SALE销售价/PURCHASE采购价',
    price           DECIMAL(20,2) NOT NULL COMMENT '特定价格',
    min_order_qty   INTEGER DEFAULT 0 COMMENT '最小起订量',
    is_active       INTEGER NOT NULL DEFAULT 1,
    effective_date  TIMESTAMP COMMENT '生效日期',
    expire_date     TIMESTAMP COMMENT '失效日期',
    remark          VARCHAR(500),
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_customer_product_price IS '客户特定产品价格表';
CREATE INDEX IF NOT EXISTS idx_cpp_partner ON erp_customer_product_price(partner_id);
CREATE INDEX IF NOT EXISTS idx_cpp_product ON erp_customer_product_price(product_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_cpp_unique ON erp_customer_product_price(tenant_id, partner_id, product_id, price_type);

-- ════════════════════════════════════════════════════
-- 五、佣金模块
-- ════════════════════════════════════════════════════

-- 5.1 佣金规则
CREATE TABLE IF NOT EXISTS erp_commission_rule (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL DEFAULT 0,
    rule_code           VARCHAR(50) NOT NULL COMMENT '规则编码',
    rule_name           VARCHAR(200) NOT NULL COMMENT '规则名称',
    commission_type     VARCHAR(30) NOT NULL
                        COMMENT '佣金类型 SALE销售佣金/PROMOTION推广佣金/SHARE分享佣金',
    calc_basis          VARCHAR(20) NOT NULL DEFAULT 'AMOUNT'
                        COMMENT '计算基础 AMOUNT按金额/QUANTITY按数量/PROFIT按利润',
    calc_method         VARCHAR(20) NOT NULL DEFAULT 'PERCENT'
                        COMMENT '计算方式 PERCENT百分比/FIXED固定金额',
    commission_value    DECIMAL(20,6) NOT NULL COMMENT '佣金值',
    max_commission      DECIMAL(20,2) DEFAULT 0 COMMENT '单笔佣金上限',
    min_order_amount    DECIMAL(20,2) DEFAULT 0 COMMENT '最低订单金额',

    -- 适用范围
    applicable_products VARCHAR(500) COMMENT '适用产品ID列表(ALL或逗号分隔)',
    applicable_partner_grades VARCHAR(500) COMMENT '适用客户等级ID列表',

    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    remark              VARCHAR(500),
    deleted             INTEGER NOT NULL DEFAULT 0,
    create_by           BIGINT,
    create_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_commission_rule IS '佣金规则表';
CREATE INDEX IF NOT EXISTS idx_cr_tenant ON erp_commission_rule(tenant_id);
CREATE INDEX IF NOT EXISTS idx_cr_type ON erp_commission_rule(commission_type);
CREATE UNIQUE INDEX IF NOT EXISTS idx_cr_code ON erp_commission_rule(tenant_id, rule_code);

-- 5.2 佣金结算配置
CREATE TABLE IF NOT EXISTS erp_commission_settlement_config (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL DEFAULT 0,
    config_name         VARCHAR(100) NOT NULL COMMENT '配置名称',
    settle_cycle        VARCHAR(20) NOT NULL DEFAULT 'MONTHLY'
                        COMMENT '结算周期 WEEKLY/MONTHLY/QUARTERLY/DAILY',
    settle_day          INTEGER DEFAULT 0 COMMENT '结算日(周几/几号)',
    min_settle_amount   DECIMAL(20,2) DEFAULT 0 COMMENT '最低结算金额',
    max_settle_amount   DECIMAL(20,2) DEFAULT 0 COMMENT '最高结算金额(0不限)',
    approve_required    INTEGER NOT NULL DEFAULT 1 COMMENT '是否需要审核',
    status              INTEGER NOT NULL DEFAULT 1,
    remark              VARCHAR(500),
    deleted             INTEGER NOT NULL DEFAULT 0,
    create_by           BIGINT,
    create_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_commission_settlement_config IS '佣金结算配置表';

-- ════════════════════════════════════════════════════
-- 六、分享佣金模块
-- ════════════════════════════════════════════════════

-- 6.1 分享活动
CREATE TABLE IF NOT EXISTS erp_share_activity (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL DEFAULT 0,
    activity_code       VARCHAR(50) NOT NULL COMMENT '活动编码',
    activity_name       VARCHAR(200) NOT NULL COMMENT '活动名称',
    share_type          VARCHAR(20) NOT NULL DEFAULT 'PRODUCT'
                        COMMENT '分享类型 PRODUCT产品/LINK链接/COUPON优惠券/ARTICLE文章',
    share_target_id     BIGINT COMMENT '分享目标ID(产品ID等)',
    share_title         VARCHAR(200) COMMENT '分享标题',
    share_description   VARCHAR(500) COMMENT '分享描述',
    share_image_url     VARCHAR(500) COMMENT '分享图片',

    -- 佣金规则关联
    commission_rule_id  BIGINT REFERENCES erp_commission_rule(id),
    first_visit_commission DECIMAL(20,6) DEFAULT 0 COMMENT '首次访问佣金',
    first_order_commission DECIMAL(20,6) DEFAULT 0 COMMENT '首次下单佣金',

    -- 时间
    start_time          TIMESTAMP,
    end_time            TIMESTAMP,
    daily_limit         INTEGER DEFAULT 0 COMMENT '每日分享次数上限',
    total_limit         INTEGER DEFAULT 0 COMMENT '总次数上限',

    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    remark              VARCHAR(500),
    deleted             INTEGER NOT NULL DEFAULT 0,
    create_by           BIGINT,
    create_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_share_activity IS '分享活动表';
CREATE UNIQUE INDEX IF NOT EXISTS idx_sa_code ON erp_share_activity(tenant_id, activity_code);

-- 6.2 分享关系链
CREATE TABLE IF NOT EXISTS erp_share_relation (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    activity_id     BIGINT REFERENCES erp_share_activity(id),
    sharer_id       BIGINT NOT NULL COMMENT '分享人(partner_id)',
    visitor_id      BIGINT COMMENT '访客(partner_id)',
    share_code      VARCHAR(100) NOT NULL COMMENT '分享码(唯一)',
    visit_count     INTEGER DEFAULT 0 COMMENT '访问次数',
    order_count     INTEGER DEFAULT 0 COMMENT '下单次数',
    total_commission DECIMAL(20,2) DEFAULT 0 COMMENT '累计佣金',
    relation_level  INTEGER DEFAULT 1 COMMENT '关系层级(1=直接,2=间接)',
    first_visit_time TIMESTAMP COMMENT '首次访问时间',
    expire_time     TIMESTAMP COMMENT '过期时间',
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_share_relation IS '分享关系链';
CREATE INDEX IF NOT EXISTS idx_srel_sharer ON erp_share_relation(sharer_id);
CREATE INDEX IF NOT EXISTS idx_srel_visitor ON erp_share_relation(visitor_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_srel_code ON erp_share_relation(share_code);

-- 6.3 分享佣金明细
CREATE TABLE IF NOT EXISTS erp_share_commission_detail (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL DEFAULT 0,
    relation_id         BIGINT NOT NULL REFERENCES erp_share_relation(id),
    order_id            BIGINT COMMENT '订单ID',
    order_amount        DECIMAL(20,2) DEFAULT 0 COMMENT '订单金额',
    commission_rate     DECIMAL(10,6) DEFAULT 0 COMMENT '佣金比例',
    commission_amount   DECIMAL(20,2) NOT NULL COMMENT '佣金金额',
    commission_status   VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        COMMENT '状态 PENDING待结算/SETTLED已结算/CANCEL已取消',
    settle_time         TIMESTAMP COMMENT '结算时间',
    remark              VARCHAR(500),
    deleted             INTEGER NOT NULL DEFAULT 0,
    create_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_share_commission_detail IS '分享佣金明细表';
CREATE INDEX IF NOT EXISTS idx_scd_relation ON erp_share_commission_detail(relation_id);
CREATE INDEX IF NOT EXISTS idx_scd_order ON erp_share_commission_detail(order_id);
CREATE INDEX IF NOT EXISTS idx_scd_status ON erp_share_commission_detail(commission_status);

-- ════════════════════════════════════════════════════
-- 七、推广渠道与推广佣金
-- ════════════════════════════════════════════════════

-- 7.1 推广渠道
CREATE TABLE IF NOT EXISTS erp_promotion_channel (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    channel_code    VARCHAR(50) NOT NULL COMMENT '渠道编码',
    channel_name    VARCHAR(200) NOT NULL COMMENT '渠道名称',
    channel_type    VARCHAR(30) NOT NULL DEFAULT 'SOCIAL'
                    COMMENT '渠道类型 SOCIAL社交/VIDEO视频/BLOG博客/EMAIL邮件/SMS短信/OTHER其他',
    contact_person  VARCHAR(100) COMMENT '联系人',
    contact_phone   VARCHAR(50) COMMENT '联系电话',
    commission_rate DECIMAL(10,6) DEFAULT 0 COMMENT '默认佣金比例',
    settle_cycle    VARCHAR(20) DEFAULT 'MONTHLY' COMMENT '结算周期',
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    remark          VARCHAR(500),
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_promotion_channel IS '推广渠道表';
CREATE UNIQUE INDEX IF NOT EXISTS idx_pc_code ON erp_promotion_channel(tenant_id, channel_code);

-- 7.2 推广活动
CREATE TABLE IF NOT EXISTS erp_promotion_activity (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    channel_id      BIGINT NOT NULL REFERENCES erp_promotion_channel(id),
    activity_code   VARCHAR(50) NOT NULL COMMENT '活动编码',
    activity_name   VARCHAR(200) NOT NULL COMMENT '活动名称',
    activity_type   VARCHAR(30) NOT NULL COMMENT '活动类型',
    commission_rule_id BIGINT REFERENCES erp_commission_rule(id),

    budget_amount   DECIMAL(20,2) DEFAULT 0 COMMENT '预算金额',
    cost_amount     DECIMAL(20,2) DEFAULT 0 COMMENT '已花费金额',
    target_amount   DECIMAL(20,2) DEFAULT 0 COMMENT '目标金额',
    current_amount  DECIMAL(20,2) DEFAULT 0 COMMENT '当前完成金额',

    start_time      TIMESTAMP,
    end_time        TIMESTAMP,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                    COMMENT '状态 DRAFT/PUBLISHED/RUNNING/ENDED',
    remark          VARCHAR(500),
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_promotion_activity IS '推广活动表';
CREATE INDEX IF NOT EXISTS idx_pa_channel ON erp_promotion_activity(channel_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_pa_code ON erp_promotion_activity(tenant_id, activity_code);

-- 7.3 推广记录
CREATE TABLE IF NOT EXISTS erp_promotion_log (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    activity_id     BIGINT NOT NULL REFERENCES erp_promotion_activity(id),
    channel_id      BIGINT NOT NULL REFERENCES erp_promotion_channel(id),
    promo_code      VARCHAR(100) COMMENT '推广码',
    visitor_id      BIGINT COMMENT '访客(partner_id)',
    visitor_ip      VARCHAR(50) COMMENT '访客IP',
    user_agent      VARCHAR(500) COMMENT '用户代理',
    referrer_url    VARCHAR(500) COMMENT '来源URL',
    visit_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    order_id        BIGINT COMMENT '关联订单',
    order_amount    DECIMAL(20,2) DEFAULT 0 COMMENT '订单金额',
    commission_amount DECIMAL(20,2) DEFAULT 0 COMMENT '佣金金额',
    status          VARCHAR(20) DEFAULT 'PENDING'
                    COMMENT '状态 CLICK点击/VISIT访问/ORDER下单/SETTLEMENT已结算',
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_promotion_log IS '推广记录表';
CREATE INDEX IF NOT EXISTS idx_pl_activity ON erp_promotion_log(activity_id);
CREATE INDEX IF NOT EXISTS idx_pl_channel ON erp_promotion_log(channel_id);
CREATE INDEX IF NOT EXISTS idx_pl_order ON erp_promotion_log(order_id);
CREATE INDEX IF NOT EXISTS idx_pl_time ON erp_promotion_log(visit_time);

-- 7.4 推广结算记录
CREATE TABLE IF NOT EXISTS erp_promotion_settlement (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL DEFAULT 0,
    channel_id          BIGINT NOT NULL REFERENCES erp_promotion_channel(id),
    activity_id         BIGINT REFERENCES erp_promotion_activity(id),
    settle_no           VARCHAR(50) NOT NULL COMMENT '结算单号',
    settle_cycle_start  DATE NOT NULL COMMENT '结算周期开始',
    settle_cycle_end    DATE NOT NULL COMMENT '结算周期结束',
    total_orders        INTEGER DEFAULT 0 COMMENT '订单数',
    total_amount        DECIMAL(20,2) DEFAULT 0 COMMENT '总金额',
    total_commission    DECIMAL(20,2) DEFAULT 0 COMMENT '总佣金',
    actual_pay_amount   DECIMAL(20,2) DEFAULT 0 COMMENT '实付金额',
    settle_status       VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        COMMENT '状态 PENDING待结算/APPROVED已审核/PAID已付款/CANCEL已取消',
    approve_by          BIGINT COMMENT '审核人',
    approve_time        TIMESTAMP COMMENT '审核时间',
    pay_time            TIMESTAMP COMMENT '付款时间',
    remark              VARCHAR(500),
    deleted             INTEGER NOT NULL DEFAULT 0,
    create_by           BIGINT,
    create_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_promotion_settlement IS '推广结算记录表';
CREATE INDEX IF NOT EXISTS idx_ps_channel ON erp_promotion_settlement(channel_id);
CREATE INDEX IF NOT EXISTS idx_ps_status ON erp_promotion_settlement(settle_status);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ps_no ON erp_promotion_settlement(settle_no);

-- ════════════════════════════════════════════════════
-- 八、用户余额/资金管理
-- ════════════════════════════════════════════════════

-- 8.1 用户余额表
CREATE TABLE IF NOT EXISTS erp_user_balance (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    partner_id      BIGINT NOT NULL UNIQUE REFERENCES erp_partner(id) COMMENT '往来单位ID',
    total_balance   DECIMAL(20,2) NOT NULL DEFAULT 0 COMMENT '总余额',
    available_balance DECIMAL(20,2) NOT NULL DEFAULT 0 COMMENT '可用余额',
    frozen_balance  DECIMAL(20,2) NOT NULL DEFAULT 0 COMMENT '冻结余额',
    total_recharged DECIMAL(20,2) DEFAULT 0 COMMENT '累计充值',
    total_withdrawn DECIMAL(20,2) DEFAULT 0 COMMENT '累计提现',
    total_earned    DECIMAL(20,2) DEFAULT 0 COMMENT '累计收入(佣金等)',
    total_spent     DECIMAL(20,2) DEFAULT 0 COMMENT '累计消费',
    version         INTEGER NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                    COMMENT '状态 ACTIVE/FROZEN/CLOSED',
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_user_balance IS '用户余额表';
CREATE INDEX IF NOT EXISTS idx_ub_partner ON erp_user_balance(partner_id);

-- 8.2 余额流水表
CREATE TABLE IF NOT EXISTS erp_balance_log (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    partner_id      BIGINT NOT NULL REFERENCES erp_partner(id),
    balance_id      BIGINT NOT NULL REFERENCES erp_user_balance(id),
    log_no          VARCHAR(50) NOT NULL COMMENT '流水号',
    change_type     VARCHAR(30) NOT NULL
                    COMMENT '变动类型 RECHARGE充值/WITHDRAW提现/COMMISSION佣金收入/ORDER_PAY订单支付/ORDER_REFUND退款/ADMIN_ADJUST后台调整/FROZEN冻结/UNFROZEN解冻',
    change_amount   DECIMAL(20,2) NOT NULL COMMENT '变动金额(正为增加,负为减少)',
    before_balance  DECIMAL(20,2) NOT NULL COMMENT '变动前余额',
    after_balance   DECIMAL(20,2) NOT NULL COMMENT '变动后余额',
    biz_type        VARCHAR(50) COMMENT '业务类型',
    biz_id          BIGINT COMMENT '业务ID',
    remark          VARCHAR(500) COMMENT '备注',
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_balance_log IS '余额流水表';
CREATE INDEX IF NOT EXISTS idx_bl_partner ON erp_balance_log(partner_id);
CREATE INDEX IF NOT EXISTS idx_bl_balance ON erp_balance_log(balance_id);
CREATE INDEX IF NOT EXISTS idx_bl_type ON erp_balance_log(change_type);
CREATE INDEX IF NOT EXISTS idx_bl_time ON erp_balance_log(create_time);
CREATE UNIQUE INDEX IF NOT EXISTS idx_bl_no ON erp_balance_log(log_no);

-- 8.3 提现申请表
CREATE TABLE IF NOT EXISTS erp_withdraw_request (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    partner_id      BIGINT NOT NULL REFERENCES erp_partner(id),
    withdraw_no     VARCHAR(50) NOT NULL COMMENT '提现单号',
    amount          DECIMAL(20,2) NOT NULL COMMENT '提现金额',
    fee             DECIMAL(20,2) DEFAULT 0 COMMENT '手续费',
    actual_amount   DECIMAL(20,2) NOT NULL COMMENT '到账金额',
    bank_account_id BIGINT REFERENCES erp_partner_bank_account(id) COMMENT '提现到账户',
    account_name    VARCHAR(200) COMMENT '开户名',
    bank_name       VARCHAR(200) COMMENT '银行',
    account_no      VARCHAR(100) COMMENT '账号',
    request_reason  VARCHAR(500) COMMENT '提现原因',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    COMMENT '状态 PENDING待审核/APPROVED已通过/REJECTED已拒绝/PAID已付款',
    approve_by      BIGINT COMMENT '审核人',
    approve_time    TIMESTAMP COMMENT '审核时间',
    approve_remark  VARCHAR(500) COMMENT '审核备注',
    pay_time        TIMESTAMP COMMENT '付款时间',
    remark          VARCHAR(500),
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_withdraw_request IS '提现申请表';
CREATE INDEX IF NOT EXISTS idx_wr_partner ON erp_withdraw_request(partner_id);
CREATE INDEX IF NOT EXISTS idx_wr_status ON erp_withdraw_request(status);
CREATE UNIQUE INDEX IF NOT EXISTS idx_wr_no ON erp_withdraw_request(withdraw_no);

-- 8.4 结算周期配置
CREATE TABLE IF NOT EXISTS erp_settlement_cycle (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    cycle_name      VARCHAR(100) NOT NULL COMMENT '周期名称',
    cycle_type      VARCHAR(20) NOT NULL COMMENT '类型 WEEKLY/MONTHLY/QUARTERLY/YEARLY',
    cycle_day       INTEGER NOT NULL COMMENT '结算日(周几或几号)',
    auto_settle     INTEGER NOT NULL DEFAULT 0 COMMENT '是否自动结算',
    min_amount      DECIMAL(20,2) DEFAULT 0 COMMENT '最低结算金额',
    max_amount      DECIMAL(20,2) DEFAULT 0 COMMENT '最高结算金额',
    status          INTEGER NOT NULL DEFAULT 1,
    remark          VARCHAR(500),
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_settlement_cycle IS '结算周期配置表';
