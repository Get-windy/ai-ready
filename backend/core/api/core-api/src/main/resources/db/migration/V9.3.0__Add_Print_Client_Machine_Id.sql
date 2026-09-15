-- =====================================================
-- V9.3.0: 打印客户端添加 machine_id 字段
-- 用于识别同一台机器（首次启动生成，用于客户端认证）
-- =====================================================

ALTER TABLE sys_print_client ADD COLUMN IF NOT EXISTS machine_id VARCHAR(64) DEFAULT NULL;

COMMENT ON COLUMN sys_print_client.machine_id IS '客户端机器标识（首次启动生成，用于识别同一台机器）';
