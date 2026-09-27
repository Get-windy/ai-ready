-- 调度任务（配送 → 调度管理 → 调度任务，菜单 80730 `dms:dispatch-task`）E2E 专用账号与种子数据
-- 用法：每次跑 E2E 前整文件执行一次（会重置配送员/任务/审计日志为可断言状态）。
--
-- 种子设计（每类断言都有确定数据）：
--   · Rider A(9210) 空闲·已实名·距取货点最近(约200m)  → 指派首选；超时升级的可用运力
--   · Rider B(9211) 空闲·已实名·较远(约3.3km)         → 改派目标 / 距离排序对照
--   · Rider C(9212) 休息中(status=3)                  → 候选「配送员休息中」拒绝原因
--   · Rider D(9213) 忙碌·已挂 5 个在途任务            → 候选「已达并接上限」拒绝原因 + 完成一单「不误释放」
--   · Rider E(9214) 忙碌·未实名(verify_status=0)·1 单在途 → 候选「实名认证未通过」拒绝原因
--                                                    + 「任务完成后回置空闲」用例（运力释放）
--                                                      （未实名 → 并行会话的派单候选一律不可派，在途数不会被抢占污染）
--   · Rider F(9215) 空闲·已实名·无定位                → 候选「无定位数据」拒绝原因
--   · Rider G(9216) 空闲·已实名                       → 超时升级承接运力（保证不因并发占满而失败）
--   · T301 待分配(普通/车辆/线路区域) → 指派用例        · T302 待分配(紧急) → 批量指派用例
--   · T303 待分配但 9999kg → 指派/自动调度「超出载重上限」
--   · T304 已分配(Rider B) → 改派用例                  · T305 已接单(Rider B) → 轨迹/异常用例
--   · T306 配送中且 deadline 已过 → 超时告警用例        · T307 已取消 → 状态筛选 7 直查
--   · T308 异常(status=8) → 是否异常筛选               · T309 待分配无坐标 → 未分配筛选
--   · T310 已完成(Rider B) → 完成时间列 / 批量取消「不可取消」用例
--   · T311 已分配(Rider G) 且派单已超阈值 → 超时升级自动重派用例
--   · 车辆 V1(9220) 绑定 T301 供「配送车辆」筛选；dms_task_log 清理后由用例重新写入

-- ═══ ① 清理历史 ═══
DELETE FROM dms_sign     WHERE task_id BETWEEN 2099000000000009300 AND 2099000000000009399;
DELETE FROM dms_task_log WHERE task_id BETWEEN 2099000000000009300 AND 2099000000000009399;
DELETE FROM dms_task      WHERE id BETWEEN 2099000000000009300 AND 2099000000000009399;
DELETE FROM dms_rider     WHERE id BETWEEN 2099000000000009210 AND 2099000000000009219;
DELETE FROM dms_vehicle   WHERE id BETWEEN 2099000000000009220 AND 2099000000000009229;
DELETE FROM sys_user_role   WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_dispatch_task');
DELETE FROM sys_user_tenant WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_dispatch_task');
DELETE FROM sys_user        WHERE username = 'e2e_dispatch_task';

-- ═══ ② 验收账号 ═══
INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                      user_type, is_super_admin, is_tenant_admin, status, data_scope)
SELECT 2099000000000009200, 1, 0, now(), now(), 'e2e_dispatch_task', password, 'E2E调度任务', 'E2E调度任务',
       1, false, false, 1, 'ALL'
FROM sys_user WHERE username = 'admin';

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
VALUES (2099000000000009201, 2099000000000009200, 1, 1, now());

INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000009202, 2099000000000009200, 1, true, 1, now(), now());

-- ═══ ③ 配送员（取货点基准 39.9100,116.3975）═══
INSERT INTO dms_rider (id, tenant_id, rider_type, rider_no, real_name, phone, vehicle_no, status, verify_status,
                       rating_score, total_orders, current_lat, current_lng, last_report_time, deleted, create_time, update_time)
