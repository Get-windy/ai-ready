-- =============================================================================
-- 采购合同表补建（2026-09-21）
--
-- 【为什么】菜单 81010「采购合同」→ 页面 `views/erp/purchase-contract/index.vue`
--   → `GET /erp/purchase/contract/page`，但：
--     ① `PurchaseContractController` **没有 /page 映射**（只有 GET /{id}、/by-no/{}、
--        /supplier/{}、/statistics 与 5 个 POST）⇒ 404；
--     ② `PurchaseContractMapper` 全部 `FROM purchase_contract`，而**该表在库里根本不存在**
--        （实测 information_schema 无此表）⇒ 即便补了 /page 也是 500。
--   即「菜单在、页面在、接口缺、表缺」的四重断点，功能整体不可用。
--
-- 【决策（2026-09-21，用户）】保留 ERP 侧合同能力：**补建表 + 补 /page 与 CRUD 接口**。
--   （未采用「删菜单 + 合同归 CRM」的替代方案。）
--
-- 【为什么带 tenant_id】合同是**租户业务数据**，不是全局数据。
--   · 主表 `purchase_contract` 必须有 tenant_id：多租户插件会往 SELECT/UPDATE/DELETE
--     注入 `tenant_id = 当前租户`（本表不在忽略清单里）。
--   · 子表同样带 tenant_id 做纵深防御：本仓有前车之鉴 —— 子表若漏 tenant_id 又不在忽略清单，
--     拦截器注入 `AND tenant_id = X` 会直接报「字段不存在」；而若漏写插入值，则会落成
--     `tenant_id = 0` 的「谁都不看不见」数据（见 平台-BREAK-03 的 51 行 NULL 数据）。
--   · ⚠️ 本仓的 `PurchaseContractMapper` / `PurchaseContractItemMapper` 用的是**自定义
--     `@Insert` 注解 SQL，不走 MyBatis-Plus 的 `insertFill`** ⇒ 租户必须由 Service 显式 set，
--     并在 INSERT 列表里显式带上 tenant_id。仅靠实体字段是**不会**被自动填充的。
--
-- 【口径】对齐 V11.434.0 / V11.438.0 的权限种子写法：
--   sys_permission: tenant_id=0、parent_id=0、permission_type=3、status=0（0=正常）、visible=1。
--   sys_role_permission 必须显式关联 role_id = 1（SUPER_ADMIN）——
--   本仓超管权限来自该关联表，不关联则平台管理员自己也会被 403。
--
-- 【id 依据】实测 2026-09-21：`SELECT count(*) FROM sys_permission WHERE id BETWEEN 90068 AND 90100` = 0。
--   ⚠️ 90066/90067 已被 V11.438.0 占用，禁止复用。
-- =============================================================================

-- ── 1. 合同主表 ──
CREATE TABLE IF NOT EXISTS purchase_contract (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL DEFAULT 0,
    contract_no         VARCHAR(64)  NOT NULL,
    inquiry_id          BIGINT,
    quote_id            BIGINT,
    supplier_id         BIGINT,
    supplier_name       VARCHAR(200),
    contract_title      VARCHAR(200) NOT NULL,
    contract_type       VARCHAR(32),
    contract_status     VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    total_amount        NUMERIC(18, 2) NOT NULL DEFAULT 0,
    executed_amount     NUMERIC(18, 2) NOT NULL DEFAULT 0,
    executed_percent    NUMERIC(5, 2)  NOT NULL DEFAULT 0,
    start_date          TIMESTAMP,
    end_date            TIMESTAMP,
    payment_terms       VARCHAR(500),
    delivery_terms      VARCHAR(500),
    quality_standard    VARCHAR(500),
    warranty_period     VARCHAR(64),
    submit_time         TIMESTAMP,
    approver_id         BIGINT,
    approval_time       TIMESTAMP,
    approval_comment    VARCHAR(500),
    activation_time     TIMESTAMP,
    completion_time     TIMESTAMP,
    termination_time    TIMESTAMP,
    termination_reason  VARCHAR(500),
    archive_no          VARCHAR(64),
    archive_time        TIMESTAMP,
    modification_no     VARCHAR(64),
    modification_reason VARCHAR(500),
    contract_file_url   VARCHAR(500),
    remark              VARCHAR(500),
    created_by          BIGINT,
    created_at          TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT now()
);

