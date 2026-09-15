-- 库存同步记录表列对齐（配送 → API监控 页「库存同步记录」表格）
--
-- 背景：实体 cn.aiedge.trade.entity.InventorySyncRecord 含 @TableLogic deleted 字段，
--   但建表时漏了该列 → MyBatis-Plus 生成 `WHERE deleted = 0` 直接报
--   「字段 "deleted" 不存在」，`GET /api/trade/inventory-sync/page` 恒 500。
-- 处理：补齐审计列 + 查询索引（幂等）。

ALTER TABLE inventory_sync_record ADD COLUMN IF NOT EXISTS deleted INTEGER DEFAULT 0;
ALTER TABLE inventory_sync_record ADD COLUMN IF NOT EXISTS update_time TIMESTAMP DEFAULT NOW();
ALTER TABLE inventory_sync_record ADD COLUMN IF NOT EXISTS create_by BIGINT;
ALTER TABLE inventory_sync_record ADD COLUMN IF NOT EXISTS update_by BIGINT;

COMMENT ON COLUMN inventory_sync_record.deleted IS '逻辑删除 0-未删除 1-已删除（多租户+逻辑删除插件依赖）';

CREATE INDEX IF NOT EXISTS idx_inventory_sync_record_tenant ON inventory_sync_record (tenant_id, sync_time);
CREATE INDEX IF NOT EXISTS idx_inventory_sync_record_channel ON inventory_sync_record (channel_code);
CREATE INDEX IF NOT EXISTS idx_inventory_sync_record_sku ON inventory_sync_record (sku_code);
