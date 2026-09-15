-- ============================================================
-- V9.45.0: 创建销售退货单表
--
-- 创建erp_sale_return_doc和erp_sale_return_doc_item表
-- 用于存储销售退货单（审批通过后实际执行的退货入库操作）
-- ============================================================

-- 创建销售退货单主表
CREATE TABLE IF NOT EXISTS erp_sale_return_doc (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    return_doc_no VARCHAR(50) NOT NULL,
    customer_id BIGINT,
    customer_name VARCHAR(200),
    customer_code VARCHAR(50),
    customer_level VARCHAR(50),
    contact_name VARCHAR(100),
    contact_phone VARCHAR(50),
    contact_address VARCHAR(500),
    customer_remark TEXT,
    warehouse_id BIGINT,
    warehouse_name VARCHAR(200),
    handler_id BIGINT,
    handler_name VARCHAR(100),
    dept_name VARCHAR(200),
    order_date TIMESTAMP,
    return_type INTEGER DEFAULT 0,
    return_apply_id BIGINT,
    return_apply_no VARCHAR(50),
    reason TEXT,
    total_quantity DECIMAL(18,4) DEFAULT 0,
    total_amount DECIMAL(18,2) DEFAULT 0,
    status INTEGER DEFAULT 0,
    print_count INTEGER DEFAULT 0,
    remark TEXT,
    creator VARCHAR(100),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(100),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_return_doc_tenant ON erp_sale_return_doc(tenant_id);
CREATE INDEX IF NOT EXISTS idx_return_doc_no ON erp_sale_return_doc(return_doc_no);
CREATE INDEX IF NOT EXISTS idx_return_doc_customer ON erp_sale_return_doc(customer_id);
CREATE INDEX IF NOT EXISTS idx_return_doc_status ON erp_sale_return_doc(status);
CREATE INDEX IF NOT EXISTS idx_return_doc_date ON erp_sale_return_doc(order_date);
CREATE INDEX IF NOT EXISTS idx_return_doc_apply ON erp_sale_return_doc(return_apply_id);

-- 创建销售退货单明细表
CREATE TABLE IF NOT EXISTS erp_sale_return_doc_item (
    id BIGINT PRIMARY KEY,
    return_doc_id BIGINT NOT NULL,
    product_id BIGINT,
    product_code VARCHAR(100),
    product_name VARCHAR(300),
    barcode VARCHAR(100),
    specification VARCHAR(200),
    unit VARCHAR(50),
    return_quantity DECIMAL(18,4) DEFAULT 0,
    conversion_relation VARCHAR(100),
    piece_quantity DECIMAL(18,4) DEFAULT 0,
    big_pack DECIMAL(18,2) DEFAULT 0,
    mid_pack DECIMAL(18,2) DEFAULT 0,
    small_pack DECIMAL(18,2) DEFAULT 0,
    unit_price DECIMAL(18,4) DEFAULT 0,
    line_amount DECIMAL(18,2) DEFAULT 0,
    remark TEXT,
    sort INTEGER DEFAULT 0
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_return_doc_item_doc ON erp_sale_return_doc_item(return_doc_id);
CREATE INDEX IF NOT EXISTS idx_return_doc_item_product ON erp_sale_return_doc_item(product_id);

-- 添加备注
COMMENT ON TABLE erp_sale_return_doc IS '销售退货单';
COMMENT ON COLUMN erp_sale_return_doc.return_doc_no IS '退货单号';
COMMENT ON COLUMN erp_sale_return_doc.return_type IS '退货类型: 0-质量问题 1-错发退货 2-其他';
COMMENT ON COLUMN erp_sale_return_doc.return_apply_id IS '关联退货申请ID';
COMMENT ON COLUMN erp_sale_return_doc.return_apply_no IS '关联退货申请单号';
COMMENT ON COLUMN erp_sale_return_doc.status IS '状态: 0-草稿 1-待审核 2-已审核 3-已完成 4-已取消';

COMMENT ON TABLE erp_sale_return_doc_item IS '销售退货单明细';
COMMENT ON COLUMN erp_sale_return_doc_item.return_quantity IS '退货数量';
COMMENT ON COLUMN erp_sale_return_doc_item.line_amount IS '行金额';
