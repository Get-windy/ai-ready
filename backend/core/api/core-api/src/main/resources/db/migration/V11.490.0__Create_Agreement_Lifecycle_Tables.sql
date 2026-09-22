-- ══════════════════════════════════════════════════════════════════════════════
-- 协议模块 · 成立过程与终止（阶段 A′ 的后半 + 阶段 C 的终止留痕）（2026-09-22）
--
-- 【设计依据】**唯一依据**是 docs/DOMAIN-MODEL-USER-PARTY-TENANT-v1.md 的 §十三：
--   §13.4 契约的成立过程（要约 → 反要约 → 承诺）、
--   §13.5 **唯一送达**（token 绑定五件事）、
--   §13.6 签署 = **自然人代表主体**、
--   §13.7 有效期 / 阶段修改 / 终止（五种来源、**终止 ≠ 免责**）、
--   §13.11 新增物件清单。
--   本文件不引入任何新裁定，只把已裁定的语义落到结构上。
--
-- 【本期做什么】
--   ① agreement_invite      —— **唯一送达**（一次性 token + 目标主体 + 目标租户 + 目标版本 + 时效 + 留痕）
--   ② agreement_signature   —— 签署留痕（谁 · 代表哪个主体 · 在哪个租户 · 凭什么代表 · 签了哪一版 · 何时 · 签章哈希）
--   ③ agreement_termination —— 终止留痕（五种来源 + 依据 + 是否主张违约 + 异议），**不含任何"责任已了结"语义的列**
--   ④ ALTER agreement_version —— 协商时间线（proposed_by_side / proposed_by_person / proposal_note）
--                                + 变更单语义（origin_version_id / no_retroactive_note）
--
-- 【⚠️ 为什么邀请必须**新建**一张表，不能拿现成的凑（§13.5 原话）】
--   实测仓库里没有语义正确的邀请机制：
--     · `mkt_share_record` 是**营销分享**（分享给谁都能看、目的是传播，不是"唯一收到"）；
--     · `erp_signature_record` 是**签名台账**（记的是"有个签名文件"，不含目标主体/时效/一次性）。
--   两者都答不出"这份契约是不是发给你本人的"，拿它们凑等于把"唯一送达"这句话变成空话。
--
-- 【⚠️ token 只存哈希 —— 怎么校验、运维怎么排查】
--   · **怎么校验**：库里的 `token_hash` = SHA-256(明文 token)。对方打开链接时把明文 token 传进来，
--     服务端**现算一次哈希**再与库里的值比对（等值比较，不反解）。明文**不落库**，
--     因此即使整库被拖走，也换不出可用的邀请链接。
--   · **运维怎么排查**：只存哈希就没法"看着明文去搜"。为此同时落两列：
--     `token_hint`（明文**前 8 位**，用于在日志/客诉里对上是哪一条，8 位不足以反推 43 位随机串）
--     与 `invite_code`（人读短码，可口述、可人工输入）。
--     排查路径 = 用 token_hint + 协议编号 + 状态去查，而不是"用明文搜"。
--
-- 【⚠️ 终止 ≠ 免责 —— 这条在结构上被反证，不在注释里写一句】
--   §13.7 用户原话强调：单方终止只记录「**停止履行**」这个**事实**，
--   系统**不自动结清、不自动免责**。
--   因此 `agreement_termination` **不得有任何**"责任已了结/已结清/已免责"语义的列，
--   并且本文件末尾的 DO $$ 在**真库上断言列名集合**（黑名单命中即整体回滚）——
--   后人想"顺手加个 settled_at"会被迁移直接拦下，而不是等出纠纷时才发现平台替双方免了责。
--
-- 【⚠️ 为什么签署记录必须记「代表哪个主体 + 凭什么代表」】
--   §13.6：只记 tenant_id 或账号 ID 是不够的 —— 司法上要回答
--   「**谁签的、凭什么代表这家公司**」。所以本表有四列是不可为空的：
--   `party_id`（代表的主体）、`signer_user_id`（自然人账号）、
--   `signer_party_tenant_id`（在哪家店签的）、`authority_basis`（授权依据）。
--
-- 【系统级归属位】三张新表都是系统级（`tenant_id` 恒为 0）：
--   协议天然跨租户（裁定⑥），三张表一并登记进 `MyBatisPlusConfig.IGNORE_TENANT_TABLES`，
--   租户拦截器在它们上面**不生效**；可见性一律由父协议两端的
--   `party_a_tenant_id` / `party_b_tenant_id` 显式判定，条件收敛在
--   `cn.aiedge.agreement.domain.AgreementVisibility`（唯一构造处）。
--   ⚠️ 正因如此，服务层插入时必须**显式 set tenant_id = 0**，否则会被填成会话租户。
--
-- 【部分唯一索引带 WHERE deleted = 0】沿用本仓 CRM-BREAK-03 教训：普通 UNIQUE 不含 deleted 时
--   软删的行仍占着键值 ⇒ "删了再建"必撞唯一约束。
--
-- 【status 一律 INTEGER + COMMENT 写明枚举】沿用本仓新建表惯例（见 V11.486.0）。
-- 【幂等】全部 CREATE ... IF NOT EXISTS / ADD COLUMN IF NOT EXISTS / INSERT 无种码，可重复执行。
-- ══════════════════════════════════════════════════════════════════════════════

