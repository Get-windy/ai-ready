-- =============================================================================
-- V11.399.0：财务期初（设置 → 数据录入 → 财务期初，菜单 70551 / set:initial-finance）
--   建 2 张表 + 权限码种子
--   依据《设置模块/财务期初开发文档.md》§7.3「路线 B（两类表）」裁定。
--
-- 【修的是什么（P0）】
--   开发文档 §12-P0 第 1 条：本页是 **218 行纯前端壳**，后端零控制器、库零表
--   （`information_schema.tables` 按 `%initial%` 查询 0 行）→ 前端 5 个调用全部 404。
--
-- 【为什么是两张表（不压成一张）】
--   对标 ql361「设置 → 期初录入 → 财务期初」是 **5 个结构不同的 Tab**：
--     银行现金期初   3 列：科目编号 · 科目名称 · 期初金额
--     应付期初       4 列：供应商编号 · 供应商名称 · 应付金额 · 预付金额
--     应收期初       5 列：客户编号 · 客户名称 · 默认经手人 · 应收金额 · 预收金额
--     固定资产期初   3 列：科目编号 · 科目名称 · 期初金额
--     资产负债期初   4 列：科目编号 · 科目名称 · 借贷方向 · 期初金额
--   其中 **两种维度**：前 3 个 Tab 的 3 个「按科目」（bank/fixed/balance）与 2 个「按往来单位」
--   结构互斥 —— 应付 Tab 无「默认经手人」、应收 Tab 无「预付金额」，混表会导致列稀疏 +
--   约束无法表达（无法要求「应收必须有客户」）。故按 §7.3 路线 B 拆两张：
--     ① erp_initial_finance_subject  按科目：银行现金 / 固定资产 / 资产负债
--     ② erp_initial_finance_partner  按往来：应付 / 应收
--
-- 【字段来源（不发明字段）】
--   所有业务列**逐字取自 ql361 实测列名**（财务期初.json 各 Tab 的 columnConfig.cols）；
--   另有 ① 显式化的隐含字段 `period_year`（对标无年度列，年度由「当前会计年」隐含，
--   开发文档 §5.3 已登记「口径风险」→ 显式落库），② 全站统一审计列。
--   **不新增**：期初币种 / 期初汇率 / 辅助核算维度 / 多经手人 / 审批状态（期初不是单据）。
--   **不加 `remark`**：开发文档 §3.7 明确「备注」是本系统自加、对标无该列。
--
-- 【红线遵守】
--   · 新增表/列一律**可空**、无破坏性 DEFAULT（`deleted` 的 DEFAULT 0 为逻辑删除必需，
--     且不影响存量数据——本表为新建表）。
--   · 期初数据是**租户级**的：两张表都**带 tenant_id**，故**不加入** MyBatisPlusConfig 的
--     IGNORE_TENANT_TABLES（加入反而会丢掉隔离）。
--   · 主键为 BIGINT 且由应用侧雪花算法（MyBatis-Plus IdType.ASSIGN_ID）生成 →
--     **无需数据库序列**（对照 CRM 三表「缺主键序列致建档必 400」的坑）。
--
-- 【权限码】
--   本页自有 4 个码，与 InitialFinanceController 的注解一一对应：
--     set:initial-finance:view    GET    /api/erp/finance/initial/subject/page、/partner/page、
--                                        /export、/trial-balance
--     set:initial-finance:create  POST   /api/erp/finance/initial/subject/save、/partner/save
--     set:initial-finance:update  PUT    /api/erp/finance/initial/subject/update、/partner/update
--     set:initial-finance:delete  DELETE /api/erp/finance/initial/subject/{id}、/partner/{id}
--   另补 `finance:subject:view`（本页「科目」下拉的数据源 AccountSubjectController#list 要求的码）：
--   实测 devdb 2026-09-18 `finance:*` 共 24 行**无 subject** → 不补种子则非超管账号下拉恒 403。
--   ⚠️ 会计科目页的 create/edit/delete 三个码仍缺（不在本页范围，未越权代配）。
--
-- 【id 区间】permission 91341–91345 / role_permission 9130741–9130745。
--   已核 devdb 该区间空闲（占用情况：91001–91193、91201–91206、91301–91309、91321–91322）。
--   新增固定 id 的种子前务必实测占用 —— V11.396.0 与 V11.395.0 曾因抢 91301 直接阻断启动。
--
-- 幂等：建表 IF NOT EXISTS、索引 IF NOT EXISTS、种子 WHERE NOT EXISTS，可重复执行。
-- =============================================================================

