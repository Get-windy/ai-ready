-- V9.7.0: 为业务编号序列表添加租户ID字段，支持多租户独立编号
-- 每个租户的每种业务类型都有独立的编号序列

-- 删除原有的 biz_type 列级唯一约束（PostgreSQL 隐式创建的 unique index）
ALTER TABLE biz_number_sequence DROP CONSTRAINT IF EXISTS biz_number_sequence_biz_type_key;

-- 删除原有的复合索引
DROP INDEX IF EXISTS idx_biz_number_sequence_type_date;

-- 添加 tenant_id 列（允许为空以支持数据迁移）
ALTER TABLE biz_number_sequence ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

-- 为现有数据设置默认租户ID
UPDATE biz_number_sequence SET tenant_id = 1 WHERE tenant_id IS NULL;

-- 修改 tenant_id 为 NOT NULL
ALTER TABLE biz_number_sequence ALTER COLUMN tenant_id SET NOT NULL;

-- 创建新的索引：租户 + 业务类型 + 日期（用于查询优化）
CREATE INDEX IF NOT EXISTS idx_biz_number_sequence_tenant_type_date
ON biz_number_sequence(tenant_id, biz_type, seq_date);

-- 创建租户 + 业务类型的唯一约束（每个租户的每种业务类型只能有一条序列记录）
CREATE UNIQUE INDEX IF NOT EXISTS uk_biz_number_sequence_tenant_type
ON biz_number_sequence(tenant_id, biz_type);
