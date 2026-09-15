-- ============================================================================
-- V11.365.0 交易模块金标准重做：店铺设置 →「注册设置」子项按对标实测重建
-- ============================================================================
-- 目标页面：商城设置 → 店铺设置 → 注册设置
--   前端：frontend/apps/pc-admin/src/views/mall/shop-config/index.vue（子项 register）
--   实体：backend/erp/erp-mall/.../b2b/model/ShopConfig.java
--   端点：GET/PUT /api/erp/mall/admin/config（MallAdminController → MallAdminServiceImpl）
--
-- ── 对标实测（2026-09-14 ql361 活体抓取，权威）──────────────────────────────
-- 对标该子项**只有 5 项**，且**没有「注册协议」**：
--   | # | label 原文             | 控件                                   | 必填 | 实测值          | 同行按钮        | 我方后端键                       |
--   | 1 | 允许注册账号           | 复选框                                 | 否   | 已勾选          | 设置注册信息    | EnableJoinApply                  |
--   | 2 | 买家注册默认级别       | 下拉                                   | 是*  | A餐饮客户(464415)| 客户级别设置   | Default2bCustomerDealerTypeId    |
--   | 3 | 买家注册默认分类       | 搜索式选择器（带放大镜，非 Dojo Select）| 是*  | 在线注册客户     | —              | 键名未实测（候选 EnableCustomerRegistersorce，**仅候选、未验证**） |
--   | 4 | 买家账号注册审核       | 下拉                                   | 是*  | 是(1)           | —              | —（取值 否(0)/是(1)）            |
--   | 5 | 新用户注册送优惠券     | 复选框                                 | 否   | 未勾选          | 设置优惠券      | MallRegGiveCoupons（实测值 {"opengive":false,"couponslist":[]}） |
--   其它实测线索：B2BRegisteredContacts（「设置注册信息」的数据结构）；
--   EnableJoinApplyLicense=false / JoinApplyLicensePrompt="上传营业执照"
--   （**营业执照门控未抓实，本迁移不为其建列、不臆造实现**）。
--
-- ── 列来源核对（⚠️ 本仓库曾因「只看第一个 CREATE TABLE IF NOT EXISTS」而写错列名）──
--   · tenant_shop_config **全库仅在一处创建**：V6.4.0__Create_B2B_Mall_Tables.sql:6
--     （CREATE TABLE IF NOT EXISTS），建表即 21 列：
--       id, tenant_id, shop_name, shop_logo, shop_desc, theme_color, banner_ids,
--       template_id, payment_methods, enable_register, enable_auto_audit,
--       min_order_amount, free_shipping_amount, freight_amount, status, deleted,
--       create_by, create_time, update_by, update_time
--   · 此后 V11.361.7（三页金标准列）与 V11.364.0（运费模板形状重建）追加列，
--     二者**均不含本迁移的 6 个列名**，故本次 6 列全部是「新增」，不会重名。
--   · 本迁移**只加列 + 改注释**，不 ALTER 任何既有列的类型、不删列、不清数据。
--
-- ── register_agreement 的处理（重要）────────────────────────────────────────
--   对标实测该子项**没有「注册协议」**，`register_agreement` 属本系统自造口径。
--   本迁移**保留该列（不 DROP、不清空既有数据）**，仅以 COMMENT 标注「对标无此项、
--   已不再作为对标项使用」。理由与 V11.364.0 对 logistics_methods 的处理一致：
--   删列会破坏旧前端 / 已发布 JAR 的 SQL 兼容性（旧代码仍在 SELECT/UPDATE 该列）。
--
-- ── 类型与命名约定 ─────────────────────────────────────────────────────────
--   · 布尔开关一律 INTEGER 0/1（遵循项目「status=1 启用」惯例），**不使用 PG BOOLEAN**；
--     实体侧必须对应 Integer（PG 列 INTEGER 时实体用 Boolean 会 setBoolean 类型不匹配）。
--   · 客户级别 id 用 BIGINT（关联客户级别字典主键，对标实测样本 464415）。
--   · JSON 结构化字段用 TEXT（**不用 jsonb**），实体侧直接映射 String，无需 TypeHandler。
--   · 全部列可空 —— 因 updateConfig 走 updateById（null 忽略），设 NOT NULL 会与
--     「三页共用单行 + 部分提交」机制冲突。
--
-- 幂等性：ADD COLUMN IF NOT EXISTS + COMMENT ON COLUMN，可重复执行。
-- ============================================================================


-- ─────────────────────────────────────────────────────────────────────────────
-- 一、新增「注册设置」对标 5 项对应列（6 列）
-- ─────────────────────────────────────────────────────────────────────────────

-- 1. 允许注册账号（对标 `enable_join_apply` / EnableJoinApply，复选框）
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS enable_join_apply    INTEGER DEFAULT 0;

-- 2. 买家注册默认级别（对标 Default2bCustomerDealerTypeId，关联客户级别字典主键）
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS reg_default_grade_id BIGINT;

