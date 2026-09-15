-- =============================================================================
-- V11.360.0 · API监控（配送 → API监控，菜单 90107 `trade:api-monitor`）金标准数据侧
--
-- 依据《API监控开发文档》§3 金标准目标设计 / §5 待完善（实施清单）：
--   ① 【复用不重复建表】「接口调用日志」底表复用 V8.14.0 已建、此前**无任何读写**的 `api_access_log`
--      （原注释即「用于限流监控」，语义一致，0 行数据、0 处代码引用），仅补齐网关级监控所需列：
--      api_name / request_id / direction / status / error_code / error_msg ——
--      **不新建 api_call_log**，避免同义表双轨（见《配送模块 README》§6 开发原则）。
--   ② `inventory_sync_record` 补「失败重试 + 错误分类」列（retry_count / last_retry_time / error_category），
--      支撑 §3.3 `POST /api/trade/api-monitor/sync/{id}/retry` 与失败原因归类统计。
--      同时放开 `product_id NOT NULL`——同步记录按 **SKU** 记账（页面 7 列无内部商品），
--      此前渠道推送写记录必然因 product_id 空值失败（表 0 行的根因之一）。
--   ③ 监控阈值/留存落**配置中心** `dms_config`（`monitor.*`，全局默认 + 系统租户双行），
--      保存即热生效；缺失时后端用代码默认值（见 §配置落位）：
--      error-rate / p95-ms / fail-count / sync-fail-count / silence-minutes / auto-refresh-seconds / log.retention-days。
--
-- 只加表列/索引 + 预置配置，**不改菜单**（90107 已存在且组件正确）。
-- =============================================================================

-- ── ① 接口调用日志底表：补齐网关级监控列 ──
ALTER TABLE api_access_log ADD COLUMN IF NOT EXISTS api_name       VARCHAR(100);
ALTER TABLE api_access_log ADD COLUMN IF NOT EXISTS request_id     VARCHAR(64);
ALTER TABLE api_access_log ADD COLUMN IF NOT EXISTS direction      VARCHAR(10) DEFAULT 'IN';
ALTER TABLE api_access_log ADD COLUMN IF NOT EXISTS status         VARCHAR(16) DEFAULT 'SUCCESS';
ALTER TABLE api_access_log ADD COLUMN IF NOT EXISTS error_code     VARCHAR(64);
ALTER TABLE api_access_log ADD COLUMN IF NOT EXISTS error_msg      VARCHAR(500);

COMMENT ON COLUMN api_access_log.api_name    IS '接口名称（URI 模板 + 处理方法的可读名称）';
COMMENT ON COLUMN api_access_log.request_id  IS '调用链请求号（入站回写 X-Request-Id，出站/联调自动生成），可跨系统对账';
COMMENT ON COLUMN api_access_log.direction   IS '方向: IN 外部调用我方开放接口 / OUT 我方调用外部渠道 / SANDBOX 页面联调自检';
COMMENT ON COLUMN api_access_log.status      IS '调用状态: SUCCESS 成功 / FAIL 失败';
COMMENT ON COLUMN api_access_log.error_code  IS '错误码（HTTP 状态或业务错误码）';
COMMENT ON COLUMN api_access_log.error_msg   IS '错误摘要（截断存储，不含凭据）';
COMMENT ON COLUMN api_access_log.response_time IS '耗时(ms)';

-- 查询侧索引：按租户+时间倒序分页、按渠道+时间下钻、按方向+状态统计
CREATE INDEX IF NOT EXISTS idx_api_log_tenant_time  ON api_access_log(tenant_id, access_time DESC);
CREATE INDEX IF NOT EXISTS idx_api_log_channel_time ON api_access_log(channel_code, access_time DESC);
CREATE INDEX IF NOT EXISTS idx_api_log_dir_status   ON api_access_log(direction, status, access_time DESC);

-- ── ② 库存同步记录：失败重试 + 错误分类 ──
ALTER TABLE inventory_sync_record ADD COLUMN IF NOT EXISTS retry_count     INT DEFAULT 0;
ALTER TABLE inventory_sync_record ADD COLUMN IF NOT EXISTS last_retry_time TIMESTAMP;
ALTER TABLE inventory_sync_record ADD COLUMN IF NOT EXISTS error_category  VARCHAR(32);

COMMENT ON COLUMN inventory_sync_record.retry_count     IS '人工/自动重试次数';
COMMENT ON COLUMN inventory_sync_record.last_retry_time IS '最近一次重试时间';
COMMENT ON COLUMN inventory_sync_record.error_category  IS '失败原因分类: NETWORK/AUTH/PARAM/RATE_LIMIT/BIZ_REJECT/UNKNOWN';

-- 同步记录按 SKU 记账（页面 7 列无内部商品），product_id 允许为空
ALTER TABLE inventory_sync_record ALTER COLUMN product_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_inventory_sync_status_time ON inventory_sync_record(sync_status, sync_time DESC);

-- ── ③ 监控阈值 / 留存：预置到配置中心（全局默认行 tenant_id = 0）──
INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time)
VALUES
  (0, 'monitor.threshold.error-rate',        '5',    'API监控：错误率告警线(%)，当日错误率高于该值产生告警（默认 5）', 'TENANT', 0, now(), now()),
  (0, 'monitor.threshold.p95-ms',            '2000', 'API监控：P95 耗时告警线(ms)，当日 P95 高于该值产生告警（默认 2000）', 'TENANT', 0, now(), now()),
  (0, 'monitor.threshold.fail-count',        '20',   'API监控：当日失败次数告警线(次)，超过该值产生告警（默认 20）', 'TENANT', 0, now(), now()),
  (0, 'monitor.threshold.sync-fail-count',   '0',    'API监控：库存同步失败数告警线(条)，超过该值产生告警（0=有失败即告警）', 'TENANT', 0, now(), now()),
  (0, 'monitor.threshold.silence-minutes',   '30',   'API监控：告警静默期(分钟)，同类型告警在静默期内只提示一次、不重复发事件（默认 30）', 'TENANT', 0, now(), now()),
  (0, 'monitor.threshold.auto-refresh-seconds', '30', 'API监控：页面自动刷新间隔(秒)，0=不自动刷新（默认 30）', 'TENANT', 0, now(), now()),
  (0, 'monitor.log.retention-days',          '30',   'API监控：调用日志保留天数(0=不自动清理)（默认 30）', 'TENANT', 0, now(), now())
ON CONFLICT (tenant_id, config_key) WHERE deleted = 0 DO UPDATE
  SET config_value = EXCLUDED.config_value,
      config_desc  = EXCLUDED.config_desc,
      scope        = EXCLUDED.scope,
      update_time  = CURRENT_TIMESTAMP;

-- 同步下发到系统租户(1)，与既有 dms.tracking.* 的双行口径一致
INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time)
SELECT 1, config_key, config_value, config_desc, scope, 0, now(), now()
FROM dms_config
WHERE tenant_id = 0
  AND config_key IN ('monitor.threshold.error-rate', 'monitor.threshold.p95-ms', 'monitor.threshold.fail-count',
                     'monitor.threshold.sync-fail-count',
                     'monitor.threshold.silence-minutes', 'monitor.threshold.auto-refresh-seconds',
                     'monitor.log.retention-days')
  AND deleted = 0
ON CONFLICT (tenant_id, config_key) WHERE deleted = 0 DO UPDATE
  SET config_value = EXCLUDED.config_value,
      update_time  = CURRENT_TIMESTAMP;