-- ═══════════════════ 1. 唯一送达（§13.5） ═══════════════════
-- token 绑定**五件事**（表结构上逐条可断言）：
--   ① 目标主体  target_party_id     ② 目标租户  target_tenant_id
--   ③ 目标文稿版本  version_id      ④ 时效      expires_at
--   ⑤ 一次性    status/used 语义（领取即置 1=已领取，再次打开直接失效）
CREATE TABLE IF NOT EXISTS agreement_invite (
    id                  BIGINT       PRIMARY KEY,
    -- 系统级归属位，恒为 0（见头部裁定⑥）
    tenant_id           BIGINT       NOT NULL DEFAULT 0,
    agreement_id        BIGINT       NOT NULL,
    -- ③ 目标文稿版本：这份邀请针对哪一版发出（对方打开看到的就是这一版）
    version_id          BIGINT       NOT NULL,
    -- ① 目标主体 + ② 目标租户：token 绑定的是"谁"（转发给别人也打不开）
    target_party_id     BIGINT       NOT NULL,
    target_tenant_id    BIGINT       NOT NULL,
    -- 目标是协议的哪一端：A 甲方 / B 乙方
    target_side         VARCHAR(8)   NOT NULL,
    -- token 只存哈希：SHA-256(明文)，校验时现算再等值比对，明文永不落库
    token_hash          VARCHAR(128) NOT NULL,
    -- 明文前 8 位：仅供运维/客诉在库里对上是哪一条（不足以反推 43 位随机串）
    token_hint          VARCHAR(16)  NOT NULL,
    -- 人读短码：可口述、可人工输入（二维码渲染交给前端，后端不引入二维码依赖）
    invite_code         VARCHAR(32)  NOT NULL,
    -- 渠道：QRCODE 二维码 / LINK 链接（留痕"通过哪个渠道领取或查看"）
    channel             VARCHAR(16)  NOT NULL DEFAULT 'LINK',
    -- 0=待领取 / 1=已领取（一次性） / 2=被发起方撤回 / 3=已过期（三种失效可分别断言）
    status              INTEGER      NOT NULL DEFAULT 0,
    -- ④ 时效：到点即失效（不看 status 也能判，服务层两者都判）
    expires_at          TIMESTAMP    NOT NULL,
    -- 留痕：谁、何时、通过哪个渠道**领取或查看**了它
    view_count          INTEGER      NOT NULL DEFAULT 0,
    first_viewed_by     BIGINT,
    first_viewed_at     TIMESTAMP,
    first_view_channel  VARCHAR(16),
    accepted_by         BIGINT,
    accepted_at         TIMESTAMP,
    accept_channel      VARCHAR(16),
    accepted_party_id   BIGINT,
    -- 撤回留痕（发起方自己的动作）
    revoke_by           BIGINT,
    revoke_at           TIMESTAMP,
    revoke_reason       VARCHAR(255),
    create_by           BIGINT,
    create_time         TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP,
    deleted             INTEGER      NOT NULL DEFAULT 0
);

