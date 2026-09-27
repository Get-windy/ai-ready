-- =====================================================================================
-- 积分台账：补**幂等唯一键**（这是"台账为权威"能成立的前提）
--
-- 【为什么必须先补这个】行业共识（见下）是"**余额绝不落主档**，只追加台账 + 派生余额 +
--   **唯一约束做幂等**"。本仓的台账代码 `PointsLedgerServiceImpl`（earn/use/expire，
--   FIFO + 过期，实现是完整的）**一个防重键都没有**：`mkt_points_journal` 只有主键 +
--   两个普通索引 ⇒ 同一张单被重试两次就**重复入账**。所以"把权威值从 biz_party 迁到台账"
--   之前，得先让台账自己守得住 —— 否则只是把一个没有一致性保证的字段换成一张没有一致性
--   保证的表。
--
-- 【幂等键怎么定：**业务事件标识**，不是"请求标识"】
--   这一条是业界反复强调的坑：按 requestId/尝试次数做键，重试照样重复入账。
--   本仓 `PointsLedgerServiceImpl` 的粒度已实测清楚：
--     `earn(memberCardNo, partnerId, points, source, billNo)` —— **一次调用 = 一个批次 + 一条流水**
--     `use(memberCardNo, points, billNo)`                —— 一次调用 = 一条流水
--   即"某主体在某**来源单据**上的一次积分变动"就是业务事件本身
--   ⇒ 键 = `(tenant_id, partner_id, change_type, source_bill_no)`。
--
--   ⚠️ 用 `partner_id`（而不是 `member_card_no`）做键的一部分，是**必须的**：
--      台账表里 `member_card_no` 可为空，而实测 `biz_party` 里"有积分"的 32 行中
--      **28 行没有卡号**（旧模型允许"客户有积分但没卡"）。用卡号当键会有 28 行进不来。
--      `partner_id` 是**必然有**的那一维（`earn/use` 的签名里就有）。
--   ⚠️ `source_bill_no IS NOT NULL` 进谓词：`expireDue()` 写的过期流水**没有来源单据**，
--      它天然不参与"同一单据不重复入账"这条约束（也不该被它挡）。
--
-- 【顺带修一个多租户 bug】**`erp_loyalty_card.uk_loyalty_card_code` 是**全局**唯一**（只有
--   `card_code` 一列）⇒ **A 店发过的卡号会挡住 B 店**。卡号是"这家店的会员卡号"，
--   必须按租户唯一。这里换成 `(tenant_id, card_code)`，并补一条
--   `(tenant_id, program_id, partner_id)`（一个主体在一个计划里只有一张卡）。
-- =====================================================================================

-- ─────────────── 1. 台账流水：同一来源单据的同一类变动只许一条 ───────────────
CREATE UNIQUE INDEX IF NOT EXISTS uk_mkt_points_journal_event
    ON mkt_points_journal (tenant_id, partner_id, change_type, source_bill_no)
    WHERE deleted = 0 AND source_bill_no IS NOT NULL AND partner_id IS NOT NULL;

COMMENT ON INDEX uk_mkt_points_journal_event IS
    '幂等键：某主体在某来源单据上的一次积分变动只许一条（业务事件标识，不是请求标识）。'
    '⚠️ 故意排除 source_bill_no IS NULL：过期/手工调整这类没有来源单据的流水不受它约束。';

-- ─────────────── 2. 积分批次：同一次获取只许一个批次（否则 FIFO 会重复消耗）───────────────
CREATE UNIQUE INDEX IF NOT EXISTS uk_mkt_points_batch_event
    ON mkt_points_batch (tenant_id, partner_id, source_bill_no)
    WHERE deleted = 0 AND source_bill_no IS NOT NULL AND partner_id IS NOT NULL;

COMMENT ON INDEX uk_mkt_points_batch_event IS
    '幂等键：同一次获取（同一来源单据）只许一个积分批次；重试会撞它而不是多记一批。';

-- ─────────────── 3. 会员卡：卡号按租户唯一 + 一主体一计划一张卡 ───────────────
DROP INDEX IF EXISTS uk_loyalty_card_code;

CREATE UNIQUE INDEX IF NOT EXISTS uk_loyalty_card_code_tenant
    ON erp_loyalty_card (tenant_id, card_code) WHERE deleted = 0;

CREATE UNIQUE INDEX IF NOT EXISTS uk_loyalty_card_partner_program
    ON erp_loyalty_card (tenant_id, program_id, partner_id) WHERE deleted = 0;

