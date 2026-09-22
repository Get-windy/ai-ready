-- ══════════════════════════════════════════════════════════════════════════════
-- 协议模块 · 阶段 A 建表（2026-09-22）
--
-- 【设计依据】**唯一依据**是 docs/DOMAIN-MODEL-USER-PARTY-TENANT-v1.md 的
--   §十二「协议模块」（含六条已拍板裁定）与 §3.4.4b / §3.4.4d / §3.4.4d0 /
--   §3.4.4d1 / §3.4.4d2 / §3.4.4d3。本文件不引入任何新裁定，只把已裁定的
--   语义**落到结构上**。
--
-- 【本期只建 4 张表】`agreement_item` / `agreement_quota` 属**阶段 B**（协议下商品、
--   佣金比例、底线价、额度），本期**刻意不建** —— 建了没有代码读它，只会让
--   "到底哪期做完了"变得说不清。
--
-- 【三条不变量落在结构上，不是注释里写一句】
--   ① 已生效版本只读  ⇒ 服务层禁止对 status = 1 的版本写 snapshot_json / 条款；
--                        并发层面再加 uk_agreement_version_active 兜底。
--   ② 双签缺一不可    ⇒ party_a_confirmed_* 与 party_b_confirmed_* **分开存**，
--                        两者都非空才允许置 ACTIVE（服务层硬校验，不是流程约定）。
--   ③ 必填条款没选完不许生效 ⇒ required 在**平台字典**上（agreement_term_option.required），
--                        生效前逐项核对生效版本里是否有该 term_code 的选择。
--
-- 【裁定⑥：主档是系统级，可见性按两端判定】
--   `agreement.tenant_id` **恒为 0**（系统级归属位，与 shop_user / sys_menu 同性质），
--   四张表全部登记进 `MyBatisPlusConfig.IGNORE_TENANT_TABLES`。
--   ⇒ 租户拦截器在这四张表上**不生效**，"谁能看到这份协议"完全由
--     `party_a_tenant_id` / `party_b_tenant_id` 显式判定，条件收敛在
--     `cn.aiedge.agreement.domain.AgreementVisibility`（唯一构造处）。
--
-- 【⚠️ 刻意没有 `default_option` 字段（不许"顺手"加回来）】
--   `agreement_term_option` 只有 required，**没有** default_option —— 平台给默认值
--   等于平台替双方做了责任划分的决定（㉜ / §3.4.4d1）。字典只回答"有哪些选项、
--   各自什么含义"，不回答"应该选哪个"。
--   `tools/verify-agreement.cjs` 会**直接断言该列不存在**，防止后人顺手加回。
--
-- 【status 一律 INTEGER + COMMENT 写明枚举】沿用本仓新建表惯例（见 V11.484.0）。
--
-- 【部分唯一索引带 WHERE deleted = 0】沿用本仓 CRM-BREAK-03 教训：普通 UNIQUE 不含
--   deleted 时**软删的行仍占着键值** ⇒ 字典项删掉再重建、条款改了再改回都会撞唯一约束。
-- ══════════════════════════════════════════════════════════════════════════════

-- ─────────────────────────── 1. 协议主档 ───────────────────────────
CREATE TABLE IF NOT EXISTS agreement (
    id                  BIGINT      PRIMARY KEY,
    -- 系统级归属位，恒为 0（见头部裁定⑥）。"这协议是谁的"看 party_*_tenant_id，不要看本列。
    tenant_id           BIGINT      NOT NULL DEFAULT 0,
    agreement_no        VARCHAR(64) NOT NULL,
    -- 协议类型：PLATFORM_SERVICE 平台↔租户 / DISTRIBUTION 代销 / GOODS_FRAMEWORK 购销框架 / CONSUMER_PROMISE 单方承诺
    agreement_type      VARCHAR(32) NOT NULL,
    title               VARCHAR(255),
    -- 两端对称：主体 + 该主体所属租户（裁定②）。平台协议里甲方 = 平台主体 + 系统租户 1。
    party_a_id          BIGINT,
    party_a_tenant_id   BIGINT,
    -- 消费者单方承诺的乙方是"不特定消费者"，故 party_b_* 可空（§3.4.4d3）
    party_b_id          BIGINT,
    party_b_tenant_id   BIGINT,
    -- 当前生效版本（双方都确认过的那一版）；未生效的草稿协议为空
    current_version_id  BIGINT,
    -- 0=DRAFT 洽谈中 / 1=ACTIVE 生效 / 2=SUSPENDED 暂停 / 3=TERMINATED 终止
    status              INTEGER     NOT NULL DEFAULT 0,
    effective_from      TIMESTAMP,
    effective_to        TIMESTAMP,
    -- 平台终止留痕（㉝ 清退权）：谁、何时、为什么终止的，可举证
    terminated_by       BIGINT,
    terminated_at       TIMESTAMP,
    terminate_reason    VARCHAR(255),
    create_by           BIGINT,
    create_time         TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP,
    deleted             INTEGER     NOT NULL DEFAULT 0
);

