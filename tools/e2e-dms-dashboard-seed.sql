-- 配送仪表盘 E2E 测试数据（租户 1，幂等：先删后插）
-- 覆盖：4 种配送员状态 / 4 种车辆状态 / 9 种任务状态 + 准时与超时样本 / 人车绑定 / 核验预警 / 收款
-- 用法：python tools/dbq2.py "$(cat tools/e2e-dms-dashboard-seed.sql | grep -v '^--')"

DELETE FROM dms_payment               WHERE id BETWEEN 2099270000000000000 AND 2099279999999999999;
DELETE FROM dms_verification_alert    WHERE id BETWEEN 2099260000000000000 AND 2099269999999999999;
DELETE FROM dms_rider_vehicle_binding WHERE id BETWEEN 2099250000000000000 AND 2099259999999999999;
DELETE FROM dms_task                  WHERE id BETWEEN 2099240000000000000 AND 2099249999999999999;
DELETE FROM dms_rider                 WHERE id BETWEEN 2099230000000000000 AND 2099239999999999999;
DELETE FROM dms_vehicle               WHERE id BETWEEN 2099220000000000000 AND 2099229999999999999;
DELETE FROM dms_channel               WHERE id BETWEEN 2099210000000000000 AND 2099219999999999999;

-- ── 运力渠道 ──
INSERT INTO dms_channel (id, tenant_id, channel_code, channel_name, channel_type, adapter_bean, status, priority, sort_order, deleted, create_time, update_time, version) VALUES
(2099210000000000001, 1, 'E2E-DADA', 'E2E达达', 1, 'dadaAdapter',  1, 1, 1, 0, now(), now(), 1),
(2099210000000000002, 1, 'E2E-MT',   'E2E美团', 2, 'meituanAdapter', 1, 2, 2, 0, now(), now(), 1);

-- ── 车辆（覆盖 使用中/空闲/已出勤/维修中） ──
INSERT INTO dms_vehicle (id, tenant_id, vehicle_code, plate_no, brand, model, vehicle_type, status, ownership_type, gps_enabled, current_mileage, deleted, create_time, update_time, version) VALUES
(2099220000000000001, 1, 'E2E-V001', '京A12345', '比亚迪', 'T5',  1, 1, 1, 1, 12000, 0, now(), now(), 1),
(2099220000000000002, 1, 'E2E-V002', '京B23456', '五菱',   '荣光', 3, 0, 1, 1,  8000, 0, now(), now(), 1),
(2099220000000000003, 1, 'E2E-V003', '京C34567', '东风',   'K01',  2, 4, 1, 1, 33000, 0, now(), now(), 1),
(2099220000000000004, 1, 'E2E-V004', '京D45678', '解放',   'J6',   4, 2, 1, 0, 56000, 0, now(), now(), 1);

-- ── 配送员（覆盖 空闲/忙碌/离线 + 在线阈值边界） ──
INSERT INTO dms_rider (id, tenant_id, user_id, rider_type, real_name, phone, vehicle_type, vehicle_no, channel_id, service_radius, last_report_time, status, verify_status, rating_score, total_orders, completed_orders, deposit_amount, max_concurrent, work_hours_start, work_hours_end, deleted, create_time, update_time, version) VALUES
(2099230000000000001, 1, NULL, 1, 'E2E张小明', '13800000001', '电动车', '京A12345', NULL, 5, now() - interval '1 minute',  1, 1, 4.80, 120, 115, 0, 5, '08:00', '20:00', 0, now(), now(), 1),
(2099230000000000002, 1, NULL, 1, 'E2E李快跑', '13800000002', '三轮车', '京C34567', NULL, 8, now() - interval '30 minutes', 2, 1, 4.60,  90,  85, 0, 5, '08:00', '20:00', 0, now(), now(), 1),
(2099230000000000003, 1, NULL, 2, 'E2E王众包', '13800000003', '电动车', '京B23456', NULL, 3, now() - interval '1 day',     0, 1, 4.20,  40,  38, 0, 3, '09:00', '18:00', 0, now(), now(), 1),
(2099230000000000004, 1, NULL, 3, 'E2E赵平台', '13800000004', '小货车', '京D45678', 2099210000000000001, 10, now() - interval '1 minute', 1, 1, 4.95, 200, 196, 0, 8, '07:00', '22:00', 0, now(), now(), 1);

