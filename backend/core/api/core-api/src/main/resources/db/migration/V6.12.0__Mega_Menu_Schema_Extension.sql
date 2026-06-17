-- ============================================================
-- V6.12.0: Mega Menu 改造 - 数据结构扩展
--
-- 变更说明：
--   1. sys_menu 新增 4 个字段：
--      - display_mode: 入口模式（0=默认, 1=双入口[表单+列表]）
--      - list_path: 双入口模式下列表页路由路径
--      - tag_label: 标签文案（历史/列表/添加）
--      - menu_level: 菜单层级（0=租户级, 1=系统级）
--   2. 新增 sys_tenant_menu 租户菜单授权表
--
-- 影响范围：
--   - sys_menu: 新增 4 个字段
--   - 新建: sys_tenant_menu 表
-- ============================================================

-- 1. sys_menu 新增 4 个字段
ALTER TABLE sys_menu
    ADD COLUMN IF NOT EXISTS display_mode SMALLINT DEFAULT 0;

ALTER TABLE sys_menu
    ADD COLUMN IF NOT EXISTS list_path VARCHAR(255) DEFAULT NULL;

ALTER TABLE sys_menu
    ADD COLUMN IF NOT EXISTS tag_label VARCHAR(20) DEFAULT NULL;

ALTER TABLE sys_menu
    ADD COLUMN IF NOT EXISTS menu_level SMALLINT DEFAULT 0;

COMMENT ON COLUMN sys_menu.display_mode IS '入口模式(0=默认,1=双入口)';
COMMENT ON COLUMN sys_menu.list_path IS '双入口模式下列表页路由路径';
COMMENT ON COLUMN sys_menu.tag_label IS '标签文案(历史/列表/添加)';
COMMENT ON COLUMN sys_menu.menu_level IS '菜单层级(0=租户级,1=系统级)';

-- 2. 新建 sys_tenant_menu 租户菜单授权表
CREATE TABLE IF NOT EXISTS sys_tenant_menu (
    id BIGSERIAL NOT NULL,
    tenant_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_tenant_menu UNIQUE (tenant_id, menu_id)
);

COMMENT ON TABLE sys_tenant_menu IS '租户菜单授权表(系统管理员授权给租户的菜单)';
COMMENT ON COLUMN sys_tenant_menu.tenant_id IS '租户ID';
COMMENT ON COLUMN sys_tenant_menu.menu_id IS '菜单ID';
COMMENT ON COLUMN sys_tenant_menu.create_time IS '授权时间';

-- 3. 创建索引
CREATE INDEX IF NOT EXISTS idx_tenant_menu_tenant_id ON sys_tenant_menu (tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_menu_menu_id ON sys_tenant_menu (menu_id);
