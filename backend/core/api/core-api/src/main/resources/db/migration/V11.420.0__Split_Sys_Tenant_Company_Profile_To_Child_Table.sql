-- =============================================================================
-- 「企业信息」（设置 → 系统配置 → 企业信息，菜单 80624 / set:company-info）收口 ②
-- V11.420.0 · 2026-09-18
--
-- 【为什么做这一版】《设置模块/README.md》§10.5 登记的未闭环项：
--   上一轮（V11.396.0）为了让该页 15 个表单项有落点，直接给 `sys_tenant` 加了 15 列，
--   表从 15 列涨到 **30 列** → 违反《开发技术规范》「单表 ≤25 列」「禁止上帝表」两条红线
--   （开发文档 §13.4-1 亦如实登记「未做拆表」）。
--   本迁移把**上一轮新增的这 15 个档案类列**整体搬到 1:1 子表 `sys_tenant_profile`，
--   让 `sys_tenant` 退回 15 列（只留身份/生命周期类列）。
--
-- 【表名与归属】`sys_tenant_profile` —— 开发文档 §8.4 自己给出的候选名之一
--   （原文：「或改为一对一子表 `sys_tenant_profile` / `sys_tenant_taxpayer`」），不新造词根。
--   `sys_tenant_taxpayer` 未采用：ql361 的 Tab②（纳税人信息）与本页 Tab① 是**同一张档案表单、
--   同一个保存按钮、同一条生命周期**（档案不存在则整体不存在），拆成两张 1:1 表只会把
--   「一次保存」变成跨三表的写操作，无任何收益；故 Tab① 10 列 + Tab② 5 列同表存放。
--
-- 【保留在 `sys_tenant` 的 15 列（不动）】迁移前的原有 15 列：
--   id / deleted / create_time / update_time / tenant_name / tenant_code /
--   contact_person / contact_phone / contact_email / address /
--   admin_user_id / level / expire_time / status / remark
--   —— 其中 tenant_name / tenant_code / status / level / expire_time / admin_user_id 是**平台侧
--   「租户管理」**在用（TenantController / TenantRegistrationService / TenantExpiryScheduler），
--   contact_person / contact_phone / contact_email / address 是**迁移前既有**的租户联系方式列
--   （`SysTenant` 原字段，被租户注册、租户管理弹窗等多处引用）→ 一律不搬，避免波及本页以外的功能。
--   ⇒ 迁移后 `sys_tenant` = **15 列**，回到规范内。
--
-- 【迁出的 15 列（本页档案）】与 V11.396.0 一一对应：
--   企业档案（Tab① 企业信息 10 列）：
--     credit_code / company_type / legal_person / registered_capital / industry /
--     company_scale / establish_date / business_scope / contact_person_phone / contact_person_email
--   纳税人信息（Tab② 纳税人信息 5 列）：
--     tax_number / taxpayer_address / taxpayer_phone / bank_name / bank_account
--   另加本次新落地的 LOGO 列 **`logo_url`**（原 §13.4-4「LOGO 未做」，本轮补：
--   通用上传端点 `POST /api/file/upload` 已存在且被 CertUploadList / 商品附件 / 银行账户等多处复用，
--   故 LOGO 上传不再缺能力；列放子表，**不回塞 `sys_tenant`**）。
--   ⇒ 子表 = 15 数据列 + logo_url + 8 审计/隔离列（id / tenant_id / create_time / update_time /
--      create_by / update_by / deleted / version）= **24 列**，同样在 ≤25 列之内。
--
-- 【数据保全（本迁移的硬要求）】：
--   ① 先建表 → ② `INSERT INTO sys_tenant_profile (...) SELECT ... FROM sys_tenant` 逐行搬值
--   （含 tenant_id = sys_tenant.id、deleted / create_time / update_time 原值照搬，不重置时间戳）；
--   ③ 搬完在**同一事务内**断言「每个 sys_tenant 行都能在子表找到对应行」，有缺口就
--   `RAISE EXCEPTION` 主动失败（宁可迁移不跑、应用起不来，也不接受静默丢档案）；
--   ④ 断言通过后才 `DROP COLUMN`。
--
-- 【幂等 / 可重复执行】建表与索引 `IF NOT EXISTS`；`DROP COLUMN IF EXISTS` 对已删列无副作用；
--   数据搬运（②）包在 DO 块里、先判「源列 credit_code 是否还存在」再执行，且带
--   `WHERE NOT EXISTS (... p.tenant_id = t.id)` 守卫 ⇒ **本文件可整体重复执行**：
--   已跑过一次（源列已删）时，② 自动跳过，不会因「引用不存在的列」而报错。
--
-- 【回滚方式（人工，按需执行；本迁移不写 undo 脚本）】
--   反向操作 = 子表列搬回主表 + 删子表。逐步：
--   ------------------------------------------------------------------
--   -- ① 主表补回 15 列（类型与 V11.396.0 完全一致）
--   ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS credit_code          VARCHAR(32);
--   ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS company_type         VARCHAR(32);
--   ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS legal_person         VARCHAR(64);
--   ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS registered_capital   NUMERIC(18,2);
--   ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS industry             VARCHAR(64);
--   ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS company_scale        VARCHAR(32);
--   ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS establish_date       DATE;
--   ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS business_scope       TEXT;
--   ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS contact_person_phone VARCHAR(32);
--   ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS contact_person_email VARCHAR(128);
--   ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS tax_number           VARCHAR(64);
--   ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS taxpayer_address     VARCHAR(255);
--   ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS taxpayer_phone       VARCHAR(32);
--   ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS bank_name            VARCHAR(128);
--   ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS bank_account         VARCHAR(64);
--   -- ② 数据搬回主表（LOGO 无对应列，回滚即丢弃 logo_url 值，请先自行导出）
--   UPDATE sys_tenant t SET
--     credit_code = p.credit_code, company_type = p.company_type, legal_person = p.legal_person,
--     registered_capital = p.registered_capital, industry = p.industry, company_scale = p.company_scale,
--     establish_date = p.establish_date, business_scope = p.business_scope,
--     contact_person_phone = p.contact_person_phone, contact_person_email = p.contact_person_email,
--     tax_number = p.tax_number, taxpayer_address = p.taxpayer_address, taxpayer_phone = p.taxpayer_phone,
--     bank_name = p.bank_name, bank_account = p.bank_account
--   FROM sys_tenant_profile p WHERE p.tenant_id = t.id;
--   -- ③ 删子表
--   DROP TABLE IF EXISTS sys_tenant_profile;
--   -- ④ 代码侧同步回滚：SysTenant 实体加回 15 字段、删除 SysTenantProfile/Mapper/Service、
--   --    并把 TenantController 的 GET/PUT `/current` 改回单表读写。
--   ------------------------------------------------------------------
--   ⚠️ 回滚前先做行数核对（本迁移只搬不改，主表行数不会变）：
--     SELECT (SELECT count(*) FROM sys_tenant) AS a, (SELECT count(*) FROM sys_tenant_profile) AS b;
--
-- 【租户隔离】`sys_tenant_profile` **有 `tenant_id`**（= 档案归属的租户，1:1 指向 sys_tenant.id），
--   属租户数据 → **不**加入 `MyBatisPlusConfig.IGNORE_TENANT_TABLES`，
--   由全局多租户插件注入 `tenant_id = 会话租户`；控制器侧仍显式按会话租户取，
--   两处口径一致（与 sys_tenant 本身在忽略清单中、只能靠会话定位的做法互补）。
-- =============================================================================

