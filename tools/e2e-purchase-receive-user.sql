-- 采购订货收货（配发收 → 收货业务 → 采购订货收货，菜单 70161）E2E 专用验收账号 + 数据基座
-- 用法：python -c "…#执行本文件…"；验收结束后执行文件末尾「清理」语句回收基座数据。
--
-- 基座说明（覆盖三种收货进度 + 三种结算状态）：
--   · E2E-CGD-0001：应收 10 / 已收 4 → 部分收货；结算 0/1000 → 未结算（不触发收货闭环）
--   · E2E-CGD-0002：应收 8 / 已收 0 → 待收货；结算 800/800 → 已结算（批量收货用例目标）
--   · E2E-CGD-0003：应收 5 / 已收 5 → 已收货；结算 200/500 → 部分结算
--   · 明细：待收货数量 = 订货数量 − 已收数量（0001 两行、0002 一行、0003 已收满一行）

-- ── 1) 验收账号（密码复用 admin = admin123） ──
DELETE FROM sys_user_role   WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_purrecv')
                               OR id = 2099000000000000982;
DELETE FROM sys_user_tenant WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_purrecv')
                               OR id = 2099000000000000983;
DELETE FROM sys_user        WHERE username = 'e2e_purrecv' OR id = 2099000000000000981;

INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                      user_type, is_super_admin, is_tenant_admin, status, data_scope)
SELECT 2099000000000000981, 1, 0, now(), now(), 'e2e_purrecv', password, 'E2E采购收货', 'E2E采购收货',
       1, false, false, 1, 'ALL'
FROM sys_user WHERE username = 'admin';

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
VALUES (2099000000000000982, 2099000000000000981, 1, 1, now());

INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000000983, 2099000000000000981, 1, true, 1, now(), now());

-- ── 2) 采购订单（仓库取既有 id=1「主仓库」；供应商名称走伙伴快照表） ──
DELETE FROM erp_purchase_order_item WHERE order_id IN (2099000000000000701, 2099000000000000702, 2099000000000000703);
DELETE FROM erp_purchase_order_partner_snapshot WHERE order_id IN (2099000000000000701, 2099000000000000702, 2099000000000000703);
DELETE FROM erp_purchase_order WHERE order_no LIKE 'E2E-CGD-%';

INSERT INTO erp_purchase_order (id, tenant_id, order_no, order_date, status, warehouse_id, supplier_id,
                                purchaser_id, dept_id, create_by, expected_receive_time,
                                total_quantity, received_amount, bill_amount, settled_amount,
                                remark, create_time, update_time, deleted)
VALUES
 (2099000000000000701, 1, 'E2E-CGD-0001', now(), 3, 1, 2099000000000000901, 2099000000000000981, NULL, 2099000000000000981,
  now() + interval '3 day', 10, 4, 1000.00, 0.00,
  'E2E部分收货单', now(), now(), 0),
 (2099000000000000702, 1, 'E2E-CGD-0002', now(), 3, 1, 2099000000000000901, 2099000000000000981, NULL, 2099000000000000981,
  now() + interval '5 day', 8, 0, 800.00, 800.00,
  'E2E待收货单', now(), now(), 0),
 (2099000000000000703, 1, 'E2E-CGD-0003', now(), 6, 1, 2099000000000000901, 2099000000000000981, NULL, 2099000000000000981,
  now() + interval '1 day', 5, 5, 500.00, 200.00,
  'E2E已收货单', now(), now(), 0);

INSERT INTO erp_purchase_order_partner_snapshot (id, order_id, supplier_name, supplier_code, contact_name, contact_phone, create_time, tenant_id)
VALUES
 (2099000000000000711, 2099000000000000701, 'E2E采购供应商', 'E2EGYS001', 'E2E联系人', '13900000001', now(), 1),
 (2099000000000000712, 2099000000000000702, 'E2E采购供应商', 'E2EGYS001', 'E2E联系人', '13900000001', now(), 1),
 (2099000000000000713, 2099000000000000703, 'E2E采购供应商', 'E2EGYS001', 'E2E联系人', '13900000001', now(), 1);

