-- 商品模块 E2E 专用验收账号（避免与并行会话共用 admin / e2e_linked_ui 互相踢下线）
-- 用法：python tools/dbq2.py < 本文件   （或 psql -f）
-- 说明：等权于 admin（role_id=1 SUPER_ADMIN），验收后可执行末尾清理语句。
DELETE FROM sys_user_role   WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_product');
DELETE FROM sys_user_tenant WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_product');
DELETE FROM sys_user        WHERE username = 'e2e_product';

-- 密码哈希复用 admin（即 admin123）
INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                      user_type, is_super_admin, is_tenant_admin, status, data_scope)
SELECT 2099000000000000888, 1, 0, now(), now(), 'e2e_product', password, 'E2E商品', 'E2E商品',
       1, false, false, 1, 'ALL'
FROM sys_user WHERE username = 'admin';

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
VALUES (2099000000000000889, 2099000000000000888, 1, 1, now());

INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000000890, 2099000000000000888, 1, true, 1, now(), now());

-- ⚠️ 清理（商品模块验收全部结束后执行）：
-- DELETE FROM sys_user_role   WHERE user_id = 2099000000000000888;
-- DELETE FROM sys_user_tenant WHERE user_id = 2099000000000000888;
-- DELETE FROM sys_user        WHERE id = 2099000000000000888;
