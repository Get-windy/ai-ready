-- ══════════════════════════════════════════════════════════════════════════════
-- 模块目录定稿（6 → 13）+ 建「模块 → 权限码」映射表 + 回填既有租户的开通记录
--
-- 背景（2026-09-21 用户口径，是权威定义）：本仓的授权是**两层**——
--   ① **模块授权（entitlement）**：平台方决定"某个租户有没有这个模块"，
--      它是**开关**，随合同变化，归平台所有；
--   ② **权限（RBAC）**：租户内的系统管理员决定"本租户的某个角色能不能做某件事"。
--   两道门、先授权后鉴权。业界同构：Salesforce / Zoho / Odoo 都是"订阅开关 + 租户内角色"。
--
-- ⚠️ 现状核对（本迁移的前提，别当成已完成）：库里**已有** entitlement 的全套零件 ——
--   `sys_module`(6 条) / `sys_tenant_module`(开通记录) / `sys_tenant_package` / `sys_tenant_quota`
--   + 前端 `views/admin/tenant/module-auth`。**唯一缺的**是"模块与权限码之间的对应关系"：
--   `TenantModuleService.hasModuleAccess()` 目前**零调用方** ⇒ 后端并不按模块拦截。
--   本迁移补的正是这个缺口的前置（映射表），不改变任何现有行为。
--
-- 模块清单（13 个，用户 2026-09-21 定稿；工作台不是一个模块，它是页面，字段随权限变化）：
--   销售 / 采购 / 仓储 / 资料 / 交易 / 客户服务 / 配送 / 人力资源 / 财务 / 营销 / 分析
--   + 设置（租户级） + 系统（平台级）
--
-- 与旧 6 条的差异：
--   · `crm` 改名「**客户服务**」—— 它是一条连续客户维护链：售前（线索/商机/报价/合同/跟进）
--     + 成交后的工单与售后，两阶段同属一个模块。业界同构：Salesforce = Sales + Service Cloud，
--     Zoho = CRM + Desk，Odoo = CRM + Helpdesk。
--   · `warehouse` **语义收窄**：只管库存/出入库/盘点/调拨（`stock:` + `wms:`）；
--     原挂在它下面的商品档案、往来单位（`product:` / `md:` / `party:`）划给新的「**资料**」。
--   · 新增 7 个：资料 / 交易 / 配送 / 人力资源 / 分析 / 设置 / 系统。
--
-- ⚠️ 已知缺口（不假装修好）：**分析模块目前没有任何权限码** —— 实测
--   `permission_code LIKE 'analytics%'` 等查询 **0 行**，`/views/analytics/**` 整片页面
--   只有登录校验（见 E-08 裸端点盘点）。故它的映射先建成空表，等该模块设计码族后再填。
-- ══════════════════════════════════════════════════════════════════════════════

-- ── 1. 修正既有两条的语义（改的是"这个模块管什么"，不是删了重建，避免动到 id/开通记录）──
UPDATE sys_module
SET module_name = '客户服务',
    description = '客户全生命周期：售前（线索/商机/报价/合同/跟进/客户池/外访/营销活动）+ 售后（工单/售后，待建）'
WHERE module_code = 'crm' AND deleted = 0;

UPDATE sys_module
SET description = '库存与出入库：库存查询、出入库、盘点、调拨、串号/批次、WMS（商品档案/往来单位已划归「资料」模块）'
WHERE module_code = 'warehouse' AND deleted = 0;

-- ── 2. 新增 7 个模块（幂等：按 module_code 判重）──
INSERT INTO sys_module (id, module_name, module_code, version, description, status, icon,
                        sort_order, tenant_id, create_time, update_time, deleted)
