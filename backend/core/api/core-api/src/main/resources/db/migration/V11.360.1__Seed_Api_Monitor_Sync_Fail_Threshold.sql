-- =============================================================================
-- V11.360.1 · 补种「库存同步失败告警线」配置键（API监控）
--
-- 背景（共享 dev 环境的既有事实，非设计意图）：
--   V11.360.0 的**首个版本**（6 键）在 2026-09-14 05:03 被并行实例抢先应用到共享库；
--   其后补充的 `monitor.threshold.sync-fail-count` 键因
--   `spring.flyway.repair-on-migrate=true`（dev 档）按磁盘改写 checksum、且该版本已标记 applied
--   **不会重跑**，故该键缺失（后端将回落到代码默认值 0，行为等价但配置中心不可见）。
--
-- 本迁移**幂等补种**该键：新环境执行 V11.360.0 时已含此键，ON CONFLICT DO UPDATE 保证无副作用。
-- 口径与 V11.360.0 完全一致（全局默认 tenant_id=0 + 系统租户 1 双行）。
-- =============================================================================

INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time)
VALUES
  (0, 'monitor.threshold.sync-fail-count', '0',
   'API监控：库存同步失败数告警线(条)，超过该值产生告警（0=有失败即告警）', 'TENANT', 0, now(), now())
ON CONFLICT (tenant_id, config_key) WHERE deleted = 0 DO UPDATE
  SET config_value = EXCLUDED.config_value,
      config_desc  = EXCLUDED.config_desc,
      scope        = EXCLUDED.scope,
      update_time  = CURRENT_TIMESTAMP;

INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time)
SELECT 1, config_key, config_value, config_desc, scope, 0, now(), now()
FROM dms_config
WHERE tenant_id = 0 AND config_key = 'monitor.threshold.sync-fail-count' AND deleted = 0
ON CONFLICT (tenant_id, config_key) WHERE deleted = 0 DO UPDATE
  SET config_value = EXCLUDED.config_value,
      update_time  = CURRENT_TIMESTAMP;
