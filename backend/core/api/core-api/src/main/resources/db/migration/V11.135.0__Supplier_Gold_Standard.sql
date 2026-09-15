-- ============================================================================
-- V11.135.0 供应商（资料 → 往来单位 → 供应商）金标准升级（2026-09-11）
--
-- 背景：供应商走 biz_party.party_type=2（往来单位-供应商），对标 ql361
--      「资料 → 往来单位 → 供应商」（列表 12 列 + 新增表单 6 分区）。
--
-- 缺口（对标系统实测抓取 + 逐字段核对源码得出）：
--   1) 列表列 经营系列 / 经营面积 在 biz_party 无列 → 永远空白。
--   2) 表单分区 期初信息(期初应付/预付) / 其他信息(付款期限方式·天数·固定账期日·
--      结算期日·经营系列·经营面积) / 顶部(启用价格跟踪) 无落库列 → 录入被静默丢弃。
--   3) 「既是供应商又是客户」是往来单位的多重身份，biz_party.party_type 单值承载不了，
--      导致列表「显示客户中的供应商」无法实现。
--
-- 说明：纳税人信息段（company_full_name / address / bank_address）、助记码
--      （mnemonic_code）已由 V11.133.0（物流公司金标准）补齐；
--      联系人 / 联系电话 / 联系地址 复用既有 biz_party_contact 主联系人口径
--      （contact_name / phone / detail_address，is_primary=1），本迁移不重复定义。
--
-- 处理：新增列全部 IF NOT EXISTS + 可空/带默认值，幂等；仅新增，不改动既有列与数据。
--      roles 按既有 party_type 回填一次，保证存量记录的多重身份最小可用。
-- ============================================================================

-- ------------------------------------------------------------
-- 1. 列表列 + 表单字段落库列
-- ------------------------------------------------------------
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS opening_payable     NUMERIC(18,2) DEFAULT 0; -- 期初应付金额
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS opening_prepaid     NUMERIC(18,2) DEFAULT 0; -- 期初预付金额
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS operating_series    VARCHAR(100);  -- 经营系列
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS operating_area      NUMERIC(18,2); -- 经营面积
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS payment_term_type   VARCHAR(20)  DEFAULT 'DYNAMIC'; -- DYNAMIC 动态付款期限 / FIXED 固定账期
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS payment_days        INTEGER      DEFAULT 30; -- 动态付款期限(天)
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS fixed_payment_day   INTEGER      DEFAULT 1;  -- 固定账期日(号)
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS settlement_day      INTEGER      DEFAULT 1;  -- 结算期(号)
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS price_track_enabled INTEGER      DEFAULT 0;  -- 启用价格跟踪

COMMENT ON COLUMN biz_party.opening_payable    IS '期初信息：期初应付金额';
COMMENT ON COLUMN biz_party.opening_prepaid    IS '期初信息：期初预付金额';
COMMENT ON COLUMN biz_party.operating_series   IS '经营系列（供应商特有）';
COMMENT ON COLUMN biz_party.operating_area     IS '经营面积（供应商特有）';
COMMENT ON COLUMN biz_party.payment_term_type  IS '付款期限方式：DYNAMIC 动态付款期限 / FIXED 固定账期';
COMMENT ON COLUMN biz_party.payment_days       IS '动态付款期限(天)';
COMMENT ON COLUMN biz_party.fixed_payment_day  IS '固定账期日(号)';
COMMENT ON COLUMN biz_party.settlement_day     IS '结算期(号)';
COMMENT ON COLUMN biz_party.price_track_enabled IS '启用价格跟踪：0-否 1-是';

-- ------------------------------------------------------------
-- 2. 多重身份（既是供应商又是客户 → 列表「显示客户中的供应商」）
-- ------------------------------------------------------------
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS roles VARCHAR(100);

COMMENT ON COLUMN biz_party.roles IS '多重身份，逗号分隔：CUSTOMER/SUPPLIER/LOGISTICS/OTHER';

UPDATE biz_party SET roles = CASE party_type
    WHEN 1 THEN 'CUSTOMER'
    WHEN 2 THEN 'SUPPLIER'
    WHEN 3 THEN 'LOGISTICS'
    WHEN 4 THEN 'OTHER'
    ELSE NULL
  END
WHERE roles IS NULL OR roles = '';

CREATE INDEX IF NOT EXISTS idx_biz_party_roles ON biz_party (roles);

-- ------------------------------------------------------------
-- 3. 验证
-- ------------------------------------------------------------
DO $$
DECLARE missing TEXT;
BEGIN
    SELECT string_agg(c, ',') INTO missing
      FROM unnest(ARRAY['opening_payable','opening_prepaid',
                        'operating_series','operating_area','payment_term_type','payment_days',
                        'fixed_payment_day','settlement_day','price_track_enabled','roles']) c
     WHERE NOT EXISTS (SELECT 1 FROM information_schema.columns
                        WHERE table_name = 'biz_party' AND column_name = c);
    IF missing IS NOT NULL THEN
        RAISE EXCEPTION 'V11.135.0 biz_party 缺列: %', missing;
    END IF;
END $$;
