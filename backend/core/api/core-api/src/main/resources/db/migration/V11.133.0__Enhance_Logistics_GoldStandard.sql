-- ============================================================================
-- V11.133.0 物流公司（基础资料）金标准升级（2026-09-11）
--
-- 背景：物流公司走 biz_party.party_type=3（往来单位-物流），对标 ql361「资料→物流公司」
--      列表 6 列（编号/名称/联系人/联系电话/物流公司地址/备注）+ 新增表单
--      （编号/名称/助记码/简称/备注 + 网点子表 + 纳税人信息）。
--
-- 缺口：
--   1) 「助记码」表单有录入项但 biz_party 无对应列 —— 录入值会被静默丢弃。
--   2) 「纳税人信息」段（公司全称 / 地址 / 开户行地址）在 biz_party 无对应列，
--      客户/供应商表单同样存在该段，此前录入值一律丢弃。
--   3) 「网点」子表需要同时记录「网点名称」与「联系人」两个姓名，biz_party_contact
--      只有 contact_name 一列承载不了。
--   4) PartyContact 实体未映射既有的 detail_address 列，网点「联系地址」取不到。
--
-- 处理：全部 IF NOT EXISTS + 可空列，幂等；对客户/供应商/其他往来单位零影响
--      （不写数据、不设默认值、不加约束）。
-- ============================================================================

-- ------------------------------------------------------------
-- 1. biz_party 补「助记码」
-- ------------------------------------------------------------
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS mnemonic_code VARCHAR(64);

COMMENT ON COLUMN biz_party.mnemonic_code IS '助记码（基础资料快速检索，如物流公司/客户名称拼音首字母）';

-- ------------------------------------------------------------
-- 2. biz_party 补「纳税人信息」段（公司全称 / 地址 / 开户行地址）
-- ------------------------------------------------------------
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS company_full_name VARCHAR(200);
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS address           VARCHAR(255);
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS bank_address      VARCHAR(255);

COMMENT ON COLUMN biz_party.company_full_name IS '纳税人信息：公司全称';
COMMENT ON COLUMN biz_party.address           IS '纳税人信息：地址';
COMMENT ON COLUMN biz_party.bank_address      IS '纳税人信息：开户行地址';

-- ------------------------------------------------------------
-- 3. biz_party_contact 补「联系人」（网点：网点名称=contact_name，联系人=linkman）
-- ------------------------------------------------------------
ALTER TABLE biz_party_contact ADD COLUMN IF NOT EXISTS linkman VARCHAR(64);

COMMENT ON COLUMN biz_party_contact.linkman IS '联系人姓名（网点场景下 contact_name 存网点名称，linkman 存该网点联系人）';

-- ------------------------------------------------------------
-- 4. 验证
-- ------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'biz_party' AND column_name = 'mnemonic_code') THEN
        RAISE EXCEPTION 'biz_party.mnemonic_code 未创建成功';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'biz_party' AND column_name = 'company_full_name') THEN
        RAISE EXCEPTION 'biz_party.company_full_name 未创建成功';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'biz_party' AND column_name = 'address') THEN
        RAISE EXCEPTION 'biz_party.address 未创建成功';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'biz_party' AND column_name = 'bank_address') THEN
        RAISE EXCEPTION 'biz_party.bank_address 未创建成功';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'biz_party_contact' AND column_name = 'linkman') THEN
        RAISE EXCEPTION 'biz_party_contact.linkman 未创建成功';
    END IF;
END $$;
