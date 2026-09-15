-- 配送·超时任务升级扫描：默认参数由「演练」改为「真执行」
--
-- 理由（承接 V11.362.0）：安全闸门由 **enabled = 0（默认停用）** 承担；若把参数默认设为 dryRun=true，
-- 运维启用任务后只会看到「演练（未落库）」摘要而没有任何实际动作 —— 那正是本次复核要消灭的
-- 「配置看起来生效、实际静默空转」缺陷类型。需要演练时把参数改成 {"dryRun": true} 即可。
UPDATE scheduled_task
SET execute_params = '{"dryRun": false}',
    task_desc = '在途任务超时未接单 → 按派单策略换人重派（确无他人可派时退回原配送员并重新计时）；配送中/取货中只告警审计。'
       || '安全闸门：默认停用 + Redis 锁多实例互斥；把 execute_params 改为 {"dryRun": true} 可先演练（只统计不落库）',
    update_time = now()
WHERE job_key = 'dms.dispatch.escalateOverdue'
  AND deleted = 0
  AND COALESCE(execute_params, '') = '{"dryRun": true}';
