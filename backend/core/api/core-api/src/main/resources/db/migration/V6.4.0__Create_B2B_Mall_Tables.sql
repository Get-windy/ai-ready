-- =====================================================
-- V6.4.0: 创建 B2B 商城模块表
-- =====================================================

-- 1. 租户商城配置表
CREATE TABLE IF NOT EXISTS tenant_shop_config (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    shop_name       VARCHAR(100),
    shop_logo       VARCHAR(500),
    shop_desc       VARCHAR(500),
    theme_color     VARCHAR(20),
    banner_ids      VARCHAR(500),
    template_id     BIGINT,
    payment_methods VARCHAR(200),
    enable_register INTEGER DEFAULT 1,
    enable_auto_audit INTEGER DEFAULT 0,
    min_order_amount DECIMAL(12,2) DEFAULT 0,
    free_shipping_amount DECIMAL(12,2) DEFAULT 0,
    freight_amount DECIMAL(12,2) DEFAULT 0,
    status          INTEGER DEFAULT 1,
    deleted         INTEGER DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP
);

COMMENT ON TABLE tenant_shop_config IS '租户商城配置';
COMMENT ON COLUMN tenant_shop_config.shop_name IS '商城名称';
COMMENT ON COLUMN tenant_shop_config.shop_logo IS '商城LOGO';
COMMENT ON COLUMN tenant_shop_config.enable_register IS '是否开放注册 1=是 0=否';
COMMENT ON COLUMN tenant_shop_config.enable_auto_audit IS '注册自动审核 1=是 0=否';

-- 2. 商城轮播图表
CREATE TABLE IF NOT EXISTS shop_banner (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    title           VARCHAR(100),
    image_url       VARCHAR(500),
    link_url        VARCHAR(500),
    link_type       VARCHAR(50),  -- product/category/none
    link_value      VARCHAR(200),
    sort_order      INTEGER DEFAULT 0,
    status          INTEGER DEFAULT 1,
    deleted         INTEGER DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP
);

COMMENT ON TABLE shop_banner IS '商城轮播图';
COMMENT ON COLUMN shop_banner.link_type IS '链接类型: product/category/none';
COMMENT ON COLUMN shop_banner.link_value IS '链接目标值(商品ID/分类ID)';

-- 3. 商城页面模板表
CREATE TABLE IF NOT EXISTS shop_template (
    id              BIGINT PRIMARY KEY,
    template_name   VARCHAR(100) NOT NULL,
    template_code   VARCHAR(50) NOT NULL UNIQUE,
    thumbnail       VARCHAR(500),
    description     VARCHAR(500),
    config_json     TEXT,
    is_default      INTEGER DEFAULT 0,
    status          INTEGER DEFAULT 1,
    deleted         INTEGER DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP
);

COMMENT ON TABLE shop_template IS '商城页面模板';

-- 4. 商城注册用户表
CREATE TABLE IF NOT EXISTS shop_user (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    username        VARCHAR(50) NOT NULL,
    password        VARCHAR(200),
    phone           VARCHAR(20),
    email           VARCHAR(100),
    company_name    VARCHAR(200),
    nickname        VARCHAR(50),
    avatar          VARCHAR(500),
    source          VARCHAR(20),  -- h5/wxapp
    audit_status    INTEGER DEFAULT 0,  -- 0=待审核 1=通过 2=驳回
    audit_time      TIMESTAMP,
    audit_by        BIGINT,
    reject_reason   VARCHAR(500),
    erp_customer_id BIGINT,
    erp_partner_id  BIGINT,
    last_login_time TIMESTAMP,
    status          INTEGER DEFAULT 1,
    deleted         INTEGER DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP
);

COMMENT ON TABLE shop_user IS '商城注册用户(B2B)';
COMMENT ON COLUMN shop_user.audit_status IS '审核状态: 0=待审核 1=通过 2=驳回';
COMMENT ON COLUMN shop_user.erp_customer_id IS '关联ERP客户ID';
COMMENT ON COLUMN shop_user.erp_partner_id IS '关联ERP往来单位ID';

CREATE INDEX idx_shop_user_tenant ON shop_user(tenant_id);
CREATE INDEX idx_shop_user_username ON shop_user(username);
CREATE INDEX idx_shop_user_audit_status ON shop_user(audit_status);

