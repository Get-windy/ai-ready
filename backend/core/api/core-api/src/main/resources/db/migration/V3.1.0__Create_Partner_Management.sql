-- =====================================================
-- V3.1.0: 往来单位统一管理
-- 包含: 分类/等级/主表/联系人/地址/银行账户/标签
-- =====================================================

-- 1. 往来单位分类表
CREATE TABLE IF NOT EXISTS erp_partner_category (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    category_code   VARCHAR(50) NOT NULL,
    category_name   VARCHAR(200) NOT NULL,
    category_type   VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER'
                    COMMENT '适用类型 CUSTOMER/SUPPLIER/BOTH/OTHER',
    parent_id       BIGINT NOT NULL DEFAULT 0,
    category_level  INTEGER NOT NULL DEFAULT 1,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    status          INTEGER NOT NULL DEFAULT 1,
    description     VARCHAR(500),
    remark          VARCHAR(500),
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_partner_category IS '往来单位分类表';
CREATE INDEX IF NOT EXISTS idx_partcat_tenant ON erp_partner_category(tenant_id);
CREATE INDEX IF NOT EXISTS idx_partcat_parent ON erp_partner_category(parent_id);
CREATE INDEX IF NOT EXISTS idx_partcat_type ON erp_partner_category(category_type);

-- 2. 往来单位等级表(客户等级/供应商等级)
CREATE TABLE IF NOT EXISTS erp_partner_grade (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    grade_code      VARCHAR(50) NOT NULL,
    grade_name      VARCHAR(100) NOT NULL,
    grade_type      VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER'
                    COMMENT '等级类型 CUSTOMER/SUPPLIER',
    grade_level     INTEGER NOT NULL DEFAULT 0 COMMENT '等级数值(越大越高)',
    sort_order      INTEGER DEFAULT 0,
    status          INTEGER NOT NULL DEFAULT 1,
    description     VARCHAR(500),
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_partner_grade IS '往来单位等级表';
CREATE UNIQUE INDEX IF NOT EXISTS idx_partgrade_code ON erp_partner_grade(tenant_id, grade_type, grade_code);

-- 3. 往来单位主表(统一管理客户/供应商)
CREATE TABLE IF NOT EXISTS erp_partner (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL DEFAULT 0,
    partner_code        VARCHAR(50) NOT NULL COMMENT '往来单位编码',
    partner_name        VARCHAR(200) NOT NULL COMMENT '单位名称',
    partner_short_name  VARCHAR(100) COMMENT '简称',
    partner_type        VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER'
                        COMMENT '类型 CUSTOMER客户/SUPPLIER供应商/BOTH购销/OTHER其他',
    partner_category_id BIGINT REFERENCES erp_partner_category(id) COMMENT '分类',
    partner_grade_id    BIGINT REFERENCES erp_partner_grade(id) COMMENT '等级',

    -- 工商信息
    unified_social_code VARCHAR(50) COMMENT '统一社会信用代码',
    tax_id              VARCHAR(50) COMMENT '税务登记号',
    legal_person        VARCHAR(100) COMMENT '法定代表人',
    registered_capital  DECIMAL(20,2) DEFAULT 0 COMMENT '注册资本',
    company_phone       VARCHAR(50) COMMENT '公司电话',
    company_email       VARCHAR(200) COMMENT '公司邮箱',
    company_website     VARCHAR(200) COMMENT '公司网址',
    industry            VARCHAR(100) COMMENT '所属行业',

    -- 默认地址信息
    country             VARCHAR(50) DEFAULT '中国',
    province            VARCHAR(50),
    city                VARCHAR(50),
    district            VARCHAR(50),
    detail_address      VARCHAR(500),

    -- 默认联系人
    contact_person      VARCHAR(100) COMMENT '默认联系人',
    contact_phone       VARCHAR(50) COMMENT '联系电话',
    contact_email       VARCHAR(200) COMMENT '联系邮箱',

    -- 财务/结算
    payment_terms       VARCHAR(100) COMMENT '付款条件',
    credit_limit        DECIMAL(20,2) DEFAULT 0 COMMENT '信用额度',
    credit_days         INTEGER DEFAULT 0 COMMENT '账期天数',
    tax_rate            DECIMAL(5,2) DEFAULT 13.00 COMMENT '税率%',
    settle_type         VARCHAR(30) DEFAULT 'MONTHLY'
                        COMMENT '结算方式 MONTHLY月结/WEEKLY周结/CASH现结/ADVANCE预付',
    opening_balance     DECIMAL(20,2) DEFAULT 0 COMMENT '期初欠款',
    current_balance     DECIMAL(20,2) DEFAULT 0 COMMENT '当前欠款',

    -- 默认仓库/地址引用
    default_warehouse_id    BIGINT COMMENT '默认发货仓库',
    default_delivery_addr_id BIGINT COMMENT '默认收货地址ID',

    -- 来源渠道
    source_channel      VARCHAR(50) COMMENT '来源渠道',
    source_partner_id   BIGINT COMMENT '来源客户(转介绍)',
    first_order_time    TIMESTAMP COMMENT '首次下单时间',
    last_order_time     TIMESTAMP COMMENT '最后下单时间',
    total_order_count   INTEGER DEFAULT 0 COMMENT '累计订单数',
    total_order_amount  DECIMAL(20,2) DEFAULT 0 COMMENT '累计订单金额',

    -- 状态
    remark              VARCHAR(500),
    status              VARCHAR(20) DEFAULT 'ENABLED',
    deleted             INTEGER NOT NULL DEFAULT 0,
    create_by           BIGINT,
    create_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_partner IS '往来单位主表(统一管理客户/供应商)';
CREATE INDEX IF NOT EXISTS idx_partner_tenant ON erp_partner(tenant_id);
CREATE INDEX IF NOT EXISTS idx_partner_type ON erp_partner(partner_type);
CREATE INDEX IF NOT EXISTS idx_partner_category ON erp_partner(partner_category_id);
CREATE INDEX IF NOT EXISTS idx_partner_grade ON erp_partner(partner_grade_id);
CREATE INDEX IF NOT EXISTS idx_partner_phone ON erp_partner(contact_phone);
CREATE UNIQUE INDEX IF NOT EXISTS idx_partner_code ON erp_partner(tenant_id, partner_code);

-- 4. 往来单位联系人表
CREATE TABLE IF NOT EXISTS erp_partner_contact (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    partner_id      BIGINT NOT NULL REFERENCES erp_partner(id),
    contact_name    VARCHAR(100) NOT NULL COMMENT '联系人姓名',
    contact_phone   VARCHAR(50) COMMENT '联系电话',
    contact_email   VARCHAR(200) COMMENT '邮箱',
    position        VARCHAR(100) COMMENT '职位',
    department      VARCHAR(100) COMMENT '部门',
    is_default      INTEGER NOT NULL DEFAULT 0 COMMENT '是否默认联系人',
    remark          VARCHAR(500),
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_partner_contact IS '往来单位联系人表';
CREATE INDEX IF NOT EXISTS idx_pcontact_partner ON erp_partner_contact(partner_id);

-- 5. 往来单位地址表
CREATE TABLE IF NOT EXISTS erp_partner_address (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    partner_id      BIGINT NOT NULL REFERENCES erp_partner(id),
    address_type    VARCHAR(20) NOT NULL DEFAULT 'DELIVERY'
                    COMMENT '地址类型 DELIVERY发货/BILLING开票/RECEIVING收货/RETURN退货',
    country         VARCHAR(50) DEFAULT '中国',
    province        VARCHAR(50),
    city            VARCHAR(50),
    district        VARCHAR(50),
    detail_address  VARCHAR(500) NOT NULL,
    zip_code        VARCHAR(20),
    contact_name    VARCHAR(100) COMMENT '收货人',
    contact_phone   VARCHAR(50) COMMENT '收货电话',
    is_default      INTEGER NOT NULL DEFAULT 0 COMMENT '是否默认地址',
    remark          VARCHAR(500),
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_partner_address IS '往来单位地址表';
CREATE INDEX IF NOT EXISTS idx_paddr_partner ON erp_partner_address(partner_id);

-- 6. 往来单位银行账户表
CREATE TABLE IF NOT EXISTS erp_partner_bank_account (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    partner_id      BIGINT NOT NULL REFERENCES erp_partner(id),
    account_name    VARCHAR(200) NOT NULL COMMENT '开户名称',
    bank_name       VARCHAR(200) NOT NULL COMMENT '开户银行',
    bank_branch     VARCHAR(200) COMMENT '支行名称',
    account_no      VARCHAR(100) NOT NULL COMMENT '银行账号',
    currency        VARCHAR(10) DEFAULT 'CNY' COMMENT '币种',
    is_default      INTEGER NOT NULL DEFAULT 0 COMMENT '是否默认账户',
    remark          VARCHAR(500),
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_partner_bank_account IS '往来单位银行账户表';
CREATE INDEX IF NOT EXISTS idx_pbank_partner ON erp_partner_bank_account(partner_id);

-- 7. 往来单位标签表
CREATE TABLE IF NOT EXISTS erp_partner_tag (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    tag_name        VARCHAR(50) NOT NULL COMMENT '标签名称',
    tag_color       VARCHAR(20) COMMENT '标签颜色',
    tag_type        VARCHAR(20) DEFAULT 'CUSTOMER'
                    COMMENT '标签类型 CUSTOMER/SUPPLIER/ALL',
    sort_order      INTEGER DEFAULT 0,
    status          INTEGER NOT NULL DEFAULT 1,
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_partner_tag IS '往来单位标签表';

-- 8. 往来单位-标签关联表
CREATE TABLE IF NOT EXISTS erp_partner_tag_relation (
    id              BIGSERIAL PRIMARY KEY,
    partner_id      BIGINT NOT NULL REFERENCES erp_partner(id),
    tag_id          BIGINT NOT NULL REFERENCES erp_partner_tag(id),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_partner_tag_relation IS '往来单位标签关联表';
CREATE INDEX IF NOT EXISTS idx_ptagrel_partner ON erp_partner_tag_relation(partner_id);
CREATE INDEX IF NOT EXISTS idx_ptagrel_tag ON erp_partner_tag_relation(tag_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ptagrel_unique ON erp_partner_tag_relation(partner_id, tag_id);

-- =====================================================
-- 插入默认数据
-- =====================================================

-- 默认客户等级
INSERT INTO erp_partner_grade (tenant_id, grade_code, grade_name, grade_type, grade_level, sort_order) VALUES
(0, 'VIP', 'VIP客户', 'CUSTOMER', 5, 1),
(0, 'A', 'A级客户', 'CUSTOMER', 4, 2),
(0, 'B', 'B级客户', 'CUSTOMER', 3, 3),
(0, 'C', 'C级客户', 'CUSTOMER', 2, 4),
(0, 'D', 'D级客户', 'CUSTOMER', 1, 5)
ON CONFLICT (tenant_id, grade_type, grade_code) DO NOTHING;

-- 默认供应商等级
INSERT INTO erp_partner_grade (tenant_id, grade_code, grade_name, grade_type, grade_level, sort_order) VALUES
(0, 'PRIORITY', '优先供应商', 'SUPPLIER', 3, 1),
(0, 'STANDARD', '标准供应商', 'SUPPLIER', 2, 2),
(0, 'BACKUP', '备用供应商', 'SUPPLIER', 1, 3)
ON CONFLICT (tenant_id, grade_type, grade_code) DO NOTHING;

-- 默认往来单位分类
INSERT INTO erp_partner_category (tenant_id, category_code, category_name, category_type, parent_id, category_level, sort_order) VALUES
(0, 'ROOT_CUSTOMER', '客户分类', 'CUSTOMER', 0, 1, 1),
(0, 'ROOT_SUPPLIER', '供应商分类', 'SUPPLIER', 0, 1, 2),
(0, 'RETAIL', '零售客户', 'CUSTOMER', 1, 2, 1),
(0, 'WHOLESALE', '批发客户', 'CUSTOMER', 1, 2, 2),
(0, 'AGENCY', '代理商', 'CUSTOMER', 1, 2, 3),
(0, 'RAW_MATERIAL', '原材料供应商', 'SUPPLIER', 2, 2, 1),
(0, 'EQUIPMENT', '设备供应商', 'SUPPLIER', 2, 2, 2),
(0, 'SERVICE', '服务供应商', 'SUPPLIER', 2, 2, 3)
ON CONFLICT DO NOTHING;