-- ── ① 1:1 档案子表 ───────────────────────────────────────────────────────────
-- id 用 `GENERATED BY DEFAULT AS IDENTITY`：本迁移需要为既有租户**定向搬入行**（必须让数据库发号），
-- 而应用侧实体走 `IdType.ASSIGN_ID`（雪花号，显式传 id）—— BY DEFAULT 两者兼容。
-- （参考本库既有做法：V11.3.0 / V11.34.0 给支付相关表补 identity。）
CREATE TABLE IF NOT EXISTS sys_tenant_profile (
    id                    BIGINT GENERATED BY DEFAULT AS IDENTITY,
    tenant_id             BIGINT       NOT NULL,
    -- 企业档案（Tab① 企业信息）
    credit_code           VARCHAR(32),
    company_type          VARCHAR(32),
    legal_person          VARCHAR(64),
    registered_capital    NUMERIC(18, 2),
    industry              VARCHAR(64),
    company_scale         VARCHAR(32),
    establish_date        DATE,
    business_scope        TEXT,
    contact_person_phone  VARCHAR(32),
    contact_person_email  VARCHAR(128),
    -- 纳税人信息（Tab② 纳税人信息）
    tax_number            VARCHAR(64),
    taxpayer_address      VARCHAR(255),
    taxpayer_phone        VARCHAR(32),
    bank_name             VARCHAR(128),
    bank_account          VARCHAR(64),
    -- 企业 LOGO（本轮新增）
    logo_url              VARCHAR(500),
    -- 审计 / 隔离列（与 BaseEntity 一一对应）
    create_time           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    create_by             BIGINT,
    update_by             BIGINT,
    deleted               INTEGER      NOT NULL DEFAULT 0,
    version               INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT pk_sys_tenant_profile PRIMARY KEY (id)
);

