-- =============================================================================
-- V11.360.2 · 补齐 `external_order_raw.deleted`（实体/表结构不一致致该表查询 500）
--
-- 背景：`ExternalOrderRaw` 实体声明 `@TableLogic deleted`（V8.14.0 建表时未建该列，
-- 后续迁移也未补），MyBatis-Plus 会在所有内置查询里追加 `deleted = 0` →
-- `SELECT ... deleted FROM external_order_raw WHERE deleted = 0` 直接报
-- 「字段 "deleted" 不存在」，表现为**开放接口订单回调（/api/open/order/callback/{channelCode}）500**
-- （由《API监控》联调沙箱回环调用暴露）。
--
-- 处理口径与 `inventory_sync_record`（V11.213.0）一致：**补列对齐实体**，而非删除 @TableLogic。
-- 同时给手写 SQL 补 `deleted = 0`（见 ExternalOrderRawMapper）。
-- =============================================================================

ALTER TABLE external_order_raw ADD COLUMN IF NOT EXISTS deleted INT DEFAULT 0;
COMMENT ON COLUMN external_order_raw.deleted IS '逻辑删除: 0 正常 / 1 已删除';

CREATE INDEX IF NOT EXISTS idx_external_order_raw_channel_alive
  ON external_order_raw(channel_code, external_order_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_external_order_raw_pending
  ON external_order_raw(process_status, receive_time) WHERE deleted = 0;
