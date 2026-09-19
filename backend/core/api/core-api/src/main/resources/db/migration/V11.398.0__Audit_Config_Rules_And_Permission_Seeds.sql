-- =============================================================================
-- 「审核设置」（设置 → 系统配置 → 审核设置，菜单 80622 / set:audit-config）金标准落地
-- 2026-09-18
--
-- 背景（《审核设置开发文档》§6.4 / §8.4 / §12）：
--   ① 本页对标 ql361 的形态是「16 类单据 × 审核条件 × 审批人集合」的**规则矩阵**
--      （三列：单据 / 审核设置 / 摘要），而本系统原实现是「流程定义台账」，语义完全不对标；
--      ql361 摘要文案形如「商品低于成本价时提交给[杨生淮]审核;」，摘要由后端按规则拼装。
--   ② 文档 §6.4 明确登记后端缺口：**无「按单据类型」的审核规则端点** —— 本轮补齐。
--   ③ 文档 §8.4「不发明字段」承诺：规则本身可落在已有列上，但对「规则矩阵」需要一个
--      **按 (租户, 单据类型) 存储规则 JSON 的载体**；文档不承诺把规则混进 `workflow_definition`
--      （那会让「流程定义」页 801/80610 的台账里凭空多出 16 条与流程引擎无关的行），故本轮
--      新建独立表 `sys_audit_rule`（一页一表，不污染流程定义台账）。
--   ④ 权限：本页新端点带方法级权限码 `workflow:audit:list` / `workflow:audit:update`；
--      实测（devdb 2026-09-18）`sys_permission` 中 `workflow%` 前缀 **0 行**（全表 264 行）
--      → 不补种子，除超管（走 `*` 通配）外所有账号访问本页会 403。
--
-- 16 类单据名逐字取自 ql361 实测（文档 §8.1-3），**不增不减**：
--   销售出库单 / 销售订单 / 销售退货申请单 / 商城订单取消 / 采购入库单 / 采购订单 /
--   费用单 / 收款单 / 付款单 / 预收款单 / 预付款单 / 会计凭证 / 预订货单 / 调拨单 /
--   费用合同 / 调拨申请单
--   （单据类型编码为技术键，中文名为展示名；两者在后端 AuditRuleCatalog 中成对登记）
--
-- ⚠️ 本迁移**不播种任何规则数据**：ql361 实测 16 行中仅 3 行有摘要，其审批人是 ql361 的
--   职员（本库不存在同名账号）—— 播种会造假数据，故 16 行统一以「未配置」起步，
--   由租户管理员在页面上自行配置。
--
-- 幂等：建表/建索引用 IF NOT EXISTS，权限种子用 WHERE NOT EXISTS，可重复执行。
-- id 区间：permission 91351–91352 / role_permission 9130301–9130302
--   ⚠️ permission 原为 91301–91302，2026-09-18 与 V11.395.0__Seed_Payment_Config_Permissions.sql
--      并发撞号（后者先应用并占用 91301–91308，本迁移随后报
--      `重复键违反唯一约束 "sys_permission_pkey"：键值 (id)=(91301) 已经存在` → 阻断后端启动），
--      已改为 91351–91352（已核 devdb 空闲）。新增固定 id 的种子前务必实测占用。
-- =============================================================================

-- ── 1. 审核规则表（按 租户 × 单据类型 存一组「条件 → 审批人集合」规则） ──
CREATE TABLE IF NOT EXISTS sys_audit_rule (
    id          BIGINT PRIMARY KEY,
    tenant_id   BIGINT      NOT NULL DEFAULT 0,
    /** 单据类型编码（AuditRuleCatalog 的 16 类之一，如 sale_outbound） */
    doc_type    VARCHAR(64) NOT NULL,
    /**
     * 规则 JSON 数组，元素形如：
     *   {"condition":"below_cost","approvers":[{"userId":"123","userName":"杨生淮"}]}
     * 仅存「已启用」的规则；摘要文案由后端按规则实时拼装（不落库，避免双真源）。
     */
    rules       TEXT,
    /** 逻辑删除（0 未删 / 1 已删）—— 与 BaseEntity 的 @TableLogic 对应 */
    deleted     INTEGER     NOT NULL DEFAULT 0,
    /** 乐观锁版本号 —— 与 BaseEntity 的 @Version 对应 */
    version     INTEGER     NOT NULL DEFAULT 1,
    create_by   BIGINT,
    create_time TIMESTAMP,
    update_by   BIGINT,
    update_time TIMESTAMP
);

-- 同一租户下每种单据类型只允许一条有效规则（逻辑删除后允许重建，故带 deleted = 0 条件）
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_audit_rule_tenant_doc
    ON sys_audit_rule (tenant_id, doc_type) WHERE deleted = 0;

COMMENT ON TABLE sys_audit_rule IS '单据审核规则（审核设置 80622：单据类型 → 条件 → 审批人集合）';
COMMENT ON COLUMN sys_audit_rule.doc_type IS '单据类型编码，取值见后端 AuditRuleCatalog（16 类）';
COMMENT ON COLUMN sys_audit_rule.rules IS '已启用规则 JSON 数组：[{condition, approvers:[{userId,userName}]}]';

-- ── 2. 权限码种子（与 WorkflowController 上新端点的方法级注解一一对应） ──
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
SELECT v.id, 1, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
    (91351::BIGINT, '审核设置查询', 'workflow:audit:list',   '/api/workflow/audit-config/list',       'GET', 397),
    (91352::BIGINT, '审核设置保存', 'workflow:audit:update', '/api/workflow/audit-config/{docType}', 'PUT', 398)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

-- 授权给超级管理员角色（role_id = 1）
-- 口径同 V11.362.0 / V11.394.0：超管另有 `*` 通配，这里只为让「权限清单」完整可审计；
-- 普通租户角色请由租户管理员在「系统 → 角色管理」中按需勾选（本迁移不越权代配）。
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9130300 + (p.sort - 396), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.permission_code IN ('workflow:audit:list', 'workflow:audit:update')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
