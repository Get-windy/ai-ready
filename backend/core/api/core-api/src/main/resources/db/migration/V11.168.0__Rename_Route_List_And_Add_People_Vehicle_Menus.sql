-- =============================================================================
-- 配送模块菜单治理（PostgreSQL）
--   ① 线路执行单更名：消除与「线路档案」(md:route, 70530) 的命名撞车
--   ② 人车管理补齐子菜单：车辆管理(双入口·添加) / 车辆维护 / 配送员管理(双入口·添加)
--
-- 背景：
--   - 60502「人车管理」分组此前**没有任何子菜单**，mega 面板不渲染空列 → 车辆/骑手页面
--     虽已实现（列表页 + 表单页 + 维护页 + 后端 /api/dms/vehicle|rider）却完全不可达。
--   - 80700「线路列表」为配送路线**执行单**（erp_delivery_route），与 70530「线路」档案
--     （erp_route）字面重复，易被误读为冗余；更名后语义唯一。
--
-- 菜单规范（docs/Yh-Spec/手动整理对标开发文档/系统菜单设计与管理/41条双入口菜单的正确配置.md）：
--   - 添加标签型：path = **列表页**，list_path = **表单页**，tag_label = '添加'
--     （例：70501 商品 → path=md/product/index, list_path=md/product/form）
--   - 历史标签型：path = 表单页，list_path = 列表页，tag_label = '历史'
--
-- 说明：
--   - 菜单为系统级（tenant_id=0）；超管（SUPER_ADMIN）菜单全量下发，不依赖 sys_role_menu，
--     故新增菜单无需补授权（sys_role_menu 现有 3 条记录均非配送菜单）。
--   - 只改展示名，保留 menu_code / path 权限码，避免破坏既有授权与路由。
--   - 幂等：更名带原值判定；新增用 ON CONFLICT (id) DO UPDATE；清理误配 ID。
-- =============================================================================

-- ① 更名
UPDATE sys_menu
   SET menu_name = '配送路线',
       update_time = CURRENT_TIMESTAMP
 WHERE id = 60501 AND menu_name = '线路管理' AND deleted = 0;

UPDATE sys_menu
   SET menu_name = '配送路线单',
       update_time = CURRENT_TIMESTAMP
 WHERE id = 80700 AND menu_name = '线路列表' AND deleted = 0;

-- ② 清理早期误配（若曾以 80800/80810 写入过骑手菜单）
DELETE FROM sys_menu WHERE id IN (80800, 80810);

-- ③ 人车管理子菜单（60502 下）
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component,
                      icon, sort, is_external, is_cache, visible, status, client_type,
                      display_group, display_mode, list_path, tag_label, menu_level)
VALUES
 -- 车辆管理：添加型双入口（主菜单→列表，标签「添加」→表单）
 ('80770', 0, 60502, '车辆管理', 'dms:vehicle',             1, 'dms/vehicle',             'views/dms/vehicle/index.vue',       'CarOutlined',  1, 0, 1, 1, 1, 'tenant-admin', 0, 1, 'dms/vehicle/form', '添加', 3),
 -- 车辆维护：单入口
 ('80780', 0, 60502, '车辆维护', 'dms:vehicle-maintenance', 1, 'dms/vehicle/maintenance', 'views/dms/vehicle/maintenance.vue', NULL,           2, 0, 1, 1, 1, 'tenant-admin', 0, 0, NULL,              NULL, 3),
 -- 配送员管理：添加型双入口（主菜单→列表，标签「添加」→表单）
 -- 命名说明：业务上「骑手」统一称「配送员」——配送员可以是**企业员工**（自有配送员），
 --           也可以是**外部平台**的骑手/配送员（众包/第三方运力），见 RiderTypeEnum。
 ('80790', 0, 60502, '配送员管理', 'dms:rider',             1, 'dms/rider',               'views/dms/rider/index.vue',         'TeamOutlined', 3, 0, 1, 1, 1, 'tenant-admin', 0, 1, 'dms/rider/form',  '添加', 3)
ON CONFLICT (id) DO UPDATE SET
  parent_id    = EXCLUDED.parent_id,
  menu_name    = EXCLUDED.menu_name,
  menu_code    = EXCLUDED.menu_code,
  menu_type    = EXCLUDED.menu_type,
  path         = EXCLUDED.path,
  component    = EXCLUDED.component,
  icon         = EXCLUDED.icon,
  sort         = EXCLUDED.sort,
  visible      = EXCLUDED.visible,
  status       = EXCLUDED.status,
  display_mode = EXCLUDED.display_mode,
  list_path    = EXCLUDED.list_path,
  tag_label    = EXCLUDED.tag_label,
  menu_level   = EXCLUDED.menu_level,
  deleted      = 0,
  update_time  = CURRENT_TIMESTAMP;
