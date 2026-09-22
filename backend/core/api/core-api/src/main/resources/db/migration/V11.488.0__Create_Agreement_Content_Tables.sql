-- ══════════════════════════════════════════════════════════════════════════════
-- 协议模块 · 内容层建表 + 字段元数据初始化（2026-09-22）
--
-- 【设计依据】**唯一依据**是 docs/DOMAIN-MODEL-USER-PARTY-TENANT-v1.md 的 §十三
--   「协议模块（二）：成立过程、可执行内核、生命周期与维权边界」，
--   尤其 §13.1（两条腿：字段设定版是可执行内核、文字版只留痕举证）、
--   §13.2（三种消费时机与「字段 → 消费方」表）、
--   §13.3（AgreementRuntime 是唯一读取入口、未约定不给默认值）、
--   §13.9（模板两级 + 平台合规抽查读 + 模板不是默认值）、
--   §13.10（履约方式是集合、可多种并存）、§13.11（新增物件清单）。
--   本文件不引入任何新裁定，只把已裁定的语义落到结构上。
--
-- 【本期做什么】把「契约的内容」做成可执行的东西：
--   ① agreement_setting_def  —— 字段元数据（**必须登记消费方**）
--   ② agreement_setting      —— **字段设定版**（强类型、一版一行一字段）
--   ③ agreement_narrative    —— **文字版**（不执行，只确认 + 哈希留痕）
--   ④ agreement_fulfillment_mode —— 履约方式**集合**（一版多行并存）
--   ⑤ agreement_template(+term/setting/narrative) —— 契约模板（平台级 / 租户级）
--
-- 【本期**刻意不做**】维权 / 争议 / 平台裁决（属阶段 C）：
--   §13.8 明确要求"实现时**不要**为了留口子提前把裁决逻辑写进自动执行链路里"，
--   因此本文件里没有 agreement_dispute / agreement_termination / 执行通道等任何物件。
--   也不做 agreement_invite（唯一送达）/ agreement_signature（签署留痕）—— 那是 A′ 的另一半。
--
-- 【三条不变量落到结构上，不是注释里写一句】
--   ① 已生效版本只读      ⇒ 内容三表都挂在 version_id 上，写入前一律过
--                            AgreementInvariants.assertVersionMutable（服务层硬校验）；
--   ② 未约定 ≠ 约定为 0   ⇒ agreement_setting **没有行**就是"未约定"，
--                            本文件不为任何字段写默认值、也不设 DEFAULT；
--   ③ 每个字段必须有人消费 ⇒ agreement_setting_def.consumer_point 是 **NOT NULL**，
--                            且迁移末尾的 DO $$ 自检在真库上断言"没有一行为空"，
--                            取值必须落在 AgreementRuntime.ConsumerPoint 的 13 个白名单内。
--
-- 【⚠️ 为什么 consumer_point 是硬要求】
--   本仓最贵的历史包袱恰恰是"配置界面能勾、勾了不生效"（游客浏览开关的两个字段零消费、
--   sys_user_data_scope 配了不生效、sys_permission.api_path 只填一半……）。
--   §13.3 因此立规矩：**没有消费方的字段不许进设定版**。落法就是这一列 + 末尾的自检 + 运行时枚举。
--
-- 【⚠️ 强类型怎么落】一表分列存（value_text / value_number / value_bool / value_date），
--   由 value_type 决定读哪一列。为什么不存一坨 JSON：设定版要被订单路由 / 发货 / 库存 /
--   定价 / 结算 / 开票 / 风控**直接消费**，每个消费方各解析一遍 JSON 迟早出现
--   "解析不出来就当成 0" —— 那就把"未约定"和"填错了"混成一件事，㉜ 就守不住了。
--
-- 【系统级归属位】八张新表都是系统级（tenant_id 恒为 0）：
--   协议天然跨租户（裁定⑥），租户拦截器在这批表上**不生效**（Mapper 级 @InterceptorIgnore），
--   可见性由两端的 party_a_tenant_id / party_b_tenant_id 显式判定，
--   条件收敛在 cn.aiedge.agreement.domain.AgreementVisibility（唯一构造处）。
--   ⚠️ 正因如此，服务层插入时必须**显式 set tenant_id = 0**：不写会被填充成会话租户，
--      另一端租户立刻读不到内容（本仓同类事故见 sys_user_data_scope）。
--
-- 【部分唯一索引带 WHERE deleted = 0】沿用本仓 CRM-BREAK-03 教训：普通 UNIQUE 不含 deleted 时
--   软删的行仍占着键值 ⇒ "删了再建"必撞唯一约束。所有唯一索引一律带该条件。
--
-- 【status 一律 INTEGER + COMMENT 写明枚举】沿用本仓新建表惯例（见 V11.486.0）。
-- 【幂等】全部 CREATE ... IF NOT EXISTS + INSERT ... SELECT ... WHERE NOT EXISTS，可重复执行。
-- ══════════════════════════════════════════════════════════════════════════════

