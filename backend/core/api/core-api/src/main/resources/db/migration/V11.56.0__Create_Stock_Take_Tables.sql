-- ═══════════════════════════════════════════════════════════════════
-- V11.56.0 盘点单（库存盘点录单）
-- 主菜单/[历史]双入口。单号前缀 KCPDD-
-- 保存 -> 盘点处理（按盈亏生成报损单/报溢单）
-- ═══════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS erp_stock_take (
    id                  BIGINT       NOT NULL,
    tenant_id           BIGINT       NOT NULL DEFAULT 1,
    stock_take_no       VARCHAR(64)  NOT NULL,
    stock_take_date     DATE         NOT NULL,
    check_method        SMALLINT     NOT NULL DEFAULT 1,
    check_type          SMALLINT     NOT NULL DEFAULT 1,
    warehouse_id        BIGINT       NOT NULL,
    warehouse_name      VARCHAR(100),
    region_name         VARCHAR(100),
    handler_id          BIGINT,
    handler_name        VARCHAR(64),
    dept_id             BIGINT,
    dept_name           VARCHAR(100),
    total_diff_quantity NUMERIC(18, 4) DEFAULT 0,
    total_diff_amount   NUMERIC(18, 2) DEFAULT 0,
    total_items         INT          DEFAULT 0,
    linked_bill_no      VARCHAR(100),
    summary             VARCHAR(500),
    remark              VARCHAR(500),
    attachment          VARCHAR(1000),
    bookkeeper_id       BIGINT,
    bookkeeper_name     VARCHAR(64),
    bookkeeping_time    TIMESTAMP,
    creator_name        VARCHAR(64),
    print_count         INT          DEFAULT 0,
    process_result      VARCHAR(1000),
    applicant_id        BIGINT,
    applicant_name      VARCHAR(64),
    apply_time          TIMESTAMP,
    approved_by         BIGINT,
    approved_time       TIMESTAMP,
    approved_note       VARCHAR(200),
    executed_by         BIGINT,
    executed_time       TIMESTAMP,
    cancel_reason       VARCHAR(200),
    deleted             SMALLINT     NOT NULL DEFAULT 0,
    create_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT,
    status              SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_stock_take PRIMARY KEY (id)
);

-- 兼容：若表已存在且缺 status 列则补齐（幂等），供下方 idx_stock_take_tenant_status 使用
ALTER TABLE erp_stock_take ADD COLUMN IF NOT EXISTS status SMALLINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_stock_take_tenant_status ON erp_stock_take (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_stock_take_no            ON erp_stock_take (stock_take_no);
CREATE INDEX IF NOT EXISTS idx_stock_take_date          ON erp_stock_take (stock_take_date);
CREATE INDEX IF NOT EXISTS idx_stock_take_warehouse     ON erp_stock_take (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_stock_take_handler       ON erp_stock_take (handler_id);

CREATE TABLE IF NOT EXISTS erp_stock_take_item (
    id                                 BIGINT       NOT NULL,
    tenant_id                          BIGINT       NOT NULL DEFAULT 1,
    stock_take_id                      BIGINT       NOT NULL,
    product_id                         BIGINT       NOT NULL,
    product_code                       VARCHAR(64),
    product_name                       VARCHAR(200),
    product_spec                       VARCHAR(100),
    product_unit                       VARCHAR(20),
    barcode                            VARCHAR(64),
    model                              VARCHAR(64),
    origin                             VARCHAR(64),
    brand                              VARCHAR(64),
    region                             VARCHAR(64),
    location                           VARCHAR(64),
    image                              VARCHAR(500),
    stock_quantity                     NUMERIC(18, 4) DEFAULT 0,
    conversion_result                  NUMERIC(18, 4),
    check_quantity                     NUMERIC(18, 4) DEFAULT 0,
    check_quantity_conversion_result   NUMERIC(18, 4),
    piece_quantity                     NUMERIC(18, 4),
    diff_quantity                      NUMERIC(18, 4),
    production_date                    DATE,
    conversion_relation                VARCHAR(100),
    shelf_life                         VARCHAR(50),
    expiry_date                        DATE,
    batch_code                         VARCHAR(64),
    big_pack                           NUMERIC(18, 4),
    mid_pack                           NUMERIC(18, 4),
    small_pack                         NUMERIC(18, 4),
    check_status                       SMALLINT     DEFAULT 1,
    cost_price                         NUMERIC(18, 4),
    diff_conversion_result             NUMERIC(18, 4),
    diff_amount                        NUMERIC(18, 2),
    item_ext_num1                      NUMERIC(18, 4),
    item_ext_num2                      NUMERIC(18, 4),
    item_ext_num3                      NUMERIC(18, 4),
    item_ext_text1                     VARCHAR(200),
    item_ext_text2                     VARCHAR(200),
    remark                             VARCHAR(500),
    deleted                            SMALLINT     NOT NULL DEFAULT 0,
    create_time                        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time                        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_stock_take_item PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_stock_take_item_parent  ON erp_stock_take_item (stock_take_id);
CREATE INDEX IF NOT EXISTS idx_stock_take_item_product ON erp_stock_take_item (product_id);
CREATE INDEX IF NOT EXISTS idx_stock_take_item_barcode ON erp_stock_take_item (barcode);

-- ═══════════════════════════════════════════════════════════════════
-- 盘点单编号序列（前缀 KCPDD-）
-- ═══════════════════════════════════════════════════════════════════
INSERT INTO biz_number_sequence (biz_type, locale, seq_date, current_seq, prefix, seq_length, tenant_id, max_seq)
VALUES ('PDD', 'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'KCPDD', 4, 1, 999999)
ON CONFLICT (tenant_id, biz_type, locale) DO NOTHING;

-- ═══════════════════════════════════════════════════════════════════
-- 盘点单(5003) 菜单双入口对齐：主菜单→新增表单，[历史]→列表
-- 与 其他入库单 5002 / 调拨单 5011 保持一致
-- ═══════════════════════════════════════════════════════════════════
UPDATE sys_menu SET
  menu_name = '盘点单',
  path = 'erp/stocktake/form',
  component = 'erp/stocktake/form',
  list_path = 'erp/stocktake/index',
  tag_label = '历史',
  display_mode = 1,
  update_time = CURRENT_TIMESTAMP
WHERE id = 5003 OR menu_code = 'erp:stocktake';
