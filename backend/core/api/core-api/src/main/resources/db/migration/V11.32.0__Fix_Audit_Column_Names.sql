-- ============================================================
-- V11.32.0: 修复 V11.29.0 审计列名不匹配
-- V11.29.0 建表时用了 create_time/deleted，但 BaseEntity
-- 映射的是 created_at/deleted_flag。本迁移将已建表的列改正确。
-- 幂等：IF EXISTS + 列存在判断
-- ============================================================

DO $$
BEGIN
    -- md_payment_method
    IF EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name = 'md_payment_method' AND column_name = 'create_time') THEN
        ALTER TABLE md_payment_method RENAME COLUMN create_time TO created_at;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name = 'md_payment_method' AND column_name = 'update_time') THEN
        ALTER TABLE md_payment_method RENAME COLUMN update_time TO updated_at;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name = 'md_payment_method' AND column_name = 'create_by') THEN
        ALTER TABLE md_payment_method RENAME COLUMN create_by TO created_by;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name = 'md_payment_method' AND column_name = 'update_by') THEN
        ALTER TABLE md_payment_method RENAME COLUMN update_by TO updated_by;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name = 'md_payment_method' AND column_name = 'deleted')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name = 'md_payment_method' AND column_name = 'deleted_flag') THEN
        ALTER TABLE md_payment_method RENAME COLUMN deleted TO deleted_flag;
    END IF;

    -- md_payment_channel
    IF EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name = 'md_payment_channel' AND column_name = 'create_time') THEN
        ALTER TABLE md_payment_channel RENAME COLUMN create_time TO created_at;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name = 'md_payment_channel' AND column_name = 'update_time') THEN
        ALTER TABLE md_payment_channel RENAME COLUMN update_time TO updated_at;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name = 'md_payment_channel' AND column_name = 'create_by') THEN
        ALTER TABLE md_payment_channel RENAME COLUMN create_by TO created_by;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name = 'md_payment_channel' AND column_name = 'update_by') THEN
        ALTER TABLE md_payment_channel RENAME COLUMN update_by TO updated_by;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name = 'md_payment_channel' AND column_name = 'deleted')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_name = 'md_payment_channel' AND column_name = 'deleted_flag') THEN
        ALTER TABLE md_payment_channel RENAME COLUMN deleted TO deleted_flag;
    END IF;
END $$;
