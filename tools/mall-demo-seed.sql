-- ═══════════════════════════════════════════════════════════════════════════════
-- 商城 C 端「可演示数据」脚本（dev 环境）
--
-- 目的：让《商城App设计方案》一期的页面**能看出效果** —— 商品有价、有库存、在架上。
-- 现状（2026-09-26 实测）：v_mall_product 只有 6 行，其中 2 行是 E2E 造数残留；
--   4 个真实商品的 retail_price/wholesale_price/standard_price **全为 0 或 null**，
--   前端因此只能显示「暂无价格」；海鲜全家福与酒酿果味酱罐头的 mall_shelf_status=0（下架）。
--
-- ⚠️ 这是**演示数据**、不是业务数据：价格是合理区间内的示例值，不是真实定价。
--    脚本幂等（重复执行结果一致）。全部改动都可用文件末尾的【回滚】段还原。
--    不动图片（image_url 保持原样）—— 不联网、不引用外链。
--
-- 用法：psql -f db/mall-demo-seed.sql   （或 python tools/dbq.py "$(cat db/mall-demo-seed.sql)"）
-- ═══════════════════════════════════════════════════════════════════════════════

BEGIN;

-- ── ① 给 4 个**真实**商品补价格（零售价 = 商城售价，批发价 = 划线市场价）──
-- 商品 id 与名称来自 erp_product；SP-TEST-* 是本项目自带的测试商品。
UPDATE erp_product SET retail_price = 32.80, wholesale_price = 39.90, mall_shelf_status = 1
 WHERE id = 990000000000000001 AND deleted = 0;   -- 博多家园百香果果酱

UPDATE erp_product SET retail_price = 68.00, wholesale_price = 79.00, mall_shelf_status = 1
 WHERE id = 990000000000000002 AND deleted = 0;   -- 海鲜全家福（原为下架）

UPDATE erp_product SET retail_price = 19.90, wholesale_price = 25.90, mall_shelf_status = 1
 WHERE id = 990000000000000003 AND deleted = 0;   -- 手工蒸饺

UPDATE erp_product SET retail_price = 45.50, wholesale_price = 55.00, mall_shelf_status = 1
 WHERE id = 2073239284579586050 AND deleted = 0;  -- 博多新米坊酒酿果味酱罐头（原为下架）

-- ── ② 演示用的商城展示口径（这两个字段是「商品上架」页可维护的商城侧字段）──
--     起订量 2 让「N 起订」角标可见；标签 TAG_2 让商品卡右上角标有内容。
UPDATE erp_product SET mall_min_order_qty = 2, mall_tags = 'TAG_2'
 WHERE id = 990000000000000002 AND deleted = 0;
UPDATE erp_product SET mall_min_order_qty = 5
 WHERE id = 2073239284579586050 AND deleted = 0;

-- ── ③ 补库存（主仓库，tenant 1；无库存的商品在商城里会整卡标「缺货」）──
--     口径与已有库存行一致：quantity 与 available_quantity 同步写。
UPDATE erp_stock SET quantity = 200.00, available_quantity = 200.0000
 WHERE product_id = 990000000000000001 AND warehouse_id = 1 AND deleted = 0;

INSERT INTO erp_stock (id, tenant_id, product_id, product_name, product_code, warehouse_id, warehouse_name,
                       quantity, available_quantity, unit, is_initial, deleted, create_time, update_time, remark)
SELECT 2090000000000000101, 1, 990000000000000002, '海鲜全家福', 'SP-TEST-002', 1, '主仓库',
       80.00, 80.0000, '袋', 0, 0, now(), now(), '商城演示数据'
 WHERE NOT EXISTS (SELECT 1 FROM erp_stock WHERE id = 2090000000000000101);

-- ── ④ 把 2 个 E2E 造数商品下架（它们是其它验收会话的残留，出现在商城首页是噪音）──
--     只下架、不删除：对方若仍需核对，数据仍在。
UPDATE erp_product SET mall_shelf_status = 0
 WHERE id IN (2098356930360291330, 2098657110695522306) AND deleted = 0;

COMMIT;

-- ═══════════════════════════════════════════════════════════════════════════════
-- 【回滚】把上面改动恢复为 2026-09-26 之前的状态：
--
-- BEGIN;
-- UPDATE erp_product SET retail_price = NULL, wholesale_price = 0.00, mall_shelf_status = 1, mall_min_order_qty = 0, mall_tags = NULL
--  WHERE id IN (990000000000000001, 990000000000000003) AND deleted = 0;
-- UPDATE erp_product SET retail_price = NULL, wholesale_price = 0.00, mall_shelf_status = 0, mall_min_order_qty = 0, mall_tags = NULL
--  WHERE id = 990000000000000002 AND deleted = 0;
-- UPDATE erp_product SET retail_price = NULL, wholesale_price = 0.00, mall_shelf_status = 0, mall_min_order_qty = 0
--  WHERE id = 2073239284579586050 AND deleted = 0;
-- UPDATE erp_product SET mall_shelf_status = 1
--  WHERE id IN (2098356930360291330, 2098657110695522306) AND deleted = 0;
-- UPDATE erp_stock SET quantity = 0.00, available_quantity = 0.0000
--  WHERE product_id = 990000000000000001 AND warehouse_id = 1 AND deleted = 0;
-- DELETE FROM erp_stock WHERE id = 2090000000000000101;
-- DELETE FROM shop_user_tenant WHERE shop_user_id = 2090000000000000301;
-- DELETE FROM shop_user WHERE id = 2090000000000000301;
-- 
-- ── ⑤ 演示买家账号（用于把 C 端「需登录」页面跑起来：购物车/我的/订单/地址/资料）──
--     密码复用 admin 的哈希（即 admin123）；tenant_id=0 是**系统级身份**的约定值
--     （顾客已升为系统级，「属于哪家店」由下面的 shop_user_tenant 表达）。
INSERT INTO shop_user (id, tenant_id, username, password, nickname, phone, user_type, audit_status, status, deleted, create_time, update_time)
SELECT 2090000000000000301, 0, 'demo_buyer', password, '演示买家', '13900000001', 'MEMBER', 1, 1, 0, now(), now()
  FROM sys_user WHERE username = 'admin'
  AND NOT EXISTS (SELECT 1 FROM shop_user WHERE id = 2090000000000000301);

--     本店已通过审核（status=1）+ 启用（enabled=1）⇒ 价格三态落在「已认证」态
INSERT INTO shop_user_tenant (id, shop_user_id, tenant_id, status, enabled, is_default, deleted, create_time, update_time)
VALUES (2090000000000000302, 2090000000000000301, 1, 1, 1, 1, 0, now(), now())
  ON CONFLICT DO NOTHING;

COMMIT;
--
-- （③ 中「手工蒸饺 152」「酒酿果味酱 105」两行库存**未被本脚本改动**，无需回滚。）
-- ═══════════════════════════════════════════════════════════════════════════════
