-- =============================================================================
-- 修复共享消息底座 `sys_message` 主键不可生成的问题（PostgreSQL）
--
--   背景（2026-09-13 实测定位）：实体 `SysMessage.id` 为 `@TableId(type = IdType.AUTO)`
--   （依赖数据库自增），但建表时**既没有序列也没有 identity**，且列 `NOT NULL`。
--   结果是 `MessageServiceImpl.sendSiteMessage / sendEmail`（以及本次新增的 `sendSms`）
--   一执行 INSERT 就报 `null value in column "id" ... violates not-null constraint` ——
--   即**整个消息底座（邮件 / 站内信 / 短信）在此库从未成功写入过**。
--   该缺陷被本次「配送 ETA 通知接入消息底座」暴露出来。
--
--   修复方式：为 id 补序列 + 默认值，并保持 AUTO 策略（与其它表一致）。
--
--   幂等：IF NOT EXISTS / SET DEFAULT 可重复执行。
-- =============================================================================

CREATE SEQUENCE IF NOT EXISTS sys_message_id_seq;

ALTER TABLE sys_message ALTER COLUMN id SET DEFAULT nextval('sys_message_id_seq');

-- 已有数据时把序列推进到 max(id)+1，避免主键冲突
SELECT setval('sys_message_id_seq', COALESCE((SELECT MAX(id) FROM sys_message), 0) + 1, false);

COMMENT ON SEQUENCE sys_message_id_seq IS '系统消息主键序列（修复 sys_message.id 无自增来源的问题）';
