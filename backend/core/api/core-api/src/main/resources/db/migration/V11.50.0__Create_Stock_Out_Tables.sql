-- ═══════════════════════════════════════════════════════════════════
-- V11.50.0 其他出库单（库存出库：领用/赠送/样品/盘亏/其他）
-- 与其他入库单互为反向单据。单号前缀 QTCKD-
-- ═══════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS erp_stock_out (
    id                BIGINT       NOT NULL,
    tenant_id         BIGINT       NOT NULL DEFAULT 1,
    stock_out_no      VARCHAR(64)  NOT NULL,
    stock_out_date    DATE         NOT NULL,
    stock_out_type    SMALLINT     NOT NULL DEFAULT 5,
    partner_id        BIGINT,
    partner_code      VARCHAR(64),
    partner_name      VARCHAR(200),
    warehouse_id      BIGINT       NOT NULL,
    warehouse_name    VARCHAR(100),
    handler_id        BIGINT,
    handler_name      VARCHAR(64),
    dept_id           BIGINT,
    dept_name         VARCHAR(100),
    total_quantity    NUMERIC(18, 4) DEFAULT 0,
    total_amount      NUMERIC(18, 2) DEFAULT 0,
    total_weight      NUMERIC(18, 4) DEFAULT 0,
    total_volume      NUMERIC(18, 4) DEFAULT 0,
    total_items       INT          DEFAULT 0,
    status            SMALLINT     NOT NULL DEFAULT 0,
    summary           VARCHAR(500),
    remark            VARCHAR(500),
    attachment        VARCHAR(1000),
    bookkeeper_id     BIGINT,
    bookkeeper_name   VARCHAR(64),
    bookkeeping_time  TIMESTAMP,
    creator_name      VARCHAR(64),
    print_count       INT          DEFAULT 0,
    applicant_id      BIGINT,
    applicant_name    VARCHAR(64),
    apply_time        TIMESTAMP,
    approved_by       BIGINT,
    approved_time     TIMESTAMP,
    approved_note     VARCHAR(200),
    executed_by       BIGINT,
    executed_time     TIMESTAMP,
    cancel_reason     VARCHAR(200),
    deleted           SMALLINT     NOT NULL DEFAULT 0,
    create_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    create_by         BIGINT,
    update_by         BIGINT,
    CONSTRAINT pk_stock_out PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_stock_out_tenant_status ON erp_stock_out (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_stock_out_no          ON erp_stock_out (stock_out_no);
CREATE INDEX IF NOT EXISTS idx_stock_out_date        ON erp_stock_out (stock_out_date);
CREATE INDEX IF NOT EXISTS idx_stock_out_partner     ON erp_stock_out (partner_id);
CREATE INDEX IF NOT EXISTS idx_stock_out_warehouse   ON erp_stock_out (warehouse_id);

CREATE TABLE IF NOT EXISTS erp_stock_out_item (
    id                   BIGINT       NOT NULL,
    stock_out_id         BIGINT       NOT NULL,
    product_id           BIGINT       NOT NULL,
    product_code         VARCHAR(64),
    product_name         VARCHAR(200),
    product_spec         VARCHAR(100),
    product_unit         VARCHAR(20),
    barcode              VARCHAR(64),
    model                VARCHAR(64),
    origin               VARCHAR(64),
    brand                VARCHAR(64),
    location             VARCHAR(64),
    item_ext_num1        NUMERIC(18, 4),
    item_ext_num2        NUMERIC(18, 4),
    item_ext_num3        NUMERIC(18, 4),
    item_ext_text1       VARCHAR(200),
    item_ext_text2       VARCHAR(200),
    batch_code           VARCHAR(64),
    production_date      DATE,
    shelf_life           VARCHAR(50),
    expiry_date          DATE,
    quantity             NUMERIC(18, 4) NOT NULL DEFAULT 0,
    conversion_relation  VARCHAR(100),
    conversion_result    NUMERIC(18, 4),
    piece_quantity       NUMERIC(18, 4),
    big_pack             NUMERIC(18, 4),
    mid_pack             NUMERIC(18, 4),
    small_pack           NUMERIC(18, 4),
    small_unit           VARCHAR(20),
    small_unit_quantity  NUMERIC(18, 4),
    small_unit_price     NUMERIC(18, 2),
    unit_price           NUMERIC(18, 2) DEFAULT 0,
    amount               NUMERIC(18, 2) DEFAULT 0,
    retail_price         NUMERIC(18, 2),
    wholesale_price      NUMERIC(18, 2),
    price_level1         NUMERIC(18, 2),
    price_level2         NUMERIC(18, 2),
    price_level3         NUMERIC(18, 2),
    price_level4         NUMERIC(18, 2),
    price_level5         NUMERIC(18, 2),
    price_level6         NUMERIC(18, 2),
    price_level7         NUMERIC(18, 2),
    price_level8         NUMERIC(18, 2),
    weight               NUMERIC(18, 4),
    volume               NUMERIC(18, 4),
    available_stock      NUMERIC(18, 4),
    remark               VARCHAR(500),
    deleted              SMALLINT     NOT NULL DEFAULT 0,
    create_time          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_stock_out_item PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_stock_out_item_parent  ON erp_stock_out_item (stock_out_id);
CREATE INDEX IF NOT EXISTS idx_stock_out_item_product ON erp_stock_out_item (product_id);
CREATE INDEX IF NOT EXISTS idx_stock_out_item_barcode ON erp_stock_out_item (barcode);

-- ═══════════════════════════════════════════════════════════════════
-- 其他出库单(5001) 菜单双入口对齐：主菜单→新增表单，[历史]→列表
-- 与其他入库单 5002 保持一致
-- ═══════════════════════════════════════════════════════════════════
UPDATE sys_menu SET
  path = 'erp/stock-out/form',
  component = 'erp/stock-out/form',
  list_path = 'erp/stock-out/index',
  tag_label = '历史',
  display_mode = 1,
  update_time = CURRENT_TIMESTAMP
WHERE id = 5001;
