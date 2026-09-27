-- 实时跟踪（配送 → 配送跟踪 → 实时跟踪，菜单 80740 `dms:realtime-tracking`）E2E 专用账号与种子数据
-- 用法：每次跑 E2E 前整文件执行一次（会重置 4 名配送员 + 轨迹点 + 1 个超时在途任务 + 在线阈值覆盖）。
--
-- 种子设计（每一类断言都有确定数据，不是随意造数）：
--   · O1 在线：今日 09:00 起 20 点（纬度每点 +0.001° ≈ 111.2 米 → 19 段 ≈ 2.11 km），其中 1 点速度 88 km/h（超速）
--              last_report_time = now() → 在线；挂任务 E2ERT-01（在途 + 已超 deadline）→ 超时在途 + 负载 1 + 车辆快照
--   · O2 在线：3 点同坐标、间隔 20 分钟（位移 <50 米）→ **异常停留**预警
--   · O3 在线：last_report_time = now() - 150 秒 → 用于验证**在线阈值配置化**（租户阈值置 3 分钟 → 在线；默认 2 分钟则离线）
--   · N1 无位置：无任何轨迹点、last_report_time 为空 → 无位置/离线
--   · 租户 1 覆盖 `dms.tracking.online.minutes = 3`（证明统计读的是配置而非硬编码）

-- ═══ ① 清理历史（按 rider_id 清，覆盖接口/自增 ID 写入的点）═══
DELETE FROM dms_tracking WHERE rider_id IN (2099000000000008010, 2099000000000008011, 2099000000000008012, 2099000000000008013);
DELETE FROM dms_tracking WHERE id BETWEEN 2099000000000008100 AND 2099000000000008299;
DELETE FROM dms_task       WHERE id = 2099000000000008200;
DELETE FROM dms_rider      WHERE id IN (2099000000000008010, 2099000000000008011, 2099000000000008012, 2099000000000008013);
DELETE FROM sys_user_role   WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_realtime');
DELETE FROM sys_user_tenant WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_realtime');
DELETE FROM sys_user        WHERE username = 'e2e_realtime';

-- ═══ ② 验收账号 ═══
INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                      user_type, is_super_admin, is_tenant_admin, status, data_scope)
SELECT 2099000000000008000, 1, 0, now(), now(), 'e2e_realtime', password, 'E2E实时', 'E2E实时',
       1, false, false, 1, 'ALL'
FROM sys_user WHERE username = 'admin';

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
VALUES (2099000000000008001, 2099000000000008000, 1, 1, now());

INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000008002, 2099000000000008000, 1, true, 1, now(), now());

-- ═══ ③ 配置覆盖（租户 1）：在线阈值 3 分钟（反证配置生效）+ 偏航阈值 + 合规项 ═══
INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time)
VALUES
 (1, 'dms.tracking.online.minutes',   '3',              '实时跟踪在线判定：最近上报在 N 分钟内视为在线（E2E 覆盖值）', 'TENANT', 0, now(), now()),
 (1, 'dms.tracking.deviation.meters', '1000',           '实时跟踪异常预警：偏航阈值(米)（E2E 覆盖值）',              'TENANT', 0, now(), now()),
 (1, 'dms.tracking.collect.hours',    '00:00-23:59:59', '实时跟踪合规：位置采集时段（E2E 覆盖值，脚本会临时改窄做反证）', 'TENANT', 0, now(), now()),
 (1, 'dms.tracking.retention.days',   '0',              '实时跟踪合规：轨迹保留天数(0=不自动清理)（E2E 覆盖值）',      'TENANT', 0, now(), now())
ON CONFLICT (tenant_id, config_key) WHERE deleted = 0 DO UPDATE
  SET config_value = EXCLUDED.config_value, update_time = CURRENT_TIMESTAMP;

-- ═══ ④ 配送员 ═══
INSERT INTO dms_rider (id, tenant_id, rider_type, rider_no, real_name, phone, status, verify_status,
                       current_lat, current_lng, last_report_time, deleted, create_time, update_time)
