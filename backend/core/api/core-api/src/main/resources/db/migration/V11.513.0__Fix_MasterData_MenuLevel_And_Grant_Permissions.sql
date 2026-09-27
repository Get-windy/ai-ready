-- 2026-09-26 资料模块全栈审计修复
-- 报告：MD_MODULE_AUDIT_20260924.md（§1 P0-1 权限、§1 P0-2 menu_level）
--
-- ══════════════════════════════════════════════════════════════════════════
-- 一、5 条资料模块菜单的 menu_level 由 3 改回 0（P0-2）
-- ══════════════════════════════════════════════════════════════════════════
-- 服务端 SysMenuServiceImpl.getUserMegaMenus 对「非系统租户且非超管」强制
--     wrapper.eq(SysMenu::getMenuLevel, 0)
-- 《mega-menu-redesign.md》与菜单管理表单都只有 0=租户级 / 1=系统级 —— 3 是无人承认的取值
-- （另一份文档《系统菜单开发文档-营销模块菜单新增.md》把它当"层级深度"用，两份文档语义冲突）。
--
-- 这 5 条是「往来单位」与「仓库管理」两个分组下的核心主数据页：
--   80510 客户 / 80511 供应商 / 80512 物流公司 / 80513 其他往来单位 / 80520 仓库规划
-- 后果：非系统租户（tenant_id<>1）的非超管**整组看不到**，4 条双入口的两个入口一起消失。
-- 同模块自证异常：同为添加型双入口的 70501 商品是 0，同分组的 70520 商品货位设置为 0。
--
-- ⚠️ dev 的 admin 账号同时命中 isSystemTenant 与 isSuperAdmin 双豁免，本地永远看不出来。
--
-- 注：全库 menu_level=3 原共 124 条，V11.499.0 分析 29 / V11.500.0 CRM / V11.501.0 配送 20 /
--    V11.502.0 HR 4 / V11.510.0 设置 10；本条只处理资料模块这 5 条。
--
-- 【回滚】UPDATE sys_menu SET menu_level = 3
--         WHERE id IN (80510, 80511, 80512, 80513, 80520);
UPDATE sys_menu SET menu_level = 0
 WHERE deleted = 0
   AND menu_level = 3
   AND id IN (80510, 80511, 80512, 80513, 80520);

-- ══════════════════════════════════════════════════════════════════════════
-- 二、给租户角色授予资料模块权限码（P0-1）
-- ══════════════════════════════════════════════════════════════════════════
-- 实测（tools/audit-md-permission.py）：资料模块 24 页用到的 161 个权限码里
-- **128 个只授予 SUPER_ADMIN** ⇒ 真机对照 e2e_hr_ta（租户1 SYSTEM_ADMIN）21/24 个接口 403，
-- 同一批接口超管 24/24 为 200。叠加菜单可见性由权限码派生（无码则菜单也不下发）⇒ 双阻断。
--
-- 授权矩阵（遵循职责分离）：
--   · SUPER_ADMIN  通配符 *，无需授权行
--   · SYSTEM_ADMIN 资料模块全部 161 个码（它是租户内的最高管理角色）
--   · DEPT_ADMIN   只读子集 80 个（:list/:view/:detail/:query/:export）——查看与导出，不含增删改
--   · E2E_T2_ADMIN 刻意不动：`tools/verify-module-authz.cjs` 用它当"无码的非超管"探针
--
-- ⚠️ 用**精确码清单**而非前缀：资料模块的码分散在 9 个命名空间
--    （md/product/party/wms/finance/erp/pricing/mall/stock），其中 wms:*、finance:*、mall:* 等
--    同时包含其他模块的码，按前缀授权会越界。
--
-- 【回滚】DELETE FROM sys_role_permission WHERE id BETWEEN 9800000 AND 9899999;

