-- ============================================================================
-- V11.153.0 互联账号（资料 → 往来单位 → 互联账号）从零复刻（2026-09-11）
--
-- 背景：对标 ql361「资料 → 往来单位 → 互联账号」在本系统原为只读桩页
--      （views/md/linked-account/index.vue 直连 /md/linked-account/page ⇒ 404），
--      后端无 LinkedAccount 内容、数据库无对应表。本次从零建表 + 建 API。
--
-- 对标实测口径（2026-09-11 22stable.ql361.com 实时抓取，证据见
--      tool-results/ql361/ldaccount-shots/ 与 docs/Yh-Spec/对标页面截图/2026.9.11/互联账号/）：
--   · 温馨提示：此页面用于管理互联平台账号与对应往来单位的绑定关系。
--   · 工具栏：刷新 / 打印(F8) / 导出 / 更多（更多 = 批量解绑 / 批量删除），无「新增」。
--   · 查询区：往来单位（放大镜 → 「往来单位选择」弹窗：客户信息/供应商信息 双 Tab）
--            + 互联账号（占位「请输入互联手机号码」）+ 查询。
--   · 数据列（列配置弹窗「个人配置 / 全局配置」，全量 3 列，全部默认显示）：
--            往来单位 | 互联用户名 | 手机号。
--
-- P0 互联账号单一口径（红线）：本表为全局基础数据，营销/会员/商城互联统一引用，
--      严禁另建重复互联账号表；平台（WECHAT/ALIPAY/DOUYIN/MALL…）+关联类型字典全局唯一。
--
-- 处理：新增表幂等（IF NOT EXISTS）；仅新增，不改动既有表结构。
-- ============================================================================

-- ------------------------------------------------------------
-- 1. 互联账号绑定关系表
--    行语义 = 「一个互联平台账号 ⇆ 一个往来单位」的绑定关系
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS erp_linked_account (
    id                  BIGINT       PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL DEFAULT 0,
    -- 往来单位（biz_party.id）；列表「往来单位」列
    party_id            BIGINT,
    -- 快照字段：列表直接取快照渲染，避免每行回查 biz_party（绑定时的名称/编号留痕）
    party_code          VARCHAR(64),
    party_name          VARCHAR(255),
    -- 互联平台标识（全局唯一字典，非列表列，供营销/会员/商城互联引用）
    platform            VARCHAR(32)  NOT NULL DEFAULT 'OTHER',
    -- 关联类型标识（全局唯一字典：MEMBER 会员 / CUSTOMER 客户 / OTHER 其它）
    link_type           VARCHAR(32)  NOT NULL DEFAULT 'MEMBER',
    -- 互联用户名；列表「互联用户名」列
    linked_user_name    VARCHAR(128),
    -- 手机号；列表「手机号」列（查询条件「互联账号」按此列匹配）
    phone               VARCHAR(32),
    -- 绑定状态：1 已绑定 / 0 已解绑（「更多 → 批量解绑」= 状态切换，保留记录可追溯）
    status              SMALLINT     NOT NULL DEFAULT 1,
    remark              VARCHAR(500),
    deleted             INTEGER      NOT NULL DEFAULT 0,
    create_by           BIGINT,
    create_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  erp_linked_account                 IS '互联账号绑定关系（互联平台账号 ⇆ 往来单位，全局基础数据单一口径）';
COMMENT ON COLUMN erp_linked_account.party_id        IS '往来单位ID（biz_party.id）';
COMMENT ON COLUMN erp_linked_account.party_code      IS '往来单位编号（绑定快照）';
COMMENT ON COLUMN erp_linked_account.party_name      IS '往来单位名称（绑定快照）';
COMMENT ON COLUMN erp_linked_account.platform        IS '互联平台：WECHAT 微信 / ALIPAY 支付宝 / DOUYIN 抖音 / MALL 商城 / OTHER 其它';
COMMENT ON COLUMN erp_linked_account.link_type       IS '关联类型：MEMBER 会员 / CUSTOMER 客户 / OTHER 其它';
COMMENT ON COLUMN erp_linked_account.linked_user_name IS '互联用户名';
COMMENT ON COLUMN erp_linked_account.phone           IS '手机号';
COMMENT ON COLUMN erp_linked_account.status          IS '绑定状态：1 已绑定 / 0 已解绑';

-- 同一个平台下同一互联账号只允许绑定一次（软删除记录不占用唯一性）
CREATE UNIQUE INDEX IF NOT EXISTS uk_ela_platform_account
    ON erp_linked_account (tenant_id, platform, linked_user_name)
    WHERE deleted = 0 AND linked_user_name IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_ela_tenant   ON erp_linked_account (tenant_id);
CREATE INDEX IF NOT EXISTS idx_ela_party    ON erp_linked_account (party_id);
CREATE INDEX IF NOT EXISTS idx_ela_phone    ON erp_linked_account (phone);
CREATE INDEX IF NOT EXISTS idx_ela_deleted  ON erp_linked_account (deleted);

-- ------------------------------------------------------------
-- 2. 验证
-- ------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_name = 'erp_linked_account') THEN
        RAISE EXCEPTION 'erp_linked_account 未创建成功';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'erp_linked_account' AND column_name = 'tenant_id') THEN
        RAISE EXCEPTION 'erp_linked_account.tenant_id 缺失（多租户插件会注入该列）';
    END IF;
END $$;
