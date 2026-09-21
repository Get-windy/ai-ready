-- =============================================================================
-- 清理两批「代码已不存在、权限码仍留在表里」的历史权限码（2026-09-20）
--
-- 【背景】sys_permission 是数据表，权限码一旦插入就不会随代码删除而消失。
--   本次清理的两个命名空间：
--
--   · purchase_order:*（7 条，由 V11.423.0__Seed_Missing_Permission_Codes.sql 插入）
--     属于旧的采购订单实现 cn.aiedge.erp.order.OrderController
--     （路径 /api/erp/purchase-orders，与 erp-purchase 模块的实现重复且映射同一张表
--      erp_purchase_order）。该 Controller 已于 2026-09-20 整包删除，
--     其中唯一被使用的统计能力迁到 GET /api/erp/purchase/order/statistics，
--     权限码体系随之收敛到 purchase:order:*。
--
--   · hr-recruitment:*（3 条，由 V11.36.0 插入、V11.380.0 又补了一遍并自行标注「历史码」）
--     招聘模块重构后权限码归入 hr:* 命名空间，后端已无任何引用。
--
-- 【删除前的核查证据】
--   · 全库检索这两个命名空间（*.java / *.ts / *.vue / *.xml）：
--     除前端权限矩阵里的中文名映射外，**没有任何 @SaCheckPermission、v-permission 或接口引用**
--     —— 即这些码没有任何消费方，勾给角色也不会控制任何功能。
--   · 不由 Java 初始化（PermissionInitializationConfig 无登记），
--     删除后不会在应用启动时被重新插入；上述两个 seed 迁移已执行过，Flyway 不会重跑。
--
-- ⚠️ 本迁移不可逆：如需恢复，按本文件下方列出的 10 个 permission_code 重建即可
--   （但要注意：对应的接口已经不存在，重建也恢复不了任何功能）。
-- =============================================================================

-- 先删角色关联，再删权限码本身
DELETE FROM sys_role_permission
 WHERE permission_id IN (
     SELECT id FROM sys_permission
      WHERE permission_code IN (
            'purchase_order:approve', 'purchase_order:create', 'purchase_order:delete',
            'purchase_order:execute', 'purchase_order:export', 'purchase_order:submit',
            'purchase_order:update',
            'hr-recruitment:add', 'hr-recruitment:edit', 'hr-recruitment:delete'
      )
 );

DELETE FROM sys_permission
 WHERE permission_code IN (
       'purchase_order:approve', 'purchase_order:create', 'purchase_order:delete',
       'purchase_order:execute', 'purchase_order:export', 'purchase_order:submit',
       'purchase_order:update',
       'hr-recruitment:add', 'hr-recruitment:edit', 'hr-recruitment:delete'
 );
