-- 发货查询（配发收 → 发货业务 → 发货查询，菜单 70156）E2E 专用验收账号 + 数据基座
-- 用法：python tools/dbq2.py "<本文件内容>" 或 python -c "…execute(open(...).read())…"
-- 验收结束后执行文件末尾「清理」语句回收基座数据。
--
-- 基座说明（对标本页 49 列/25 查询项/2 固定项所需的最小真实数据）：
--   · erp_sale_outbound / _item —— 销售出库单 XSCKD 台账（含 1 张红冲负数金额单，用于「显示红冲」）
--   · erp_route                 —— 线路档案（固定项「配送线路」数据源）
--   · dms_task                  —— 配送任务（固定项「配送状态」数据源，按 source_bill_no 关联出库单）

-- ── 1) 验收账号（密码复用 admin = admin123，避免与并行会话共用 admin 互踢） ──
DELETE FROM sys_user_role   WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_shipq')
                               OR id = 2099000000000000971;
DELETE FROM sys_user_tenant WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_shipq')
                               OR id = 2099000000000000972;
DELETE FROM sys_user        WHERE username = 'e2e_shipq' OR id = 2099000000000000970;

INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                      user_type, is_super_admin, is_tenant_admin, status, data_scope)
SELECT 2099000000000000970, 1, 0, now(), now(), 'e2e_shipq', password, 'E2E发货查询', 'E2E发货查询',
       1, false, false, 1, 'ALL'
FROM sys_user WHERE username = 'admin';

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
VALUES (2099000000000000971, 2099000000000000970, 1, 1, now());

INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000000972, 2099000000000000970, 1, true, 1, now(), now());

-- ── 2) 线路档案（配送线路固定项） ──
DELETE FROM erp_route WHERE id = 2099000000000000401 OR route_code = 'E2EXL001';
INSERT INTO erp_route (id, tenant_id, route_code, route_name, route_self, route_logistics, status, deleted, create_time, update_time)
VALUES (2099000000000000401, 1, 'E2EXL001', 'E2E发货查询线路', 1, 0, 'ENABLED', 0, now(), now());

-- ── 3) 销售出库单台账（含 1 张红冲负数金额） ──
DELETE FROM erp_sale_outbound_item
 WHERE outbound_id IN (2099000000000000501, 2099000000000000502, 2099000000000000503)
    OR outbound_id IN (SELECT id FROM erp_sale_outbound WHERE outbound_no LIKE 'E2E-XSCKD-%');
DELETE FROM erp_sale_outbound WHERE outbound_no LIKE 'E2E-XSCKD-%';

-- 金额构成列（promo_discount/coupon_amount/direct_discount/freight/other_fee）刻意取非零值：
--   本单金额 = 商品金额 − 促销优惠 − 优惠劵 − 直接优惠 + 运费 + 其他费用
--   0001：1000 − 50 − 0 − 0 + 20 + 10 = 980.00（≠ 商品金额 1000，用于验证页面口径未错绑商品金额）
--   0002： 500 − 0 − 100 − 50 + 30 + 0 = 380.00
INSERT INTO erp_sale_outbound (id, tenant_id, outbound_no, outbound_date, status, order_no, customer_name,
                               warehouse_name, sales_person_name, department_name, total_quantity, total_amount,
                               promo_discount, coupon_amount, direct_discount, freight, other_fee, settled_amount,
                               settlement_status, generation_method, remark, print_count, creator_name,
                               auditor_name, bookkeeper_name, ext_num1, ext_num2, ext_text1, ext_text2, ext_text3, delivery_method,
                               logistics_company, tracking_number, receiver_name, receiver_phone,
                               shipping_address, total_weight, total_volume, summary, create_time, update_time, deleted)
