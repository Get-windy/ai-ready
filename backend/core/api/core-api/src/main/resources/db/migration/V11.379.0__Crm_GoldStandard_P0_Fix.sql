-- =============================================================================
-- CRM 模块金标准 · P0 修复（2026-09-18）
--
-- 背景：CRM 为本系统独有模块（ql361 对标系统无 CRM 域），按业界成熟 CRM 建模。
--       本迁移处理 README《CRM 模块 · 开发文档集》§7 中的两条 P0：
--
--   P0-1  三张 crm 表漏建 tenant_id 列，且未登记进 MyBatisPlusConfig.IGNORE_TENANT_TABLES
--         → 多租户拦截器注入 `AND tenant_id = ?` → SQL 报「字段 tenant_id 不存在」
--         → 线索页 / 商机页 / 客户跟进页在登录态下整页 500。
--         同族的 crm_customer / crm_customer_pool / crm_contract / crm_quotation /
--         crm_visit_plan / crm_visit_record **都有**该列 —— 属漏建，不是设计如此。
--         修法：补列（与 crm_customer 一致，DEFAULT 1），而非把三张业务表加进忽略表
--         （加忽略表会让这三张表彻底失去租户隔离）。
--
--   P0-2  sys_permission 中 `crm%` 权限码只有 5 条（crm:customer:create|delete|list|update
--         + crm:manage），而前端 16 个页面使用了 60+ 个未登记码 → 非超管账号下受控按钮
--         全部隐藏。本迁移按前端实际引用补齐；已存在的码不重复插入（幂等写法）。
--         （超管角色走 `*` 通配，不受影响，故该条不阻塞验收，但必须补全。）
--
-- 说明：本迁移**不改** crm_customer.status —— 真库实测该列默认值已是 1，
--       与 form.vue / customer-grade / customer-analysis / /dropdown 的「1=正常」一致；
--       唯一按「0=正常」判读的是客户列表页前端，属前端缺陷，在页面侧修正。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. 三张业务表补 tenant_id 列
--    表当前 0 行（2026-09-18 实测），DEFAULT 1 不会翻转任何存量行语义；
--    与 crm_customer.tenant_id（DEFAULT 1）保持完全一致。
-- ---------------------------------------------------------------------------
ALTER TABLE crm_customer_lead        ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT 1;
ALTER TABLE crm_customer_opportunity ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT 1;
ALTER TABLE crm_customer_follow_up   ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT 1;

COMMENT ON COLUMN crm_customer_lead.tenant_id        IS '租户ID（多租户隔离，与 crm_customer 一致）';
COMMENT ON COLUMN crm_customer_opportunity.tenant_id IS '租户ID（多租户隔离，与 crm_customer 一致）';
COMMENT ON COLUMN crm_customer_follow_up.tenant_id   IS '租户ID（多租户隔离，与 crm_customer 一致）';

CREATE INDEX IF NOT EXISTS idx_crm_lead_tenant        ON crm_customer_lead(tenant_id);
CREATE INDEX IF NOT EXISTS idx_crm_opportunity_tenant ON crm_customer_opportunity(tenant_id);
CREATE INDEX IF NOT EXISTS idx_crm_follow_up_tenant   ON crm_customer_follow_up(tenant_id);

-- ---------------------------------------------------------------------------
-- 2. 补齐 CRM 权限码种子
--    取值逐条来自前端源码实际引用的 `v-permission="'crm:xxx'"`（2026-09-18 grep 全量）。
--    permission_type = 3（按钮级），parent_id = 0，tenant_id = 0（系统级定义，与 finance: 段一致）。
--    id 段 91000-91099 为 CRM 专用（90000-90099 已被 finance 占用）。
--    幂等：按 permission_code 去重，已登记的不再插入（存量 5 条不受影响）。
-- ---------------------------------------------------------------------------
INSERT INTO sys_permission
  (id, tenant_id, parent_id, deleted, create_time, update_time,
   permission_name, permission_code, permission_type, sort, visible, status, remark)
SELECT v.id, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
       v.pname, v.pcode, 3, v.sort, 1, 0, v.remark
