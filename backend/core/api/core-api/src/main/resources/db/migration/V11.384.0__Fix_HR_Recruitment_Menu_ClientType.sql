-- =============================================================================
-- 修复招聘管理分组的 client_type 缺失（2026-09-18）
--
-- 背景：V11.380.0 为修「招聘管理是孤儿菜单（父节点 900 不存在）」，新建了分组
--       61406「招聘管理」并把菜单 907 挂上去。**但那条 INSERT 漏写了 client_type**，
--       于是 61406 的 client_type = NULL。
--
-- 后果（本次实测）：`SysMenuServiceImpl.getUserMegaMenus` 对菜单的查询条件是
--       `client_type = :clientType AND status = 1 AND visible = 1 AND deleted = 0`，
--       而 61406 的 client_type 为 NULL → **永远不匹配** → 该分组连同其下的
--       907 招聘管理整支从菜单树上消失，用户依旧点不到（原来点不到是因为父节点不存在，
--       修完变成因为 client_type 为空，症状一模一样）。
--       直接访问 /hr/recruitment 会落到 404 兜底路由。
--
-- 说明：同级的 61401–61405、顶层 60014、以及 907 本身都是 `tenant-admin`。
--       本迁移补齐缺失值，并顺带做一次「子菜单继承父菜单 client_type」的通用订正
--       （只填 NULL，不覆盖任何已有值）。
-- =============================================================================

-- 一、定点修复本次引入的 61406
UPDATE sys_menu
   SET client_type = 'tenant-admin',
       update_time = CURRENT_TIMESTAMP
 WHERE id = 61406
   AND client_type IS NULL;

-- 二、通用订正：任何 client_type 为空、但父节点有值的菜单，继承父节点的 client_type。
--     只影响 NULL 行（当前全库仅 61406 一行），不会覆盖既有取值。
UPDATE sys_menu c
   SET client_type = p.client_type,
       update_time = CURRENT_TIMESTAMP
  FROM sys_menu p
 WHERE c.parent_id = p.id
   AND c.client_type IS NULL
   AND p.client_type IS NOT NULL
   AND c.deleted = 0;
