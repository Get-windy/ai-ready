-- 收款管理金标准 · 第二轮（配送 → 结算收款 → 收款管理，菜单 80920）
--
-- 背景（《收款管理开发文档》§3.1 金标准目标设计 / §6.9 遗留）：
--   ① 未付（挂账）只有一句备注 → **无催收/核销闭环**（§3.4-6）；
--   ② 支付平台流水**无处落库**、无逐笔对账（§3.4-7，`/reconcile` 未实现）；
--   ③ 与财务未打通（收款确认后不生成《收款单》/不核销应收，§3.3 `push-finance` 未实现）；
--   ④ 资金限额与交款时限（§3.4-2 资金安全刚需）无配置、无超时预警。
--
-- 本次落地（只加表/列 + 预置配置，不改菜单、不改既有语义）：
--   1) `dms_payment_flow`      支付平台流水（导入 + 逐笔对账 + 匹配结果）
--   2) `dms_payment_collection` 未付催收/承诺付款/核销流水
--   3) `dms_payment` 补财务推送 2 列（推 ERP 幂等键）
--   4) 预置资金安全 3 键（单笔/单日现金限额、交款时限）
--
-- 幂等：IF NOT EXISTS / WHERE NOT EXISTS。

-- ─────────────────────────────────────────────
-- 1) 支付平台流水（对账底账）
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS dms_payment_flow (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT,
    channel_code  VARCHAR(32),                 -- 渠道编码（WECHAT/ALIPAY/POS/BANK…）
    channel_name  VARCHAR(64),                 -- 渠道名称快照
    trade_no      VARCHAR(64),                 -- 平台交易号（对账/幂等键）
    out_trade_no  VARCHAR(64),                 -- 商户单号/外部单号
    amount        NUMERIC(18, 2),              -- 流水金额
    trade_time    TIMESTAMP,                   -- 平台成交时间
    payer         VARCHAR(100),                -- 付款人（脱敏后）
    batch_no      VARCHAR(64),                 -- 导入批次号
    match_status  INTEGER NOT NULL DEFAULT 0,  -- 0-未匹配 1-已匹配 2-差异 3-已忽略
    payment_id    BIGINT,                      -- 匹配到的收款记录
    match_type    INTEGER,                     -- 1-自动匹配 2-人工匹配
    match_time    TIMESTAMP,
    remark        VARCHAR(500),
    deleted       INTEGER DEFAULT 0,
    create_time   TIMESTAMP,
    update_time   TIMESTAMP,
    create_by     BIGINT,
    update_by     BIGINT,
    version       INTEGER DEFAULT 0
);

COMMENT ON TABLE  dms_payment_flow IS '支付平台流水（日终导入，与 dms_payment 逐笔对账）';
COMMENT ON COLUMN dms_payment_flow.match_status IS '匹配状态 0-未匹配 1-已匹配 2-差异（金额不符/系统无此笔） 3-已忽略';
COMMENT ON COLUMN dms_payment_flow.payment_id   IS '对账匹配到的收款记录 dms_payment.id';

-- 同一租户 + 渠道 + 平台交易号唯一（重复导入幂等）
CREATE UNIQUE INDEX IF NOT EXISTS uk_dms_payment_flow_trade
    ON dms_payment_flow (tenant_id, channel_code, trade_no) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_dms_payment_flow_status ON dms_payment_flow (tenant_id, match_status);
CREATE INDEX IF NOT EXISTS idx_dms_payment_flow_time   ON dms_payment_flow (tenant_id, trade_time);

-- ─────────────────────────────────────────────
-- 2) 未付催收 / 承诺付款 / 核销流水
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS dms_payment_collection (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT,
    payment_id    BIGINT NOT NULL,             -- dms_payment.id
    task_id       BIGINT,
    action_type   INTEGER NOT NULL,            -- 1-催收 2-核销 3-承诺付款
    amount        NUMERIC(18, 2),              -- 核销金额（action_type=2）
    promise_date  DATE,                        -- 承诺付款日（action_type=3）
    content       VARCHAR(500),                -- 催收/核销说明
    operator_id   BIGINT,
    operator_name VARCHAR(100),
    deleted       INTEGER DEFAULT 0,
    create_time   TIMESTAMP,
    update_time   TIMESTAMP,
    create_by     BIGINT,
    update_by     BIGINT,
    version       INTEGER DEFAULT 0
);

COMMENT ON TABLE  dms_payment_collection IS '未付（挂账）催收/承诺付款/核销流水（挂账闭环留痕）';
COMMENT ON COLUMN dms_payment_collection.action_type IS '动作 1-催收 2-核销（收款到账） 3-承诺付款';
COMMENT ON COLUMN dms_payment_collection.amount      IS '核销金额（仅 action_type=2）';

CREATE INDEX IF NOT EXISTS idx_dms_payment_collection_payment ON dms_payment_collection (tenant_id, payment_id, action_type);
CREATE INDEX IF NOT EXISTS idx_dms_payment_collection_task    ON dms_payment_collection (tenant_id, task_id);

-- ─────────────────────────────────────────────
-- 3) 财务打通（推 ERP 生成收款单/核销应收）幂等键
-- ─────────────────────────────────────────────
ALTER TABLE dms_payment ADD COLUMN IF NOT EXISTS finance_push_status INTEGER NOT NULL DEFAULT 0;
ALTER TABLE dms_payment ADD COLUMN IF NOT EXISTS finance_trace_id    VARCHAR(64);

COMMENT ON COLUMN dms_payment.finance_push_status IS '财务推送状态 0-未推送 1-已推送（幂等：同单只推一次）';
COMMENT ON COLUMN dms_payment.finance_trace_id    IS '财务推送追踪号（事件发件箱 traceId，凭证追溯）';

CREATE INDEX IF NOT EXISTS idx_dms_payment_finance ON dms_payment (tenant_id, finance_push_status);

-- ─────────────────────────────────────────────
-- 4) 资金安全规则（配置化；0 = 不限）
-- ─────────────────────────────────────────────
INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time, version)
SELECT 0, 'dms.payment.cash.limit.per.order', '0', '单笔现金收款限额（元，0=不限；超限须改用扫码/POS）', 'TENANT', 0, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM dms_config c WHERE c.config_key = 'dms.payment.cash.limit.per.order' AND c.tenant_id = 0 AND c.deleted = 0);

INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time, version)
SELECT 0, 'dms.payment.cash.limit.daily', '0', '单配送员单日现金收款限额（元，0=不限）', 'TENANT', 0, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM dms_config c WHERE c.config_key = 'dms.payment.cash.limit.daily' AND c.tenant_id = 0 AND c.deleted = 0);

INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time, version)
SELECT 0, 'dms.payment.handover.deadline.hours', '24', '交款时限（小时，自收款起算；0=不限。超时进入待上交预警）', 'TENANT', 0, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM dms_config c WHERE c.config_key = 'dms.payment.handover.deadline.hours' AND c.tenant_id = 0 AND c.deleted = 0);
