-- 交易中心重构 - SaleOrder 扩展字段
-- V8.11.0__Trade_Center_SaleOrder_Extension.sql

-- 扩展 erp_sale_order 表字段
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS order_source INT DEFAULT 1;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS payment_method VARCHAR(20);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS payment_status INT DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS delivery_status INT DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS consignee VARCHAR(50);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS consignee_phone VARCHAR(20);

COMMENT ON COLUMN erp_sale_order.order_source IS '订单来源: 1内部销售, 2B2B商城, 3B2C零售, 4H5商城, 5小程序';
COMMENT ON COLUMN erp_sale_order.payment_method IS '支付方式: ALIPAY, WECHAT, UNIONPAY, BANK, CASH';
COMMENT ON COLUMN erp_sale_order.payment_status IS '支付状态: 0待支付, 1支付中, 2已支付, 3部分支付, 4已退款';
COMMENT ON COLUMN erp_sale_order.delivery_status IS '发货状态: 0待发货, 1部分发货, 2已发货, 3已签收';
COMMENT ON COLUMN erp_sale_order.consignee IS '收货人';
COMMENT ON COLUMN erp_sale_order.consignee_phone IS '收货人电话';

-- 订单来源枚举定义表（参考）
-- 1 = INTERNAL_SALES   内部销售
-- 2 = B2B_MALL         B2B批发商城
-- 3 = B2C_POS          B2C零售收银
-- 4 = H5_MALL          H5商城
-- 5 = MINIAPP          小程序商城

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_sale_order_source ON erp_sale_order(order_source);
CREATE INDEX IF NOT EXISTS idx_sale_order_payment_status ON erp_sale_order(payment_status);
CREATE INDEX IF NOT EXISTS idx_sale_order_delivery_status ON erp_sale_order(delivery_status);

-- 菜单数据 - 交易中心（替代原商城菜单）
UPDATE sys_menu SET menu_name = '交易中心', path = 'trade-center', icon = 'shop' WHERE id = 600;
UPDATE sys_menu SET menu_name = '商城订单', path = 'trade-mall-order', component = 'views/trade/mall-order/list' WHERE id = 601;
UPDATE sys_menu SET menu_name = '购物车', path = 'trade-cart', component = 'views/trade/cart/list' WHERE id = 602;

-- 新增零售POS菜单
INSERT INTO sys_menu (id, parent_id, menu_name, path, menu_type, component, icon, sort, visible)
VALUES
(603, 600, '零售收银', 'trade-pos', 1, 'views/trade/pos/index', NULL, 3, 1)
ON CONFLICT (id) DO NOTHING;