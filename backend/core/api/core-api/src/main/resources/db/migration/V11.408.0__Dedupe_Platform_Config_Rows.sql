-- =============================================================================
-- 平台级配置表 · 去重 + 加租户唯一约束
--
-- 问题（2026-09-19 实测）：`sys_mail_config` / `sys_sms_config` / `sys_storage_config` /
--   `sys_security_policy` 四张表**每张都有 2 行 `tenant_id = 0`**（id=1 建于 2026-06-17 17:15:31、
--   id=2 建于同日 20:07:51 —— 两次种子迁移各插了一份，四表完全同构）。
--
--   后果：服务层用 `selectOne` 读「本租户的配置」（`SecurityPolicyServiceImpl:38` 等），
--   一旦租户口径收敛到 0（平台级配置的正确口径），`selectOne` 就会抛
--   `TooManyResultsException: Expected one result (or null) to be returned by selectOne(), but found: 2`
--   → 平台设置 4 页（62502-62505）**整页 500**。
--   此前之所以没暴露，是因为控制器把「请求头里的会话租户 1」当成了查询口径（那里没有行），
--   属于「用错误的口径掩盖了错误的数据」。
--
-- 处置：
--   ① 同租户只保留 id 最小的一行（即 17:15:31 的原始种子），删除多余行；
--   ② 加 `UNIQUE (tenant_id)`，从结构上杜绝再次出现同租户多行。
--      （四张表都**没有 deleted 列**，故不能用 `WHERE deleted = 0` 部分索引。）
-- =============================================================================

-- ① 去重：同一 tenant_id 只留最小 id
DELETE FROM sys_mail_config      a USING sys_mail_config      b WHERE a.tenant_id = b.tenant_id AND a.id > b.id;
DELETE FROM sys_sms_config       a USING sys_sms_config       b WHERE a.tenant_id = b.tenant_id AND a.id > b.id;
DELETE FROM sys_storage_config   a USING sys_storage_config   b WHERE a.tenant_id = b.tenant_id AND a.id > b.id;
DELETE FROM sys_security_policy  a USING sys_security_policy  b WHERE a.tenant_id = b.tenant_id AND a.id > b.id;

-- ② 唯一约束（幂等：索引已存在则跳过）
CREATE UNIQUE INDEX IF NOT EXISTS uk_mail_config_tenant     ON sys_mail_config     (tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sms_config_tenant      ON sys_sms_config      (tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_storage_config_tenant  ON sys_storage_config  (tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_security_policy_tenant ON sys_security_policy (tenant_id);
