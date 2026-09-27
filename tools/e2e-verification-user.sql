-- 实名认证（配送 → 人车管理 → 实名认证，80840）E2E 专用验收账号
-- 避免与并行会话共用 admin 被 sa-token 互踢（页面会伪装成「跳登录、断言全空」的假失败）
-- 用法：python tools/dbq.py "<本文件的 INSERT 语句>"；验收结束后执行文件末尾清理语句。
DELETE FROM sys_user_role   WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_verif');
DELETE FROM sys_user_tenant WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_verif');
DELETE FROM sys_user        WHERE username = 'e2e_verif';

INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                      user_type, is_super_admin, is_tenant_admin, status, data_scope)
SELECT 2099000000000001988, 1, 0, now(), now(), 'e2e_verif', password, 'E2E实名', 'E2E实名',
       1, false, false, 1, 'ALL'
FROM sys_user WHERE username = 'admin';

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
VALUES (2099000000000001989, 2099000000000001988, 1, 1, now());

INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000001990, 2099000000000001988, 1, true, 1, now(), now());

-- ⚠️ 清理（验收结束后执行）：
-- DELETE FROM sys_user_role   WHERE user_id = 2099000000000001988;
-- DELETE FROM sys_user_tenant WHERE user_id = 2099000000000001988;
-- DELETE FROM sys_user        WHERE id = 2099000000000001988;

-- ⚠️ 可选：清理历次验收产生的配送员与实名认证台账
-- （e2e-verification.cjs 每轮会新建「E2E实名/E2E平台/E2E驳回」前缀的配送员，保证可重复执行）
-- DELETE FROM dms_rider_certificate  WHERE rider_id IN (SELECT id FROM dms_rider WHERE real_name LIKE 'E2E实名%' OR real_name LIKE 'E2E平台%' OR real_name LIKE 'E2E驳回%');
-- DELETE FROM dms_rider_verification WHERE rider_id IN (SELECT id FROM dms_rider WHERE real_name LIKE 'E2E实名%' OR real_name LIKE 'E2E平台%' OR real_name LIKE 'E2E驳回%');
-- DELETE FROM dms_rider_vehicle_binding WHERE rider_id IN (SELECT id FROM dms_rider WHERE real_name LIKE 'E2E实名%' OR real_name LIKE 'E2E平台%' OR real_name LIKE 'E2E驳回%');
-- DELETE FROM dms_rider WHERE real_name LIKE 'E2E实名%' OR real_name LIKE 'E2E平台%' OR real_name LIKE 'E2E驳回%';