COMMENT ON TABLE agreement_invite IS
  '**唯一送达**（§13.5）：token 绑定五件事 —— 目标主体 + 目标租户 + 目标文稿版本 + 时效 + 一次性。'
  '打开链接的人必须先登录，且其会话租户与所代表主体都要与目标匹配，否则一律回「这份契约不是发给你的」；'
  '转发给别人打不开，这正是"保证另一方唯一收到"的技术形态。'
  '⚠️ 三种失效（过期 / 被撤回 / 已使用）分别对应 status = 3 / 2 / 1，可分别断言。';
COMMENT ON COLUMN agreement_invite.token_hash IS
  'SHA-256(明文 token)。明文**不落库**：校验时把对方传入的明文现算哈希再与本列等值比对。'
  '同时落 token_hint（明文前 8 位）供运维排查 —— 只存哈希就"没法用明文搜库"，这是对可运维性的取舍。';
COMMENT ON COLUMN agreement_invite.token_hint IS
  '明文 token 的前 8 位，仅供在库里/日志里对上是哪一条邀请；8 位不足以反推完整的随机串。';
COMMENT ON COLUMN agreement_invite.invite_code IS
  '人读短码（可口述、可人工输入）。二维码/短链由后端给出内容、渲染交给前端——本仓没有二维码依赖，不为"生成图片"新引入。';
COMMENT ON COLUMN agreement_invite.status IS
  '0=待领取 / 1=已领取（一次性，用掉即失效） / 2=被发起方撤回 / 3=已过期。三种失效分别可断言，报错文案也各不相同。';
COMMENT ON COLUMN agreement_invite.version_id IS
  '目标文稿版本：token 绑定的第③件事。这一版之外的内容对方看不到，也签不了。';
COMMENT ON COLUMN agreement_invite.accepted_by IS
  '领取人（登录用户 ID）。与 first_viewed_by 区分：看一眼不算领取，领取才把邀请置为已使用。';