-- 3. 买家注册默认分类（对标实测值「在线注册客户」；**选项字典未实测**，先存原值字符串）
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS reg_default_category VARCHAR(100);

-- 4. 买家账号注册审核（对标取值 否(0)/是(1)）
--    ⚠️ 不能写 `DEFAULT 0`：PG 的 ADD COLUMN ... DEFAULT 会把默认值**回填到既有行**，
--       于是「从未配置过」被填成 0＝免审核，与旧列 enable_auto_audit 的原语义**相反**，
--       导致存量租户注册审核行为被静默翻转为「免审核」。
--    处置：① 无 DEFAULT 新增（存量行保持 NULL）；② 按旧列**反向映射**订正存量行，
--       使行为与迁移前完全一致；③ 保持 NULL 让运行时代码在缺值时回落旧列/旧默认（需审核）。
--    反向映射依据（MallAuthServiceImpl 原实现 `if (getEnableAutoAudit() == 1) needAudit = false;`）：
--        enable_auto_audit = 1（自动审核＝免审） → reg_audit_required = 0（否）
--        enable_auto_audit = 0（需人工审核）     → reg_audit_required = 1（是）
--        enable_auto_audit IS NULL / 行不存在    → 1（是，与原代码 needAudit 默认 true 一致）
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS reg_audit_required   INTEGER;

UPDATE tenant_shop_config
   SET reg_audit_required = CASE WHEN enable_auto_audit = 1 THEN 0 ELSE 1 END
 WHERE reg_audit_required IS NULL;

-- 5. 新用户注册送优惠券（复选框）
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS reg_give_coupon      INTEGER DEFAULT 0;

-- 6. 新用户注册送优惠券-优惠券设置（对标 MallRegGiveCoupons 实测值
--    {"opengive":false,"couponslist":[]}； couponslist 元素结构未实测，暂保持空数组）
ALTER TABLE tenant_shop_config ADD COLUMN IF NOT EXISTS reg_give_coupons     TEXT;


-- ─────────────────────────────────────────────────────────────────────────────
-- 二、列注释
-- ─────────────────────────────────────────────────────────────────────────────

COMMENT ON COLUMN tenant_shop_config.enable_join_apply    IS '【注册设置】允许注册账号 1=是 0=否（对标 name=enable_join_apply / EnableJoinApply，复选框）';
COMMENT ON COLUMN tenant_shop_config.reg_default_grade_id IS '【注册设置】买家注册默认级别（必填，关联客户级别字典 id，即 /erp/partner/grades?gradeType=CUSTOMER 的 PartnerGrade.id；对标 Default2bCustomerDealerTypeId）';
COMMENT ON COLUMN tenant_shop_config.reg_default_category IS '【注册设置】买家注册默认分类（必填；⚠️ 对标选项字典未实测，暂存原值字符串，对标实测样本为「在线注册客户」）';
COMMENT ON COLUMN tenant_shop_config.reg_audit_required   IS '【注册设置】买家账号注册审核 1=是(需审核) 0=否(免审核)（对标取值 否(0)/是(1)）。⚠️ 无 DEFAULT：NULL 表示从未配置，运行时代码回落 enable_auto_audit（无则按需审核）。既有行已由本迁移按旧列反向映射订正，勿再改为 DEFAULT 0/1。';
COMMENT ON COLUMN tenant_shop_config.reg_give_coupon      IS '【注册设置】新用户注册送优惠券 1=是 0=否（复选框，对标实测未勾选）';
COMMENT ON COLUMN tenant_shop_config.reg_give_coupons     IS '【注册设置】新用户注册送优惠券-优惠券设置 JSON：{"opengive":false,"couponslist":[]}（对标 MallRegGiveCoupons 实测形状；couponslist 元素结构未实测）';

-- ── 废弃标注（不删列、不清数据）──
COMMENT ON COLUMN tenant_shop_config.register_agreement    IS '⚠️已废弃：对标 ql361「店铺设置 → 注册设置」实测**无此项**，本列系我方自造，Flyway V11.365.0 起已不再作为对标项使用，前端「注册设置」子项不再读写。列与既有数据仅为兼容旧前端/已发布 JAR 的 SQL 兼容性而保留，新代码禁止使用。';

-- ── 历史列注释订正（原文写「对标该子项字段标签缺失」，实测已拿到字段，故订正）──
COMMENT ON COLUMN tenant_shop_config.enable_register       IS '【历史自造口径】是否开放注册 1=是 0=否（V6.4.0 建表既有列）。⚠️ 对标「注册设置」的真实复选框为 enable_join_apply（列 enable_join_apply）；本列非对标项，仅为旧实现/旧前端兼容保留。';
COMMENT ON COLUMN tenant_shop_config.enable_auto_audit     IS '【历史自造口径】注册自动审核 1=是 0=否（V6.4.0 建表既有列）。⚠️ 对标「买家账号注册审核」为**下拉「否(0)/是(1)」**，对应列 reg_audit_required；本列非对标项，仅为旧实现/旧前端兼容保留。';