SELECT v.id, v.name, v.code, '1.0.0', v.descr, 1, v.icon, v.sort, 0, now(), now(), 0
FROM (VALUES
    ( 7::BIGINT, '资料',     'master-data', '基础档案：商品、商品分类/品牌/单位、价格等级、往来单位（客户/供应商）', 'database',  7),
    ( 8::BIGINT, '交易',     'trade',       '网上订货与网络销售：商城商品、商城标签、订货单（与线下的「销售」分开）', 'shopping',  8),
    ( 9::BIGINT, '配送',     'dms',         '配送履约：调度、派单、骑手/车辆、履约核销、结算',                     'car',       9),
    (10::BIGINT, '人力资源', 'hr',          '组织与人事：员工、考勤、请假、招聘、绩效、薪酬',                       'team',     10),
    (11::BIGINT, '分析',     'analytics',   '经营分析：财务分析、往来余额、查费用、各类报表（⚠️ 权限码族待建）',   'bar-chart', 11),
    (12::BIGINT, '设置',     'settings',    '**租户级**设置：企业信息、期初、打印配置、账套重建、工作流、系统任务', 'setting',  12),
    (13::BIGINT, '系统',     'system',      '**平台级**：租户与套餐/配额、模块与权限、组织（部门/岗位）、审计日志、数据源、平台参数', 'cluster', 13)
) AS v(id, name, code, descr, icon, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_module m WHERE m.module_code = v.code AND m.deleted = 0);

-- ── 3. 「模块 → 权限码」映射表 ──
-- 口径：某权限码属于模块 M，当且仅当它匹配 M 的某个 permission_prefix（前缀匹配）。
--      一个码可能匹配多个前缀 ⇒ **取最长前缀**（最具体的赢）；仍冲突则以本表 sort 小者优先。
--      本表只是 entitlement 与码族之间的对应关系，**不参与鉴权判定**（鉴权仍是 @SaCheckPermission）。
CREATE TABLE IF NOT EXISTS sys_module_permission (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT       NOT NULL DEFAULT 0,
    deleted           INTEGER      NOT NULL DEFAULT 0,
    module_code       VARCHAR(64)  NOT NULL,
    permission_prefix VARCHAR(128) NOT NULL,
    sort              INTEGER      NOT NULL DEFAULT 0,
    remark            VARCHAR(255),
    create_time       TIMESTAMP,
    update_time       TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_module_permission_prefix
    ON sys_module_permission (module_code, permission_prefix) WHERE deleted = 0;

COMMENT ON TABLE sys_module_permission IS
    '模块 → 权限码前缀 的映射（entitlement 前置）。判归属取最长前缀。由本迁移维护，不参与鉴权。';
COMMENT ON COLUMN sys_module_permission.permission_prefix IS
    '权限码前缀，含结尾冒号（如 stock: / erp:product:）。匹配规则：permission_code LIKE prefix || ''%''';

INSERT INTO sys_module_permission (tenant_id, module_code, permission_prefix, sort, remark, create_time, update_time)
SELECT 0, v.module_code, v.prefix, v.sort,
       v.remark, now(), now()
FROM (VALUES
    -- 销售（线下的销售出库/收款方向）
    ('sale',           'sale:',                10, '销售订单、出库、退货、价格跟踪'),
    -- 采购
    ('purchase',       'purchase:',            20, '采购订单、入库、退货、换货、询价、价格跟踪'),
    -- 仓储（库存与出入库，语义已收窄）
    ('warehouse',      'stock:',               30, '库存查询/出入库/盘点/调拨/串号批次'),
    ('warehouse',      'wms:',                 31, 'WMS：收货、上架、拣货、发货、移库、PDA'),
    -- 资料（基础档案）
    ('master-data',    'product:',             40, '商品子档案：属性/分类/品牌/单位/等级/条码/授权/关联/规格'),
    ('master-data',    'erp:product:',         41, '商品主档（旧前缀 erp:product:）'),
    ('master-data',    'md:',                  42, '资料域（md）'),
    ('master-data',    'party:',               43, '往来单位：客户/供应商'),
    -- 交易（网上订货 / 网络销售）
    ('trade',          'mall:',                50, '商城商品与标签、订货单'),
    -- 客户服务（售前 + 售后，两阶段同属一个模块）
    ('crm',            'crm:',                 60, '售前：客户/线索/商机/报价/合同/跟进/客户池/外访/营销活动；售后码族 crm:service:* 待建'),
    -- 配送
    ('dms',            'dms:',                 70, '调度、派单、骑手/车辆、履约核销、结算'),
    -- 人力资源
    ('hr',             'hr:',                  80, '员工、考勤、请假、招聘、绩效、薪酬'),
    -- 财务（含费用、发票、预算、固资、收付款、单据）
    ('finance',        'finance:',             90, '应收应付、凭证、报表、对账'),
    ('finance',        'erp:expense:',         91, '费用申请/审批/付款/报销/统计（旧前缀 erp:expense:）'),
    ('finance',        'invoice:',             92, '发票'),
    ('finance',        'budget:',              93, '预算'),
    ('finance',        'fixed-asset:',         94, '固定资产'),
    ('finance',        'payment:',             95, '付款与对冲'),
    ('finance',        'receipt:',             96, '收款'),
    ('finance',        'doc:',                 97, '单据动作（反冲/作废等）'),
    -- 营销
    ('marketing',      'marketing:',          100, '营销活动、优惠券、积分'),
    -- 分析（⚠️ 码族待建，先占位：将来 analytics:* 落地后按前缀落这里）
    ('analytics',      'analytics:',          110, '分析模块码族（尚无码，待建）'),
    -- 设置（租户级）
    ('settings',       'set:',                120, '企业信息、期初、打印配置、账套重建、系统任务'),
    ('settings',       'workflow:',           121, '工作流（V9.6.5 起并入设置）'),
    ('settings',       'print:',              122, '打印动作'),
    -- 系统（平台级）
    ('system',         'system:',             130, '平台参数、用户/角色/权限、模块/菜单/字典、SoD、字段权限'),
    ('system',         'tenant:',             131, '租户'),
    ('system',         'platform:',           132, '平台级配置：邮件/短信/存储/安全策略/套餐/配额'),
    ('system',         'log:',                133, '审计日志'),
    ('system',         'datasource:',         134, '数据源、备份、清理、慢查询'),
    ('system',         'data-scope:',         135, '数据范围'),
    ('system',         'department:',         136, '部门'),
    ('system',         'position:',           137, '岗位'),
    ('system',         'permission-template:',138, '权限模板'),
    ('system',         'role-inheritance:',   139, '角色继承')
) AS v(module_code, prefix, sort, remark)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_module_permission p
    WHERE p.module_code = v.module_code AND p.permission_prefix = v.prefix AND p.deleted = 0
);