CREATE UNIQUE INDEX IF NOT EXISTS uk_agreement_invite_token_hash
    ON agreement_invite (token_hash) WHERE deleted = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_agreement_invite_code
    ON agreement_invite (invite_code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agreement_invite_agreement
    ON agreement_invite (agreement_id) WHERE deleted = 0;
-- 「这份邀请是不是发给你的」的判定腿：目标租户 + 目标主体
CREATE INDEX IF NOT EXISTS idx_agreement_invite_target
    ON agreement_invite (target_tenant_id, target_party_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agreement_invite_version
    ON agreement_invite (version_id) WHERE deleted = 0;


-- ═══════════════════ 2. 签署留痕（§13.6：自然人代表主体） ═══════════════════
CREATE TABLE IF NOT EXISTS agreement_signature (
    id                      BIGINT       PRIMARY KEY,
    tenant_id               BIGINT       NOT NULL DEFAULT 0,
    agreement_id            BIGINT       NOT NULL,
    -- 签了哪一版
    version_id              BIGINT       NOT NULL,
    -- 代表协议的哪一端：A / B
    party_side              VARCHAR(8)   NOT NULL,
    -- ⚠️ 代表的主体（不可空）：司法上回答"代表哪个主体签的"
    party_id                BIGINT       NOT NULL,
    -- ⚠️ 在哪个租户签的（不可空）：主体是系统级、租户只是"哪一端的店"
    signer_party_tenant_id  BIGINT       NOT NULL,
    -- ⚠️ 签署的自然人：登录账号（不可空）+ 自然人主体 ID（R1 通用化属文档 §5.2 阶段 2，本期允许为空但要留痕）
    signer_user_id          BIGINT       NOT NULL,
    signer_person_id        BIGINT,
    signer_name             VARCHAR(64),
    -- ⚠️ 凭什么代表（不可空）：授权依据文本（如"法定代表人"/"授权委托书"）+ 外部凭证号
    authority_basis         VARCHAR(255) NOT NULL,
    authority_evidence_no   VARCHAR(128),
    -- 签章哈希：对该版本的快照正文取 SHA-256（v1 口径，与裁定③一致）
    sign_hash               VARCHAR(128) NOT NULL,
    signed_at               TIMESTAMP    NOT NULL,
    sign_channel            VARCHAR(16),
    -- 签署类型：VERSION 版本签署（本期唯一取值；第三方电子签章只留列位不接，见裁定③）
    signature_type          VARCHAR(16)  NOT NULL DEFAULT 'VERSION',
    remark                  VARCHAR(255),
    create_by               BIGINT,
    create_time             TIMESTAMP,
    update_by               BIGINT,
    update_time             TIMESTAMP,
    deleted                 INTEGER      NOT NULL DEFAULT 0
);

COMMENT ON TABLE agreement_signature IS
  '签署留痕（§13.6）：一条 = **一个自然人代表一个主体**在某个租户对某一版协议签了字。'
  '司法上要回答的是「谁签的、凭什么代表这家公司」，所以 party_id / signer_user_id / '
  'signer_party_tenant_id / authority_basis 四列**都不可为空** —— 只记租户或账号是不够的。'
  '⚠️ 同一版同一方可能有多条（内容被改后必须重新签署），因此**刻意不加** (version_id, party_side) 唯一索引：'
  '每条都是当时的留痕；"当前有效的那一条"由 sign_hash 与当前快照哈希是否一致判定。';
COMMENT ON COLUMN agreement_signature.authority_basis IS
  '凭什么代表这个主体签字（授权依据）：如"法定代表人本人"、"授权委托书（含授权范围与限额）"。'
  '接文档㉟ / G13 的授权范围与限额口径，本期以文本 + 外部凭证号留痕，不引入第二套鉴权机制。';
COMMENT ON COLUMN agreement_signature.signer_person_id IS
  '自然人主体 ID（R1 自然人×往来单位，通用化属文档 §5.2 阶段 2）。'
  '本期允许为空（R1 尚未通用化），但 signer_user_id + signer_name + authority_basis 必须留下，'
  '否则等于只记了"某个账号点了一下"，答不出"谁签的"。';
COMMENT ON COLUMN agreement_signature.sign_hash IS
  '签章哈希 = SHA-256(该版本快照正文)，与裁定③的双签哈希同口径。内容被改则哈希对不上，旧签署自然失效。';

CREATE INDEX IF NOT EXISTS idx_agreement_signature_version
    ON agreement_signature (version_id, party_side) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agreement_signature_agreement
    ON agreement_signature (agreement_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agreement_signature_signer
    ON agreement_signature (signer_user_id) WHERE deleted = 0;


-- ═══════════════════ 3. 终止留痕（§13.7：终止 ≠ 免责） ═══════════════════
-- ⚠️⚠️ 本表**刻意没有任何**"责任已了结 / 已结清 / 已免责"语义的列（见文件头与末尾的列名黑名单断言）。
--   它只回答一件事：**从某一刻起停止履行** —— 谁提的、依据什么、是否主张对方违约、对方有没有异议。
--   "是否违约、赔多少"走 §13.8（平台不裁判），本表不预设结论。
CREATE TABLE IF NOT EXISTS agreement_termination (
    id                        BIGINT       PRIMARY KEY,
    tenant_id                 BIGINT       NOT NULL DEFAULT 0,
    agreement_id              BIGINT       NOT NULL,
    -- 终止发生时正在执行的那一版（"依据哪一版的约定终止"）；草稿协议尚无生效版本时为空
    version_id                BIGINT,
    -- 五种来源（㉝）：MUTUAL_AGREEMENT 协商一致 / NATURAL_EXPIRY 自然到期 / UNILATERAL 单方终止
    --                / COUNTERPARTY_BREACH 因对方违约 / PLATFORM_EXPULSION 平台清退
    source                    VARCHAR(32)  NOT NULL,
    -- 0=待对方确认 / 1=已终止（停止履行已生效） / 2=对方有异议（未终止） / 3=已撤回
    status                    INTEGER      NOT NULL DEFAULT 0,
    -- 谁、何时、代表哪一端发起
    requested_by              BIGINT       NOT NULL,
    requested_side            VARCHAR(8)   NOT NULL,
    requested_at              TIMESTAMP    NOT NULL,
    -- 依据：引用协议条款 / 法定情形（必填 —— "凭什么终止"必须说得出来）
    basis_text                VARCHAR(500) NOT NULL,
    -- 是否**主张**对方违约。⚠️ 只是"当事人主张"，不是"平台认定"（§13.8 平台不裁判）
    claim_counterparty_breach BOOLEAN      NOT NULL DEFAULT false,
    breach_note               VARCHAR(500),
    -- 「停止履行」这个事实的生效时刻（⚠️ 只是事实：不结清、不免责）
    stop_performance_at       TIMESTAMP,
    effective_at              TIMESTAMP,
    -- 对方表态（确认 / 提异议）
    counterparty_action_by    BIGINT,
    counterparty_action_at    TIMESTAMP,
    counterparty_objection    BOOLEAN      NOT NULL DEFAULT false,
    objection_reason          VARCHAR(500),
    -- 撤回留痕（发起方在对方确认前可撤回）
    withdrawn_by              BIGINT,
    withdrawn_at              TIMESTAMP,
    withdraw_reason           VARCHAR(255),
    create_by                 BIGINT,
    create_time               TIMESTAMP,
    update_by                 BIGINT,
    update_time               TIMESTAMP,
    deleted                   INTEGER      NOT NULL DEFAULT 0
);

COMMENT ON TABLE agreement_termination IS
  '终止留痕（§13.7 / ㉝）：五种来源 —— 协商一致 / 自然到期 / 单方终止 / 因对方违约 / 平台清退。'
  '⚠️⚠️ **终止 ≠ 免责**：本表只记录「**停止履行**」这个事实（stop_performance_at），'
  '系统**不自动结清、不自动免责**；是否违约、赔多少走 §13.8（平台不裁判，平台只按双方设定执行程序可执行的部分）。'
  '因此本表**不得有任何**"责任已了结 / 已结清 / 已免责"语义的字段 —— '
  '这条不是注释，是迁移末尾 DO $$ 在真库上对列名集合的断言（命中黑名单即整体回滚）。';
COMMENT ON COLUMN agreement_termination.source IS
  'MUTUAL_AGREEMENT 协商一致 / NATURAL_EXPIRY 自然到期 / UNILATERAL 单方终止 / '
  'COUNTERPARTY_BREACH 因对方违约 / PLATFORM_EXPULSION 平台清退（㉝）。';
COMMENT ON COLUMN agreement_termination.status IS
  '0=待对方确认（协商一致必须双方确认） / 1=已终止（停止履行已生效） / 2=对方有异议（未终止） / 3=已撤回。';
COMMENT ON COLUMN agreement_termination.basis_text IS
  '终止依据：引用协议条款（如"第 N 版第 X 条"）或法定情形。必填 —— 凭什么终止必须说得出来，否则日后无法解释。';
COMMENT ON COLUMN agreement_termination.claim_counterparty_breach IS
  '是否**主张**对方违约。⚠️ 这是当事人的主张，**不是**平台的认定：平台不认定谁违约、不判赔多少（§13.8）。';
COMMENT ON COLUMN agreement_termination.stop_performance_at IS
  '「停止履行」的生效时刻。⚠️ 本表只记这个**事实**：系统不据此自动结清、不自动免责、也不回写任何既有单据。';
COMMENT ON COLUMN agreement_termination.counterparty_objection IS
  '对方是否提异议。已终止的单子上提异议**不回滚终止事实**，只留痕（是否违约另走 §13.8）。';

CREATE INDEX IF NOT EXISTS idx_agreement_termination_agreement
    ON agreement_termination (agreement_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_agreement_termination_status
    ON agreement_termination (status, source) WHERE deleted = 0;


-- ═══════════════════ 4. 协商时间线 + 变更单语义（ALTER agreement_version） ═══════════════════
-- 一串 DRAFT 版本 = 协商时间线（§13.4）：修改 = 反要约 = **新建一个 DRAFT 版本**，
-- 因此"谁提的这一版、提的时候说了什么"必须落在版本行上。
-- 阶段修改（§13.7）= 新版本 + 双方签署，且**不追溯**已发生的单据与结算。
ALTER TABLE agreement_version ADD COLUMN IF NOT EXISTS proposed_by_side    VARCHAR(8);
ALTER TABLE agreement_version ADD COLUMN IF NOT EXISTS proposed_by_person  BIGINT;
ALTER TABLE agreement_version ADD COLUMN IF NOT EXISTS proposal_note       VARCHAR(500);
-- 变更单语义：本版关联的**原版本**（从哪一版改出来的）——"这一版改的是哪一版"可回答
ALTER TABLE agreement_version ADD COLUMN IF NOT EXISTS origin_version_id   BIGINT;
-- 显式声明：本变更不影响已发生单据与结算（不追溯，第三条通用原则）
ALTER TABLE agreement_version ADD COLUMN IF NOT EXISTS no_retroactive_note VARCHAR(255);

COMMENT ON COLUMN agreement_version.proposed_by_side IS
  '这一版是谁提的（要约方）：A / B / NONE。多轮协商时据此画出"谁在第几轮提了什么"。';
COMMENT ON COLUMN agreement_version.proposed_by_person IS
  '提出这一版的操作人（登录用户 ID）。与 proposed_by_side 一起回答"这一版是谁代表哪一方提的"。';
COMMENT ON COLUMN agreement_version.proposal_note IS
  '协商留言（反要约时说明"改了什么、为什么改"）。与 change_reason 的区别：'
  'change_reason 是变更单的正式原因，proposal_note 是多轮协商里的对话留痕。';
COMMENT ON COLUMN agreement_version.origin_version_id IS
  '变更单语义：本版是从**哪一版**改出来的（原版本 ID）。首次签订为空；'
  '"本变更基于哪一版"必须能回答，否则多轮协商后说不清链条。';
COMMENT ON COLUMN agreement_version.no_retroactive_note IS
  '显式声明：本次变更**不追溯**变更前已发生的单据与结算（第三条通用原则）。'
  '写进版本行是因为这条声明要随版本一起被双方看到、一起被举证，而不是只写在流程说明里。';

CREATE INDEX IF NOT EXISTS idx_agreement_version_origin
    ON agreement_version (origin_version_id) WHERE deleted = 0;


-- ═══════════════════ 5. 自检（任一断言不成立则整个迁移回滚，不留半成品） ═══════════════════
DO $$
DECLARE
    n_tables   int;
    n_cols     int;
    n_uk       int;
    n_bad      int;
    v_bad      text;
BEGIN
    -- 5.1 三张新表齐备（少一张就是"只建了一半"，宁可整体回滚）
    SELECT count(*) INTO n_tables FROM information_schema.tables
     WHERE table_schema = 'public'
       AND table_name IN ('agreement_invite', 'agreement_signature', 'agreement_termination');
    IF n_tables <> 3 THEN
        RAISE EXCEPTION '协议成立过程/终止应有 3 张新表，实际 % 张', n_tables;
    END IF;

    -- 5.2 唯一送达的「五绑定」逐条落到列上（这是 §13.5 的核心，缺一列就不叫唯一送达）
    SELECT count(*) INTO n_cols FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'agreement_invite'
       AND column_name IN ('target_party_id', 'target_tenant_id', 'version_id', 'expires_at', 'status',
                           'token_hash', 'token_hint', 'invite_code',
                           'first_viewed_by', 'first_viewed_at', 'first_view_channel', 'accepted_by');
    IF n_cols <> 12 THEN
        RAISE EXCEPTION 'agreement_invite 的五绑定/留痕列不齐，实际只有 % 列', n_cols;
    END IF;
    -- token 不可枚举：必须只存哈希 + 必须唯一（同一 token 不能发两份邀请）
    SELECT count(*) INTO n_uk FROM pg_indexes
     WHERE schemaname = 'public' AND tablename = 'agreement_invite'
       AND indexdef LIKE '%UNIQUE%' AND indexdef LIKE '%(token_hash)%';
    IF n_uk <> 1 THEN
        RAISE EXCEPTION 'agreement_invite 缺少 token_hash 唯一索引（重复 token 会让"唯一送达"失效）';
    END IF;

    -- 5.3 签署记录必须能回答「谁签的、凭什么代表这家公司」（四列不可为空，§13.6）
    SELECT count(*) INTO n_cols FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'agreement_signature'
       AND is_nullable = 'NO'
       AND column_name IN ('party_id', 'signer_user_id', 'signer_party_tenant_id', 'authority_basis',
                           'version_id', 'party_side', 'sign_hash', 'signed_at');
    IF n_cols <> 8 THEN
        RAISE EXCEPTION 'agreement_signature 里"谁/代表谁/凭什么/签了哪一版"的列有缺失或可空，实际只有 % 列', n_cols;
    END IF;
    -- ⚠️ 刻意**不加** (version_id, party_side) 唯一索引：内容被改后必须重新签署，同一版同一方会有多条，
    --    每条都是当时的留痕；"当前有效的那一条"由 sign_hash 与当前快照哈希一致来判定。
    --    断言时排除主键索引（主键本身就是唯一索引，不排除会永远命中）。
    SELECT count(*) INTO n_bad
      FROM pg_index i JOIN pg_class c ON c.oid = i.indrelid
     WHERE c.relname = 'agreement_signature' AND i.indisunique AND NOT i.indisprimary;
    IF n_bad > 0 THEN
        RAISE EXCEPTION 'agreement_signature 出现了唯一索引 —— 重新签署会被唯一约束挡住，签署留痕不完整';
    END IF;

    -- 5.4 ⚠️⚠️ **终止 ≠ 免责**（§13.7）：真库上断言列名集合，命中"了结/结清/免责/违约认定"黑名单即回滚
    SELECT string_agg(column_name, ', ') INTO v_bad FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'agreement_termination'
       AND (column_name ~* '(settle|clearance|cleared|liabilit|exempt|disclaim|release|write_off|writeoff'
                          '|indemn|responsib|fault_determ|breach_confirm|breach_determ)'
            OR column_name IN ('is_settled', 'closed', 'is_closed', 'liability_closed'));
    IF v_bad IS NOT NULL THEN
        RAISE EXCEPTION 'agreement_termination 出现了"责任已了结/已结清/已免责"语义的列（%）：'
                        '终止只记录「停止履行」这个事实，系统不自动结清、不自动免责（§13.7）', v_bad;
    END IF;
    -- 反证的另一半：必须**有**记录"停止履行这个事实"的列，否则"只记事实"就成了空话
    SELECT count(*) INTO n_cols FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'agreement_termination'
       AND column_name IN ('stop_performance_at', 'source', 'basis_text',
                           'claim_counterparty_breach', 'counterparty_objection', 'objection_reason');
    IF n_cols <> 6 THEN
        RAISE EXCEPTION 'agreement_termination 缺少"停止履行事实 + 五种来源 + 依据 + 主张违约 + 异议"的列，实际 % 列', n_cols;
    END IF;
    -- 五种来源必须在注释里可枚举（这里断言 comment 不为空，防止建表时漏写枚举说明）
    SELECT count(*) INTO n_bad FROM pg_description d
      JOIN pg_class c ON c.oid = d.objoid
      JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = d.objsubid
     WHERE c.relname = 'agreement_termination' AND a.attname = 'source'
       AND d.description LIKE '%PLATFORM_EXPULSION%';
    IF n_bad <> 1 THEN
        RAISE EXCEPTION 'agreement_termination.source 的注释里没有写全五种来源（㉝）';
    END IF;

    -- 5.5 ALTER 的五列都在（协商时间线 3 列 + 变更单语义 2 列）
    SELECT count(*) INTO n_cols FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'agreement_version'
       AND column_name IN ('proposed_by_side', 'proposed_by_person', 'proposal_note',
                           'origin_version_id', 'no_retroactive_note');
    IF n_cols <> 5 THEN
        RAISE EXCEPTION 'agreement_version 的协商/变更单列不齐，实际只有 % 列', n_cols;
    END IF;

    RAISE NOTICE 'V11.490.0 自检通过：唯一送达五绑定齐 + 签署四要素不可空 + 终止列名反证"终止≠免责" + 版本协商/变更单列齐';
END $$;
