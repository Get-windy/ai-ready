-- =============================================================================
-- 「系统任务」（设置 → 账套操作 → 系统任务，菜单 70561 / set:system-task）创建人收口
-- 2026-09-18
--
-- 背景（《设置模块 README》§10.5 未闭环项）：
--   V11.401.0 已给 `scheduled_task_log` 补了 created_by_name（姓名，供 9 列台账的「创建人」列展示），
--   但**写入侧从未写过它** —— TaskExecutor 建日志行时只落 task_id/task_name/start_time/...，
--   于是台账所有行（含 15 行历史数据）的「创建人」恒为空 → 页面恒显示「-」。
--
-- 本次补齐（写入侧见 `cn.aiedge.scheduler.task.TaskExecutor#executeTask`）：
--   · 有会话的写入（开发工具 → 定时任务 → 立即执行 / 重试）→ 记当前登录用户的 id + 姓名；
--   · 无会话的调度线程触发 → created_by_name 记系统标识「定时调度」，create_by 保持 NULL。
--
-- create_by 列（发起人用户 id，sys_user.id）：
--   本表原本**没有任何列记录过发起人**，只存姓名会有重名歧义、也无法反查账号，
--   故按仓库通用审计列约定（biz_party / sys_user / shop_user 等同名同义）补 id 列。
--   列可空 —— 平台调度触发不属任何用户，NULL 是真实语义（此时姓名列必为「定时调度」）。
--
-- ⚠️ 历史行**不做任何回填**（不编造数据）：
--   create_by 本迁移才建立、created_by_name 此前从未被写入，历史行**没有任何来源**可以推出发起人；
--   若用 create_by 联 sys_user 回填，只会得到 NULL。故历史行两列保持 NULL，页面继续如实显示「-」。
--
-- 幂等：ADD COLUMN IF NOT EXISTS + COMMENT 可重复执行。
-- =============================================================================

ALTER TABLE scheduled_task_log ADD COLUMN IF NOT EXISTS create_by BIGINT;

COMMENT ON COLUMN scheduled_task_log.create_by IS
    '发起人用户 id（sys_user.id）：有会话的立即执行/重试记当前登录用户；无会话的定时调度为 NULL（此时 created_by_name 记「定时调度」）。本列建立前的历史行无来源可回填，保持 NULL';
