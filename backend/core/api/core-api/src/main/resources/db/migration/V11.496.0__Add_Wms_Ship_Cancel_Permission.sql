-- 补 `wms:ship:cancel` 权限码。
--
-- 背景：发货单「取消发货」前端早已定义并调用（api/wms/ship.ts → POST /wms/ship/cancel，
-- 页面 views/wh/shipping-order/form/index.vue），但后端 ShipController/ShipService 从未实现该端点，
-- 权限码也未登记 ⇒ 点按钮必然 404。本次补齐 ShipController./cancel 后同步登记种子。
--
-- 铁律：补 @SaCheckPermission 注解前必须先补权限码种子，否则该接口对所有非超管用户一律 403。
-- 幂等：按 permission_code 判重，重复执行安全。
--
-- ⚠️ 主键取值（2026-09-23 修正）：本迁移最初写死 107077，但该号已被 `wms:task:complete` 占用，
--    而 NOT EXISTS 只判了 permission_code、没判 id ⇒ 首次执行即
--    `重复键违反唯一约束 sys_permission_pkey`，Flyway 无成功记录 ⇒ **每次启动都重试失败、应用起不来**。
--    WMS 权限码占用 107000~107085 的连续段（107085 = wms:warehouse:view 为段内最大），
--    故改取段内下一个空号 107086。
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT 107086, 0, 0, 0, now(), now(), '仓储发货单取消', 'wms:ship:cancel', 3,
       '/api/wms/ship/cancel', 'POST', 107086, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p
                   WHERE p.permission_code = 'wms:ship:cancel');

-- 同步授权：凡已持有 `wms:ship:confirm`（确认发货）的角色，一并授予 `wms:ship:cancel`。
-- 背景：本迁移执行前 D3 的仓储权限批量授权已跑过一轮，那时库中还没有本权限码，
-- 若不在此补齐，重启后新接口对**所有非超管**都是 403（超管有 `*` 通配不受影响）。
-- 只跟 `confirm` 走：只读角色（部门管理员）没有 confirm，不会被误授取消权。
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time, create_by)
SELECT (SELECT COALESCE(max(id), 0) FROM sys_role_permission) + row_number() OVER (ORDER BY rp.role_id),
       rp.role_id, pnew.id, 0, now(), 1
FROM sys_role_permission rp
JOIN sys_permission pold ON pold.id = rp.permission_id
                        AND pold.permission_code = 'wms:ship:confirm'
CROSS JOIN (SELECT id FROM sys_permission WHERE permission_code = 'wms:ship:cancel') pnew
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission x
                  WHERE x.role_id = rp.role_id AND x.permission_id = pnew.id);
