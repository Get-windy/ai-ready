-- =====================================================================================
-- 存量搬运：`biz_party.points` → 积分台账（`mkt_points_batch` + `mkt_points_journal`）
--
-- 【为什么要搬】按业界口径（"**余额绝不落主档**：可变余额字段会在重试重复发放、并发丢更新、
--   '我的积分去哪了'答不出来、出错没法安全更正这四件事上坏掉"），`biz_party.points` 是**反模式**。
--   实测：`biz_party` 里"有积分"的有 **32 行**，而台账三张表**全是 0 行** ——
--   也就是说**唯一有数据的地方，恰好是设计上最不该放的地方**。
--
-- 【搬运口径（三条，都是"不猜"）】
--   1. **金额原样搬**：`earned_points = remaining_points = b.points`，一笔一个批次。
--   2. **不设有效期**（`expire_time = NULL`）：旧模型上积分本来就没有有效期概念，
--      这里若按 `mkt_member_config.points_valid_months` 凭空设一个到期日，
--      等于**静默销毁用户已有的积分**。要设有效期必须由业务显式决定（见文末 NOTICE）。
--   3. **不给"没卡但有积分"的行编一张卡**：实测 32 行里 **28 行没有卡号**
--      （旧模型允许在客户表单上直接填积分，与卡无关）。业界做法是"积分属于**会员/卡**，
--      不属于客户主档" ⇒ 这些行按**伙伴级**入账（`partner_id` 有值、`member_card_no` 为 NULL），
--      **如实报出来**让业务补发卡，而不是替它们编一张卡。
--      ⚠️ 附带后果（必须知道）：`PointsLedgerServiceImpl.earn/use/available` 是**按卡号**工作的
--      （`earn` 第一行就 `memberCardNo == null → return null`），所以这些伙伴级积分
--      **走不了卡路径兑付**；它们进台账的意义是"**不丢、可查、可对账**"，
--      要用必须先补发卡再调整。本迁移同时补一个 `(tenant_id, partner_id)` 索引，
--      让"按伙伴求和"这个只读口径跑得动（配套的 `availableByPartner` 见同批 Java 改动）。
--
-- 【幂等】两段 INSERT 都带 `NOT EXISTS` 守卫 + 固定的 `source_bill_no = 'OPENING:V11.523.0'`，
--   即使被重复执行也不会重复入账（且 V11.522.0 已给台账加了业务事件唯一键兜底）。
-- =====================================================================================

-- 伙伴级求和的索引（台账本来是"按卡"设计的，没有这一维的索引）
CREATE INDEX IF NOT EXISTS idx_mkt_points_batch_partner
    ON mkt_points_batch (tenant_id, partner_id) WHERE deleted = 0;

-- ─────────────── 1. 批次：一行存量积分 = 一个批次 ───────────────
INSERT INTO mkt_points_batch (tenant_id, member_card_no, partner_id, earned_points, remaining_points,
                              earned_time, expire_time, source, source_bill_no, status, remark,
                              deleted, create_time, update_time)
SELECT b.tenant_id, b.member_card_no, b.id, b.points, b.points,
       COALESCE(b.update_time, b.create_time, now()),
       NULL,                       -- 不设有效期：旧模型无此概念，凭空设一个等于销毁积分
       'OPENING', 'OPENING:V11.523.0',
       'ACTIVE',
       CASE WHEN COALESCE(b.member_card_no, '') = ''
            THEN '期初迁移：由 biz_party.points 搬入（⚠️ 该主体**没有卡号** ⇒ 走不了卡路径兑付，需补发卡）'
            ELSE '期初迁移：由 biz_party.points 搬入（旧模型把积分存在主档上）' END,
       0, now(), now()
FROM biz_party b
WHERE b.deleted = 0
  AND COALESCE(b.points, 0) <> 0
  AND NOT EXISTS (SELECT 1 FROM mkt_points_batch x
                   WHERE x.tenant_id = b.tenant_id AND x.partner_id = b.id
                     AND x.source_bill_no = 'OPENING:V11.523.0' AND x.deleted = 0);

-- ─────────────── 2. 流水：每个批次配一条 OPENING 流水（币值口径与我们自己的常量一致）───────────────
INSERT INTO mkt_points_journal (tenant_id, member_card_no, partner_id, change_type, change_points,
                                balance_after, batch_id, source_bill_no, remark, deleted, create_time)
