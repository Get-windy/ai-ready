-- ============================================================================
-- V11.157.0 商品货位设置（资料 → 仓库管理 → 商品货位设置，菜单 70520 / md:location）
-- 2026-09-11 对标 ql361 GoodsGPositionList 从零复刻
--
-- 视角：按「商品 × 仓库」维护推荐货位（不是货位清单页）。
--   对标源码证据 tool-results/ql361/mdloc-shots/js-GoodsGPositionList.js：
--     - 列表主体为商品（erp_product），逐商品显示其在所选仓库下的推荐货位 gpcode
--     - 写端点 setgoodsposition(goodssetpoint, goods[], gpid) / batchremove(goodssetpoint, goods[])
--     - 查询条件 sid(仓库,必填) + gpid/code(货位) + onlyUnsettedGoods + onlyStockGoods
--
-- 货位口径（红线）：货位为全局基础数据，**复用 wms_location，严禁另建货位表**；
--   本表仅存「商品 → 货位」的推荐绑定关系（商品-货位关联），不复制货位属性。
--   仓库口径统一 erp_warehouse.id（与 wms_location.warehouse_id 同源）。
-- ============================================================================

CREATE TABLE IF NOT EXISTS erp_product_location (
    id              BIGINT       PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL DEFAULT 0,
    warehouse_id    BIGINT       NOT NULL,
    product_id      BIGINT       NOT NULL,
    location_id     BIGINT       NOT NULL,
    location_code   VARCHAR(100),
    remark          VARCHAR(500),
    deleted         INTEGER      NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  erp_product_location               IS '商品推荐货位（商品×仓库→货位绑定，货位主数据见 wms_location）';
COMMENT ON COLUMN erp_product_location.warehouse_id  IS '仓库ID（erp_warehouse.id，与 wms_location.warehouse_id 同源）';
COMMENT ON COLUMN erp_product_location.product_id    IS '商品ID（erp_product.id）';
COMMENT ON COLUMN erp_product_location.location_id   IS '货位ID（wms_location.id，全局唯一货位口径）';
COMMENT ON COLUMN erp_product_location.location_code IS '货位编码快照（列表「推荐货位」列显示，避免每次 JOIN）';
COMMENT ON COLUMN erp_product_location.remark        IS '备注（对标 gpremark 列）';

-- 一个商品在同一仓库下只保留一条推荐货位绑定（软删除记录不占用唯一性）
CREATE UNIQUE INDEX IF NOT EXISTS uk_epl_product_warehouse
    ON erp_product_location (tenant_id, product_id, warehouse_id)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_epl_tenant    ON erp_product_location (tenant_id);
CREATE INDEX IF NOT EXISTS idx_epl_product   ON erp_product_location (product_id);
CREATE INDEX IF NOT EXISTS idx_epl_warehouse ON erp_product_location (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_epl_location  ON erp_product_location (location_id);
CREATE INDEX IF NOT EXISTS idx_epl_deleted   ON erp_product_location (deleted);

-- 迁移内自验证
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'erp_product_location') THEN
        RAISE EXCEPTION 'erp_product_location 未创建成功';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'erp_product_location' AND column_name = 'tenant_id') THEN
        RAISE EXCEPTION 'erp_product_location.tenant_id 缺失（多租户插件会注入该列）';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'erp_product_location' AND column_name = 'location_code') THEN
        RAISE EXCEPTION 'erp_product_location.location_code 缺失（列表推荐货位列依赖）';
    END IF;
END $$;