-- 5. 商城商品表
CREATE TABLE IF NOT EXISTS mall_product (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    product_id      VARCHAR(50),    -- 关联erp_product.product_code
    product_code    VARCHAR(50),
    product_name    VARCHAR(200),
    image_url       VARCHAR(500),
    sale_price      DECIMAL(12,2),
    market_price    DECIMAL(12,2),
    stock_quantity  INTEGER DEFAULT 0,
    category_id     VARCHAR(50),
    category_name   VARCHAR(100),
    status          VARCHAR(20) DEFAULT 'ACTIVE',  -- ACTIVE/INACTIVE
    description     TEXT,
    sales_count     INTEGER DEFAULT 0,
    deleted         INTEGER DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP
);

COMMENT ON TABLE mall_product IS '商城商品(映射ERP产品)';
COMMENT ON COLUMN mall_product.product_id IS '商品编码，关联erp_product.product_code';

CREATE INDEX idx_mall_product_tenant ON mall_product(tenant_id);
CREATE INDEX idx_mall_product_code ON mall_product(product_code);
CREATE INDEX idx_mall_product_category ON mall_product(category_id);

-- 6. 商城订单表
CREATE TABLE IF NOT EXISTS mall_order (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    order_no        VARCHAR(50) NOT NULL,
    customer_id     BIGINT,
    customer_name   VARCHAR(100),
    total_amount    DECIMAL(12,2),
    discount_amount DECIMAL(12,2) DEFAULT 0,
    pay_amount      DECIMAL(12,2),
    order_status    VARCHAR(20) DEFAULT 'PENDING_PAYMENT',
    payment_method  VARCHAR(50),
    payment_status  VARCHAR(20) DEFAULT 'UNPAID',
    delivery_status VARCHAR(20) DEFAULT 'UNDELIVERED',
    consignee       VARCHAR(100),
    phone           VARCHAR(20),
    address         VARCHAR(500),
    remark          VARCHAR(500),
    source          VARCHAR(20),  -- h5/wxapp/admin
    deleted         INTEGER DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP
);

COMMENT ON TABLE mall_order IS '商城订单';
COMMENT ON COLUMN mall_order.order_status IS '订单状态: PENDING_PAYMENT/PAID/APPROVED/REJECTED/DELIVERED/COMPLETED/CANCELLED';
COMMENT ON COLUMN mall_order.payment_status IS '支付状态: UNPAID/PAID/REFUNDED';
COMMENT ON COLUMN mall_order.delivery_status IS '发货状态: UNDELIVERED/DELIVERING/DELIVERED';

CREATE INDEX idx_mall_order_tenant ON mall_order(tenant_id);
CREATE INDEX idx_mall_order_no ON mall_order(order_no);
CREATE INDEX idx_mall_order_customer ON mall_order(customer_id);
CREATE INDEX idx_mall_order_status ON mall_order(order_status);

-- 7. 商城订单明细表
CREATE TABLE IF NOT EXISTS mall_order_item (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    order_id        BIGINT NOT NULL,
    product_id      VARCHAR(50),
    product_name    VARCHAR(200),
    product_image   VARCHAR(500),
    price           DECIMAL(12,2),
    quantity        INTEGER,
    subtotal        DECIMAL(12,2),
    deleted         INTEGER DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP
);

COMMENT ON TABLE mall_order_item IS '商城订单明细';

CREATE INDEX idx_mall_order_item_order ON mall_order_item(order_id);
CREATE INDEX idx_mall_order_item_product ON mall_order_item(product_id);

-- 8. 插入默认模板数据
INSERT INTO shop_template (id, template_name, template_code, thumbnail, description, is_default, status, create_time)
VALUES
(1, '经典蓝', 'classic_blue', '/static/templates/blue.png', '经典蓝色商务风格', 1, 1, CURRENT_TIMESTAMP),
(2, '简约白', 'simple_white', '/static/templates/white.png', '简洁白色风格', 0, 1, CURRENT_TIMESTAMP),
(3, '商务橙', 'business_orange', '/static/templates/orange.png', '活力橙色风格', 0, 1, CURRENT_TIMESTAMP)
ON CONFLICT (template_code) DO NOTHING;

-- 9. 为系统租户创建默认商城配置
INSERT INTO tenant_shop_config (id, tenant_id, shop_name, shop_desc, theme_color, template_id, enable_register, enable_auto_audit, min_order_amount, free_shipping_amount, freight_amount, status, create_time)
VALUES (1, 1, 'AI-Ready B2B商城', '企业智能采购平台', '#1890ff', 1, 1, 0, 100, 500, 10, 1, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;