VALUES
 (2099000000000009210, 1, 1, 'DPD001', 'E2E调度任务R1', '13900006001', '京A00001', 1, 1, 4.80, 120, 39.910500, 116.397500, now(), 0, now(), now()),
 (2099000000000009211, 1, 1, 'DPD002', 'E2E调度任务R2', '13900006002', '京A00002', 1, 1, 4.20, 80,  39.938700, 116.397500, now(), 0, now(), now()),
 (2099000000000009212, 1, 1, 'DPD003', 'E2E调度任务R3', '13900006003', '京A00003', 3, 1, 4.90, 200, 39.909000, 116.397600, now(), 0, now(), now()),
 (2099000000000009213, 1, 1, 'DPD004', 'E2E调度任务R4', '13900006004', '京A00004', 2, 1, 4.00, 60,  39.909200, 116.397700, now(), 0, now(), now()),
 (2099000000000009214, 1, 1, 'DPD005', 'E2E调度任务R5', '13900006005', '京A00005', 2, 0, 4.50, 10,  39.909300, 116.397800, now(), 0, now(), now()),
 (2099000000000009215, 1, 1, 'DPD006', 'E2E调度任务R6', '13900006006', '京A00006', 1, 1, 4.60, 30,  NULL,         NULL,         NULL,  0, now(), now()),
 (2099000000000009216, 1, 1, 'DPD007', 'E2E调度任务R7', '13900006007', '京A00007', 1, 1, 4.70, 150, 39.912000, 116.398000, now(), 0, now(), now());

-- ═══ ④ 车辆（供「配送车辆」筛选；Rider A 自带车另计）═══
INSERT INTO dms_vehicle (id, tenant_id, vehicle_code, plate_no, vehicle_type, ownership_type, status,
                         deleted, create_time, update_time, version)
VALUES (2099000000000009220, 1, 'E2EDPV1', '京E2E001', 2, 1, 0, 0, now(), now(), 0);

-- ═══ ⑤ Rider D 的 5 个在途任务（触发并接上限）+ Rider H 的 1 个在途任务（运力释放用例）═══
INSERT INTO dms_task (id, tenant_id, task_no, order_no, order_type, customer_id, customer_name, customer_lat, customer_lng,
                      rider_id, rider_name, total_quantity, status, priority, deleted, create_time, update_time, version)
SELECT 2099000000000009331 + g, 1, 'E2EDP-BUSY-' || g, 'E2EDP-BUSYO-' || g, 1, 1, 'E2E调度任务占位客户',
       39.908700, 116.397500, 2099000000000009213, 'E2E调度任务R4', 1, 4, 1, 0, now(), now(), 1
FROM generate_series(1, 5) AS g;

-- Rider E：名下仅此 1 单 → 签收审核通过（任务 → 已完成）后应回置「空闲(1)」；
-- Rider D 的 9332 同理但名下仍有 4 单在途 → 保持「忙碌(2)」（不得误释放）
INSERT INTO dms_task (id, tenant_id, task_no, order_no, order_type, customer_id, customer_name, customer_phone,
                      customer_lat, customer_lng, rider_id, rider_name, total_items, total_quantity,
                      delivery_fee, status, priority, deleted, create_time, update_time, version)
VALUES (2099000000000009341, 1, 'E2EDP-FREE-1', 'E2EDP-FREEO-1', 1, 1, 'E2E调度任务释放客户', '13800008888',
        39.908700, 116.397500, 2099000000000009214, 'E2E调度任务R5', 1, 5,
        10.00, 4, 1, 0, now(), now(), 1);

-- ═══ ⑥ 主用例任务 ═══
INSERT INTO dms_task (id, tenant_id, task_no, order_no, order_type, source_bill_no, customer_id, customer_name, customer_phone,
                      customer_address, source_lat, source_lng, customer_lat, customer_lng,
                      total_items, total_quantity, total_weight, total_volume, goods_amount, delivery_fee, collect_on_delivery,
                      priority, status, rider_id, rider_name, vehicle_id, vehicle_name, route_area, delivery_date,
                      dispatch_type, dispatch_time, deadline_time, completed_time, print_count, creator_name,
                      remark, deleted, create_time, update_time, version)