-- ── 4. 回填既有租户的开通记录（保证**不改变今天的实际可访问性**）──
-- 现在租户 1（系统租户）与租户 2 各开了 5 个模块；新增的 7 个若不补，将来 entitlement
-- 一旦真的接到请求链上，这两个租户会**一夜之间少掉资料/分析/设置等模块**。故按"现状即全部能力"
-- 先补全，再由平台职员按合同**主动关掉**不该给的 —— 迁移不替业务做减法。
-- ⚠️ 平台级 `system` 模块：本题有产品歧义（见迁移末尾注释），先按"现状不降级"补上，待用户裁定。
INSERT INTO sys_tenant_module (id, tenant_id, module_code, module_name, purchase_type,
                               expire_time, status, create_time, update_time, deleted)
SELECT (9000000000000000000 + t.tenant_id * 100 + m.sort_order)::BIGINT,
       t.tenant_id, m.module_code, m.module_name, 'permanent', NULL, 0, now(), now(), 0
FROM (VALUES (1::BIGINT), (2::BIGINT)) AS t(tenant_id)
CROSS JOIN sys_module m
WHERE m.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_tenant_module tm
      WHERE tm.tenant_id = t.tenant_id AND tm.module_code = m.module_code AND tm.deleted = 0
  );

-- 既有两租户里 `crm` 那条记录的名称也要跟着改（否则页面上仍显示"客户关系"）
UPDATE sys_tenant_module SET module_name = '客户服务' WHERE module_code = 'crm' AND deleted = 0;

