-- 营销模块两个 P0 的根因修复（2026-09-26）
-- 依据：MARKETING_MODULE_AUDIT_20260924.md §2.2 / §2.3，以及《系统菜单设计与管理/mega-menu-redesign.md》§6.3/§6.7。
--
-- ─────────────────────────────────────────────────────────────────────────────
-- 一、menu_level：营销 19 条叶子页 3 → 0
--
-- 根因（文档冲突被数据化）：
--   · `mega-menu-redesign.md` §6.3 定义 `menu_level` 只有 0(租户级) / 1(系统级)，§6.7 明写「营销 = 租户级 menu_level=0」；
--   · 而《系统菜单开发文档-营销模块菜单新增.md》§2 把值 **3** 写死进 V11.377.0（"沿用同组既有值"）——
--     值 3 在两篇文档里从未被定义过，是历史的越权取值。
-- 服务端口径：`SysMenuServiceImpl#getUserMegaMenus`：
--     if (!isSystemTenant && !isSuperAdmin) wrapper.eq(SysMenu::getMenuLevel, 0)
--   ⇒ 非 1 号租户的非超管用户，营销 19 页**全不下发**（只剩 5 个点不开的空分组）。
--     实测：租户 2 账号 `e2e_hr_t2` 下发 0/19；而 `admin`（系统租户+超管）走早退分支不受过滤，
--     所以开发与验收从未暴露该问题。
-- 同类已修先例：分析模块 29 条(V11.499.0) / DMS 20 条(V11.501.0) / CRM 17 条(V11.500.0)。
-- 幂等：只动 parent_id 属于营销四分组且当前为 3 的行，重复执行安全。
UPDATE sys_menu
SET menu_level = 0, update_time = now()
WHERE parent_id IN (60801, 60802, 60803, 60804)
  AND menu_type = 1
  AND deleted = 0
  AND menu_level <> 0;

-- ─────────────────────────────────────────────────────────────────────────────
-- 二、角色授权：营销权限码只授给了 SUPER_ADMIN
--
-- 实测（修复前）：`sys_permission` 中 `marketing:%` 共 105 条，**105 条全部只授 SUPER_ADMIN**，
--   其它角色 0 条 ⇒ 非超管登录后 18 个代表端点 **18/18 全 403**（`e2e_hr_ta` = SYSTEM_ADMIN 实测）。
-- 角色矩阵口径（与本库既有事实一致）：SYSTEM_ADMIN 已是事实上的「租户管理员」
--   （已持 stock 106 / dms 103 / wms 98 / hr 37 / finance 16 / delivery 14 / md 9），
--   却唯独从未拿到营销；DEPT_ADMIN 是「部门管理员」，各处拿的是只读子集
--   （dms 58 / stock 44 / wms 39 / finance 16 …）。
-- 因此本迁移：SYSTEM_ADMIN ← 营销全量；DEPT_ADMIN ← 只读子集(list/view/detail)。
--
-- 另：营销的「商城弹窗广告 / 热门搜索词推荐 / 套餐」三页复用商城与资料域端点
--   （`mall:popup-ad:*`、`mall:keyword:*`、`product:kit:*`），不一起授权的话，
--   菜单救回来、这三页仍然 403。故同批处理。
-- 还有「会员管理」页：取数走 `/erp/md/customer/member/page`，鉴权码是 **`md:customer:list`**
--   （MD 资料域，见 MdCustomerController.java:228）—— 不一起授权，第 1 页就是 403。
--   它的完整角色矩阵属《资料模块全栈审计（2026-09-24）》范围，本迁移只补「营销页要用到的那些」。
--
-- ⚠️ 主键：`sys_role_permission.id` 无序列默认值，必须显式给号。
--    沿用 V11.487.0 的做法：`max(id) + row_number()`，且用 NOT EXISTS 保证幂等。
-- ⚠️ 补权限码铁律：本迁移只**授权**、不新增权限码（105 条码已在 V11.447.0 / V11.481.0 落库）。

-- 2.1 SYSTEM_ADMIN（role_id = 2065122951570362369）：营销全量 + 三页跨域全量
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time, create_by)
SELECT (SELECT COALESCE(max(id), 0) FROM sys_role_permission) + row_number() OVER (ORDER BY p.id),
       r.id, p.id, 0, now(), 1
FROM sys_permission p
CROSS JOIN (SELECT id FROM sys_role WHERE role_code = 'SYSTEM_ADMIN') r
WHERE (p.permission_code LIKE 'marketing:%'
    OR p.permission_code LIKE 'mall:popup-ad:%'
    OR p.permission_code LIKE 'mall:keyword:%'
    OR p.permission_code LIKE 'product:kit:%'
    OR p.permission_code LIKE 'md:customer:%')          -- 会员管理页取数（会员管理子标签）
  AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission x
                  WHERE x.role_id = r.id AND x.permission_id = p.id);

-- 2.2 DEPT_ADMIN（role_id = 2065122951620694018）：营销只读子集 + 三页跨域只读子集
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time, create_by)
SELECT (SELECT COALESCE(max(id), 0) FROM sys_role_permission) + row_number() OVER (ORDER BY p.id),
       r.id, p.id, 0, now(), 1
FROM sys_permission p
CROSS JOIN (SELECT id FROM sys_role WHERE role_code = 'DEPT_ADMIN') r
WHERE (p.permission_code LIKE 'marketing:%:list'
    OR p.permission_code LIKE 'marketing:%:view'
    OR p.permission_code LIKE 'marketing:%:detail'
    OR p.permission_code = 'marketing:points:list'
    OR p.permission_code IN ('mall:popup-ad:list', 'mall:popup-ad:detail',
                             'mall:keyword:list', 'product:kit:list', 'product:kit:detail',
                             'md:customer:list', 'md:customer:detail'))
  AND p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission x
                  WHERE x.role_id = r.id AND x.permission_id = p.id);

-- 2.3 复核口径（执行后应满足）：
--   SELECT r.role_code, count(*) FROM sys_role_permission rp
--     JOIN sys_permission p ON p.id = rp.permission_id JOIN sys_role r ON r.id = rp.role_id
--    WHERE p.permission_code LIKE 'marketing:%' GROUP BY 1;
--   -- 期望：SUPER_ADMIN 105 / SYSTEM_ADMIN 105 / DEPT_ADMIN ≈ (只读子集)