COMMENT ON TABLE  sys_tenant_profile                       IS '租户企业档案（1:1 于 sys_tenant；设置 → 系统配置 → 企业信息 80624）';
COMMENT ON COLUMN sys_tenant_profile.tenant_id             IS '租户ID（= sys_tenant.id，1:1；多租户红线，服务端取会话值）';
COMMENT ON COLUMN sys_tenant_profile.credit_code           IS '统一社会信用代码（18 位）';
COMMENT ON COLUMN sys_tenant_profile.company_type          IS '企业类型（有限责任公司/股份有限公司/合伙企业/个体工商户）';
COMMENT ON COLUMN sys_tenant_profile.legal_person          IS '法定代表人';
COMMENT ON COLUMN sys_tenant_profile.registered_capital    IS '注册资本（万元）';
COMMENT ON COLUMN sys_tenant_profile.industry              IS '所属行业';
COMMENT ON COLUMN sys_tenant_profile.company_scale         IS '企业规模';
COMMENT ON COLUMN sys_tenant_profile.establish_date        IS '成立日期';
COMMENT ON COLUMN sys_tenant_profile.business_scope        IS '经营范围';
COMMENT ON COLUMN sys_tenant_profile.contact_person_phone  IS '联系人电话';
COMMENT ON COLUMN sys_tenant_profile.contact_person_email  IS '联系人邮箱';
COMMENT ON COLUMN sys_tenant_profile.tax_number            IS '纳税人识别号（开票资料；沿用全库词根，不用拟名 taxpayer_id）';
COMMENT ON COLUMN sys_tenant_profile.taxpayer_address      IS '开票地址';
COMMENT ON COLUMN sys_tenant_profile.taxpayer_phone        IS '开票电话';
COMMENT ON COLUMN sys_tenant_profile.bank_name             IS '开户行地址';
COMMENT ON COLUMN sys_tenant_profile.bank_account          IS '开户行账号';
COMMENT ON COLUMN sys_tenant_profile.logo_url              IS '企业 LOGO 访问地址（POST /api/file/upload 返回的 url）';

-- 一行一租户（只对未删除行生效；逻辑删除的行不参与唯一性，避免删后无法重建）
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_tenant_profile_tenant
    ON sys_tenant_profile (tenant_id) WHERE deleted = 0;

