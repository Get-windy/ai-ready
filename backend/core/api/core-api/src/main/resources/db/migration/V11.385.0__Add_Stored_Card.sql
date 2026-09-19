-- 营销模块 P3：储值卡（菜单 80304「储值卡」，本系统建模页）
--
-- 定位：独立账户 + 单据的预付费载体——开卡 → 充值（含赠送）→ 消费扣减 → 退款 → 余额流水台账。
-- 业界口径：金蝶云星辰「积分储值」、有赞「储值即会员（充 300 送 60）」、微盟「权益卡」。
-- ⚠️ ql361 营销域无对应页（实测仅 17 页），本页为**本系统建模**，不编造对标列。
--
-- ⚠️ 合规前置（页面必须提供退款入口与告知，不得设计为"只进不出"）：
--   · 商务部《单用途商业预付卡管理办法》——发卡须备案；充值档位不宜过高、赠送比例须在毛利承受范围内
--   · 最高法《关于审理预付式消费民事纠纷案件适用法律若干问题的解释》（法释〔2025〕4 号，2025-05-01 施行）
--     ——对"卷款跑路""套路营销"有明确责任认定，须明确退款规则与告知义务
CREATE TABLE IF NOT EXISTS mkt_stored_card (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL DEFAULT 0,
    card_no         VARCHAR(64)  NOT NULL,
    partner_id      BIGINT,
    partner_name    VARCHAR(255),
    /** 卡类型：STORED 储值卡（充值后消费） / GIFT 礼品卡（固定面值消费） */
    card_type       VARCHAR(16),
    /** 面值（礼品卡为固定面值；储值卡为开卡金额） */
    face_value      NUMERIC(18, 2),
    /** 当前余额 */
    balance         NUMERIC(18, 2),
    /** 累计充值（含赠送） */
    total_recharge  NUMERIC(18, 2),
    /** 累计消费 */
    total_consume   NUMERIC(18, 2),
    /** 累计赠送 */
    total_bonus     NUMERIC(18, 2),
    /** 状态：ACTIVE 正常 / FROZEN 已冻结 / USED_UP 已用尽 / EXPIRED 已过期 / REFUNDED 已退卡 */
    status          VARCHAR(16),
    issue_time      TIMESTAMP,
    expire_time     TIMESTAMP,
    remark          VARCHAR(255),
    deleted         INTEGER      NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_mkt_stored_card_no
    ON mkt_stored_card (tenant_id, card_no) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_mkt_stored_card_partner
    ON mkt_stored_card (tenant_id, partner_id);

COMMENT ON TABLE mkt_stored_card IS '储值卡/礼品卡档案（本系统建模）';
COMMENT ON COLUMN mkt_stored_card.card_type IS 'STORED 储值卡 / GIFT 礼品卡';
COMMENT ON COLUMN mkt_stored_card.status IS 'ACTIVE 正常 / FROZEN 已冻结 / USED_UP 已用尽 / EXPIRED 已过期 / REFUNDED 已退卡';

CREATE TABLE IF NOT EXISTS mkt_stored_card_flow (
    id             BIGINT PRIMARY KEY,
    tenant_id      BIGINT      NOT NULL DEFAULT 0,
    card_id        BIGINT,
    card_no        VARCHAR(64),
    partner_id     BIGINT,
    /** 流水类型：ISSUE 开卡 / RECHARGE 充值 / BONUS 赠送 / CONSUME 消费 / REFUND 退款 / ADJUST 调整 */
    flow_type      VARCHAR(16),
    /** 本金变动（正=增加，负=减少） */
    amount         NUMERIC(18, 2),
    /** 赠送金额变动（正=增加） */
    bonus_amount   NUMERIC(18, 2),
    /** 变动后余额 */
    balance_after  NUMERIC(18, 2),
    source_bill_no VARCHAR(64),
    handler_name   VARCHAR(64),
    remark         VARCHAR(255),
    deleted        INTEGER     NOT NULL DEFAULT 0,
    create_time    TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mkt_stored_card_flow_card
    ON mkt_stored_card_flow (tenant_id, card_id, create_time);

COMMENT ON TABLE mkt_stored_card_flow IS '储值卡收支流水（开卡/充值/赠送/消费/退款/调整，含变动后余额）';
