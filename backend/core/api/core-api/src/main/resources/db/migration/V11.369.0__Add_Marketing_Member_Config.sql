-- 营销模块 → 会员设置（菜单 80302）：会员体系参数单行配置
-- 对标 ql361「营销 → 会员中心 → 会员设置」三分区设置单（会员设置 / 会员积分 / 积分抵现）
-- 口径说明：
--   · 单行配置（每个租户一行），读不到时由服务端按默认值建行，读接口永不返回 null
--   · 布尔开关一律 INTEGER 0/1（与全库既有配置表一致）
--   · consume_points_mode：BY_AMOUNT=按销售金额积分（amount_per_point：N 元=1 分）
--                          BY_PRODUCT=按不同商品累计积分（商品级系数由「详细设置」维护）
--   · points_round_rule：ROUND=四舍五入 / FLOOR=舍去 / CEIL=进位
--   · apply_scene_offline / apply_scene_mall：积分应用场景（线下开单 / 微商城）双通道
--   · points_per_yuan：积分抵现比例（N 积分=1 元）；max_deduct_percent：单笔订单最高可抵扣金额百分比
CREATE TABLE IF NOT EXISTS mkt_member_config (
    id                     BIGINT PRIMARY KEY,
    tenant_id              BIGINT       NOT NULL DEFAULT 0,
    member_enabled         INTEGER,
    auto_upgrade_enabled   INTEGER,
    points_reward_enabled  INTEGER,
    register_points        NUMERIC(18, 2),
    birthday_multiple      NUMERIC(18, 2),
    consume_points_enabled INTEGER,
    consume_points_mode    VARCHAR(32),
    amount_per_point       NUMERIC(18, 2),
    points_by_discount     INTEGER,
    points_round_rule      VARCHAR(32),
    apply_scene_offline    INTEGER,
    apply_scene_mall       INTEGER,
    signin_enabled         INTEGER,
    signin_first_points    INTEGER,
    signin_increment       INTEGER,
    signin_max_points      INTEGER,
    cash_deduct_enabled    INTEGER,
    points_per_yuan        NUMERIC(18, 2),
    max_deduct_percent     NUMERIC(5, 2),
    deleted                INTEGER      NOT NULL DEFAULT 0,
    create_time            TIMESTAMP,
    update_time            TIMESTAMP
);

COMMENT ON TABLE mkt_member_config IS '会员设置（营销域参数单行配置，菜单 80302）';
COMMENT ON COLUMN mkt_member_config.member_enabled IS '客户|会员管理开关';
COMMENT ON COLUMN mkt_member_config.auto_upgrade_enabled IS '会员自动升级开关';
COMMENT ON COLUMN mkt_member_config.points_reward_enabled IS '积分奖励区块开关';
COMMENT ON COLUMN mkt_member_config.register_points IS '注册初始积分';
COMMENT ON COLUMN mkt_member_config.birthday_multiple IS '会员生日倍积分';
COMMENT ON COLUMN mkt_member_config.consume_points_enabled IS '消费积分区块开关';
COMMENT ON COLUMN mkt_member_config.consume_points_mode IS '消费积分算法：BY_AMOUNT=按销售金额/BY_PRODUCT=按不同商品累计';
COMMENT ON COLUMN mkt_member_config.amount_per_point IS '按销售金额积分：N 元=1 分';
COMMENT ON COLUMN mkt_member_config.points_by_discount IS '按折扣积分';
COMMENT ON COLUMN mkt_member_config.points_round_rule IS '积分取整规则：ROUND/FLOOR/CEIL';
COMMENT ON COLUMN mkt_member_config.apply_scene_offline IS '积分应用场景-线下开单';
COMMENT ON COLUMN mkt_member_config.apply_scene_mall IS '积分应用场景-微商城';
COMMENT ON COLUMN mkt_member_config.signin_enabled IS '签到积分区块开关';
COMMENT ON COLUMN mkt_member_config.signin_first_points IS '第一天签到积分';
COMMENT ON COLUMN mkt_member_config.signin_increment IS '连续签到每天增加';
COMMENT ON COLUMN mkt_member_config.signin_max_points IS '连续签到最大获得';
COMMENT ON COLUMN mkt_member_config.cash_deduct_enabled IS '积分抵现区块开关';
COMMENT ON COLUMN mkt_member_config.points_per_yuan IS '抵现比例：N 积分=1 元';
COMMENT ON COLUMN mkt_member_config.max_deduct_percent IS '单笔订单最高可抵扣金额百分比';

-- 商品级积分系数（会员设置 → 会员积分 → 消费积分 →「按不同商品累计积分」→「详细设置」）
-- ⚠️ 本系统建模：对标该弹窗的表单明细未实测（见《会员设置开发文档》§2「详细设置」），
--    此处按「商品 × 积分系数」最小结构落库，仅服务于本系统按不同商品累计积分的算法。
CREATE TABLE IF NOT EXISTS mkt_product_points_rule (
    id                 BIGINT PRIMARY KEY,
    tenant_id          BIGINT       NOT NULL DEFAULT 0,
    product_id         BIGINT       NOT NULL,
    product_code       VARCHAR(64),
    product_name       VARCHAR(255),
    points_coefficient NUMERIC(18, 4),
    status             INTEGER      NOT NULL DEFAULT 1,
    remark             VARCHAR(255),
    deleted            INTEGER      NOT NULL DEFAULT 0,
    create_time        TIMESTAMP,
    update_time        TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mkt_product_points_rule_tenant
    ON mkt_product_points_rule (tenant_id, product_id);

COMMENT ON TABLE mkt_product_points_rule IS '商品级积分系数（会员设置-按不同商品累计积分-详细设置，本系统建模）';
COMMENT ON COLUMN mkt_product_points_rule.points_coefficient IS '积分系数：每 1 元销售金额累计的积分数';
