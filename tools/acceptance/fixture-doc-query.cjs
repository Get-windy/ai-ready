// 销售单据查询页验收夹具：补齐 4 类单据（订单 / 出库 / 退货 / 换货）可查询数据
// 幂等：固定 ID + ON CONFLICT DO UPDATE
const path = require('path')
const { Client } = require(path.join(__dirname, '../../frontend/apps/pc-admin/node_modules/pg'))

const ORDER_ID = '9100000000000000001'
const ORDER_ITEM_1 = '9100000000000000011'
const ORDER_ITEM_2 = '9100000000000000012'
const RETURN_ID = '9100000000000000003'
const RETURN_ITEM_1 = '9100000000000000031'
const EXCHANGE_ID = '9200000000000000001'   // 已存在的换货单
const EXCHANGE_ITEM_1 = '9100000000000000021'

async function main() {
  const c = new Client({ host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' })
  await c.connect()
  const q = (sql, params) => c.query(sql, params)

  // ── 1. 销售订单（正常状态，含结算金额 → 结算状态应派生为「部分结算」） ──
  await q(`
    INSERT INTO erp_sale_order (id, tenant_id, order_no, order_date, sale_type, status,
      customer_id, warehouse_id, salesman_id, dept_id,
      product_amount, bill_amount, settled_amount, total_quantity,
      customer_name, customer_code, customer_level, warehouse_name, salesman_name, dept_name,
      freight_payer, shipping_fee, other_fee, promo_discount, coupon_amount, direct_discount, used_points,
      receiver_name, receiver_phone, shipping_address, region, generation_method,
      ext_num1, ext_num2, ext_text1, ext_text2, ext_text3,
      remark, buyer_remark, summary, print_count, creator_name,
      source_order, create_time, update_time, deleted)
    VALUES ($1, 1, 'ZZT-QO-001', '2026-09-02', 1, 0,
      NULL, NULL, NULL, NULL,
      200.00, 200.00, 100.00, 5,
      '验收测试客户', 'C0001', 'A级', '主仓库', '张三', '销售部',
      '卖方', 10.00, 5.00, 2.00, 1.00, 3.00, 4.00,
      '李四', '13800000000', '测试地址1号', '华东', 'MANUAL',
      11.00, 22.00, 'EXT-T1', 'EXT-T2', 'EXT-T3',
      '夹具订单备注', '夹具买家备注', '夹具摘要', 0, 'admin',
      'ZZT-SRC-001', now(), now(), 0)
    ON CONFLICT (id) DO UPDATE SET remark = EXCLUDED.remark, status = EXCLUDED.status
  `, [ORDER_ID])

  await q(`
    INSERT INTO erp_sale_order_item (id, order_id, tenant_id, line_no, product_id, product_name,
      quantity, unit_price, amount, cost_price, cost_amount, gross_profit, line_attribute, create_time)
    VALUES ($1, $2, 1, 1, NULL, '夹具商品A', 3, 40.00, 120.00, 40.00, 120.00, 80.00, 'NORMAL', now()),
           ($3, $2, 1, 2, NULL, '夹具商品B', 2, 40.00, 80.00, 40.00, 80.00, 60.00, 'GIFT', now())
    ON CONFLICT (id) DO NOTHING
  `, [ORDER_ITEM_1, ORDER_ID, ORDER_ITEM_2])

  // ── 2. 销售退货单（含参考成本 → 成本金额 60，毛利 = 本单金额 - 成本） ──
  await q(`
    INSERT INTO erp_sale_return_doc (id, tenant_id, return_doc_no, order_date, status,
      customer_id, warehouse_id, handler_id, dept_id,
      product_amount, total_amount, settled_amount, total_quantity,
      customer_name, customer_code, customer_level, warehouse_name, handler_name, dept_name,
      settle_status, freight_payer, shipping_fee, other_fee, promo_discount, coupon_amount, direct_discount,
      receiver_name, receiver_phone, shipping_address,
      ext_num1, ext_num2, ext_text1, ext_text2, ext_text3,
      remark, buyer_remark, summary, print_count, creator_name, bookkeeper_name,
      source_order, create_time, update_time, deleted)
    VALUES ($1, 1, 'ZZT-RET-001', '2026-09-03', 0,
      NULL, NULL, NULL, NULL,
      100.00, 100.00, 0.00, 2,
      '验收测试客户', 'C0001', 'A级', '主仓库', '张三', '销售部',
      NULL, '卖方', 8.00, 3.00, 1.00, 0.50, 1.50,
      '李四', '13800000000', '测试地址2号',
      33.00, 44.00, 'RET-T1', 'RET-T2', 'RET-T3',
      '夹具退货备注', '夹具退货买家备注', '夹具退货摘要', 0, 'admin', NULL,
      'ZZT-QO-001', now(), now(), 0)
    ON CONFLICT (id) DO UPDATE SET remark = EXCLUDED.remark, status = EXCLUDED.status
  `, [RETURN_ID])

  await q(`
    INSERT INTO erp_sale_return_doc_item (id, return_doc_id, tenant_id, product_id, product_name,
      return_quantity, unit_price, line_amount, ref_cost_price, ref_cost_amount, product_line_attr, deleted, create_time)
    VALUES ($1, $2, 1, NULL, '夹具退货商品', 2, 50.00, 100.00, 30.00, 60.00, 'NORMAL', 0, now())
    ON CONFLICT (id) DO NOTHING
  `, [RETURN_ITEM_1, RETURN_ID])

  // ── 3. 销售换货单（已有单据，补仓库/人员快照 + 明细成本） ──
  await q(`
    UPDATE erp_sale_exchange
    SET in_warehouse_name = '主仓库', out_warehouse_name = '次仓库',
        handler_name = '张三', dept_name = '销售部', creator_name = 'admin',
        bookkeeper_name = 'admin', sales_type = '换货', settle_status = 'unsettled',
        customer_code = 'C0001', customer_level = 'A级',
        ext_num1 = 55, ext_num2 = 66, ext_text1 = 'EX-T1', ext_text2 = 'EX-T2', ext_text3 = 'EX-T3'
    WHERE id = $1
  `, [EXCHANGE_ID])

  // ── 4. 已取消订单（验证「显示红冲」过滤口径） ──
  await q(`
    INSERT INTO erp_sale_exchange_item (id, exchange_id, tenant_id, product_id, product_name,
      original_quantity, exchange_quantity, original_price, exchange_price, unit,
      cost_price, cost_amount, product_line_attr, deleted, create_time, update_time)
    VALUES ($1, $2, 1, NULL, '夹具换货商品', 1, 1, 100.00, 100.00, '件',
      40.00, 40.00, 'NORMAL', 0, now(), now())
    ON CONFLICT (id) DO NOTHING
  `, [EXCHANGE_ITEM_1, EXCHANGE_ID])

  await q(`
    INSERT INTO erp_sale_order (id, tenant_id, order_no, order_date, sale_type, status,
      product_amount, bill_amount, total_quantity, customer_name,
      creator_name, create_time, update_time, deleted)
    VALUES ('9100000000000000002', 1, 'ZZT-QO-CANCEL', '2026-09-04', 1, 6,
      50.00, 50.00, 1, '验收测试客户',
      'admin', now(), now(), 0)
    ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status
  `)

  const check = await q(`
    SELECT
      (SELECT count(*) FROM erp_sale_order WHERE order_no LIKE 'ZZT-%' OR order_no = 'ZZT-QO-001') AS orders,
      (SELECT count(*) FROM erp_sale_return_doc WHERE return_doc_no = 'ZZT-RET-001') AS returns,
      (SELECT count(*) FROM erp_sale_exchange WHERE exchange_no = 'ZZTEST-HH-001') AS exchanges,
      (SELECT count(*) FROM erp_sale_outbound) AS outbounds
  `)
  console.log('夹具就绪:', JSON.stringify(check.rows[0]))
  await c.end()
}

main().catch(e => { console.error('FIXTURE ERROR:', e.message); process.exit(1) })