-- 编号唯一按租户隔离：不同租户可以各自出现相同合同号
CREATE UNIQUE INDEX IF NOT EXISTS uk_purchase_contract_no_tenant
    ON purchase_contract (tenant_id, contract_no);
CREATE INDEX IF NOT EXISTS idx_purchase_contract_supplier ON purchase_contract (supplier_id);
CREATE INDEX IF NOT EXISTS idx_purchase_contract_status   ON purchase_contract (contract_status);
CREATE INDEX IF NOT EXISTS idx_purchase_contract_end_date ON purchase_contract (end_date);

COMMENT ON TABLE purchase_contract IS '采购合同主表（菜单 81010）；租户业务数据，tenant_id 由 Service 显式写入';

-- ── 2. 合同明细表 ──
CREATE TABLE IF NOT EXISTS purchase_contract_item (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT NOT NULL DEFAULT 0,
    contract_id       BIGINT NOT NULL,
    material_name     VARCHAR(200),
    specification     VARCHAR(200),
    unit              VARCHAR(32),
    quantity          NUMERIC(18, 4) NOT NULL DEFAULT 0,
    unit_price        NUMERIC(18, 4) NOT NULL DEFAULT 0,
    amount            NUMERIC(18, 2) NOT NULL DEFAULT 0,
    tax_rate          NUMERIC(8, 4),
    tax_amount        NUMERIC(18, 2),
    brand             VARCHAR(100),
    model             VARCHAR(100),
    quality_level     VARCHAR(64),
    origin_country    VARCHAR(64),
    lead_time         INTEGER,
    delivery_location VARCHAR(200),
    item_note         VARCHAR(500),
    created_at        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_purchase_contract_item_contract ON purchase_contract_item (contract_id);

COMMENT ON TABLE purchase_contract_item IS '采购合同明细表（子表，随主表 tenant_id）';

-- ── 3. 合同变更记录表 ──
-- `PurchaseContractMapper.insertModification` 引用该表；此前同样缺失。
CREATE TABLE IF NOT EXISTS purchase_contract_modification (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL DEFAULT 0,
    contract_id         BIGINT NOT NULL,
    modification_no     VARCHAR(64),
    modification_reason VARCHAR(500),
    created_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_purchase_contract_mod_contract ON purchase_contract_modification (contract_id);

COMMENT ON TABLE purchase_contract_modification IS '采购合同变更记录表';

-- ── 4. 权限码种子（先补码，再补注解 —— 顺序不能反） ──
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
VALUES
 (90068, 0, 0, 0, now(), now(), '采购合同查询', 'purchase:contract:list',   3, '/api/erp/purchase/contract/page',        'GET',    1100, 1, 0),
 (90069, 0, 0, 0, now(), now(), '采购合同详情', 'purchase:contract:detail', 3, '/api/erp/purchase/contract/{id}',        'GET',    1101, 1, 0),
 (90070, 0, 0, 0, now(), now(), '采购合同新增', 'purchase:contract:create', 3, '/api/erp/purchase/contract',             'POST',   1102, 1, 0),
 (90071, 0, 0, 0, now(), now(), '采购合同编辑', 'purchase:contract:update', 3, '/api/erp/purchase/contract/{id}',        'PUT',    1103, 1, 0),
 (90072, 0, 0, 0, now(), now(), '采购合同删除', 'purchase:contract:delete', 3, '/api/erp/purchase/contract/{id}',        'DELETE', 1104, 1, 0),
 (90073, 0, 0, 0, now(), now(), '采购合同审批', 'purchase:contract:approve',3, '/api/erp/purchase/contract/{id}/approve','POST',   1105, 1, 0),
 (90074, 0, 0, 0, now(), now(), '采购合同导出', 'purchase:contract:export', 3, '/api/erp/purchase/contract/export',      'GET',    1106, 1, 0)
ON CONFLICT (id) DO NOTHING;

-- 关联超管角色：不关联则平台管理员自己也会被拒（本仓超管权限来自该关联表）
-- ⚠️ 命名对齐同域既有码的约定 `purchase:<资源>:<动作>`（用 detail/update，不是 view/edit），
--    见 `purchase:order:*` / `purchase:cost-sharing:*`，避免自造第三种风格。
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9400000 + (p.sort - 1100), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.permission_code LIKE 'purchase:contract:%'
  AND p.id BETWEEN 90068 AND 90074
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