SELECT bt.tenant_id, bt.member_card_no, bt.partner_id, 'OPENING', bt.earned_points,
       bt.earned_points, bt.id, 'OPENING:V11.523.0',
       '期初迁移：由 biz_party.points 搬入（change_type=OPENING 与 PointsJournal.OPENING 常量一致）',
       0, now()
FROM mkt_points_batch bt
WHERE bt.deleted = 0
  AND bt.source_bill_no = 'OPENING:V11.523.0'
  AND NOT EXISTS (SELECT 1 FROM mkt_points_journal j
                   WHERE j.tenant_id = bt.tenant_id AND j.partner_id = bt.partner_id
                     AND j.change_type = 'OPENING' AND j.source_bill_no = 'OPENING:V11.523.0'
                     AND j.deleted = 0);

-- ─────────────── 3. 自检 ───────────────
DO $$
DECLARE
    src_sum   NUMERIC;
    dst_sum   NUMERIC;
    n_bad     INTEGER;
    n_src     INTEGER;
    n_dst     INTEGER;
    n_nocard  INTEGER;
    n_months  INTEGER;
BEGIN
    -- 3.1 **逐主体**金额一致（比总量更强：总量相等也可能"张三的搬到李四头上"）
    SELECT count(*) INTO n_bad
      FROM biz_party b
     WHERE b.deleted = 0 AND COALESCE(b.points, 0) <> 0
       AND COALESCE((SELECT sum(x.remaining_points) FROM mkt_points_batch x
                      WHERE x.tenant_id = b.tenant_id AND x.partner_id = b.id
                        AND x.source = 'OPENING' AND x.deleted = 0), 0)
           <> COALESCE(b.points, 0);
    IF n_bad > 0 THEN
        RAISE EXCEPTION 'V11.523.0 自检失败：% 个主体的台账批次金额与 biz_party.points 不一致', n_bad;
    END IF;

    -- 3.2 总量对平（迁移期快照：同一事务内比"刚搬过去的值"，不会随线上数据漂移）
    SELECT COALESCE(sum(b.points), 0), count(*) INTO src_sum, n_src
      FROM biz_party b WHERE b.deleted = 0 AND COALESCE(b.points, 0) <> 0;
    SELECT COALESCE(sum(x.remaining_points), 0), count(*) INTO dst_sum, n_dst
      FROM mkt_points_batch x WHERE x.deleted = 0 AND x.source = 'OPENING';
    IF src_sum <> dst_sum THEN
        RAISE EXCEPTION 'V11.523.0 自检失败：源合计 % ≠ 台账合计 %', src_sum, dst_sum;
    END IF;
    IF n_src <> n_dst THEN
        RAISE EXCEPTION 'V11.523.0 自检失败：源 % 行 ≠ 批次 % 个', n_src, n_dst;
    END IF;

    -- 3.3 每个批次都有流水（没流水的批次=审计链断了）
    SELECT count(*) INTO n_bad FROM mkt_points_batch x
     WHERE x.deleted = 0 AND x.source = 'OPENING'
       AND NOT EXISTS (SELECT 1 FROM mkt_points_journal j
                        WHERE j.batch_id = x.id AND j.change_type = 'OPENING' AND j.deleted = 0);
    IF n_bad > 0 THEN
        RAISE EXCEPTION 'V11.523.0 自检失败：% 个期初批次没有配套流水', n_bad;
    END IF;

    -- 3.4 **如实报出**需要业务跟进的量（这是报告，不是断言）
    SELECT count(*) INTO n_nocard FROM mkt_points_batch
     WHERE deleted = 0 AND source = 'OPENING' AND COALESCE(member_card_no, '') = '';
    SELECT COALESCE(points_valid_months, 0) INTO n_months FROM mkt_member_config WHERE deleted = 0 LIMIT 1;

    RAISE NOTICE 'V11.523.0：期初入账完成 —— 主体 % 个 / 批次 % 个 / 合计积分 %', n_src, n_dst, dst_sum;
    RAISE NOTICE 'V11.523.0 ⚠️ 待业务跟进：其中 % 个批次**没有卡号**（积分已进台账但不丢、可查；'
                 '因台账 API 按卡号工作，需先补发会员卡才能兑付）', n_nocard;
    IF n_months > 0 THEN
        RAISE NOTICE 'V11.523.0 ⚠️ 注意：mkt_member_config.points_valid_months = % 个月，'
                     '但本次期初**一律不设有效期**（旧模型无此概念，凭空设到期日等于销毁积分）。'
                     '是否给存量积分设有效期，需业务显式决定后单独处理。', n_months;
    END IF;
END $$;
