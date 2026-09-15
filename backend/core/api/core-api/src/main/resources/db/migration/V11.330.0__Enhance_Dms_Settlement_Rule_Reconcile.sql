-- 配送结算金标准（第二轮 · 配送 → 结算收款 → 配送结算，菜单 80910）
--
-- 背景（《配送结算开发文档》§3.1/§3.2/§3.4/§3.5 与 §6.7 遗留）：
--   ① 计费规则仍是「5 个全局配置键」，缺 §3.1 要求的**规则模型**（按渠道/线路、计价方式、
--      生效期、结算周期、优先级）——不同运力/线路不能差异化计价；
--   ② 结算对象只有「配送员」，`target_type=2 渠道` 预留但生成逻辑未实现（§6.7 遗留 3）；
--   ③ 部分签收仍按全量计费（§3.5-3：应按实际签收数量计费，否则多算）；
--   ④ 推送 ERP 只写 outbox，**未真实生成凭证/应付**（§2.3 缺陷 2）；
--   ⑤ 无对账能力（§2.3 缺陷 5、§3.2 对账环节）。
--
-- 本次落地：
--   1) 新建计费规则表 `dms_settlement_rule`（规则 CRUD + 命中即用，未命中回落全局缺省费率）；
--   2) `dms_settlement` 补 ERP 记账回执列（凭证号/应付单号/应付ID）与命中规则ID；
--   3) `dms_settlement_item` 补签收口径列（签收类型/应签收/实签收/计费系数），支撑部分签收折算与追溯；
--   4) 配送费记账科目可配置（`dms.settlement.account.*`，默认 6602 / 2202 / 2241）。
--
-- 幂等：IF NOT EXISTS / 列级 ADD COLUMN IF NOT EXISTS / WHERE NOT EXISTS。

-- ─────────────────────────────────────────────
-- 0) 记账科目配置（全局缺省，租户可覆盖）
--    借：管理费用-配送费（6602）   贷：应付账款（2202，外部运力）/ 其他应付款（2241，自有配送员）
-- ─────────────────────────────────────────────
INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time)
SELECT 0, v.k, v.val, v.descr, 'TENANT', 0, NOW(), NOW()
FROM (VALUES
    ('dms.settlement.account.debit.subject',  '6602', '配送结算-借方科目（配送成本/管理费用）'),
    ('dms.settlement.account.credit.subject', '2202', '配送结算-贷方科目（外部运力：应付账款）'),
    ('dms.settlement.account.staff.subject',  '2241', '配送结算-贷方科目（自有配送员：其他应付款）')
) AS v(k, val, descr)
WHERE NOT EXISTS (
    SELECT 1 FROM dms_config c WHERE c.config_key = v.k AND c.tenant_id = 0 AND c.deleted = 0
);

-- ─────────────────────────────────────────────
-- 1) 计费规则表（按 结算对象 + 渠道/线路 + 生效期 差异化计价）
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS dms_settlement_rule (
    id                  BIGSERIAL      PRIMARY KEY,
    tenant_id           BIGINT         NOT NULL DEFAULT 0,
    rule_code           VARCHAR(64)    NOT NULL,
    rule_name           VARCHAR(100)   NOT NULL,
    -- 适用结算对象：1-配送员（自有/外部个人） 2-渠道（平台/外部运力）
    target_type         SMALLINT       NOT NULL DEFAULT 1,
    -- 适用范围（NULL = 不限）：渠道ID（dms_channel.id）/ 线路ID（erp_route.id）
    channel_id          BIGINT,
    route_id            BIGINT,
    -- 计价方式：1-按单（固定） 2-按距离 3-按重量 4-组合（距离+重量）
    billing_type        SMALLINT       NOT NULL DEFAULT 2,
    base_fee            NUMERIC(18, 2) NOT NULL DEFAULT 0,
    free_distance_km    NUMERIC(18, 2) NOT NULL DEFAULT 0,
    per_km_rate         NUMERIC(18, 2) NOT NULL DEFAULT 0,
    per_kg_rate         NUMERIC(18, 2) NOT NULL DEFAULT 0,
    -- 夜间时段附加系数（22:00-06:00，按起步价倍数）
    time_surcharge_rate NUMERIC(18, 4) NOT NULL DEFAULT 0,
    urgent_surcharge    NUMERIC(18, 2) NOT NULL DEFAULT 0,
    -- 结算周期：1-日结 2-周结 3-月结
    settle_cycle        SMALLINT       NOT NULL DEFAULT 1,
    effective_start     DATE,
    effective_end       DATE,
    -- 优先级（数字越小越优先；同优先级下指定渠道/线路的规则更具体者优先）
    priority            INTEGER        NOT NULL DEFAULT 100,
    -- 0-停用 1-启用
    status              SMALLINT       NOT NULL DEFAULT 1,
    remark              VARCHAR(500),
    deleted             INTEGER        DEFAULT 0,
    create_time         TIMESTAMP      DEFAULT NOW(),
    update_time         TIMESTAMP      DEFAULT NOW(),
    create_by           BIGINT,
    update_by           BIGINT,
    version             INTEGER        DEFAULT 0
);

