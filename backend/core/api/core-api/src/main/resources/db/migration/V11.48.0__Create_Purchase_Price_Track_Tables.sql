-- =====================================================================
-- V11.48.0 采购价格跟踪
-- 建表 erp_purchase_price_track（商品×往来单位 采购价台账，支持新增/修改/删除/趋势）
-- 一条记录代表一次采购价格点；列表按 product_id+partner_id 分组取"最近一条"；
-- 趋势按 product_id 取全部历史（价格折线 + 悬停显示成交供应商）。
-- 种子数据：从采购订单明细 JOIN 商品主数据 JOIN 往来单位，导入真实历史价格点。
-- =====================================================================

CREATE TABLE IF NOT EXISTS erp_purchase_price_track (
    id                BIGINT       NOT NULL,
    tenant_id         BIGINT,
    product_id        BIGINT,
    product_code      VARCHAR(64),
    product_name      VARCHAR(255),
    item_code         VARCHAR(64),
    unit              VARCHAR(32),
    specification     VARCHAR(255),
    model             VARCHAR(64),
    origin            VARCHAR(64),
    barcode           VARCHAR(64),
    partner_id        BIGINT,
    partner_code      VARCHAR(64),
    partner_name      VARCHAR(255),
    purchase_price    NUMERIC(18, 4),
    purchase_date     DATE,
    last_modify_time  TIMESTAMP,
    source            VARCHAR(20),
    remark            VARCHAR(255),
    deleted           SMALLINT     NOT NULL DEFAULT 0,
    create_by         BIGINT,
    create_time       TIMESTAMP,
    update_by         BIGINT,
    update_time       TIMESTAMP,
    CONSTRAINT pk_purchase_price_track PRIMARY KEY (id)
);

-- 查询索引：列表分组（product×partner）、分类过滤、价格/日期范围过滤
CREATE INDEX IF NOT EXISTS idx_price_track_product_partner ON erp_purchase_price_track (product_id, partner_id);
CREATE INDEX IF NOT EXISTS idx_price_track_partner_name ON erp_purchase_price_track (partner_name);
CREATE INDEX IF NOT EXISTS idx_price_track_product_name  ON erp_purchase_price_track (product_name);
CREATE INDEX IF NOT EXISTS idx_price_track_purchase_date ON erp_purchase_price_track (purchase_date);
CREATE INDEX IF NOT EXISTS idx_price_track_deleted ON erp_purchase_price_track (deleted);

-- 全表存在数据时不重复播种
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM erp_purchase_price_track WHERE deleted = 0 LIMIT 1) THEN
        -- 从采购订单明细 JOIN 采购订单头 JOIN 商品主数据 JOIN 往来单位
        -- 导入每一条采购明细作为一个价格点（商品基础信息以主数据为准，单位/编号缺失时回退）
        INSERT INTO erp_purchase_price_track
            (id, tenant_id, product_id, product_code, product_name,
             item_code, unit, specification, model, origin, barcode,
             partner_id, partner_code, partner_name,
             purchase_price, purchase_date, last_modify_time,
             source, deleted, create_time, update_time)
        SELECT
            -(item.id),
            COALESCE(h.tenant_id, p.tenant_id, 0),
            item.product_id,
            item.product_code,
            COALESCE(NULLIF(item.product_name, ''), p.product_name),
            COALESCE(NULLIF(item.item_code, ''), NULLIF(p.product_code_alias, ''), p.product_code)  AS item_code,
            COALESCE(NULLIF(item.unit, ''), NULLIF(p.unit, '')),
            COALESCE(NULLIF(item.specification, ''), NULLIF(p.spec, '')),
            COALESCE(NULLIF(item.model, ''), NULLIF(p.model, '')),
            COALESCE(NULLIF(item.origin, ''), NULLIF(p.origin, '')),
            COALESCE(NULLIF(item.barcode, ''), NULLIF(p.barcode, '')),
            h.supplier_id,
            COALESCE(NULLIF(pp.party_code, ''), NULLIF(s.supplier_code, '')),
            COALESCE(NULLIF(h.supplier_name, ''), NULLIF(pp.party_name, ''), NULLIF(s.supplier_name, '')),
            COALESCE(item.unit_price, 0),
            h.order_date::date,
            COALESCE(h.update_time, item.update_time),
            'INIT',
            0,
            item.create_time,
            COALESCE(h.update_time, item.update_time)
        FROM erp_purchase_order_item item
        JOIN erp_purchase_order h ON h.id = item.order_id AND h.deleted = 0
        LEFT JOIN erp_product p ON p.id = item.product_id AND p.deleted = 0
        LEFT JOIN biz_party pp ON pp.id = h.supplier_id AND pp.deleted = 0
        LEFT JOIN erp_purchase_order_partner_snapshot s ON s.order_id = h.id
        WHERE item.product_id IS NOT NULL
          AND h.supplier_id IS NOT NULL
          AND h.status >= 1
          AND COALESCE(h.closed_flag, 0) = 0
          AND COALESCE(h.cancellation_flag, 0) = 0;
    END IF;
END $$;

-- =====================================================================
-- 备注：
--  - id 使用 -(明细id) 作为种子主键，避免与 MyBatis-Plus ASSIGN_ID（雪花正数）冲突
--  - unit/supplier 等字段缺失时该行为空，属真实数据现状，不做假数据填充
--  - status >= 1：已生效采购订单（排除草稿 0）；closed/cancelled 也排除，确保为有效成交价
-- =====================================================================
