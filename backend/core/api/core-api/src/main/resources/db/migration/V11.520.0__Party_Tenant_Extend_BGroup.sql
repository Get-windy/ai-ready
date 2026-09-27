-- =====================================================================================
-- 批 2：`party_tenant` 补 **B 组**（关系与商务条件）—— 逐列归位见
--   `docs/PHASE-3-5-MIGRATION-PLAN-v1.md` §3.2b/§3.2c（批 2 的前置＝批 1 已完成、方向已定）。
--
-- 【为什么必须有这一批】`biz_party` 76 列里，读方需要的一大批还**只在旧表**上
--   （`credit_days`/`payment_days` 等正是三层结算口径「租户内 → 档案」的取值列）
--   ⇒ 不补齐，"读路径逐表切换"一步都开不了工。
--
-- 【⚠️ 本批的核心纪律：**账期不可传递**（DOMAIN-MODEL §6.1 裁定 ⑲）】
--   同一对主体可以并存两条边（SALE 我卖给他 / PURCHASE 我向他买），
--   而**商务条件分属各自方向**：把客户的账期写到供应商那边，就是 ⑲ 明令禁止的"可传递"。
--   因此下面**逐列定"归哪个方向"**，不搞"照抄到每条边"：
--
--   | 归向 | 列 | 判据（谁说的） |
--   |---|---|---|
--   | **SALE 独有** | `credit_limit` `current_debt` `credit_days` `fixed_credit_day` | §3.4.2 `credit_limit` 注释「**我给他的赊销额度**（SALE 方向才有意义）」；`PartyCreditServiceImpl.getAvailableCredit = creditLimit - currentDebt` ⇒ `current_debt` 是同一杆秤的已用量；`credit_days` = **应收**期限（`BusinessAccountingServiceImpl.resolveIntraTenantDueDate`：`receivableSide ? creditDays : paymentDays`） |
--   | **PURCHASE 独有** | `payment_days` `fixed_payment_day` | 同上，应付侧取 `payment_days`；`fixed_payment_day` 与 `payment_days` 同词根 |
--   | **逐边照抄** | 其余（含 `settlement_days`/`statement_day`/`settlement_day`/`payment_term_type`） | 源表**只有单值**且**尚无按方向消费的代码**；照抄＝不丢信息 |
--
--   ⚠️ "逐边照抄"那批里有 4 列（`settlement_days` `statement_day` `settlement_day` `payment_term_type`）
--      名字上**可能**也隐含方向，但当前没有任何消费方按方向读它们
--      ⇒ 本批按"忠实保留"处理，**记入 `PHASE-3-5-MIGRATION-PLAN-v1.md` 的"切读前必须定案"清单**，
--      不许在切读时把它们当作"方向已定"用。
--
-- 【`settlement_type` 的口径（本批定案）】
--   源 `biz_party.settlement_type` 是 `V9.14.0` 定下的**历史两值整数**（0 = 现结；非 0 = 有账期），
--   而目标列是 `VARCHAR(32)`，且域模型 §3.4.2 里标注的是**文案**「现结 / 账期 / 预付 …」。
--   ⇒ 取**本模块自己正在用的词表**（`MdCustomerController.resolveSettlementType` 类注释：
--      「前端传「挂账/现结」文案，库中存 1/0」）：`0 → '现结'`、非 0 → `'挂账'`。
--   ⚠️ 这与协议侧的 `SETTLEMENT_TYPE` 字段字典（`CASH_PREPAY`/`CASH_SPOT`/…，
--      `V11.492.0`，五值，`consumer_point = AR_DUE_DATE`）**不是同一套东西** ——
--      `PartySettlementProfile` 的类注释对此有明确警告，**不要混用**。
--      将来若统一到五项编码，这是一次**文本值重映射**（可逆，19 行级别）。
-- =====================================================================================

-- ─────────────── 1. `handler_id` 更名 ───────────────
-- 建表时叫 `handler_id`，而 B 组口径是 `default_handler_id`（与源列同名，免得两份代码各叫一个名）。
-- 该列自建表起**从未被任何代码写过**（实测全仓无消费点、表内全 NULL）⇒ 更名无风险、可逆。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'party_tenant'
                  AND column_name = 'handler_id')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                        WHERE table_schema = 'public' AND table_name = 'party_tenant'
                          AND column_name = 'default_handler_id') THEN
        ALTER TABLE party_tenant RENAME COLUMN handler_id TO default_handler_id;
    END IF;
