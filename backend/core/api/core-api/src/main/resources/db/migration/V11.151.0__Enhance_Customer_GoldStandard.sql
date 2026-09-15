-- ============================================================================
-- V11.151.0 客户（资料 → 往来单位 → 客户）金标准升级（2026-09-11）
--
-- 背景：客户走 biz_party.party_type=1（往来单位-客户），对标 ql361
--      「资料 → 往来单位 → 客户」（5 子标签：全部客户 / 会员管理 / 全部联系人 /
--      客户级别 / 区域管理）。抓取实据见 tool-results/ql361/customer-deep/。
--
-- 缺口（对标系统实测抓取 + 逐字段核对源码得出）：
--   1) 「全部客户」26 列中 所属仓库/所属区域/推广人/买家账号/客户一票通/
--      动态收款期限(天)/固定账期/结算期/客户来源/营业执照有效期/最近交易时间
--      在 biz_party 无落库列 → 永远空白。
--   2) 「会员管理」子标签（10 列）缺 会员名称/会员级别/会员卡状态/有效时间/
--      累计消费额/发卡时间/初始积分 落库列 → 会员档案无法维护。
--   3) 「全部联系人」子标签（8 列）缺 性别/客户所属区域/配送方式/物流公司 列。
--   4) 「区域管理」子标签（区域编号/区域名称/备注 + 层级）无表。
--
-- 说明：客户级别（客户级别 / 级别默认价）复用既有 erp_customer_level；
--      结款方式（settlement_type）、级别（party_level）、开户银行/账号、
--      税号、助记码、默认经手人、联系人主记录口径均已存在，本迁移不重复定义。
--
-- 处理：新增列全部 IF NOT EXISTS + 可空/带默认值，幂等；仅新增，不改动既有列与数据。
-- ============================================================================

-- ------------------------------------------------------------
-- 1. 「全部客户」列表列 + 表单「其他信息/期初信息」落库列
-- ------------------------------------------------------------
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS warehouse_name          VARCHAR(100); -- 所属仓库
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS region                  VARCHAR(100); -- 所属区域
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS promoter_id             BIGINT;       -- 推广人ID
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS promoter_name           VARCHAR(50);  -- 推广人
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS buyer_account           VARCHAR(100); -- 买家账号（商城账号）
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS customer_one_pass       VARCHAR(50);  -- 客户一票通
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS customer_source         VARCHAR(50);  -- 客户来源
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS business_license_expiry DATE;         -- 营业执照有效期
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS last_trade_time         TIMESTAMP;    -- 最近交易时间
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS credit_days             INTEGER DEFAULT 0; -- 动态收款期限（天）
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS fixed_credit_day        INTEGER DEFAULT 1; -- 固定账期（号）
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS statement_day           INTEGER DEFAULT 1; -- 结算期（号）
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS opening_receivable      NUMERIC(18,2) DEFAULT 0; -- 期初应收金额
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS opening_pre_received    NUMERIC(18,2) DEFAULT 0; -- 期初预收金额

COMMENT ON COLUMN biz_party.warehouse_name          IS '所属仓库（客户列表/表单）';
COMMENT ON COLUMN biz_party.region                  IS '所属区域（客户列表/表单）';
COMMENT ON COLUMN biz_party.promoter_id             IS '推广人ID';
COMMENT ON COLUMN biz_party.promoter_name           IS '推广人';
COMMENT ON COLUMN biz_party.buyer_account           IS '买家账号（商城账号）';
COMMENT ON COLUMN biz_party.customer_one_pass       IS '客户一票通';
COMMENT ON COLUMN biz_party.customer_source         IS '客户来源';
COMMENT ON COLUMN biz_party.business_license_expiry IS '营业执照有效期';
COMMENT ON COLUMN biz_party.last_trade_time         IS '最近交易时间';
COMMENT ON COLUMN biz_party.credit_days             IS '动态收款期限（天）';
COMMENT ON COLUMN biz_party.fixed_credit_day        IS '固定账期（号）';
COMMENT ON COLUMN biz_party.statement_day           IS '结算期（号）';
COMMENT ON COLUMN biz_party.opening_receivable      IS '期初信息：期初应收金额';
COMMENT ON COLUMN biz_party.opening_pre_received    IS '期初信息：期初预收金额';

-- ------------------------------------------------------------
-- 2. 「会员管理」子标签（10 列）会员档案列
-- ------------------------------------------------------------
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS member_name          VARCHAR(100); -- 会员名称
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS member_level         VARCHAR(50);  -- 会员级别
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS member_card_status   VARCHAR(20) DEFAULT 'NORMAL'; -- 会员卡状态：NORMAL 正常 / STOPPED 停用 / EXPIRED 已过期
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS member_valid_start   DATE;         -- 会员卡有效期（起）
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS member_valid_end     DATE;         -- 会员卡有效期（止）
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS member_total_consume NUMERIC(18,2) DEFAULT 0; -- 累计消费额
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS member_issue_time    TIMESTAMP;    -- 发卡时间
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS member_initial_points INTEGER DEFAULT 0; -- 初始积分