COMMENT ON TABLE agreement IS
  '协议主档（一张表 + agreement_type 承载三类协议：平台↔租户 / 租户↔租户 / 租户↔消费者）。'
  'tenant_id 恒为 0（系统级），可见性按两端的 party_a_tenant_id / party_b_tenant_id 显式判定。';
COMMENT ON COLUMN agreement.tenant_id IS
  '系统级归属位，恒为 0。⚠️ 不要用本列判断"协议属于哪个租户"——协议天然跨租户，'
  '可见性只看 party_a_tenant_id / party_b_tenant_id（裁定⑥）。';
COMMENT ON COLUMN agreement.agreement_type IS
  'PLATFORM_SERVICE=平台服务协议 / DISTRIBUTION=代销协议 / GOODS_FRAMEWORK=购销框架协议 / CONSUMER_PROMISE=消费者单方承诺';
COMMENT ON COLUMN agreement.status IS
  '0=DRAFT 洽谈中 / 1=ACTIVE 生效 / 2=SUSPENDED 暂停 / 3=TERMINATED 终止';
COMMENT ON COLUMN agreement.current_version_id IS
  '当前生效版本 ID（= agreement_version 中 status=1 的那一条）。变更谈成之前不变，交易照常按它执行。';
COMMENT ON COLUMN agreement.terminate_reason IS
  '终止原因。与"未约定"区分：终止是双方/平台按约定行使终止权，未约定是不执行任何动作（§3.4.4d1）。';

-- 协议编号全局唯一（号段复用 biz_number_sequence，见本文件第 5 节）
CREATE UNIQUE INDEX IF NOT EXISTS uk_agreement_no
    ON agreement (agreement_no) WHERE deleted = 0;

