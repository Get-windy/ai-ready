-- 车辆管理（配送 → 人车管理 → 车辆管理，dms:vehicle）E2E 专用验收账号与运力数据
-- 目的：避免与并行会话共用 admin 被 sa-token 互踢（页面会伪装成「跳登录、断言全空」的假失败）
-- 用法：python tools/dbq.py "<本文件语句>"；验收结束后执行文件末尾清理语句。
--
-- 说明：
--   · 验收账号复用 admin 的密码哈希（admin123），角色 = 1(SUPER_ADMIN) → 权限码 '*'，可过 @SaCheckPermission；
--   · 配送员写 dms_rider（人车绑定所需）：A/B 审核通过（verify_status=1）可指派，C 未审核用于验证「仅可指派」过滤。

-- ═══ ① 清理历史 ═══
DELETE FROM sys_user_role   WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_vehicle');
DELETE FROM sys_user_tenant WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_vehicle');
DELETE FROM sys_user        WHERE username = 'e2e_vehicle';
DELETE FROM dms_rider       WHERE phone IN ('13900000001', '13900000002', '13900000003');

-- ═══ ② 验收账号 ═══
INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                      user_type, is_super_admin, is_tenant_admin, status, data_scope)
SELECT 2099000000000001100, 1, 0, now(), now(), 'e2e_vehicle', password, 'E2E车辆', 'E2E车辆',
       1, false, false, 1, 'ALL'
FROM sys_user WHERE username = 'admin';

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
VALUES (2099000000000001101, 2099000000000001100, 1, 1, now());

INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000001102, 2099000000000001100, 1, true, 1, now(), now());

-- ═══ ③ 运力（配送员）数据 ═══
INSERT INTO dms_rider (id, tenant_id, rider_type, real_name, phone, status, verify_status, deleted, create_time, update_time)
VALUES
 (2099000000000001110, 1, 1, 'E2E配送员A', '13900000001', 1, 1, 0, now(), now()),
 (2099000000000001111, 1, 1, 'E2E配送员B', '13900000002', 1, 1, 0, now(), now()),
 (2099000000000001112, 1, 1, 'E2E未审核C', '13900000003', 0, 0, 0, now(), now());

-- ═══ ④ 清理（验收结束后执行） ═══
-- DELETE FROM sys_user_role   WHERE user_id = 2099000000000001100;
-- DELETE FROM sys_user_tenant WHERE user_id = 2099000000000001100;
-- DELETE FROM sys_user        WHERE id = 2099000000000001100;
-- DELETE FROM dms_rider       WHERE id IN (2099000000000001110, 2099000000000001111, 2099000000000001112);
