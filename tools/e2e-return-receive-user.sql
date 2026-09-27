-- 物流退货收货（配发收 → 收货业务 → 物流退货收货，菜单 70160）E2E 专用验收账号 + 数据基座
-- 用法：python tools/dbq2.py "<本文件内容>" 或 psql -f；验收结束后执行文件末尾「清理」语句回收基座数据。
--
-- 基座说明（覆盖收货进度三态，全部初始为「已审核待收货」）：
--   · E2E-THSH-0001：退货 10（果酱 6 + 海鲜全家福 4）→ 部分收货用例（退货单收 4）
--   · E2E-THSH-0002：退货 8（手工蒸饺 8）           → 全部收货用例（退货单收 8 → 申请已完成）
--   · E2E-THSH-0003：退货 5（果酱 5）               → 取消回退用例（收货后再取消退货单）
--   明细必须带 product_id：收货回写按商品 ID 匹配（erp_sale_return_item.received_quantity）
--
-- 商品快照取既有商品：990000000000000001/2/3（SP-TEST-001/2/3）

-- ── 1) 验收账号（密码复用 admin = admin123） ──
DELETE FROM sys_user_role   WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_retrecv')
                               OR id = 2099000000000000802;
DELETE FROM sys_user_tenant WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_retrecv')
                               OR id = 2099000000000000803;
DELETE FROM sys_user        WHERE username = 'e2e_retrecv' OR id = 2099000000000000801;

INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                      user_type, is_super_admin, is_tenant_admin, status, data_scope)
SELECT 2099000000000000801, 1, 0, now(), now(), 'e2e_retrecv', password, 'E2E退货收货', 'E2E退货收货',
       1, false, false, 1, 'ALL'
FROM sys_user WHERE username = 'admin';

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
VALUES (2099000000000000802, 2099000000000000801, 1, 1, now());

INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000000803, 2099000000000000801, 1, true, 1, now(), now());

-- ── 2) 退货申请单（status=2 已审核待收货；已收数量 0，未收 = 退货数量） ──
DELETE FROM erp_sale_return_item WHERE return_id IN (2099000000000000810, 2099000000000000811, 2099000000000000812);
DELETE FROM erp_sale_return      WHERE return_no LIKE 'E2E-THSH-%';

INSERT INTO erp_sale_return (id, tenant_id, deleted, return_no, customer_id, customer_name, customer_code, customer_level,
                             contact_name, contact_phone, contact_address, customer_remark, customer_ticket,
                             warehouse_id, warehouse_name, handler_id, handler_name, dept_name,
                             order_date, status, settle_status, sales_type,
                             total_quantity, ordered_quantity, received_quantity, unreceived_quantity,
                             total_amount, product_amount, discount_amount, discount_bill_amount, bill_amount,
                             shipping_fee, other_fee, freight_payer, delivery_method,
                             logistics_company, waybill_no,
                             generate_type, print_count, creator_name, submit_by, submit_time,
                             auditor_name, auditor_id, audit_time, attachment, remark, summary,
                             ext_num1, ext_num2, ext_text1, ext_text2, ext_text3,
                             footer_ext_text1, footer_ext_text2, create_time, update_time)
VALUES
 (2099000000000000810, 1, 0, 'E2E-THSH-0001', 2099000000000000851, 'E2E退货客户', 'E2EKHSH001', '一级客户',
  'E2E联系人', '13900000011', 'E2E退货地址1号', 'E2E客户备注', 'YPT001',
  1, '主仓库', 2099000000000000801, 'E2E退货收货', 'E2E部门',
  now(), 2, '未结算', '正常销售',
  10, 10, 0, 10,
  1000.00, 1000.00, 0.00, 1000.00, 1000.00,
  0.00, 0.00, '我方', '配送',
  'E2E物流公司', 'E2EWL0001',
  '手动创建', 1, 'E2E退货收货', 2099000000000000801, now(),
  'E2E审核人', 2099000000000000801, now(), '', 'E2E单据备注1', 'E2E摘要1',
  11, 12, 'E2E文本1', 'E2E文本2', 'E2E文本3',
  'E2E表尾1', 'E2E表尾2', now(), now()),
 (2099000000000000811, 1, 0, 'E2E-THSH-0002', 2099000000000000852, 'E2E退货客户二', 'E2EKHSH002', '二级客户',
  'E2E联系人二', '13900000012', 'E2E退货地址2号', 'E2E客户备注二', '',
  1, '主仓库', 2099000000000000801, 'E2E退货收货', 'E2E部门',
  now(), 2, '未结算', '正常销售',
  8, 8, 0, 8,
  400.00, 400.00, 0.00, 400.00, 400.00,
  0.00, 0.00, '我方', '配送',
  '', '',
  '手动创建', 0, 'E2E退货收货', 2099000000000000801, now(),
  'E2E审核人', 2099000000000000801, now(), '', 'E2E单据备注2', 'E2E摘要2',
  21, 22, 'E2E文本21', 'E2E文本22', 'E2E文本23',
  '', '', now(), now()),
 (2099000000000000812, 1, 0, 'E2E-THSH-0003', 2099000000000000853, 'E2E退货客户三', 'E2EKHSH003', '一级客户',
  'E2E联系人三', '13900000013', 'E2E退货地址3号', '', '',
  1, '主仓库', 2099000000000000801, 'E2E退货收货', 'E2E部门',
  now(), 2, '未结算', '正常销售',
  5, 5, 0, 5,
  500.00, 500.00, 0.00, 500.00, 500.00,
  0.00, 0.00, '我方', '配送',
  '', '',
  '手动创建', 0, 'E2E退货收货', 2099000000000000801, now(),
  'E2E审核人', 2099000000000000801, now(), '', 'E2E单据备注3', 'E2E摘要3',
  31, 32, 'E2E文本31', 'E2E文本32', 'E2E文本33',
  '', '', now(), now());