END $$;

-- ─────────────── 2. 补列 ───────────────
-- 类型**一律对齐源列**（`biz_party`），避免双写时发生隐式窄化/截断。
ALTER TABLE party_tenant ADD COLUMN IF NOT EXISTS default_handler_name VARCHAR(100);
ALTER TABLE party_tenant ADD COLUMN IF NOT EXISTS current_debt         NUMERIC(18,2);
ALTER TABLE party_tenant ADD COLUMN IF NOT EXISTS credit_days          INTEGER;
ALTER TABLE party_tenant ADD COLUMN IF NOT EXISTS payment_days         INTEGER;
ALTER TABLE party_tenant ADD COLUMN IF NOT EXISTS payment_term_type    VARCHAR(20);
ALTER TABLE party_tenant ADD COLUMN IF NOT EXISTS fixed_credit_day     INTEGER;
ALTER TABLE party_tenant ADD COLUMN IF NOT EXISTS settlement_day       INTEGER;
ALTER TABLE party_tenant ADD COLUMN IF NOT EXISTS promoter_id          BIGINT;
ALTER TABLE party_tenant ADD COLUMN IF NOT EXISTS promoter_name        VARCHAR(50);
ALTER TABLE party_tenant ADD COLUMN IF NOT EXISTS buyer_account        VARCHAR(100);
ALTER TABLE party_tenant ADD COLUMN IF NOT EXISTS customer_source      VARCHAR(50);
ALTER TABLE party_tenant ADD COLUMN IF NOT EXISTS roles                VARCHAR(100);
ALTER TABLE party_tenant ADD COLUMN IF NOT EXISTS category_id          BIGINT;
ALTER TABLE party_tenant ADD COLUMN IF NOT EXISTS warehouse_name       VARCHAR(100);
ALTER TABLE party_tenant ADD COLUMN IF NOT EXISTS last_trade_time      TIMESTAMP;

-- `party_level` 建表时给的 32，而源列是 VARCHAR(100)：**目标比源窄**属于隐藏地雷
-- （今天值最长 6 字符，但只要有人录进第 33 个字符，双写就会报错 —— 而双写失败是不许阻断建档的）
ALTER TABLE party_tenant ALTER COLUMN party_level TYPE VARCHAR(100);

COMMENT ON COLUMN party_tenant.settlement_type IS
    '本方向的结算方式**文案**：''现结'' = 无账期；''挂账'' = 有账期。'
    '取自 biz_party.settlement_type（历史两值整数 0/非0）。'
    '⚠️ 与协议侧 SETTLEMENT_TYPE 字段字典（CASH_SPOT/CREDIT/…）不是同一套编码。';
COMMENT ON COLUMN party_tenant.credit_limit IS '我给他的**赊销额度**（SALE 方向才有意义；PURCHASE 边恒 NULL）。';
COMMENT ON COLUMN party_tenant.current_debt IS '已用赊销额度（与 credit_limit 同一杆秤；SALE 方向才有意义）。';
COMMENT ON COLUMN party_tenant.credit_days IS '**应收**账期天数（SALE 边）；PURCHASE 边恒 NULL —— 账期不可传递（裁定 ⑲）。';
COMMENT ON COLUMN party_tenant.payment_days IS '**应付**账期天数（PURCHASE 边）；SALE 边恒 NULL —— 账期不可传递（裁定 ⑲）。';

