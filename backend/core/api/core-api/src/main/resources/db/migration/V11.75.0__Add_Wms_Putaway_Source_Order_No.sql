-- 上架任务补来源单号，便于按来源单号溯源/检索
ALTER TABLE wms_putaway_task ADD COLUMN IF NOT EXISTS source_order_no VARCHAR(64);
COMMENT ON COLUMN wms_putaway_task.source_order_no IS '来源单号（收货单/移库单等上游单据编号）';
