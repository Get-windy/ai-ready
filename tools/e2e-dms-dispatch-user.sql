-- 智能调度（配送 → 调度管理 → 智能调度，菜单 80850 `dms:dispatch`）E2E 专用账号与种子数据
-- 用法：每次跑 E2E 前整文件执行一次（会重置配送员/任务/策略配置为可断言状态）。
--
-- 种子设计（每类断言都有确定数据）：
--   · R1 空闲且最近（约 200 米）→ 预览首选；R2 空闲但较远（约 3.3 公里）→ 次选
--   · R3 空闲但已挂 5 个在途任务 → 触发「已达并接上限」（策略 maxConcurrent 可调小以必现）
--   · R4 休息中（status=3）→ 触发「配送员休息中」
--   · T1/T2 待分配（普通重量）且归属「E2E调度线路甲」；T3 待分配但 total_weight=9999kg → 触发「超出载重上限」
--   · 区域分包：线路档案 E2ERT-01 只绑定 R2 → AREA 策略下 R2（较远）优先于 R1（最近）；严格模式下 R1 不可派
--   · 抢单/竞价样本各 1 单（dispatch_type=3/4）→ 效果复盘分布条有确定数据
--   · 策略配置重置为：NEAREST / 权重 1:1:1 / 并接上限 5 / 需空闲 / 载重 2000kg / 容积 10m³ / 超时 30 分钟 / 区域分包非严格

-- ═══ ① 清理历史 ═══
DELETE FROM dms_route_rider WHERE id BETWEEN 2099000000000009500 AND 2099000000000009599
   OR route_id = 2099000000000009501;
DELETE FROM erp_route      WHERE id = 2099000000000009501;
DELETE FROM dms_task  WHERE id BETWEEN 2099000000000009100 AND 2099000000000009299;
DELETE FROM dms_rider WHERE id BETWEEN 2099000000000009010 AND 2099000000000009019;
DELETE FROM sys_user_role   WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_dispatch');
DELETE FROM sys_user_tenant WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_dispatch');
DELETE FROM sys_user        WHERE username = 'e2e_dispatch';

-- ═══ ② 验收账号 ═══
INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                      user_type, is_super_admin, is_tenant_admin, status, data_scope)
SELECT 2099000000000009000, 1, 0, now(), now(), 'e2e_dispatch', password, 'E2E调度', 'E2E调度',
       1, false, false, 1, 'ALL'
FROM sys_user WHERE username = 'admin';

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
VALUES (2099000000000009001, 2099000000000009000, 1, 1, now());

INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000009002, 2099000000000009000, 1, true, 1, now(), now());

-- ═══ ③ 配送员（客户坐标 39.9087,116.3975 为基准）═══
INSERT INTO dms_rider (id, tenant_id, rider_type, rider_no, real_name, phone, status, verify_status, rating_score,
                       current_lat, current_lng, last_report_time, deleted, create_time, update_time)
VALUES
 (2099000000000009010, 1, 1, 'DP0001', 'E2E调度骑手R1', '13900005001', 1, 1, 4.80, 39.910500, 116.397500, now(), 0, now(), now()),
 (2099000000000009011, 1, 1, 'DP0002', 'E2E调度骑手R2', '13900005002', 1, 1, 4.20, 39.938700, 116.397500, now(), 0, now(), now()),
 (2099000000000009012, 1, 1, 'DP0003', 'E2E调度骑手R3', '13900005003', 1, 1, 4.90, 39.909000, 116.397600, now(), 0, now(), now()),
 (2099000000000009013, 1, 1, 'DP0004', 'E2E调度骑手R4', '13900005004', 3, 1, 4.00, 39.909200, 116.397700, now(), 0, now(), now()),
 -- 资质不通过的骑手（未审核）→ 触发「资质不满足」
 (2099000000000009014, 1, 1, 'DP0005', 'E2E调度骑手R5', '13900005005', 1, 0, 4.50, 39.909300, 116.397800, now(), 0, now(), now());

-- ═══ ④ R3 的 5 个在途任务（触发并接上限）═══
INSERT INTO dms_task (id, tenant_id, task_no, customer_id, customer_name, customer_lat, customer_lng,
                      rider_id, rider_name, total_quantity, status, deleted, create_time, update_time, version)
SELECT 2099000000000009110 + g, 1, 'E2EDP-BUSY-' || g, 1, 'E2E调度占位客户', 39.908700, 116.397500,
       2099000000000009012, 'E2E调度骑手R3', 1, 4, 0, now(), now(), 1
FROM generate_series(1, 5) AS g;

-- ═══ ⑤ 待分配任务（T3 超载重，触发超限约束）═══
-- 取货点(source_lat/lng) = 39.9100,116.3975（R1 距此约 200 米）；距离与围栏都以取货点为准
INSERT INTO dms_task (id, tenant_id, task_no, order_no, customer_id, customer_name, customer_phone,
                      source_lat, source_lng, customer_lat, customer_lng,
                      total_items, total_quantity, total_weight, total_volume,
                      delivery_fee, status, deleted, create_time, update_time, version)
