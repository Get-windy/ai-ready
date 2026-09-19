-- =============================================================================
-- 「企业信息」（设置 → 系统配置 → 企业信息，菜单 80624 / set:company-info）收口
-- 2026-09-18
--
-- 背景（《企业信息开发文档》§7 / §9.2 / §12，README §7.1-13）：
--   页面 15 个表单项里 **11 项永远为空** —— `sys_tenant` 表与 `SysTenant` 实体都没有对应列；
--   且「企业名称」以 companyName 提交、实体收的是 tenantName → 保存被 Spring 静默丢弃。
--   本迁移补齐该页需要的档案列（企业档案 10 列 + 纳税人信息 5 列，共 15 列）。
--
-- 命名口径：**沿用本库既有词根，不新造**（实测 information_schema）：
--   credit_code / legal_person / registered_capital / business_scope / industry ← erp_supplier / erp_partner
--   tax_number / bank_name / bank_account ← biz_party / erp_supplier / crm_customer
--   （开发文档 §8.4 曾拟名 `taxpayer_id`；此处改用全库统一的 `tax_number`，避免同一业务含义两个词根）
--
-- 列清单（企业档案 Tab①）：
--   credit_code           统一社会信用代码（18 位）
--   company_type          企业类型
--   legal_person          法定代表人
--   registered_capital    注册资本（万元）
--   industry              所属行业
--   company_scale         企业规模
--   establish_date        成立日期
--   business_scope        经营范围
--   contact_person_phone  联系人电话
--   contact_person_email  联系人邮箱
-- 列清单（纳税人信息 Tab②）：
--   tax_number            纳税人识别号
--   taxpayer_address      地址
--   taxpayer_phone        电话
--   bank_name             开户行地址
--   bank_account          开户行账号
--
-- ⚠️ 全部列**可空、且不带 DEFAULT**：这些列在迁移前的既有语义是「无值」，
--    任何非空 DEFAULT 都会被回填到既有 3 行（id=0 / id=1 系统租户 / id=2 E2E验收租户2），
--    制造出「用户从没填过却有值」的假数据。既有行一律保持 NULL。
--
-- 权限码种子：`set:company-info:save`（保存企业档案）
--   实测（devdb 2026-09-18）：`sys_permission` 中 `set:%` 前缀 **0 行**（全表 264 行），
--   本页写端点若不补码，则除超管（持有 `*` 通配）外所有账号保存一律 403。
--   做法沿用 V11.394.0（操作日志）的既有口径：只授权给超级管理员角色（role_id = 1），
--   普通租户角色请由租户管理员在「系统 → 角色管理」中按需勾选，本迁移不越权代配。
--
-- 幂等：列用 ADD COLUMN IF NOT EXISTS；权限用 WHERE NOT EXISTS，可重复执行。
-- =============================================================================

-- ── ① 企业档案列（Tab① 企业信息）─────────────────────────────────────────────
ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS credit_code          VARCHAR(32);
ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS company_type         VARCHAR(32);
ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS legal_person         VARCHAR(64);
ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS registered_capital   NUMERIC(18, 2);
ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS industry             VARCHAR(64);
ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS company_scale        VARCHAR(32);
ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS establish_date       DATE;
ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS business_scope       TEXT;
ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS contact_person_phone VARCHAR(32);
ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS contact_person_email VARCHAR(128);

-- ── ② 纳税人信息列（Tab② 纳税人信息）─────────────────────────────────────────
ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS tax_number       VARCHAR(64);
ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS taxpayer_address VARCHAR(255);
ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS taxpayer_phone   VARCHAR(32);
ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS bank_name        VARCHAR(128);
ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS bank_account     VARCHAR(64);

COMMENT ON COLUMN sys_tenant.credit_code          IS '统一社会信用代码（18 位，企业信息页）';
COMMENT ON COLUMN sys_tenant.company_type         IS '企业类型（有限责任公司/股份有限公司/合伙企业/个体工商户）';
COMMENT ON COLUMN sys_tenant.legal_person         IS '法定代表人';
COMMENT ON COLUMN sys_tenant.registered_capital   IS '注册资本（万元）';
COMMENT ON COLUMN sys_tenant.industry             IS '所属行业';
COMMENT ON COLUMN sys_tenant.company_scale        IS '企业规模';
COMMENT ON COLUMN sys_tenant.establish_date       IS '成立日期';
COMMENT ON COLUMN sys_tenant.business_scope       IS '经营范围';
COMMENT ON COLUMN sys_tenant.contact_person_phone IS '联系人电话';
COMMENT ON COLUMN sys_tenant.contact_person_email IS '联系人邮箱';
COMMENT ON COLUMN sys_tenant.tax_number           IS '纳税人识别号（开票资料）';
COMMENT ON COLUMN sys_tenant.taxpayer_address     IS '开票地址';
COMMENT ON COLUMN sys_tenant.taxpayer_phone       IS '开票电话';
COMMENT ON COLUMN sys_tenant.bank_name            IS '开户行地址';
COMMENT ON COLUMN sys_tenant.bank_account         IS '开户行账号';

-- ── ③ 权限码种子：set:company-info:save ───────────────────────────────────────
-- ⚠️ id 由 91301 改为 91309（2026-09-18）：本迁移与 V11.395.0__Seed_Payment_Config_Permissions.sql
--    并发写入时抢了同一个 id —— V11.395.0 已占用 91301..91308（payment:*），
--    于是本迁移在应用时报 `重复键违反唯一约束 "sys_permission_pkey"：键值 (id)=(91301) 已经存在`，
--    直接阻断后端启动（Flyway 失败 → 应用起不来）。91309 已核 devdb 空闲。
--    新增固定 id 的权限种子迁移前，务必先 `SELECT id FROM sys_permission WHERE id BETWEEN ... ORDER BY id` 实测占用。
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
SELECT 91309::BIGINT, 1, 0, 0, now(), now(), '保存企业信息', 'set:company-info:save', 3,
       '/api/tenant/current', 'PUT', 397, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = 'set:company-info:save');

-- 授权给超级管理员角色（role_id = 1），已存在则跳过
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9130901::BIGINT, 1, p.id, 1, now()
FROM sys_permission p
WHERE p.permission_code = 'set:company-info:save'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
