-- =============================================================================
-- 库存数量不变量约束（2026-09-21）
--
-- 【为什么】STK-BREAK-03 实测到 `wms_inventory` 存在**自相矛盾**的行：
--     · product_id=2079717784150593538  quantity=31  而 available_quantity=43
--     · product_id=9909046245377695746  quantity=0   而 available_quantity=152
--   「可用量 > 总量」会让超卖校验放行**并不存在的库存** —— 这正是两轨漂移
--   （STK-BREAK-01：erp_stock 可用 95 vs wms_inventory 可用 43）的伴生症状。
--
--   历史脏数据已由 `V11.437.0__Reconcile_Wms_Inventory_To_Erp_Stock.sql` 校准
--   （按用户决策「以 erp_stock 为准」把 WMS 轨对齐 ERP 轨）。
--   本迁移负责**防再生**：把不变量下沉到数据库，而不是只靠调用方自觉。
--
--   ⚠️ 只靠 Service 断言不够：本项目存在**多条写入路径**（正向走 InventoryService、
--      反向/取消路径曾被直写、运维脚本与手工 SQL 亦可写），任何一条漏掉断言就会
--      重新引入矛盾数据。约束是唯一能覆盖全部写入方的收口点。
--
-- 【口径】
--   · CHECK 对 NULL 求值为 NULL ⇒ **不阻断** NULL 行（PostgreSQL 语义），
--     所以本迁移不会因为历史 NULL 值而失败，也不会强制列非空。
--   · 加约束时 PostgreSQL 会校验既有全表行 —— 已实测两表均 0 违规（2026-09-21）：
--       wms_inventory 3 行 / erp_stock 4 行，可用大于总量 = 0、负数 = 0。
--   · 用 `DO` 块 + `pg_constraint` 判存在，使本迁移**幂等**（重跑不报 constraint already exists）。
--
-- 【施工顺序】必须先有 V11.437.0 的数据校准，再有本约束；顺序反了会在有脏数据时加约束失败。
-- =============================================================================

DO $$
BEGIN
    -- ── wms_inventory（WMS 轨）──
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_wms_inventory_avail_le_qty') THEN
        ALTER TABLE wms_inventory
            ADD CONSTRAINT ck_wms_inventory_avail_le_qty CHECK (available_quantity <= quantity);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_wms_inventory_qty_nonneg') THEN
        ALTER TABLE wms_inventory
            ADD CONSTRAINT ck_wms_inventory_qty_nonneg CHECK (quantity >= 0);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_wms_inventory_avail_nonneg') THEN
        ALTER TABLE wms_inventory
            ADD CONSTRAINT ck_wms_inventory_avail_nonneg CHECK (available_quantity >= 0);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_wms_inventory_frozen_nonneg') THEN
        ALTER TABLE wms_inventory
            ADD CONSTRAINT ck_wms_inventory_frozen_nonneg CHECK (frozen_quantity >= 0);
    END IF;

    -- ── erp_stock（ERP 轨，当前为准的一轨）──
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_erp_stock_avail_le_qty') THEN
        ALTER TABLE erp_stock
            ADD CONSTRAINT ck_erp_stock_avail_le_qty CHECK (available_quantity <= quantity);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_erp_stock_qty_nonneg') THEN
        ALTER TABLE erp_stock
            ADD CONSTRAINT ck_erp_stock_qty_nonneg CHECK (quantity >= 0);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_erp_stock_avail_nonneg') THEN
        ALTER TABLE erp_stock
            ADD CONSTRAINT ck_erp_stock_avail_nonneg CHECK (available_quantity >= 0);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_erp_stock_frozen_nonneg') THEN
        ALTER TABLE erp_stock
            ADD CONSTRAINT ck_erp_stock_frozen_nonneg CHECK (frozen_quantity >= 0);
    END IF;
END $$;