-- ─────────────── 3. 回填（存量边） ───────────────
UPDATE party_tenant pt
SET party_level          = b.party_level,
    settlement_type      = CASE WHEN b.settlement_type IS NULL THEN NULL
                                WHEN b.settlement_type = 0 THEN '现结' ELSE '挂账' END,
    settlement_days      = b.settlement_days,
    statement_day        = b.statement_day,
    settlement_day       = b.settlement_day,
    payment_term_type    = b.payment_term_type,
    -- ↓↓ 方向绑定：只有本方向的那条边才拿得到值 ↓↓
    credit_limit         = CASE WHEN pt.direction = 'SALE'     THEN b.credit_limit     END,
    current_debt         = CASE WHEN pt.direction = 'SALE'     THEN b.current_debt     END,
    credit_days          = CASE WHEN pt.direction = 'SALE'     THEN b.credit_days      END,
    fixed_credit_day     = CASE WHEN pt.direction = 'SALE'     THEN b.fixed_credit_day END,
    payment_days         = CASE WHEN pt.direction = 'PURCHASE' THEN b.payment_days     END,
    fixed_payment_day    = CASE WHEN pt.direction = 'PURCHASE' THEN b.fixed_payment_day END,
    -- ↓↓ 逐边照抄（源表单值、尚无按方向消费的代码）↓↓
    price_track_enabled  = (COALESCE(b.price_track_enabled, 0) <> 0),
    default_handler_id   = b.default_handler_id,
    default_handler_name = b.default_handler_name,
    promoter_id          = b.promoter_id,
    promoter_name        = b.promoter_name,
    buyer_account        = b.buyer_account,
    customer_source      = b.customer_source,
    roles                = b.roles,
    category_id          = b.category_id,
    warehouse_name       = b.warehouse_name,
    last_trade_time      = b.last_trade_time,
    update_time          = now()
FROM biz_party b
WHERE pt.party_id = b.id
  AND pt.tenant_id = b.tenant_id
  AND pt.deleted = 0;

-- ─────────────── 4. 自检 ───────────────
-- ⚠️ 只断言**不变量**，不断言快照（跨表行数相等这类断言在"迁移之间又有 E2E 造数"时必失败，
--    2026-09-26 批 1 已实踩，见记忆 flyway-selfcheck-no-snapshot-assert）。
DO $$
DECLARE
    n_cols   INTEGER;
    n_orphan INTEGER;
    n_mixed  INTEGER;
    n_edges  INTEGER;
    n_lv     INTEGER;
    n_roles  INTEGER;
    n_settle INTEGER;
BEGIN
    -- 4.1 列齐备：本批新增/更名后应存在 15 个 B 组列 + 更名后的 default_handler_id
    SELECT count(*) INTO n_cols FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'party_tenant'
       AND column_name IN ('default_handler_id', 'default_handler_name', 'current_debt',
                           'credit_days', 'payment_days', 'payment_term_type', 'fixed_credit_day',
                           'settlement_day', 'promoter_id', 'promoter_name', 'buyer_account',
                           'customer_source', 'roles', 'category_id', 'warehouse_name',
                           'last_trade_time');
    IF n_cols <> 16 THEN
        RAISE EXCEPTION '批 2 自检失败：B 组列应有 16 个，实际 %', n_cols;
    END IF;

    -- 4.2 不悬空：边必须指向存在的主档
    SELECT count(*) INTO n_orphan FROM party_tenant pt
     WHERE pt.deleted = 0 AND NOT EXISTS (SELECT 1 FROM party p WHERE p.id = pt.party_id);
    IF n_orphan > 0 THEN
        RAISE EXCEPTION '批 2 自检失败：party_tenant 有 % 条边指向不存在的 party', n_orphan;
    END IF;

    -- 4.3 方向纯度（⑲ 的机检）：应付列不得出现在 SALE 边、应收列不得出现在 PURCHASE 边
    SELECT count(*) INTO n_mixed FROM party_tenant
     WHERE deleted = 0
       AND ((direction = 'SALE'
             AND (payment_days IS NOT NULL OR fixed_payment_day IS NOT NULL))
         OR (direction = 'PURCHASE'
             AND (credit_limit IS NOT NULL OR current_debt IS NOT NULL
                  OR credit_days IS NOT NULL OR fixed_credit_day IS NOT NULL)));
    IF n_mixed > 0 THEN
        RAISE EXCEPTION '批 2 自检失败：有 % 条边把"另一方向"的商务条件抄了过来（违反裁定 ⑲）', n_mixed;
    END IF;

    -- 4.4 回填覆盖情况 —— **报告，不是断言**（存量边少、源列多为空，断言会变成假门槛）
    SELECT count(*), count(party_level), count(roles), count(settlement_type)
      INTO n_edges, n_lv, n_roles, n_settle
      FROM party_tenant WHERE deleted = 0;
    RAISE NOTICE '批 2 回填：边 % 条；其中 party_level 有值 %、roles 有值 %、settlement_type 有值 %',
        n_edges, n_lv, n_roles, n_settle;
END $$;
