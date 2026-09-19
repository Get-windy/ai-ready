-- 营销模块 P1：会员等级规则 + 积分有效期与过期
--
-- 一、会员等级规则：复用既有 `erp_member_level`（会员等级档案，已有数据）补 5 列，不另建等级表
--     业界口径（Oracle Membership / 有赞会员权益系统）：等级 = 门槛（累计消费 / 成长值）+ 保级周期 + 默认等级
ALTER TABLE erp_member_level ADD COLUMN IF NOT EXISTS upgrade_amount NUMERIC(18, 2);
ALTER TABLE erp_member_level ADD COLUMN IF NOT EXISTS upgrade_points INTEGER;
ALTER TABLE erp_member_level ADD COLUMN IF NOT EXISTS keep_months INTEGER;
ALTER TABLE erp_member_level ADD COLUMN IF NOT EXISTS is_default INTEGER;

COMMENT ON COLUMN erp_member_level.upgrade_amount IS '升级门槛：累计消费额达到该值即升到本级（NULL=不按消费额）';
COMMENT ON COLUMN erp_member_level.upgrade_points IS '升级门槛：成长值/累计积分达到该值即升到本级（NULL=不按积分）';
COMMENT ON COLUMN erp_member_level.keep_months IS '保级周期（月，NULL=永久保级）';
COMMENT ON COLUMN erp_member_level.is_default IS '是否默认等级（1=新会员初始等级，全租户唯一）';

-- 去重：等级档案此前被重复播种（同一 level_name + sort_order 多行），保留 id 最小的一行
DELETE FROM erp_member_level a
    USING erp_member_level b
    WHERE a.id > b.id
      AND a.tenant_id = b.tenant_id
      AND a.level_name = b.level_name
      AND a.sort_order = b.sort_order
      AND a.deleted = 0 AND b.deleted = 0;

-- 兜底门槛（按 sort_order 递增；已配置的行不覆盖）
UPDATE erp_member_level SET upgrade_amount = 0    WHERE sort_order = 1 AND upgrade_amount IS NULL;
UPDATE erp_member_level SET upgrade_amount = 2000 WHERE sort_order = 2 AND upgrade_amount IS NULL;
UPDATE erp_member_level SET upgrade_amount = 10000 WHERE sort_order = 3 AND upgrade_amount IS NULL;
UPDATE erp_member_level SET upgrade_amount = 50000 WHERE sort_order = 4 AND upgrade_amount IS NULL;
UPDATE erp_member_level SET keep_months = 12 WHERE keep_months IS NULL;
UPDATE erp_member_level SET is_default = CASE WHEN sort_order = 1 THEN 1 ELSE 0 END WHERE is_default IS NULL;

-- 二、积分有效期：参数 + 批次台账
ALTER TABLE mkt_member_config ADD COLUMN IF NOT EXISTS points_valid_months INTEGER;
ALTER TABLE mkt_member_config ADD COLUMN IF NOT EXISTS points_expire_remind_days INTEGER;

COMMENT ON COLUMN mkt_member_config.points_valid_months IS '积分有效期（月，0/NULL=永不过期；业界通行 12 个月）';
COMMENT ON COLUMN mkt_member_config.points_expire_remind_days IS '到期前提醒天数（默认 30）';

-- 积分批次台账：一笔"获得"= 一个批次，扣减按 FIFO（先进先出）消耗各批次剩余
-- 这样"过期"才有意义：过期的是**某个批次尚未用完的剩余**，而不是账户总额
CREATE TABLE IF NOT EXISTS mkt_points_batch (
    id               BIGINT PRIMARY KEY,
    tenant_id        BIGINT         NOT NULL DEFAULT 0,
    member_card_no   VARCHAR(64),
    partner_id       BIGINT,
    /** 本批次获得积分 */
    earned_points    NUMERIC(18, 2),
    /** 本批次剩余可用积分（FIFO 扣减后） */
    remaining_points NUMERIC(18, 2),
    earned_time      TIMESTAMP,
    expire_time      TIMESTAMP,
    /** 来源：SALE 销售获得 / ADJUST 手工调整 / SIGNIN 签到 / GIFT 赠送 */
    source           VARCHAR(32),
    source_bill_no   VARCHAR(64),
    /** 状态：ACTIVE 有效 / EXHAUSTED 已用完 / EXPIRED 已过期 */
    status           VARCHAR(16),
    remark           VARCHAR(255),
    deleted          INTEGER        NOT NULL DEFAULT 0,
    create_time      TIMESTAMP,
    update_time      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mkt_points_batch_card
    ON mkt_points_batch (tenant_id, member_card_no, status);
CREATE INDEX IF NOT EXISTS idx_mkt_points_batch_expire
    ON mkt_points_batch (tenant_id, status, expire_time);

COMMENT ON TABLE mkt_points_batch IS '会员积分批次台账（FIFO 扣减 + 按批次过期，积分有效期闭环）';

-- 积分过期/变动流水（与销售订单积分流水 erp_sale_order_points_journal 互补：后者只记单据产生，本表记批次级变动）
CREATE TABLE IF NOT EXISTS mkt_points_journal (
    id             BIGINT PRIMARY KEY,
    tenant_id      BIGINT         NOT NULL DEFAULT 0,
    member_card_no VARCHAR(64),
    partner_id     BIGINT,
    /** 变动类型：EARN 获得 / USE 使用 / EXPIRE 过期扣减 / ADJUST 调整 */
    change_type    VARCHAR(16),
    /** 变动积分（正数=增加，负数=减少） */
    change_points  NUMERIC(18, 2),
    /** 变动后账户余额 */
    balance_after  NUMERIC(18, 2),
    batch_id       BIGINT,
    source_bill_no VARCHAR(64),
    remark         VARCHAR(255),
    deleted        INTEGER        NOT NULL DEFAULT 0,
    create_time    TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mkt_points_journal_card
    ON mkt_points_journal (tenant_id, member_card_no, create_time);

COMMENT ON TABLE mkt_points_journal IS '会员积分变动流水（EARN/USE/EXPIRE/ADJUST，含变动后余额）';
