-- 渠道管理 E2E 测试数据（租户 1，幂等：先删后插）
-- 覆盖：自有/外部平台渠道、被配送员引用（引用保护）、未引用（可删除）、对接配置脱敏样本
-- 用法：python tools/dbq2.py "$(cat tools/e2e-dms-channel-seed.sql | grep -v '^--')"

DELETE FROM dms_rider   WHERE id BETWEEN 2099310000000000000 AND 2099319999999999999;
DELETE FROM dms_channel WHERE id BETWEEN 2099300000000000000 AND 2099309999999999999;

-- ── 渠道样本 ──
-- ① 自有员工渠道（无适配器，内部运力）
INSERT INTO dms_channel (id, tenant_id, channel_code, channel_name, channel_type, adapter_bean, config_json,
                         status, priority, sort_order, link_status, coverage_area, billing_type, billing_config,
                         remark, deleted, create_time, update_time, version) VALUES
(2099300000000000001, 1, 'E2E-CH-OWN', 'E2E自有配送队', 1, NULL, NULL,
 1, 10, 1, 0, '北京市朝阳区', 1, '{"basePrice":5,"unitPrice":2}', 'E2E 样本：自有运力', 0, now(), now(), 1);

-- ② 外部平台渠道（配置适配器 + 含密钥的对接配置，用于脱敏与引用保护）
INSERT INTO dms_channel (id, tenant_id, channel_code, channel_name, channel_type, adapter_bean, config_json,
                         status, priority, sort_order, link_status, coverage_area, billing_type, billing_config,
                         remark, deleted, create_time, update_time, version) VALUES
(2099300000000000002, 1, 'E2E-CH-DADA', 'E2E达达平台', 3, 'dadaAdapter',
 '{"appKey":"E2E-AK-001","appSecret":"E2E-SECRET-RAW-001","callbackUrl":"https://e2e.example.com/cb","token":"E2E-TOKEN-RAW"}',
 1, 20, 2, 0, '北京市,天津市', 2, '{"basePrice":8,"unitPrice":3,"extraPerKm":1.5}', 'E2E 样本：外部平台', 0, now(), now(), 1);

-- ③ 临时渠道（无引用，用于删除/批量用例）
INSERT INTO dms_channel (id, tenant_id, channel_code, channel_name, channel_type, adapter_bean, config_json,
                         status, priority, sort_order, link_status, coverage_area, billing_type, billing_config,
                         remark, deleted, create_time, update_time, version) VALUES
(2099300000000000003, 1, 'E2E-CH-TMP', 'E2E临时渠道', 4, NULL, NULL,
 0, 30, 3, 0, NULL, 1, NULL, 'E2E 样本：待删除', 0, now(), now(), 1);

-- ── 引用样本：一名配送员挂在 ② 渠道上（触发删除引用保护） ──
INSERT INTO dms_rider (id, tenant_id, user_id, rider_type, real_name, phone, vehicle_type, vehicle_no, channel_id,
                       last_report_time, status, verify_status, rating_score, total_orders, completed_orders,
                       max_concurrent, deleted, create_time, update_time, version) VALUES
(2099310000000000001, 1, NULL, 3, 'E2E渠道骑手', '13900000001', '电动车', '京E99999', 2099300000000000002,
 now() - interval '1 minute', 1, 1, 4.50, 10, 9, 5, 0, now(), now(), 1);

-- ── 渠道派单/回调样本：一条「待分配」任务（派单成功后转「已分配」，回调再回写状态） ──
DELETE FROM dms_task WHERE id BETWEEN 2099300000000000010 AND 2099300000000000019;
INSERT INTO dms_task (id, tenant_id, task_no, order_no, order_type, channel_id, status,
                      source_address, source_lat, source_lng, customer_name, customer_address,
                      customer_lat, customer_lng, total_weight, total_volume, remark,
                      deleted, create_time, update_time, version) VALUES
(2099300000000000010, 1, 'E2E-CH-TASK-001', 'E2E-CH-SO-001', 1, 2099300000000000001, 0,
 '北京市朝阳区仓库A', 39.9219, 116.4434, 'E2E渠道客户', '北京市海淀区中关村1号',
 39.9836, 116.3164, 12.5, 0.8, 'E2E 渠道派单样本', 0, now(), now(), 1);
