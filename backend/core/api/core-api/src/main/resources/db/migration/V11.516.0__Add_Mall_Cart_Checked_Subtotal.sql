-- ═══════════════════════════════════════════════════════════════════════════════
-- mall_cart 补两列：checked（本行勾选）与 subtotal（行小计）
--
-- 背景（2026-09-26）：实体 `MallCart` 上有 `subtotal` / `checked`，服务层也一直在算
--   小计（`price × quantity`）与设置勾选，但 **V9.34.0 建表时漏了这两列** ⇒
--   这两处写入全部落空（MP 会把不存在的列拼进 SQL，接口直接 500；标记为
--   `@TableField(exist = false)` 之后则变成"写了但不落库"）。用户裁定：**给表加列**，
--   让勾选状态与行小计真正持久化。
--
-- 类型约定：
--   · `checked` 用 **integer 0/1** 而不是 boolean —— 同族表 `mall_address.is_default`
--     也是 integer，且 PostgreSQL **不做 int↔boolean 隐式转换**（此前实体用 Boolean
--     映射 integer 列，插入直接报「类型为 integer 但表达式为 boolean」）。
--   · `subtotal` 用 numeric(14,2)：与 `mall_cart.price`（numeric(12,2)）同精度口径，
--     多留两位整数位容纳"单价 × 大数量"。
-- ═══════════════════════════════════════════════════════════════════════════════

ALTER TABLE mall_cart ADD COLUMN IF NOT EXISTS checked  integer       DEFAULT 1;
ALTER TABLE mall_cart ADD COLUMN IF NOT EXISTS subtotal numeric(14,2);

COMMENT ON COLUMN mall_cart.checked  IS '本行是否勾选（1=勾选 0=未勾选）；结算与库存校验按它过滤';
COMMENT ON COLUMN mall_cart.subtotal IS '行小计 = price × quantity（服务端维护）';

-- 存量行回填（本表此前写入不落库，故多为 NULL；回填成与默认一致的语义）
UPDATE mall_cart SET checked  = 1                WHERE checked  IS NULL;
UPDATE mall_cart SET subtotal = price * quantity WHERE subtotal IS NULL;
