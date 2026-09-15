-- V8.15.0: 修复"商品"菜单重复导致的路由冲突
-- 问题：id=70501(V6.21.0) 和 id=80500(V6.22.0) 都是"商品"菜单，path=md/product
-- id=80500 component=views/common/placeholder/index.vue（占位页）
-- 解决方案：彻底删除重复的 id=80500，确保 id=70501 配置与其他"添加"标签一致
-- "添加"标签约定：path → 表单组件（主按钮跳 listPath=列表页），listPath → 列表页路径（标签按钮跳 path=表单页）

-- 彻底删除重复的"商品"菜单（id=80500）
DELETE FROM sys_menu WHERE id = 80500;

-- 重新插入正确的"商品"菜单（id=70501）- 以防被意外删除
-- 与其他"添加"标签项保持一致：path → 表单组件，listPath → 列表页路径
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, path, component, icon, sort, visible, status,
    client_type, display_mode, list_path, tag_label, display_group, menu_level)
VALUES (70501, 0, 61101, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    '商品', 'md:product', 1, 'md/product/form', 'views/erp/product/form.vue',
    'AppstoreOutlined', 1, 1, 1, 'tenant-admin', 1, 'md/product', '添加', 0, 0)
ON CONFLICT (id) DO UPDATE SET
    path = 'md/product/form',
    component = 'views/erp/product/form.vue',
    list_path = 'md/product',
    display_mode = 1,
    tag_label = '添加',
    deleted = 0,
    update_time = CURRENT_TIMESTAMP;