-- ── 配送任务（今日 9 条覆盖 9 态 + 历史样本） ──
-- 今日基准 = date_trunc('day', now())；准时口径 = completed_time <= deadline_time
INSERT INTO dms_task (id, tenant_id, task_no, order_type, channel_id, rider_id, dispatch_type, customer_name, customer_phone, customer_address,
                      total_items, goods_amount, delivery_fee, collect_on_delivery, priority, status,
                      pickup_time, completed_time, deadline_time, deleted, create_time, update_time, version) VALUES
-- 今日
(2099240000000000001, 1, 'E2E-TSK-001', 1, 2099210000000000001, NULL, NULL, 'E2E客户A', '13800000011', '北京市朝阳区A', 3, 500, 10,   0, 1, 0, NULL, NULL, now() - interval '35 minutes', 0, date_trunc('day', now()) + interval '9 hours', now(), 1),
(2099240000000000002, 1, 'E2E-TSK-002', 1, 2099210000000000001, 2099230000000000001, 2, 'E2E客户B', '13800000012', '北京市海淀区B', 2, 300,  8,   0, 1, 1, NULL, NULL, now() - interval '30 minutes', 0, date_trunc('day', now()) + interval '9 hours', now(), 1),
(2099240000000000003, 1, 'E2E-TSK-003', 1, 2099210000000000002, 2099230000000000001, 2, 'E2E客户C', '13800000013', '北京市西城区C', 1, 150,  6,   0, 2, 2, NULL, NULL, now() - interval '25 minutes', 0, date_trunc('day', now()) + interval '9 hours', now(), 1),
(2099240000000000004, 1, 'E2E-TSK-004', 1, 2099210000000000001, 2099230000000000002, 1, 'E2E客户D', '13800000014', '北京市东城区D', 4, 800, 12,   0, 1, 3, date_trunc('day', now()) + interval '10 hours', NULL, now() - interval '20 minutes', 0, date_trunc('day', now()) + interval '9 hours', now(), 1),
(2099240000000000005, 1, 'E2E-TSK-005', 2, 2099210000000000002, 2099230000000000002, 1, 'E2E客户E', '13800000015', '北京市丰台区E', 6, 1200, 20,   0, 2, 4, date_trunc('day', now()) + interval '10 hours', NULL, now() - interval '15 minutes', 0, date_trunc('day', now()) + interval '9 hours', now(), 1),
(2099240000000000006, 1, 'E2E-TSK-006', 1, 2099210000000000001, 2099230000000000001, 2, 'E2E客户F', '13800000016', '北京市通州区F', 2, 600, 10, 100, 1, 5, date_trunc('day', now()) + interval '9 hours',  date_trunc('day', now()) + interval '9 hours 30 minutes', date_trunc('day', now()) + interval '10 hours', 0, date_trunc('day', now()) + interval '8 hours', now(), 1),
(2099240000000000007, 1, 'E2E-TSK-007', 1, 2099210000000000001, 2099230000000000002, 3, 'E2E客户G', '13800000017', '北京市昌平区G', 3, 900, 14,  50, 3, 6, date_trunc('day', now()) + interval '10 hours', date_trunc('day', now()) + interval '11 hours', date_trunc('day', now()) + interval '10 hours 30 minutes', 0, date_trunc('day', now()) + interval '8 hours', now(), 1),
(2099240000000000008, 1, 'E2E-TSK-008', 3, NULL,                 NULL, NULL, 'E2E客户H', '13800000018', '北京市大兴区H', 1, 200,  0,   0, 1, 7, NULL, NULL, NULL, 0, date_trunc('day', now()) + interval '7 hours', now(), 1),
(2099240000000000009, 1, 'E2E-TSK-009', 1, 2099210000000000002, 2099230000000000003, 1, 'E2E客户I', '13800000019', '北京市顺义区I', 2, 400, 11,   0, 2, 8, NULL, NULL, date_trunc('day', now()) + interval '12 hours', 0, date_trunc('day', now()) + interval '7 hours', now(), 1),
-- 昨日
(2099240000000000010, 1, 'E2E-TSK-010', 1, 2099210000000000001, 2099230000000000001, 1, 'E2E客户J', '13800000020', '北京市朝阳区J', 2, 500,  8,   0, 1, 5, date_trunc('day', now()) - interval '14 hours', date_trunc('day', now()) - interval '13 hours 20 minutes', date_trunc('day', now()) - interval '13 hours', 0, date_trunc('day', now()) - interval '15 hours', now(), 1),
(2099240000000000011, 1, 'E2E-TSK-011', 1, 2099210000000000002, 2099230000000000003, 1, 'E2E客户K', '13800000021', '北京市海淀区K', 5, 1500, 15,   0, 1, 6, date_trunc('day', now()) - interval '12 hours', date_trunc('day', now()) - interval '10 hours 40 minutes', date_trunc('day', now()) - interval '10 hours', 0, date_trunc('day', now()) - interval '13 hours', now(), 1),
-- 3 天前
(2099240000000000012, 1, 'E2E-TSK-012', 2, 2099210000000000001, 2099230000000000001, 1, 'E2E客户L', '13800000022', '北京市石景山区L', 1, 300, 20,   0, 1, 6, date_trunc('day', now()) - interval '3 days 13 hours', date_trunc('day', now()) - interval '3 days 11 hours', date_trunc('day', now()) - interval '3 days 12 hours', 0, date_trunc('day', now()) - interval '3 days 14 hours', now(), 1),
-- 5 天前
(2099240000000000013, 1, 'E2E-TSK-013', 1, 2099210000000000001, 2099230000000000004, 1, 'E2E客户M', '13800000023', '北京市门头沟区M', 3, 700,  9,   0, 1, 5, date_trunc('day', now()) - interval '5 days 13 hours', date_trunc('day', now()) - interval '5 days 12 hours', date_trunc('day', now()) - interval '5 days 12 hours 30 minutes', 0, date_trunc('day', now()) - interval '5 days 14 hours', now(), 1);

