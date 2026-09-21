-- ═══════════════════════════════════════════════════════════════════════
-- STK-BREAK-04：回填 erp_stock 的展示快照列（2026-09-21）
--
-- 背景：erp_stock 带一组冗余的展示列（商品名/编码/单位、仓库名）。实测存量行
-- 这些列 **100% 为 NULL**，因为写入口的口径不一致：
--   * recordStockIn / saveInitialStock 会带上这些值（调用方如报溢单确实传了）
--   * increaseStock / checkStock 只收 productId + warehouseId + quantity，
--     调用方（wms 的 ERP 轨镜像、盘点调整）**无从传入**
--     ⇒ 新建的行天然是空快照，而期初库存列表的「仓库」列是**直接取快照**的
--       （InitialStockQueryMapper:60 没有 COALESCE）
--
-- 本迁移只做**存量回填**；写入侧的根治见 StockServiceImpl.fillSnapshot()
-- （只补空列、不覆盖调用方已给的值，已完整的行零成本）。
--
-- 幂等：两个 UPDATE 都以「该列为空」为条件，重复执行不会改变任何行。
-- 不处理 supplier_id / supplier_name：这两个值无法从商品/仓库档案推导，
-- 只能由单据传入，而当前没有任何调用方传（功能缺口，已在 MASTER_TODO 登记）。
-- ═══════════════════════════════════════════════════════════════════════

-- 商品侧快照：名称 / 编码 / 单位
UPDATE erp_stock s
   SET product_name = COALESCE(NULLIF(s.product_name, ''), p.product_name),
       product_code = COALESCE(NULLIF(s.product_code, ''), p.product_code),
       unit         = COALESCE(NULLIF(s.unit, ''), p.unit)
  FROM erp_product p
 WHERE p.id = s.product_id
   AND p.deleted = 0
   -- 租户必须一致（含两边都为 NULL）：erp_product 存在小整数 id 的历史行，
   -- 只按 id 关联在多租户下有取到别家商品名的风险
   AND p.tenant_id IS NOT DISTINCT FROM s.tenant_id
   AND (s.product_name IS NULL OR s.product_name = ''
        OR s.product_code IS NULL OR s.product_code = ''
        OR s.unit IS NULL OR s.unit = '');

-- 仓库侧快照：名称（erp_warehouse 的 id=1/2 就是"主仓库/华东分仓"这类小整数，故租户条件不可省）
UPDATE erp_stock s
   SET warehouse_name = COALESCE(NULLIF(s.warehouse_name, ''), w.warehouse_name)
  FROM erp_warehouse w
 WHERE w.id = s.warehouse_id
   AND w.deleted = 0
   AND w.tenant_id IS NOT DISTINCT FROM s.tenant_id
   AND (s.warehouse_name IS NULL OR s.warehouse_name = '');
