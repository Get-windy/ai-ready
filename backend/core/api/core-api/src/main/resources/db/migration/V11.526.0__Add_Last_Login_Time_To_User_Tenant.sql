-- 用户-企业关联：记录「上次登录该企业」的时间。
--
-- 用途：一个用户关联多个企业时，登录要让他选企业 —— 把上次登录的那个排在第一位，
-- 免去每次重新找。排序口径见 SysUserTenantMapper#selectTenantsByUserId：
--   ORDER BY last_login_time DESC NULLS LAST, is_default DESC, tenant_name ASC
--
-- 不做存量回填：登录/切换企业时即写入，老用户首次登录后即自举生效。

ALTER TABLE sys_user_tenant
    ADD COLUMN IF NOT EXISTS last_login_time TIMESTAMP;

COMMENT ON COLUMN sys_user_tenant.last_login_time IS '上次登录该企业的时间（登录成功/切换企业时更新，用于登录时置顶）';
