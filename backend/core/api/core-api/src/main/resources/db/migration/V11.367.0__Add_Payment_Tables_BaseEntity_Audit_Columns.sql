-- ============================================================================
-- V11.367.0 支付中心 5 张表补齐 BaseEntity 审计列（create_by / update_by / version）
-- ============================================================================
-- 目标页面（交易模块 → 支付中心）：
--   · 支付记录   frontend/apps/pc-admin/src/views/payment/record/list.vue
--   · 支付请求   frontend/apps/pc-admin/src/views/payment/request/list.vue
--   · 退款管理   frontend/apps/pc-admin/src/views/payment/refund/list.vue
--   · 每日对账   frontend/apps/pc-admin/src/views/payment/reconciliation/daily.vue
--
-- ── 问题（2026-09-14 真机实测，非推测）──────────────────────────────────────
-- core-payment 的 5 个实体**全部** `extends BaseEntity`：
--   backend/core/payment/core-payment/src/main/java/cn/aiedge/payment/entity/
--     PaymentRequest   @TableName("payment_request")
--     PaymentRecord    @TableName("payment_record")
--     RefundRequest    @TableName("refund_request")
--     RefundRecord     @TableName("refund_record")
--     PaymentReconciliation @TableName("payment_reconciliation")
-- 而 BaseEntity 声明了 `createBy` / `updateBy` / `version`（含 @Version 乐观锁）：
--   backend/core/base/core-base/src/main/java/cn/aiedge/base/entity/BaseEntity.java
--     L40 @TableField(fill = INSERT)        private Long createBy;
--     L46 @TableField(fill = INSERT_UPDATE) private Long updateBy;
--     L58 @Version @TableField(fill = INSERT) private Integer version;
--
-- 但真库这 5 张表**都没有**这三列（information_schema 只读复核，2026-09-14）：
--   payment_request        | create_by=f | update_by=f | version=f | 18 列
--   payment_record         | create_by=f | update_by=f | version=f | 15 列
--   refund_request         | create_by=f | update_by=f | version=f | 17 列
--   refund_record          | create_by=f | update_by=f | version=f | 14 列
--   payment_reconciliation | create_by=f | update_by=f | version=f | 17 列
--
-- 全仓也**没有** MetaObjectHandler 实现（`grep "implements MetaObjectHandler"` 无命中），
-- 故这三个字段不会被自动填充 → INSERT 时因 MyBatis-Plus 默认 FieldStrategy.NOT_NULL
-- 被忽略（写入侥幸不报错），但 **SELECT 一定会把这 3 列列进列清单** → 每次查询必抛
--   org.postgresql.util.PSQLException: 错误: 字段 "create_by" 不存在
-- 实测复现（运行中的后端，GET）：
--   /api/reconciliation/pending-dates → 500  BadSqlGrammarException
--     SQL: SELECT id, biz_type, ..., create_time, update_time, create_by, update_by, deleted, version
--          FROM payment_request WHERE deleted = 0 AND (create_time >= ? AND create_time < ?) AND tenant_id = 1
--     at cn.aiedge.payment.service.impl.ReconciliationServiceImpl.getPendingDates(ReconciliationServiceImpl.java:338)
-- 即：**支付中心 4 个页面的列表/统计查询全部读不出来**（读路径必挂，而非偶发）。
--
-- ── 处置（与仓库既有约定一致，且**不动任何实体/服务代码**）──────────────────
-- 这 5 张表缺的是**全项目通用的审计列**，补列即可同时修好读路径与（未来的）写路径；
-- 反过来把实体改成不继承 BaseEntity 会破坏全项目一致性，故不采用。
--   · `create_by` / `update_by`：可空 BIGINT（无 MetaObjectHandler 时为 NULL，不写默认值，
--     避免把「未记录操作人」伪装成某个具体用户）。
--   · `version`：`INTEGER DEFAULT 0`。这里**应当**用 DEFAULT 回填既有行为 0 ——
--     @Version 乐观锁 UPDATE 的 WHERE 是 `version = ?`，NULL 会导致更新匹配不到任何行；
--     且 0 是「初始版本」的正确语义，不存在语义翻转风险（对比 V11.365.0 的
--     reg_audit_required 陷阱：那一列的默认值会翻转业务判定，故那里**不能**给默认值）。
--
-- 依据：docs/Yh-Spec/手动整理对标开发文档/交易模块/支付记录开发文档.md、
--       支付请求开发文档.md、退款管理开发文档.md、每日对账开发文档.md
-- ============================================================================


-- ─────────────────────────────────────────────────────────────────────────────
-- 一、补列（幂等：ADD COLUMN IF NOT EXISTS）
-- ─────────────────────────────────────────────────────────────────────────────