COMMENT ON TABLE  dms_settlement_rule                 IS '配送计费规则（按结算对象+渠道/线路+生效期差异化计价；未命中规则时回落配送参数 dms.settlement.* 缺省费率）';
COMMENT ON COLUMN dms_settlement_rule.target_type     IS '适用结算对象：1-配送员 2-渠道';
COMMENT ON COLUMN dms_settlement_rule.channel_id      IS '适用渠道ID（dms_channel.id，NULL=不限）';
COMMENT ON COLUMN dms_settlement_rule.route_id        IS '适用线路ID（erp_route.id，NULL=不限）';
COMMENT ON COLUMN dms_settlement_rule.billing_type    IS '计价方式：1-按单 2-按距离 3-按重量 4-组合';
COMMENT ON COLUMN dms_settlement_rule.settle_cycle    IS '结算周期：1-日结 2-周结 3-月结';
COMMENT ON COLUMN dms_settlement_rule.priority        IS '优先级（越小越优先）';

CREATE INDEX IF NOT EXISTS idx_dms_settlement_rule_tenant  ON dms_settlement_rule (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_dms_settlement_rule_match   ON dms_settlement_rule (tenant_id, target_type, channel_id, route_id);
-- 规则编码租户内唯一（软删除可复用）
CREATE UNIQUE INDEX IF NOT EXISTS uk_dms_settlement_rule_code ON dms_settlement_rule (tenant_id, rule_code) WHERE deleted = 0;

-- ─────────────────────────────────────────────
-- 2) 结算单：补 ERP 记账回执 + 命中规则
-- ─────────────────────────────────────────────
ALTER TABLE dms_settlement ADD COLUMN IF NOT EXISTS rule_id        BIGINT;
ALTER TABLE dms_settlement ADD COLUMN IF NOT EXISTS erp_voucher_no  VARCHAR(64);
ALTER TABLE dms_settlement ADD COLUMN IF NOT EXISTS erp_payable_id  BIGINT;
ALTER TABLE dms_settlement ADD COLUMN IF NOT EXISTS erp_payable_no  VARCHAR(64);

COMMENT ON COLUMN dms_settlement.rule_id       IS '生成时命中的计费规则ID（NULL=用全局缺省费率）';
COMMENT ON COLUMN dms_settlement.erp_voucher_no IS '推送 ERP 生成的记账凭证号（KJPZ-…，幂等凭据）';
COMMENT ON COLUMN dms_settlement.erp_payable_id IS '推送 ERP 生成的应付单ID（外部运力/渠道结算；对账查核销状态用）';
COMMENT ON COLUMN dms_settlement.erp_payable_no IS '推送 ERP 生成的应付单号';

-- ─────────────────────────────────────────────
-- 3) 结算明细：补签收口径（部分签收按实际数量计费）
-- ─────────────────────────────────────────────
ALTER TABLE dms_settlement_item ADD COLUMN IF NOT EXISTS sign_type        SMALLINT;
ALTER TABLE dms_settlement_item ADD COLUMN IF NOT EXISTS planned_quantity  NUMERIC(18, 4);
ALTER TABLE dms_settlement_item ADD COLUMN IF NOT EXISTS actual_quantity   NUMERIC(18, 4);
ALTER TABLE dms_settlement_item ADD COLUMN IF NOT EXISTS billing_ratio     NUMERIC(18, 4) NOT NULL DEFAULT 1;
ALTER TABLE dms_settlement_item ADD COLUMN IF NOT EXISTS weight_fee        NUMERIC(18, 2) NOT NULL DEFAULT 0;

COMMENT ON COLUMN dms_settlement_item.sign_type       IS '签收类型：1-正常 2-部分签收 3-拒收（拒收不计费）';
COMMENT ON COLUMN dms_settlement_item.billing_ratio   IS '计费系数（部分签收 = 实签收/应签收，作用于里程费/重量费/夜间/加急；起步价全收）';
COMMENT ON COLUMN dms_settlement_item.weight_fee      IS '重量费（按重量/组合计价时的每公斤费用）';

CREATE INDEX IF NOT EXISTS idx_dms_settlement_erp_voucher ON dms_settlement (tenant_id, erp_voucher_no);