-- ── ① 系统管理员：资料模块全部 ──
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9800000 + row_number() OVER (ORDER BY p.id), r.id, p.id, 1, now()
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'SYSTEM_ADMIN'
  AND p.deleted = 0
  AND p.permission_code IN (
      'erp:batch:detail', 'erp:batch:list', 'erp:batch:update',
      'erp:product:create', 'erp:product:delete', 'erp:product:list',
      'erp:product:price-batch', 'erp:product:status', 'erp:product:update',
      'erp:product:view', 'finance:account:create', 'finance:account:delete',
      'finance:account:detail', 'finance:account:export', 'finance:account:list',
      'finance:account:update', 'finance:account:view', 'finance:bank-account:create',
      'finance:bank-account:delete', 'finance:bank-account:detail', 'finance:bank-account:export',
      'finance:bank-account:list', 'finance:bank-account:status', 'finance:bank-account:update',
      'finance:bank-account:view', 'mall:tag:create', 'mall:tag:delete',
      'mall:tag:export', 'mall:tag:list', 'mall:tag:update',
      'md:customer:create', 'md:customer:delete', 'md:customer:detail',
      'md:customer:export', 'md:customer:list', 'md:customer:update',
      'md:customer:view', 'md:expense-type:create', 'md:expense-type:delete',
      'md:expense-type:detail', 'md:expense-type:export', 'md:expense-type:list',
      'md:expense-type:update', 'md:expense-type:view', 'md:image:create',
      'md:image:delete', 'md:image:list', 'md:image:view',
      'md:linked-account:create', 'md:linked-account:delete', 'md:linked-account:detail',
      'md:linked-account:list', 'md:linked-account:update', 'md:linked-account:view',
      'md:other-income:create', 'md:other-income:delete', 'md:other-income:detail',
      'md:other-income:export', 'md:other-income:list', 'md:other-income:update',
      'md:other-income:view', 'md:payment-channel:edit', 'md:payment-channel:export',
      'md:payment-channel:view', 'md:payment-method:create', 'md:payment-method:delete',
      'md:payment-method:detail', 'md:payment-method:export', 'md:payment-method:list',
      'md:payment-method:status', 'md:payment-method:update', 'md:route-master:create',
      'md:route-master:delete', 'md:route-master:detail', 'md:route-master:export',
      'md:route-master:import', 'md:route-master:list', 'md:route-master:status',
      'md:route-master:update', 'md:route-master:view', 'party:attachments:create',
      'party:attachments:delete', 'party:attachments:detail', 'party:categories:create',
      'party:categories:delete', 'party:categories:list', 'party:categories:update',
      'party:categories:view', 'party:contacts:create', 'party:contacts:delete',
      'party:contacts:detail', 'party:contacts:update', 'party:customer-region:create',
      'party:customer-region:delete', 'party:customer-region:detail', 'party:customer-region:list',
      'party:customer-region:update', 'party:grades:create', 'party:grades:detail',
      'party:grades:view', 'pricing:approval:approve', 'pricing:approval:create',
      'pricing:approval:detail', 'pricing:approval:list', 'pricing:approval:reject',
      'pricing:approval:view', 'product:barcodes:detail', 'product:brand:create',
      'product:brand:delete', 'product:brand:export', 'product:brand:list',
      'product:brand:update', 'product:category:create', 'product:category:delete',
      'product:category:detail', 'product:category:list', 'product:category:update',
      'product:grade:create', 'product:grade:delete', 'product:grade:detail',
      'product:grade:list', 'product:grade:update', 'product:kit:list',
      'product:location:create', 'product:location:export', 'product:location:list',
      'product:unit-dict:create', 'product:unit-dict:delete', 'product:unit-dict:export',
      'product:unit-dict:list', 'product:unit-dict:update', 'product:unit-group:create',
      'product:unit-group:delete', 'product:unit-group:detail', 'product:unit-group:export',
      'product:unit-group:list', 'product:unit-group:update', 'stock:inventory-mode:list',
      'stock:inventory-mode:update', 'stock:inventory-mode:view', 'stock:view',
      'wms:category:create', 'wms:category:delete', 'wms:category:detail',
      'wms:category:list', 'wms:category:update', 'wms:create',
      'wms:delete', 'wms:detail', 'wms:export',
      'wms:list', 'wms:location:create', 'wms:location:delete',
      'wms:location:detail', 'wms:location:export', 'wms:location:generate',
      'wms:location:list', 'wms:location:view', 'wms:view',
      'wms:warehouse:delete', 'wms:warehouse:view'
  )
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- ── ② 部门管理员：只读子集 ──
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9850000 + row_number() OVER (ORDER BY p.id), r.id, p.id, 1, now()
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'DEPT_ADMIN'
  AND p.deleted = 0
  AND p.permission_code IN (
      'erp:batch:detail', 'erp:batch:list', 'erp:product:list',
      'erp:product:view', 'finance:account:detail', 'finance:account:export',
      'finance:account:list', 'finance:account:view', 'finance:bank-account:detail',
      'finance:bank-account:export', 'finance:bank-account:list', 'finance:bank-account:view',
      'mall:tag:export', 'mall:tag:list', 'md:customer:detail',
      'md:customer:export', 'md:customer:list', 'md:customer:view',
      'md:expense-type:detail', 'md:expense-type:export', 'md:expense-type:list',
      'md:expense-type:view', 'md:image:list', 'md:image:view',
      'md:linked-account:detail', 'md:linked-account:list', 'md:linked-account:view',
      'md:other-income:detail', 'md:other-income:export', 'md:other-income:list',
      'md:other-income:view', 'md:payment-channel:export', 'md:payment-channel:view',
      'md:payment-method:detail', 'md:payment-method:export', 'md:payment-method:list',
      'md:route-master:detail', 'md:route-master:export', 'md:route-master:list',
      'md:route-master:view', 'party:attachments:detail', 'party:categories:list',
      'party:categories:view', 'party:contacts:detail', 'party:customer-region:detail',
      'party:customer-region:list', 'party:grades:detail', 'party:grades:view',
      'pricing:approval:detail', 'pricing:approval:list', 'pricing:approval:view',
      'product:barcodes:detail', 'product:brand:export', 'product:brand:list',
      'product:category:detail', 'product:category:list', 'product:grade:detail',
      'product:grade:list', 'product:kit:list', 'product:location:export',
      'product:location:list', 'product:unit-dict:export', 'product:unit-dict:list',
      'product:unit-group:detail', 'product:unit-group:export', 'product:unit-group:list',
      'stock:inventory-mode:list', 'stock:inventory-mode:view', 'stock:view',
      'wms:category:detail', 'wms:category:list', 'wms:detail',
      'wms:export', 'wms:list', 'wms:location:detail',
      'wms:location:export', 'wms:location:list', 'wms:location:view',
      'wms:view', 'wms:warehouse:view'
  )
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = r.id AND rp.permission_id = p.id);
