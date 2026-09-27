-- 签收管理（配送 → 配送跟踪 → 签收管理，菜单 80900 `dms:sign`）E2E 专用账号与种子数据
-- 用法：python tools/dbq.py 执行本文件（或 psycopg2 整文件执行）；**每次跑 E2E 前重新执行一次**（会重置种子任务状态）。
--
-- 说明：
--   · 验收账号复用 admin 的密码哈希（admin123），角色 1(SUPER_ADMIN) → 权限码 '*'，可过 @SaCheckPermission；
--   · 种子任务 task_no 前缀固定 `E2ESIGN-`，E2E 脚本按此前缀反查任务，无需硬编码雪花 ID；
--   · 全部任务置 status=4（配送中）——签收提交的前置条件；
--   · 额外插一条**全局阈值配置** `dms.sign.deviation.threshold = 200`，用于验证「偏差阈值配置化」（§3.6.2）
--     生效：150 米偏差应判为「正常」，若回落默认 100 米则会误判超阈值。

-- ═══ ① 清理历史（含上一轮验收残留）═══
DELETE FROM dms_sign WHERE task_id IN (SELECT id FROM dms_task WHERE task_no LIKE 'E2ESIGN-%');
DELETE FROM dms_task WHERE task_no LIKE 'E2ESIGN-%';
DELETE FROM sys_user_role   WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_sign');
DELETE FROM sys_user_tenant WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_sign');
DELETE FROM sys_user        WHERE username = 'e2e_sign';
DELETE FROM dms_rider       WHERE id = 2099000000000005010;
DELETE FROM dms_config      WHERE config_key = 'dms.sign.deviation.threshold' AND id = 2099000000000005020;

-- ═══ ② 验收账号 ═══
INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                      user_type, is_super_admin, is_tenant_admin, status, data_scope)
SELECT 2099000000000005000, 1, 0, now(), now(), 'e2e_sign', password, 'E2E签收', 'E2E签收',
       1, false, false, 1, 'ALL'
FROM sys_user WHERE username = 'admin';

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
VALUES (2099000000000005001, 2099000000000005000, 1, 1, now());

INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000005002, 2099000000000005000, 1, true, 1, now(), now());

-- ═══ ③ 配送员（任务上的名称快照 + 电话联查）═══
INSERT INTO dms_rider (id, tenant_id, rider_type, real_name, phone, status, verify_status, deleted, create_time, update_time)
VALUES (2099000000000005010, 1, 1, 'E2E签收配送员', '13900002001', 1, 1, 0, now(), now());

-- ═══ ④ 全局偏差阈值配置（200 米，用于验证配置化）═══
INSERT INTO dms_config (id, tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time)
VALUES (2099000000000005020, 0, 'dms.sign.deviation.threshold', '200',
        '签收定位偏差阈值(米)：超出仅标记待复核，不拒绝签收', 'global', 0, now(), now());

-- ═══ ⑤ 6 个「配送中(4)」任务（客户坐标 = 北京天安门 39.9087,116.3975）═══
INSERT INTO dms_task (id, tenant_id, task_no, order_no, source_bill_no, customer_id, customer_name, customer_phone,
                      customer_lat, customer_lng, rider_id, rider_name, total_items, total_quantity,
                      collect_on_delivery, delivery_fee, status, deleted, create_time, update_time, version)
VALUES
 (2099000000000005101, 1, 'E2ESIGN-01', 'E2ESO-01', 'XSCKD-E2E-01', 1, 'E2E签收客户甲', '13800001111', 39.908700, 116.397500, 2099000000000005010, 'E2E签收配送员', 2, 10, 200.00, 15.00, 4, 0, now(), now(), 1),
 (2099000000000005102, 1, 'E2ESIGN-02', 'E2ESO-02', 'XSCKD-E2E-02', 1, 'E2E签收客户乙', '13800002222', 39.908700, 116.397500, 2099000000000005010, 'E2E签收配送员', 3, 20, 0.00, 12.00, 4, 0, now(), now(), 1),
 (2099000000000005103, 1, 'E2ESIGN-03', 'E2ESO-03', 'XSCKD-E2E-03', 1, 'E2E签收客户丙', '13800003333', 39.908700, 116.397500, 2099000000000005010, 'E2E签收配送员', 1, 5, 0.00, 8.00, 4, 0, now(), now(), 1),
 (2099000000000005104, 1, 'E2ESIGN-04', 'E2ESO-04', 'XSCKD-E2E-04', 1, 'E2E签收客户丁', '13800004444', 39.908700, 116.397500, 2099000000000005010, 'E2E签收配送员', 4, 30, 500.00, 20.00, 4, 0, now(), now(), 1),
 (2099000000000005105, 1, 'E2ESIGN-05', 'E2ESO-05', 'XSCKD-E2E-05', 1, 'E2E签收客户戊', '13800005555', 39.908700, 116.397500, 2099000000000005010, 'E2E签收配送员', 1, 6, 0.00, 9.00, 4, 0, now(), now(), 1),
 (2099000000000005106, 1, 'E2ESIGN-06', 'E2ESO-06', 'XSCKD-E2E-06', 1, 'E2E签收客户己', '13800006666', 39.908700, 116.397500, 2099000000000005010, 'E2E签收配送员', 2, 12, 0.00, 11.00, 4, 0, now(), now(), 1);

-- ═══ ⑥ 清理（验收结束后执行）═══
-- DELETE FROM dms_sign WHERE task_id IN (SELECT id FROM dms_task WHERE task_no LIKE 'E2ESIGN-%');
-- DELETE FROM dms_task WHERE task_no LIKE 'E2ESIGN-%';
-- DELETE FROM sys_user_role   WHERE user_id = 2099000000000005000;
-- DELETE FROM sys_user_tenant WHERE user_id = 2099000000000005000;
-- DELETE FROM sys_user        WHERE id = 2099000000000005000;
-- DELETE FROM dms_rider       WHERE id = 2099000000000005010;
-- DELETE FROM dms_config      WHERE id = 2099000000000005020;
