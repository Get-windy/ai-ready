-- 营销模块 P2：营销自动化（菜单 80303「营销自动化」，本系统建模页）
--
-- 定位：会员生命周期的**自动化触达**——在固定节点（新客首单后 / 生日 / 沉睡 / 复购周期 / 积分即将过期 /
--       会员卡到期）按规则自动执行动作（发优惠券 / 发短信 / 赠积分），并留执行台账。
-- 业界口径：有赞「营销画布」（定时 / 周期 / 行为触发 / 商品事件触发四类）、微盟营销中心、
--           SAP Emarsys Win-Back 战术、畅捷通「100+ 场景自动化」。
-- ⚠️ ql361 营销域无对应页（实测仅 17 页），本页为**本系统建模**，不编造对标列。
--
-- 复用：触达走既有 `SmsMarketingService`（含合规四件套）与 `CouponTemplateService.issue`；
--       赠积分走 `PointsLedgerService.earn`（含有效期批次）；调度由 `scheduled_task` 驱动。
CREATE TABLE IF NOT EXISTS mkt_auto_campaign (
    id               BIGINT PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL DEFAULT 0,
    name             VARCHAR(128) NOT NULL,
    /** 触发点：NEW_CUSTOMER 新客首单后 / BIRTHDAY 会员生日 / SLEEPING 沉睡未消费 /
     *         REPURCHASE 复购周期到期 / POINTS_EXPIRING 积分即将过期 / CARD_EXPIRING 会员卡到期 */
    trigger_type     VARCHAR(32),
    /** 触发参数：沉睡天数 / 生日提前天数 / 复购周期天数 / 积分到期前天数 / 卡到期前天数 */
    trigger_days     INTEGER,
    /** 动作：COUPON 发优惠券 / SMS 发短信 / POINTS 赠积分 */
    action_type      VARCHAR(16),
    coupon_template_id BIGINT,
    sms_template_id  BIGINT,
    sms_content      VARCHAR(500),
    points_value     NUMERIC(18, 2),
    /** 频控：同一会员 N 天内最多触达 M 次（0/NULL = 不限） */
    freq_days        INTEGER,
    freq_count       INTEGER,
    /** 同一会员是否只触发一次 */
    once_per_member  INTEGER,
    status           INTEGER      NOT NULL DEFAULT 1,
    last_run_time    TIMESTAMP,
    last_run_count   INTEGER,
    last_run_success INTEGER,
    remark           VARCHAR(255),
    deleted          INTEGER      NOT NULL DEFAULT 0,
    create_by        BIGINT,
    create_time      TIMESTAMP,
    update_by        BIGINT,
    update_time      TIMESTAMP
);

COMMENT ON TABLE mkt_auto_campaign IS '营销自动化规则（会员生命周期触达，本系统建模）';
COMMENT ON COLUMN mkt_auto_campaign.trigger_days IS '触发参数：沉睡天数/生日提前天数/复购周期天数/到期前天数';
COMMENT ON COLUMN mkt_auto_campaign.once_per_member IS '同一会员是否只触发一次：1 是 / 0 否';

CREATE TABLE IF NOT EXISTS mkt_auto_campaign_log (
    id            BIGINT PRIMARY KEY,
    tenant_id     BIGINT      NOT NULL DEFAULT 0,
    campaign_id   BIGINT,
    campaign_name VARCHAR(128),
    trigger_type  VARCHAR(32),
    action_type   VARCHAR(16),
    partner_id    BIGINT,
    member_name   VARCHAR(128),
    mobile        VARCHAR(32),
    /** 结果：SUCCESS 成功 / SKIPPED 跳过（频控/只一次/合规过滤）/ FAILED 失败 */
    result        VARCHAR(16),
    result_msg    VARCHAR(500),
    batch_no      VARCHAR(64),
    /** 触发依据说明（如「沉睡 95 天」「生日还有 3 天」） */
    trigger_note  VARCHAR(255),
    create_time   TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_auto_campaign_log_campaign
    ON mkt_auto_campaign_log (tenant_id, campaign_id, create_time);

COMMENT ON TABLE mkt_auto_campaign_log IS '营销自动化执行台账（一规则 × 一会员 = 一行）';