-- ── ② 数据搬运：sys_tenant 的 15 个档案列 → 子表（含 tenant_id，逐行对应）─────────
-- `deleted` / `create_time` / `update_time` 照搬原值，保证「搬完之后语义与搬之前完全一致」
-- （软删租户的档案行也是 deleted=1，不会因为搬表而"复活"）。
-- 幂等两道守卫：① 源列 `credit_code` 已不存在（本迁移已跑过）→ 整段跳过；
--              ② 子表已有该租户的行 → 不重复插入。
DO $mig$
DECLARE
    v_copied BIGINT;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                    WHERE table_name = 'sys_tenant' AND column_name = 'credit_code') THEN
        RAISE NOTICE 'sys_tenant 档案列已不存在（迁移已执行过），跳过数据搬运';
        RETURN;
    END IF;

    EXECUTE $copy$
        INSERT INTO sys_tenant_profile (
            tenant_id,
            credit_code, company_type, legal_person, registered_capital, industry,
            company_scale, establish_date, business_scope, contact_person_phone, contact_person_email,
            tax_number, taxpayer_address, taxpayer_phone, bank_name, bank_account,
            create_time, update_time, deleted, version)
        SELECT
            t.id,
            t.credit_code, t.company_type, t.legal_person, t.registered_capital, t.industry,
            t.company_scale, t.establish_date, t.business_scope, t.contact_person_phone, t.contact_person_email,
            t.tax_number, t.taxpayer_address, t.taxpayer_phone, t.bank_name, t.bank_account,
            t.create_time, t.update_time, t.deleted, 0
        FROM sys_tenant t
        WHERE NOT EXISTS (SELECT 1 FROM sys_tenant_profile p WHERE p.tenant_id = t.id)
    $copy$;

    GET DIAGNOSTICS v_copied = ROW_COUNT;
    RAISE NOTICE 'sys_tenant_profile 新增 % 行档案（其余租户此前已有行）', v_copied;
END $mig$;

-- ── ③ 删列前的强制性校验：每个租户都必须有子表行，否则主动失败（不静默丢数据）────
DO $$
DECLARE
    v_tenant_cnt  BIGINT;
    v_profile_cnt BIGINT;
    v_missing     BIGINT;
BEGIN
    SELECT count(*) INTO v_tenant_cnt  FROM sys_tenant;
    SELECT count(*) INTO v_profile_cnt FROM sys_tenant_profile;
    SELECT count(*) INTO v_missing
      FROM sys_tenant t
     WHERE NOT EXISTS (SELECT 1 FROM sys_tenant_profile p WHERE p.tenant_id = t.id);

    IF v_missing > 0 THEN
        RAISE EXCEPTION
            '拒绝删除 sys_tenant 档案列：仍有 % 个租户在 sys_tenant_profile 中无对应行（sys_tenant=% 行 / sys_tenant_profile=% 行）',
            v_missing, v_tenant_cnt, v_profile_cnt;
    END IF;

    RAISE NOTICE 'sys_tenant_profile 数据搬运完成：租户 % 行 → 档案 % 行，缺口 0', v_tenant_cnt, v_profile_cnt;
END $$;

-- ── ④ 主表退列（断言通过后才执行）──────────────────────────────────────────────
ALTER TABLE sys_tenant DROP COLUMN IF EXISTS credit_code;
ALTER TABLE sys_tenant DROP COLUMN IF EXISTS company_type;
ALTER TABLE sys_tenant DROP COLUMN IF EXISTS legal_person;
ALTER TABLE sys_tenant DROP COLUMN IF EXISTS registered_capital;
ALTER TABLE sys_tenant DROP COLUMN IF EXISTS industry;
ALTER TABLE sys_tenant DROP COLUMN IF EXISTS company_scale;
ALTER TABLE sys_tenant DROP COLUMN IF EXISTS establish_date;
ALTER TABLE sys_tenant DROP COLUMN IF EXISTS business_scope;
ALTER TABLE sys_tenant DROP COLUMN IF EXISTS contact_person_phone;
ALTER TABLE sys_tenant DROP COLUMN IF EXISTS contact_person_email;
ALTER TABLE sys_tenant DROP COLUMN IF EXISTS tax_number;
ALTER TABLE sys_tenant DROP COLUMN IF EXISTS taxpayer_address;
ALTER TABLE sys_tenant DROP COLUMN IF EXISTS taxpayer_phone;
ALTER TABLE sys_tenant DROP COLUMN IF EXISTS bank_name;
ALTER TABLE sys_tenant DROP COLUMN IF EXISTS bank_account;

-- ── ⑤ 收尾注释：说明拆表后的口径（便于后续维护者理解两份表的职责边界）────────────
COMMENT ON TABLE sys_tenant IS
    '租户表（身份/生命周期类 15 列：租户名、编码、状态、等级、到期、管理员、租户联系方式等，平台侧维护）；'
    '企业档案（企业信息页 80624 的档案字段）见 1:1 子表 sys_tenant_profile';
