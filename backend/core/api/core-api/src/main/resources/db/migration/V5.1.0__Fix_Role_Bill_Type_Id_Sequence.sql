-- 修复 sys_role_bill_type 表的 id 字段，改为自增序列
-- 问题：V5.0.0中定义为 BIGINT PRIMARY KEY，导致插入时id为null

-- 创建序列（如果不存在）
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_sequences WHERE sequencename = 'sys_role_bill_type_id_seq') THEN
        CREATE SEQUENCE sys_role_bill_type_id_seq;
    END IF;
END $$;

-- 设置当前最大id为序列起始值
SELECT setval('sys_role_bill_type_id_seq', COALESCE((SELECT MAX(id) FROM sys_role_bill_type), 0) + 1);

-- 修改列默认值
ALTER TABLE sys_role_bill_type
ALTER COLUMN id SET DEFAULT nextval('sys_role_bill_type_id_seq');

-- 将序列关联到列
ALTER SEQUENCE sys_role_bill_type_id_seq OWNED BY sys_role_bill_type.id;