COMMENT ON INDEX uk_loyalty_card_code_tenant IS
    '卡号在**本租户内**唯一。⚠️ 原 uk_loyalty_card_code 是全局唯一（只含 card_code），'
    '会让 A 店发过的卡号挡住 B 店 —— 卡号是"这家店的会员卡号"，必须按租户切分。';

-- ─────────────── 4. 台账两张表的主键补**序列默认值**（让 SQL 侧也能入账）───────────────
-- 为什么要它：这两张表的 id 由 Java 侧 `@TableId(type = IdType.ASSIGN_ID)` 给（雪花），
-- 所以**表上没有默认值** ⇒ 任何"从 SQL 搬数据进来"的写法都会撞
-- 「null value in column "id" ... violates not-null constraint」（V11.523.0 第一次跑就撞了这个）。
-- 起始值取 9e18 以上，与 Java 雪花 id（约 1.9e18 量级）**不会撞号** —— 与 `party_tenant`
-- 在 `V11.507.1` 的处置完全一致。对 Java 侧无影响（它自己总是给 id，默认值只是兜底）。
CREATE SEQUENCE IF NOT EXISTS seq_mkt_points_batch   START 9000000000000000001;
CREATE SEQUENCE IF NOT EXISTS seq_mkt_points_journal START 9000000000000000001;
ALTER TABLE mkt_points_batch   ALTER COLUMN id SET DEFAULT nextval('seq_mkt_points_batch');
ALTER TABLE mkt_points_journal ALTER COLUMN id SET DEFAULT nextval('seq_mkt_points_journal');

-- ─────────────── 5. 自检（只断言不变量；不断言跨表行数相等）───────────────
DO $$
DECLARE
    n_idx  INTEGER;
    n_old  INTEGER;
    n_dup  INTEGER;
    n_def  INTEGER;
BEGIN
    SELECT count(*) INTO n_idx FROM pg_indexes
     WHERE schemaname = 'public'
       AND indexname IN ('uk_mkt_points_journal_event', 'uk_mkt_points_batch_event',
                         'uk_loyalty_card_code_tenant', 'uk_loyalty_card_partner_program');
    IF n_idx <> 4 THEN
        RAISE EXCEPTION 'V11.522.0 自检失败：应有 4 个幂等/唯一索引，实际 %', n_idx;
    END IF;

    -- 两张台账表的主键必须有序列默认值（否则 V11.523.0 那种 SQL 侧入账插不进去）
    SELECT count(*) INTO n_def FROM information_schema.columns
     WHERE table_schema = 'public'
       AND ((table_name = 'mkt_points_batch'   AND column_name = 'id')
         OR (table_name = 'mkt_points_journal' AND column_name = 'id'))
       AND column_default LIKE 'nextval%';
    IF n_def <> 2 THEN
        RAISE EXCEPTION 'V11.522.0 自检失败：台账两张表的主键默认值应有 2 个，实际 %', n_def;
    END IF;

    -- 旧的全局唯一索引必须已经不在了（否则它还在跨租户挡住卡号）
    SELECT count(*) INTO n_old FROM pg_indexes
     WHERE schemaname = 'public' AND indexname = 'uk_loyalty_card_code';
    IF n_old <> 0 THEN
        RAISE EXCEPTION 'V11.522.0 自检失败：旧的全局卡号唯一索引 uk_loyalty_card_code 仍在';
    END IF;

    -- 建索引**之前**就不该有重复（建索引本身会拦，但要把结论验出来而不是假设）
    SELECT count(*) INTO n_dup FROM (
        SELECT tenant_id, partner_id, change_type, source_bill_no
          FROM mkt_points_journal
         WHERE deleted = 0 AND source_bill_no IS NOT NULL AND partner_id IS NOT NULL
         GROUP BY tenant_id, partner_id, change_type, source_bill_no HAVING count(*) > 1
    ) d;
    IF n_dup > 0 THEN
        RAISE EXCEPTION 'V11.522.0：mkt_points_journal 已有 % 组重复的业务事件（需先人工判定再重跑）', n_dup;
    END IF;

    RAISE NOTICE 'V11.522.0：积分台账幂等键就绪（流水 %，批次 %，会员卡 %）',
        (SELECT count(*) FROM mkt_points_journal WHERE deleted = 0),
        (SELECT count(*) FROM mkt_points_batch   WHERE deleted = 0),
        (SELECT count(*) FROM erp_loyalty_card   WHERE deleted = 0);
END $$;
