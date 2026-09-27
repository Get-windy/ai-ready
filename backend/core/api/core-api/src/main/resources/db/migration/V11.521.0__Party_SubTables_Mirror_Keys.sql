-- =====================================================================================
-- 批 2b：给 `party_cert` / `party_bank` / `party_address` 补**幂等唯一键**，
--   让双写能把它们也镜像过来。
--
-- 【为什么非补不可】这三张子表在批 1（`V11.506.0`）就已建好并回填，但**双写没有覆盖**它们：
--   客户表单里 `business_license` / `tax_number` / `bank_name` / `bank_account` /
--   `address` 全是可编辑项 ⇒ 并存期只要有人填一次**银行账号**，`party_bank` 就永远缺这一行，
--   而**读路径一旦切过去就等于丢数据**（这正是"配了不生效/接了没走通"那一类历史包袱）。
--
-- 【为什么必须先加索引】三张子表建的时候只有 `IDENTITY` 主键 + 普通索引
--   （`idx_party_*_party`），**没有可用于 `ON CONFLICT` 的键**；而双写要的语义是
--   幂等 upsert（"已有就刷、没有就插"）。⇒ 先补**部分唯一索引**，再写 upsert。
--
-- 【键怎么定：与批 1 的回填口径一一对应，不许另起一套】
--   批 1 的回填规则（`V11.506.0` §5.2~5.4）是：
--     证件：执照(`BUSINESS_LICENSE`)与税务登记(`TAX`)各一条，**只对有值的建行**；
--     银行：`biz_party` 上的那一个账户建一条并标 `is_default = 1`；
--     地址：`biz_party` 上的地址落成"注册地址"(`address_type = 1`)一条。
--   所以幂等键就是：
--     `party_cert`    → `(party_id, cert_type)`
--     `party_bank`    → `(party_id)` **限默认账户**（源表只有一个账户，它天然就是默认的那个）
--     `party_address` → `(party_id, address_type)`
--   ⇒ 双写与回填两条路落出来的行形状因此**完全一致**（否则切读时会出现"同一主体两种形状"）。
--
-- ⚠️ 三张表当前**都是 0 行**（实测 2026-09-27：源列执照/税号/银行/地址在 21 行里全为空），
--    所以加唯一索引不会撞存量重复 —— 但仍写自检把它验证出来，不靠"应该没有"。
-- =====================================================================================

-- `party_cert`：一个主体在同一 cert_type 上只留一条未删的行
CREATE UNIQUE INDEX IF NOT EXISTS uk_party_cert_type
    ON party_cert (party_id, cert_type) WHERE deleted = 0;

-- `party_bank`：一个主体只留一条未删的**默认**账户
-- （非默认账户不设限，将来"多账户"功能进来时不会被这条索引挡住）
CREATE UNIQUE INDEX IF NOT EXISTS uk_party_bank_default
    ON party_bank (party_id) WHERE deleted = 0 AND is_default = 1;

-- `party_address`：一个主体在同一 address_type 上只留一条未删的行
CREATE UNIQUE INDEX IF NOT EXISTS uk_party_address_type
    ON party_address (party_id, address_type) WHERE deleted = 0;

COMMENT ON INDEX uk_party_cert_type IS
    '双写/回填的幂等键：一个主体在同一 cert_type（BUSINESS_LICENSE/TAX/…）上一条未删行。';
COMMENT ON INDEX uk_party_bank_default IS
    '双写/回填的幂等键：一个主体一条未删的**默认**银行账户（对应 biz_party 上那唯一一个账户）。';
COMMENT ON INDEX uk_party_address_type IS
    '双写/回填的幂等键：一个主体在同一 address_type（1 注册/2 营业/3 收货/4 发货）上一条未删行。';

-- ─────────────── 自检 ───────────────
-- ⚠️ 只断言不变量（索引存在 / 无重复 / 子表不悬空），不写跨表行数相等那类快照断言
--    （2026-09-26 批 1 已因此恒失败过，见记忆 flyway-selfcheck-no-snapshot-assert）。
DO $$
DECLARE
    n_idx    INTEGER;
    n_dup    INTEGER;
    n_orphan INTEGER;
BEGIN
    SELECT count(*) INTO n_idx FROM pg_indexes
     WHERE schemaname = 'public'
       AND indexname IN ('uk_party_cert_type', 'uk_party_bank_default', 'uk_party_address_type');
    IF n_idx <> 3 THEN
        RAISE EXCEPTION '批 2b 自检失败：三个幂等唯一索引应都在，实际 %', n_idx;
    END IF;

    -- 存量重复（加索引前为空表，但把结论验出来而不是假设）
    -- ⚠️ UNION 的每一支列类型必须一致：`cert_type` 是 varchar，所以下面那两个分支
    --    不能直接写整数/整型列字面量（首次写就撞了 "UNION 的类型 character varying 和 integer 不匹配"）
    SELECT count(*) INTO n_dup FROM (
        SELECT party_id, cert_type FROM party_cert WHERE deleted = 0
         GROUP BY party_id, cert_type HAVING count(*) > 1
        UNION ALL
        SELECT party_id, 'DEFAULT' FROM party_bank WHERE deleted = 0 AND is_default = 1
         GROUP BY party_id HAVING count(*) > 1
        UNION ALL
        SELECT party_id, address_type::text FROM party_address WHERE deleted = 0
         GROUP BY party_id, address_type HAVING count(*) > 1
    ) d;
    IF n_dup > 0 THEN
        RAISE EXCEPTION '批 2b 自检失败：子表存在 % 组重复键，幂等索引建不起来', n_dup;
    END IF;

    -- 子表不悬空（三张表一起看）
    SELECT count(*) INTO n_orphan FROM (
        SELECT c.party_id FROM party_cert c WHERE c.deleted = 0
         AND NOT EXISTS (SELECT 1 FROM party p WHERE p.id = c.party_id)
        UNION ALL
        SELECT k.party_id FROM party_bank k WHERE k.deleted = 0
         AND NOT EXISTS (SELECT 1 FROM party p WHERE p.id = k.party_id)
        UNION ALL
        SELECT a.party_id FROM party_address a WHERE a.deleted = 0
         AND NOT EXISTS (SELECT 1 FROM party p WHERE p.id = a.party_id)
    ) o;
    IF n_orphan > 0 THEN
        RAISE EXCEPTION '批 2b 自检失败：子表有 % 行指向不存在的 party', n_orphan;
    END IF;

    RAISE NOTICE '批 2b：三个幂等唯一索引已就绪（子表当前 cert % / bank % / address % 行）',
        (SELECT count(*) FROM party_cert WHERE deleted = 0),
        (SELECT count(*) FROM party_bank WHERE deleted = 0),
        (SELECT count(*) FROM party_address WHERE deleted = 0);
END $$;