VALUES
 (2099000000000009101, 1, 'E2EDP-01', 'E2EDPO-01', 1, 'E2E调度客户甲', '13800007001', 39.910000, 116.397500, 39.908700, 116.397500, 2, 5, 120.00, 0.50, 15.00, 0, 0, now() - interval '5 minutes', now(), 1),
 (2099000000000009102, 1, 'E2EDP-02', 'E2EDPO-02', 1, 'E2E调度客户乙', '13800007002', 39.910000, 116.397500, 39.908700, 116.397500, 1, 3, 60.00, 0.30, 12.00, 0, 0, now() - interval '3 minutes', now(), 1),
 (2099000000000009103, 1, 'E2EDP-03', 'E2EDPO-03', 1, 'E2E调度客户丙', '13800007003', 39.910000, 116.397500, 39.908700, 116.397500, 9, 99, 9999.00, 0.90, 30.00, 0, 0, now() - interval '1 minutes', now(), 1);

-- ═══ ⑥ 抢单/竞价样本（效果复盘「调度方式分布」的归口统计：dispatch_type 3-抢单 4-竞价）═══
INSERT INTO dms_task (id, tenant_id, task_no, customer_id, customer_name, customer_lat, customer_lng,
                      rider_id, rider_name, total_quantity, status, dispatch_type, dispatch_time,
                      deleted, create_time, update_time, version)
VALUES
 (2099000000000009120, 1, 'E2EDP-GRAB-01', 1, 'E2E调度抢单客户', 39.908700, 116.397500,
  2099000000000009011, 'E2E调度骑手R2', 1, 4, 3, now() - interval '10 minutes', 0, now() - interval '12 minutes', now(), 1),
 (2099000000000009121, 1, 'E2EDP-BID-01', 1, 'E2E调度竞价客户', 39.908700, 116.397500,
  2099000000000009011, 'E2E调度骑手R2', 1, 4, 4, now() - interval '8 minutes', 0, now() - interval '9 minutes', now(), 1);

-- ═══ ⑦ 线路档案 + 区域分包绑定（AREA 策略：R2 绑定线路甲，R1 未绑定）═══
-- 线路主数据（资料 → 配送管理 → 线路）DMS 侧只读引用；E2E 用固定 ID 便于断言
INSERT INTO erp_route (id, tenant_id, route_code, route_name, route_self, route_logistics, status,
                       remark, deleted, create_time, update_time)
VALUES (2099000000000009501, 1, 'E2ERT-01', 'E2E调度线路甲', 1, 0, 'ENABLED',
        'E2E 区域分包用例线路', 0, now(), now());

INSERT INTO dms_route_rider (id, tenant_id, route_id, route_code, route_name, rider_id, rider_name,
                             priority, status, remark, deleted, create_time, update_time)
VALUES (2099000000000009500, 1, 2099000000000009501, 'E2ERT-01', 'E2E调度线路甲',
        2099000000000009011, 'E2E调度骑手R2', 0, 1, 'E2E 区域分包种子绑定', 0, now(), now());

-- T1/T2 归属线路甲（T3 不设线路，保留「超载重」与「区域分包降级」两条互不干扰的用例）
UPDATE dms_task SET route_id = 2099000000000009501, route_area = 'E2E区域甲'
WHERE id IN (2099000000000009101, 2099000000000009102);

-- ═══ ⑧ 策略配置重置（默认值；E2E 会临时改大再还原）═══
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
  (1, 'dms.dispatch.timeout.escalate.minutes', '30', '超时升级阈值(分钟)', 'TENANT', 0, now(), now()),
  (1, 'dms.dispatch.area.strict',    'false', '区域分包严格模式', 'TENANT', 0, now(), now())
ON CONFLICT (tenant_id, config_key) WHERE deleted = 0 DO UPDATE
  SET config_value = EXCLUDED.config_value, update_time = CURRENT_TIMESTAMP;

-- ═══ ⑨ 清理（验收结束后执行）═══
-- DELETE FROM dms_route_rider WHERE route_id = 2099000000000009501;
-- DELETE FROM erp_route WHERE id = 2099000000000009501;
-- DELETE FROM dms_task  WHERE id BETWEEN 2099000000000009100 AND 2099000000000009299;
-- DELETE FROM dms_rider WHERE id BETWEEN 2099000000000009010 AND 2099000000000009019;
-- DELETE FROM sys_user_role   WHERE user_id = 2099000000000009000;
-- DELETE FROM sys_user_tenant WHERE user_id = 2099000000000009000;
-- DELETE FROM sys_user        WHERE id = 2099000000000009000;
