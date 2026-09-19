-- =============================================================================
-- 系统模块 · 数据管理（备份管理 62303 / 同步任务 62304 / 清理规则 62305）运行结果列
--
-- 背景（2026-09-19）：三页的「核心动作」此前是空实现 ——
--   · 备份：create 只插台账（无 pg_dump、无文件落盘、status 永停 running）；restore 是空实现
--   · 同步：execute 只 setLastSyncTime + setStatus("running")，无数据搬运，running 永不复位
--   · 清理：execute 只 setStatus("running")，targetTable/conditionColumn/retentionDays 全不被读取
--   本次把三者改为真实执行/诚实降级，但**现有列无法承载「执行结果」**：
--   · `sys_backup_record` 只能表达「备份本身」的状态，无法表达「最近一次恢复」的结果；
--   · `sys_sync_task.last_sync_time` 只能表达时间，无法表达投递成功/失败与原因；
--   · `sys_data_cleanup_rule` 无任何执行结果列（文档 §8.4「执行可见性」登记为 P1：★需新增两列）。
--
-- 口径（重要）：
--   ① `sys_sync_task.status` / `sys_data_cleanup_rule.status` 是**启停开关**（前端「启用/暂停」直接写它，
--      值域 running/paused/stopped），**不是执行状态** —— 因此本次新增的 `last_run_status` 与之分离，
--      执行动作**不再写 status**（旧实现的「永停 running」正是把开关当成了执行态）。
--   ② `sys_backup_record.status` 是**备份本身**的状态（dump 结果），恢复动作写 `restore_status`，
--      不污染备份状态（一次失败的恢复不代表这份备份是坏的）。
--   ③ 全部新增列可空、无默认值 —— 历史行为空，语义为「从未执行过」。
--
-- 列数核对（仓库规范：单表 ≤ 25 列）：
--   sys_backup_record 13 → 16、sys_sync_task 16 → 19、sys_data_cleanup_rule 14 → 18，均达标。
-- =============================================================================

-- ── 备份管理：最近一次「恢复」的结果（恢复是最高危动作，必须留痕且失败可见）──
ALTER TABLE sys_backup_record ADD COLUMN IF NOT EXISTS restore_status  VARCHAR(32);
ALTER TABLE sys_backup_record ADD COLUMN IF NOT EXISTS restore_time    TIMESTAMP;
ALTER TABLE sys_backup_record ADD COLUMN IF NOT EXISTS restore_message TEXT;

COMMENT ON COLUMN sys_backup_record.restore_status  IS '最近一次恢复状态：none/dispatched/success/failed（NULL=从未恢复过）';
COMMENT ON COLUMN sys_backup_record.restore_time    IS '最近一次恢复的执行时间';
COMMENT ON COLUMN sys_backup_record.restore_message IS '最近一次恢复的结果摘要或失败原因';

-- ── 同步任务：最近一次「立即执行」的结果（投递到 sync-engine 的结论）──
ALTER TABLE sys_sync_task ADD COLUMN IF NOT EXISTS last_run_time    TIMESTAMP;
ALTER TABLE sys_sync_task ADD COLUMN IF NOT EXISTS last_run_status  VARCHAR(32);
ALTER TABLE sys_sync_task ADD COLUMN IF NOT EXISTS last_run_message TEXT;

COMMENT ON COLUMN sys_sync_task.last_run_time    IS '最近一次「立即执行」的发起时间（NULL=从未执行过）';
COMMENT ON COLUMN sys_sync_task.last_run_status  IS '最近一次执行结论：running(在途)/dispatched(已投递同步引擎)/failed(未投递或引擎拒绝)';
COMMENT ON COLUMN sys_sync_task.last_run_message IS '最近一次执行的结果说明或失败原因（诚实口径，不代表数据已搬运完成）';

-- ── 清理规则：最近一次「立即执行」的删除结果（破坏性动作，必须可审计/可追溯）──
ALTER TABLE sys_data_cleanup_rule ADD COLUMN IF NOT EXISTS last_run_time        TIMESTAMP;
ALTER TABLE sys_data_cleanup_rule ADD COLUMN IF NOT EXISTS last_run_status      VARCHAR(32);
ALTER TABLE sys_data_cleanup_rule ADD COLUMN IF NOT EXISTS last_deleted_count   BIGINT;
ALTER TABLE sys_data_cleanup_rule ADD COLUMN IF NOT EXISTS last_run_result      TEXT;

COMMENT ON COLUMN sys_data_cleanup_rule.last_run_time      IS '最近一次「立即执行」的时间（NULL=从未执行过）';
COMMENT ON COLUMN sys_data_cleanup_rule.last_run_status    IS '最近一次执行结论：success(含部分删除)/failed/rejected(前置校验拒绝)';
COMMENT ON COLUMN sys_data_cleanup_rule.last_deleted_count IS '最近一次实际删除的行数（执行前预统计与执行后复统计均落 last_run_result）';
COMMENT ON COLUMN sys_data_cleanup_rule.last_run_result    IS '最近一次执行的明细：目标表/条件列/保留天数/预统计行数/实删行数/是否触及单次上限/失败原因';
