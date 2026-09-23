-- 2026-09-23 配送模块（DMS）审计修复
-- 报告：DMS_MODULE_AUDIT_20260923.md §10.2
--
-- 一、20 条配送自建菜单的 menu_level 由 3 改回 0
--
--   服务端 SysMenuServiceImpl.getUserMegaMenus:276-279 对「非系统租户且非超管」强制
--        wrapper.eq(SysMenu::getMenuLevel, 0)
--   《mega-menu-redesign.md》与菜单管理表单都只有 0=租户级 / 1=系统级 —— 3 是无人承认的取值。
--
--   这 20 条是 V11.168.0 / V11.169.0 / V11.170.0 / V11.201.0 四个迁移补录时统一写成了 3
--   （且 `ON CONFLICT (id) DO UPDATE SET menu_level = EXCLUDED.menu_level` 会持续覆盖回 3）。
--   后果：**普通租户的非超管用户看不到整个配送自建体系**（配送单/仪表盘/路线单/规划/
--   人车管理 5 页/调度 3 页/跟踪 3 页/配置 3 页/结算 2 页）。
--   叠加「配送权限码只授超管」⇒ 这 20 页 = 看不见 + 调不了（双阻断）。
--
--   ⚠️ dev 的 admin 账号同时命中 isSystemTenant 与 isSuperAdmin 双豁免（走 :259 早退分支
--      取全量），**本地永远看不出问题** —— 这也是历次 E2E 全绿的原因。
--
--   注：全库 menu_level=3 共 124 条，V11.499.0 已处理分析模块 29 条、V11.500.0 处理 CRM，
--      各模块自成迁移；本条只处理配送（20 条）。
--
--   修法用**显式 id 列表**而非 path 前缀：80760 配送单的 path 是
--   `dispatch/dispatch-order/form`（不含 dms/ 前缀），按 path LIKE 'dms/%' 会漏掉它。

UPDATE sys_menu SET menu_level = 0
WHERE deleted = 0 AND menu_level = 3
  AND id IN (80700, 80730, 80740, 80750, 80760, 80770, 80780, 80790, 80820, 80830,
             80840, 80847, 80850, 80860, 80870, 80880, 80890, 80900, 80910, 80920);

-- 二、60401「配送业务」下 3 个叶子的 sort 并列（原 1/1/2）
--
--   并列时展示顺序不稳定（orderByAsc(sort) 无二级排序）。
UPDATE sys_menu SET sort = 2 WHERE id = 80760 AND deleted = 0 AND sort = 1;
UPDATE sys_menu SET sort = 3 WHERE id = 80820 AND deleted = 0 AND sort = 2;
