-- ============================================================================
-- V11.138.0 银行账户（资料 → 财务账户 → 银行账户）金标准升级（2026-09-11）
--
-- 背景：菜单 70540 银行账户（md:bank-account）此前为占位桩页，前端直连
--      /md/bank-account/page，后端无 Controller（404）。
--
-- 口径（严格遵循《银行账户开发文档》最终裁决）：
--   · 银行账户 = 资金账户主数据，**单一口径复用 finance_account**（与《支付账户》同源），
--     严禁另建重复银行账户表；收付款/对账/资金余额统一引用本表。
--   · 对标 ql361「资料 → 财务账户 → 银行账户」：以会计科目为载体
--     （科目编号/科目名称）+ 账户类型 + 是否用于商城线下转账收款。
--
-- 处理：finance_account 补 9 列（新增列全部可空/带默认，存量行为零影响）
--   1) subject_code         资金账户即科目，与 finance_account_subject.subject_code 对齐（科目编号）
--   2) parent_id            上级账户（「显示层次结构」树形展示依据）
--   3) mall_transfer_enabled 是否用于商城线下转账收款（0-否 1-是）
--   4) easy_code            助记码（对标表单字段 easycode）
--   5) brief_name           银行简称（对标表单字段 briefname）
--   6) account_holder       户主名（对标表单字段 bankaccountname）
--   7) qrcode_url           收款码（对标表单「二维码/上传收款码」）
--   8) is_system            系统预置（1-预置，禁止改名/删除）
--   9) sort_no              同级排序
--
-- 存量数据回填：按账户类型映射到资金科目编号（银行→1002 银行存款，现金→1001 库存现金），
--   内部/外部账户非资金科目，留空。幂等（IF NOT EXISTS / WHERE 守卫）。
-- ============================================================================

-- ------------------------------------------------------------
-- 1. 补列
-- ------------------------------------------------------------
ALTER TABLE finance_account ADD COLUMN IF NOT EXISTS subject_code          VARCHAR(32);
ALTER TABLE finance_account ADD COLUMN IF NOT EXISTS parent_id             BIGINT;
ALTER TABLE finance_account ADD COLUMN IF NOT EXISTS mall_transfer_enabled SMALLINT DEFAULT 0;
ALTER TABLE finance_account ADD COLUMN IF NOT EXISTS easy_code             VARCHAR(32);
ALTER TABLE finance_account ADD COLUMN IF NOT EXISTS brief_name            VARCHAR(64);
ALTER TABLE finance_account ADD COLUMN IF NOT EXISTS account_holder        VARCHAR(64);
ALTER TABLE finance_account ADD COLUMN IF NOT EXISTS qrcode_url            VARCHAR(500);
ALTER TABLE finance_account ADD COLUMN IF NOT EXISTS is_system             SMALLINT DEFAULT 0;
ALTER TABLE finance_account ADD COLUMN IF NOT EXISTS sort_no               INT DEFAULT 0;

-- ------------------------------------------------------------
-- 2. 存量回填：账户类型 → 资金科目编号（仅回填空值，不覆盖已维护数据）
--    账户类型：1-银行账户 2-现金账户 3-内部账户 4-外部账户
-- ------------------------------------------------------------
UPDATE finance_account
SET subject_code = CASE account_type WHEN 1 THEN '1002' WHEN 2 THEN '1001' ELSE NULL END
WHERE subject_code IS NULL
  AND account_type IN (1, 2);

UPDATE finance_account SET mall_transfer_enabled = 0 WHERE mall_transfer_enabled IS NULL;
UPDATE finance_account SET is_system = 0 WHERE is_system IS NULL;
UPDATE finance_account SET sort_no = id WHERE sort_no IS NULL;

-- ------------------------------------------------------------
-- 3. 索引（层次结构树查询 / 科目编号检索）
-- ------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_finance_account_parent  ON finance_account (tenant_id, parent_id);
CREATE INDEX IF NOT EXISTS idx_finance_account_subject ON finance_account (tenant_id, subject_code);

-- ------------------------------------------------------------
-- 4. 验证
-- ------------------------------------------------------------
DO $$
DECLARE
    col_cnt int;
BEGIN
    SELECT count(*) INTO col_cnt
    FROM information_schema.columns
    WHERE table_name = 'finance_account'
      AND column_name IN ('subject_code','parent_id','mall_transfer_enabled','easy_code',
                          'brief_name','account_holder','qrcode_url','is_system','sort_no');
    IF col_cnt <> 9 THEN
        RAISE EXCEPTION 'V11.138.0 校验失败：finance_account 新增列数 = %（期望 9）', col_cnt;
    END IF;
    RAISE NOTICE 'V11.138.0 银行账户金标准升级完成：finance_account 新增 % 列', col_cnt;
END $$;