COMMENT ON COLUMN biz_party.member_name           IS '会员信息：会员名称';
COMMENT ON COLUMN biz_party.member_level          IS '会员信息：会员级别';
COMMENT ON COLUMN biz_party.member_card_status    IS '会员信息：会员卡状态 NORMAL/STOPPED/EXPIRED';
COMMENT ON COLUMN biz_party.member_valid_start    IS '会员信息：会员卡有效期（起）';
COMMENT ON COLUMN biz_party.member_valid_end      IS '会员信息：会员卡有效期（止）';
COMMENT ON COLUMN biz_party.member_total_consume  IS '会员信息：累计消费额';
COMMENT ON COLUMN biz_party.member_issue_time     IS '会员信息：发卡时间';
COMMENT ON COLUMN biz_party.member_initial_points IS '会员信息：初始积分';

CREATE INDEX IF NOT EXISTS idx_biz_party_member_card ON biz_party (member_card_no);
CREATE INDEX IF NOT EXISTS idx_biz_party_last_trade  ON biz_party (last_trade_time);

-- ------------------------------------------------------------
-- 3. 「全部联系人」子标签列（性别 / 客户所属区域 / 配送方式 / 物流公司）
-- ------------------------------------------------------------
ALTER TABLE biz_party_contact ADD COLUMN IF NOT EXISTS gender            VARCHAR(10);  -- 性别
ALTER TABLE biz_party_contact ADD COLUMN IF NOT EXISTS region            VARCHAR(100); -- 客户所属区域
ALTER TABLE biz_party_contact ADD COLUMN IF NOT EXISTS delivery_method   VARCHAR(50);  -- 配送方式
ALTER TABLE biz_party_contact ADD COLUMN IF NOT EXISTS delivery_route    VARCHAR(100); -- 配送线路
ALTER TABLE biz_party_contact ADD COLUMN IF NOT EXISTS logistics_company VARCHAR(100); -- 物流公司

COMMENT ON COLUMN biz_party_contact.gender            IS '联系人：性别';
COMMENT ON COLUMN biz_party_contact.region            IS '联系人：客户所属区域';
COMMENT ON COLUMN biz_party_contact.delivery_method   IS '联系人：配送方式';
COMMENT ON COLUMN biz_party_contact.delivery_route    IS '联系人：配送线路';
COMMENT ON COLUMN biz_party_contact.logistics_company IS '联系人：物流公司';

-- ------------------------------------------------------------
-- 4. 「区域管理」子标签（区域编号 / 区域名称 / 备注，树形层级）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS erp_customer_region (
    id           BIGINT       PRIMARY KEY,
    tenant_id    BIGINT,
    region_code  VARCHAR(50)  NOT NULL,            -- 区域编号
    region_name  VARCHAR(100) NOT NULL,            -- 区域名称
    parent_id    BIGINT,                           -- 上级区域
    region_level INTEGER      DEFAULT 1,           -- 层级
    sort_order   INTEGER      DEFAULT 0,           -- 排序
    status       INTEGER      DEFAULT 1,           -- 1 启用 / 0 停用
    remark       VARCHAR(500),                     -- 备注
    create_by    BIGINT,
    create_time  TIMESTAMP    DEFAULT now(),
    update_by    BIGINT,
    update_time  TIMESTAMP    DEFAULT now(),
    deleted      INTEGER      DEFAULT 0
);

COMMENT ON TABLE  erp_customer_region IS '客户区域管理（资料 → 往来单位 → 客户 → 区域管理）';
COMMENT ON COLUMN erp_customer_region.region_code IS '区域编号';
COMMENT ON COLUMN erp_customer_region.region_name IS '区域名称';

CREATE INDEX IF NOT EXISTS idx_erp_customer_region_parent  ON erp_customer_region (parent_id);
CREATE INDEX IF NOT EXISTS idx_erp_customer_region_deleted ON erp_customer_region (deleted);

-- 商品资料/客户分类已有数据时，区域管理表以「默认区域」为种子，保证列表非空可操作
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM erp_customer_region WHERE deleted = 0) THEN
        INSERT INTO erp_customer_region (id, tenant_id, region_code, region_name, region_level, sort_order, status, remark)
        VALUES (1, 1, 'QY001', '默认区域', 1, 1, 1, '系统预置区域');
    END IF;
END $$;

-- ------------------------------------------------------------
-- 5. 验证
-- ------------------------------------------------------------
DO $$
DECLARE missing TEXT;
BEGIN
    SELECT string_agg(c, ',') INTO missing
      FROM unnest(ARRAY['warehouse_name','region','promoter_name','buyer_account','customer_one_pass',
                        'customer_source','business_license_expiry','last_trade_time','credit_days',
                        'fixed_credit_day','statement_day','opening_receivable','opening_pre_received',
                        'member_name','member_level','member_card_status','member_valid_start',
                        'member_valid_end','member_total_consume','member_issue_time','member_initial_points'])
           c
     WHERE NOT EXISTS (SELECT 1 FROM information_schema.columns
                        WHERE table_name = 'biz_party' AND column_name = c);
    IF missing IS NOT NULL THEN
        RAISE EXCEPTION 'V11.151.0 biz_party 缺列: %', missing;
    END IF;

    SELECT string_agg(c, ',') INTO missing
      FROM unnest(ARRAY['gender','region','delivery_method','delivery_route','logistics_company']) c
     WHERE NOT EXISTS (SELECT 1 FROM information_schema.columns
                        WHERE table_name = 'biz_party_contact' AND column_name = c);
    IF missing IS NOT NULL THEN
        RAISE EXCEPTION 'V11.151.0 biz_party_contact 缺列: %', missing;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'erp_customer_region') THEN
        RAISE EXCEPTION 'V11.151.0 缺表 erp_customer_region';
    END IF;
END $$;
