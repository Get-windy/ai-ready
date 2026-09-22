-- 商城顾客模型：**系统顾客 + 租户关联** 双层（2026-09-22 用户裁定）
--
-- 【模型（用户原话）】「商城顾客应该又是**系统顾客**！商城顾客是属于租户的，
--   系统顾客是属于系统的，**一个系统顾客可以属于多个租户，前提是租户添加该顾客**」。
--   即：`shop_user` 是**系统级身份**（与 `sys_user` 同性质），
--       「某个租户的顾客」是**关联关系**（与 `sys_user_tenant` 同性质）。
--
-- 【改造前的冲突（已实测）】`shop_user.tenant_id` 是 **NOT NULL 的单租户列**
--   ⇒ 今天一个商城账号属于且只属于一个租户，与"可属多个租户"直接冲突；
--   而且**没有**系统级顾客实体、**没有**"顾客 × 租户"关联表、也**没有**"租户添加该顾客"这条流程。
--
-- 【本次改动】
--   1) `shop_user.tenant_id` 语义改为「**系统级归属位**」：一律 **0**（本仓 tenant_id=0 已是
--      「全局共享数据」容器，见 F-01 裁定）。它不再表示"这个顾客属于哪个租户"。
--   2) 新建 `shop_user_tenant`：`(shop_user_id, tenant_id)` 的关联 + 状态 + 审核字段，
--      照 `sys_user_tenant` 的形状做。**它才是"这个顾客属于哪个租户"的唯一事实来源**。
--   3) `shop_user.audit_status` 等审核列**降级为历史列**（不再作为租户级审核依据）——
--      因为审核必须是**逐租户**的：同一个系统顾客，A 店批准、B 店拒绝是完全合法的。
--
-- 【唯一索引为什么用「部分唯一索引」（带 WHERE deleted = 0）】
--   本仓踩过这个坑（见 MASTER_TODO 的 CRM-BREAK-03）：普通 UNIQUE 不含 deleted 时，
--   **软删的行仍占着键值** ⇒ 顾客被某租户"解除关联"后，再想重新注册/重新添加会撞唯一约束。
--   注册/解除这类需要"可重来"的业务，唯一索引必须只约束未删除行。
--
-- 【数据面】改造前 `shop_user` 与 `shop_user_party_link` **均为 0 行**（已实测）
--   ⇒ 无历史数据需要迁移，本迁移不做任何回填。
--
-- 【顺带登记进租户忽略清单】`shop_user` 现在是系统级身份表（tenant_id 恒 0），
--   必须加入 `MyBatisPlusConfig.IGNORE_TENANT_TABLES`，否则租户会话读它会注入
--   `AND tenant_id = <会话租户>` ⇒ 一行读不到。已在同一批代码里改。
--   `shop_user_tenant` **不**忽略：它本身就是租户维度数据，让拦截器按会话租户过滤正合适。
-- =============================================================

ALTER TABLE shop_user ALTER COLUMN tenant_id SET DEFAULT 0;
COMMENT ON COLUMN shop_user.tenant_id IS
  '系统级归属位，恒为 0（全局顾客身份）。"属于哪个租户"看 shop_user_tenant，不要用本列判断归属。';

CREATE TABLE IF NOT EXISTS shop_user_tenant (
    id              BIGINT       PRIMARY KEY,
    shop_user_id    BIGINT       NOT NULL,
    tenant_id       BIGINT       NOT NULL,
    -- 0=待审核 1=正常 2=已拒绝 3=已解除
    status          INTEGER      NOT NULL DEFAULT 0,
    -- 来源：self_register=本店自助注册 / system_reuse=复用已有系统顾客身份绑定 / tenant_add=租户后台添加
    source          VARCHAR(32),
    -- 该顾客在某租户下的默认身份（未来一个顾客在一店可有多个身份时用）
    is_default      INTEGER      NOT NULL DEFAULT 0,
    -- 申请时留的手机号（租户审核时对照用，与 shop_user_party_link 同口径）
    applied_phone   VARCHAR(32),
    audit_by        BIGINT,
    audit_time      TIMESTAMP,
    reject_reason   VARCHAR(255),
    create_by       BIGINT,
    create_time     TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP,
    deleted         INTEGER      NOT NULL DEFAULT 0
);

COMMENT ON TABLE shop_user_tenant IS
  '商城「系统顾客 × 租户」关联：一行 = 某系统顾客在某个租户商城里的注册/入驻关系与审核状态。'
  '唯一事实来源 —— 判断"顾客属于哪个租户"只能查本表，不要看 shop_user.tenant_id（那是系统级归属位，恒 0）。';

-- 部分唯一索引：只约束未删除行，保证「解除后可重新注册/重新添加」（见头部说明）
CREATE UNIQUE INDEX IF NOT EXISTS uk_shop_user_tenant
    ON shop_user_tenant (shop_user_id, tenant_id) WHERE deleted = 0;

-- 租户后台「顾客管理」按租户 + 状态翻页
CREATE INDEX IF NOT EXISTS idx_shop_user_tenant_tenant_status
    ON shop_user_tenant (tenant_id, status) WHERE deleted = 0;

-- 商城登录时按"这个系统顾客有没有被本租户添加"反查
CREATE INDEX IF NOT EXISTS idx_shop_user_tenant_user
    ON shop_user_tenant (shop_user_id) WHERE deleted = 0;
