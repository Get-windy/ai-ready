-- ============================================================================
-- 销售订单金标准增强（订单处理中心 · 拣货/发货 Tab）
--
-- 背景：拣货/发货 Tab 表格列 37 项，其中三列在库内无真实落点，页面只能恒空：
--   1) 已拣货数量 / 未拣货数量 —— 拣货作业的真实结果量，与「已发货数量」是两个口径
--      （先拣后发，可分批），原表仅有 shipped_quantity（已发），无法表达拣货进度。
--   2) 排序 / 排序值 —— 拣货顺序的人工重排依据（按线路/货位优化拣货路径），原表无列。
--
-- 口径说明（非冗余）：
--   - erp_sale_order_item.picked_quantity  明细级已拣数量，由拣货作业回写；
--   - erp_sale_order.picked_quantity       主表汇总，= Σ 明细已拣数量；
--   - 未拣货数量、未发货数量均为派生值（数量 − 已拣 / 数量 − 已发），不落列，避免双写不一致；
--   - sort_order  排序序号（人工调整拣货顺序），sort_value 排序值（二级排序权重）。
--
-- 全部 ADD COLUMN IF NOT EXISTS / CREATE INDEX IF NOT EXISTS，可重复执行。
-- ============================================================================

-- ------------------------------------------------------------
-- 1. 主表：拣货汇总 + 拣货排序
-- ------------------------------------------------------------
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS picked_quantity numeric(18,4) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS sort_order integer;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS sort_value integer;

COMMENT ON COLUMN erp_sale_order.picked_quantity IS '已拣货数量汇总（= Σ 明细 picked_quantity，由拣货作业回写）';
COMMENT ON COLUMN erp_sale_order.sort_order IS '排序（拣货顺序序号）';
COMMENT ON COLUMN erp_sale_order.sort_value IS '排序值（拣货顺序二级权重）';

-- ------------------------------------------------------------
-- 2. 明细表：明细级已拣数量
-- ------------------------------------------------------------
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS picked_quantity numeric(18,4) DEFAULT 0;

COMMENT ON COLUMN erp_sale_order_item.picked_quantity IS '已拣货数量（明细级，拣货作业回写）';

-- ------------------------------------------------------------
-- 3. 存量校准：主表汇总与明细保持一致（未拣货的历史单据保持 0，不臆造拣货结果）
-- ------------------------------------------------------------
UPDATE erp_sale_order o
SET picked_quantity = COALESCE(agg.picked, 0)
FROM (
    SELECT order_id, SUM(COALESCE(picked_quantity, 0)) AS picked
    FROM erp_sale_order_item
    GROUP BY order_id
) agg
WHERE o.id = agg.order_id
  AND COALESCE(o.picked_quantity, 0) IS DISTINCT FROM COALESCE(agg.picked, 0);

-- ------------------------------------------------------------
-- 4. 拣货/发货 Tab 查询索引
-- ------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_sale_order_picking_status
    ON erp_sale_order (tenant_id, status, warehouse_id);
CREATE INDEX IF NOT EXISTS idx_sale_order_sort_order
    ON erp_sale_order (sort_order);
