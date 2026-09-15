-- =====================================================
-- V11.9.0: 秒杀场次 (erp-marketing)
--
-- 1. mkt_flash_sale        秒杀场次主表
-- 2. mkt_flash_sale_order  秒杀参与/下单记录
--
-- status 口径: 0=草稿 1=已发布 2=已取消 3=已结束
-- sold_count 由参与记录(mkt_flash_sale_order)累计，不手工维护
-- =====================================================

-- ═══════════════════════════════════════════════════════
-- 1. 秒杀场次主表
-- ═══════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS mkt_flash_sale (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    title           VARCHAR(200)    NOT NULL,

    -- 商品信息(冗余快照)
    product_id      BIGINT,
    product_code    VARCHAR(50),
    product_name    VARCHAR(200),

    -- 价格与库存
    flash_price     DECIMAL(18,2)   NOT NULL DEFAULT 0,
    original_price  DECIMAL(18,2)   DEFAULT 0,
    stock_limit     INT             NOT NULL DEFAULT 0,
    sold_count      INT             NOT NULL DEFAULT 0,

    -- 时间窗口
    start_time      TIMESTAMP       NOT NULL,
    end_time        TIMESTAMP       NOT NULL,

    -- 0=草稿 1=已发布 2=已取消 3=已结束
    status          INT             NOT NULL DEFAULT 0,
    sort            INT             DEFAULT 0,
    remark          TEXT,

    -- 审计列
    create_by       BIGINT,
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT             NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_mkt_flash_tenant  ON mkt_flash_sale(tenant_id);
CREATE INDEX IF NOT EXISTS idx_mkt_flash_status  ON mkt_flash_sale(status);
CREATE INDEX IF NOT EXISTS idx_mkt_flash_product ON mkt_flash_sale(product_id);
CREATE INDEX IF NOT EXISTS idx_mkt_flash_time    ON mkt_flash_sale(start_time, end_time);

COMMENT ON TABLE  mkt_flash_sale IS '秒杀场次主表';
COMMENT ON COLUMN mkt_flash_sale.status IS '状态: 0=草稿 1=已发布 2=已取消 3=已结束';
COMMENT ON COLUMN mkt_flash_sale.stock_limit IS '秒杀限量库存';
COMMENT ON COLUMN mkt_flash_sale.sold_count IS '已售数量(由参与记录累计)';

-- ═══════════════════════════════════════════════════════
-- 2. 秒杀参与/下单记录
-- ═══════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS mkt_flash_sale_order (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    session_id      BIGINT          NOT NULL,
    order_id        BIGINT,
    customer_id     BIGINT,
    customer_name   VARCHAR(200),
    quantity        DECIMAL(18,4)   NOT NULL DEFAULT 1,
    amount          DECIMAL(18,2)   NOT NULL DEFAULT 0,
    -- 参与状态: 0=已参与 1=已下单 2=已取消
    status          INT             NOT NULL DEFAULT 0,
    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mkt_fso_tenant  ON mkt_flash_sale_order(tenant_id);
CREATE INDEX IF NOT EXISTS idx_mkt_fso_session ON mkt_flash_sale_order(session_id);
CREATE INDEX IF NOT EXISTS idx_mkt_fso_order   ON mkt_flash_sale_order(order_id);

COMMENT ON TABLE  mkt_flash_sale_order IS '秒杀参与/下单记录';
COMMENT ON COLUMN mkt_flash_sale_order.session_id IS '秒杀场次ID(mkt_flash_sale.id)';
COMMENT ON COLUMN mkt_flash_sale_order.status IS '参与状态: 0=已参与 1=已下单 2=已取消';

-- ═══════════════════════════════════════════════════════
-- 示例数据 (tenant_id=1, 固定ID幂等, 可安全删除)
-- ═══════════════════════════════════════════════════════
INSERT INTO mkt_flash_sale (id, tenant_id, title, product_id, product_code, product_name,
                            flash_price, original_price, stock_limit, sold_count,
                            start_time, end_time, status, sort, remark)
VALUES (1, 1, '【示例】限时秒杀-酒酿果味酱罐头', 2073239284579586050, 'SP-20260704-041', '博多新米坊酒酿果味酱罐头',
        9.90, 19.90, 100, 0,
        CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '7 day', 1, 1, '示例数据-进行中场次')
ON CONFLICT (id) DO NOTHING;

INSERT INTO mkt_flash_sale (id, tenant_id, title, product_id, product_code, product_name,
                            flash_price, original_price, stock_limit, sold_count,
                            start_time, end_time, status, sort, remark)
VALUES (2, 1, '【示例】秒杀草稿-待发布', 2073239284579586050, 'SP-20260704-041', '博多新米坊酒酿果味酱罐头',
        8.80, 19.90, 50, 0,
        CURRENT_TIMESTAMP + INTERVAL '7 day', CURRENT_TIMESTAMP + INTERVAL '14 day', 0, 2, '示例数据-草稿场次')
ON CONFLICT (id) DO NOTHING;
