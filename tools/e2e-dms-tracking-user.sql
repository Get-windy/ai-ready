-- 配送跟踪（配送 → 配送跟踪 → 配送跟踪，菜单 80870 `dms:tracking`）E2E 专用账号与种子数据
-- 用法：每次跑 E2E 前整文件执行一次（重置 31 个轨迹点）。
--
-- 种子设计（便于断言，不是随意造数）：
--   · 配送员 A（**在线**：last_report_time 取「未来 2 小时」以在整轮验收期间稳定命中心跳口径
--       ——后端冷启动约 2.5 分钟、UI 段又要跑数分钟，写死 NOW() 会在中途过期导致断言抖动；
--       今日 09:00 起 20 点，纬度每点 +0.001° ≈ 111.2 米 → 19 段 ≈ 2.11 km）：
--       带任务、来源含 1(APP)/3(渠道)、末点速度 80 km/h（速度异常标红）
--   · 配送员 B（离线 status=0，昨日 10 点，来源 2(后台补录)，无任务）→ 用于昨日/离线/无任务过滤与 day 聚合
--   · 配送员 C（无任何轨迹点）→ 位置上报接口专用，保证脚本可重复执行
--   · 配送员 D（姓名**不含**「E2E跟踪」故不进任何计数断言；含 1 个 20 分钟前的停留点）
--       → 位置上报时触发 §3.5.5「异常停留」联动预警
--   · 轨迹 id 段 2099000000000006000+，配送员 2099000000000006010/6011/6012/6013，任务 2099000000000006100

-- ═══ ① 清理历史 ═══
-- 按 rider_id 清理：位置上报接口插入的点用自增 ID，不在下面的固定 ID 段内
DELETE FROM dms_tracking WHERE rider_id IN (2099000000000006010, 2099000000000006011, 2099000000000006012, 2099000000000006013);
DELETE FROM dms_tracking WHERE id BETWEEN 2099000000000006000 AND 2099000000000006299;
-- 联动预警按同名同 ID 清理，保证「§3.5.5 预警 + 去重」断言可重复执行
DELETE FROM dms_verification_alert WHERE rider_id IN (2099000000000006012, 2099000000000006013);
DELETE FROM dms_task       WHERE id = 2099000000000006100;
DELETE FROM dms_rider      WHERE id IN (2099000000000006010, 2099000000000006011, 2099000000000006012, 2099000000000006013);
DELETE FROM sys_user_role   WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_tracking');
DELETE FROM sys_user_tenant WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_tracking');
DELETE FROM sys_user        WHERE username = 'e2e_tracking';

-- ═══ ② 验收账号 ═══
INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                      user_type, is_super_admin, is_tenant_admin, status, data_scope)
SELECT 2099000000000007000, 1, 0, now(), now(), 'e2e_tracking', password, 'E2E跟踪', 'E2E跟踪',
       1, false, false, 1, 'ALL'
FROM sys_user WHERE username = 'admin';

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
VALUES (2099000000000007001, 2099000000000007000, 1, 1, now());

INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000007002, 2099000000000007000, 1, true, 1, now(), now());

-- ═══ ③ 配送员（A 在线 + 今日有上报；B 离线）═══
INSERT INTO dms_rider (id, tenant_id, rider_type, real_name, phone, status, verify_status,
                       current_lat, current_lng, last_report_time, deleted, create_time, update_time)
VALUES
 (2099000000000006010, 1, 1, 'E2E跟踪配送员A', '13900003001', 1, 1, 39.919000, 116.400000, NOW() + interval '2 hours', 0, now(), now()),
 (2099000000000006011, 1, 1, 'E2E跟踪配送员B', '13900003002', 0, 1, 39.954500, 116.450000, date_trunc('day', NOW()) - interval '1 day' + interval '10 hours', 0, now(), now()),
 (2099000000000006012, 1, 1, 'E2E跟踪配送员C', '13900003003', 0, 1, NULL, NULL, NULL, 0, now(), now()),
 (2099000000000006013, 1, 1, 'E2E预警配送员D', '13900003004', 0, 1, NULL, NULL, NULL, 0, now(), now());