-- 1) payment_request（支付请求；ReconciliationServiceImpl#getPendingDates 实测报错点）
ALTER TABLE payment_request ADD COLUMN IF NOT EXISTS create_by BIGINT;
ALTER TABLE payment_request ADD COLUMN IF NOT EXISTS update_by BIGINT;
ALTER TABLE payment_request ADD COLUMN IF NOT EXISTS version   INTEGER DEFAULT 0;

-- 2) payment_record（支付记录）
ALTER TABLE payment_record ADD COLUMN IF NOT EXISTS create_by BIGINT;
ALTER TABLE payment_record ADD COLUMN IF NOT EXISTS update_by BIGINT;
ALTER TABLE payment_record ADD COLUMN IF NOT EXISTS version   INTEGER DEFAULT 0;

-- 3) refund_request（退款申请）
ALTER TABLE refund_request ADD COLUMN IF NOT EXISTS create_by BIGINT;
ALTER TABLE refund_request ADD COLUMN IF NOT EXISTS update_by BIGINT;
ALTER TABLE refund_request ADD COLUMN IF NOT EXISTS version   INTEGER DEFAULT 0;

-- 4) refund_record（退款记录）
ALTER TABLE refund_record ADD COLUMN IF NOT EXISTS create_by BIGINT;
ALTER TABLE refund_record ADD COLUMN IF NOT EXISTS update_by BIGINT;
ALTER TABLE refund_record ADD COLUMN IF NOT EXISTS version   INTEGER DEFAULT 0;

-- 5) payment_reconciliation（每日对账）
ALTER TABLE payment_reconciliation ADD COLUMN IF NOT EXISTS create_by BIGINT;
ALTER TABLE payment_reconciliation ADD COLUMN IF NOT EXISTS update_by BIGINT;
ALTER TABLE payment_reconciliation ADD COLUMN IF NOT EXISTS version   INTEGER DEFAULT 0;


-- ─────────────────────────────────────────────────────────────────────────────
-- 二、既有行 version 兜底为 0（ADD COLUMN ... DEFAULT 0 已回填，此处仅防御性补 NULL）
-- ─────────────────────────────────────────────────────────────────────────────

UPDATE payment_request        SET version = 0 WHERE version IS NULL;
UPDATE payment_record         SET version = 0 WHERE version IS NULL;
UPDATE refund_request         SET version = 0 WHERE version IS NULL;
UPDATE refund_record          SET version = 0 WHERE version IS NULL;
UPDATE payment_reconciliation SET version = 0 WHERE version IS NULL;


-- ─────────────────────────────────────────────────────────────────────────────
-- 三、列注释
-- ─────────────────────────────────────────────────────────────────────────────

COMMENT ON COLUMN payment_request.create_by IS '创建人 id（BaseEntity 审计列；本模块无 MetaObjectHandler，未记录时为 NULL）';
COMMENT ON COLUMN payment_request.update_by IS '更新人 id（BaseEntity 审计列；本模块无 MetaObjectHandler，未记录时为 NULL）';
COMMENT ON COLUMN payment_request.version   IS '乐观锁版本号（BaseEntity @Version，DEFAULT 0）';

COMMENT ON COLUMN payment_record.create_by IS '创建人 id（BaseEntity 审计列；本模块无 MetaObjectHandler，未记录时为 NULL）';
COMMENT ON COLUMN payment_record.update_by IS '更新人 id（BaseEntity 审计列；本模块无 MetaObjectHandler，未记录时为 NULL）';
COMMENT ON COLUMN payment_record.version   IS '乐观锁版本号（BaseEntity @Version，DEFAULT 0）';

COMMENT ON COLUMN refund_request.create_by IS '创建人 id（BaseEntity 审计列；本模块无 MetaObjectHandler，未记录时为 NULL）';
COMMENT ON COLUMN refund_request.update_by IS '更新人 id（BaseEntity 审计列；本模块无 MetaObjectHandler，未记录时为 NULL）';
COMMENT ON COLUMN refund_request.version   IS '乐观锁版本号（BaseEntity @Version，DEFAULT 0）';

COMMENT ON COLUMN refund_record.create_by IS '创建人 id（BaseEntity 审计列；本模块无 MetaObjectHandler，未记录时为 NULL）';
COMMENT ON COLUMN refund_record.update_by IS '更新人 id（BaseEntity 审计列；本模块无 MetaObjectHandler，未记录时为 NULL）';
COMMENT ON COLUMN refund_record.version   IS '乐观锁版本号（BaseEntity @Version，DEFAULT 0）';

COMMENT ON COLUMN payment_reconciliation.create_by IS '创建人 id（BaseEntity 审计列；本模块无 MetaObjectHandler，未记录时为 NULL）';
COMMENT ON COLUMN payment_reconciliation.update_by IS '更新人 id（BaseEntity 审计列；本模块无 MetaObjectHandler，未记录时为 NULL）';
COMMENT ON COLUMN payment_reconciliation.version   IS '乐观锁版本号（BaseEntity @Version，DEFAULT 0）';
