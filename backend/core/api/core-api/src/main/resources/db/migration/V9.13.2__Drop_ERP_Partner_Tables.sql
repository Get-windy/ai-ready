-- V9.13.2 删除旧的 erp_partner 系列表
-- biz_party 已完全替代 erp_partner，废弃的 PartnerController 标记为 @Deprecated
-- 删除前确保数据已迁移（由 V9.13.1 保证）

-- ============================================================
-- 1. 检查 biz_party 是否有数据（防止误删）
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM biz_party LIMIT 1) THEN
        RAISE WARNING 'biz_party 表为空，请先执行 V9.13.1 数据迁移';
    END IF;
END $$;

-- ============================================================
-- 2. 删除 erp_partner 系列表
-- ============================================================
DROP TABLE IF EXISTS erp_partner CASCADE;
DROP TABLE IF EXISTS erp_partner_category CASCADE;
DROP TABLE IF EXISTS erp_partner_grade CASCADE;

-- ============================================================
-- 3. 记录迁移日志
-- ============================================================
INSERT INTO biz_migration_log (source_table, target_table, source_id, status)
VALUES ('erp_partner(ALL)', 'biz_party', 0, 'SUCCESS');