-- ═══ ④ 任务（配送员 A 的关联任务）═══
INSERT INTO dms_task (id, tenant_id, task_no, order_no, customer_id, customer_name, customer_phone,
                      rider_id, rider_name, total_quantity, status, deleted, create_time, update_time, version)
VALUES (2099000000000006100, 1, 'E2ETRK-01', 'E2ETRKO-01', 1, 'E2E跟踪客户', '13800009999',
        2099000000000006010, 'E2E跟踪配送员A', 8, 4, 0, now(), now(), 1);

-- ═══ ⑤ 轨迹点：A 今日 20 点（纬度每点 +0.001°，末点速度 80 触发异常）═══
INSERT INTO dms_tracking (id, tenant_id, rider_id, task_id, lat, lng, speed, direction, accuracy, address,
                          report_time, source, deleted, create_time, update_time, version)
SELECT 2099000000000006000 + g, 1, 2099000000000006010, 2099000000000006100,
       39.900000 + g * 0.001, 116.400000,
       CASE WHEN g = 19 THEN 80 ELSE 20 + g END,
       (g * 18) % 360, 5.5, 'E2E跟踪地址' || g,
       date_trunc('day', NOW()) + interval '9 hours' + (g || ' minutes')::interval,
       CASE WHEN g % 5 = 0 THEN 3 ELSE 1 END, 0, now(), now(), 1
FROM generate_series(0, 19) AS g;

-- ═══ ⑥ 轨迹点：B 昨日 10 点（无任务、来源 2-后台补录）═══
INSERT INTO dms_tracking (id, tenant_id, rider_id, task_id, lat, lng, speed, direction, accuracy, address,
                          report_time, source, deleted, create_time, update_time, version)
SELECT 2099000000000006200 + g, 1, 2099000000000006011, NULL,
       39.950000 + g * 0.0005, 116.450000, 15, 180, 8.0, NULL,
       date_trunc('day', NOW()) - interval '1 day' + interval '10 hours' + (g || ' minutes')::interval,
       2, 0, now(), now(), 1
FROM generate_series(0, 9) AS g;

-- ═══ ⑦ 轨迹点：D 的停留基准点（20 分钟前，速度 0，**不挂任务**）═══
-- 位置上报接口用 NOW() 作为 report_time，故「异常停留」判定（默认 15 分钟未移动）只能靠这条历史点构造：
-- E2E 随后在同一坐标上报一次 → 时间差 ≥ 15 分钟且位移 < 50 米 → 触发 §3.5.5 联动预警（type=2）
-- ⚠️ task_id 必须为 NULL：D 不属于 E2ETRK-01 任务，挂上会让「按任务过滤=20」的断言变成 21
INSERT INTO dms_tracking (id, tenant_id, rider_id, task_id, lat, lng, speed, direction, accuracy, address,
                          report_time, source, deleted, create_time, update_time, version)
VALUES (2099000000000006300, 1, 2099000000000006013, NULL,
        39.930000, 116.420000, 0, 0, 6.0, 'E2E预警基准点',
        NOW() - interval '20 minutes', 1, 0, now(), now(), 1);

-- ═══ ⑧ 清理（验收结束后执行）═══
-- DELETE FROM dms_tracking WHERE id BETWEEN 2099000000000006000 AND 2099000000000006399;
-- DELETE FROM dms_verification_alert WHERE rider_id IN (2099000000000006012, 2099000000000006013);
-- DELETE FROM dms_task  WHERE id = 2099000000000006100;
-- DELETE FROM dms_rider WHERE id IN (2099000000000006010, 2099000000000006011, 2099000000000006012, 2099000000000006013);
-- DELETE FROM sys_user_role   WHERE user_id = 2099000000000007000;
-- DELETE FROM sys_user_tenant WHERE user_id = 2099000000000007000;
-- DELETE FROM sys_user        WHERE id = 2099000000000007000;
