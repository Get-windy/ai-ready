-- ============================================================
-- V4.1.0: 密码过期跟踪
-- 1. sys_user 增加 password_update_time 字段
-- 2. 已存在的用户初始化为当前时间
-- ============================================================

-- 1. 用户表增加密码最后更新时间字段
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS password_update_time TIMESTAMP;

COMMENT ON COLUMN sys_user.password_update_time IS '密码最后更新时间，用于密码过期校验（null 表示首次登录后更新）';

-- 2. 将已有用户的密码更新时间初始化为账号创建时间
UPDATE sys_user SET password_update_time = create_time
WHERE password_update_time IS NULL AND create_time IS NOT NULL;

-- 3. 若 create_time 也为空，初始化为当前时间
UPDATE sys_user SET password_update_time = NOW()
WHERE password_update_time IS NULL;
