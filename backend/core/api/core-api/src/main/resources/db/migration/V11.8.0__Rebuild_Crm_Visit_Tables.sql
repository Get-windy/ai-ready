-- V11.8.0 重建 CRM 外勤拜访表（拜访规划/执行/检视）
-- 说明: V9.10.1 创建的 crm_visit_plan / crm_visit_execution / crm_visit_review 字段口径
--       与前端三页（拜访规划/执行/检视）及新接口需求不符，无任何代码引用且数据为空，
--       故 DROP 重建。拜访检视改为统计查询，不再单独落表。
-- 幂等: 脚本以 DROP TABLE IF EXISTS 开头，可重复执行。

DROP TABLE IF EXISTS crm_visit_review;
DROP TABLE IF EXISTS crm_visit_execution;
DROP TABLE IF EXISTS crm_visit_record;
DROP TABLE IF EXISTS crm_visit_plan;

-- ============================================================
-- 拜访计划表
-- ============================================================
CREATE TABLE crm_visit_plan (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT,
    plan_no           VARCHAR(64)  NOT NULL,
    customer_id       BIGINT,
    customer_name     VARCHAR(200),
    sales_person_id   BIGINT,
    sales_person_name VARCHAR(100),
    plan_date         DATE,
    plan_time         VARCHAR(50),
    purpose           VARCHAR(200),
    address           VARCHAR(500),
    status            INTEGER      NOT NULL DEFAULT 0,
    remark            VARCHAR(500),
    created_by        VARCHAR(64),
    created_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER      NOT NULL DEFAULT 0,
    version           INTEGER      NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_crm_visit_plan_no ON crm_visit_plan(plan_no);
CREATE INDEX idx_crm_visit_plan_customer ON crm_visit_plan(customer_id);
CREATE INDEX idx_crm_visit_plan_sales_person ON crm_visit_plan(sales_person_id);
CREATE INDEX idx_crm_visit_plan_date ON crm_visit_plan(plan_date);
CREATE INDEX idx_crm_visit_plan_status ON crm_visit_plan(status);

COMMENT ON TABLE crm_visit_plan IS 'CRM外勤拜访计划表';
COMMENT ON COLUMN crm_visit_plan.status IS '0待执行 1执行中 2已完成 3已取消';

-- ============================================================
-- 拜访执行记录表（签到打卡）
-- ============================================================
CREATE TABLE crm_visit_record (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT,
    plan_id           BIGINT,
    customer_id       BIGINT,
    customer_name     VARCHAR(200),
    sales_person_id   BIGINT,
    sales_person_name VARCHAR(100),
    visit_time        TIMESTAMP,
    visit_type        INTEGER,
    location          VARCHAR(500),
    longitude         NUMERIC(10,7),
    latitude          NUMERIC(10,7),
    content           TEXT,
    result            INTEGER,
    next_action       VARCHAR(500),
    next_visit_date   DATE,
    attachments       TEXT,
    create_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER      NOT NULL DEFAULT 0
);
CREATE INDEX idx_crm_visit_record_plan ON crm_visit_record(plan_id);
CREATE INDEX idx_crm_visit_record_customer ON crm_visit_record(customer_id);
CREATE INDEX idx_crm_visit_record_sales_person ON crm_visit_record(sales_person_id);
CREATE INDEX idx_crm_visit_record_time ON crm_visit_record(visit_time);
CREATE INDEX idx_crm_visit_record_result ON crm_visit_record(result);

COMMENT ON TABLE crm_visit_record IS 'CRM外勤拜访执行记录表(签到打卡)';
COMMENT ON COLUMN crm_visit_record.plan_id IS '关联拜访计划ID, 可空=无计划临时拜访';
COMMENT ON COLUMN crm_visit_record.visit_type IS '1上门 2电话 3其他';
COMMENT ON COLUMN crm_visit_record.result IS '1有意向 2一般 3无意向';
COMMENT ON COLUMN crm_visit_record.attachments IS '附件URL json数组';
