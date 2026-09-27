-- 司机端签收（配送 Tab → 配送详情 → 签收/收款）E2E 专用账号与种子数据
-- 用法（每次跑 E2E 前重新执行一次，会重置种子任务状态）：
--   python - tools/dbq.py 不接收整文件；用 psycopg2 整文件执行：
--   python -c "import psycopg2;c=psycopg2.connect('host=localhost port=5432 dbname=devdb user=devuser password=devuser123');c.autocommit=True;c.cursor().execute(open('tools/e2e-driver-sign-user.sql',encoding='utf-8').read());c.close()"
--
-- 说明：
--   · 验收账号 `e2e_dsign` 复用 admin 的密码哈希（admin123）+ 角色 1(SUPER_ADMIN) → 权限码 '*'；
--   · **必须**把 dms_rider.user_id 指向该账号：司机端「我的任务」是按登录人反查配送员档案的
--     （`GET /api/dms/verification/me/rider`），不绑定就一条任务都查不到；
--   · 任务编号前缀固定 `E2EDRV-`，脚本按前缀反查，不硬编码雪花 ID；
--   · 客户坐标 = 北京天安门 39.9087,116.3975；司机端签收不回传客户坐标，
--     由服务端回落到任务快照算偏差（本次改进点），故偏差必须 > 0 才能证明回落生效。

-- ═══ ① 清理历史 ═══
DELETE FROM dms_sign WHERE task_id IN (SELECT id FROM dms_task WHERE task_no LIKE 'E2EDRV-%');
DELETE FROM dms_payment WHERE task_id IN (SELECT id FROM dms_task WHERE task_no LIKE 'E2EDRV-%');
DELETE FROM dms_task WHERE task_no LIKE 'E2EDRV-%';
DELETE FROM sys_user_role   WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_dsign');
DELETE FROM sys_user_tenant WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_dsign');
DELETE FROM sys_user        WHERE username = 'e2e_dsign';
DELETE FROM dms_rider       WHERE id = 2099000000000007010;

-- ═══ ② 验收账号 ═══
INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                      user_type, is_super_admin, is_tenant_admin, status, data_scope)
SELECT 2099000000000007000, 1, 0, now(), now(), 'e2e_dsign', password, 'E2E司机签收', 'E2E司机签收',
       1, false, false, 1, 'ALL'
FROM sys_user WHERE username = 'admin';

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
VALUES (2099000000000007001, 2099000000000007000, 1, 1, now());

INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000007002, 2099000000000007000, 1, true, 1, now(), now());

-- ═══ ③ 配送员档案（user_id 绑定登录账号，司机端据此反查）═══
INSERT INTO dms_rider (id, tenant_id, user_id, rider_type, real_name, phone, status, verify_status, deleted, create_time, update_time)
VALUES (2099000000000007010, 1, 2099000000000007000, 1, 'E2E司机配送员', '13900003001', 1, 1, 0, now(), now());

-- ═══ ④ 任务 ═══
-- 7101 配送中 → 正常签收（签名 + 定位，客户坐标不回传 → 服务端回落）
-- 7102 配送中 → 部分签收（实际数量）
-- 7103 配送中 → 拒收（照片 + 原因）
-- 7104 配送中 → 幂等 / 驳回后重新签收
-- 7105 已分配 → 状态机 1→2
-- 7106 配送中 → 收款（有代收货款）
-- 7107 配送中 → UI 全流程（选部分签收 → 填数量 → 画签名 → 提交）
-- 7108 已分配 → API 状态机（1→2→3→4 合法 / 非法迁移被拒）
INSERT INTO dms_task (id, tenant_id, task_no, order_no, source_bill_no, customer_id, customer_name, customer_phone,
                      customer_address, customer_lat, customer_lng, rider_id, rider_name, total_items, total_quantity,
                      collect_on_delivery, delivery_fee, status, deleted, create_time, update_time, version)
VALUES
 (2099000000000007101, 1, 'E2EDRV-01', 'E2EDRO-01', 'XSCKD-DRV-01', 1, 'E2E司机客户甲', '13700001111', '北京市东城区东长安街1号', 39.908700, 116.397500, 2099000000000007010, 'E2E司机配送员', 2, 10,   0.00, 15.00, 4, 0, now(), now(), 1),
 (2099000000000007102, 1, 'E2EDRV-02', 'E2EDRO-02', 'XSCKD-DRV-02', 1, 'E2E司机客户乙', '13700002222', '北京市东城区东长安街2号', 39.908700, 116.397500, 2099000000000007010, 'E2E司机配送员', 3, 20,   0.00, 12.00, 4, 0, now(), now(), 1),
 (2099000000000007103, 1, 'E2EDRV-03', 'E2EDRO-03', 'XSCKD-DRV-03', 1, 'E2E司机客户丙', '13700003333', '北京市东城区东长安街3号', 39.908700, 116.397500, 2099000000000007010, 'E2E司机配送员', 1,  5,   0.00,  8.00, 4, 0, now(), now(), 1),
 (2099000000000007104, 1, 'E2EDRV-04', 'E2EDRO-04', 'XSCKD-DRV-04', 1, 'E2E司机客户丁', '13700004444', '北京市东城区东长安街4号', 39.908700, 116.397500, 2099000000000007010, 'E2E司机配送员', 4, 30,   0.00, 20.00, 4, 0, now(), now(), 1),
 (2099000000000007105, 1, 'E2EDRV-05', 'E2EDRO-05', 'XSCKD-DRV-05', 1, 'E2E司机客户戊', '13700005555', '北京市东城区东长安街5号', 39.908700, 116.397500, 2099000000000007010, 'E2E司机配送员', 1,  6,   0.00,  9.00, 1, 0, now(), now(), 1),
 (2099000000000007106, 1, 'E2EDRV-06', 'E2EDRO-06', 'XSCKD-DRV-06', 1, 'E2E司机客户己', '13700006666', '北京市东城区东长安街6号', 39.908700, 116.397500, 2099000000000007010, 'E2E司机配送员', 2, 12, 500.00, 11.00, 4, 0, now(), now(), 1),
 (2099000000000007107, 1, 'E2EDRV-07', 'E2EDRO-07', 'XSCKD-DRV-07', 1, 'E2E司机客户庚', '13700007777', '北京市东城区东长安街7号', 39.908700, 116.397500, 2099000000000007010, 'E2E司机配送员', 3, 30,   0.00, 14.00, 4, 0, now(), now(), 1),
 (2099000000000007108, 1, 'E2EDRV-08', 'E2EDRO-08', 'XSCKD-DRV-08', 1, 'E2E司机客户辛', '13700008888', '北京市东城区东长安街8号', 39.908700, 116.397500, 2099000000000007010, 'E2E司机配送员', 1,  4,   0.00,  7.00, 1, 0, now(), now(), 1);

-- ═══ ⑤ 清理（验收结束后执行）═══
-- DELETE FROM dms_sign WHERE task_id IN (SELECT id FROM dms_task WHERE task_no LIKE 'E2EDRV-%');
-- DELETE FROM dms_payment WHERE task_id IN (SELECT id FROM dms_task WHERE task_no LIKE 'E2EDRV-%');
-- DELETE FROM dms_task WHERE task_no LIKE 'E2EDRV-%';
-- DELETE FROM sys_user_role   WHERE user_id = 2099000000000007000;
-- DELETE FROM sys_user_tenant WHERE user_id = 2099000000000007000;
-- DELETE FROM sys_user        WHERE id = 2099000000000007000;
-- DELETE FROM dms_rider       WHERE id = 2099000000000007010;