VALUES
 (2099000000000008010, 1, 1, 'RT0001', 'E2E实时骑手O1', '13900004001', 1, 1, 39.919000, 116.400000, now(),                        0, now(), now()),
 (2099000000000008011, 1, 1, 'RT0002', 'E2E实时骑手O2', '13900004002', 2, 1, 39.910000, 116.410000, now() - interval '30 seconds', 0, now(), now()),
 (2099000000000008012, 1, 1, 'RT0003', 'E2E实时骑手O3', '13900004003', 1, 1, 39.930000, 116.420000, now() - interval '150 seconds', 0, now(), now()),
 (2099000000000008013, 1, 1, 'RT0004', 'E2E实时骑手N1', '13900004004', 0, 1, NULL, NULL, NULL,                                     0, now(), now());

-- ═══ ⑤ 在途且已超时的任务（超时在途 + 在途负载 + 车辆快照 + 偏航基线）═══
--   source（39.90,116.40）→ customer（39.9087,116.3975）构成配送基线；
--   O1 最新位置（39.919,116.40）到该基线约 1159 米 > 阈值 1000 → 偏航预警
INSERT INTO dms_task (id, tenant_id, task_no, order_no, customer_id, customer_name, customer_phone,
                      source_lat, source_lng, customer_lat, customer_lng, rider_id, rider_name, vehicle_name, total_quantity,
                      status, deadline_time, deleted, create_time, update_time, version)
VALUES (2099000000000008200, 1, 'E2ERT-01', 'E2ERTO-01', 1, 'E2E实时客户', '13800008888',
        39.900000, 116.400000, 39.908700, 116.397500, 2099000000000008010, 'E2E实时骑手O1', '京A88888', 6,
        4, now() - interval '30 minutes', 0, now(), now(), 1);

-- ═══ ⑥ O1 今日 20 点（09:00 起，纬度每点 +0.001°；末点速度 88 → 超速）═══
INSERT INTO dms_tracking (id, tenant_id, rider_id, task_id, lat, lng, speed, direction, accuracy, address,
                          report_time, source, deleted, create_time, update_time, version)
SELECT 2099000000000008100 + g, 1, 2099000000000008010, 2099000000000008200,
       39.900000 + g * 0.001, 116.400000,
       CASE WHEN g = 19 THEN 88 ELSE 20 + g END,
       (g * 18) % 360, 5.0, 'E2E实时地址' || g,
       date_trunc('day', NOW()) + interval '9 hours' + (g || ' minutes')::interval,
       1, 0, now(), now(), 1
FROM generate_series(0, 19) AS g;

-- ═══ ⑦ O2 同坐标 3 点、间隔 20 分钟（位移 0 米 → 异常停留）═══
INSERT INTO dms_tracking (id, tenant_id, rider_id, task_id, lat, lng, speed, direction, accuracy, address,
                          report_time, source, deleted, create_time, update_time, version)
SELECT 2099000000000008130 + g, 1, 2099000000000008011, NULL,
       39.910000, 116.410000, 0, 90, 6.0, 'E2E实时停留点',
       date_trunc('day', NOW()) + interval '9 hours' + (g * 20 || ' minutes')::interval,
       1, 0, now(), now(), 1
FROM generate_series(0, 2) AS g;

-- ═══ ⑧ 清理（验收结束后执行）═══
-- DELETE FROM dms_tracking WHERE rider_id IN (2099000000000008010, 2099000000000008011, 2099000000000008012, 2099000000000008013);
-- DELETE FROM dms_task  WHERE id = 2099000000000008200;
-- DELETE FROM dms_rider WHERE id IN (2099000000000008010, 2099000000000008011, 2099000000000008012, 2099000000000008013);
-- DELETE FROM sys_user_role   WHERE user_id = 2099000000000008000;
-- DELETE FROM sys_user_tenant WHERE user_id = 2099000000000008000;
-- DELETE FROM sys_user        WHERE id = 2099000000000008000;
-- UPDATE dms_config SET config_value = '2' WHERE tenant_id = 1 AND config_key = 'dms.tracking.online.minutes';
