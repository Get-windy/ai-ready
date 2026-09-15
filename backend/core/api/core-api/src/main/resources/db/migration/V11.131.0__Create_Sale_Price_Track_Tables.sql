-- =====================================================================
-- V11.131.0 销售价格跟踪
-- 建表 erp_sale_price_track（商品×往来单位 销售价台账，支持新增/修改/删除/趋势）
-- 一条记录代表一次销售价格点；列表按 product_id+partner_id 分组取"最近一条"；
-- 趋势按 product_id 取全部历史（价格折线 + 悬停显示成交客户）。
-- 种子数据：从销售出库明细 JOIN 出库单头 JOIN 商品主数据 JOIN 往来单位，
--           导入真实历史成交价格点（不含草稿/已取消单据）。
-- =====================================================================

CREATE TABLE IF NOT EXISTS erp_sale_price_track (
    id                BIGINT       NOT NULL,
    tenant_id         BIGINT,
    product_id        BIGINT,
    product_code      VARCHAR(64),
    product_name      VARCHAR(255),
    unit              VARCHAR(32),
    barcode           VARCHAR(64),
    specification     VARCHAR(255),
    model             VARCHAR(64),
    origin            VARCHAR(64),
    partner_id        BIGINT,
    partner_code      VARCHAR(64),
    partner_name      VARCHAR(255),
    sale_price        NUMERIC(18, 4),
    discount_rate     NUMERIC(18, 4),
    sale_date         DATE,
    last_modify_time  TIMESTAMP,
    source            VARCHAR(20),
    remark            VARCHAR(255),
    deleted           SMALLINT     NOT NULL DEFAULT 0,
    create_by         BIGINT,
    create_time       TIMESTAMP,
    update_by         BIGINT,
    update_time       TIMESTAMP,
    CONSTRAINT pk_sale_price_track PRIMARY KEY (id)
);

-- 查询索引：列表分组（product×partner）、名称模糊、日期范围过滤
CREATE INDEX IF NOT EXISTS idx_sale_price_track_product_partner ON erp_sale_price_track (product_id, partner_id);
CREATE INDEX IF NOT EXISTS idx_sale_price_track_partner_name ON erp_sale_price_track (partner_name);
CREATE INDEX IF NOT EXISTS idx_sale_price_track_product_name  ON erp_sale_price_track (product_name);
CREATE INDEX IF NOT EXISTS idx_sale_price_track_sale_date ON erp_sale_price_track (sale_date);
CREATE INDEX IF NOT EXISTS idx_sale_price_track_deleted ON erp_sale_price_track (deleted);

-- 全表存在数据时不重复播种
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM erp_sale_price_track WHERE deleted = 0 LIMIT 1) THEN
        -- 每一条有效销售出库明细 = 一个销售价格点
        -- 商品基础信息优先取明细快照，缺失时回退商品主数据；客户信息优先取出库单快照
        INSERT INTO erp_sale_price_track
            (id, tenant_id, product_id, product_code, product_name,
             unit, barcode, specification, model, origin,
             partner_id, partner_code, partner_name,
             sale_price, discount_rate, sale_date, last_modify_time,
             source, deleted, create_time, update_time)
        SELECT
            -(item.id),
            COALESCE(h.tenant_id, p.tenant_id, 0),
            item.product_id,
            COALESCE(NULLIF(item.product_code, ''), NULLIF(p.product_code_alias, ''), p.product_code),
            COALESCE(NULLIF(item.product_name, ''), p.product_name),
            COALESCE(NULLIF(item.product_unit, ''), NULLIF(item.small_unit, ''), NULLIF(p.unit, '')),
            COALESCE(NULLIF(item.barcode, ''), NULLIF(p.barcode, '')),
            COALESCE(NULLIF(item.specification, ''), NULLIF(p.spec, '')),
            COALESCE(NULLIF(item.model, ''), NULLIF(p.model, '')),
            COALESCE(NULLIF(item.origin, ''), NULLIF(p.origin, '')),
            h.customer_id,
            COALESCE(NULLIF(h.customer_code, ''), NULLIF(bp.party_code, '')),
            COALESCE(NULLIF(h.customer_name, ''), NULLIF(bp.party_name, '')),
            COALESCE(item.unit_price, 0),
            COALESCE(item.discount_rate, 100),
            h.outbound_date::date,
            COALESCE(h.update_time, item.update_time),
            'INIT',
            0,
            item.create_time,
            COALESCE(h.update_time, item.update_time)
        FROM erp_sale_outbound_item item
        JOIN erp_sale_outbound h ON h.id = item.outbound_id AND h.deleted = 0
        LEFT JOIN erp_product p ON p.id = item.product_id AND p.deleted = 0
        LEFT JOIN biz_party bp ON bp.id = h.customer_id AND bp.deleted = 0
        WHERE item.deleted = 0
          AND item.product_id IS NOT NULL
          AND h.customer_id IS NOT NULL
          AND h.status >= 1
          AND h.status <> 12
          AND h.outbound_date IS NOT NULL;
    END IF;
END $$;

-- =====================================================================
-- 备注：
--  - id 使用 -(明细id) 作为种子主键，避免与 MyBatis-Plus ASSIGN_ID（雪花正数）冲突
--  - status >= 1 且 <> 12：排除草稿(0)与已取消(12)，确保为真实成交价
--  - discount_rate 缺省取 100（表示无折扣，百分比语义）
--  - 字段缺失时留空，属真实数据现状，不做假数据填充
-- =====================================================================