-- ─────────────── 1. 字段元数据（平台维护：可以约定哪些字段、被谁消费） ───────────────
CREATE TABLE IF NOT EXISTS agreement_setting_def (
    id              BIGINT       PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL DEFAULT 0,
    setting_key     VARCHAR(64)  NOT NULL,
    label           VARCHAR(64)  NOT NULL,
    -- ENUM / NUMBER / TEXT / BOOL / DATE / DURATION
    value_type      VARCHAR(16)  NOT NULL,
    -- ENUM 的候选值，逗号分隔（如 DROP_SHIP,TRANSIT_STOCK）；非 ENUM 留空
    options         VARCHAR(512),
    required        BOOLEAN      NOT NULL DEFAULT false,
    -- ⚠️ **消费方**（硬要求）：这个字段被哪个下游环节消费，取值见 AgreementRuntime.ConsumerPoint
    consumer_point  VARCHAR(32)  NOT NULL,
    -- 对消费后果的中文说明（"改了这一项会影响什么"），给界面显示
    semantics       VARCHAR(512),
    sort            INTEGER      NOT NULL DEFAULT 0,
    status          INTEGER      NOT NULL DEFAULT 1,
    create_time     TIMESTAMP,
    update_time     TIMESTAMP,
    deleted         INTEGER      NOT NULL DEFAULT 0
    -- ⚠️⚠️ 本表**刻意没有 default_value / default_option 之类的列**（别顺手加）：
    --    平台给默认值 = 平台替双方决定商业条款 = 平台干预（㉜ / §3.4.4d1）。
    --    与 agreement_term_option 没有 default_option 同一条理由。
);

COMMENT ON TABLE agreement_setting_def IS
  '协议字段元数据（平台维护）：协议里可以约定哪些设定项、各是什么类型、**被谁消费**。'
  '⚠️ consumer_point 不是注释而是 NOT NULL 列，且迁移的 DO $$ 自检会断言没有一行为空 —— 没有消费方的字段不许进设定版（§13.3）。';
COMMENT ON COLUMN agreement_setting_def.consumer_point IS
  '消费方（这个字段被谁消费），取值必须是 AgreementRuntime.ConsumerPoint 的枚举名：'
  'ORDER_ROUTING 订单路由 / SHIPMENT_GEN 发货单生成 / STOCK_DEDUCT 库存扣减点 / AVAILABLE_QTY 可售量校验 /'
  'PRICING 定价引擎 / AR_DUE_DATE 应收应付到期日 / SETTLEMENT_SPLIT 结算分账 / INVOICING 发票生成 /'
  'ORDER_RISK 下单风控 / CANCEL_FLOW 取消流程 / RETURN_FLOW 退货流程 / CLAIM_FLOW 理赔流程 / DEPOSIT_FORFEIT 保证金扣罚。';
COMMENT ON COLUMN agreement_setting_def.value_type IS
  'ENUM 枚举单选 / NUMBER 数值 / TEXT 文本 / BOOL 是或否 / DATE 日期 / DURATION 时长（以天为单位存 value_number）';
COMMENT ON COLUMN agreement_setting_def.required IS
  '是否必填：责任划分类一律 true。未约定必填项时，保存回执与就绪查询会明确列出（界面据此提示还差什么）。';

CREATE UNIQUE INDEX IF NOT EXISTS uk_agreement_setting_def_key
    ON agreement_setting_def (setting_key) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agreement_setting_def_consumer
    ON agreement_setting_def (consumer_point, status) WHERE deleted = 0;


