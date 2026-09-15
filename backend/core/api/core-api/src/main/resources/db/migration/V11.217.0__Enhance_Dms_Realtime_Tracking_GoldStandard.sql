-- =============================================================================
-- 实时跟踪（配送 → 配送跟踪 → 实时跟踪，dms:realtime-tracking，菜单 80740）金标准增强（PostgreSQL）
--   文档：《实时跟踪开发文档》§3.5 后端接口 / §4 业务规范 / §配置落位（2026-09-13 增补）
--
--   本次**不加业务列**（本页是聚合读模型：配送员最新位置 / 统计 / 回放 / 异常预警，全部由
--   既有 dms_rider + dms_tracking + dms_task 派生），只做两件事：
--     1. 落地 §4「在线判定阈值（默认 2 分钟）」「异常预警：超速/异常停留」的可配置项 ——
--        按《配置落位》口径，**系统级单值**配置进配置中心（dms_config，配送参数页可改），
--        缺失时代码回落默认值，不强制每租户预置；
--     2. 复用 V11.215.0 已建的时序索引（tenant_id + rider_id + report_time），
--        「取每人最新位置」的批量聚合正是走该索引。
--
--   幂等：ON CONFLICT (tenant_id, config_key) WHERE deleted = 0 DO UPDATE
-- =============================================================================

INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time)
VALUES
  (0, 'dms.tracking.online.minutes', '2',  '实时跟踪在线判定：最近上报在 N 分钟内视为在线（默认 2）', 'TENANT', 0, now(), now()),
  (0, 'dms.tracking.speed.limit',    '60', '实时跟踪异常预警：超速阈值(km/h)（默认 60）',              'TENANT', 0, now(), now()),
  (0, 'dms.tracking.stop.minutes',   '15', '实时跟踪异常预警：异常停留阈值(分钟，位移<50米)（默认 15）',  'TENANT', 0, now(), now())
ON CONFLICT (tenant_id, config_key) WHERE deleted = 0 DO UPDATE
  SET config_value = EXCLUDED.config_value,
      config_desc  = EXCLUDED.config_desc,
      scope        = EXCLUDED.scope,
      update_time  = CURRENT_TIMESTAMP;

-- 同时下发到系统租户(1)，与既有 energy.* / verification.* 配置的双行口径保持一致
INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time)
SELECT 1, config_key, config_value, config_desc, scope, 0, now(), now()
FROM dms_config
WHERE tenant_id = 0
  AND config_key IN ('dms.tracking.online.minutes', 'dms.tracking.speed.limit', 'dms.tracking.stop.minutes')
  AND deleted = 0
ON CONFLICT (tenant_id, config_key) WHERE deleted = 0 DO UPDATE
  SET config_value = EXCLUDED.config_value,
      update_time  = CURRENT_TIMESTAMP;