VALUES
 -- T301 待分配（普通；有车辆/区域，最近骑手在围栏内）
 (2099000000000009301, 1, 'E2EDPT-01', 'E2EDPTO-01', 1, 'XSCKD-E2E-01', 1, 'E2E调度任务客户甲', '13800008001',
  '北京市东城区E2E路1号', 39.910000, 116.397500, 39.908700, 116.397500,
  2, 5, 120.00, 0.50, 1880.00, 15.00, 100.00,
  1, 0, NULL, NULL, 2099000000000009220, '京E2E001', 'E2E区域甲', CURRENT_DATE,
  NULL, NULL, NULL, NULL, 0, 'E2E调度任务',
  'T301', 0, now() - interval '30 minutes', now(), 1),
 -- T302 待分配（紧急）
 (2099000000000009302, 1, 'E2EDPT-02', 'E2EDPTO-02', 1, 'XSCKD-E2E-02', 1, 'E2E调度任务客户乙', '13800008002',
  '北京市东城区E2E路2号', 39.910000, 116.397500, 39.908700, 116.397500,
  1, 3, 60.00, 0.30, 900.00, 12.00, 0.00,
  2, 0, NULL, NULL, NULL, NULL, 'E2E区域甲', CURRENT_DATE,
  NULL, NULL, NULL, NULL, 0, 'E2E调度任务',
  'T302', 0, now() - interval '28 minutes', now(), 1),
 -- T303 待分配但超载重（9999kg > 2000kg 上限）
 (2099000000000009303, 1, 'E2EDPT-03', 'E2EDPTO-03', 1, 'XSCKD-E2E-03', 1, 'E2E调度任务客户丙', '13800008003',
  '北京市东城区E2E路3号', 39.910000, 116.397500, 39.908700, 116.397500,
  9, 99, 9999.00, 0.90, 3600.00, 30.00, 0.00,
  3, 0, NULL, NULL, NULL, NULL, 'E2E区域乙', CURRENT_DATE,
  NULL, NULL, NULL, NULL, 0, 'E2E调度任务',
  'T303', 0, now() - interval '26 minutes', now(), 1),
 -- T304 已分配（Rider B）→ 改派用例
 (2099000000000009304, 1, 'E2EDPT-04', 'E2EDPTO-04', 1, 'XSCKD-E2E-04', 1, 'E2E调度任务客户丁', '13800008004',
  '北京市东城区E2E路4号', 39.910000, 116.397500, 39.908700, 116.397500,
  1, 2, 40.00, 0.20, 660.00, 10.00, 0.00,
  1, 1, 2099000000000009211, 'E2E调度任务R2', NULL, NULL, 'E2E区域乙', CURRENT_DATE,
  2, now() - interval '40 minutes', now() + interval '3 hours', NULL, 1, 'E2E调度任务',
  'T304', 0, now() - interval '50 minutes', now(), 1),
 -- T305 已接单（Rider B）→ 轨迹 / 异常用例
 (2099000000000009305, 1, 'E2EDPT-05', 'E2EDPTO-05', 1, 'XSCKD-E2E-05', 1, 'E2E调度任务客户戊', '13800008005',
  '北京市东城区E2E路5号', 39.910000, 116.397500, 39.908700, 116.397500,
  1, 1, 20.00, 0.10, 320.00, 8.00, 0.00,
  1, 2, 2099000000000009211, 'E2E调度任务R2', NULL, NULL, 'E2E区域乙', CURRENT_DATE,
  2, now() - interval '35 minutes', now() + interval '2 hours', NULL, 0, 'E2E调度任务',
  'T305', 0, now() - interval '45 minutes', now(), 1),
 -- T306 配送中且已超时 → 超时告警用例
 (2099000000000009306, 1, 'E2EDPT-06', 'E2EDPTO-06', 1, 'XSCKD-E2E-06', 1, 'E2E调度任务客户己', '13800008006',
  '北京市东城区E2E路6号', 39.910000, 116.397500, 39.908700, 116.397500,
  1, 4, 80.00, 0.40, 1500.00, 20.00, 0.00,
  3, 4, 2099000000000009211, 'E2E调度任务R2', NULL, NULL, 'E2E区域丙', CURRENT_DATE,
  2, now() - interval '4 hours', now() - interval '2 hours', NULL, 0, 'E2E调度任务',
  'T306', 0, now() - interval '5 hours', now(), 1),
 -- T307 已取消 → 显式状态筛选直查
 (2099000000000009307, 1, 'E2EDPT-07', 'E2EDPTO-07', 1, 'XSCKD-E2E-07', 1, 'E2E调度任务客户庚', '13800008007',
  '北京市东城区E2E路7号', 39.910000, 116.397500, 39.908700, 116.397500,
  1, 1, 10.00, 0.05, 100.00, 5.00, 0.00,
  1, 7, NULL, NULL, NULL, NULL, NULL, CURRENT_DATE,
  NULL, NULL, NULL, NULL, 0, 'E2E调度任务',
  'T307', 0, now() - interval '3 hours', now(), 1),
 -- T308 异常
 (2099000000000009308, 1, 'E2EDPT-08', 'E2EDPTO-08', 1, 'XSCKD-E2E-08', 1, 'E2E调度任务客户辛', '13800008008',
  '北京市东城区E2E路8号', 39.910000, 116.397500, 39.908700, 116.397500,
  1, 1, 10.00, 0.05, 120.00, 5.00, 0.00,
  1, 8, 2099000000000009211, 'E2E调度任务R2', NULL, NULL, NULL, CURRENT_DATE,
  2, now() - interval '90 minutes', NULL, NULL, 0, 'E2E调度任务',
  'T308', 0, now() - interval '2 hours', now(), 1),
 -- T309 待分配但无取货点坐标 → 未分配筛选 / 候选「缺少取货点坐标」
 (2099000000000009309, 1, 'E2EDPT-09', 'E2EDPTO-09', 3, 'XSTHD-E2E-09', 1, 'E2E调度任务客户壬', '13800008009',
  '北京市东城区E2E路9号', NULL, NULL, NULL, NULL,
  1, 1, 10.00, 0.05, 150.00, 5.00, 0.00,
  1, 0, NULL, NULL, NULL, NULL, NULL, CURRENT_DATE,
  NULL, NULL, NULL, NULL, 0, 'E2E调度任务',
  'T309', 0, now() - interval '20 minutes', now(), 1),
 -- T310 已完成（Rider B）→ 批量取消「不可取消」用例
 (2099000000000009310, 1, 'E2EDPT-10', 'E2EDPTO-10', 1, 'XSCKD-E2E-10', 1, 'E2E调度任务客户癸', '13800008010',
  '北京市东城区E2E路10号', 39.910000, 116.397500, 39.908700, 116.397500,
  1, 6, 90.00, 0.45, 2100.00, 18.00, 0.00,
  1, 6, 2099000000000009211, 'E2E调度任务R2', NULL, NULL, 'E2E区域丙', CURRENT_DATE,
  2, now() - interval '8 hours', now() - interval '6 hours', now() - interval '6 hours', 2, 'E2E调度任务',
  'T310', 0, now() - interval '9 hours', now(), 1),
 -- T311 已分配（Rider G）且派单已超阈值 → 超时升级自动重派用例
 (2099000000000009311, 1, 'E2EDPT-11', 'E2EDPTO-11', 1, 'XSCKD-E2E-11', 1, 'E2E调度任务客户子', '13800008011',
  '北京市东城区E2E路11号', 39.910000, 116.397500, 39.908700, 116.397500,
  1, 1, 30.00, 0.15, 480.00, 9.00, 0.00,
  1, 1, 2099000000000009216, 'E2E调度任务R7', NULL, NULL, 'E2E区域甲', CURRENT_DATE,
  2, now() - interval '2 hours', now() + interval '4 hours', NULL, 0, 'E2E调度任务',
  'T311', 0, now() - interval '3 hours', now(), 1);