-- ── 2.1) 订单扩展信息（摘要 / 附件 / 自定义字段 1-2(数字) 3-5(文本) / 打印次数） ──
DELETE FROM erp_purchase_order_ext_info WHERE order_id IN (2099000000000000701, 2099000000000000702, 2099000000000000703);
INSERT INTO erp_purchase_order_ext_info (id, order_id, summary, attachment, ext_num_1, ext_num_2, ext_text_1, ext_text_2, ext_text_3, print_count, create_time, tenant_id)
VALUES
 (2099000000000000741, 2099000000000000701, 'E2E采购摘要1', '/api/file/view/2026/09/13/e2e0000000000000000000000000001.pdf', 11, 12, 'E2E文本1', 'E2E文本2', 'E2E文本3', 1, now(), 1),
 (2099000000000000742, 2099000000000000702, 'E2E采购摘要2', '', 21, 22, 'E2E文本21', 'E2E文本22', 'E2E文本23', 0, now(), 1),
 (2099000000000000743, 2099000000000000703, 'E2E采购摘要3', '', 31, 32, 'E2E文本31', 'E2E文本32', 'E2E文本33', 0, now(), 1);

-- ── 3) 采购订单明细（数量 = 订货；received_quantity_detail = 已收） ──
INSERT INTO erp_purchase_order_item (id, tenant_id, order_id, line_no, product_id, product_code, product_name,
                                     item_code, specification, unit, quantity, received_quantity, unit_price, tax_rate,
                                     amount, remark)
VALUES
 (2099000000000000721, 1, 2099000000000000701, 1, NULL, 'E2E-P101', 'E2E采购商品甲', 'E2E-P101', '规格A', '件', 6, 2, 100.0000, 0.0000, 600.00, 'E2E明细备注1'),
 (2099000000000000722, 1, 2099000000000000701, 2, NULL, 'E2E-P102', 'E2E采购商品乙', 'E2E-P102', '规格B', '件', 4, 2, 100.0000, 0.0000, 400.00, 'E2E明细备注2'),
 (2099000000000000723, 1, 2099000000000000702, 1, NULL, 'E2E-P101', 'E2E采购商品甲', 'E2E-P101', '规格A', '件', 8, 0, 100.0000, 0.0000, 800.00, 'E2E明细备注3'),
 (2099000000000000724, 1, 2099000000000000703, 1, NULL, 'E2E-P103', 'E2E采购商品丙', 'E2E-P103', '规格C', '件', 5, 5, 100.0000, 0.0000, 500.00, 'E2E明细备注4');

-- ⚠️ 清理（验收结束后执行；验收产生的采购入库单按 order_id 关联清理）：
-- DELETE FROM erp_purchase_inbound_item WHERE inbound_id IN (SELECT id FROM erp_purchase_inbound WHERE order_id IN (2099000000000000701,2099000000000000702,2099000000000000703));
-- DELETE FROM erp_purchase_inbound      WHERE order_id IN (2099000000000000701,2099000000000000702,2099000000000000703);
-- DELETE FROM erp_purchase_order_ext_info WHERE order_id IN (2099000000000000701,2099000000000000702,2099000000000000703);
-- DELETE FROM erp_purchase_order_item   WHERE order_id IN (2099000000000000701,2099000000000000702,2099000000000000703);
-- DELETE FROM erp_purchase_order_partner_snapshot WHERE order_id IN (2099000000000000701,2099000000000000702,2099000000000000703);
-- DELETE FROM erp_purchase_order        WHERE order_no LIKE 'E2E-CGD-%';
-- DELETE FROM sys_user_role   WHERE user_id = 2099000000000000981;
-- DELETE FROM sys_user_tenant WHERE user_id = 2099000000000000981;
-- DELETE FROM sys_user        WHERE id = 2099000000000000981;
