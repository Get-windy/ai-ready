-- 会计科目（资料 → 财务账户 → 会计科目）金标准升级
-- 1) 补对标编辑器字段：助记码 / 科目全名 / 核算项（辅助核算类型）
-- 2) 核算项直接引用 finance_auxiliary_type，不新建科目表（P0 单一口径红线）

-- 0) 核算项主数据 finance_auxiliary_type 的种子行 tenant_id=0，与业务租户(1)不一致 → 租户插件过滤后查不到，
--    统一到业务租户，保证「核算项」下拉与辅助核算口径一致（不新建字典表）
UPDATE finance_auxiliary_type SET tenant_id = 1 WHERE tenant_id = 0;
UPDATE finance_auxiliary_item SET tenant_id = 1 WHERE tenant_id = 0;

ALTER TABLE finance_account_subject ADD COLUMN IF NOT EXISTS mnemonic_code varchar(50);
ALTER TABLE finance_account_subject ADD COLUMN IF NOT EXISTS full_name varchar(500);
ALTER TABLE finance_account_subject ADD COLUMN IF NOT EXISTS auxiliary_type_id bigint;

COMMENT ON COLUMN finance_account_subject.mnemonic_code IS '助记码（拼音首字母，用于快速检索）';
COMMENT ON COLUMN finance_account_subject.full_name IS '科目全名（上级科目链 + 本科目名称，留空自动拼装）';
COMMENT ON COLUMN finance_account_subject.auxiliary_type_id IS '核算项 = 辅助核算类型ID（finance_auxiliary_type.id）';

-- 3) 标志位类型对齐实体：is_enabled / is_leaf 建表时为 integer，而 AccountSubject 实体映射为 Boolean，
--    MyBatis-Plus 传 boolean 时 PG 报「操作符不存在: integer = boolean」→ 新增/停用/按启用过滤全部 500
ALTER TABLE finance_account_subject ALTER COLUMN is_enabled DROP DEFAULT;
ALTER TABLE finance_account_subject ALTER COLUMN is_leaf    DROP DEFAULT;
ALTER TABLE finance_account_subject
    ALTER COLUMN is_enabled TYPE boolean USING (is_enabled <> 0),
    ALTER COLUMN is_leaf    TYPE boolean USING (is_leaf <> 0);
ALTER TABLE finance_account_subject ALTER COLUMN is_enabled SET DEFAULT true;
ALTER TABLE finance_account_subject ALTER COLUMN is_leaf    SET DEFAULT true;

-- 存量数据回填科目全名：按上级科目链递归拼装（顶级科目 = 本科目名称）
WITH RECURSIVE subject_chain AS (
    SELECT s.id,
           (s.subject_name)::varchar(500) AS derived_name
    FROM finance_account_subject s
    WHERE s.parent_id IS NULL
    UNION ALL
    SELECT c.id,
           (p.derived_name || '/' || c.subject_name)::varchar(500)
    FROM finance_account_subject c
    JOIN subject_chain p ON c.parent_id = p.id
)
UPDATE finance_account_subject target
SET full_name = sc.derived_name
FROM subject_chain sc
WHERE target.id = sc.id
  AND (target.full_name IS NULL OR target.full_name = '' OR target.full_name = target.subject_name);

CREATE INDEX IF NOT EXISTS idx_finance_account_subject_aux_type
    ON finance_account_subject (auxiliary_type_id);