-- ═══ ⑦ Rider B 的在途占用与状态保持一致（T304/T305/T306/T308/T310 已挂其名下）═══
UPDATE dms_rider SET status = 2 WHERE id = 2099000000000009211;

-- ═══ ⑧ 派单策略重置为可断言默认值 ═══
INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, create_time, update_time)
VALUES
  (1, 'dms.dispatch.strategy',        'NEAREST', '派单策略：NEAREST/BALANCED/SCORE/AREA', 'TENANT', 0, now(), now()),
  (1, 'dms.dispatch.weight.distance', '1', '策略权重：距离', 'TENANT', 0, now(), now()),
  (1, 'dms.dispatch.weight.load',     '1', '策略权重：负载', 'TENANT', 0, now(), now()),
  (1, 'dms.dispatch.weight.score',    '1', '策略权重：评分', 'TENANT', 0, now(), now()),
  (1, 'dms.dispatch.max.concurrent',  '5', '单人最大并接在途单数(0=不限)', 'TENANT', 0, now(), now()),
  (1, 'dms.dispatch.require.online',  'true', '是否仅向空闲配送员派单', 'TENANT', 0, now(), now()),
  (1, 'dms.dispatch.max.load.kg',     '2000', '单任务载重上限(kg)', 'TENANT', 0, now(), now()),
  (1, 'dms.dispatch.max.volume.m3',   '10', '单任务容积上限(m³)', 'TENANT', 0, now(), now()),
  (1, 'dms.dispatch.timeout.escalate.minutes', '30', '超时升级阈值(分钟)', 'TENANT', 0, now(), now())
ON CONFLICT (tenant_id, config_key) WHERE deleted = 0 DO UPDATE
  SET config_value = EXCLUDED.config_value, update_time = CURRENT_TIMESTAMP;

-- ═══ ⑨ 清理（验收结束后执行）═══
-- DELETE FROM dms_task_log WHERE task_id BETWEEN 2099000000000009300 AND 2099000000000009399;
-- DELETE FROM dms_task    WHERE id BETWEEN 2099000000000009300 AND 2099000000000009399;
-- DELETE FROM dms_rider   WHERE id BETWEEN 2099000000000009210 AND 2099000000000009219;
-- DELETE FROM dms_vehicle WHERE id BETWEEN 2099000000000009220 AND 2099000000000009229;
-- DELETE FROM sys_user_role   WHERE user_id = 2099000000000009200;
-- DELETE FROM sys_user_tenant WHERE user_id = 2099000000000009200;
-- DELETE FROM sys_user        WHERE id = 2099000000000009200;
