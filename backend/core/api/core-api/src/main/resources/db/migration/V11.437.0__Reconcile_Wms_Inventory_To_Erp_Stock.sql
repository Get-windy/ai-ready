-- =============================================================================
-- 库存双轨校准：以 erp_stock 为准，把 wms_inventory 对齐过来（2026-09-20）
--
-- 【背景】项目存在两套库存账：
--   · erp_stock      —— ERP 轨，仓库级汇总账，**所有库存类报表/可用库存/缺货预警/智能补货的唯一数据源**
--   · wms_inventory  —— WMS 轨，带库位/批次维度，仓库现场作业依据，且有 wms_inventory_log 流水可回溯
--
--   `InventoryServiceImpl.decrease()` 在镜像扣减失败时**只记 error 不回滚**
--   （原注释理由是"避免历史漂移卡死仓库现场作业"），叠加多处反向路径直写 erp_stock
--   ⇒ 两轨产生单向漂移。devdb 实测（2026-09-20）：
--       product_id=2073239284579586050, warehouse_id=1
--       erp_stock.available_quantity = 95  vs  wms_inventory.available_quantity = 43  （差 52）
--   且该 WMS 行自身矛盾：quantity=31 < available_quantity=43（可用量大于总量）。
--
-- 【决策】用户 2026-09-20 明确：**以 erp_stock 为准**（ERP 是系统核心）。
--   配套代码修复：`InventoryServiceImpl.decrease()` 的镜像失败已改为抛异常回滚（不再只记日志），
--   从源头停止新的漂移。
--
-- 【本迁移做什么】把 wms_inventory 的 quantity / available_quantity 按 (product_id, warehouse_id, batch_no)
--   对齐到 erp_stock。**仅处理单行 key**：同一 key 在 WMS 有多行（多库位/多批次明细）时**跳过**，
--   因为 erp_stock 是仓库级汇总，自动摊到多行会改错，这类差异留给人工按库位对账。
--
-- 【幂等】只更新数值不一致的行，可重复执行。
-- 【可回滚性】如需回滚：本迁移之前两轨差异行的原值（43/31）见上方【背景】记录。
-- =============================================================================

UPDATE wms_inventory w
SET quantity = e.quantity,
    available_quantity = e.available_quantity
FROM erp_stock e
WHERE w.deleted = 0
  AND e.deleted = 0
  AND w.location_id IS NULL
  AND e.product_id = w.product_id
  AND e.warehouse_id = w.warehouse_id
  AND COALESCE(e.batch_no, '') = COALESCE(w.batch_no, '')
  AND (w.quantity IS DISTINCT FROM e.quantity
       OR w.available_quantity IS DISTINCT FROM e.available_quantity)
  AND (
      SELECT COUNT(*)
      FROM wms_inventory x
      WHERE x.deleted = 0
        AND x.product_id = w.product_id
        AND x.warehouse_id = w.warehouse_id
        AND COALESCE(x.batch_no, '') = COALESCE(w.batch_no, '')
  ) = 1;