-- ── 1. 按科目的财务期初（银行现金 / 固定资产 / 资产负债 三选一） ──
CREATE TABLE IF NOT EXISTS erp_initial_finance_subject (
    id             BIGINT PRIMARY KEY,                 -- 雪花 ID（应用侧生成，无需序列）
    tenant_id      BIGINT,                             -- 租户（多租户拦截器自动注入）
    initial_type   VARCHAR(30),                        -- 期初类型：BANK_CASH 银行现金 / FIXED_ASSET 固定资产 / BALANCE_SHEET 资产负债
    period_year    INT,                                -- 期初年度（对标无该列，为「当前会计年」的显式化）
    subject_id     BIGINT,                             -- 会计科目 ID（finance_account_subject.id）
    subject_code   VARCHAR(100),                       -- 科目编号（冗余快照，对标列「科目编号」）
    subject_name   VARCHAR(200),                       -- 科目名称（冗余快照，对标列「科目名称」）
    direction      VARCHAR(20),                        -- 借贷方向：DEBIT 借方 / CREDIT 贷方（**仅资产负债期初有值**）
    opening_amount NUMERIC(18,2),                      -- 期初金额（对标列「期初金额」，允许负数）
    deleted        INT DEFAULT 0,                      -- 逻辑删除 0-正常 1-已删除
    create_time    TIMESTAMP,                          -- 创建时间
    update_time    TIMESTAMP,                          -- 更新时间
    create_by      BIGINT,                             -- 创建人
    update_by      BIGINT                              -- 更新人
);
COMMENT ON TABLE erp_initial_finance_subject IS '财务期初（按科目）：银行现金/固定资产/资产负债期初台账，本页为其唯一维护入口';
COMMENT ON COLUMN erp_initial_finance_subject.initial_type IS '期初类型：BANK_CASH 银行现金 / FIXED_ASSET 固定资产 / BALANCE_SHEET 资产负债';
COMMENT ON COLUMN erp_initial_finance_subject.period_year IS '期初年度（2000-2099）';
COMMENT ON COLUMN erp_initial_finance_subject.subject_id IS '会计科目ID（finance_account_subject.id）';
COMMENT ON COLUMN erp_initial_finance_subject.subject_code IS '科目编号（快照，对标列「科目编号」）';
COMMENT ON COLUMN erp_initial_finance_subject.subject_name IS '科目名称（快照，对标列「科目名称」）';
COMMENT ON COLUMN erp_initial_finance_subject.direction IS '借贷方向 DEBIT 借方 / CREDIT 贷方（仅资产负债期初有值）';
COMMENT ON COLUMN erp_initial_finance_subject.opening_amount IS '期初金额（允许负数）';

-- 防重录：同租户 + 同类型 + 同年度 + 同科目 只能有一条有效期初
-- （对标「保存期初」是整体保存语义，隐含唯一；服务层先查重返回 400，本索引为最后一道防线）
CREATE UNIQUE INDEX IF NOT EXISTS uk_initial_finance_subject
    ON erp_initial_finance_subject (tenant_id, initial_type, period_year, subject_id)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_initial_finance_subject_page
    ON erp_initial_finance_subject (tenant_id, initial_type, period_year);

