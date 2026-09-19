-- =============================================================================
-- 价格审批（erp/pricing/approval）：补齐后端缺失的实现
-- V11.424.0 · 2026-09-19
--
-- 【背景】前端页面 `views/erp/pricing/approval/index.vue` 与其 API 层
--   `api/pricing-approval.ts` 定义了完整的审批契约（8 个端点 + PriceApproval 实体），
--   但后端**零实现**——实测 `grep -rn "PriceApproval" backend --exclude-dir=target`
--   只命中 `DatabaseInitializer.java:1625` 的一条遗留菜单定义（9005 定价审批），
--   没有任何 Controller/Service/Entity ⇒ 该页所有请求 404。
--
-- 【本次处置】按前端既有契约补齐后端（不重新设计协议）：
--   迁移建表 erp_price_approval + Entity/PriceApproval + Mapper + Controller。
--
-- 【契约来源】frontend/apps/pc-admin/src/api/pricing-approval.ts
--   GET  /api/erp/pricing/approval/statistics
--   GET  /api/erp/pricing/approval/pending
--   GET  /api/erp/pricing/approval/list/{status}
--   GET  /api/erp/pricing/approval/my/{applicantId}
--   GET  /api/erp/pricing/approval/{id}
--   POST /api/erp/pricing/approval/apply
--   PUT  /api/erp/pricing/approval/{id}/approve?approverId=&remark=
--   PUT  /api/erp/pricing/approval/{id}/reject?approverId=&remark=
--
-- 【字段口径】id 由 MyBatis-Plus `@TableId(type = IdType.ASSIGN_ID)` 生成雪花值，
--   故 id 列**不需要**序列默认值（与 erp_customer_product_price 等同模块表一致）。
--   表名含下划线 ⇒ 会被 MyBatis-Plus 租户拦截器自动注入 tenant_id，故必须有该列。
--   deleted/create_time/update_time 三列配合实体的 @TableLogic 与 @TableField(fill=...)。
--
-- 【幂等】CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS，可重复执行。
-- 【回滚】DROP TABLE IF EXISTS erp_price_approval;
-- =============================================================================

CREATE TABLE IF NOT EXISTS erp_price_approval (
    id                  BIGINT       PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL DEFAULT 0,
    product_id          BIGINT,
    product_name        VARCHAR(200),
    product_code        VARCHAR(100),
    customer_id         BIGINT,
    customer_name       VARCHAR(200),
    old_price           NUMERIC(18, 4),
    new_price           NUMERIC(18, 4),
    price_change        NUMERIC(18, 4),
    price_change_type   VARCHAR(20),
    approval_type       VARCHAR(50),
    approval_type_label VARCHAR(100),
    applicant_id        BIGINT,
    applicant_name      VARCHAR(100),
    apply_time          TIMESTAMP    DEFAULT now(),
    status              VARCHAR(20)  NOT NULL DEFAULT 'pending',
    approver_id         BIGINT,
    approver_name       VARCHAR(100),
    approve_time        TIMESTAMP,
    approval_reason     VARCHAR(500),
    approve_remark      VARCHAR(500),
    effective_start     DATE,
    effective_end       DATE,
    deleted             INTEGER      NOT NULL DEFAULT 0,
    create_by           BIGINT,
    create_time         TIMESTAMP    DEFAULT now(),
    update_by           BIGINT,
    update_time         TIMESTAMP    DEFAULT now()
);

-- 列表/统计的主查询路径：按租户 + 状态过滤
CREATE INDEX IF NOT EXISTS idx_price_approval_tenant_status
    ON erp_price_approval (tenant_id, status);
-- 「我的申请」按申请人查
CREATE INDEX IF NOT EXISTS idx_price_approval_applicant
    ON erp_price_approval (applicant_id);
-- 按商品查历史
CREATE INDEX IF NOT EXISTS idx_price_approval_product
    ON erp_price_approval (product_id);