FROM (VALUES
  -- 客户
  (91001, 'CRM客户查询',     'crm:customer:view',            100, 'CRM客户页'),
  (91003, 'CRM客户编辑',     'crm:customer:edit',            102, 'CRM客户页'),
  (91004, 'CRM客户刷新',     'crm:customer:refresh',         103, 'CRM客户页'),
  (91005, 'CRM客户批量分配', 'crm:customer:batchassign',     104, 'CRM客户页'),
  (91006, 'CRM客户导入',     'crm:customer:import',          105, 'CRM客户页'),
  (91007, 'CRM客户跟进',     'crm:customer:follow',          106, 'CRM客户页'),
  (91008, 'CRM客户加入分级', 'crm:customer:addtolevel',      107, 'CRM客户分级页'),
  -- 线索
  (91011, 'CRM线索查询',     'crm:lead:view',        120, 'CRM线索页'),
  (91012, 'CRM线索新增',     'crm:lead:create',      121, 'CRM线索页'),
  (91013, 'CRM线索编辑',     'crm:lead:edit',        122, 'CRM线索页'),
  (91014, 'CRM线索导入',     'crm:lead:import',      123, 'CRM线索页'),
  (91015, 'CRM线索分配',     'crm:lead:assign',      124, 'CRM线索页'),
  (91016, 'CRM线索批量分配', 'crm:lead:batchassign', 125, 'CRM线索页'),
  (91017, 'CRM线索转化',     'crm:lead:convert',     126, 'CRM线索页'),
  (91018, 'CRM线索批量转化', 'crm:lead:batchconvert', 127, 'CRM线索转化页'),
  -- 商机
  (91021, 'CRM商机查询',     'crm:opportunity:view',           140, 'CRM商机页'),
  (91022, 'CRM商机新增',     'crm:opportunity:create',         141, 'CRM商机页'),
  (91023, 'CRM商机编辑',     'crm:opportunity:edit',           142, 'CRM商机页'),
  (91024, 'CRM商机移动阶段', 'crm:opportunity:move',           143, 'CRM商机页'),
  (91025, 'CRM商机转报价',   'crm:opportunity:convert',        144, 'CRM商机页'),
  (91026, 'CRM商机刷新',     'crm:opportunity:detailrefresh',  145, 'CRM商机页'),
  (91027, 'CRM商机重置',     'crm:opportunity:reset',          146, 'CRM商机页'),
  (91028, 'CRM商机搜索',     'crm:opportunity:search',         147, 'CRM商机页'),
  -- 报价单
  (91031, 'CRM报价查询',     'crm:quotation:view',             160, 'CRM报价单页'),
  (91032, 'CRM报价新增',     'crm:quotation:create',           161, 'CRM报价单页'),
  (91033, 'CRM报价编辑',     'crm:quotation:edit',             162, 'CRM报价单页'),
  (91034, 'CRM报价发送',     'crm:quotation:send',             163, 'CRM报价单页'),
  (91035, 'CRM报价批量发送', 'crm:quotation:batchsend',        164, 'CRM报价单页'),
  (91036, 'CRM报价转订单',   'crm:quotation:convert',          165, 'CRM报价单页'),
  (91037, 'CRM报价明细转订单', 'crm:quotation:convertfromdetail', 166, 'CRM报价单页'),
  (91038, 'CRM报价下载PDF',  'crm:quotation:downloadpdf',      167, 'CRM报价单页'),
  (91039, 'CRM报价刷新',     'crm:quotation:detailrefresh',    168, 'CRM报价单页'),
  (91040, 'CRM报价明细发送', 'crm:quotation:sendfromdetail',   169, 'CRM报价单页'),
  -- 合同
  (91051, 'CRM合同查询',     'crm:contract:view',          180, 'CRM合同页'),
  (91052, 'CRM合同新增',     'crm:contract:create',        181, 'CRM合同页'),
  (91053, 'CRM合同编辑',     'crm:contract:edit',          182, 'CRM合同页'),
  (91054, 'CRM合同审批',     'crm:contract:approve',       183, 'CRM合同审批页'),
  (91055, 'CRM合同批量审批', 'crm:contract:batchapprove',  184, 'CRM合同审批页'),
  (91056, 'CRM合同签署',     'crm:contract:sign',          185, 'CRM合同页'),
  (91057, 'CRM合同下载',     'crm:contract:download',      186, 'CRM合同页'),
  (91058, 'CRM合同续签',     'crm:contract:renewapply',    187, 'CRM合同页'),
  (91059, 'CRM合同刷新',     'crm:contract:refresh',       188, 'CRM合同页'),
  (91060, 'CRM合同明细刷新', 'crm:contract:detailrefresh', 189, 'CRM合同页'),
  -- 发票
  (91071, 'CRM发票查询',     'crm:invoice:view',           200, 'CRM发票页'),
  (91072, 'CRM发票新增',     'crm:invoice:create',         201, 'CRM发票页'),
  (91073, 'CRM发票编辑',     'crm:invoice:edit',           202, 'CRM发票页'),
  (91074, 'CRM发票开具',     'crm:invoice:issue',          203, 'CRM发票页'),
  (91075, 'CRM发票作废',     'crm:invoice:cancelconfirm',  204, 'CRM发票页'),
  (91076, 'CRM发票发送',     'crm:invoice:send',           205, 'CRM发票页'),
  (91077, 'CRM发票明细刷新', 'crm:invoice:detailrefresh',  206, 'CRM发票页'),
  -- 通用（工具栏级）
  (91091, 'CRM新增', 'crm:create',  220, 'CRM通用'),
  (91092, 'CRM刷新', 'crm:refresh', 221, 'CRM通用')
) AS v(id, pname, pcode, sort, remark)
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission p
  WHERE p.permission_code = v.pcode AND p.deleted = 0
);
