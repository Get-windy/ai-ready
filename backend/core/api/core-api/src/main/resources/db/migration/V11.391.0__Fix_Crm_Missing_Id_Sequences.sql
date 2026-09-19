-- =============================================================================
-- CRM · 补齐三张表缺失的自增主键序列（2026-09-18）
--
-- 【缺陷】真库实测（information_schema / pg_attrdef）：
--   `crm_customer.id`、`crm_quotation.id`、`crm_quotation_item.id`
--   **没有 DEFAULT**（无 nextval），而 CRM 模块其余 **19 张表**都有自增序列。
--
--   而五个实体统一声明 `@TableId(type = IdType.AUTO)`（`Customer` / `CustomerFollowUp` /
--   `CustomerLead` / `CustomerOpportunity` / `CustomerPool`；报价同理）——
--   `IdType.AUTO` 表示「由数据库自增填 id」，MyBatis-Plus 生成的 INSERT **不含 id 列**。
--   三张表没有 DEFAULT ⇒ 插入落 `id = NULL` ⇒
--   `null value in column "id" ... violates not-null constraint`。
--
-- 【影响（真机实测，非推断）】
--   · `POST /api/customer`（客户建档）→ **必然 HTTP 400**「请求数据不完整或存在冲突」；
--   · `POST /api/crm/quotation`（报价单创建）→ 同上；
--   · 报价明细 `crm_quotation_item` 同上；
--   · 连带：客户建不出来 ⇒ 客户跟进建不了（`customer_id` NOT NULL）、
--     线索转化 `convertToCustomer` 抛异常 ⇒ `POST /crm/lead/batch-convert` 全量失败。
--   客户页 / 报价单页因此**从未真正写入过数据**（`crm_customer` 真库 0 行）。
--
-- 【根因】`V9.11.0__Create_CRM_Core_Tables.sql` 里这三张表的建表语句写的是
--   `id BIGSERIAL PRIMARY KEY`（有序列），但真库实际生效的建表来自**更早的迁移**
--   —— `CREATE TABLE IF NOT EXISTS` 只认**先执行的那次**，后一条同表定义被整体跳过。
--   属本仓库既有陷阱（见《_开发指南-金标准》§八之二「事故先例」：erp_product_grade_price /
--   erp_product_unit / erp_sale_return 同源）。
--
-- 【修法】补序列并设为列默认值（与同模块其余 19 张表一致），而非改实体为雪花 ID
--   —— 因为建表意图本就是 BIGSERIAL，且 `crm_customer` 已有其它 CRM 表按自增口径引用。
--   序列起点取「现有最大 id + 1」：空表从 1 开始；已有历史行（报价单存量 2 行、
--   明细 4 行，id 为雪花量级）则顺延，绝不与存量行冲突。
-- =============================================================================

-- ── crm_customer ──────────────────────────────────────────────────────────
CREATE SEQUENCE IF NOT EXISTS crm_customer_id_seq;
ALTER TABLE crm_customer ALTER COLUMN id SET DEFAULT nextval('crm_customer_id_seq');
ALTER SEQUENCE crm_customer_id_seq OWNED BY crm_customer.id;
SELECT setval('crm_customer_id_seq',
              GREATEST(COALESCE((SELECT MAX(id) FROM crm_customer), 0), 0) + 1,
              false);

-- ── crm_quotation ─────────────────────────────────────────────────────────
CREATE SEQUENCE IF NOT EXISTS crm_quotation_id_seq;
ALTER TABLE crm_quotation ALTER COLUMN id SET DEFAULT nextval('crm_quotation_id_seq');
ALTER SEQUENCE crm_quotation_id_seq OWNED BY crm_quotation.id;
SELECT setval('crm_quotation_id_seq',
              GREATEST(COALESCE((SELECT MAX(id) FROM crm_quotation), 0), 0) + 1,
              false);

-- ── crm_quotation_item ────────────────────────────────────────────────────
CREATE SEQUENCE IF NOT EXISTS crm_quotation_item_id_seq;
ALTER TABLE crm_quotation_item ALTER COLUMN id SET DEFAULT nextval('crm_quotation_item_id_seq');
ALTER SEQUENCE crm_quotation_item_id_seq OWNED BY crm_quotation_item.id;
SELECT setval('crm_quotation_item_id_seq',
              GREATEST(COALESCE((SELECT MAX(id) FROM crm_quotation_item), 0), 0) + 1,
              false);
