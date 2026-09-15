-- V9.16.0: 为 biz_number_sequence 表添加租户隔离和语言区域支持

-- 添加 locale 字段
ALTER TABLE biz_number_sequence ADD COLUMN IF NOT EXISTS locale VARCHAR(20) NOT NULL DEFAULT 'zh_CN';

-- 添加 tenant_id 字段
ALTER TABLE biz_number_sequence ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;

-- 删除原来的唯一约束（如果存在）
ALTER TABLE biz_number_sequence DROP CONSTRAINT IF EXISTS biz_number_sequence_biz_type_key;

-- 添加新的唯一约束（包含 tenant_id, biz_type, locale）
ALTER TABLE biz_number_sequence ADD CONSTRAINT uk_biz_number_sequence UNIQUE (tenant_id, biz_type, locale);

-- 更新已有记录的 locale 和 tenant_id
UPDATE biz_number_sequence SET locale = 'zh_CN', tenant_id = 1 WHERE locale IS NULL OR tenant_id IS NULL;