-- ── 人车绑定（2 条绑定中 + 1 条已交车） ──
INSERT INTO dms_rider_vehicle_binding (id, tenant_id, rider_id, rider_name, rider_phone, vehicle_id, plate_no, bind_time, bind_mileage, handover_time, handover_mileage, bind_reason, status, deleted, create_time, update_time, version) VALUES
(2099250000000000001, 1, 2099230000000000001, 'E2E张小明', '13800000001', 2099220000000000001, '京A12345', date_trunc('day', now()) + interval '8 hours', 12000, NULL, NULL, 'E2E 早班出车', 0, 0, now(), now(), 1),
(2099250000000000002, 1, 2099230000000000002, 'E2E李快跑', '13800000002', 2099220000000000003, '京C34567', date_trunc('day', now()) + interval '8 hours 30 minutes', 33000, NULL, NULL, 'E2E 早班出车', 0, 0, now(), now(), 1),
(2099250000000000003, 1, 2099230000000000003, 'E2E王众包', '13800000003', 2099220000000000002, '京B23456', date_trunc('day', now()) - interval '1 day 8 hours', 8000, date_trunc('day', now()) - interval '1 day 1 hour', 8040, 'E2E 昨日出车', 1, 0, now(), now(), 1);

-- ── 核验预警（3 条未处理 + 1 条已处理） ──
INSERT INTO dms_verification_alert (id, tenant_id, binding_id, alert_type, alert_level, rider_id, rider_name, vehicle_id, plate_no, alert_content,
                                    handle_status, deleted, create_time, update_time, version) VALUES
(2099260000000000001, 1, 2099250000000000001, 1, 3, 2099230000000000001, 'E2E张小明', 2099220000000000001, '京A12345', 'E2E 人车位置分离 1500 米', 0, 0, now() - interval '20 minutes', now(), 1),
(2099260000000000002, 1, 2099250000000000002, 2, 2, 2099230000000000002, 'E2E李快跑', 2099220000000000003, '京C34567', 'E2E 异常滞留 45 分钟',     0, 0, now() - interval '35 minutes', now(), 1),
(2099260000000000003, 1, NULL,                 4, 3, 2099230000000000003, 'E2E王众包', NULL,                 NULL,       'E2E 偏离路线 3.2 公里',    0, 0, now() - interval '50 minutes', now(), 1),
(2099260000000000004, 1, 2099250000000000003, 5, 1, 2099230000000000003, 'E2E王众包', 2099220000000000002, '京B23456', 'E2E 绑定超时未交车（已处理）', 1, 0, now() - interval '2 hours', now(), 1);

-- ── 收款（代收货款样本） ──
INSERT INTO dms_payment (id, tenant_id, task_id, payment_type, amount, external_order_no, pay_time, status, audit_status, deleted, create_time, update_time, version) VALUES
(2099270000000000001, 1, 2099240000000000006, 1, 100, 'E2E-WX-0001', date_trunc('day', now()) + interval '9 hours 31 minutes', 1, 1, 0, now(), now(), 1);
