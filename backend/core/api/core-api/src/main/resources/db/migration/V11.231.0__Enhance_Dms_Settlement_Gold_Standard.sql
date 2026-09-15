-- 配送结算金标准（配送 → 结算收款 → 配送结算，菜单 80910）
--
-- 背景（《配送结算开发文档》§2.3 缺陷）：
--   ① 费率**硬编码**在 SettlementService（BASE_FEE/PER_KM_RATE/TIME_SURCHARGE_RATE/URGENT_SURCHARGE）
--      —— 改价要改代码发版；
--   ② **无结算单实体**：报表是实时算的，无法锁定周期、无法对账、无法幂等推送；
--   ③ 报表口径错误：按 `status = 4`（配送中）当"已完成"统计。
--
-- 本次落地：
--   1) 计费规则**配置化**（dms_config，改价不改代码）；
--   2) 结算单 `dms_settlement` + 明细 `dms_settlement_item`（生成 → 确认 → 推 ERP 幂等）；
--   3) 结算单号 `JSD-YYYYMMDD-序号`。
--
-- 幂等：IF NOT EXISTS / WHERE NOT EXISTS。

-- ─────────────────────────────────────────────
-- 1) 计费规则（配置化，tenant_id = 0 为全局缺省，租户可覆盖）
-- ─────────────────────────────────────────────
INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time)
SELECT 0, v.k, v.val, v.descr, 'TENANT', 0, NOW(), NOW()
FROM (VALUES
    ('dms.settlement.base.fee',            '5.00',  '配送费-起步价（元）'),
    ('dms.settlement.free.distance.km',    '0',     '配送费-免费里程（公里，超出部分才计里程费）'),
    ('dms.settlement.per.km.rate',         '2.00',  '配送费-每公里单价（元/公里）'),
    ('dms.settlement.time.surcharge.rate', '1.50',  '配送费-夜间时段附加系数（22:00-06:00，按起步价倍数）'),
    ('dms.settlement.urgent.surcharge',    '10.00', '配送费-加急附加费（优先级≥2，元）')
) AS v(k, val, descr)
WHERE NOT EXISTS (
    SELECT 1 FROM dms_config c WHERE c.config_key = v.k AND c.tenant_id = 0 AND c.deleted = 0
);

-- ─────────────────────────────────────────────
-- 2) 结算单（按周期 + 结算对象锁定，可对账、可幂等推送）
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS dms_settlement (
    id             BIGSERIAL      PRIMARY KEY,
    tenant_id      BIGINT         NOT NULL DEFAULT 0,
    settlement_no  VARCHAR(64)    NOT NULL,
    -- 1-配送员（自有员工/外部运力） 2-渠道（平台结算）
    target_type    SMALLINT       NOT NULL DEFAULT 1,
    target_id      BIGINT,
    target_name    VARCHAR(100),
    period_start   DATE           NOT NULL,
    period_end     DATE           NOT NULL,
    task_count     INTEGER        NOT NULL DEFAULT 0,
    total_amount   NUMERIC(18, 2) NOT NULL DEFAULT 0,
    -- 0-草稿 1-已确认 2-已推送（已推送为终态，幂等：同单只推一次）
    status         SMALLINT       NOT NULL DEFAULT 0,
    push_time      TIMESTAMP,
    push_count     INTEGER        NOT NULL DEFAULT 0,
    push_trace_id  VARCHAR(64),
    -- 计费规则快照（生成时的费率，保证历史可回溯）
    rule_snapshot  VARCHAR(1000),
    remark         VARCHAR(500),
    deleted        INTEGER        DEFAULT 0,
    create_time    TIMESTAMP      DEFAULT NOW(),
    update_time    TIMESTAMP      DEFAULT NOW(),
    create_by      BIGINT,
    update_by      BIGINT,
    version        INTEGER        DEFAULT 0
);

COMMENT ON TABLE  dms_settlement              IS '配送结算单（按周期+结算对象锁定已签收任务，锁定金额后可对账与推送）';
COMMENT ON COLUMN dms_settlement.target_type  IS '1-配送员 2-渠道';
COMMENT ON COLUMN dms_settlement.status       IS '0-草稿 1-已确认 2-已推送（终态，推送幂等）';
COMMENT ON COLUMN dms_settlement.rule_snapshot IS '生成时的计费规则快照（费率配置化后可回溯）';
COMMENT ON COLUMN dms_settlement.push_trace_id IS '推送 ERP 的事件 traceId（幂等与对账凭据）';

CREATE INDEX IF NOT EXISTS idx_dms_settlement_tenant_no   ON dms_settlement (tenant_id, settlement_no);
CREATE INDEX IF NOT EXISTS idx_dms_settlement_tenant_time ON dms_settlement (tenant_id, period_start, period_end);
CREATE INDEX IF NOT EXISTS idx_dms_settlement_status      ON dms_settlement (tenant_id, status);

-- ─────────────────────────────────────────────
-- 3) 结算单明细（每单挂：任务 + 签收时间 + 费用构成）
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS dms_settlement_item (
    id              BIGSERIAL      PRIMARY KEY,
    tenant_id       BIGINT         NOT NULL DEFAULT 0,
    settlement_id   BIGINT         NOT NULL,
    task_id         BIGINT,
    task_no         VARCHAR(64),
    sign_time       TIMESTAMP,
    distance_km     NUMERIC(18, 4) NOT NULL DEFAULT 0,
    base_fee        NUMERIC(18, 2) NOT NULL DEFAULT 0,
    mileage_fee     NUMERIC(18, 2) NOT NULL DEFAULT 0,
    time_surcharge  NUMERIC(18, 2) NOT NULL DEFAULT 0,
    urgent_surcharge NUMERIC(18, 2) NOT NULL DEFAULT 0,
    total_fee       NUMERIC(18, 2) NOT NULL DEFAULT 0,
    deleted         INTEGER        DEFAULT 0,
    create_time     TIMESTAMP      DEFAULT NOW()
);

COMMENT ON TABLE  dms_settlement_item           IS '配送结算单明细（费用构成，用于"这 500 元由哪些单构成"的追溯）';
COMMENT ON COLUMN dms_settlement_item.sign_time IS '签收时间（计费触发点）';

CREATE INDEX IF NOT EXISTS idx_dms_settlement_item_sid  ON dms_settlement_item (tenant_id, settlement_id);
CREATE INDEX IF NOT EXISTS idx_dms_settlement_item_task ON dms_settlement_item (task_id);
