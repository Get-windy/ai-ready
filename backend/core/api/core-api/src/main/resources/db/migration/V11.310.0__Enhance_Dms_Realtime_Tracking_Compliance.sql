-- =============================================================================
-- 实时跟踪（配送 → 配送跟踪 → 实时跟踪，dms:realtime-tracking，菜单 80740）
--   补齐《实时跟踪开发文档》§5/§6 三项遗留：偏航预警 + 实时推送(SSE) + 合规(采集时段/保留策略)
--
--   本页仍是纯聚合读模型，**不加业务表/列**，只把新增阈值与合规项落到配置中心（dms_config），
--   缺失时代码统一回落默认值，不强制每租户预置：
--     1. dms.tracking.deviation.meters  偏航阈值(米)：当前位置偏离「任务起点→客户」配送基线的上限，默认 1000
--     2. dms.tracking.collect.hours     位置采集时段（HH:mm-HH:mm，支持跨天；空/缺失=全天采集）
--     3. dms.tracking.retention.days    轨迹保留天数（0/缺失=不做自动清理，默认安全）
--
--   幂等：ON CONFLICT (tenant_id, config_key) WHERE deleted = 0 DO UPDATE
-- =============================================================================

INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time)
VALUES
  (0, 'dms.tracking.deviation.meters', '1000', '实时跟踪异常预警：偏航阈值(米，偏离「起点→客户」配送基线)（默认 1000）', 'TENANT', 0, now(), now()),
  (0, 'dms.tracking.collect.hours',    '00:00-23:59:59', '实时跟踪合规：位置采集时段(HH:mm-HH:mm，支持跨天；空=全天)（默认全天）', 'TENANT', 0, now(), now()),
  (0, 'dms.tracking.retention.days',   '0',    '实时跟踪合规：轨迹保留天数(0=不自动清理)（默认 0）', 'TENANT', 0, now(), now())
ON CONFLICT (tenant_id, config_key) WHERE deleted = 0 DO UPDATE
  SET config_value = EXCLUDED.config_value,
      config_desc  = EXCLUDED.config_desc,
      scope        = EXCLUDED.scope,
      update_time  = CURRENT_TIMESTAMP;

-- 同时下发到系统租户(1)，与既有 dms.tracking.* 的双行口径保持一致
INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time)
SELECT 1, config_key, config_value, config_desc, scope, 0, now(), now()
FROM dms_config
WHERE tenant_id = 0
  AND config_key IN ('dms.tracking.deviation.meters', 'dms.tracking.collect.hours', 'dms.tracking.retention.days')
  AND deleted = 0
ON CONFLICT (tenant_id, config_key) WHERE deleted = 0 DO UPDATE
  SET config_value = EXCLUDED.config_value,
      update_time  = CURRENT_TIMESTAMP;
