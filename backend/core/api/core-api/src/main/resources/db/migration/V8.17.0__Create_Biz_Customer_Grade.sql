-- V8.17.0: 创建客户等级表 biz_customer_grade
-- CustomerGrade 实体需要的表，用于客户分级管理

CREATE TABLE IF NOT EXISTS biz_customer_grade (
    id              BIGSERIAL PRIMARY KEY,
    grade_code      VARCHAR(50),
    grade_name      VARCHAR(100) NOT NULL,
    grade_level     INTEGER DEFAULT 0,
    grade_icon      VARCHAR(100),
    grade_color     VARCHAR(50),
    min_amount      DECIMAL(18,2),
    max_amount      DECIMAL(18,2),
    min_price       DECIMAL(18,2),
    max_price       DECIMAL(18,2),
    discount_rate   DECIMAL(10,4),
    point_rate      DECIMAL(10,4),
    credit_limit    DECIMAL(18,2),
    credit_days     INTEGER,
    free_shipping   BOOLEAN DEFAULT FALSE,
    free_shipping_min_amount DECIMAL(18,2),
    birthday_privilege VARCHAR(200),
    exclusive_service  BOOLEAN DEFAULT FALSE,
    priority_shipping  BOOLEAN DEFAULT FALSE,
    description     VARCHAR(500),
    sort_order      INTEGER DEFAULT 0,
    status          INTEGER DEFAULT 1,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER DEFAULT 0
);

-- 添加租户隔离字段（与其他业务表保持一致）
ALTER TABLE biz_customer_grade ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT 1;

-- 索引
CREATE INDEX IF NOT EXISTS idx_biz_customer_grade_tenant ON biz_customer_grade(tenant_id);
CREATE INDEX IF NOT EXISTS idx_biz_customer_grade_status ON biz_customer_grade(status, deleted);