-- ── 2. 按往来单位的财务期初（应付 / 应收 二选一） ──
CREATE TABLE IF NOT EXISTS erp_initial_finance_partner (
    id                 BIGINT PRIMARY KEY,             -- 雪花 ID（应用侧生成，无需序列）
    tenant_id          BIGINT,                         -- 租户
    initial_type       VARCHAR(30),                    -- 期初类型：PAYABLE 应付 / RECEIVABLE 应收
    period_year        INT,                            -- 期初年度
    partner_id         BIGINT,                         -- 往来单位 ID（往来单位档案 biz_party.id，即 /erp/md/customer/* 的数据源）
    partner_code       VARCHAR(100),                   -- 供应商编号 / 客户编号（对标列）
    partner_name       VARCHAR(200),                   -- 供应商名称 / 客户名称（对标列）
    default_handler    VARCHAR(100),                   -- 默认经手人（**仅应收期初有值**，应付 Tab 无此列）
    payable_amount     NUMERIC(18,2),                  -- 应付金额（应付 Tab）
    prepay_amount      NUMERIC(18,2),                  -- 预付金额（应付 Tab）
    receivable_amount  NUMERIC(18,2),                  -- 应收金额（应收 Tab）
    advance_amount     NUMERIC(18,2),                  -- 预收金额（应收 Tab）
    deleted            INT DEFAULT 0,                  -- 逻辑删除
    create_time        TIMESTAMP,
    update_time        TIMESTAMP,
    create_by          BIGINT,
    update_by          BIGINT
);
COMMENT ON TABLE erp_initial_finance_partner IS '财务期初（按往来单位）：应付/应收期初台账，本页为其唯一维护入口';
COMMENT ON COLUMN erp_initial_finance_partner.initial_type IS '期初类型：PAYABLE 应付 / RECEIVABLE 应收';
COMMENT ON COLUMN erp_initial_finance_partner.partner_id IS '往来单位ID（往来单位档案 biz_party.id）';
COMMENT ON COLUMN erp_initial_finance_partner.partner_code IS '供应商编号/客户编号（快照）';
COMMENT ON COLUMN erp_initial_finance_partner.partner_name IS '供应商名称/客户名称（快照）';
COMMENT ON COLUMN erp_initial_finance_partner.default_handler IS '默认经手人（仅应收期初有值）';
COMMENT ON COLUMN erp_initial_finance_partner.payable_amount IS '应付金额（应付 Tab）';
COMMENT ON COLUMN erp_initial_finance_partner.prepay_amount IS '预付金额（应付 Tab）';
COMMENT ON COLUMN erp_initial_finance_partner.receivable_amount IS '应收金额（应收 Tab）';
COMMENT ON COLUMN erp_initial_finance_partner.advance_amount IS '预收金额（应收 Tab）';

CREATE UNIQUE INDEX IF NOT EXISTS uk_initial_finance_partner
    ON erp_initial_finance_partner (tenant_id, initial_type, period_year, partner_id)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_initial_finance_partner_page
    ON erp_initial_finance_partner (tenant_id, initial_type, period_year);

-- ── 3. 权限码种子（本页自有 4 个 + 科目下拉依赖的 finance:subject:view） ──
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
SELECT v.id, 1, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
    (91341::BIGINT, '财务期初查询', 'set:initial-finance:view',   '/api/erp/finance/initial/*',       'GET',    841),
    (91342::BIGINT, '财务期初录入', 'set:initial-finance:create', '/api/erp/finance/initial/*/save',  'POST',   842),
    (91343::BIGINT, '财务期初编辑', 'set:initial-finance:update', '/api/erp/finance/initial/*/update','PUT',    843),
    (91344::BIGINT, '财务期初删除', 'set:initial-finance:delete', '/api/erp/finance/initial/*/*',     'DELETE', 844),
    -- 本页「科目」下拉的数据源端点（AccountSubjectController GET /api/erp/finance/subject/list）
    -- 要求该码；实测 devdb 缺 → 不补则非超管账号科目下拉恒 403（开发文档 §5.5 / §12-P1 第 8 条）
    (91345::BIGINT, '会计科目查询', 'finance:subject:view',      '/api/erp/finance/subject/*',       'GET',    845)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

-- 授权给超级管理员角色（role_id = 1）；已存在则跳过
-- ⚠️ role_permission 主键**显式列出**，不用「权限id + 偏移」的算式生成：
--    算式极易与既有区间撞号（V11.395.0 已占用 9130701–9130708；V11.396.0 曾因抢 sys_permission
--    的 91301 直接让 Flyway 失败、应用起不来）→ 9xxxxxx 区间必须实测后再写死。
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT v.rp_id, 1, p.id, 1, now()
FROM (VALUES
    (9130741::BIGINT, 91341::BIGINT),
    (9130742::BIGINT, 91342::BIGINT),
    (9130743::BIGINT, 91343::BIGINT),
    (9130744::BIGINT, 91344::BIGINT),
    (9130745::BIGINT, 91345::BIGINT)
) AS v(rp_id, perm_id)
JOIN sys_permission p ON p.id = v.perm_id
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