VALUES
 (2099000000000000501, 1, 'E2E-XSCKD-0001', CURRENT_DATE, 11, 'E2E-XSDD-0001', 'E2E发货客户甲',
  'E2E主仓', 'E2E经手人', 'E2E发货部门', 10, 1000.00,
  50.00, 0.00, 0.00, 20.00, 10.00, 400.00,
  'partial', '订单生成', 'E2E发货备注', 0, 'E2E制单人',
  'E2E审核人', 'E2E记账人', 8, 99, 'E2E表头文本1', 'E2E文本2A', 'E2E文本3A', 'delivery',
  'E2E物流公司', 'E2E运单0001', 'E2E收货人', '13800000001',
  'E2E收货地址1', 12.500, 0.800, 'E2E摘要', now(), now(), 0),
 (2099000000000000502, 1, 'E2E-XSCKD-0002', CURRENT_DATE, 10, 'E2E-XSDD-0002', 'E2E发货客户乙',
  'E2E主仓', 'E2E经手人', 'E2E发货部门', 4, 500.00,
  0.00, 100.00, 50.00, 30.00, 0.00, 500.00,
  'settled', '手工创建', 'E2E发货备注2', 2, 'E2E制单人',
  'E2E审核人', 'E2E记账人', 9, NULL, 'E2E表头文本2', 'E2E文本2B', NULL, 'express',
  'E2E物流公司', 'E2E运单0002', 'E2E收货人2', '13800000002',
  'E2E收货地址2', 6.000, 0.400, 'E2E摘要2', now(), now(), 0),
 (2099000000000000503, 1, 'E2E-XSCKD-9001', CURRENT_DATE, 11, 'E2E-XSDD-0001', 'E2E发货客户甲',
  'E2E主仓', 'E2E经手人', 'E2E发货部门', -1, -100.00,
  0.00, 0.00, 0.00, 0.00, 0.00, 0.00,
  'unsettled', '手工创建', 'E2E红冲单（默认隐藏）', 0, 'E2E制单人',
  'E2E审核人', 'E2E记账人', 8, 99, 'E2E表头文本1', 'E2E文本2A', 'E2E文本3A', 'delivery',
  'E2E物流公司', 'E2E运单9001', 'E2E收货人', '13800000001',
  'E2E收货地址1', 0.000, 0.000, 'E2E红冲摘要', now(), now(), 0);

-- ── 4) 出库明细（「商品汇总」与「查看明细」的数据源） ──
INSERT INTO erp_sale_outbound_item (id, tenant_id, outbound_id, line_no, product_code, product_name,
                                    product_spec, product_unit, outbound_quantity, unit_price,
                                    line_amount, deleted, create_time, update_time)
VALUES
 (2099000000000000601, 1, 2099000000000000501, 1, 'E2E-P001', 'E2E商品甲', '规格A', '件', 6, 100.0000, 600.00, 0, now(), now()),
 (2099000000000000602, 1, 2099000000000000501, 2, 'E2E-P002', 'E2E商品乙', '规格B', '件', 4, 100.0000, 400.00, 0, now(), now()),
 (2099000000000000603, 1, 2099000000000000502, 1, 'E2E-P001', 'E2E商品甲', '规格A', '件', 4, 125.0000, 500.00, 0, now(), now());

-- ── 5) 配送任务（配送状态 / 配送线路 固定项数据源） ──
DELETE FROM dms_task WHERE task_no LIKE 'E2E-PSD-9%';
INSERT INTO dms_task (id, tenant_id, task_no, source_bill_no, status, route_id, deleted, create_time, update_time)
VALUES
 (2099000000000000801, 1, 'E2E-PSD-9001', 'E2E-XSCKD-0001', 4, 2099000000000000401, 0, now(), now()),
 (2099000000000000802, 1, 'E2E-PSD-9002', 'E2E-XSCKD-0002', 6, NULL, 0, now(), now());

-- ⚠️ 清理（验收结束后执行）：
-- DELETE FROM sys_user_role   WHERE user_id = 2099000000000000970;
-- DELETE FROM sys_user_tenant WHERE user_id = 2099000000000000970;
-- DELETE FROM sys_user        WHERE id = 2099000000000000970;
-- DELETE FROM erp_sale_outbound_item WHERE outbound_id IN (2099000000000000501,2099000000000000502,2099000000000000503);
-- DELETE FROM erp_sale_outbound      WHERE outbound_no LIKE 'E2E-XSCKD-%';
-- DELETE FROM dms_task               WHERE task_no LIKE 'E2E-PSD-9%';
-- DELETE FROM erp_route              WHERE id = 2099000000000000401;