-- ── 3) 退货申请明细（received_quantity 初始 0；未收数量由 退货数量-已收数量 派生） ──
INSERT INTO erp_sale_return_item (id, tenant_id, return_id, line_no, product_id, product_code, product_name,
                                  barcode, specification, model_no, origin_place, unit,
                                  return_quantity, received_quantity, unit_price, line_amount,
                                  item_remark, product_line_attr, is_gift, weight, volume,
                                  ext_num1, ext_num2, ext_num3, ext_text1, ext_text2,
                                  create_time, update_time, deleted)
VALUES
 (2099000000000000820, 1, 2099000000000000810, 1, 990000000000000001, 'SP-TEST-001', '博多家园百香果果酱',
  '6900000000001', '500g', 'E2E-M1', '国产', '瓶',
  6, 0, 100.0000, 600.00,
  'E2E明细备注1', '正常', false, 1.20, 0.50,
  1, 2, 3, 'E2E明细文本1', 'E2E明细文本2', now(), now(), 0),
 (2099000000000000821, 1, 2099000000000000810, 2, 990000000000000002, 'SP-TEST-002', '海鲜全家福',
  '6900000000002', '1kg', 'E2E-M2', '国产', '袋',
  4, 0, 100.0000, 400.00,
  'E2E明细备注2', '正常', false, 2.00, 1.00,
  4, 5, 6, 'E2E明细文本3', 'E2E明细文本4', now(), now(), 0),
 (2099000000000000822, 1, 2099000000000000811, 1, 990000000000000003, 'SP-TEST-003', '手工蒸饺',
  '6900000000003', '500g', 'E2E-M3', '国产', '袋',
  8, 0, 50.0000, 400.00,
  'E2E明细备注3', '正常', false, 1.50, 0.60,
  7, 8, 9, 'E2E明细文本5', 'E2E明细文本6', now(), now(), 0),
 (2099000000000000823, 1, 2099000000000000812, 1, 990000000000000001, 'SP-TEST-001', '博多家园百香果果酱',
  '6900000000001', '500g', 'E2E-M1', '国产', '瓶',
  5, 0, 100.0000, 500.00,
  'E2E明细备注4', '正常', false, 1.20, 0.50,
  11, 12, 13, 'E2E明细文本7', 'E2E明细文本8', now(), now(), 0);

-- ⚠️ 清理（验收结束后执行；退货单由脚本按 return_apply_id 关联清理）：
-- DELETE FROM erp_sale_return_doc_item WHERE return_doc_id IN (SELECT id FROM erp_sale_return_doc WHERE return_apply_no LIKE 'E2E-THSH-%');
-- DELETE FROM erp_sale_return_doc      WHERE return_apply_no LIKE 'E2E-THSH-%';
-- DELETE FROM erp_sale_return_item     WHERE return_id IN (2099000000000000810, 2099000000000000811, 2099000000000000812);
-- DELETE FROM erp_sale_return          WHERE return_no LIKE 'E2E-THSH-%';
-- DELETE FROM sys_user_role   WHERE user_id = 2099000000000000801;
-- DELETE FROM sys_user_tenant WHERE user_id = 2099000000000000801;
-- DELETE FROM sys_user        WHERE id = 2099000000000000801;