-- ─────────────── 2. 字段设定版（**可执行内核**：这一版约定了什么） ───────────────
CREATE TABLE IF NOT EXISTS agreement_setting (
    id            BIGINT      PRIMARY KEY,
    tenant_id     BIGINT      NOT NULL DEFAULT 0,
    agreement_id  BIGINT      NOT NULL,
    -- 设定挂在**版本**上：判"交易发生时生效的是哪一版"靠它（㉛：下单时刻生效的那一版）
    version_id    BIGINT      NOT NULL,
    setting_key   VARCHAR(64) NOT NULL,
    -- 本行的取值类型（冗余存，便于取证与解析；元数据日后被改也能读懂这一行）
    value_type    VARCHAR(16) NOT NULL,
    value_text    VARCHAR(1024),
    -- 数值 / 比例 / 天数（DURATION 也存这里，单位为天）
    value_number  NUMERIC(18,6),
    value_bool    BOOLEAN,
    value_date    TIMESTAMP,
    remark        VARCHAR(255),
    create_time   TIMESTAMP,
    update_time   TIMESTAMP,
    deleted       INTEGER     NOT NULL DEFAULT 0
    -- ⚠️ 没有任何 DEFAULT：某字段**没有行** = 未约定；有行且值为 0 = 已约定为 0。
    --    两者绝不允许混同（㉜：未约定就不自动执行、挂人工，不给平台默认值）。
);

COMMENT ON TABLE agreement_setting IS
  '协议**字段设定版**：一行 = 某一版对某个字段约定的取值。它是**运行时配置**、不是文档 —— '
  '被订单路由 / 发货单生成 / 库存扣减点 / 可售量校验 / 定价引擎 / 应收应付到期日 / 结算分账 / 发票生成 / 下单风控'
  '/ 取消退货理赔流程 / 保证金扣罚直接消费（§13.1 / §13.2）。'
  '⚠️ 某字段没有行 = 未约定，下游必须拦下并报错，不许回落默认值。';
COMMENT ON COLUMN agreement_setting.value_type IS
  '本行的取值类型（与元数据同口径的冗余列）：决定读 value_text / value_number / value_bool / value_date 中的哪一列。';
COMMENT ON COLUMN agreement_setting.value_number IS '数值 / 比例 / 天数（value_type = DURATION 时为天数）。';
COMMENT ON COLUMN agreement_setting.value_date IS '日期型取值（含时刻）。';
COMMENT ON COLUMN agreement_setting.remark IS '约定时的备注（为什么这么定），人读，不参与任何自动执行。';

