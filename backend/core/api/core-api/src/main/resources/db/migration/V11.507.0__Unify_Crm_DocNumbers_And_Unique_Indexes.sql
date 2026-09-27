-- 2026-09-26 CRM 单号体系统一 + 唯一索引改部分唯一（见 I:/AI-Ready/CRM_MODULE_AUDIT_20260923.md §2.6）
--
-- 背景（两个缺陷叠在一起）：
--   ① 号段算法是「查当日最大号 + 1」，**天然有并发竞态** —— 两个请求算出同一个号；
--   ② CRM 域 12 个业务单号唯一索引**不含 deleted**，而取号 SQL 有的带 `deleted = 0`
--      ⇒ 逻辑删除的行不参与计数但占着号 ⇒ 把当天最后一张单删掉后新建必撞唯一索引（前端看到 400）。
--      域内修复进度还分裂：5 张表已换成「不带 deleted 的当日最大号」，其余 4 处仍是旧写法。
--
-- 本迁移只做「数据结构 + 号段种子」两件事，算法侧改为复用系统统一号段服务
-- （`cn.aiedge.common.serial.BizNumberGeneratorService`，实现是 biz_number_sequence 上的
--  SELECT ... FOR UPDATE + UPDATE，跨进程原子；格式 {prefix}-{yyyyMMdd}-{NNNN}，按日重置）。
--
-- ⚠️ 号段格式变化（用户可见）：例如客户编码由 `CUS-202609180001` 变为 `CUS-20260926-0001`。
--    存量单号不动（单号是不透明字符串），只影响新生成的号。

-- ══════════════════════════════════════════════════════════════════════════
-- 一、CRM 号段种子（biz_type 用 CRM_ 前缀，避免与既有 HT(HR劳动合同)/KH(客户编码) 等撞型）
-- ══════════════════════════════════════════════════════════════════════════
-- 唯一键是 (tenant_id, biz_type, locale)。这里只给租户 1 种；
-- 其它租户首次使用时会由 BizNumberSequenceMapper#seedMissingSequence 按同 biz_type 模板自愈。

INSERT INTO biz_number_sequence (biz_type, locale, seq_date, current_seq, prefix, seq_length, tenant_id, max_seq) VALUES
    ('CRM_CUSTOMER',   'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'CUS',  4, 1, 999999),
    ('CRM_LEAD',       'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'LEAD', 4, 1, 999999),
    ('CRM_OPPORTUNITY','zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'OPP',  4, 1, 999999),
    ('CRM_FOLLOWUP',   'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'FUP',  4, 1, 999999),
    ('CRM_VISITPLAN',  'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'VP',   4, 1, 999999),
    ('CRM_CONTRACT',   'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'CT',   4, 1, 999999),
    ('CRM_QUOTATION',  'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'QT',   4, 1, 999999),
    ('CRM_QUOTTPL',    'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'QTPL', 4, 1, 999999),
    ('CRM_CAMPAIGN',   'zh_CN', TO_CHAR(CURRENT_DATE, 'YYYYMMDD'), 0, 'MC',   4, 1, 999999)
ON CONFLICT (tenant_id, biz_type, locale) DO NOTHING;

-- ══════════════════════════════════════════════════════════════════════════
-- 二、12 个业务单号唯一索引 → 部分唯一索引（WHERE deleted = 0）
-- ══════════════════════════════════════════════════════════════════════════
-- 语义最正确：唯一性只约束「活着的行」。逻辑删除的行不再占用号段，
-- 也不再与新建行冲突；同时保留「同一业务单号不可重复建单」的约束。
--
-- ⚠️ 前置条件：各表**已存在**的 deleted = 0 行之间不能有重复单号，否则建索引失败。
--    本迁移前已核：CRM 域 22 张表合计仅 8 行数据（crm_customer 0 / crm_contract 0 /
--    crm_quotation 2 / crm_customer_lead 2 且均为 deleted=1 / crm_customer_opportunity 4），
--    且一直挂在这些索引上 ⇒ 无重复，可安全重建。

DROP INDEX IF EXISTS uk_crm_contract_no;
CREATE UNIQUE INDEX uk_crm_contract_no ON crm_contract (contract_no) WHERE deleted = 0;

DROP INDEX IF EXISTS uk_crm_contract_change_no;
CREATE UNIQUE INDEX uk_crm_contract_change_no ON crm_contract_change (change_no) WHERE deleted = 0;

DROP INDEX IF EXISTS uk_crm_customer_code;
CREATE UNIQUE INDEX uk_crm_customer_code ON crm_customer (customer_code) WHERE deleted = 0;

DROP INDEX IF EXISTS uk_crm_follow_up_code;
CREATE UNIQUE INDEX uk_crm_follow_up_code ON crm_customer_follow_up (follow_up_code) WHERE deleted = 0;

DROP INDEX IF EXISTS uk_crm_lead_code;
CREATE UNIQUE INDEX uk_crm_lead_code ON crm_customer_lead (lead_code) WHERE deleted = 0;

DROP INDEX IF EXISTS uk_crm_opportunity_code;
CREATE UNIQUE INDEX uk_crm_opportunity_code ON crm_customer_opportunity (opportunity_code) WHERE deleted = 0;

DROP INDEX IF EXISTS uk_crm_campaign_code;
CREATE UNIQUE INDEX uk_crm_campaign_code ON crm_marketing_campaign (campaign_code) WHERE deleted = 0;

DROP INDEX IF EXISTS uk_crm_channel_code;
CREATE UNIQUE INDEX uk_crm_channel_code ON crm_marketing_channel (channel_code) WHERE deleted = 0;

DROP INDEX IF EXISTS uk_crm_content_code;
CREATE UNIQUE INDEX uk_crm_content_code ON crm_marketing_content (content_code) WHERE deleted = 0;

DROP INDEX IF EXISTS uk_crm_quotation_no;
CREATE UNIQUE INDEX uk_crm_quotation_no ON crm_quotation (quotation_no) WHERE deleted = 0;

DROP INDEX IF EXISTS uk_crm_quotation_template_code;
CREATE UNIQUE INDEX uk_crm_quotation_template_code ON crm_quotation_template (template_code) WHERE deleted = 0;

DROP INDEX IF EXISTS uk_crm_visit_plan_no;
CREATE UNIQUE INDEX uk_crm_visit_plan_no ON crm_visit_plan (plan_no) WHERE deleted = 0;

-- ══════════════════════════════════════════════════════════════════════════
-- 三、自检（跑完应全部返回 0 / 空）
-- ══════════════════════════════════════════════════════════════════════════
-- 1) 号段种子齐否：应返回 9
--   SELECT count(*) FROM biz_number_sequence WHERE tenant_id = 1 AND locale = 'zh_CN' AND biz_type LIKE 'CRM_%';
-- 2) 还有没有非部分的 uk_crm_* 唯一索引：应返回 0
--   SELECT count(*) FROM pg_indexes WHERE schemaname='public' AND tablename LIKE 'crm\_%'
--     AND indexname LIKE 'uk_crm_%' AND indexdef LIKE 'CREATE UNIQUE INDEX%'
--     AND indexdef NOT LIKE '%WHERE%';
