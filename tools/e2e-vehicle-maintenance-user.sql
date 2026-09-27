-- 车辆维护（配送 → 人车管理 → 车辆维护）E2E 专用验收账号
-- 避免与并行会话共用 admin 被 sa-token 互踢（页面会伪装成"跳登录、断言全空"的假失败）
-- 用法：python tools/dbq2.py "<本文件的 INSERT 语句>"；验收结束后执行文件末尾清理语句。
DELETE FROM sys_user_role   WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_vehmaint')
                               OR id = 2099000000000000986;
DELETE FROM sys_user_tenant WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_vehmaint')
                               OR id = 2099000000000000987;
DELETE FROM sys_user        WHERE username = 'e2e_vehmaint' OR id = 2099000000000000985;

-- 密码哈希复用 admin（即 admin123）
INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                      user_type, is_super_admin, is_tenant_admin, status, data_scope)
SELECT 2099000000000000985, 1, 0, now(), now(), 'e2e_vehmaint', password, 'E2E维保', 'E2E维保',
       1, false, false, 1, 'ALL'
FROM sys_user WHERE username = 'admin';

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
VALUES (2099000000000000986, 2099000000000000985, 1, 1, now());

INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000000987, 2099000000000000985, 1, true, 1, now(), now());

-- ── 维保厂商选择器数据源：一条 E2E 专用「供应商」往来单位（生产级：厂商引用 biz_party，不新建厂商表） ──
DELETE FROM biz_party WHERE id = 2099000000000000301 OR party_code = 'E2EVENDOR';
INSERT INTO biz_party (id, tenant_id, party_code, party_name, party_type, status, deleted, create_time, update_time)
VALUES (2099000000000000301, 1, 'E2EVENDOR', 'E2E维保服务商', 2, 1, 0, now(), now());

-- ⚠️ 清理（验收结束后执行）：
-- DELETE FROM sys_user_role   WHERE user_id = 2099000000000000985;
-- DELETE FROM sys_user_tenant WHERE user_id = 2099000000000000985;
-- DELETE FROM sys_user        WHERE id = 2099000000000000985;
-- DELETE FROM biz_party       WHERE id = 2099000000000000301;
