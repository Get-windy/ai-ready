-- =============================================================================
-- 创建「操作员数据权限（对象清单授权）」表 + 权限码种子
-- V11.430.0 · 2026-09-19
--
-- 【依据】
--   对标 ql361「资料 → 职员权限 → 全部操作员」页：每个操作员有 7 类数据权限 ——
--   仓库权限 / 调拨权限 / 部门权限 / 往来单位权限 / 商品权限 / 现金银行权限 / 客户级别权限
--   （2026-09-19 登录 22stable.ql361.com 实抓确认）。
--   本轮为「操作员（sys_user）× 维度 × 授权对象清单」补读写能力。
--   我方 views/system/user/index.vue 原先完全没有这套能力。
--
-- 【为什么新建独立表，而不是复用已有的两张表】
--   库里已有：
--     sys_data_permission —— 角色/用户 + 权限码 + dataScope，**规则型**（按规则推导行级可见范围）
--     sys_data_scope      —— 角色 + rule_type + dept_ids，**规则型**（按规则推导行级可见范围）
--   两者都是「行级数据过滤规则」模型，语义上**没有**「维度 → 授权对象 id 清单」这一层；
--   且经实测两表均为 0 行、未被任何链路读写（未接线）。
--   硬塞进这两张表要么污染其语义，要么得堆一堆多态字段 —— 故新建独立表，两张旧表保持原样不动。
--
-- 【版本号实测依据】
--   落地前 ls 本目录（core/api/core-api/src/main/resources/db/migration）：
--   当前最大迁移版本为 V11.429.0__Converge_Staff_Role_Menu_To_Role_View.sql，
--   V11.430.0 未被占用，故取 V11.430.0。
--
-- 【sys_permission.id 号段实测依据】（devdb，2026-09-19，psycopg2 直连 localhost:5432/devdb）
--   SELECT max(id) FROM sys_permission;
--     → 2067913177527037954（雪花段，非小整数）
--   SELECT id FROM sys_permission WHERE id BETWEEN 91500 AND 91999 ORDER BY id;
--     → 已占用 91501..91558 与 91601..91692（共 150 个），**91559..91600 为实测空闲连续段**
--   SELECT count(*) FROM sys_permission WHERE id = 91559;   → 0
--   SELECT count(*) FROM sys_permission WHERE id = 91560;   → 0
--   SELECT count(*) FROM sys_permission WHERE id BETWEEN 91693 AND 91799; → 0
--   SELECT id FROM sys_permission WHERE permission_code IN ('data-scope:view','data-scope:set'); → 0 行
--   ⇒ 本次取 **91559（data-scope:view） / 91560（data-scope:set）**。
--   ⚠️ sys_permission.id **没有序列默认值**，必须显式给值。
--
-- 【sys_permission 字段口径】
--   tenant_id=1 / parent_id=0 / deleted=0 / permission_type=3（按钮型）
--   api_path=NULL / method=NULL / visible=1 / **status=0**
--   ⚠️ status 语义经实证为 **0=正常**（不是 1），与 SysPermission 实体注释一致。
--
-- 【幂等】
--   建表 / 索引：IF NOT EXISTS；权限码：ON CONFLICT (id) DO NOTHING（id 为主键）。
--   本迁移**不向任何角色授权**（普通角色该不该有这些码属业务决策，
--   由租户在「系统 → 角色管理」自行勾选；超管另有 `*` 通配不受影响），故不写 sys_role_permission。
--
-- 【回滚】
--   DROP TABLE IF EXISTS sys_user_data_scope;
--   DELETE FROM sys_permission WHERE id IN (91559, 91560)
--     AND permission_code IN ('data-scope:view', 'data-scope:set');
-- =============================================================================

-- ── ① 操作员数据权限表（对象清单授权）────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sys_user_data_scope (
    id          BIGINT PRIMARY KEY,                 -- 应用侧 ASSIGN_ID 雪花
    tenant_id   BIGINT NOT NULL DEFAULT 0,
    user_id     BIGINT NOT NULL,                    -- 操作员 sys_user.id
    scope_key   VARCHAR(32) NOT NULL,               -- 维度：warehouse/transfer/department/partner/product/fund/customer_level
    target_ids  TEXT,                               -- 授权对象 id 列表，逗号分隔；空/NULL = 未设置
    deleted     INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    update_time TIMESTAMP DEFAULT now(),
    create_by   BIGINT,
    update_by   BIGINT,
    version     INTEGER DEFAULT 0
);

COMMENT ON TABLE sys_user_data_scope IS '操作员数据权限（维度 → 授权对象清单，覆盖式保存）';
COMMENT ON COLUMN sys_user_data_scope.user_id IS '操作员ID，对应 sys_user.id';
COMMENT ON COLUMN sys_user_data_scope.scope_key IS '权限维度：warehouse/transfer/department/partner/product/fund/customer_level';
COMMENT ON COLUMN sys_user_data_scope.target_ids IS '授权对象 id 列表（逗号分隔）；空/NULL 表示该维度未设置';
COMMENT ON COLUMN sys_user_data_scope.deleted IS '逻辑删除标记 0-未删除 1-已删除';

-- 唯一约束：同一操作员同一维度只允许一行有效记录（部分唯一索引，软删行不占键值）
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_data_scope
    ON sys_user_data_scope (user_id, scope_key) WHERE deleted = 0;

-- ── ② 权限码种子（id 91559 / 91560）──────────────────────────────────────────
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
VALUES (91559, 1, 0, 0, now(), now(), '数据权限查看', 'data-scope:view', 3, NULL, NULL, 1032, 1, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
VALUES (91560, 1, 0, 0, now(), now(), '数据权限设置', 'data-scope:set', 3, NULL, NULL, 1033, 1, 0)
ON CONFLICT (id) DO NOTHING;