-- 可见性条件（party_a_tenant_id = ? OR party_b_tenant_id = ?）的两条腿，各建一条索引
CREATE INDEX IF NOT EXISTS idx_agreement_party_a_tenant
    ON agreement (party_a_tenant_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agreement_party_b_tenant
    ON agreement (party_b_tenant_id) WHERE deleted = 0;
-- 列表页按类型 / 状态筛选
CREATE INDEX IF NOT EXISTS idx_agreement_type_status
    ON agreement (agreement_type, status) WHERE deleted = 0;


-- ─────────────────────────── 2. 协议版本（快照，不可变） ───────────────────────────
CREATE TABLE IF NOT EXISTS agreement_version (
    id                    BIGINT      PRIMARY KEY,
    agreement_id          BIGINT      NOT NULL,
    version_no            INTEGER     NOT NULL,
    -- 完整条款快照（条款选项 + 参数 + 期限）。**一旦 ACTIVE 永久不可改**（㉛ / §3.4.4d2 规定 1、3）。
    -- 改协议的唯一合法路径 = 新建一个 DRAFT 版本。
    snapshot_json         JSONB       NOT NULL,
    -- 0=DRAFT 待对方确认 / 1=ACTIVE 生效 / 2=SUPERSEDED 已被新版取代 / 3=REJECTED 被否决
    status                INTEGER     NOT NULL DEFAULT 0,
    -- ↓ 双签：两方确认痕迹**分开存**，两者都非空才允许 ACTIVE（裁定③ / §3.4.4d2 规定 2）
    party_a_confirmed_by  BIGINT,
    party_a_confirmed_at  TIMESTAMP,
    -- 内容哈希（v1 用 SHA-256，对快照正文取哈希）；第三方电子签章**只留列位不接**
    party_a_sign_hash     VARCHAR(128),
    party_b_confirmed_by  BIGINT,
    party_b_confirmed_at  TIMESTAMP,
    party_b_sign_hash     VARCHAR(128),
    effective_from        TIMESTAMP,
    effective_to          TIMESTAMP,
    -- 变更原因（"为什么有这一版"；首次签订为空）
    change_reason         VARCHAR(255),
    create_by             BIGINT,
    create_time           TIMESTAMP,
    update_by             BIGINT,
    update_time           TIMESTAMP,
    deleted               INTEGER     NOT NULL DEFAULT 0
);

COMMENT ON TABLE agreement_version IS
  '协议版本：一行 = 一次"谈定的条款快照"。**只追加不改写** —— 已生效版本不可变，改协议只能新建 DRAFT 版本。';
COMMENT ON COLUMN agreement_version.snapshot_json IS
  '完整条款快照（JSONB，不可变）：条款选项 + 参数 + 期限。一旦本行 status=1（ACTIVE），'
  '服务层禁止再写入本列——这是"不能单方改协议"的技术保证，也是司法举证的材料。';
COMMENT ON COLUMN agreement_version.status IS
  '0=DRAFT 待对方确认 / 1=ACTIVE 生效 / 2=SUPERSEDED 已被新版取代 / 3=REJECTED 被否决';
COMMENT ON COLUMN agreement_version.party_a_sign_hash IS
  '甲方签署时的内容哈希（v1 = SHA-256(快照正文)）。第三方电子签章只留本列位、本期不接，见裁定③。';

-- 同一协议的版本号不重复
CREATE UNIQUE INDEX IF NOT EXISTS uk_agreement_version_no
    ON agreement_version (agreement_id, version_no) WHERE deleted = 0;
-- **一份协议同时只能有一个生效版本**（不变量③的并发兜底）：
-- 生效动作在同一事务里"先把旧 ACTIVE 置 SUPERSEDED、再把新版本置 ACTIVE"，顺序颠倒会撞本索引。
CREATE UNIQUE INDEX IF NOT EXISTS uk_agreement_version_active
    ON agreement_version (agreement_id) WHERE status = 1 AND deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agreement_version_agreement
    ON agreement_version (agreement_id) WHERE deleted = 0;


-- ─────────────────────────── 3. 协议条款（从字典选的选项 + 参数） ───────────────────────────
CREATE TABLE IF NOT EXISTS agreement_term (
    id            BIGINT      PRIMARY KEY,
    agreement_id  BIGINT      NOT NULL,
    -- 条款挂在**版本**上：判"交易发生时生效的是哪一版"就靠它
    version_id    BIGINT      NOT NULL,
    term_code     VARCHAR(32) NOT NULL,
    option_code   VARCHAR(32),
    -- 需要参数的选项（字典 needs_param 非空）必须把参数填上，否则不许生效（§3.4.4d）
    param_value   VARCHAR(64),
    create_time   TIMESTAMP,
    update_time   TIMESTAMP,
    deleted       INTEGER     NOT NULL DEFAULT 0
);

COMMENT ON TABLE agreement_term IS
  '协议条款实例：一行 = 某个版本里对某个条款类别（term_code）选定的选项。'
  '条款可选自平台字典（agreement_term_option），但字典**不提供默认值**——没选就是"未约定"。';
COMMENT ON COLUMN agreement_term.param_value IS
  '选项的附带参数（字典 needs_param 指定的那个），如分账比例。needs_param 非空却没填 ⇒ 不许置生效。';

-- 一个版本里，同一条款类别只能有一个选择
CREATE UNIQUE INDEX IF NOT EXISTS uk_agreement_term_version_code
    ON agreement_term (version_id, term_code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agreement_term_version
    ON agreement_term (version_id) WHERE deleted = 0;


-- ─────────────────────────── 4. 平台条款字典 ───────────────────────────
CREATE TABLE IF NOT EXISTS agreement_term_option (
    id                  BIGINT      PRIMARY KEY,
    term_code           VARCHAR(32) NOT NULL,
    option_code         VARCHAR(32) NOT NULL,
    -- 中文名（双方在下拉里看到的）
    option_label        VARCHAR(64) NOT NULL,
    -- 语义说明：选了它，系统会怎么执行。给双方看的"条款说明书"，也是司法举证要点。
    semantics           TEXT,
    -- 需要附带参数时填参数名（如"分账比例"），否则空
    needs_param         VARCHAR(32),
    -- 是否必填：责任划分类条款一律 true。必填项没选完 ⇒ 协议不许置生效（逼双方签约时说清）。
    required            BOOLEAN     NOT NULL DEFAULT false,
    -- 法务审核（§3.4.4d0 校验点 1）：PENDING 待审 / APPROVED 已通过 / REJECTED 判定违法
    legal_review_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    sort                INTEGER     NOT NULL DEFAULT 0,
    -- 1=启用 / 0=停用（与前端「条款字典维护」页一致）。停用只影响以后新签，历史快照不受影响。
    status              INTEGER     NOT NULL DEFAULT 1,
    create_time         TIMESTAMP,
    update_time         TIMESTAMP,
    deleted             INTEGER     NOT NULL DEFAULT 0
    -- ⚠️⚠️ 本表**刻意没有 default_option 列**（再次强调，别顺手加）：
    --   平台给默认值 = 平台替双方决定责任划分 = 平台干预（㉜ / §3.4.4d1）。
    --   tools/verify-agreement.cjs 会直接断言本列不存在。
);

COMMENT ON TABLE agreement_term_option IS
  '平台条款选项字典：**只定义"有哪些选项、各自什么含义"，不规定"必须选哪个"**。'
  '⚠️ 刻意没有 default_option 列 —— 平台给默认值就是替双方做决定（§3.4.4d / §3.4.4d1）。';
COMMENT ON COLUMN agreement_term_option.option_label IS '选项中文名（双方在协议里看到的文字）';
COMMENT ON COLUMN agreement_term_option.semantics IS
  '含义说明：选择了它，系统会怎么执行（谁承担、怎么扣、什么时候扣）。选的时候就知道系统会怎么执行。';
COMMENT ON COLUMN agreement_term_option.needs_param IS
  '需要附带参数时填参数名（如"分账比例"），否则为空。非空则协议生效前必须把参数填上。';
COMMENT ON COLUMN agreement_term_option.required IS
  '是否必填条款（责任划分类一律 true）。必填项没选完，协议不许置生效（§3.4.4d1）。';
COMMENT ON COLUMN agreement_term_option.legal_review_status IS
  '法务审核：PENDING 待审 / APPROVED 已通过 / REJECTED 判定违法。REJECTED 的选项不许被选中生效'
  '（《消费者权益保护法》第 26 条、《民法典》第 496~498 条，见 §3.4.4d0）。';
COMMENT ON COLUMN agreement_term_option.status IS '1=启用 / 0=停用。停用只影响以后新签的协议，历史协议快照不受影响。';

CREATE UNIQUE INDEX IF NOT EXISTS uk_agreement_term_option
    ON agreement_term_option (term_code, option_code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agreement_term_option_required
    ON agreement_term_option (required, status) WHERE deleted = 0;


-- ─────────────────────────── 5. 号段登记（复用系统唯一号段服务） ───────────────────────────
-- 本仓裁定：**号段全系统只实现一次** ⇒ agreement_no 不自造，复用
-- `cn.aiedge.common.serial.BizNumberGeneratorService`（biz_number_sequence 行锁 + 日期重置）。
-- 该服务要求 bizType 在库里已有配置行，否则抛「未配置编号序列」⇒ 本迁移登记一行。
-- tenant_id 取 1：与 biz_number_sequence 现有 78 行**全部**为 tenant_id=1 的既成口径一致
-- （协议号是全局流水，序号不按租户切分）。
-- seq_date 取 '19700101'（先例：EMP en_US 行）：与"今天"必然不等 ⇒ 首次调用从 0001 起。
INSERT INTO biz_number_sequence (biz_type, locale, tenant_id, prefix, seq_length, max_seq, current_seq, seq_date, update_time)
SELECT 'AGREEMENT', 'zh_CN', 1, 'XY', 4, 999999, 0, '19700101', now()
WHERE NOT EXISTS (SELECT 1 FROM biz_number_sequence WHERE biz_type = 'AGREEMENT' AND locale = 'zh_CN' AND tenant_id = 1);


-- ─────────────────────────── 6. 自检（任一断言不成立则整个迁移回滚） ───────────────────────────
DO $$
DECLARE
    n_tables   int;
    n_snap     int;
    n_default  int;
    n_seq      int;
BEGIN
    -- 6.1 四张表都存在，且 agreement_item / agreement_quota（阶段 B）**不在**本次范围内
    SELECT count(*) INTO n_tables FROM information_schema.tables
     WHERE table_schema = 'public'
       AND table_name IN ('agreement', 'agreement_version', 'agreement_term', 'agreement_term_option');
    IF n_tables <> 4 THEN
        RAISE EXCEPTION '协议四张表应存在 4 张，实际 % 张', n_tables;
    END IF;

    -- 6.2 snapshot_json 必须是 NOT NULL（"平台保留双方协议快照"是硬要求，不能允许空快照出版本）
    SELECT count(*) INTO n_snap FROM information_schema.columns
     WHERE table_name = 'agreement_version' AND column_name = 'snapshot_json' AND is_nullable = 'NO';
    IF n_snap <> 1 THEN
        RAISE EXCEPTION 'agreement_version.snapshot_json 必须是 NOT NULL';
    END IF;

    -- 6.3 字典表**不许**出现 default_option（防止后人"顺手"把平台默认值加回来）
    SELECT count(*) INTO n_default FROM information_schema.columns
     WHERE table_name = 'agreement_term_option'
       AND column_name IN ('default_option', 'default_option_code', 'default_value');
    IF n_default <> 0 THEN
        RAISE EXCEPTION 'agreement_term_option 出现了默认值列（%）—— 平台给默认值等于替双方做决定，见 §3.4.4d1', n_default;
    END IF;

    -- 6.4 号段配置行已就位（否则 createAgreement 会在运行时抛「未配置编号序列」）
    SELECT count(*) INTO n_seq FROM biz_number_sequence WHERE biz_type = 'AGREEMENT';
    IF n_seq < 1 THEN
        RAISE EXCEPTION 'biz_number_sequence 缺少 AGREEMENT 号段配置，协议编号无法生成';
    END IF;

    RAISE NOTICE 'V11.486.0 自检通过：协议四张表 + 号段已就位，字典表无默认值列';
END $$;
