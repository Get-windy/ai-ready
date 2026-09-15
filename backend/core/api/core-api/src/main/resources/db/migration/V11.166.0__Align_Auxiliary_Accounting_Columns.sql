-- 辅助核算类型/项目表列名对齐 finance 模块统一口径
--
-- 背景（会计科目开发文档 P2 遗留缺陷）：
--   V11.37.0 建表时两张表用了 create_by / create_time / update_by / update_time / deleted，
--   而实体 FinanceAuxiliaryType / FinanceAuxiliaryItem 继承 finance BaseEntity，
--   映射的是 created_by / created_at / updated_by / updated_at / deleted_flag；
--   两个 Mapper 的手写 SQL 同样按 deleted_flag 过滤。
--   → /api/erp/finance/auxiliary/type/**(list|page) 与 /item/** 全部 500
--     （报「字段 created_by 不存在」），「辅助核算设置」页不可用；
--     连带「会计科目 → 核算项」下拉的数据源无法维护。
--
-- 处理口径：以实体/代码为准（与 finance_account_subject 等 finance 模块其它表一致），
--   仅重命名列 + 将 created_by/updated_by 由 bigint 转为 varchar（BaseEntity 中为 String），
--   不动任何业务数据。
--   注：finance_auxiliary_balance 表未在该链路上使用（余额接口走凭证分录聚合），故不处理。
--
-- 幂等：以 information_schema 判定旧列是否存在后再改名，可重复执行。

DO $$
BEGIN
    -- ============ finance_auxiliary_type ============
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'finance_auxiliary_type'
                 AND column_name = 'create_by') THEN
        ALTER TABLE finance_auxiliary_type RENAME COLUMN create_by TO created_by;
        ALTER TABLE finance_auxiliary_type ALTER COLUMN created_by TYPE varchar(64) USING created_by::varchar;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'finance_auxiliary_type'
                 AND column_name = 'update_by') THEN
        ALTER TABLE finance_auxiliary_type RENAME COLUMN update_by TO updated_by;
        ALTER TABLE finance_auxiliary_type ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::varchar;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'finance_auxiliary_type'
                 AND column_name = 'create_time') THEN
        ALTER TABLE finance_auxiliary_type RENAME COLUMN create_time TO created_at;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'finance_auxiliary_type'
                 AND column_name = 'update_time') THEN
        ALTER TABLE finance_auxiliary_type RENAME COLUMN update_time TO updated_at;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'finance_auxiliary_type'
                 AND column_name = 'deleted') THEN
        ALTER TABLE finance_auxiliary_type RENAME COLUMN deleted TO deleted_flag;
    END IF;

    -- ============ finance_auxiliary_item ============
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'finance_auxiliary_item'
                 AND column_name = 'create_by') THEN
        ALTER TABLE finance_auxiliary_item RENAME COLUMN create_by TO created_by;
        ALTER TABLE finance_auxiliary_item ALTER COLUMN created_by TYPE varchar(64) USING created_by::varchar;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'finance_auxiliary_item'
                 AND column_name = 'update_by') THEN
        ALTER TABLE finance_auxiliary_item RENAME COLUMN update_by TO updated_by;
        ALTER TABLE finance_auxiliary_item ALTER COLUMN updated_by TYPE varchar(64) USING updated_by::varchar;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'finance_auxiliary_item'
                 AND column_name = 'create_time') THEN
        ALTER TABLE finance_auxiliary_item RENAME COLUMN create_time TO created_at;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'finance_auxiliary_item'
                 AND column_name = 'update_time') THEN
        ALTER TABLE finance_auxiliary_item RENAME COLUMN update_time TO updated_at;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'finance_auxiliary_item'
                 AND column_name = 'deleted') THEN
        ALTER TABLE finance_auxiliary_item RENAME COLUMN deleted TO deleted_flag;
    END IF;
END $$;

COMMENT ON COLUMN finance_auxiliary_type.created_by IS '创建者';
COMMENT ON COLUMN finance_auxiliary_type.created_at IS '创建时间';
COMMENT ON COLUMN finance_auxiliary_type.updated_by IS '更新者';
COMMENT ON COLUMN finance_auxiliary_type.updated_at IS '更新时间';
COMMENT ON COLUMN finance_auxiliary_type.deleted_flag IS '逻辑删除(0=未删除)';

COMMENT ON COLUMN finance_auxiliary_item.created_by IS '创建者';
COMMENT ON COLUMN finance_auxiliary_item.created_at IS '创建时间';
COMMENT ON COLUMN finance_auxiliary_item.updated_by IS '更新者';
COMMENT ON COLUMN finance_auxiliary_item.updated_at IS '更新时间';
COMMENT ON COLUMN finance_auxiliary_item.deleted_flag IS '逻辑删除(0=未删除)';
