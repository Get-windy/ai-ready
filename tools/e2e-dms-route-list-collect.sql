-- 配送路线单「围栏归集」E2E 测试夹具
--
-- 用途：让「销售出库单（已发货）→ 客户配送坐标 → 落入围栏 → 自动加入路线单」链路可被端到端验证。
-- 现有测试营业数据里，出库单 XSCK202609030001/2 无收货地址、客户 1 无配送坐标，故补齐。
--
-- 用法：python tools/dbq.py "<语句>" 逐条执行；验收结束后执行文件末尾「清理」段。

-- ① 客户配送坐标（北京市东城区，GCJ-02）
UPDATE biz_party SET latitude = 39.928900, longitude = 116.416400 WHERE id = 1;

-- ② 待配送出库单补齐收货地址与收货人
UPDATE erp_sale_outbound
   SET shipping_address = '北京市东城区东长安街1号',
       receiver_name    = '张收货',
       receiver_phone   = '13900001111'
 WHERE outbound_no IN ('XSCK202609030001', 'XSCK202609030002');

-- ③ 待发货销售订单同理（status=3 部分发货）
UPDATE erp_sale_order
   SET shipping_address = '北京市东城区东长安街1号',
       receiver_name    = '张收货',
       receiver_phone   = '13900001111'
 WHERE order_no = 'QO202607232493';

-- ⚠️ 清理（验收结束后执行）：
-- DELETE FROM erp_route_point WHERE order_id LIKE 'OUT:%' OR order_id LIKE 'SO:%';
-- DELETE FROM erp_delivery_route WHERE fence_id IS NOT NULL;
-- UPDATE biz_party SET latitude = NULL, longitude = NULL WHERE id = 1;
-- UPDATE erp_sale_outbound SET shipping_address = NULL, receiver_name = NULL, receiver_phone = NULL
--  WHERE outbound_no IN ('XSCK202609030001', 'XSCK202609030002');
-- UPDATE erp_sale_order SET shipping_address = NULL, receiver_name = NULL, receiver_phone = NULL
--  WHERE order_no = 'QO202607232493';

-- ④ 每次执行 E2E 前的「归集状态重置」（让出库单/订单重新变成「未入线」）
-- DELETE FROM erp_route_point WHERE order_id LIKE 'OUT:%' OR order_id LIKE 'SO:%';
