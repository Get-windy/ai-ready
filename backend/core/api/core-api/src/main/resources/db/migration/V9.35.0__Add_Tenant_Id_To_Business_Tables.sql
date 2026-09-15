-- V9.35.0: 为缺少 tenant_id 的业务表补齐租户隔离字段
-- 最佳实践：所有业务数据表都应有 tenant_id，只有全局共享系统表才加入忽略列表
-- 全局共享数据使用 tenant_id = 0，租户数据使用实际 tenant_id

-- ==================== 租户业务数据表（回填 tenant_id = 1）====================

-- 1. sys_file：文件元数据
ALTER TABLE sys_file ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE sys_file SET tenant_id = 1 WHERE tenant_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_sys_file_tenant_id ON sys_file(tenant_id);

-- 2. sys_print_chain_item：打印链路项
ALTER TABLE sys_print_chain_item ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE sys_print_chain_item SET tenant_id = 1 WHERE tenant_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_sys_print_chain_item_tenant_id ON sys_print_chain_item(tenant_id);

-- 3. sys_screenshot_task：截图任务
ALTER TABLE sys_screenshot_task ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE sys_screenshot_task SET tenant_id = 1 WHERE tenant_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_sys_screenshot_task_tenant_id ON sys_screenshot_task(tenant_id);

-- ==================== 全局共享数据表（回填 tenant_id = 0）====================
-- 这些表的数据为所有租户共享，使用 tenant_id = 0
-- 拦截器注入 (tenant_id = currentTenant OR tenant_id = 0)

-- 4. sys_permission：权限定义（全局 + 租户自定义）
ALTER TABLE sys_permission ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT 0;
UPDATE sys_permission SET tenant_id = 0 WHERE tenant_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_sys_permission_tenant_id ON sys_permission(tenant_id);

-- 5. sys_role_permission：角色权限关联
ALTER TABLE sys_role_permission ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT 0;
UPDATE sys_role_permission SET tenant_id = 0 WHERE tenant_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_sys_role_permission_tenant_id ON sys_role_permission(tenant_id);

-- 6. sys_permission_template：权限模板
ALTER TABLE sys_permission_template ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT 0;
UPDATE sys_permission_template SET tenant_id = 0 WHERE tenant_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_sys_permission_template_tenant_id ON sys_permission_template(tenant_id);

-- 7. sys_menu：菜单定义（全局 + 租户自定义）
ALTER TABLE sys_menu ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT 0;
UPDATE sys_menu SET tenant_id = 0 WHERE tenant_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_sys_menu_tenant_id ON sys_menu(tenant_id);

-- 8. sys_role_menu：角色菜单关联
ALTER TABLE sys_role_menu ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT 0;
UPDATE sys_role_menu SET tenant_id = 0 WHERE tenant_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_sys_role_menu_tenant_id ON sys_role_menu(tenant_id);