-- ── 5. 自检 ──
DO $$
DECLARE
    n_modules    int;
    n_platform   int;
    n_tenant1    int;
    n_tenant2    int;
    n_prefix     int;
    n_orphan     int;
    dup_codes    int;
BEGIN
    -- 5.1 模块目录必须是 13 个
    SELECT count(*) INTO n_modules FROM sys_module WHERE deleted = 0;
    IF n_modules <> 13 THEN
        RAISE EXCEPTION '模块目录应为 13 条，实际 % 条', n_modules;
    END IF;

    -- 5.2 平台级 system 模块必须存在且只有它一条是平台级语义（这里只校验存在性）
    SELECT count(*) INTO n_platform FROM sys_module WHERE deleted = 0 AND module_code = 'system';
    IF n_platform <> 1 THEN
        RAISE EXCEPTION 'system 模块缺失或重复';
    END IF;

    -- 5.3 crm 已改名
    IF EXISTS (SELECT 1 FROM sys_module WHERE deleted = 0 AND module_code = 'crm' AND module_name <> '客户服务') THEN
        RAISE EXCEPTION 'crm 模块名未改为「客户服务」';
    END IF;

    -- 5.4 映射表前缀数（13 模块中分析为空码族，故 37 条前缀对应 12 个有码模块）
    SELECT count(*) INTO n_prefix FROM sys_module_permission WHERE deleted = 0;
    IF n_prefix < 30 THEN
        RAISE EXCEPTION '模块→码 映射前缀过少（%），疑似种子未生效', n_prefix;
    END IF;

    -- 5.5 映射里的 module_code 必须都在 sys_module 里（防拼错模块码）
    SELECT count(*) INTO n_orphan
    FROM sys_module_permission p
    WHERE p.deleted = 0
      AND NOT EXISTS (SELECT 1 FROM sys_module m WHERE m.module_code = p.module_code AND m.deleted = 0);
    IF n_orphan > 0 THEN
        RAISE EXCEPTION '映射表里有 % 条指向不存在的模块码', n_orphan;
    END IF;

    -- 5.6 两个租户的开通记录必须覆盖全部 13 个模块（本迁移的回填口径）
    SELECT count(*) INTO n_tenant1 FROM sys_tenant_module WHERE tenant_id = 1 AND deleted = 0;
    SELECT count(*) INTO n_tenant2 FROM sys_tenant_module WHERE tenant_id = 2 AND deleted = 0;
    IF n_tenant1 < 13 OR n_tenant2 < 13 THEN
        RAISE EXCEPTION '租户开通记录未回填完整：租户1=% 条，租户2=% 条（应 ≥13）', n_tenant1, n_tenant2;
    END IF;

    -- 5.7 同码不得多行（E-02 的老毛病，防复发）
    SELECT count(*) INTO dup_codes
    FROM (SELECT permission_code FROM sys_permission WHERE deleted = 0
          GROUP BY permission_code HAVING count(*) > 1) t;
    IF dup_codes > 0 THEN
        RAISE EXCEPTION '出现 % 组同码多行', dup_codes;
    END IF;
END $$;

-- ══════════════════════════════════════════════════════════════════════════════
-- ⚠️ 留给用户裁定的一个歧义（不阻塞本迁移，但会影响 entitlement 接线的口径）：
--     「系统」模块（平台级）到底该开给谁？
--       · 说法 A：只开给**系统租户**（租户 1）—— 依据用户口述"系统模块的权限由系统超管
--         授权给系统租户所属的用户和部门管理员"。
--       · 说法 B：**每个租户都开** —— 依据用户口述"租户内的部门管理员权限由租户所在
--         系统管理员给与配置"，而租户的系统管理员要配本租户的角色/用户，就得有这个模块。
--     两说法都出自同一段口述，属产品口径问题，**不猜**。本迁移按"现状不降级"（两租户都开）
--     处理，等裁定后再用一个迁移收紧。
-- ══════════════════════════════════════════════════════════════════════════════