-- 一个版本里，同一个字段只能有一行
CREATE UNIQUE INDEX IF NOT EXISTS uk_agreement_setting_version_key
    ON agreement_setting (version_id, setting_key) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agreement_setting_version
    ON agreement_setting (version_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agreement_setting_key
    ON agreement_setting (setting_key) WHERE deleted = 0;


-- ─────────────── 3. 文字版（**只留痕举证**，系统永不自动执行） ───────────────
CREATE TABLE IF NOT EXISTS agreement_narrative (
    id                    BIGINT      PRIMARY KEY,
    tenant_id             BIGINT      NOT NULL DEFAULT 0,
    agreement_id          BIGINT      NOT NULL,
    version_id            BIGINT      NOT NULL,
    -- DISPUTE 争议解决与管辖 / CONFIDENTIALITY 保密 / FORCE_MAJEURE 不可抗力
    -- / SPECIAL_TERMS 特别约定 / BREACH_LIABILITY_TEXT 违约责任文字表述
    section_code          VARCHAR(32) NOT NULL,
    section_title         VARCHAR(64),
    content_text          TEXT,
    -- 正文哈希（SHA-256）：证明"双方确认之后这段文字没被改过"
    content_hash          VARCHAR(128),
    party_a_confirmed_by  BIGINT,
    party_a_confirmed_at  TIMESTAMP,
    party_b_confirmed_by  BIGINT,
    party_b_confirmed_at  TIMESTAMP,
    sort                  INTEGER     NOT NULL DEFAULT 0,
    create_time           TIMESTAMP,
    update_time           TIMESTAMP,
    deleted               INTEGER     NOT NULL DEFAULT 0
);

COMMENT ON TABLE agreement_narrative IS
  '协议**文字版**：争议解决与管辖 / 保密 / 不可抗力 / 特别约定 / 违约责任的具体文字表述。'
  '⚠️ **没有消费方、永不自动执行**（§13.2 第三行）：不解析，只做双方确认 + 哈希留痕，供举证。'
  '接口必须显式告知用户"此类条款系统不会自动执行，需人工处理"，否则用户会以为写了系统就会照做。';
COMMENT ON COLUMN agreement_narrative.content_hash IS
  '正文哈希（SHA-256）：双方确认的是这一段文字，确认之后若正文被改，哈希对不上即可发现。';
COMMENT ON COLUMN agreement_narrative.section_code IS
  'DISPUTE 争议解决与管辖 / CONFIDENTIALITY 保密 / FORCE_MAJEURE 不可抗力 / SPECIAL_TERMS 特别约定 / BREACH_LIABILITY_TEXT 违约责任（文字表述）';

CREATE UNIQUE INDEX IF NOT EXISTS uk_agreement_narrative_version_section
    ON agreement_narrative (version_id, section_code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agreement_narrative_version
    ON agreement_narrative (version_id) WHERE deleted = 0;


-- ─────────────── 4. 履约方式**集合**（一版多行并存） ───────────────
CREATE TABLE IF NOT EXISTS agreement_fulfillment_mode (
    id            BIGINT      PRIMARY KEY,
    tenant_id     BIGINT      NOT NULL DEFAULT 0,
    agreement_id  BIGINT      NOT NULL,
    version_id    BIGINT      NOT NULL,
    -- DROP_SHIP 直发 / TRANSIT_STOCK 中转 / PICKUP 自提 / LOCAL_STOCK 自有库存
    mode          VARCHAR(32) NOT NULL,
    -- 适用范围说明（如"仅限江浙沪"），人读，不参与自动执行
    scope_note    VARCHAR(255),
    sort          INTEGER     NOT NULL DEFAULT 0,
    create_time   TIMESTAMP,
    update_time   TIMESTAMP,
    deleted       INTEGER     NOT NULL DEFAULT 0
);

COMMENT ON TABLE agreement_fulfillment_mode IS
  '协议约定的**履约方式集合**：用户明确要求"同城直发 + 异地中转并存才是真实交易"（§13.10），'
  '因此一版**多行并存** —— 唯一索引是 (version_id, mode)，不是 (version_id)。'
  '订单路由问的第一个问题就是"这份协议允许哪些履约方式"；空集 = 未约定，不等于"随便用哪种"。';
COMMENT ON COLUMN agreement_fulfillment_mode.mode IS
  'DROP_SHIP 直发（供货方直接发终端客户）/ TRANSIT_STOCK 中转（经中间方仓再发）/ PICKUP 自提 / LOCAL_STOCK 自有库存';

CREATE UNIQUE INDEX IF NOT EXISTS uk_agreement_fulfillment_mode_version_mode
    ON agreement_fulfillment_mode (version_id, mode) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agreement_fulfillment_mode_version
    ON agreement_fulfillment_mode (version_id) WHERE deleted = 0;


-- ─────────────── 5. 契约模板主档（平台级 / 租户级） ───────────────
CREATE TABLE IF NOT EXISTS agreement_template (
    id                    BIGINT       PRIMARY KEY,
    -- PLATFORM 级恒为 0；TENANT 级记本租户（两级混在一张表，故不能靠租户拦截器自动过滤）
    tenant_id             BIGINT       NOT NULL DEFAULT 0,
    -- PLATFORM 平台模板（全员可选）/ TENANT 租户模板（本租户内可选）
    scope                 VARCHAR(16)  NOT NULL,
    template_name         VARCHAR(128) NOT NULL,
    agreement_type        VARCHAR(32)  NOT NULL,
    description           VARCHAR(512),
    -- 法务审核：PENDING 待审 / APPROVED 已通过 / REJECTED 判定违法（REJECTED 不许用于发起）
    legal_review_status   VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    status                INTEGER      NOT NULL DEFAULT 1,
    create_by             BIGINT,
    create_time           TIMESTAMP,
    update_by             BIGINT,
    update_time           TIMESTAMP,
    deleted               INTEGER      NOT NULL DEFAULT 0
);

COMMENT ON TABLE agreement_template IS
  '契约模板（§13.9）：平台模板全员可选、租户模板本租户内可选。'
  '⚠️ **模板不是默认值**：模板项不写入 agreement_setting 的"已约定"状态，只作发起时预填；'
  'AgreementRuntime 绝不读模板。"模板里有 ⇒ 视为已约定"是绝不允许的（那等于平台替双方定商业条款，㉜）。'
  '读口径：**平台可读全部（含租户模板）—— 合规抽查读权限，目的是避免非法交易**；租户只读平台模板 + 自己的。';
COMMENT ON COLUMN agreement_template.tenant_id IS
  'PLATFORM 级恒为 0；TENANT 级记归属租户。⚠️ 这是"数据归属"不是"会话过滤条件"，故本表不参与租户拦截器。';
COMMENT ON COLUMN agreement_template.scope IS 'PLATFORM 平台模板（tenant_id=0）/ TENANT 租户模板（tenant_id=归属租户）';
COMMENT ON COLUMN agreement_template.legal_review_status IS
  'PENDING 待审 / APPROVED 已通过 / REJECTED 判定违法。模板同样过合法审核（㊲），违法条款无效，不许用于发起。';

CREATE UNIQUE INDEX IF NOT EXISTS uk_agreement_template_scope_tenant_name
    ON agreement_template (scope, tenant_id, template_name) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agreement_template_tenant
    ON agreement_template (tenant_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agreement_template_scope_type
    ON agreement_template (scope, agreement_type) WHERE deleted = 0;


-- ─────────────── 6. 模板预填：条款 / 设定 / 文字（三张子表） ───────────────
CREATE TABLE IF NOT EXISTS agreement_template_term (
    id           BIGINT      PRIMARY KEY,
    tenant_id    BIGINT      NOT NULL DEFAULT 0,
    template_id  BIGINT      NOT NULL,
    term_code    VARCHAR(32) NOT NULL,
    option_code  VARCHAR(32),
    param_value  VARCHAR(64),
    sort         INTEGER     NOT NULL DEFAULT 0,
    create_time  TIMESTAMP,
    update_time  TIMESTAMP,
    deleted      INTEGER     NOT NULL DEFAULT 0
);
COMMENT ON TABLE agreement_template_term IS
  '模板预填的**条款**：发起时先替双方勾好，仍需双方在那一版协议上显式确认才算约定。';

CREATE TABLE IF NOT EXISTS agreement_template_setting (
    id            BIGINT      PRIMARY KEY,
    tenant_id     BIGINT      NOT NULL DEFAULT 0,
    template_id   BIGINT      NOT NULL,
    setting_key   VARCHAR(64) NOT NULL,
    value_type    VARCHAR(16) NOT NULL,
    value_text    VARCHAR(1024),
    value_number  NUMERIC(18,6),
    value_bool    BOOLEAN,
    value_date    TIMESTAMP,
    sort          INTEGER     NOT NULL DEFAULT 0,
    create_time   TIMESTAMP,
    update_time   TIMESTAMP,
    deleted       INTEGER     NOT NULL DEFAULT 0
);
COMMENT ON TABLE agreement_template_setting IS
  '模板预填的**设定项**。⚠️ 字段与 agreement_setting 几乎一样但语义完全不同：'
  '这里是"模板建议这么填"，**不是**"这一版已经约定了"。AgreementRuntime 只读 agreement_setting，绝不读本表（㉜ 的防线）。';

CREATE TABLE IF NOT EXISTS agreement_template_narrative (
    id             BIGINT      PRIMARY KEY,
    tenant_id      BIGINT      NOT NULL DEFAULT 0,
    template_id    BIGINT      NOT NULL,
    section_code   VARCHAR(32) NOT NULL,
    section_title  VARCHAR(64),
    content_text   TEXT,
    sort           INTEGER     NOT NULL DEFAULT 0,
    create_time    TIMESTAMP,
    update_time    TIMESTAMP,
    deleted        INTEGER     NOT NULL DEFAULT 0
);
COMMENT ON TABLE agreement_template_narrative IS
  '模板预填的**文字条款**：只有被写进正式协议版本、双方确认之后才有意义（文字条款本就不自动执行）。';

CREATE UNIQUE INDEX IF NOT EXISTS uk_agreement_template_term
    ON agreement_template_term (template_id, term_code) WHERE deleted = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_agreement_template_setting
    ON agreement_template_setting (template_id, setting_key) WHERE deleted = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_agreement_template_narrative
    ON agreement_template_narrative (template_id, section_code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agreement_template_term_tid
    ON agreement_template_term (template_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agreement_template_setting_tid
    ON agreement_template_setting (template_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agreement_template_narrative_tid
    ON agreement_template_narrative (template_id) WHERE deleted = 0;


-- ─────────────── 7. 字段元数据初始化（**覆盖 §13.2 的字段表，一行不缺、消费方一行不空**） ───────────────
-- 说明：这里初始化的是"**协议里可以约定哪些字段**"，不是"协议约定了什么"。
-- 每行的 consumer_point 就是 §13.2「字段 → 消费方」表的落地形式；
-- required = true 的项是责任划分/执行前提类，未约定时界面与回执会明确列出。
INSERT INTO agreement_setting_def
    (id, tenant_id, setting_key, label, value_type, options, required, consumer_point, semantics, sort, status, create_time, update_time, deleted)
SELECT v.id, 0, v.setting_key, v.label, v.value_type, v.options, v.required, v.consumer_point, v.semantics, v.sort, 1, now(), now(), 0
FROM (VALUES
 -- ── 运营设定（下单 / 发货 / 结算**每次**被消费，§13.2 第一行）──
 (111201, 'FULFILLMENT_MODES',      '履约方式集合',        'ENUM',     'DROP_SHIP,TRANSIT_STOCK,PICKUP,LOCAL_STOCK', true,  'ORDER_ROUTING',    '订单按本协议允许的履约方式路由。⚠️ 可多种并存（同城直发 + 异地中转同时存在才是真实交易），值以协议履约方式表为准；空 = 未约定。', 10),
 (111202, 'STOCK_MODE',             '库存模式',            'ENUM',     'OWN_STOCK,ALLOCATED,DROPSHIP_NO_STOCK',      true,  'STOCK_DEDUCT',     '决定下单扣谁的库存：自有库存 / 分配额度（占用供货方额度）/ 直发不占本仓库存。', 20),
 (111203, 'STOCK_QUOTA',            '库存额度',            'NUMBER',   NULL,                                          false, 'AVAILABLE_QTY',    '库存模式为「分配额度」时的可售上限；未约定则不做额度校验（而不是按 0 拦单）。', 30),
 (111204, 'AUTHORIZED_REGION',      '授权区域',            'TEXT',     NULL,                                          false, 'ORDER_RISK',       '允许在哪里做这门生意（如「江浙沪」）。超出区域的订单应被下单风控拦下。', 40),
 (111205, 'SETTLEMENT_CYCLE',       '结算周期',            'DURATION', NULL,                                          false, 'SETTLEMENT_SPLIT', '每隔多少天结一次账（单位：天）。未约定则结算分账不应自动跑，需人工设定。', 50),
 (111206, 'AR_CREDIT_DAYS',         '账期天数',            'NUMBER',   NULL,                                          false, 'AR_DUE_DATE',      '赊销多少天到期（方向另见「账期方向」）。未约定时**无法生成应收到期日**，这是最典型的一条"未约定就不自动执行"。', 60),
 (111207, 'AR_CREDIT_PROVIDER',     '账期方向（谁给谁）',  'ENUM',     'PARTY_A,PARTY_B',                             false, 'AR_DUE_DATE',      '账期是**有向**的：甲方给乙方账期，还是乙方给甲方账期。与「账期天数」配套使用。', 70),
 (111208, 'COMMISSION_RATE',        '佣金率',              'NUMBER',   NULL,                                          false, 'SETTLEMENT_SPLIT', '代销/分销时中间方抽多少（比例）。未约定则不分账，挂人工。', 80),
 (111209, 'SELLER_PRICE',           '售价',                'NUMBER',   NULL,                                          false, 'PRICING',          '本协议口径下的销售价。未约定则定价引擎不采用本协议的价，走其它价源。', 90),
 (111210, 'PRICE_FLOOR',            '底线价',              'NUMBER',   NULL,                                          false, 'PRICING',          '最低可售价，低于它不许成交。', 100),
 (111211, 'COMMISSION_BEARER',      '抽成承担方',          'ENUM',     'BUYER,SELLER,SPLIT',                          false, 'SETTLEMENT_SPLIT', '抽成由谁承担：买方加价 / 卖方让利 / 双方分摊。', 110),
 (111212, 'INVOICE_ISSUER',         '开票方',              'ENUM',     'PARTY_A,PARTY_B,PLATFORM',                    false, 'INVOICING',        '由谁给最终客户开票。未约定则发票生成必须先人工确认。', 120),
 (111213, 'CREDIT_LIMIT',           '信用额度',            'NUMBER',   NULL,                                          false, 'ORDER_RISK',       '允许的赊销上限；超限应被下单风控拦下。', 130),
 -- ── 后果设定（**出事时**才消费，§13.2 第二行；每一条执行都必须引用"当时生效版本"里的该字段）──
 (111214, 'CANCEL_BEARER',          '取消承担方',          'ENUM',     'BUYER,SELLER,BOTH_AGREE,NO_ONE',              false, 'CANCEL_FLOW',      '下单后取消由谁承担损失。未约定则取消流程**不自动扣款**，挂人工协商。', 140),
 (111215, 'CANCEL_FEE_RATE',        '取消费率',            'NUMBER',   NULL,                                          false, 'CANCEL_FLOW',      '取消费按什么比例计（配合「取消承担方」）。', 150),
 (111216, 'RETURN_PATH',            '退货路径',            'ENUM',     'TO_SUPPLIER,TO_SELLER,DIRECT_TO_END,NO_RETURN', false, 'RETURN_FLOW',      '退货往哪退：退回供货方 / 退回销售方 / 直接退终端 / 不支持退货。', 160),
 (111217, 'RETURN_FREIGHT_BEARER',  '退货运费承担',        'ENUM',     'BUYER,SELLER,BOTH_AGREE',                     false, 'RETURN_FLOW',      '退货运费由谁出。', 170),
 (111218, 'FORWARD_FREIGHT_BEARER', '正向运费承担',        'ENUM',     'BUYER,SELLER,BOTH_AGREE',                     false, 'SHIPMENT_GEN',     '正常发货的运费由谁出（生成发货单时消费）。', 180),
 (111219, 'QUALITY_LIABILITY',      '质量责任归属',        'ENUM',     'SUPPLIER,SELLER,MANUFACTURER,BOTH_AGREE',      false, 'CLAIM_FLOW',       '出了质量问题谁负责，用于理赔定责口径（仍不等同于平台判责，见 §13.8）。', 190),
 (111220, 'PENALTY_RATE',           '违约金比例',          'NUMBER',   NULL,                                          false, 'DEPOSIT_FORFEIT',  '违约时按什么比例计罚（依据**必须**来自双方设定，不得由平台自创规则）。', 200),
 (111221, 'PENALTY_CAP',            '违约金上限',          'NUMBER',   NULL,                                          false, 'DEPOSIT_FORFEIT',  '罚多少封顶。未约定则**不自动扣罚**，挂人工。', 210)
) AS v(id, setting_key, label, value_type, options, required, consumer_point, semantics, sort)
WHERE NOT EXISTS (SELECT 1 FROM agreement_setting_def d WHERE d.setting_key = v.setting_key AND d.deleted = 0);


-- ─────────────── 8. 自检（任一断言不成立则整个迁移回滚，不留半成品） ───────────────
DO $$
DECLARE
    n_tables      int;
    n_def         int;
    n_no_consumer int;
    n_bad_consumer int;
    n_expected    int;
    n_points      int;
    n_composite   int;
    n_scalar_uk   int;
    n_tpl         int;
BEGIN
    -- 8.1 八张新表齐备（少一张就是"只建了一半"，宁可整体回滚）
    SELECT count(*) INTO n_tables FROM information_schema.tables
     WHERE table_schema = 'public' AND table_name IN (
        'agreement_setting_def', 'agreement_setting', 'agreement_narrative', 'agreement_fulfillment_mode',
        'agreement_template', 'agreement_template_term', 'agreement_template_setting', 'agreement_template_narrative');
    IF n_tables <> 8 THEN
        RAISE EXCEPTION '协议内容层应有 8 张表，实际 % 张', n_tables;
    END IF;

    -- 8.2 ⚠️ §13.3 硬要求：**没有消费方的字段不许进设定版** —— 真库上断言，不采信注释
    SELECT count(*) INTO n_no_consumer FROM agreement_setting_def
     WHERE deleted = 0 AND (consumer_point IS NULL OR btrim(consumer_point) = '');
    IF n_no_consumer > 0 THEN
        RAISE EXCEPTION 'agreement_setting_def 有 % 行的 consumer_point 为空 —— 没有消费方的字段不许进设定版（§13.3）', n_no_consumer;
    END IF;

    -- 8.3 消费方必须落在 AgreementRuntime.ConsumerPoint 的 13 个白名单内（否则运行时枚举认不出它）
    SELECT count(*) INTO n_bad_consumer FROM agreement_setting_def
     WHERE deleted = 0 AND consumer_point NOT IN (
        'ORDER_ROUTING','SHIPMENT_GEN','STOCK_DEDUCT','AVAILABLE_QTY','PRICING','AR_DUE_DATE','SETTLEMENT_SPLIT',
        'INVOICING','ORDER_RISK','CANCEL_FLOW','RETURN_FLOW','CLAIM_FLOW','DEPOSIT_FORFEIT');
    IF n_bad_consumer > 0 THEN
        RAISE EXCEPTION 'agreement_setting_def 有 % 行的 consumer_point 不在允许的下游环节清单里', n_bad_consumer;
    END IF;

    -- 8.4 §13.2 的字段表**一行不缺**（21 项：13 运营设定 + 8 后果设定）
    SELECT count(*) INTO n_expected FROM (VALUES
        ('FULFILLMENT_MODES'), ('STOCK_MODE'), ('STOCK_QUOTA'), ('AUTHORIZED_REGION'), ('SETTLEMENT_CYCLE'),
        ('AR_CREDIT_DAYS'), ('AR_CREDIT_PROVIDER'), ('COMMISSION_RATE'), ('SELLER_PRICE'), ('PRICE_FLOOR'),
        ('COMMISSION_BEARER'), ('INVOICE_ISSUER'), ('CREDIT_LIMIT'),
        ('CANCEL_BEARER'), ('CANCEL_FEE_RATE'), ('RETURN_PATH'), ('RETURN_FREIGHT_BEARER'),
        ('FORWARD_FREIGHT_BEARER'), ('QUALITY_LIABILITY'), ('PENALTY_RATE'), ('PENALTY_CAP')
    ) AS e(setting_key)
     WHERE NOT EXISTS (SELECT 1 FROM agreement_setting_def d
                       WHERE d.setting_key = e.setting_key AND d.deleted = 0);
    IF n_expected <> 0 THEN
        RAISE EXCEPTION '§13.2 的字段表有 % 项没有初始化到 agreement_setting_def', n_expected;
    END IF;

    -- 8.5 13 个消费方**每一个**都至少有一个字段在用（防止"白名单里有、实际没人用"的空壳环节）
    SELECT count(*) INTO n_points FROM (
        SELECT p.point FROM (VALUES
            ('ORDER_ROUTING'),('SHIPMENT_GEN'),('STOCK_DEDUCT'),('AVAILABLE_QTY'),('PRICING'),('AR_DUE_DATE'),
            ('SETTLEMENT_SPLIT'),('INVOICING'),('ORDER_RISK'),('CANCEL_FLOW'),('RETURN_FLOW'),('CLAIM_FLOW'),
            ('DEPOSIT_FORFEIT')) AS p(point)
        WHERE NOT EXISTS (SELECT 1 FROM agreement_setting_def d
                          WHERE d.consumer_point = p.point AND d.deleted = 0)) x;
    IF n_points <> 0 THEN
        RAISE EXCEPTION '有 % 个消费方环节没有任何字段与之对应（字段元数据覆盖不全）', n_points;
    END IF;

    -- 8.6 履约方式必须是**集合**：唯一索引必须含 mode 列。
    --     若哪天有人把它改成 (version_id) 唯一，协议就退回成"单选"，用户明确要求的"并存"就没了。
    SELECT count(*) INTO n_composite FROM pg_indexes
     WHERE schemaname = 'public' AND tablename = 'agreement_fulfillment_mode'
       AND indexdef LIKE '%UNIQUE%' AND indexdef LIKE '%(version_id, mode)%';
    IF n_composite <> 1 THEN
        RAISE EXCEPTION 'agreement_fulfillment_mode 缺少 (version_id, mode) 唯一索引 —— 履约方式必须是集合、可多行并存（§13.10）';
    END IF;
    SELECT count(*) INTO n_scalar_uk FROM pg_indexes
     WHERE schemaname = 'public' AND tablename = 'agreement_fulfillment_mode'
       AND indexdef LIKE '%UNIQUE%' AND indexdef LIKE '%(version_id)%' AND indexdef NOT LIKE '%mode%';
    IF n_scalar_uk > 0 THEN
        RAISE EXCEPTION 'agreement_fulfillment_mode 出现了只按 (version_id) 的唯一索引 —— 那等于把履约方式退回成单选';
    END IF;

    -- 8.7 模板两级结构齐备：主档 + 三张预填子表，且 scope 列存在
    SELECT count(*) INTO n_tpl FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'agreement_template' AND column_name = 'scope';
    IF n_tpl <> 1 THEN
        RAISE EXCEPTION 'agreement_template.scope 列缺失（模板必须区分平台级 / 租户级）';
    END IF;

    -- 8.8 ⚠️ 三个"不许存在"的防线，防止后人"顺手"把平台默认值加回来
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND column_name IN ('default_value', 'default_option', 'default_option_code')
                 AND table_name IN ('agreement_setting', 'agreement_setting_def',
                                    'agreement_template_setting', 'agreement_term_option')) THEN
        RAISE EXCEPTION '协议内容层出现了默认值列 —— 平台给默认值等于替双方做决定（㉜ / §3.4.4d1）';
    END IF;

    RAISE NOTICE 'V11.488.0 自检通过：8 张内容层表 + 21 项字段元数据（消费方全部非空且在 13 个白名单内）+ 履约方式集合结构正确';
END $$;
