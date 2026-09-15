-- =============================================================================
-- 智能调度（配送 → 调度管理 → 智能调度，菜单 80850 / `dms:dispatch`）金标准增强（PostgreSQL）
--   文档：《智能调度开发文档》§3.2 策略配置 Tab / §3.4 派单策略须落到可配置 / §配置落位（2026-09-13）
--
--   本页定位重塑为「**调度策略与执行台**」（任务列表/手工指派归《调度任务》），因此**不新增业务表**，
--   只把派单策略参数落到**配置中心**（`dms_config`，配置中心页保存即热生效）：
--     策略选择 / 权重（距离·负载·评分）/ 约束（并接上限·在线要求·载重·容积）/ 超时升级阈值。
--   `DispatchService` 读不到时一律回落代码默认值，不强制每租户预置。
--
--   幂等：ON CONFLICT (tenant_id, config_key) WHERE deleted = 0 DO UPDATE
-- =============================================================================

INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time)
VALUES
  (0, 'dms.dispatch.strategy',            'NEAREST', '派单策略：NEAREST-最近可用 / BALANCED-负载均衡 / SCORE-评分优先 / AREA-区域分包', 'TENANT', 0, now(), now()),
  (0, 'dms.dispatch.weight.distance',     '1',       '策略权重：距离(越近越优先)',            'TENANT', 0, now(), now()),
  (0, 'dms.dispatch.weight.load',         '1',       '策略权重：负载(在途单越少越优先)',      'TENANT', 0, now(), now()),
  (0, 'dms.dispatch.weight.score',        '1',       '策略权重：评分(综合评分越高越优先)',    'TENANT', 0, now(), now()),
  (0, 'dms.dispatch.max.concurrent',      '5',       '约束：单配送员最大并接在途单数(0=不限)', 'TENANT', 0, now(), now()),
  (0, 'dms.dispatch.require.online',      'true',    '约束：是否仅向「空闲」配送员派单',       'TENANT', 0, now(), now()),
  (0, 'dms.dispatch.max.load.kg',         '2000',    '约束：单任务核定载重上限(kg，0=不限，超限不可指派)', 'TENANT', 0, now(), now()),
  (0, 'dms.dispatch.max.volume.m3',       '10',      '约束：单任务货厢容积上限(m³，0=不限)',  'TENANT', 0, now(), now()),
  (0, 'dms.dispatch.timeout.escalate.minutes', '30', '超时升级阈值(分钟)：在途任务超此值进入超时统计/预警', 'TENANT', 0, now(), now())
ON CONFLICT (tenant_id, config_key) WHERE deleted = 0 DO UPDATE
  SET config_value = EXCLUDED.config_value,
      config_desc  = EXCLUDED.config_desc,
      scope        = EXCLUDED.scope,
      update_time  = CURRENT_TIMESTAMP;

-- 同步下发到系统租户(1)，与既有配置双行口径一致
INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time)
SELECT 1, config_key, config_value, config_desc, scope, 0, now(), now()
FROM dms_config
WHERE tenant_id = 0 AND deleted = 0 AND config_key LIKE 'dms.dispatch.%'
ON CONFLICT (tenant_id, config_key) WHERE deleted = 0 DO UPDATE
  SET config_value = EXCLUDED.config_value,
      update_time  = CURRENT_TIMESTAMP;
