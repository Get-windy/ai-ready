-- 订单池（配送 → 调度管理 → 订单池，菜单 80860 / `dms:order-pool`）金标准二轮复验 · 数据侧（PostgreSQL）
--   文档：《订单池开发文档》§3.5 业界生产级实践 3「池生命周期：发布时限、过期回流、**下架原因留痕**」
--
--   背景：`POST /api/dms/order-pool/{id}/offline` 一直接收 `reason` 参数，但服务端只写日志、
--   未落库——用户填写的下架原因**静默丢失**，事后无法追溯「这条池为什么被下架」。
--   本迁移只补 2 列审计字段 + 1 个查询索引，不改状态机、不改菜单、不改既有列语义。
--
--   幂等：IF NOT EXISTS。

ALTER TABLE dms_order_pool ADD COLUMN IF NOT EXISTS offline_reason VARCHAR(200);
ALTER TABLE dms_order_pool ADD COLUMN IF NOT EXISTS offline_time   TIMESTAMP;

COMMENT ON COLUMN dms_order_pool.offline_reason IS '下架原因（人工下架时填写，供事后追溯）';
COMMENT ON COLUMN dms_order_pool.offline_time   IS '下架时间';

-- 发布入池前需按 (task_id, pool_status) 判重（同一任务不得同时存在活跃池记录）
CREATE INDEX IF NOT EXISTS idx_dms_order_pool_task_status
    ON dms_order_pool (tenant_id, task_id, pool_status);
