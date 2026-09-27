-- 用户与三方平台（钉钉 / 企业微信 / 飞书）账号的绑定关系。
--
-- 用途：① 已登录用户绑定三方账号，提高账号安全性；② 之后可用三方扫码免密登录。
--
-- 归属是「用户」而非「租户」：一个用户可能属于多个企业，绑定关系跟着人走，
-- 故本表**没有 tenant_id 列**，须同步登记进 MyBatisPlusConfig 的租户插件忽略清单
-- （否则拦截器会给 INSERT 自动补一列不存在的 tenant_id，报错后整个事务静默回滚）。

CREATE TABLE IF NOT EXISTS sys_user_social_binding (
    id              BIGINT PRIMARY KEY,
    user_id         BIGINT        NOT NULL,
    -- dingtalk / wecom / feishu
    platform        VARCHAR(20)   NOT NULL,
    -- 三方侧企业标识。路线 B（服务商模式）下每个客户企业各不相同，
    -- 是「同一平台不同应用」的区分键，参与唯一约束。
    corp_id         VARCHAR(64),
    -- 平台内用户标识（同一应用内唯一）
    open_id         VARCHAR(128)  NOT NULL,
    -- 跨应用唯一标识（钉钉 unionId / 微信 unionid，飞书为 union_id）
    union_id        VARCHAR(128),
    -- 企业内成员 ID（企微 userid / 钉钉 userid），用于组织相关能力
    corp_user_id    VARCHAR(128),
    nickname        VARCHAR(100),
    avatar          VARCHAR(500),
    bind_time       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    last_login_time TIMESTAMP,
    deleted         INTEGER       NOT NULL DEFAULT 0,
    create_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_user_social_binding IS '用户三方账号绑定（钉钉/企业微信/飞书）';

-- 同一个三方身份只能绑到一个系统账号，防止串号（A 的钉钉被绑到 B 的账号上）。
-- corp_id 可能为空（如钉钉扫码登录第三方网站只返回 unionId，不含企业信息），
-- PG 中 NULL 互不相等会使唯一约束失效，故用 COALESCE 规范化后再建唯一索引。
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_social_identity
    ON sys_user_social_binding(platform, COALESCE(corp_id, ''), open_id)
    WHERE deleted = 0;

-- 查「某个用户绑了哪些平台」
CREATE INDEX IF NOT EXISTS idx_sys_user_social_user
    ON sys_user_social_binding(user_id, platform)
    WHERE deleted = 0;
